import axios from "axios";
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from "axios";

/**
 * Build a stable deduplication key from a request config.
 * Requests sharing the same key are considered duplicates — only the most
 * recent one survives; any earlier in-flight request is cancelled.
 */
function dedupeKey(config: AxiosRequestConfig): string {
  const { method, url, params, data } = config;
  const raw = JSON.stringify({ method, url, params, data });
  let hash = 0;
  for (let i = 0; i < raw.length; i++) {
    hash = ((hash << 5) - hash + raw.charCodeAt(i)) | 0;
  }
  return `${method ?? "get"}:${url ?? ""}:${hash}`;
}

const inFlight = new Map<string, AbortController>();

// --- request interceptor: cancel duplicate in-flight requests ---
axios.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const key = dedupeKey(config);

  // Cancel any previous identical request still in flight.
  const existing = inFlight.get(key);
  if (existing) {
    existing.abort();
    inFlight.delete(key);
  }

  const controller = new AbortController();
  config.signal = controller.signal;
  inFlight.set(key, controller);

  // Mark the request with its dedupe key so the response interceptor
  // can clean up.
  (config as unknown as Record<string, unknown>).__dedupeKey = key;

  return config;
});

// --- response interceptor: clean up in-flight tracking ---
axios.interceptors.response.use(
  (response) => {
    const key = (response.config as unknown as Record<string, unknown>).__dedupeKey as string | undefined;
    if (key) {
      inFlight.delete(key);
    }
    return response;
  },
  (error) => {
    if (axios.isCancel(error)) {
      // Silently swallow cancelled requests — they were replaced by a newer one.
      return Promise.reject(error);
    }
    const config = error?.config as unknown as Record<string, unknown> | undefined;
    const key = config?.__dedupeKey as string | undefined;
    if (key) {
      inFlight.delete(key);
    }
    return Promise.reject(error);
  }
);

export const API_BASE_URL = "http://172.20.14.234:8080/";

export function buildApiUrl(path: string): string {
  const normalizedBaseUrl = API_BASE_URL.replace(/\/+$/, "");
  const normalizedPath = path.replace(/^\/+/, "");

  return `${normalizedBaseUrl}/${normalizedPath}`;
}
 
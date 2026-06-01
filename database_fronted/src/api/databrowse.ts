import axios from "axios";
import { buildApiUrl } from "@/config/api";

export interface BrowseSampleQuery {
  keyword?: string;
  species?: string;
  sampleType?: string;
  tissue?: string;
  page: number;
  pageSize: number;
}

export interface BrowseFacetQuery {
  keyword?: string;
  species?: string;
  sampleType?: string;
  tissue?: string;
}

export interface BrowseSample {
  datasetId: string;
  sampleType: string;
  tissue: string;
  sampleName: string;
  cells: number | null;
  platform: string;
  sourceId: string;
  disease: string;
  sampleSource: string;
}

export interface BrowseSamplePage {
  records: BrowseSample[];
  total: number;
  page: number;
  pageSize: number;
}

export interface BrowseFacetItem {
  label: string;
  count: number;
}

export interface BrowseFacetResponse {
  species: BrowseFacetItem[];
  sampleType: BrowseFacetItem[];
  tissue: BrowseFacetItem[];
}

export type BrowseFacetKey = keyof BrowseFacetResponse;

interface BackendEnvelope<T> {
  code?: number;
  message?: string;
  data?: T;
}

function cleanParams<T extends object>(params: T): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== "")
  );
}

function unwrapBackendResponse<T>(
  payload: T | BackendEnvelope<T>,
  fallback: T,
  endpointName: string
): T {
  if (payload && typeof payload === "object" && "code" in payload) {
    const envelope = payload as BackendEnvelope<T>;

    if (envelope.code !== 200) {
      console.error(
        `[Browse API] ${endpointName} returned unsuccessful code ${envelope.code}: ${envelope.message ?? ""}`
      );
      return fallback;
    }

    return envelope.data ?? fallback;
  }

  return payload as T;
}

export async function fetchBrowseSamples(params: BrowseSampleQuery): Promise<BrowseSamplePage> {
  const fallback: BrowseSamplePage = {
    records: [],
    total: 0,
    page: params.page,
    pageSize: params.pageSize,
  };

  const { data } = await axios.get<BrowseSamplePage | BackendEnvelope<BrowseSamplePage>>(
    buildApiUrl("api/browse/samples"),
    {
      params: cleanParams(params),
    }
  );

  return unwrapBackendResponse(data, fallback, "GET /api/browse/samples");
}

export async function fetchBrowseFacets(params: BrowseFacetQuery = {}): Promise<BrowseFacetResponse> {
  const fallback: BrowseFacetResponse = {
    species: [],
    sampleType: [],
    tissue: [],
  };

  const { data } = await axios.get<BrowseFacetResponse | BackendEnvelope<BrowseFacetResponse>>(
    buildApiUrl("api/browse/facets"),
    {
      params: cleanParams(params),
    }
  );

  return unwrapBackendResponse(data, fallback, "GET /api/browse/facets");
}

import axios from "axios";
import { buildApiUrl } from "@/config/api";

// =============================================================================
// Types
// =============================================================================

export type AnalysisDomain = "integration" | "rna" | "atac";

export type AnalysisGroupBy =
  | "cell_type"
  | "cluster";

export type AnalysisMarkerReference = "integration_expression";

export type AnalysisFdrMethod = "BH";

const MARKER_REFERENCE_LABELS: Record<AnalysisMarkerReference, string> = {
  integration_expression: "Integration expression markers",
};

export function markerReferenceLabel(ref: AnalysisMarkerReference): string {
  return MARKER_REFERENCE_LABELS[ref] ?? ref;
}

const GROUP_BY_LABELS: Record<AnalysisGroupBy, string> = {
  cell_type:          "Cell type",
  cluster:            "Cluster",
};

export function groupByLabel(gb: AnalysisGroupBy): string {
  return GROUP_BY_LABELS[gb] ?? gb;
}

export interface CellTypeEnrichmentRequest {
  geneSymbols: string[];
  tissue: string;
  datasetId: string;
  markerReference: AnalysisMarkerReference;
  resultLevel: AnalysisGroupBy;
  minOverlap: number;
  backgroundUniverse: "selected_marker_reference";
  fdrMethod: AnalysisFdrMethod;
}

export interface EnrichmentResultRow {
  rank: number;
  cellType: string;
  context: string;
  overlap: number;
  enrichmentFold: number;
  pValue: number;
  pvalue?: number;
  fdr: number;
  datasetCount: number;
  genes: string[];
}

export interface CellTypeEnrichmentResponse {
  inputGenes: string[];
  matchedGenes: string[];
  unmatchedGenes: string[];
  totalResults: number;
  significantResults: number;
  topEnrichedCellType: string | null;
  results: EnrichmentResultRow[];
}

// =============================================================================
// Domain display helpers (shared with front-end components)
// =============================================================================

const DOMAIN_DISPLAY: Record<AnalysisDomain, string> = {
  integration: "Integration",
  rna: "RNA",
  atac: "ATAC",
};

export function domainDisplayLabel(domain: AnalysisDomain): string {
  return DOMAIN_DISPLAY[domain] ?? domain;
}

// =============================================================================
// Envelope unwrap (mirrors searchResult.ts pattern)
// =============================================================================

interface BackendEnvelope<T> {
  code?: number;
  message?: string;
  data?: T;
}

function unwrapAnalysisResponse<T>(payload: T | BackendEnvelope<T>, endpointName: string): T {
  if (payload && typeof payload === "object" && "code" in payload) {
    const envelope = payload as BackendEnvelope<T>;
    if (envelope.code !== 200) {
      throw new Error(
        `${endpointName} returned code ${envelope.code}: ${envelope.message ?? ""}`
      );
    }
    if (envelope.data === undefined || envelope.data === null) {
      throw new Error(`${endpointName} returned no data.`);
    }
    return envelope.data;
  }
  return payload as T;
}


// =============================================================================
// Sequence-to-Peak2Gene
// =============================================================================

export interface SequencePeak2GeneRequest {
  sequence: string;
  genomeBuild?: string;
  referenceScope?: string;
  datasetId?: string | null;
  resultContent?: string;
  blastTask?: string;
  maxTargetSeqs?: number;
  maxHsps?: number;
  evalueCutoff?: number;
  flankBp?: number;
  limit?: number | null;
}

export interface BlastHitDto {
  rank: number;
  hitId: string;
  chromosome: string;
  start: number;
  end: number;
  bedStart: number;
  bedEnd: number;
  strand: string;
  identity: number;
  queryCoverage: number;
  scoreRatio: number;
  primary: boolean;
  nearEquivalent: boolean;
  alignLen: number;
  mismatch: number;
  gapOpen: number;
  qStart: number;
  qEnd: number;
  evalue: string;
  bitScore: number;
}

export interface PeakGeneLinkDto {
  datasetId: string;
  domain: string;
  peakName: string;
  chromosome: string;
  peakStart: number;
  peakEnd: number;
  geneName: string;
  correlation: number;
  fdr: number;
  linkScore: number;
  sourceFile: string;
}

export interface MarkerPeakDto {
  datasetId: string;
  domain: string;
  clusterSource: string;
  groupName: string;
  peakName: string;
  chromosome: string;
  peakStart: number;
  peakEnd: number;
  log2fc: number;
  fdr: number;
  meanDiff: number;
  sourceFile: string;
  peakGeneLinks: PeakGeneLinkDto[];
}

export type SequenceMappingStatus = "NO_HIT" | "PARTIAL" | "UNIQUE" | "BEST_SUPPORTED" | "AMBIGUOUS";

export interface SequencePeak2GeneResponse {
  query: {
    sequenceLength: number;
    genomeBuild: string;
    usedHitIndex: number;
    nearEquivalentScoreRatio: number;
    blastCoordinateSystem: string;
    evidenceCoordinateSystem: string;
  };
  summary: {
    blastHitCount: number;
    returnedBlastHitCount: number;
    blastHitsTruncated: boolean;
    mappedRegionCount: number;
    candidateLocusCount: number;
    qualifiedCandidateCount: number;
    nearEquivalentLocusCount: number;
    subjectCount: number;
    candidateSearchLimited: boolean;
    overlappingPeakCount: number;
    linkedGeneCount: number;
    markerPeakCount: number;
    returnedP2gCount: number;
    returnedMarkerPeakCount: number;
    evidencePossiblyTruncated: boolean;
  };
  mappingStatus: SequenceMappingStatus;
  mappingMessage: string;
  evidenceHitId: string | null;
  blastHits: BlastHitDto[];
  peakGeneLinks: PeakGeneLinkDto[];
  markerPeaks: MarkerPeakDto[];
}

export type SequenceJobStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface SequencePeak2GeneJobResponse {
  jobId: string;
  status: SequenceJobStatus;
  progress: number;
  stage: string;
  message: string;
  error?: string | null;
  pollAfterMs: number;
  createdAt: string;
  updatedAt: string;
  result?: SequencePeak2GeneResponse | null;
}

export interface SequencePeak2GeneEvidenceRequest {
  hitId: string;
  hitRank: number;
  chromosome: string;
  start: number;
  end: number;
  strand: string;
  referenceScope?: string;
  datasetId?: string | null;
  resultContent?: string;
  flankBp?: number;
  limit?: number | null;
}

export interface SequencePeak2GeneEvidenceResponse {
  hitId: string;
  hitRank: number;
  chromosome: string;
  start: number;
  end: number;
  bedStart: number;
  bedEnd: number;
  coordinateSystem: string;
  overlappingPeakCount: number;
  linkedGeneCount: number;
  returnedP2gCount: number;
  returnedMarkerPeakCount: number;
  possiblyTruncated: boolean;
  peakGeneLinks: PeakGeneLinkDto[];
  markerPeaks: MarkerPeakDto[];
}

export async function runSequencePeak2Gene(
  request: SequencePeak2GeneRequest
): Promise<SequencePeak2GeneResponse> {
  const { data } = await axios.post<SequencePeak2GeneResponse>(
    buildApiUrl("api/analysis/sequence-peak2gene"),
    request
  );
  return data;
}

export async function submitSequencePeak2GeneJob(
  request: SequencePeak2GeneRequest
): Promise<SequencePeak2GeneJobResponse> {
  const { data } = await axios.post<SequencePeak2GeneJobResponse>(
    buildApiUrl("api/analysis/sequence-peak2gene/jobs"),
    request
  );
  return data;
}

export async function fetchSequencePeak2GeneJob(jobId: string): Promise<SequencePeak2GeneJobResponse> {
  const { data } = await axios.get<SequencePeak2GeneJobResponse>(
    buildApiUrl(`api/analysis/sequence-peak2gene/jobs/${encodeURIComponent(jobId)}`)
  );
  return data;
}

export async function fetchSequencePeak2GeneEvidence(
  request: SequencePeak2GeneEvidenceRequest
): Promise<SequencePeak2GeneEvidenceResponse> {
  const { data } = await axios.post<SequencePeak2GeneEvidenceResponse>(
    buildApiUrl("api/analysis/sequence-peak2gene/evidence"),
    request
  );
  return data;
}

/**
 * Check if the analysis endpoint is temporarily unavailable.
 */
// =============================================================================
// API functions
// =============================================================================

/**
 * Run cell type enrichment analysis.
 * POST /api/analysis/cell-type-enrichment
 */
export async function runCellTypeEnrichment(
  request: CellTypeEnrichmentRequest
): Promise<CellTypeEnrichmentResponse> {
  const { data } = await axios.post<
    CellTypeEnrichmentResponse | BackendEnvelope<CellTypeEnrichmentResponse>
  >(buildApiUrl("api/analysis/cell-type-enrichment"), request);

  const response = unwrapAnalysisResponse(data, "POST /api/analysis/cell-type-enrichment");
  response.results = (response.results ?? []).map((row) => ({
    ...row,
    pValue: row.pValue ?? row.pvalue ?? 1,
    genes: row.genes ?? [],
  }));
  return response;
}

export interface CellTypeEnrichmentDatasetOption {
  dataset_id: string;
  sample_name: string;
}

export async function fetchCellTypeEnrichmentTissues(): Promise<string[]> {
  const { data } = await axios.get<string[]>(
    buildApiUrl("api/analysis/cell-type-enrichment/tissues")
  );
  return Array.isArray(data) ? data : [];
}

export async function fetchCellTypeEnrichmentDatasets(
  tissue: string
): Promise<CellTypeEnrichmentDatasetOption[]> {
  const { data } = await axios.get<CellTypeEnrichmentDatasetOption[]>(
    buildApiUrl("api/analysis/cell-type-enrichment/datasets"),
    { params: { tissue } }
  );
  return Array.isArray(data) ? data : [];
}

/**
 * Fetch all visible dataset IDs from the browse endpoint.
 */
export interface DatasetOption {
  value: string;  // dataset_id
  label: string;  // sample_name or dataset_id
}

export async function fetchAllDatasets(): Promise<DatasetOption[]> {
  const { data } = await axios.get<Array<{ dataset_id: string; sample_name: string }>>(
    buildApiUrl("api/analysis/datasets")
  );
  if (!Array.isArray(data)) return [];
  return data.map((item: any) => ({
    value: item.dataset_id ?? item.DATASET_ID ?? "",
    label: `${item.dataset_id ?? item.DATASET_ID} — ${item.sample_name ?? item.SAMPLE_NAME ?? ""}`,
  }));
}

export interface TissueCountItem {
  tissue: string;
  cnt: number;
}

export async function fetchSearchTissueOptions(): Promise<string[]> {
  const { data } = await axios.get<TissueCountItem[]>(
    buildApiUrl("api/search/tissue-counts"),
    { timeout: 10000 }
  );
  if (!Array.isArray(data)) return [];
  return data
    .map((item) => item.tissue)
    .filter((tissue): tissue is string => Boolean(tissue))
    .sort((a, b) => a.localeCompare(b));
}

export function isAnalysisEndpointUnavailable(error: unknown): boolean {
  if (error && typeof error === "object" && "response" in error) {
    const status = (error as { response?: { status?: number } }).response?.status;
    return status === 503 || status === 502 || status === 504;
  }
  return false;
}

// =============================================================================
// Peak-Gene Regulatory Context Analysis
// =============================================================================

export interface PeakGeneContextRequest {
  peaks: Array<{ chrom: string; start: number; end: number }>;
  genes: string[];
  tissue: string;
  datasetId?: string | null;
  referenceMode: "p2g_only" | "p2g_markers";
  resultType: "general" | "cell_type";
  advanced: {
    minOverlapBp: number;
    maxReturnedLinks: number | null;
  };
}

export interface PeakGeneContextDatasetOption {
  dataset_id: string;
  sample_name: string;
}

export interface PeakGeneContextResponse {
  summary: {
    totalPairs: number;
    uniquePeaks: number;
    uniqueGenes: number;
    uniqueDatasets: number;
    uniqueCellTypes: number;
    topCellType: string | null;
    topCellTypeEvidence: number;
  };
  cellTypeResults: Array<{
    cellType: string;
    evidenceCount: number;
    peakCount: number;
    geneCount: number;
    datasetCount: number;
    geneDetails: Array<{ gene: string; count: number }>;
  }>;
  pairs: Array<{
    peakName: string;
    chromosome: string;
    peakStart: number;
    peakEnd: number;
    geneName: string;
    cellType: string | null;
    contextLabel: string | null;
    clusterLabel: string | null;
    datasetId: string;
    linkScore: number | null;
    linkFdr: number | null;
    hasMarkerPeak: boolean;
    hasMarkerGene: boolean;
    signalType: string | null;
    geneMarkerTypes: string[];
  }>;
  networkData: {
    nodes: Array<{ id: string; name: string; category: string; value: number }>;
    edges: Array<{ source: string; target: string; weight: number | null; evidenceCount: number }>;
    peakLimitPerGene: number;
  };
}

export type PeakGeneContextJobStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface PeakGeneContextJobResponse {
  jobId: string;
  status: PeakGeneContextJobStatus;
  progress: number;
  stage: string;
  message: string;
  error: string | null;
  pollAfterMs: number;
  createdAt: string;
  updatedAt: string;
  result: PeakGeneContextResponse | null;
}

export async function fetchPeakGeneContextTissues(): Promise<string[]> {
  const { data } = await axios.get<string[]>(
    buildApiUrl("api/analysis/peak-gene-context/tissues")
  );
  return data;
}

export async function fetchPeakGeneContextDatasets(
  tissue: string
): Promise<PeakGeneContextDatasetOption[]> {
  const { data } = await axios.get<PeakGeneContextDatasetOption[]>(
    buildApiUrl("api/analysis/peak-gene-context/datasets"),
    { params: { tissue } }
  );
  return data;
}

export async function runPeakGeneContextAnalysis(
  request: PeakGeneContextRequest
): Promise<PeakGeneContextResponse> {
  const { data } = await axios.post<PeakGeneContextResponse>(
    buildApiUrl("api/analysis/peak-gene-context"),
    request
  );
  return data;
}

export async function submitPeakGeneContextJob(
  request: PeakGeneContextRequest
): Promise<PeakGeneContextJobResponse> {
  const { data } = await axios.post<PeakGeneContextJobResponse>(
    buildApiUrl("api/analysis/peak-gene-context/jobs"),
    request
  );
  return data;
}

export async function fetchPeakGeneContextJob(jobId: string): Promise<PeakGeneContextJobResponse> {
  const { data } = await axios.get<PeakGeneContextJobResponse>(
    buildApiUrl(`api/analysis/peak-gene-context/jobs/${encodeURIComponent(jobId)}`)
  );
  return data;
}

// =============================================================================
// Gene Search
// =============================================================================

export interface GeneSearchRequest {
  genes: string[];
  sortBy?: string;
  matchMode?: string;
  resultSize?: number;
  domain?: string;
  signalType?: string;
  tissue?: string;
}

export interface GeneSearchResponse {
  summary: {
    matchedSamples: number;
    markerGeneEvidence: number;
    linkedPeaks: number;
  };
  samples: Array<{
    sampleId: string;
    sampleName: string;
    tissue: string;
    cellContext: string;
    cellCount?: number;
    disease?: string;
    platform?: string;
    sourceId?: string;
    sampleSource?: string;
    matchedGenes: number;
    linkedPeaks: number;
    hasAtac: boolean;
    hasRna: boolean;
  }>;
}

export async function runGeneSearch(request: GeneSearchRequest): Promise<GeneSearchResponse> {
  const { data } = await axios.post<GeneSearchResponse>(buildApiUrl("api/search/gene"), request);
  return data;
}

export interface PeakSearchRequest {
  regions: Array<{ chrom: string; start: number; end: number }>;
  matchMode?: "any" | "all";
  domain?: "integration" | "atac";
  datasetId: string;
}

export interface PeakSearchResponse {
  summary: {
    inputRegions: number;
    matchedSamples: number;
    matchedInputRegions: number;
    overlappingPeaks: number;
    linkedGenes: number;
  };
  samples: Array<{
    sampleId: string;
    sampleName: string;
    tissue: string;
    cellContext: string;
    cellCount?: number;
    platform?: string;
    sourceId?: string;
    disease?: string;
    sampleSource?: string;
    matchedRegions: number;
    overlappingPeaks: number;
    linkedGenes: number;
    hasAtac: boolean;
    hasP2g: boolean;
  }>;
}

export async function runPeakSearch(request: PeakSearchRequest): Promise<PeakSearchResponse> {
  const { data } = await axios.post<PeakSearchResponse>(buildApiUrl("api/search/peak"), request);
  return data;
}

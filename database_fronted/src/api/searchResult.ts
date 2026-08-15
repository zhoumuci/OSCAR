import axios from "axios";
import { buildApiUrl } from "@/config/api";

export type SearchResultColorBy = "celltype" | "cluster";
export type SearchResultEmbedding = "umap" | "tsne";
export type SearchResultGroupBy = SearchResultColorBy;
export type SearchResultDomain = "integration" | "rna" | "atac";
export type RegulatoryAnnotationType = "marker_gene" | "marker_peak" | "linked_region";
export type RegulatoryAnnotationRegionType =
  | "all"
  | "promoter"
  | "enhancer"
  | "linked_peak"
  | "super_enhancer";
export type RegulatoryNetworkMode = "gene" | "peak";
export type RegulatoryNetworkNodeType = "gene" | "peak";
export type RegulatoryFeatureType = "gene" | "peak";
export type BedtoolsAnnotationType =
  | "marker_peak"
  | "p2g_link"
  | "gene"
  | "transcript"
  | "tss_promoter"
  | "tf_annotation"
  | "common_snp"
  | "risk_snp"
  | "gtex_eqtl"
  | "tfbs"
  | "enhancer"
  | "super_enhancer"
  | "methylation"
  | "crispr"
  | "atac_peaks"
  | "3d_interactions"
  | "dnase_peaks"
  | "tad"
  | "erna"
  | "tf_chip_seq"
  | "tcof";
export type BedtoolsSourceScope = "sample" | "reference" | "future" | string;

export interface SearchResultOverviewData {
  datasetId: string;
  domain?: string;
  tissue?: string;
  disease?: string;
  sampleNumber?: number;
  pmid?: string;
  downloadUrl?: string;
  species?: string;
  sampleType?: string;
  sampleName?: string;
  platform?: string;
  sourceId?: string;
  sampleSource?: string;
  cells?: number;
}

export interface CellTypeCompositionItem {
  label: string;
  count: number;
  ratio: number;
}

export interface CellTypeCompositionData {
  datasetId: string;
  groupBy: SearchResultGroupBy;
  items: CellTypeCompositionItem[];
}

export interface QcDensityPoint {
  x: number;
  y: number;
}

export interface QcViolinGroup {
  label: string;
  count: number;
  min?: number;
  q1?: number;
  median?: number;
  q3?: number;
  max?: number;
  density?: QcDensityPoint[];
  values?: number[];
}

export interface QcViolinMetric {
  metric: string;
  label: string;
  groups: QcViolinGroup[];
}

export interface QcViolinData {
  datasetId: string;
  groupBy: SearchResultGroupBy;
  metrics: QcViolinMetric[];
}

export interface UmapPoint {
  barcode: string;
  x: number;
  y: number;
  label: string;
  celltype?: string;
  cluster?: string | number;
}

export interface UmapData {
  datasetId: string;
  domain?: SearchResultDomain;
  embedding?: SearchResultEmbedding;
  colorBy: SearchResultColorBy;
  total: number;
  returned: number;
  points: UmapPoint[];
}

export type SearchResultUmapResponse = UmapData;

export interface MarkerGeneRecord {
  gene?: string;
  group?: string;
  logFc?: number | null;
  avgExpression?: number | null;
  pctExpressed?: number | null;
  adjustedPValue?: number | null;
  source?: string;
}

export interface MarkerGenesResponse {
  records: MarkerGeneRecord[];
  total: number;
  page: number;
  size: number;
}


export interface RegulatoryAnnotationRecord {
  id: string;
  annotationType: RegulatoryAnnotationType;
  datasetId?: string;
  domain?: SearchResultDomain | string;
  context?: string;
  cellType?: string;
  clusterLabel?: string;
  source?: string;
  evidence?: string;
  targetGene?: string;
  geneSymbol?: string;
  geneId?: string;
  geneChromosome?: string;
  geneStart?: number;
  geneEnd?: number;
  geneRegion?: string;
  strand?: string;
  promoterRegion?: string;
  geneLog2fc?: number;
  geneFdr?: number;
  geneMeanDiff?: number;
  peakName?: string;
  peakChromosome?: string;
  peakStart?: number;
  peakEnd?: number;
  peakRegion?: string;
  peakLog2fc?: number;
  peakFdr?: number;
  peakMeanDiff?: number;
  linkedPeak?: string;
  linkedGene?: string;
  linkScore?: number;
  correlation?: number;
  linkFdr?: number;
  varQrna?: number;
  varQatac?: number;
  signalType?: string;
  distance?: number;
  regionType?: RegulatoryAnnotationRegionType | string;
  regulatoryRegion?: string;
  tf?: string;
  tfName?: string;
  tfAnnotation?: string;
  motifName?: string;
  motifId?: string;
  motifSource?: string;
  motifLogoUrl?: string;
  tfbsRegion?: string;
  genie3Weight?: number;
  nes?: number;
  promoterTf?: string;
  seTf?: string;
  teTf?: string;
  // Temporary legacy fields accepted from the current backend shape.
  chromosome?: string;
  start?: number;
  end?: number;
  region?: string;
  peak?: string;
  peakId?: string;
  gene?: string;
  logFc?: number | null;
  log2fc?: number | null;
  fdr?: number | null;
  meanDiff?: number | null;
  adjustedPValue?: number | null;
}

export interface RegulatoryAnnotationQuery {
  datasetId: string;
  domain: SearchResultDomain;
  annotationType: RegulatoryAnnotationType;
  page?: number;
  pageSize?: number;
  targetGene?: string;
  peak?: string;
  regionType?: RegulatoryAnnotationRegionType;
  contextCellType?: string;
  contextCluster?: string;
  maxFdr?: number;
  minLog2fc?: number;
  minP2gScore?: number;
  signalType?: string;
  sortBy?: string;
  sortOrder?: "asc" | "desc";
  p2gMode?: "marker" | "all";
}

export interface RegulatoryTfSummaryQuery {
  datasetId: string;
  domain: SearchResultDomain | string;
  featureType: RegulatoryFeatureType;
  gene?: string;
  chrom?: string;
  start?: number;
  end?: number;
  peakId?: string;
}

export interface RegulatoryTfSummary {
  datasetId: string;
  domain?: string;
  featureType: RegulatoryFeatureType | string;
  featureId?: string;
  available: boolean;
  status: string;
  tf: unknown | null;
  reason?: string;
}

export interface RegulatoryAnnotationContextOption {
  label: string;
  value: string;
  cellType?: string;
  cluster?: string;
  count?: number;
}

export interface RegulatoryAnnotationResponse {
  total: number;
  page: number;
  pageSize: number;
  items: RegulatoryAnnotationRecord[];
}

export interface RegulatoryNetworkNode {
  id: string;
  type: RegulatoryNetworkNodeType | string;
  label: string;
  datasetId?: string;
  dataset_id?: string;
  sampleName?: string;
  sample_name?: string;
  domain?: string;
  chromosome?: string;
  start?: number;
  end?: number;
  fdr?: number | null;
  varQAtac?: number | null;
  varQRna?: number | null;
  linkedGenesCount?: number | null;
  linked_genes_count?: number | null;
  topLinkedGenes?: string[] | string;
  top_linked_genes?: string[] | string;
  remainingLinkedGenesCount?: number | null;
  remaining_linked_genes_count?: number | null;
  linkedPeaksCount?: number | null;
  linked_peaks_count?: number | null;
  topLinkedPeaks?: string[] | string;
  top_linked_peaks?: string[] | string;
  remainingLinkedPeaksCount?: number | null;
  remaining_linked_peaks_count?: number | null;
  maxLinkScore?: number | null;
  max_link_score?: number | null;
  correlationMin?: number | null;
  correlation_min?: number | null;
  correlationMax?: number | null;
  correlation_max?: number | null;
  fdrMin?: number | null;
  fdr_min?: number | null;
  fdrMax?: number | null;
  fdr_max?: number | null;
  minFdr?: number | null;
  min_fdr?: number | null;
  totalLinks?: number | null;
  total_links?: number | null;
  source?: string;
}

export interface RegulatoryNetworkEdge {
  source: string;
  target: string;
  type: string;
  score?: number | null;
  distanceToTss?: number | null;
  correlation?: number | null;
  fdr?: number | null;
  varQAtac?: number | null;
  varQRna?: number | null;
  sourceMethod?: string;
}

export interface RegulatoryNetworkLink {
  peak?: string;
  linkedGene?: string;
  datasetId?: string;
  dataset_id?: string;
  sampleName?: string;
  sample_name?: string;
  domain?: string;
  distanceToTss?: number | null;
  linkScore?: number | null;
  score?: number | null;
  correlation?: number | null;
  fdr?: number | null;
  varQAtac?: number | null;
  varQRna?: number | null;
  peakId?: string;
  geneSymbol?: string;
  linkType?: string;
  source?: string;
  linkedGenesCount?: number | null;
  linked_genes_count?: number | null;
  topLinkedGenes?: string[] | string;
  top_linked_genes?: string[] | string;
  remainingLinkedGenesCount?: number | null;
  remaining_linked_genes_count?: number | null;
  linkedPeaksCount?: number | null;
  linked_peaks_count?: number | null;
  topLinkedPeaks?: string[] | string;
  top_linked_peaks?: string[] | string;
  remainingLinkedPeaksCount?: number | null;
  remaining_linked_peaks_count?: number | null;
  maxLinkScore?: number | null;
  max_link_score?: number | null;
  correlationMin?: number | null;
  correlation_min?: number | null;
  correlationMax?: number | null;
  correlation_max?: number | null;
  fdrMin?: number | null;
  fdr_min?: number | null;
  fdrMax?: number | null;
  fdr_max?: number | null;
  minFdr?: number | null;
  min_fdr?: number | null;
  totalLinks?: number | null;
  total_links?: number | null;
}

export interface RegulatoryNetworkResponse {
  nodes: RegulatoryNetworkNode[];
  edges: RegulatoryNetworkEdge[];
  links: RegulatoryNetworkLink[];
  totalLinks?: number;
  hasMore?: boolean;
  datasetId?: string;
  dataset_id?: string;
  sampleName?: string;
  sample_name?: string;
  domain?: string;
}

export interface RegulatoryNetworkQuery {
  datasetId: string;
  domain: SearchResultDomain;
  mode: RegulatoryNetworkMode;
  gene?: string;
  peak?: string;
  minScore?: number;
  maxNodes?: number;
  maxEdges?: number;
}

export interface RegulatoryNetworkExpandQuery {
  datasetId: string;
  domain: SearchResultDomain;
  nodeId: string;
  nodeType: RegulatoryNetworkNodeType;
  gene?: string;
  peak?: string;
  minScore?: number;
  maxNeighbors?: number;
}

export interface RegulatoryNetworkExpansionResponse extends RegulatoryNetworkResponse {
  totalNeighbors?: number;
}

export interface RegulatoryNetworkLinksQuery {
  datasetId: string;
  domain: SearchResultDomain;
  nodeType: RegulatoryNetworkNodeType;
  nodeId: string;
  page?: number;
  pageSize?: number;
  gene?: string;
  peak?: string;
  minScore?: number;
}

export interface RegulatoryNetworkLinksResponse {
  total: number;
  page: number;
  pageSize: number;
  items: RegulatoryNetworkLink[];
}

export interface BedtoolsSourceOption {
  type: BedtoolsAnnotationType | string;
  label: string;
  scope: BedtoolsSourceScope;
  available: boolean;
  status: string;
  reason?: string | null;
  description?: string | null;
}

export interface BedtoolsSourcesResponse {
  datasetId: string;
  domain: SearchResultDomain | string;
  genomeBuild: string;
  coordinateSystem: string;
  sources: BedtoolsSourceOption[];
}

export interface BedtoolsIntersectRequest {
  datasetId: string;
  domain: SearchResultDomain;
  genomeBuild?: string;
  region: string;
  annotationTypes: Array<BedtoolsAnnotationType | string>;
  minOverlapBp?: number;
  page?: number;
  pageSize?: number;
}

export interface BedtoolsQueryRegion {
  raw?: string;
  chrom?: string;
  start?: number;
  end?: number;
}

export interface BedtoolsIntersectSummary {
  totalHits?: number;
  byAnnotationType?: Record<string, number>;
  elapsedMillis?: number;
}

export interface BedtoolsOverlapRecord {
  annotationType?: string;
  annotationLabel?: string;
  scope?: string;
  featureId?: string;
  featureRegion?: string;
  featureName?: string;
  chrom?: string;
  chromosome?: string;
  start?: number;
  end?: number;
  overlapBp?: number;
  overlap?: number;
  overlap_bp?: number;
  gene?: string;
  geneName?: string;
  gene_id?: string;
  transcriptId?: string;
  transcript_id?: string;
  strand?: string;
  cellType?: string;
  cluster?: string;
  cellCluster?: string;
  score?: number | string | null;
  evidence?: string;
  sample?: string;
  queryRegion?: string;
  queryEnhancerRegion?: string;
  [key: string]: unknown;
}

export interface BedtoolsIntersectResponse {
  status?: string;
  message?: string;
  datasetId?: string;
  domain?: SearchResultDomain | string;
  genomeBuild?: string;
  coordinateSystem?: string;
  queryRegion?: BedtoolsQueryRegion;
  selectedAnnotationTypes?: string[];
  page?: number;
  pageSize?: number;
  total?: number;
  summary?: BedtoolsIntersectSummary;
  records?: BedtoolsOverlapRecord[];
  warnings?: string[];
}

type RegulatoryNetworkLinksPayload = {
  total?: number;
  page?: number;
  pageSize?: number;
  page_size?: number;
  size?: number;
  items?: RegulatoryNetworkLink[];
  records?: RegulatoryNetworkLink[];
};

type RegulatoryAnnotationPayload = {
  total?: number;
  page?: number;
  pageSize?: number;
  page_size?: number;
  size?: number;
  items?: RegulatoryAnnotationRawRecord[];
  records?: RegulatoryAnnotationRawRecord[];
};

type RegulatoryAnnotationRawRecord = Partial<Omit<RegulatoryAnnotationRecord, "id" | "annotationType">> & {
  id?: number | string;
  _id?: number | string;
  annotationType?: RegulatoryAnnotationType | string;
  annotation_type?: RegulatoryAnnotationType | string;
  annotation?: RegulatoryAnnotationType | string;
  dataset_id?: string;
  target_gene?: string;
  gene_symbol?: string;
  gene_id?: string;
  gene_chromosome?: string;
  gene_start?: number | string | null;
  gene_end?: number | string | null;
  gene_region?: string;
  promoter_region?: string;
  gene_log2fc?: number | string | null;
  gene_fdr?: number | string | null;
  gene_mean_diff?: number | string | null;
  avg_log2FC?: number | string | null;
  avg_log2fc?: number | string | null;
  p_val_adj?: number | string | null;
  peak_name?: string;
  peak_chromosome?: string;
  peak_start?: number | string | null;
  peak_end?: number | string | null;
  peak_region?: string;
  peak_log2fc?: number | string | null;
  peak_fdr?: number | string | null;
  peak_mean_diff?: number | string | null;
  linked_peak?: string;
  linked_gene?: string;
  link_score?: number | string | null;
  link_fdr?: number | string | null;
  distance_to_tss?: number | string | null;
  region_type?: string;
  regulatory_region?: string;
  cell_type?: string;
  cluster_label?: string;
  motif_name?: string;
  motif_id?: string;
  motif_source?: string;
  motif_logo_url?: string;
  tfbs_region?: string;
  genie3_weight?: number | string | null;
  promoter_tf?: string;
  se_tf?: string;
  te_tf?: string;
  logFC?: number | string | null;
  logFc?: number | string | null;
  log2fc?: number | string | null;
  fdr?: number | string | null;
  meanDiff?: number | string | null;
  mean_diff?: number | string | null;
  adjustedPValue?: number | string | null;
  score?: number | string | null;
  correlation?: number | string | null;
  nes?: number | string | null;
  weight?: number | string | null;
};

interface BackendEnvelope<T> {
  code?: number;
  message?: string;
  data?: T;
}

const DEFAULT_QC_METRICS = ["TSSEnrichment", "nFrags", "Gex_nGenes", "Gex_MitoRatio"] as const;
const DEFAULT_UMAP_MAX_POINTS = 4000;

type CellTypeCompositionQuery = {
  datasetId: string;
  domain: SearchResultDomain;
  groupBy?: SearchResultGroupBy;
};

type QcViolinQuery = {
  datasetId: string;
  domain: SearchResultDomain;
  groupBy?: SearchResultGroupBy;
  metrics?: readonly string[];
};

export type SearchResultUmapRequest = {
  datasetId: string;
  domain: SearchResultDomain;
  embedding?: SearchResultEmbedding;
  colorBy?: SearchResultColorBy;
  maxPoints?: number;
};

function unwrapSearchResultResponse<T>(payload: T | BackendEnvelope<T>, endpointName: string): T {
  if (payload && typeof payload === "object" && "code" in payload) {
    const envelope = payload as BackendEnvelope<T>;

    if (envelope.code !== 200) {
      throw new Error(
        `${endpointName} returned unsuccessful code ${envelope.code}: ${envelope.message ?? ""}`
      );
    }

    if (envelope.data === undefined || envelope.data === null) {
      throw new Error(`${endpointName} returned no data.`);
    }

    return envelope.data;
  }

  return payload as T;
}

function samplePath(datasetId: string, resource: string): string {
  return `api/samples/${encodeURIComponent(datasetId)}/${resource}`;
}

function searchResultPath(datasetId: string, resource: string): string {
  return `api/search-result/${encodeURIComponent(datasetId)}/${resource}`;
}

function normalizeRegulatoryNetworkLinksResponse(
  payload: RegulatoryNetworkLinksPayload,
  fallbackPage: number,
  fallbackPageSize: number
): RegulatoryNetworkLinksResponse {
  const items = payload.items ?? payload.records ?? [];
  const total = typeof payload.total === "number" && Number.isFinite(payload.total)
    ? payload.total
    : items.length;
  const page = typeof payload.page === "number" && Number.isFinite(payload.page)
    ? payload.page
    : fallbackPage;
  const pageSizeValue = payload.pageSize ?? payload.page_size ?? payload.size;
  const pageSize = typeof pageSizeValue === "number" && Number.isFinite(pageSizeValue)
    ? pageSizeValue
    : fallbackPageSize;

  return {
    total,
    page,
    pageSize,
    items,
  };
}

function firstString(record: RegulatoryAnnotationRawRecord, keys: string[]): string | undefined {
  const source = record as Record<string, unknown>;

  for (const key of keys) {
    const value = source[key];
    if (value === undefined || value === null) continue;
    const text = String(value).trim();
    if (text) return text;
  }

  return undefined;
}

function firstNumber(record: RegulatoryAnnotationRawRecord, keys: string[]): number | undefined {
  const source = record as Record<string, unknown>;

  for (const key of keys) {
    const value = source[key];
    if (value === undefined || value === null || value === "") continue;
    const numericValue = typeof value === "number"
      ? value
      : Number(String(value).replace(/,/g, "").trim());
    if (Number.isFinite(numericValue)) return numericValue;
  }

  return undefined;
}

function coordinateRegion(
  chromosome: string | undefined,
  start: number | undefined,
  end: number | undefined
): string | undefined {
  if (!chromosome || start === undefined || end === undefined) return undefined;
  return `${chromosome}:${start}-${end}`;
}

function normalizeAnnotationType(value: unknown): RegulatoryAnnotationType | null {
  const normalized = String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");

  const aliases: Record<string, RegulatoryAnnotationType> = {
    gene: "marker_gene",
    marker_gene: "marker_gene",
    marker_genes: "marker_gene",
    marker_peak: "marker_peak",
    marker_peaks: "marker_peak",
    peak: "marker_peak",
    peaks: "marker_peak",
    p2g: "linked_region",
    p2g_link: "linked_region",
    p2g_links: "linked_region",
    linked_region: "linked_region",
    linked_regions: "linked_region",
    linked_peak: "linked_region",
    enhancer: "linked_region",
  };

  return aliases[normalized] ?? null;
}

function normalizeRegionType(value: unknown): RegulatoryAnnotationRegionType | string | undefined {
  const normalized = String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");

  if (!normalized) return undefined;
  if (normalized === "se" || normalized === "superenhancer") return "super_enhancer";
  if (normalized === "te" || normalized === "typical_enhancer") return "enhancer";
  return normalized;
}

function inferAnnotationType(
  record: RegulatoryAnnotationRawRecord,
  fallbackAnnotationType: RegulatoryAnnotationType
): RegulatoryAnnotationType {
  const explicitType = normalizeAnnotationType(
    firstString(record, ["annotationType", "annotation_type", "annotation"])
  );
  if (explicitType) return explicitType;

  const regionType = normalizeRegionType(firstString(record, ["regionType", "region_type", "linkType"]));
  if (regionType === "promoter") return "marker_gene";

  const hasLinkEvidence = firstString(record, ["linkedPeak", "linked_peak", "linkedGene", "linked_gene"]) ||
    firstNumber(record, ["linkScore", "link_score", "correlation", "score"]);
  if (hasLinkEvidence) return "linked_region";

  if (regionType === "enhancer" || regionType === "linked_peak" || regionType === "super_enhancer") {
    return "linked_region";
  }

  const hasPeakEvidence = firstString(record, [
    "peakName",
    "peak_name",
    "peakRegion",
    "peak_region",
    "peak",
    "peakId",
    "peak_id",
  ]);
  if (hasPeakEvidence) return "marker_peak";

  return fallbackAnnotationType;
}

export function normalizeRegulatoryAnnotationRecord(
  record: unknown,
  fallbackAnnotationType: RegulatoryAnnotationType = "linked_region",
  index = 0
): RegulatoryAnnotationRecord {
  const raw: RegulatoryAnnotationRawRecord =
    record && typeof record === "object" ? record as RegulatoryAnnotationRawRecord : {};
  const annotationType = inferAnnotationType(raw, fallbackAnnotationType);
  const regionType = normalizeRegionType(firstString(raw, ["regionType", "region_type", "linkType"]));
  const legacyChromosome = firstString(raw, ["chromosome"]);
  const legacyStart = firstNumber(raw, ["start"]);
  const legacyEnd = firstNumber(raw, ["end"]);
  const legacyRegion = firstString(raw, ["region"]);
  const legacyCoordinateRegion = coordinateRegion(legacyChromosome, legacyStart, legacyEnd);
  const isPromoterRegion = regionType === "promoter";
  const isLinkedRegionType = regionType === "enhancer" ||
    regionType === "linked_peak" ||
    regionType === "super_enhancer";
  const targetGene = firstString(raw, [
    "targetGene",
    "target_gene",
    "geneSymbol",
    "gene_symbol",
    "linkedGene",
    "linked_gene",
    "gene",
  ]);
  const geneChromosome = firstString(raw, ["geneChromosome", "gene_chromosome"]) ||
    (isPromoterRegion ? legacyChromosome : undefined);
  let geneStart = firstNumber(raw, ["geneStart", "gene_start"]) ||
    (isPromoterRegion ? legacyStart : undefined);
  let geneEnd = firstNumber(raw, ["geneEnd", "gene_end"]) ||
    (isPromoterRegion ? legacyEnd : undefined);
  // Normalize: ensure gene start < end regardless of how the DB stored it
  if (geneStart !== undefined && geneEnd !== undefined && geneStart > geneEnd) {
    const tmp = geneStart; geneStart = geneEnd; geneEnd = tmp;
  }
  const geneRegion = firstString(raw, ["geneRegion", "gene_region"]) ||
    coordinateRegion(geneChromosome, geneStart, geneEnd) ||
    (isPromoterRegion ? legacyRegion || legacyCoordinateRegion : undefined);
  const promoterRegion = firstString(raw, ["promoterRegion", "promoter_region"]) ||
    (isPromoterRegion ? legacyRegion || geneRegion : undefined);
  const peakChromosome = firstString(raw, ["peakChromosome", "peak_chromosome"]) ||
    (!isPromoterRegion ? legacyChromosome : undefined);
  let peakStart = firstNumber(raw, ["peakStart", "peak_start"]) ||
    (!isPromoterRegion ? legacyStart : undefined);
  let peakEnd = firstNumber(raw, ["peakEnd", "peak_end"]) ||
    (!isPromoterRegion ? legacyEnd : undefined);
  // Normalize: ensure peak start < end regardless of how the DB stored it
  if (peakStart !== undefined && peakEnd !== undefined && peakStart > peakEnd) {
    const tmp = peakStart; peakStart = peakEnd; peakEnd = tmp;
  }
  const peakRegion = firstString(raw, ["peakRegion", "peak_region"]) ||
    (!isPromoterRegion ? legacyRegion || legacyCoordinateRegion : undefined);
  const regulatoryRegion = firstString(raw, ["regulatoryRegion", "regulatory_region"]) ||
    (isLinkedRegionType ? legacyRegion || peakRegion || legacyCoordinateRegion : undefined);
  const peakName = firstString(raw, ["peakName", "peak_name", "peak", "peakId", "peak_id"]) ||
    peakRegion ||
    regulatoryRegion;
  const linkedPeak = firstString(raw, ["linkedPeak", "linked_peak"]) ||
    (annotationType === "linked_region" ? peakName || regulatoryRegion : undefined);
  const linkedGene = firstString(raw, ["linkedGene", "linked_gene"]) || targetGene;
  const geneMetricKeys = annotationType === "marker_gene"
    ? ["geneLog2fc", "gene_log2fc", "log2fc", "logFC", "logFc", "avg_log2FC", "avg_log2fc"]
    : ["geneLog2fc", "gene_log2fc"];
  const geneFdrKeys = annotationType === "marker_gene"
    ? ["geneFdr", "gene_fdr", "fdr", "p_val_adj", "adjustedPValue"]
    : ["geneFdr", "gene_fdr"];
  const geneMeanKeys = annotationType === "marker_gene"
    ? ["geneMeanDiff", "gene_mean_diff", "meanDiff", "mean_diff"]
    : ["geneMeanDiff", "gene_mean_diff"];
  const peakMetricKeys = annotationType === "marker_peak" || annotationType === "linked_region"
    ? ["peakLog2fc", "peak_log2fc", "log2fc", "logFC", "logFc", "avg_log2FC", "avg_log2fc"]
    : ["peakLog2fc", "peak_log2fc"];
  const peakFdrKeys = annotationType === "marker_peak" || annotationType === "linked_region"
    ? ["peakFdr", "peak_fdr", "fdr", "p_val_adj", "adjustedPValue"]
    : ["peakFdr", "peak_fdr"];
  const peakMeanKeys = annotationType === "marker_peak" || annotationType === "linked_region"
    ? ["peakMeanDiff", "peak_mean_diff", "meanDiff", "mean_diff"]
    : ["peakMeanDiff", "peak_mean_diff"];
  const id = firstString(raw, ["id", "_id"]) ||
    `${annotationType}-${targetGene || linkedGene || peakName || regulatoryRegion || "record"}-${index + 1}`;

  return {
    id,
    annotationType,
    datasetId: firstString(raw, ["datasetId", "dataset_id"]),
    domain: firstString(raw, ["domain"]) as SearchResultDomain | string | undefined,
    context: firstString(raw, ["context", "group", "cellTypeGroup", "clusterGroup"]),
    cellType: firstString(raw, ["cellType", "cell_type"]),
    clusterLabel: firstString(raw, ["clusterLabel", "cluster_label"]),
    source: firstString(raw, ["source", "motifSource", "motif_source"]),
    evidence: firstString(raw, ["evidence", "sourceMethod", "source_method"]),
    targetGene,
    geneSymbol: firstString(raw, ["geneSymbol", "gene_symbol"]) || targetGene,
    geneId: firstString(raw, ["geneId", "gene_id"]),
    geneChromosome,
    geneStart,
    geneEnd,
    geneRegion,
    strand: firstString(raw, ["strand"]),
    promoterRegion,
    geneLog2fc: firstNumber(raw, geneMetricKeys),
    geneFdr: firstNumber(raw, geneFdrKeys),
    geneMeanDiff: firstNumber(raw, geneMeanKeys),
    peakName,
    peakChromosome,
    peakStart,
    peakEnd,
    peakRegion,
    peakLog2fc: firstNumber(raw, peakMetricKeys),
    peakFdr: firstNumber(raw, peakFdrKeys),
    peakMeanDiff: firstNumber(raw, peakMeanKeys),
    linkedPeak,
    linkedGene,
    linkScore: firstNumber(raw, ["linkScore", "link_score", "score"]),
    correlation: firstNumber(raw, ["correlation"]),
    linkFdr: firstNumber(raw, ["linkFdr", "link_fdr"]),
    varQrna: firstNumber(raw, ["varQrna", "var_qrna", "varQRna"]),
    varQatac: firstNumber(raw, ["varQatac", "var_qatac", "varQatac"]),
    distance: firstNumber(raw, ["distance", "distanceToTss", "distance_to_tss"]),
    regionType,
    regulatoryRegion,
    tf: firstString(raw, ["tf", "tfName", "tf_name", "tfAnnotation", "tf_annotation", "transcriptionFactor", "transcription_factor"]),
    tfName: firstString(raw, ["tfName", "tf_name"]),
    tfAnnotation: firstString(raw, ["tfAnnotation", "tf_annotation"]),
    motifName: firstString(raw, ["motifName", "motif_name"]),
    motifId: firstString(raw, ["motifId", "motif_id"]),
    motifSource: firstString(raw, ["motifSource", "motif_source"]),
    motifLogoUrl: firstString(raw, ["motifLogoUrl", "motif_logo_url"]),
    tfbsRegion: firstString(raw, ["tfbsRegion", "tfbs_region"]),
    genie3Weight: firstNumber(raw, ["genie3Weight", "genie3_weight", "weight"]),
    nes: firstNumber(raw, ["nes"]),
    promoterTf: firstString(raw, ["promoterTf", "promoter_tf"]),
    seTf: firstString(raw, ["seTf", "se_tf"]),
    teTf: firstString(raw, ["teTf", "te_tf"]),
    chromosome: legacyChromosome,
    start: legacyStart,
    end: legacyEnd,
    region: legacyRegion,
    peak: firstString(raw, ["peak"]),
    peakId: firstString(raw, ["peakId", "peak_id"]),
    gene: firstString(raw, ["gene"]),
    signalType: firstString(raw, ["signalType", "signal_type"]),
    logFc: firstNumber(raw, ["logFc", "logFC"]),
    log2fc: firstNumber(raw, ["log2fc"]),
    fdr: firstNumber(raw, ["fdr"]),
    meanDiff: firstNumber(raw, ["meanDiff", "mean_diff"]),
    adjustedPValue: firstNumber(raw, ["adjustedPValue", "p_val_adj"]),
  };
}

function normalizeRegulatoryAnnotationResponse(
  payload: RegulatoryAnnotationPayload,
  fallbackPage: number,
  fallbackPageSize: number,
  fallbackAnnotationType: RegulatoryAnnotationType
): RegulatoryAnnotationResponse {
  const items = payload.items ?? payload.records ?? [];
  const total = typeof payload.total === "number" && Number.isFinite(payload.total)
    ? payload.total
    : items.length;
  const page = typeof payload.page === "number" && Number.isFinite(payload.page)
    ? payload.page
    : fallbackPage;
  const pageSizeValue = payload.pageSize ?? payload.page_size ?? payload.size;
  const pageSize = typeof pageSizeValue === "number" && Number.isFinite(pageSizeValue)
    ? pageSizeValue
    : fallbackPageSize;

  return {
    total,
    page,
    pageSize,
    items: items.map((item, index) => normalizeRegulatoryAnnotationRecord(item, fallbackAnnotationType, index)),
  };
}

export function isSearchResultEndpointUnavailable(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 404;
}

export async function fetchSearchResultOverview(datasetId: string): Promise<SearchResultOverviewData> {
  const { data } = await axios.get<SearchResultOverviewData | BackendEnvelope<SearchResultOverviewData>>(
    buildApiUrl("api/search-result/overview"),
    { params: { datasetId } }
  );

  return unwrapSearchResultResponse(data, "GET /api/search-result/overview");
}

export async function fetchCellTypeComposition({
  datasetId,
  domain,
  groupBy = "celltype",
}: CellTypeCompositionQuery): Promise<CellTypeCompositionData> {
  const { data } = await axios.get<CellTypeCompositionData | BackendEnvelope<CellTypeCompositionData>>(
    buildApiUrl("api/search-result/celltype-composition"),
    { params: { datasetId, domain, groupBy } }
  );

  return unwrapSearchResultResponse(data, "GET /api/search-result/celltype-composition");
}

export async function fetchQcViolin({
  datasetId,
  domain,
  groupBy = "celltype",
  metrics = DEFAULT_QC_METRICS,
}: QcViolinQuery): Promise<QcViolinData> {
  const { data } = await axios.get<QcViolinData | BackendEnvelope<QcViolinData>>(
    buildApiUrl("api/search-result/qc-violin"),
    {
      params: {
        datasetId,
        domain,
        groupBy,
        metrics: metrics.join(","),
      },
    }
  );

  return unwrapSearchResultResponse(data, "GET /api/search-result/qc-violin");
}

export async function fetchUmap({
  datasetId,
  domain,
  embedding = "umap",
  colorBy = "celltype",
  maxPoints = DEFAULT_UMAP_MAX_POINTS,
}: SearchResultUmapRequest): Promise<SearchResultUmapResponse> {
  const { data } = await axios.get<UmapData | BackendEnvelope<UmapData>>(
    buildApiUrl("api/search-result/umap"),
    { params: { datasetId, domain, embedding, colorBy, maxPoints } }
  );

  return unwrapSearchResultResponse(data, "GET /api/search-result/umap");
}

export async function fetchRegulatoryAnnotations({
  datasetId,
  domain,
  annotationType,
  page = 1,
  pageSize = 10,
  targetGene,
  peak,
  regionType,
  contextCellType,
  contextCluster,
  maxFdr,
  minLog2fc,
  minP2gScore,
  signalType,
  sortBy,
  sortOrder,
  p2gMode,
}: RegulatoryAnnotationQuery): Promise<RegulatoryAnnotationResponse> {
  const { data } = await axios.get<RegulatoryAnnotationPayload | BackendEnvelope<RegulatoryAnnotationPayload>>(
    buildApiUrl(samplePath(datasetId, "regulatory-annotations")),
    {
      params: {
        domain,
        annotationType,
        page,
        pageSize,
        targetGene: targetGene?.trim() || undefined,
        peak: peak?.trim() || undefined,
        regionType: annotationType !== "linked_region" && regionType && regionType !== "all" ? regionType : undefined,
        contextCellType: contextCellType?.trim() || undefined,
        contextCluster: contextCluster?.trim() || undefined,
        maxFdr,
        minLog2fc,
        minP2gScore: annotationType === "linked_region" ? minP2gScore : undefined,
        signalType: annotationType === "marker_gene" ? signalType : undefined,
        sortBy: sortBy?.trim() || undefined,
        sortOrder: sortBy ? sortOrder : undefined,
        p2gMode: annotationType === "linked_region" ? p2gMode : undefined,
      },
    }
  );

  const payload = unwrapSearchResultResponse(data, "GET /api/samples/{datasetId}/regulatory-annotations");
  return normalizeRegulatoryAnnotationResponse(payload, page, pageSize, annotationType);
}

export async function fetchRegulatoryAnnotationsDownload({
  datasetId,
  domain,
  annotationType,
  targetGene,
  peak,
  regionType,
  contextCellType,
  contextCluster,
  maxFdr,
  minLog2fc,
  minP2gScore,
  signalType,
  sortBy,
  sortOrder,
  p2gMode,
  sampleLabel,
}: Omit<RegulatoryAnnotationQuery, "page" | "pageSize"> & { sampleLabel?: string }): Promise<{ blob: Blob; filename?: string }> {
  const response = await axios.get<Blob>(
    buildApiUrl(samplePath(datasetId, "regulatory-annotations/download.csv")),
    {
      responseType: "blob",
      params: {
        domain,
        annotationType,
        targetGene: targetGene?.trim() || undefined,
        peak: peak?.trim() || undefined,
        regionType: annotationType !== "linked_region" && regionType && regionType !== "all" ? regionType : undefined,
        contextCellType: contextCellType?.trim() || undefined,
        contextCluster: contextCluster?.trim() || undefined,
        maxFdr,
        minLog2fc,
        minP2gScore: annotationType === "linked_region" ? minP2gScore : undefined,
        signalType: annotationType === "marker_gene" ? signalType : undefined,
        sortBy: sortBy?.trim() || undefined,
        sortOrder: sortBy ? sortOrder : undefined,
        p2gMode: annotationType === "linked_region" ? p2gMode : undefined,
        sampleLabel: sampleLabel?.trim() || undefined,
      },
    }
  );
  const disposition = String(response.headers["content-disposition"] ?? "");
  const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
  return {
    blob: response.data,
    filename: filenameMatch?.[1],
  };
}

export async function fetchRegulatoryAnnotationContextOptions({
  datasetId,
  domain,
  annotationType,
}: Pick<RegulatoryAnnotationQuery, "datasetId" | "domain" | "annotationType">): Promise<RegulatoryAnnotationContextOption[]> {
  const { data } = await axios.get<
    RegulatoryAnnotationContextOption[] | BackendEnvelope<RegulatoryAnnotationContextOption[]>
  >(buildApiUrl(samplePath(datasetId, "regulatory-annotations/context-options")), {
    params: {
      domain,
      annotationType,
    },
  });

  return unwrapSearchResultResponse(data, "GET /api/samples/{datasetId}/regulatory-annotations/context-options");
}

export async function fetchRegulatoryTfSummary({
  datasetId,
  domain,
  featureType,
  gene,
  chrom,
  start,
  end,
  peakId,
}: RegulatoryTfSummaryQuery): Promise<RegulatoryTfSummary> {
  const { data } = await axios.get<RegulatoryTfSummary | BackendEnvelope<RegulatoryTfSummary>>(
    buildApiUrl(searchResultPath(datasetId, "regulatory/tf-summary")),
    {
      params: {
        domain,
        featureType,
        gene: gene?.trim() || undefined,
        chrom: chrom?.trim() || undefined,
        start,
        end,
        peakId: peakId?.trim() || undefined,
      },
    }
  );

  return unwrapSearchResultResponse(data, "GET /api/search-result/{datasetId}/regulatory/tf-summary");
}

export async function fetchRegulatoryNetwork({
  datasetId,
  domain,
  mode,
  gene,
  peak,
  minScore,
  maxNodes,
  maxEdges,
}: RegulatoryNetworkQuery): Promise<RegulatoryNetworkResponse> {
  // TODO: Backend should implement GET /api/samples/{datasetId}/regulatory-network.
  const { data } = await axios.get<RegulatoryNetworkResponse | BackendEnvelope<RegulatoryNetworkResponse>>(
    buildApiUrl(samplePath(datasetId, "regulatory-network")),
    {
      params: {
        domain,
        mode,
        gene: gene?.trim() || undefined,
        peak: peak?.trim() || undefined,
        minScore,
        maxNodes,
        maxEdges,
      },
    }
  );

  return unwrapSearchResultResponse(data, "GET /api/samples/{datasetId}/regulatory-network");
}

export async function fetchRegulatoryNetworkExpansion({
  datasetId,
  domain,
  nodeId,
  nodeType,
  gene,
  peak,
  minScore,
  maxNeighbors,
}: RegulatoryNetworkExpandQuery): Promise<RegulatoryNetworkExpansionResponse> {
  // TODO: Backend should implement GET /api/samples/{datasetId}/regulatory-network/expand.
  const { data } = await axios.get<
    RegulatoryNetworkExpansionResponse | BackendEnvelope<RegulatoryNetworkExpansionResponse>
  >(buildApiUrl(samplePath(datasetId, "regulatory-network/expand")), {
    params: {
      domain,
      nodeId,
      nodeType,
      gene: gene?.trim() || undefined,
      peak: peak?.trim() || undefined,
      minScore,
      maxNeighbors,
    },
  });

  return unwrapSearchResultResponse(data, "GET /api/samples/{datasetId}/regulatory-network/expand");
}

export async function fetchRegulatoryNetworkLinks({
  datasetId,
  domain,
  nodeType,
  nodeId,
  page = 1,
  pageSize = 20,
  gene,
  peak,
  minScore,
}: RegulatoryNetworkLinksQuery): Promise<RegulatoryNetworkLinksResponse> {
  const { data } = await axios.get<RegulatoryNetworkLinksPayload | BackendEnvelope<RegulatoryNetworkLinksPayload>>(
    buildApiUrl(samplePath(datasetId, "regulatory-network/links")),
    {
      params: {
        domain,
        nodeType,
        nodeId,
        page,
        pageSize,
        gene: gene?.trim() || undefined,
        peak: peak?.trim() || undefined,
        minScore,
      },
    }
  );

  const payload = unwrapSearchResultResponse(data, "GET /api/samples/{datasetId}/regulatory-network/links");
  return normalizeRegulatoryNetworkLinksResponse(payload, page, pageSize);
}

export async function fetchBedtoolsSources({
  datasetId,
  domain,
  genomeBuild = "hg38",
}: {
  datasetId: string;
  domain: SearchResultDomain;
  genomeBuild?: string;
}): Promise<BedtoolsSourcesResponse> {
  const { data } = await axios.get<BedtoolsSourcesResponse | BackendEnvelope<BedtoolsSourcesResponse>>(
    buildApiUrl(searchResultPath(datasetId, "regulatory/bedtools/sources")),
    {
      params: {
        domain,
        genomeBuild,
      },
    }
  );

  return unwrapSearchResultResponse(data, "GET /api/search-result/{datasetId}/regulatory/bedtools/sources");
}

export async function fetchReferenceSources({
  genomeBuild = "hg38",
}: {
  genomeBuild?: string;
}): Promise<BedtoolsSourcesResponse> {
  const { data } = await axios.get<BedtoolsSourcesResponse | BackendEnvelope<BedtoolsSourcesResponse>>(
    buildApiUrl("api/feature-detail/regulatory/reference-sources"),
    {
      params: { genomeBuild },
    }
  );
  return unwrapSearchResultResponse(data, "GET /api/feature-detail/regulatory/reference-sources");
}

export async function runBedtoolsIntersect({
  datasetId,
  domain,
  genomeBuild = "hg38",
  region,
  annotationTypes,
  minOverlapBp = 1,
  page = 1,
  pageSize = 10,
}: BedtoolsIntersectRequest): Promise<BedtoolsIntersectResponse> {
  const { data } = await axios.post<BedtoolsIntersectResponse | BackendEnvelope<BedtoolsIntersectResponse>>(
    buildApiUrl(searchResultPath(datasetId, "regulatory/bedtools/intersect")),
    {
      domain,
      genomeBuild,
      region,
      annotationTypes,
      minOverlapBp,
      page,
      pageSize,
    }
  );

  return unwrapSearchResultResponse(data, "POST /api/search-result/{datasetId}/regulatory/bedtools/intersect");
}

export async function runReferenceIntersect({
  genomeBuild = "hg38",
  region,
  annotationTypes,
  minOverlapBp = 1,
  page = 1,
  pageSize = 10,
}: {
  genomeBuild?: string;
  region: string;
  annotationTypes: BedtoolsAnnotationType[];
  minOverlapBp?: number;
  page?: number;
  pageSize?: number;
}): Promise<BedtoolsIntersectResponse> {
  const { data } = await axios.post<BedtoolsIntersectResponse | BackendEnvelope<BedtoolsIntersectResponse>>(
    buildApiUrl("api/feature-detail/regulatory/reference-intersect"),
    {
      region,
      annotationTypes,
      minOverlapBp,
      page,
      pageSize,
    },
    {
      params: { genomeBuild },
    }
  );
  return unwrapSearchResultResponse(data, "POST /api/feature-detail/regulatory/reference-intersect");
}

export type FeatureRegulatoryAnnotationMode =
  | "gene_body"
  | "promoter"
  | "super_enhancer"
  | "typical_enhancer";

export async function fetchFeatureRegulatoryAnnotation({
  gene,
  chrom,
  start,
  end,
  strand,
  mode,
  annotationType,
  domain = "integration",
  genomeBuild = "hg38",
}: {
  gene: string;
  chrom?: string;
  start?: number;
  end?: number;
  strand?: string;
  mode: FeatureRegulatoryAnnotationMode;
  annotationType?: BedtoolsAnnotationType;
  domain?: SearchResultDomain | string;
  genomeBuild?: string;
}): Promise<BedtoolsIntersectResponse> {
  const { data } = await axios.get<BedtoolsIntersectResponse | BackendEnvelope<BedtoolsIntersectResponse>>(
    buildApiUrl("api/feature-detail/regulatory/gene-annotation"),
    {
      params: {
        gene,
        chrom,
        start,
        end,
        strand,
        mode,
        annotationType: annotationType || undefined,
        domain,
        genomeBuild,
      },
    }
  );
  return unwrapSearchResultResponse(data, "GET /api/feature-detail/regulatory/gene-annotation");
}

export interface FeatureOccurrenceTopCellType {
  cellType?: string;
  count?: number;
}

export interface FeatureOccurrenceDatasetEntry {
  datasetId?: string;
  cellType?: string;
  cluster?: string;
  occurrenceCount?: number;
}

export interface DatasetRankingItem {
  datasetId?: string;
  sampleName?: string;
  recordCount?: number;
  cellContextCount?: number;
  clusterCount?: number;
}

export interface CellContextRankingItem {
  cellType?: string;
  recordCount?: number;
  datasetCount?: number;
  clusterCount?: number;
}

export interface GeneEnhancerSummary {
  enhancerType: string;
  label: string;
  matchedRegionCount: number;
  biosampleCount: number;
  exampleBiosamples: string[];
  exampleRegions: string[];
}

export interface FeatureOccurrenceResponse {
  featureType?: string;
  featureId?: string;
  domain?: string;
  genomeBuild?: string;
  geneBodyRegion?: string;
  promoterRegion?: string;
  enhancerSummaries?: GeneEnhancerSummary[];
  datasetCount?: number;
  cellTypeCount?: number;
  clusterCount?: number;
  totalOccurrences?: number;
  topCellTypes?: FeatureOccurrenceTopCellType[];
  datasets?: FeatureOccurrenceDatasetEntry[];
  datasetRanking?: DatasetRankingItem[];
  cellContextRanking?: CellContextRankingItem[];
  available?: boolean;
  message?: string | null;
}

export async function fetchFeatureOccurrence({
  type,
  gene,
  chrom,
  start,
  end,
  strand,
  domain,
  full,
}: {
  type: "gene" | "peak";
  gene?: string;
  chrom?: string;
  start?: number;
  end?: number;
  strand?: string;
  domain?: string;
  full?: boolean;
}): Promise<FeatureOccurrenceResponse> {
  const { data } = await axios.get<FeatureOccurrenceResponse>(
    buildApiUrl("/api/feature-detail/occurrence"),
    { params: { type, gene, chrom, start, end, strand, domain, full: full ? "true" : undefined } }
  );
  return data;
}

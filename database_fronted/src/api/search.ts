// src/api/search.ts
export type DomainType = "integration" | "rna" | "atac" | "tf";

// 你说的 tag=3 这种：保留，但建议后端也回 type
export interface SearchResultPayload {
  tag?: number;
  type?: DomainType;

  overview: Record<string, any>;
  charts?: Array<{ key: string; label: string; option?: any }>;
  tables?: Array<{
    title: string;
    columns: Array<{ prop: string; label: string; minWidth?: number }>;
    rows: any[];
  }>;
}

export function domainFromTag(tag?: number): DomainType | null {
  if (tag === 1) return "integration";
  if (tag === 2) return "rna";
  if (tag === 3) return "atac";
  if (tag === 4) return "tf";
  return null;
}

/** ========= 查询模式（建议 URL 也带 mode，方便 SearchResultView 判断/展示） ========= */
export type SearchMode =
  | "id"
  | "genome_region"
  | "tissue"
  | "tf"
  | "gene"
  | "snp";

/** ========= 各类查询参数（先定签名，后端再细化） ========= */
export interface QueryByIdParams {
  mode: "id";
  id: string;                 // 精准 ID（Sample / Peak / TF 等，后端决定语义）
  domain?: DomainType;         // 可选：由入口页面带来（Download 点击时很常见）
}

export interface GenomeRegionParams {
  mode: "genome_region";
  chr: string;
  start: number;
  end: number;
  domain?: DomainType;         // ATAC/TF 常用
}

export interface TissueParams {
  mode: "tissue";
  tissue: string;
  disease?: string;
  category?: string;
  domain?: DomainType;
  page?: number;
  pageSize?: number;
}

export interface TFParams {
  mode: "tf";
  tf: string;
  tissue?: string;
  domain?: DomainType;         // 多数是 TF 域，但也可能 cross-domain
  page?: number;
  pageSize?: number;
}

export interface GeneParams {
  mode: "gene";
  gene: string;
  tissue?: string;
  domain?: DomainType;
  page?: number;
  pageSize?: number;
}

export interface SNPParams {
  mode: "snp";
  rsid?: string;
  chr?: string;
  pos?: number;
  domain?: DomainType;
  page?: number;
  pageSize?: number;
}

export type AnySearchParams =
  | QueryByIdParams
  | GenomeRegionParams
  | TissueParams
  | TFParams
  | GeneParams
  | SNPParams;

/** ========= 你要的：清晰 API ========= */
export async function searchQueryById(params: QueryByIdParams): Promise<SearchResultPayload> {
  return mockResult(params.domain ?? "integration", params.id, params.mode);
}

export async function searchByGenomeRegion(params: GenomeRegionParams): Promise<SearchResultPayload> {
  const id = `${params.chr}:${params.start}-${params.end}`;
  return mockResult(params.domain ?? "atac", id, params.mode);
}

export async function searchByTissue(params: TissueParams): Promise<SearchResultPayload> {
  const id = `${params.tissue}${params.disease ? "_" + params.disease : ""}`;
  return mockResult(params.domain ?? "integration", id, params.mode);
}

export async function searchByTF(params: TFParams): Promise<SearchResultPayload> {
  const id = params.tf;
  return mockResult(params.domain ?? "tf", id, params.mode);
}

export async function searchByGene(params: GeneParams): Promise<SearchResultPayload> {
  const id = params.gene;
  return mockResult(params.domain ?? "rna", id, params.mode);
}

export async function searchBySNP(params: SNPParams): Promise<SearchResultPayload> {
  const id = params.rsid ?? (params.chr && params.pos ? `${params.chr}:${params.pos}` : "SNP");
  return mockResult(params.domain ?? "integration", id, params.mode);
}

/** ========= 统一入口（SearchResultView 用它：根据 params.mode 分发） ========= */
export async function runSearch(params: AnySearchParams): Promise<SearchResultPayload> {
  switch (params.mode) {
    case "id": return searchQueryById(params);
    case "genome_region": return searchByGenomeRegion(params);
    case "tissue": return searchByTissue(params);
    case "tf": return searchByTF(params);
    case "gene": return searchByGene(params);
    case "snp": return searchBySNP(params);
  }
}

/** ========= mock（后面删掉换 axios） ========= */
function mockResult(type: DomainType, id: string, mode: SearchMode): SearchResultPayload {
  const tag = type === "integration" ? 1 : type === "rna" ? 2 : type === "atac" ? 3 : 4;

  return {
    type,
    tag,
    overview: {
      id,
      mode,
      domain: type.toUpperCase(),
      tissue: "Lung",
      disease: "Inflammation",
      pmid: "31427789",
      downloadUrl: "/api/download/mock",
    },
    charts: [
      { key: "stat", label: "Statistics" },
      { key: "density", label: "Density" },
      { key: "qq", label: "QQ-plot" },
      { key: "manhattan", label: "Manhattan" },
    ],
    tables: [
      {
        title: `${type.toUpperCase()} related items`,
        columns: [
          { prop: "id", label: "ID", minWidth: 140 },
          { prop: "tissue", label: "Tissue", minWidth: 140 },
          { prop: "cellType", label: "Cell Type", minWidth: 220 },
          { prop: "score", label: "score", minWidth: 120 },
        ],
        rows: Array.from({ length: 10 }, (_, i) => ({
          id: `${type.toUpperCase()}_${String(i + 1).padStart(4, "0")}`,
          tissue: ["Lung", "Kidney", "Brain"][i % 3],
          cellType: ["CD4+ T", "CD8+ T", "ENDO", "LEUK"][i % 4],
          score: (Math.random() * 0.3 + 0.1).toFixed(3),
        })),
      },
    ],
  };
}
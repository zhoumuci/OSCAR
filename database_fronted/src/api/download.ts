// src/api/download.ts
export type TabKey = "integration" | "rna" | "atac" | "tf";
export type QCStatus = "pass" | "warn" | "fail";

export interface DownloadFile {
  id: string;
  title: string;
  format: string; // bed/csv/txt/zip/tsv/h5ad/pdf...
  url: string;    // 后端/OSS 给真链接即可
  size?: string;  // 可选
}

export interface DownloadGroup {
  title: string;
  files: DownloadFile[];
}

export interface DownloadRow {
  sampleId: string;
  biosampleType: string;
  biosampleName: string;
  cellType?: string;
  tissueType: string;
  disease: string;
  category: string;
  regionNumber?: number;
  qc: QCStatus[];
  downloads: DownloadGroup[];
}

function qcPack(seed: number): QCStatus[] {
  const arr: QCStatus[] = [];
  for (let i = 0; i < 5; i++) {
    const x = (seed * 97 + i * 13) % 100;
    if (x < 78) arr.push("pass");
    else if (x < 95) arr.push("warn");
    else arr.push("fail");
  }
  return arr;
}

function makeUrl(sampleId: string, tab: TabKey, suffix: string): string {
  // 先占位：你后面接后端时，直接返回真实 url（例如 OSS https 链接）即可
  return `/api/download/${tab}/${sampleId}.${suffix}`;
}

function buildDownloads(sampleId: string, tab: TabKey): DownloadGroup[] {
  if (tab === "atac") {
    return [
      {
        title: "Accessible chromatin regions",
        files: [
          { id: `${sampleId}-bed`, title: "Regions (BED)", format: "bed", url: makeUrl(sampleId, tab, "bed"), size: "—" },
          { id: `${sampleId}-csv`, title: "Regions (CSV)", format: "csv", url: makeUrl(sampleId, tab, "csv"), size: "—" },
        ],
      },
      {
        title: "Associated genes",
        files: [{ id: `${sampleId}-gene`, title: "Associated genes (TXT)", format: "txt", url: makeUrl(sampleId, tab, "txt"), size: "—" }],
      },
    ];
  }

  if (tab === "tf") {
    return [
      {
        title: "TF footprint",
        files: [
          { id: `${sampleId}-footprint`, title: "Footprint (TXT)", format: "txt", url: makeUrl(sampleId, tab, "txt"), size: "—" },
          { id: `${sampleId}-bundle`, title: "Footprint bundle (ZIP)", format: "zip", url: makeUrl(sampleId, tab, "zip"), size: "—" },
        ],
      },
      {
        title: "Associated genes",
        files: [{ id: `${sampleId}-tf-gene`, title: "TF→Gene links (TSV)", format: "tsv", url: makeUrl(sampleId, tab, "tsv"), size: "—" }],
      },
    ];
  }

  if (tab === "rna") {
    return [
      {
        title: "Expression resources",
        files: [
          { id: `${sampleId}-expr`, title: "Expression matrix (TSV)", format: "tsv", url: makeUrl(sampleId, tab, "tsv"), size: "—" },
          { id: `${sampleId}-deg`, title: "DEG table (CSV)", format: "csv", url: makeUrl(sampleId, tab, "csv"), size: "—" },
        ],
      },
      {
        title: "Gene annotation",
        files: [{ id: `${sampleId}-anno`, title: "Gene annotation (TXT)", format: "txt", url: makeUrl(sampleId, tab, "txt"), size: "—" }],
      },
    ];
  }

  // integration
  return [
    {
      title: "Integrated outputs",
      files: [
        { id: `${sampleId}-embed`, title: "Joint embedding (H5AD)", format: "h5ad", url: makeUrl(sampleId, tab, "h5ad"), size: "—" },
        { id: `${sampleId}-links`, title: "Peak–Gene links (TSV)", format: "tsv", url: makeUrl(sampleId, tab, "tsv"), size: "—" },
      ],
    },
    {
      title: "Summary",
      files: [{ id: `${sampleId}-report`, title: "Report (PDF)", format: "pdf", url: makeUrl(sampleId, tab, "pdf"), size: "—" }],
    },
  ];
}

function genRows(tab: TabKey, n: number): DownloadRow[] {
  const tissuePool = ["Kidney", "Lung", "Brain", "Liver", "Blood", "Colon"];
  const diseasePool = ["Normal", "Paracancerous", "Cancer", "Inflammation"];
  const categoryPool = ["Tumor", "Normal", "Other"];
  const cellTypePool = ["PT", "DCT", "ENDO", "LEUK", "PODO", "PEC", "MES", "CD4.T", "CD8.T"];

  const rows: DownloadRow[] = [];
  for (let i = 1; i <= n; i++) {
    const sampleId = `Sample_H_${String(i).padStart(4, "0")}`;
    const tissueType = tissuePool[(i * 7) % tissuePool.length];
    const disease = diseasePool[(i * 11) % diseasePool.length];
    const category = categoryPool[(i * 3) % categoryPool.length];
    const biosampleType = i % 5 === 0 ? "Cell Line" : "Tissue";
    const biosampleName = biosampleType === "Cell Line" ? "H1703" : tissueType;

    const row: DownloadRow = {
      sampleId: sampleId ?? "",
      biosampleType: biosampleType ?? "",
      biosampleName: biosampleName ?? "",
      tissueType: tissueType ?? "",
      disease: disease ?? "",
      category: category ?? "",
      qc: qcPack(i),
      downloads: buildDownloads(sampleId ?? "", tab ?? ""),
    };

    if (tab === "atac" || tab === "tf") row.regionNumber = 70000 + ((i * 131) % 60000);
    if (tab !== "rna") row.cellType = cellTypePool[(i * 5) % cellTypePool.length];

    rows.push(row);
  }
  return rows;
}

const DB: Record<TabKey, DownloadRow[]> = {
  integration: genRows("integration", 180),
  rna: genRows("rna", 220),
  atac: genRows("atac", 615),
  tf: genRows("tf", 260),
};

export async function fetchDownloadRows(tab: TabKey): Promise<DownloadRow[]> {
  // 后端接入时替换成 axios.get(...)
  return DB[tab];
}
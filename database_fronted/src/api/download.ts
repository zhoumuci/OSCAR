import axios from "axios";
import { buildApiUrl } from "@/config/api";

export type TabKey = "integration" | "rna" | "atac" | "tf";

export interface DownloadFile {
  id: string;
  title: string;
  format: string;
  url: string;
  size?: string;
}

export interface DownloadGroup {
  title: string;
  files: DownloadFile[];
}

export interface DownloadRow {
  datasetId: string;
  sampleType: string;
  tissue: string;
  sampleName: string;
  cells: number;
  platform: string;
  sourceId: string;
  disease: string;
  sampleSource: string;
  downloads: DownloadDomainNode[];
}

type DownloadSamplePayload = {
  items?: Array<Record<string, unknown>>;
};

export async function fetchDownloadRows(_tab: TabKey): Promise<DownloadRow[]> {
  const { data } = await axios.get<DownloadSamplePayload | Array<Record<string, unknown>>>(
    buildApiUrl("api/download/samples"),
    { timeout: 10000 }
  );
  const items = Array.isArray(data) ? data : data.items ?? [];
  return items.map((s) => {
    const datasetId = String(s.datasetId ?? "");
    return {
      datasetId,
      sampleType: String(s.sampleType ?? "-"),
      tissue: String(s.tissue ?? "-"),
      sampleName: String(s.sampleName ?? "-"),
      cells: Number(s.cellCount ?? 0),
      platform: String(s.platform ?? "-"),
      sourceId: String(s.sourceId ?? "-"),
      disease: String(s.disease ?? "-"),
      sampleSource: String(s.sampleSource ?? "-"),
      downloads: buildDownloads(datasetId),
    };
  });
}

function makeUrl(sampleId: string, domain: string, fileType: string, format: string, signalType?: string): string {
  const params = new URLSearchParams({ type: fileType, format });
  if (signalType) params.set("signalType", signalType);
  return buildApiUrl(`api/download/${domain}/${sampleId}?${params.toString()}`);
}

export interface DownloadDomainNode {
  domain: string;
  label: string;
  color: string;
  children: DownloadTypeNode[];
}

export interface DownloadTypeNode {
  type: string;
  label: string;
  files: DownloadFile[];
}

const DOMAIN_TREE: DownloadDomainNode[] = [
  {
    domain: "integration",
    label: "Integration",
    color: "#8FA59C",
    children: [
      {
        type: "gene_exp",
        label: "GENE EXPRESSION MARKERS",
        files: [
          { id: "int-gene-exp-tsv", title: "Marker genes (TSV)", format: "tsv", url: "", size: "-" },
          { id: "int-gene-exp-csv", title: "Marker genes (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
      {
        type: "gene_score",
        label: "GENE SCORE MARKERS",
        files: [
          { id: "int-gene-score-tsv", title: "Marker genes (TSV)", format: "tsv", url: "", size: "-" },
          { id: "int-gene-score-csv", title: "Marker genes (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
      {
        type: "marker_peak",
        label: "MARKER PEAKS",
        files: [
          { id: "int-peak-tsv", title: "Marker peaks (TSV)", format: "tsv", url: "", size: "-" },
          { id: "int-peak-csv", title: "Marker peaks (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
      {
        type: "p2g_marker",
        label: "P2G LINKS (MARKER)",
        files: [
          { id: "int-p2g-marker-tsv", title: "P2G Marker links (TSV)", format: "tsv", url: "", size: "-" },
          { id: "int-p2g-marker-csv", title: "P2G Marker links (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
      {
        type: "p2g",
        label: "P2G LINKS (ALL)",
        files: [
          { id: "int-p2g-tsv", title: "All Peak-to-Gene links (TSV)", format: "tsv", url: "", size: "-" },
          { id: "int-p2g-csv", title: "All Peak-to-Gene links (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
    ],
  },
  {
    domain: "rna",
    label: "RNA",
    color: "#7BA7C9",
    children: [
      {
        type: "marker_gene",
        label: "MARKER GENES",
        files: [
          { id: "rna-marker-gene-tsv", title: "Marker genes (TSV)", format: "tsv", url: "", size: "-" },
          { id: "rna-marker-gene-csv", title: "Marker genes (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
    ],
  },
  {
    domain: "atac",
    label: "ATAC",
    color: "#E8A87C",
    children: [
      {
        type: "gene_score",
        label: "GENE SCORE MARKERS",
        files: [
          { id: "atac-gene-score-tsv", title: "Marker genes (TSV)", format: "tsv", url: "", size: "-" },
          { id: "atac-gene-score-csv", title: "Marker genes (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
      {
        type: "marker_peak",
        label: "MARKER PEAKS",
        files: [
          { id: "atac-peak-tsv", title: "Marker peaks (TSV)", format: "tsv", url: "", size: "-" },
          { id: "atac-peak-csv", title: "Marker peaks (CSV)", format: "csv", url: "", size: "-" },
        ],
      },
    ],
  },
];

export function buildDownloads(sampleId: string): DownloadDomainNode[] {
  return DOMAIN_TREE.map((domainNode) => ({
    ...domainNode,
    children: domainNode.children.map((typeNode) => ({
      ...typeNode,
      files: typeNode.files.map((file) => ({
        ...file,
        id: `${sampleId}-${file.id}`,
        url: makeUrl(
          sampleId,
          domainNode.domain,
          typeNode.type === "p2g" ? "p2g" : typeNode.type === "p2g_marker" ? "p2g_marker" : typeNode.type === "marker_peak" ? "marker_peak" : "marker_gene",
          file.format,
          typeNode.type === "gene_exp" ? "gene_expression" : typeNode.type === "gene_score" ? "gene_score" : undefined
        ),
      })),
    })),
  }));
}

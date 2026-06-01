import type { SearchResultDomain } from "@/api/searchResult";

export function domainDisplayLabel(domain: SearchResultDomain): string {
  const labels: Record<SearchResultDomain, string> = {
    integration: "INTEGRATION",
    rna: "RNA",
    atac: "ATAC",
  };

  return labels[domain];
}

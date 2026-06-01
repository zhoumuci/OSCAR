import type {
  MarkerGeneGroupBy,
  MarkerGeneRecord,
  MarkerGenesResponse,
  RegulatoryNetworkEdge,
  RegulatoryNetworkExpansionResponse,
  RegulatoryNetworkLink,
  RegulatoryNetworkMode,
  RegulatoryNetworkNode,
  RegulatoryNetworkNodeType,
  RegulatoryNetworkResponse,
} from "@/api/searchResult";

export const DEMO_DATA_SOURCE = "frontend_demo_only";
export type DemoDataSize = "small" | "medium" | "large";

const FIXED_SEED = 0x5eed2026;

type DemoPreset = {
  geneCount: number;
  groupCount: number;
  minPeaks: number;
  maxPeaks: number;
};

type DemoMarkerDefinition = {
  gene: string;
  cellTypeGroup: string;
  clusterGroup: string;
  logFc: number;
  avgExpression: number;
  pctExpressed: number;
  adjustedPValue: number;
};

type DemoPeakDefinition = {
  peak: string;
  chromosome: string;
  start: number;
  end: number;
  gene: string;
  score: number;
  distanceToTss: number;
  correlation: number;
};

type DemoRegulatoryLinkDefinition = {
  peakId: string;
  geneSymbol: string;
  score: number;
  distanceToTss: number;
  correlation: number;
  fdr: number;
  varQAtac: number;
  varQRna: number;
  source: string;
  cellTypeGroup: string;
  clusterGroup: string;
};

type DemoDataset = {
  markers: DemoMarkerDefinition[];
  peaks: DemoPeakDefinition[];
  regulatoryLinks: DemoRegulatoryLinkDefinition[];
  groupCellCounts: Record<MarkerGeneGroupBy, Record<string, number>>;
};

type DemoRegulatoryNode = RegulatoryNetworkNode & {
  cellCount?: number;
  proportion?: number;
  markerScore?: number;
  markerRank?: number;
};

type DemoMarkerQuery = {
  demoSize?: DemoDataSize;
  groupBy: MarkerGeneGroupBy;
  query?: string;
  page?: number;
  size?: number;
};

type DemoNetworkQuery = {
  demoSize?: DemoDataSize;
  mode: RegulatoryNetworkMode;
  gene?: string;
  peak?: string;
  groupBy?: MarkerGeneGroupBy;
  minScore?: number;
  maxDistance?: number;
};

type DemoExpansionQuery = {
  demoSize?: DemoDataSize;
  nodeId: string;
  nodeType: RegulatoryNetworkNodeType;
  groupBy?: MarkerGeneGroupBy;
  minScore?: number;
  maxDistance?: number;
  maxNeighbors?: number;
};

const DEMO_PRESETS: Record<DemoDataSize, DemoPreset> = {
  small: {
    geneCount: 8,
    groupCount: 5,
    minPeaks: 3,
    maxPeaks: 5,
  },
  medium: {
    geneCount: 25,
    groupCount: 8,
    minPeaks: 6,
    maxPeaks: 10,
  },
  large: {
    geneCount: 60,
    groupCount: 11,
    minPeaks: 10,
    maxPeaks: 18,
  },
};

const GROUPS = [
  "CD4+ T cells",
  "Naive CD4+ T cells",
  "CD8+ T cells",
  "B cells",
  "NK cells",
  "Classical Monocytes",
  "Non-classical Monocytes",
  "Inflammatory Monocytes",
  "Dendritic cells",
  "Platelets",
  "Basophils",
];

const CLUSTERS = [
  "Cluster 0",
  "Cluster 1",
  "Cluster 2",
  "Cluster 3",
  "Cluster 4",
  "Cluster 5",
  "Cluster 6",
  "Cluster 7",
];

const GENE_GROUP_MAP: Record<string, string> = {
  CD3D: "CD4+ T cells",
  CD3E: "CD4+ T cells",
  CD3G: "CD4+ T cells",
  IL7R: "Naive CD4+ T cells",
  CCR7: "Naive CD4+ T cells",
  LTB: "Naive CD4+ T cells",
  TRAC: "CD4+ T cells",
  CD4: "CD4+ T cells",
  CD8A: "CD8+ T cells",
  CD8B: "CD8+ T cells",
  GZMK: "CD8+ T cells",
  GZMA: "CD8+ T cells",
  MS4A1: "B cells",
  CD79A: "B cells",
  CD79B: "B cells",
  BANK1: "B cells",
  CD74: "B cells",
  "HLA-DRA": "B cells",
  "HLA-DPB1": "B cells",
  NKG7: "NK cells",
  GNLY: "NK cells",
  PRF1: "NK cells",
  GZMB: "NK cells",
  KLRD1: "NK cells",
  FCGR3A: "Non-classical Monocytes",
  LST1: "Classical Monocytes",
  LYZ: "Classical Monocytes",
  S100A8: "Inflammatory Monocytes",
  S100A9: "Inflammatory Monocytes",
  FCN1: "Classical Monocytes",
  CTSS: "Classical Monocytes",
  LGALS3: "Inflammatory Monocytes",
  MS4A7: "Non-classical Monocytes",
  FCER1A: "Dendritic cells",
  CLEC10A: "Dendritic cells",
  PPBP: "Platelets",
  PF4: "Platelets",
  IRF7: "Dendritic cells",
  ISG15: "Dendritic cells",
  IFITM3: "Basophils",
};

const GENE_POOL = [
  "CD3D",
  "CD3E",
  "CD3G",
  "IL7R",
  "CCR7",
  "LTB",
  "TRAC",
  "CD4",
  "CD8A",
  "CD8B",
  "GZMK",
  "GZMA",
  "LEF1",
  "MAL",
  "SELL",
  "TCF7",
  "MS4A1",
  "CD79A",
  "CD79B",
  "BANK1",
  "CD74",
  "HLA-DRA",
  "HLA-DPB1",
  "HLA-DQA1",
  "JCHAIN",
  "NKG7",
  "GNLY",
  "PRF1",
  "GZMB",
  "KLRD1",
  "FCGR3A",
  "TYROBP",
  "KLRF1",
  "LST1",
  "LYZ",
  "S100A8",
  "S100A9",
  "FCN1",
  "CTSS",
  "LGALS3",
  "MS4A7",
  "VCAN",
  "S100A12",
  "FCER1A",
  "CLEC10A",
  "PPBP",
  "PF4",
  "IRF7",
  "ISG15",
  "IFITM3",
  "GZMH",
  "CXCR4",
  "CCR10",
  "AIF1",
  "CST3",
  "ITGAX",
  "LILRA4",
  "TPSAB1",
  "CPA3",
  "MKI67",
  "TOP2A",
  "HBB",
  "HBA1",
  "HBA2",
  "RPS12",
  "RPL13A",
];

const datasetCache = new Map<DemoDataSize, DemoDataset>();

// TODO: Remove demo data after real marker gene and peak-to-gene backend endpoints are implemented.
export function getDemoMarkerGenes({
  demoSize = "medium",
  groupBy,
  query = "",
  page = 1,
  size = 10,
}: DemoMarkerQuery): MarkerGenesResponse {
  const dataset = getDemoDataset(demoSize);
  const normalizedQuery = normalize(query);
  const records = dataset.markers.map((marker) => toMarkerRecord(marker, groupBy)).filter((record) => {
    if (!normalizedQuery) return true;
    return normalize(record.gene).includes(normalizedQuery) || normalize(record.group).includes(normalizedQuery);
  });
  const safePage = Math.max(1, page);
  const safeSize = Math.max(1, size);
  const start = (safePage - 1) * safeSize;

  return {
    records: records.slice(start, start + safeSize),
    total: records.length,
    page: safePage,
    size: safeSize,
  };
}

export function getDemoRegulatoryNetwork({
  demoSize = "medium",
  mode,
  gene,
  peak,
  groupBy = "cell_type",
  minScore,
  maxDistance,
}: DemoNetworkQuery): RegulatoryNetworkResponse {
  const dataset = getDemoDataset(demoSize);
  const selectedLinks = getDemoLocalLinks({
    dataset,
    groupBy,
    mode,
    gene,
    peak,
    minScore,
    maxDistance,
  });

  return buildDemoNetworkResponse(dataset, selectedLinks, groupBy);
}

export function getDemoRegulatoryExpansion({
  demoSize = "medium",
  nodeId,
  nodeType,
  groupBy = "cell_type",
  minScore,
  maxDistance,
  maxNeighbors = 40,
}: DemoExpansionQuery): RegulatoryNetworkExpansionResponse {
  const dataset = getDemoDataset(demoSize);
  const safeNeighborCap = Math.max(1, Math.min(maxNeighbors, 50));
  const selectedLinks = getExpansionLinksForNode({
    dataset,
    nodeId,
    nodeType,
    groupBy,
    minScore,
    maxDistance,
    maxNeighbors: safeNeighborCap,
  });
  const response = buildDemoNetworkResponse(dataset, selectedLinks, groupBy);

  return {
    ...response,
    totalNeighbors: selectedLinks.length,
    hasMore: false,
  };
}

export function normalizeDemoDataSize(value: unknown): DemoDataSize {
  const normalizedValue = normalize(value);
  if (normalizedValue === "small" || normalizedValue === "medium" || normalizedValue === "large") {
    return normalizedValue;
  }

  return "medium";
}

export function getDemoPresetLabel(size: DemoDataSize): string {
  return size.toUpperCase();
}

function getDemoDataset(size: DemoDataSize): DemoDataset {
  const cachedDataset = datasetCache.get(size);
  if (cachedDataset) return cachedDataset;

  const preset = DEMO_PRESETS[size];
  const rng = createRng(FIXED_SEED + preset.geneCount * 101 + preset.maxPeaks * 17);
  const allowedGroups = GROUPS.slice(0, preset.groupCount);
  const markers = GENE_POOL.slice(0, preset.geneCount).map((gene, index) => {
    const preferredGroup = GENE_GROUP_MAP[gene];
    const fallbackGroup = allowedGroups[index % allowedGroups.length] ?? GROUPS[0] ?? "CD4+ T cells";
    const cellTypeGroup = preferredGroup && allowedGroups.includes(preferredGroup)
      ? preferredGroup
      : fallbackGroup;

    return {
      gene,
      cellTypeGroup,
      clusterGroup: CLUSTERS[index % CLUSTERS.length] ?? "Cluster 0",
      logFc: round(randomInRange(rng, 0.72, 2.85), 2),
      avgExpression: round(randomInRange(rng, 0.65, 3.55), 2),
      pctExpressed: round(randomInRange(rng, 0.38, 0.94), 3),
      adjustedPValue: Number(randomInRange(rng, 0.000001, 0.0009).toPrecision(3)),
    };
  });
  const peaks = markers.flatMap((marker, markerIndex) => {
    const peakCount = randomInt(rng, preset.minPeaks, preset.maxPeaks);
    const chromosome = getChromosome(markerIndex);
    const geneStart = 100000 + markerIndex * 380000;

    return Array.from({ length: peakCount }, (_, peakIndex) => {
      const start = geneStart + peakIndex * randomInt(rng, 850, 4600) + randomInt(rng, 0, 900);
      const end = start + randomInt(rng, 450, 1250);

      return {
        peak: `${chromosome}:${start}-${end}`,
        chromosome,
        start,
        end,
        gene: marker.gene,
        score: round(randomInRange(rng, 0.2, 0.95), 3),
        distanceToTss: randomInt(rng, 500, 200000),
        correlation: round(randomInRange(rng, -0.5, 0.9), 3),
      };
    });
  });
  const regulatoryLinks = buildRegulatoryLinks(markers, peaks, rng);
  const groupCellCounts = buildGroupCellCounts(markers, allowedGroups, rng);
  const dataset = { markers, peaks, regulatoryLinks, groupCellCounts };

  datasetCache.set(size, dataset);
  return dataset;
}

function buildRegulatoryLinks(
  markers: DemoMarkerDefinition[],
  peaks: DemoPeakDefinition[],
  rng: () => number
): DemoRegulatoryLinkDefinition[] {
  const markerByGene = new Map(markers.map((marker) => [marker.gene, marker]));
  const markerIndexByGene = new Map(markers.map((marker, index) => [marker.gene, index]));
  const linkMap = new Map<string, DemoRegulatoryLinkDefinition>();

  peaks.forEach((peak, peakIndex) => {
    const primaryMarker = markerByGene.get(peak.gene);
    if (!primaryMarker) return;

    const primaryIndex = markerIndexByGene.get(primaryMarker.gene) ?? 0;
    const sameCellTypeGenes = markers
      .filter((marker) => marker.cellTypeGroup === primaryMarker.cellTypeGroup && marker.gene !== primaryMarker.gene)
      .map((marker) => marker.gene);
    const nearbyGene = markers[(primaryIndex + 1) % markers.length]?.gene;
    const secondaryGene = sameCellTypeGenes[peakIndex % Math.max(1, sameCellTypeGenes.length)] ?? nearbyGene;
    const tertiaryGene = sameCellTypeGenes[(peakIndex + 1) % Math.max(1, sameCellTypeGenes.length)];
    const linkedGenes = [primaryMarker.gene, secondaryGene, peakIndex % 4 === 0 ? tertiaryGene : undefined]
      .filter((gene): gene is string => Boolean(gene))
      .filter((gene, index, genes) => genes.indexOf(gene) === index);

    linkedGenes.forEach((gene, linkIndex) => {
      const marker = markerByGene.get(gene);
      if (!marker) return;

      const scorePenalty = linkIndex === 0 ? 0 : randomInRange(rng, 0.06, 0.22);
      const distanceLift = linkIndex === 0 ? 0 : randomInt(rng, 1500, 60000);
      const correlationShift = linkIndex === 0 ? 0 : randomInRange(rng, -0.18, 0.12);
      const link: DemoRegulatoryLinkDefinition = {
        peakId: peak.peak,
        geneSymbol: gene,
        score: round(Math.max(0.12, peak.score - scorePenalty), 3),
        distanceToTss: peak.distanceToTss + distanceLift,
        correlation: round(Math.max(-0.65, Math.min(0.95, peak.correlation + correlationShift)), 3),
        fdr: Number(randomInRange(rng, 0.0001, linkIndex === 0 ? 0.035 : 0.08).toPrecision(3)),
        varQAtac: round(randomInRange(rng, 0.18, 0.92), 3),
        varQRna: round(randomInRange(rng, 0.16, 0.88), 3),
        source: DEMO_DATA_SOURCE,
        cellTypeGroup: marker.cellTypeGroup,
        clusterGroup: marker.clusterGroup,
      };

      linkMap.set(`${link.peakId}|${link.geneSymbol}`, link);
    });
  });

  return Array.from(linkMap.values());
}

function buildGroupCellCounts(
  markers: DemoMarkerDefinition[],
  allowedGroups: string[],
  rng: () => number
): Record<MarkerGeneGroupBy, Record<string, number>> {
  const cellTypeCounts: Record<string, number> = {};
  const clusterCounts: Record<string, number> = {};

  allowedGroups.forEach((group, index) => {
    const markerBonus = markers.filter((marker) => marker.cellTypeGroup === group).length * 23;
    cellTypeCounts[group] = Math.max(80, 980 - index * 68 + markerBonus + randomInt(rng, 0, 42));
  });

  CLUSTERS.forEach((cluster, index) => {
    const markerBonus = markers.filter((marker) => marker.clusterGroup === cluster).length * 18;
    clusterCounts[cluster] = Math.max(45, 760 - index * 46 + markerBonus + randomInt(rng, 0, 36));
  });

  return {
    cell_type: cellTypeCounts,
    cluster: clusterCounts,
  };
}

function getDemoLocalLinks({
  dataset,
  groupBy,
  mode,
  gene,
  peak,
  minScore,
  maxDistance,
}: {
  dataset: DemoDataset;
  groupBy: MarkerGeneGroupBy;
  mode: RegulatoryNetworkMode;
  gene?: string;
  peak?: string;
  minScore?: number;
  maxDistance?: number;
}): DemoRegulatoryLinkDefinition[] {
  const normalizedGene = normalize(gene);
  if (mode === "gene" && normalizedGene) {
    const marker = dataset.markers.find((candidate) => normalize(candidate.gene) === normalizedGene)
      ?? dataset.markers.find((candidate) => normalize(candidate.gene).includes(normalizedGene));
    return marker ? getTopLinksForGene(dataset, marker.gene, minScore, maxDistance, 5) : [];
  }

  if (mode === "peak") {
    return getSearchLinksForPeakMode(dataset, peak, gene, minScore, maxDistance, 5);
  }

  return getDefaultPreviewLinks(dataset, groupBy, minScore, maxDistance);
}

function getDefaultPreviewLinks(
  dataset: DemoDataset,
  groupBy: MarkerGeneGroupBy,
  minScore?: number,
  maxDistance?: number
): DemoRegulatoryLinkDefinition[] {
  const dominantGroup = getDominantGroup(dataset, groupBy);
  const markerGenes = getTopMarkerGenesForGroup(dataset, groupBy, dominantGroup, 2);

  return markerGenes.flatMap((marker) => getTopLinksForGene(dataset, marker.gene, minScore, maxDistance, 5));
}

function getExpansionLinksForNode({
  dataset,
  nodeId,
  nodeType,
  groupBy,
  minScore,
  maxDistance,
  maxNeighbors,
}: {
  dataset: DemoDataset;
  nodeId: string;
  nodeType: RegulatoryNetworkNodeType;
  groupBy: MarkerGeneGroupBy;
  minScore?: number;
  maxDistance?: number;
  maxNeighbors: number;
}): DemoRegulatoryLinkDefinition[] {
  if (nodeType === "group") {
    const group = getNodeIdValue(nodeId, "group");
    const markerGenes = getTopMarkerGenesForGroup(dataset, groupBy, group, Math.min(8, Math.max(2, Math.ceil(maxNeighbors / 5))));
    return markerGenes
      .flatMap((marker) => getTopLinksForGene(dataset, marker.gene, minScore, maxDistance, 3))
      .slice(0, maxNeighbors);
  }

  if (nodeType === "peak") {
    return getLinksForPeak(dataset, getNodeIdValue(nodeId, "peak"), minScore, maxDistance, maxNeighbors);
  }

  return getTopLinksForGene(dataset, getNodeIdValue(nodeId, "gene"), minScore, maxDistance, Math.min(maxNeighbors, 12));
}

function getSearchLinksForPeakMode(
  dataset: DemoDataset,
  peak?: string,
  gene?: string,
  minScore?: number,
  maxDistance?: number,
  limit = 5
): DemoRegulatoryLinkDefinition[] {
  const normalizedPeak = normalize(peak);
  const normalizedGene = normalize(gene);
  const candidates = dataset.regulatoryLinks.filter((link) => {
    if (normalizedPeak && !normalize(link.peakId).includes(normalizedPeak)) return false;
    if (normalizedGene && !normalize(link.geneSymbol).includes(normalizedGene)) return false;
    return true;
  });
  const sortedCandidates = sortRegulatoryLinks(filterRegulatoryLinks(candidates, minScore, maxDistance));
  const selectedPeakId = sortedCandidates[0]?.peakId;

  return selectedPeakId ? getLinksForPeak(dataset, selectedPeakId, minScore, maxDistance, limit) : [];
}

function getDominantGroup(dataset: DemoDataset, groupBy: MarkerGeneGroupBy): string {
  const counts = dataset.groupCellCounts[groupBy];
  const groups = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  const fallbackMarker = dataset.markers[0];
  return groups[0]?.[0] ?? (fallbackMarker ? getMarkerGroup(fallbackMarker, groupBy) : "");
}

function getTopMarkerGenesForGroup(
  dataset: DemoDataset,
  groupBy: MarkerGeneGroupBy,
  group: string,
  limit: number
): DemoMarkerDefinition[] {
  return dataset.markers
    .filter((marker) => getMarkerGroup(marker, groupBy) === group)
    .sort((a, b) => markerPriorityScore(b) - markerPriorityScore(a))
    .slice(0, limit);
}

function getMarkerRank(dataset: DemoDataset, groupBy: MarkerGeneGroupBy, marker: DemoMarkerDefinition): number {
  const rankedMarkers = dataset.markers
    .filter((candidate) => getMarkerGroup(candidate, groupBy) === getMarkerGroup(marker, groupBy))
    .sort((a, b) => markerPriorityScore(b) - markerPriorityScore(a));

  return rankedMarkers.findIndex((candidate) => candidate.gene === marker.gene) + 1;
}

function markerPriorityScore(marker: DemoMarkerDefinition): number {
  return marker.logFc * 0.45 + marker.avgExpression * 0.35 + marker.pctExpressed * 0.2;
}

function getTopLinksForGene(
  dataset: DemoDataset,
  gene: string,
  minScore?: number,
  maxDistance?: number,
  limit = 10
): DemoRegulatoryLinkDefinition[] {
  return sortRegulatoryLinks(filterRegulatoryLinks(
    dataset.regulatoryLinks.filter((link) => link.geneSymbol === gene),
    minScore,
    maxDistance
  )).slice(0, limit);
}

function getLinksForPeak(
  dataset: DemoDataset,
  peakId: string,
  minScore?: number,
  maxDistance?: number,
  limit = 50
): DemoRegulatoryLinkDefinition[] {
  return sortRegulatoryLinks(filterRegulatoryLinks(
    dataset.regulatoryLinks.filter((link) => link.peakId === peakId),
    minScore,
    maxDistance
  )).slice(0, limit);
}

function filterRegulatoryLinks(
  links: DemoRegulatoryLinkDefinition[],
  minScore?: number,
  maxDistance?: number
): DemoRegulatoryLinkDefinition[] {
  return links.filter((link) => {
    if (typeof minScore === "number" && link.score < minScore) return false;
    if (typeof maxDistance === "number" && link.distanceToTss > maxDistance) return false;
    return true;
  });
}

function sortRegulatoryLinks(links: DemoRegulatoryLinkDefinition[]): DemoRegulatoryLinkDefinition[] {
  return [...links].sort((a, b) => b.score - a.score || a.distanceToTss - b.distanceToTss);
}

function buildDemoNetworkResponse(
  dataset: DemoDataset,
  selectedLinks: DemoRegulatoryLinkDefinition[],
  groupBy: MarkerGeneGroupBy
): RegulatoryNetworkResponse {
  const genesWithLinks = new Set(selectedLinks.map((candidate) => candidate.geneSymbol));
  const markerNodes = dataset.markers.filter((marker) => genesWithLinks.has(marker.gene));
  const nodes: DemoRegulatoryNode[] = [];
  const edges: RegulatoryNetworkEdge[] = [];
  const links: RegulatoryNetworkLink[] = [];
  const groupCounts = dataset.groupCellCounts[groupBy];
  const totalGroupCount = Object.values(groupCounts).reduce((sum, count) => sum + count, 0);

  markerNodes.forEach((marker) => {
    const group = getMarkerGroup(marker, groupBy);
    const cellCount = groupCounts[group] ?? 0;

    nodes.push({
      id: geneNodeId(marker.gene),
      type: "gene",
      label: marker.gene,
      markerStatus: "marker",
      group,
      markerScore: Number(markerPriorityScore(marker).toFixed(3)),
      markerRank: getMarkerRank(dataset, groupBy, marker),
    });

    nodes.push({
      id: groupNodeId(group),
      type: "group",
      label: group,
      group,
      cellCount,
      proportion: totalGroupCount ? cellCount / totalGroupCount : 0,
    });

    edges.push({
      source: geneNodeId(marker.gene),
      target: groupNodeId(group),
      type: "marker_of",
      sourceMethod: DEMO_DATA_SOURCE,
    });
  });

  selectedLinks.forEach((candidate) => {
    const peak = getPeak(dataset, candidate.peakId);
    if (!peak) return;

    const marker = getMarker(dataset, candidate.geneSymbol);
    const group = marker ? getMarkerGroup(marker, groupBy) : "";

    nodes.push({
      id: peakNodeId(candidate.peakId),
      type: "peak",
      label: candidate.peakId,
      chromosome: peak.chromosome,
      start: peak.start,
      end: peak.end,
    });

    edges.push({
      source: peakNodeId(candidate.peakId),
      target: geneNodeId(candidate.geneSymbol),
      type: "peak_to_gene",
      score: candidate.score,
      distanceToTss: candidate.distanceToTss,
      correlation: candidate.correlation,
      fdr: candidate.fdr,
      varQAtac: candidate.varQAtac,
      varQRna: candidate.varQRna,
      sourceMethod: DEMO_DATA_SOURCE,
    });

    links.push({
      peakId: candidate.peakId,
      peak: candidate.peakId,
      geneSymbol: candidate.geneSymbol,
      linkedGene: candidate.geneSymbol,
      group,
      cellTypeGroup: candidate.cellTypeGroup,
      clusterGroup: candidate.clusterGroup,
      markerStatus: "marker",
      distanceToTss: candidate.distanceToTss,
      score: candidate.score,
      linkScore: candidate.score,
      correlation: candidate.correlation,
      fdr: candidate.fdr,
      varQAtac: candidate.varQAtac,
      varQRna: candidate.varQRna,
      linkType: "peak_to_gene",
      source: DEMO_DATA_SOURCE,
    });
  });

  const dedupedLinks = dedupeLinks(links);

  return {
    nodes: dedupeNodes(nodes),
    edges: dedupeEdges(edges),
    links: dedupedLinks,
    totalLinks: dedupedLinks.length,
    hasMore: false,
  };
}

function toMarkerRecord(marker: DemoMarkerDefinition, groupBy: MarkerGeneGroupBy): MarkerGeneRecord {
  return {
    gene: marker.gene,
    group: getMarkerGroup(marker, groupBy),
    logFc: marker.logFc,
    avgExpression: marker.avgExpression,
    pctExpressed: marker.pctExpressed,
    adjustedPValue: marker.adjustedPValue,
    source: DEMO_DATA_SOURCE,
  };
}

function getMarkerGroup(marker: DemoMarkerDefinition, groupBy: MarkerGeneGroupBy): string {
  return groupBy === "cluster" ? marker.clusterGroup : marker.cellTypeGroup;
}

function getMarker(dataset: DemoDataset, gene: string): DemoMarkerDefinition | undefined {
  return dataset.markers.find((marker) => marker.gene === gene);
}

function getPeak(dataset: DemoDataset, peakId: string): DemoPeakDefinition | undefined {
  return dataset.peaks.find((peak) => peak.peak === peakId);
}

function dedupeNodes(nodes: RegulatoryNetworkNode[]): RegulatoryNetworkNode[] {
  const nodeMap = new Map<string, RegulatoryNetworkNode>();
  nodes.forEach((node) => nodeMap.set(node.id, node));
  return Array.from(nodeMap.values());
}

function dedupeEdges(edges: RegulatoryNetworkEdge[]): RegulatoryNetworkEdge[] {
  const edgeMap = new Map<string, RegulatoryNetworkEdge>();
  edges.forEach((edge) => edgeMap.set(`${edge.source}|${edge.target}|${edge.type}`, edge));
  return Array.from(edgeMap.values());
}

function dedupeLinks(links: RegulatoryNetworkLink[]): RegulatoryNetworkLink[] {
  const linkMap = new Map<string, RegulatoryNetworkLink>();
  links.forEach((link) => linkMap.set(`${link.peak ?? ""}|${link.linkedGene ?? ""}|${link.linkType ?? ""}`, link));
  return Array.from(linkMap.values());
}

function geneNodeId(gene: string): string {
  return `gene:${gene}`;
}

function peakNodeId(peak: string): string {
  return `peak:${peak}`;
}

function groupNodeId(group: string): string {
  return `group:${group}`;
}

function getNodeIdValue(nodeId: string, prefix: RegulatoryNetworkNodeType): string {
  const expectedPrefix = `${prefix}:`;
  return nodeId.startsWith(expectedPrefix) ? nodeId.slice(expectedPrefix.length) : nodeId;
}

function getChromosome(index: number): string {
  const chromosome = (index % 22) + 1;
  return `chr${chromosome}`;
}

function createRng(seed: number): () => number {
  let state = seed >>> 0;

  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 0x100000000;
  };
}

function randomInRange(rng: () => number, min: number, max: number): number {
  return min + rng() * (max - min);
}

function randomInt(rng: () => number, min: number, max: number): number {
  return Math.floor(randomInRange(rng, min, max + 1));
}

function round(value: number, decimals: number): number {
  const factor = 10 ** decimals;
  return Math.round(value * factor) / factor;
}

function normalize(value: unknown): string {
  return String(value ?? "").trim().toLowerCase();
}

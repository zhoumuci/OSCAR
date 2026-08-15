import type {
  RegulatoryNetworkEdge,
  RegulatoryNetworkLink,
  RegulatoryNetworkNode,
  RegulatoryNetworkNodeType,
  RegulatoryNetworkResponse,
} from "@/api/searchResult";

export type GraphNodeType = "gene" | "peak";
export type GraphEdgeType = "peakGene";

export type GraphPoint = {
  x: number;
  y: number;
};

export type GraphDimensions = {
  width: number;
  height: number;
};

export type GraphNodeMetadata = {
  datasetId?: string;
  sampleName?: string;
  domain?: string;
  chromosome?: string;
  start?: number;
  end?: number;
  source?: string;
  linkedGenesCount?: number | null;
  topLinkedGenes?: string[] | string;
  remainingLinkedGenesCount?: number | null;
  linkedPeaksCount?: number | null;
  topLinkedPeaks?: string[] | string;
  remainingLinkedPeaksCount?: number | null;
  maxLinkScore?: number | null;
  correlationMin?: number | null;
  correlationMax?: number | null;
  fdrMin?: number | null;
  fdrMax?: number | null;
  minFdr?: number | null;
  totalLinks?: number | null;
  raw?: RegulatoryNetworkNode;
};

export interface GraphNode {
  id: string;
  type: GraphNodeType;
  label: string;
  x: number;
  y: number;
  radius: number;
  visible: boolean;
  selected: boolean;
  newlyAdded: boolean;
  dimmed: boolean;
  metadata: GraphNodeMetadata;
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  type: GraphEdgeType;
  score?: number | null;
  correlation?: number | null;
  fdr?: number | null;
  distanceToTss?: number | null;
  varQAtac?: number | null;
  varQRna?: number | null;
  sourceMethod?: string;
  visible: boolean;
  highlighted: boolean;
  newlyAdded: boolean;
  dimmed: boolean;
  curve: number;
}

export interface GraphLink {
  id: string;
  peakId: string;
  peak: string;
  geneSymbol: string;
  linkedGene: string;
  datasetId?: string;
  sampleName?: string;
  domain?: string;
  score?: number | null;
  linkScore?: number | null;
  correlation?: number | null;
  distanceToTss?: number | null;
  fdr?: number | null;
  varQAtac?: number | null;
  varQRna?: number | null;
  linkType?: string;
  source?: string;
  linkedGenesCount?: number | null;
  topLinkedGenes?: string[] | string;
  remainingLinkedGenesCount?: number | null;
  linkedPeaksCount?: number | null;
  topLinkedPeaks?: string[] | string;
  remainingLinkedPeaksCount?: number | null;
  maxLinkScore?: number | null;
  correlationMin?: number | null;
  correlationMax?: number | null;
  fdrMin?: number | null;
  fdrMax?: number | null;
  minFdr?: number | null;
  totalLinks?: number | null;
}

export interface RegulatoryGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
  links: GraphLink[];
}

export type MergeGraphResult = {
  graph: RegulatoryGraph;
  newNodeIds: Set<string>;
  newEdgeIds: Set<string>;
  newLinkIds: Set<string>;
};

type ReadableRecord = Record<string, unknown>;

const NODE_RADIUS: Record<GraphNodeType, number> = {
  gene: 30,
  peak: 13,
};

const DEFAULT_DIMENSIONS: GraphDimensions = {
  width: 960,
  height: 520,
};

const INITIAL_GENE_LIMIT = 2;
const INITIAL_PEAK_LIMIT_PER_GENE = 5;
const FOCUS_PEAK_LIMIT = 6;
const FOCUS_GENE_LIMIT = 8;
const SAFE_PADDING = 56;
const LABEL_GAP = 10;
const COLLISION_PADDING = 14;
const COLLISION_ITERATIONS = 90;
const MAX_LAYOUT_SCALE = 1.42;
const ORGANIC_RING_JITTER = 0.14;
const REVEAL_SECTOR_COUNT = 12;
const REVEAL_SECTOR_SPREAD = Math.PI * 0.82;
const INITIAL_LAYOUT_CANDIDATE_COUNT = 15;
const INITIAL_RELAX_ITERATIONS = 26;
const INITIAL_GENE_REFINEMENT_PASSES = 2;
const EDGE_ROUTE_SAMPLE_COUNT = 10;
const EDGE_NUDGE_ITERATIONS = 3;

type LabelBox = {
  left: number;
  top: number;
  right: number;
  bottom: number;
};

type CandidateScore = {
  point: GraphPoint;
  score: number;
  hardCollision: boolean;
  crossingCount: number;
};

type InitialLayoutCandidate = {
  positions: Map<string, GraphPoint>;
  score: number;
};

type RoutedEdge = {
  edge: GraphEdge;
  points: GraphPoint[];
};

export function emptyRegulatoryGraph(): RegulatoryGraph {
  return {
    nodes: [],
    edges: [],
    links: [],
  };
}

export function getGeneNodeId(gene: string): string {
  return `gene:${gene.trim()}`;
}

export function getPeakNodeId(peak: string): string {
  return `peak:${peak.trim()}`;
}

export function graphEdgeId(source: string, target: string, type: GraphEdgeType): string {
  const [left, right] = [source, target].sort();
  return `${type}:${left}|${right}`;
}

export function graphLinkId(peak: string, gene: string): string {
  return `peakGene:${getPeakNodeId(peak)}|${getGeneNodeId(gene)}`;
}

export function adaptRegulatoryResponse(
  response: RegulatoryNetworkResponse | null | undefined
): RegulatoryGraph {
  const nodeMap = new Map<string, GraphNode>();
  const edgeMap = new Map<string, GraphEdge>();
  const linkMap = new Map<string, GraphLink>();
  const responseRecord = (response ?? {}) as ReadableRecord;
  const responseDatasetId = readString(responseRecord, "datasetId", "dataset_id");
  const responseSampleName = readString(responseRecord, "sampleName", "sample_name");
  const responseDomain = readString(responseRecord, "domain");

  (response?.nodes ?? []).forEach((rawNode) => {
    const normalizedNode = normalizeRawNode(rawNode);
    const node = normalizedNode
      ? {
          ...normalizedNode,
          metadata: {
            ...normalizedNode.metadata,
            datasetId: normalizedNode.metadata.datasetId ?? responseDatasetId,
            sampleName: normalizedNode.metadata.sampleName ?? responseSampleName,
            domain: normalizedNode.metadata.domain ?? responseDomain,
          },
        }
      : null;
    if (node) mergeNode(nodeMap, node);
  });

  (response?.links ?? []).forEach((rawLink) => {
    const normalizedLink = normalizeRawLink(rawLink);
    const link = normalizedLink
      ? {
          ...normalizedLink,
          datasetId: normalizedLink.datasetId ?? responseDatasetId,
          sampleName: normalizedLink.sampleName ?? responseSampleName,
          domain: normalizedLink.domain ?? responseDomain,
        }
      : null;
    if (!link) return;

    linkMap.set(link.id, link);
    mergeNode(nodeMap, createGeneNode(link.geneSymbol, {
      datasetId: link.datasetId,
      sampleName: link.sampleName,
      domain: link.domain,
      linkedPeaksCount: link.linkedPeaksCount,
      topLinkedPeaks: link.topLinkedPeaks,
      remainingLinkedPeaksCount: link.remainingLinkedPeaksCount,
      maxLinkScore: link.maxLinkScore,
      correlationMin: link.correlationMin,
      correlationMax: link.correlationMax,
      fdrMin: link.fdrMin,
      fdrMax: link.fdrMax,
      minFdr: link.minFdr,
      totalLinks: link.totalLinks,
      source: link.source,
    }));
    mergeNode(nodeMap, createPeakNode(link.peak, {
      datasetId: link.datasetId,
      sampleName: link.sampleName,
      domain: link.domain,
      linkedGenesCount: link.linkedGenesCount,
      topLinkedGenes: link.topLinkedGenes,
      remainingLinkedGenesCount: link.remainingLinkedGenesCount,
      maxLinkScore: link.maxLinkScore,
      correlationMin: link.correlationMin,
      correlationMax: link.correlationMax,
      fdrMin: link.fdrMin,
      fdrMax: link.fdrMax,
      minFdr: link.minFdr,
      totalLinks: link.totalLinks,
      source: link.source,
    }));

    const peakGeneEdge = createPeakGeneEdge(link);
    edgeMap.set(peakGeneEdge.id, mergeEdge(edgeMap.get(peakGeneEdge.id), peakGeneEdge));
  });

  (response?.edges ?? []).forEach((rawEdge) => {
    const edge = normalizeRawEdge(rawEdge);
    if (!edge) return;

    edgeMap.set(edge.id, mergeEdge(edgeMap.get(edge.id), edge));
  });

  const nodes = Array.from(nodeMap.values()).sort(sortNodes);
  const nodeIds = new Set(nodes.map((node) => node.id));
  const edges = Array.from(edgeMap.values())
    .filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target))
    .sort(sortEdges);
  const links = Array.from(linkMap.values()).sort(sortLinks);

  return {
    nodes,
    edges,
    links,
  };
}

export function createAnchorGraph(node: Pick<GraphNode, "id" | "type" | "label">): RegulatoryGraph {
  return layoutInitialGraph({
    nodes: [{
      ...node,
      x: DEFAULT_DIMENSIONS.width / 2,
      y: DEFAULT_DIMENSIONS.height / 2,
      radius: NODE_RADIUS[node.type],
      visible: true,
      selected: false,
      newlyAdded: false,
      dimmed: false,
      metadata: {},
    }],
    edges: [],
    links: [],
  });
}

export function selectInitialSubgraph(graph: RegulatoryGraph): RegulatoryGraph {
  if (!graph.nodes.length) return cloneGraph(graph);
  return selectGenePeakInitialSubgraph(graph);
}

export function mergeRegulatoryGraphs(base: RegulatoryGraph, addition: RegulatoryGraph): MergeGraphResult {
  const currentNodeIds = new Set(base.nodes.map((node) => node.id));
  const currentEdgeIds = new Set(base.edges.map((edge) => edge.id));
  const currentLinkIds = new Set(base.links.map((link) => link.id));
  const nodeMap = new Map(base.nodes.map((node) => [node.id, cloneNode(node)] as const));
  const edgeMap = new Map(base.edges.map((edge) => [edge.id, cloneEdge(edge)] as const));
  const linkMap = new Map(base.links.map((link) => [link.id, { ...link }] as const));

  addition.nodes.forEach((node) => mergeNode(nodeMap, node));
  addition.edges.forEach((edge) => edgeMap.set(edge.id, mergeEdge(edgeMap.get(edge.id), edge)));
  addition.links.forEach((link) => linkMap.set(link.id, { ...link }));

  const graph = {
    nodes: Array.from(nodeMap.values()).sort(sortNodes),
    edges: Array.from(edgeMap.values()).sort(sortEdges),
    links: Array.from(linkMap.values()).sort(sortLinks),
  };

  return {
    graph,
    newNodeIds: new Set(graph.nodes.filter((node) => !currentNodeIds.has(node.id)).map((node) => node.id)),
    newEdgeIds: new Set(graph.edges.filter((edge) => !currentEdgeIds.has(edge.id)).map((edge) => edge.id)),
    newLinkIds: new Set(graph.links.filter((link) => !currentLinkIds.has(link.id)).map((link) => link.id)),
  };
}

export function layoutInitialGraph(
  graph: RegulatoryGraph,
  dimensions: GraphDimensions = DEFAULT_DIMENSIONS
): RegulatoryGraph {
  const next = cloneGraph(graph);
  if (!next.nodes.length) return next;

  const anchor = getOverviewAnchorNode(next) ?? next.nodes[0];
  if (!anchor) return next;

  const candidates = buildInitialLayoutCandidates(next, anchor, dimensions)
    .sort((a, b) => a.score - b.score);
  const best = candidates[0];
  if (!best) return next;

  applyPositions(next, best.positions);
  return finalizeGraphLayout(next, dimensions, SAFE_PADDING);
}

export function layoutFocusGraph(
  graph: RegulatoryGraph,
  anchorId: string,
  dimensions: GraphDimensions = DEFAULT_DIMENSIONS
): RegulatoryGraph {
  const local = createFocusSubgraph(graph, anchorId);
  const next = cloneGraph(local);
  const anchor = next.nodes.find((node) => node.id === anchorId) ?? next.nodes[0];
  if (!anchor) return layoutInitialGraph(next, dimensions);

  const center = { x: dimensions.width * 0.48, y: dimensions.height * 0.5 };
  const positions = new Map<string, GraphPoint>([[anchor.id, center]]);
  const directNeighbors = getDirectNeighborNodes(next, anchor.id)
    .sort(sortNodesForFocus())
    .filter((node) => node.id !== anchor.id);
  const directRadius = clamp(Math.min(dimensions.width, dimensions.height) * 0.28, 130, 205);
  const focusAngles = distributeOrganicAngles(directNeighbors.length, -Math.PI, Math.PI, anchor.id);

  directNeighbors.forEach((node, index) => {
    const preferredAngle = focusNeighborAngle(index, directNeighbors.length);
    const angle = blendAngles(preferredAngle, focusAngles[index] ?? preferredAngle, 0.32);
    const radius = directRadius + deterministicJitter(node.id, 24);
    positions.set(node.id, {
      x: center.x + Math.cos(angle) * radius,
      y: center.y + Math.sin(angle) * radius,
    });
  });

  const secondaryNodes = next.nodes.filter((node) => !positions.has(node.id));
  secondaryNodes.forEach((node, index) => {
    const parent = getDirectNeighborNodes(next, node.id).find((candidate) => positions.has(candidate.id));
    const basePosition = parent ? positions.get(parent.id) ?? { x: 0, y: 0 } : { x: 0, y: 0 };
    const parentAngle = Math.atan2(basePosition.y - center.y, basePosition.x - center.x);
    const angle = parentAngle + (index % 2 === 0 ? 0.58 : -0.58) + deterministicJitter(node.id, 0.18);
    positions.set(node.id, {
      x: basePosition.x + Math.cos(angle) * 110,
      y: basePosition.y + Math.sin(angle) * 110,
    });
  });

  applyPositions(next, positions);
  return finalizeGraphLayout(next, dimensions, SAFE_PADDING);
}

export function layoutProgressiveGraph(
  graph: RegulatoryGraph,
  previous: RegulatoryGraph,
  anchorId: string,
  newNodeIds: Set<string>,
  dimensions: GraphDimensions = DEFAULT_DIMENSIONS
): RegulatoryGraph {
  const next = cloneGraph(graph);
  const previousPositions = new Map(previous.nodes.map((node) => [node.id, { x: node.x, y: node.y }] as const));
  const positions = new Map<string, GraphPoint>();

  next.nodes.forEach((node) => {
    const previousPosition = previousPositions.get(node.id);
    if (previousPosition && !newNodeIds.has(node.id)) positions.set(node.id, previousPosition);
  });

  const anchor = next.nodes.find((node) => node.id === anchorId);
  if (!anchor || !positions.has(anchor.id)) {
    return layoutInitialGraph(next, dimensions);
  }

  const directNewNodes = next.nodes
    .filter((node) => newNodeIds.has(node.id))
    .sort(sortNodes)
    .sort((a, b) => Number(isDirectNeighbor(next, anchor.id, b.id)) - Number(isDirectNeighbor(next, anchor.id, a.id)));

  placeProgressiveRevealSector(next, positions, anchor, directNewNodes, dimensions);

  placeUnpositionedNodes(next, positions, dimensions);
  applyPositions(next, positions);
  return finalizeGraphLayout(next, dimensions, SAFE_PADDING);
}

export function applyInteractionState(
  graph: RegulatoryGraph,
  selectedNodeId: string,
  selectedEdgeId: string,
  hoveredNodeId: string,
  newlyAddedNodeIds: Set<string>,
  newlyAddedEdgeIds: Set<string>,
  hoveredEdgeId = ""
): RegulatoryGraph {
  const activeNodeId = hoveredNodeId || selectedNodeId;
  const selectedEdge = graph.edges.find((edge) => edge.id === selectedEdgeId);
  const activeEdgeId = hoveredEdgeId || (hoveredNodeId ? "" : selectedEdgeId);
  const activeEdge = graph.edges.find((edge) => edge.id === activeEdgeId);
  const adjacentEdgeIds = new Set<string>();
  const adjacentNodeIds = new Set<string>();

  const addEdgeContext = (edge: GraphEdge) => {
    adjacentEdgeIds.add(edge.id);
    adjacentNodeIds.add(edge.source);
    adjacentNodeIds.add(edge.target);
  };

  const addDirectNodeContext = (nodeId: string) => {
    graph.edges.forEach((edge) => {
      if (edge.source === nodeId || edge.target === nodeId) addEdgeContext(edge);
    });
  };

  const addNodePreviewContext = (nodeId: string) => {
    adjacentNodeIds.add(nodeId);
    addDirectNodeContext(nodeId);
  };

  const addEdgePreviewContext = (edge: GraphEdge) => {
    addEdgeContext(edge);
  };

  if (activeNodeId) {
    addNodePreviewContext(activeNodeId);
  }

  if (activeEdge) {
    addEdgePreviewContext(activeEdge);
  }

  const hasActiveSelection = Boolean(activeNodeId || activeEdge);

  return {
    ...graph,
    nodes: graph.nodes.map((node) => ({
      ...cloneNode(node),
      selected: node.id === selectedNodeId || Boolean(selectedEdge && (selectedEdge.source === node.id || selectedEdge.target === node.id)),
      newlyAdded: newlyAddedNodeIds.has(node.id),
      dimmed: hasActiveSelection && !adjacentNodeIds.has(node.id),
    })),
    edges: graph.edges.map((edge) => ({
      ...cloneEdge(edge),
      highlighted: adjacentEdgeIds.has(edge.id),
      newlyAdded: newlyAddedEdgeIds.has(edge.id),
      dimmed: hasActiveSelection && !adjacentEdgeIds.has(edge.id),
    })),
  };
}

export function getDirectNeighborNodes(graph: RegulatoryGraph, nodeId: string): GraphNode[] {
  const nodeMap = new Map(graph.nodes.map((node) => [node.id, node] as const));
  const neighborIds = new Set<string>();
  graph.edges.forEach((edge) => {
    if (edge.source === nodeId) neighborIds.add(edge.target);
    if (edge.target === nodeId) neighborIds.add(edge.source);
  });
  return Array.from(neighborIds)
    .map((id) => nodeMap.get(id))
    .filter((node): node is GraphNode => Boolean(node))
    .sort(sortNodes);
}

export function getPeakGeneLinksForGene(graph: RegulatoryGraph, geneId: string): GraphLink[] {
  const gene = graph.nodes.find((node) => node.id === geneId);
  if (!gene) return [];
  return graph.links
    .filter((link) => getGeneNodeId(link.geneSymbol) === gene.id)
    .sort(sortLinks);
}

export function getPeakGeneLinksForPeak(graph: RegulatoryGraph, peakId: string): GraphLink[] {
  const peak = graph.nodes.find((node) => node.id === peakId);
  if (!peak) return [];
  return graph.links
    .filter((link) => getPeakNodeId(link.peak) === peak.id)
    .sort(sortLinks);
}

export function getVisiblePeakGeneLinks(graph: RegulatoryGraph): GraphLink[] {
  const visiblePeakGeneEdges = new Set(graph.edges.filter((edge) => edge.type === "peakGene").map((edge) => edge.id));
  return graph.links
    .filter((link) => visiblePeakGeneEdges.has(graphEdgeId(getGeneNodeId(link.geneSymbol), getPeakNodeId(link.peak), "peakGene")))
    .sort(sortLinks);
}

export function selectDirectNeighborhoodSubgraph(
  graph: RegulatoryGraph,
  anchorId: string,
  neighborLimit = Number.POSITIVE_INFINITY
): RegulatoryGraph {
  const anchor = graph.nodes.find((node) => node.id === anchorId);
  if (!anchor) return emptyRegulatoryGraph();

  const nodeIds = new Set<string>([anchor.id]);
  const edgeIds = new Set<string>();
  const linkIds = new Set<string>();
  const safeNeighborLimit = Math.max(0, neighborLimit);

  if (anchor.type === "peak") {
    getPeakGeneLinksForPeak(graph, anchor.id).slice(0, safeNeighborLimit).forEach((link) => {
      const geneId = getGeneNodeId(link.geneSymbol);
      nodeIds.add(geneId);
      linkIds.add(link.id);
      const edge = getPeakGeneEdge(graph, anchor.id, geneId);
      if (edge) edgeIds.add(edge.id);
    });
  } else {
    getPeakGeneLinksForGene(graph, anchor.id).slice(0, safeNeighborLimit).forEach((link) => {
      const peakId = getPeakNodeId(link.peak);
      nodeIds.add(peakId);
      linkIds.add(link.id);
      const edge = getPeakGeneEdge(graph, peakId, anchor.id);
      if (edge) edgeIds.add(edge.id);
    });
  }

  const remainingNeighborSlots = Number.isFinite(safeNeighborLimit)
    ? Math.max(0, safeNeighborLimit - (nodeIds.size - 1))
    : Number.POSITIVE_INFINITY;
  getDirectNeighborNodes(graph, anchor.id)
    .filter((node) => !nodeIds.has(node.id))
    .slice(0, remainingNeighborSlots)
    .forEach((node) => nodeIds.add(node.id));

  const selectedNeighborIds = new Set(nodeIds);
  graph.edges.forEach((edge) => {
    const touchesAnchor = edge.source === anchor.id || edge.target === anchor.id;
    const sourceSelected = selectedNeighborIds.has(edge.source);
    const targetSelected = selectedNeighborIds.has(edge.target);
    if (touchesAnchor && sourceSelected && targetSelected) edgeIds.add(edge.id);
  });

  return filterGraphByIds(graph, nodeIds, edgeIds, linkIds);
}

export function getHighScorePeakIds(graph: RegulatoryGraph, limit = 5): Set<string> {
  return new Set(
    graph.links
      .slice()
      .sort(sortLinks)
      .slice(0, limit)
      .map((link) => getPeakNodeId(link.peak))
  );
}

export function shouldShowNodeLabel(
  node: GraphNode,
  visibleNodeCount: number,
  selectedNodeId: string,
  selectedEdge: GraphEdge | undefined,
  hoveredNodeId: string,
  highScorePeakIds: Set<string>
): boolean {
  if (node.type === "gene") return true;
  if (node.id === selectedNodeId || node.id === hoveredNodeId) return true;
  if (selectedEdge && (selectedEdge.source === node.id || selectedEdge.target === node.id)) return true;
  if (node.newlyAdded) return visibleNodeCount <= 34;
  return highScorePeakIds.has(node.id) && visibleNodeCount <= 34;
}

export function shortPeakLabel(label: string): string {
  const trimmed = label.trim();
  if (trimmed.length <= 24) return trimmed;
  const match = trimmed.match(/^(chr[^:]+):(\d+)-(\d+)$/i);
  if (match) {
    const [, chromosome, start, end] = match;
    return `${chromosome}:${compactNumber(Number(start))}-${compactNumber(Number(end))}`;
  }
  return `${trimmed.slice(0, 11)}...${trimmed.slice(-8)}`;
}

export function estimateLabelWidth(label: string, nodeType: GraphNodeType): number {
  const fontSize = nodeType === "peak" ? 11 : 13;
  const cappedLabel = nodeType === "peak" ? shortPeakLabel(label) : label;
  return Math.max(nodeType === "peak" ? 54 : 32, cappedLabel.length * fontSize * 0.58 + 12);
}

export function normalizeNodeType(value: unknown): GraphNodeType {
  const normalized = String(value ?? "").trim().toLowerCase();
  if (normalized === "peak") return "peak";
  return "gene";
}

function normalizeRawNode(rawNode: RegulatoryNetworkNode): GraphNode | null {
  const record = rawNode as unknown as ReadableRecord;
  const type = normalizeNodeType(rawNode.type);
  const label = String(rawNode.label ?? "").trim() || getNodeIdValue(rawNode.id, type);
  if (!label) return null;

  return {
    id: rawNode.id || nodeIdForType(type, label),
    type,
    label,
    x: DEFAULT_DIMENSIONS.width / 2,
    y: DEFAULT_DIMENSIONS.height / 2,
    radius: NODE_RADIUS[type],
    visible: true,
    selected: false,
    newlyAdded: false,
    dimmed: false,
    metadata: {
      datasetId: readString(record, "datasetId", "dataset_id"),
      sampleName: readString(record, "sampleName", "sample_name"),
      domain: readString(record, "domain"),
      chromosome: cleanString(rawNode.chromosome),
      start: cleanNumber(rawNode.start) ?? undefined,
      end: cleanNumber(rawNode.end) ?? undefined,
      source: readString(record, "source", "Source"),
      linkedGenesCount: readNumber(record, "linkedGenesCount", "linked_genes_count"),
      topLinkedGenes: readStringList(record, "topLinkedGenes", "top_linked_genes"),
      remainingLinkedGenesCount: readNumber(record, "remainingLinkedGenesCount", "remaining_linked_genes_count"),
      linkedPeaksCount: readNumber(record, "linkedPeaksCount", "linked_peaks_count"),
      topLinkedPeaks: readStringList(record, "topLinkedPeaks", "top_linked_peaks"),
      remainingLinkedPeaksCount: readNumber(record, "remainingLinkedPeaksCount", "remaining_linked_peaks_count"),
      maxLinkScore: readNumber(record, "maxLinkScore", "max_link_score"),
      correlationMin: readNumber(record, "correlationMin", "correlation_min"),
      correlationMax: readNumber(record, "correlationMax", "correlation_max"),
      fdrMin: readNumber(record, "fdrMin", "fdr_min"),
      fdrMax: readNumber(record, "fdrMax", "fdr_max"),
      minFdr: readNumber(record, "minFdr", "min_fdr"),
      totalLinks: readNumber(record, "totalLinks", "total_links"),
      raw: rawNode,
    },
  };
}

function normalizeRawLink(rawLink: RegulatoryNetworkLink): GraphLink | null {
  const record = rawLink as ReadableRecord;
  const peak = readString(record, "peak", "peakId", "Peak_Name", "peakName");
  const gene = readString(record, "linkedGene", "geneSymbol", "Gene_Name", "geneName");
  if (!peak || !gene) return null;

  const score = readNumber(record, "linkScore", "score", "Score");
  const correlation = readNumber(record, "correlation", "Correlation");
  const fdr = readNumber(record, "fdr", "FDR");
  const distanceToTss = readNumber(record, "distanceToTss", "distance_to_tss", "DistanceToTSS", "Distance_To_TSS");
  const varQAtac = readNumber(record, "varQAtac", "VarQATAC", "var_q_atac");
  const varQRna = readNumber(record, "varQRna", "VarQRNA", "var_q_rna");

  return {
    id: graphLinkId(peak, gene),
    peakId: peak,
    peak,
    geneSymbol: gene,
    linkedGene: gene,
    datasetId: readString(record, "datasetId", "dataset_id"),
    sampleName: readString(record, "sampleName", "sample_name"),
    domain: readString(record, "domain"),
    score,
    linkScore: score,
    correlation,
    fdr,
    distanceToTss,
    varQAtac,
    varQRna,
    linkType: readString(record, "linkType", "link_type") ?? "peak_to_gene",
    source: readString(record, "source", "Source"),
    linkedGenesCount: readNumber(record, "linkedGenesCount", "linked_genes_count"),
    topLinkedGenes: readStringList(record, "topLinkedGenes", "top_linked_genes"),
    remainingLinkedGenesCount: readNumber(record, "remainingLinkedGenesCount", "remaining_linked_genes_count"),
    linkedPeaksCount: readNumber(record, "linkedPeaksCount", "linked_peaks_count"),
    topLinkedPeaks: readStringList(record, "topLinkedPeaks", "top_linked_peaks"),
    remainingLinkedPeaksCount: readNumber(record, "remainingLinkedPeaksCount", "remaining_linked_peaks_count"),
    maxLinkScore: readNumber(record, "maxLinkScore", "max_link_score"),
    correlationMin: readNumber(record, "correlationMin", "correlation_min"),
    correlationMax: readNumber(record, "correlationMax", "correlation_max"),
    fdrMin: readNumber(record, "fdrMin", "fdr_min"),
    fdrMax: readNumber(record, "fdrMax", "fdr_max"),
    minFdr: readNumber(record, "minFdr", "min_fdr"),
    totalLinks: readNumber(record, "totalLinks", "total_links"),
  };
}

function normalizeRawEdge(rawEdge: RegulatoryNetworkEdge): GraphEdge | null {
  const source = String(rawEdge.source ?? "").trim();
  const target = String(rawEdge.target ?? "").trim();
  if (!source || !target) return null;

  const type: GraphEdgeType = "peakGene";
  return {
    id: graphEdgeId(source, target, type),
    source,
    target,
    type,
    score: cleanNumber(rawEdge.score),
    correlation: cleanNumber(rawEdge.correlation),
    fdr: cleanNumber(rawEdge.fdr),
    distanceToTss: cleanNumber(rawEdge.distanceToTss),
    varQAtac: cleanNumber(rawEdge.varQAtac),
    varQRna: cleanNumber(rawEdge.varQRna),
    sourceMethod: cleanString(rawEdge.sourceMethod),
    visible: true,
    highlighted: false,
    newlyAdded: false,
    dimmed: false,
    curve: 0,
  };
}

function createGeneNode(gene: string, metadata: GraphNodeMetadata = {}): GraphNode {
  return {
    id: getGeneNodeId(gene),
    type: "gene",
    label: gene,
    x: DEFAULT_DIMENSIONS.width / 2,
    y: DEFAULT_DIMENSIONS.height / 2,
    radius: NODE_RADIUS.gene,
    visible: true,
    selected: false,
    newlyAdded: false,
    dimmed: false,
    metadata,
  };
}

function createPeakNode(peak: string, metadata: GraphNodeMetadata = {}): GraphNode {
  const parsed = parsePeakRegion(peak);
  return {
    id: getPeakNodeId(peak),
    type: "peak",
    label: peak,
    x: DEFAULT_DIMENSIONS.width / 2,
    y: DEFAULT_DIMENSIONS.height / 2,
    radius: NODE_RADIUS.peak,
    visible: true,
    selected: false,
    newlyAdded: false,
    dimmed: false,
    metadata: {
      ...metadata,
      ...parsed,
    },
  };
}

function createPeakGeneEdge(link: GraphLink): GraphEdge {
  const source = getGeneNodeId(link.geneSymbol);
  const target = getPeakNodeId(link.peak);
  return {
    id: graphEdgeId(source, target, "peakGene"),
    source,
    target,
    type: "peakGene",
    score: link.score ?? link.linkScore,
    correlation: link.correlation,
    fdr: link.fdr,
    distanceToTss: link.distanceToTss,
    varQAtac: link.varQAtac,
    varQRna: link.varQRna,
    sourceMethod: link.source,
    visible: true,
    highlighted: false,
    newlyAdded: false,
    dimmed: false,
    curve: 0,
  };
}

function selectGenePeakInitialSubgraph(graph: RegulatoryGraph): RegulatoryGraph {
  const geneIds = getGeneNodes(graph)
    .sort((a, b) => genePriority(graph, b.id) - genePriority(graph, a.id))
    .slice(0, INITIAL_GENE_LIMIT)
    .map((node) => node.id);
  const nodeIds = new Set<string>(geneIds);
  const edgeIds = new Set<string>();
  const linkIds = new Set<string>();

  geneIds.forEach((geneId) => {
    getPeakGeneLinksForGene(graph, geneId).slice(0, INITIAL_PEAK_LIMIT_PER_GENE).forEach((link) => {
      const peakId = getPeakNodeId(link.peak);
      nodeIds.add(peakId);
      linkIds.add(link.id);
      const edge = getPeakGeneEdge(graph, peakId, geneId);
      if (edge) edgeIds.add(edge.id);
    });
  });

  return filterGraphByIds(graph, nodeIds, edgeIds, linkIds);
}

function createFocusSubgraph(graph: RegulatoryGraph, anchorId: string): RegulatoryGraph {
  const anchor = graph.nodes.find((node) => node.id === anchorId);
  if (!anchor) return selectInitialSubgraph(graph);

  const nodeIds = new Set<string>([anchor.id]);
  const edgeIds = new Set<string>();
  const linkIds = new Set<string>();

  if (anchor.type === "peak") {
    getPeakGeneLinksForPeak(graph, anchor.id).slice(0, FOCUS_GENE_LIMIT).forEach((link) => {
      const geneId = getGeneNodeId(link.geneSymbol);
      nodeIds.add(geneId);
      linkIds.add(link.id);
      const edge = getPeakGeneEdge(graph, anchor.id, geneId);
      if (edge) edgeIds.add(edge.id);
    });
  } else {
    getPeakGeneLinksForGene(graph, anchor.id).slice(0, FOCUS_PEAK_LIMIT).forEach((link) => {
      const peakId = getPeakNodeId(link.peak);
      nodeIds.add(peakId);
      linkIds.add(link.id);
      const edge = getPeakGeneEdge(graph, peakId, anchor.id);
      if (edge) edgeIds.add(edge.id);
    });
  }

  return filterGraphByIds(graph, nodeIds, edgeIds, linkIds);
}

function filterGraphByIds(
  graph: RegulatoryGraph,
  nodeIds: Set<string>,
  edgeIds: Set<string>,
  linkIds: Set<string>
): RegulatoryGraph {
  const nodes = graph.nodes.filter((node) => nodeIds.has(node.id)).map(cloneNode);
  const nodeIdSet = new Set(nodes.map((node) => node.id));
  return {
    ...graph,
    nodes,
    edges: graph.edges
      .filter((edge) => edgeIds.has(edge.id) && nodeIdSet.has(edge.source) && nodeIdSet.has(edge.target))
      .map(cloneEdge),
    links: graph.links.filter((link) => linkIds.has(link.id)).map((link) => ({ ...link })),
  };
}

function finalizeGraphLayout(graph: RegulatoryGraph, dimensions: GraphDimensions, padding: number): RegulatoryGraph {
  resolveGraphCollisions(graph, dimensions);
  fitGraphToView(graph, dimensions, padding);
  resolveGraphCollisions(graph, dimensions);
  fitGraphToView(graph, dimensions, padding);
  refineEdgeRoutesWithNudging(graph, dimensions);
  return graph;
}

function getOverviewAnchorNode(graph: RegulatoryGraph): GraphNode | undefined {
  return getGeneNodes(graph).sort((a, b) => genePriority(graph, b.id) - genePriority(graph, a.id))[0]
    ?? graph.nodes.find((node) => node.type !== "peak")
    ?? graph.nodes[0];
}

function getOverviewGeneNodes(graph: RegulatoryGraph, anchor: GraphNode): GraphNode[] {
  if (anchor.type === "gene") {
    const neighborGenes = getDirectNeighborNodes(graph, anchor.id).filter((node) => node.type === "gene" && node.id !== anchor.id);
    return [anchor, ...neighborGenes];
  }
  return getGeneNodes(graph);
}

function buildInitialLayoutCandidates(
  graph: RegulatoryGraph,
  anchor: GraphNode,
  dimensions: GraphDimensions
): InitialLayoutCandidate[] {
  const candidates: InitialLayoutCandidate[] = [];

  for (let index = 0; index < INITIAL_LAYOUT_CANDIDATE_COUNT; index += 1) {
    const center = {
      x: dimensions.width * (0.44 + (index % 5) * 0.025),
      y: dimensions.height * (0.47 + (Math.floor(index / 5) - 1) * 0.035),
    };
    const angleOffset = (-0.34 + (index % 5) * 0.17) + deterministicJitter(`${anchor.id}:initial:${index}`, 0.035);
    const radialScale = 0.96 + (Math.floor(index / 5) % 3) * 0.055;
    const positions = buildInitialOverviewPositions(graph, anchor, center, dimensions, angleOffset, radialScale);

    refineInitialGenePositions(graph, positions, anchor, center, dimensions, angleOffset, radialScale);
    relaxInitialCandidatePositions(graph, positions, dimensions);
    candidates.push({
      positions,
      score: scoreOverviewLayout(graph, positions, dimensions),
    });
  }

  return candidates;
}

function buildInitialOverviewPositions(
  graph: RegulatoryGraph,
  anchor: GraphNode,
  center: GraphPoint,
  dimensions: GraphDimensions,
  angleOffset: number,
  radialScale: number
): Map<string, GraphPoint> {
  const positions = new Map<string, GraphPoint>();
  positions.set(anchor.id, clampPoint(center, anchor, dimensions));

  const primaryGenes = getOverviewGeneNodes(graph, anchor)
    .sort((a, b) => genePriority(graph, b.id) - genePriority(graph, a.id));
  const geneRadius = clamp(Math.min(dimensions.width, dimensions.height) * 0.32 * radialScale, 132, 224);
  const geneAngles = distributeOrganicAngles(primaryGenes.length, -Math.PI * 0.86, Math.PI * 0.86, anchor.id);

  primaryGenes.forEach((gene, index) => {
    const angle = (geneAngles[index] ?? 0) + angleOffset;
    const radius = geneRadius + deterministicJitter(gene.id, 24);
    positions.set(gene.id, clampPoint({
      x: center.x + Math.cos(angle) * radius,
      y: center.y + Math.sin(angle) * radius,
    }, gene, dimensions));
  });

  placeOverviewPeaks(graph, positions, center, dimensions, angleOffset, radialScale);
  placeUnpositionedNodes(graph, positions, dimensions);

  return positions;
}

function placeOverviewPeaks(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  center: GraphPoint,
  dimensions: GraphDimensions,
  angleOffset = 0,
  radialScale = 1
) {
  const positionedGenes = graph.nodes
    .filter((node) => node.type === "gene" && positions.has(node.id))
    .sort((a, b) => genePriority(graph, b.id) - genePriority(graph, a.id));
  const peakOwnerIds = getOverviewPeakOwnerIds(graph, positionedGenes);

  positionedGenes.forEach((gene) => {
    placePeakClusterForGene(graph, positions, gene, peakOwnerIds, center, dimensions, angleOffset, radialScale);
  });
}

function refineInitialGenePositions(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  anchor: GraphNode,
  center: GraphPoint,
  dimensions: GraphDimensions,
  angleOffset: number,
  radialScale: number
) {
  const genes = graph.nodes
    .filter((node) => node.type === "gene" && positions.has(node.id))
    .sort((a, b) => genePriority(graph, b.id) - genePriority(graph, a.id));
  if (!genes.length) return;

  const peakOwnerIds = getOverviewPeakOwnerIds(graph, genes);
  const anchorPoint = positions.get(anchor.id) ?? center;

  for (let pass = 0; pass < INITIAL_GENE_REFINEMENT_PASSES; pass += 1) {
    genes.forEach((gene) => {
      const current = positions.get(gene.id);
      if (!current) return;

      const candidates = buildGenePositionCandidates(gene, current, anchorPoint, dimensions);
      const scoredCandidates = candidates
        .map((point) => {
          const candidatePositions = clonePositions(positions);
          candidatePositions.set(gene.id, point);
          placePeakClusterForGene(graph, candidatePositions, gene, peakOwnerIds, center, dimensions, angleOffset, radialScale);
          return {
            point,
            positions: candidatePositions,
            score: scoreGenePositionCandidate(graph, gene, candidatePositions, dimensions),
          };
        })
        .sort((a, b) => a.score - b.score);

      const best = scoredCandidates[0];
      if (!best) return;
      positions.set(gene.id, best.point);
      placePeakClusterForGene(graph, positions, gene, peakOwnerIds, center, dimensions, angleOffset, radialScale);
    });
  }
}

function buildGenePositionCandidates(
  gene: GraphNode,
  current: GraphPoint,
  anchorPoint: GraphPoint,
  dimensions: GraphDimensions
): GraphPoint[] {
  const dx = current.x - anchorPoint.x;
  const dy = current.y - anchorPoint.y;
  const distance = Math.max(1, Math.hypot(dx, dy));
  const radial = { x: dx / distance, y: dy / distance };
  const tangent = { x: -radial.y, y: radial.x };
  const angle = Math.atan2(dy, dx);
  const rawCandidates: GraphPoint[] = [
    current,
    { x: current.x, y: current.y - 54 },
    { x: current.x, y: current.y + 54 },
    { x: current.x + 56, y: current.y - 46 },
    { x: current.x + 56, y: current.y + 46 },
    {
      x: current.x + radial.x * 54,
      y: current.y + radial.y * 54,
    },
    {
      x: current.x + radial.x * 92,
      y: current.y + radial.y * 92,
    },
    {
      x: current.x + tangent.x * 54,
      y: current.y + tangent.y * 54,
    },
    {
      x: current.x - tangent.x * 54,
      y: current.y - tangent.y * 54,
    },
    {
      x: current.x + radial.x * 46 + tangent.x * 48,
      y: current.y + radial.y * 46 + tangent.y * 48,
    },
    {
      x: current.x + radial.x * 46 - tangent.x * 48,
      y: current.y + radial.y * 46 - tangent.y * 48,
    },
    rotatedGeneCandidate(anchorPoint, angle + 0.22, distance + 26),
    rotatedGeneCandidate(anchorPoint, angle - 0.22, distance + 26),
    rotatedGeneCandidate(anchorPoint, angle + 0.4, distance + 62),
    rotatedGeneCandidate(anchorPoint, angle - 0.4, distance + 62),
  ];

  const seen = new Set<string>();
  const candidates: GraphPoint[] = [];
  rawCandidates.forEach((point) => {
    const clamped = clampPoint(point, gene, dimensions);
    const key = `${Math.round(clamped.x)}:${Math.round(clamped.y)}`;
    if (seen.has(key)) return;
    seen.add(key);
    candidates.push(clamped);
  });

  return candidates;
}

function rotatedGeneCandidate(anchorPoint: GraphPoint, angle: number, radius: number): GraphPoint {
  return {
    x: anchorPoint.x + Math.cos(angle) * radius,
    y: anchorPoint.y + Math.sin(angle) * radius,
  };
}

function scoreGenePositionCandidate(
  graph: RegulatoryGraph,
  gene: GraphNode,
  positions: Map<string, GraphPoint>,
  dimensions: GraphDimensions
): number {
  const genePosition = positions.get(gene.id);
  if (!genePosition) return Number.POSITIVE_INFINITY;

  let score = scoreOverviewLayout(graph, positions, dimensions);
  const geneLabelBox = getNodeLabelBox(gene, genePosition, true, dimensions);

  graph.nodes.forEach((node) => {
    if (node.id === gene.id) return;
    const point = positions.get(node.id);
    if (!point) return;

    const distance = Math.hypot(genePosition.x - point.x, genePosition.y - point.y);
    const minDistance = gene.radius + node.radius + 30;
    if (distance < minDistance) score += 90000 + (minDistance - distance) * 1500;

    const nodeBox = getNodeCircleBox(node, point);
    if (boxesOverlap(geneLabelBox, nodeBox, 5)) score += node.type === "peak" ? 6200 : 11800;

    const labelBox = getNodeLabelBox(node, point, true, dimensions);
    if (boxesOverlap(geneLabelBox, labelBox, 5)) score += node.type === "peak" ? 4800 : 10400;
  });

  graph.edges.forEach((edge) => {
    if (edge.source !== gene.id && edge.target !== gene.id) return;
    const source = positions.get(edge.source);
    const target = positions.get(edge.target);
    if (!source || !target) return;

    score += scoreLineObstacles(graph, edge, source, target, positions, dimensions) * 2.4;

    graph.edges.forEach((otherEdge) => {
      if (edge.id === otherEdge.id || edgesShareEndpoint(edge, otherEdge)) return;
      const otherSource = positions.get(otherEdge.source);
      const otherTarget = positions.get(otherEdge.target);
      if (!otherSource || !otherTarget) return;
      if (segmentsIntersect(source, target, otherSource, otherTarget)) score += 7200;
    });
  });

  score += boundaryPenalty(genePosition, gene, dimensions) * 90;
  return score;
}

function placePeakClusterForGene(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  gene: GraphNode,
  peakOwnerIds: Map<string, string>,
  center: GraphPoint,
  dimensions: GraphDimensions,
  angleOffset = 0,
  radialScale = 1
) {
  const genePosition = positions.get(gene.id);
  if (!genePosition) return;

  const peaks = getPeakGeneLinksForGene(graph, gene.id)
    .map((link) => graph.nodes.find((node) => node.id === getPeakNodeId(link.peak)))
    .filter((node): node is GraphNode => Boolean(node))
    .filter((peak) => peakOwnerIds.get(peak.id) === gene.id)
    .sort((a, b) => peakPriority(graph, b.id) - peakPriority(graph, a.id));
  const baseAngle = Math.atan2(genePosition.y - center.y, genePosition.x - center.x) + angleOffset * 0.28;
  const spread = Math.min(Math.PI * 0.96, 0.34 + peaks.length * 0.18);
  const radius = clamp(Math.min(dimensions.width, dimensions.height) * 0.18 * radialScale, 84, 142);

  peaks.forEach((peak, peakIndex) => {
    const fanOffset = peaks.length <= 1 ? 0 : -spread / 2 + (spread * peakIndex) / (peaks.length - 1);
    const angle = baseAngle + fanOffset + deterministicJitter(`${gene.id}:${peak.id}`, ORGANIC_RING_JITTER);
    positions.set(peak.id, clampPoint({
      x: genePosition.x + Math.cos(angle) * (radius + deterministicJitter(peak.id, 16)),
      y: genePosition.y + Math.sin(angle) * (radius + deterministicJitter(`${peak.id}:y`, 12)),
    }, peak, dimensions));
  });
}

function getOverviewPeakOwnerIds(graph: RegulatoryGraph, genes: GraphNode[]): Map<string, string> {
  const ownerIds = new Map<string, string>();

  genes.forEach((gene) => {
    getPeakGeneLinksForGene(graph, gene.id)
      .map((link) => getPeakNodeId(link.peak))
      .forEach((peakId) => {
        if (!ownerIds.has(peakId)) ownerIds.set(peakId, gene.id);
      });
  });

  return ownerIds;
}

function clonePositions(positions: Map<string, GraphPoint>): Map<string, GraphPoint> {
  return new Map(Array.from(positions.entries()).map(([nodeId, point]) => [nodeId, { ...point }] as const));
}

function relaxInitialCandidatePositions(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  dimensions: GraphDimensions
) {
  const peakLabelIds = getPeakLabelReserveIds(graph);

  for (let iteration = 0; iteration < INITIAL_RELAX_ITERATIONS; iteration += 1) {
    let moved = false;

    for (let i = 0; i < graph.nodes.length; i += 1) {
      for (let j = i + 1; j < graph.nodes.length; j += 1) {
        const leftNode = graph.nodes[i];
        const rightNode = graph.nodes[j];
        if (!leftNode || !rightNode) continue;
        const leftPoint = positions.get(leftNode.id);
        const rightPoint = positions.get(rightNode.id);
        if (!leftPoint || !rightPoint) continue;

        const leftBox = getNodeCollisionBox(leftNode, leftPoint, dimensions, peakLabelIds);
        const rightBox = getNodeCollisionBox(rightNode, rightPoint, dimensions, peakLabelIds);
        if (!boxesOverlap(leftBox, rightBox, COLLISION_PADDING * 0.72)) continue;

        const leftCenter = boxCenter(leftBox);
        const rightCenter = boxCenter(rightBox);
        const dx = rightCenter.x - leftCenter.x;
        const dy = rightCenter.y - leftCenter.y;
        const distance = Math.max(0.001, Math.hypot(dx, dy));
        const overlapX = Math.min(leftBox.right + COLLISION_PADDING - rightBox.left, rightBox.right + COLLISION_PADDING - leftBox.left);
        const overlapY = Math.min(leftBox.bottom + COLLISION_PADDING - rightBox.top, rightBox.bottom + COLLISION_PADDING - leftBox.top);
        const push = Math.max(0.6, Math.min(overlapX, overlapY) * 0.36);
        const ux = dx / distance;
        const uy = dy / distance;

        positions.set(leftNode.id, clampPoint({
          x: leftPoint.x - ux * push * 0.46,
          y: leftPoint.y - uy * push * 0.46,
        }, leftNode, dimensions));
        positions.set(rightNode.id, clampPoint({
          x: rightPoint.x + ux * push * 0.54,
          y: rightPoint.y + uy * push * 0.54,
        }, rightNode, dimensions));
        moved = true;
      }
    }

    graph.edges.forEach((edge) => {
      const source = positions.get(edge.source);
      const target = positions.get(edge.target);
      if (!source || !target) return;

      graph.nodes.forEach((node) => {
        if (node.id === edge.source || node.id === edge.target) return;
        const point = positions.get(node.id);
        if (!point) return;
        const clearance = node.radius + 15;
        const distance = distanceToSegment(point, source, target);
        if (distance >= clearance) return;

        const dx = target.x - source.x;
        const dy = target.y - source.y;
        const length = Math.max(1, Math.hypot(dx, dy));
        const side = Math.sign((point.x - source.x) * dy - (point.y - source.y) * dx) || 1;
        const strength = (clearance - distance) * 0.26;
        positions.set(node.id, clampPoint({
          x: point.x - (dy / length) * side * strength,
          y: point.y + (dx / length) * side * strength,
        }, node, dimensions));
        moved = true;
      });
    });

    if (!moved) break;
  }
}

function scoreOverviewLayout(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  dimensions: GraphDimensions
): number {
  const peakLabelIds = getPeakLabelReserveIds(graph);
  let score = 0;

  graph.nodes.forEach((node) => {
    const point = positions.get(node.id);
    if (!point) {
      score += 100000;
      return;
    }
    score += boundaryPenalty(point, node, dimensions) * 54;
  });

  for (let i = 0; i < graph.nodes.length; i += 1) {
    for (let j = i + 1; j < graph.nodes.length; j += 1) {
      const leftNode = graph.nodes[i];
      const rightNode = graph.nodes[j];
      if (!leftNode || !rightNode) continue;
      const leftPoint = positions.get(leftNode.id);
      const rightPoint = positions.get(rightNode.id);
      if (!leftPoint || !rightPoint) continue;

      const distance = Math.hypot(leftPoint.x - rightPoint.x, leftPoint.y - rightPoint.y);
      const minDistance = leftNode.radius + rightNode.radius + 22;
      if (distance < minDistance) score += 120000 + (minDistance - distance) * 1700;

      const leftLabel = getNodeLabelBox(leftNode, leftPoint, true, dimensions);
      const rightLabel = getNodeLabelBox(rightNode, rightPoint, true, dimensions);
      if (boxesOverlap(leftLabel, rightLabel, 5)) {
        const protectedOverlap = leftNode.type !== "peak" || rightNode.type !== "peak";
        score += protectedOverlap ? 16500 : 6200;
      }

      const leftBox = getNodeCollisionBox(leftNode, leftPoint, dimensions, peakLabelIds);
      const rightBox = getNodeCollisionBox(rightNode, rightPoint, dimensions, peakLabelIds);
      if (boxesOverlap(leftBox, rightBox, COLLISION_PADDING)) score += 11500;
    }
  }

  graph.edges.forEach((edge) => {
    const source = positions.get(edge.source);
    const target = positions.get(edge.target);
    if (!source || !target) return;
    score += scoreLineObstacles(graph, edge, source, target, positions, dimensions);
  });

  for (let i = 0; i < graph.edges.length; i += 1) {
    for (let j = i + 1; j < graph.edges.length; j += 1) {
      const leftEdge = graph.edges[i];
      const rightEdge = graph.edges[j];
      if (!leftEdge || !rightEdge || edgesShareEndpoint(leftEdge, rightEdge)) continue;
      const leftSource = positions.get(leftEdge.source);
      const leftTarget = positions.get(leftEdge.target);
      const rightSource = positions.get(rightEdge.source);
      const rightTarget = positions.get(rightEdge.target);
      if (!leftSource || !leftTarget || !rightSource || !rightTarget) continue;
      if (segmentsIntersect(leftSource, leftTarget, rightSource, rightTarget)) score += 3600;
    }
  }

  return score;
}

function scoreLineObstacles(
  graph: RegulatoryGraph,
  edge: GraphEdge,
  source: GraphPoint,
  target: GraphPoint,
  positions: Map<string, GraphPoint>,
  dimensions: GraphDimensions
): number {
  let score = 0;

  graph.nodes.forEach((node) => {
    if (node.id === edge.source || node.id === edge.target) return;
    const point = positions.get(node.id);
    if (!point) return;

    const nodeClearance = node.radius + 11;
    const nodeDistance = distanceToSegment(point, source, target);
    if (nodeDistance < nodeClearance) score += 25000 + (nodeClearance - nodeDistance) * 1100;

    const labelBox = getNodeLabelBox(node, point, true, dimensions);
    if (segmentIntersectsBox(source, target, labelBox)) {
      score += node.type === "peak" ? 7200 : 11200;
    }
  });

  return score;
}

function placeProgressiveRevealSector(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  anchor: GraphNode,
  newNodes: GraphNode[],
  dimensions: GraphDimensions
) {
  if (!newNodes.length) return;

  const anchorPosition = positions.get(anchor.id) ?? { x: dimensions.width / 2, y: dimensions.height / 2 };
  const candidates = Array.from({ length: REVEAL_SECTOR_COUNT }, (_, index) => {
    const baseAngle = -Math.PI + (Math.PI * 2 * index) / REVEAL_SECTOR_COUNT;
    const candidatePositions = buildRevealSectorCandidate(graph, positions, anchor, newNodes, anchorPosition, baseAngle, dimensions);
    return {
      positions: candidatePositions,
      score: scoreRevealSector(graph, positions, candidatePositions, anchor, dimensions),
    };
  }).sort((a, b) => a.score - b.score);

  const best = candidates[0]?.positions ?? new Map<string, GraphPoint>();
  best.forEach((point, nodeId) => positions.set(nodeId, point));

  nudgeLocalOldNodes(graph, positions, anchor, newNodes, dimensions);
}

function buildRevealSectorCandidate(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  anchor: GraphNode,
  newNodes: GraphNode[],
  anchorPosition: GraphPoint,
  baseAngle: number,
  dimensions: GraphDimensions
): Map<string, GraphPoint> {
  const candidatePositions = new Map<string, GraphPoint>();
  const sortedNodes = newNodes.slice().sort((a, b) => {
    return Number(isDirectNeighbor(graph, anchor.id, b.id)) - Number(isDirectNeighbor(graph, anchor.id, a.id)) || sortNodes(a, b);
  });

  sortedNodes.forEach((node, index) => {
    const parent = getNearestPlacedNeighbor(graph, positions, candidatePositions, anchor, node);
    const parentPosition = candidatePositions.get(parent.id) ?? positions.get(parent.id) ?? anchorPosition;
    const localIndex = sortedNodes.length <= 1 ? 0 : index / (sortedNodes.length - 1);
    const angle = baseAngle - REVEAL_SECTOR_SPREAD / 2
      + REVEAL_SECTOR_SPREAD * localIndex
      + deterministicJitter(`${anchor.id}:${node.id}`, 0.16);
    const radius = (node.type === "peak" ? 98 : 126)
      + Math.floor(index / 5) * 42
      + deterministicJitter(node.id, 14);

    candidatePositions.set(node.id, clampPoint({
      x: parentPosition.x + Math.cos(angle) * radius,
      y: parentPosition.y + Math.sin(angle) * radius,
    }, node, dimensions));
  });

  return candidatePositions;
}

function scoreRevealSector(
  graph: RegulatoryGraph,
  existingPositions: Map<string, GraphPoint>,
  candidatePositions: Map<string, GraphPoint>,
  anchor: GraphNode,
  dimensions: GraphDimensions
): number {
  const mergedPositions = new Map([...existingPositions, ...candidatePositions]);
  const placedNodes = graph.nodes.filter((node) => existingPositions.has(node.id) && !candidatePositions.has(node.id));
  let score = 0;

  candidatePositions.forEach((point, nodeId) => {
    const node = graph.nodes.find((candidate) => candidate.id === nodeId);
    if (!node) return;

    const parent = getNearestPlacedNeighbor(graph, existingPositions, candidatePositions, anchor, node);
    const parentPosition = mergedPositions.get(parent.id) ?? mergedPositions.get(anchor.id) ?? point;
    const candidateScore = scoreCandidatePosition({
      graph,
      node,
      anchor: parent,
      anchorPosition: parentPosition,
      point,
      placedNodes,
      positions: mergedPositions,
      dimensions,
    });
    score += candidateScore.score + candidateScore.crossingCount * 3400;
  });

  candidatePositions.forEach((point, nodeId) => {
    const node = graph.nodes.find((candidate) => candidate.id === nodeId);
    if (!node) return;
    score += edgeNodeRoutePenalty(graph, node.id, point, mergedPositions);
  });

  return score;
}

function nudgeLocalOldNodes(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  anchor: GraphNode,
  newNodes: GraphNode[],
  dimensions: GraphDimensions
) {
  const anchorPosition = positions.get(anchor.id);
  if (!anchorPosition) return;
  const newNodeIds = new Set(newNodes.map((node) => node.id));
  const newCenters = newNodes
    .map((node) => positions.get(node.id))
    .filter((point): point is GraphPoint => Boolean(point));

  graph.nodes.forEach((node) => {
    if (node.id === anchor.id || newNodeIds.has(node.id)) return;
    const position = positions.get(node.id);
    if (!position) return;
    const nearestNew = newCenters
      .map((point) => ({ point, distance: Math.hypot(point.x - position.x, point.y - position.y) }))
      .sort((a, b) => a.distance - b.distance)[0];
    if (!nearestNew || nearestNew.distance > 135) return;

    const dx = position.x - nearestNew.point.x;
    const dy = position.y - nearestNew.point.y;
    const distance = Math.max(1, Math.hypot(dx, dy));
    const strength = (135 - nearestNew.distance) * 0.12;
    positions.set(node.id, clampPoint({
      x: position.x + (dx / distance) * strength,
      y: position.y + (dy / distance) * strength,
    }, node, dimensions));
  });
}

function getNearestPlacedNeighbor(
  graph: RegulatoryGraph,
  existingPositions: Map<string, GraphPoint>,
  candidatePositions: Map<string, GraphPoint>,
  fallback: GraphNode,
  node: GraphNode
): GraphNode {
  const allPositions = new Map([...existingPositions, ...candidatePositions]);
  return getDirectNeighborNodes(graph, node.id)
    .filter((neighbor) => allPositions.has(neighbor.id))
    .sort((a, b) => {
      const aPoint = allPositions.get(a.id);
      const bPoint = allPositions.get(b.id);
      if (!aPoint || !bPoint) return 0;
      const nodePoint = allPositions.get(node.id) ?? allPositions.get(fallback.id) ?? { x: 0, y: 0 };
      return Math.hypot(aPoint.x - nodePoint.x, aPoint.y - nodePoint.y)
        - Math.hypot(bPoint.x - nodePoint.x, bPoint.y - nodePoint.y);
    })[0] ?? fallback;
}

function placeUnpositionedNodes(
  graph: RegulatoryGraph,
  positions: Map<string, GraphPoint>,
  dimensions: GraphDimensions = DEFAULT_DIMENSIONS
) {
  const missingNodes = graph.nodes.filter((node) => !positions.has(node.id)).sort(sortNodes);
  if (!missingNodes.length) return;

  const center = { x: dimensions.width * 0.5, y: dimensions.height * 0.5 };
  const angles = distributeOrganicAngles(missingNodes.length, -Math.PI, Math.PI, "missing");
  missingNodes.forEach((node, index) => {
    const angle = angles[index] ?? 0;
    const radius = 122 + Math.floor(index / 7) * 46 + deterministicJitter(node.id, 18);
    positions.set(node.id, clampPoint({
      x: center.x + Math.cos(angle) * radius,
      y: center.y + Math.sin(angle) * radius,
    }, node, dimensions));
  });
}

function focusNeighborAngle(index: number, total: number): number {
  const spread = Math.PI * 1.5;
  return -spread / 2 + (total <= 1 ? 0 : (spread * index) / (total - 1));
}

function distributeOrganicAngles(total: number, start: number, end: number, seed: string): number[] {
  if (total <= 0) return [];
  if (total === 1) return [(start + end) / 2 + deterministicJitter(seed, ORGANIC_RING_JITTER)];

  return Array.from({ length: total }, (_, index) => {
    const t = index / (total - 1);
    return start + (end - start) * t + deterministicJitter(`${seed}:${index}`, ORGANIC_RING_JITTER);
  });
}

function blendAngles(left: number, right: number, rightWeight: number): number {
  const x = Math.cos(left) * (1 - rightWeight) + Math.cos(right) * rightWeight;
  const y = Math.sin(left) * (1 - rightWeight) + Math.sin(right) * rightWeight;
  return Math.atan2(y, x);
}

function deterministicJitter(seed: string, amplitude: number): number {
  let hash = 2166136261;
  for (let index = 0; index < seed.length; index += 1) {
    hash ^= seed.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  const normalized = ((hash >>> 0) % 10000) / 10000;
  return (normalized - 0.5) * 2 * amplitude;
}

function scoreCandidatePosition({
  graph,
  node,
  anchor,
  anchorPosition,
  point,
  placedNodes,
  positions,
  dimensions,
}: {
  graph: RegulatoryGraph;
  node: GraphNode;
  anchor: GraphNode;
  anchorPosition: GraphPoint;
  point: GraphPoint;
  placedNodes: GraphNode[];
  positions: Map<string, GraphPoint>;
  dimensions: GraphDimensions;
}): CandidateScore {
  let score = 0;
  let hardCollision = false;
  let labelOverlapCount = 0;
  const candidateLabelBox = getNodeLabelBox(node, point, true, dimensions);

  placedNodes.forEach((placedNode) => {
    const placedPosition = positions.get(placedNode.id);
    if (!placedPosition) return;

    const distance = Math.hypot(point.x - placedPosition.x, point.y - placedPosition.y);
    const minDistance = node.radius + placedNode.radius + 28;
    if (distance < minDistance) {
      hardCollision = true;
      score += 100000 + (minDistance - distance) * 1200;
    } else {
      score += Math.max(0, minDistance + 18 - distance) * 18;
    }

    const placedLabelBox = getNodeLabelBox(placedNode, placedPosition, true, dimensions);
    if (boxesOverlap(candidateLabelBox, placedLabelBox, 6)) {
      labelOverlapCount += 1;
      const protectedLabel = placedNode.type === "gene" || placedNode.id === anchor.id;
      score += protectedLabel ? 9000 : 2400;
    }
  });

  const crossingCount = countCandidateEdgeCrossings(graph, node.id, anchor.id, point, anchorPosition, positions);
  score += crossingCount * 5200;
  score += edgeNodeRoutePenalty(graph, node.id, point, positions);
  score += edgeLabelRoutePenalty(graph, node.id, point, positions, dimensions);
  score += labelOverlapCount * 700;
  score += Math.hypot(point.x - anchorPosition.x, point.y - anchorPosition.y) * 0.65;
  score += boundaryPenalty(point, node, dimensions) * 14;

  return {
    point,
    score,
    hardCollision,
    crossingCount,
  };
}

function clampPoint(point: GraphPoint, node: GraphNode, dimensions: GraphDimensions): GraphPoint {
  const labelWidth = estimateLabelWidth(node.label, node.type);
  const labelSide = getLabelSide(node, point, dimensions, labelWidth);
  const centeredLabelReserve = Math.max(node.radius, labelWidth / 2);
  const labelReserve = node.type === "peak" ? labelWidth + LABEL_GAP : centeredLabelReserve;
  const leftPadding = SAFE_PADDING + (node.type === "peak" ? node.radius + (labelSide === "left" ? labelReserve : 0) : labelReserve);
  const rightPadding = SAFE_PADDING + (node.type === "peak" ? node.radius + (labelSide === "right" ? labelReserve : 0) : labelReserve);
  const minX = Math.min(leftPadding, dimensions.width / 2);
  const maxX = Math.max(minX, dimensions.width - rightPadding);
  return {
    x: clamp(point.x, minX, maxX),
    y: clamp(point.y, SAFE_PADDING + node.radius, dimensions.height - SAFE_PADDING - node.radius),
  };
}

function getNodeLabelBox(
  node: GraphNode,
  point: GraphPoint,
  includePeakLabel: boolean,
  dimensions: GraphDimensions
): LabelBox {
  const width = estimateLabelWidth(node.label, node.type);
  const height = node.type === "peak" ? 18 : 21;
  const side = getLabelSide(node, point, dimensions, width);

  if (node.type === "peak") {
    if (!includePeakLabel) {
      return {
        left: point.x - node.radius,
        top: point.y - node.radius,
        right: point.x + node.radius,
        bottom: point.y + node.radius,
      };
    }

    if (side === "left") {
      return {
        left: point.x - node.radius - LABEL_GAP - width,
        top: point.y - height / 2,
        right: point.x - node.radius - LABEL_GAP,
        bottom: point.y + height / 2,
      };
    }

    return {
      left: point.x + node.radius + LABEL_GAP,
      top: point.y - height / 2,
      right: point.x + node.radius + LABEL_GAP + width,
      bottom: point.y + height / 2,
    };
  }

  return {
    left: point.x - width / 2,
    top: point.y - height / 2,
    right: point.x + width / 2,
    bottom: point.y + height / 2,
  };
}

function getLabelSide(
  node: GraphNode,
  point: GraphPoint,
  dimensions: GraphDimensions,
  labelWidth = estimateLabelWidth(node.label, node.type)
): "left" | "right" {
  const rightEdge = point.x + node.radius + LABEL_GAP + labelWidth;
  if (rightEdge > dimensions.width - SAFE_PADDING * 0.5) return "left";
  return "right";
}

function getNodeCircleBox(node: GraphNode, point: GraphPoint): LabelBox {
  return {
    left: point.x - node.radius,
    top: point.y - node.radius,
    right: point.x + node.radius,
    bottom: point.y + node.radius,
  };
}

function getNodeCollisionBox(
  node: GraphNode,
  point: GraphPoint,
  dimensions: GraphDimensions,
  peakLabelIds: Set<string>
): LabelBox {
  const circleBox = getNodeCircleBox(node, point);
  const shouldReserveLabel = node.type !== "peak" || peakLabelIds.has(node.id);
  if (!shouldReserveLabel) return circleBox;

  const labelBox = getNodeLabelBox(node, point, true, dimensions);
  return {
    left: Math.min(circleBox.left, labelBox.left),
    top: Math.min(circleBox.top, labelBox.top),
    right: Math.max(circleBox.right, labelBox.right),
    bottom: Math.max(circleBox.bottom, labelBox.bottom),
  };
}

function getPeakLabelReserveIds(graph: RegulatoryGraph): Set<string> {
  return new Set(graph.nodes.filter((node) => node.type === "peak").map((node) => node.id));
}

function boxesOverlap(a: LabelBox, b: LabelBox, padding = 0): boolean {
  return a.left - padding < b.right
    && a.right + padding > b.left
    && a.top - padding < b.bottom
    && a.bottom + padding > b.top;
}

function boxCenter(box: LabelBox): GraphPoint {
  return {
    x: (box.left + box.right) / 2,
    y: (box.top + box.bottom) / 2,
  };
}

function countCandidateEdgeCrossings(
  graph: RegulatoryGraph,
  nodeId: string,
  anchorId: string,
  nodePoint: GraphPoint,
  anchorPoint: GraphPoint,
  positions: Map<string, GraphPoint>
): number {
  let crossings = 0;
  graph.edges.forEach((edge) => {
    if (edge.source === nodeId || edge.target === nodeId || edge.source === anchorId || edge.target === anchorId) return;
    const source = positions.get(edge.source);
    const target = positions.get(edge.target);
    if (!source || !target) return;
    if (segmentsIntersect(anchorPoint, nodePoint, source, target)) crossings += 1;
  });
  return crossings;
}

function edgeNodeRoutePenalty(
  graph: RegulatoryGraph,
  nodeId: string,
  nodePoint: GraphPoint,
  positions: Map<string, GraphPoint>
): number {
  let penalty = 0;
  graph.edges.forEach((edge) => {
    if (edge.source !== nodeId && edge.target !== nodeId) return;
    const otherId = edge.source === nodeId ? edge.target : edge.source;
    const otherPoint = positions.get(otherId);
    if (!otherPoint) return;

    graph.nodes.forEach((candidate) => {
      if (candidate.id === nodeId || candidate.id === otherId) return;
      const candidatePoint = positions.get(candidate.id);
      if (!candidatePoint) return;
      const distance = distanceToSegment(candidatePoint, nodePoint, otherPoint);
      const clearance = candidate.radius + 18;
      if (distance < clearance) penalty += (clearance - distance) * 620;
    });
  });
  return penalty;
}

function edgeLabelRoutePenalty(
  graph: RegulatoryGraph,
  nodeId: string,
  nodePoint: GraphPoint,
  positions: Map<string, GraphPoint>,
  dimensions: GraphDimensions
): number {
  let penalty = 0;

  graph.edges.forEach((edge) => {
    if (edge.source !== nodeId && edge.target !== nodeId) return;
    const otherId = edge.source === nodeId ? edge.target : edge.source;
    const otherPoint = positions.get(otherId);
    if (!otherPoint) return;

    graph.nodes.forEach((candidate) => {
      if (candidate.id === nodeId || candidate.id === otherId) return;
      const candidatePoint = positions.get(candidate.id);
      if (!candidatePoint) return;
      const labelBox = getNodeLabelBox(candidate, candidatePoint, true, dimensions);
      if (segmentIntersectsBox(nodePoint, otherPoint, labelBox)) {
        penalty += candidate.type === "peak" ? 4200 : 6800;
      }
    });
  });

  return penalty;
}

function segmentsIntersect(a: GraphPoint, b: GraphPoint, c: GraphPoint, d: GraphPoint): boolean {
  const abx = b.x - a.x;
  const aby = b.y - a.y;
  const acx = c.x - a.x;
  const acy = c.y - a.y;
  const adx = d.x - a.x;
  const ady = d.y - a.y;
  const cdx = d.x - c.x;
  const cdy = d.y - c.y;
  const cax = a.x - c.x;
  const cay = a.y - c.y;
  const cbx = b.x - c.x;
  const cby = b.y - c.y;
  const cross1 = abx * acy - aby * acx;
  const cross2 = abx * ady - aby * adx;
  const cross3 = cdx * cay - cdy * cax;
  const cross4 = cdx * cby - cdy * cbx;
  return cross1 * cross2 < 0 && cross3 * cross4 < 0;
}

function segmentIntersectsBox(start: GraphPoint, end: GraphPoint, box: LabelBox): boolean {
  if (pointInBox(start, box) || pointInBox(end, box)) return true;
  const topLeft = { x: box.left, y: box.top };
  const topRight = { x: box.right, y: box.top };
  const bottomRight = { x: box.right, y: box.bottom };
  const bottomLeft = { x: box.left, y: box.bottom };

  return segmentsIntersect(start, end, topLeft, topRight)
    || segmentsIntersect(start, end, topRight, bottomRight)
    || segmentsIntersect(start, end, bottomRight, bottomLeft)
    || segmentsIntersect(start, end, bottomLeft, topLeft);
}

function pointInBox(point: GraphPoint, box: LabelBox): boolean {
  return point.x >= box.left
    && point.x <= box.right
    && point.y >= box.top
    && point.y <= box.bottom;
}

function distanceToSegment(point: GraphPoint, start: GraphPoint, end: GraphPoint): number {
  const dx = end.x - start.x;
  const dy = end.y - start.y;
  const lengthSquared = dx * dx + dy * dy;
  if (lengthSquared <= 1e-6) return Math.hypot(point.x - start.x, point.y - start.y);
  const t = clamp(((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared, 0, 1);
  const projected = {
    x: start.x + dx * t,
    y: start.y + dy * t,
  };
  return Math.hypot(point.x - projected.x, point.y - projected.y);
}

function distanceToPolyline(point: GraphPoint, points: GraphPoint[]): number {
  if (points.length < 2) return Number.POSITIVE_INFINITY;
  let distance = Number.POSITIVE_INFINITY;

  for (let index = 0; index < points.length - 1; index += 1) {
    const start = points[index];
    const end = points[index + 1];
    if (!start || !end) continue;
    distance = Math.min(distance, distanceToSegment(point, start, end));
  }

  return distance;
}

function polylineIntersectsBox(points: GraphPoint[], box: LabelBox): boolean {
  for (let index = 0; index < points.length - 1; index += 1) {
    const start = points[index];
    const end = points[index + 1];
    if (!start || !end) continue;
    if (segmentIntersectsBox(start, end, box)) return true;
  }

  return false;
}

function polylinesIntersect(left: GraphPoint[], right: GraphPoint[]): boolean {
  for (let leftIndex = 0; leftIndex < left.length - 1; leftIndex += 1) {
    const leftStart = left[leftIndex];
    const leftEnd = left[leftIndex + 1];
    if (!leftStart || !leftEnd) continue;

    for (let rightIndex = 0; rightIndex < right.length - 1; rightIndex += 1) {
      const rightStart = right[rightIndex];
      const rightEnd = right[rightIndex + 1];
      if (!rightStart || !rightEnd) continue;
      if (segmentsIntersect(leftStart, leftEnd, rightStart, rightEnd)) return true;
    }
  }

  return false;
}

function edgesShareEndpoint(left: GraphEdge, right: GraphEdge): boolean {
  return left.source === right.source
    || left.source === right.target
    || left.target === right.source
    || left.target === right.target;
}

function resolveGraphCollisions(graph: RegulatoryGraph, dimensions: GraphDimensions) {
  const peakLabelIds = getPeakLabelReserveIds(graph);

  for (let iteration = 0; iteration < COLLISION_ITERATIONS; iteration += 1) {
    let moved = false;

    for (let i = 0; i < graph.nodes.length; i += 1) {
      for (let j = i + 1; j < graph.nodes.length; j += 1) {
        const leftNode = graph.nodes[i];
        const rightNode = graph.nodes[j];
        if (!leftNode || !rightNode) continue;
        const leftBox = getNodeCollisionBox(leftNode, leftNode, dimensions, peakLabelIds);
        const rightBox = getNodeCollisionBox(rightNode, rightNode, dimensions, peakLabelIds);

        if (!boxesOverlap(leftBox, rightBox, COLLISION_PADDING)) continue;

        const leftCenter = boxCenter(leftBox);
        const rightCenter = boxCenter(rightBox);
        const dx = rightCenter.x - leftCenter.x;
        const dy = rightCenter.y - leftCenter.y;
        const distance = Math.max(0.001, Math.hypot(dx, dy));
        const overlapX = Math.min(leftBox.right + COLLISION_PADDING - rightBox.left, rightBox.right + COLLISION_PADDING - leftBox.left);
        const overlapY = Math.min(leftBox.bottom + COLLISION_PADDING - rightBox.top, rightBox.bottom + COLLISION_PADDING - leftBox.top);
        const push = Math.max(0.7, Math.min(overlapX, overlapY) * 0.52);
        const ux = dx / distance;
        const uy = dy / distance;

        const leftWeight = 0.5;
        const rightWeight = 0.5;
        leftNode.x -= ux * push * leftWeight;
        leftNode.y -= uy * push * leftWeight;
        rightNode.x += ux * push * rightWeight;
        rightNode.y += uy * push * rightWeight;
        moved = true;
      }
    }

    graph.nodes.forEach((node) => {
      const clamped = clampPoint(node, node, dimensions);
      node.x = clamped.x;
      node.y = clamped.y;
    });

    if (!moved) break;
  }
}

function boundaryPenalty(point: GraphPoint, node: GraphNode, dimensions: GraphDimensions): number {
  const box = getNodeCollisionBox(node, point, dimensions, new Set([node.id]));
  return Math.max(0, SAFE_PADDING - box.left)
    + Math.max(0, box.right - (dimensions.width - SAFE_PADDING))
    + Math.max(0, SAFE_PADDING - box.top)
    + Math.max(0, box.bottom - (dimensions.height - SAFE_PADDING));
}

function fitGraphToView(graph: RegulatoryGraph, dimensions: GraphDimensions, padding: number): RegulatoryGraph {
  const bounds = getGraphBounds(graph, dimensions);
  if (!bounds) return graph;

  const width = Math.max(1, bounds.maxX - bounds.minX);
  const height = Math.max(1, bounds.maxY - bounds.minY);
  const scale = Math.min(MAX_LAYOUT_SCALE, (dimensions.width - padding * 2) / width, (dimensions.height - padding * 2) / height);
  const centerX = (bounds.minX + bounds.maxX) / 2;
  const centerY = (bounds.minY + bounds.maxY) / 2;
  const targetX = dimensions.width / 2;
  const targetY = dimensions.height / 2;

  graph.nodes.forEach((node) => {
    node.x = targetX + (node.x - centerX) * scale;
    node.y = targetY + (node.y - centerY) * scale;
  });

  return graph;
}

function getGraphBounds(
  graph: RegulatoryGraph,
  dimensions: GraphDimensions
): { minX: number; minY: number; maxX: number; maxY: number } | null {
  if (!graph.nodes.length) return null;

  let minX = Number.POSITIVE_INFINITY;
  let minY = Number.POSITIVE_INFINITY;
  let maxX = Number.NEGATIVE_INFINITY;
  let maxY = Number.NEGATIVE_INFINITY;
  const peakLabelIds = getPeakLabelReserveIds(graph);

  graph.nodes.forEach((node) => {
    const box = getNodeCollisionBox(node, node, dimensions, peakLabelIds);
    minX = Math.min(minX, box.left);
    minY = Math.min(minY, box.top);
    maxX = Math.max(maxX, box.right);
    maxY = Math.max(maxY, box.bottom);
  });

  return { minX, minY, maxX, maxY };
}

function applyEdgeCurves(graph: RegulatoryGraph, dimensions: GraphDimensions) {
  const pairCounts = new Map<string, number>();
  const routedEdges: RoutedEdge[] = [];

  graph.edges.forEach((edge) => {
    const pairKey = [edge.source, edge.target].sort().join("|");
    const index = pairCounts.get(pairKey) ?? 0;
    pairCounts.set(pairKey, index + 1);

    const source = graph.nodes.find((node) => node.id === edge.source);
    const target = graph.nodes.find((node) => node.id === edge.target);
    if (!source || !target) {
      edge.curve = 0;
      return;
    }

    const curve = chooseEdgeCurve(graph, edge, source, target, index, routedEdges, dimensions);
    edge.curve = Math.abs(curve) < 1 ? 0 : curve;
    routedEdges.push({
      edge,
      points: makeQuadraticRoutePoints(source, target, edge.curve),
    });
  });
}

function refineEdgeRoutesWithNudging(graph: RegulatoryGraph, dimensions: GraphDimensions) {
  for (let iteration = 0; iteration < EDGE_NUDGE_ITERATIONS; iteration += 1) {
    applyEdgeCurves(graph, dimensions);
    const moved = nudgeNodesForBlockedRoutes(graph, dimensions);
    if (!moved) return;
    resolveGraphCollisions(graph, dimensions);
    fitGraphToView(graph, dimensions, SAFE_PADDING);
  }

  applyEdgeCurves(graph, dimensions);
}

function nudgeNodesForBlockedRoutes(graph: RegulatoryGraph, dimensions: GraphDimensions): boolean {
  let moved = false;

  graph.edges.forEach((edge) => {
    const source = graph.nodes.find((node) => node.id === edge.source);
    const target = graph.nodes.find((node) => node.id === edge.target);
    if (!source || !target) return;
    const routePoints = makeQuadraticRoutePoints(source, target, edge.curve);

    graph.nodes.forEach((obstacle) => {
      if (obstacle.id === edge.source || obstacle.id === edge.target) return;
      const nodeClearance = obstacle.radius + 11;
      const nodeDistance = distanceToPolyline(obstacle, routePoints);
      if (nodeDistance < nodeClearance) {
        moved = nudgeRouteEndpoints(source, target, obstacle, nodeClearance - nodeDistance, graph, dimensions) || moved;
        return;
      }

      const labelBox = getNodeLabelBox(obstacle, obstacle, true, dimensions);
      if (!polylineIntersectsBox(routePoints, labelBox)) return;
      moved = nudgeRouteEndpoints(source, target, boxCenter(labelBox), 10, graph, dimensions) || moved;
    });
  });

  return moved;
}

function nudgeRouteEndpoints(
  source: GraphNode,
  target: GraphNode,
  obstaclePoint: GraphPoint,
  pressure: number,
  graph: RegulatoryGraph,
  dimensions: GraphDimensions
): boolean {
  const movableNodes = [source, target].filter((node) => node.type === "gene" || node.type === "peak");
  if (!movableNodes.length) return false;

  const dx = target.x - source.x;
  const dy = target.y - source.y;
  const length = Math.max(1, Math.hypot(dx, dy));
  const normal = { x: -dy / length, y: dx / length };
  const routeMidpoint = { x: (source.x + target.x) / 2, y: (source.y + target.y) / 2 };
  const side = Math.sign((obstaclePoint.x - routeMidpoint.x) * normal.x + (obstaclePoint.y - routeMidpoint.y) * normal.y) || 1;
  const strength = clamp(pressure * 0.28, 2, 9);
  let moved = false;

  movableNodes.forEach((node) => {
    const weight = node.type === "peak" ? 0.72 : 1;
    const nextPoint = clampPoint({
      x: node.x - normal.x * side * strength * weight,
      y: node.y - normal.y * side * strength * weight,
    }, node, dimensions);
    if (Math.hypot(nextPoint.x - node.x, nextPoint.y - node.y) <= 0.2) return;
    node.x = nextPoint.x;
    node.y = nextPoint.y;
    moved = true;
  });

  if (!moved) {
    const obstacleNode = graph.nodes.find((node) => node.id !== source.id && node.id !== target.id
      && Math.hypot(node.x - obstaclePoint.x, node.y - obstaclePoint.y) <= node.radius + 1);
    if (obstacleNode && (obstacleNode.type === "gene" || obstacleNode.type === "peak")) {
      const nextPoint = clampPoint({
        x: obstacleNode.x + normal.x * side * strength * 0.72,
        y: obstacleNode.y + normal.y * side * strength * 0.72,
      }, obstacleNode, dimensions);
      if (Math.hypot(nextPoint.x - obstacleNode.x, nextPoint.y - obstacleNode.y) > 0.2) {
        obstacleNode.x = nextPoint.x;
        obstacleNode.y = nextPoint.y;
        moved = true;
      }
    }
  }

  return moved;
}

function chooseEdgeCurve(
  graph: RegulatoryGraph,
  edge: GraphEdge,
  source: GraphNode,
  target: GraphNode,
  siblingIndex: number,
  routedEdges: RoutedEdge[],
  dimensions: GraphDimensions
): number {
  const direction = source.x > target.x ? -1 : 1;
  const baseCurve = 15
    + siblingIndex * 11
    + Math.abs(deterministicJitter(edge.id, 4));
  const directPenalty = scoreEdgeRoute(graph, edge, source, target, 0, routedEdges, dimensions);
  const routePressure = Math.min(32, directPenalty / 1200);
  const candidates = uniqueNumbers([
    0,
    direction * (baseCurve + routePressure),
    -direction * (baseCurve + routePressure),
    direction * (baseCurve * 1.58 + routePressure),
    -direction * (baseCurve * 1.58 + routePressure),
    direction * (baseCurve * 2.22 + routePressure),
    -direction * (baseCurve * 2.22 + routePressure),
  ]);

  return candidates
    .map((curve) => ({
      curve,
      score: scoreEdgeRoute(graph, edge, source, target, curve, routedEdges, dimensions),
    }))
    .sort((a, b) => a.score - b.score)[0]?.curve ?? 0;
}

function scoreEdgeRoute(
  graph: RegulatoryGraph,
  edge: GraphEdge,
  source: GraphNode,
  target: GraphNode,
  curve: number,
  routedEdges: RoutedEdge[],
  dimensions: GraphDimensions
): number {
  const points = makeQuadraticRoutePoints(source, target, curve);
  let score = Math.abs(curve) * 1.35;

  points.forEach((point) => {
    score += Math.max(0, -point.x + SAFE_PADDING * 0.2) * 3;
    score += Math.max(0, point.x - dimensions.width + SAFE_PADDING * 0.2) * 3;
    score += Math.max(0, -point.y + SAFE_PADDING * 0.2) * 3;
    score += Math.max(0, point.y - dimensions.height + SAFE_PADDING * 0.2) * 3;
  });

  graph.nodes.forEach((node) => {
    if (node.id === edge.source || node.id === edge.target) return;
    const nodeClearance = node.radius + 10;
    const nodeDistance = distanceToPolyline(node, points);
    if (nodeDistance < nodeClearance) score += 36000 + (nodeClearance - nodeDistance) * 1450;

    const labelBox = getNodeLabelBox(node, node, true, dimensions);
    if (polylineIntersectsBox(points, labelBox)) {
      score += node.type === "peak" ? 9200 : 13800;
    }
  });

  routedEdges.forEach((routedEdge) => {
    if (edgesShareEndpoint(edge, routedEdge.edge)) return;
    if (polylinesIntersect(points, routedEdge.points)) score += 4800;
  });

  return score;
}

function makeQuadraticRoutePoints(source: GraphPoint, target: GraphPoint, curve: number): GraphPoint[] {
  if (Math.abs(curve) < 1) return [source, target];

  const midX = (source.x + target.x) / 2;
  const midY = (source.y + target.y) / 2;
  const dx = target.x - source.x;
  const dy = target.y - source.y;
  const length = Math.max(1, Math.hypot(dx, dy));
  const control = {
    x: midX - (dy / length) * curve,
    y: midY + (dx / length) * curve,
  };
  const points: GraphPoint[] = [];

  for (let index = 0; index <= EDGE_ROUTE_SAMPLE_COUNT; index += 1) {
    const t = index / EDGE_ROUTE_SAMPLE_COUNT;
    const oneMinusT = 1 - t;
    points.push({
      x: oneMinusT * oneMinusT * source.x + 2 * oneMinusT * t * control.x + t * t * target.x,
      y: oneMinusT * oneMinusT * source.y + 2 * oneMinusT * t * control.y + t * t * target.y,
    });
  }

  return points;
}

function uniqueNumbers(values: number[]): number[] {
  const rounded = new Set<string>();
  const result: number[] = [];

  values.forEach((value) => {
    const key = value.toFixed(2);
    if (rounded.has(key)) return;
    rounded.add(key);
    result.push(value);
  });

  return result;
}

function applyPositions(graph: RegulatoryGraph, positions: Map<string, GraphPoint>) {
  graph.nodes.forEach((node) => {
    const position = positions.get(node.id);
    if (!position) return;
    node.x = position.x;
    node.y = position.y;
  });
}

function genePriority(graph: RegulatoryGraph, geneId: string): number {
  const links = getPeakGeneLinksForGene(graph, geneId);
  const topLinkScore = links[0]?.score ?? links[0]?.linkScore ?? 0;
  return Number(topLinkScore ?? 0) * 10 + links.length;
}

function peakPriority(graph: RegulatoryGraph, peakId: string): number {
  const links = getPeakGeneLinksForPeak(graph, peakId);
  const topLinkScore = links[0]?.score ?? links[0]?.linkScore ?? 0;
  return Number(topLinkScore ?? 0) * 10 + links.length;
}

function getGeneNodes(graph: RegulatoryGraph): GraphNode[] {
  return graph.nodes.filter((node) => node.type === "gene").sort(sortNodes);
}

function getPeakGeneEdge(graph: RegulatoryGraph, peakId: string, geneId: string): GraphEdge | undefined {
  const id = graphEdgeId(geneId, peakId, "peakGene");
  return graph.edges.find((edge) => edge.id === id);
}

function isDirectNeighbor(graph: RegulatoryGraph, sourceId: string, targetId: string): boolean {
  return graph.edges.some((edge) => {
    return (edge.source === sourceId && edge.target === targetId) || (edge.target === sourceId && edge.source === targetId);
  });
}

function sortNodesForFocus(): (a: GraphNode, b: GraphNode) => number {
  return (a, b) => {
    const score = (node: GraphNode) => {
      if (node.type === "gene") return 2;
      if (node.type === "peak") return 1;
      return 0;
    };
    return score(b) - score(a) || sortNodes(a, b);
  };
}

function sortNodes(a: GraphNode, b: GraphNode): number {
  const order: Record<GraphNodeType, number> = { gene: 0, peak: 1 };
  return order[a.type] - order[b.type] || a.label.localeCompare(b.label);
}

function sortEdges(a: GraphEdge, b: GraphEdge): number {
  return a.id.localeCompare(b.id);
}

function sortLinks(a: GraphLink, b: GraphLink): number {
  const aScore = a.score ?? a.linkScore ?? 0;
  const bScore = b.score ?? b.linkScore ?? 0;
  return Number(bScore) - Number(aScore) || Math.abs(a.distanceToTss ?? 0) - Math.abs(b.distanceToTss ?? 0);
}

function cloneGraph(graph: RegulatoryGraph): RegulatoryGraph {
  return {
    ...graph,
    nodes: graph.nodes.map(cloneNode),
    edges: graph.edges.map(cloneEdge),
    links: graph.links.map((link) => ({ ...link })),
  };
}

function cloneNode(node: GraphNode): GraphNode {
  return {
    ...node,
    metadata: {
      ...node.metadata,
    },
  };
}

function cloneEdge(edge: GraphEdge): GraphEdge {
  return { ...edge };
}

function mergeNode(nodeMap: Map<string, GraphNode>, node: GraphNode) {
  const existing = nodeMap.get(node.id);
  if (!existing) {
    nodeMap.set(node.id, cloneNode(node));
    return;
  }

  nodeMap.set(node.id, {
    ...existing,
    label: existing.label || node.label,
    radius: Math.max(existing.radius, node.radius),
    metadata: {
      ...node.metadata,
      ...existing.metadata,
      datasetId: existing.metadata.datasetId ?? node.metadata.datasetId,
      sampleName: existing.metadata.sampleName ?? node.metadata.sampleName,
      domain: existing.metadata.domain ?? node.metadata.domain,
      source: existing.metadata.source ?? node.metadata.source,
      linkedGenesCount: existing.metadata.linkedGenesCount ?? node.metadata.linkedGenesCount,
      topLinkedGenes: existing.metadata.topLinkedGenes ?? node.metadata.topLinkedGenes,
      remainingLinkedGenesCount: existing.metadata.remainingLinkedGenesCount ?? node.metadata.remainingLinkedGenesCount,
      linkedPeaksCount: existing.metadata.linkedPeaksCount ?? node.metadata.linkedPeaksCount,
      topLinkedPeaks: existing.metadata.topLinkedPeaks ?? node.metadata.topLinkedPeaks,
      remainingLinkedPeaksCount: existing.metadata.remainingLinkedPeaksCount ?? node.metadata.remainingLinkedPeaksCount,
      maxLinkScore: existing.metadata.maxLinkScore ?? node.metadata.maxLinkScore,
      correlationMin: existing.metadata.correlationMin ?? node.metadata.correlationMin,
      correlationMax: existing.metadata.correlationMax ?? node.metadata.correlationMax,
      fdrMin: existing.metadata.fdrMin ?? node.metadata.fdrMin,
      fdrMax: existing.metadata.fdrMax ?? node.metadata.fdrMax,
      minFdr: existing.metadata.minFdr ?? node.metadata.minFdr,
      totalLinks: existing.metadata.totalLinks ?? node.metadata.totalLinks,
    },
  });
}

function mergeEdge(existing: GraphEdge | undefined, next: GraphEdge): GraphEdge {
  if (!existing) return cloneEdge(next);
  return {
    ...existing,
    score: existing.score ?? next.score,
    correlation: existing.correlation ?? next.correlation,
    fdr: existing.fdr ?? next.fdr,
    distanceToTss: existing.distanceToTss ?? next.distanceToTss,
    varQAtac: existing.varQAtac ?? next.varQAtac,
    varQRna: existing.varQRna ?? next.varQRna,
    sourceMethod: existing.sourceMethod ?? next.sourceMethod,
  };
}

function readString(record: ReadableRecord, ...keys: string[]): string | undefined {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "string" && value.trim()) return value.trim();
    if (typeof value === "number" && Number.isFinite(value)) return String(value);
  }
  return undefined;
}

function readStringList(record: ReadableRecord, ...keys: string[]): string[] | string | undefined {
  for (const key of keys) {
    const value = record[key];
    if (Array.isArray(value)) {
      const values = value
        .map((item) => typeof item === "string" || typeof item === "number" ? String(item).trim() : "")
        .filter(Boolean);
      if (values.length) return values;
    }
    if (typeof value === "string" && value.trim()) return value.trim();
  }
  return undefined;
}

function readNumber(record: ReadableRecord, ...keys: string[]): number | null {
  for (const key of keys) {
    const value = cleanNumber(record[key]);
    if (value !== null) return value;
  }
  return null;
}

function cleanString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function cleanNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function nodeIdForType(type: GraphNodeType, label: string): string {
  if (type === "peak") return getPeakNodeId(label);
  return getGeneNodeId(label);
}

function getNodeIdValue(nodeId: string, type: RegulatoryNetworkNodeType): string {
  const prefix = `${type}:`;
  return nodeId.startsWith(prefix) ? nodeId.slice(prefix.length) : nodeId;
}

function parsePeakRegion(peak: string): Pick<GraphNodeMetadata, "chromosome" | "start" | "end"> {
  const match = peak.match(/^(chr[^:]+):(\d+)-(\d+)$/i);
  if (!match) return {};
  return {
    chromosome: match[1],
    start: Number(match[2]),
    end: Number(match[3]),
  };
}

function compactNumber(value: number): string {
  if (!Number.isFinite(value)) return "";
  if (Math.abs(value) >= 1_000_000) return `${Math.round(value / 100_000) / 10}M`;
  if (Math.abs(value) >= 1_000) return `${Math.round(value / 100) / 10}k`;
  return String(value);
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

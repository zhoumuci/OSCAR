<template>
  <div class="regulatory-network-canvas" :style="{ height: `${dimensions.height}px` }">
    <svg
      class="network-svg"
      :viewBox="`0 0 ${dimensions.width} ${dimensions.height}`"
      role="img"
      aria-label="Regulatory network graph"
      @pointermove="handlePointerMove"
      @pointerup="finishPan"
      @pointerleave="finishPan"
      @click="handleSvgClick"
    >
      <defs>
        <linearGradient id="gene-node-gradient" x1="20%" y1="10%" x2="80%" y2="90%">
          <stop offset="0%" stop-color="#fff6d7" stop-opacity="0.96" />
          <stop offset="100%" stop-color="#f2d28a" stop-opacity="0.9" />
        </linearGradient>
        <linearGradient id="peak-node-gradient" x1="18%" y1="12%" x2="82%" y2="88%">
          <stop offset="0%" stop-color="#f8fbff" stop-opacity="0.96" />
          <stop offset="100%" stop-color="#d8e2ef" stop-opacity="0.9" />
        </linearGradient>
        <linearGradient id="group-node-gradient" x1="18%" y1="12%" x2="82%" y2="88%">
          <stop offset="0%" stop-color="#e9fffa" stop-opacity="0.96" />
          <stop offset="100%" stop-color="#91ded2" stop-opacity="0.9" />
        </linearGradient>
        <linearGradient id="tf-node-gradient" x1="18%" y1="12%" x2="82%" y2="88%">
          <stop offset="0%" stop-color="#faf7ff" stop-opacity="0.96" />
          <stop offset="100%" stop-color="#d8cff2" stop-opacity="0.9" />
        </linearGradient>
        <filter id="node-soft-shadow" x="-60%" y="-60%" width="220%" height="220%">
          <feDropShadow dx="0" dy="2" stdDeviation="2.2" flood-color="#31524d" flood-opacity="0.11" />
        </filter>
        <filter id="selected-node-halo" x="-80%" y="-80%" width="260%" height="260%">
          <feGaussianBlur stdDeviation="7" result="blur" />
          <feColorMatrix
            in="blur"
            type="matrix"
            values="0 0 0 0 0.09 0 0 0 0 0.55 0 0 0 0 0.49 0 0 0 0.25 0"
          />
          <feMerge>
            <feMergeNode />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <g class="viewport-layer" :transform="`translate(${pan.x} ${pan.y})`">
        <g class="grid-layer">
          <rect
            class="pan-surface"
            :x="-gridExtent"
            :y="-gridExtent"
            :width="gridExtent * 2 + dimensions.width"
            :height="gridExtent * 2 + dimensions.height"
            @pointerdown="startPan"
          />
          <line
            v-for="line in verticalGridLines"
            :key="`v-${line}`"
            class="grid-line"
            :x1="line"
            :x2="line"
            :y1="-gridExtent"
            :y2="gridExtent + dimensions.height"
          />
          <line
            v-for="line in horizontalGridLines"
            :key="`h-${line}`"
            class="grid-line"
            :x1="-gridExtent"
            :x2="gridExtent + dimensions.width"
            :y1="line"
            :y2="line"
          />
        </g>

        <g class="edge-layer">
          <g
            v-for="(edge, edgeIndex) in renderEdges"
            :key="edge.id"
            class="network-edge-item"
            :class="edgeClass(edge)"
          >
            <path
              class="network-edge-hit"
              :d="edgePath(edge)"
              @click.stop="emit('edge-click', edge.id)"
              @pointerenter="emit('edge-hover', edge.id)"
              @pointerleave="emit('edge-hover', '')"
            />
            <path
              class="network-edge"
              :class="edgeClass(edge)"
              :d="edgePath(edge)"
              :style="[edgeStyle(edge), edgeDelay(edge, edgeIndex)]"
            />
          </g>
        </g>

        <g class="node-layer">
          <g
            v-for="(node, nodeIndex) in displayGraph.nodes"
            :key="node.id"
            class="network-node"
            :class="nodeClass(node)"
            :style="nodeGroupStyle(node)"
            @click.stop="emit('node-click', node.id)"
            @pointerenter="emit('node-hover', node.id)"
            @pointerleave="emit('node-hover', '')"
          >
            <g class="node-visual" :style="nodeVisualStyle(node, nodeIndex)">
              <circle
                class="node-halo"
                :r="node.radius + 9"
              />
                <circle
                  class="node-circle"
                  :r="node.radius"
                  :fill="nodeFill(node)"
                  :stroke="nodeStroke(node)"
                  filter="url(#node-soft-shadow)"
                />
              <circle
                class="node-sheen"
                :cx="-node.radius * 0.18"
                :cy="-node.radius * 0.2"
                :r="node.radius * 0.68"
              />
            </g>
          </g>
        </g>

        <g class="label-layer">
          <g
            v-for="label in labels"
            :key="label.node.id"
            class="label-item"
            :class="[labelClass(label.node), { 'label-centered': label.placement === 'center', 'label-external': label.placement === 'external' }]"
            :style="labelStyle(label)"
          >
            <rect
              v-if="label.placement === 'external'"
              class="label-pill"
              :x="label.pillX"
              :y="label.pillY"
              :width="label.width + 12"
              :height="label.height"
              :rx="label.height / 2"
            />
            <text
              class="node-label"
              :text-anchor="label.anchor"
              dominant-baseline="middle"
            >
              {{ label.text }}
            </text>
          </g>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import type { GraphDimensions, GraphEdge, GraphNode, GraphPoint, RegulatoryGraph } from "@/utils/regulatoryNetworkGraph";
import {
  estimateLabelWidth,
  shortPeakLabel,
} from "@/utils/regulatoryNetworkGraph";

const props = defineProps<{
  graph: RegulatoryGraph;
  dimensions: GraphDimensions;
  pan: GraphPoint;
  selectedNodeId: string;
  selectedEdgeId: string;
  hoveredNodeId: string;
  hoveredEdgeId: string;
  focusedNodeId: string;
  peakLabelMode: PeakLabelMode;
  enteringNodeOrigins?: Record<string, GraphPoint>;
}>();

const emit = defineEmits<{
  "update:pan": [value: GraphPoint];
  "node-click": [nodeId: string];
  "edge-click": [edgeId: string];
  "blank-click": [];
  "node-hover": [nodeId: string];
  "edge-hover": [edgeId: string];
}>();

type PanStart = {
  pointerId: number;
  x: number;
  y: number;
  origin: GraphPoint;
  dragging: boolean;
};

type SvgLabel = {
  node: GraphNode;
  text: string;
  anchor: "start" | "end" | "middle";
  placement: "center" | "external";
  x: number;
  y: number;
  width: number;
  height: number;
  pillX: number;
  pillY: number;
  priority: number;
  box: LabelBox;
};

type LabelBox = {
  left: number;
  top: number;
  right: number;
  bottom: number;
};

type PeakLabelMode = "overview" | "active" | "hidden";
type PeakLabelDirection = "right" | "left" | "top" | "bottom" | "top-right" | "top-left" | "bottom-right" | "bottom-left";

type PeakLabelCandidate = {
  direction: PeakLabelDirection;
  anchor: "start" | "end" | "middle";
  x: number;
  y: number;
  pillX: number;
  pillY: number;
  box: LabelBox;
  angle: number;
  distance: number;
  unit: GraphPoint;
};

const GRID_STEP = 44;
const PEAK_LABEL_GAP = 8;
const PEAK_LABEL_MAX_GAP = 12;
const PEAK_LABEL_PADDING_X = 6;
const PEAK_LABEL_MAX_EXTRA_DISTANCE = 28;
const PEAK_LABEL_MAX_NUDGE = 6;
const gridExtent = 2200;
const peakLabelDirections: Array<{ direction: PeakLabelDirection; vector: GraphPoint }> = [
  { direction: "right", vector: { x: 1, y: 0 } },
  { direction: "left", vector: { x: -1, y: 0 } },
  { direction: "top", vector: { x: 0, y: -1 } },
  { direction: "bottom", vector: { x: 0, y: 1 } },
  { direction: "top-right", vector: { x: 1, y: -1 } },
  { direction: "top-left", vector: { x: -1, y: -1 } },
  { direction: "bottom-right", vector: { x: 1, y: 1 } },
  { direction: "bottom-left", vector: { x: -1, y: 1 } },
];
const panStart = ref<PanStart | null>(null);
const suppressBlankClick = ref(false);
const animatedNodes = ref<GraphNode[]>(props.graph.nodes.map(cloneNode));
let nodeAnimationFrame: number | undefined;

const displayGraph = computed<RegulatoryGraph>(() => ({
  ...props.graph,
  nodes: animatedNodes.value,
}));
const nodeById = computed(() => new Map(displayGraph.value.nodes.map((node) => [node.id, node] as const)));
const selectedEdge = computed(() => props.graph.edges.find((edge) => edge.id === props.selectedEdgeId));
const hoveredEdge = computed(() => props.graph.edges.find((edge) => edge.id === props.hoveredEdgeId));
const selectedPeakLabelIds = computed(() => getRelatedPeakIdsForNode(props.selectedNodeId));
const hoveredPeakLabelIds = computed(() => getRelatedPeakIdsForNode(props.hoveredNodeId));
const selectedEdgePeakLabelIds = computed(() => getRelatedPeakIdsForEdge(props.selectedEdgeId));
const hoveredEdgePeakLabelIds = computed(() => getRelatedPeakIdsForEdge(props.hoveredEdgeId));
const verticalGridLines = computed(() => makeGridLines(-gridExtent, props.dimensions.width + gridExtent));
const horizontalGridLines = computed(() => makeGridLines(-gridExtent, props.dimensions.height + gridExtent));
const renderEdges = computed(() => props.graph.edges.filter((edge) => {
  return nodeById.value.has(edge.source) && nodeById.value.has(edge.target);
}));

const labels = computed(() => {
  const visibleLabelNodes = displayGraph.value.nodes.filter((node) => shouldRenderNodeLabel(node));
  const centeredLabels = visibleLabelNodes
    .filter((node) => isCenteredNodeLabel(node))
    .map((node): SvgLabel => createCenteredLabel(node));
  const placedLabels: SvgLabel[] = [...centeredLabels];
  const peakLabels = visibleLabelNodes
    .filter((node) => node.type === "peak")
    .sort((a, b) => labelPriority(b) - labelPriority(a))
    .map((node): SvgLabel => {
      const label = createPeakLabel(node, placedLabels);
      placedLabels.push(label);
      return label;
    });

  return [...centeredLabels, ...peakLabels]
    .sort((a, b) => nodeLayerOrder(a.node) - nodeLayerOrder(b.node) || b.priority - a.priority);
});

watch(() => props.graph, (graph) => {
  animateNodePositions(graph);
}, { deep: true, immediate: true });

onBeforeUnmount(() => {
  if (nodeAnimationFrame !== undefined) window.cancelAnimationFrame(nodeAnimationFrame);
});

function startPan(event: PointerEvent) {
  panStart.value = {
    pointerId: event.pointerId,
    x: event.clientX,
    y: event.clientY,
    origin: { ...props.pan },
    dragging: false,
  };
  (event.currentTarget as SVGElement).setPointerCapture(event.pointerId);
}

function handlePointerMove(event: PointerEvent) {
  if (!panStart.value) return;
  const dx = event.clientX - panStart.value.x;
  const dy = event.clientY - panStart.value.y;
  if (!panStart.value.dragging && Math.hypot(dx, dy) <= 3) return;

  panStart.value.dragging = true;
  emit("update:pan", {
    x: panStart.value.origin.x + dx,
    y: panStart.value.origin.y + dy,
  });
}

function finishPan(event: PointerEvent) {
  if (!panStart.value) return;
  if (panStart.value.dragging) suppressBlankClick.value = true;
  const target = event.target instanceof SVGElement ? event.target : null;
  if (target?.hasPointerCapture?.(panStart.value.pointerId)) {
    target.releasePointerCapture(panStart.value.pointerId);
  }
  panStart.value = null;
}

function handleSvgClick() {
  if (suppressBlankClick.value) {
    suppressBlankClick.value = false;
    return;
  }
  emit("blank-click");
}

function edgePath(edge: GraphEdge): string {
  const source = nodeById.value.get(edge.source);
  const target = nodeById.value.get(edge.target);
  if (!source || !target) return "";

  const dx = target.x - source.x;
  const dy = target.y - source.y;
  const length = Math.max(1, Math.hypot(dx, dy));
  const start = {
    x: source.x + (dx / length) * Math.max(0, source.radius - 1),
    y: source.y + (dy / length) * Math.max(0, source.radius - 1),
  };
  const end = {
    x: target.x - (dx / length) * Math.max(0, target.radius - 1),
    y: target.y - (dy / length) * Math.max(0, target.radius - 1),
  };

  if (!edge.curve) return `M ${start.x} ${start.y} L ${end.x} ${end.y}`;

  const midX = (source.x + target.x) / 2;
  const midY = (source.y + target.y) / 2;
  const controlX = midX - (dy / length) * edge.curve;
  const controlY = midY + (dx / length) * edge.curve;
  return `M ${start.x} ${start.y} Q ${controlX} ${controlY} ${end.x} ${end.y}`;
}

function nodeGroupStyle(node: GraphNode) {
  return {
    transform: `translate(${node.x}px, ${node.y}px)`,
  };
}

function labelStyle(label: SvgLabel) {
  return {
    transform: `translate(${label.x}px, ${label.y}px)`,
  };
}

function edgeStyle(edge: GraphEdge) {
  const score = normalizedScore(edge);
  const baseWidth = edge.type === "marker" ? 0.68 : 0.68 + score * 0.96;
  const width = edge.highlighted ? baseWidth + 0.36 : baseWidth;
  const opacity = edge.newlyAdded ? undefined : edge.dimmed ? 0.12 : edge.highlighted ? 0.74 : edge.type === "marker" ? 0.16 : 0.16 + score * 0.12;
  const stroke = edge.type === "marker"
    ? edge.highlighted ? "rgba(14, 118, 106, 0.52)" : "rgba(22, 140, 124, 0.16)"
    : edge.highlighted ? `rgba(31, 79, 88, ${0.58 + score * 0.12})` : `rgba(88, 109, 126, ${0.18 + score * 0.12})`;

  return {
    stroke,
    strokeWidth: width,
    opacity,
  };
}

function nodeVisualStyle(node: GraphNode, index: number) {
  const origin = props.enteringNodeOrigins?.[node.id];
  if (!origin || !node.newlyAdded) return undefined;
  return {
    "--enter-x": `${origin.x - node.x}px`,
    "--enter-y": `${origin.y - node.y}px`,
    "--enter-delay": `${Math.min(index, 10) * 28}ms`,
  };
}

function nodeFill(node: GraphNode): string {
  if (node.type === "gene") return "url(#gene-node-gradient)";
  if (node.type === "peak") return "url(#peak-node-gradient)";
  if (node.type === "group") return "url(#group-node-gradient)";
  return "url(#tf-node-gradient)";
}

function nodeStroke(node: GraphNode): string {
  if (node.type === "gene") return "#d49b30";
  if (node.type === "peak") return "#8fa1b8";
  if (node.type === "group") return "#159383";
  return "#8b7ab8";
}

function edgeDelay(edge: GraphEdge, index: number) {
  if (!edge.newlyAdded) return undefined;
  return {
    animationDelay: `${Math.min(index, 10) * 24}ms`,
  };
}

function nodeClass(node: GraphNode): Record<string, boolean> {
  const hasActiveContext = Boolean(props.selectedNodeId || props.selectedEdgeId || props.hoveredNodeId || props.hoveredEdgeId);
  return {
    [`node-${node.type}`]: true,
    "is-selected": node.selected,
    "is-focused": props.focusedNodeId === node.id && !node.selected,
    "is-hovered": props.hoveredNodeId === node.id,
    "is-related": hasActiveContext && !node.dimmed && !node.selected,
    "is-new": node.newlyAdded,
    "is-dimmed": node.dimmed,
  };
}

function edgeClass(edge: GraphEdge): Record<string, boolean> {
  return {
    [`edge-${edge.type}`]: true,
    "is-highlighted": edge.highlighted,
    "is-new": edge.newlyAdded,
    "is-dimmed": edge.dimmed,
  };
}

function labelClass(node: GraphNode): Record<string, boolean> {
  return {
    [`label-${node.type}`]: true,
    "is-active-label": node.type === "peak" && isActivePeakLabel(node),
    "is-new": node.newlyAdded,
    "is-dimmed": node.dimmed,
  };
}

function createCenteredLabel(node: GraphNode): SvgLabel {
  const text = node.label;
  const width = estimateLabelWidth(text, node.type);
  const height = 23;

  return {
    node,
    text,
    anchor: "middle",
    placement: "center",
    x: node.x,
    y: node.y,
    width,
    height,
    pillX: -width / 2 - PEAK_LABEL_PADDING_X,
    pillY: -height / 2,
    priority: labelPriority(node),
    box: getCenteredLabelBox(node, text),
  };
}

function createPeakLabel(node: GraphNode, placedLabels: SvgLabel[]): SvgLabel {
  const text = isFullPeakLabel(node) ? node.label : shortPeakLabel(node.label);
  const width = estimateLabelWidth(text, node.type);
  const height = 20;
  const candidate = choosePeakLabelCandidate(node, width, height, placedLabels);

  return {
    node,
    text,
    anchor: candidate.anchor,
    placement: "external",
    x: candidate.x,
    y: candidate.y,
    width,
    height,
    pillX: candidate.pillX,
    pillY: candidate.pillY,
    priority: labelPriority(node),
    box: candidate.box,
  };
}

function labelPriority(node: GraphNode): number {
  if (node.id === props.selectedNodeId || node.id === props.hoveredNodeId) return 100;
  if (node.id === props.focusedNodeId) return 96;
  const edge = selectedEdge.value ?? hoveredEdge.value;
  if (edge && (edge.source === node.id || edge.target === node.id)) return 92;
  if (node.type === "gene" || node.type === "group" || node.type === "tf") return 82;
  if (node.newlyAdded) return 62;
  if (selectedPeakLabelIds.value.has(node.id) || hoveredPeakLabelIds.value.has(node.id)) return 58;
  return 10;
}

function getCenteredLabelBox(node: GraphNode, text: string): LabelBox {
  const width = estimateLabelWidth(text, node.type);
  const height = 23;
  return {
    left: node.x - width / 2,
    top: node.y - height / 2,
    right: node.x + width / 2,
    bottom: node.y + height / 2,
  };
}

function nodeLayerOrder(node: GraphNode): number {
  if (node.type === "peak") return 0;
  if (node.type === "gene" || node.type === "tf") return 1;
  return 2;
}

function choosePeakLabelCandidate(node: GraphNode, width: number, height: number, placedLabels: SvgLabel[]): PeakLabelCandidate {
  const candidates = peakLabelDirections
    .map(({ direction, vector }) => buildPeakLabelCandidate(node, width, height, direction, vector))
    .map((candidate) => nudgePeakLabelCandidate(node, candidate, placedLabels));

  return candidates
    .map((candidate) => ({
      candidate,
      score: scorePeakLabelCandidate(node, candidate, placedLabels),
    }))
    .sort((a, b) => a.score - b.score)[0]?.candidate
    ?? buildPeakLabelCandidate(node, width, height, "right", { x: 1, y: 0 });
}

function buildPeakLabelCandidate(
  node: GraphNode,
  width: number,
  height: number,
  direction: PeakLabelDirection,
  vector: GraphPoint
): PeakLabelCandidate {
  const unit = normalizeVector(vector);
  const halfWidth = width / 2 + PEAK_LABEL_PADDING_X;
  const halfHeight = height / 2;
  const supportDistance = Math.abs(unit.x) * halfWidth + Math.abs(unit.y) * halfHeight;
  const maxDistance = getPeakLabelMaxDistance(node, width);
  const distance = Math.min(node.radius + PEAK_LABEL_GAP + supportDistance, maxDistance);
  const center = {
    x: node.x + unit.x * distance,
    y: node.y + unit.y * distance,
  };
  const box = {
    left: center.x - halfWidth,
    top: center.y - halfHeight,
    right: center.x + halfWidth,
    bottom: center.y + halfHeight,
  };
  const anchor = getPeakLabelAnchor(unit);
  const x = anchor === "start"
    ? box.left + PEAK_LABEL_PADDING_X
    : anchor === "end"
      ? box.right - PEAK_LABEL_PADDING_X
      : center.x;
  const pillX = anchor === "start"
    ? -PEAK_LABEL_PADDING_X
    : anchor === "end"
      ? -width - PEAK_LABEL_PADDING_X
      : -width / 2 - PEAK_LABEL_PADDING_X;

  return {
    direction,
    anchor,
    x,
    y: center.y,
    pillX,
    pillY: -height / 2,
    box,
    angle: Math.atan2(unit.y, unit.x),
    distance,
    unit,
  };
}

function getPeakLabelAnchor(unit: GraphPoint): "start" | "end" | "middle" {
  if (unit.x > 0.35) return "start";
  if (unit.x < -0.35) return "end";
  return "middle";
}

function nudgePeakLabelCandidate(
  node: GraphNode,
  candidate: PeakLabelCandidate,
  placedLabels: SvgLabel[]
): PeakLabelCandidate {
  const tangent = { x: -candidate.unit.y, y: candidate.unit.x };
  const offsets: GraphPoint[] = [
    { x: 0, y: 0 },
    { x: candidate.unit.x * PEAK_LABEL_MAX_NUDGE, y: candidate.unit.y * PEAK_LABEL_MAX_NUDGE },
    { x: tangent.x * PEAK_LABEL_MAX_NUDGE, y: tangent.y * PEAK_LABEL_MAX_NUDGE },
    { x: -tangent.x * PEAK_LABEL_MAX_NUDGE, y: -tangent.y * PEAK_LABEL_MAX_NUDGE },
    { x: candidate.unit.x * 4 + tangent.x * 4, y: candidate.unit.y * 4 + tangent.y * 4 },
    { x: candidate.unit.x * 4 - tangent.x * 4, y: candidate.unit.y * 4 - tangent.y * 4 },
  ];

  return offsets
    .map((offset) => {
      const nudged = constrainPeakLabelCandidate(node, shiftPeakLabelCandidate(candidate, offset));
      const nudgeCost = Math.hypot(offset.x, offset.y) * 34;
      return {
        candidate: nudged,
        score: scorePeakLabelCandidate(node, nudged, placedLabels) + nudgeCost,
      };
    })
    .sort((a, b) => a.score - b.score)[0]?.candidate ?? candidate;
}

function shiftPeakLabelCandidate(candidate: PeakLabelCandidate, offset: GraphPoint): PeakLabelCandidate {
  return {
    ...candidate,
    x: candidate.x + offset.x,
    y: candidate.y + offset.y,
    box: {
      left: candidate.box.left + offset.x,
      top: candidate.box.top + offset.y,
      right: candidate.box.right + offset.x,
      bottom: candidate.box.bottom + offset.y,
    },
    distance: candidate.distance + Math.max(0, candidate.unit.x * offset.x + candidate.unit.y * offset.y),
  };
}

function constrainPeakLabelCandidate(node: GraphNode, candidate: PeakLabelCandidate): PeakLabelCandidate {
  let constrained = candidate;
  const labelWidth = constrained.box.right - constrained.box.left - PEAK_LABEL_PADDING_X * 2;
  const maxDistance = getPeakLabelMaxDistance(node, labelWidth);
  const center = boxCenter(constrained.box);
  const centerDistance = Math.hypot(center.x - node.x, center.y - node.y);

  if (centerDistance > maxDistance) {
    const unit = normalizeVector({ x: center.x - node.x, y: center.y - node.y });
    const clampedCenter = {
      x: node.x + unit.x * maxDistance,
      y: node.y + unit.y * maxDistance,
    };
    constrained = shiftPeakLabelCandidate(constrained, {
      x: clampedCenter.x - center.x,
      y: clampedCenter.y - center.y,
    });
  }

  const edgeGap = Math.max(0, distanceFromPointToBox(node, constrained.box) - node.radius);
  if (edgeGap > PEAK_LABEL_MAX_GAP) {
    const currentCenter = boxCenter(constrained.box);
    const inward = normalizeVector({ x: node.x - currentCenter.x, y: node.y - currentCenter.y });
    constrained = shiftPeakLabelCandidate(constrained, {
      x: inward.x * (edgeGap - PEAK_LABEL_MAX_GAP),
      y: inward.y * (edgeGap - PEAK_LABEL_MAX_GAP),
    });
  }

  const finalCenter = boxCenter(constrained.box);
  return {
    ...constrained,
    distance: Math.hypot(finalCenter.x - node.x, finalCenter.y - node.y),
  };
}

function scorePeakLabelCandidate(node: GraphNode, candidate: PeakLabelCandidate, placedLabels: SvgLabel[]): number {
  const relatedEdges = getNodeEdges(node.id);
  const relatedEdgeIds = new Set(relatedEdges.map((edge) => edge.id));
  let score = boundaryPenalty(candidate.box) * 2400;
  score += peakLabelAnchorDistancePenalty(node, candidate);

  relatedEdges.forEach((edge) => {
    if (polylineIntersectsBox(getEdgeRoutePoints(edge), candidate.box)) score += 42000;
  });

  renderEdges.value.forEach((edge) => {
    if (relatedEdgeIds.has(edge.id)) return;
    if (polylineIntersectsBox(getEdgeRoutePoints(edge), candidate.box)) score += 22000;
  });

  placedLabels.forEach((label) => {
    if (boxesOverlap(candidate.box, label.box, 4)) {
      score += label.node.type === "peak" ? 28000 : 36000;
    }
  });

  displayGraph.value.nodes.forEach((otherNode) => {
    if (otherNode.id === node.id) return;
    if (circleIntersectsBox(otherNode, candidate.box, 4)) {
      score += otherNode.type === "peak" ? 24000 : 38000;
    }
  });

  score += edgeDensityPenalty(node, candidate);
  score += candidate.distance * 72;
  score -= outwardDirectionReward(node, candidate) * 800;
  return score;
}

function peakLabelAnchorDistancePenalty(node: GraphNode, candidate: PeakLabelCandidate): number {
  const center = boxCenter(candidate.box);
  const centerDistance = Math.hypot(center.x - node.x, center.y - node.y);
  const maxDistance = getPeakLabelMaxDistance(node, candidate.box.right - candidate.box.left - PEAK_LABEL_PADDING_X * 2);
  const edgeGap = Math.max(0, distanceFromPointToBox(node, candidate.box) - node.radius);
  let score = 0;

  if (centerDistance > maxDistance) score += 180000 + (centerDistance - maxDistance) * 9000;
  if (edgeGap > PEAK_LABEL_MAX_GAP) score += 85000 + (edgeGap - PEAK_LABEL_MAX_GAP) * 5200;
  if (edgeGap < 6) score += (6 - edgeGap) * 1800;
  score += Math.max(0, edgeGap - PEAK_LABEL_GAP) * 1400;
  return score;
}

function edgeDensityPenalty(node: GraphNode, candidate: PeakLabelCandidate): number {
  const relatedAngles = getNodeEdges(node.id)
    .map((edge) => {
      const otherId = edge.source === node.id ? edge.target : edge.source;
      const otherNode = nodeById.value.get(otherId);
      return otherNode ? Math.atan2(otherNode.y - node.y, otherNode.x - node.x) : null;
    })
    .filter((angle): angle is number => angle !== null);

  return relatedAngles.reduce((score, angle) => {
    const distance = angularDistance(candidate.angle, angle);
    if (distance < Math.PI / 5) return score + (1 - distance / (Math.PI / 5)) * 16500;
    if (distance < Math.PI / 3) return score + (1 - distance / (Math.PI / 3)) * 6200;
    return score;
  }, 0);
}

function outwardDirectionReward(node: GraphNode, candidate: PeakLabelCandidate): number {
  const graphCenter = { x: props.dimensions.width / 2, y: props.dimensions.height / 2 };
  const outward = normalizeVector({ x: node.x - graphCenter.x, y: node.y - graphCenter.y });
  return Math.max(0, outward.x * candidate.unit.x + outward.y * candidate.unit.y);
}

function getPeakLabelMaxDistance(node: GraphNode, labelWidth: number): number {
  return node.radius + labelWidth / 2 + PEAK_LABEL_MAX_EXTRA_DISTANCE;
}

function normalizeVector(vector: GraphPoint): GraphPoint {
  const length = Math.max(0.001, Math.hypot(vector.x, vector.y));
  return {
    x: vector.x / length,
    y: vector.y / length,
  };
}

function getNodeEdges(nodeId: string): GraphEdge[] {
  return renderEdges.value.filter((edge) => edge.source === nodeId || edge.target === nodeId);
}

function getEdgeRoutePoints(edge: GraphEdge): GraphPoint[] {
  const source = nodeById.value.get(edge.source);
  const target = nodeById.value.get(edge.target);
  if (!source || !target) return [];

  const dx = target.x - source.x;
  const dy = target.y - source.y;
  const length = Math.max(1, Math.hypot(dx, dy));
  const start = {
    x: source.x + (dx / length) * Math.max(0, source.radius - 1),
    y: source.y + (dy / length) * Math.max(0, source.radius - 1),
  };
  const end = {
    x: target.x - (dx / length) * Math.max(0, target.radius - 1),
    y: target.y - (dy / length) * Math.max(0, target.radius - 1),
  };

  if (!edge.curve) return [start, end];

  const midX = (source.x + target.x) / 2;
  const midY = (source.y + target.y) / 2;
  const control = {
    x: midX - (dy / length) * edge.curve,
    y: midY + (dx / length) * edge.curve,
  };
  const points: GraphPoint[] = [];

  for (let index = 0; index <= 12; index += 1) {
    const t = index / 12;
    const oneMinusT = 1 - t;
    points.push({
      x: oneMinusT * oneMinusT * start.x + 2 * oneMinusT * t * control.x + t * t * end.x,
      y: oneMinusT * oneMinusT * start.y + 2 * oneMinusT * t * control.y + t * t * end.y,
    });
  }

  return points;
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

function boxCenter(box: LabelBox): GraphPoint {
  return {
    x: (box.left + box.right) / 2,
    y: (box.top + box.bottom) / 2,
  };
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

function boxesOverlap(a: LabelBox, b: LabelBox, padding = 0): boolean {
  return a.left - padding < b.right
    && a.right + padding > b.left
    && a.top - padding < b.bottom
    && a.bottom + padding > b.top;
}

function circleIntersectsBox(node: GraphNode, box: LabelBox, padding = 0): boolean {
  const nearestX = Math.max(box.left - padding, Math.min(node.x, box.right + padding));
  const nearestY = Math.max(box.top - padding, Math.min(node.y, box.bottom + padding));
  return Math.hypot(node.x - nearestX, node.y - nearestY) <= node.radius + padding;
}

function distanceFromPointToBox(point: GraphPoint, box: LabelBox): number {
  const dx = Math.max(box.left - point.x, 0, point.x - box.right);
  const dy = Math.max(box.top - point.y, 0, point.y - box.bottom);
  return Math.hypot(dx, dy);
}

function boundaryPenalty(box: LabelBox): number {
  return Math.max(0, -box.left + 10)
    + Math.max(0, box.right - props.dimensions.width + 10)
    + Math.max(0, -box.top + 10)
    + Math.max(0, box.bottom - props.dimensions.height + 10);
}

function angularDistance(left: number, right: number): number {
  const raw = Math.abs(left - right) % (Math.PI * 2);
  return raw > Math.PI ? Math.PI * 2 - raw : raw;
}

function shouldRenderNodeLabel(node: GraphNode): boolean {
  if (isCenteredNodeLabel(node)) return true;
  if (node.type !== "peak") return true;

  if (hasHoverPreviewContext()) return isHoverRelatedPeak(node.id);
  if (hasSelectedPreviewContext()) return isSelectedRelatedPeak(node.id);

  if (props.peakLabelMode === "overview") return true;
  return false;
}

function isActivePeakLabel(node: GraphNode): boolean {
  if (node.type !== "peak") return false;
  if (hasHoverPreviewContext()) return isHoverRelatedPeak(node.id);
  if (hasSelectedPreviewContext()) return isSelectedRelatedPeak(node.id);
  return false;
}

function isCenteredNodeLabel(node: GraphNode): boolean {
  return node.type === "gene" || node.type === "group" || node.type === "tf";
}

function hasHoverPreviewContext(): boolean {
  return Boolean(props.hoveredNodeId || props.hoveredEdgeId);
}

function hasSelectedPreviewContext(): boolean {
  return Boolean(props.selectedNodeId || props.selectedEdgeId);
}

function isHoverRelatedPeak(nodeId: string): boolean {
  return hoveredPeakLabelIds.value.has(nodeId) || hoveredEdgePeakLabelIds.value.has(nodeId);
}

function isSelectedRelatedPeak(nodeId: string): boolean {
  return selectedPeakLabelIds.value.has(nodeId) || selectedEdgePeakLabelIds.value.has(nodeId);
}

function getRelatedPeakIdsForNode(seedNodeId: string): Set<string> {
  const peakIds = new Set<string>();
  const seedNode = nodeById.value.get(seedNodeId);
  if (!seedNode) return peakIds;

  if (seedNode.type === "peak") {
    peakIds.add(seedNode.id);
    return peakIds;
  }

  if (seedNode.type === "group") {
    const markerGeneIds = getMarkerGeneIdsForGroup(seedNode.id);
    markerGeneIds.forEach((geneId) => addDirectPeakNeighbors(peakIds, geneId));
    addDirectPeakNeighbors(peakIds, seedNode.id);
    return peakIds;
  }

  addDirectPeakNeighbors(peakIds, seedNode.id);
  return peakIds;
}

function getRelatedPeakIdsForEdge(edgeId: string): Set<string> {
  const peakIds = new Set<string>();
  const edge = renderEdges.value.find((candidate) => candidate.id === edgeId);
  if (!edge) return peakIds;

  addPeakIfPresent(peakIds, edge.source);
  addPeakIfPresent(peakIds, edge.target);

  if (!peakIds.size) {
    const sourceNode = nodeById.value.get(edge.source);
    const targetNode = nodeById.value.get(edge.target);
    if (sourceNode?.type === "gene" || sourceNode?.type === "tf") addDirectPeakNeighbors(peakIds, sourceNode.id);
    if (targetNode?.type === "gene" || targetNode?.type === "tf") addDirectPeakNeighbors(peakIds, targetNode.id);
    if (sourceNode?.type === "group") getMarkerGeneIdsForGroup(sourceNode.id).forEach((geneId) => addDirectPeakNeighbors(peakIds, geneId));
    if (targetNode?.type === "group") getMarkerGeneIdsForGroup(targetNode.id).forEach((geneId) => addDirectPeakNeighbors(peakIds, geneId));
  }

  return peakIds;
}

function getMarkerGeneIdsForGroup(groupId: string): Set<string> {
  const geneIds = new Set<string>();
  renderEdges.value.forEach((edge) => {
    if (edge.type !== "marker") return;
    if (edge.source !== groupId && edge.target !== groupId) return;
    const otherId = edge.source === groupId ? edge.target : edge.source;
    const otherNode = nodeById.value.get(otherId);
    if (otherNode?.type === "gene") geneIds.add(otherNode.id);
  });
  return geneIds;
}

function addDirectPeakNeighbors(peakIds: Set<string>, nodeId: string) {
  renderEdges.value.forEach((edge) => {
    if (edge.source !== nodeId && edge.target !== nodeId) return;
    addPeakIfPresent(peakIds, edge.source === nodeId ? edge.target : edge.source);
  });
}

function addPeakIfPresent(peakIds: Set<string>, nodeId: string) {
  const node = nodeById.value.get(nodeId);
  if (node?.type === "peak") peakIds.add(node.id);
}

function normalizedScore(edge: GraphEdge): number {
  const scores = props.graph.edges
    .filter((candidate) => candidate.type === "peakGene" && typeof candidate.score === "number")
    .map((candidate) => candidate.score as number);
  if (!scores.length || typeof edge.score !== "number") return 0.38;
  const min = Math.min(...scores);
  const max = Math.max(...scores);
  if (Math.abs(max - min) <= 1e-6) return Math.max(0.2, Math.min(1, edge.score));
  return Math.max(0, Math.min(1, (edge.score - min) / (max - min)));
}

function isFullPeakLabel(node: GraphNode): boolean {
  if (node.type !== "peak") return true;
  if (hasHoverPreviewContext()) return isHoverRelatedPeak(node.id);
  if (hasSelectedPreviewContext()) return isSelectedRelatedPeak(node.id);
  return props.peakLabelMode === "overview";
}

function makeGridLines(min: number, max: number): number[] {
  const start = Math.floor(min / GRID_STEP) * GRID_STEP;
  const lines: number[] = [];
  for (let value = start; value <= max; value += GRID_STEP) lines.push(value);
  return lines;
}

function animateNodePositions(graph: RegulatoryGraph) {
  if (nodeAnimationFrame !== undefined) window.cancelAnimationFrame(nodeAnimationFrame);

  const previousNodes = new Map(animatedNodes.value.map((node) => [node.id, node] as const));
  const isSameGraph = graph.nodes.length === animatedNodes.value.length
    && graph.nodes.every((n) => previousNodes.has(n.id));

  if (isSameGraph) {
    // Same node set (e.g., expansion / pan change) — smooth positional animation.
    animatedNodes.value = graph.nodes.map((node) => {
      const prev = previousNodes.get(node.id)!;
      return { ...cloneNode(node), x: prev.x, y: prev.y };
    });
    nodeAnimationFrame = window.requestAnimationFrame(() => {
      animatedNodes.value = graph.nodes.map(cloneNode);
    });
  } else {
    // Different node set (e.g., new search) — instant swap, no animation.
    nodeAnimationFrame = window.requestAnimationFrame(() => {
      animatedNodes.value = graph.nodes.map(cloneNode);
    });
  }
}

function cloneNode(node: GraphNode): GraphNode {
  return {
    ...node,
    metadata: {
      ...node.metadata,
    },
  };
}
</script>

<style scoped>
.regulatory-network-canvas {
  position: relative;
  isolation: isolate;
  width: 100%;
  min-height: 480px;
  max-height: 540px;
  overflow: hidden;
  cursor: grab;
  background:
    radial-gradient(ellipse at 19% 22%, rgba(53, 186, 174, 0.082), rgba(53, 186, 174, 0) 52%),
    radial-gradient(ellipse at 78% 66%, rgba(108, 158, 218, 0.078), rgba(108, 158, 218, 0) 58%),
    radial-gradient(ellipse at 52% 108%, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0) 46%),
    linear-gradient(118deg, rgba(255, 255, 255, 0.98), rgba(247, 253, 251, 0.94) 43%, rgba(250, 253, 255, 0.97));
  touch-action: none;
  backdrop-filter: blur(14px) saturate(112%);
}

.regulatory-network-canvas::before,
.regulatory-network-canvas::after {
  position: absolute;
  inset: -18%;
  z-index: -1;
  content: "";
  pointer-events: none;
}

.regulatory-network-canvas::before {
  background:
    radial-gradient(ellipse at 16% 38%, rgba(83, 188, 177, 0.064), rgba(83, 188, 177, 0) 50%),
    radial-gradient(ellipse at 73% 42%, rgba(150, 188, 226, 0.058), rgba(150, 188, 226, 0) 54%),
    linear-gradient(104deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.22) 43%, rgba(255, 255, 255, 0) 70%);
  filter: blur(34px);
  opacity: 0.34;
  transform: translate3d(-3%, -2%, 0) rotate(0.001deg);
  animation: glass-flow 44s ease-in-out infinite alternate;
}

.regulatory-network-canvas::after {
  background:
    radial-gradient(ellipse at 62% 26%, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0) 48%),
    radial-gradient(ellipse at 34% 76%, rgba(212, 244, 240, 0.052), rgba(212, 244, 240, 0) 52%),
    linear-gradient(150deg, rgba(255, 255, 255, 0.1), rgba(225, 247, 243, 0.06) 38%, rgba(241, 247, 255, 0.09) 70%, rgba(255, 255, 255, 0));
  filter: blur(48px);
  opacity: 0.26;
  animation: glass-mist 52s ease-in-out infinite alternate;
}

.regulatory-network-canvas:active {
  cursor: grabbing;
}

.network-svg {
  --enter-x: 0px;
  --enter-y: 0px;
  --enter-delay: 0ms;
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 100%;
}

.pan-surface {
  fill: transparent;
  pointer-events: all;
}

.grid-line {
  stroke: rgba(104, 138, 134, 0.024);
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
}

.network-edge-item {
  cursor: pointer;
}

.network-edge-hit {
  fill: none;
  stroke: transparent;
  stroke-linecap: round;
  stroke-width: 14;
  cursor: pointer;
  pointer-events: stroke;
}

.network-edge {
  fill: none;
  pointer-events: none;
  stroke-linecap: round;
  filter: drop-shadow(0 1px 1px rgba(45, 64, 70, 0.028));
  transition:
    opacity 0.22s ease,
    stroke 0.22s ease,
    stroke-width 0.22s ease;
}

.edge-marker {
  stroke-dasharray: 2.5 7;
}

.network-edge.is-new {
  animation: edge-grow 620ms ease both;
}

.network-edge.is-highlighted:not(.is-new) {
  stroke-dasharray: 8 18;
  animation: edge-flow 5.8s linear infinite;
  filter: drop-shadow(0 2px 4px rgba(34, 64, 70, 0.046));
}

.edge-marker.is-highlighted:not(.is-new) {
  stroke-dasharray: 3 8;
  animation-duration: 6.4s;
}

.network-node {
  cursor: pointer;
  transition:
    opacity 0.22s ease,
    filter 0.24s ease,
    transform 620ms cubic-bezier(0.19, 0.8, 0.22, 1);
  transform-box: view-box;
  transform-origin: center;
}

.node-visual {
  transform-box: fill-box;
  transform-origin: center;
}

.node-circle {
  stroke-width: 1.15;
  fill-opacity: 0.88;
  transition:
    fill-opacity 0.18s ease,
    stroke 0.18s ease,
    stroke-width 0.18s ease,
    filter 0.18s ease;
}

.node-sheen {
  fill: rgba(255, 255, 255, 0.18);
  opacity: 0.3;
  pointer-events: none;
  mix-blend-mode: screen;
}

.node-halo {
  opacity: 0;
  fill: transparent;
  transition: opacity 0.18s ease;
}

.node-gene .node-circle {
  stroke: #e6a23c;
}

.node-peak .node-circle {
  stroke: #94a3b8;
}

.node-group .node-circle {
  stroke: #168c7c;
}

.node-tf .node-circle {
  stroke: #8b7ab8;
}

.network-node.is-selected .node-circle,
.network-node.is-focused .node-circle,
.network-node.is-hovered .node-circle {
  stroke-width: 1.85;
  fill-opacity: 0.96;
}

.network-node.is-selected .node-halo,
.network-node.is-focused .node-halo {
  opacity: 0.56;
  filter: url("#selected-node-halo");
  fill: rgba(22, 140, 124, 0.096);
  animation: halo-breathe 3.8s ease-in-out infinite;
}

.network-node.is-focused .node-halo {
  fill: rgba(82, 142, 180, 0.078);
  animation-duration: 4.2s;
}

.network-node.is-hovered {
  filter: drop-shadow(0 7px 14px rgba(45, 64, 70, 0.105));
}

.network-node.is-hovered .node-halo {
  opacity: 0.42;
  fill: rgba(22, 140, 124, 0.075);
  animation: halo-breathe 3.2s ease-in-out infinite;
}

.network-node.is-related .node-halo {
  opacity: 0.28;
  fill: rgba(122, 185, 174, 0.07);
}

.network-node.is-related {
  filter: drop-shadow(0 5px 11px rgba(45, 64, 70, 0.065));
}

.network-node.is-new .node-halo {
  opacity: 0.46;
  fill: rgba(86, 154, 191, 0.1);
  animation: halo-breathe 3.1s ease-in-out infinite;
}

.network-node.is-dimmed {
  opacity: 0.46;
}

.network-node.is-new .node-visual {
  animation: node-wake 560ms cubic-bezier(0.2, 0.82, 0.2, 1) both;
  animation-delay: var(--enter-delay, 0ms);
  transform-box: fill-box;
  transform-origin: center;
}

.label-item {
  pointer-events: none;
  transition:
    opacity 0.22s ease,
    transform 620ms cubic-bezier(0.19, 0.8, 0.22, 1);
  transform-box: view-box;
}

.label-pill {
  fill: rgba(255, 255, 255, 0.34);
  stroke: rgba(132, 155, 150, 0.09);
  stroke-width: 1;
  filter: drop-shadow(0 1px 3px rgba(45, 64, 70, 0.032));
}

.node-label {
  pointer-events: none;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.86);
  stroke-width: 2.8px;
  font-weight: 760;
  letter-spacing: 0;
}

.label-gene {
  fill: #5c4320;
  font-size: 12px;
}

.label-group {
  fill: #07574f;
  font-size: 12px;
}

.label-peak {
  fill: rgba(71, 85, 105, 0.78);
  font-size: 11px;
  font-weight: 610;
}

.label-tf {
  fill: #5f5384;
  font-size: 12px;
}

.label-centered .node-label {
  stroke: rgba(255, 255, 255, 0.26);
  stroke-width: 0.75px;
  font-weight: 780;
  filter: drop-shadow(0 1px 1px rgba(255, 255, 255, 0.34)) drop-shadow(0 1px 1px rgba(35, 56, 58, 0.12));
}

.label-item.is-dimmed {
  opacity: 0.54;
}

.label-item.is-new {
  animation: label-wake 420ms ease both;
}

.label-peak .label-pill {
  fill: rgba(248, 251, 255, 0.3);
  stroke: rgba(106, 132, 143, 0.075);
}

.label-peak.is-active-label {
  fill: rgba(38, 54, 68, 0.9);
  font-weight: 700;
}

.label-peak.is-active-label .label-pill {
  fill: rgba(255, 255, 255, 0.58);
  stroke: rgba(58, 118, 126, 0.16);
  filter: drop-shadow(0 2px 5px rgba(31, 58, 64, 0.055));
}

@keyframes glass-flow {
  from {
    transform: translate3d(-2%, -1%, 0) scale(1);
  }

  to {
    transform: translate3d(2.5%, 1.5%, 0) scale(1.025);
  }
}

@keyframes glass-mist {
  from {
    transform: translate3d(1.5%, 1%, 0) scale(1.01);
  }

  to {
    transform: translate3d(-1.5%, -2%, 0) scale(1.03);
  }
}

@keyframes node-wake {
  from {
    opacity: 0;
    transform: translate(var(--enter-x, 0), var(--enter-y, 0)) scale(0.84);
  }

  68% {
    opacity: 1;
    transform: translate(0, 0) scale(1.018);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes label-wake {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes edge-grow {
  from {
    opacity: 0;
    stroke-dasharray: 3 42;
    stroke-dashoffset: 45;
  }

  to {
    opacity: 1;
    stroke-dasharray: 45 0;
    stroke-dashoffset: 0;
  }
}

@keyframes edge-flow {
  from {
    stroke-dashoffset: 25;
  }

  to {
    stroke-dashoffset: 0;
  }
}

@keyframes halo-breathe {
  0%,
  100% {
    opacity: 0.36;
    transform: scale(0.98);
  }

  50% {
    opacity: 0.6;
    transform: scale(1.05);
  }
}

@media (max-width: 760px) {
  .regulatory-network-canvas {
    min-height: 480px;
    max-height: 520px;
  }
}
</style>

<template>
  <section ref="sectionEl" class="regulatory-section float-card">
    <div class="section-head">
      <div>
        <div class="section-title">Regulatory network</div>
        <div class="section-sub">Explore direct peak-to-gene links within this sample.</div>
      </div>

      <div class="section-tags">
        <span class="data-chip" :title="domainTitle">{{ domainChip }}</span>
        <span class="data-chip mono" :title="datasetId">{{ datasetId }}</span>
      </div>
    </div>

    <div class="network-control-card">
      <div class="mode-switch" role="tablist" aria-label="Regulatory query mode">
        <button
          v-for="option in modeOptions"
          :key="option.value"
          type="button"
          class="mode-button"
          :class="{ active: queryMode === option.value }"
          :aria-selected="queryMode === option.value"
          role="tab"
          @click="setQueryMode(option.value)"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="query-panel">
        <template v-if="queryMode === 'gene'">
          <label class="control-field control-field--grow" :class="{ 'control-field--invalid': geneQueryError }">
            <span class="control-label">Gene symbol</span>
            <Transition name="query-error">
              <span v-if="geneQueryError" class="query-input-error" role="alert" aria-live="polite">{{ geneQueryError }}</span>
            </Transition>
            <el-input
              v-model="geneQuery"
              class="control-input"
              placeholder="Enter single gene symbol"
              clearable
              size="small"
              @input="clearQueryInputError('gene')"
              @keyup.enter="searchNetwork"
            />
          </label>

          <label class="control-field">
            <span class="control-label">Minimum score</span>
            <el-input
              v-model="geneMinScore"
              class="control-input"
              placeholder="0.5"
              clearable
              inputmode="decimal"
              size="small"
              @keyup.enter="searchNetwork"
            />
          </label>
        </template>

        <template v-else>
          <label class="control-field control-field--grow" :class="{ 'control-field--invalid': peakQueryError }">
            <span class="control-label">Peak region</span>
            <Transition name="query-error">
              <span v-if="peakQueryError" class="query-input-error" role="alert" aria-live="polite">{{ peakQueryError }}</span>
            </Transition>
            <el-input
              v-model="peakQuery"
              class="control-input"
              placeholder="Enter single peak region"
              clearable
              size="small"
              @input="clearQueryInputError('peak')"
              @keyup.enter="searchNetwork"
            />
          </label>

          <label class="control-field" :class="{ 'control-field--invalid': linkedGeneQueryError }">
            <span class="control-label">Linked gene</span>
            <Transition name="query-error">
              <span v-if="linkedGeneQueryError" class="query-input-error" role="alert" aria-live="polite">{{ linkedGeneQueryError }}</span>
            </Transition>
            <el-input
              v-model="linkedGeneQuery"
              class="control-input"
              placeholder="Enter single gene symbol"
              clearable
              size="small"
              @input="clearQueryInputError('linkedGene')"
              @keyup.enter="searchNetwork"
            />
          </label>

          <label class="control-field">
            <span class="control-label">Minimum score</span>
            <el-input
              v-model="peakMinScore"
              class="control-input"
              placeholder="0.5"
              clearable
              inputmode="decimal"
              size="small"
              @keyup.enter="searchNetwork"
            />
          </label>
        </template>

        <el-button type="primary" class="section-button" :loading="loading" @click="searchNetwork">
          <el-icon><Search /></el-icon>
          <span>Search</span>
        </el-button>
        <el-button class="soft-button reset-button" @click="resetNetwork">
          <el-icon><Refresh /></el-icon>
          <span>Reset</span>
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading && !hasGraphData" animated :rows="8" />

    <div v-else-if="error" class="state-message state-message--error">
      Regulatory network request failed. Please retry.
    </div>

    <template v-else>
      <div class="network-browser">
        <div ref="graphHostEl" class="graph-shell" aria-label="Regulatory network graph">
          <div class="graph-heading">
            <span>Network view</span>
            <div v-if="hasGraphData" class="graph-legend" aria-label="Regulatory graph legend">
              <span class="legend-item">
                <span class="legend-swatches" aria-hidden="true">
                  <span class="legend-dot legend-dot--gene"></span>
                  <span class="legend-dot legend-dot--gene-alt"></span>
                </span>
                Gene
              </span>
              <span class="legend-item">
                <span class="legend-swatches" aria-hidden="true">
                  <span class="legend-dot legend-dot--peak"></span>
                  <span class="legend-dot legend-dot--peak-alt"></span>
                </span>
                Peak
              </span>
              <span class="legend-item"><span class="legend-line legend-line--peak"></span>Peak-to-gene link</span>
            </div>
          </div>

          <RegulatoryNetworkSvg
            v-if="hasGraphData"
            v-model:pan="viewportPan"
            :graph="renderGraph"
            :dimensions="graphDimensions"
            :selected-node-id="selectedNodeId"
            :selected-edge-id="selectedEdgeId"
            :hovered-node-id="hoveredNodeId"
            :hovered-edge-id="hoveredEdgeId"
            :focused-node-id="focusedNodeId"
            :peak-label-mode="peakLabelMode"
            :entering-node-origins="enteringNodeOrigins"
            @node-click="selectNode"
            @edge-click="selectEdge"
            @blank-click="clearSelection"
            @node-hover="setHoveredNode"
            @edge-hover="setHoveredEdge"
          />
          <button v-if="hasGraphData" class="graph-reset-view" type="button" @click.stop="resetGraphView">
            Reset view
          </button>
          <div v-else class="graph-body">
            {{ graphMessage }}
          </div>
        </div>

        <aside class="inspector-panel" aria-label="Detail inspector">
          <template v-if="selectedDetail">
            <div
              :key="`${selectedDetail.kind}:${selectedDetail.title}`"
              class="inspector-card inspector-card--details"
            >
              <div class="inspector-head">
                <div class="inspector-heading-copy">
                  <div class="inspector-kicker">Details</div>
                  <span
                    class="inspector-chip"
                    :class="`inspector-chip--${selectedDetail.kind}`"
                    :style="inspectorChipStyle"
                  >
                    {{ selectedDetail.chip }}
                  </span>
                </div>
                <el-button
                  v-if="fullLinksAction"
                  class="inspector-full-links-button"
                  :class="{ active: fullLinksDialogVisible }"
                  size="small"
                  plain
                  :loading="fullLinksLoading && fullLinksDialogVisible"
                  @click="openFullLinksDialog"
                >
                  {{ fullLinksAction.label }}
                </el-button>
                <button class="inspector-close" type="button" aria-label="Clear detail" @click="clearSelection">
                  x
                </button>
              </div>

              <div class="inspector-details-scroll">
                <div
                  v-if="selectedDetail.kind === 'edge'"
                  class="inspector-title-scroll"
                  tabindex="0"
                  aria-label="Edge detail title"
                >
                  <div class="inspector-title inspector-title--edge">
                    {{ selectedDetail.title }}
                  </div>
                </div>
                <div v-else class="inspector-title">
                  {{ selectedDetail.title }}
                </div>
                <div v-if="selectedDetailSubtitle" class="inspector-subtitle">
                  {{ selectedDetailSubtitle }}
                </div>

                <div class="inspector-grid">
                  <div v-for="item in selectedDetail.items" :key="item.label" class="detail-row">
                    <span class="detail-label">{{ item.label }}</span>
                    <span class="detail-value" :title="item.value">{{ item.value }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="selectedDetail.actions?.length || inspectorHelper" class="inspector-card inspector-card--actions">
              <div class="inspector-card-label">Actions</div>
              <div v-if="selectedDetail.actions?.length" class="inspector-actions">
                <el-button
                  v-for="action in selectedDetail.actions"
                  :key="action.action"
                  :type="action.variant === 'primary' ? 'primary' : undefined"
                  :class="[
                    'inspector-action',
                    action.variant === 'primary' ? 'inspector-action--primary' : 'soft-button',
                  ]"
                  size="small"
                  :disabled="action.disabled || actionLoading(action.action)"
                  :loading="actionLoading(action.action)"
                  :title="action.title"
                  @click="runInspectorAction(action.action)"
                >
                  {{ action.label }}
                </el-button>
              </div>

              <div v-if="inspectorHelper" class="inspector-helper">
                <span class="inspector-helper-text">{{ inspectorHelper }}</span>
                <el-button
                  v-if="helperContinueAvailable"
                  class="inspector-helper-action"
                  size="small"
                  plain
                  :loading="expanding"
                  @click="continueHelperExpansion"
                >
                  Continue expanding
                </el-button>
              </div>
            </div>
          </template>

          <div v-else class="inspector-card inspector-card--details inspector-card--empty">
            <div class="inspector-kicker">Details</div>
            <div class="inspector-empty">
              Select a node or edge to inspect details.
            </div>
          </div>
        </aside>
      </div>

      <div class="table-head">
        <div>
          <div class="table-title">Graph visible links</div>
          <div class="table-subtitle">{{ visibleLinksSummary }}</div>
        </div>
        <el-button class="soft-button" :disabled="visibleLinks.length === 0" @click="downloadCsv">
          <el-icon><Download /></el-icon>
          <span>CSV</span>
        </el-button>
      </div>

      <el-table
        v-if="visibleLinks.length > 0"
        :data="pagedLinks"
        size="small"
        stripe
        border
        class="detail-table"
        max-height="260"
        :row-class-name="getLinkRowClassName"
      >
        <el-table-column prop="peak" label="Peak" min-width="190">
          <template #default="{ row }">
            <el-popover
              trigger="click"
              placement="right-start"
              :width="380"
              popper-class="annotation-detail-popper network-peak-detail-popper"
            >
              <template #reference>
                <button type="button" class="network-peak-link">{{ displayText(row.peak) }}</button>
              </template>
              <div class="network-peak-card">
                <div class="network-peak-card-head">
                  <div class="network-peak-card-head-row">
                    <div class="network-peak-kicker">Peak detail</div>
                    <button
                      type="button"
                      class="network-peak-detail-button"
                      :disabled="!canViewPeakDetail(row)"
                      @click="goToPeakDetail(row)"
                    >
                      View peak detail
                    </button>
                </div>
                <div class="network-peak-title">{{ displayText(row.peak) }}</div>
                <div class="network-peak-pills">
                    <span class="network-peak-pill">{{ peakDatasetLabel(row) }} / {{ peakDomainLabel(row) }}</span>
                  </div>
                </div>
                <div class="network-peak-card-body">
                  <div class="network-peak-grid">
                    <div><span>Feature type</span><strong>Peak / Regulatory region</strong></div>
                    <div><span>Peak ID</span><strong>{{ displayText(row.peakId) }}</strong></div>
                    <div class="network-peak-grid-wide"><span>Location</span><strong>{{ displayText(row.peak) }}</strong></div>
                    <div><span>Linked gene</span><strong>{{ displayText(row.linkedGene) }}</strong></div>
                  </div>
                  <div class="network-peak-metrics">
                    <div><span>Link score</span><strong>{{ formatNumber(row.linkScore ?? row.score) }}</strong></div>
                    <div><span>Correlation</span><strong>{{ formatNumber(row.correlation) }}</strong></div>
                    <div><span>FDR</span><strong>{{ formatNumber(row.fdr) }}</strong></div>
                  </div>
                </div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="linkedGene" label="Linked gene" min-width="130">
          <template #default="{ row }">{{ displayText(row.linkedGene) }}</template>
        </el-table-column>
        <el-table-column v-if="hasDistanceValues" label="Distance to TSS" min-width="140" align="center">
          <template #default="{ row }">{{ formatNumber(row.distanceToTss) }}</template>
        </el-table-column>
        <el-table-column label="Link score" min-width="110" align="center">
          <template #default="{ row }">{{ formatNumber(row.linkScore ?? row.score) }}</template>
        </el-table-column>
        <el-table-column label="Correlation" min-width="110" align="center">
          <template #default="{ row }">{{ formatNumber(row.correlation) }}</template>
        </el-table-column>
        <el-table-column v-if="hasFdrValues" label="FDR" min-width="100" align="center">
          <template #default="{ row }">{{ formatNumber(row.fdr) }}</template>
        </el-table-column>
        <el-table-column v-if="hasVarQValues" label="VarQ ATAC" min-width="110" align="center">
          <template #default="{ row }">{{ formatNumber(row.varQAtac) }}</template>
        </el-table-column>
        <el-table-column v-if="hasVarQValues" label="VarQ RNA" min-width="110" align="center">
          <template #default="{ row }">{{ formatNumber(row.varQRna) }}</template>
        </el-table-column>
        <el-table-column v-if="hasVisibleSourceValues" prop="source" label="Source" min-width="160">
          <template #default="{ row }">{{ displayText(getVisibleSource(row.source)) }}</template>
        </el-table-column>
        <el-table-column label="Action" min-width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain class="view-button" @click="openLinkDetail(row)">View</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="empty-state">
        {{ linksEmptyMessage }}
      </div>

      <div v-if="visibleLinks.length > 0" class="pager">
        <el-pagination
          class="oscar-pagination"
          background
          layout="total, sizes, prev, pager, next, jumper"
          popper-class="oscar-select-popper"
          :total="visibleLinks.length"
          :page-sizes="linkPageSizeOptions"
          :page-size="linkPageSize"
          :current-page="linkPage"
          @size-change="onLinkPageSizeChange"
          @current-change="onLinkPageChange"
        />
      </div>
    </template>

    <el-dialog
      v-model="fullLinksDialogVisible"
      class="full-links-dialog"
      :title="fullLinksDialogTitle"
      width="920px"
      destroy-on-close
      @open="loadFullLinks"
    >
      <div class="full-links-toolbar">
        <div class="full-links-subtitle">{{ fullLinksDialogSubtitle }}</div>
        <el-button class="soft-button" size="small" @click="downloadFullLinksAllCsv">
          <el-icon><Download /></el-icon>
          <span>Download CSV</span>
        </el-button>
      </div>

      <div v-if="fullLinksError" class="state-message state-message--error full-links-error">
        {{ fullLinksError }}
      </div>

      <el-table
        v-else
        v-loading="fullLinksLoading"
        :data="fullLinksItems"
        size="small"
        stripe
        border
        class="detail-table full-links-table"
        empty-text="No links found."
      >
        <el-table-column prop="peak" label="Peak" min-width="280">
          <template #default="{ row }">{{ displayText(row.peak) }}</template>
        </el-table-column>
        <el-table-column prop="linkedGene" label="Linked gene" min-width="160">
          <template #default="{ row }">{{ displayText(row.linkedGene) }}</template>
        </el-table-column>
        <el-table-column label="Link score" min-width="120" align="center">
          <template #default="{ row }">{{ formatNumber(row.linkScore ?? row.score) }}</template>
        </el-table-column>
        <el-table-column label="Correlation" min-width="120" align="center">
          <template #default="{ row }">{{ formatNumber(row.correlation) }}</template>
        </el-table-column>
        <el-table-column v-if="hasFullLinksFdrValues" label="FDR" min-width="110" align="center">
          <template #default="{ row }">{{ formatNumber(row.fdr) }}</template>
        </el-table-column>
        <el-table-column v-if="hasFullLinksVarQValues" label="VarQ ATAC" min-width="120" align="center">
          <template #default="{ row }">{{ formatNumber(row.varQAtac) }}</template>
        </el-table-column>
        <el-table-column v-if="hasFullLinksVarQValues" label="VarQ RNA" min-width="120" align="center">
          <template #default="{ row }">{{ formatNumber(row.varQRna) }}</template>
        </el-table-column>
        <el-table-column v-if="hasFullLinksDatasetValues" prop="datasetId" label="Dataset" min-width="160">
          <template #default="{ row }">{{ displayText(row.datasetId) }}</template>
        </el-table-column>
        <el-table-column v-if="hasFullLinksSampleValues" prop="sampleName" label="Sample" min-width="180">
          <template #default="{ row }">{{ displayText(row.sampleName) }}</template>
        </el-table-column>
      </el-table>

      <div class="pager full-links-pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="fullLinksTotal"
          :page-sizes="fullLinksPageSizeOptions"
          :page-size="fullLinksPageSize"
          :current-page="fullLinksPage"
          @size-change="onFullLinksPageSizeChange"
          @current-change="onFullLinksPageChange"
        />
      </div>
    </el-dialog>
  </section>

</template>

<script setup lang="ts">
import { Download, Refresh, Search } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  RegulatoryNetworkExpansionResponse,
  RegulatoryNetworkLink,
  RegulatoryNetworkMode,
  RegulatoryNetworkNodeType,
  RegulatoryNetworkResponse,
  SearchResultDomain,
} from "@/api/searchResult";
import {
  fetchRegulatoryNetwork,
  fetchRegulatoryNetworkExpansion,
  fetchRegulatoryNetworkLinks,
  isSearchResultEndpointUnavailable,
} from "@/api/searchResult";
import RegulatoryNetworkSvg from "@/components/search-result/RegulatoryNetworkSvg.vue";
import { domainDisplayLabel } from "@/utils/searchResultDomain";
import type {
  GraphDimensions,
  GraphEdge,
  GraphLink,
  GraphNode,
  GraphPoint,
  RegulatoryGraph,
} from "@/utils/regulatoryNetworkGraph";
import {
  adaptRegulatoryResponse,
  applyInteractionState,
  createAnchorGraph,
  emptyRegulatoryGraph,
  getGeneNodeId,
  getPeakGeneLinksForGene,
  getPeakGeneLinksForPeak,
  getPeakNodeId,
  getVisiblePeakGeneLinks,
  graphEdgeId,
  layoutFocusGraph,
  layoutInitialGraph,
  layoutProgressiveGraph,
  mergeRegulatoryGraphs,
  normalizeNodeType,
  selectDirectNeighborhoodSubgraph,
  selectInitialSubgraph,
} from "@/utils/regulatoryNetworkGraph";

type InspectorActionName = "focusNode" | "expandNeighbors";
type InspectorKind = "gene" | "peak" | "edge";
type QueryIntent = "default" | "gene" | "peak";
type ExpandReason = "auto" | "manual";
type PeakLabelMode = "overview" | "active" | "hidden";

type InspectorAction = {
  label: string;
  action: InspectorActionName;
  variant: "primary" | "secondary";
  disabled?: boolean;
  title?: string;
};

type InspectorDetail = {
  kind: InspectorKind;
  chip: string;
  title: string;
  subtitle?: string;
  items: Array<{ label: string; value: string }>;
  actions?: InspectorAction[];
};
type InspectorDetailItem = InspectorDetail["items"][number];
type TopListValue = string[] | string | undefined;
type DatasetSummary = {
  datasetId?: string;
  sampleName?: string;
  domain?: string;
};
type FullLinksAction = {
  label: string;
  total: number;
};

const props = withDefaults(defineProps<{
  datasetId: string;
  domain: SearchResultDomain;
}>(), {
});
const route = useRoute();
const router = useRouter();

const DEFAULT_GRAPH_DIMENSIONS: GraphDimensions = {
  width: 960,
  height: 520,
};

const GENE_INITIAL_LINK_LIMIT = 5;
const LOCAL_NODE_LINK_LIMIT = 30;
const MAX_VISIBLE_GRAPH_NODES = 50;
const NODE_RELATED_STEP = 5;
const AUTO_RELATED_STEP = 3;
const NEW_ITEM_FLAG_MS = 820;
const NO_ADDITIONAL_RELATED_MESSAGE = "No additional related nodes available.";
const LOCAL_LINK_LIMIT_MESSAGE = "Graph view shows top 30 links. Use View all links to inspect the complete list.";
const NODE_DISPLAY_LIMIT_MESSAGE = "Node display limit reached. Please focus on the node of interest.";

const modeOptions: Array<{ value: RegulatoryNetworkMode; label: string }> = [
  { value: "gene", label: "Gene" },
  { value: "peak", label: "Peak" },
];

const sectionEl = ref<HTMLElement | null>(null);
const queryMode = ref<RegulatoryNetworkMode>("gene");
const geneQuery = ref("");
const geneMinScore = ref("");
const peakQuery = ref("");
const linkedGeneQuery = ref("");
const peakMinScore = ref("");
const geneQueryError = ref("");
const peakQueryError = ref("");
const linkedGeneQueryError = ref("");
type QueryInputErrorField = "gene" | "peak" | "linkedGene";
const queryInputErrorTimers: Record<QueryInputErrorField, number | undefined> = {
  gene: undefined,
  peak: undefined,
  linkedGene: undefined,
};
const sourceGraph = ref<RegulatoryGraph>(emptyRegulatoryGraph());
const visibleGraph = ref<RegulatoryGraph>(emptyRegulatoryGraph());
const graphDimensions = ref<GraphDimensions>({ ...DEFAULT_GRAPH_DIMENSIONS });
const graphHostEl = ref<HTMLElement | null>(null);
const loading = ref(false);
const error = ref(false);
const loaded = ref(false);
const selectedNodeId = ref("");
const selectedEdgeId = ref("");
const hoveredNodeId = ref("");
const hoveredEdgeId = ref("");
const focusedNodeId = ref("");
const peakLabelMode = ref<PeakLabelMode>("overview");
const inspectorHelper = ref("");
const helperContinueNodeId = ref("");
const interactionEpoch = ref(0);
const expanding = ref(false);
const focusing = ref(false);
const exhaustedNodeIds = ref<Set<string>>(new Set());
const autoExpandedNodeIds = ref<Set<string>>(new Set());
const newlyAddedNodeIds = ref<Set<string>>(new Set());
const newlyAddedEdgeIds = ref<Set<string>>(new Set());
const enteringNodeOrigins = ref<Record<string, GraphPoint>>({});
const viewportPan = ref<GraphPoint>({ x: 0, y: 0 });
const linkPage = ref(1);
const linkPageSize = ref(20);
const fullLinksDialogVisible = ref(false);
const fullLinksLoading = ref(false);
const fullLinksPage = ref(1);
const fullLinksPageSize = ref(20);
const fullLinksTotal = ref(0);
const fullLinksItems = ref<GraphLink[]>([]);
const fullLinksError = ref("");

let requestToken = 0;
let fullLinksRequestToken = 0;
let newItemTimer: number | undefined;
let graphResizeObserver: ResizeObserver | null = null;

const linkPageSizeOptions = [10, 20, 50];
const fullLinksPageSizeOptions = [20, 50, 100];
const domainChip = computed(() => domainDisplayLabel(props.domain));
const domainTitle = computed(() => domainChip.value);
const hasGraphData = computed(() => visibleGraph.value.nodes.length > 0);
const nodeById = computed(() => new Map(visibleGraph.value.nodes.map((node) => [node.id, node] as const)));
const edgeById = computed(() => new Map(visibleGraph.value.edges.map((edge) => [edge.id, edge] as const)));
const sourceNodeById = computed(() => new Map(sourceGraph.value.nodes.map((node) => [node.id, node] as const)));
const selectedNode = computed(() => nodeById.value.get(selectedNodeId.value) ?? null);
const selectedEdge = computed(() => edgeById.value.get(selectedEdgeId.value) ?? null);
const renderGraph = computed(() => {
  return applyInteractionState(
    visibleGraph.value,
    selectedNodeId.value,
    selectedEdgeId.value,
    hoveredNodeId.value,
    newlyAddedNodeIds.value,
    newlyAddedEdgeIds.value,
    hoveredEdgeId.value
  );
});
const visibleLinks = computed(() => {
  return getVisiblePeakGeneLinks(visibleGraph.value)
    .slice()
    .sort((a, b) => getLinkRelevance(b) - getLinkRelevance(a) || getLinkScore(b) - getLinkScore(a));
});
const visibleLinksSummary = computed(() => {
  return "Links currently visible in the network graph";
});
const pagedLinks = computed(() => {
  const start = (linkPage.value - 1) * linkPageSize.value;
  return visibleLinks.value.slice(start, start + linkPageSize.value);
});
const hasDistanceValues = computed(() => visibleLinks.value.some((link) => isFiniteNumber(link.distanceToTss)));
const hasFdrValues = computed(() => visibleLinks.value.some((link) => isFiniteNumber(link.fdr)));
const hasVarQValues = computed(() => visibleLinks.value.some((link) => isFiniteNumber(link.varQAtac) || isFiniteNumber(link.varQRna)));
const hasVisibleSourceValues = computed(() => visibleLinks.value.some((link) => hasDisplayValue(getVisibleSource(link.source))));
const linksEmptyMessage = computed(() => {
  if (!hasGraphData.value) return "No regulatory links matched the current query.";
  return "No visible peak-to-gene links are currently shown in the graph.";
});
const graphMessage = computed(() => {
  return "No regulatory network data is available for this sample yet.";
});
const actionNode = computed(() => {
  if (selectedNode.value) return selectedNode.value;
  if (!selectedEdge.value) return null;
  return getPreferredNodeForEdgeAction(selectedEdge.value);
});
const selectedDetail = computed<InspectorDetail | null>(() => {
  if (selectedEdge.value) return getEdgeDetail(selectedEdge.value);
  if (selectedNode.value) return getNodeDetail(selectedNode.value);
  return null;
});
const selectedDetailSubtitle = computed(() => {
  const detail = selectedDetail.value;
  if (!detail?.subtitle) return "";
  return hasDistinctSubtitle(detail.title, detail.subtitle) ? detail.subtitle : "";
});

function hashId(id: string): number {
  let h = 0;
  for (let i = 0; i < id.length; i++) {
    h = ((h << 5) - h + id.charCodeAt(i)) | 0;
  }
  return Math.abs(h);
}
const GENE_COLORS = ["#B11D56", "#11A3B8"] as const;
const PEAK_COLORS = ["#F59ABB", "#8FD9E5"] as const;

const inspectorChipStyle = computed(() => {
  const node = selectedNode.value;
  if (!node || (node.type !== "gene" && node.type !== "peak")) return {} as Record<string, string>;
  const colors: readonly string[] = node.type === "gene" ? GENE_COLORS : PEAK_COLORS;
  const hex = colors[hashId(node.id) % colors.length]!;
  return {
    backgroundColor: `color-mix(in srgb, ${hex} 28%, #ffffff)`,
    borderColor: hex,
    color: `color-mix(in srgb, ${hex} 52%, #243238)`,
    boxShadow: `inset 0 1px 0 rgba(255, 255, 255, 0.72), 0 2px 7px ${hex}38`,
  };
});
const helperContinueNode = computed(() => {
  return helperContinueNodeId.value ? nodeById.value.get(helperContinueNodeId.value) ?? null : null;
});
const helperContinueAvailable = computed(() => {
  const node = helperContinueNode.value;
  return Boolean(node && !isNodeExpansionDisabled(node));
});
const fullLinksAction = computed<FullLinksAction | null>(() => {
  const node = selectedNode.value;
  if (!node || !supportsFullLinksQuery(node)) return null;
  const total = getNodeFullLinksTotal(node);
  return {
    label: getFullLinksButtonLabel(node),
    total: isFiniteNumber(total) ? total : 0,
  };
});
const fullLinksDialogTitle = computed(() => {
  const node = selectedNode.value;
  if (!node) return "All links";
  const label = node.label;
  if (node.type === "gene") return `All peak links for ${label}`;
  if (node.type === "peak") return `All gene links for ${label}`;
  return `All links for ${label}`;
});
const fullLinksDialogSubtitle = computed(() => {
  const total = (fullLinksTotal.value || fullLinksAction.value?.total || 0).toLocaleString();
  return `${total} total links`;
});
const hasFullLinksFdrValues = computed(() => fullLinksItems.value.some((link) => isFiniteNumber(link.fdr)));
const hasFullLinksVarQValues = computed(() => fullLinksItems.value.some((link) => isFiniteNumber(link.varQAtac) || isFiniteNumber(link.varQRna)));
const hasFullLinksDatasetValues = computed(() => fullLinksItems.value.some((link) => hasDisplayValue(link.datasetId)));
const hasFullLinksSampleValues = computed(() => fullLinksItems.value.some((link) => hasDisplayValue(link.sampleName)));

watch(() => [props.datasetId, props.domain] as const, () => {
  resetNetwork();
}, { immediate: true });

watch(visibleLinks, () => {
  const maxPage = Math.max(1, Math.ceil(visibleLinks.value.length / linkPageSize.value));
  if (linkPage.value > maxPage) linkPage.value = maxPage;
});

watch(graphDimensions, () => {
  relayoutVisibleGraph();
});

watch(selectedNodeId, () => {
  if (!fullLinksDialogVisible.value) return;
  if (!selectedNode.value || !fullLinksAction.value) {
    fullLinksDialogVisible.value = false;
    return;
  }
  fullLinksPage.value = 1;
  void loadFullLinks();
});

onMounted(() => {
  updateGraphDimensions();
  if (!graphHostEl.value) return;

  graphResizeObserver = new ResizeObserver(() => {
    updateGraphDimensions();
  });
  graphResizeObserver.observe(graphHostEl.value);
});

onBeforeUnmount(() => {
  window.clearTimeout(newItemTimer);
  clearAllQueryInputErrors();
  graphResizeObserver?.disconnect();
  graphResizeObserver = null;
});

async function resetNetwork() {
  clearAllQueryInputErrors();
  queryMode.value = "gene";
  geneQuery.value = "";
  geneMinScore.value = "";
  peakQuery.value = "";
  linkedGeneQuery.value = "";
  peakMinScore.value = "";
  clearSelection();
  exhaustedNodeIds.value = new Set();
  autoExpandedNodeIds.value = new Set();
  focusedNodeId.value = "";
  await loadNetwork("default");
}

const GENE_SYMBOL_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/;
const PEAK_REGION_PATTERN = /^chr[A-Za-z0-9_.-]+:(\d+)-(\d+)$/i;

function queryInputErrorRef(field: QueryInputErrorField) {
  if (field === "gene") return geneQueryError;
  if (field === "peak") return peakQueryError;
  return linkedGeneQueryError;
}

function showQueryInputError(field: QueryInputErrorField, message: string) {
  clearQueryInputError(field);
  queryInputErrorRef(field).value = message;
  queryInputErrorTimers[field] = window.setTimeout(() => {
    queryInputErrorRef(field).value = "";
    queryInputErrorTimers[field] = undefined;
  }, 3000);
}

function clearQueryInputError(field: QueryInputErrorField) {
  const timer = queryInputErrorTimers[field];
  if (timer !== undefined) window.clearTimeout(timer);
  queryInputErrorTimers[field] = undefined;
  queryInputErrorRef(field).value = "";
}

function clearAllQueryInputErrors() {
  clearQueryInputError("gene");
  clearQueryInputError("peak");
  clearQueryInputError("linkedGene");
}

async function searchNetwork() {
  const gene = queryMode.value === "gene" ? geneQuery.value.trim() : linkedGeneQuery.value.trim();
  const peak = queryMode.value === "peak" ? peakQuery.value.trim() : "";
  if (gene && !GENE_SYMBOL_PATTERN.test(gene)) {
    showQueryInputError(queryMode.value === "gene" ? "gene" : "linkedGene", "Enter a single valid gene symbol.");
    return;
  }
  if (peak && !isValidPeakRegion(peak)) {
    showQueryInputError("peak", "Enter a single peak region in chr:start-end format.");
    return;
  }

  clearAllQueryInputErrors();

  clearSelection();
  exhaustedNodeIds.value = new Set();
  autoExpandedNodeIds.value = new Set();
  focusedNodeId.value = "";

  const intent: QueryIntent = queryMode.value === "gene" && geneQuery.value.trim()
    ? "gene"
    : queryMode.value === "peak" && (peakQuery.value.trim() || linkedGeneQuery.value.trim())
      ? "peak"
      : "default";
  await loadNetwork(intent);
}

function isValidPeakRegion(value: string): boolean {
  const match = value.match(PEAK_REGION_PATTERN);
  if (!match) return false;
  const start = Number(match[1]);
  const end = Number(match[2]);
  return Number.isSafeInteger(start) && Number.isSafeInteger(end) && start >= 0 && end > start;
}

async function loadNetwork(intent: QueryIntent) {
  const normalizedDatasetId = props.datasetId.trim();
  if (!normalizedDatasetId) {
    sourceGraph.value = emptyRegulatoryGraph();
    visibleGraph.value = emptyRegulatoryGraph();
    loaded.value = true;
    return;
  }

  const token = ++requestToken;
  loading.value = true;
  error.value = false;
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
  clearNewItemState();

  try {
    const response = await requestNetwork(intent);
    if (token !== requestToken) return;

    const adapted = adaptRegulatoryResponse(response);
    sourceGraph.value = adapted;
    const nextGraph = createGraphForIntent(sourceGraph.value, intent);
    visibleGraph.value = nextGraph;
    viewportPan.value = { x: 0, y: 0 };
    loaded.value = true;
    linkPage.value = 1;
    peakLabelMode.value = intent === "default" ? "overview" : "active";

    const anchor = getIntentAnchorNode(intent);
    focusedNodeId.value = intent === "default" ? "" : anchor?.id ?? "";
    if (anchor) {
      selectedNodeId.value = anchor.id;
      selectedEdgeId.value = "";
    }
  } catch (loadError) {
    if (token !== requestToken) return;
    if (isSearchResultEndpointUnavailable(loadError)) {
      sourceGraph.value = emptyRegulatoryGraph();
      visibleGraph.value = emptyRegulatoryGraph();
      loaded.value = true;
    } else {
      console.error("[SearchResult] Failed to load regulatory network:", loadError);
      error.value = true;
    }
  } finally {
    if (token === requestToken) loading.value = false;
  }
}

async function requestNetwork(intent: QueryIntent): Promise<RegulatoryNetworkResponse> {
  const params = currentQueryParams();
  const mode = intent === "peak" ? "peak" : "gene";
  const gene = intent === "gene" ? geneQuery.value.trim() : linkedGeneQuery.value.trim();
  const peak = intent === "peak" ? peakQuery.value.trim() : "";

  return fetchRegulatoryNetwork({
    datasetId: props.datasetId,
    domain: props.domain,
    mode,
    gene,
    peak,
    minScore: params.minScore,
    maxNodes: 80,
    maxEdges: 120,
  });
}

function createGraphForIntent(adapted: RegulatoryGraph, intent: QueryIntent): RegulatoryGraph {
  if (!adapted.nodes.length) {
    const anchor = getQueryAnchorDefinition(intent);
    return anchor ? createAnchorGraph(anchor) : emptyRegulatoryGraph();
  }

  if (intent === "default") {
    return layoutInitialGraph(selectInitialSubgraph(adapted), graphDimensions.value);
  }

  const anchor = getIntentAnchorNodeFromGraph(adapted, intent);
  return anchor
    ? layoutFocusGraph(adapted, anchor.id, graphDimensions.value)
    : layoutInitialGraph(adapted, graphDimensions.value);
}

function getIntentAnchorNode(intent: QueryIntent): GraphNode | null {
  const anchor = getQueryAnchorDefinition(intent);
  if (!anchor) return null;
  return nodeById.value.get(anchor.id) ?? null;
}

function getIntentAnchorNodeFromGraph(sourceGraph: RegulatoryGraph, intent: QueryIntent): GraphNode | null {
  const anchor = getQueryAnchorDefinition(intent);
  if (!anchor) return null;
  const targetId = anchor.id.toLowerCase();
  return sourceGraph.nodes.find((node) => node.id.toLowerCase() === targetId) ?? null;
}

function getQueryAnchorDefinition(intent: QueryIntent): Pick<GraphNode, "id" | "type" | "label"> | null {
  if (intent === "gene") {
    const gene = geneQuery.value.trim();
    return gene ? { id: getGeneNodeId(gene), type: "gene", label: gene } : null;
  }

  if (intent === "peak") {
    const peak = peakQuery.value.trim();
    if (peak) return { id: getPeakNodeId(peak), type: "peak", label: peak };
    const linkedGene = linkedGeneQuery.value.trim();
    return linkedGene ? { id: getGeneNodeId(linkedGene), type: "gene", label: linkedGene } : null;
  }

  return null;
}

function setQueryMode(nextMode: RegulatoryNetworkMode) {
  interactionEpoch.value += 1;
  clearAllQueryInputErrors();
  queryMode.value = nextMode;
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
}

function selectNode(nodeId: string) {
  interactionEpoch.value += 1;
  const epoch = interactionEpoch.value;
  selectedNodeId.value = nodeId;
  selectedEdgeId.value = "";
  peakLabelMode.value = "active";
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
  if (!autoExpandedNodeIds.value.has(nodeId)) {
    autoExpandedNodeIds.value = new Set([...autoExpandedNodeIds.value, nodeId]);
    void expandNode(nodeId, "auto", requestToken, epoch);
  }
}

function selectEdge(edgeId: string) {
  interactionEpoch.value += 1;
  selectedNodeId.value = "";
  selectedEdgeId.value = edgeId;
  peakLabelMode.value = "active";
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
}

function clearSelection() {
  interactionEpoch.value += 1;
  selectedNodeId.value = "";
  selectedEdgeId.value = "";
  hoveredNodeId.value = "";
  hoveredEdgeId.value = "";
  peakLabelMode.value = "hidden";
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
}

function resetGraphView() {
  interactionEpoch.value += 1;
  selectedNodeId.value = "";
  selectedEdgeId.value = "";
  hoveredNodeId.value = "";
  hoveredEdgeId.value = "";
  focusedNodeId.value = "";
  peakLabelMode.value = "overview";
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
  exhaustedNodeIds.value = new Set();
  autoExpandedNodeIds.value = new Set();
  clearNewItemState();
  viewportPan.value = { x: 0, y: 0 };
  visibleGraph.value = sourceGraph.value.nodes.length
    ? layoutInitialGraph(selectInitialSubgraph(sourceGraph.value), graphDimensions.value)
    : emptyRegulatoryGraph();
  linkPage.value = 1;
}

function setHoveredNode(nodeId: string) {
  hoveredNodeId.value = nodeId;
}

function setHoveredEdge(edgeId: string) {
  hoveredEdgeId.value = edgeId;
}

function actionLoading(action: InspectorActionName): boolean {
  return action === "focusNode" ? focusing.value : expanding.value;
}

function runInspectorAction(action: InspectorActionName) {
  const node = actionNode.value;
  if (!node) return;

  if (action === "focusNode") {
    focusOnNode(node.id);
    return;
  }

  expandNode(node.id, "manual");
}

async function focusOnNode(nodeId: string) {
  const node = nodeById.value.get(nodeId);
  if (!node || focusing.value) return;

  interactionEpoch.value += 1;
  const epoch = interactionEpoch.value;
  const token = ++requestToken;
  focusing.value = true;
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
  clearNodeExhausted(node.id);
  clearNewItemState();

  if (focusedNodeId.value === node.id) {
    visibleGraph.value = layoutFocusGraph(getSourceGraphForNode(node.id), node.id, graphDimensions.value);
    selectedNodeId.value = node.id;
    selectedEdgeId.value = "";
    focusedNodeId.value = node.id;
    peakLabelMode.value = "active";
    viewportPan.value = { x: 0, y: 0 };
    linkPage.value = 1;
    focusing.value = false;
    return;
  }

  try {
    const sourceNode = sourceNodeById.value.get(node.id) ?? node;
    const addition = await requestExpansion(sourceNode, getFocusNeighborCap(sourceNode));
    if (token !== requestToken) return;
    if (epoch !== interactionEpoch.value) return;
    const adapted = adaptRegulatoryResponse(addition);
    mergeIntoSourceGraph(adapted);
    visibleGraph.value = layoutFocusGraph(getSourceGraphForNode(node.id), node.id, graphDimensions.value);
    selectedNodeId.value = node.id;
    selectedEdgeId.value = "";
    focusedNodeId.value = node.id;
    peakLabelMode.value = "active";
    viewportPan.value = { x: 0, y: 0 };
    linkPage.value = 1;
  } catch (focusError) {
    if (token !== requestToken) return;
    if (epoch !== interactionEpoch.value) return;
    if (isSearchResultEndpointUnavailable(focusError)) {
      visibleGraph.value = layoutFocusGraph(getSourceGraphForNode(node.id), node.id, graphDimensions.value);
      selectedNodeId.value = node.id;
      selectedEdgeId.value = "";
      focusedNodeId.value = node.id;
      peakLabelMode.value = "active";
      viewportPan.value = { x: 0, y: 0 };
    } else {
      console.error("[SearchResult] Failed to focus regulatory node:", focusError);
      inspectorHelper.value = "Focus failed. Please retry.";
      helperContinueNodeId.value = "";
    }
  } finally {
    if (token === requestToken) focusing.value = false;
  }
}

async function expandNode(
  nodeId: string,
  reason: ExpandReason,
  token = requestToken,
  epoch = interactionEpoch.value
) {
  const node = nodeById.value.get(nodeId);
  if (!node || expanding.value) return;

  const maxNeighbors = getExpansionNeighborCap(node, reason);
  if (maxNeighbors <= 0) {
    if (reason === "manual") markNodeExhausted(node);
    return;
  }

  if (reason === "manual") {
    inspectorHelper.value = "";
    helperContinueNodeId.value = "";
  }
  expanding.value = true;

  try {
    const previousGraph = visibleGraph.value;
    let result = mergeRegulatoryGraphs(
      previousGraph,
      selectDirectNeighborhoodSubgraph(getSourceGraphForNode(node.id), node.id, maxNeighbors)
    );

    if (!hasMergeChanges(result)) {
      const sourceNode = sourceNodeById.value.get(node.id) ?? node;
      const addition = await requestExpansion(sourceNode, maxNeighbors);
      if (token !== requestToken) return;
      if (isStaleAutoExpansion(reason, node.id, epoch)) return;
      mergeIntoSourceGraph(adaptRegulatoryResponse(addition));
      result = mergeRegulatoryGraphs(
        previousGraph,
        selectDirectNeighborhoodSubgraph(getSourceGraphForNode(node.id), node.id, maxNeighbors)
      );
    } else if (token !== requestToken) {
      return;
    }

    if (isStaleAutoExpansion(reason, node.id, epoch)) return;
    if (result.newNodeIds.size && previousGraph.nodes.length + result.newNodeIds.size > MAX_VISIBLE_GRAPH_NODES) {
      ElMessage.warning(NODE_DISPLAY_LIMIT_MESSAGE);
      return;
    }

    const nextGraph = result.newNodeIds.size
      ? layoutProgressiveGraph(result.graph, previousGraph, node.id, result.newNodeIds, graphDimensions.value)
      : result.graph;

    visibleGraph.value = nextGraph;
    const shouldWriteSelection = reason === "auto"
      ? selectedNodeId.value === node.id
      : Boolean(selectedNodeId.value || selectedEdgeId.value);
    if (shouldWriteSelection) {
      selectedNodeId.value = node.id;
      selectedEdgeId.value = "";
      peakLabelMode.value = "active";
    }
    linkPage.value = 1;

    if (result.newNodeIds.size || result.newEdgeIds.size || result.newLinkIds.size) {
      startNewItemAnimation(node.id, result.newNodeIds, result.newEdgeIds);
      clearNodeExhausted(node.id);
      if (reason === "manual" && shouldWriteSelection) {
        const nextNode = nodeById.value.get(node.id) ?? node;
        if (isNodeExhausted(nextNode)) {
          markNodeExhausted(nextNode);
        } else {
          const count = result.newNodeIds.size + result.newEdgeIds.size;
          inspectorHelper.value = `${count.toLocaleString()} related items added.`;
          helperContinueNodeId.value = node.id;
        }
      }
    } else if (reason === "manual" && shouldWriteSelection) {
      const nextNode = nodeById.value.get(node.id) ?? node;
      if (isNodeExhausted(nextNode)) {
        markNodeExhausted(nextNode);
      } else {
        inspectorHelper.value = "No new related nodes returned.";
        helperContinueNodeId.value = node.id;
      }
    }
  } catch (expandError) {
    if (isSearchResultEndpointUnavailable(expandError)) {
      if (reason === "manual" && Boolean(selectedNodeId.value || selectedEdgeId.value)) markNodeExhausted(node);
    } else {
      console.error("[SearchResult] Failed to expand regulatory network:", expandError);
      inspectorHelper.value = "Neighbor expansion failed. Please retry.";
      helperContinueNodeId.value = "";
    }
  } finally {
    expanding.value = false;
  }
}

async function requestExpansion(node: GraphNode, maxNeighbors: number): Promise<RegulatoryNetworkExpansionResponse> {
  const params = currentQueryParams();
  const nodeType = getExpansionNodeType(node);

  return fetchRegulatoryNetworkExpansion({
    datasetId: props.datasetId,
    domain: props.domain,
    nodeId: node.id,
    nodeType,
    gene: params.gene,
    peak: params.peak,
    minScore: params.minScore,
    maxNeighbors,
  });
}

function mergeIntoSourceGraph(addition: RegulatoryGraph) {
  if (!addition.nodes.length && !addition.edges.length && !addition.links.length) return;
  if (!sourceGraph.value.nodes.length && !sourceGraph.value.edges.length && !sourceGraph.value.links.length) {
    sourceGraph.value = addition;
    return;
  }

  sourceGraph.value = mergeRegulatoryGraphs(sourceGraph.value, addition).graph;
}

function getSourceGraphForNode(nodeId: string): RegulatoryGraph {
  return sourceNodeById.value.has(nodeId) ? sourceGraph.value : visibleGraph.value;
}

function hasMergeChanges(result: ReturnType<typeof mergeRegulatoryGraphs>): boolean {
  return Boolean(result.newNodeIds.size || result.newEdgeIds.size || result.newLinkIds.size);
}

function isStaleAutoExpansion(reason: ExpandReason, nodeId: string, epoch: number): boolean {
  return reason === "auto" && (epoch !== interactionEpoch.value || selectedNodeId.value !== nodeId);
}

function updateGraphDimensions() {
  const width = graphHostEl.value?.getBoundingClientRect().width ?? DEFAULT_GRAPH_DIMENSIONS.width;
  const nextWidth = Math.max(360, Math.round(width));
  const nextHeight = window.matchMedia("(max-width: 760px)").matches ? 480 : DEFAULT_GRAPH_DIMENSIONS.height;

  if (Math.abs(nextWidth - graphDimensions.value.width) <= 1 && nextHeight === graphDimensions.value.height) return;
  graphDimensions.value = {
    width: nextWidth,
    height: nextHeight,
  };
}

function relayoutVisibleGraph() {
  if (!visibleGraph.value.nodes.length) return;

  if (focusedNodeId.value) {
    visibleGraph.value = layoutFocusGraph(getSourceGraphForNode(focusedNodeId.value), focusedNodeId.value, graphDimensions.value);
    return;
  }

  visibleGraph.value = layoutInitialGraph(visibleGraph.value, graphDimensions.value);
}

function getExpansionNodeType(node: GraphNode): RegulatoryNetworkNodeType {
  return normalizeNodeType(node.type);
}

function getFocusNeighborCap(node: GraphNode): number {
  if (node.type === "gene") return GENE_INITIAL_LINK_LIMIT;
  return NODE_RELATED_STEP;
}

function getExpansionNeighborCap(node: GraphNode, reason: ExpandReason): number {
  if (node.type === "gene") {
    const currentPeaks = getVisibleDirectPeakCount(node.id);
    const maxPeaks = getGeneExpansionNeighborLimit(node);
    if (currentPeaks >= maxPeaks) return 0;
    if (reason === "auto") return Math.min(maxPeaks, Math.max(GENE_INITIAL_LINK_LIMIT, currentPeaks + AUTO_RELATED_STEP));
    return Math.min(maxPeaks, currentPeaks + NODE_RELATED_STEP);
  }

  if (node.type === "peak") {
    const currentGenes = getVisibleDirectGeneCount(node.id);
    const maxGenes = getPeakExpansionNeighborLimit(node);
    if (currentGenes >= maxGenes) return 0;
    if (reason === "auto") return Math.min(maxGenes, Math.max(AUTO_RELATED_STEP, currentGenes + AUTO_RELATED_STEP));
    return Math.min(maxGenes, currentGenes + NODE_RELATED_STEP);
  }

  return NODE_RELATED_STEP;
}

function getVisibleDirectPeakCount(geneId: string): number {
  return getPeakGeneLinksForGene(visibleGraph.value, geneId).length;
}

function getVisibleDirectGeneCount(peakId: string): number {
  return getPeakGeneLinksForPeak(visibleGraph.value, peakId).length;
}

function getGeneExpansionNeighborLimit(node: GraphNode): number {
  return getLocalExpansionLimit(getLinkedPeaksTotal(node));
}

function getPeakExpansionNeighborLimit(node: GraphNode): number {
  return getLocalExpansionLimit(getLinkedGenesTotal(node));
}

function getLocalExpansionLimit(totalCount: number | null): number {
  if (totalCount === null) return LOCAL_NODE_LINK_LIMIT;
  return Math.min(totalCount, LOCAL_NODE_LINK_LIMIT);
}

function isLocalLinkLimitReached(node: GraphNode): boolean {
  if (node.type === "gene") {
    const totalCount = getLinkedPeaksTotal(node);
    const visibleCount = getVisibleDirectPeakCount(node.id);
    return totalCount !== null && totalCount > LOCAL_NODE_LINK_LIMIT && visibleCount >= LOCAL_NODE_LINK_LIMIT;
  }

  if (node.type === "peak") {
    const totalCount = getLinkedGenesTotal(node);
    const visibleCount = getVisibleDirectGeneCount(node.id);
    return totalCount !== null && totalCount > LOCAL_NODE_LINK_LIMIT && visibleCount >= LOCAL_NODE_LINK_LIMIT;
  }

  return false;
}

function getNodeExhaustionMessage(node: GraphNode): string {
  return isLocalLinkLimitReached(node) ? LOCAL_LINK_LIMIT_MESSAGE : NO_ADDITIONAL_RELATED_MESSAGE;
}

function getLocalLinkLimitHint(totalCount: number | null | undefined): string | undefined {
  return isFiniteNumber(totalCount) && totalCount > LOCAL_NODE_LINK_LIMIT ? LOCAL_LINK_LIMIT_MESSAGE : undefined;
}

function supportsFullLinksQuery(node: GraphNode): boolean {
  return node.type === "gene" || node.type === "peak";
}

function getFullLinksButtonLabel(node: GraphNode): string {
  if (node.type === "gene") return "View all peak links";
  if (node.type === "peak") return "View all gene links";
  return "View all links";
}

function getNodeFullLinksTotal(node: GraphNode): number | null {
  if (node.type === "gene") return getLinkedPeaksTotal(node);
  return getLinkedGenesTotal(node);
}

function isNodeExhausted(node: GraphNode): boolean {
  if (node.type === "gene") return getVisibleDirectPeakCount(node.id) >= getGeneExpansionNeighborLimit(node);
  return getVisibleDirectGeneCount(node.id) >= getPeakExpansionNeighborLimit(node);
}

function hasNodeExpandableDirectNeighbors(node: GraphNode): boolean {
  if (node.type === "gene") {
    return getVisibleDirectPeakCount(node.id) < getGeneExpansionNeighborLimit(node);
  }

  return getVisibleDirectGeneCount(node.id) < getPeakExpansionNeighborLimit(node);
}

function isGraphNodeDisplayLimitBlockingExpansion(node: GraphNode): boolean {
  return hasNodeExpandableDirectNeighbors(node) && visibleGraph.value.nodes.length >= MAX_VISIBLE_GRAPH_NODES;
}

function isNodeExpansionDisabled(node: GraphNode): boolean {
  return isNodeExhausted(node) || isGraphNodeDisplayLimitBlockingExpansion(node);
}

function getNodeExpansionDisabledTitle(node: GraphNode): string | undefined {
  if (isNodeExhausted(node)) return getNodeExhaustionMessage(node);
  if (isGraphNodeDisplayLimitBlockingExpansion(node)) return NODE_DISPLAY_LIMIT_MESSAGE;
  return undefined;
}

function markNodeExhausted(node: GraphNode) {
  exhaustedNodeIds.value = new Set([...exhaustedNodeIds.value, node.id]);
  inspectorHelper.value = getNodeExhaustionMessage(node);
  helperContinueNodeId.value = "";
}

function clearNodeExhausted(nodeId: string) {
  if (!exhaustedNodeIds.value.has(nodeId)) return;
  const nextIds = new Set(exhaustedNodeIds.value);
  nextIds.delete(nodeId);
  exhaustedNodeIds.value = nextIds;
}

function startNewItemAnimation(anchorId: string, nodeIds: Set<string>, edgeIds: Set<string>) {
  window.clearTimeout(newItemTimer);
  const anchor = nodeById.value.get(anchorId);
  const origin = anchor
    ? { x: anchor.x, y: anchor.y }
    : { x: graphDimensions.value.width / 2, y: graphDimensions.value.height / 2 };
  enteringNodeOrigins.value = Object.fromEntries(Array.from(nodeIds).map((nodeId) => [nodeId, origin]));
  newlyAddedNodeIds.value = new Set(nodeIds);
  newlyAddedEdgeIds.value = new Set(edgeIds);
  newItemTimer = window.setTimeout(clearNewItemState, NEW_ITEM_FLAG_MS);
}

function clearNewItemState() {
  window.clearTimeout(newItemTimer);
  newlyAddedNodeIds.value = new Set();
  newlyAddedEdgeIds.value = new Set();
  enteringNodeOrigins.value = {};
}

function getNodeActions(node: GraphNode): InspectorAction[] {
  const actions: InspectorAction[] = [];
  const expansionDisabled = isNodeExpansionDisabled(node);
  actions.push({
    label: focusedNodeId.value === node.id ? "Re-focus layout" : "Focus on this node",
    action: "focusNode",
    variant: "secondary",
  });

  actions.push({
    label: "Show more related nodes",
    action: "expandNeighbors",
    variant: "primary",
    disabled: expansionDisabled,
    title: expansionDisabled ? getNodeExpansionDisabledTitle(node) : undefined,
  });

  return actions;
}

function getNodeDetail(node: GraphNode): InspectorDetail {
  const actions = getNodeActions(node);

  if (node.type === "gene") {
    const geneLinks = getPeakGeneLinksForGene(visibleGraph.value, node.id);
    const summary = getDatasetSummaryFromLinks(geneLinks, node.metadata);
    const topLinkedPeaks = getTopListValue(node.metadata.topLinkedPeaks, geneLinks.map((link) => link.topLinkedPeaks), geneLinks.map((link) => link.peak));
    const linkedPeaksTotal = getLinkedPeaksTotal(node, geneLinks) ?? getUniqueLinkCount(geneLinks, "peak");
    const visiblePeakCount = getVisibleDirectPeakCount(node.id);
    const items: InspectorDetailItem[] = [];
    appendIfValue(items, "Gene symbol", node.label);
    appendIfValue(items, "Best linked peak", getFirstTopListItem(topLinkedPeaks));
    appendIfValue(items, "Linked peaks", formatTotalCount(linkedPeaksTotal));
    appendIfValue(items, "Visible in graph", formatVisibleInGraphCount(visiblePeakCount, linkedPeaksTotal));
    appendIfValue(items, "Graph view", getLocalLinkLimitHint(linkedPeaksTotal));
    appendIfValue(items, "Max link score", formatNumberValue(node.metadata.maxLinkScore ?? getFirstFiniteNumber(geneLinks.map((link) => link.maxLinkScore)) ?? getMaxLinkScoreValue(geneLinks)));
    appendIfValue(items, "Correlation range", formatRangeFromSummary(
      node.metadata.correlationMin ?? getFirstFiniteNumber(geneLinks.map((link) => link.correlationMin)),
      node.metadata.correlationMax ?? getFirstFiniteNumber(geneLinks.map((link) => link.correlationMax)),
      geneLinks.map((link) => link.correlation)
    ));
    appendFdrSummary(items, node.metadata, geneLinks);
    appendDatasetSummaryItems(items, summary);
    appendProvenance(items, summary, geneLinks.map((link) => link.source), node.metadata.source);

    return {
      kind: node.type,
      chip: "Gene",
      title: node.label,
      items,
      actions,
    };
  }

  const peakLinks = getPeakGeneLinksForPeak(visibleGraph.value, node.id);
  const title = node.label;
  const summary = getDatasetSummaryFromLinks(peakLinks, node.metadata);
  const topLinkedGenes = getTopListValue(node.metadata.topLinkedGenes, peakLinks.map((link) => link.topLinkedGenes), peakLinks.map((link) => link.geneSymbol));
  const linkedGenesTotal = getLinkedGenesTotal(node, peakLinks) ?? getUniqueLinkCount(peakLinks, "gene");
  const visibleGeneCount = getVisibleDirectGeneCount(node.id);
  const items: InspectorDetailItem[] = [];
  appendIfValue(items, "Linked genes", formatTotalCount(linkedGenesTotal));
  appendIfValue(items, "Visible in graph", formatVisibleInGraphCount(visibleGeneCount, linkedGenesTotal));
  appendIfValue(items, "Graph view", getLocalLinkLimitHint(linkedGenesTotal));
  appendIfValue(items, "Top linked genes", formatTopListWithMore(
    topLinkedGenes,
    node.metadata.remainingLinkedGenesCount ?? getFirstFiniteNumber(peakLinks.map((link) => link.remainingLinkedGenesCount)),
    3
  ));
  appendIfValue(items, "Best linked gene", getFirstTopListItem(topLinkedGenes));
  appendIfValue(items, "Max link score", formatNumberValue(node.metadata.maxLinkScore ?? getFirstFiniteNumber(peakLinks.map((link) => link.maxLinkScore)) ?? getMaxLinkScoreValue(peakLinks)));
  appendIfValue(items, "Correlation range", formatRangeFromSummary(
    node.metadata.correlationMin ?? getFirstFiniteNumber(peakLinks.map((link) => link.correlationMin)),
    node.metadata.correlationMax ?? getFirstFiniteNumber(peakLinks.map((link) => link.correlationMax)),
    peakLinks.map((link) => link.correlation)
  ));
  appendFdrSummary(items, node.metadata, peakLinks);
  appendDatasetSummaryItems(items, summary);
  appendProvenance(items, summary, peakLinks.map((link) => link.source), node.metadata.source);

  return {
    kind: "peak",
    chip: "Peak",
    title,
    subtitle: node.metadata.chromosome
      ? `${node.metadata.chromosome}:${displayText(node.metadata.start)}-${displayText(node.metadata.end)}`
      : undefined,
    items: filterPeakDetailItems(title, items),
    actions,
  };
}

function hasDistinctSubtitle(title: string, subtitle: string): boolean {
  const normalizedTitle = normalizeDetailText(title);
  const normalizedSubtitle = normalizeDetailText(subtitle);
  return Boolean(normalizedSubtitle && normalizedSubtitle !== normalizedTitle);
}

function filterPeakDetailItems(title: string, items: InspectorDetailItem[]): InspectorDetailItem[] {
  const duplicateLabels = new Set(["peak", "peak id", "region"]);
  return items.filter((item) => {
    if (duplicateLabels.has(normalizeDetailLabel(item.label))) return false;
    return !isDuplicateHeaderValue(title, item.value);
  });
}

function isDuplicateHeaderValue(title: string, value: string): boolean {
  return title.trim() === value.trim();
}

function normalizeDetailText(value: string): string {
  return value.trim().replace(/\s+/g, " ").toLowerCase();
}

function normalizeDetailLabel(value: string): string {
  return value.trim().replace(/\s+/g, " ").toLowerCase();
}

function getEdgeDetail(edge: GraphEdge): InspectorDetail {
  const link = getLinkForEdge(edge);
  const preferredNode = getPreferredNodeForEdgeAction(edge);

  const items: InspectorDetailItem[] = [];
  appendIfValue(items, "Peak", link?.peak ?? getNodeLabel(edge.source));
  appendIfValue(items, "Linked gene", link?.geneSymbol ?? getNodeLabel(edge.target));
  appendIfValue(items, "Link score", formatNumberValue(link?.linkScore ?? link?.score ?? edge.score));
  appendIfValue(items, "Distance to TSS", formatNumberValue(link?.distanceToTss ?? edge.distanceToTss));
  appendIfValue(items, "Correlation", formatNumberValue(link?.correlation ?? edge.correlation));
  appendIfValue(items, "FDR", formatNumberValue(link?.fdr ?? edge.fdr));
  appendIfValue(items, "VarQ ATAC", formatNumberValue(link?.varQAtac ?? edge.varQAtac));
  appendIfValue(items, "VarQ RNA", formatNumberValue(link?.varQRna ?? edge.varQRna));
  const summary = getDatasetSummaryFromLinks(link ? [link] : []);
  appendDatasetSummaryItems(items, summary);
  appendProvenance(items, summary, [link?.source, edge.sourceMethod]);

  return {
    kind: "edge",
    chip: "Peak-to-gene",
    title: `${displayText(link?.peak ?? getNodeLabel(edge.source))} -> ${displayText(link?.geneSymbol ?? getNodeLabel(edge.target))}`,
    items,
    actions: preferredNode ? getNodeActions(preferredNode) : undefined,
  };
}

function getPreferredNodeForEdgeAction(edge: GraphEdge): GraphNode | null {
  const source = nodeById.value.get(edge.source);
  const target = nodeById.value.get(edge.target);
  if (source?.type === "gene") return source;
  if (target?.type === "gene") return target;
  if (source?.type === "peak") return source;
  return target ?? source ?? null;
}

function getLinkForEdge(edge: GraphEdge): GraphLink | undefined {
  return visibleGraph.value.links.find((link) => {
    const edgeId = graphEdgeId(getGeneNodeId(link.geneSymbol), getPeakNodeId(link.peak), "peakGene");
    return edgeId === edge.id;
  });
}

function openLinkDetail(row: GraphLink) {
  interactionEpoch.value += 1;
  selectedNodeId.value = "";
  selectedEdgeId.value = graphEdgeId(getGeneNodeId(row.geneSymbol), getPeakNodeId(row.peak), "peakGene");
  peakLabelMode.value = "active";
  inspectorHelper.value = "";
  helperContinueNodeId.value = "";
}

function peakCoordinates(row: GraphLink): { chrom: string; start: number; end: number } | null {
  const match = row.peak.replace(/,/g, "").trim().match(/^(chr[^:\s]+):(\d+)-(\d+)$/i);
  if (!match) return null;
  const start = Number(match[2]);
  const end = Number(match[3]);
  if (!Number.isFinite(start) || !Number.isFinite(end) || start >= end) return null;
  return { chrom: match[1]!, start, end };
}

function canViewPeakDetail(row: GraphLink): boolean {
  return peakCoordinates(row) !== null;
}

function peakDatasetLabel(row: GraphLink): string {
  return displayText(row.datasetId || props.datasetId);
}

function peakDomainLabel(row: GraphLink): string {
  return displayText(formatDomainValue(row.domain || props.domain));
}

function goToPeakDetail(row: GraphLink) {
  const coordinates = peakCoordinates(row);
  if (!coordinates) return;
  void router.push({
    path: "/feature-detail",
    query: {
      type: "peak",
      chrom: coordinates.chrom,
      start: String(coordinates.start),
      end: String(coordinates.end),
      peakId: row.peakId || undefined,
      datasetId: row.datasetId || props.datasetId,
      domain: row.domain || props.domain,
      source: "sample_regulatory_network",
      returnTo: route.fullPath,
    },
  });
}

function continueHelperExpansion() {
  const node = helperContinueNode.value;
  if (!node || isNodeExpansionDisabled(node)) return;
  void expandNode(node.id, "manual");
}

function getLinkRelevance(row: GraphLink): number {
  if (selectedEdge.value) {
    const rowEdgeId = graphEdgeId(getGeneNodeId(row.geneSymbol), getPeakNodeId(row.peak), "peakGene");
    return rowEdgeId === selectedEdge.value.id ? 4 : 0;
  }

  if (!selectedNode.value) return 0;
  if (selectedNode.value.type === "peak") return getPeakNodeId(row.peak) === selectedNode.value.id ? 3 : 0;
  if (selectedNode.value.type === "gene") return getGeneNodeId(row.geneSymbol) === selectedNode.value.id ? 3 : 0;
  return 0;
}

function getLinkRowClassName({ row }: { row: GraphLink }) {
  return getLinkRelevance(row) > 0 ? "detail-table-row--related" : "";
}

function getLinkScore(link: GraphLink): number {
  return Number(link.linkScore ?? link.score ?? 0);
}

function currentQueryParams(): { minScore?: number; gene?: string; peak?: string } {
  const minScore = parseOptionalNumber(queryMode.value === "gene" ? geneMinScore.value : peakMinScore.value);
  const gene = queryMode.value === "gene" ? geneQuery.value.trim() : linkedGeneQuery.value.trim();
  const peak = queryMode.value === "peak" ? peakQuery.value.trim() : "";
  return {
    minScore,
    gene: gene || undefined,
    peak: peak || undefined,
  };
}

function parseOptionalNumber(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function getNodeLabel(nodeId: string): string {
  return nodeById.value.get(nodeId)?.label ?? nodeId;
}

function appendIfValue(items: InspectorDetailItem[], label: string, value: unknown) {
  if (!hasDisplayValue(value)) return;
  const text = typeof value === "number" ? formatNumber(value) : String(value).trim();
  items.push({ label, value: text });
}

function hasDisplayValue(value: unknown): boolean {
  if (value === null || value === undefined) return false;
  if (typeof value === "number") return Number.isFinite(value);
  if (typeof value === "string") {
    const trimmed = value.trim();
    return Boolean(trimmed && trimmed !== "-");
  }
  return true;
}

function formatTopListWithMore(values: TopListValue, remainingCount: number | null | undefined, limit: number): string | undefined {
  const list = toTopListValues(values);
  if (!list.length) return undefined;
  const visible = list.slice(0, limit);
  const fallbackRemaining = Math.max(0, list.length - visible.length);
  const remaining = isFiniteNumber(remainingCount) && remainingCount > 0 ? remainingCount : fallbackRemaining;
  return `${visible.join(", ")}${remaining > 0 ? ` +${remaining} more` : ""}`;
}

function getTopListValue(primary: TopListValue, summaryValues: TopListValue[], fallbackValues: string[]): TopListValue {
  if (toTopListValues(primary).length) return primary;
  const summary = summaryValues.find((value) => toTopListValues(value).length);
  return summary ?? fallbackValues;
}

function getFirstTopListItem(values: TopListValue): string | undefined {
  return toTopListValues(values)[0];
}

function toTopListValues(values: TopListValue): string[] {
  const rawValues = Array.isArray(values)
    ? values
    : typeof values === "string"
      ? values.split(/[,;|]/)
      : [];
  return Array.from(new Set(rawValues.map((value) => String(value).trim()).filter(Boolean)));
}

function getFirstFiniteNumber(values: Array<number | null | undefined>): number | null {
  return values.find(isFiniteNumber) ?? null;
}

function getMaxFiniteCount(values: Array<number | null | undefined>): number | null {
  const counts = values
    .filter((candidate): candidate is number => isFiniteNumber(candidate) && candidate >= 0)
    .map((value) => Math.round(value));
  return counts.length ? Math.max(...counts) : null;
}

function getLinkedPeaksTotal(
  node: GraphNode,
  visibleNodeLinks: GraphLink[] = getPeakGeneLinksForGene(visibleGraph.value, node.id)
): number | null {
  const sourceNode = sourceNodeById.value.get(node.id);
  const sourceNodeLinks = sourceNode ? getPeakGeneLinksForGene(sourceGraph.value, node.id) : [];
  return getMaxFiniteCount([
    node.metadata.linkedPeaksCount,
    sourceNode?.metadata.linkedPeaksCount,
    ...visibleNodeLinks.map((link) => link.linkedPeaksCount),
    ...sourceNodeLinks.map((link) => link.linkedPeaksCount),
    node.metadata.totalLinks,
    sourceNode?.metadata.totalLinks,
    ...visibleNodeLinks.map((link) => link.totalLinks),
    ...sourceNodeLinks.map((link) => link.totalLinks),
  ]);
}

function getLinkedGenesTotal(
  node: GraphNode,
  visibleNodeLinks: GraphLink[] = getPeakGeneLinksForPeak(visibleGraph.value, node.id)
): number | null {
  const sourceNode = sourceNodeById.value.get(node.id);
  const sourceNodeLinks = sourceNode ? getPeakGeneLinksForPeak(sourceGraph.value, node.id) : [];
  return getMaxFiniteCount([
    node.metadata.linkedGenesCount,
    sourceNode?.metadata.linkedGenesCount,
    ...visibleNodeLinks.map((link) => link.linkedGenesCount),
    ...sourceNodeLinks.map((link) => link.linkedGenesCount),
    node.metadata.totalLinks,
    sourceNode?.metadata.totalLinks,
    ...visibleNodeLinks.map((link) => link.totalLinks),
    ...sourceNodeLinks.map((link) => link.totalLinks),
  ]);
}

function getMaxLinkScoreValue(nodeLinks: GraphLink[]): number | null {
  const scores = nodeLinks
    .map((link) => link.linkScore ?? link.score)
    .filter(isFiniteNumber);
  return scores.length ? Math.max(...scores) : null;
}

function getUniqueLinkCount(nodeLinks: GraphLink[], target: "gene" | "peak"): number {
  const values = nodeLinks.map((link) => target === "gene" ? link.geneSymbol : link.peak);
  return new Set(values.filter(Boolean)).size;
}

function formatCount(value: number | null | undefined): string | undefined {
  return isFiniteNumber(value) ? Math.round(value).toLocaleString() : undefined;
}

function formatTotalCount(value: number | null | undefined): string | undefined {
  const count = formatCount(value);
  return count ? `${count} total` : undefined;
}

function formatVisibleInGraphCount(visibleCount: number, totalCount: number | null | undefined): string {
  const total = formatCount(totalCount);
  return total ? `${visibleCount.toLocaleString()} / ${total}` : visibleCount.toLocaleString();
}

function formatNumberValue(value: unknown): string | undefined {
  return isFiniteNumber(value) ? formatNumber(value) : undefined;
}

function formatRangeFromSummary(
  minValue: number | null | undefined,
  maxValue: number | null | undefined,
  fallbackValues: Array<number | null | undefined>
): string | undefined {
  if (isFiniteNumber(minValue) && isFiniteNumber(maxValue)) {
    return Math.abs(maxValue - minValue) <= 1e-9 ? formatNumber(minValue) : `${formatNumber(minValue)} - ${formatNumber(maxValue)}`;
  }
  if (isFiniteNumber(minValue)) return formatNumber(minValue);
  if (isFiniteNumber(maxValue)) return formatNumber(maxValue);

  const numbers = fallbackValues.filter(isFiniteNumber);
  if (!numbers.length) return undefined;
  const min = Math.min(...numbers);
  const max = Math.max(...numbers);
  return Math.abs(max - min) <= 1e-9 ? formatNumber(min) : `${formatNumber(min)} - ${formatNumber(max)}`;
}

function appendFdrSummary(items: InspectorDetailItem[], metadata: GraphNode["metadata"], nodeLinks: GraphLink[]) {
  const minFdr = metadata.minFdr
    ?? metadata.fdrMin
    ?? getFirstFiniteNumber(nodeLinks.map((link) => link.minFdr))
    ?? getFirstFiniteNumber(nodeLinks.map((link) => link.fdrMin));
  if (isFiniteNumber(minFdr)) {
    appendIfValue(items, "Min FDR", formatNumber(minFdr));
    return;
  }

  appendIfValue(items, "FDR range", formatRangeFromSummary(
    metadata.fdrMin ?? getFirstFiniteNumber(nodeLinks.map((link) => link.fdrMin)),
    metadata.fdrMax ?? getFirstFiniteNumber(nodeLinks.map((link) => link.fdrMax)),
    nodeLinks.map((link) => link.fdr)
  ));
}

function getDatasetSummaryFromLinks(nodeLinks: GraphLink[], metadata?: GraphNode["metadata"]): DatasetSummary {
  return {
    datasetId: firstDisplayValue(metadata?.datasetId, ...nodeLinks.map((link) => link.datasetId), props.datasetId),
    sampleName: firstDisplayValue(metadata?.sampleName, ...nodeLinks.map((link) => link.sampleName)),
    domain: firstDisplayValue(metadata?.domain, ...nodeLinks.map((link) => link.domain), props.domain),
  };
}

function appendDatasetSummaryItems(items: InspectorDetailItem[], summary: DatasetSummary) {
  appendIfValue(items, "Dataset", summary.datasetId);
  appendIfValue(items, "Sample", summary.sampleName);
  appendIfValue(items, "Domain", formatDomainValue(summary.domain));
}

function formatDomainValue(value: string | undefined): string | undefined {
  if (!hasDisplayValue(value)) return undefined;
  return value === props.domain ? domainDisplayLabel(props.domain) : value;
}

function appendProvenance(
  items: InspectorDetailItem[],
  summary: DatasetSummary,
  sources: Array<string | undefined>,
  fallback?: string
) {
  const visibleSources = Array.from(new Set([fallback, ...sources].map((source) => getVisibleSource(source, summary)).filter(hasDisplayValue)));
  appendIfValue(items, "Provenance", visibleSources.slice(0, 3).join(", "));
}

function getVisibleSource(value: string | undefined, summary?: DatasetSummary): string | undefined {
  if (isInternalSource(value)) return undefined;
  const trimmed = value?.trim();
  if (!trimmed) return undefined;
  if (isSameDetailSource(trimmed, summary?.sampleName) || isSameDetailSource(trimmed, summary?.datasetId)) return undefined;
  return trimmed;
}

function isSameDetailSource(source: string, value: string | undefined): boolean {
  return typeof value === "string" && source === value.trim();
}

function isInternalSource(value: unknown): boolean {
  if (typeof value !== "string") return true;
  const normalized = value.trim().toLowerCase();
  return !normalized || normalized === "-" || normalized === "frontend_import" || normalized === "import" || normalized === "internal" || normalized === "unknown";
}

function firstDisplayValue(...values: Array<string | undefined>): string | undefined {
  return values.find(hasDisplayValue);
}

function formatNumber(value: unknown): string {
  if (!isFiniteNumber(value)) return "-";
  if (Math.abs(value) >= 1000) return Math.round(value).toLocaleString();
  if (Math.abs(value) < 0.001 && value !== 0) return value.toExponential(2);
  return Number(value.toFixed(3)).toString();
}

function displayText(value: unknown): string {
  if (value === undefined || value === null || value === "") return "-";
  return String(value);
}

function isFiniteNumber(value: unknown): value is number {
  return typeof value === "number" && Number.isFinite(value);
}

function onLinkPageSizeChange(size: number) {
  linkPageSize.value = size;
  linkPage.value = 1;
}

function onLinkPageChange(page: number) {
  linkPage.value = page;
}

function openFullLinksDialog() {
  if (!fullLinksAction.value || !selectedNode.value) return;
  fullLinksError.value = "";
  fullLinksPage.value = 1;
  if (fullLinksDialogVisible.value) {
    void loadFullLinks();
    return;
  }
  fullLinksDialogVisible.value = true;
}

async function loadFullLinks() {
  const node = selectedNode.value;
  if (!node || !fullLinksAction.value) return;

  const token = ++fullLinksRequestToken;
  fullLinksLoading.value = true;
  fullLinksError.value = "";

  try {
    const response = await requestFullLinks(node);

    if (token !== fullLinksRequestToken) return;
    fullLinksTotal.value = response.total;
    fullLinksPage.value = response.page;
    fullLinksPageSize.value = response.pageSize;
    fullLinksItems.value = normalizeFullLinkItems(response.items);
  } catch (fullLinksErrorValue) {
    if (token !== fullLinksRequestToken) return;
    console.error("[SearchResult] Failed to load regulatory links:", fullLinksErrorValue);
    fullLinksItems.value = [];
    fullLinksTotal.value = 0;
    fullLinksError.value = "Full links request failed. Please retry.";
  } finally {
    if (token === fullLinksRequestToken) fullLinksLoading.value = false;
  }
}

async function requestFullLinks(node: GraphNode) {
  const params = currentQueryParams();
  return fetchRegulatoryNetworkLinks({
    datasetId: props.datasetId,
    domain: props.domain,
    nodeType: getExpansionNodeType(node),
    nodeId: node.id,
    page: fullLinksPage.value,
    pageSize: fullLinksPageSize.value,
    gene: params.gene,
    peak: params.peak,
    minScore: params.minScore,
  });
}

function normalizeFullLinkItems(items: RegulatoryNetworkLink[]): GraphLink[] {
  return adaptRegulatoryResponse({
    nodes: [],
    edges: [],
    links: items,
    datasetId: props.datasetId,
    domain: props.domain,
  }).links;
}

function onFullLinksPageSizeChange(size: number) {
  fullLinksPageSize.value = size;
  fullLinksPage.value = 1;
  void loadFullLinks();
}

function onFullLinksPageChange(page: number) {
  fullLinksPage.value = page;
  void loadFullLinks();
}

function downloadCsv() {
  if (!visibleLinks.value.length) return;
  downloadLinksCsv(
    visibleLinks.value,
    `${sanitizeFilenamePart(props.datasetId)}_${props.domain}_graph_visible_regulatory_links.csv`
  );
}

async function downloadFullLinksAllCsv() {
  const node = selectedNode.value;
  if (!node || !fullLinksAction.value) return;
  const nodePart = sanitizeFilenamePart(node.label);
  const filename = `${sanitizeFilenamePart(props.datasetId)}_${props.domain}_${nodePart}_regulatory_links_all.csv`;
  const allItems: GraphLink[] = [];
  const params = currentQueryParams();
  const pageSize = 100;
  let page = 1;
  let total = 0;
  try {
    while (true) {
      const response = await fetchRegulatoryNetworkLinks({
        datasetId: props.datasetId,
        domain: props.domain,
        nodeType: getExpansionNodeType(node),
        nodeId: node.id,
        page,
        pageSize,
        gene: params.gene,
        peak: params.peak,
        minScore: params.minScore,
      });
      const items = normalizeFullLinkItems(response.items);
      allItems.push(...items);
      total = response.total;
      if (allItems.length >= total) break;
      page++;
    }
    downloadLinksCsv(allItems, filename);
  } catch (e) {
    console.error("Failed to download all regulatory links:", e);
  }
}

function downloadLinksCsv(rows: GraphLink[], filename: string) {
  const header = [
    "peak",
    "linkedGene",
    "distanceToTss",
    "linkScore",
    "correlation",
    "fdr",
    "varQAtac",
    "varQRna",
    "source",
  ];
  const csvRows = rows.map((row) => [
    row.peak,
    row.geneSymbol,
    row.distanceToTss ?? "",
    row.linkScore ?? row.score ?? "",
    row.correlation ?? "",
    row.fdr ?? "",
    row.varQAtac ?? "",
    row.varQRna ?? "",
    row.source ?? "",
  ]);
  const csv = [header, ...csvRows]
    .map((row) => row.map((value) => `"${String(value).replace(/"/g, '""')}"`).join(","))
    .join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

function sanitizeFilenamePart(value: string): string {
  return value.replace(/[^a-zA-Z0-9._-]+/g, "_") || "sample";
}

async function focusGene(gene: string) {
  queryMode.value = "gene";
  geneQuery.value = gene;
  await searchNetwork();
  sectionEl.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

defineExpose({
  focusGene,
});
</script>

<style scoped>
.regulatory-section {
  --detail-teal: var(--brand-primary-3);
  --detail-teal-hover: #7f9a90;
  --detail-teal-active: #6f887d;
  --detail-teal-border: var(--nav-active-border);
  --graph-gene: #B11D56;
  --graph-gene-border: #B11D56;
  --graph-gene-alt: #11A3B8;
  --graph-peak: #F59ABB;
  --graph-peak-border: #F59ABB;
  --graph-peak-alt: #8FD9E5;
  --graph-edge: rgba(71, 85, 105, 0.58);
  --el-color-primary: var(--detail-teal);
  position: relative;
  overflow: hidden;
  padding: 15px 18px 17px;
  border-color: var(--border);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.regulatory-section:hover {
  border-color: var(--border-brand);
  box-shadow: var(--shadow-hover);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.section-title {
  font-size: 18px;
  font-weight: 900;
}

.section-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.section-tags {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.data-chip,
.demo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 5px 9px;
  border: 1px solid var(--nav-active-border);
  border-radius: 999px;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.data-chip {
  max-width: 220px;
  min-height: 30px;
  padding: 6px 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.demo-badge {
  border-color: rgba(143, 165, 156, 0.28);
  background: rgba(255, 255, 255, 0.68);
  color: rgba(95, 125, 112, 0.82);
}

.mono {
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
}

.network-control-card {
  box-sizing: border-box;
  margin: 12px 0 16px;
  padding: 15px 16px 16px;
  border: 1px solid rgba(143, 165, 156, 0.22);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 252, 250, 0.88)),
    rgba(245, 250, 248, 0.82);
  box-shadow:
    0 14px 32px rgba(15, 23, 42, 0.055),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.network-control-card:hover {
  border-color: rgba(143, 165, 156, 0.34);
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.075),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
  transform: translateY(-2px);
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  min-width: 214px;
  padding: 4px;
  margin-bottom: 14px;
  border: 1px solid rgba(143, 165, 156, 0.22);
  border-radius: 999px;
  background: #f5faf8;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.035);
}

.mode-button {
  appearance: none;
  min-width: 98px;
  min-height: 38px;
  padding: 8px 24px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: rgba(74, 96, 88, 0.78);
  cursor: pointer;
  font-size: 14px;
  font-weight: 760;
  line-height: 1;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.mode-button:hover {
  background: rgba(255, 255, 255, 0.72);
  color: var(--detail-teal-active);
}

.mode-button.active {
  background: #c7d8d2;
  color: #173f38;
  font-weight: 900;
  box-shadow:
    0 6px 16px rgba(95, 125, 112, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

.query-panel {
  display: flex;
  align-items: flex-end;
  column-gap: 12px;
  row-gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 0;
}

.control-field {
  display: grid;
  flex: 0 0 166px;
  min-width: 150px;
  gap: 7px;
}

.control-field--grow {
  flex: 1 1 340px;
  min-width: 280px;
}

.control-label {
  color: rgba(39, 66, 58, 0.84);
  font-size: 12px;
  font-weight: 900;
}

.control-input {
  width: 100%;
}

.query-input-error {
  display: flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  min-height: 25px;
  box-sizing: border-box;
  padding: 5px 9px;
  border: 1px solid rgba(190, 78, 78, 0.34);
  border-radius: 8px;
  background: rgba(255, 242, 242, 0.98);
  color: #a43f3f;
  font-size: 11px;
  font-weight: 850;
  line-height: 1.35;
  box-shadow: 0 5px 13px rgba(143, 55, 55, 0.08);
}

.query-error-enter-active,
.query-error-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.query-error-enter-from,
.query-error-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

.control-field--invalid :deep(.el-input__wrapper) {
  background: #fffafa;
  box-shadow:
    0 0 0 1px rgba(190, 78, 78, 0.48) inset,
    0 0 0 3px rgba(190, 78, 78, 0.08);
}

.query-panel :deep(.el-input__wrapper),
.query-panel :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(143, 165, 156, 0.2) inset;
  transition:
    background-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.query-panel :deep(.el-input__wrapper:hover),
.query-panel :deep(.el-select__wrapper:hover) {
  background: #ffffff;
  box-shadow:
    0 0 0 1px rgba(143, 165, 156, 0.34) inset,
    0 5px 14px rgba(95, 125, 112, 0.08);
}

.query-panel :deep(.el-input__wrapper.is-focus),
.query-panel :deep(.el-select__wrapper.is-focused) {
  background: #ffffff;
  box-shadow:
    0 0 0 1px rgba(127, 154, 144, 0.5) inset,
    0 0 0 3px rgba(143, 165, 156, 0.16);
}

.query-panel :deep(.el-input__inner),
.query-panel :deep(.el-select__placeholder),
.query-panel :deep(.el-select__selected-item) {
  font-size: 14px;
  font-weight: 760;
}

.section-button,
.soft-button,
.view-button {
  font-weight: 900;
}

.section-button,
.query-panel .reset-button {
  align-self: end;
  min-width: 112px;
  min-height: 44px;
  border-radius: 13px;
  font-size: 14px;
  line-height: 1;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.section-button :deep(span),
.query-panel .reset-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.section-button {
  --el-button-bg-color: #8faea3;
  --el-button-border-color: #8faea3;
  --el-button-hover-bg-color: #7f9f94;
  --el-button-hover-border-color: #7f9f94;
  --el-button-active-bg-color: #6f8f84;
  --el-button-active-border-color: #6f8f84;
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.2);
}

.section-button:hover {
  box-shadow: 0 10px 22px rgba(95, 125, 112, 0.26);
  transform: translateY(-1px);
}

.soft-button,
.view-button {
  border-color: var(--border);
  background: var(--surface);
  color: var(--text);
}

.query-panel .reset-button {
  border-color: rgba(143, 165, 156, 0.26);
  background: rgba(255, 255, 255, 0.9);
  color: #173f38;
  box-shadow: 0 5px 14px rgba(15, 23, 42, 0.04);
}

.soft-button:hover,
.view-button:hover {
  border-color: var(--detail-teal-border);
  background: var(--surface-2);
  color: var(--detail-teal-active);
}

.query-panel .reset-button:hover {
  border-color: rgba(143, 165, 156, 0.4);
  background: rgba(245, 250, 248, 0.98);
  color: #173f38;
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.12);
  transform: translateY(-1px);
}

.network-browser {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(270px, 340px);
  gap: 14px;
  align-items: start;
  margin-bottom: 8px;
  overflow: clip;
  isolation: isolate;
}

.graph-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(143, 165, 156, 0.18);
  border-radius: 16px;
  background:
    radial-gradient(ellipse at 18% 8%, rgba(230, 249, 245, 0.62), rgba(230, 249, 245, 0) 38%),
    rgba(255, 255, 255, 0.7);
  box-shadow:
    0 16px 38px rgba(15, 23, 42, 0.048),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  align-self: start;
  backdrop-filter: blur(10px) saturate(108%);
}

.graph-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(143, 165, 156, 0.16);
  background: rgba(255, 255, 255, 0.52);
  color: var(--text);
  font-size: 12px;
  font-weight: 900;
}

.graph-reset-view {
  position: absolute;
  top: 48px;
  right: 10px;
  z-index: 3;
  min-height: 30px;
  padding: 6px 11px;
  border: 1px solid rgba(143, 165, 156, 0.2);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.46);
  box-shadow:
    0 6px 14px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.62);
  color: rgba(50, 72, 68, 0.76);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
  backdrop-filter: blur(12px) saturate(112%);
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.graph-reset-view:hover {
  border-color: rgba(22, 140, 124, 0.2);
  background: rgba(255, 255, 255, 0.66);
  box-shadow:
    0 8px 18px rgba(15, 23, 42, 0.085),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  transform: translateY(-1px);
}

.graph-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  color: rgba(91, 105, 100, 0.72);
  font-size: 10px;
  font-weight: 800;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.legend-swatches {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.legend-dot {
  flex: 0 0 auto;
  width: 8px;
  height: 8px;
  border: 1px solid rgba(95, 125, 112, 0.3);
  border-radius: 999px;
}

.legend-dot--gene {
  background: var(--graph-gene);
  border-color: var(--graph-gene-border);
}

.legend-dot--gene-alt {
  background: var(--graph-gene-alt);
  border-color: var(--graph-gene-alt);
}

.legend-dot--peak {
  background: var(--graph-peak);
  border-color: var(--graph-peak-border);
}

.legend-dot--peak-alt {
  background: var(--graph-peak-alt);
  border-color: var(--graph-peak-alt);
}

.legend-line {
  display: inline-block;
  width: 15px;
  height: 0;
  border-top: 1.5px solid var(--graph-edge);
}

.graph-body {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 520px;
  min-height: 520px;
  max-height: 520px;
  padding: 24px;
  color: var(--muted);
  font-weight: 800;
  text-align: center;
}

.inspector-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  box-sizing: border-box;
  min-width: 0;
  max-width: 360px;
  height: 562px;
  min-height: 0;
  padding: 6px 0 6px 4px;
  overflow: hidden;
  align-self: start;
  isolation: isolate;
}

.inspector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  box-sizing: border-box;
  width: 100%;
  margin-bottom: 8px;
}

.inspector-heading-copy {
  min-width: 0;
}

.inspector-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0;
}

.inspector-card {
  position: relative;
  box-sizing: border-box;
  width: 100%;
  padding: 15px;
  border: 1px solid rgba(143, 165, 156, 0.145);
  border-radius: 16px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.88), rgba(248, 253, 251, 0.72)),
    radial-gradient(ellipse at 12% 4%, rgba(232, 249, 245, 0.58), rgba(232, 249, 245, 0) 42%);
  box-shadow:
    0 10px 22px rgba(15, 23, 42, 0.036),
    0 1px 2px rgba(15, 23, 42, 0.02),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  animation: inspector-fade-slide 240ms cubic-bezier(0.2, 0.82, 0.2, 1) both;
  backdrop-filter: blur(12px) saturate(110%);
}

.inspector-card + .inspector-card {
  margin-top: 8px;
}

.inspector-card--details {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.inspector-card--details::before,
.inspector-card--details::after {
  position: absolute;
  right: 15px;
  left: 15px;
  z-index: 1;
  height: 16px;
  content: "";
  pointer-events: none;
}

.inspector-card--details::before {
  top: 52px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0));
}

.inspector-card--details::after {
  bottom: 12px;
  background: linear-gradient(to top, rgba(255, 255, 255, 0.62), rgba(255, 255, 255, 0));
}

.inspector-card--empty::before,
.inspector-card--empty::after {
  display: none;
}

.inspector-card--actions {
  flex: 0 0 auto;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.84), rgba(248, 253, 251, 0.68)),
    radial-gradient(ellipse at 90% 10%, rgba(226, 244, 241, 0.36), rgba(226, 244, 241, 0) 46%);
}

.inspector-card--empty {
  justify-content: flex-start;
}

.inspector-details-scroll {
  position: relative;
  min-height: 0;
  overflow: auto;
  padding: 2px 5px 4px 0;
  scrollbar-color: rgba(143, 165, 156, 0.28) transparent;
  scrollbar-width: thin;
  -webkit-mask-image: linear-gradient(to bottom, #000 0, #000 calc(100% - 18px), transparent 100%);
  mask-image: linear-gradient(to bottom, #000 0, #000 calc(100% - 18px), transparent 100%);
}

.inspector-details-scroll::-webkit-scrollbar {
  width: 6px;
}

.inspector-details-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(143, 165, 156, 0.24);
}

.inspector-details-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.inspector-card-label {
  margin-bottom: 8px;
  color: rgba(80, 99, 94, 0.72);
  font-size: 13px;
  font-weight: 900;
}

.inspector-title-scroll {
  box-sizing: border-box;
  display: block;
  width: 100%;
  max-width: 100%;
  margin-bottom: 7px;
  padding-bottom: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  scrollbar-color: rgba(97, 122, 116, 0.35) transparent;
  scrollbar-width: thin;
}

.inspector-title-scroll:focus-visible {
  outline: 1px solid rgba(22, 140, 124, 0.24);
  outline-offset: 2px;
}

.inspector-title-scroll::-webkit-scrollbar {
  height: 5px;
}

.inspector-title-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(97, 122, 116, 0.32);
}

.inspector-title-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.inspector-title {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  margin-bottom: 6px;
  overflow: hidden;
  color: rgba(27, 40, 45, 0.92);
  font-size: 18px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inspector-title--edge {
  display: inline-block;
  width: auto;
  min-width: max-content;
  max-width: none;
  margin-bottom: 0;
  padding-right: 12px;
  overflow: visible;
  text-overflow: clip;
  white-space: nowrap;
}

.inspector-subtitle {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  margin-bottom: 11px;
  overflow: hidden;
  color: rgba(81, 99, 101, 0.72);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inspector-chip {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 5px 9px;
  border: 1.5px solid rgba(143, 165, 156, 0.17);
  border-radius: 999px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.64);
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.inspector-chip--gene {
  border-color: rgba(177, 29, 86, 0.30);
  background: rgba(177, 29, 86, 0.12);
  color: #8a163f;
}

.inspector-chip--peak {
  border-color: rgba(245, 154, 187, 0.40);
  background: rgba(245, 154, 187, 0.18);
  color: #9b4a63;
}

.inspector-chip--edge {
  border-color: rgba(44, 119, 126, 0.18);
  background: rgba(226, 246, 243, 0.46);
  color: #4f5a56;
}

.inspector-close {
  flex: 0 0 auto;
  appearance: none;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid rgba(143, 165, 156, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.54);
  color: var(--muted);
  cursor: pointer;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
}

.inspector-close:hover {
  border-color: rgba(22, 140, 124, 0.24);
  background: rgba(255, 255, 255, 0.78);
  color: var(--detail-teal-active);
}

.inspector-full-links-button {
  flex: 0 0 auto;
  max-width: 178px;
  min-height: 28px;
  border-color: rgba(143, 165, 156, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  color: rgba(55, 78, 76, 0.82);
  box-shadow:
    0 6px 14px rgba(15, 23, 42, 0.055),
    inset 0 1px 0 rgba(255, 255, 255, 0.64);
  backdrop-filter: blur(12px) saturate(112%);
  font-size: 12px;
  font-weight: 900;
}

.inspector-full-links-button:hover,
.inspector-full-links-button.active {
  border-color: rgba(22, 140, 124, 0.24);
  background: rgba(255, 255, 255, 0.78);
  color: var(--detail-teal-active);
}

.inspector-full-links-button :deep(span) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inspector-grid {
  display: grid;
  box-sizing: border-box;
  width: 100%;
  gap: 5px;
}

.detail-row {
  display: grid;
  box-sizing: border-box;
  grid-template-columns: minmax(96px, 0.4fr) minmax(0, 1fr);
  width: 100%;
  gap: 10px;
  align-items: start;
  padding: 8px 0;
  border-bottom: 1px solid rgba(143, 165, 156, 0.085);
}

.detail-row:last-child {
  border-bottom: 0;
}

.detail-label {
  color: rgba(82, 99, 96, 0.64);
  font-size: 12px;
  font-weight: 900;
}

.detail-value {
  min-width: 0;
  overflow: hidden;
  color: rgba(28, 41, 48, 0.9);
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inspector-actions {
  display: grid;
  box-sizing: border-box;
  grid-template-columns: 1fr;
  width: 100%;
  gap: 9px;
  padding-top: 8px;
}

.inspector-action {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 35px;
  margin-left: 0 !important;
  border-radius: 11px;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
  text-align: center;
}

.inspector-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.inspector-action :deep(span) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
  text-align: center;
  white-space: normal;
}

.inspector-action--primary {
  --el-button-bg-color: var(--detail-teal);
  --el-button-border-color: var(--detail-teal);
  --el-button-hover-bg-color: var(--detail-teal-hover);
  --el-button-hover-border-color: var(--detail-teal-hover);
  --el-button-active-bg-color: var(--detail-teal-active);
  --el-button-active-border-color: var(--detail-teal-active);
  background: linear-gradient(135deg, rgba(143, 165, 156, 0.96), var(--detail-teal));
  border-color: rgba(95, 125, 112, 0.78);
  color: var(--surface);
  box-shadow:
    0 8px 16px rgba(95, 125, 112, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.inspector-helper {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  margin-top: 9px;
  padding: 8px 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(22, 140, 124, 0.115);
  border-radius: 12px;
  background:
    linear-gradient(135deg, rgba(236, 249, 247, 0.62), rgba(255, 255, 255, 0.5));
  color: rgba(43, 83, 79, 0.8);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.35;
  text-align: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.52);
}

.inspector-helper-text {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  text-align: center;
}

.inspector-helper-action {
  flex: 0 0 auto;
  min-height: 28px;
  margin-left: auto;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 900;
}

.inspector-empty {
  box-sizing: border-box;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 18px 8px;
  color: var(--muted);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.5;
  text-align: center;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 6px 0 7px;
}

.table-title {
  font-size: 14px;
  font-weight: 900;
}

.table-subtitle {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.detail-table {
  border-radius: 11px;
  overflow: hidden;
}

:deep(.detail-table th.el-table__cell),
:deep(.detail-table td.el-table__cell) {
  text-align: center;
  vertical-align: middle;
  padding: 6px 0;
}

:deep(.detail-table th.el-table__cell > .cell),
:deep(.detail-table td.el-table__cell > .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  line-height: 1.35;
  text-align: center;
}

:deep(.detail-table .detail-table-row--related td.el-table__cell) {
  background: rgba(216, 243, 234, 0.42) !important;
}

.network-peak-link {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  min-height: 20px;
  margin: 0;
  padding: 0 4px;
  overflow: hidden;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #237b76;
  cursor: pointer;
  font: inherit;
  font-size: inherit;
  font-weight: 800;
  line-height: inherit;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.network-peak-link:hover {
  background: color-mix(in srgb, var(--nav-active-bg) 52%, transparent);
  color: #6f887d;
  box-shadow: 0 0 0 1px var(--nav-active-border) inset;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.network-peak-link:focus-visible {
  outline: 2px solid var(--nav-active-border);
  outline-offset: 2px;
}

:global(.network-peak-detail-popper.el-popover.el-popper) {
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border-brand);
  border-radius: 14px;
  box-shadow: 0 18px 42px rgba(18, 24, 38, 0.18);
}

.network-peak-card {
  background: var(--surface);
}

.network-peak-card-head {
  padding: 15px 16px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(135deg, var(--nav-active-bg), #ffffff);
}

.network-peak-card-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.network-peak-kicker {
  color: var(--brand-primary-3);
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
}

.network-peak-detail-button {
  appearance: none;
  padding: 5px 16px;
  border: 1px solid var(--brand-primary-3);
  border-radius: 999px;
  background: var(--brand-primary-3);
  color: #ffffff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  transition: opacity 0.15s;
}

.network-peak-detail-button:hover:not(:disabled) {
  opacity: 0.85;
}

.network-peak-detail-button:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.network-peak-title {
  margin-top: 4px;
  overflow-wrap: anywhere;
  color: var(--text);
  font-size: 20px;
  font-weight: 950;
  line-height: 1.15;
}

.network-peak-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.network-peak-pill {
  max-width: 100%;
  padding: 5px 8px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #ffffffc9;
  color: var(--text);
  font-size: 11px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.network-peak-card-body {
  padding: 14px 16px 16px;
}

.network-peak-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.network-peak-grid > div,
.network-peak-metrics > div {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface-2);
}

.network-peak-grid-wide {
  grid-column: 1 / -1;
}

.network-peak-grid span,
.network-peak-metrics span {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 900;
}

.network-peak-grid strong,
.network-peak-metrics strong {
  display: block;
  overflow-wrap: anywhere;
  color: var(--text);
  font-size: 13px;
  font-weight: 900;
}

.network-peak-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
  text-align: center;
}

.empty-state,
.state-message {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 112px;
  padding: 18px;
  border: 1px dashed var(--border);
  border-radius: 14px;
  background: var(--surface-2);
  color: var(--muted);
  font-weight: 800;
  text-align: center;
}

.state-message--error {
  color: #8b3f3f;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
}

.pager :deep(.el-pagination) {
  --el-color-primary: var(--detail-teal);
  --el-pagination-hover-color: var(--detail-teal-active);
  --el-pagination-button-bg-color: var(--surface);
  --el-pagination-button-disabled-bg-color: var(--surface-2);
}

:global(.full-links-dialog.el-dialog) {
  max-width: calc(100vw - 28px);
}

:global(.full-links-dialog.el-dialog) .el-dialog__header {
  padding-right: 48px;
}

:global(.full-links-dialog.el-dialog) .el-dialog__title {
  color: rgba(27, 40, 45, 0.92);
  font-weight: 900;
}

.full-links-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.full-links-subtitle {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.full-links-error {
  min-height: 86px;
}

.full-links-pager {
  padding-top: 12px;
}

@keyframes inspector-fade-slide {
  from {
    opacity: 0;
    transform: translateY(5px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 760px) {
  .section-head {
    flex-direction: column;
  }

  .section-tags {
    justify-content: flex-start;
  }

  .control-field,
  .control-field--grow {
    flex-basis: 100%;
    width: 100%;
    min-width: 0;
  }

  .network-control-card {
    padding: 13px;
    border-radius: 18px;
  }

  .mode-switch {
    width: 100%;
    min-width: 0;
  }

  .mode-button {
    flex: 1;
    min-width: 0;
    padding-right: 16px;
    padding-left: 16px;
  }

  .section-button,
  .query-panel .reset-button {
    flex: 1 1 140px;
  }

  .network-browser {
    grid-template-columns: 1fr;
  }

  .inspector-panel {
    max-width: none;
    height: auto;
    max-height: none;
    overflow: hidden;
  }

  .graph-body {
    height: 480px;
    min-height: 480px;
    max-height: 520px;
  }

  .detail-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .inspector-full-links-button {
    align-self: flex-start;
    max-width: 100%;
    margin-bottom: 10px;
  }

  .full-links-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>

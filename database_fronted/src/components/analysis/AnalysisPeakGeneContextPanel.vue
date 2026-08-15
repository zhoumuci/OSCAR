<template>
  <div class="pgc-root">
    <p class="pgc-desc">
      Match a peak set and a gene set against OSCAR Peak-to-Gene links, with an optional requirement that both ends are markers.
    </p>

    <div class="pgc-workbench">
      <div class="pgc-main">
        <div class="pgc-input-row">
          <div class="cte-card pgc-input-card">
            <div class="cte-card-title">Peak set input <span class="cte-max-badge">MAX input: 100 regions</span></div>
            <p class="cte-hint">Paste BED regions or chr:start-end coordinates, one region per line.</p>
            <textarea
              v-model="peakInput"
              class="cte-textarea"
              :class="{ 'cte-textarea--error': peakInputError }"
              rows="5"
              placeholder="chr1:10000-10500&#10;chr2:20000-20500&#10;chr5:150727553-150727794"
              :disabled="loading"
            ></textarea>
            <div v-if="peakInputError" class="input-feedback-card" role="alert">
              <span class="input-feedback-card__icon" aria-hidden="true">!</span>
              <span class="input-feedback-card__body"><strong>Check your peak set</strong><span>{{ peakInputError }}</span></span>
            </div>
            <div class="cte-btn-row">
              <span class="pgc-upload-wrap">
                <button type="button" class="soft-btn" :disabled="loading" @click="peakFileRef?.click()">Upload BED</button>
                <el-tooltip placement="top" effect="light" :show-after="200">
                  <template #content><div><div>Accepted files: .bed and .txt.</div><div>Each row must contain chr:start-end or the first three BED columns: chromosome, start, and end.</div></div></template>
                  <span class="pgc-help-icon">?</span>
                </el-tooltip>
              </span>
              <button type="button" class="soft-btn" :disabled="loading" @click="loadBrainSample">Load sample</button>
              <button type="button" class="soft-btn" :disabled="loading" @click="peakInput = ''">Clear</button>
            </div>
            <div class="pgc-stat-row">
              <div class="pgc-stat pgc-stat--muted"><span class="pgc-stat-num">{{ peakStats.input }}</span><span class="pgc-stat-label">Input peaks</span></div>
              <div class="pgc-stat"><span class="pgc-stat-num">{{ peakStats.valid }}</span><span class="pgc-stat-label">Valid peaks</span></div>
              <div class="pgc-stat pgc-stat--bad"><span class="pgc-stat-num">{{ peakStats.invalid }}</span><span class="pgc-stat-label">Invalid lines</span></div>
            </div>
            <input ref="peakFileRef" type="file" accept=".bed,.txt" class="pgc-hidden-input" @change="onPeakFileSelected" />
          </div>

          <div class="cte-card pgc-input-card">
            <div class="cte-card-title">Gene set input <span class="cte-max-badge">MAX input: 100 genes</span></div>
            <p class="cte-hint">Paste human gene symbols, one per line or separated by comma or space.</p>
            <textarea
              v-model="geneInput"
              class="cte-textarea"
              :class="{ 'cte-textarea--error': geneInputError }"
              rows="5"
              placeholder="MS4A1&#10;CD79A&#10;IL7R&#10;CCR7&#10;CD3D"
              :disabled="loading"
            ></textarea>
            <div v-if="geneInputError" class="input-feedback-card" role="alert">
              <span class="input-feedback-card__icon" aria-hidden="true">!</span>
              <span class="input-feedback-card__body"><strong>Check your gene set</strong><span>{{ geneInputError }}</span></span>
            </div>
            <div class="cte-btn-row">
              <span class="pgc-upload-wrap">
                <button type="button" class="soft-btn" :disabled="loading" @click="geneFileRef?.click()">Upload file</button>
                <el-tooltip placement="top" effect="light" :show-after="200">
                  <template #content><div><div>Accepted files: .txt and .csv.</div><div>Gene symbols may appear one per line or in delimited cells. Duplicate symbols are removed before matching.</div></div></template>
                  <span class="pgc-help-icon">?</span>
                </el-tooltip>
              </span>
              <button type="button" class="soft-btn" :disabled="loading" @click="loadBrainSample">Load sample</button>
              <button type="button" class="soft-btn" :disabled="loading" @click="geneInput = ''">Clear</button>
            </div>
            <div class="pgc-stat-row">
              <div class="pgc-stat pgc-stat--muted"><span class="pgc-stat-num">{{ geneStats.input }}</span><span class="pgc-stat-label">Input genes</span></div>
              <div class="pgc-stat"><span class="pgc-stat-num">{{ geneStats.valid }}</span><span class="pgc-stat-label">Valid genes</span></div>
              <div class="pgc-stat pgc-stat--bad"><span class="pgc-stat-num">{{ geneStats.invalid }}</span><span class="pgc-stat-label">Invalid tokens</span></div>
            </div>
            <input ref="geneFileRef" type="file" accept=".txt,.csv" class="pgc-hidden-input" @change="onGeneFileSelected" />
          </div>
        </div>

        <div class="cte-card pgc-engine-card">
          <div class="pgc-engine-head">
            <div class="cte-card-title">OSCAR Peak-to-Gene matching engine</div>
            <button type="button" class="pgc-advanced-toggle" :disabled="loading" @click="advancedOpen = !advancedOpen">
              <span :class="{ open: advancedOpen }">›</span> Advanced settings
            </button>
          </div>

          <div class="cte-fields cte-fields--grid">
            <label class="cte-field">
              <span class="cte-field-label">Tissue <em>required</em></span>
              <el-select
                v-model="tissue"
                class="cte-select"
                popper-class="oscar-select-popper"
                size="small"
                filterable
                placeholder="Select tissue"
                :loading="tissueLoading"
                :disabled="loading"
              >
                <el-option v-for="option in tissueOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </label>

            <label class="cte-field">
              <span class="cte-field-label">Dataset <small>optional</small></span>
              <el-select
                v-model="datasetId"
                class="cte-select"
                popper-class="oscar-select-popper"
                size="small"
                filterable
                clearable
                placeholder="All datasets in selected tissue"
                :loading="datasetLoading"
                :disabled="loading || !tissue"
              >
                <el-option
                  v-for="option in datasetOptions"
                  :key="option.dataset_id"
                  :label="`${option.dataset_id} · ${option.sample_name}`"
                  :value="option.dataset_id"
                />
              </el-select>
            </label>

            <label class="cte-field">
              <span class="cte-field-label pgc-label-with-help">
                Reference mode
                <el-tooltip placement="top" effect="light" :show-after="200">
                  <template #content>
                    <div class="pgc-tooltip-copy">
                      <div><strong>Peak-to-Gene links only:</strong> searches the original P2G table using the submitted genes, selected tissue or dataset, and overlapping submitted peak regions.</div>
                      <div><strong>Peak-to-Gene links + marker:</strong> applies the same P2G search, then keeps only links whose peak and gene are both markers in the same sample and cell type or cluster.</div>
                    </div>
                  </template>
                  <span class="pgc-help-icon pgc-help-icon--inline">?</span>
                </el-tooltip>
              </span>
              <el-select v-model="referenceMode" class="cte-select" popper-class="oscar-select-popper" size="small" :disabled="loading">
                <el-option label="Peak-to-Gene links only" value="p2g_only" />
                <el-option label="Peak-to-Gene links + marker" value="p2g_markers" />
              </el-select>
            </label>

            <label class="cte-field">
              <span class="cte-field-label">Result type</span>
              <el-select v-model="resultType" class="cte-select" popper-class="oscar-select-popper" size="small" :disabled="loading">
                <el-option label="General" value="general" />
                <el-option v-if="referenceMode === 'p2g_markers'" label="Cell type" value="cell_type" />
              </el-select>
            </label>
          </div>

          <div v-show="advancedOpen" class="cte-advanced">
            <label class="cte-field">
              <span class="cte-field-label">Minimum overlap (bp)</span>
              <el-input-number v-model="minOverlapBp" class="cte-number" size="small" :min="1" :disabled="loading" />
            </label>
            <label class="cte-field">
              <span class="cte-field-label">Maximum returned records</span>
              <el-input-number v-model="maxReturnedLinks" class="cte-number" size="small" :min="1" placeholder="All" :disabled="loading" />
              <small class="cte-field-hint">Leave empty to return all matched results.</small>
            </label>
          </div>

          <div class="cte-card-actions">
            <button type="button" class="primary-btn" :disabled="loading || hasBlockingInputError" @click="runAnalysis">
              <span v-if="loading" class="btn-spinner"></span>{{ loading ? "Running…" : "Run analysis" }}
            </button>
            <button type="button" class="soft-btn" :disabled="loading" @click="resetAll">Reset</button>
          </div>

          <div v-if="loading" class="pgc-progress-card" role="status" aria-live="polite">
            <div class="pgc-progress-head">
              <div>
                <div class="pgc-progress-stage">{{ progressStageLabel }}</div>
                <div class="pgc-progress-message">{{ progressMessage }}</div>
              </div>
              <span class="pgc-progress-value">{{ jobProgress }}%</span>
            </div>
            <div class="pgc-progress-track" :class="{ indeterminate: loading && jobProgress < 100 }">
              <div class="pgc-progress-fill" :style="{ width: `${jobProgress}%` }"></div>
            </div>
            <div class="pgc-progress-steps">
              <span :class="{ done: jobProgress >= 8, active: progressStage === 'VALIDATING' }">Validate inputs</span>
              <span :class="{ done: jobProgress >= 22, active: progressStage === 'QUERYING_CANDIDATES' }">Indexed candidates</span>
              <span :class="{ done: jobProgress >= 45, active: progressStage === 'INTERSECTING_REGIONS' }">bedtools overlap</span>
              <span :class="{ done: jobProgress >= 100, active: resultBuildingStages.has(progressStage) }">Build results</span>
            </div>
          </div>
        </div>
      </div>

      <div class="pgc-side">
        <div class="cte-card pgc-illustration">
          <div class="cte-card-title">Workflow overview</div>
          <img :src="baseUrl + 'images/P2G-link.jpg'" alt="Peak-to-Gene linkage analysis" class="pgc-slot-img" />
        </div>
        <div class="cte-card cte-how-card">
          <div class="cte-card-title">How it works</div>
          <div class="how-steps">
            <div class="how-step"><span class="how-num">1</span><div class="how-step-body"><strong>Select tissue</strong><span>Optionally narrow the analysis to one dataset in that tissue.</span></div></div>
            <div class="how-step"><span class="how-num">2</span><div class="how-step-body"><strong>Find matching P2G links</strong><span>Use the submitted genes and selected data scope to retrieve candidate P2G links.</span></div></div>
            <div class="how-step"><span class="how-num">3</span><div class="how-step-body"><strong>Intersect peak regions</strong><span>Use bedtools to apply the requested minimum overlap.</span></div></div>
            <div class="how-step"><span class="how-num">4</span><div class="how-step-body"><strong>Build results</strong><span>Return unique P2G links in General mode or count every matched cell-type row in Cell type mode.</span></div></div>
          </div>
          <p class="how-note">
            Marker mode requires marker support at both ends. Marker peak, expression-marker, and gene-score labels remain visible annotations in General results.
          </p>
        </div>
      </div>
    </div>

    <div v-if="result" class="pgc-results">
      <div class="pgc-summary-row">
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result.summary.totalPairs) }}</span><span class="sum-label">{{ resultTypeAtRun === 'general' ? 'P2G links' : 'Matched records' }}</span></div>
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result.summary.uniqueDatasets) }}</span><span class="sum-label">Datasets</span></div>
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result.summary.uniquePeaks) }}</span><span class="sum-label">Linked peaks</span></div>
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result.summary.uniqueGenes) }}</span><span class="sum-label">Linked genes</span></div>
        <div v-if="resultTypeAtRun === 'cell_type'" class="cte-summary-card"><span class="sum-num">{{ fmt(result.summary.uniqueCellTypes) }}</span><span class="sum-label">Cell types</span></div>
      </div>

      <div v-if="resultTypeAtRun === 'cell_type' && result.summary.topCellType" class="pgc-result-top-cell">
        <span class="pgc-top-label">Top cell type:</span>
        <strong>{{ result.summary.topCellType }}</strong>
        <span class="pgc-top-count">{{ fmt(result.summary.topCellTypeEvidence) }} matched records</span>
      </div>

      <div class="cte-tabs-row">
        <div class="cte-tabs">
          <button v-for="tab in resultTabs" :key="tab.key" type="button" class="cte-tab" :class="{ active: resTab === tab.key }" @click="resTab = tab.key">
            {{ tab.label }}
            <el-tooltip v-if="tab.tipLines" placement="top" effect="light" :show-after="300">
              <template #content><div class="pgc-tooltip-copy"><div v-for="line in tab.tipLines" :key="line">{{ line }}</div></div></template>
              <span class="pgc-tab-help">?</span>
            </el-tooltip>
          </button>
        </div>
        <el-tooltip :content="resTab === 'network' ? 'Download network' : resTab === 'table' ? 'Download all returned records as CSV' : 'Download displayed chart as PNG'" placement="top" effect="light">
          <button type="button" class="annotation-download-button" @click="downloadActiveResult"><el-icon><Download /></el-icon></button>
        </el-tooltip>
      </div>

      <div class="pgc-res-content">
        <div v-show="resTab === 'cell_chart'" ref="cellChartEl" class="pgc-chart"></div>
        <div v-show="resTab === 'bubble'" ref="bubbleChartEl" class="pgc-chart"></div>
        <div v-show="resTab === 'network'" class="pgc-network-view">
          <div ref="networkChartEl" class="pgc-chart pgc-chart--tall"></div>
        </div>

        <div v-show="resTab === 'table'" class="cte-table-wrap">
          <table class="cte-table">
            <thead>
              <tr>
                <th>Peak</th>
                <th class="gsc-sort-th" @click="togglePairSort('geneName')">Gene <span class="gsc-sort-arrow">{{ pairSortArrow('geneName') }}</span></th>
                <th v-if="resultTypeAtRun === 'cell_type'" class="gsc-sort-th" @click="togglePairSort('cellType')">Cell type <span class="gsc-sort-arrow">{{ pairSortArrow('cellType') }}</span></th>
                <th class="gsc-sort-th" @click="togglePairSort('datasetId')">Dataset <span class="gsc-sort-arrow">{{ pairSortArrow('datasetId') }}</span></th>
                <th class="gsc-sort-th" @click="togglePairSort('linkScore')">Link score <span class="gsc-sort-arrow">{{ pairSortArrow('linkScore') }}</span></th>
                <th class="gsc-sort-th" @click="togglePairSort('linkFdr')">Link FDR <span class="gsc-sort-arrow">{{ pairSortArrow('linkFdr') }}</span></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!paginatedPairs.length"><td :colspan="resultTypeAtRun === 'cell_type' ? 6 : 5" class="cte-no-data">No matching links found.</td></tr>
              <tr v-for="(row, rowIndex) in paginatedPairs" :key="(pairPage - 1) * PAIR_PAGE_SIZE + rowIndex">
                <td>
                  <div class="pgc-feature-cell">
                    <div class="pgc-feature-line">
                      <strong class="pgc-feature-value">{{ row.chromosome }}:{{ row.peakStart }}-{{ row.peakEnd }}</strong>
                      <span v-if="row.hasMarkerPeak" class="pgc-marker-stack">
                        <span class="pgc-marker-badge pgc-marker-badge--peak">Marker peak</span>
                      </span>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="pgc-feature-cell">
                    <div class="pgc-feature-line">
                      <strong class="pgc-feature-value pgc-feature-value--gene">{{ row.geneName }}</strong>
                      <span v-if="row.geneMarkerTypes?.length" class="pgc-marker-stack">
                        <span
                          v-for="markerType in row.geneMarkerTypes"
                          :key="markerType"
                          class="pgc-marker-badge"
                          :class="markerBadgeClass(markerType)"
                        >{{ markerTypeLabel(markerType) }}</span>
                      </span>
                    </div>
                  </div>
                </td>
                <td v-if="resultTypeAtRun === 'cell_type'">{{ row.cellType }}</td>
                <td><code>{{ row.datasetId }}</code></td>
                <td>{{ formatDecimal(row.linkScore) }}</td>
                <td>{{ formatScientific(row.linkFdr) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="pairTotalPages > 1" class="cte-pagination">
            <button type="button" class="cte-page-btn" :disabled="pairPage <= 1" @click="setPairPage(1)">« First</button>
            <button type="button" class="cte-page-btn" :disabled="pairPage <= 1" @click="setPairPage(pairPage - 1)">‹ Prev</button>
            <span class="cte-page-info">Page <input v-model="pairPageInput" type="number" class="cte-page-jump" min="1" :max="pairTotalPages" @keyup.enter="goToPairPage" /> / {{ pairTotalPages }}</span>
            <button type="button" class="cte-page-btn" @click="goToPairPage">Go</button>
            <button type="button" class="cte-page-btn" :disabled="pairPage >= pairTotalPages" @click="setPairPage(pairPage + 1)">Next ›</button>
            <button type="button" class="cte-page-btn" :disabled="pairPage >= pairTotalPages" @click="setPairPage(pairTotalPages)">Last »</button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="networkDownloadDialogOpen"
      width="640px"
      title="Download network"
      custom-class="bubble-dialog pgc-download-dialog"
      modal-class="bubble-overlay"
      :append-to-body="true"
    >
      <div class="landscape-download-body">
        <div class="landscape-download-meta">
          <span>Peak-to-Gene network</span>
        </div>
        <div class="landscape-download-grid">
          <button
            type="button"
            class="landscape-download-chip"
            :class="{ 'landscape-download-chip--loading': activeNetworkDownloadAction === 'image' }"
            :disabled="activeNetworkDownloadAction !== null"
            @click="runNetworkDownload('image', downloadNetworkImage)"
          >
            <span class="landscape-download-chip-left">
              <span class="landscape-download-chip-name">Current network image</span>
              <span class="landscape-download-chip-format">PNG · Current view</span>
            </span>
            <span class="landscape-download-chip-action" aria-live="polite">
              <span v-if="activeNetworkDownloadAction === 'image'" class="landscape-download-spinner" aria-hidden="true" />
              {{ activeNetworkDownloadAction === 'image' ? 'Starting...' : 'Download' }}
            </span>
          </button>
          <button
            type="button"
            class="landscape-download-chip"
            :class="{ 'landscape-download-chip--loading': activeNetworkDownloadAction === 'current' }"
            :disabled="activeNetworkDownloadAction !== null"
            @click="runNetworkDownload('current', downloadCurrentNetworkCsv)"
          >
              <span class="landscape-download-chip-left">
                <span class="landscape-download-chip-name">Current network data</span>
                <span class="landscape-download-chip-format">CSV · Top {{ result?.networkData.peakLimitPerGene ?? 30 }} Peaks/Gene</span>
            </span>
            <span class="landscape-download-chip-action" aria-live="polite">
              <span v-if="activeNetworkDownloadAction === 'current'" class="landscape-download-spinner" aria-hidden="true" />
              {{ activeNetworkDownloadAction === 'current' ? 'Starting...' : 'Download' }}
            </span>
          </button>
          <button
            type="button"
            class="landscape-download-chip"
            :class="{ 'landscape-download-chip--loading': activeNetworkDownloadAction === 'full' }"
            :disabled="activeNetworkDownloadAction !== null"
            @click="runNetworkDownload('full', downloadFullNetworkCsv)"
          >
            <span class="landscape-download-chip-left">
              <span class="landscape-download-chip-name">Full network data</span>
              <span class="landscape-download-chip-format">CSV · All</span>
            </span>
            <span class="landscape-download-chip-action" aria-live="polite">
              <span v-if="activeNetworkDownloadAction === 'full'" class="landscape-download-spinner" aria-hidden="true" />
              {{ activeNetworkDownloadAction === 'full' ? 'Starting...' : 'Download' }}
            </span>
          </button>
        </div>
      </div>
      <template #footer>
        <el-button @click="networkDownloadDialogOpen = false">Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from "vue";
import { Download } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import {
  fetchPeakGeneContextDatasets,
  fetchPeakGeneContextJob,
  fetchPeakGeneContextTissues,
  submitPeakGeneContextJob,
  type PeakGeneContextDatasetOption,
  type PeakGeneContextJobResponse,
  type PeakGeneContextRequest,
  type PeakGeneContextResponse,
} from "@/api/analysis";
import { parseFileContent, parseGenes as parseGenesByFormat } from "@/utils/geneParser";

const baseUrl = import.meta.env.BASE_URL;
type ReferenceMode = PeakGeneContextRequest["referenceMode"];
type ResultType = PeakGeneContextRequest["resultType"];
type ResultTab = "table" | "cell_chart" | "bubble" | "network";
type NetworkNode = PeakGeneContextResponse["networkData"]["nodes"][number];

const peakInput = ref("");
const geneInput = ref("");
const MAX_P2G_PEAKS = 100;
const MAX_P2G_GENES = 100;
const tissue = ref("");
const datasetId = ref("");
const referenceMode = ref<ReferenceMode>("p2g_only");
const resultType = ref<ResultType>("general");
const minOverlapBp = ref(1);
const maxReturnedLinks = ref<number | null>(null);
const advancedOpen = ref(false);
const loading = ref(false);

const tissueOptions = ref<string[]>([]);
const datasetOptions = ref<PeakGeneContextDatasetOption[]>([]);
const tissueLoading = ref(false);
const datasetLoading = ref(false);
let datasetRequestGeneration = 0;

const result = ref<PeakGeneContextResponse | null>(null);
const resultTypeAtRun = ref<ResultType>("general");
const resTab = ref<ResultTab>("table");
type NetworkDownloadAction = "image" | "current" | "full";
const networkDownloadDialogOpen = ref(false);
const activeNetworkDownloadAction = ref<NetworkDownloadAction | null>(null);
let networkDownloadFeedbackTimer: number | undefined;
const pairPage = ref(1);
const pairPageInput = ref("1");
const PAIR_PAGE_SIZE = 10;
type PairSortColumn = "geneName" | "cellType" | "datasetId" | "linkScore" | "linkFdr";
const pairSortColumn = ref<PairSortColumn | null>(null);
const pairSortDirection = ref<"asc" | "desc">("desc");

const CELL_TYPE_CHART_COLORS = [
  "#E8A87C",
  "#8FC9B3",
  "#D9826B",
  "#7BA7C9",
  "#E8936B",
  "#C4A882",
  "#D4956A",
  "#8FB89C",
  "#C9876B",
  "#A8C8D8",
  "#EAB082",
  "#B8C9A0",
];
const BUBBLE_HEATMAP_COLORS = ["#A8C8D8", "#8FC9B3", "#F5D98B", "#E8A87C", "#D9826B"];
const NETWORK_CATEGORIES = [
  { key: "peak", name: "Peak", color: "#6f9f94" },
  { key: "gene", name: "Gene", color: "#8b7aa8" },
  { key: "cellType", name: "Cell type", color: "#d69a45" },
] as const;

const jobProgress = ref(0);
const progressStage = ref("IDLE");
const progressMessage = ref("Waiting to start.");
let pollGeneration = 0;

const peakFileRef = ref<HTMLInputElement | null>(null);
const geneFileRef = ref<HTMLInputElement | null>(null);
const peakUploadError = ref<string | null>(null);
const geneUploadError = ref<string | null>(null);

const cellChartEl = ref<HTMLDivElement | null>(null);
const bubbleChartEl = ref<HTMLDivElement | null>(null);
const networkChartEl = ref<HTMLDivElement | null>(null);
let cellChart: echarts.ECharts | null = null;
let bubbleChart: echarts.ECharts | null = null;
let networkChart: echarts.ECharts | null = null;

const resultBuildingStages = new Set(["ANNOTATING_MARKERS", "AGGREGATING_CELL_TYPES", "BUILDING_RESULTS", "COMPLETED"]);

const parsedPeaks = computed(() => parsePeaks(peakInput.value));
const parsedGenes = computed(() => parseGenes(geneInput.value));
const peakStats = computed(() => ({
  input: parsedPeaks.value.inputCount,
  valid: parsedPeaks.value.values.length,
  invalid: parsedPeaks.value.invalidCount,
}));
const geneStats = computed(() => ({
  input: parsedGenes.value.inputCount,
  valid: parsedGenes.value.values.length,
  invalid: parsedGenes.value.invalidCount,
}));
const inputLimitExceeded = computed(() =>
  parsedPeaks.value.values.length > MAX_P2G_PEAKS
  || parsedGenes.value.values.length > MAX_P2G_GENES,
);
const peakInputError = computed(() => {
  if (peakUploadError.value) return peakUploadError.value;
  if (parsedPeaks.value.values.length > MAX_P2G_PEAKS) return `Too many peak regions: ${parsedPeaks.value.values.length}. Maximum is ${MAX_P2G_PEAKS}.`;
  if (parsedPeaks.value.invalidCount) return "Remove invalid or duplicate peak lines before running the analysis.";
  return null;
});
const geneInputError = computed(() => {
  if (geneUploadError.value) return geneUploadError.value;
  if (parsedGenes.value.values.length > MAX_P2G_GENES) return `Too many genes: ${parsedGenes.value.values.length}. Maximum is ${MAX_P2G_GENES}.`;
  if (parsedGenes.value.invalidCount) return "Remove invalid or duplicate gene symbols before running the analysis.";
  return null;
});
const hasBlockingInputError = computed(() => inputLimitExceeded.value || Boolean(peakInputError.value) || Boolean(geneInputError.value));

const progressStageLabel = computed(() => ({
  QUEUED: "Queued",
  STARTING: "Starting analysis",
  VALIDATING: "Validating analysis inputs",
  QUERYING_CANDIDATES: "Retrieving indexed candidates",
  INTERSECTING_REGIONS: "Intersecting peak regions with bedtools",
  ANNOTATING_MARKERS: "Adding marker labels",
  AGGREGATING_CELL_TYPES: "Grouping matches by cell type",
  BUILDING_RESULTS: "Building result views",
  COMPLETED: "Analysis complete",
  FAILED: "Analysis failed",
} as Record<string, string>)[progressStage.value] ?? "Processing");

const returnLimitApplied = computed(() => Boolean(
  result.value && result.value.pairs.length < result.value.summary.totalPairs
));

const networkHelperLines = computed(() => {
  if (!result.value) return [
    "Shows no more than 30 Peak nodes for each Gene so a large result remains readable.",
    "Nodes are arranged from left to right and automatically spaced; you can drag nodes and zoom the graph.",
  ];
  const network = result.value.networkData;
  const recordLabel = resultTypeAtRun.value === "general" ? "P2G links" : "matched records";
  const lines = [
    `Shows no more than ${network.peakLimitPerGene} Peak nodes for each Gene. This view contains ${fmt(network.nodes.length)} unique nodes and ${fmt(network.edges.length)} unique connections.`,
    `Nodes are arranged from left to right as Peak → Gene${resultTypeAtRun.value === "cell_type" ? " → Cell type" : ""} and automatically spaced. You can drag nodes and zoom the graph.`,
    `The download menu can save this picture, the CSV rows used by the current graph, or all ${fmt(result.value.pairs.length)} returned ${recordLabel}.`,
  ];
  if (returnLimitApplied.value) lines.push(`The return limit kept ${fmt(result.value.pairs.length)} of ${fmt(result.value.summary.totalPairs)} matches.`);
  return lines;
});

const resultTabs = computed<Array<{ key: ResultTab; label: string; tipLines?: string[] }>>(() => {
  if (resultTypeAtRun.value === "general") {
    return [
      { key: "table", label: "Table" },
      { key: "network", label: "Peak–Gene network", tipLines: networkHelperLines.value },
    ];
  }
  return [
    { key: "table", label: "Table" },
    { key: "cell_chart", label: "Cell type chart", tipLines: [
      "Shows the 20 cell types with the most matched rows. Every matched row contributes once to its cell-type count.",
      "The percentage is that cell type's share of all matched rows, including cell types outside the displayed top 20.",
    ] },
    { key: "bubble", label: "Bubble heatmap", tipLines: [
      "Shows up to the top 15 cell types and top 20 genes, ranked by matched-row count.",
      "Bubble size is the cell type's share of matches for that gene. Colour is the cell type's share of all matched rows.",
    ] },
    { key: "network", label: "Network", tipLines: networkHelperLines.value },
  ];
});

const sortedPairs = computed(() => {
  const rows = [...(result.value?.pairs ?? [])];
  const column = pairSortColumn.value;
  if (!column) return rows;

  const direction = pairSortDirection.value === "desc" ? -1 : 1;
  rows.sort((left, right) => {
    if (column === "geneName" || column === "cellType" || column === "datasetId") {
      return direction * (left[column] ?? "").localeCompare(right[column] ?? "", undefined, { numeric: true, sensitivity: "base" });
    }

    const leftValue = left[column];
    const rightValue = right[column];
    if (leftValue == null && rightValue == null) return 0;
    if (leftValue == null) return 1;
    if (rightValue == null) return -1;
    return direction * (leftValue - rightValue);
  });
  return rows;
});

const pairTotalPages = computed(() => Math.max(1, Math.ceil(sortedPairs.value.length / PAIR_PAGE_SIZE)));
const paginatedPairs = computed(() => {
  const start = (pairPage.value - 1) * PAIR_PAGE_SIZE;
  return sortedPairs.value.slice(start, start + PAIR_PAGE_SIZE);
});

function setPairPage(page: number) {
  pairPage.value = Math.min(pairTotalPages.value, Math.max(1, page));
  pairPageInput.value = String(pairPage.value);
}

function goToPairPage() {
  const page = Number.parseInt(pairPageInput.value, 10);
  if (!Number.isFinite(page)) {
    pairPageInput.value = String(pairPage.value);
    return;
  }
  setPairPage(page);
}

function togglePairSort(column: PairSortColumn) {
  if (pairSortColumn.value === column) {
    if (pairSortDirection.value === "desc") {
      pairSortDirection.value = "asc";
    } else {
      pairSortColumn.value = null;
      pairSortDirection.value = "desc";
    }
  } else {
    pairSortColumn.value = column;
    pairSortDirection.value = "desc";
  }
  pairPage.value = 1;
}

function pairSortArrow(column: PairSortColumn) {
  if (pairSortColumn.value !== column) return "\u21C5";
  return pairSortDirection.value === "desc" ? "\u25BC" : "\u25B2";
}

watch(referenceMode, mode => {
  resultType.value = mode === "p2g_markers" ? "cell_type" : "general";
});

watch(peakInput, () => {
  peakUploadError.value = null;
  result.value = null;
});
watch(geneInput, () => {
  geneUploadError.value = null;
  result.value = null;
});

watch(tissue, async selectedTissue => {
  const generation = ++datasetRequestGeneration;
  datasetId.value = "";
  datasetOptions.value = [];
  if (!selectedTissue) return;
  datasetLoading.value = true;
  try {
    const options = await fetchPeakGeneContextDatasets(selectedTissue);
    if (generation === datasetRequestGeneration) datasetOptions.value = options;
  } catch (error) {
    if (generation === datasetRequestGeneration) ElMessage.error(errorMessage(error, "Failed to load datasets for the selected tissue."));
  } finally {
    if (generation === datasetRequestGeneration) datasetLoading.value = false;
  }
});

watch(resTab, async () => {
  await nextTick();
  renderActiveChart();
});

watch(pairTotalPages, total => {
  if (pairPage.value > total) setPairPage(total);
});

watch(pairPage, page => {
  pairPageInput.value = String(page);
});

onMounted(async () => {
  tissueLoading.value = true;
  try {
    tissueOptions.value = await fetchPeakGeneContextTissues();
  } catch (error) {
    ElMessage.error(errorMessage(error, "Failed to load Peak-to-Gene tissues."));
  } finally {
    tissueLoading.value = false;
  }
  window.addEventListener("resize", resizeCharts);
});

onActivated(() => nextTick(renderActiveChart));
onDeactivated(disposeCharts);
onBeforeUnmount(() => {
  pollGeneration++;
  datasetRequestGeneration++;
  window.removeEventListener("resize", resizeCharts);
  window.clearTimeout(networkDownloadFeedbackTimer);
  disposeCharts();
});

function parsePeaks(value: string) {
  const lines = value.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
  const values: Array<{ chrom: string; start: number; end: number }> = [];
  const seen = new Set<string>();
  let invalidCount = 0;
  for (const line of lines) {
    let chrom = "";
    let startText = "";
    let endText = "";
    const regionMatch = line.match(/^(chr[A-Za-z0-9_.-]+):(\d+)-(\d+)$/);
    if (regionMatch) {
      chrom = regionMatch[1] ?? "";
      startText = regionMatch[2] ?? "";
      endText = regionMatch[3] ?? "";
    } else {
      const columns = line.split(/\s+/);
      if (columns.length < 3) {
        invalidCount++;
        continue;
      }
      chrom = columns[0] ?? "";
      startText = columns[1] ?? "";
      endText = columns[2] ?? "";
    }
    const start = Number(startText);
    const end = Number(endText);
    const key = `${chrom}:${start}-${end}`;
    if (!/^chr[A-Za-z0-9_.-]+$/.test(chrom) || !Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start < 0 || start >= end || seen.has(key)) {
      invalidCount++;
      continue;
    }
    seen.add(key);
    values.push({ chrom, start, end });
  }
  return { inputCount: lines.length, invalidCount, values };
}

function parseGenes(value: string) {
  try {
    const structured = parseGenesByFormat(value);
    if (structured.format === "csv") {
      const duplicateCount = Math.max(
        0,
        structured.totalFound - structured.filteredOut.length - structured.genes.length,
      );
      return {
        inputCount: structured.totalFound,
        invalidCount: structured.filteredOut.length + duplicateCount,
        values: structured.genes,
      };
    }
  } catch {
    const tokens = value.split(/[\s,;]+/).map(token => token.trim()).filter(Boolean);
    return { inputCount: tokens.length, invalidCount: tokens.length, values: [] as string[] };
  }
  const tokens = value.split(/[\s,;]+/).map(token => token.trim()).filter(Boolean);
  const values: string[] = [];
  const seen = new Set<string>();
  let invalidCount = 0;
  for (const token of tokens) {
    const gene = token.toUpperCase();
    if (!/^[A-Z0-9][A-Z0-9._-]{0,127}$/.test(gene) || seen.has(gene)) {
      invalidCount++;
      continue;
    }
    seen.add(gene);
    values.push(gene);
  }
  return { inputCount: tokens.length, invalidCount, values };
}

function loadBrainSample() {
  peakInput.value = [
    "chr10:70493274-70493774",
    "chr5:150081813-150082313",
    "chr10:70478459-70478959",
    "chr10:70494887-70495387",
    "chr5:150086252-150086752",
    "chr10:70508704-70509204",
    "chr5:150065281-150065781",
    "chr5:150082903-150083403"
  ].join("\n");
  geneInput.value = ["PALD1", "CSF1R"].join("\n");
  datasetId.value = "";
  tissue.value = "Brain";
}

async function onPeakFileSelected(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  if (!/\.(?:bed|txt)$/i.test(file.name)) {
    peakUploadError.value = "Unsupported file type. Please choose a .bed or .txt file.";
    return;
  }
  peakInput.value = await file.text();
}

async function onGeneFileSelected(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) {
    if (!/\.(?:txt|csv)$/i.test(file.name)) {
      geneUploadError.value = "Unsupported file type. Please choose a .txt or .csv file.";
      input.value = "";
      return;
    }
    try {
      const parsed = parseFileContent(file.name, await file.text());
      geneInput.value = parsed.genes.join("\n");
    } catch (error) {
      ElMessage.warning(errorMessage(error, "Unable to read the gene file."));
    }
  }
  input.value = "";
}

function validateAnalysisInput() {
  if (!parsedPeaks.value.values.length) return "Enter at least one valid peak region.";
  if (parsedPeaks.value.invalidCount) return "Remove invalid or duplicate peak lines before running the analysis.";
  if (parsedPeaks.value.values.length > MAX_P2G_PEAKS) return `Peak set cannot exceed ${MAX_P2G_PEAKS} regions.`;
  if (!parsedGenes.value.values.length) return "Enter at least one valid gene symbol.";
  if (parsedGenes.value.invalidCount) return "Remove invalid or duplicate gene symbols before running the analysis.";
  if (parsedGenes.value.values.length > MAX_P2G_GENES) return `Gene set cannot exceed ${MAX_P2G_GENES} symbols.`;
  if (!tissue.value) return "Select a tissue before running the analysis.";
  return null;
}

function buildRequest(): PeakGeneContextRequest {
  return {
    peaks: parsedPeaks.value.values,
    genes: parsedGenes.value.values,
    tissue: tissue.value,
    datasetId: datasetId.value || null,
    referenceMode: referenceMode.value,
    resultType: resultType.value,
    advanced: {
      minOverlapBp: minOverlapBp.value,
      maxReturnedLinks: maxReturnedLinks.value,
    },
  };
}

function updateJobProgress(job: PeakGeneContextJobResponse) {
  jobProgress.value = Math.max(jobProgress.value, job.progress);
  progressStage.value = job.stage;
  progressMessage.value = job.message;
}

function waitForPoll(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms));
}

async function awaitJob(initial: PeakGeneContextJobResponse, generation: number) {
  let job = initial;
  updateJobProgress(job);
  while (job.status === "QUEUED" || job.status === "RUNNING") {
    await waitForPoll(Math.max(700, Math.min(job.pollAfterMs, 2000)));
    if (generation !== pollGeneration) return null;
    job = await fetchPeakGeneContextJob(job.jobId);
    updateJobProgress(job);
  }
  if (job.status === "FAILED") throw new Error(job.error ?? "Peak-to-Gene analysis failed.");
  if (!job.result) throw new Error("The Peak-to-Gene job completed without a result.");
  return job.result;
}

async function runAnalysis() {
  if (loading.value) return;
  const validationError = validateAnalysisInput();
  if (validationError) {
    ElMessage.warning(validationError);
    return;
  }

  const request = buildRequest();
  const generation = ++pollGeneration;
  loading.value = true;
  result.value = null;
  disposeCharts();
  jobProgress.value = 1;
  progressStage.value = "QUEUED";
  progressMessage.value = "Submitting the Peak-to-Gene linkage query.";

  try {
    const submitted = await submitPeakGeneContextJob(request);
    const completed = await awaitJob(submitted, generation);
    if (!completed || generation !== pollGeneration) return;
    result.value = completed;
    resultTypeAtRun.value = request.resultType;
    resTab.value = "table";
    pairPage.value = 1;
    pairSortColumn.value = null;
    pairSortDirection.value = "desc";
    jobProgress.value = 100;
    progressStage.value = "COMPLETED";
  } catch (error) {
    progressStage.value = "FAILED";
    progressMessage.value = errorMessage(error, "Peak-to-Gene analysis failed.");
    ElMessage.error(progressMessage.value);
  } finally {
    if (generation === pollGeneration) loading.value = false;
  }
}

function resetAll() {
  pollGeneration++;
  peakInput.value = "";
  geneInput.value = "";
  peakUploadError.value = null;
  geneUploadError.value = null;
  tissue.value = "";
  datasetId.value = "";
  referenceMode.value = "p2g_only";
  resultType.value = "general";
  minOverlapBp.value = 1;
  maxReturnedLinks.value = null;
  advancedOpen.value = false;
  result.value = null;
  resultTypeAtRun.value = "general";
  resTab.value = "table";
  pairPage.value = 1;
  pairSortColumn.value = null;
  pairSortDirection.value = "desc";
  jobProgress.value = 0;
  progressStage.value = "IDLE";
  progressMessage.value = "Waiting to start.";
  disposeCharts();
}

function renderActiveChart() {
  if (!result.value) return;
  if (resTab.value === "cell_chart") renderCellChart();
  if (resTab.value === "bubble") renderBubbleChart();
  if (resTab.value === "network") renderNetworkChart();
}

function renderCellChart() {
  if (!cellChartEl.value || !result.value) return;
  cellChart ??= echarts.init(cellChartEl.value);
  const rows = result.value.cellTypeResults.slice(0, 20);
  const totalEvidence = result.value.cellTypeResults.reduce((sum, row) => sum + row.evidenceCount, 0);
  cellChart.setOption({
    grid: { left: 170, right: 84, top: 24, bottom: 48 },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: (params: any[]) => {
        const item = params[0];
        const count = Number(item?.value ?? 0);
        const share = totalEvidence > 0 ? (count / totalEvidence) * 100 : 0;
        return `${item?.name ?? ""}<br/>Matched records: ${fmt(count)}<br/>Share: ${share.toFixed(1)}%`;
      },
    },
    xAxis: { type: "value", name: "Matched records", minInterval: 1 },
    yAxis: { type: "category", inverse: true, data: rows.map(row => row.cellType), axisLabel: { width: 150, overflow: "truncate" } },
    series: [{
      type: "bar",
      barMaxWidth: 24,
      label: {
        show: true,
        position: "right",
        color: "#5E6C67",
        fontSize: 10,
        fontWeight: 700,
        formatter: (params: any) => {
          const share = totalEvidence > 0 ? (Number(params.value) / totalEvidence) * 100 : 0;
          return `${share.toFixed(1)}%`;
        },
      },
      data: rows.map((row, index) => ({
        value: row.evidenceCount,
        itemStyle: {
          color: CELL_TYPE_CHART_COLORS[index % CELL_TYPE_CHART_COLORS.length],
          borderRadius: [0, 6, 6, 0]
        }
      }))
    }],
  }, true);
}

function renderBubbleChart() {
  if (!bubbleChartEl.value || !result.value) return;
  bubbleChart ??= echarts.init(bubbleChartEl.value);
  const cellTypes = result.value.cellTypeResults.slice(0, 15);
  const geneTotals = new Map<string, number>();
  for (const cellType of result.value.cellTypeResults) {
    for (const gene of cellType.geneDetails) geneTotals.set(gene.gene, (geneTotals.get(gene.gene) ?? 0) + gene.count);
  }
  const genes = [...geneTotals.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).slice(0, 20).map(entry => entry[0]);
  const geneIndex = new Map(genes.map((gene, index) => [gene, index]));
  const totalEvidence = result.value.cellTypeResults.reduce((sum, row) => sum + row.evidenceCount, 0);
  const data: Array<[number, number, number, number, number]> = [];
  cellTypes.forEach((cellType, cellIndex) => {
    cellType.geneDetails.forEach(gene => {
      const x = geneIndex.get(gene.gene);
      if (x !== undefined) {
        const geneShare = gene.count / Math.max(1, geneTotals.get(gene.gene) ?? 1) * 100;
        const cellTypeShare = cellType.evidenceCount / Math.max(1, totalEvidence) * 100;
        data.push([x, cellIndex, gene.count, geneShare, cellTypeShare]);
      }
    });
  });
  bubbleChart.setOption({
    grid: { left: 170, right: 45, top: 28, bottom: 100 },
    tooltip: {
      formatter: (params: any) => `${genes[params.value[0]] ?? ""}<br/>${cellTypes[params.value[1]]?.cellType ?? ""}<br/>Matched records: ${fmt(params.value[2])}<br/>Share for this gene: ${params.value[3].toFixed(1)}%<br/>Cell-type share overall: ${params.value[4].toFixed(1)}%`,
    },
    xAxis: { type: "category", data: genes, axisLabel: { rotate: 45 } },
    yAxis: { type: "category", inverse: true, data: cellTypes.map(row => row.cellType), axisLabel: { width: 150, overflow: "truncate" } },
    visualMap: {
      min: 0,
      max: 100,
      dimension: 4,
      orient: "horizontal",
      left: "center",
      bottom: 6,
      text: ["Higher cell-type share", "Lower"],
      calculable: false,
      inRange: { color: BUBBLE_HEATMAP_COLORS },
    },
    series: [{
      type: "scatter",
      data,
      symbolSize: (item: number[]) => 8 + 38 * Math.sqrt(Math.max(0, item[3] ?? 0) / 100),
      itemStyle: { borderColor: "#fff", borderWidth: 1.2, opacity: 0.9 },
      emphasis: { scale: 1.5, itemStyle: { opacity: 1, shadowBlur: 10, shadowColor: "rgba(0,0,0,0.2)" } }
    }],
  }, true);
}

function renderNetworkChart() {
  if (!networkChartEl.value || !result.value) return;
  networkChart ??= echarts.init(networkChartEl.value);
  const { nodes, edges } = result.value.networkData;
  const categories = NETWORK_CATEGORIES
    .filter(category => resultTypeAtRun.value === "cell_type" || category.key !== "cellType")
    .map(category => ({ name: category.name, itemStyle: { color: category.color } }));
  const chartWidth = Math.max(760, networkChartEl.value.clientWidth);
  const chartHeight = Math.max(680, networkChartEl.value.clientHeight);
  const geneNodes = nodes.filter(node => node.category === "gene");
  const geneY = new Map<string, number>(geneNodes.map((node, index) => [
    node.id,
    72 + ((chartHeight - 144) * (index + 0.5)) / Math.max(1, geneNodes.length),
  ] as const));
  const connectedGenes = new Map<string, Set<string>>();
  edges.forEach(edge => {
    if (geneY.has(edge.source)) {
      const genes = connectedGenes.get(edge.target) ?? new Set<string>();
      genes.add(edge.source);
      connectedGenes.set(edge.target, genes);
    }
    if (geneY.has(edge.target)) {
      const genes = connectedGenes.get(edge.source) ?? new Set<string>();
      genes.add(edge.target);
      connectedGenes.set(edge.source, genes);
    }
  });
  const nodesByCategory = new Map<string, NetworkNode[]>();
  nodes.forEach(node => {
    const group = nodesByCategory.get(node.category) ?? [];
    group.push(node);
    nodesByCategory.set(node.category, group);
  });
  const categoryX = resultTypeAtRun.value === "cell_type"
    ? { peak: 0.16, gene: 0.5, cellType: 0.84 }
    : { peak: 0.25, gene: 0.75, cellType: 0.9 };
  networkChart.setOption({
    tooltip: {
      formatter: (params: any) => {
        if (resultTypeAtRun.value === "cell_type") {
          return params.dataType === "edge"
            ? `Matched records: ${params.data.evidenceCount}`
            : `${params.data.name}<br>Matched records: ${params.data.value}`;
        }
        return params.dataType === "edge"
          ? `P2G link score: ${Number(params.data.weight).toFixed(3)}`
          : `${params.data.name}<br>Connected P2G links: ${params.data.value}`;
      },
    },
    legend: [{ data: categories.map(category => category.name) }],
    series: [{
      type: "graph",
      layout: "force",
      roam: true,
      draggable: true,
      categories,
      data: nodes.map(node => {
        const group = nodesByCategory.get(node.category) ?? [node];
        const groupIndex = group.findIndex(candidate => candidate.id === node.id);
        const verticalStep = (chartHeight - 120) / Math.max(1, group.length);
        const relatedGeneY = [...(connectedGenes.get(node.id) ?? [])]
          .map(geneId => geneY.get(geneId))
          .filter((value): value is number => value !== undefined);
        const anchorY = node.category === "gene"
          ? geneY.get(node.id)
          : relatedGeneY.length
            ? relatedGeneY.reduce((sum, value) => sum + value, 0) / relatedGeneY.length
            : 60 + verticalStep * (groupIndex + 0.5);
        const jitterSeed = [...node.id].reduce((hash, character) => (hash * 31 + character.charCodeAt(0)) >>> 0, 0);
        const verticalJitter = node.category === "gene" ? 0 : ((jitterSeed % 201) - 100);
        return {
          ...node,
          category: node.category === "peak" ? 0 : node.category === "gene" ? 1 : 2,
          symbolSize: 14 + Math.min(34, Math.sqrt(node.value) * 4),
          x: chartWidth * (categoryX[node.category as keyof typeof categoryX] ?? 0.5) + (groupIndex % 2 === 0 ? -14 : 14),
          y: Math.max(54, Math.min(chartHeight - 54, (anchorY ?? chartHeight / 2) + verticalJitter)),
          fixed: node.category === "gene",
        };
      }),
      links: edges.map(edge => ({
        ...edge,
        lineStyle: { width: 1 + Math.min(6, Math.log2(edge.evidenceCount + 1)), opacity: 0.55 },
      })),
      force: { repulsion: [380, 720], edgeLength: [150, 270], gravity: 0.018, layoutAnimation: true },
      label: { show: true, position: "right", distance: 5, fontSize: 10, color: "#39443f" },
      labelLayout: { hideOverlap: true, moveOverlap: "shiftY" },
      emphasis: { focus: "adjacency" },
    }],
  }, true);
}

function disposeCharts() {
  cellChart?.dispose();
  bubbleChart?.dispose();
  networkChart?.dispose();
  cellChart = null;
  bubbleChart = null;
  networkChart = null;
}

function resizeCharts() {
  cellChart?.resize();
  bubbleChart?.resize();
  networkChart?.resize();
}

function markerBadgeClass(signalType: string) {
  if (signalType === "gene_expression") return "pgc-marker-badge--expression";
  if (signalType === "gene_score") return "pgc-marker-badge--score";
  return "pgc-marker-badge--other";
}

function markerTypeLabel(signalType: string) {
  if (signalType === "gene_expression") return "Exp";
  if (signalType === "gene_score") return "Score";
  return `${signalType.replace(/_/g, " ")} marker`;
}

function formatDecimal(value: number | null) {
  return value == null ? "—" : value.toFixed(3);
}

function formatScientific(value: number | null) {
  return value == null ? "—" : value.toExponential(2);
}

function fmt(value: number) {
  return value.toLocaleString();
}

function errorMessage(error: unknown, fallback: string) {
  const candidate = error as any;
  return candidate?.response?.data?.detail
    ?? candidate?.response?.data?.message
    ?? candidate?.response?.data?.error
    ?? candidate?.message
    ?? fallback;
}

function downloadActiveResult() {
  if (!result.value) return;
  if (resTab.value === "network") {
    networkDownloadDialogOpen.value = true;
    return;
  }
  if (resTab.value !== "table") {
    const chart = resTab.value === "cell_chart" ? cellChart : bubbleChart;
    if (!chart) return;
    const anchor = document.createElement("a");
    anchor.href = chart.getDataURL({ type: "png", pixelRatio: 2, backgroundColor: "#ffffff" });
    anchor.download = `oscar_p2g_${resTab.value}_${new Date().toISOString().slice(0, 10)}.png`;
    anchor.click();
    return;
  }

  downloadResultCsv("oscar_p2g_links", result.value.pairs);
}

async function runNetworkDownload(
  action: NetworkDownloadAction,
  download: () => boolean | Promise<boolean>
) {
  if (activeNetworkDownloadAction.value !== null) return;
  activeNetworkDownloadAction.value = action;
  await nextTick();
  const startedAt = performance.now();
  let started = false;
  try {
    started = await download();
  } finally {
    const remaining = Math.max(0, 900 - (performance.now() - startedAt));
    window.clearTimeout(networkDownloadFeedbackTimer);
    networkDownloadFeedbackTimer = window.setTimeout(() => {
      activeNetworkDownloadAction.value = null;
      if (started) networkDownloadDialogOpen.value = false;
      networkDownloadFeedbackTimer = undefined;
    }, remaining);
  }
}

function downloadNetworkImage() {
  if (!networkChart) return false;
  const anchor = document.createElement("a");
  anchor.href = networkChart.getDataURL({ type: "png", pixelRatio: 2, backgroundColor: "#ffffff" });
  anchor.download = `oscar_p2g_network_${new Date().toISOString().slice(0, 10)}.png`;
  anchor.click();
  return true;
}

function downloadCurrentNetworkCsv() {
  if (!result.value) return false;
  const displayedNodeIds = new Set(result.value.networkData.nodes.map(node => node.id));
  const displayedLinks = new Set(
    result.value.networkData.edges
      .filter(edge => edge.source.startsWith("p_") && edge.target.startsWith("g_"))
      .map(edge => `${edge.source}\u0000${edge.target}`)
  );
  const rows = result.value.pairs.filter(row => {
    if (!displayedLinks.has(`p_${row.peakName}\u0000g_${row.geneName}`)) return false;
    return resultTypeAtRun.value !== "cell_type"
      || (row.cellType != null && displayedNodeIds.has(`c_${row.cellType}`));
  });
  downloadResultCsv("oscar_p2g_network_current", rows);
  return true;
}

function downloadFullNetworkCsv() {
  if (!result.value) return false;
  downloadResultCsv("oscar_p2g_network_full", result.value.pairs);
  return true;
}

function downloadResultCsv(
  fileStem: string,
  sourceRows: PeakGeneContextResponse["pairs"]
) {
  if (!result.value) return;
  const includeCellType = resultTypeAtRun.value === "cell_type";
  const header = [
    "Peak region",
    "Peak name",
    "Marker peak",
    "Gene",
    "Gene marker types",
    ...(includeCellType ? ["Cell type"] : []),
    "Dataset",
    "Link score",
    "Link FDR",
  ];
  const rows = sourceRows.map(row => [
    `${row.chromosome}:${row.peakStart}-${row.peakEnd}`,
    row.peakName,
    row.hasMarkerPeak ? "yes" : "no",
    row.geneName,
    row.geneMarkerTypes.join("|"),
    ...(includeCellType ? [row.cellType ?? ""] : []),
    row.datasetId,
    row.linkScore ?? "",
    row.linkFdr ?? "",
  ]);
  const csv = [header, ...rows].map(row => row.map(csvCell).join(",")).join("\n");
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${fileStem}_${new Date().toISOString().slice(0, 10)}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function csvCell(value: unknown) {
  const text = String(value ?? "");
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}
</script>

<style scoped>
.pgc-root { display: flex; flex-direction: column; gap: 14px; }
.pgc-desc { margin: 0; color: var(--muted); font-size: 14px; font-weight: 750; line-height: 1.55; }
.pgc-workbench { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; align-items: stretch; }
.pgc-main, .pgc-input-row, .pgc-side { display: contents; }
.pgc-input-card { display: flex; flex-direction: column; gap: 8px; min-width: 0; height: 100%; }
.pgc-input-card:nth-child(1) { grid-column: 1; grid-row: 1; }
.pgc-input-card:nth-child(2) { grid-column: 2; grid-row: 1; }
.pgc-engine-card { grid-column: 1 / span 2; grid-row: 2; display: flex; flex-direction: column; gap: 12px; min-width: 0; height: 100%; }
.pgc-engine-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.pgc-engine-head .cte-card-title { margin-bottom: 0; }
.pgc-advanced-toggle { display: inline-flex; align-items: center; gap: 4px; border: none; background: transparent; color: var(--muted); font-size: 13px; font-weight: 900; cursor: pointer; }
.pgc-advanced-toggle span { display: inline-block; font-size: 18px; transition: transform .18s ease; }
.pgc-advanced-toggle span.open { transform: rotate(90deg); }
.pgc-advanced-toggle:disabled { opacity: .5; cursor: not-allowed; }
.pgc-stat-row { display: flex; gap: 8px; }
.pgc-stat { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 10px 8px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); text-align: center; }
.pgc-stat--muted { background: #f3f4f6; border-color: rgba(160,165,175,0.25); color: var(--muted); }
.pgc-stat--bad { background: #fdf0f0; border-color: rgba(200,125,125,0.22); }
.pgc-stat--bad .pgc-stat-num { color: #b55a5a; }
.pgc-stat-num { font-size: 16px; font-weight: 900; color: var(--text); }
.pgc-stat-label { font-size: 10px; font-weight: 700; color: var(--muted); }
.pgc-hidden-input { display: none; }
.pgc-illustration { grid-column: 3; grid-row: 1; display: flex; flex-direction: column; min-width: 0; height: 100%; }
.pgc-slot-img { flex: 1 1 auto; width: 100%; min-height: 0; height: 0; object-fit: contain; border-radius: 10px; }
.pgc-side .cte-how-card { grid-column: 3; grid-row: 2; display: flex; flex-direction: column; min-width: 0; height: 100%; }

.cte-card { box-sizing: border-box; background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-card); }
.cte-card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 900; margin: 0 0 8px; color: var(--text); }
.cte-max-badge { display: inline-flex; align-items: center; min-height: 20px; padding: 1px 8px; border: 1px solid rgba(95,125,112,0.24); border-radius: 999px; background: rgba(143,165,156,0.10); color: var(--brand-primary-3); font-size: 10px; font-weight: 900; letter-spacing: 0.02em; white-space: nowrap; }
.cte-hint { margin: 0 0 8px; color: var(--muted); font-size: 12px; font-weight: 700; line-height: 1.45; }
.cte-textarea { width: 100%; box-sizing: border-box; min-height: 88px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); color: var(--text); font-family: "JetBrains Mono","SFMono-Regular",Consolas,monospace; font-size: 13px; line-height: 1.6; resize: vertical; transition: border-color .18s ease, box-shadow .18s ease; }
.cte-textarea:focus { outline: none; border-color: var(--border-brand); box-shadow: 0 0 0 3px rgba(143,165,156,0.14); }
.cte-textarea:disabled { opacity: .55; cursor: not-allowed; }
.cte-btn-row { display: flex; gap: 8px; flex-wrap: wrap; }
.soft-btn { display: inline-flex; align-items: center; gap: 5px; min-height: 34px; padding: 6px 14px; border: 1px solid var(--border); border-radius: 9px; background: var(--surface); color: var(--text); font-size: 13px; font-weight: 800; cursor: pointer; transition: border-color .18s ease, background .18s ease; }
.soft-btn:hover:not(:disabled) { border-color: var(--border-brand); background: var(--surface-2); }
.soft-btn:disabled { opacity: .5; cursor: not-allowed; }
.primary-btn { display: inline-flex; align-items: center; gap: 6px; min-height: 38px; padding: 8px 22px; border: none; border-radius: 11px; background: var(--brand-primary-3); color: #fff; font-size: 14px; font-weight: 900; cursor: pointer; box-shadow: 0 6px 16px rgba(95,125,112,0.18); transition: background .18s ease, transform .18s ease, box-shadow .18s ease; }
.primary-btn:hover:not(:disabled) { background: #7f9f94; transform: translateY(-1px); box-shadow: 0 8px 20px rgba(95,125,112,0.24); }
.primary-btn:disabled { opacity: .55; cursor: not-allowed; box-shadow: none; }
.btn-spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.35); border-top-color: #fff; border-radius: 999px; animation: spin .6s linear infinite; }
.cte-fields { display: flex; flex-direction: column; gap: 10px; }
.cte-fields--grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px; }
.cte-field { display: flex; flex-direction: column; gap: 5px; }
.cte-field-label { font-size: 13px; font-weight: 900; color: rgba(39,66,58,0.84); }
.cte-field-label em {
  display: inline-flex;
  align-items: center;
  min-height: 17px;
  margin-left: 5px;
  padding: 1px 7px;
  border: 1px solid rgba(194,65,65,0.24);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255,247,247,0.98), rgba(251,226,226,0.92));
  color: #b93838;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.9), 0 2px 6px rgba(185,56,56,0.09);
  font-size: 9px;
  font-style: normal;
  font-weight: 900;
  letter-spacing: 0.055em;
  line-height: 1;
  text-transform: uppercase;
  vertical-align: 1px;
}
.cte-field-label small { color: var(--muted); font-size: 10px; font-weight: 750; text-transform: uppercase; }
.cte-field-hint { color: var(--muted); font-size: 10px; font-weight: 700; line-height: 1.35; }
.cte-select, .cte-number { width: 100%; }
.cte-advanced { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; padding-top: 12px; border-top: 1px solid var(--border); }
.cte-card-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-top: auto; padding-top: 12px; border-top: 1px solid var(--border); }
.pgc-label-with-help { display: inline-flex; align-items: center; gap: 5px; width: fit-content; }
.pgc-tooltip-copy { max-width: 330px; line-height: 1.55; }
.pgc-upload-wrap { position: relative; display: inline-flex; }
.pgc-help-icon { position: absolute; top: -6px; right: -6px; display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; z-index: 1; }
.pgc-help-icon--inline { position: static; width: 15px; height: 15px; }

.pgc-progress-card { margin-top: 12px; padding: 14px 15px 13px; border: 1px solid rgba(143,165,156,0.34); border-radius: 13px; background: linear-gradient(145deg, rgba(247,250,248,0.98), rgba(239,246,243,0.92)); box-shadow: inset 0 1px 0 rgba(255,255,255,0.82); }
.pgc-progress-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 11px; }
.pgc-progress-stage { color: var(--text); font-size: 13px; font-weight: 900; }
.pgc-progress-message { margin-top: 2px; color: var(--muted); font-size: 11.5px; font-weight: 700; line-height: 1.4; }
.pgc-progress-value { flex: 0 0 auto; color: var(--brand-primary-3); font-size: 17px; font-variant-numeric: tabular-nums; font-weight: 950; }
.pgc-progress-track { position: relative; height: 9px; overflow: hidden; border-radius: 999px; background: rgba(143,165,156,0.18); box-shadow: inset 0 1px 2px rgba(39,66,58,0.08); }
.pgc-progress-fill { position: relative; height: 100%; min-width: 4px; border-radius: inherit; background: linear-gradient(90deg, #9bb4aa, var(--brand-primary-3), #668c7d); box-shadow: 0 0 12px rgba(95,125,112,0.28); transition: width 0.5s cubic-bezier(0.22,1,0.36,1); }
.pgc-progress-track.indeterminate .pgc-progress-fill::after { content: ""; position: absolute; inset: 0; width: 42%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.72), transparent); animation: pgcProgressShimmer 1.35s ease-in-out infinite; }
.pgc-progress-steps { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 9px; }
.pgc-progress-steps span { position: relative; padding-top: 8px; color: #9aa7a2; font-size: 10px; font-weight: 800; text-align: center; transition: color 0.2s ease; }
.pgc-progress-steps span::before { content: ""; position: absolute; top: 0; left: 50%; width: 5px; height: 5px; border-radius: 999px; background: #cbd4d0; transform: translateX(-50%); }
.pgc-progress-steps span.done, .pgc-progress-steps span.active { color: var(--brand-primary-3); }
.pgc-progress-steps span.done::before, .pgc-progress-steps span.active::before { background: var(--brand-primary-3); box-shadow: 0 0 0 3px rgba(143,165,156,0.16); }

.cte-how-card .cte-card-title { font-size: 16px; }
.how-steps { display: flex; flex-direction: column; gap: 12px; }
.how-step { display: flex; gap: 12px; align-items: flex-start; }
.how-num { flex: 0 0 30px; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 999px; background: var(--nav-active-bg); color: var(--nav-active-text); font-size: 14px; font-weight: 900; border: 1px solid var(--nav-active-border); }
.how-step-body { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.how-step-body strong { font-size: 13px; font-weight: 900; color: var(--text); }
.how-step-body span { font-size: 12px; font-weight: 700; color: var(--muted); line-height: 1.45; }
.how-note { margin: 12px 0 0; padding-top: 10px; border-top: 1px solid var(--border); font-size: 12px; font-weight: 700; color: var(--muted); line-height: 1.6; }

.pgc-results { display: flex; flex-direction: column; gap: 14px; }
.pgc-summary-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
.cte-summary-card { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; padding: 16px 10px; border: 1px solid var(--border); border-radius: 14px; background: var(--surface); box-shadow: var(--shadow-card); text-align: center; }
.sum-num { font-size: 22px; font-weight: 900; color: var(--text); }
.sum-label { font-size: 11px; font-weight: 800; color: var(--muted); }
.pgc-result-top-cell { display: flex; align-items: baseline; gap: 7px; padding: 10px 14px; border: 1px solid var(--border-brand); border-radius: 10px; background: rgba(143,165,156,0.06); font-size: 14px; color: var(--text); }
.pgc-top-label { color: var(--muted); font-weight: 700; }
.pgc-top-count { color: var(--brand-primary-3); font-weight: 700; }
.cte-tabs-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px solid var(--border); }
.cte-tabs { display: flex; gap: 4px; flex-wrap: wrap; }
.cte-tab { min-height: 34px; padding: 8px 16px; border: none; border-bottom: 2px solid transparent; background: transparent; color: var(--muted); font-size: 13px; font-weight: 800; cursor: pointer; transition: color .18s ease, border-color .18s ease; }
.cte-tab:hover { color: var(--text); }
.cte-tab.active { color: var(--text); border-bottom-color: var(--brand-primary-3); font-weight: 900; }
.pgc-tab-help { display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; margin-left: 3px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 8px; font-weight: 900; cursor: help; }
.annotation-download-button { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid var(--border-brand); border-radius: 999px; background: #fffffff2; color: var(--brand-primary-3); box-shadow: inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor: pointer; margin-bottom: 4px; }
.pgc-res-content { min-height: 100px; }
.pgc-network-view { display: flex; flex-direction: column; }
.pgc-chart { width: 100%; height: 520px; }
.pgc-chart--tall { height: 720px; }
.cte-table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: 11px; background: var(--surface); }
.cte-table { width: 100%; min-width: 940px; border-collapse: collapse; font-size: 15px; color: #4f5964; }
.cte-table th { padding: 12px 16px; text-align: center; vertical-align: middle; border-bottom: 1px solid var(--border); white-space: nowrap; background: var(--surface-2); font-size: 13px; font-weight: 750; color: #7d8792; }
.cte-table td { padding: 14px 16px; text-align: center; vertical-align: middle; border-bottom: 1px solid var(--border); white-space: nowrap; font-weight: 560; }
.cte-table tbody tr { transition: background-color .16s ease; }
.cte-table tbody tr:hover { background: color-mix(in srgb, var(--brand-primary-1) 5%, #ffffff); }
.cte-table tbody tr:last-child td { border-bottom: none; }
.cte-no-data { text-align: center; color: var(--muted); padding: 32px; }
.pgc-feature-cell { display: flex; align-items: center; justify-content: center; min-height: 40px; }
.pgc-feature-line { display: inline-flex; align-items: center; justify-content: center; gap: 12px; }
.pgc-feature-value { color: #4c5661; font-size: 15px; font-weight: 820; letter-spacing: .01em; }
.pgc-feature-value--gene { min-width: 58px; font-size: 16px; }
.pgc-marker-stack { display: inline-flex; flex-direction: column; align-items: flex-start; justify-content: center; gap: 4px; }
.pgc-marker-badge { display: inline-flex; align-items: center; justify-content: center; width: fit-content; min-width: 52px; box-sizing: border-box; padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 850; line-height: 1.25; }
.pgc-marker-badge--peak { min-width: 76px; color: #ffffff; background: var(--brand-primary-3); border: 1px solid var(--brand-primary-3); box-shadow: 0 3px 8px color-mix(in srgb, var(--brand-primary-3) 22%, transparent); }
.pgc-marker-badge--expression { color: #187a3a; background: #e9f9ee; border: 1px solid #22c55e; }
.pgc-marker-badge--score { color: #6f5700; background: #fff8cf; border: 1px solid #facc15; }
.pgc-marker-badge--other { color: #5d4c78; background: #f1edf7; border: 1px solid #aa99c0; }
.cte-table code { font-family: "JetBrains Mono", monospace; color: var(--brand-primary-3); font-size: 14px; font-weight: 760; }
.gsc-sort-th { cursor: pointer; user-select: none; transition: color .16s ease, background-color .16s ease; }
.gsc-sort-th:hover { color: var(--brand-primary-3); background: color-mix(in srgb, var(--brand-primary-1) 7%, var(--surface-2)); }
.gsc-sort-arrow { display: inline-block; min-width: 12px; margin-left: 3px; color: var(--brand-primary-3); font-size: 10px; font-weight: 900; }
.cte-pagination { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px 0; flex-wrap: wrap; }
.cte-page-jump { width: 52px; padding: 2px 4px; border: 1px solid var(--border); border-radius: 4px; text-align: center; font-size: 13px; font-weight: 700; color: var(--text); background: var(--surface); }
.cte-page-jump::-webkit-inner-spin-button,
.cte-page-jump::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.cte-page-btn { min-height: 30px; padding: 4px 14px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); color: var(--text); font-size: 12px; font-weight: 800; cursor: pointer; }
.cte-page-btn:disabled { opacity: .4; cursor: not-allowed; }
.cte-page-info { font-size: 12px; font-weight: 800; color: var(--muted); }

:global(.el-dialog.pgc-download-dialog) { overflow: hidden; border: 1px solid rgba(0,0,0,.06); background: rgba(255,255,255,.98); box-shadow: 0 18px 60px rgba(0,0,0,.18); transform-origin: top center; }
:global(.el-dialog.pgc-download-dialog .el-dialog__header) { padding: 16px 18px 12px; border-bottom: 1px solid var(--border); background: linear-gradient(90deg, rgba(0,0,0,.02), rgba(0,0,0,0)); }
:global(.el-dialog.pgc-download-dialog .el-dialog__body) { padding: 14px 18px 16px; }
:global(.el-dialog.pgc-download-dialog .el-dialog__footer) { padding: 12px 18px 16px; border-top: 1px solid var(--border); background: rgba(0,0,0,.01); }
:global(.el-dialog.pgc-download-dialog .el-dialog__headerbtn) { border-radius: 10px; }
:global(.el-dialog.pgc-download-dialog .el-dialog__headerbtn:hover) { background: rgba(0,0,0,.04); }
.landscape-download-body { display: flex; flex-direction: column; }
.landscape-download-meta { display: flex; flex-wrap: wrap; gap: 10px 16px; padding: 6px 0 14px; margin-bottom: 14px; border-bottom: 1px solid var(--border); color: var(--muted); font-size: 13px; font-weight: 750; }
.landscape-download-grid { display: grid; grid-template-columns: 1fr; gap: 10px; }
.landscape-download-chip { display: flex; align-items: center; justify-content: space-between; gap: 14px; width: 100%; padding: 12px 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-card); cursor: pointer; transition: transform .16s ease, box-shadow .16s ease, border-color .16s ease; }
.landscape-download-chip:hover:not(:disabled) { border-color: rgba(0,0,0,.12); box-shadow: 0 10px 22px rgba(0,0,0,.08); transform: translateY(-1px); }
.landscape-download-chip:disabled { cursor: wait; opacity: .82; transform: none; }
.landscape-download-chip--loading .landscape-download-chip-action { min-width: 94px; }
.landscape-download-chip-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.landscape-download-chip-name { overflow: hidden; color: var(--text); font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.landscape-download-chip-format { padding: 3px 10px; border: 1px solid rgba(0,0,0,.1); border-radius: 999px; background: rgba(0,0,0,.03); font-size: 12px; font-weight: 900; }
.landscape-download-chip-action { display: inline-flex; align-items: center; justify-content: center; gap: 7px; padding: 6px 10px; border-radius: 999px; background: var(--brand-primary); color: #fff; font-size: 12px; font-weight: 900; white-space: nowrap; }
.landscape-download-spinner { width: 12px; height: 12px; border: 2px solid rgba(255,255,255,.45); border-top-color: #fff; border-radius: 50%; animation: landscapeDownloadSpin .7s linear infinite; }
@keyframes landscapeDownloadSpin { to { transform: rotate(360deg); } }

@keyframes spin { to { transform: rotate(360deg); } }
@keyframes pgcProgressShimmer { from { transform: translateX(-120%); } to { transform: translateX(340%); } }

@media (max-width: 1024px) {
  .pgc-workbench { grid-template-columns: 1fr 1fr; }
  .pgc-input-card:nth-child(1) { grid-column: 1; }
  .pgc-input-card:nth-child(2) { grid-column: 2; }
  .pgc-engine-card, .pgc-illustration, .pgc-side .cte-how-card { grid-column: 1 / -1; grid-row: auto; }
  .pgc-slot-img { height: auto; }
}

@media (max-width: 760px) {
  .pgc-workbench { grid-template-columns: 1fr; }
  .pgc-input-card, .pgc-engine-card, .pgc-illustration, .pgc-side .cte-how-card { grid-column: 1 / -1 !important; grid-row: auto !important; }
  .cte-fields--grid, .cte-advanced { grid-template-columns: 1fr; }
  .pgc-progress-steps { gap: 2px; }
  .pgc-progress-steps span { font-size: 9px; }
  .pgc-chart--tall { height: 420px; }
}
</style>

<template>
  <section class="sample-landscape-card float-card">
    <div class="landscape-head">
      <div class="landscape-title-cell">
        <div class="landscape-title-block">
          <div class="landscape-title">Sample landscape</div>
          <div class="landscape-sub">Aggregated sample-level visualizations for <span class="mono">{{ datasetId }}</span></div>
        </div>
        <label class="umap-control-field">
          <span class="umap-control-label">Group</span>
          <el-select
            v-model="compositionView"
            class="umap-select"
            popper-class="umap-toolbar-popper"
            size="small"
            aria-label="Composition grouping"
          >
            <el-option
              v-for="option in compositionViewOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>
      </div>
      <div class="qc-control-slot">
        <label class="umap-control-field qc-control-field">
          <span class="qc-metric-switch" role="radiogroup" aria-label="QC metric">
            <button
              v-for="option in qcMetricOptions"
              :key="option.value"
              type="button"
              class="qc-metric-button"
              :class="{ active: selectedQcMetric === option.value }"
              :aria-pressed="selectedQcMetric === option.value"
              :title="option.title"
              @click="selectedQcMetric = option.value"
            >
              {{ option.label }}
            </button>
          </span>
        </label>
      </div>
      <div class="landscape-toolbar" aria-label="UMAP chart controls">
        <label class="umap-control-field">
          <span class="umap-control-label">Embedding</span>
          <el-select
            v-model="selectedEmbedding"
            class="umap-select"
            popper-class="umap-toolbar-popper"
            size="small"
          >
            <el-option
              v-for="option in embeddingOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>
        <label class="umap-control-field">
          <span class="umap-control-label">Color</span>
          <el-select
            v-model="selectedColorBy"
            class="umap-select"
            popper-class="umap-toolbar-popper"
            size="small"
          >
            <el-option
              v-for="option in colorByOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>
      </div>
    </div>

    <div class="landscape-chart-grid">
      <article class="landscape-chart-region" :aria-label="compositionChartLabel">
        <button
          class="chart-download-button"
          type="button"
          :disabled="!canDownloadComposition"
          :title="canDownloadComposition ? 'Download chart or data table' : 'Chart data unavailable'"
          :aria-label="`Download ${datasetId} ${compositionChartLabel.toLowerCase()} chart or data`"
          @click.stop="openDownloadDialog('composition')"
        >
          <el-icon><Download /></el-icon>
        </button>
        <CellTypeCompositionChart
          ref="compositionChartRef"
          :data="displayedComposition"
          :loading="composition.loading"
          :error="composition.error"
          :group-label="compositionGroupLabel"
        />
      </article>

      <article class="landscape-chart-region" aria-label="QC distribution">
        <button
          class="chart-download-button chart-download-button--qc"
          type="button"
          :disabled="!canDownloadQc"
          :title="canDownloadQc ? 'Download chart as PNG' : 'Chart image unavailable'"
          :aria-label="`Download ${datasetId} QC distribution chart`"
          @click.stop="downloadQcChart"
        >
          <el-icon><Download /></el-icon>
        </button>
        <QcViolinChart
          ref="qcChartRef"
          :data="qc.data"
          :loading="qc.loading"
          :error="qc.error"
        />
      </article>

      <article class="landscape-chart-region" aria-label="UMAP">
        <button
          class="chart-download-button"
          type="button"
          :disabled="!canDownloadUmap"
          :title="canDownloadUmap ? 'Download chart or data table' : 'Chart data unavailable'"
          :aria-label="`Download ${datasetId} ${selectedEmbedding} ${selectedColorBy} embedding chart or data`"
          @click.stop="openDownloadDialog('umap')"
        >
          <el-icon><Download /></el-icon>
        </button>
        <UmapChart
          ref="umapChartRef"
          :data="umap.data"
          :loading="umap.loading"
          :error="umap.error"
          :embedding="selectedEmbedding"
          :color-by="selectedColorBy"
        />
      </article>
    </div>
  </section>

  <el-dialog
    v-model="downloadDialogOpen"
    width="640px"
    :title="downloadDialogTitle"
    custom-class="bubble-dialog landscape-download-dialog"
    modal-class="bubble-overlay"
    :append-to-body="true"
    class="float-card"
  >
    <div class="landscape-download-body">
      <div class="landscape-download-meta">
        <span class="mono">{{ datasetId }}</span>
        <span>{{ domainDisplayLabel(domain) }}</span>
        <span>{{ downloadDialogChartLabel }}</span>
      </div>
      <div class="landscape-download-grid">
        <button
          type="button"
          class="landscape-download-chip"
          :class="{ 'landscape-download-chip--loading': activeDownloadAction === 'image' }"
          :disabled="activeDownloadAction !== null"
          @click="runLandscapeDownload('image', downloadDialogImage)"
        >
          <span class="landscape-download-chip-left">
            <span class="landscape-download-chip-name">Chart image</span>
            <span class="landscape-download-chip-format">PNG</span>
          </span>
          <span class="landscape-download-chip-action" aria-live="polite">
            <span v-if="activeDownloadAction === 'image'" class="landscape-download-spinner" aria-hidden="true" />
            {{ activeDownloadAction === 'image' ? 'Starting...' : 'Download' }}
          </span>
        </button>
        <button
          type="button"
          class="landscape-download-chip"
          :class="{ 'landscape-download-chip--loading': activeDownloadAction === 'table' }"
          :disabled="activeDownloadAction !== null"
          @click="runLandscapeDownload('table', downloadDialogTable)"
        >
          <span class="landscape-download-chip-left">
            <span class="landscape-download-chip-name">Underlying data table</span>
            <span class="landscape-download-chip-format">
              {{ downloadDialogKind === 'umap' ? 'CSV · Displayed' : 'CSV' }}
            </span>
          </span>
          <span class="landscape-download-chip-action" aria-live="polite">
            <span v-if="activeDownloadAction === 'table'" class="landscape-download-spinner" aria-hidden="true" />
            {{ activeDownloadAction === 'table' ? 'Starting...' : 'Download' }}
          </span>
        </button>
        <button
          v-if="downloadDialogKind === 'umap'"
          type="button"
          class="landscape-download-chip"
          :class="{ 'landscape-download-chip--loading': activeDownloadAction === 'full' }"
          :disabled="activeDownloadAction !== null"
          @click="runLandscapeDownload('full', downloadFullUmapTable)"
        >
          <span class="landscape-download-chip-left">
            <span class="landscape-download-chip-name">Full data table</span>
            <span class="landscape-download-chip-format">CSV · All</span>
          </span>
          <span class="landscape-download-chip-action" aria-live="polite">
            <span v-if="activeDownloadAction === 'full'" class="landscape-download-spinner" aria-hidden="true" />
            {{ activeDownloadAction === 'full' ? 'Starting...' : 'Download' }}
          </span>
        </button>
      </div>
    </div>
    <template #footer>
      <el-button @click="downloadDialogOpen = false">Close</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Download } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from "vue";
import type {
  CellTypeCompositionData,
  QcViolinData,
  SearchResultColorBy,
  SearchResultDomain,
  SearchResultEmbedding,
  UmapData,
} from "@/api/searchResult";
import { fetchCellTypeComposition, fetchQcViolin, fetchUmap } from "@/api/searchResult";
import CellTypeCompositionChart from "@/components/search-result/CellTypeCompositionChart.vue";
import QcViolinChart from "@/components/search-result/QcViolinChart.vue";
import UmapChart from "@/components/search-result/UmapChart.vue";
import { buildApiUrl } from "@/config/api";
import { downloadCsv } from "@/utils/downloadCsv";
import { domainDisplayLabel } from "@/utils/searchResultDomain";

const props = defineProps<{
  datasetId: string;
  domain: SearchResultDomain;
  prefetchEnabled: boolean;
}>();

type ChartState<T> = {
  data: T | null;
  loading: boolean;
  error: boolean;
};

type QcMetricKey = "TSSEnrichment" | "nFrags" | "Gex_nGenes" | "Gex_MitoRatio";

type QcMetricOption = {
  value: QcMetricKey;
  label: string;
  title: string;
  domains: ReadonlyArray<SearchResultDomain>;
};

const composition = reactive<ChartState<CellTypeCompositionData>>(createChartState());
const qc = reactive<ChartState<QcViolinData>>(createChartState());
const umap = reactive<ChartState<UmapData>>(createChartState());
const selectedEmbedding = ref<SearchResultEmbedding>("umap");
const selectedColorBy = ref<SearchResultColorBy>(props.domain === "integration" ? "celltype" : "cluster");
const selectedQcMetric = ref<QcMetricKey>("TSSEnrichment");
type LandscapeDownloadKind = "composition" | "umap";
type LandscapeDownloadAction = "image" | "table" | "full";
const downloadDialogOpen = ref(false);
const downloadDialogKind = ref<LandscapeDownloadKind>("composition");
const activeDownloadAction = ref<LandscapeDownloadAction | null>(null);

type ChartExportHandle = {
  downloadImage: (filename: string) => boolean;
};

const compositionChartRef = ref<ChartExportHandle | null>(null);
const qcChartRef = ref<ChartExportHandle | null>(null);
const umapChartRef = ref<ChartExportHandle | null>(null);

const embeddingOptions: Array<{ value: SearchResultEmbedding; label: string }> = [
  { value: "umap", label: "UMAP" },
  { value: "tsne", label: "t-SNE" },
];

const colorByOptions = computed(() => {
  if (props.domain !== "integration") {
    return [{ value: "cluster" as const, label: "Cluster" }];
  }
  return [
    { value: "celltype" as const, label: "Cell type" },
    { value: "cluster" as const, label: "Cluster" },
  ];
});

const compositionView = ref<SearchResultColorBy>("cluster");
const compositionViewOptions = computed(() => {
  if (props.domain !== "integration") {
    return [{ value: "cluster" as const, label: "Cluster" }];
  }
  return [
    { value: "celltype" as const, label: "Cell type" },
    { value: "cluster" as const, label: "Cluster" },
  ];
});
const compositionGroupLabel = computed(() =>
  compositionView.value === "celltype" ? "Cell type" : "Cluster"
);
// Celltype 视图：把后端按 cluster 返回的 "Celltype / Cluster" 标签去重聚合，按 celltype 汇总。
const displayedComposition = computed<CellTypeCompositionData | null>(() => {
  const data = composition.data;
  if (!data || compositionView.value !== "celltype") return data;
  const merged = new Map<string, number>();
  for (const item of data.items) {
    const celltype = (item.label.split(" / ")[0] || item.label).trim() || item.label;
    merged.set(celltype, (merged.get(celltype) ?? 0) + item.count);
  }
  const total = Array.from(merged.values()).reduce((sum, count) => sum + count, 0);
  const items = Array.from(merged.entries())
    .map(([label, count]) => ({ label, count, ratio: total > 0 ? count / total : 0 }))
    .sort((a, b) => b.count - a.count);
  return { datasetId: data.datasetId, groupBy: "celltype", items };
});
const compositionChartLabel = computed(() => `${compositionGroupLabel.value} composition`);

const allQcMetricOptions: QcMetricOption[] = [
  {
    value: "TSSEnrichment",
    label: "TSS",
    title: "TSSEnrichment",
    domains: ["integration", "atac"],
  },
  {
    value: "nFrags",
    label: "nFrags",
    title: "Unique fragments per cell",
    domains: ["integration", "atac"],
  },
  {
    value: "Gex_nGenes",
    label: "nGenes",
    title: "Gex_nGenes",
    domains: ["integration", "rna"],
  },
  {
    value: "Gex_MitoRatio",
    label: "Mito",
    title: "Gex_MitoRatio",
    domains: ["integration", "rna"],
  },
];

const qcMetricOptions = computed(() => {
  return allQcMetricOptions.filter((option) => option.domains.includes(props.domain));
});

const canDownloadComposition = computed(() => {
  return !composition.loading && !composition.error && (composition.data?.items?.length ?? 0) > 0;
});

const canDownloadQc = computed(() => {
  return !qc.loading && !qc.error && Boolean(qc.data?.metrics?.some((metric) => metric.groups?.length));
});

const canDownloadUmap = computed(() => {
  return !umap.loading && !umap.error && (umap.data?.points?.length ?? 0) > 0;
});

const downloadDialogChartLabel = computed(() => downloadDialogKind.value === "composition"
  ? compositionChartLabel.value
  : `${selectedEmbedding.value === "tsne" ? "t-SNE" : "UMAP"} · ${selectedColorBy.value === "celltype" ? "Cell type" : "Cluster"}`);

const downloadDialogTitle = computed(() => `Download ${downloadDialogChartLabel.value}`);

let lastCompositionRequestKey = "";
let compositionLoadToken = 0;
let qcLoadToken = 0;
let umapLoadToken = 0;
let prefetchTimer: number | undefined;
let downloadFeedbackTimer: number | undefined;
let prefetchGeneration = 0;
let landscapeDisposed = false;

const qcDataCache = new Map<string, QcViolinData>();
const qcRequestCache = new Map<string, Promise<QcViolinData>>();
const umapDataCache = new Map<string, UmapData>();
const umapRequestCache = new Map<string, Promise<UmapData>>();
const prefetchedContexts = new Set<string>();

function createChartState<T>(): ChartState<T> {
  return {
    data: null,
    loading: false,
    error: false,
  };
}

function startLoading<T>(state: ChartState<T>) {
  state.loading = true;
  state.error = false;
  state.data = null;
}

function finishWithError<T>(state: ChartState<T>, error: unknown, label: string) {
  console.error(`[SearchResult] Failed to load ${label}:`, error);
  state.loading = false;
  state.error = true;
  state.data = null;
}

function finishWithData<T>(state: ChartState<T>, data: T) {
  state.loading = false;
  state.error = false;
  state.data = data;
}

function sanitizeFilenamePart(value: string) {
  return value.trim().replace(/[\\/:*?"<>|\s]+/g, "_") || "sample";
}

function showDownloadUnavailableMessage() {
  ElMessage.warning("Chart data is not ready yet.");
}

function triggerChartDownload(chartRef: ChartExportHandle | null, filename: string) {
  if (!chartRef?.downloadImage(filename)) {
    showDownloadUnavailableMessage();
    return false;
  }
  return true;
}

function downloadCompositionChart() {
  if (!canDownloadComposition.value) {
    showDownloadUnavailableMessage();
    return false;
  }

  const datasetPart = sanitizeFilenamePart(props.datasetId);
  return triggerChartDownload(
    compositionChartRef.value,
    `${datasetPart}_${compositionView.value}_composition.png`
  );
}

function downloadQcChart() {
  if (!canDownloadQc.value) {
    showDownloadUnavailableMessage();
    return;
  }

  const datasetPart = sanitizeFilenamePart(props.datasetId);
  const metricPart = sanitizeFilenamePart(selectedQcMetric.value);
  triggerChartDownload(qcChartRef.value, `${datasetPart}_${metricPart}_qc_distribution.png`);
}

function downloadUmapChart() {
  if (!canDownloadUmap.value) {
    showDownloadUnavailableMessage();
    return false;
  }

  const datasetPart = sanitizeFilenamePart(props.datasetId);
  const embeddingPart = sanitizeFilenamePart(selectedEmbedding.value);
  const colorPart = sanitizeFilenamePart(selectedColorBy.value);
  return triggerChartDownload(umapChartRef.value, `${datasetPart}_${embeddingPart}_${colorPart}.png`);
}

function openDownloadDialog(kind: LandscapeDownloadKind) {
  if (kind === "composition" && !canDownloadComposition.value) {
    showDownloadUnavailableMessage();
    return;
  }
  if (kind === "umap" && !canDownloadUmap.value) {
    showDownloadUnavailableMessage();
    return;
  }
  downloadDialogKind.value = kind;
  downloadDialogOpen.value = true;
}

async function runLandscapeDownload(
  action: LandscapeDownloadAction,
  download: () => boolean | Promise<boolean>
) {
  if (activeDownloadAction.value !== null) return;
  activeDownloadAction.value = action;
  await nextTick();
  const startedAt = performance.now();
  let started = false;
  try {
    started = await download();
  } finally {
    const remaining = Math.max(0, 900 - (performance.now() - startedAt));
    window.clearTimeout(downloadFeedbackTimer);
    downloadFeedbackTimer = window.setTimeout(() => {
      activeDownloadAction.value = null;
      if (started) downloadDialogOpen.value = false;
      downloadFeedbackTimer = undefined;
    }, remaining);
  }
}

function downloadDialogImage() {
  return downloadDialogKind.value === "composition"
    ? downloadCompositionChart()
    : downloadUmapChart();
}

function downloadDialogTable() {
  const datasetPart = sanitizeFilenamePart(props.datasetId);
  if (downloadDialogKind.value === "composition") {
    const data = composition.data;
    if (!data?.items?.length) {
      showDownloadUnavailableMessage();
      return false;
    }
    downloadCsv(
      `${datasetPart}_${compositionView.value}_composition.csv`,
      ["dataset_id", "domain", "group_by", "label", "count", "ratio"],
      data.items.map((item) => [
        data.datasetId || props.datasetId,
        props.domain,
        data.groupBy,
        item.label,
        String(item.count),
        String(item.ratio),
      ])
    );
    return true;
  }

  const data = umap.data;
  if (!data?.points?.length) {
    showDownloadUnavailableMessage();
    return false;
  }
  const embedding = data.embedding ?? selectedEmbedding.value;
  const colorBy = data.colorBy ?? selectedColorBy.value;
  const includeCellType = props.domain === "integration";
  const headers = ["dataset_id", "domain", "embedding", "color_by", "barcode", "x", "y", "label"];
  if (includeCellType) headers.push("cell_type");
  headers.push("cluster");
  downloadCsv(
    `${datasetPart}_${sanitizeFilenamePart(embedding)}_${sanitizeFilenamePart(colorBy)}.csv`,
    headers,
    data.points.map((point) => {
      const row = [
        data.datasetId || props.datasetId,
        data.domain || props.domain,
        embedding,
        colorBy,
        point.barcode,
        String(point.x),
        String(point.y),
        point.label,
      ];
      if (includeCellType) row.push(point.celltype || "");
      row.push(point.cluster == null ? "" : String(point.cluster));
      return row;
    })
  );
  return true;
}

function downloadFullUmapTable() {
  const params = new URLSearchParams({
    datasetId: props.datasetId,
    domain: props.domain,
    embedding: selectedEmbedding.value,
    colorBy: selectedColorBy.value,
  });
  const anchor = document.createElement("a");
  anchor.href = `${buildApiUrl("api/search-result/umap/download")}?${params.toString()}`;
  anchor.download = [
    sanitizeFilenamePart(props.datasetId),
    props.domain,
    selectedEmbedding.value,
    selectedColorBy.value,
    "full.csv",
  ].join("_");
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  return true;
}

function defaultQcMetricForDomain(domain: SearchResultDomain): QcMetricKey {
  return domain === "rna" ? "Gex_nGenes" : "TSSEnrichment";
}

function isQcMetricAvailable(domain: SearchResultDomain, metric: QcMetricKey) {
  return allQcMetricOptions.some((option) => option.value === metric && option.domains.includes(domain));
}

function ensureQcMetricForDomain(domain: SearchResultDomain) {
  if (!isQcMetricAvailable(domain, selectedQcMetric.value)) {
    selectedQcMetric.value = defaultQcMetricForDomain(domain);
  }
}

function qcRequestKey(datasetId: string, domain: SearchResultDomain, metric: QcMetricKey) {
  return `${datasetId}::${domain}::${metric}`;
}

function umapRequestKey(
  datasetId: string,
  domain: SearchResultDomain,
  embedding: SearchResultEmbedding,
  colorBy: SearchResultColorBy
) {
  return `${datasetId}::${domain}::${embedding}::${colorBy}`;
}

function requestQcData(datasetId: string, domain: SearchResultDomain, metric: QcMetricKey) {
  const key = qcRequestKey(datasetId, domain, metric);
  const cached = qcDataCache.get(key);
  if (cached) return Promise.resolve(cached);

  const pending = qcRequestCache.get(key);
  if (pending) return pending;

  const request = fetchQcViolin({
    datasetId,
    domain,
    groupBy: "cluster",
    metrics: [metric],
  }).then((data) => {
    qcDataCache.set(key, data);
    return data;
  }).finally(() => {
    if (qcRequestCache.get(key) === request) qcRequestCache.delete(key);
  });

  qcRequestCache.set(key, request);
  return request;
}

function requestUmapData(
  datasetId: string,
  domain: SearchResultDomain,
  embedding: SearchResultEmbedding,
  colorBy: SearchResultColorBy
) {
  const key = umapRequestKey(datasetId, domain, embedding, colorBy);
  const cached = umapDataCache.get(key);
  if (cached) return Promise.resolve(cached);

  const pending = umapRequestCache.get(key);
  if (pending) return pending;

  const request = fetchUmap({ datasetId, domain, embedding, colorBy }).then((data) => {
    umapDataCache.set(key, data);
    return data;
  }).finally(() => {
    if (umapRequestCache.get(key) === request) umapRequestCache.delete(key);
  });

  umapRequestCache.set(key, request);
  return request;
}

async function loadComposition(datasetId: string, domain: SearchResultDomain) {
  const normalizedDatasetId = datasetId.trim();
  const requestKey = `${normalizedDatasetId}::${domain}`;
  if (!normalizedDatasetId || requestKey === lastCompositionRequestKey) return;

  lastCompositionRequestKey = requestKey;
  const currentToken = ++compositionLoadToken;

  startLoading(composition);

  try {
    const data = await fetchCellTypeComposition({
      datasetId: normalizedDatasetId,
      domain,
      groupBy: domain === "integration" ? "celltype" : "cluster",
    });
    if (currentToken !== compositionLoadToken) return;
    finishWithData(composition, data);
  } catch (error) {
    if (currentToken !== compositionLoadToken) return;
    finishWithError(composition, error, `${domain === "integration" ? "cell type" : "cluster"} composition`);
  }
}

async function loadQc(datasetId: string, domain: SearchResultDomain, metric: QcMetricKey) {
  if (!isQcMetricAvailable(domain, metric)) {
    ensureQcMetricForDomain(domain);
    return;
  }

  const normalizedDatasetId = datasetId.trim();
  const requestKey = `${normalizedDatasetId}::${domain}::${metric}`;
  if (!normalizedDatasetId) return;

  const currentToken = ++qcLoadToken;
  const cached = qcDataCache.get(requestKey);
  if (cached) {
    finishWithData(qc, cached);
    return;
  }

  startLoading(qc);

  try {
    const data = await requestQcData(normalizedDatasetId, domain, metric);
    if (currentToken !== qcLoadToken) return;
    finishWithData(qc, data);
  } catch (error) {
    if (currentToken !== qcLoadToken) return;
    finishWithError(qc, error, "QC distribution");
  }
}

async function loadUmap(
  datasetId: string,
  domain: SearchResultDomain,
  embedding: SearchResultEmbedding,
  colorBy: SearchResultColorBy
) {
  const normalizedDatasetId = datasetId.trim();
  const requestKey = `${normalizedDatasetId}::${domain}::${embedding}::${colorBy}`;
  if (!normalizedDatasetId) return;

  const currentToken = ++umapLoadToken;
  const cached = umapDataCache.get(requestKey);
  if (cached) {
    finishWithData(umap, cached);
    return;
  }

  startLoading(umap);

  try {
    const data = await requestUmapData(normalizedDatasetId, domain, embedding, colorBy);
    if (currentToken !== umapLoadToken) return;
    finishWithData(umap, data);
  } catch (error) {
    if (currentToken !== umapLoadToken) return;
    finishWithError(umap, error, "UMAP");
  }
}

async function prefetchQcMetrics(
  datasetId: string,
  domain: SearchResultDomain,
  initialMetric: QcMetricKey,
  generation: number
) {
  const metrics = allQcMetricOptions
    .filter((option) => option.domains.includes(domain) && option.value !== initialMetric)
    .map((option) => option.value);

  for (const metric of metrics) {
    if (landscapeDisposed || generation !== prefetchGeneration) return;
    try {
      await requestQcData(datasetId, domain, metric);
    } catch {
      // A foreground selection can retry a failed background request.
    }
  }
}

async function prefetchUmapVariants(
  datasetId: string,
  domain: SearchResultDomain,
  initialEmbedding: SearchResultEmbedding,
  initialColorBy: SearchResultColorBy,
  generation: number
) {
  const otherEmbedding: SearchResultEmbedding = initialEmbedding === "umap" ? "tsne" : "umap";
  const primaryColor: SearchResultColorBy = domain === "integration" ? initialColorBy : "cluster";
  const variants: Array<{ embedding: SearchResultEmbedding; colorBy: SearchResultColorBy }> = [
    { embedding: otherEmbedding, colorBy: primaryColor },
  ];
  if (domain === "integration") {
    const otherColor: SearchResultColorBy = primaryColor === "celltype" ? "cluster" : "celltype";
    variants.push(
      { embedding: initialEmbedding, colorBy: otherColor },
      { embedding: otherEmbedding, colorBy: otherColor }
    );
  }

  for (const variant of variants) {
    if (landscapeDisposed || generation !== prefetchGeneration) return;
    try {
      await requestUmapData(datasetId, domain, variant.embedding, variant.colorBy);
    } catch {
      // A foreground selection can retry a failed background request.
    }
  }
}

function scheduleLandscapePrefetch(datasetId: string, domain: SearchResultDomain, enabled: boolean) {
  window.clearTimeout(prefetchTimer);
  prefetchTimer = undefined;
  const generation = ++prefetchGeneration;
  const normalizedDatasetId = datasetId.trim();
  if (!enabled || !normalizedDatasetId) return;

  const contextKey = `${normalizedDatasetId}::${domain}`;
  if (prefetchedContexts.has(contextKey)) return;

  prefetchTimer = window.setTimeout(() => {
    prefetchTimer = undefined;
    if (landscapeDisposed || generation !== prefetchGeneration) return;
    void Promise.all([
      prefetchQcMetrics(
        normalizedDatasetId,
        domain,
        selectedQcMetric.value,
        generation
      ),
      prefetchUmapVariants(
        normalizedDatasetId,
        domain,
        selectedEmbedding.value,
        selectedColorBy.value,
        generation
      ),
    ]).then(() => {
      if (!landscapeDisposed && generation === prefetchGeneration) {
        prefetchedContexts.add(contextKey);
      }
    });
  }, 400);
}

watch(() => [props.datasetId, props.domain] as const, ([nextDatasetId, nextDomain]) => {
  if (nextDomain !== "integration" && selectedColorBy.value === "celltype") {
    selectedColorBy.value = "cluster";
  }
  ensureQcMetricForDomain(nextDomain);
  loadComposition(nextDatasetId, nextDomain);
}, { immediate: true });

watch(() => [props.datasetId, props.domain, selectedQcMetric.value] as const, ([
  nextDatasetId,
  nextDomain,
  nextQcMetric,
]) => {
  loadQc(nextDatasetId, nextDomain, nextQcMetric);
}, { immediate: true });

watch(() => [props.datasetId, props.domain, selectedEmbedding.value, selectedColorBy.value] as const, ([
  nextDatasetId,
  nextDomain,
  nextEmbedding,
  nextColorBy,
]) => {
  loadUmap(nextDatasetId, nextDomain, nextEmbedding, nextColorBy);
}, { immediate: true });

watch(() => [props.datasetId, props.domain, props.prefetchEnabled] as const, ([
  nextDatasetId,
  nextDomain,
  nextPrefetchEnabled,
]) => {
  scheduleLandscapePrefetch(nextDatasetId, nextDomain, nextPrefetchEnabled);
}, { immediate: true });

onBeforeUnmount(() => {
  landscapeDisposed = true;
  ++compositionLoadToken;
  ++qcLoadToken;
  ++umapLoadToken;
  ++prefetchGeneration;
  window.clearTimeout(prefetchTimer);
  window.clearTimeout(downloadFeedbackTimer);
});
</script>

<style scoped>
.sample-landscape-card {
  position: relative;
  overflow: hidden;
  padding: 16px 18px 18px;
  margin-bottom: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  animation: landscapeIn 0.28s ease both;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.sample-landscape-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.landscape-head {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
  margin-bottom: 14px;
}

.landscape-title-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.landscape-title-block {
  flex: 1;
  min-width: 0;
}

.landscape-title {
  font-size: 18px;
  font-weight: 900;
}

.landscape-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.qc-control-slot,
.landscape-toolbar {
  min-width: 0;
}

.qc-control-slot {
  display: flex;
  align-items: center;
  justify-content: center;
}

.landscape-chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.landscape-chart-region {
  position: relative;
  min-width: 0;
  min-height: 440px;
  padding: 0;
  border: 0;
  background: transparent;
}

.chart-download-button {
  appearance: none;
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: #fffffff2;
  color: var(--brand-primary-3);
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 6px 14px #12182614;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.chart-download-button--qc {
  right: -18px;
}

.chart-download-button:hover:not(:disabled) {
  border-color: var(--nav-active-border);
  background: var(--surface-2);
  color: var(--text);
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 8px 16px rgba(95, 125, 112, 0.16);
  transform: translateY(-1px);
}

.chart-download-button:focus-visible {
  outline: 2px solid #8fa59c40;
  outline-offset: 2px;
}

.chart-download-button:disabled {
  border-color: var(--border);
  background: #ffffffb8;
  color: var(--muted);
  cursor: not-allowed;
  opacity: 0.56;
  box-shadow: inset 0 1px 0 #ffffffcc;
}

.chart-download-button :deep(.el-icon) {
  font-size: 15px;
}

.qc-metric-switch {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  max-width: 100%;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  box-shadow: none;
  overflow-x: auto;
  scrollbar-width: none;
}

.qc-metric-switch::-webkit-scrollbar {
  display: none;
}

.qc-metric-button {
  appearance: none;
  flex: 0 0 auto;
  min-width: 44px;
  min-height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 11px;
  font-weight: 900;
  cursor: pointer;
  line-height: 1;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.qc-metric-button:hover {
  background: var(--surface-3);
  color: var(--text);
}

.qc-metric-button.active {
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  box-shadow: 0 0 0 1px var(--nav-active-border) inset;
}

.qc-metric-button:focus-visible {
  outline: 2px solid #8fa59c40;
  outline-offset: 2px;
}

.landscape-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.umap-control-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px 4px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #ffffffeb;
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 6px 14px #1218260f;
}

.umap-control-label {
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
}

.umap-select {
  width: 104px;
}

.umap-select :deep(.el-select__wrapper) {
  min-height: 28px;
  border-radius: 999px;
  background: var(--surface);
  box-shadow: 0 0 0 1px var(--border) inset;
  padding: 0 9px 0 10px;
  transition:
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.umap-select :deep(.el-select__wrapper:hover) {
  background: var(--surface-2);
  box-shadow: 0 0 0 1px var(--border-brand) inset;
}

.umap-select :deep(.el-select__wrapper.is-focused) {
  box-shadow:
    0 0 0 1px var(--nav-active-border) inset,
    0 5px 12px rgba(95, 125, 112, 0.16);
}

.umap-select :deep(.el-select__selected-item) {
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
}

.umap-select :deep(.el-select__caret) {
  color: var(--muted);
}

.qc-control-field {
  max-width: min(360px, 100%);
}

:global(.umap-toolbar-popper) {
  border: 1px solid var(--border) !important;
  border-radius: 12px !important;
  background: #fffffff2 !important;
  box-shadow:
    0 16px 32px rgba(18, 24, 38, 0.14),
    inset 0 1px 0 #ffffffcc !important;
  overflow: hidden;
}

:global(.umap-toolbar-popper .el-popper__arrow::before) {
  border-color: var(--border) !important;
  background: #fffffff2 !important;
}

:global(.umap-toolbar-popper .el-select-dropdown) {
  border-radius: 12px;
  background: transparent;
}

:global(.umap-toolbar-popper .el-select-dropdown__list) {
  padding: 6px;
}

:global(.umap-toolbar-popper .el-select-dropdown__item) {
  height: 32px;
  border-radius: 8px;
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
  line-height: 32px;
  padding: 0 12px;
}

:global(.umap-toolbar-popper .el-select-dropdown__item.hover),
:global(.umap-toolbar-popper .el-select-dropdown__item:hover) {
  background: var(--surface-3) !important;
  color: var(--text) !important;
}

:global(.umap-toolbar-popper .el-select-dropdown__item.is-selected) {
  background: var(--nav-active-bg) !important;
  color: var(--nav-active-text) !important;
}

:global(.bubble-overlay) {
  background-color: rgba(0, 0, 0, 0.35) !important;
  backdrop-filter: blur(8px);
}

:global(.el-dialog.landscape-download-dialog) {
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.18);
  transform-origin: top center;
}

:global(.el-dialog.landscape-download-dialog .el-dialog__header) {
  padding: 16px 18px 12px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0));
}

:global(.el-dialog.landscape-download-dialog .el-dialog__body) {
  padding: 14px 18px 16px;
}

:global(.el-dialog.landscape-download-dialog .el-dialog__footer) {
  padding: 12px 18px 16px;
  border-top: 1px solid var(--border);
  background: rgba(0, 0, 0, 0.01);
}

:global(.el-dialog.landscape-download-dialog .el-dialog__headerbtn) {
  border-radius: 10px;
}

:global(.el-dialog.landscape-download-dialog .el-dialog__headerbtn:hover) {
  background: rgba(0, 0, 0, 0.04);
}

:global(.dialog-fade-enter-active .el-dialog.landscape-download-dialog) {
  animation: landscapeBubbleIn 0.18s ease-out both;
}

:global(.dialog-fade-leave-active .el-dialog.landscape-download-dialog) {
  animation: landscapeBubbleOut 0.14s ease-in both;
}

@keyframes landscapeBubbleIn {
  from { opacity: 0; transform: translateY(-10px) scale(0.985); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes landscapeBubbleOut {
  from { opacity: 1; transform: translateY(0) scale(1); }
  to { opacity: 0; transform: translateY(-8px) scale(0.99); }
}

.landscape-download-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  padding: 6px 0 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid var(--border);
  color: var(--muted);
  font-size: 13px;
  font-weight: 750;
}

.landscape-download-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.landscape-download-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}

.landscape-download-chip:hover:not(:disabled) {
  border-color: rgba(0, 0, 0, 0.12);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.landscape-download-chip:disabled {
  cursor: wait;
  opacity: 0.82;
  transform: none;
}

.landscape-download-chip--loading .landscape-download-chip-action {
  min-width: 94px;
}

.landscape-download-chip-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.landscape-download-chip-name {
  overflow: hidden;
  color: var(--text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.landscape-download-chip-format {
  padding: 3px 10px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.03);
  font-size: 12px;
  font-weight: 900;
}

.landscape-download-chip-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--brand-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.landscape-download-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  border-radius: 50%;
  animation: landscapeDownloadSpin 0.7s linear infinite;
}

@keyframes landscapeDownloadSpin {
  to { transform: rotate(360deg); }
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

@keyframes landscapeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1280px) {
  .landscape-head {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .landscape-title-cell {
    grid-column: 1 / -1;
  }

  .qc-control-slot {
    grid-column: 1;
    justify-content: center;
  }

  .landscape-toolbar {
    grid-column: 2;
    justify-content: center;
  }

  .landscape-chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .landscape-chart-region {
    min-height: 390px;
  }
}

@media (max-width: 760px) {
  .landscape-head {
    grid-template-columns: 1fr;
  }

  .qc-control-slot,
  .landscape-toolbar {
    grid-column: 1;
    justify-content: flex-start;
  }

  .landscape-chart-grid {
    grid-template-columns: 1fr;
  }

  .landscape-chart-region {
    min-height: 340px;
  }
}
</style>

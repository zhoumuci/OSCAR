<template>
  <section class="sample-landscape-card float-card">
    <div class="landscape-head">
      <div class="landscape-title-block">
        <div class="landscape-title">Sample landscape</div>
        <div class="landscape-sub">Aggregated sample-level visualizations for <span class="mono">{{ datasetId }}</span></div>
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
      <article class="landscape-chart-region" aria-label="Cell type composition">
        <button
          class="chart-download-button"
          type="button"
          :disabled="!canDownloadComposition"
          :title="canDownloadComposition ? 'Download chart as PNG' : 'Chart image unavailable'"
          :aria-label="`Download ${datasetId} cell type composition chart`"
          @click.stop="downloadCompositionChart"
        >
          <el-icon><Download /></el-icon>
        </button>
        <CellTypeCompositionChart
          ref="compositionChartRef"
          :data="composition.data"
          :loading="composition.loading"
          :error="composition.error"
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
          :title="canDownloadUmap ? 'Download chart as PNG' : 'Chart image unavailable'"
          :aria-label="`Download ${datasetId} ${selectedEmbedding} ${selectedColorBy} embedding chart`"
          @click.stop="downloadUmapChart"
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
</template>

<script setup lang="ts">
import { Download } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { computed, reactive, ref, watch } from "vue";
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

const props = defineProps<{
  datasetId: string;
  domain: SearchResultDomain;
}>();

type ChartState<T> = {
  data: T | null;
  loading: boolean;
  error: boolean;
};

const composition = reactive<ChartState<CellTypeCompositionData>>(createChartState());
const qc = reactive<ChartState<QcViolinData>>(createChartState());
const umap = reactive<ChartState<UmapData>>(createChartState());
const selectedEmbedding = ref<SearchResultEmbedding>("umap");
const selectedColorBy = ref<SearchResultColorBy>("celltype");

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

const colorByOptions: Array<{ value: SearchResultColorBy; label: string }> = [
  { value: "celltype", label: "Cell type" },
  { value: "cluster", label: "Cluster" },
];

const canDownloadComposition = computed(() => {
  return !composition.loading && !composition.error && (composition.data?.items?.length ?? 0) > 0;
});

const canDownloadQc = computed(() => {
  return !qc.loading && !qc.error && Boolean(qc.data?.metrics?.some((metric) => metric.groups?.length));
});

const canDownloadUmap = computed(() => {
  return !umap.loading && !umap.error && (umap.data?.points?.length ?? 0) > 0;
});

let lastLandscapeRequestKey = "";
let lastUmapRequestKey = "";
let landscapeLoadToken = 0;
let umapLoadToken = 0;

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
  ElMessage.warning("Chart image is not ready yet.");
}

function triggerChartDownload(chartRef: ChartExportHandle | null, filename: string) {
  if (!chartRef?.downloadImage(filename)) {
    showDownloadUnavailableMessage();
  }
}

function downloadCompositionChart() {
  if (!canDownloadComposition.value) {
    showDownloadUnavailableMessage();
    return;
  }

  const datasetPart = sanitizeFilenamePart(props.datasetId);
  triggerChartDownload(compositionChartRef.value, `${datasetPart}_celltype_composition.png`);
}

function downloadQcChart() {
  if (!canDownloadQc.value) {
    showDownloadUnavailableMessage();
    return;
  }

  const datasetPart = sanitizeFilenamePart(props.datasetId);
  triggerChartDownload(qcChartRef.value, `${datasetPart}_qc_distribution.png`);
}

function downloadUmapChart() {
  if (!canDownloadUmap.value) {
    showDownloadUnavailableMessage();
    return;
  }

  const datasetPart = sanitizeFilenamePart(props.datasetId);
  const embeddingPart = sanitizeFilenamePart(selectedEmbedding.value);
  const colorPart = sanitizeFilenamePart(selectedColorBy.value);
  triggerChartDownload(umapChartRef.value, `${datasetPart}_${embeddingPart}_${colorPart}.png`);
}

async function loadLandscape(datasetId: string, domain: SearchResultDomain) {
  const normalizedDatasetId = datasetId.trim();
  const requestKey = `${normalizedDatasetId}::${domain}`;
  if (!normalizedDatasetId || requestKey === lastLandscapeRequestKey) return;

  lastLandscapeRequestKey = requestKey;
  const currentToken = ++landscapeLoadToken;

  startLoading(composition);
  startLoading(qc);

  const [compositionResult, qcResult] = await Promise.allSettled([
    fetchCellTypeComposition({ datasetId: normalizedDatasetId, domain, groupBy: "celltype" }),
    fetchQcViolin({ datasetId: normalizedDatasetId, domain, groupBy: "celltype" }),
  ]);

  if (currentToken !== landscapeLoadToken) return;

  if (compositionResult.status === "fulfilled") {
    finishWithData(composition, compositionResult.value);
  } else {
    finishWithError(composition, compositionResult.reason, "cell type composition");
  }

  if (qcResult.status === "fulfilled") {
    finishWithData(qc, qcResult.value);
  } else {
    finishWithError(qc, qcResult.reason, "QC distribution");
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
  if (!normalizedDatasetId || requestKey === lastUmapRequestKey) return;

  lastUmapRequestKey = requestKey;
  const currentToken = ++umapLoadToken;

  startLoading(umap);

  try {
    const data = await fetchUmap({ datasetId: normalizedDatasetId, domain, embedding, colorBy });
    if (currentToken !== umapLoadToken) return;
    finishWithData(umap, data);
  } catch (error) {
    if (currentToken !== umapLoadToken) return;
    finishWithError(umap, error, "UMAP");
  }
}

watch(() => [props.datasetId, props.domain] as const, ([nextDatasetId, nextDomain]) => {
  loadLandscape(nextDatasetId, nextDomain);
}, { immediate: true });

watch(() => [props.datasetId, props.domain, selectedEmbedding.value, selectedColorBy.value] as const, ([
  nextDatasetId,
  nextDomain,
  nextEmbedding,
  nextColorBy,
]) => {
  loadUmap(nextDatasetId, nextDomain, nextEmbedding, nextColorBy);
}, { immediate: true });
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
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.landscape-title-block {
  min-width: 240px;
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

.landscape-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
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
  .landscape-chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .landscape-chart-region {
    min-height: 390px;
  }
}

@media (max-width: 760px) {
  .landscape-chart-grid {
    grid-template-columns: 1fr;
  }

  .landscape-chart-region {
    min-height: 340px;
  }

  .landscape-head {
    align-items: flex-start;
  }

  .landscape-toolbar {
    justify-content: flex-start;
  }
}
</style>

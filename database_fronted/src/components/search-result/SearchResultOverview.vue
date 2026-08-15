<template>
  <section class="search-result-overview-card float-card">
    <div class="overview-head">
      <div>
        <div class="overview-title">Overview</div>
        <div class="overview-sub">Sample summary for <span class="mono">{{ datasetId }}</span></div>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="3" />

    <div v-else class="overview-layout">
      <div class="overview-content-grid">
        <div v-for="item in visibleItems" :key="item.label" class="overview-item">
          <div class="item-label">{{ item.label }}</div>
          <div class="item-value" :class="{ muted: item.value === '-' }">
            {{ item.value }}
          </div>
        </div>
      </div>

      <div class="overview-download-panel">
        <button
          v-if="downloadRow"
          class="overview-download-button"
          type="button"
          @click="openDownloadDialog"
        >
          <span class="download-label">Download</span>
          <span class="download-sub">Open sample files</span>
        </button>

        <button v-else class="overview-download-button disabled" type="button" disabled>
          <span class="download-label">No download</span>
          <span class="download-sub">Resource unavailable</span>
        </button>
      </div>
    </div>

    <div v-if="error" class="overview-note">Overview data is currently unavailable.</div>
    <DownloadFilesDialog v-model="downloadDialogOpen" :row="downloadRow" />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { buildDownloads } from "@/api/download";
import type { DownloadRow } from "@/api/download";
import type { SearchResultOverviewData } from "@/api/searchResult";
import DownloadFilesDialog from "@/components/download/DownloadFilesDialog.vue";

const props = defineProps<{
  datasetId: string;
  overview: SearchResultOverviewData | null;
  loading?: boolean;
  error?: boolean;
}>();

type OverviewDisplayItem = {
  label: string;
  value: string;
};

const visibleItems = computed<OverviewDisplayItem[]>(() => {
  const overview = props.overview;

  return [
    { label: "DatasetID", value: formatValue(overview?.datasetId || props.datasetId) },
    { label: "Species", value: formatValue(overview?.species) },
    { label: "Tissue", value: formatValue(overview?.tissue) },
    { label: "Disease", value: formatValue(overview?.disease) },
    { label: "Sample Type", value: formatValue(overview?.sampleType) },
    { label: "Sample Name", value: formatValue(overview?.sampleName) },
    { label: "Cells", value: formatNumber(overview?.cells ?? overview?.sampleNumber) },
    { label: "Platform", value: formatValue(overview?.platform) },
  ];
});

const downloadDialogOpen = ref(false);

const downloadRow = computed<DownloadRow | null>(() => {
  const datasetId = formatValue(props.overview?.datasetId || props.datasetId);
  if (datasetId === "-") return null;
  return {
    datasetId,
    sampleType: formatValue(props.overview?.sampleType),
    tissue: formatValue(props.overview?.tissue),
    sampleName: formatValue(props.overview?.sampleName),
    cells: numericValue(props.overview?.cells ?? props.overview?.sampleNumber),
    platform: formatValue(props.overview?.platform),
    sourceId: formatValue(props.overview?.sourceId),
    disease: formatValue(props.overview?.disease),
    sampleSource: formatValue(props.overview?.sampleSource),
    downloads: buildDownloads(datasetId),
  };
});

function openDownloadDialog() {
  if (!downloadRow.value) return;
  downloadDialogOpen.value = true;
}

function formatValue(value: unknown): string {
  const normalizedValue = String(value ?? "").trim();
  return normalizedValue || "-";
}

function formatNumber(value: number | undefined): string {
  return typeof value === "number" && Number.isFinite(value) ? value.toLocaleString() : "-";
}

function numericValue(value: unknown): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

</script>

<style scoped>
.search-result-overview-card {
  position: relative;
  overflow: hidden;
  padding: 16px 18px 18px;
  margin-bottom: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  animation: overviewIn 0.28s ease both;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.search-result-overview-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.overview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.overview-title {
  font-weight: 900;
  font-size: 18px;
}

.overview-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.overview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 14px;
  align-items: stretch;
}

.overview-content-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
}

.overview-item {
  min-width: 0;
  padding: 9px 11px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #ffffffc7;
}

.item-label {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.item-value {
  margin-top: 4px;
  min-height: 19px;
  overflow: hidden;
  color: var(--text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted,
.overview-note {
  color: var(--muted);
}

.overview-download-panel {
  position: relative;
  z-index: 1;
  isolation: isolate;
  min-height: 100%;
  overflow: hidden;
  border-radius: 15px;
  contain: paint;
}

.overview-download-button {
  -webkit-appearance: none;
  appearance: none;
  position: relative;
  z-index: 0;
  isolation: isolate;
  overflow: hidden;
  width: 100%;
  height: 100%;
  min-height: 106px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--nav-active-border);
  border-radius: 15px;
  background: var(--nav-active-bg);
  background-clip: padding-box;
  color: var(--nav-active-text);
  box-shadow:
    inset 0 1px 0 #ffffff9c,
    0 4px 12px rgba(95, 125, 112, 0.16);
  cursor: pointer;
  text-align: center;
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.overview-download-button:hover {
  transform: translateY(-2px);
  box-shadow:
    inset 0 1px 0 #ffffffb8,
    0 7px 16px rgba(95, 125, 112, 0.20);
  filter: saturate(1.03);
}

.overview-download-button.disabled {
  border-color: var(--border);
  background: linear-gradient(135deg, #f4f7f6 0%, #e9efed 100%);
  cursor: not-allowed;
  opacity: 0.72;
  filter: grayscale(0.15);
}

.overview-download-button.disabled:hover {
  transform: none;
  box-shadow: inset 0 1px 0 #ffffff73, inset 0 -1px 0 #1b2a2714;
}

.download-label {
  font-size: 18px;
  font-weight: 900;
}

.download-sub {
  font-size: 12px;
  font-weight: 800;
  opacity: 0.76;
}

.overview-note {
  margin-top: 10px;
  font-size: 13px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

@keyframes overviewIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1100px) {
  .overview-layout {
    grid-template-columns: 1fr;
  }

  .overview-content-grid {
    grid-template-columns: repeat(2, minmax(130px, 1fr));
  }

  .overview-download-button {
    min-height: 78px;
  }
}

@media (max-width: 560px) {
  .overview-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .overview-content-grid {
    grid-template-columns: 1fr;
  }
}
</style>

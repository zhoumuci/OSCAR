<template>
  <div class="db-page">
    <div class="container">
      <div class="db-layout">
        <aside class="left" v-loading="facetLoading && viewState === 'ready'">
          <!-- Active filters -->
          <div v-if="activeFilters.length > 0" class="active-filters">
            <span class="af-label">Active filters:</span>
            <span
              v-for="f in activeFilters"
              :key="f.label"
              class="af-chip"
            >
              <span class="af-chip-key">{{ f.key }}</span>
              <span class="af-chip-val">{{ f.label }}</span>
              <button type="button" class="af-chip-x" @click="onSelectFacet(f.facetKey, f.label)">&times;</button>
            </span>
            <button type="button" class="af-clear" @click="clearAllFilters">Clear all</button>
          </div>

          <!-- Species info chip (display-only, single species) -->
          <div v-if="showSpeciesChip" class="species-info">
            <div class="species-icon">🧬</div>
            <div class="species-text">
              <span class="species-label">Species</span>
              <span class="species-value">{{ facets.species[0]?.label ?? 'Homo sapiens' }}</span>
            </div>
          </div>

          <FacetCard
            v-for="panel in visiblePanels"
            :key="panel.key"
            :title="panel.title"
            :items="facets[panel.key]"
            :selected="filters[panel.key]"
            @select="(label: string) => onSelectFacet(panel.key, label)"
          />
        </aside>

        <main class="right">
          <div class="topbar">
            <span class="module-badge">DATA</span>
          </div>

          <div class="search-row">
            <div class="search-label">Search:</div>
            <el-input
              v-model="keyword"
              placeholder="Please enter the search content"
              clearable
              @keyup.enter="onSearch"
            />
            <el-button type="primary" :loading="sampleLoading" @click="onSearch">Search</el-button>
          </div>

          <div v-if="viewState === 'loading'" class="float-card state-card">
            <el-skeleton animated :rows="10" />
          </div>

          <div v-else-if="viewState === 'error'" class="float-card state-card state-box">
            <div class="state-title">{{ errorTitle }}</div>
            <div class="state-msg">{{ errorMsg }}</div>
            <div class="state-actions">
              <el-button type="primary" @click="loadBrowseData">Retry</el-button>
            </div>
          </div>

          <template v-else>
            <el-table
              :data="rows"
              stripe
              border
              class="tbl"
              @sort-change="onTableSortChange"
            >
              <el-table-column prop="datasetId" label="DatasetID" min-width="140" fixed sortable="custom">
                <template #default="{ row }">
                  <el-link
                    type="primary"
                    underline="never"
                    class="dataset-link"
                    @click="openDataset(row.datasetId)"
                  >
                    {{ formatText(row.datasetId) }}
                  </el-link>
                </template>
              </el-table-column>

              <el-table-column prop="sampleType" label="Sample Type" min-width="120" sortable="custom" />
              <el-table-column prop="tissue" label="Tissue" min-width="100" sortable="custom" />
              <el-table-column prop="sampleName" label="Sample Name" min-width="180" sortable="custom" />
              <el-table-column prop="cells" label="Cells" min-width="110" align="center" sortable="custom">
                <template #default="{ row }">
                  {{ formatCells(row.cells) }}
                </template>
              </el-table-column>
              <el-table-column prop="platform" label="Platform" min-width="120" sortable="custom" />
              <el-table-column prop="sourceId" label="SourceID" min-width="120" sortable="custom" />
              <el-table-column prop="disease" label="Disease" min-width="110" sortable="custom" />
              <el-table-column prop="sampleSource" label="Sample Source" min-width="150" sortable="custom" />
            </el-table>

            <div v-if="!sampleLoading && rows.length === 0" class="table-empty">
              <el-empty description="No results. Try adjusting filters or keyword." />
            </div>

            <div class="pager" v-if="total > 0">
              <el-pagination
                background
                layout="total, prev, pager, next, jumper"
                :total="total"
                :page-size="pageSize"
                :current-page="page"
                @current-change="onPageChange"
              />
            </div>
          </template>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import axios from "axios";
import type {
  BrowseFacetKey,
  BrowseFacetQuery,
  BrowseFacetResponse,
  BrowseSample,
  BrowseSampleQuery,
} from "@/api/databrowse";
import { fetchBrowseFacets, fetchBrowseSamples } from "@/api/databrowse";
import FacetCard from "@/components/FacetCard.vue";

type ViewState = "loading" | "ready" | "error";

const router = useRouter();
const route = useRoute();

const facetPanels: Array<{ key: BrowseFacetKey; title: string }> = [
  { key: "species", title: "Species" },
  { key: "sampleType", title: "Biosample type" },
  { key: "tissue", title: "Tissue Type" },
];

const keyword = ref("");
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const rows = ref<BrowseSample[]>([]);
const sortBy = ref("datasetId");
const sortDir = ref<"asc" | "desc">("asc");

const filters = reactive<Record<BrowseFacetKey, string>>({
  species: "",
  sampleType: "",
  tissue: "",
});

const activeFilters = computed(() => {
  const result: Array<{ facetKey: BrowseFacetKey; key: string; label: string }> = [];
  const keyLabels: Record<BrowseFacetKey, string> = {
    species: "Species",
    sampleType: "Sample Type",
    tissue: "Tissue",
  };
  for (const panel of facetPanels) {
    const val = filters[panel.key];
    if (val) result.push({ facetKey: panel.key, key: keyLabels[panel.key] ?? panel.key, label: val });
  }
  return result;
});

const showSpeciesChip = computed(() => {
  const items = facets.species;
  return items.length === 1;
});

const visiblePanels = computed(() => {
  return facetPanels.filter((panel) => {
    if (panel.key === "species") return !showSpeciesChip.value; // hide if chip, show if multi
    return true;
  });
});

function clearAllFilters() {
  filters.species = "";
  filters.sampleType = "";
  filters.tissue = "";
  page.value = 1;
  loadBrowseData();
}

const facets = reactive<BrowseFacetResponse>({
  species: [],
  sampleType: [],
  tissue: [],
});

const viewState = ref<ViewState>("loading");
const sampleLoading = ref(false);
const facetLoading = ref(false);
const errorCode = ref<number | null>(null);
const errorMsg = ref("");

const errorTitle = computed(() => {
  if (errorCode.value === 429) return "Too Many Requests";
  if (errorCode.value === 403) return "Access Restricted";
  return "Request Failed";
});

function currentFacetQuery(): BrowseFacetQuery {
  const trimmedKeyword = keyword.value.trim();

  return {
    keyword: trimmedKeyword || undefined,
    species: filters.species || undefined,
    sampleType: filters.sampleType || undefined,
    tissue: filters.tissue || undefined,
  };
}

function currentSampleQuery(): BrowseSampleQuery {
  return {
    ...currentFacetQuery(),
    page: page.value,
    pageSize: pageSize.value,
    sortBy: sortBy.value,
    sortDir: sortDir.value,
  };
}

function applyFacets(data: BrowseFacetResponse) {
  facets.species = data.species ?? [];
  facets.sampleType = data.sampleType ?? [];
  facets.tissue = data.tissue ?? [];
}

function applySamples(data: { records: BrowseSample[]; total: number; page: number; pageSize: number }) {
  rows.value = data.records ?? [];
  total.value = data.total ?? 0;
  page.value = data.page || page.value;
  pageSize.value = data.pageSize || pageSize.value;
}

function clearRows() {
  rows.value = [];
  total.value = 0;
}

function clearFacets() {
  facets.species = [];
  facets.sampleType = [];
  facets.tissue = [];
}

function clearBrowseData() {
  clearRows();
  clearFacets();
}

function getErrorStatus(error: unknown): number | null {
  if (!error || typeof error !== "object" || !("response" in error)) return null;

  const response = (error as { response?: { status?: unknown } }).response;
  return typeof response?.status === "number" ? response.status : null;
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) return error.message;
  return "Request failed. Please retry.";
}

function setError(error: unknown) {
  const status = getErrorStatus(error);
  errorCode.value = status;

  if (status === 429) {
    errorMsg.value = "You are sending requests too frequently. Please try again later.";
  } else if (status === 403) {
    errorMsg.value = "Your access is temporarily restricted. Please try again later.";
  } else {
    errorMsg.value = getErrorMessage(error);
  }

  console.error("[Browse API] Request failed:", error);
  viewState.value = "ready";
}

async function loadBrowseData() {
  sampleLoading.value = true;
  facetLoading.value = true;
  errorCode.value = null;
  errorMsg.value = "";

  if (viewState.value !== "ready") {
    viewState.value = "loading";
  }

  try {
    const [facetData, sampleData] = await Promise.all([
      fetchBrowseFacets(currentFacetQuery()),
      fetchBrowseSamples(currentSampleQuery()),
    ]);

    applyFacets(facetData);
    applySamples(sampleData);
    viewState.value = "ready";
  } catch (error) {
    // 被 axios 去重拦截器取消的请求不是错误：新请求会正常回填数据
    if (axios.isCancel(error)) return;
    clearBrowseData();
    setError(error);
  } finally {
    sampleLoading.value = false;
    facetLoading.value = false;
  }
}

async function loadSamplesOnly() {
  sampleLoading.value = true;
  errorCode.value = null;
  errorMsg.value = "";

  try {
    applySamples(await fetchBrowseSamples(currentSampleQuery()));
    viewState.value = "ready";
  } catch (error) {
    // 被 axios 去重拦截器取消的请求不是错误：新请求会正常回填数据
    if (axios.isCancel(error)) return;
    // 只清表格数据：facets 不属于本函数的职责范围，保持上次成功的结果
    clearRows();
    setError(error);
  } finally {
    sampleLoading.value = false;
  }
}

function onSearch() {
  page.value = 1;
  loadBrowseData();
}

// Auto-search when user clears the search box
watch(keyword, (newVal, oldVal) => {
  if (oldVal.trim() && !newVal.trim()) {
    page.value = 1;
    loadBrowseData();
  }
});

function onSelectFacet(facet: BrowseFacetKey, label: string) {
  filters[facet] = filters[facet] === label ? "" : label;
  page.value = 1;
  loadBrowseData();
}

function onTableSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (order) {
    sortBy.value = prop;
    sortDir.value = order === "descending" ? "desc" : "asc";
  } else {
    sortBy.value = "datasetId";
    sortDir.value = "asc";
  }
  page.value = 1;
  loadSamplesOnly();
}

function onPageChange(nextPage: number) {
  page.value = nextPage;
  loadSamplesOnly();
}

function openDataset(datasetId: string) {
  if (!datasetId) return;

  router.push({
    name: "SampleDetail",
    params: { id: datasetId },
    query: { domain: "integration", source: "browse" },
  });
}

function formatText(value: string) {
  return value || "-";
}

function formatCells(value: number | null | undefined) {
  return typeof value === "number" && Number.isFinite(value) ? value.toLocaleString() : "-";
}

onMounted(() => {
  const q = route.query;
  if (q.keyword) keyword.value = String(q.keyword);
  if (q.facet && q.facetValue) {
    const facetKey = String(q.facet);
    if (facetKey === "tissue" || facetKey === "sampleType") {
      filters[facetKey] = String(q.facetValue);
    }
  }
  loadBrowseData();
});
</script>

<style scoped>
.db-page {
  --browse-teal: var(--brand-primary-3);
  --browse-teal-hover: #7f9a90;
  --browse-teal-active: #6f887d;
  --browse-teal-soft: var(--nav-active-bg);
  --browse-teal-border: var(--nav-active-border);
  --browse-teal-focus: #8fa59c40;
  --el-color-primary: var(--browse-teal);
  --el-color-primary-dark-2: var(--browse-teal-active);
  --el-color-primary-light-3: var(--brand-primary);
  --el-color-primary-light-5: var(--brand-primary-2);
  --el-color-primary-light-7: #dbe4e0;
  --el-color-primary-light-8: var(--surface-3);
  --el-color-primary-light-9: var(--surface-2);
  width: 100%;
  padding: 18px 0 30px;
  background: var(--bg);
}

.db-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: start;
}

.left {
  position: sticky;
  top: 88px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 108px);
  overflow-y: auto;
  padding-right: 4px;
}

/* ── Active filters ─────────────────────────── */
.active-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.af-label {
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
  margin-right: 2px;
}

.af-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: #f4f8f6;
  font-size: 11px;
  font-weight: 800;
}

.af-chip-key {
  color: var(--muted);
}

.af-chip-val {
  color: var(--text);
}

.af-chip-x {
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.af-chip-x:hover {
  background: var(--surface-3);
  color: var(--text);
}

.af-clear {
  border: none;
  background: transparent;
  color: var(--brand-primary-3);
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
}
.af-clear:hover {
  background: var(--surface-3);
}

/* ── Species info chip ──────────────────────── */
.species-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #f6f9f7, var(--surface));
  border: 1px solid rgba(80, 100, 90, 0.16);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(31, 48, 42, 0.06);
}

.species-icon {
  font-size: 24px;
  line-height: 1;
}

.species-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.species-label {
  font-size: 11px;
  font-weight: 800;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.species-value {
  font-size: 15px;
  font-weight: 900;
  color: var(--text);
}

.left::-webkit-scrollbar {
  width: 8px;
}

.left::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 999px;
}

.right {
  min-width: 0;
}

.topbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 12px;
}

.module-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 7px 16px;
  border: 1px solid var(--nav-active-border);
  border-radius: 12px;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  box-shadow: 0 8px 18px rgba(27, 92, 84, 0.12);
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.search-row {
  display: grid;
  grid-template-columns: 70px 1fr 110px;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.search-label {
  color: var(--text);
  font-weight: 800;
}

.search-row :deep(.el-input) {
  --el-input-focus-border-color: var(--browse-teal);
}

.search-row :deep(.el-input__wrapper) {
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.search-row :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--browse-teal-border) inset;
}

.search-row :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--browse-teal) inset,
    0 0 0 3px var(--browse-teal-focus);
}

.search-row :deep(.el-button--primary),
.state-actions :deep(.el-button--primary) {
  --el-button-bg-color: var(--browse-teal);
  --el-button-border-color: var(--browse-teal);
  --el-button-hover-bg-color: var(--browse-teal-hover);
  --el-button-hover-border-color: var(--browse-teal-hover);
  --el-button-active-bg-color: var(--browse-teal-active);
  --el-button-active-border-color: var(--browse-teal-active);
  background: linear-gradient(135deg, var(--brand-primary), var(--browse-teal));
  border-color: var(--browse-teal);
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.22);
  color: var(--surface);
  font-weight: 700;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease,
    border-color 0.18s ease;
}

.search-row :deep(.el-button--primary:hover),
.state-actions :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  background: linear-gradient(135deg, var(--browse-teal-hover), var(--browse-teal-active));
  border-color: var(--browse-teal-hover);
  box-shadow: 0 10px 22px rgba(95, 125, 112, 0.26);
}

.search-row :deep(.el-button--primary:active),
.state-actions :deep(.el-button--primary:active) {
  transform: translateY(0);
  background: var(--browse-teal-active);
  border-color: var(--browse-teal-active);
  box-shadow: 0 5px 12px rgba(95, 125, 112, 0.2);
}

.search-row :deep(.el-button--primary:focus-visible),
.state-actions :deep(.el-button--primary:focus-visible) {
  outline: 2px solid var(--browse-teal-focus);
  outline-offset: 2px;
}

.search-row :deep(.el-button--primary.is-disabled),
.search-row :deep(.el-button--primary.is-disabled:hover),
.state-actions :deep(.el-button--primary.is-disabled),
.state-actions :deep(.el-button--primary.is-disabled:hover) {
  transform: none;
  background: var(--brand-primary-2);
  border-color: var(--border-brand);
  box-shadow: none;
  color: var(--muted);
}

.tbl {
  border-radius: 14px;
  overflow: hidden;
  position: relative;
}

:deep(.tbl th.el-table__cell),
:deep(.tbl td.el-table__cell) {
  text-align: center;
  vertical-align: middle;
  padding: 10px 8px;
  white-space: nowrap;
}

:deep(.tbl th.el-table__cell.is-sortable .cell) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

:deep(.tbl th.el-table__cell.is-sortable .caret-wrapper) {
  flex: 0 0 auto;
}

.dataset-link {
  color: var(--browse-teal);
  font-weight: 800;
}

.tbl :deep(.dataset-link.el-link),
.tbl :deep(.dataset-link.el-link--primary) {
  --el-link-text-color: var(--browse-teal);
  --el-link-hover-text-color: var(--browse-teal-active);
  border-radius: 8px;
  color: var(--browse-teal);
  padding: 2px 6px;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.tbl :deep(.dataset-link.el-link:hover),
.tbl :deep(.dataset-link.el-link:focus-visible) {
  background: color-mix(in srgb, var(--browse-teal-soft) 52%, transparent);
  box-shadow: 0 0 0 1px var(--browse-teal-border) inset;
  color: var(--browse-teal-active);
}

.tbl :deep(.dataset-link.el-link:focus-visible) {
  outline: 2px solid var(--browse-teal-focus);
  outline-offset: 2px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.pager :deep(.el-pagination) {
  --el-color-primary: var(--browse-teal);
  --el-pagination-hover-color: var(--browse-teal-active);
  --el-pagination-button-bg-color: var(--surface);
  --el-pagination-button-disabled-bg-color: var(--surface-2);
}

.pager :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--browse-teal);
  border-color: var(--browse-teal);
  color: var(--surface);
  font-weight: 800;
}

.pager :deep(.el-pagination.is-background .el-pager li:not(.is-active):hover),
.pager :deep(.el-pagination.is-background button:not(:disabled):hover) {
  background: var(--browse-teal-soft);
  color: var(--browse-teal-active);
}

.pager :deep(.el-pagination .el-pager li:focus-visible),
.pager :deep(.el-pagination button:focus-visible) {
  outline: 2px solid var(--browse-teal-focus);
  outline-offset: 2px;
}

.pager :deep(.el-pagination .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--browse-teal) inset,
    0 0 0 3px var(--browse-teal-focus);
}

.state-card {
  padding: 14px;
}

.state-box {
  text-align: center;
  padding: 26px 14px;
}

.state-title {
  font-weight: 900;
  font-size: 16px;
  margin-bottom: 8px;
}

.state-msg {
  color: var(--muted);
  margin-bottom: 14px;
}

.state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-empty {
  padding: 10px 0 0;
}

@media (max-width: 1100px) {
  .db-layout {
    grid-template-columns: 1fr;
  }

  .left {
    position: static;
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
}
@media (max-width: 480px) {
  .search-row { grid-template-columns: 1fr; gap: 8px; }
  .search-label { width: 100%; }
}
</style>

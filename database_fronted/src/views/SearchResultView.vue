<template>
  <div class="sr-page">
    <div class="container">
      <div class="page-title">Sample details</div>
      <div class="page-sub">Explore single-cell multi-omics profiles and regulatory annotations for this sample.</div>

      <div class="float-card hint-card">
        <div class="hint-left">
          <div class="hint-title">Query</div>
          <div class="hint-chips">
            <span v-for="c in queryChips" :key="c" class="qchip">{{ c }}</span>
          </div>
        </div>
        <div ref="switchRef" class="data-view-switch" role="radiogroup" aria-label="Data view">
          <div
            class="data-view-slider"
            :style="sliderStyle"
          />
          <button
            v-for="(option, idx) in dataViewOptions"
            :key="option.value"
            :ref="(el) => { if (el) btnRefs[idx] = el as HTMLElement }"
            type="button"
            class="data-view-button"
            :class="{ active: selectedDomain === option.value }"
            :aria-pressed="selectedDomain === option.value"
            @click="setSelectedDomain(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
        <div class="hint-right">
          <el-button class="btn" @click="goBack">{{ backLabel }}</el-button>
        </div>
      </div>

      <div v-if="state === 'idle'" class="float-card block state-box">
        <div class="state-title">No query provided</div>
        <div class="state-msg">Sorry, you did not submit the query content, can not query data information for you..</div>
        <div class="state-actions">
          <el-button type="primary" @click="goBack">{{ backLabel }}</el-button>
        </div>
      </div>

      <div v-else-if="state === 'error'" class="float-card block state-box">
        <div class="state-title">{{ errorTitle }}</div>
        <div class="state-msg">{{ errorMsg }}</div>
        <div class="state-actions">
          <el-button type="primary" @click="load">Retry</el-button>
          <el-button class="btn" @click="goBack">{{ backLabel }}</el-button>
        </div>
      </div>

      <template v-else>
        <SearchResultOverview
          :dataset-id="datasetId"
          :overview="overviewData"
          :loading="overviewLoading"
          :error="overviewError"
        />

        <SampleLandscapeSection
          :dataset-id="datasetId"
          :domain="selectedDomain"
          :prefetch-enabled="state === 'ready'"
        />

        <div class="deferred-sections-anchor">
          <Suspense v-if="deferredSectionsEnabled">
            <div>
              <RegulatoryAnnotationSection
                :dataset-id="datasetId"
                :domain="selectedDomain"
                :overview="overviewData"
              />

              <RegulatoryNetworkSection
                v-if="showRegulatoryNetwork"
                :dataset-id="datasetId"
                :domain="selectedDomain"
              />
            </div>
            <template #fallback>
              <div class="float-card deferred-loading-card" role="status" aria-live="polite">
                <div class="deferred-loading-title">Loading regulatory sections…</div>
                <el-skeleton animated :rows="4" />
              </div>
            </template>
          </Suspense>
          <div v-else class="float-card deferred-loading-card deferred-loading-card--queued" role="status" aria-live="polite">
            <div class="deferred-loading-title">Preparing regulatory sections…</div>
            <el-skeleton animated :rows="3" />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { SearchResultDomain, SearchResultOverviewData } from "@/api/searchResult";
import { fetchSearchResultOverview } from "@/api/searchResult";

import SearchResultOverview from "@/components/search-result/SearchResultOverview.vue";
import SampleLandscapeSection from "@/components/search-result/SampleLandscapeSection.vue";
const RegulatoryAnnotationSection = defineAsyncComponent(() => import("@/components/search-result/MarkerGenesSection.vue"));
const RegulatoryNetworkSection = defineAsyncComponent(() => import("@/components/search-result/RegulatoryNetworkSection.vue"));

type DataViewDomain = SearchResultDomain;

type ViewState = "idle" | "loading" | "ready" | "error";

const route = useRoute();
const router = useRouter();

const dataViewOptions: Array<{ value: DataViewDomain; label: string }> = [
  { value: "integration", label: "Integration" },
  { value: "rna", label: "RNA" },
  { value: "atac", label: "ATAC" },
];

const selectedDomain = computed<DataViewDomain>(() => {
  const d = String(route.query.domain || "").toLowerCase();
  if (d === "integration" || d === "rna" || d === "atac") return d;
  return "integration";
});

// ---- domain switch slider animation ----
const switchRef = ref<HTMLElement | null>(null);
const btnRefs = ref<Record<number, HTMLElement>>({});
const sliderStyle = ref<Record<string, string>>({ width: "0px", transform: "translateX(0px)" });

function updateSlider() {
  const idx = dataViewOptions.findIndex((o) => o.value === selectedDomain.value);
  const el = btnRefs.value[idx];
  const container = switchRef.value;
  if (!el || !container) return;
  const containerRect = container.getBoundingClientRect();
  const btnRect = el.getBoundingClientRect();
  sliderStyle.value = {
    width: `${btnRect.width}px`,
    transform: `translateX(${btnRect.left - containerRect.left}px)`,
  };
}

watch(selectedDomain, () => nextTick(updateSlider));
let domainSwitchResizeObserver: ResizeObserver | null = null;
onMounted(() => {
  nextTick(updateSlider);
  if (switchRef.value) {
    domainSwitchResizeObserver = new ResizeObserver(updateSlider);
    domainSwitchResizeObserver.observe(switchRef.value);
  }
});





const primaryIdFromQuery = computed(() => {
  // Temporary local development fallback until every entry route passes datasetId.
  const routeId = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
  return String(route.query.id || routeId || route.query.q || "H_000001");
});

const datasetId = computed(() => primaryIdFromQuery.value);

const state = ref<ViewState>("loading");
const errorMsg = ref<string>("");
const errorCode = ref<number | null>(null);
const overviewData = ref<SearchResultOverviewData | null>(null);
const overviewLoading = ref(false);
const overviewError = ref(false);

const errorTitle = computed(() => {
  if (errorCode.value === 429) return "Too Many Requests";
  if (errorCode.value === 403) return "Access Restricted";
  return "Request Failed";
});

const showRegulatoryNetwork = computed(() => selectedDomain.value === "integration");

const deferredSectionsEnabled = ref(false);

function enableDeferredSections() {
  if (deferredSectionsEnabled.value) return;
  deferredSectionsEnabled.value = true;
}

function resetDeferredSections() {
  deferredSectionsEnabled.value = false;
}

const queryChips = computed(() => {
  const q = route.query;
  const chips: string[] = [];
  if (q.mode) chips.push(`mode=${String(q.mode)}`);
  chips.push(`domain=${selectedDomain.value}`);
  if (q.field) chips.push(`field=${String(q.field)}`);
  if (q.q) chips.push(`q=${String(q.q)}`);
  if (q.id) chips.push(`id=${String(q.id)}`);
  if (chips.length === 0) chips.push("id=H_000001");
  return chips;
});

const backSource = computed(() => (route.query.source as string) || "browse");
const backLabel = computed(() => {
  const map: Record<string, string> = { analysis: "Back to Analysis", search: "Back to Search", home: "Back to Home", browse: "Back to Browse", download: "Back to Download" };
  return map[backSource.value] || "Back";
});
const backPath = computed(() => {
  const map: Record<string, string> = { analysis: "/analysis", search: "/search", home: "/", browse: "/browse", download: "/download" };
  return map[backSource.value] || "/browse";
});
function goBack() { router.push(backPath.value); }

function setSelectedDomain(nextDomain: DataViewDomain) {
  if (String(route.query.domain || "").toLowerCase() === nextDomain) return;

  router.push({
    path: route.path,
    query: {
      ...route.query,
      domain: nextDomain,
    },
  });
}

let loadToken = 0;

async function load() {
  const myToken = ++loadToken;
  const currentDatasetId = datasetId.value;

  state.value = "loading";
  errorMsg.value = "";
  errorCode.value = null;
  overviewData.value = null;
  overviewError.value = false;
  overviewLoading.value = true;

  try {
    const data = await fetchSearchResultOverview(currentDatasetId);
    if (myToken !== loadToken) return;
    overviewData.value = data;
  } catch (error) {
    if (myToken !== loadToken) return;
    console.error("[SearchResult] Failed to load overview:", error);
    overviewError.value = true;
  } finally {
    if (myToken === loadToken) {
      overviewLoading.value = false;
      state.value = "ready";
      await nextTick();
      enableDeferredSections();
    }
  }
}

onMounted(() => {
  void load();
});
watch(datasetId, () => {
  resetDeferredSections();
  void load();
});

onBeforeUnmount(() => {
  ++loadToken;
  domainSwitchResizeObserver?.disconnect();
  domainSwitchResizeObserver = null;
});
</script>

<style scoped>
.sr-page {
  --browse-teal: var(--brand-primary-3);
  --browse-teal-hover: #7f9a90;
  --browse-teal-active: #6f887d;
  --browse-teal-focus: #8fa59c40;
  --el-color-primary: var(--browse-teal);
  width: 100%;
  padding: 18px 0 10px;
  background: var(--bg);
}

.deferred-sections-anchor {
  min-height: 1px;
}

.deferred-loading-card {
  min-height: 168px;
  padding: 18px;
  margin-bottom: 14px;
  background: var(--surface);
}

.deferred-loading-card--queued {
  opacity: 0.82;
}

.deferred-loading-title {
  margin-bottom: 12px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 850;
}

.page-title {
  font-size: 32px;
  font-weight: 900;
  margin: 6px 0 6px;
}

.page-sub {
  color: var(--muted);
  margin-bottom: 12px;
}

.hint-card {
  width: 100%;
  padding: 12px 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.hint-card:hover {
  border-color: var(--border-brand);
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.hint-left {
  min-width: 0;
  justify-self: start;
}

.hint-right {
  display: flex;
  justify-self: end;
}

.hint-title {
  font-weight: 900;
  font-size: 18px;
  margin-bottom: 6px;
}

.hint-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.qchip {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface);
  font-weight: 700;
  font-size: 12px;
}

.data-view-switch {
  position: relative;
  display: inline-flex;
  justify-self: center;
  align-items: center;
  gap: 4px;
  min-height: 44px;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #ffffffd9;
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 6px 16px #1218260d;
}

.data-view-slider {
  position: absolute;
  top: 4px;
  left: 0;
  height: calc(100% - 8px);
  border-radius: 999px;
  background: var(--nav-active-bg);
  border: 1px solid var(--nav-active-border);
  box-shadow:
    inset 0 1px 0 #ffffff9c,
    0 4px 12px rgba(95, 125, 112, 0.20);
  transition:
    transform 0.42s cubic-bezier(0.34, 1.56, 0.64, 1),
    width 0.42s cubic-bezier(0.34, 1.56, 0.64, 1);
  pointer-events: none;
  z-index: 0;
}

.data-view-button {
  position: relative;
  z-index: 1;
  appearance: none;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font: inherit;
  min-height: 34px;
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  padding: 8px 20px;
  transition:
    color 0.22s ease;
}

.data-view-button:hover {
  color: var(--text);
}

.data-view-button.active {
  color: var(--nav-active-text);
}

.data-view-button:focus-visible {
  outline: 2px solid #8fa59c40;
  outline-offset: 2px;
}

.btn {
  border: 1px solid var(--border);
  background: var(--surface);
}

.btn:hover {
  border-color: var(--border-brand);
  background: var(--surface-2);
  color: var(--browse-teal-active);
}

.block {
  padding: 14px;
  margin-bottom: 14px;
}

.state-box {
  text-align: center;
  padding: 28px 14px;
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
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

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
  font-weight: 800;
}

.state-actions :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  background: linear-gradient(135deg, var(--browse-teal-hover), var(--browse-teal-active));
  border-color: var(--browse-teal-hover);
  box-shadow: 0 10px 22px rgba(95, 125, 112, 0.26);
}

@media (max-width: 760px) {
  .hint-card {
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .hint-left,
  .data-view-switch,
  .hint-right {
    justify-self: stretch;
  }

  .data-view-switch {
    justify-content: center;
    width: max-content;
    max-width: 100%;
    justify-self: center;
  }

  .hint-right {
    justify-content: flex-start;
  }
}
@media (max-width: 480px) {
  .hint-card { gap: 10px; padding: 12px; }
  .hint-chips { flex-wrap: wrap; }
}
</style>

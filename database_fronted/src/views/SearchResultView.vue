<template>
  <div class="sr-page">
    <div class="container">
      <div class="page-title">Search Result</div>
      <div class="page-sub">Welcome to our database to search for the data you need.</div>

      <div class="float-card hint-card">
        <div class="hint-left">
          <div class="hint-title">Query</div>
          <div class="hint-chips">
            <span v-for="c in queryChips" :key="c" class="qchip">{{ c }}</span>
          </div>
        </div>
        <div class="data-view-switch" role="radiogroup" aria-label="Data view">
          <button
            v-for="option in dataViewOptions"
            :key="option.value"
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
          <el-button class="btn" @click="goBackToSearch">Back to Search</el-button>
        </div>
      </div>

      <div v-if="state === 'loading'" class="float-card block">
        <el-skeleton animated :rows="10" />
      </div>

      <div v-else-if="state === 'idle'" class="float-card block state-box">
        <div class="state-title">No query provided</div>
        <div class="state-msg">Sorry, you did not submit the query content, can not query data information for you..</div>
        <div class="state-actions">
          <el-button type="primary" @click="goBackToSearch">Back to Search</el-button>
        </div>
      </div>

      <div v-else-if="state === 'error'" class="float-card block state-box">
        <div class="state-title">{{ errorTitle }}</div>
        <div class="state-msg">{{ errorMsg }}</div>
        <div class="state-actions">
          <el-button type="primary" @click="load">Retry</el-button>
          <el-button class="btn" @click="goBackToSearch">Back to Search</el-button>
        </div>
      </div>

      <template v-else>
        <SearchResultOverview
          :dataset-id="datasetId"
          :overview="overviewData"
          :loading="overviewLoading"
          :error="overviewError"
        />

        <SampleLandscapeSection :dataset-id="datasetId" :domain="selectedDomain" />

        <RegulatoryAnnotationSection
          :dataset-id="datasetId"
          :domain="selectedDomain"
          :overview="overviewData"
          :demo-mode="demoMode"
          :demo-size="demoSize"
        />

        <component :is="domainExtraComp" :payload="payload" />

        <RegulatoryNetworkSection
          v-if="showRegulatoryNetwork"
          :dataset-id="datasetId"
          :domain="selectedDomain"
          :demo-mode="demoMode"
          :demo-size="demoSize"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { SearchResultDomain, SearchResultOverviewData } from "@/api/searchResult";
import { fetchSearchResultOverview } from "@/api/searchResult";

import IntegrationExtra from "@/components/IntegrationResult.vue";
import RNAExtra from "@/components/RNAResult.vue";
import ATACExtra from "@/components/ATACResult.vue";
import SearchResultOverview from "@/components/search-result/SearchResultOverview.vue";
import SampleLandscapeSection from "@/components/search-result/SampleLandscapeSection.vue";
import RegulatoryAnnotationSection from "@/components/search-result/MarkerGenesSection.vue";
import RegulatoryNetworkSection from "@/components/search-result/RegulatoryNetworkSection.vue";
import { normalizeDemoDataSize, type DemoDataSize } from "@/mock/searchResultDemoData";

type DataViewDomain = SearchResultDomain;

type Payload = {
  domain: DataViewDomain;
  primaryId?: string;
};

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

const demoMode = computed(() => {
  const queryDemo = String(route.query.demo || "").toLowerCase();
  const envDemo = String(import.meta.env.VITE_OSCAR_DEMO_DATA || "").toLowerCase();

  return queryDemo === "1" || queryDemo === "true" || envDemo === "true";
});

const demoSize = computed<DemoDataSize>(() => normalizeDemoDataSize(route.query.demoSize));

const primaryIdFromQuery = computed(() => {
  // Temporary local development fallback until every entry route passes datasetId.
  const routeId = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
  return String(route.query.id || routeId || route.query.q || "H_000001");
});

const datasetId = computed(() => primaryIdFromQuery.value);

const state = ref<ViewState>("idle");
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

const payload = computed<Payload>(() => ({
  domain: selectedDomain.value,
  primaryId: datasetId.value,
}));

const showRegulatoryNetwork = computed(() => selectedDomain.value === "integration");

const queryChips = computed(() => {
  const q = route.query;
  const chips: string[] = [];
  if (q.mode) chips.push(`mode=${String(q.mode)}`);
  chips.push(`domain=${selectedDomain.value}`);
  if (q.field) chips.push(`field=${String(q.field)}`);
  if (q.q) chips.push(`q=${String(q.q)}`);
  if (q.id) chips.push(`id=${String(q.id)}`);
  if (demoMode.value) chips.push("demo=1");
  if (demoMode.value) chips.push(`demoSize=${demoSize.value}`);
  if (chips.length === 0) chips.push("id=H_000001");
  return chips;
});

function goBackToSearch() {
  router.push({ path: "/search" });
}

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

const domainExtraComp = computed(() => {
  const map: Record<DataViewDomain, any> = {
    integration: IntegrationExtra,
    rna: RNAExtra,
    atac: ATACExtra,
  };
  return map[selectedDomain.value];
});

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
    }
  }
}

onMounted(load);
watch(datasetId, () => load());
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
  display: inline-flex;
  justify-self: center;
  align-items: center;
  gap: 6px;
  min-height: 46px;
  padding: 5px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #ffffffd9;
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 8px 18px #1218260f;
}

.data-view-button {
  appearance: none;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font: inherit;
  min-height: 36px;
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  padding: 9px 20px;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.data-view-button:hover {
  background: var(--surface-2);
  border-color: var(--border-brand);
  color: var(--text);
}

.data-view-button.active {
  background: var(--nav-active-bg);
  border-color: var(--nav-active-border);
  color: var(--nav-active-text);
  box-shadow:
    inset 0 1px 0 #ffffff9c,
    0 6px 14px rgba(95, 125, 112, 0.18);
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
</style>

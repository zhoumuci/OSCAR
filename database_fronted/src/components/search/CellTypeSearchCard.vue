<template>
  <div class="gsc-root">
    <p class="gsc-desc">Identify OSCAR samples containing specific cell types for cross-dataset regulatory analysis.</p>

    <div class="gsc-workbench">
      <!-- LEFT: input card -->
      <div class="cte-card gsc-main-card">
        <span class="cte-field-label">Cell type query</span>
        <div class="gsc-celltype-picker">
          <span class="gsc-celltype-picker-icon" aria-hidden="true">
            <span></span><span></span><span></span>
          </span>
          <el-select
            v-model="selectedCellType"
            class="gsc-celltype-select"
            popper-class="oscar-select-popper gsc-celltype-popper"
            placeholder="Search and select a cell type"
            filterable
            clearable
            :loading="cellTypeOptionsLoading"
            :disabled="loading"
          >
            <el-option
              v-for="option in cellTypeOptions"
              :key="option.name"
              :label="option.name"
              :value="option.name"
            >
              <span class="gsc-celltype-option">
                <span class="gsc-celltype-option-name"><i></i>{{ option.name }}</span>
                <span class="gsc-celltype-option-count">{{ option.count.toLocaleString() }} samples</span>
              </span>
            </el-option>
          </el-select>
        </div>
        <div class="gsc-hint-row">
          <span class="cte-hint">Choose one standardized cell type. Start typing to filter the available options.</span>
        </div>
        <div class="gsc-filter-row">
          <label class="cte-field"><span class="cte-field-label">Per page</span>
            <el-select v-model="resultSize" class="cte-select" popper-class="oscar-select-popper" :disabled="loading">
              <el-option label="10" :value="10" /><el-option label="20" :value="20" /><el-option label="50" :value="50" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Sort by</span>
            <el-select v-model="sortBy" class="cte-select" popper-class="oscar-select-popper" :disabled="loading" @change="onSortByChange">
              <el-option label="Dataset ID" value="sampleId" />
              <el-option label="Cell Counts" value="cellCount" />
              <el-option label="Matched cell count" value="matchedCellCount" />
            </el-select>
          </label>
        </div>
        <div class="gsc-btn-row">
          <button type="button" class="primary-btn" :disabled="loading || !selectedCellType" @click="doSearch">
            <svg width="16" height="16" viewBox="0 0 20 20" fill="none"><circle cx="9" cy="9" r="6" stroke="currentColor" stroke-width="1.5"/><line x1="14" y1="14" x2="18" y2="18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg> Search
          </button>
          <button type="button" class="soft-btn" :disabled="loading" @click="resetAll">Reset</button>
        </div>
        <div class="gsc-how-brief">
          <div class="gsc-how-title">How it works</div>
          <div class="gsc-how-list">
            <div class="gsc-how-item"><span class="gsc-how-dot">1</span> Select one standardized cell type from the searchable list.</div>
            <div class="gsc-how-item"><span class="gsc-how-dot">2</span> Click Search to find OSCAR samples annotated with the selected cell type.</div>
            <div class="gsc-how-item"><span class="gsc-how-dot">3</span> Browse sample profiles and regulatory results per cell type.</div>
            <div class="gsc-how-item"><span class="gsc-how-dot">💡</span> Click any cell type in the donut chart on the right to search instantly.</div>
          </div>
        </div>
      </div>

      <div class="gsc-side">
        <div class="cte-card">
          <button
            type="button"
            class="chart-dl-btn"
            title="Download chart as PNG"
            @click.stop="downloadChartPng"
          >
            <el-icon><Download /></el-icon>
          </button>
          <div class="cte-card-title">TOP 12 Cell Types</div>
          <div ref="donutEl" class="gsc-donut"></div>
          <p class="gsc-donut-hint">Click a cell type to search it instantly.</p>
        </div>
      </div>
    </div>

    <div class="gsc-divider"></div>

    <div v-if="hasResults" class="gsc-results">
      <div class="gsc-summary-row">
        <div class="gsc-summary-card"><span class="gsc-sum-num">{{ results.matchedSamples }}</span><span class="gsc-sum-label">Matched samples</span></div>
      </div>
      <div class="gsc-res-head">
        <span class="gsc-res-title">Associated samples</span>
        <button type="button" class="gsc-dl-btn" title="Download all results as CSV" @click="downloadTableCsv">
          <el-icon><Download /></el-icon>
        </button>
      </div>
      <div class="cte-table-wrap">
        <table class="cte-table"><thead><tr><th class="gsc-sort-th" @click="toggleHeaderSort('sampleId')">Dataset ID <span class="gsc-sort-arrow">{{ headerSortArrow('sampleId') }}</span></th><th>Tissue</th><th>Sample name</th><th class="gsc-sort-th" @click="toggleHeaderSort('cellCount')">Cells <span class="gsc-sort-arrow">{{ headerSortArrow('cellCount') }}</span></th><th>Platform</th><th>Source ID</th><th>Disease</th><th>Sample source</th></tr></thead><tbody>
          <tr v-if="!rows.length"><td colspan="8" class="gsc-empty">No samples found.</td></tr>
          <tr v-for="r in paginatedRows" :key="r.sampleId">
            <td><a @click.stop="router.push({name:'SampleDetail',params:{id:r.sampleId},query:{domain:'integration',source:'search'}})" class="gsc-link"><code>{{ r.sampleId }}</code></a></td>
            <td>{{ r.tissue || '—' }}</td><td>{{ r.sampleName || '—' }}</td>
            <td>{{ (r.cellCount || 0).toLocaleString() }}</td>
            <td>{{ r.platform || '—' }}</td><td>{{ r.sourceId || '—' }}</td><td>{{ r.disease || '—' }}</td><td>{{ r.sampleSource || '—' }}</td>
          </tr>
        </tbody></table>
        <div class="cte-pagination" v-if="totalPages > 1">
          <span class="cte-page-info">{{ page }} / {{ totalPages }}</span>
          <div class="cte-page-btns">
            <button type="button" class="cte-page-btn" :disabled="page <= 1" @click="page--">‹</button>
            <button type="button" class="cte-page-btn active">{{ page }}</button>
            <button type="button" class="cte-page-btn" :disabled="page >= totalPages" @click="page++">›</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Download } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import axios from "axios";
import * as echarts from "echarts";
import { buildApiUrl } from "@/config/api";
import { downloadCsv } from "@/utils/downloadCsv";
import { downloadChart } from "@/utils/downloadChart";

const router = useRouter();
const props = defineProps<{ active?: boolean }>();
const loading = ref(false); const hasResults = ref(false);
const selectedCellType = ref(""); const resultSize = ref(10); const sortBy = ref("sampleId");
const sortDir = ref<"asc" | "desc">("asc");
const cellTypeOptions = ref<Array<{ name: string; count: number }>>([]);
const cellTypeOptionsLoading = ref(false);

function toggleHeaderSort(col: string) {
  if (sortBy.value === col) {
    sortDir.value = sortDir.value === "asc" ? "desc" : "asc";
  } else {
    sortBy.value = col;
    sortDir.value = col === "sampleId" ? "asc" : "desc";
  }
  applySort();
}

function headerSortArrow(col: string): string {
  if (sortBy.value !== col) return "⇅";
  return sortDir.value === "asc" ? "▲" : "▼";
}

function onSortByChange() {
  sortDir.value = sortBy.value === "sampleId" ? "asc" : "desc";
  page.value = 1;
  applySort();
}

const page = ref(1); const pageSize = computed(() => resultSize.value);
watch(resultSize, () => { page.value = 1; });
const rows = ref<any[]>([]); const results = ref({ matchedSamples: 0 });

const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize.value)));
const paginatedRows = computed(() => { const s = (page.value - 1) * pageSize.value; return rows.value.slice(s, s + pageSize.value); });

/* donut chart */
const donutEl = ref<HTMLDivElement | null>(null);
let donutChart: echarts.ECharts | null = null;

async function renderDonut() {
  if (!donutEl.value) return;
  cellTypeOptionsLoading.value = true;
  try {
    const [{ data: standardizedTypes }, { data: counts }] = await Promise.all([
      axios.get(buildApiUrl("api/search/cell-types")),
      axios.get(buildApiUrl("api/search/cell-type-counts")),
    ]);
    const countByType = new Map<string, number>(
      (Array.isArray(counts) ? counts : []).map((d: any) => [String(d.cellType || "").trim(), Number(d.cnt) || 0]),
    );
    cellTypeOptions.value = (Array.isArray(standardizedTypes) ? standardizedTypes : [])
      .map((name: any) => String(name || "").trim())
      .filter((name: string) => name)
      .map((name: string) => ({ name, count: countByType.get(name) || 0 }))
      .sort((a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name));
    if (!Array.isArray(counts) || !counts.length) return;
    const top = counts.slice(0, 12);
    if (donutChart && donutChart.getDom() !== donutEl.value) disposeDonut();
    if (!donutChart) donutChart = echarts.init(donutEl.value);
    const colors = ["#8FA59C","#7BA7C9","#E8A87C","#C4A882","#8FC9B3","#D4A0C4","#D9826B","#B088C0","#A8C8D8","#E8936B","#D4956A","#8FB89C"];
    donutChart.setOption({
      tooltip: { trigger: "item", formatter: "{b}: {c} samples" },
      series: [{
        type: "pie", radius: ["45%","75%"], center: ["50%","45%"],
        data: top.map((d: any) => ({ name: d.cellType, value: d.cnt })),
        label: { fontSize: 12, color: "#5E6C67" },
        itemStyle: { borderColor: "#fff", borderWidth: 1.5 },
        color: colors,
      }],
    }, true);
    donutChart.off("click");
    donutChart.on("click", (p: any) => {
      selectedCellType.value = p.name;
      nextTick(() => doSearch());
    });
    requestAnimationFrame(() => donutChart?.resize());
  } catch {}
  finally { cellTypeOptionsLoading.value = false; }
}

function disposeDonut() {
  donutChart?.dispose();
  donutChart = null;
}

onMounted(() => {
  if (props.active !== false) nextTick(renderDonut);
});

watch(() => props.active, async (active) => {
  if (!active) return;
  await nextTick();
  await renderDonut();
});

onBeforeUnmount(disposeDonut);

async function doSearch() {
  const cellType = selectedCellType.value.trim();
  if (!cellType) { ElMessage.warning("Please select a cell type."); return; }
  loading.value = true; hasResults.value = false;
  try {
    const { data } = await axios.post(buildApiUrl("api/search/cell-type"), { cellType });
    results.value = { matchedSamples: data.summary.matchedSamples };
    rows.value = data.samples.map((s: any) => ({
      sampleId: s.sampleId,
      sampleName: s.sampleName,
      tissue: s.tissue,
      cellCount: s.cellCount ?? s.cell_count ?? 0,
      platform: s.platform,
      sourceId: s.sourceId ?? s.source_id,
      disease: s.disease,
      sampleSource: s.sampleSource ?? s.sample_source,
      matchedCellCount: s.matchedCells ?? s.matched_cells ?? 0,
    }));
    applySort();
    page.value = 1; hasResults.value = true;
  } catch (e: any) { ElMessage.error("Search failed."); }
  finally { loading.value = false; }
}

function applySort() {
  const dir = sortDir.value === "asc" ? 1 : -1;
  if (sortBy.value === "sampleId") {
    rows.value.sort((a: any, b: any) => dir * (a.sampleId || "").localeCompare(b.sampleId || ""));
  } else if (sortBy.value === "cellCount") {
    rows.value.sort((a: any, b: any) => dir * ((a.cellCount || 0) - (b.cellCount || 0)));
  } else if (sortBy.value === "matchedCellCount") {
    rows.value.sort((a: any, b: any) => dir * ((a.matchedCellCount || 0) - (b.matchedCellCount || 0)));
  }
}

function resetAll() { selectedCellType.value = ""; resultSize.value = 10; sortBy.value = "sampleId"; sortDir.value = "asc"; hasResults.value = false; rows.value = []; }

function downloadTableCsv() {
  if (!rows.value.length) return;
  const headers = ["Dataset ID", "Tissue", "Sample name", "Cells", "Platform", "Source ID", "Disease", "Sample source"];
  const data = rows.value.map(r => [
    r.sampleId ?? "", r.tissue ?? "", r.sampleName ?? "",
    String(r.cellCount ?? r.cell_count ?? 0), r.platform ?? "",
    r.sourceId ?? "", r.disease ?? "", r.sampleSource ?? "",
  ]);
  downloadCsv("oscar_celltype_search.csv", headers, data);
}

function downloadChartPng() {
  if (!donutChart) return;
  downloadChart(donutChart, "oscar_celltype_distribution.png");
}
</script>

<style scoped>
/* same core styles as GeneSearchCard, abbreviated */
.gsc-root { display: flex; flex-direction: column; gap: 14px; }
.gsc-desc { margin: 0; color: var(--muted); font-size: 14px; font-weight: 750; }
.gsc-workbench { display: grid; grid-template-columns: minmax(0, 3fr) minmax(250px, 2fr); gap: 14px; align-items: stretch; }
.gsc-main-card { display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.gsc-side { display: flex; flex-direction: column; gap: 14px; min-width: 0; height: 100%; }
.gsc-side > .cte-card { flex: 1; display: flex; flex-direction: column; }
.gsc-side > .cte-card .gsc-donut { flex: 1; }
.gsc-how-brief { margin-top: auto; padding-top: 10px; border-top: 1px solid var(--border); }
.gsc-how-title { font-size: 13px; font-weight: 900; color: var(--text); margin-bottom: 6px; }
.gsc-how-list { display: flex; flex-direction: column; gap: 6px; }
.gsc-how-item { font-size: 11px; font-weight: 600; color: var(--muted); line-height: 1.5; display: flex; gap: 6px; align-items: baseline; }
.gsc-how-dot { flex-shrink: 0; width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: rgba(143,165,156,0.12); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; }
.gsc-btn-row { display: flex; gap: 8px; flex-wrap: wrap; }
.gsc-filter-row { display: flex; align-items: flex-end; gap: 16px; }
.gsc-filter-row .cte-field { flex: 1 1 140px; }
.gsc-hint-row { display: flex; align-items: center; justify-content: space-between; }
.gsc-divider { height: 1px; background: var(--border); }
.gsc-empty { padding: 32px; text-align: center; color: var(--muted); font-size: 14px; }
.gsc-results { display: flex; flex-direction: column; gap: 12px; }
.gsc-summary-row { display: flex; gap: 10px; }
.gsc-summary-card { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 14px 18px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-card); }
.gsc-sum-num { font-size: 22px; font-weight: 900; color: var(--text); }
.gsc-sum-label { font-size: 10px; font-weight: 700; color: var(--muted); text-transform: uppercase; }
.gsc-res-head { display: flex; align-items: center; justify-content: space-between; }
.gsc-res-title { font-weight: 900; font-size: 14px; }
.gsc-dl-btn { appearance: none; display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid var(--border-brand); border-radius: 999px; background: #fffffff2; color: var(--brand-primary-3); box-shadow: inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor: pointer; transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, transform 0.18s ease; }
.gsc-dl-btn:hover { border-color: var(--nav-active-border); background: var(--surface-2); color: var(--text); box-shadow: inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16); transform: translateY(-1px); }
.gsc-dl-btn :deep(.el-icon) { font-size: 15px; }
.cte-field { display: flex; flex-direction: column; gap: 5px; }
.cte-field-label { font-size: 14px; font-weight: 900; color: rgba(39,66,58,0.84); }
.cte-hint { font-size: 12px; font-weight: 600; color: var(--muted); }
.gsc-celltype-picker { position: relative; padding: 1px; border-radius: 16px; background: linear-gradient(120deg, rgba(95,125,112,0.72), rgba(123,167,201,0.68), rgba(143,201,179,0.72)); box-shadow: 0 12px 28px rgba(95,125,112,0.14); transition: transform 0.2s ease, box-shadow 0.2s ease; }
.gsc-celltype-picker:focus-within { transform: translateY(-1px); box-shadow: 0 16px 34px rgba(95,125,112,0.20), 0 0 0 4px rgba(143,165,156,0.10); }
.gsc-celltype-picker-icon { position: absolute; top: 50%; left: 18px; z-index: 2; display: grid; grid-template-columns: repeat(2, 8px); grid-template-rows: repeat(2, 8px); gap: 3px; transform: translateY(-50%) rotate(45deg); pointer-events: none; }
.gsc-celltype-picker-icon span { display: block; border-radius: 3px; background: var(--brand-primary-3); box-shadow: 0 2px 5px rgba(39,66,58,0.14); }
.gsc-celltype-picker-icon span:nth-child(2) { background: #7ba7c9; }
.gsc-celltype-picker-icon span:nth-child(3) { grid-column: 1 / 3; width: 8px; justify-self: center; background: #8fc9b3; }
.gsc-celltype-select { width: 100%; }
.gsc-celltype-select :deep(.el-select__wrapper) { min-height: 58px; padding: 8px 16px 8px 58px; border: 0; border-radius: 15px; background: linear-gradient(135deg, rgba(255,255,255,0.98), rgba(244,249,247,0.96)); box-shadow: inset 0 1px 0 rgba(255,255,255,0.9); }
.gsc-celltype-select :deep(.el-select__wrapper.is-focused) { box-shadow: inset 0 1px 0 rgba(255,255,255,0.9) !important; }
.gsc-celltype-select :deep(.el-select__placeholder) { color: rgba(39,66,58,0.54); font-size: 14px; font-weight: 750; }
.gsc-celltype-select :deep(.el-select__selected-item) { color: var(--text); font-size: 15px; font-weight: 900; }
.gsc-celltype-option { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 18px; }
.gsc-celltype-option-name { display: inline-flex; align-items: center; gap: 8px; font-weight: 750; }
.gsc-celltype-option-name i { width: 7px; height: 7px; border-radius: 999px; background: linear-gradient(135deg, var(--brand-primary-3), #7ba7c9); box-shadow: 0 0 0 3px rgba(143,165,156,0.12); }
.gsc-celltype-option-count { color: var(--muted); font-size: 11px; font-weight: 700; }
.cte-textarea { width: 100%; box-sizing: border-box; min-height: 56px; padding: 12px 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface-2); color: var(--text); font-family: "JetBrains Mono",monospace; font-size: 14px; line-height: 1.5; resize: vertical; }
.cte-textarea:focus { outline: none; border-color: var(--border-brand); box-shadow: 0 0 0 3px rgba(143,165,156,0.14); }
.gsc-parse-stats { font-size: 11px; font-weight: 700; color: var(--brand-primary-3); }
.cte-select { width: 100%; }
.primary-btn { display: inline-flex; align-items: center; gap: 6px; min-height: 38px; padding: 10px 18px; border: none; border-radius: 11px; background: var(--brand-primary-3); color: #fff; font-size: 14px; font-weight: 900; cursor: pointer; box-shadow: 0 4px 12px rgba(95,125,112,0.18); }
.primary-btn:hover:not(:disabled) { background: #7f9f94; }
.primary-btn:disabled { opacity: .55; cursor: not-allowed; }
.soft-btn { display: inline-flex; align-items: center; gap: 5px; min-height: 38px; padding: 10px 16px; border: 1px solid var(--border); border-radius: 11px; background: var(--surface); color: var(--text); font-size: 14px; font-weight: 800; cursor: pointer; }
.soft-btn:hover:not(:disabled) { border-color: var(--border-brand); background: var(--surface-2); }
.soft-btn:disabled { opacity: .5; cursor: not-allowed; }
.cte-table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: 11px; background: var(--surface); }
.cte-table { width: 100%; border-collapse: collapse; font-size: 14px; color: #606266; }
.cte-table th { padding: 10px 14px; text-align: center; vertical-align: middle; border-bottom: 1px solid var(--border); white-space: nowrap; background: var(--surface-2); font-weight: 600; color: #909399; font-size: 14px; }
.cte-table td { padding: 10px 14px; text-align: center; vertical-align: middle; border-bottom: 1px solid var(--border); white-space: nowrap; }
.cte-table code { font-size: 13px; color: var(--brand-primary-3); font-weight: 700; }
.gsc-sort-th { cursor: pointer; user-select: none; }
.gsc-sort-th:hover { color: var(--brand-primary-3); }
.gsc-sort-arrow { font-size: 10px; margin-left: 2px; }
.gsc-link { color: var(--brand-primary-3); font-weight: 700; cursor: pointer; text-decoration: none; }
.gsc-link:hover { text-decoration: underline; }
.cte-pagination { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-top: 1px solid var(--border); }
.cte-page-info { font-size: 12px; font-weight: 600; color: var(--muted); }
.cte-page-btns { display: flex; gap: 4px; }
.cte-page-btn { min-height: 28px; padding: 2px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--surface); color: var(--text); font-size: 12px; font-weight: 700; cursor: pointer; }
.cte-page-btn.active { background: var(--brand-primary-3); color: #fff; border-color: var(--brand-primary-3); }
.cte-page-btn:disabled { opacity: .4; cursor: not-allowed; }

.cte-card { position: relative; box-sizing: border-box; background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-card); }
.cte-card-title { font-size: 15px; font-weight: 900; margin: 0 0 6px; color: var(--text); }
.chart-dl-btn { appearance: none; position: absolute; top: 8px; right: 8px; z-index: 2; display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid var(--border-brand); border-radius: 999px; background: #fffffff2; color: var(--brand-primary-3); box-shadow: inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor: pointer; transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, transform 0.18s ease; }
.chart-dl-btn:hover { border-color: var(--nav-active-border); background: var(--surface-2); color: var(--text); box-shadow: inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16); transform: translateY(-1px); }
.chart-dl-btn :deep(.el-icon) { font-size: 15px; }
.gsc-donut { width: 100%; height: 340px; }
.gsc-donut-hint { margin: 6px 0 0; font-size: 11px; font-weight: 600; color: var(--muted); text-align: center; }

@media (max-width: 760px) {
  .gsc-root,
  .gsc-workbench,
  .gsc-main-card,
  .gsc-side,
  .cte-card,
  .gsc-results,
  .cte-table-wrap {
    max-width: 100%;
    min-width: 0;
  }

  .gsc-workbench { grid-template-columns: minmax(0, 1fr); }
  .gsc-filter-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .gsc-filter-row .cte-field { min-width: 0; }
  .gsc-hint-row { align-items: flex-start; flex-direction: column; gap: 6px; }
  .gsc-btn-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .primary-btn,
  .soft-btn,
  .cte-textarea,
  .cte-select,
  .gsc-filter-row :deep(.el-select),
  .gsc-filter-row :deep(.el-input),
  .gsc-filter-row :deep(.el-input__wrapper),
  .gsc-filter-row :deep(.el-select__wrapper),
  .gsc-filter-row :deep(.el-textarea) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }
  .gsc-donut {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    flex: 0 0 300px;
    height: 300px;
    overflow: hidden;
  }
  .gsc-side > .cte-card .gsc-donut {
    flex: 0 0 300px;
  }
  .gsc-donut :deep(canvas) {
    max-width: 100% !important;
  }
  .cte-pagination { flex-wrap: wrap; gap: 8px; }
}

@media (max-width: 480px) {
  .gsc-workbench { grid-template-columns: minmax(0, 1fr); }
  .gsc-side { order: -1; }
  .gsc-filter-row,
  .gsc-btn-row { grid-template-columns: 1fr; }
  .gsc-summary-row { flex-direction: column; }
  .gsc-summary-card { width: 100%; }
  .gsc-res-head { flex-wrap: wrap; gap: 8px; }
  .gsc-donut,
  .gsc-side > .cte-card .gsc-donut {
    flex-basis: 260px;
    height: 260px;
  }
}
</style>

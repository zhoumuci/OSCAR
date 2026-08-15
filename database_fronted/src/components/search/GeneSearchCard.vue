<template>
  <div class="gsc-root">
    <p class="gsc-desc">Find OSCAR samples associated with your genes of interest.</p>

    <!-- Gene query label above both columns -->
    <span class="cte-query-label">
      <span class="cte-field-label">Gene query</span>
      <span class="cte-max-badge">MAX input: 200 genes</span>
    </span>

    <!-- textarea + filters (left) || buttons (right) -->
    <div class="gsc-input-row">
      <div class="gsc-input-left">
        <textarea v-model="geneInput" class="cte-textarea" :class="{ 'cte-textarea--error': displayedInputError }" rows="3" placeholder="MS4A1" :disabled="loading" @input="onTextareaInput"></textarea>
        <span class="cte-hint">Enter one or more human gene symbols, separated by comma, space, or line break.</span>
        <div v-if="displayedInputError" class="input-feedback-card" role="alert">
          <span class="input-feedback-card__icon" aria-hidden="true">!</span>
          <span class="input-feedback-card__body">
            <strong>Check your gene input</strong>
            <span>{{ displayedInputError }}</span>
          </span>
          <button v-if="!geneLimitExceeded" type="button" class="input-feedback-card__close" aria-label="Dismiss input message" @click="clearInputError">&times;</button>
        </div>
        <div class="cte-btn-row">
          <span class="spg-upload-wrap">
            <button type="button" class="soft-btn" :disabled="loading" @click="uploadFile">
              <span>📎</span> Upload file
            </button>
            <el-tooltip placement="top" effect="light" :show-after="200">
              <template #content>
                <div><div>Accepted files: .txt, .csv, and .tsv.</div><div>The parser reads a recognised gene column when one is present; otherwise it reads gene symbols from the text cells. Duplicate symbols are removed.</div></div>
              </template>
              <span class="spg-help-icon">?</span>
            </el-tooltip>
          </span>
          <button type="button" class="soft-btn" :disabled="loading" @click="loadExample">
            <span>📋</span> Load example
          </button>
          <button type="button" class="soft-btn" :disabled="loading" @click="clearInput">
            <span>✕</span> Clear
          </button>
        </div>
        <input ref="fileInputRef" type="file" accept=".txt,.csv,.tsv" style="display:none" @change="onFileSelected" />
        <div v-if="geneInput.trim()" class="gsc-stat-row">
          <div class="gsc-stat gsc-stat--muted"><span class="gsc-stat-num">{{ geneStats.total }}</span><span class="gsc-stat-label">Input genes</span></div>
          <div class="gsc-stat"><span class="gsc-stat-num">{{ geneStats.valid }}</span><span class="gsc-stat-label">Valid genes</span></div>
          <div class="gsc-stat" :class="{ 'gsc-stat--bad': geneStats.invalid > 0 }"><span class="gsc-stat-num">{{ geneStats.invalid }}</span><span class="gsc-stat-label">Invalid tokens</span></div>
          <div v-if="geneLimitExceeded" class="gsc-stat gsc-stat--warn"><span class="gsc-stat-num">!</span><span class="gsc-stat-label">Max 200 exceeded</span></div>
        </div>
        <div class="gsc-filter-row">
          <label class="cte-field"><span class="cte-field-label">Sort by</span>
            <el-select v-model="sortBy" class="cte-select" popper-class="oscar-select-popper" :disabled="loading" @change="applySelectedSort">
              <el-option label="Dataset ID" value="sampleId" />
              <el-option label="Cell counts" value="cellCount" />
              <el-option label="Matched genes" value="matchedGenes" :disabled="parsedGenes.length <= 1" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Per page</span>
            <el-select v-model="resultSize" class="cte-select" popper-class="oscar-select-popper" :disabled="loading">
              <el-option label="10" :value="10" />
              <el-option label="20" :value="20" />
              <el-option label="50" :value="50" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Signal Type</span>
            <el-select v-model="signalType" class="cte-select" popper-class="oscar-select-popper" :disabled="loading">
              <el-option label="Gene expression markers" value="gene_expression" />
              <el-option label="Gene score markers" value="gene_score" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Tissue</span>
            <el-select
              v-model="tissue"
              class="cte-select"
              popper-class="oscar-select-popper"
              :disabled="loading || tissueLoading"
              :loading="tissueLoading"
              :placeholder="tissuePlaceholder"
              filterable
              clearable
              @visible-change="handleTissueVisible"
            >
              <el-option label="All" value="" />
              <el-option v-for="t in tissueOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </label>
        </div>
      </div>
      <div class="gsc-input-right">
        <button type="button" class="primary-btn gsc-search-btn" :disabled="loading || !parsedGenes.length || geneLimitExceeded" @click="doSearch">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="9" cy="9" r="6" stroke="currentColor" stroke-width="1.5"/><line x1="14" y1="14" x2="18" y2="18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          Search
        </button>
        <button type="button" class="soft-btn gsc-reset-btn" :disabled="loading" @click="resetAll">Reset</button>
      </div>
    </div>

    <div class="gsc-divider"></div>

    <!-- results -->
    <div v-if="hasResults" class="gsc-results">
      <!-- summary cards -->
      <div class="gsc-summary-row">
        <div class="gsc-summary-card">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><rect x="2" y="3" width="14" height="12" rx="2" stroke="var(--brand-primary-3)" stroke-width="1.2"/><line x1="2" y1="7" x2="16" y2="7" stroke="var(--brand-primary-3)" stroke-width="1"/><line x1="6" y1="7" x2="6" y2="15" stroke="var(--brand-primary-3)" stroke-width="1"/></svg>
          <span class="gsc-sum-num">{{ result.matchedSamples }}</span>
          <span class="gsc-sum-label">Matched samples</span>
        </div>
        <div class="gsc-summary-card">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M2 14L6 9l3 3 5-6" stroke="var(--brand-primary-3)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="13" cy="5" r="2" stroke="var(--brand-primary-3)" stroke-width="1"/></svg>
          <span class="gsc-sum-num">{{ result.markerGeneEvidence }}</span>
          <span class="gsc-sum-label">Marker records</span>
        </div>
      </div>

      <!-- results header -->
      <div class="gsc-res-head">
        <span class="gsc-res-title">Associated samples</span>
        <button
          type="button"
          class="gsc-dl-btn"
          title="Download all results as CSV"
          @click="downloadTableCsv"
        >
          <el-icon><Download /></el-icon>
        </button>
      </div>

      <!-- table -->
      <div class="cte-table-wrap">
        <table class="cte-table">
          <thead><tr>
            <th class="gsc-sort-th" @click="toggleSort('sampleId')">Dataset ID <span class="gsc-sort-arrow">{{ sortArrow('sampleId') }}</span></th>
            <th class="gsc-sort-th" @click="toggleSort('tissue')">Tissue <span class="gsc-sort-arrow">{{ sortArrow('tissue') }}</span></th>
            <th class="gsc-sort-th" @click="toggleSort('sampleName')">Sample name <span class="gsc-sort-arrow">{{ sortArrow('sampleName') }}</span></th>
            <th class="gsc-sort-th" @click="toggleSort('platform')">Platform <span class="gsc-sort-arrow">{{ sortArrow('platform') }}</span></th>
            <th class="gsc-sort-th" @click="toggleSort('sourceId')">Source ID <span class="gsc-sort-arrow">{{ sortArrow('sourceId') }}</span></th>
            <th class="gsc-sort-th" @click="toggleSort('cellCount')">Cells <span class="gsc-sort-arrow">{{ sortArrow('cellCount') }}</span></th>
            <th class="gsc-sort-th" @click="toggleSort('disease')">Disease <span class="gsc-sort-arrow">{{ sortArrow('disease') }}</span></th>
            <th v-if="showMatchedGenes" class="gsc-sort-th" @click="toggleSort('matchedGenes')">Matched genes <span class="gsc-sort-arrow">{{ sortArrow('matchedGenes') }}</span></th>
          </tr></thead>
          <tbody>
            <tr v-for="row in paginatedRows" :key="row.sampleId">
              <td><a @click.stop="viewSample(row.sampleId)" class="gsc-link"><code>{{ row.sampleId }}</code></a></td>
              <td>{{ row.tissue || '—' }}</td>
              <td>{{ row.sampleName || '—' }}</td>
              <td>{{ row.platform || '—' }}</td>
              <td>{{ row.sourceId || '—' }}</td>
              <td>{{ (row.cellCount || 0).toLocaleString() }}</td>
              <td>{{ row.disease || '—' }}</td>
              <td v-if="showMatchedGenes">{{ row.matchedGenes }}</td>
            </tr>
          </tbody>
        </table>
        <div class="cte-pagination">
          <span class="cte-page-info">{{ Math.min((page - 1) * pageSize + 1, totalItems) }}–{{ Math.min(page * pageSize, totalItems) }} of {{ totalItems }} results</span>
          <div class="cte-page-btns">
            <button type="button" class="cte-page-btn" :disabled="page <= 1" @click="page--">Previous</button>
            <button v-for="p in visiblePages" :key="p" type="button" class="cte-page-btn" :class="{ active: p === page }" @click="page = p">{{ p }}</button>
            <button type="button" class="cte-page-btn" :disabled="page >= totalPages" @click="page++">Next</button>
          </div>
        </div>
      </div>
    </div>

    <!-- empty -->
    <div v-else-if="searched" class="gsc-empty">No results found for this query.</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Download } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { fetchSearchTissueOptions, runGeneSearch } from "@/api/analysis";
import { parseFileContent, parseGenes as parseGenesSafe } from "@/utils/geneParser";
import { downloadCsv } from "@/utils/downloadCsv";

const EXAMPLE_GENES = ["MS4A1"];
const MAX_GENES = 200;

const router = useRouter();
const loading = ref(false);
const searched = ref(false);
const geneInput = ref("");
const inputError = ref<string | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
let errorTimer: ReturnType<typeof setTimeout> | null = null;
let validateDebounce: ReturnType<typeof setTimeout> | null = null;

const sortBy = ref("sampleId");
const resultSize = ref(10);
const signalType = ref("gene_expression");
const tissue = ref("");
const tissueOptions = ref<string[]>([]);
const tissueLoading = ref(false);
const tissueLoadError = ref(false);
const tissuePlaceholder = computed(() => {
  if (tissueLoading.value && tissueOptions.value.length === 0) return "Loading tissues...";
  return "All tissues";
});

async function loadTissueOptions(force = false) {
  if (tissueLoading.value || (!force && tissueOptions.value.length > 0)) return;
  tissueLoading.value = true;
  tissueLoadError.value = false;
  try {
    tissueOptions.value = await fetchSearchTissueOptions();
  } catch (error) {
    tissueLoadError.value = true;
    console.error("Failed to load tissue options", error);
  } finally {
    tissueLoading.value = false;
  }
}

function handleTissueVisible(open: boolean) {
  if (open && (tissueLoadError.value || tissueOptions.value.length === 0)) {
    void loadTissueOptions(true);
  }
}

void loadTissueOptions();

const sortColumn = ref<string | null>(null);
const sortDirection = ref<"asc" | "desc">("desc");

function toggleSort(col: string) {
  if (sortColumn.value === col) {
    if (sortDirection.value === "desc") { sortDirection.value = "asc"; }
    else if (sortDirection.value === "asc") { sortColumn.value = null; sortDirection.value = "desc"; }
  } else {
    sortColumn.value = col;
    sortDirection.value = "desc";
  }
  applySort();
}

function sortArrow(col: string): string {
  if (sortColumn.value !== col) return "⇅";
  return sortDirection.value === "desc" ? "▼" : "▲";
}

function applySort() {
  if (!sortColumn.value) {
    rows.value.sort((a, b) => (Number(a._searchOrder) || 0) - (Number(b._searchOrder) || 0));
    return;
  }
  const k = sortColumn.value;
  const dir = sortDirection.value === "desc" ? -1 : 1;
  if (["sampleId", "tissue", "sampleName", "platform", "sourceId", "disease"].includes(k)) {
    rows.value.sort((a, b) => dir * String(a[k] || "").localeCompare(String(b[k] || ""), undefined, { numeric: true, sensitivity: "base" }));
  } else {
    rows.value.sort((a, b) => dir * ((Number(a[k]) || 0) - (Number(b[k]) || 0)));
  }
}

function applySelectedSort() {
  if (!rows.value.length) return;
  sortColumn.value = sortBy.value;
  sortDirection.value = sortBy.value === "sampleId" ? "asc" : "desc";
  page.value = 1;
  applySort();
}

const page = ref(1);
const pageSize = computed(() => resultSize.value);
watch(resultSize, () => { page.value = 1; });

const hasResults = ref(false);
const result = ref({ matchedSamples: 0, markerGeneEvidence: 0 });
const searchedGeneCount = ref(0);
const showMatchedGenes = computed(() => searchedGeneCount.value > 1);

const rows = ref<any[]>([]);
const totalItems = computed(() => rows.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)));
const visiblePages = computed(() => {
  const pages: number[] = [];
  for (let p = Math.max(1, page.value - 2); p <= Math.min(totalPages.value, page.value + 2); p++) pages.push(p);
  return pages;
});
const paginatedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return rows.value.slice(start, start + pageSize.value);
});

function clearInputError() {
  inputError.value = null;
  if (errorTimer) { clearTimeout(errorTimer); errorTimer = null; }
}

function setInputError(msg: string) {
  clearInputError();
  inputError.value = msg;
  errorTimer = setTimeout(() => { inputError.value = null; }, 8000);
}

function validateTextarea(text: string) {
  if (!text || !text.trim()) {
    clearInputError();
    return;
  }
  try {
    const result = parseGenesSafe(text);
    if (result.totalFound > MAX_GENES || result.genes.length > MAX_GENES) {
      setInputError(`Too many genes: ${result.totalFound}. Maximum is ${MAX_GENES}.`);
      return;
    }
    if (result.filteredOut.length > 0) {
      const names = result.filteredOut.slice(0, 3).map(f => f.value).join(", ");
      const more = result.filteredOut.length > 3 ? ` +${result.filteredOut.length - 3} more` : "";
      setInputError(`${result.filteredOut.length} invalid entr${result.filteredOut.length === 1 ? "y" : "ies"} filtered: ${names}${more}`);
      return;
    }
    if (result.warnings.length > 0) {
      setInputError(result.warnings[0] ?? "Input warning.");
      return;
    }
    clearInputError();
  } catch (err: any) {
    setInputError(`Input rejected: ${err.message}`);
  }
}

function onTextareaInput() {
  if (validateDebounce) clearTimeout(validateDebounce);
  validateDebounce = setTimeout(() => validateTextarea(geneInput.value), 400);
}

const parsedGeneResult = computed(() => {
  const text = geneInput.value;
  if (!text || !text.trim()) return null;
  try {
    return parseGenesSafe(text);
  } catch {
    return null;
  }
});
const parsedGenes = computed<string[]>(() => parsedGeneResult.value?.genes ?? []);

watch(() => parsedGenes.value.length, (count) => {
  if (count <= 1 && sortBy.value === "matchedGenes") {
    sortBy.value = "sampleId";
    applySelectedSort();
  }
});

const geneStats = computed(() => {
  const result = parsedGeneResult.value;
  const total = result?.totalFound ?? 0;
  return { total, valid: parsedGenes.value.length, invalid: result?.filteredOut.length ?? 0 };
});
const geneLimitExceeded = computed(() => geneStats.value.valid > MAX_GENES || geneStats.value.total > MAX_GENES);
const displayedInputError = computed(() => geneLimitExceeded.value
  ? `Too many genes: ${geneStats.value.total}. Maximum is ${MAX_GENES}.`
  : inputError.value);

function loadExample() {
  geneInput.value = EXAMPLE_GENES.join("\n");
  clearInputError();
}

function clearInput() {
  geneInput.value = "";
  clearInputError();
}

function uploadFile() {
  fileInputRef.value?.click();
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  input.value = "";
  const reader = new FileReader();
  reader.onerror = () => {
    setInputError("Failed to read file — it may be corrupted or unreadable.");
  };
  reader.onload = () => {
    const text = String(reader.result ?? "");
    if (!text || !text.trim()) {
      setInputError("File is empty or could not be decoded as text.");
      return;
    }
    try {
      const result = parseFileContent(file.name, text);
      if (result.totalFound > MAX_GENES || result.genes.length > MAX_GENES) {
        setInputError(`Too many genes (${result.totalFound}). Maximum is ${MAX_GENES}.`);
        return;
      }
      geneInput.value = result.genes.join("\n");
      clearInputError();
      ElMessage.success(`Loaded ${result.genes.length} gene${result.genes.length !== 1 ? "s" : ""} from ${file.name}`);
    } catch (err: any) {
      setInputError(`File rejected: ${err.message}`);
    }
  };
  reader.readAsText(file);
}

async function doSearch() {
  const genes = parsedGenes.value;
  if (!genes.length) { ElMessage.warning("Please enter at least one valid gene symbol."); return; }
  if (geneLimitExceeded.value) { ElMessage.warning(`Too many genes (${geneStats.value.total}). Please limit to ${MAX_GENES}.`); return; }
  loading.value = true; hasResults.value = false;
  try {
    const data = await runGeneSearch({ genes, sortBy: sortBy.value === "matchedGenes" ? "geneCount" : "sampleName", resultSize: 0, domain: "integration", signalType: signalType.value, tissue: tissue.value || undefined });
    result.value = { matchedSamples: data.summary.matchedSamples, markerGeneEvidence: data.summary.markerGeneEvidence };
    searchedGeneCount.value = genes.length;
    sortColumn.value = null;
    sortDirection.value = "desc";
    rows.value = data.samples.map((s, index) => ({
      _searchOrder: index,
      sampleId: s.sampleId, tissue: s.tissue || '—',
      sampleName: s.sampleName || '—', disease: s.disease || '—',
      platform: s.platform || '—', sourceId: s.sourceId || '—',
      cellCount: s.cellCount ?? 0,
      matchedGenes: s.matchedGenes,
    }));
    page.value = 1;
    applySelectedSort();
    hasResults.value = true; searched.value = true;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || "Search failed.");
  } finally { loading.value = false; }
}

function resetAll() {
  geneInput.value = ""; clearInputError(); sortBy.value = "sampleId"; resultSize.value = 10; signalType.value = "gene_expression"; tissue.value = "";
  sortColumn.value = null; sortDirection.value = "desc";
  hasResults.value = false; searched.value = false; searchedGeneCount.value = 0; page.value = 1; rows.value = [];
}

function viewSample(id: string) {
  router.push({ name: "SampleDetail", params: { id }, query: { domain: "integration", source: "search" } });
}

function downloadTableCsv() {
  if (!rows.value.length) return;
  const headers = ["Dataset ID", "Tissue", "Sample name", "Platform", "Source ID", "Cells", "Disease"];
  if (showMatchedGenes.value) headers.push("Matched genes");
  const data = rows.value.map(r => [
    r.sampleId ?? "", r.tissue ?? "", r.sampleName ?? "",
    r.platform ?? "", r.sourceId ?? "",
    String(r.cellCount ?? 0), r.disease ?? "",
    ...(showMatchedGenes.value ? [String(r.matchedGenes ?? "")] : []),
  ]);
  downloadCsv("oscar_gene_search.csv", headers, data);
}
</script>

<style scoped>
.gsc-root { display: flex; flex-direction: column; gap: 16px; }
.gsc-desc { margin: 0; color: var(--muted); font-size: 14px; font-weight: 750; }

/* input row: left (textarea+filters) + right (buttons side by side) */
.gsc-input-row { display: flex; gap: 16px; align-items: stretch; }
.gsc-input-left { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.gsc-input-right { display: flex; flex-direction: row; gap: 10px; flex-shrink: 0; align-items: stretch; }
.gsc-search-btn { width: 110px; justify-content: center; font-size: 16px; }
.gsc-reset-btn { width: 110px; justify-content: center; font-size: 16px; }

/* filter row */
.gsc-filter-row { display: flex; align-items: flex-end; gap: 16px; flex-wrap: wrap; }
.gsc-filter-row .cte-field { flex: 1 1 140px; }
.gsc-divider { height: 1px; background: var(--border); margin: 0; }
.gsc-empty { padding: 32px; text-align: center; color: var(--muted); font-size: 14px; }

.gsc-summary-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; align-items: start; }
.gsc-summary-card { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 14px 8px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); text-align: center; box-shadow: var(--shadow-card); }
.gsc-sum-num { font-size: 22px; font-weight: 900; color: var(--text); }
.gsc-sum-label { font-size: 10px; font-weight: 700; color: var(--muted); text-transform: uppercase; }

.gsc-results { display: flex; flex-direction: column; gap: 12px; }
.gsc-res-head { display: flex; align-items: center; justify-content: space-between; }
.gsc-res-title { font-weight: 900; font-size: 14px; }
.gsc-dl-btn { appearance: none; display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid var(--border-brand); border-radius: 999px; background: #fffffff2; color: var(--brand-primary-3); box-shadow: inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor: pointer; transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, transform 0.18s ease; }
.gsc-dl-btn:hover { border-color: var(--nav-active-border); background: var(--surface-2); color: var(--text); box-shadow: inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16); transform: translateY(-1px); }
.gsc-dl-btn :deep(.el-icon) { font-size: 15px; }
.gsc-view-tabs { display: flex; gap: 4px; }
.gsc-view-tab { padding: 4px 12px; border: 1px solid var(--border); border-radius: 6px; background: var(--surface); font-size: 12px; font-weight: 700; color: var(--muted); cursor: pointer; }
.gsc-view-tab.active { background: var(--brand-primary-3); color: #fff; border-color: var(--brand-primary-3); }

.gsc-badge { display: inline-block; padding: 1px 8px; border-radius: 4px; font-size: 10px; font-weight: 800; margin-right: 4px; }
.gsc-badge--atac { background: rgba(143,165,156,0.15); color: #5F7D70; }
.gsc-badge--rna { background: rgba(123,167,201,0.15); color: #4A7A9E; }
.gsc-action-btn { border: none; background: transparent; color: var(--brand-primary-3); font-size: 12px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; gap: 3px; }
.gsc-action-btn:hover { text-decoration: underline; }
.gsc-action-arrow { font-size: 10px; }

.cte-field { display: flex; flex-direction: column; gap: 5px; }
.cte-field-label-row { display: inline-flex; align-items: center; gap: 5px; width: fit-content; }
.cte-query-label { display: inline-flex; align-items: center; gap: 8px; width: fit-content; }
.cte-max-badge { display: inline-flex; align-items: center; min-height: 20px; padding: 1px 8px; border: 1px solid rgba(95,125,112,0.24); border-radius: 999px; background: rgba(143,165,156,0.10); color: var(--brand-primary-3); font-size: 10px; font-weight: 900; letter-spacing: 0.02em; }
.cte-field-label { font-size: 14px; font-weight: 900; color: rgba(39,66,58,0.84); }
.gsc-match-help { display: inline-flex; align-items: center; justify-content: center; width: 15px; height: 15px; border-radius: 999px; border: 1px solid var(--border-brand); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; }
.cte-textarea { width: 100%; box-sizing: border-box; min-height: 80px; padding: 12px 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface-2); color: var(--text); font-family: "JetBrains Mono",monospace; font-size: 14px; line-height: 1.6; resize: vertical; }
.cte-textarea:focus { outline: none; border-color: var(--border-brand); box-shadow: 0 0 0 3px rgba(143,165,156,0.14); }
.cte-textarea--error { border-color: #d9857c !important; background: #fefafa !important; box-shadow: 0 0 0 3px rgba(217,133,124,0.08) !important; }
.cte-input-err { font-size: 12px; font-weight: 600; color: #c46a62; margin-top: 4px; line-height: 1.4; }
.cte-hint { margin-top: 5px; font-size: 12px; font-weight: 600; color: var(--muted); }
.cte-btn-row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.spg-upload-wrap { position: relative; display: inline-flex; }
.spg-help-icon { position: absolute; top: -6px; right: -6px; display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; z-index: 1; }
.gsc-stat-row { display: flex; gap: 8px; margin-top: 8px; }
.gsc-stat { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 8px 6px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); text-align: center; position: relative; }
.gsc-stat--muted { background: #f3f4f6; border-color: rgba(160,165,175,0.25); }
.gsc-stat--bad { background: #fdf0f0; border-color: rgba(200,125,125,0.20); }
.gsc-stat--warn { background: #fef8f0; border-color: rgba(200,160,100,0.25); }
.gsc-stat--warn .gsc-stat-num { color: #c8842a; font-size: 18px; }
.gsc-sort-th { cursor: pointer; user-select: none; }
.gsc-sort-th:hover { color: var(--brand-primary-3); }
.gsc-sort-arrow { font-size: 10px; margin-left: 2px; }
.gsc-stat-num { font-size: 16px; font-weight: 900; color: var(--text); }
.gsc-stat-label { font-size: 10px; font-weight: 700; color: var(--muted); }
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
.cte-table tbody tr:last-child td { border-bottom: none; }
.cte-table code { font-size: 13px; color: var(--brand-primary-3); font-weight: 700; }
.gsc-link { color: var(--brand-primary-3); font-weight: 700; cursor: pointer; text-decoration: none; }
.gsc-link:hover { text-decoration: underline; }

.cte-pagination { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-top: 1px solid var(--border); flex-wrap: wrap; gap: 8px; }
.cte-page-info { font-size: 12px; font-weight: 600; color: var(--muted); }
.cte-page-btns { display: flex; gap: 4px; }
.cte-page-btn { min-height: 28px; padding: 2px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--surface); color: var(--text); font-size: 12px; font-weight: 700; cursor: pointer; }
.cte-page-btn.active { background: var(--brand-primary-3); color: #fff; border-color: var(--brand-primary-3); }
.cte-page-btn:disabled { opacity: .4; cursor: not-allowed; }

@media (max-width: 760px) {
  .gsc-root,
  .gsc-input-row,
  .gsc-input-left,
  .gsc-input-right,
  .gsc-results,
  .cte-table-wrap {
    max-width: 100%;
    min-width: 0;
  }

  .gsc-input-row {
    flex-direction: column;
  }

  .gsc-input-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .gsc-search-btn,
  .gsc-reset-btn {
    flex: 1 1 0;
    max-width: 100%;
  }

  .gsc-summary-row { grid-template-columns: repeat(2, 1fr); }
  .gsc-filter-row { flex-direction: column; }
  .gsc-filter-row .cte-field { width: 100%; flex: 1 1 auto; min-width: 0; }

  .cte-textarea,
  .cte-select,
  .primary-btn,
  .soft-btn,
  .gsc-filter-row :deep(.el-select),
  .gsc-filter-row :deep(.el-input),
  .gsc-filter-row :deep(.el-input__wrapper),
  .gsc-filter-row :deep(.el-select__wrapper),
  .gsc-filter-row :deep(.el-textarea) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }
}
@media (max-width: 480px) {
  .gsc-input-right { flex-direction: column; width: 100%; }
  .gsc-search-btn, .gsc-reset-btn { width: 100%; }
  .gsc-summary-row { grid-template-columns: 1fr; }
  .gsc-stat-row { flex-wrap: wrap; }
  .gsc-stat { flex: 1 1 48%; }
  .gsc-filter-row .cte-field { flex: 1 1 100%; }
  .gsc-res-head { flex-wrap: wrap; gap: 8px; }
  .cte-btn-row .soft-btn,
  .spg-upload-wrap {
    width: 100%;
  }
}
</style>

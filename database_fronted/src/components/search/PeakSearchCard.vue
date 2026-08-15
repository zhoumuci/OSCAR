<template>
  <div class="gsc-root">
    <p class="gsc-desc">Find OSCAR samples with regulatory peaks overlapping your genomic regions of interest.</p>

    <div class="cte-card gsc-main-card">
        <div v-if="loading" class="gsc-loading-overlay" role="status" aria-live="polite">
          <div class="gsc-loading-panel">
            <span class="gsc-loading-spinner" aria-hidden="true"></span>
            <span class="gsc-loading-title">Searching regions...</span>
            <span class="gsc-loading-message">This search may take a while. Please wait patiently.</span>
          </div>
        </div>
        <span class="cte-query-label">
          <span class="cte-field-label">Region query</span>
          <span class="cte-max-badge">MAX input: 200 regions</span>
        </span>
        <textarea v-model="regionInput" class="cte-textarea" :class="{ 'cte-textarea--error': displayedInputError }" rows="3" placeholder="chr3:194136145-194138732" :disabled="loading" @input="onTextareaInput"></textarea>
        <div class="gsc-hint-row">
          <span class="cte-hint">Enter one or more genomic regions in chr:start-end or BED-like format, one per line.</span>
          <span v-if="regionInput.trim()" class="gsc-parse-stats">{{ regionStats.valid }}/{{ regionStats.input }} valid</span>
        </div>
        <div v-if="displayedInputError" class="input-feedback-card" role="alert">
          <span class="input-feedback-card__icon" aria-hidden="true">!</span>
          <span class="input-feedback-card__body">
            <strong>Check your region input</strong>
            <span>{{ displayedInputError }}</span>
          </span>
          <button v-if="!regionLimitExceeded" type="button" class="input-feedback-card__close" aria-label="Dismiss input message" @click="clearInputError">&times;</button>
        </div>
        <div class="cte-btn-row">
          <span class="spg-upload-wrap">
            <button type="button" class="soft-btn" :disabled="loading" @click="uploadBed">
              <span>📎</span> Upload BED
            </button>
            <el-tooltip placement="top" effect="light" :show-after="200">
              <template #content>
                <div><div>Accepted files: .bed, .txt, .csv, and .tsv.</div><div>Each row must contain chr:start-end or at least the first three BED columns: chromosome, start, and end.</div></div>
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
        <input ref="fileInputRef" type="file" accept=".bed,.txt,.csv,.tsv" style="display:none" @change="onBedSelected" />
        <div v-if="regionInput.trim()" class="gsc-stat-row">
          <div class="gsc-stat gsc-stat--muted"><span class="gsc-stat-num">{{ regionStats.input }}</span><span class="gsc-stat-label">Input regions</span></div>
          <div class="gsc-stat"><span class="gsc-stat-num">{{ regionStats.valid }}</span><span class="gsc-stat-label">Valid regions</span></div>
          <div class="gsc-stat" :class="{ 'gsc-stat--bad': regionStats.invalid > 0 }"><span class="gsc-stat-num">{{ regionStats.invalid }}</span><span class="gsc-stat-label">Invalid lines</span></div>
        </div>
        <div class="gsc-filter-row">
          <label class="cte-field">
            <span class="cte-field-label-row">
              <span class="cte-field-label">Match mode</span>
              <el-tooltip placement="top" effect="light" :show-after="200">
                <template #content>
                  <div><div><strong>Any input region:</strong> returns a sample when one or more submitted regions overlap a marker peak in that sample.</div><div><strong>All input regions:</strong> returns a sample only when every submitted region has at least one marker-peak overlap in that same sample.</div></div>
                </template>
                <span class="gsc-match-help">?</span>
              </el-tooltip>
            </span>
            <el-select v-model="matchMode" class="cte-select" popper-class="oscar-select-popper" :disabled="loading">
              <el-option label="Any input region" value="any" /><el-option label="All input regions" value="all" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Per page</span>
            <el-select v-model="resultSize" class="cte-select" popper-class="oscar-select-popper" :disabled="loading">
              <el-option label="10" :value="10" /><el-option label="20" :value="20" /><el-option label="50" :value="50" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Domain</span>
            <el-select v-model="domain" class="cte-select" popper-class="oscar-select-popper" :disabled="loading">
              <el-option label="Integration" value="integration" />
            </el-select>
          </label>
          <label class="cte-field"><span class="cte-field-label">Dataset ID</span>
            <el-input
              v-model="datasetId"
              class="cte-input"
              :disabled="loading"
              placeholder="H_000001"
              clearable
            />
          </label>
        </div>
        <div class="gsc-btn-row">
          <button type="button" class="primary-btn" :disabled="loading || regionStats.valid === 0 || regionLimitExceeded || !datasetId.trim()" @click="doSearch">
            <svg width="16" height="16" viewBox="0 0 20 20" fill="none"><circle cx="9" cy="9" r="6" stroke="currentColor" stroke-width="1.5"/><line x1="14" y1="14" x2="18" y2="18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg> Search
          </button>
          <button type="button" class="soft-btn" :disabled="loading" @click="resetAll">Reset</button>
        </div>
        <div class="gsc-how-brief">
          <div class="gsc-how-title">How it works</div>
          <div class="gsc-how-list">
            <div class="gsc-how-item"><span class="gsc-how-dot">1</span> Enter genomic regions in chr:start-end or BED format.</div>
            <div class="gsc-how-item"><span class="gsc-how-dot">2</span> Choose match mode: Any (union) or All (intersection).</div>
            <div class="gsc-how-item"><span class="gsc-how-dot">3</span> Click Search to find samples with overlapping OSCAR peaks.</div>
            <div class="gsc-how-item"><span class="gsc-how-dot">💡</span> Multi-region search supports up to 200 regions interactively.</div>
          </div>
        </div>
      </div>

    <div class="gsc-divider"></div>

    <div v-if="hasResults" class="gsc-results">
      <div class="gsc-summary-row">
        <div class="gsc-summary-card"><span class="gsc-sum-num">{{ results.matchedSamples }}</span><span class="gsc-sum-label">Matched samples</span></div>
        <div class="gsc-summary-card"><span class="gsc-sum-num">{{ results.matchedInputRegions }}/{{ results.inputRegions || '—' }}</span><span class="gsc-sum-label">Matched input regions</span></div>
        <div class="gsc-summary-card"><span class="gsc-sum-num">{{ results.overlappingPeaks.toLocaleString() }}</span><span class="gsc-sum-label">Overlapping peaks</span></div>
        <div class="gsc-summary-card"><span class="gsc-sum-num">{{ results.linkedGenes }}</span><span class="gsc-sum-label">Linked marker genes</span></div>
      </div>

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

      <div class="cte-table-wrap">
        <table class="cte-table"><thead><tr>
          <th class="gsc-sort-th" @click="toggleSort('sampleId')">Dataset ID <span class="gsc-sort-arrow">{{ sortArrow('sampleId') }}</span></th>
          <th class="gsc-sort-th" @click="toggleSort('tissue')">Tissue <span class="gsc-sort-arrow">{{ sortArrow('tissue') }}</span></th>
          <th class="gsc-sort-th" @click="toggleSort('sampleName')">Sample name <span class="gsc-sort-arrow">{{ sortArrow('sampleName') }}</span></th>
          <th class="gsc-sort-th" @click="toggleSort('matchedRegions')">Matched regions <span class="gsc-sort-arrow">{{ sortArrow('matchedRegions') }}</span></th>
          <th class="gsc-sort-th" @click="toggleSort('overlappingPeaks')">Overlapping peaks <span class="gsc-sort-arrow">{{ sortArrow('overlappingPeaks') }}</span></th>
          <th class="gsc-sort-th" @click="toggleSort('linkedGenes')">Linked marker genes <span class="gsc-sort-arrow">{{ sortArrow('linkedGenes') }}</span></th>
          <th>Data Type</th>
        </tr></thead><tbody>
          <tr v-if="!rows.length"><td colspan="7" class="gsc-empty">No samples found.</td></tr>
          <tr v-for="r in paginatedRows" :key="r.sampleId">
            <td><a @click.stop="router.push({name:'SampleDetail',params:{id:r.sampleId},query:{domain:'integration',source:'search'}})" class="gsc-link"><code>{{ r.sampleId }}</code></a></td>
            <td>{{ r.tissue || '—' }}</td><td>{{ r.sampleName || '—' }}</td>
            <td>{{ r.matchedRegions }}/{{ results.inputRegions }}</td><td>{{ (r.overlappingPeaks || 0).toLocaleString() }}</td><td>{{ r.linkedGenes || 0 }}</td>
            <td>
              <span v-if="r.hasAtac" class="gsc-badge gsc-badge--atac">ATAC</span>
              <span v-if="r.hasP2g" class="gsc-badge gsc-badge--p2g">P2G</span>
            </td>
          </tr>
        </tbody></table>
        <div class="cte-pagination">
          <span class="cte-page-info">{{ (page - 1) * pageSize + 1 }}–{{ Math.min(page * pageSize, totalItems) }} of {{ totalItems }} results</span>
          <div class="cte-page-btns">
            <button type="button" class="cte-page-btn" :disabled="page <= 1" @click="page--">‹</button>
            <button v-for="p in visiblePages" :key="p" type="button" class="cte-page-btn" :class="{ active: p === page }" @click="page = p">{{ p }}</button>
            <button type="button" class="cte-page-btn" :disabled="page >= totalPages" @click="page++">›</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Download } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { runPeakSearch } from "@/api/analysis";
import { downloadCsv } from "@/utils/downloadCsv";

const MAX_REGIONS = 200;
const EXAMPLE_REGIONS = [
  "chr3:194136145-194138732",
];

const router = useRouter();
const loading = ref(false); const hasResults = ref(false);
const regionInput = ref(""); const inputError = ref<string | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
let errorTimer: ReturnType<typeof setTimeout> | null = null;
let validateDebounce: ReturnType<typeof setTimeout> | null = null;

const domain = ref("integration");
const DEFAULT_DATASET_ID = "H_000001";
const datasetId = ref(DEFAULT_DATASET_ID);
const matchMode = ref("any"); const resultSize = ref(10);
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
    rows.value.sort((a, b) => (b.overlappingPeaks || 0) - (a.overlappingPeaks || 0));
    return;
  }
  const k = sortColumn.value;
  const dir = sortDirection.value === "desc" ? -1 : 1;
  if (["sampleId", "tissue", "sampleName"].includes(k)) {
    rows.value.sort((a, b) => dir * String(a[k] || "").localeCompare(String(b[k] || "")));
  } else {
    rows.value.sort((a, b) => dir * ((Number(b[k]) || 0) - (Number(a[k]) || 0)));
  }
}

const page = ref(1); const pageSize = computed(() => resultSize.value);
watch(resultSize, () => { page.value = 1; });
const rows = ref<any[]>([]);
const results = ref({ inputRegions: 0, matchedSamples: 0, matchedInputRegions: 0, overlappingPeaks: 0, linkedGenes: 0 });

function clearInputError() {
  inputError.value = null;
  if (errorTimer) { clearTimeout(errorTimer); errorTimer = null; }
}

function setInputError(msg: string) {
  clearInputError();
  inputError.value = msg;
  errorTimer = setTimeout(() => { inputError.value = null; }, 8000);
}

function validateRegions(text: string) {
  if (!text || !text.trim()) {
    clearInputError();
    return;
  }
  const lines = text.split(/[\r\n]+/).map(l => l.trim()).filter(l => l);
  if (lines.length > MAX_REGIONS) {
    setInputError(`Too many regions: ${lines.length}. Maximum is ${MAX_REGIONS}.`);
    return;
  }
  const parsed = parseRegions(text);
  const invalid = parsed.filter(r => !r.valid);
  if (invalid.length > 0) {
    const samples = invalid.slice(0, 3).map(r => r.raw.length > 40 ? r.raw.slice(0, 40) + "…" : r.raw).join("; ");
    const more = invalid.length > 3 ? ` +${invalid.length - 3} more` : "";
    setInputError(`${invalid.length} invalid region${invalid.length === 1 ? "" : "s"}: ${samples}${more}`);
    return;
  }
  clearInputError();
}

function onTextareaInput() {
  if (validateDebounce) clearTimeout(validateDebounce);
  validateDebounce = setTimeout(() => validateRegions(regionInput.value), 400);
}

function loadExample() {
  regionInput.value = EXAMPLE_REGIONS.join("\n");
  clearInputError();
}

function clearInput() {
  regionInput.value = "";
  clearInputError();
}

function uploadBed() {
  fileInputRef.value?.click();
}

function onBedSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  input.value = "";
  if (!/\.(?:bed|txt|csv|tsv)$/i.test(file.name)) {
    setInputError("Unsupported file type. Please choose a .bed, .txt, .csv, or .tsv file.");
    return;
  }
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
    // Extract chr:start-end or BED-like lines
    const lines = text.split(/[\r\n]+/).map(l => l.trim()).filter(l =>
      l
      && !l.startsWith("#")
      && !l.startsWith("track")
      && !l.startsWith("browser")
      && !/^(?:chrom|chromosome)[,\s\t]+start[,\s\t]+end(?:[,\s\t]|$)/i.test(l)
    );
    if (lines.length > MAX_REGIONS) {
      setInputError(`Too many regions: ${lines.length}. Maximum is ${MAX_REGIONS}.`);
      return;
    }
    const regions = lines.map(l => {
      const m = l.match(/^(chr[\w]+):(\d+)-(\d+)$/i);
      if (m) return `${m[1]}:${m[2]}-${m[3]}`;
      const parts = l.split(/[,\s\t]+/);
      const chrom = parts[0] ?? "";
      const start = parts[1] ?? "";
      const end = parts[2] ?? "";
      if (parts.length >= 3 && /^chr/i.test(chrom) && /^\d+$/.test(start) && /^\d+$/.test(end)) {
        return `${chrom}:${start}-${end}`;
      }
      return null;
    }).filter((r): r is string => r !== null);
    const invalidCount = lines.length - regions.length;
    if (invalidCount > 0) {
      setInputError(`${invalidCount} invalid region row${invalidCount === 1 ? "" : "s"} found. Fix or remove invalid rows before uploading.`);
      return;
    }
    if (!regions.length) {
      setInputError("No valid genomic regions found in file.");
      return;
    }
    if (regions.length > MAX_REGIONS) {
      setInputError(`Too many regions: ${regions.length}. Maximum is ${MAX_REGIONS}.`);
      return;
    }
    regionInput.value = regions.join("\n");
    clearInputError();
    ElMessage.success(`Loaded ${regions.length} region${regions.length !== 1 ? "s" : ""} from ${file.name}`);
  };
  reader.readAsText(file);
}

type ParsedRegion = { raw: string; chrom: string; start: number; end: number; valid: boolean; reason?: string };

function parseRegions(input: string): ParsedRegion[] {
  return input.split(/[\r\n]+/).map(l => l.trim()).filter(l => l).map(raw => {
    const m = raw.match(/^(chr[\w]+):(\d+)-(\d+)$/i);
    if (m?.[1] && m[2] && m[3]) {
      const s = +m[2], e = +m[3];
      const start = Math.min(s, e), end = Math.max(s, e);
      if (start < end) return { raw, chrom: m[1], start, end, valid: true };
      return { raw, chrom: "", start: 0, end: 0, valid: false, reason: "start === end" };
    }
    const parts = raw.split(/[,\s\t]+/);
    const [chrom, startText, endText] = parts;
    if (chrom && startText && endText) {
      const s = +startText, e = +endText;
      const start = Math.min(s, e), end = Math.max(s, e);
      if (!isNaN(s) && !isNaN(e) && start < end) return { raw, chrom, start, end, valid: true };
    }
    return { raw, chrom: "", start: 0, end: 0, valid: false, reason: "invalid format" };
  });
}

const parsedRegions = computed(() => parseRegions(regionInput.value));
const regionStats = computed(() => {
  const all = parsedRegions.value;
  if (!regionInput.value.trim()) return { input: 0, valid: 0, invalid: 0 };
  return { input: all.length, valid: all.filter(r => r.valid).length, invalid: all.filter(r => !r.valid).length };
});
const regionLimitExceeded = computed(() => regionStats.value.input > MAX_REGIONS || regionStats.value.valid > MAX_REGIONS);
const displayedInputError = computed(() => regionLimitExceeded.value
  ? `Too many regions: ${regionStats.value.input}. Maximum is ${MAX_REGIONS}.`
  : inputError.value);

const totalItems = computed(() => rows.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)));
const visiblePages = computed(() => {
  const p: number[] = []; for (let i = Math.max(1, page.value - 2); i <= Math.min(totalPages.value, page.value + 2); i++) p.push(i); return p;
});
const paginatedRows = computed(() => { const s = (page.value - 1) * pageSize.value; return rows.value.slice(s, s + pageSize.value); });

async function doSearch() {
  const valid = parsedRegions.value.filter(r => r.valid);
  if (!regionInput.value.trim()) { ElMessage.warning("Please enter at least one genomic region."); return; }
  if (!valid.length) { ElMessage.warning("No valid genomic region was found."); return; }
  if (valid.length > MAX_REGIONS) { ElMessage.warning(`Please provide no more than ${MAX_REGIONS} regions. For larger sets, use the analysis tools.`); return; }
  const normalizedDatasetId = datasetId.value.trim();
  if (!normalizedDatasetId) { ElMessage.warning("Please enter a dataset ID."); return; }
  loading.value = true; hasResults.value = false;
  try {
    const data = await runPeakSearch({
      regions: valid.map(r => ({ chrom: r.chrom, start: r.start, end: r.end })),
      matchMode: matchMode.value as "any" | "all",
      domain: domain.value as "integration" | "atac",
      datasetId: normalizedDatasetId,
    });
    rows.value = data.samples.map(s => ({
      sampleId: s.sampleId,
      tissue: s.tissue || '—',
      cellContext: s.cellContext || '—',
      sampleName: s.sampleName || '—',
      matchedRegions: s.matchedRegions,
      overlappingPeaks: s.overlappingPeaks,
      linkedGenes: s.linkedGenes,
      hasAtac: s.hasAtac,
      hasP2g: s.hasP2g,
    }));
    results.value = data.summary;
    applySort();
    page.value = 1; hasResults.value = true;
  } catch (e: any) { ElMessage.error("Search failed."); }
  finally { loading.value = false; }
}

function resetAll() { regionInput.value = ""; clearInputError(); matchMode.value = "any"; resultSize.value = 10; domain.value = "integration"; datasetId.value = DEFAULT_DATASET_ID; hasResults.value = false; rows.value = []; }

function downloadTableCsv() {
  if (!rows.value.length) return;
  const headers = ["Dataset ID", "Tissue", "Sample name", "Matched regions", "Overlapping peaks", "Linked marker genes", "Data Type"];
  const data = rows.value.map(r => [
    r.sampleId ?? "", r.tissue ?? "", r.sampleName ?? "",
    String(r.matchedRegions ?? ""), String(r.overlappingPeaks ?? 0),
    String(r.linkedGenes ?? 0),
    [r.hasAtac ? "ATAC" : "", r.hasP2g ? "P2G" : ""].filter(Boolean).join("/") || "—",
  ]);
  downloadCsv("oscar_peak_search.csv", headers, data);
}
</script>

<style scoped>
.gsc-root { display: flex; flex-direction: column; gap: 14px; }
.gsc-desc { margin: 0; color: var(--muted); font-size: 14px; font-weight: 750; }
.gsc-main-card { position: relative; display: flex; flex-direction: column; gap: 12px; min-width: 0; overflow: hidden; }
.gsc-loading-overlay { position: absolute; inset: 0; z-index: 20; display: flex; align-items: center; justify-content: center; padding: 24px; border-radius: inherit; background: rgba(247, 250, 249, 0.88); backdrop-filter: blur(2px); }
.gsc-loading-panel { display: flex; min-width: min(320px, 100%); max-width: 460px; flex-direction: column; align-items: center; gap: 10px; padding: 24px 28px; border: 1px solid var(--border-brand); border-radius: 14px; background: rgba(255, 255, 255, 0.96); box-shadow: 0 18px 44px rgba(39, 66, 58, 0.16); text-align: center; }
.gsc-loading-spinner { width: 34px; height: 34px; box-sizing: border-box; border: 3px solid rgba(95, 125, 112, 0.20); border-top-color: var(--brand-primary-3); border-radius: 50%; animation: gsc-region-spin 0.8s linear infinite; }
.gsc-loading-title { color: var(--text); font-size: 15px; font-weight: 900; }
.gsc-loading-message { color: var(--muted); font-size: 12px; font-weight: 650; line-height: 1.55; }
@keyframes gsc-region-spin { to { transform: rotate(360deg); } }
.gsc-btn-row { display: flex; gap: 8px; flex-wrap: wrap; }
.gsc-filter-row { display: flex; align-items: flex-end; gap: 16px; }
.gsc-filter-row .cte-field { flex: 1 1 140px; }
.gsc-hint-row { display: flex; align-items: center; justify-content: space-between; }
.gsc-divider { height: 1px; background: var(--border); }
.gsc-how-brief { margin-top: auto; padding-top: 10px; border-top: 1px solid var(--border); }
.gsc-how-title { font-size: 13px; font-weight: 900; color: var(--text); margin-bottom: 6px; }
.gsc-how-list { display: flex; flex-direction: column; gap: 6px; }
.gsc-how-item { font-size: 11px; font-weight: 600; color: var(--muted); line-height: 1.5; display: flex; gap: 6px; align-items: baseline; }
.gsc-how-dot { flex-shrink: 0; width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: rgba(143,165,156,0.12); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; }
.gsc-empty { padding: 32px; text-align: center; color: var(--muted); font-size: 14px; }
.gsc-results { display: flex; flex-direction: column; gap: 12px; }
.gsc-summary-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.gsc-summary-card { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 14px 18px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-card); }
.gsc-sum-num { font-size: 22px; font-weight: 900; color: var(--text); }
.gsc-sum-label { font-size: 10px; font-weight: 700; color: var(--muted); text-transform: uppercase; }
.gsc-res-head { display: flex; align-items: center; justify-content: space-between; }
.gsc-res-title { font-weight: 900; font-size: 14px; }
.gsc-dl-btn { appearance: none; display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; padding: 0; border: 1px solid var(--border-brand); border-radius: 999px; background: #fffffff2; color: var(--brand-primary-3); box-shadow: inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor: pointer; transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, transform 0.18s ease; }
.gsc-dl-btn:hover { border-color: var(--nav-active-border); background: var(--surface-2); color: var(--text); box-shadow: inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16); transform: translateY(-1px); }
.gsc-dl-btn :deep(.el-icon) { font-size: 15px; }
.gsc-stat-row { display: flex; gap: 8px; margin-top: 8px; }
.gsc-stat { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 8px 6px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); text-align: center; }
.gsc-stat--muted { background: #f3f4f6; border-color: rgba(160,165,175,0.25); }
.gsc-stat--bad { background: #fdf0f0; border-color: rgba(200,125,125,0.20); }
.gsc-stat-num { font-size: 16px; font-weight: 900; color: var(--text); }
.gsc-stat-label { font-size: 10px; font-weight: 700; color: var(--muted); }
.gsc-badge { display: inline-block; padding: 1px 8px; border-radius: 4px; font-size: 10px; font-weight: 800; margin-right: 4px; }
.gsc-badge--atac { background: rgba(143,165,156,0.15); color: #5F7D70; }
.gsc-badge--p2g { background: rgba(123,167,201,0.15); color: #4A7A9E; }
.cte-field { display: flex; flex-direction: column; gap: 5px; }
.cte-field-label-row { display: inline-flex; align-items: center; gap: 5px; width: fit-content; }
.cte-query-label { display: inline-flex; align-items: center; gap: 8px; width: fit-content; }
.cte-max-badge { display: inline-flex; align-items: center; min-height: 20px; padding: 1px 8px; border: 1px solid rgba(95,125,112,0.24); border-radius: 999px; background: rgba(143,165,156,0.10); color: var(--brand-primary-3); font-size: 10px; font-weight: 900; letter-spacing: 0.02em; }
.cte-field-label { font-size: 14px; font-weight: 900; color: rgba(39,66,58,0.84); }
.gsc-match-help { display: inline-flex; align-items: center; justify-content: center; width: 15px; height: 15px; border-radius: 999px; border: 1px solid var(--border-brand); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; }
.cte-hint { font-size: 12px; font-weight: 600; color: var(--muted); }
.cte-textarea { width: 100%; box-sizing: border-box; min-height: 56px; padding: 12px 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface-2); color: var(--text); font-family: "JetBrains Mono",monospace; font-size: 14px; line-height: 1.5; resize: vertical; }
.cte-textarea:focus { outline: none; border-color: var(--border-brand); box-shadow: 0 0 0 3px rgba(143,165,156,0.14); }
.cte-textarea--error { border-color: #d9857c !important; background: #fefafa !important; box-shadow: 0 0 0 3px rgba(217,133,124,0.08) !important; }
.cte-input-err { font-size: 12px; font-weight: 600; color: #c46a62; margin-top: 4px; line-height: 1.4; }
.cte-btn-row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.spg-upload-wrap { position: relative; display: inline-flex; }
.spg-help-icon { position: absolute; top: -6px; right: -6px; display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; z-index: 1; }
.gsc-sort-th { cursor: pointer; user-select: none; }
.gsc-sort-th:hover { color: var(--brand-primary-3); }
.gsc-sort-arrow { font-size: 10px; margin-left: 2px; }
.gsc-parse-stats { font-size: 11px; font-weight: 700; color: var(--brand-primary-3); }
.cte-select { width: 100%; }
.cte-input { width: 100%; }
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
.gsc-link { color: var(--brand-primary-3); font-weight: 700; cursor: pointer; text-decoration: none; }
.gsc-link:hover { text-decoration: underline; }
.cte-pagination { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-top: 1px solid var(--border); }
.cte-page-info { font-size: 12px; font-weight: 600; color: var(--muted); }
.cte-page-btns { display: flex; gap: 4px; }
.cte-page-btn { min-height: 28px; padding: 2px 10px; border: 1px solid var(--border); border-radius: 6px; background: var(--surface); color: var(--text); font-size: 12px; font-weight: 700; cursor: pointer; }
.cte-page-btn.active { background: var(--brand-primary-3); color: #fff; border-color: var(--brand-primary-3); }
.cte-page-btn:disabled { opacity: .4; cursor: not-allowed; }
.cte-card { box-sizing: border-box; background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-card); }
@media (max-width: 760px) {
  .gsc-root,
  .gsc-main-card,
  .gsc-results,
  .cte-table-wrap {
    max-width: 100%;
    min-width: 0;
  }

  .gsc-filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .gsc-filter-row .cte-field {
    width: 100%;
    flex: 1 1 auto;
    min-width: 0;
  }

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

  .gsc-hint-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .gsc-summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .gsc-res-head { flex-wrap: wrap; gap: 8px; }
  .gsc-summary-row { grid-template-columns: 1fr; }
  .cte-btn-row .soft-btn,
  .spg-upload-wrap {
    width: 100%;
  }
}
</style>

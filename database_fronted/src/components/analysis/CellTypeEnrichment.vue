<template>
  <div class="cte-root">
    <!-- ─── Description ──────────────────────────────────────────── -->
    <p class="cte-desc">
      Input a gene set to identify enriched cell types based on OSCAR marker genes.
    </p>

    <div class="cte-workbench">
      <div class="cte-card cte-builder-card">
        <section class="cte-builder-section">
          <div class="cte-card-title">Gene set input <span class="cte-max-badge">MAX input: 200 genes</span></div>

      <div
        v-if="displayedInputError"
        class="input-feedback-card"
        role="alert"
      >
        <span class="input-feedback-card__icon" aria-hidden="true">!</span>
        <span class="input-feedback-card__body">
          <strong>Check your gene set</strong>
          <span>{{ displayedInputError }}</span>
        </span>
        <button v-if="!geneLimitExceeded" type="button" class="input-feedback-card__close" aria-label="Dismiss input message" @click="clearInputError">&times;</button>
      </div>

      <textarea
        v-model="geneSetText"
        class="cte-textarea"
        :class="{ 'cte-textarea--error': displayedInputError }"
        rows="5"
        placeholder="CD3D
CD3E
TRAC
IL7R
CCR7"
        :disabled="loading"
        @input="onTextareaInput"
      ></textarea>

      <p class="cte-hint">
        Paste human gene symbols, one per line or separated by comma / space.
      </p>

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

      <div class="cte-gene-stats">
        <div class="stat-item stat-item--muted">
          <span class="stat-label">Input genes</span>
          <span class="stat-value">{{ inputGeneCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">Matched genes</span>
          <span class="stat-value">{{ matchedGeneCount }}</span>
        </div>
        <div class="stat-item stat-item--bad">
          <span class="stat-label">Unmatched genes</span>
          <span class="stat-value stat-value--muted">{{ unmatchedGeneCount }}</span>
        </div>
      </div>
        </section>

        <section class="cte-builder-section cte-settings-card">
        <div class="cte-card-title">Analysis settings</div>

        <div class="cte-settings-body">
          <!-- Main fields (left sub-column) -->
          <div class="cte-settings-main">
            <div class="cte-fields">
              <!-- 1. Tissue -->
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

              <!-- 2. Dataset -->
              <label class="cte-field">
                <span class="cte-field-label">Dataset <em>required</em></span>
                <el-select
                  v-model="referenceDatasetId"
                  class="cte-select"
                  popper-class="oscar-select-popper"
                  size="small"
                  filterable
                  placeholder="Select one dataset"
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

              <!-- 3. Result level -->
              <div class="cte-field">
                <span class="cte-field-label cte-field-label--help">
                  Result level
                  <el-tooltip
                    placement="top"
                    effect="light"
                    :show-after="200"
                  >
                    <template #content>
                      <div class="cte-result-level-help">
                        <div v-for="line in resultLevelHelpLines" :key="line.label">
                          <strong>{{ line.label }}:</strong> {{ line.text }}
                        </div>
                      </div>
                    </template>
                    <span
                      class="cte-inline-help-icon"
                      role="button"
                      tabindex="0"
                      aria-label="Result level help"
                    >?</span>
                  </el-tooltip>
                </span>
                <el-select
                  v-model="resultLevel"
                  aria-label="Result level"
                  class="cte-select"
                  popper-class="oscar-select-popper"
                  size="small"
                  :disabled="loading"
                >
                  <el-option label="Cell type (recommended)" value="cell_type" />
                  <el-option label="Cluster" value="cluster" />
                </el-select>
              </div>

            </div>
          </div>

          <!-- Advanced (right sub-column) -->
          <div class="cte-settings-advanced-col">
            <button
              type="button"
              class="cte-advanced-toggle"
              :class="{ open: advancedOpen }"
              @click="advancedOpen = !advancedOpen"
            >
              <span class="cte-toggle-chev">▸</span>
              Advanced settings
            </button>

            <div v-show="advancedOpen" class="cte-advanced">
              <label class="cte-field">
                <span class="cte-field-label">Marker reference</span>
                <el-select
                  v-model="markerReference"
                  class="cte-select"
                  popper-class="oscar-select-popper"
                  size="small"
                  :disabled="loading"
                >
                  <el-option label="Integration markers" value="integration_expression" />
                </el-select>
              </label>

              <label class="cte-field">
                <span class="cte-field-label">Minimum overlap</span>
                <el-input-number
                  v-model="minOverlap"
                  class="cte-number"
                  size="small"
                  :min="1"
                  :max="100"
                  :disabled="loading"
                />
              </label>

              <label class="cte-field">
                <span class="cte-field-label">FDR method</span>
                <el-select
                  v-model="fdrMethod"
                  class="cte-select"
                  popper-class="oscar-select-popper"
                  size="small"
                  :disabled="loading"
                >
                  <el-option label="BH (Benjamini-Hochberg)" value="BH" />
                </el-select>
              </label>
            </div>
          </div>
        </div>

        <div class="cte-card-actions">
          <button
            type="button"
            class="primary-btn"
            :disabled="loading || parsedGenes.length === 0 || geneLimitExceeded || !tissue || !referenceDatasetId"
            @click="runAnalysis"
          >
            <span v-if="loading" class="btn-spinner"></span>
            {{ loading ? "Running…" : "Run enrichment" }}
          </button>
          <button type="button" class="soft-btn" :disabled="loading" @click="resetAll">
            Reset
          </button>
        </div>

        <div v-if="loading" class="cte-progress-card" role="status" aria-live="polite">
          <div class="cte-progress-head">
            <div>
              <div class="cte-progress-stage">{{ progressStageLabel }}</div>
              <div class="cte-progress-message">{{ progressMessage }}</div>
            </div>
            <span class="cte-progress-status">Running</span>
          </div>
          <div class="cte-progress-track" :class="{ indeterminate: progressStage === 'QUERYING_ENRICHMENT' }">
            <div class="cte-progress-fill" :style="{ width: `${progressWidth}%` }"></div>
          </div>
          <div class="cte-progress-steps">
            <span :class="{ done: progressStage !== 'VALIDATING', active: progressStage === 'VALIDATING' }">Validate input</span>
            <span :class="{ done: progressStage === 'BUILDING_RESULTS' || progressStage === 'COMPLETED', active: progressStage === 'QUERYING_ENRICHMENT' }">Enrichment + BH</span>
            <span :class="{ done: progressStage === 'COMPLETED', active: progressStage === 'BUILDING_RESULTS' }">Build results</span>
          </div>
        </div>
        </section>
      </div>

      <div class="cte-side-column">
        <div class="cte-card cte-image-card">
          <img :src="baseUrl + 'images/cell_enrichment.jpg'" alt="Cell type enrichment" class="cte-slot-img" />
        </div>

        <div class="cte-card cte-how-card">
          <div class="cte-card-title">How it works</div>

          <div class="how-steps">
          <div class="how-step">
            <span class="how-num">1</span>
            <div class="how-step-body">
              <strong>Input gene set</strong>
              <span>Provide a list of human gene symbols.</span>
            </div>
          </div>
          <div class="how-step">
            <span class="how-num">2</span>
            <div class="how-step-body">
              <strong>Match OSCAR marker genes</strong>
              <span>Compare the input genes with marker genes from the selected dataset.</span>
            </div>
          </div>
          <div class="how-step">
            <span class="how-num">3</span>
            <div class="how-step-body">
              <strong>Hypergeometric enrichment test</strong>
              <span>Evaluate whether the observed overlap is significantly higher than expected by chance.</span>
            </div>
          </div>
          <div class="how-step">
            <span class="how-num">4</span>
            <div class="how-step-body">
              <strong>Rank enriched groups</strong>
              <span>Rank cell types or individual clusters from the selected dataset after multiple-testing correction.</span>
            </div>
          </div>
        </div>

        <p class="how-note">
          This module tests whether the input gene set is significantly enriched in
          human marker genes from one selected OSCAR dataset. Cell type mode combines
          clusters with the same standardised cell-type name; Cluster mode tests each
          dataset cluster separately. Significance is evaluated using a hypergeometric
          test followed by multiple-testing correction.
        </p>
        </div>
      </div>
    </div>

    <!-- ─── Row 3: Results (full width) ──────────────────────────── -->
    <div v-if="hasRun" class="cte-results">
      <!-- Summary cards -->
      <div class="cte-summary-row">
        <div class="cte-summary-card">
          <span class="sum-num">{{ enrichmentResults.inputGenes?.length ?? 0 }}</span>
          <span class="sum-label">Input genes</span>
        </div>
        <div class="cte-summary-card">
          <span class="sum-num">{{ enrichmentResults.matchedGenes?.length ?? 0 }}</span>
          <span class="sum-label">Matched genes</span>
        </div>
        <div class="cte-summary-card">
          <span class="sum-num">{{ enrichmentResults.significantResults ?? 0 }}</span>
          <span class="sum-label">Significant results</span>
        </div>
        <div class="cte-summary-card">
          <span class="sum-num sum-num--sm">{{ topEnrichedLabel }}</span>
          <span class="sum-label">{{ topEnrichedSummaryLabel }}</span>
        </div>
      </div>

      <!-- Overview chart placeholder -->
      <div v-if="false" class="cte-chart-placeholder">
        <div class="cte-chart-title">Top enriched cell types</div>
        <div class="cte-chart-area">
          <span class="cte-chart-hint">Horizontal bar chart: −log₁₀(FDR) vs Cell type</span>
        </div>
      </div>

      <!-- Result tabs -->
      <div class="cte-tabs">
        <button
          v-for="tab in resultTabs"
          :key="tab.key"
          type="button"
          class="cte-tab"
          :class="{ active: resultTab === tab.key }"
          @click="resultTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Tab content -->
      <div class="cte-tab-content">
        <template v-if="resultTab === 'overview'">
          <div class="cte-overview-grid">
            <div class="cte-overview-panel">
              <div class="cte-chart-title">{{ overviewTitle }}</div>
              <div v-if="topOverviewRows.length === 0" class="cte-empty-state">No enrichment results found.</div>
              <div v-else class="cte-bar-list">
                <button
                  v-for="row in topOverviewRows"
                  :key="row.rank"
                  type="button"
                  class="cte-bar-row"
                  :title="resultPrimaryLabel(row)"
                  @click="focusResult(row)"
                >
                  <span class="cte-bar-label">{{ resultPrimaryLabel(row) }}</span>
                  <span class="cte-bar-track">
                    <span class="cte-bar-fill" :style="{ width: `${row.width}%`, background: barColor(row.rank) }"></span>
                  </span>
                  <span class="cte-bar-value">{{ formatScore(row.score) }}</span>
                </button>
              </div>
            </div>

            <div class="cte-overview-panel">
              <div class="cte-chart-title">Input coverage</div>
              <div class="cte-coverage-list">
                <div class="cte-coverage-item">
                  <span class="cte-coverage-label">Matched genes</span>
                  <span class="cte-coverage-value">{{ enrichmentResults.matchedGenes?.join(", ") || "-" }}</span>
                </div>
                <div class="cte-coverage-item">
                  <span class="cte-coverage-label">Unmatched genes</span>
                  <span class="cte-coverage-value">{{ enrichmentResults.unmatchedGenes?.join(", ") || "-" }}</span>
                </div>
                <div class="cte-coverage-item">
                  <span class="cte-coverage-label">{{ displayedResultCountLabel }}</span>
                  <span class="cte-coverage-value">{{ resultRows.length.toLocaleString() }}</span>
                </div>
              </div>
            </div>
          </div>

        </template>

        <template v-else-if="resultTab === 'bubble'">
          <div v-show="resultRows.length === 0" class="cte-empty-state">No enrichment results found.</div>
          <div v-show="resultRows.length > 0" class="cte-bubble-section">
            <div ref="bubbleChartEl" class="cte-bubble-chart"></div>
          </div>
        </template>

        <template v-else-if="resultTab === 'table'">
          <div class="cte-table-wrap">
            <table class="cte-table">
              <thead>
                <tr>
                  <th>Rank</th>
                  <th class="gsc-sort-th" @click="toggleSort(primaryResultSortColumn)">{{ primaryResultColumnLabel }} <span class="gsc-sort-arrow">{{ sortArrow(primaryResultSortColumn) }}</span></th>
                  <th class="gsc-sort-th" @click="toggleSort('overlap')">Overlap <span class="gsc-sort-arrow">{{ sortArrow('overlap') }}</span></th>
                  <th>Overlap genes</th>
                  <th class="gsc-sort-th" @click="toggleSort('enrichmentFold')">Enrichment fold <span class="gsc-sort-arrow">{{ sortArrow('enrichmentFold') }}</span></th>
                  <th class="gsc-sort-th" @click="toggleSort('pValue')">P value <span class="gsc-sort-arrow">{{ sortArrow('pValue') }}</span></th>
                  <th class="gsc-sort-th" @click="toggleSort('fdr')">FDR <span class="gsc-sort-arrow">{{ sortArrow('fdr') }}</span></th>
                  <th class="gsc-sort-th" @click="toggleSort('datasetCount')">Dataset count <span class="gsc-sort-arrow">{{ sortArrow('datasetCount') }}</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="resultRows.length === 0">
                  <td colspan="8" class="cte-no-data">No significant results found.</td>
                </tr>
                <tr
                  v-for="row in paginatedEnrichmentRows"
                  :key="row.rank"
                  :class="{ 'cte-selected-row': row.rank === selectedResultRank }"
                >
                  <td>{{ row.rank }}</td>
                  <td class="cte-cell-strong">{{ resultPrimaryLabel(row) }}</td>
                  <td>{{ row.overlap }}</td>
                  <td class="cte-gene-cell">
                    <button
                      v-for="gene in row.genes"
                      :key="`${row.rank}-${gene}`"
                      type="button"
                      class="cte-gene-chip cte-gene-chip--link"
                      @click="openGeneDetail(gene, row)"
                    >
                      {{ gene }}
                    </button>
                  </td>
                  <td>{{ formatFold(row.enrichmentFold) }}</td>
                  <td>{{ formatPValue(row.pValue) }}</td>
                  <td>{{ formatPValue(row.fdr) }}</td>
                  <td>{{ row.datasetCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="tableTotalPages > 1" class="cte-pagination">
            <button type="button" class="cte-page-btn" :disabled="tablePage <= 1" @click="setTablePage(1)">« First</button>
            <button type="button" class="cte-page-btn" :disabled="tablePage <= 1" @click="setTablePage(tablePage - 1)">‹ Prev</button>
            <span class="cte-page-info">Page <input v-model="tablePageInput" type="number" class="cte-page-jump" min="1" :max="tableTotalPages" @keyup.enter="goToTablePage" /> / {{ tableTotalPages }}</span>
            <button type="button" class="cte-page-btn" @click="goToTablePage">Go</button>
            <button type="button" class="cte-page-btn" :disabled="tablePage >= tableTotalPages" @click="setTablePage(tablePage + 1)">Next ›</button>
            <button type="button" class="cte-page-btn" :disabled="tablePage >= tableTotalPages" @click="setTablePage(tableTotalPages)">Last »</button>
          </div>
        </template>

      </div>
    </div>

    <!-- Hidden file input -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".txt,.csv,.tsv"
      style="display:none"
      @change="onFileSelected"
    />

    <!-- Upload preview modal -->
    <Teleport to="body">
      <div v-if="previewVisible" class="cte-modal-overlay" @click.self="previewVisible = false">
        <div class="cte-modal">
          <div class="cte-modal-header">
            <div class="cte-modal-title">Gene set preview</div>
            <button type="button" class="cte-modal-close" @click="previewVisible = false">×</button>
          </div>
          <div class="cte-modal-file">{{ previewFileName }}</div>
          <div class="cte-modal-meta">
            <span>{{ previewFormat }}</span>
            <span>{{ previewGenes.length }} genes detected</span>
            <span v-if="previewFiltered.length > 0" class="cte-meta-warn">
              {{ previewFiltered.length }} filtered out
            </span>
          </div>

          <div v-if="previewWarnings.length > 0" class="cte-modal-warnings">
            <div v-for="w in previewWarnings" :key="w" class="cte-warn-item">{{ w }}</div>
          </div>

          <div class="cte-modal-genes">
            <div v-for="g in previewGenes.slice(0, 30)" :key="g" class="cte-gene-chip">{{ g }}</div>
            <div v-if="previewGenes.length > 30" class="cte-gene-chip cte-gene-more">
              +{{ previewGenes.length - 30 }} more
            </div>
          </div>

          <div v-if="previewFiltered.length > 0" class="cte-modal-filtered">
            <div class="cte-filtered-title">Filtered entries ({{ previewFiltered.length }})</div>
            <div v-for="f in previewFiltered.slice(0, 10)" :key="f.value" class="cte-filtered-item">
              <code>{{ f.value }}</code> — {{ f.reason }}
            </div>
          </div>

          <div class="cte-modal-actions">
            <button type="button" class="soft-btn" @click="previewVisible = false">Cancel</button>
            <button type="button" class="primary-btn" @click="confirmPreviewGenes">
              Use {{ previewGenes.length }} gene{{ previewGenes.length !== 1 ? 's' : '' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
const baseUrl = import.meta.env.BASE_URL;
import { computed, nextTick, onMounted, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
import type {
  CellTypeEnrichmentDatasetOption,
  CellTypeEnrichmentResponse,
  EnrichmentResultRow,
} from "@/api/analysis";
import {
  fetchCellTypeEnrichmentDatasets,
  fetchCellTypeEnrichmentTissues,
  runCellTypeEnrichment,
} from "@/api/analysis";
import { parseFileContent, parseGenes as parseGenesSafe, type ParseResult } from "@/utils/geneParser";

const MAX_ENRICHMENT_GENES = 200;

const props = withDefaults(defineProps<{ active?: boolean }>(), {
  active: true,
});

// ===========================================================================
// Constants
// ===========================================================================

const EXAMPLE_GENES = [
  "TNNT2", "TNNI1", "TNNI3", "MYH6", "MYH7",
  "MYL2", "MYL4", "MYL7", "ACTN2", "NKX2-5",
  "GATA4", "NPPA", "TTN", "RYR2", "ATP2A2",
  "MYOZ2", "HAND2", "HEY2", "ANKRD1", "FHL2",
];
const EXAMPLE_TISSUE = "Heart";
const EXAMPLE_DATASET_ID = "H_000100";

const router = useRouter();

const tissueOptions = ref<string[]>([]);
const datasetOptions = ref<CellTypeEnrichmentDatasetOption[]>([]);
const tissueLoading = ref(false);
const datasetLoading = ref(false);
let datasetRequestGeneration = 0;
let pendingExampleDatasetId = "";

// ===========================================================================
// Reactive state — input
// ===========================================================================

const geneSetText = ref("");

// ===========================================================================
// Reactive state — settings (main)
// ===========================================================================

const tissue = ref("");
const referenceDatasetId = ref("");
type EnrichmentResultLevel = "cell_type" | "cluster";
const resultLevel = ref<EnrichmentResultLevel>("cell_type");
const displayedResultLevel = ref<EnrichmentResultLevel>("cell_type");
const markerReference = ref<"integration_expression">("integration_expression");

const resultLevelHelpLines = [
  { label: "Cell type", text: "Within the selected dataset, combines marker genes from clusters that share the same standardised cell-type name, then runs one enrichment test for each cell type." },
  { label: "Cluster", text: "Tests each cluster in the selected dataset separately. Results are uniquely identified by dataset and cluster, with the standardised cell type shown beside the cluster." },
];

// ===========================================================================
// Reactive state — settings (advanced, collapsed by default)
// ===========================================================================

const advancedOpen = ref(false);
const minOverlap = ref(1);
const fdrMethod = ref<"BH">("BH");

// ===========================================================================
// Reactive state — run & results
// ===========================================================================

const loading = ref(false);
type EnrichmentProgressStage = "IDLE" | "VALIDATING" | "QUERYING_ENRICHMENT" | "BUILDING_RESULTS" | "COMPLETED" | "FAILED";
const progressStage = ref<EnrichmentProgressStage>("IDLE");
const progressMessage = ref("Waiting to start.");
const hasRun = ref(false);
const enrichmentResults = ref<CellTypeEnrichmentResponse>(emptyResults());
const resultTab = ref("overview");
const fileInputRef = ref<HTMLInputElement | null>(null);
const inputError = ref<string | null>(null);
const PAGE_SIZE = 10;
const tablePage = ref(1);
const tablePageInput = ref("1");
const selectedResultRank = ref<number | null>(null);
let errorTimer: ReturnType<typeof setTimeout> | null = null;

function clearInputError() {
  inputError.value = null;
  if (errorTimer) { clearTimeout(errorTimer); errorTimer = null; }
}

function setInputError(msg: string) {
  clearInputError();
  inputError.value = msg;
  errorTimer = setTimeout(() => { inputError.value = null; }, 8000);
}

let validateDebounce: ReturnType<typeof setTimeout> | null = null;

function validateTextarea(text: string) {
  if (!text || !text.trim()) {
    clearInputError();
    return;
  }
  try {
    const result = parseGenesSafe(text);
    if (result.totalFound > MAX_ENRICHMENT_GENES || result.genes.length > MAX_ENRICHMENT_GENES) {
      setInputError(`Too many genes: ${result.totalFound}. Maximum is ${MAX_ENRICHMENT_GENES}.`);
      return;
    }
    if (result.filteredOut.length > 0) {
      const names = result.filteredOut.slice(0, 3).map((f) => f.value).join(", ");
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
  // Results belong to the submitted gene set.  Clear them as soon as the
  // textarea changes so counts from the previous run cannot be mistaken for
  // the new input while the debounced validation is still pending.
  hasRun.value = false;
  enrichmentResults.value = emptyResults();
  resultTab.value = "overview";
  resetResultPages();
  if (validateDebounce) clearTimeout(validateDebounce);
  validateDebounce = setTimeout(() => validateTextarea(geneSetText.value), 400);
}

// ---- upload preview ----
const previewVisible = ref(false);
const previewFileName = ref("");
const previewFormat = ref("");
const previewGenes = ref<string[]>([]);
const previewFiltered = ref<{ value: string; reason: string }[]>([]);
const previewWarnings = ref<string[]>([]);

const resultTabs = [
  { key: "overview", label: "Overview" },
  { key: "bubble", label: "Bubble" },
  { key: "table", label: "Results table" },
];

const progressStageLabel = computed(() => ({
  IDLE: "Ready",
  VALIDATING: "Validating input",
  QUERYING_ENRICHMENT: "Running enrichment",
  BUILDING_RESULTS: "Building results",
  COMPLETED: "Completed",
  FAILED: "Analysis failed",
})[progressStage.value]);

const progressWidth = computed(() => ({
  IDLE: 0,
  VALIDATING: 18,
  QUERYING_ENRICHMENT: 62,
  BUILDING_RESULTS: 92,
  COMPLETED: 100,
  FAILED: 100,
})[progressStage.value]);

// ===========================================================================
// Computed
// ===========================================================================

const parsedGeneResult = computed<ParseResult | null>(() => {
  const text = geneSetText.value;
  if (!text || !text.trim()) return null;
  try {
    return parseGenesSafe(text);
  } catch {
    return null;
  }
});
const parsedGenes = computed<string[]>(() => parsedGeneResult.value?.genes ?? []);
const parsedGeneCount = computed(() => parsedGeneResult.value?.totalFound ?? 0);
const geneLimitExceeded = computed(() => parsedGeneCount.value > MAX_ENRICHMENT_GENES || parsedGenes.value.length > MAX_ENRICHMENT_GENES);
const displayedInputError = computed(() => geneLimitExceeded.value
  ? `Too many genes: ${parsedGeneCount.value}. Maximum is ${MAX_ENRICHMENT_GENES}.`
  : inputError.value);

const inputGeneCount = computed(() => {
  return parsedGeneCount.value > 0 ? parsedGeneCount.value.toLocaleString() : "—";
});

const matchedGeneCount = computed(() => {
  if (!hasRun.value) return "—";
  return (enrichmentResults.value.matchedGenes?.length ?? 0).toLocaleString();
});

const unmatchedGeneCount = computed(() => {
  if (!hasRun.value) return "—";
  const unmatched = enrichmentResults.value.unmatchedGenes ?? [];
  return unmatched.length > 0 ? unmatched.length.toLocaleString() : "—";
});

type OverviewRow = EnrichmentResultRow & { score: number; width: number };

const resultRows = computed<EnrichmentResultRow[]>(() => enrichmentResults.value.results ?? []);

function resultPrimaryLabel(row: EnrichmentResultRow): string {
  if (displayedResultLevel.value === "cluster") {
    const cellType = row.cellType || "Unknown";
    const cluster = row.context || "Unknown";
    return `${cellType} / ${cluster}`;
  }
  return row.cellType || "Unknown";
}

const topEnrichedLabel = computed(() => {
  const topRow = resultRows.value[0];
  return topRow ? resultPrimaryLabel(topRow) : "—";
});

const topEnrichedSummaryLabel = computed(() =>
  displayedResultLevel.value === "cluster"
    ? "Top enriched cluster"
    : "Top enriched cell type",
);

const overviewTitle = computed(() =>
  displayedResultLevel.value === "cluster"
    ? "Top enriched clusters"
    : "Top enriched cell types",
);

const primaryResultColumnLabel = computed(() =>
  displayedResultLevel.value === "cluster" ? "Cell type / Cluster" : "Cell type",
);
const primaryResultSortColumn = computed(() =>
  displayedResultLevel.value === "cluster" ? "context" : "cellType",
);

const displayedResultCountLabel = computed(() => {
  if (displayedResultLevel.value === "cluster") return "Displayed clusters";
  return "Displayed cell types";
});

/* ---- Bubble chart ---- */
const bubbleChartEl = ref<HTMLElement | null>(null);
let bubbleChart: echarts.ECharts | null = null;
let bubbleResizeObserver: ResizeObserver | null = null;
let bubbleRenderTimer: ReturnType<typeof setTimeout> | null = null;
const BUBBLE_VISIBLE_LIMIT = 30;
const BUBBLE_COLOR_RANGE = ["#A8D8D0", "#5FA89C", "#3A7D8C", "#D98C3A", "#C05045"];

function clearBubbleRenderTimer() {
  if (bubbleRenderTimer) {
    clearTimeout(bubbleRenderTimer);
    bubbleRenderTimer = null;
  }
}

function scheduleBubbleChartRender(delay = 0, retry = 0) {
  if (resultTab.value !== "bubble" || !props.active) return;
  clearBubbleRenderTimer();
  bubbleRenderTimer = setTimeout(() => {
    bubbleRenderTimer = null;
    renderBubbleChart(retry);
  }, delay);
}

function disposeBubbleChart() {
  clearBubbleRenderTimer();
  bubbleResizeObserver?.disconnect();
  bubbleResizeObserver = null;
  bubbleChart?.dispose();
  bubbleChart = null;
}

function bubbleXAxisLabel(row: EnrichmentResultRow): string {
  return resultPrimaryLabel(row);
}

function renderBubbleChartStable(retry = 0) {
  if (resultTab.value !== "bubble" || !props.active) return;

  if (!bubbleChartEl.value) {
    if (retry < 8) scheduleBubbleChartRender(100, retry + 1);
    return;
  }

  const el = bubbleChartEl.value;
  if (el.clientWidth === 0 || el.clientHeight === 0) {
    if (retry < 8) scheduleBubbleChartRender(100, retry + 1);
    return;
  }

  const rows = resultRows.value;
  if (!rows.length) {
    bubbleChart?.clear();
    return;
  }

  if (bubbleChart && bubbleChart.getDom() !== el) {
    disposeBubbleChart();
  }

  if (!bubbleChart) {
    bubbleChart = echarts.init(el);
    bubbleResizeObserver = new ResizeObserver(() => {
      if (resultTab.value === "bubble" && props.active) {
        bubbleChart?.resize();
      }
    });
    bubbleResizeObserver.observe(el);
  }

  const raw = rows.slice(0, BUBBLE_VISIBLE_LIMIT).map((row, index) => {
    const fdrScore = scoreFromFdr(row.fdr ?? row.pValue ?? (row as any).pvalue);
    const fold = Number.isFinite(row.enrichmentFold) ? row.enrichmentFold : 0;
    return {
      name: bubbleXAxisLabel(row),
      x: index,
      y: fdrScore,
      size: Math.max(row.overlap ?? 1, 1),
      fold,
      context: row.context ?? "",
      cellType: row.cellType ?? "Unknown",
    };
  });

  const maxScore = Math.max(...raw.map((row) => row.y), 1);
  const minFold = Math.min(...raw.map((row) => row.fold));
  const maxFold = Math.max(...raw.map((row) => row.fold), minFold + 0.01);
  const data = raw.map((row) => ({
    name: row.name,
    value: [row.x, row.y, row.size, row.fold, row.context, row.cellType],
  }));

  bubbleChart.setOption({
    tooltip: {
      trigger: "item",
      backgroundColor: "#fff",
      borderColor: "#C4D4CD",
      textStyle: { color: "#1B2A27", fontSize: 12 },
      formatter: (p: any) => {
        const d = p.data;
        const detail = displayedResultLevel.value === "cluster"
          ? `Cell type: ${d.value[5] || "Unknown"}`
          : "";
        return `<b>${d.name}</b><br/>Fold: ${Number(d.value[3]).toFixed(2)}<br/>-log10(FDR): ${Number(d.value[1]).toFixed(2)}<br/>Genes: ${d.value[2]}${detail ? `<br/>${detail}` : ""}`;
      },
    },
    grid: { left: 70, right: 46, top: 24, bottom: 96 },
    xAxis: {
      name: displayedResultLevel.value === "cluster"
        ? "Cell type / Cluster"
        : "Cell type",
      nameLocation: "middle",
      nameGap: 72,
      type: "category",
      data: raw.map((row) => row.name),
      axisLabel: {
        color: "#5E6C67",
        fontSize: 10,
        interval: 0,
        rotate: 38,
        overflow: "truncate",
        width: 92,
      },
      axisTick: { alignWithLabel: true },
      splitLine: { show: false },
    },
    yAxis: {
      name: "-log10(FDR)",
      nameLocation: "middle",
      nameGap: 50,
      type: "value",
      min: 0,
      max: Math.ceil(maxScore),
      axisLabel: { color: "#5E6C67", fontSize: 11 },
      splitLine: { lineStyle: { color: "rgba(143,165,156,0.12)" } },
    },
    visualMap: {
      min: minFold,
      max: maxFold,
      dimension: 3,
      inRange: { color: BUBBLE_COLOR_RANGE },
      calculable: true,
      orient: "vertical",
      right: 6,
      top: "center",
      text: ["High fold", "Low fold"],
      textStyle: { color: "#5E6C67", fontSize: 10 },
    },
    series: [{
      type: "scatter",
      data,
      encode: { x: 0, y: 1 },
      symbolSize: (val: number[]) => {
        const size = val?.[2] ?? 1;
        return Math.max(12, Math.min(72, size * 4));
      },
      emphasis: {
        focus: "series",
        scale: 1.35,
        label: { show: true, formatter: "{b}", position: "top", fontSize: 12, fontWeight: "bold" },
      },
    }],
  }, true);

  requestAnimationFrame(() => bubbleChart?.resize());
}

function renderBubbleChart(retry = 0) {
  renderBubbleChartStable(retry);
}

watch(resultRows, () => {
  nextTick(() => scheduleBubbleChartRender());
}, { deep: true });

watch(resultTab, async (tab) => {
  if (tab === "bubble") {
    await nextTick();
    await nextTick();
    scheduleBubbleChartRender();
  } else {
    disposeBubbleChart();
  }
});

watch(() => props.active, async (active) => {
  if (active && resultTab.value === "bubble") {
    await nextTick();
    scheduleBubbleChartRender(80);
  } else {
    disposeBubbleChart();
  }
});

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
  setTablePage(1);
}

function sortArrow(col: string): string {
  if (sortColumn.value !== col) return "⇅";
  return sortDirection.value === "desc" ? "▼" : "▲";
}

const sortedRows = computed<EnrichmentResultRow[]>(() => {
  const rows = [...resultRows.value];
  if (!sortColumn.value) return rows;
  const k = sortColumn.value as keyof EnrichmentResultRow;
  const dir = sortDirection.value === "desc" ? -1 : 1;
  if (k === "cellType" || k === "context") {
    rows.sort((a, b) => dir * String(a[k] ?? "").localeCompare(String(b[k] ?? ""), undefined, { numeric: true, sensitivity: "base" }));
    return rows;
  }
  rows.sort((a, b) => dir * ((Number(a[k]) || 0) - (Number(b[k]) || 0)));
  return rows;
});

const BAR_COLORS = [
  "#8FA59C", "#7BA7C9", "#E8A87C", "#C4A882",
  "#8FC9B3", "#D4A0C4", "#D9826B", "#B088C0",
];

function barColor(rank: number): string {
  return BAR_COLORS[(rank - 1) % BAR_COLORS.length] ?? "#8FA59C";
}

const topOverviewRows = computed<OverviewRow[]>(() => {
  const scored = resultRows.value.slice(0, 8).map((row) => ({
    ...row,
    score: scoreFromFdr(row.fdr),
    width: 0,
  }));
  const maxScore = Math.max(0.001, ...scored.map((row) => row.score));
  return scored.map((row) => ({
    ...row,
    width: Math.max(4, Math.min(100, (row.score / maxScore) * 100)),
  }));
});

const tableTotalPages = computed(() => pageCount(sortedRows.value.length));

const paginatedEnrichmentRows = computed(() => paginateRows(sortedRows.value, tablePage.value));

function setTablePage(page: number) {
  tablePage.value = Math.min(tableTotalPages.value, Math.max(1, page));
  tablePageInput.value = String(tablePage.value);
}

function goToTablePage() {
  const page = Number.parseInt(tablePageInput.value, 10);
  if (!Number.isFinite(page)) {
    tablePageInput.value = String(tablePage.value);
    return;
  }
  setTablePage(page);
}

watch(tablePage, page => {
  tablePageInput.value = String(page);
});

watch(resultTab, () => {
  clampResultPages();
});

// ===========================================================================
// Methods
// ===========================================================================

function emptyResults(): CellTypeEnrichmentResponse {
  return {
    inputGenes: [],
    matchedGenes: [],
    unmatchedGenes: [],
    totalResults: 0,
    significantResults: 0,
    topEnrichedCellType: null,
    results: [],
  };
}

function loadExample() {
  geneSetText.value = EXAMPLE_GENES.join("\n");
  pendingExampleDatasetId = EXAMPLE_DATASET_ID;
  if (tissue.value === EXAMPLE_TISSUE) {
    void loadDatasetOptions(EXAMPLE_TISSUE);
  } else {
    tissue.value = EXAMPLE_TISSUE;
  }
}

function clearInput() {
  geneSetText.value = "";
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
      showPreview(file.name, result);
    } catch (err: any) {
      setInputError(`File rejected: ${err.message}`);
    }
  };
  reader.readAsText(file);
}

function showPreview(fileName: string, result: ParseResult) {
  if (result.totalFound > MAX_ENRICHMENT_GENES || result.genes.length > MAX_ENRICHMENT_GENES) {
    setInputError(`Too many genes: ${result.totalFound}. Maximum is ${MAX_ENRICHMENT_GENES}.`);
    return;
  }
  previewFileName.value = fileName;
  previewFormat.value =
    result.format === "csv" ? "CSV (auto-detected)" :
    result.format === "tsv" ? "TSV (auto-detected)" :
    "Plain text";
  previewGenes.value = result.genes;
  previewFiltered.value = result.filteredOut;
  previewWarnings.value = result.warnings;
  previewVisible.value = true;
}

function confirmPreviewGenes() {
  if (previewGenes.value.length > MAX_ENRICHMENT_GENES) {
    setInputError(`Too many genes: ${previewGenes.value.length}. Maximum is ${MAX_ENRICHMENT_GENES}.`);
    previewVisible.value = false;
    return;
  }
  geneSetText.value = previewGenes.value.join("\n");
  previewVisible.value = false;
}

async function runAnalysis() {
  if (loading.value || parsedGenes.value.length === 0) return;
  if (geneLimitExceeded.value) {
    setInputError(`Too many genes: ${parsedGeneCount.value}. Maximum is ${MAX_ENRICHMENT_GENES}.`);
    return;
  }
  if (!tissue.value || !referenceDatasetId.value) return;

  const requestedResultLevel = resultLevel.value;
  loading.value = true;
  progressStage.value = "VALIDATING";
  progressMessage.value = "Checking the gene set and selected reference dataset.";
  hasRun.value = false;
  enrichmentResults.value = emptyResults();
  resultTab.value = "overview";
  resetResultPages();
  try {
    await nextTick();
    progressStage.value = "QUERYING_ENRICHMENT";
    progressMessage.value = "Testing marker-gene overlap and applying BH correction.";
    const response = await runCellTypeEnrichment({
      geneSymbols: parsedGenes.value,
      tissue: tissue.value,
      datasetId: referenceDatasetId.value,
      markerReference: markerReference.value,
      resultLevel: requestedResultLevel,
      minOverlap: minOverlap.value,
      backgroundUniverse: "selected_marker_reference",
      fdrMethod: fdrMethod.value,
    });
    progressStage.value = "BUILDING_RESULTS";
    progressMessage.value = "Preparing the overview, bubble chart, and paginated table.";
    enrichmentResults.value = response;
    displayedResultLevel.value = requestedResultLevel;
    resetResultPages();
    hasRun.value = true;
    progressStage.value = "COMPLETED";
    progressMessage.value = "Enrichment results are ready.";
  } catch (err) {
    progressStage.value = "FAILED";
    progressMessage.value = "Cell type enrichment could not be completed.";
    console.error("Cell type enrichment failed:", err);
  } finally {
    loading.value = false;
  }
}

function resetAll() {
  geneSetText.value = "";
  tissue.value = "";
  referenceDatasetId.value = "";
  datasetOptions.value = [];
  resultLevel.value = "cell_type";
  displayedResultLevel.value = "cell_type";
  markerReference.value = "integration_expression";
  minOverlap.value = 1;
  fdrMethod.value = "BH";
  progressStage.value = "IDLE";
  progressMessage.value = "Waiting to start.";
  advancedOpen.value = false;
  hasRun.value = false;
  enrichmentResults.value = emptyResults();
  resultTab.value = "overview";
  resetResultPages();
}

function resetResultPages() {
  tablePage.value = 1;
  selectedResultRank.value = null;
}

function pageCount(total: number): number {
  return Math.max(1, Math.ceil(total / PAGE_SIZE));
}

function paginateRows<T>(rows: T[], page: number): T[] {
  const current = Math.max(1, page);
  const start = (current - 1) * PAGE_SIZE;
  return rows.slice(start, start + PAGE_SIZE);
}

function clampResultPages() {
  setTablePage(Math.min(tablePage.value, tableTotalPages.value));
}

function scoreFromFdr(value: number | undefined | null): number {
  if (value == null || !Number.isFinite(value)) return 0;
  const safe = Math.max(value, 1e-300);
  return Math.max(0, -Math.log10(safe));
}

// ── Display helpers ────────────────────────────────────────────────

function formatFold(value: number | undefined | null): string {
  if (value == null) return "—";
  return value.toFixed(2);
}

function formatPValue(value: number | undefined | null): string {
  if (value == null) return "—";
  if (value < 1e-4) return value.toExponential(2);
  return value.toFixed(4);
}

function formatScore(value: number | undefined | null): string {
  if (value == null || !Number.isFinite(value)) return "0.00";
  return value.toFixed(2);
}

function focusResult(row: { rank?: number; genes?: string[] }) {
  selectedResultRank.value = row.rank ?? null;
  const index = resultRows.value.findIndex((item) => item.rank === row.rank);
  tablePage.value = index >= 0 ? Math.floor(index / PAGE_SIZE) + 1 : 1;
  resultTab.value = "table";
}


function openGeneDetail(gene: string, row: { rank?: number }) {
  const symbol = gene.trim();
  if (!symbol) return;
  selectedResultRank.value = row.rank ?? selectedResultRank.value;
  router.push({
    path: "/feature-detail",
    query: {
      type: "gene",
      gene: symbol,
      domain: "integration",
      source: "analysis_enrichment",
      returnTo: "/analysis",
      module: "cell_type_enrichment",
    },
  });
}

async function loadDatasetOptions(selectedTissue: string) {
  const generation = ++datasetRequestGeneration;
  referenceDatasetId.value = "";
  datasetOptions.value = [];
  hasRun.value = false;
  enrichmentResults.value = emptyResults();
  resetResultPages();
  if (!selectedTissue) return;
  if (selectedTissue !== EXAMPLE_TISSUE) pendingExampleDatasetId = "";
  datasetLoading.value = true;
  try {
    const options = await fetchCellTypeEnrichmentDatasets(selectedTissue);
    if (generation === datasetRequestGeneration) {
      datasetOptions.value = options;
      if (
        selectedTissue === EXAMPLE_TISSUE
        && pendingExampleDatasetId
        && options.some(option => option.dataset_id === pendingExampleDatasetId)
      ) {
        referenceDatasetId.value = pendingExampleDatasetId;
        pendingExampleDatasetId = "";
      }
    }
  } catch (error) {
    if (generation === datasetRequestGeneration) {
      ElMessage.error("Failed to load datasets for the selected tissue.");
    }
  } finally {
    if (generation === datasetRequestGeneration) datasetLoading.value = false;
  }
}

watch(tissue, selectedTissue => {
  void loadDatasetOptions(selectedTissue);
});

watch(referenceDatasetId, (selectedDataset, previousDataset) => {
  hasRun.value = false;
  enrichmentResults.value = emptyResults();
  resetResultPages();
  if (selectedDataset && selectedDataset !== previousDataset) {
    resultLevel.value = "cell_type";
  }
});

watch(resultLevel, () => {
  hasRun.value = false;
  enrichmentResults.value = emptyResults();
  resetResultPages();
});

onMounted(async () => {
  // Remove state written by older builds; current-session state is owned by KeepAlive.
  sessionStorage.removeItem("oscar.cellTypeEnrichment.state.v1");
  tissueLoading.value = true;
  try {
    tissueOptions.value = await fetchCellTypeEnrichmentTissues();
  } catch {
    ElMessage.error("Failed to load tissues for cell type enrichment.");
  } finally {
    tissueLoading.value = false;
  }
  nextTick(() => scheduleBubbleChartRender());
});

onDeactivated(() => {
  previewVisible.value = false;
});

onBeforeUnmount(() => {
  disposeBubbleChart();
});
</script>

<style scoped>
/* ── Root ────────────────────────────────────────────────────────── */
.cte-root {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ── Description ─────────────────────────────────────────────────── */
.cte-desc {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  font-weight: 750;
  line-height: 1.55;
}

/* ── Cards ───────────────────────────────────────────────────────── */
.cte-card {
  box-sizing: border-box;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  box-shadow: var(--shadow-card);
}

.cte-card-title {
  font-size: 15px;
  font-weight: 900;
  margin: 0 0 12px;
  color: var(--text);
}
.cte-max-badge {
  display: inline-block;
  margin-left: 10px;
  padding: 2px 9px;
  border: 1px solid var(--brand-primary-3);
  border-radius: 6px;
  background: rgba(78,133,118,0.08);
  color: var(--brand-primary-3);
  font-size: 10.5px;
  font-weight: 800;
  vertical-align: middle;
}

.cte-settings-card {
  position: relative;
}

.cte-settings-card .cte-card-title {
  padding-right: 150px;
}

/* ── Textarea ────────────────────────────────────────────────────── */
.cte-textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 96px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface-2);
  color: var(--text);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.cte-textarea:focus {
  outline: none;
  border-color: var(--border-brand);
  box-shadow: 0 0 0 3px rgba(143, 165, 156, 0.14);
}

.cte-textarea:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.cte-textarea--error {
  border-color: #e05555 !important;
  box-shadow: 0 0 0 3px rgba(224, 85, 85, 0.12) !important;
}

.cte-input-error {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px 13px;
  background: #fef2f2;
  border: 1px solid #f5b8b8;
  border-radius: 10px;
  margin-bottom: 10px;
  color: #b53b3b;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
  overflow: hidden;
}

.cte-error-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #e05555;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
}

.cte-error-text {
  flex: 1;
  min-width: 0;
}

.cte-error-close {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #b53b3b;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.cte-error-close:hover {
  background: rgba(224, 85, 85, 0.12);
}

.cte-error-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: #e05555;
  border-radius: 0 0 10px 10px;
  animation: cteErrorShrink 8s linear forwards;
}

@keyframes cteErrorShrink {
  from { width: 100%; }
  to   { width: 0%; }
}

.cte-hint {
  margin: 6px 0 10px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

/* ── Button rows ─────────────────────────────────────────────────── */
.cte-btn-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.cte-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.cte-progress-card {
  margin-top: 12px;
  padding: 14px 15px 13px;
  border: 1px solid rgba(143, 165, 156, 0.34);
  border-radius: 13px;
  background: linear-gradient(145deg, rgba(247, 250, 248, 0.98), rgba(239, 246, 243, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.cte-progress-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 11px;
}

.cte-progress-stage {
  color: var(--text);
  font-size: 13px;
  font-weight: 900;
}

.cte-progress-message {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11.5px;
  font-weight: 700;
  line-height: 1.4;
}

.cte-progress-status {
  flex: 0 0 auto;
  padding: 3px 9px;
  border: 1px solid rgba(95, 125, 112, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--brand-primary-3);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.cte-progress-track {
  position: relative;
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(143, 165, 156, 0.18);
  box-shadow: inset 0 1px 2px rgba(39, 66, 58, 0.08);
}

.cte-progress-fill {
  position: relative;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
  background: linear-gradient(90deg, #9bb4aa, var(--brand-primary-3), #668c7d);
  box-shadow: 0 0 12px rgba(95, 125, 112, 0.28);
  transition: width 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.cte-progress-track.indeterminate .cte-progress-fill::after {
  content: "";
  position: absolute;
  inset: 0;
  width: 42%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.72), transparent);
  animation: cteProgressShimmer 1.35s ease-in-out infinite;
}

.cte-progress-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 9px;
}

.cte-progress-steps span {
  position: relative;
  padding-top: 8px;
  color: #9aa7a2;
  font-size: 10px;
  font-weight: 800;
  text-align: center;
  transition: color 0.2s ease;
}

.cte-progress-steps span::before {
  content: "";
  position: absolute;
  top: 0;
  left: 50%;
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: #cbd4d0;
  transform: translateX(-50%);
}

.cte-progress-steps span.done,
.cte-progress-steps span.active {
  color: var(--brand-primary-3);
}

.cte-progress-steps span.done::before,
.cte-progress-steps span.active::before {
  background: var(--brand-primary-3);
  box-shadow: 0 0 0 3px rgba(143, 165, 156, 0.16);
}

/* ── Buttons ─────────────────────────────────────────────────────── */
.primary-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 8px 22px;
  border: none;
  border-radius: 11px;
  background: var(--brand-primary-3, #8faea3);
  color: #fff;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(95, 125, 112, 0.18);
  transition: background 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.primary-btn:hover:not(:disabled) {
  background: #7f9f94;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(95, 125, 112, 0.24);
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.soft-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 34px;
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--surface);
  color: var(--text);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.soft-btn:hover:not(:disabled) {
  border-color: var(--border-brand);
  background: var(--surface-2);
}

.soft-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 6px 10px;
  border: none;
  background: transparent;
  color: var(--brand-primary-3, #8faea3);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
  transition: color 0.18s ease;
}

.link-btn:hover:not(:disabled) {
  color: #6f8f84;
}

.link-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.link-btn--sm {
  min-height: auto;
  padding: 2px 8px;
  font-size: 11px;
}

.btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,.35);
  border-top-color: #fff;
  border-radius: 999px;
  animation: spin .6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes cteProgressShimmer {
  from { transform: translateX(-120%); }
  to { transform: translateX(340%); }
}

/* ── Two-column row ──────────────────────────────────────────────── */
.cte-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(360px, 1fr);
  gap: 14px;
  align-items: stretch;
}

.cte-builder-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
  height: 100%;
}

.cte-builder-section {
  position: relative;
  min-width: 0;
}

.cte-builder-section + .cte-builder-section {
  padding-top: 14px;
  border-top: 1px solid var(--border);
}

.cte-side-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  height: 100%;
}

.cte-image-card {
  flex: 0 0 auto;
  aspect-ratio: 1619 / 895;
  height: auto;
  max-height: none;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 0;
}

.cte-slot-img {
  width: 100%;
  height: 100%;
  max-width: none;
  max-height: none;
  object-fit: contain;
  border-radius: inherit;
  display: block;
}

/* ── Gene stats ──────────────────────────────────────────────────── */
.cte-gene-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-item {
  flex: 1 1 120px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 120px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface-2);
}

.stat-label {
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
}

.stat-value {
  font-size: 16px;
  font-weight: 900;
  color: var(--text);
}

.stat-value--muted { color: var(--muted); font-size: 14px; }
.stat-item--muted { background: #f3f4f6; border-color: rgba(160,165,175,0.25); }
.stat-item--bad { background: #fdf0f0; border-color: rgba(200,125,125,0.22); }

/* ── Fields (single column inside settings card) ─────────────────── */
.cte-fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cte-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.cte-field-label {
  font-size: 13px;
  font-weight: 900;
  color: rgba(39, 66, 58, 0.84);
}

.cte-field-label em {
  display: inline-flex;
  align-items: center;
  min-height: 17px;
  margin-left: 5px;
  padding: 1px 7px;
  border: 1px solid rgba(194, 65, 65, 0.24);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 247, 247, 0.98), rgba(251, 226, 226, 0.92));
  color: #b93838;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 2px 6px rgba(185, 56, 56, 0.09);
  font-size: 9px;
  font-style: normal;
  font-weight: 900;
  letter-spacing: 0.055em;
  line-height: 1;
  text-transform: uppercase;
  vertical-align: 1px;
}

.cte-field-label--help {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 5px;
}

.cte-inline-help-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  flex: 0 0 15px;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: var(--surface);
  color: var(--brand-primary-3);
  font-size: 9px;
  font-weight: 900;
  line-height: 1;
  cursor: help;
}

.cte-inline-help-icon:focus-visible {
  outline: 2px solid rgba(78, 133, 118, 0.3);
  outline-offset: 2px;
}

.cte-result-level-help {
  display: grid;
  gap: 5px;
  max-width: 340px;
  line-height: 1.45;
}

.cte-select {
  width: 100%;
}

.cte-select :deep(.el-select__wrapper) {
  min-height: 30px;
  border-radius: 9px;
}

.cte-number {
  width: 100%;
}

.cte-number :deep(.el-input__wrapper) {
  min-height: 30px;
  border-radius: 9px;
}

/* ── Settings card: left sub-column = main, right = advanced ────── */
.cte-settings-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  margin-bottom: 10px;
}

.cte-settings-main .cte-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ── Advanced column (inside settings card, right side) ─────────── */
.cte-settings-advanced-col {
  display: flex;
  flex-direction: column;
}

.cte-advanced-toggle {
  position: absolute;
  top: 16px;
  right: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  border: none;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
  transition: color 0.18s ease;
  align-self: flex-start;
}

.cte-advanced-toggle:hover {
  color: var(--text);
}

.cte-toggle-chev {
  display: inline-block;
  font-size: 10px;
  transition: transform 0.18s ease;
}

.cte-advanced-toggle.open .cte-toggle-chev {
  transform: rotate(90deg);
}

.cte-advanced {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

/* ── How it works ────────────────────────────────────────────────── */
.how-steps {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.how-step {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.how-num {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  font-size: 13px;
  font-weight: 900;
  border: 1px solid var(--nav-active-border);
}

.cte-how-card .cte-card-title {
  font-size: 16px;
}

.cte-side-column .cte-how-card {
  flex: 1 1 auto;
  min-height: 0;
  height: auto;
  padding: 16px 18px;
}

.how-step-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.how-step-body strong {
  font-size: 12.5px;
  font-weight: 900;
  color: var(--text);
}

.how-step-body span {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--muted);
  line-height: 1.38;
}

.how-note {
  margin: 9px 0 0;
  padding-top: 8px;
  border-top: 1px solid var(--border);
  font-size: 11.5px;
  font-weight: 700;
  color: var(--muted);
  line-height: 1.45;
}

/* ── Results ─────────────────────────────────────────────────────── */
.cte-results {
  /* full width */
}

/* ── Summary cards ───────────────────────────────────────────────── */
.cte-summary-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}

.cte-summary-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 16px 10px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  text-align: center;
}

.sum-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 28px;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.2;
  color: var(--text);
}

.sum-num--sm {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.sum-label {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 100%;
  min-height: 28px;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.3;
  color: var(--muted);
}

/* ── Chart placeholder ───────────────────────────────────────────── */
.cte-chart-placeholder {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  padding: 16px;
  margin-bottom: 14px;
}

.cte-chart-title {
  font-size: 14px;
  font-weight: 900;
  margin-bottom: 12px;
}

.cte-chart-sub {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  margin-top: -8px;
  margin-bottom: 10px;
}

.cte-bubble-section {
  padding-top: 4px;
}

.cte-bubble-chart {
  width: 100%;
  height: 430px;
}

.cte-chart-area {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  border: 1px dashed var(--border-brand);
  border-radius: 10px;
  background: var(--surface-2);
}

.cte-chart-hint {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
}

.cte-overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(260px, 0.65fr);
  gap: 12px;
}

.cte-overview-panel {
  min-width: 0;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface);
  padding: 14px;
}

.cte-bar-list,
.cte-coverage-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.cte-bar-row {
  display: grid;
  grid-template-columns: minmax(160px, 0.8fr) minmax(120px, 1fr) 64px;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 34px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  text-align: left;
}

.cte-bar-row:hover {
  background: var(--surface-2);
}

.cte-bar-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 800;
}

.cte-bar-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface-2);
  border: 1px solid var(--border);
}

.cte-bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--brand-primary-3, #8faea3);
}

.cte-bar-value {
  text-align: right;
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
}

.cte-coverage-item {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 9px 0;
  border-bottom: 1px solid var(--border);
}

.cte-coverage-item:last-child {
  border-bottom: 0;
}

.cte-coverage-label {
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
}

.cte-coverage-value {
  min-width: 0;
  color: var(--text);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
  word-break: break-word;
}

.cte-empty-state {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

/* ── Tabs ────────────────────────────────────────────────────────── */
.cte-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.cte-tab {
  min-height: 34px;
  padding: 8px 16px;
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease;
}

.cte-tab:hover {
  color: var(--text);
}

.cte-tab.active {
  color: var(--text);
  border-bottom-color: var(--brand-primary-3, #8faea3);
  font-weight: 900;
}

/* ── Table ───────────────────────────────────────────────────────── */
.cte-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface);
}

.cte-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  color: var(--el-text-color-regular, #606266);
}

.cte-table th,
.cte-table td {
  padding: 10px 12px;
  text-align: center;
  vertical-align: middle;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.cte-table th {
  background: var(--surface-2);
  font-weight: 600;
  color: var(--el-text-color-secondary, #909399);
  font-size: 14px;
}

.cte-table tbody tr:last-child td {
  border-bottom: none;
}

.cte-cell-strong {
  font-weight: 700;
  color: var(--el-text-color-primary, #303133);
}

.cte-no-data {
  text-align: center !important;
  color: var(--muted);
  padding: 32px !important;
}

.cte-selected-row td {
  background: rgba(143, 174, 163, 0.12);
}

.cte-gene-cell {
  max-width: 520px;
  white-space: normal !important;
}

.cte-gene-chip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  margin: 2px;
  padding: 2px 7px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-2);
  color: var(--text);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.cte-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  flex-wrap: wrap;
}

.cte-page-jump {
  width: 52px;
  padding: 2px 4px;
  border: 1px solid var(--border);
  border-radius: 4px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
  background: var(--surface);
}

.cte-page-jump::-webkit-inner-spin-button,
.cte-page-jump::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.cte-page-btn {
  min-height: 30px;
  padding: 4px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.cte-page-btn:hover:not(:disabled) {
  border-color: var(--border-brand);
  background: var(--surface-2);
}

.cte-page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.cte-page-info {
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
}

/* ── Placeholder tab content ─────────────────────────────────────── */
.cte-placeholder-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  background: var(--surface-2);
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  padding: 24px;
}

/* ── Upload preview modal ────────────────────────────────────────── */
.cte-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(18, 24, 38, 0.44);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.cte-modal {
  width: min(560px, calc(100vw - 32px));
  max-height: 85vh;
  overflow-y: auto;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-hover);
}

.cte-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.cte-modal-title {
  font-size: 18px;
  font-weight: 900;
}

.cte-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  font-size: 20px;
  font-weight: 900;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, color 0.15s;
}
.cte-modal-close:hover {
  background: var(--surface-3);
  color: var(--text);
}

.cte-modal-file {
  font-size: 13px;
  color: var(--muted);
  font-family: "JetBrains Mono", monospace;
  margin-bottom: 12px;
  word-break: break-all;
}

.cte-modal-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}

.cte-meta-warn {
  color: #c88a3b;
}

.cte-modal-warnings {
  background: #fef6e8;
  border: 1px solid #f0d49a;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  font-size: 12px;
  font-weight: 700;
}

.cte-warn-item {
  color: #8a6c2f;
  line-height: 1.5;
}

.cte-modal-genes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.cte-gene-chip {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  font-size: 12px;
  font-weight: 800;
  color: var(--text);
  font-family: "JetBrains Mono", monospace;
}

.cte-gene-more {
  color: var(--muted);
  font-style: italic;
}

.cte-gene-chip--link {
  appearance: none;
  cursor: pointer;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

.cte-gene-chip--link:hover {
  border-color: var(--border-brand);
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  transform: translateY(-1px);
}

.cte-gene-chip--link:focus-visible {
  outline: 2px solid var(--border-brand);
  outline-offset: 2px;
}

.cte-modal-filtered {
  border-top: 1px solid var(--border);
  padding-top: 10px;
  margin-bottom: 16px;
}

.cte-filtered-title {
  font-size: 12px;
  font-weight: 800;
  color: var(--muted);
  margin-bottom: 6px;
}

.cte-filtered-item {
  font-size: 11px;
  color: var(--muted);
  line-height: 1.6;
}

.cte-filtered-item code {
  font-family: "JetBrains Mono", monospace;
  background: var(--surface-2);
  padding: 1px 6px;
  border-radius: 4px;
}

.cte-modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

/* ── Responsive ──────────────────────────────────────────────────── */
@media (max-width: 1024px) {
  .cte-workbench {
    grid-template-columns: 1fr;
  }

  .cte-overview-grid {
    grid-template-columns: 1fr;
  }

  .cte-image-card {
    flex-basis: auto;
    height: auto;
    max-height: none;
  }

  .cte-side-column .cte-how-card {
    flex: initial;
  }
}

@media (max-width: 760px) {

  .cte-image-slot {
    display: none;
  }

  .cte-settings-body {
    grid-template-columns: 1fr;
  }

  .cte-settings-card .cte-card-title {
    padding-right: 0;
  }

  .cte-advanced-toggle {
    position: static;
    margin-bottom: 10px;
  }

  .cte-summary-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .cte-bar-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .cte-bar-value {
    text-align: left;
  }
}



.spg-upload-wrap { position: relative; display: inline-flex; }
.spg-help-icon { position: absolute; top: -6px; right: -6px; display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; z-index: 1; }
.gsc-sort-th { cursor: pointer; user-select: none; }
.gsc-sort-th:hover { color: var(--brand-primary-3); }
.gsc-sort-arrow { font-size: 10px; margin-left: 2px; }
</style>

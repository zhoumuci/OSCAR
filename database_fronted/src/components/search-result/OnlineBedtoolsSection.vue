<template>
  <section class="online-bedtools-card float-card">
    <div class="bedtools-head">
      <div class="head-copy">
        <div class="section-title">Online BEDTools</div>
        <div class="section-sub">
          Intersect a genomic region with sample-specific tracks and hg38 reference annotations.
        </div>
      </div>
      <div class="context-pills" aria-label="Current data context">
        <span class="context-chip">{{ domainLabel }}</span>
        <span class="context-chip">{{ datasetLabel }}</span>
      </div>
    </div>

    <div v-if="sourcesError" class="inline-alert">
      {{ sourcesError }}
    </div>

    <div class="source-panel">
      <div class="panel-title">Annotation sources</div>
      <div v-if="sourcesLoading" class="source-loading">Loading sources...</div>
      <div v-else class="source-grid">
        <label
          v-for="source in orderedSources"
          :key="source.type"
          class="source-option"
          :class="{ selected: isSourceSelected(source.type), disabled: !source.available }"
          :title="source.reason || source.description || source.label"
        >
          <input
            type="checkbox"
            :disabled="!source.available"
            :checked="isSourceSelected(source.type)"
            @change="toggleSource(source.type)"
          />
          <span class="source-check" aria-hidden="true"></span>
          <span class="source-main">
            <span class="source-label-row">
              <span class="source-label">{{ source.label }}</span>
              <span class="source-scope">{{ source.scope }}</span>
            </span>
            <span v-if="source.reason" class="source-reason">{{ source.reason }}</span>
            <span v-else-if="source.description" class="source-reason">{{ source.description }}</span>
          </span>
          <span class="source-status" :class="statusClass(source.status)">
            {{ source.status || "UNKNOWN" }}
          </span>
        </label>
      </div>
    </div>

    <form class="query-form" @submit.prevent="runFirstPage">
      <div class="region-field">
        <label class="field-label" for="bedtools-region">
          <span>Region</span>
          <span class="info-tip" tabindex="0" aria-label="Coordinate system information">
            ?
            <span class="info-tooltip">
              Coordinates use BED 0-based half-open intervals. Example: chr19:35330428-35330929.
            </span>
          </span>
        </label>
        <input
          id="bedtools-region"
          v-model.trim="region"
          class="region-input"
          type="text"
          placeholder="chr19:35330428-35330929"
          :aria-invalid="showRegionError"
          @blur="regionTouched = true"
        />
        <div v-if="showRegionError" class="field-error">{{ regionValidationMessage }}</div>
      </div>

      <label class="number-field">
        <span>Min overlap bp</span>
        <input v-model.number="minOverlapBp" type="number" min="1" step="1" />
      </label>
      <label class="number-field">
        <span>Page size</span>
        <select v-model.number="pageSize">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </label>
      <button class="run-button" type="submit" :disabled="runDisabled">
        {{ intersectLoading ? "Running..." : "Run intersect" }}
      </button>
    </form>

    <div v-if="intersectError" class="inline-alert">
      {{ intersectError }}
    </div>

    <div v-if="hasResult" class="result-panel">
      <div class="result-summary">
        <div>
          <div class="summary-label">Region</div>
          <div class="summary-value">{{ resultRegionLabel }}</div>
        </div>
        <div>
          <div class="summary-label">Selected sources</div>
          <div class="summary-value">{{ selectedSourceLabels }}</div>
        </div>
        <div>
          <div class="summary-label">Total hits</div>
          <div class="summary-value">{{ totalHits }}</div>
        </div>
      </div>

      <div v-if="records.length === 0" class="empty-state">
        No overlaps found for this region.
      </div>
      <div v-else class="result-table-wrap">
        <table class="result-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Location</th>
              <th>Feature</th>
              <th>Annotation</th>
              <th>Overlap</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(record, index) in records" :key="recordKey(record, index)">
              <td>
                <span class="type-pill">{{ recordTypeLabel(record) }}</span>
              </td>
              <td>{{ recordLocation(record) }}</td>
              <td>{{ recordFeature(record) }}</td>
              <td>{{ recordAnnotation(record) }}</td>
              <td>{{ recordOverlap(record) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-row">
        <button class="page-button" type="button" :disabled="!canGoPrevious || intersectLoading" @click="goPage(page - 1)">
          Previous
        </button>
        <span class="page-state">Page {{ page }}</span>
        <button class="page-button" type="button" :disabled="!canGoNext || intersectLoading" @click="goPage(page + 1)">
          Next
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import axios from "axios";
import { computed, onMounted, ref, watch } from "vue";
import type {
  BedtoolsIntersectResponse,
  BedtoolsOverlapRecord,
  BedtoolsSourceOption,
  SearchResultDomain,
} from "@/api/searchResult";
import { fetchBedtoolsSources, runBedtoolsIntersect } from "@/api/searchResult";

const props = withDefaults(defineProps<{
  datasetId: string;
  domain: SearchResultDomain;
  genomeBuild?: string;
}>(), {
  genomeBuild: "hg38",
});

const sourceOrder = ["marker_peak", "p2g_link", "gene", "transcript", "tss_promoter", "tf_annotation"];
const regionPattern = /^chr[^:\s]+:\d+-\d+$/i;

const sources = ref<BedtoolsSourceOption[]>([]);
const selectedTypes = ref<string[]>([]);
const sourcesLoading = ref(false);
const sourcesError = ref("");
const region = ref("");
const regionTouched = ref(false);
const hasSubmitted = ref(false);
const minOverlapBp = ref(1);
const page = ref(1);
const pageSize = ref(10);
const intersectLoading = ref(false);
const intersectError = ref("");
const response = ref<BedtoolsIntersectResponse | null>(null);
const submittedRegion = ref("");
const submittedTypes = ref<string[]>([]);

const genomeBuild = computed(() => props.genomeBuild || "hg38");
const domainLabel = computed(() => props.domain.toUpperCase());
const datasetLabel = computed(() => props.datasetId || "H_000001");

const orderedSources = computed(() => {
  return [...sources.value].sort((a, b) => {
    const left = sourceOrder.indexOf(String(a.type));
    const right = sourceOrder.indexOf(String(b.type));
    return (left === -1 ? 99 : left) - (right === -1 ? 99 : right);
  });
});

const availableTypes = computed(() => {
  return orderedSources.value
    .filter((source) => source.available)
    .map((source) => String(source.type));
});

const regionValidationMessage = computed(() => {
  const value = region.value.trim();
  if (!value) return "Enter a genomic region.";
  if (!regionPattern.test(value)) return "Use a region like chr19:35330428-35330929.";
  const parsed = parseRegion(value);
  if (!parsed) return "Use numeric BED coordinates.";
  if (parsed.end <= parsed.start) return "End must be greater than start.";
  return "";
});

const showRegionError = computed(() => {
  if (!regionValidationMessage.value) return false;
  if (hasSubmitted.value) return true;
  return regionTouched.value && region.value.trim().length > 0;
});

const selectedAvailableTypes = computed(() => {
  const available = new Set(availableTypes.value);
  return selectedTypes.value.filter((type) => available.has(type));
});

const runDisabled = computed(() => {
  return intersectLoading.value
    || sourcesLoading.value
    || selectedAvailableTypes.value.length === 0
    || !Number.isFinite(Number(minOverlapBp.value))
    || Number(minOverlapBp.value) < 1;
});

const records = computed(() => response.value?.records ?? []);
const hasResult = computed(() => response.value !== null);
const totalHits = computed(() => {
  if (typeof response.value?.total === "number") return response.value.total;
  if (typeof response.value?.summary?.totalHits === "number") return response.value.summary.totalHits;
  return records.value.length;
});
const canGoPrevious = computed(() => page.value > 1);
const canGoNext = computed(() => page.value * pageSize.value < totalHits.value);
const resultRegionLabel = computed(() => response.value?.queryRegion?.raw || submittedRegion.value || region.value);
const selectedSourceLabels = computed(() => {
  const labels = new Map(orderedSources.value.map((source) => [String(source.type), source.label]));
  return (submittedTypes.value.length ? submittedTypes.value : selectedAvailableTypes.value)
    .map((type) => labels.get(type) || type)
    .join(", ");
});

let sourcesToken = 0;
let intersectToken = 0;

async function loadSources() {
  const token = ++sourcesToken;
  sourcesLoading.value = true;
  sourcesError.value = "";
  try {
    const data = await fetchBedtoolsSources({
      datasetId: props.datasetId,
      domain: props.domain,
      genomeBuild: genomeBuild.value,
    });
    if (token !== sourcesToken) return;
    sources.value = data.sources ?? [];
    selectedTypes.value = sources.value
      .filter((source) => source.available)
      .map((source) => String(source.type));
  } catch (error) {
    if (token !== sourcesToken) return;
    sources.value = [];
    selectedTypes.value = [];
    sourcesError.value = "Unable to load annotation sources. Please retry after the backend is available.";
    console.error("[SearchResult] Failed to load BEDTools sources:", error);
  } finally {
    if (token === sourcesToken) {
      sourcesLoading.value = false;
    }
  }
}

function toggleSource(type: string) {
  if (!availableTypes.value.includes(type)) return;
  if (selectedTypes.value.includes(type)) {
    selectedTypes.value = selectedTypes.value.filter((value) => value !== type);
    return;
  }
  selectedTypes.value = [...selectedTypes.value, type];
}

function isSourceSelected(type: string) {
  return selectedTypes.value.includes(String(type));
}

function statusClass(status: string) {
  const normalized = String(status || "").toLowerCase();
  return {
    ready: normalized === "ready",
    unavailable: normalized === "not_available" || normalized === "reference_not_ready" || normalized === "tracks_not_ready",
  };
}

function parseRegion(value: string): { start: number; end: number } | null {
  const match = value.match(/^chr[^:\s]+:(\d+)-(\d+)$/i);
  if (!match) return null;
  const start = Number(match[1]);
  const end = Number(match[2]);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return null;
  return { start, end };
}

function runFirstPage() {
  hasSubmitted.value = true;
  if (regionValidationMessage.value) return;
  page.value = 1;
  runQuery();
}

async function goPage(nextPage: number) {
  if (nextPage < 1 || intersectLoading.value) return;
  page.value = nextPage;
  await runQuery();
}

async function runQuery() {
  if (regionValidationMessage.value) return;
  if (runDisabled.value) return;
  const token = ++intersectToken;
  intersectLoading.value = true;
  intersectError.value = "";
  const selected = [...selectedAvailableTypes.value];
  try {
    const data = await runBedtoolsIntersect({
      datasetId: props.datasetId,
      domain: props.domain,
      genomeBuild: genomeBuild.value,
      region: region.value.trim(),
      annotationTypes: selected,
      minOverlapBp: Math.max(1, Number(minOverlapBp.value) || 1),
      page: page.value,
      pageSize: pageSize.value,
    });
    if (token !== intersectToken) return;
    response.value = data;
    submittedRegion.value = region.value.trim();
    submittedTypes.value = selected;
  } catch (error) {
    if (token !== intersectToken) return;
    intersectError.value = errorMessage(error);
    response.value = null;
    console.error("[SearchResult] BEDTools intersect failed:", error);
  } finally {
    if (token === intersectToken) {
      intersectLoading.value = false;
    }
  }
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const backendMessage = backendErrorMessage(error.response?.data);
    if (status === 400) return backendMessage || "The region or request parameters may not be formatted correctly.";
    if (status === 409) return backendMessage || "Some selected sources are currently unavailable. Refresh sources and try again.";
    return backendMessage || "BEDTools intersect failed. Please retry.";
  }
  return "BEDTools intersect failed. Please retry.";
}

function backendErrorMessage(data: unknown): string {
  if (!data || typeof data !== "object") return "";
  const payload = data as { message?: unknown; error?: unknown; status?: unknown };
  if (typeof payload.message === "string" && payload.message.trim()) {
    return payload.message;
  }
  if (typeof payload.error === "string" && payload.error.trim()) {
    return payload.error;
  }
  if (typeof payload.status === "string" && payload.status.trim()) {
    return payload.status;
  }
  return "";
}

function recordKey(record: BedtoolsOverlapRecord, index: number) {
  return `${record.annotationType || "hit"}-${record.featureId || record.featureRegion || index}-${index}`;
}

function recordTypeLabel(record: BedtoolsOverlapRecord) {
  return stringValue(record.annotationLabel) || labelForType(stringValue(record.annotationType)) || "-";
}

function recordLocation(record: BedtoolsOverlapRecord) {
  const direct = firstString(record, ["featureRegion", "location", "region"]);
  if (direct) return direct;
  const chrom = firstString(record, ["chrom", "chromosome", "featureChrom"]);
  const start = firstNumber(record, ["start", "featureStart"]);
  const end = firstNumber(record, ["end", "featureEnd"]);
  return chrom && start !== null && end !== null ? `${chrom}:${start}-${end}` : "-";
}

function recordFeature(record: BedtoolsOverlapRecord) {
  return firstString(record, [
    "featureName",
    "name",
    "geneName",
    "gene",
    "gene_id",
    "geneId",
    "transcriptId",
    "transcript_id",
    "id",
    "featureId",
  ]) || "-";
}

function recordAnnotation(record: BedtoolsOverlapRecord) {
  const parts = [
    firstString(record, ["evidence"]),
    firstString(record, ["cellCluster"]),
    joinPair(firstString(record, ["cellType"]), firstString(record, ["cluster"])),
    firstString(record, ["strand"]) ? `strand=${firstString(record, ["strand"])}` : "",
    valueWithLabel("score", record.score),
    firstString(record, ["sample"]),
  ].filter(Boolean);

  return parts.slice(0, 4).join(" · ") || "-";
}

function recordOverlap(record: BedtoolsOverlapRecord) {
  const value = firstNumber(record, ["overlapBp", "overlap", "overlap_bp"]);
  return value === null ? "-" : `${value} bp`;
}

function firstString(record: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    const normalized = stringValue(value);
    if (normalized) return normalized;
  }
  return "";
}

function stringValue(value: unknown) {
  if (typeof value === "string") return value.trim();
  if (typeof value === "number" && Number.isFinite(value)) return String(value);
  return "";
}

function firstNumber(record: Record<string, unknown>, keys: string[]) {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "number" && Number.isFinite(value)) return value;
    if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value);
  }
  return null;
}

function joinPair(left: string, right: string) {
  if (left && right) return `${left} / ${right}`;
  return left || right || "";
}

function valueWithLabel(label: string, value: unknown) {
  const normalized = stringValue(value);
  return normalized ? `${label}=${normalized}` : "";
}

function labelForType(type: string) {
  const labels: Record<string, string> = {
    marker_peak: "Marker peak",
    p2g_link: "P2G link",
    gene: "Gene",
    transcript: "Transcript",
    tss_promoter: "TSS/promoter",
    tf_annotation: "TF annotation",
  };
  return labels[type] || type;
}

onMounted(loadSources);
watch(
  () => [props.datasetId, props.domain, genomeBuild.value],
  () => {
    response.value = null;
    intersectError.value = "";
    regionTouched.value = false;
    hasSubmitted.value = false;
    page.value = 1;
    loadSources();
  }
);
</script>

<style scoped>
.online-bedtools-card {
  --bedtools-teal: var(--brand-primary-3);
  --bedtools-teal-dark: #5f7d70;
  --bedtools-soft: #eef5f2;
  --bedtools-ready: #dff2e8;
  --bedtools-ready-text: #2c6b4f;
  --bedtools-disabled: #eef1f0;
  --bedtools-error-bg: #fff0f0;
  --bedtools-error-border: #efb8b8;
  padding: 18px;
  margin-bottom: 14px;
  background: var(--surface);
}

.bedtools-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.head-copy {
  min-width: 0;
}

.section-title {
  font-size: 18px;
  font-weight: 900;
}

.section-sub {
  max-width: 760px;
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.45;
}

.context-pills,
.source-label-row,
.pagination-row {
  display: flex;
  align-items: center;
}

.context-pills {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.context-chip,
.source-status,
.source-scope,
.type-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 5px 10px;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: var(--bedtools-soft);
  color: var(--text);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.context-chip {
  min-height: 30px;
  padding: 6px 12px;
  background: #f4faf7;
  border-color: #9fc6b5;
  color: var(--bedtools-teal-dark);
  letter-spacing: 0;
}

.source-panel,
.result-panel {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fbfdfc;
}

.panel-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 900;
  color: var(--text);
}

.source-loading {
  color: var(--muted);
  font-size: 13px;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.source-option {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
  min-height: 86px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    background-color 0.16s ease;
}

.source-option input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.source-option:hover {
  border-color: var(--border-brand);
  box-shadow: 0 8px 16px #12182612;
}

.source-option.selected {
  border-color: #86b69f;
  background: #f5fbf8;
}

.source-option.disabled {
  cursor: not-allowed;
  background: var(--bedtools-disabled);
  color: var(--muted);
}

.source-check {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  border: 2px solid var(--border-strong);
  border-radius: 5px;
  background: var(--surface);
}

.source-option.selected .source-check {
  border-color: var(--bedtools-teal-dark);
  background:
    linear-gradient(135deg, transparent 48%, #ffffff 50% 58%, transparent 60%),
    var(--bedtools-teal-dark);
}

.source-main {
  min-width: 0;
}

.source-label-row {
  flex-wrap: wrap;
  gap: 7px;
}

.source-label {
  font-weight: 900;
}

.source-scope {
  min-height: 22px;
  padding: 4px 8px;
  background: var(--surface-2);
  border-color: var(--border);
  color: var(--muted);
  font-size: 11px;
}

.source-reason {
  display: -webkit-box;
  margin-top: 7px;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.source-status {
  min-height: 24px;
  padding: 4px 8px;
  border-color: var(--border);
  background: var(--surface-2);
  color: var(--muted);
  font-size: 11px;
}

.source-status.ready {
  border-color: #9bd6bd;
  background: var(--bedtools-ready);
  color: var(--bedtools-ready-text);
}

.source-status.unavailable {
  background: #ecefed;
  color: #6b7470;
}

.query-form {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) 160px 140px 160px;
  gap: 12px;
  align-items: end;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fbfdfc;
}

.field-label,
.number-field span,
.summary-label {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.field-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.info-tip {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: var(--surface);
  color: var(--bedtools-teal-dark);
  cursor: help;
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
}

.info-tooltip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  z-index: 5;
  width: 292px;
  max-width: min(292px, 78vw);
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 22px #1218261f;
  color: var(--text);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  opacity: 0;
  pointer-events: none;
  transform: translate(-50%, 4px);
  transition:
    opacity 0.14s ease,
    transform 0.14s ease;
}

.info-tip:hover .info-tooltip,
.info-tip:focus-visible .info-tooltip {
  opacity: 1;
  transform: translate(-50%, 0);
}

.region-input,
.number-field input,
.number-field select {
  width: 100%;
  min-height: 52px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  font: inherit;
  font-size: 14px;
  outline: none;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.region-input {
  padding: 0 12px;
}

.region-input:focus,
.number-field input:focus,
.number-field select:focus {
  border-color: var(--bedtools-teal);
  box-shadow: 0 0 0 3px #8fa59c30;
}

.field-error {
  margin-top: 6px;
  font-size: 12px;
}

.field-error {
  color: #b44a4a;
  font-weight: 700;
}

.number-field {
  display: grid;
  gap: 6px;
}

.number-field input,
.number-field select {
  padding: 0 10px;
}

.run-button,
.page-button {
  min-height: 52px;
  border: 1px solid var(--bedtools-teal-dark);
  border-radius: 8px;
  background: var(--bedtools-teal-dark);
  color: #ffffff;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 900;
  white-space: nowrap;
  transition:
    background-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.run-button {
  padding: 0 18px;
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.22);
}

.run-button:not(:disabled):hover,
.page-button:not(:disabled):hover {
  background: #4f6f61;
  transform: translateY(-1px);
}

.run-button:disabled,
.page-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.inline-alert {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid var(--bedtools-error-border);
  border-radius: 8px;
  background: var(--bedtools-error-bg);
  color: #9f3f3f;
  font-size: 13px;
  font-weight: 700;
}

.result-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr) auto;
  gap: 12px;
  align-items: start;
  padding-bottom: 12px;
}

.summary-value {
  margin-top: 3px;
  font-size: 13px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.result-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
}

.result-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
  font-size: 13px;
}

.result-table th,
.result-table td {
  padding: 11px 12px;
  border-bottom: 1px solid var(--border);
  text-align: left;
  vertical-align: top;
}

.result-table th {
  background: var(--surface-2);
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.result-table tbody tr:last-child td {
  border-bottom: 0;
}

.type-pill {
  min-height: 24px;
  padding: 4px 8px;
  background: #eef5f2;
  color: var(--bedtools-teal-dark);
}

.empty-state {
  display: flex;
  min-height: 86px;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--muted);
  font-weight: 800;
}

.pagination-row {
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

.page-button {
  min-height: 34px;
  padding: 0 12px;
  background: var(--surface);
  color: var(--bedtools-teal-dark);
  box-shadow: none;
}

.page-button:not(:disabled):hover {
  color: #ffffff;
}

.page-state {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 980px) {
  .query-form {
    grid-template-columns: minmax(280px, 1fr) 150px 130px;
  }

  .bedtools-head {
    flex-direction: column;
  }

  .context-pills {
    justify-content: flex-start;
  }

  .run-button {
    grid-column: 1 / -1;
  }

  .source-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .source-grid,
  .query-form,
  .result-summary {
    grid-template-columns: 1fr;
  }
}
</style>

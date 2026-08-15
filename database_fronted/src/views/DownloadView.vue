<template>
  <div class="dl-page">
    <div class="container">
      <div class="page-title">Download</div>
      <div class="page-sub">Use our database to download the data you need.</div>

      <div class="float-card dl-card">
        <!-- Stat line -->
        <div class="dl-stat-bar">
          <div class="dl-stat-left">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 1L2 4.5v4.5c0 3.73 2.88 7.22 7 7.88 4.12-.66 7-4.15 7-7.88V4.5L9 1z" stroke="var(--brand-primary-3)" stroke-width="1.5" stroke-linejoin="round"/><path d="M6 9l2 2 4-4" stroke="var(--brand-primary-3)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <span>{{ allRows.length }} samples available for download</span>
          </div>
          <a
            href="/OSCAR/static/combined.tar.gz"
            download
            class="ref-dl-btn"
            :class="{ 'is-downloading': referenceDownloadStarting }"
            @click="acknowledgeReferenceDownload"
          >
            <span v-if="referenceDownloadStarting" class="download-spinner"></span>
            {{ referenceDownloadStarting ? "STARTING…" : "Epi(genetic) Annotation" }}
          </a>
        </div>

        <!-- 搜索区（统一风格） -->
        <div class="search-row">
          <div class="search-label">Search:</div>

          <el-select v-model="field" placeholder="Select field" class="sel" clearable>
            <el-option v-for="f in fields" :key="f.value" :label="f.label" :value="f.value" />
          </el-select>

          <el-input
            v-model="keyword"
            placeholder="Please enter the search content"
            clearable
            @keyup.enter="onSearch"
          />

          <el-button type="primary" :loading="state==='loading'" @click="onSearch">Search</el-button>

          <el-button class="cart-btn" :disabled="selected.length===0" @click="openCart">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 2h1.5l1.2 6.5a1 1 0 00.98.8h5.6a1 1 0 00.98-.8L13 4.7H4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="6" cy="13" r="1" fill="currentColor"/><circle cx="11" cy="13" r="1" fill="currentColor"/></svg>
              Download cart{{ selected.length > 0 ? ` (${selected.length}/10)` : '' }}
            </el-button>
        </div>

        <!-- 表格区：状态机 -->
        <div class="table-zone" :style="{ height: tableHeight }">
          <!-- LOADING -->
          <div v-if="state === 'loading'" class="table-skeleton">
            <el-skeleton animated :rows="10" />
          </div>

          <!-- ERROR -->
          <div v-else-if="state === 'error'" class="state-box">
            <div class="state-title">{{ errorTitle }}</div>
            <div class="state-msg">{{ errorMsg }}</div>
            <div class="state-actions">
              <el-button type="primary" @click="fetchRows">Retry</el-button>
            </div>
          </div>

          <!-- READY -->
          <template v-else>
            <el-table
              v-if="sortedRows.length > 0"
              :data="pageRows"
              stripe
              border
              class="tbl"
              @selection-change="onSelectionChange"
              @sort-change="onTableSortChange"
            >
              <el-table-column type="selection" width="44" fixed :selectable="checkSelectable" />
              <el-table-column prop="datasetId" label="DatasetID" min-width="140" fixed sortable="custom">
                <template #default="{ row }">
                  <el-link type="primary" underline="never" class="dataset-link" @click="openDataset(row.datasetId)">
                    {{ row.datasetId }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="sampleType" label="Sample Type" min-width="120" />
              <el-table-column prop="tissue" label="Tissue" min-width="100" />
              <el-table-column prop="sampleName" label="Sample Name" min-width="180" />
              <el-table-column prop="cells" label="Cells" min-width="100" align="center" sortable="custom">
                <template #default="{ row }">
                  {{ (row.cells ?? 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="platform" label="Platform" min-width="120" />
              <el-table-column prop="sourceId" label="Source ID" min-width="120" />
              <el-table-column prop="disease" label="Disease" min-width="100" />
              <el-table-column prop="sampleSource" label="Sample Source" min-width="140" />

              <el-table-column label="Download" min-width="140" align="center">
                <template #default="{ row }">
                  <el-button size="small" class="dl-file-btn" @click="openDownloads(row)">Files</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- EMPTY -->
            <div v-else class="table-empty">
              <el-empty description="No results. Try adjusting the search conditions." />
            </div>
          </template>
        </div>

        <!-- pager：只有 ready 且有数据时显示更合理 -->
        <div class="pager" v-if="state==='ready' && sortedRows.length > 0">
          <el-pagination
            class="oscar-pagination"
            background
            layout="total, sizes, prev, pager, next, jumper"
            popper-class="oscar-select-popper"
            :total="sortedRows.length"
            :page-sizes="[10, 20, 50]"
            :page-size="pageSize"
            :current-page="page"
            @size-change="onPageSizeChange"
            @current-change="onPageChange"
          />
        </div>
      </div>

      <!-- Cart dialog -->
      <el-dialog
        v-model="cartOpen"
        width="900px"
        title="Download Cart"
        custom-class="bubble-dialog"
        modal-class="bubble-overlay"
        :append-to-body="true"
        class="float-card"
      >
        <div class="dlg-body">
          <div class="dlg-meta" style="margin-bottom:16px">
            <b>{{ selected.length }}</b> sample(s) selected
          </div>
          <div v-if="cartDownloadTotal > 0" class="cart-download-progress" aria-live="polite">
            <div class="cart-download-progress-head">
              <span>
                {{ cartDownloadActive ? `Downloading ${cartDownloadCurrent} of ${cartDownloadTotal}` : `Processed ${cartDownloadProcessed} of ${cartDownloadTotal}` }}
              </span>
              <span v-if="cartDownloadDataset" class="mono">{{ cartDownloadDataset }}</span>
            </div>
            <el-progress :percentage="cartDownloadPercent" :stroke-width="10" :status="cartDownloadActive ? undefined : cartDownloadErrors ? 'exception' : 'success'" />
            <div class="cart-download-progress-note">
              <span v-if="cartDownloadActive">Please keep this dialog open while files are prepared sequentially.</span>
              <span v-else-if="cartDownloadErrors">Completed with {{ cartDownloadErrors }} failed download(s).</span>
              <span v-else>All selected downloads have been prepared.</span>
            </div>
          </div>
          <div v-for="dom in cartTree" :key="dom.domain" class="dl-tree-domain" :style="{ borderColor: dom.color }">
            <div class="dl-tree-root" :style="{ background: dom.color+'18', color: dom.color, borderColor: dom.color }">
              <span class="dl-tree-root-dot" :style="{ background: dom.color }"></span>
              {{ dom.label }}
            </div>
            <div class="dl-tree-branches">
              <div v-for="ch in dom.children" :key="ch.type" class="dl-tree-branch">
                <div class="dl-tree-branch-label">{{ ch.label }}</div>
                <div class="chip-grid">
                  <button
                    v-for="f in ch.files"
                    :key="f.id"
                    class="chip"
                    :class="{ 'is-downloading': cartDownloadActive && cartDownloadKey === cartFileKey(dom.domain, ch.type, f.format) }"
                    :disabled="cartDownloadActive"
                    @click="triggerCartBatchDownload(dom.domain, ch, f)"
                    type="button"
                  >
                    <span class="chip-left">
                      <span class="chip-name">{{ f.title }}</span>
                      <span class="chip-format">{{ f.format.toUpperCase() }}</span>
                    </span>
                    <span class="chip-action">
                      <span v-if="cartDownloadActive && cartDownloadKey === cartFileKey(dom.domain, ch.type, f.format)" class="download-spinner"></span>
                      {{ cartDownloadActive && cartDownloadKey === cartFileKey(dom.domain, ch.type, f.format) ? `${cartDownloadCurrent}/${cartDownloadTotal}` : `Download ${selected.length}` }}
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <template #footer>
          <el-button :disabled="cartDownloadActive" @click="cartOpen = false">Close</el-button>
        </template>
      </el-dialog>

      <!-- Single-file dialog -->
      <el-dialog
        v-model="dlgOpen"
        width="900px"
        :title="dlgTitle"
        custom-class="bubble-dialog"
        modal-class="bubble-overlay"
        :append-to-body="true"
        class="float-card"
      >
        <div v-if="dlgRow" class="dlg-body">
          <div class="dlg-meta" style="margin-bottom:16px">
            <span class="mono" style="font-weight:800">{{ dlgRow.datasetId }}</span> · {{ dlgRow.tissue }} · {{ dlgRow.disease }}
          </div>
          <template v-for="dom in dlgRow.downloads" :key="dom.domain">
            <div class="dl-tree-domain" :style="{ borderColor: dom.color }">
              <div class="dl-tree-root" :style="{ background: dom.color+'18', color: dom.color, borderColor: dom.color }">
                <span class="dl-tree-root-dot" :style="{ background: dom.color }"></span>
                {{ dom.label }}
              </div>
              <div class="dl-tree-branches">
                <div v-for="ch in dom.children" :key="ch.type" class="dl-tree-branch">
                  <div class="dl-tree-branch-label">{{ ch.label }}</div>
                  <div class="chip-grid">
                    <button
                      v-for="f in ch.files"
                      :key="f.id"
                      class="chip"
                      :class="{ 'is-downloading': singleDownloadId === f.id }"
                      :disabled="singleDownloadId !== ''"
                      @click="triggerDownload(f)"
                      type="button"
                    >
                      <span class="chip-left"><span class="chip-name">{{ f.title }}</span><span class="chip-format">{{ f.format.toUpperCase() }}</span></span>
                      <span class="chip-action">
                        <span v-if="singleDownloadId === f.id" class="download-spinner"></span>
                        {{ singleDownloadId === f.id ? "Preparing…" : "Download" }}
                      </span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
        <template #footer><el-button @click="dlgOpen = false">Close</el-button></template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { fetchDownloadRows, buildDownloads } from "@/api/download";
import type { DownloadFile, DownloadRow, DownloadTypeNode, TabKey } from "@/api/download";

const router = useRouter();
const selected = ref<DownloadRow[]>([]);
function onSelectionChange(rows: DownloadRow[]) { selected.value = rows; }
function checkSelectable(row: DownloadRow) {
  if (selected.value.length < 10) return true;
  return selected.value.some(s => s.datasetId === row.datasetId);
}
function openCart() {
  if (!selected.value.length) return;
  dlgRow.value = null;
  cartOpen.value = true;
}
function openDataset(id: string) {
  if (!id) return;
  router.push({ name: "SampleDetail", params: { id }, query: { domain: "integration", source: "download" } });
}

type ViewState = "loading" | "ready" | "error";

const state = ref<ViewState>("loading");
const errorCode = ref<number | null>(null);
const errorMsg = ref<string>("");

const errorTitle = computed(() => {
  if (errorCode.value === 429) return "Too Many Requests";
  if (errorCode.value === 403) return "Access Restricted";
  return "Request Failed";
});

const tableHeight = computed(() => "calc(100vh - 280px)");

const DOWNLOAD_TAB: TabKey = "integration";
const field = ref<string>("");
const keyword = ref<string>("");

const page = ref(1);
const sortBy = ref<string>("datasetId");
const sortDir = ref<"asc" | "desc">("asc");

function onPageSizeChange(s: number) { pageSize.value = s; page.value = 1; }
function onPageChange(p: number) { page.value = p; }
function onTableSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (order) { sortBy.value = prop; sortDir.value = order === "descending" ? "desc" : "asc"; }
  else { sortBy.value = "datasetId"; sortDir.value = "asc"; }
  page.value = 1;
}
const pageSize = ref(10);

const allRows = ref<DownloadRow[]>([]);

const fields = computed(() => {
  return [
    { label: "Tissue type (e.g. Brain)", value: "tissue" },
    { label: "Biosample type (e.g. Cell line)", value: "sampleType" },
    { label: "Sample ID (e.g. H_000001)", value: "datasetId" },
  ];
});

function normalize(v: unknown) {
  return String(v ?? "").toLowerCase().trim();
}

const cartTree = computed(() => {
  const samples = selected.value;
  if (!samples.length) return [];
  const firstSample = samples[0];
  if (!firstSample) return [];
  return firstSample.downloads.map(d => ({
    ...d,
    children: d.children.map(t => ({ ...t, files: t.files.map(f => ({ ...f })) })),
  }));
});

const filteredRows = computed(() => {
  const kw = normalize(keyword.value);
  const kf = field.value;

  if (!kw) return allRows.value;

  return allRows.value.filter((r) => {
    if (kf) return normalize((r as any)[kf]).includes(kw);
    const hay = [
      r.datasetId, r.sampleType, r.sampleName,
      r.tissue, r.disease, r.platform ?? "", r.sourceId ?? "", r.sampleSource ?? ""
    ].map(normalize);
    return hay.some(x => x.includes(kw));
  });
});

const sortedRows = computed(() => {
  const rows = [...filteredRows.value];
  const dir = sortDir.value === "desc" ? -1 : 1;
  if (sortBy.value === "cells") {
    rows.sort((a, b) => dir * ((a.cells ?? 0) - (b.cells ?? 0)));
  } else {
    rows.sort((a, b) => dir * String(a.datasetId ?? "").localeCompare(String(b.datasetId ?? "")));
  }
  return rows;
});

const pageRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return sortedRows.value.slice(start, start + pageSize.value);
});

async function fetchRows() {
  state.value = "loading";
  errorMsg.value = "";
  errorCode.value = null;

  try {
    allRows.value = await fetchDownloadRows(DOWNLOAD_TAB);
    state.value = "ready";
  } catch (e: any) {
    const status = e?.response?.status ?? null; // 未来接 axios 用得到
    errorCode.value = status;

    if (status === 429) errorMsg.value = "You are sending requests too frequently. Please try again later.";
    else if (status === 403) errorMsg.value = "Your access is temporarily restricted. Please try again later.";
    else errorMsg.value = e?.message || "Request failed. Please retry.";

    state.value = "error";
  }
}

function onSearch() {
  page.value = 1;
}

const referenceDownloadStarting = ref(false);
const singleDownloadId = ref("");
const cartDownloadActive = ref(false);
const cartDownloadCurrent = ref(0);
const cartDownloadProcessed = ref(0);
const cartDownloadTotal = ref(0);
const cartDownloadDataset = ref("");
const cartDownloadErrors = ref(0);
const cartDownloadKey = ref("");
const cartDownloadPercent = computed(() => cartDownloadTotal.value
  ? Math.round((cartDownloadProcessed.value / cartDownloadTotal.value) * 100)
  : 0);

function cartFileKey(domain: string, type: string, format: string) {
  return `${domain}:${type}:${format}`;
}

function acknowledgeReferenceDownload() {
  referenceDownloadStarting.value = true;
  window.setTimeout(() => { referenceDownloadStarting.value = false; }, 1600);
}

function filenameFromResponse(response: Response, fallback: string) {
  const disposition = response.headers.get("content-disposition") ?? "";
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) {
    try { return decodeURIComponent(encoded); } catch { return encoded; }
  }
  return disposition.match(/filename="?([^";]+)"?/i)?.[1] || fallback;
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.style.display = "none";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

async function fetchAndSaveDownload(file: DownloadFile) {
  const response = await fetch(file.url, { credentials: "same-origin" });
  if (!response.ok) throw new Error(`Download failed with HTTP ${response.status}`);
  const blob = await response.blob();
  saveBlob(blob, filenameFromResponse(response, `${file.id}.${file.format}`));
}

async function triggerCartBatchDownload(domain: string, ch: Pick<DownloadTypeNode, "type">, requestedFile: Pick<DownloadFile, "format">) {
  if (cartDownloadActive.value) return;
  const samples = [...selected.value];
  cartDownloadActive.value = true;
  cartDownloadCurrent.value = 0;
  cartDownloadProcessed.value = 0;
  cartDownloadTotal.value = samples.length;
  cartDownloadDataset.value = "";
  cartDownloadErrors.value = 0;
  cartDownloadKey.value = cartFileKey(domain, ch.type, requestedFile.format);

  for (let i = 0; i < samples.length; i++) {
    const sample = samples[i];
    if (!sample) continue;
    cartDownloadCurrent.value = i + 1;
    cartDownloadDataset.value = sample.datasetId;
    const exactDomain = buildDownloads(sample.datasetId).find(item => item.domain === domain);
    const exactType = exactDomain?.children.find(item => item.type === ch.type);
    const file = exactType?.files.find(item => item.format === requestedFile.format);
    try {
      if (!file) throw new Error(`No ${domain}/${ch.type}/${requestedFile.format} download is available`);
      await fetchAndSaveDownload(file);
    } catch (error) {
      cartDownloadErrors.value += 1;
      console.error(`[Download cart] ${sample.datasetId}`, error);
    } finally {
      cartDownloadProcessed.value += 1;
    }
  }

  cartDownloadActive.value = false;
  cartDownloadDataset.value = "";
  cartDownloadKey.value = "";
  if (cartDownloadErrors.value) {
    ElMessage.warning(`${cartDownloadProcessed.value - cartDownloadErrors.value} download(s) prepared; ${cartDownloadErrors.value} failed.`);
  } else {
    ElMessage.success(`${cartDownloadProcessed.value} download(s) prepared.`);
  }
}

async function triggerDownload(file: DownloadFile) {
  if (singleDownloadId.value) return;
  singleDownloadId.value = file.id;
  try {
    await fetchAndSaveDownload(file);
    ElMessage.success("Download prepared.");
  } catch (error: any) {
    ElMessage.error(error?.message || "Download failed. Please retry.");
  } finally {
    singleDownloadId.value = "";
  }
}

onMounted(fetchRows);

// ---------------- 弹窗 ----------------
const dlgOpen = ref(false);
const cartOpen = ref(false);
const dlgRow = ref<DownloadRow | null>(null);

const dlgTitle = computed(() => {
  if (!dlgRow.value) return "Downloads";
  return `Downloads • ${dlgRow.value.datasetId}`;
});

function openDownloads(row: DownloadRow) {
  dlgRow.value = row;
  dlgOpen.value = true;
}
</script>

<style scoped>
.dl-page {
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

.dl-card {
  padding: 14px 14px 12px;
  display: flex;
  flex-direction: column;
}

.table-zone{
  flex: 1;
  min-height: 0;
}

/* 新增：loading skeleton / error / empty 的容器样式 */
.table-skeleton{
  padding: 10px 6px;
}
.state-box{
  height: 100%;
  display:flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align:center;
  padding: 24px 14px;
}
.state-title{
  font-weight: 900;
  font-size: 16px;
  margin-bottom: 8px;
}
.state-msg{
  color: var(--muted);
  margin-bottom: 14px;
  max-width: 520px;
}
.state-actions{
  display:flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content:center;
}
.table-empty{
  height: 100%;
  display:flex;
  align-items:center;
  justify-content:center;
  padding: 10px 0;
}

.dl-stat-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 20px;
  margin-bottom: 16px;
  background: rgba(143, 165, 156, 0.06);
  border: 1px solid rgba(143, 165, 156, 0.14);
  border-radius: 10px;
  font-size: 16px; font-weight: 800;
  color: #5F7D70;
}

.dl-stat-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ref-dl-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: var(--brand-primary-3);
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.4px;
  text-decoration: none;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
  box-shadow: 0 4px 12px rgba(95, 125, 112, 0.18);
}
.ref-dl-btn:hover {
  background: #7f9f94;
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(95, 125, 112, 0.22);
}
.ref-dl-btn:active {
  transform: translateY(0);
}
.ref-dl-btn.is-downloading {
  pointer-events: none;
  opacity: 0.82;
}

.cart-btn {
  --el-button-bg-color: rgba(143, 165, 156, 0.12);
  --el-button-border-color: rgba(143, 165, 156, 0.30);
  --el-button-text-color: #4a6b5c;
  --el-button-hover-bg-color: rgba(143, 165, 156, 0.22);
  --el-button-hover-border-color: #8fa59c;
  --el-button-hover-text-color: #2d4a3e;
  gap: 6px;
  font-weight: 700;
  justify-self: end;
}
.cart-btn svg { flex-shrink: 0; }

.dl-file-btn {
  --el-button-bg-color: rgba(143, 165, 156, 0.12);
  --el-button-border-color: rgba(143, 165, 156, 0.30);
  --el-button-text-color: #4a6b5c;
  --el-button-hover-bg-color: rgba(143, 165, 156, 0.22);
  --el-button-hover-border-color: #8fa59c;
  --el-button-hover-text-color: #2d4a3e;
  font-weight: 700;
}

/* domain tree */
.dl-tree-domain {
  border-left: 3px solid;
  border-radius: 0 10px 10px 0;
  padding: 10px 0 10px 16px;
  margin-bottom: 12px;
  background: var(--surface);
}
.dl-tree-root {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 4px 14px; border-radius: 8px; border: 1px solid;
  font-size: 14px; font-weight: 800; margin-bottom: 8px;
}
.dl-tree-root-dot { width: 8px; height: 8px; border-radius: 99px; flex-shrink: 0; }
.dl-tree-branches { display: flex; flex-direction: column; gap: 8px; }
.dl-tree-branch { padding-left: 4px; }
.dl-tree-branch-label { font-size: 13px; font-weight: 700; color: var(--muted); margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.3px; }

.search-row {
  display: grid;
  grid-template-columns: 64px 140px 1fr 78px auto;
  gap: 10px;
  align-items: center;
  margin: 10px 0 12px;
}

.search-label {
  font-weight: 900;
  color: var(--text);
}

.sel {
  width: 100%;
}

.btn {
  border: 1px solid var(--border);
  background: var(--surface);
}

.tbl {
  border-radius: 14px;
  overflow: hidden;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

/* tabs：气泡 */
:deep(.dl-tabs .el-tabs__header){ margin: 0 0 10px; }
:deep(.dl-tabs .el-tabs__nav-wrap::after){ display:none; }
:deep(.dl-tabs .el-tabs__active-bar){ display:none; }
:deep(.dl-tabs .el-tabs__nav){ gap: 10px; }
:deep(.dl-tabs .el-tabs__item){
  height: auto;
  line-height: 1;
  padding: 10px 18px;
  margin: 0 !important;
  border-radius: 999px;
  background: var(--surface);
  border: 1px solid var(--border);
  font-weight: 900;
  color: var(--text);
  box-shadow: 0 6px 14px rgba(0,0,0,.06), inset 0 1px 0 rgba(255,255,255,.05);
  transition: transform .15s ease, box-shadow .15s ease, background .15s ease, border-color .15s ease;
}
:deep(.dl-tabs .el-tabs__item:hover){
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(0,0,0,.10);
}
:deep(.dl-tabs .el-tabs__item.is-active){
  background: var(--brand-primary);
  border-color: transparent;
  color: #fff !important;
  box-shadow: 0 12px 26px rgba(0,0,0,.14);
}

/* Table — match DataBrowse */
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

/* Dataset link — match DataBrowse exact */
.tbl :deep(a.dataset-link) {
  color: var(--brand-primary-3) !important;
  font-weight: 800;
  border-radius: 8px;
  padding: 2px 6px;
  text-decoration: none;
  transition: background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}
.tbl :deep(a.dataset-link:hover) {
  background: rgba(143, 165, 156, 0.22);
  box-shadow: 0 0 0 1px rgba(143, 165, 156, 0.35) inset;
  color: #6f887d !important;
  text-decoration: none;
}
.tbl :deep(a.dataset-link:focus-visible) {
  outline: 2px solid rgba(143, 165, 156, 0.25);
  outline-offset: 2px;
}

@media (max-width: 980px) {
  .search-row { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .dl-stat-bar { flex-wrap: wrap; font-size: 14px; }
  .dl-card { padding: 10px; }
}

/* ===== dialog 样式你原来的保留即可（省略不动） ===== */
:global(.bubble-overlay){
  background-color: rgba(0, 0, 0, 0.35) !important;
  backdrop-filter: blur(8px);
}
:global(.el-dialog.bubble-dialog){
  overflow: hidden;
  border: 1px solid rgba(0,0,0,.06);
  box-shadow: 0 18px 60px rgba(0,0,0,.18);
  background: rgba(255, 255, 255, 0.98);
  transform-origin: top center;
}
:global(.el-dialog.bubble-dialog .el-dialog__header){
  padding: 16px 18px 12px;
  background: linear-gradient(90deg, rgba(0,0,0,.02), rgba(0,0,0,0));
  border-bottom: 1px solid var(--border);
}
:global(.el-dialog.bubble-dialog .el-dialog__footer){
  padding: 12px 18px 16px;
  border-top: 1px solid var(--border);
  background: rgba(0,0,0,.01);
}
:global(.el-dialog.bubble-dialog .el-dialog__headerbtn){ border-radius: 10px; }
:global(.el-dialog.bubble-dialog .el-dialog__headerbtn:hover){ background: rgba(0,0,0,.04); }
:global(.el-dialog.bubble-dialog .el-dialog__body){ padding: 14px 18px 16px; }
:global(.dialog-fade-enter-active .el-dialog.bubble-dialog){ animation: bubbleIn .18s ease-out both; }
:global(.dialog-fade-leave-active .el-dialog.bubble-dialog){ animation: bubbleOut .14s ease-in both; }
@keyframes bubbleIn { from{opacity:0; transform:translateY(-10px) scale(.985)} to{opacity:1; transform:translateY(0) scale(1)} }
@keyframes bubbleOut{ from{opacity:1; transform:translateY(0) scale(1)} to{opacity:0; transform:translateY(-8px) scale(.99)} }

.dlg-meta{
  display:flex; flex-wrap: wrap; gap: 16px;
  padding: 6px 0 12px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 10px;
}
.grp{ padding: 12px 0; border-bottom: 1px solid var(--border); }
.grp:last-child{ border-bottom: none; }
.grp-title{ font-weight: 900; margin-bottom: 10px; }

.chip-grid{
  display:grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.chip{
  width: 100%;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: var(--surface);
  padding: 10px 12px;
  cursor: pointer;
  display:flex;
  align-items:center;
  justify-content: space-between;
  gap: 12px;
  transition: transform .14s ease, box-shadow .14s ease, border-color .14s ease;
}
.chip:hover{
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(0,0,0,.08);
  border-color: rgba(0,0,0,.12);
}
.chip:disabled{
  cursor: wait;
  opacity: .62;
  transform: none;
  box-shadow: none;
}
.chip.is-downloading{
  opacity: 1;
  border-color: rgba(95, 125, 112, .48);
  box-shadow: 0 8px 20px rgba(95, 125, 112, .14);
}
.chip-left{ display:flex; align-items:center; gap: 10px; min-width: 0; }
.chip-name{
  font-weight: 800;
  color: var(--text);
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chip-format{
  font-size: 12px;
  font-weight: 900;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(0,0,0,.10);
  background: rgba(0,0,0,.03);
}
.chip-action{
  font-size: 12px;
  font-weight: 900;
  padding: 6px 12px;
  border-radius: 999px;
  min-width: 82px;
  background: var(--brand-primary-3, #8fa59c) !important;
  color: #fff !important;
  box-shadow: 0 4px 10px rgba(95,125,112,.20);
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.download-spinner{
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,.45);
  border-top-color: #fff;
  animation: downloadSpin .72s linear infinite;
  flex: 0 0 auto;
}
.cart-download-progress{
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid rgba(143, 165, 156, .28);
  border-radius: 12px;
  background: rgba(143, 165, 156, .08);
}
.cart-download-progress-head{
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #456659;
  font-size: 13px;
  font-weight: 800;
}
.cart-download-progress-note{
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}
@keyframes downloadSpin { to { transform: rotate(360deg); } }
@media (max-width: 860px){
  .chip-grid{ grid-template-columns: 1fr; }
  .chip-name{ max-width: 240px; }
}
</style>

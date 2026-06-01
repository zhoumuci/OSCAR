<template>
  <div class="dl-page">
    <div class="container">
      <div class="page-title">Download</div>
      <div class="page-sub">Use our database to download the data you need.</div>

      <div class="float-card dl-card">
        <el-tabs v-model="activeTab" class="dl-tabs" stretch>
          <el-tab-pane label="Integration" name="integration" />
          <el-tab-pane label="RNA" name="rna" />
          <el-tab-pane label="ATAC" name="atac" />
          <el-tab-pane label="TF" name="tf" />
        </el-tabs>

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

          <el-button class="btn" :disabled="state!=='ready'" @click="downloadAll">Download all</el-button>
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
              v-if="filteredRows.length > 0"
              :data="pageRows"
              stripe
              border
              class="tbl"
            >
              <el-table-column prop="sampleId" label="Sample ID" min-width="160">
                <template #default="{ row }">
                  <span class="mono linkish" @click="copyText(row.sampleId)">{{ row.sampleId }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="biosampleType" label="Biosample type" min-width="140" />
              <el-table-column prop="biosampleName" label="Biosample name" min-width="160" show-overflow-tooltip />

              <el-table-column v-if="showCellType" prop="cellType" label="Cell Type" min-width="120" />

              <el-table-column prop="tissueType" label="Tissue" min-width="120" />
              <el-table-column prop="disease" label="Disease" min-width="160" show-overflow-tooltip />
              <el-table-column prop="category" label="Category" min-width="120" />

              <el-table-column v-if="showRegionNumber" prop="regionNumber" label="Region number" min-width="140" />

              <el-table-column label="QC" min-width="130" align="center">
                <template #default="{ row }">
                  <div class="qc">
                    <span v-for="(q, i) in row.qc" :key="i" class="qc-dot" :class="q" :title="q" />
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="Download" min-width="140" align="center">
                <template #default="{ row }">
                  <el-button size="small" type="primary" plain @click="openDownloads(row)">Files</el-button>
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
        <div class="pager" v-if="state==='ready' && filteredRows.length > 0">
          <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :total="filteredRows.length"
            :page-size="pageSize"
            :current-page="page"
            @current-change="onPageChange"
          />
        </div>
      </div>

      <!-- 下载弹窗 -->
      <el-dialog
        v-model="dlgOpen"
        width="820px"
        :title="dlgTitle"
        custom-class="bubble-dialog"
        modal-class="bubble-overlay"
        :append-to-body="true"
        class="float-card"
      >
        <div v-if="dlgRow" class="dlg-body">
          <div class="dlg-meta">
            <div><b>Sample:</b> <span class="mono">{{ dlgRow.sampleId }}</span></div>
            <div><b>Tissue:</b> {{ dlgRow.tissueType }}</div>
            <div><b>Disease:</b> {{ dlgRow.disease }}</div>
          </div>

          <div v-for="(g, gi) in dlgRow.downloads" :key="gi" class="grp">
            <div class="grp-title">{{ g.title }}</div>

            <div class="chip-grid">
              <button
                v-for="f in g.files"
                :key="f.id"
                class="chip"
                @click="triggerDownload(f.url)"
                type="button"
              >
                <span class="chip-left">
                  <span class="chip-name">{{ f.title }}</span>
                  <span class="chip-format">{{ f.format.toUpperCase() }}</span>
                </span>
                <span class="chip-action">Download</span>
              </button>
            </div>
          </div>
        </div>

        <template #footer>
          <el-button @click="dlgOpen = false">Close</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { fetchDownloadRows } from "@/api/download";
import type { DownloadRow, TabKey } from "@/api/download";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

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

const activeTab = ref<TabKey>("integration");
const field = ref<string>("");
const keyword = ref<string>("");

const page = ref(1);


function onPageChange(p: number) {
page.value = p;
}
const pageSize = ref(13);

const allRows = ref<DownloadRow[]>([]);

const showRegionNumber = computed(() => activeTab.value === "atac" || activeTab.value === "tf");
const showCellType = computed(() => activeTab.value !== "rna");

const fields = computed(() => {
  const base = [
    { label: "Sample ID", value: "sampleId" },
    { label: "Biosample type", value: "biosampleType" },
    { label: "Biosample name", value: "biosampleName" },
    { label: "Tissue", value: "tissueType" },
    { label: "Disease", value: "disease" },
    { label: "Category", value: "category" },
  ];
  if (showCellType.value) base.splice(3, 0, { label: "Cell Type", value: "cellType" });
  if (showRegionNumber.value) base.push({ label: "Region number", value: "regionNumber" });
  return base;
});

function normalize(v: unknown) {
  return String(v ?? "").toLowerCase().trim();
}

const filteredRows = computed(() => {
  const kw = normalize(keyword.value);
  const kf = field.value;

  if (!kw) return allRows.value;

  return allRows.value.filter((r) => {
    if (kf) return normalize((r as any)[kf]).includes(kw);
    const hay = [
      r.sampleId, r.biosampleType, r.biosampleName, r.cellType ?? "",
      r.tissueType, r.disease, r.category, r.regionNumber ?? ""
    ].map(normalize);
    return hay.some(x => x.includes(kw));
  });
});

const pageRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

async function fetchRows() {
  state.value = "loading";
  errorMsg.value = "";
  errorCode.value = null;

  try {
    if (USE_MOCK) {
      // 你可以加一点延迟来观察 loading
      //await new Promise(r => setTimeout(r, 500));
      allRows.value = await fetchDownloadRows(activeTab.value);
      state.value = "ready";
      return;
    }

    // TODO: 后端接口接入时替换：
    // const { data } = await axios.get("/api/download/list", { params: { domain: activeTab.value, ... } })
    // allRows.value = data.items
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

function triggerDownload(url: string) {
  const a = document.createElement("a");
  a.href = url;
  a.target = "_blank";
  a.rel = "noopener";
  a.click();
}

function downloadAll() {
  triggerDownload(`/api/download/${activeTab.value}/ALL.zip`);
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    // ignore
  }
}

/** tab 切换：重置搜索/分页，符合你现在全站的直觉 */
watch(activeTab, async () => {
  page.value = 1;
  field.value = "";
  keyword.value = "";
  await fetchRows();
});

onMounted(fetchRows);

// ---------------- 弹窗 ----------------
const dlgOpen = ref(false);
const dlgRow = ref<DownloadRow | null>(null);

const dlgTitle = computed(() => {
  if (!dlgRow.value) return "Downloads";
  return `Downloads • ${activeTab.value.toUpperCase()} • ${dlgRow.value.sampleId}`;
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

.search-row {
  display: grid;
  grid-template-columns: 70px 180px 1fr 100px 140px;
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

/* ID */
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}
.linkish { cursor: pointer; font-weight: 800; }
.linkish:hover { text-decoration: underline; }

/* QC dots */
.qc { display:flex; justify-content:center; gap: 6px; }
.qc-dot{
  width: 9px; height: 9px; border-radius: 999px;
  display:inline-block;
  border: 1px solid rgba(0,0,0,.08);
}
.qc-dot.pass{ background: var(--cuitao, #14b8a6); }
.qc-dot.warn{ background: #f59e0b; }
.qc-dot.fail{ background: #ef4444; }

@media (max-width: 980px) {
  .search-row { grid-template-columns: 1fr; }
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
  padding: 6px 10px;
  border-radius: 999px;
  color: #fff;
  background: var(--brand-primary);
  white-space: nowrap;
}
@media (max-width: 860px){
  .chip-grid{ grid-template-columns: 1fr; }
  .chip-name{ max-width: 240px; }
}
</style>
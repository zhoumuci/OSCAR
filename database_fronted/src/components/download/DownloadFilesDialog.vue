<template>
  <el-dialog
    v-model="visible"
    width="900px"
    :title="dialogTitle"
    custom-class="bubble-dialog"
    modal-class="bubble-overlay"
    :append-to-body="true"
    class="float-card"
  >
    <div v-if="row" class="dlg-body">
      <div class="dlg-meta" style="margin-bottom:16px">
        <span class="mono" style="font-weight:800">{{ row.datasetId }}</span> · {{ row.tissue }} · {{ row.disease }}
      </div>
      <template v-for="dom in row.downloads" :key="dom.domain">
        <div class="dl-tree-domain" :style="{ borderColor: dom.color }">
          <div class="dl-tree-root" :style="{ background: dom.color+'18', color: dom.color, borderColor: dom.color }">
            <span class="dl-tree-root-dot" :style="{ background: dom.color }" />
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
                  :class="{ 'is-downloading': activeDownloadId === f.id }"
                  :disabled="activeDownloadId !== ''"
                  type="button"
                  @click="triggerDownload(f)"
                >
                  <span class="chip-left">
                    <span class="chip-name">{{ f.title }}</span>
                    <span class="chip-format">{{ f.format.toUpperCase() }}</span>
                  </span>
                  <span class="chip-action" aria-live="polite">
                    <span v-if="activeDownloadId === f.id" class="download-spinner" aria-hidden="true" />
                    {{ activeDownloadId === f.id ? "Preparing…" : "Download" }}
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
    <template #footer>
      <el-button :disabled="activeDownloadId !== ''" @click="visible = false">Close</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import type { DownloadFile, DownloadRow } from "@/api/download";

const props = defineProps<{
  modelValue: boolean;
  row: DownloadRow | null;
}>();
const emit = defineEmits<{ "update:modelValue": [value: boolean]; }>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});
const dialogTitle = computed(() => props.row ? `Downloads - ${props.row.datasetId}` : "Downloads");
const activeDownloadId = ref("");

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
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.style.display = "none";
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

async function triggerDownload(file: DownloadFile) {
  if (activeDownloadId.value) return;
  activeDownloadId.value = file.id;
  try {
    const response = await fetch(file.url, { credentials: "same-origin" });
    if (!response.ok) throw new Error(`Download failed with HTTP ${response.status}`);
    const blob = await response.blob();
    saveBlob(blob, filenameFromResponse(response, `${file.id}.${file.format}`));
    ElMessage.success("Download prepared.");
  } catch (error: any) {
    ElMessage.error(error?.message || "Download failed. Please retry.");
  } finally {
    activeDownloadId.value = "";
  }
}
</script>

<style scoped>
.dlg-body { display: flex; flex-direction: column; gap: 14px; }
.mono { font-family: "JetBrains Mono", "Fira Code", "Cascadia Code", monospace; }
.dlg-meta { display: flex; flex-wrap: wrap; gap: 16px; padding: 6px 0 12px; border-bottom: 1px solid var(--border); font-size: 13px; color: var(--text); }
.dl-tree-domain { border-left: 3px solid; border-radius: 0 10px 10px 0; padding: 10px 0 10px 16px; margin-bottom: 12px; background: var(--surface); }
.dl-tree-root { display: inline-flex; align-items: center; gap: 8px; padding: 4px 14px; border: 1px solid; border-radius: 8px; margin-bottom: 8px; font-size: 14px; font-weight: 800; }
.dl-tree-root-dot { width: 8px; height: 8px; flex-shrink: 0; border-radius: 99px; }
.dl-tree-branches { display: flex; flex-direction: column; gap: 8px; }
.dl-tree-branch { padding-left: 4px; }
.dl-tree-branch-label { margin-bottom: 6px; color: var(--muted); font-size: 13px; font-weight: 700; letter-spacing: 0.3px; text-transform: uppercase; }
.chip-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.chip { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 16px; background: var(--surface); color: var(--text); text-align: left; appearance: none; cursor: pointer; transition: transform .14s ease, box-shadow .14s ease, border-color .14s ease; }
.chip:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 10px 22px rgba(0,0,0,.08); border-color: rgba(0,0,0,.12); }
.chip:disabled { cursor: wait; opacity: .62; transform: none; box-shadow: none; }
.chip.is-downloading { opacity: 1; border-color: rgba(95,125,112,.48); box-shadow: 0 8px 20px rgba(95,125,112,.14); }
.chip-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.chip-name { max-width: 320px; overflow: hidden; color: var(--text); font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.chip-format { padding: 3px 10px; border: 1px solid rgba(0,0,0,.10); border-radius: 999px; background: rgba(0,0,0,.03); color: var(--muted); font-size: 12px; font-weight: 900; }
.chip-action { display: inline-flex; align-items: center; justify-content: center; min-width: 82px; gap: 6px; padding: 6px 12px; border-radius: 999px; background: var(--brand-primary-3, #8fa59c) !important; color: #fff !important; box-shadow: 0 4px 10px rgba(95,125,112,.2); font-size: 12px; font-weight: 900; white-space: nowrap; }
.download-spinner { width: 12px; height: 12px; flex: 0 0 auto; border: 2px solid rgba(255,255,255,.45); border-top-color: #fff; border-radius: 50%; animation: downloadSpin .72s linear infinite; }
@keyframes downloadSpin { to { transform: rotate(360deg); } }
@media (max-width: 520px) { .chip-grid { grid-template-columns: 1fr; } .chip-name { max-width: 240px; } }
</style>

<style>
.bubble-overlay { background-color: rgba(0,0,0,.35) !important; backdrop-filter: blur(8px); }
.el-dialog.bubble-dialog { overflow: hidden; border: 1px solid rgba(0,0,0,.06); box-shadow: 0 18px 60px rgba(0,0,0,.18); background: rgba(255,255,255,.98); transform-origin: top center; }
.el-dialog.bubble-dialog .el-dialog__header { padding: 16px 18px 12px; background: linear-gradient(90deg,rgba(0,0,0,.02),rgba(0,0,0,0)); border-bottom: 1px solid var(--border); }
.el-dialog.bubble-dialog .el-dialog__footer { padding: 12px 18px 16px; border-top: 1px solid var(--border); background: rgba(0,0,0,.01); }
.el-dialog.bubble-dialog .el-dialog__headerbtn { border-radius: 10px; }
.el-dialog.bubble-dialog .el-dialog__headerbtn:hover { background: rgba(0,0,0,.04); }
.el-dialog.bubble-dialog .el-dialog__body { padding: 14px 18px 16px; }
.dialog-fade-enter-active .el-dialog.bubble-dialog { animation: bubbleIn .18s ease-out both; }
.dialog-fade-leave-active .el-dialog.bubble-dialog { animation: bubbleOut .14s ease-in both; }
@keyframes bubbleIn { from { opacity: 0; transform: translateY(-10px) scale(.985); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes bubbleOut { from { opacity: 1; transform: translateY(0) scale(1); } to { opacity: 0; transform: translateY(-8px) scale(.99); } }
</style>

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
        </div>

        <!-- 搜索区（统一风格） -->
        <div class="search-row">
          <div class="search-label">Search:</div>

          <el-tooltip placement="top" effect="light" :show-after="200">
            <template #content>
              <div class="cart-help-copy">
                Choose a field to search it alone; without a field, the keyword searches all sample metadata columns.
              </div>
            </template>
            <span class="select-field-help-wrap">
              <el-select v-model="field" placeholder="Select field" class="sel" clearable>
                <el-option v-for="f in fields" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
              <span class="cart-btn-help-icon select-field-help-icon" role="button" tabindex="0" aria-label="Search field help">?</span>
            </span>
          </el-tooltip>

          <el-input
            v-model="keyword"
            placeholder="Please enter the search content"
            clearable
            @keyup.enter="onSearch"
          />

          <el-button type="primary" :loading="state==='loading'" @click="onSearch">Search</el-button>

          <el-tooltip placement="top" effect="light" :show-after="200">
            <template #content>
              {{ allFilteredRowsSelected
                ? `Clear the ${filteredRows.length} selected sample(s) matching the current filters.`
                : `Select all ${filteredRows.length} sample(s) matching the current filters, not only the current table page.` }}
            </template>
            <span class="filtered-selection-wrap">
              <el-button
                class="select-all-results-btn"
                :disabled="state !== 'ready' || filteredRows.length === 0 || cartDownloadActive"
                @click="toggleFilteredRowsSelection"
              >
                {{ filteredSelectionLabel }}
              </el-button>
            </span>
          </el-tooltip>

          <el-tooltip placement="top" effect="light" :show-after="200">
            <template #content>
              <div class="cart-help-copy">{{ cartButtonHelp }}</div>
            </template>
            <span class="cart-btn-help-wrap">
              <el-button class="cart-btn" :disabled="!canOpenCart" @click="openCart">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 2h1.5l1.2 6.5a1 1 0 00.98.8h5.6a1 1 0 00.98-.8L13 4.7H4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="6" cy="13" r="1" fill="currentColor"/><circle cx="11" cy="13" r="1" fill="currentColor"/></svg>
                {{ cartButtonLabel }}
              </el-button>
              <span class="cart-btn-help-icon" role="button" tabindex="0" aria-label="Download cart help">?</span>
            </span>
          </el-tooltip>
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
              ref="downloadTableRef"
              v-if="sortedRows.length > 0"
              :data="pageRows"
              row-key="datasetId"
              stripe
              border
              class="tbl"
              @selection-change="onSelectionChange"
              @sort-change="onTableSortChange"
            >
              <el-table-column type="selection" width="44" fixed :selectable="checkSelectable" />
              <el-table-column prop="datasetId" label="DatasetID" min-width="165" fixed sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>DatasetID</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.datasetId" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
                <template #default="{ row }">
                  <el-link type="primary" underline="never" class="dataset-link" @click="openDataset(row.datasetId)">
                    {{ row.datasetId }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="sampleType" label="Sample Type" min-width="165" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Sample Type</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.sampleType" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="tissue" label="Tissue" min-width="125" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Tissue</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.tissue" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="sampleName" label="Sample Name" min-width="205" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Sample Name</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.sampleName" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="cells" label="Cells" min-width="125" align="center" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Cells</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.cells" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
                <template #default="{ row }">
                  {{ (row.cells ?? 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="platform" label="Platform" min-width="150" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Platform</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.platform" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="sourceId" label="SourceID" min-width="145" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>SourceID</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.sourceId" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="disease" label="Disease" min-width="135" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Disease</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.disease" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="sampleSource" label="Sample Source" min-width="180" sortable="custom">
                <template #header>
                  <span class="browse-column-header">
                    <span>Sample Source</span>
                    <el-tooltip placement="top" effect="light" :show-after="180">
                      <template #content><div v-for="line in DOWNLOAD_COLUMN_TOOLTIPS.sampleSource" :key="line">{{ line }}</div></template>
                      <el-icon class="column-help-icon" @click.stop><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </template>
              </el-table-column>

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

        <section class="additional-resources-panel" aria-labelledby="additional-resources-title">
          <div class="additional-resources-heading">
            <div id="additional-resources-title" class="additional-resources-title">Additional Resources</div>
            <div class="additional-resources-subtitle">
              Reference annotations and tissue/cell type-specific marker Peak-to-Gene links
            </div>
          </div>
          <div class="additional-resource-grid">
            <a
              v-for="resource in additionalResources"
              :key="resource.id"
              :href="resource.url"
              download
              class="chip additional-resource-chip"
              :class="[
                resource.toneClass,
                { 'is-downloading': additionalResourceStarting === resource.id },
              ]"
              @click="acknowledgeAdditionalResource(resource.id)"
            >
              <span class="additional-resource-card-head">
                <span class="chip-left">
                  <span class="chip-name">{{ resource.title }}</span>
                  <span class="chip-format">{{ resource.format }}</span>
                </span>
                <span class="chip-action">
                  <span v-if="additionalResourceStarting === resource.id" class="download-spinner"></span>
                  {{ additionalResourceStarting === resource.id ? "Starting…" : "Download" }}
                </span>
              </span>
              <span v-if="resource.id === 'epi-genetic-annotation'" class="epi-annotation-list">
                <span v-for="annotation in EPI_ANNOTATION_LABELS" :key="annotation" class="epi-annotation-tag">
                  {{ annotation }}
                </span>
              </span>
            </a>
          </div>
        </section>
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
            {{ cartDialogSummary }}
          </div>
          <div v-if="cartDownloadCheckpoint && !cartDownloadActive" class="cart-recovery-card">
            <div class="cart-recovery-copy">
              <strong>{{ checkpointPendingCount > 0 ? 'Unfinished download queue' : 'Failed downloads available' }}</strong>
              <span v-if="checkpointPendingCount > 0">
                {{ checkpointRecoverableCount }} unfinished or failed sample(s) can continue in their original order.
              </span>
              <span v-else>{{ checkpointFailedCount }} failed sample(s) can be retried without downloading successful files again.</span>
            </div>
            <div class="cart-recovery-actions">
              <el-button v-if="checkpointPendingCount > 0" type="primary" @click="resumeCartDownload">Resume</el-button>
              <el-button v-if="checkpointFailedCount > 0 && checkpointPendingCount === 0" type="primary" plain @click="retryFailedCartDownloads">Retry failed</el-button>
              <el-button @click="discardCartDownloadCheckpoint">Discard</el-button>
            </div>
          </div>
          <div v-if="cartDownloadTotal > 0" class="cart-download-progress" aria-live="polite">
            <div class="cart-download-progress-head">
              <span>
                <template v-if="cartDownloadActive">
                  Batch {{ cartDownloadBatch }} of {{ cartDownloadBatchTotal }} ·
                  {{ cartDownloadWaitingNextBatch ? 'starting next batch' : `downloading ${cartDownloadCurrent} of ${cartDownloadTotal}` }}
                </template>
                <template v-else>Processed {{ cartDownloadProcessed }} of {{ cartDownloadTotal }}</template>
              </span>
              <span v-if="cartDownloadDataset" class="mono">{{ cartDownloadDataset }}</span>
            </div>
            <el-progress :percentage="cartDownloadPercent" :stroke-width="10" :status="cartDownloadActive ? undefined : cartDownloadErrors ? 'exception' : 'success'" />
            <div class="cart-download-progress-note">
              <span v-if="cartDownloadActive && cartDownloadWaitingNextBatch">The completed batch is closed. The next batch will start automatically.</span>
              <span v-else-if="cartDownloadActive">Files are prepared sequentially. You may pause safely; the current unfinished file will restart from the beginning when resumed.</span>
              <span v-else-if="checkpointPendingCount > 0">The queue is paused or was interrupted. Resume continues from the first unfinished or failed sample.</span>
              <span v-else-if="cartDownloadErrors">Completed with {{ cartDownloadErrors }} failed download(s).</span>
              <span v-else>All selected downloads have been prepared.</span>
            </div>
          </div>
          <div v-if="cartDownloadFailedDatasetIds.length > 0 && !cartDownloadActive" class="cart-failed-card">
            <strong>{{ cartDownloadFailedDatasetIds.length }} failed sample(s)</strong>
            <span>{{ cartDownloadFailedDatasetIds.join(', ') }}</span>
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
                    :disabled="cartDownloadActive || cartDownloadCheckpoint !== null"
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
          <el-button v-if="cartDownloadActive" type="warning" plain @click="pauseCartDownload">Pause</el-button>
          <el-button :disabled="cartDownloadActive" @click="cartOpen = false">Close</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="humanVerificationOpen"
        width="440px"
        title="Human verification"
        custom-class="bubble-dialog human-verification-dialog"
        modal-class="bubble-overlay"
        :append-to-body="true"
        :close-on-click-modal="false"
        class="float-card"
        @closed="resetHumanVerification"
      >
        <div class="human-verification-body">
          <div class="human-verification-note">
            This download contains more than {{ HUMAN_VERIFICATION_SAMPLE_THRESHOLD }} samples. Complete this quick check before the automatic download queue starts.
          </div>
          <label class="human-verification-challenge">
            <span v-if="humanVerificationBusy">Preparing secure verification…</span>
            <span v-else>What is {{ humanVerificationLeft }} + {{ humanVerificationRight }}?</span>
            <el-input
              v-model="humanVerificationAnswer"
              inputmode="numeric"
              autocomplete="off"
              placeholder="Enter the answer"
              :disabled="humanVerificationBusy || !humanVerificationChallengeId"
              @input="humanVerificationError = ''"
              @keyup.enter="confirmHumanVerification"
            />
          </label>
          <div v-if="humanVerificationError" class="human-verification-error" role="alert">
            {{ humanVerificationError }}
          </div>
        </div>
        <template #footer>
          <el-button :disabled="humanVerificationBusy" @click="humanVerificationOpen = false">Cancel</el-button>
          <el-button type="primary" :loading="humanVerificationBusy" :disabled="!humanVerificationChallengeId" @click="confirmHumanVerification">Verify and continue</el-button>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { InfoFilled } from "@element-plus/icons-vue";
import { fetchDownloadRows, buildDownloads } from "@/api/download";
import type { DownloadFile, DownloadRow, DownloadTypeNode, TabKey } from "@/api/download";
import { buildApiUrl } from "@/config/api";

const DOWNLOAD_COLUMN_TOOLTIPS = {
  datasetId: [
    "OSCAR sample ID. Select it to open Sample Details.",
  ],
  sampleType: [
    "Type of biological material, such as tissue or sorted cells.",
  ],
  tissue: [
    "Standardized tissue assigned to the sample.",
  ],
  sampleName: [
    "Sample name from the source dataset.",
  ],
  cells: [
    "Number of cells retained for this sample.",
  ],
  platform: [
    "Sequencing platform reported for the sample.",
  ],
  sourceId: [
    "Sample ID used by the original data source.",
  ],
  disease: [
    "Disease or control status recorded for the sample.",
  ],
  sampleSource: [
    "Original tissue or sample-source label.",
  ],
} as const;

type CartDownloadCheckpoint = {
  version: 1 | 2;
  domain: string;
  type: string;
  format: string;
  sampleIds: string[];
  pendingSampleIds: string[];
  failedSampleIds: string[];
  updatedAt: number;
  bulkAuthorizationToken?: string;
  bulkAuthorizationExpiresAt?: number;
};

type PendingCartDownloadRequest = {
  sampleIds: string[];
  domain: string;
  type: string;
  format: string;
};

type BulkDownloadAuthorization = {
  token: string;
  expiresAt: number;
};

type DownloadTableInstance = {
  clearSelection: () => void;
  toggleRowSelection: (row: DownloadRow, selected?: boolean) => void;
};

const router = useRouter();
const CART_DOWNLOAD_BATCH_SIZE = 10;
const CART_DOWNLOAD_BATCH_PAUSE_MS = 350;
const CART_DOWNLOAD_MIN_REQUEST_INTERVAL_MS = 650;
const HUMAN_VERIFICATION_SAMPLE_THRESHOLD = 10;
const CART_DOWNLOAD_CHECKPOINT_KEY = "oscar.download.cart.checkpoint.v1";
const selected = ref<DownloadRow[]>([]);
const downloadTableRef = ref<DownloadTableInstance | null>(null);
let syncingTableSelection = false;
const cartDownloadCheckpoint = ref<CartDownloadCheckpoint | null>(null);
const cartDownloadFailedDatasetIds = ref<string[]>([]);
const humanVerificationOpen = ref(false);
const humanVerificationLeft = ref(0);
const humanVerificationRight = ref(0);
const humanVerificationAnswer = ref("");
const humanVerificationError = ref("");
const humanVerificationChallengeId = ref("");
const humanVerificationBusy = ref(false);
const pendingCartDownloadRequest = ref<PendingCartDownloadRequest | null>(null);
const pendingCartDownloadCheckpoint = ref<CartDownloadCheckpoint | null>(null);
const selectedCartBatchCount = computed(() => Math.ceil(selected.value.length / CART_DOWNLOAD_BATCH_SIZE));
const checkpointPendingCount = computed(() => cartDownloadCheckpoint.value?.pendingSampleIds.length ?? 0);
const checkpointFailedCount = computed(() => cartDownloadCheckpoint.value?.failedSampleIds.length ?? 0);
const checkpointRecoverableCount = computed(() => {
  const checkpoint = cartDownloadCheckpoint.value;
  if (!checkpoint) return 0;
  return new Set([...checkpoint.pendingSampleIds, ...checkpoint.failedSampleIds]).size;
});
const canOpenCart = computed(() => selected.value.length > 0 || cartDownloadCheckpoint.value !== null);
const cartButtonLabel = computed(() => {
  if (selected.value.length > 0) return `Download cart (${selected.value.length})`;
  return checkpointRecoverableCount.value > 0
    ? `Resume downloads (${checkpointRecoverableCount.value})`
    : "Download cart";
});
const cartButtonHelp = computed(() => {
  if (checkpointPendingCount.value > 0) {
    return `${checkpointPendingCount.value} unfinished sample download(s) are saved. Open the cart to resume from the checkpoint.`;
  }
  if (checkpointFailedCount.value > 0) {
    return `${checkpointFailedCount.value} failed sample download(s) can be retried without repeating successful files.`;
  }
  if (selected.value.length === 0) {
    return "Select one or more samples from the table first. Downloads run sequentially in automatic groups of up to 10 samples.";
  }
  return `Download files for ${selected.value.length} selected sample${selected.value.length === 1 ? "" : "s"} in ${selectedCartBatchCount.value} automatic batch${selectedCartBatchCount.value === 1 ? "" : "es"}.`;
});
const cartDialogSummary = computed(() => {
  if (selected.value.length > 0) {
    return `${selected.value.length} sample(s) selected · up to ${CART_DOWNLOAD_BATCH_SIZE} per batch`;
  }
  if (!cartDownloadCheckpoint.value) return "";
  return `${checkpointPendingCount.value} unfinished · ${checkpointFailedCount.value} failed`;
});
function onSelectionChange(rows: DownloadRow[]) {
  if (syncingTableSelection) return;
  const currentPageIds = new Set(pageRows.value.map(row => row.datasetId));
  const outsideCurrentPage = selected.value.filter(row => !currentPageIds.has(row.datasetId));
  const uniqueRows = new Map<string, DownloadRow>();
  [...outsideCurrentPage, ...rows].forEach(row => uniqueRows.set(row.datasetId, row));
  selected.value = [...uniqueRows.values()];
}
function checkSelectable() {
  return !cartDownloadActive.value;
}
function openCart() {
  if (!canOpenCart.value) return;
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
  const firstSample = selected.value[0];
  const checkpointSampleId = cartDownloadCheckpoint.value?.sampleIds[0];
  const downloads = firstSample?.downloads ?? (checkpointSampleId ? buildDownloads(checkpointSampleId) : []);
  return downloads.map(d => ({
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

const selectedDatasetIds = computed(() => new Set(selected.value.map(row => row.datasetId)));
const allFilteredRowsSelected = computed(() => filteredRows.value.length > 0
  && filteredRows.value.every(row => selectedDatasetIds.value.has(row.datasetId)));
const filteredSelectionLabel = computed(() => allFilteredRowsSelected.value
  ? `Clear filtered (${filteredRows.value.length})`
  : `Select all (${filteredRows.value.length})`);

function toggleFilteredRowsSelection() {
  if (!filteredRows.value.length || cartDownloadActive.value) return;
  const shouldSelect = !allFilteredRowsSelected.value;
  const filteredIds = new Set(filteredRows.value.map(row => row.datasetId));
  if (shouldSelect) {
    const uniqueRows = new Map(selected.value.map(row => [row.datasetId, row] as const));
    filteredRows.value.forEach(row => uniqueRows.set(row.datasetId, row));
    selected.value = [...uniqueRows.values()];
  } else {
    selected.value = selected.value.filter(row => !filteredIds.has(row.datasetId));
  }
  void syncCurrentPageSelection();
}

const sortedRows = computed(() => {
  const rows = [...filteredRows.value];
  const dir = sortDir.value === "desc" ? -1 : 1;
  rows.sort((a, b) => {
    let result: number;
    if (sortBy.value === "cells") {
      result = (a.cells ?? 0) - (b.cells ?? 0);
    } else {
      const sortKey = sortBy.value as keyof DownloadRow;
      const left = String(a[sortKey] ?? "");
      const right = String(b[sortKey] ?? "");
      result = left.localeCompare(right, undefined, { numeric: true, sensitivity: "base" });
    }
    if (result !== 0) return dir * result;
    return String(a.datasetId ?? "").localeCompare(String(b.datasetId ?? ""));
  });
  return rows;
});

const pageRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return sortedRows.value.slice(start, start + pageSize.value);
});

async function syncCurrentPageSelection() {
  await nextTick();
  const table = downloadTableRef.value;
  if (!table) return;
  syncingTableSelection = true;
  try {
    table.clearSelection();
    const selectedIds = selectedDatasetIds.value;
    pageRows.value.forEach(row => {
      if (selectedIds.has(row.datasetId)) table.toggleRowSelection(row, true);
    });
  } finally {
    syncingTableSelection = false;
  }
}

watch(pageRows, () => {
  void syncCurrentPageSelection();
}, { flush: "post" });

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

const additionalResourceStarting = ref("");
const EPI_ANNOTATION_LABELS = [
  "Risk SNP",
  "Common SNP",
  "GTEx eQTL",
  "TFBS",
  "Enhancer",
  "Super Enhancer",
  "Methylation",
  "CRISPR",
  "ATAC",
  "3D interactions",
  "DNase peaks",
  "TAD",
  "eRNA",
  "TF-Chip-Seq",
  "T(co)F",
] as const;
const additionalResources = [
  {
    id: "epi-genetic-annotation",
    title: "Epi(genetic) Annotation",
    format: "TAR.GZ",
    url: "/OSCAR/static/combined.tar.gz",
    toneClass: "additional-resource-chip--epi",
  },
  {
    id: "marker-p2g-gene-score",
    title: "Tissue/cell type-specific marker Peak-to-Gene links · Gene score",
    format: "TSV",
    url: "/OSCAR/static/merged_relations_by_tissue_gene_score.tsv",
    toneClass: "additional-resource-chip--score",
  },
  {
    id: "marker-p2g-gene-expression",
    title: "Tissue/cell type-specific marker Peak-to-Gene links · Gene expression",
    format: "TSV",
    url: "/OSCAR/static/merged_relations_by_tissue_gene_expression.tsv",
    toneClass: "additional-resource-chip--expression",
  },
] as const;
const singleDownloadId = ref("");
const cartDownloadActive = ref(false);
const cartDownloadCurrent = ref(0);
const cartDownloadProcessed = ref(0);
const cartDownloadTotal = ref(0);
const cartDownloadDataset = ref("");
const cartDownloadErrors = ref(0);
const cartDownloadKey = ref("");
const cartDownloadBatch = ref(0);
const cartDownloadBatchTotal = ref(0);
const cartDownloadWaitingNextBatch = ref(false);
let cartDownloadAbortController: AbortController | null = null;
let cartDownloadPauseRequested = false;
let pageUnloading = false;
const cartDownloadPercent = computed(() => cartDownloadTotal.value
  ? Math.round((cartDownloadProcessed.value / cartDownloadTotal.value) * 100)
  : 0);

function cartFileKey(domain: string, type: string, format: string) {
  return `${domain}:${type}:${format}`;
}

function acknowledgeAdditionalResource(resourceId: string) {
  additionalResourceStarting.value = resourceId;
  window.setTimeout(() => {
    if (additionalResourceStarting.value === resourceId) {
      additionalResourceStarting.value = "";
    }
  }, 1600);
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

class DownloadHttpError extends Error {
  constructor(readonly status: number) {
    super(`Download failed with HTTP ${status}`);
  }
}

function bulkDownloadUrl(fileUrl: string) {
  const url = new URL(fileUrl, window.location.origin);
  url.pathname = url.pathname.replace(/\/api\/download\//, "/api/download/bulk/");
  return url.toString();
}

async function fetchAndSaveDownload(
  file: DownloadFile,
  bulkAuthorizationToken?: string,
  signal?: AbortSignal
) {
  const response = await fetch(
    bulkAuthorizationToken ? bulkDownloadUrl(file.url) : file.url,
    {
      credentials: "same-origin",
      headers: bulkAuthorizationToken
        ? { "X-OSCAR-Bulk-Download-Token": bulkAuthorizationToken }
        : undefined,
      signal,
    }
  );
  if (!response.ok) throw new DownloadHttpError(response.status);
  const blob = await response.blob();
  saveBlob(blob, filenameFromResponse(response, `${file.id}.${file.format}`));
}

function persistCartDownloadCheckpoint(checkpoint: CartDownloadCheckpoint) {
  checkpoint.updatedAt = Date.now();
  cartDownloadCheckpoint.value = checkpoint;
  cartDownloadFailedDatasetIds.value = [...checkpoint.failedSampleIds];
  try {
    window.sessionStorage.setItem(CART_DOWNLOAD_CHECKPOINT_KEY, JSON.stringify(checkpoint));
  } catch (error) {
    console.warn("[Download cart] Unable to persist download checkpoint", error);
  }
}

function clearCartDownloadCheckpoint() {
  cartDownloadCheckpoint.value = null;
  cartDownloadFailedDatasetIds.value = [];
  try {
    window.sessionStorage.removeItem(CART_DOWNLOAD_CHECKPOINT_KEY);
  } catch (error) {
    console.warn("[Download cart] Unable to clear download checkpoint", error);
  }
}

function restoreCartDownloadCheckpoint() {
  try {
    const stored = window.sessionStorage.getItem(CART_DOWNLOAD_CHECKPOINT_KEY);
    if (!stored) return;
    const parsed = JSON.parse(stored) as Partial<CartDownloadCheckpoint>;
    if (
      (parsed.version !== 1 && parsed.version !== 2)
      || typeof parsed.domain !== "string"
      || typeof parsed.type !== "string"
      || typeof parsed.format !== "string"
      || !Array.isArray(parsed.sampleIds)
      || !Array.isArray(parsed.pendingSampleIds)
      || !Array.isArray(parsed.failedSampleIds)
    ) {
      window.sessionStorage.removeItem(CART_DOWNLOAD_CHECKPOINT_KEY);
      return;
    }
    const checkpoint = parsed as CartDownloadCheckpoint;
    cartDownloadCheckpoint.value = checkpoint;
    cartDownloadFailedDatasetIds.value = [...checkpoint.failedSampleIds];
  } catch (error) {
    console.warn("[Download cart] Unable to restore download checkpoint", error);
    clearCartDownloadCheckpoint();
  }
}

function createCartDownloadCheckpoint(
  sampleIds: string[],
  domain: string,
  type: string,
  format: string,
  authorization?: BulkDownloadAuthorization
): CartDownloadCheckpoint {
  const uniqueSampleIds = [...new Set(sampleIds.filter(Boolean))];
  return {
    version: 2,
    domain,
    type,
    format,
    sampleIds: uniqueSampleIds,
    pendingSampleIds: [...uniqueSampleIds],
    failedSampleIds: [],
    updatedAt: Date.now(),
    bulkAuthorizationToken: authorization?.token,
    bulkAuthorizationExpiresAt: authorization?.expiresAt,
  };
}

async function triggerCartBatchDownload(domain: string, ch: Pick<DownloadTypeNode, "type">, requestedFile: Pick<DownloadFile, "format">) {
  if (cartDownloadActive.value) return;
  if (cartDownloadCheckpoint.value) {
    ElMessage.warning("Resume, retry, or discard the saved download queue before starting a new one.");
    return;
  }
  const request: PendingCartDownloadRequest = {
    sampleIds: selected.value.map(sample => sample.datasetId),
    domain,
    type: ch.type,
    format: requestedFile.format,
  };
  if (!request.sampleIds.length) return;
  if (request.sampleIds.length > HUMAN_VERIFICATION_SAMPLE_THRESHOLD) {
    openHumanVerification(request);
    return;
  }
  await startCartDownloadRequest(request);
}

async function openHumanVerification(
  request: PendingCartDownloadRequest,
  checkpoint: CartDownloadCheckpoint | null = null
) {
  pendingCartDownloadRequest.value = request;
  pendingCartDownloadCheckpoint.value = checkpoint;
  humanVerificationChallengeId.value = "";
  humanVerificationLeft.value = 0;
  humanVerificationRight.value = 0;
  humanVerificationAnswer.value = "";
  humanVerificationError.value = "";
  humanVerificationOpen.value = true;
  humanVerificationBusy.value = true;
  try {
    const response = await fetch(buildApiUrl("api/download/cart/challenge"), {
      credentials: "same-origin",
      cache: "no-store",
    });
    if (!response.ok) throw new Error(`Verification service returned HTTP ${response.status}`);
    const challenge = await response.json() as {
      challengeId?: string;
      left?: number;
      right?: number;
    };
    if (!challenge.challengeId || !Number.isFinite(challenge.left) || !Number.isFinite(challenge.right)) {
      throw new Error("Verification service returned an invalid challenge");
    }
    humanVerificationChallengeId.value = challenge.challengeId;
    humanVerificationLeft.value = Number(challenge.left);
    humanVerificationRight.value = Number(challenge.right);
  } catch (error: any) {
    humanVerificationOpen.value = false;
    ElMessage.error(error?.message || "Unable to start human verification.");
  } finally {
    humanVerificationBusy.value = false;
  }
}

function resetHumanVerification() {
  pendingCartDownloadRequest.value = null;
  pendingCartDownloadCheckpoint.value = null;
  humanVerificationChallengeId.value = "";
  humanVerificationLeft.value = 0;
  humanVerificationRight.value = 0;
  humanVerificationAnswer.value = "";
  humanVerificationError.value = "";
  humanVerificationBusy.value = false;
}

async function confirmHumanVerification() {
  const request = pendingCartDownloadRequest.value;
  const challengeId = humanVerificationChallengeId.value;
  if (!request || !challengeId || humanVerificationBusy.value) return;
  const answer = Number(humanVerificationAnswer.value.trim());
  if (!Number.isInteger(answer)) {
    humanVerificationError.value = "Enter the numeric answer.";
    return;
  }
  humanVerificationBusy.value = true;
  try {
    const response = await fetch(buildApiUrl("api/download/cart/authorize"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        challengeId,
        answer,
        sampleIds: request.sampleIds,
        domain: request.domain,
        type: request.type,
        format: request.format,
      }),
    });
    if (!response.ok) {
      humanVerificationError.value = response.status === 400
        ? "Incorrect or expired verification. Please try again."
        : `Verification failed with HTTP ${response.status}.`;
      return;
    }
    const result = await response.json() as Partial<BulkDownloadAuthorization>;
    if (!result.token || !Number.isFinite(result.expiresAt)) {
      throw new Error("Verification service returned an invalid authorization");
    }
    const authorization: BulkDownloadAuthorization = {
      token: result.token,
      expiresAt: Number(result.expiresAt),
    };
    const checkpoint = pendingCartDownloadCheckpoint.value;
    pendingCartDownloadRequest.value = null;
    pendingCartDownloadCheckpoint.value = null;
    humanVerificationOpen.value = false;
    if (checkpoint) {
      checkpoint.version = 2;
      checkpoint.bulkAuthorizationToken = authorization.token;
      checkpoint.bulkAuthorizationExpiresAt = authorization.expiresAt;
      persistCartDownloadCheckpoint(checkpoint);
      await runCartDownloadCheckpoint(checkpoint);
    } else {
      await startCartDownloadRequest(request, authorization);
    }
  } catch (error: any) {
    humanVerificationError.value = error?.message || "Verification failed. Please retry.";
  } finally {
    humanVerificationBusy.value = false;
  }
}

function hasValidBulkAuthorization(checkpoint: CartDownloadCheckpoint) {
  return Boolean(
    checkpoint.bulkAuthorizationToken
    && checkpoint.bulkAuthorizationExpiresAt
    && checkpoint.bulkAuthorizationExpiresAt > Date.now() + 5_000
  );
}

function requeueRecoverableCartDownloads(checkpoint: CartDownloadCheckpoint) {
  const recoverableIds = new Set([
    ...checkpoint.failedSampleIds,
    ...checkpoint.pendingSampleIds,
  ]);
  checkpoint.pendingSampleIds = checkpoint.sampleIds.filter(sampleId => recoverableIds.has(sampleId));
  checkpoint.failedSampleIds = [];
  persistCartDownloadCheckpoint(checkpoint);
}

async function startCartDownloadRequest(
  request: PendingCartDownloadRequest,
  authorization?: BulkDownloadAuthorization
) {
  if (cartDownloadActive.value) return;
  if (cartDownloadCheckpoint.value) {
    ElMessage.warning("A saved download queue already exists.");
    return;
  }
  const checkpoint = createCartDownloadCheckpoint(
    request.sampleIds,
    request.domain,
    request.type,
    request.format,
    authorization
  );
  if (!checkpoint.sampleIds.length) return;
  if (checkpoint.sampleIds.length > HUMAN_VERIFICATION_SAMPLE_THRESHOLD && !hasValidBulkAuthorization(checkpoint)) {
    await openHumanVerification(request);
    return;
  }
  persistCartDownloadCheckpoint(checkpoint);
  await runCartDownloadCheckpoint(checkpoint);
}

async function resumeCartDownload() {
  const checkpoint = cartDownloadCheckpoint.value;
  if (!checkpoint || !checkpoint.pendingSampleIds.length || cartDownloadActive.value) return;
  requeueRecoverableCartDownloads(checkpoint);
  if (checkpoint.sampleIds.length > HUMAN_VERIFICATION_SAMPLE_THRESHOLD && !hasValidBulkAuthorization(checkpoint)) {
    await openHumanVerification({
      sampleIds: checkpoint.sampleIds,
      domain: checkpoint.domain,
      type: checkpoint.type,
      format: checkpoint.format,
    }, checkpoint);
    return;
  }
  await runCartDownloadCheckpoint(checkpoint);
}

function pauseCartDownload() {
  if (!cartDownloadActive.value) return;
  cartDownloadPauseRequested = true;
  cartDownloadAbortController?.abort();
}

function handleDownloadPageExit() {
  pageUnloading = true;
  if (cartDownloadCheckpoint.value) persistCartDownloadCheckpoint(cartDownloadCheckpoint.value);
  cartDownloadAbortController?.abort();
}

async function retryFailedCartDownloads() {
  const checkpoint = cartDownloadCheckpoint.value;
  if (!checkpoint || !checkpoint.failedSampleIds.length || cartDownloadActive.value) return;
  const retryCheckpoint = createCartDownloadCheckpoint(
    checkpoint.failedSampleIds,
    checkpoint.domain,
    checkpoint.type,
    checkpoint.format,
    hasValidBulkAuthorization(checkpoint)
      ? {
          token: checkpoint.bulkAuthorizationToken!,
          expiresAt: checkpoint.bulkAuthorizationExpiresAt!,
        }
      : undefined
  );
  if (retryCheckpoint.sampleIds.length > HUMAN_VERIFICATION_SAMPLE_THRESHOLD && !hasValidBulkAuthorization(retryCheckpoint)) {
    await openHumanVerification({
      sampleIds: retryCheckpoint.sampleIds,
      domain: retryCheckpoint.domain,
      type: retryCheckpoint.type,
      format: retryCheckpoint.format,
    });
    return;
  }
  persistCartDownloadCheckpoint(retryCheckpoint);
  await runCartDownloadCheckpoint(retryCheckpoint);
}

function discardCartDownloadCheckpoint() {
  if (cartDownloadActive.value) return;
  clearCartDownloadCheckpoint();
  selected.value = [];
  downloadTableRef.value?.clearSelection();
  cartDownloadCurrent.value = 0;
  cartDownloadProcessed.value = 0;
  cartDownloadTotal.value = 0;
  cartDownloadDataset.value = "";
  cartDownloadErrors.value = 0;
  cartDownloadKey.value = "";
  cartDownloadBatch.value = 0;
  cartDownloadBatchTotal.value = 0;
  cartDownloadWaitingNextBatch.value = false;
  cartDownloadPauseRequested = false;
  cartOpen.value = false;
}

async function runCartDownloadCheckpoint(checkpoint: CartDownloadCheckpoint) {
  if (cartDownloadActive.value || !checkpoint.pendingSampleIds.length) return;
  pageUnloading = false;
  cartDownloadPauseRequested = false;
  const total = checkpoint.sampleIds.length;
  const processedBeforeResume = total - checkpoint.pendingSampleIds.length;
  cartDownloadActive.value = true;
  cartDownloadCurrent.value = processedBeforeResume;
  cartDownloadProcessed.value = processedBeforeResume;
  cartDownloadTotal.value = total;
  cartDownloadDataset.value = "";
  cartDownloadErrors.value = checkpoint.failedSampleIds.length;
  cartDownloadKey.value = cartFileKey(checkpoint.domain, checkpoint.type, checkpoint.format);
  cartDownloadBatch.value = Math.min(
    Math.floor(processedBeforeResume / CART_DOWNLOAD_BATCH_SIZE) + 1,
    Math.ceil(total / CART_DOWNLOAD_BATCH_SIZE)
  );
  cartDownloadBatchTotal.value = Math.ceil(total / CART_DOWNLOAD_BATCH_SIZE);
  cartDownloadWaitingNextBatch.value = false;
  let lastRequestStartedAt = 0;

  try {
    while (checkpoint.pendingSampleIds.length > 0) {
      if (cartDownloadPauseRequested || pageUnloading) {
        persistCartDownloadCheckpoint(checkpoint);
        break;
      }
      const datasetId = checkpoint.pendingSampleIds[0];
      if (!datasetId) {
        checkpoint.pendingSampleIds.shift();
        persistCartDownloadCheckpoint(checkpoint);
        continue;
      }
      cartDownloadCurrent.value = cartDownloadProcessed.value + 1;
      cartDownloadBatch.value = Math.min(
        Math.floor(cartDownloadProcessed.value / CART_DOWNLOAD_BATCH_SIZE) + 1,
        cartDownloadBatchTotal.value
      );
      cartDownloadWaitingNextBatch.value = false;
      cartDownloadDataset.value = datasetId;
      let removePendingItem = true;
      let stopQueue = false;
      const abortController = new AbortController();
      cartDownloadAbortController = abortController;
      try {
        const requestDelay = Math.max(
          0,
          CART_DOWNLOAD_MIN_REQUEST_INTERVAL_MS - (performance.now() - lastRequestStartedAt)
        );
        if (lastRequestStartedAt > 0 && requestDelay > 0) {
          await new Promise<void>((resolve) => window.setTimeout(resolve, requestDelay));
        }
        lastRequestStartedAt = performance.now();
        const exactDomain = buildDownloads(datasetId).find(item => item.domain === checkpoint.domain);
        const exactType = exactDomain?.children.find(item => item.type === checkpoint.type);
        const file = exactType?.files.find(item => item.format === checkpoint.format);
        if (!file) throw new Error(`No ${checkpoint.domain}/${checkpoint.type}/${checkpoint.format} download is available`);
        await fetchAndSaveDownload(
          file,
          checkpoint.sampleIds.length > HUMAN_VERIFICATION_SAMPLE_THRESHOLD
            ? checkpoint.bulkAuthorizationToken
            : undefined,
          abortController.signal
        );
      } catch (error) {
        const interrupted = pageUnloading
          || cartDownloadPauseRequested
          || (error as { name?: string } | null)?.name === "AbortError";
        if (interrupted) {
          removePendingItem = false;
          stopQueue = true;
          persistCartDownloadCheckpoint(checkpoint);
          if (!pageUnloading) {
            ElMessage.info(`Download queue paused at ${datasetId}. Resume will retry this file.`);
          }
        } else if (
          checkpoint.sampleIds.length > HUMAN_VERIFICATION_SAMPLE_THRESHOLD
          && error instanceof DownloadHttpError
          && (error.status === 401 || error.status === 403)
        ) {
          checkpoint.version = 2;
          checkpoint.bulkAuthorizationToken = undefined;
          checkpoint.bulkAuthorizationExpiresAt = undefined;
          removePendingItem = false;
          persistCartDownloadCheckpoint(checkpoint);
          ElMessage.warning("Bulk download authorization expired. Verify again to continue the remaining files.");
          break;
        } else {
          if (!checkpoint.failedSampleIds.includes(datasetId)) checkpoint.failedSampleIds.push(datasetId);
          cartDownloadErrors.value = checkpoint.failedSampleIds.length;
          console.error(`[Download cart] ${datasetId}`, error);
        }
      } finally {
        if (cartDownloadAbortController === abortController) cartDownloadAbortController = null;
        if (removePendingItem) {
          checkpoint.pendingSampleIds.shift();
          cartDownloadProcessed.value += 1;
          persistCartDownloadCheckpoint(checkpoint);
        }
      }

      if (stopQueue) break;

      const completedBatch = cartDownloadProcessed.value % CART_DOWNLOAD_BATCH_SIZE === 0;
      if (completedBatch && checkpoint.pendingSampleIds.length > 0) {
        cartDownloadWaitingNextBatch.value = true;
        cartDownloadDataset.value = "";
        await new Promise<void>((resolve) => window.setTimeout(resolve, CART_DOWNLOAD_BATCH_PAUSE_MS));
      }
    }
  } finally {
    cartDownloadAbortController = null;
    cartDownloadActive.value = false;
    cartDownloadWaitingNextBatch.value = false;
    cartDownloadDataset.value = "";
    cartDownloadKey.value = "";
  }

  if (checkpoint.pendingSampleIds.length > 0) {
    persistCartDownloadCheckpoint(checkpoint);
  } else if (checkpoint.failedSampleIds.length > 0) {
    persistCartDownloadCheckpoint(checkpoint);
    ElMessage.warning(`${total - checkpoint.failedSampleIds.length} download(s) prepared; ${checkpoint.failedSampleIds.length} failed and can be retried.`);
  } else {
    clearCartDownloadCheckpoint();
    ElMessage.success(`${total} download(s) prepared.`);
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

onMounted(() => {
  restoreCartDownloadCheckpoint();
  window.addEventListener("beforeunload", handleDownloadPageExit);
  void fetchRows();
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleDownloadPageExit);
  handleDownloadPageExit();
});

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

.additional-resources-panel {
  padding: 14px 16px 16px;
  margin-top: 14px;
  border: 1px solid rgba(143, 165, 156, 0.16);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.035);
}

.additional-resources-heading {
  margin-bottom: 12px;
}

.additional-resources-title {
  color: var(--text);
  font-size: 15px;
  font-weight: 900;
}

.additional-resources-subtitle {
  margin-top: 3px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
}

.additional-resource-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}

.additional-resource-chip {
  min-height: 92px;
  box-sizing: border-box;
  align-items: stretch;
  flex-direction: column;
  justify-content: flex-start;
  padding: 14px 16px;
  border-left-width: 4px;
  color: inherit;
  text-decoration: none;
}

.additional-resource-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.additional-resource-card-head .chip-left {
  align-items: flex-start;
  flex-direction: column;
  gap: 7px;
}

.additional-resource-chip .chip-name {
  max-width: none;
  white-space: normal;
  text-align: left;
  line-height: 1.4;
}

.epi-annotation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  width: 100%;
  padding-top: 11px;
  margin-top: 12px;
  border-top: 1px solid rgba(95, 125, 112, 0.17);
}

.epi-annotation-tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 3px 8px;
  border: 1px solid rgba(95, 125, 112, 0.22);
  border-radius: 999px;
  background: rgba(95, 125, 112, 0.08);
  color: #557468;
  font-size: 10px;
  font-weight: 850;
  line-height: 1.25;
}

.additional-resource-chip--epi {
  border-color: rgba(95, 125, 112, 0.32);
  background: linear-gradient(135deg, rgba(247, 251, 249, 0.98), #fff);
}

.additional-resource-chip--score {
  min-height: 80px;
  padding-top: 10px;
  padding-bottom: 10px;
  border-color: rgba(91, 132, 166, 0.38);
  background: linear-gradient(135deg, rgba(241, 248, 253, 0.98), #fff);
}

.additional-resource-chip--score .chip-format {
  border-color: rgba(91, 132, 166, 0.28);
  background: rgba(91, 132, 166, 0.10);
  color: #486f8f;
}

.additional-resource-chip--score .chip-action {
  background: #6f96b5 !important;
  box-shadow: 0 4px 10px rgba(91, 132, 166, 0.24);
}

.additional-resource-chip--expression {
  min-height: 80px;
  padding-top: 10px;
  padding-bottom: 10px;
  border-color: rgba(186, 126, 82, 0.38);
  background: linear-gradient(135deg, rgba(255, 247, 240, 0.98), #fff);
}

.additional-resource-chip--expression .chip-format {
  border-color: rgba(186, 126, 82, 0.28);
  background: rgba(186, 126, 82, 0.10);
  color: #9a633b;
}

.additional-resource-chip--expression .chip-action {
  background: #bd845c !important;
  box-shadow: 0 4px 10px rgba(186, 126, 82, 0.24);
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
}
.cart-btn svg { flex-shrink: 0; }

.filtered-selection-wrap {
  display: inline-flex;
}

.select-all-results-btn {
  --el-button-bg-color: rgba(143, 165, 156, 0.12);
  --el-button-border-color: rgba(143, 165, 156, 0.30);
  --el-button-text-color: #4a6b5c;
  --el-button-hover-bg-color: rgba(143, 165, 156, 0.22);
  --el-button-hover-border-color: var(--brand-primary-3);
  --el-button-hover-text-color: #2d4a3e;
  width: 100%;
  font-weight: 800;
  white-space: nowrap;
}

.human-verification-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.human-verification-note {
  padding: 11px 12px;
  border: 1px solid rgba(143, 165, 156, 0.24);
  border-radius: 10px;
  background: rgba(143, 165, 156, 0.08);
  color: var(--muted);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.5;
}

.human-verification-challenge {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text);
  font-size: 14px;
  font-weight: 900;
}

.human-verification-error {
  color: #a43f3f;
  font-size: 12px;
  font-weight: 800;
}

.cart-btn-help-wrap {
  position: relative;
  display: inline-flex;
  justify-self: end;
}

.cart-btn-help-icon {
  position: absolute;
  top: -7px;
  right: -7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: var(--surface);
  color: var(--brand-primary-3);
  font-size: 9px;
  font-weight: 900;
  line-height: 1;
  cursor: help;
  z-index: 1;
}

.cart-btn-help-icon:focus-visible {
  outline: 2px solid rgba(78, 133, 118, 0.3);
  outline-offset: 2px;
}

.cart-help-copy {
  max-width: 340px;
  line-height: 1.55;
}

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
  grid-template-columns: 64px 140px minmax(0, 1fr) 78px auto auto;
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

.select-field-help-wrap {
  position: relative;
  display: inline-flex;
  width: 100%;
}

.select-field-help-icon {
  top: -7px;
  right: -7px;
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

:deep(.tbl th.el-table__cell.is-sortable .cell) {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

:deep(.tbl th.el-table__cell.is-sortable .caret-wrapper) {
  flex: 0 0 auto;
  margin-left: 8px;
}

.browse-column-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  min-width: 0;
  line-height: 1.2;
}

.column-help-icon {
  flex: 0 0 auto;
  color: var(--muted);
  cursor: help;
  font-size: 13px;
  transition: color 0.16s ease;
}

.column-help-icon:hover {
  color: var(--brand-primary-3);
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
  .additional-resource-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .dl-stat-bar { flex-wrap: wrap; font-size: 14px; }
  .dl-card { padding: 10px; }
  .additional-resource-card-head { flex-direction: column; }
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
.cart-recovery-card,
.cart-failed-card{
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid rgba(143, 165, 156, .28);
  border-radius: 12px;
  background: rgba(143, 165, 156, .08);
}
.cart-recovery-copy{
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.45;
}
.cart-recovery-copy strong,
.cart-failed-card strong{
  color: var(--text);
  font-size: 13px;
  font-weight: 900;
}
.cart-recovery-actions{
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
}
.cart-failed-card{
  align-items: flex-start;
  flex-direction: column;
  background: rgba(186, 126, 82, .08);
  border-color: rgba(186, 126, 82, .26);
}
.cart-failed-card span{
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
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
  .cart-recovery-card{ align-items: stretch; flex-direction: column; }
}
</style>

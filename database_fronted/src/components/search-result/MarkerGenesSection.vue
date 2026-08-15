<template>
  <section class="annotation-section float-card" :aria-busy="downloading">
    <div class="annotation-content" :inert="downloading" :aria-hidden="downloading">
    <div class="section-head">
      <div class="section-heading">
        <div class="section-title">Regulatory annotation</div>
        <div class="section-sub">{{ sectionDescription }}</div>
      </div>

      <div class="annotation-actions">
        <div ref="annTabsRef" class="annotation-tabs" role="tablist" aria-label="Regulatory annotation view">
          <div class="annotation-tab-slider" :style="annSliderStyle" />
          <button
            v-for="(tab, idx) in availableTabs"
            :key="tab.value"
            :ref="(el) => { if (el) annBtnRefs[idx] = el as HTMLElement }"
            type="button"
            class="annotation-tab"
            :class="{ active: activeAnnotationType === tab.value }"
            :aria-selected="activeAnnotationType === tab.value"
            :disabled="downloading"
            role="tab"
            @click="setAnnotationType(tab.value)"
          >
            {{ tab.value === 'marker_gene' && activeAnnotationType === 'marker_gene' ? markerGeneSubtypeLabel : tab.label }}
          </button>
          <el-dropdown
            v-if="activeAnnotationType === 'marker_gene' && domain === 'integration'"
            trigger="click"
            class="marker-subtype-dropdown"
            @command="onMarkerGeneSubtypeChange"
          >
            <span class="marker-subtype-trigger">{{ markerGeneSubtypeLabel }} ▾</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="gene_exp">Gene expression markers</el-dropdown-item>
                <el-dropdown-item command="gene_score">Gene score markers</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown
            v-if="activeAnnotationType === 'linked_region'"
            trigger="click"
            class="marker-subtype-dropdown"
            @command="onP2gModeChange"
          >
            <span class="marker-subtype-trigger">{{ p2gModeLabel }} ▾</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="marker">Marker (both sides)</el-dropdown-item>
                <el-dropdown-item command="all">All P2G links</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-tooltip content="Download every record matching the current filters" placement="top" effect="light" :show-after="220">
          <span class="annotation-download-tooltip">
            <button
              type="button"
              class="annotation-download-button"
              :disabled="csvDisabled || loading || downloading"
              aria-label="Download every record matching the current filters"
              @click="downloadCsv"
            >
              <el-icon><Download /></el-icon>
            </button>
          </span>
        </el-tooltip>
      </div>

      <div class="section-tags">
        <span class="data-chip" :title="domainTitle">{{ domainChip }}</span>
        <span class="data-chip mono" :title="datasetId">{{ datasetId }}</span>
      </div>
    </div>

      <div class="query-panel" :class="{ 'query-panel--p2g': activeAnnotationType === 'linked_region' }">
        <div v-if="activeAnnotationType === 'linked_region'" class="control-field control-field--grow control-field--p2g-search">
          <div class="control-label-row">
            <span class="control-label">{{ p2gSearchConfig.label }}</span>
            <button
              type="button"
              class="p2g-mode-switch"
              :aria-label="p2gSearchConfig.switchLabel"
              :title="p2gSearchConfig.switchLabel"
              @click="toggleP2gSearchMode"
            >
              <el-icon><Switch /></el-icon>
              <span>{{ p2gSearchConfig.switchText }}</span>
            </button>
          </div>
          <el-input
            v-model="p2gSearchQuery"
            class="control-input"
            :placeholder="p2gSearchConfig.placeholder"
            clearable
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </div>

        <label v-if="showTargetGeneFilter" class="control-field control-field--grow">
          <span class="control-label">{{ activeSearchConfig.label }}</span>
          <el-input
            v-model="targetGeneQuery"
            class="control-input"
            :placeholder="activeSearchConfig.placeholder"
            clearable
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </label>

        <div v-if="activeAnnotationType === 'marker_peak'" class="control-field control-field--grow">
          <div class="control-label-row">
            <span class="control-label">{{ markerPeakSearchConfig.label }}</span>
            <button
              type="button"
              class="p2g-mode-switch"
              :aria-label="markerPeakSearchConfig.switchLabel"
              :title="markerPeakSearchConfig.switchLabel"
              @click="toggleMarkerPeakSearchMode"
            >
              <el-icon><Switch /></el-icon>
              <span>{{ markerPeakSearchConfig.switchText }}</span>
            </button>
          </div>
          <el-input
            v-model="markerPeakSearchQuery"
            class="control-input"
            :placeholder="markerPeakSearchConfig.placeholder"
            clearable
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </div>

        <label v-if="showContextFilter" class="control-field control-field--context">
          <span class="control-label">{{ contextFieldLabel }}</span>
          <el-select
            v-model="context"
            class="control-input oscar-pill-select"
            popper-class="oscar-select-popper"
            size="small"
            filterable
            clearable
            :loading="contextOptionsLoading"
            placeholder="All"
            @change="searchAnnotations"
          >
            <el-option
              v-for="option in contextOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>

        <label v-if="showMaxFdrFilter" class="control-field control-field--compact">
          <span class="control-label">Max FDR</span>
          <el-input
            v-model="maxFdr"
            class="control-input"
            placeholder="0.05"
            clearable
            inputmode="decimal"
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </label>

        <label v-if="showMinLog2fcFilter" class="control-field control-field--compact">
          <span class="control-label">Min Log2FC</span>
          <el-input
            v-model="minLog2fc"
            class="control-input"
            placeholder="0.25"
            clearable
            inputmode="decimal"
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </label>

        <label v-if="showMinP2gScoreFilter" class="control-field control-field--compact control-field--p2g-score">
          <span class="control-label">Min P2G score</span>
          <el-input
            v-model="minP2gScore"
            class="control-input"
            placeholder="0.25"
            clearable
            inputmode="decimal"
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </label>

        <el-button type="primary" class="section-button" :loading="loading" @click="searchAnnotations">
          <el-icon><Search /></el-icon>
          <span>Search</span>
        </el-button>
        <el-button class="soft-button" @click="resetAnnotations">
          <el-icon><Refresh /></el-icon>
          <span>Reset</span>
        </el-button>
      </div>

      <el-skeleton v-if="loading && records.length === 0 && !hasLoaded" animated :rows="6" />

      <template v-else>
        <el-table
          v-if="records.length > 0"
          v-loading="loading"
          :element-loading-text="downloading ? 'This download may take a while, please be patient...' : ''"
          :data="records"
          stripe
          border
          class="detail-table"
        >
        <el-table-column
          v-for="column in activeColumns"
          :key="column.key"
          :prop="column.key"
          :label="column.label"
          :min-width="column.minWidth"
          :align="column.align"
          :show-overflow-tooltip="column.overflowTooltip"
          :class-name="column.key === 'context' ? 'annotation-context-column' : undefined"
        >
          <template #header>
            <span class="column-header">
              <span>{{ column.label }}</span>
              <el-tooltip
                v-if="column.headerTooltip"
                placement="top"
                effect="light"
                :show-after="180"
              >
                <template #content>
                  <div v-if="column.kind === 'signal-type'">
                    <div class="signal-legend">
                      <span class="signal-dot signal-dot--exp" /> Gene expression marker
                      &nbsp;&nbsp;
                      <span class="signal-dot signal-dot--score" /> Gene activity score marker
                    </div>
                    <div>Shows which marker-gene measurement supplied the gene values in this row.</div>
                  </div>
                  <div v-else><div v-for="line in column.headerTooltip" :key="line">{{ line }}</div></div>
                </template>
                <el-icon class="column-help-icon"><InfoFilled /></el-icon>
              </el-tooltip>
              <button
                v-if="column.sortable"
                type="button"
                class="annotation-sort-toggle"
                :class="{
                  'is-active': sortBy === column.key && sortOrder,
                  'is-ascending': sortBy === column.key && sortOrder === 'asc',
                  'is-descending': sortBy === column.key && sortOrder === 'desc',
                }"
                :aria-label="sortButtonLabel(column)"
                :aria-pressed="sortBy === column.key && Boolean(sortOrder)"
                @click.stop="cycleTableSort(column)"
              >
                <span class="annotation-sort-arrow annotation-sort-arrow--up" aria-hidden="true" />
                <span class="annotation-sort-arrow annotation-sort-arrow--down" aria-hidden="true" />
              </button>
            </span>
          </template>

          <template #default="{ row }">
            <template v-if="column.kind === 'gene-link'">
              <el-popover
                v-if="hasGeneDetail(row)"
                trigger="click"
                placement="right-start"
                :width="380"
                popper-class="annotation-detail-popper"
              >
                <template #reference>
                  <button type="button" class="link-button gene-link">{{ geneLabel(row) }}</button>
                </template>
                <div class="detail-card">
                  <div class="detail-card-head detail-card-head--gene">
                    <div class="detail-card-head-row">
                      <div class="detail-kicker">Gene detail</div>
                      <button type="button" class="gene-detail-btn" @click="goToGeneDetail(row)">View gene detail</button>
                    </div>
                    <div class="detail-title">{{ geneLabel(row) }}</div>
                    <div class="detail-pills">
                      <span v-if="formatCellTypeCluster(row) !== '-'" class="detail-pill">{{ formatCellTypeCluster(row) }}</span>
                      <span class="detail-pill">{{ formatDisplaySource(row) }}</span>
                    </div>
                  </div>
                  <div class="detail-card-body">
                    <div class="detail-grid">
                      <div><span>Feature type</span><strong>Gene</strong></div>
                      <div><span>Gene symbol</span><strong>{{ geneLabel(row) }}</strong></div>
                      <div><span>{{ contextFieldLabel }}</span><strong>{{ formatCellTypeCluster(row) }}</strong></div>
                      <div><span>Gene region</span><strong>{{ geneRegionDisplay(row) }}</strong></div>
                      <div><span>Strand</span><strong>{{ displayText(row.strand) }}</strong></div>
                      <div class="detail-grid-wide"><span>Promoter region</span><strong>{{ promoterDisplay(row) }}</strong></div>
                      <div class="detail-grid-wide"><span>Dataset / Domain</span><strong>{{ sourceSampleId(row) }} / {{ sourceDomainLabel(row) }}</strong></div>
                    </div>
                    <div class="metric-row">
                      <div class="metric-card"><span>Log2FC</span><strong>{{ formatMetric(row.geneLog2fc) }}</strong></div>
                      <div class="metric-card"><span>FDR</span><strong>{{ formatMetric(row.geneFdr) }}</strong></div>
                      <div class="metric-card"><span>MeanDiff</span><strong>{{ formatMetric(row.geneMeanDiff) }}</strong></div>
                    </div>
                  </div>
                </div>
              </el-popover>
              <span v-else>{{ geneLabel(row) }}</span>
            </template>

            <template v-else-if="column.kind === 'peak-link'">
              <el-popover
                v-if="hasPeakDetail(row, column.key)"
                trigger="click"
                placement="right-start"
                :width="380"
                popper-class="annotation-detail-popper"
              >
                <template #reference>
                  <button type="button" class="link-button peak-link">{{ peakLabel(row, column.key) }}</button>
                </template>
                <div class="detail-card">
                  <div class="detail-card-head">
                    <div class="detail-card-head-row">
                      <div class="detail-kicker">Peak detail</div>
                      <button type="button" class="gene-detail-btn" @click="goToPeakDetail(row, column.key)">View peak detail</button>
                    </div>
                    <div class="detail-title">{{ peakLabel(row, column.key) }}</div>
                    <div class="detail-pills">
                      <span v-if="formatCellTypeCluster(row) !== '-'" class="detail-pill">{{ formatCellTypeCluster(row) }}</span>
                      <span class="detail-pill">{{ formatDisplaySource(row) }}</span>
                    </div>
                  </div>
                  <div class="detail-card-body">
                    <div class="detail-grid">
                      <div><span>Feature type</span><strong>Peak / Regulatory region</strong></div>
                      <div class="detail-grid-wide"><span>Location</span><strong>{{ peakLabel(row, column.key) }}</strong></div>
                      <div v-if="peakIdDisplay(row) !== '-'"><span>Peak ID</span><strong>{{ peakIdDisplay(row) }}</strong></div>
                      <div><span>{{ contextFieldLabel }}</span><strong>{{ formatCellTypeCluster(row) }}</strong></div>
                      <div><span>Linked gene</span><strong>{{ linkedGeneDisplay(row) }}</strong></div>
                      <div class="detail-grid-wide"><span>Peak coordinates</span><strong>{{ peakCoordinatesDisplay(row) }}</strong></div>
                      <div class="detail-grid-wide"><span>Dataset / Domain</span><strong>{{ sourceSampleId(row) }} / {{ sourceDomainLabel(row) }}</strong></div>
                    </div>
                    <div class="metric-row">
                      <div class="metric-card"><span>Log2FC</span><strong>{{ formatMetric(row.peakLog2fc) }}</strong></div>
                      <div class="metric-card"><span>FDR</span><strong>{{ formatMetric(row.peakFdr) }}</strong></div>
                      <div class="metric-card"><span>MeanDiff</span><strong>{{ formatMetric(row.peakMeanDiff) }}</strong></div>
                      <div v-if="linkScoreDisplay(row) !== '-'" class="metric-card"><span>{{ activeAnnotationType === "linked_region" ? "P2G score" : "Link score" }}</span><strong>{{ linkScoreDisplay(row) }}</strong></div>
                    </div>
                  </div>
                </div>
              </el-popover>
              <span v-else>{{ peakLabel(row, column.key) }}</span>
            </template>

            <span v-else-if="column.kind === 'metric'" class="metric-badge">
              {{ metricDisplay(row, column.key) }}
            </span>

            <span v-else-if="column.kind === 'gene-evidence'" class="marker-evidence-badge">
              {{ geneEvidenceDisplay(row) }}
            </span>

            <span v-else-if="column.kind === 'peak-evidence'" class="marker-evidence-badge">
              {{ peakEvidenceDisplay(row) }}
            </span>

            <span v-else-if="column.kind === 'signal-type'" class="signal-type-cell">
              <span
                v-if="row.signalType === 'gene_expression'"
                class="signal-dot signal-dot--exp"
                title="Gene expression marker — from RNA expression data"
              />
              <span
                v-else-if="row.signalType === 'gene_score'"
                class="signal-dot signal-dot--score"
                title="Gene score marker — from ATAC gene activity score"
              />
              <span v-else class="signal-type-label">-</span>
            </span>

            <span v-else-if="column.kind === 'link-score'" class="metric-badge metric-badge--strong">
              {{ linkScoreDisplay(row) }}
            </span>

            <span v-else-if="column.kind === 'tf'" class="tf-null-badge">
              {{ tfColumnDisplay(row) }}
            </span>

            <el-tooltip
              v-else-if="column.kind === 'source'"
              placement="top"
              effect="light"
            >
              <template #content>
                <div class="source-tooltip">
                  <div>Sample: {{ sourceSampleId(row) }}</div>
                  <div>Domain: {{ sourceDomainLabel(row) }}</div>
                  <div>Annotation: {{ annotationLabel(row.annotationType) }}</div>
                </div>
              </template>
              <span class="source-chip">{{ sourceDisplay(row) }}</span>
            </el-tooltip>

            <span v-else>{{ cellDisplay(row, column.key) }}</span>
          </template>
        </el-table-column>
        </el-table>

      <div v-else-if="hasLoaded" class="empty-state empty-state--stacked">
        <div class="empty-title">{{ emptyTitle }}</div>
        <div class="empty-subtitle">{{ emptySubtitle }}</div>
      </div>

      <div v-if="total > 0" class="pager">
        <el-pagination
          class="oscar-pagination"
          background
          layout="total, sizes, prev, pager, next, jumper"
          popper-class="oscar-select-popper"
          :total="total"
          :page-sizes="pageSizeOptions"
          :page-size="pageSize"
          :current-page="page"
          @size-change="onPageSizeChange"
          @current-change="onPageChange"
        />
      </div>
      </template>
    </div>

    <transition name="annotation-download-lock">
      <div v-if="downloading" class="annotation-download-lock" role="status" aria-live="polite">
        <div class="annotation-download-lock-card">
          <span class="annotation-download-lock-spinner" aria-hidden="true" />
          <div>
            <div class="annotation-download-lock-title">Downloading {{ downloadLabel }} CSV</div>
            <div class="annotation-download-lock-copy">Rows are being streamed with the current filters. The controls will unlock as soon as the file is ready.</div>
          </div>
        </div>
      </div>
    </transition>
  </section>
</template>

<script setup lang="ts">
import { Download, InfoFilled, Refresh, Search, Switch } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type {
  RegulatoryAnnotationContextOption,
  RegulatoryAnnotationRecord,
  RegulatoryAnnotationType,
  SearchResultOverviewData,
  SearchResultDomain,
} from "@/api/searchResult";
import {
  fetchRegulatoryAnnotationContextOptions,
  fetchRegulatoryAnnotations,
  fetchRegulatoryAnnotationsDownload,
  isSearchResultEndpointUnavailable,
} from "@/api/searchResult";
import {
  getRegulatoryAnnotationDownloadTask,
  runRegulatoryAnnotationDownload,
} from "@/state/regulatoryAnnotationDownloadState";
import { domainDisplayLabel } from "@/utils/searchResultDomain";

type ColumnKind =
  | "text"
  | "gene-link"
  | "peak-link"
  | "metric"
  | "gene-evidence"
  | "peak-evidence"
  | "signal-type"
  | "link-score"
  | "tf"
  | "source";

interface AnnotationColumn {
  key: string;
  label: string;
  kind: ColumnKind;
  minWidth: number;
  align?: "left" | "center" | "right";
  sortable?: boolean;
  overflowTooltip?: boolean;
  headerTooltip?: readonly string[];
}

interface AnnotationTabConfig {
  value: RegulatoryAnnotationType;
  label: string;
  description: string;
}

interface AnnotationSearchConfig {
  label: string;
  placeholder: string;
}

type P2gSearchMode = "gene" | "region";
type MarkerPeakSearchMode = "gene" | "region";

interface AnnotationTabState {
  targetGeneQuery: string;
  peakQuery: string;
  p2gSearchMode: P2gSearchMode;
  markerPeakSearchMode: MarkerPeakSearchMode;
  context: string;
  maxFdr: string;
  minLog2fc: string;
  minP2gScore: string;
  markerGeneSubtype: "gene_score" | "gene_exp";
  page: number;
  pageSize: number;
  sortBy: string;
  sortOrder: AnnotationSortOrder;
  total: number;
  records: RegulatoryAnnotationRecord[];
  hasLoaded: boolean;
  contextOptions: RegulatoryAnnotationContextOption[];
  loadedQuerySignature: string;
}

type AnnotationSortOrder = "asc" | "desc" | "";

const props = defineProps<{
  datasetId: string;
  domain: SearchResultDomain;
  overview?: SearchResultOverviewData | null;

}>();

const router = useRouter();
const route = useRoute();

const annotationTabConfig: Record<RegulatoryAnnotationType, AnnotationTabConfig> = {
  marker_gene: {
    value: "marker_gene",
    label: "Marker genes",
    description: "Cell type-associated marker genes with expression-level statistics.",
  },
  marker_peak: {
    value: "marker_peak",
    label: "Marker peaks",
    description: "Cell type-associated marker peaks with accessibility-level statistics.",
  },
  linked_region: {
    value: "linked_region",
    label: "P2G links",
    description: "Peak-to-gene links retrieved directly from the OSCAR database.",
  },
};

const COLUMN_TOOLTIPS = {
  geneRegion: ["Genomic coordinates assigned to the marker gene in the source annotation.", "The displayed interval is the gene location, not the promoter interval."],
  promoterRegion: ["The promoter interval used by OSCAR for this gene.", "It spans 2 kb upstream and 2 kb downstream of the transcription start site."],
  markerGeneLog2fc: ["Log2 fold-change reported for this marker gene in the listed cell type or cluster.", "A larger positive value means the gene is more strongly increased in that group."],
  markerGeneFdr: ["P-value after correction for testing many genes.", "A smaller value means the marker-gene result is less likely to be due to chance."],
  markerGeneMeanDiff: ["Difference between the gene's average value in the selected group and the comparison cells.", "The exact source value is kept when a dataset uses its own equivalent marker statistic."],
  markerGeneSample: ["OSCAR dataset identifier, sample name, and data type that supplied this row.", "Use the sample link to open the complete sample details."],
  markerPeakPeak: ["Chromosome, start, and end of the marker accessibility peak.", "The coordinates are the stored peak interval for the listed sample and cell type or cluster."],
  markerPeakLinkedGene: ["Gene name attached to this marker peak by the source annotation when one is available.", "This label alone does not prove a cell-type-specific regulatory link."],
  markerPeakLog2fc: ["Log2 fold-change in accessibility for this marker peak in the listed cell type or cluster.", "A larger positive value means the peak is more accessible in that group."],
  markerPeakFdr: ["P-value after correction for testing many peaks.", "A smaller value means the marker-peak result is less likely to be due to chance."],
  markerPeakMeanDiff: ["Difference between average accessibility in the selected group and the comparison cells.", "The exact source value is kept when a dataset uses its own equivalent marker statistic."],
  markerPeakSample: ["OSCAR dataset identifier, sample name, and data type that supplied this marker peak.", "Use the sample link to open the complete sample details."],
  p2gGene: ["Gene at the gene end of the stored peak-to-gene link.", "This value comes from the P2G table."],
  p2gLinkedPeak: ["Peak interval at the peak end of the stored peak-to-gene link.", "This value comes from the P2G table and can be opened in the peak details page."],
  p2gMarkerGene: ["Gene at the gene end of the stored peak-to-gene link.", "In Marker mode, this gene must also be an OSCAR marker gene in the same sample and cell type or cluster.", "Each displayed row therefore has marker support at both the gene end and the peak end; it is not selected from the P2G table alone."],
  p2gMarkerPeak: ["Peak interval at the peak end of the stored peak-to-gene link.", "In Marker mode, this peak must also be an OSCAR marker peak in the same sample and cell type or cluster.", "Each displayed row therefore has marker support at both the peak end and the gene end; it is not selected from the P2G table alone."],
  p2gScore: ["Strength assigned to this peak-to-gene link by the source P2G method.", "It describes the stored link and is not recalculated separately for each cell type."],
  p2gGeneEvidence: ["Marker-gene values for the gene end of this P2G link, including Log2FC and FDR when available.", "Their presence confirms that the linked gene is a marker in the matching sample and cell type or cluster."],
  p2gPeakEvidence: ["Marker-peak values for the peak end of this P2G link, including Log2FC and FDR when available.", "Their presence confirms that the linked peak is a marker in the same sample and cell type or cluster."],
  p2gSample: ["OSCAR dataset identifier, sample name, and data type for the stored P2G link.", "Use the sample link to open the complete sample details."],
  tf: ["Reserved for a future transcription-factor annotation.", "A blank value means this information is not available in the current data."],
} as const;

const pageSizeOptions = [10, 20, 50];
const DEFAULT_ANNOTATION_TYPE: RegulatoryAnnotationType = "marker_gene";

const activeAnnotationType = ref<RegulatoryAnnotationType>(DEFAULT_ANNOTATION_TYPE);

// ---- marker gene sub-type (gene_score / gene_exp) ----
const markerGeneSubtype = ref<"gene_score" | "gene_exp">(props.domain === "atac" ? "gene_score" : "gene_exp");
const markerGeneSubtypeLabel = computed(() =>
  markerGeneSubtype.value === "gene_score" ? "Gene score markers" : "Gene expression markers"
);
function onMarkerGeneSubtypeChange(cmd: string) {
  if (cmd === "gene_score" || cmd === "gene_exp") {
    markerGeneSubtype.value = cmd;
    page.value = 1;
    void reloadContextOptionsAndAnnotations();
  }
}

watch(() => props.domain, (d) => {
  if (d === "atac") markerGeneSubtype.value = "gene_score";
  else markerGeneSubtype.value = "gene_exp";
});

// Auto-search when user clears a search box
function watchClearToSearch(queryRef: any) {
  watch(queryRef, (newVal, oldVal) => {
    if (oldVal.trim() && !newVal.trim()) {
      if (!hasLoaded.value || loading.value) return;
      page.value = 1;
      void loadAnnotations();
    }
  });
}
// ---- tab slider animation ----
const annTabsRef = ref<HTMLElement | null>(null);
const annBtnRefs = ref<Record<number, HTMLElement>>({});
const annSliderStyle = ref<Record<string, string>>({ width: "0px", transform: "translateX(0px)" });

function updateAnnSlider() {
  const idx = availableTabs.value.findIndex((t) => t.value === activeAnnotationType.value);
  const el = annBtnRefs.value[idx];
  const container = annTabsRef.value;
  if (!el || !container) return;
  const cr = container.getBoundingClientRect();
  const br = el.getBoundingClientRect();
  annSliderStyle.value = {
    width: `${br.width}px`,
    transform: `translateX(${br.left - cr.left}px)`,
  };
}

watch(activeAnnotationType, () => nextTick(updateAnnSlider));
let annotationTabsResizeObserver: ResizeObserver | null = null;
onMounted(() => {
  nextTick(updateAnnSlider);
  if (annTabsRef.value) {
    annotationTabsResizeObserver = new ResizeObserver(updateAnnSlider);
    annotationTabsResizeObserver.observe(annTabsRef.value);
  }
});
onBeforeUnmount(() => {
  ++requestToken;
  ++contextOptionsRequestToken;
  annotationTabsResizeObserver?.disconnect();
  annotationTabsResizeObserver = null;
});
const targetGeneQuery = ref("");
const peakQuery = ref("");
const p2gSearchMode = ref<P2gSearchMode>("gene");
const markerPeakSearchMode = ref<MarkerPeakSearchMode>("gene");
const context = ref("");
const maxFdr = ref("");
const minLog2fc = ref("");
const minP2gScore = ref("0.25");
const p2gMode = ref<"marker" | "all">("marker");
const page = ref(1);
const pageSize = ref(10);
const sortBy = ref("");
const sortOrder = ref<AnnotationSortOrder>("");
const total = ref(0);
const records = ref<RegulatoryAnnotationRecord[]>([]);
const loading = ref(false);
const activeDownloadTask = computed(() => getRegulatoryAnnotationDownloadTask(props.datasetId));
const downloading = computed(() => activeDownloadTask.value !== undefined);
const downloadLabel = computed(() => activeDownloadTask.value?.label ?? "regulatory annotation");
const reloadDeferredForDownload = ref(false);
const hasLoaded = ref(false);
const loadedContextOptions = ref<RegulatoryAnnotationContextOption[]>([]);
const contextOptionsLoading = ref(false);
const annotationTabStates = new Map<RegulatoryAnnotationType, AnnotationTabState>();
const loadedQuerySignature = ref("");


let requestToken = 0;
let contextOptionsRequestToken = 0;

const domainChip = computed(() => domainDisplayLabel(props.domain));
const domainTitle = computed(() => domainChip.value);

const domainDescriptions: Record<string, Record<string, string>> = {
  integration: {
    marker_gene: "Cell type-associated marker genes with expression / gene-score statistics.",
    marker_peak: "Cell type-associated marker peaks with accessibility-level statistics.",
    linked_region: "Candidate peak-to-gene links supported by marker gene and marker peak records.",
  },
  rna: {
    marker_gene: "RNA cluster-associated marker genes with expression-level statistics.",
  },
  atac: {
    marker_gene: "Gene score markers associated with ATAC clusters.",
    marker_peak: "ATAC marker peaks with accessibility-level statistics.",
  },
};

/** Domain-aware tab availability */
const availableTabs = computed<AnnotationTabConfig[]>(() => {
  if (props.domain === "integration") return Object.values(annotationTabConfig);
  if (props.domain === "rna") return [annotationTabConfig.marker_gene];
  if (props.domain === "atac") return [annotationTabConfig.marker_gene, annotationTabConfig.marker_peak];
  return [annotationTabConfig.marker_gene];
});

const csvDisabled = computed(() => records.value.length === 0);
const activeAnnotationConfig = computed(() => annotationTabConfig[activeAnnotationType.value]);

const sectionDescription = computed(() => {
  if (activeAnnotationType.value === "linked_region") {
    return p2gMode.value === "marker"
      ? "P2G links whose gene end and peak end are both OSCAR markers in the same sample and cell type or cluster."
      : "All stored peak-to-gene links for this sample, without requiring marker support at either end.";
  }
  return domainDescriptions[props.domain]?.[activeAnnotationType.value]
      ?? activeAnnotationConfig.value.description;
});
const contextFieldLabel = computed(() =>
  props.domain === "integration" ? "Cell type / Cluster" : "Cluster"
);
const contextColumnLabel = computed(() =>
  props.domain === "integration" ? "Cell / Cluster" : "Cluster"
);
const activeSearchConfig = computed<AnnotationSearchConfig>(() => {
  return {
    label: "Gene search",
    placeholder: "Exact gene symbol, e.g. CD22",
  };
});
const markerPeakSearchQuery = computed({
  get: () => markerPeakSearchMode.value === "gene" ? targetGeneQuery.value : peakQuery.value,
  set: (value: string) => {
    if (markerPeakSearchMode.value === "gene") targetGeneQuery.value = value;
    else peakQuery.value = value;
  },
});
const markerPeakSearchConfig = computed(() => markerPeakSearchMode.value === "gene"
  ? {
      label: "Linked gene search",
      placeholder: "Exact linked gene, e.g. CD22",
      switchText: "Peak / region search",
      switchLabel: "Switch to peak or overlapping region search",
    }
  : {
      label: "Peak / region search",
      placeholder: "Exact peak or overlapping region, e.g. chr1:10000-20000",
      switchText: "Linked gene search",
      switchLabel: "Switch to linked gene search",
    });
const p2gSearchQuery = computed({
  get: () => p2gSearchMode.value === "gene" ? targetGeneQuery.value : peakQuery.value,
  set: (value: string) => {
    if (p2gSearchMode.value === "gene") targetGeneQuery.value = value;
    else peakQuery.value = value;
  },
});
watchClearToSearch(targetGeneQuery);
watchClearToSearch(p2gSearchQuery);
watchClearToSearch(markerPeakSearchQuery);

const p2gSearchConfig = computed(() => p2gSearchMode.value === "gene"
  ? {
      label: "Target gene search",
      placeholder: "Exact target gene, e.g. CD22",
      switchText: "Region search",
      switchLabel: "Switch to region search",
    }
  : {
      label: "Region search",
      placeholder: "Exact peak or overlapping region, e.g. chr1:10000-20000",
      switchText: "Target gene search",
      switchLabel: "Switch to target gene search",
    });
const showTargetGeneFilter = computed(() =>
  activeAnnotationType.value === "marker_gene"
);
const showContextFilter = computed(() =>
  activeAnnotationType.value === "marker_gene" ||
  activeAnnotationType.value === "marker_peak" ||
  activeAnnotationType.value === "linked_region"
);
const showMaxFdrFilter = computed(() => activeAnnotationType.value !== "linked_region");
const showMinLog2fcFilter = computed(() => activeAnnotationType.value !== "linked_region");
const showMinP2gScoreFilter = computed(() => activeAnnotationType.value === "linked_region");

const contextOptions = computed(() => loadedContextOptions.value);

const activeColumns = computed<AnnotationColumn[]>(() => getActiveColumns(activeAnnotationType.value));

const emptyTitle = computed(() => {
  if (activeAnnotationType.value === "linked_region") {
    return "No P2G links match the current filters.";
  }
  if (activeAnnotationType.value === "marker_gene") return "No marker genes match the current filters.";
  if (activeAnnotationType.value === "marker_peak") return "No marker peaks match the current filters.";
  return "No regulatory annotations match the current filters.";
});

const emptySubtitle = computed(() => {
  const contextHint = props.domain === "integration" ? "cell type / cluster" : "cluster";
  if (activeAnnotationType.value === "linked_region") {
    return p2gSearchMode.value === "gene"
      ? `Try another target gene, ${contextHint}, or P2G score threshold.`
      : `Try another peak or overlapping region, ${contextHint}, or P2G score threshold.`;
  }
  if (activeAnnotationType.value === "marker_gene") {
    return "Marker genes will appear here when available.";
  }
  if (activeAnnotationType.value === "marker_peak") {
    return "Marker peaks will appear here when available.";
  }
  return `Try another target gene, peak, ${contextHint}, or marker-value threshold.`;
});

function getActiveColumns(annotationType: RegulatoryAnnotationType): AnnotationColumn[] {
  if (annotationType === "marker_gene") {
    return [
      { key: "targetGene", label: "Gene", kind: "gene-link", minWidth: 140, sortable: true, overflowTooltip: true },
      { key: "context", label: contextColumnLabel.value, kind: "text", minWidth: 170, sortable: true },
      {
        key: "geneRegion",
        label: "Gene region",
        kind: "text",
        minWidth: 190,
        sortable: true,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.geneRegion,
      },
      {
        key: "promoterRegion",
        label: "Promoter region",
        kind: "text",
        minWidth: 210,
        sortable: true,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.promoterRegion,
      },
      { key: "strand", label: "Strand", kind: "text", minWidth: 90, align: "center" },
      {
        key: "geneLog2fc",
        label: "Log2FC",
        kind: "metric", sortable: true,
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerGeneLog2fc,
      },
      {
        key: "geneFdr",
        label: "FDR",
        kind: "metric", sortable: true,
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerGeneFdr,
      },
      {
        key: "geneMeanDiff",
        label: "MeanDiff",
        kind: "metric", sortable: true,
        minWidth: 110,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerGeneMeanDiff,
      },
      {
        key: "source",
        label: "Sample",
        kind: "source",
        minWidth: 170,
        headerTooltip: COLUMN_TOOLTIPS.markerGeneSample,
      },
    ];
  }

  if (annotationType === "marker_peak") {
    return [
      {
        key: "peakRegion",
        label: "Peak",
        kind: "peak-link",
        minWidth: 230,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.markerPeakPeak,
      },
      { key: "context", label: contextColumnLabel.value, kind: "text", minWidth: 170 },
      {
        key: "linkedGene",
        label: "Linked gene",
        kind: "text",
        minWidth: 140,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.markerPeakLinkedGene,
      },
      {
        key: "peakLog2fc",
        label: "Log2FC",
        kind: "metric", sortable: true,
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerPeakLog2fc,
      },
      {
        key: "peakFdr",
        label: "FDR",
        kind: "metric", sortable: true,
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerPeakFdr,
      },
      {
        key: "peakMeanDiff",
        label: "MeanDiff",
        kind: "metric", sortable: true,
        minWidth: 110,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerPeakMeanDiff,
      },
      {
        key: "source",
        label: "Sample",
        kind: "source",
        minWidth: 170,
        headerTooltip: COLUMN_TOOLTIPS.markerPeakSample,
      },
    ];
  }

  if (annotationType === "linked_region") {
    if (p2gMode.value === "all") {
      return [
        {
          key: "targetGene",
          label: "Gene",
          kind: "gene-link",
          minWidth: 140,
          overflowTooltip: true,
          headerTooltip: COLUMN_TOOLTIPS.p2gGene,
        },
        {
          key: "linkedRegion",
          label: "Linked peak",
          kind: "peak-link",
          minWidth: 230,
          overflowTooltip: true,
          headerTooltip: COLUMN_TOOLTIPS.p2gLinkedPeak,
        },
        {
          key: "linkScore",
          label: "P2G score",
          kind: "link-score", sortable: true,
          minWidth: 120,
          align: "center",
          headerTooltip: COLUMN_TOOLTIPS.p2gScore,
        },
        {
          key: "linkFdr",
          label: "FDR",
          kind: "metric",
          minWidth: 100,
          align: "center",
          headerTooltip: ["P-value for the stored peak-to-gene link after correction for multiple tests.", "A smaller value means the link is less likely to be due to chance."],
        },
        {
          key: "varQrna",
          label: "VarQ RNA",
          kind: "metric",
          minWidth: 110,
          align: "center",
          headerTooltip: ["Corrected variance value for the RNA part of the P2G calculation.", "A smaller value indicates stronger support from RNA variation."],
        },
        {
          key: "varQatac",
          label: "VarQ ATAC",
          kind: "metric",
          minWidth: 110,
          align: "center",
          headerTooltip: ["Corrected variance value for the ATAC part of the P2G calculation.", "A smaller value indicates stronger support from accessibility variation."],
        },
        {
          key: "source",
          label: "Sample",
          kind: "source",
          minWidth: 170,
          headerTooltip: COLUMN_TOOLTIPS.p2gSample,
        },
      ];
    }
    return [
      {
        key: "targetGene",
        label: "Gene",
        kind: "gene-link",
        minWidth: 140,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.p2gMarkerGene,
      },
      { key: "context", label: contextColumnLabel.value, kind: "text", minWidth: 170 },
      {
        key: "linkedRegion",
        label: "Linked peak",
        kind: "peak-link",
        minWidth: 230,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.p2gMarkerPeak,
      },
      {
        key: "linkScore",
        label: "P2G score",
        kind: "link-score", sortable: true,
        minWidth: 120,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.p2gScore,
      },
      {
        key: "geneEvidence",
        label: "Gene Diff",
        kind: "gene-evidence",
        minWidth: 150, sortable: true,
        headerTooltip: COLUMN_TOOLTIPS.p2gGeneEvidence,
      },
      {
        key: "peakEvidence",
        label: "Peak Diff",
        kind: "peak-evidence",
        minWidth: 150, sortable: true,
        headerTooltip: COLUMN_TOOLTIPS.p2gPeakEvidence,
      },
      {
        key: "signalType",
        label: "Gene marker type",
        kind: "signal-type",
        minWidth: 120,
        align: "center",
        headerTooltip: ["Shows which marker-gene measurement supplied the gene values in this row.", "Gene expression uses RNA values; gene activity score uses ATAC-derived gene activity values."],
      },
      {
        key: "source",
        label: "Sample",
        kind: "source",
        minWidth: 170,
        headerTooltip: COLUMN_TOOLTIPS.p2gSample,
      },
    ];
  }

  return [];
}

function setAnnotationType(nextType: RegulatoryAnnotationType) {
  if (downloading.value) return;
  if (activeAnnotationType.value === nextType) return;

  saveActiveAnnotationTabState();
  ++requestToken;
  ++contextOptionsRequestToken;
  activeAnnotationType.value = nextType;

  const savedState = annotationTabStates.get(nextType);
  if (savedState) {
    restoreAnnotationTabState(savedState);
    return;
  }

  resetAnnotationTabState();
  void reloadContextOptionsAndAnnotations();
}

function saveActiveAnnotationTabState() {
  if (!hasLoaded.value || currentAnnotationQuerySignature() !== loadedQuerySignature.value) {
    annotationTabStates.delete(activeAnnotationType.value);
    return;
  }

  annotationTabStates.set(activeAnnotationType.value, {
    targetGeneQuery: targetGeneQuery.value,
    peakQuery: peakQuery.value,
    p2gSearchMode: p2gSearchMode.value,
    markerPeakSearchMode: markerPeakSearchMode.value,
    context: context.value,
    maxFdr: maxFdr.value,
    minLog2fc: minLog2fc.value,
    minP2gScore: minP2gScore.value,
    markerGeneSubtype: markerGeneSubtype.value,
    page: page.value,
    pageSize: pageSize.value,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
    total: total.value,
    records: [...records.value],
    hasLoaded: hasLoaded.value,
    contextOptions: [...loadedContextOptions.value],
    loadedQuerySignature: loadedQuerySignature.value,
  });
}

function restoreAnnotationTabState(state: AnnotationTabState) {
  targetGeneQuery.value = state.targetGeneQuery;
  peakQuery.value = state.peakQuery;
  p2gSearchMode.value = state.p2gSearchMode;
  markerPeakSearchMode.value = state.markerPeakSearchMode;
  context.value = state.context;
  maxFdr.value = state.maxFdr;
  minLog2fc.value = state.minLog2fc;
  minP2gScore.value = state.minP2gScore;
  markerGeneSubtype.value = state.markerGeneSubtype;
  page.value = state.page;
  pageSize.value = state.pageSize;
  sortBy.value = state.sortBy;
  sortOrder.value = state.sortOrder;
  total.value = state.total;
  records.value = [...state.records];
  hasLoaded.value = state.hasLoaded;
  loadedContextOptions.value = [...state.contextOptions];
  loadedQuerySignature.value = state.loadedQuerySignature;
  loading.value = false;
  contextOptionsLoading.value = false;
}

function resetAnnotationTabState() {
  targetGeneQuery.value = "";
  peakQuery.value = "";
  p2gSearchMode.value = "gene";
  markerPeakSearchMode.value = "gene";
  context.value = "";
  maxFdr.value = "";
  minLog2fc.value = "";
  minP2gScore.value = "0.25";
  page.value = 1;
  pageSize.value = 10;
  sortBy.value = "";
  sortOrder.value = "";
  total.value = 0;
  records.value = [];
  hasLoaded.value = false;
  loadedContextOptions.value = [];
  loadedQuerySignature.value = "";
  loading.value = false;
  contextOptionsLoading.value = false;
}

const p2gModeLabel = computed(() => p2gMode.value === "marker" ? "Marker" : "All links");

function onP2gModeChange(mode: string) {
  if (downloading.value) return;
  p2gMode.value = mode as "marker" | "all";
  searchAnnotations();
}

function toggleP2gSearchMode() {
  if (downloading.value) return;
  p2gSearchMode.value = p2gSearchMode.value === "gene" ? "region" : "gene";
}

function toggleMarkerPeakSearchMode() {
  if (downloading.value) return;
  markerPeakSearchMode.value = markerPeakSearchMode.value === "gene" ? "region" : "gene";
}

function currentAnnotationQuerySignature(): string {
  const sharedFilters = {
    context: context.value,
    maxFdr: maxFdr.value.trim(),
    minLog2fc: minLog2fc.value.trim(),
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
  };

  if (activeAnnotationType.value === "marker_gene") {
    return JSON.stringify({
      annotationType: "marker_gene",
      gene: targetGeneQuery.value.trim(),
      markerGeneSubtype: markerGeneSubtype.value,
      ...sharedFilters,
    });
  }

  if (activeAnnotationType.value === "marker_peak") {
    return JSON.stringify({
      annotationType: "marker_peak",
      searchMode: markerPeakSearchMode.value,
      query: markerPeakSearchQuery.value.trim(),
      ...sharedFilters,
    });
  }

  return JSON.stringify({
    annotationType: "linked_region",
    searchMode: p2gSearchMode.value,
    query: p2gSearchQuery.value.trim(),
    context: context.value,
    minP2gScore: minP2gScore.value.trim(),
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
  });
}

function displayText(value: unknown): string {
  const text = String(value ?? "").trim();
  return text || "-";
}

function cleanText(value: unknown): string {
  return String(value ?? "").replace(/\s+/g, " ").trim();
}

function normalizeComparableText(value: unknown): string {
  return cleanText(value).toLowerCase().replace(/[\s_()-]+/g, "");
}

function normalizeClusterLabel(value: unknown): string {
  const text = cleanText(value);
  if (!text) return "";

  const clusterMatch = text.match(/^cluster[\s_-]*(\d+)$/i);
  if (clusterMatch) return `C${clusterMatch[1]}`;

  const compactClusterMatch = text.match(/^c[\s_-]*(\d+)$/i);
  if (compactClusterMatch) return `C${compactClusterMatch[1]}`;

  return text;
}

function looksLikeClusterLabel(value: unknown): boolean {
  const text = cleanText(value);
  return /^(?:cluster[\s_-]*\d+|c[\s_-]*\d+)$/i.test(text);
}

function stripClusterSuffix(value: string): string {
  return cleanText(value).replace(/\s*\((?:cluster[\s_-]*\d+|c[\s_-]*\d+)\)\s*$/i, "").trim();
}

function parseContextParts(contextValue: unknown): { cellType?: string; clusterLabel?: string } {
  const contextText = cleanText(contextValue);
  if (!contextText) return {};

  const slashParts = contextText.split(/\s*\/\s*/).filter(Boolean);
  if (slashParts.length >= 2) {
    let cellType = "";
    let clusterLabel = "";

    slashParts.forEach((part) => {
      const parentheticalPart = part.match(/^(.+?)\s*\(([^()]+)\)$/);
      if (parentheticalPart) {
        if (!cellType) cellType = stripClusterSuffix(parentheticalPart[1] ?? "");
        if (!clusterLabel) clusterLabel = normalizeClusterLabel(parentheticalPart[2] ?? "");
        return;
      }

      if (looksLikeClusterLabel(part)) {
        if (!clusterLabel) clusterLabel = normalizeClusterLabel(part);
        return;
      }

      const strippedPart = stripClusterSuffix(part);
      if (!cellType || normalizeComparableText(strippedPart) !== normalizeComparableText(cellType)) {
        if (!cellType) cellType = strippedPart;
      }
    });

    return { cellType, clusterLabel };
  }

  const parentheticalMatch = contextText.match(/^(.+?)\s*\(([^()]+)\)$/);
  if (parentheticalMatch) {
    return {
      cellType: stripClusterSuffix(parentheticalMatch[1] ?? ""),
      clusterLabel: normalizeClusterLabel(parentheticalMatch[2] ?? ""),
    };
  }

  if (looksLikeClusterLabel(contextText)) return { clusterLabel: normalizeClusterLabel(contextText) };
  return { cellType: stripClusterSuffix(contextText) };
}

function formatCellTypeCluster(row: RegulatoryAnnotationRecord): string {
  const { cellType, cluster } = recordContextParts(row);
  const values = contextLabelParts(cellType, cluster);

  return values.length ? values.join(" / ") : "-";
}

function recordContextParts(row: RegulatoryAnnotationRecord): { cellType: string; cluster: string } {
  const parsedContext = parseContextParts(row.context);
  const cluster = normalizeClusterLabel(row.clusterLabel) || parsedContext.clusterLabel || "";
  if (props.domain !== "integration") {
    return { cellType: "", cluster };
  }
  const rawCellType = cleanText(row.cellType) || parsedContext.cellType || "";
  const cellType = stripClusterSuffix(rawCellType);

  return { cellType, cluster };
}

function contextLabelParts(cellTypeValue?: string, clusterValue?: string): string[] {
  const cluster = normalizeClusterLabel(clusterValue);
  if (props.domain !== "integration") {
    return cluster ? [cluster] : [];
  }
  const cellType = stripClusterSuffix(cleanText(cellTypeValue));

  return [cellType, cluster]
    .filter(Boolean)
    .filter((value, index, items) => items.findIndex((item) =>
      normalizeComparableText(item) === normalizeComparableText(value)
    ) === index);
}

function contextOptionLabel(cellTypeValue?: string, clusterValue?: string): string {
  return contextLabelParts(cellTypeValue, clusterValue).join(" / ");
}

function contextOptionValue(cellTypeValue?: string, clusterValue?: string): string {
  const cellType = props.domain === "integration"
    ? stripClusterSuffix(cleanText(cellTypeValue))
    : "";
  return `${cellType}||${normalizeClusterLabel(clusterValue)}`;
}

function normalizeContextOption(option: RegulatoryAnnotationContextOption): RegulatoryAnnotationContextOption | null {
  const cellType = props.domain === "integration"
    ? stripClusterSuffix(cleanText(option.cellType))
    : "";
  const cluster = normalizeClusterLabel(option.cluster);
  const label = contextOptionLabel(cellType, cluster);
  if (!label) return null;

  return {
    label,
    value: contextOptionValue(cellType, cluster),
    cellType: cellType || undefined,
    cluster: cluster || undefined,
    count: option.count,
  };
}

function selectedContextOption(): RegulatoryAnnotationContextOption | null {
  if (!context.value) return null;
  return contextOptions.value.find((option) => option.value === context.value) ?? parseContextOptionValue(context.value);
}

function parseContextOptionValue(value: string): RegulatoryAnnotationContextOption | null {
  const [cellType = "", cluster = ""] = value.split("||");
  return normalizeContextOption({
    label: "",
    value,
    cellType,
    cluster,
  });
}

function reconcileSelectedContext() {
  if (!context.value) return;
  if (!contextOptions.value.some((option) => option.value === context.value)) {
    context.value = "";
  }
}

function contextDisplay(row: RegulatoryAnnotationRecord): string {
  return formatCellTypeCluster(row);
}

function geneLabel(row: RegulatoryAnnotationRecord): string {
  return displayText(row.targetGene || row.geneSymbol || row.linkedGene || row.gene);
}

function hasGeneDetail(row: RegulatoryAnnotationRecord): boolean {
  return geneLabel(row) !== "-";
}

function peakLabel(row: RegulatoryAnnotationRecord, key = "peakRegion"): string {
  if (key === "linkedRegion") {
    return displayText(getLinkedRegion(row));
  }
  if (key === "linkedPeak") {
    return displayText(row.linkedPeak || row.peakName || row.peakRegion || row.regulatoryRegion);
  }
  if (key === "regulatoryRegion") {
    return displayText(row.regulatoryRegion || row.linkedPeak || row.peakRegion || row.peakName);
  }
  return displayText(row.peakName || row.peakRegion || row.regulatoryRegion || row.linkedPeak || row.peak);
}

function hasPeakDetail(row: RegulatoryAnnotationRecord, key = "peakRegion"): boolean {
  return peakLabel(row, key) !== "-";
}

function geneRegionDisplay(row: RegulatoryAnnotationRecord): string {
  return displayText(row.geneRegion || coordinateDisplay(row.geneChromosome, row.geneStart, row.geneEnd));
}

function promoterDisplay(row: RegulatoryAnnotationRecord): string {
  return displayText(row.promoterRegion || row.geneRegion);
}

function peakCoordinatesDisplay(row: RegulatoryAnnotationRecord): string {
  return displayText(
    row.peakRegion ||
    coordinateDisplay(row.peakChromosome, row.peakStart, row.peakEnd) ||
    row.regulatoryRegion ||
    row.linkedPeak
  );
}

function linkedGeneDisplay(row: RegulatoryAnnotationRecord): string {
  return displayText(row.linkedGene || row.targetGene || row.geneSymbol);
}

type PeakFeatureInfo = {
  chrom?: string;
  start?: number;
  end?: number;
  peakId?: string;
  location: string;
};

function getLinkedRegion(row: RegulatoryAnnotationRecord): string {
  return cleanText(row.peakRegion || row.linkedPeak || row.regulatoryRegion || row.peakName || row.peak);
}

function parseCoordinateRegion(value: unknown): { chrom: string; start: number; end: number } | null {
  const text = cleanText(value).replace(/,/g, "");
  const match = text.match(/^(chr[^:\s]+):(\d+)-(\d+)$/i);
  if (!match) return null;

  const start = Number(match[2]);
  const end = Number(match[3]);
  if (!Number.isFinite(start) || !Number.isFinite(end)) return null;

  return {
    chrom: match[1] ?? "",
    start,
    end,
  };
}

function coordinateNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  return undefined;
}

function peakFeature(row: RegulatoryAnnotationRecord, key = "peakRegion"): PeakFeatureInfo {
  const parsed = [
    peakLabel(row, key),
    getLinkedRegion(row),
    row.peakRegion,
    row.linkedPeak,
    row.regulatoryRegion,
    row.peakName,
    row.region,
  ]
    .map(parseCoordinateRegion)
    .find((region): region is { chrom: string; start: number; end: number } => region !== null);

  const chrom = cleanText(row.peakChromosome || row.chromosome || parsed?.chrom) || undefined;
  let start = coordinateNumber(row.peakStart) ?? coordinateNumber(row.start) ?? parsed?.start;
  let end = coordinateNumber(row.peakEnd) ?? coordinateNumber(row.end) ?? parsed?.end;
  if (start !== undefined && end !== undefined && start > end) {
    const tmp = start; start = end; end = tmp;
  }
  const location = chrom && start !== undefined && end !== undefined
    ? `${chrom}:${start}-${end}`
    : peakLabel(row, key);

  return {
    chrom,
    start,
    end,
    peakId: cleanText(row.peakId) || undefined,
    location,
  };
}

function peakIdDisplay(row: RegulatoryAnnotationRecord): string {
  return displayText(row.peakId);
}

function canViewGeneDetail(row: RegulatoryAnnotationRecord): boolean {
  const gene = cleanText(geneLabel(row));
  if (!gene || gene === "-") return false;
  const region = geneCoordinateRegion(row);
  return Boolean(region.chrom && region.start !== undefined && region.end !== undefined);
}

function canViewPeakDetail(row: RegulatoryAnnotationRecord, key = "peakRegion"): boolean {
  const feature = peakFeature(row, key);
  return Boolean(feature.chrom && feature.start !== undefined && feature.end !== undefined);
}

function goToGeneDetail(row: RegulatoryAnnotationRecord) {
  if (!canViewGeneDetail(row)) return;
  const region = geneCoordinateRegion(row);
  router.push({
    path: "/feature-detail",
    query: {
      type: "gene",
      gene: geneLabel(row),
      chrom: region.chrom,
      start: String(region.start),
      end: String(region.end),
      strand: cleanText(row.strand) || undefined,
      datasetId: sourceSampleId(row),
      domain: sourceDomain(row),
      source: "sample_regulatory_annotation",
      returnTo: route.fullPath,
    },
  });
}

function geneCoordinateRegion(row: RegulatoryAnnotationRecord): { chrom: string; start?: number; end?: number } {
  const parsedRegion = parseCoordinateRegion(row.geneRegion);
  let start = coordinateNumber(row.geneStart) ?? parsedRegion?.start;
  let end = coordinateNumber(row.geneEnd) ?? parsedRegion?.end;
  if (start !== undefined && end !== undefined && start > end) {
    const tmp = start; start = end; end = tmp;
  }
  return {
    chrom: cleanText(row.geneChromosome || parsedRegion?.chrom) || "",
    start,
    end,
  };
}

function goToPeakDetail(row: RegulatoryAnnotationRecord, key = "peakRegion") {
  if (!canViewPeakDetail(row, key)) return;
  const feature = peakFeature(row, key);
  router.push({
    path: "/feature-detail",
    query: {
      type: "peak",
      chrom: feature.chrom,
      start: String(feature.start),
      end: String(feature.end),
      peakId: feature.peakId,
      datasetId: sourceSampleId(row),
      domain: sourceDomain(row),
      source: "sample_regulatory_annotation",
      returnTo: route.fullPath,
    },
  });
}

function coordinateDisplay(chromosome?: string, start?: number, end?: number): string {
  if (!chromosome || !Number.isFinite(start) || !Number.isFinite(end)) return "";
  return `${chromosome}:${Number(start).toLocaleString()}-${Number(end).toLocaleString()}`;
}

function finiteNumber(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function formatMetric(value: unknown): string {
  const numberValue = finiteNumber(value);
  if (numberValue === null) return "-";
  const absoluteValue = Math.abs(numberValue);
  if (absoluteValue > 0 && absoluteValue < 0.001) return numberValue.toExponential(2);
  return numberValue.toLocaleString(undefined, { maximumFractionDigits: 4 });
}

function parseOptionalNumber(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function metricDisplay(row: RegulatoryAnnotationRecord, key: string): string {
  const value = row[key as keyof RegulatoryAnnotationRecord];
  return formatMetric(value);
}

function formatMarkerEvidence(log2fcValue: unknown, fdrValue: unknown, emptyLabel = "-"): string {
  const log2fc = formatMetric(log2fcValue);
  const fdr = formatMetric(fdrValue);
  const parts = [];
  if (log2fc !== "-") parts.push(`L2FC ${log2fc}`);
  if (fdr !== "-") parts.push(`FDR ${fdr}`);
  return parts.length ? parts.join(" / ") : emptyLabel;
}

function geneEvidenceDisplay(row: RegulatoryAnnotationRecord): string {
  return formatMarkerEvidence(row.geneLog2fc, row.geneFdr);
}

function peakEvidenceDisplay(row: RegulatoryAnnotationRecord): string {
  return formatMarkerEvidence(row.peakLog2fc, row.peakFdr);
}

function linkScoreDisplay(row: RegulatoryAnnotationRecord): string {
  if (activeAnnotationType.value === "linked_region") {
    return formatMetric(row.linkScore);
  }
  return formatMetric(row.linkScore ?? row.correlation);
}

function tfColumnDisplay(row: RegulatoryAnnotationRecord): string {
  return displayText(row.tf || row.tfName || row.tfAnnotation);
}

function isSearchResultDomain(value: unknown): value is SearchResultDomain {
  return value === "integration" || value === "rna" || value === "atac";
}

function sourceDomain(row: RegulatoryAnnotationRecord): SearchResultDomain {
  return isSearchResultDomain(row.domain) ? row.domain : props.domain;
}

function sourceDomainLabel(row: RegulatoryAnnotationRecord): string {
  return domainDisplayLabel(sourceDomain(row));
}

function sourceSampleId(row?: RegulatoryAnnotationRecord): string {
  return cleanText(row?.datasetId || props.overview?.datasetId || props.datasetId) || "Sample";
}

function normalizeSourceLabel(value: unknown): string {
  const text = cleanText(value);
  if (!text) return "";
  if (/\.(csv|tsv|txt)$/i.test(text)) return "";
  if (/(marker[\s_-]*(gene|peak)|gene[\s_-]*exp|peak[\s_-]*gene|gene[\s_-]*peak|tf[\s_-]*binding|linked[\s_-]*region)/i.test(text)) return "";
  return text;
}

function overviewSourceLabel(): string {
  const overview = props.overview;
  const candidates = [
    overview?.sampleType,
    overview?.tissue,
    overview?.sampleName,
    overview?.sampleSource,
  ];
  const dataset = sourceSampleId();
  return candidates
    .map(normalizeSourceLabel)
    .find((value) => value && normalizeComparableText(value) !== normalizeComparableText(dataset)) || "";
}

function formatDisplaySource(row: RegulatoryAnnotationRecord): string {
  const sampleId = sourceSampleId(row);
  const sourceLabel = overviewSourceLabel() || normalizeSourceLabel(row.source) || sourceDomainLabel(row);
  return `${sampleId} / ${sourceLabel}`;
}

function sourceDisplay(row: RegulatoryAnnotationRecord): string {
  return formatDisplaySource(row);
}

function annotationLabel(annotationType: RegulatoryAnnotationType): string {
  const labels: Record<RegulatoryAnnotationType, string> = {
    marker_gene: "Marker gene",
    marker_peak: "Marker peak",
    linked_region: "P2G link",
  };
  return labels[annotationType];
}

function cellDisplay(row: RegulatoryAnnotationRecord, key: string): string {
  if (key === "context") return contextDisplay(row);
  if (key === "geneRegion") return geneRegionDisplay(row);
  if (key === "promoterRegion") return promoterDisplay(row);
  if (key === "linkedGene") return linkedGeneDisplay(row);

  const value = row[key as keyof RegulatoryAnnotationRecord];
  return displayText(value);
}

function sanitizeFilenamePart(value: string): string {
  return value.trim().replace(/[\\/:*?"<>|\s]+/g, "_") || "sample";
}

function clearRows() {
  records.value = [];
  total.value = 0;
}

function filterRowsForActiveTab(items: RegulatoryAnnotationRecord[]): RegulatoryAnnotationRecord[] {
  return items.filter((row) => row.annotationType === activeAnnotationType.value);
}

async function loadAnnotations(existingToken?: number) {
  const normalizedDatasetId = props.datasetId.trim();
  const currentToken = existingToken ?? ++requestToken;
  const requestedQuerySignature = currentAnnotationQuerySignature();
  loading.value = true;
  hasLoaded.value = false;

  if (!normalizedDatasetId) {
    if (currentToken === requestToken) {
      clearRows();
      hasLoaded.value = true;
      loading.value = false;
    }
    return;
  }

  try {
    const selectedContext = selectedContextOption();
    const data = await fetchRegulatoryAnnotations({
        datasetId: normalizedDatasetId,
        domain: props.domain,
        annotationType: activeAnnotationType.value,
        page: page.value,
        pageSize: pageSize.value,
        targetGene: activeAnnotationType.value === "linked_region"
          ? p2gSearchMode.value === "gene" ? targetGeneQuery.value : ""
          : activeAnnotationType.value === "marker_peak"
            ? markerPeakSearchMode.value === "gene" ? targetGeneQuery.value : ""
            : showTargetGeneFilter.value ? targetGeneQuery.value : "",
        peak: activeAnnotationType.value === "linked_region"
          ? p2gSearchMode.value === "region" ? peakQuery.value : ""
          : activeAnnotationType.value === "marker_peak" && markerPeakSearchMode.value === "region"
            ? peakQuery.value
            : "",
        contextCellType: showContextFilter.value ? selectedContext?.cellType : undefined,
        contextCluster: showContextFilter.value ? selectedContext?.cluster : undefined,
        maxFdr: showMaxFdrFilter.value ? parseOptionalNumber(maxFdr.value) : undefined,
        minLog2fc: showMinLog2fcFilter.value ? parseOptionalNumber(minLog2fc.value) : undefined,
        minP2gScore: showMinP2gScoreFilter.value ? parseOptionalNumber(minP2gScore.value) : undefined,
        signalType: activeAnnotationType.value === "marker_gene" ? markerGeneSubtype.value : undefined,
        sortBy: sortBy.value || undefined,
        sortOrder: sortOrder.value || undefined,
        p2gMode: activeAnnotationType.value === "linked_region" ? p2gMode.value : undefined,
      });

    if (currentToken !== requestToken) return;

    const incomingItems = Array.isArray(data.items) ? data.items : [];
    const filteredItems = filterRowsForActiveTab(incomingItems);
    records.value = filteredItems;
    total.value = filteredItems.length === incomingItems.length
      ? Number.isFinite(data.total) ? data.total : records.value.length
      : filteredItems.length;
    page.value = Number.isFinite(data.page) && data.page > 0 ? data.page : page.value;
    pageSize.value = Number.isFinite(data.pageSize) && data.pageSize > 0 ? data.pageSize : pageSize.value;
    loadedQuerySignature.value = requestedQuerySignature;
  } catch (loadError) {
    if (currentToken !== requestToken) return;
    console.error("[SearchResult] Failed to load regulatory annotations:", loadError);
    clearRows();
  } finally {
    if (currentToken === requestToken) {
      hasLoaded.value = true;
      loading.value = false;
    }
  }
}

async function loadContextOptions() {
  if (!showContextFilter.value) {
    loadedContextOptions.value = [];
    context.value = "";
    return;
  }

  const normalizedDatasetId = props.datasetId.trim();
  if (!normalizedDatasetId) {
    loadedContextOptions.value = [];
    context.value = "";
    return;
  }

  const currentToken = ++contextOptionsRequestToken;
  contextOptionsLoading.value = true;

  try {
    const options = await fetchRegulatoryAnnotationContextOptions({
      datasetId: normalizedDatasetId,
      domain: props.domain,
      annotationType: activeAnnotationType.value,
    });
    if (currentToken !== contextOptionsRequestToken) return;

    loadedContextOptions.value = options
      .map(normalizeContextOption)
      .filter((option): option is RegulatoryAnnotationContextOption => option !== null);
    reconcileSelectedContext();
  } catch (loadError) {
    if (currentToken !== contextOptionsRequestToken) return;
    if (!isSearchResultEndpointUnavailable(loadError)) {
      console.error("[SearchResult] Failed to load regulatory annotation context options:", loadError);
    }
    loadedContextOptions.value = [];
    context.value = "";
  } finally {
    if (currentToken === contextOptionsRequestToken) contextOptionsLoading.value = false;
  }
}

async function reloadContextOptionsAndAnnotations() {
  if (downloading.value) {
    reloadDeferredForDownload.value = true;
    return;
  }
  reloadDeferredForDownload.value = false;
  const currentToken = ++requestToken;
  loading.value = true;
  hasLoaded.value = false;
  // Load context options in background — never block annotation loading
  loadContextOptions().catch(() => {});
  await loadAnnotations(currentToken);
}

function searchAnnotations() {
  if (downloading.value) return;
  page.value = 1;
  void loadAnnotations();
}

function resetAnnotations() {
  if (downloading.value) return;
  targetGeneQuery.value = "";
  peakQuery.value = "";
  context.value = "";
  maxFdr.value = "";
  minLog2fc.value = "";
  minP2gScore.value = "0.25";
  sortBy.value = "";
  sortOrder.value = "";
  page.value = 1;
  void loadAnnotations();
}

function sortButtonLabel(column: AnnotationColumn): string {
  if (sortBy.value !== column.key || !sortOrder.value) {
    return `Sort ${column.label} ascending`;
  }
  if (sortOrder.value === "asc") {
    return `${column.label} sorted ascending; click for descending`;
  }
  return `${column.label} sorted descending; click to clear sorting`;
}

function cycleTableSort(column: AnnotationColumn) {
  if (downloading.value) return;
  if (!column.sortable) return;

  if (sortBy.value !== column.key || !sortOrder.value) {
    sortBy.value = column.key;
    sortOrder.value = "asc";
  } else if (sortOrder.value === "asc") {
    sortOrder.value = "desc";
  } else {
    sortBy.value = "";
    sortOrder.value = "";
  }
  page.value = 1;
  void loadAnnotations();
}

function onPageSizeChange(nextPageSize: number) {
  if (downloading.value) return;
  pageSize.value = nextPageSize;
  page.value = 1;
  void loadAnnotations();
}

function onPageChange(nextPage: number) {
  if (downloading.value) return;
  page.value = nextPage;
  void loadAnnotations();
}

async function downloadCsv() {
  if (downloading.value || records.value.length === 0 || total.value === 0) return;

  const requestedAnnotationType = activeAnnotationType.value;
  const requestedP2gMode = p2gMode.value;
  const requestedDownloadLabel = requestedAnnotationType === "linked_region"
    ? requestedP2gMode === "marker" ? "P2G Marker" : "All P2G links"
    : activeAnnotationConfig.value.label;
  const requestedDatasetId = props.datasetId.trim();
  const requestedDomain = props.domain;
  const selectedContext = selectedContextOption();
  const annotationFilename = requestedAnnotationType === "linked_region"
    ? `${requestedAnnotationType}_${requestedP2gMode}.csv`
    : `${requestedAnnotationType}_all.csv`;
  const fallbackFilename = `${sanitizeFilenamePart(requestedDatasetId)}_${requestedDomain}_${annotationFilename}`;
  const downloadRequest = {
    datasetId: requestedDatasetId,
    domain: requestedDomain,
    annotationType: requestedAnnotationType,
    targetGene: requestedAnnotationType === "linked_region"
      ? p2gSearchMode.value === "gene" ? targetGeneQuery.value : ""
      : requestedAnnotationType === "marker_peak"
        ? markerPeakSearchMode.value === "gene" ? targetGeneQuery.value : ""
        : showTargetGeneFilter.value ? targetGeneQuery.value : "",
    peak: requestedAnnotationType === "linked_region"
      ? p2gSearchMode.value === "region" ? peakQuery.value : ""
      : requestedAnnotationType === "marker_peak" && markerPeakSearchMode.value === "region"
        ? peakQuery.value
        : "",
    contextCellType: showContextFilter.value ? selectedContext?.cellType : undefined,
    contextCluster: showContextFilter.value ? selectedContext?.cluster : undefined,
    maxFdr: showMaxFdrFilter.value ? parseOptionalNumber(maxFdr.value) : undefined,
    minLog2fc: showMinLog2fcFilter.value ? parseOptionalNumber(minLog2fc.value) : undefined,
    minP2gScore: showMinP2gScoreFilter.value ? parseOptionalNumber(minP2gScore.value) : undefined,
    signalType: requestedAnnotationType === "marker_gene" ? markerGeneSubtype.value : undefined,
    sortBy: sortBy.value || undefined,
    sortOrder: sortOrder.value || undefined,
    p2gMode: requestedAnnotationType === "linked_region" ? requestedP2gMode : undefined,
    sampleLabel: overviewSourceLabel(),
  };

  try {
    await runRegulatoryAnnotationDownload(requestedDatasetId, requestedDownloadLabel, async () => {
      const download = await fetchRegulatoryAnnotationsDownload(downloadRequest);

      const url = URL.createObjectURL(download.blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = download.filename || fallbackFilename;
      anchor.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    });
    ElMessage.success(`${requestedDownloadLabel} download is ready.`);
  } catch (e) {
    console.error("Failed to download all annotations:", e);
    ElMessage.error("Download failed. Please retry.");
  }
}

watch(() => props.domain, (newDomain) => {
  annotationTabStates.clear();
  // Auto-switch to a valid tab if current tab not available in new domain
  const tabs = newDomain === "integration"
    ? [annotationTabConfig.marker_gene, annotationTabConfig.marker_peak, annotationTabConfig.linked_region]
    : newDomain === "rna"
      ? [annotationTabConfig.marker_gene]
      : newDomain === "atac"
        ? [annotationTabConfig.marker_gene, annotationTabConfig.marker_peak]
        : [annotationTabConfig.marker_gene];
  const firstTab = tabs[0];
  if (firstTab && !tabs.some(t => t.value === activeAnnotationType.value)) {
    activeAnnotationType.value = firstTab.value;
  }
  page.value = 1;
  sortBy.value = "";
  sortOrder.value = "";
  void reloadContextOptionsAndAnnotations();
}, { immediate: true });

watch(() => props.datasetId, () => {
  annotationTabStates.clear();
  activeAnnotationType.value = DEFAULT_ANNOTATION_TYPE;
  page.value = 1;
  sortBy.value = "";
  sortOrder.value = "";
  void reloadContextOptionsAndAnnotations();
});

watch(downloading, (isDownloading, wasDownloading) => {
  if (wasDownloading && !isDownloading && reloadDeferredForDownload.value) {
    void reloadContextOptionsAndAnnotations();
  }
});

</script>

<style scoped>
.annotation-section {
  --detail-teal: var(--brand-primary-3);
  --detail-teal-hover: #7f9a90;
  --detail-teal-active: #6f887d;
  --detail-teal-soft: var(--nav-active-bg);
  --detail-teal-border: var(--nav-active-border);
  --detail-teal-focus: #8fa59c40;
  --el-color-primary: var(--detail-teal);
  position: relative;
  overflow: hidden;
  padding: 16px 18px 18px;
  margin-bottom: 14px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.annotation-section:hover {
  border-color: var(--border-brand);
  box-shadow: var(--shadow-hover);
}

.annotation-content[aria-hidden="true"] {
  user-select: none;
}

.annotation-download-lock {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(248, 251, 250, 0.86);
  backdrop-filter: blur(3px);
  cursor: wait;
}

.annotation-download-lock-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: min(520px, 100%);
  padding: 18px 20px;
  border: 1px solid rgba(95, 125, 112, 0.28);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 44px rgba(39, 66, 58, 0.16);
}

.annotation-download-lock-spinner {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border: 3px solid rgba(95, 125, 112, 0.2);
  border-top-color: var(--brand-primary-3, #8fa59c);
  border-radius: 50%;
  animation: annotationDownloadSpin 0.78s linear infinite;
}

.annotation-download-lock-title {
  color: var(--text);
  font-size: 15px;
  font-weight: 900;
}

.annotation-download-lock-copy {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 650;
  line-height: 1.5;
}

.annotation-download-lock-enter-active,
.annotation-download-lock-leave-active {
  transition: opacity 0.18s ease;
}

.annotation-download-lock-enter-from,
.annotation-download-lock-leave-to {
  opacity: 0;
}

@keyframes annotationDownloadSpin {
  to { transform: rotate(360deg); }
}

.section-head {
  position: relative;
  display: grid;
  grid-template-columns: minmax(260px, 1fr) max-content;
  grid-template-areas: "heading tags";
  align-items: center;
  column-gap: clamp(16px, 2.4vw, 32px);
  row-gap: 10px;
  min-height: 54px;
  margin-bottom: 14px;
}

.section-heading {
  grid-area: heading;
  min-width: 0;
  max-width: min(470px, 40vw);
}

.section-title {
  font-size: 18px;
  font-weight: 900;
}

.section-sub {
  margin-top: 4px;
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: normal;
}

.annotation-actions {
  grid-area: actions;
  position: absolute;
  left: 52%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: max-content;
}

.annotation-tabs {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  flex: 0 0 auto;
  gap: 4px;
  min-height: 44px;
  padding: 4px;
  overflow: hidden;
  border: 1px solid rgba(143, 165, 156, 0.22);
  border-radius: 999px;
  background: #f5faf8;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.035);
}

.marker-subtype-dropdown {
  margin-left: 6px;
  display: inline-flex;
  align-items: center;
}
.marker-subtype-trigger {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-primary-3, #8FA59C);
  cursor: pointer;
  white-space: nowrap;
  padding: 2px 6px;
  border-radius: 6px;
  transition: background 0.15s;
}
.marker-subtype-trigger:hover {
  background: rgba(143,165,156,0.10);
}

.annotation-tab-slider {
  position: absolute;
  top: 4px;
  left: 0;
  height: calc(100% - 8px);
  border-radius: 999px;
  background: #c7d8d2;
  box-shadow:
    0 6px 16px rgba(95, 125, 112, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.42);
  transition:
    transform 0.42s cubic-bezier(0.34, 1.56, 0.64, 1),
    width 0.42s cubic-bezier(0.34, 1.56, 0.64, 1);
  pointer-events: none;
  z-index: 0;
}

.annotation-tab {
  position: relative;
  z-index: 1;
  -webkit-appearance: none;
  appearance: none;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  margin: 0;
  padding: 7px 17px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: rgba(74, 96, 88, 0.78);
  box-shadow: none;
  cursor: pointer;
  font: inherit;
  font-size: 13.75px;
  font-weight: 760;
  line-height: 1;
  white-space: nowrap;
  user-select: none;
  transition: color 0.22s ease;
}

.annotation-tab:hover {
  color: var(--detail-teal-active);
}

.annotation-tab.active {
  color: #173f38;
  font-weight: 900;
  background: transparent;
  box-shadow: none;
}

.annotation-tab:focus-visible {
  outline: 2px solid var(--detail-teal-focus);
  outline-offset: 2px;
}

.annotation-download-tooltip {
  display: inline-flex;
  flex: 0 0 auto;
}

.annotation-download-button {
  -webkit-appearance: none;
  appearance: none;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: #fffffff2;
  color: var(--brand-primary-3);
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 6px 14px #12182614;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.annotation-download-button:hover:not(:disabled) {
  border-color: var(--nav-active-border);
  background: var(--surface-2);
  color: var(--text);
  box-shadow:
    inset 0 1px 0 #ffffffcc,
    0 8px 16px rgba(95, 125, 112, 0.16);
  transform: translateY(-1px);
}

.annotation-download-button:focus-visible {
  outline: 2px solid var(--detail-teal-focus);
  outline-offset: 2px;
}

.annotation-download-button:disabled {
  border-color: var(--border);
  background: #ffffffb8;
  color: var(--muted);
  cursor: not-allowed;
  opacity: 0.56;
  pointer-events: none;
  box-shadow: inset 0 1px 0 #ffffffcc;
}

.annotation-download-button :deep(.el-icon) {
  font-size: 15px;
}

.section-tags {
  grid-area: tags;
  display: flex;
  justify-self: end;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.data-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 220px;
  min-height: 30px;
  padding: 6px 12px;
  overflow: hidden;
  border: 1px solid var(--nav-active-border);
  border-radius: 999px;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}



.mono {
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
}

.query-panel {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px;
  margin-bottom: 14px;
  border: 1px solid rgba(143, 165, 156, 0.18);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.query-panel:hover {
  border-color: rgba(143, 165, 156, 0.34);
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.12);
  transform: translateY(-1px);
}

.query-panel--pending {
  background:
    linear-gradient(135deg, #ffffffd9, #f4f7f6d9);
}

.control-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 170px;
}

.control-field--grow {
  flex: 1;
  min-width: 220px;
}

.control-field--compact {
  min-width: 132px;
}

.query-panel--p2g .control-field--p2g-search {
  flex: 1 1 400px;
  min-width: 360px;
}

.query-panel--p2g .control-field--context {
  flex: 0 1 220px;
  min-width: 210px;
}

.query-panel--p2g .control-field--p2g-score {
  flex: 0 1 160px;
  min-width: 160px;
}

.control-label-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  min-height: 18px;
}

.p2g-mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border: 1px solid rgba(143, 165, 156, 0.3);
  border-radius: 999px;
  background: rgba(143, 174, 163, 0.1);
  color: #53766c;
  font: inherit;
  font-size: 11px;
  font-weight: 850;
  line-height: 1.2;
  cursor: pointer;
  transition: color 0.18s ease, background-color 0.18s ease, border-color 0.18s ease;
}

.p2g-mode-switch:hover,
.p2g-mode-switch:focus-visible {
  border-color: rgba(111, 143, 132, 0.58);
  background: rgba(143, 174, 163, 0.2);
  color: #244f46;
  outline: none;
}

.p2g-mode-switch .el-icon {
  font-size: 12px;
}

.control-label {
  color: rgba(39, 66, 58, 0.84);
  font-size: 12px;
  font-weight: 900;
}

.control-input {
  width: 100%;
}

.control-input :deep(.el-input__wrapper),
.control-input :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(143, 165, 156, 0.2) inset;
  transition:
    background-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.control-input :deep(.el-input__wrapper:hover),
.control-input :deep(.el-select__wrapper:hover) {
  background: #ffffff;
  box-shadow:
    0 0 0 1px rgba(143, 165, 156, 0.34) inset,
    0 5px 14px rgba(95, 125, 112, 0.08);
}

.control-input :deep(.el-input__wrapper.is-focus),
.control-input :deep(.el-select__wrapper.is-focused) {
  background: #ffffff;
  box-shadow:
    0 0 0 1px rgba(127, 154, 144, 0.5) inset,
    0 0 0 3px rgba(143, 165, 156, 0.16);
}

.control-input :deep(.el-input__inner),
.control-input :deep(.el-select__placeholder),
.control-input :deep(.el-select__selected-item) {
  font-size: 14px;
  font-weight: 760;
}

.pending-filter-note {
  align-self: center;
  max-width: 240px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
}

.section-button,
.soft-button {
  font-weight: 900;
}

.section-button {
  align-self: end;
  min-width: 112px;
  min-height: 44px;
  border-radius: 13px;
  font-size: 14px;
  line-height: 1;
  --el-button-bg-color: #8faea3;
  --el-button-border-color: #8faea3;
  --el-button-hover-bg-color: #7f9f94;
  --el-button-hover-border-color: #7f9f94;
  --el-button-active-bg-color: #6f8f84;
  --el-button-active-border-color: #6f8f84;
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.2);
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.section-button:hover {
  box-shadow: 0 10px 22px rgba(95, 125, 112, 0.26);
  transform: translateY(-1px);
}

.section-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.section-button :deep(.el-icon),
.soft-button :deep(.el-icon) {
  margin-right: 5px;
}

.soft-button {
  align-self: end;
  min-height: 44px;
  border-radius: 13px;
  border-color: rgba(143, 165, 156, 0.26);
  background: rgba(255, 255, 255, 0.9);
  color: #173f38;
  box-shadow: 0 5px 14px rgba(15, 23, 42, 0.04);
}

.annotation-section :deep(.el-button--primary) {
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.22);
}

.annotation-section :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(95, 125, 112, 0.26);
}

.soft-button.is-disabled,
.soft-button.is-disabled:hover {
  border-color: var(--border);
  background: var(--surface-2);
  color: var(--muted);
}

.detail-table {
  border-radius: 14px;
  overflow: hidden;
}

:deep(.detail-table th.el-table__cell),
:deep(.detail-table td.el-table__cell) {
  text-align: center;
  vertical-align: middle;
  padding: 10px 0;
}

:deep(.detail-table th.el-table__cell > .cell),
:deep(.detail-table td.el-table__cell > .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  line-height: 1.35;
  text-align: center;
}

:deep(.detail-table td.el-table__cell.annotation-context-column > .cell) {
  min-height: 38px;
  height: auto;
  padding: 5px 10px;
  overflow: visible;
  text-overflow: clip;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.column-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  max-width: 100%;
  line-height: 1.2;
}

.column-help-icon {
  flex: 0 0 auto;
  color: var(--muted);
  cursor: help;
  font-size: 13px;
}

.column-help-icon:hover {
  color: var(--detail-teal-active);
}

.annotation-sort-toggle {
  position: relative;
  flex: 0 0 auto;
  width: 18px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.annotation-sort-toggle:hover,
.annotation-sort-toggle:focus-visible {
  background: rgba(66, 105, 92, 0.1);
  outline: none;
}

.annotation-sort-arrow {
  position: absolute;
  left: 50%;
  width: 0;
  height: 0;
  border-right: 4px solid transparent;
  border-left: 4px solid transparent;
  opacity: 0.42;
  transform: translateX(-50%);
  transition: border-color 0.16s ease, opacity 0.16s ease, transform 0.16s ease;
}

.annotation-sort-arrow--up {
  top: 3px;
  border-bottom: 5px solid currentColor;
}

.annotation-sort-arrow--down {
  bottom: 3px;
  border-top: 5px solid currentColor;
}

.annotation-sort-toggle.is-active {
  color: var(--brand-primary);
}

.annotation-sort-toggle.is-ascending .annotation-sort-arrow--up,
.annotation-sort-toggle.is-descending .annotation-sort-arrow--down {
  opacity: 1;
  filter: drop-shadow(0 0 2px rgba(66, 105, 92, 0.32));
  transform: translateX(-50%) scale(1.12);
}

.annotation-sort-toggle.is-ascending .annotation-sort-arrow--down,
.annotation-sort-toggle.is-descending .annotation-sort-arrow--up {
  opacity: 0.16;
}

.gene-link-inline {
  display: inline-flex; align-items: center; gap: 6px;
}
.gene-detail-btn {
  -webkit-appearance: none; appearance: none;
  padding: 2px 10px; border: 1px solid var(--brand-primary-3);
  border-radius: 999px; background: var(--brand-primary-3);
  color: #fff; font-size: 11px; font-weight: 700;
  cursor: pointer; white-space: nowrap;
  transition: opacity 0.15s;
}
.gene-detail-btn:hover { opacity: 0.85; }
.gene-detail-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.gene-link-name { font-weight: 700; }

.link-button {
  -webkit-appearance: none;
  appearance: none;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  min-height: 22px;
  margin: 0;
  padding: 2px 6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  box-shadow: none;
  color: var(--detail-teal);
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.link-button:hover {
  background: color-mix(in srgb, var(--detail-teal-soft) 52%, transparent);
  box-shadow: 0 0 0 1px var(--detail-teal-border) inset;
  color: var(--detail-teal-active);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.link-button:focus-visible {
  outline: 2px solid var(--detail-teal-focus);
  outline-offset: 2px;
}

.peak-link {
  color: #237b76;
}

.metric-badge,
.marker-evidence-badge,
.tf-null-badge,
.source-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  min-height: 24px;
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-2);
  color: var(--text);
  font-size: 12px;
  font-weight: 850;
  line-height: 1.15;
}

.metric-badge--strong {
  border-color: var(--detail-teal-border);
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
}

.marker-evidence-badge {
  border-color: var(--border-brand);
  background: #ffffff;
  color: var(--text);
}

.signal-type-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.signal-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.signal-dot--exp {
  background-color: #22c55e;
}

.signal-dot--score {
  background-color: #facc15;
}

.signal-legend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.signal-legend .signal-dot {
  width: 8px;
  height: 8px;
}

:deep(.el-tooltip__content .signal-legend) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  white-space: nowrap;
}

:deep(.el-tooltip__content .signal-dot) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

:deep(.el-tooltip__content .signal-dot--exp) {
  background-color: #22c55e;
}

:deep(.el-tooltip__content .signal-dot--score) {
  background-color: #facc15;
}

.signal-type-label {
  color: var(--muted);
}

.tf-null-badge {
  min-width: 34px;
  border-color: rgba(143, 165, 156, 0.24);
  background: #ffffff;
  color: var(--muted);
}

.source-chip {
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-tooltip {
  display: grid;
  gap: 2px;
  max-width: 260px;
  color: var(--text);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
}

.empty-state,
.state-message {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 116px;
  padding: 18px;
  border: 1px dashed var(--border);
  border-radius: 14px;
  background: var(--surface-2);
  color: var(--muted);
  font-weight: 800;
  text-align: center;
}

.empty-state--stacked {
  flex-direction: column;
  gap: 7px;
}

.empty-title {
  color: var(--text);
  font-size: 15px;
  font-weight: 900;
}

.empty-subtitle {
  max-width: 680px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.45;
}

.annotation-pending-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  min-height: 150px;
  padding: 20px;
  border: 1px solid var(--border-brand);
  border-radius: 14px;
  background:
    linear-gradient(135deg, #f4f7f6, #ffffff 52%, #e9efed);
  box-shadow: inset 0 1px 0 #ffffffcc;
}

.pending-kicker {
  margin-bottom: 6px;
  color: var(--brand-primary-3);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.pending-title {
  color: var(--text);
  font-size: 18px;
  font-weight: 950;
}

.pending-copy {
  max-width: 760px;
  margin-top: 7px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
}

.future-fields {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  max-width: 360px;
}

.future-fields span {
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #ffffffcf;
  color: var(--text);
  box-shadow: 0 6px 14px rgba(18, 24, 38, 0.06);
  font-size: 12px;
  font-weight: 850;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.pager :deep(.el-pagination) {
  --el-color-primary: var(--detail-teal);
  --el-pagination-hover-color: var(--detail-teal-active);
  --el-pagination-button-bg-color: var(--surface);
  --el-pagination-button-disabled-bg-color: var(--surface-2);
}

.pager :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--detail-teal);
  border-color: var(--detail-teal);
  color: var(--surface);
  font-weight: 800;
}

:global(.annotation-detail-popper.el-popover.el-popper) {
  padding: 0;
  border: 1px solid var(--border-brand);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 18px 42px rgba(18, 24, 38, 0.18);
}

.detail-card {
  background: var(--surface);
}

.detail-card-head {
  padding: 15px 16px;
  background: linear-gradient(135deg, var(--nav-active-bg), #ffffff);
  border-bottom: 1px solid var(--border);
}
.detail-card-head-row {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
}
.gene-detail-btn {
  -webkit-appearance: none; appearance: none;
  padding: 5px 16px; border: 1px solid var(--brand-primary-3);
  border-radius: 999px; background: var(--brand-primary-3);
  color: #fff; font-size: 13px; font-weight: 700;
  cursor: pointer; white-space: nowrap;
  transition: opacity 0.15s;
}
.gene-detail-btn:hover { opacity: 0.85; }

.detail-kicker {
  color: var(--brand-primary-3);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.detail-title {
  margin-top: 4px;
  color: var(--text);
  font-size: 20px;
  font-weight: 950;
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.detail-pills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.detail-pill {
  max-width: 100%;
  padding: 5px 8px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #ffffffc9;
  color: var(--text);
  font-size: 11px;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-card-body {
  padding: 14px 16px 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.detail-grid div,
.metric-card {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface-2);
}

.detail-grid-wide {
  grid-column: 1 / -1;
}

.detail-grid span,
.metric-card span,
.detail-evidence span {
  display: block;
  margin-bottom: 4px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 900;
}

.detail-grid strong,
.metric-card strong {
  display: block;
  color: var(--text);
  font-size: 13px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(86px, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.metric-card {
  text-align: center;
}

.detail-primary-button {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 34px;
  margin-top: 12px;
  padding: 0 12px;
  border: 1px solid var(--detail-teal-border, #8fa59c66);
  border-radius: 8px;
  background: #ffffff;
  color: var(--detail-teal-active, #6f887d);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
  text-align: center;
  text-shadow: none;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.detail-primary-button:hover:not(:disabled) {
  border-color: var(--detail-teal, #8fa59c);
  background: var(--detail-teal-soft, #c6d4ce);
  color: var(--brand-ink, #1b2a27);
  box-shadow: 0 8px 18px rgba(95, 125, 112, 0.24);
  transform: translateY(-1px);
}

.detail-primary-button:disabled {
  border-color: var(--border);
  background: var(--surface-2);
  color: var(--muted);
  cursor: not-allowed;
}

:global(.annotation-detail-popper .detail-primary-button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--border-brand, #8fa59c66);
  border-radius: 8px;
  background: #ffffff;
  color: #6f887d;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
  text-align: center;
  text-shadow: none;
}

:global(.annotation-detail-popper .detail-primary-button:hover:not(:disabled)) {
  border-color: var(--brand-primary-3, #8fa59c);
  background: var(--nav-active-bg, #c6d4ce);
  color: var(--brand-ink, #1b2a27);
}

.detail-evidence {
  margin-top: 10px;
  padding: 11px 12px;
  border: 1px dashed var(--border-brand);
  border-radius: 10px;
  background: #ffffff;
}

.detail-evidence p {
  margin: 0;
  color: var(--text);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

@media (max-width: 1100px) {
  .section-head {
    grid-template-columns: minmax(0, 1fr) max-content;
    grid-template-areas:
      "heading tags"
      "actions tags";
    align-items: start;
    min-height: 0;
  }

  .section-heading {
    max-width: none;
  }

  .annotation-actions {
    position: static;
    left: auto;
    top: auto;
    transform: none;
    justify-self: start;
    width: auto;
    max-width: 100%;
  }
}

@media (max-width: 760px) {
  .section-head {
    grid-template-columns: 1fr;
    grid-template-areas:
      "heading"
      "actions"
      "tags";
  }

  .annotation-actions {
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .annotation-tabs {
    flex: 0 0 auto;
  }

  .section-tags {
    justify-content: flex-start;
  }

  .control-field,
  .control-field--grow,
  .control-field--compact,
  .query-panel--p2g .control-field--p2g-search,
  .query-panel--p2g .control-field--context,
  .query-panel--p2g .control-field--p2g-score {
    width: 100%;
    min-width: 0;
  }

  .annotation-pending-card {
    grid-template-columns: 1fr;
  }

  .future-fields {
    justify-content: flex-start;
    max-width: none;
  }

  .pager {
    justify-content: flex-start;
  }
}
</style>

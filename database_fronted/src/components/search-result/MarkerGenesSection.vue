<template>
  <section class="annotation-section float-card">
    <div class="section-head">
      <div class="section-heading">
        <div class="section-title">Regulatory annotation</div>
        <div class="section-sub">{{ sectionDescription }}</div>
      </div>

      <div v-if="isIntegrationDomain" class="annotation-actions">
        <div class="annotation-tabs" role="tablist" aria-label="Regulatory annotation view">
          <button
            v-for="tab in annotationTabs"
            :key="tab.value"
            type="button"
            class="annotation-tab"
            :class="{ active: activeAnnotationType === tab.value }"
            :aria-selected="activeAnnotationType === tab.value"
            role="tab"
            @click="setAnnotationType(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <el-tooltip content="Download current page" placement="top" effect="light" :show-after="220">
          <span class="annotation-download-tooltip">
            <button
              type="button"
              class="annotation-download-button"
              :disabled="csvDisabled"
              aria-label="Download current page"
              @click="downloadCsv"
            >
              <el-icon><Download /></el-icon>
            </button>
          </span>
        </el-tooltip>
      </div>

      <div class="section-tags">
        <span v-if="demoMode" class="demo-badge">DEMO DATA</span>
        <span class="data-chip" :title="domainTitle">{{ domainChip }}</span>
        <span class="data-chip mono" :title="datasetId">{{ datasetId }}</span>
      </div>
    </div>

    <div v-if="!isIntegrationDomain" class="annotation-pending-card annotation-unavailable-card">
      <div class="pending-header">
        <div class="pending-kicker">Unavailable</div>
        <div class="pending-title">Regulatory annotations unavailable</div>
        <div class="pending-copy">
          Regulatory annotations are currently available for the Integration view only. RNA-only and ATAC-only
          marker annotations have not been provided for this sample.
        </div>
      </div>
    </div>

    <template v-else>
      <div class="query-panel">
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

        <label v-if="showPeakFilter" class="control-field control-field--grow">
          <span class="control-label">{{ activeSearchConfig.label }}</span>
          <el-input
            v-model="peakQuery"
            class="control-input"
            :placeholder="activeSearchConfig.placeholder"
            clearable
            size="small"
            @keyup.enter="searchAnnotations"
          />
        </label>

        <label v-if="showContextFilter" class="control-field">
          <span class="control-label">Cell type / Cluster</span>
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

        <el-button type="primary" class="section-button" :loading="loading" @click="searchAnnotations">
          <el-icon><Search /></el-icon>
          <span>Search</span>
        </el-button>
        <el-button class="soft-button" @click="resetAnnotations">
          <el-icon><Refresh /></el-icon>
          <span>Reset</span>
        </el-button>
      </div>

      <el-skeleton v-if="loading" animated :rows="6" />

      <template v-else>
        <el-table
          v-if="records.length > 0"
          :data="records"
          stripe
          border
          class="detail-table"
        >
        <el-table-column
          v-for="column in activeColumns"
          :key="column.key"
          :label="column.label"
          :min-width="column.minWidth"
          :align="column.align"
          :show-overflow-tooltip="column.overflowTooltip"
        >
          <template #header>
            <span class="column-header">
              <span>{{ column.label }}</span>
              <el-tooltip
                v-if="column.headerTooltip"
                :content="column.headerTooltip"
                placement="top"
                effect="light"
                :show-after="180"
              >
                <el-icon class="column-help-icon"><InfoFilled /></el-icon>
              </el-tooltip>
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
                    <div class="detail-kicker">Gene detail</div>
                    <div class="detail-title">{{ geneLabel(row) }}</div>
                    <div class="detail-pills">
                      <span v-if="formatCellTypeCluster(row) !== '-'" class="detail-pill">{{ formatCellTypeCluster(row) }}</span>
                      <span class="detail-pill">{{ formatDisplaySource(row) }}</span>
                    </div>
                  </div>
                  <div class="detail-card-body">
                    <div class="detail-grid">
                      <div>
                        <span>Feature type</span>
                        <strong>Gene</strong>
                      </div>
                      <div>
                        <span>Gene symbol</span>
                        <strong>{{ geneLabel(row) }}</strong>
                      </div>
                      <div>
                        <span>Cell type / Cluster</span>
                        <strong>{{ formatCellTypeCluster(row) }}</strong>
                      </div>
                      <div>
                        <span>Gene region</span>
                        <strong>{{ geneRegionDisplay(row) }}</strong>
                      </div>
                      <div>
                        <span>Strand</span>
                        <strong>{{ displayText(row.strand) }}</strong>
                      </div>
                      <div class="detail-grid-wide">
                        <span>Promoter region</span>
                        <strong>{{ promoterDisplay(row) }}</strong>
                      </div>
                      <div class="detail-grid-wide">
                        <span>Dataset / Domain</span>
                        <strong>{{ sourceSampleId(row) }} / {{ sourceDomainLabel(row) }}</strong>
                      </div>
                    </div>
                    <div class="tf-summary-box">
                      <span>{{ tfSummaryDisplay("") }}</span>
                      <small>{{ tfSummaryReason("") }}</small>
                    </div>
                    <div class="metric-row">
                      <div class="metric-card">
                        <span>Log2FC</span>
                        <strong>{{ formatMetric(row.geneLog2fc) }}</strong>
                      </div>
                      <div class="metric-card">
                        <span>FDR</span>
                        <strong>{{ formatMetric(row.geneFdr) }}</strong>
                      </div>
                      <div class="metric-card">
                        <span>MeanDiff</span>
                        <strong>{{ formatMetric(row.geneMeanDiff) }}</strong>
                      </div>
                    </div>
                    <button
                      type="button"
                      class="detail-primary-button"
                      :disabled="!canViewGeneDetail(row)"
                      :title="geneDetailButtonTitle(row)"
                      @click="goToGeneDetail(row)"
                    >
                      View gene detail
                    </button>
                    <button
                      v-if="canViewLinkedPeakFromGene(row)"
                      type="button"
                      class="detail-primary-button"
                      :disabled="!canViewPeakDetail(row, p2gPeakColumnKey)"
                      :title="peakDetailButtonTitle(row, p2gPeakColumnKey)"
                      @click="goToPeakDetail(row, p2gPeakColumnKey)"
                    >
                      View peak detail
                    </button>
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
                  <div class="detail-card-head detail-card-head--peak">
                    <div class="detail-kicker">Peak detail</div>
                    <div class="detail-title">{{ peakLabel(row, column.key) }}</div>
                    <div class="detail-pills">
                      <span v-if="formatCellTypeCluster(row) !== '-'" class="detail-pill">{{ formatCellTypeCluster(row) }}</span>
                      <span class="detail-pill">{{ formatDisplaySource(row) }}</span>
                    </div>
                  </div>
                  <div class="detail-card-body">
                    <div class="detail-grid">
                      <div>
                        <span>Feature type</span>
                        <strong>Peak / Regulatory region</strong>
                      </div>
                      <div class="detail-grid-wide">
                        <span>Location</span>
                        <strong>{{ peakLabel(row, column.key) }}</strong>
                      </div>
                      <div v-if="peakIdDisplay(row) !== '-'">
                        <span>Peak ID</span>
                        <strong>{{ peakIdDisplay(row) }}</strong>
                      </div>
                      <div>
                        <span>Cell type / Cluster</span>
                        <strong>{{ formatCellTypeCluster(row) }}</strong>
                      </div>
                      <div>
                        <span>Linked gene</span>
                        <strong>{{ linkedGeneDisplay(row) }}</strong>
                      </div>
                      <div class="detail-grid-wide">
                        <span>Peak coordinates</span>
                        <strong>{{ peakCoordinatesDisplay(row) }}</strong>
                      </div>
                      <div class="detail-grid-wide">
                        <span>Dataset / Domain</span>
                        <strong>{{ sourceSampleId(row) }} / {{ sourceDomainLabel(row) }}</strong>
                      </div>
                    </div>
                    <div class="tf-summary-box">
                      <span>{{ tfSummaryDisplay("") }}</span>
                      <small>{{ tfSummaryReason("") }}</small>
                    </div>
                    <div class="metric-row">
                      <div class="metric-card">
                        <span>Log2FC</span>
                        <strong>{{ formatMetric(row.peakLog2fc) }}</strong>
                      </div>
                      <div class="metric-card">
                        <span>FDR</span>
                        <strong>{{ formatMetric(row.peakFdr) }}</strong>
                      </div>
                      <div class="metric-card">
                        <span>MeanDiff</span>
                        <strong>{{ formatMetric(row.peakMeanDiff) }}</strong>
                      </div>
                      <div v-if="linkScoreDisplay(row) !== '-'" class="metric-card">
                        <span>{{ activeAnnotationType === "linked_region" ? "P2G score" : "Link score" }}</span>
                        <strong>{{ linkScoreDisplay(row) }}</strong>
                      </div>
                    </div>
                    <button
                      type="button"
                      class="detail-primary-button"
                      :disabled="!canViewPeakDetail(row, column.key)"
                      :title="peakDetailButtonTitle(row, column.key)"
                      @click="goToPeakDetail(row, column.key)"
                    >
                      View peak detail
                    </button>
                    <button
                      v-if="canViewLinkedGeneFromPeak(row)"
                      type="button"
                      class="detail-primary-button"
                      :disabled="!canViewGeneDetail(row)"
                      :title="geneDetailButtonTitle(row)"
                      @click="goToGeneDetail(row)"
                    >
                      View gene detail
                    </button>
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
    </template>
  </section>
</template>

<script setup lang="ts">
import { Download, InfoFilled, Refresh, Search } from "@element-plus/icons-vue";
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
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
  isSearchResultEndpointUnavailable,
  normalizeRegulatoryAnnotationRecord,
} from "@/api/searchResult";
import type { DemoDataSize } from "@/mock/searchResultDemoData";
import { domainDisplayLabel } from "@/utils/searchResultDomain";

type ColumnKind =
  | "text"
  | "gene-link"
  | "peak-link"
  | "metric"
  | "gene-evidence"
  | "peak-evidence"
  | "link-score"
  | "tf"
  | "source";

interface AnnotationColumn {
  key: string;
  label: string;
  kind: ColumnKind;
  minWidth: number;
  align?: "left" | "center" | "right";
  overflowTooltip?: boolean;
  headerTooltip?: string;
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

const props = defineProps<{
  datasetId: string;
  domain: SearchResultDomain;
  overview?: SearchResultOverviewData | null;
  demoMode: boolean;
  demoSize: DemoDataSize;
}>();

const router = useRouter();

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
    description: "Candidate peak-to-gene links supported by marker gene and marker peak evidence.",
  },
};

const annotationTabs = Object.values(annotationTabConfig);

const COLUMN_TOOLTIPS = {
  geneRegion: "Genomic coordinates of the marker gene from the source annotation.",
  promoterRegion: "Inferred promoter/TSS-proximal region based on gene annotation and strand information.",
  markerGeneLog2fc: "Log2 fold-change of the marker gene in the selected cell type / cluster.",
  markerGeneFdr: "False discovery rate for marker gene significance.",
  markerGeneMeanDiff: "Mean expression difference or source-provided marker effect size.",
  markerGeneSample: "Dataset/sample identifier and data domain.",
  markerPeakPeak: "Genomic coordinates of a marker chromatin accessibility peak.",
  markerPeakLinkedGene: "Gene associated with this peak in the source annotation. This does not necessarily imply cell-type-specific regulation.",
  markerPeakLog2fc: "Log2 fold-change of accessibility for this marker peak in the selected cell type / cluster.",
  markerPeakFdr: "False discovery rate for marker peak significance.",
  markerPeakMeanDiff: "Mean accessibility difference or source-provided marker effect size.",
  markerPeakSample: "Dataset/sample identifier and data domain.",
  p2gGene: "Target gene in the candidate peak-to-gene link.",
  p2gLinkedPeak: "Linked chromatin accessibility peak associated with the target gene by the source peak-to-gene link.",
  p2gScore: "Global peak-to-gene link score. This link is not cell-type-specific in the current version.",
  p2gGeneEvidence: "Marker gene statistics for the target gene in the selected cell type / cluster, such as Log2FC and FDR.",
  p2gPeakEvidence: "Marker peak statistics for the linked peak in the selected cell type / cluster, such as Log2FC and FDR.",
  p2gSample: "Dataset/sample identifier and data domain. Do not display CSV file names or pseudo source labels.",
  tf: "Transcription factor annotation reserved for future integration. Current datasets may return no TF value.",
} as const;

const pageSizeOptions = [10, 20, 50];
const DEFAULT_ANNOTATION_TYPE: RegulatoryAnnotationType = "marker_gene";

const demoRecords: RegulatoryAnnotationRecord[] = [
  normalizeRegulatoryAnnotationRecord({
    id: "demo-gene-fos",
    annotationType: "marker_gene",
    targetGene: "FOS",
    geneSymbol: "FOS",
    geneRegion: "chr14:75,734,821-75,737,303",
    strand: "+",
    promoterRegion: "chr14:75,732,821-75,736,821",
    cellType: "Tissue stem cells",
    clusterLabel: "Cluster 1",
    geneLog2fc: 1.284,
    geneFdr: 0.00042,
    geneMeanDiff: 0.318,
    source: "marker_gene_exp",
    evidence: "RNA marker gene",
  }),
  normalizeRegulatoryAnnotationRecord({
    id: "demo-gene-il7r",
    annotationType: "marker_gene",
    targetGene: "IL7R",
    geneSymbol: "IL7R",
    geneRegion: "chr5:35,852,659-35,879,618",
    strand: "+",
    promoterRegion: "chr5:35,850,659-35,854,659",
    cellType: "CD4+ T cells",
    clusterLabel: "Cluster 3",
    geneLog2fc: 0.963,
    geneFdr: 0.0021,
    geneMeanDiff: 0.247,
    source: "marker_gene_exp",
    evidence: "RNA marker gene",
  }),
  normalizeRegulatoryAnnotationRecord({
    id: "demo-peak-il7r",
    annotationType: "marker_peak",
    peakName: "chr5:35,892,120-35,893,010",
    peakRegion: "chr5:35,892,120-35,893,010",
    peakChromosome: "chr5",
    peakStart: 35892120,
    peakEnd: 35893010,
    linkedGene: "IL7R",
    cellType: "CD4+ T cells",
    clusterLabel: "Cluster 3",
    peakLog2fc: 0.812,
    peakFdr: 0.0036,
    peakMeanDiff: 0.204,
    source: "marker_peak",
    evidence: "ATAC marker peak",
  }),
  normalizeRegulatoryAnnotationRecord({
    id: "demo-peak-mki67",
    annotationType: "marker_peak",
    peakName: "chr10:128,096,740-128,100,220",
    peakRegion: "chr10:128,096,740-128,100,220",
    peakChromosome: "chr10",
    peakStart: 128096740,
    peakEnd: 128100220,
    linkedGene: "MKI67",
    context: "Cycling cells",
    peakLog2fc: 1.046,
    peakFdr: 0.0008,
    peakMeanDiff: 0.291,
    source: "marker_peak",
    evidence: "candidate regulatory peak",
  }),
  normalizeRegulatoryAnnotationRecord({
    id: "demo-link-fos",
    annotationType: "linked_region",
    targetGene: "FOS",
    linkedGene: "FOS",
    regulatoryRegion: "chr14:75,735,860-75,736,420",
    linkedPeak: "chr14:75,735,860-75,736,420",
    peakRegion: "chr14:75,735,860-75,736,420",
    promoterRegion: "chr14:75,732,821-75,736,821",
    cellType: "Tissue stem cells",
    clusterLabel: "Cluster 1",
    geneLog2fc: 1.284,
    geneFdr: 0.00042,
    peakLog2fc: 0.716,
    peakFdr: 0.0042,
    linkScore: 0.783,
    correlation: 0.682,
    source: "peak-gene link",
    evidence: "marker gene + marker peak evidence",
  }),
  normalizeRegulatoryAnnotationRecord({
    id: "demo-link-il7r",
    annotationType: "linked_region",
    targetGene: "IL7R",
    linkedGene: "IL7R",
    regulatoryRegion: "chr5:35,892,120-35,893,010",
    linkedPeak: "chr5:35,892,120-35,893,010",
    peakRegion: "chr5:35,892,120-35,893,010",
    cellType: "CD4+ T cells",
    clusterLabel: "Cluster 3",
    geneLog2fc: 0.963,
    geneFdr: 0.0021,
    peakLog2fc: 0.812,
    peakFdr: 0.0036,
    linkScore: 0.691,
    correlation: 0.618,
    distance: 39155,
    source: "peak-gene link",
    evidence: "candidate peak-to-gene link with marker evidence",
  }),
];

const activeAnnotationType = ref<RegulatoryAnnotationType>(DEFAULT_ANNOTATION_TYPE);
const targetGeneQuery = ref("");
const peakQuery = ref("");
const context = ref("");
const maxFdr = ref("");
const minLog2fc = ref("");
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const records = ref<RegulatoryAnnotationRecord[]>([]);
const loading = ref(false);
const hasLoaded = ref(false);
const loadedContextOptions = ref<RegulatoryAnnotationContextOption[]>([]);
const contextOptionsLoading = ref(false);


let requestToken = 0;
let contextOptionsRequestToken = 0;

const domainChip = computed(() => domainDisplayLabel(props.domain));
const domainTitle = computed(() => domainChip.value);
const isIntegrationDomain = computed(() => props.domain === "integration");
const csvDisabled = computed(() => !isIntegrationDomain.value || records.value.length === 0);
const activeAnnotationConfig = computed(() => annotationTabConfig[activeAnnotationType.value]);
const unavailableDescription =
  "Regulatory annotations are currently available for the Integration view only. RNA-only and ATAC-only marker annotations have not been provided for this sample.";
const sectionDescription = computed(() =>
  isIntegrationDomain.value ? activeAnnotationConfig.value.description : unavailableDescription
);
const activeSearchConfig = computed<AnnotationSearchConfig>(() => {
  if (activeAnnotationType.value === "marker_peak") {
    return {
      label: "Peak / gene search",
      placeholder: "Exact linked gene or peak region",
    };
  }

  if (activeAnnotationType.value === "linked_region") {
    return {
      label: "Target gene search",
      placeholder: "Exact target gene, e.g. CD22",
    };
  }

  return {
    label: "Gene search",
    placeholder: "Exact gene symbol, e.g. CD22",
  };
});
const showTargetGeneFilter = computed(() =>
  activeAnnotationType.value === "marker_gene" ||
  activeAnnotationType.value === "linked_region"
);
const showPeakFilter = computed(() => activeAnnotationType.value === "marker_peak");
const showContextFilter = computed(() =>
  activeAnnotationType.value === "marker_gene" ||
  activeAnnotationType.value === "marker_peak" ||
  activeAnnotationType.value === "linked_region"
);
const showMaxFdrFilter = computed(() => true);
const showMinLog2fcFilter = computed(() => true);

const demoContextOptions = computed(() => buildContextOptionsFromRecords(
  demoRecords.filter((row) => row.annotationType === activeAnnotationType.value)
));
const contextOptions = computed(() => props.demoMode ? demoContextOptions.value : loadedContextOptions.value);

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
  if (activeAnnotationType.value === "linked_region") {
    return "Try another target gene, cell type / cluster, or evidence threshold.";
  }
  if (activeAnnotationType.value === "marker_gene") {
    return "Marker genes will appear here when available.";
  }
  if (activeAnnotationType.value === "marker_peak") {
    return "Marker peaks will appear here when available.";
  }
  return "Try another target gene, peak, cell type / cluster, or evidence threshold.";
});

function getActiveColumns(annotationType: RegulatoryAnnotationType): AnnotationColumn[] {
  if (annotationType === "marker_gene") {
    return [
      { key: "targetGene", label: "Gene", kind: "gene-link", minWidth: 140, overflowTooltip: true },
      { key: "tf", label: "TF", kind: "tf", minWidth: 90, align: "center", headerTooltip: COLUMN_TOOLTIPS.tf },
      { key: "context", label: "Cell / Cluster", kind: "text", minWidth: 170, overflowTooltip: true },
      {
        key: "geneRegion",
        label: "Gene region",
        kind: "text",
        minWidth: 190,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.geneRegion,
      },
      {
        key: "promoterRegion",
        label: "Promoter region",
        kind: "text",
        minWidth: 210,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.promoterRegion,
      },
      { key: "strand", label: "Strand", kind: "text", minWidth: 90, align: "center" },
      {
        key: "geneLog2fc",
        label: "Log2FC",
        kind: "metric",
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerGeneLog2fc,
      },
      {
        key: "geneFdr",
        label: "FDR",
        kind: "metric",
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerGeneFdr,
      },
      {
        key: "geneMeanDiff",
        label: "MeanDiff",
        kind: "metric",
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
      { key: "tf", label: "TF", kind: "tf", minWidth: 90, align: "center", headerTooltip: COLUMN_TOOLTIPS.tf },
      { key: "context", label: "Cell / Cluster", kind: "text", minWidth: 170, overflowTooltip: true },
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
        kind: "metric",
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerPeakLog2fc,
      },
      {
        key: "peakFdr",
        label: "FDR",
        kind: "metric",
        minWidth: 105,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.markerPeakFdr,
      },
      {
        key: "peakMeanDiff",
        label: "MeanDiff",
        kind: "metric",
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
    return [
      {
        key: "targetGene",
        label: "Gene",
        kind: "gene-link",
        minWidth: 140,
        overflowTooltip: true,
        headerTooltip: COLUMN_TOOLTIPS.p2gGene,
      },
      { key: "tf", label: "TF", kind: "tf", minWidth: 90, align: "center", headerTooltip: COLUMN_TOOLTIPS.tf },
      { key: "context", label: "Cell / Cluster", kind: "text", minWidth: 170, overflowTooltip: true },
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
        kind: "link-score",
        minWidth: 120,
        align: "center",
        headerTooltip: COLUMN_TOOLTIPS.p2gScore,
      },
      {
        key: "geneEvidence",
        label: "Gene evidence",
        kind: "gene-evidence",
        minWidth: 150,
        headerTooltip: COLUMN_TOOLTIPS.p2gGeneEvidence,
      },
      {
        key: "peakEvidence",
        label: "Peak evidence",
        kind: "peak-evidence",
        minWidth: 150,
        headerTooltip: COLUMN_TOOLTIPS.p2gPeakEvidence,
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
  if (activeAnnotationType.value === nextType) return;
  activeAnnotationType.value = nextType;
  page.value = 1;
  void reloadContextOptionsAndAnnotations();
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

function normalizeGeneSearchText(value: unknown): string {
  return cleanText(value).toLowerCase();
}

function isGenomicRegionQuery(value: unknown): boolean {
  const text = cleanText(value).replace(/,/g, "");
  return /^chr[^:]+:\d+(?:-\d+)?$/i.test(text);
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
  const rawCellType = cleanText(row.cellType) || parsedContext.cellType || "";
  const cellType = stripClusterSuffix(rawCellType);
  const cluster = normalizeClusterLabel(row.clusterLabel) || parsedContext.clusterLabel || "";

  return { cellType, cluster };
}

function contextLabelParts(cellTypeValue?: string, clusterValue?: string): string[] {
  const cellType = stripClusterSuffix(cleanText(cellTypeValue));
  const cluster = normalizeClusterLabel(clusterValue);

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
  return `${stripClusterSuffix(cleanText(cellTypeValue))}||${normalizeClusterLabel(clusterValue)}`;
}

function normalizeContextOption(option: RegulatoryAnnotationContextOption): RegulatoryAnnotationContextOption | null {
  const cellType = stripClusterSuffix(cleanText(option.cellType));
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

function buildContextOptionsFromRecords(sourceRecords: RegulatoryAnnotationRecord[]): RegulatoryAnnotationContextOption[] {
  const optionsByValue = new Map<string, RegulatoryAnnotationContextOption>();

  sourceRecords.forEach((row) => {
    const { cellType, cluster } = recordContextParts(row);
    const option = normalizeContextOption({
      label: "",
      value: "",
      cellType,
      cluster,
      count: 1,
    });
    if (!option) return;

    const existing = optionsByValue.get(option.value);
    if (existing) {
      existing.count = (existing.count ?? 0) + 1;
      return;
    }
    optionsByValue.set(option.value, option);
  });

  return Array.from(optionsByValue.values())
    .sort((left, right) => left.label.localeCompare(right.label));
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
  const start = coordinateNumber(row.peakStart) ?? coordinateNumber(row.start) ?? parsed?.start;
  const end = coordinateNumber(row.peakEnd) ?? coordinateNumber(row.end) ?? parsed?.end;
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

function geneDetailButtonTitle(row: RegulatoryAnnotationRecord): string {
  if (!cleanText(geneLabel(row)) || geneLabel(row) === "-") return "Gene symbol is required.";
  if (!geneCoordinateRegion(row).chrom) return "Gene coordinates are required.";
  return "Open gene detail";
}

function peakDetailButtonTitle(row: RegulatoryAnnotationRecord, key = "peakRegion"): string {
  return canViewPeakDetail(row, key) ? "Open peak detail" : "Peak coordinates are required.";
}

const p2gPeakColumnKey = "linkedRegion";

function canViewLinkedPeakFromGene(row: RegulatoryAnnotationRecord): boolean {
  return activeAnnotationType.value === "linked_region" && hasPeakDetail(row, p2gPeakColumnKey);
}

function canViewLinkedGeneFromPeak(row: RegulatoryAnnotationRecord): boolean {
  return activeAnnotationType.value === "linked_region" && hasGeneDetail(row);
}

function tfSummaryDisplay(_key: string): string {
  return "TF: --";
}

function tfSummaryReason(_key: string): string {
  return "TF annotation data have not been integrated yet.";
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
      datasetId: sourceSampleId(row),
      domain: sourceDomain(row),
    },
  });
}

function geneCoordinateRegion(row: RegulatoryAnnotationRecord): { chrom: string; start?: number; end?: number } {
  const parsedRegion = parseCoordinateRegion(row.geneRegion);
  return {
    chrom: cleanText(row.geneChromosome || parsedRegion?.chrom) || "",
    start: coordinateNumber(row.geneStart) ?? parsedRegion?.start,
    end: coordinateNumber(row.geneEnd) ?? parsedRegion?.end,
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

function getRowFdrValues(row: RegulatoryAnnotationRecord): number[] {
  if (activeAnnotationType.value === "marker_gene") return [row.geneFdr].filter(isFiniteNumber);
  if (activeAnnotationType.value === "marker_peak") return [row.peakFdr].filter(isFiniteNumber);
  return [row.geneFdr, row.peakFdr, row.linkFdr].filter(isFiniteNumber);
}

function getRowLog2fcValues(row: RegulatoryAnnotationRecord): number[] {
  if (activeAnnotationType.value === "marker_gene") return [row.geneLog2fc].filter(isFiniteNumber);
  if (activeAnnotationType.value === "marker_peak") return [row.peakLog2fc].filter(isFiniteNumber);
  return [row.geneLog2fc, row.peakLog2fc].filter(isFiniteNumber);
}

function isFiniteNumber(value: unknown): value is number {
  return typeof value === "number" && Number.isFinite(value);
}

function rowMatchesActiveFilters(row: RegulatoryAnnotationRecord): boolean {
  const targetGene = targetGeneQuery.value.trim();
  const peak = peakQuery.value.trim();
  const selectedContext = selectedContextOption();
  const maxFdrValue = parseOptionalNumber(maxFdr.value);
  const minLog2fcValue = parseOptionalNumber(minLog2fc.value);

  if (activeAnnotationType.value === "marker_gene" || activeAnnotationType.value === "linked_region") {
    if (targetGene && normalizeGeneSearchText(geneLabel(row)) !== normalizeGeneSearchText(targetGene)) return false;
  }

  if (activeAnnotationType.value === "marker_peak") {
    if (peak && isGenomicRegionQuery(peak)) {
      const peakHaystack = [
        peakLabel(row),
        row.peakName,
        row.peakRegion,
        row.linkedPeak,
      ].map((value) => String(value ?? "").toLowerCase()).join(" ");
      if (!peakHaystack.includes(peak.toLowerCase())) return false;
    } else if (peak) {
      const queryGene = normalizeGeneSearchText(peak);
      const matchesLinkedGene = [
        linkedGeneDisplay(row),
        row.targetGene,
      ].some((value) => normalizeGeneSearchText(value) === queryGene);
      if (!matchesLinkedGene) return false;
    }
  }

  if (selectedContext) {
    const { cellType, cluster } = recordContextParts(row);
    if (selectedContext.cellType && normalizeComparableText(cellType) !== normalizeComparableText(selectedContext.cellType)) {
      return false;
    }
    if (selectedContext.cluster && normalizeComparableText(cluster) !== normalizeComparableText(selectedContext.cluster)) {
      return false;
    }
  }

  if (maxFdrValue !== undefined) {
    const fdrValues = getRowFdrValues(row);
    if (fdrValues.length === 0 || Math.min(...fdrValues) > maxFdrValue) return false;
  }

  if (minLog2fcValue !== undefined) {
    const log2fcValues = getRowLog2fcValues(row);
    if (log2fcValues.length === 0 || Math.max(...log2fcValues) < minLog2fcValue) return false;
  }

  return true;
}

function getFilteredDemoRecords(): RegulatoryAnnotationRecord[] {
  return demoRecords
    .filter((row) => row.annotationType === activeAnnotationType.value)
    .filter(rowMatchesActiveFilters);
}

async function loadAnnotations(existingToken?: number) {
  const normalizedDatasetId = props.datasetId.trim();
  const currentToken = existingToken ?? ++requestToken;
  loading.value = true;
  hasLoaded.value = false;

  if (!isIntegrationDomain.value) {
    if (currentToken === requestToken) {
      clearRows();
      hasLoaded.value = true;
      loading.value = false;
    }
    return;
  }

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
    const data = props.demoMode
      ? getDemoAnnotationResponse()
      : await fetchRegulatoryAnnotations({
        datasetId: normalizedDatasetId,
        domain: props.domain,
        annotationType: activeAnnotationType.value,
        page: page.value,
        pageSize: pageSize.value,
        targetGene: showTargetGeneFilter.value ? targetGeneQuery.value : activeAnnotationType.value === "marker_peak" ? peakQuery.value : "",
        peak: showPeakFilter.value ? peakQuery.value : "",
        contextCellType: showContextFilter.value ? selectedContext?.cellType : undefined,
        contextCluster: showContextFilter.value ? selectedContext?.cluster : undefined,
        maxFdr: showMaxFdrFilter.value ? parseOptionalNumber(maxFdr.value) : undefined,
        minLog2fc: showMinLog2fcFilter.value ? parseOptionalNumber(minLog2fc.value) : undefined,
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
  } catch (loadError) {
    if (currentToken !== requestToken) return;
    if (!isSearchResultEndpointUnavailable(loadError)) {
      console.error("[SearchResult] Failed to load regulatory annotations:", loadError);
    }
    clearRows();
  } finally {
    if (currentToken === requestToken) {
      hasLoaded.value = true;
      loading.value = false;
    }
  }
}

function getDemoAnnotationResponse() {
  const filtered = getFilteredDemoRecords();
  const start = (page.value - 1) * pageSize.value;
  return {
    total: filtered.length,
    page: page.value,
    pageSize: pageSize.value,
    items: filtered.slice(start, start + pageSize.value),
  };
}

async function loadContextOptions() {
  if (!isIntegrationDomain.value) {
    contextOptionsRequestToken += 1;
    loadedContextOptions.value = [];
    context.value = "";
    contextOptionsLoading.value = false;
    return;
  }

  if (!showContextFilter.value) {
    loadedContextOptions.value = [];
    context.value = "";
    return;
  }

  if (props.demoMode) {
    reconcileSelectedContext();
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
  const currentToken = ++requestToken;
  loading.value = true;
  hasLoaded.value = false;
  await loadContextOptions();
  if (currentToken !== requestToken) return;
  await loadAnnotations(currentToken);
}

function searchAnnotations() {
  page.value = 1;
  void loadAnnotations();
}

function resetAnnotations() {
  targetGeneQuery.value = "";
  peakQuery.value = "";
  context.value = "";
  maxFdr.value = "";
  minLog2fc.value = "";
  page.value = 1;
  void loadAnnotations();
}

function onPageSizeChange(nextPageSize: number) {
  pageSize.value = nextPageSize;
  page.value = 1;
  void loadAnnotations();
}

function onPageChange(nextPage: number) {
  page.value = nextPage;
  void loadAnnotations();
}

function csvValue(value: unknown): string {
  const raw = String(value ?? "");
  return `"${raw.replace(/"/g, '""')}"`;
}

function columnCsvValue(row: RegulatoryAnnotationRecord, column: AnnotationColumn): string {
  if (column.kind === "gene-link") return geneLabel(row);
  if (column.kind === "peak-link") return peakLabel(row, column.key);
  if (column.kind === "metric") return metricDisplay(row, column.key);
  if (column.kind === "gene-evidence") return geneEvidenceDisplay(row);
  if (column.kind === "peak-evidence") return peakEvidenceDisplay(row);
  if (column.kind === "link-score") return linkScoreDisplay(row);
  if (column.kind === "tf") return tfColumnDisplay(row);
  if (column.kind === "source") return sourceDisplay(row);
  return cellDisplay(row, column.key);
}

function downloadCsv() {
  if (records.value.length === 0) return;

  const headers = activeColumns.value.map((column) => column.label);
  const rows = records.value.map((row) => activeColumns.value.map((column) => columnCsvValue(row, column)));
  const csv = [headers, ...rows].map((row) => row.map(csvValue).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");

  anchor.href = url;
  anchor.download = `${sanitizeFilenamePart(props.datasetId)}_${props.domain}_${activeAnnotationType.value}_page_${page.value}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

watch(() => props.domain, () => {
  page.value = 1;
  void reloadContextOptionsAndAnnotations();
}, { immediate: true });

watch(() => props.datasetId, () => {
  activeAnnotationType.value = DEFAULT_ANNOTATION_TYPE;
  page.value = 1;
  void reloadContextOptionsAndAnnotations();
});

watch(() => [props.demoMode, props.demoSize] as const, () => {
  page.value = 1;
  void reloadContextOptionsAndAnnotations();
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

.annotation-tab {
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
  transition:
    background-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease;
}

.annotation-tab:hover {
  background: rgba(255, 255, 255, 0.72);
  color: var(--detail-teal-active);
}

.annotation-tab.active {
  background: #c7d8d2;
  color: #173f38;
  font-weight: 900;
  box-shadow:
    0 6px 16px rgba(95, 125, 112, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.42);
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
  max-width: 220px;
  min-height: 30px;
  padding: 6px 12px;
  overflow: hidden;
  border: 1px solid var(--nav-active-border);
  border-radius: 999px;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  box-shadow: 0 8px 18px rgba(27, 92, 84, 0.1);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.demo-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 6px 12px;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: #ffffff;
  color: var(--brand-primary-3);
  box-shadow: inset 0 1px 0 #ffffffcc;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
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

.detail-card-head--peak {
  background: linear-gradient(135deg, #e9efed, #ffffff);
}

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

.tf-summary-box {
  display: grid;
  gap: 4px;
  margin-top: 10px;
  padding: 11px 12px;
  border: 1px dashed var(--border-brand);
  border-radius: 10px;
  background: #ffffff;
}

.tf-summary-box span {
  color: var(--text);
  font-size: 13px;
  font-weight: 900;
}

.tf-summary-box small {
  color: var(--muted);
  font-size: 11px;
  font-weight: 750;
  line-height: 1.35;
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
  .control-field--compact {
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

<template>
  <div class="fd-page" :class="isPeakDetail ? 'fd-page--peak' : 'fd-page--gene'">
    <div class="container">
      <!-- ===== Hero header ===== -->
      <div class="fd-hero-bar">
        <div>
          <div class="fd-kicker">{{ featureKicker }}</div>
          <h1>{{ mainTitle }}</h1>
          <div class="fd-hero-tags">
            <span class="fd-hero-tag">{{ genomeBuild }}</span>
            <span v-if="isPeakDetail && region" class="fd-hero-tag fd-hero-tag--region">{{ region }}</span>
            <template v-if="!isPeakDetail">
              <span v-if="geneBodyRegion" class="fd-hero-tag fd-hero-tag--region">Gene body: {{ geneBodyRegion }}</span>
              <span v-if="promoterRegion" class="fd-hero-tag fd-hero-tag--promoter">Promoter: {{ promoterRegion }}</span>
            </template>
          </div>
        </div>
        <button type="button" class="fd-back-btn" @click="goBack">{{ backLabel }}</button>
      </div>

      <!-- ===== Module A: Overview ===== -->
      <section class="fd-module">
        <div class="fd-module-inner">
          <div class="fd-occ-header">
            <div class="fd-section-label">Overview</div>
            <div class="fd-section-desc">{{ overviewSubtitle }}</div>
          </div>

          <el-skeleton v-if="occurrenceLoading" animated :rows="3" />
          <div v-else-if="occurrenceError" class="fd-hint">Occurrence request failed. Please retry.</div>
          <div v-else-if="!occurrenceData" class="fd-hint">Overview data will be shown after feature-level index APIs are integrated.</div>
          <div v-else-if="!occurrenceData.available" class="fd-hint">{{ occurrenceData.message || 'Overview data is not available.' }}</div>

          <template v-else>
            <div class="fd-dash-cards">
              <div class="fd-dash-card">
                <strong>{{ occurrenceData.datasetCount ?? 0 }}</strong>
                <span>Datasets</span>
              </div>
              <div class="fd-dash-card">
                <strong>{{ occurrenceData.cellTypeCount ?? 0 }}</strong>
                <span>Cell types</span>
              </div>
              <div class="fd-dash-card">
                <strong>{{ overviewClusterCount }}</strong>
                <span>Clusters</span>
              </div>
              <div class="fd-dash-card">
                <strong>{{ occurrenceData.totalOccurrences ?? 0 }}</strong>
                <span>Marker records</span>
              </div>
            </div>

            <div class="fd-chart-row" v-if="(occurrenceData.totalOccurrences ?? 0) > 0">
              <div class="fd-chart-box">
                <div class="fd-chart-title">Top 10 datasets</div>
                <button class="fd-chart-dl" title="Download chart" @click="openDownloadDialog('dataset')"><el-icon><Download /></el-icon></button>
                <div v-if="datasetRanking.length === 0" class="fd-chart-empty">No dataset data available</div>
                <div v-else ref="datasetChartEl" class="fd-chart-canvas" />
              </div>
              <div class="fd-chart-box">
                <div class="fd-chart-title">Top 10 cell types</div>
                <button class="fd-chart-dl" title="Download chart" @click="openDownloadDialog('cellContext')"><el-icon><Download /></el-icon></button>
                <div v-if="cellContextRanking.length === 0" class="fd-chart-empty">No cell type data available</div>
                <div v-else ref="cellContextChartEl" class="fd-chart-canvas" />
              </div>
            </div>

            <div v-if="!isPeakDetail" class="fd-expression-panel">
              <div class="fd-chart-row">
                <div class="fd-chart-box fd-chart-box--wide">
                  <div class="fd-expression-head">
                    <div class="fd-exp-left">
                      <div class="fd-chart-title">Expression profile</div>
                      <button class="fd-chart-dl fd-chart-dl--inline" title="Download chart" :disabled="expressionLoading || expressionData.length === 0" @click="openDownloadDialog('expression')"><el-icon><Download /></el-icon></button>
                    </div>
                    <div class="fd-exp-tabs">
                      <button v-for="p in expPlatforms" :key="p" type="button" class="fd-exp-tab" :class="{ active: expPlatform === p }" :disabled="expressionLoading" @click="switchExpPlatform(p)">{{ p }}</button>
                    </div>
                  </div>
                  <div v-if="expressionLoading" class="fd-chart-empty">Loading {{ expPlatform }} expression data...</div>
                  <div v-else-if="expressionData.length === 0" class="fd-chart-empty">No {{ expPlatform }} expression data available for this gene.</div>
                  <div v-else ref="expChartEl" class="fd-chart-canvas" />
                </div>
              </div>
            </div>
          </template>
        </div>
      </section>

      <!-- ===== Module B: Regulatory annotation ===== -->
      <section class="fd-module fd-ra-module" :aria-busy="intersectLoading">
        <div class="fd-module-inner" :inert="intersectLoading">
          <div class="fd-occ-header fd-ra-header">
            <div class="fd-ra-title-block">
              <div class="fd-section-label">Epi(genetic) Annotation</div>
              <div class="fd-section-desc">
                <template v-if="isPeakDetail">Epi(genetic) annotation sources overlapping {{ region || 'the selected peak region' }}.</template>
                <template v-else>Epi(genetic) annotation of <strong>{{ geneSymbol || 'this gene' }}</strong> using reference annotation sources.</template>
              </div>
            </div>
            <div v-if="!isPeakDetail" class="fd-ra-mode-actions">
              <div class="fd-ra-mode-tabs" role="tablist" aria-label="Epi(genetic) annotation view">
                <button
                  v-for="mode in regulatoryModeOptions"
                  :key="mode.value"
                  type="button"
                  class="fd-ra-mode-tab"
                  :class="{ active: selectedGeneRegMode === mode.value }"
                  :aria-selected="selectedGeneRegMode === mode.value"
                  :disabled="intersectLoading"
                  role="tab"
                  @click="selectGeneRegMode(mode.value)"
                >
                  {{ mode.label }}
                </button>
              </div>
            </div>
          </div>

          <div class="fd-ra-sources">
            <label v-for="source in annotationSourceCards" :key="source.mode || source.type" class="fd-ra-source" :class="{ selected: isAnnotationSourceSelected(source), disabled: !source.available || intersectLoading }" :title="source.label" @click="selectAnnotationSourceCard(source)">
              <span class="fd-ra-radio" aria-hidden="true"></span>
              <span class="fd-ra-main">
                <span class="fd-ra-label-row">
                  <span class="fd-ra-label">{{ source.label }}</span>
                  <span class="fd-ra-scope">{{ source.available ? 'reference' : 'future' }}</span>
                </span>
                <span class="fd-ra-reason" v-if="source.available">Using {{ source.label }} reference for annotation</span>
                <span class="fd-ra-reason" v-else>{{ source.reason || source.status }}</span>
              </span>
              <span class="fd-ra-status" :class="source.available ? 'ready' : 'unavailable'">{{ source.available ? (source.status || 'READY') : 'PENDING' }}</span>
            </label>
          </div>

          <div v-if="!hasAnnotationContext" class="fd-hint">{{ annotationContextMessage }}</div>
          <template v-else>
            <div v-if="!raLoaded" class="fd-hint">
              Choose a reference source to load annotation results.
            </div>
            <template v-else>
            <div v-if="intersectError" class="fd-ra-error">
              {{ intersectError }}
              <button v-if="intersectErrorStatus === 409" type="button" class="fd-ra-refresh" @click="loadAnnotationOverlap">Refresh</button>
            </div>
            <div class="fd-ra-summary">
              <span>{{ annotationSummaryText }}</span>
              <el-button class="soft-button" :disabled="intersectLoading || intersectRecords.length === 0" @click="downloadOverlapCsv"><el-icon><Download /></el-icon><span>CSV</span></el-button>
            </div>
            <el-table :data="intersectRecords" stripe border class="detail-table fd-ra-overlap-table" size="small" v-loading="intersectLoading" element-loading-text="This operation may take a while — thank you for your patience." empty-text="No epi(genetic) annotation overlaps found">
              <el-table-column v-if="showQueryEnhancerColumns" min-width="200">
                <template #header><span class="fd-col-header">{{ enhancerRegionLabel }} <el-tooltip placement="top" effect="light" :show-after="200"><template #content><div><div v-for="line in enhancerRegionTooltipLines" :key="line">{{ line }}</div></div></template><span class="fd-col-help">?</span></el-tooltip></span></template>
                <template #default="{ row }"><span class="fd-mono" :title="getQueryEnhancerRegion(row)">{{ getQueryEnhancerRegion(row) }}</span></template>
              </el-table-column>
              <el-table-column v-if="showQueryEnhancerColumns" min-width="150">
                <template #header><span class="fd-col-header">Cell type <el-tooltip placement="top" effect="light" :show-after="200"><template #content><div><div>This is the tissue or cell label stored with the enhancer-to-gene record.</div><div>It describes the enhancer source and is not taken from the selected comparison track.</div></div></template><span class="fd-col-help">?</span></el-tooltip></span></template>
                <template #default="{ row }"><span :title="getCellType(row)">{{ getCellType(row) }}</span></template>
              </el-table-column>
              <el-table-column min-width="190">
                <template #header><span class="fd-col-header">Reference match <el-tooltip placement="top" effect="light" :show-after="200"><template #content><div><div>Genomic interval from the selected comparison track that overlaps the queried gene, promoter, enhancer, or peak region.</div><div>One row is shown for each returned overlap. Table pagination only changes the visible page and does not cut the downloaded result.</div></div></template><span class="fd-col-help">?</span></el-tooltip></span></template>
                <template #default="{ row }"><span class="fd-mono" :title="getLocation(row)">{{ getLocation(row) }}</span></template>
              </el-table-column>
              <el-table-column v-if="showFeatureColumn" :label="featureColumnLabel" min-width="150"><template #default="{ row }"><span :title="getFeature(row)">{{ getFeature(row) }}</span></template></el-table-column>
              <el-table-column v-for="col in sourceColumnDefs" :key="col.label" :label="col.label" min-width="130"><template #default="{ row }"><span :title="getRawField(row, col.idx)">{{ getRawField(row, col.idx) }}</span></template></el-table-column>
              <el-table-column label="Overlap" min-width="80" align="center"><template #default="{ row }">{{ getOverlap(row) }}</template></el-table-column>
            </el-table>
            <div v-if="intersectTotal > 0" class="pager">
              <el-pagination class="oscar-pagination" background layout="sizes, prev, pager, next" popper-class="oscar-select-popper" :total="intersectTotal" :page-sizes="[10, 20, 50]" :page-size="pageSize" :current-page="page" @size-change="onPageSizeChange" @current-change="onPageChange" />
            </div>
            </template>
          </template>
        </div>
        <transition name="fd-ra-lock">
          <div v-if="intersectLoading" class="fd-ra-lock" role="status" aria-live="polite">
            <div class="fd-ra-lock-card">
              <span class="fd-ra-lock-spinner" aria-hidden="true" />
              <div>
                <div class="fd-ra-lock-title">Loading epi(genetic) annotation</div>
                <div class="fd-ra-lock-copy">Some genes have many enhancer regions. Please wait for this reference search to finish before selecting another option.</div>
              </div>
            </div>
          </div>
        </transition>
      </section>
    </div>
  </div>

  <el-dialog
    v-model="dlDialogOpen"
    width="640px"
    :title="`Download ${dlDialogLabel}`"
    custom-class="bubble-dialog fd-download-dialog"
    modal-class="bubble-overlay"
    :append-to-body="true"
  >
    <div class="landscape-download-body">
      <div class="landscape-download-meta">
        <span>{{ dlDialogLabel }}</span>
      </div>
      <div class="landscape-download-grid">
        <button
          type="button"
          class="landscape-download-chip"
          :class="{ 'landscape-download-chip--loading': activeDlAction === 'image' }"
          :disabled="activeDlAction !== null"
          @click="runDlDialogDownload('image', dlDialogImage)"
        >
          <span class="landscape-download-chip-left">
            <span class="landscape-download-chip-name">Chart image</span>
            <span class="landscape-download-chip-format">PNG</span>
          </span>
          <span class="landscape-download-chip-action" aria-live="polite">
            <span v-if="activeDlAction === 'image'" class="landscape-download-spinner" aria-hidden="true" />
            {{ activeDlAction === 'image' ? 'Starting...' : 'Download' }}
          </span>
        </button>
        <button
          type="button"
          class="landscape-download-chip"
          :class="{ 'landscape-download-chip--loading': activeDlAction === 'table' }"
          :disabled="activeDlAction !== null"
          @click="runDlDialogDownload('table', dlDialogTable)"
        >
          <span class="landscape-download-chip-left">
            <span class="landscape-download-chip-name">Underlying data table</span>
            <span class="landscape-download-chip-format">CSV · {{ dlDialogKind === 'expression' ? 'Top 30' : 'Top 10' }}</span>
          </span>
          <span class="landscape-download-chip-action" aria-live="polite">
            <span v-if="activeDlAction === 'table'" class="landscape-download-spinner" aria-hidden="true" />
            {{ activeDlAction === 'table' ? 'Starting...' : 'Download' }}
          </span>
        </button>
        <button
          type="button"
          class="landscape-download-chip"
          :class="{ 'landscape-download-chip--loading': activeDlAction === 'full' }"
          :disabled="activeDlAction !== null"
          @click="runDlDialogDownload('full', dlDialogFullCsv)"
        >
          <span class="landscape-download-chip-left">
            <span class="landscape-download-chip-name">Full data table</span>
            <span class="landscape-download-chip-format">CSV · All</span>
          </span>
          <span class="landscape-download-chip-action" aria-live="polite">
            <span v-if="activeDlAction === 'full'" class="landscape-download-spinner" aria-hidden="true" />
            {{ activeDlAction === 'full' ? 'Starting...' : 'Download' }}
          </span>
        </button>
      </div>
    </div>
    <template #footer>
      <el-button @click="dlDialogOpen = false">Close</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Download } from "@element-plus/icons-vue";
import axios from "axios";
import * as echarts from "echarts";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { buildApiUrl } from "@/config/api";
import type { BedtoolsAnnotationType, BedtoolsOverlapRecord, BedtoolsSourceOption, BedtoolsSourcesResponse, FeatureOccurrenceResponse, FeatureRegulatoryAnnotationMode, SearchResultDomain } from "@/api/searchResult";
import { fetchFeatureOccurrence, fetchFeatureRegulatoryAnnotation, fetchReferenceSources, runReferenceIntersect } from "@/api/searchResult";
import { getBioChartColor, getBioChartColorMap } from "@/utils/chartPalette";

type FeatureType = "gene" | "peak";
type AnnotationSourceCard = {
  type: BedtoolsAnnotationType;
  label: string;
  available: boolean;
  status?: string | null;
  reason?: string | null;
  description?: string | null;
  mode?: FeatureRegulatoryAnnotationMode;
};
const PEAK_REG_SOURCE_TYPES: BedtoolsAnnotationType[] = ["risk_snp","common_snp","gtex_eqtl","tfbs","enhancer","super_enhancer","methylation","crispr","atac_peaks","3d_interactions","dnase_peaks","tad","erna","tf_chip_seq","tcof"];
const regulatoryModeOptions: Array<{ value: FeatureRegulatoryAnnotationMode; label: string; annotationType: BedtoolsAnnotationType }> = [
  { value: "promoter", label: "Promoter", annotationType: "tss_promoter" },
  { value: "super_enhancer", label: "Super Enhancer", annotationType: "super_enhancer" },
  { value: "typical_enhancer", label: "Typical Enhancer", annotationType: "enhancer" },
];
const SOURCE_LABELS: Record<string, string> = {
  gene:"Gene body",
  tss_promoter:"Promoter",
  risk_snp:"Risk SNP",
  common_snp:"Common SNP",
  gtex_eqtl:"GTEx eQTL",
  tfbs:"TFBS",
  enhancer:"Enhancer",
  super_enhancer:"Super Enhancer",
  methylation:"Methylation",
  crispr:"CRISPR",
  atac_peaks:"ATAC",
  "3d_interactions":"3D interactions",
  dnase_peaks:"DNase peaks",
  tad:"TAD",
  erna:"eRNA",
  tf_chip_seq:"TF-Chip-Seq",
  tcof:"T(co)F",
};
const SOURCE_FIELD_NAMES: Record<string, string[]> = {
  gene: ["Gene biotype", "Source"],
  tss_promoter: ["Promoter name", "Source"],
  common_snp: ["Ref", "Alt"], risk_snp: ["Ref", "Alt", "Gene", "Disease / Trait", "GWAS Type", "P value", "OR", "PubMed"],
  enhancer: ["Enhancer type", "Source"], super_enhancer: ["Tissue", "Cell type"], tfbs: ["Description"],
  gtex_eqtl: ["Ref", "Alt", "TSS Distance", "MAF", "P-value", "Tissue"],
  methylation: ["Biosample", "Source", "Beta Value"], crispr: ["Source"],
  atac_peaks: ["Peak name", "Score", "Source"],
  "3d_interactions": ["Interaction", "Partner", "Score"],
  dnase_peaks: ["Peak name", "Score", "Source"],
  tad: ["TAD ID", "Source"],
  erna: ["eRNA ID", "Gene", "Score"],
  tf_chip_seq: ["TF", "Cell type", "Source"],
  tcof: ["TF", "Cell type", "Source"],
};

const route = useRoute(); const router = useRouter();
const genomeBuild = "hg38"; const pageSize = ref(10);

const featureType = computed<FeatureType>(() => queryString("type").toLowerCase() === "peak" ? "peak" : "gene");
const isPeakDetail = computed(() => featureType.value === "peak");
const geneSymbol = computed(() => queryString("gene"));
const chrom = computed(() => queryString("chrom"));
const sv = computed(() => queryNumber("start")); const ev = computed(() => queryNumber("end"));
const strand = computed(() => queryString("strand"));
const datasetId = computed(() => queryString("datasetId"));
const domain = computed<SearchResultDomain>(() => "integration");
const detailSource = computed(() => queryString("source"));
const detailReturnTo = computed(() => queryString("returnTo"));
const backLabel = computed(() => {
  if (detailSource.value === "analysis_enrichment") return "Back to enrichment results";
  if (detailSource.value === "analysis_sequence") return "Back to analysis results";
  if (detailSource.value.startsWith("sample_") || datasetId.value) return "Back to sample details";
  return "Back";
});
const featureKicker = computed(() => isPeakDetail.value ? "Peak detail" : "Gene detail");
const region = computed(() => chrom.value && sv.value !== null && ev.value !== null ? `${chrom.value}:${sv.value}-${ev.value}` : "");
const mainTitle = computed(() => isPeakDetail.value ? (region.value || "Peak") : (geneSymbol.value || "Gene"));

const overviewSubtitle = computed(() => isPeakDetail.value
  ? "Marker peak occurrence landscape across OSCAR datasets and cell types."
  : "Marker gene occurrence landscape across OSCAR datasets and cell types.");

/* ---- Overview data ---- */
const occurrenceLoading = ref(false);
const occurrenceData = ref<FeatureOccurrenceResponse | null>(null);
const occurrenceError = ref(false);

const overviewClusterCount = computed(() => occurrenceData.value?.clusterCount ?? 0);
const geneBodyRegion = computed(() => occurrenceData.value?.geneBodyRegion ?? "");
const promoterRegion = computed(() => occurrenceData.value?.promoterRegion ?? "");

const datasetRanking = computed(() => occurrenceData.value?.datasetRanking?.map(d => ({
  datasetId: d.datasetId || "Unknown",
  sampleName: d.sampleName || "",
  recordCount: d.recordCount ?? 0,
  cellContextCount: d.cellContextCount ?? 0,
  clusterCount: d.clusterCount ?? 0,
})) ?? []);

const cellContextRanking = computed(() => occurrenceData.value?.cellContextRanking?.map(c => ({
  cellType: c.cellType || "Unknown",
  recordCount: c.recordCount ?? 0,
  datasetCount: c.datasetCount ?? 0,
  clusterCount: c.clusterCount ?? 0,
})) ?? []);


/* ---- Regulatory annotation ---- */
const loadedSources = ref<BedtoolsSourceOption[]>([]);
const intersectLoading = ref(false); const intersectTotal = ref(0);
const intersectRecords = ref<BedtoolsOverlapRecord[]>([]);
const allIntersectRecords = ref<BedtoolsOverlapRecord[]>([]);
const intersectSummary = ref<any>(null);
const intersectError = ref(""); const intersectErrorStatus = ref<number | null>(null);
const raLoaded = ref(false);
const page = ref(1); const raSelectedType = ref<BedtoolsAnnotationType | "">("");
const selectedGeneRegMode = ref<FeatureRegulatoryAnnotationMode | "">("promoter");
const selectedGeneAnnotationType = ref<BedtoolsAnnotationType | "">("");

const activeAnnotationType = computed<BedtoolsAnnotationType | "">(() => isPeakDetail.value ? raSelectedType.value : selectedGeneAnnotationType.value);
const hasAnnotationContext = computed(() => isPeakDetail.value ? !!region.value : !!geneSymbol.value);
const annotationContextMessage = computed(() => isPeakDetail.value ? "No genomic region was provided for epi(genetic) annotation." : "No gene symbol was provided for epi(genetic) annotation.");
const showQueryEnhancerColumns = computed(() => !isPeakDetail.value && (selectedGeneRegMode.value === "super_enhancer" || selectedGeneRegMode.value === "typical_enhancer"));
const enhancerRegionLabel = computed(() => selectedGeneRegMode.value === "super_enhancer" ? "Super Enhancer region" : "Typical Enhancer region");
const enhancerRegionTooltipLines = computed(() => {
  const type = selectedGeneRegMode.value === "super_enhancer" ? "super enhancer" : "typical enhancer";
  return [
    `A ${type} linked to this gene in the OSCAR enhancer-to-gene data. All returned enhancer regions are checked, not only the rows visible on the current table page.`,
    "Each enhancer region is then compared with the selected annotation track by genomic overlap.",
  ];
});
const showFeatureColumn = computed(() => activeAnnotationType.value !== "crispr" && !(isPeakDetail.value && activeAnnotationType.value === "enhancer"));

const raSources = computed<AnnotationSourceCard[]>(() => PEAK_REG_SOURCE_TYPES.map(type => {
  const s = loadedSources.value.find(x => x.type === type);
  return { type, label: SOURCE_LABELS[type]||type, available: s?.available??false, status: s?.status??"PENDING", reason: s?.reason??null, description: s?.description??null };
}));
const geneRaSources = computed<AnnotationSourceCard[]>(() => PEAK_REG_SOURCE_TYPES.map(type => {
  const s = loadedSources.value.find(x => x.type === type);
  return { type, label: SOURCE_LABELS[type]||type, available: s?.available??false, status: s?.status??"PENDING", reason: s?.reason??null, description: s?.description??null };
}));
const annotationSourceCards = computed<AnnotationSourceCard[]>(() => isPeakDetail.value ? raSources.value : geneRaSources.value);

const sourceColumnDefs = computed(() => { const names = SOURCE_FIELD_NAMES[activeAnnotationType.value] || []; return names.map((label, idx) => ({ label, idx })).filter(col => intersectRecords.value.some(row => getRawField(row, col.idx) !== "-")); });
const featureColumnLabel = computed(() => { switch(activeAnnotationType.value){ case"gene":return"Reference gene";case"tss_promoter":return"Reference promoter";case"common_snp":case"risk_snp":return"SNP ID";case"tfbs":return"TF / Motif";case"enhancer":return"Element";case"super_enhancer":return"SE ID";case"gtex_eqtl":return"Gene";case"methylation":return"CpG ID";case"atac_peaks":return"ATAC peak";case"3d_interactions":return"Interaction";case"dnase_peaks":return"Original sample";case"tad":return"Original sample";case"erna":return"eRNA frequency in eRNAbase";case"tf_chip_seq":return"TF";case"tcof":return"TF";default:return"Feature";} });
const annotationSummaryText = computed(() => { const n = intersectRecords.value.length; if (n === 0) return "No overlaps found"; return `Showing ${n} of ${intersectTotal.value} overlaps`; });

function getRawField(row: any, idx: number): string { const f: unknown[] = Array.isArray(row.rawFields)?row.rawFields:[]; return cleanText(f[idx])||"-"; }
function getFeature(row: any): string { const v = cleanText(row.name||row.featureName||row.featureId||row.gene||row.geneName||row.transcriptId||row.feature||""); return v && v!=="." ? v : "-"; }
function getLocation(row: any): string { const c = cleanText(row.chrom||row.chromosome||row.featureRegion); if(c)return c; const rs=row.start!=null?row.start:""; const re=row.end!=null?row.end:""; return rs!==""&&re!==""?`${rs}-${re}`:"-"; }
function getOverlap(row: any): string { const bp=row.overlapBp??row.overlap??row.overlap_bp; return bp!=null?String(bp):"--"; }
function getQueryEnhancerRegion(row: any): string { return cleanText(row.queryEnhancerRegion||row.queryRegion)||"-"; }
function getCellType(row: any): string { return cleanText(row.cellType||row.cell_type||row.biosampleName||row.biosample)||"-"; }
/* ---- Chart state ---- */
const datasetChartEl = ref<HTMLElement|null>(null);
const cellContextChartEl = ref<HTMLElement|null>(null);
let datasetChart: echarts.ECharts|null = null;
let cellContextChart: echarts.ECharts|null = null;
let chartResizers: ResizeObserver[] = [];

function initChart(elRef: any): { el: HTMLElement; chart: echarts.ECharts }|null {
  const el = elRef.value as HTMLElement|null;
  if (!el) return null;
  const chart = echarts.init(el);
  const ro = new ResizeObserver(() => { requestAnimationFrame(() => chart.resize()); });
  ro.observe(el);
  chartResizers.push(ro);
  return { el, chart };
}

function renderDatasetChart() {
  if (!datasetChartEl.value || datasetRanking.value.length === 0) return;
  if (!datasetChart) { const r = initChart(datasetChartEl); if (!r) return; datasetChart = r.chart; }
  const data = datasetRanking.value;
  const yData = data.map(d => d.datasetId);
  const yTooltips = data.map(d => d.sampleName ? `${d.datasetId} / ${d.sampleName}` : d.datasetId);
  const seriesData = data.map(d=>d.recordCount);
  const len = seriesData.length;
  datasetChart.setOption({
    tooltip: { trigger:"axis", axisPointer:{type:"shadow"}, formatter:(p:any) => { const idx = p[0]?.dataIndex; return `${yTooltips[idx] ?? ""}<br/>Records: ${seriesData[idx] ?? ""}`; } },
    grid: { left:100, right:56, top:10, bottom:30 },
    xAxis: { type:"value", axisLabel:{ color:"#5E6C67" } },
    yAxis: { type:"category", data: yData, axisLabel:{ color:"#5E6C67", width:90, overflow:"truncate" }, inverse:true },
    series: [{ type:"bar", barMaxWidth:24,
      data: seriesData.map((v, i) => ({ value: v, itemStyle: { color: getBioChartColor(len - 1 - i), borderRadius: [0,4,4,0] } })),
      label: { show: true, position: "right", color: "#5E6C67", fontSize: 11, fontWeight: 700, formatter: "{c}" } }],
  }, true);
}

function renderCellContextChart() {
  if (!cellContextChartEl.value || cellContextRanking.value.length === 0) return;
  if (!cellContextChart) { const r = initChart(cellContextChartEl); if (!r) return; cellContextChart = r.chart; }
  const data = cellContextRanking.value;
  const colorMap = getBioChartColorMap(data.map(d=>d.cellType));
  cellContextChart.setOption({
    color: data.map((d,i)=>colorMap.get(d.cellType)??getBioChartColor(i)),
    tooltip: { trigger:"item", formatter:(p:any) => `${p.name}<br/>Count: ${p.value}<br/>Ratio: ${p.percent}%` },
    series: [{ type:"pie", radius:["42%","70%"], center:["50%","45%"], avoidLabelOverlap:true, itemStyle:{ borderRadius:3, borderColor:"#fff", borderWidth:1 },
      label: { color:"#5E6C67", fontSize:12, formatter: "{b}: {c} ({d}%)" }, data: data.map(d=>({ name:d.cellType, value:d.recordCount })) }],
  }, true);
}

watch([datasetRanking, cellContextRanking], async () => { await nextTick(); renderDatasetChart(); renderCellContextChart(); });

/* ---- Loaders ---- */
async function loadSources() { try{ const data: BedtoolsSourcesResponse = await fetchReferenceSources({genomeBuild}); loadedSources.value = data.sources??[]; }catch(err){ console.error("[FeatureDetail] sources:",err); } }
function sliceCurrentPage() {
  const all = allIntersectRecords.value;
  const start = (page.value - 1) * pageSize.value;
  intersectRecords.value = all.slice(start, start + pageSize.value);
  intersectTotal.value = all.length;
}
async function loadAnnotationOverlap() {
  if(intersectLoading.value||!hasAnnotationContext.value)return;
  const geneMode = selectedGeneRegMode.value;
  const geneAnnotationType = selectedGeneAnnotationType.value;
  if(isPeakDetail.value ? !raSelectedType.value : (!geneMode || !geneAnnotationType))return;
  raLoaded.value=true; intersectLoading.value=true; intersectError.value=""; intersectErrorStatus.value=null; try{
  const data = isPeakDetail.value ? await loadPeakAnnotationOverlap() : await fetchFeatureRegulatoryAnnotation({gene:geneSymbol.value,chrom:chrom.value||undefined,start:sv.value??undefined,end:ev.value??undefined,strand:strand.value||undefined,mode:geneMode as FeatureRegulatoryAnnotationMode,annotationType:geneAnnotationType as BedtoolsAnnotationType,domain:domain.value,genomeBuild});
  if (!isPeakDetail.value) {
    allIntersectRecords.value = data.records ?? [];
    sliceCurrentPage();
  } else {
    allIntersectRecords.value = [];
    intersectRecords.value = data.records ?? [];
    intersectTotal.value = data.total ?? (data.records?.length ?? 0);
  }
  intersectSummary.value = data.summary ?? null;
}catch(err:any){ const st=err?.response?.status; intersectErrorStatus.value=st??null; intersectError.value=st===400?"Region or parameter format error.":st===409?"Some annotation sources are unavailable.":`Request failed: ${err?.message||"Unknown error"}`; intersectRecords.value=[]; intersectTotal.value=0; allIntersectRecords.value=[]; intersectSummary.value=null; }finally{ intersectLoading.value=false; } }
async function loadPeakAnnotationOverlap() { const type=raSelectedType.value; if(!type) return { records: [], total: 0, summary: null }; return runReferenceIntersect({genomeBuild,region:region.value,annotationTypes:[type],minOverlapBp:1,page:page.value,pageSize:pageSize.value}); }
function resetAnnotationResults(){ intersectRecords.value=[]; allIntersectRecords.value=[]; intersectTotal.value=0; intersectSummary.value=null; intersectError.value=""; intersectErrorStatus.value=null; raLoaded.value=false; }
function selectRaSource(type:BedtoolsAnnotationType){ if(intersectLoading.value)return; const s=raSources.value.find(x=>x.type===type); if(!s?.available)return; const changed=raSelectedType.value!==type; if(changed){ raSelectedType.value=type; page.value=1; resetAnnotationResults(); } if(changed||!raLoaded.value||!!intersectError.value)loadAnnotationOverlap(); }
function selectGeneRegMode(mode:FeatureRegulatoryAnnotationMode){ if(intersectLoading.value)return; const changed=selectedGeneRegMode.value!==mode; if(changed){ selectedGeneRegMode.value=mode; page.value=1; resetAnnotationResults(); } if(selectedGeneAnnotationType.value&&(changed||!raLoaded.value||!!intersectError.value))loadAnnotationOverlap(); }
function isAnnotationSourceSelected(source: AnnotationSourceCard){ return isPeakDetail.value ? raSelectedType.value===source.type : selectedGeneAnnotationType.value===source.type; }
function selectAnnotationSourceCard(source: AnnotationSourceCard){ if(intersectLoading.value||!source.available)return; if(isPeakDetail.value){ selectRaSource(source.type); return; } selectGeneAnnotationType(source.type); }
function selectGeneAnnotationType(type: BedtoolsAnnotationType){ if(intersectLoading.value)return; const changed=selectedGeneAnnotationType.value!==type; if(changed){ selectedGeneAnnotationType.value=type; page.value=1; resetAnnotationResults(); } if(selectedGeneRegMode.value&&(changed||!raLoaded.value||!!intersectError.value))loadAnnotationOverlap(); }
function onPageChange(p:number){ if(intersectLoading.value)return; page.value=p; if(raLoaded.value){ if(!isPeakDetail.value)sliceCurrentPage(); else loadAnnotationOverlap(); } }
function onPageSizeChange(sz:number){ if(intersectLoading.value)return; pageSize.value=sz; page.value=1; if(raLoaded.value){ if(!isPeakDetail.value)sliceCurrentPage(); else loadAnnotationOverlap(); } }

/* ---- Expression profile ---- */
const expChartEl = ref<HTMLElement|null>(null);
let expChart: echarts.ECharts|null = null;
let expChartResizeObserver: ResizeObserver|null = null;
const expPlatform = ref("GTEX");
const expPlatforms = ["GTEX", "CCLE", "ENCODE", "TCGA"];
const expressionLoading = ref(false);
const expressionData = ref<any[]>([]);

function switchExpPlatform(p: string) {
  if (expPlatform.value === p) return;
  expPlatform.value = p;
  void loadExpression();
}

function expressionSampleName(row: any): string {
  return (row.sample_name ?? row.sampleName ?? row.sample ?? row.tissue ?? "Unknown") as string;
}

function expressionValue(row: any): number {
  const v = Number(row.expression_value ?? row.expressionValue ?? row.value ?? row.tpm);
  return Number.isFinite(v) ? v : 0;
}

function renderExpressionChart() {
  const el = expChartEl.value;
  if (!el || expressionData.value.length === 0) return;
  const data = [...expressionData.value]
    .sort((a: any, b: any) => expressionValue(b) - expressionValue(a));
  expChartResizeObserver?.disconnect();
  expChartResizeObserver = null;
  if (expChart) { expChart.dispose(); expChart = null; }
  expChart = echarts.init(el);
  expChartResizeObserver = new ResizeObserver(() => { requestAnimationFrame(() => expChart?.resize()); });
  expChartResizeObserver.observe(el);
  if (data.length === 0) { expChart.clear(); return; }
  const barColors = data.map((_: any, idx: number) => getBioChartColor(idx));
  expChart.setOption({
    color: barColors,
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: 76, right: 24, top: 28, bottom: 82 },
    xAxis: {
      type: "category",
      data: data.map(expressionSampleName),
      axisLabel: { color: "#5E6C67", interval: 0, rotate: 34, width: 82, overflow: "truncate" },
      axisTick: { alignWithLabel: true },
    },
    yAxis: {
      type: "value",
      name: "Expression (TPM/FPKM)",
      axisLabel: { color: "#5E6C67" },
      splitLine: { lineStyle: { color: "rgba(143,165,156,0.22)" } },
    },
    series: [{ type: "bar", data: data.map((row: any, idx: number) => ({ value: expressionValue(row), itemStyle: { color: barColors[idx] } })), barMaxWidth: 18, itemStyle: { borderRadius: [4, 4, 0, 0] } }],
  }, true);
}

async function loadExpression() {
  const g = geneSymbol.value; if (!g) return;
  expressionLoading.value = true;
  try {
    const { data } = await axios.get(buildApiUrl("api/feature-detail/expression"), {
      params: { gene: g, platform: expPlatform.value }
    });
    expressionData.value = data ?? [];
  } catch (err) {
    console.error("[FeatureDetail] expression:", err);
    expressionData.value = [];
  } finally { expressionLoading.value = false; }
}

watch([expressionData, expPlatform, occurrenceData, expressionLoading], () => { if (!expressionLoading.value && occurrenceData.value && expressionData.value.length > 0) nextTick(renderExpressionChart); }, { deep: true, flush: "post" });

async function loadOccurrence() { occurrenceLoading.value=true; occurrenceError.value=false; occurrenceData.value=null; try{ if(isPeakDetail.value){ if(!chrom.value||sv.value===null||ev.value===null)return; occurrenceData.value=await fetchFeatureOccurrence({type:"peak",chrom:chrom.value,start:sv.value,end:ev.value,domain:domain.value}); }else{ const g=geneSymbol.value; if(!g)return; occurrenceData.value=await fetchFeatureOccurrence({type:"gene",gene:g,chrom:chrom.value||undefined,start:sv.value??undefined,end:ev.value??undefined,strand:strand.value||undefined,domain:domain.value}); } }catch(err){ console.error("[FeatureDetail] occurrence:",err); occurrenceError.value=true; }finally{ occurrenceLoading.value=false; } }

/* ---- Download dialog ---- */
type DownloadDialogKind = "dataset" | "cellContext" | "expression";
type DownloadDialogAction = "image" | "table" | "full";
const dlDialogOpen = ref(false);
const dlDialogKind = ref<DownloadDialogKind>("dataset");
const activeDlAction = ref<DownloadDialogAction | null>(null);
let dlFeedbackTimer: number | undefined;
const dlDialogLabel = computed(() => {
  switch (dlDialogKind.value) {
    case "dataset": return "Top 10 datasets";
    case "cellContext": return "Top 10 cell types";
    case "expression": return `Expression profile (${expPlatform.value})`;
  }
});

function openDownloadDialog(kind: DownloadDialogKind) {
  dlDialogKind.value = kind;
  dlDialogOpen.value = true;
}

async function runDlDialogDownload(
  action: DownloadDialogAction,
  download: () => boolean | Promise<boolean>
) {
  if (activeDlAction.value !== null) return;
  activeDlAction.value = action;
  await nextTick();
  const startedAt = performance.now();
  let started = false;
  try {
    started = await download();
  } finally {
    const remaining = Math.max(0, 900 - (performance.now() - startedAt));
    window.clearTimeout(dlFeedbackTimer);
    dlFeedbackTimer = window.setTimeout(() => {
      activeDlAction.value = null;
      if (started) dlDialogOpen.value = false;
      dlFeedbackTimer = undefined;
    }, remaining);
  }
}

function dlDialogImage() {
  let chart: echarts.ECharts | null = null;
  switch (dlDialogKind.value) {
    case "dataset": chart = datasetChart; break;
    case "cellContext": chart = cellContextChart; break;
    case "expression": chart = expChart; break;
  }
  if (!chart) return false;
  const url = chart.getDataURL({ type:"png", pixelRatio:2, backgroundColor:"#fff" });
  const a = document.createElement("a"); a.href = url; a.download = `${dlDialogLabel.value.replace(/\s+/g, "_")}.png`; a.click();
  return true;
}

function dlDialogTable() {
  let headers: string[] = [];
  let rows: string[][] = [];
  let filename = "";
  switch (dlDialogKind.value) {
    case "dataset": {
      filename = "marker_records_by_dataset.csv";
      headers = ["Dataset ID", "Sample Name", "Records", "Cell types", "Clusters"];
      rows = datasetRanking.value.map(d => [d.datasetId, d.sampleName, String(d.recordCount), String(d.cellContextCount), String(d.clusterCount)]);
      break;
    }
    case "cellContext": {
      filename = "marker_records_by_cell_type.csv";
      headers = ["Cell Type", "Records", "Datasets", "Clusters"];
      rows = cellContextRanking.value.map(c => [c.cellType, String(c.recordCount), String(c.datasetCount), String(c.clusterCount)]);
      break;
    }
    case "expression": {
      filename = `expression_${expPlatform.value}_top30.csv`;
      const data = expressionData.value;
      headers = ["Platform", "Sample", "Expression Value"];
      rows = data.map((row: any) => [row.platform || "", row.sample_name || row.sampleName || "", String(row.expression_value ?? row.expressionValue ?? "")]);
      break;
    }
  }
  if (rows.length === 0) return false;
  const csv = [headers, ...rows].map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\n");
  const blob = new Blob([csv], { type:"text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a"); a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
  return true;
}

async function dlDialogFullCsv() {
  try {
    let headers: string[] = [];
    let rows: string[][] = [];
    let filename = "";
    if (dlDialogKind.value === "expression") {
      const { data } = await axios.get(buildApiUrl("api/feature-detail/expression"), {
        params: { gene: geneSymbol.value, platform: expPlatform.value, full: true }
      });
      filename = `expression_${expPlatform.value}_full.csv`;
      headers = ["Platform", "Sample", "Expression Value"];
      rows = (data ?? []).map((row: any) => [
        row.platform || "",
        row.sample_name || row.sampleName || "",
        String(row.expression_value ?? row.expressionValue ?? "")
      ]);
    } else {
      const params: any = { type: featureType.value, domain: domain.value, full: true };
      if (isPeakDetail.value) {
        params.chrom = chrom.value;
        params.start = sv.value;
        params.end = ev.value;
      } else {
        params.gene = geneSymbol.value;
        params.chrom = chrom.value || undefined;
        params.start = sv.value ?? undefined;
        params.end = ev.value ?? undefined;
        params.strand = strand.value || undefined;
      }
      const fullData = await fetchFeatureOccurrence(params);
      if (dlDialogKind.value === "dataset") {
        filename = "marker_records_by_dataset_full.csv";
        headers = ["Dataset ID", "Sample Name", "Records", "Cell types", "Clusters"];
        rows = (fullData.datasetRanking ?? []).map(d => [d.datasetId ?? "Unknown", d.sampleName ?? "", String(d.recordCount ?? 0), String(d.cellContextCount ?? 0), String(d.clusterCount ?? 0)]);
      } else {
        filename = "marker_records_by_cell_type_full.csv";
        headers = ["Cell Type", "Records", "Datasets", "Clusters"];
        rows = (fullData.cellContextRanking ?? []).map(c => [c.cellType ?? "Unknown", String(c.recordCount ?? 0), String(c.datasetCount ?? 0), String(c.clusterCount ?? 0)]);
      }
    }
    if (rows.length === 0) return false;
    const csv = [headers, ...rows].map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a"); a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
    return true;
  } catch (err) {
    console.error("[FeatureDetail] full download:", err);
    return false;
  }
}

async function downloadOverlapCsv() {
  let records: BedtoolsOverlapRecord[];
  if (!isPeakDetail.value && allIntersectRecords.value.length > 0) {
    records = allIntersectRecords.value;
  } else if (intersectRecords.value.length > 0) {
    records = intersectRecords.value;
  } else {
    return;
  }
  try {
    const headers = [
      ...(showQueryEnhancerColumns.value ? [enhancerRegionLabel.value, "Cell type"] : []),
      "Reference match",
      ...(showFeatureColumn.value ? [featureColumnLabel.value] : []),
      ...sourceColumnDefs.value.map(c => c.label),
      "Overlap",
    ];
    const rows = records.map(r => [
      ...(showQueryEnhancerColumns.value ? [getQueryEnhancerRegion(r), getCellType(r)] : []),
      getLocation(r),
      ...(showFeatureColumn.value ? [getFeature(r)] : []),
      ...sourceColumnDefs.value.map(c => getRawField(r, c.idx)),
      getOverlap(r),
    ]);
    const csv = [headers, ...rows].map(row => row.map(v => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${mainTitle.value}_regulatory_annotation.csv`;
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    /* silently ignore download errors */
  }
}

function queryString(name:string):string{ const v=route.query[name]; return String(Array.isArray(v)?v[0]??"":v??"").trim(); }
function queryNumber(name:string):number|null{ const v=Number(queryString(name)); return Number.isFinite(v)?v:null; }
function cleanText(value:unknown):string{ const t=String(value??"").trim(); return t||""; }
function safeReturnPath(value:string):string{ return value.startsWith("/")&&!value.startsWith("//")?value:""; }
function goBack(){ const returnPath=safeReturnPath(detailReturnTo.value); if(returnPath){ router.push(returnPath); return; } if(datasetId.value){ const src = detailSource.value.startsWith("analysis") ? "analysis" : detailSource.value.startsWith("sample_") ? "search" : ""; router.push({name:"SampleDetail",params:{id:datasetId.value},query:{domain:domain.value, ...(src ? {source:src} : {})}}); return; } router.back(); }

watch([region,occurrenceData], async () => { await nextTick(); renderDatasetChart(); renderCellContextChart(); });
onMounted(()=>{ void loadSources(); loadOccurrence(); if(!isPeakDetail.value) loadExpression(); });
onBeforeUnmount(()=>{ datasetChart?.dispose(); cellContextChart?.dispose(); expChart?.dispose(); expChartResizeObserver?.disconnect(); chartResizers.forEach(r=>r.disconnect()); window.clearTimeout(dlFeedbackTimer); });
</script>

<style scoped>
.fd-page { width:100%; padding:20px 0 32px; background:#fbfcfb; }
.fd-page--gene { background:linear-gradient(180deg, rgba(198,212,206,0.42) 0%, rgba(255,255,255,0) 320px), #fbfcfb; }
.fd-page--peak { background:linear-gradient(180deg, rgba(233,239,237,0.5) 0%, rgba(255,255,255,0) 320px), #fbfcfb; }

.fd-hero-bar { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; padding:10px 0 16px; margin-bottom:14px; }
.fd-kicker { color:var(--brand-primary-3); font-size:13px; font-weight:950; text-transform:uppercase; letter-spacing:0.04em; }
.fd-hero-bar h1 { margin:4px 0 12px; font-size:36px; font-weight:950; line-height:1.1; color:#1a2623; overflow-wrap:anywhere; }
.fd-hero-tags { display:flex; gap:8px; flex-wrap:wrap; }
.fd-hero-tag { display:inline-flex; align-items:center; min-height:30px; padding:6px 12px; border:1px solid var(--nav-active-border); border-radius:999px; background:var(--nav-active-bg); color:var(--nav-active-text); box-shadow:0 8px 18px rgba(27,92,84,0.1); font-size:13px; font-weight:800; line-height:1; }
.fd-hero-tag--region,
.fd-hero-tag--promoter { font-weight:900; }
.fd-back-btn { appearance:none; flex-shrink:0; display:inline-flex; align-items:center; gap:6px; min-height:34px; padding:0 14px; border:1px solid var(--border); border-radius:8px; background:var(--surface); color:var(--text); cursor:pointer; font-size:12px; font-weight:900; white-space:nowrap; transition:border-color 0.18s ease, color 0.18s ease; }
.fd-back-btn:hover { border-color:var(--border-brand); color:var(--brand-primary-3); }

.fd-module { margin-bottom:14px; }
.fd-module-inner { padding:18px; border:1px solid var(--border); border-radius:var(--radius-lg); background:var(--surface); box-shadow:var(--shadow-card); transition:box-shadow 0.18s ease, border-color 0.18s ease; }
.fd-module-inner:hover { border-color:var(--border-brand); box-shadow:var(--shadow-hover); }
.fd-ra-module { position:relative; }
.fd-ra-module[aria-busy="true"] .fd-module-inner { pointer-events:none; user-select:none; }
.fd-ra-lock { position:absolute; inset:0; z-index:20; display:flex; align-items:center; justify-content:center; padding:20px; border-radius:var(--radius-lg); background:rgba(247,251,249,0.76); backdrop-filter:blur(2px); }
.fd-ra-lock-card { display:flex; align-items:center; gap:14px; width:min(560px,100%); padding:18px 20px; border:1px solid rgba(71,112,97,0.24); border-radius:14px; background:rgba(255,255,255,0.97); box-shadow:0 16px 38px rgba(35,66,56,0.16); }
.fd-ra-lock-spinner { flex:0 0 auto; width:30px; height:30px; border:3px solid rgba(67,108,93,0.18); border-top-color:var(--brand-primary-3); border-radius:50%; animation:fdRaSpin 0.8s linear infinite; }
.fd-ra-lock-title { color:var(--text); font-size:14px; font-weight:900; }
.fd-ra-lock-copy { margin-top:4px; color:var(--muted); font-size:12px; line-height:1.45; }
.fd-ra-lock-enter-active, .fd-ra-lock-leave-active { transition:opacity 0.16s ease; }
.fd-ra-lock-enter-from, .fd-ra-lock-leave-to { opacity:0; }

.fd-occ-header { margin-bottom:14px; }
.fd-section-label { font-size:18px; font-weight:900; color:var(--text); }
.fd-section-desc { margin-top:4px; color:var(--muted); font-size:13px; line-height:1.35; }
.fd-ra-header { display:grid; grid-template-columns:minmax(0,1fr) auto minmax(0,1fr); align-items:center; gap:14px; min-height:54px; }
.fd-ra-title-block { grid-column:1; min-width:0; }
.fd-ra-mode-actions { grid-column:2; justify-self:center; display:inline-flex; align-items:center; justify-content:center; }
.fd-ra-mode-tabs { display:inline-flex; align-items:center; justify-content:center; gap:4px; min-height:44px; padding:4px; overflow:hidden; border:1px solid rgba(143,165,156,0.22); border-radius:999px; background:#f5faf8; box-shadow:inset 0 1px 2px rgba(15,23,42,0.035); }
.fd-ra-mode-tab { appearance:none; box-sizing:border-box; display:inline-flex; align-items:center; justify-content:center; min-height:36px; min-width:116px; margin:0; padding:7px 17px; border:0; border-radius:999px; background:transparent; color:rgba(74,96,88,0.78); cursor:pointer; font:inherit; font-size:13.75px; font-weight:760; line-height:1; white-space:nowrap; transition:color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease; }
.fd-ra-mode-tab:hover { color:var(--brand-primary-3); }
.fd-ra-mode-tab.active { background:#c7d8d2; color:#173f38; box-shadow:0 6px 16px rgba(95,125,112,0.18), inset 0 1px 0 rgba(255,255,255,0.42); font-weight:900; }
.fd-ra-mode-tab:focus-visible { outline:2px solid var(--border-brand); outline-offset:2px; }

/* Dashboard cards */
.fd-dash-cards { display:flex; justify-content:space-between; margin-bottom:18px; padding:16px 4px; border-bottom:1px solid rgba(143,165,156,0.3); box-shadow:0 1px 0 rgba(255,255,255,0.6); }
.fd-dash-card { flex:1; text-align:center; cursor:default; }
.fd-dash-card strong { display:block; font-size:36px; font-weight:950; color:var(--text); line-height:1.15; transition:color 0.18s ease, transform 0.18s ease; }
.fd-dash-card:hover strong { color:var(--brand-primary-3); transform:translateY(-3px); }
.fd-dash-card span { display:block; margin-top:4px; font-size:12px; font-weight:800; color:var(--muted); text-transform:uppercase; }

/* Charts */
.fd-chart-row { display:flex; justify-content:space-between; margin-bottom:18px; }
.fd-chart-box { width:46%; position:relative; }
.fd-chart-title { font-size:13px; font-weight:900; color:var(--text); margin-bottom:8px; }
.fd-exp-left .fd-chart-title { margin-bottom:0; }
.fd-chart-canvas { width:100%; height:400px; }
.fd-chart-empty { display:flex; align-items:center; justify-content:center; height:200px; color:var(--muted); font-size:13px; font-weight:700; }
.fd-chart-dl { appearance:none; position:absolute; top:-6px; right:0; z-index:2; display:inline-flex; align-items:center; justify-content:center; width:32px; height:32px; padding:0; border:1px solid var(--border-brand); border-radius:999px; background:#fffffff2; color:var(--brand-primary-3); box-shadow:inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor:pointer; transition:transform 0.18s ease, box-shadow 0.18s ease; flex-shrink:0; }
.fd-chart-dl:hover { transform:translateY(-1px); box-shadow:inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16); }
.fd-chart-dl--inline { position:static; top:auto; right:auto; }

.fd-hint { padding:16px; border:1px dashed #d4ddd8; border-radius:10px; background:#fafbfb; color:#6b7d76; font-size:13px; font-weight:700; }

/* Source cards */
.fd-ra-sources { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:10px; margin-bottom:14px; }
.fd-ra-source { display:grid; grid-template-columns:20px minmax(0,1fr) auto; align-items:center; gap:10px; min-height:82px; padding:12px; border:1px solid var(--border); border-radius:8px; background:var(--surface); cursor:pointer; transition:border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease; }
.fd-ra-source:hover:not(.disabled) { border-color:var(--border-brand); box-shadow:0 8px 16px #12182612; }
.fd-ra-source.selected { border-color:var(--nav-active-border); background:var(--nav-active-bg); }
.fd-ra-source.disabled { cursor:not-allowed; background:#eef1f0; color:var(--muted); }
.fd-ra-radio { width:18px; height:18px; border:2px solid var(--border-strong); border-radius:50%; background:var(--surface); align-self:center; transition:border-color 0.16s ease, background 0.16s ease; }
.fd-ra-source.selected .fd-ra-radio { border-color:var(--brand-primary-3); background:radial-gradient(circle at center, var(--brand-primary-3) 40%, var(--surface) 44%); }
.fd-ra-main { min-width:0; }
.fd-ra-label-row { display:flex; flex-wrap:wrap; align-items:center; gap:7px; }
.fd-ra-label { font-size:15px; font-weight:900; color:var(--text); }
.fd-ra-scope { display:inline-flex; align-items:center; justify-content:center; min-height:22px; padding:3px 7px; border:1px solid var(--border); border-radius:999px; background:var(--surface-2); color:var(--muted); font-size:11px; font-weight:800; line-height:1; white-space:nowrap; }
.fd-ra-reason { display:-webkit-box; margin-top:7px; overflow:hidden; color:var(--muted); font-size:12px; line-height:1.35; -webkit-box-orient:vertical; -webkit-line-clamp:2; }
.fd-ra-status { display:inline-flex; align-items:center; justify-content:center; min-height:24px; padding:4px 8px; border:1px solid var(--border); border-radius:999px; background:var(--surface-2); color:var(--muted); font-size:11px; font-weight:800; line-height:1; white-space:nowrap; align-self:center; }
.fd-ra-status.ready { border-color:var(--nav-active-border); background:var(--nav-active-bg); color:var(--nav-active-text); }
.fd-ra-status.unavailable { background:#eef1f0; color:var(--muted); border-color:var(--border); }

.fd-ra-summary { display:flex; justify-content:space-between; align-items:center; padding:10px 0 14px; font-size:15px; font-weight:800; color:var(--text); }
.soft-button { border-color:var(--border); background:var(--surface); color:var(--text); font-weight:900; }
.soft-button:hover { border-color:var(--nav-active-border); background:var(--surface-2); color:#6f887d; }
.fd-ra-error { padding:10px 14px; border:1px solid #efb8b8; border-radius:8px; background:#fff0f0; color:#8b3a3a; font-size:13px; font-weight:700; display:flex; align-items:center; gap:10px; }
.fd-ra-refresh { appearance:none; border:1px solid #d4b8b8; border-radius:6px; background:#fff; color:#6b3a3a; cursor:pointer; font-size:12px; font-weight:800; padding:4px 10px; }
.fd-ra-refresh:hover { background:#fdf5f5; }
.fd-mono { font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-size:14px; }
.fd-col-header { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }
.fd-col-help { display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; flex-shrink: 0; }
.fd-pager { display:flex; justify-content:flex-end; padding-top:12px; }

.pager { display:flex; justify-content:flex-end; padding-top:12px; }
.pager :deep(.el-pagination) { --el-color-primary:var(--brand-primary-3); --el-pagination-hover-color:#6f887d; --el-pagination-button-bg-color:var(--surface); --el-pagination-button-disabled-bg-color:var(--surface-2); }
.pager :deep(.el-pagination.is-background .el-pager li.is-active) { background:var(--brand-primary-3); border-color:var(--brand-primary-3); color:var(--surface); font-weight:800; }

:deep(.detail-table) { border-radius:14px; overflow:hidden; font-size:14px; }
:deep(.detail-table th.el-table__cell), :deep(.detail-table td.el-table__cell) { text-align:center; vertical-align:middle; padding:12px 0; }
:deep(.detail-table th.el-table__cell > .cell), :deep(.detail-table td.el-table__cell > .cell) { display:flex; align-items:center; justify-content:center; min-height:28px; line-height:1.4; text-align:center; font-size:14px; }
:deep(.fd-ra-overlap-table td.el-table__cell > .cell) { display:block; min-height:38px; height:auto; padding:5px 10px; overflow:visible; text-overflow:clip; white-space:normal; overflow-wrap:anywhere; word-break:break-word; }
:deep(.fd-ra-overlap-table td.el-table__cell > .cell > *) { width:100%; max-width:100%; white-space:normal; overflow-wrap:anywhere; word-break:break-word; }

.fd-expression-panel { margin-top: 18px; }
.fd-expression-panel .fd-chart-row { margin-bottom: 0; }
.fd-expression-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.fd-exp-left { display: flex; align-items: center; gap: 6px; }
.fd-exp-tabs { display: flex; gap: 4px; }
.fd-exp-tab { appearance: none; padding: 4px 10px; border: 1px solid var(--border); border-radius: 999px; background: var(--surface); color: var(--muted); font-size: 11px; font-weight: 800; cursor: pointer; transition: all 0.18s ease; }
.fd-exp-tab:hover { border-color: var(--border-brand); color: var(--text); }
.fd-exp-tab.active { background: var(--nav-active-bg); color: var(--nav-active-text); border-color: var(--nav-active-border); }
.fd-chart-box--wide { width: 100%; }

@media (max-width:960px) { .fd-chart-row { flex-direction:column; gap:18px; } .fd-chart-box { width:100%; } .fd-ra-header { grid-template-columns:1fr; align-items:start; } .fd-ra-title-block, .fd-ra-mode-actions { grid-column:1; justify-self:start; } .fd-ra-mode-tabs { max-width:100%; flex-wrap:wrap; border-radius:14px; } }
@media (max-width:900px) { .fd-ra-sources { grid-template-columns:repeat(2,minmax(0,1fr)); } .fd-dash-cards { flex-wrap:wrap; justify-content:center; gap:32px; } }
@media (max-width:760px) { .fd-hero-bar h1 { font-size:24px; } .fd-ra-sources { grid-template-columns:1fr; } .fd-ra-mode-tab { min-width:0; flex:1 1 calc(50% - 4px); padding:8px 10px; } .fd-ra-mode-tabs { width:100%; } }

/* ---- Download dialog ---- */
:global(.el-dialog.fd-download-dialog) {
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.18);
  transform-origin: top center;
}
:global(.el-dialog.fd-download-dialog .el-dialog__header) {
  padding: 16px 18px 12px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0));
}
:global(.el-dialog.fd-download-dialog .el-dialog__body) {
  padding: 14px 18px 16px;
}
:global(.el-dialog.fd-download-dialog .el-dialog__footer) {
  padding: 12px 18px 16px;
  border-top: 1px solid var(--border);
  background: rgba(0, 0, 0, 0.01);
}
:global(.el-dialog.fd-download-dialog .el-dialog__headerbtn) {
  border-radius: 10px;
}
:global(.el-dialog.fd-download-dialog .el-dialog__headerbtn:hover) {
  background: rgba(0, 0, 0, 0.04);
}
:global(.dialog-fade-enter-active .el-dialog.fd-download-dialog) {
  animation: fdBubbleIn 0.18s ease-out both;
}
:global(.dialog-fade-leave-active .el-dialog.fd-download-dialog) {
  animation: fdBubbleOut 0.14s ease-in both;
}
@keyframes fdBubbleIn {
  from { opacity: 0; transform: translateY(-10px) scale(0.985); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes fdRaSpin { to { transform:rotate(360deg); } }
@keyframes fdBubbleOut {
  from { opacity: 1; transform: translateY(0) scale(1); }
  to { opacity: 0; transform: translateY(-8px) scale(0.99); }
}
.landscape-download-body {
  display: flex;
  flex-direction: column;
}
.landscape-download-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  padding: 6px 0 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid var(--border);
  color: var(--muted);
  font-size: 13px;
  font-weight: 750;
}
.landscape-download-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}
.landscape-download-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}
.landscape-download-chip:hover:not(:disabled) {
  border-color: rgba(0, 0, 0, 0.12);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}
.landscape-download-chip:disabled {
  opacity: 0.82;
  cursor: wait;
  transform: none;
}
.landscape-download-chip--loading .landscape-download-chip-action {
  min-width: 94px;
}
.landscape-download-chip-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.landscape-download-chip-name {
  overflow: hidden;
  color: var(--text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.landscape-download-chip-format {
  padding: 3px 10px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.03);
  font-size: 12px;
  font-weight: 900;
}
.landscape-download-chip-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--brand-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}
.landscape-download-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  border-radius: 50%;
  animation: landscapeDownloadSpin 0.7s linear infinite;
}
@keyframes landscapeDownloadSpin {
  to { transform: rotate(360deg); }
}
</style>

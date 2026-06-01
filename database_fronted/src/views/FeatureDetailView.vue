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
            <span v-if="region" class="fd-hero-tag fd-hero-tag--region">{{ region }}</span>
          </div>
        </div>
        <button type="button" class="fd-back-btn" @click="goBack">Back to sample</button>
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
                <span>Cell contexts</span>
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
                <div class="fd-chart-title">Marker evidence by dataset</div>
                <button class="fd-chart-dl" title="Download chart" @click="downloadChartImage(datasetChart)"><el-icon><Download /></el-icon></button>
                <div v-if="datasetRanking.length === 0" class="fd-chart-empty">No dataset data available</div>
                <div v-else ref="datasetChartEl" class="fd-chart-canvas" />
              </div>
              <div class="fd-chart-box">
                <div class="fd-chart-title">Marker evidence by cell context</div>
                <button class="fd-chart-dl" title="Download chart" @click="downloadChartImage(cellContextChart)"><el-icon><Download /></el-icon></button>
                <div v-if="cellContextRanking.length === 0" class="fd-chart-empty">No cell context data available</div>
                <div v-else ref="cellContextChartEl" class="fd-chart-canvas" />
              </div>
            </div>

            <div class="fd-qc-section fd-chart-empty">
              Expression graph coming soon
            </div>
          </template>
        </div>
      </section>

      <!-- ===== Module B: Regulatory annotation ===== -->
      <section class="fd-module">
        <div class="fd-module-inner">
          <div class="fd-occ-header">
            <div class="fd-section-label">Regulatory annotation</div>
            <div class="fd-section-desc">
              <template v-if="isPeakDetail">Regulatory annotation sources overlapping {{ region || 'the selected peak region' }}.</template>
              <template v-else>Regulatory annotation of <strong>{{ geneSymbol || 'this gene' }}</strong> using reference annotation sources.</template>
            </div>
          </div>

          <div class="fd-ra-sources">
            <label v-for="source in raSources" :key="source.type" class="fd-ra-source" :class="{ selected: raSelectedType === source.type, disabled: !source.available }" :title="source.label" @click="selectRaSource(source.type)">
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

          <div v-if="!region" class="fd-hint">No genomic region was provided for automatic annotation overlap.</div>
          <template v-else>
            <div v-if="intersectError" class="fd-ra-error">
              {{ intersectError }}
              <button v-if="intersectErrorStatus === 409" type="button" class="fd-ra-refresh" @click="loadAnnotationOverlap">Refresh</button>
            </div>
            <template v-else>
              <div v-if="intersectRecords.length === 0 && !intersectLoading" class="fd-hint">No regulatory annotation overlaps found for this region.</div>
              <template v-else>
                <div class="fd-ra-summary">
                  <span>Showing {{ intersectRecords.length }} of {{ intersectTotal }} hits</span>
                  <el-button class="soft-button" :disabled="intersectRecords.length === 0" @click="downloadOverlapCsv"><el-icon><Download /></el-icon><span>CSV</span></el-button>
                </div>
                <el-table :data="intersectRecords" stripe border class="detail-table" size="small" v-loading="intersectLoading">
                  <el-table-column label="Location" min-width="190"><template #default="{ row }"><span class="fd-mono" :title="getLocation(row)">{{ getLocation(row) }}</span></template></el-table-column>
                  <el-table-column v-if="raSelectedType !== 'enhancer' && raSelectedType !== 'crispr'" :label="featureColumnLabel" min-width="150"><template #default="{ row }"><span :title="getFeature(row)">{{ getFeature(row) }}</span></template></el-table-column>
                  <el-table-column v-for="col in sourceColumnDefs" :key="col.label" :label="col.label" min-width="130"><template #default="{ row }"><span :title="getRawField(row, col.idx)">{{ getRawField(row, col.idx) }}</span></template></el-table-column>
                  <el-table-column label="Overlap" min-width="80" align="center"><template #default="{ row }">{{ getOverlap(row) }}</template></el-table-column>
                </el-table>
                <div v-if="intersectTotal > 0" class="pager">
                  <el-pagination class="oscar-pagination" background layout="sizes, prev, pager, next" popper-class="oscar-select-popper" :total="intersectTotal" :page-sizes="[10, 20, 50]" :page-size="pageSize" :current-page="page" @size-change="onPageSizeChange" @current-change="onPageChange" />
                </div>
              </template>
            </template>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Download } from "@element-plus/icons-vue";
import * as echarts from "echarts";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { BedtoolsOverlapRecord, BedtoolsSourceOption, BedtoolsSourcesResponse, FeatureOccurrenceResponse, SearchResultDomain } from "@/api/searchResult";
import { fetchBedtoolsSources, fetchFeatureOccurrence, runBedtoolsIntersect } from "@/api/searchResult";
import { getBioChartColor, getBioChartColorMap } from "@/utils/chartPalette";

type FeatureType = "gene" | "peak";
const REG_SOURCE_TYPES = ["enhancer","super_enhancer","tfbs","common_snp","risk_snp","gtex_eqtl","methylation","crispr"];
const SOURCE_LABELS: Record<string, string> = { risk_snp:"Risk SNP", common_snp:"Common SNP", gtex_eqtl:"GTEx eQTL", tfbs:"TFBS", enhancer:"Enhancer", super_enhancer:"Super Enhancer", methylation:"Methylation", crispr:"CRISPR" };
const SOURCE_FIELD_NAMES: Record<string, string[]> = {
  common_snp: ["Ref", "Alt"], risk_snp: ["Ref", "Alt", "Gene", "Disease / Trait", "GWAS Type", "P value", "OR", "PubMed"],
  enhancer: ["Enhancer type", "Source"], super_enhancer: ["Tissue", "Cell type"], tfbs: ["Description"],
  gtex_eqtl: ["Ref", "Alt", "TSS Distance", "MAF", "P-value", "Tissue"],
  methylation: ["Biosample", "Source", "Beta Value"], crispr: ["Source"],
};

const route = useRoute(); const router = useRouter();
const genomeBuild = "hg38"; const pageSize = ref(10);

const featureType = computed<FeatureType>(() => queryString("type").toLowerCase() === "peak" ? "peak" : "gene");
const isPeakDetail = computed(() => featureType.value === "peak");
const geneSymbol = computed(() => queryString("gene"));
const chrom = computed(() => queryString("chrom"));
const sv = computed(() => queryNumber("start")); const ev = computed(() => queryNumber("end"));
const datasetId = computed(() => queryString("datasetId"));
const domain = computed<SearchResultDomain>(() => { const v = queryString("domain").toLowerCase(); return v === "rna" || v === "atac" ? v : "integration"; });
const featureKicker = computed(() => isPeakDetail.value ? "Peak detail" : "Gene detail");
const region = computed(() => chrom.value && sv.value !== null && ev.value !== null ? `${chrom.value}:${sv.value}-${ev.value}` : "");
const mainTitle = computed(() => isPeakDetail.value ? (region.value || "Peak") : (geneSymbol.value || "Gene"));

const overviewSubtitle = computed(() => isPeakDetail.value
  ? "Marker peak occurrence landscape across OSCAR datasets and cell contexts."
  : "Marker gene occurrence landscape across OSCAR datasets and cell contexts.");

/* ---- Overview data ---- */
const occurrenceLoading = ref(false);
const occurrenceData = ref<FeatureOccurrenceResponse | null>(null);
const occurrenceError = ref(false);

const overviewClusterCount = computed(() => occurrenceData.value?.clusterCount ?? 0);

const datasetRanking = computed(() => occurrenceData.value?.datasetRanking?.map(d => ({
  datasetId: d.datasetId || "Unknown",
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
const intersectSummary = ref<any>(null);
const intersectError = ref(""); const intersectErrorStatus = ref<number | null>(null);
const page = ref(1); const raSelectedType = ref("");

const raSources = computed(() => REG_SOURCE_TYPES.map(type => {
  const s = loadedSources.value.find(x => x.type === type);
  return { type, label: SOURCE_LABELS[type]||type, available: s?.available??false, status: s?.status??"PENDING", reason: s?.reason??null, description: s?.description??null };
}));

const sourceColumnDefs = computed(() => { const names = SOURCE_FIELD_NAMES[raSelectedType.value] || []; return names.map((label, idx) => ({ label, idx })); });
const featureColumnLabel = computed(() => { switch(raSelectedType.value){ case"common_snp":case"risk_snp":return"SNP ID";case"tfbs":return"TF / Motif";case"enhancer":return"Element";case"super_enhancer":return"SE ID";case"gtex_eqtl":return"Gene";case"methylation":return"CpG ID";default:return"Feature";} });

function getRawField(row: any, idx: number): string { const f: unknown[] = Array.isArray(row.rawFields)?row.rawFields:[]; return cleanText(f[idx])||"-"; }
function getFeature(row: any): string { const v = cleanText(row.name||row.featureName||row.featureId||row.feature||""); return v && v!=="." ? v : "-"; }
function getLocation(row: any): string { const c = cleanText(row.chrom||row.chromosome||row.featureRegion); if(c)return c; const rs=row.start!=null?row.start:""; const re=row.end!=null?row.end:""; return rs!==""&&re!==""?`${rs}-${re}`:"-"; }
function getOverlap(row: any): string { const bp=row.overlapBp??row.overlap??row.overlap_bp; return bp!=null?String(bp):"--"; }
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
  const barColors = data.map((_,i) => getBioChartColor(i));
  datasetChart.setOption({
    color: barColors, tooltip: { trigger:"axis", axisPointer:{type:"shadow"}, formatter:(p:any)=> data[p[0]?.dataIndex]?.datasetId ?? "" },
    grid: { left:100, right:40, top:10, bottom:30 },
    xAxis: { type:"value", axisLabel:{ color:"#5E6C67" } },
    yAxis: { type:"category", data: data.map(d=>d.datasetId).reverse(), axisLabel:{ color:"#5E6C67", width:90, overflow:"truncate" }, inverse:true },
    series: [{ type:"bar", data: data.map(d=>d.recordCount).reverse(), barMaxWidth:24, itemStyle:{ borderRadius:[0,4,4,0] } }],
  }, true);
}

function renderCellContextChart() {
  if (!cellContextChartEl.value || cellContextRanking.value.length === 0) return;
  if (!cellContextChart) { const r = initChart(cellContextChartEl); if (!r) return; cellContextChart = r.chart; }
  const data = cellContextRanking.value;
  const colorMap = getBioChartColorMap(data.map(d=>d.cellType));
  cellContextChart.setOption({
    color: data.map((d,i)=>colorMap.get(d.cellType)??getBioChartColor(i)),
    tooltip: { trigger:"item", formatter:(p:any)=> p.name },
    series: [{ type:"pie", radius:["42%","70%"], center:["50%","45%"], avoidLabelOverlap:true, itemStyle:{ borderRadius:3, borderColor:"#fff", borderWidth:1 },
      label: { color:"#5E6C67", fontSize:12 }, data: data.map(d=>({ name:d.cellType, value:d.recordCount })) }],
  }, true);
}

watch([datasetRanking, cellContextRanking], async () => { await nextTick(); renderDatasetChart(); renderCellContextChart(); });

/* ---- Loaders ---- */
async function loadSources() { if(!datasetId.value||!region.value)return; try{ const data: BedtoolsSourcesResponse = await fetchBedtoolsSources({datasetId:datasetId.value,domain:domain.value,genomeBuild}); loadedSources.value = data.sources??[]; autoPickSource(); }catch(err){ console.error("[FeatureDetail] sources:",err); } }
async function loadAnnotationOverlap() { if(!datasetId.value||!region.value)return; const type=raSelectedType.value; if(!type)return; intersectLoading.value=true; intersectError.value=""; intersectErrorStatus.value=null; try{ const data = await runBedtoolsIntersect({datasetId:datasetId.value,domain:domain.value,genomeBuild,region:region.value,annotationTypes:[type],minOverlapBp:1,page:page.value,pageSize:pageSize.value}); intersectRecords.value=data.records??[]; intersectTotal.value=data.total??(data.records?.length??0); intersectSummary.value=data.summary??null; }catch(err:any){ const st=err?.response?.status; intersectErrorStatus.value=st??null; intersectError.value=st===400?"Region or parameter format error.":st===409?"Some annotation sources are unavailable.":`Request failed: ${err?.message||"Unknown error"}`; intersectRecords.value=[]; intersectTotal.value=0; intersectSummary.value=null; }finally{ intersectLoading.value=false; } }
function selectRaSource(type:string){ const s=raSources.value.find(x=>x.type===type); if(!s?.available)return; if(raSelectedType.value===type)return; raSelectedType.value=type; page.value=1; loadAnnotationOverlap(); }
function autoPickSource(){ if(raSelectedType.value)return; const avail=raSources.value.filter(s=>s.available); if(avail.length>0)raSelectedType.value=avail[0]!.type; }
function onPageChange(p:number){ page.value=p; loadAnnotationOverlap(); }
function onPageSizeChange(sz:number){ pageSize.value=sz; page.value=1; loadAnnotationOverlap(); }

async function loadOccurrence() { occurrenceLoading.value=true; occurrenceError.value=false; occurrenceData.value=null; try{ if(isPeakDetail.value){ if(!chrom.value||sv.value===null||ev.value===null)return; occurrenceData.value=await fetchFeatureOccurrence({type:"peak",chrom:chrom.value,start:sv.value,end:ev.value,domain:domain.value}); }else{ const g=geneSymbol.value; if(!g)return; occurrenceData.value=await fetchFeatureOccurrence({type:"gene",gene:g,domain:domain.value}); } }catch(err){ console.error("[FeatureDetail] occurrence:",err); occurrenceError.value=true; }finally{ occurrenceLoading.value=false; } }

function downloadChartImage(chart: echarts.ECharts|null) {
  if (!chart) return;
  const url = chart.getDataURL({ type:"png", pixelRatio:2, backgroundColor:"#fff" });
  const a = document.createElement("a"); a.href = url; a.download = "chart.png"; a.click();
}

function downloadOverlapCsv() { if(intersectRecords.value.length===0)return; const skipFeature=raSelectedType.value==="enhancer"; const headers=["Location",...(skipFeature?[]:[featureColumnLabel.value]),...sourceColumnDefs.value.map(c=>c.label),"Overlap"]; const rows=intersectRecords.value.map(r=>[getLocation(r),...(skipFeature?[]:[getFeature(r)]),...sourceColumnDefs.value.map(c=>getRawField(r,c.idx)),getOverlap(r)]); const csv=[headers,...rows].map(row=>row.map(v=>`"${String(v).replace(/"/g,'""')}"`).join(",")).join("\n"); const blob=new Blob([csv],{type:"text/csv;charset=utf-8"}); const url=URL.createObjectURL(blob); const a=document.createElement("a"); a.href=url; a.download=`${datasetId.value}_regulatory_annotation_page_${page.value}.csv`; a.click(); URL.revokeObjectURL(url); }

function queryString(name:string):string{ const v=route.query[name]; return String(Array.isArray(v)?v[0]??"":v??"").trim(); }
function queryNumber(name:string):number|null{ const v=Number(queryString(name)); return Number.isFinite(v)?v:null; }
function cleanText(value:unknown):string{ const t=String(value??"").trim(); return t||""; }
function goBack(){ if(datasetId.value){ router.push({path:`/search-result/${encodeURIComponent(datasetId.value)}`,query:{domain:domain.value}}); return; } router.back(); }

watch([region,occurrenceData], async () => { await nextTick(); renderDatasetChart(); renderCellContextChart(); });
onMounted(()=>{ if(region.value){ loadSources().then(()=>loadAnnotationOverlap()); } loadOccurrence(); });
onBeforeUnmount(()=>{ datasetChart?.dispose(); cellContextChart?.dispose(); chartResizers.forEach(r=>r.disconnect()); });
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
.fd-hero-tag--region { font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-weight:900; }
.fd-back-btn { appearance:none; flex-shrink:0; display:inline-flex; align-items:center; gap:6px; min-height:34px; padding:0 14px; border:1px solid var(--border); border-radius:8px; background:var(--surface); color:var(--text); cursor:pointer; font-size:12px; font-weight:900; white-space:nowrap; transition:border-color 0.18s ease, color 0.18s ease; }
.fd-back-btn:hover { border-color:var(--border-brand); color:var(--brand-primary-3); }

.fd-module { margin-bottom:14px; }
.fd-module-inner { padding:18px; border:1px solid var(--border); border-radius:var(--radius-lg); background:var(--surface); box-shadow:var(--shadow-card); transition:box-shadow 0.18s ease, border-color 0.18s ease; }
.fd-module-inner:hover { border-color:var(--border-brand); box-shadow:var(--shadow-hover); }

.fd-occ-header { margin-bottom:14px; }
.fd-section-label { font-size:18px; font-weight:900; color:var(--text); }
.fd-section-desc { margin-top:4px; color:var(--muted); font-size:13px; line-height:1.35; }

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
.fd-chart-canvas { width:100%; height:400px; }
.fd-chart-empty { display:flex; align-items:center; justify-content:center; height:200px; color:var(--muted); font-size:13px; font-weight:700; }
.fd-chart-dl { appearance:none; position:absolute; top:-6px; right:0; z-index:2; display:inline-flex; align-items:center; justify-content:center; width:32px; height:32px; padding:0; border:1px solid var(--border-brand); border-radius:999px; background:#fffffff2; color:var(--brand-primary-3); box-shadow:inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614; cursor:pointer; transition:transform 0.18s ease, box-shadow 0.18s ease; }
.fd-chart-dl:hover { transform:translateY(-1px); box-shadow:inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16); }

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
.fd-pager { display:flex; justify-content:flex-end; padding-top:12px; }

.pager { display:flex; justify-content:flex-end; padding-top:12px; }
.pager :deep(.el-pagination) { --el-color-primary:var(--brand-primary-3); --el-pagination-hover-color:#6f887d; --el-pagination-button-bg-color:var(--surface); --el-pagination-button-disabled-bg-color:var(--surface-2); }
.pager :deep(.el-pagination.is-background .el-pager li.is-active) { background:var(--brand-primary-3); border-color:var(--brand-primary-3); color:var(--surface); font-weight:800; }

:deep(.detail-table) { border-radius:14px; overflow:hidden; font-size:14px; }
:deep(.detail-table th.el-table__cell), :deep(.detail-table td.el-table__cell) { text-align:center; vertical-align:middle; padding:12px 0; }
:deep(.detail-table th.el-table__cell > .cell), :deep(.detail-table td.el-table__cell > .cell) { display:flex; align-items:center; justify-content:center; min-height:28px; line-height:1.4; text-align:center; font-size:14px; }

@media (max-width:960px) { .fd-chart-row { flex-direction:column; gap:18px; } .fd-chart-box { width:100%; } }
@media (max-width:900px) { .fd-ra-sources { grid-template-columns:repeat(2,minmax(0,1fr)); } .fd-dash-cards { flex-wrap:wrap; justify-content:center; gap:32px; } }
@media (max-width:760px) { .fd-hero-bar h1 { font-size:24px; } .fd-ra-sources { grid-template-columns:1fr; } }
</style>

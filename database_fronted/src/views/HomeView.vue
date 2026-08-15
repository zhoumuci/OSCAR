<template>
    <div class="home">
      <!-- HERO：背景全宽铺开 -->
      <section class="hero-wrap">
        <div class="container">
          <!-- 左：标题 + 搜索 + 轮播 -->
          <div class="float-card hero-card">
            <div class="hero">
              <section class="left">
                <div class="title-row">
                <div class="big-title">OSCAR</div>
                </div>
                <div class="subtitle">
                <span class="typewriter">A human single-cell multi-omics platform for paired RNA-seq and ATAC-seq data.</span>
                </div>

                <div class="searchbar">
                <el-select v-model="searchMode" class="sel" clearable @change="onSearchModeChange">
                    <el-option label="Tissue type" value="tissue" />
                    <el-option label="Biosample type" value="biosample" />
                    <el-option label="Sample ID" value="sample" />
                </el-select>

                <el-input
                    v-model="keyword"
                    :placeholder="keyword ? '' : placeholderText"
                    class="inp"
                    clearable
                    @keyup.enter="onSearch"
                    @focus="placeholderPaused = true"
                    @blur="placeholderPaused = false; if(!keyword) animatePlaceholder()"
                >
                    <template #append>
                    <el-button class="search-btn" @click="onSearch">Search</el-button>
                    </template>
                </el-input>
                </div>
    
                <div class="example">
                Example:
                <button type="button" class="example-link" @click="quickSearch('tissue','Brain', $event)"><b>Tissue type</b>: Brain</button>
                &nbsp;&nbsp;
                <button type="button" class="example-link" @click="quickSearch('biosample','Cell line', $event)"><b>Biosample type</b>: Cell line</button>
                &nbsp;&nbsp;
                <button type="button" class="example-link" @click="quickSearch('sample','H_000001', $event)"><b>Sample ID</b>: H_000001</button>
                </div>
    
                <!-- ✅ 左侧轮播：你说的 carousel -->
                <el-carousel
                class="carousel"
                indicator-position="outside"
                arrow="always"
                :interval="4500"
                >
                <el-carousel-item v-for="(s, i) in slides" :key="i">
                    <div class="slide">
                    <img v-if="s.src" :src="s.src" class="slide-img" alt="slide" />
                    <div v-else class="slide-placeholder">
                        <div class="ph-title">Carousel Slide {{ i + 1 }}</div>
                        <div class="ph-sub">所需要的轮播图片放到 src/assets/slides/ 下即可</div>
                    </div>
                    </div>
                </el-carousel-item>
                </el-carousel>
              </section>
          <!-- 右：介绍 -->
              <aside class="right">
            <div class="h2">What is our database?</div>

            <div class="desc">
              <p>
                We present OSCAR, an omics atlas integrating single-cell ATAC-seq and
                RNA-seq, to provide a comprehensive resource for human single-cell paired
                transcriptome and chromatin accessibility profiles. OSCAR aims to collect,
                standardize, annotate, and visualize large-scale single-cell multi-omics
                datasets in single cell and cluster resolutions. The current release
                contains <span class="hl">7,749,898 human single cells</span> from <span class="hl">1,136 samples</span> across <span class="hl">138 datasets</span>, covering <span class="hl">30 tissue types</span>, <span class="hl">9,151 integration clusters</span>, and <span class="hl">221 major cell types</span>. By integrating paired scRNA-seq and
                scATAC-seq profiles, OSCAR enables systematic exploration of
                transcriptional programs and their underlying chromatin regulatory
                landscapes and peak-to-gene regulatory links within the same cells. OSCAR
                provides detailed and abundant (epi)genetic annotations for the regulatory
                regions of marker genes (super enhancers, enhancers and promoters) and for
                marker peaks, such as super enhancer, enhancer, common SNPs, motif changes,
                expression quantitative trait locus (eQTL), risk SNPs, transcription factor
                binding sites (TFBSs), CRISPR/Cas9 target sites, DNase I hypersensitivity
                sites (DHSs), chromatin accessibility regions, methylation sites, chromatin
                interactions regions and TADs. Together, OSCAR provides <span class="hl">a comprehensive and user-friendly platform</span> for dissecting cell-specific regulatory programs,
                uncovering disease-associated regulatory mechanisms, and advancing the
                interpretation of single-cell multi-omics data.
              </p>
            </div>
              </aside>
            </div>
          </div>
        </div>
      </section>
  
      <!-- 分割线（全宽） -->
      <div class="divider"></div>

    <!-- Data overview -->
    <section class="section-wrap">
    <div class="container">
      <div class="float-card section-card">
        <div class="sec-head">
          <div class="sec-title">Data overview</div>
          <div class="sec-sub">A summary of human single-cell paired RNA-seq and ATAC-seq datasets, sample composition, and tissue coverage in OSCAR.</div>
        </div>

        <!-- Stats cards (hardcoded) -->
        <div class="overview-stats">
          <div class="os-card">
            <div class="os-num">7,749,898</div>
            <div class="os-label">Human single cells</div>
          </div>
          <div class="os-card">
            <div class="os-num">1,136</div>
            <div class="os-label">Multiome samples</div>
          </div>
          <div class="os-card">
            <div class="os-num">138</div>
            <div class="os-label">Curated datasets</div>
          </div>
          <div class="os-card">
            <div class="os-num">30</div>
            <div class="os-label">Tissue types</div>
          </div>
          <div class="os-card">
            <div class="os-num">9,151</div>
            <div class="os-label">Integration clusters</div>
          </div>
          <div class="os-card">
            <div class="os-num">221</div>
            <div class="os-label">Major cell types</div>
          </div>
        </div>

        <!-- Charts row -->
        <div class="overview-charts">
          <div class="ochart">
            <div class="ochart-title">Biosample composition</div>
            <div ref="biosampleChartEl" class="ochart-canvas"></div>
          </div>
          <div class="ochart">
            <div class="ochart-title">Tissue coverage
              <el-tooltip placement="top" effect="light" :show-after="200">
                <template #content>
                  <div><div>Shows the 12 tissues with the largest number of OSCAR samples.</div><div>Bar length represents sample count; hover over a bar to see its sample, cell, and dataset totals.</div></div>
                </template>
                <span class="ochart-help">?</span>
              </el-tooltip>
            </div>
            <div ref="tissueChartEl" class="ochart-canvas"></div>
          </div>
        </div>
      </div>
    </div>
    </section>

    <!-- Workflow -->
    <section class="section-wrap">
    <div class="container">
      <div class="float-card workflow-card">
        <div class="sec-head">
          <div class="sec-title">WorkFlow</div>
          <div class="sec-sub">Data processing, integration, and annotation pipeline of the OSCAR platform.</div>
        </div>
        <div class="workflow-img-wrap">
          <img
            v-if="workflowImgReady"
            :src="baseUrl + 'images/workflow.jpg'"
            alt="OSCAR analysis workflow"
            class="workflow-img"
          />
          <div v-else class="workflow-placeholder">
            <span class="workflow-placeholder-icon">🖼️</span>
            <span>Place your workflow image at <code>public/images/workflow.jpg</code></span>
          </div>
        </div>
      </div>
    </div>
    </section>

    <!-- Sister projects -->
    <section class="section-wrap">
    <div class="container">
        <div class="float-card section-card">
        <div class="sec-head">
            <div class="sec-title">Sister projects</div>
            <div class="sec-sub">Other database/resource entry points of the research group.</div>
        </div>

        <div class="proj-grid">
            <a
            v-for="p in sisterProjects"
            :key="p.name"
            class="proj-card"
            :href="p.url"
            target="_blank"
            rel="noreferrer"
            >
            <div class="proj-name">{{ p.name }}</div>
            <div class="proj-desc">{{ p.desc }}</div>
            <div class="proj-meta">{{ p.tag }}</div>
            </a>
        </div>
        </div>
    </div>
    </section>

    <!-- ③ Cite（引用信息 + 链接占位）—— 暂隐藏，发文后恢复 -->
    <section v-if="false" class="section-wrap">
    <div class="container">
        <div class="float-card section-card">
        <div class="sec-head">
            <div class="sec-title">Publication</div>
            <div class="sec-sub">发文后替换为正式引用（BibTeX / DOI / PMID）</div>
        </div>

        <div class="cite-box">
            <div class="cite-label">Recommended citation</div>
            <pre class="cite-pre">{{ citeText }}</pre>
            <div class="cite-actions">
            <el-button size="small" @click="copyCite">Copy</el-button>
            <el-button size="small" type="primary" disabled>DOI (coming soon)</el-button>
            </div>
        </div>
        </div>
    </div>
    </section>

    <!-- Visitors -->
    <section class="section-wrap">
    <div class="container">
      <div class="float-card section-card visitors-section">
        <div class="sec-head">
          <div class="sec-title">Global visitors</div>
          <div class="sec-sub">Real-time visitor distribution across the world.</div>
        </div>
        <div class="visitors-wrap">
          <div class="visitors-stats">
            <div class="vstat-card" ref="vstatTotalRef" @mousemove="onStatTilt($event, 'total')" @mouseleave="resetStatTilt('total')">
              <div class="vstat-icon">🌍</div>
              <div class="vstat-num" ref="vstatNumTotal">{{ fmtCount(visitorStats.totalVisitors) }}</div>
              <div class="vstat-label">Total Visits</div>
              <div class="vstat-sublabel">All‑time page opens</div>
              <div class="vstat-ring"></div>
            </div>
            <div class="vstat-card" ref="vstatCountryRef" @mousemove="onStatTilt($event, 'country')" @mouseleave="resetStatTilt('country')">
              <div class="vstat-deco vstat-deco--dots"></div>
              <div class="vstat-icon">🌐</div>
              <div class="vstat-num">{{ visitorStats.countryCount }}</div>
              <div class="vstat-label">Countries</div>
              <div class="vstat-sublabel">Across all continents</div>
              <div class="vstat-ring"></div>
            </div>
            <div class="vstat-card" ref="vstatActiveRef" @mousemove="onStatTilt($event, 'active')" @mouseleave="resetStatTilt('active')">
              <div class="vstat-deco vstat-deco--waves"></div>
              <div class="vstat-icon">🟢</div>
              <div class="vstat-num">{{ visitorStats.activeToday }}</div>
              <div class="vstat-label">Active Today</div>
              <div class="vstat-sublabel">Last 24 hours</div>
              <div class="vstat-dot"></div>
            </div>
          </div>
          <div class="visitors-map">
            <VisitorsMap :points="visitorPoints" />
          </div>
          <div class="visitors-globe">
            <div class="globe-window">
              <RotatingGlobe />
            </div>
          </div>
        </div>
      </div>
    </div>
    </section>
    </div>
  </template>
  
  <script setup lang="ts">
const baseUrl = import.meta.env.BASE_URL;
  import { defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
  import { useRouter } from "vue-router";
  import { ElMessage } from "element-plus";
  import { fetchVisitorPoints, fetchVisitorStats, type VisitorPoint, type VisitorStats } from "@/api/home";
  
import slide1 from "@/assets/slides/slide1.webp";
import slide2 from "@/assets/slides/slide2.webp";
import slide3 from "@/assets/slides/slide3.webp";
import slide4 from "@/assets/slides/slide4.webp";

const VisitorsMap = defineAsyncComponent(() => import("@/components/VisitorsMap.vue"));
const RotatingGlobe = defineAsyncComponent(() => import("@/components/RotatingGlobe.vue"));

type EChartsModule = typeof import("echarts");
type EChartsInstance = import("echarts").ECharts;
let echartsModule: EChartsModule | null = null;

async function loadECharts() {
  if (!echartsModule) {
    echartsModule = await import("echarts");
  }
  return echartsModule;
}
  
  const router = useRouter();
  const searchMode = ref("tissue");
  const keyword = ref<string>("");

  // ---- dynamic placeholder typing ----
  const DEFAULT_PLACEHOLDER_EXAMPLES = ["Brain", "PBMC", "Eye", "Heart", "Intestine"];
  const placeholderMap: Record<string, string[]> = {
    tissue:   DEFAULT_PLACEHOLDER_EXAMPLES,
    biosample:["Tissue", "Cell line", "Sorted cells", "Organoid"],
    sample:   ["H_000001", "H_000002", "H_000003", "H_000004", "H_000005"],
  };

  function onSearchModeChange() {
    clearTimeout(placeholderTimer);
    charIdx = 0; exampleIdx = 0; typingForward = true;
    placeholderText.value = "";
    animatePlaceholder();
  }

  function placeholderExamples(): string[] {
    return placeholderMap[searchMode.value] ?? DEFAULT_PLACEHOLDER_EXAMPLES;
  }
  const placeholderText = ref("");
  let placeholderTimer: ReturnType<typeof setTimeout> | undefined;
  let exampleIdx = 0;
  let charIdx = 0;
  let typingForward = true;

  const placeholderPaused = ref(false);

  function animatePlaceholder() {
    if (placeholderPaused.value || keyword.value) return;
    const examples = placeholderExamples();
    const current = examples[exampleIdx % examples.length] ?? "";
    if (!current) return;
    if (typingForward) {
      charIdx++;
      placeholderText.value = current.slice(0, charIdx);
      if (charIdx >= current.length) {
        typingForward = false;
        placeholderTimer = setTimeout(animatePlaceholder, 2000); // pause at end
        return;
      }
    } else {
      charIdx--;
      placeholderText.value = current.slice(0, charIdx);
      if (charIdx <= 0) {
        typingForward = true;
        exampleIdx = (exampleIdx + 1) % placeholderExamples().length;
        placeholderTimer = setTimeout(animatePlaceholder, 200);
        return;
      }
    }
    placeholderTimer = setTimeout(animatePlaceholder, typingForward ? 60 : 30);
  }
  
  const slides = ref<Array<{ src?: string }>>([
    { src: slide1 },
    { src: slide2 },
    { src: slide3 },
    { src: slide4 },
  ]);
  
  function quickSearch(mode: string, term: string, event?: MouseEvent) {
    let route;
    if (mode === "sample") {
      route = router.resolve({ name: "SampleDetail", params: { id: term }, query: { domain: "integration", source: "home" } });
    } else {
      const facetKey = mode === "tissue" ? "tissue" : "sampleType";
      route = router.resolve({ path: "/browse", query: { keyword: term, facet: facetKey, facetValue: term } });
    }
    // 鼠标 Ctrl+Click / Cmd+Click → 新标签页; 键盘 Enter 一律本页跳转
    if (event instanceof MouseEvent && (event.ctrlKey || event.metaKey)) {
      window.open(route.href, '_blank');
    } else {
      router.push(route);
    }
  }

  const onSearch = (e?: MouseEvent) => {
    const kw = keyword.value.trim();
    if (!kw) { ElMessage.info("Please enter a search term."); return; }
    quickSearch(searchMode.value, kw, e);
  };

  const tissueChartEl = ref<HTMLElement | null>(null);
  const biosampleChartEl = ref<HTMLElement | null>(null);
  let tissueChart: EChartsInstance | null = null;
  let biosampleChart: EChartsInstance | null = null;
  let chartRenderFrame: number | null = null;
  let chartRenderTimer: number | null = null;
  let homeUnmounted = false;

  // ---- hardcoded overview data (run SQL to update) ----
  const overviewData = {
    tissueCoverage: [
      { label:"Brain", sampleCount:268, cellCount:1765580, datasetCount:268 },
      { label:"PBMC", sampleCount:150, cellCount:1159447, datasetCount:150 },
      { label:"Intestine", sampleCount:105, cellCount:520716, datasetCount:105 },
      { label:"Eye", sampleCount:84, cellCount:539621, datasetCount:84 },
      { label:"Heart", sampleCount:80, cellCount:322949, datasetCount:80 },
      { label:"Pancreas", sampleCount:52, cellCount:345740, datasetCount:52 },
      { label:"Lymph node", sampleCount:50, cellCount:500372, datasetCount:50 },
      { label:"Bone marrow", sampleCount:45, cellCount:376255, datasetCount:45 },
      { label:"Prostate", sampleCount:43, cellCount:233103, datasetCount:43 },
      { label:"Kidney", sampleCount:41, cellCount:442844, datasetCount:41 },
      { label:"Spleen", sampleCount:36, cellCount:390896, datasetCount:36 },
      { label:"Blood", sampleCount:34, cellCount:210899, datasetCount:34 },
    ],
    biosampleComposition: [
      { label:"Tissue", sampleCount:703, cellCount:4135631 },
      { label:"Cell line", sampleCount:194, cellCount:1424455 },
      { label:"Sorted cells", sampleCount:157, cellCount:1513629 },
      { label:"Stem cell-derived", sampleCount:47, cellCount:317144 },
      { label:"Organoid", sampleCount:20, cellCount:140102 },
      { label:"Primary cells", sampleCount:15, cellCount:218937 },
    ],
  };

  async function renderCharts() {
    const echarts = await loadECharts();
    if (homeUnmounted) return;
    const o = overviewData;

    const WARM_COLORS = ["#E8936B","#7BA7C9","#B088C0","#8FC9B3","#D9826B","#C4A882"];
    const WARM_BARS = ["#E8A87C","#8FC9B3","#D9826B","#7BA7C9","#E8936B","#C4A882","#D4956A","#8FB89C","#C9876B","#A8C8D8","#EAB082","#B8C9A0"];

    // --- biosample donut (LEFT) ---
    if (biosampleChartEl.value && o.biosampleComposition.length > 0) {
      if (!biosampleChart) biosampleChart = echarts.init(biosampleChartEl.value);
      const bc = o.biosampleComposition;
      const total = bc.reduce((s, i) => s + i.sampleCount, 0);
      biosampleChart.setOption({
        color: WARM_COLORS,
        tooltip: { trigger: "item", formatter: (p: any) => `${p.name}<br/>Samples: ${p.value.toLocaleString()}<br/>Cells: ${bc[p.dataIndex]?.cellCount?.toLocaleString()??'-'}<br/>Ratio: ${((p.value/total)*100).toFixed(1)}%` },
        legend: { type:"scroll", bottom: 0, textStyle:{color:"#5E6C67",fontSize:11} },
        series: [{
          type: "pie", radius: ["50%","75%"], center: ["50%","43%"],
          label: { color: "#1B2A27", formatter:"{b}", fontSize: 12 },
          data: bc.map(i => ({ name:i.label, value:i.sampleCount })),
          itemStyle: { borderColor:"#fff", borderWidth:2 },
        }],
      }, true);
    }

    // --- tissue coverage bar (RIGHT) ---
    if (tissueChartEl.value && o.tissueCoverage.length > 0) {
      if (!tissueChart) tissueChart = echarts.init(tissueChartEl.value);
      const tc = [...o.tissueCoverage].reverse();
      tissueChart.setOption({
        tooltip: { trigger: "axis", formatter: (p: any) => {
          const d = p[0]; const item = tc[d.dataIndex];
          if (!item) return "";
          return `${item.label}<br/>Samples: ${item.sampleCount.toLocaleString()}<br/>Cells: ${item.cellCount.toLocaleString()}<br/>Datasets: ${item.datasetCount.toLocaleString()}`;
        }},
        grid: { left: 130, right: 40, top: 8, bottom: 24 },
        xAxis: { type: "value", axisLabel: { formatter: (v: number) => v >= 1000 ? (v/1000).toFixed(0)+'k' : v } },
        yAxis: { type: "category", data: tc.map(i => i.label), axisLabel: { fontSize: 12, color: "#5E6C67" } },
        series: [{
          type: "bar", data: tc.map((item, idx) => ({
            value: item.sampleCount,
            itemStyle: { color: WARM_BARS[idx % WARM_BARS.length], borderRadius: [0,6,6,0] }
          })), barMaxWidth: 20
        }],
      }, true);
    }

  }

  function scheduleChartsRender() {
    if (chartRenderFrame !== null) window.cancelAnimationFrame(chartRenderFrame);
    if (chartRenderTimer !== null) window.clearTimeout(chartRenderTimer);
    chartRenderFrame = window.requestAnimationFrame(() => {
      chartRenderFrame = null;
      chartRenderTimer = window.setTimeout(() => {
        chartRenderTimer = null;
        void renderCharts();
      }, 0);
    });
  }

const sisterProjects = [
  { name: "SEdb", url: "http://www.licpathway.net:8081/sedb/", desc: "The comprehensive human Super-Enhancer database.", tag: "Database" },
  { name: "eRNAbase", url: "http://bio.liclab.net/eRNAbase/index.php", desc: "A comprehensive human and mouse enhancer RNA (eRNA) annotation and analysis database.", tag: "Database" },
  { name: "scGRN", url: "https://bio.liclab.net/scGRN/", desc: "A comprehensive single-cell gene regulatory network platform of human and mouse.", tag: "Database" },
  { name: "KnockTF", url: "http://www.licpathway.net/KnockTF/", desc: "TF perturbation/knockdown expression profiles.", tag: "Database" },
  { name: "scATAC-Ref", url: "https://bio.liclab.net/scATAC-Ref/", desc: "A reference of scATAC-seq with known cell labels in multiple species.", tag: "Database" },
  { name: "TcoFbase", url: "http://bio.liclab.net/TcoFbase/", desc: "A comprehensive database for decoding the regulatory transcription co-factors in human and mouse.", tag: "Database" },
  { name: "sc2GWAS", url: "https://bio.liclab.net/sc2GWAS/", desc: "a comprehensive platform linking single cell and GWAS traits of human.", tag: "Database" },
  { name: "LncSEA", url: "http://bio.liclab.net/LncSEA/index.php", desc: "A platform for long non-coding RNA related sets and enrichment analysis.", tag: "Database" },
];

const citeText =
  "Author et al. Title of the database paper. Journal (Year).\nDOI: coming soon";

const copyCite = async () => {
  try {
    await navigator.clipboard.writeText(citeText);
    ElMessage.success("Copied");
  } catch {
    ElMessage.error("Copy failed");
  }

};


const visitorPoints = ref<VisitorPoint[]>([]);
const visitorStats = ref<VisitorStats>({ totalVisitors: 0, countryCount: 0, activeToday: 0 });
const workflowImgReady = ref(true);

function fmtCount(n: number): string {
  if (n >= 10000) return (n / 1000).toFixed(1) + "k";
  return n.toLocaleString();
}

const statTilts = ref<Record<string, { rx: number; ry: number }>>({
  total: { rx: 0, ry: 0 },
  country: { rx: 0, ry: 0 },
  active: { rx: 0, ry: 0 },
});

function onStatTilt(e: MouseEvent, key: string) {
  const card = e.currentTarget as HTMLElement;
  const rect = card.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  const rx = ((y / rect.height) - 0.5) * -16;
  const ry = ((x / rect.width) - 0.5) * 16;
  statTilts.value[key] = { rx, ry };
}

function resetStatTilt(key: string) {
  statTilts.value[key] = { rx: 0, ry: 0 };
}

onMounted(async () => {
  animatePlaceholder();
  await nextTick();
  scheduleChartsRender();
  try {
    const points = await fetchVisitorPoints();
    if (!homeUnmounted && points && points.length > 0) {
      visitorPoints.value = points;
    }
  } catch {}
  try {
    const stats = await fetchVisitorStats();
    if (!homeUnmounted && stats) visitorStats.value = stats;
  } catch {}
});

onBeforeUnmount(() => {
  homeUnmounted = true;
  clearTimeout(placeholderTimer);
  if (chartRenderFrame !== null) window.cancelAnimationFrame(chartRenderFrame);
  if (chartRenderTimer !== null) window.clearTimeout(chartRenderTimer);
  chartRenderFrame = null;
  chartRenderTimer = null;
  tissueChart?.dispose();
  biosampleChart?.dispose();
  tissueChart = null;
  biosampleChart = null;
});
  </script>
  
  <style scoped>
  /* 全局：让 section 背景铺满 */
  .hero-wrap {
    width: 100%;
    background: linear-gradient(180deg, var(--bg) 0%, var(--surface) 100%);
    padding: 34px 0 26px;
  }
  
  .hero {
    display: grid;
    grid-template-columns: 1.15fr 0.85fr;
    gap: 34px;
    align-items: stretch;
  }
  .left { display: flex; flex-direction: column; height: 100%; }
  .left .carousel { flex: 1; min-height: 320px; }
  .left .carousel :deep(.el-carousel__container),
  .left .carousel :deep(.el-carousel__item),
  .left .carousel :deep(.el-carousel__item),
  .left .slide { height: 100%; }
  
  /* 左侧 */
  .title-row { display: flex; align-items: baseline; gap: 14px; }
  
  .big-title{
    font-size: 58px;
    font-weight: 900;
    letter-spacing: 0.5px;
    display: inline-block;
  
    background-image: repeating-linear-gradient(
      90deg,
      var(--brand-ink) 0%,
      var(--ink-soft) 28%,
      var(--brand-primary) 46%,
      var(--brand-primary-2) 52%,
      var(--brand-primary) 58%,
      var(--ink-soft) 76%,
      var(--brand-ink) 100%
    );
  
    --tile: 900px;
    background-size: var(--tile) 100%;
    background-repeat: repeat;
    background-position: 0px 50%;
  
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent !important;
    -webkit-text-fill-color: transparent !important;
  
    animation: titleLoopL2R 4.2s linear infinite;
    will-change: background-position;
  }
  
  @keyframes titleLoopL2R{
    from { background-position: 0px 50%; }
    to   { background-position: calc(var(--tile) * 1) 50%; }
  }
  
  @media (prefers-reduced-motion: reduce) {
    .big-title { animation: none; }
  }
  
  .subtitle { margin-top: 10px; font-size: 15px; color: var(--muted); }
  .typewriter {
    display: inline-block;
    overflow: hidden;
    white-space: nowrap;
    border-right: 2px solid var(--brand-primary-3);
    animation: typed 2.5s steps(80, end), blink 0.7s step-end 4;
    animation-fill-mode: forwards;
  }
  @keyframes typed {
    from { max-width: 0; }
    to   { max-width: 100%; border-right-color: transparent; }
  }
  @keyframes blink {
    50% { border-right-color: transparent; }
  }

  .search-btn {
    background: var(--brand-primary-3) !important;
    border-color: var(--brand-primary-3) !important;
    color: #fff !important;
    font-weight: 800 !important;
    transition: background 0.18s ease, box-shadow 0.18s ease !important;
  }
  .search-btn:hover {
    background: #7f9a90 !important;
    border-color: #7f9a90 !important;
    box-shadow: 0 4px 14px rgba(95,125,112,0.24) !important;
  }
  
  .searchbar {
    margin-top: 16px;
    display: grid;
    grid-template-columns: 180px 1fr;
    gap: 10px;
  }
  .sel { width: 100%; }
  .inp { width: 100%; }
  
  .example {
    margin-top: 10px;
    font-size: 13px;
    color: var(--muted);
  }
  .example-link { border: none; background: transparent; color: var(--muted); font: inherit; cursor: pointer; padding: 0; transition: color 0.15s; }
  .example-link:hover { color: var(--text); }
  .example-link b { color: var(--brand-primary-3); }
  /* ✅ 轮播 */
  .carousel {
    margin-top: 14px;
    border-radius: 14px;
    overflow: hidden;
    border: 1px solid var(--border);
    background: var(--carousel-bg);
  }
  
  .slide {
    width: 100%;
    height: 320px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .slide-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  
  .slide-placeholder {
    width: 100%;
    height: 100%;
    background: repeating-linear-gradient(
      135deg,
      var(--surface),
      var(--surface) 14px,
      var(--surface-2) 14px,
      var(--surface-2) 28px
    );
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: var(--text-soft);
  }
  .ph-title { font-weight: 900; font-size: 18px; }
  .ph-sub { font-size: 13px; opacity: .9; }
  
  /* 右侧 */
  .right .h2 { font-size: 34px; font-weight: 900; margin-top: 14px; color: #1a2623; }
  .desc { margin-top: 12px; line-height: 1.75; text-align: justify; }
  .desc, .desc p, .desc span { color: #1a2623 !important; }
  .desc .hl { color: #2d6a4f !important; font-weight: 900; font-style: italic; }
  .desc p { margin: 0; }

  /* ── Data overview ──────────────────────── */
  .overview-stats {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 12px;
    margin-bottom: 20px;
  }
  .os-card {
    text-align: center;
    padding: 20px 10px;
    background: linear-gradient(160deg, #fbfdfc 0%, #f4f7f6 50%, #eef3f1 100%);
    border: 1px solid rgba(143,165,156,0.18);
    border-radius: 16px;
    cursor: default;
    transition:
      border-color 0.35s ease,
      box-shadow 0.35s ease;
    animation: osCardRest 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
    position: relative;
    overflow: hidden;
  }
  .os-card::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at 50% 0%, rgba(143,165,156,0.08) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s ease;
  }
  .os-card:hover {
    border-color: rgba(143,165,156,0.40);
    box-shadow:
      0 12px 28px rgba(95,125,112,0.14),
      0 0 0 1px rgba(143,165,156,0.15) inset;
    animation: osCardJelly 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  }
  .os-card:hover::before {
    opacity: 1;
  }
  @keyframes osCardRest {
    0%   { transform: scale(1.04); border-radius: 20px; }
    100% { transform: scale(1);    border-radius: 16px; }
  }
  @keyframes osCardJelly {
    0%   { transform: scale(1) rotate(0deg);    border-radius: 16px 16px 16px 16px; }
    15%  { transform: scale(1.04) rotate(0.3deg);  border-radius: 18px 14px 20px 13px; }
    30%  { transform: scale(0.97) rotate(-0.2deg); border-radius: 13px 19px 14px 19px; }
    50%  { transform: scale(1.02) rotate(0.1deg);  border-radius: 19px 13px 18px 15px; }
    70%  { transform: scale(0.99) rotate(-0.05deg); border-radius: 15px 18px 14px 18px; }
    100% { transform: scale(1) rotate(0deg);       border-radius: 16px; }
  }
  .os-num {
    font-size: 24px;
    font-weight: 900;
    font-style: italic;
    color: var(--brand-primary-3);
    line-height: 1.1;
    letter-spacing: -0.5px;
    transition: transform 0.3s ease;
    position: relative;
    z-index: 1;
  }
  .os-card:hover .os-num {
    transform: scale(1.08);
  }
  .os-label {
    font-size: 11.5px;
    font-weight: 800;
    color: var(--muted);
    margin-top: 5px;
    letter-spacing: 0.3px;
    position: relative;
    z-index: 1;
  }
  .overview-charts {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }
  .ochart--wide { grid-column: 1 / -1; }
  .ochart { min-height: 420px; }
  .ochart-title { font-size: 15px; font-weight: 900; margin-bottom: 10px; position: relative; }
  .ochart-help { position: absolute; top: -2px; margin-left: 6px; display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; }
  .ochart-canvas {
    width: 100%;
    height: 380px;
    position: relative;
    overflow: hidden;
  }
  .ochart-canvas:empty {
    border-radius: 12px;
    background: linear-gradient(100deg, rgba(244,247,246,0.9) 0%, rgba(255,255,255,0.95) 45%, rgba(232,239,235,0.9) 100%);
    background-size: 200% 100%;
    animation: ochart-loading 1.2s ease-in-out infinite;
  }
  .ochart-canvas:empty::after {
    content: "";
    position: absolute;
    inset: 22px;
    border-radius: 10px;
    border: 1px dashed rgba(143,165,156,0.25);
    background:
      linear-gradient(rgba(143,165,156,0.10), rgba(143,165,156,0.10)) 12% 24% / 54% 8px no-repeat,
      linear-gradient(rgba(143,165,156,0.12), rgba(143,165,156,0.12)) 12% 45% / 72% 8px no-repeat,
      linear-gradient(rgba(143,165,156,0.10), rgba(143,165,156,0.10)) 12% 66% / 38% 8px no-repeat;
  }
  @keyframes ochart-loading {
    0%, 100% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
  }
  @media (prefers-reduced-motion: reduce) {
    .ochart-canvas:empty {
      animation: none;
    }
  }

  @media (max-width: 760px) {
    .overview-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .overview-charts { grid-template-columns: minmax(0, 1fr); }
    .ochart,
    .ochart-canvas {
      max-width: 100%;
      min-width: 0;
      overflow: hidden;
    }
    .ochart-canvas :deep(div),
    .ochart-canvas :deep(canvas) {
      width: 100% !important;
      max-width: 100% !important;
    }
    .os-num { font-size: 18px; }
  }

  .notice {
    margin-top: 18px;
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    padding: 12px 14px;
  }
  .notice-title { font-weight: 900; margin-bottom: 8px; }
  .notice ul { margin: 0; padding-left: 18px; color: var(--muted); line-height: 1.7; }
  
  /* 分割线全宽 */
  .divider { height: 1px; background: var(--divider); display: none !important; }
  
  /* 卡片区背景也铺开 */
  .cards-wrap {
    width: 100%;
    background: var(--surface);
    padding: 22px 0 44px;
  }
  
  .cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  
  .card {
    padding: 16px 16px; /* card 自己不再管 border/shadow，交给 float-card */
  }
  .card-title { font-weight: 900; margin-bottom: 8px; }
  .card-body { color: var(--muted); line-height: 1.6; font-size: 14px; }
  
  @media (max-width: 980px) {
    .hero { grid-template-columns: 1fr; }
    .cards { grid-template-columns: 1fr; }
    .searchbar { grid-template-columns: 1fr; }
  }
  
  /* Figure1 区块整体（全宽背景 + 内部 container） */
  .figure1-wrap{
    width: 100%;
    background: var(--surface);
    padding: 26px 0 60px;
  }
  
  .figure1-card{
    padding: 18px 18px 14px;
  }
  
  /* 头部文字 */
  .figure1-head{
    padding: 2px 6px 10px;
  }
  .figure1-title{
    font-weight: 900;
    font-size: 18px;
    color: var(--text);
  }
  .figure1-sub{
    margin-top: 6px;
    font-size: 13px;
    color: var(--muted);
    line-height: 1.6;
  }
  
  /* 图区域 */
  .figure1-body{
    margin-top: 10px;
    padding: 10px;
    border-radius: 16px;
    background: var(--figure-body-bg);
    border: 1px dashed var(--border-brand);
  }
  
  /* 占位框：保证有“论文图那种大块感” */
  .figure1-placeholder{
    width: 100%;
    aspect-ratio: 21 / 9;
    border-radius: 14px;
    background: repeating-linear-gradient(
      135deg,
      var(--surface),
      var(--surface) 14px,
      var(--surface-2) 14px,
      var(--surface-2) 28px
    );
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    text-align: center;
  }
  .ph-main{
    font-size: 20px;
    font-weight: 900;
    color: var(--text);
  }
  .ph-sub{
    font-size: 13px;
    color: var(--muted);
    max-width: 720px;
    line-height: 1.6;
  }
  
  /* 发文后替换为 img 时用 */
  .figure1-img{
    width: 100%;
    height: auto;
    display: block;
    border-radius: 14px;
  }
  
  @media (max-width: 980px){
    .figure1-placeholder{ aspect-ratio: 16 / 9; }
  }
  
  /* 整块 Hero 大卡片 */
  .hero-card{
    padding: 22px 22px 18px;
  }
  
  /* section modules */
  .section-wrap{
    width: 100%;
    background: var(--surface);
    padding: 22px 0 22px;
  }
  
  .section-card{
    padding: 18px 18px 16px;
  }
  
  .sec-head{
    padding: 2px 6px 12px;
  }
  .sec-title{
    font-weight: 900;
    font-size: 18px;
  }
  .sec-sub{
    margin-top: 6px;
    font-size: 13px;
    color: var(--muted);
  }
  
  .sec-foot{
    margin-top: 10px;
    padding: 0 6px;
    font-size: 12px;
    color: var(--muted);
  }

  /* ── Workflow card ── */
  .workflow-card { padding: 18px 18px 14px; }
  .workflow-img-wrap {
    position: relative;
    border-radius: 14px;
    overflow: hidden;
    box-shadow:
      0 2px 12px rgba(95,125,112,0.10),
      0 0 0 1px rgba(143,165,156,0.18);
    background: linear-gradient(135deg, #f8faf9 0%, #f0f5f3 50%, #f8faf9 100%);
    transition: box-shadow 0.35s ease;
  }
  .workflow-img-wrap:hover {
    box-shadow:
      0 8px 28px rgba(95,125,112,0.16),
      0 0 0 1px rgba(143,165,156,0.30);
  }
  /* subtle inner glow at edges */
  .workflow-img-wrap::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 14px;
    pointer-events: none;
    z-index: 1;
    box-shadow: inset 0 0 40px rgba(143,165,156,0.08);
  }
  .workflow-img {
    display: block;
    width: 100%;
    height: auto;
    border-radius: 14px;
  }
  .workflow-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    min-height: 260px;
    padding: 40px 24px;
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
    text-align: center;
  }
  .workflow-placeholder-icon { font-size: 48px; opacity: 0.5; }
  .workflow-placeholder code {
    background: rgba(143,165,156,0.12);
    padding: 3px 8px;
    border-radius: 6px;
    font-size: 12px;
    color: var(--brand-primary-3);
    font-weight: 800;
  }

  .proj-grid{
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
    padding: 4px 6px 2px;
  }
  @media (max-width: 1200px){
    .proj-grid{ grid-template-columns: repeat(2, 1fr); }
  }
  @media (max-width: 980px){
    .proj-grid{ grid-template-columns: 1fr; }
  }

  /* ── Visitors: globe + map + stats 融合 ── */
  .visitors-section { position: relative; }
  .visitors-wrap { display: flex; gap: 16px; align-items: stretch; }
  .visitors-stats {
    flex-shrink: 0;
    width: 260px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding-top: 4px;
  }
  .visitors-map { flex: 1; min-width: 0; z-index: 1; }
  .visitors-globe {
    position: absolute;
    top: 14px;
    right: 16px;
    z-index: 2;
    pointer-events: none;
  }
  .visitors-globe .globe-window {
    pointer-events: auto;
  }

  /* Stats cards with 3D tilt */
  .vstat-card {
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 18px 12px;
    border-radius: 14px;
    background: linear-gradient(145deg, #fcfdfc, #f4f7f5);
    border: 1px solid rgba(143,165,156,0.16);
    box-shadow: 0 4px 16px rgba(15,23,42,0.04);
    cursor: default;
    overflow: hidden;
    transition: transform 0.35s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.35s ease;
    transform-style: preserve-3d;
    perspective: 600px;
  }
  .vstat-card:hover {
    box-shadow: 0 12px 32px rgba(15,23,42,0.10);
  }
  .vstat-icon { font-size: 28px; line-height: 1; }
  .vstat-num {
    font-size: 28px;
    font-weight: 900;
    color: #1B2A27;
    letter-spacing: -0.4px;
  }
  .vstat-label {
    font-size: 11px;
    font-weight: 700;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.6px;
  }
  .vstat-sublabel {
    font-size: 10px;
    font-weight: 600;
    color: rgba(143,165,156,0.6);
    margin-top: -2px;
  }
  /* Decorative background accents */
  .vstat-deco {
    position: absolute;
    pointer-events: none;
    opacity: 0.12;
  }
  .vstat-deco--dots {
    bottom: -14px; right: -10px;
    width: 56px; height: 56px;
    background: radial-gradient(circle, var(--brand-primary-3) 2px, transparent 2px);
    background-size: 14px 14px;
  }
  .vstat-deco--waves {
    bottom: 0; left: 0; right: 0;
    height: 28px;
    background: repeating-linear-gradient(
      90deg,
      transparent,
      transparent 6px,
      rgba(143,165,156,0.3) 6px,
      rgba(143,165,156,0.3) 7px
    );
    mask-image: linear-gradient(to top, rgba(0,0,0,0.15), transparent);
  }
  .vstat-ring {
    position: absolute;
    inset: -1px;
    border-radius: 14px;
    border: 2px solid transparent;
    background: linear-gradient(145deg, rgba(143,165,156,0.22), transparent 50%, rgba(143,165,156,0.08)) border-box;
    -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    pointer-events: none;
  }
  .vstat-dot {
    position: absolute;
    top: 10px;
    right: 12px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #4CAF50;
    box-shadow: 0 0 8px rgba(76,175,80,0.5);
    animation: vstat-pulse 2s ease-in-out infinite;
  }
  @keyframes vstat-pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(1.4); }
  }
  .globe-window {
    width: 160px;
    height: 160px;
    border-radius: 50%;
    border: 3px solid rgba(255,255,255,0.85);
    box-shadow: 0 0 0 4px rgba(143,165,156,0.12), 0 4px 20px rgba(15,23,42,0.06);
    display: flex;
    align-items: center;
    justify-content: center;
  }
  @media (max-width: 980px){
    .visitors-wrap {
      flex-direction: column;
    }
    .visitors-stats {
      width: 100%;
      flex-direction: row;
      gap: 8px;
    }
    .vstat-card {
      flex: 1;
      padding: 14px 8px;
    }
    .vstat-num { font-size: 22px; }
    .visitors-globe {
      top: 4px;
      right: 4px;
    }
  }

  .proj-card{
    display: flex;
    flex-direction: column;

    min-height: 132px;          /* 你想更高就 140/150 */
    height: 100%;

    padding: 14px 14px 12px;
    border-radius: 16px;
    border: 1px solid var(--border);
    background: var(--surface-2);
    transition: transform .12s ease, box-shadow .12s ease;
  }
  .proj-card:hover{
    transform: translateY(-2px);
    box-shadow: var(--shadow-hover);
  }
  .proj-name{ font-weight: 900; }
  .proj-desc{ 
    margin-top: 6px; 
    color: var(--muted); 
    font-size:13px; 
    line-height:1.6; 

    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
}
  .proj-meta{ 
    margin-top: auto; 
    padding-top: 10px;

    font-size: 12px; 
    color: var(--meta-text); 
    font-weight: 700; 
    display: block;
}
  
  .cite-box{
    padding: 10px 12px;
    border: 1px solid var(--border);
    border-radius: 16px;
    background: var(--surface-2);
    margin: 0 6px;
  }
  .cite-label{ font-weight: 900; font-size: 13px; margin-bottom: 8px; }
  .cite-pre{
    margin: 0;
    padding: 10px 10px;
    border-radius: 12px;
    background: var(--surface);
    border: 1px dashed var(--border-brand);
    font-size: 12px;
    color: var(--text);
    white-space: pre-wrap;
    line-height: 1.6;
  }
  .cite-actions{ margin-top: 10px; display:flex; gap: 10px; }
  
  @media (max-width: 980px){
    .proj-grid{ grid-template-columns: 1fr; }
  }
  @media (max-width: 480px) {
    .big-title { font-size: 32px; }
    .subtitle { font-size: 14px; }
    .hero-card { padding: 10px; }
    .hero-wrap { padding: 16px 0 14px; }
    .carousel { min-height: 200px; }
    .slide { height: 200px; }
    .overview-stats { grid-template-columns: repeat(2, 1fr); }
    .os-num { font-size: 20px; }
    .os-card { padding: 10px 6px; }
    .visitors-layout { grid-template-columns: 1fr; }
    .workflow-img { width: 100%; }
    .section-wrap { padding: 0; }
    .section-card { padding: 12px; border-radius: 12px; }
  }

  @media (max-width: 768px) {
    .home,
    .hero-wrap,
    .section-wrap,
    .hero-card,
    .section-card,
    .workflow-card {
      max-width: 100%;
      min-width: 0;
      overflow-x: clip;
    }

    .hero,
    .left,
    .right,
    .searchbar,
    .carousel,
    .slide,
    .overview-stats,
    .overview-charts,
    .workflow-img-wrap,
    .proj-grid,
    .visitors-map {
      max-width: 100%;
      min-width: 0;
    }

    .hero {
      grid-template-columns: minmax(0, 1fr);
    }

    .typewriter {
      max-width: 100%;
      white-space: normal;
      overflow: visible;
      border-right: none;
      animation: none;
    }

    .searchbar :deep(.el-select),
    .searchbar :deep(.el-input),
    .searchbar :deep(.el-input-group),
    .searchbar :deep(.el-input__wrapper) {
      width: 100%;
      max-width: 100%;
      min-width: 0;
    }

    .example {
      overflow-wrap: anywhere;
    }

    .slide-img,
    .workflow-img {
      max-width: 100%;
    }

    .visitors-wrap {
      gap: 10px;
    }

    .visitors-map :deep(.map) {
      height: 320px;
    }

    .visitors-globe {
      position: static;
      display: flex;
      justify-content: center;
      align-self: center;
      width: 100%;
      margin-top: 0;
      pointer-events: auto;
    }

    .globe-window,
    .visitors-globe :deep(.globe) {
      width: 112px;
      height: 112px;
      max-width: 100%;
    }
  }

  @media (max-width: 480px) {
    .visitors-stats {
      flex-direction: column;
    }

    .ochart {
      min-height: 320px;
    }

    .ochart-canvas {
      height: 300px;
    }

    .visitors-map :deep(.map) {
      height: 280px;
    }

    .globe-window,
    .visitors-globe :deep(.globe) {
      width: 104px;
      height: 104px;
    }
  }
  </style>

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
                A comprehensive human and mouse chromatin accessibility source platform
                </div>
    
                <div class="searchbar">
                <el-select v-model="tissue" placeholder="Tissue type" class="sel" clearable>
                    <el-option v-for="x in tissueOptions" :key="x" :label="x" :value="x" />
                </el-select>
    
                <el-input
                    v-model="keyword"
                    placeholder="Please input"
                    class="inp"
                    clearable
                    @keyup.enter="onSearch"
                >
                    <template #append>
                    <el-button @click="onSearch">Search</el-button>
                    </template>
                </el-input>
                </div>
    
                <div class="example">
                Example: <b>Tissue type</b>: Lung &nbsp;&nbsp; <b>Disease type</b>: Lung cancer
                &nbsp;&nbsp; <b>Sample ID</b>: Sample_H_0001
                </div>
    
                <!-- ✅ 左侧轮播：你说的 carousel -->
                <el-carousel
                class="carousel"
                height="320px"
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
                数据库的简介：包含哪些数据类型（bulk ATAC / scATAC / pseudo-bulk /
                peak annotation / TF footprint 等），支持哪些检索与可视化（按组织/疾病/细胞类型、
                基因/区域检索、下载、统计、Genome browser）。
              </p>
              <p>
                写成 6~10 行，不要堆满；细节将会放到 Help / About 页面。
              </p>
            </div>
  
            <div class="notice">
              <div class="notice-title">Quick notes</div>
              <ul>
                <li>Data sources: GEO / SRA / internal curated datasets</li>
                <li>Core modules: Browse / Search / Genome browser / Download</li>
                <li>Versioning: keep track of releases and changes</li>
              </ul>
            </div>
              </aside>
            </div>
          </div>
        </div>
      </section>
  
      <!-- 分割线（全宽） -->
      <div class="divider"></div>
  
      <!-- 卡片区：也做成全宽 -->
      <section class="cards-wrap">
        <div class="container cards">
          <div class="card float-card">
            <div class="card-title">Data collection</div>
            <div class="card-body">数据来源、样本统计、物种与组织覆盖范围。</div>
          </div>
          <div class="card float-card">
            <div class="card-title">Annotation</div>
            <div class="card-body">CAR/peak 注释、enhancer、SNP/eQTL、3D 互作等。</div>
          </div>
          <div class="card float-card">
            <div class="card-title">TF footprint</div>
            <div class="card-body">motif / footprint / TF activity / TR matrix 等模块入口。</div>
          </div>
        </div>
      </section>

      <!-- Figure 1 预留区（新增） -->
    <section class="figure1-wrap">
    <div class="container">
        <div class="float-card figure1-card">
            <div class="figure1-head">
                <div class="figure1-title">Figure 1 · Database overview</div>
                <div class="figure1-sub">
                预留论文 Figure 1 区域（发文后替换为正式图；当前仅占位）
                </div>
            </div>

        <!-- 占位-->
            <div class="figure1-body">
                <div class="figure1-placeholder">
                <div class="ph-main">Figure 1 Placeholder</div>
                <div class="ph-sub">
                    建议最终图片宽高比接近 16:9 或 21:9；发布后替换为实际 Figure 1。
                </div>
                </div>

                <!--  -->
                
                <!-- <img class="figure1-img" :src="figure1" alt="Figure 1" />
                -->
            </div>
        </div>
    </div>
    </section>

    <!-- ① Table Analysis（占位表格） -->
    <section class="section-wrap">
    <div class="container">
        <div class="float-card section-card">
        <div class="sec-head">
            <div class="sec-title">Table analysis</div>
            <div class="sec-sub">占位：后续可接后端 API 动态加载 / 筛选 / 分页</div>
        </div>

        <StatsBarCharts :data="statsData" />

        <div class="sec-foot">
            <div class="muted">* 当前使用前端 mock 数据；后续由服务器返回统计结果。</div>
        </div>
        </div>
    </div>
    </section>

    <!-- ② Sister projects（悬浮卡片列表） -->
    <section class="section-wrap">
    <div class="container">
        <div class="float-card section-card">
        <div class="sec-head">
            <div class="sec-title">Sister projects</div>
            <div class="sec-sub">Other database/resource entry points of the research group</div>
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

    <!-- ③ Cite（引用信息 + 链接占位） -->
    <section class="section-wrap">
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

    <!-- ④ Contact + Visitors（联系信息 + 动态世界地图） -->
    <section class="section-wrap">
    <div class="container">
        <div class="float-card section-card">
        <div class="sec-head">
            <div class="sec-title">Contact us & Visitors</div>
            <div class="sec-sub">联系信息 + 访问分布展示</div>
        </div>

        <div class="contact-grid">
            <div class="contact-left">
            <div class="contact-item"><b>Lab:</b> Your Lab Name</div>
            <div class="contact-item"><b>Email:</b> lab@example.com</div>
            <div class="contact-item"><b>Address:</b> Your institute / city</div>

            <div class="contact-note">
                * 这里后续可加一个简单的留言表单（也可只放邮箱）。
            </div>
            </div>

            <div class="contact-right">
            <VisitorsMap :points="visitorPoints" />
            </div>
        </div>
        </div>
    </div>
    </section>
    </div>
  </template>
  
  <script setup lang="ts">
  import { onMounted, ref } from "vue";
  import { ElMessage } from "element-plus";
  import StatsBarCharts from "@/components/StatsBarCharts.vue";
  import VisitorsMap from "@/components/VisitorsMap.vue";
  import { fetchVisitorPoints, type VisitorPoint } from "@/api/home";
  
  // ✅ 你后面把真实截图放到 src/assets/slides/ 下面，再把 import 打开
  // import slide1 from "@/assets/slides/slide1.png";
  // import slide2 from "@/assets/slides/slide2.png";
  // import slide3 from "@/assets/slides/slide3.png";
  
  const tissue = ref<string>("");
  const keyword = ref<string>("");
  
  const tissueOptions = ["Lung", "Liver", "Brain", "Blood", "Colon", "Skin", "Breast", "Pancreas"];
  
  // 没图也能跑：先用 placeholder；你把上面的 import 打开后，把 src 换成 slide1/2/3
  const slides = ref<Array<{ src?: string }>>([
    { src: undefined },
    { src: undefined },
    { src: undefined },
  ]);
  
  const onSearch = () => {
    ElMessage.info(`Search: tissue=${tissue.value || "-"} keyword=${keyword.value || "-"}`);
  };

  const statsData = {
  versions: ["ATACdb1.0", "ATACdb2.0"],
  samples: {
    human: [1493, 4031],
    mouse: [0, 1273],
  },
  cars: {
    human: [52078833, 319968559],
    mouse: [0, 75639252],
  },
};

const sisterProjects = [
  { name: "SEdb", url: "http://www.licpathway.net:8081/sedb/", desc: "The comprehensive human Super-Enhancer database.", tag: "Database" },
  { name: "SEanalysis", url: "https://bio.liclab.net/SEanalysis/", desc: "Super-Enhancer associated regulatory analysis.", tag: "Tools" },
  { name: "ATACdb", url: "https://bio.liclab.net/ATACdb/", desc: "A comprehensive human and mouse chromatin accessibility source platform.", tag: "Database" },
  { name: "KnockTF", url: "http://www.licpathway.net/KnockTF/", desc: "TF perturbation/knockdown expression profiles.", tag: "Database" },
  { name: "scATAC-Ref", url: "https://bio.liclab.net/scATAC-Ref/", desc: "A reference of scATAC-seq with known cell labels in multiple species.", tag: "Database" },
  { name: "SpatialRef", url: "https://bio.liclab.net/spatialref/", desc: "A reference of spatial omics with known spot annotation.", tag: "Database" },
  { name: "sc2GWAS", url: "https://bio.liclab.net/sc2GWAS/", desc: "a comprehensive platform linking single cell and GWAS traits of human.", tag: "Database" },
  { name: "scImmOmics", url: "https://bio.liclab.net/scImmOmics/home", desc: " a manually curated single-cell multi-omics immune data.", tag: "Database" },
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


const FALLBACK_VISITOR_POINTS: VisitorPoint[] = [
  { name: "Singapore", lat: 1.29, lon: 103.85, value: 2 },
  { name: "Beijing", lat: 39.90, lon: 116.40, value: 3 },
  { name: "Los Angeles", lat: 34.05, lon: -118.24, value: 2 },
  { name: "London", lat: 51.50, lon: -0.12, value: 1 },
];

const visitorPoints = ref<VisitorPoint[]>(FALLBACK_VISITOR_POINTS);

onMounted(async () => {
  try {
    const points = await fetchVisitorPoints();
    if (points && points.length > 0) {
      visitorPoints.value = points;
    }
  } catch {
    // Keep fallback data if the API is unreachable.
  }
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
    align-items: start;
  }
  
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
    object-fit: cover;
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
  .right .h2 { font-size: 34px; font-weight: 900; margin-top: 6px; }
  .desc { margin-top: 12px; color: var(--text); line-height: 1.75; }
  
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
  
  .contact-grid{
    display: grid;
    grid-template-columns: 0.42fr 0.58fr;
    gap: 16px;
    padding: 0 6px;
  }
  .contact-left{
    border: 1px solid var(--border);
    border-radius: 16px;
    background: var(--surface-2);
    padding: 14px;
  }
  .contact-item{ margin-bottom: 10px; color: var(--text); }
  .contact-note{ margin-top: 12px; font-size: 12px; color: var(--muted); line-height:1.6; }
  .contact-right{
    border-radius: 16px;
    min-width: 0;
    overflow: hidden;
  }

  @media (max-width: 980px){
    .proj-grid{ grid-template-columns: 1fr; }
    .contact-grid{ grid-template-columns: 1fr; }
    .contact-right {
      min-width: 0;
      overflow: hidden;
      max-width: 100%;
    }
  }
  </style>

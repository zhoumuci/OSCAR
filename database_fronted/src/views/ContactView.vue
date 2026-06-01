<template>
    <div class="contact-page">
        <div class="container">
            <div class="page-title">Contact</div>

            <!-- 上半：Contact + Map -->
            <div class="contact-top float-card">
                <div class="contact-grid">
                    <!-- 左侧信息 -->
                    <div class="info">
                        <div class="line">
                            <span class="label">Principal Investigator:</span>
                            <span class="value">Chunquan Li, Ph.D.</span>
                        </div>

                        <div class="line">
                            <span class="label">Affiliation:</span>
                            <span class="value">
                                The First Affiliated Hospital, University of South China, Hengyang 421001, China
                            </span>
                        </div>

                        <div class="line">
                            <span class="label">Email:</span>
                            <a class="mail" :href="`mailto:${CONTACT_EMAIL}`">{{ CONTACT_EMAIL }}</a>
                        </div>

                        <div class="divider"></div>

                        <div class="note">
                            <i>We welcome researchers from all over the world to provide valuable advice for
                            <b>OSCAR</b>, and make <b>OSCAR</b> more and more perfect.
                            </i>
                        </div>
                    </div>

                    <!-- 右侧地图（先用 iframe 占位，后续可换成你们 VisitorsMap/ECharts） -->
                    <div class="map-wrap">
                        <!-- 方案1：Google Maps Embed（你把 q= 后面换成你们地址即可） -->
                        <iframe class="map" loading="lazy" referrerpolicy="no-referrer-when-downgrade"
                            :src="mapSrc"></iframe>

                        <!-- 方案2（可选）：你们自己的 VisitorsMap
              <VisitorsMap :points="visitorPoints" />
              -->
                    </div>
                </div>
            </div>

            <!-- 下半：把 Sister projects 搬上来撑页面 -->
            <div class="sister float-card">
                <div class="sister-head">
                    <div class="sister-title">Sister projects</div>
                    <div class="sister-sub">Other database/resource entry points of the research group</div>
                </div>

                <div class="proj-grid">
                    <a v-for="p in sisterProjects" :key="p.name" class="proj-card" :href="p.url" target="_blank"
                        rel="noreferrer">
                        <div class="proj-name">{{ p.name }}</div>
                        <div class="proj-desc">{{ p.desc }}</div>
                        <div class="proj-meta">{{ p.tag }}</div>
                    </a>
                </div>
            </div>

            <!-- 额外留一点底部呼吸，避免贴 footer 太近 -->
            <div class="bottom-pad"></div>
        </div>
    </div>
</template>

<script setup lang="ts">
// 如果你后续要换 VisitorsMap，把下面注释打开
// import VisitorsMap from "@/components/VisitorsMap.vue";

const CONTACT_EMAIL = "lcqbio@163.com";
const mapSrc =
  "https://www.google.com/maps?q=26.902942492244456,112.60116926831718&z=17&output=embed";
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
</script>

<style scoped>
.contact-page {
    width: 100%;
    padding: 18px 0 30px;
    background: var(--bg);
}

.page-title {
    font-size: 32px;
    font-weight: 900;
    margin: 6px 0 14px;
}

/* 上半卡片 */
.contact-top {
    padding: 16px;
}

.contact-grid {
    display: grid;
    grid-template-columns: 1.05fr 0.95fr;
    gap: 16px;
    align-items: start;
}

/* 左侧信息 */
.info {
    padding: 10px 10px 8px;
}

.line {
    display: grid;
    grid-template-columns: 170px 1fr;
    gap: 10px;
    padding: 8px 0;
}

.label {
    font-weight: 900;
    color: var(--text);
}

.value {
    color: var(--text);
    line-height: 1.6;
}

.mail {
    color: var(--brand-primary-3);
    font-weight: 800;
    text-decoration: none;
}

.mail:hover {
    text-decoration: underline;
}

.divider {
    height: 1px;
    background: var(--border);
    margin: 12px 0;
}

.note {
    color: var(--muted);
    line-height: 1.7;
}

/* 右侧地图 */
.map-wrap {
    border: 1px solid var(--border);
    border-radius: 14px;
    overflow: hidden;
    background: var(--surface-2);
}

.map {
    width: 100%;
    height: 360px;
    border: 0;
    display: block;
}

/* Sister projects */
.sister {
    margin-top: 14px;
    padding: 16px 16px 14px;
}

.sister-head {
    padding: 2px 6px 10px;
}

.sister-title {
    font-weight: 900;
    font-size: 18px;
    color: var(--text);
}

.sister-sub {
    margin-top: 6px;
    font-size: 13px;
    color: var(--muted);
}

.proj-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
    padding: 6px;
}

@media (max-width: 1200px) {
    .proj-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 980px) {
    .contact-grid {
        grid-template-columns: 1fr;
    }

    .proj-grid {
        grid-template-columns: 1fr;
    }
}

.proj-card {
    display: flex;
    flex-direction: column;
    min-height: 132px;
    height: 100%;

    padding: 14px 14px 12px;
    border-radius: 16px;
    border: 1px solid var(--border);
    background: var(--surface-2);
    transition: transform .12s ease, box-shadow .12s ease;
    text-decoration: none;
    color: inherit;
}

.proj-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-hover);
}

.proj-name {
    font-weight: 900;
}

.proj-desc {
    margin-top: 6px;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
}

.proj-meta {
    margin-top: auto;
    padding-top: 10px;
    font-size: 12px;
    font-weight: 700;
    color: var(--meta-text);
    display: block;
}

.bottom-pad {
    height: 10px;
}
</style>

<template>
  <div class="contact-page">
    <div class="container">
      <div class="page-title">Contact</div>

      <div class="contact-top float-card">
        <div class="contact-grid">
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

          <div class="map-wrap">
            <div ref="mapEl" class="map"></div>
          </div>
        </div>
      </div>

      <div class="sister float-card">
        <div class="sister-head">
          <div class="sister-title">Sister projects</div>
          <div class="sister-sub">Other database/resource entry points of the research group</div>
        </div>
        <div class="proj-grid">
          <a v-for="p in sisterProjects" :key="p.name" class="proj-card" :href="p.url" target="_blank" rel="noreferrer">
            <div class="proj-name">{{ p.name }}</div>
            <div class="proj-desc">{{ p.desc }}</div>
            <div class="proj-meta">{{ p.tag }}</div>
          </a>
        </div>
      </div>

      <div class="bottom-pad"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const CONTACT_EMAIL = "lcqbio@163.com";

const mapEl = ref<HTMLDivElement | null>(null);
let contactMap: L.Map | null = null;

onMounted(() => {
  if (!mapEl.value) return;
  // Fix Leaflet default icon paths broken by bundler
  delete (L.Icon.Default.prototype as any)._getIconUrl;
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  });

  const lat = 26.901883;
  const lng = 112.601585;

  contactMap = L.map(mapEl.value, {
    center: [lat, lng],
    zoom: 15,
    zoomControl: true,
    attributionControl: false,
  });

  L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png", {
    maxZoom: 18,
    subdomains: ['a', 'b', 'c', 'd'],
  }).addTo(contactMap);

  L.marker([lat, lng])
    .addTo(contactMap)
    .bindPopup("<b>The First Affiliated Hospital, USC</b><br/>Hengyang 421001, China")
    .openPopup();
});

onBeforeUnmount(() => {
  contactMap?.remove();
  contactMap = null;
});

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
</script>

<style scoped>
.contact-page { width: 100%; padding: 18px 0 30px; background: var(--bg); }
.page-title { font-size: 32px; font-weight: 900; margin: 6px 0 14px; }
.contact-top { padding: 16px; }
.contact-grid { display: grid; grid-template-columns: 1.05fr 0.95fr; gap: 16px; align-items: start; }
.info { padding: 10px 10px 8px; }
.line { display: grid; grid-template-columns: 170px 1fr; gap: 10px; padding: 10px 0; font-size: 17px; }
.label { font-weight: 900; color: var(--text); font-size: 17px; }
.value { color: var(--text); line-height: 1.6; font-size: 17px; }
.mail { color: var(--brand-primary-3); font-weight: 800; text-decoration: none; font-size: 17px; }
.mail:hover { text-decoration: underline; }
.divider { height: 1px; background: var(--border); margin: 14px 0; }
.note { color: var(--muted); line-height: 1.7; font-size: 16px; }

.map-wrap {
  border: 1px solid var(--border); border-radius: 14px;
  overflow: hidden; background: var(--surface-2);
}
.map { width: 100%; height: 360px; }
.map :deep(.leaflet-control-zoom) { z-index: 1 !important; }
.map :deep(.leaflet-top) { z-index: 1 !important; }

.sister { margin-top: 14px; padding: 16px 16px 14px; }
.sister-head { padding: 2px 6px 10px; }
.sister-title { font-weight: 900; font-size: 20px; color: var(--text); }
.sister-sub { margin-top: 6px; font-size: 15px; color: var(--muted); }
.proj-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; padding: 6px; }
@media (max-width: 1200px) { .proj-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 980px) { .contact-grid { grid-template-columns: 1fr; } .proj-grid { grid-template-columns: 1fr; } }
@media (max-width: 480px) { .contact-info { padding: 12px; } .map-wrap { height: 260px; } .map { height: 260px; } }
.proj-card {
  display: flex; flex-direction: column; min-height: 132px; height: 100%;
  padding: 14px 14px 12px; border-radius: 16px;
  border: 1px solid var(--border); background: var(--surface-2);
  transition: transform .12s ease, box-shadow .12s ease;
  text-decoration: none; color: inherit;
}
.proj-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-hover); }
.proj-name { font-weight: 900; font-size: 16px; }
.proj-desc {
  margin-top: 6px; color: var(--muted); font-size: 14px; line-height: 1.6;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden;
}
.proj-meta { margin-top: auto; padding-top: 10px; font-size: 13px; font-weight: 700; color: var(--meta-text); display: block; }
.bottom-pad { height: 10px; }
</style>

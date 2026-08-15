<template>
  <div class="help-shell">
    <aside class="help-sidebar">
      <div class="help-toc-title">Contents</div>
      <nav class="help-toc" v-if="tocTree.length">
        <div v-for="section in tocTree" :key="section.id" class="toc-section">
          <div class="toc-section-row" :class="{ active: activeId === section.id, 'parent-active': isParentActive(section) }">
            <button v-if="section.children.length" class="toc-toggle" type="button" @click.stop="toggleSection(section.id)">
              <span class="toc-toggle-icon" :class="{ expanded: expanded.has(section.id) }">›</span>
            </button>
            <span v-else class="toc-toggle-placeholder"></span>
            <a class="toc-section-link" :href="'#' + section.id" @click.prevent="scrollTo(section.id)">{{ section.text }}</a>
          </div>
          <div v-if="section.children.length" v-show="expanded.has(section.id)" class="toc-children">
            <a v-for="child in section.children" :key="child.id" class="toc-child-link" :class="{ active: activeId === child.id }" :href="'#' + child.id" @click.prevent="scrollTo(child.id)">{{ child.text }}</a>
          </div>
        </div>
      </nav>
    </aside>

    <article class="help-content">
      <div v-if="loading" class="help-loading">Loading help documentation...</div>
      <div v-else-if="error" class="help-error">{{ error }}</div>
      <div v-else v-html="renderedHtml"></div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

const loading = ref(true);
const error = ref("");
const renderedHtml = ref("");
const activeId = ref("");
const expanded = ref(new Set<string>());
const helpAssetVersion = Date.now().toString(36);

interface TocChild { id: string; text: string }
interface TocSection { id: string; text: string; children: TocChild[] }
const tocTree = ref<TocSection[]>([]);

function isParentActive(s: TocSection) {
  return activeId.value === s.id || s.children.some(c => c.id === activeId.value);
}

function parseTocAndRender(md: string): string {
  // Remove TOC heading if present
  md = md.replace(/^## Table of Contents\n\n[\s\S]*?\n(?=## )/m, '');

  const tree: TocSection[] = [];
  let cur: TocSection | null = null;

  function slug(t: string) { return t.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''); }

  // Single pass: process headings in order (h2 first, then h3 under it)
  let html = md.replace(/^(#{1,3}) (.+)$/gm, (_: string, hashes: string, text: string) => {
    const level = hashes.length;
    if (level === 1) return `<h1>${text}</h1>`; // skip h1 from TOC
    const id = slug(text);
    if (level === 2) {
      cur = { id, text, children: [] };
      tree.push(cur);
      return `<h2 id="${id}">${text}</h2>`;
    }
    if (level === 3 && cur) {
      cur.children.push({ id, text });
      return `<h3 id="${id}">${text}</h3>`;
    }
    return `<h${level}>${text}</h${level}>`;
  });

  tocTree.value = tree;
  if (tree.length) expanded.value = new Set(tree.map(s => s.id));

  // Rest of markdown rendering (non-heading elements)
  html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em>$1</em>');
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
  html = html.replace(/^---$/gm, '<hr>');
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_m, alt, src) => {
    const fixedSrc = src.replace(/^\.\.\//, import.meta.env.BASE_URL);
    const separator = fixedSrc.includes("?") ? "&" : "?";
    return `<img src="${fixedSrc}${separator}v=${helpAssetVersion}" alt="${alt}" class="help-img">`;
  });
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');
  html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>');
  // Markdown table → proper <table> with thead/tbody
  html = html.replace(/((?:^\|.+\|$\n?)+)/gm, (tableBlock: string) => {
    const lines = tableBlock.trim().split('\n');
    if (lines.length < 2) return tableBlock;
    const headerLine = lines[0]!;
    const sepLine = lines[1]!;
    const dataLines = lines.slice(2);
    // Only treat as table if second line is a separator row
    if (!/^\|[\s\-:|]+\|$/.test(sepLine)) return tableBlock;
    const cellTag = (line: string, tag: string) => {
      return '<tr>' + line.split('|').filter(c => c.trim()).map(c => `<${tag}>${c.trim()}</${tag}>`).join('') + '</tr>';
    };
    const thead = cellTag(headerLine, 'th');
    const tbody = dataLines.map(l => cellTag(l, 'td')).join('');
    return `<div class="help-table-wrap"><table><thead>${thead}</thead><tbody>${tbody}</tbody></table></div>`;
  });
  const blocks = html.split(/\n\n+/);
  return blocks.map(b => {
    const t = b.trim();
    if (!t) return '';
    if (/^<(h[1-6]|pre|div|table|thead|tr|hr|blockquote|ul|ol|img)/.test(t)) return t;
    if (/^\d+\.\s/.test(t)) {
      const items = t.split('\n').filter(l => /^\d+\.\s/.test(l)).map(l => `<li>${l.replace(/^\d+\.\s/, '')}</li>`).join('');
      return `<ol>${items}</ol>`;
    }
    if (/^[-*]\s/.test(t)) {
      const items = t.split('\n').filter(l => /^[-*]\s/.test(l)).map(l => `<li>${l.replace(/^[-*]\s/, '')}</li>`).join('');
      return `<ul>${items}</ul>`;
    }
    return `<p>${t.replace(/\n/g, '<br>')}</p>`;
  }).join('\n');
}

function toggleSection(id: string) {
  const s = new Set(expanded.value);
  s.has(id) ? s.delete(id) : s.add(id);
  expanded.value = s;
}

function scrollTo(id: string) {
  activeId.value = id;
  for (const p of tocTree.value) {
    if (p.children.some(c => c.id === id)) {
      const s = new Set(expanded.value); s.add(p.id); expanded.value = s;
    }
  }
  const el = document.getElementById(id);
  if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
}

function onScroll() {
  let best = "";
  for (const p of tocTree.value) {
    for (const child of [p, ...p.children]) {
      const el = document.getElementById(child.id);
      if (el && el.getBoundingClientRect().top <= 120) best = child.id;
    }
  }
  if (best) activeId.value = best;
}

let timer: any = null;
let helpUnmounted = false;
function onWindowScroll() {
  clearTimeout(timer);
  timer = setTimeout(onScroll, 80);
}

onMounted(async () => {
  try {
    const res = await fetch(
      `${import.meta.env.BASE_URL}help-tutorial.md?v=${helpAssetVersion}`,
      { cache: "no-store" },
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const md = await res.text();
    if (helpUnmounted) return;
    renderedHtml.value = parseTocAndRender(md);
    window.addEventListener("scroll", onWindowScroll, { passive: true });
  } catch (e: any) {
    error.value = "Failed to load documentation. " + (e.message || "");
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  helpUnmounted = true;
  window.removeEventListener("scroll", onWindowScroll);
  clearTimeout(timer);
});
</script>

<style scoped>
/* === LAYOUT === */
.help-shell { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 0; width: 100%; max-width: none; margin: 0; min-height: 100vh; background: var(--bg); }
.help-sidebar { position: sticky; top: 64px; align-self: start; height: calc(100vh - 64px); overflow-y: auto; padding: 20px 16px 32px 28px; border-right: 1px solid rgba(143,165,156,0.2); background: var(--surface); }
.help-toc-title { font-size: 11px; font-weight: 900; color: var(--muted); text-transform: uppercase; letter-spacing: .6px; margin-bottom: 12px; }
.help-toc { display: flex; flex-direction: column; gap: 1px; }
.help-content { min-width: 0; max-width: none; margin: 0; padding: 28px 48px 80px 48px; font-size: 15px; line-height: 1.75; color: #2c3e3a; }

/* === TOC SECTIONS === */
.toc-section { }
.toc-section-row { display: flex; align-items: flex-start; gap: 4px; border-radius: 8px; padding: 5px 6px; margin-bottom: 1px; transition: background .12s; }
.toc-section-row:hover { background: rgba(143,165,156,.06); }
.toc-section-row.active { background: rgba(143,165,156,.12); }
.toc-section-row.parent-active { background: rgba(143,165,156,.08); }
.toc-toggle, .toc-toggle-placeholder { width: 16px; height: 20px; flex: 0 0 16px; display: flex; align-items: center; justify-content: center; border: none; background: none; padding: 0; color: var(--muted); cursor: pointer; font-size: 14px; }
.toc-toggle-icon { display: inline-block; transition: transform .15s; line-height: 1; }
.toc-toggle-icon.expanded { transform: rotate(90deg); }
.toc-section-link { flex: 1; font-size: 14px; font-weight: 750; line-height: 1.35; color: var(--text); text-decoration: none; overflow-wrap: anywhere; word-break: break-word; }
.toc-section-link:hover { color: var(--brand-primary-3); }
.toc-section-link.active { font-weight: 800; color: var(--brand-primary-3); }

/* === TOC CHILDREN === */
.toc-children { margin: 1px 0 6px 20px; padding-left: 8px; border-left: 1.5px solid rgba(143,165,156,.22); display: flex; flex-direction: column; gap: 1px; }
.toc-child-link { display: block; padding: 4px 8px; border-radius: 6px; font-size: 13px; font-weight: 600; line-height: 1.35; color: var(--muted); text-decoration: none; overflow-wrap: anywhere; word-break: break-word; transition: all .12s; }
.toc-child-link:hover { color: var(--brand-primary-3); background: rgba(143,165,156,.05); }
.toc-child-link.active { background: rgba(143,165,156,.12); color: #2c4a37; font-weight: 700; }

/* === CONTENT STYLES === */
.help-content :deep(h1) { font-size: 28px; font-weight: 950; margin: 0 0 6px; color: #1a2623; scroll-margin-top: 90px; }
.help-content :deep(h2) { font-size: 22px; font-weight: 850; margin: 36px 0 14px; padding-bottom: 7px; border-bottom: 2px solid rgba(143,165,156,.25); color: #1a2623; scroll-margin-top: 90px; }
.help-content :deep(h3) { font-size: 17px; font-weight: 800; margin: 24px 0 10px; color: #2c3e3a; scroll-margin-top: 90px; }
.help-content :deep(p) { margin: 10px 0; }
.help-content :deep(code) { background: rgba(143,165,156,.1); padding: 2px 6px; border-radius: 4px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 13px; }
.help-content :deep(pre) { background: #f6f8f7; border: 1px solid rgba(143,165,156,.2); border-radius: 8px; padding: 16px; overflow-x: auto; margin: 14px 0; }
.help-content :deep(pre code) { background: none; padding: 0; }
.help-content :deep(blockquote) { border-left: 3px solid #8FA59C; padding: 10px 14px; margin: 14px 0; background: rgba(143,165,156,.04); border-radius: 0 8px 8px 0; color: #4a6b5c; }
.help-content :deep(.help-table-wrap) { margin: 18px 0; overflow-x: auto; }
.help-content :deep(table) { width: 100%; border-collapse: collapse; font-size: 14px; border: 1px solid rgba(143,165,156,0.18); border-radius: 10px; overflow: hidden; }
.help-content :deep(thead) { background: rgba(143,165,156,0.10); }
.help-content :deep(th) { padding: 10px 14px; text-align: left; font-weight: 750; color: #1a2623; border-bottom: 2px solid rgba(143,165,156,0.22); font-size: 13px; text-transform: none; }
.help-content :deep(td) { padding: 9px 14px; border-bottom: 1px solid rgba(143,165,156,0.10); vertical-align: top; }
.help-content :deep(tbody tr:last-child td) { border-bottom: none; }
.help-content :deep(tbody tr:nth-child(even) td) { background: rgba(143,165,156,0.03); }
.help-content :deep(td code) { font-size: 13px; white-space: nowrap; }
.help-content :deep(hr) { border: none; border-top: 1px solid rgba(143,165,156,.2); margin: 22px 0; }
.help-content :deep(a) { color: var(--brand-primary-3); text-decoration: underline; }
.help-content :deep(ol), .help-content :deep(ul) { padding-left: 24px; margin: 10px 0; }
.help-content :deep(li) { margin: 3px 0; }
.help-img-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 24px; margin: 14px 0; border: 2px dashed rgba(143,165,156,.35); border-radius: 12px; background: rgba(143,165,156,.03); text-align: center; }
.help-img-icon { font-size: 32px; }
.help-img-text { font-size: 13px; font-weight: 700; color: #5F7D70; }
.help-img-placeholder code { font-size: 11px; color: var(--muted); }
.help-table-wrap { overflow-x: auto; }
.help-content :deep(img.help-img) { display: block; width: 800px; max-width: 100%; border-radius: 10px; margin: 20px auto; border: 1px solid rgba(143,165,156,0.18); box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.help-loading, .help-error { text-align: center; padding: 80px 20px; color: var(--muted); font-size: 16px; }

@media (max-width: 860px) {
  .help-shell { grid-template-columns: 1fr; }
  .help-sidebar { position: static; height: auto; max-height: 300px; border-right: none; border-bottom: 1px solid rgba(143,165,156,.2); margin: 0 16px; padding: 14px; }
  .help-content { padding: 20px 18px 60px; }
}
</style>

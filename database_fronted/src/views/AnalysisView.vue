<template>
    <div class="analysis-page">
      <div class="container">
        <div class="page-title">Analysis</div>
  
        <div class="stack">
          <transition-group name="stack" tag="div" class="stack-inner">
            <section
              v-for="m in ordered"
              :key="m.id"
              class="panel float-card"
              :class="[
                m.id === activeId ? 'active' : 'collapsed',
                mounted ? 'mounted' : ''
              ]"
            >
              <button class="panel-head" type="button" @click="activate(m.id)">
                <div class="head-left">
                  <span class="icon" v-html="m.icon"></span>
                  <span class="head-title">{{ m.title }}</span>
                </div>
                <span class="chev" :class="{ up: m.id === activeId }">⌄</span>
              </button>
  
              <div class="panel-body">
                <component :is="m.component" v-bind="m.props" :active="m.id === activeId" />
              </div>
            </section>
          </transition-group>
        </div>
      </div>
    </div>
  </template>
  
  <script setup lang="ts">
  import { computed, nextTick, onMounted, ref } from "vue";

defineOptions({ name: "AnalysisView" });
  import { markRaw } from "vue";
  import CellTypeEnrichment from "@/components/analysis/CellTypeEnrichment.vue";
import SequencePeak2Gene from "@/components/analysis/SequencePeak2Gene.vue";
import AnalysisPeakGeneContextPanel from "@/components/analysis/AnalysisPeakGeneContextPanel.vue";
  
  type Module = {
    id: string;
    title: string;
    icon: string;
    component: any;
    props?: Record<string, any>;
  };
  
  /** ✅ Analysis：4 个模块 */
  const initialModules: Module[] = [
    {
      id: "cell_type_enrichment",
      title: "Cell type enrichment analysis",
      icon: "∑",
      component: markRaw(CellTypeEnrichment),
      props: {},
    },
    {
      id: "sequence_peak2gene",
      title: "Sequence-based peak regulatory analysis",
      icon: "🧬",
      component: markRaw(SequencePeak2Gene),
      props: {},
    },
    {
      id: "peak_gene_context",
      title: "Peak-to-Gene linkage analysis",
      icon: `<svg width="14" height="14" viewBox="0 0 16 16" fill="none" style="display:block;margin:auto"><circle cx="8" cy="8" r="2.5" stroke="currentColor" stroke-width="1.5"/><circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.2" stroke-dasharray="2,2"/><line x1="8" y1="2" x2="8" y2="5.5" stroke="currentColor" stroke-width="1.2"/><line x1="8" y1="10.5" x2="8" y2="14" stroke="currentColor" stroke-width="1.2"/><line x1="2" y1="8" x2="5.5" y2="8" stroke="currentColor" stroke-width="1.2"/><line x1="10.5" y1="8" x2="14" y2="8" stroke="currentColor" stroke-width="1.2"/></svg>`,
      component: markRaw(AnalysisPeakGeneContextPanel),
      props: {},
    },
  ];
  
  const modules = ref<Module[]>(initialModules);
  /** 默认展开第一个；并支持点击已展开模块收起（activeId=null） */
  const activeId = ref<string | null>(initialModules[0]?.id ?? "cell_type_enrichment");
  
  const mounted = ref(false);
  
  const ordered = computed(() => {
    if (!activeId.value) return modules.value;
    const active = modules.value.find((x) => x.id === activeId.value);
    const rest = modules.value.filter((x) => x.id !== activeId.value);
    return active ? [active, ...rest] : modules.value;
  });
  
  function activate(id: string) {
    activeId.value = activeId.value === id ? null : id;
  }
  
  onMounted(async () => {
    await nextTick();
    mounted.value = true;
  });
  </script>
  
  <style scoped>
  .analysis-page {
    width: 100%;
    padding: 18px 0 30px;
    background: var(--bg);
  }
  
  .page-title {
    font-size: 32px;
    font-weight: 900;
    margin: 4px 0 10px;
  }
  
  .stack {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  
  .stack-inner{
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
  
  .stack-move{
    transition: transform .28s cubic-bezier(.2,.8,.2,1);
  }
  
  /* panel card */
  .panel {
    overflow: hidden;
    transition:
      transform .22s cubic-bezier(.2,.8,.2,1),
      box-shadow .22s ease,
      border-color .22s ease;
    will-change: transform;
  }
  
  /* 头部：像条形按钮 */
  .panel-head {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
  
    padding: 12px 14px;
    cursor: pointer;
  
    background: var(--surface-2);
    border: none;
    color: var(--text);
  }
  
  .head-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  
  .icon {
    width: 22px;
    height: 22px;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: var(--surface-3);
    border: 1px solid var(--border);
    font-weight: 900;
  }
  
  .head-title {
    font-weight: 900;
    text-align: left;
  }
  
  .chev {
    transition: transform .18s ease;
    opacity: .7;
  }
  
  .chev.up {
    transform: rotate(180deg);
  }
  
  /* active 的卡片更“上浮”一点 */
  .panel.active {
    border: 1px solid var(--border-brand);
    box-shadow: var(--shadow-hover);
  }
  
  /* collapsed 是小条：只显示 header */
  .panel.collapsed .panel-head {
    background: var(--surface);
  }
  
  .panel.collapsed {
    border: 1px solid var(--border);
    box-shadow: var(--shadow-card);
  }
  
  /* body */
  .panel-body {
    overflow: hidden;
    max-height: 0;
    opacity: 0;
    padding: 0 14px;
    border-top: 1px solid var(--border);
    background: var(--surface);
    transition:
      max-height .32s cubic-bezier(.2, .8, .2, 1),
      opacity .18s ease,
      padding .22s ease;
    pointer-events: none;
  }
  
  .panel.active .panel-body{
    max-height: 4000px;
    opacity: 1;
    padding: 14px 16px 18px;
    pointer-events: auto;
  }
  
  /* 入场动画：默认第一个展开更自然 */
  .panel.mounted.active {
    animation: popIn .28s ease both;
  }
  
  @keyframes popIn {
    from {
      transform: translateY(8px);
      opacity: .95;
    }
    to {
      transform: translateY(0);
      opacity: 1;
    }
  }
  
  /* placeholder */
  .ph-h {
    font-weight: 900;
    margin-bottom: 8px;
  }
  
  .ph-p {
    color: var(--muted);
    line-height: 1.6;
    margin-bottom: 12px;
  }
  
  .ph-box {
    height: 260px;
    border-radius: 14px;
    border: 1px dashed var(--border-brand);
    background: var(--surface-2);
  }

@media (max-width: 768px) {
  .analysis-layout { grid-template-columns: 1fr; }
  .analysis-tabs { flex-wrap: wrap; gap: 8px; }
}
@media (max-width: 480px) {
  .analysis-card { padding: 12px; }
  .analysis-header { flex-direction: column; gap: 10px; align-items: flex-start; }
}
  </style>

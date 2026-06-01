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
                  <span class="icon">∑</span>
                  <span class="head-title">{{ m.title }}</span>
                </div>
                <span class="chev" :class="{ up: m.id === activeId }">⌄</span>
              </button>
  
              <div class="panel-body">
                <component :is="m.component" v-bind="m.props" />
              </div>
            </section>
          </transition-group>
        </div>
      </div>
    </div>
  </template>
  
  <script setup lang="ts">
  import { computed, defineComponent, h, nextTick, onMounted, ref } from "vue";
  
  /** 占位组件：后面把每个分析模块的表单/结果替换进来 */
  const Placeholder = defineComponent({
    name: "AnalysisPlaceholder",
    props: {
      title: { type: String, required: true },
    },
    setup(props) {
      return () =>
        h("div", { class: "ph" }, [
          h("div", { class: "ph-h" }, `Module: ${props.title}`),
          h(
            "div",
            { class: "ph-p" },
            "这里放该分析模块的参数输入与结果展示。现在先占位，验证动画与布局逻辑。"
          ),
          h("div", { class: "ph-box" }),
        ]);
    },
  });
  
  type Module = {
    id: string;
    title: string;
    component: any;
    props?: Record<string, any>;
  };
  
  /** ✅ Analysis：4 个模块 */
  const initialModules: Module[] = [
    {
      id: "diff_overlap",
      title: "Differential-overlapping chromatin accessible regions",
      component: Placeholder,
      props: { title: "Differential-overlapping CARs" },
    },
    {
      id: "two_tf_overlap",
      title: "Overlapping chromatin accessible regions co-bound by two TFs",
      component: Placeholder,
      props: { title: "Two-TF co-binding overlap" },
    },
    {
      id: "region_enrichment",
      title: "Genomic region enrichment analysis",
      component: Placeholder,
      props: { title: "Genomic region enrichment" },
    },
    {
      id: "gene_car_overlap",
      title: "Gene–CAR overlapping analysis",
      component: Placeholder,
      props: { title: "Gene–CAR overlap" },
    },
  ];
  
  const modules = ref<Module[]>(initialModules);
  /** 默认展开第一个；并支持点击已展开模块收起（activeId=null） */
  const activeId = ref<string | null>(initialModules[0]?.id ?? "diff_overlap");
  
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
    margin: 6px 0 14px;
  }
  
  .stack {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  .stack-inner{
    display: flex;
    flex-direction: column;
    gap: 12px;
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
    max-height: 2000px;
    opacity: 1;
    padding: 14px 14px 16px;
    pointer-events: auto;
  
    /* ✅ 保持你原来的“大功能区”高度逻辑 */
    min-height: calc(100vh - 72px - 24px - 350px);
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
  </style>
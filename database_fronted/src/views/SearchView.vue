<template>
    <div class="search-page">
        <div class="container">
            <div class="page-title">Search</div>

            <div class="stack">
                <transition-group name="stack" tag="div" class="stack-inner">
                    <section v-for="m in ordered" :key="m.id" class="panel float-card" :class="[
                        m.id === activeId ? 'active' : 'collapsed',
                        mounted ? 'mounted' : ''
                    ]">
                        <button class="panel-head" type="button" @click="activate(m.id)">
                            <div class="head-left">
                                <span class="icon">⌕</span>
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

/** 占位组件：后面你把每个模块真正表单替换进来 */
const Placeholder = defineComponent({
    name: "SearchPlaceholder",
    props: {
        title: { type: String, required: true },
    },
    setup(props) {
        return () =>
            h("div", { class: "ph" }, [
                h("div", { class: "ph-h" }, `Module: ${props.title}`),
                h("div", { class: "ph-p" }, "这里放该模块的表单与结果。现在先占位，验证动画与布局逻辑。"),
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

const initialModules: Module[] = [
    { id: "region", title: "Search chromatin accessible regions by genome region", component: Placeholder, props: { title: "Genome region" } },
    { id: "tissue", title: "Search chromatin accessible regions by tissue type", component: Placeholder, props: { title: "Tissue type" } },
    { id: "tf", title: "Search chromatin accessible regions by TF", component: Placeholder, props: { title: "TF" } },
    { id: "gene", title: "Search chromatin accessible regions by gene", component: Placeholder, props: { title: "Gene" } },
    { id: "snp", title: "Search chromatin accessible regions by SNP", component: Placeholder, props: { title: "SNP" } },
];

const modules = ref<Module[]>(initialModules);
    const activeId = ref<string | null>(initialModules[0]?.id ?? "region");

const mounted = ref(false);

const ordered = computed(() => {
    if (!activeId.value) return modules.value;
    const active = modules.value.find((x) => x.id === activeId.value);
    const rest = modules.value.filter((x) => x.id !== activeId.value);
    return active ? [active, ...rest] : modules.value;
});

function activate(id: string) {
    activeId.value = (activeId.value === id) ? null : id;
}

onMounted(async () => {
    // 默认打开第一个，并在首帧后触发动画
    await nextTick();
    mounted.value = true;
});
</script>

<style scoped>
.search-page {
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
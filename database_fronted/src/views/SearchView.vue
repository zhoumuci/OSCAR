<template>
    <div class="search-page">
        <div class="container">
            <div class="page-title">Search</div>

            <div class="stack">
                <transition-group name="stack" tag="div" class="stack-inner">
                    <section v-for="m in ordered" :key="m.id" class="panel float-card" :class="[
                        m.id === activeId ? 'active' : 'collapsed',
                        pageMounted ? 'mounted' : ''
                    ]">
                        <button class="panel-head" type="button" @click="activate(m.id)">
                            <div class="head-left">
                                <span class="icon" v-html="m.icon"></span>
                                <span class="head-title">{{ m.title }}</span>
                            </div>
                            <span class="chev" :class="{ up: m.id === activeId }">⌄</span>
                        </button>

                        <div class="panel-body" :aria-hidden="m.id !== activeId">
                            <Suspense v-if="mountedModuleIds.has(m.id)">
                                <component :is="m.component" v-bind="m.props" :active="m.id === activeId" />
                                <template #fallback>
                                    <div class="module-loading" role="status">Loading {{ m.title }}…</div>
                                </template>
                            </Suspense>
                        </div>
                    </section>
                </transition-group>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, markRaw, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from "vue";

defineOptions({ name: "SearchView" });

import GeneSearchCard from "@/components/search/GeneSearchCard.vue";

const loadPeakSearchCard = () => import("@/components/search/PeakSearchCard.vue");
const loadTissueSearchCard = () => import("@/components/search/TissueSearchCard.vue");
const loadCellTypeSearchCard = () => import("@/components/search/CellTypeSearchCard.vue");

const PeakSearchCard = markRaw(defineAsyncComponent(() => loadPeakSearchCard().then((module) => module.default)));
const TissueSearchCard = markRaw(defineAsyncComponent(() => loadTissueSearchCard().then((module) => module.default)));
const CellTypeSearchCard = markRaw(defineAsyncComponent(() => loadCellTypeSearchCard().then((module) => module.default)));

type Module = { id: string; title: string; icon: string; component: any; props?: Record<string, unknown> };

const initialModules: Module[] = [
    { id: "gene", title: "Search associated samples by gene", icon: "⌕", component: GeneSearchCard },
    { id: "peak", title: "Search associated samples by genomic region", icon: "⌕", component: PeakSearchCard },
    { id: "tissue", title: "Search associated samples by tissue type", icon: "⌕", component: TissueSearchCard },
    { id: "celltype", title: "Search associated samples by cell type", icon: "⌕", component: CellTypeSearchCard },
];

const modules = shallowRef<Module[]>(initialModules);
const activeId = ref<string | null>(initialModules[0]?.id ?? "gene");
const mountedModuleIds = ref(new Set<string>([initialModules[0]?.id ?? "gene"]));
const pageMounted = ref(false);

const ordered = computed(() => {
    if (!activeId.value) return modules.value;
    const active = modules.value.find((x) => x.id === activeId.value);
    const rest = modules.value.filter((x) => x.id !== activeId.value);
    return active ? [active, ...rest] : modules.value;
});

function activate(id: string) {
    if (activeId.value !== id && !mountedModuleIds.value.has(id)) {
        mountedModuleIds.value = new Set([...mountedModuleIds.value, id]);
    }
    activeId.value = (activeId.value === id) ? null : id;
}

type IdleWindow = Window & {
    requestIdleCallback?: (callback: () => void, options?: { timeout?: number }) => number;
    cancelIdleCallback?: (handle: number) => void;
};

let preloadIdleHandle: number | null = null;
let preloadTimer: number | null = null;
let searchViewDisposed = false;

async function preloadDeferredModules() {
    const deferredModules = [
        { id: "peak", loader: loadPeakSearchCard },
        { id: "tissue", loader: loadTissueSearchCard },
        { id: "celltype", loader: loadCellTypeSearchCard },
    ];
    for (const deferred of deferredModules) {
        if (searchViewDisposed) return;
        try {
            await deferred.loader();
            if (searchViewDisposed) return;
            if (!mountedModuleIds.value.has(deferred.id)) {
                mountedModuleIds.value = new Set([...mountedModuleIds.value, deferred.id]);
                await nextTick();
            }
        } catch (error) {
            console.warn("Failed to preload a search module:", error);
        }
        await new Promise<void>((resolve) => window.setTimeout(resolve, 0));
    }
}

function scheduleDeferredModulePreload() {
    preloadTimer = window.setTimeout(() => {
        preloadTimer = null;
        if (searchViewDisposed) return;
        const idleWindow = window as IdleWindow;
        if (idleWindow.requestIdleCallback) {
            preloadIdleHandle = idleWindow.requestIdleCallback(() => {
                preloadIdleHandle = null;
                void preloadDeferredModules();
            }, { timeout: 1600 });
            return;
        }
        void preloadDeferredModules();
    }, 420);
}

onMounted(async () => {
    await nextTick();
    pageMounted.value = true;
    scheduleDeferredModulePreload();
});

onBeforeUnmount(() => {
    searchViewDisposed = true;
    const idleWindow = window as IdleWindow;
    if (preloadIdleHandle !== null) idleWindow.cancelIdleCallback?.(preloadIdleHandle);
    if (preloadTimer !== null) window.clearTimeout(preloadTimer);
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

.module-loading {
    min-height: 96px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--muted);
    font-size: 13px;
    font-weight: 800;
}

.stack-inner{
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stack-move {
  transition: transform .28s cubic-bezier(.2, .8, .2, 1);
}

/* panel card */
.panel {
    overflow: hidden;
    transition:
        transform .22s cubic-bezier(.2, .8, .2, 1),
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

.panel.active .panel-body {
    max-height: 4000px;
    opacity: 1;
    padding: 14px 14px 16px;
    pointer-events: auto;
}

/* Keep the original first-open motion without delaying first-content loading. */
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

@media (max-width: 768px) {
    .search-page,
    .stack,
    .stack-inner,
    .panel,
    .panel-body,
    .panel-head {
        max-width: 100%;
        min-width: 0;
    }

    .search-page {
        overflow-x: clip;
        padding: 14px 0 24px;
    }

    .page-title {
        font-size: 28px;
    }

    .panel-head {
        gap: 10px;
    }

    .head-left {
        min-width: 0;
    }

    .head-title {
        min-width: 0;
        overflow-wrap: anywhere;
    }

    .chev,
    .icon {
        flex: 0 0 auto;
    }

    .panel.active .panel-body {
        padding: 12px;
    }
}

</style>

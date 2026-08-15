<template>
  <div class="facet float-card">
    <div class="facet-head">{{ title }}</div>

    <!-- search (items > 12) -->
    <div v-if="items.length > 12" class="facet-search">
      <el-input
        v-model="searchText"
        :placeholder="`Search ${title.toLowerCase()}…`"
        size="small"
        clearable
        @input="onSearchInput"
      />
    </div>

    <div class="facet-body">
      <button
        v-for="item in pagedItems"
        :key="item.label"
        class="facet-item"
        :class="{ active: selected === item.label }"
        type="button"
        :title="item.label"
        @click="$emit('select', item.label)"
      >
        <span class="label">{{ item.label }}</span>
        <span class="count">{{ item.count.toLocaleString() }}</span>
      </button>

      <div v-if="filteredItems.length === 0 && !searchText" class="facet-empty">No options</div>
      <div v-else-if="filteredItems.length === 0 && searchText" class="facet-empty">No match</div>
    </div>

    <!-- pagination (items > 8) -->
    <div v-if="totalPages > 1" class="facet-pager">
      <button
        type="button"
        class="pager-btn"
        :disabled="page === 1"
        @click="page = 1"
      >«</button>
      <button
        type="button"
        class="pager-btn"
        :disabled="page === 1"
        @click="page = page - 1"
      >‹</button>

      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '…'" class="pager-ellipsis">…</span>
        <button
          v-else
          type="button"
          class="pager-btn pager-num"
          :class="{ active: p === page }"
          @click="page = (p as number)"
        >{{ p }}</button>
      </template>

      <button
        type="button"
        class="pager-btn"
        :disabled="page === totalPages"
        @click="page = page + 1"
      >›</button>
      <button
        type="button"
        class="pager-btn"
        :disabled="page === totalPages"
        @click="page = totalPages"
      >»</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { BrowseFacetItem } from "@/api/databrowse";

const PAGE_SIZE = 8;

const props = defineProps<{
  title: string;
  items: BrowseFacetItem[];
  selected?: string;
}>();

defineEmits<{
  select: [label: string];
}>();

const searchText = ref("");
const page = ref(1);

// reset page when items or search change
watch(() => [props.items, searchText.value], () => { page.value = 1; });

const filteredItems = computed(() => {
  const q = searchText.value.trim().toLowerCase();
  if (!q) return props.items;
  return props.items.filter((item) => item.label.toLowerCase().includes(q));
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)));

const pagedItems = computed(() => {
  if (props.items.length <= 8) return filteredItems.value; // no pagination
  const start = (page.value - 1) * PAGE_SIZE;
  return filteredItems.value.slice(start, start + PAGE_SIZE);
});

const visiblePages = computed(() => {
  const tp = totalPages.value;
  const p = page.value;
  if (tp <= 7) return Array.from({ length: tp }, (_, i) => i + 1);

  const pages: (number | "…")[] = [];
  pages.push(1);
  if (p > 3) pages.push("…");

  const start = Math.max(2, p - 1);
  const end = Math.min(tp - 1, p + 1);
  for (let i = start; i <= end; i++) pages.push(i);

  if (p < tp - 2) pages.push("…");
  pages.push(tp);
  return pages;
});

function onSearchInput() {
  page.value = 1;
}
</script>

<style scoped>
.facet {
  overflow: hidden;
  flex: 0 0 auto;
  background: #fff;
  border: 1px solid rgba(80, 100, 90, 0.16);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(31, 48, 42, 0.06);
  margin-bottom: 20px;
  transition: box-shadow 0.18s ease;
}

.facet:last-child {
  margin-bottom: 0;
}

.facet:hover {
  box-shadow: 0 4px 12px rgba(31, 48, 42, 0.10);
}

.facet-head {
  padding: 12px 14px;
  font-weight: 900;
  font-size: 15px;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  border-bottom: 1px solid var(--nav-active-border);
}

.facet-search {
  padding: 8px 10px 0;
}

.facet-search :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(80, 100, 90, 0.16) inset;
}

.facet-body {
  padding: 6px 8px;
}

.facet-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  color: var(--text);
  text-align: left;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  transition: background 0.12s ease;
}

.facet-item:hover {
  background: var(--surface-3);
}

.facet-item.active {
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
}

.facet-item.active .count {
  color: var(--nav-active-text);
}

.label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.count {
  flex: 0 0 auto;
  font-weight: 800;
  color: var(--muted);
  font-size: 12px;
}

.facet-empty {
  padding: 10px 8px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

/* ── pagination ─────────────────────────────── */
.facet-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 6px 8px 10px;
  border-top: 1px solid rgba(80, 100, 90, 0.12);
}

.pager-btn {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.12s ease, color 0.12s ease;
}

.pager-btn:hover:not(:disabled) {
  background: var(--surface-3);
  color: var(--text);
}

.pager-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.pager-num.active {
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  font-weight: 900;
}

.pager-ellipsis {
  width: 26px;
  text-align: center;
  color: var(--muted);
  font-size: 12px;
}
</style>

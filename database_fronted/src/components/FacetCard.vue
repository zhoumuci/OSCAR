<template>
  <div class="facet float-card">
    <div class="facet-head">{{ title }}</div>

    <div class="facet-body">
      <button
        v-for="item in items"
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

      <div v-if="items.length === 0" class="facet-empty">No options</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BrowseFacetItem } from "@/api/databrowse";

defineProps<{
  title: string;
  items: BrowseFacetItem[];
  selected?: string;
}>();

defineEmits<{
  select: [label: string];
}>();
</script>

<style scoped>
.facet {
  overflow: hidden;
  flex: 0 0 auto;
}

.facet-head {
  padding: 10px 12px;
  font-weight: 900;
  background: var(--nav-active-bg);
  color: var(--nav-active-text);
  border-bottom: 1px solid var(--nav-active-border);
}

.facet-body {
  padding: 8px 10px;
}

.facet-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  color: var(--text);
  text-align: left;
  font: inherit;
}

.facet-item:hover {
  background: var(--surface-3);
}

.facet-item.active {
  background: var(--surface-3);
  outline: 1px solid var(--border-brand);
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
}

.facet-empty {
  padding: 8px;
  color: var(--muted);
  font-size: 13px;
}
</style>

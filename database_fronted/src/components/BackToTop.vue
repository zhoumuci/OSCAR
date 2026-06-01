<template>
    <button
      class="btt"
      :class="{ show: visible }"
      type="button"
      title="Back to top"
      @click="scrollToTop"
    >
      ↑
    </button>
  </template>
  
  <script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref } from "vue";
  
  const visible = ref(false);
  
  const onScroll = () => {
    visible.value = window.scrollY > 400; // 滚动超过 400px 出现
  };
  
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  
  onMounted(() => {
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  });
  
  onBeforeUnmount(() => {
    window.removeEventListener("scroll", onScroll);
  });
  </script>
  
  <style scoped>
  .btt{
    position: fixed;
    right: 22px;
    bottom: 22px;
    width: 44px;
    height: 44px;
    border-radius: 999px;
  
    border: 1px solid var(--border);
    background: var(--surface);
    color: var(--text);
  
    box-shadow: var(--shadow-card);
    cursor: pointer;
  
    opacity: 0;
    transform: translateY(8px);
    pointer-events: none;
    transition: opacity .18s ease, transform .18s ease;
  }
  
  .btt.show{
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }
  
  .btt:hover{
    border-color: var(--border-brand);
  }
  </style>
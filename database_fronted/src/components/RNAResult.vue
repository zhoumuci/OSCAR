<template>
    <div class="float-card block">
      <div class="block-title">RNA specific</div>
      <div class="muted">Placeholder: expression summary / DEG tables / gene links...</div>
    </div>
  </template>
  
  <script setup lang="ts">
  defineProps<{ payload: any }>();
  </script>
  
  <style scoped>
  .block{ padding: 14px; margin-bottom: 12px; }
  .block-title{ font-weight: 900; margin-bottom: 8px; }
  .muted{ color: var(--muted); }
  </style>
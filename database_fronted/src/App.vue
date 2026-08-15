<template>
  <MainLayout />
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import MainLayout from "@/layouts/MainLayout.vue";
import { reportVisit } from "@/api/home";

// SPA 每次加载 = 用户打开一次页面，算 1 次访问；
// 站内路由跳转不会重新挂载 App，因此不会再计。
onMounted(() => {
  reportVisit();
});
</script>
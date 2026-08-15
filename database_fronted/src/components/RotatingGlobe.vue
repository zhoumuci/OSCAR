<template>
  <div ref="el" class="globe"></div>
</template>

<script setup lang="ts">
const baseUrl = import.meta.env.BASE_URL;
import { onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";
import "echarts-gl";

const el = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

onMounted(() => {
  if (!el.value) return;
  chart = echarts.init(el.value);

  chart.setOption({
    globe: {
      baseTexture: baseUrl + 'images/earth.jpg',
      shading: "realistic",
      environment: "auto",
      realisticMaterial: {
        roughness: 0.8,
        metalness: 0.1,
      },
      atmosphere: {
        show: true,
        offset: 6,
      },
      light: {
        ambient: { intensity: 0.6 },
        main: { intensity: 0.8, alpha: 30, beta: 20 },
      },
      viewControl: {
        autoRotate: true,
        autoRotateSpeed: 1.2,
        targetCoord: [116.4, 39.9], // 北京
      },
      silent: true,
    },
  });
});

onBeforeUnmount(() => {
  chart?.dispose();
  chart = null;
});
</script>

<style scoped>
.globe {
  width: 160px;
  height: 160px;
}
</style>

<template>
  <div ref="el" class="map"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import world110m from "world-atlas/countries-110m.json";
import { feature } from "topojson-client";

type Point = { name?: string; lat: number; lon: number; value?: number };

const props = defineProps<{ points: Point[] }>();

const el = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

/** 读取 CSS 变量，拿到真正的颜色字符串 */
function cssVar(name: string, fallback: string) {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return v || fallback;
}

function registerWorldIfNeeded() {
  const gj = feature(world110m as any, (world110m as any).objects.countries) as any;
  echarts.registerMap("WORLD", gj);
}

function render() {
  if (!el.value) return;
  if (!chart) chart = echarts.init(el.value);

  registerWorldIfNeeded();

  const data = props.points.map((p) => ({
    name: p.name ?? "",
    value: [p.lon, p.lat, p.value ?? 1],
  }));

  // ✅ 从 theme token 读取颜色（你可以在 theme.css 里控制它们）
  const mapBg = cssVar("--map-bg", cssVar("--surface-2", "#F1F6F4"));
  const land = cssVar("--map-land", cssVar("--surface", "#FFFFFF"));
  const border = cssVar("--map-border", cssVar("--border", "rgba(0,0,0,0.10)"));
  const landHover = cssVar("--map-land-hover", cssVar("--surface-3", "#E6EFEB"));
  const dot = cssVar("--map-dot", cssVar("--brand-primary-3", "#5F7D70"));
  const ripple = cssVar("--map-ripple", cssVar("--brand-primary", "#819D8E"));

  chart.setOption(
    {
      backgroundColor: mapBg, // ✅ 直接让 ECharts 画布背景 = 右侧背景
      tooltip: {
        trigger: "item",
        formatter: (p: any) => p.name || "visitor",
      },
      geo: {
        map: "WORLD",
        roam: true,
        silent: true,
        itemStyle: {
          areaColor: land,      // ✅ 陆地白/卡片白
          borderColor: border,  // ✅ 国界线用 token
          borderWidth: 0.8,     // ✅ 别太粗，粗了就像你截图那样“黑线横飞”
        },
        emphasis: {
          itemStyle: {
            areaColor: landHover,
          },
        },
      },
      series: [
        {
          name: "Visitors",
          type: "effectScatter",
          coordinateSystem: "geo",
          data,
          symbolSize: (val: any) => 6 + Math.min(10, (val[2] || 1) * 2),
          itemStyle: { color: dot },
          rippleEffect: {
            brushType: "stroke",
            scale: 3,
            period: 4,
            color: ripple,
          },
          emphasis: { scale: 1.1 },
        },
      ],
    },
    true
  );
}

let ro: ResizeObserver | null = null;

onMounted(() => {
  render();
  ro = new ResizeObserver(() => chart?.resize());
  if (el.value) ro.observe(el.value);
});

onBeforeUnmount(() => {
  ro?.disconnect();
  chart?.dispose();
  chart = null;
});

watch(() => props.points, () => render(), { deep: true });
</script>

<style scoped>
.map{
  width: 100%;
  height: 380px;
  border-radius: 16px;

  /* ✅ 外层也用同样背景，保证边缘不露白 */
  background: var(--map-bg);
  border: 1px solid var(--border);
}
</style>
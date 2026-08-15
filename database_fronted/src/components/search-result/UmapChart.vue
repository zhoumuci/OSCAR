<template>
  <div class="chart-body">
    <el-skeleton v-if="loading" animated :rows="6" />
    <div v-else-if="error" class="chart-state">Failed to load chart data</div>
    <div v-else-if="!hasData" class="chart-state">No chart data available</div>
    <div v-else ref="chartEl" class="chart-canvas" />
  </div>
</template>

<script setup lang="ts">
import * as echarts from "echarts";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import type { SearchResultColorBy, SearchResultEmbedding, UmapData, UmapPoint } from "@/api/searchResult";
import { getBioChartColor, getBioChartColorMap } from "@/utils/chartPalette";
import { downloadChart, type ChartDownloadOptions } from "@/utils/downloadChart";

const props = defineProps<{
  data: UmapData | null;
  loading: boolean;
  error: boolean;
  embedding: SearchResultEmbedding;
  colorBy: SearchResultColorBy;
}>();

const chartEl = ref<HTMLElement | null>(null);
const hasData = computed(() => (props.data?.points?.length ?? 0) > 0);

let chart: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
let resizeTimer: number | undefined;

function scheduleResize() {
  window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => chart?.resize(), 80);
}

function ensureChart() {
  if (!chartEl.value) return null;

  if (!chart) {
    chart = echarts.init(chartEl.value);
    resizeObserver = new ResizeObserver(scheduleResize);
    resizeObserver.observe(chartEl.value);
  }

  return chart;
}

async function renderChart() {
  if (props.loading || props.error || !hasData.value) {
    disposeChart();
    return;
  }

  await nextTick();
  const currentChart = ensureChart();
  if (!currentChart || !props.data) return;

  const groupedPoints = groupPointsByLabel(props.data.points);
  const groupedEntries = Object.entries(groupedPoints).sort(([a], [b]) => a.localeCompare(b));
  const colorMap = getBioChartColorMap(groupedEntries.map(([label]) => label));
  const colors = groupedEntries.map(([label], index) => colorMap.get(label) ?? getBioChartColor(index));
  const useLargeMode = props.data.points.length > 3000;
  const embedding = props.data.embedding ?? props.embedding;
  const [xAxisName, yAxisName] = getAxisNames(embedding);
  const colorBy = props.data.colorBy ?? props.colorBy;

  currentChart.setOption(
    {
      color: colors,
      animation: !useLargeMode,
      tooltip: {
        trigger: "item",
        formatter: ({ data }: { data: [number, number, string, string] }) => {
          const [, , barcode, label] = data;
          return `${barcode}<br/>${colorBy}: ${label}`;
        },
      },
      grid: { top: 18, left: 44, right: 18, bottom: 42 },
      legend: {
        type: "scroll",
        bottom: 0,
        textStyle: { color: "#5E6C67" },
      },
      xAxis: {
        type: "value",
        name: xAxisName,
        scale: true,
        splitLine: { lineStyle: { color: "#1B2A2714" } },
      },
      yAxis: {
        type: "value",
        name: yAxisName,
        scale: true,
        splitLine: { lineStyle: { color: "#1B2A2714" } },
      },
      series: groupedEntries.map(([label, points], index) => {
        const color = colorMap.get(label) ?? getBioChartColor(index);

        return {
          name: label,
          type: "scatter",
          data: points.map((point) => [point.x, point.y, point.barcode, point.label]),
          symbolSize: useLargeMode ? 4 : 5,
          large: useLargeMode,
          largeThreshold: 3000,
          progressive: 2000,
          progressiveThreshold: 3000,
          itemStyle: { color, opacity: 0.84 },
          emphasis: { focus: "series" },
        };
      }),
    },
    true
  );
}

function getAxisNames(embedding: SearchResultEmbedding) {
  return embedding === "tsne" ? ["TSNE_1", "TSNE_2"] : ["UMAP_1", "UMAP_2"];
}

function groupPointsByLabel(points: UmapPoint[]): Record<string, UmapPoint[]> {
  return points.reduce<Record<string, UmapPoint[]>>((groups, point) => {
    const label = point.label || "Unlabeled";
    groups[label] = groups[label] ?? [];
    groups[label].push(point);
    return groups;
  }, {});
}

function disposeChart() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  window.clearTimeout(resizeTimer);
  chart?.dispose();
  chart = null;
}

function downloadImage(filename: string, options?: ChartDownloadOptions) {
  return downloadChart(chart, filename, options);
}

defineExpose({ downloadImage });

watch(() => [props.data, props.loading, props.error, props.embedding, props.colorBy], renderChart, { flush: "post" });
onMounted(renderChart);
onBeforeUnmount(disposeChart);
</script>

<style scoped>
.chart-body {
  height: 100%;
  min-height: 440px;
}

.chart-canvas {
  width: 100%;
  height: 100%;
  min-height: 440px;
}

.chart-state {
  height: 100%;
  min-height: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #1b2a2712;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff99 0%, var(--surface-2) 100%);
  color: var(--muted);
  font-weight: 700;
  text-align: center;
}

@media (max-width: 1280px) {
  .chart-body,
  .chart-canvas,
  .chart-state {
    min-height: 390px;
  }
}

@media (max-width: 760px) {
  .chart-body,
  .chart-canvas,
  .chart-state {
    min-height: 340px;
  }
}
</style>

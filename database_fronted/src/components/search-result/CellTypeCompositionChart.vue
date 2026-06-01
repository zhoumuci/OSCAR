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
import type { CellTypeCompositionData } from "@/api/searchResult";
import { getBioChartColor, getBioChartColorMap } from "@/utils/chartPalette";
import { downloadChart, type ChartDownloadOptions } from "@/utils/downloadChart";

const props = defineProps<{
  data: CellTypeCompositionData | null;
  loading: boolean;
  error: boolean;
}>();

const chartEl = ref<HTMLElement | null>(null);
const hasData = computed(() => (props.data?.items?.length ?? 0) > 0);

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

  const colorMap = getBioChartColorMap(props.data.items.map((item) => item.label));
  const colors = props.data.items.map((item, index) => colorMap.get(item.label) ?? getBioChartColor(index));

  currentChart.setOption(
    {
      color: colors,
      tooltip: {
        trigger: "item",
        formatter: ({ name, value, percent }: { name: string; value: number; percent: number }) =>
          `${name}<br/>Cells: ${Number(value).toLocaleString()}<br/>Ratio: ${percent}%`,
      },
      legend: {
        type: "scroll",
        bottom: 0,
        textStyle: { color: "#5E6C67" },
      },
      series: [
        {
          name: "Cell type",
          type: "pie",
          radius: ["42%", "70%"],
          center: ["50%", "43%"],
          avoidLabelOverlap: true,
          itemStyle: {
            borderColor: "#fff",
            borderWidth: 2,
          },
          label: {
            color: "#1B2A27",
            formatter: "{b}",
          },
          data: props.data.items.map((item, index) => ({
            name: item.label,
            value: item.count,
            itemStyle: {
              color: colors[index] ?? getBioChartColor(index),
            },
          })),
        },
      ],
    },
    true
  );
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

watch(() => [props.data, props.loading, props.error], renderChart, { deep: true, flush: "post" });
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

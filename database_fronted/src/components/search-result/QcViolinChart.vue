<template>
  <div class="chart-body">
    <el-skeleton v-if="loading" animated :rows="6" />
    <div v-else-if="error" class="chart-state">Failed to load chart data</div>
    <div v-else-if="!metric" class="chart-state">No chart data available</div>
    <div v-else class="chart-wrap">
      <div ref="chartEl" class="chart-canvas" />
    </div>
  </div>
</template>

<script setup lang="ts">
import * as echarts from "echarts";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import type { QcViolinData, QcViolinGroup, QcViolinMetric } from "@/api/searchResult";
import { getBioChartColor, getBioChartColorMap } from "@/utils/chartPalette";
import { downloadChart, type ChartDownloadOptions } from "@/utils/downloadChart";

const props = defineProps<{
  data: QcViolinData | null;
  loading: boolean;
  error: boolean;
}>();

const chartEl = ref<HTMLElement | null>(null);
const metric = computed<QcViolinMetric | null>(() => {
  return props.data?.metrics?.find((item) => item.groups?.length) ?? null;
});

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
  if (props.loading || props.error || !metric.value) {
    disposeChart();
    return;
  }

  await nextTick();
  const currentChart = ensureChart();
  if (!currentChart || !metric.value) return;

  const groupsWithDensity = metric.value.groups.filter((group) => group.density?.length);

  if (groupsWithDensity.length > 0) {
    renderDensityChart(currentChart, groupsWithDensity);
    return;
  }

  renderBoxplotChart(currentChart, metric.value.groups);
}

function renderDensityChart(currentChart: echarts.ECharts, groups: QcViolinGroup[]) {
  const colorMap = getBioChartColorMap(groups.map((group) => group.label));
  const colors = groups.map((group, index) => colorMap.get(group.label) ?? getBioChartColor(index));

  currentChart.setOption(
    {
      color: colors,
      tooltip: { trigger: "axis" },
      grid: { top: 22, left: 48, right: 18, bottom: 42 },
      xAxis: { type: "value", name: metric.value?.label || metric.value?.metric },
      yAxis: { type: "value", name: "Density" },
      legend: {
        type: "scroll",
        bottom: 0,
        textStyle: { color: "#5E6C67" },
      },
      series: groups.map((group, index) => {
        const color = colorMap.get(group.label) ?? getBioChartColor(index);

        return {
          name: group.label,
          type: "line",
          smooth: true,
          showSymbol: false,
          lineStyle: { color },
          itemStyle: { color },
          areaStyle: { color, opacity: 0.2 },
          data: group.density?.map((point) => [point.x, point.y]) ?? [],
        };
      }),
    },
    true
  );
}

function renderBoxplotChart(currentChart: echarts.ECharts, groups: QcViolinGroup[]) {
  const summaries = groups
    .map((group) => ({ label: group.label, summary: getSummary(group) }))
    .filter((item): item is { label: string; summary: [number, number, number, number, number] } => Boolean(item.summary));

  if (summaries.length === 0) {
    currentChart.clear();
    return;
  }

  const colorMap = getBioChartColorMap(summaries.map((item) => item.label));
  const colors = summaries.map((item, index) => colorMap.get(item.label) ?? getBioChartColor(index));

  currentChart.setOption(
    {
      color: colors,
      tooltip: {
        trigger: "item",
        formatter: ({ name, data, value }: { name: string; data: unknown; value: unknown }) => {
          const rawValues = getTooltipValues(data, value);
          const summaryValues = rawValues.length >= 6 ? rawValues.slice(1) : rawValues;
          const values = summaryValues.map((item) => Number(item).toFixed(3));
          return `${name}<br/>min ${values[0]}<br/>q1 ${values[1]}<br/>median ${values[2]}<br/>q3 ${values[3]}<br/>max ${values[4]}`;
        },
      },
      grid: { top: 18, left: 48, right: 18, bottom: 64 },
      xAxis: {
        type: "category",
        data: summaries.map((item) => item.label),
        axisLabel: { interval: 0, rotate: summaries.length > 5 ? 28 : 0 },
      },
      yAxis: { type: "value", name: metric.value?.label || metric.value?.metric },
      series: [
        {
          name: "QC",
          type: "boxplot",
          data: summaries.map((item, index) => {
            const color = colorMap.get(item.label) ?? getBioChartColor(index);

            return {
              value: item.summary,
              itemStyle: {
                color,
                borderColor: color,
                opacity: 0.82,
              },
            };
          }),
        },
      ],
    },
    true
  );
}

function getTooltipValues(data: unknown, value: unknown) {
  if (Array.isArray(value)) return value;
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object" && "value" in data && Array.isArray(data.value)) return data.value;
  return [];
}

function getSummary(group: QcViolinGroup): [number, number, number, number, number] | null {
  if (
    group.min !== undefined &&
    group.q1 !== undefined &&
    group.median !== undefined &&
    group.q3 !== undefined &&
    group.max !== undefined
  ) {
    return [group.min, group.q1, group.median, group.q3, group.max];
  }

  if (!group.values?.length) return null;

  const sortedValues = group.values
    .filter((value) => Number.isFinite(value))
    .slice()
    .sort((a, b) => a - b);

  if (sortedValues.length === 0) return null;

  return [
    sortedValues[0]!,
    quantile(sortedValues, 0.25),
    quantile(sortedValues, 0.5),
    quantile(sortedValues, 0.75),
    sortedValues[sortedValues.length - 1]!,
  ];
}

function quantile(sortedValues: number[], q: number) {
  const position = (sortedValues.length - 1) * q;
  const base = Math.floor(position);
  const rest = position - base;
  const next = sortedValues[base + 1];

  return next === undefined ? sortedValues[base]! : sortedValues[base]! + rest * (next - sortedValues[base]!);
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

.chart-wrap {
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
  .chart-wrap,
  .chart-canvas,
  .chart-state {
    min-height: 390px;
  }
}

@media (max-width: 760px) {
  .chart-body,
  .chart-wrap,
  .chart-canvas,
  .chart-state {
    min-height: 340px;
  }
}
</style>

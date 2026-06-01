<template>
    <div class="grid">
      <div ref="c1" class="chart"></div>
      <div ref="c2" class="chart"></div>
    </div>
  </template>
  
  <script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, watch } from "vue";
  import * as echarts from "echarts";
  
  type Stats = {
    versions: string[]; // ["ATACdb1.0", "ATACdb2.0"]
    samples: { human: number[]; mouse: number[] };
    cars: { human: number[]; mouse: number[] }; // chromatin accessibility regions
  };
  
  const props = defineProps<{ data: Stats }>();
  
  const c1 = ref<HTMLDivElement | null>(null);
  const c2 = ref<HTMLDivElement | null>(null);
  
  let chart1: echarts.ECharts | null = null;
  let chart2: echarts.ECharts | null = null;
  
  function makeBarOption(title: string, versions: string[], human: number[], mouse: number[]) {
    return {
      title: { text: title, left: "center", top: 6, textStyle: { fontWeight: 800 } },
      tooltip: { trigger: "axis" },
      legend: { top: 34, left: "center" },
      grid: { left: 50, right: 20, top: 70, bottom: 45 },
      xAxis: { type: "category", data: versions },
      yAxis: {
        type: "value",
        axisLabel: { formatter: (v: number) => v.toLocaleString() },
        splitLine: { lineStyle: { opacity: 0.4 } },
      },
      series: [
        {
          name: "Human",
          type: "bar",
          data: human,
          barWidth: 44,
          label: { show: true, position: "inside", rotate: 90, formatter: (p: any) => Number(p.value).toLocaleString() },
        },
        {
          name: "Mouse",
          type: "bar",
          data: mouse,
          barWidth: 44,
          label: { show: true, position: "inside", rotate: 90, formatter: (p: any) => Number(p.value).toLocaleString() },
        },
      ],
    };
  }
  
  function render() {
    if (!c1.value || !c2.value) return;
  
    if (!chart1) chart1 = echarts.init(c1.value);
    if (!chart2) chart2 = echarts.init(c2.value);
  
    chart1.setOption(makeBarOption("Number of samples", props.data.versions, props.data.samples.human, props.data.samples.mouse), true);
    chart2.setOption(
      makeBarOption(
        "Number of chromatin accessibility regions",
        props.data.versions,
        props.data.cars.human,
        props.data.cars.mouse
      ),
      true
    );
  }
  
  let ro: ResizeObserver | null = null;
  
  onMounted(() => {
    render();
    ro = new ResizeObserver(() => {
      chart1?.resize();
      chart2?.resize();
    });
    if (c1.value) ro.observe(c1.value);
    if (c2.value) ro.observe(c2.value);
  });
  
  onBeforeUnmount(() => {
    ro?.disconnect();
    chart1?.dispose();
    chart2?.dispose();
    chart1 = null;
    chart2 = null;
  });
  
  watch(() => props.data, () => render(), { deep: true });
  </script>
  
  <style scoped>
  .grid{
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }
  .chart{
    width: 100%;
    height: 340px;
    border-radius: 16px;
    background: #fbfbff;
    border: 1px solid #eeeef7;
  }
  @media (max-width: 980px){
    .grid{ grid-template-columns: 1fr; }
  }
  </style>
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

function registerWorldIfNeeded() {
  const gj = feature(world110m as any, (world110m as any).objects.countries) as any;
  echarts.registerMap("WORLD", gj);
}

const HUB = { lon: 112.57, lat: 26.89, name: "Hengyang" };

function makeFlightData(pts: Point[]) {
  if (pts.length === 0) return [];
  return pts.map(p => ({ coords: [[p.lon, p.lat], [HUB.lon, HUB.lat]] as [number, number][] }));
}

function continent(name: string): string {
  const map: Record<string, string> = {
    "China":"Asia","India":"Asia","Japan":"Asia","Indonesia":"Asia","Thailand":"Asia","Vietnam":"Asia",
    "Malaysia":"Asia","Singapore":"Asia","Philippines":"Asia","South Korea":"Asia",
    "Russia":"Europe","Germany":"Europe","France":"Europe","United Kingdom":"Europe","Italy":"Europe",
    "Spain":"Europe","Netherlands":"Europe","Switzerland":"Europe","Sweden":"Europe",
    "Nigeria":"Africa","South Africa":"Africa","Egypt":"Africa","Kenya":"Africa",
    "United States":"North America","Canada":"North America","Mexico":"North America",
    "Brazil":"South America","Argentina":"South America","Colombia":"South America",
    "Australia":"Oceania","New Zealand":"Oceania",
  };
  return map[name] || name;
}

function render() {
  if (!el.value) return;
  if (!chart) chart = echarts.init(el.value);
  registerWorldIfNeeded();

  const data = props.points.map(p => ({ name: p.name ?? "", value: [p.lon, p.lat, p.value ?? 1] }));
  const flights = makeFlightData(props.points);

  const LAND = "#D6E4F0";
  const DOT = "#8FA59C";
  const DOT_GLOW = "rgba(143,165,156,0.50)";
  const LINE = "rgba(143,165,156,0.28)";

  // Subtle continent colour palette — all within a tight blue-gray range
  const continentColors: Record<string, string> = {
    "North America": "#dce7f1",
    "Europe":        "#dae6ec",
    "Asia":          "#d5e1ec",
    "Africa":        "#d9e5ee",
    "South America": "#dee8f2",
    "Oceania":       "#e0e9f3",
  };

  // Visitor-heavy countries get a slightly deeper tint for subtle emphasis
  const visitorCountries = new Set([
    "China", "United States", "Singapore", "United Kingdom", "Germany",
    "Japan", "India", "France", "Canada", "Australia", "Brazil",
  ]);
  const VISITOR_EMPHASIS = "#cddce8";

  function buildRegions(gj: any): Array<{ name: string; itemStyle: { areaColor: string } }> {
    const regions: Array<{ name: string; itemStyle: { areaColor: string } }> = [];
    if (!gj?.features) return regions;
    for (const f of gj.features) {
      const name = f.properties?.name;
      if (!name) continue;
      const cont = continent(name);
      let color = continentColors[cont] || LAND;
      if (visitorCountries.has(name)) color = VISITOR_EMPHASIS;
      regions.push({ name, itemStyle: { areaColor: color } });
    }
    return regions;
  }

  const worldGeo = feature(world110m as any, (world110m as any).objects.countries) as any;
  const regions = buildRegions(worldGeo);

  chart.setOption(
    {
      backgroundColor: "transparent",
      tooltip: {
        trigger: "item",
        backgroundColor: "#fff",
        borderColor: "#C4D4CD",
        borderWidth: 1,
        textStyle: { color: "#1B2A27", fontSize: 12 },
        formatter: (p: any) => {
          if (p.seriesType === "effectScatter" || p.seriesType === "scatter")
            return p.name || "";
          return `<b>${p.name}</b><br/>${continent(p.name)}`;
        },
      },
      geo: {
        map: "WORLD",
        roam: true,
        silent: false,
        zoom: 1.8,
        center: [30, 28],
        regions: regions,
        itemStyle: {
          areaColor: LAND,
          borderColor: "rgba(255,255,255,0.65)",
          borderWidth: 0.5,
        },
        emphasis: {
          itemStyle: {
            areaColor: "#c8d8e6",
            borderColor: "#fff",
            borderWidth: 1,
          },
          label: {
            show: true,
            color: "#1B2A27",
            fontSize: 11,
            fontWeight: "bold",
            formatter: (p: any) => p.name,
          },
        },
        label: { show: false },
      },
      series: [
        {
          name: "Flights",
          type: "lines",
          coordinateSystem: "geo",
          polyline: false,
          data: flights,
          lineStyle: { color: LINE, width: 1.0, curveness: 0.3 },
          effect: {
            show: true,
            period: 4,
            trailLength: 0.2,
            symbol: "circle",
            symbolSize: 3,
            color: DOT,
          },
          zlevel: 1,
        },
        {
          name: "Visitors",
          type: "effectScatter",
          coordinateSystem: "geo",
          data: data,
          symbolSize: (val: any) => 6 + Math.min(10, (val[2] || 1) * 2),
          itemStyle: { color: DOT, shadowColor: DOT_GLOW, shadowBlur: 8 },
          rippleEffect: { brushType: "stroke", scale: 3, period: 4, color: DOT_GLOW },
          emphasis: { scale: 1.3, itemStyle: { color: "#fff" } },
          zlevel: 2,
        },
        {
          name: "Base",
          type: "scatter",
          coordinateSystem: "geo",
          data: data,
          symbolSize: 3,
          itemStyle: { color: DOT, opacity: 0.5 },
          zlevel: 1,
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
.map {
  width: 100%;
  height: 440px;
  border-radius: 16px;
  background: transparent;
}
</style>

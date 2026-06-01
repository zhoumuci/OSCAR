import type * as echarts from "echarts";

export type ChartDownloadOptions = {
  type?: "png" | "jpeg" | "svg";
  pixelRatio?: number;
  backgroundColor?: string;
  excludeComponents?: string[];
};

const DEFAULT_DOWNLOAD_OPTIONS: ChartDownloadOptions = {
  type: "png",
  pixelRatio: 3,
  backgroundColor: "#ffffff",
};

export function downloadChart(
  chart: Pick<echarts.ECharts, "getDataURL"> | null | undefined,
  filename: string,
  options: ChartDownloadOptions = {}
) {
  if (!chart) return false;

  try {
    const dataUrl = chart.getDataURL({
      ...DEFAULT_DOWNLOAD_OPTIONS,
      ...options,
    });

    if (!dataUrl) return false;

    const link = document.createElement("a");
    link.href = dataUrl;
    link.download = filename;
    link.style.display = "none";
    document.body.appendChild(link);
    link.click();
    link.remove();

    return true;
  } catch (error) {
    console.warn("[ChartDownload] Failed to export chart image:", error);
    return false;
  }
}

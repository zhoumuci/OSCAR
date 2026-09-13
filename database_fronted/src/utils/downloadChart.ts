import * as echarts from "echarts";

export type ChartImageFormat = "png" | "jpeg" | "svg";

export type ChartDownloadOptions = {
  type?: ChartImageFormat;
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
  chart: Pick<echarts.ECharts, "getDataURL" | "getOption" | "getWidth" | "getHeight"> | null | undefined,
  filename: string,
  options: ChartDownloadOptions = {}
) {
  if (!chart) return false;

  try {
    const resolvedOptions = {
      ...DEFAULT_DOWNLOAD_OPTIONS,
      ...options,
    };
    const dataUrl = resolvedOptions.type === "svg"
      ? createSvgDataUrl(chart, resolvedOptions)
      : chart.getDataURL(resolvedOptions);

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

export function downloadChartPdf(
  chart: Pick<echarts.ECharts, "getDataURL" | "getWidth" | "getHeight"> | null | undefined,
  filename: string,
  options: Omit<ChartDownloadOptions, "type"> = {}
) {
  if (!chart) return false;

  try {
    const pixelRatio = Math.max(1, options.pixelRatio ?? DEFAULT_DOWNLOAD_OPTIONS.pixelRatio ?? 3);
    const jpegDataUrl = chart.getDataURL({
      type: "jpeg",
      pixelRatio,
      backgroundColor: options.backgroundColor ?? DEFAULT_DOWNLOAD_OPTIONS.backgroundColor,
      excludeComponents: options.excludeComponents,
    });
    if (!jpegDataUrl?.startsWith("data:image/jpeg")) return false;

    const imageWidth = Math.max(1, Math.round(chart.getWidth() * pixelRatio));
    const imageHeight = Math.max(1, Math.round(chart.getHeight() * pixelRatio));
    const pdf = createSingleImagePdf(jpegDataUrl, imageWidth, imageHeight);
    const pdfBuffer = new ArrayBuffer(pdf.byteLength);
    new Uint8Array(pdfBuffer).set(pdf);
    const url = URL.createObjectURL(new Blob([pdfBuffer], { type: "application/pdf" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.style.display = "none";
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
    return true;
  } catch (error) {
    console.warn("[ChartDownload] Failed to export chart PDF:", error);
    return false;
  }
}

function createSingleImagePdf(jpegDataUrl: string, imageWidth: number, imageHeight: number): Uint8Array {
  const encoded = jpegDataUrl.slice(jpegDataUrl.indexOf(",") + 1);
  const binary = window.atob(encoded);
  const jpeg = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) jpeg[i] = binary.charCodeAt(i);

  const landscape = imageWidth >= imageHeight;
  const pageWidth = landscape ? 842 : 595;
  const pageHeight = landscape ? 595 : 842;
  const margin = 28;
  const scale = Math.min(
    (pageWidth - margin * 2) / imageWidth,
    (pageHeight - margin * 2) / imageHeight
  );
  const drawWidth = imageWidth * scale;
  const drawHeight = imageHeight * scale;
  const drawX = (pageWidth - drawWidth) / 2;
  const drawY = (pageHeight - drawHeight) / 2;
  const content = `q\n${pdfNumber(drawWidth)} 0 0 ${pdfNumber(drawHeight)} ${pdfNumber(drawX)} ${pdfNumber(drawY)} cm\n/Im0 Do\nQ\n`;

  const parts: Uint8Array[] = [];
  const offsets: number[] = [0];
  let length = 0;
  const push = (part: Uint8Array) => {
    parts.push(part);
    length += part.length;
  };
  const pushText = (value: string) => push(new TextEncoder().encode(value));
  const pushObject = (number: number, body: string | Uint8Array) => {
    offsets[number] = length;
    pushText(`${number} 0 obj\n`);
    if (typeof body === "string") pushText(body);
    else push(body);
    pushText("\nendobj\n");
  };

  pushText("%PDF-1.4\n");
  pushObject(1, "<< /Type /Catalog /Pages 2 0 R >>");
  pushObject(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
  pushObject(3, `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageWidth} ${pageHeight}] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>`);

  offsets[4] = length;
  pushText(`4 0 obj\n<< /Type /XObject /Subtype /Image /Width ${imageWidth} /Height ${imageHeight} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.length} >>\nstream\n`);
  push(jpeg);
  pushText("\nendstream\nendobj\n");

  pushObject(5, `<< /Length ${content.length} >>\nstream\n${content}endstream`);
  const xrefOffset = length;
  pushText("xref\n0 6\n0000000000 65535 f \n");
  for (let i = 1; i <= 5; i++) {
    pushText(`${String(offsets[i]).padStart(10, "0")} 00000 n \n`);
  }
  pushText(`trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF\n`);

  const pdf = new Uint8Array(length);
  let cursor = 0;
  parts.forEach((part) => {
    pdf.set(part, cursor);
    cursor += part.length;
  });
  return pdf;
}

function pdfNumber(value: number): string {
  return value.toFixed(3).replace(/\.?0+$/, "");
}

function createSvgDataUrl(
  sourceChart: Pick<echarts.ECharts, "getOption" | "getWidth" | "getHeight">,
  options: ChartDownloadOptions
) {
  const host = document.createElement("div");
  const width = Math.max(1, sourceChart.getWidth());
  const height = Math.max(1, sourceChart.getHeight());
  host.style.cssText = `position:fixed;left:-100000px;top:0;width:${width}px;height:${height}px;visibility:hidden;`;
  document.body.appendChild(host);

  const svgChart = echarts.init(host, undefined, {
    renderer: "svg",
    width,
    height,
  });

  try {
    const sourceOption = sourceChart.getOption() as Record<string, unknown>;
    const sourceSeries = Array.isArray(sourceOption.series) ? sourceOption.series : [];
    svgChart.setOption({
      ...sourceOption,
      animation: false,
      series: sourceSeries.map((series) => ({
        ...(series as Record<string, unknown>),
        animation: false,
        large: false,
        progressive: 0,
        progressiveThreshold: Number.MAX_SAFE_INTEGER,
      })),
    }, true);
    const dataUrl = svgChart.getDataURL({
      type: "svg",
      backgroundColor: options.backgroundColor,
      excludeComponents: options.excludeComponents,
    });
    if (!dataUrl?.startsWith("data:image/svg+xml")) {
      throw new Error("ECharts did not return SVG data");
    }
    assertVectorSvg(dataUrl);
    return dataUrl;
  } finally {
    svgChart.dispose();
    host.remove();
  }
}

function assertVectorSvg(dataUrl: string) {
  const separator = dataUrl.indexOf(",");
  if (separator < 0) throw new Error("SVG data URL is malformed");

  const metadata = dataUrl.slice(0, separator);
  const payload = dataUrl.slice(separator + 1);
  const svg = /;base64(?:;|$)/i.test(metadata)
    ? window.atob(payload)
    : decodeURIComponent(payload);

  if (!/<svg(?:\s|>)/i.test(svg)) {
    throw new Error("SVG export does not contain an SVG document");
  }
  if (/<image(?:\s|>)/i.test(svg)) {
    throw new Error("SVG export contains an embedded or linked raster image");
  }
}

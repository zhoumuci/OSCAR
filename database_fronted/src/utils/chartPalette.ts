export const BIO_CHART_PALETTE = [
  "#6F6AAE",
  "#E69F45",
  "#8DBF4F",
  "#D9655D",
  "#5FA8C7",
  "#B875B8",
  "#66B8A6",
  "#C7A76C",
  "#8C8C8C",
  "#D58AB3",
  "#7C9A5B",
  "#9A7BB5",
] as const;

export function getBioChartColor(index: number) {
  const safeIndex = ((index % BIO_CHART_PALETTE.length) + BIO_CHART_PALETTE.length) % BIO_CHART_PALETTE.length;
  return BIO_CHART_PALETTE[safeIndex] ?? BIO_CHART_PALETTE[0];
}

export function getBioChartColorMap(labels: readonly string[]) {
  const uniqueLabels = Array.from(new Set(labels.map((label) => label || "Unlabeled"))).sort((a, b) =>
    a.localeCompare(b)
  );
  const colorMap = new Map(uniqueLabels.map((label, index) => [label, getBioChartColor(index)]));

  applyProliferatingNkColorSwap(uniqueLabels, colorMap);

  return colorMap;
}

const NON_GREEN_SWAP_COLORS = new Set([
  "#6F6AAE",
  "#E69F45",
  "#D9655D",
  "#5FA8C7",
  "#B875B8",
  "#C7A76C",
  "#8C8C8C",
  "#D58AB3",
  "#9A7BB5",
]);

const PROLIFERATING_NK_FALLBACK_SWAP_LABELS = [
  "basophil",
  "basophils",
  "classical monocyte",
  "classical monocytes",
  "eosinophil",
  "eosinophils",
  "mast cell",
  "mast cells",
  "plasma cell",
  "plasma cells",
  "b cell",
  "b cells",
  "dendritic cell",
  "dendritic cells",
  "dc",
  "monocyte",
  "monocytes",
];

function applyProliferatingNkColorSwap(labels: readonly string[], colorMap: Map<string, string>) {
  const proliferatingNkLabel = labels.find(isProliferatingNkCellLabel);
  if (!proliferatingNkLabel) return;

  const proliferatingNkColor = colorMap.get(proliferatingNkLabel);
  if (!proliferatingNkColor || NON_GREEN_SWAP_COLORS.has(proliferatingNkColor)) return;

  const targetLabel =
    findNonGreenSwapLabel(labels, colorMap, isBasophilLabel) ??
    findNonGreenSwapLabel(labels, colorMap, (label) =>
      PROLIFERATING_NK_FALLBACK_SWAP_LABELS.includes(normalizeLabel(label))
    ) ??
    findNonGreenSwapLabel(labels, colorMap);

  if (!targetLabel) return;

  const targetColor = colorMap.get(targetLabel);
  if (!targetColor) return;

  colorMap.set(proliferatingNkLabel, targetColor);
  colorMap.set(targetLabel, proliferatingNkColor);
}

function findNonGreenSwapLabel(
  labels: readonly string[],
  colorMap: Map<string, string>,
  predicate: (label: string) => boolean = () => true
) {
  return labels.find((label) => {
    if (isProliferatingNkCellLabel(label) || isNkCellLabel(label) || isCd4TCellLabel(label) || !predicate(label)) {
      return false;
    }

    const color = colorMap.get(label);
    return color ? NON_GREEN_SWAP_COLORS.has(color) : false;
  });
}

function isProliferatingNkCellLabel(label: string) {
  const normalized = normalizeLabel(label);
  return normalized.includes("proliferating") && (normalized.includes("nk") || normalized.includes("natural killer"));
}

function isNkCellLabel(label: string) {
  const normalized = normalizeLabel(label);
  return normalized === "nk" || normalized === "nk cell" || normalized === "nk cells" || normalized === "natural killer cell" || normalized === "natural killer cells";
}

function isBasophilLabel(label: string) {
  const normalized = normalizeLabel(label);
  return normalized === "basophil" || normalized === "basophils";
}

function isCd4TCellLabel(label: string) {
  const normalized = normalizeLabel(label);
  return normalized.includes("cd4") && normalized.includes("t");
}

function normalizeLabel(label: string) {
  return label.trim().toLowerCase().replace(/[._-]+/g, " ").replace(/\s+/g, " ");
}

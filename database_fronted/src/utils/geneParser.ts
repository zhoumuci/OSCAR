/**
 * Gene symbol parser with security validation and smart format detection.
 *
 * Supported input formats:
 *   - Plain text: one gene per line, or comma/space/semicolon separated
 *   - CSV: auto-detects gene column by header name, skips non-gene columns
 *   - TSV: same as CSV but tab-delimited
 *
 * Security:
 *   - Rejects SQL injection patterns (' OR 1=1, ; DROP, etc.)
 *   - Only allows valid HGNC gene symbols (alphanumeric + . - @)
 *   - Caps at MAX_GENE_COUNT to prevent DoS
 */

// ---- constants ----
const MAX_GENE_COUNT = 1000;
const MAX_GENE_LENGTH = 30;
const MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

/** Valid HGNC gene symbol: starts with letter, then letters/digits/./-/@ */
const GENE_SYMBOL_RE = /^[A-Za-z][A-Za-z0-9.\-@]*$/;

/** Injection / suspicious patterns — reject entire input if any line matches */
const INJECTION_PATTERNS = [
  /\bOR\s+\d+\s*=\s*\d+\b/i,           // OR 1=1
  /\bAND\s+\d+\s*=\s*\d+\b/i,          // AND 1=1
  /'?\s*OR\s+'?\d+'?\s*=\s*'?\d/i,    // ' OR '1'='1
  /;\s*(DROP|DELETE|INSERT|UPDATE|ALTER|TRUNCATE|CREATE|EXEC|EXECUTE)\b/i,
  /--\s*$/,                             // SQL comment
  /\/\*.*\*\//,                         // block comment
  /\bUNION\s+(ALL\s+)?SELECT\b/i,
  /<script\b/i,                         // XSS
  /\bon\w+\s*=\s*/i,                    // JS event handler
  /javascript\s*:/i,
];

/** Common gene column header names (case-insensitive) */
const GENE_COLUMN_NAMES = [
  "gene", "gene_symbol", "genesymbol", "symbol",
  "gene_name", "genename", "name", "hgnc_symbol",
  "hgnc", "marker", "marker_gene", "markergene",
];

// ---- types ----
export interface ParseResult {
  genes: string[];
  totalFound: number;
  filteredOut: { value: string; reason: string }[];
  warnings: string[];
  format: "plain" | "csv" | "tsv" | "unknown";
}

// ---- public API ----

/**
 * Parse a raw text string (from textarea or file) into a validated gene list.
 */
export function parseGenes(raw: string): ParseResult {
  const warnings: string[] = [];
  const filteredOut: { value: string; reason: string }[] = [];

  if (!raw || !raw.trim()) {
    return { genes: [], totalFound: 0, filteredOut, warnings: ["Input is empty."], format: "unknown" };
  }

  // Step 0 — security scan on raw input
  const injectionHit = INJECTION_PATTERNS.find((p) => p.test(raw));
  if (injectionHit) {
    throw new Error(`Input rejected: suspicious pattern detected (${injectionHit.source}).`);
  }

  // Step 1 — detect format and extract candidate tokens
  const { tokens, format } = extractTokens(raw, warnings);

  // Step 2 — validate each token
  const seen = new Set<string>();
  const genes: string[] = [];

  for (const token of tokens) {
    const upper = token.toUpperCase().trim();
    if (!upper) continue;
    if (seen.has(upper)) continue;

    // Length check
    if (upper.length > MAX_GENE_LENGTH) {
      filteredOut.push({ value: token, reason: `exceeds max length ${MAX_GENE_LENGTH}` });
      continue;
    }

    // Symbol format check
    if (!GENE_SYMBOL_RE.test(upper)) {
      filteredOut.push({ value: token, reason: "invalid gene symbol format" });
      continue;
    }

    // Count cap
    if (genes.length >= MAX_GENE_COUNT) {
      warnings.push(`Reached max gene count (${MAX_GENE_COUNT}). Truncated.`);
      break;
    }

    seen.add(upper);
    genes.push(upper);
  }

  return {
    genes,
    totalFound: tokens.length,
    filteredOut,
    warnings: warnings.length > 0 ? warnings : [],
    format,
  };
}

/**
 * Parse file content — handles .txt, .csv, .tsv.
 * Returns a ParseResult with gene list and metadata.
 */
export function parseFileContent(
  fileName: string,
  text: string
): ParseResult {
  const ext = fileName.toLowerCase().split(".").pop() || "";
  if (!["txt", "csv", "tsv", "tab"].includes(ext)) {
    throw new Error(`Unsupported file type: .${ext}. Supported: .txt, .csv, .tsv`);
  }
  if (text.length > MAX_FILE_BYTES) {
    throw new Error(`File too large (max ${MAX_FILE_BYTES / 1024 / 1024} MB).`);
  }
  const nonEmptyLines = text.split(/\r?\n/).filter((line) => line.trim());
  const firstCell = (nonEmptyLines[0] ?? "").trim().replace(/["']/g, "").toLowerCase();
  if (["csv", "tsv", "tab"].includes(ext) && GENE_COLUMN_NAMES.includes(firstCell)) {
    const result = parseGenes(nonEmptyLines.slice(1).join("\n"));
    return {
      ...result,
      format: ext === "csv" ? "csv" : "tsv",
      warnings: [`Detected a single gene column named "${firstCell}".`, ...result.warnings],
    };
  }
  return parseGenes(text);
}

/**
 * Validate a single gene symbol. Returns null if valid, error string if not.
 */
export function validateGeneSymbol(gene: string): string | null {
  if (!gene || !gene.trim()) return "empty";
  const upper = gene.toUpperCase().trim();
  if (upper.length > MAX_GENE_LENGTH) return `too long (max ${MAX_GENE_LENGTH})`;
  if (!GENE_SYMBOL_RE.test(upper)) return "invalid characters";
  return null;
}

// ---- internal helpers ----

/**
 * Split raw text into candidate gene tokens, auto-detecting format.
 */
function extractTokens(
  raw: string,
  warnings: string[]
): { tokens: string[]; format: "plain" | "csv" | "tsv" | "unknown" } {
  // Try CSV/TSV detection: does first line look like a header?
  const lines = raw.split(/\r?\n/).filter((l) => l.trim());
  if (lines.length === 0) return { tokens: [], format: "unknown" };

  const firstLine = lines[0] ?? "";

  // Detect delimiter
  const delim = detectDelimiter(firstLine);

  if (delim && firstLineHasGeneHeader(firstLine, delim)) {
    // Structured format — extract gene column
    const geneColIdx = findGeneColumnIndex(firstLine, delim);
    if (geneColIdx >= 0) {
      const tokens: string[] = [];
      for (let i = 1; i < lines.length; i++) {
        const cols = splitLine(lines[i] ?? "", delim);
        if (cols.length > geneColIdx) {
          const val = (cols[geneColIdx] ?? "").trim();
          if (val && !val.match(/^["']?$/) && !isLikelyNumeric(val)) {
            // Split cell content by common separators (comma, semicolon, space)
            const subTokens = val.split(/[,;\s]+/).filter(Boolean);
            tokens.push(...subTokens);
          }
        }
      }
      warnings.push(
        `Detected ${delim === "\t" ? "TSV" : "CSV"} format with header. ` +
        `Using column "${(firstLine.split(delim)[geneColIdx] ?? "").trim()}" (index ${geneColIdx}).`
      );
      return { tokens, format: delim === "\t" ? "tsv" : "csv" };
    }
    // Header found but no gene column — fall through to plain text
    warnings.push("Header detected but no gene column found. Treating as plain text.");
  }

  // Plain text: split by common separators
  const normalized = raw
    .replace(/[,;\t]/g, " ")
    .replace(/\r?\n/g, " ")
    .replace(/\s+/g, " ");
  const tokens = normalized.split(" ").filter(Boolean);
  return { tokens, format: "plain" };
}

function detectDelimiter(line: string): string | null {
  const tabs = (line.match(/\t/g) || []).length;
  const commas = (line.match(/,/g) || []).length;
  if (tabs >= 2) return "\t";
  if (commas >= 2) return ",";
  if (tabs >= 1 && commas === 0) return "\t";
  if (commas >= 1 && tabs === 0) return ",";
  return null; // single column or plain text
}

function firstLineHasGeneHeader(line: string, delim: string): boolean {
  const cols = splitLine(line, delim).map((c) => c.trim().toLowerCase().replace(/["']/g, ""));
  return cols.some((c) => GENE_COLUMN_NAMES.some((name) => c === name || c.startsWith(name)));
}

function findGeneColumnIndex(line: string, delim: string): number {
  const cols = splitLine(line, delim).map((c) => c.trim().toLowerCase().replace(/["']/g, ""));
  for (const name of GENE_COLUMN_NAMES) {
    const idx = cols.findIndex((c) => c === name);
    if (idx >= 0) return idx;
  }
  // Fuzzy: first column that contains "gene"
  const fuzzyIdx = cols.findIndex((c) => c.includes("gene") || c.includes("symbol"));
  return fuzzyIdx;
}

function splitLine(line: string, delim: string): string[] {
  // Simple split — handles quoted fields loosely
  const result: string[] = [];
  let current = "";
  let inQuotes = false;
  for (const ch of line) {
    if (ch === '"' || ch === "'") {
      inQuotes = !inQuotes;
    } else if (ch === delim && !inQuotes) {
      result.push(current);
      current = "";
    } else {
      current += ch;
    }
  }
  result.push(current);
  return result;
}

function isLikelyNumeric(val: string): boolean {
  const trimmed = val.trim();
  if (!trimmed) return false;
  // Numbers, decimals, scientific notation
  return /^-?\d+(\.\d+)?([eE][+-]?\d+)?$/.test(trimmed);
}

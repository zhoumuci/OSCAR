# OSCAR User Guide

OSCAR is a human single-cell multi-omics regulatory database. This guide describes the functions that are currently available in the web interface, the accepted input formats, and the exact limits applied by the browser before a request is submitted.

> All genomic coordinates used by OSCAR are based on the hg38 (GRCh38) human reference genome.

## 1. Overview

OSCAR provides five connected workflows:

- Search samples by marker gene, genomic region, tissue, or standardized cell type.
- Browse all samples using metadata filters and keyword search.
- Open a Sample Details page to inspect sample metadata, cell landscapes, regulatory annotations, and regulatory networks.
- Open Gene Details or Peak Details pages to inspect occurrence patterns and reference regulatory annotations.
- Run Cell Enrichment, Sequence-based Peak Regulatory, or Peak-to-Gene Linkage analyses.
- Download sample-level marker and Peak-to-Gene files in TSV or CSV format.

The counts displayed on the Home page are loaded from the current database and may change when OSCAR is updated.

![OSCAR home page](../assets/help/oscar-home-overview.png)

> *Figure 1. OSCAR home page and database summary.*

## 2. Data Types

### 2.1 Marker genes

OSCAR stores two marker-gene signal types:

- **Gene expression markers** are derived from RNA expression.
- **Gene score markers** are derived from ATAC-based gene activity scores.

The two signal types remain separate during Search, Sample Details display, and download.

### 2.2 Marker peaks

Marker peaks are differentially accessible genomic regions associated with a sample group, cluster, or cell-type label. Peak coordinates are displayed as `chromosome:start-end`.

### 2.3 Peak-to-Gene links

Peak-to-Gene, or P2G, links connect accessible peaks with putative target genes. These links are regulatory candidates, not proof of a causal relationship.

Two P2G views are available where applicable:

- **P2G links (all)** returns stored P2G links without requiring marker status at both ends.
- **P2G links (marker)** keeps links for which the peak is a marker peak and the gene is a marker gene in the matching sample data.

### 2.4 Reference regulatory annotations

OSCAR can compare gene-associated regions or peak regions with available reference tracks, including SNP, eQTL, TFBS, enhancer, super-enhancer, methylation, CRISPR, ATAC, chromatin-interaction, DNase, TAD, eRNA, TF ChIP-seq, and transcription-factor cofactor tracks. Only sources marked as available can be selected.

## 3. Quick Start

1. Open **Search** and choose a query card.
2. Enter a valid query or select a standardized tissue or cell type.
3. Open a Dataset ID from the result table.
4. Inspect the sample overview, cell landscape, regulatory annotation, and regulatory network.
5. Open a linked gene or peak when a details button is available.
6. Use the page-level download controls or the main **Download** page to export data.

![Quick-start workflow](../assets/help/quick-start-workflow.png)

> *Figure 2. Search, Sample Details, regulatory exploration, and download workflow.*

## 4. Search

The Search page contains four expandable cards. Results remain in the card that produced them and are shown in paginated tables.

### 4.1 Gene Search

Gene Search finds samples in which the submitted genes occur as the selected marker signal type.

**Input rules:**

- Maximum: **200 gene entries**.
- Textarea separators: line break, comma, space, or semicolon.
- Upload formats: **`.txt`, `.csv`, `.tsv`**.
- CSV and TSV files may contain a recognized gene column such as `gene`, `gene_symbol`, `symbol`, `gene_name`, `hgnc_symbol`, or `marker_gene`.
- Gene symbols are converted to uppercase and duplicate symbols are removed.
- Unsupported symbols and files are rejected before Search can run.

**Available settings:**

- **Sort by:** Dataset ID, Cell counts, or Matched genes when more than one gene is supplied.
- **Match mode:** Union returns samples matching at least one gene; Intersection requires every submitted gene in the same sample.
- **Per page:** 10, 20, or 50 table rows. This changes pagination only and does not truncate the underlying result set.
- **Signal Type:** Gene expression markers or Gene score markers.
- **Tissue:** optionally restricts the search to one tissue; leave it empty to search all tissues.
- **Tissue:** optional searchable single-tissue filter.

If more than 200 entries are pasted or uploaded, the input box is highlighted, a detailed warning is displayed, and the Search button is disabled.

![Search by Gene](../assets/help/search-by-gene.png)

> *Figure 3. Gene Search input, settings, validation, and paginated results.*

### 4.2 Region Search

Region Search finds marker peaks that overlap submitted genomic intervals in one specified OSCAR dataset.

**Input rules:**

- Maximum: **200 regions**.
- Accepted text: `chr:start-end` or the first three BED columns `chromosome start end`.
- Upload formats: **`.bed`, `.txt`, `.csv`, `.tsv`**.
- Comment, `track`, and `browser` lines in uploaded region files are ignored.
- A **Dataset ID is required**.
- The current search domain is Integration.

**Available settings:**

- **Match mode:** Any input region or All input regions.
- **Per page:** 10, 20, or 50 rows.

Invalid rows are reported. More than 200 regions produces a persistent warning and disables Search. A loading overlay is displayed because a multi-region overlap search may take longer than the other Search modes.

![Search by Genome Region](../assets/help/search-by-region.png)

> *Figure 4. Region Search input, dataset selection, settings, and results.*

### 4.3 Tissue Search

Tissue Search accepts one tissue at a time. Select a value from the searchable dropdown; free-text tissue names and multi-tissue input are not used.

The **TOP 12 Tissues** chart ranks tissues by sample count. Clicking a chart segment selects and searches that tissue. Results can be sorted by Dataset ID or Cell Counts and paginated at 10, 20, or 50 rows per page.

![Search by Tissue Type](../assets/help/search-by-tissue.png)

> *Figure 5. Searchable single-tissue selection and tissue results.*

### 4.4 Cell Type Search

Cell Type Search accepts one standardized cell type at a time. Start typing in the searchable dropdown and select one of the stored cell-type names. This avoids spelling and alias mismatches.

The **TOP 12 Cell Types** chart ranks standardized cell types by sample coverage. Clicking a chart segment runs that cell-type search. Results can be sorted by Dataset ID, total Cell Counts, or the number of cells matching the selected cell type.

![Search by Cell Type](../assets/help/search-by-cell-type.png)

> *Figure 6. Searchable standardized cell-type selection and cell-type results.*

### 4.5 Search result navigation

Clicking a Dataset ID opens Sample Details. The button in the upper-right corner of Sample Details returns to the page that opened it: Search, Browse, Home, or Download. Sample Details and Gene/Peak Details are standalone detail pages, so no main navigation item is highlighted while they are open.

## 5. Browse

The Browse page lists OSCAR samples in a server-paginated table.

- Use Biosample type and Tissue Type facets to filter the table.
- Active filters appear as removable chips.
- Use the keyword box to search the available sample metadata.
- Sort supported columns from the table header.
- Click a Dataset ID to open Sample Details.

The Sample Details return button leads back to Browse when the sample was opened from this page.

![Data Browse page](../assets/help/data-browse.png)

> *Figure 7. Browse filters, sample table, sorting, and navigation.*

## 6. Sample Details

Sample Details is organized into an overview, cell landscape, regulatory annotation, and, for Integration data, a regulatory network.

### 6.1 Domain selector and overview

The Integration, RNA, and ATAC buttons switch the displayed data domain when that domain is available for the sample. The overview shows the sample identifier and its stored metadata.

![Sample Details overview](../assets/help/sample-detail-overview.png)

> *Figure 8. Sample Details domain selector and metadata overview.*

### 6.2 Cell landscape

The landscape section can display the available embedding, cell-type composition, and QC views. Chart download buttons export the current image or the corresponding data table. Full-data downloads are requested separately from the displayed chart subset.

![Sample visualization panel](../assets/help/sample-visualization.png)

> *Figure 9. Sample-level cell landscape and visualization controls.*

### 6.3 Regulatory annotation tables

The regulatory annotation module provides the data types available for the selected domain, including gene expression markers, gene score markers, marker peaks, P2G marker links, and all P2G links.

- Filters applied in the module also apply to its regulatory-annotation CSV download.
- Result tables are paginated in the browser.
- Gene and peak detail buttons are shown only when the row contains sufficient identifiers or genomic coordinates.
- P2G marker rows represent links with marker support at both the peak and gene ends.
- Full downloads are prepared by the download endpoint and do not depend on the current visible table page.

During a long download, the annotation module is locked and displays progress feedback. The main navigation remains usable. If the user leaves and later returns to the same Sample Details page while the download is still running, the download state remains visible.

![Peak-to-Gene link table](../assets/help/p2g-link-table.png)

> *Figure 10. Sample regulatory annotation filters, paginated P2G table, and download action.*

### 6.4 Regulatory network

The Integration view can display a Peak-to-Gene regulatory network. Network controls, filters, and downloads apply to the current sample and domain.

## 7. Gene Details and Peak Details

Gene Details and Peak Details are opened from four current interface paths:

- A gene row in Sample Details regulatory annotation.
- A peak row in Sample Details regulatory annotation.
- A peak in the Sample Details regulatory network.
- A result link from Cell Enrichment or Sequence-based analysis.

The upper-right return button uses the recorded source. It returns to Sample Details for sample-origin links and to the appropriate Analysis results for analysis-origin links. Direct links without a recorded source use the browser history.

### 7.1 Overview

The Overview module summarizes marker occurrence across OSCAR datasets, cell types, and clusters. Gene Details additionally shows available expression profiles by platform. These overview queries begin when the details page opens.

![Gene Details overview](../assets/help/gene-detail-header.png)

> *Figure 11. Gene Details header, occurrence summary, and expression overview.*

### 7.2 Gene regulatory annotation

The regulatory annotation module does **not** run automatically when Gene Details opens. To run it:

1. Choose Promoter, Super Enhancer, or Typical Enhancer.
2. Choose one available reference annotation source.
3. Wait for the request to finish before selecting another option.

Promoter mode uses the gene promoter region. Super Enhancer and Typical Enhancer modes first obtain all enhancer regions linked to the selected gene and then compare every returned enhancer region with the selected reference track. The process is not limited to the first table page.

The loading mask locks only the regulatory annotation module. It does not lock the Overview module or the main navigation. Leaving and reopening Gene Details starts a fresh, unlocked page and does not automatically restart the previous annotation request.

![Gene regulatory annotation cards](../assets/help/gene-regulatory-annotation-cards.png)

> *Figure 12. Gene regulatory annotation type and reference-source selection.*

### 7.3 Peak regulatory annotation

Peak Details compares the selected peak interval with one available reference track. Choose a source card to start the overlap query. One row is returned for each reported overlap.

![Peak Details](../assets/help/peak-detail-overview.png)

> *Figure 13. Peak Details overview and reference annotation query.*

### 7.4 Pagination and download

Gene and Peak regulatory annotation tables support page sizes of 10, 20, and 50. Pagination changes only the visible rows. The CSV action exports the complete returned annotation result rather than only the visible page.

## 8. Analysis

The Analysis page contains three expandable modules. Each module validates its input in the browser and disables its run button when a stated maximum is exceeded.

### 8.1 Cell Enrichment Analysis

Cell Enrichment tests an input gene set against OSCAR Integration marker genes using a hypergeometric test and Benjamini-Hochberg FDR correction.

**Input:**

- Maximum: **200 gene entries**.
- Upload formats: **`.txt`, `.csv`, `.tsv`**.
- The same gene-column recognition and symbol validation used by Gene Search is applied.

**Settings:**

- **Tissue and dataset:** select a tissue first, then select exactly one dataset from that tissue. The analysis does not combine different datasets.
- **Result level:** Cell type combines clusters that share the same standardized cell type within the selected dataset. Cluster tests each cluster separately; every result is identified by the selected dataset and cluster.
- **Marker reference:** Integration markers.
- **Minimum overlap:** minimum number of overlapping genes required for a result.
- **FDR method:** BH (Benjamini-Hochberg).

Results include summary values, charts, and a paginated results table. Genes displayed in a result can open Gene Details and return to the preserved enrichment result.

![Cell Enrichment Analysis](../assets/help/analysis-cell-enrichment.png)

> *Figure 14. Cell Enrichment input, settings, and results.*

### 8.2 Sequence-based Peak Regulatory Analysis

This analysis maps a DNA sequence to hg38 with BLAST and then queries OSCAR records around the selected genomic match.

**Input:**

- Minimum: **10 bases**.
- Maximum: **20,000 bases (20 kb)** after FASTA headers and whitespace are removed.
- Allowed sequence characters: `A`, `C`, `G`, `T`, and `N`.
- Upload formats: **`.fasta`, `.fa`, `.txt`**.

**Settings:**

- **Reference scope:** all OSCAR datasets or one selected dataset.
- **Show results:** P2G links + Marker peaks, P2G links only, or Marker peaks only.
- **BLAST task:** Auto uses `blastn-short` for sequences up to 50 bases. For longer sequences, Auto first tries `megablast`, then automatically retries with `blastn` only when `megablast` returns no match. `megablast` is the fast option for highly similar sequences, including long sequences originating from hg38. `blastn` is slower but more sensitive to mismatches and local similarities; long or repeat-rich queries approaching 20 kb can take substantially longer and may return many local alignments. `blastn-short` is tuned for short nucleotide queries. A manually selected task overrides Auto and does not use this fallback.
- **Max target sequences, Max HSPs per target, E-value cutoff, and Flanking region.**
- **Maximum returned records:** optional; leave empty to return all matches. When both result types are selected, the limit is applied to each result query.

Results include BLAST candidates and only the selected OSCAR result type or types. When both result types are selected, an additional combined overlapping-peak overview is shown. Each result table is paginated. Selecting another BLAST hit refreshes the OSCAR records for that hit.

![Sequence-based Peak Regulatory Analysis](../assets/help/analysis-sequence-peak.png)

> *Figure 15. Sequence mapping settings, progress, and regulatory results.*

### 8.3 Peak-to-Gene Linkage Analysis

This analysis matches an input peak set and gene set against stored OSCAR P2G links.

**Peak input:**

- Maximum: **100 regions**.
- Accepted text: `chr:start-end` or the first three BED columns.
- Upload formats: **`.bed`, `.txt`**.

**Gene input:**

- Maximum: **100 gene entries**.
- Upload formats: **`.txt`, `.csv`**.

**Settings:**

- **Tissue:** required.
- **Dataset:** optional; leaving it empty searches all datasets in the selected tissue.
- **Reference mode:** P2G links only, or P2G links + marker.
- **Result type:** General, or Cell type when marker mode is selected.
- **Minimum overlap:** required overlap between the submitted region and the stored P2G peak.
- **Maximum returned records:** optional; leave empty for all matched results.

Marker mode requires marker support at both link ends. General results show P2G links without mixing in an unselected result type. Cell-type results count matched rows using the standardized cell-type labels stored with the matching records.

The result table is paginated. The network view displays no more than 30 Peak nodes per Gene and arranges nodes from left to right. Its download menu can save the current network image, the rows represented by the current network, or all returned rows.

![Peak-to-Gene Linkage Analysis](../assets/help/analysis-peak-gene-linkage.png)

> *Figure 16. Peak-to-Gene input, reference settings, results, and network view.*

## 9. Download

The Download page provides sample-level files and a complete reference archive.

### 9.1 Single-sample download

Click **Files** in a sample row. Available files are grouped by Integration, RNA, and ATAC according to the data present for that sample. Each download button is disabled while its file is being prepared and displays a spinner.

### 9.2 Download cart

Select up to **10 samples** and open **Download cart**. Choosing one file type and format downloads the corresponding file for each selected sample sequentially. The dialog displays the current sample number, total count, progress bar, and failed-download count if any file fails.

Keep the cart dialog open until the sequence completes.

### 9.3 Available formats and signal types

- Marker genes: TSV or CSV, separated by gene expression or gene score signal type.
- Marker peaks: TSV or CSV.
- P2G marker links: TSV or CSV.
- All P2G links: TSV or CSV.

The exact list depends on the selected sample and domain. Raw matrices and H5AD files are not offered by these sample file buttons.

![Download page](../assets/help/download-page.png)

> *Figure 17. Download table, Files dialog, and Download cart.*

## 10. Input Limits and Upload Formats

| Function | Maximum input | Accepted upload files | Additional requirements |
|----------|---------------|-----------------------|-------------------------|
| Gene Search | 200 genes | `.txt`, `.csv`, `.tsv` | Valid human gene symbols |
| Region Search | 200 regions | `.bed`, `.txt`, `.csv`, `.tsv` | Dataset ID required |
| Tissue Search | One selected tissue | No upload | Select from searchable list |
| Cell Type Search | One selected cell type | No upload | Select from standardized list |
| Cell Enrichment | 200 genes | `.txt`, `.csv`, `.tsv` | Integration marker reference |
| Sequence-based analysis | 20,000 bases | `.fasta`, `.fa`, `.txt` | At least 10 A/C/G/T/N bases |
| P2G peak set | 100 regions | `.bed`, `.txt` | Tissue required for analysis |
| P2G gene set | 100 genes | `.txt`, `.csv` | Tissue required for analysis |
| Download cart | 10 samples | Not applicable | Files are prepared sequentially |

An over-limit input is never silently truncated. The page displays a styled warning containing the observed count and the maximum, highlights the corresponding input, and prevents the Search or Analysis request from starting.

## 11. Interpretation Notes

### 11.1 Interpreting markers

A marker belongs to the sample, cell type or cluster, and analysis in which it was detected. It should not be interpreted as universally specific to one cell type.

### 11.2 Interpreting Peak-to-Gene links

P2G links are predicted regulatory associations. Interpret the link score, correlation, FDR, marker support, cell type, and dataset together. Experimental validation is required to establish causality.

### 11.3 Enhancer annotation

On Gene Details, Super Enhancer and Typical Enhancer are gene-associated query regions. The selected reference source is a separate comparison track. OSCAR checks all gene-associated enhancer regions returned for the query, then paginates the completed result for display.

### 11.4 Table pagination

Unless a control is explicitly named **Maximum returned records**, a page-size selector changes only how many rows are visible. It does not change the complete returned or downloaded result.

## 12. Frequently Asked Questions

### Why is the Search or Run button disabled?

The input may be empty, invalid, missing a required selection, or above the displayed maximum. Read the warning below the input and correct it before continuing.

### Why can I select only one tissue or cell type in Search?

OSCAR uses standardized stored values. Single-selection searchable dropdowns prevent spelling differences and ambiguous multi-value matching.

### Why can a regulatory annotation request take longer for some genes?

Some genes are associated with many enhancer regions. Every relevant region must be compared with the selected reference source before the complete result can be paginated and downloaded.

### Does a Marker Peak table show every possible target gene?

The displayed annotation follows the selected table mode and filters. P2G downloads remain separate and provide the corresponding complete link records for the selected file type.

### Are downloaded results limited to the visible page?

No. Table pagination controls only the displayed rows. Full-data download actions request the complete result associated with the selected query or file type.

### What happens when I open a details page?

Sample Details and Gene/Peak Details do not highlight Search or Analysis in the main navigation. Their upper-right return buttons use the recorded source page when one is available.

## 13. Citation and Contact

If OSCAR supports your research, cite the OSCAR manuscript using the citation shown on the website when it becomes available. For questions, data issues, or collaboration requests, use the Contact page.

## 14. Development Environment

- **Website:** TypeScript + Vue 3
- **Server:** Spring Boot + MyBatis
- **Database:** MySQL

## 15. Mobile and Tablet Access

OSCAR is designed to be responsive and supports browsing and analysis on mobile phones and tablets.

> **Document version:** 1.0
> **Last updated:** 2026-08-13

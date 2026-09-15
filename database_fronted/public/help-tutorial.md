# OSCAR User Guide

OSCAR is a human single-cell multi-omics regulatory database. This guide describes the functions that are currently available in the web interface, the accepted input formats, and the exact limits applied by the browser before a request is submitted.

> All genomic coordinates used by OSCAR are based on the hg38 (GRCh38) human reference genome.

## 1. Overview

OSCAR provides six connected workflows:

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

Where available, **ABC support** indicates whether Activity-by-Contact evidence supports a stored link. A blue point means supported and a gray point means evaluated but unsupported. Downloads use `1` for supported and `0` for evaluated but unsupported.

For ArchR-derived P2G links, **Correlation** measures the association between peak accessibility and linked-gene expression, and its sign gives the direction. **Link score** is the absolute correlation, so larger values indicate a stronger association regardless of direction. **FDR** is the multiple-testing adjusted significance of that correlation. **VarQ ATAC** and **VarQ RNA** are variance quantiles for peak accessibility and gene expression; higher values indicate greater variation across cells and are not P values.

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

The Search page contains four expandable cards. Each card has its own input, settings, summary, result table, and download action. Search results remain in the card that produced them. Changing the page size changes only the visible page; it does not rerun the search or reduce the downloaded result.

### 4.1 Gene Search

Gene Search finds samples in which at least one submitted gene occurs as the selected marker signal type. It does not require every submitted gene to be present in the same sample.

**How to run the search:**

1. Paste gene symbols or upload a supported file.
2. Check the Input genes, Valid genes, and Invalid tokens counters. Input genes counts parsed tokens, while Valid genes counts unique symbols that pass validation; duplicates are removed from the submitted list.
3. Choose the marker signal type and, if needed, one tissue.
4. Choose the initial sort field and page size, then select **Search**.
5. Use the column headers to change the global order of the returned rows, or download the complete result as CSV.

**Input rules:**

- Maximum: **200 gene entries**.
- Textarea separators: line break, comma, space, or semicolon.
- Upload formats: **`.txt`, `.csv`, `.tsv`**.
- CSV and TSV files may contain a recognized gene column such as `gene`, `gene_symbol`, `symbol`, `gene_name`, `hgnc_symbol`, or `marker_gene`.
- Gene symbols are converted to uppercase and duplicate symbols are removed.
- Invalid tokens are reported and excluded from the submitted list. More than 200 parsed gene entries disables the Search button; the list is never silently truncated.

**Parameters:**

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Sort by | Sets the initial order of the complete returned sample list. | Dataset ID; Cell counts; Matched genes when more than one valid gene is submitted |
| Per page | Controls only how many rows are visible on one page. | 10, 20, or 50 |
| Signal Type | Chooses which independently derived marker-gene evidence is searched. | Gene expression markers by default; Gene score markers |
| Tissue | Restricts results to one standardized tissue. | All tissues by default; one searchable tissue value |

**Summary and result columns:**

| Field | Meaning |
|-------|---------|
| Matched samples | Number of distinct samples containing at least one submitted gene as the selected marker type. |
| Marker records | Total matching marker-gene records across all returned samples. One sample can contribute multiple records. |
| Dataset ID | Sample-level OSCAR identifier. Select it to open Sample Details. |
| Tissue | Standardized tissue assigned to the sample. |
| Sample name | Descriptive sample name stored in OSCAR. |
| Platform | Sequencing platform or assay platform recorded for the sample. |
| Source ID | Identifier or label from the original data source. |
| Cells | Total number of cells stored for the sample, not the number matching the input genes. |
| Disease | Disease or control status recorded for the sample. |
| Matched genes | Number of submitted genes with marker evidence in this sample. This column appears only when more than one gene was submitted. |

All visible result columns can be used for global sorting. The CSV action exports every returned sample row in the current result, not only the visible page.

![Search by Gene](../assets/help/search-by-gene.png)

> *Figure 3. Gene Search input, settings, validation, and paginated results.*

### 4.2 Region Search

Region Search finds samples and marker peaks that overlap submitted genomic intervals in one specified OSCAR dataset. All coordinates are interpreted as hg38.

**How to run the search:**

1. Paste genomic regions or upload a supported interval file.
2. Enter the required OSCAR Dataset ID.
3. Choose whether any submitted region or every submitted region must match.
4. Select **Search**, then review both the summary cards and sample-level table.

**Input rules:**

- Maximum: **200 regions**.
- Accepted text: `chr:start-end` or the first three BED columns `chromosome start end`.
- Upload formats: **`.bed`, `.txt`, `.csv`, `.tsv`**.
- Comment, `track`, and `browser` lines in uploaded region files are ignored.
- A **Dataset ID is required**.
- The current search domain is fixed to Integration.

**Parameters:**

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Dataset ID | Required sample-level identifier used to constrain the overlap search. | A valid OSCAR Dataset ID such as `H_000001` |
| Match mode | Any returns a sample when at least one submitted region overlaps a marker peak. All requires every valid submitted region to overlap at least one marker peak in the same sample. | Any input region by default; All input regions |
| Per page | Controls visible table rows only. | 10, 20, or 50 |

Invalid rows are reported. More than 200 regions produces a persistent warning and disables Search. A loading overlay is displayed because a multi-region overlap search may take longer than the other Search modes.

**Summary fields:**

| Field | Meaning |
|-------|---------|
| Matched samples | Number of returned samples satisfying the selected match mode. |
| Matched input regions | Number of submitted regions having at least one marker-peak overlap, shown over the total number of valid input regions. |
| Overlapping peaks | Number of distinct OSCAR marker peaks overlapping the submitted regions. |
| Linked marker genes | Number of distinct marker genes connected to the overlapping peaks by OSCAR P2G evidence. |

**Result columns:**

| Column | Meaning |
|--------|---------|
| Dataset ID | OSCAR sample containing the overlap. |
| Tissue | Standardized sample tissue. |
| Sample name | Stored sample name. |
| Matched regions | Number of submitted regions with an overlap in this sample, shown over the number submitted. |
| Overlapping peaks | Number of distinct marker peaks overlapping the input in this sample. |
| Linked marker genes | Number of distinct marker genes linked to those overlapping peaks. |
| Data Type | Evidence represented by the row, such as ATAC marker-peak evidence, P2G evidence, or both. |

Sortable headers order the complete returned sample list before pagination. The CSV action downloads all returned rows.

![Search by Genome Region](../assets/help/search-by-region.png)

> *Figure 4. Region Search input, dataset selection, settings, and results.*

### 4.3 Tissue Search

Tissue Search accepts one standardized tissue at a time. Select a value from the searchable dropdown; arbitrary free text and multi-tissue input are not used.

The **TOP 12 Tissues** chart ranks tissues by sample count. Clicking a chart segment selects that tissue and starts the same search as the dropdown.

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Tissue | Standardized tissue to search. | One value from the searchable list |
| Sort by | Sets the initial global order of the returned samples. | Dataset ID by default; Cell Counts |
| Per page | Controls visible table rows only. | 10, 20, or 50 |

The result table contains Dataset ID, Tissue, Sample Name, Cells, Platform, Source ID, Disease, and Sample Source. **Cells** is the total sample cell count; **Sample Source** describes the anatomical or source label stored for the sample and can differ from the standardized Tissue value. Dataset ID and Cells can also be sorted from their table headers. The CSV action exports all returned rows. The chart download action exports the TOP 12 chart as PNG, PDF, or editable SVG.

![Search by Tissue Type](../assets/help/search-by-tissue.png)

> *Figure 5. Searchable single-tissue selection and tissue results.*

### 4.4 Cell Type Search

Cell Type Search accepts one standardized cell type at a time. Start typing in the searchable dropdown and select one of the stored cell-type names. This avoids spelling and alias mismatches.

The **TOP 12 Cell Types** chart ranks standardized cell types by sample coverage. Clicking a chart segment selects that cell type and starts the search.

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Cell type | Required standardized cell type to search. | One value from the searchable list |
| Tissue | Optional standardized tissue restriction. After a cell type is selected, only tissues containing at least one matching sample are offered. | All matching tissues by default; one valid tissue |
| Sort by | Sets the initial global order. Matched cell count orders samples by the number of cells assigned to the selected cell type even though that metric is not a visible table column. | Dataset ID by default; Cell Counts; Matched cell count |
| Per page | Controls visible table rows only. | 10, 20, or 50 |

**Matched samples** is the number of samples containing the selected standardized cell type under the current tissue restriction. The result table contains Dataset ID, Tissue, Sample Name, Cells, Platform, Source ID, Disease, and Sample Source with the same meanings described for Tissue Search. Dataset ID and Cells can be sorted from their table headers. The CSV action exports all returned rows, and the TOP 12 chart can be exported as PNG, PDF, or editable SVG.

![Search by Cell Type](../assets/help/search-by-cell-type.png)

> *Figure 6. Searchable standardized cell-type selection and cell-type results.*

### 4.5 Search result navigation

Clicking a Dataset ID opens Sample Details. The button in the upper-right corner of Sample Details returns to the page that opened it: Search, Browse, Home, or Download. Sample Details and Gene/Peak Details are standalone detail pages, so no main navigation item is highlighted while they are open.

Search-table sorting is applied to the complete result array returned by the search and then paginated; it is not limited to the current page. A control explicitly named **Maximum returned records** is different: that control can cap the upstream result set before a table is displayed.

## 5. Browse

The Browse page lists OSCAR samples in a server-paginated table.

- Use Biosample type and Tissue Type facets to filter the table.
- Active filters appear as removable chips.
- Use the keyword box to search the available sample metadata.
- Sort supported columns from the table header.
- Click a Dataset ID to open Sample Details.
- Select **CSV** to download every sample in the current filtered and sorted result, not only the visible page.

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
- In P2G tables, **Gene marker type** indicates expression-marker or gene-score-marker evidence. The following **ABC support** column shows independent Activity-by-Contact support and must not be interpreted as the gene marker type itself.
- The **All P2G links** table uses the ArchR Correlation, Link score, FDR, VarQ RNA, and VarQ ATAC meanings defined in Section 2.3.
- Full downloads are prepared by the download endpoint and do not depend on the current visible table page.

During a long download, the annotation module is locked and displays progress feedback. The main navigation remains usable. If the user leaves and later returns to the same Sample Details page while the download is still running, the download state remains visible.

![Peak-to-Gene link table](../assets/help/p2g-link-table.png)

> *Figure 10. Sample regulatory annotation filters, paginated P2G table, and download action.*

### 6.4 Regulatory network

The Integration view can display a Peak-to-Gene regulatory network. Network controls, filters, and downloads apply to the current sample and domain. **Graph visible links** contains exactly the links currently drawn: search, focus, and expansion can change that set, while table pagination does not. Its CSV download includes every currently visible link.

## 7. Gene Details and Peak Details

Gene Details and Peak Details are opened from four current interface paths:

- A gene row in Sample Details regulatory annotation.
- A peak row in Sample Details regulatory annotation.
- A peak in the Sample Details regulatory network.
- A result link from Cell Enrichment or Sequence-based analysis.

The upper-right return button uses the recorded source. It returns to Sample Details for sample-origin links and to the appropriate Analysis results for analysis-origin links. Direct links without a recorded source use the browser history.

### 7.1 Overview

The Overview module summarizes how often the selected gene or exact peak interval occurs in OSCAR marker records. These are occurrence summaries, not expression-level, effect-size, or significance rankings.

| Summary | How it is calculated | What users can learn |
|---------|----------------------|----------------------|
| Top 10 datasets for a gene | Adds up the gene's marker records within each dataset and ranks datasets by count. | Shows which datasets report the gene most often as a marker, not where expression is highest. |
| Top 10 datasets for a peak | Counts marker records for the exact displayed hg38 interval within each dataset and ranks datasets by count. | Shows which datasets report the peak most often, not where accessibility is highest. |
| Top 10 cell types | Adds up the gene's or exact peak's marker records for each mapped major cell type and ranks cell types by count. | Shows which cell types report the marker most often. `Unknown` means no major cell type was mapped. |

The first 10 groups are shown; ties are ordered alphabetically. Gene Details also shows available expression profiles by platform.

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

Gene regulatory-annotation filenames distinguish the gene, selected gene-region mode, selected reference annotation, and real download time. This prevents downloads from different Promoter, Super Enhancer, Typical Enhancer, or reference-track selections from overwriting one another or becoming indistinguishable later.

## 8. Analysis

The Analysis page contains three independent modules. Each module validates input before submission, reports progress, keeps its completed result in the current browser view while the user inspects detail pages, and provides downloads for the current result type. Once the browser receives a completed asynchronous result, the server-side job result is released; a timed cleanup removes any completed result that was never acknowledged. Parameters labelled as page size affect display only; parameters labelled **Maximum returned records** can make an analysis result incomplete by design.

### 8.1 Cell Enrichment Analysis

Cell Enrichment asks whether an input gene set overlaps a selected dataset's Integration gene-expression marker sets more often than expected by chance. OSCAR uses a hypergeometric test for each eligible marker set and then applies Benjamini-Hochberg correction across the tested sets.

**Step-by-step workflow:**

1. Paste or upload a list of human gene symbols.
2. Select a tissue, then select exactly one dataset from that tissue.
3. Choose Cell type or Cluster as the biological grouping level.
4. Open Advanced settings only if the minimum required overlap must be changed.
5. Select **Run enrichment**, then inspect the input coverage, significance summary, charts, and complete results table.

**Input:**

- Maximum: **200 gene entries**.
- Upload formats: **`.txt`, `.csv`, `.tsv`**.
- The same gene-column recognition, uppercase conversion, duplicate removal, and symbol validation used by Gene Search are applied.
- The input counters distinguish all parsed input genes, genes found in the selected dataset's marker universe, and unmatched genes.

**Parameters:**

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Tissue | Filters the dataset selector and defines the tissue context of the analysis. | Required; one standardized tissue |
| Dataset | Supplies the marker-gene universe and groups tested by the analysis. Datasets are not pooled. | Required; exactly one dataset from the selected tissue |
| Result level | Cell type combines clusters sharing the same standardized cell type within the selected dataset before testing. Cluster tests each stored cluster separately. | Cell type by default and recommended; Cluster |
| Marker reference | Marker signal used for the test. | Fixed to Integration gene-expression markers; gene-score markers are not mixed into this analysis |
| Minimum overlap | Excludes a group when fewer than this many input genes occur in its marker set. Increasing it removes weak, small-overlap results but does not change the input list. | 1 by default; integer from 1 to 100 |
| FDR method | Multiple-testing correction applied to the collection of raw hypergeometric P values. | Fixed to BH, Benjamini-Hochberg |

**Result summary and views:**

| Field or view | Meaning |
|---------------|---------|
| Input genes | Number of valid unique genes submitted to the completed run. |
| Matched genes | Number of submitted genes found in the selected dataset's Integration marker-gene universe. |
| Significant results | Number of returned groups with BH FDR at or below 0.05. |
| Top enriched result | Returned cell type or cluster with the strongest ranked enrichment result. |
| Overview | Up to the top 8 results ordered by lowest FDR. Bar length is minus log10 FDR, so longer bars indicate stronger statistical evidence. |
| Bubble chart | Up to the top 30 ranked results. Position, size, and colour summarize enrichment strength, overlap, and significance as labelled in the chart legend. |
| Results table | Complete returned enrichment result, globally sortable and displayed 10 rows per page. |

**Results-table columns:**

| Column | Meaning |
|--------|---------|
| Rank | Position after the enrichment results are ranked. |
| Cell type | Standardized cell type tested in Cell type mode. |
| Cell type / Cluster | Standardized cell type and stored cluster tested in Cluster mode. |
| Overlap | Number of input genes found in the marker set represented by this row. |
| Overlap genes | The shared gene symbols. Selecting a gene opens Gene Details and preserves the enrichment result for return navigation. |
| Enrichment fold | Observed overlap divided by overlap expected by chance. Values above 1 indicate enrichment relative to the background. |
| P value | Raw hypergeometric-test probability before multiple-testing correction. Smaller values indicate less compatibility with chance overlap. |
| FDR | Benjamini-Hochberg adjusted P value. FDR at or below 0.05 is counted as significant in the summary. |
| Dataset count | Number of OSCAR datasets contributing to the returned group as reported by the analysis result. |

The Overview download exports its displayed overview data, the Bubble chart can be exported as PNG, PDF, or editable SVG, and the Table download exports all globally sorted table rows rather than only the current page.

![Cell Enrichment Analysis](../assets/help/analysis-cell-enrichment.png)

> *Figure 14. Cell Enrichment input, settings, and results.*

### 8.2 Sequence-based Peak Regulatory Analysis

This analysis maps one DNA sequence to hg38 with BLAST, ranks candidate genomic loci, selects one candidate, and then retrieves overlapping OSCAR marker peaks and/or P2G links. BLAST mapping and OSCAR evidence retrieval are separate stages: changing Reference scope does not change where BLAST maps the sequence.

**Step-by-step workflow:**

1. Paste one plain DNA sequence or one FASTA record, or upload a supported file.
2. Review sequence length, valid A/C/G/T bases, GC percentage, and ambiguous N bases.
3. Choose the OSCAR evidence scope and result content.
4. Use Auto BLAST unless the biological question requires a specific BLAST task.
5. Run the analysis, inspect the mapping status, and compare candidate loci.
6. Select a different candidate locus when needed; OSCAR evidence is refreshed for that locus without changing the original sequence.

**Input:**

- Minimum: **10 bases**.
- Maximum: **20,000 bases (20 kb)** after FASTA headers and whitespace are removed.
- Allowed sequence characters: `A`, `C`, `G`, `T`, and `N`.
- Upload formats: **`.fasta`, `.fa`, `.txt`**.

**Main parameters:**

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Genome build | Reference genome used by BLAST and all displayed genomic coordinates. | Fixed to hg38 |
| Reference scope | Determines which OSCAR datasets are searched after a locus is selected. It does not alter BLAST. | All OSCAR datasets by default; Single OSCAR dataset |
| Dataset | Required only for Single OSCAR dataset scope. | One OSCAR Dataset ID selected in the popup |
| Show results | Chooses which evidence queries and result tabs are produced. | P2G links + Marker peaks by default; P2G links only; Marker peaks only |

**Advanced BLAST and evidence parameters:**

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| BLAST task | Auto uses `blastn-short` at 50 bp or shorter. For longer sequences it tries `megablast` first and retries with the more sensitive `blastn` only when megablast finds no hit. A manually chosen task has no automatic fallback. | Auto by default; `megablast`; `blastn`; `blastn-short` |
| Max target sequences | Maximum number of reference subjects or targets BLAST may retain. It is not a maximum number of final distinct loci. | 500 by default; integer of at least 1 |
| Max HSPs per target | Maximum number of local high-scoring segment pairs retained for one BLAST target. | 200 by default; integer of at least 1 |
| E-value cutoff | Largest BLAST expectation value accepted. Smaller values are more stringent. | 10.0 by default; minimum accepted input `1e-12` |
| Flanking region | Bases added to both sides of the selected alignment before OSCAR evidence is searched. It does not change the BLAST alignment. | 0 bp by default; 0 to 1,000,000 bp |
| Maximum returned records | Optional cap on OSCAR evidence rows, not BLAST candidates. When both evidence types are requested, the cap is applied independently to each evidence query. | Empty by default for all matching evidence; positive integer |

The mapping status summarizes how confidently the application can choose a locus: **No hit** means BLAST returned no accepted alignment; **Partial** means mapping evidence is incomplete; **Unique** means one clear candidate was found; **Best supported** means one candidate was selected from multiple candidates using the ranking criteria; and **Ambiguous** warns that near-equivalent candidates remain. An ambiguous result should be reviewed before biological interpretation.

**Summary and selected-candidate fields:**

| Field | Meaning |
|-------|---------|
| BLAST hits | Number of BLAST alignments returned under the selected settings. |
| Mapped genomic regions | Number of distinct hg38 candidate loci retained after classification and ranking. |
| Overlapping peaks | Number of OSCAR peaks overlapping the selected candidate interval, including the configured flank. |
| Linked genes | Number of unique genes linked to returned peaks for the selected candidate. |
| Selected region | hg38 interval currently used for OSCAR evidence retrieval. |
| Strand | Alignment orientation on hg38. |
| Identity | Percentage of identical bases inside the local alignment. |
| Align length | Number of aligned query/reference positions. |
| Query coverage | Percentage of the cleaned input sequence covered by the alignment. |
| E-value | Expected number of similarly strong chance matches; smaller is stronger. |
| Bit score | Normalized BLAST alignment score; larger is stronger. |

**Mapped genomic regions columns:**

| Column | Meaning |
|--------|---------|
| Rank | Candidate order, strongest candidate first. |
| Candidate region | hg38 coordinates of the candidate alignment. |
| Strand | Forward or reverse alignment orientation. |
| Identity | Percentage of aligned bases that are identical. |
| Query coverage | Percentage of the input sequence included in the alignment. |
| E-value | BLAST chance expectation for the match. |
| Bit score | BLAST strength score. |
| Top-score ratio | Candidate bit score divided by the best candidate's bit score. Values near 1 identify near-equivalent candidates. |
| Select | Chooses this locus and refreshes OSCAR evidence for it. |

**Evidence-table columns:**

| Result table | Columns and interpretation |
|--------------|----------------------------|
| All overlapping peaks | Dataset; Peak region in hg38; Source indicating P2G, Marker, or Both; Linked genes; FDR from the strongest available link; Link score from the strongest available link. Peaks with the same coordinates are merged in this overview. |
| P2G links | Dataset; Peak region; Linked gene; ArchR peak-accessibility/gene-expression Correlation; link FDR; absolute-correlation Link score. A marker badge identifies a peak also found in marker-peak evidence. |
| Marker peaks | Dataset; marker-analysis Domain; Cluster; Peak region; Linked genes when available; best linked P2G FDR; best linked P2G Link score. |

Each result tab displays 10 rows per page and downloads all rows belonging to that tab as CSV. If **Maximum returned records** was used, the download contains the capped evidence result and the interface displays a truncation warning.

Results include BLAST candidates and only the selected OSCAR result type or types. When both result types are selected, an additional combined overlapping-peak overview is shown. Each result table is paginated. Selecting another BLAST hit refreshes the OSCAR records for that hit.

![Sequence-based Peak Regulatory Analysis](../assets/help/analysis-sequence-peak.png)

> *Figure 15. Sequence mapping settings, progress, and regulatory results.*

### 8.3 Peak-to-Gene Linkage Analysis

This analysis intersects an input peak set with stored P2G peak intervals and then retains links to the submitted genes. It can return general P2G associations or require marker context in the same dataset and cell type/cluster.

**Step-by-step workflow:**

1. Enter 1 to 100 hg38 peak regions and 1 to 100 human gene symbols.
2. Correct duplicate or invalid entries reported by either input panel.
3. Select a required tissue and optionally one dataset.
4. Choose whether marker evidence is required and which result level to display.
5. Run the analysis, then inspect the complete matched-set summary, table, charts, and network.

**Load sample** fills the GBM example, selects Brain, sets **Reference mode** to **P2G links + marker**, and sets **Result type** to **Cell type**.

**Peak input:**

- Maximum: **100 regions**.
- Accepted text: `chr:start-end` or the first three BED columns.
- Upload formats: **`.bed`, `.txt`**.
- Duplicate or malformed regions must be corrected before the analysis can run.

**Gene input:**

- Maximum: **100 gene entries**.
- Upload formats: **`.txt`, `.csv`**.
- Gene symbols may be separated by line breaks, commas, spaces, or semicolons; they are converted to uppercase. Duplicate or invalid symbols must be corrected.

**Parameters:**

| Parameter | Meaning | Default or allowed values |
|-----------|---------|---------------------------|
| Tissue | Restricts the search to datasets assigned to one standardized tissue. | Required |
| Dataset | Further restricts the analysis to one sample. | Empty by default for all datasets in the selected tissue; one Dataset ID |
| Reference mode | P2G links only matches by region and gene. P2G links + marker also requires peak and gene marker evidence from the same dataset and cell type/cluster. | P2G links only initially; Load sample selects P2G links + marker |
| Result type | General returns unique links. Cell type keeps the marker cell type/cluster. | General initially; Load sample selects Cell type |
| Minimum overlap | Minimum number of base pairs shared by an input region and a stored P2G peak interval. | 1 bp by default; integer of at least 1 |
| Maximum returned records | Optional cap applied only after all matches are found and ranked. Summary cards continue to describe the complete matched set, so totals can exceed displayed rows. | Empty by default for all rows; positive integer |

**Summary fields:**

| Field | Meaning |
|-------|---------|
| P2G links | Number of unique general P2G links found before the optional returned-record cap. |
| Matched records | Number of marker-supported context records found in Cell type mode before the cap. |
| Datasets | Number of distinct OSCAR datasets in the complete matched set. |
| Linked peaks | Number of distinct P2G peak regions in the complete matched set. |
| Linked genes | Number of distinct genes in the complete matched set. |
| Cell types | Number of distinct cell types or clusters in a marker-supported result. |
| Top cell type | Cell type or cluster contributing the largest number of matched context records. |

**Results-table columns:**

| Column | Meaning |
|--------|---------|
| Peak | Matched P2G peak interval in hg38. A badge indicates marker-peak support where applicable. |
| Gene | Submitted gene linked to the matched peak. Marker badges distinguish expression and gene-score marker evidence. |
| Cell type | Cell type or cluster supporting the marker-context record; shown only for Cell type results. |
| Dataset | OSCAR sample containing the result. |
| Link score | Absolute ArchR peak–gene correlation. Larger values indicate a stronger association regardless of direction but are not proof of causality. |
| Link FDR | ArchR false discovery rate for the peak–gene correlation. Smaller values indicate stronger statistical evidence. |

General results provide Table and Peak-Gene network tabs. Cell-type results additionally provide a Cell type chart and Bubble heatmap. Table sorting is applied before the fixed 10-row pagination. The table CSV includes all returned rows and also records Peak name, Marker peak status, and Gene marker types.

The network view displays no more than 30 Peak nodes per Gene and arranges nodes from left to right. Its download menu provides: the current rendered network as PNG, PDF, or editable SVG; **Graph visible links** as CSV for the links currently represented in the network; and the complete returned analysis rows as CSV. Image export reflects the current network view, whereas the complete-row download is not limited to visible network nodes.

![Peak-to-Gene Linkage Analysis](../assets/help/analysis-peak-gene-linkage.png)

> *Figure 16. Peak-to-Gene input, reference settings, results, and network view.*

## 9. Download

The Download page provides searchable sample-level exports, a recoverable multi-sample download queue, and three database-wide additional resources. Sample-file downloads are generated for the requested Dataset ID, domain, data type, and format; additional resources are preassembled files downloaded directly from their resource cards.

### 9.1 Finding and selecting samples

Use **Select field** to search by Tissue type, Biosample type, or Sample ID. If no field is selected, the keyword is compared with Dataset ID, biosample type, sample name, tissue, disease, platform, Source ID, and Sample Source.

Every metadata column has the same helper meaning used in Data Browse. Sorting from a table header orders the complete filtered sample list before pagination; it is not a current-page-only sort.

There are two intentionally different selection controls:

- The checkbox in the table header selects or clears only the rows on the current page.
- **Select all (N)** selects every sample matching the current filters across all pages. When all filtered rows are selected, it changes to **Clear filtered (N)**.

Selections are preserved while the user changes pages. Narrow the filters before using Select all when only a biological subset is required.

### 9.2 Single-sample files

Click **Files** in a sample row. Available files are grouped by data domain and signal type. Each button is disabled while its file is being prepared and displays visible progress feedback.

| Domain | Available sample-level files |
|--------|------------------------------|
| Integration | Gene expression marker genes; Gene score marker genes; Marker peaks; P2G links with marker support; All P2G links |
| RNA | Marker genes |
| ATAC | Gene score marker genes; Marker peaks |

Marker-gene signal types remain separate: selecting gene expression does not include gene-score markers, and selecting gene score does not include expression markers. The exact list depends on the selected sample and domain. Raw matrices and H5AD files are not offered by these sample file buttons.

### 9.3 Multi-sample Download cart

The cart applies one chosen domain, data type, and format to every selected sample. Files are requested sequentially in automatic batches of up to **10 samples**; selecting 20 or 50 rows does not send 20 or 50 simultaneous backend requests.

1. Select samples using current-page checkboxes or Select all for the filtered result.
2. Open **Download cart** and choose one available file type and TSV or CSV format.
3. If more than 10 samples are selected, complete the server-issued human-verification challenge. The resulting authorization is valid for 24 hours and is limited to the selected Dataset IDs and file type.
4. The progress area shows the current sample, batch, and failures. Select **Pause** to stop safely; Resume restarts the unfinished file from the beginning.
5. The next batch starts automatically after the preceding batch; no manual reconnection is required.

The browser saves the queue in session storage. After a pause or refresh, **Resume** puts failed and unfinished samples back in their original order, starting with the first incomplete file. Successful files are not requested again. **Retry failed** is available when only failed items remain, and **Discard** removes the saved queue, clears the selection, and returns the cart to its initial empty state. This is a browser checkpoint, not a server-side download job.

### 9.4 File formats and P2G fields

| File type | Format | Intended use |
|-----------|--------|--------------|
| Marker genes | TSV or CSV | Sample-level marker records for the explicitly selected expression or gene-score signal type. |
| Marker peaks | TSV or CSV | Sample-level differentially accessible peak records. |
| P2G links with marker support | TSV or CSV | P2G records whose peak and gene have the required marker context. |
| All P2G links | TSV or CSV | Stored P2G records without requiring marker status at both ends. |

All P2G downloads retain the ArchR **Link score**, **Correlation**, and **Link FDR** fields described above; the all-links file also retains **VarQ RNA** and **VarQ ATAC**. When **ABC support** is present, `1` means supported and `0` means evaluated but unsupported. Downloads keep these numeric values instead of replacing them with display labels.

TSV is convenient for command-line and statistical workflows because tabs do not conflict with commas inside text fields. CSV is convenient for spreadsheet software. Both formats represent the same selected file type; changing the format does not change the biological filtering.

### 9.5 Additional Resources

The three cards below the sample table are database-wide resources and do not depend on table selection:

| Resource | Format | Contents and use |
|----------|--------|------------------|
| Epi(genetic) Annotation | TAR.GZ | Compressed reference-track archive used for broad regulatory annotation workflows. Extract the archive with a TAR-compatible tool before using the contained annotation files. |
| Tissue/cell type-specific marker Peak-to-Gene links · Gene score | TSV | Marker-supported P2G relationships defined with gene-score marker evidence, organized for tissue or cell-type-level downstream analysis. |
| Tissue/cell type-specific marker Peak-to-Gene links · Gene expression | TSV | Marker-supported P2G relationships defined with gene-expression marker evidence, organized for tissue or cell-type-level downstream analysis. |

The Epi(genetic) Annotation archive contains 15 annotation categories: **Risk SNP, Common SNP, GTEx eQTL, TFBS, Enhancer, Super Enhancer, Methylation, CRISPR, ATAC, 3D interactions, DNase peaks, TAD, eRNA, TF-Chip-Seq, and T(co)F**. These are reference annotations, not sample-level OSCAR marker calls. Users should inspect each extracted file's header and coordinate fields before joining it to their own hg38 regions.

![Download page](../assets/help/download-page.png)

> *Figure 17. Download filtering, current-page and all-filtered selection, Files dialog, recoverable cart queue, and Additional Resources.*

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
| Download cart | No fixed selection cap in the interface | Not applicable | More than 10 selections require human verification; files run sequentially in automatic batches of up to 10 |

An over-limit input is never silently truncated. The page displays a styled warning containing the observed count and the maximum, highlights the corresponding input, and prevents the Search or Analysis request from starting.

## 11. Interpretation Notes

### 11.1 Interpreting markers

A marker belongs to the sample, cell type or cluster, and analysis in which it was detected. It should not be interpreted as universally specific to one cell type.

### 11.2 Interpreting Peak-to-Gene links

P2G links are predicted regulatory associations. Interpret the link score, correlation, FDR, marker support, ABC support, cell type, and dataset together. ABC support is an additional evidence flag, not proof that the peak causally regulates the gene. Experimental validation is required to establish causality.

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

### Why can an Analysis summary be larger than its result table?

In Peak-to-Gene Linkage Analysis, **Maximum returned records** is applied after the complete set has been matched and ranked. Summary cards describe that complete matched set, while the table and its CSV contain the capped returned rows. Leave the parameter empty when every matched row is required.

### Does a Marker Peak table show every possible target gene?

The displayed annotation follows the selected table mode and filters. P2G downloads remain separate and provide the corresponding complete link records for the selected file type.

### Are downloaded results limited to the visible page?

No. Table pagination controls only the displayed rows. Full-data download actions request the complete result associated with the selected query or file type.

### What should I do if a multi-sample download is interrupted?

Return to the Download cart in the same browser session and select **Resume**. It starts with the first failed or unfinished sample; completed files are not downloaded again. The interrupted file itself restarts from the beginning.

### Why do ABC support values appear as 0 and 1 in a download?

The interface uses coloured points for quick reading, while downloads keep the stored values: `1` means supported and `0` means evaluated but unsupported.

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

> **Document version:** 1.1
> **Last updated:** 2026-09-15

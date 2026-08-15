<template>
  <div class="spg-root">
    <!-- ─── Description ──────────────────────────────────────────── -->
    <p class="spg-desc">
      Input a DNA sequence, map it to the hg38 genome using BLAST, and identify
      overlapping accessible peaks and linked genes in OSCAR.
    </p>

    <div class="spg-workbench">
      <div class="cte-card spg-builder-card">
        <!-- ── DNA input ───────────────────────────────────── -->
        <section class="cte-builder-section">
          <div class="cte-card-title">DNA sequence input <span class="cte-max-badge">MAX input: 20 kb</span></div>

          <div v-if="displayedInputError" class="input-feedback-card" role="alert">
            <span class="input-feedback-card__icon" aria-hidden="true">!</span>
            <span class="input-feedback-card__body">
              <strong>Check your DNA sequence</strong>
              <span>{{ displayedInputError }}</span>
            </span>
            <button v-if="!sequenceLimitExceeded" type="button" class="input-feedback-card__close" aria-label="Dismiss input message" @click="clearInputError">&times;</button>
          </div>

          <textarea
            v-model="sequenceText"
            class="cte-textarea"
            :class="{ 'cte-textarea--error': displayedInputError }"
            rows="5"
            placeholder=">sequence_1 ACTGACTGACTGACTG..."
            :disabled="loading"
            @input="onSeqInput"
          ></textarea>

          <p class="cte-hint">
            Paste a human DNA sequence in plain text or FASTA format.
          </p>

          <div class="cte-btn-row">
            <span class="spg-upload-wrap">
              <button type="button" class="soft-btn" :disabled="loading" @click="uploadFasta">
                <span>📎</span> Upload FASTA
              </button>
              <el-tooltip placement="top" effect="light" :show-after="200">
                <template #content>
                  <span class="spg-field-help">
                    <span>Accepted files: .fasta, .fa, and .txt.</span>
                    <span>The file may contain one plain DNA sequence or one FASTA record. Spaces and line breaks are ignored.</span>
                  </span>
                </template>
                <span class="spg-help-icon">?</span>
              </el-tooltip>
            </span>
            <button type="button" class="soft-btn" :disabled="loading" @click="loadExample">
              <span>📋</span> Load example
            </button>
            <button type="button" class="soft-btn" :disabled="loading" @click="clearInput">
              <span>✕</span> Clear
            </button>
          </div>

          <div v-if="seqStats" class="spg-qc-row">
            <div class="spg-qc-card spg-qc-card--muted">
              <span class="spg-qc-label">Sequence length</span>
              <span class="spg-qc-val">{{ seqStats.length }} bp</span>
              <el-tooltip placement="top" effect="light" :show-after="200"><template #content><span class="spg-field-help"><span>Total bases after spaces and FASTA headers are removed.</span><span>A, C, G, T, and N are all included in this number.</span></span></template><span class="spg-qc-help">?</span></el-tooltip>
            </div>
            <div class="spg-qc-card">
              <span class="spg-qc-label">Valid bases</span>
              <span class="spg-qc-val">{{ seqStats.valid }}</span>
              <el-tooltip placement="top" effect="light" :show-after="200"><template #content><span class="spg-field-help"><span>Number of A, C, G, and T bases.</span><span>N bases are not counted here because their exact base is unknown.</span></span></template><span class="spg-qc-help">?</span></el-tooltip>
            </div>
            <div class="spg-qc-card spg-qc-card--info">
              <span class="spg-qc-label">GC content</span>
              <span class="spg-qc-val">{{ seqStats.gcPct }}%</span>
              <el-tooltip placement="top" effect="light" :show-after="200"><template #content><span class="spg-field-help"><span>Percentage of G and C among A, C, G, and T bases.</span><span>N bases are excluded from this calculation.</span></span></template><span class="spg-qc-help">?</span></el-tooltip>
            </div>
            <div class="spg-qc-card spg-qc-card--bad">
              <span class="spg-qc-label">Ambiguous bases</span>
              <span class="spg-qc-val">{{ seqStats.ambig }}</span>
              <el-tooltip placement="top" effect="light" :show-after="200"><template #content><span class="spg-field-help"><span>Number of N bases whose exact identity is unknown.</span><span>Other unsupported characters are rejected before the analysis can run.</span></span></template><span class="spg-qc-help">?</span></el-tooltip>
            </div>
          </div>
        </section>

        <!-- ── Settings ───────────────────────────────────── -->
        <section class="cte-builder-section cte-settings-card">
        <div class="cte-card-title">Analysis settings</div>

        <div class="cte-settings-body">
          <!-- Main fields (left sub-column) -->
          <div class="cte-settings-main">
            <div class="cte-fields">
              <label class="cte-field">
                <span class="cte-field-label">Genome build</span>
                <el-select v-model="genomeBuild" class="cte-select" popper-class="oscar-select-popper" size="small" disabled>
                  <el-option label="hg38" value="hg38" />
                </el-select>
              </label>

              <label class="cte-field">
                <span class="cte-field-label">Reference scope</span>
                <div class="cte-ref-scope-row">
                  <el-select v-model="referenceScope" class="cte-select" popper-class="oscar-select-popper" size="small" :disabled="loading" @change="onReferenceScopeChange">
                    <el-option label="All OSCAR datasets" value="all" />
                    <el-option label="Single OSCAR dataset" value="single_dataset" />
                  </el-select>
                  <span v-if="referenceScope === 'single_dataset' && referenceDatasetId" class="cte-ref-chip">
                    <span class="cte-ref-chip-id">{{ referenceDatasetId }}</span>
                    <button type="button" class="cte-ref-chip-x" @click="clearReferenceDataset">&times;</button>
                  </span>
                </div>
              </label>

              <div class="cte-field">
                <span class="cte-field-label cte-field-label--help">
                  Show results
                  <el-tooltip placement="top-start" effect="light" :show-after="200">
                    <template #content>
                      <span class="spg-field-help">
                        <span><strong>Peak-to-gene links:</strong> returns stored links whose peak overlaps the selected hg38 match. The linked gene comes from the P2G table.</span>
                        <span><strong>Marker peaks:</strong> first finds marker peaks overlapping the selected hg38 match, then looks up P2G links with the same dataset, data type, chromosome, start, and end so linked genes can be shown when available.</span>
                        <span><strong>Both:</strong> shows a combined overlapping-peak overview followed by separate P2G-link and marker-peak tables. Choosing only one option does not show tables from the other option.</span>
                        <span>The return limit in Advanced settings is applied to each selected result query. Leave it empty to return all matches.</span>
                      </span>
                    </template>
                    <span class="cte-inline-help-icon" role="button" tabindex="0" aria-label="Show results help">?</span>
                  </el-tooltip>
                </span>
                <el-select v-model="resultContent" aria-label="Show results" class="cte-select" popper-class="oscar-select-popper" size="small" :disabled="loading">
                  <el-option label="Peak-to-gene links + Marker peaks" value="all" />
                  <el-option label="Peak-to-gene links" value="peak_to_gene" />
                  <el-option label="Marker peaks" value="marker_peaks" />
                </el-select>
              </div>
            </div>
          </div>

          <!-- Advanced (right sub-column) -->
          <div class="cte-settings-advanced-col">
            <button
              type="button"
              class="cte-advanced-toggle"
              :class="{ open: advancedOpen }"
              @click="advancedOpen = !advancedOpen"
            >
              <span class="cte-toggle-chev">▸</span>
              Advanced settings
            </button>

            <div v-show="advancedOpen" class="cte-advanced">
              <label class="cte-field">
                <span class="cte-field-label cte-field-label--help">
                  BLAST task
                  <el-tooltip placement="top-start" effect="light" :show-after="200">
                    <template #content>
                      <span class="spg-field-help">
                        <span><strong>Auto:</strong> uses blastn-short for sequences up to 50 bases. For longer sequences, it first tries megablast and automatically retries with blastn only when megablast returns no match.</span>
                        <span><strong>megablast:</strong> provides fast genomic mapping for highly similar sequences, especially long sequences from hg38. It may omit weaker or partial similarities.</span>
                        <span><strong>blastn:</strong> is slower but more sensitive to mismatches and local similarities. Long sequences, particularly repeat-rich sequences approaching 20 kb, can take substantially longer and return many local alignments.</span>
                        <span><strong>blastn-short:</strong> is tuned for short nucleotide queries. Selecting a task manually overrides Auto.</span>
                      </span>
                    </template>
                    <span class="cte-inline-help-icon" role="button" tabindex="0" aria-label="BLAST task help">?</span>
                  </el-tooltip>
                </span>
                <el-select v-model="blastTask" class="cte-select" popper-class="oscar-select-popper" size="small" :disabled="loading">
                  <el-option label="Auto" value="auto" />
                  <el-option label="megablast" value="megablast" />
                  <el-option label="blastn" value="blastn" />
                  <el-option label="blastn-short" value="blastn-short" />
                </el-select>
              </label>
              <div class="cte-field">
                <span class="cte-field-label cte-field-label--help">
                  Max target sequences
                  <el-tooltip placement="top-start" effect="light" :show-after="200">
                    <template #content>
                      <span class="spg-field-help">
                        <span>Limits BLAST subject sequences, not genomic loci.</span>
                        <span>One subject, such as a chromosome or contig, may contain multiple HSPs.</span>
                      </span>
                    </template>
                    <span class="cte-inline-help-icon" role="button" tabindex="0" aria-label="Max target sequences help">?</span>
                  </el-tooltip>
                </span>
                <el-input-number v-model="maxTargetSeqs" aria-label="Max target sequences" class="cte-number" size="small" :min="1" :disabled="loading" />
              </div>
              <div class="cte-field">
                <span class="cte-field-label cte-field-label--help">
                  Max HSPs per target
                  <el-tooltip placement="top-start" effect="light" :show-after="200">
                    <template #content>
                      <span class="spg-field-help">
                        <span>Maximum HSP alignments retained for each BLAST subject.</span>
                        <span>Keeping alternatives is required to assess multi-locus ambiguity.</span>
                      </span>
                    </template>
                    <span class="cte-inline-help-icon" role="button" tabindex="0" aria-label="Max HSPs per target help">?</span>
                  </el-tooltip>
                </span>
                <el-input-number v-model="maxHsps" aria-label="Max HSPs per target" class="cte-number" size="small" :min="1" :disabled="loading" />
              </div>
              <div class="cte-field">
                <span class="cte-field-label cte-field-label--help">
                  E-value cutoff
                  <el-tooltip placement="top-start" effect="light" :show-after="200">
                    <template #content>
                      <span class="spg-field-help">
                        <span>Maximum BLAST E-value accepted by the search.</span>
                        <span>Smaller values require stronger sequence-match results.</span>
                      </span>
                    </template>
                    <span class="cte-inline-help-icon" role="button" tabindex="0" aria-label="E-value cutoff help">?</span>
                  </el-tooltip>
                </span>
                <el-input-number v-model="evalueCutoff" aria-label="E-value cutoff" class="cte-number" size="small" :min="0.000000000001" :step="0.00001" :precision="8" :disabled="loading" />
              </div>
              <label class="cte-field">
                <span class="cte-field-label">Flanking region (bp)</span>
                <el-input-number v-model="flankBp" class="cte-number" size="small" :min="0" :max="1000000" :disabled="loading" />
              </label>
              <label class="cte-field">
                <span class="cte-field-label">Maximum returned records</span>
                <el-input-number v-model="resultLimit" class="cte-number" size="small" :min="1" placeholder="All" :disabled="loading" />
                <small class="cte-field-hint">Leave empty to return all matched results.</small>
              </label>
            </div>
          </div>
        </div>

        <div class="cte-card-actions">
          <button type="button" class="primary-btn" :disabled="loading || !seqStats || !seqStats.valid || sequenceLimitExceeded" @click="runMapping">
            <span v-if="loading" class="btn-spinner"></span>
            {{ loading ? "Mapping…" : "Run sequence mapping" }}
          </button>
          <button type="button" class="soft-btn" :disabled="loading" @click="resetAll">Reset</button>
        </div>
        <div v-if="loading" class="spg-progress-card" role="status" aria-live="polite">
          <div class="spg-progress-head">
            <div>
              <div class="spg-progress-stage">{{ progressStageLabel }}</div>
              <div class="spg-progress-message">{{ progressMessage }}</div>
            </div>
            <span class="spg-progress-value">{{ jobProgress }}%</span>
          </div>
          <div class="spg-progress-track" :class="{ indeterminate: progressStage === 'BLASTING' || progressStage === 'BLAST_FALLBACK' }">
            <div class="spg-progress-fill" :style="{ width: `${jobProgress}%` }"></div>
          </div>
          <div class="spg-progress-steps">
            <span :class="{ done: jobProgress >= 8, active: progressStage === 'PREPARING' }">Validate</span>
            <span :class="{ done: jobProgress >= 72, active: progressStage === 'BLASTING' || progressStage === 'BLAST_FALLBACK' }">BLAST hg38</span>
            <span :class="{ done: jobProgress >= 84, active: progressStage === 'CLASSIFYING' }">Classify loci</span>
            <span :class="{ done: jobProgress >= 100, active: progressStage === 'QUERYING_EVIDENCE' }">Build</span>
          </div>
        </div>
        </section>
      </div>

      <!-- ── How it works ────────────────────────────────── -->
      <div class="spg-side-column">
        <div class="cte-card spg-image-card">
          <img :src="baseUrl + 'images/Sequence-based.jpg'" alt="Sequence analysis" class="spg-slot-img" />
        </div>
        <div class="cte-card cte-how-card">
          <div class="cte-card-title">How it works</div>
          <div class="how-steps">
            <div class="how-step"><span class="how-num">1</span><div class="how-step-body"><strong>Input DNA sequence</strong><span>Provide a DNA sequence in plain text or FASTA format.</span></div></div>
            <div class="how-step"><span class="how-num">2</span><div class="how-step-body"><strong>Map to hg38</strong><span>Use BLAST to locate the sequence in the human hg38 genome.</span></div></div>
            <div class="how-step"><span class="how-num">3</span><div class="how-step-body"><strong>Query overlapping peaks</strong><span>Identify OSCAR accessible peaks overlapping the mapped genomic region.</span></div></div>
            <div class="how-step"><span class="how-num">4</span><div class="how-step-body"><strong>Build selected results</strong><span>Return P2G links, marker peaks, or both according to the selected result option.</span></div></div>
          </div>
          <p class="how-note">
            This module maps user-provided DNA sequences to the hg38 genome and
            links the mapped regions to OSCAR peak-to-gene regulatory information
            and marker peak annotations.
          </p>
        </div>
      </div>
    </div>

    <!-- ── Results ────────────────────────────────────────── -->
    <div v-if="hasResults" class="spg-results">
      <div v-if="result" class="spg-mapping-banner" :class="`status-${result.mappingStatus.toLowerCase()}`">
        <div class="spg-status-badge">{{ mappingStatusLabel }}</div>
        <div class="spg-status-copy">
          <strong>{{ mappingStatusTitle }}</strong>
          <span>{{ result.mappingMessage }}</span>
          <small v-if="result.mappingStatus === 'AMBIGUOUS' || result.mappingStatus === 'BEST_SUPPORTED'" class="spg-ambiguity-rule">
            Ambiguity gate: bit score ≥ {{ (result.query.nearEquivalentScoreRatio * 100).toFixed(0) }}% of the top candidate.
          </small>
        </div>
      </div>

      <div v-if="result?.summary?.candidateSearchLimited || result?.summary?.blastHitsTruncated" class="spg-science-note">
        Candidate discovery reached a configured BLAST or response limit. The displayed alternatives may not be exhaustive.
      </div>
      <div v-if="result?.summary?.evidencePossiblyTruncated" class="spg-science-note">
        Result tables reached the configured limit; displayed counts reflect returned rows only, not a genome-wide total.
      </div>
      <div v-if="evidenceError" class="cte-input-error spg-evidence-error">{{ evidenceError }}</div>

      <div class="spg-summary-row">
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result?.summary?.blastHitCount ?? 0) }}</span><span class="sum-label">BLAST hits</span></div>
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result?.summary?.candidateLocusCount ?? 0) }}</span><span class="sum-label">Mapped genomic regions</span></div>
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result?.summary?.overlappingPeakCount ?? 0) }}</span><span class="sum-label">Overlapping peaks</span></div>
        <div class="cte-summary-card"><span class="sum-num">{{ fmt(result?.summary?.linkedGeneCount ?? 0) }}</span><span class="sum-label">Linked genes</span></div>
      </div>

      <div v-if="selectedHit" class="spg-top-hit-cards">
        <div class="spg-top-heading">
          <div>
            <div class="spg-top-title">Regulatory information for selected BLAST candidate #{{ selectedHit.rank }}</div>
          </div>
        </div>
        <div class="spg-detail-grid">
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.chromosome }}:{{ fmt(selectedHit.start) }}-{{ fmt(selectedHit.end) }}</span><span class="spg-detail-label">Selected region</span></div>
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.strand }}</span><span class="spg-detail-label">Strand</span></div>
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.identity }}%</span><span class="spg-detail-label">Identity</span></div>
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.alignLen }} bp</span><span class="spg-detail-label">Align length</span></div>
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.queryCoverage }}%</span><span class="spg-detail-label">Query coverage</span></div>
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.evalue }}</span><span class="spg-detail-label">E-value</span></div>
          <div class="spg-detail-card"><span class="spg-detail-val">{{ selectedHit.bitScore }}</span><span class="spg-detail-label">Bit score</span></div>
        </div>
      </div>

      <div class="spg-flow">
        <div class="spg-flow-step"><span class="spg-flow-num">{{ seqStats?.length ?? 0 }} bp</span><span class="spg-flow-label">Input sequence</span></div>
        <span class="spg-flow-arrow">→</span>
        <div class="spg-flow-step"><span class="spg-flow-num">{{ selectedRegionLabel }}</span><span class="spg-flow-label">Selected hg38 candidate</span></div>
        <span class="spg-flow-arrow">→</span>
        <div class="spg-flow-step"><span class="spg-flow-num">{{ fmt(result?.summary?.overlappingPeakCount ?? 0) }} loci</span><span class="spg-flow-label">Returned overlaps</span></div>
        <span class="spg-flow-arrow">→</span>
        <div class="spg-flow-step"><span class="spg-flow-num">{{ fmt(result?.summary?.linkedGeneCount ?? 0) }} genes</span><span class="spg-flow-label">Selected-hit genes</span></div>
      </div>

      <div class="cte-tabs-row">
        <div class="cte-tabs">
          <button v-for="tab in resultTabs" :key="tab.key" type="button" class="cte-tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
        </div>
        <el-tooltip content="Download current table" placement="top" effect="light" :show-after="220">
          <button type="button" class="annotation-download-button" :disabled="!activeTableRows?.length" aria-label="Download current table" @click="downloadCurrentTable"><el-icon><Download /></el-icon></button>
        </el-tooltip>
      </div>

      <div class="spg-tab-content" :class="{ 'spg-tab-loading': evidenceLoading }">
        <div v-if="evidenceLoading" class="spg-tab-overlay">
          <span class="btn-spinner dark"></span>
          <span>Updating results, please wait…</span>
        </div>
        <div v-if="activeTab === 'blastHits'" class="cte-table-wrap">
          <table class="cte-table spg-candidate-table">
            <thead><tr><th>Rank</th><th>Candidate region</th><th>Strand</th><th>Identity</th><th>Query coverage</th><th>E-value</th><th>Bit score</th><th>Top-score ratio</th><th>Select</th></tr></thead>
            <tbody>
              <tr v-if="!paginatedBlastRows.length"><td colspan="9" class="cte-no-data">No BLAST candidates found.</td></tr>
              <tr v-for="row in paginatedBlastRows" :key="row.hitId" :class="{ selected: row.hitId === result?.evidenceHitId, equivalent: row.nearEquivalent }">
                <td><span class="spg-rank-pill">#{{ row.rank }}</span></td>
                <td>{{ row.chromosome }}:{{ fmt(row.start) }}-{{ fmt(row.end) }}</td><td>{{ row.strand }}</td><td>{{ row.identity }}%</td><td>{{ row.queryCoverage }}%</td>
                <td>{{ row.evalue }}</td><td>{{ row.bitScore }}</td><td>{{ (row.scoreRatio * 100).toFixed(1) }}%</td>
                <td><button type="button" class="spg-select-hit" :class="{ active: row.hitId === result?.evidenceHitId }" :disabled="evidenceLoading" @click="selectEvidenceHit(row)">{{ row.hitId === result?.evidenceHitId ? 'Selected' : 'Use this locus' }}</button></td>
              </tr>
            </tbody>
          </table>
          <div v-if="totalPages > 1" class="cte-pagination"><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(1)">« First</button><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(currentPage - 1)">‹ Prev</button><span class="cte-page-info">Page <input v-model="pageJumpInput" type="number" class="cte-page-jump" min="1" :max="totalPages" @keyup.enter="onJumpPage" /> / {{ totalPages }}</span><button type="button" class="cte-page-btn" @click="onJumpPage">Go</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(currentPage + 1)">Next ›</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(totalPages)">Last »</button></div>
        </div>

        <div v-if="activeTab === 'allPeaks'" class="cte-table-wrap">
          <table class="cte-table">
            <thead><tr><th class="gsc-sort-th" @click="toggleSort('datasetId')">Dataset <span class="gsc-sort-arrow">{{ sortArrow('datasetId') }}</span></th><th>Peak region</th><th class="gsc-sort-th" @click="toggleSort('source')">Source <span class="gsc-sort-arrow">{{ sortArrow('source') }}</span></th><th class="gsc-sort-th" @click="toggleSort('linkedGenes')">Linked genes <span class="gsc-sort-arrow">{{ sortArrow('linkedGenes') }}</span></th><th class="gsc-sort-th" @click="toggleSort('linkFdr')">FDR <span class="gsc-sort-arrow">{{ sortArrow('linkFdr') }}</span></th><th class="gsc-sort-th" @click="toggleSort('linkScore')">Link score <span class="gsc-sort-arrow">{{ sortArrow('linkScore') }}</span></th></tr></thead>
            <tbody>
              <tr v-if="!paginatedAllPeakRows.length"><td colspan="6" class="cte-no-data">No overlapping peaks found for the selected candidate.</td></tr>
              <tr v-for="(row, i) in paginatedAllPeakRows" :key="(currentPage - 1) * PAGE_SIZE + i">
                <td><span v-for="(ds, idx) in splitDatasets(row.datasetId)" :key="idx"><a v-if="ds" class="spg-peak-link" @click.prevent="goToSampleDetail(ds)">{{ ds }}</a><template v-if="idx < splitDatasets(row.datasetId).length - 1">, </template></span></td>
                <td><a class="spg-peak-link" @click.prevent="goToPeakDetail(row.firstDatasetId, row.domain, row.chromosome, row.peakStart, row.peakEnd)">{{ row.chromosome }}:{{ fmt(row.peakStart) }}-{{ fmt(row.peakEnd) }}</a></td>
                <td><span class="spg-source-badge" :class="'spg-source-' + row.source.toLowerCase()">{{ row.source }}</span></td>
                <td>{{ row.linkedGenes || '—' }}</td>
                <td>{{ row.linkFdr || '—' }}</td>
                <td>{{ row.linkScore || '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="totalPages > 1" class="cte-pagination"><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(1)">« First</button><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(currentPage - 1)">‹ Prev</button><span class="cte-page-info">Page <input v-model="pageJumpInput" type="number" class="cte-page-jump" min="1" :max="totalPages" @keyup.enter="onJumpPage" /> / {{ totalPages }}</span><button type="button" class="cte-page-btn" @click="onJumpPage">Go</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(currentPage + 1)">Next ›</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(totalPages)">Last »</button></div>
        </div>

        <div v-if="activeTab === 'p2g'" class="cte-table-wrap">
          <table class="cte-table">
            <thead><tr><th class="gsc-sort-th" @click="toggleSort('datasetId')">Dataset <span class="gsc-sort-arrow">{{ sortArrow('datasetId') }}</span></th><th>Peak region</th><th class="gsc-sort-th" @click="toggleSort('geneName')">Linked gene <span class="gsc-sort-arrow">{{ sortArrow('geneName') }}</span></th><th class="gsc-sort-th" @click="toggleSort('correlation')">Correlation <span class="gsc-sort-arrow">{{ sortArrow('correlation') }}</span></th><th class="gsc-sort-th" @click="toggleSort('fdr')">FDR <span class="gsc-sort-arrow">{{ sortArrow('fdr') }}</span></th><th class="gsc-sort-th" @click="toggleSort('linkScore')">Link score <span class="gsc-sort-arrow">{{ sortArrow('linkScore') }}</span></th></tr></thead>
            <tbody>
              <tr v-if="!paginatedP2gRows.length"><td colspan="6" class="cte-no-data">No peak-to-gene links found for the selected candidate.</td></tr>
              <tr v-for="(row, i) in paginatedP2gRows" :key="(currentPage - 1) * PAGE_SIZE + i">
                <td><a class="spg-peak-link" @click.prevent="goToSampleDetail(row.datasetId)">{{ row.datasetId }}</a></td>
                <td class="spg-peak-col">
                  <a class="spg-peak-link" @click.prevent="goToPeakDetail(row.datasetId, row.domain, row.chromosome, row.peakStart, row.peakEnd)">{{ row.chromosome }}:{{ fmt(row.peakStart) }}-{{ fmt(row.peakEnd) }}</a>
                  <span v-if="isMarkerPeak(row)" class="spg-marker-badge">marker peak</span>
                </td>
                <td>{{ row.geneName }}</td>
                <td>{{ row.correlation }}</td><td>{{ row.fdr }}</td><td>{{ row.linkScore }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="totalPages > 1" class="cte-pagination"><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(1)">« First</button><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(currentPage - 1)">‹ Prev</button><span class="cte-page-info">Page <input v-model="pageJumpInput" type="number" class="cte-page-jump" min="1" :max="totalPages" @keyup.enter="onJumpPage" /> / {{ totalPages }}</span><button type="button" class="cte-page-btn" @click="onJumpPage">Go</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(currentPage + 1)">Next ›</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(totalPages)">Last »</button></div>
        </div>

        <div v-if="activeTab === 'markerPeaks'" class="cte-table-wrap">
          <table class="cte-table">
            <thead><tr><th class="gsc-sort-th" @click="toggleSort('datasetId')">Dataset <span class="gsc-sort-arrow">{{ sortArrow('datasetId') }}</span></th><th>Domain</th><th class="gsc-sort-th" @click="toggleSort('groupName')">Cluster <span class="gsc-sort-arrow">{{ sortArrow('groupName') }}</span></th><th>Peak region</th><th class="gsc-sort-th" @click="toggleSort('linkedGenes')">Linked genes <span class="gsc-sort-arrow">{{ sortArrow('linkedGenes') }}</span></th><th class="gsc-sort-th" @click="toggleSort('linkFdr')">FDR <span class="gsc-sort-arrow">{{ sortArrow('linkFdr') }}</span></th><th class="gsc-sort-th" @click="toggleSort('linkScore')">Link score <span class="gsc-sort-arrow">{{ sortArrow('linkScore') }}</span></th></tr></thead>
            <tbody>
              <tr v-if="!paginatedMarkerPeakRows.length"><td colspan="7" class="cte-no-data">No marker peaks found for the selected candidate.</td></tr>
              <tr v-for="(row, i) in paginatedMarkerPeakRows" :key="(currentPage - 1) * PAGE_SIZE + i">
                <td><a class="spg-peak-link" @click.prevent="goToSampleDetail(row.datasetId)">{{ row.datasetId }}</a></td><td>{{ row.domain }}</td><td>{{ row.groupName }}</td>
                <td><a class="spg-peak-link" @click.prevent="goToPeakDetail(row.datasetId, row.domain, row.chromosome, row.peakStart, row.peakEnd)">{{ row.chromosome }}:{{ fmt(row.peakStart) }}-{{ fmt(row.peakEnd) }}</a></td>
                <td>{{ markerLinkedGeneLabel(row) }}</td>
                <td>{{ markerBestLinkFdr(row) }}</td><td>{{ markerBestLinkScore(row) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="totalPages > 1" class="cte-pagination"><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(1)">« First</button><button type="button" class="cte-page-btn" :disabled="currentPage <= 1" @click="onPageChange(currentPage - 1)">‹ Prev</button><span class="cte-page-info">Page <input v-model="pageJumpInput" type="number" class="cte-page-jump" min="1" :max="totalPages" @keyup.enter="onJumpPage" /> / {{ totalPages }}</span><button type="button" class="cte-page-btn" @click="onJumpPage">Go</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(currentPage + 1)">Next ›</button><button type="button" class="cte-page-btn" :disabled="currentPage >= totalPages" @click="onPageChange(totalPages)">Last »</button></div>
        </div>

      </div>
    </div>

    <!-- Dataset picker modal -->
    <Teleport to="body">
      <div v-if="datasetPickerVisible" class="cte-modal-overlay" @click.self="onDatasetPickerClose">
        <div class="cte-modal cte-dataset-modal">
          <div class="cte-modal-header">
            <div class="cte-modal-title">Select a dataset</div>
            <button type="button" class="cte-modal-close" @click="onDatasetPickerClose">×</button>
          </div>
          <div v-if="datasetLoading" class="cte-dataset-empty">Loading datasets…</div>
          <div v-else-if="datasetOptions.length === 0" class="cte-dataset-empty">No datasets available.</div>
          <div v-else class="cte-dataset-list">
            <button v-for="ds in datasetOptions" :key="ds.value" type="button" class="cte-dataset-item"
              :class="{ active: referenceDatasetId === ds.value }" @click="selectDataset(ds.value)">
              <span class="cte-dataset-id">{{ ds.value }}</span>
              <span class="cte-dataset-label">{{ ds.label.split(' — ')[1] || ds.label }}</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Hidden file input -->
    <input ref="fileInputRef" type="file" accept=".fasta,.fa,.txt" style="display:none" @change="onFastaSelected" />
  </div>
</template>

<script setup lang="ts">
const baseUrl = import.meta.env.BASE_URL;
import { computed, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
import { useRouter } from "vue-router";
const router = useRouter();
import { Download } from "@element-plus/icons-vue";
import {
  fetchAllDatasets,
  fetchSequencePeak2GeneEvidence,
  fetchSequencePeak2GeneJob,
  submitSequencePeak2GeneJob,
} from "@/api/analysis";
import type {
  BlastHitDto,
  MarkerPeakDto,
  PeakGeneLinkDto,
  SequencePeak2GeneJobResponse,
  SequencePeak2GeneRequest,
  SequencePeak2GeneResponse,
} from "@/api/analysis";

const EXAMPLE_SEQ = `>gata4_eRNA::chr8:11571962-11572135
GCTGCAGGTCACACAGTGAGCAAGAGCCAGAGCTGGAGCTGGCCCCAGGCGCCGCCAGGCTTGTGCCCCGCCCACACCCCTCTGGCCTGGCCTCCTTGCCAGTTgcaggtcatgtcgtagaagagcaggctttggaggagtcaggcagggcttacttcacctcttggttctgc`;

const loading = ref(false);
const sequenceText = ref("");
const MAX_SEQUENCE_BP = 20_000;
const inputError = ref<string | null>(null);
let errorTimer: ReturnType<typeof setTimeout> | undefined;

// settings
const genomeBuild = ref("hg38");
const referenceScope = ref<"all" | "single_dataset">("all");
const referenceDatasetId = ref("");
const resultContent = ref<"all" | "peak_to_gene" | "marker_peaks">("all");
const advancedOpen = ref(false);

// dataset picker
const datasetOptions = ref<{ value: string; label: string }[]>([]);
const datasetLoading = ref(true);
const datasetPickerVisible = ref(false);

async function loadDatasets() {
  try { datasetOptions.value = await fetchAllDatasets(); } catch {}
  finally { datasetLoading.value = false; }
}
loadDatasets();

function onReferenceScopeChange(val: string) {
  if (val === "single_dataset") {
    referenceScope.value = "all";
    datasetPickerVisible.value = true;
    void loadDatasets();
  }
}
function selectDataset(id: string) { referenceDatasetId.value = id; referenceScope.value = "single_dataset"; datasetPickerVisible.value = false; }
function onDatasetPickerClose() { datasetPickerVisible.value = false; if (!referenceDatasetId.value) referenceScope.value = "all"; }
function clearReferenceDataset() { referenceDatasetId.value = ""; referenceScope.value = "all"; }
const blastTask = ref<"auto" | "megablast" | "blastn" | "blastn-short">("auto");
const maxTargetSeqs = ref(500);
const maxHsps = ref(200);
const evalueCutoff = ref(10.0);
const flankBp = ref(0);
const resultLimit = ref<number | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const hasResults = ref(false);
type SequenceResultTab = "p2g" | "allPeaks" | "markerPeaks" | "blastHits";

const activeTab = ref<SequenceResultTab>("blastHits");
const resultTabs = computed(() => {
  const requestedContent = lastRunRequest.value?.resultContent ?? resultContent.value;
  const tabs: Array<{ key: SequenceResultTab; label: string }> = [
    { key: "blastHits", label: "Mapped genomic regions" },
  ];
  if (requestedContent === "all") {
    tabs.push({ key: "allPeaks", label: "All overlapping peaks" });
  }
  if (requestedContent === "all" || requestedContent === "peak_to_gene") {
    tabs.push({ key: "p2g", label: "Peak-to-gene links" });
  }
  if (requestedContent === "all" || requestedContent === "marker_peaks") {
    tabs.push({ key: "markerPeaks", label: "Marker peaks" });
  }
  return tabs;
});

const result = ref<SequencePeak2GeneResponse | null>(null);
const evidenceLoading = ref(false);
const evidenceError = ref<string | null>(null);
const jobProgress = ref(0);
const progressStage = ref("IDLE");
const progressMessage = ref("Waiting to start.");
const lastRunRequest = ref<SequencePeak2GeneRequest | null>(null);
let pollGeneration = 0;

const selectedHit = computed(() => {
  if (!result.value?.blastHits?.length) return null;
  return result.value.blastHits.find(hit => hit.hitId === result.value?.evidenceHitId)
    ?? result.value.blastHits[0];
});
const selectedRegionLabel = computed(() => selectedHit.value
  ? `${selectedHit.value.chromosome}:${fmt(selectedHit.value.start)}-${fmt(selectedHit.value.end)}`
  : "—");
const progressStageLabel = computed(() => ({
  QUEUED: "Queued",
  STARTING: "Starting analysis",
  PREPARING: "Validating sequence",
  BLASTING: "Searching hg38 with BLAST",
  BLAST_FALLBACK: "Retrying with sensitive blastn",
  CLASSIFYING: "Classifying candidate loci",
  QUERYING_EVIDENCE: "Retrieving selected-hit results",
  COMPLETED: "Analysis complete",
  FAILED: "Analysis failed",
} as Record<string, string>)[progressStage.value] ?? "Processing");
const mappingStatusLabel = computed(() => ({
  NO_HIT: "No hit",
  PARTIAL: "Partial mapping",
  UNIQUE: "Unique candidate",
  BEST_SUPPORTED: "Best-supported candidate",
  AMBIGUOUS: "Ambiguous mapping",
} as Record<string, string>)[result.value?.mappingStatus ?? ""] ?? "Mapping result");
const mappingStatusTitle = computed(() => ({
  NO_HIT: "No regulatory location can be assigned.",
  PARTIAL: "The selected alignment covers only part of the query.",
  UNIQUE: "One candidate passed the scientific coverage gate.",
  BEST_SUPPORTED: "The selected locus is stronger, but alternatives exist.",
  AMBIGUOUS: "The input sequence does not map uniquely.",
} as Record<string, string>)[result.value?.mappingStatus ?? ""] ?? "Sequence mapping result");

/* ── merged all overlapping peaks ── */
interface MergedPeakRow {
  datasetId: string; firstDatasetId: string; domain: string; chromosome: string; peakStart: number; peakEnd: number;
  source: string; linkedGenes: string; linkFdr: string; linkScore: string;
}

const allOverlappingPeaks = computed<MergedPeakRow[]>(() => {
  if (!result.value) return [];
  const p2g = result.value.peakGeneLinks ?? [];
  const mps = (result.value.markerPeaks ?? []).filter(p => p.domain === 'integration');
  const map = new Map<string, { datasets: Set<string>; domain: string; genes: Set<string>; fdrs: number[]; scores: number[]; inP2g: boolean; inMarker: boolean }>();

  const coordKey = (r: any) => `${r.chromosome}:${r.peakStart}:${r.peakEnd}`;

  for (const r of p2g) {
    const k = coordKey(r);
    if (!map.has(k)) map.set(k, { datasets: new Set(), domain: r.domain || 'integration', genes: new Set(), fdrs: [], scores: [], inP2g: false, inMarker: false });
    const e = map.get(k)!;
    e.inP2g = true;
    if (r.datasetId) e.datasets.add(r.datasetId);
    if (r.geneName) e.genes.add(r.geneName);
    if (r.fdr != null) e.fdrs.push(r.fdr);
    if (r.linkScore != null) e.scores.push(r.linkScore);
  }
  for (const r of mps) {
    const k = coordKey(r);
    if (!map.has(k)) map.set(k, { datasets: new Set(), domain: r.domain || 'integration', genes: new Set(), fdrs: [], scores: [], inP2g: false, inMarker: false });
    const e = map.get(k)!;
    e.inMarker = true;
    if (r.datasetId) e.datasets.add(r.datasetId);
    for (const link of (r.peakGeneLinks ?? [])) {
      if (link.geneName) e.genes.add(link.geneName);
      if (link.fdr != null) e.fdrs.push(link.fdr);
      if (link.linkScore != null) e.scores.push(link.linkScore);
    }
  }

  return [...map.entries()].map(([k, e]) => {
    const [chromosome, ps, pe] = k.split(':');
    const source = e.inP2g && e.inMarker ? 'Both' : e.inP2g ? 'P2G' : 'Marker';
    const linkedGenes = [...e.genes].sort().join(', ');
    const linkFdr = e.fdrs.length ? Math.min(...e.fdrs).toExponential(2) : '';
    const linkScore = e.scores.length ? Math.max(...e.scores).toFixed(3) : '';
    const dsSorted = [...e.datasets].sort();
    return { datasetId: dsSorted.join(', '), firstDatasetId: dsSorted[0] ?? "", domain: e.domain, chromosome: chromosome ?? "", peakStart: Number(ps), peakEnd: Number(pe), source, linkedGenes, linkFdr, linkScore };
  });
});

/* ── table data ── */
const activeTableRows = computed(() => {
  if (!result.value) return null;
  if (activeTab.value === "p2g") return result.value.peakGeneLinks ?? [];
  if (activeTab.value === "allPeaks") return allOverlappingPeaks.value;
  if (activeTab.value === "markerPeaks") return (result.value.markerPeaks ?? []).filter(p => p.domain === 'integration');
  if (activeTab.value === "blastHits") return result.value.blastHits ?? [];
  return null;
});

const sortColumn = ref<string | null>(null);
const sortDirection = ref<"asc" | "desc">("desc");

/** Marker peak coordinate keys for cross-annotation into the P2G table (integration only). */
const markerPeakKeySet = computed(() => {
  const peaks = result.value?.markerPeaks;
  if (!peaks?.length) return null;
  const set = new Set<string>();
  for (const p of peaks) {
    if (p.domain !== 'integration') continue;
    if (p.chromosome && p.peakStart != null && p.peakEnd != null) {
      set.add(`${p.chromosome}:${p.peakStart}-${p.peakEnd}`);
    }
  }
  return set;
});

const sortedTableRows = computed(() => {
  const rows = activeTableRows.value ? [...activeTableRows.value] : [];
  if (!sortColumn.value || !rows.length) return rows;
  const k = sortColumn.value;
  const dir = sortDirection.value === "desc" ? -1 : 1;

  const resolve = (row: any): string | number => {
    if (k === 'linkedGenes' && row.peakGeneLinks !== undefined) return markerLinkedGeneLabel(row as MarkerPeakDto);
    if (k === 'linkedGenes' && row.source !== undefined) return row.linkedGenes || '';
    if (k === 'linkFdr' && row.peakGeneLinks !== undefined) return markerBestLinkFdrNumeric(row as MarkerPeakDto);
    if (k === 'linkFdr' && row.source !== undefined) return Number(row.linkFdr) || 0;
    if (k === 'linkScore' && row.peakGeneLinks !== undefined) return markerBestLinkScoreNumeric(row as MarkerPeakDto);
    if (k === 'linkScore' && row.source !== undefined) return Number(row.linkScore) || 0;
    if (k === 'source') return row.source || '';
    return row[k] ?? '';
  };

  rows.sort((a: any, b: any) => {
    const va = resolve(a);
    const vb = resolve(b);
    if (typeof va === 'number' && typeof vb === 'number') {
      return dir * (va - vb);
    }
    return dir * String(va).localeCompare(String(vb));
  });
  return rows;
});

/* ── table pagination (all tabs) ── */
const PAGE_SIZE = 10;

const currentPage = ref(1);
const pageJumpInput = ref("1");
watch(activeTab, () => { onPageChange(1); });
const totalPages = computed(() => Math.ceil((sortedTableRows.value?.length ?? 0) / PAGE_SIZE));
const paginatedRows = computed(() => {
  const rows = sortedTableRows.value ?? [];
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return rows.slice(start, start + PAGE_SIZE);
});
const paginatedP2gRows = computed(() => activeTab.value === "p2g" ? paginatedRows.value as PeakGeneLinkDto[] : []);
const paginatedAllPeakRows = computed(() => activeTab.value === "allPeaks" ? paginatedRows.value as MergedPeakRow[] : []);
const paginatedMarkerPeakRows = computed(() => activeTab.value === "markerPeaks" ? paginatedRows.value as MarkerPeakDto[] : []);
const paginatedBlastRows = computed(() => activeTab.value === "blastHits" ? paginatedRows.value as BlastHitDto[] : []);
function markerLinkedGeneLabel(row: MarkerPeakDto): string {
  const genes = [...new Set((row.peakGeneLinks ?? []).map(link => link.geneName).filter(Boolean))];
  return genes.length ? genes.join(", ") : "—";
}
function markerLinkedGenesForCsv(row: MarkerPeakDto): string {
  const genes = [...new Set((row.peakGeneLinks ?? []).map(link => link.geneName).filter(Boolean))];
  return genes.join("; ");
}
function markerBestLinkFdr(row: MarkerPeakDto): string {
  const fdrs = (row.peakGeneLinks ?? []).map(l => l.fdr).filter(f => f != null);
  return fdrs.length ? Math.min(...fdrs).toExponential(2) : "—";
}
function markerBestLinkFdrNumeric(row: MarkerPeakDto): number {
  const fdrs = (row.peakGeneLinks ?? []).map(l => l.fdr).filter(f => f != null);
  return fdrs.length ? Math.min(...fdrs) : 0;
}
function markerBestLinkScore(row: MarkerPeakDto): string {
  const scores = (row.peakGeneLinks ?? []).map(l => l.linkScore).filter(s => s != null);
  return scores.length ? Math.max(...scores).toFixed(3) : "—";
}
function markerBestLinkScoreNumeric(row: MarkerPeakDto): number {
  const scores = (row.peakGeneLinks ?? []).map(l => l.linkScore).filter(s => s != null);
  return scores.length ? Math.max(...scores) : 0;
}
function onPageChange(page: number) {
  currentPage.value = Math.min(Math.max(1, totalPages.value), Math.max(1, page));
  pageJumpInput.value = String(currentPage.value);
}
function onJumpPage() {
  const page = Number.parseInt(pageJumpInput.value, 10);
  if (!Number.isFinite(page)) {
    pageJumpInput.value = String(currentPage.value);
    return;
  }
  onPageChange(page);
}
watch(totalPages, () => { onPageChange(currentPage.value); });

function clearInputError() {
  inputError.value = null;
  if (errorTimer) { clearTimeout(errorTimer); errorTimer = undefined; }
}
function setInputError(msg: string) {
  clearInputError();
  inputError.value = msg;
  errorTimer = setTimeout(() => { inputError.value = null; }, 8000);
}

function validateSeq(text: string): string | null {
  const cleaned = text.replace(/^>.*$/gm, "").replace(/\s/g, "").toUpperCase();
  if (!cleaned) return null;
  const invalid = cleaned.match(/[^ACGTN]/g);
  if (invalid) return `Invalid nucleotide characters: ${[...new Set(invalid)].join(" ")}`;
  if (cleaned.length < 10) return "Sequence too short (minimum 10 bases).";
  if (cleaned.length > MAX_SEQUENCE_BP) return `Sequence is too long. Maximum is ${MAX_SEQUENCE_BP.toLocaleString()} bp.`;
  if (!/[ACGT]/.test(cleaned)) return "Sequence must contain at least one A, C, G, or T base.";
  return null;
}

const seqStats = computed(() => {
  const cleaned = sequenceText.value.replace(/^>.*$/gm, "").replace(/\s/g, "").toUpperCase();
  if (!cleaned) return null;
  const validMatch = cleaned.match(/[ACGT]/g);
  const nMatch = cleaned.match(/N/g);
  const gcMatch = cleaned.match(/[GC]/g);
  const len = cleaned.length;
  const valid = validMatch ? validMatch.length : 0;
  const gc = gcMatch ? gcMatch.length : 0;
  const ambig = nMatch ? nMatch.length : 0;
  return { length: len, valid, gcPct: len ? ((gc / len) * 100).toFixed(1) : "0.0", ambig };
});
const sequenceLimitExceeded = computed(() => (seqStats.value?.length ?? 0) > MAX_SEQUENCE_BP);
const displayedInputError = computed(() => sequenceLimitExceeded.value
  ? `Sequence is too long. Maximum is ${MAX_SEQUENCE_BP.toLocaleString()} bp.`
  : inputError.value);

let validateDebounce: ReturnType<typeof setTimeout> | undefined;
function onSeqInput() {
  // A mapping result is tied to the exact submitted sequence.  Remove the
  // previous result immediately when the input changes so it is never shown
  // beside a different sequence during the validation debounce.
  hasResults.value = false;
  result.value = null;
  lastRunRequest.value = null;
  evidenceError.value = null;
  if (validateDebounce) clearTimeout(validateDebounce);
  validateDebounce = setTimeout(() => {
    const err = validateSeq(sequenceText.value);
    if (err) setInputError(err); else clearInputError();
  }, 500);
}

function loadExample() { sequenceText.value = EXAMPLE_SEQ; }
function clearInput() { sequenceText.value = ""; clearInputError(); }

function uploadFasta() { fileInputRef.value?.click(); }
function onFastaSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  input.value = "";
  if (!/\.(?:fasta|fa|txt)$/i.test(file.name)) {
    setInputError("Unsupported file type. Please choose a .fasta, .fa, or .txt file.");
    return;
  }
  const reader = new FileReader();
  reader.onerror = () => setInputError("Failed to read file.");
  reader.onload = () => {
    sequenceText.value = String(reader.result ?? "");
    const err = validateSeq(sequenceText.value);
    if (err) setInputError(err); else clearInputError();
  };
  reader.readAsText(file);
}

function buildSequenceRequest(): SequencePeak2GeneRequest {
  return {
    sequence: sequenceText.value.replace(/^>.*$/gm, "").replace(/\s/g, ""),
    genomeBuild: genomeBuild.value,
    referenceScope: referenceScope.value,
    datasetId: referenceScope.value === "single_dataset" ? referenceDatasetId.value : null,
    resultContent: resultContent.value,
    blastTask: blastTask.value,
    maxTargetSeqs: maxTargetSeqs.value,
    maxHsps: maxHsps.value,
    evalueCutoff: evalueCutoff.value,
    flankBp: flankBp.value,
    limit: resultLimit.value ?? 0,
  };
}

function updateJobProgress(job: SequencePeak2GeneJobResponse) {
  jobProgress.value = Math.max(jobProgress.value, job.progress ?? 0);
  progressStage.value = job.stage || job.status;
  progressMessage.value = job.message || "Sequence analysis is running.";
}

function waitForPoll(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms));
}

async function awaitSequenceJob(initial: SequencePeak2GeneJobResponse, generation: number) {
  let job = initial;
  updateJobProgress(job);
  while (job.status === "QUEUED" || job.status === "RUNNING") {
    await waitForPoll(Math.max(700, Math.min(job.pollAfterMs || 1000, 2000)));
    if (generation !== pollGeneration) return null;
    job = await fetchSequencePeak2GeneJob(job.jobId);
    updateJobProgress(job);
  }
  if (job.status === "FAILED") throw new Error(job.error || "Sequence mapping failed.");
  if (!job.result) throw new Error("The sequence job completed without a result.");
  return job.result;
}

async function runMapping() {
  if (loading.value) return;
  const err = validateSeq(sequenceText.value);
  if (err) { setInputError(err); return; }
  const generation = ++pollGeneration;
  const request = buildSequenceRequest();
  loading.value = true;
  hasResults.value = false;
  result.value = null;
  lastRunRequest.value = null;
  evidenceError.value = null;
  jobProgress.value = 1;
  progressStage.value = "QUEUED";
  progressMessage.value = "Submitting the sequence to the BLAST queue.";

  try {
    const submitted = await submitSequencePeak2GeneJob(request);
    const completed = await awaitSequenceJob(submitted, generation);
    if (!completed || generation !== pollGeneration) return;
    result.value = completed;
    lastRunRequest.value = request;
    jobProgress.value = 100;
    progressStage.value = "COMPLETED";
    hasResults.value = true;
    activeTab.value = "blastHits";
  } catch (e: any) {
    progressStage.value = "FAILED";
    progressMessage.value = e?.response?.data?.detail
      || e?.response?.data?.message
      || e?.response?.data?.error
      || e?.message
      || "Mapping failed.";
    setInputError(progressMessage.value);
  } finally {
    if (generation === pollGeneration) loading.value = false;
  }
}

async function selectEvidenceHit(hit: BlastHitDto) {
  if (!result.value || evidenceLoading.value || hit.hitId === result.value.evidenceHitId) return;
  evidenceLoading.value = true;
  evidenceError.value = null;
  const analysisContext = lastRunRequest.value ?? buildSequenceRequest();
  try {
    const evidence = await fetchSequencePeak2GeneEvidence({
      hitId: hit.hitId,
      hitRank: hit.rank,
      chromosome: hit.chromosome,
      start: hit.start,
      end: hit.end,
      strand: hit.strand,
      referenceScope: analysisContext.referenceScope,
      datasetId: analysisContext.datasetId,
      resultContent: analysisContext.resultContent,
      flankBp: analysisContext.flankBp,
      limit: analysisContext.limit,
    });
    result.value.evidenceHitId = evidence.hitId;
    result.value.query.usedHitIndex = Math.max(0, evidence.hitRank - 1);
    result.value.peakGeneLinks = evidence.peakGeneLinks;
    result.value.markerPeaks = evidence.markerPeaks;
    result.value.summary.overlappingPeakCount = evidence.overlappingPeakCount;
    result.value.summary.linkedGeneCount = evidence.linkedGeneCount;
    result.value.summary.markerPeakCount = evidence.returnedMarkerPeakCount;
    result.value.summary.returnedP2gCount = evidence.returnedP2gCount;
    result.value.summary.returnedMarkerPeakCount = evidence.returnedMarkerPeakCount;
    result.value.summary.evidencePossiblyTruncated = evidence.possiblyTruncated;
    onPageChange(1);
  } catch (e: any) {
    evidenceError.value = e?.response?.data?.detail
      || e?.response?.data?.message
      || e?.response?.data?.error
      || e?.message
      || "Failed to load results for this candidate.";
  } finally {
    evidenceLoading.value = false;
  }
}

function resetAll() {
  pollGeneration++;
  sequenceText.value = "";
  referenceScope.value = "all";
  resultContent.value = "all";
  advancedOpen.value = false;
  blastTask.value = "auto";
  maxTargetSeqs.value = 500;
  maxHsps.value = 200;
  evalueCutoff.value = 10.0;
  flankBp.value = 0;
  resultLimit.value = null;
  hasResults.value = false;
  result.value = null;
  lastRunRequest.value = null;
  evidenceError.value = null;
  jobProgress.value = 0;
  progressStage.value = "IDLE";
  progressMessage.value = "Waiting to start.";
  clearInputError();
}

watch(activeTab, () => { sortColumn.value = null; sortDirection.value = "desc"; });

function toggleSort(col: string) {
  if (sortColumn.value === col) {
    if (sortDirection.value === "desc") { sortDirection.value = "asc"; }
    else if (sortDirection.value === "asc") { sortColumn.value = null; sortDirection.value = "desc"; }
  } else {
    sortColumn.value = col;
    sortDirection.value = "desc";
  }
}

function sortArrow(col: string): string {
  if (sortColumn.value !== col) return "⇅";
  return sortDirection.value === "desc" ? "▼" : "▲";
}

function downloadCurrentTable() {
  const rows = activeTableRows.value;
  if (!rows?.length) return;

  let csv: string;
  if (activeTab.value === "p2g") {
    csv = "Dataset,Peak region,Linked gene,Correlation,FDR,Link score\n"
      + rows.map((r: any) => [r.datasetId, `${r.chromosome}:${r.peakStart}-${r.peakEnd}`, r.geneName, r.correlation, r.fdr, r.linkScore].join(",")).join("\n");
  } else if (activeTab.value === "allPeaks") {
    csv = "Dataset,Peak region,Source,Linked genes,FDR,Link score\n"
      + (rows as MergedPeakRow[]).map(r => [r.datasetId, `${r.chromosome}:${r.peakStart}-${r.peakEnd}`, r.source, r.linkedGenes, r.linkFdr, r.linkScore].join(",")).join("\n");
  } else if (activeTab.value === "markerPeaks") {
    csv = "Dataset,Domain,Cluster,Peak region,Linked genes,FDR,Link score\n"
      + (rows as MarkerPeakDto[]).map(r => [r.datasetId, r.domain, r.groupName, `${r.chromosome}:${r.peakStart}-${r.peakEnd}`, markerLinkedGenesForCsv(r), markerBestLinkFdr(r), markerBestLinkScore(r)].join(",")).join("\n");
  } else {
    csv = "Hit ID,Rank,Chromosome,Start,End,Strand,Identity,Query coverage,Align len,E-value,Bit score,Top-score ratio,Selected for analysis\n"
      + rows.map((r: any) => [r.hitId, r.rank, r.chromosome, r.start, r.end, r.strand, `${r.identity}%`, `${r.queryCoverage}%`, r.alignLen, r.evalue, r.bitScore, r.scoreRatio, r.hitId === result.value?.evidenceHitId].join(",")).join("\n");
  }

  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `oscar_${activeTab.value}_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function fmt(n: number): string { return n?.toLocaleString() ?? "—"; }
function isMarkerPeak(row: PeakGeneLinkDto): boolean {
  if (!markerPeakKeySet.value) return false;
  return markerPeakKeySet.value.has(`${row.chromosome}:${row.peakStart}-${row.peakEnd}`);
}
function splitDatasets(datasetId: string): string[] {
  return (datasetId || "").split(",").map((s) => s.trim()).filter(Boolean);
}
function goToSampleDetail(datasetId: string) {
  if (!datasetId) return;
  router.push({ name: "SampleDetail", params: { id: datasetId }, query: { domain: "integration", source: "analysis" } });
}
function goToPeakDetail(datasetId: string, domain: string, chromosome: string, start: number, end: number) {
  router.push({
    path: "/feature-detail",
    query: {
      type: "peak",
      chrom: chromosome,
      start: String(start),
      end: String(end),
      peakId: `${chromosome}:${start}-${end}`,
      datasetId,
      domain: domain || "integration",
      source: "analysis_sequence",
      returnTo: "/analysis",
    },
  });
}
onBeforeUnmount(() => {
  pollGeneration++;
  if (validateDebounce) clearTimeout(validateDebounce);
  if (errorTimer) clearTimeout(errorTimer);
});
onDeactivated(() => {
  datasetPickerVisible.value = false;
});
</script>

<style scoped>
.spg-root { display: flex; flex-direction: column; gap: 12px; }
.spg-desc { margin: 0; color: var(--muted); font-size: 14px; font-weight: 750; line-height: 1.55; }
.spg-workbench { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(360px, 1fr); gap: 14px; align-items: stretch; }
.spg-builder-card { display: flex; flex-direction: column; gap: 14px; width: 100%; height: 100%; }
.spg-section { position: relative; min-width: 0; }
.spg-section + .spg-section { padding-top: 14px; border-top: 1px solid var(--border); }
.spg-side-column { display: flex; flex-direction: column; gap: 12px; min-width: 0; height: 100%; }
.cte-card.spg-image-card { flex: 0 0 auto; aspect-ratio: 1619 / 972; height: auto; max-height: none; display: flex; align-items: center; justify-content: center; overflow: hidden; padding: 0; }
.spg-slot-img { width: 100%; height: 100%; max-width: none; max-height: none; object-fit: contain; border-radius: inherit; display: block; }
.spg-qc-row { display: flex; gap: 8px; flex-wrap: wrap; }
.spg-qc-card { position: relative; flex: 1 1 110px; display: flex; flex-direction: column; gap: 2px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); }
.spg-qc-card--muted { background: #f3f4f6; border-color: rgba(160,165,175,0.25); }
.spg-qc-card--info { background: #f0f4f8; border-color: rgba(140,170,200,0.25); }
.spg-qc-card--bad { background: #fdf0f0; border-color: rgba(200,125,125,0.22); }
.spg-qc-label { font-size: 11px; font-weight: 800; color: var(--muted); }
.spg-qc-val { font-size: 15px; font-weight: 900; color: var(--text); }
.spg-progress-card { margin-top: 12px; padding: 14px 15px 13px; border: 1px solid rgba(143,165,156,0.34); border-radius: 13px; background: linear-gradient(145deg, rgba(247,250,248,0.98), rgba(239,246,243,0.92)); box-shadow: inset 0 1px 0 rgba(255,255,255,0.82); }
.spg-progress-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 11px; }
.spg-progress-stage { color: var(--text); font-size: 13px; font-weight: 900; }
.spg-progress-message { margin-top: 2px; color: var(--muted); font-size: 11.5px; font-weight: 700; line-height: 1.4; }
.spg-progress-value { flex: 0 0 auto; color: var(--brand-primary-3); font-size: 17px; font-variant-numeric: tabular-nums; font-weight: 950; }
.spg-progress-track { position: relative; height: 9px; overflow: hidden; border-radius: 999px; background: rgba(143,165,156,0.18); box-shadow: inset 0 1px 2px rgba(39,66,58,0.08); }
.spg-progress-fill { position: relative; height: 100%; min-width: 4px; border-radius: inherit; background: linear-gradient(90deg, #9bb4aa, var(--brand-primary-3), #668c7d); box-shadow: 0 0 12px rgba(95,125,112,0.28); transition: width 0.5s cubic-bezier(0.22,1,0.36,1); }
.spg-progress-track.indeterminate .spg-progress-fill::after { content: ""; position: absolute; inset: 0; width: 42%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.72), transparent); animation: spgProgressShimmer 1.35s ease-in-out infinite; }
.spg-progress-steps { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 9px; }
.spg-progress-steps span { position: relative; padding-top: 8px; color: #9aa7a2; font-size: 10px; font-weight: 800; text-align: center; transition: color 0.2s ease; }
.spg-progress-steps span::before { content: ""; position: absolute; top: 0; left: 50%; width: 5px; height: 5px; border-radius: 999px; background: #cbd4d0; transform: translateX(-50%); }
.spg-progress-steps span.done, .spg-progress-steps span.active { color: var(--brand-primary-3); }
.spg-progress-steps span.done::before, .spg-progress-steps span.active::before { background: var(--brand-primary-3); box-shadow: 0 0 0 3px rgba(143,165,156,0.16); }
.spg-results { display: flex; flex-direction: column; gap: 14px; }
.spg-mapping-banner { display: flex; align-items: flex-start; gap: 13px; padding: 14px 16px; border: 1px solid rgba(143,165,156,0.34); border-left-width: 4px; border-radius: 13px; background: #f4f8f6; }
.spg-status-badge { flex: 0 0 auto; padding: 5px 10px; border-radius: 999px; background: var(--brand-primary-3); color: #fff; font-size: 11px; font-weight: 900; letter-spacing: 0.15px; white-space: nowrap; }
.spg-status-copy { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.spg-status-copy strong { color: var(--text); font-size: 13.5px; font-weight: 900; }
.spg-status-copy span { color: var(--muted); font-size: 12px; font-weight: 700; line-height: 1.48; }
.spg-ambiguity-rule { color: rgba(66,82,76,0.72); font-size: 10.5px; font-weight: 750; line-height: 1.4; }
.spg-mapping-banner.status-ambiguous { border-color: rgba(216,154,54,0.42); border-left-color: #d89a36; background: #fff9ed; }
.spg-mapping-banner.status-ambiguous .spg-status-badge { background: #b97918; }
.spg-mapping-banner.status-partial { border-color: rgba(196,117,82,0.4); border-left-color: #c47552; background: #fff6f1; }
.spg-mapping-banner.status-partial .spg-status-badge { background: #a65d3d; }
.spg-mapping-banner.status-no_hit { border-color: rgba(181,72,72,0.34); border-left-color: #b54848; background: #fff3f3; }
.spg-mapping-banner.status-no_hit .spg-status-badge { background: #a13f3f; }
.spg-mapping-banner.status-best_supported { border-left-color: #648d9d; background: #f2f8fa; }
.spg-mapping-banner.status-best_supported .spg-status-badge { background: #527b8a; }
.spg-science-note { padding: 10px 13px; border: 1px solid rgba(216,154,54,0.28); border-radius: 10px; background: #fffaf0; color: #7d5b22; font-size: 12px; font-weight: 750; line-height: 1.48; }
.spg-evidence-error { margin: 0; }
.spg-summary-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.spg-top-hit-cards { border: 1px solid var(--border); border-radius: 14px; padding: 16px; background: var(--surface); box-shadow: var(--shadow-card); }
.spg-top-hit-cards.loading { opacity: 0.72; }
.spg-top-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 10px; }
.spg-top-title { font-size: 14px; font-weight: 900; }
.spg-coordinate-note { margin-top: 3px; color: var(--muted); font-size: 11px; font-weight: 700; }
.spg-tab-overlay { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: rgba(251,252,251,0.85); z-index: 4; border-radius: 12px; color: var(--brand-primary-3); font-size: 14px; font-weight: 900; }
.btn-spinner.dark { border-color: rgba(95,125,112,0.22); border-top-color: var(--brand-primary-3); }
.spg-detail-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.spg-detail-card { display: flex; flex-direction: column; align-items: center; justify-content: space-between; padding: 12px 6px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); text-align: center; min-height: 64px; }
.spg-detail-val { font-size: 14px; font-weight: 900; color: var(--text); word-break: break-all; }
.spg-detail-label { font-size: 11px; font-weight: 700; color: var(--muted); text-transform: uppercase; letter-spacing: 0.3px; }
.spg-top-actions { margin-top: 10px; display: flex; gap: 8px; }
.spg-flow { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 14px; border: 1px solid var(--border); border-radius: 14px; background: var(--surface); flex-wrap: wrap; }
.spg-flow-step { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.spg-flow-num { font-size: 15px; font-weight: 900; color: var(--brand-primary-3); }
.spg-flow-label { font-size: 11px; font-weight: 700; color: var(--muted); }
.spg-flow-arrow { font-size: 18px; color: var(--border-brand); font-weight: 900; }
.spg-tab-content { position: relative; min-height: 120px; }
.spg-tab-loading { pointer-events: none; opacity: 0.55; }

.cte-card { box-sizing: border-box; background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-card); }
.cte-card-title { font-size: 15px; font-weight: 900; margin: 0 0 12px; color: var(--text); }
.cte-max-badge { display: inline-flex; align-items: center; min-height: 20px; margin-left: 6px; padding: 1px 8px; border: 1px solid rgba(95,125,112,0.24); border-radius: 999px; background: rgba(143,165,156,0.10); color: var(--brand-primary-3); font-size: 10px; font-weight: 900; letter-spacing: 0.02em; vertical-align: middle; }
.cte-settings-card { position: relative; }
.spg-builder-card .cte-settings-card { display: flex; flex: 1 1 auto; flex-direction: column; min-height: 0; }
.cte-settings-card .cte-card-title { padding-right: 150px; }
.cte-textarea { width: 100%; box-sizing: border-box; min-height: 96px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-2); color: var(--text); font-family: "JetBrains Mono","SFMono-Regular",Consolas,monospace; font-size: 14px; line-height: 1.6; resize: vertical; transition: border-color 0.18s ease, box-shadow 0.18s ease; }
.cte-textarea:focus { outline: none; border-color: var(--border-brand); box-shadow: 0 0 0 3px rgba(143,165,156,0.14); }
.cte-textarea:disabled { opacity: 0.55; cursor: not-allowed; }
.cte-textarea--error { border-color: #e05555; box-shadow: 0 0 0 3px rgba(224,85,85,0.12); }
.cte-hint { margin: 6px 0 10px; color: var(--muted); font-size: 12px; font-weight: 700; }
.cte-btn-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.cte-card-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-top: auto; padding-top: 12px; border-top: 1px solid var(--border); }
.primary-btn { display: inline-flex; align-items: center; gap: 6px; min-height: 38px; padding: 8px 22px; border: none; border-radius: 11px; background: var(--brand-primary-3); color: #fff; font-size: 14px; font-weight: 900; cursor: pointer; box-shadow: 0 6px 16px rgba(95,125,112,0.18); transition: background 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease; }
.primary-btn:hover:not(:disabled) { background: #7f9f94; transform: translateY(-1px); box-shadow: 0 8px 20px rgba(95,125,112,0.24); }
.primary-btn:disabled { opacity: 0.55; cursor: not-allowed; box-shadow: none; }
.soft-btn { display: inline-flex; align-items: center; gap: 4px; min-height: 34px; padding: 6px 14px; border: 1px solid var(--border); border-radius: 9px; background: var(--surface); color: var(--text); font-size: 13px; font-weight: 800; cursor: pointer; transition: border-color 0.18s ease, background 0.18s ease; }
.soft-btn:hover:not(:disabled) { border-color: var(--border-brand); background: var(--surface-2); }
.soft-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-spinner { display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.35); border-top-color: #fff; border-radius: 999px; animation: spin 0.6s linear infinite; }
.cte-fields { display: flex; flex-direction: column; gap: 10px; }
.cte-field { display: flex; flex-direction: column; gap: 5px; }
.cte-field-label { font-size: 13px; font-weight: 900; color: rgba(39,66,58,0.84); }
.cte-field-label--help { display: inline-flex; align-items: center; align-self: flex-start; gap: 5px; }
.cte-inline-help-icon { display: inline-flex; align-items: center; justify-content: center; width: 15px; height: 15px; flex: 0 0 15px; border: 1px solid var(--border-brand); border-radius: 999px; background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; line-height: 1; cursor: help; }
.cte-inline-help-icon:focus-visible { outline: 2px solid rgba(78,133,118,0.3); outline-offset: 2px; }
.spg-field-help { display: flex; flex-direction: column; gap: 5px; max-width: 300px; line-height: 1.45; }
.cte-select { width: 100%; }
.cte-number { width: 100%; }
.cte-field-hint { color: var(--muted); font-size: 10px; font-weight: 700; line-height: 1.35; }
.cte-settings-body { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; align-items: start; margin-bottom: 10px; }
.cte-settings-advanced-col { display: flex; flex-direction: column; }
.cte-advanced-toggle { position: absolute; top: 16px; right: 0; display: inline-flex; align-items: center; gap: 6px; padding: 2px 0; border: none; background: transparent; color: var(--muted); font-size: 13px; font-weight: 900; cursor: pointer; transition: color 0.18s ease; align-self: flex-start; }
.cte-advanced-toggle:hover { color: var(--text); }
.cte-toggle-chev { display: inline-block; font-size: 10px; transition: transform 0.18s ease; }
.cte-advanced-toggle.open .cte-toggle-chev { transform: rotate(90deg); }
.cte-advanced { display: flex; flex-direction: column; gap: 10px; }
.how-steps { display: flex; flex-direction: column; gap: 9px; }
.how-step { display: flex; gap: 10px; align-items: flex-start; }
.how-num { flex: 0 0 28px; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border-radius: 999px; background: var(--nav-active-bg); color: var(--nav-active-text); font-size: 13px; font-weight: 900; border: 1px solid var(--nav-active-border); }
.how-step-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.how-step-body strong { font-size: 12.5px; font-weight: 900; color: var(--text); }
.how-step-body span { font-size: 11.5px; font-weight: 700; color: var(--muted); line-height: 1.38; }
.how-note { margin: 9px 0 0; padding-top: 8px; border-top: 1px solid var(--border); font-size: 11.5px; font-weight: 700; color: var(--muted); line-height: 1.45; }
.cte-how-card .cte-card-title { font-size: 16px; }
.spg-side-column .cte-how-card { flex: 1 1 auto; min-height: 0; height: auto; display: flex; flex-direction: column; padding: 16px 18px; }
.spg-side-column .how-steps { flex: 1 1 auto; min-height: 0; justify-content: space-evenly; }
.spg-side-column .how-step { flex: 0 0 auto; }
.cte-summary-card { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; padding: 16px 10px; border: 1px solid var(--border); border-radius: 14px; background: var(--surface); box-shadow: var(--shadow-card); text-align: center; }
.sum-num { font-size: 22px; font-weight: 900; color: var(--text); }
.sum-label { font-size: 11px; font-weight: 800; color: var(--muted); }
.cte-tabs-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; border-bottom: 1px solid var(--border); }
.cte-tabs { display: flex; gap: 4px; }
.cte-tab { min-height: 34px; padding: 8px 16px; border: none; border-bottom: 2px solid transparent; background: transparent; color: var(--muted); font-size: 13px; font-weight: 800; cursor: pointer; transition: color 0.18s ease, border-color 0.18s ease; }
.cte-tab:hover { color: var(--text); }
.cte-tab.active { color: var(--text); border-bottom-color: var(--brand-primary-3); font-weight: 900; }

/* ── download button (same as Sample details) ── */
.annotation-download-button {
  -webkit-appearance: none; appearance: none;
  box-sizing: border-box;
  display: inline-flex; align-items: center; justify-content: center;
  flex: 0 0 32px; width: 32px; height: 32px; padding: 0;
  border: 1px solid var(--border-brand);
  border-radius: 999px;
  background: #fffffff2;
  color: var(--brand-primary-3);
  box-shadow: inset 0 1px 0 #ffffffcc, 0 6px 14px #12182614;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, transform 0.18s ease;
  margin-bottom: 4px;
}
.annotation-download-button:hover:not(:disabled) {
  border-color: var(--nav-active-border);
  background: var(--surface-2);
  color: var(--text);
  box-shadow: inset 0 1px 0 #ffffffcc, 0 8px 16px rgba(95,125,112,0.16);
  transform: translateY(-1px);
}
.annotation-download-button:disabled {
  border-color: var(--border); background: #ffffffb8; color: var(--muted);
  cursor: not-allowed; opacity: 0.56; pointer-events: none;
  box-shadow: inset 0 1px 0 #ffffffcc;
}
.annotation-download-button :deep(.el-icon) { font-size: 15px; }

.cte-pagination { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px 0; flex-wrap: wrap; }
.cte-page-jump { width: 52px; padding: 2px 4px; border: 1px solid var(--border); border-radius: 4px; text-align: center; font-size: 13px; font-weight: 700; color: var(--text); background: var(--surface); }
.cte-page-jump::-webkit-inner-spin-button,
.cte-page-jump::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.cte-page-btn {
  min-height: 30px; padding: 4px 14px;
  border: 1px solid var(--border); border-radius: 8px;
  background: var(--surface); color: var(--text); font-size: 12px; font-weight: 800;
  cursor: pointer; transition: border-color 0.15s, background 0.15s;
}
.cte-page-btn:hover:not(:disabled) { border-color: var(--border-brand); background: var(--surface-2); }
.cte-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.cte-page-info { font-size: 12px; font-weight: 800; color: var(--muted); }

.cte-table-wrap { overflow-x: auto; border: 1px solid var(--border); border-radius: 11px; background: var(--surface); }
.cte-table { width: 100%; border-collapse: collapse; font-size: 14px; color: var(--el-text-color-regular, #606266); }
.cte-table th { padding: 10px 12px; text-align: center; vertical-align: middle; border-bottom: 1px solid var(--border); white-space: nowrap; background: var(--surface-2); font-weight: 600; color: var(--el-text-color-secondary, #909399); font-size: 14px; }
.cte-table td { padding: 10px 12px; text-align: center; vertical-align: middle; border-bottom: 1px solid var(--border); white-space: nowrap; }
.cte-table tbody tr:last-child td { border-bottom: none; }
.spg-candidate-table tbody tr { transition: background 0.16s ease, box-shadow 0.16s ease; }
.spg-candidate-table tbody tr.equivalent { background: rgba(216,154,54,0.055); }
.spg-candidate-table tbody tr.selected { background: rgba(143,165,156,0.13); box-shadow: inset 3px 0 0 var(--brand-primary-3); }
.spg-rank-pill { display: inline-flex; align-items: center; justify-content: center; min-width: 30px; padding: 3px 7px; border-radius: 999px; background: var(--surface-3); color: var(--text); font-size: 11px; font-weight: 900; }
.spg-select-hit { min-height: 29px; padding: 4px 10px; border: 1px solid var(--border-brand); border-radius: 8px; background: var(--surface); color: var(--brand-primary-3); font-size: 11px; font-weight: 900; cursor: pointer; transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease; }
.spg-select-hit:hover:not(:disabled) { background: var(--surface-2); border-color: var(--brand-primary-3); }
.spg-select-hit.active { background: var(--brand-primary-3); border-color: var(--brand-primary-3); color: #fff; cursor: default; }
.spg-select-hit:disabled:not(.active) { opacity: 0.48; cursor: wait; }
.cte-no-data { text-align: center; color: var(--muted); padding: 32px; }
.cte-input-error { position: relative; display: flex; align-items: flex-start; gap: 8px; padding: 10px 14px 13px; background: #fef2f2; border: 1px solid #f5b8b8; border-radius: 10px; margin-bottom: 10px; color: #b53b3b; font-size: 13px; font-weight: 800; line-height: 1.5; overflow: hidden; }
.cte-error-icon { flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px; border-radius: 999px; background: #e05555; color: #fff; font-size: 11px; font-weight: 900; }
.cte-error-text { flex: 1; min-width: 0; }
.cte-error-close { flex-shrink: 0; width: 24px; height: 24px; border: none; border-radius: 6px; background: transparent; color: #b53b3b; font-size: 16px; font-weight: 900; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background 0.15s; }
.cte-error-close:hover { background: rgba(224,85,85,0.12); }
.cte-error-bar { position: absolute; bottom: 0; left: 0; height: 3px; background: #e05555; border-radius: 0 0 10px 10px; animation: cteErrorShrink 8s linear forwards; }
.cte-builder-section { position: relative; min-width: 0; }
.cte-builder-section + .cte-builder-section { padding-top: 16px; border-top: 1px solid var(--border); }

@keyframes cteErrorShrink { from { width: 100%; } to { width: 0%; } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes spgProgressShimmer { from { transform: translateX(-120%); } to { transform: translateX(340%); } }

@media (max-width: 1024px) {
  .spg-workbench { grid-template-columns: 1fr; }
  .cte-card.spg-image-card { flex-basis: auto; height: auto; max-height: none; }
  .spg-side-column .cte-how-card { flex: initial; }
}

@media (max-width: 760px) {
  .spg-summary-row { grid-template-columns: repeat(2, 1fr); }
  .spg-detail-grid { grid-template-columns: repeat(4, 1fr); }
  .cte-settings-body { grid-template-columns: 1fr; }
  .cte-settings-card .cte-card-title { padding-right: 0; }
  .cte-advanced-toggle { position: static; margin-bottom: 10px; }
  .spg-mapping-banner, .spg-top-heading { flex-direction: column; }
  .spg-progress-steps { gap: 2px; }
  .spg-progress-steps span { font-size: 9px; }
}

.cte-modal-overlay { position: fixed; inset: 0; z-index: 9999; background: rgba(18,24,38,0.44); display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); }
.cte-modal { width: min(560px, calc(100vw - 32px)); max-height: 85vh; overflow-y: auto; background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 24px; box-shadow: var(--shadow-hover); }
.cte-modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.cte-modal-title { font-size: 18px; font-weight: 900; }
.cte-modal-close { width: 32px; height: 32px; border: none; border-radius: 8px; background: transparent; color: var(--muted); font-size: 20px; font-weight: 900; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background 0.15s, color 0.15s; }
.cte-modal-close:hover { background: var(--surface-3); color: var(--text); }
.cte-dataset-modal { max-width: 420px; }
.cte-dataset-list { display: flex; flex-direction: column; gap: 6px; max-height: 360px; overflow-y: auto; }
.cte-dataset-empty { padding: 24px; text-align: center; color: var(--muted); font-size: 13px; font-weight: 700; }
.cte-dataset-item { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface); cursor: pointer; transition: border-color 0.15s, background 0.15s; text-align: left; }
.cte-dataset-item:hover { border-color: var(--border-brand); background: var(--surface-2); }
.cte-dataset-item.active { border-color: var(--brand-primary-3); background: #f4f8f6; }
.cte-dataset-id { font-family: "JetBrains Mono", monospace; font-size: 13px; font-weight: 900; color: var(--text); }
.cte-dataset-label { font-size: 13px; font-weight: 700; color: var(--muted); }
.cte-ref-scope-row { display: flex; align-items: center; gap: 8px; }
.cte-ref-chip { display: inline-flex; align-items: center; gap: 6px; padding: 3px 8px 3px 10px; border: 1px solid var(--border-brand); border-radius: 999px; background: #f4f8f6; font-size: 12px; font-weight: 800; }
.cte-ref-chip-id { font-family: "JetBrains Mono", monospace; color: var(--text); }
.cte-ref-chip-x { width: 18px; height: 18px; border: none; border-radius: 999px; background: transparent; color: var(--muted); font-size: 14px; font-weight: 900; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; transition: background 0.15s, color 0.15s; }
.cte-ref-chip-x:hover { background: var(--surface-3); color: var(--text); }


.spg-upload-wrap { position: relative; display: inline-flex; }
.spg-help-icon { position: absolute; top: -6px; right: -6px; display: inline-flex; align-items: center; justify-content: center; width: 16px; height: 16px; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; z-index: 1; }
.spg-qc-help { position: absolute; top: -6px; right: -6px; width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; border: 1px solid var(--border-brand); background: var(--surface); color: var(--brand-primary-3); font-size: 9px; font-weight: 900; cursor: help; z-index: 1; }
.gsc-sort-th { cursor: pointer; user-select: none; }
.gsc-sort-th:hover { color: var(--brand-primary-3); }
.gsc-sort-arrow { font-size: 10px; margin-left: 2px; }

/* marker peak badge in P2G table */
.spg-peak-col {
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.spg-peak-link {
  color: var(--brand-primary-3);
  text-decoration: none;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s;
}
.spg-peak-link:hover {
  color: #5E7D6E;
  text-decoration: underline;
}
.spg-marker-badge {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  padding: 2px 7px;
  border: 1px solid var(--brand-primary-3);
  border-radius: 5px;
  background: var(--brand-primary-3);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.2px;
  line-height: 1;
  white-space: nowrap;
}

.spg-source-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 5px;
  font-size: 10.5px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}
.spg-source-p2g { background: rgba(78,133,118,0.12); color: var(--brand-primary-3); }
.spg-source-marker { background: rgba(180,140,70,0.12); color: #8a6a2e; }
.spg-source-both { background: rgba(78,133,118,0.18); color: #1a4a3e; }

</style>

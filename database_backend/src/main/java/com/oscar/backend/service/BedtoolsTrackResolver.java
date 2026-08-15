package com.oscar.backend.service;

import com.oscar.backend.config.BedtoolsProperties;
import com.oscar.backend.entity.BedtoolsSourceOption;
import com.oscar.backend.entity.BedtoolsTrackItemStatusResponse;
import com.oscar.backend.entity.BedtoolsTrackStatusResponse;
import com.oscar.backend.entity.ReferenceTrack;
import com.oscar.backend.mapper.ReferenceTrackMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class BedtoolsTrackResolver {

    private static final String STATUS_READY = "READY";
    private static final String STATUS_NOT_AVAILABLE = "NOT_AVAILABLE";
    private static final String STATUS_MISSING = "MISSING";
    private static final String STATUS_TRACKS_NOT_READY = "TRACKS_NOT_READY";
    private static final String STATUS_REFERENCE_NOT_READY = "REFERENCE_NOT_READY";
    private static final String CATEGORY_REGULATORY = "regulatory";

    private static final List<BedtoolsAnnotationType> REGULATORY_REFERENCE_TYPES = List.of(
            BedtoolsAnnotationType.RISK_SNP,
            BedtoolsAnnotationType.COMMON_SNP,
            BedtoolsAnnotationType.GTEX_EQTL,
            BedtoolsAnnotationType.TFBS,
            BedtoolsAnnotationType.ENHANCER,
            BedtoolsAnnotationType.SUPER_ENHANCER,
            BedtoolsAnnotationType.METHYLATION,
            BedtoolsAnnotationType.CRISPR,
            BedtoolsAnnotationType.ATAC_PEAKS,
            BedtoolsAnnotationType.GENOMIC_3D,
            BedtoolsAnnotationType.DNASE_PEAKS,
            BedtoolsAnnotationType.TAD,
            BedtoolsAnnotationType.ERNA,
            BedtoolsAnnotationType.TF_CHIP_SEQ,
            BedtoolsAnnotationType.TCOF
    );

    private final BedtoolsProperties bedtoolsProperties;
    private final BedtoolsTrackBuildService bedtoolsTrackBuildService;
    private final ReferenceTrackMapper referenceTrackMapper;

    public BedtoolsTrackResolver(
            BedtoolsProperties bedtoolsProperties,
            BedtoolsTrackBuildService bedtoolsTrackBuildService,
            ReferenceTrackMapper referenceTrackMapper
    ) {
        this.bedtoolsProperties = bedtoolsProperties;
        this.bedtoolsTrackBuildService = bedtoolsTrackBuildService;
        this.referenceTrackMapper = referenceTrackMapper;
    }

    public List<BedtoolsSourceOption> listSources(String datasetId, String domain, String genomeBuild) {
        List<BedtoolsSourceOption> sources = new ArrayList<>();
        BedtoolsTrackStatusResponse trackStatus = "integration".equals(domain)
                ? bedtoolsTrackBuildService.getTrackStatus(datasetId, domain, genomeBuild)
                : null;

        sources.add(sampleSource(
                BedtoolsAnnotationType.MARKER_PEAK,
                domain,
                trackStatus,
                "Sample-specific marker peak tracks are currently available for the Integration view only."
        ));
        sources.add(sampleSource(
                BedtoolsAnnotationType.P2G_LINK,
                domain,
                trackStatus,
                "Sample-specific P2G link tracks are currently available for the Integration view only."
        ));

        for (BedtoolsAnnotationType type : REGULATORY_REFERENCE_TYPES) {
            sources.add(regulatoryReferenceSource(type, genomeBuild));
        }

        sources.add(new BedtoolsSourceOption(
                BedtoolsAnnotationType.TF_ANNOTATION.value(),
                BedtoolsAnnotationType.TF_ANNOTATION.sourceLabel(),
                BedtoolsAnnotationType.TF_ANNOTATION.scope(),
                false,
                STATUS_NOT_AVAILABLE,
                "TF annotation data have not been integrated yet.",
                null
        ));
        return sources;
    }

    public List<BedtoolsSourceOption> listReferenceSources(String genomeBuild) {
        List<BedtoolsSourceOption> sources = new ArrayList<>();
        for (BedtoolsAnnotationType type : REGULATORY_REFERENCE_TYPES) {
            sources.add(regulatoryReferenceSource(type, genomeBuild));
        }
        sources.add(new BedtoolsSourceOption(
                BedtoolsAnnotationType.TF_ANNOTATION.value(),
                BedtoolsAnnotationType.TF_ANNOTATION.sourceLabel(),
                BedtoolsAnnotationType.TF_ANNOTATION.scope(),
                false,
                STATUS_NOT_AVAILABLE,
                "TF annotation data have not been integrated yet.",
                null
        ));
        return sources;
    }

    public ResolvedTrack resolveForIntersect(
            BedtoolsAnnotationType type,
            String datasetId,
            String domain,
            String genomeBuild
    ) {
        if (type == BedtoolsAnnotationType.TF_ANNOTATION) {
            throw new BedtoolsQueryException(
                    "TRACK_NOT_AVAILABLE",
                    "TF annotation data have not been integrated yet.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if ("sample".equals(type.scope())) {
            if (!"integration".equals(domain)) {
                throw new BedtoolsQueryException(
                        "TRACK_NOT_AVAILABLE",
                        type.sourceLabel() + " are currently available for the Integration view only.",
                        HttpStatus.CONFLICT
                );
            }
            BedtoolsTrackStatusResponse status = bedtoolsTrackBuildService.getTrackStatus(datasetId, domain, genomeBuild);
            BedtoolsTrackItemStatusResponse item = itemStatus(status, type.value());
            if (item == null || !STATUS_READY.equals(item.getStatus())) {
                throw new BedtoolsQueryException(
                        STATUS_TRACKS_NOT_READY,
                        "Sample track is not READY for datasetId=" + datasetId + ", domain=" + domain
                                + ", trackType=" + type.value(),
                        HttpStatus.CONFLICT
                );
            }
            Path path = Paths.get(item.getTrackPath()).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                throw new BedtoolsQueryException(
                        STATUS_TRACKS_NOT_READY,
                        "Sample track file is missing for datasetId=" + datasetId + ", domain=" + domain
                                + ", trackType=" + type.value(),
                        HttpStatus.CONFLICT
                );
            }
            return new ResolvedTrack(type, type.scope(), path);
        }

        if (isRegulatoryReferenceType(type)) {
            return resolveRegulatoryReferenceTrack(type, genomeBuild);
        }

        Path path = firstExistingReferenceTrack(type, genomeBuild);
        if (path == null) {
            throw new BedtoolsQueryException(
                    STATUS_REFERENCE_NOT_READY,
                    referenceMissingReason(type, genomeBuild),
                    HttpStatus.CONFLICT
            );
        }
        return new ResolvedTrack(type, type.scope(), path);
    }

    public List<BedtoolsAnnotationType> getAvailableRegulatoryTypes(String genomeBuild) {
        List<BedtoolsAnnotationType> available = new ArrayList<>();
        for (BedtoolsAnnotationType type : REGULATORY_REFERENCE_TYPES) {
            ReferenceTrack track = referenceTrackMapper.findByGenomeBuildCategoryAndSourceType(
                    genomeBuild, CATEGORY_REGULATORY, type.value());
            if (track != null && STATUS_READY.equals(track.getStatus())) {
                Path path = Paths.get(track.getFilePath());
                if (Files.exists(path)) {
                    available.add(type);
                }
            }
        }
        return available;
    }

    private boolean isRegulatoryReferenceType(BedtoolsAnnotationType type) {
        return REGULATORY_REFERENCE_TYPES.contains(type);
    }

    private ResolvedTrack resolveRegulatoryReferenceTrack(BedtoolsAnnotationType type, String genomeBuild) {
        ReferenceTrack track = referenceTrackMapper.findByGenomeBuildCategoryAndSourceType(
                genomeBuild, CATEGORY_REGULATORY, type.value());
        if (track == null) {
            throw new BedtoolsQueryException(
                    STATUS_REFERENCE_NOT_READY,
                    "Reference track not found in oscar_reference_track: "
                            + "genomeBuild=" + genomeBuild + ", sourceType=" + type.value(),
                    HttpStatus.CONFLICT
            );
        }
        if (!STATUS_READY.equals(track.getStatus())) {
            throw new BedtoolsQueryException(
                    STATUS_REFERENCE_NOT_READY,
                    "Reference track is not READY: " + type.value()
                            + " (status=" + track.getStatus() + ")",
                    HttpStatus.CONFLICT
            );
        }
        Path path = Paths.get(track.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new BedtoolsQueryException(
                    STATUS_REFERENCE_NOT_READY,
                    "Reference track file is missing: " + track.getFilePath(),
                    HttpStatus.CONFLICT
            );
        }
        return new ResolvedTrack(type, type.scope(), path);
    }

    private BedtoolsSourceOption regulatoryReferenceSource(BedtoolsAnnotationType type, String genomeBuild) {
        ReferenceTrack track = referenceTrackMapper.findByGenomeBuildCategoryAndSourceType(
                genomeBuild, CATEGORY_REGULATORY, type.value());
        if (track == null) {
            return new BedtoolsSourceOption(
                    type.value(),
                    type.sourceLabel(),
                    type.scope(),
                    false,
                    STATUS_MISSING,
                    "No reference track record in oscar_reference_track for " + type.value(),
                    null
            );
        }
        Path path = Paths.get(track.getFilePath());
        boolean fileExists = Files.exists(path);
        boolean isReady = STATUS_READY.equals(track.getStatus()) && fileExists;
        String status = isReady ? STATUS_READY
                : (fileExists ? track.getStatus() : STATUS_MISSING);
        String reason = isReady ? null
                : "Reference track not available: "
                        + (fileExists ? "status=" + track.getStatus() : "file missing: " + track.getFilePath());
        return new BedtoolsSourceOption(
                type.value(),
                type.sourceLabel(),
                type.scope(),
                isReady,
                status,
                reason,
                isReady ? "Using reference track: " + track.getFilePath() : null
        );
    }

    private BedtoolsSourceOption sampleSource(
            BedtoolsAnnotationType type,
            String domain,
            BedtoolsTrackStatusResponse trackStatus,
            String domainUnavailableReason
    ) {
        if (!"integration".equals(domain)) {
            return new BedtoolsSourceOption(
                    type.value(),
                    type.sourceLabel(),
                    type.scope(),
                    false,
                    STATUS_NOT_AVAILABLE,
                    domainUnavailableReason,
                    null
            );
        }

        BedtoolsTrackItemStatusResponse item = itemStatus(trackStatus, type.value());
        boolean available = item != null && STATUS_READY.equals(item.getStatus());
        return new BedtoolsSourceOption(
                type.value(),
                type.sourceLabel(),
                type.scope(),
                available,
                available ? STATUS_READY : STATUS_TRACKS_NOT_READY,
                available ? null : "Sample track is not READY for this dataset/domain.",
                null
        );
    }

    @SuppressWarnings("unused")
    private BedtoolsSourceOption referenceSource(BedtoolsAnnotationType type, String genomeBuild) {
        Path path = firstExistingReferenceTrack(type, genomeBuild);
        boolean available = path != null;
        return new BedtoolsSourceOption(
                type.value(),
                type.sourceLabel(),
                type.scope(),
                available,
                available ? STATUS_READY : STATUS_REFERENCE_NOT_READY,
                available ? null : referenceMissingReason(type, genomeBuild),
                available ? "Using reference track: " + path : null
        );
    }

    private BedtoolsTrackItemStatusResponse itemStatus(BedtoolsTrackStatusResponse status, String trackType) {
        if (status == null || status.getItems() == null) {
            return null;
        }
        return status.getItems()
                .stream()
                .filter(item -> trackType.equals(item.getTrackType()))
                .findFirst()
                .orElse(null);
    }

    private Path referenceRoot(String genomeBuild) {
        return Paths.get(bedtoolsProperties.getBedtools().getReferenceRoot())
                .toAbsolutePath()
                .normalize()
                .resolve(genomeBuild.toLowerCase(Locale.ROOT))
                .normalize();
    }

    private List<String> referenceCandidateFiles(BedtoolsAnnotationType type) {
        return switch (type) {
            case GENE -> List.of("genes.bed", "regions/genes.bed", "regions/gene.bed");
            case TRANSCRIPT -> List.of("transcripts.bed", "regions/transcripts.bed");
            case TSS_PROMOTER -> List.of("tss_promoters.bed", "regions/tss_promoters.bed", "regions/tss.bed");
            default -> throw new IllegalArgumentException("Unsupported reference source: " + type.value());
        };
    }

    private Path firstExistingReferenceTrack(BedtoolsAnnotationType type, String genomeBuild) {
        Path root = referenceRoot(genomeBuild);
        List<String> candidateFiles = referenceCandidateFiles(type);
        for (String candidateFile : candidateFiles) {
            Path candidate = root.resolve(candidateFile).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String referenceMissingReason(BedtoolsAnnotationType type, String genomeBuild) {
        Path root = referenceRoot(genomeBuild);
        List<String> candidateFiles = referenceCandidateFiles(type);
        List<String> checkedPaths = candidateFiles
                .stream()
                .map(candidateFile -> root.resolve(candidateFile).normalize().toString())
                .toList();

        String reason = "Reference BED track is missing. Checked: " + checkedPaths;
        if (type == BedtoolsAnnotationType.GENE && Files.exists(root.resolve("genes.gtf.gz").normalize())) {
            reason = reason + " genes.gtf.gz exists, but GTF-to-BED conversion is not part of this query endpoint.";
        }
        return reason;
    }

    public record ResolvedTrack(BedtoolsAnnotationType type, String scope, Path path) {
    }
}

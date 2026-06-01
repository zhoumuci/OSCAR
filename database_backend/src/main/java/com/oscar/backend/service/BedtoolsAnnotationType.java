package com.oscar.backend.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum BedtoolsAnnotationType {
    MARKER_PEAK("marker_peak", "Marker peak", "Marker peaks", "sample"),
    P2G_LINK("p2g_link", "P2G link", "P2G links", "sample"),
    GENE("gene", "Gene", "Genes", "reference"),
    TRANSCRIPT("transcript", "Transcript", "Transcripts", "reference"),
    TSS_PROMOTER("tss_promoter", "TSS/promoter", "TSS/promoters", "reference"),
    TF_ANNOTATION("tf_annotation", "TF annotation", "TF annotation", "future"),
    RISK_SNP("risk_snp", "Risk SNP", "Risk SNPs", "reference"),
    COMMON_SNP("common_snp", "Common SNP", "Common SNPs", "reference"),
    GTEX_EQTL("gtex_eqtl", "GTEx eQTL", "GTEx eQTLs", "reference"),
    TFBS("tfbs", "TFBS", "TFBS", "reference"),
    ENHANCER("enhancer", "Enhancer", "Enhancers", "reference"),
    SUPER_ENHANCER("super_enhancer", "Super Enhancer", "Super Enhancers", "reference"),
    METHYLATION("methylation", "Methylation", "Methylation", "reference"),
    CRISPR("crispr", "CRISPR", "CRISPR", "reference");

    private final String value;
    private final String recordLabel;
    private final String sourceLabel;
    private final String scope;

    BedtoolsAnnotationType(String value, String recordLabel, String sourceLabel, String scope) {
        this.value = value;
        this.recordLabel = recordLabel;
        this.sourceLabel = sourceLabel;
        this.scope = scope;
    }

    public String value() {
        return value;
    }

    public String recordLabel() {
        return recordLabel;
    }

    public String sourceLabel() {
        return sourceLabel;
    }

    public String scope() {
        return scope;
    }

    public static BedtoolsAnnotationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (BedtoolsAnnotationType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public static List<String> allowedValues() {
        return Arrays.stream(values()).map(BedtoolsAnnotationType::value).toList();
    }
}

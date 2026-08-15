package com.oscar.backend.service;

import com.oscar.backend.entity.RegulatoryNetworkEdge;
import com.oscar.backend.entity.RegulatoryNetworkExpansionResponse;
import com.oscar.backend.entity.RegulatoryNetworkGraphSummary;
import com.oscar.backend.entity.RegulatoryNetworkLink;
import com.oscar.backend.entity.RegulatoryNetworkLinkPageResponse;
import com.oscar.backend.entity.RegulatoryNetworkNode;
import com.oscar.backend.entity.RegulatoryNetworkRange;
import com.oscar.backend.entity.RegulatoryNetworkResponse;
import com.oscar.backend.entity.RegulatoryNetworkSummaryRow;
import com.oscar.backend.entity.RegulatoryNetworkTopItemRow;
import com.oscar.backend.mapper.RegulatoryNetworkMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegulatoryNetworkServiceImpl implements RegulatoryNetworkService {

    private static final String DEFAULT_DOMAIN = "integration";
    private static final int DEFAULT_MAX_NODES = 80;
    private static final int HARD_MAX_NODES = 120;
    private static final int DEFAULT_MAX_EDGES = 120;
    private static final int HARD_MAX_EDGES = 200;
    private static final int DEFAULT_MAX_NEIGHBORS = 40;
    private static final int HARD_MAX_NEIGHBORS = 80;
    private static final int LOCAL_NODE_LINK_LIMIT = 30;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int HARD_PAGE_SIZE = 100;
    private static final int BALANCED_PER_GENE_PEAK_LIMIT = 6;
    private static final int MIN_BALANCED_TOP_GENE_LIMIT = 2;
    private static final int HARD_BALANCED_TOP_GENE_LIMIT = 20;
    private static final Set<String> DOMAINS = Set.of("integration", "rna", "atac");
    private static final Set<String> MODES = Set.of("gene", "peak");
    private static final Set<String> NODE_TYPES = Set.of("gene", "peak");
    private static final Pattern GENE_SYMBOL_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");
    private static final Pattern PEAK_REGION_PATTERN = Pattern.compile(
            "^chr[A-Za-z0-9_.-]+:(\\d+)-(\\d+)$",
            Pattern.CASE_INSENSITIVE
    );

    private final RegulatoryNetworkMapper regulatoryNetworkMapper;

    public RegulatoryNetworkServiceImpl(RegulatoryNetworkMapper regulatoryNetworkMapper) {
        this.regulatoryNetworkMapper = regulatoryNetworkMapper;
    }

    @Override
    public RegulatoryNetworkResponse getRegulatoryNetwork(
            String datasetId,
            String domain,
            String mode,
            String gene,
            String peak,
            Double minScore,
            Integer maxNodes,
            Integer maxEdges
    ) {
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        String normalizedMode = normalizeMode(mode);
        String normalizedGene = normalizeGeneSymbol(gene);
        String normalizedPeak = normalizePeakRegion(peak);
        Double normalizedMinScore = normalizeMinScore(minScore);
        int nodeLimit = clampLimit(maxNodes, DEFAULT_MAX_NODES, HARD_MAX_NODES);
        int edgeLimit = clampLimit(maxEdges, DEFAULT_MAX_EDGES, HARD_MAX_EDGES);

        List<RegulatoryNetworkLink> links;
        RegulatoryNetworkGraphSummary summary = null;
        int responseEdgeLimit = edgeLimit;
        if ("gene".equals(normalizedMode) && normalizedGene != null) {
            responseEdgeLimit = localNodeLinkLimit(edgeLimit);
            Long totalLinks = regulatoryNetworkMapper.countGeneLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedGene,
                    normalizedPeak,
                    normalizedMinScore
            );
            summary = newGraphSummary(geneNodeId(normalizedGene), "gene", totalLinks, responseEdgeLimit);
            links = regulatoryNetworkMapper.selectGeneLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedGene,
                    false,
                    normalizedPeak,
                    normalizedMinScore,
                    responseEdgeLimit
            );
        } else if ("peak".equals(normalizedMode) && normalizedPeak != null) {
            responseEdgeLimit = localNodeLinkLimit(edgeLimit);
            Long totalLinks = regulatoryNetworkMapper.countPeakLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedPeak,
                    normalizedGene,
                    normalizedMinScore
            );
            summary = newGraphSummary(peakNodeId(normalizedPeak), "peak", totalLinks, responseEdgeLimit);
            links = regulatoryNetworkMapper.selectPeakLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedPeak,
                    normalizedGene,
                    normalizedMinScore,
                    responseEdgeLimit
            );
        } else if ("peak".equals(normalizedMode) && normalizedGene != null) {
            responseEdgeLimit = localNodeLinkLimit(edgeLimit);
            Long totalLinks = regulatoryNetworkMapper.countGeneLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedGene,
                    null,
                    normalizedMinScore
            );
            summary = newGraphSummary(geneNodeId(normalizedGene), "gene", totalLinks, responseEdgeLimit);
            links = regulatoryNetworkMapper.selectGeneLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedGene,
                    false,
                    null,
                    normalizedMinScore,
                    responseEdgeLimit
            );
        } else {
            BalancedQueryLimits balancedLimits = balancedQueryLimits(edgeLimit);
            links = regulatoryNetworkMapper.selectBalancedOverviewLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    normalizedMinScore,
                    balancedLimits.topGeneLimit(),
                    balancedLimits.perGenePeakLimit(),
                    edgeLimit
            );
        }

        return buildResponse(
                normalizedDatasetId,
                normalizedDomain,
                normalizedMinScore,
                normalizedGene,
                normalizedPeak,
                links,
                nodeLimit,
                responseEdgeLimit,
                summary
        );
    }

    @Override
    public RegulatoryNetworkExpansionResponse expandRegulatoryNetwork(
            String datasetId,
            String domain,
            String nodeId,
            String nodeType,
            String gene,
            String peak,
            Double minScore,
            Integer maxNeighbors
    ) {
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        String normalizedNodeId = normalizeRequired(nodeId, "nodeId");
        String normalizedNodeType = normalizeNodeType(nodeType);
        String normalizedGene = normalizeGeneSymbol(gene);
        String normalizedPeak = normalizePeakRegion(peak);
        Double normalizedMinScore = normalizeMinScore(minScore);
        int neighborLimit = clampLimit(maxNeighbors, DEFAULT_MAX_NEIGHBORS, HARD_MAX_NEIGHBORS);

        String nodeValue = normalizeNodeValue(normalizedNodeId, normalizedNodeType);
        if (nodeValue == null) {
            return emptyExpansionResponse();
        }

        List<RegulatoryNetworkLink> links;
        RegulatoryNetworkGraphSummary summary = null;
        int responseLinkLimit = neighborLimit;
        if ("gene".equals(normalizedNodeType)) {
            responseLinkLimit = localNodeLinkLimit(neighborLimit);
            Long totalLinks = regulatoryNetworkMapper.countGeneLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    nodeValue,
                    normalizedPeak,
                    normalizedMinScore
            );
            summary = newGraphSummary(geneNodeId(nodeValue), "gene", totalLinks, responseLinkLimit);
            links = regulatoryNetworkMapper.selectGeneLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    nodeValue,
                    false,
                    normalizedPeak,
                    normalizedMinScore,
                    responseLinkLimit
            );
        } else if ("peak".equals(normalizedNodeType)) {
            responseLinkLimit = localNodeLinkLimit(neighborLimit);
            Long totalLinks = regulatoryNetworkMapper.countPeakLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    nodeValue,
                    normalizedGene,
                    normalizedMinScore
            );
            summary = newGraphSummary(peakNodeId(nodeValue), "peak", totalLinks, responseLinkLimit);
            links = regulatoryNetworkMapper.selectPeakLinks(
                    normalizedDatasetId,
                    normalizedDomain,
                    nodeValue,
                    normalizedGene,
                    normalizedMinScore,
                    responseLinkLimit
            );
        } else {
            links = List.of();
        }

        RegulatoryNetworkResponse response = buildResponse(
                normalizedDatasetId,
                normalizedDomain,
                normalizedMinScore,
                normalizedGene,
                normalizedPeak,
                links,
                HARD_MAX_NODES,
                responseLinkLimit,
                summary
        );
        RegulatoryNetworkExpansionResponse expansionResponse = new RegulatoryNetworkExpansionResponse(
                response.getNodes(),
                response.getEdges(),
                response.getLinks(),
                response.getTotalLinks(),
                response.getHasMore(),
                response.getSummary() == null ? (long) response.getLinks().size() : response.getSummary().getTotalLinks()
        );
        expansionResponse.setSummary(response.getSummary());
        return expansionResponse;
    }

    @Override
    public RegulatoryNetworkLinkPageResponse getRegulatoryNetworkLinks(
            String datasetId,
            String domain,
            String nodeType,
            String nodeId,
            Integer page,
            Integer pageSize,
            String gene,
            String peak,
            Double minScore
    ) {
        String normalizedDatasetId = normalizeRequired(datasetId, "datasetId");
        String normalizedDomain = normalizeDomain(domain);
        String normalizedNodeType = normalizeNodeType(nodeType);
        String normalizedNodeId = normalizeRequired(nodeId, "nodeId");
        String normalizedGene = normalizeGeneSymbol(gene);
        String normalizedPeak = normalizePeakRegion(peak);
        Double normalizedMinScore = normalizeMinScore(minScore);
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = clampLimit(pageSize, DEFAULT_PAGE_SIZE, HARD_PAGE_SIZE);
        int offset = pageOffset(normalizedPage, normalizedPageSize);

        String nodeValue = normalizeNodeValue(normalizedNodeId, normalizedNodeType);
        if (nodeValue == null) {
            return new RegulatoryNetworkLinkPageResponse(0L, normalizedPage, normalizedPageSize, List.of());
        }

        PagedLinks pagedLinks = selectPagedLinks(
                normalizedDatasetId,
                normalizedDomain,
                normalizedNodeType,
                nodeValue,
                normalizedGene,
                normalizedPeak,
                normalizedMinScore,
                normalizedPageSize,
                offset
        );
        List<RegulatoryNetworkLink> items = pagedLinks.items().stream()
                .map(this::normalizeLink)
                .filter(Objects::nonNull)
                .toList();
        return new RegulatoryNetworkLinkPageResponse(
                pagedLinks.total(),
                normalizedPage,
                normalizedPageSize,
                items
        );
    }

    private PagedLinks selectPagedLinks(
            String datasetId,
            String domain,
            String nodeType,
            String nodeValue,
            String geneFilter,
            String peakFilter,
            Double minScore,
            int limit,
            int offset
    ) {
        Long total = switch (nodeType) {
            case "gene" -> regulatoryNetworkMapper.countGeneLinks(datasetId, domain, nodeValue, peakFilter, minScore);
            case "peak" -> regulatoryNetworkMapper.countPeakLinks(datasetId, domain, nodeValue, geneFilter, minScore);
            default -> 0L;
        };
        long normalizedTotal = defaultLong(total);
        if (normalizedTotal == 0L) {
            return new PagedLinks(0L, List.of());
        }

        List<RegulatoryNetworkLink> items = switch (nodeType) {
            case "gene" -> regulatoryNetworkMapper.selectGeneLinksPage(datasetId, domain, nodeValue, peakFilter, minScore, limit, offset);
            case "peak" -> regulatoryNetworkMapper.selectPeakLinksPage(datasetId, domain, nodeValue, geneFilter, minScore, limit, offset);
            default -> List.of();
        };
        return new PagedLinks(normalizedTotal, items);
    }

    private BalancedQueryLimits balancedQueryLimits(int finalLinkLimit) {
        int perGenePeakLimit = BALANCED_PER_GENE_PEAK_LIMIT;
        int topGeneLimit = (finalLinkLimit + perGenePeakLimit - 1) / perGenePeakLimit;
        topGeneLimit = Math.max(MIN_BALANCED_TOP_GENE_LIMIT, topGeneLimit);
        topGeneLimit = Math.min(HARD_BALANCED_TOP_GENE_LIMIT, topGeneLimit);
        return new BalancedQueryLimits(topGeneLimit, perGenePeakLimit);
    }

    private RegulatoryNetworkResponse buildResponse(
            String datasetId,
            String domain,
            Double minScore,
            String geneFilter,
            String peakFilter,
            List<RegulatoryNetworkLink> rawLinks,
            int maxNodes,
            int maxEdges,
            RegulatoryNetworkGraphSummary summary
    ) {
        List<RegulatoryNetworkLink> normalizedLinks = rawLinks == null
                ? List.of()
                : rawLinks.stream()
                .map(this::normalizeLink)
                .filter(Objects::nonNull)
                .toList();
        List<RegulatoryNetworkLink> links = limitLinksByBudget(normalizedLinks, maxNodes, maxEdges);
        List<RegulatoryNetworkNode> nodes = buildNodes(
                datasetId,
                domain,
                minScore,
                geneFilter,
                peakFilter,
                links
        );
        List<RegulatoryNetworkEdge> edges = List.of();
        boolean hasMore = normalizedLinks.size() > links.size() || normalizedLinks.size() >= maxEdges;
        if (summary != null) {
            summary.setReturnedLinks((long) links.size());
            summary.setHasMoreLinks(defaultLong(summary.getTotalLinks()) > links.size());
            decorateLinksWithGraphSummary(links, summary);
            decorateAnchorNodeWithGraphSummary(nodes, links, summary);
            hasMore = Boolean.TRUE.equals(summary.getHasMoreLinks());
        }

        long totalLinks = summary == null ? links.size() : defaultLong(summary.getTotalLinks());
        return new RegulatoryNetworkResponse(nodes, edges, links, totalLinks, hasMore, summary);
    }

    private void decorateLinksWithGraphSummary(
            List<RegulatoryNetworkLink> links,
            RegulatoryNetworkGraphSummary summary
    ) {
        for (RegulatoryNetworkLink link : links) {
            link.setGraphLimit(summary.getGraphLimit());
            link.setHasMoreLinks(summary.getHasMoreLinks());
            if ("gene".equals(summary.getAnchorNodeType())) {
                link.setLinkedPeaksCount(summary.getTotalLinks());
            } else if ("peak".equals(summary.getAnchorNodeType())) {
                link.setLinkedGenesCount(summary.getTotalLinks());
            }
        }
    }

    private void decorateAnchorNodeWithGraphSummary(
            List<RegulatoryNetworkNode> nodes,
            List<RegulatoryNetworkLink> links,
            RegulatoryNetworkGraphSummary summary
    ) {
        RegulatoryNetworkNode anchor = nodes.stream()
                .filter(node -> summary.getAnchorNodeId().equals(node.getId()))
                .findFirst()
                .orElse(null);
        if (anchor == null) {
            return;
        }

        long totalLinks = defaultLong(summary.getTotalLinks());
        if ("gene".equals(summary.getAnchorNodeType())) {
            List<String> peaks = links.stream()
                    .map(RegulatoryNetworkLink::getPeak)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            anchor.setLinkedPeaksCount(totalLinks);
            anchor.setTopLinkedPeaks(limitTopItems(peaks));
            anchor.setRemainingLinkedPeaksCount(Math.max(0L, totalLinks - anchor.getTopLinkedPeaks().size()));
        } else if ("peak".equals(summary.getAnchorNodeType())) {
            List<String> genes = links.stream()
                    .map(RegulatoryNetworkLink::getGeneSymbol)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            anchor.setLinkedGenesCount(totalLinks);
            anchor.setTopLinkedGenes(limitTopItems(genes));
            anchor.setRemainingLinkedGenesCount(Math.max(0L, totalLinks - anchor.getTopLinkedGenes().size()));
        }
    }

    private RegulatoryNetworkGraphSummary newGraphSummary(
            String anchorNodeId,
            String anchorNodeType,
            Long totalLinks,
            int graphLimit
    ) {
        return new RegulatoryNetworkGraphSummary(
                anchorNodeId,
                anchorNodeType,
                defaultLong(totalLinks),
                0L,
                graphLimit,
                false
        );
    }

    private RegulatoryNetworkLink normalizeLink(RegulatoryNetworkLink link) {
        String peak = trimToNull(link.getPeak());
        String gene = trimToNull(link.getGeneSymbol());
        if (peak == null || gene == null) {
            return null;
        }

        link.setPeak(peak);
        link.setPeakId(peak);
        link.setGeneSymbol(gene);
        link.setLinkedGene(gene);
        link.setDatasetId(trimToNull(link.getDatasetId()));
        link.setSampleName(trimToNull(link.getSampleName()));
        link.setDomain(trimToNull(link.getDomain()));
        Double score = link.getLinkScore() == null ? link.getScore() : link.getLinkScore();
        link.setScore(score);
        link.setLinkScore(score);
        link.setDistanceToTss(null);
        link.setLinkType("peak_to_gene");
        link.setProvenanceSource(trimToNull(link.getProvenanceSource()));
        link.setSource(getDisplaySource(link));
        link.setTfName(trimToNull(link.getTfName()));
        return link;
    }

    private String getDisplaySource(RegulatoryNetworkLink link) {
        String sampleName = trimToNull(link.getSampleName());
        if (sampleName != null) {
            return sampleName;
        }

        String datasetId = trimToNull(link.getDatasetId());
        if (datasetId != null) {
            return datasetId;
        }

        String source = trimToNull(link.getSource());
        return "frontend_import".equals(source) ? null : source;
    }

    private List<RegulatoryNetworkLink> limitLinksByBudget(
            List<RegulatoryNetworkLink> links,
            int maxNodes,
            int maxEdges
    ) {
        List<RegulatoryNetworkLink> selectedLinks = new ArrayList<>();
        Set<String> nodeIds = new LinkedHashSet<>();

        for (RegulatoryNetworkLink link : links) {
            if (selectedLinks.size() >= maxEdges) {
                break;
            }

            Set<String> candidateNodes = getNodeIdsForLink(link);
            Set<String> nextNodeIds = new LinkedHashSet<>(nodeIds);
            nextNodeIds.addAll(candidateNodes);
            if (nextNodeIds.size() > maxNodes) {
                continue;
            }

            selectedLinks.add(link);
            nodeIds = nextNodeIds;
        }

        return selectedLinks;
    }

    private Set<String> getNodeIdsForLink(RegulatoryNetworkLink link) {
        Set<String> nodeIds = new LinkedHashSet<>();
        nodeIds.add(geneNodeId(link.getGeneSymbol()));
        nodeIds.add(peakNodeId(link.getPeak()));
        return nodeIds;
    }

    private List<RegulatoryNetworkNode> buildNodes(
            String datasetId,
            String domain,
            Double minScore,
            String geneFilter,
            String peakFilter,
            List<RegulatoryNetworkLink> links
    ) {
        Map<String, RegulatoryNetworkNode> nodeMap = new LinkedHashMap<>();
        List<String> genes = links.stream()
                .map(RegulatoryNetworkLink::getGeneSymbol)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> peaks = links.stream()
                .map(RegulatoryNetworkLink::getPeak)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, RegulatoryNetworkSummaryRow> geneSummaries = selectGeneSummaryMap(
                datasetId,
                domain,
                genes,
                peakFilter,
                minScore
        );
        Map<String, List<String>> topPeaksByGene = selectTopPeaksByGene(
                datasetId,
                domain,
                genes,
                peakFilter,
                minScore
        );
        Map<String, RegulatoryNetworkSummaryRow> peakSummaries = selectPeakSummaryMap(
                datasetId,
                domain,
                peaks,
                geneFilter,
                minScore
        );
        Map<String, List<String>> topGenesByPeak = selectTopGenesByPeak(
                datasetId,
                domain,
                peaks,
                geneFilter,
                minScore
        );

        for (String gene : genes) {
            RegulatoryNetworkNode node = new RegulatoryNetworkNode();
            node.setId(geneNodeId(gene));
            node.setType("gene");
            node.setLabel(gene);
            applySharedNodeContext(node, links);
            applyGeneSummary(node, geneSummaries.get(gene), topPeaksByGene.getOrDefault(gene, List.of()));
            nodeMap.put(node.getId(), node);
        }

        for (String peak : peaks) {
            RegulatoryNetworkNode node = new RegulatoryNetworkNode();
            node.setId(peakNodeId(peak));
            node.setType("peak");
            node.setLabel(peak);
            applySharedNodeContext(node, links);
            applyPeakSummary(node, peakSummaries.get(peak), topGenesByPeak.getOrDefault(peak, List.of()));
            nodeMap.put(node.getId(), node);
        }

        return new ArrayList<>(nodeMap.values());
    }

    private void applySharedNodeContext(RegulatoryNetworkNode node, List<RegulatoryNetworkLink> links) {
        RegulatoryNetworkLink first = links.stream()
                .filter(link -> {
                    if ("gene".equals(node.getType())) {
                        return node.getLabel().equals(link.getGeneSymbol());
                    }
                    if ("peak".equals(node.getType())) {
                        return node.getLabel().equals(link.getPeak());
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
        if (first == null) {
            return;
        }

        node.setDatasetId(first.getDatasetId());
        node.setSampleName(first.getSampleName());
        node.setDomain(first.getDomain());
    }

    private Map<String, RegulatoryNetworkSummaryRow> selectGeneSummaryMap(
            String datasetId,
            String domain,
            List<String> genes,
            String peakFilter,
            Double minScore
    ) {
        if (genes.isEmpty()) {
            return Map.of();
        }

        Map<String, RegulatoryNetworkSummaryRow> summaryMap = new LinkedHashMap<>();
        regulatoryNetworkMapper.selectGeneSummaries(datasetId, domain, genes, peakFilter, minScore)
                .forEach(summary -> summaryMap.put(summary.getNodeKey(), summary));
        return summaryMap;
    }

    private Map<String, List<String>> selectTopPeaksByGene(
            String datasetId,
            String domain,
            List<String> genes,
            String peakFilter,
            Double minScore
    ) {
        if (genes.isEmpty()) {
            return Map.of();
        }

        return groupTopItems(regulatoryNetworkMapper.selectTopPeaksForGenes(
                datasetId,
                domain,
                genes,
                peakFilter,
                minScore
        ));
    }

    private Map<String, RegulatoryNetworkSummaryRow> selectPeakSummaryMap(
            String datasetId,
            String domain,
            List<String> peaks,
            String geneFilter,
            Double minScore
    ) {
        if (peaks.isEmpty()) {
            return Map.of();
        }

        Map<String, RegulatoryNetworkSummaryRow> summaryMap = new LinkedHashMap<>();
        regulatoryNetworkMapper.selectPeakSummaries(datasetId, domain, peaks, geneFilter, minScore)
                .forEach(summary -> summaryMap.put(summary.getNodeKey(), summary));
        return summaryMap;
    }

    private Map<String, List<String>> selectTopGenesByPeak(
            String datasetId,
            String domain,
            List<String> peaks,
            String geneFilter,
            Double minScore
    ) {
        if (peaks.isEmpty()) {
            return Map.of();
        }

        return groupTopItems(regulatoryNetworkMapper.selectTopGenesForPeaks(
                datasetId,
                domain,
                peaks,
                geneFilter,
                minScore
        ));
    }

    private Map<String, List<String>> groupTopItems(List<RegulatoryNetworkTopItemRow> rows) {
        Map<String, List<String>> itemMap = new LinkedHashMap<>();
        for (RegulatoryNetworkTopItemRow row : rows) {
            itemMap.computeIfAbsent(row.getNodeKey(), ignored -> new ArrayList<>()).add(row.getItem());
        }
        return itemMap;
    }

    private void applyGeneSummary(
            RegulatoryNetworkNode node,
            RegulatoryNetworkSummaryRow summary,
            List<String> topLinkedPeaks
    ) {
        if (summary == null) {
            node.setLinkedPeaksCount(0L);
            node.setTopLinkedPeaks(List.of());
            node.setRemainingLinkedPeaksCount(0L);
            return;
        }

        long total = defaultLong(summary.getLinkedCount());
        node.setLinkedPeaksCount(total);
        node.setTopLinkedPeaks(limitTopItems(topLinkedPeaks));
        node.setRemainingLinkedPeaksCount(Math.max(0L, total - node.getTopLinkedPeaks().size()));
        applyCommonSummary(node, summary);
    }

    private void applyPeakSummary(
            RegulatoryNetworkNode node,
            RegulatoryNetworkSummaryRow summary,
            List<String> topLinkedGenes
    ) {
        if (summary == null) {
            node.setLinkedGenesCount(0L);
            node.setTopLinkedGenes(List.of());
            node.setRemainingLinkedGenesCount(0L);
            return;
        }

        long total = defaultLong(summary.getLinkedCount());
        node.setLinkedGenesCount(total);
        node.setTopLinkedGenes(limitTopItems(topLinkedGenes));
        node.setRemainingLinkedGenesCount(Math.max(0L, total - node.getTopLinkedGenes().size()));
        applyCommonSummary(node, summary);
        node.setDistanceRange(null);
    }

    private void applyCommonSummary(RegulatoryNetworkNode node, RegulatoryNetworkSummaryRow summary) {
        node.setMaxLinkScore(summary.getMaxLinkScore());
        node.setCorrelationRange(toRange(summary.getMinCorrelation(), summary.getMaxCorrelation()));
        node.setMinFdr(summary.getMinFdr());
        node.setFdrRange(toRange(summary.getMinFdr(), summary.getMaxFdr()));
    }

    private List<String> limitTopItems(List<String> items) {
        return items.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .toList();
    }

    private RegulatoryNetworkRange toRange(Double min, Double max) {
        if (min == null && max == null) {
            return null;
        }
        return new RegulatoryNetworkRange(min, max);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private int localNodeLinkLimit(int requestedLimit) {
        return Math.min(requestedLimit, LOCAL_NODE_LINK_LIMIT);
    }

    private RegulatoryNetworkExpansionResponse emptyExpansionResponse() {
        return new RegulatoryNetworkExpansionResponse(List.of(), List.of(), List.of(), 0L, false, 0L);
    }

    private String normalizeDomain(String domain) {
        String normalized = trimToNull(domain);
        String value = normalized == null ? DEFAULT_DOMAIN : normalized.toLowerCase(Locale.ROOT);
        if (!DOMAINS.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain must be integration, rna, or atac");
        }
        return value;
    }

    private String normalizeMode(String mode) {
        String value = normalizeRequired(mode, "mode").toLowerCase(Locale.ROOT);
        if (!MODES.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode must be gene or peak");
        }
        return value;
    }

    private String normalizeNodeType(String nodeType) {
        String value = normalizeRequired(nodeType, "nodeType").toLowerCase(Locale.ROOT);
        if (!NODE_TYPES.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nodeType must be gene or peak");
        }
        return value;
    }

    private Double normalizeMinScore(Double minScore) {
        if (minScore == null || minScore.isNaN() || minScore.isInfinite()) {
            return null;
        }
        return minScore;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int pageOffset(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    private int clampLimit(Integer requestedLimit, int defaultValue, int hardMax) {
        if (requestedLimit == null || requestedLimit < 1) {
            return defaultValue;
        }
        return Math.min(requestedLimit, hardMax);
    }

    private String normalizeRequired(String value, String parameterName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " is required");
        }
        return normalized;
    }

    private String stripNodePrefix(String nodeId, String nodeType) {
        String prefix = nodeType + ":";
        return nodeId.startsWith(prefix) ? nodeId.substring(prefix.length()) : nodeId;
    }

    private String normalizeGeneSymbol(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!GENE_SYMBOL_PATTERN.matcher(trimmed).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "gene must be a single gene symbol using letters, numbers, '.', '_' or '-'"
            );
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizePeakRegion(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        Matcher matcher = PEAK_REGION_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw invalidPeakRegion();
        }

        try {
            long start = Long.parseLong(matcher.group(1));
            long end = Long.parseLong(matcher.group(2));
            if (start < 0 || end <= start) {
                throw invalidPeakRegion();
            }
        } catch (NumberFormatException exception) {
            throw invalidPeakRegion();
        }
        return trimmed;
    }

    private String normalizeNodeValue(String nodeId, String nodeType) {
        String value = trimToNull(stripNodePrefix(nodeId, nodeType));
        if (value == null) {
            return null;
        }
        return "gene".equals(nodeType) ? normalizeGeneSymbol(value) : normalizePeakRegion(value);
    }

    private ResponseStatusException invalidPeakRegion() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "peak must be a single region in chr:start-end format with end greater than start"
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private String geneNodeId(String gene) {
        return "gene:" + gene;
    }

    private String peakNodeId(String peak) {
        return "peak:" + peak;
    }

    private record BalancedQueryLimits(int topGeneLimit, int perGenePeakLimit) {
    }

    private record PagedLinks(long total, List<RegulatoryNetworkLink> items) {
    }
}

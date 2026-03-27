package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SemanticDiffService {

    private static final Logger log = LoggerFactory.getLogger(SemanticDiffService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String SYSTEM_PROMPT = """
            Tu analyses la différence entre deux synthèses juridiques (FROM et TO) du même dossier.
            Pour chaque item, assigne un état :
            - unchanged : sémantiquement identique dans FROM et TO
            - added : nouveau dans TO, absent dans FROM
            - removed : présent dans FROM, absent dans TO
            - enriched : même concept dans FROM et TO mais reformulé ou enrichi dans TO
            Fournis une "reason" courte (20 mots max) pour added/removed/enriched. Pour unchanged, reason = null.
            Réponds UNIQUEMENT avec un JSON valide, sans texte avant ni après.
            Format exact :
            {"faits":[{"text":"...","state":"unchanged|added|removed|enriched","reason":null|"..."}],"points_juridiques":[...],"risques":[...],"questions_ouvertes":[...],"timeline":[{"date":"...","evenement":"...","state":"...","reason":null|"..."}]}
            """;

    private final AnthropicService anthropicService;
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;

    public SemanticDiffService(AnthropicService anthropicService,
                               AiQuestionRepository aiQuestionRepository,
                               AiQuestionAnswerRepository aiQuestionAnswerRepository) {
        this.anthropicService = anthropicService;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
    }

    public AnalysisDiffResponse diff(CaseAnalysis fromAnalysis, CaseAnalysis toAnalysis,
                                     List<AnalysisDocument> fromDocs, List<AnalysisDocument> toDocs) {
        CaseAnalysisResponse from = CaseAnalysisResponse.from(fromAnalysis);
        CaseAnalysisResponse to = CaseAnalysisResponse.from(toAnalysis);

        UUID caseFileId = toAnalysis.getCaseFile().getId();

        try {
            String prompt = buildPrompt(from, to, fromDocs, toDocs, toAnalysis.getAnalysisType(), caseFileId);
            AnthropicResult result = anthropicService.analyzeFast(SYSTEM_PROMPT, prompt, 2048);
            return parseHaikuResponse(result.content(), fromAnalysis, toAnalysis);
        } catch (Exception e) {
            log.warn("Semantic diff failed for analyses {}/{}, falling back to exact diff: {}",
                    fromAnalysis.getId(), toAnalysis.getId(), e.getMessage());
            return exactDiff(from, to, fromAnalysis, toAnalysis);
        }
    }

    String buildPrompt(CaseAnalysisResponse from, CaseAnalysisResponse to,
                       List<AnalysisDocument> fromDocs, List<AnalysisDocument> toDocs,
                       AnalysisType toType, UUID caseFileId) {
        Set<String> fromDocNames = fromDocs.stream()
                .map(AnalysisDocument::getDocumentName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> toDocNames = toDocs.stream()
                .map(AnalysisDocument::getDocumentName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> addedDocs = toDocNames.stream().filter(n -> !fromDocNames.contains(n)).toList();
        List<String> removedDocs = fromDocNames.stream().filter(n -> !toDocNames.contains(n)).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("[Contexte — documents]\n");
        sb.append("Documents ajoutés dans TO : ")
                .append(addedDocs.isEmpty() ? "aucun" : String.join(", ", addedDocs)).append("\n");
        sb.append("Documents retirés dans TO : ")
                .append(removedDocs.isEmpty() ? "aucun" : String.join(", ", removedDocs)).append("\n\n");

        if (toType == AnalysisType.ENRICHED) {
            sb.append("[Q&R de l'avocat]\n");
            List<AiQuestion> questions = aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId);
            List<AiQuestion> answered = questions.stream()
                    .filter(q -> "ANSWERED".equals(q.getStatus())).toList();
            if (answered.isEmpty()) {
                sb.append("(aucune réponse)\n");
            } else {
                for (int i = 0; i < answered.size(); i++) {
                    AiQuestion q = answered.get(i);
                    String answer = aiQuestionAnswerRepository
                            .findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                            .map(AiQuestionAnswer::getAnswerText)
                            .orElse("(sans réponse)");
                    sb.append("Q").append(i + 1).append(" : ").append(q.getQuestionText()).append("\n");
                    sb.append("R").append(i + 1).append(" : ").append(answer).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("[FROM]\n");
        appendSection(sb, "faits", from.faits());
        appendSection(sb, "points_juridiques", from.pointsJuridiques());
        appendSection(sb, "risques", from.risques());
        appendSection(sb, "questions_ouvertes", from.questionsOuvertes());
        appendTimeline(sb, "timeline", from.timeline());

        sb.append("\n[TO]\n");
        appendSection(sb, "faits", to.faits());
        appendSection(sb, "points_juridiques", to.pointsJuridiques());
        appendSection(sb, "risques", to.risques());
        appendSection(sb, "questions_ouvertes", to.questionsOuvertes());
        appendTimeline(sb, "timeline", to.timeline());

        return sb.toString();
    }

    AnalysisDiffResponse parseHaikuResponse(String json, CaseAnalysis fromAnalysis,
                                            CaseAnalysis toAnalysis) throws Exception {
        String cleaned = CaseAnalysisResponse.stripMarkdownCodeBlock(json);
        JsonNode root = MAPPER.readTree(cleaned);

        return new AnalysisDiffResponse(
                toVersionInfo(fromAnalysis),
                toVersionInfo(toAnalysis),
                parseSectionDiff(root, "faits"),
                parseSectionDiff(root, "points_juridiques"),
                parseSectionDiff(root, "risques"),
                parseSectionDiff(root, "questions_ouvertes"),
                parseTimelineSectionDiff(root, "timeline")
        );
    }

    AnalysisDiffResponse exactDiff(CaseAnalysisResponse from, CaseAnalysisResponse to,
                                   CaseAnalysis fromAnalysis, CaseAnalysis toAnalysis) {
        return new AnalysisDiffResponse(
                toVersionInfo(fromAnalysis),
                toVersionInfo(toAnalysis),
                exactSectionDiff(from.faits(), to.faits()),
                exactSectionDiff(from.pointsJuridiques(), to.pointsJuridiques()),
                exactSectionDiff(from.risques(), to.risques()),
                exactSectionDiff(from.questionsOuvertes(), to.questionsOuvertes()),
                exactTimelineDiff(from.timeline(), to.timeline())
        );
    }

    private AnalysisDiffResponse.SectionDiff parseSectionDiff(JsonNode root, String field) {
        JsonNode node = root.path(field);
        List<AnalysisDiffResponse.DiffItem> added = new ArrayList<>();
        List<AnalysisDiffResponse.DiffItem> removed = new ArrayList<>();
        List<AnalysisDiffResponse.DiffItem> unchanged = new ArrayList<>();
        List<AnalysisDiffResponse.DiffItem> enriched = new ArrayList<>();

        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = item.path("text").asText("");
                String state = item.path("state").asText("added");
                String reason = item.path("reason").isNull() ? null : item.path("reason").asText(null);
                AnalysisDiffResponse.DiffItem diffItem = new AnalysisDiffResponse.DiffItem(text, reason);
                switch (state) {
                    case "removed" -> removed.add(diffItem);
                    case "unchanged" -> unchanged.add(new AnalysisDiffResponse.DiffItem(text, null));
                    case "enriched" -> enriched.add(diffItem);
                    default -> added.add(diffItem);
                }
            }
        }
        return new AnalysisDiffResponse.SectionDiff(added, removed, unchanged, enriched);
    }

    private AnalysisDiffResponse.TimelineSectionDiff parseTimelineSectionDiff(JsonNode root, String field) {
        JsonNode node = root.path(field);
        List<AnalysisDiffResponse.TimelineDiffItem> added = new ArrayList<>();
        List<AnalysisDiffResponse.TimelineDiffItem> removed = new ArrayList<>();
        List<AnalysisDiffResponse.TimelineDiffItem> unchanged = new ArrayList<>();
        List<AnalysisDiffResponse.TimelineDiffItem> enriched = new ArrayList<>();

        if (node.isArray()) {
            for (JsonNode item : node) {
                String date = item.path("date").asText("");
                String evenement = item.path("evenement").asText("");
                String state = item.path("state").asText("added");
                String reason = item.path("reason").isNull() ? null : item.path("reason").asText(null);
                AnalysisDiffResponse.TimelineDiffItem diffItem =
                        new AnalysisDiffResponse.TimelineDiffItem(date, evenement, reason);
                switch (state) {
                    case "removed" -> removed.add(diffItem);
                    case "unchanged" -> unchanged.add(new AnalysisDiffResponse.TimelineDiffItem(date, evenement, null));
                    case "enriched" -> enriched.add(diffItem);
                    default -> added.add(diffItem);
                }
            }
        }
        return new AnalysisDiffResponse.TimelineSectionDiff(added, removed, unchanged, enriched);
    }

    private AnalysisDiffResponse.SectionDiff exactSectionDiff(List<String> from, List<String> to) {
        Set<String> fromSet = new LinkedHashSet<>(from);
        Set<String> toSet = new LinkedHashSet<>(to);

        List<AnalysisDiffResponse.DiffItem> unchanged = new ArrayList<>();
        List<AnalysisDiffResponse.DiffItem> removed = new ArrayList<>();
        for (String item : fromSet) {
            if (toSet.contains(item)) unchanged.add(new AnalysisDiffResponse.DiffItem(item, null));
            else removed.add(new AnalysisDiffResponse.DiffItem(item, null));
        }
        List<AnalysisDiffResponse.DiffItem> added = toSet.stream()
                .filter(item -> !fromSet.contains(item))
                .map(item -> new AnalysisDiffResponse.DiffItem(item, null))
                .toList();

        return new AnalysisDiffResponse.SectionDiff(added, removed, unchanged, List.of());
    }

    private AnalysisDiffResponse.TimelineSectionDiff exactTimelineDiff(
            List<CaseAnalysisResponse.TimelineEntry> from,
            List<CaseAnalysisResponse.TimelineEntry> to) {
        Set<String> fromKeys = from.stream()
                .map(e -> e.date() + "§" + e.evenement())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> toKeys = to.stream()
                .map(e -> e.date() + "§" + e.evenement())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AnalysisDiffResponse.TimelineDiffItem> unchanged = new ArrayList<>();
        List<AnalysisDiffResponse.TimelineDiffItem> removed = new ArrayList<>();
        for (CaseAnalysisResponse.TimelineEntry e : from) {
            AnalysisDiffResponse.TimelineDiffItem item =
                    new AnalysisDiffResponse.TimelineDiffItem(e.date(), e.evenement(), null);
            if (toKeys.contains(e.date() + "§" + e.evenement())) unchanged.add(item);
            else removed.add(item);
        }
        List<AnalysisDiffResponse.TimelineDiffItem> added = to.stream()
                .filter(e -> !fromKeys.contains(e.date() + "§" + e.evenement()))
                .map(e -> new AnalysisDiffResponse.TimelineDiffItem(e.date(), e.evenement(), null))
                .toList();

        return new AnalysisDiffResponse.TimelineSectionDiff(added, removed, unchanged, List.of());
    }

    private AnalysisDiffResponse.VersionInfo toVersionInfo(CaseAnalysis analysis) {
        return new AnalysisDiffResponse.VersionInfo(
                analysis.getId(), analysis.getVersion(),
                analysis.getAnalysisType().name(), analysis.getUpdatedAt());
    }

    private void appendSection(StringBuilder sb, String name, List<String> items) {
        sb.append(name).append(" : ").append(items.isEmpty() ? "[]" : items.toString()).append("\n");
    }

    private void appendTimeline(StringBuilder sb, String name,
                                List<CaseAnalysisResponse.TimelineEntry> items) {
        if (items.isEmpty()) {
            sb.append(name).append(" : []\n");
            return;
        }
        sb.append(name).append(" : [");
        for (int i = 0; i < items.size(); i++) {
            CaseAnalysisResponse.TimelineEntry e = items.get(i);
            sb.append("{\"date\":\"").append(e.date())
                    .append("\",\"evenement\":\"").append(e.evenement()).append("\"}");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]\n");
    }
}

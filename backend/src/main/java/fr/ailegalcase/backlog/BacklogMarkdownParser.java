package fr.ailegalcase.backlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-178 SF-178-01 : parse les fichiers PRODUCT_SPEC.md et MARKETING_BACKLOG.md
 * pour extraire les features / subfeatures / tâches marketing.
 * Stateless — peut être appelé en parallèle (testé via fixtures).
 */
@Component
public class BacklogMarkdownParser {

    private static final Logger log = LoggerFactory.getLogger(BacklogMarkdownParser.class);

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    private static final Pattern FEATURE_ROW = Pattern.compile("^\\s*\\|\\s*(F-[A-Z\\d-]+?)\\s*\\|");
    private static final Pattern MARKETING_ROW = Pattern.compile("^\\s*\\|\\s*(M-\\d+)\\s*\\|");
    private static final Pattern SUBFEATURE_REF = Pattern.compile("SF-(?:[A-Z]{2,3}-)?\\d{1,3}-\\d{1,2}[a-z]?");
    private static final Pattern SECTION_HEADING = Pattern.compile("^#{2,3}\\s+(.+?)\\s*$");

    /**
     * Order matters: more specific markers (planning markers, absorption) come
     * BEFORE generic markers like "Bloqué" which can appear in any feature's
     * description as a dependency note (e.g. "Bloqué par F-IA-04"). The
     * column "Cible" carries the canonical status — those markers win.
     */
    private static final List<StatusPattern> TECH_STATUS_PATTERNS = List.of(
            new StatusPattern(BacklogStatus.ABSORBED, Pattern.compile("Absorb(?:é|e)e?\\s+par\\s+F-", FLAGS)),
            new StatusPattern(BacklogStatus.DONE, Pattern.compile("(?:🎉\\s*)?\\*?\\*?Termin(?:é|e)e?", FLAGS)),
            new StatusPattern(BacklogStatus.IN_PROGRESS, Pattern.compile("\\bEn\\s+cours\\b", FLAGS)),
            new StatusPattern(BacklogStatus.PARTIAL, Pattern.compile("\\bPartielle?\\b", FLAGS)),
            new StatusPattern(BacklogStatus.READY, Pattern.compile("\\bReady\\s+to\\s+dev\\b|\\bReady\\b", FLAGS)),
            new StatusPattern(BacklogStatus.PLANNED, Pattern.compile("(?:À|A)\\s+planifier|(?:À|A)\\s+sp(?:é|e)cifier", FLAGS)),
            new StatusPattern(BacklogStatus.BLOCKED, Pattern.compile("Bloqu(?:é|e)e?", FLAGS))
    );

    private static final List<StatusPattern> SUBFEATURE_STATUS_PATTERNS = List.of(
            new StatusPattern(BacklogStatus.DONE, Pattern.compile("\\bmerg(?:é|e)e?\\b|\\bDone\\b|Termin(?:é|e)e?", FLAGS)),
            new StatusPattern(BacklogStatus.IN_PROGRESS, Pattern.compile("\\ben\\s+cours\\b|\\bin[\\s-]?progress\\b", FLAGS)),
            new StatusPattern(BacklogStatus.BLOCKED, Pattern.compile("Bloqu(?:é|e)e?|\\bblocked\\b", FLAGS)),
            new StatusPattern(BacklogStatus.READY, Pattern.compile("\\bready\\b", FLAGS))
    );

    private static final List<StatusPattern> MARKETING_STATUS_PATTERNS = List.of(
            new StatusPattern(BacklogMarketingStatus.DONE, Pattern.compile("Termin(?:é|e)", FLAGS)),
            new StatusPattern(BacklogMarketingStatus.IN_PROGRESS, Pattern.compile("\\bEn\\s+cours\\b", FLAGS)),
            new StatusPattern(BacklogMarketingStatus.BLOCKED, Pattern.compile("Bloqu(?:é|e)", FLAGS)),
            new StatusPattern(BacklogMarketingStatus.DRAFTED, Pattern.compile("R(?:é|e)dig(?:é|e)", FLAGS)),
            new StatusPattern(BacklogMarketingStatus.TODO, Pattern.compile("(?:À|A)\\s+faire", FLAGS))
    );

    private static final Map<String, BacklogDomain> DOMAIN_KEYWORDS = new LinkedHashMap<>() {{
        put("droit du travail", BacklogDomain.WORK);
        put("droit travail", BacklogDomain.WORK);
        put("travail", BacklogDomain.WORK);
        put("droit de l'immigration", BacklogDomain.IMMIGRATION);
        put("immigration", BacklogDomain.IMMIGRATION);
        put("droit de la famille", BacklogDomain.FAMILY);
        put("famille", BacklogDomain.FAMILY);
    }};

    public ParsedTechnicalBacklog parseTechnicalBacklog(String content, String sourceFile) {
        if (content == null || content.isBlank()) {
            return new ParsedTechnicalBacklog(List.of(), List.of());
        }
        List<ParsedFeature> features = new ArrayList<>();
        List<ParsedSubfeature> subfeatures = new ArrayList<>();
        Map<String, ParsedFeature> seen = new LinkedHashMap<>();

        String[] lines = content.split("\\R");
        BacklogDomain currentDomain = BacklogDomain.CROSS;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher headingMatch = SECTION_HEADING.matcher(line);
            if (headingMatch.matches()) {
                BacklogDomain detected = detectDomainFromHeading(headingMatch.group(1));
                currentDomain = detected != null ? detected : BacklogDomain.CROSS;
                continue;
            }

            Matcher rowMatch = FEATURE_ROW.matcher(line);
            if (!rowMatch.find()) continue;

            List<String> cells = splitTableRow(line);
            if (cells.size() < 3) continue;

            String code = cells.get(0).trim();
            if (!code.startsWith("F-")) continue;

            String title = cells.get(1).trim();
            String body = String.join(" ", cells.subList(2, cells.size()));

            BacklogStatus status = detectTechnicalStatus(body);
            String targetVersion = extractTargetVersion(body);
            BacklogPriority priority = detectPriority(body);

            ParsedFeature parsed = new ParsedFeature(
                    code, title, targetVersion, status,
                    sanitizeDescription(body), currentDomain, priority,
                    sourceFile, i + 1
            );

            if (seen.containsKey(code)) {
                log.warn("Duplicate feature code {} at line {} in {} — last occurrence wins", code, i + 1, sourceFile);
            }
            seen.put(code, parsed);

            for (Matcher subMatch = SUBFEATURE_REF.matcher(body); subMatch.find(); ) {
                String subCode = subMatch.group();
                BacklogStatus subStatus = detectSubfeatureStatusInContext(body, subCode);
                String subContext = extractSubfeatureContext(body, subCode);
                subfeatures.add(new ParsedSubfeature(
                        subCode, code, null, subStatus, subContext, sourceFile, i + 1
                ));
            }
        }

        features.addAll(seen.values());
        List<ParsedSubfeature> dedupedSubs = dedupeSubfeatures(subfeatures);
        return new ParsedTechnicalBacklog(features, dedupedSubs);
    }

    public List<ParsedMarketingTask> parseMarketingBacklog(String content, String sourceFile) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<ParsedMarketingTask> tasks = new ArrayList<>();
        Map<String, ParsedMarketingTask> seen = new LinkedHashMap<>();
        String[] lines = content.split("\\R");

        String currentCategory = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher headingMatch = SECTION_HEADING.matcher(line);
            if (headingMatch.matches()) {
                currentCategory = headingMatch.group(1).trim();
                continue;
            }

            Matcher rowMatch = MARKETING_ROW.matcher(line);
            if (!rowMatch.find()) continue;

            List<String> cells = splitTableRow(line);
            if (cells.size() < 3) continue;

            String code = cells.get(0).trim();
            String title = cells.get(1).trim();
            String body = String.join(" ", cells.subList(2, cells.size()));

            BacklogMarketingStatus status = detectMarketingStatus(body);

            ParsedMarketingTask task = new ParsedMarketingTask(
                    code, title, status, sanitizeDescription(body),
                    currentCategory, sourceFile, i + 1
            );

            if (seen.containsKey(code)) {
                log.warn("Duplicate marketing code {} at line {} — last occurrence wins", code, i + 1);
            }
            seen.put(code, task);
        }
        tasks.addAll(seen.values());
        return tasks;
    }

    private List<ParsedSubfeature> dedupeSubfeatures(List<ParsedSubfeature> subfeatures) {
        Map<String, ParsedSubfeature> uniq = new LinkedHashMap<>();
        for (ParsedSubfeature sub : subfeatures) {
            ParsedSubfeature existing = uniq.get(sub.code());
            if (existing == null || (existing.status() == BacklogStatus.UNKNOWN && sub.status() != BacklogStatus.UNKNOWN)) {
                uniq.put(sub.code(), sub);
            }
        }
        return new ArrayList<>(uniq.values());
    }

    private List<String> splitTableRow(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inCode = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '`') {
                inCode = !inCode;
                current.append(c);
            } else if (c == '|' && !inCode) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private BacklogStatus detectTechnicalStatus(String text) {
        for (StatusPattern p : TECH_STATUS_PATTERNS) {
            if (p.pattern.matcher(text).find()) {
                return (BacklogStatus) p.status;
            }
        }
        return BacklogStatus.UNKNOWN;
    }

    private BacklogStatus detectSubfeatureStatusInContext(String text, String subCode) {
        int idx = text.indexOf(subCode);
        if (idx < 0) return BacklogStatus.UNKNOWN;
        int start = idx;
        int end = Math.min(text.length(), idx + subCode.length() + 80);
        String window = text.substring(start, end);
        for (StatusPattern p : SUBFEATURE_STATUS_PATTERNS) {
            if (p.pattern.matcher(window).find()) {
                return (BacklogStatus) p.status;
            }
        }
        return BacklogStatus.UNKNOWN;
    }

    private String extractSubfeatureContext(String text, String subCode) {
        int idx = text.indexOf(subCode);
        if (idx < 0) return null;
        int start = Math.max(0, idx - 20);
        int end = Math.min(text.length(), idx + subCode.length() + 200);
        return text.substring(start, end).trim();
    }

    private BacklogMarketingStatus detectMarketingStatus(String text) {
        for (StatusPattern p : MARKETING_STATUS_PATTERNS) {
            if (p.pattern.matcher(text).find()) {
                return (BacklogMarketingStatus) p.status;
            }
        }
        return BacklogMarketingStatus.UNKNOWN;
    }

    private String extractTargetVersion(String text) {
        Matcher m = Pattern.compile("\\b(V\\d+\\+?)").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private BacklogPriority detectPriority(String text) {
        if (text.contains("🔴")) return BacklogPriority.HIGH;
        if (text.contains("🟡")) return BacklogPriority.MEDIUM;
        if (text.contains("🟢")) return BacklogPriority.LOW;
        return null;
    }

    private BacklogDomain detectDomainFromHeading(String heading) {
        String lower = heading.toLowerCase();
        for (Map.Entry<String, BacklogDomain> entry : DOMAIN_KEYWORDS.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private String sanitizeDescription(String body) {
        if (body == null) return null;
        String trimmed = body.trim();
        if (trimmed.length() > 16000) {
            return trimmed.substring(0, 16000);
        }
        return trimmed;
    }

    private record StatusPattern(Enum<?> status, Pattern pattern) {}

    public record ParsedFeature(
            String code,
            String title,
            String targetVersion,
            BacklogStatus status,
            String description,
            BacklogDomain domain,
            BacklogPriority priority,
            String sourceFile,
            int sourceLine
    ) {}

    public record ParsedSubfeature(
            String code,
            String parentFeatureCode,
            String title,
            BacklogStatus status,
            String description,
            String sourceFile,
            int sourceLine
    ) {}

    public record ParsedMarketingTask(
            String code,
            String title,
            BacklogMarketingStatus status,
            String description,
            String category,
            String sourceFile,
            int sourceLine
    ) {}

    public record ParsedTechnicalBacklog(
            List<ParsedFeature> features,
            List<ParsedSubfeature> subfeatures
    ) {}
}

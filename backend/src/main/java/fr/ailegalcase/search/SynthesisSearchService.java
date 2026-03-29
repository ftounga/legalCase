package fr.ailegalcase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SynthesisSearchService {

    private static final int MAX_EXCERPTS = 3;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SynthesisSearchRepository searchRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public SynthesisSearchService(SynthesisSearchRepository searchRepository,
                                  CurrentUserResolver currentUserResolver,
                                  WorkspaceMemberRepository workspaceMemberRepository) {
        this.searchRepository = searchRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional(readOnly = true)
    public SynthesisSearchResponse search(String query, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        var member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        UUID workspaceId = member.getWorkspace().getId();

        List<CaseAnalysis> matches = searchRepository.searchByWorkspace(workspaceId, query);

        List<SynthesisSearchResult> results = matches.stream()
                .map(ca -> toResult(ca, query))
                .toList();

        return new SynthesisSearchResponse(query, results.size(), results);
    }

    SynthesisSearchResult toResult(CaseAnalysis ca, String query) {
        List<String> excerpts = extractExcerpts(ca.getAnalysisResult(), query);
        return new SynthesisSearchResult(
                ca.getCaseFile().getId(),
                ca.getCaseFile().getTitle(),
                ca.getCaseFile().getLegalDomain(),
                ca.getAnalysisType().name(),
                excerpts.size(),
                excerpts
        );
    }

    List<String> extractExcerpts(String analysisResult, String query) {
        String raw = CaseAnalysisResponse.stripMarkdownCodeBlock(analysisResult);
        if (raw == null || raw.isBlank()) return List.of();

        List<String> excerpts = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try {
            JsonNode root = MAPPER.readTree(raw);
            for (String field : new String[]{"faits", "points_juridiques", "risques", "questions_ouvertes"}) {
                JsonNode node = root.get(field);
                if (node == null || !node.isArray()) continue;
                for (JsonNode item : node) {
                    if (!item.isTextual()) continue;
                    String text = item.asText();
                    if (text.toLowerCase().contains(lowerQuery)) {
                        excerpts.add(text);
                        if (excerpts.size() == MAX_EXCERPTS) return List.copyOf(excerpts);
                    }
                }
            }
        } catch (Exception ignored) {}

        return List.copyOf(excerpts);
    }
}

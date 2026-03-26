package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AnalysisDiffService {

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public AnalysisDiffService(CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public AnalysisDiffResponse diff(UUID caseFileId, UUID fromId, UUID toId,
                                     OidcUser oidcUser, String provider, Principal principal) {
        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fromId and toId must be different");
        }

        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        CaseAnalysis from = caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));

        CaseAnalysis to = caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));

        if (from.getAnalysisStatus() != AnalysisStatus.DONE || to.getAnalysisStatus() != AnalysisStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Both analyses must have status DONE");
        }

        CaseAnalysisResponse fromResponse = CaseAnalysisResponse.from(from);
        CaseAnalysisResponse toResponse = CaseAnalysisResponse.from(to);

        return new AnalysisDiffResponse(
                toVersionInfo(from),
                toVersionInfo(to),
                diffStrings(fromResponse.faits(), toResponse.faits()),
                diffStrings(fromResponse.pointsJuridiques(), toResponse.pointsJuridiques()),
                diffStrings(fromResponse.risques(), toResponse.risques()),
                diffStrings(fromResponse.questionsOuvertes(), toResponse.questionsOuvertes()),
                diffTimeline(fromResponse.timeline(), toResponse.timeline())
        );
    }

    private AnalysisDiffResponse.VersionInfo toVersionInfo(CaseAnalysis analysis) {
        return new AnalysisDiffResponse.VersionInfo(
                analysis.getId(),
                analysis.getVersion(),
                analysis.getAnalysisType().name(),
                analysis.getUpdatedAt()
        );
    }

    private AnalysisDiffResponse.SectionDiff<String> diffStrings(List<String> from, List<String> to) {
        Set<String> fromSet = new LinkedHashSet<>(from);
        Set<String> toSet = new LinkedHashSet<>(to);

        List<String> unchanged = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (String item : fromSet) {
            if (toSet.contains(item)) unchanged.add(item);
            else removed.add(item);
        }
        List<String> added = toSet.stream()
                .filter(item -> !fromSet.contains(item))
                .toList();

        return new AnalysisDiffResponse.SectionDiff<>(added, removed, unchanged);
    }

    private AnalysisDiffResponse.SectionDiff<AnalysisDiffResponse.TimelineEntry> diffTimeline(
            List<CaseAnalysisResponse.TimelineEntry> from,
            List<CaseAnalysisResponse.TimelineEntry> to) {

        Set<String> fromKeys = new LinkedHashSet<>();
        for (CaseAnalysisResponse.TimelineEntry e : from) fromKeys.add(key(e));

        Set<String> toKeys = new LinkedHashSet<>();
        for (CaseAnalysisResponse.TimelineEntry e : to) toKeys.add(key(e));

        List<AnalysisDiffResponse.TimelineEntry> unchanged = new ArrayList<>();
        List<AnalysisDiffResponse.TimelineEntry> removed = new ArrayList<>();
        for (CaseAnalysisResponse.TimelineEntry e : from) {
            if (toKeys.contains(key(e))) unchanged.add(toDiffEntry(e));
            else removed.add(toDiffEntry(e));
        }
        List<AnalysisDiffResponse.TimelineEntry> added = to.stream()
                .filter(e -> !fromKeys.contains(key(e)))
                .map(this::toDiffEntry)
                .toList();

        return new AnalysisDiffResponse.SectionDiff<>(added, removed, unchanged);
    }

    private String key(CaseAnalysisResponse.TimelineEntry e) {
        return e.date() + "§" + e.evenement();
    }

    private AnalysisDiffResponse.TimelineEntry toDiffEntry(CaseAnalysisResponse.TimelineEntry e) {
        return new AnalysisDiffResponse.TimelineEntry(e.date(), e.evenement());
    }
}

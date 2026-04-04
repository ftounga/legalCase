package fr.ailegalcase.dashboard;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.ProcedureCheck;
import fr.ailegalcase.analysis.ProcedureCheckRepository;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private static final String STATUS_CLOSED = "CLOSED";
    private static final int URGENT_DAYS = 7;
    private static final long STALE_DAYS = 7L;

    private final CaseFileRepository caseFileRepository;
    private final CaseDeadlineRepository caseDeadlineRepository;
    private final ProcedureCheckRepository procedureCheckRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;

    public DashboardService(CaseFileRepository caseFileRepository,
                            CaseDeadlineRepository caseDeadlineRepository,
                            ProcedureCheckRepository procedureCheckRepository,
                            CaseAnalysisRepository caseAnalysisRepository) {
        this.caseFileRepository = caseFileRepository;
        this.caseDeadlineRepository = caseDeadlineRepository;
        this.procedureCheckRepository = procedureCheckRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse buildSummary(Workspace workspace) {
        List<DashboardOpenCaseItem> openCases = buildOpenCases(workspace);
        long openCasesCount = caseFileRepository.countByWorkspaceAndDeletedAtIsNullAndStatusNot(workspace, STATUS_CLOSED);
        List<DashboardDeadlineItem> urgentDeadlines = buildUrgentDeadlines(workspace.getId());
        List<DashboardStaleCheckItem> staleChecks = buildStaleChecks(workspace.getId());
        List<DashboardAnalysisItem> recentAnalyses = buildRecentAnalyses(workspace);

        return new DashboardSummaryResponse(openCases, openCasesCount, urgentDeadlines, staleChecks, recentAnalyses);
    }

    private List<DashboardOpenCaseItem> buildOpenCases(Workspace workspace) {
        return caseFileRepository
                .findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc(workspace, STATUS_CLOSED)
                .stream()
                .map(cf -> new DashboardOpenCaseItem(cf.getId(), cf.getTitle(), cf.getLegalDomain(), cf.getStatus()))
                .toList();
    }

    private List<DashboardDeadlineItem> buildUrgentDeadlines(UUID workspaceId) {
        LocalDate cutoff = LocalDate.now().plusDays(URGENT_DAYS);
        return caseDeadlineRepository.findUrgentByWorkspaceId(workspaceId, cutoff)
                .stream()
                .map(d -> new DashboardDeadlineItem(d.getId(), d.getLabel(), d.getDueDate(),
                        d.getCaseFile().getId(), d.getCaseFile().getTitle()))
                .toList();
    }

    private List<DashboardStaleCheckItem> buildStaleChecks(UUID workspaceId) {
        Instant cutoff = Instant.now().minus(STALE_DAYS, ChronoUnit.DAYS);
        List<ProcedureCheck> stale = procedureCheckRepository.findStaleNonCompliantByWorkspaceId(workspaceId, cutoff);

        Map<UUID, DashboardStaleCheckItem> byCase = new LinkedHashMap<>();
        for (ProcedureCheck pc : stale) {
            UUID caseFileId = pc.getCaseAnalysis().getCaseFile().getId();
            String caseFileTitle = pc.getCaseAnalysis().getCaseFile().getTitle();
            byCase.merge(caseFileId,
                    new DashboardStaleCheckItem(caseFileId, caseFileTitle, 1L),
                    (existing, newItem) -> new DashboardStaleCheckItem(
                            existing.caseFileId(), existing.caseFileTitle(), existing.nonCompliantCount() + 1));
        }
        return new ArrayList<>(byCase.values());
    }

    private List<DashboardAnalysisItem> buildRecentAnalyses(Workspace workspace) {
        return caseAnalysisRepository
                .findTop5ByCaseFile_WorkspaceAndAnalysisStatusOrderByCreatedAtDesc(workspace, AnalysisStatus.DONE)
                .stream()
                .map(ca -> new DashboardAnalysisItem(ca.getId(), ca.getCaseFile().getId(),
                        ca.getCaseFile().getTitle(), ca.getAnalysisType().name(), ca.getCreatedAt()))
                .toList();
    }
}

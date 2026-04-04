package fr.ailegalcase.referential;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ReferentialReportService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialReportService.class);
    private static final List<String> OWNER_ADMIN = List.of("OWNER", "ADMIN");

    private final LegalReferentialRepository referentialRepository;
    private final ReferentialReportRepository reportRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final fr.ailegalcase.workspace.EmailService emailService;

    public ReferentialReportService(LegalReferentialRepository referentialRepository,
                                    ReferentialReportRepository reportRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    fr.ailegalcase.workspace.EmailService emailService) {
        this.referentialRepository = referentialRepository;
        this.reportRepository = reportRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void report(UUID entryId, UUID workspaceId, User reporter, String comment) {
        LegalReferential entry = referentialRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Référentiel introuvable"));

        // Prevent duplicate OPEN report
        reportRepository.findByEntry_IdAndReporter_IdAndStatus(
                entryId, reporter.getId(), ReferentialReport.Status.OPEN)
                .ifPresent(r -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Vous avez déjà signalé cette valeur.");
                });

        ReferentialReport report = new ReferentialReport();
        report.setEntry(entry);
        report.setReporter(reporter);
        report.setWorkspaceId(workspaceId);
        report.setComment(comment);
        reportRepository.save(report);
        log.info("ReferentialReportService: new report on entry {} by user {}", entryId, reporter.getId());

        // Notify OWNER and ADMIN of the workspace
        List<WorkspaceMember> recipients = workspaceMemberRepository
                .findByWorkspace_IdAndMemberRoleIn(workspaceId, OWNER_ADMIN);
        recipients.forEach(m -> {
            try {
                emailService.sendReferentialReport(
                        m.getUser().getEmail(),
                        entry.getLabel() != null ? entry.getLabel() : entry.getEntryKey(),
                        entry.getValueJson(),
                        comment,
                        reporter.getEmail());
            } catch (Exception e) {
                log.warn("ReferentialReportService: failed to notify {} — {}", m.getUser().getEmail(), e.getMessage());
            }
        });
    }
}

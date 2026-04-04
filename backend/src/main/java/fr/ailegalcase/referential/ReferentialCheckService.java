package fr.ailegalcase.referential;

import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cron service that checks all system referential entries against official legal sources.
 * Runs every 6 months by default (configurable). Creates ReferentialAlert on divergence
 * and notifies all OWNER members across all workspaces.
 */
@Service
public class ReferentialCheckService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialCheckService.class);
    private static final int MAX_TOKENS = 512;

    private static final String SYSTEM_PROMPT = """
            Tu es un expert juridique spécialisé en droit français et belge.
            Tu dois vérifier si une valeur de référentiel métier est toujours à jour par rapport
            aux textes officiels en vigueur (Légifrance, UNAF, Journal Officiel).

            Réponds UNIQUEMENT par l'une des deux formes suivantes :
            - UP_TO_DATE (si la valeur est conforme aux textes officiels en vigueur)
            - OUTDATED: [valeur officielle actuelle en JSON] | [explication courte en français]
              (si la valeur est obsolète ou incorrecte — inclure la valeur JSON corrigée)

            Ne fournis aucune autre explication. Sois concis.
            """;

    private final LegalReferentialRepository referentialRepository;
    private final ReferentialAlertRepository alertRepository;
    private final AnthropicService anthropicService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final fr.ailegalcase.workspace.EmailService emailService;
    private final String frontendUrl;

    public ReferentialCheckService(
            LegalReferentialRepository referentialRepository,
            ReferentialAlertRepository alertRepository,
            AnthropicService anthropicService,
            WorkspaceMemberRepository workspaceMemberRepository,
            fr.ailegalcase.workspace.EmailService emailService,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.referentialRepository = referentialRepository;
        this.alertRepository = alertRepository;
        this.anthropicService = anthropicService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Scheduled(cron = "${app.referentials.check-cron:0 0 0 1 */6 *}")
    @Transactional
    public void checkAllSystemEntries() {
        log.info("ReferentialCheckService: starting periodic check of system entries");
        List<LegalReferential> systemEntries = referentialRepository.findAllSystemEntries();
        log.info("ReferentialCheckService: checking {} system entries", systemEntries.size());

        int alertsCreated = 0;
        for (LegalReferential entry : systemEntries) {
            try {
                CheckResult result = checkEntry(entry);
                if (!result.upToDate()) {
                    boolean alreadyPending = alertRepository
                            .findByEntry_IdAndStatus(entry.getId(), ReferentialAlert.Status.PENDING)
                            .isPresent();
                    if (!alreadyPending) {
                        createAlert(entry, result);
                        alertsCreated++;
                    }
                }
            } catch (Exception e) {
                log.warn("ReferentialCheckService: fail-open for entry {}/{} — {}",
                        entry.getLegalDomain(), entry.getEntryKey(), e.getMessage());
            }
        }

        if (alertsCreated > 0) {
            log.info("ReferentialCheckService: {} new alerts created — notifying OWNERs", alertsCreated);
            notifyOwners();
        } else {
            log.info("ReferentialCheckService: all entries up to date");
        }
    }

    private CheckResult checkEntry(LegalReferential entry) {
        String userMessage = """
                Domaine juridique : %s
                Type de référentiel : %s
                Clé : %s
                Libellé : %s
                Valeur actuelle : %s
                Source : %s
                """.formatted(
                entry.getLegalDomain(), entry.getReferentialType(),
                entry.getEntryKey(), entry.getLabel(),
                entry.getValueJson(), entry.getSourceRef() != null ? entry.getSourceRef() : "non spécifiée");

        var result = anthropicService.analyzeFast(SYSTEM_PROMPT, userMessage, MAX_TOKENS);
        String content = result.content().trim();

        if (content.startsWith("OUTDATED:")) {
            String body = content.substring("OUTDATED:".length()).trim();
            int sep = body.indexOf('|');
            String proposedJson = sep > 0 ? body.substring(0, sep).trim() : entry.getValueJson();
            String aiMessage = sep > 0 ? body.substring(sep + 1).trim() : body;
            return new CheckResult(false, proposedJson, aiMessage);
        }
        return new CheckResult(true, null, null);
    }

    private void createAlert(LegalReferential entry, CheckResult result) {
        ReferentialAlert alert = new ReferentialAlert();
        alert.setEntry(entry);
        alert.setProposedValueJson(result.proposedJson());
        alert.setAiMessage(result.aiMessage());
        alertRepository.save(alert);
        log.info("ReferentialCheckService: alert created for entry {}/{}", entry.getLegalDomain(), entry.getEntryKey());
    }

    void notifyOwners() {
        List<WorkspaceMember> owners = workspaceMemberRepository.findByMemberRole("OWNER");
        Map<String, List<WorkspaceMember>> byWorkspace = owners.stream()
                .collect(Collectors.groupingBy(m -> m.getWorkspace().getId().toString()));

        byWorkspace.forEach((wsId, ownerList) -> ownerList.forEach(owner -> {
            try {
                emailService.sendReferentialAlert(owner.getUser().getEmail(), frontendUrl);
            } catch (Exception e) {
                log.warn("ReferentialCheckService: failed to notify owner {} — {}", owner.getUser().getEmail(), e.getMessage());
            }
        }));
    }

    record CheckResult(boolean upToDate, String proposedJson, String aiMessage) {}
}

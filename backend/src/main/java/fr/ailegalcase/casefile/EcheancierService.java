package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.EcheancierItem.EcheancierKind;
import fr.ailegalcase.casefile.EcheancierItem.EcheancierUrgency;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * F-284 — Échéancier procédural proactif (vue de lecture agrégée).
 *
 * <p>Agrège, pour un dossier, les délais confirmés ({@code case_deadlines} hors AI PENDING) et les
 * échéances de réponse des rounds contradictoires ({@code contradictoire_rounds.response_due_at}),
 * priorisés par urgence. <b>Lecture seule</b> : aucune mutation, aucune nouvelle table. La création
 * et l'édition restent dans F-69 / F-282 ; les alertes mail restent dans {@code DeadlineAlertService}.
 */
@Service
public class EcheancierService {

    private final CaseDeadlineRepository deadlineRepository;
    private final ContradictoireRoundRepository roundRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public EcheancierService(CaseDeadlineRepository deadlineRepository,
                             ContradictoireRoundRepository roundRepository,
                             CaseFileRepository caseFileRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             CurrentUserResolver currentUserResolver) {
        this.deadlineRepository = deadlineRepository;
        this.roundRepository = roundRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public EcheancierResponse forCaseFile(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        LocalDate today = LocalDate.now();

        List<EcheancierItem> items = new ArrayList<>();

        // 1) Délais confirmés : MANUAL, STATUTORY, ou AI déjà ACCEPTED (les AI PENDING sont exclus).
        for (CaseDeadline d : deadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFile.getId())) {
            if (!isConfirmed(d)) continue;
            items.add(toItem(d.getId(), d.getLabel(), d.getDueDate(), today,
                    EcheancierKind.DEADLINE, d.getSource()));
        }

        // 2) Échéance de réponse du DERNIER round contradictoire uniquement (SF-290-04).
        // Une échéance de réponse dont un round postérieur existe est considérée *satisfaite*
        // (la réponse attendue est arrivée) → on ne l'affiche plus comme due/dépassée. Seul le
        // dernier round (aucun round postérieur) porte encore une échéance de réponse vivante.
        var rounds = roundRepository.findByCaseFileIdOrderByRoundNumberAsc(caseFile.getId());
        if (!rounds.isEmpty()) {
            ContradictoireRound last = rounds.get(rounds.size() - 1);
            if (last.getResponseDueAt() != null) {
                items.add(toItem(last.getId(), responseLabel(last), last.getResponseDueAt(), today,
                        EcheancierKind.CONTRADICTOIRE, "CONTRADICTOIRE"));
            }
        }

        items.sort(Comparator.comparing(EcheancierItem::dueDate));

        EcheancierItem nextItem = items.isEmpty() ? null : items.get(0);
        EcheancierResponse.Counts counts = new EcheancierResponse.Counts(
                count(items, EcheancierUrgency.OVERDUE),
                count(items, EcheancierUrgency.CRITICAL),
                count(items, EcheancierUrgency.SOON),
                count(items, EcheancierUrgency.UPCOMING),
                items.size());

        return new EcheancierResponse(items, nextItem, counts);
    }

    private static boolean isConfirmed(CaseDeadline d) {
        if ("AI".equals(d.getSource())) {
            return "ACCEPTED".equals(d.getAiStatus());
        }
        return true; // MANUAL, STATUTORY
    }

    private static EcheancierItem toItem(UUID id, String label, LocalDate dueDate, LocalDate today,
                                         EcheancierKind kind, String source) {
        long daysUntil = ChronoUnit.DAYS.between(today, dueDate);
        return new EcheancierItem(id, label, dueDate, daysUntil,
                EcheancierItem.urgencyOf(daysUntil), kind, source);
    }

    private static String responseLabel(ContradictoireRound r) {
        if (r.getLabel() != null && !r.getLabel().isBlank()) {
            return "Réponse — " + r.getLabel().trim();
        }
        return "Réponse au round " + r.getRoundNumber();
    }

    private static long count(List<EcheancierItem> items, EcheancierUrgency urgency) {
        return items.stream().filter(i -> i.urgency() == urgency).count();
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }
}

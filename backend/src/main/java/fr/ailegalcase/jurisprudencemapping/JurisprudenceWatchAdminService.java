package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-05 — service d'arbitrage admin des flags de veille
 * jurisprudentielle.
 */
@Service
public class JurisprudenceWatchAdminService {

    private final JurisprudenceWatchFlagRepository flagRepository;
    private final ToolJurisprudenceMappingRepository mappingRepository;
    private final JurisprudenceAuditLogRepository auditLogRepository;

    public JurisprudenceWatchAdminService(JurisprudenceWatchFlagRepository flagRepository,
                                          ToolJurisprudenceMappingRepository mappingRepository,
                                          JurisprudenceAuditLogRepository auditLogRepository) {
        this.flagRepository = flagRepository;
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<JurisprudenceWatchFlagResponse> listFlags(JurisprudenceWatchFlagStatut statut,
                                                          int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        JurisprudenceWatchFlagStatut effective = statut == null ? JurisprudenceWatchFlagStatut.PENDING : statut;
        return flagRepository.findByStatutOrderByCreatedAtDesc(effective, pageable)
                .map(JurisprudenceWatchFlagResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JurisprudenceAuditLogResponse> listAuditLog(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(JurisprudenceAuditLogResponse::from);
    }

    @Transactional
    public JurisprudenceWatchFlagResponse arbitrate(UUID flagId,
                                                    JurisprudenceArbitrateRequest request,
                                                    User actorUser) {
        JurisprudenceWatchFlag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flag not found"));
        if (flag.getStatut() != JurisprudenceWatchFlagStatut.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Flag already arbitrated");
        }

        switch (request.decision()) {
            case REPLACE -> applyReplaceDecision(flag);
            case ADD -> applyAddDecision(flag);
            case IGNORE -> { /* no-op sur les mappings */ }
        }

        flag.setStatut(request.decision() == JurisprudenceWatchFlagDecision.IGNORE
                ? JurisprudenceWatchFlagStatut.IGNORED
                : JurisprudenceWatchFlagStatut.REVIEWED);
        flag.setDecision(request.decision());
        flag.setReviewedAt(Instant.now());
        flag.setReviewedBy(actorUser);
        if (request.comment() != null && !request.comment().isBlank()) {
            flag.setCommentUser(request.comment().trim());
        }
        flagRepository.save(flag);

        writeAuditLog(flag, mapManualAction(request.decision()), actorUser);
        return JurisprudenceWatchFlagResponse.from(flag);
    }

    private void applyReplaceDecision(JurisprudenceWatchFlag flag) {
        ToolJurisprudenceMapping current = flag.getMappingActuel();
        if (current != null) {
            current.setArchived(true);
            mappingRepository.save(current);
        }
        // Le nouvel arrêt entrant est référencé via arret_entrant_ref. L'admin
        // pourra créer manuellement le nouveau mapping via le bootstrap ; on
        // ne le crée pas automatiquement ici car on n'a pas tous les champs
        // (juridiction / date / chapeau) du flag (seulement la ref). V2 : étendre
        // le flag pour porter ces champs ou enrichir Claude en SF-02.
    }

    private void applyAddDecision(JurisprudenceWatchFlag flag) {
        // Idem REPLACE : on ne peut pas créer le nouveau mapping sans tous les
        // champs. L'admin doit déclencher un bootstrap manuel via l'endpoint
        // dédié pour ajouter explicitement l'arrêt.
    }

    private JurisprudenceAuditAction mapManualAction(JurisprudenceWatchFlagDecision decision) {
        return switch (decision) {
            case REPLACE -> JurisprudenceAuditAction.MANUAL_REPLACE;
            case ADD -> JurisprudenceAuditAction.MANUAL_ADD;
            case IGNORE -> JurisprudenceAuditAction.MANUAL_IGNORE;
        };
    }

    private void writeAuditLog(JurisprudenceWatchFlag flag, JurisprudenceAuditAction action, User actorUser) {
        if (flag.getMappingActuel() == null) {
            return;
        }
        JurisprudenceAuditLog entry = new JurisprudenceAuditLog();
        entry.setMapping(flag.getMappingActuel());
        entry.setAction(action);
        entry.setActor(JurisprudenceAuditActor.SUPER_ADMIN);
        entry.setActorUser(actorUser);
        entry.setClaudeConfidence(BigDecimal.ZERO);
        entry.setClaudeReason("Arbitrage admin flag " + flag.getId());
        auditLogRepository.save(entry);
    }

    private static int clampSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 100);
    }
}

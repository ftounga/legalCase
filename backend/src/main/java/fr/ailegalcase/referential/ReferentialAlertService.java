package fr.ailegalcase.referential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles apply/dismiss of referential alerts and exposes the pending count.
 */
@Service
public class ReferentialAlertService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialAlertService.class);

    private final ReferentialAlertRepository alertRepository;
    private final LegalReferentialRepository referentialRepository;

    public ReferentialAlertService(ReferentialAlertRepository alertRepository,
                                   LegalReferentialRepository referentialRepository) {
        this.alertRepository = alertRepository;
        this.referentialRepository = referentialRepository;
    }

    public long pendingCount() {
        return alertRepository.countByStatus(ReferentialAlert.Status.PENDING);
    }

    @Transactional
    public void apply(UUID alertId, UUID workspaceId, UUID userId) {
        ReferentialAlert alert = getAlertForOwner(alertId);

        LegalReferential source = alert.getEntry();

        // Create or update workspace override with proposed value
        LegalReferential target;
        var overrides = referentialRepository.findWorkspaceEntry(
                workspaceId, source.getLegalDomain(), source.getReferentialType(),
                source.getEntryKey(), source.getCountry());
        if (!overrides.isEmpty()) {
            target = overrides.get(0);
        } else {
            target = new LegalReferential();
            target.setWorkspaceId(workspaceId);
            target.setLegalDomain(source.getLegalDomain());
            target.setReferentialType(source.getReferentialType());
            target.setEntryKey(source.getEntryKey());
            target.setCountry(source.getCountry());
            target.setSystem(false);
            target.setActive(true);
            target.setSourceRef(source.getSourceRef());
            target.setLabel(source.getLabel());
        }
        target.setValueJson(alert.getProposedValueJson());
        target.setUpdatedAt(Instant.now());
        target.setUpdatedBy(userId);
        referentialRepository.save(target);

        alert.setStatus(ReferentialAlert.Status.APPLIED);
        alert.setResolvedAt(Instant.now());
        alertRepository.save(alert);
        log.info("ReferentialAlertService: alert {} applied by user {}", alertId, userId);
    }

    @Transactional
    public void dismiss(UUID alertId, UUID userId) {
        ReferentialAlert alert = getAlertForOwner(alertId);
        alert.setStatus(ReferentialAlert.Status.DISMISSED);
        alert.setResolvedAt(Instant.now());
        alertRepository.save(alert);
        log.info("ReferentialAlertService: alert {} dismissed by user {}", alertId, userId);
    }

    private ReferentialAlert getAlertForOwner(UUID alertId) {
        ReferentialAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte introuvable"));
        if (alert.getStatus() != ReferentialAlert.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alerte déjà traitée");
        }
        return alert;
    }
}

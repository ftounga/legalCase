package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JurisprudenceWatchAdminServiceTest {

    private JurisprudenceWatchFlagRepository flagRepo;
    private ToolJurisprudenceMappingRepository mappingRepo;
    private JurisprudenceAuditLogRepository auditRepo;
    private JurisprudenceWatchAdminService service;
    private User actor;

    @BeforeEach
    void setUp() {
        flagRepo = mock(JurisprudenceWatchFlagRepository.class);
        mappingRepo = mock(ToolJurisprudenceMappingRepository.class);
        auditRepo = mock(JurisprudenceAuditLogRepository.class);
        service = new JurisprudenceWatchAdminService(flagRepo, mappingRepo, auditRepo);
        actor = new User();
        actor.setEmail("admin@example.com");
    }

    @Test
    void arbitrate_unknownFlag_throws404() {
        UUID id = UUID.randomUUID();
        when(flagRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.arbitrate(id, new JurisprudenceArbitrateRequest(JurisprudenceWatchFlagDecision.IGNORE, null), actor));
    }

    @Test
    void arbitrate_alreadyReviewed_throws409() {
        JurisprudenceWatchFlag flag = buildFlag();
        flag.setStatut(JurisprudenceWatchFlagStatut.REVIEWED);
        when(flagRepo.findById(flag.getId())).thenReturn(Optional.of(flag));

        assertThrows(ResponseStatusException.class,
                () -> service.arbitrate(flag.getId(), new JurisprudenceArbitrateRequest(JurisprudenceWatchFlagDecision.IGNORE, null), actor));
    }

    @Test
    void arbitrate_ignore_marksFlagIgnored_andAuditsManualIgnore() {
        JurisprudenceWatchFlag flag = buildFlag();
        when(flagRepo.findById(flag.getId())).thenReturn(Optional.of(flag));

        service.arbitrate(flag.getId(),
                new JurisprudenceArbitrateRequest(JurisprudenceWatchFlagDecision.IGNORE, "faux positif"),
                actor);

        assertThat(flag.getStatut()).isEqualTo(JurisprudenceWatchFlagStatut.IGNORED);
        assertThat(flag.getDecision()).isEqualTo(JurisprudenceWatchFlagDecision.IGNORE);
        assertThat(flag.getCommentUser()).isEqualTo("faux positif");
        assertThat(flag.getReviewedBy()).isSameAs(actor);

        verify(flagRepo, times(1)).save(flag);
        ArgumentCaptor<JurisprudenceAuditLog> captor = ArgumentCaptor.forClass(JurisprudenceAuditLog.class);
        verify(auditRepo).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(JurisprudenceAuditAction.MANUAL_IGNORE);
        assertThat(captor.getValue().getActor()).isEqualTo(JurisprudenceAuditActor.SUPER_ADMIN);
    }

    @Test
    void arbitrate_replace_archivesCurrentMapping_andAuditsManualReplace() {
        JurisprudenceWatchFlag flag = buildFlag();
        when(flagRepo.findById(flag.getId())).thenReturn(Optional.of(flag));

        service.arbitrate(flag.getId(),
                new JurisprudenceArbitrateRequest(JurisprudenceWatchFlagDecision.REPLACE, null),
                actor);

        assertThat(flag.getMappingActuel().isArchived()).isTrue();
        verify(mappingRepo, times(1)).save(flag.getMappingActuel());
        verify(auditRepo).save(any());
    }

    @Test
    void arbitrate_add_doesNotArchiveCurrentMapping() {
        JurisprudenceWatchFlag flag = buildFlag();
        when(flagRepo.findById(flag.getId())).thenReturn(Optional.of(flag));

        service.arbitrate(flag.getId(),
                new JurisprudenceArbitrateRequest(JurisprudenceWatchFlagDecision.ADD, null),
                actor);

        assertThat(flag.getMappingActuel().isArchived()).isFalse();
        verify(mappingRepo, never()).save(any());
    }

    private JurisprudenceWatchFlag buildFlag() {
        ToolJurisprudenceMapping mapping = new ToolJurisprudenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setToolId("f-dt-30");
        mapping.setBrancheCalculId("b1");
        mapping.setArretRef("ref");
        mapping.setJuridiction("Cour de cassation");
        mapping.setDateArret(LocalDate.now());
        mapping.setNumeroPourvoi("n");
        mapping.setLienLegifrance("url");
        mapping.setChapeauOfficiel("ch");
        mapping.setConfidenceScore(new BigDecimal("0.90"));
        mapping.setArchived(false);

        JurisprudenceWatchFlag flag = new JurisprudenceWatchFlag();
        flag.setId(UUID.randomUUID());
        flag.setToolId("f-dt-30");
        flag.setBrancheCalculId("b1");
        flag.setArretEntrantRef("ref-entrant");
        flag.setMappingActuel(mapping);
        flag.setSource(JurisprudenceWatchFlagSource.CRON);
        flag.setStatut(JurisprudenceWatchFlagStatut.PENDING);
        flag.setCreatedAt(Instant.now());
        return flag;
    }
}

package fr.ailegalcase.referential;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferentialAlertServiceTest {

    @Mock private ReferentialAlertRepository alertRepository;
    @Mock private LegalReferentialRepository referentialRepository;
    private ReferentialAlertService service;

    @BeforeEach
    void setUp() {
        service = new ReferentialAlertService(alertRepository, referentialRepository);
    }

    private LegalReferential systemEntry() {
        LegalReferential e = new LegalReferential();
        e.setId(UUID.randomUUID());
        e.setLegalDomain("DROIT_DU_TRAVAIL");
        e.setReferentialType("BAREME_MACRON");
        e.setEntryKey("0_ANS");
        e.setLabel("Barème Macron — 0 ans");
        e.setValueJson("{\"value\":1000}");
        e.setSystem(true);
        e.setActive(true);
        return e;
    }

    private ReferentialAlert pendingAlert(LegalReferential entry) {
        ReferentialAlert a = new ReferentialAlert();
        a.setId(UUID.randomUUID());
        a.setEntry(entry);
        a.setStatus(ReferentialAlert.Status.PENDING);
        a.setProposedValueJson("{\"value\":1050}");
        a.setAiMessage("Valeur revalorisation 2026");
        return a;
    }

    // ALS-01 : pendingCount retourne le nombre d'alertes PENDING
    @Test
    void pendingCount_returnsCount() {
        when(alertRepository.countByStatus(ReferentialAlert.Status.PENDING)).thenReturn(3L);
        assertThat(service.pendingCount()).isEqualTo(3L);
    }

    // ALS-02 : apply → crée workspace override, status=APPLIED
    @Test
    void apply_createsWorkspaceOverrideAndSetsApplied() {
        LegalReferential entry = systemEntry();
        ReferentialAlert alert = pendingAlert(entry);
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(alertRepository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(referentialRepository.findWorkspaceEntry(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(referentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.apply(alert.getId(), workspaceId, userId);

        ArgumentCaptor<LegalReferential> refCap = ArgumentCaptor.forClass(LegalReferential.class);
        verify(referentialRepository).save(refCap.capture());
        LegalReferential saved = refCap.getValue();
        assertThat(saved.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(saved.getValueJson()).isEqualTo("{\"value\":1050}");
        assertThat(saved.isSystem()).isFalse();

        ArgumentCaptor<ReferentialAlert> alertCap = ArgumentCaptor.forClass(ReferentialAlert.class);
        verify(alertRepository).save(alertCap.capture());
        assertThat(alertCap.getValue().getStatus()).isEqualTo(ReferentialAlert.Status.APPLIED);
        assertThat(alertCap.getValue().getResolvedAt()).isNotNull();
    }

    // ALS-03 : dismiss → status=DISMISSED
    @Test
    void dismiss_setsDismissed() {
        LegalReferential entry = systemEntry();
        ReferentialAlert alert = pendingAlert(entry);
        UUID userId = UUID.randomUUID();

        when(alertRepository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.dismiss(alert.getId(), userId);

        ArgumentCaptor<ReferentialAlert> cap = ArgumentCaptor.forClass(ReferentialAlert.class);
        verify(alertRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ReferentialAlert.Status.DISMISSED);
        assertThat(cap.getValue().getResolvedAt()).isNotNull();
    }

    // ALS-04 : apply sur alerte déjà APPLIED → 409 CONFLICT
    @Test
    void apply_alreadyApplied_throws409() {
        LegalReferential entry = systemEntry();
        ReferentialAlert alert = pendingAlert(entry);
        alert.setStatus(ReferentialAlert.Status.APPLIED);

        when(alertRepository.findById(alert.getId())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service.apply(alert.getId(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà traitée");
    }

    // ALS-05 : pendingCount = 0 si aucune alerte
    @Test
    void pendingCount_zero_whenNoAlerts() {
        when(alertRepository.countByStatus(ReferentialAlert.Status.PENDING)).thenReturn(0L);
        assertThat(service.pendingCount()).isEqualTo(0L);
    }
}

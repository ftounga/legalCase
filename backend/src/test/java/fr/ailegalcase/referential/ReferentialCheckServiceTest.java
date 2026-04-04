package fr.ailegalcase.referential;

import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.workspace.EmailService;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferentialCheckServiceTest {

    @Mock private LegalReferentialRepository referentialRepository;
    @Mock private ReferentialAlertRepository alertRepository;
    @Mock private AnthropicService anthropicService;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private EmailService emailService;

    private ReferentialCheckService service;

    @BeforeEach
    void setUp() {
        service = new ReferentialCheckService(
                referentialRepository, alertRepository, anthropicService,
                workspaceMemberRepository, emailService, "http://localhost:4200");
    }

    private LegalReferential systemEntry(String key) {
        LegalReferential e = new LegalReferential();
        e.setId(UUID.randomUUID());
        e.setLegalDomain("DROIT_DU_TRAVAIL");
        e.setReferentialType("BAREME_MACRON");
        e.setEntryKey(key);
        e.setLabel("Barème Macron — " + key);
        e.setValueJson("{\"value\":1000}");
        e.setSystem(true);
        e.setActive(true);
        return e;
    }

    // CHK-01 : Claude répond UP_TO_DATE → aucune alerte créée
    @Test
    void checkAllSystemEntries_upToDate_noAlertCreated() {
        LegalReferential entry = systemEntry("0_ANS");
        when(referentialRepository.findAllSystemEntries()).thenReturn(List.of(entry));
        when(anthropicService.analyzeFast(anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult("UP_TO_DATE", "haiku", 10, 5));

        service.checkAllSystemEntries();

        verify(alertRepository, never()).save(any());
    }

    // CHK-02 : Claude répond OUTDATED → alerte créée + notifyOwners appelé
    @Test
    void checkAllSystemEntries_outdated_alertCreatedAndOwnersNotified() {
        LegalReferential entry = systemEntry("0_ANS");
        when(referentialRepository.findAllSystemEntries()).thenReturn(List.of(entry));
        when(anthropicService.analyzeFast(anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult(
                        "OUTDATED: {\"value\":1050} | Le barème a été revalorisé en janvier 2026.",
                        "haiku", 10, 30));
        when(alertRepository.findByEntry_IdAndStatus(any(), eq(ReferentialAlert.Status.PENDING)))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.findByMemberRole("OWNER")).thenReturn(List.of());

        service.checkAllSystemEntries();

        ArgumentCaptor<ReferentialAlert> cap = ArgumentCaptor.forClass(ReferentialAlert.class);
        verify(alertRepository).save(cap.capture());
        ReferentialAlert saved = cap.getValue();
        assertThat(saved.getProposedValueJson()).isEqualTo("{\"value\":1050}");
        assertThat(saved.getAiMessage()).contains("revalorisé");
        assertThat(saved.getStatus()).isEqualTo(ReferentialAlert.Status.PENDING);
    }

    // CHK-03 : exception Claude → fail-open, aucune alerte créée
    @Test
    void checkAllSystemEntries_claudeException_failOpen() {
        LegalReferential entry = systemEntry("0_ANS");
        when(referentialRepository.findAllSystemEntries()).thenReturn(List.of(entry));
        when(anthropicService.analyzeFast(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic unavailable"));

        service.checkAllSystemEntries();

        verify(alertRepository, never()).save(any());
    }

    // CHK-04 : alerte déjà PENDING pour la même entrée → pas de doublon
    @Test
    void checkAllSystemEntries_alreadyPendingAlert_noDuplicate() {
        LegalReferential entry = systemEntry("0_ANS");
        when(referentialRepository.findAllSystemEntries()).thenReturn(List.of(entry));
        when(anthropicService.analyzeFast(anyString(), anyString(), eq(512)))
                .thenReturn(new AnthropicResult(
                        "OUTDATED: {\"value\":1050} | Valeur obsolète.", "haiku", 10, 20));
        when(alertRepository.findByEntry_IdAndStatus(any(), eq(ReferentialAlert.Status.PENDING)))
                .thenReturn(Optional.of(new ReferentialAlert()));

        service.checkAllSystemEntries();

        verify(alertRepository, never()).save(any());
    }
}

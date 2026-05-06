package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * F-176 SF-176-01 + F-192 SF-192-01 — Tests de régression sur
 * {@link StrategicOptionService#updateStatus}.
 *
 * <p><b>CA-01 cohérence F-176 strict</b> : le PUT statut piste reste un PUT pur,
 * AUCUN side-effect F-192 (pieces_manquantes, case_deadlines, alignment) n'est
 * déclenché. Tous les effets F-192 sont gating Synthèse enrichie.</p>
 */
class StrategicOptionServiceTest {

    private final StrategicOptionRepository strategicOptionRepository = mock(StrategicOptionRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

    // F-192 deps — vérifient qu'AUCUN appel n'est fait pendant le PUT statut
    private final CaseDeadlineRepository caseDeadlineRepository = mock(CaseDeadlineRepository.class);

    private final StrategicOptionService service = new StrategicOptionService(
            strategicOptionRepository, caseAnalysisRepository, caseFileRepository,
            workspaceMemberRepository, currentUserResolver);

    @Test
    void updateStatus_RETAINED_isPurePut_noPiecesNoDeadlinesNoAlignment() {
        UUID optionId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        User user = new User();
        Workspace ws = new Workspace();
        try {
            java.lang.reflect.Field idField = Workspace.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ws, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        StrategicOption option = new StrategicOption();
        option.setStatut(StrategicOptionStatus.TO_STUDY);
        when(strategicOptionRepository.findByIdAndWorkspaceId(optionId, workspaceId))
                .thenReturn(Optional.of(option));
        when(strategicOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StrategicOptionResponse response = service.updateStatus(optionId, "RETAINED", null, null, null);

        assertThat(response.statut()).isEqualTo("RETAINED");
        // 1 save de l'option ; rien d'autre.
        verify(strategicOptionRepository, times(1)).save(any());
        // F-192 cohérence stricte : aucun appel sur les repos pieces/délais
        verifyNoInteractions(caseDeadlineRepository);
        // Aucune mutation de l'analyse parente (pas de save sur caseAnalysisRepository)
        verify(caseAnalysisRepository, never()).save(any());
    }

    @Test
    void updateStatus_DISCARDED_isPurePut_noPiecesNoDeadlines() {
        UUID optionId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        User user = new User();
        Workspace ws = new Workspace();
        try {
            java.lang.reflect.Field idField = Workspace.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ws, workspaceId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        StrategicOption option = new StrategicOption();
        option.setStatut(StrategicOptionStatus.TO_STUDY);
        when(strategicOptionRepository.findByIdAndWorkspaceId(optionId, workspaceId))
                .thenReturn(Optional.of(option));
        when(strategicOptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StrategicOptionResponse response = service.updateStatus(
                optionId, "DISCARDED", "Pas pertinent", null, null);

        assertThat(response.statut()).isEqualTo("DISCARDED");
        assertThat(response.raisonDiscard()).isEqualTo("Pas pertinent");
        verify(strategicOptionRepository, times(1)).save(any());
        verifyNoInteractions(caseDeadlineRepository);
    }
}

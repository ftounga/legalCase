package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiQuestionAnswerCommandServiceTest {

    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
    private final AuthAccountRepository authAccountRepository = mock(AuthAccountRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);

    private final AiQuestionAnswerCommandService service = new AiQuestionAnswerCommandService(
            aiQuestionRepository, aiQuestionAnswerRepository, authAccountRepository, workspaceMemberRepository);

    private final OidcUser oidcUser = mock(OidcUser.class);

    // U-01 : réponse nominale → answer persisté, question status=ANSWERED
    @Test
    void answer_nominal_persistsAnswerAndUpdatesQuestionStatus() {
        UUID workspaceId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        Workspace workspace = workspace(workspaceId);
        User user = user();
        AiQuestion question = question(questionId, workspace);

        setupAuth(user, workspace);
        when(aiQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(aiQuestionAnswerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(oidcUser.getSubject()).thenReturn("sub");

        service.answer(questionId, "Ma réponse", oidcUser, "GOOGLE");

        ArgumentCaptor<AiQuestionAnswer> answerCaptor = ArgumentCaptor.forClass(AiQuestionAnswer.class);
        verify(aiQuestionAnswerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getAnswerText()).isEqualTo("Ma réponse");
        assertThat(answerCaptor.getValue().getAnsweredByUser()).isEqualTo(user);

        ArgumentCaptor<AiQuestion> questionCaptor = ArgumentCaptor.forClass(AiQuestion.class);
        verify(aiQuestionRepository).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getStatus()).isEqualTo("ANSWERED");
        assertThat(questionCaptor.getValue().getAnsweredAt()).isNotNull();
    }

    // U-02 : question inconnue → 404
    @Test
    void answer_unknownQuestion_throws404() {
        UUID workspaceId = UUID.randomUUID();
        User user = user();
        Workspace workspace = workspace(workspaceId);
        setupAuth(user, workspace);
        when(aiQuestionRepository.findById(any())).thenReturn(Optional.empty());
        when(oidcUser.getSubject()).thenReturn("sub");

        assertThatThrownBy(() -> service.answer(UUID.randomUUID(), "réponse", oidcUser, "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // U-03 : question d'un autre workspace → 404
    @Test
    void answer_otherWorkspace_throws404() {
        UUID myWorkspaceId = UUID.randomUUID();
        UUID otherWorkspaceId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        User user = user();
        Workspace myWorkspace = workspace(myWorkspaceId);
        Workspace otherWorkspace = workspace(otherWorkspaceId);
        AiQuestion question = question(questionId, otherWorkspace);

        setupAuth(user, myWorkspace);
        when(aiQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(oidcUser.getSubject()).thenReturn("sub");

        assertThatThrownBy(() -> service.answer(questionId, "réponse", oidcUser, "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // Helpers
    private User user() {
        User u = new User();
        u.setEmail("test@example.com");
        return u;
    }

    private Workspace workspace(UUID id) {
        Workspace w = new Workspace();
        w.setId(id);
        return w;
    }

    private AiQuestion question(UUID id, Workspace workspace) {
        CaseFile cf = new CaseFile();
        cf.setWorkspace(workspace);
        AiQuestion q = new AiQuestion();
        q.setId(id);
        q.setCaseFile(cf);
        return q;
    }

    private void setupAuth(User user, Workspace workspace) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        when(authAccountRepository.findByProviderAndProviderUserId(any(), any()))
                .thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(member));
    }
}

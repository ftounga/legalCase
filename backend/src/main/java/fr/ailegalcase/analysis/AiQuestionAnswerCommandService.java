package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AiQuestionAnswerCommandService {

    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public AiQuestionAnswerCommandService(AiQuestionRepository aiQuestionRepository,
                                          AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                          AuthAccountRepository authAccountRepository,
                                          WorkspaceMemberRepository workspaceMemberRepository) {
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.authAccountRepository = authAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public void answer(UUID questionId, String answerText, OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        var workspace = workspaceMemberRepository
                .findFirstByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        AiQuestion question = aiQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (!question.getCaseFile().getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }

        AiQuestionAnswer answer = new AiQuestionAnswer();
        answer.setAiQuestion(question);
        answer.setAnsweredByUser(user);
        answer.setAnswerText(answerText);
        aiQuestionAnswerRepository.save(answer);

        question.setStatus("ANSWERED");
        question.setAnsweredAt(Instant.now());
        aiQuestionRepository.save(question);
    }
}

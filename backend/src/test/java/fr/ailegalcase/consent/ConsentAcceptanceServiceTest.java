package fr.ailegalcase.consent;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.consent.dto.ConsentAcceptanceRequest;
import fr.ailegalcase.consent.dto.ConsentAcceptanceResponse;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * F-240 SF-240-01 — Tests unitaires de {@link ConsentAcceptanceService}.
 *
 * <p>Note d'implémentation : la mini-spec mentionnait {@code IllegalArgumentException}
 * pour les erreurs de validation ; le service lève en réalité
 * {@link ResponseStatusException}(400) — cohérence avec le pattern du projet
 * (CurrentUserResolver, controllers) et mapping automatique par GlobalExceptionHandler.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConsentAcceptanceServiceTest {

    @Mock private UserConsentAcceptanceRepository acceptanceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private HttpServletRequest httpRequest;

    private ConsentAcceptanceService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ConsentAcceptanceService(acceptanceRepository, workspaceMemberRepository);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("consent-test@example.com");

        lenient().when(acceptanceRepository.save(any(UserConsentAcceptance.class)))
                .thenAnswer(invocation -> {
                    UserConsentAcceptance entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID());
                    }
                    return entity;
                });
    }

    private void stubRequest(String forwardedFor, String userAgent, String remoteAddr) {
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn(userAgent);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn(remoteAddr);
    }

    private ConsentAcceptanceRequest request(List<String> types, String version) {
        return new ConsentAcceptanceRequest(types, version);
    }

    // CASE-01
    @Test
    void accept_twoConsentTypes_persistsTwoEntriesWithConsistentTimestamps() {
        stubRequest("82.65.1.1", "Mozilla/5.0", "10.0.0.1");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        ConsentAcceptanceResponse response = service.accept(
                request(List.of("SIGNUP_TERMS", "PRIVACY_POLICY"), "2026-05-11"),
                user, httpRequest);

        assertThat(response.acceptances()).hasSize(2);
        assertThat(response.acceptances()).extracting(ConsentAcceptanceResponse.Acceptance::consentType)
                .containsExactly("SIGNUP_TERMS", "PRIVACY_POLICY");
        assertThat(response.acceptances()).allSatisfy(a -> {
            assertThat(a.acceptedAt()).isNotNull();
            assertThat(a.acceptanceIp()).isEqualTo("82.65.1.1");
            assertThat(a.acceptanceUserAgent()).isEqualTo("Mozilla/5.0");
            assertThat(a.userId()).isEqualTo(user.getId());
        });
        assertThat(response.acceptances().get(0).acceptedAt())
                .isEqualTo(response.acceptances().get(1).acceptedAt());
    }

    // CASE-02
    @Test
    void accept_unknownConsentType_throwsBadRequest() {
        stubRequest(null, "Mozilla/5.0", "10.0.0.1");

        assertThatThrownBy(() -> service.accept(
                request(List.of("UNKNOWN_TYPE"), "2026-05-11"), user, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // CASE-03
    @Test
    void accept_emptyOrNullConsentTypes_throwsBadRequest() {
        assertThatThrownBy(() -> service.accept(request(List.of(), "2026-05-11"), user, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.accept(request(null, "2026-05-11"), user, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // CASE-04
    @Test
    void accept_invalidVersion_throwsBadRequest() {
        String tooLong = "v".repeat(65);
        for (String badVersion : new String[]{null, "", "   ", tooLong}) {
            assertThatThrownBy(() -> service.accept(
                    request(List.of("SIGNUP_TERMS"), badVersion), user, httpRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    // CASE-05
    @Test
    void accept_userWithPrimaryWorkspace_fillsWorkspaceId() {
        stubRequest("82.65.1.1", "Mozilla/5.0", "10.0.0.1");
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(member));

        ConsentAcceptanceResponse response = service.accept(
                request(List.of("PAYMENT_TERMS"), "2026-05-11"), user, httpRequest);

        assertThat(response.acceptances()).hasSize(1);
        assertThat(response.acceptances().get(0).workspaceId()).isEqualTo(workspaceId);
    }

    // CASE-06
    @Test
    void accept_userWithoutPrimaryWorkspace_workspaceIdIsNull() {
        stubRequest("82.65.1.1", "Mozilla/5.0", "10.0.0.1");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        ConsentAcceptanceResponse response = service.accept(
                request(List.of("SIGNUP_TERMS"), "2026-05-11"), user, httpRequest);

        assertThat(response.acceptances().get(0).workspaceId()).isNull();
    }

    // CASE-07
    @Test
    void accept_resolvesIpFromForwardedForList() {
        stubRequest("82.65.1.1, 10.0.0.1", "Mozilla/5.0", "192.168.0.5");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        ConsentAcceptanceResponse response = service.accept(
                request(List.of("SIGNUP_TERMS"), "2026-05-11"), user, httpRequest);

        assertThat(response.acceptances().get(0).acceptanceIp()).isEqualTo("82.65.1.1");
    }

    // CASE-08
    @Test
    void accept_fallsBackToRemoteAddrWhenNoForwardedFor() {
        stubRequest(null, "Mozilla/5.0", "192.168.0.5");
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        ConsentAcceptanceResponse response = service.accept(
                request(List.of("SIGNUP_TERMS"), "2026-05-11"), user, httpRequest);

        assertThat(response.acceptances().get(0).acceptanceIp()).isEqualTo("192.168.0.5");
    }
}

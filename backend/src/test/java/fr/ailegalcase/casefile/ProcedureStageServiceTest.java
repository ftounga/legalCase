package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * F-243 / SF-243-01 — Tests unitaires de {@link ProcedureStageService} :
 * validations de cohérence, cascade et codes d'erreur.
 */
@ExtendWith(MockitoExtension.class)
class ProcedureStageServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private OidcUser oidcUser;
    @Mock private Principal principal;

    private ProcedureStageService service;

    private final UUID caseFileId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        service = new ProcedureStageService(caseFileRepository, workspaceMemberRepository, currentUserResolver);
    }

    private void setupWorkspace(String domain, String country) {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setLegalDomain(domain);
        workspace.setCountry(country);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setPrimary(true);

        caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        lenient().when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        lenient().when(caseFileRepository.save(any(CaseFile.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // U-01 : combinaison valide persistée.
    @Test
    void update_validCombination_persistsAndReturns() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest("CPH", "FOND", "DEMANDEUR");

        ProcedureStageResponse response =
                service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal);

        assertThat(response.jurisdiction()).isEqualTo("CPH");
        assertThat(response.stage()).isEqualTo("FOND");
        assertThat(response.position()).isEqualTo("DEMANDEUR");
        assertThat(response.jurisdictionLabel()).isEqualTo("Conseil de prud'hommes");
        assertThat(caseFile.getProcedureJurisdiction()).isEqualTo("CPH");
        assertThat(caseFile.getProcedureStage()).isEqualTo("FOND");
        assertThat(caseFile.getProcedurePosition()).isEqualTo("DEMANDEUR");
    }

    // U-02 : normalisation trim + uppercase.
    @Test
    void update_lowercaseInput_normalisedToUppercase() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest(" cph ", "fond", "demandeur");

        ProcedureStageResponse response =
                service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal);

        assertThat(response.jurisdiction()).isEqualTo("CPH");
        assertThat(response.stage()).isEqualTo("FOND");
        assertThat(response.position()).isEqualTo("DEMANDEUR");
    }

    // U-03 : stade hors juridiction → 422.
    @Test
    void update_stageNotUnderJurisdiction_throws422() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest("CPH", "APPEL", null);

        assertThatThrownBy(() -> service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // U-04 : position hors stade → 422.
    @Test
    void update_positionNotValidForStage_throws422() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest("CPH", "FOND", "APPELANT");

        assertThatThrownBy(() -> service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // U-05 : valeur hors domaine du dossier → 422.
    @Test
    void update_valueNotInCaseDomain_throws422() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        // JAF est une juridiction du droit de la famille, pas du travail.
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest("JAF", null, null);

        assertThatThrownBy(() -> service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // U-06 : cascade — effacer jurisdiction efface stage + position.
    @Test
    void update_clearJurisdiction_cascadesStageAndPosition() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        caseFile.setProcedureJurisdiction("CPH");
        caseFile.setProcedureStage("FOND");
        caseFile.setProcedurePosition("DEMANDEUR");

        // jurisdiction=null mais stage/position encore fournis → cascade les ignore.
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest(null, "FOND", "DEMANDEUR");

        ProcedureStageResponse response =
                service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal);

        assertThat(response.jurisdiction()).isNull();
        assertThat(response.stage()).isNull();
        assertThat(response.position()).isNull();
        assertThat(caseFile.getProcedureJurisdiction()).isNull();
        assertThat(caseFile.getProcedureStage()).isNull();
        assertThat(caseFile.getProcedurePosition()).isNull();
    }

    // U-07 : cascade — effacer stage efface position.
    @Test
    void update_clearStage_cascadesPosition() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        ProcedureStageUpdateRequest req = new ProcedureStageUpdateRequest("CPH", null, "DEMANDEUR");

        ProcedureStageResponse response =
                service.updateProcedureStage(caseFileId, req, oidcUser, "GOOGLE", principal);

        assertThat(response.jurisdiction()).isEqualTo("CPH");
        assertThat(response.stage()).isNull();
        assertThat(response.position()).isNull();
    }

    // U-08 : tout effacer → 200, dossier vidé.
    @Test
    void update_allNull_clearsEverything() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        caseFile.setProcedureJurisdiction("CPH");
        caseFile.setProcedureStage("FOND");
        caseFile.setProcedurePosition("DEMANDEUR");

        ProcedureStageResponse response = service.updateProcedureStage(
                caseFileId, new ProcedureStageUpdateRequest(null, null, null), oidcUser, "GOOGLE", principal);

        assertThat(response.jurisdiction()).isNull();
        assertThat(response.stage()).isNull();
        assertThat(response.position()).isNull();
    }

    // U-09 : lecture d'un dossier non renseigné → tous champs null.
    @Test
    void getProcedureStage_emptyCaseFile_returnsNulls() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");

        ProcedureStageResponse response =
                service.getProcedureStage(caseFileId, oidcUser, "GOOGLE", principal);

        assertThat(response.caseFileId()).isEqualTo(caseFileId);
        assertThat(response.jurisdiction()).isNull();
        assertThat(response.jurisdictionLabel()).isNull();
        assertThat(response.stage()).isNull();
        assertThat(response.position()).isNull();
    }

    // U-10 : dossier inexistant → 404.
    @Test
    void getProcedureStage_unknownCaseFile_throws404() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        UUID unknown = UUID.randomUUID();
        when(caseFileRepository.findByIdAndDeletedAtIsNull(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProcedureStage(unknown, oidcUser, "GOOGLE", principal))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // U-11 : dossier d'un autre workspace → 403.
    @Test
    void getProcedureStage_differentWorkspace_throws403() {
        setupWorkspace("DROIT_DU_TRAVAIL", "FRANCE");
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());
        caseFile.setWorkspace(otherWorkspace);

        assertThatThrownBy(() -> service.getProcedureStage(caseFileId, oidcUser, "GOOGLE", principal))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // U-12 : options — domaine invalide → 400.
    @Test
    void getOptions_invalidDomain_throws400() {
        assertThatThrownBy(() -> service.getOptions("DROIT_PENAL", "FRANCE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // U-13 : options — pays invalide → 400.
    @Test
    void getOptions_invalidCountry_throws400() {
        assertThatThrownBy(() -> service.getOptions("DROIT_DU_TRAVAIL", "SUISSE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // U-14 : options — combinaison valide → référentiel non vide.
    @Test
    void getOptions_validCombination_returnsCatalog() {
        ProcedureStageOptionsResponse response = service.getOptions("DROIT_FAMILLE", "BELGIQUE");

        assertThat(response.domain()).isEqualTo("DROIT_FAMILLE");
        assertThat(response.country()).isEqualTo("BELGIQUE");
        assertThat(response.jurisdictions()).isNotEmpty();
        assertThat(response.stages()).isNotEmpty();
        assertThat(response.positions()).isNotEmpty();
    }
}

package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-207-04 : service orchestrant l'analyse du délai de déclaration AT
 * Fedris — gate <b>BELGIQUE strict</b> (404 côté FR pour préserver
 * l'isolation BE-only) + isolation workspace standard.
 *
 * <p>Pattern mirroré de {@link PrescriptionBeLitigeTravailService}
 * (SF-207-01) et {@link ContestationC4OnemService} (SF-207-03), adapté à
 * la substance juridique BE accident du travail (Loi 10/04/1971 art. 62 ;
 * AR 21/12/1971 art. 25).</p>
 *
 * <p>Note isolation : la réponse 404 côté FR est <b>indistinguable</b>
 * d'un dossier inconnu — l'outil n'existe pas côté FR. C'est cohérent
 * avec les autres outils BE-only de F-207.</p>
 */
@Service
public class AtFedrisDeclarationService {

    private final AtFedrisDeclarationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AtFedrisDeclarationService(AtFedrisDeclarationRepository repository,
                                      CaseFileRepository caseFileRepository,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      CurrentUserResolver currentUserResolver,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AtFedrisDeclarationResponse calculate(UUID caseFileId,
                                                 AtFedrisDeclarationRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer l'existence
        // de l'outil côté FR — réponse indistinguable d'un dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        validateDates(request);

        AtFedrisDeclarationResult result;
        try {
            result = AtFedrisDeclarationCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AtFedrisDeclarationResponse response = toResponse(caseFileId, country, result);

        AtFedrisDeclarationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AtFedrisDeclarationAnalysis a = new AtFedrisDeclarationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public AtFedrisDeclarationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        AtFedrisDeclarationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de déclaration AT Fedris trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    /**
     * Contrôles de cohérence des dates (au-delà du {@code @NotNull} Bean
     * Validation sur {@code dateAccident}). Toute violation → HTTP 400.
     *
     * <p>Règles (mini-spec) :
     * <ul>
     *   <li>{@code dateAccident} ne peut pas être dans le futur.</li>
     *   <li>{@code dateConnaissanceEmployeur ≥ dateAccident} si renseignée
     *       (l'employeur ne peut pas avoir eu connaissance avant la survenance
     *       de l'accident).</li>
     *   <li>{@code dateDeclarationEffectuee ≥ dateAccident} si renseignée
     *       (la déclaration intervient nécessairement après l'accident).</li>
     * </ul>
     */
    private void validateDates(AtFedrisDeclarationRequest request) {
        LocalDate dateAccident = request.dateAccident();
        if (dateAccident == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateAccident est requise");
        }
        LocalDate today = LocalDate.now(AtFedrisDeclarationCalculator.ZONE_BRUSSELS);
        if (dateAccident.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateAccident ne peut pas être dans le futur");
        }
        if (request.dateConnaissanceEmployeur() != null
                && request.dateConnaissanceEmployeur().isBefore(dateAccident)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateConnaissanceEmployeur doit être postérieure ou égale à dateAccident");
        }
        if (request.dateDeclarationEffectuee() != null
                && request.dateDeclarationEffectuee().isBefore(dateAccident)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateDeclarationEffectuee doit être postérieure ou égale à dateAccident");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private AtFedrisDeclarationResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, AtFedrisDeclarationResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AtFedrisDeclarationResponse toResponse(UUID caseFileId, String country,
                                                   AtFedrisDeclarationResult r) {
        return new AtFedrisDeclarationResponse(
                caseFileId,
                r.dateAccident(),
                r.dateConnaissanceEmployeur(),
                r.dateActionEnvisagee(),
                r.dateDeclarationEffectuee(),
                r.verdict(),
                r.dateLimiteDeclaration(),
                r.joursRestants(),
                r.regleAppliquee(),
                r.baseJuridique(),
                r.formuleCalcul(),
                r.consequencesNonRespect(),
                country);
    }
}

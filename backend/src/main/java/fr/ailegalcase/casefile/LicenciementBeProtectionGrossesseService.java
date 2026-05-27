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
import java.util.UUID;

/**
 * SF-213-05 : service orchestrant l'analyse de la validité d'un licenciement
 * pendant la grossesse / maternité en droit belge (<b>Loi du 16/03/1971
 * art. 40</b>).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir {@link LicenciementBeFormuleClaeysService}
 * SF-213-04) + isolation workspace standard + persistance JSON snapshot.</p>
 *
 * <p>Le validateur sous-jacent
 * ({@link LicenciementBeProtectionGrossesseValidator}) est une fonction pure
 * indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class LicenciementBeProtectionGrossesseService {

    private final LicenciementBeProtectionGrossesseAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementBeProtectionGrossesseService(
            LicenciementBeProtectionGrossesseAnalysisRepository repository,
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
    public LicenciementBeProtectionGrossesseResponse analyze(
            UUID caseFileId,
            LicenciementBeProtectionGrossesseRequest request,
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

        LicenciementBeProtectionGrossesseResult result;
        try {
            result = LicenciementBeProtectionGrossesseValidator.validate(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LicenciementBeProtectionGrossesseResponse response = toResponse(caseFileId, result);

        LicenciementBeProtectionGrossesseAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LicenciementBeProtectionGrossesseAnalysis a =
                            new LicenciementBeProtectionGrossesseAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public LicenciementBeProtectionGrossesseResponse get(UUID caseFileId,
                                                         OidcUser oidcUser,
                                                         Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        LicenciementBeProtectionGrossesseAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse protection grossesse BE trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
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

    private LicenciementBeProtectionGrossesseResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, LicenciementBeProtectionGrossesseResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private LicenciementBeProtectionGrossesseResponse toResponse(
            UUID caseFileId, LicenciementBeProtectionGrossesseResult r) {
        return new LicenciementBeProtectionGrossesseResponse(
                caseFileId,
                r.dateDebutGrossesse(),
                r.dateAccouchement(),
                r.dateCongeMaterniteDebut(),
                r.dateCongeMaterniteFinale(),
                r.dateLicenciement(),
                r.grossesseNotifieeParEcrit(),
                r.remunerationMensuelleBrute(),
                r.motifInvoqueParEmployeur(),
                r.verdict(),
                r.licenciementDansLaPeriodeProtegee(),
                r.dateDebutProtection(),
                r.dateFinProtection(),
                r.indemniteForfaitaire(),
                r.chargePreuveEmployeur(),
                r.baseJuridique(),
                r.formuleCalcul(),
                r.avertissement());
    }
}

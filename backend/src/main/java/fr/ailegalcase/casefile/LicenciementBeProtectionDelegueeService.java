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
 * SF-213-08 : service orchestrant l'analyse de la protection d'un délégué
 * syndical / candidat non élu BE en cas de licenciement
 * (Loi 19/03/1991 + CCT n° 5 du 24/05/1971).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir SF-213-07) + isolation workspace standard
 * + persistance JSON snapshot.</p>
 *
 * <p>Le validateur sous-jacent
 * ({@link LicenciementBeProtectionDelegueeValidator}) est une fonction pure
 * indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class LicenciementBeProtectionDelegueeService {

    private final LicenciementBeProtectionDelegueeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementBeProtectionDelegueeService(
            LicenciementBeProtectionDelegueeAnalysisRepository repository,
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
    public LicenciementBeProtectionDelegueeResponse analyze(
            UUID caseFileId,
            LicenciementBeProtectionDelegueeRequest request,
            OidcUser oidcUser,
            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer
        // l'existence de l'outil côté FR — réponse indistinguable d'un
        // dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        LicenciementBeProtectionDelegueeResult result;
        try {
            result = LicenciementBeProtectionDelegueeValidator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LicenciementBeProtectionDelegueeResponse response = toResponse(caseFileId, result);

        LicenciementBeProtectionDelegueeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LicenciementBeProtectionDelegueeAnalysis a =
                            new LicenciementBeProtectionDelegueeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public LicenciementBeProtectionDelegueeResponse get(
            UUID caseFileId,
            OidcUser oidcUser,
            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        LicenciementBeProtectionDelegueeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse licenciement BE protection délégué "
                                + "trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private LicenciementBeProtectionDelegueeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    LicenciementBeProtectionDelegueeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private LicenciementBeProtectionDelegueeResponse toResponse(
            UUID caseFileId, LicenciementBeProtectionDelegueeResult r) {
        return new LicenciementBeProtectionDelegueeResponse(
                caseFileId,
                r.statutProtege(),
                r.ancienneteAnnees(),
                r.remunerationAnnuelleBrute(),
                r.dateLicenciement(),
                r.dateElectionOuMandat(),
                r.demandeReintegrationDansTrente(),
                r.employeurRefuseReintegration(),
                r.circonstancesAggravantes(),
                r.verdict(),
                r.licenciementDansProtection(),
                r.dateFinProtection(),
                r.indemniteForfaitaire(),
                r.anneesForfait(),
                r.dateLimiteDemandeReintegration(),
                r.joursRestantsReintegration(),
                r.delaiReintegrationDepasse(),
                r.baseJuridique(),
                r.avertissement());
    }
}

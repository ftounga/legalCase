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
 * SF-219-07 : service orchestrant la checklist procédurale licenciement
 * collectif BE (Loi du 13/02/1998 « Renault » + CCT n° 24 + CCT n° 39 +
 * délai d'attente 30 jours).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06) + isolation workspace
 * standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) + persistance
 * JSON snapshot.</p>
 *
 * <p>Le procedure checker sous-jacent ({@link
 * LicenciementBeCollectifRenaultProcedureChecker}) est une fonction
 * pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class LicenciementBeCollectifRenaultService {

    private final LicenciementBeCollectifRenaultAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementBeCollectifRenaultService(
            LicenciementBeCollectifRenaultAnalysisRepository repository,
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
    public LicenciementBeCollectifRenaultResponse analyze(
            UUID caseFileId,
            LicenciementBeCollectifRenaultRequest request,
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
        // dossier inconnu (PSE FR procéduralement très différent, pas
        // de mapping possible).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        LicenciementBeCollectifRenaultResult result;
        try {
            result = LicenciementBeCollectifRenaultProcedureChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LicenciementBeCollectifRenaultResponse response = toResponse(caseFileId, result);

        LicenciementBeCollectifRenaultAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            LicenciementBeCollectifRenaultAnalysis a =
                                    new LicenciementBeCollectifRenaultAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public LicenciementBeCollectifRenaultResponse get(
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

        LicenciementBeCollectifRenaultAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse de licenciement collectif BE"
                                        + " (loi Renault) trouvée pour ce dossier"));
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

    private LicenciementBeCollectifRenaultResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    LicenciementBeCollectifRenaultResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private LicenciementBeCollectifRenaultResponse toResponse(
            UUID caseFileId, LicenciementBeCollectifRenaultResult r) {
        return new LicenciementBeCollectifRenaultResponse(
                caseFileId,
                r.dateProjet(),
                r.tailleEntreprise(),
                r.effectifMoyen(),
                r.nombreLicenciementsEnvisages(),
                r.phaseAtteinte(),
                r.informationEcriteCePrealable(),
                r.documentsLegauxCommuniques(),
                r.consultationEffectiveTenue(),
                r.reponsesMotiveesEmployeur(),
                r.notificationAutoriteRegionaleFaite(),
                r.dateNotificationAutorite(),
                r.datePremierPreavisNotifie(),
                r.verdict(),
                r.conforme(),
                r.seuilDeclenchementAtteint(),
                r.seuilLegalApplicable(),
                r.dateFinDelaiAttente(),
                r.delaiAttenteRespecte(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}

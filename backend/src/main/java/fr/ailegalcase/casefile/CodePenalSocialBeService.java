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
 * SF-219-24 : service orchestrant l'analyse <i>Code pénal social BE
 * — qualification d'infraction + niveau de sanction</i> (Loi du
 * 06/06/2010 introduisant le Code pénal social, M.B. 01/07/2010).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/
 * 17/18/19/20/21/22) + isolation workspace standard + gate domaine
 * {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>La France a un régime distinct (Code du travail Livre VIII —
 * sanctions pénales art. L. 8221-1 et s. travail illégal, art.
 * L. 4741-1 et s. santé / sécurité, art. L. 2316-1 IRP ; Code pénal
 * art. 222-9 violences ; quanta différents : 45 000 € amende
 * travail dissimulé personne physique, 225 000 € personne morale).
 * Restitution séparée par outil — pas de tentative d'harmonisation
 * FR/BE.</p>
 *
 * <p>Le checker sous-jacent ({@link CodePenalSocialBeChecker}) est
 * une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class CodePenalSocialBeService {

    private final CodePenalSocialBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CodePenalSocialBeService(
            CodePenalSocialBeAnalysisRepository repository,
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
    public CodePenalSocialBeResponse analyze(
            UUID caseFileId,
            CodePenalSocialBeRequest request,
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
        // l'existence de l'outil côté FR — pattern miroir
        // SF-219-06/08/10/12/14/15/16/17/18/19/20/21/22. La France a
        // un régime distinct (Code du travail Livre VIII + Code pénal
        // travail illégal art. L. 8221-1 et s. ; quanta différents,
        // 45 000 € amende personne physique / 225 000 € personne
        // morale). Restitution séparée par outil — pas de tentative
        // d'harmonisation FR/BE.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        CodePenalSocialBeResult result;
        try {
            result = CodePenalSocialBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        CodePenalSocialBeResponse response = toResponse(caseFileId, result);

        CodePenalSocialBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            CodePenalSocialBeAnalysis a =
                                    new CodePenalSocialBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public CodePenalSocialBeResponse get(
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

        CodePenalSocialBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse Code pénal social BE"
                                        + " trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId()
                        .equals(cf.getWorkspace().getId()))
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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private CodePenalSocialBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    CodePenalSocialBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private CodePenalSocialBeResponse toResponse(
            UUID caseFileId, CodePenalSocialBeResult r) {
        return new CodePenalSocialBeResponse(
                caseFileId,
                r.typeInfraction(),
                r.niveauPropose(),
                r.dateFaits(),
                r.nombreTravailleursConcernes(),
                r.personneMorale(),
                r.recidiveDansLAn(),
                r.prevenuPreposeOuMandataire(),
                r.elementMoralIntentionnel(),
                r.verdict(),
                r.niveauSanction(),
                r.amendeAdminMin(),
                r.amendeAdminMax(),
                r.amendePenaleMin(),
                r.amendePenaleMax(),
                r.emprisonnementApplicable(),
                r.emprisonnementMinMois(),
                r.emprisonnementMaxMois(),
                r.multiplicationParTravailleur(),
                r.majorationPersonneMorale(),
                r.majorationRecidive(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}

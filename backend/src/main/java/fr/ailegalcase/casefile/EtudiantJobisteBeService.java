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
 * SF-219-13 : service orchestrant l'analyse <i>étudiant jobiste BE</i>
 * (Loi du 03/07/1978, Titre VII + Loi-programme 24/12/2002 + AR
 * 14/07/1995 + Loi-programme 22/12/2023 — quota 600h/an + cotisations
 * réduites + Dimona STU).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>Le checker sous-jacent ({@link EtudiantJobisteBeChecker}) est une
 * fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class EtudiantJobisteBeService {

    private final EtudiantJobisteBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public EtudiantJobisteBeService(
            EtudiantJobisteBeAnalysisRepository repository,
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
    public EtudiantJobisteBeResponse analyze(
            UUID caseFileId,
            EtudiantJobisteBeRequest request,
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
        // l'existence de l'outil côté FR — pattern miroir SF-219-06/08/10/12.
        // Le régime français du "job étudiant" relève du droit commun
        // du travail FR (Code du travail art. L. 1242-1 et s.) sans
        // cotisations réduites spécifiques, et la Dimona n'existe pas
        // en France. Restitution séparée par outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        EtudiantJobisteBeResult result;
        try {
            result = EtudiantJobisteBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        EtudiantJobisteBeResponse response = toResponse(caseFileId, result);

        EtudiantJobisteBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            EtudiantJobisteBeAnalysis a = new EtudiantJobisteBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public EtudiantJobisteBeResponse get(
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

        EtudiantJobisteBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse étudiant jobiste BE trouvée"
                                        + " pour ce dossier"));
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

    private EtudiantJobisteBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, EtudiantJobisteBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private EtudiantJobisteBeResponse toResponse(UUID caseFileId,
                                                 EtudiantJobisteBeResult r) {
        return new EtudiantJobisteBeResponse(
                caseFileId,
                r.dateDebutOccupation(),
                r.statutEtudiant(),
                r.heuresDejaPresteesDansAnnee(),
                r.heuresContratEnCours(),
                r.quotaAnnuelHeures(),
                r.contratEcritSigne(),
                r.dimonaStuDeclaree(),
                r.cotisationsReduitesAppliquees(),
                r.remunerationBruteHoraire(),
                r.verdict(),
                r.statutEligible(),
                r.quotaRespecte(),
                r.formalismeRespecte(),
                r.cotisationsConformes(),
                r.heuresRestantesAvantContrat(),
                r.heuresHorsQuota(),
                r.montantRedressementEstime(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}

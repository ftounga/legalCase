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
 * SF-219-26 : service orchestrant l'analyse <i>Travail noir BE —
 * DIMONA, requalification et sanctions</i> (Loi-programme du
 * 24/12/2002 art. 167-184 + AR du 05/11/2002 + Code pénal social
 * art. 181 niveau 4).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/
 * 17/18/19/20/21/22/23/24) + isolation workspace standard + gate
 * domaine {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>La France a un régime distinct (Code du travail art. L. 8221-1
 * et s. travail dissimulé, amende 45 000 € personne physique /
 * 225 000 € personne morale art. L. 8224-1, URSSAF redressement
 * majoration 25 %, DPAE art. L. 1221-10 et non DIMONA, procédure
 * parquet et non auditorat). Restitution séparée par outil — pas de
 * tentative d'harmonisation FR/BE.</p>
 *
 * <p>Le checker sous-jacent ({@link TravailNoirBeDimonaChecker}) est
 * une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class TravailNoirBeDimonaService {

    private final TravailNoirBeDimonaAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TravailNoirBeDimonaService(
            TravailNoirBeDimonaAnalysisRepository repository,
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
    public TravailNoirBeDimonaResponse analyze(
            UUID caseFileId,
            TravailNoirBeDimonaRequest request,
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
        // SF-219-06/08/10/12/14/15/16/17/18/19/20/21/22/23/24. La
        // France a un régime distinct (Code du travail art. L. 8221-1
        // et s., DPAE et non DIMONA, parquet et non auditorat, quanta
        // sensiblement différents). Restitution séparée par outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        TravailNoirBeDimonaResult result;
        try {
            result = TravailNoirBeDimonaChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        TravailNoirBeDimonaResponse response = toResponse(caseFileId, result);

        TravailNoirBeDimonaAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            TravailNoirBeDimonaAnalysis a =
                                    new TravailNoirBeDimonaAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public TravailNoirBeDimonaResponse get(
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

        TravailNoirBeDimonaAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse Travail noir BE DIMONA"
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

    private TravailNoirBeDimonaResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    TravailNoirBeDimonaResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private TravailNoirBeDimonaResponse toResponse(
            UUID caseFileId, TravailNoirBeDimonaResult r) {
        return new TravailNoirBeDimonaResponse(
                caseFileId,
                r.statutDimona(),
                r.dateDebutOccupation(),
                r.dateDimonaEffective(),
                r.dateControle(),
                r.salaireBrutMensuel(),
                r.nombreTravailleursConcernes(),
                r.personneMorale(),
                r.recidiveDansLAn(),
                r.elementsSubordination(),
                r.verdict(),
                r.dureeNonDeclareeJours(),
                r.cotisationsOnssEmployeur(),
                r.cotisationsOnssTravailleur(),
                r.cotisationsOnssTotal(),
                r.amendeOnssForfaitaire3x(),
                r.sanctionPenaleNiveau4Applicable(),
                r.amendePenaleAdminMin(),
                r.amendePenaleAdminMax(),
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

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
 * SF-219-23 : service orchestrant l'analyse <i>refus d'aménagements
 * raisonnables handicap BE</i> (Loi du 10/05/2007 art. 4 + art. 14 +
 * art. 17 + art. 28 + CCT n° 95 + Directive 2000/78/CE art. 5 +
 * Convention ONU 13/12/2006 art. 27 + Convention OIT n° 159).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/
 * 17/18/19/21/22) + isolation workspace standard + gate domaine
 * {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>La France a un dispositif distinct (Code du travail art. L.
 * 5213-1 et s. obligation d'emploi des travailleurs handicapés OETH
 * 6 % + art. L. 5213-6 obligation d'aménagement raisonnable + art.
 * L. 1132-1 prohibition discrimination + Code de l'action sociale et
 * des familles art. L. 114 définition du handicap + Décret n° 2009-1272
 * + Décret n° 2020-1208). Les définitions, seuils, sanctions et
 * organismes de subside diffèrent substantiellement. Restitution
 * séparée par outil — pas de tentative d'harmonisation FR/BE.</p>
 *
 * <p>Le checker sous-jacent
 * ({@link DiscriminationBeHandicapAmenagementChecker}) est une
 * fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class DiscriminationBeHandicapAmenagementService {

    private final DiscriminationBeHandicapAmenagementAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DiscriminationBeHandicapAmenagementService(
            DiscriminationBeHandicapAmenagementAnalysisRepository repository,
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
    public DiscriminationBeHandicapAmenagementResponse analyze(
            UUID caseFileId,
            DiscriminationBeHandicapAmenagementRequest request,
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
        // SF-219-06/08/10/12/14/15/16/17/18/19/21/22. La France a un
        // dispositif distinct (Code du travail art. L. 5213-1 et s.
        // OETH + L. 5213-6 + L. 1132-1 + Décret n° 2009-1272 + Décret
        // n° 2020-1208). Restitution séparée par outil — pas de
        // tentative d'harmonisation FR/BE.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        DiscriminationBeHandicapAmenagementResult result;
        try {
            result = DiscriminationBeHandicapAmenagementChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        DiscriminationBeHandicapAmenagementResponse response =
                toResponse(caseFileId, result);

        DiscriminationBeHandicapAmenagementAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            DiscriminationBeHandicapAmenagementAnalysis a =
                                    new DiscriminationBeHandicapAmenagementAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public DiscriminationBeHandicapAmenagementResponse get(
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

        DiscriminationBeHandicapAmenagementAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse discrimination handicap"
                                        + " aménagement BE trouvée pour ce"
                                        + " dossier"));
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

    private DiscriminationBeHandicapAmenagementResponse deserialize(
            String json) {
        try {
            return objectMapper.readValue(json,
                    DiscriminationBeHandicapAmenagementResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private DiscriminationBeHandicapAmenagementResponse toResponse(
            UUID caseFileId,
            DiscriminationBeHandicapAmenagementResult r) {
        return new DiscriminationBeHandicapAmenagementResponse(
                caseFileId,
                r.statutHandicap(),
                r.dateDemandeAmenagement(),
                r.typeAmenagementDemande(),
                r.coutEstimeAmenagement(),
                r.subsidesDemandes(),
                r.effectifEntreprise(),
                r.chiffreAffairesAnnuel(),
                r.reponseEmployeur(),
                r.motivationDetailleeFournie(),
                r.avisSeppFavorable(),
                r.chargeDisproportionneeInvoquee(),
                r.devisExterneFourni(),
                r.mesuresAlternativesProposees(),
                r.sanctionSubie(),
                r.dateSanction(),
                r.salaireMensuelBrut(),
                r.procedureUniaSaisie(),
                r.verdict(),
                r.handicapQualifie(),
                r.demandeFormalisee(),
                r.refusCaracterise(),
                r.chargeDisproportionneeDemontree(),
                r.representaillesPresumees(),
                r.indemniteForfaitaire6Mois(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}

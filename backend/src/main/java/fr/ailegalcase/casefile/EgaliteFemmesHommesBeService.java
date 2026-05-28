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
 * SF-219-22 : service orchestrant l'analyse <i>égalité salariale
 * femmes / hommes BE</i> (Loi du 22/04/2012 + AR 17/08/2013 + AR
 * 25/04/2014 + CCT n° 25 + Loi 10/05/2007 + C. pén. social
 * art. 195/1).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/
 * 17/18/19) + isolation workspace standard + gate domaine
 * {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>La France a un régime distinct (Code du travail art. L. 1142-7 et
 * s. — index égalité professionnelle annuel, art. L. 1142-8
 * publication, art. L. 1142-9 plan de rattrapage en cas de note
 * &lt; 75/100 ; Décret n° 2019-15 du 08/01/2019 — qui couvre les
 * entreprises ≥ 50 salariés selon une méthodologie de notation
 * différente). Restitution séparée par outil — pas de tentative
 * d'harmonisation FR/BE.</p>
 *
 * <p>Le checker sous-jacent ({@link EgaliteFemmesHommesBeChecker}) est
 * une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class EgaliteFemmesHommesBeService {

    private final EgaliteFemmesHommesBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public EgaliteFemmesHommesBeService(
            EgaliteFemmesHommesBeAnalysisRepository repository,
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
    public EgaliteFemmesHommesBeResponse analyze(
            UUID caseFileId,
            EgaliteFemmesHommesBeRequest request,
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
        // SF-219-06/08/10/12/14/15/16/17/18/19. La France a un régime
        // d'index égalité professionnelle distinct (Code du travail
        // art. L. 1142-7 et s. + Décret n° 2019-15 du 08/01/2019).
        // Restitution séparée par outil — pas de tentative
        // d'harmonisation FR/BE.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        EgaliteFemmesHommesBeResult result;
        try {
            result = EgaliteFemmesHommesBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        EgaliteFemmesHommesBeResponse response = toResponse(caseFileId, result);

        EgaliteFemmesHommesBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            EgaliteFemmesHommesBeAnalysis a =
                                    new EgaliteFemmesHommesBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public EgaliteFemmesHommesBeResponse get(
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

        EgaliteFemmesHommesBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse égalité salariale H/F"
                                        + " BE trouvée pour ce dossier"));
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

    private EgaliteFemmesHommesBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    EgaliteFemmesHommesBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private EgaliteFemmesHommesBeResponse toResponse(
            UUID caseFileId, EgaliteFemmesHommesBeResult r) {
        return new EgaliteFemmesHommesBeResponse(
                caseFileId,
                r.effectifEntreprise(),
                r.statutRapport(),
                r.dateDepotRapport(),
                r.ventilationNiveauFonctionFournie(),
                r.ventilationAncienneteFournie(),
                r.ventilationQualificationFournie(),
                r.ventilationRegimeTravailFournie(),
                r.ventilationComposantsRemunerationFournie(),
                r.ecartSalarialNonJustifieConstate(),
                r.pourcentageEcartConstate(),
                r.planActionEtabli(),
                r.mediateurDesigne(),
                r.plainteIefhOuInspectionEnCours(),
                r.verdict(),
                r.seuilAtteint(),
                r.rapportDepose(),
                r.ventilationComplete(),
                r.planActionRequis(),
                r.planActionConforme(),
                r.typeFormulaireRequis(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}

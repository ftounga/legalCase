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
 * SF-219-06 : service orchestrant l'analyse <i>licenciement BE
 * fermeture entreprise</i> (Loi 26/06/2002 + AR 23/03/2007 + CCT
 * n° 9bis, Fonds Fermeture Entreprises FFE).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-04) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>Le calculator sous-jacent ({@link
 * LicenciementBeFermetureEntrepriseCalculator}) est une fonction pure
 * indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class LicenciementBeFermetureEntrepriseService {

    private final LicenciementBeFermetureEntrepriseAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementBeFermetureEntrepriseService(
            LicenciementBeFermetureEntrepriseAnalysisRepository repository,
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
    public LicenciementBeFermetureEntrepriseResponse analyze(
            UUID caseFileId,
            LicenciementBeFermetureEntrepriseRequest request,
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
        // dossier inconnu (aucun équivalent strict du FFE en droit FR).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        LicenciementBeFermetureEntrepriseResult result;
        try {
            result = LicenciementBeFermetureEntrepriseCalculator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LicenciementBeFermetureEntrepriseResponse response = toResponse(caseFileId, result);

        LicenciementBeFermetureEntrepriseAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            LicenciementBeFermetureEntrepriseAnalysis a =
                                    new LicenciementBeFermetureEntrepriseAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public LicenciementBeFermetureEntrepriseResponse get(
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

        LicenciementBeFermetureEntrepriseAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse de fermeture d'entreprise BE"
                                        + " trouvée pour ce dossier"));
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

    private LicenciementBeFermetureEntrepriseResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    LicenciementBeFermetureEntrepriseResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private LicenciementBeFermetureEntrepriseResponse toResponse(
            UUID caseFileId, LicenciementBeFermetureEntrepriseResult r) {
        return new LicenciementBeFermetureEntrepriseResponse(
                caseFileId,
                r.dateNaissance(),
                r.dateDebutContrat(),
                r.dateFermeture(),
                r.remunerationMensuelleBrute(),
                r.typeFermeture(),
                r.statutEmployeur(),
                r.effectifEtp(),
                r.salairesImpayes(),
                r.peculeVacancesImpaye(),
                r.indemniteRuptureImpayee(),
                r.verdict(),
                r.eligible(),
                r.ageADateFermeture(),
                r.anneesAnciennete(),
                r.indemniteFermeture(),
                r.montantForfaitaireParAnnee(),
                r.supplementAgeMensuel(),
                r.montantTotalCreancesFfe(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}

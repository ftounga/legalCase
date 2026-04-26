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
import java.util.List;
import java.util.UUID;

/**
 * SF-IM-17-01 : service de l'analyse du régime franco-algérien (accord 27/12/1968).
 * Gates : DROIT_IMMIGRATION + FRANCE + nationalité algérienne.
 */
@Service
public class RegimeAlgerienService {

    private final RegimeAlgerienRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RegimeAlgerienService(RegimeAlgerienRepository repository,
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
    public RegimeAlgerienResponse calculate(UUID caseFileId,
                                            RegimeAlgerienRequest request,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Accord franco-algérien applicable uniquement en France");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        // Gate métier : nationalité algérienne. Default true si non renseigné explicitement.
        if (Boolean.FALSE.equals(request.nationaliteAlgerienne())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime applicable uniquement aux ressortissants algériens"
                            + " (accord franco-algérien 27/12/1968)");
        }

        boolean casierVierge = !Boolean.FALSE.equals(request.casierJudiciaireVierge());

        RegimeAlgerienResult result;
        try {
            result = RegimeAlgerienCalculator.compute(
                    request.voieDemande(),
                    request.documentEtatCivilOriginal(),
                    request.presenceReguliereFranceMois(),
                    casierVierge,
                    request.visaLongSejourValide(),
                    request.conjointFrancais(),
                    request.parentEnfantFrancais(),
                    request.neEnFrance(),
                    request.arriveeAvant13Ans(),
                    request.contratTravailValide(),
                    request.ressourcesSuffisantes(),
                    request.logementDecent(),
                    request.nombrePersonnesFoyer());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RegimeAlgerienAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RegimeAlgerienAnalysis a = new RegimeAlgerienAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setVoieDemande(result.voieDemande());
        entity.setNationaliteAlgerienne(true);
        entity.setDocumentEtatCivilOriginal(request.documentEtatCivilOriginal());
        entity.setPresenceReguliereFranceMois(request.presenceReguliereFranceMois());
        entity.setCasierJudiciaireVierge(casierVierge);
        entity.setVisaLongSejourValide(request.visaLongSejourValide());
        entity.setConjointFrancais(request.conjointFrancais());
        entity.setParentEnfantFrancais(request.parentEnfantFrancais());
        entity.setNeEnFrance(request.neEnFrance());
        entity.setArriveeAvant13Ans(request.arriveeAvant13Ans());
        entity.setContratTravailValide(request.contratTravailValide());
        entity.setRessourcesSuffisantes(request.ressourcesSuffisantes());
        entity.setLogementDecent(request.logementDecent());
        entity.setNombrePersonnesFoyer(request.nombrePersonnesFoyer());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RegimeAlgerienResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RegimeAlgerienAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse régime algérien trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
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

    private RegimeAlgerienResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, RegimeAlgerienResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private RegimeAlgerienResponse toResponse(UUID caseFileId, String country,
                                              RegimeAlgerienResult r) {
        return new RegimeAlgerienResponse(
                caseFileId,
                country,
                r.voieDemande(),
                r.voieRecommandee(),
                r.verdictRecevabilite(),
                r.titreApplicable(),
                r.dureeTitreAnnees(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : List.of(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.delaiInstructionMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}

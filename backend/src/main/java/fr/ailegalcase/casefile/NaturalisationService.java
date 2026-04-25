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
 * SF-IM-13-01 : service de l'analyse de naturalisation française. Gates : DROIT_IMMIGRATION + FRANCE.
 */
@Service
public class NaturalisationService {

    private final NaturalisationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public NaturalisationService(NaturalisationRepository repository,
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
    public NaturalisationResponse calculate(UUID caseFileId,
                                            NaturalisationRequest request,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime de naturalisation propre à la France (Code civil art. 21-15+)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        // Default true : casier vierge sauf information contraire explicite
        boolean casierVierge = !Boolean.FALSE.equals(request.casierJudiciaireVierge());
        // Default false : opposition inactive sauf information contraire explicite
        boolean oppositionActive = Boolean.TRUE.equals(request.oppositionGouvernementaleActive());

        NaturalisationResult result;
        try {
            result = NaturalisationCalculator.compute(
                    request.voieNaturalisation(),
                    request.dureeResidenceReguliereAnnees(),
                    request.dureeMariageAnnees(),
                    request.cohabitationContinue(),
                    request.ageDemandeur(),
                    request.ascendantDirectFrancais(),
                    request.parentAcquiertNationalite(),
                    request.vitAvecParentAcquereur(),
                    request.ancienFrancais(),
                    casierVierge,
                    request.assimilationLangueB1(),
                    request.ressourcesStables(),
                    oppositionActive,
                    request.etudesSuperieuresFrance());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        NaturalisationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    NaturalisationAnalysis a = new NaturalisationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setVoieNaturalisation(result.voieNaturalisation());
        entity.setDureeResidenceReguliereAnnees(request.dureeResidenceReguliereAnnees());
        entity.setDureeMariageAnnees(request.dureeMariageAnnees());
        entity.setCohabitationContinue(request.cohabitationContinue());
        entity.setAgeDemandeur(request.ageDemandeur());
        entity.setAscendantDirectFrancais(request.ascendantDirectFrancais());
        entity.setParentAcquiertNationalite(request.parentAcquiertNationalite());
        entity.setVitAvecParentAcquereur(request.vitAvecParentAcquereur());
        entity.setAncienFrancais(request.ancienFrancais());
        entity.setCasierJudiciaireVierge(casierVierge);
        entity.setAssimilationLangueB1(request.assimilationLangueB1());
        entity.setRessourcesStables(request.ressourcesStables());
        entity.setOppositionGouvernementaleActive(oppositionActive);
        entity.setEtudesSuperieuresFrance(request.etudesSuperieuresFrance());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public NaturalisationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        NaturalisationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de naturalisation trouvée pour ce dossier"));
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

    private NaturalisationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, NaturalisationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private NaturalisationResponse toResponse(UUID caseFileId, String country,
                                              NaturalisationResult r) {
        return new NaturalisationResponse(
                caseFileId,
                country,
                r.voieNaturalisation(),
                r.voieRecommandee(),
                r.verdictRecevabilite(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : List.of(),
                r.documentsAFournir() != null ? r.documentsAFournir() : List.of(),
                r.delaiInstructionMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}

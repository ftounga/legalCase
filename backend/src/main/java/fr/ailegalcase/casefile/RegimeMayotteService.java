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
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-220-02 : service de l'outil décisionnel "Portée territoriale du titre à
 * Mayotte" (F-IM-48-regime-mayotte-fr). Outil single-country FR.
 *
 * <p>Pattern miroir de {@link RegimeTunisienService} (SF-220-01).</p>
 */
@Service
public class RegimeMayotteService {

    private final RegimeMayotteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RegimeMayotteService(RegimeMayotteRepository repository,
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
    public RegimeMayotteResponse analyze(UUID caseFileId, RegimeMayotteRequest request,
                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime Mayotte — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.typeTitre() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ typeTitre est requis : VPF | SALARIE | ETUDIANT | RESIDENT | AUTRE");
        }

        boolean titreDelivreAMayotte = Boolean.TRUE.equals(request.titreDelivreAMayotte());
        boolean projetDeplacementMetropole = Boolean.TRUE.equals(request.projetDeplacementMetropole());
        LocalDate dateDelivrance = request.dateDelivrance();

        RegimeMayotteResult result;
        try {
            result = RegimeMayotteAnalyzer.analyze(
                    titreDelivreAMayotte,
                    request.typeTitre(),
                    projetDeplacementMetropole);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RegimeMayotteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RegimeMayotteAnalysis a = new RegimeMayotteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTitreDelivreAMayotte(result.titreDelivreAMayotte());
        entity.setTypeTitre(result.typeTitre());
        entity.setProjetDeplacementMetropole(result.projetDeplacementMetropole());
        entity.setDateDelivrance(dateDelivrance);
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, dateDelivrance, result);
    }

    @Transactional(readOnly = true)
    public RegimeMayotteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RegimeMayotteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse régime Mayotte trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, entity.getDateDelivrance(),
                deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private RegimeMayotteResult deserialize(String json) {
        try { return objectMapper.readValue(json, RegimeMayotteResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RegimeMayotteResponse toResponse(UUID caseFileId, String country,
                                             LocalDate dateDelivrance, RegimeMayotteResult r) {
        return new RegimeMayotteResponse(
                caseFileId,
                r.titreDelivreAMayotte(),
                r.typeTitre(),
                r.projetDeplacementMetropole(),
                dateDelivrance,
                country,
                r.porteeTerritoriale(),
                r.sousStatutDeplacement(),
                r.obligationsSpecifiques(),
                r.demarchesDeplacementMetropole(),
                r.basesJuridiques(),
                r.messages());
    }
}

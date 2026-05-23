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
 * SF-216-29 : service orchestrant l'outil Donation-partage FR (art. 1075 à
 * 1075-5 Cciv + art. 1078, 1078-1, 1080 + art. 912-928).
 * Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class DonationPartageService {

    private final DonationPartageAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DonationPartageService(
            DonationPartageAnalysisRepository repository,
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
    public DonationPartageResponse calculate(UUID caseFileId,
                                             DonationPartageRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        DonationPartageResult result;
        try {
            result = DonationPartageCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DonationPartageAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DonationPartageAnalysis a = new DonationPartageAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DonationPartageResponse get(UUID caseFileId,
                                       OidcUser oidcUser,
                                       Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DonationPartageAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Donation-partage trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(DonationPartageRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-DONATION-PARTAGE applicable uniquement en France "
                            + "(art. 1075 à 1075-5 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.nombreDescendants() == null || req.nombreDescendants() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nombreDescendants doit être >= 1.");
        }
        if (req.valeurPartageTotal() != null && req.valeurPartageTotal() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "valeurPartageTotal doit être >= 0.");
        }
        if (req.agesDonateurs() != null) {
            if (req.agesDonateurs().size() > 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "agesDonateurs : au plus 2 éléments (couple de donateurs).");
            }
            for (Integer age : req.agesDonateurs()) {
                if (age != null && age < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "agesDonateurs : âges négatifs interdits.");
                }
            }
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private DonationPartageResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, DonationPartageResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private DonationPartageResponse toResponse(UUID caseFileId, String country,
                                               DonationPartageResult r) {
        return new DonationPartageResponse(
                caseFileId,
                r.conditionsRemplies(),
                r.interet(),
                r.gelValeurEffet(),
                r.rapportExclu(),
                r.alerteQuotite(),
                r.etapesNotariales(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}

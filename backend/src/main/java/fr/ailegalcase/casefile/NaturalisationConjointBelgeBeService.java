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
 * SF-215-09 : service applicatif de l'outil naturalisation conjoint Belge BE
 * (Code de la nationalité belge art. 16 — déclaration par mariage avec un(e) Belge).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 *
 * <p>Pattern miroir de {@link Naturalisation12bisBeService} (SF-215-07).
 */
@Service
public class NaturalisationConjointBelgeBeService {

    private final NaturalisationConjointBelgeBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public NaturalisationConjointBelgeBeService(
            NaturalisationConjointBelgeBeRepository repository,
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
    public NaturalisationConjointBelgeBeResponse calculate(UUID caseFileId,
                                                           NaturalisationConjointBelgeBeRequest request,
                                                           OidcUser oidcUser,
                                                           Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Naturalisation conjoint Belge art. 16 BE uniquement — pour la France voir "
                            + "F-IM-13-naturalisation (Cciv art. 21-2 par mariage)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        NaturalisationConjointBelgeBeResult result;
        try {
            result = NaturalisationConjointBelgeBeCalculator.compute(
                    request.dateMarriage(),
                    request.cohabitationLegale(),
                    request.dureeCohabitationMois(),
                    request.niveauLangue(),
                    request.preuveIntegration(),
                    request.menaceOrdrePublic(),
                    request.condamnationPenale());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        NaturalisationConjointBelgeBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    NaturalisationConjointBelgeBeAnalysis a = new NaturalisationConjointBelgeBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateMarriage(result.dateMarriage());
        entity.setCohabitationLegale(result.cohabitationLegale());
        entity.setDureeCohabitationMois(result.dureeCohabitationMois());
        entity.setNiveauLangue(result.niveauLangue());
        entity.setPreuveIntegration(result.preuveIntegration());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setCondamnationPenale(result.condamnationPenale());
        entity.setEligible(result.eligible());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public NaturalisationConjointBelgeBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        NaturalisationConjointBelgeBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse naturalisation conjoint Belge BE trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private NaturalisationConjointBelgeBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, NaturalisationConjointBelgeBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private NaturalisationConjointBelgeBeResponse toResponse(UUID caseFileId, NaturalisationConjointBelgeBeResult r) {
        return new NaturalisationConjointBelgeBeResponse(
                caseFileId,
                r.dateMarriage(),
                r.cohabitationLegale(),
                r.dureeCohabitationMois(),
                r.niveauLangue(),
                r.preuveIntegration(),
                r.menaceOrdrePublic(),
                r.condamnationPenale(),
                r.eligible(),
                r.verdict(),
                r.dureeManquante(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.delaiDeclaration(),
                r.baseJuridique()
        );
    }
}

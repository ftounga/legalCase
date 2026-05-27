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
 * SF-215-07 : service applicatif de l'outil naturalisation 12bis BE (Code de la
 * nationalité belge — loi 28/06/1984 consolidée art. 12bis).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class Naturalisation12bisBeService {

    private final Naturalisation12bisBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Naturalisation12bisBeService(
            Naturalisation12bisBeRepository repository,
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
    public Naturalisation12bisBeResponse calculate(UUID caseFileId,
                                                  Naturalisation12bisBeRequest request,
                                                  OidcUser oidcUser,
                                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Naturalisation 12bis BE uniquement — pour la France voir "
                            + "F-IM-13-naturalisation (Cciv art. 21-15 et seq.)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        Naturalisation12bisBeResult result;
        try {
            result = Naturalisation12bisBeCalculator.compute(
                    request.dureeSejour(),
                    request.typeSejour(),
                    request.niveauLangue(),
                    request.preuveIntegration(),
                    request.preuveEmploi(),
                    request.menaceOrdrePublic(),
                    request.condamnationPenale());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Naturalisation12bisBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Naturalisation12bisBeAnalysis a = new Naturalisation12bisBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDureeSejour(result.dureeSejour());
        entity.setTypeSejour(result.typeSejour());
        entity.setNiveauLangue(result.niveauLangue());
        entity.setPreuveIntegration(result.preuveIntegration());
        entity.setPreuveEmploi(result.preuveEmploi());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setCondamnationPenale(result.condamnationPenale());
        entity.setVoieEligible(result.voieEligible());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public Naturalisation12bisBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        Naturalisation12bisBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse naturalisation 12bis BE trouvée pour ce dossier"));
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

    private Naturalisation12bisBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, Naturalisation12bisBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Naturalisation12bisBeResponse toResponse(UUID caseFileId, Naturalisation12bisBeResult r) {
        return new Naturalisation12bisBeResponse(
                caseFileId,
                r.dureeSejour(),
                r.typeSejour(),
                r.niveauLangue(),
                r.preuveIntegration(),
                r.preuveEmploi(),
                r.menaceOrdrePublic(),
                r.condamnationPenale(),
                r.voie5Ans(),
                r.voie10Ans(),
                r.voieEligible(),
                r.dureeManquante(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.procedureReference(),
                r.baseJuridique()
        );
    }
}

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
 * SF-221-06 : service applicatif de l'outil titre de séjour victime de la traite des êtres
 * humains BE (art. 61/2 et s. Loi 15/12/1980 ; circulaire du 26/09/2008).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>{@code phaseProcedure} absente / hors whitelist → 400 ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class VictimeTraiteBeService {

    private final VictimeTraiteBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public VictimeTraiteBeService(
            VictimeTraiteBeRepository repository,
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
    public VictimeTraiteBeResponse calculate(UUID caseFileId,
                                             VictimeTraiteBeRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Titre victime de la traite : outil BELGIQUE uniquement — pour la France "
                            + "voir l'outil victime de la traite L. 425-1 CESEDA (F-IM-35).");
        }

        if (request == null || request.phaseProcedure() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "phaseProcedure est requise");
        }

        VictimeTraiteBeResult result;
        try {
            result = VictimeTraiteBeCalculator.compute(
                    request.phaseProcedure(),
                    request.ruptureAvecReseau(),
                    request.cooperationJudiciaire(),
                    request.accompagnementCentreSpecialise(),
                    request.dateDebutAccompagnement());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        VictimeTraiteBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    VictimeTraiteBeAnalysis a = new VictimeTraiteBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setPhaseProcedure(result.phaseProcedure());
        entity.setRuptureAvecReseau(result.ruptureAvecReseau());
        entity.setCooperationJudiciaire(result.cooperationJudiciaire());
        entity.setAccompagnementCentreSpecialise(result.accompagnementCentreSpecialise());
        entity.setDateDebutAccompagnement(result.dateDebutAccompagnement());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public VictimeTraiteBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        VictimeTraiteBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse victime de la traite BE trouvée pour ce dossier"));
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

    private VictimeTraiteBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, VictimeTraiteBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private VictimeTraiteBeResponse toResponse(UUID caseFileId, VictimeTraiteBeResult r) {
        return new VictimeTraiteBeResponse(
                caseFileId,
                r.phaseProcedure(),
                r.ruptureAvecReseau(),
                r.cooperationJudiciaire(),
                r.accompagnementCentreSpecialise(),
                r.dateDebutAccompagnement(),
                r.verdict(),
                r.etapeProcedure(),
                r.basesJuridiques() != null ? r.basesJuridiques() : java.util.List.of(),
                r.messages() != null ? r.messages() : java.util.List.of());
    }
}

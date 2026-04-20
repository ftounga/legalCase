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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

@Service
public class RuptureConvIndemniteService {

    private final RuptureConvIndemniteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RuptureConvIndemniteService(RuptureConvIndemniteRepository repository,
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
    public RuptureConvIndemniteResponse calculate(UUID caseFileId, RuptureConvIndemniteRequest request,
                                                  OidcUser oidcUser, Principal principal) {
        validate(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        RuptureConvIndemniteResult result;
        try {
            result = RuptureConvIndemniteCalculator.computeMinimumLegal(
                    request.ancienneteAnnees(), request.salaireMensuel());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RuptureConvIndemniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RuptureConvIndemniteAnalysis e = new RuptureConvIndemniteAnalysis();
                    e.setCaseFile(caseFile);
                    return e;
                });
        entity.setAncienneteAnnees(request.ancienneteAnnees());
        entity.setSalaireMensuel(request.salaireMensuel());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public RuptureConvIndemniteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RuptureConvIndemniteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'indemnité de rupture conventionnelle trouvée pour ce dossier"));
        RuptureConvIndemniteResult result = deserialize(entity.getResultData());
        return toResponse(caseFileId, result);
    }

    private void validate(RuptureConvIndemniteRequest r) {
        if (r.ancienneteAnnees() == null || r.ancienneteAnnees() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ancienneté requise et positive");
        if (r.salaireMensuel() == null || r.salaireMensuel().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire mensuel requis et positif");
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce dossier n'est pas un dossier de droit du travail");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation"); }
    }

    private RuptureConvIndemniteResult deserialize(String json) {
        try { return objectMapper.readValue(json, RuptureConvIndemniteResult.class); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation"); }
    }

    private RuptureConvIndemniteResponse toResponse(UUID caseFileId, RuptureConvIndemniteResult r) {
        return new RuptureConvIndemniteResponse(
                caseFileId,
                r.ancienneteAnnees(),
                r.salaireMensuel(),
                r.indemniteLegaleMinimum(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

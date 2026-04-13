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

@Service
public class IndemniteComparatifService {

    private final IndemniteComparatifRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IndemniteComparatifService(IndemniteComparatifRepository repository,
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
    public IndemniteComparatifResponse calculate(UUID caseFileId, IndemniteComparatifRequest request,
                                                  OidcUser oidcUser, Principal principal) {
        validate(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        IndemniteComparatifResult result;
        try {
            result = IndemniteComparatifCalculator.calculate(
                    request.country(), request.typeRupture(), request.ancienneteAnnees(),
                    request.age(), request.salaireMensuel());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndemniteComparatif entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> { IndemniteComparatif e = new IndemniteComparatif(); e.setCaseFile(caseFile); return e; });
        entity.setCountry(request.country());
        entity.setTypeRupture(request.typeRupture());
        entity.setAncienneteAnnees(request.ancienneteAnnees());
        entity.setAge(request.age());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public IndemniteComparatifResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        IndemniteComparatif entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun comparatif d'indemnités trouvé pour ce dossier"));
        IndemniteComparatifResult result = deserialize(entity.getResultData(), IndemniteComparatifResult.class);
        return toResponse(caseFileId, result);
    }

    private void validate(IndemniteComparatifRequest r) {
        if (!"FRANCE".equals(r.country()) && !"BELGIQUE".equals(r.country()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pays non supporté : " + r.country());
        if (r.salaireMensuel() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire mensuel requis");
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
    private <T> T deserialize(String json, Class<T> c) {
        try { return objectMapper.readValue(json, c); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation"); }
    }

    private IndemniteComparatifResponse toResponse(UUID caseFileId, IndemniteComparatifResult r) {
        return new IndemniteComparatifResponse(caseFileId, r.country(), r.typeRupture(),
                r.ancienneteAnnees(), r.age(), r.salaireMensuel(), r.displayMode(),
                r.baremePlancherMois(), r.baremePlafondMois(),
                r.fourchetteBasseMois(), r.fourchetteMedMois(), r.fourhetteHauteMois(),
                r.fourchetteBasseMontant(), r.fourchetteMedMontant(), r.fourhetteHauteMontant(),
                r.indemniteLegaleMontant(),
                r.baremeSource(), r.commentaire(),
                r.contextualMessages() != null ? r.contextualMessages() : java.util.List.of());
    }
}

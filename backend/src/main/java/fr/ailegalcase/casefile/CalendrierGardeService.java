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
public class CalendrierGardeService {

    private final CalendrierGardeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CalendrierGardeService(CalendrierGardeRepository repository, CaseFileRepository caseFileRepository,
                                   WorkspaceMemberRepository workspaceMemberRepository,
                                   CurrentUserResolver currentUserResolver, ObjectMapper objectMapper) {
        this.repository = repository; this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver; this.objectMapper = objectMapper;
    }

    @Transactional
    public CalendrierGardeResponse generate(UUID caseFileId, CalendrierGardeRequest request,
                                             OidcUser oidcUser, Principal principal) {
        if (!GardeModeReferentiel.isCodeValid(request.gardeCode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode de garde inconnu : " + request.gardeCode());
        User user = resolveUser(oidcUser, principal);
        CaseFile cf = resolveCaseFile(caseFileId, user);

        CalendrierGardeResult result = CalendrierGardeGenerator.generate(
                request.gardeCode(), request.parentANom(), request.parentBNom());

        CalendrierGarde entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> { CalendrierGarde e = new CalendrierGarde(); e.setCaseFile(cf); return e; });
        entity.setGardeCode(request.gardeCode());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public CalendrierGardeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CalendrierGarde entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun calendrier de garde trouvé"));
        CalendrierGardeResult result = deserialize(entity.getResultData(), CalendrierGardeResult.class);
        return toResponse(caseFileId, result);
    }

    private User resolveUser(OidcUser o, Principal p) {
        return currentUserResolver.resolve(o, OAuthProviderResolver.resolve(p), p);
    }

    private CaseFile resolveCaseFile(UUID id, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean m = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(mem -> mem.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!m) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce dossier n'est pas un dossier de droit de la famille");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur"); }
    }
    private <T> T deserialize(String json, Class<T> c) {
        try { return objectMapper.readValue(json, c); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur"); }
    }

    private CalendrierGardeResponse toResponse(UUID id, CalendrierGardeResult r) {
        return new CalendrierGardeResponse(id, r.gardeCode(), r.gardeLabel(), r.country(),
                r.parentANom(), r.parentBNom(), r.repartitionType(),
                r.semaineTypeParentA(), r.semaineTypeParentB(), r.vacancesRegle(),
                r.joursParAnParentA(), r.joursParAnParentB(), r.baseJuridique(), r.commentaire());
    }
}

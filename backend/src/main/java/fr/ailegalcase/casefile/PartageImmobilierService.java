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
public class PartageImmobilierService {

    private final PartageImmobilierRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PartageImmobilierService(PartageImmobilierRepository repository,
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
    public PartageImmobilierResponse calculate(UUID caseFileId, PartageImmobilierRequest request,
                                                OidcUser oidcUser, Principal principal) {
        validate(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        PartageImmobilierResult result = PartageImmobilierCalculator.calculate(
                request.country(), request.valeurVenale(), request.capitalRestantDu(),
                request.quotePartAttributaire(), request.isDivorce());

        PartageImmobilier entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> { PartageImmobilier e = new PartageImmobilier(); e.setCaseFile(caseFile); return e; });
        entity.setCountry(request.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public PartageImmobilierResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        PartageImmobilier entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune simulation de partage trouvée pour ce dossier"));
        PartageImmobilierResult result = deserialize(entity.getResultData(), PartageImmobilierResult.class);
        return toResponse(caseFileId, result);
    }

    private void validate(PartageImmobilierRequest r) {
        if (!"FRANCE".equals(r.country()) && !"BELGIQUE".equals(r.country()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pays non supporté : " + r.country());
        if (r.valeurVenale() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valeur vénale requise");
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce dossier n'est pas un dossier de droit de la famille");
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

    private PartageImmobilierResponse toResponse(UUID caseFileId, PartageImmobilierResult r) {
        return new PartageImmobilierResponse(caseFileId, r.country(), r.valeurVenale(), r.capitalRestantDu(),
                r.valeurNette(), r.quotePartAttributaire(), r.quotePartCedant(), r.partAttributaire(),
                r.partCedant(), r.soulte(), r.droitPartage(), r.tauxDroitPartage(),
                r.fraisNotaireEstimes(), r.coutTotal(), r.baseJuridique(), r.commentaire());
    }
}

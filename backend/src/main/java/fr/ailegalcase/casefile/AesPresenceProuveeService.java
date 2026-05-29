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
 * SF-214-11 : service du calcul de présence prouvée en France et de l'éligibilité
 * aux 4 voies AES (L. 435-1 / L. 435-3 CESEDA). Outil single-country FR.
 */
@Service
public class AesPresenceProuveeService {

    private final AesPresenceProuveeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AesPresenceProuveeService(AesPresenceProuveeRepository repository,
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
    public AesPresenceProuveeResponse analyze(UUID caseFileId, AesPresenceProuveeRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AES présence prouvée — outil FRANCE uniquement");
        }

        if (request == null || request.periodesPresentees() == null
                || request.periodesPresentees().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "periodesPresentees est requis et ne peut pas être vide");
        }

        AesPresenceProuveeResult result;
        try {
            result = AesPresenceProuveeCalculator.analyze(request.periodesPresentees(), LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AesPresenceProuveeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AesPresenceProuveeAnalysis a = new AesPresenceProuveeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setAnneesTotalesProuvees(result.anneesTotalesProuvees());
        entity.setMoisTotauxProuves(result.moisTotauxProuves());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AesPresenceProuveeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AesPresenceProuveeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de présence prouvée AES trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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

    private AesPresenceProuveeResult deserialize(String json) {
        try { return objectMapper.readValue(json, AesPresenceProuveeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AesPresenceProuveeResponse toResponse(UUID caseFileId, String country,
                                                  AesPresenceProuveeResult r) {
        return new AesPresenceProuveeResponse(
                caseFileId,
                country,
                r.periodesNormalisees(),
                r.periodesFusionnees(),
                r.moisTotauxProuves(),
                r.anneesTotalesProuvees(),
                r.eligibiliteParVoie(),
                r.gapsPeriodes(),
                r.recommandationsPieces(),
                r.baseJuridique());
    }
}

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
 * SF-214-15 : service de l'analyse récépissé vs attestation de prolongation
 * R. 311-4 / R. 311-6 CESEDA. Outil single-country FR.
 */
@Service
public class RecepisseAttestationService {

    private final RecepisseAttestationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RecepisseAttestationService(RecepisseAttestationRepository repository,
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
    public RecepisseAttestationResponse analyze(UUID caseFileId, RecepisseAttestationRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Récépissé vs attestation R.311-4/R.311-6 — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.typeDocument() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ typeDocument est requis (RECEPISSE | ATTESTATION_PROLONGATION | INCONNU)");
        }

        RecepisseAttestationResult result;
        try {
            result = RecepisseAttestationAnalyzer.analyze(
                    request.typeDocument(),
                    request.dateDelivrance(),
                    request.dateExpiration(),
                    request.mentionAutorisationTravail());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RecepisseAttestationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RecepisseAttestationAnalysis a = new RecepisseAttestationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeDocument(result.typeDocument());
        entity.setDateDelivrance(request.dateDelivrance());
        entity.setDateExpiration(request.dateExpiration());
        entity.setMentionAutorisationTravail(request.mentionAutorisationTravail());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RecepisseAttestationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RecepisseAttestationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse récépissé/attestation trouvée pour ce dossier"));
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

    private RecepisseAttestationResult deserialize(String json) {
        try { return objectMapper.readValue(json, RecepisseAttestationResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RecepisseAttestationResponse toResponse(UUID caseFileId, String country,
                                                    RecepisseAttestationResult r) {
        return new RecepisseAttestationResponse(
                caseFileId,
                r.typeDocument(),
                r.dateDelivrance(),
                r.dateExpiration(),
                r.mentionAutorisationTravail(),
                country,
                r.droitSejour(),
                r.droitTravail(),
                r.dureeValiditeJours(),
                r.risqueEmployeur(),
                r.recommandations(),
                r.baseJuridique());
    }
}

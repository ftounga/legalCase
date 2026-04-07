package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.referential.LegalReferentialService;
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
public class ImmigrationWorkRightService {

    private final ImmigrationWorkRightRepository workRightRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    private final LegalReferentialService referentialService;

    public ImmigrationWorkRightService(ImmigrationWorkRightRepository workRightRepository,
                                        CaseFileRepository caseFileRepository,
                                        WorkspaceMemberRepository workspaceMemberRepository,
                                        CurrentUserResolver currentUserResolver,
                                        ObjectMapper objectMapper,
                                        LegalReferentialService referentialService) {
        this.workRightRepository = workRightRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.referentialService = referentialService;
    }

    @Transactional
    public ImmigrationWorkRightResponse resolve(UUID caseFileId,
                                                 ImmigrationWorkRightRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        validateRequest(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        WorkRightResult result = referentialService.getWorkRight(request.titreType(), request.country());
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Titre inconnu pour ce pays : " + request.titreType() + " / " + request.country());
        }

        ImmigrationWorkRight entity = workRightRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ImmigrationWorkRight wr = new ImmigrationWorkRight();
                    wr.setCaseFile(caseFile);
                    return wr;
                });

        entity.setTitreType(request.titreType());
        entity.setCountry(request.country());
        entity.setResultData(serialize(result));

        workRightRepository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public ImmigrationWorkRightResponse get(UUID caseFileId,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);

        ImmigrationWorkRight entity = workRightRepository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse droit au travail trouvée pour ce dossier"));

        WorkRightResult result = deserialize(entity.getResultData(), WorkRightResult.class);
        return toResponse(caseFileId, result);
    }

    private void validateRequest(ImmigrationWorkRightRequest request) {
        if (request.titreType() == null || request.titreType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de titre requis");
        }
        if (!"FRANCE".equals(request.country()) && !"BELGIQUE".equals(request.country())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pays non supporté : " + request.country() + ". Valeurs : FRANCE, BELGIQUE");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(caseFile.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return caseFile;
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ImmigrationWorkRightResponse toResponse(UUID caseFileId, WorkRightResult result) {
        return new ImmigrationWorkRightResponse(
                caseFileId,
                result.titreType(),
                result.titreLabel(),
                result.country(),
                result.droitTravail(),
                result.conditions(),
                result.obligationsEmployeur(),
                result.baseJuridique()
        );
    }
}

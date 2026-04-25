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
import java.util.List;
import java.util.UUID;

/**
 * SF-IM-19-01 : service d'analyse d'éligibilité mineur étranger
 * (MNA, L.435-3, DCEM, TIR). Gates : DROIT_IMMIGRATION + FRANCE.
 */
@Service
public class MineursImmigrationService {

    private final MineursImmigrationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MineursImmigrationService(MineursImmigrationRepository repository,
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
    public MineursImmigrationResponse calculate(UUID caseFileId,
                                                MineursImmigrationRequest request,
                                                OidcUser oidcUser,
                                                Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime mineurs étrangers propre à la France (CESEDA / Cciv / CASF)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        boolean parentRegulier = Boolean.TRUE.equals(request.parentRegulier());
        boolean isolement = Boolean.TRUE.equals(request.isolementAvere());
        boolean ordrePublic = Boolean.TRUE.equals(request.motifOrdrePublic());

        MineursImmigrationResult result;
        try {
            result = MineursImmigrationCalculator.compute(
                    request.dispositifVise(),
                    request.dateNaissance(),
                    request.dateEntreeFrance(),
                    parentRegulier,
                    isolement,
                    ordrePublic,
                    request.nationalite());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MineursImmigrationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MineursImmigrationAnalysis a = new MineursImmigrationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDispositifVise(result.dispositifVise());
        entity.setDateNaissance(result.dateNaissance());
        entity.setDateEntreeFrance(result.dateEntreeFrance());
        entity.setParentRegulier(result.parentRegulier());
        entity.setIsolementAvere(result.isolementAvere());
        entity.setMotifOrdrePublic(result.motifOrdrePublic());
        entity.setNationalite(result.nationalite());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public MineursImmigrationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        MineursImmigrationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse mineurs immigration trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private MineursImmigrationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, MineursImmigrationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private MineursImmigrationResponse toResponse(UUID caseFileId, String country,
                                                  MineursImmigrationResult r) {
        return new MineursImmigrationResponse(
                caseFileId,
                country,
                r.dispositifVise(),
                r.dispositifRecommande(),
                r.dateNaissance(),
                r.dateEntreeFrance(),
                r.parentRegulier(),
                r.isolementAvere(),
                r.motifOrdrePublic(),
                r.nationalite(),
                r.ageAnnees(),
                r.verdictEligibilite(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : List.of(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.delaiInstructionMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}

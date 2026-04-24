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
public class AesFamilleService {

    private final AesFamilleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AesFamilleService(AesFamilleRepository repository,
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
    public AesFamilleResponse calculate(UUID caseFileId,
                                        AesFamilleRequest request,
                                        OidcUser oidcUser,
                                        Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime AES voie familiale propre à la France (L.435-1 CESEDA)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        if (request.dureePresenceMois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureePresenceMois est requis");
        }
        if (request.enfantsScolarisesFrance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "enfantsScolarisesFrance est requis");
        }
        if (request.dureeScolaritePlusAncienEnfantAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureeScolaritePlusAncienEnfantAnnees est requis");
        }

        boolean conjointFrancaisOuRegulier = Boolean.TRUE.equals(request.conjointFrancaisOuRegulier());
        boolean preuvesInsertion = Boolean.TRUE.equals(request.preuvesInsertion());
        boolean menaceOrdrePublic = Boolean.TRUE.equals(request.menaceOrdrePublic());

        AesFamilleResult result;
        try {
            result = AesFamilleCalculator.compute(
                    request.dateEntreeFrance(),
                    request.dureePresenceMois(),
                    conjointFrancaisOuRegulier,
                    request.enfantsScolarisesFrance(),
                    request.dureeScolaritePlusAncienEnfantAnnees(),
                    preuvesInsertion,
                    menaceOrdrePublic,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AesFamilleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AesFamilleAnalysis a = new AesFamilleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntreeFrance(result.dateEntreeFrance());
        entity.setDureePresenceMois(result.dureePresenceMois());
        entity.setConjointFrancaisOuRegulier(result.conjointFrancaisOuRegulier());
        entity.setEnfantsScolarisesFrance(result.enfantsScolarisesFrance());
        entity.setDureeScolaritePlusAncienEnfantAnnees(result.dureeScolaritePlusAncienEnfantAnnees());
        entity.setPreuvesInsertion(result.preuvesInsertion());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AesFamilleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AesFamilleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AES voie familiale trouvée pour ce dossier"));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private AesFamilleResult deserialize(String json) {
        try { return objectMapper.readValue(json, AesFamilleResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AesFamilleResponse toResponse(UUID caseFileId, String country,
                                          AesFamilleResult r) {
        return new AesFamilleResponse(
                caseFileId,
                r.dateEntreeFrance(),
                r.dureePresenceMois(),
                r.conjointFrancaisOuRegulier(),
                r.enfantsScolarisesFrance(),
                r.dureeScolaritePlusAncienEnfantAnnees(),
                r.preuvesInsertion(),
                r.menaceOrdrePublic(),
                r.dateDepotDemande(),
                country,
                r.presence5AnsOk(),
                r.presence10AnsOk(),
                r.liensFamiliauxOk(),
                r.insertionOk(),
                r.pasMenace(),
                r.scoreGlobal(),
                r.verdictProbabiliteAcceptation(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstructionSiDemande(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

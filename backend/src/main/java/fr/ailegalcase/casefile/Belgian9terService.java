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
public class Belgian9terService {

    private final Belgian9terRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Belgian9terService(Belgian9terRepository repository,
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
    public Belgian9terResponse calculate(UUID caseFileId,
                                         Belgian9terRequest request,
                                         OidcUser oidcUser,
                                         Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "9ter procédure BE uniquement (art. 9ter Loi 15/12/1980) — "
                            + "en France voir étranger malade L.425-9 CESEDA");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        boolean maladieGraveCertifiee = Boolean.TRUE.equals(request.maladieGraveCertifiee());
        boolean soinsNecessairesDisponiblesBe = Boolean.TRUE.equals(request.soinsNecessairesDisponiblesBe());
        boolean soinsInaccessiblesPaysOrigine = Boolean.TRUE.equals(request.soinsInaccessiblesPaysOrigine());
        boolean menaceOrdrePublic = Boolean.TRUE.equals(request.menaceOrdrePublic());

        Belgian9terResult result;
        try {
            result = Belgian9terCalculator.compute(
                    request.dateDebutSymptomes(),
                    maladieGraveCertifiee,
                    soinsNecessairesDisponiblesBe,
                    soinsInaccessiblesPaysOrigine,
                    menaceOrdrePublic,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Belgian9terAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Belgian9terAnalysis a = new Belgian9terAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDebutSymptomes(result.dateDebutSymptomes());
        entity.setMaladieGraveCertifiee(result.maladieGraveCertifiee());
        entity.setSoinsNecessairesDisponiblesBe(result.soinsNecessairesDisponiblesBe());
        entity.setSoinsInaccessiblesPaysOrigine(result.soinsInaccessiblesPaysOrigine());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public Belgian9terResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        Belgian9terAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse 9ter médical BE trouvée pour ce dossier"));
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

    private Belgian9terResult deserialize(String json) {
        try { return objectMapper.readValue(json, Belgian9terResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Belgian9terResponse toResponse(UUID caseFileId, String country,
                                           Belgian9terResult r) {
        return new Belgian9terResponse(
                caseFileId,
                r.dateDebutSymptomes(),
                r.maladieGraveCertifiee(),
                r.soinsNecessairesDisponiblesBe(),
                r.soinsInaccessiblesPaysOrigine(),
                r.menaceOrdrePublic(),
                r.dateDepotDemande(),
                country,
                r.certificatMedicalType1Ok(),
                r.soinsRequisOk(),
                r.inaccessibiliteOk(),
                r.pasMenace(),
                r.scoreGlobal(),
                r.verdictProbabiliteAcceptation(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstruction(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

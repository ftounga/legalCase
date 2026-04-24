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
public class Belgian9bisService {

    private final Belgian9bisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Belgian9bisService(Belgian9bisRepository repository,
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
    public Belgian9bisResponse calculate(UUID caseFileId,
                                         Belgian9bisRequest request,
                                         OidcUser oidcUser,
                                         Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régularisation 9bis : procédure propre à la Belgique (art. 9bis Loi 15/12/1980)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        if (request.dureePresenceMois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureePresenceMois est requis");
        }

        boolean circonstancesExceptionnelles = Boolean.TRUE.equals(request.circonstancesExceptionnelles());
        boolean liensFamiliauxBe = Boolean.TRUE.equals(request.liensFamiliauxBe());
        boolean liensProfessionnels = Boolean.TRUE.equals(request.liensProfessionnels());
        boolean scolariteEnfantsBe = Boolean.TRUE.equals(request.scolariteEnfantsBe());
        boolean menaceOrdrePublic = Boolean.TRUE.equals(request.menaceOrdrePublic());

        Belgian9bisResult result;
        try {
            result = Belgian9bisCalculator.compute(
                    request.dateEntreeBelgique(),
                    request.dureePresenceMois(),
                    circonstancesExceptionnelles,
                    liensFamiliauxBe,
                    liensProfessionnels,
                    scolariteEnfantsBe,
                    menaceOrdrePublic,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Belgian9bisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Belgian9bisAnalysis a = new Belgian9bisAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntreeBelgique(result.dateEntreeBelgique());
        entity.setDureePresenceMois(result.dureePresenceMois());
        entity.setCirconstancesExceptionnelles(result.circonstancesExceptionnelles());
        entity.setLiensFamiliauxBe(result.liensFamiliauxBe());
        entity.setLiensProfessionnels(result.liensProfessionnels());
        entity.setScolariteEnfantsBe(result.scolariteEnfantsBe());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public Belgian9bisResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        Belgian9bisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse 9bis humanitaire BE trouvée pour ce dossier"));
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

    private Belgian9bisResult deserialize(String json) {
        try { return objectMapper.readValue(json, Belgian9bisResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Belgian9bisResponse toResponse(UUID caseFileId, String country,
                                           Belgian9bisResult r) {
        return new Belgian9bisResponse(
                caseFileId,
                r.dateEntreeBelgique(),
                r.dureePresenceMois(),
                r.circonstancesExceptionnelles(),
                r.liensFamiliauxBe(),
                r.liensProfessionnels(),
                r.scolariteEnfantsBe(),
                r.menaceOrdrePublic(),
                r.dateDepotDemande(),
                country,
                r.presence3AnsOk(),
                r.liensConstitutifsOk(),
                r.pasMenace(),
                r.scoreGlobal(),
                r.verdictProbabilite(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstructionPrevisionnelle(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

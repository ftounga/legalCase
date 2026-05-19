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

/**
 * SF-207-01 : service orchestrant l'analyse de prescription des actions
 * Travail BE — gate <b>BELGIQUE strict</b> (404 côté FR pour préserver
 * l'isolation BE-only) + isolation workspace standard.
 *
 * <p>Pattern mirroré de {@code ContestationAreService} (F-DT-35), adapté à
 * la substance juridique BE (Loi 03/07/1978 art. 15 ; CCT 109 art. 11).</p>
 *
 * <p>Note isolation : contrairement à F-DT-35 qui répond 400 quand le pays
 * ne correspond pas (l'outil est visible FR uniquement, mais l'erreur 400
 * peut être renvoyée), cet outil renvoie <b>404</b> côté FR car la
 * mini-spec impose la non-divulgation de l'existence de l'outil — la
 * réponse 404 est indistinguable d'un dossier inconnu.</p>
 */
@Service
public class PrescriptionBeLitigeTravailService {

    private final PrescriptionBeLitigeTravailRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PrescriptionBeLitigeTravailService(PrescriptionBeLitigeTravailRepository repository,
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
    public PrescriptionBeLitigeTravailResponse calculate(UUID caseFileId,
                                                         PrescriptionBeLitigeTravailRequest request,
                                                         OidcUser oidcUser,
                                                         Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer l'existence
        // de l'outil côté FR — réponse indistinguable d'un dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        PrescriptionBeLitigeTravailResult result;
        try {
            result = PrescriptionBeLitigeTravailCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PrescriptionBeLitigeTravailResponse response = toResponse(caseFileId, country, result);

        PrescriptionBeLitigeTravailAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PrescriptionBeLitigeTravailAnalysis a = new PrescriptionBeLitigeTravailAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public PrescriptionBeLitigeTravailResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        PrescriptionBeLitigeTravailAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de prescription Travail BE trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private PrescriptionBeLitigeTravailResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, PrescriptionBeLitigeTravailResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PrescriptionBeLitigeTravailResponse toResponse(UUID caseFileId, String country,
                                                           PrescriptionBeLitigeTravailResult r) {
        return new PrescriptionBeLitigeTravailResponse(
                caseFileId,
                r.dateRupture(),
                r.typeCreance(),
                r.dateActionEnvisagee(),
                r.verdict(),
                r.dateLimitePrescription(),
                r.joursRestants(),
                r.regleAppliquee(),
                r.baseJuridique(),
                r.formuleCalcul(),
                country);
    }
}

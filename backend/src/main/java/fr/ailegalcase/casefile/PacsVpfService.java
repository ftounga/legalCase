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
 * SF-220-04 : service de l'outil décisionnel « VPF au titre d'un PACS L.423-23 »
 * (F-IM-50-pacs-vpf-fr). Outil single-country FR.
 *
 * <p>Pattern miroir de {@link VpfJeuneMajeurService} (SF-220-03).</p>
 */
@Service
public class PacsVpfService {

    private final PacsVpfRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PacsVpfService(PacsVpfRepository repository,
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
    public PacsVpfResponse analyze(UUID caseFileId, PacsVpfRequest request,
                                   OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "VPF au titre d'un PACS — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.intensiteCommunauteVie() != null
                && !PacsVpfAnalyzer.INTENSITE_VALEURS.contains(request.intensiteCommunauteVie())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ intensiteCommunauteVie doit être l'une des valeurs "
                            + PacsVpfAnalyzer.INTENSITE_VALEURS);
        }
        if (request.partenaireStatut() != null
                && !PacsVpfAnalyzer.PARTENAIRE_STATUTS.contains(request.partenaireStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ partenaireStatut doit être l'une des valeurs "
                            + PacsVpfAnalyzer.PARTENAIRE_STATUTS);
        }
        if (request.dureeVieCommuneMois() != null && request.dureeVieCommuneMois() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ dureeVieCommuneMois doit être positif ou nul");
        }

        boolean pacsConclu = Boolean.TRUE.equals(request.pacsConclu());
        String partenaireStatut = request.partenaireStatut() != null
                ? request.partenaireStatut() : PacsVpfAnalyzer.PARTENAIRE_AUTRE;
        String intensite = request.intensiteCommunauteVie() != null
                ? request.intensiteCommunauteVie() : PacsVpfAnalyzer.INTENSITE_NON_ETABLIE;
        boolean autresLiens = Boolean.TRUE.equals(request.autresLiensPrivesFamiliaux());

        PacsVpfResult result = PacsVpfAnalyzer.analyze(
                pacsConclu,
                partenaireStatut,
                request.dureeVieCommuneMois(),
                intensite,
                autresLiens);

        PacsVpfAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PacsVpfAnalysis a = new PacsVpfAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setPacsConclu(result.pacsConclu());
        entity.setDatePacs(request.datePacs());
        entity.setPartenaireStatut(result.partenaireStatut());
        entity.setDureeVieCommuneMois(result.dureeVieCommuneMois());
        entity.setIntensiteCommunauteVie(result.intensiteCommunauteVie());
        entity.setAutresLiensPrivesFamiliaux(result.autresLiensPrivesFamiliaux());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, request.datePacs(), result);
    }

    @Transactional(readOnly = true)
    public PacsVpfResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PacsVpfAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse VPF PACS trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, entity.getDatePacs(), deserialize(entity.getResultData()));
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

    private PacsVpfResult deserialize(String json) {
        try { return objectMapper.readValue(json, PacsVpfResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PacsVpfResponse toResponse(UUID caseFileId, String country,
                                       LocalDate datePacs, PacsVpfResult r) {
        return new PacsVpfResponse(
                caseFileId,
                r.pacsConclu(),
                datePacs,
                r.partenaireStatut(),
                r.dureeVieCommuneMois(),
                r.intensiteCommunauteVie(),
                r.autresLiensPrivesFamiliaux(),
                country,
                r.eligibilite(),
                r.elementsFavorables(),
                r.elementsManquants(),
                r.basesJuridiques(),
                r.messages());
    }
}

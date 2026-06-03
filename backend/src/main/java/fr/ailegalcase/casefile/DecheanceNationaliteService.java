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
 * SF-220-05 : service de l'outil décisionnel « validité d'une mesure de
 * déchéance de nationalité » (Cciv 25 / 25-1,
 * F-IM-51-decheance-nationalite-fr). Outil single-country FR.
 *
 * <p>Pattern miroir de {@link PacsVpfService} (SF-220-04).</p>
 */
@Service
public class DecheanceNationaliteService {

    private final DecheanceNationaliteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DecheanceNationaliteService(DecheanceNationaliteRepository repository,
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
    public DecheanceNationaliteResponse analyze(UUID caseFileId, DecheanceNationaliteRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Déchéance de nationalité — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.motif() != null
                && !DecheanceNationaliteAnalyzer.MOTIF_VALEURS.contains(request.motif())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ motif doit être l'une des valeurs "
                            + DecheanceNationaliteAnalyzer.MOTIF_VALEURS);
        }

        boolean mesurePrononcee = Boolean.TRUE.equals(request.mesurePrononcee());

        DecheanceNationaliteResult result = DecheanceNationaliteAnalyzer.analyze(
                request.motif(),
                request.binational(),
                request.dateAcquisitionNationalite(),
                request.dateFaits(),
                mesurePrononcee,
                request.dateDecret());

        DecheanceNationaliteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DecheanceNationaliteAnalysis a = new DecheanceNationaliteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMotif(request.motif());
        entity.setBinational(request.binational());
        entity.setDateAcquisitionNationalite(request.dateAcquisitionNationalite());
        entity.setDateFaits(request.dateFaits());
        entity.setMesurePrononcee(mesurePrononcee);
        entity.setDateDecret(request.dateDecret());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, request, result);
    }

    @Transactional(readOnly = true)
    public DecheanceNationaliteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DecheanceNationaliteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de déchéance de nationalité trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        DecheanceNationaliteResult r = deserialize(entity.getResultData());
        return new DecheanceNationaliteResponse(
                caseFileId,
                entity.getMotif(),
                entity.getBinational(),
                entity.getDateAcquisitionNationalite(),
                entity.getDateFaits(),
                entity.isMesurePrononcee(),
                entity.getDateDecret(),
                country,
                r.validite(),
                r.conditionsManquantes(),
                r.voiesRecours(),
                r.delaiRecoursJours(),
                r.basesJuridiques(),
                r.messages());
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

    private DecheanceNationaliteResult deserialize(String json) {
        try { return objectMapper.readValue(json, DecheanceNationaliteResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DecheanceNationaliteResponse toResponse(UUID caseFileId, String country,
                                                    DecheanceNationaliteRequest request,
                                                    DecheanceNationaliteResult r) {
        return new DecheanceNationaliteResponse(
                caseFileId,
                r.motif(),
                r.binational(),
                request.dateAcquisitionNationalite(),
                request.dateFaits(),
                r.mesurePrononcee(),
                request.dateDecret(),
                country,
                r.validite(),
                r.conditionsManquantes(),
                r.voiesRecours(),
                r.delaiRecoursJours(),
                r.basesJuridiques(),
                r.messages());
    }
}

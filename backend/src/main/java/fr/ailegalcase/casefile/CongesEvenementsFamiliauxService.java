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
 * SF-218-43 : service applicatif de l'outil "Congés pour évènements familiaux"
 * (art. L.3142-1 à L.3142-5 CT, F-DT-76) — détermine la durée de congé
 * applicable (la plus favorable entre loi et CCN), la base de calcul retenue et
 * le maintien intégral du salaire. Outil <b>FRANCE UNIQUEMENT</b>, distinct du
 * congé de paternité/maternité (F-212) et du congé parental d'éducation
 * (F-DT-78).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents et valides (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CongesEvenementsFamiliauxService {

    private final CongesEvenementsFamiliauxRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CongesEvenementsFamiliauxService(CongesEvenementsFamiliauxRepository repository,
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
    public CongesEvenementsFamiliauxResponse analyze(UUID caseFileId,
                                                     CongesEvenementsFamiliauxRequest request,
                                                     OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Congés pour évènements familiaux — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CongesEvenementsFamiliauxResult result;
        try {
            result = CongesEvenementsFamiliauxAnalyzer.analyze(
                    request.typeEvenement(),
                    request.conventionPlusFavorable(),
                    request.dureeConventionnelleJours());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CongesEvenementsFamiliauxAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CongesEvenementsFamiliauxAnalysis a = new CongesEvenementsFamiliauxAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeEvenement(result.typeEvenement());
        entity.setConventionPlusFavorable(result.conventionPlusFavorable());
        entity.setDureeConventionnelleJours(result.dureeConventionnelleJours());
        entity.setDureeLegaleJours(result.dureeLegaleJours());
        entity.setDureeApplicableJours(result.dureeApplicableJours());
        entity.setBase(result.base());
        entity.setMaintienSalaire(result.maintienSalaire());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public CongesEvenementsFamiliauxResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        CongesEvenementsFamiliauxAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de congé pour évènement familial trouvée pour ce dossier"));
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

    private CongesEvenementsFamiliauxResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CongesEvenementsFamiliauxResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CongesEvenementsFamiliauxResponse toResponse(UUID caseFileId, String country,
                                                         CongesEvenementsFamiliauxResult r) {
        return new CongesEvenementsFamiliauxResponse(
                caseFileId,
                r.typeEvenement(),
                r.conventionPlusFavorable(),
                r.dureeConventionnelleJours(),
                r.dureeLegaleJours(),
                r.dureeApplicableJours(),
                r.base(),
                r.maintienSalaire(),
                r.assimileTempsTravailEffectif(),
                r.dureeMajoreePossible(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}

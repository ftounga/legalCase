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

/**
 * SF-DT-25-01 : service d'orchestration du calcul d'indemnité compensatrice de préavis FR.
 * Résout le contexte (case file, workspace FR, domaine travail), interroge le référentiel
 * CCN ({@link LegalReferentialService#getConventionPreavis(String, IndemnitePreavisFonction, int)})
 * puis délègue le calcul à {@link IndemnitePreavisCalculator}.
 */
@Service
public class IndemnitePreavisService {

    private final IndemnitePreavisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final LegalReferentialService legalReferentialService;
    private final ObjectMapper objectMapper;

    public IndemnitePreavisService(IndemnitePreavisRepository repository,
                                   CaseFileRepository caseFileRepository,
                                   WorkspaceMemberRepository workspaceMemberRepository,
                                   CurrentUserResolver currentUserResolver,
                                   LegalReferentialService legalReferentialService,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.legalReferentialService = legalReferentialService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IndemnitePreavisResponse calculate(UUID caseFileId,
                                              IndemnitePreavisRequest request,
                                              OidcUser oidcUser,
                                              Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        validate(request);

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        Integer ccnDureeMois = null;
        String ccnArticle = null;
        String ccnNormalized = null;
        if (request.conventionCollectiveCode() != null && !request.conventionCollectiveCode().isBlank()) {
            LegalReferentialService.ConventionPreavis preavis = legalReferentialService.getConventionPreavis(
                    request.conventionCollectiveCode(), request.fonction(), request.ancienneteMois());
            if (preavis != null) {
                ccnDureeMois = preavis.dureeMois();
                ccnArticle = preavis.article();
                ccnNormalized = preavis.code();
            } else {
                ccnNormalized = ConventionCodeNormalizer.normalize(request.conventionCollectiveCode());
            }
        }

        IndemnitePreavisResult result;
        try {
            result = IndemnitePreavisCalculator.compute(
                    request.ancienneteMois(),
                    request.fonction(),
                    ccnNormalized,
                    ccnDureeMois,
                    ccnArticle,
                    request.salaireMensuelBrutEur(),
                    Boolean.TRUE.equals(request.exemptionEmployeur()),
                    request.dateRupture());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndemnitePreavisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IndemnitePreavisAnalysis a = new IndemnitePreavisAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setAncienneteMois(result.ancienneteMois());
        entity.setFonction(result.fonction());
        entity.setConventionCollectiveCode(result.conventionCollectiveCode());
        entity.setSalaireMensuelBrutEur(result.salaireMensuelBrutEur());
        entity.setExemptionEmployeur(result.exemptionEmployeur());
        entity.setDateRupture(result.dateRupture());
        entity.setResultData(serialize(result));
        repository.save(entity);

        int ancienneteAnnees = request.ancienneteAnnees() != null
                ? request.ancienneteAnnees()
                : result.ancienneteMois() / 12;

        return toResponse(caseFileId, ancienneteAnnees, result);
    }

    @Transactional(readOnly = true)
    public IndemnitePreavisResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        IndemnitePreavisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'indemnité de préavis trouvée pour ce dossier"));
        IndemnitePreavisResult result = deserialize(entity.getResultData());
        int ancienneteAnnees = result.ancienneteMois() / 12;
        return toResponse(caseFileId, ancienneteAnnees, result);
    }

    private void validate(IndemnitePreavisRequest r) {
        if (r.ancienneteMois() == null || r.ancienneteMois() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ancienneté en mois requise et positive");
        if (r.salaireMensuelBrutEur() == null || r.salaireMensuelBrutEur().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire mensuel brut requis et strictement positif");
        if (r.fonction() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fonction du salarié requise (OUVRIER, EMPLOYE, AGENT_MAITRISE, CADRE)");
        if (r.exemptionEmployeur() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le champ exemptionEmployeur est requis (booléen)");
        if (r.dateRupture() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date de rupture requise");
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        String country = cf.getWorkspace() != null ? cf.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil disponible uniquement pour les dossiers FRANCE — pour la Belgique, "
                            + "utiliser la formule Claeys / CCT 109 (feature jumelle BE à venir)");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private IndemnitePreavisResult deserialize(String json) {
        try { return objectMapper.readValue(json, IndemnitePreavisResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private IndemnitePreavisResponse toResponse(UUID caseFileId, int ancienneteAnnees, IndemnitePreavisResult r) {
        return new IndemnitePreavisResponse(
                caseFileId,
                ancienneteAnnees,
                r.ancienneteMois(),
                r.salaireMensuelBrutEur(),
                r.conventionCollectiveCode(),
                r.fonction(),
                r.exemptionEmployeur(),
                r.dateRupture(),
                r.dureePreavisMois(),
                r.sourceDuree(),
                r.montantIndemniteEur(),
                r.exemptionRetenue(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                "FRANCE"
        );
    }
}

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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

/**
 * SF-DT-20-01 : service applicatif de l'outil décisionnel "Rappel de salaire FR"
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN).
 *
 * <p>Gates : workspace membership, dossier {@code DROIT_DU_TRAVAIL}, country
 * {@code FRANCE} (équivalent BE non couvert — feature jumelle à ouvrir au backlog).</p>
 */
@Service
public class RappelSalaireService {

    private final RappelSalaireRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final LegalReferentialService legalReferentialService;
    private final ObjectMapper objectMapper;

    public RappelSalaireService(RappelSalaireRepository repository,
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
    public RappelSalaireResponse calculate(UUID caseFileId,
                                           RappelSalaireRequest request,
                                           OidcUser oidcUser,
                                           Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        validate(request);

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        // Résolution de la prime d'ancienneté CCN via le référentiel.
        String ccnNormalized = null;
        BigDecimal primePctCcn = null;
        if (request.conventionCollectiveCode() != null && !request.conventionCollectiveCode().isBlank()) {
            ccnNormalized = ConventionCodeNormalizer.normalize(request.conventionCollectiveCode());
            ConventionBareme bareme = legalReferentialService.getConventionBareme(request.conventionCollectiveCode());
            if (bareme != null && bareme.primesAnciennete() != null) {
                int anciennete = request.ancienneteAnneesPrime() != null ? request.ancienneteAnneesPrime() : 0;
                primePctCcn = bareme.primesAnciennete().stream()
                        .filter(p -> anciennete >= p.ancienneteMinAnnees())
                        .map(ConventionBareme.PrimeAnciennete::pourcentage)
                        .max(BigDecimal::compareTo)
                        .orElse(null);
            }
        }

        RappelSalaireResult result;
        try {
            result = RappelSalaireCalculator.compute(
                    request.periodeDebut(),
                    request.periodeFin(),
                    request.montantSalaireDuMensuelEur(),
                    request.montantSalairePerVerseMensuelEur(),
                    ccnNormalized != null ? ccnNormalized : request.conventionCollectiveCode(),
                    request.ancienneteAnneesPrime(),
                    primePctCcn,
                    Boolean.TRUE.equals(request.indexInseeRevalorise()),
                    request.tauxRevalorisationPct(),
                    request.methodeCpSurRappel());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RappelSalaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RappelSalaireAnalysis a = new RappelSalaireAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setPeriodeDebut(result.periodeDebut());
        entity.setPeriodeFin(result.periodeFin());
        entity.setMontantSalaireDuMensuelEur(result.montantSalaireDuMensuelEur());
        entity.setMontantSalairePerVerseMensuelEur(result.montantSalairePerVerseMensuelEur());
        entity.setConventionCollectiveCode(result.conventionCollectiveCode());
        entity.setAncienneteAnneesPrime(result.ancienneteAnneesPrime() != null ? result.ancienneteAnneesPrime() : 0);
        entity.setIndexInseeRevalorise(result.indexInseeRevalorise());
        entity.setTauxRevalorisationPct(result.tauxRevalorisationPct());
        entity.setMethodeCpSurRappel(result.methodeCpSurRappel().name());
        entity.setTotalAvecCpEur(result.totalAvecCpEur());
        entity.setCountry("FRANCE");
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public RappelSalaireResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RappelSalaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de rappel de salaire trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private void validate(RappelSalaireRequest r) {
        if (r.periodeDebut() == null || r.periodeFin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période début et fin requises");
        }
        if (r.periodeFin().isBefore(r.periodeDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période invalide : fin avant début");
        }
        if (r.montantSalaireDuMensuelEur() == null || r.montantSalaireDuMensuelEur().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Salaire dû mensuel requis et strictement positif");
        }
        if (r.montantSalairePerVerseMensuelEur() == null || r.montantSalairePerVerseMensuelEur().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Salaire perçu mensuel requis et positif ou nul");
        }
        if (r.ancienneteAnneesPrime() == null || r.ancienneteAnneesPrime() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ancienneté (années) requise et ≥ 0");
        }
        if (r.indexInseeRevalorise() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ indexInseeRevalorise est requis (booléen)");
        }
        if (r.methodeCpSurRappel() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Méthode CP sur rappel requise (DIX_POURCENT, MAINTIEN, AUCUN)");
        }
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        String country = cf.getWorkspace() != null ? cf.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil disponible uniquement pour les dossiers FRANCE — pour la Belgique, "
                            + "ouvrir une feature jumelle (F-DT-20-BE) au backlog si besoin");
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

    private RappelSalaireResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, RappelSalaireResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private RappelSalaireResponse toResponse(UUID caseFileId, RappelSalaireResult r) {
        return new RappelSalaireResponse(
                caseFileId,
                r.periodeDebut(),
                r.periodeFin(),
                r.montantSalaireDuMensuelEur(),
                r.montantSalairePerVerseMensuelEur(),
                r.conventionCollectiveCode(),
                r.ancienneteAnneesPrime(),
                r.indexInseeRevalorise(),
                r.tauxRevalorisationPct(),
                r.methodeCpSurRappel(),
                r.nbMoisPeriode(),
                r.differentielMensuelEur(),
                r.totalRappelBrutHorsRevalorisationEur(),
                r.montantRevalorisationEur(),
                r.primeAncienneteEur(),
                r.totalRappelBrutEur(),
                r.congesPayesSurRappelEur(),
                r.totalAvecCpEur(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                "FRANCE"
        );
    }
}

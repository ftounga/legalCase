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
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-29 : service applicatif de l'outil "NAO — négociation annuelle obligatoire"
 * (F-DT-66) — apprécie la conformité de la négociation annuelle dans les entreprises
 * pourvues d'un délégué syndical (engagement des blocs obligatoires, périodicité,
 * PV de désaccord) et calcule l'échéance de la prochaine négociation. Outil
 * <b>FRANCE UNIQUEMENT</b>, distinct de F-DT-67 (validité d'un accord d'entreprise)
 * et de F-DT-101 (index égalité professionnelle F/H).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>booléens requis présents, effectif &gt; 0, periodiciteMois ∈ [1, 48]
 *       (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class NaoNegociationAnnuelleService {

    private final NaoNegociationAnnuelleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    /** Test-only seam — override la {@link Clock} interne pour le calcul d'échéance. */
    private Clock clock = Clock.systemDefaultZone();

    public NaoNegociationAnnuelleService(NaoNegociationAnnuelleRepository repository,
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

    /** Test-only seam — override la {@link Clock} interne. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional
    public NaoNegociationAnnuelleResponse analyze(UUID caseFileId,
                                                  NaoNegociationAnnuelleRequest request,
                                                  OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "NAO — négociation annuelle obligatoire — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        NaoNegociationAnnuelleResult result;
        try {
            result = NaoNegociationAnnuelleAnalyzer.analyze(
                    request.effectif(),
                    request.delegueSyndicalPresent(),
                    request.blocRemunerationNegocie(),
                    request.blocEgaliteQvtNegocie(),
                    request.accordMethodePeriodicite(),
                    request.dateDerniereNegociation(),
                    request.periodiciteMois(),
                    request.pvDesaccordEtabli(),
                    request.negociationAboutie(),
                    LocalDate.now(clock));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        NaoNegociationAnnuelleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    NaoNegociationAnnuelleAnalysis a = new NaoNegociationAnnuelleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEffectif(result.effectif());
        entity.setDelegueSyndicalPresent(result.delegueSyndicalPresent());
        entity.setApplicable(result.applicable());
        entity.setPeriodiciteMois(result.periodiciteMois());
        entity.setDateProchaineEcheance(result.dateProchaineEcheance());
        entity.setJoursAvantEcheance(result.joursAvantEcheance());
        entity.setStatutEcheance(result.statutEcheance());
        entity.setItemsObligatoiresManquants(result.itemsObligatoiresManquants());
        entity.setStatut(result.statut());
        entity.setRisqueEntrave(result.risqueEntrave());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public NaoNegociationAnnuelleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        NaoNegociationAnnuelleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de négociation annuelle obligatoire trouvée pour ce dossier"));
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

    private NaoNegociationAnnuelleResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, NaoNegociationAnnuelleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private NaoNegociationAnnuelleResponse toResponse(UUID caseFileId, String country,
                                                      NaoNegociationAnnuelleResult r) {
        return new NaoNegociationAnnuelleResponse(
                caseFileId,
                r.effectif(),
                r.delegueSyndicalPresent(),
                r.applicable(),
                r.checklist(),
                r.periodiciteMois(),
                r.dateProchaineEcheance(),
                r.joursAvantEcheance(),
                r.statutEcheance(),
                r.itemsObligatoiresManquants(),
                r.statut(),
                r.risqueEntrave(),
                r.consequences(),
                country,
                r.baseJuridique());
    }
}

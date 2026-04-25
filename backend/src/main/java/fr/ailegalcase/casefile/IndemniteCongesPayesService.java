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
 * SF-DT-26-01 : service applicatif de l'outil decisionnel Indemnite compensatrice
 * de conges payes (art. L.3141-26 et L.3141-28 Code du travail).
 *
 * <p>Gates : workspace membership, dossier {@code DROIT_DU_TRAVAIL}, country
 * {@code FRANCE} (equivalent BE = pecule de vacances, F-DT-28 backlog).</p>
 */
@Service
public class IndemniteCongesPayesService {

    private final IndemniteCongesPayesRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IndemniteCongesPayesService(IndemniteCongesPayesRepository repository,
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
    public IndemniteCongesPayesResponse calculate(UUID caseFileId,
                                                  IndemniteCongesPayesRequest request,
                                                  OidcUser oidcUser,
                                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil FR uniquement - voir F-DT-28 pour pecule de vacances BE");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete manquante");
        }

        IndemniteCongesPayesResult result;
        try {
            result = IndemniteCongesPayesCalculator.compute(
                    request.totalRemunerationPeriodeEur(),
                    request.joursAcquisAnnee(),
                    request.joursPris(),
                    request.salaireMensuelBrutEur(),
                    request.dateRupture(),
                    request.methodeForcee());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndemniteCongesPayesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IndemniteCongesPayesAnalysis a = new IndemniteCongesPayesAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTotalRemunerationPeriodeEur(result.totalRemunerationPeriodeEur());
        entity.setJoursAcquisAnnee(result.joursAcquisAnnee());
        entity.setJoursPris(result.joursPris());
        entity.setSalaireMensuelBrutEur(result.salaireMensuelBrutEur());
        entity.setDateRupture(result.dateRupture());
        entity.setMethodeForcee(result.methodeForcee() != null ? result.methodeForcee().name() : null);
        entity.setMethodeRetenue(result.methodeRetenue().name());
        entity.setMontantIndemniteEur(result.montantIndemniteEur());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public IndemniteCongesPayesResponse get(UUID caseFileId,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        IndemniteCongesPayesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'indemnite compensatrice de conges payes trouvee pour ce dossier"));
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de serialisation");
        }
    }

    private IndemniteCongesPayesResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, IndemniteCongesPayesResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de deserialisation");
        }
    }

    private IndemniteCongesPayesResponse toResponse(UUID caseFileId,
                                                    String country,
                                                    IndemniteCongesPayesResult r) {
        return new IndemniteCongesPayesResponse(
                caseFileId,
                r.totalRemunerationPeriodeEur(),
                r.joursAcquisAnnee(),
                r.joursPris(),
                r.salaireMensuelBrutEur(),
                r.dateRupture(),
                r.methodeForcee(),
                r.joursDus(),
                r.montantMethodeDixPourcentEur(),
                r.montantMethodeMaintienEur(),
                r.methodeRetenue(),
                r.montantIndemniteEur(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                country
        );
    }
}

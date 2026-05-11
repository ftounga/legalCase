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
public class TribunalFamilleBeMesuresProvisoiresService {

    private final TribunalFamilleBeMesuresProvisoiresRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TribunalFamilleBeMesuresProvisoiresService(
            TribunalFamilleBeMesuresProvisoiresRepository repository,
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
    public TribunalFamilleBeMesuresProvisoiresResponse calculate(
            UUID caseFileId,
            TribunalFamilleBeMesuresProvisoiresRequest request,
            OidcUser oidcUser,
            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mesures provisoires Tribunal de la famille — outil BELGIQUE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        TribunalFamilleBeMesuresProvisoiresResult result;
        try {
            result = TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                    Boolean.TRUE.equals(request.violenceFamiliale()),
                    Boolean.TRUE.equals(request.deplacementEnfantImminent()),
                    Boolean.TRUE.equals(request.dilapidationPatrimoine()),
                    Boolean.TRUE.equals(request.besoinResidenceSeparee()),
                    Boolean.TRUE.equals(request.besoinContributionAlimentaire()),
                    Boolean.TRUE.equals(request.besoinAutoriteParentaleExclusive()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        TribunalFamilleBeMesuresProvisoiresAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TribunalFamilleBeMesuresProvisoiresAnalysis a =
                            new TribunalFamilleBeMesuresProvisoiresAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setViolenceFamiliale(result.violenceFamiliale());
        entity.setDeplacementEnfantImminent(result.deplacementEnfantImminent());
        entity.setDilapidationPatrimoine(result.dilapidationPatrimoine());
        entity.setBesoinResidenceSeparee(result.besoinResidenceSeparee());
        entity.setBesoinContributionAlimentaire(result.besoinContributionAlimentaire());
        entity.setBesoinAutoriteParentaleExclusive(result.besoinAutoriteParentaleExclusive());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public TribunalFamilleBeMesuresProvisoiresResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        TribunalFamilleBeMesuresProvisoiresAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse mesures provisoires trouvée pour ce dossier"));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private TribunalFamilleBeMesuresProvisoiresResult deserialize(String json) {
        try { return objectMapper.readValue(json, TribunalFamilleBeMesuresProvisoiresResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private TribunalFamilleBeMesuresProvisoiresResponse toResponse(UUID caseFileId, String country,
                                                                   TribunalFamilleBeMesuresProvisoiresResult r) {
        return new TribunalFamilleBeMesuresProvisoiresResponse(
                caseFileId,
                country,
                r.violenceFamiliale(),
                r.deplacementEnfantImminent(),
                r.dilapidationPatrimoine(),
                r.besoinResidenceSeparee(),
                r.besoinContributionAlimentaire(),
                r.besoinAutoriteParentaleExclusive(),
                r.scoreUrgence(),
                r.urgenceLevel(),
                r.mesuresRecommandees() != null ? r.mesuresRecommandees() : java.util.List.of(),
                r.verdict(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

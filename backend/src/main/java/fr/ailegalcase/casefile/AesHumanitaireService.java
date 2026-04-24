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
public class AesHumanitaireService {

    private final AesHumanitaireRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AesHumanitaireService(AesHumanitaireRepository repository,
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
    public AesHumanitaireResponse calculate(UUID caseFileId,
                                            AesHumanitaireRequest request,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime AES voie humanitaire propre à la France (L.435-2 CESEDA)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.motifHumanitaireDominant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "motifHumanitaireDominant est requis");
        }

        boolean preuvesMedicales = Boolean.TRUE.equals(request.preuvesMedicales());
        boolean preuvesViolencesOuTraite = Boolean.TRUE.equals(request.preuvesViolencesOuTraite());
        boolean demandeAsileDeposeeEtRejetee = Boolean.TRUE.equals(request.demandeAsileDeposeeEtRejetee());
        boolean commissionTitreSejourSaisie = Boolean.TRUE.equals(request.commissionTitreSejourSaisie());
        boolean menaceOrdrePublic = Boolean.TRUE.equals(request.menaceOrdrePublic());

        AesHumanitaireResult result;
        try {
            result = AesHumanitaireCalculator.compute(
                    request.dateEntreeFrance(),
                    request.motifHumanitaireDominant(),
                    preuvesMedicales,
                    preuvesViolencesOuTraite,
                    demandeAsileDeposeeEtRejetee,
                    commissionTitreSejourSaisie,
                    menaceOrdrePublic,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AesHumanitaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AesHumanitaireAnalysis a = new AesHumanitaireAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntreeFrance(result.dateEntreeFrance());
        entity.setMotifHumanitaireDominant(result.motifHumanitaireDominant());
        entity.setPreuvesMedicales(result.preuvesMedicales());
        entity.setPreuvesViolencesOuTraite(result.preuvesViolencesOuTraite());
        entity.setDemandeAsileDeposeeEtRejetee(result.demandeAsileDeposeeEtRejetee());
        entity.setCommissionTitreSejourSaisie(result.commissionTitreSejourSaisie());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AesHumanitaireResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AesHumanitaireAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AES voie humanitaire trouvée pour ce dossier"));
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

    private AesHumanitaireResult deserialize(String json) {
        try { return objectMapper.readValue(json, AesHumanitaireResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AesHumanitaireResponse toResponse(UUID caseFileId, String country,
                                              AesHumanitaireResult r) {
        return new AesHumanitaireResponse(
                caseFileId,
                r.dateEntreeFrance(),
                r.motifHumanitaireDominant(),
                r.preuvesMedicales(),
                r.preuvesViolencesOuTraite(),
                r.demandeAsileDeposeeEtRejetee(),
                r.commissionTitreSejourSaisie(),
                r.menaceOrdrePublic(),
                r.dateDepotDemande(),
                country,
                r.motifEligible(),
                r.preuvesAdaptees(),
                r.commissionRequise(),
                r.pasMenace(),
                r.scoreGlobal(),
                r.verdictProbabiliteAcceptation(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstruction(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

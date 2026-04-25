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
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-25-01 : service applicatif majeurs protégés (art. 433-441 +
 * 494-1 et s. Cciv). Gate FR + DROIT_FAMILLE, isolation workspace,
 * persistance JSON.
 */
@Service
public class MajeursProtegesService {

    private final MajeursProtegesRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MajeursProtegesService(MajeursProtegesRepository repository,
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
    public MajeursProtegesResponse calculate(UUID caseFileId,
                                             MajeursProtegesRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil disponible uniquement en FRANCE — équivalent BE "
                            + "(administration provisoire art. 488 Code civil BE) "
                            + "en cours de scoping");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.regimeProtectionDemande() == null
                || request.regimeProtectionDemande().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "regimeProtectionDemande est requis");
        }
        if (request.demandeurFamilial() == null
                || request.demandeurFamilial().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "demandeurFamilial est requis");
        }

        boolean alterationMentale = Boolean.TRUE.equals(request.altertationFacultesMentales());
        boolean alterationPhysique = Boolean.TRUE.equals(request.altertationFacultesPhysiques());
        boolean certificat = Boolean.TRUE.equals(request.certificatMedicalCirconstancie());
        boolean consentement = Boolean.TRUE.equals(request.consentementPersonneAProteger());
        boolean urgence = Boolean.TRUE.equals(request.urgencePatrimoniale());
        boolean patrimoine = Boolean.TRUE.equals(request.patrimoineSignificatif());
        boolean isolement = Boolean.TRUE.equals(request.isolementSocial());

        MajeursProtegesResult result;
        try {
            result = MajeursProtegesCalculator.compute(
                    request.regimeProtectionDemande(),
                    alterationMentale,
                    alterationPhysique,
                    certificat,
                    request.dateCertificatMedical(),
                    consentement,
                    request.demandeurFamilial(),
                    request.actesEnvisages(),
                    urgence,
                    patrimoine,
                    isolement);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MajeursProtegesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MajeursProtegesAnalysis a = new MajeursProtegesAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setRegimeProtectionDemande(result.regimeProtectionDemande());
        entity.setAltertationFacultesMentales(result.altertationFacultesMentales());
        entity.setAltertationFacultesPhysiques(result.altertationFacultesPhysiques());
        entity.setCertificatMedicalCirconstancie(result.certificatMedicalCirconstancie());
        entity.setDateCertificatMedical(result.dateCertificatMedical());
        entity.setConsentementPersonneAProteger(result.consentementPersonneAProteger());
        entity.setDemandeurFamilial(result.demandeurFamilial());
        entity.setActesEnvisages(serialize(result.actesEnvisages()));
        entity.setUrgencePatrimoniale(result.urgencePatrimoniale());
        entity.setPatrimoineSignificatif(result.patrimoineSignificatif());
        entity.setIsolementSocial(result.isolementSocial());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public MajeursProtegesResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        MajeursProtegesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de majeur protégé trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private MajeursProtegesResult deserialize(String json) {
        try { return objectMapper.readValue(json, MajeursProtegesResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private MajeursProtegesResponse toResponse(UUID caseFileId, String country,
                                               MajeursProtegesResult r) {
        return new MajeursProtegesResponse(
                caseFileId,
                r.regimeProtectionDemande(),
                r.altertationFacultesMentales(),
                r.altertationFacultesPhysiques(),
                r.certificatMedicalCirconstancie(),
                r.dateCertificatMedical(),
                r.consentementPersonneAProteger(),
                r.demandeurFamilial(),
                r.actesEnvisages() != null ? r.actesEnvisages() : List.of(),
                r.urgencePatrimoniale(),
                r.patrimoineSignificatif(),
                r.isolementSocial(),
                r.scoreEligibilite(),
                r.regimeOptimalRecommande(),
                r.verdictAcceptabiliteJaf(),
                r.delaiProcedureMoisPrevisionnel(),
                r.auditionPersonneObligatoire(),
                r.expertisePsyComplementaireRecommandee(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}

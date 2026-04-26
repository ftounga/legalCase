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
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-33-01 : service de l'analyse de recevabilité AT/MP française
 * (RECONNAISSANCE_AT / RECONNAISSANCE_MP / CONTESTATION_TAUX_IPP).
 * Gates : DROIT_DU_TRAVAIL + FRANCE.
 */
@Service
public class AtMpService {

    private final AtMpRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AtMpService(AtMpRepository repository,
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
    public AtMpResponse calculate(UUID caseFileId,
                                  AtMpRequest request,
                                  OidcUser oidcUser,
                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime AT/MP propre à la France (CSS L.411-1+ / L.461-1+ / L.434-2)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        AtMpResult result;
        try {
            result = AtMpCalculator.compute(
                    request.dispositif(),
                    request.dateAccident(),
                    request.lieuTravail(),
                    request.declarationEmployeurDansLes48h(),
                    request.certificatMedicalInitial(),
                    request.numeroTableau(),
                    request.delaiPriseEnChargeRespecte(),
                    request.dateExposition(),
                    request.tauxFixeParCpam(),
                    request.tauxRevendique(),
                    request.expertiseMedicaleProduite(),
                    request.datePremierAvisCpam(),
                    LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AtMpAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AtMpAnalysis a = new AtMpAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDispositif(result.dispositif());
        entity.setDateAccident(request.dateAccident());
        entity.setLieuTravail(request.lieuTravail());
        entity.setDeclarationEmployeurDansLes48h(request.declarationEmployeurDansLes48h());
        entity.setCertificatMedicalInitial(request.certificatMedicalInitial());
        entity.setNumeroTableau(request.numeroTableau());
        entity.setDelaiPriseEnChargeRespecte(request.delaiPriseEnChargeRespecte());
        entity.setDateExposition(request.dateExposition());
        entity.setTauxFixeParCpam(request.tauxFixeParCpam());
        entity.setTauxRevendique(request.tauxRevendique());
        entity.setExpertiseMedicaleProduite(request.expertiseMedicaleProduite());
        entity.setDatePremierAvisCpam(request.datePremierAvisCpam());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, request, result);
    }

    @Transactional(readOnly = true)
    public AtMpResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AtMpAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AT/MP trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        AtMpResult result = deserialize(entity.getResultData());
        AtMpRequest reconstructed = new AtMpRequest(
                entity.getDispositif(),
                entity.getDateAccident(),
                entity.getLieuTravail(),
                entity.getDeclarationEmployeurDansLes48h(),
                entity.getCertificatMedicalInitial(),
                entity.getNumeroTableau(),
                entity.getDelaiPriseEnChargeRespecte(),
                entity.getDateExposition(),
                entity.getTauxFixeParCpam(),
                entity.getTauxRevendique(),
                entity.getExpertiseMedicaleProduite(),
                entity.getDatePremierAvisCpam());
        return toResponse(caseFileId, country, reconstructed, result);
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
                    "Erreur de sérialisation");
        }
    }

    private AtMpResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AtMpResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private AtMpResponse toResponse(UUID caseFileId, String country,
                                    AtMpRequest request,
                                    AtMpResult r) {
        return new AtMpResponse(
                caseFileId,
                country,
                r.dispositif(),
                r.dispositifLibelle(),
                request.dateAccident(),
                request.lieuTravail(),
                request.declarationEmployeurDansLes48h(),
                request.certificatMedicalInitial(),
                request.numeroTableau(),
                request.delaiPriseEnChargeRespecte(),
                request.dateExposition(),
                request.tauxFixeParCpam(),
                request.tauxRevendique(),
                request.expertiseMedicaleProduite(),
                request.datePremierAvisCpam(),
                r.verdictRecevabilite(),
                r.delaiInstructionJours(),
                r.competence(),
                r.expertiseRequise(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.risqueRefus() != null ? r.risqueRefus() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}

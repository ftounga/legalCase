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
public class AesEtudiantService {

    private final AesEtudiantRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AesEtudiantService(AesEtudiantRepository repository,
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
    public AesEtudiantResponse calculate(UUID caseFileId,
                                         AesEtudiantRequest request,
                                         OidcUser oidcUser,
                                         Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime AES voie étudiante propre à la France (circulaire Valls 28/11/2012)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureePresenceMois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureePresenceMois est requis");
        }
        if (request.anneesScolariteEnFranceConsecutives() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "anneesScolariteEnFranceConsecutives est requis");
        }
        boolean inscription = Boolean.TRUE.equals(request.inscriptionEtablissementReconnu());
        boolean moyens = Boolean.TRUE.equals(request.moyensSubsistance());
        boolean menace = Boolean.TRUE.equals(request.menaceOrdrePublic());
        boolean parcours = Boolean.TRUE.equals(request.parcoursCoherent());

        AesEtudiantResult result;
        try {
            result = AesEtudiantCalculator.compute(
                    request.dateEntreeFrance(),
                    request.dureePresenceMois(),
                    request.anneesScolariteEnFranceConsecutives(),
                    request.niveauEtudesActuel(),
                    request.resultatsAcademiques(),
                    inscription,
                    moyens,
                    menace,
                    parcours,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AesEtudiantAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AesEtudiantAnalysis a = new AesEtudiantAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntreeFrance(result.dateEntreeFrance());
        entity.setDureePresenceMois(result.dureePresenceMois());
        entity.setAnneesScolariteEnFranceConsecutives(
                result.anneesScolariteEnFranceConsecutives());
        entity.setNiveauEtudesActuel(result.niveauEtudesActuel());
        entity.setResultatsAcademiques(result.resultatsAcademiques());
        entity.setInscriptionEtablissementReconnu(result.inscriptionEtablissementReconnu());
        entity.setMoyensSubsistance(result.moyensSubsistance());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setParcoursCoherent(result.parcoursCoherent());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AesEtudiantResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AesEtudiantAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AES voie étudiante trouvée pour ce dossier"));
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private AesEtudiantResult deserialize(String json) {
        try { return objectMapper.readValue(json, AesEtudiantResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AesEtudiantResponse toResponse(UUID caseFileId, String country,
                                           AesEtudiantResult r) {
        return new AesEtudiantResponse(
                caseFileId,
                r.dateEntreeFrance(),
                r.dureePresenceMois(),
                r.anneesScolariteEnFranceConsecutives(),
                r.niveauEtudesActuel(),
                r.resultatsAcademiques(),
                r.inscriptionEtablissementReconnu(),
                r.moyensSubsistance(),
                r.menaceOrdrePublic(),
                r.parcoursCoherent(),
                r.dateDepotDemande(),
                country,
                r.presence3AnsOk(),
                r.scolarite2AnsConsecutivesOk(),
                r.resultatsAcceptables(),
                r.inscriptionValide(),
                r.moyensOk(),
                r.pasMenace(),
                r.parcoursCoherentOk(),
                r.scoreGlobal(),
                r.verdictProbabiliteAcceptation(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstructionSiDemande(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}

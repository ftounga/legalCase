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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DivorceChecklistService {

    private final DivorceChecklistRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    private final LegalReferentialService referentialService;

    public DivorceChecklistService(DivorceChecklistRepository repository, CaseFileRepository caseFileRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    CurrentUserResolver currentUserResolver, ObjectMapper objectMapper,
                                    LegalReferentialService referentialService) {
        this.repository = repository; this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver; this.objectMapper = objectMapper;
        this.referentialService = referentialService;
    }

    @Transactional
    public DivorceChecklistResponse save(UUID caseFileId, DivorceChecklistRequest request,
                                          OidcUser oidcUser, Principal principal) {
        if (!DivorceChecklistReferentiel.isCountryValid(request.country()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pays non supporté : " + request.country());
        User user = resolveUser(oidcUser, principal);
        CaseFile cf = resolveCaseFile(caseFileId, user);

        DivorceChecklistResult result = buildResult(request.country(),
                request.etapeStatuts() != null ? request.etapeStatuts() : Map.of(),
                request.pieceStatuts() != null ? request.pieceStatuts() : Map.of());

        DivorceChecklist entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> { DivorceChecklist e = new DivorceChecklist(); e.setCaseFile(cf); return e; });
        entity.setCountry(request.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public DivorceChecklistResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        DivorceChecklist entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune checklist divorce trouvée"));
        DivorceChecklistResult result = deserialize(entity.getResultData(), DivorceChecklistResult.class);
        return toResponse(caseFileId, result);
    }

    private DivorceChecklistResult buildResult(String country, Map<String, String> etapeStatuts, Map<String, String> pieceStatuts) {
        List<DivorceChecklistResult.EtapeStatus> etapes = referentialService.getDivorceEtapes(country).stream()
                .map(e -> new DivorceChecklistResult.EtapeStatus(e.code(), e.label(), e.ordre(), e.description(),
                        e.delai(), e.obligatoire(), etapeStatuts.getOrDefault(e.code(), "A_FAIRE")))
                .toList();
        List<DivorceChecklistResult.PieceStatus> pieces = referentialService.getDivorcePieces(country).stream()
                .map(p -> new DivorceChecklistResult.PieceStatus(p.code(), p.label(), p.description(),
                        p.obligatoire(), pieceStatuts.getOrDefault(p.code(), "MANQUANTE")))
                .toList();
        int etapesOk = (int) etapes.stream().filter(e -> "FAIT".equals(e.statut())).count();
        int piecesOk = (int) pieces.stream().filter(p -> "PRESENTE".equals(p.statut())).count();
        return new DivorceChecklistResult(country, etapes, pieces, etapesOk, etapes.size(),
                piecesOk, pieces.size(), DivorceChecklistReferentiel.getBaseJuridique(country));
    }

    private User resolveUser(OidcUser o, Principal p) {
        return currentUserResolver.resolve(o, OAuthProviderResolver.resolve(p), p);
    }
    private CaseFile resolveCaseFile(UUID id, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean m = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(mem -> mem.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!m) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce dossier n'est pas un dossier de droit de la famille");
        return cf;
    }
    private String serialize(Object o) { try { return objectMapper.writeValueAsString(o); } catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur"); } }
    private <T> T deserialize(String json, Class<T> c) { try { return objectMapper.readValue(json, c); } catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur"); } }

    private DivorceChecklistResponse toResponse(UUID id, DivorceChecklistResult r) {
        return new DivorceChecklistResponse(id, r.country(), r.etapes(), r.pieces(),
                r.etapesCompletees(), r.etapesTotal(), r.piecesPresentes(), r.piecesTotal(), r.baseJuridique());
    }
}

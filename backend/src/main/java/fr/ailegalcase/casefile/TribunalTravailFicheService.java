package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.BelgianCompensationCalculator;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TribunalTravailFicheService {

    private final TribunalTravailFicheRepository ficheRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public TribunalTravailFicheService(TribunalTravailFicheRepository ficheRepository,
                                        CaseFileRepository caseFileRepository,
                                        WorkspaceMemberRepository workspaceMemberRepository,
                                        CurrentUserResolver currentUserResolver,
                                        CaseAnalysisRepository caseAnalysisRepository,
                                        DocumentRepository documentRepository,
                                        ObjectMapper objectMapper) {
        this.ficheRepository = ficheRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TribunalTravailFicheResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        validateBelgianLabourCase(caseFile);

        return ficheRepository.findByCaseFileId(caseFileId)
                .map(f -> toResponse(f, buildPiecesList(caseFile)))
                .orElseGet(() -> buildPrefilledResponse(caseFile));
    }

    @Transactional
    public TribunalTravailFicheResponse upsert(UUID caseFileId, TribunalTravailFicheRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        validateBelgianLabourCase(caseFile);

        TribunalTravailFiche fiche = ficheRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TribunalTravailFiche f = new TribunalTravailFiche();
                    f.setCaseFile(caseFile);
                    return f;
                });

        fiche.setRequerant(toJson(request.requerant()));
        fiche.setDefendeur(toJson(request.defendeur()));
        fiche.setProcedureInfo(toJson(request.procedureInfo()));
        fiche.setContratInfo(toJson(request.contratInfo()));
        fiche.setDemandes(toJson(request.demandes()));
        fiche.setExposeDesMoyens(request.exposeDesMoyens());

        TribunalTravailFiche saved = ficheRepository.save(fiche);
        return toResponse(saved, buildPiecesList(caseFile));
    }

    private void validateBelgianLabourCase(CaseFile caseFile) {
        String country = caseFile.getWorkspace().getCountry();
        String domain = caseFile.getLegalDomain();
        if (!"BELGIQUE".equals(country) || !"DROIT_DU_TRAVAIL".equals(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La requête tribunal du travail n'est disponible que pour les dossiers de droit du travail belge");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }

    private List<TribunalTravailFicheResponse.PieceEntry> buildPiecesList(CaseFile caseFile) {
        List<Document> docs = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
        List<TribunalTravailFicheResponse.PieceEntry> pieces = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            pieces.add(new TribunalTravailFicheResponse.PieceEntry(i + 1, docs.get(i).getOriginalFilename()));
        }
        return pieces;
    }

    private TribunalTravailFicheResponse buildPrefilledResponse(CaseFile caseFile) {
        TribunalTravailFicheRequest.Requerant requerant = new TribunalTravailFicheRequest.Requerant(
                "", null, null, null);
        TribunalTravailFicheRequest.Defendeur defendeur = new TribunalTravailFicheRequest.Defendeur(
                null, null, null, null);
        TribunalTravailFicheRequest.ProcedureInfo procedureInfo = new TribunalTravailFicheRequest.ProcedureInfo(
                null, null, "FR", null);
        TribunalTravailFicheRequest.ContratInfo contratInfo = new TribunalTravailFicheRequest.ContratInfo(
                null, null, null, null);
        List<TribunalTravailFicheRequest.Demande> demandes = new ArrayList<>();

        caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE)
                .ifPresent(analysis -> prefillFromAnalysis(analysis, demandes));

        return new TribunalTravailFicheResponse(
                null, requerant, defendeur, procedureInfo, contratInfo,
                demandes, null, buildPiecesList(caseFile), null);
    }

    private void prefillFromAnalysis(CaseAnalysis analysis,
                                      List<TribunalTravailFicheRequest.Demande> demandes) {
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        if (response.compensationEstimate() != null) {
            var est = response.compensationEstimate();
            var belgian = BelgianCompensationCalculator.calculate(
                    est.ancienneteAnnees(), est.ancienneteMois(), est.salaireReference());
            belgian.ifPresent(b -> demandes.add(new TribunalTravailFicheRequest.Demande(
                    "Indemnité compensatoire de préavis (" + b.preavisSemaines() + " semaines)",
                    b.indemniteCompensatoire())));
        }
    }

    private TribunalTravailFicheResponse toResponse(TribunalTravailFiche fiche,
                                                      List<TribunalTravailFicheResponse.PieceEntry> pieces) {
        return new TribunalTravailFicheResponse(
                fiche.getId(),
                fromJson(fiche.getRequerant(), TribunalTravailFicheRequest.Requerant.class),
                fromJson(fiche.getDefendeur(), TribunalTravailFicheRequest.Defendeur.class),
                fromJson(fiche.getProcedureInfo(), TribunalTravailFicheRequest.ProcedureInfo.class),
                fromJson(fiche.getContratInfo(), TribunalTravailFicheRequest.ContratInfo.class),
                fromJsonList(fiche.getDemandes(), new TypeReference<>() {}),
                fiche.getExposeDesMoyens(),
                pieces,
                fiche.getUpdatedAt());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T fromJsonList(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }
}

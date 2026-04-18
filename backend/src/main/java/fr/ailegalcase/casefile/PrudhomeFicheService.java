package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
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
public class PrudhomeFicheService {

    private final PrudhomeFicheRepository ficheRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public PrudhomeFicheService(PrudhomeFicheRepository ficheRepository,
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
    public PrudhomeFicheResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        return ficheRepository.findByCaseFileId(caseFileId)
                .map(f -> toResponse(f, buildPiecesList(caseFile)))
                .orElseGet(() -> buildPrefilledResponse(caseFile));
    }

    @Transactional
    public PrudhomeFicheResponse upsert(UUID caseFileId, PrudhomeFicheRequest request,
                                        OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        PrudhomeFiche fiche = ficheRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PrudhomeFiche f = new PrudhomeFiche();
                    f.setCaseFile(caseFile);
                    return f;
                });

        fiche.setDemandeur(toJson(request.demandeur()));
        fiche.setDefendeur(toJson(request.defendeur()));
        fiche.setDemandes(toJson(request.demandes()));
        fiche.setFaitsTexte(request.faitsTexte());
        fiche.setMoyensDroitTexte(request.moyensDroitTexte());

        PrudhomeFiche saved = ficheRepository.save(fiche);
        return toResponse(saved, buildPiecesList(caseFile));
    }

    // --- private helpers ---

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

    private List<PrudhomeFicheResponse.PieceEntry> buildPiecesList(CaseFile caseFile) {
        List<Document> docs = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
        List<PrudhomeFicheResponse.PieceEntry> pieces = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            pieces.add(new PrudhomeFicheResponse.PieceEntry(i + 1, docs.get(i).getOriginalFilename()));
        }
        return pieces;
    }

    private PrudhomeFicheResponse buildPrefilledResponse(CaseFile caseFile) {
        PrefillAccumulator acc = new PrefillAccumulator();

        caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE)
                .ifPresent(analysis -> prefillFromAnalysis(analysis, acc));

        return new PrudhomeFicheResponse(
                null,
                acc.buildDemandeur(),
                acc.buildDefendeur(),
                acc.demandes,
                null,
                null,
                buildPiecesList(caseFile),
                null
        );
    }

    private void prefillFromAnalysis(CaseAnalysis analysis, PrefillAccumulator acc) {
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var travail = response.travailExtractedData();
        if (travail != null) {
            acc.nomSalarie           = travail.nomSalarie();
            acc.prenomSalarie        = travail.prenomSalarie();
            acc.adresseSalarie       = travail.adresseSalarie();
            acc.professionSalarie    = travail.poste();
            acc.nomEmployeur         = travail.nomEmployeur();
            acc.adresseEmployeur     = travail.adresseEmployeur();
            acc.siretEmployeur       = travail.siretEmployeur();
            acc.representantEmployeur = travail.representantEmployeur();
        }

        if (response.compensationEstimate() != null) {
            var est = response.compensationEstimate();
            acc.demandes.add(new PrudhomeFicheRequest.Demande(
                    "Indemnité de licenciement", est.indemnite()
            ));

            // Demandes additionnelles contextuelles — salaire et type rupture licenciement requis
            String typeRupture = est.typeRupture();
            double salaire = est.salaireReference();
            int anciennete = est.ancienneteAnnees();
            if (salaire > 0 && typeRupture != null
                    && (typeRupture.equals("LICENCIEMENT") || typeRupture.equals("LICENCIEMENT_ECONOMIQUE"))) {
                // Indemnité compensatoire de préavis : 1 mois si < 2 ans anc., 2 mois si ≥ 2 ans
                double preavisMois = anciennete >= 2 ? 2.0 : 1.0;
                acc.demandes.add(new PrudhomeFicheRequest.Demande(
                        "Indemnité compensatoire de préavis (" + (int) preavisMois + " mois)",
                        salaire * preavisMois
                ));
                // DI licenciement sans cause réelle et sérieuse — valeur indicative 1 mois de salaire
                acc.demandes.add(new PrudhomeFicheRequest.Demande(
                        "Dommages et intérêts pour licenciement sans cause réelle et sérieuse (valeur indicative)",
                        salaire
                ));
            }
        }
    }

    /** Accumulator qui collecte les champs de pré-remplissage depuis plusieurs sources. */
    private static final class PrefillAccumulator {
        String nomSalarie, prenomSalarie, adresseSalarie, professionSalarie;
        String nomEmployeur, adresseEmployeur, siretEmployeur, representantEmployeur;
        final List<PrudhomeFicheRequest.Demande> demandes = new ArrayList<>();

        PrudhomeFicheRequest.Demandeur buildDemandeur() {
            return new PrudhomeFicheRequest.Demandeur(
                    nomSalarie != null ? nomSalarie : "",
                    prenomSalarie, adresseSalarie, null, null, professionSalarie);
        }

        PrudhomeFicheRequest.Defendeur buildDefendeur() {
            return new PrudhomeFicheRequest.Defendeur(
                    nomEmployeur, adresseEmployeur, siretEmployeur, representantEmployeur);
        }
    }

    private PrudhomeFicheResponse toResponse(PrudhomeFiche fiche,
                                              List<PrudhomeFicheResponse.PieceEntry> pieces) {
        return new PrudhomeFicheResponse(
                fiche.getId(),
                fromJson(fiche.getDemandeur(), PrudhomeFicheRequest.Demandeur.class),
                fromJson(fiche.getDefendeur(), PrudhomeFicheRequest.Defendeur.class),
                fromJsonList(fiche.getDemandes(), new TypeReference<List<PrudhomeFicheRequest.Demande>>() {}),
                fiche.getFaitsTexte(),
                fiche.getMoyensDroitTexte(),
                pieces,
                fiche.getUpdatedAt()
        );
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
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
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

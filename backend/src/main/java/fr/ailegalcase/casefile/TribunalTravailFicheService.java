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

    /** CCT 109 : fourchette 3-17 semaines. Milieu = 10 semaines comme valeur indicative. */
    private static final int CCT109_SEMAINES_INDICATIF = 10;

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
        PrefillAccumulator acc = new PrefillAccumulator();

        caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE)
                .ifPresent(analysis -> prefillFromAnalysis(analysis, acc));

        TribunalTravailFicheRequest.ProcedureInfo procedureInfo = new TribunalTravailFicheRequest.ProcedureInfo(
                null, null, "FR", null);

        return new TribunalTravailFicheResponse(
                null,
                acc.buildRequerant(),
                acc.buildDefendeur(),
                procedureInfo,
                acc.buildContratInfo(),
                acc.demandes,
                null,
                buildPiecesList(caseFile),
                null);
    }

    private void prefillFromAnalysis(CaseAnalysis analysis, PrefillAccumulator acc) {
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var travail = response.travailExtractedData();
        if (travail != null) {
            acc.nomSalarie           = travail.nomSalarie();
            acc.prenomSalarie        = travail.prenomSalarie();
            acc.adresseSalarie       = travail.adresseSalarie();
            acc.nomEmployeur         = travail.nomEmployeur();
            acc.adresseEmployeur     = travail.adresseEmployeur();
            acc.bceEmployeur         = travail.bceEmployeur();
            acc.representantEmployeur = travail.representantEmployeur();
            acc.dateEntree           = travail.dateEntree();
            acc.dateLicenciement     = travail.dateLicenciement();
        }

        if (response.compensationEstimate() != null) {
            var est = response.compensationEstimate();
            var belgianOpt = BelgianCompensationCalculator.calculate(
                    est.ancienneteAnnees(), est.ancienneteMois(), est.salaireReference());
            belgianOpt.ifPresent(b -> {
                acc.demandes.add(new TribunalTravailFicheRequest.Demande(
                        "Indemnité compensatoire de préavis (" + b.preavisSemaines() + " semaines)",
                        b.indemniteCompensatoire()));

                // CCT 109 — licenciement manifestement déraisonnable : 3 à 17 semaines.
                // On pré-remplit à 10 semaines (milieu de fourchette) comme valeur indicative.
                // Condition : rupture = licenciement ET salaire réellement connu (donneesPartielles false).
                String typeRupture = est.typeRupture();
                boolean isLicenciement = typeRupture != null
                        && (typeRupture.equals("LICENCIEMENT") || typeRupture.equals("LICENCIEMENT_ECONOMIQUE"));
                if (isLicenciement && !b.donneesPartielles() && b.salaireReference() > 0) {
                    // Calcul depuis le salaire mensuel brut (non arrondi) pour éviter la double
                    // arrondi que provoquerait l'usage de b.salaireHebdomadaire() (déjà à 2 décimales).
                    double cct109Indicatif = Math.round(
                            b.salaireReference() * 12.0 / 52.0 * CCT109_SEMAINES_INDICATIF * 100.0) / 100.0;
                    acc.demandes.add(new TribunalTravailFicheRequest.Demande(
                            "Indemnité pour licenciement manifestement déraisonnable (CCT 109, "
                                    + "valeur indicative " + CCT109_SEMAINES_INDICATIF
                                    + " semaines, fourchette 3-17)",
                            cct109Indicatif));
                }
            });
        }
    }

    /** Accumulator qui collecte les champs de pré-remplissage depuis la synthèse IA. */
    private static final class PrefillAccumulator {
        String nomSalarie, prenomSalarie, adresseSalarie;
        String nomEmployeur, adresseEmployeur, bceEmployeur, representantEmployeur;
        String dateEntree, dateLicenciement;
        final List<TribunalTravailFicheRequest.Demande> demandes = new ArrayList<>();

        TribunalTravailFicheRequest.Requerant buildRequerant() {
            return new TribunalTravailFicheRequest.Requerant(
                    nomSalarie != null ? nomSalarie : "",
                    prenomSalarie,
                    adresseSalarie,
                    null);
        }

        TribunalTravailFicheRequest.Defendeur buildDefendeur() {
            return new TribunalTravailFicheRequest.Defendeur(
                    nomEmployeur,
                    adresseEmployeur,
                    bceEmployeur,
                    representantEmployeur);
        }

        TribunalTravailFicheRequest.ContratInfo buildContratInfo() {
            return new TribunalTravailFicheRequest.ContratInfo(
                    null,
                    dateEntree,
                    dateLicenciement,
                    null);
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

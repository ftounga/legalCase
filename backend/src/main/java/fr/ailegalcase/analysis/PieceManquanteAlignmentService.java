package fr.ailegalcase.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ExpectedPiece;
import fr.ailegalcase.referential.LegalReferentialService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * F-194 SF-194-01 — Matérialise (et lit) l'alignement entre les pièces manquantes
 * extraites par l'IA dans {@code analysis_result.pieces_manquantes} et les
 * statuts avocat curés dans {@link PieceManquanteStatus}.
 *
 * <p>Pattern miroir {@link RetainedPisteAlignmentService} (F-192) et
 * {@link ProcedureCheckAlignmentService} (F-193). Activation : appelé
 * UNIQUEMENT depuis {@code EnrichedAnalysisService.run} APRÈS les hooks F-192
 * et F-193 pour matérialiser :
 * <ol>
 *   <li>l'alignement (persisté dans {@code case_analyses.pieces_alignment_json})</li>
 *   <li>les case_deadlines ({@code source = "PIECE_A_DEMANDER"}) au niveau dossier
 *       pour chaque pièce statut {@code A_DEMANDER} (date butoir today + 14j),
 *       idempotent sur la clé {@code (caseFileId, source, label)}</li>
 * </ol>
 *
 * <p>Cohérence F-92 STRICTE : le JSON {@code pieces_manquantes} dans
 * {@code analysis_result} N'EST PAS muté par F-194 (différent de F-192/F-193
 * qui injectent des entrées). L'overlay statut est purement matérialisé en
 * dehors du JSON, dans {@code pieces_alignment_json}.</p>
 *
 * <p>Strict gating Synthèse enrichie : aucun side-effect ailleurs.
 * {@link PieceManquanteStatusService#upsertStatus} reste un PUT pur (test
 * régression CA-10 obligatoire).</p>
 */
@Service
public class PieceManquanteAlignmentService {

    private static final Logger log = LoggerFactory.getLogger(PieceManquanteAlignmentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<PieceManquanteAlignment>> LIST_OF_ALIGNMENT =
            new TypeReference<>() {};

    /** Délai butoir fixe V1 pour les pièces A_DEMANDER : aujourd'hui + 14 jours. */
    static final int DELAI_PIECE_A_DEMANDER_JOURS = 14;

    /** Source des délais auto F-194 dans {@link CaseDeadline}. */
    static final String DEADLINE_SOURCE = "PIECE_A_DEMANDER";

    private final PieceManquanteStatusRepository pieceManquanteStatusRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseDeadlineRepository caseDeadlineRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    /** F-294 SF-294-01 — référentiel des pièces attendues (canonisation). Nullable en test. */
    private final LegalReferentialService legalReferentialService;
    private final Clock clock;

    @Autowired
    public PieceManquanteAlignmentService(PieceManquanteStatusRepository pieceManquanteStatusRepository,
                                           CaseAnalysisRepository caseAnalysisRepository,
                                           CaseFileRepository caseFileRepository,
                                           CaseDeadlineRepository caseDeadlineRepository,
                                           WorkspaceMemberRepository workspaceMemberRepository,
                                           CurrentUserResolver currentUserResolver,
                                           LegalReferentialService legalReferentialService) {
        this(pieceManquanteStatusRepository, caseAnalysisRepository, caseFileRepository,
                caseDeadlineRepository, workspaceMemberRepository, currentUserResolver,
                legalReferentialService, Clock.systemUTC());
    }

    /** Constructeur de test (injection {@link Clock}). */
    PieceManquanteAlignmentService(PieceManquanteStatusRepository pieceManquanteStatusRepository,
                                    CaseAnalysisRepository caseAnalysisRepository,
                                    CaseFileRepository caseFileRepository,
                                    CaseDeadlineRepository caseDeadlineRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    CurrentUserResolver currentUserResolver,
                                    LegalReferentialService legalReferentialService,
                                    Clock clock) {
        this.pieceManquanteStatusRepository = pieceManquanteStatusRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseDeadlineRepository = caseDeadlineRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.legalReferentialService = legalReferentialService;
        this.clock = clock;
    }

    // ====================================================================
    //  MATÉRIALISATION (appelée UNIQUEMENT depuis EnrichedAnalysisService.run
    //  APRÈS RetainedPisteAlignmentService + ProcedureCheckAlignmentService)
    // ====================================================================

    /**
     * Calcule + persiste l'alignement (overlay statut sur le JSON
     * pieces_manquantes IA) puis propage les délais auto pour les pièces
     * statut {@code A_DEMANDER}.
     *
     * <p>Cohérence F-92 STRICTE : ne mute PAS le JSON
     * {@code analysis_result.pieces_manquantes}.</p>
     *
     * <p>Fail-open : toute exception déclenche un log warn et laisse la
     * nouvelle analyse intacte (l'alignement reste {@code null}, mais le run
     * de Synthèse enrichie réussit quand même).</p>
     *
     * @param newAnalysis nouvelle analyse {@code DONE} créée par
     *                    {@code EnrichedAnalysisService.finalizeEnrichedAnalysis}
     */
    @Transactional
    public void materializeForAnalysis(CaseAnalysis newAnalysis) {
        if (newAnalysis == null || newAnalysis.getId() == null) return;
        try {
            UUID caseFileId = newAnalysis.getCaseFile().getId();

            // (1) extraire la liste pieces_manquantes du JSON IA (lecture seule)
            List<String> piecesIa = extractPiecesFromAnalysisResult(newAnalysis.getAnalysisResult());

            // (1bis) F-294 SF-294-01 — canonisation des libellés AVANT la jointure :
            // toute pièce LLM dont la forme normalisée correspond EXACTEMENT à un
            // libellé du socle du dossier est remplacée par le libellé canonique.
            // Correspondance normalisée exacte uniquement (jamais de fuzzy, CA10) ;
            // ne retire jamais de pièce (CA9). Fail-open (CA7).
            Map<String, String> canonicalByNorm = buildCanonicalByNorm(newAnalysis.getCaseFile());
            if (!canonicalByNorm.isEmpty()) {
                List<String> canonized = new ArrayList<>(piecesIa.size());
                for (String piece : piecesIa) {
                    String canonical = piece == null ? null : canonicalByNorm.get(normalize(piece));
                    canonized.add(canonical != null ? canonical : piece);
                }
                piecesIa = canonized;
            }

            // (2) charger l'overlay statut avocat
            List<PieceManquanteStatus> statusesAll =
                    pieceManquanteStatusRepository.findByCaseFileId(caseFileId);
            Map<String, PieceManquanteStatus> statusByNorm = new HashMap<>();
            for (PieceManquanteStatus s : statusesAll) {
                if (s.getPieceLibelleNormalise() != null) {
                    statusByNorm.put(s.getPieceLibelleNormalise(), s);
                }
            }

            // (3) construire l'alignement : (a) chaque pièce IA + statut overlay
            //     (défaut A_DEMANDER), (b) ajouter les pièces overlay statut
            //     OBTENUE / NON_APPLICABLE qui ne sont plus dans le JSON IA
            //     (l'IA a régénéré sans les mentionner) — utile pour mémoire
            //     dashboard.
            List<PieceManquanteAlignment> alignments = new ArrayList<>();
            // dédup sur libellé normalisé
            LinkedHashMap<String, PieceManquanteAlignment> dedup = new LinkedHashMap<>();
            for (String piece : piecesIa) {
                if (piece == null || piece.isBlank()) continue;
                String norm = normalize(piece);
                if (dedup.containsKey(norm)) continue;
                PieceManquanteStatus overlay = statusByNorm.get(norm);
                String statut = overlay != null ? overlay.getStatut()
                        : PieceManquanteStatus.STATUT_A_DEMANDER;
                String destinataire = overlay != null ? overlay.getDestinataire() : null;
                String raison = overlay != null ? overlay.getRaisonNonApp() : null;
                java.util.UUID docId = overlay != null ? overlay.getObtainedDocumentId() : null;
                dedup.put(norm, new PieceManquanteAlignment(piece.trim(), statut, destinataire, raison, docId));
            }
            // ajouter les pièces overlay non encore dans dedup (statut OBTENUE / NON_APPLICABLE)
            for (PieceManquanteStatus s : statusesAll) {
                String norm = s.getPieceLibelleNormalise();
                if (norm == null || dedup.containsKey(norm)) continue;
                if (PieceManquanteStatus.STATUT_OBTENUE.equals(s.getStatut())
                        || PieceManquanteStatus.STATUT_NON_APPLICABLE.equals(s.getStatut())) {
                    dedup.put(norm, new PieceManquanteAlignment(
                            s.getPieceLibelleOriginal(), s.getStatut(),
                            s.getDestinataire(), s.getRaisonNonApp(), s.getObtainedDocumentId()));
                }
            }
            alignments.addAll(dedup.values());

            // (4) persiste le JSON sur la nouvelle analyse — sans toucher analysis_result
            try {
                String json = MAPPER.writeValueAsString(alignments);
                newAnalysis.setPiecesAlignmentJson(json);
                caseAnalysisRepository.save(newAnalysis);
            } catch (Exception e) {
                log.warn("F-194: failed to serialize pieces alignment for analysis {} — fail-open",
                        newAnalysis.getId(), e);
            }

            // (5) propagation case_deadlines (idempotent niveau dossier) pour A_DEMANDER
            try {
                propagateToDeadlines(newAnalysis.getCaseFile(), alignments);
            } catch (Exception e) {
                log.warn("F-194: propagateToDeadlines failed for caseFile {} — fail-open",
                        newAnalysis.getCaseFile().getId(), e);
            }
        } catch (Exception e) {
            log.warn("F-194: materializeForAnalysis fail-open for analysis {}",
                    newAnalysis.getId(), e);
        }
    }

    /**
     * F-294 SF-294-01 — construit la table {@code normalize(label) → label canonique}
     * du socle de pièces attendues pour le dossier (résolu par
     * {@code (legalDomain × country × procedureStage)}).
     *
     * <p>Fail-open (CA7) : si le référentiel n'est pas injecté, ou si le dossier
     * / workspace est incomplet, ou si la résolution lève une exception, renvoie
     * une map vide → aucune canonisation (comportement F-194 strict inchangé).
     */
    private Map<String, String> buildCanonicalByNorm(CaseFile caseFile) {
        if (legalReferentialService == null || caseFile == null) return Map.of();
        try {
            Workspace ws = caseFile.getWorkspace();
            if (ws == null) return Map.of();
            String legalDomain = ws.getLegalDomain();
            String country = ws.getCountry();
            List<ExpectedPiece> socle = legalReferentialService.getExpectedPieces(
                    legalDomain, country, caseFile.getProcedureStage());
            if (socle == null || socle.isEmpty()) return Map.of();
            Map<String, String> map = new HashMap<>();
            for (ExpectedPiece p : socle) {
                if (p.label() != null && !p.label().isBlank()) {
                    // En cas de doublon normalisé, la première (ordre croissant) prime.
                    map.putIfAbsent(normalize(p.label()), p.label());
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("F-294: buildCanonicalByNorm fail-open for caseFile {}",
                    caseFile.getId(), e);
            return Map.of();
        }
    }

    // ---- extraction depuis analysis_result ----

    /** Extrait les libellés pieces_manquantes du JSON {@code analysis_result}. */
    static List<String> extractPiecesFromAnalysisResult(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        try {
            String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
            JsonNode root = MAPPER.readTree(stripped);
            JsonNode pieces = root.get("pieces_manquantes");
            if (pieces == null || !pieces.isArray()) return List.of();
            List<String> out = new ArrayList<>();
            for (JsonNode p : pieces) {
                String t = extractPieceTexte(p);
                if (t != null && !t.isBlank()) out.add(t);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String extractPieceTexte(JsonNode p) {
        if (p == null) return null;
        if (p.isTextual()) return p.asText();
        if (p.isObject()) {
            JsonNode t = p.get("texte");
            if (t != null && t.isTextual()) return t.asText();
        }
        return null;
    }

    static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    // ---- propagation case_deadlines ----

    /**
     * Pour chaque pièce statut {@code A_DEMANDER}, crée une entrée
     * {@link CaseDeadline} {@code source = "PIECE_A_DEMANDER"} idempotente
     * sur la clé {@code (caseFileId, source, label)}, échéance today + 14j.
     */
    private void propagateToDeadlines(CaseFile caseFile, List<PieceManquanteAlignment> alignments) {
        if (caseFile == null) return;
        UUID caseFileId = caseFile.getId();

        List<CaseDeadline> existing = caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFileId);
        Set<String> existingKeys = new HashSet<>();
        for (CaseDeadline d : existing) {
            if (DEADLINE_SOURCE.equals(d.getSource())) {
                existingKeys.add(d.getLabel());
            }
        }

        LocalDate dueDate = LocalDate.now(clock).plusDays(DELAI_PIECE_A_DEMANDER_JOURS);

        for (PieceManquanteAlignment a : alignments) {
            if (!PieceManquanteStatus.STATUT_A_DEMANDER.equals(a.statut())) continue;
            String label = buildDeadlineLabel(a.pieceLibelle());
            if (existingKeys.contains(label)) continue;

            CaseDeadline d = new CaseDeadline();
            d.setCaseFile(caseFile);
            d.setLabel(label);
            d.setDueDate(dueDate);
            d.setSource(DEADLINE_SOURCE);
            caseDeadlineRepository.save(d);
            existingKeys.add(label);
        }
    }

    static String buildDeadlineLabel(String pieceLibelle) {
        String texte = pieceLibelle == null ? "" : pieceLibelle.trim();
        // Limite la longueur pour respecter VARCHAR(255) côté case_deadlines.label
        // Préfixe "Demander au client : " = 21 caractères → suffixe max 230 char
        String suffix = texte.length() <= 230 ? texte : texte.substring(0, 230) + "…";
        return "Demander au client : " + suffix;
    }

    // ====================================================================
    //  LECTURE (endpoint GET /pieces-manquantes-alignment)
    // ====================================================================

    /**
     * Renvoie la liste matérialisée pour la dernière analyse {@code DONE} du
     * dossier — pure lecture, aucun calcul à la volée. Isolation workspace
     * stricte (404 camouflage si dossier hors workspace).
     */
    @Transactional(readOnly = true)
    public List<PieceManquanteAlignment> getForLatestAnalysis(UUID caseFileId,
                                                               OidcUser oidcUser,
                                                               Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        return caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(a -> deserializeAlignment(a.getPiecesAlignmentJson()))
                .orElseGet(List::of);
    }

    /**
     * Helper public pour les consommateurs ({@code CaseFileDashboardService},
     * tests). Désérialise le JSON persisté ; renvoie liste vide en cas
     * d'échec (fail-open).
     */
    public List<PieceManquanteAlignment> deserializeAlignment(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<PieceManquanteAlignment> list = MAPPER.readValue(json, LIST_OF_ALIGNMENT);
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("F-194: failed to deserialize pieces_alignment_json — empty list");
            return List.of();
        }
    }
}

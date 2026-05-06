package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_analyses")
@Getter
@Setter
public class CaseAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 20)
    private AnalysisType analysisType;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private AnalysisStatus analysisStatus;

    @Column(name = "analysis_result", length = Integer.MAX_VALUE)
    private String analysisResult;

    /**
     * F-185 SF-185-01 — état partiel persisté pendant le streaming Sonnet.
     * Accumulé section JSON par section JSON ; purgé (NULL) au DONE/FAILED quand
     * {@code analysisResult} complet le remplace. Permet le refresh-safe sur la
     * page synthèse pendant le streaming.
     * Stocké en JSONB côté DB (cf. migration 201).
     */
    @Column(name = "partial_state", columnDefinition = "TEXT")
    private String partialState;

    /**
     * F-192 SF-192-01 — alignement matérialisé des pistes stratégiques RETAINED de cette analyse
     * vers les outils décisionnels du dossier (statique JSON, calculé une fois en fin de run
     * de Synthèse enrichie par {@code RetainedPisteAlignmentService.materializeForAnalysis}).
     *
     * <p>Schéma : tableau d'objets
     * {@code [{"pisteId":..., "texte":..., "baseJuridique":..., "horizonTemporel":...,
     * "conditions":[...], "toolIdCible":..., "matchStatus":"ALIGNED|DIVERGENT|NOT_ANALYZED|NO_TARGET_TOOL"}]}.
     *
     * <p>{@code null} pour les analyses pré-F-192 ou les runs où la matérialisation a échoué
     * (fail-open) — l'endpoint {@code /retained-pistes-alignment} retourne {@code []} dans ce cas.</p>
     */
    @Column(name = "retained_pistes_alignment_json", columnDefinition = "TEXT")
    private String retainedPistesAlignmentJson;

    /**
     * F-193 SF-193-01 — alignement matérialisé des points procéduraux F-96 de cette analyse
     * vers les outils décisionnels du dossier (statique JSON, calculé une fois en fin de run
     * de Synthèse enrichie par {@code ProcedureCheckAlignmentService.materializeForAnalysis},
     * APRÈS le hook F-192).
     *
     * <p>Schéma : tableau d'objets
     * {@code [{"checkId":..., "libelle":..., "critereCode":..., "statut":..., "expectedValue":...,
     * "raison":..., "toolIdCible":..., "matchStatus":"ALIGNED|DIVERGENT|NON_COMPLIANT_FLAG|TO_VERIFY_FLAG|NOT_ANALYZED|NO_TARGET_TOOL"}]}.
     *
     * <p>{@code null} pour les analyses pré-F-193 ou les runs où la matérialisation a échoué
     * (fail-open) — l'endpoint {@code /procedure-checks-alignment} retourne {@code []} dans ce cas.</p>
     */
    @Column(name = "procedure_checks_alignment_json", columnDefinition = "TEXT")
    private String procedureChecksAlignmentJson;

    /**
     * F-194 SF-194-01 — alignement matérialisé des pièces manquantes (statuts avocat) de cette
     * analyse, avec propagation auto vers {@code case_deadlines} (source {@code PIECE_A_DEMANDER})
     * pour les pièces statut {@code A_DEMANDER}. Calculé une fois en fin de run de Synthèse
     * enrichie par {@code PieceManquanteAlignmentService.materializeForAnalysis}, APRÈS les
     * hooks F-192 et F-193.
     *
     * <p>Schéma : tableau d'objets
     * {@code [{"pieceLibelle":..., "statut":"A_DEMANDER|OBTENUE|NON_APPLICABLE",
     * "destinataire":..., "raisonNonApp":...}]}.
     *
     * <p>Cohérence F-92 STRICTE : ce champ est un overlay calculé, le JSON
     * {@code pieces_manquantes} dans {@code analysis_result} N'EST PAS modifié par
     * la matérialisation F-194 (à la différence de F-192/F-193 qui injectent des entrées
     * pieces_manquantes).</p>
     *
     * <p>{@code null} pour les analyses pré-F-194 ou les runs où la matérialisation a échoué
     * (fail-open) — l'endpoint {@code /pieces-manquantes-alignment} retourne {@code []} dans ce cas.</p>
     */
    @Column(name = "pieces_alignment_json", columnDefinition = "TEXT")
    private String piecesAlignmentJson;

    /**
     * F-195 SF-195-01 — alignement matérialisé des risques (statuts avocat trichotomiques)
     * de cette analyse, calculé une fois en fin de run de Synthèse enrichie par
     * {@code RisqueAlignmentService.materializeForAnalysis}, APRÈS les hooks F-192,
     * F-193 et F-194.
     *
     * <p>Schéma : tableau d'objets
     * {@code [{"risqueLibelle":..., "statut":"A_CREUSER|VALIDE|ECARTE",
     * "raisonEcarte":..., "toolIdsCibles":[...]}]}.
     *
     * <p>Cohérence F-IA-02 STRICTE : ce champ est un overlay calculé, le JSON
     * {@code risques} dans {@code analysis_result} N'EST PAS modifié par la
     * matérialisation F-195.</p>
     *
     * <p>{@code null} pour les analyses pré-F-195 ou les runs où la matérialisation a échoué
     * (fail-open) — l'endpoint {@code /risques-alignment} retourne {@code []} dans ce cas.</p>
     */
    @Column(name = "risques_alignment_json", columnDefinition = "TEXT")
    private String risquesAlignmentJson;

    /**
     * F-195 SF-195-01 — score risque recomputé excluant les risques ÉCARTÉ par
     * l'avocat (parallèle au {@code score_risque} IA brut). Le score IA brut
     * reste inchangé dans {@link #riskScore} / {@link #riskLevel} (cohérence
     * F-IA-02 stricte) ; F-195 produit un score "validé avocat" séparé pour
     * permettre à l'UI d'afficher les 2 valeurs (transparence).
     *
     * <p>Schéma : objet
     * {@code {"niveau":"FAIBLE|MOYEN|ELEVE","valeur":N,"totalRisques":N,
     * "risquesValides":N,"risquesEcartes":N,"risquesACreuser":N,
     * "scoreIaBrut":N,"niveauIaBrut":"..."}}.
     */
    @Column(name = "score_risque_avocat_json", columnDefinition = "TEXT")
    private String scoreRisqueAvocatJson;

    /**
     * F-197 SF-197-01 — Override avocat du {@code type_litige_detecte} IA pour les
     * dossiers Travail FR (7 enums LICENCIEMENT_SANS_CAUSE_REELLE / LICENCIEMENT_ECONOMIQUE
     * / PRISE_ACTE_RUPTURE / HARCELEMENT_MORAL / DISCRIMINATION / HEURES_SUPPLEMENTAIRES /
     * RAPPEL_SALAIRE).
     *
     * <p>Persistance pure (cohérence F-176). Effets matérialisés au prochain run de
     * Synthèse enrichie : (a) injecté dans le prompt enrichi section
     * {@code [Type litige fixé par l'avocat]}, (b) cloné automatiquement sur la
     * nouvelle analyse créée, (c) consommé par {@code DecisionToolVisibilityService}
     * en priorité sur la valeur IA brute pour la résolution F-IA-04.</p>
     */
    @Column(name = "type_litige_avocat_override", length = 50)
    private String typeLitigeAvocatOverride;

    /**
     * F-197 SF-197-01 — Override avocat du {@code type_procedure_detectee} IA pour les
     * dossiers Immigration (codes RENOUVELLEMENT_TITRE_SEJOUR / DEMANDE_ASILE_OFPRA /
     * RECOURS_CNDA / REGROUPEMENT_FAMILIAL / NATURALISATION_DECRET / CHANGEMENT_STATUT /
     * AES_SALARIE / REGULARISATION_EXCEPTIONNELLE / OQTF_AVEC_DELAI / OQTF_SANS_DELAI).
     *
     * <p>Mêmes règles que {@link #typeLitigeAvocatOverride} (PUT pur + matérialisation
     * au run suivant + clone automatique).</p>
     */
    @Column(name = "type_procedure_avocat_override", length = 50)
    private String typeProcedureAvocatOverride;

    /**
     * F-197 SF-197-01 — Raison libre justifiant l'override (optionnelle).
     */
    @Column(name = "type_override_raison", columnDefinition = "TEXT")
    private String typeOverrideRaison;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "faits_count")
    private Integer faitsCount;

    @Column(name = "points_juridiques_count")
    private Integer pointsJuridiquesCount;

    @Column(name = "risques_count")
    private Integer risquesCount;

    @Column(name = "questions_ouvertes_count")
    private Integer questionsOuvertesCount;

    @Column(name = "timeline_count")
    private Integer timelineCount;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}

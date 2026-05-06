package fr.ailegalcase.analysis;

/**
 * F-193 SF-193-01 — Mapping statique {@code critereCode → toolId} pour les
 * points procéduraux F-96, exécuté à la matérialisation par
 * {@link ProcedureCheckAlignmentService}.
 *
 * <p>Pattern miroir {@link RetainedPisteToolMatcher} (F-192 SF-192-01) mais
 * indexé sur {@code critereCode} au lieu de baseJuridique/keyword. Les
 * {@code critereCode} sont déjà transversaux dans F-96 (Travail / Immigration
 * / Famille × FR + BE), donc le mapping V1 couvre nativement les 3 domaines
 * × 2 pays sans nécessiter une stratégie de matching multi-niveaux.</p>
 *
 * <p>Tout {@code critereCode} non mappé renvoie {@code null} → cas
 * {@code NO_TARGET_TOOL} (fail-open).</p>
 *
 * <p>Les {@code critereCode} ci-dessous reprennent ceux référencés dans
 * {@code EnrichedAnalysisService} (prompt système IA F-96 / F-IA-03) :
 * <ul>
 *   <li>F-DT-08 Validité licenciement : {@code FR_*}, {@code BE_*}</li>
 *   <li>F-DT-10 Validité rupture conventionnelle : {@code RC_*}</li>
 *   <li>F-DT-09 Type de rupture : {@code DT09_TYPE_RUPTURE}</li>
 *   <li>F-FA-07 Checklist divorce : {@code FR_*} / {@code BE_*}</li>
 *   <li>F-IM-21 Validité dossier immigration : {@code IM21_*}</li>
 *   <li>F-IM-05 Titre de séjour : {@code IM05_MOTIF}</li>
 *   <li>F-IM-06 Recours : {@code IM06_RECOURS_TYPE}</li>
 *   <li>F-IM-07 Droit au travail : {@code IM07_TITRE_TYPE}</li>
 *   <li>F-FA-06 Calendrier garde : {@code FA06_MODE_GARDE}</li>
 *   <li>F-FA-05 Partage immobilier : {@code FA05_VALEUR_VENALE}, {@code FA05_CAPITAL_RESTANT}</li>
 * </ul>
 */
public final class ProcedureCheckToolMatcher {

    public static final String TOOL_DT_08_LICENCIEMENT = "F-DT-08-validite-licenciement";
    public static final String TOOL_DT_09_INDEMNITES = "F-DT-09-comparateur-indemnites";
    public static final String TOOL_DT_10_RUPTURE_CONV = "F-DT-10-rupture-conventionnelle";
    public static final String TOOL_FA_05_PARTAGE = "F-FA-05-partage-immobilier";
    public static final String TOOL_FA_06_GARDE = "F-FA-06-calendrier-garde";
    public static final String TOOL_FA_07_DIVORCE = "F-FA-07-checklist-divorce";
    public static final String TOOL_IM_05_TITRE = "F-IM-05-arbre-decisionnel-titre";
    public static final String TOOL_IM_06_RECOURS = "F-IM-06-recours";
    public static final String TOOL_IM_07_DROIT_TRAVAIL = "F-IM-07-droit-au-travail";
    public static final String TOOL_IM_21_VALIDITE = "F-IM-21-validite-dossier-immigration";

    private ProcedureCheckToolMatcher() {}

    /**
     * Renvoie l'identifiant TOOL_REGISTRY frontend pour le {@code critereCode}
     * donné, ou {@code null} si aucun mapping V1 n'est applicable (cas
     * {@code NO_TARGET_TOOL}).
     *
     * @param critereCode code de critère extrait de
     *                    {@link ProcedureCheck#getCritereCode()} (peut être {@code null})
     * @return toolId ou {@code null}
     */
    public static String resolveToolId(String critereCode) {
        if (critereCode == null) return null;
        String code = critereCode.trim().toUpperCase();
        if (code.isEmpty()) return null;

        // ── F-DT-08 Validité licenciement (FR + BE) ──────────────────────
        switch (code) {
            case "FR_CONVOCATION":
            case "FR_ENTRETIEN":
            case "FR_DELAI_NOTIFICATION":
            case "FR_MOTIVATION":
            case "FR_MOTIF_REEL":
            case "FR_PROCEDURE_DISCIPLINAIRE":
            case "FR_ORDRE_LICENCIEMENT":
            case "BE_NOTIFICATION":
            case "BE_PREAVIS":
            case "BE_MOTIVATION":
            case "BE_AUDITION":
            case "BE_NON_DISCRIMINATION":
            case "BE_PROTECTION_SPECIALE":
            case "BE_INDEMNITE_MANIFESTE":
                return TOOL_DT_08_LICENCIEMENT;

            // ── F-DT-09 Type de rupture (FR + BE) ────────────────────────
            case "DT09_TYPE_RUPTURE":
                return TOOL_DT_09_INDEMNITES;

            // ── F-DT-10 Validité rupture conventionnelle (FR) ────────────
            case "RC_CONSENTEMENT":
            case "RC_DELAI_RETRACTATION":
            case "RC_HOMOLOGATION":
            case "RC_ASSISTANCE":
            case "RC_INDEMNITE":
            case "RC_ENTRETIENS":
                return TOOL_DT_10_RUPTURE_CONV;

            // ── F-IM-05 Titre de séjour (FR + BE) ────────────────────────
            case "IM05_MOTIF":
                return TOOL_IM_05_TITRE;

            // ── F-IM-06 Recours immigration (FR + BE) ────────────────────
            case "IM06_RECOURS_TYPE":
                return TOOL_IM_06_RECOURS;

            // ── F-IM-07 Droit au travail (FR + BE) ───────────────────────
            case "IM07_TITRE_TYPE":
                return TOOL_IM_07_DROIT_TRAVAIL;

            // ── F-FA-05 Partage immobilier ───────────────────────────────
            case "FA05_VALEUR_VENALE":
            case "FA05_CAPITAL_RESTANT":
                return TOOL_FA_05_PARTAGE;

            // ── F-FA-06 Calendrier garde (FR + BE) ───────────────────────
            case "FA06_MODE_GARDE":
                return TOOL_FA_06_GARDE;

            // ── F-FA-07 Checklist divorce (FR + BE) ──────────────────────
            case "FR_CHOIX_AVOCATS":
            case "FR_REDACTION_CONVENTION":
            case "FR_ENVOI_LRAR":
            case "FR_DELAI_REFLEXION":
            case "FR_SIGNATURE_CONVENTION":
            case "FR_DEPOT_NOTAIRE":
            case "FR_ENREGISTREMENT":
            case "BE_CHOIX_AVOCAT":
            case "BE_REDACTION_CONVENTION":
            case "BE_REQUETE_CONJOINTE":
            case "BE_COMPARUTION":
            case "BE_JUGEMENT":
            case "BE_TRANSCRIPTION":
                return TOOL_FA_07_DIVORCE;

            default:
                // ── F-IM-21 Validité dossier immigration (FR + BE) ───────
                // 18 critères IM21_* — préfixe couvre FR et BE
                if (code.startsWith("IM21_")) return TOOL_IM_21_VALIDITE;
                return null;
        }
    }
}

package fr.ailegalcase.analysis;

import java.util.regex.Pattern;

/**
 * F-192 SF-192-01 — Mapping statique baseJuridique / keyword → toolId pour les pistes
 * stratégiques RETAINED, exécuté à la matérialisation par
 * {@link RetainedPisteAlignmentService}.
 *
 * <p>Stratégie ordre :
 * <ol>
 *   <li>Match exact / regex sur baseJuridique (CESEDA L.421 → titre, L.512 / R.776 → recours)</li>
 *   <li>Match keyword sur baseJuridique (référentiel BE 15/12/1980)</li>
 *   <li>Match keyword sur texte (RAPO / REP / recours hiérarchique → recours,
 *       passeport talent / VPF / carte résident → titre)</li>
 *   <li>Sinon {@code null} (cas {@code NO_TARGET_TOOL})</li>
 * </ol>
 *
 * <p>V1 = Immigration FR + BE seulement. Famille / Travail à venir V2 selon signal terrain
 * (cf. mini-spec SF-192-01 § Hors scope).</p>
 */
public final class RetainedPisteToolMatcher {

    public static final String TOOL_TITRE = "F-IM-05-arbre-decisionnel-titre";
    public static final String TOOL_RECOURS = "F-IM-06-recours";

    /** Articles CESEDA "Titre de séjour" (L.421-x) — mappent vers F-IM-05. */
    private static final Pattern CESEDA_TITRE = Pattern.compile(
            "(?i)\\bL\\.?\\s*42[1-9](?:-\\d+)?\\b");

    /** Articles CESEDA "Recours" (L.512, R.776, L.614 OQTF) — mappent vers F-IM-06. */
    private static final Pattern CESEDA_RECOURS = Pattern.compile(
            "(?i)\\b(?:L\\.?\\s*51[2-9](?:-\\d+)?|R\\.?\\s*77[6-9](?:-\\d+)?|L\\.?\\s*614(?:-\\d+)?|L\\.?\\s*532(?:-\\d+)?)\\b");

    /** Loi belge 15/12/1980 articles "recours" (39/x). */
    private static final Pattern BE_RECOURS = Pattern.compile(
            "(?i)\\b(?:art(?:icle)?\\s*)?39[/\\-]\\d+\\b");

    /** Texte mentionnant un recours administratif. */
    private static final Pattern RECOURS_KEYWORDS = Pattern.compile(
            "(?i)\\b(rapo|recours\\s+(?:gracieux|hi[eé]rarchique|contentieux|en\\s+exc[eè]s|administratif)|REP|CNDA|CGRA|CCE|tribunal\\s+administratif)\\b");

    /** Texte mentionnant un titre de séjour. */
    private static final Pattern TITRE_KEYWORDS = Pattern.compile(
            "(?i)\\b(passeport\\s*talent|VPF|vie\\s+priv[eé]e\\s+et\\s+familiale|carte\\s+de\\s+r[eé]sident|carte\\s+de\\s+s[eé]jour|titre\\s+de\\s+s[eé]jour|naturalisation|9bis|9ter|40bis|40ter)\\b");

    /** Indique un référentiel belge "loi du 15/12/1980". */
    private static final Pattern BE_LOI_15_12_1980 = Pattern.compile(
            "(?i)(loi\\s+du\\s+15[/\\-]?12[/\\-]?1980|15\\s+d[eé]cembre\\s+1980)");

    private RetainedPisteToolMatcher() {}

    /**
     * Renvoie l'identifiant TOOL_REGISTRY frontend pour la piste, ou {@code null}
     * si aucun mapping V1 n'est applicable (cas {@code NO_TARGET_TOOL}).
     *
     * @param texte         texte libre de la piste (jamais {@code null} en pratique mais tolérant)
     * @param baseJuridique référence légale extraite ({@code null}-tolérant)
     * @return toolId ou {@code null}
     */
    public static String resolveToolId(String texte, String baseJuridique) {
        // (1) baseJuridique exact / regex prioritaire
        if (baseJuridique != null && !baseJuridique.isBlank()) {
            if (CESEDA_RECOURS.matcher(baseJuridique).find()) return TOOL_RECOURS;
            if (CESEDA_TITRE.matcher(baseJuridique).find()) return TOOL_TITRE;
            if (BE_RECOURS.matcher(baseJuridique).find()) return TOOL_RECOURS;
            if (BE_LOI_15_12_1980.matcher(baseJuridique).find()) {
                // Loi BE 15/12/1980 sans préfixe d'article → discrimination keyword sur texte
                if (texte != null && RECOURS_KEYWORDS.matcher(texte).find()) return TOOL_RECOURS;
                return TOOL_TITRE;
            }
            // CESEDA seul (sans article identifié) → priorité keyword sur texte
            if (baseJuridique.toUpperCase().contains("CESEDA")) {
                if (texte != null && RECOURS_KEYWORDS.matcher(texte).find()) return TOOL_RECOURS;
                if (texte != null && TITRE_KEYWORDS.matcher(texte).find()) return TOOL_TITRE;
                return TOOL_TITRE; // défaut Immigration FR sans signal recours
            }
        }

        // (2) keyword sur texte
        if (texte != null && !texte.isBlank()) {
            if (RECOURS_KEYWORDS.matcher(texte).find()) return TOOL_RECOURS;
            if (TITRE_KEYWORDS.matcher(texte).find()) return TOOL_TITRE;
        }

        // (3) sinon — pas d'outil cible identifiable
        return null;
    }
}

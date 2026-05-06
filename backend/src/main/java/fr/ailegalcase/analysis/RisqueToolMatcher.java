package fr.ailegalcase.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * F-195 SF-195-01 — Mapping statique keyword → toolIds pour les risques,
 * exécuté à la matérialisation par {@link RisqueAlignmentService}.
 *
 * <p>V1 keyword-based. Mapping selon mini-spec :</p>
 * <ul>
 *   <li>Travail FR : "harcèlement" / "harcèlement moral" / "harcèlement sexuel"
 *       → F-DT-12 (harcèlement-licenciement-nul)</li>
 *   <li>Travail FR : "discrimination" → F-DT-13 (si existe) ou F-DT-08 (validité licenciement)</li>
 *   <li>Travail FR : "prescription échue" / "délai forclos" / "prescrit" → F-DT-03 (prescription)</li>
 *   <li>Travail FR : "clause non-concurrence" / "non-concurrence abusive" → F-DT-24 (non-concurrence)</li>
 *   <li>Immigration : "OQTF" / "expulsion" / "perte titre" → F-IM-08, F-IM-20</li>
 *   <li>Famille : "violence intra-familiale" / "violence conjugale" → F-FA-14 (ordonnance protection)</li>
 *   <li>Famille : "déplacement illicite enfant" / "déplacement enfant" → F-FA-19</li>
 *   <li>Famille : "dilapidation" / "dilapidation patrimoine" → F-FA-15</li>
 * </ul>
 *
 * <p>Un même risque peut matcher plusieurs outils (ex. OQTF → F-IM-08 + F-IM-20).
 * Si aucun keyword ne matche, retourne liste vide (cas {@code NO_TARGET_TOOL}).</p>
 *
 * <p>Pattern miroir {@link RetainedPisteToolMatcher} (F-192) +
 * {@link ProcedureCheckToolMatcher} (F-193). Différence : F-195 retourne
 * {@code List<String>} (un risque peut cibler plusieurs outils).</p>
 */
public final class RisqueToolMatcher {

    // ---- Travail FR ----
    public static final String TOOL_HARCELEMENT_NULLITE = "F-DT-12-harcelement-licenciement-nul";
    public static final String TOOL_DISCRIMINATION = "F-DT-13-discrimination";
    public static final String TOOL_LICENCIEMENT_VALIDITE = "F-DT-08-licenciement-validity";
    public static final String TOOL_PRESCRIPTION = "F-DT-03-prescription";
    public static final String TOOL_NON_CONCURRENCE = "F-DT-24-non-concurrence";

    // ---- Immigration ----
    public static final String TOOL_OQTF_AVEC_DELAI = "F-IM-08-oqtf-avec-delai";
    public static final String TOOL_OQTF_SANS_DELAI = "F-IM-20-oqtf-sans-delai";

    // ---- Famille ----
    public static final String TOOL_ORDONNANCE_PROTECTION = "F-FA-14-ordonnance-protection";
    public static final String TOOL_DEPLACEMENT_ENFANT = "F-FA-19-deplacement-illicite-enfant";
    public static final String TOOL_DILAPIDATION = "F-FA-15-mesures-urgentes-patrimoine";

    /** Harcèlement (moral, sexuel, agissements répétés). */
    private static final Pattern HARCELEMENT = Pattern.compile(
            "(?i)\\bharc[èeé]lement(\\s+(moral|sexuel|au\\s+travail))?\\b|\\bagissements\\s+r[ée]p[ée]t[ée]s\\b");

    /** Discrimination (origine, sexe, religion, etc.). */
    private static final Pattern DISCRIMINATION = Pattern.compile(
            "(?i)\\bdiscrimination(s)?\\b|\\bdiscriminatoire(s)?\\b");

    /** Prescription échue / délai forclos / action prescrite. */
    private static final Pattern PRESCRIPTION = Pattern.compile(
            "(?i)\\bprescription\\s+(?:[ée]chue|acquise|forclose)\\b"
                    + "|\\bd[ée]lai\\s+forclos\\b"
                    + "|\\bprescrit(e|s)?\\b"
                    + "|\\baction\\s+prescrite\\b"
                    + "|\\bforclusion\\b");

    /** Clause non-concurrence (abusive ou non). */
    private static final Pattern NON_CONCURRENCE = Pattern.compile(
            "(?i)\\bclause\\s+(de\\s+)?non[\\s-]concurrence\\b|\\bnon[\\s-]concurrence\\b");

    /** OQTF / expulsion / perte titre / éloignement. */
    private static final Pattern OQTF_EXPULSION = Pattern.compile(
            "(?i)\\bOQTF\\b|\\bexpuls(?:ion|er|able)\\b|\\b[ée]loignement\\b"
                    + "|\\bperte\\s+(du\\s+)?titre\\b|\\bobligation\\s+de\\s+quitter\\b");

    /** Violence intra-familiale / conjugale. */
    private static final Pattern VIOLENCE = Pattern.compile(
            "(?i)\\bviolence(s)?\\s+(intra[\\s-]?familial(e|es)?|conjugal(e|es)?|domestique(s)?)\\b"
                    + "|\\bviolence(s)?\\s+(physique|psychologique|sexuelle)(s)?\\b"
                    + "|\\bemprise\\b");

    /** Déplacement illicite d'enfant / enlèvement parental. */
    private static final Pattern DEPLACEMENT_ENFANT = Pattern.compile(
            "(?i)\\bd[ée]placement\\s+(illicite\\s+)?(d['e]\\s*)?enfant(s)?\\b"
                    + "|\\benl[èe]vement\\s+(parental|d['e]\\s*enfant(s)?)\\b"
                    + "|\\bnon[\\s-]repr[ée]sentation\\s+d['e]\\s*enfant\\b");

    /** Dilapidation patrimoine / actes de dispositions frauduleux. */
    private static final Pattern DILAPIDATION = Pattern.compile(
            "(?i)\\bdilapidation\\b|\\bd[ée]tournement\\s+(de\\s+)?patrimoine\\b"
                    + "|\\bactes?\\s+(de\\s+)?disposition(s)?\\s+frauduleux\\b");

    private RisqueToolMatcher() {}

    /**
     * Renvoie la liste des identifiants TOOL_REGISTRY frontend ciblés par le
     * risque (peut être vide = {@code NO_TARGET_TOOL}). Un même risque peut
     * cibler plusieurs outils (ex. OQTF → F-IM-08 + F-IM-20).
     *
     * @param texte texte libre du risque ({@code null}-tolérant)
     * @return liste (jamais {@code null}, possiblement vide) des toolIds cibles
     */
    public static List<String> resolveToolIds(String texte) {
        List<String> out = new ArrayList<>();
        if (texte == null || texte.isBlank()) return out;

        // Travail FR
        if (HARCELEMENT.matcher(texte).find()) {
            out.add(TOOL_HARCELEMENT_NULLITE);
        }
        if (DISCRIMINATION.matcher(texte).find()) {
            out.add(TOOL_DISCRIMINATION);
            // Aussi flag F-DT-08 (validité licenciement) pour discrimination — risque transversal
            if (!out.contains(TOOL_LICENCIEMENT_VALIDITE)) {
                out.add(TOOL_LICENCIEMENT_VALIDITE);
            }
        }
        if (PRESCRIPTION.matcher(texte).find()) {
            out.add(TOOL_PRESCRIPTION);
        }
        if (NON_CONCURRENCE.matcher(texte).find()) {
            out.add(TOOL_NON_CONCURRENCE);
        }

        // Immigration
        if (OQTF_EXPULSION.matcher(texte).find()) {
            out.add(TOOL_OQTF_AVEC_DELAI);
            out.add(TOOL_OQTF_SANS_DELAI);
        }

        // Famille
        if (VIOLENCE.matcher(texte).find()) {
            out.add(TOOL_ORDONNANCE_PROTECTION);
        }
        if (DEPLACEMENT_ENFANT.matcher(texte).find()) {
            out.add(TOOL_DEPLACEMENT_ENFANT);
        }
        if (DILAPIDATION.matcher(texte).find()) {
            out.add(TOOL_DILAPIDATION);
        }

        return out;
    }

    /**
     * Indique si un libellé de risque contient un keyword critique tile-side :
     * harcèlement / violence / expulsion / dilapidation. Utilisé par le
     * dashboard pour relever {@code alertLevel = ALERT} dès qu'un risque
     * VALIDÉ critique est présent.
     */
    public static boolean isCriticalKeyword(String texte) {
        if (texte == null || texte.isBlank()) return false;
        return HARCELEMENT.matcher(texte).find()
                || VIOLENCE.matcher(texte).find()
                || OQTF_EXPULSION.matcher(texte).find()
                || DILAPIDATION.matcher(texte).find();
    }
}

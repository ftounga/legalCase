package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-04 : moteur décisionnel BE cadrant l'<b>établissement de la filiation
 * après une gestation pour autrui (GPA)</b>, dans un contexte de <b>vide
 * juridique</b> belge (absence de loi spécifique GPA — ni autorisée ni
 * pénalement interdite, à vérifier par avocat belge). La convention de GPA
 * n'est pas opposable ; la filiation s'établit par les voies de droit commun.
 *
 * <p><b>Arbre, pas calcul.</b> L'outil oriente vers les voies d'établissement
 * de la filiation (reconnaissance, adoption après naissance, reconnaissance de
 * l'acte étranger) sans citer de jurisprudence (vide juridique BE — F-JU-04
 * parké, silence &gt; erreur).</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la situation cadrée est
 * l'établissement de la filiation post-GPA. DISTINCT de {@code adoption-be}
 * (l'adoption peut être <i>une voie</i> du verdict, pas la situation cadrée) et
 * de {@code contestation-filiation-be} (F-217). Ne tranche pas l'aspect
 * international (renvoi {@code dip-be-loi-applicable-famille} /
 * {@code dip-be-reconnaissance-decision-etrangere} si GPA à l'étranger).</p>
 *
 * <p>Logique du verdict (4 niveaux) :</p>
 * <ul>
 *   <li><b>RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE</b> — GPA à l'étranger avec
 *       acte de naissance étranger établi : renvoi vers la reconnaissance de
 *       l'acte / la loi applicable (DIP).</li>
 *   <li><b>FILIATION_PAR_RECONNAISSANCE</b> — le parent intentionnel a un lien
 *       génétique avec l'enfant (au moins le père) : reconnaissance possible
 *       par les voies de droit commun.</li>
 *   <li><b>FILIATION_PAR_ADOPTION_POST_NAISSANCE</b> — parent intentionnel sans
 *       lien génétique : voie de l'adoption après la naissance (renvoi
 *       {@code adoption-be}).</li>
 *   <li><b>QUALIFICATION_INCOMPLETE</b> — éléments insuffisants pour orienter.</li>
 * </ul>
 *
 * <p><b>Validation juridique requise</b> : les bases citées (CC mater semper
 * certa, reconnaissance, adoption) sont à <b>valider par un avocat belge avant
 * production</b> — vide juridique GPA + renumérotation CC post-réformes
 * 2017-2019. Aucune citation jurisprudentielle (F-JU-04 parké).</p>
 */
public final class GpaBeCalculator {

    /** Lieu où la GPA a été réalisée. */
    public enum LieuGpa {
        BELGIQUE,
        ETRANGER
    }

    /** Lien génétique entre le ou les parents intentionnels et l'enfant. */
    public enum LienGenetique {
        PERE_INTENTIONNEL,
        MERE_INTENTIONNELLE,
        AUCUN,
        LES_DEUX
    }

    /** Verdict de l'analyse (4 niveaux — arbre filiation post-GPA). */
    public enum GpaBeVerdict {
        FILIATION_PAR_RECONNAISSANCE,
        FILIATION_PAR_ADOPTION_POST_NAISSANCE,
        RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE,
        QUALIFICATION_INCOMPLETE
    }

    private static final int COMMENTAIRE_MAX = 1000;

    private GpaBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE d'établissement de la filiation post-GPA.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, chemin contentieux, risques, bases
     *         juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static GpaBeResult compute(GpaBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — filiation post-GPA");
        }
        validateInputs(input);

        // Message commun : la convention de GPA n'est pas opposable + la mère
        // qui accouche est la mère en droit belge (mater semper certa).
        final String inopposabilite =
                "La convention de gestation pour autrui n'est pas opposable en droit belge (vide "
                        + "juridique — à vérifier par avocat belge) : la mère qui accouche (mère porteuse) "
                        + "est la mère en droit (principe mater semper certa — CC, à vérifier). La filiation "
                        + "à l'égard du ou des parents intentionnels s'établit par les voies de droit commun.";

        // --- (d) GPA à l'étranger avec acte de naissance étranger établi ---
        // → reconnaissance / loi applicable de l'acte étranger (DIP).
        boolean acteEtranger = Boolean.TRUE.equals(input.acteNaissanceEtrangerEtabli());
        if (input.gpaRealiseeEnBelgiqueOuEtranger() == LieuGpa.ETRANGER && acteEtranger) {
            List<String> chemin = List.of(
                    "Instruire la reconnaissance de l'acte de naissance étranger en Belgique et la loi "
                            + "applicable à la filiation (voies de DIP) avant toute action de droit interne.",
                    "Apprécier la conformité de l'acte étranger à l'ordre public international belge "
                            + "(la GPA n'étant ni autorisée ni interdite — à vérifier par avocat belge).");
            List<String> risques = List.of(
                    "Le refus de transcription / reconnaissance de l'acte étranger reste possible au "
                            + "titre de l'ordre public : prévoir une voie subsidiaire (reconnaissance par le "
                            + "parent génétique, adoption après naissance).",
                    inopposabilite);
            return new GpaBeResult(
                    GpaBeVerdict.RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE,
                    chemin,
                    risques,
                    basesJuridiques(),
                    List.of("GPA réalisée à l'étranger avec acte de naissance étranger : la voie première "
                            + "est l'instruction de la reconnaissance de l'acte / de la loi applicable via "
                            + "les outils de droit international privé (dip-be-reconnaissance-decision-etrangere, "
                            + "dip-be-loi-applicable-famille). " + inopposabilite));
        }

        // --- (b) Lien génétique du parent intentionnel → reconnaissance ---
        LienGenetique lien = input.lienGenetiqueParentIntentionnel();
        boolean lienGenetique = lien == LienGenetique.PERE_INTENTIONNEL
                || lien == LienGenetique.MERE_INTENTIONNELLE
                || lien == LienGenetique.LES_DEUX;
        if (lienGenetique) {
            List<String> chemin = new ArrayList<>();
            chemin.add("Établir la filiation du parent intentionnel disposant d'un lien génétique par "
                    + "la voie de la reconnaissance (généralement le père intentionnel), une fois la "
                    + "filiation maternelle de la mère porteuse écartée / réglée (CC — à vérifier).");
            chemin.add("Pour le second parent intentionnel dépourvu de lien génétique, prévoir une "
                    + "adoption après la naissance (voir l'outil adoption-be) une fois la filiation du "
                    + "parent génétique établie.");
            List<String> risques = List.of(
                    "La filiation de la mère porteuse (mater semper certa) doit être préalablement "
                            + "réglée (absence de reconnaissance / contestation — voir contestation-filiation-be).",
                    inopposabilite);
            return new GpaBeResult(
                    GpaBeVerdict.FILIATION_PAR_RECONNAISSANCE,
                    chemin,
                    risques,
                    basesJuridiques(),
                    List.of("Un parent intentionnel a un lien génétique avec l'enfant : la filiation peut "
                            + "s'établir par reconnaissance (voie de droit commun), le second parent passant "
                            + "le cas échéant par l'adoption après naissance (adoption-be). " + inopposabilite));
        }

        // --- (c) Aucun lien génétique → adoption après naissance ---
        if (lien == LienGenetique.AUCUN) {
            List<String> chemin = List.of(
                    "Établir la filiation des parents intentionnels par la voie de l'adoption après la "
                            + "naissance de l'enfant (voir l'outil adoption-be — recevabilité de l'adoption).",
                    "Vérifier au préalable le consentement de la mère porteuse à l'adoption et le "
                            + "respect des délais et conditions de l'adoption belge (CC — à vérifier).");
            List<String> risques = List.of(
                    "L'adoption suppose le consentement de la mère porteuse (mère en droit) : un refus "
                            + "fait obstacle à l'établissement de la filiation intentionnelle.",
                    inopposabilite);
            return new GpaBeResult(
                    GpaBeVerdict.FILIATION_PAR_ADOPTION_POST_NAISSANCE,
                    chemin,
                    risques,
                    basesJuridiques(),
                    List.of("Aucun parent intentionnel n'a de lien génétique avec l'enfant : la voie "
                            + "d'établissement de la filiation est l'adoption après naissance "
                            + "(adoption-be). " + inopposabilite));
        }

        // --- Sécurité : qualification incomplète (théoriquement inatteignable) ---
        return new GpaBeResult(
                GpaBeVerdict.QUALIFICATION_INCOMPLETE,
                List.of("Compléter les éléments (lieu de la GPA, lien génétique, acte de naissance) "
                        + "pour orienter l'établissement de la filiation."),
                List.of(inopposabilite),
                basesJuridiques(),
                List.of("Éléments insuffisants pour qualifier le chemin d'établissement de la "
                        + "filiation post-GPA. " + inopposabilite));
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(GpaBeInput in) {
        if (in.gpaRealiseeEnBelgiqueOuEtranger() == null) {
            throw new IllegalArgumentException(
                    "Le lieu de réalisation de la GPA est requis (BELGIQUE / ETRANGER)");
        }
        if (in.lienGenetiqueParentIntentionnel() == null) {
            throw new IllegalArgumentException(
                    "Le lien génétique du ou des parents intentionnels est requis "
                            + "(PERE_INTENTIONNEL / MERE_INTENTIONNELLE / AUCUN / LES_DEUX)");
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "Vide juridique GPA en Belgique (ni autorisée ni pénalement interdite) (à vérifier "
                        + "par avocat belge) — la convention de GPA n'est pas opposable",
                "Principe mater semper certa (CC — à vérifier) — la mère qui accouche est la mère "
                        + "en droit ; la filiation maternelle intentionnelle ne s'établit pas de plein droit",
                "Reconnaissance et adoption (CC — à vérifier, renumérotation post-réformes 2017-2019) "
                        + "— voies de droit commun d'établissement de la filiation à l'égard des parents "
                        + "intentionnels",
                "Droit international privé (renvoi dip-be-reconnaissance-decision-etrangere / "
                        + "dip-be-loi-applicable-famille) — reconnaissance de l'acte de naissance étranger "
                        + "si la GPA a été réalisée à l'étranger");
    }
}

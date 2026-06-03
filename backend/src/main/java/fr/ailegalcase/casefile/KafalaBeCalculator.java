package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-03 : moteur décisionnel BE qualifiant le sort d'une <b>kafala (recueil
 * légal)</b> prononcée à l'étranger (CDIP — loi du 16/07/2004 ; Code civil belge
 * art. 343 al. 2 nouveau — à vérifier par avocat belge, renumérotation CC
 * post-réformes 2017-2019).
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la kafala n'est PAS une
 * adoption : elle ne crée aucun lien de filiation, c'est une mesure de
 * recueil / protection. L'outil est donc DISTINCT de {@code adoption-be}
 * (recevabilité de l'adoption) et de {@code mariage-etranger-be-reconnaissance}
 * (acte de famille de nature différente). Il ne tranche pas le séjour de
 * l'enfant (immigration F-IM-*) ni la succession ({@code dip-be-loi-applicable-famille}).</p>
 *
 * <p>Logique du verdict :</p>
 * <ul>
 *   <li><b>EFFET_ADOPTIF_EXCLU</b> — l'objectif visé est une adoption
 *       ({@code TENTATIVE_ADOPTION}) : la kafala ne crée pas de lien de
 *       filiation (CC art. 343 al. 2 — exclusion de l'adoption-kafala). Le
 *       client est orienté vers {@code adoption-be} ou la délégation
 *       d'autorité parentale.</li>
 *   <li><b>RECUEIL_RECONNU_COMME_PROTECTION</b> — acte officiel (judiciaire ou
 *       adoulaire) + conformité à l'ordre public belge : reconnaissance comme
 *       mesure de recueil / protection via le CDIP, avec effets dérivés
 *       possibles (délégation d'autorité parentale, tutelle officieuse).</li>
 *   <li><b>RECONNAISSANCE_SOUS_CONDITIONS</b> — acte non officiel, ou éléments
 *       à compléter avant de pouvoir se prévaloir du recueil en Belgique.</li>
 *   <li><b>QUALIFICATION_INCOMPLETE</b> — éléments insuffisants pour qualifier.</li>
 * </ul>
 *
 * <p><b>Validation juridique requise</b> : les bases citées (CDIP, CC art. 343
 * al. 2) sont à <b>valider par un avocat belge avant production</b>. Aucune
 * citation jurisprudentielle (F-JU-04 parké).</p>
 */
public final class KafalaBeCalculator {

    /** Objectif que l'avocat envisage pour la kafala en Belgique. */
    public enum ObjectifEnvisage {
        RECUEIL_PROTECTION,
        TENTATIVE_ADOPTION,
        SEJOUR,
        SUCCESSION
    }

    /** Verdict de l'analyse (4 niveaux). */
    public enum KafalaBeVerdict {
        RECUEIL_RECONNU_COMME_PROTECTION,
        RECONNAISSANCE_SOUS_CONDITIONS,
        EFFET_ADOPTIF_EXCLU,
        QUALIFICATION_INCOMPLETE
    }

    private static final int COMMENTAIRE_MAX = 1000;

    private KafalaBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE de qualification du recueil légal (kafala)
     * sur les éléments saisis.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, motifs/conditions, actes à produire,
     *         bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static KafalaBeResult compute(KafalaBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — recueil légal (kafala)");
        }
        validateInputs(input);

        // --- (a) Objectif adoption → effet adoptif exclu (CC art. 343 al. 2) ---
        if (input.objectifEnvisage() == ObjectifEnvisage.TENTATIVE_ADOPTION) {
            List<String> motifs = List.of(
                    "La kafala (recueil légal) ne crée aucun lien de filiation : elle ne peut être "
                            + "convertie en adoption (CC art. 343 al. 2 nouveau — à vérifier par avocat "
                            + "belge, renumérotation CC post-réformes 2017-2019).",
                    "Le recueil légal confère une mesure de protection / prise en charge de l'enfant, "
                            + "non un rapport de filiation adoptif.");
            List<String> actes = List.of(
                    "Réorienter le projet vers l'outil de recevabilité de l'adoption (adoption-be) si "
                            + "une adoption est réellement souhaitée, en vérifiant les conditions propres "
                            + "à l'adoption belge.",
                    "À défaut d'adoption, envisager une délégation d'autorité parentale ou une tutelle "
                            + "officieuse au profit du kafil pour sécuriser la prise en charge en Belgique.");
            return new KafalaBeResult(
                    KafalaBeVerdict.EFFET_ADOPTIF_EXCLU,
                    motifs,
                    actes,
                    basesJuridiques(),
                    List.of("La voie de l'adoption à partir d'une kafala est exclue (CC art. 343 al. 2 "
                            + "— à vérifier). La kafala reste reconnaissable comme mesure de recueil / "
                            + "protection (voir l'outil adoption-be pour une éventuelle adoption distincte)."));
        }

        // --- (b) Conditions de reconnaissance comme mesure de recueil / protection ---
        List<String> conditions = new ArrayList<>();
        boolean acteOfficiel = Boolean.TRUE.equals(input.acteOfficielJudiciaireOuAdoul());
        boolean consentement = Boolean.TRUE.equals(input.consentementParentsBiologiquesOuAutorite());

        if (!acteOfficiel) {
            conditions.add(
                    "Produire un acte de kafala officiel (décision judiciaire ou acte adoulaire / "
                            + "notarial dûment établi) : un recueil purement privé ou non officiel ne peut "
                            + "être reconnu via le CDIP (loi du 16/07/2004 — à vérifier).");
        }
        if (!consentement) {
            conditions.add(
                    "Justifier du consentement des parents biologiques ou de la décision de l'autorité "
                            + "compétente du pays d'origine (notamment pour un enfant abandonné ou orphelin), "
                            + "condition de conformité à l'ordre public belge (CDIP — à vérifier).");
        }

        // --- Effets dérivés et renvois (toujours fournis) ---
        List<String> actes = new ArrayList<>();
        if (acteOfficiel && consentement) {
            actes.add("Faire valoir le recueil légal comme mesure de protection via le CDIP "
                    + "(art. 22 et s. relatifs à l'autorité parentale et à la protection de l'enfant "
                    + "— à vérifier), en produisant l'acte de kafala traduit et légalisé / apostillé.");
            actes.add("Solliciter, le cas échéant, une délégation d'autorité parentale ou une tutelle "
                    + "officieuse au profit du kafil pour exercer les actes de la vie courante de l'enfant "
                    + "en Belgique.");
        } else {
            actes.add("Compléter le dossier (acte officiel, consentements) avant de se prévaloir du "
                    + "recueil légal devant les autorités belges.");
        }
        // Renvois explicites séjour (immigration) + succession.
        actes.add("Pour le séjour de l'enfant kafil en Belgique, saisir les outils d'immigration "
                + "dédiés (la kafala ne règle pas, par elle-même, le droit au séjour).");
        actes.add("Pour les effets successoraux (la kafala n'ouvre pas de vocation héréditaire de "
                + "plein droit), se reporter à l'outil de loi applicable en matière familiale "
                + "(dip-be-loi-applicable-famille) et à un éventuel testament.");

        if (!conditions.isEmpty()) {
            return new KafalaBeResult(
                    KafalaBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS,
                    conditions,
                    actes,
                    basesJuridiques(),
                    List.of("Le recueil légal pourra être reconnu en Belgique comme mesure de protection "
                            + "sous réserve des conditions ci-dessus (acte officiel / consentements — CDIP ; "
                            + "CC art. 343 al. 2 — à vérifier). L'effet adoptif reste exclu."));
        }

        // --- (c) Recueil reconnu comme mesure de protection ---
        return new KafalaBeResult(
                KafalaBeVerdict.RECUEIL_RECONNU_COMME_PROTECTION,
                List.of(),
                actes,
                basesJuridiques(),
                List.of("Acte de kafala officiel et conditions de conformité à l'ordre public belge "
                        + "réunies : le recueil légal est reconnaissable comme mesure de recueil / "
                        + "protection via le CDIP (CC art. 343 al. 2 — à vérifier). Effets dérivés "
                        + "possibles (délégation d'autorité parentale, tutelle officieuse). L'effet "
                        + "adoptif reste exclu."));
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(KafalaBeInput in) {
        if (in.objectifEnvisage() == null) {
            throw new IllegalArgumentException(
                    "L'objectif envisagé est requis "
                            + "(RECUEIL_PROTECTION / TENTATIVE_ADOPTION / SEJOUR / SUCCESSION)");
        }
        if (in.paysOrigineKafala() != null
                && !in.paysOrigineKafala().matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException(
                    "Le pays d'origine de la kafala doit être un code ISO 3166-1 alpha-2 (ex. MA, DZ)");
        }
        if (in.dateActeKafala() != null && in.dateActeKafala().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de l'acte de kafala ne peut être future");
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
                "Code de droit international privé (CDIP — loi du 16/07/2004) (à vérifier) — "
                        + "reconnaissance des décisions et actes étrangers / ordre public",
                "CC art. 343 al. 2 nouveau (à vérifier) — exclusion de l'adoption-kafala, "
                        + "admission du recueil légal via le DIP",
                "CDIP — autorité parentale et protection de l'enfant (à vérifier) — "
                        + "effets dérivés du recueil (délégation d'autorité parentale, tutelle officieuse)");
    }
}

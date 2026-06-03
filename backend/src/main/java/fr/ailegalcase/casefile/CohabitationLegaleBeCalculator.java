package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-01 : moteur décisionnel BE du régime de la <b>cohabitation légale</b>
 * (loi du 23/11/1998 ; Code civil belge art. 1475-1479 — à vérifier par avocat
 * belge, renumérotation CC post-réformes 2017-2019).
 *
 * <p>Outil multi-vues unique (1 outil = 1 situation « régime de la cohabitation
 * légale ») couvrant :</p>
 * <ul>
 *   <li><b>FORMATION</b> — conditions de la déclaration à l'officier de l'état
 *       civil (CC art. 1475 — deux personnes capables, non mariées, non déjà
 *       liées par une autre cohabitation légale) ;</li>
 *   <li><b>EFFETS</b> — protection du logement familial commun (CC art. 1477)
 *       et contribution aux charges proportionnelle ;</li>
 *   <li><b>DISSOLUTION</b> — modalités de fin (CC art. 1476 — déclaration
 *       commune / déclaration unilatérale signifiée par huissier / mariage de
 *       l'un / décès).</li>
 * </ul>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. ≠ PACS français (F-FA-12, structurellement
 * distinct) et ≠ cohabitation de fait (P4 F-224). Outil bâti depuis les sources
 * belges ({@code feedback_belgique_never_forget}).</p>
 *
 * <p><b>Validation juridique requise</b> : les articles cités (CC art. 1475-1479,
 * loi du 23/11/1998) sont à <b>valider par un avocat belge avant production</b> —
 * renumérotation du Code civil post-réformes 2017-2019.</p>
 */
public final class CohabitationLegaleBeCalculator {

    /** Vue analysée. */
    public enum VueCohabitationLegaleBe {
        FORMATION,
        EFFETS,
        DISSOLUTION
    }

    /** Verdict de l'analyse. */
    public enum CohabitationLegaleBeVerdict {
        FORMATION_VALIDE,
        FORMATION_IMPOSSIBLE,
        EFFETS_QUALIFIES,
        DISSOLUTION_QUALIFIEE
    }

    /** Mode de dissolution envisagé (CC art. 1476). */
    public enum ModeDissolutionCohabitationLegaleBe {
        DECLARATION_COMMUNE,
        DECLARATION_UNILATERALE,
        MARIAGE,
        DECES
    }

    private static final int COMMENTAIRE_MAX = 1000;

    private CohabitationLegaleBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE de la cohabitation légale sur les éléments
     * saisis, selon la vue choisie.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, conditions/motifs, actes à produire,
     *         bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static CohabitationLegaleBeResult compute(
            CohabitationLegaleBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — régime de la cohabitation légale");
        }
        validateInputs(input);

        return switch (input.vue()) {
            case FORMATION -> resultFormation(input);
            case EFFETS -> resultEffets(input);
            case DISSOLUTION -> resultDissolution(input);
        };
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(CohabitationLegaleBeInput in) {
        if (in.vue() == null) {
            throw new IllegalArgumentException("La vue d'analyse est requise (FORMATION / EFFETS / DISSOLUTION)");
        }
        if (in.vue() == VueCohabitationLegaleBe.DISSOLUTION && in.modeDissolutionEnvisage() == null) {
            throw new IllegalArgumentException(
                    "Le mode de dissolution est requis pour la vue DISSOLUTION");
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    // ---------------------------------------------------------------
    // Vue FORMATION (CC art. 1475)
    // ---------------------------------------------------------------

    private static CohabitationLegaleBeResult resultFormation(CohabitationLegaleBeInput in) {
        List<String> conditionsManquantes = new ArrayList<>();
        if (!Boolean.TRUE.equals(in.deuxPersonnesNonMariees())) {
            conditionsManquantes.add("Les deux personnes doivent ne pas être mariées (CC art. 1475 — à vérifier).");
        }
        if (!Boolean.TRUE.equals(in.capaciteJuridique())) {
            conditionsManquantes.add("Les deux personnes doivent être capables de contracter (CC art. 1475 — à vérifier).");
        }
        if (!Boolean.TRUE.equals(in.pasDejaLieParMariageOuAutreCohabitation())) {
            conditionsManquantes.add(
                    "Aucune des personnes ne doit être déjà liée par un mariage ou une autre "
                            + "cohabitation légale (CC art. 1475 — à vérifier).");
        }

        if (!conditionsManquantes.isEmpty()) {
            List<String> actes = List.of(
                    "Informer le client que la déclaration de cohabitation légale ne peut pas être "
                            + "enregistrée tant que les conditions de l'art. 1475 CC ne sont pas réunies.",
                    "Vérifier l'état civil et la capacité juridique des deux partenaires avant toute "
                            + "démarche auprès de l'officier de l'état civil.");
            return new CohabitationLegaleBeResult(
                    CohabitationLegaleBeVerdict.FORMATION_IMPOSSIBLE,
                    conditionsManquantes,
                    actes,
                    basesJuridiques(),
                    List.of("Conditions de formation de la cohabitation légale non réunies (CC art. 1475 "
                            + "— à vérifier). La déclaration commune devant l'officier de l'état civil ne "
                            + "produira pas d'effet."));
        }

        List<String> actes = List.of(
                "Préparer la déclaration de cohabitation légale à déposer en personne devant l'officier "
                        + "de l'état civil du domicile commun (CC art. 1476 — à vérifier).",
                "Joindre les pièces d'identité des deux partenaires et la preuve du domicile commun.",
                "Le cas échéant, conseiller la rédaction d'une convention de cohabitation légale par acte "
                        + "notarié pour organiser les effets patrimoniaux (CC art. 1478 — à vérifier).");
        return new CohabitationLegaleBeResult(
                CohabitationLegaleBeVerdict.FORMATION_VALIDE,
                List.of(),
                actes,
                basesJuridiques(),
                List.of("Conditions de l'art. 1475 CC (à vérifier) réunies : la cohabitation légale peut "
                        + "être formée par déclaration commune devant l'officier de l'état civil."));
    }

    // ---------------------------------------------------------------
    // Vue EFFETS (CC art. 1477)
    // ---------------------------------------------------------------

    private static CohabitationLegaleBeResult resultEffets(CohabitationLegaleBeInput in) {
        List<String> conditions = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        if (Boolean.TRUE.equals(in.logementFamilialEnJeu())) {
            conditions.add("Protection du logement familial commun et de son mobilier : aucun des "
                    + "cohabitants ne peut disposer seul du logement servant à la résidence commune "
                    + "(CC art. 1477 — à vérifier).");
            messages.add("Le logement familial commun est protégé (CC art. 1477 — à vérifier) : les actes "
                    + "de disposition requièrent l'accord des deux cohabitants.");
        } else {
            messages.add("Aucun logement familial commun n'est signalé en jeu — la protection de l'art. "
                    + "1477 CC ne s'applique pas en l'espèce (à vérifier).");
        }
        if (Boolean.TRUE.equals(in.domicileCommun())) {
            conditions.add("Contribution aux charges de la vie commune en proportion des facultés de "
                    + "chacun (CC art. 1477 — à vérifier).");
        }

        List<String> actes = List.of(
                "Documenter le domicile commun et la consistance du logement familial pour la mise en "
                        + "œuvre de la protection de l'art. 1477 CC.",
                "Le cas échéant, conseiller une convention de cohabitation légale notariée pour préciser "
                        + "la contribution aux charges (CC art. 1478 — à vérifier).");
        return new CohabitationLegaleBeResult(
                CohabitationLegaleBeVerdict.EFFETS_QUALIFIES,
                conditions,
                actes,
                basesJuridiques(),
                messages);
    }

    // ---------------------------------------------------------------
    // Vue DISSOLUTION (CC art. 1476)
    // ---------------------------------------------------------------

    private static CohabitationLegaleBeResult resultDissolution(CohabitationLegaleBeInput in) {
        List<String> conditions = new ArrayList<>();
        List<String> actes = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        switch (in.modeDissolutionEnvisage()) {
            case DECLARATION_COMMUNE -> {
                conditions.add("Dissolution par déclaration commune des deux cohabitants (CC art. 1476 "
                        + "— à vérifier).");
                actes.add("Rédiger et déposer la déclaration commune de fin de cohabitation légale devant "
                        + "l'officier de l'état civil du domicile commun.");
                messages.add("La dissolution par déclaration commune est immédiate à compter de son "
                        + "enregistrement par l'officier de l'état civil (CC art. 1476 — à vérifier).");
            }
            case DECLARATION_UNILATERALE -> {
                conditions.add("Dissolution par déclaration unilatérale d'un seul cohabitant (CC art. 1476 "
                        + "— à vérifier).");
                actes.add("Rédiger la déclaration unilatérale de fin de cohabitation légale et la déposer "
                        + "devant l'officier de l'état civil.");
                actes.add("Faire signifier la déclaration à l'autre cohabitant par exploit d'huissier de "
                        + "justice (CC art. 1476 — à vérifier) ; provisionner les frais de signification.");
                messages.add("La déclaration unilatérale doit être signifiée par huissier à l'autre "
                        + "cohabitant pour produire ses effets (CC art. 1476 — à vérifier).");
            }
            case MARIAGE -> {
                conditions.add("Dissolution de plein droit par le mariage de l'un des cohabitants "
                        + "(CC art. 1476 — à vérifier).");
                actes.add("Constater la dissolution automatique de la cohabitation légale à la date du "
                        + "mariage ; aucune déclaration de dissolution distincte n'est requise.");
                messages.add("Le mariage de l'un des cohabitants met fin de plein droit à la cohabitation "
                        + "légale (CC art. 1476 — à vérifier).");
            }
            case DECES -> {
                conditions.add("Dissolution de plein droit par le décès de l'un des cohabitants "
                        + "(CC art. 1476 — à vérifier).");
                actes.add("Constater la dissolution à la date du décès et examiner les droits successoraux "
                        + "du cohabitant survivant (notamment l'usufruit sur le logement familial — "
                        + "CC art. 745octies — à vérifier).");
                messages.add("Le décès de l'un des cohabitants met fin de plein droit à la cohabitation "
                        + "légale (CC art. 1476 — à vérifier).");
            }
        }

        return new CohabitationLegaleBeResult(
                CohabitationLegaleBeVerdict.DISSOLUTION_QUALIFIEE,
                conditions,
                actes,
                basesJuridiques(),
                messages);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "Loi du 23/11/1998 instaurant la cohabitation légale (à vérifier)",
                "CC art. 1475 (à vérifier) — conditions de formation de la cohabitation légale",
                "CC art. 1476 (à vérifier) — dissolution (déclaration commune / unilatérale / mariage / décès)",
                "CC art. 1477 (à vérifier) — protection du logement familial commun et contribution aux charges",
                "CC art. 1478 (à vérifier) — convention de cohabitation légale par acte notarié",
                "CC art. 1479 (à vérifier) — mesures urgentes et provisoires en cas de mésentente");
    }
}

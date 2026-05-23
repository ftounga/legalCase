package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-29 : calculateur statique pour la donation-partage en droit
 * français (art. 1075 à 1075-5 Cciv + art. 1078, 1078-1, 1080 Cciv +
 * art. 912-928 Cciv).
 *
 * <p>Le calculateur évalue :</p>
 * <ul>
 *   <li>les <strong>conditions de validité</strong> de la donation-partage
 *       (au moins un descendant ; cas petits-enfants par substitution
 *       art. 1075-1 nécessite consentement du descendant intermédiaire) ;</li>
 *   <li>le <strong>gel de valeur</strong> au jour de la donation
 *       (art. 1078 Cciv) — neutralisation de la fluctuation des biens
 *       jusqu'au décès ;</li>
 *   <li>l'<strong>exclusion du rapport successoral</strong> (art. 1075-3
 *       Cciv) — pas de rapport à la masse à partager ;</li>
 *   <li>le caractère <strong>conjonctif</strong> ou non du partage
 *       (art. 1075-2 Cciv) — donation conjointe des deux parents ;</li>
 *   <li>la possibilité de <strong>réincorporer</strong> des donations
 *       antérieures pour équilibrer (art. 1078-1 Cciv) ;</li>
 *   <li>l'<strong>alerte quotité disponible</strong> en cas d'atteinte
 *       à la réserve héréditaire (art. 912-928 Cciv).</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la donation-partage
 * est régie par les art. 4.243 et suivants CC BE (outil
 * F-FA-BE-DONATION-PARTAGE futur, hors périmètre F-216).</p>
 */
public final class DonationPartageCalculator {

    static final String BASE_JURIDIQUE =
            "art. 1075 à 1075-5 Cciv (donation-partage) + art. 1078 Cciv "
                    + "(gel de valeur au jour de la donation) + art. 1078-1 "
                    + "Cciv (réincorporation de donations antérieures) + "
                    + "art. 1080 Cciv (quasi-usufruit en donation-partage) + "
                    + "art. 912-928 Cciv (réserve héréditaire / réduction)";

    private DonationPartageCalculator() {}

    /**
     * Évalue la donation-partage et émet le verdict + les alertes.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE} ou si
     *                                  les invariants métier sont violés.
     */
    public static DonationPartageResult compute(
            DonationPartageRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-DONATION-PARTAGE applicable uniquement en "
                            + "France (art. 1075 à 1075-5 Cciv).");
        }
        if (req == null) {
            throw new IllegalArgumentException("Requête manquante.");
        }
        if (req.nombreDescendants() == null || req.nombreDescendants() < 1) {
            throw new IllegalArgumentException(
                    "nombreDescendants doit être >= 1 (art. 1075 Cciv — "
                            + "donation-partage suppose au moins un descendant "
                            + "présomptif).");
        }
        if (req.valeurPartageTotal() != null && req.valeurPartageTotal() < 0) {
            throw new IllegalArgumentException("valeurPartageTotal doit être >= 0.");
        }
        if (req.agesDonateurs() != null) {
            for (Integer age : req.agesDonateurs()) {
                if (age != null && age < 0) {
                    throw new IllegalArgumentException(
                            "agesDonateurs : âges négatifs interdits.");
                }
            }
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();
        List<String> etapes = new ArrayList<>();

        int nbDesc = req.nombreDescendants();
        boolean petitsEnfantsSubst = Boolean.TRUE.equals(req.presencePetitsEnfantsParSubstitution());
        boolean conjonctive = Boolean.TRUE.equals(req.donationPartageConjonctive());
        boolean reincorporation = Boolean.TRUE.equals(req.donationsAnterieuresAReinorporer());
        Boolean quotiteRespectee = req.respectQuotiteDisponible();

        // ---------------------------------------------------------------
        // 1. Conditions de validité (art. 1075, 1075-1, 1075-2 Cciv).
        // ---------------------------------------------------------------
        boolean conditionsRemplies = true;
        if (petitsEnfantsSubst) {
            messages.add("Donation-partage avec attribution aux petits-enfants "
                    + "par substitution (art. 1075-1 Cciv) : la libéralité au "
                    + "petit-enfant suppose le CONSENTEMENT exprès du descendant "
                    + "intermédiaire (enfant du donateur), formalisé dans l'acte "
                    + "notarié. À défaut, la donation est nulle pour défaut de "
                    + "consentement.");
            alertes.add("Consentement du descendant intermédiaire à recueillir "
                    + "expressément dans l'acte (art. 1075-1 Cciv) — sans ce "
                    + "consentement, l'attribution au petit-enfant ne vaut pas "
                    + "donation-partage et bascule en simple donation ordinaire "
                    + "(rapport applicable, gel de valeur perdu).");
        }
        if (conjonctive) {
            messages.add("Donation-partage conjonctive (art. 1075-2 Cciv) : les "
                    + "deux parents font ensemble une donation de leurs biens "
                    + "propres et communs. L'acte notarié unique permet de "
                    + "répartir les biens issus des deux patrimoines parmi les "
                    + "descendants, en neutralisant les rapports et les "
                    + "réévaluations entre les deux successions futures.");
            if (req.agesDonateurs() != null && req.agesDonateurs().size() < 2) {
                alertes.add("Donation-partage conjonctive déclarée mais un seul "
                        + "âge de donateur renseigné — vérifier la qualité des "
                        + "co-donateurs (art. 1075-2 Cciv) : l'acte doit faire "
                        + "intervenir les deux parents.");
            }
        }

        // ---------------------------------------------------------------
        // 2. Gel de valeur (art. 1078 Cciv).
        // ---------------------------------------------------------------
        String gelValeur =
                "Gel de la valeur au jour de la donation-partage (art. 1078 "
                        + "Cciv) : les biens donnés-partagés sont évalués à la "
                        + "DATE DE LA DONATION, et non à la date du décès. "
                        + "La fluctuation ultérieure (plus-value ou moins-value) "
                        + "ne profite ni ne nuit aux copartagés au moment de la "
                        + "succession. Avantage majeur en cas de bien à fort "
                        + "potentiel d'appréciation (immobilier, parts sociales).";
        messages.add(gelValeur);

        // ---------------------------------------------------------------
        // 3. Exclusion du rapport successoral (art. 1075-3 Cciv).
        // ---------------------------------------------------------------
        boolean rapportExclu = true;
        messages.add("Exclusion du rapport successoral (art. 1075-3 Cciv) : les "
                + "biens reçus en donation-partage ne sont pas sujets au rapport "
                + "à la masse partageable au décès du donateur, contrairement "
                + "aux donations ordinaires (art. 843 Cciv). Le partage anticipé "
                + "est donc définitif sous réserve de l'action en réduction "
                + "(art. 920 Cciv).");

        // ---------------------------------------------------------------
        // 4. Réincorporation de donations antérieures (art. 1078-1 Cciv).
        // ---------------------------------------------------------------
        if (reincorporation) {
            messages.add("Réincorporation de donations antérieures (art. 1078-1 "
                    + "Cciv) : possibilité d'intégrer dans la donation-partage "
                    + "des biens précédemment donnés à un descendant pour "
                    + "équilibrer le partage anticipé. La réincorporation peut "
                    + "se faire au profit du même donataire (changement de "
                    + "qualification — simple donation → donation-partage) ou "
                    + "d'un autre descendant (avec accord du donataire initial).");
            etapes.add("Lister les donations antérieures à réincorporer et "
                    + "recueillir l'accord exprès du / des donataires initiaux "
                    + "(art. 1078-1 Cciv).");
        }

        // ---------------------------------------------------------------
        // 5. Alerte quotité disponible (art. 912-928 Cciv).
        // ---------------------------------------------------------------
        boolean alerteQuotite = false;
        if (Boolean.FALSE.equals(quotiteRespectee)) {
            alerteQuotite = true;
            alertes.add("Quotité disponible dépassée (art. 912-928 Cciv) : la "
                    + "donation-partage excède la quotité disponible ordinaire "
                    + "(1/2 si 1 enfant, 1/3 si 2 enfants, 1/4 si 3 enfants et "
                    + "plus — art. 913 Cciv). Au décès du donateur, les "
                    + "héritiers réservataires pourront exercer l'action en "
                    + "réduction (art. 920-924 Cciv) sur les libéralités "
                    + "excessives. Rééquilibrer avant signature ou prévoir une "
                    + "soulte.");
        } else if (quotiteRespectee == null) {
            alertes.add("Respect de la quotité disponible non documenté — "
                    + "réaliser un audit patrimonial préalable (art. 912-928 "
                    + "Cciv) avant signature pour s'assurer que la donation-"
                    + "partage n'expose pas à une action en réduction au décès.");
        } else {
            messages.add("Quotité disponible respectée — la donation-partage est "
                    + "à l'abri d'une action en réduction par les autres "
                    + "héritiers réservataires (art. 920 Cciv).");
        }

        // ---------------------------------------------------------------
        // 6. Quasi-usufruit éventuel (art. 1080 Cciv) — vigilance générique.
        // ---------------------------------------------------------------
        messages.add("Quasi-usufruit possible (art. 1080 Cciv) : la donation-"
                + "partage peut être stipulée avec démembrement (nue-propriété "
                + "donnée, usufruit réservé) ou intégrer un quasi-usufruit sur "
                + "sommes d'argent. La créance de restitution naît à l'extinction "
                + "de l'usufruit et est payable sur l'actif successoral.");

        // ---------------------------------------------------------------
        // 7. Verdict d'intérêt global.
        // ---------------------------------------------------------------
        String interet;
        if (alerteQuotite) {
            interet = "MOYEN";
        } else if (nbDesc >= 2 && !petitsEnfantsSubst) {
            interet = "FORT";
        } else if (nbDesc == 1) {
            interet = "MOYEN";
            messages.add("Donation-partage à un seul descendant : l'effet "
                    + "anti-rapport est neutre (art. 1075-3 — pas de copartage "
                    + "à équilibrer), mais le gel de valeur (art. 1078) conserve "
                    + "son intérêt fiscal et successoral. Comparer avec une "
                    + "donation simple en avance de part successorale.");
        } else {
            interet = "FORT";
        }

        // ---------------------------------------------------------------
        // 8. Étapes notariales (checklist ordonnée).
        // ---------------------------------------------------------------
        etapes.add("Consultation notariale préalable : audit patrimonial, "
                + "valorisation des biens, vérification de la quotité disponible.");
        etapes.add("Rédaction du projet d'acte authentique de donation-partage "
                + "(art. 931 Cciv — forme notariée obligatoire pour les donations).");
        if (petitsEnfantsSubst) {
            etapes.add("Recueil du consentement exprès du descendant intermédiaire "
                    + "à la donation-partage transgénérationnelle (art. 1075-1 Cciv).");
        }
        if (conjonctive) {
            etapes.add("Comparution des deux donateurs à l'acte unique de "
                    + "donation-partage conjonctive (art. 1075-2 Cciv).");
        }
        etapes.add("Signature de l'acte notarié et publicité foncière si biens "
                + "immobiliers (décret 4 janv. 1955).");
        etapes.add("Liquidation et paiement des droits de mutation à titre "
                + "gratuit (CGI art. 777 et s. — abattement 100 000 € par "
                + "parent/enfant, renouvelable tous les 15 ans).");

        return new DonationPartageResult(
                conditionsRemplies,
                interet,
                gelValeur,
                rapportExclu,
                alerteQuotite,
                etapes,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }
}

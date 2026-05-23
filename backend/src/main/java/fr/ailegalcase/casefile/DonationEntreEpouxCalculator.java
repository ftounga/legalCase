package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-23 : calculateur statique pour la donation entre époux en droit
 * français (art. 1091-1100 Cciv + art. 265 al. 2 Cciv + art. 1527 al. 2
 * Cciv + art. 912-928 Cciv).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>évalue la <strong>révocabilité</strong> de la donation selon son
 *       type (art. 1096 al. 1 — révocable ad nutum pour donation au
 *       dernier vivant ; biens présents intégrés au contrat de mariage
 *       irrévocables sauf changement de régime) ;</li>
 *   <li>détecte la <strong>révocation de plein droit en cas de divorce</strong>
 *       (art. 265 al. 2 Cciv — la dissolution du mariage révoque les
 *       libéralités à cause de mort consenties au conjoint divorcé) ;</li>
 *   <li>signale l'<strong>action en retranchement</strong> ouverte aux
 *       enfants non communs en cas de clause d'attribution intégrale
 *       excessive (art. 1527 al. 2 Cciv) ;</li>
 *   <li>rappelle l'<strong>impact sur la réserve héréditaire</strong> pour
 *       la donation de biens futurs (art. 912-928 + quotité disponible
 *       spéciale entre époux art. 1094-1).</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la donation entre époux
 * est régie par les art. 4.243 et suivants CC BE (outil F-FA-BE-DONATION-EPOUX
 * futur, hors périmètre F-216).</p>
 */
public final class DonationEntreEpouxCalculator {

    static final String BASE_JURIDIQUE =
            "art. 1091-1100 Cciv (donation entre époux) + art. 265 al. 2 Cciv "
                    + "(révocation en cas de divorce) + art. 1527 al. 2 Cciv "
                    + "(action en retranchement enfants non communs) + "
                    + "art. 912-928 Cciv (réserve héréditaire) + "
                    + "Cass. 1ère civ., 4 juin 2014 (distinction avantage "
                    + "matrimonial / libéralité)";

    private DonationEntreEpouxCalculator() {}

    /**
     * Évalue les conditions de la donation entre époux et émet le verdict
     * + les alertes correspondantes.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static DonationEntreEpouxResult compute(
            DonationEntreEpouxRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-DONATION-ENTRE-EPOUX applicable uniquement "
                            + "en France (art. 1091-1100 Cciv).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        AvantageMatrimonialTypeEnum type = req.avantageMatrimonialType();
        boolean divorce = Boolean.TRUE.equals(req.mariageDissous());
        boolean revocationExpresse = Boolean.TRUE.equals(req.revocabiliteDetectee());
        boolean enfantsNonCommuns = Boolean.TRUE.equals(req.enfantsNonCommunsDetected());
        RegimeMatrimonialEnum regime = req.regimeMatrimonial();

        // 1. Effet de la dissolution du mariage par divorce.
        //    Art. 265 al. 2 Cciv : « Le divorce emporte révocation de plein
        //    droit des avantages matrimoniaux qui ne prennent effet qu'à
        //    la dissolution du régime matrimonial ou au décès de l'un des
        //    époux et des dispositions à cause de mort. »
        String effetDissolution;
        if (divorce && (type == AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS
                || type == AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE
                || type == AvantageMatrimonialTypeEnum.AVANTAGE_MATRIMONIAL)) {
            messages.add("Divorce prononcé / en cours (art. 265 al. 2 Cciv) : "
                    + "les avantages matrimoniaux à effet au décès et les "
                    + "dispositions à cause de mort consenties à l'ex-conjoint "
                    + "sont révoqués de plein droit. La donation au dernier "
                    + "vivant et la clause d'attribution intégrale tombent "
                    + "automatiquement.");
            effetDissolution = "Révocation de plein droit (art. 265 al. 2 Cciv) — "
                    + "l'ex-conjoint perd le bénéfice de la donation à cause "
                    + "de mort dès le prononcé du divorce.";
        } else if (divorce && type == AvantageMatrimonialTypeEnum.DONATION_BIEN_PRESENT) {
            messages.add("Divorce prononcé / en cours : la donation entre "
                    + "vifs portant sur un bien présent (déjà transférée) "
                    + "n'est PAS automatiquement révoquée par le divorce "
                    + "(art. 265 al. 1 Cciv). La révocation suppose une "
                    + "décision expresse du donateur ou une action en justice.");
            effetDissolution = "Pas de révocation automatique pour la donation "
                    + "de biens présents (art. 265 al. 1 Cciv) — la donation "
                    + "subsiste sauf révocation expresse du donateur.";
        } else if (divorce && type == AvantageMatrimonialTypeEnum.ASSURANCE_VIE) {
            messages.add("Divorce prononcé / en cours : le contrat "
                    + "d'assurance-vie au profit du conjoint n'est PAS "
                    + "automatiquement annulé par le divorce — il faut "
                    + "vérifier la clause bénéficiaire et procéder à une "
                    + "modification expresse auprès de l'assureur "
                    + "(art. L.132-9 Code des assurances).");
            effetDissolution = "Pas d'effet automatique sur le contrat "
                    + "d'assurance-vie — modification expresse de la clause "
                    + "bénéficiaire requise.";
        } else {
            effetDissolution = "Mariage non dissous — la donation produit "
                    + "ses effets selon les règles propres à son type "
                    + "(révocabilité art. 1096 Cciv).";
        }

        // 2. Évaluation de la révocabilité (art. 1096 Cciv).
        String revocabilite;
        String validite;
        if (type == null) {
            alertes.add("Type d'avantage matrimonial non renseigné — analyse "
                    + "incomplète. Préciser : donation de bien présent, "
                    + "donation de biens futurs (au dernier vivant), "
                    + "avantage matrimonial, assurance-vie, ou clause "
                    + "d'attribution intégrale.");
            revocabilite = "INDETERMINEE";
            validite = "A_VERIFIER";
        } else if (revocationExpresse) {
            messages.add("Révocation expresse ou tacite détectée (art. 1096 "
                    + "al. 2 Cciv) : le donateur a manifesté sa volonté de "
                    + "révoquer la donation (par testament postérieur, acte "
                    + "notarié, ou faits incompatibles avec le maintien de "
                    + "la libéralité). La donation est révoquée.");
            revocabilite = "REVOCATION_CONSTATEE";
            validite = "REVOQUEE";
        } else if (divorce && (type == AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS
                || type == AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE
                || type == AvantageMatrimonialTypeEnum.AVANTAGE_MATRIMONIAL)) {
            revocabilite = "REVOCATION_DE_PLEIN_DROIT";
            validite = "REVOQUEE";
        } else {
            switch (type) {
                case DONATION_BIENS_FUTURS -> {
                    messages.add("Donation au dernier vivant (biens futurs) — "
                            + "art. 1094 et 1094-1 Cciv : révocable ad nutum "
                            + "par le donateur (par testament postérieur, "
                            + "déclaration devant notaire, etc.) tant que le "
                            + "donateur est en vie (art. 1096 al. 1).");
                    revocabilite = "REVOCABLE_AD_NUTUM";
                    validite = "VALIDE";
                }
                case DONATION_BIEN_PRESENT -> {
                    boolean integreContratMariage = regime == RegimeMatrimonialEnum.COMMUNAUTE_UNIVERSELLE
                            && Boolean.TRUE.equals(req.enfantsNonCommunsDetected() == null);
                    if (integreContratMariage) {
                        messages.add("Donation de biens présents intégrée au "
                                + "contrat de mariage (régime communautaire "
                                + "universel) — révocabilité limitée : suit "
                                + "le régime du changement de contrat de "
                                + "mariage (art. 1397 Cciv).");
                        revocabilite = "REVOCABILITE_LIMITEE";
                    } else {
                        messages.add("Donation de biens présents (transfert "
                                + "immédiat) — révocable selon les causes de "
                                + "l'art. 953 Cciv (inexécution des charges, "
                                + "ingratitude, survenance d'enfant) et "
                                + "art. 1096 al. 2 Cciv (révocation expresse "
                                + "ou tacite par le donateur).");
                        revocabilite = "REVOCABLE_AD_NUTUM";
                    }
                    validite = "VALIDE";
                }
                case CLAUSE_ATTRIBUTION_INTEGRALE -> {
                    messages.add("Clause d'attribution intégrale de la "
                            + "communauté au conjoint survivant (art. 1524 "
                            + "Cciv) — qualifiée d'avantage matrimonial, "
                            + "irrévocable pendant le mariage sauf changement "
                            + "de régime matrimonial (art. 1397 Cciv).");
                    revocabilite = "IRREVOCABLE";
                    validite = "VALIDE";
                }
                case AVANTAGE_MATRIMONIAL -> {
                    messages.add("Avantage matrimonial (préciput, partage "
                            + "inégal, etc. — art. 1515-1525 Cciv) : "
                            + "irrévocable pendant le mariage. La révocation "
                            + "impose un changement de régime matrimonial "
                            + "(art. 1397 Cciv, procédure notariale avec "
                            + "homologation judiciaire si enfants mineurs).");
                    revocabilite = "IRREVOCABLE";
                    validite = "VALIDE";
                }
                case ASSURANCE_VIE -> {
                    messages.add("Contrat d'assurance-vie au profit du "
                            + "conjoint — la clause bénéficiaire est "
                            + "modifiable librement par le souscripteur "
                            + "(art. L.132-9 Code des assurances) tant "
                            + "qu'aucune acceptation expresse du bénéficiaire "
                            + "n'est intervenue.");
                    revocabilite = "REVOCABLE_AD_NUTUM";
                    validite = "VALIDE";
                }
                default -> {
                    revocabilite = "INDETERMINEE";
                    validite = "A_VERIFIER";
                }
            }
        }

        // 3. Action en retranchement (art. 1527 al. 2 Cciv) — enfants non
        //    communs en présence d'une clause d'attribution intégrale ou
        //    d'un avantage matrimonial excessif.
        boolean actionRetranchement = false;
        if ((type == AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE
                || type == AvantageMatrimonialTypeEnum.AVANTAGE_MATRIMONIAL)
                && enfantsNonCommuns) {
            actionRetranchement = true;
            alertes.add("Action en retranchement ouverte (art. 1527 al. 2 "
                    + "Cciv) : la présence d'enfants non communs au couple "
                    + "permet à ces enfants de demander la réduction de "
                    + "l'avantage matrimonial / de la clause d'attribution "
                    + "intégrale à la quotité disponible spéciale entre époux "
                    + "(art. 1094-1) — l'avantage excédant la quotité est "
                    + "retranché à leur profit.");
        }

        // 4. Impact sur la réserve héréditaire (art. 912-928 Cciv).
        String impactReserve;
        if (type == AvantageMatrimonialTypeEnum.DONATION_BIENS_FUTURS) {
            impactReserve = "Donation au dernier vivant soumise à la "
                    + "quotité disponible spéciale entre époux (art. 1094-1 "
                    + "Cciv) — soit la quotité ordinaire, soit 1/4 en "
                    + "pleine propriété + 3/4 en usufruit, soit la totalité "
                    + "en usufruit. Réductible si elle excède cette quotité "
                    + "spéciale (art. 920-924 Cciv). En présence d'enfants "
                    + "non communs : limite réduite à la quotité disponible "
                    + "ordinaire.";
        } else if (type == AvantageMatrimonialTypeEnum.DONATION_BIEN_PRESENT) {
            impactReserve = "Donation de biens présents soumise à la "
                    + "réserve héréditaire des descendants (art. 912 Cciv) "
                    + "— réductible si elle porte atteinte à la réserve "
                    + "(art. 920-924 Cciv). Le rapport éventuel se fait "
                    + "selon la valeur au jour du partage.";
        } else if (type == AvantageMatrimonialTypeEnum.CLAUSE_ATTRIBUTION_INTEGRALE
                || type == AvantageMatrimonialTypeEnum.AVANTAGE_MATRIMONIAL) {
            impactReserve = "Avantage matrimonial : non-libéralité en "
                    + "principe (art. 1527 al. 1 Cciv) — pas de réduction "
                    + "pour atteinte à la réserve des enfants communs, sauf "
                    + "action en retranchement par les enfants non communs "
                    + "(art. 1527 al. 2 Cciv).";
        } else if (type == AvantageMatrimonialTypeEnum.ASSURANCE_VIE) {
            impactReserve = "Contrat d'assurance-vie : capital hors "
                    + "succession (art. L.132-12 Code des assurances) sauf "
                    + "primes manifestement exagérées au regard des "
                    + "facultés du souscripteur — auquel cas la réintégration "
                    + "à la masse successorale est possible (art. L.132-13 "
                    + "Code des assurances).";
        } else {
            impactReserve = "Impact sur la réserve à déterminer après "
                    + "qualification précise de l'avantage (libéralité vs "
                    + "avantage matrimonial — distinction Cass. 1ère civ., "
                    + "4 juin 2014).";
        }

        // 5. Vigilance complémentaire — révocabilité limitée + divorce
        //    déclenche une alerte sur la procédure à suivre.
        if (revocationExpresse && divorce) {
            alertes.add("Révocation expresse documentée ET divorce en cours : "
                    + "la révocation est doublement assurée — par la volonté "
                    + "du donateur (art. 1096 al. 2) ET par la dissolution "
                    + "du mariage (art. 265 al. 2). Conserver les pièces "
                    + "documentant la chronologie pour éviter tout litige "
                    + "successoral ultérieur.");
        }

        return new DonationEntreEpouxResult(
                validite,
                revocabilite,
                effetDissolution,
                actionRetranchement,
                impactReserve,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }
}

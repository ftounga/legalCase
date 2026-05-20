package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-01 : calculateur statique de la prestation compensatoire (art. 270-281 Cciv).
 *
 * <p>Règles de calcul (jurisprudence Cass. 1ère civ., 8/10/2014, n°13-21.804) :</p>
 * <ul>
 *   <li>Disparité de niveaux de vie = delta revenus annuels + impact patrimonial.</li>
 *   <li>Montant capital indicatif = durée × delta mensuel × facteur correctif âge/durée.</li>
 *   <li>Rente mensuelle = delta mensuel / 3 (limitée au tiers de la différence).</li>
 *   <li>Forme recommandée : capital si demandeur &lt; 60 ans et durée &lt; 20 ans ;
 *       rente si durée ≥ 20 ans OU demandeur ≥ 60 ans (Cass. 1ère civ. 2014).</li>
 *   <li>Avantage matrimonial imputable sur la prestation (art. 270 al. 2 Cciv).</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE (art. 270-281 Cciv — droit français uniquement).</p>
 *
 * <p>Classe distincte de {@code PrestationCompensatoireCalculator} (legacy pipeline IA)
 * pour éviter toute collision de noms.</p>
 */
public final class PrestationCompensatoireCalculatorSF216 {

    static final String BASE_JURIDIQUE =
            "art. 270-281 Cciv + Cass. 1ère civ. 8/10/2014 n°13-21.804 + art. 272 Cciv";

    private PrestationCompensatoireCalculatorSF216() {}

    /**
     * Calcule la prestation compensatoire indicative.
     *
     * @param dureeMariageAnnees  durée du mariage en années (≥ 0)
     * @param revenus1Eur         revenus annuels époux 1 (demandeur / moins aisé, ≥ 0)
     * @param revenus2Eur         revenus annuels époux 2 (défendeur, ≥ 0)
     * @param patrimoine1Eur      patrimoine propre époux 1 (optionnel, ≥ 0)
     * @param patrimoine2Eur      patrimoine propre époux 2 (optionnel, ≥ 0)
     * @param age1Annees          âge époux 1 (demandeur, ≥ 0)
     * @param age2Annees          âge époux 2 (≥ 0)
     * @param formesDemandee      forme demandée par l'avocat (peut être null → INCERTAIN)
     * @param avantageMatrimonial avantage matrimonial détecté (art. 270 al. 2)
     * @param country             pays du workspace
     * @return résultat du calcul
     * @throws IllegalArgumentException si données invalides
     */
    public static PrestationCompensatoireResult compute(
            int dureeMariageAnnees,
            int revenus1Eur,
            int revenus2Eur,
            Integer patrimoine1Eur,
            Integer patrimoine2Eur,
            int age1Annees,
            int age2Annees,
            FormePrestationEnum formesDemandee,
            boolean avantageMatrimonial,
            String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException("Outil F-FA-01 applicable uniquement en France (art. 270 Cciv).");
        }
        if (dureeMariageAnnees < 0) {
            throw new IllegalArgumentException("La durée du mariage ne peut être négative.");
        }
        if (revenus1Eur < 0 || revenus2Eur < 0) {
            throw new IllegalArgumentException("Les revenus annuels ne peuvent être négatifs.");
        }
        if (age1Annees < 0 || age2Annees < 0) {
            throw new IllegalArgumentException("L'âge des époux ne peut être négatif.");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        // 1. Calcul de la disparité
        int deltaRevenusAnnuels = revenus2Eur - revenus1Eur;
        int deltaRevenusMenuel = deltaRevenusAnnuels / 12;

        // Impact patrimonial estimé (différence de patrimoine propre)
        int pat1 = patrimoine1Eur != null ? patrimoine1Eur : 0;
        int pat2 = patrimoine2Eur != null ? patrimoine2Eur : 0;
        int deltaPatrimoine = pat2 - pat1;

        String dispAriteNiveauxVie;
        if (Math.abs(deltaRevenusAnnuels) < 1200 && Math.abs(deltaPatrimoine) < 5000) {
            dispAriteNiveauxVie = "NULLE";
            messages.add("Les niveaux de vie des époux sont comparables — prestation compensatoire faible ou nulle.");
        } else if (Math.abs(deltaRevenusAnnuels) < 6000) {
            dispAriteNiveauxVie = "FAIBLE";
            messages.add("Disparité de niveaux de vie faible — montant indicatif de référence uniquement.");
        } else if (Math.abs(deltaRevenusAnnuels) < 24000) {
            dispAriteNiveauxVie = "MODEREE";
        } else {
            dispAriteNiveauxVie = "IMPORTANTE";
            messages.add("Disparité importante — base juridique solide pour la prestation compensatoire (art. 272 Cciv).");
        }

        if (deltaRevenusAnnuels <= 0) {
            // Époux 1 gagne plus que l'époux 2 — pas de disparité en faveur du demandeur
            return new PrestationCompensatoireResult(0, 0, FormePrestationEnum.INCERTAIN,
                    "NULLE",
                    BASE_JURIDIQUE,
                    List.of("Les revenus de l'époux 1 sont supérieurs ou égaux — pas de prestation compensatoire indicative.",
                            "Si une prestation est demandée par l'époux 2, inverser les rôles."),
                    List.of());
        }

        // 2. Calcul du montant indicatif capital
        // Formule : durée × delta mensuel × facteur correctif (âge/durée)
        double facteurCorrectif = computeFacteurCorrectif(dureeMariageAnnees, age1Annees);
        int montantCapital = (int) (dureeMariageAnnees * deltaRevenusMenuel * facteurCorrectif);
        if (montantCapital < 0) montantCapital = 0;

        // 3. Réduction si avantage matrimonial (art. 270 al. 2 Cciv)
        if (avantageMatrimonial) {
            int reduction = (int) (montantCapital * 0.20);
            montantCapital = Math.max(0, montantCapital - reduction);
            alertes.add("Avantage matrimonial détecté (art. 270 al. 2 Cciv) — montant capital réduit de 20% indicatif.");
            messages.add("Clause d'attribution intégrale ou communauté universelle : imputable sur la prestation compensatoire.");
        }

        // 4. Rente mensuelle indicative = delta mensuel / 3
        int renteMensuelle = deltaRevenusMenuel / 3;
        if (renteMensuelle < 0) renteMensuelle = 0;

        // 5. Recommandation de forme (Cass. 1ère civ. 8/10/2014 : capital = règle, rente = exception)
        FormePrestationEnum formeRecommandee;
        if (dureeMariageAnnees >= 20 || age1Annees >= 60) {
            formeRecommandee = FormePrestationEnum.RENTE;
            messages.add("Rente recommandée : durée ≥ 20 ans OU demandeur ≥ 60 ans (Cass. 1ère civ. 8/10/2014 n°13-21.804).");
        } else {
            formeRecommandee = FormePrestationEnum.CAPITAL;
            messages.add("Capital recommandé : clôture définitive des comptes patrimoniaux entre époux (règle de principe).");
        }

        // Si l'avocat a une préférence différente, on avertit
        if (formesDemandee != null && formesDemandee != FormePrestationEnum.INCERTAIN
                && formesDemandee != formeRecommandee) {
            alertes.add("Forme demandée (" + formesDemandee + ") diffère de la recommandation (" + formeRecommandee + ") selon la jurisprudence.");
        }

        // Alertes complémentaires
        if (dureeMariageAnnees < 2) {
            alertes.add("Mariage de courte durée (< 2 ans) — la prestation compensatoire peut être limitée ou refusée.");
        }
        if (age1Annees > 0 && age1Annees < 30 && dureeMariageAnnees < 5) {
            messages.add("Jeune demandeur avec courte durée de mariage — évaluer la capacité à retrouver un emploi (art. 272 Cciv).");
        }

        messages.add("Montant indicatif — le juge peut fixer librement la prestation en fonction des critères art. 272 Cciv.");

        return new PrestationCompensatoireResult(
                montantCapital,
                renteMensuelle,
                formeRecommandee,
                dispAriteNiveauxVie,
                BASE_JURIDIQUE,
                messages,
                alertes
        );
    }

    /**
     * Facteur correctif basé sur l'âge du demandeur et la durée du mariage.
     * Plus la durée est longue et l'âge avancé, plus le facteur est élevé.
     */
    private static double computeFacteurCorrectif(int dureeMariageAnnees, int ageDemandeur) {
        double facteur = 1.0;
        // Facteur durée
        if (dureeMariageAnnees >= 20) {
            facteur += 0.5;
        } else if (dureeMariageAnnees >= 10) {
            facteur += 0.25;
        }
        // Facteur âge (impact sur capacité à retrouver un niveau de vie)
        if (ageDemandeur >= 60) {
            facteur += 0.4;
        } else if (ageDemandeur >= 50) {
            facteur += 0.2;
        }
        return facteur;
    }
}

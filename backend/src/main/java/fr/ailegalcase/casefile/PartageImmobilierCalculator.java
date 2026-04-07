package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule la soulte, les droits de partage et les frais estimés pour un partage immobilier.
 */
public final class PartageImmobilierCalculator {

    private PartageImmobilierCalculator() {}

    // France : droit de partage = 1.1% de l'actif net partagé
    private static final BigDecimal TAUX_DROIT_PARTAGE_FR = new BigDecimal("1.1");

    // Belgique : 1% si partage suite à divorce, 2.5% dans les autres cas
    private static final BigDecimal TAUX_DROIT_PARTAGE_BE_DIVORCE = new BigDecimal("1");
    private static final BigDecimal TAUX_DROIT_PARTAGE_BE_AUTRE = new BigDecimal("2.5");

    // Frais de notaire estimés (hors droits) : ~1.5% en FR, ~1% en BE
    private static final BigDecimal TAUX_FRAIS_NOTAIRE_FR = new BigDecimal("1.5");
    private static final BigDecimal TAUX_FRAIS_NOTAIRE_BE = new BigDecimal("1");

    /**
     * @param country               FRANCE ou BELGIQUE
     * @param valeurVenale          valeur vénale du bien
     * @param capitalRestantDu      capital restant dû sur le prêt immobilier
     * @param quotePartAttributaire quote-part de celui qui garde le bien (ex: 0.50 = 50%)
     * @param isDivorce             true si le partage intervient dans le cadre d'un divorce (impact sur taux BE)
     */
    public static PartageImmobilierResult calculate(
            String country,
            BigDecimal valeurVenale,
            BigDecimal capitalRestantDu,
            BigDecimal quotePartAttributaire,
            boolean isDivorce
    ) {
        if (!"FRANCE".equals(country) && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException("Pays non supporté : " + country);
        }

        BigDecimal valeurNette = valeurVenale.subtract(capitalRestantDu);
        BigDecimal quotePartCedant = BigDecimal.ONE.subtract(quotePartAttributaire);

        BigDecimal partAttributaire = valeurNette.multiply(quotePartAttributaire).setScale(2, RoundingMode.HALF_UP);
        BigDecimal partCedant = valeurNette.multiply(quotePartCedant).setScale(2, RoundingMode.HALF_UP);

        // Soulte = ce que l'attributaire doit payer au cédant pour compenser sa part
        BigDecimal soulte = partCedant.setScale(2, RoundingMode.HALF_UP);

        BigDecimal tauxDroit;
        BigDecimal tauxFraisNotaire;
        String baseJuridique;
        String commentaire;

        if ("FRANCE".equals(country)) {
            tauxDroit = TAUX_DROIT_PARTAGE_FR;
            tauxFraisNotaire = TAUX_FRAIS_NOTAIRE_FR;
            baseJuridique = "Article 746 du Code général des impôts — droit de partage à 1,10 % de l'actif net";
            commentaire = "Le droit de partage s'applique sur l'actif net partagé. Les frais de notaire sont estimatifs (émoluments + formalités).";
        } else {
            tauxDroit = isDivorce ? TAUX_DROIT_PARTAGE_BE_DIVORCE : TAUX_DROIT_PARTAGE_BE_AUTRE;
            tauxFraisNotaire = TAUX_FRAIS_NOTAIRE_BE;
            baseJuridique = isDivorce
                    ? "Code des droits d'enregistrement — taux réduit de 1 % en cas de partage suite à divorce"
                    : "Code des droits d'enregistrement — taux de 2,5 % pour les partages entre copropriétaires";
            commentaire = isDivorce
                    ? "Taux réduit de 1 % applicable car le partage intervient dans le cadre d'un divorce."
                    : "Taux standard de 2,5 %. Le taux réduit de 1 % est applicable uniquement en cas de divorce.";
        }

        BigDecimal droitPartage = valeurNette.multiply(tauxDroit)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal fraisNotaire = valeurVenale.multiply(tauxFraisNotaire)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal coutTotal = soulte.add(droitPartage).add(fraisNotaire);

        return new PartageImmobilierResult(
                country, valeurVenale, capitalRestantDu, valeurNette,
                quotePartAttributaire, quotePartCedant,
                partAttributaire, partCedant, soulte,
                droitPartage, tauxDroit, fraisNotaire, coutTotal,
                baseJuridique, commentaire
        );
    }
}

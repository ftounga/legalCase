package fr.ailegalcase.casefile;

/**
 * SF-218-07 : résultat interne business du calcul de la quotité saisissable
 * d'une rémunération. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param remunerationNetteMensuelle assiette servant au calcul (€).
 * @param nombrePersonnesACharge nombre de personnes à charge prises en compte.
 * @param creanceTotale montant de la créance à recouvrer (€).
 * @param creanceAlimentaire true si la créance est alimentaire (paiement direct).
 * @param quotiteSaisissableMensuelle part mensuelle saisissable (€).
 * @param montantLaisseAuSalarie part mensuelle laissée au salarié (€), jamais
 *        inférieure à la fraction absolument insaisissable.
 * @param fractionInsaisissable fraction absolument insaisissable retenue (RSA, €).
 * @param nombreMoisRecouvrement nombre de mois nécessaires au recouvrement de la
 *        créance (ceil(creance / quotité)), 0 si quotité nulle.
 * @param verdict orientation de la saisie.
 * @param baseJuridique fondements juridiques applicables.
 */
public record SaisieRemunerationResult(
        double remunerationNetteMensuelle,
        int nombrePersonnesACharge,
        double creanceTotale,
        boolean creanceAlimentaire,
        double quotiteSaisissableMensuelle,
        double montantLaisseAuSalarie,
        double fractionInsaisissable,
        int nombreMoisRecouvrement,
        SaisieRemunerationVerdict verdict,
        String baseJuridique
) {}

package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * Résultat du comparateur jurisprudentiel d'indemnités.
 */
public record IndemniteComparatifResult(
        String country,
        int ancienneteAnnees,
        int age,
        BigDecimal salaireMensuel,
        BigDecimal baremePlancherMois,
        BigDecimal baremePlafondMois,
        BigDecimal fourchetteBasseMois,
        BigDecimal fourchetteMedMois,
        BigDecimal fourhetteHauteMois,
        BigDecimal fourchetteBasseMontant,
        BigDecimal fourchetteMedMontant,
        BigDecimal fourhetteHauteMontant,
        String baremeSource,
        String commentaire
) {}

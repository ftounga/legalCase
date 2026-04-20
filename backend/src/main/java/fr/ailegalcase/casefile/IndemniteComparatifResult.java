package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat du comparateur jurisprudentiel d'indemnités.
 */
public record IndemniteComparatifResult(
        String country,
        String typeRupture,
        int ancienneteAnnees,
        int age,
        BigDecimal salaireMensuel,
        String displayMode,
        BigDecimal baremePlancherMois,
        BigDecimal baremePlafondMois,
        BigDecimal fourchetteBasseMois,
        BigDecimal fourchetteMedMois,
        BigDecimal fourhetteHauteMois,
        BigDecimal fourchetteBasseMontant,
        BigDecimal fourchetteMedMontant,
        BigDecimal fourhetteHauteMontant,
        BigDecimal indemniteLegaleMontant,
        String baremeSource,
        String commentaire,
        List<String> contextualMessages
) {}

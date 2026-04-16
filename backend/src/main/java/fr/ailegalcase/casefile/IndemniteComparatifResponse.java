package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IndemniteComparatifResponse(
        UUID caseFileId,
        String country,
        String typeRupture,
        int ancienneteAnnees,
        int ancienneteMois,
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

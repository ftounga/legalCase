package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.UUID;

public record IndemniteComparatifResponse(
        UUID caseFileId,
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

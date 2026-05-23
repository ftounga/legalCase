package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-05 : données d'entrée du calculateur d'applicabilité de
 * l'art. L. 1224-1 CT (transfert d'entreprise — FRANCE).
 */
public record TransfertEntrepriseL12241Input(
        TransfertEntrepriseL12241Calculator.TypeTransfert typeTransfert,
        Boolean eeaIdentifieeAvantTransfert,
        Boolean activiteEconomiquePreservee,
        Boolean salariesTransferes,
        Boolean contratsModifiesParRepreneur,
        Boolean licenciementsPreTransfert,
        int nbLicenciementsPreTransfert,
        Boolean informationConsultationCseRealisee,
        LocalDate dateTransfert
) {}

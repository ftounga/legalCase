package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-05 : requête HTTP pour l'endpoint d'analyse de transfert d'entreprise
 * (FRANCE — L. 1224-1 CT).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record TransfertEntrepriseL12241Request(
        TransfertEntrepriseL12241Calculator.TypeTransfert typeTransfert,
        Boolean eeaIdentifieeAvantTransfert,
        Boolean activiteEconomiquePreservee,
        Boolean salariesTransferes,
        Boolean contratsModifiesParRepreneur,
        Boolean licenciementsPreTransfert,
        int nbLicenciementsPreTransfert,
        Boolean informationConsultationCseRealisee,
        LocalDate dateTransfert
) {

    TransfertEntrepriseL12241Input toInput() {
        return new TransfertEntrepriseL12241Input(
                typeTransfert,
                eeaIdentifieeAvantTransfert,
                activiteEconomiquePreservee,
                salariesTransferes,
                contratsModifiesParRepreneur,
                licenciementsPreTransfert,
                nbLicenciementsPreTransfert,
                informationConsultationCseRealisee,
                dateTransfert
        );
    }
}

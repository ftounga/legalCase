package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-25 : résultat interne business de l'analyse du licenciement pour fin de
 * chantier du CDI de chantier / d'opération (art. L.1223-8 et s. ; L.1236-8 CT,
 * F-DT-37). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat.
 * @param dateRupture notification du licenciement.
 * @param fondementRecours fondement juridique du recours.
 * @param secteur secteur d'activité.
 * @param chantierAcheve true si le chantier est achevé.
 * @param salaireMensuelMoyen salaire mensuel moyen (base de l'indemnité).
 * @param reclassementAutreChantierPropose true si une poursuite sur un autre
 *        chantier a été proposée.
 * @param ancienneteAnnees ancienneté en années pleines au jour de la rupture.
 * @param recoursValide true si le recours au CDI de chantier est valable.
 * @param motifRecours libellé synthétique de la validité du recours.
 * @param motifLicenciement qualification du motif du licenciement.
 * @param indemniteLicenciement indemnité légale de licenciement (R.1234-2).
 * @param procedureRequise true (procédure de licenciement de droit commun).
 * @param verdictGlobal verdict global de l'analyse.
 * @param consequences conséquences / points de vigilance identifiés.
 * @param motif libellé synthétique du motif retenu.
 * @param baseJuridique fondements juridiques applicables.
 */
public record CdiChantierResult(
        LocalDate dateEntree,
        LocalDate dateRupture,
        CdiChantierFondementRecours fondementRecours,
        CdiChantierSecteur secteur,
        boolean chantierAcheve,
        BigDecimal salaireMensuelMoyen,
        boolean reclassementAutreChantierPropose,
        int ancienneteAnnees,
        boolean recoursValide,
        String motifRecours,
        CdiChantierMotifLicenciement motifLicenciement,
        BigDecimal indemniteLicenciement,
        boolean procedureRequise,
        CdiChantierVerdict verdictGlobal,
        List<String> consequences,
        String motif,
        String baseJuridique
) {}

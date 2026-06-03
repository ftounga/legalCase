package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-218-37 : résultat interne business de l'analyse de monétisation de jours de
 * RTT (loi n° 2022-1157 du 16/08/2022 art. 5, F-DT-51). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param nombreJoursRttRenonces nombre de jours de RTT renoncés.
 * @param salaireJournalierBrut salaire journalier brut de référence.
 * @param tauxApplique taux de majoration effectivement appliqué (borné 10–25).
 * @param joursAcquisDansFenetre true si les jours sont acquis dans la fenêtre du
 *        dispositif (01/01/2022 → 31/12/2026).
 * @param montantBrut montant brut majoré de la monétisation (null si
 *        NON_ELIGIBLE).
 * @param regimeSocialFiscal régime social et fiscal applicable.
 * @param statut verdict d'éligibilité.
 * @param notes notes / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record RttMonetisationResult(
        int nombreJoursRttRenonces,
        BigDecimal salaireJournalierBrut,
        double tauxApplique,
        boolean joursAcquisDansFenetre,
        BigDecimal montantBrut,
        String regimeSocialFiscal,
        RttMonetisationStatut statut,
        List<String> notes,
        String baseJuridique
) {}

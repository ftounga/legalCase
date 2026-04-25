package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-FA-15-01 : résultat du calcul des récompenses (art. 1437 et 1469 Cciv).
 *
 * <p>Outil single-country FR. Conserve la totalité des éléments de calcul
 * pour persistance et restitution GET.
 */
public record RecompensesResult(
        String regimeMatrimonial,
        List<RecompenseDetail> recompenses,
        BigDecimal totalRecompensesDuesParCommunauteEur,
        BigDecimal totalRecompensesDuesParEpouxEur,
        BigDecimal soldeNetPourEpouxEur,
        String baseJuridiqueGenerale,
        List<String> messages
) {

    /**
     * Détail d'une opération calculée.
     *
     * @param operationId            identifiant fonctionnel relayé depuis l'input
     * @param type                   DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE / DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE
     * @param natureBien             ACQUISITION_BIEN_PROPRE / CONSERVATION_BIEN_PROPRE / AMELIORATION_BIEN_PROPRE / DEPENSES_USUELLES / AUTRE
     * @param regleApplicable        DEPENSE_FAITE (al. 1) / PLUS_FAIBLE_DEPENSE_OU_PROFIT (al. 2) / PROFIT_SUBSISTANT_PLUS_FORTE (al. 3)
     * @param depenseFaiteEur        montant de la dépense effectivement faite
     * @param profitSubsistantEur    montant du profit subsistant calculé (peut être null si non pertinent)
     * @param montantRecompenseEur   montant de la récompense due au final
     * @param directionRecompense    COMMUNAUTE_DOIT_PROPRE / PROPRE_DOIT_COMMUNAUTE
     * @param baseJuridiqueOperation référence d'article + alinéa
     * @param formule                formule lisible humainement
     * @param description            description libre relayée depuis l'input
     */
    public record RecompenseDetail(
            String operationId,
            String type,
            String natureBien,
            String regleApplicable,
            BigDecimal depenseFaiteEur,
            BigDecimal profitSubsistantEur,
            BigDecimal montantRecompenseEur,
            String directionRecompense,
            String baseJuridiqueOperation,
            String formule,
            String description
    ) {}
}

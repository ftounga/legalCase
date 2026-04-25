package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-FA-15-01 : payload du calcul des récompenses (art. 1437 et s. Cciv).
 */
public record RecompensesRequest(
        String regimeMatrimonial,
        List<OperationRecompense> operations
) {

    /**
     * Une opération unitaire à analyser.
     *
     * @param id                    identifiant fonctionnel libre fourni par le client (référence en sortie)
     * @param type                  DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE / DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE
     * @param natureBien            ACQUISITION_BIEN_PROPRE / CONSERVATION_BIEN_PROPRE / AMELIORATION_BIEN_PROPRE / DEPENSES_USUELLES / AUTRE
     * @param montantDepenseEur     montant de la dépense effectivement faite
     * @param valeurInitialeBienEur valeur du bien au jour de la dépense (utile pour le ratio acquisition/amélioration)
     * @param valeurActuelleBienEur valeur du bien au jour de la liquidation
     * @param description           libellé libre
     */
    public record OperationRecompense(
            String id,
            String type,
            String natureBien,
            BigDecimal montantDepenseEur,
            BigDecimal valeurInitialeBienEur,
            BigDecimal valeurActuelleBienEur,
            String description
    ) {}
}

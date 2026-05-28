package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-219-10 : résultat brut produit par {@link
 * DelegueSyndicalCct5Checker} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * DelegueSyndicalCct5Service} dans {@link
 * DelegueSyndicalCct5Response}).</p>
 *
 * <p>Outputs dérivés : éligibilité au statut DS CCT 5, missions
 * effectivement exerçables (selon CE/CPPT présents ou non), date de fin
 * de mandat indicative (calcul 4 ans = durée sectorielle standard).</p>
 */
public record DelegueSyndicalCct5Result(
        // Inputs (snapshot)
        LocalDate dateDesignation,
        int effectifEntreprise,
        int seuilSectorielRequis,
        boolean presenceOsRepresentative,
        DelegueSyndicalCct5StatutDesignation statutDesignation,
        boolean ceExistant,
        boolean cpptExistant,

        // Outputs
        DelegueSyndicalCct5Verdict verdict,
        boolean eligibleStatutDs,
        boolean entrepriseDansChamp,
        boolean designationReguliere,
        List<String> missionsExercables,
        LocalDate dateFinMandatIndicative,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}

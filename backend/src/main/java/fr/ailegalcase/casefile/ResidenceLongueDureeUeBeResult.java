package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-221-03 : résultat interne business du calcul d'éligibilité au statut de
 * RÉSIDENT LONGUE DURÉE UE (art. 15bis Loi 15/12/1980 — directive 2003/109/CE).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de F-IM-54 (carte B, séjour ILLIMITÉ
 * NATIONAL sans mobilité UE). Le statut résident longue durée UE ajoute des
 * conditions propres (ressources stables/suffisantes, assurance maladie, intégration)
 * ET confère la mobilité intra-UE : les deux outils ne chevauchent pas. Snapshot
 * complet pour restitution UI sans recalcul (pattern F-DT-42).
 */
public record ResidenceLongueDureeUeBeResult(
        LocalDate dateDebutSejourLegal,
        boolean sejourLegalIninterrompu,
        boolean ressourcesStablesSuffisantes,
        boolean assuranceMaladie,
        boolean conditionIntegrationRemplie,
        boolean absencesHorsUeExcessives,
        ResidenceLongueDureeUeBeVerdict verdict,
        int dureeSejourMois,
        int moisRestants,
        List<String> conditionsManquantes,
        List<String> basesJuridiques,
        List<String> messages
) {}

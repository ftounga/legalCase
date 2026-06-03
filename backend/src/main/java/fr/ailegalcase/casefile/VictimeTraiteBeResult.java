package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-221-06 : résultat interne business du titre de séjour « victime de la traite des
 * êtres humains » (BE — art. 61/2 et s. Loi 15/12/1980 ; circulaire du 26/09/2008).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b>, régime BE propre (3 phases) distinct du pendant FR
 * {@code F-IM-35-victime-traite-l4251-fr}. Snapshot complet (inputs + outputs) pour
 * restitution UI sans recalcul (pattern F-DT-42).
 */
public record VictimeTraiteBeResult(
        VictimeTraiteBePhase phaseProcedure,
        boolean ruptureAvecReseau,
        boolean cooperationJudiciaire,
        boolean accompagnementCentreSpecialise,
        LocalDate dateDebutAccompagnement,
        VictimeTraiteBeVerdict verdict,
        String etapeProcedure,
        List<String> basesJuridiques,
        List<String> messages
) {}

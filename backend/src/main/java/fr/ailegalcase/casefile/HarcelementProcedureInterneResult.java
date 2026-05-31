package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-27 : résultat interne business de l'analyse de conformité de la
 * procédure interne de traitement d'un signalement de harcèlement (art.
 * L.1153-5-1, L.2314-1, L.1152-4, L.4121-1 CT, F-DT-59). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise.
 * @param signalementRecu true si un signalement a été reçu.
 * @param checklist items de conformité (référent CSE, référent employeur,
 *        affichage, enquête, contradictoire, mesures conservatoires).
 * @param delaiReactionJours nombre de jours entre signalement et ouverture de
 *        l'enquête (null si une des dates manque).
 * @param delaiRaisonnable appréciation du délai de réaction.
 * @param itemsObligatoiresManquants nombre d'items obligatoires non conformes.
 * @param statut verdict global de conformité.
 * @param risqueResponsabiliteEmployeur niveau de risque pour l'employeur.
 * @param consequences conséquences / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record HarcelementProcedureInterneResult(
        int effectif,
        boolean signalementRecu,
        List<HarcelementChecklistItem> checklist,
        Integer delaiReactionJours,
        HarcelementProcedureInterneDelaiRaisonnable delaiRaisonnable,
        int itemsObligatoiresManquants,
        HarcelementProcedureInterneVerdict statut,
        HarcelementProcedureInterneRisque risqueResponsabiliteEmployeur,
        List<String> consequences,
        String baseJuridique
) {}

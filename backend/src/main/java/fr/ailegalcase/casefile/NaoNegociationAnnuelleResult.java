package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-29 : résultat interne business de l'analyse de conformité de la
 * négociation annuelle obligatoire (NAO, art. L.2242-1 à L.2242-8 CT, F-DT-66).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise.
 * @param delegueSyndicalPresent true si au moins un délégué syndical est désigné.
 * @param applicable true si l'obligation de NAO est déclenchée (DS présent).
 * @param checklist items de conformité (blocs de négociation, périodicité, PV de
 *        désaccord).
 * @param periodiciteMois périodicité retenue en mois.
 * @param dateProchaineEcheance date de la prochaine échéance de négociation (null
 *        si {@code dateDerniereNegociation} absente).
 * @param joursAvantEcheance nombre de jours avant la prochaine échéance (peut être
 *        négatif ; null si non calculable).
 * @param statutEcheance statut de l'échéance (A_JOUR / ECHEANCE_PROCHE / DEPASSEE ;
 *        null si non calculable).
 * @param itemsObligatoiresManquants nombre d'items obligatoires non conformes.
 * @param statut verdict global de conformité.
 * @param risqueEntrave niveau de risque d'entrave / de sanction.
 * @param consequences conséquences / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record NaoNegociationAnnuelleResult(
        int effectif,
        boolean delegueSyndicalPresent,
        boolean applicable,
        List<NaoChecklistItem> checklist,
        int periodiciteMois,
        LocalDate dateProchaineEcheance,
        Integer joursAvantEcheance,
        NaoStatutEcheance statutEcheance,
        int itemsObligatoiresManquants,
        NaoNegociationAnnuelleStatut statut,
        NaoRisqueEntrave risqueEntrave,
        List<String> consequences,
        String baseJuridique
) {}

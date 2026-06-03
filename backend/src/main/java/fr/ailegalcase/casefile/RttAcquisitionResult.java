package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-49 : résultat interne business de l'outil "RTT — acquisition selon
 * accord d'aménagement" (art. L.3121-41 à L.3121-44 CT, F-DT-80). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param statut verdict (CALCULE / RENVOI_HEURES_SUP).
 * @param horaireHebdomadaireCollectif horaire hebdomadaire collectif saisi.
 * @param accordCollectifPresent un accord d'aménagement est présent.
 * @param semainesTravailleesAn nombre de semaines travaillées dans l'année retenu.
 * @param nombreJrttTheorique nombre théorique de JRTT acquis sur l'année (sans
 *        majoration), null si renvoi heures supplémentaires.
 * @param base description de la base de calcul (horaire, semaines, sans majoration).
 * @param notes notes / points de vigilance.
 * @param baseJuridique fondements juridiques applicables.
 */
public record RttAcquisitionResult(
        RttAcquisitionStatut statut,
        double horaireHebdomadaireCollectif,
        boolean accordCollectifPresent,
        int semainesTravailleesAn,
        Double nombreJrttTheorique,
        String base,
        List<String> notes,
        String baseJuridique
) {}

package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-IM-20-01 : requête pour l'analyse de légalité d'une mesure d'éloignement administrative
 * française autre que l'OQTF (CESEDA L.631+, L.612+, L.222+).
 *
 * <p>Outil <b>single-country FR</b>. Couvre 5 dispositifs :
 * EXPULSION_PREFECTORALE / EXPULSION_MINISTERIELLE / EXPULSION_SECURITE_ETAT / IRTF / IAT.
 * L'équivalent belge (Loi 15/12/1980 art. 20-21, 74/15) sera traité par F-IM-20-BE (backlog).
 *
 * @param dispositif                       enum dispositif visé (5 valeurs)
 * @param motifMenace                      enum motif de la menace (ORDRE_PUBLIC / SECURITE_ETAT
 *                                         / TERRORISME / RECIDIVE_GRAVE / AUTRE)
 * @param procedureCommissionRespectee     procédure CESED L.632-1 (commission expulsion)
 *                                         respectée — Boolean tristate (default true)
 * @param urgenceAbsolueJustifiee          urgence absolue dérogeant à la commission
 * @param dureeCircularitePrecaire         durée du séjour précaire en mois (IRTF)
 * @param dureePresenceIrreguliereMois     durée présence irrégulière en mois (IRTF)
 * @param comportementAggravant            comportement justifiant le prolongement (IRTF)
 * @param recoursDelai                     date prévue ou effective du dépôt du recours
 */
public record MesuresEloignementRequest(
        String dispositif,
        String motifMenace,
        Boolean procedureCommissionRespectee,
        Boolean urgenceAbsolueJustifiee,
        Integer dureeCircularitePrecaire,
        Integer dureePresenceIrreguliereMois,
        Boolean comportementAggravant,
        LocalDate recoursDelai
) {}

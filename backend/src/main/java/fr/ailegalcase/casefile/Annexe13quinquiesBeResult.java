package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-215-17 : résultat interne business du calcul lié à l'Annexe 13quinquies
 * (OQT + interdiction d'entrée — art. 74/11 Loi 15/12/1980).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-08 (Annexe 13 simple — OQT sans interdiction d'entrée) ;</li>
 *   <li>F-IM-31 (recours en annulation 30 jours calendaires, SF-215-13) ;</li>
 *   <li>F-IM-32 (recours en extrême urgence — 5 jours ouvrables, SF-215-15).</li>
 * </ul>
 */
public record Annexe13quinquiesBeResult(
        LocalDate dateNotificationAnnexe,
        Annexe13quinquiesBeMotifEnum motifInterdictionEntree,
        boolean precedentSejour,
        int dureeInterdiction,
        LocalDate dateFinInterdiction,
        LocalDate datePossibleLevePrecoce,
        LocalDate dateLimiteRecoursAnnulation,
        long joursRestantsRecours,
        boolean recoursForme,
        LocalDate dateRecours,
        Annexe13quinquiesBeStatut statutRecours,
        List<String> conditionsLevee,
        String recommandation,
        String baseJuridique
) {}

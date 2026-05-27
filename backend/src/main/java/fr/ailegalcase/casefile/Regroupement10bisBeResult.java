package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-215-05 : résultat interne business du calcul d'éligibilité au regroupement
 * familial art. 10 et 10bis Loi 15/12/1980 (ressortissant tiers en séjour limité
 * — carte A).
 *
 * <p>Différence majeure vs {@link Regroupement10terBeResult} : champ
 * {@code dateFinCarteA} (date d'expiration du titre A du regroupant) et flag
 * dérivé {@code conditionTitreEnCours} (true si dateFinCarteA &gt;= aujourd'hui).
 * Si {@code conditionTitreEnCours} est false, le verdict est forcé INELIGIBLE
 * peu importe le score (règle dure : titre A expiré → pas de regroupement).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>SF-215-03 (10ter — regroupant en séjour illimité, carte B ou C) ;</li>
 *   <li>F-IM-14 40ter (regroupant Belge) ;</li>
 *   <li>regroupement familial FR (CESEDA L. 434-2 et seq., outils F-IM-XX).</li>
 * </ul>
 */
public record Regroupement10bisBeResult(
        Regroupement10bisBeLienFamilialEnum lienFamilial,
        Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
        int revenusMensuelsNetsRegroupant,
        int dureeSejour,
        LocalDate dateFinCarteA,
        boolean conditionTitreEnCours,
        boolean logementConforme,
        boolean assuranceMaladie,
        boolean menaceOrdrePublic,
        int seuilRessources,
        int differentielRevenus,
        int scoreEligibilite,
        Regroupement10bisBeVerdict verdict,
        List<String> criteresNonRemplis,
        String baseJuridique
) {}

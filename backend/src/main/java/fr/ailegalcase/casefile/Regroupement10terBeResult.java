package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-215-03 : résultat interne business du calcul d'éligibilité au regroupement
 * familial art. 10 et 10ter Loi 15/12/1980 (ressortissant tiers — carte B ou C).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distincte de :
 * <ul>
 *   <li>SF-215-05 (10bis — regroupant en séjour limité) ;</li>
 *   <li>F-IM-14 40ter (regroupant Belge) ;</li>
 *   <li>regroupement familial FR (CESEDA L. 434-2 et seq., outils F-IM-XX).</li>
 * </ul>
 */
public record Regroupement10terBeResult(
        Regroupement10terBeLienFamilialEnum lienFamilial,
        Regroupement10terBeTypeCarteEnum typeCarteRegroupant,
        int revenusMensuelsNetsRegroupant,
        int dureeSejour,
        boolean logementConforme,
        boolean assuranceMaladie,
        boolean menaceOrdrePublic,
        int seuilRessources,
        int differentielRevenus,
        int scoreEligibilite,
        Regroupement10terBeVerdict verdict,
        List<String> criteresNonRemplis,
        String baseJuridique
) {}

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-215-19 : résultat interne business de l'outil "Protection temporaire Ukraine BE"
 * (F-IM-34) — directive 2001/55/CE activée par la décision UE 2022/382 du 04/03/2022,
 * prolongée annuellement (toujours active en 2026) ; transposition belge : Loi du
 * 15/12/1980 art. 57/29 et suivants.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>la protection subsidiaire art. 48/4 Loi 15/12/1980 (F-221) ;</li>
 *   <li>la demande d'asile / protection internationale CGRA.</li>
 * </ul>
 *
 * <p>Outil de type information + checklist + calcul de durée (pas de logique de délai
 * de recours).
 */
public record ProtectionTemporaireUkraineBeResult(
        LocalDate dateArrivee,
        boolean nationaliteUkrainienne,
        boolean residenceUkraineAvant24Fev2022,
        boolean apatridesUkraine,
        boolean membreFamilleProtege,
        ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE,
        boolean eligible,
        LocalDate dateFinProtection,
        long dureeProtectionRestante,
        boolean prochainRenouvellement,
        String droitsTravail,
        List<String> droitsAides,
        List<String> cheminProcedure,
        String recommandation,
        String baseJuridique
) {}

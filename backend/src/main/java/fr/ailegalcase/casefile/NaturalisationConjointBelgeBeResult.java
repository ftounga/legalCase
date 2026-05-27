package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-215-09 : résultat interne business du calcul d'éligibilité à la déclaration de
 * nationalité belge par mariage avec un(e) Belge — art. 16 Code de la nationalité belge.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>SF-215-07 (F-IM-28 naturalisation 12bis BE — résidence 5/10 ans) ;</li>
 *   <li>F-IM-13 (naturalisation FR — Cciv) ;</li>
 *   <li>F-221 (naturalisation par décret art. 19-21 CNB — P3).</li>
 * </ul>
 *
 * <p>{@code dureeManquante} : nombre de mois restants à attendre pour atteindre les
 * 60 mois (5 ans) de cohabitation requis par l'art. 16 §1 2°. Vaut 0 si la condition
 * de durée est satisfaite (l'éligibilité peut toutefois être {@link
 * NaturalisationConjointBelgeBeVerdict#INELIGIBLE INELIGIBLE} pour un autre motif —
 * langue, intégration, ordre public, condamnation).
 */
public record NaturalisationConjointBelgeBeResult(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateMarriage,
        boolean cohabitationLegale,
        int dureeCohabitationMois,
        NaturalisationBeNiveauLangueEnum niveauLangue,
        boolean preuveIntegration,
        boolean menaceOrdrePublic,
        boolean condamnationPenale,
        boolean eligible,
        NaturalisationConjointBelgeBeVerdict verdict,
        int dureeManquante,
        List<String> criteresNonRemplis,
        String delaiDeclaration,
        String baseJuridique
) {}

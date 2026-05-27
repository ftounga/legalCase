package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-215-07 : résultat interne business du calcul d'éligibilité à la déclaration
 * de nationalité belge art. 12bis CNB (loi 28/06/1984 consolidée).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-13-naturalisation (FR — Cciv art. 21-15 et seq., 6 voies FR) ;</li>
 *   <li>SF-215-09 (naturalisation conjoint Belge art. 16 CNB) ;</li>
 *   <li>F-221 (naturalisation par décret art. 19-21 CNB — très rare, P3).</li>
 * </ul>
 *
 * <p>{@code dureeManquante} : nombre de mois restants à attendre pour que l'une
 * des deux voies devienne accessible, calculé uniquement quand toutes les autres
 * conditions sont remplies (séjour illimité + langue ≥ A2 + pas de menace OP
 * + pas de condamnation). Vaut 0 si voie éligible ou si une condition non-temporelle
 * bloque définitivement la déclaration.
 */
public record Naturalisation12bisBeResult(
        int dureeSejour,
        NaturalisationBeTypeSejourEnum typeSejour,
        NaturalisationBeNiveauLangueEnum niveauLangue,
        boolean preuveIntegration,
        boolean preuveEmploi,
        boolean menaceOrdrePublic,
        boolean condamnationPenale,
        boolean voie5Ans,
        boolean voie10Ans,
        Naturalisation12bisBeVoieEligible voieEligible,
        int dureeManquante,
        List<String> criteresNonRemplis,
        String procedureReference,
        String baseJuridique
) {}

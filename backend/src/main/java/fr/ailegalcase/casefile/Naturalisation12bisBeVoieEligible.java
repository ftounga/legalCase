package fr.ailegalcase.casefile;

/**
 * SF-215-07 : voie d'éligibilité retenue pour la déclaration de nationalité belge
 * art. 12bis CNB (loi 28/06/1984 consolidée).
 *
 * <p>Le calculateur {@link Naturalisation12bisBeCalculator} priorise la voie 5 ans
 * sur la voie 10 ans lorsque les deux sont satisfaites (la voie 5 ans est
 * strictement plus exigeante via la condition supplémentaire d'intégration / emploi
 * — si elle est satisfaite, la voie 10 ans l'est nécessairement).
 *
 * <ul>
 *   <li>VOIE_5_ANS : dureeSejour ≥ 60 mois + séjour illimité + niveau langue ≥ A2
 *       + (preuve intégration OU preuve emploi) + !menaceOrdrePublic + !condamnationPenale.</li>
 *   <li>VOIE_10_ANS : dureeSejour ≥ 120 mois + séjour illimité + niveau langue ≥ A2
 *       + !menaceOrdrePublic + !condamnationPenale (sans condition intégration/emploi).</li>
 *   <li>AUCUNE : aucune des deux voies n'est satisfaite — la déclaration 12bis
 *       n'est pas recevable en l'état.</li>
 * </ul>
 */
public enum Naturalisation12bisBeVoieEligible {
    VOIE_5_ANS,
    VOIE_10_ANS,
    AUCUNE
}

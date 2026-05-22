package fr.ailegalcase.casefile;

/**
 * SF-216-11 : body POST /api/v1/case-files/{id}/retrait-autorite-parentale.
 *
 * <p>Outil single-country FRANCE — art. 378-381 Cciv (retrait total ou partiel
 * d'autorité parentale) + loi n°2022-140 du 7 février 2022 LMVSS (suspension
 * accélérée si violences conjugales en présence de l'enfant).</p>
 *
 * <p>Le service rejette en 400 : pays ≠ FRANCE, age &lt; 0 ou age &gt; 18,
 * typeRetrait manquant, motifRetrait manquant.</p>
 */
public record RetraitAutoriteParentaleRequest(
        TypeRetraitApEnum typeRetrait,
        MotifRetraitApEnum motifRetrait,
        Boolean condamnationPenaleDetectee,
        Boolean dangerCaracterise,
        Boolean violencesConjugalesDetectees,
        Integer ageEnfant,
        Boolean decisionsJudiciairesPrecedentes
) {}

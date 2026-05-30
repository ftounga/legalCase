package fr.ailegalcase.casefile;

/**
 * SF-218-05 : analyse d'un cas d'ouverture invoqué au soutien du pourvoi en
 * cassation sociale. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param cas cas d'ouverture invoqué.
 * @param libelle libellé lisible du cas d'ouverture.
 * @param baseJuridique fondement textuel du cas d'ouverture.
 * @param forceProbatoire force probatoire (FORTE / MOYENNE / FAIBLE) — pondère
 *        le risque de non-admission (art. 1014 CPC).
 */
public record PourvoiCassationSocCasOuvertureAnalyse(
        PourvoiCassationSocCasOuverture cas,
        String libelle,
        String baseJuridique,
        PourvoiCassationSocForce forceProbatoire
) {}

package fr.ailegalcase.casefile;

/**
 * SF-207-02 : énumération des 10 mentions obligatoires du document C4 ONEM
 * (formulaire de fin de contrat employeur belge) au sens de l'AR du 25 novembre
 * 1991 portant réglementation du chômage, art. 92.
 *
 * <p>Le NRN (numéro national de registre) n'est <b>pas</b> dans la liste des
 * mentions obligatoires — il peut être masqué pour des raisons RGPD ; il n'est
 * donc pas vérifié par le {@link C4OnemChecklistCalculator}.</p>
 *
 * <p>Substance juridique BE-only — pas de calque FR (loi du 3 juillet 1978 et
 * AR du 25/11/1991 art. 92 en référence ; l'équivalent FR — attestation France
 * Travail R.1234-9 — est un régime distinct géré par
 * {@link DocumentsFinContratCalculator}).</p>
 */
public enum C4OnemChecklistMention {

    /** Raison sociale (dénomination juridique) de l'employeur. */
    RAISON_SOCIALE_EMPLOYEUR,

    /** Numéro BCE (Banque-Carrefour des Entreprises) — exactement 10 chiffres. */
    NUMERO_BCE,

    /** Nom du salarié. */
    NOM_SALARIE,

    /** Date d'entrée en service. */
    DATE_ENTREE_SERVICE,

    /** Date de sortie de service — doit être ≥ date d'entrée. */
    DATE_SORTIE_SERVICE,

    /** Catégorie / code ONEM (ex. "9" pour faute grave). */
    CATEGORIE_ONEM,

    /** Motif explicite de la fin de contrat — longueur ≥ 5 caractères. */
    MOTIF_EXPLICITE,

    /** Période d'occupation cohérente : date de sortie ≥ date d'entrée. */
    PERIODE_OCCUPATION_COHERENTE,

    /** Préavis presté précisé (sauf si faute grave). */
    PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE,

    /** Dernier salaire mensuel brut indiqué. */
    DERNIER_SALAIRE_INDIQUE
}

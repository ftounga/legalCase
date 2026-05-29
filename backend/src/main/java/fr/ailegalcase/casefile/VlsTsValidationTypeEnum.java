package fr.ailegalcase.casefile;

/**
 * SF-214-07 : type de VLS-TS (visa long séjour valant titre de séjour) soumis à
 * validation OFII (art. R. 311-3 CESEDA). Le type n'affecte pas le calcul du
 * délai de 3 mois — il est conservé à titre informatif et de contexte.
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>.
 */
public enum VlsTsValidationTypeEnum {
    ETUDIANT,
    SALARIE,
    VISITEUR,
    CONJOINT_FRANCAIS,
    AUTRE
}

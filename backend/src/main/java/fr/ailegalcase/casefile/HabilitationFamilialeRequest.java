package fr.ailegalcase.casefile;

/**
 * SF-222-03 : body POST /api/v1/case-files/{id}/habilitation-familiale-analysis.
 *
 * <p>Outil décisionnel habilitation familiale (art. 494-1 à 494-12 Cciv),
 * Famille FRANCE uniquement. Évalue les conditions propres de l'habilitation
 * familiale, alternative simplifiée à une mesure judiciaire de protection
 * lorsqu'un consensus familial existe.</p>
 *
 * <p>Champs :</p>
 * <ul>
 *   <li>{@code alterationFacultesMedicalementConstatee} — altération des facultés
 *       mentales ou corporelles médicalement constatée (art. 425 / 494-1 Cciv) ;</li>
 *   <li>{@code lienFamilialEligible} — lien familial du demandeur avec le majeur :
 *       ASCENDANT / DESCENDANT / FRERE_SOEUR / CONJOINT_PARTENAIRE / AUTRE ;</li>
 *   <li>{@code consensusFamilial} — absence d'opposition d'un proche (consensus
 *       familial requis, art. 494-1 Cciv) ;</li>
 *   <li>{@code besoinActesPatrimoniaux} — besoin d'actes patrimoniaux ;</li>
 *   <li>{@code besoinActesPersonnels} — besoin d'actes relatifs à la personne ;</li>
 *   <li>{@code protectionPonctuelleOuGenerale} — étendue : PONCTUELLE (actes
 *       déterminés → habilitation spéciale) ou GENERALE (habilitation générale).</li>
 * </ul>
 *
 * <p>Anti-doublon F-FA-25 (sélecteur de régime de protection) : cet outil ne
 * re-sélectionne pas le régime ; il cadre les conditions PROPRES de
 * l'habilitation familiale. En cas d'absence de consensus ou de lien inéligible,
 * il renvoie vers F-FA-25 (curatelle / tutelle).</p>
 */
public record HabilitationFamilialeRequest(
        Boolean alterationFacultesMedicalementConstatee,
        LienFamilialHabilitationEnum lienFamilialEligible,
        Boolean consensusFamilial,
        Boolean besoinActesPatrimoniaux,
        Boolean besoinActesPersonnels,
        EtendueHabilitationEnum protectionPonctuelleOuGenerale
) {}

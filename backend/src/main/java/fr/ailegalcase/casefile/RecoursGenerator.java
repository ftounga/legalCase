package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * Routeur entre les générateurs spécialisés de recours (F-133).
 *
 * <p>Cette classe ne contient plus de logique métier depuis SF-133-01.
 * Elle résout le {@link RecoursType} depuis le référentiel, puis délègue
 * à l'outil décisionnel approprié selon le code de recours :
 * <ul>
 *   <li>{@code RECOURS_GRACIEUX_PREFET}, {@code RECOURS_CONTENTIEUX_TA}
 *       → {@link RecoursGeneratorFrance}</li>
 *   <li>{@code RECOURS_CNDA} → {@link RecoursGeneratorCnda}</li>
 *   <li>{@code RECOURS_CGRA}, {@code RECOURS_CCE}, {@code RECOURS_CE_BELGIQUE}
 *       → {@link RecoursGeneratorBelgique}</li>
 * </ul>
 *
 * <p>La signature publique est préservée pour ne pas impacter les consumers
 * existants ({@link ImmigrationRecoursService}).
 */
public final class RecoursGenerator {

    private RecoursGenerator() {}

    public static GeneratedRecours generate(
            String recoursTypeCode,
            LocalDate dateNotification,
            String requerantNom,
            String requerantPrenom,
            String requerantNationalite,
            String requerantAdresse,
            String autoriteDecision,
            LocalDate dateDecision,
            String referenceDecision,
            String exposeFaits
    ) {
        RecoursType type = ImmigrationRecoursReferentiel.getByCode(recoursTypeCode);
        return generate(type, recoursTypeCode, dateNotification, requerantNom, requerantPrenom,
                requerantNationalite, requerantAdresse, autoriteDecision, dateDecision, referenceDecision, exposeFaits);
    }

    public static GeneratedRecours generate(
            RecoursType type,
            String recoursTypeCode,
            LocalDate dateNotification,
            String requerantNom, String requerantPrenom,
            String requerantNationalite, String requerantAdresse,
            String autoriteDecision, LocalDate dateDecision,
            String referenceDecision, String exposeFaits
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Type de recours inconnu : " + recoursTypeCode);
        }
        return switch (recoursTypeCode) {
            case "RECOURS_GRACIEUX_PREFET", "RECOURS_CONTENTIEUX_TA" ->
                    RecoursGeneratorFrance.generate(type, recoursTypeCode, dateNotification,
                            requerantNom, requerantPrenom, requerantNationalite, requerantAdresse,
                            autoriteDecision, dateDecision, referenceDecision, exposeFaits);
            case "RECOURS_CNDA" ->
                    RecoursGeneratorCnda.generate(type, recoursTypeCode, dateNotification,
                            requerantNom, requerantPrenom, requerantNationalite, requerantAdresse,
                            autoriteDecision, dateDecision, referenceDecision, exposeFaits);
            case "RECOURS_CGRA", "RECOURS_CCE", "RECOURS_CE_BELGIQUE" ->
                    RecoursGeneratorBelgique.generate(type, recoursTypeCode, dateNotification,
                            requerantNom, requerantPrenom, requerantNationalite, requerantAdresse,
                            autoriteDecision, dateDecision, referenceDecision, exposeFaits);
            default ->
                    throw new IllegalArgumentException("Type de recours inconnu : " + recoursTypeCode);
        };
    }
}

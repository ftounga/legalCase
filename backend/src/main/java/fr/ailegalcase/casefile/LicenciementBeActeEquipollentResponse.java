package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-213-09 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 (miroir
 * {@link LicenciementBeProtectionDelegueeResponse} SF-213-08).</p>
 */
public record LicenciementBeActeEquipollentResponse(
        UUID caseFileId,

        TypeModificationUnilateraleEnum typeModification,
        AmpleurModificationEnum ampleurModification,
        boolean elementEssentielDuContrat,
        LocalDate dateModification,
        boolean salarieAProteste,
        Integer delaiDepuisModificationJours,
        BigDecimal remunerationHebdomadaireBrute,
        Integer dureePreavisCalculeeSemaines,

        LicenciementBeActeEquipollentVerdict verdict,
        String fondementJuridique,
        BigDecimal icpIndicatif,
        boolean risqueAcceptationTacite,
        int delaiRecommandeProtestationJours,
        String baseJuridique,
        String avertissement
) {
}

package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * SF-213-04 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE (miroir
 * {@link LicenciementBeStatutUniquePreavisResponse} SF-213-03).</p>
 */
public record LicenciementBeFormuleClaeysResponse(
        UUID caseFileId,
        int ancienneteAnneesPreStatutUnique,
        int ancienneteMoisPreStatutUnique,
        BigDecimal remunerationAnnuelleBruteEnMilliers,
        boolean appliquerClauseSauvegarde,
        Integer ancienneteAnneesPostStatutUnique,
        BigDecimal salaireHebdomadaireBrut,
        BigDecimal preavisClaeysMois,
        int preavisClaeysSemaines,
        int preavisStatutUniquesSemaines,
        int preavisTotalSemaines,
        BigDecimal indemniteClaeysBrute,
        BigDecimal indemniteTotaleBrute,
        String formuleClaeys,
        String baseJuridique,
        String avertissement
) {}

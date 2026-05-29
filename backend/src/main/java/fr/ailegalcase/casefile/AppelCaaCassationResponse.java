package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-33 : réponse de l'analyse des délais d'appel CAA / cassation CE en
 * contentieux des étrangers (art. L. 811-1 / R. 811-1 et L. 821-1 / R. 821-1 CJA).
 * Outil single-country FR.
 */
public record AppelCaaCassationResponse(
        UUID caseFileId,
        LocalDate dateJugementTA,
        AppelCaaCassationTypeDecisionEnum typeDecisionTA,
        AppelCaaCassationTypeContentieuxEnum typeContentieux,
        boolean delaiSpecialOQTF,
        LocalDate dateEcheanceAppelCaa,
        long joursRestantsAppel,
        String courAppelCompetente,
        List<String> motifsAppelPossibles,
        boolean filtrePourvoisCassation,
        int delaiCassationCeMois,
        AppelCaaCassationStatut statut,
        String recommandation,
        String country,
        String baseJuridique
) {}

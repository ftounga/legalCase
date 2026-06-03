package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-221-01 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/carte-a-prorogation-be-analysis}.
 */
public record CarteAProrogationBeResponse(
        UUID caseFileId,
        LocalDate dateExpirationCarteA,
        boolean motifSejourPersiste,
        boolean conditionsInitialesToujoursReunies,
        boolean demandeDeposee,
        LocalDate dateDemande,
        CarteAProrogationBeVerdict verdict,
        long joursAvantExpiration,
        LocalDate dateOuvertureFenetre,
        LocalDate dateLimiteRecommandee,
        List<String> basesJuridiques,
        List<String> messages
) {}

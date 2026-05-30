package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-15 : réponse de l'analyse du statut de journaliste professionnel lors
 * d'une rupture — clause de cession / conscience, indemnité de congédiement,
 * commission arbitrale (art. L.7111-1 et s. CT). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record JournalisteStatutResponse(
        UUID caseFileId,
        LocalDate dateEntree,
        LocalDate dateRupture,
        JournalisteStatutTypeRupture typeRupture,
        BigDecimal salaireMensuelMoyen,
        boolean carteIdentiteProfessionnelle,
        int ancienneteAnnees,
        JournalisteStatutQualification statutJournaliste,
        JournalisteStatutClauseValidite clauseValide,
        String motifClause,
        BigDecimal indemniteCongediement,
        boolean commissionArbitraleRequise,
        String noteCommissionArbitrale,
        JournalisteStatutVerdict verdictGlobal,
        String country,
        String baseJuridique
) {}

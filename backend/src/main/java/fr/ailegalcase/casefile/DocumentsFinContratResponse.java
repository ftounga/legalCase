package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-32-01 : réponse de l'endpoint de conformité documents de fin de contrat.
 *
 * <p>Inclut snapshot des inputs (pré-remplissage UI) ET sorties calculées.</p>
 */
public record DocumentsFinContratResponse(
        UUID caseFileId,
        // --- Inputs (snapshot) ---
        LocalDate dateFinContrat,
        Boolean certificatTravailRemis,
        LocalDate dateCertificatTravail,
        Boolean attestationFranceTravailRemise,
        LocalDate dateAttestationFranceTravail,
        Boolean souldeToutCompteSigne,
        LocalDate dateSouldeToutCompte,
        BigDecimal salaireMensuelBrutEur,
        // --- Outputs ---
        boolean certificatRemisDansDelai,
        boolean attestationRemiseDansDelai,
        boolean souldeToutCompteValide,
        boolean souldeToutCompteContestable,
        int totalSanctionsCumulables,
        BigDecimal indemniteRetardCertificatEur,
        BigDecimal indemniteRetardAttestationEur,
        int scoreConformiteEmployeur,
        DocumentsFinContratCalculator.VerdictRisqueContentieux verdictRisqueContentieux,
        String baseJuridique,
        String formule,
        List<String> messages,
        List<String> messagesContentieux,
        String country
) {}

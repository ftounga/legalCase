package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AesEtudiantResponse(
        UUID caseFileId,
        LocalDate dateEntreeFrance,
        int dureePresenceMois,
        int anneesScolariteEnFranceConsecutives,
        String niveauEtudesActuel,
        String resultatsAcademiques,
        boolean inscriptionEtablissementReconnu,
        boolean moyensSubsistance,
        boolean menaceOrdrePublic,
        boolean parcoursCoherent,
        LocalDate dateDepotDemande,
        String country,
        boolean presence3AnsOk,
        boolean scolarite2AnsConsecutivesOk,
        boolean resultatsAcceptables,
        boolean inscriptionValide,
        boolean moyensOk,
        boolean pasMenace,
        boolean parcoursCoherentOk,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionSiDemande,
        String formule,
        String baseJuridique,
        List<String> messages
) {}

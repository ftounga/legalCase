package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-09-04 : résultat de l'analyse AES voie étudiante (régularisation d'un étranger
 * présent en France et poursuivant des études sans titre adapté). Outil single-country FR —
 * basé sur la circulaire Valls du 28/11/2012 (point III) actualisée (Darmanin 2024) et
 * interprétation jurisprudentielle constante (art. L.412-1 CESEDA).
 */
public record AesEtudiantResult(
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

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-09-02 : résultat de l'analyse AES voie familiale (art. L.435-1 CESEDA
 * + circulaire Valls 28/11/2012). Outil single-country FR — pas d'équivalent BE.
 */
public record AesFamilleResult(
        LocalDate dateEntreeFrance,
        int dureePresenceMois,
        boolean conjointFrancaisOuRegulier,
        int enfantsScolarisesFrance,
        int dureeScolaritePlusAncienEnfantAnnees,
        boolean preuvesInsertion,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        boolean presence5AnsOk,
        boolean presence10AnsOk,
        boolean liensFamiliauxOk,
        boolean insertionOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionSiDemande,
        String formule,
        String baseJuridique,
        List<String> messages
) {}

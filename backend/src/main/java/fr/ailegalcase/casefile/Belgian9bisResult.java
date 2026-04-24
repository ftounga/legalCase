package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-14-01 : résultat de l'analyse 9bis humanitaire BE
 * (art. 9bis Loi 15/12/1980 + AR 17/05/2007). Outil single-country BELGIQUE.
 */
public record Belgian9bisResult(
        LocalDate dateEntreeBelgique,
        int dureePresenceMois,
        boolean circonstancesExceptionnelles,
        boolean liensFamiliauxBe,
        boolean liensProfessionnels,
        boolean scolariteEnfantsBe,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        boolean presence3AnsOk,
        boolean liensConstitutifsOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabilite,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionPrevisionnelle,
        String formule,
        String baseJuridique,
        List<String> messages
) {}

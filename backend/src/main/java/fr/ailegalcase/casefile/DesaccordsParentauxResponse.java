package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-19-05 : réponse de l'API désaccords parentaux (art. 373-2-10 Cciv).
 */
public record DesaccordsParentauxResponse(
        UUID caseFileId,
        String domaineDesaccord,
        String intensiteDesaccord,
        List<String> tentativesMediation,
        List<Integer> ageEnfantsConcernes,
        boolean interetSuperieurInvoque,
        boolean expertiseDejaRealisee,
        boolean urgence,
        LocalDate dateRequete,
        int scoreEligibiliteJaf,
        String verdictProbabiliteAcceptation,
        boolean mediationPrealableRecommandee,
        boolean auditionEnfantsRecommandee,
        boolean expertisePsyRecommandee,
        int delaiTraitementJoursPrevisionnel,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MesuresEloignementResponse(
        UUID caseFileId,
        String country,
        String dispositif,
        String dispositifRecommande,
        String motifMenace,
        Boolean procedureCommissionRespectee,
        Boolean urgenceAbsolueJustifiee,
        Integer dureeCircularitePrecaire,
        Integer dureePresenceIrreguliereMois,
        Boolean comportementAggravant,
        LocalDate recoursDelai,
        String verdictLegalite,
        List<String> risqueAnnulation,
        int delaiRecoursJours,
        String juridictionRecours,
        List<String> documentsRequis,
        String baseJuridique,
        String formule,
        List<String> messages
) {}

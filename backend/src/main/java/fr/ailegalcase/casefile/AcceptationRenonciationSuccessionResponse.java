package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AcceptationRenonciationSuccessionResponse(
        UUID caseFileId,
        LocalDate dateOuvertureSuccession,
        String qualiteHeritier,
        double actifBrutEur,
        double passifEur,
        boolean actesEquivalentAcceptationDejaPosesDetected,
        boolean inventaireRealise,
        boolean dettesIncertainesDetected,
        String intentionExprimee,
        List<String> optionsOuvertes,
        String optionRecommandee,
        int delaiRestantJours,
        int delaiTotalJours,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}

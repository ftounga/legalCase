package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record Annexe13BeRequest(
        LocalDate dateNotificationAnnexe13,
        Integer delaiDepartImposeJours,
        String motifOqt,
        Boolean transfertImminent,
        Boolean recoursForme,
        LocalDate dateRecours,
        String typeRecours
) {}

package fr.ailegalcase.casefile;

public record ImmigrationTitleDecisionRequest(
        String country,
        boolean nationaliteUe,
        String motif,
        String duree,
        String situationFamiliale
) {}

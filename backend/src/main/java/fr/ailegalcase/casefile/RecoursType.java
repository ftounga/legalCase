package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Type de recours immigration avec ses caractéristiques légales.
 */
public record RecoursType(
        String code,
        String label,
        String country,
        int delaiJours,
        String juridiction,
        List<String> textesApplicables,
        List<String> sections,
        List<String> piecesStandard
) {}

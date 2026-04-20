package fr.ailegalcase.referential;

public record ReferentialUpdateRequest(
        String label,
        String valueJson,
        boolean force,
        /** SF-140-03 : description métier optionnelle (null = pas modifiée). */
        String description
) {}

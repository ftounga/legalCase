package fr.ailegalcase.referential;

public record ReferentialUpdateRequest(
        String label,
        String valueJson,
        boolean force
) {}

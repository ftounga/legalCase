package fr.ailegalcase.referential;

public record ReferentialUpdateResponse(
        boolean saved,
        ReferentialResponse.Entry entry,
        String warning
) {}

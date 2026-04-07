package fr.ailegalcase.casefile;

import java.util.Map;

public record LicenciementRequest(
        String country,
        Map<String, String> reponses
) {}

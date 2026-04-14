package fr.ailegalcase.casefile;

import java.util.Map;

public record RuptureConvRequest(
        String country,
        Map<String, String> reponses
) {}

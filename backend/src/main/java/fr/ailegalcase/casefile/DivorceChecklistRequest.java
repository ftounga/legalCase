package fr.ailegalcase.casefile;

import java.util.Map;

public record DivorceChecklistRequest(
        String country,
        Map<String, String> etapeStatuts,
        Map<String, String> pieceStatuts
) {}

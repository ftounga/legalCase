package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StrategicOptionResponse(
        UUID id,
        int ordre,
        String texte,
        String baseJuridique,
        String horizonTemporel,
        List<String> conditions,
        String source,
        String statut,
        String raisonDiscard,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};

    static StrategicOptionResponse from(StrategicOption option) {
        return new StrategicOptionResponse(
                option.getId(),
                option.getOrdre(),
                option.getTexte(),
                option.getBaseJuridique(),
                option.getHorizonTemporel(),
                deserializeConditions(option.getConditionsJson()),
                option.getSource(),
                option.getStatut().name(),
                option.getRaisonDiscard(),
                option.getCreatedAt(),
                option.getUpdatedAt()
        );
    }

    private static List<String> deserializeConditions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> raw = MAPPER.readValue(json, LIST_OF_STRING);
            return raw == null ? List.of() : raw;
        } catch (Exception e) {
            return List.of();
        }
    }
}

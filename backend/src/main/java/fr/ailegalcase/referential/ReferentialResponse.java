package fr.ailegalcase.referential;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReferentialResponse(
        String domain,
        Map<String, List<Entry>> sections
) {
    public record Entry(
            UUID id,
            String key,
            String label,
            String country,
            String valueJson,
            boolean isSystem,
            String sourceRef
    ) {}
}

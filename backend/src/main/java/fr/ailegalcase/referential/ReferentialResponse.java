package fr.ailegalcase.referential;

import java.util.List;
import java.util.Map;

public record ReferentialResponse(
        String domain,
        Map<String, List<Entry>> sections
) {
    public record Entry(
            String key,
            String label,
            String country,
            String valueJson,
            boolean isSystem,
            String sourceRef
    ) {}
}

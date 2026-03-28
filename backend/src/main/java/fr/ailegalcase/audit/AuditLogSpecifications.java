package fr.ailegalcase.audit;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    static Specification<AuditLog> workspaceId(UUID workspaceId) {
        return (root, query, cb) -> cb.equal(root.get("workspaceId"), workspaceId);
    }

    static Specification<AuditLog> fromDate(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<AuditLog> toDate(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    static Specification<AuditLog> action(String action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }
}

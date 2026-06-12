package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * F-284 — un élément de l'échéancier proactif (vue de lecture agrégée).
 *
 * @param kind    DEADLINE (issu de case_deadlines) ou CONTRADICTOIRE (échéance de réponse d'un round)
 * @param source  pour DEADLINE : MANUAL/AI/STATUTORY ; pour CONTRADICTOIRE : CONTRADICTOIRE
 * @param urgency dérivé serveur de daysUntil : OVERDUE/CRITICAL/SOON/UPCOMING
 */
public record EcheancierItem(
        UUID id,
        String label,
        LocalDate dueDate,
        long daysUntil,
        EcheancierUrgency urgency,
        EcheancierKind kind,
        String source
) {
    enum EcheancierKind { DEADLINE, CONTRADICTOIRE }

    enum EcheancierUrgency { OVERDUE, CRITICAL, SOON, UPCOMING }

    static EcheancierUrgency urgencyOf(long daysUntil) {
        if (daysUntil < 0) return EcheancierUrgency.OVERDUE;
        if (daysUntil <= 7) return EcheancierUrgency.CRITICAL;
        if (daysUntil <= 15) return EcheancierUrgency.SOON;
        return EcheancierUrgency.UPCOMING;
    }
}

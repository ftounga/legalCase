package fr.ailegalcase.analysis;

import java.util.UUID;

/**
 * F-257 — Contexte obligatoire pour tout appel sortant vers l'API Anthropic via
 * {@link AnthropicService}. Porte les informations nécessaires au gate token user
 * ({@code PlanLimitService.isMonthlyTokenBudgetExceeded(workspaceId)}) et à
 * l'enregistrement automatique de la consommation
 * ({@code UsageEventService.record(caseFileId, userId, jobType, in, out)}).
 *
 * <p><b>Invariants</b> :
 * <ul>
 *   <li>{@code jobType} non null.</li>
 *   <li>Pour {@code jobType.isUserLevel()} : {@code workspaceId} ET {@code userId} non null.</li>
 *   <li>Pour {@code jobType.isSystemLevel()} : tous les champs sauf {@code jobType} peuvent être null.</li>
 * </ul>
 */
public record AiCallContext(
        UUID workspaceId,
        UUID userId,
        UUID caseFileId,
        JobType jobType
) {
    public AiCallContext {
        if (jobType == null) {
            throw new IllegalArgumentException("AiCallContext.jobType must not be null");
        }
        if (jobType.isUserLevel()) {
            if (workspaceId == null) {
                throw new IllegalArgumentException(
                        "AiCallContext.workspaceId must not be null for user-level JobType " + jobType);
            }
            if (userId == null) {
                throw new IllegalArgumentException(
                        "AiCallContext.userId must not be null for user-level JobType " + jobType);
            }
        }
    }

    /** Constructeur user-level explicite. */
    public static AiCallContext userLevel(UUID workspaceId, UUID userId, UUID caseFileId, JobType jobType) {
        return new AiCallContext(workspaceId, userId, caseFileId, jobType);
    }

    /** Constructeur system-level sans dossier (crons, blog, super-admin). */
    public static AiCallContext systemLevel(JobType jobType) {
        return new AiCallContext(null, null, null, jobType);
    }

    /** Constructeur system-level rattaché à un dossier (vision, conclusion, pièce, etc.). */
    public static AiCallContext systemLevel(JobType jobType, UUID caseFileId) {
        return new AiCallContext(null, null, caseFileId, jobType);
    }
}

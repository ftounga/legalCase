package fr.ailegalcase.superadmin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * F-187 SF-187-01 — Input du endpoint d'export PDF traction commerciale.
 *
 * <p>Body de la requête {@code POST /api/v1/super-admin/traction-onepager/pdf}.
 * Toutes les contraintes Bean Validation sont appliquées automatiquement via
 * {@code @Valid} sur le controller. Une violation provoque un 400 via
 * {@link fr.ailegalcase.shared.GlobalExceptionHandler}.
 *
 * <p>Les KPIs eux-mêmes ne sont PAS dans ce payload : ils sont lus côté serveur
 * depuis {@code SuperAdminService.getMetrics(...)}. Le client n'envoie que la
 * sélection ({@link IncludedKpis}).
 */
public record TractionOnePagerInput(
        @NotNull(message = "includeKpis is required") @Valid IncludedKpis includeKpis,
        @Size(max = 2, message = "verbatims must not contain more than 2 elements") @Valid List<VerbatimDto> verbatims,
        @Size(max = 10, message = "partners must not contain more than 10 elements") List<@NotBlank(message = "partner name must not be blank") @Size(max = 100, message = "partner name must be at most 100 characters") String> partners,
        @NotBlank(message = "headline is required") @Size(max = 200, message = "headline must be at most 200 characters") String headline,
        @NotNull(message = "contact is required") @Valid ContactDto contact
) {

    /** Map booléenne des 7 KPIs F-76 à inclure dans le PDF généré. */
    public record IncludedKpis(
            @NotNull(message = "totalWorkspaces flag is required") Boolean totalWorkspaces,
            @NotNull(message = "trialWorkspaces flag is required") Boolean trialWorkspaces,
            @NotNull(message = "paidWorkspaces flag is required") Boolean paidWorkspaces,
            @NotNull(message = "conversionRatePct flag is required") Boolean conversionRatePct,
            @NotNull(message = "activeWorkspaces30d flag is required") Boolean activeWorkspaces30d,
            @NotNull(message = "analysesLast7Days flag is required") Boolean analysesLast7Days,
            @NotNull(message = "analysesLast30Days flag is required") Boolean analysesLast30Days
    ) {}

    /** Verbatim client (jusqu'à 2 par PDF). */
    public record VerbatimDto(
            @NotBlank(message = "verbatim quote must not be blank") @Size(max = 500, message = "verbatim quote must be at most 500 characters") String quote,
            @NotBlank(message = "verbatim author must not be blank") @Size(max = 200, message = "verbatim author must be at most 200 characters") String author
    ) {}

    /** Contact figurant dans le footer du PDF. */
    public record ContactDto(
            @NotBlank(message = "contact.name must not be blank") @Size(max = 100, message = "contact.name must be at most 100 characters") String name,
            @NotBlank(message = "contact.email must not be blank") @Email(message = "contact.email must be a valid email") @Size(max = 200, message = "contact.email must be at most 200 characters") String email,
            @NotBlank(message = "contact.url must not be blank") @Size(max = 300, message = "contact.url must be at most 300 characters") @Pattern(regexp = "^https?://.+", message = "contact.url must be a valid http(s) URL") String url
    ) {}
}

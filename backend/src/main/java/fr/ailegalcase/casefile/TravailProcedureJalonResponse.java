package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-136-02 : DTO d'un jalon procédural travail (FR + BE) pour exposition REST.
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code code} — code du type de procédure (ex. {@code PRUDHOMMES_FR}, {@code TRIBUNAL_TRAVAIL_BE})</li>
 *   <li>{@code label} — intitulé avocat du jalon</li>
 *   <li>{@code offsetDays} — délai en jours par rapport à la date déclencheur</li>
 *   <li>{@code articleRef} — référence légale (ex. {@code Code travail Art. R.1452-3}, {@code CJ Art. 1051})</li>
 *   <li>{@code dateCible} — date calculée si {@code dateDeclencheur} fournie ; sinon {@code null}</li>
 * </ul>
 */
public record TravailProcedureJalonResponse(
        String code,
        String label,
        int offsetDays,
        String articleRef,
        LocalDate dateCible
) {}

package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-05 : payload de création / mise à jour d'une analyse de la validité
 * d'un licenciement pendant la grossesse / maternité en droit belge
 * (<b>Loi du 16/03/1971 art. 40</b>).
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent direct en droit français (la
 * France a une protection similaire mais l'indemnité et la durée de
 * protection diffèrent — outil dédié hors scope SF-213-05).</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateDebutGrossesse} : date de début de grossesse (ou de
 *       notification à l'employeur) — point de départ de la période protégée.</li>
 *   <li>{@code dateLicenciement} : date de notification du licenciement.</li>
 *   <li>{@code remunerationMensuelleBrute} : rémunération mensuelle brute,
 *       base de calcul de l'indemnité forfaitaire (×6).</li>
 * </ul>
 *
 * <h2>Champs optionnels</h2>
 * <ul>
 *   <li>{@code dateAccouchement} : précise la fin de protection (accouchement
 *       + 3 mois) si {@code dateCongeMaterniteFinale} est absente.</li>
 *   <li>{@code dateCongeMaterniteDebut} / {@code dateCongeMaterniteFinale} :
 *       si {@code dateCongeMaterniteFinale} est renseignée, la fin de
 *       protection devient {@code dateCongeMaterniteFinale + 1 mois}
 *       (calcul exact art. 40 al. 1).</li>
 *   <li>{@code grossesseNotifieeParEcrit} : défaut {@code false} — bascule
 *       le verdict vers {@code PROTECTION_PRESUMEE} si {@code true} +
 *       licenciement ≤ 10 sem post début grossesse.</li>
 *   <li>{@code motifInvoqueParEmployeur} : motif allégué par l'employeur —
 *       documenté pour traçabilité, non analysé automatiquement.</li>
 * </ul>
 *
 * <p>La validation chronologique
 * ({@code dateLicenciement >= dateDebutGrossesse}) est appliquée par
 * {@link LicenciementBeProtectionGrossesseValidator}, qui lève une
 * {@link IllegalArgumentException} mappée en 400 par
 * {@link LicenciementBeProtectionGrossesseService}.</p>
 *
 * @see LicenciementBeProtectionGrossesseValidator
 */
public record LicenciementBeProtectionGrossesseRequest(

        @NotNull(message = "dateDebutGrossesse est requise")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateDebutGrossesse,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateAccouchement,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateCongeMaterniteDebut,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateCongeMaterniteFinale,

        @NotNull(message = "dateLicenciement est requise")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateLicenciement,

        /**
         * Défaut {@code false} si non renseigné. Activé via l'extraction IA
         * du courrier de notification de la grossesse (champ BE-only
         * {@code grossesseNotifieeParEcrit} du sous-objet
         * {@code travail_be_detection}).
         */
        boolean grossesseNotifieeParEcrit,

        @NotNull(message = "remunerationMensuelleBrute est requise")
        @DecimalMin(value = "0.01",
                message = "remunerationMensuelleBrute doit être strictement positive")
        BigDecimal remunerationMensuelleBrute,

        /**
         * Optionnel — motif allégué par l'employeur (extrait de la lettre
         * de licenciement). Non analysé automatiquement, mais persisté pour
         * traçabilité et utilisé en synthèse de dossier par l'avocat.
         */
        String motifInvoqueParEmployeur
) {}

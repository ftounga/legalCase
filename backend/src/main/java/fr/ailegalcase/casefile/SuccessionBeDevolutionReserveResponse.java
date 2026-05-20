package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-11 : réponse de l'endpoint de dévolution / réserve héréditaire belge.
 *
 * <p>Ré-expose l'intégralité du snapshot d'inputs (pour pré-remplissage et
 * ré-édition du formulaire UI — leçon F-DT-36) ET les sorties calculées
 * (verdict, héritiers, réserve, quotité disponible, dépassement).
 * Le champ `libertesConsentiesEur` représente à la fois l'input snapshot et la
 * valeur ré-exposée par le résultat (mêmes valeurs — clé JSON unique
 * conformément au contrat API figé).</p>
 */
public record SuccessionBeDevolutionReserveResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour ré-édition du formulaire UI) ---
        LocalDate dateDeces,
        SuccessionBeDevolutionReserveCalculator.EtatCivilDefuntBe etatCivilDefunt,
        SuccessionBeDevolutionReserveCalculator.RegimeMatrimonialDefuntBe regimeMatrimonialDefunt,
        Integer nombreEnfantsVivants,
        Integer nombreEnfantsPredecedesAvecDescendants,
        Boolean presenceParentsVivants,
        Boolean presenceFreresSoeursOuDescendants,
        BigDecimal masseSuccessoraleEur,
        BigDecimal libertesConsentiesEur,
        String commentaire,
        // --- Outputs calculés ---
        SuccessionBeDevolutionReserveCalculator.Verdict verdict,
        List<SuccessionBeDevolutionReserveCalculator.Heritier> heritiers,
        BigDecimal reserveGlobaleEur,
        String reserveGlobaleFraction,
        BigDecimal quotiteDisponibleEur,
        String quotiteDisponibleFraction,
        BigDecimal depassementQuotiteEur,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}

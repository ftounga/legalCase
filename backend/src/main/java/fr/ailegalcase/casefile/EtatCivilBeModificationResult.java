package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-09 : résultat structuré du moteur décisionnel BE de modification de
 * l'état civil (changement de nom / prénom — loi 18/06/2018 ; changement de
 * sexe — loi 25/06/2017).
 *
 * <p>{@code motifs} et {@code messages} ne sont jamais vides. {@code demarches}
 * liste la procédure et l'autorité compétente (officier de l'état civil pour le
 * prénom et le sexe ; SPF Justice pour le changement de nom) — vide pour
 * {@code QUALIFICATION_INCOMPLETE}. Aucune citation jurisprudentielle (F-JU-04
 * parké).</p>
 */
public record EtatCivilBeModificationResult(
        EtatCivilBeModificationCalculator.EtatCivilBeModificationVerdict verdict,
        String autoriteCompetente,
        List<String> motifs,
        List<String> conseils,
        List<String> demarches,
        List<String> basesJuridiques,
        List<String> messages
) {}

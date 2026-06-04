package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-223-09 : réponse des endpoints POST / GET de l'outil de modification de
 * l'état civil BE (changement de nom / prénom / sexe).
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire) ET les sorties calculées (verdict, autorité
 * compétente, motifs, conseils, démarches, bases juridiques, messages).</p>
 */
public record EtatCivilBeModificationResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        EtatCivilBeModificationCalculator.TypeModification typeModification,
        boolean personneMajeure,
        boolean nationaliteBelgeOuResident,
        Boolean motifLegitime,
        Boolean secondeDemandePrenom,
        Boolean declarationSexeReiteree,
        Boolean consentementRepresentantsSiMineur,
        // --- Outputs calculés ---
        EtatCivilBeModificationCalculator.EtatCivilBeModificationVerdict verdict,
        String autoriteCompetente,
        List<String> motifs,
        List<String> conseils,
        List<String> demarches,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}

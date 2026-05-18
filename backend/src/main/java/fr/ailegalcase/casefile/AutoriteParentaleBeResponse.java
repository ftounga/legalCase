package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-04 : réponse de l'endpoint d'analyse de l'autorité parentale belge.
 *
 * <p>Ré-expose l'intégralité du snapshot d'inputs (pour pré-remplissage et
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, voie
 * procédurale, facteurs, bases juridiques).</p>
 */
public record AutoriteParentaleBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour ré-édition du formulaire UI) ---
        Boolean filiationEtablieDeuxParents,
        Boolean accordParentalExiste,
        Boolean demandeAutoriteExclusive,
        Boolean desinteretDurableParent,
        Boolean miseEnDangerEnfant,
        Boolean incapaciteParent,
        Boolean decisionJudiciaireAnterieure,
        AutoriteParentaleBeCalculator.ModeHebergement modeHebergementPrincipal,
        String commentaire,
        // --- Outputs calculés ---
        AutoriteParentaleBeCalculator.Verdict verdict,
        AutoriteParentaleBeCalculator.VoieProcedurale voieProcedurale,
        List<AutoriteParentaleBeCalculator.Facteur> facteurs,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}

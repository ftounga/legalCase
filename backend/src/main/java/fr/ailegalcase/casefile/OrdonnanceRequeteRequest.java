package fr.ailegalcase.casefile;

/**
 * SF-FA-23-01 : requête d'analyse d'ordonnance sur requête (mesures urgentes familiales).
 *
 * <p>Procédure non-contradictoire — art. 493 CPC (FR) / art. 1025 et s. CJ (BE).
 * Permet au juge d'autoriser des mesures urgentes (consultation dossier bancaire,
 * IST avec enfant, saisies conservatoires, constat d'huissier, production forcée
 * de pièces) sans convoquer la partie adverse.</p>
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service. Applicable FR + BE.</p>
 */
public record OrdonnanceRequeteRequest(
        OrdonnanceRequeteCalculator.MotifRequete motifRequete,
        Boolean urgenceJustifiee,
        Boolean derogationContradictoireJustifiee,
        Boolean pieceJustificativeFournie,
        Boolean presenceEnfants,
        String commentaireUrgence
) {}

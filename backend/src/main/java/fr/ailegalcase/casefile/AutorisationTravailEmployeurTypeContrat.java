package fr.ailegalcase.casefile;

/**
 * SF-214-43 : type de contrat proposé au travailleur étranger recruté par
 * l'employeur (outil F-IM-46-autorisation-travail-employeur-fr, FRANCE).
 *
 * <p>Le type de contrat n'affecte pas la nécessité d'une autorisation de travail
 * (déterminée par la nationalité), mais conditionne les pièces et la durée de la
 * demande (R. 5221-1+ Code du travail).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>.
 */
public enum AutorisationTravailEmployeurTypeContrat {
    CDI,
    CDD,
    INTERIM
}

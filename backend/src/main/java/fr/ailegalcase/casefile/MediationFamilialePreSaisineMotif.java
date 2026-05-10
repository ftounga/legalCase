package fr.ailegalcase.casefile;

/**
 * SF-210-01 : motifs de saisine JAF analysés par l'outil de médiation familiale
 * obligatoire pré-saisine (Cciv art. 373-2-10 al. 3 + Loi 18/11/2016 + CPC
 * art. 1108).
 *
 * <p>Les motifs <b>in-scope</b> sont ceux pour lesquels la médiation est
 * obligatoire avant saisine. Les autres sont retournés en {@code NON_CONCERNE}.
 */
public enum MediationFamilialePreSaisineMotif {

    /** Modalités d'autorité parentale (art. 373-2-10) — IN-SCOPE. */
    AUTORITE_PARENTALE(true),
    /** Contribution à l'entretien et à l'éducation de l'enfant — IN-SCOPE. */
    CONTRIBUTION_ENTRETIEN(true),
    /** Résidence de l'enfant — IN-SCOPE (rattaché AP). */
    RESIDENCE_ENFANT(true),
    /** Droit de visite et d'hébergement — IN-SCOPE. */
    DROIT_VISITE(true),
    /** Divorce contentieux — hors champ de la médiation pré-saisine 373-2-10. */
    DIVORCE_CONTENTIEUX(false),
    /** Succession — hors champ. */
    SUCCESSION(false),
    /** Autre — hors champ par défaut. */
    AUTRE(false);

    private final boolean inScope;

    MediationFamilialePreSaisineMotif(boolean inScope) {
        this.inScope = inScope;
    }

    public boolean isInScope() {
        return inScope;
    }
}

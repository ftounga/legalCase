package fr.ailegalcase.analysis;

/**
 * F-197 SF-197-01 — Body du PUT override.
 *
 * <p>Champ {@code type} obligatoire (validé contre l'enum Travail OU Immigration
 * selon le {@code legalDomain} du dossier). Champ {@code raison} optionnel
 * (texte libre justifiant le choix de l'avocat).</p>
 */
public record TypeLitigeOverrideRequest(String type, String raison) {
}

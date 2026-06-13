package fr.ailegalcase.casefile.conclusion;

import java.util.UUID;

/**
 * F-261 / SF-261-05 — moyen adverse persisté, porteur de son identifiant
 * <strong>stable</strong> ({@link #id}) accolé au contenu {@link AdverseMoyen}.
 *
 * <p>Sert la curation des moyens (F-288 vague 3), qui référence chaque moyen par
 * son id (clé d'exclusion stable entre générations). La génération de conclusions
 * consomme uniquement le {@link #moyen()} nu (le record reste l'intrant inchangé du
 * {@code CaseConclusionPromptBuilder}) — l'id n'altère ni le prompt ni son ordre.</p>
 *
 * @param id    identifiant stable du moyen persisté
 * @param moyen contenu du moyen (thèse / fondements / pièces), intrant prompt inchangé
 */
public record AdverseMoyenWithId(UUID id, AdverseMoyen moyen) {
}

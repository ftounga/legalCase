package fr.ailegalcase.casefile;

/**
 * SF-238-03 : body de POST activation manuelle. {@code toolId} doit être non
 * vide et limité à 128 caractères (contrainte DB).
 */
public record ManualToolActivationRequest(
        String toolId
) {
}

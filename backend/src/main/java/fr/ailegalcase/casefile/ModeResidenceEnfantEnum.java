package fr.ailegalcase.casefile;

/**
 * SF-216-03 : mode de résidence des enfants impliqués dans la pension alimentaire
 * (art. 371-2 Cciv + barème indicatif Cour de cassation). Coefficient appliqué
 * sur le taux barème :
 * <ul>
 *   <li>{@code ALTERNEE} : 0,5 (chaque parent assume la moitié des frais directs).</li>
 *   <li>{@code PRINCIPALE_PARENT1} : 1,0 (parent 1 héberge — parent 2 verse).</li>
 *   <li>{@code PRINCIPALE_PARENT2} : 1,0 (parent 2 héberge — parent 1 verse).</li>
 * </ul>
 */
public enum ModeResidenceEnfantEnum {
    ALTERNEE,
    PRINCIPALE_PARENT1,
    PRINCIPALE_PARENT2
}

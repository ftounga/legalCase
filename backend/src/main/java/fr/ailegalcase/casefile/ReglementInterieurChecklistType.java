package fr.ailegalcase.casefile;

/**
 * SF-218-35 : nature d'un item de la checklist de validité d'un règlement
 * intérieur (F-DT-100). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>OBLIGATOIRE : contenu que le règlement intérieur doit obligatoirement
 *       comporter (hygiène/sécurité, discipline, droits de la défense,
 *       harcèlement — art. L.1321-1 et L.1321-2 CT).</li>
 *   <li>INTERDIT : clause prohibée dont la présence vicie le règlement intérieur
 *       (atteinte aux libertés non justifiée/proportionnée, sanction pécuniaire —
 *       art. L.1321-3 et L.1331-2 CT). {@code conforme=true} = absence de la
 *       clause.</li>
 *   <li>PROCEDURE : formalité de mise en place conditionnant l'opposabilité
 *       (consultation du CSE, transmission à l'inspection du travail, dépôt au
 *       greffe du CPH — art. L.1321-4 CT).</li>
 * </ul>
 */
public enum ReglementInterieurChecklistType {
    OBLIGATOIRE,
    INTERDIT,
    PROCEDURE
}

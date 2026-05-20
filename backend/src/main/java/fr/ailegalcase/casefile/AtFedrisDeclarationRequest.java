package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-207-04 : payload de création/upsert d'une analyse de délai de déclaration
 * d'un accident du travail à Fedris (Loi du 10/04/1971 art. 62 ;
 * AR 21/12/1971 art. 25).
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code dateAccident} — date de survenance de l'accident du travail
 *       (obligatoire, Bean Validation {@code @NotNull}).</li>
 *   <li>{@code dateConnaissanceEmployeur} — date à laquelle l'employeur a
 *       eu connaissance de l'accident. Optionnel ; à défaut le service
 *       prend {@code dateAccident} comme valeur (cas où l'accident a été
 *       déclaré immédiatement à l'employeur).</li>
 *   <li>{@code dateActionEnvisagee} — date à laquelle l'employeur envisage
 *       de déclarer l'accident à Fedris (mode prospectif). Optionnel ; à
 *       défaut le service prend {@code today} (Europe/Brussels).</li>
 *   <li>{@code dateDeclarationEffectuee} — date effective à laquelle la
 *       déclaration a été transmise à Fedris (mode rétrospectif).
 *       Optionnel — si renseigné, le calculateur bascule en mode
 *       rétrospectif et émet un verdict {@code DECLARATION_DANS_LES_TEMPS}
 *       ou {@code DECLARATION_HORS_DELAI}.</li>
 * </ul>
 *
 * <p>Les contrôles de cohérence métier (dates non-futures, ordre des dates)
 * sont délégués au service (qui convertit {@link IllegalArgumentException}
 * en HTTP 400).</p>
 */
public record AtFedrisDeclarationRequest(
        @NotNull LocalDate dateAccident,
        LocalDate dateConnaissanceEmployeur,
        LocalDate dateActionEnvisagee,
        LocalDate dateDeclarationEffectuee
) {}

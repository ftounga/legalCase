package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-216-27 : body POST /api/v1/case-files/{id}/partage-notarial.
 *
 * <p>Outil single-country FRANCE — art. 816 et s. Cciv (partage
 * successoral) + art. 870 Cciv (déclaration de succession) + art. 1592
 * CGI (notaire obligatoire en présence d'immeubles) + art. 641 CGI
 * (délai 6 mois déclaration fiscale) + art. 840 Cciv (bascule
 * judiciaire en cas de désaccord).</p>
 *
 * <p>Le service rejette : valeurs négatives, {@code dateOuvertureSuccession}
 * future, {@code nombreCoheritiers < 1}, country != FRANCE.</p>
 *
 * @param dateOuvertureSuccession date d'ouverture de la succession (= date
 *                                de décès en droit français). Requise.
 * @param nombreCoheritiers       nombre de cohéritiers. Requis, >= 1.
 * @param consentementsTousDetecte true si tous les héritiers ont marqué
 *                                  leur accord pour le partage amiable.
 * @param presenceImmeuble        true si la succession comprend un
 *                                immeuble (déclenche l'obligation
 *                                notariale — art. 1592 CGI).
 * @param desaccordPersistant     true si un désaccord persistant entre
 *                                cohéritiers est documenté (bascule
 *                                vers F-FA-17-partage-judiciaire,
 *                                art. 840 Cciv).
 * @param valeurMasseSuccessoraleEur valeur de la masse successorale en
 *                                    euros. Optionnel. Non négatif.
 * @param notaireDesigne          true si le notaire a déjà été désigné.
 * @param declarationSuccessionEcheance échéance fiscale (déclaration de
 *                                       succession — 6 mois depuis le
 *                                       décès, art. 641 CGI). Optionnel.
 *                                       Si non renseigné, le service la
 *                                       calcule depuis {@code
 *                                       dateOuvertureSuccession}.
 */
public record PartageNotarialRequest(
        LocalDate dateOuvertureSuccession,
        Integer nombreCoheritiers,
        Boolean consentementsTousDetecte,
        Boolean presenceImmeuble,
        Boolean desaccordPersistant,
        Integer valeurMasseSuccessoraleEur,
        Boolean notaireDesigne,
        LocalDate declarationSuccessionEcheance
) {}

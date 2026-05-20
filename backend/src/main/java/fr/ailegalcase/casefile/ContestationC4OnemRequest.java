package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-207-03 : payload de création/upsert d'une analyse de contestation
 * d'une décision ONEM.
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code dateNotificationDecisionOnem} — date de notification de la
 *       décision ONEM contestée (obligatoire). Point de départ du délai de
 *       1 mois du recours administratif au Directeur (AR 25/11/1991 art. 144).</li>
 *   <li>{@code dateActionEnvisagee} — date à laquelle l'avocat envisage
 *       d'agir. Si absente, le service la remplace par {@code LocalDate.now()}
 *       (Europe/Brussels).</li>
 *   <li>{@code recoursAdminDejaForme} — booléen indiquant si le recours
 *       administratif au Directeur a déjà été formé. Si {@code true},
 *       {@code dateDecisionDirecteur} est requis (cas B — calcul du délai
 *       tribunal seulement). Si {@code false}, calcul du palier ADMIN
 *       uniquement (cas A).</li>
 *   <li>{@code dateDecisionDirecteur} — date de notification de la décision
 *       du Directeur sur le recours administratif. Requis si
 *       {@code recoursAdminDejaForme=true} (validation conditionnelle côté
 *       service / calculateur).</li>
 * </ul>
 *
 * <p>Bean Validation : {@code @NotNull} sur {@code dateNotificationDecisionOnem}.
 * La validation conditionnelle (dateDecisionDirecteur requise si
 * recoursAdminDejaForme=true) est portée par le service et levée en
 * {@link IllegalArgumentException} convertie en 400.</p>
 */
public record ContestationC4OnemRequest(
        @NotNull(message = "dateNotificationDecisionOnem est requise") LocalDate dateNotificationDecisionOnem,
        LocalDate dateActionEnvisagee,
        Boolean recoursAdminDejaForme,
        LocalDate dateDecisionDirecteur
) {}

package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-207-01 : payload de création/upsert d'une analyse de prescription Travail BE.
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code dateRupture} — date de rupture du contrat (obligatoire pour
 *       {@code EX_CONTRAT_*}) OU date d'échéance / fait générateur pour
 *       {@code PENDANT_CONTRAT} / {@code ARRIERES_SALAIRE}.</li>
 *   <li>{@code typeCreance} — type de créance (4 valeurs cf.
 *       {@link PrescriptionBeLitigeTravailTypeCreance}).</li>
 *   <li>{@code dateActionEnvisagee} — date à laquelle l'avocat envisage d'agir.
 *       Si absente, le service la remplace par {@code LocalDate.now()} (Europe/Brussels).</li>
 * </ul>
 *
 * <p>Bean Validation déférée au service / calculateur : les contrôles de cohérence
 * (date présente, date d'action ≥ date de rupture, format ISO) déclenchent
 * une {@link IllegalArgumentException} convertie en 400 par le service.</p>
 */
public record PrescriptionBeLitigeTravailRequest(
        LocalDate dateRupture,
        String typeCreance,
        LocalDate dateActionEnvisagee
) {}

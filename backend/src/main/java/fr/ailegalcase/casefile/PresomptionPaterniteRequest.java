package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-216-25 : body POST /api/v1/case-files/{id}/presomption-paternite.
 *
 * <p>Outil single-country FRANCE — art. 312-315 Cciv (Pater is est quem
 * nuptiae demonstrant) + art. 316 al. 2 Cciv (désaveu, délai 6 mois)
 * + art. 333 al. 1 Cciv (possession d'état conforme neutralise
 * contestation).</p>
 *
 * <p>Le service rejette : valeurs incohérentes (dissolution avant
 * mariage), absence de {@code dateNaissanceEnfant} ou
 * {@code dateConclusionMariage}, country != FRANCE.</p>
 *
 * @param dateNaissanceEnfant            date de naissance de l'enfant.
 *                                       Requis.
 * @param dateConclusionMariage          date de conclusion du mariage.
 *                                       Requis.
 * @param dateDissolutionMariage         date de dissolution du mariage
 *                                       (divorce, décès). Optionnel — si
 *                                       le mariage n'est pas dissous.
 * @param dateAccouchement               date d'accouchement. Optionnel —
 *                                       égal à {@code dateNaissanceEnfant}
 *                                       sauf accouchement posthume.
 * @param conceptionEn180PremiersMoisMariage  true si l'enfant a été
 *                                       conçu dans les 180 premiers jours
 *                                       du mariage (art. 313 al. 2 Cciv).
 * @param enfantNeApresDisso             true si l'enfant est né plus de
 *                                       300 jours après la dissolution
 *                                       du mariage (art. 313 al. 1 Cciv).
 * @param desaveuEnvisage                true si une action en désaveu de
 *                                       paternité est envisagée
 *                                       (art. 316 al. 2 Cciv).
 * @param possessionEtatConformeDetecte  true si la possession d'état
 *                                       conforme du mari est documentée
 *                                       (art. 333 al. 1 Cciv) — renforce
 *                                       la présomption, désaveu difficile.
 * @param dateConnaissanceNaissance      date de connaissance de la
 *                                       naissance par le mari (point de
 *                                       départ délai désaveu — Cass. 1ère
 *                                       civ., 19/2/2014). Optionnel.
 */
public record PresomptionPaterniteRequest(
        LocalDate dateNaissanceEnfant,
        LocalDate dateConclusionMariage,
        LocalDate dateDissolutionMariage,
        LocalDate dateAccouchement,
        Boolean conceptionEn180PremiersMoisMariage,
        Boolean enfantNeApresDisso,
        Boolean desaveuEnvisage,
        Boolean possessionEtatConformeDetecte,
        LocalDate dateConnaissanceNaissance
) {}

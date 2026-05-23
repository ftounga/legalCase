package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-29 : body POST /api/v1/case-files/{id}/donation-partage.
 *
 * <p>Outil single-country FRANCE — art. 1075 à 1075-5 Cciv (donation-partage) +
 * art. 1078 Cciv (gel valeur) + art. 1078-1 Cciv (réincorporation) +
 * art. 1080 Cciv (quasi-usufruit) + art. 912-928 Cciv (réserve héréditaire).</p>
 *
 * <p>Le service rejette : valeurs négatives, {@code nombreDescendants < 1},
 * âges donateurs négatifs, country != FRANCE.</p>
 *
 * @param nombreDescendants               nombre total de descendants
 *                                        bénéficiaires (requis, >= 1).
 * @param presencePetitsEnfantsParSubstitution true si la donation-partage
 *                                        bénéficie aux petits-enfants en
 *                                        substitution du descendant
 *                                        consentant (art. 1075-1 Cciv).
 * @param donationPartageConjonctive      true si les deux parents font
 *                                        ensemble la donation de leurs biens
 *                                        propres et communs (art. 1075-2
 *                                        Cciv).
 * @param valeurPartageTotal              valeur totale du patrimoine donné-
 *                                        partagé en euros (optionnel, >= 0).
 * @param respectQuotiteDisponible        true si la donation-partage respecte
 *                                        la quotité disponible ; false si
 *                                        elle l'excède ; null si à vérifier.
 * @param donationsAnterieuresAReinorporer true si des donations antérieures
 *                                        sont à réincorporer pour équilibrer
 *                                        le partage anticipé (art. 1078-1
 *                                        Cciv).
 * @param agesDonateurs                   âges des donateurs (1 ou 2 éléments,
 *                                        chacun >= 0). Si la donation-partage
 *                                        est conjonctive, attendu 2 éléments.
 */
public record DonationPartageRequest(
        Integer nombreDescendants,
        Boolean presencePetitsEnfantsParSubstitution,
        Boolean donationPartageConjonctive,
        Integer valeurPartageTotal,
        Boolean respectQuotiteDisponible,
        Boolean donationsAnterieuresAReinorporer,
        List<Integer> agesDonateurs
) {}

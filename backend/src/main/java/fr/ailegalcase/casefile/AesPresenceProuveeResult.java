package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * SF-214-11 : résultat du calcul de présence prouvée en France et de
 * l'éligibilité aux 4 voies AES (L. 435-1 / L. 435-3 CESEDA). Outil single-country FR.
 *
 * @param periodesNormalisees    périodes saisies, normalisées et triées (debut, fin, typePiece)
 * @param periodesFusionnees     périodes après fusion des chevauchements / contiguïtés
 * @param moisTotauxProuves      durée totale couverte par les pièces, en mois entiers
 * @param anneesTotalesProuvees  durée totale couverte par les pièces, en années complètes
 * @param eligibiliteParVoie     map {@code aes_famille / aes_humanitaire / aes_etudiant /
 *                               aes_metiers_tension} → booléen d'éligibilité (seuils 5/10/3/3 ans)
 * @param gapsPeriodes           lacunes sans preuve entre la première et la dernière période
 * @param recommandationsPieces  suggestions de pièces pour combler les gaps
 * @param baseJuridique          références juridiques mobilisées
 */
public record AesPresenceProuveeResult(
        List<PeriodeNormalisee> periodesNormalisees,
        List<PeriodeNormalisee> periodesFusionnees,
        int moisTotauxProuves,
        int anneesTotalesProuvees,
        Map<String, Boolean> eligibiliteParVoie,
        List<Gap> gapsPeriodes,
        List<String> recommandationsPieces,
        String baseJuridique
) {

    /** Période normalisée (bornes incluses) avec son type de pièce. */
    public record PeriodeNormalisee(
            LocalDate debut,
            LocalDate fin,
            AesPieceType typePiece
    ) {}

    /** Lacune de présence (sans pièce) entre deux périodes prouvées. */
    public record Gap(
            LocalDate debut,
            LocalDate fin,
            int dureeMois
    ) {}
}

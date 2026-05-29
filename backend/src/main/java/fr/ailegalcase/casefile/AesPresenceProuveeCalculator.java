package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SF-214-11 : calculateur de présence prouvée en France et d'éligibilité aux 4
 * voies d'admission exceptionnelle au séjour (AES). Outil single-country FR.
 *
 * <p>Méthode :
 * <ol>
 *   <li>valide et trie les périodes saisies ;</li>
 *   <li>fusionne les périodes qui se chevauchent ou se touchent (≤ 1 jour) ;</li>
 *   <li>somme la durée couverte (mois entiers, années complètes) ;</li>
 *   <li>évalue les 4 seuils AES (famille 5 ans / humanitaire 10 ans / étudiant 3 ans /
 *       métiers en tension 3 ans) ;</li>
 *   <li>identifie les lacunes (gaps) sans preuve entre la première et la dernière
 *       période ;</li>
 *   <li>propose des pièces pour combler ces lacunes.</li>
 * </ol>
 *
 * <p>Source juridique :
 * <ul>
 *   <li>Circulaire du 28/11/2012 (Valls) — pièces recevables pour établir la
 *       présence habituelle en France ;</li>
 *   <li>L. 435-1 CESEDA — AES motif familial (5 ans) et humanitaire (10 ans) ;</li>
 *   <li>L. 435-3 CESEDA (loi 2024) — AES métiers en tension (3 ans) ;</li>
 *   <li>AES étudiant — 3 ans de présence (pratique préfectorale) ;</li>
 *   <li>CE 4 décembre 2009, n° 310980 — notion de présence habituelle.</li>
 * </ul>
 */
public final class AesPresenceProuveeCalculator {

    public static final String VOIE_FAMILLE = "aes_famille";
    public static final String VOIE_HUMANITAIRE = "aes_humanitaire";
    public static final String VOIE_ETUDIANT = "aes_etudiant";
    public static final String VOIE_METIERS_TENSION = "aes_metiers_tension";

    /** Seuils d'éligibilité, en années complètes de présence prouvée. */
    public static final int SEUIL_FAMILLE_ANNEES = 5;
    public static final int SEUIL_HUMANITAIRE_ANNEES = 10;
    public static final int SEUIL_ETUDIANT_ANNEES = 3;
    public static final int SEUIL_METIERS_TENSION_ANNEES = 3;

    private static final String BASE_JURIDIQUE =
            "Circulaire du 28/11/2012 (Valls) — pièces recevables ; "
            + "L. 435-1 CESEDA (AES familial 5 ans, humanitaire 10 ans) ; "
            + "L. 435-3 CESEDA (AES métiers en tension 3 ans, loi 2024) ; "
            + "AES étudiant 3 ans (pratique préfectorale) ; "
            + "CE 4 décembre 2009 n° 310980 (présence habituelle)";

    private AesPresenceProuveeCalculator() {}

    /**
     * Analyse la présence prouvée et l'éligibilité aux 4 voies AES.
     *
     * @param periodes   périodes présentées (liste non vide ; chaque période :
     *                   debut ≤ fin, debut non future)
     * @param today      date de référence pour le contrôle "date future"
     * @return résultat de l'analyse
     * @throws IllegalArgumentException si la liste est vide / null, ou si une
     *                                  période est invalide (fin < debut, debut future,
     *                                  bornes ou type nuls)
     */
    public static AesPresenceProuveeResult analyze(List<AesPresenceProuveeRequest.PeriodePresentee> periodes,
                                                   LocalDate today) {
        if (periodes == null || periodes.isEmpty()) {
            throw new IllegalArgumentException("periodesPresentees ne peut pas être vide");
        }

        List<AesPresenceProuveeResult.PeriodeNormalisee> normalisees = new ArrayList<>();
        for (AesPresenceProuveeRequest.PeriodePresentee p : periodes) {
            if (p == null || p.debut() == null || p.fin() == null) {
                throw new IllegalArgumentException("Chaque période doit avoir un debut et une fin");
            }
            if (p.typePiece() == null) {
                throw new IllegalArgumentException("Chaque période doit avoir un typePiece");
            }
            if (p.fin().isBefore(p.debut())) {
                throw new IllegalArgumentException("periode.fin ne peut pas être antérieure à periode.debut");
            }
            if (p.debut().isAfter(today)) {
                throw new IllegalArgumentException("periode.debut ne peut pas être dans le futur");
            }
            normalisees.add(new AesPresenceProuveeResult.PeriodeNormalisee(p.debut(), p.fin(), p.typePiece()));
        }
        normalisees.sort(Comparator
                .comparing(AesPresenceProuveeResult.PeriodeNormalisee::debut)
                .thenComparing(AesPresenceProuveeResult.PeriodeNormalisee::fin));

        List<AesPresenceProuveeResult.PeriodeNormalisee> fusionnees = fusionner(normalisees);

        long totalJours = 0;
        for (AesPresenceProuveeResult.PeriodeNormalisee p : fusionnees) {
            // bornes incluses → +1 jour
            totalJours += ChronoUnit.DAYS.between(p.debut(), p.fin()) + 1;
        }
        // Conversion : un mois ≈ 30,4375 jours (365,25 / 12) pour neutraliser les mois courts.
        int moisTotaux = (int) Math.floor(totalJours / 30.4375);
        int anneesTotales = moisTotaux / 12;

        Map<String, Boolean> eligibilite = new LinkedHashMap<>();
        eligibilite.put(VOIE_FAMILLE, anneesTotales >= SEUIL_FAMILLE_ANNEES);
        eligibilite.put(VOIE_HUMANITAIRE, anneesTotales >= SEUIL_HUMANITAIRE_ANNEES);
        eligibilite.put(VOIE_ETUDIANT, anneesTotales >= SEUIL_ETUDIANT_ANNEES);
        eligibilite.put(VOIE_METIERS_TENSION, anneesTotales >= SEUIL_METIERS_TENSION_ANNEES);

        List<AesPresenceProuveeResult.Gap> gaps = computeGaps(fusionnees);
        List<String> recommandations = buildRecommandations(gaps, anneesTotales);

        return new AesPresenceProuveeResult(
                normalisees,
                fusionnees,
                moisTotaux,
                anneesTotales,
                eligibilite,
                gaps,
                recommandations,
                BASE_JURIDIQUE);
    }

    /**
     * Fusionne les périodes triées qui se chevauchent ou sont contiguës (gap ≤ 1 jour).
     */
    private static List<AesPresenceProuveeResult.PeriodeNormalisee> fusionner(
            List<AesPresenceProuveeResult.PeriodeNormalisee> triees) {
        List<AesPresenceProuveeResult.PeriodeNormalisee> out = new ArrayList<>();
        for (AesPresenceProuveeResult.PeriodeNormalisee p : triees) {
            if (out.isEmpty()) {
                out.add(p);
                continue;
            }
            AesPresenceProuveeResult.PeriodeNormalisee last = out.get(out.size() - 1);
            // Chevauchement ou contiguïté : début ≤ fin précédente + 1 jour.
            if (!p.debut().isAfter(last.fin().plusDays(1))) {
                LocalDate newFin = p.fin().isAfter(last.fin()) ? p.fin() : last.fin();
                // Type de pièce conservé du segment qui s'étend le plus loin (info indicative).
                AesPieceType type = p.fin().isAfter(last.fin()) ? p.typePiece() : last.typePiece();
                out.set(out.size() - 1,
                        new AesPresenceProuveeResult.PeriodeNormalisee(last.debut(), newFin, type));
            } else {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Identifie les lacunes (≥ 1 jour) entre périodes fusionnées consécutives.
     */
    private static List<AesPresenceProuveeResult.Gap> computeGaps(
            List<AesPresenceProuveeResult.PeriodeNormalisee> fusionnees) {
        List<AesPresenceProuveeResult.Gap> gaps = new ArrayList<>();
        for (int i = 1; i < fusionnees.size(); i++) {
            LocalDate finPrec = fusionnees.get(i - 1).fin();
            LocalDate debutSuiv = fusionnees.get(i).debut();
            LocalDate gapDebut = finPrec.plusDays(1);
            LocalDate gapFin = debutSuiv.minusDays(1);
            if (!gapFin.isBefore(gapDebut)) {
                long jours = ChronoUnit.DAYS.between(gapDebut, gapFin) + 1;
                int mois = (int) Math.floor(jours / 30.4375);
                gaps.add(new AesPresenceProuveeResult.Gap(gapDebut, gapFin, mois));
            }
        }
        return gaps;
    }

    private static List<String> buildRecommandations(List<AesPresenceProuveeResult.Gap> gaps,
                                                     int anneesTotales) {
        List<String> reco = new ArrayList<>();
        for (AesPresenceProuveeResult.Gap g : gaps) {
            reco.add(String.format(
                    "Période sans preuve du %s au %s (%d mois) : produire quittances de loyer, "
                    + "factures EDF/GDF, RIB ou avis d'imposition couvrant cet intervalle.",
                    g.debut(), g.fin(), g.dureeMois()));
        }
        if (gaps.isEmpty()) {
            reco.add("Continuité de présence établie sur l'intervalle couvert — "
                    + "veiller à conserver au moins une pièce par semestre pour consolider l'ancienneté.");
        }
        if (anneesTotales < SEUIL_ETUDIANT_ANNEES) {
            reco.add("Présence prouvée inférieure à 3 ans : aucune voie AES n'est encore atteinte — "
                    + "rassembler des pièces complémentaires pour étendre l'ancienneté établie.");
        }
        return reco;
    }
}

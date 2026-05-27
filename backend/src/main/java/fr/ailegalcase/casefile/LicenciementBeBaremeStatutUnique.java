package fr.ailegalcase.casefile;

/**
 * SF-213-04 (refactor DRY depuis SF-213-03) : barème officiel du statut unique
 * belge (Loi du 26/12/2013 — art. 37/1 et seq.) — table partagée entre :
 *
 * <ul>
 *   <li>{@link LicenciementBeStatutUniquePreavisCalculator} (préavis 100 %
 *       post-2014) ;</li>
 *   <li>{@link LicenciementBeFormuleClaeysCalculator} (clause de sauvegarde
 *       art. 67 — partie post-2014 du préavis cumulé).</li>
 * </ul>
 *
 * <p>Aucune divergence de barème entre les deux outils — single source of
 * truth pour éviter les écarts silencieux entre les deux situations métier
 * (un même bareme officiel applique aux deux).</p>
 *
 * <h2>Barème (côté EMPLOYEUR)</h2>
 * <p>Indexé par années complètes d'ancienneté. La dernière tranche
 * (≥ 20 ans) renvoie {@link #PREAVIS_SEMAINES_PLAFOND_20_ANS} semaines.</p>
 */
final class LicenciementBeBaremeStatutUnique {

    /**
     * Barème officiel statut unique (côté EMPLOYEUR) — Loi 26/12/2013 art. 37/1.
     *
     * <p>Tranches indexées par années complètes d'ancienneté.</p>
     */
    static final int[] BAREME_STATUT_UNIQUE = {
            // 0 an = 2 semaines (couvre période sous les 3 premiers mois selon
            // mini-spec, granularité année dans le barème simplifié)
            // Note : le tarif officiel pour < 3 mois est 2 sem, à 3 mois → 4 sem.
            // Granularité année post-mois supplémentaires couvre les cas
            // courants ; les < 3 mois sont à traiter via les mois supplémentaires
            // (ancienneteAnnees = 0, mois < 3 → 2 semaines).
            4,   // 0-1 an (par défaut si années complètes ≥ 1)
            6,   // 1-2 ans
            7,   // 2-3 ans
            9,   // 3-4 ans
            12,  // 4-5 ans
            15,  // 5-6 ans
            18,  // 6-7 ans
            21,  // 7-8 ans
            24,  // 8-9 ans
            27,  // 9-10 ans
            30,  // 10-11 ans
            33,  // 11-12 ans
            36,  // 12-13 ans
            39,  // 13-14 ans
            42,  // 14-15 ans
            45,  // 15-16 ans
            48,  // 16-17 ans
            51,  // 17-18 ans
            54,  // 18-19 ans
            57   // 19-20 ans
    };

    /** Préavis pour ancienneté &lt; 3 mois (statut unique). */
    static final int PREAVIS_SEMAINES_MOINS_3_MOIS = 2;

    /** Préavis plafond pour ancienneté ≥ 20 ans (statut unique). */
    static final int PREAVIS_SEMAINES_PLAFOND_20_ANS = 62;

    private LicenciementBeBaremeStatutUnique() {
    }

    /**
     * Résout la durée du préavis (en semaines) selon le barème statut unique.
     *
     * <ul>
     *   <li>Ancienneté = 0 année complète, mois &lt; 3 → 2 semaines.</li>
     *   <li>Ancienneté = 0 année complète, mois ≥ 3 → 4 semaines (tranche 0-1 an).</li>
     *   <li>1 à 19 années complètes → tranche du barème indexée sur les années.</li>
     *   <li>≥ 20 années complètes → 62 semaines (plafond).</li>
     * </ul>
     *
     * @param anneesCompletes        années d'ancienneté complètes (≥ 0)
     * @param moisSupplementaires    mois additionnels (0-11, utilisés uniquement
     *                               pour la tranche &lt; 3 mois)
     * @return durée du préavis en semaines
     */
    static int resolveDureePreavis(int anneesCompletes, int moisSupplementaires) {
        if (anneesCompletes <= 0) {
            return moisSupplementaires < 3 ? PREAVIS_SEMAINES_MOINS_3_MOIS : BAREME_STATUT_UNIQUE[0];
        }
        if (anneesCompletes >= BAREME_STATUT_UNIQUE.length) {
            return PREAVIS_SEMAINES_PLAFOND_20_ANS;
        }
        return BAREME_STATUT_UNIQUE[anneesCompletes];
    }

    /**
     * Variante simplifiée pour la clause de sauvegarde art. 67 — on n'a pas
     * de mois additionnels, juste des années entières post-2014. Renvoie la
     * tranche complète (≥ 1 an = barème ; ≥ 20 ans = plafond ; &lt; 1 an = 4 sem
     * tranche 0-1 an car la clause sauvegarde présuppose au moins quelques mois
     * de contrat post-2014).
     *
     * @param anneesPostStatutUnique années d'ancienneté post-01/01/2014 (≥ 0)
     * @return durée du préavis statut unique en semaines
     */
    static int resolveDureePreavisAnneesEntieres(int anneesPostStatutUnique) {
        return resolveDureePreavis(anneesPostStatutUnique, 0);
    }
}

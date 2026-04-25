package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-DT-24-01 : moteur de calcul de validité d'une clause de non-concurrence en
 * droit du travail français selon les 4 critères cumulatifs posés par la Cour de
 * cassation (Cass. soc. 10/07/2002, n° 00-45.135 + 00-45.387 + 99-43.336) et
 * confirmés depuis (Cass. soc. 12/12/2007, etc.) sur le fondement de l'article
 * L.1221-1 du Code du travail.
 *
 * <p>Les 4 critères cumulatifs :</p>
 * <ol>
 *     <li><b>Limitation territoriale</b> — la clause doit définir un territoire précis,
 *     pas « tout pays » / « monde entier » / « illimité ».</li>
 *     <li><b>Limitation temporelle</b> — durée raisonnable, en pratique ≤ 36 mois ;
 *     un dépassement met la clause en péril de nullité.</li>
 *     <li><b>Limitation à un objet</b> — le périmètre des activités interdites doit
 *     être circonscrit (un secteur, un type de fonction), pas « toute activité ».</li>
 *     <li><b>Contrepartie financière effective et non dérisoire</b> — la jurisprudence
 *     a posé un seuil prudent autour de 25-30 % du salaire brut mensuel.</li>
 * </ol>
 *
 * <p><b>Pays</b> : FRANCE uniquement. Le régime belge (Loi 03/07/1978 art. 65 + CCT
 * 1bis du 04/03/1981) repose sur des paramètres distincts (durée max 12 mois,
 * indemnité forfaitaire 50 % brut/durée) et fera l'objet d'une feature jumelle.</p>
 */
public final class NonConcurrenceCalculator {

    /** Secteurs d'activité — purement informatif pour le moment, n'impacte pas le scoring. */
    public enum SecteurActivite {
        INFORMATIQUE,
        COMMERCE,
        INDUSTRIE,
        SERVICES,
        AUTRE
    }

    /** Verdict de validité dérivé du nombre de critères respectés et du ratio. */
    public enum VerdictValidite {
        VALIDE,
        RISQUE_NULLITE_PARTIELLE,
        NULLE
    }

    /** Durée maximale raisonnable jurisprudence — au-delà : critère 2 KO. */
    static final int DUREE_MAX_MOIS = 36;
    /** Seuil minimal du ratio contrepartie/salaire (25 %) — non dérisoire. */
    static final BigDecimal RATIO_MINIMAL_PCT = new BigDecimal("25.0");
    /** Pondération de chaque critère respecté (×4 = 100). */
    static final int POIDS_CRITERE = 25;
    /** Plancher 1 mois × salaire pour clause exécutée mais nulle (jurisprudence). */
    static final BigDecimal NULLITE_PLANCHER_MOIS = BigDecimal.ONE;

    /** Mots-clefs trahissant un territoire non limité. */
    private static final String[] TERRITOIRE_MOTS_INTERDITS = {
            "monde", "tout pays", "tous pays", "illimite", "illimité", "universel"
    };
    /** Mots-clefs trahissant un objet non limité. */
    private static final String[] OBJET_MOTS_INTERDITS = {
            "toute activite", "toute activité", "toutes activites", "toutes activités",
            "toute fonction", "toutes fonctions", "tout poste", "tous postes"
    };

    private NonConcurrenceCalculator() {}

    /**
     * Calcule la validité de la clause + l'indemnité due ou potentielle en nullité.
     *
     * @param input  données saisies par l'avocat
     * @param country pays du workspace ("FRANCE" uniquement supporté pour cette SF)
     * @return résultat structuré
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static NonConcurrenceResult compute(NonConcurrenceInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (input.salaireMensuelBrutEur() == null || input.salaireMensuelBrutEur().signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et strictement positif");
        }
        if (input.dureeMois() != null && input.dureeMois() < 0) {
            throw new IllegalArgumentException("Durée en mois ne peut être négative");
        }
        if (input.contrepartieMontantMensuelEur() != null
                && input.contrepartieMontantMensuelEur().signum() < 0) {
            throw new IllegalArgumentException("Contrepartie financière ne peut être négative");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — variante BE en backlog (CCT 1bis)");
        }

        BigDecimal salaire = input.salaireMensuelBrutEur().setScale(2, RoundingMode.HALF_UP);
        boolean clausePresente = Boolean.TRUE.equals(input.clausePresenteContrat());

        // --- Critère 1 : limitation territoriale ---
        boolean critere1 = clausePresente
                && Boolean.TRUE.equals(input.limiteTerritoireDefini())
                && territoireValide(input.territoireDescription());

        // --- Critère 2 : limitation temporelle ---
        boolean critere2 = clausePresente
                && Boolean.TRUE.equals(input.limiteDureeDefinie())
                && input.dureeMois() != null
                && input.dureeMois() > 0
                && input.dureeMois() <= DUREE_MAX_MOIS;

        // --- Critère 3 : limitation à un objet ---
        boolean critere3 = clausePresente
                && Boolean.TRUE.equals(input.limiteObjetDefini())
                && objetValide(input.objetDescription());

        // --- Critère 4 : contrepartie financière effective ---
        BigDecimal contrepartie = input.contrepartieMontantMensuelEur() == null
                ? BigDecimal.ZERO
                : input.contrepartieMontantMensuelEur().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratio = ratioPourcentage(contrepartie, salaire);
        boolean critere4 = clausePresente
                && Boolean.TRUE.equals(input.contrepartieFinancierePresente())
                && contrepartie.signum() > 0
                && ratio.compareTo(RATIO_MINIMAL_PCT) >= 0;

        int nbCriteresOk = (critere1 ? 1 : 0)
                + (critere2 ? 1 : 0)
                + (critere3 ? 1 : 0)
                + (critere4 ? 1 : 0);
        int score = nbCriteresOk * POIDS_CRITERE;

        VerdictValidite verdict;
        if (!clausePresente || nbCriteresOk <= 2) {
            verdict = VerdictValidite.NULLE;
        } else if (nbCriteresOk == 4 && ratio.compareTo(RATIO_MINIMAL_PCT) >= 0) {
            verdict = VerdictValidite.VALIDE;
        } else {
            verdict = VerdictValidite.RISQUE_NULLITE_PARTIELLE;
        }

        // Indemnité contrepartie due seulement si VALIDE et durée renseignée.
        BigDecimal indemniteContrepartie;
        if (verdict == VerdictValidite.VALIDE && input.dureeMois() != null && input.dureeMois() > 0) {
            indemniteContrepartie = contrepartie
                    .multiply(BigDecimal.valueOf(input.dureeMois()))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            indemniteContrepartie = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Indemnité potentielle nullité : plancher 1 mois × salaire si clause nulle/risque
        // ET clause présente au contrat (sinon il n'y a rien à compenser).
        BigDecimal indemniteNullite;
        if (clausePresente && verdict != VerdictValidite.VALIDE) {
            indemniteNullite = salaire.multiply(NULLITE_PLANCHER_MOIS)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            indemniteNullite = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String baseJuridique = "Cass. soc. 10/07/2002 (n° 00-45.135) + art. L.1221-1 C. trav."
                + " (4 critères cumulatifs : territoire, durée, objet, contrepartie)";
        String formule = construireFormule(nbCriteresOk, ratio, verdict,
                contrepartie, input.dureeMois(), indemniteContrepartie, indemniteNullite, salaire);
        List<String> messages = construireMessages(clausePresente, critere1, critere2, critere3,
                critere4, ratio, verdict, input.dureeMois());

        return new NonConcurrenceResult(
                critere1, critere2, critere3, critere4,
                ratio, score, verdict,
                indemniteContrepartie, indemniteNullite,
                baseJuridique, formule, messages, countryNormalized
        );
    }

    private static boolean territoireValide(String desc) {
        if (desc == null || desc.isBlank()) return false;
        String norm = normalize(desc);
        for (String mot : TERRITOIRE_MOTS_INTERDITS) {
            if (norm.contains(mot)) return false;
        }
        return true;
    }

    private static boolean objetValide(String desc) {
        if (desc == null || desc.isBlank()) return false;
        String norm = normalize(desc);
        for (String mot : OBJET_MOTS_INTERDITS) {
            if (norm.contains(mot)) return false;
        }
        return true;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
                .replace('à', 'a').replace('â', 'a').replace('ä', 'a')
                .replace('î', 'i').replace('ï', 'i')
                .replace('ô', 'o').replace('ö', 'o')
                .replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
                .replace('ç', 'c');
    }

    static BigDecimal ratioPourcentage(BigDecimal contrepartie, BigDecimal salaire) {
        if (salaire == null || salaire.signum() <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        if (contrepartie == null || contrepartie.signum() <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return contrepartie.multiply(new BigDecimal("100"))
                .divide(salaire, 1, RoundingMode.HALF_UP);
    }

    private static String construireFormule(int nbCriteresOk,
                                            BigDecimal ratio,
                                            VerdictValidite verdict,
                                            BigDecimal contrepartie,
                                            Integer dureeMois,
                                            BigDecimal indemniteContrepartie,
                                            BigDecimal indemniteNullite,
                                            BigDecimal salaire) {
        StringBuilder sb = new StringBuilder();
        sb.append(nbCriteresOk).append("/4 critères respectés × ").append(POIDS_CRITERE)
                .append(" = ").append(nbCriteresOk * POIDS_CRITERE).append("/100");
        sb.append(" | Ratio contrepartie/salaire = ").append(format(ratio)).append(" %");
        sb.append(" | Verdict : ").append(verdict.name());
        if (verdict == VerdictValidite.VALIDE && dureeMois != null && dureeMois > 0) {
            sb.append(" | Indemnité contrepartie due : ").append(format(contrepartie))
                    .append(" € × ").append(dureeMois).append(" mois = ")
                    .append(format(indemniteContrepartie)).append(" €");
        }
        if (verdict != VerdictValidite.VALIDE && indemniteNullite.signum() > 0) {
            sb.append(" | Indemnité potentielle nullité : ").append(format(salaire))
                    .append(" € × 1 mois = ").append(format(indemniteNullite)).append(" €");
        }
        return sb.toString();
    }

    private static List<String> construireMessages(boolean clausePresente,
                                                   boolean c1, boolean c2, boolean c3, boolean c4,
                                                   BigDecimal ratio,
                                                   VerdictValidite verdict,
                                                   Integer dureeMois) {
        List<String> msgs = new ArrayList<>();
        if (!clausePresente) {
            msgs.add("Aucune clause de non-concurrence présente au contrat — aucune obligation post-contractuelle.");
            return msgs;
        }
        if (!c1) msgs.add("Critère 1 — Territoire non limité ou trop large : risque de nullité (Cass. soc. 10/07/2002).");
        if (!c2) msgs.add("Critère 2 — Durée non définie ou supérieure à 36 mois : risque de nullité pour atteinte excessive à la liberté du travail.");
        if (!c3) msgs.add("Critère 3 — Objet trop large (« toute activité ») : risque de nullité pour atteinte disproportionnée.");
        if (!c4) {
            msgs.add("Critère 4 — Contrepartie financière insuffisante (ratio "
                    + format(ratio) + " % < 25 %) ou absente : risque de nullité (Cass. soc. 15/11/2006 — contrepartie non dérisoire).");
        }
        switch (verdict) {
            case VALIDE -> msgs.add("Clause valide : opposable au salarié pour la durée prévue. L'employeur doit verser la contrepartie financière.");
            case RISQUE_NULLITE_PARTIELLE -> msgs.add(
                    "Risque de nullité partielle : le juge peut prononcer la nullité totale ou réduire la portée (Cass. soc. 18/09/2002 — pouvoir de réduction).");
            case NULLE -> msgs.add(
                    "Clause manifestement nulle. Si exécutée par le salarié, indemnisation due (Cass. soc. 12/05/2010, plancher symbolique 1 mois).");
        }
        if (dureeMois != null && dureeMois > DUREE_MAX_MOIS) {
            msgs.add("La durée " + dureeMois + " mois dépasse la durée maximale raisonnable (36 mois) — solliciter une réduction par le juge.");
        }
        return msgs;
    }

    private static String format(BigDecimal montant) {
        if (montant == null) return "0,00";
        return montant.toPlainString().replace('.', ',');
    }
}

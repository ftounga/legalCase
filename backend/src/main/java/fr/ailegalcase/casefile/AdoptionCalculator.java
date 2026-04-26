package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-18-09 : calculateur de recevabilité d'une <strong>adoption</strong>
 * (FR — art. 343-370-2 Cciv).
 *
 * <p>Le droit français connaît deux formes d'adoption :</p>
 * <ul>
 *   <li><strong>Adoption plénière</strong> (art. 343-359) — remplace
 *       définitivement la filiation d'origine. Conditions strictes : âge
 *       adoptant, différence d'âge ≥ 15 ans, adopté < 15 ans (sauf
 *       exceptions), placement 6 mois, consentements multiples.
 *       Irrévocable.</li>
 *   <li><strong>Adoption simple</strong> (art. 360-370-2) — ajoute un lien
 *       sans effacer la filiation d'origine. Conditions plus souples,
 *       aucune limite d'âge maximum pour l'adopté. Révocable pour motif
 *       grave (art. 370).</li>
 * </ul>
 *
 * <p>Le calculateur peut basculer plénière → simple lorsque les conditions
 * de la plénière ne sont pas remplies mais celles de la simple le sont
 * (recommandation à l'avocat).</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong>. La Belgique a un régime
 * distinct (CC art. 343 et s. — conditions d'âge et procédure parquet
 * différentes) qui sera traité par une feature jumelle au backlog.</p>
 */
public final class AdoptionCalculator {

    /** Forme d'adoption (entrée + recommandée en sortie). */
    public enum FormeAdoption {
        PLENIERE,
        SIMPLE,
        /** Forme recommandée seulement : aucune forme applicable. */
        AUCUNE
    }

    /** Verdict de recevabilité. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Âge minimum adoptant (couple marié / partenaires depuis L. n°2022-219). */
    public static final int AGE_MIN_ADOPTANT_COUPLE = 26;
    /** Âge minimum adoptant célibataire (art. 343-1). */
    public static final int AGE_MIN_ADOPTANT_CELIBATAIRE = 28;
    /** Différence d'âge minimum avec l'adopté (art. 344). */
    public static final int DIFF_AGE_MIN_ANS = 15;
    /** Âge maximum adopté pour adoption plénière (art. 345). */
    public static final int AGE_MAX_ADOPTE_PLENIERE = 15;
    /** Âge à partir duquel le consentement de l'adopté est obligatoire. */
    public static final int AGE_CONSENTEMENT_ADOPTE = 13;
    /** Délai d'instruction minimal (cas simple). */
    public static final int DELAI_INSTRUCTION_MIN_MOIS = 6;
    /** Délai d'instruction maximal (cas complexe). */
    public static final int DELAI_INSTRUCTION_MAX_MOIS = 18;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE = "Art. 343-370-2 Cciv";

    private AdoptionCalculator() {}

    /**
     * Évalue la recevabilité d'une adoption.
     *
     * @param formeAdoption                 forme demandée (PLENIERE / SIMPLE) — obligatoire
     * @param ageAdoptant                   âge de l'adoptant (≥ 0)
     * @param ageAdopte                     âge de l'adopté (≥ 0)
     * @param consentementParents           consentement des parents biologiques recueilli ?
     * @param consentementAdopte            consentement de l'adopté ≥ 13 ans recueilli ?
     * @param consentementConjointAdoptant  consentement du conjoint si adoptant marié ?
     * @param enquetes                      enquêtes sociales conduites ?
     * @param placement6mois                placement de 6 mois (art. 345-1) effectué ?
     * @param pupilleEtat                   l'adopté est-il pupille de l'État ?
     * @param adoptantMarie                 l'adoptant est-il marié ?
     * @param country                       pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static AdoptionResult compute(FormeAdoption formeAdoption,
                                         Integer ageAdoptant,
                                         Integer ageAdopte,
                                         Boolean consentementParents,
                                         Boolean consentementAdopte,
                                         Boolean consentementConjointAdoptant,
                                         Boolean enquetes,
                                         Boolean placement6mois,
                                         Boolean pupilleEtat,
                                         Boolean adoptantMarie,
                                         String country) {
        if (formeAdoption == null) {
            throw new IllegalArgumentException("Forme d'adoption requise (PLENIERE ou SIMPLE)");
        }
        if (formeAdoption == FormeAdoption.AUCUNE) {
            throw new IllegalArgumentException(
                    "AUCUNE n'est pas une forme d'entrée valide (forme recommandée seulement)");
        }
        if (ageAdoptant == null) {
            throw new IllegalArgumentException("Âge de l'adoptant requis");
        }
        if (ageAdoptant < 0) {
            throw new IllegalArgumentException("Âge de l'adoptant doit être positif");
        }
        if (ageAdopte == null) {
            throw new IllegalArgumentException("Âge de l'adopté requis");
        }
        if (ageAdopte < 0) {
            throw new IllegalArgumentException("Âge de l'adopté doit être positif");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC art. 343 et s. — régime distinct, "
                            + "conditions d'âge et procédure parquet propres) sera traité dans "
                            + "une feature jumelle dédiée au backlog.");
        }

        if (consentementParents == null) consentementParents = false;
        if (consentementAdopte == null) consentementAdopte = false;
        if (consentementConjointAdoptant == null) consentementConjointAdoptant = false;
        if (enquetes == null) enquetes = false;
        if (placement6mois == null) placement6mois = false;
        if (pupilleEtat == null) pupilleEtat = false;
        if (adoptantMarie == null) adoptantMarie = false;

        int differenceAgeAns = ageAdoptant - ageAdopte;

        // Évaluation indépendante des conditions plénière vs simple
        ConditionsBilan plenierBilan = evaluerPleniere(
                ageAdoptant, ageAdopte, differenceAgeAns,
                consentementParents, consentementAdopte, consentementConjointAdoptant,
                enquetes, placement6mois, pupilleEtat, adoptantMarie);
        ConditionsBilan simpleBilan = evaluerSimple(
                ageAdoptant, differenceAgeAns,
                consentementParents, consentementAdopte, consentementConjointAdoptant,
                ageAdopte, adoptantMarie);

        // Forme recommandée : si demande plénière OK → PLENIERE ; sinon si simple OK → SIMPLE ;
        // sinon AUCUNE.
        FormeAdoption formeRecommandee;
        VerdictRecevabilite verdict;
        ConditionsBilan bilanFinal;
        boolean basculePleniereVersSimple = false;

        if (formeAdoption == FormeAdoption.PLENIERE) {
            if (plenierBilan.cardinalsOk) {
                formeRecommandee = FormeAdoption.PLENIERE;
                bilanFinal = plenierBilan;
            } else if (simpleBilan.cardinalsOk) {
                formeRecommandee = FormeAdoption.SIMPLE;
                bilanFinal = simpleBilan;
                basculePleniereVersSimple = true;
            } else {
                formeRecommandee = FormeAdoption.AUCUNE;
                bilanFinal = plenierBilan;
            }
        } else { // SIMPLE
            if (simpleBilan.cardinalsOk) {
                formeRecommandee = FormeAdoption.SIMPLE;
                bilanFinal = simpleBilan;
            } else {
                formeRecommandee = FormeAdoption.AUCUNE;
                bilanFinal = simpleBilan;
            }
        }

        // Verdict
        if (formeRecommandee == FormeAdoption.AUCUNE) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (bilanFinal.cardinalsOk && bilanFinal.secondairesOk) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else if (bilanFinal.cardinalsOk) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else {
            verdict = VerdictRecevabilite.FAIBLE;
        }

        // Délai d'instruction estimé
        int delaiInstructionMois;
        if (formeRecommandee == FormeAdoption.AUCUNE) {
            delaiInstructionMois = DELAI_INSTRUCTION_MAX_MOIS;
        } else if (verdict == VerdictRecevabilite.ELEVEE) {
            delaiInstructionMois = DELAI_INSTRUCTION_MIN_MOIS;
        } else {
            delaiInstructionMois = (DELAI_INSTRUCTION_MIN_MOIS + DELAI_INSTRUCTION_MAX_MOIS) / 2;
        }

        // Documents requis
        List<String> documentsRequis = buildDocumentsRequis(formeRecommandee, pupilleEtat,
                adoptantMarie, ageAdopte);

        // Risque de refus
        List<String> risqueRefus = buildRisqueRefus(bilanFinal.criteresNonRemplis,
                basculePleniereVersSimple, formeRecommandee);

        String formule = String.format(Locale.ROOT,
                "Forme demandée=%s + AgeAdoptant=%d + AgeAdopte=%d + DiffAge=%d + "
                        + "ConsParents=%s + ConsAdopte=%s + ConsConjoint=%s + Enquetes=%s + "
                        + "Placement6m=%s + Pupille=%s + Marie=%s → "
                        + "Forme recommandée=%s, Verdict=%s, Délai=%d mois, "
                        + "%d critère(s) non rempli(s)%s",
                formeAdoption.name(), ageAdoptant, ageAdopte, differenceAgeAns,
                consentementParents, consentementAdopte, consentementConjointAdoptant,
                enquetes, placement6mois, pupilleEtat, adoptantMarie,
                formeRecommandee.name(), verdict.name(), delaiInstructionMois,
                bilanFinal.criteresNonRemplis.size(),
                basculePleniereVersSimple ? " (bascule PLENIERE → SIMPLE)" : "");

        List<String> messages = buildMessages(formeAdoption, formeRecommandee, verdict,
                ageAdoptant, ageAdopte, differenceAgeAns,
                bilanFinal.criteresNonRemplis, basculePleniereVersSimple, pupilleEtat);

        return new AdoptionResult(
                formeAdoption,
                ageAdoptant,
                ageAdopte,
                differenceAgeAns,
                consentementParents,
                consentementAdopte,
                consentementConjointAdoptant,
                enquetes,
                placement6mois,
                pupilleEtat,
                adoptantMarie,
                countryNormalized,
                verdict,
                formeRecommandee,
                bilanFinal.criteresNonRemplis,
                delaiInstructionMois,
                documentsRequis,
                risqueRefus,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static final class ConditionsBilan {
        final boolean cardinalsOk;
        final boolean secondairesOk;
        final List<String> criteresNonRemplis;

        ConditionsBilan(boolean cardinalsOk, boolean secondairesOk, List<String> criteresNonRemplis) {
            this.cardinalsOk = cardinalsOk;
            this.secondairesOk = secondairesOk;
            this.criteresNonRemplis = criteresNonRemplis;
        }
    }

    private static ConditionsBilan evaluerPleniere(int ageAdoptant, int ageAdopte,
                                                   int differenceAgeAns,
                                                   boolean consentementParents,
                                                   boolean consentementAdopte,
                                                   boolean consentementConjointAdoptant,
                                                   boolean enquetes,
                                                   boolean placement6mois,
                                                   boolean pupilleEtat,
                                                   boolean adoptantMarie) {
        List<String> manquants = new ArrayList<>();
        boolean cardinalsOk = true;

        // Âge adoptant — 26 ans pour couple marié, 28 pour célibataire
        int ageMin = adoptantMarie ? AGE_MIN_ADOPTANT_COUPLE : AGE_MIN_ADOPTANT_CELIBATAIRE;
        if (ageAdoptant < ageMin) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Âge adoptant insuffisant : %d ans < %d ans requis (art. 343-1 Cciv)",
                    ageAdoptant, ageMin));
        }

        // Différence d'âge ≥ 15 ans
        if (differenceAgeAns < DIFF_AGE_MIN_ANS) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Différence d'âge insuffisante : %d ans < 15 ans requis (art. 344 Cciv)",
                    differenceAgeAns));
        }

        // Adopté < 15 ans (sauf exceptions)
        if (ageAdopte >= AGE_MAX_ADOPTE_PLENIERE) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Adopté trop âgé pour plénière : %d ans ≥ 15 ans (art. 345 Cciv) — "
                            + "sauf exceptions limitées (recueilli avant 15 ans, possession d'état)",
                    ageAdopte));
        }

        // Consentement parents biologiques (sauf pupille État)
        if (!consentementParents && !pupilleEtat) {
            cardinalsOk = false;
            manquants.add("Consentement des parents biologiques manquant (art. 348 Cciv) — "
                    + "sauf statut pupille de l'État (art. 347)");
        }

        // Consentement de l'adopté ≥ 13 ans
        if (ageAdopte >= AGE_CONSENTEMENT_ADOPTE && !consentementAdopte) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Consentement de l'adopté manquant (art. 345 al. 3 Cciv) — adopté %d ans ≥ 13 ans",
                    ageAdopte));
        }

        // Consentement conjoint si adoptant marié
        if (adoptantMarie && !consentementConjointAdoptant) {
            cardinalsOk = false;
            manquants.add("Consentement du conjoint adoptant manquant (art. 343 Cciv)");
        }

        // Placement 6 mois
        boolean secondairesOk = true;
        if (!placement6mois) {
            cardinalsOk = false;
            manquants.add("Placement de 6 mois non effectué (art. 345-1 Cciv) — "
                    + "obligatoire en plénière sauf cas pupille déjà confié");
        }

        // Enquêtes sociales — secondaire (peuvent être en cours)
        if (!enquetes) {
            secondairesOk = false;
            manquants.add("Enquêtes sociales non finalisées (services départementaux, "
                    + "art. L. 225-2 CASF) — critère secondaire mais nécessaire avant audience");
        }

        return new ConditionsBilan(cardinalsOk, secondairesOk, manquants);
    }

    private static ConditionsBilan evaluerSimple(int ageAdoptant,
                                                 int differenceAgeAns,
                                                 boolean consentementParents,
                                                 boolean consentementAdopte,
                                                 boolean consentementConjointAdoptant,
                                                 int ageAdopte,
                                                 boolean adoptantMarie) {
        List<String> manquants = new ArrayList<>();
        boolean cardinalsOk = true;

        // Adoption simple : âge minimum 26 ans (art. 343 al. 1 par renvoi via 361)
        if (ageAdoptant < AGE_MIN_ADOPTANT_COUPLE) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Âge adoptant insuffisant pour adoption simple : %d ans < 26 ans "
                            + "(art. 343 par renvoi art. 361 Cciv)",
                    ageAdoptant));
        }

        if (differenceAgeAns < DIFF_AGE_MIN_ANS) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Différence d'âge insuffisante : %d ans < 15 ans (art. 344 Cciv)",
                    differenceAgeAns));
        }

        // Consentement adopté ≥ 13 ans (art. 360 al. 3)
        if (ageAdopte >= AGE_CONSENTEMENT_ADOPTE && !consentementAdopte) {
            cardinalsOk = false;
            manquants.add(String.format(Locale.ROOT,
                    "Consentement de l'adopté manquant (art. 360 al. 3 Cciv) — adopté %d ans ≥ 13 ans",
                    ageAdopte));
        }

        // Consentement parents si adopté mineur (en simple, l'adopté peut être majeur)
        if (ageAdopte < 18 && !consentementParents) {
            cardinalsOk = false;
            manquants.add("Consentement des parents biologiques manquant pour adopté mineur "
                    + "(art. 360 par renvoi art. 348 Cciv)");
        }

        // Consentement conjoint si adoptant marié
        if (adoptantMarie && !consentementConjointAdoptant) {
            cardinalsOk = false;
            manquants.add("Consentement du conjoint adoptant manquant (art. 343 Cciv "
                    + "applicable par renvoi art. 361)");
        }

        // En adoption simple, pas de placement requis ni enquêtes lourdes (mais
        // une enquête peut être ordonnée par le juge — secondaire)
        return new ConditionsBilan(cardinalsOk, true, manquants);
    }

    private static List<String> buildDocumentsRequis(FormeAdoption forme, boolean pupille,
                                                     boolean marie, int ageAdopte) {
        List<String> docs = new ArrayList<>();
        docs.add("Requête en adoption signée par avocat");
        docs.add("Extrait d'acte de naissance de l'adopté");
        docs.add("Extrait d'acte de naissance de l'adoptant");
        docs.add("Justificatifs de situation de l'adoptant (revenus, logement)");
        if (forme == FormeAdoption.PLENIERE) {
            docs.add("Décision d'agrément du conseil départemental (art. L. 225-2 CASF)");
            if (!pupille) {
                docs.add("Acte authentique de consentement des parents biologiques "
                        + "(art. 348-3 Cciv)");
            } else {
                docs.add("Procès-verbal d'admission comme pupille de l'État (art. 347)");
            }
            docs.add("Rapport d'enquête sociale (services départementaux)");
            docs.add("Rapport médico-psychologique");
            docs.add("Justificatif de placement 6 mois (art. 345-1)");
        } else if (forme == FormeAdoption.SIMPLE) {
            if (ageAdopte < 18) {
                docs.add("Acte authentique de consentement des parents biologiques "
                        + "(art. 360 par renvoi 348-3)");
            }
        }
        if (ageAdopte >= AGE_CONSENTEMENT_ADOPTE) {
            docs.add("Acte authentique de consentement de l'adopté (≥ 13 ans)");
        }
        if (marie) {
            docs.add("Acte authentique de consentement du conjoint adoptant");
            docs.add("Extrait d'acte de mariage");
        }
        return docs;
    }

    private static List<String> buildRisqueRefus(List<String> manquants, boolean bascule,
                                                  FormeAdoption formeRecommandee) {
        List<String> risques = new ArrayList<>();
        if (formeRecommandee == FormeAdoption.AUCUNE) {
            risques.add("Risque ÉLEVÉ — aucune forme d'adoption n'est applicable en l'état. "
                    + "Le tribunal rejettera la requête sans régularisation préalable des "
                    + "critères cardinaux.");
        }
        if (bascule) {
            risques.add("Bascule recommandée plénière → simple — la requête initiale en "
                    + "plénière serait probablement rejetée (conditions plénière non remplies). "
                    + "Une requête en adoption simple présente une perspective favorable.");
        }
        risques.addAll(manquants);
        return risques;
    }

    private static List<String> buildMessages(FormeAdoption demandee, FormeAdoption recommandee,
                                              VerdictRecevabilite verdict,
                                              int ageAdoptant, int ageAdopte, int diffAge,
                                              List<String> manquants, boolean bascule,
                                              boolean pupille) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Adoption (art. 343-370-2 Cciv) — le droit français connaît 2 formes : "
                + "plénière (effacement filiation d'origine, art. 343-359, irrévocable) et "
                + "simple (double filiation, art. 360-370-2, révocable pour motif grave).");

        msgs.add(String.format(Locale.ROOT,
                "Adoptant : %d ans — Adopté : %d ans — Différence d'âge : %d ans (≥ 15 ans requis art. 344).",
                ageAdoptant, ageAdopte, diffAge));

        if (demandee == FormeAdoption.PLENIERE) {
            msgs.add("Forme demandée : ADOPTION PLÉNIÈRE (art. 343-359 Cciv) — conditions strictes : "
                    + "adoptant ≥ 28 ans (ou 26 ans si couple marié, L. n°2022-219), différence "
                    + "d'âge ≥ 15 ans, adopté < 15 ans (sauf exceptions), placement 6 mois "
                    + "(art. 345-1), consentements parents/adopté/conjoint, enquêtes sociales.");
        } else {
            msgs.add("Forme demandée : ADOPTION SIMPLE (art. 360-370-2 Cciv) — conditions : "
                    + "adoptant ≥ 26 ans, différence d'âge ≥ 15 ans, consentement adopté ≥ 13 ans, "
                    + "consentement parents si adopté mineur, consentement conjoint si adoptant marié. "
                    + "La filiation d'origine est maintenue.");
        }

        if (pupille) {
            msgs.add("Adopté pupille de l'État (art. 347 Cciv) — le consentement des parents "
                    + "biologiques n'est pas requis. Voie privilégiée pour l'adoption plénière.");
        }

        if (bascule) {
            msgs.add("BASCULE RECOMMANDÉE : plénière → simple. Les conditions de l'adoption "
                    + "plénière ne sont pas remplies en l'état mais celles de l'adoption simple "
                    + "le sont. L'adoption simple permet d'établir un lien juridique sans "
                    + "effacer la filiation d'origine.");
        }

        if (recommandee == FormeAdoption.AUCUNE) {
            msgs.add("Forme recommandée : AUCUNE — aucune forme n'est applicable en l'état. "
                    + "Régulariser les conditions cardinales avant tout dépôt.");
        } else {
            msgs.add("Forme recommandée : " + recommandee.name() + ".");
        }

        if (!manquants.isEmpty()) {
            msgs.add("Critères non remplis (" + manquants.size() + ") : "
                    + String.join(" | ", manquants));
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — toutes les conditions cardinales et "
                    + "secondaires sont remplies. Préparer la requête et l'audience devant "
                    + "le tribunal judiciaire (chambre civile, formation collégiale).");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — conditions cardinales remplies mais "
                    + "critères secondaires manquants (typiquement enquêtes sociales en cours). "
                    + "Compléter le dossier avant audience.");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — un ou plusieurs critères cardinaux ne sont "
                    + "pas remplis. La requête serait rejetée. Régulariser ou réorienter "
                    + "vers la forme alternative (si applicable).");
        }

        msgs.add("Délai d'instruction typique : " + DELAI_INSTRUCTION_MIN_MOIS + " à "
                + DELAI_INSTRUCTION_MAX_MOIS + " mois selon le tribunal et la complexité.");
        msgs.add("Tribunal compétent : tribunal judiciaire — pôle famille, formation "
                + "collégiale, représentation par avocat obligatoire (art. 1166 CPC). "
                + "Ministère public obligatoirement entendu.");
        msgs.add("Effets en cas de prononcé : plénière = filiation d'origine effacée + "
                + "nom acquis (art. 357), changement d'état civil ; simple = double filiation, "
                + "double nom possible (art. 363), vocation successorale double (art. 368).");
        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }
}

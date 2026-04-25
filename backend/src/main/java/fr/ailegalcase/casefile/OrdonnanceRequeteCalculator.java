package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-23-01 : calculateur de probabilité d'obtention d'une ordonnance sur requête
 * (mesures urgentes familiales — art. 493 + 497 CPC FR / art. 1025 et s. CJ BE).
 *
 * <p>Procédure <strong>non-contradictoire</strong> par exception au principe du
 * contradictoire (art. 14 CPC FR). Le juge statue sans convoquer l'adversaire,
 * lequel peut former un référé-rétractation art. 497 CPC dans les 15 jours suivant
 * la signification.</p>
 *
 * <p>Critères d'éligibilité évalués (art. 493 CPC) :</p>
 * <ul>
 *   <li><strong>urgenceJustifiee</strong> : urgence concrète documentée par pièces</li>
 *   <li><strong>derogationContradictoireJustifiee</strong> : raison concrète de
 *       ne pas convoquer l'adverse (l'avertir détruirait l'effet utile)</li>
 *   <li><strong>pieceJustificativeFournie</strong> : pièce minimale au dossier</li>
 * </ul>
 *
 * <p>Outil applicable <strong>FRANCE et BELGIQUE</strong> — la procédure est
 * équivalente dans les 2 pays. La base juridique est adaptée selon le {@code country}.</p>
 */
public final class OrdonnanceRequeteCalculator {

    /** Motif de la requête (situation justifiant la procédure non-contradictoire). */
    public enum MotifRequete {
        /** Suspicion de détournement du patrimoine du conjoint avant divorce — accès au dossier bancaire. */
        DETOURNEMENT_PATRIMOINE,
        /** Risque imminent d'enlèvement international d'enfant — IST 373-2-6 Cciv. */
        ENLEVEMENT_INTERNATIONAL,
        /** Suspicion d'organisation d'insolvabilité — saisies conservatoires. */
        ORGANISATION_INSOLVABILITE,
        /** Constat d'huissier au domicile commun pour preuve de cohabitation/abandon. */
        PREUVE_ABANDON,
        /** Production forcée de pièces (relevés bancaires, factures, comptabilité). */
        ACCES_PIECES
    }

    /** Verdict de probabilité d'obtention de l'ordonnance. */
    public enum VerdictAccordeProbabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Critères de recevabilité évalués par le calculator. */
    public enum CritereRecevabilite {
        URGENCE_JUSTIFIEE,
        DEROGATION_CONTRADICTOIRE_JUSTIFIEE,
        PIECE_JUSTIFICATIVE_FOURNIE
    }

    /** Délai de référé-rétractation art. 497 CPC (FR) / opposition (BE). */
    public static final int RECOURS_ADVERSE_DELAI_JOURS = 15;

    /** Délai typique IST (enfant) : 24-72h. */
    public static final int DELAI_IST_MIN_JOURS = 1;
    public static final int DELAI_IST_MAX_JOURS = 3;

    /** Délai typique autres motifs : 5-15 jours. */
    public static final int DELAI_AUTRES_MIN_JOURS = 5;
    public static final int DELAI_AUTRES_MAX_JOURS = 15;

    private OrdonnanceRequeteCalculator() {}

    /**
     * Évalue la probabilité de délivrance de l'ordonnance sur requête.
     *
     * @param motifRequete                       motif (obligatoire)
     * @param urgenceJustifiee                   l'urgence est-elle concrètement justifiée
     * @param derogationContradictoireJustifiee  la dérogation au contradictoire est-elle justifiée
     * @param pieceJustificativeFournie          au moins une pièce minimale au dossier
     * @param presenceEnfants                    enfants concernés (force le délai IST si motif IST)
     * @param country                            pays du workspace (FRANCE ou BELGIQUE)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static OrdonnanceRequeteResult compute(MotifRequete motifRequete,
                                                   boolean urgenceJustifiee,
                                                   boolean derogationContradictoireJustifiee,
                                                   boolean pieceJustificativeFournie,
                                                   boolean presenceEnfants,
                                                   String country) {
        if (motifRequete == null) {
            throw new IllegalArgumentException("Motif de requête requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized) && !"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — la requête unilatérale est applicable FRANCE et BELGIQUE uniquement.");
        }

        // 1) Critères remplis / manquants
        List<CritereRecevabilite> remplis = new ArrayList<>();
        List<CritereRecevabilite> manquants = new ArrayList<>();
        if (urgenceJustifiee) remplis.add(CritereRecevabilite.URGENCE_JUSTIFIEE);
        else manquants.add(CritereRecevabilite.URGENCE_JUSTIFIEE);

        if (derogationContradictoireJustifiee) remplis.add(CritereRecevabilite.DEROGATION_CONTRADICTOIRE_JUSTIFIEE);
        else manquants.add(CritereRecevabilite.DEROGATION_CONTRADICTOIRE_JUSTIFIEE);

        if (pieceJustificativeFournie) remplis.add(CritereRecevabilite.PIECE_JUSTIFICATIVE_FOURNIE);
        else manquants.add(CritereRecevabilite.PIECE_JUSTIFICATIVE_FOURNIE);

        // 2) Score : 33 points par critère
        int score;
        switch (remplis.size()) {
            case 3 -> score = 100;
            case 2 -> score = 66;
            case 1 -> score = 33;
            default -> score = 0;
        }

        // 3) Verdict :
        //   - dérogation au contradictoire manquante → toujours FAIBLE (le juge renvoie en référé)
        //   - sinon, mapping selon score
        VerdictAccordeProbabilite verdict;
        if (!derogationContradictoireJustifiee) {
            verdict = VerdictAccordeProbabilite.FAIBLE;
        } else if (score == 100) {
            verdict = VerdictAccordeProbabilite.ELEVEE;
        } else if (score >= 66) {
            verdict = VerdictAccordeProbabilite.MOYENNE;
        } else {
            verdict = VerdictAccordeProbabilite.FAIBLE;
        }

        // 4) Délai typique : IST (enfants ou motif ENLEVEMENT) → 1-3 jours, sinon 5-15
        boolean estIst = motifRequete == MotifRequete.ENLEVEMENT_INTERNATIONAL || presenceEnfants;
        int delaiMin = estIst ? DELAI_IST_MIN_JOURS : DELAI_AUTRES_MIN_JOURS;
        int delaiMax = estIst ? DELAI_IST_MAX_JOURS : DELAI_AUTRES_MAX_JOURS;

        // 5) Base juridique
        String baseJuridique = buildBaseJuridique(countryNormalized, motifRequete, presenceEnfants);

        // 6) Formule
        String formule = String.format(Locale.ROOT,
                "Motif %s + %d critères remplis / %d manquants → score %d → verdict %s → délai %d-%d jours",
                motifRequete.name(), remplis.size(), manquants.size(), score, verdict.name(),
                delaiMin, delaiMax);

        // 7) Messages
        List<String> messages = buildMessages(motifRequete, verdict, remplis, manquants,
                delaiMin, delaiMax, countryNormalized, presenceEnfants);

        return new OrdonnanceRequeteResult(
                motifRequete,
                urgenceJustifiee,
                derogationContradictoireJustifiee,
                pieceJustificativeFournie,
                presenceEnfants,
                countryNormalized,
                score,
                verdict,
                remplis,
                manquants,
                delaiMin,
                delaiMax,
                RECOURS_ADVERSE_DELAI_JOURS,
                baseJuridique,
                formule,
                messages
        );
    }

    private static String buildBaseJuridique(String country, MotifRequete motif, boolean presenceEnfants) {
        StringBuilder sb = new StringBuilder();
        if ("FRANCE".equals(country)) {
            sb.append("Art. 493 + 497 CPC");
            if (motif == MotifRequete.ENLEVEMENT_INTERNATIONAL || presenceEnfants) {
                sb.append(" + 373-2-6 Cciv (mesures concernant l'enfant)");
            }
            if (motif == MotifRequete.ORGANISATION_INSOLVABILITE) {
                sb.append(" + 67 et s. L. 09/07/1991 (saisies conservatoires)");
            }
            if (motif == MotifRequete.ACCES_PIECES) {
                sb.append(" + 145 CPC (mesures d'instruction in futurum)");
            }
        } else {
            // BELGIQUE
            sb.append("Art. 1025 et s. CJ (requête unilatérale)");
            if (motif == MotifRequete.ENLEVEMENT_INTERNATIONAL || presenceEnfants) {
                sb.append(" + 387bis Cciv (mesures urgentes concernant l'enfant)");
            }
            if (motif == MotifRequete.ORGANISATION_INSOLVABILITE) {
                sb.append(" + 1413 et s. CJ (saisies conservatoires)");
            }
            if (motif == MotifRequete.ACCES_PIECES) {
                sb.append(" + 877 CJ (production forcée de pièces)");
            }
        }
        return sb.toString();
    }

    private static List<String> buildMessages(MotifRequete motif,
                                               VerdictAccordeProbabilite verdict,
                                               List<CritereRecevabilite> remplis,
                                               List<CritereRecevabilite> manquants,
                                               int delaiMin, int delaiMax,
                                               String country,
                                               boolean presenceEnfants) {
        List<String> msgs = new ArrayList<>();

        msgs.add(libelleMotif(motif) + " — procédure non-contradictoire par exception "
                + "au principe du contradictoire (" + (country.equals("FRANCE") ? "art. 14 CPC" : "art. 774 CJ") + ").");

        msgs.add("Critères remplis : " + remplis.size() + "/3.");
        if (!manquants.isEmpty()) {
            StringBuilder mq = new StringBuilder("Critères manquants :");
            for (CritereRecevabilite c : manquants) {
                mq.append("\n • ").append(libelleCritere(c));
            }
            msgs.add(mq.toString());
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — délivrance probable de l'ordonnance. "
                    + "Délai typique : " + delaiMin + "-" + delaiMax + " jours.");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — délivrance possible mais pas garantie. "
                    + "Renforcer les pièces et la motivation. Délai : " + delaiMin + "-" + delaiMax + " jours.");
            case FAIBLE -> {
                msgs.add("VERDICT FAIBLE — délivrance peu probable.");
                if (manquants.contains(CritereRecevabilite.DEROGATION_CONTRADICTOIRE_JUSTIFIEE)) {
                    msgs.add("La dérogation au principe du contradictoire n'étant pas justifiée, "
                            + "le juge renverra vraisemblablement vers le référé contradictoire "
                            + "(" + (country.equals("FRANCE") ? "art. 484 CPC" : "art. 1035 CJ") + ").");
                }
            }
        }

        if (motif == MotifRequete.ENLEVEMENT_INTERNATIONAL || presenceEnfants) {
            msgs.add("Mesures concernant l'enfant — l'IST (interdiction de sortie du territoire) "
                    + "est délivrée en urgence (24-72h) et inscrite au fichier des personnes "
                    + "recherchées (FPR) "
                    + (country.equals("FRANCE") ? "(art. 1180-4 CPC FR)" : "(art. 387bis Cciv BE)") + ".");
        }

        if (motif == MotifRequete.ORGANISATION_INSOLVABILITE) {
            msgs.add("Saisies conservatoires — exécution immédiate sur autorisation, "
                    + "rendues caduques si non confirmées au fond dans les délais légaux.");
        }

        msgs.add("RECOURS ADVERSE — la partie adverse peut former un référé-rétractation "
                + "(" + (country.equals("FRANCE") ? "art. 497 CPC" : "art. 1033 CJ") + ") "
                + "dans les " + RECOURS_ADVERSE_DELAI_JOURS + " jours suivant la signification "
                + "de l'ordonnance.");

        return msgs;
    }

    private static String libelleMotif(MotifRequete m) {
        return switch (m) {
            case DETOURNEMENT_PATRIMOINE -> "Consultation du dossier bancaire / patrimonial du conjoint "
                    + "(suspicion de détournement avant divorce)";
            case ENLEVEMENT_INTERNATIONAL -> "Interdiction de sortie du territoire (IST) avec enfant "
                    + "(risque d'enlèvement international)";
            case ORGANISATION_INSOLVABILITE -> "Saisies conservatoires "
                    + "(suspicion d'organisation d'insolvabilité)";
            case PREUVE_ABANDON -> "Constat d'huissier au domicile commun "
                    + "(preuve de cohabitation / d'abandon)";
            case ACCES_PIECES -> "Production forcée de pièces "
                    + "(relevés bancaires, factures, comptabilité)";
        };
    }

    private static String libelleCritere(CritereRecevabilite c) {
        return switch (c) {
            case URGENCE_JUSTIFIEE -> "Urgence concrète justifiée par pièces";
            case DEROGATION_CONTRADICTOIRE_JUSTIFIEE -> "Dérogation au principe du contradictoire justifiée "
                    + "(l'avertir détruirait l'effet utile)";
            case PIECE_JUSTIFICATIVE_FOURNIE -> "Pièce justificative minimale fournie au dossier";
        };
    }
}

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-19-01 : calculateur pour l'analyse d'éligibilité d'un mineur étranger
 * à l'un des 4 dispositifs FR :
 *
 * <ul>
 *   <li><b>MNA_ORDONNANCE_JE</b> (Mineur Non Accompagné) — art. 375 Cciv +
 *       L.221-2-2 CASF : ordonnance du juge des enfants plaçant le mineur étranger
 *       isolé sous protection ASE.</li>
 *   <li><b>TITRE_SEJOUR_L435_3</b> — CESEDA L.435-3 : enfant étranger né en France
 *       ayant ≥ 3 ans + un parent en situation régulière.</li>
 *   <li><b>DCEM</b> — CESEDA R.321-3 : Document de Circulation Étranger Mineur,
 *       permet à un mineur étranger résidant en France de revenir après voyage.</li>
 *   <li><b>TIR</b> — CESEDA R.321-7 : Titre d'Identité Républicain, pour mineurs
 *       apatrides ou bénéficiaires asile.</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. La Belgique relève d'une procédure tutelle MENA
 * via le Service des Tutelles (DGDE) — feature jumelle au backlog (F-IM-19-BE).
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>ELEVEE</b> : tous les critères du dispositif sont réunis.</li>
 *   <li><b>MOYENNE</b> : critères limites (minorité contestable proche 18 ans,
 *       entrée non documentée, etc.).</li>
 *   <li><b>FAIBLE</b> : critère bloquant (majorité, isolement non avéré pour MNA,
 *       parent non régulier pour L.435-3, motif d'ordre public pour DCEM, etc.).</li>
 * </ul>
 */
public final class MineursImmigrationCalculator {

    /** Âge maximum d'un mineur (la majorité civile FR est à 18 ans). */
    public static final int AGE_MAJORITE = 18;

    /** Seuil d'âge à partir duquel la minorité devient contestable (proche 18 ans). */
    public static final int AGE_MINORITE_CONTESTABLE = 17;

    /** Durée minimale de résidence en France pour L.435-3 (3 ans). */
    public static final int DUREE_RESIDENCE_MIN_ANNEES_L435_3 = 3;

    public static final String VERDICT_ELEVEE = "ELEVEE";
    public static final String VERDICT_MOYENNE = "MOYENNE";
    public static final String VERDICT_FAIBLE = "FAIBLE";

    public static final String DISPOSITIF_MNA = "MNA_ORDONNANCE_JE";
    public static final String DISPOSITIF_L435_3 = "TITRE_SEJOUR_L435_3";
    public static final String DISPOSITIF_DCEM = "DCEM";
    public static final String DISPOSITIF_TIR = "TIR";

    /** Délais d'instruction par dispositif (mois). */
    public static final int DELAI_MNA_MOIS = 4;
    public static final int DELAI_L435_3_MOIS = 6;
    public static final int DELAI_DCEM_MOIS = 2;
    public static final int DELAI_TIR_MOIS = 3;

    private MineursImmigrationCalculator() {
    }

    public static MineursImmigrationResult compute(String dispositifVise,
                                                   LocalDate dateNaissance,
                                                   LocalDate dateEntreeFrance,
                                                   boolean parentRegulier,
                                                   boolean isolementAvere,
                                                   boolean motifOrdrePublic,
                                                   String nationalite) {
        return compute(dispositifVise, dateNaissance, dateEntreeFrance,
                parentRegulier, isolementAvere, motifOrdrePublic, nationalite,
                LocalDate.now());
    }

    /**
     * Variante avec date d'analyse explicite (pour tests reproductibles).
     */
    public static MineursImmigrationResult compute(String dispositifVise,
                                                   LocalDate dateNaissance,
                                                   LocalDate dateEntreeFrance,
                                                   boolean parentRegulier,
                                                   boolean isolementAvere,
                                                   boolean motifOrdrePublic,
                                                   String nationalite,
                                                   LocalDate dateAnalyse) {
        validateInputs(dispositifVise, dateNaissance, dateEntreeFrance, dateAnalyse);

        String dispositif = dispositifVise.trim().toUpperCase();
        Dispositif d = resolveDispositif(dispositif);

        int ageAnnees = Period.between(dateNaissance, dateAnalyse).getYears();
        boolean estMajeur = ageAnnees >= AGE_MAJORITE;

        List<String> criteresNonRemplis = new ArrayList<>();
        List<String> documentsRequis = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // Bloque transversal : majorité → tous les dispositifs deviennent FAIBLE
        if (estMajeur) {
            criteresNonRemplis.add("Majorité atteinte (" + ageAnnees + " ans) — les 4 dispositifs"
                    + " sont strictement réservés aux mineurs.");
        }

        String verdictBase = applyDispositif(d, ageAnnees, estMajeur, dateNaissance, dateEntreeFrance,
                parentRegulier, isolementAvere, motifOrdrePublic, dateAnalyse,
                criteresNonRemplis, documentsRequis, messages);

        String verdictFinal = estMajeur ? VERDICT_FAIBLE : verdictBase;

        int delai = delaiPourDispositif(d.code());

        String formule = buildFormule(d, ageAnnees, verdictFinal);
        messages.add("Délai d'instruction estimatif : " + delai + " mois.");

        return new MineursImmigrationResult(
                dispositif,
                d.code(),
                dateNaissance,
                dateEntreeFrance,
                parentRegulier,
                isolementAvere,
                motifOrdrePublic,
                nationalite,
                ageAnnees,
                verdictFinal,
                criteresNonRemplis,
                documentsRequis,
                delai,
                d.baseJuridique(),
                formule,
                messages);
    }

    private static void validateInputs(String dispositifVise,
                                       LocalDate dateNaissance,
                                       LocalDate dateEntreeFrance,
                                       LocalDate dateAnalyse) {
        if (dispositifVise == null || dispositifVise.isBlank()) {
            throw new IllegalArgumentException("dispositifVise est requis");
        }
        if (dateNaissance == null) {
            throw new IllegalArgumentException("dateNaissance est requise");
        }
        if (dateNaissance.isAfter(dateAnalyse)) {
            throw new IllegalArgumentException("dateNaissance ne peut pas être dans le futur");
        }
        if (dateEntreeFrance != null && dateEntreeFrance.isBefore(dateNaissance)) {
            throw new IllegalArgumentException(
                    "dateEntreeFrance doit être ≥ dateNaissance");
        }
    }

    private static Dispositif resolveDispositif(String code) {
        switch (code) {
            case DISPOSITIF_MNA:
                return new Dispositif(DISPOSITIF_MNA,
                        "Cciv art. 375 + CASF L.221-2-2");
            case DISPOSITIF_L435_3:
                return new Dispositif(DISPOSITIF_L435_3,
                        "CESEDA L.435-3");
            case DISPOSITIF_DCEM:
                return new Dispositif(DISPOSITIF_DCEM,
                        "CESEDA R.321-3");
            case DISPOSITIF_TIR:
                return new Dispositif(DISPOSITIF_TIR,
                        "CESEDA R.321-7");
            default:
                throw new IllegalArgumentException(
                        "Dispositif non supporté : " + code
                                + " (attendus : MNA_ORDONNANCE_JE, TITRE_SEJOUR_L435_3, DCEM, TIR)");
        }
    }

    private static String applyDispositif(Dispositif d,
                                          int ageAnnees,
                                          boolean estMajeur,
                                          LocalDate dateNaissance,
                                          LocalDate dateEntreeFrance,
                                          boolean parentRegulier,
                                          boolean isolementAvere,
                                          boolean motifOrdrePublic,
                                          LocalDate dateAnalyse,
                                          List<String> criteresNonRemplis,
                                          List<String> documentsRequis,
                                          List<String> messages) {
        switch (d.code()) {
            case DISPOSITIF_MNA: {
                documentsRequis.add("Acte de naissance original (ou copie certifiée)");
                documentsRequis.add("Justificatif d'isolement (rapport éducateur, signalement préfecture)");
                documentsRequis.add("Examens médicaux d'âge (osseux) — contestables, à utiliser avec prudence");
                documentsRequis.add("Pièce d'identité étrangère si disponible");

                if (estMajeur) {
                    return VERDICT_FAIBLE;
                }
                if (!isolementAvere) {
                    criteresNonRemplis.add("Isolement non avéré — un adulte référent est présent."
                            + " La saisine du JE pour MNA n'est pas pertinente (cf. art. 375 Cciv).");
                    return VERDICT_FAIBLE;
                }
                messages.add("Saisine JE par requête conjointe procureur + ASE (signalement obligatoire).");
                if (ageAnnees >= AGE_MINORITE_CONTESTABLE) {
                    messages.add("Âge proche 18 ans : la minorité peut être contestée par la préfecture"
                            + " (examens osseux non probants à eux seuls — Cass. crim. 2017).");
                    return VERDICT_MOYENNE;
                }
                return VERDICT_ELEVEE;
            }

            case DISPOSITIF_L435_3: {
                documentsRequis.add("Acte de naissance prouvant la naissance en France");
                documentsRequis.add("Justificatifs de résidence en France pendant ≥ 3 ans");
                documentsRequis.add("Titre de séjour ou justificatif de régularité du parent");
                documentsRequis.add("Livret de famille ou acte de filiation");

                if (estMajeur) {
                    return VERDICT_FAIBLE;
                }
                // Vérification née en France : si dateEntreeFrance ≈ dateNaissance, on considère né en France
                boolean neEnFrance = dateEntreeFrance == null
                        || !dateEntreeFrance.isAfter(dateNaissance.plusDays(31));
                if (!neEnFrance) {
                    criteresNonRemplis.add("Enfant non né en France (date d'entrée postérieure à la naissance)"
                            + " — L.435-3 réservé aux enfants nés sur le territoire français.");
                    return VERDICT_FAIBLE;
                }
                if (!parentRegulier) {
                    criteresNonRemplis.add("Aucun parent en situation régulière —"
                            + " L.435-3 exige au moins un parent titulaire d'un titre de séjour.");
                    return VERDICT_FAIBLE;
                }
                int dureeResidence = Period.between(dateNaissance, dateAnalyse).getYears();
                if (dureeResidence < DUREE_RESIDENCE_MIN_ANNEES_L435_3) {
                    criteresNonRemplis.add("Résidence en France < " + DUREE_RESIDENCE_MIN_ANNEES_L435_3
                            + " ans (actuelle : " + dureeResidence + " an(s)) — condition L.435-3 non remplie.");
                    return VERDICT_FAIBLE;
                }
                messages.add("À 18 ans : titre de séjour automatique selon résidence (cf. CESEDA L.435-3).");
                return VERDICT_ELEVEE;
            }

            case DISPOSITIF_DCEM: {
                documentsRequis.add("Justificatif du titre de séjour ou statut régulier du mineur");
                documentsRequis.add("Acte de naissance ou pièce d'identité");
                documentsRequis.add("Justificatif de domicile");
                documentsRequis.add("Photos d'identité aux normes");

                if (estMajeur) {
                    return VERDICT_FAIBLE;
                }
                if (motifOrdrePublic) {
                    criteresNonRemplis.add("Motif d'ordre public présent —"
                            + " bloquant pour DCEM (art. R.321-3 CESEDA).");
                    return VERDICT_FAIBLE;
                }
                messages.add("DCEM permet le retour en France après voyage à l'étranger sans"
                        + " visa de retour. Validité 5 ans renouvelable.");
                return VERDICT_ELEVEE;
            }

            case DISPOSITIF_TIR: {
                documentsRequis.add("Décision OFPRA / CNDA reconnaissant statut apatride ou réfugié");
                documentsRequis.add("Acte de naissance");
                documentsRequis.add("Justificatif de domicile du parent / représentant légal");
                documentsRequis.add("Photos d'identité aux normes");

                if (estMajeur) {
                    return VERDICT_FAIBLE;
                }
                messages.add("TIR : tient lieu de pièce d'identité pour le mineur apatride / réfugié."
                        + " Permet la circulation sans passeport du pays d'origine.");
                return VERDICT_ELEVEE;
            }
        }
        return VERDICT_FAIBLE; // unreachable
    }

    private static int delaiPourDispositif(String code) {
        switch (code) {
            case DISPOSITIF_MNA: return DELAI_MNA_MOIS;
            case DISPOSITIF_L435_3: return DELAI_L435_3_MOIS;
            case DISPOSITIF_DCEM: return DELAI_DCEM_MOIS;
            case DISPOSITIF_TIR: return DELAI_TIR_MOIS;
            default: return 3;
        }
    }

    private static String buildFormule(Dispositif d, int ageAnnees, String verdict) {
        return "Mineur étranger — dispositif " + d.code()
                + " — âge " + ageAnnees + " ans — verdict " + verdict + ".";
    }

    /** Couple code dispositif + base juridique. */
    private record Dispositif(String code, String baseJuridique) {
    }
}

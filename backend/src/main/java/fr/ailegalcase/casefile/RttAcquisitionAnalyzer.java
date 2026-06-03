package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-49 : analyseur de l'outil "RTT — acquisition selon accord d'aménagement
 * du temps de travail" (art. L.3121-41 à L.3121-44 CT, F-DT-80). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ;
 * <b>DISTINCT</b> des heures supplémentaires F-DT-19 et de la monétisation de
 * RTT F-DT-51) : lorsqu'un accord collectif fixe un horaire hebdomadaire
 * supérieur à 35 h (ex. 37 h ou 39 h), les heures effectuées entre 35 h et
 * l'horaire collectif sont compensées par l'attribution de jours de repos
 * (JRTT), <b>sans majoration</b>.
 *
 * <ul>
 *   <li><b>Renvoi heures supplémentaires</b> — si aucun accord d'aménagement
 *       n'est présent, les heures au-delà de 35 h relèvent du régime des heures
 *       supplémentaires (outil dédié F-DT-19) : statut RENVOI_HEURES_SUP, pas de
 *       calcul de JRTT.</li>
 *   <li><b>Calcul JRTT</b> (accord présent) :
 *       heuresAuDela = horaireCollectif − 35 ;
 *       heuresAnnuelles = heuresAuDela × semainesTravailleesAn ;
 *       dureeJourEnHeures = horaireCollectif / 5 ;
 *       nombreJrttTheorique = heuresAnnuelles / dureeJourEnHeures.</li>
 * </ul>
 *
 * <p>Garde-fou : l'horaire hebdomadaire collectif doit être &gt; 35 et &le; 48
 * (durée maximale hebdomadaire, art. L.3121-20 CT). Base juridique annotée « à
 * vérifier par avocat ».
 */
public final class RttAcquisitionAnalyzer {

    /** Durée légale hebdomadaire de référence (heures). */
    static final double DUREE_LEGALE_HEBDO = 35d;

    /** Durée maximale hebdomadaire absolue (art. L.3121-20 CT). */
    static final double HORAIRE_MAX_HEBDO = 48d;

    /** Nombre de semaines travaillées par défaut (hors congés). */
    static final int SEMAINES_DEFAUT = 47;

    /** Nombre de jours travaillés par semaine pour le calcul de la durée d'un JRTT. */
    static final int JOURS_PAR_SEMAINE = 5;

    static final String BASE_JURIDIQUE =
            "art. L.3121-41 à L.3121-44 du Code du travail — aménagement du temps de "
                    + "travail sur une période supérieure à la semaine (au plus égale à "
                    + "l'année) par accord collectif : les heures effectuées au-delà de 35 h "
                    + "en moyenne peuvent être compensées par des jours de repos (JRTT) sans "
                    + "constituer des heures supplémentaires, tant que la moyenne reste dans "
                    + "les limites de l'accord. Les JRTT compensent les heures effectuées "
                    + "entre 35 h et l'horaire collectif et ne donnent lieu à aucune "
                    + "majoration (à la différence des heures supplémentaires — outil dédié "
                    + "F-DT-19). Accord d'entreprise / convention collective à vérifier "
                    + "(à vérifier par avocat)";

    static final String NOTE_SANS_MAJORATION =
            "Les JRTT compensent les heures effectuées entre 35 h et l'horaire collectif "
                    + "et ne donnent lieu à aucune majoration (à la différence des heures "
                    + "supplémentaires).";

    static final String NOTE_RENVOI_HEURES_SUP =
            "À défaut d'accord d'aménagement, les heures effectuées au-delà de 35 h "
                    + "relèvent du régime des heures supplémentaires (voir l'outil dédié — "
                    + "F-DT-19).";

    private RttAcquisitionAnalyzer() {
    }

    /**
     * Calcule le nombre théorique de JRTT acquis sur l'année, ou renvoie au
     * régime des heures supplémentaires à défaut d'accord d'aménagement.
     */
    public static RttAcquisitionResult analyze(
            Double horaireHebdomadaireCollectif,
            Boolean accordCollectifPresent,
            Integer semainesTravailleesAn) {

        validate(horaireHebdomadaireCollectif, accordCollectifPresent, semainesTravailleesAn);

        double horaire = horaireHebdomadaireCollectif;
        int semaines = semainesTravailleesAn != null ? semainesTravailleesAn : SEMAINES_DEFAUT;
        boolean accord = Boolean.TRUE.equals(accordCollectifPresent);
        List<String> notes = new ArrayList<>();

        if (!accord) {
            notes.add(NOTE_RENVOI_HEURES_SUP);
            return new RttAcquisitionResult(
                    RttAcquisitionStatut.RENVOI_HEURES_SUP,
                    horaire,
                    false,
                    semaines,
                    null,
                    "absence d'accord d'aménagement du temps de travail sur l'année",
                    List.copyOf(notes),
                    BASE_JURIDIQUE);
        }

        double heuresAuDela = horaire - DUREE_LEGALE_HEBDO;
        double heuresAnnuelles = heuresAuDela * semaines;
        double dureeJourEnHeures = horaire / JOURS_PAR_SEMAINE;
        double jrtt = BigDecimal.valueOf(heuresAnnuelles / dureeJourEnHeures)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        String base = "horaire collectif " + formatHoraire(horaire) + " h, " + semaines
                + " semaines travaillées, JRTT sans majoration";

        notes.add("Accord d'aménagement du temps de travail sur l'année présent : les "
                + "heures effectuées entre 35 h et " + formatHoraire(horaire) + " h sont "
                + "compensées par l'attribution de JRTT (art. L.3121-41 à L.3121-44 CT).");
        notes.add("Calcul : (" + formatHoraire(horaire) + " − 35) × " + semaines
                + " semaines = " + formatHoraire(heuresAnnuelles) + " h compensées, "
                + "réparties en journées de " + formatHoraire(dureeJourEnHeures)
                + " h (horaire collectif / 5) → " + formatHoraire(jrtt) + " JRTT théoriques "
                + "sur l'année.");
        notes.add(NOTE_SANS_MAJORATION);

        return new RttAcquisitionResult(
                RttAcquisitionStatut.CALCULE,
                horaire,
                true,
                semaines,
                jrtt,
                base,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(Double horaireHebdomadaireCollectif,
                                 Boolean accordCollectifPresent,
                                 Integer semainesTravailleesAn) {
        if (horaireHebdomadaireCollectif == null) {
            throw new IllegalArgumentException("horaireHebdomadaireCollectif est requis");
        }
        if (accordCollectifPresent == null) {
            throw new IllegalArgumentException("accordCollectifPresent est requis");
        }
        if (horaireHebdomadaireCollectif <= DUREE_LEGALE_HEBDO) {
            throw new IllegalArgumentException(
                    "horaireHebdomadaireCollectif doit être strictement supérieur à 35 h");
        }
        if (horaireHebdomadaireCollectif > HORAIRE_MAX_HEBDO) {
            throw new IllegalArgumentException(
                    "horaireHebdomadaireCollectif doit être inférieur ou égal à 48 h "
                            + "(durée maximale hebdomadaire, art. L.3121-20 CT)");
        }
        if (semainesTravailleesAn != null && semainesTravailleesAn <= 0) {
            throw new IllegalArgumentException(
                    "semainesTravailleesAn doit être strictement positif");
        }
    }

    private static String formatHoraire(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}

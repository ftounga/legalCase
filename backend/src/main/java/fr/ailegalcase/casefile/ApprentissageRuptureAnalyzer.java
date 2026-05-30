package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * SF-218-23 : analyseur de la <b>validité de la rupture du contrat
 * d'apprentissage</b> (art. L.6222-18 et s. CT, F-DT-110). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Calcul de la période</b> : {@code joursDepuisDebut = dateDebutContrat
 *       → dateRupture} (bornes incluses). Au plus tard au 45e jour →
 *       {@code DANS_45_PREMIERS_JOURS} ; au-delà → {@code APRES_45_JOURS}.
 *       Constante {@code SEUIL_RUPTURE_LIBRE_JOURS = 45}.</li>
 *   <li><b>Dans les 45 jours</b> : rupture libre par l'une ou l'autre partie,
 *       sans motif ni indemnité (forme écrite requise). Quel que soit le motif
 *       invoqué, {@code validite = VALIDE} (rupture libre).</li>
 *   <li><b>Après 45 jours</b> : le motif doit appartenir à { ACCORD_PARTIES,
 *       FAUTE_GRAVE, FORCE_MAJEURE, INAPTITUDE, EXCLUSION_DEFINITIVE_CFA }.
 *       <ul>
 *         <li>SANS_MOTIF → {@code NON_VALIDE} (rupture irrégulière :
 *             dommages-intérêts possibles, saisine CPH) ;</li>
 *         <li>FAUTE_GRAVE → {@code A_SECURISER} : la résiliation passe désormais
 *             par une procédure de licenciement (réforme 2018) ;</li>
 *         <li>ACCORD_PARTIES / FORCE_MAJEURE / INAPTITUDE /
 *             EXCLUSION_DEFINITIVE_CFA → {@code VALIDE}.</li>
 *       </ul></li>
 *   <li><b>Verdict global</b> : VALIDE → RUPTURE_REGULIERE ; NON_VALIDE →
 *       RUPTURE_IRREGULIERE ; A_SECURISER → RUPTURE_A_SECURISER.</li>
 * </ul>
 *
 * <p>Hors périmètre : chiffrage des dommages-intérêts en cas de rupture
 * irrégulière (renvoi vers les calculateurs existants), contrat de
 * professionnalisation, régime du stagiaire (F-DT-109). Base juridique annotée
 * « à vérifier par avocat ».
 */
public final class ApprentissageRuptureAnalyzer {

    /** Seuil de la rupture libre : 45 premiers jours de formation pratique en entreprise. */
    static final int SEUIL_RUPTURE_LIBRE_JOURS = 45;

    /** Motifs recevables pour rompre le contrat au-delà des 45 jours. */
    static final Set<ApprentissageMotifRupture> MOTIFS_AUTORISES_APRES_45J = EnumSet.of(
            ApprentissageMotifRupture.ACCORD_PARTIES,
            ApprentissageMotifRupture.FAUTE_GRAVE,
            ApprentissageMotifRupture.FORCE_MAJEURE,
            ApprentissageMotifRupture.INAPTITUDE,
            ApprentissageMotifRupture.EXCLUSION_DEFINITIVE_CFA);

    static final String BASE_JURIDIQUE =
            "art. L.6222-18 CT — rupture du contrat d'apprentissage : libre durant "
                    + "les 45 premiers jours (en entreprise), puis sur accord écrit des "
                    + "parties ou, à défaut, pour faute grave / manquements répétés / "
                    + "inaptitude ; art. L.6222-18-1 CT (rupture pour inaptitude médicale "
                    + "constatée par le médecin du travail, dispense de reclassement) ; "
                    + "art. L.6222-21 CT (exclusion définitive du CFA, motif de rupture) ; "
                    + "art. L.6222-23 CT (poursuite de la formation). Réforme 2018 (loi "
                    + "Avenir professionnel) : suppression de la résiliation judiciaire "
                    + "systématique, alignement partiel sur le licenciement. Qualification "
                    + "et conséquences à vérifier par l'avocat (à vérifier par avocat)";

    private ApprentissageRuptureAnalyzer() {
    }

    /**
     * Analyse la validité de la rupture du contrat d'apprentissage : détermine
     * la période (avant / après le 45e jour), apprécie la recevabilité du motif
     * et rend la validité, les conséquences et le verdict global.
     *
     * @param dateDebutContrat début de l'exécution du contrat (requis).
     * @param dateRupture date de la rupture (requise, ≥ dateDebutContrat).
     * @param auteurRupture auteur de la rupture (requis).
     * @param motifRupture motif invoqué (requis).
     * @param apprentiMajeur true si l'apprenti est majeur (défaut true).
     */
    public static ApprentissageRuptureResult analyze(LocalDate dateDebutContrat,
                                                     LocalDate dateRupture,
                                                     ApprentissageAuteurRupture auteurRupture,
                                                     ApprentissageMotifRupture motifRupture,
                                                     Boolean apprentiMajeur) {
        validate(dateDebutContrat, dateRupture, auteurRupture, motifRupture);

        boolean majeur = apprentiMajeur == null || apprentiMajeur;

        // Jours écoulés depuis le début du contrat, bornes incluses.
        long joursDepuisDebut = ChronoUnit.DAYS.between(dateDebutContrat, dateRupture) + 1;
        boolean dansPeriodeLibre = joursDepuisDebut <= SEUIL_RUPTURE_LIBRE_JOURS;
        ApprentissagePeriodeRupture periode = dansPeriodeLibre
                ? ApprentissagePeriodeRupture.DANS_45_PREMIERS_JOURS
                : ApprentissagePeriodeRupture.APRES_45_JOURS;

        List<String> consequences = new ArrayList<>();
        ApprentissageRuptureValidite validite;
        String motif;

        if (dansPeriodeLibre) {
            // Rupture libre des 45 premiers jours : valable quel que soit le motif.
            validite = ApprentissageRuptureValidite.VALIDE;
            motif = "Rupture libre durant les 45 premiers jours de formation pratique en "
                    + "entreprise (art. L.6222-18) : aucune motivation requise, forme écrite "
                    + "obligatoire et notification au CFA.";
            consequences.add("Notifier la rupture par écrit et informer le CFA (art. L.6222-18).");
        } else if (!MOTIFS_AUTORISES_APRES_45J.contains(motifRupture)) {
            // SANS_MOTIF après 45 jours → rupture irrégulière.
            validite = ApprentissageRuptureValidite.NON_VALIDE;
            motif = "Rupture sans motif au-delà du 45e jour : irrégulière. Au-delà de la "
                    + "période initiale, la rupture est limitée à l'accord écrit des parties, "
                    + "la faute grave, la force majeure, l'inaptitude médicale ou l'exclusion "
                    + "définitive du CFA (art. L.6222-18).";
            consequences.add("Rupture irrégulière : dommages-intérêts possibles au profit de "
                    + "l'apprenti (à chiffrer).");
            consequences.add("Saisine du conseil de prud'hommes (CPH).");
        } else {
            switch (motifRupture) {
                case ACCORD_PARTIES -> {
                    validite = ApprentissageRuptureValidite.VALIDE;
                    motif = "Rupture d'un commun accord des parties au-delà du 45e jour : "
                            + "valable si constatée par un écrit signé des deux parties "
                            + "(art. L.6222-18).";
                    consequences.add("Établir un écrit de rupture signé des deux parties et en "
                            + "informer le CFA.");
                }
                case FAUTE_GRAVE -> {
                    validite = ApprentissageRuptureValidite.A_SECURISER;
                    motif = "Rupture pour faute grave de l'apprenti : recevable mais soumise à "
                            + "la procédure de licenciement depuis la réforme 2018 (loi Avenir "
                            + "professionnel).";
                    consequences.add("Engager la procédure de licenciement (convocation à "
                            + "entretien préalable, notification motivée) — formalité requise.");
                }
                case FORCE_MAJEURE -> {
                    validite = ApprentissageRuptureValidite.VALIDE;
                    motif = "Rupture pour force majeure : événement extérieur, imprévisible et "
                            + "irrésistible rendant l'exécution du contrat impossible "
                            + "(art. L.6222-18).";
                    consequences.add("Documenter la situation de force majeure invoquée.");
                }
                case INAPTITUDE -> {
                    validite = ApprentissageRuptureValidite.VALIDE;
                    motif = "Rupture pour inaptitude médicale constatée par le médecin du "
                            + "travail (art. L.6222-18-1) : l'employeur est dispensé de "
                            + "l'obligation de reclassement.";
                    consequences.add("S'appuyer sur le constat d'inaptitude du médecin du "
                            + "travail (art. L.6222-18-1).");
                }
                case EXCLUSION_DEFINITIVE_CFA -> {
                    validite = ApprentissageRuptureValidite.VALIDE;
                    motif = "Rupture consécutive à l'exclusion définitive de l'apprenti du CFA "
                            + "(art. L.6222-21) : motif valable de rupture par l'employeur.";
                    consequences.add("Justifier l'exclusion définitive prononcée par le CFA "
                            + "(art. L.6222-21).");
                }
                default -> throw new IllegalArgumentException("motifRupture inconnu");
            }
        }

        // Continuité de formation (art. L.6222-23) — information générale.
        consequences.add("Possibilité pour l'apprenti de poursuivre sa formation théorique au "
                + "CFA pendant six mois (art. L.6222-23).");
        if (!majeur) {
            consequences.add("Apprenti mineur : associer le représentant légal aux formalités "
                    + "de rupture.");
        }

        ApprentissageRuptureVerdict verdict = switch (validite) {
            case VALIDE -> ApprentissageRuptureVerdict.RUPTURE_REGULIERE;
            case NON_VALIDE -> ApprentissageRuptureVerdict.RUPTURE_IRREGULIERE;
            case A_SECURISER -> ApprentissageRuptureVerdict.RUPTURE_A_SECURISER;
        };

        return new ApprentissageRuptureResult(
                dateDebutContrat,
                dateRupture,
                auteurRupture,
                motifRupture,
                majeur,
                joursDepuisDebut,
                periode,
                dansPeriodeLibre,
                validite,
                verdict,
                List.copyOf(consequences),
                motif,
                BASE_JURIDIQUE);
    }

    private static void validate(LocalDate dateDebutContrat,
                                 LocalDate dateRupture,
                                 ApprentissageAuteurRupture auteurRupture,
                                 ApprentissageMotifRupture motifRupture) {
        if (dateDebutContrat == null) {
            throw new IllegalArgumentException("dateDebutContrat est requise");
        }
        if (dateRupture == null) {
            throw new IllegalArgumentException("dateRupture est requise");
        }
        if (dateRupture.isBefore(dateDebutContrat)) {
            throw new IllegalArgumentException("dateRupture ne peut pas précéder dateDebutContrat");
        }
        if (auteurRupture == null) {
            throw new IllegalArgumentException("auteurRupture est requis");
        }
        if (motifRupture == null) {
            throw new IllegalArgumentException("motifRupture est requis");
        }
    }
}

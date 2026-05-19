package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-18-05 : calculateur de recevabilité d'une action en recherche de
 * paternité (FR — art. 327 + 340 + 16-11 + 321 Cciv).
 *
 * <p>L'action en recherche de paternité est l'action judiciaire engagée par
 * l'enfant (ou son représentant) pour faire <strong>établir</strong>
 * judiciairement un lien de paternité avec un homme qui ne l'a pas reconnu
 * volontairement. Symétrique inverse de la contestation (SF-03) :
 * recherche = création, contestation = annulation.</p>
 *
 * <p>Trois qualités du demandeur :</p>
 * <ul>
 *   <li><strong>ENFANT_MAJEUR</strong> (art. 327 al. 2) — l'enfant agit lui-même
 *       à sa majorité. Délai : 10 ans à compter de la majorité (art. 321).</li>
 *   <li><strong>REPRESENTANT_LEGAL_MINEUR</strong> (art. 327 al. 2) — pendant
 *       la minorité, le représentant légal (mère, tuteur) agit au nom de
 *       l'enfant. Le délai est suspendu pendant la minorité (art. 321).</li>
 *   <li><strong>MERE</strong> (art. 327) — la mère agit en représentation
 *       légale du mineur (cas le plus courant) ou pour soutenir l'enfant
 *       majeur. L'action de la mère à titre personnel (art. 340 ancien) est
 *       caduque depuis 2005.</li>
 * </ul>
 *
 * <p>Expertise ADN (art. 16-11 Cciv) : presque systématique — la
 * jurisprudence Cass. 1ère civ. 28 mars 2000 pose qu'elle est de droit hors
 * motif légitime de refus, avec présomption en faveur du demandeur en cas de
 * refus injustifié du défendeur.</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong>. La Belgique a un régime
 * distinct (CC art. 322 al. 1, 332ter) qui sera traité par une feature
 * jumelle au backlog.</p>
 */
public final class RecherchePaterniteCalculator {

    /** Qualité du demandeur (art. 327 al. 2 Cciv). */
    public enum QualiteDuDemandeur {
        /** Enfant agissant lui-même à sa majorité. */
        ENFANT_MAJEUR,
        /** Représentant légal du mineur (le plus souvent la mère, ou un tuteur). */
        REPRESENTANT_LEGAL_MINEUR,
        /** Mère agissant en représentation de son enfant mineur. */
        MERE
    }

    /** Verdict de recevabilité de l'action en recherche. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Délai de prescription à compter de la majorité (art. 321 Cciv). */
    public static final int DELAI_PRESCRIPTION_ANS = 10;

    /** Âge de la majorité civile en France. */
    public static final int AGE_MAJORITE_FR = 18;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 327 + 340 + 16-11 + 321 Cciv";

    private RecherchePaterniteCalculator() {}

    /**
     * Évalue la recevabilité d'une action en recherche de paternité.
     *
     * @param qualiteDuDemandeur          qualité du demandeur (obligatoire)
     * @param dateNaissanceEnfant         date de naissance de l'enfant (obligatoire)
     * @param presomptionPossessionEtat   éléments de possession d'état (traitement, fama, nomen) ?
     * @param expertiseAdnDemandee        expertise ADN demandée / envisagée ?
     * @param pereDesigneRefuseADN        le père désigné a-t-il refusé l'ADN sans motif légitime ?
     * @param motifsSerieux               éléments précis et concordants (correspondances, témoignages) ?
     * @param today                       date de référence pour les calculs (utilisée pour les tests ; en prod = LocalDate.now())
     * @param country                     pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static RecherchePaterniteResult compute(QualiteDuDemandeur qualiteDuDemandeur,
                                                   LocalDate dateNaissanceEnfant,
                                                   Boolean presomptionPossessionEtat,
                                                   Boolean expertiseAdnDemandee,
                                                   Boolean pereDesigneRefuseADN,
                                                   Boolean motifsSerieux,
                                                   LocalDate today,
                                                   String country) {
        if (qualiteDuDemandeur == null) {
            throw new IllegalArgumentException("Qualité du demandeur requise");
        }
        if (dateNaissanceEnfant == null) {
            throw new IllegalArgumentException(
                    "Date de naissance de l'enfant requise");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC art. 322 al. 1, 332ter — qualités à agir,"
                            + " délais et présomptions ADN spécifiques) sera traité dans une"
                            + " feature jumelle dédiée au backlog.");
        }
        if (presomptionPossessionEtat == null) presomptionPossessionEtat = false;
        if (expertiseAdnDemandee == null) expertiseAdnDemandee = false;
        if (pereDesigneRefuseADN == null) pereDesigneRefuseADN = false;
        if (motifsSerieux == null) motifsSerieux = false;
        LocalDate referenceDate = (today != null) ? today : LocalDate.now();

        // Calcul de la majorité de l'enfant et des délais
        LocalDate dateMajorite = dateNaissanceEnfant.plusYears(AGE_MAJORITE_FR);
        boolean enfantEstMineur = referenceDate.isBefore(dateMajorite);

        // Pour ENFANT_MAJEUR et MERE/REPRESENTANT_LEGAL agissant pour un enfant
        // déjà majeur : prescription 10 ans à compter de la majorité.
        // Pendant la minorité (REPRESENTANT_LEGAL_MINEUR ou MERE pour un mineur) :
        // suspension du délai (art. 321) — délai prescrit "restant" = délai entier
        // + (date majorité - today) en mois. Exposition lisible : on retourne le
        // nombre de mois restants jusqu'à la forclusion = (dateMajorite + 10 ans) - today.
        LocalDate dateForclusion = dateMajorite.plusYears(DELAI_PRESCRIPTION_ANS);
        long delaiPrescriptionRestantMois = ChronoUnit.MONTHS.between(referenceDate, dateForclusion);

        boolean prescrit = delaiPrescriptionRestantMois <= 0;

        // Présomption refus ADN — pleinement effective seulement si ADN demandée
        boolean presomptionRefusADN = pereDesigneRefuseADN && expertiseAdnDemandee;

        // Faisceau d'indices
        int signauxPositifs = 0;
        if (presomptionPossessionEtat) signauxPositifs++;
        if (expertiseAdnDemandee) signauxPositifs++;
        if (motifsSerieux) signauxPositifs++;
        if (presomptionRefusADN) signauxPositifs++;

        // Calcul du score (0 à 100)
        int score = 0;
        if (!prescrit) {
            score += 30; // critère cardinal
            // Bonus si le délai est très peu consommé (cas typique : minorité ou jeune majeur)
            long delaiTotalMois = (long) DELAI_PRESCRIPTION_ANS * 12;
            if (enfantEstMineur) {
                // Suspension : on bonifie quand l'enfant est mineur (le délai n'a même pas commencé)
                score += 15;
            } else if (delaiTotalMois > 0) {
                double pctRestant = (double) delaiPrescriptionRestantMois / delaiTotalMois;
                if (pctRestant > 0.5) {
                    score += 10;
                }
            }
        }
        if (presomptionPossessionEtat) score += 15;
        if (expertiseAdnDemandee) score += 20;
        if (motifsSerieux) score += 15;
        if (presomptionRefusADN) score += 15;

        // Verdict — seuils calibrés sur le faisceau d'indices typique :
        // un demandeur recevable a au moins ADN + (motifs sérieux OU possession
        // d'état OU refus ADN) + délai non prescrit → score ≥ 65.
        VerdictRecevabilite verdict;
        if (prescrit) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (signauxPositifs == 0) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (score >= 65) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else if (score >= 45) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else {
            verdict = VerdictRecevabilite.FAIBLE;
        }

        // Recommandation expertise ADN — toujours quand l'action est non prescrite
        // car l'expertise est de droit (art. 16-11 + Cass. 28/3/2000)
        boolean expertiseAdnRecommandee = !prescrit;

        // Risques de refus
        List<String> risquesRefus = buildRisquesRefus(qualiteDuDemandeur, prescrit,
                expertiseAdnDemandee, motifsSerieux, presomptionPossessionEtat,
                pereDesigneRefuseADN, signauxPositifs, delaiPrescriptionRestantMois,
                enfantEstMineur);

        // Documents requis
        List<String> documents = buildDocuments(qualiteDuDemandeur, expertiseAdnDemandee,
                presomptionPossessionEtat);

        String formule = String.format(Locale.ROOT,
                "Qualité=%s + naissance=%s + majorité=%s + mineur=%s + délai=%d ans + "
                        + "restant=%d mois + possession état=%s + ADN=%s + refus ADN=%s + "
                        + "motifs=%s → score %d → verdict %s → ADN recommandée=%s, "
                        + "présomption refus=%s, %d risque(s), %d document(s)",
                qualiteDuDemandeur.name(), dateNaissanceEnfant.toString(),
                dateMajorite.toString(), enfantEstMineur,
                DELAI_PRESCRIPTION_ANS, delaiPrescriptionRestantMois,
                presomptionPossessionEtat, expertiseAdnDemandee, pereDesigneRefuseADN,
                motifsSerieux, score, verdict.name(), expertiseAdnRecommandee,
                presomptionRefusADN, risquesRefus.size(), documents.size());

        List<String> messages = buildMessages(qualiteDuDemandeur, verdict, prescrit,
                enfantEstMineur, presomptionPossessionEtat, expertiseAdnDemandee,
                pereDesigneRefuseADN, presomptionRefusADN, motifsSerieux,
                expertiseAdnRecommandee, delaiPrescriptionRestantMois,
                signauxPositifs);

        return new RecherchePaterniteResult(
                qualiteDuDemandeur,
                dateNaissanceEnfant,
                presomptionPossessionEtat,
                expertiseAdnDemandee,
                pereDesigneRefuseADN,
                motifsSerieux,
                countryNormalized,
                verdict,
                score,
                DELAI_PRESCRIPTION_ANS,
                delaiPrescriptionRestantMois,
                expertiseAdnRecommandee,
                presomptionRefusADN,
                risquesRefus,
                documents,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    /** Surcharge sans paramètre {@code today} (utilise {@code LocalDate.now()}). */
    public static RecherchePaterniteResult compute(QualiteDuDemandeur qualiteDuDemandeur,
                                                   LocalDate dateNaissanceEnfant,
                                                   Boolean presomptionPossessionEtat,
                                                   Boolean expertiseAdnDemandee,
                                                   Boolean pereDesigneRefuseADN,
                                                   Boolean motifsSerieux,
                                                   String country) {
        return compute(qualiteDuDemandeur, dateNaissanceEnfant,
                presomptionPossessionEtat, expertiseAdnDemandee,
                pereDesigneRefuseADN, motifsSerieux, null, country);
    }

    private static List<String> buildRisquesRefus(QualiteDuDemandeur qualite,
                                                  boolean prescrit,
                                                  boolean expertiseAdn,
                                                  boolean motifsSerieux,
                                                  boolean possessionEtat,
                                                  boolean refusADN,
                                                  int signauxPositifs,
                                                  long restantMois,
                                                  boolean enfantMineur) {
        List<String> risques = new ArrayList<>();
        if (prescrit) {
            risques.add("Prescription acquise — l'action est forclose ("
                    + Math.abs(restantMois) + " mois après l'expiration du délai légal "
                    + "de 10 ans à compter de la majorité). Aucune voie de droit ouverte "
                    + "sauf cas exceptionnel (art. 2240 et s. Cciv).");
        } else if (!enfantMineur && restantMois < 24) {
            risques.add("Délai de prescription quasi écoulé (" + restantMois + " mois restants) "
                    + "— assignation à délivrer en urgence pour interrompre la prescription.");
        }
        if (signauxPositifs == 0 && !prescrit) {
            risques.add("Aucun élément matériel produit — l'action en recherche de paternité "
                    + "exige un faisceau d'indices (possession d'état, correspondances, "
                    + "témoignages, expertise ADN demandée). Une demande d'ADN seule n'est "
                    + "pas systématiquement ordonnée à défaut de motif légitime initial.");
        }
        if (!expertiseAdn && !prescrit) {
            risques.add("Expertise ADN non demandée à ce stade — il convient de la solliciter "
                    + "dans l'assignation (art. 16-11 Cciv ; Cass. 1ère civ. 28 mars 2000 : "
                    + "l'expertise est de droit en matière de filiation hors motif légitime "
                    + "de refus).");
        }
        if (!motifsSerieux && !possessionEtat && !prescrit) {
            risques.add("Motifs sérieux et possession d'état non démontrés — la jurisprudence "
                    + "exige des éléments précis et concordants pour que le juge ordonne "
                    + "l'expertise génétique. Préparer les pièces probatoires en amont.");
        }
        if (refusADN && !expertiseAdn) {
            risques.add("Refus d'ADN allégué mais expertise non demandée — la présomption en "
                    + "faveur du demandeur (Cass. 28/3/2000) ne peut jouer que si l'expertise "
                    + "a été ordonnée et refusée. Demander d'abord l'expertise dans l'assignation.");
        }
        if (qualite == QualiteDuDemandeur.MERE && !enfantMineur) {
            risques.add("Mère agissant pour un enfant majeur — vérifier que l'enfant a "
                    + "consenti à l'action (art. 327 al. 2 : l'action appartient à l'enfant). "
                    + "Si l'enfant ne souhaite pas agir, l'action est irrecevable.");
        }
        return risques;
    }

    private static List<String> buildDocuments(QualiteDuDemandeur qualite,
                                               boolean expertiseAdn,
                                               boolean possessionEtat) {
        List<String> docs = new ArrayList<>();
        docs.add("Acte de naissance intégral de l'enfant (de moins de 3 mois)");
        docs.add("Pièce d'identité du demandeur (CNI ou passeport en cours de validité)");
        switch (qualite) {
            case ENFANT_MAJEUR -> docs.add("Justificatif de majorité de l'enfant "
                    + "(CNI / passeport / extrait registre état civil)");
            case REPRESENTANT_LEGAL_MINEUR -> docs.add("Justificatif de la qualité de "
                    + "représentant légal (livret de famille, jugement de tutelle, "
                    + "extrait registre tutelles)");
            case MERE -> docs.add("Livret de famille ou tout document établissant la "
                    + "qualité de mère et l'autorité parentale exclusive ou partagée");
        }
        docs.add("Coordonnées complètes du père désigné (état civil, adresse) "
                + "pour assignation");
        if (expertiseAdn) {
            docs.add("Demande d'expertise génétique judiciaire à inclure dans l'assignation "
                    + "(art. 16-11 Cciv — laboratoire agréé, prélèvement contradictoire)");
        } else {
            docs.add("Note d'audience pour solliciter une expertise ADN au visa de l'art. "
                    + "16-11 Cciv et de la jurisprudence Cass. 1ère civ. 28 mars 2000");
        }
        if (possessionEtat) {
            docs.add("Pièces établissant la possession d'état (art. 311-1 Cciv) : "
                    + "correspondances, photos de famille, témoignages écrits, "
                    + "actes de la vie civile mentionnant la paternité du défendeur");
        } else {
            docs.add("Tout élément de fait permettant de fonder la présomption de paternité "
                    + "(correspondances, photos, témoignages d'entourage, attestations)");
        }
        docs.add("Justificatif de domicile du demandeur");
        return docs;
    }

    private static List<String> buildMessages(QualiteDuDemandeur qualite,
                                              VerdictRecevabilite verdict,
                                              boolean prescrit,
                                              boolean enfantMineur,
                                              boolean possessionEtat,
                                              boolean expertiseAdn,
                                              boolean refusADN,
                                              boolean presomptionRefus,
                                              boolean motifsSerieux,
                                              boolean expertiseAdnRecommandee,
                                              long restantMois,
                                              int signauxPositifs) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Qualité du demandeur : " + libelleQualite(qualite));
        msgs.add("Délai de prescription applicable : 10 ans à compter de la majorité "
                + "de l'enfant (art. 321 Cciv) — suspension automatique pendant la minorité.");

        if (prescrit) {
            msgs.add("PRESCRIPTION ACQUISE — l'action est forclose ("
                    + Math.abs(restantMois) + " mois après l'expiration du délai). "
                    + "L'assignation serait déclarée irrecevable.");
        } else if (enfantMineur) {
            msgs.add("Enfant mineur — délai suspendu (art. 321 Cciv). Forclusion à 10 ans "
                    + "après la majorité, soit dans " + restantMois + " mois.");
        } else {
            msgs.add("Délai non prescrit — il reste " + restantMois
                    + " mois pour assigner.");
        }

        if (possessionEtat) {
            msgs.add("Possession d'état alléguée (art. 311-1 Cciv : tractatus, fama, nomen) "
                    + "— élément central qui facilite l'admission de l'action et l'octroi "
                    + "de l'expertise ADN.");
        }
        if (expertiseAdn) {
            msgs.add("Expertise ADN demandée — l'expertise génétique est de droit en matière "
                    + "de filiation hors motif légitime de refus (art. 16-11 Cciv ; Cass. 1ère "
                    + "civ. 28 mars 2000).");
        }
        if (presomptionRefus) {
            msgs.add("PRÉSOMPTION DE PATERNITÉ — le refus injustifié d'ADN par le défendeur "
                    + "constitue un indice grave que le juge peut retenir en faveur du "
                    + "demandeur (Cass. 1ère civ. 28 mars 2000 ; art. 11 CPC).");
        } else if (refusADN && !expertiseAdn) {
            msgs.add("Refus d'ADN signalé mais expertise non demandée — la présomption ne "
                    + "joue qu'après ordonnance et refus formel. Demander l'expertise.");
        }
        if (motifsSerieux) {
            msgs.add("Motifs sérieux et concordants énoncés — conformes à l'exigence "
                    + "jurisprudentielle pour ouvrir l'expertise génétique.");
        } else if (!possessionEtat) {
            msgs.add("Motifs sérieux NON démontrés et pas de possession d'état — la "
                    + "jurisprudence exige un faisceau d'indices (au-delà d'un simple "
                    + "doute) pour fonder l'expertise ADN.");
        }
        if (expertiseAdnRecommandee) {
            msgs.add("Expertise ADN RECOMMANDÉE — à solliciter dans l'assignation au visa de "
                    + "l'art. 16-11 Cciv (prélèvement contradictoire en laboratoire agréé). "
                    + "Demande quasi systématique en matière de recherche de paternité.");
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — l'action en recherche de paternité "
                    + "présente toutes les conditions favorables (qualité à agir + délai + "
                    + "faisceau d'indices + ADN). Engagement contentieux fortement recommandé.");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — l'action est recevable mais comporte "
                    + "des facteurs de risque (faisceau d'indices partiel, motifs limités). "
                    + "Préparer la défense avec une stratégie probatoire renforcée.");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — l'action serait probablement déclarée "
                    + "irrecevable (prescription) ou rejetée au fond (absence d'éléments). "
                    + "Étudier les voies alternatives (action en contestation 332 si déjà un "
                    + "lien à annuler ; possession d'état 317 ; reconnaissance volontaire 316) "
                    + "avant tout dépôt.");
        }

        msgs.add("Effets en cas de succès : la filiation paternelle est établie "
                + "rétroactivement (art. 327), avec les droits-devoirs associés "
                + "(autorité parentale art. 372, nom art. 311-21, contribution à l'entretien "
                + "art. 371-2, vocation successorale art. 733).");
        msgs.add("Tribunal compétent : tribunal judiciaire avec représentation obligatoire "
                + "par avocat (art. 318 al. 2 Cciv applicable par renvoi).");
        msgs.add("Note : l'action de la mère à titre personnel pour son propre préjudice "
                + "(art. 340 ancien) est caduque depuis l'ordonnance n°2005-759 du 4 juillet "
                + "2005. Seule subsiste l'action en représentation de l'enfant.");
        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }

    private static String libelleQualite(QualiteDuDemandeur q) {
        return switch (q) {
            case ENFANT_MAJEUR -> "Enfant majeur agissant lui-même (art. 327 al. 2 Cciv)";
            case REPRESENTANT_LEGAL_MINEUR -> "Représentant légal du mineur (art. 327 al. 2 Cciv)";
            case MERE -> "Mère agissant en représentation de l'enfant (art. 327 Cciv)";
        };
    }
}

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SF-223-07 : moteur décisionnel BE déterminant la <b>loi applicable</b> à une
 * situation familiale présentant un élément d'extranéité (DIP belge) — selon la
 * matière : divorce (Rome III, Règl. UE 1259/2010), régime matrimonial
 * (Règl. UE 2016/1103) ou succession (Règl. UE 650/2012) — complété par le CDIP
 * belge (loi du 16/07/2004). À VÉRIFIER PAR AVOCAT BELGE.
 *
 * <p><b>Arbre, pas calcul.</b> L'outil applique l'ÉCHELLE DE RATTACHEMENT propre
 * à chaque matière pour désigner la loi applicable et expose le
 * {@code fondementRattachement} retenu (traçabilité du raisonnement) :</p>
 * <ul>
 *   <li><b>DIVORCE</b> (Rome III, art. 5 puis art. 8) : 1) loi choisie par les
 *       parties (professio juris) ; 2) à défaut, résidence habituelle commune ;
 *       3) dernière résidence habituelle commune (non distinguée en V1, fusionnée
 *       avec la résidence commune actuelle) ; 4) nationalité commune ; 5) loi du
 *       for.</li>
 *   <li><b>REGIME_MATRIMONIAL</b> (Règl. 2016/1103, art. 22 puis art. 26) :
 *       loi choisie sinon première résidence habituelle commune après le mariage
 *       sinon nationalité commune au mariage sinon lien le plus étroit.</li>
 *   <li><b>SUCCESSION</b> (Règl. 650/2012, art. 22 puis art. 21) : professio
 *       juris (loi nationale du défunt) sinon résidence habituelle du défunt au
 *       décès.</li>
 * </ul>
 *
 * <p>Quand les éléments de rattachement disponibles ne permettent de désigner
 * AUCUNE loi (échelle épuisée sans donnée exploitable, hors loi du for du
 * divorce), le verdict est {@code LOI_INDETERMINABLE} : l'avocat doit compléter
 * les éléments de fait.</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la situation cadrée est la
 * <i>détermination de la loi applicable à une situation familiale internationale
 * à instruire</i>. DISTINCT de la reconnaissance / exequatur d'une décision
 * étrangère déjà rendue ({@code dip-be-reconnaissance-decision-etrangere},
 * SF-223-08). Ne tranche pas la compétence juridictionnelle (Bruxelles II ter —
 * hors périmètre V1). Aucune citation jurisprudentielle (F-JU-04 parké).</p>
 */
public final class DipBeLoiApplicableFamilleCalculator {

    /** Matière de DIP familial couverte. */
    public enum Matiere {
        DIVORCE,
        REGIME_MATRIMONIAL,
        SUCCESSION
    }

    /** Fondement de rattachement retenu (traçabilité du raisonnement). */
    public enum FondementRattachement {
        CHOIX_LOI_PARTIES,
        RESIDENCE_HABITUELLE_COMMUNE,
        NATIONALITE_COMMUNE,
        RESIDENCE_HABITUELLE_DEFUNT,
        LOI_DU_FOR,
        LIEN_LE_PLUS_ETROIT,
        AUCUN
    }

    /** Verdict de l'analyse. */
    public enum DipBeLoiApplicableFamilleVerdict {
        LOI_APPLICABLE_DETERMINEE,
        LOI_INDETERMINABLE
    }

    private static final Pattern ISO2 = Pattern.compile("^[A-Z]{2}$");

    private DipBeLoiApplicableFamilleCalculator() {}

    /**
     * Applique l'échelle de rattachement DIP propre à la matière.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, loi applicable, fondement, motifs,
     *         conseils, bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static DipBeLoiApplicableFamilleResult compute(
            DipBeLoiApplicableFamilleInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — loi applicable en droit de la famille (DIP)");
        }
        validateInputs(input);

        switch (input.matiere()) {
            case DIVORCE:
                return divorce(input);
            case REGIME_MATRIMONIAL:
                return regimeMatrimonial(input);
            case SUCCESSION:
                return succession(input);
            default:
                // Sécurité (théoriquement inatteignable — matière validée plus haut).
                return indeterminable(input,
                        "Matière non reconnue : compléter la matière (DIVORCE / REGIME_MATRIMONIAL / "
                                + "SUCCESSION).");
        }
    }

    // ---------------------------------------------------------------
    // DIVORCE — Rome III (Règl. UE 1259/2010), échelle art. 5 puis art. 8
    // ---------------------------------------------------------------

    private static DipBeLoiApplicableFamilleResult divorce(DipBeLoiApplicableFamilleInput in) {
        List<String> motifs = new ArrayList<>();
        // 1) Loi choisie par les parties (art. 5).
        if (in.choixLoiParLesParties() != null) {
            motifs.add("Les parties ont désigné la loi applicable au divorce (art. 5 du Règl. Rome III) : "
                    + "cette loi prime sur l'échelle subsidiaire (à vérifier par avocat belge — limites de "
                    + "l'art. 5 : lien de rattachement requis).");
            return determinee(in, in.choixLoiParLesParties(), FondementRattachement.CHOIX_LOI_PARTIES,
                    motifs, divorceConseils(in), basesDivorce());
        }
        // 2) Résidence habituelle commune (art. 8 a) — la « dernière résidence
        //    commune » de l'art. 8 b) n'est pas distinguée de la résidence commune
        //    actuelle en V1 : le champ unique alimente les deux échelons.
        if (in.residenceHabituelleCommune() != null) {
            motifs.add("À défaut de choix des parties, la loi de la résidence habituelle commune des époux "
                    + "s'applique (art. 8 a) — ou, le cas échéant, la dernière résidence commune au sens de "
                    + "l'art. 8 b) — du Règl. Rome III ; à vérifier par avocat belge).");
            return determinee(in, in.residenceHabituelleCommune(),
                    FondementRattachement.RESIDENCE_HABITUELLE_COMMUNE,
                    motifs, divorceConseils(in), basesDivorce());
        }
        // 3) Nationalité commune (art. 8 c).
        if (in.nationaliteCommune() != null) {
            motifs.add("À défaut de résidence habituelle commune, la loi de la nationalité commune des "
                    + "époux s'applique (art. 8 c) du Règl. Rome III ; à vérifier par avocat belge).");
            return determinee(in, in.nationaliteCommune(), FondementRattachement.NATIONALITE_COMMUNE,
                    motifs, divorceConseils(in), basesDivorce());
        }
        // 4) Loi du for (art. 8 d) — juridiction belge saisie → loi belge.
        motifs.add("Aucun choix, résidence ou nationalité commune n'étant établi, la loi de la juridiction "
                + "saisie s'applique (art. 8 d) du Règl. Rome III) : pour une juridiction belge, la loi "
                + "belge (à vérifier par avocat belge — sous réserve de la compétence, non tranchée ici).");
        return determinee(in, "BE", FondementRattachement.LOI_DU_FOR,
                motifs, divorceConseils(in), basesDivorce());
    }

    // ---------------------------------------------------------------
    // RÉGIME MATRIMONIAL — Règl. UE 2016/1103, art. 22 puis art. 26
    // ---------------------------------------------------------------

    private static DipBeLoiApplicableFamilleResult regimeMatrimonial(DipBeLoiApplicableFamilleInput in) {
        List<String> motifs = new ArrayList<>();
        // 1) Loi choisie par les époux (art. 22).
        if (in.choixLoiParLesParties() != null) {
            motifs.add("Les époux ont désigné la loi applicable à leur régime matrimonial (art. 22 du "
                    + "Règl. UE 2016/1103) : cette loi prime sur les rattachements objectifs (à vérifier "
                    + "par avocat belge — conditions de forme du choix).");
            return determinee(in, in.choixLoiParLesParties(), FondementRattachement.CHOIX_LOI_PARTIES,
                    motifs, regimeConseils(in), basesRegime());
        }
        // 2) Première résidence habituelle commune après le mariage (art. 26 § 1 a).
        if (in.residenceHabituelleCommune() != null) {
            motifs.add("À défaut de choix, la loi de la première résidence habituelle commune des époux "
                    + "après le mariage s'applique (art. 26 § 1 a) du Règl. UE 2016/1103 ; à vérifier par "
                    + "avocat belge).");
            return determinee(in, in.residenceHabituelleCommune(),
                    FondementRattachement.RESIDENCE_HABITUELLE_COMMUNE,
                    motifs, regimeConseils(in), basesRegime());
        }
        // 3) Nationalité commune au moment du mariage (art. 26 § 1 b).
        if (in.nationaliteCommune() != null) {
            motifs.add("À défaut de résidence habituelle commune, la loi de la nationalité commune des "
                    + "époux au moment du mariage s'applique (art. 26 § 1 b) du Règl. UE 2016/1103 ; à "
                    + "vérifier par avocat belge).");
            return determinee(in, in.nationaliteCommune(), FondementRattachement.NATIONALITE_COMMUNE,
                    motifs, regimeConseils(in), basesRegime());
        }
        // 4) Lien le plus étroit (art. 26 § 1 c) — nécessite une appréciation des
        //    faits qui ne peut être désignée automatiquement → indéterminable.
        return indeterminable(in,
                "À défaut de choix, de résidence habituelle commune et de nationalité commune, le régime "
                        + "relève de la loi de l'État avec lequel les époux ont, ensemble, le lien le plus "
                        + "étroit (art. 26 § 1 c) du Règl. UE 2016/1103) : ce rattachement suppose une "
                        + "appréciation des faits par l'avocat (à vérifier par avocat belge).");
    }

    // ---------------------------------------------------------------
    // SUCCESSION — Règl. UE 650/2012, art. 22 puis art. 21
    // ---------------------------------------------------------------

    private static DipBeLoiApplicableFamilleResult succession(DipBeLoiApplicableFamilleInput in) {
        List<String> motifs = new ArrayList<>();
        // 1) Professio juris : loi nationale du défunt choisie (art. 22).
        if (in.choixLoiParLesParties() != null) {
            motifs.add("Le défunt a choisi sa loi nationale pour régir sa succession (professio juris — "
                    + "art. 22 du Règl. UE 650/2012) : ce choix prime sur le rattachement objectif (à "
                    + "vérifier par avocat belge — le choix est limité à une loi nationale du défunt).");
            return determinee(in, in.choixLoiParLesParties(), FondementRattachement.CHOIX_LOI_PARTIES,
                    motifs, successionConseils(in), basesSuccession());
        }
        // 2) Résidence habituelle du défunt au moment du décès (art. 21 § 1).
        if (in.residenceHabituelleCommune() != null) {
            motifs.add("À défaut de professio juris, la loi de l'État de la résidence habituelle du défunt "
                    + "au moment du décès régit l'ensemble de la succession (art. 21 § 1 du Règl. UE "
                    + "650/2012 ; à vérifier par avocat belge — réserve de la clause d'exception art. 21 § 2).");
            return determinee(in, in.residenceHabituelleCommune(),
                    FondementRattachement.RESIDENCE_HABITUELLE_DEFUNT,
                    motifs, successionConseils(in), basesSuccession());
        }
        // 3) Aucune donnée → indéterminable.
        return indeterminable(in,
                "Ni professio juris ni résidence habituelle du défunt au décès n'étant documentées, la loi "
                        + "applicable à la succession ne peut être désignée (art. 21-22 du Règl. UE 650/2012) : "
                        + "compléter la résidence habituelle du défunt (à vérifier par avocat belge).");
    }

    // ---------------------------------------------------------------
    // Conseils par matière
    // ---------------------------------------------------------------

    private static List<String> divorceConseils(DipBeLoiApplicableFamilleInput in) {
        List<String> c = new ArrayList<>();
        c.add("Vérifier la compétence juridictionnelle (Bruxelles II ter) séparément : la loi applicable "
                + "(Rome III) ne préjuge pas du tribunal compétent (hors périmètre de cet outil).");
        c.add("Confirmer la résidence habituelle au sens autonome du droit de l'Union (centre des intérêts "
                + "de la vie), pas le simple domicile administratif (à vérifier par avocat belge).");
        return c;
    }

    private static List<String> regimeConseils(DipBeLoiApplicableFamilleInput in) {
        List<String> c = new ArrayList<>();
        c.add("Le rattachement du régime matrimonial est en principe IMMUABLE (figé au mariage) sauf "
                + "changement volontaire de loi : distinguer ce rattachement de celui du divorce (à vérifier "
                + "par avocat belge).");
        c.add("Articuler la loi du régime matrimonial avec la loi successorale en cas de décès : ce sont "
                + "deux rattachements distincts (Règl. 2016/1103 vs Règl. 650/2012).");
        return c;
    }

    private static List<String> successionConseils(DipBeLoiApplicableFamilleInput in) {
        List<String> c = new ArrayList<>();
        c.add("La loi successorale unique (art. 21) couvre meubles ET immeubles, où qu'ils soient situés : "
                + "le lieu de situation des immeubles n'est PAS un critère de rattachement autonome dans le "
                + "Règl. 650/2012 (à vérifier par avocat belge).");
        c.add("Vérifier l'opportunité d'un certificat successoral européen (CSE) et l'existence d'une "
                + "professio juris dans un testament antérieur (à vérifier par avocat belge).");
        return c;
    }

    // ---------------------------------------------------------------
    // Construction des résultats
    // ---------------------------------------------------------------

    private static DipBeLoiApplicableFamilleResult determinee(
            DipBeLoiApplicableFamilleInput in, String loi, FondementRattachement fondement,
            List<String> motifs, List<String> conseils, List<String> bases) {
        List<String> messages = new ArrayList<>();
        messages.add("Loi applicable déterminée : loi de « " + loi + " » (fondement : "
                + libelleFondement(fondement) + "). Cette désignation est issue de l'échelle de "
                + "rattachement du règlement applicable — à vérifier par avocat belge avant production.");
        if (in.lieuSituationBiensImmobiliers() != null
                && in.matiere() == Matiere.SUCCESSION) {
            messages.add("Le lieu de situation des biens immobiliers (« " + in.lieuSituationBiensImmobiliers()
                    + " ») est indicatif : le Règl. 650/2012 retient une loi successorale UNIQUE et ne "
                    + "scinde pas selon la situation des immeubles (à vérifier par avocat belge).");
        }
        return new DipBeLoiApplicableFamilleResult(
                DipBeLoiApplicableFamilleVerdict.LOI_APPLICABLE_DETERMINEE,
                loi, fondement, motifs, conseils, bases, messages);
    }

    private static DipBeLoiApplicableFamilleResult indeterminable(
            DipBeLoiApplicableFamilleInput in, String message) {
        List<String> motifs = new ArrayList<>();
        motifs.add("Les éléments de rattachement disponibles ne permettent pas de désigner la loi "
                + "applicable pour la matière « " + in.matiere() + " » (à vérifier par avocat belge).");
        List<String> conseils = new ArrayList<>();
        conseils.add("Compléter les éléments de fait pertinents : choix de loi des parties (professio "
                + "juris), résidence(s) habituelle(s) commune(s), nationalité commune, et — pour une "
                + "succession — la résidence habituelle du défunt au décès.");
        List<String> bases = basesPourMatiere(in.matiere());
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return new DipBeLoiApplicableFamilleResult(
                DipBeLoiApplicableFamilleVerdict.LOI_INDETERMINABLE,
                null, FondementRattachement.AUCUN, motifs, conseils, bases, messages);
    }

    private static String libelleFondement(FondementRattachement f) {
        switch (f) {
            case CHOIX_LOI_PARTIES: return "loi choisie par les parties (professio juris)";
            case RESIDENCE_HABITUELLE_COMMUNE: return "résidence habituelle commune";
            case NATIONALITE_COMMUNE: return "nationalité commune";
            case RESIDENCE_HABITUELLE_DEFUNT: return "résidence habituelle du défunt au décès";
            case LOI_DU_FOR: return "loi de la juridiction saisie (loi du for)";
            case LIEN_LE_PLUS_ETROIT: return "lien le plus étroit";
            default: return "indéterminé";
        }
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(DipBeLoiApplicableFamilleInput in) {
        if (in.matiere() == null) {
            throw new IllegalArgumentException(
                    "La matière est requise (DIVORCE / REGIME_MATRIMONIAL / SUCCESSION)");
        }
        validateIso2("residenceHabituelleCommune", in.residenceHabituelleCommune());
        validateIso2("nationaliteCommune", in.nationaliteCommune());
        validateIso2("choixLoiParLesParties", in.choixLoiParLesParties());
        validateIso2("lieuSituationBiensImmobiliers", in.lieuSituationBiensImmobiliers());
        if (in.dateMariageOuDeces() != null && in.dateMariageOuDeces().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date (mariage / décès) ne peut pas être dans le futur");
        }
    }

    private static void validateIso2(String field, String value) {
        if (value != null && !ISO2.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Le champ " + field + " doit être un code pays ISO 3166-1 alpha-2 (2 lettres majuscules)");
        }
    }

    // ---------------------------------------------------------------
    // Bases juridiques (aucune citation jurisprudentielle — F-JU-04 parké)
    // ---------------------------------------------------------------

    private static List<String> basesPourMatiere(Matiere matiere) {
        if (matiere == null) {
            return basesDivorce();
        }
        switch (matiere) {
            case DIVORCE: return basesDivorce();
            case REGIME_MATRIMONIAL: return basesRegime();
            case SUCCESSION: return basesSuccession();
            default: return basesDivorce();
        }
    }

    private static List<String> basesDivorce() {
        return List.of(
                "Règlement (UE) n° 1259/2010 (Rome III) — loi applicable au divorce et à la séparation de "
                        + "corps : art. 5 (choix des parties) et art. 8 (échelle subsidiaire) — à vérifier "
                        + "par avocat belge",
                "Code de droit international privé belge (loi du 16/07/2004) — règles résiduelles de DIP "
                        + "familial (à vérifier par avocat belge — renumérotation CC post-réformes 2017-2019)",
                "Distinction compétence / loi applicable : la compétence juridictionnelle (Règl. "
                        + "Bruxelles II ter) est hors périmètre de cet outil (à vérifier séparément)");
    }

    private static List<String> basesRegime() {
        return List.of(
                "Règlement (UE) 2016/1103 — loi applicable aux régimes matrimoniaux : art. 22 (choix des "
                        + "époux) et art. 26 (rattachements objectifs) — à vérifier par avocat belge",
                "Code de droit international privé belge (loi du 16/07/2004) — règles résiduelles de DIP "
                        + "patrimonial (à vérifier par avocat belge)",
                "Principe d'immutabilité du rattachement du régime, sauf changement volontaire de loi (à "
                        + "vérifier par avocat belge)");
    }

    private static List<String> basesSuccession() {
        return List.of(
                "Règlement (UE) n° 650/2012 — loi applicable aux successions : art. 21 (résidence "
                        + "habituelle du défunt) et art. 22 (professio juris) — à vérifier par avocat belge",
                "Loi successorale UNIQUE : meubles et immeubles, sans scission selon le lieu de situation "
                        + "des biens (art. 21 § 1) — à vérifier par avocat belge",
                "Code de droit international privé belge (loi du 16/07/2004) — règles résiduelles de DIP "
                        + "successoral (à vérifier par avocat belge — renumérotation CC post-réformes 2017-2019)");
    }
}

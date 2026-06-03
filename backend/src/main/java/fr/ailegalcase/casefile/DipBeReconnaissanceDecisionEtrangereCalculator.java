package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SF-223-08 : moteur décisionnel BE qualifiant la <b>reconnaissance ou
 * l'exequatur en Belgique d'une décision familiale étrangère</b> au regard du
 * Code de droit international privé belge (CDIP — loi du 16/07/2004, art. 22-27)
 * et, pour le cas du mariage religieux non précédé d'un mariage civil, de
 * l'art. 21 de la Constitution / CC art. 161. À VÉRIFIER PAR AVOCAT BELGE.
 *
 * <p><b>Arbre, pas calcul.</b> L'outil distingue deux situations :</p>
 * <ul>
 *   <li><b>JUGEMENT_ETRANGER_HORS_UE</b> (CDIP art. 22-25) : une décision
 *       juridictionnelle étrangère hors UE est reconnue <i>de plein droit</i> si
 *       elle est définitive, que les droits de la défense ont été respectés,
 *       qu'elle n'est pas contraire à l'ordre public belge et qu'il n'y a pas de
 *       fraude. À défaut d'un de ces motifs, la reconnaissance est refusée
 *       (ordre public / défense / fraude) ; si les conditions de fond paraissent
 *       réunies mais qu'une mesure d'exécution est nécessaire (ou qu'un contrôle
 *       juridictionnel reste requis), une procédure d'exequatur est requise.</li>
 *   <li><b>MARIAGE_RELIGIEUX_NON_CIVIL</b> (art. 21 Const. / CC art. 161) : un
 *       mariage religieux non précédé du mariage civil n'a aucun effet civil en
 *       Belgique → reconnaissance refusée (défaut de civil préalable).</li>
 * </ul>
 *
 * <p>Quand les éléments de qualification disponibles ne permettent pas de
 * trancher (champs nullables non documentés pour le jugement), le verdict est
 * {@code QUALIFICATION_INCOMPLETE} : l'avocat doit compléter les éléments de
 * fait.</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la situation cadrée est la
 * <i>reconnaissance / exequatur d'une décision juridictionnelle étrangère déjà
 * rendue (cadre général CDIP art. 22-27) + le mariage religieux non-civil</i>.
 * DISTINCT de la détermination de la loi applicable
 * ({@code dip-be-loi-applicable-famille}, SF-223-07 : quelle loi régit une
 * situation à instruire). DISTINCT de la reconnaissance d'un mariage / divorce
 * valablement célébré à l'étranger ({@code mariage-etranger-be-reconnaissance},
 * F-217) : ici on traite la décision juridictionnelle et le mariage religieux
 * non précédé d'un civil (≠ mariage étranger valable). Aucune citation
 * jurisprudentielle (F-JU-04 parké).</p>
 */
public final class DipBeReconnaissanceDecisionEtrangereCalculator {

    /** Nature de la décision étrangère soumise à reconnaissance. */
    public enum NatureDecision {
        JUGEMENT_ETRANGER_HORS_UE,
        MARIAGE_RELIGIEUX_NON_CIVIL
    }

    /** Verdict de l'analyse. */
    public enum DipBeReconnaissanceVerdict {
        RECONNAISSANCE_DE_PLEIN_DROIT,
        EXEQUATUR_REQUIS,
        RECONNAISSANCE_REFUSEE,
        QUALIFICATION_INCOMPLETE
    }

    private static final Pattern ISO2 = Pattern.compile("^[A-Z]{2}$");

    private DipBeReconnaissanceDecisionEtrangereCalculator() {}

    /**
     * Applique l'arbre de qualification CDIP art. 22-27.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, motifs, conseils, actes à produire,
     *         bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static DipBeReconnaissanceDecisionEtrangereResult compute(
            DipBeReconnaissanceDecisionEtrangereInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — reconnaissance / exequatur d'une décision "
                            + "familiale étrangère (DIP)");
        }
        validateInputs(input);

        switch (input.natureDecision()) {
            case JUGEMENT_ETRANGER_HORS_UE:
                return jugementEtranger(input);
            case MARIAGE_RELIGIEUX_NON_CIVIL:
                return mariageReligieuxNonCivil(input);
            default:
                // Sécurité (théoriquement inatteignable — nature validée plus haut).
                return incomplete(input,
                        "Nature de la décision non reconnue : compléter la nature "
                                + "(JUGEMENT_ETRANGER_HORS_UE / MARIAGE_RELIGIEUX_NON_CIVIL).");
        }
    }

    // ---------------------------------------------------------------
    // JUGEMENT ÉTRANGER HORS UE — CDIP art. 22-25
    // ---------------------------------------------------------------

    private static DipBeReconnaissanceDecisionEtrangereResult jugementEtranger(
            DipBeReconnaissanceDecisionEtrangereInput in) {
        // Pour un jugement, les éléments de qualification doivent être documentés
        // (la mini-spec impose des champs propres au jugement). S'il manque l'un
        // des booleans de fond (définitivité, droits de la défense, fraude), la
        // qualification est incomplète.
        if (in.decisionDefinitive() == null
                || in.droitsDefenseRespectes() == null
                || in.absenceFraude() == null) {
            return incomplete(in,
                    "La qualification d'un jugement étranger hors UE suppose de renseigner son caractère "
                            + "définitif, le respect des droits de la défense et l'absence de fraude (CDIP "
                            + "art. 25 — à vérifier par avocat belge). Compléter ces éléments.");
        }

        List<String> motifs = new ArrayList<>();
        List<String> conseils = jugementConseils(in);
        List<String> bases = basesJugement();
        boolean refus = false;

        // Motif de refus : ordre public belge (CDIP art. 25, § 1, 1°).
        if (!in.conformiteOrdrePublicBelge()) {
            refus = true;
            motifs.add("La décision est jugée contraire à l'ordre public belge : la reconnaissance doit "
                    + "être refusée (CDIP art. 25, § 1, 1° — à vérifier par avocat belge ; appréciation in "
                    + "concreto de l'effet de la décision en Belgique).");
        }
        // Motif de refus : droits de la défense (CDIP art. 25, § 1, 2°).
        if (!in.droitsDefenseRespectes()) {
            refus = true;
            motifs.add("Les droits de la défense n'ont pas été respectés dans la procédure étrangère "
                    + "(défaut régulièrement cité / délai utile) : motif de refus de reconnaissance "
                    + "(CDIP art. 25, § 1, 2° — à vérifier par avocat belge).");
        }
        // Motif de refus : fraude à la loi / fraude au jugement (CDIP art. 25, § 1, 3°).
        if (!in.absenceFraude()) {
            refus = true;
            motifs.add("La décision paraît entachée de fraude (fraude à la loi ou détournement de "
                    + "compétence pour obtenir le jugement) : motif de refus de reconnaissance "
                    + "(CDIP art. 25, § 1, 3° — à vérifier par avocat belge).");
        }
        if (refus) {
            List<String> messages = new ArrayList<>();
            messages.add("Reconnaissance refusée : un ou plusieurs motifs de refus du CDIP art. 25 sont "
                    + "réunis (à vérifier par avocat belge avant toute démarche).");
            return new DipBeReconnaissanceDecisionEtrangereResult(
                    DipBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE,
                    motifs, conseils, actesRefus(), bases, messages);
        }

        // Pas de motif de refus. Si la décision n'est pas définitive, une simple
        // reconnaissance de plein droit n'est pas acquise : un contrôle
        // juridictionnel (exequatur) est requis.
        if (!in.decisionDefinitive()) {
            motifs.add("Aucun motif de refus n'est relevé, mais la décision n'est pas (ou pas encore) "
                    + "définitive : la reconnaissance de plein droit n'est pas acquise — une procédure de "
                    + "déclaration de force exécutoire (exequatur) devant le tribunal de la famille est "
                    + "requise (CDIP art. 22-23 — à vérifier par avocat belge).");
            List<String> messages = new ArrayList<>();
            messages.add("Exequatur requis : saisir le tribunal de la famille d'une requête en déclaration "
                    + "de force exécutoire (CDIP art. 23 — à vérifier par avocat belge).");
            return new DipBeReconnaissanceDecisionEtrangereResult(
                    DipBeReconnaissanceVerdict.EXEQUATUR_REQUIS,
                    motifs, conseils, actesExequatur(), bases, messages);
        }

        // Définitive + pas de motif de refus → reconnaissance de plein droit.
        motifs.add("La décision est définitive, les droits de la défense ont été respectés, elle n'est "
                + "pas contraire à l'ordre public belge et aucune fraude n'est relevée : elle bénéficie de "
                + "la reconnaissance DE PLEIN DROIT en Belgique, sans procédure préalable (CDIP art. 22 — à "
                + "vérifier par avocat belge ; l'exequatur reste nécessaire pour une mesure d'exécution "
                + "forcée).");
        List<String> messages = new ArrayList<>();
        messages.add("Reconnaissance de plein droit : la décision produit ses effets en Belgique sans "
                + "procédure préalable (CDIP art. 22). Une déclaration de force exécutoire (exequatur) "
                + "demeure requise pour toute exécution forcée — à vérifier par avocat belge.");
        return new DipBeReconnaissanceDecisionEtrangereResult(
                DipBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT,
                motifs, conseils, actesPleinDroit(), bases, messages);
    }

    // ---------------------------------------------------------------
    // MARIAGE RELIGIEUX NON PRÉCÉDÉ D'UN CIVIL — art. 21 Const. / CC art. 161
    // ---------------------------------------------------------------

    private static DipBeReconnaissanceDecisionEtrangereResult mariageReligieuxNonCivil(
            DipBeReconnaissanceDecisionEtrangereInput in) {
        List<String> motifs = new ArrayList<>();
        List<String> bases = basesMariageReligieux();
        List<String> conseils = new ArrayList<>();
        conseils.add("Distinguer nettement ce cas de la reconnaissance d'un MARIAGE étranger valablement "
                + "célébré à l'étranger (mécanique CDIP générale, outil "
                + "`mariage-etranger-be-reconnaissance`) : ici le mariage religieux n'a PAS été précédé du "
                + "mariage civil et ne peut donc produire d'effet civil en Belgique (à vérifier par avocat "
                + "belge).");
        conseils.add("Orienter le client vers la célébration d'un mariage civil devant l'officier de "
                + "l'état civil belge s'il souhaite produire des effets civils (à vérifier par avocat belge).");

        // Si l'avocat a explicitement renseigné qu'un mariage civil préalable
        // existe, l'hypothèse de l'outil n'est pas remplie → qualification
        // incomplète (le cas relève alors d'un autre outil / d'une reconnaissance
        // de mariage étranger).
        if (Boolean.TRUE.equals(in.mariageCivilPrealable())) {
            return incomplete(in,
                    "Un mariage civil préalable est renseigné : la situation ne relève pas du défaut de "
                            + "civil préalable mais, le cas échéant, de la reconnaissance d'un mariage "
                            + "valablement célébré à l'étranger (outil `mariage-etranger-be-reconnaissance`, "
                            + "F-217). Vérifier la qualification (à vérifier par avocat belge).");
        }

        motifs.add("Un mariage religieux non précédé du mariage civil n'a AUCUN effet civil en Belgique : "
                + "l'art. 21 de la Constitution impose la priorité du mariage civil sur la célébration "
                + "religieuse et le CC art. 161 (ancien art. 21) en tire les conséquences — la "
                + "reconnaissance est refusée (défaut de civil préalable — à vérifier par avocat belge ; "
                + "renumérotation CC post-réformes 2017-2019).");
        List<String> messages = new ArrayList<>();
        messages.add("Reconnaissance refusée : le mariage religieux non précédé du mariage civil ne "
                + "produit pas d'effet civil en Belgique (art. 21 Constitution / CC art. 161 — à vérifier "
                + "par avocat belge).");
        return new DipBeReconnaissanceDecisionEtrangereResult(
                DipBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE,
                motifs, conseils, actesMariageReligieux(), bases, messages);
    }

    // ---------------------------------------------------------------
    // Conseils / actes à produire
    // ---------------------------------------------------------------

    private static List<String> jugementConseils(DipBeReconnaissanceDecisionEtrangereInput in) {
        List<String> c = new ArrayList<>();
        c.add("Vérifier que la décision relève bien d'un État HORS Union européenne : pour une décision "
                + "rendue dans l'UE, le régime de reconnaissance des règlements (dont Bruxelles II ter) "
                + "s'applique — de plein droit et hors périmètre de cet outil (à vérifier par avocat belge).");
        c.add("Réunir une expédition de la décision, la preuve de son caractère définitif et, le cas "
                + "échéant, la preuve de la régularité de la citation (droits de la défense) — pièces "
                + "exigées par le CDIP art. 24 (à vérifier par avocat belge).");
        return c;
    }

    private static List<String> actesPleinDroit() {
        List<String> a = new ArrayList<>();
        a.add("Légalisation / apostille de la décision étrangère et traduction jurée si nécessaire.");
        a.add("Pour la mise à jour des registres (état civil) : présenter la décision reconnue de plein "
                + "droit à l'officier de l'état civil / à l'autorité concernée (à vérifier par avocat belge).");
        a.add("Pour toute exécution forcée : requête en déclaration de force exécutoire (exequatur) devant "
                + "le tribunal de la famille (CDIP art. 23).");
        return a;
    }

    private static List<String> actesExequatur() {
        List<String> a = new ArrayList<>();
        a.add("Requête en déclaration de force exécutoire (exequatur) devant le tribunal de la famille "
                + "(CDIP art. 23 — à vérifier par avocat belge).");
        a.add("Légalisation / apostille de la décision étrangère et traduction jurée si nécessaire.");
        a.add("Pièces du CDIP art. 24 : expédition de la décision, preuve du caractère exécutoire / "
                + "définitif, preuve de la citation régulière le cas échéant.");
        return a;
    }

    private static List<String> actesRefus() {
        List<String> a = new ArrayList<>();
        a.add("Documenter le ou les motifs de refus (ordre public / droits de la défense / fraude) pour "
                + "anticiper un éventuel litige.");
        a.add("Examiner l'opportunité d'introduire une nouvelle procédure au fond en Belgique plutôt que "
                + "de tenter la reconnaissance (à vérifier par avocat belge).");
        return a;
    }

    private static List<String> actesMariageReligieux() {
        List<String> a = new ArrayList<>();
        a.add("Orienter vers la célébration d'un mariage civil devant l'officier de l'état civil belge "
                + "(condition de tout effet civil).");
        a.add("Vérifier les éventuelles conséquences pénales de la célébration religieuse non précédée du "
                + "civil pour le ministre du culte (à vérifier par avocat belge).");
        return a;
    }

    // ---------------------------------------------------------------
    // Construction du résultat incomplet
    // ---------------------------------------------------------------

    private static DipBeReconnaissanceDecisionEtrangereResult incomplete(
            DipBeReconnaissanceDecisionEtrangereInput in, String message) {
        List<String> motifs = new ArrayList<>();
        motifs.add("Les éléments de qualification disponibles ne permettent pas de trancher la "
                + "reconnaissance pour la nature « " + in.natureDecision() + " » (à vérifier par avocat "
                + "belge).");
        List<String> conseils = new ArrayList<>();
        conseils.add("Compléter les éléments de fait : caractère définitif de la décision, respect des "
                + "droits de la défense, conformité à l'ordre public belge, absence de fraude (et, pour le "
                + "mariage religieux, l'existence ou non d'un mariage civil préalable).");
        List<String> bases = in.natureDecision() == NatureDecision.MARIAGE_RELIGIEUX_NON_CIVIL
                ? basesMariageReligieux() : basesJugement();
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return new DipBeReconnaissanceDecisionEtrangereResult(
                DipBeReconnaissanceVerdict.QUALIFICATION_INCOMPLETE,
                motifs, conseils, List.of(), bases, messages);
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(DipBeReconnaissanceDecisionEtrangereInput in) {
        if (in.natureDecision() == null) {
            throw new IllegalArgumentException(
                    "La nature de la décision est requise (JUGEMENT_ETRANGER_HORS_UE / "
                            + "MARIAGE_RELIGIEUX_NON_CIVIL)");
        }
        if (in.paysOrigine() != null && !ISO2.matcher(in.paysOrigine()).matches()) {
            throw new IllegalArgumentException(
                    "Le champ paysOrigine doit être un code pays ISO 3166-1 alpha-2 (2 lettres majuscules)");
        }
        if (in.dateDecision() != null && in.dateDecision().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de la décision ne peut pas être dans le futur");
        }
    }

    // ---------------------------------------------------------------
    // Bases juridiques (aucune citation jurisprudentielle — F-JU-04 parké)
    // ---------------------------------------------------------------

    private static List<String> basesJugement() {
        return List.of(
                "Code de droit international privé belge (loi du 16/07/2004) — art. 22 (reconnaissance de "
                        + "plein droit), art. 23 (déclaration de force exécutoire / exequatur devant le "
                        + "tribunal de la famille), art. 24 (pièces à produire) et art. 25 (motifs de refus : "
                        + "ordre public, droits de la défense, fraude) — à vérifier par avocat belge",
                "Reconnaissance des décisions rendues dans l'Union européenne (dont Règl. Bruxelles II "
                        + "ter) : régime de plein droit propre, HORS périmètre de cet outil (à vérifier "
                        + "séparément)",
                "Légalisation / apostille (Convention de La Haye du 05/10/1961) et traduction jurée des "
                        + "actes étrangers (à vérifier par avocat belge)");
    }

    private static List<String> basesMariageReligieux() {
        return List.of(
                "Constitution belge, art. 21, al. 2 — le mariage civil devra toujours précéder la "
                        + "bénédiction nuptiale (priorité du civil sur le religieux) — à vérifier par avocat "
                        + "belge",
                "Code civil belge, art. 161 (renumérotation post-réformes 2017-2019) — un mariage "
                        + "religieux non précédé du mariage civil ne produit aucun effet civil en Belgique — "
                        + "à vérifier par avocat belge",
                "Distinction avec la reconnaissance d'un mariage valablement célébré à l'étranger "
                        + "(mécanique CDIP générale) : hors périmètre de cet outil — à vérifier par avocat "
                        + "belge");
    }
}

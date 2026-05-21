package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-217-16 : moteur décisionnel BE pour la reconnaissance d'un mariage ou
 * divorce étranger en Belgique — incluant le talaq (répudiation), les
 * mariages religieux non précédés du civil et les mariages polygames.
 *
 * <p>Arbre décisionnel construit à partir des sources belges : <b>Code de
 * droit international privé (CDIP, loi du 16/07/2004)</b>, articles 21
 * (ordre public), 22+ (exequatur), 25 (conditions de reconnaissance des
 * décisions étrangères), 27 (reconnaissance de plein droit) et 46 (loi
 * applicable aux conditions de fond du mariage). La jurisprudence belge
 * sur le talaq (Cassation et juridictions du fond, post-Moudawana 2004)
 * est synthétisée dans les règles : effectivité du consentement de
 * l'épouse + caractère officiel / écrit / notifié de la décision.</p>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. Outil bâti depuis les sources
 * belges. Le DIP français (CC + Règlements UE) est structurellement
 * distinct et ne se prête pas à la réutilisation
 * ({@code feedback_belgique_never_forget}).</p>
 *
 * <p><b>Validation juridique requise</b> : les articles cités (CDIP
 * art. 21 / 22 / 25 / 27 / 46), les conventions bilatérales (Belgique-Maroc,
 * Belgique-Algérie, Belgique-Turquie), le Règlement Bruxelles II bis pour
 * la reconnaissance des divorces UE sont à <b>valider par un avocat belge
 * avant mise en production</b>. Articles tagués (à vérifier) cohérent avec
 * l'audit F-191.</p>
 */
public final class MariageEtrangerBeReconnaissanceCalculator {

    /** Verdict de l'analyse de reconnaissance. */
    public enum MariageEtrangerBeReconnaissanceVerdict {
        RECONNAISSANCE_DE_PLEIN_DROIT,
        RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS,
        RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC,
        EXEQUATUR_REQUIS,
        QUALIFICATION_INCOMPLETE
    }

    /** Nature de l'acte étranger à reconnaître. */
    public enum NatureActeEtrangerBe {
        MARIAGE_CIVIL_ETRANGER,
        MARIAGE_RELIGIEUX_NON_CIVIL,
        MARIAGE_POLYGAME,
        DIVORCE_JUDICIAIRE_ETRANGER,
        TALAQ_REPUDIATION
    }

    /** Localisation de la résidence habituelle d'au moins une des parties. */
    public enum ResidenceHabituelleBe {
        BELGIQUE,
        ETRANGER,
        INCONNU
    }

    /** Nationalité d'au moins une des parties au moment de l'acte. */
    public enum NationalitePartiesBe {
        BELGIQUE,
        UE,
        HORS_UE,
        INCONNU
    }

    /** Code structuré d'un motif (refus ou réserve). */
    public enum MotifReconnaissanceEtrangerBeCode {
        POLYGAMIE_ORDRE_PUBLIC,
        MARIAGE_RELIGIEUX_NON_CIVIL,
        TALAQ_CONSENTEMENT_EPOUSE_ABSENT,
        TALAQ_PROCEDURE_NON_CONTRADICTOIRE,
        TALAQ_EPOUSE_NON_PRESENTE_NI_REPRESENTEE,
        FRAUDE_LOI_RECONNAISSABLE,
        MARIAGE_FORCE_DETECTE,
        MARIAGE_ENFANT,
        FOND_DROIT_PERSONNEL_NON_CONFORME,
        FORME_LOCUS_REGIT_ACTUM_NON_CONFORME,
        DECISION_NON_OFFICIELLE
    }

    /** Sévérité d'un motif. */
    public enum SeveriteMotifBe {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Motif structuré — exposé dans la réponse. */
    public record MotifReconnaissanceEtrangerBe(
            MotifReconnaissanceEtrangerBeCode code,
            String libelle,
            String fondement,
            SeveriteMotifBe severite
    ) {}

    // ---------------------------------------------------------------
    // Constantes documentées (à vérifier — validation avocat belge)
    // ---------------------------------------------------------------

    /** Longueur maximale du commentaire libre. */
    private static final int COMMENTAIRE_MAX = 1000;

    /**
     * Liste statique des pays membres de l'Union européenne (ISO 3166-1
     * alpha-2). Utilisée pour distinguer le régime UE (Règlement
     * Bruxelles II bis — reconnaissance de plein droit) du régime hors UE
     * (CDIP — exequatur requis). À mettre à jour en cas d'entrée / sortie UE
     * (paramètre documenté — à vérifier).
     */
    private static final Set<String> PAYS_UE = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );

    private MariageEtrangerBeReconnaissanceCalculator() {}

    /**
     * Applique l'arbre décisionnel BE de reconnaissance d'un mariage / divorce
     * étranger sur les éléments saisis.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, motifs, actes à produire,
     *         bases juridiques, messages d'aide)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static MariageEtrangerBeReconnaissanceResult compute(
            MariageEtrangerBeReconnaissanceInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — reconnaissance mariage / divorce étranger");
        }
        validateInputs(input);

        // Branches par nature de l'acte étranger (mini-spec règles 2 → 6).
        return switch (input.natureActe()) {
            case MARIAGE_POLYGAME -> resultPolygame(input, countryNormalized);
            case MARIAGE_RELIGIEUX_NON_CIVIL -> resultMariageReligieuxNonCivil(input, countryNormalized);
            case TALAQ_REPUDIATION -> resultTalaq(input, countryNormalized);
            case DIVORCE_JUDICIAIRE_ETRANGER -> resultDivorceJudiciaire(input, countryNormalized);
            case MARIAGE_CIVIL_ETRANGER -> resultMariageCivilEtranger(input, countryNormalized);
        };
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(MariageEtrangerBeReconnaissanceInput in) {
        if (in.natureActe() == null) {
            throw new IllegalArgumentException("La nature de l'acte étranger est requise");
        }
        if (in.paysOrigine() == null || in.paysOrigine().isBlank()) {
            throw new IllegalArgumentException("Le pays d'origine de l'acte est requis (ISO 3166-1 alpha-2)");
        }
        if (!in.paysOrigine().matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException(
                    "Le pays d'origine doit être un code ISO 3166-1 alpha-2 (2 lettres majuscules)");
        }
        if (in.dateActe() == null) {
            throw new IllegalArgumentException("La date de l'acte étranger est requise");
        }
        if (in.dateActe().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de l'acte ne peut pas être dans le futur");
        }
        if (in.residenceHabituelleAuMoinsUnePartie() == null) {
            throw new IllegalArgumentException(
                    "La résidence habituelle d'au moins une des parties est requise");
        }
        if (in.nationaliteAuMoinsUnePartie() == null) {
            throw new IllegalArgumentException(
                    "La nationalité d'au moins une des parties est requise");
        }
        if (in.conformiteDroitFondPersonnel() == null) {
            throw new IllegalArgumentException(
                    "La conformité au droit personnel de fond est requise");
        }
        if (in.conformiteFormeLocusRegitActum() == null) {
            throw new IllegalArgumentException(
                    "La conformité aux formes du lieu de célébration est requise");
        }
        if (in.conventionBilateraleApplicable() == null) {
            throw new IllegalArgumentException(
                    "L'applicabilité d'une convention bilatérale est requise");
        }
        if (in.natureActe() == NatureActeEtrangerBe.TALAQ_REPUDIATION) {
            if (in.consentementEpouse() == null) {
                throw new IllegalArgumentException(
                        "Le consentement de l'épouse est requis pour un talaq");
            }
            if (in.epousePresente() == null) {
                throw new IllegalArgumentException(
                        "La présence de l'épouse est requise pour un talaq");
            }
            if (in.procedureContradictoire() == null) {
                throw new IllegalArgumentException(
                        "Le caractère contradictoire de la procédure est requis pour un talaq");
            }
            if (in.decisionEcriteOfficielle() == null) {
                throw new IllegalArgumentException(
                        "Le caractère officiel et écrit de la décision est requis pour un talaq");
            }
        }
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException(
                    "Le commentaire ne peut dépasser " + COMMENTAIRE_MAX + " caractères");
        }
    }

    // ---------------------------------------------------------------
    // Branches par nature d'acte
    // ---------------------------------------------------------------

    /** Polygamie civile → refus absolu d'ordre public (CDIP art. 21 — à vérifier). */
    private static MariageEtrangerBeReconnaissanceResult resultPolygame(
            MariageEtrangerBeReconnaissanceInput in, String country) {
        List<MotifReconnaissanceEtrangerBe> refus = List.of(
                new MotifReconnaissanceEtrangerBe(
                        MotifReconnaissanceEtrangerBeCode.POLYGAMIE_ORDRE_PUBLIC,
                        "Mariage polygame — atteinte manifeste à l'ordre public belge (refus civil absolu).",
                        "CDIP art. 21 (à vérifier) — ordre public international belge ; "
                                + "monogamie d'ordre public civil en BE",
                        SeveriteMotifBe.HIGH));
        List<String> actes = List.of(
                "Notifier au client le refus civil de reconnaissance et expliquer les effets résiduels "
                        + "possibles (succession ab intestat, pension de réversion — à vérifier au cas par cas).",
                "Examiner si la première union (la seule reconnaissable civilement) peut être transcrite.");
        List<String> messages = new ArrayList<>();
        messages.add("La polygamie civile n'est pas reconnue en droit belge (ordre public). Le refus civil "
                + "est constant, mais certains effets résiduels peuvent subsister en pratique : droits "
                + "successoraux ab intestat, pension de réversion, droits sociaux — à examiner cas par "
                + "cas (à vérifier).");
        addConventionBilateraleMessage(in, messages);
        return new MariageEtrangerBeReconnaissanceResult(
                MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC,
                refus, List.of(), actes, basesJuridiques(), messages, country);
    }

    /** Mariage religieux non précédé du mariage civil → refus civil. */
    private static MariageEtrangerBeReconnaissanceResult resultMariageReligieuxNonCivil(
            MariageEtrangerBeReconnaissanceInput in, String country) {
        List<MotifReconnaissanceEtrangerBe> refus = List.of(
                new MotifReconnaissanceEtrangerBe(
                        MotifReconnaissanceEtrangerBeCode.MARIAGE_RELIGIEUX_NON_CIVIL,
                        "Mariage religieux non précédé du mariage civil — sans effet civil en Belgique.",
                        "Constitution belge art. 21 (à vérifier) — le mariage civil doit "
                                + "toujours précéder la bénédiction nuptiale",
                        SeveriteMotifBe.HIGH));
        List<String> actes = List.of(
                "Informer le client qu'un mariage religieux non précédé du civil n'a pas d'effet civil en BE.",
                "Examiner s'il est possible de régulariser par un mariage civil belge (à vérifier — "
                        + "consultation officier état civil).");
        List<String> messages = new ArrayList<>();
        messages.add("La Constitution belge (art. 21 — à vérifier) impose que le mariage civil précède "
                + "toute bénédiction religieuse. Un mariage religieux étranger non précédé du civil n'a "
                + "pas d'effet civil en Belgique.");
        addConventionBilateraleMessage(in, messages);
        return new MariageEtrangerBeReconnaissanceResult(
                MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC,
                refus, List.of(), actes, basesJuridiques(), messages, country);
    }

    /** Talaq — analyse fine consentement / procédure / forme. */
    private static MariageEtrangerBeReconnaissanceResult resultTalaq(
            MariageEtrangerBeReconnaissanceInput in, String country) {
        List<MotifReconnaissanceEtrangerBe> refus = new ArrayList<>();
        List<MotifReconnaissanceEtrangerBe> reserves = new ArrayList<>();

        // Consentement de l'épouse absent → refus d'ordre public.
        if (Boolean.FALSE.equals(in.consentementEpouse())) {
            refus.add(new MotifReconnaissanceEtrangerBe(
                    MotifReconnaissanceEtrangerBeCode.TALAQ_CONSENTEMENT_EPOUSE_ABSENT,
                    "Talaq prononcé sans consentement effectif de l'épouse — atteinte à l'ordre public "
                            + "belge (égalité des époux).",
                    "CDIP art. 21 / art. 25 (à vérifier) ; jurisprudence belge sur le talaq post-Moudawana",
                    SeveriteMotifBe.HIGH));
        }

        // Épouse non présente ni représentée → réserve HIGH.
        if (Boolean.FALSE.equals(in.epousePresente())) {
            reserves.add(new MotifReconnaissanceEtrangerBe(
                    MotifReconnaissanceEtrangerBeCode.TALAQ_EPOUSE_NON_PRESENTE_NI_REPRESENTEE,
                    "Épouse ni présente ni représentée lors de la procédure de talaq — point sensible "
                            + "pour la reconnaissance.",
                    "CDIP art. 25 (à vérifier) — exigences procédurales pour la reconnaissance des "
                            + "décisions étrangères",
                    SeveriteMotifBe.HIGH));
        }

        // Procédure non contradictoire → réserve HIGH.
        if (Boolean.FALSE.equals(in.procedureContradictoire())) {
            reserves.add(new MotifReconnaissanceEtrangerBe(
                    MotifReconnaissanceEtrangerBeCode.TALAQ_PROCEDURE_NON_CONTRADICTOIRE,
                    "Talaq prononcé sans procédure contradictoire — point sensible jurisprudence "
                            + "belge sur ordre public.",
                    "CDIP art. 25 (à vérifier) — exigences procédurales pour la reconnaissance des "
                            + "décisions étrangères",
                    SeveriteMotifBe.HIGH));
        }

        // Décision non officielle (pas écrite ou pas officielle) → réserve HIGH.
        if (Boolean.FALSE.equals(in.decisionEcriteOfficielle())) {
            reserves.add(new MotifReconnaissanceEtrangerBe(
                    MotifReconnaissanceEtrangerBeCode.DECISION_NON_OFFICIELLE,
                    "Talaq non formalisé par une décision écrite et officielle — la reconnaissance "
                            + "exige un acte authentique ou décision judiciaire.",
                    "CDIP art. 25 / 27 (à vérifier) — caractère officiel de la décision étrangère",
                    SeveriteMotifBe.HIGH));
        }

        MariageEtrangerBeReconnaissanceVerdict verdict;
        if (!refus.isEmpty()) {
            verdict = MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC;
        } else {
            // Pas de refus dur → reconnaissance possible sous conditions (verdict prudent).
            verdict = MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS;
        }

        List<String> actes = new ArrayList<>();
        if (verdict == MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS) {
            actes.add("Demande de reconnaissance auprès de l'officier de l'état civil compétent — à vérifier.");
            actes.add("Production de l'acte étranger légalisé / apostillé + traduction jurée.");
            actes.add("Préparation d'une argumentation sur l'effectivité du consentement de l'épouse "
                    + "(jurisprudence Cassation).");
        } else {
            actes.add("Notifier au client le refus civil de reconnaissance du talaq et expliquer la "
                    + "voie de la procédure de divorce civil belge.");
            actes.add("Examiner les effets résiduels possibles (état des personnes, succession — à vérifier).");
        }

        List<String> messages = new ArrayList<>();
        messages.add("Talaq" + paysOrigineSuffix(in)
                + " — la jurisprudence belge accepte la reconnaissance lorsque le consentement de "
                + "l'épouse est effectif et la procédure officielle (post-Moudawana 2004 marocaine "
                + "notamment). Les réserves procédurales sont des facteurs de risque, pas nécessairement "
                + "des refus.");
        addConventionBilateraleMessage(in, messages);

        return new MariageEtrangerBeReconnaissanceResult(
                verdict, List.copyOf(refus), List.copyOf(reserves),
                actes, basesJuridiques(), messages, country);
    }

    /** Divorce judiciaire étranger — UE = plein droit, hors UE = exequatur requis. */
    private static MariageEtrangerBeReconnaissanceResult resultDivorceJudiciaire(
            MariageEtrangerBeReconnaissanceInput in, String country) {
        boolean ue = PAYS_UE.contains(in.paysOrigine());
        MariageEtrangerBeReconnaissanceVerdict verdict = ue
                ? MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT
                : MariageEtrangerBeReconnaissanceVerdict.EXEQUATUR_REQUIS;

        List<String> actes = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        if (ue) {
            actes.add("Demande de transcription du jugement de divorce auprès de l'officier de l'état "
                    + "civil compétent (Règlement Bruxelles II bis — à vérifier).");
            actes.add("Production du certificat Bruxelles II bis (formulaire annexé) délivré par "
                    + "l'autorité d'origine.");
            messages.add("Divorce judiciaire prononcé dans un État membre de l'UE — reconnaissance de "
                    + "plein droit en vertu du Règlement Bruxelles II bis (à vérifier).");
        } else {
            actes.add("Saisine du Tribunal de la famille (TF) en exequatur — CDIP art. 22+ (à vérifier).");
            actes.add("Production du jugement étranger légalisé / apostillé + traduction jurée.");
            actes.add("Démonstration du respect des conditions CDIP art. 25 (à vérifier) — compétence, "
                    + "ordre public, droits de la défense, etc.");
            messages.add("Divorce judiciaire hors UE — procédure d'exequatur requise devant le Tribunal "
                    + "de la famille (CDIP art. 22+ — à vérifier).");
        }
        addConventionBilateraleMessage(in, messages);

        return new MariageEtrangerBeReconnaissanceResult(
                verdict, List.of(), List.of(), actes, basesJuridiques(), messages, country);
    }

    /** Mariage civil étranger — fond + forme + ordre public. */
    private static MariageEtrangerBeReconnaissanceResult resultMariageCivilEtranger(
            MariageEtrangerBeReconnaissanceInput in, String country) {
        // Fond KO → refus d'ordre public.
        if (Boolean.FALSE.equals(in.conformiteDroitFondPersonnel())) {
            List<MotifReconnaissanceEtrangerBe> refus = List.of(
                    new MotifReconnaissanceEtrangerBe(
                            MotifReconnaissanceEtrangerBeCode.FOND_DROIT_PERSONNEL_NON_CONFORME,
                            "Conditions de fond du mariage non conformes à la loi personnelle des parties.",
                            "CDIP art. 46 (à vérifier) — loi applicable aux conditions de fond du mariage",
                            SeveriteMotifBe.HIGH));
            List<String> actes = List.of(
                    "Notifier au client le refus civil de reconnaissance et expliquer les voies "
                            + "alternatives (mariage civil belge, examen au cas par cas).");
            List<String> messages = new ArrayList<>();
            messages.add("Conditions de fond du mariage non conformes à la loi nationale au moment du "
                    + "mariage (CDIP art. 46 — à vérifier) — la reconnaissance civile est refusée.");
            addConventionBilateraleMessage(in, messages);
            return new MariageEtrangerBeReconnaissanceResult(
                    MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC,
                    refus, List.of(), actes, basesJuridiques(), messages, country);
        }

        // Fond OK + forme KO → réserve MEDIUM (mais reconnaissance encore possible).
        List<MotifReconnaissanceEtrangerBe> reserves = new ArrayList<>();
        if (Boolean.FALSE.equals(in.conformiteFormeLocusRegitActum())) {
            reserves.add(new MotifReconnaissanceEtrangerBe(
                    MotifReconnaissanceEtrangerBeCode.FORME_LOCUS_REGIT_ACTUM_NON_CONFORME,
                    "Formes non conformes au droit du lieu de célébration (locus regit actum) — "
                            + "réserve sur la régularité formelle.",
                    "CDIP art. 27 (à vérifier) — locus regit actum / reconnaissance de plein droit",
                    SeveriteMotifBe.MEDIUM));
        }

        // Fond + forme OK → reconnaissance de plein droit.
        MariageEtrangerBeReconnaissanceVerdict verdict = reserves.isEmpty()
                ? MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT
                : MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS;

        List<String> actes = new ArrayList<>();
        actes.add("Demande de transcription de l'acte de mariage étranger auprès de l'officier de "
                + "l'état civil compétent (CDIP art. 27 — à vérifier).");
        actes.add("Production de l'acte étranger légalisé / apostillé + traduction jurée.");

        List<String> messages = new ArrayList<>();
        if (verdict == MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT) {
            messages.add("Mariage civil étranger conforme au fond (CDIP art. 46) et à la forme (locus "
                    + "regit actum) — reconnaissance de plein droit (CDIP art. 27 — à vérifier).");
        } else {
            messages.add("Mariage civil étranger conforme au fond mais réserve sur la forme — la "
                    + "reconnaissance est possible mais la réserve formelle doit être documentée.");
        }
        addConventionBilateraleMessage(in, messages);

        return new MariageEtrangerBeReconnaissanceResult(
                verdict, List.of(), List.copyOf(reserves),
                actes, basesJuridiques(), messages, country);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static void addConventionBilateraleMessage(MariageEtrangerBeReconnaissanceInput in,
                                                       List<String> messages) {
        if (Boolean.TRUE.equals(in.conventionBilateraleApplicable())) {
            messages.add("Une convention bilatérale Belgique-" + in.paysOrigine()
                    + " peut s'appliquer et faciliter / encadrer la reconnaissance (BE-Maroc 1995, "
                    + "BE-Algérie 1991, BE-Turquie 1958 — dates indicatives à vérifier). L'avocat doit "
                    + "consulter le texte précis et invoquer ses dispositions.");
        }
    }

    private static String paysOrigineSuffix(MariageEtrangerBeReconnaissanceInput in) {
        return in.paysOrigine() != null ? " (pays d'origine " + in.paysOrigine() + ")" : "";
    }

    private static List<String> basesJuridiques() {
        return List.of(
                "CDIP (loi du 16/07/2004) art. 21+ (à vérifier) — reconnaissance des actes étrangers",
                "CDIP art. 22+ (à vérifier) — exequatur des décisions étrangères devant le TF",
                "CDIP art. 25 (à vérifier) — conditions de reconnaissance des décisions étrangères",
                "CDIP art. 27 (à vérifier) — reconnaissance de plein droit / refus pour contrariété à l'ordre public",
                "CDIP art. 46 (à vérifier) — loi applicable aux conditions de fond du mariage",
                "Constitution belge art. 21 (à vérifier) — primauté du mariage civil sur la bénédiction religieuse",
                "Règlement Bruxelles II bis (à vérifier) — reconnaissance de plein droit des divorces UE"
        );
    }
}

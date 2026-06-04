package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-09 : moteur décisionnel BE qualifiant une <b>modification de l'état
 * civil</b> en Belgique. À VÉRIFIER PAR AVOCAT BELGE.
 *
 * <p><b>Arbre, pas calcul.</b> L'outil distingue les trois procédures, qui
 * relèvent d'autorités et de régimes distincts :</p>
 * <ul>
 *   <li><b>CHANGEMENT_PRENOM</b> (loi du 18/06/2018) : compétence de l'<i>officier
 *       de l'état civil</i> de la commune ; conditions allégées (le prénom ne
 *       doit pas prêter à confusion ni nuire au demandeur ou aux tiers) ; la 1re
 *       demande bénéficie d'un tarif réduit / d'une gratuité dans certains cas.</li>
 *   <li><b>CHANGEMENT_NOM</b> (procédure réformée — loi du 18/06/2018 ; CC) :
 *       compétence du <i>SPF Justice</i> (ministre de la Justice) ; un motif
 *       sérieux est requis et le nom sollicité ne doit pas prêter à confusion ni
 *       porter atteinte aux tiers.</li>
 *   <li><b>CHANGEMENT_SEXE</b> (loi du 25/06/2017) : <i>auto-déclaration
 *       administrative</i> devant l'officier de l'état civil ; pour la personne
 *       majeure, la modification suppose un délai de réflexion suivi d'une
 *       <i>seconde déclaration confirmative</i> ; le mineur relève d'un régime
 *       spécifique (assistance / autorisation).</li>
 * </ul>
 *
 * <p>Verdict 4 niveaux : {@code MODIFICATION_RECEVABLE} (conditions réunies) /
 * {@code MODIFICATION_RECEVABLE_SOUS_CONDITIONS} (recevable moyennant une
 * démarche complémentaire — seconde déclaration de sexe, consentement des
 * représentants d'un mineur…) / {@code MODIFICATION_IRRECEVABLE} (condition
 * dirimante non remplie : ni Belge ni résident, motif illégitime…) /
 * {@code QUALIFICATION_INCOMPLETE} (éléments de fait manquants).</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la situation cadrée est la
 * <i>modification</i> de l'état civil (nom / prénom / sexe). DISTINCT de la
 * rectification d'état civil (erreur matérielle d'acte — P4 différé F-224) et du
 * changement d'état civil FR (sous-objet {@code changement_etat_civil_detection}
 * FR-only). Aucune citation jurisprudentielle (F-JU-04 parké).</p>
 */
public final class EtatCivilBeModificationCalculator {

    /** Type de modification de l'état civil sollicitée. */
    public enum TypeModification {
        CHANGEMENT_NOM,
        CHANGEMENT_PRENOM,
        CHANGEMENT_SEXE
    }

    /** Verdict de l'analyse. */
    public enum EtatCivilBeModificationVerdict {
        MODIFICATION_RECEVABLE,
        MODIFICATION_RECEVABLE_SOUS_CONDITIONS,
        MODIFICATION_IRRECEVABLE,
        QUALIFICATION_INCOMPLETE
    }

    private static final String AUTORITE_OFFICIER = "Officier de l'état civil de la commune";
    private static final String AUTORITE_SPF_JUSTICE = "SPF Justice (ministre de la Justice)";

    private EtatCivilBeModificationCalculator() {}

    /**
     * Applique l'arbre de qualification de la modification de l'état civil BE.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, autorité compétente, motifs,
     *         conseils, démarches, bases juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static EtatCivilBeModificationResult compute(
            EtatCivilBeModificationInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — modification de l'état civil "
                            + "(changement de nom / prénom / sexe)");
        }
        if (input.typeModification() == null) {
            throw new IllegalArgumentException(
                    "Le type de modification est requis (CHANGEMENT_NOM / CHANGEMENT_PRENOM / "
                            + "CHANGEMENT_SEXE)");
        }

        switch (input.typeModification()) {
            case CHANGEMENT_PRENOM:
                return changementPrenom(input);
            case CHANGEMENT_NOM:
                return changementNom(input);
            case CHANGEMENT_SEXE:
                return changementSexe(input);
            default:
                // Sécurité (théoriquement inatteignable — type validé plus haut).
                return incomplete(input,
                        "Type de modification non reconnu : compléter le type (CHANGEMENT_NOM / "
                                + "CHANGEMENT_PRENOM / CHANGEMENT_SEXE).");
        }
    }

    // ---------------------------------------------------------------
    // CHANGEMENT DE PRÉNOM — loi du 18/06/2018, officier de l'état civil
    // ---------------------------------------------------------------

    private static EtatCivilBeModificationResult changementPrenom(EtatCivilBeModificationInput in) {
        List<String> motifs = new ArrayList<>();
        List<String> conseils = prenomConseils(in);
        List<String> bases = basesPrenom();

        // Condition dirimante : être Belge ou résident inscrit (compétence de la
        // commune de résidence).
        if (!in.nationaliteBelgeOuResident()) {
            return irrecevable(AUTORITE_OFFICIER, motifs, conseils, bases,
                    "Le changement de prénom devant l'officier de l'état civil suppose la nationalité "
                            + "belge OU une inscription aux registres de la population / des étrangers : la "
                            + "condition de rattachement n'est pas remplie (à vérifier par avocat belge).",
                    "Modification irrecevable : ni Belge ni résident inscrit (loi du 18/06/2018 — à "
                            + "vérifier par avocat belge).");
        }

        // Mineur : l'officier statue avec l'intervention / le consentement des
        // représentants légaux.
        if (!in.personneMajeure()) {
            if (!Boolean.TRUE.equals(in.consentementRepresentantsSiMineur())) {
                motifs.add("Demande formée pour un mineur : la modification du prénom suppose le "
                        + "consentement / l'intervention des représentants légaux (et, le cas échéant, "
                        + "l'audition du mineur) — la démarche est recevable sous cette condition (à "
                        + "vérifier par avocat belge).");
                return sousConditions(AUTORITE_OFFICIER, motifs, conseils, demarchesPrenom(), bases,
                        "Recevable sous condition : recueillir le consentement des représentants légaux du "
                                + "mineur avant le dépôt de la demande (à vérifier par avocat belge).");
            }
            motifs.add("Demande formée pour un mineur avec le consentement des représentants légaux : la "
                    + "demande de changement de prénom peut être instruite par l'officier de l'état civil "
                    + "(à vérifier par avocat belge).");
        }

        motifs.add("Le changement de prénom relève de l'officier de l'état civil de la commune (loi du "
                + "18/06/2018) : la demande est recevable dès lors que le prénom sollicité ne prête pas à "
                + "confusion et ne nuit ni au demandeur ni aux tiers (appréciation de l'officier — à "
                + "vérifier par avocat belge).");
        if (Boolean.FALSE.equals(in.secondeDemandePrenom())) {
            motifs.add("Première demande de changement de prénom : la redevance communale est réduite (et "
                    + "peut être gratuite dans certains cas, p. ex. transidentité) — à vérifier auprès de la "
                    + "commune (à vérifier par avocat belge).");
        } else if (Boolean.TRUE.equals(in.secondeDemandePrenom())) {
            motifs.add("Demande ultérieure (au-delà de la première) : la redevance communale de droit "
                    + "commun s'applique — à vérifier auprès de la commune (à vérifier par avocat belge).");
        }
        return recevable(AUTORITE_OFFICIER, motifs, conseils, demarchesPrenom(), bases,
                "Modification recevable : déposer la demande de changement de prénom auprès de l'officier "
                        + "de l'état civil de la commune (loi du 18/06/2018 — à vérifier par avocat belge).");
    }

    // ---------------------------------------------------------------
    // CHANGEMENT DE NOM — SPF Justice, motif sérieux requis
    // ---------------------------------------------------------------

    private static EtatCivilBeModificationResult changementNom(EtatCivilBeModificationInput in) {
        List<String> motifs = new ArrayList<>();
        List<String> conseils = nomConseils(in);
        List<String> bases = basesNom();

        if (!in.nationaliteBelgeOuResident()) {
            return irrecevable(AUTORITE_SPF_JUSTICE, motifs, conseils, bases,
                    "Le changement de nom auprès du SPF Justice suppose la nationalité belge OU un statut "
                            + "assimilé (apatride / réfugié reconnu, selon les cas) : la condition de "
                            + "rattachement n'est pas remplie (à vérifier par avocat belge).",
                    "Modification irrecevable : condition de nationalité / rattachement non remplie pour le "
                            + "changement de nom (à vérifier par avocat belge).");
        }

        // Motif : élément de fond. S'il n'est pas renseigné, on ne peut pas
        // trancher la recevabilité au fond → qualification incomplète.
        if (in.motifLegitime() == null) {
            return incomplete(in,
                    "Le changement de nom suppose un motif sérieux et l'absence de confusion / d'atteinte "
                            + "aux tiers : ces éléments ne sont pas renseignés. Compléter le motif (à "
                            + "vérifier par avocat belge).");
        }
        if (!in.motifLegitime()) {
            return irrecevable(AUTORITE_SPF_JUSTICE, motifs, conseils, bases,
                    "Le changement de nom suppose un motif sérieux (et l'absence de confusion ou d'atteinte "
                            + "aux tiers) : le motif invoqué ne paraît pas remplir cette exigence — risque de "
                            + "refus du SPF Justice (à vérifier par avocat belge).",
                    "Modification irrecevable en l'état : consolider un motif sérieux avant toute requête au "
                            + "SPF Justice (à vérifier par avocat belge).");
        }

        if (!in.personneMajeure() && !Boolean.TRUE.equals(in.consentementRepresentantsSiMineur())) {
            motifs.add("Demande formée pour un mineur : la requête en changement de nom suppose le "
                    + "consentement / l'intervention des représentants légaux — recevable sous cette "
                    + "condition (à vérifier par avocat belge).");
            return sousConditions(AUTORITE_SPF_JUSTICE, motifs, conseils, demarchesNom(), bases,
                    "Recevable sous condition : recueillir le consentement des représentants légaux du "
                            + "mineur avant la requête au SPF Justice (à vérifier par avocat belge).");
        }

        motifs.add("Le changement de nom relève du SPF Justice (ministre de la Justice) : un motif sérieux "
                + "est invoqué et le nom sollicité est présumé ne pas prêter à confusion ni porter atteinte "
                + "aux tiers — la requête est recevable, sous réserve de l'appréciation du SPF Justice (à "
                + "vérifier par avocat belge).");
        return recevable(AUTORITE_SPF_JUSTICE, motifs, conseils, demarchesNom(), bases,
                "Modification recevable : introduire la requête en changement de nom auprès du SPF Justice "
                        + "(à vérifier par avocat belge).");
    }

    // ---------------------------------------------------------------
    // CHANGEMENT DE SEXE — loi du 25/06/2017, auto-déclaration
    // ---------------------------------------------------------------

    private static EtatCivilBeModificationResult changementSexe(EtatCivilBeModificationInput in) {
        List<String> motifs = new ArrayList<>();
        List<String> conseils = sexeConseils(in);
        List<String> bases = basesSexe();

        if (!in.nationaliteBelgeOuResident()) {
            return irrecevable(AUTORITE_OFFICIER, motifs, conseils, bases,
                    "L'auto-déclaration de changement de sexe (loi du 25/06/2017) suppose la nationalité "
                            + "belge OU une inscription aux registres de la population / des étrangers : la "
                            + "condition de rattachement n'est pas remplie (à vérifier par avocat belge).",
                    "Modification irrecevable : ni Belge ni résident inscrit pour l'auto-déclaration de "
                            + "changement de sexe (à vérifier par avocat belge).");
        }

        // Mineur : régime spécifique (assistance, capacité de discernement,
        // selon l'âge) → recevable sous conditions.
        if (!in.personneMajeure()) {
            motifs.add("Demande formée pour un mineur : le changement de sexe relève d'un régime "
                    + "spécifique (capacité de discernement / assistance des représentants légaux selon "
                    + "l'âge — loi du 25/06/2017) ; la démarche est recevable sous cette condition (à "
                    + "vérifier par avocat belge).");
            return sousConditions(AUTORITE_OFFICIER, motifs, conseils, demarchesSexe(), bases,
                    "Recevable sous condition : appliquer le régime spécifique du mineur (assistance / "
                            + "discernement) avant la déclaration (à vérifier par avocat belge).");
        }

        // Majeur : procédure d'auto-déclaration en deux temps. La 2e déclaration
        // confirmative, après le délai de réflexion, conditionne l'effet.
        motifs.add("Le changement de sexe relève d'une AUTO-DÉCLARATION administrative devant l'officier "
                + "de l'état civil (loi du 25/06/2017) : il n'est subordonné à aucune condition médicale "
                + "(traitement / stérilisation) — spécificité belge (à vérifier par avocat belge).");
        motifs.add("La procédure se déroule en DEUX temps : une première déclaration, puis — après un "
                + "délai de réflexion (≥ 3 mois et ≤ 6 mois) — une SECONDE déclaration confirmative devant "
                + "l'officier de l'état civil, qui acte la modification (à vérifier par avocat belge).");

        if (!Boolean.TRUE.equals(in.declarationSexeReiteree())) {
            motifs.add("La seconde déclaration confirmative n'a pas (encore) été réitérée après le délai "
                    + "de réflexion : la modification n'est pas acquise tant que cette seconde déclaration "
                    + "n'est pas faite — recevable sous cette condition (à vérifier par avocat belge).");
            return sousConditions(AUTORITE_OFFICIER, motifs, conseils, demarchesSexe(), bases,
                    "Recevable sous condition : réitérer la déclaration (seconde déclaration confirmative) "
                            + "après le délai de réflexion pour que la modification soit actée (à vérifier "
                            + "par avocat belge).");
        }

        motifs.add("La seconde déclaration confirmative a été réitérée après le délai de réflexion : "
                + "l'officier de l'état civil acte la modification du sexe (et, le cas échéant, du / des "
                + "prénoms) à l'état civil (à vérifier par avocat belge).");
        return recevable(AUTORITE_OFFICIER, motifs, conseils, demarchesSexe(), bases,
                "Modification recevable : la seconde déclaration confirmative étant faite, l'officier de "
                        + "l'état civil acte le changement de sexe (loi du 25/06/2017 — à vérifier par avocat "
                        + "belge).");
    }

    // ---------------------------------------------------------------
    // Construction des résultats
    // ---------------------------------------------------------------

    private static EtatCivilBeModificationResult recevable(
            String autorite, List<String> motifs, List<String> conseils,
            List<String> demarches, List<String> bases, String message) {
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return new EtatCivilBeModificationResult(
                EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE,
                autorite, motifs, conseils, demarches, bases, messages);
    }

    private static EtatCivilBeModificationResult sousConditions(
            String autorite, List<String> motifs, List<String> conseils,
            List<String> demarches, List<String> bases, String message) {
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return new EtatCivilBeModificationResult(
                EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE_SOUS_CONDITIONS,
                autorite, motifs, conseils, demarches, bases, messages);
    }

    private static EtatCivilBeModificationResult irrecevable(
            String autorite, List<String> motifs, List<String> conseils,
            List<String> bases, String motif, String message) {
        motifs.add(motif);
        List<String> messages = new ArrayList<>();
        messages.add(message);
        // Actes / démarches vides : la situation est dirimante en l'état.
        return new EtatCivilBeModificationResult(
                EtatCivilBeModificationVerdict.MODIFICATION_IRRECEVABLE,
                autorite, motifs, conseils, List.of(), bases, messages);
    }

    private static EtatCivilBeModificationResult incomplete(
            EtatCivilBeModificationInput in, String message) {
        List<String> motifs = new ArrayList<>();
        motifs.add("Les éléments de qualification disponibles ne permettent pas de trancher la "
                + "modification de l'état civil pour le type « " + in.typeModification() + " » (à vérifier "
                + "par avocat belge).");
        List<String> conseils = new ArrayList<>();
        conseils.add("Compléter les éléments de fait : majorité, nationalité / résidence et, selon le "
                + "type, motif (nom), rang de la demande (prénom) ou réitération de la déclaration (sexe).");
        List<String> bases = basesForType(in.typeModification());
        List<String> messages = new ArrayList<>();
        messages.add(message);
        return new EtatCivilBeModificationResult(
                EtatCivilBeModificationVerdict.QUALIFICATION_INCOMPLETE,
                autoriteForType(in.typeModification()), motifs, conseils, List.of(), bases, messages);
    }

    // ---------------------------------------------------------------
    // Conseils / démarches par branche
    // ---------------------------------------------------------------

    private static List<String> prenomConseils(EtatCivilBeModificationInput in) {
        List<String> c = new ArrayList<>();
        c.add("Distinguer nettement le changement de PRÉNOM (officier de l'état civil — loi du "
                + "18/06/2018) du changement de NOM (SPF Justice) : les autorités et les conditions "
                + "diffèrent (à vérifier par avocat belge).");
        c.add("Vérifier auprès de la commune le tarif de la redevance (réduit / gratuit pour la 1re "
                + "demande dans certains cas, notamment transidentité) — à vérifier par avocat belge.");
        return c;
    }

    private static List<String> nomConseils(EtatCivilBeModificationInput in) {
        List<String> c = new ArrayList<>();
        c.add("Le changement de nom relève du SPF Justice (≠ officier de l'état civil) : documenter "
                + "soigneusement le motif sérieux et l'absence de confusion / d'atteinte aux tiers (à "
                + "vérifier par avocat belge).");
        c.add("Anticiper les délais d'instruction du SPF Justice et, le cas échéant, la redevance / le "
                + "droit d'enregistrement applicable (à vérifier par avocat belge).");
        return c;
    }

    private static List<String> sexeConseils(EtatCivilBeModificationInput in) {
        List<String> c = new ArrayList<>();
        c.add("Spécificité belge : l'auto-déclaration de changement de sexe (loi du 25/06/2017) est "
                + "purement administrative et sans condition médicale — à distinguer d'une procédure "
                + "judiciaire (à vérifier par avocat belge).");
        c.add("Planifier les DEUX rendez-vous devant l'officier de l'état civil (déclaration initiale puis "
                + "seconde déclaration confirmative) en respectant le délai de réflexion (à vérifier par "
                + "avocat belge).");
        return c;
    }

    private static List<String> demarchesPrenom() {
        List<String> a = new ArrayList<>();
        a.add("Déposer la demande de changement de prénom auprès de l'officier de l'état civil de la "
                + "commune de résidence (loi du 18/06/2018).");
        a.add("Joindre les pièces requises (identité, justificatifs) et s'acquitter de la redevance "
                + "communale (réduite pour la 1re demande dans certains cas).");
        a.add("Obtenir la décision de l'officier et la mise à jour de l'acte / des registres.");
        return a;
    }

    private static List<String> demarchesNom() {
        List<String> a = new ArrayList<>();
        a.add("Introduire la requête en changement de nom auprès du SPF Justice (ministre de la Justice), "
                + "motif sérieux à l'appui.");
        a.add("Joindre les pièces requises et s'acquitter de la redevance / du droit d'enregistrement "
                + "éventuel.");
        a.add("Après autorisation, faire procéder à la transcription / mise à jour des actes de l'état "
                + "civil.");
        return a;
    }

    private static List<String> demarchesSexe() {
        List<String> a = new ArrayList<>();
        a.add("Faire la première déclaration de changement de sexe devant l'officier de l'état civil "
                + "(loi du 25/06/2017).");
        a.add("Respecter le délai de réflexion (≥ 3 mois et ≤ 6 mois) puis réitérer une SECONDE "
                + "déclaration confirmative devant l'officier de l'état civil.");
        a.add("L'officier de l'état civil acte la modification du sexe (et, le cas échéant, des prénoms) "
                + "et met à jour les actes / registres.");
        return a;
    }

    private static String autoriteForType(TypeModification t) {
        return t == TypeModification.CHANGEMENT_NOM ? AUTORITE_SPF_JUSTICE : AUTORITE_OFFICIER;
    }

    private static List<String> basesForType(TypeModification t) {
        switch (t) {
            case CHANGEMENT_NOM: return basesNom();
            case CHANGEMENT_SEXE: return basesSexe();
            case CHANGEMENT_PRENOM:
            default: return basesPrenom();
        }
    }

    // ---------------------------------------------------------------
    // Bases juridiques (aucune citation jurisprudentielle — F-JU-04 parké)
    // ---------------------------------------------------------------

    private static List<String> basesPrenom() {
        return List.of(
                "Loi du 18/06/2018 portant dispositions diverses en matière de droit civil — "
                        + "déjudiciarisation du changement de PRÉNOM : compétence de l'officier de l'état "
                        + "civil de la commune (à vérifier par avocat belge)",
                "Conditions : le prénom sollicité ne doit pas prêter à confusion ni nuire au demandeur ou "
                        + "aux tiers ; redevance communale (réduite / gratuite pour la 1re demande dans "
                        + "certains cas) — à vérifier par avocat belge",
                "Distinction avec le changement de NOM (SPF Justice) et avec la rectification d'état civil "
                        + "(erreur matérielle d'acte — hors périmètre de cet outil)");
    }

    private static List<String> basesNom() {
        return List.of(
                "Changement de NOM : procédure auprès du SPF Justice (ministre de la Justice) — motif "
                        + "sérieux requis, absence de confusion / d'atteinte aux tiers (Code civil, "
                        + "renumérotation post-réformes 2017-2019 ; loi du 18/06/2018) — à vérifier par "
                        + "avocat belge",
                "Condition de rattachement : nationalité belge ou statut assimilé (apatride / réfugié "
                        + "reconnu selon les cas) — à vérifier par avocat belge",
                "Distinction avec le changement de PRÉNOM (officier de l'état civil) et avec la "
                        + "rectification d'état civil (erreur matérielle d'acte — hors périmètre de cet "
                        + "outil)");
    }

    private static List<String> basesSexe() {
        return List.of(
                "Loi du 25/06/2017 réformant des régimes relatifs aux personnes transgenres en ce qui "
                        + "concerne la mention d'un changement de l'enregistrement du sexe dans les actes de "
                        + "l'état civil — AUTO-DÉCLARATION administrative, sans condition médicale (à "
                        + "vérifier par avocat belge)",
                "Procédure en deux temps : première déclaration, délai de réflexion (≥ 3 mois et ≤ 6 "
                        + "mois), puis seconde déclaration confirmative devant l'officier de l'état civil — à "
                        + "vérifier par avocat belge",
                "Régime spécifique pour le MINEUR (capacité de discernement / assistance des "
                        + "représentants légaux selon l'âge) — à vérifier par avocat belge");
    }
}

package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-24-03 : calculateur de validité d'un testament (FR — art. 967-1035 +
 * 901-911 Cciv) — détermine le verdict (VALIDE / CONTESTABLE / NUL) et la liste
 * des vices identifiés à partir de la forme du testament, de la capacité du
 * testateur, des vices de consentement éventuels et des causes de révocation.
 *
 * <p>4 formes de testament en droit français :</p>
 * <ol>
 *   <li>{@link FormeTestament#TESTAMENT_OLOGRAPHE} (art. 970) — entièrement
 *       écrit, daté et signé de la main du testateur.</li>
 *   <li>{@link FormeTestament#TESTAMENT_AUTHENTIQUE} (art. 971-975) — reçu par
 *       2 notaires ou 1 notaire + 2 témoins, dicté en présence, lu finalement,
 *       signatures complètes.</li>
 *   <li>{@link FormeTestament#TESTAMENT_MYSTIQUE} (art. 976-980) — remis
 *       cacheté à un notaire devant 2 témoins, acte de suscription dressé.</li>
 *   <li>{@link FormeTestament#TESTAMENT_INTERNATIONAL} (Convention Washington
 *       1973) — forme dérogatoire pour situations internationales.</li>
 * </ol>
 *
 * <p>Capacité (art. 901-911) : ≥ 16 ans (art. 904), sain d'esprit (art. 901),
 * majeur protégé avec assistance (art. 470, 476).</p>
 *
 * <p>Vices de consentement (art. 901+) : dol, violence, erreur substantielle.</p>
 *
 * <p>Révocation (art. 1035-1038) : testament postérieur contradictoire,
 * déchirure volontaire de l'original.</p>
 *
 * <p>Réserve / quotité disponible (art. 913-920) : un legs excédant la quotité
 * disponible n'invalide pas le testament — il ouvre une <strong>action en
 * réduction</strong> (art. 920+) — délai 5 ans à compter de l'ouverture de la
 * succession (art. 921).</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE (CC BE
 * art. 895+) suit un régime distinct (formes équivalentes mais quelques
 * exigences et délais différents) et fait l'objet d'une feature jumelle au
 * backlog (F-FA-24-BE-testament).</p>
 */
public final class TestamentValiditeCalculator {

    /** Forme du testament. */
    public enum FormeTestament {
        /** Testament olographe (art. 970) : manuscrit, daté, signé. */
        TESTAMENT_OLOGRAPHE,
        /** Testament authentique (art. 971-975) : devant notaire(s) et témoins. */
        TESTAMENT_AUTHENTIQUE,
        /** Testament mystique (art. 976-980) : pli cacheté + acte de suscription. */
        TESTAMENT_MYSTIQUE,
        /** Testament international (Convention Washington 1973). */
        TESTAMENT_INTERNATIONAL
    }

    /** Verdict de validité. */
    public enum VerdictValidite {
        /** Toutes les conditions sont réunies. */
        VALIDE,
        /** Critères douteux — le juge tranchera. */
        CONTESTABLE,
        /** Vice rédhibitoire — testament nul. */
        NUL
    }

    /** Codes de vices identifiés. */
    public enum CodeVice {
        FORME_OLOGRAPHE_NON_MANUSCRITE,
        FORME_OLOGRAPHE_NON_DATE,
        FORME_OLOGRAPHE_NON_SIGNE,
        FORME_AUTHENTIQUE_NOTAIRES_TEMOINS,
        FORME_AUTHENTIQUE_DICTEE_MANQUANTE,
        FORME_AUTHENTIQUE_LECTURE_MANQUANTE,
        FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES,
        FORME_MYSTIQUE_PLI_NON_CACHE,
        FORME_MYSTIQUE_TEMOINS,
        FORME_MYSTIQUE_SUSCRIPTION,
        FORME_INTERNATIONAL_WASHINGTON,
        INCAPACITE_MINEUR_MOINS_16_ANS,
        INSANITE_ESPRIT,
        MAJEUR_PROTEGE_SANS_ASSISTANCE,
        VICE_CONSENTEMENT_DOL,
        VICE_CONSENTEMENT_ERREUR,
        REVOCATION_TESTAMENT_POSTERIEUR,
        REVOCATION_DECHIRURE
    }

    /** Vice identifié avec libellé explicite. */
    public record ViceIdentifie(CodeVice code, String description) {}

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 967, 970, 971-975, 976-980, 901-911, 1035-1038, 920 et s. Cciv";

    /** Délai d'action en nullité (art. 1304 — 5 ans à compter de la connaissance). */
    private static final int DELAI_CONTESTATION_ANS = 5;

    private TestamentValiditeCalculator() {}

    /**
     * Évalue la validité d'un testament.
     *
     * @return résultat structuré avec verdict, vices, alertes
     * @throws IllegalArgumentException si paramètre invalide
     */
    public static TestamentValiditeResult compute(FormeTestament formeTestament,
                                                  String dateRedaction,
                                                  Integer ageTestateurAnsRedaction,
                                                  Boolean saineDEsprit,
                                                  Boolean majeurProtegeAvecAssistance,
                                                  Boolean ecritureManuscritIntegrale,
                                                  Boolean dateComplete,
                                                  Boolean signatureTestateur,
                                                  Boolean presenceNotaireEtTemoinsConforme,
                                                  Boolean dicteEnPresence,
                                                  Boolean lectureFinaleAuTestateur,
                                                  Boolean signaturesCompletes,
                                                  Boolean remiseSousPliCache,
                                                  Boolean declarationDevant2Temoins,
                                                  Boolean acteSuscriptionNotaire,
                                                  Boolean respecteFormeWashington,
                                                  Boolean vicesConsentementDol,
                                                  Boolean erreurSubstantielle,
                                                  Boolean testamentPosterieurContradictoire,
                                                  Boolean dechirureVolontaireOriginal,
                                                  Boolean legsExcedeQuotiteDisponible,
                                                  String country) {
        // ---- Validations strictes
        if (formeTestament == null) {
            throw new IllegalArgumentException("Forme du testament requise");
        }
        if (dateRedaction == null || dateRedaction.isBlank()) {
            throw new IllegalArgumentException("Date de rédaction du testament requise");
        }
        if (ageTestateurAnsRedaction == null) {
            throw new IllegalArgumentException("Âge du testateur à la rédaction requis");
        }
        if (ageTestateurAnsRedaction < 0 || ageTestateurAnsRedaction > 130) {
            throw new IllegalArgumentException("Âge du testateur invalide (doit être entre 0 et 130 ans)");
        }
        if (saineDEsprit == null) {
            throw new IllegalArgumentException("État sain d'esprit (oui/non) requis (art. 901 Cciv)");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC BE art. 895+ avec exigences"
                            + " et délais différents) sera traité dans une feature"
                            + " jumelle dédiée (F-FA-24-BE-testament).");
        }

        List<ViceIdentifie> vices = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ============================================================
        // 1. Capacité (art. 901-911)
        // ============================================================
        if (ageTestateurAnsRedaction < 16) {
            vices.add(new ViceIdentifie(CodeVice.INCAPACITE_MINEUR_MOINS_16_ANS,
                    "Le testateur avait moins de 16 ans à la date de rédaction "
                            + "(art. 904 Cciv) — incapacité absolue de tester."));
        }
        if (Boolean.FALSE.equals(saineDEsprit)) {
            vices.add(new ViceIdentifie(CodeVice.INSANITE_ESPRIT,
                    "Le testateur n'était pas sain d'esprit lors de la rédaction "
                            + "(art. 901 Cciv) — testament nul."));
        }
        if (Boolean.FALSE.equals(majeurProtegeAvecAssistance)) {
            // Majeur protégé qui a testé sans l'assistance requise
            vices.add(new ViceIdentifie(CodeVice.MAJEUR_PROTEGE_SANS_ASSISTANCE,
                    "Majeur protégé ayant testé sans l'assistance requise "
                            + "(art. 470, 476 Cciv) — testament contestable, "
                            + "le juge appréciera selon le régime de protection."));
        }

        // ============================================================
        // 2. Forme (selon type de testament)
        // ============================================================
        switch (formeTestament) {
            case TESTAMENT_OLOGRAPHE -> checkOlographe(
                    ecritureManuscritIntegrale, dateComplete, signatureTestateur, vices);
            case TESTAMENT_AUTHENTIQUE -> checkAuthentique(
                    presenceNotaireEtTemoinsConforme, dicteEnPresence,
                    lectureFinaleAuTestateur, signaturesCompletes, vices);
            case TESTAMENT_MYSTIQUE -> checkMystique(
                    remiseSousPliCache, declarationDevant2Temoins,
                    acteSuscriptionNotaire, signaturesCompletes, vices);
            case TESTAMENT_INTERNATIONAL -> checkInternational(
                    respecteFormeWashington, signaturesCompletes, vices);
        }

        // ============================================================
        // 3. Vices de consentement (art. 901+)
        // ============================================================
        if (Boolean.TRUE.equals(vicesConsentementDol)) {
            vices.add(new ViceIdentifie(CodeVice.VICE_CONSENTEMENT_DOL,
                    "Vice de consentement (dol ou violence) au moment de la rédaction "
                            + "(art. 901 Cciv) — testament nul."));
        }
        if (Boolean.TRUE.equals(erreurSubstantielle)) {
            vices.add(new ViceIdentifie(CodeVice.VICE_CONSENTEMENT_ERREUR,
                    "Erreur substantielle ayant déterminé la volonté du testateur "
                            + "(art. 901 Cciv) — testament nul."));
        }

        // ============================================================
        // 4. Révocation (art. 1035-1038)
        // ============================================================
        if (Boolean.TRUE.equals(testamentPosterieurContradictoire)) {
            vices.add(new ViceIdentifie(CodeVice.REVOCATION_TESTAMENT_POSTERIEUR,
                    "Testament postérieur contradictoire identifié "
                            + "(art. 1036 Cciv) — révocation tacite du testament examiné."));
        }
        if (Boolean.TRUE.equals(dechirureVolontaireOriginal)) {
            vices.add(new ViceIdentifie(CodeVice.REVOCATION_DECHIRURE,
                    "Déchirure volontaire de l'original par le testateur "
                            + "(art. 1038 Cciv) — révocation matérielle."));
        }

        // ============================================================
        // 5. Verdict
        // ============================================================
        VerdictValidite verdict = computeVerdict(vices);

        // ============================================================
        // 6. Action en réduction (art. 920+) — n'invalide pas le testament
        // ============================================================
        boolean actionReduction = Boolean.TRUE.equals(legsExcedeQuotiteDisponible);
        if (actionReduction) {
            messages.add("Le testament prévoit des legs excédant la quotité disponible "
                    + "(art. 913-920 Cciv) — les héritiers réservataires peuvent intenter "
                    + "une ACTION EN RÉDUCTION (art. 920 et s. Cciv) dans un délai de 5 ans "
                    + "à compter de l'ouverture de la succession (art. 921). Cette action "
                    + "n'affecte pas la validité du testament : elle réduit les legs à "
                    + "la quotité disponible.");
        }

        // ============================================================
        // 7. Messages de synthèse selon verdict
        // ============================================================
        switch (verdict) {
            case VALIDE -> messages.add("Testament VALIDE — toutes les conditions de forme, "
                    + "capacité et consentement sont réunies. Aucune cause de révocation "
                    + "identifiée.");
            case CONTESTABLE -> messages.add("Testament CONTESTABLE — au moins un critère "
                    + "douteux est identifié. La validité dépendra de l'appréciation "
                    + "souveraine des juges du fond. Recueillir la preuve avant action.");
            case NUL -> messages.add("Testament NUL — un ou plusieurs vices rédhibitoires "
                    + "ont été identifiés (forme, capacité, consentement ou révocation). "
                    + "Action en nullité ouverte (art. 1304 Cciv — délai 5 ans à compter "
                    + "de la connaissance du vice).");
        }

        // ============================================================
        // 8. Score
        // ============================================================
        int score = computeScore(verdict, vices, actionReduction);

        String formule = String.format(Locale.ROOT,
                "Forme %s + verdict %s + %d vice(s) + action en réduction=%s "
                        + "+ âge=%d + sain d'esprit=%s + majeur protégé assisté=%s "
                        + "→ score %d",
                formeTestament.name(), verdict.name(), vices.size(),
                actionReduction, ageTestateurAnsRedaction,
                saineDEsprit, majeurProtegeAvecAssistance,
                score);

        messages.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return new TestamentValiditeResult(
                formeTestament,
                dateRedaction,
                ageTestateurAnsRedaction,
                saineDEsprit,
                majeurProtegeAvecAssistance,
                ecritureManuscritIntegrale,
                dateComplete,
                signatureTestateur,
                presenceNotaireEtTemoinsConforme,
                dicteEnPresence,
                lectureFinaleAuTestateur,
                signaturesCompletes,
                remiseSousPliCache,
                declarationDevant2Temoins,
                acteSuscriptionNotaire,
                respecteFormeWashington,
                vicesConsentementDol,
                erreurSubstantielle,
                testamentPosterieurContradictoire,
                dechirureVolontaireOriginal,
                legsExcedeQuotiteDisponible,
                countryNormalized,
                verdict,
                vices,
                actionReduction,
                DELAI_CONTESTATION_ANS,
                score,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static void checkOlographe(Boolean ecritureManuscritIntegrale,
                                       Boolean dateComplete,
                                       Boolean signatureTestateur,
                                       List<ViceIdentifie> vices) {
        if (!Boolean.TRUE.equals(ecritureManuscritIntegrale)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_OLOGRAPHE_NON_MANUSCRITE,
                    "Testament olographe non entièrement écrit de la main du "
                            + "testateur (art. 970 Cciv) — nul."));
        }
        if (!Boolean.TRUE.equals(dateComplete)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_OLOGRAPHE_NON_DATE,
                    "Testament olographe sans date complète (jour, mois, année) "
                            + "(art. 970 Cciv) — nul."));
        }
        if (!Boolean.TRUE.equals(signatureTestateur)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_OLOGRAPHE_NON_SIGNE,
                    "Testament olographe non signé de la main du testateur "
                            + "(art. 970 Cciv) — nul."));
        }
    }

    private static void checkAuthentique(Boolean presenceNotaireEtTemoinsConforme,
                                         Boolean dicteEnPresence,
                                         Boolean lectureFinaleAuTestateur,
                                         Boolean signaturesCompletes,
                                         List<ViceIdentifie> vices) {
        if (!Boolean.TRUE.equals(presenceNotaireEtTemoinsConforme)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_AUTHENTIQUE_NOTAIRES_TEMOINS,
                    "Testament authentique sans la présence requise (2 notaires ou "
                            + "1 notaire + 2 témoins, art. 971 Cciv) — nul."));
        }
        if (!Boolean.TRUE.equals(dicteEnPresence)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_AUTHENTIQUE_DICTEE_MANQUANTE,
                    "Testament authentique non dicté par le testateur en présence "
                            + "des notaires/témoins (art. 972 Cciv) — nul."));
        }
        if (!Boolean.TRUE.equals(lectureFinaleAuTestateur)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_AUTHENTIQUE_LECTURE_MANQUANTE,
                    "Testament authentique non lu intégralement au testateur avant "
                            + "signatures (art. 972 Cciv) — vice de forme contestable."));
        }
        if (!Boolean.TRUE.equals(signaturesCompletes)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES,
                    "Testament authentique avec signatures incomplètes (testateur, "
                            + "notaires et témoins, art. 973-974 Cciv) — nul."));
        }
    }

    private static void checkMystique(Boolean remiseSousPliCache,
                                      Boolean declarationDevant2Temoins,
                                      Boolean acteSuscriptionNotaire,
                                      Boolean signaturesCompletes,
                                      List<ViceIdentifie> vices) {
        if (!Boolean.TRUE.equals(remiseSousPliCache)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_MYSTIQUE_PLI_NON_CACHE,
                    "Testament mystique non remis sous pli cacheté au notaire "
                            + "(art. 976 Cciv) — nul."));
        }
        if (!Boolean.TRUE.equals(declarationDevant2Temoins)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_MYSTIQUE_TEMOINS,
                    "Testament mystique sans déclaration du testateur devant 2 témoins "
                            + "(art. 976 Cciv) — nul."));
        }
        if (!Boolean.TRUE.equals(acteSuscriptionNotaire)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_MYSTIQUE_SUSCRIPTION,
                    "Testament mystique sans acte de suscription dressé par le notaire "
                            + "(art. 976 Cciv) — nul."));
        }
        if (signaturesCompletes != null && !signaturesCompletes) {
            vices.add(new ViceIdentifie(CodeVice.FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES,
                    "Testament mystique avec signatures incomplètes "
                            + "(testateur + notaire + 2 témoins, art. 976 Cciv) — nul."));
        }
    }

    private static void checkInternational(Boolean respecteFormeWashington,
                                           Boolean signaturesCompletes,
                                           List<ViceIdentifie> vices) {
        if (!Boolean.TRUE.equals(respecteFormeWashington)) {
            vices.add(new ViceIdentifie(CodeVice.FORME_INTERNATIONAL_WASHINGTON,
                    "Testament international non conforme aux exigences de la "
                            + "Convention de Washington 1973 — nul."));
        }
        if (signaturesCompletes != null && !signaturesCompletes) {
            vices.add(new ViceIdentifie(CodeVice.FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES,
                    "Testament international avec signatures incomplètes "
                            + "(testateur, témoins, personne habilitée) — nul."));
        }
    }

    private static VerdictValidite computeVerdict(List<ViceIdentifie> vices) {
        if (vices.isEmpty()) {
            return VerdictValidite.VALIDE;
        }
        // Vices CONTESTABLE-only (jamais NUL si seuls)
        for (ViceIdentifie v : vices) {
            if (isRedhibitoire(v.code())) {
                return VerdictValidite.NUL;
            }
        }
        return VerdictValidite.CONTESTABLE;
    }

    private static boolean isRedhibitoire(CodeVice code) {
        return switch (code) {
            // Vices de forme rédhibitoires
            case FORME_OLOGRAPHE_NON_MANUSCRITE,
                 FORME_OLOGRAPHE_NON_DATE,
                 FORME_OLOGRAPHE_NON_SIGNE,
                 FORME_AUTHENTIQUE_NOTAIRES_TEMOINS,
                 FORME_AUTHENTIQUE_DICTEE_MANQUANTE,
                 FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES,
                 FORME_MYSTIQUE_PLI_NON_CACHE,
                 FORME_MYSTIQUE_TEMOINS,
                 FORME_MYSTIQUE_SUSCRIPTION,
                 FORME_INTERNATIONAL_WASHINGTON,
                 INCAPACITE_MINEUR_MOINS_16_ANS,
                 INSANITE_ESPRIT,
                 VICE_CONSENTEMENT_DOL,
                 VICE_CONSENTEMENT_ERREUR,
                 REVOCATION_TESTAMENT_POSTERIEUR,
                 REVOCATION_DECHIRURE -> true;
            // Vices contestables (le juge tranchera)
            case FORME_AUTHENTIQUE_LECTURE_MANQUANTE,
                 MAJEUR_PROTEGE_SANS_ASSISTANCE -> false;
        };
    }

    private static int computeScore(VerdictValidite verdict,
                                    List<ViceIdentifie> vices,
                                    boolean actionReduction) {
        int base = switch (verdict) {
            case VALIDE -> 100;
            case CONTESTABLE -> 50;
            case NUL -> 10;
        };
        // Pénalité par vice (max 5 vices comptés)
        int penalty = Math.min(vices.size(), 5) * 5;
        int score = Math.max(0, base - penalty);
        if (actionReduction && verdict == VerdictValidite.VALIDE) {
            // Une action en réduction n'invalide pas mais signale un risque
            score = Math.max(score - 10, 60);
        }
        return Math.min(score, 100);
    }
}

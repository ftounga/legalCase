package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-24-05 : calculateur de validité d'une donation entre vifs (FR — art.
 * 893-958 + 902-906 + 920+ Cciv) — détermine le verdict (VALIDE / CONTESTABLE /
 * NUL), la liste des risques de requalification ou de nullité, l'éventuelle
 * action en réduction et la possibilité de révocation.
 *
 * <p>4 formes de donation en droit français :</p>
 * <ol>
 *   <li>{@link FormeDonation#DONATION_NOTARIEE} (art. 931) — authentique,
 *       obligatoire pour les immeubles et les promesses de donation.</li>
 *   <li>{@link FormeDonation#DONATION_MANUELLE} (art. 894+) — remise effective
 *       d'un bien meuble ; pas d'écrit obligatoire mais preuve fragile.</li>
 *   <li>{@link FormeDonation#DON_INDIRECT} — avantage octroyé sans intention
 *       apparente (renonciation à un droit, remise de dette, garantie...).</li>
 *   <li>{@link FormeDonation#DONATION_DEGUISEE} — sous l'apparence d'un acte
 *       onéreux (vente à prix vil) — risque de requalification fiscale et
 *       civile.</li>
 * </ol>
 *
 * <p>Capacité (art. 902-906) : sain d'esprit, ≥ 16 ans pour mobilier ; ≥ 18 ans
 * pour le reste. Récipiendaire : exister à la date, pas être incapable absolu
 * (médecin du donateur, art. 909).</p>
 *
 * <p>Vices de consentement (art. 901+) : dol, violence, erreur substantielle.</p>
 *
 * <p>Quotité disponible (art. 913-920) : si la donation excède la quotité, elle
 * reste valide mais ouvre une <strong>action en réduction</strong> (art. 920+)
 * — délai 5 ans à compter du décès du donateur.</p>
 *
 * <p>Révocation (art. 953-958) : 3 motifs limitatifs — ingratitude (art. 955),
 * inexécution des charges (art. 953), survenance d'enfant (art. 960-961, abrogé
 * 2007 sauf clause expresse).</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE (CC BE
 * art. 893+) suit un régime distinct (formes équivalentes mais quelques
 * exigences spécifiques notamment pour les donations entre époux) et fait
 * l'objet d'une feature jumelle au backlog (F-FA-24-BE-donation).</p>
 */
public final class DonationCalculator {

    /** Forme de la donation. */
    public enum FormeDonation {
        /** Donation notariée (art. 931) — acte authentique. */
        DONATION_NOTARIEE,
        /** Donation manuelle (art. 894+) — remise effective d'un bien meuble. */
        DONATION_MANUELLE,
        /** Don indirect — avantage octroyé sans intention apparente. */
        DON_INDIRECT,
        /** Donation déguisée — sous apparence d'acte onéreux. */
        DONATION_DEGUISEE
    }

    /** Verdict de validité. */
    public enum VerdictValidite {
        /** Toutes les conditions sont réunies. */
        VALIDE,
        /** Critères douteux — le juge tranchera. */
        CONTESTABLE,
        /** Vice rédhibitoire — donation nulle. */
        NUL
    }

    /** Codes de risques identifiés (vices de validité ou de requalification). */
    public enum CodeRisque {
        FORME_NOTARIEE_NON_AUTHENTIQUE,
        FORME_NOTARIEE_SANS_ACCEPTATION,
        FORME_MANUELLE_SANS_REMISE,
        FORME_MANUELLE_BIEN_NON_MEUBLE,
        DON_INDIRECT_INTENTION_LIBERALE,
        REQUALIFICATION_DEGUISEMENT,
        DEGUISEMENT_PRIX_VIL,
        INCAPACITE_DONATEUR,
        INSANITE_ESPRIT,
        INCAPACITE_RECIPIENDAIRE,
        VICE_CONSENTEMENT_DOL,
        VICE_CONSENTEMENT_ERREUR,
        OBJET_INDETERMINE,
        FORMALISME_NON_RESPECTE,
        EXCES_QUOTITE_DISPONIBLE,
        REVOCATION_INGRATITUDE,
        REVOCATION_INEXECUTION_CHARGE
    }

    /** Risque identifié avec libellé explicite. */
    public record RisqueIdentifie(CodeRisque code, String description) {}

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 893-958, 902-906, 920 et s., 931, 953-958 Cciv";

    /** Délai d'action de droit commun (art. 1304 / 2224 — 5 ans). */
    private static final int DELAI_CONTESTATION_ANS = 5;

    private DonationCalculator() {}

    /**
     * Évalue la validité d'une donation entre vifs.
     *
     * @return résultat structuré avec verdict, risques, alertes
     * @throws IllegalArgumentException si paramètre invalide
     */
    public static DonationResult compute(FormeDonation formeDonation,
                                         String dateDonation,
                                         Integer ageDonateurAns,
                                         Boolean saineDEsprit,
                                         Boolean capaciteDonateur,
                                         Boolean capaciteRecipiendaire,
                                         Boolean consentementLibre,
                                         Boolean objetDeterminé,
                                         Boolean respectFormalisme,
                                         Boolean respectQuotiteDisponible,
                                         Boolean acteAuthentique,
                                         Boolean acceptationExpresse,
                                         Boolean remiseEffective,
                                         Boolean bienMeuble,
                                         Boolean intentionLiberale,
                                         Boolean actePrincipalNeutre,
                                         Boolean apparenceOnerueuse,
                                         Boolean prixIncoherent,
                                         Boolean vicesConsentementDol,
                                         Boolean erreurSubstantielle,
                                         Boolean ingratitudeAvere,
                                         Boolean inexecutionCharge,
                                         String country) {
        // ---- Validations strictes
        if (formeDonation == null) {
            throw new IllegalArgumentException("Forme de la donation requise");
        }
        if (dateDonation == null || dateDonation.isBlank()) {
            throw new IllegalArgumentException("Date de la donation requise");
        }
        if (ageDonateurAns == null) {
            throw new IllegalArgumentException("Âge du donateur requis");
        }
        if (ageDonateurAns < 0 || ageDonateurAns > 130) {
            throw new IllegalArgumentException("Âge du donateur invalide (doit être entre 0 et 130 ans)");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC BE art. 893+ avec exigences"
                            + " spécifiques notamment pour les donations entre"
                            + " époux) sera traité dans une feature jumelle"
                            + " dédiée (F-FA-24-BE-donation).");
        }

        List<RisqueIdentifie> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ============================================================
        // 1. Capacité du donateur (art. 902-904)
        // ============================================================
        if (ageDonateurAns < 16) {
            risques.add(new RisqueIdentifie(CodeRisque.INCAPACITE_DONATEUR,
                    "Le donateur avait moins de 16 ans à la date de la donation "
                            + "(art. 904 Cciv) — incapacité absolue de donner."));
        }
        if (Boolean.FALSE.equals(saineDEsprit)) {
            risques.add(new RisqueIdentifie(CodeRisque.INSANITE_ESPRIT,
                    "Le donateur n'était pas sain d'esprit lors de la donation "
                            + "(art. 901 Cciv) — donation nulle."));
        }
        if (Boolean.FALSE.equals(capaciteDonateur)) {
            risques.add(new RisqueIdentifie(CodeRisque.INCAPACITE_DONATEUR,
                    "Le donateur n'avait pas la capacité de donner "
                            + "(art. 902 Cciv) — donation nulle."));
        }

        // ============================================================
        // 2. Capacité du récipiendaire (art. 906, 909)
        // ============================================================
        if (Boolean.FALSE.equals(capaciteRecipiendaire)) {
            risques.add(new RisqueIdentifie(CodeRisque.INCAPACITE_RECIPIENDAIRE,
                    "Le récipiendaire est frappé d'une incapacité absolue de "
                            + "recevoir (art. 906, 909 Cciv — par ex. médecin du "
                            + "donateur, ministre du culte) — donation nulle."));
        }

        // ============================================================
        // 3. Consentement (art. 901+)
        // ============================================================
        if (Boolean.FALSE.equals(consentementLibre)) {
            risques.add(new RisqueIdentifie(CodeRisque.VICE_CONSENTEMENT_DOL,
                    "Le consentement du donateur n'était pas libre "
                            + "(art. 901 Cciv) — donation nulle."));
        }
        if (Boolean.TRUE.equals(vicesConsentementDol)) {
            risques.add(new RisqueIdentifie(CodeRisque.VICE_CONSENTEMENT_DOL,
                    "Vice de consentement (dol ou violence) au moment de la "
                            + "donation (art. 901 Cciv) — donation nulle."));
        }
        if (Boolean.TRUE.equals(erreurSubstantielle)) {
            risques.add(new RisqueIdentifie(CodeRisque.VICE_CONSENTEMENT_ERREUR,
                    "Erreur substantielle ayant déterminé la volonté du donateur "
                            + "(art. 901 Cciv) — donation nulle."));
        }

        // ============================================================
        // 4. Objet (art. 893+)
        // ============================================================
        if (Boolean.FALSE.equals(objetDeterminé)) {
            risques.add(new RisqueIdentifie(CodeRisque.OBJET_INDETERMINE,
                    "L'objet de la donation n'est pas déterminé ou n'existe pas "
                            + "à la date de la donation (art. 893+ Cciv) — donation nulle."));
        }

        // ============================================================
        // 5. Forme (selon type de donation)
        // ============================================================
        switch (formeDonation) {
            case DONATION_NOTARIEE -> checkNotariee(
                    acteAuthentique, acceptationExpresse, respectFormalisme, risques);
            case DONATION_MANUELLE -> checkManuelle(
                    remiseEffective, bienMeuble, risques);
            case DON_INDIRECT -> checkIndirect(
                    intentionLiberale, actePrincipalNeutre, risques);
            case DONATION_DEGUISEE -> checkDeguisee(
                    apparenceOnerueuse, prixIncoherent, risques);
        }

        // ============================================================
        // 6. Verdict
        // ============================================================
        VerdictValidite verdict = computeVerdict(risques);

        // ============================================================
        // 7. Action en réduction (art. 920+) — n'invalide pas la donation
        // ============================================================
        boolean actionReduction = Boolean.FALSE.equals(respectQuotiteDisponible);
        if (actionReduction) {
            messages.add("La donation excède la quotité disponible "
                    + "(art. 913-920 Cciv) — les héritiers réservataires peuvent "
                    + "intenter une ACTION EN RÉDUCTION (art. 920 et s. Cciv) dans "
                    + "un délai de 5 ans à compter du décès du donateur. Cette "
                    + "action n'affecte pas la validité de la donation : elle "
                    + "réduit le montant donné à la quotité disponible.");
        }

        // ============================================================
        // 8. Révocation (art. 953-958) — événement post-donation
        // ============================================================
        boolean revocationPossible = Boolean.TRUE.equals(ingratitudeAvere)
                || Boolean.TRUE.equals(inexecutionCharge);
        if (Boolean.TRUE.equals(ingratitudeAvere)) {
            messages.add("Ingratitude avérée du donataire (art. 955-958 Cciv) — "
                    + "RÉVOCATION POSSIBLE pour ingratitude (attentat à la vie, "
                    + "sévices, refus d'aliments). Délai 1 an à compter du jour "
                    + "où l'ingratitude a été connue (art. 957). Note : la "
                    + "révocation ne nullifie pas la donation à sa formation, "
                    + "elle agit pour l'avenir.");
        }
        if (Boolean.TRUE.equals(inexecutionCharge)) {
            messages.add("Inexécution des charges grevant la donation "
                    + "(art. 953 Cciv) — RÉVOCATION POSSIBLE pour inexécution. "
                    + "Pas de délai légal spécifique : prescription droit commun "
                    + "5 ans (art. 2224).");
        }

        // ============================================================
        // 9. Messages de synthèse selon verdict
        // ============================================================
        switch (verdict) {
            case VALIDE -> messages.add("Donation VALIDE — toutes les conditions "
                    + "de forme, capacité et consentement sont réunies.");
            case CONTESTABLE -> messages.add("Donation CONTESTABLE — au moins un "
                    + "critère douteux est identifié, notamment un risque de "
                    + "requalification (déguisement, intention libérale). La "
                    + "validité dépendra de l'appréciation souveraine des juges "
                    + "du fond. Recueillir la preuve avant action.");
            case NUL -> messages.add("Donation NULLE — un ou plusieurs vices "
                    + "rédhibitoires ont été identifiés (forme, capacité, "
                    + "consentement, objet). Action en nullité ouverte (art. 1304 "
                    + "Cciv — délai 5 ans à compter de la connaissance du vice).");
        }

        // ============================================================
        // 10. Score
        // ============================================================
        int score = computeScore(verdict, risques, actionReduction);

        String formule = String.format(Locale.ROOT,
                "Forme %s + verdict %s + %d risque(s) + action en réduction=%s "
                        + "+ révocation possible=%s + âge=%d → score %d",
                formeDonation.name(), verdict.name(), risques.size(),
                actionReduction, revocationPossible, ageDonateurAns,
                score);

        messages.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return new DonationResult(
                formeDonation,
                dateDonation,
                ageDonateurAns,
                saineDEsprit,
                capaciteDonateur,
                capaciteRecipiendaire,
                consentementLibre,
                objetDeterminé,
                respectFormalisme,
                respectQuotiteDisponible,
                acteAuthentique,
                acceptationExpresse,
                remiseEffective,
                bienMeuble,
                intentionLiberale,
                actePrincipalNeutre,
                apparenceOnerueuse,
                prixIncoherent,
                vicesConsentementDol,
                erreurSubstantielle,
                ingratitudeAvere,
                inexecutionCharge,
                countryNormalized,
                verdict,
                risques,
                actionReduction,
                revocationPossible,
                DELAI_CONTESTATION_ANS,
                score,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static void checkNotariee(Boolean acteAuthentique,
                                      Boolean acceptationExpresse,
                                      Boolean respectFormalisme,
                                      List<RisqueIdentifie> risques) {
        if (!Boolean.TRUE.equals(acteAuthentique)) {
            risques.add(new RisqueIdentifie(CodeRisque.FORME_NOTARIEE_NON_AUTHENTIQUE,
                    "Donation notariée sans acte authentique reçu par notaire "
                            + "(art. 931 Cciv) — nulle de plein droit."));
        }
        if (!Boolean.TRUE.equals(acceptationExpresse)) {
            risques.add(new RisqueIdentifie(CodeRisque.FORME_NOTARIEE_SANS_ACCEPTATION,
                    "Donation notariée sans acceptation expresse du donataire "
                            + "(art. 932 Cciv) — nulle."));
        }
        if (Boolean.FALSE.equals(respectFormalisme)) {
            risques.add(new RisqueIdentifie(CodeRisque.FORMALISME_NON_RESPECTE,
                    "Formalisme général de la donation notariée non respecté "
                            + "(art. 931+ Cciv)."));
        }
    }

    private static void checkManuelle(Boolean remiseEffective,
                                      Boolean bienMeuble,
                                      List<RisqueIdentifie> risques) {
        if (!Boolean.TRUE.equals(remiseEffective)) {
            risques.add(new RisqueIdentifie(CodeRisque.FORME_MANUELLE_SANS_REMISE,
                    "Donation manuelle sans remise effective du bien au donataire "
                            + "(jurisprudence constante sur art. 894 Cciv) — "
                            + "donation inexistante (l'élément matériel fait défaut)."));
        }
        if (!Boolean.TRUE.equals(bienMeuble)) {
            risques.add(new RisqueIdentifie(CodeRisque.FORME_MANUELLE_BIEN_NON_MEUBLE,
                    "Donation manuelle portant sur un bien non meuble (immeuble) "
                            + "— forme notariée obligatoire (art. 931 Cciv) — nulle."));
        }
    }

    private static void checkIndirect(Boolean intentionLiberale,
                                      Boolean actePrincipalNeutre,
                                      List<RisqueIdentifie> risques) {
        if (!Boolean.TRUE.equals(intentionLiberale)) {
            risques.add(new RisqueIdentifie(CodeRisque.DON_INDIRECT_INTENTION_LIBERALE,
                    "Don indirect sans intention libérale clairement caractérisée "
                            + "— le juge appréciera. La requalification en "
                            + "donation est contestable."));
        }
        if (Boolean.FALSE.equals(actePrincipalNeutre)) {
            risques.add(new RisqueIdentifie(CodeRisque.REQUALIFICATION_DEGUISEMENT,
                    "L'acte principal présente un caractère onéreux marqué — "
                            + "risque de requalification en donation déguisée "
                            + "plutôt qu'en don indirect."));
        }
    }

    private static void checkDeguisee(Boolean apparenceOnerueuse,
                                      Boolean prixIncoherent,
                                      List<RisqueIdentifie> risques) {
        if (Boolean.TRUE.equals(apparenceOnerueuse) && Boolean.TRUE.equals(prixIncoherent)) {
            risques.add(new RisqueIdentifie(CodeRisque.REQUALIFICATION_DEGUISEMENT,
                    "Donation déguisée sous l'apparence d'un acte onéreux (vente "
                            + "à prix vil, par ex.) — risque de REQUALIFICATION "
                            + "civile et fiscale (art. 911, 918 Cciv)."));
            risques.add(new RisqueIdentifie(CodeRisque.DEGUISEMENT_PRIX_VIL,
                    "Prix manifestement incohérent avec la valeur du bien — "
                            + "indice fort de déguisement."));
        } else if (Boolean.TRUE.equals(apparenceOnerueuse) && !Boolean.TRUE.equals(prixIncoherent)) {
            // Apparence onéreuse cohérente : pas de risque de requalification.
        } else if (!Boolean.TRUE.equals(apparenceOnerueuse)) {
            risques.add(new RisqueIdentifie(CodeRisque.REQUALIFICATION_DEGUISEMENT,
                    "Donation déguisée déclarée mais sans apparence onéreuse — "
                            + "incohérence dans la qualification."));
        }
    }

    private static VerdictValidite computeVerdict(List<RisqueIdentifie> risques) {
        if (risques.isEmpty()) {
            return VerdictValidite.VALIDE;
        }
        for (RisqueIdentifie r : risques) {
            if (isRedhibitoire(r.code())) {
                return VerdictValidite.NUL;
            }
        }
        return VerdictValidite.CONTESTABLE;
    }

    private static boolean isRedhibitoire(CodeRisque code) {
        return switch (code) {
            // Vices de forme rédhibitoires
            case FORME_NOTARIEE_NON_AUTHENTIQUE,
                 FORME_NOTARIEE_SANS_ACCEPTATION,
                 FORME_MANUELLE_SANS_REMISE,
                 FORME_MANUELLE_BIEN_NON_MEUBLE,
                 INCAPACITE_DONATEUR,
                 INSANITE_ESPRIT,
                 INCAPACITE_RECIPIENDAIRE,
                 VICE_CONSENTEMENT_DOL,
                 VICE_CONSENTEMENT_ERREUR,
                 OBJET_INDETERMINE -> true;
            // Risques contestables (le juge tranchera ou événement post-don)
            case DON_INDIRECT_INTENTION_LIBERALE,
                 REQUALIFICATION_DEGUISEMENT,
                 DEGUISEMENT_PRIX_VIL,
                 FORMALISME_NON_RESPECTE,
                 EXCES_QUOTITE_DISPONIBLE,
                 REVOCATION_INGRATITUDE,
                 REVOCATION_INEXECUTION_CHARGE -> false;
        };
    }

    private static int computeScore(VerdictValidite verdict,
                                    List<RisqueIdentifie> risques,
                                    boolean actionReduction) {
        int base = switch (verdict) {
            case VALIDE -> 100;
            case CONTESTABLE -> 50;
            case NUL -> 10;
        };
        int penalty = Math.min(risques.size(), 5) * 5;
        int score = Math.max(0, base - penalty);
        if (actionReduction && verdict == VerdictValidite.VALIDE) {
            score = Math.max(score - 10, 60);
        }
        return Math.min(score, 100);
    }
}

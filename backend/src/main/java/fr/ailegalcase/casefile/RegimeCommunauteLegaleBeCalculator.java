package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-217-01 : moteur de qualification du patrimoine d'un couple marié sous le
 * régime de communauté légale belge (Livre 3 du Code civil, loi du 22/07/2018).
 *
 * <p>Qualifie chaque bien ({@code COMMUN} / {@code PROPRE} / {@code MIXTE_RECOMPENSE}
 * / {@code INDETERMINE}), détermine son mode de gestion, qualifie chaque dette
 * ({@code DETTE_COMMUNE} / {@code DETTE_PROPRE} / {@code DETTE_PROPRE_AVEC_RECOURS})
 * et rend un verdict global de composition.</p>
 *
 * <p><b>Logique du verdict</b> : un contrat de mariage signé fait basculer le
 * verdict en {@code REGIME_CONVENTIONNEL_DETECTE} (la qualification légale reste
 * fournie, à titre indicatif). À défaut de contrat, la présence d'au moins un bien
 * {@code INDETERMINE} rend le verdict {@code QUALIFICATION_INCOMPLETE} ; sinon
 * {@code COMMUNAUTE_LEGALE_APPLICABLE}.</p>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. La communauté légale belge (Livre 3 CC,
 * loi 22/07/2018) diffère de la communauté réduite aux acquêts française —
 * aucun Calculator FR n'est réutilisé.</p>
 *
 * <p><b>Validation juridique requise</b> : la numérotation du Livre 3 du Code
 * civil belge issue de la loi du 22/07/2018 (entrée en vigueur 01/09/2018) a été
 * massivement renumérotée. Les articles cités ci-dessous (art. 3.45, 3.46, 3.55,
 * 3.56, 3.58, 3.65) reflètent l'état du droit connu du modèle et sont
 * <b>à valider par un avocat belge avant mise en production</b>. Le contenu
 * juridique (articles, règles de qualification, libellés de fondement) est
 * centralisé ici (source unique de vérité).</p>
 */
public final class RegimeCommunauteLegaleBeCalculator {

    private static final String COUNTRY_BE = "BELGIQUE";
    private static final int MAX_LIBELLE = 200;
    private static final int MAX_COMMENTAIRE = 1000;

    // --- Fondements juridiques (Livre 3 CC belge — à valider par un avocat belge) ---
    private static final String FOND_BIEN_COMMUN =
            "CC Livre 3, art. 3.45 (biens communs — acquêts)";
    private static final String FOND_BIEN_PROPRE =
            "CC Livre 3, art. 3.46 (biens propres — origine / affectation)";
    private static final String FOND_BIEN_MIXTE =
            "CC Livre 3, art. 3.46 (biens propres) + régime des récompenses";
    private static final String FOND_GESTION_CONCURRENTE =
            "CC Livre 3, art. 3.65 (gestion concurrente)";
    private static final String FOND_GESTION_EXCLUSIVE =
            "CC Livre 3, art. 3.65 (gestion exclusive du bien propre / de l'outil de travail)";
    private static final String FOND_COGESTION =
            "CC Livre 3, art. 3.65 (cogestion — actes graves sur biens communs)";
    private static final String FOND_DETTE_COMMUNE =
            "CC Livre 3, art. 3.55 (dettes communes — intérêt du ménage)";
    private static final String FOND_DETTE_PROPRE =
            "CC Livre 3, art. 3.56 (dettes propres)";
    private static final String FOND_DETTE_PROPRE_RECOURS =
            "CC Livre 3, art. 3.56 et 3.58 (dette propre — recours du créancier sur les biens communs)";

    private RegimeCommunauteLegaleBeCalculator() {}

    /**
     * Qualifie le patrimoine d'un couple marié sous le régime de communauté légale belge.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, biens et dettes qualifiés, synthèse, bases juridiques)
     * @throws IllegalArgumentException si la validation échoue ou si le pays n'est pas supporté
     */
    public static RegimeCommunauteLegaleBeResult compute(RegimeCommunauteLegaleBeInput input,
                                                         String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_BE.equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE (régime de communauté légale belge)");
        }
        validateInput(input);

        List<RegimeCommunauteLegaleBeResult.BienQualifie> biens = new ArrayList<>();
        for (RegimeCommunauteLegaleBeInput.BienItem b : input.biens()) {
            biens.add(qualifierBien(b));
        }
        List<RegimeCommunauteLegaleBeResult.DetteQualifiee> dettes = new ArrayList<>();
        for (RegimeCommunauteLegaleBeInput.DetteItem d : input.dettes()) {
            dettes.add(qualifierDette(d));
        }

        RegimeCommunauteLegaleBeVerdict verdict = verdictPour(input, biens);
        RegimeCommunauteLegaleBeResult.SyntheseComposition synthese = synthese(biens, dettes);
        List<String> bases = basesJuridiques(biens, dettes);
        List<String> messages = construireMessages(input, biens, verdict);

        return new RegimeCommunauteLegaleBeResult(
                verdict,
                List.copyOf(biens),
                List.copyOf(dettes),
                synthese,
                bases,
                messages,
                countryNormalized
        );
    }

    private static void validateInput(RegimeCommunauteLegaleBeInput in) {
        if (in.dateMariage() == null) {
            throw new IllegalArgumentException("La date de mariage est obligatoire");
        }
        if (in.dateMariage().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de mariage ne peut pas être future");
        }
        if (in.contratMariageSigne() == null) {
            throw new IllegalArgumentException("L'indication de signature d'un contrat de mariage est obligatoire");
        }
        if (in.biens() == null || in.biens().isEmpty()) {
            throw new IllegalArgumentException("Au moins un bien doit être renseigné");
        }
        if (in.dettes() == null) {
            throw new IllegalArgumentException("La liste des dettes est obligatoire (peut être vide)");
        }
        for (RegimeCommunauteLegaleBeInput.BienItem b : in.biens()) {
            if (b == null) {
                throw new IllegalArgumentException("Un bien ne peut pas être nul");
            }
            if (b.libelle() == null || b.libelle().isBlank()) {
                throw new IllegalArgumentException("Le libellé d'un bien est obligatoire");
            }
            if (b.libelle().length() > MAX_LIBELLE) {
                throw new IllegalArgumentException(
                        "Le libellé d'un bien ne peut dépasser " + MAX_LIBELLE + " caractères");
            }
            if (b.categorie() == null) {
                throw new IllegalArgumentException("La catégorie d'un bien est obligatoire");
            }
            validateCommentaire(b.commentaire(), "commentaire d'un bien");
        }
        for (RegimeCommunauteLegaleBeInput.DetteItem d : in.dettes()) {
            if (d == null) {
                throw new IllegalArgumentException("Une dette ne peut pas être nulle");
            }
            if (d.libelle() == null || d.libelle().isBlank()) {
                throw new IllegalArgumentException("Le libellé d'une dette est obligatoire");
            }
            if (d.libelle().length() > MAX_LIBELLE) {
                throw new IllegalArgumentException(
                        "Le libellé d'une dette ne peut dépasser " + MAX_LIBELLE + " caractères");
            }
            validateCommentaire(d.commentaire(), "commentaire d'une dette");
        }
    }

    private static void validateCommentaire(String commentaire, String champ) {
        if (commentaire != null && commentaire.length() > MAX_COMMENTAIRE) {
            throw new IllegalArgumentException(
                    "Le champ " + champ + " ne peut dépasser " + MAX_COMMENTAIRE + " caractères");
        }
    }

    /**
     * Qualifie un bien selon les règles du Livre 3 du Code civil belge.
     *
     * <ul>
     *   <li>Critères de propre : antériorité au mariage, donation/succession, outil de
     *       travail strictement personnel. Deux origines de propre exclusives cochées
     *       en même temps (antériorité + donation/succession) → {@code INDETERMINE}.</li>
     *   <li>Aucun critère de propre coché → {@code COMMUN} (acquêt — présomption de
     *       communauté pendant le mariage).</li>
     *   <li>Bien propre dont le financement par fonds propres est explicitement nié
     *       ({@code financeParFondsPropres = false}) → {@code MIXTE_RECOMPENSE} :
     *       financé par des fonds communs, il ouvre droit à récompense.</li>
     * </ul>
     */
    private static RegimeCommunauteLegaleBeResult.BienQualifie qualifierBien(
            RegimeCommunauteLegaleBeInput.BienItem b) {
        boolean anterieur = Boolean.TRUE.equals(b.acquisAvantMariage());
        boolean donation = Boolean.TRUE.equals(b.acquisParDonationOuSuccession());
        boolean outilPersonnel = Boolean.TRUE.equals(b.outilDeTravailPersonnel());
        boolean propreParOrigine = anterieur || donation || outilPersonnel;

        // Critères contradictoires : antériorité au mariage ET donation/succession
        // sont des origines mutuellement exclusives — impossible de trancher.
        if (anterieur && donation) {
            return new RegimeCommunauteLegaleBeResult.BienQualifie(
                    b.libelle(),
                    QualificationBienBe.INDETERMINE,
                    ModeGestionBe.GESTION_EXCLUSIVE,
                    FOND_BIEN_PROPRE,
                    "Critères de qualification contradictoires : le bien est déclaré à la "
                            + "fois acquis avant le mariage et reçu par donation/succession. "
                            + "L'origine du bien doit être tranchée avant de le qualifier.");
        }

        if (!propreParOrigine) {
            // Acquêt : bien acquis à titre onéreux pendant le mariage.
            ModeGestionBe gestion = modeGestionBienCommun(b);
            return new RegimeCommunauteLegaleBeResult.BienQualifie(
                    b.libelle(),
                    QualificationBienBe.COMMUN,
                    gestion,
                    FOND_BIEN_COMMUN + " ; " + fondementGestion(gestion),
                    "Bien acquis à titre onéreux pendant le mariage : il entre dans le "
                            + "patrimoine commun (acquêt). " + explicationGestion(gestion));
        }

        // Bien propre par origine. Si le financement par fonds propres est explicitement
        // nié, le bien a été financé par des fonds communs → récompense due à la communauté.
        if (Boolean.FALSE.equals(b.financeParFondsPropres())) {
            return new RegimeCommunauteLegaleBeResult.BienQualifie(
                    b.libelle(),
                    QualificationBienBe.MIXTE_RECOMPENSE,
                    ModeGestionBe.GESTION_EXCLUSIVE,
                    FOND_BIEN_MIXTE + " ; " + FOND_GESTION_EXCLUSIVE,
                    "Bien propre par son origine mais financé par des fonds communs : il "
                            + "reste propre, mais le patrimoine commun a droit à une récompense. "
                            + "Le bien reste géré exclusivement par l'époux propriétaire.");
        }

        return new RegimeCommunauteLegaleBeResult.BienQualifie(
                b.libelle(),
                QualificationBienBe.PROPRE,
                ModeGestionBe.GESTION_EXCLUSIVE,
                FOND_BIEN_PROPRE + " ; " + FOND_GESTION_EXCLUSIVE,
                explicationPropre(anterieur, donation, outilPersonnel)
                        + " Le bien est géré exclusivement par l'époux propriétaire.");
    }

    /**
     * Détermine le mode de gestion d'un bien commun :
     * <ul>
     *   <li>immobilier → {@code COGESTION} (acte grave : accord des deux époux) ;</li>
     *   <li>affecté à la profession d'un époux → {@code GESTION_EXCLUSIVE} ;</li>
     *   <li>sinon → {@code GESTION_CONCURRENTE}.</li>
     * </ul>
     */
    private static ModeGestionBe modeGestionBienCommun(RegimeCommunauteLegaleBeInput.BienItem b) {
        if (b.categorie() == BienCategorieBe.IMMOBILIER) {
            return ModeGestionBe.COGESTION;
        }
        if (Boolean.TRUE.equals(b.affectationProfessionnelle())) {
            return ModeGestionBe.GESTION_EXCLUSIVE;
        }
        return ModeGestionBe.GESTION_CONCURRENTE;
    }

    private static String fondementGestion(ModeGestionBe gestion) {
        return switch (gestion) {
            case GESTION_CONCURRENTE -> FOND_GESTION_CONCURRENTE;
            case COGESTION -> FOND_COGESTION;
            case GESTION_EXCLUSIVE -> FOND_GESTION_EXCLUSIVE;
        };
    }

    private static String explicationGestion(ModeGestionBe gestion) {
        return switch (gestion) {
            case GESTION_CONCURRENTE -> "Gestion concurrente : chaque époux peut accomplir "
                    + "seul les actes de gestion courante.";
            case COGESTION -> "S'agissant d'un acte grave (bien immobilier), la vente ou la "
                    + "donation requièrent l'accord des deux époux (cogestion).";
            case GESTION_EXCLUSIVE -> "Bien commun affecté à la profession d'un époux : il est "
                    + "géré exclusivement par l'époux qui exerce cette profession.";
        };
    }

    private static String explicationPropre(boolean anterieur, boolean donation, boolean outil) {
        if (anterieur) {
            return "Bien acquis avant le mariage : il reste propre à l'époux qui le détenait.";
        }
        if (donation) {
            return "Bien reçu par donation ou succession : il reste propre à l'époux "
                    + "gratifié ou héritier.";
        }
        if (outil) {
            return "Outil de travail strictement personnel : il est propre par affectation "
                    + "à l'époux qui l'exploite.";
        }
        return "Bien propre par son origine.";
    }

    /**
     * Qualifie une dette selon les règles du Livre 3 du Code civil belge.
     *
     * <ul>
     *   <li>dette antérieure au mariage → {@code DETTE_PROPRE} ;</li>
     *   <li>dette contractée par un seul époux hors intérêt du ménage →
     *       {@code DETTE_PROPRE_AVEC_RECOURS} ;</li>
     *   <li>sinon (intérêt du ménage ou aucun critère discriminant) →
     *       {@code DETTE_COMMUNE} (présomption de communauté pendant le mariage).</li>
     * </ul>
     */
    private static RegimeCommunauteLegaleBeResult.DetteQualifiee qualifierDette(
            RegimeCommunauteLegaleBeInput.DetteItem d) {
        boolean anterieure = Boolean.TRUE.equals(d.anterieureAuMariage());
        boolean interetMenage = Boolean.TRUE.equals(d.contracteeDansLInteretDuMenage());
        boolean unSeulEpoux = Boolean.TRUE.equals(d.contracteeParUnSeulEpoux());

        if (anterieure) {
            return new RegimeCommunauteLegaleBeResult.DetteQualifiee(
                    d.libelle(),
                    QualificationDetteBe.DETTE_PROPRE,
                    FOND_DETTE_PROPRE,
                    "Dette antérieure au mariage : elle est propre à l'époux qui l'a "
                            + "contractée et n'engage pas le patrimoine commun.");
        }
        if (unSeulEpoux && !interetMenage) {
            return new RegimeCommunauteLegaleBeResult.DetteQualifiee(
                    d.libelle(),
                    QualificationDetteBe.DETTE_PROPRE_AVEC_RECOURS,
                    FOND_DETTE_PROPRE_RECOURS,
                    "Dette contractée par un seul époux en dehors de l'intérêt du ménage : "
                            + "elle est propre, mais le créancier peut poursuivre les biens "
                            + "communs, à charge de récompense au profit de la communauté.");
        }
        return new RegimeCommunauteLegaleBeResult.DetteQualifiee(
                d.libelle(),
                QualificationDetteBe.DETTE_COMMUNE,
                FOND_DETTE_COMMUNE,
                interetMenage
                        ? "Dette contractée dans l'intérêt du ménage : elle est commune et "
                                + "engage le patrimoine commun."
                        : "Aucun critère ne permet de qualifier la dette de propre : la "
                                + "présomption de communauté pendant le mariage s'applique, "
                                + "la dette est commune.");
    }

    /**
     * Verdict global de composition :
     * <ul>
     *   <li>contrat de mariage signé → {@code REGIME_CONVENTIONNEL_DETECTE} ;</li>
     *   <li>sinon, au moins un bien {@code INDETERMINE} → {@code QUALIFICATION_INCOMPLETE} ;</li>
     *   <li>sinon → {@code COMMUNAUTE_LEGALE_APPLICABLE}.</li>
     * </ul>
     */
    private static RegimeCommunauteLegaleBeVerdict verdictPour(
            RegimeCommunauteLegaleBeInput in,
            List<RegimeCommunauteLegaleBeResult.BienQualifie> biens) {
        if (Boolean.TRUE.equals(in.contratMariageSigne())) {
            return RegimeCommunauteLegaleBeVerdict.REGIME_CONVENTIONNEL_DETECTE;
        }
        boolean indetermine = biens.stream()
                .anyMatch(b -> b.qualification() == QualificationBienBe.INDETERMINE);
        if (indetermine) {
            return RegimeCommunauteLegaleBeVerdict.QUALIFICATION_INCOMPLETE;
        }
        return RegimeCommunauteLegaleBeVerdict.COMMUNAUTE_LEGALE_APPLICABLE;
    }

    private static RegimeCommunauteLegaleBeResult.SyntheseComposition synthese(
            List<RegimeCommunauteLegaleBeResult.BienQualifie> biens,
            List<RegimeCommunauteLegaleBeResult.DetteQualifiee> dettes) {
        int communs = 0;
        int propres = 0;
        int mixtes = 0;
        for (RegimeCommunauteLegaleBeResult.BienQualifie b : biens) {
            switch (b.qualification()) {
                case COMMUN -> communs++;
                case PROPRE -> propres++;
                case MIXTE_RECOMPENSE -> mixtes++;
                case INDETERMINE -> { /* non compté — qualification non tranchée */ }
            }
        }
        int dettesCommunes = 0;
        int dettesPropres = 0;
        for (RegimeCommunauteLegaleBeResult.DetteQualifiee d : dettes) {
            switch (d.qualification()) {
                case DETTE_COMMUNE -> dettesCommunes++;
                case DETTE_PROPRE, DETTE_PROPRE_AVEC_RECOURS -> dettesPropres++;
            }
        }
        return new RegimeCommunauteLegaleBeResult.SyntheseComposition(
                communs, propres, mixtes, dettesCommunes, dettesPropres);
    }

    private static List<String> basesJuridiques(
            List<RegimeCommunauteLegaleBeResult.BienQualifie> biens,
            List<RegimeCommunauteLegaleBeResult.DetteQualifiee> dettes) {
        Set<String> bases = new LinkedHashSet<>();
        for (RegimeCommunauteLegaleBeResult.BienQualifie b : biens) {
            for (String f : b.fondement().split(" ; ")) {
                if (!f.isBlank()) {
                    bases.add(f.trim());
                }
            }
        }
        for (RegimeCommunauteLegaleBeResult.DetteQualifiee d : dettes) {
            if (d.fondement() != null && !d.fondement().isBlank()) {
                bases.add(d.fondement().trim());
            }
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(
            RegimeCommunauteLegaleBeInput in,
            List<RegimeCommunauteLegaleBeResult.BienQualifie> biens,
            RegimeCommunauteLegaleBeVerdict verdict) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case COMMUNAUTE_LEGALE_APPLICABLE -> msgs.add(
                    "Aucun contrat de mariage : le régime légal de communauté "
                            + "(loi du 22/07/2018) s'applique de plein droit.");
            case REGIME_CONVENTIONNEL_DETECTE -> msgs.add(
                    "Un contrat de mariage est signé : la qualification ci-dessous est "
                            + "fournie à titre indicatif sur la base du régime légal — le "
                            + "contrat de mariage peut y déroger et doit être confronté à "
                            + "cette analyse.");
            case QUALIFICATION_INCOMPLETE -> msgs.add(
                    "Un ou plusieurs biens présentent des critères contradictoires : la "
                            + "qualification ne peut être tranchée tant que leur origine "
                            + "exacte n'est pas précisée.");
        }
        long nbIndetermines = biens.stream()
                .filter(b -> b.qualification() == QualificationBienBe.INDETERMINE)
                .count();
        if (nbIndetermines > 0) {
            msgs.add("Biens à requalifier (critères contradictoires) : " + nbIndetermines
                    + " — préciser l'origine de chaque bien concerné.");
        }
        if (biens.stream().anyMatch(b -> b.qualification() == QualificationBienBe.MIXTE_RECOMPENSE)) {
            msgs.add("Un ou plusieurs biens propres ont été financés par des fonds communs : "
                    + "un calcul de récompense entre patrimoine commun et propre devra être "
                    + "mené séparément.");
        }
        return msgs;
    }
}

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-06 : moteur décisionnel BE qualifiant le <b>régime matrimonial de
 * séparation de biens belge</b> (Livre 3 du Code civil ; loi du 22/07/2018 —
 * à vérifier par avocat belge, renumérotation CC post-réformes 2017-2019).
 *
 * <p><b>Arbre, pas calcul.</b> L'outil qualifie la VARIANTE du régime de
 * séparation et ses effets patrimoniaux : (a) séparation pure, (b) séparation
 * avec adjonction d'une société d'acquêts, (c) séparation avec clause de
 * participation aux acquêts (créance de participation, loi 2018), et le jeu du
 * correctif équitable introduit en 2018 (correction judiciaire en équité en cas
 * de disproportion patrimoniale alléguée). Aucune citation jurisprudentielle
 * (F-JU-04 parké — silence &gt; erreur).</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la situation cadrée est le
 * <i>régime de séparation de biens BE et ses variantes</i>. DISTINCT de la
 * communauté légale belge ({@code regime-mat-be-communaute-legale}, F-217) et de
 * la liquidation-partage notariale chiffrée ({@code liquidation-partage-be},
 * F-217) vers laquelle l'outil RENVOIE pour tout chiffrage de la créance de
 * participation ou du partage des acquêts.</p>
 *
 * <p>Logique du verdict (4 niveaux) :</p>
 * <ul>
 *   <li><b>SEPARATION_PURE_QUALIFIEE</b> — chaque époux conserve la propriété
 *       et la gestion de ses biens ; aucune masse commune.</li>
 *   <li><b>SOCIETE_ACQUETS_QUALIFIEE</b> — une société d'acquêts est adjointe :
 *       masse limitée d'acquêts à partager (renvoi liquidation-partage-be).</li>
 *   <li><b>PARTICIPATION_ACQUETS_QUALIFIEE</b> — clause de participation aux
 *       acquêts : créance de participation calculable à la dissolution
 *       (loi 2018 ; chiffrage renvoyé à liquidation-partage-be).</li>
 *   <li><b>QUALIFICATION_INCOMPLETE</b> — variante ou éléments insuffisants.</li>
 * </ul>
 *
 * <p><b>Validation juridique requise</b> : les bases citées (Livre 3 CC ; loi
 * du 22/07/2018) sont à <b>valider par un avocat belge avant production</b>
 * (renumérotation CC post-réformes 2017-2019). Aucune citation jurisprudentielle
 * (F-JU-04 parké).</p>
 */
public final class RegimeBeSeparationBiensCalculator {

    /** Variante du régime de séparation de biens. */
    public enum VarianteRegime {
        SEPARATION_PURE,
        SEPARATION_AVEC_SOCIETE_ACQUETS,
        SEPARATION_AVEC_PARTICIPATION_ACQUETS
    }

    /** Verdict de l'analyse (4 niveaux). */
    public enum RegimeBeSeparationBiensVerdict {
        SEPARATION_PURE_QUALIFIEE,
        SOCIETE_ACQUETS_QUALIFIEE,
        PARTICIPATION_ACQUETS_QUALIFIEE,
        QUALIFICATION_INCOMPLETE
    }

    private RegimeBeSeparationBiensCalculator() {}

    /**
     * Applique l'arbre décisionnel BE du régime de séparation de biens.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, motifs, effets patrimoniaux, bases
     *         juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static RegimeBeSeparationBiensResult compute(
            RegimeBeSeparationBiensInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — régime de séparation de biens");
        }
        validateInputs(input);

        VarianteRegime variante = input.varianteRegime();

        // Rappel transversal : chiffrage du partage / de la créance vers F-217.
        final String renvoiLiquidation =
                "Pour le chiffrage du partage des acquêts ou de la créance de participation, "
                        + "s'appuyer sur l'outil liquidation-partage-be (F-217) : le présent outil "
                        + "qualifie le régime et ses effets, sans calculer le partage notarial.";

        // Correctif équitable 2018 : à signaler si disproportion alléguée.
        boolean correctifEquitable = Boolean.TRUE.equals(input.disproportionPatrimonialeAllegee());
        String mentionCorrectif = correctifEquitable
                ? "Disproportion patrimoniale alléguée : la loi du 22/07/2018 a introduit la "
                        + "possibilité d'une correction judiciaire en équité au profit de l'époux "
                        + "économiquement défavorisé (à signaler, sans chiffrage — à vérifier par "
                        + "avocat belge)."
                : null;

        switch (variante) {
            case SEPARATION_PURE:
                return separationPure(input, correctifEquitable, mentionCorrectif, renvoiLiquidation);
            case SEPARATION_AVEC_SOCIETE_ACQUETS:
                return societeAcquets(input, correctifEquitable, mentionCorrectif, renvoiLiquidation);
            case SEPARATION_AVEC_PARTICIPATION_ACQUETS:
                return participationAcquets(input, correctifEquitable, mentionCorrectif, renvoiLiquidation);
            default:
                // Sécurité (théoriquement inatteignable — variante validée plus haut).
                return new RegimeBeSeparationBiensResult(
                        RegimeBeSeparationBiensVerdict.QUALIFICATION_INCOMPLETE,
                        List.of("Compléter la variante du régime de séparation de biens pour orienter "
                                + "la qualification."),
                        "Effets patrimoniaux à instruire une fois la variante qualifiée.",
                        basesJuridiques(),
                        List.of("Éléments insuffisants pour qualifier le régime. " + renvoiLiquidation));
        }
    }

    // ---------------------------------------------------------------
    // Variantes
    // ---------------------------------------------------------------

    private static RegimeBeSeparationBiensResult separationPure(
            RegimeBeSeparationBiensInput in, boolean correctif, String mentionCorrectif,
            String renvoi) {
        List<String> motifs = new ArrayList<>();
        motifs.add("Séparation de biens pure : chaque époux conserve la propriété et la gestion "
                + "exclusives de ses biens propres et de ses acquêts ; il n'existe aucune masse "
                + "commune (Livre 3 CC — à vérifier par avocat belge).");
        motifs.add(Boolean.TRUE.equals(in.contratMariageNotarie())
                ? "Un contrat de mariage notarié est mentionné : il fige le choix du régime de "
                        + "séparation pure (forme authentique requise — à vérifier)."
                : "Aucun contrat de mariage notarié n'est documenté : la séparation de biens "
                        + "suppose un contrat notarié (à vérifier — le régime légal supplétif est la "
                        + "communauté).");
        if (correctif) {
            motifs.add(mentionCorrectif);
        }
        List<String> messages = new ArrayList<>();
        messages.add("Séparation de biens pure : pas de partage d'acquêts ni de créance de "
                + "participation (à vérifier par avocat belge). " + renvoi);
        if (correctif) {
            messages.add(mentionCorrectif);
        }
        return new RegimeBeSeparationBiensResult(
                RegimeBeSeparationBiensVerdict.SEPARATION_PURE_QUALIFIEE,
                motifs,
                "Aucune masse commune : chaque époux conserve l'intégralité de ses biens. "
                        + (correctif
                                ? "Correctif équitable 2018 mobilisable si disproportion établie "
                                        + "(à vérifier)."
                                : "Effets patrimoniaux limités aux biens propres de chaque époux."),
                basesJuridiques(),
                messages);
    }

    private static RegimeBeSeparationBiensResult societeAcquets(
            RegimeBeSeparationBiensInput in, boolean correctif, String mentionCorrectif,
            String renvoi) {
        List<String> motifs = new ArrayList<>();
        motifs.add("Séparation de biens avec adjonction d'une société d'acquêts : le régime reste "
                + "séparatiste pour les biens propres, mais une masse limitée d'acquêts est "
                + "constituée et partagée à la dissolution (Livre 3 CC — à vérifier par avocat "
                + "belge).");
        motifs.add("La société d'acquêts adjointe fonctionne comme une communauté restreinte aux "
                + "acquêts désignés au contrat : le partage de cette masse suit les règles "
                + "conventionnelles (à vérifier).");
        if (correctif) {
            motifs.add(mentionCorrectif);
        }
        List<String> messages = new ArrayList<>();
        messages.add("Société d'acquêts adjointe : masse limitée d'acquêts à partager — chiffrage "
                + "renvoyé à liquidation-partage-be (F-217). " + renvoi);
        if (correctif) {
            messages.add(mentionCorrectif);
        }
        return new RegimeBeSeparationBiensResult(
                RegimeBeSeparationBiensVerdict.SOCIETE_ACQUETS_QUALIFIEE,
                motifs,
                "Masse limitée d'acquêts à partager (les biens propres restent hors partage). "
                        + (correctif
                                ? "Correctif équitable 2018 mobilisable si disproportion établie "
                                        + "(à vérifier)."
                                : "Partage chiffré renvoyé à liquidation-partage-be."),
                basesJuridiques(),
                messages);
    }

    private static RegimeBeSeparationBiensResult participationAcquets(
            RegimeBeSeparationBiensInput in, boolean correctif, String mentionCorrectif,
            String renvoi) {
        List<String> motifs = new ArrayList<>();
        motifs.add("Séparation de biens avec clause de participation aux acquêts : pendant le "
                + "mariage le régime est séparatiste, mais à la dissolution une CRÉANCE DE "
                + "PARTICIPATION est due par l'époux dont les acquêts ont le plus augmenté "
                + "(loi du 22/07/2018 — à vérifier par avocat belge).");
        boolean patrimoinesConnus = in.patrimoinePropreEpoux1Eur() != null
                && in.patrimoinePropreEpoux2Eur() != null;
        motifs.add(patrimoinesConnus
                ? "Les patrimoines propres des deux époux sont documentés : la créance de "
                        + "participation est calculable à la dissolution (le chiffrage relève de "
                        + "liquidation-partage-be)."
                : "Les patrimoines propres ne sont pas tous documentés : la créance de "
                        + "participation reste due, mais devra être chiffrée à partir des patrimoines "
                        + "originaire et final de chaque époux.");
        if (correctif) {
            motifs.add(mentionCorrectif);
        }
        List<String> messages = new ArrayList<>();
        messages.add("Clause de participation aux acquêts : créance de participation à liquider à "
                + "la dissolution — chiffrage renvoyé à liquidation-partage-be (F-217). " + renvoi);
        if (correctif) {
            messages.add(mentionCorrectif);
        }
        return new RegimeBeSeparationBiensResult(
                RegimeBeSeparationBiensVerdict.PARTICIPATION_ACQUETS_QUALIFIEE,
                motifs,
                "Créance de participation due à la dissolution au profit de l'époux dont les "
                        + "acquêts ont le moins augmenté. "
                        + (correctif
                                ? "Correctif équitable 2018 mobilisable en sus si disproportion "
                                        + "établie (à vérifier)."
                                : "Chiffrage de la créance renvoyé à liquidation-partage-be."),
                basesJuridiques(),
                messages);
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(RegimeBeSeparationBiensInput in) {
        if (in.varianteRegime() == null) {
            throw new IllegalArgumentException(
                    "La variante du régime est requise (SEPARATION_PURE / "
                            + "SEPARATION_AVEC_SOCIETE_ACQUETS / SEPARATION_AVEC_PARTICIPATION_ACQUETS)");
        }
        if (in.varianteRegime() == VarianteRegime.SEPARATION_AVEC_PARTICIPATION_ACQUETS
                && in.clauseParticipationPrevue() == null) {
            throw new IllegalArgumentException(
                    "La présence d'une clause de participation aux acquêts doit être précisée "
                            + "pour la variante SEPARATION_AVEC_PARTICIPATION_ACQUETS");
        }
        if (in.dateContrat() != null && in.dateContrat().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date du contrat ne peut pas être dans le futur");
        }
        if (in.patrimoinePropreEpoux1Eur() != null && in.patrimoinePropreEpoux1Eur() < 0d) {
            throw new IllegalArgumentException(
                    "Le patrimoine propre de l'époux 1 ne peut pas être négatif");
        }
        if (in.patrimoinePropreEpoux2Eur() != null && in.patrimoinePropreEpoux2Eur() < 0d) {
            throw new IllegalArgumentException(
                    "Le patrimoine propre de l'époux 2 ne peut pas être négatif");
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "Livre 3 du Code civil belge — régimes matrimoniaux, dont le régime de séparation "
                        + "de biens (à vérifier par avocat belge — renumérotation CC post-réformes "
                        + "2017-2019)",
                "Loi du 22/07/2018 modifiant le Code civil et divers dispositions en matière de "
                        + "droit des régimes matrimoniaux — correctifs équitables et clause de "
                        + "participation aux acquêts (à vérifier par avocat belge)",
                "Contrat de mariage notarié — choix du régime de séparation et de ses correctifs "
                        + "conventionnels (forme authentique requise — à vérifier par avocat belge)",
                "Renvoi liquidation-partage-be (F-217) — chiffrage du partage des acquêts et de la "
                        + "créance de participation (le présent outil qualifie le régime, sans "
                        + "calculer le partage notarial)");
    }
}

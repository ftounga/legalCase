package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-223-05 : moteur décisionnel BE qualifiant le sort en Belgique d'un
 * <b>mariage, d'un talaq ou d'une dot (mahr) relevant du droit algérien</b>
 * (CDIP — loi du 16/07/2004 ; Convention algéro-belge — à vérifier par avocat
 * belge).
 *
 * <p><b>Arbre, pas calcul.</b> L'outil cadre la SPÉCIFICITÉ ALGÉRIENNE :
 * conformité au fond au Code de la famille algérien (capacité, consentement,
 * absence d'empêchement), conformité à l'ordre public international belge, et
 * effets reconnus (mariage, dot / mahr, répudiation). Aucune citation
 * jurisprudentielle (F-JU-04 parké — silence &gt; erreur).</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation »</b> — la situation cadrée est le
 * <i>corridor algérien</i> (Code de la famille algérien, dot/mahr, Convention
 * bilatérale algéro-belge). DISTINCT de
 * {@code mariage-etranger-be-reconnaissance} (F-217), qui traite la mécanique
 * CDIP GÉNÉRALE de reconnaissance (dont le talaq) : pour cette mécanique
 * générale, l'outil RENVOIE vers F-217 et n'instruit que les éléments propres
 * au régime algérien.</p>
 *
 * <p>Logique du verdict (4 niveaux) :</p>
 * <ul>
 *   <li><b>RECONNAISSANCE_DE_PLEIN_DROIT</b> — mariage algérien consenti par les
 *       deux époux, sans atteinte à l'ordre public (pas de polygamie / mariage
 *       forcé) ; ou dot/mahr qualifiable comme simple effet patrimonial.</li>
 *   <li><b>RECONNAISSANCE_SOUS_CONDITIONS</b> — talaq algérien (renvoi méthode
 *       CDIP générale F-217 + spécificités Convention algéro-belge) ; ou
 *       éléments à compléter / vérifier (consentement, Convention invoquée).</li>
 *   <li><b>RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC</b> — atteinte à l'ordre public
 *       international belge (consentement vicié / mariage forcé attesté).</li>
 *   <li><b>QUALIFICATION_INCOMPLETE</b> — nature de l'acte ou rattachement
 *       insuffisants pour orienter.</li>
 * </ul>
 *
 * <p><b>Validation juridique requise</b> : les bases citées (Convention
 * algéro-belge, Code de la famille algérien, CDIP) sont à <b>valider par un
 * avocat belge avant production</b> (renumérotation CC post-réformes 2017-2019).
 * Aucune citation jurisprudentielle (F-JU-04 parké).</p>
 */
public final class RegimeAlgerienBeCalculator {

    /** Nature de l'acte algérien soumis à reconnaissance. */
    public enum NatureActe {
        MARIAGE_ALGERIEN,
        TALAQ_ALGERIEN,
        DOT_MAHR
    }

    /** Lien de rattachement de la situation à la Belgique. */
    public enum LienRattachement {
        RESIDENCE,
        NATIONALITE,
        AUCUN
    }

    /** Verdict de l'analyse (4 niveaux). */
    public enum RegimeAlgerienBeVerdict {
        RECONNAISSANCE_DE_PLEIN_DROIT,
        RECONNAISSANCE_SOUS_CONDITIONS,
        RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC,
        QUALIFICATION_INCOMPLETE
    }

    private RegimeAlgerienBeCalculator() {}

    /**
     * Applique l'arbre décisionnel BE du corridor algérien.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, motifs, effets de la dot, bases
     *         juridiques, messages)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static RegimeAlgerienBeResult compute(RegimeAlgerienBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        if (!"BELGIQUE".equals(country.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en BELGIQUE — régime algérien");
        }
        validateInputs(input);

        NatureActe nature = input.natureActe();
        LienRattachement rattachement = input.lienRattachementBelgique();

        // Rappel transversal : renvoi méthode CDIP générale vers F-217.
        final String renvoiF217 =
                "Pour la mécanique générale de reconnaissance au sens du CDIP (loi du 16/07/2004 — "
                        + "compétence indirecte, absence de fraude, contrôle de l'ordre public), s'appuyer "
                        + "sur l'outil mariage-etranger-be-reconnaissance (F-217) : le présent outil n'instruit "
                        + "que la spécificité algérienne (Code de la famille algérien, dot/mahr, Convention "
                        + "algéro-belge — à vérifier par avocat belge).";

        // --- (c) Dot / mahr : qualification de l'effet patrimonial ---
        if (nature == NatureActe.DOT_MAHR) {
            boolean montantPositif = input.montantDotConnu() != null && input.montantDotConnu() > 0d;
            List<String> motifs = new ArrayList<>();
            motifs.add("La dot (mahr) prévue par le Code de la famille algérien constitue une "
                    + "obligation patrimoniale au profit de l'épouse : en principe, elle peut être "
                    + "reconnue comme un simple effet pécuniaire du mariage, sans heurter en soi "
                    + "l'ordre public belge (à vérifier par avocat belge).");
            motifs.add(montantPositif
                    ? "Un montant de dot est documenté : la créance de mahr est qualifiable et peut "
                            + "être réclamée comme effet patrimonial du mariage."
                    : "Le montant de la dot n'est pas documenté : qualifier l'obligation reste "
                            + "possible, mais la créance devra être chiffrée pour produire ses effets.");
            if (input.conventionAlgeroBelgeInvoquee()) {
                motifs.add("Convention algéro-belge invoquée : vérifier ses stipulations propres aux "
                        + "effets patrimoniaux du mariage (à vérifier par avocat belge).");
            }
            return new RegimeAlgerienBeResult(
                    RegimeAlgerienBeVerdict.RECONNAISSANCE_DE_PLEIN_DROIT,
                    motifs,
                    "La dot (mahr) est qualifiée comme un EFFET PATRIMONIAL du mariage (créance de "
                            + "l'épouse) et non comme une condition contraire à l'ordre public belge "
                            + "(à vérifier par avocat belge).",
                    basesJuridiques(),
                    List.of("Dot / mahr algérien : effet patrimonial du mariage, reconnu comme créance "
                            + "de l'épouse (à vérifier par avocat belge). " + renvoiF217));
        }

        // --- Atteinte à l'ordre public : consentement vicié / mariage forcé ---
        boolean consentement = Boolean.TRUE.equals(input.consentementEpouxEpouse());
        if (nature == NatureActe.MARIAGE_ALGERIEN && input.consentementEpouxEpouse() != null && !consentement) {
            List<String> motifs = List.of(
                    "Le consentement libre des deux époux n'est pas établi : un mariage forcé / sans "
                            + "consentement heurte frontalement l'ordre public international belge et le "
                            + "Code de la famille algérien lui-même (capacité et consentement requis).",
                    "La reconnaissance du mariage algérien doit être refusée tant que le consentement "
                            + "libre des deux époux n'est pas attesté (à vérifier par avocat belge).");
            return new RegimeAlgerienBeResult(
                    RegimeAlgerienBeVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC,
                    motifs,
                    "Sans objet (mariage non reconnu) : la question de la dot ne se pose qu'après "
                            + "reconnaissance du mariage.",
                    basesJuridiques(),
                    List.of("Atteinte à l'ordre public international belge (consentement non établi / "
                            + "mariage forcé) : reconnaissance refusée. " + renvoiF217));
        }

        // --- (a) Mariage algérien : conformité fond + ordre public ---
        if (nature == NatureActe.MARIAGE_ALGERIEN) {
            boolean rattachementBelge = rattachement != LienRattachement.AUCUN;
            if (consentement && rattachementBelge) {
                List<String> motifs = new ArrayList<>();
                motifs.add("Mariage algérien consenti par les deux époux et rattaché à la Belgique "
                        + "(résidence / nationalité) : conformité au fond au Code de la famille algérien "
                        + "(capacité, consentement, absence d'empêchement — à vérifier) et absence "
                        + "d'atteinte à l'ordre public (ni polygamie ni mariage forcé attestés).");
                if (input.conventionAlgeroBelgeInvoquee()) {
                    motifs.add("Convention algéro-belge invoquée : ses stipulations sur la "
                            + "reconnaissance de l'état des personnes facilitent la reconnaissance "
                            + "(à vérifier par avocat belge).");
                }
                if (Boolean.TRUE.equals(input.dotMahrPrevue())) {
                    motifs.add("Une dot (mahr) est prévue : elle suit le sort du mariage comme effet "
                            + "patrimonial (utiliser la nature DOT_MAHR pour la qualifier précisément).");
                }
                return new RegimeAlgerienBeResult(
                        RegimeAlgerienBeVerdict.RECONNAISSANCE_DE_PLEIN_DROIT,
                        motifs,
                        Boolean.TRUE.equals(input.dotMahrPrevue())
                                ? "La dot (mahr) prévue est un effet patrimonial du mariage reconnu "
                                        + "(à qualifier via la nature DOT_MAHR)."
                                : "Aucune dot renseignée : effets patrimoniaux à instruire séparément "
                                        + "le cas échéant.",
                        basesJuridiques(),
                        List.of("Mariage algérien consenti et rattaché à la Belgique : reconnaissance "
                                + "de plein droit, sous réserve du contrôle de l'ordre public et de "
                                + "l'absence de fraude. " + renvoiF217));
            }
            // Consentement non renseigné OU pas de rattachement BE → sous conditions.
            List<String> motifs = new ArrayList<>();
            motifs.add(rattachementBelge
                    ? "Le consentement libre des deux époux n'est pas documenté : la reconnaissance "
                            + "du mariage algérien est subordonnée à la preuve de ce consentement "
                            + "(condition de fond du Code de la famille algérien + ordre public belge)."
                    : "Aucun lien de rattachement à la Belgique n'est établi : l'intérêt à faire "
                            + "reconnaître le mariage algérien en Belgique doit être caractérisé "
                            + "(résidence / nationalité) avant toute reconnaissance.");
            if (input.conventionAlgeroBelgeInvoquee()) {
                motifs.add("Convention algéro-belge invoquée : vérifier ses conditions propres de "
                        + "reconnaissance (à vérifier par avocat belge).");
            }
            return new RegimeAlgerienBeResult(
                    RegimeAlgerienBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS,
                    motifs,
                    "Effets patrimoniaux (dot/mahr) à instruire une fois le mariage reconnu.",
                    basesJuridiques(),
                    List.of("Mariage algérien : reconnaissance subordonnée à la preuve du consentement "
                            + "et/ou du rattachement à la Belgique. " + renvoiF217));
        }

        // --- (b) Talaq algérien : renvoi méthode CDIP générale F-217 + Convention ---
        if (nature == NatureActe.TALAQ_ALGERIEN) {
            List<String> motifs = new ArrayList<>();
            motifs.add("Le talaq (répudiation) prononcé selon le Code de la famille algérien relève de "
                    + "la mécanique GÉNÉRALE de reconnaissance des décisions étrangères en matière de "
                    + "divorce (CDIP — contrôle renforcé de l'ordre public, respect des droits de la "
                    + "défense de l'épouse, absence de répudiation unilatérale non contradictoire).");
            motifs.add("Spécificité algérienne : apprécier les garanties offertes à l'épouse par la "
                    + "procédure algérienne (présence, convocation, droits patrimoniaux) et le jeu "
                    + "éventuel de la Convention algéro-belge (à vérifier par avocat belge).");
            if (input.conventionAlgeroBelgeInvoquee()) {
                motifs.add("Convention algéro-belge invoquée : vérifier ses dispositions relatives à la "
                        + "reconnaissance des décisions de dissolution du mariage (à vérifier par "
                        + "avocat belge).");
            }
            return new RegimeAlgerienBeResult(
                    RegimeAlgerienBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS,
                    motifs,
                    "Sans objet (dissolution) : la dot acquise reste due comme effet patrimonial déjà "
                            + "constitué.",
                    basesJuridiques(),
                    List.of("Talaq algérien : la mécanique générale de reconnaissance CDIP est instruite "
                            + "par l'outil mariage-etranger-be-reconnaissance (F-217). Le présent outil "
                            + "n'ajoute que la spécificité algérienne (Convention bilatérale, garanties "
                            + "de l'épouse). " + renvoiF217));
        }

        // --- Sécurité : qualification incomplète (théoriquement inatteignable) ---
        return new RegimeAlgerienBeResult(
                RegimeAlgerienBeVerdict.QUALIFICATION_INCOMPLETE,
                List.of("Compléter les éléments (nature de l'acte, lien de rattachement à la Belgique) "
                        + "pour orienter la reconnaissance."),
                "Effets de la dot à instruire une fois la nature de l'acte qualifiée.",
                basesJuridiques(),
                List.of("Éléments insuffisants pour qualifier le sort de l'acte algérien. " + renvoiF217));
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    private static void validateInputs(RegimeAlgerienBeInput in) {
        if (in.natureActe() == null) {
            throw new IllegalArgumentException(
                    "La nature de l'acte est requise (MARIAGE_ALGERIEN / TALAQ_ALGERIEN / DOT_MAHR)");
        }
        if (in.lienRattachementBelgique() == null) {
            throw new IllegalArgumentException(
                    "Le lien de rattachement à la Belgique est requis (RESIDENCE / NATIONALITE / AUCUN)");
        }
        if (in.dateActe() != null && in.dateActe().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de l'acte ne peut pas être dans le futur");
        }
        if (in.montantDotConnu() != null && in.montantDotConnu() < 0d) {
            throw new IllegalArgumentException("Le montant de la dot ne peut pas être négatif");
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static List<String> basesJuridiques() {
        return List.of(
                "Code de droit international privé belge (CDIP — loi du 16/07/2004) — reconnaissance "
                        + "de l'état des personnes et des décisions étrangères, contrôle de l'ordre "
                        + "public international (à vérifier par avocat belge — renumérotation CC "
                        + "post-réformes 2017-2019)",
                "Convention algéro-belge (corridor bilatéral, à vérifier par avocat belge) — "
                        + "dispositions propres à la reconnaissance des actes et décisions algériens "
                        + "relatifs à l'état des personnes",
                "Code de la famille algérien — conditions de fond du mariage (capacité, consentement, "
                        + "absence d'empêchement) et régime de la dot (mahr) / du talaq (à vérifier par "
                        + "avocat belge)",
                "Renvoi mariage-etranger-be-reconnaissance (F-217) — mécanique CDIP générale de "
                        + "reconnaissance du mariage et du talaq étrangers (le présent outil n'instruit "
                        + "que la spécificité algérienne)");
    }
}

package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-222-03 : analyseur statique des conditions de l'habilitation familiale
 * (art. 494-1 à 494-12 Cciv), Famille FRANCE uniquement.
 *
 * <p>Conditions cumulatives propres de l'habilitation familiale :</p>
 * <ol>
 *   <li>altération des facultés (mentales ou corporelles) médicalement
 *       constatée empêchant l'expression de la volonté (art. 425 / 494-1) ;</li>
 *   <li>lien familial éligible (ascendant, descendant, frère/sœur, conjoint ou
 *       partenaire — art. 494-1) ;</li>
 *   <li>consensus familial : aucune opposition d'un proche (art. 494-1).</li>
 * </ol>
 *
 * <p>Verdict (3 niveaux) :</p>
 * <ul>
 *   <li><b>ELIGIBLE_HABILITATION_GENERALE</b> : conditions réunies + étendue
 *       GENERALE (art. 494-6) ;</li>
 *   <li><b>ELIGIBLE_HABILITATION_SPECIALE</b> : conditions réunies + étendue
 *       PONCTUELLE — un ou plusieurs actes déterminés (art. 494-1) ;</li>
 *   <li><b>ORIENTER_VERS_MESURE_JUDICIAIRE</b> : au moins une condition propre
 *       manque (pas d'altération constatée, lien inéligible ou pas de consensus)
 *       → orienter vers une mesure judiciaire de protection (F-FA-25).</li>
 * </ul>
 *
 * <p>Modalité (art. 494-1) : ASSISTANCE pour un besoin léger, REPRESENTATION pour
 * un besoin lourd (besoin d'actes patrimoniaux + personnels, ou habilitation
 * générale).</p>
 *
 * <p>L'outil <b>conseille</b> l'avocat ; le prononcé de l'habilitation relève du
 * juge des contentieux de la protection.</p>
 *
 * <p>Anti-doublon F-FA-25 (sélecteur de régime de protection) : cet analyseur ne
 * re-sélectionne pas le régime ; il cadre les conditions PROPRES de
 * l'habilitation familiale et renvoie vers F-FA-25 quand elles ne sont pas
 * réunies.</p>
 */
public final class HabilitationFamilialeCalculator {

    static final String BASE_494_1 = "art. 494-1 Cciv (habilitation familiale — conditions et personnes habilitées)";
    static final String BASE_494_6 = "art. 494-6 Cciv (habilitation familiale générale)";
    static final String BASE_425 = "art. 425 Cciv (altération des facultés médicalement constatée)";
    static final String BASE_F_FA_25 =
            "Mesure judiciaire de protection (sauvegarde de justice / curatelle / tutelle) — outil F-FA-25";

    static final String COND_ALTERATION =
            "Altération des facultés mentales ou corporelles médicalement constatée (art. 425 Cciv)";
    static final String COND_LIEN =
            "Lien familial éligible (ascendant, descendant, frère/sœur, conjoint ou partenaire — art. 494-1 Cciv)";
    static final String COND_CONSENSUS =
            "Consensus familial — absence d'opposition d'un proche (art. 494-1 Cciv)";

    static final String MSG_DECISION_JUGE =
            "Décision de prononcé : l'habilitation familiale est prononcée par le juge des contentieux de la "
                    + "protection (art. 494-1 et s. Cciv). Cet outil évalue les conditions et conseille l'avocat ; "
                    + "il ne prononce pas la mesure.";

    static final String MSG_RENVOI_F_FA_25 =
            "Les conditions propres de l'habilitation familiale ne sont pas réunies (altération non constatée, "
                    + "lien familial inéligible ou absence de consensus familial). En cas de conflit familial ou "
                    + "de lien inéligible, orienter vers une mesure judiciaire de protection — outil F-FA-25 "
                    + "(sauvegarde de justice / curatelle / tutelle).";

    private HabilitationFamilialeCalculator() {}

    /**
     * Analyse les conditions de l'habilitation familiale.
     *
     * @param req     requête validée (gate pays vérifié par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat de l'analyse
     * @throws IllegalArgumentException si les pré-requis pays ne sont pas respectés
     */
    public static HabilitationFamilialeResult compute(HabilitationFamilialeRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-HABILITATION-FAMILIALE applicable uniquement en France (art. 494-1 et s. Cciv).");
        }
        if (req == null) {
            throw new IllegalArgumentException("Requête habilitation familiale manquante.");
        }

        boolean alteration = Boolean.TRUE.equals(req.alterationFacultesMedicalementConstatee());
        LienFamilialHabilitationEnum lien = req.lienFamilialEligible();
        boolean lienEligible = lien != null && lien != LienFamilialHabilitationEnum.AUTRE;
        boolean consensus = Boolean.TRUE.equals(req.consensusFamilial());
        boolean actesPatrimoniaux = Boolean.TRUE.equals(req.besoinActesPatrimoniaux());
        boolean actesPersonnels = Boolean.TRUE.equals(req.besoinActesPersonnels());
        EtendueHabilitationEnum etendue = req.protectionPonctuelleOuGenerale();

        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<String> manquantes = new ArrayList<>();
        bases.add(BASE_494_1);

        if (!alteration) manquantes.add(COND_ALTERATION);
        if (!lienEligible) manquantes.add(COND_LIEN);
        if (!consensus) manquantes.add(COND_CONSENSUS);

        // Conditions propres non réunies → orienter vers mesure judiciaire (F-FA-25).
        if (!manquantes.isEmpty()) {
            bases.add(BASE_F_FA_25);
            messages.add(MSG_RENVOI_F_FA_25);
            messages.add(MSG_DECISION_JUGE);
            return new HabilitationFamilialeResult(
                    VerdictHabilitationFamilialeEnum.ORIENTER_VERS_MESURE_JUDICIAIRE,
                    null,
                    List.of(),
                    manquantes,
                    bases,
                    messages);
        }

        // Conditions réunies → éligible. Modalité + actes couverts + étendue.
        bases.add(BASE_425);
        ModaliteHabilitationEnum modalite =
                (actesPatrimoniaux && actesPersonnels) || etendue == EtendueHabilitationEnum.GENERALE
                        ? ModaliteHabilitationEnum.REPRESENTATION
                        : ModaliteHabilitationEnum.ASSISTANCE;

        List<String> actesCouverts = new ArrayList<>();
        if (actesPatrimoniaux) {
            actesCouverts.add("Actes patrimoniaux (gestion et disposition du patrimoine)");
        }
        if (actesPersonnels) {
            actesCouverts.add("Actes relatifs à la personne (santé, logement, vie personnelle)");
        }
        if (actesCouverts.isEmpty()) {
            actesCouverts.add("Aucun besoin d'acte renseigné — préciser l'étendue des actes à couvrir.");
        }

        VerdictHabilitationFamilialeEnum verdict;
        if (etendue == EtendueHabilitationEnum.GENERALE) {
            bases.add(BASE_494_6);
            verdict = VerdictHabilitationFamilialeEnum.ELIGIBLE_HABILITATION_GENERALE;
            messages.add("Les conditions de l'habilitation familiale sont réunies (altération constatée, lien "
                    + "familial éligible, consensus familial) et l'étendue demandée est générale : habilitation "
                    + "familiale GÉNÉRALE (art. 494-6 Cciv).");
        } else {
            verdict = VerdictHabilitationFamilialeEnum.ELIGIBLE_HABILITATION_SPECIALE;
            messages.add("Les conditions de l'habilitation familiale sont réunies (altération constatée, lien "
                    + "familial éligible, consensus familial) et l'étendue demandée est ponctuelle : habilitation "
                    + "familiale SPÉCIALE pour un ou plusieurs actes déterminés (art. 494-1 Cciv).");
        }
        messages.add("Modalité retenue : "
                + (modalite == ModaliteHabilitationEnum.REPRESENTATION ? "REPRÉSENTATION (besoin lourd)"
                        : "ASSISTANCE (besoin léger)") + ".");
        messages.add(MSG_DECISION_JUGE);

        return new HabilitationFamilialeResult(verdict, modalite, actesCouverts, manquantes, bases, messages);
    }
}

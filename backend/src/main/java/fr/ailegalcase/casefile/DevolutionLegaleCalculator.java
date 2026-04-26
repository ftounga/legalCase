package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-24-01 : calculateur de dévolution légale successorale (FR — art. 731
 * et s. Cciv) — détermine les héritiers et leurs quotes-parts à partir de la
 * composition familiale du défunt, en l'absence de testament.
 *
 * <p>Système des 4 ordres d'héritiers (chacun exclut le suivant sauf
 * représentation) :</p>
 * <ol>
 *   <li>Descendants (art. 734)</li>
 *   <li>Ascendants privilégiés (parents) + collatéraux privilégiés (frères/sœurs) (art. 738)</li>
 *   <li>Ascendants ordinaires (grands-parents) (art. 739)</li>
 *   <li>Collatéraux ordinaires jusqu'au 6ème degré (art. 740)</li>
 * </ol>
 *
 * <p>Spécial conjoint survivant (art. 757 et s.) :</p>
 * <ul>
 *   <li>Descendants tous communs : option ¼ pleine propriété OU usufruit total (757).</li>
 *   <li>Descendants non communs (recomposée) : ¼ pleine propriété obligatoire.</li>
 *   <li>Sans descendants, parents survivants : conjoint ½ + parents ½ (757-1).</li>
 *   <li>Sans descendants ni parents : conjoint exclut ordres 3 et 4 (757-2).</li>
 * </ul>
 *
 * <p>Représentation (art. 751-755) : descendants d'un héritier prédécédé prennent
 * sa place. Fente successorale (art. 746-749) : 50/50 paternelle/maternelle pour
 * les ascendants ordinaires.</p>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE (CC BE
 * art. 731+) suit un régime distinct (quotités, fente, réserve différentes) et
 * fait l'objet d'une feature jumelle au backlog (F-FA-24-BE).</p>
 */
public final class DevolutionLegaleCalculator {

    /** Option du conjoint survivant en présence de descendants tous communs. */
    public enum OptionConjoint {
        /** ¼ en pleine propriété (art. 757). */
        QUART,
        /** Usufruit sur la totalité de la succession (art. 757). */
        USUFRUIT
    }

    /** Ordre d'héritiers actif dans la dévolution. */
    public enum OrdreActif {
        /** Ordre 1 — descendants (art. 734). */
        DESCENDANTS,
        /** Ordre 2 — ascendants privilégiés + collatéraux privilégiés (art. 738). */
        PRIVILEGIES,
        /** Ordre 3 — ascendants ordinaires (art. 739). */
        ASCENDANTS_ORDINAIRES,
        /** Ordre 4 — collatéraux ordinaires (art. 740). */
        COLLATERAUX_ORDINAIRES,
        /** Conjoint seul (art. 757-2 — exclut ordres 3 et 4). */
        CONJOINT_SEUL,
        /** Aucun héritier — succession en déshérence (art. 768). */
        DESHERENCE
    }

    /** Qualité de l'héritier désigné. */
    public enum QualiteHeritier {
        CONJOINT,
        DESCENDANT,
        PERE,
        MERE,
        FRERE_SOEUR,
        ASCENDANT_ORDINAIRE_PATERNEL,
        ASCENDANT_ORDINAIRE_MATERNEL,
        COLLATERAL_ORDINAIRE,
        REPRESENTANT
    }

    /** Modalité de propriété (pleine, usufruit, nue-propriété). */
    public enum ModaliteHeritier {
        PLEINE_PROPRIETE,
        USUFRUIT,
        NUE_PROPRIETE
    }

    /** Héritier désigné par la dévolution avec sa quote-part. */
    public record HeritierDesigne(
            QualiteHeritier qualite,
            int ordre,
            double quotePartPct,
            ModaliteHeritier modalite
    ) {}

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 731, 734, 738, 745-749, 751-755, 757 et s. Cciv";

    private DevolutionLegaleCalculator() {}

    /**
     * Évalue la dévolution légale successorale.
     *
     * @return résultat structuré avec héritiers, quotes-parts, alertes
     * @throws IllegalArgumentException si paramètre invalide
     */
    public static DevolutionLegaleResult compute(Boolean conjointSurvivant,
                                                 Integer nbDescendants,
                                                 Boolean tousDescendantsCommunsAvecConjoint,
                                                 Integer nbDescendantsPredecedes,
                                                 Integer nbPetitsEnfantsParRepresentation,
                                                 Boolean pereVivant,
                                                 Boolean mereVivant,
                                                 Integer nbFreresSoeurs,
                                                 Integer nbFreresSoeursPredecedes,
                                                 Boolean ascendantsOrdinaires,
                                                 Boolean collateralOrdinaires,
                                                 OptionConjoint optionConjoint,
                                                 String country) {
        // ---- Validations strictes
        if (conjointSurvivant == null) {
            throw new IllegalArgumentException("Présence du conjoint survivant (oui/non) requise");
        }
        if (nbDescendants == null || nbDescendants < 0) {
            throw new IllegalArgumentException("Nombre de descendants doit être ≥ 0");
        }
        if (nbDescendantsPredecedes == null || nbDescendantsPredecedes < 0) {
            throw new IllegalArgumentException("Nombre de descendants prédécédés doit être ≥ 0");
        }
        if (nbPetitsEnfantsParRepresentation == null || nbPetitsEnfantsParRepresentation < 0) {
            throw new IllegalArgumentException("Nombre de petits-enfants par représentation doit être ≥ 0");
        }
        if (pereVivant == null) {
            throw new IllegalArgumentException("Père vivant (oui/non) requis");
        }
        if (mereVivant == null) {
            throw new IllegalArgumentException("Mère vivante (oui/non) requise");
        }
        if (nbFreresSoeurs == null || nbFreresSoeurs < 0) {
            throw new IllegalArgumentException("Nombre de frères/sœurs doit être ≥ 0");
        }
        if (nbFreresSoeursPredecedes == null || nbFreresSoeursPredecedes < 0) {
            throw new IllegalArgumentException("Nombre de frères/sœurs prédécédés doit être ≥ 0");
        }
        if (nbFreresSoeursPredecedes > nbFreresSoeurs) {
            throw new IllegalArgumentException(
                    "Frères/sœurs prédécédés ne peut excéder le total");
        }
        if (ascendantsOrdinaires == null) {
            throw new IllegalArgumentException("Présence ascendants ordinaires (oui/non) requise");
        }
        if (collateralOrdinaires == null) {
            throw new IllegalArgumentException("Présence collatéraux ordinaires (oui/non) requise");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC BE art. 731+ avec quotités différentes)"
                            + " sera traité dans une feature jumelle dédiée (F-FA-24-BE).");
        }
        // Vérifier valeur de tousDescendantsCommunsAvecConjoint requise si descendants + conjoint
        boolean hasDescendants = nbDescendants > 0;
        if (hasDescendants && conjointSurvivant && tousDescendantsCommunsAvecConjoint == null) {
            throw new IllegalArgumentException(
                    "Indication 'descendants tous communs avec le conjoint' (oui/non) "
                            + "requise lorsque le défunt laisse un conjoint et des descendants");
        }
        // Option conjoint requise si descendants TOUS communs + conjoint
        boolean tousCommuns = Boolean.TRUE.equals(tousDescendantsCommunsAvecConjoint);
        if (hasDescendants && conjointSurvivant && tousCommuns && optionConjoint == null) {
            throw new IllegalArgumentException(
                    "Option du conjoint requise (USUFRUIT ou QUART, art. 757) "
                            + "lorsque le défunt laisse un conjoint et des descendants tous communs");
        }

        List<HeritierDesigne> heritiers = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<String> risques = new ArrayList<>();

        OrdreActif ordreActif;
        double quotePartConjoint = 0.0;
        ModaliteHeritier modaliteConjoint = null;
        boolean representationActive = false;
        boolean fenteApplicable = false;

        // ============================================================
        // Aiguillage des 4 ordres + cas conjoint
        // ============================================================
        if (hasDescendants) {
            // ---- Ordre 1 : descendants (+ éventuel conjoint)
            ordreActif = OrdreActif.DESCENDANTS;
            DescendantsResult dr = computeOrdreDescendants(
                    conjointSurvivant, nbDescendants, tousCommuns,
                    nbDescendantsPredecedes, nbPetitsEnfantsParRepresentation,
                    optionConjoint, heritiers, messages, risques);
            quotePartConjoint = dr.quotePartConjoint;
            modaliteConjoint = dr.modaliteConjoint;
            representationActive = dr.representationActive;
        } else if (conjointSurvivant && (pereVivant || mereVivant || nbFreresSoeurs > 0)) {
            // ---- Ordre 2 : pas de descendants, conjoint + parents/fratrie
            ordreActif = OrdreActif.PRIVILEGIES;
            PrivilegiesResult pr = computeOrdrePrivilegies(
                    true, pereVivant, mereVivant,
                    nbFreresSoeurs, nbFreresSoeursPredecedes,
                    heritiers, messages, risques);
            quotePartConjoint = pr.quotePartConjoint;
            modaliteConjoint = pr.modaliteConjoint;
            representationActive = pr.representationActive;
        } else if (conjointSurvivant) {
            // ---- Conjoint seul (pas descendants, pas parents, pas fratrie)
            // Art. 757-2 : conjoint exclut ordres 3 et 4
            ordreActif = OrdreActif.CONJOINT_SEUL;
            heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 100.0,
                    ModaliteHeritier.PLEINE_PROPRIETE));
            quotePartConjoint = 100.0;
            modaliteConjoint = ModaliteHeritier.PLEINE_PROPRIETE;
            messages.add("Le conjoint survivant recueille la totalité de la succession "
                    + "(art. 757-2 Cciv) — il exclut les ascendants ordinaires (ordre 3) "
                    + "et les collatéraux ordinaires (ordre 4).");
        } else if (pereVivant || mereVivant || nbFreresSoeurs > 0) {
            // ---- Ordre 2 sans conjoint (parents et/ou fratrie)
            ordreActif = OrdreActif.PRIVILEGIES;
            PrivilegiesResult pr = computeOrdrePrivilegies(
                    false, pereVivant, mereVivant,
                    nbFreresSoeurs, nbFreresSoeursPredecedes,
                    heritiers, messages, risques);
            representationActive = pr.representationActive;
        } else if (ascendantsOrdinaires) {
            // ---- Ordre 3 : ascendants ordinaires (avec fente 50/50)
            ordreActif = OrdreActif.ASCENDANTS_ORDINAIRES;
            // Fente successorale art. 746-749 : 50% paternelle / 50% maternelle
            heritiers.add(new HeritierDesigne(
                    QualiteHeritier.ASCENDANT_ORDINAIRE_PATERNEL, 3, 50.0,
                    ModaliteHeritier.PLEINE_PROPRIETE));
            heritiers.add(new HeritierDesigne(
                    QualiteHeritier.ASCENDANT_ORDINAIRE_MATERNEL, 3, 50.0,
                    ModaliteHeritier.PLEINE_PROPRIETE));
            fenteApplicable = true;
            messages.add("Ordre 3 — ascendants ordinaires (art. 739 Cciv). "
                    + "Application de la FENTE SUCCESSORALE (art. 746-749) : "
                    + "50% ligne paternelle, 50% ligne maternelle.");
            risques.add("Fente successorale active — vérifier la composition de chaque "
                    + "ligne (paternelle / maternelle) pour répartir les 50% intra-ligne. "
                    + "En l'absence d'ascendant dans une ligne, la part revient à la ligne "
                    + "opposée (art. 749 Cciv).");
        } else if (collateralOrdinaires) {
            // ---- Ordre 4 : collatéraux ordinaires
            ordreActif = OrdreActif.COLLATERAUX_ORDINAIRES;
            heritiers.add(new HeritierDesigne(
                    QualiteHeritier.COLLATERAL_ORDINAIRE, 4, 100.0,
                    ModaliteHeritier.PLEINE_PROPRIETE));
            messages.add("Ordre 4 — collatéraux ordinaires (art. 740 Cciv) jusqu'au "
                    + "6ème degré. La part totale (100%) doit être répartie entre les "
                    + "collatéraux selon leur degré de parenté (proximité prime).");
            risques.add("Identifier nominativement les collatéraux ordinaires et leur "
                    + "degré (oncles/tantes degré 3, cousins germains degré 4, etc.). "
                    + "Au-delà du 6ème degré : succession en déshérence partielle.");
        } else {
            // ---- Aucun héritier identifié
            ordreActif = OrdreActif.DESHERENCE;
            messages.add("Succession en DÉSHÉRENCE (art. 768 Cciv) — aucun héritier "
                    + "identifié. La succession est dévolue à l'État (art. 539, 768).");
            risques.add("Vérifier l'absence d'héritiers connus avant déclaration de "
                    + "déshérence : recherche notariale exhaustive (généalogiste), "
                    + "publicité légale.");
        }

        // ============================================================
        // Risques contentieux transversaux
        // ============================================================
        if (hasDescendants && conjointSurvivant && !tousCommuns) {
            risques.add("Famille recomposée — descendants non issus de l'union avec "
                    + "le conjoint survivant : risque de contentieux entre enfants du "
                    + "premier lit et conjoint (art. 757). Possibilité d'action en retranchement "
                    + "(art. 1527 al. 2 Cciv) si avantages matrimoniaux excessifs.");
        }
        if (hasDescendants) {
            risques.add("Réserve héréditaire des descendants (art. 913 Cciv) — toute "
                    + "libéralité (testament/donation) excédant la quotité disponible "
                    + "ouvre une action en réduction (art. 920 et s.). Vérifier l'existence "
                    + "de testament ou donations antérieures.");
        }

        // ============================================================
        // Score d'éligibilité (cohérence des règles appliquées) — pour formule
        // ============================================================
        int score = computeScore(ordreActif, heritiers, representationActive, fenteApplicable);

        String formule = String.format(Locale.ROOT,
                "Ordre %s actif + conjoint=%s + descendants=%d (tous communs=%s) "
                        + "+ père=%s + mère=%s + frères/sœurs=%d (prédécédés=%d) "
                        + "+ ascendants ordinaires=%s + collatéraux ordinaires=%s "
                        + "+ option conjoint=%s + représentation=%s + fente=%s "
                        + "→ %d héritier(s) → score %d",
                ordreActif.name(), conjointSurvivant, nbDescendants, tousCommuns,
                pereVivant, mereVivant, nbFreresSoeurs, nbFreresSoeursPredecedes,
                ascendantsOrdinaires, collateralOrdinaires,
                optionConjoint == null ? "N/A" : optionConjoint.name(),
                representationActive, fenteApplicable, heritiers.size(), score);

        messages.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return new DevolutionLegaleResult(
                conjointSurvivant,
                nbDescendants,
                tousCommuns,
                nbDescendantsPredecedes,
                nbPetitsEnfantsParRepresentation,
                pereVivant,
                mereVivant,
                nbFreresSoeurs,
                nbFreresSoeursPredecedes,
                ascendantsOrdinaires,
                collateralOrdinaires,
                optionConjoint,
                countryNormalized,
                ordreActif,
                heritiers,
                quotePartConjoint,
                modaliteConjoint,
                representationActive,
                fenteApplicable,
                score,
                risques,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    /** Petit container pour le résultat de l'ordre 1. */
    private record DescendantsResult(double quotePartConjoint,
                                     ModaliteHeritier modaliteConjoint,
                                     boolean representationActive) {}

    private static DescendantsResult computeOrdreDescendants(boolean conjointSurvivant,
                                                             int nbDescendants,
                                                             boolean tousCommuns,
                                                             int nbDescendantsPredecedes,
                                                             int nbPetitsEnfantsParRepresentation,
                                                             OptionConjoint optionConjoint,
                                                             List<HeritierDesigne> heritiers,
                                                             List<String> messages,
                                                             List<String> risques) {
        double quotePartConjoint = 0.0;
        ModaliteHeritier modaliteConjoint = null;
        boolean representation = nbDescendantsPredecedes > 0
                && nbPetitsEnfantsParRepresentation > 0;

        if (conjointSurvivant) {
            // Spécial conjoint art. 757
            if (tousCommuns) {
                if (optionConjoint == OptionConjoint.USUFRUIT) {
                    // Usufruit total → conjoint 100% USUFRUIT, descendants 100% NUE_PROPRIETE
                    heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 100.0,
                            ModaliteHeritier.USUFRUIT));
                    quotePartConjoint = 100.0;
                    modaliteConjoint = ModaliteHeritier.USUFRUIT;
                    repartirDescendants(nbDescendants, 100.0,
                            nbDescendantsPredecedes, nbPetitsEnfantsParRepresentation,
                            ModaliteHeritier.NUE_PROPRIETE, heritiers);
                    messages.add("Conjoint survivant + descendants tous communs : option "
                            + "USUFRUIT TOTAL retenue (art. 757) — conjoint 100% en usufruit, "
                            + "descendants 100% en nue-propriété.");
                } else {
                    // QUART pleine propriété
                    heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 25.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                    quotePartConjoint = 25.0;
                    modaliteConjoint = ModaliteHeritier.PLEINE_PROPRIETE;
                    repartirDescendants(nbDescendants, 75.0,
                            nbDescendantsPredecedes, nbPetitsEnfantsParRepresentation,
                            ModaliteHeritier.PLEINE_PROPRIETE, heritiers);
                    messages.add("Conjoint survivant + descendants tous communs : option "
                            + "QUART PLEINE PROPRIÉTÉ retenue (art. 757) — conjoint 25%, "
                            + "descendants se partagent 75%.");
                }
            } else {
                // Descendants non communs : ¼ pleine propriété OBLIGATOIRE (option ignorée)
                heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 25.0,
                        ModaliteHeritier.PLEINE_PROPRIETE));
                quotePartConjoint = 25.0;
                modaliteConjoint = ModaliteHeritier.PLEINE_PROPRIETE;
                repartirDescendants(nbDescendants, 75.0,
                        nbDescendantsPredecedes, nbPetitsEnfantsParRepresentation,
                        ModaliteHeritier.PLEINE_PROPRIETE, heritiers);
                messages.add("Conjoint survivant + descendants NON tous communs (famille "
                        + "recomposée) : ¼ pleine propriété obligatoire pour le conjoint "
                        + "(art. 757) — option usufruit non disponible. Descendants 75%.");
            }
        } else {
            // Pas de conjoint : descendants se partagent 100%
            repartirDescendants(nbDescendants, 100.0,
                    nbDescendantsPredecedes, nbPetitsEnfantsParRepresentation,
                    ModaliteHeritier.PLEINE_PROPRIETE, heritiers);
            messages.add("Ordre 1 — descendants seuls (art. 734 Cciv) : "
                    + nbDescendants + " descendant(s) se partage(nt) 100%.");
        }

        if (representation) {
            messages.add("REPRÉSENTATION active (art. 751-755 Cciv) — "
                    + nbDescendantsPredecedes + " descendant(s) prédécédé(s) "
                    + "représenté(s) par " + nbPetitsEnfantsParRepresentation
                    + " petit(s)-enfant(s).");
        }

        return new DescendantsResult(quotePartConjoint, modaliteConjoint, representation);
    }

    /** Répartit la quote-part totale {@code totalPct} entre descendants vivants
     * et représentants. Souches égales : chaque descendant vivant + chaque
     * souche prédécédée représentée comptent comme une part. */
    private static void repartirDescendants(int nbDescendants,
                                            double totalPct,
                                            int nbDescendantsPredecedes,
                                            int nbPetitsEnfantsParRepresentation,
                                            ModaliteHeritier modalite,
                                            List<HeritierDesigne> heritiers) {
        int souches = nbDescendants; // total souches = vivants + prédécédés représentés
        // nbDescendants compte tous les enfants (vivants + prédécédés)
        // Cela suppose qu'on saisisse le total ; les prédécédés sortent de la ligne sauf représentation
        int nbDescendantsVivants = Math.max(0, nbDescendants - nbDescendantsPredecedes);
        if (nbDescendantsPredecedes > 0 && nbPetitsEnfantsParRepresentation > 0) {
            // Représentation active : chaque souche prédécédée compte pour une part,
            // partagée entre ses petits-enfants représentants.
            souches = nbDescendantsVivants + nbDescendantsPredecedes;
        } else {
            // Pas de représentation : seuls les vivants héritent
            souches = nbDescendantsVivants;
        }
        if (souches <= 0) {
            return;
        }
        double partParSouche = totalPct / souches;

        // Descendants vivants
        for (int i = 0; i < nbDescendantsVivants; i++) {
            heritiers.add(new HeritierDesigne(
                    QualiteHeritier.DESCENDANT, 1, partParSouche, modalite));
        }
        // Représentants (souches prédécédées subdivisées entre petits-enfants)
        if (nbDescendantsPredecedes > 0 && nbPetitsEnfantsParRepresentation > 0) {
            // Pour chaque souche prédécédée : la part de souche est divisée entre
            // les petits-enfants. On suppose une distribution uniforme.
            int petitsParSouche = Math.max(1,
                    nbPetitsEnfantsParRepresentation / nbDescendantsPredecedes);
            int petitsRestants = nbPetitsEnfantsParRepresentation;
            for (int souche = 0; souche < nbDescendantsPredecedes && petitsRestants > 0; souche++) {
                int n = Math.min(petitsParSouche, petitsRestants);
                if (souche == nbDescendantsPredecedes - 1) {
                    n = petitsRestants; // dernière souche prend le reste
                }
                if (n <= 0) continue;
                double partParPetit = partParSouche / n;
                for (int j = 0; j < n; j++) {
                    heritiers.add(new HeritierDesigne(
                            QualiteHeritier.REPRESENTANT, 1, partParPetit, modalite));
                }
                petitsRestants -= n;
            }
        }
    }

    /** Petit container pour le résultat de l'ordre 2. */
    private record PrivilegiesResult(double quotePartConjoint,
                                     ModaliteHeritier modaliteConjoint,
                                     boolean representationActive) {}

    private static PrivilegiesResult computeOrdrePrivilegies(boolean conjointSurvivant,
                                                             boolean pereVivant,
                                                             boolean mereVivant,
                                                             int nbFreresSoeurs,
                                                             int nbFreresSoeursPredecedes,
                                                             List<HeritierDesigne> heritiers,
                                                             List<String> messages,
                                                             List<String> risques) {
        double quotePartConjoint = 0.0;
        ModaliteHeritier modaliteConjoint = null;
        boolean representation = false;

        if (conjointSurvivant) {
            // Pas de descendants — conjoint + parents (art. 757-1)
            // ou conjoint exclut frères/sœurs (art. 757-2) si pas de parents
            int nbParentsVivants = (pereVivant ? 1 : 0) + (mereVivant ? 1 : 0);
            if (nbParentsVivants == 0) {
                // Conjoint exclut totalement frères/sœurs (art. 757-2)
                heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 100.0,
                        ModaliteHeritier.PLEINE_PROPRIETE));
                quotePartConjoint = 100.0;
                modaliteConjoint = ModaliteHeritier.PLEINE_PROPRIETE;
                messages.add("Conjoint survivant SANS descendants ni parents : "
                        + "le conjoint recueille la totalité de la succession (art. 757-2). "
                        + "Les frères/sœurs sont exclus de la dévolution légale.");
                if (nbFreresSoeurs > 0) {
                    risques.add("Frères/sœurs présents mais EXCLUS par le conjoint "
                            + "(art. 757-2 Cciv). Vérifier le régime des biens de famille "
                            + "(art. 757-3 — droit de retour des frères/sœurs sur les biens "
                            + "reçus du défunt par succession ou donation des parents).");
                }
            } else if (nbParentsVivants == 2) {
                // 2 parents + conjoint : conjoint 50% + parents 25% chacun (art. 757-1)
                heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 50.0,
                        ModaliteHeritier.PLEINE_PROPRIETE));
                quotePartConjoint = 50.0;
                modaliteConjoint = ModaliteHeritier.PLEINE_PROPRIETE;
                heritiers.add(new HeritierDesigne(QualiteHeritier.PERE, 2, 25.0,
                        ModaliteHeritier.PLEINE_PROPRIETE));
                heritiers.add(new HeritierDesigne(QualiteHeritier.MERE, 2, 25.0,
                        ModaliteHeritier.PLEINE_PROPRIETE));
                messages.add("Conjoint survivant + 2 parents (art. 757-1) : "
                        + "conjoint 50%, père 25%, mère 25%. Frères/sœurs sont exclus.");
            } else {
                // 1 seul parent : conjoint 75% + parent 25% (art. 757-1 al. 2)
                heritiers.add(new HeritierDesigne(QualiteHeritier.CONJOINT, 0, 75.0,
                        ModaliteHeritier.PLEINE_PROPRIETE));
                quotePartConjoint = 75.0;
                modaliteConjoint = ModaliteHeritier.PLEINE_PROPRIETE;
                if (pereVivant) {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.PERE, 2, 25.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                } else {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.MERE, 2, 25.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                }
                messages.add("Conjoint survivant + 1 parent (art. 757-1 al. 2) : "
                        + "conjoint 75%, parent 25%. Frères/sœurs sont exclus.");
            }
        } else {
            // Pas de conjoint — ordre 2 classique : parents + frères/sœurs
            int nbParentsVivants = (pereVivant ? 1 : 0) + (mereVivant ? 1 : 0);
            int nbFreresSoeursVivants = nbFreresSoeurs - nbFreresSoeursPredecedes;

            if (nbParentsVivants > 0 && nbFreresSoeurs > 0) {
                // Parents 1/4 chacun + frères/sœurs se partagent le reste
                double partParents = 25.0 * nbParentsVivants;
                double partFreres = 100.0 - partParents;
                if (pereVivant) {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.PERE, 2, 25.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                }
                if (mereVivant) {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.MERE, 2, 25.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                }
                repartirFreresSoeurs(nbFreresSoeursVivants, nbFreresSoeursPredecedes,
                        partFreres, heritiers);
                messages.add(String.format(Locale.ROOT,
                        "Ordre 2 sans conjoint — %d parent(s) et %d frère(s)/sœur(s) "
                                + "(art. 738 Cciv) : parents %.2f%% (25%% chacun), "
                                + "frères/sœurs %.2f%% à partager.",
                        nbParentsVivants, nbFreresSoeurs, partParents, partFreres));
            } else if (nbParentsVivants > 0) {
                // Parents seuls : 50/50 ou 100%
                if (nbParentsVivants == 2) {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.PERE, 2, 50.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                    heritiers.add(new HeritierDesigne(QualiteHeritier.MERE, 2, 50.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                } else if (pereVivant) {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.PERE, 2, 100.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                } else {
                    heritiers.add(new HeritierDesigne(QualiteHeritier.MERE, 2, 100.0,
                            ModaliteHeritier.PLEINE_PROPRIETE));
                }
                messages.add("Ordre 2 sans conjoint — parents seuls (art. 738) : "
                        + "100% partagés entre les parents survivants.");
            } else {
                // Frères/sœurs seuls : 100% partagés
                repartirFreresSoeurs(nbFreresSoeursVivants, nbFreresSoeursPredecedes,
                        100.0, heritiers);
                messages.add("Ordre 2 sans conjoint — frères/sœurs seuls (art. 738) : "
                        + "100% partagés entre les frères/sœurs.");
            }
            if (nbFreresSoeursPredecedes > 0) {
                representation = true;
                messages.add("REPRÉSENTATION (art. 752-2) : " + nbFreresSoeursPredecedes
                        + " frère(s)/sœur(s) prédécédé(s) — ses descendants neveux/nièces "
                        + "prennent sa part (à détailler nominativement).");
            }
        }

        return new PrivilegiesResult(quotePartConjoint, modaliteConjoint, representation);
    }

    private static void repartirFreresSoeurs(int nbFreresSoeursVivants,
                                             int nbFreresSoeursPredecedes,
                                             double totalPct,
                                             List<HeritierDesigne> heritiers) {
        int souches = nbFreresSoeursVivants
                + (nbFreresSoeursPredecedes > 0 ? nbFreresSoeursPredecedes : 0);
        if (souches <= 0) {
            return;
        }
        double partParSouche = totalPct / souches;
        for (int i = 0; i < nbFreresSoeursVivants; i++) {
            heritiers.add(new HeritierDesigne(
                    QualiteHeritier.FRERE_SOEUR, 2, partParSouche,
                    ModaliteHeritier.PLEINE_PROPRIETE));
        }
        for (int i = 0; i < nbFreresSoeursPredecedes; i++) {
            heritiers.add(new HeritierDesigne(
                    QualiteHeritier.REPRESENTANT, 2, partParSouche,
                    ModaliteHeritier.PLEINE_PROPRIETE));
        }
    }

    private static int computeScore(OrdreActif ordre,
                                    List<HeritierDesigne> heritiers,
                                    boolean representation,
                                    boolean fente) {
        int score = 0;
        if (ordre != OrdreActif.DESHERENCE) {
            score += 60;
        }
        if (!heritiers.isEmpty()) {
            score += 20;
        }
        if (representation) {
            score += 10;
        }
        if (fente) {
            score += 10;
        }
        return Math.min(score, 100);
    }
}

package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * SF-207-02 : calculateur de conformité du document C4 ONEM (formulaire de
 * fin de contrat employeur belge).
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>AR du 25 novembre 1991 portant réglementation du chômage,
 *       art. 92</b> — liste des mentions obligatoires du C4 ONEM.</li>
 *   <li><b>AR du 25 novembre 1991 art. 144</b> — sanction d'exclusion ONEM
 *       4 à 52 semaines en cas de licenciement pour faute grave.</li>
 *   <li><b>Loi du 3 juillet 1978 relative aux contrats de travail</b> —
 *       référence générale.</li>
 * </ul>
 *
 * <p>Logique :
 * <ol>
 *   <li>Vérifier les 10 mentions obligatoires (cf. {@link C4OnemChecklistMention}).</li>
 *   <li>Si {@code fauteGraveMentionnee=true} → verdict
 *       {@link C4OnemChecklistResult.Verdict#RISQUE_EXCLUSION_FAUTE_GRAVE} ;
 *       exclusionOnemRange 4-52 sem. ; etapeSuivante = CONTESTATION_C4 ;
 *       lettreRectificativeProposee = null (la rectification n'est pas la
 *       voie : il faut contester le C4 — outil SF-207-03 à venir).</li>
 *   <li>Sinon, si ≥ 1 mention manquante → verdict {@code NON_CONFORME} ;
 *       lettreRectificativeProposee non null ; etapeSuivante =
 *       RECTIFICATION_AUPRES_EMPLOYEUR.</li>
 *   <li>Sinon → verdict {@code CONFORME} ; etapeSuivante = AUCUNE.</li>
 * </ol>
 *
 * <p>Outil <b>BE-only</b> par construction — aucune logique FR (cf. mémoire
 * {@code feedback_belgique_never_forget}). L'équivalent FR est l'attestation
 * France Travail (R.1234-9) géré par {@link DocumentsFinContratCalculator},
 * qui répond à un régime juridiquement distinct.</p>
 */
public final class C4OnemChecklistCalculator {

    /** Fourchette d'exclusion ONEM en cas de faute grave reconnue. */
    public static final int EXCLUSION_ONEM_MIN_SEMAINES = 4;
    public static final int EXCLUSION_ONEM_MAX_SEMAINES = 52;

    /** Longueur minimale exigée pour un motif explicite (heuristique anti-mention vague). */
    public static final int MOTIF_EXPLICITE_LONGUEUR_MIN = 5;

    /** Format BCE : exactement 10 chiffres (Banque-Carrefour des Entreprises BE). */
    private static final String BCE_REGEX = "^[0-9]{10}$";

    /** Format NRN : exactement 11 chiffres (numéro national de registre). */
    private static final String NRN_REGEX = "^[0-9]{11}$";

    public static final String BASE_JURIDIQUE_COMPLET =
            "AR du 25 novembre 1991 art. 92 ; art. 144 (exclusion faute grave) ; loi du 3 juillet 1978";

    /** Libellés humains des mentions pour la lettre rectificative. */
    private static final Map<C4OnemChecklistMention, String> LIBELLES_MENTIONS =
            new EnumMap<>(C4OnemChecklistMention.class);
    static {
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.RAISON_SOCIALE_EMPLOYEUR,
                "raison sociale de l'employeur");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.NUMERO_BCE,
                "numéro BCE (10 chiffres)");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.NOM_SALARIE,
                "nom du salarié");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.DATE_ENTREE_SERVICE,
                "date d'entrée en service");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.DATE_SORTIE_SERVICE,
                "date de sortie de service");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.CATEGORIE_ONEM,
                "catégorie ONEM");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.MOTIF_EXPLICITE,
                "motif explicite de la fin de contrat (au moins " + MOTIF_EXPLICITE_LONGUEUR_MIN + " caractères)");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.PERIODE_OCCUPATION_COHERENTE,
                "cohérence de la période d'occupation (date de sortie ≥ date d'entrée)");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE,
                "préavis presté (en jours) — obligatoire en l'absence de faute grave");
        LIBELLES_MENTIONS.put(C4OnemChecklistMention.DERNIER_SALAIRE_INDIQUE,
                "dernier salaire mensuel brut");
    }

    private C4OnemChecklistCalculator() {
    }

    /**
     * Calcule la conformité du C4 ONEM pour les inputs donnés. Fonction pure :
     * aucun effet de bord, déterministe à inputs égaux.
     *
     * @param request payload (les contrôles Bean Validation ont déjà été passés
     *                en amont par le service ; le calculateur valide en plus
     *                les formats BCE/NRN et la cohérence des dates).
     * @return résultat structuré (verdict + liste des mentions manquantes +
     *         fourchette d'exclusion ONEM + lettre rectificative + base
     *         juridique + étape suivante).
     * @throws IllegalArgumentException si {@code request} est null, si
     *         {@code numeroBce} non null est mal formaté (≠ 10 chiffres),
     *         si {@code numeroNationalRegistre} non null est mal formaté
     *         (≠ 11 chiffres), ou si {@code dateSortieService <
     *         dateEntreeService} (incohérence dure → 400).
     */
    public static C4OnemChecklistResult compute(C4OnemChecklistRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête C4 ONEM requise");
        }
        // 1. Validations dures (hors mention manquante) → 400 côté service.
        if (request.numeroBce() != null && !request.numeroBce().isBlank()
                && !request.numeroBce().matches(BCE_REGEX)) {
            throw new IllegalArgumentException(
                    "Format BCE invalide : exactement 10 chiffres attendus (sans préfixe BE ni points)");
        }
        if (request.numeroNationalRegistre() != null && !request.numeroNationalRegistre().isBlank()
                && !request.numeroNationalRegistre().matches(NRN_REGEX)) {
            throw new IllegalArgumentException(
                    "Format NRN invalide : exactement 11 chiffres attendus");
        }
        if (request.dateSortieService() != null
                && request.dateEntreeService() != null
                && request.dateSortieService().isBefore(request.dateEntreeService())) {
            throw new IllegalArgumentException(
                    "date de sortie antérieure à la date d'entrée");
        }

        // 2. Détection des mentions manquantes (vérification de chaque règle).
        List<C4OnemChecklistMention> manquantes = new ArrayList<>();

        if (isBlank(request.raisonSocialeEmployeur())) {
            manquantes.add(C4OnemChecklistMention.RAISON_SOCIALE_EMPLOYEUR);
        }
        if (request.numeroBce() == null || !request.numeroBce().matches(BCE_REGEX)) {
            manquantes.add(C4OnemChecklistMention.NUMERO_BCE);
        }
        if (isBlank(request.nomSalarie())) {
            manquantes.add(C4OnemChecklistMention.NOM_SALARIE);
        }
        if (request.dateEntreeService() == null) {
            manquantes.add(C4OnemChecklistMention.DATE_ENTREE_SERVICE);
        }
        // DATE_SORTIE_SERVICE : non null + ≥ dateEntreeService.
        // L'antériorité dure (sortie < entrée) a déjà levé 400 plus haut.
        // Reste à vérifier la présence et la cohérence faible (sortie présente
        // mais entrée absente → la date est, par construction, incohérente).
        if (request.dateSortieService() == null
                || (request.dateEntreeService() != null
                && request.dateSortieService().isBefore(request.dateEntreeService()))) {
            manquantes.add(C4OnemChecklistMention.DATE_SORTIE_SERVICE);
        }
        if (isBlank(request.categorieOnem())) {
            manquantes.add(C4OnemChecklistMention.CATEGORIE_ONEM);
        }
        if (request.motifExplicite() == null
                || request.motifExplicite().trim().length() < MOTIF_EXPLICITE_LONGUEUR_MIN) {
            manquantes.add(C4OnemChecklistMention.MOTIF_EXPLICITE);
        }
        // PERIODE_OCCUPATION_COHERENTE : dateSortie ≥ dateEntree (les dates
        // doivent être présentes et cohérentes). Si une des deux dates est
        // null, on considère que la cohérence ne peut pas être établie.
        boolean periodeCoherente = request.dateEntreeService() != null
                && request.dateSortieService() != null
                && !request.dateSortieService().isBefore(request.dateEntreeService());
        if (!periodeCoherente) {
            manquantes.add(C4OnemChecklistMention.PERIODE_OCCUPATION_COHERENTE);
        }
        // PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE : si fauteGraveMentionnee=false,
        // preavisPresteJours doit être renseigné (sinon mention manquante).
        boolean fauteGrave = Boolean.TRUE.equals(request.fauteGraveMentionnee());
        if (!fauteGrave && request.preavisPresteJours() == null) {
            manquantes.add(C4OnemChecklistMention.PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE);
        }
        if (request.dernierSalaireMensuelBrut() == null) {
            manquantes.add(C4OnemChecklistMention.DERNIER_SALAIRE_INDIQUE);
        }

        // 3. Verdict : priorité faute grave > non-conforme > conforme.
        C4OnemChecklistResult.Verdict verdict;
        C4OnemChecklistResult.ExclusionOnemRange exclusionRange;
        String lettreRectificative;
        C4OnemChecklistResult.EtapeSuivante etapeSuivante;

        if (fauteGrave) {
            verdict = C4OnemChecklistResult.Verdict.RISQUE_EXCLUSION_FAUTE_GRAVE;
            exclusionRange = new C4OnemChecklistResult.ExclusionOnemRange(
                    EXCLUSION_ONEM_MIN_SEMAINES, EXCLUSION_ONEM_MAX_SEMAINES);
            // La faute grave appelle une contestation du C4, pas une simple
            // rectification (cf. mini-spec décision §282). On ne génère donc
            // pas de lettre rectificative même si d'autres mentions manquent
            // par ailleurs.
            lettreRectificative = null;
            etapeSuivante = C4OnemChecklistResult.EtapeSuivante.CONTESTATION_C4;
        } else if (!manquantes.isEmpty()) {
            verdict = C4OnemChecklistResult.Verdict.NON_CONFORME;
            exclusionRange = null;
            lettreRectificative = construireLettreRectificative(manquantes);
            etapeSuivante = C4OnemChecklistResult.EtapeSuivante.RECTIFICATION_AUPRES_EMPLOYEUR;
        } else {
            verdict = C4OnemChecklistResult.Verdict.CONFORME;
            exclusionRange = null;
            lettreRectificative = null;
            etapeSuivante = C4OnemChecklistResult.EtapeSuivante.AUCUNE;
        }

        return new C4OnemChecklistResult(
                trimToNull(request.raisonSocialeEmployeur()),
                trimToNull(request.numeroBce()),
                trimToNull(request.nomSalarie()),
                trimToNull(request.numeroNationalRegistre()),
                request.dateEntreeService(),
                request.dateSortieService(),
                trimToNull(request.categorieOnem()),
                trimToNull(request.motifExplicite()),
                fauteGrave,
                request.preavisPresteJours(),
                normalizeSalaire(request.dernierSalaireMensuelBrut()),
                verdict,
                List.copyOf(manquantes),
                fauteGrave,
                exclusionRange,
                lettreRectificative,
                BASE_JURIDIQUE_COMPLET,
                etapeSuivante);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static BigDecimal normalizeSalaire(BigDecimal s) {
        if (s == null) return null;
        // Pas de mise à l'échelle imposée — on conserve la précision saisie.
        return s;
    }

    /**
     * Construit le contenu textuel de la lettre rectificative à envoyer à
     * l'employeur. Le texte est sans en-tête ni signature (l'avocat les
     * ajoute lui-même dans son traitement de texte) ; il liste chaque
     * mention manquante explicitement et cite la base juridique.
     */
    private static String construireLettreRectificative(List<C4OnemChecklistMention> manquantes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Objet : Demande de rectification du document C4 ONEM\n\n");
        sb.append("Madame, Monsieur,\n\n");
        sb.append("Conformément à l'arrêté royal du 25 novembre 1991 portant ");
        sb.append("réglementation du chômage (art. 92), le document C4 ONEM ");
        sb.append("que vous avez remis à mon client comporte les omissions ou ");
        sb.append("imprécisions suivantes :\n\n");
        for (C4OnemChecklistMention m : manquantes) {
            String libelle = LIBELLES_MENTIONS.getOrDefault(m, m.name());
            sb.append("  - ").append(libelle).append(".\n");
        }
        sb.append("\nCes mentions étant obligatoires, je vous prie de bien ");
        sb.append("vouloir établir et transmettre un C4 rectifié comportant ");
        sb.append("l'ensemble des informations requises, dans les meilleurs ");
        sb.append("délais et au plus tard avant toute déclaration ONEM.\n\n");
        sb.append("À défaut de régularisation, je me réserve le droit ");
        sb.append("d'engager toute action utile devant le tribunal du travail ");
        sb.append("compétent pour préserver les droits de mon client à l'égard ");
        sb.append("de l'ONEM (loi du 3 juillet 1978 ; AR du 25 novembre 1991).\n\n");
        sb.append("Je vous prie d'agréer, Madame, Monsieur, l'expression de ");
        sb.append("mes salutations distinguées.\n");
        return sb.toString();
    }

    /**
     * Utilitaire interne — calcul rapide d'une date butoir « X jours après »
     * (non utilisé par {@link #compute(C4OnemChecklistRequest)} mais réservé
     * pour de futurs gardes-fous de cohérence calendaires).
     */
    static LocalDate plusJours(LocalDate date, int jours) {
        return date == null ? null : date.plusDays(jours);
    }
}

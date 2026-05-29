package fr.ailegalcase.casefile;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * SF-214-43 — analyseur des obligations de l'employeur souhaitant recruter un
 * travailleur étranger hors UE (autorisation de travail préalable, L. 5221-1
 * Code du travail) — côté employeur, complémentaire à F-IM-07 (côté étranger).
 *
 * <p>Logique :
 * <ul>
 *   <li>Si le candidat est ressortissant UE / EEE / Suisse → aucune autorisation
 *       de travail préalable (libre circulation, L. 5221-2 1°) →
 *       {@code AUTORISATION_NON_REQUISE}.</li>
 *   <li>Sinon, autorisation de travail préalable obligatoire (L. 5221-1) →
 *       {@code AUTORISATION_REQUISE}, avec liste des obligations de demande et
 *       délai d'instruction OFII de 2 mois (R. 5221-20).</li>
 *   <li>En cas de refus d'autorisation notifié : statut {@code RECOURS_POSSIBLE}
 *       si le délai de recours devant le tribunal administratif (2 mois à compter
 *       de la notification) est encore ouvert, {@code RECOURS_PRESCRIT} sinon.</li>
 * </ul>
 *
 * <p>Sources :
 * <ul>
 *   <li>L. 5221-1 à L. 5221-12 Code du travail — autorisation de travail employeur ;</li>
 *   <li>R. 5221-1 à R. 5221-44 Code du travail — procédure de demande ;</li>
 *   <li>R. 5221-20 Code du travail — délai d'instruction de 2 mois ;</li>
 *   <li>L. 421-1 CESEDA — titre autorisant le séjour pour activité salariée.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>.
 */
public final class AutorisationTravailEmployeurAnalyzer {

    /** Longueur maximale acceptée pour l'intitulé du poste. */
    public static final int MAX_POSTE_LENGTH = 200;

    /** Délai d'instruction de la demande d'autorisation de travail — 2 mois (R. 5221-20). */
    public static final int DELAI_INSTRUCTION_OFII_MOIS = 2;

    /** Délai de recours devant le tribunal administratif contre un refus — 2 mois. */
    public static final int DELAI_RECOURS_TA_MOIS = 2;

    /**
     * Liste indicative des nationalités UE / EEE / Suisse dispensées d'autorisation
     * de travail (libre circulation). Comparaison insensible à la casse et aux
     * accents. Liste non exhaustive — usage indicatif d'aide à la décision.
     */
    static final Set<String> NATIONALITES_UE_EEE_SUISSE = Set.of(
            // Union européenne (27)
            "allemande", "autrichienne", "belge", "bulgare", "chypriote", "croate",
            "danoise", "espagnole", "estonienne", "finlandaise", "francaise",
            "grecque", "hongroise", "irlandaise", "italienne", "lettone",
            "lituanienne", "luxembourgeoise", "maltaise", "neerlandaise",
            "polonaise", "portugaise", "roumaine", "slovaque", "slovene",
            "suedoise", "tcheque",
            // EEE hors UE
            "islandaise", "norvegienne", "liechtensteinoise",
            // Suisse
            "suisse"
    );

    private static final List<String> OBLIGATIONS_DEMANDE = List.of(
            "Formulaire CERFA 15187*03 (demande d'autorisation de travail)",
            "Contrat de travail signé ou promesse d'embauche",
            "Fiche descriptive du métier et des conditions d'emploi",
            "Justificatif de publication de l'offre d'emploi (3 semaines, opposabilité de l'emploi)"
    );

    private static final String TAXE_OFII =
            "Après obtention de l'autorisation de travail, l'employeur est redevable d'une "
                    + "taxe OFII (taxe employeur pour l'embauche d'un travailleur étranger). "
                    + "Le montant dépend du type et de la durée du contrat (montants : F-220).";

    private static final String BASE_JURIDIQUE =
            "L. 5221-1 à L. 5221-12 Code du travail (autorisation de travail employeur) ; "
                    + "R. 5221-1 à R. 5221-44 Code du travail (procédure) ; "
                    + "R. 5221-20 Code du travail (délai d'instruction 2 mois) ; "
                    + "L. 421-1 CESEDA (titre activité salariée)";

    private final LocalDate today;

    public AutorisationTravailEmployeurAnalyzer() {
        this(LocalDate.now());
    }

    /** @param today date de référence injectable (testabilité). */
    public AutorisationTravailEmployeurAnalyzer(LocalDate today) {
        this.today = today != null ? today : LocalDate.now();
    }

    public AutorisationTravailEmployeurResult analyze(AutorisationTravailEmployeurRequest request) {
        validate(request);

        boolean dispense = isUeEeeSuisse(request.nationaliteCandidat());
        boolean autorisationRequise = !dispense;

        List<String> obligations = autorisationRequise ? OBLIGATIONS_DEMANDE : List.of();
        Integer delaiInstruction = autorisationRequise ? DELAI_INSTRUCTION_OFII_MOIS : null;
        String taxeOFII = autorisationRequise ? TAXE_OFII : null;

        boolean recoursPossible = false;
        LocalDate delaiRecoursTa = null;
        AutorisationTravailEmployeurStatut statut;

        if (request.refusAutorisation()) {
            // Le refus d'autorisation porte sur une situation où l'autorisation était requise.
            if (request.dateRefusAutorisation() != null) {
                delaiRecoursTa = request.dateRefusAutorisation().plusMonths(DELAI_RECOURS_TA_MOIS);
                if (!today.isAfter(delaiRecoursTa)) {
                    recoursPossible = true;
                    statut = AutorisationTravailEmployeurStatut.RECOURS_POSSIBLE;
                } else {
                    statut = AutorisationTravailEmployeurStatut.RECOURS_PRESCRIT;
                }
            } else {
                // Refus notifié sans date connue : recours présumé ouvert (prudence).
                recoursPossible = true;
                statut = AutorisationTravailEmployeurStatut.RECOURS_POSSIBLE;
            }
        } else if (autorisationRequise) {
            statut = AutorisationTravailEmployeurStatut.AUTORISATION_REQUISE;
        } else {
            statut = AutorisationTravailEmployeurStatut.AUTORISATION_NON_REQUISE;
        }

        String recommandation = buildRecommandation(statut);

        return new AutorisationTravailEmployeurResult(
                request.typeContrat(),
                request.posteProposes(),
                request.nationaliteCandidat(),
                request.dureeContratMois(),
                autorisationRequise,
                obligations,
                delaiInstruction,
                taxeOFII,
                request.refusAutorisation(),
                request.dateRefusAutorisation(),
                recoursPossible,
                delaiRecoursTa,
                statut,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static String buildRecommandation(AutorisationTravailEmployeurStatut statut) {
        return switch (statut) {
            case AUTORISATION_NON_REQUISE -> "Candidat ressortissant UE/EEE/Suisse : aucune "
                    + "autorisation de travail préalable (libre circulation, L. 5221-2). Procéder "
                    + "à l'embauche selon le droit commun (DPAE, etc.).";
            case AUTORISATION_REQUISE -> "Déposer la demande d'autorisation de travail sur la "
                    + "plateforme dématérialisée (CERFA 15187*03) avant l'embauche (L. 5221-1) ; "
                    + "anticiper le délai d'instruction OFII de 2 mois (R. 5221-20) et prévoir "
                    + "la taxe OFII après obtention.";
            case RECOURS_POSSIBLE -> "Refus d'autorisation de travail : le délai de recours devant "
                    + "le tribunal administratif (2 mois) est ouvert — exercer un recours gracieux "
                    + "et/ou contentieux avant l'échéance.";
            case RECOURS_PRESCRIT -> "Délai de recours contre le refus d'autorisation dépassé "
                    + "(2 mois) : envisager une nouvelle demande plutôt qu'un recours tardif.";
        };
    }

    static boolean isUeEeeSuisse(String nationalite) {
        if (nationalite == null || nationalite.isBlank()) {
            return false;
        }
        String normalized = Normalizer.normalize(nationalite.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return NATIONALITES_UE_EEE_SUISSE.contains(normalized);
    }

    private static void validate(AutorisationTravailEmployeurRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request est requis");
        }
        if (request.typeContrat() == null) {
            throw new IllegalArgumentException("typeContrat est requis");
        }
        if (request.posteProposes() == null || request.posteProposes().isBlank()) {
            throw new IllegalArgumentException("posteProposes est requis");
        }
        if (request.posteProposes().length() > MAX_POSTE_LENGTH) {
            throw new IllegalArgumentException(
                    "posteProposes ne peut excéder " + MAX_POSTE_LENGTH + " caractères");
        }
        if (request.nationaliteCandidat() == null || request.nationaliteCandidat().isBlank()) {
            throw new IllegalArgumentException("nationaliteCandidat est requise");
        }
        if (request.dureeContratMois() != null && request.dureeContratMois() < 0) {
            throw new IllegalArgumentException("dureeContratMois ne peut être négative");
        }
        if (request.refusAutorisation() && request.dateRefusAutorisation() != null
                && request.dateRefusAutorisation().isAfter(LocalDate.now().plusYears(1))) {
            throw new IllegalArgumentException("dateRefusAutorisation est incohérente");
        }
    }

    /** Exposé pour calcul d'écart de jours éventuel côté tuile dashboard. */
    static long joursAvant(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }
}

package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-25 : calculateur statique pour la présomption de paternité du
 * mari et l'action en désaveu en droit français (art. 312-315 Cciv +
 * art. 316 al. 2 Cciv + art. 333 al. 1 Cciv).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>évalue l'<strong>application de la présomption</strong>
 *       (art. 312 Cciv — « L'enfant conçu ou né pendant le mariage a pour
 *       père le mari ») : enfant conçu pendant le mariage + né moins de
 *       300 jours après dissolution ;</li>
 *   <li>détecte le <strong>renversement de plein droit</strong>
 *       (art. 313 Cciv) : enfant né plus de 300 jours après dissolution
 *       OU moins de 180 jours après conclusion du mariage ET mari nie +
 *       pas de possession d'état ;</li>
 *   <li>évalue la <strong>recevabilité du désaveu</strong> (art. 316
 *       al. 2 Cciv) : délai 6 mois à compter de la naissance ou de la
 *       connaissance de la naissance (Cass. 1ère civ., 19/2/2014) ;</li>
 *   <li>signale l'<strong>impact de la possession d'état conforme</strong>
 *       (art. 333 al. 1 Cciv) : neutralisation de la contestation au-delà
 *       de 5 ans, désaveu difficile.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la matière est régie
 * par les art. 315 et s. CC BE (outil F-FA-BE-PRESOMPTION-PATERNITE futur,
 * hors périmètre F-216).</p>
 */
public final class PresomptionPaterniteCalculator {

    static final String BASE_JURIDIQUE =
            "art. 312 Cciv (présomption de paternité du mari — Pater is est "
                    + "quem nuptiae demonstrant) + art. 313 Cciv (renversement) "
                    + "+ art. 314 Cciv (rétablissement par possession d'état) "
                    + "+ art. 316 al. 2 Cciv (action en désaveu — délai 6 mois) "
                    + "+ art. 333 al. 1 Cciv (possession d'état conforme "
                    + "neutralise contestation) + Cass. 1ère civ., 19/2/2014 "
                    + "(point de départ délai désaveu)";

    /** Délai art. 313 al. 1 Cciv — naissance > 300 jours après dissolution. */
    static final long SEUIL_DISSO_JOURS = 300L;

    /** Délai art. 313 al. 2 Cciv — naissance < 180 jours après mariage. */
    static final long SEUIL_180_JOURS_POST_MARIAGE = 180L;

    /** Délai art. 316 al. 2 Cciv — désaveu 6 mois. */
    static final long DELAI_DESAVEU_MOIS = 6L;

    private PresomptionPaterniteCalculator() {}

    /**
     * Évalue les conditions de la présomption de paternité du mari et
     * émet le verdict + les alertes correspondantes.
     *
     * @param req      requête validée (gates pays/domaine vérifiés par le service)
     * @param country  pays du workspace ("FRANCE" attendu)
     * @param today    date courante (pour le calcul du délai désaveu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE} ou si
     *         {@code dateDissolutionMariage} est antérieure à
     *         {@code dateConclusionMariage}.
     */
    public static PresomptionPaterniteResult compute(
            PresomptionPaterniteRequest req, String country, LocalDate today) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-PRESOMPTION-PATERNITE applicable uniquement "
                            + "en France (art. 312 Cciv).");
        }
        if (req == null) {
            throw new IllegalArgumentException("Requête nulle.");
        }
        if (req.dateNaissanceEnfant() == null) {
            throw new IllegalArgumentException(
                    "dateNaissanceEnfant est requis.");
        }
        if (req.dateConclusionMariage() == null) {
            throw new IllegalArgumentException(
                    "dateConclusionMariage est requis.");
        }
        if (req.dateDissolutionMariage() != null
                && req.dateDissolutionMariage().isBefore(req.dateConclusionMariage())) {
            throw new IllegalArgumentException(
                    "dateDissolutionMariage ne peut pas être antérieure à "
                            + "dateConclusionMariage.");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        LocalDate dateNaissance = req.dateNaissanceEnfant();
        LocalDate dateMariage = req.dateConclusionMariage();
        LocalDate dateDisso = req.dateDissolutionMariage();
        boolean conceptionAvantMariage = Boolean.TRUE.equals(
                req.conceptionEn180PremiersMoisMariage());
        boolean possessionEtat = Boolean.TRUE.equals(
                req.possessionEtatConformeDetecte());
        boolean desaveuEnvisage = Boolean.TRUE.equals(req.desaveuEnvisage());

        // 1. Calcul de l'écart en jours entre naissance et dissolution.
        Long joursDepuisDisso = null;
        if (dateDisso != null) {
            joursDepuisDisso = ChronoUnit.DAYS.between(dateDisso, dateNaissance);
        }
        // Source de vérité : dates si disponibles, sinon le flag transmis.
        boolean neApresDisso300j;
        if (joursDepuisDisso != null) {
            neApresDisso300j = joursDepuisDisso > SEUIL_DISSO_JOURS;
        } else if (req.enfantNeApresDisso() != null) {
            neApresDisso300j = req.enfantNeApresDisso();
        } else {
            neApresDisso300j = false;
        }

        // 2. Application de la présomption (art. 312 Cciv).
        //    « L'enfant conçu ou né pendant le mariage a pour père le mari. »
        boolean presomptionApplicable;
        boolean presomptionRenversee = false;

        if (neApresDisso300j) {
            // Art. 313 al. 1 Cciv : présomption écartée de plein droit.
            presomptionApplicable = false;
            presomptionRenversee = true;
            messages.add("Enfant né plus de 300 jours après la dissolution "
                    + "du mariage (art. 313 al. 1 Cciv) : la présomption de "
                    + "paternité du mari est écartée de plein droit. "
                    + "L'établissement de la paternité suppose une "
                    + "reconnaissance (art. 316) ou une action en "
                    + "recherche de paternité (art. 327).");
            if (joursDepuisDisso != null) {
                messages.add("Écart calculé : " + joursDepuisDisso + " jours "
                        + "entre la dissolution du mariage et la naissance.");
            }
        } else if (conceptionAvantMariage && !possessionEtat && desaveuEnvisage) {
            // Art. 313 al. 2 Cciv : présomption peut être écartée à condition
            // que le mari désavoue et qu'il n'y ait pas possession d'état.
            presomptionApplicable = true;
            presomptionRenversee = true;
            messages.add("Enfant conçu dans les 180 premiers jours du "
                    + "mariage (art. 313 al. 2 Cciv) ET mari désavoue ET "
                    + "absence de possession d'état conforme : la présomption "
                    + "de paternité peut être écartée.");
        } else if (conceptionAvantMariage && possessionEtat) {
            // Présomption maintenue : possession d'état neutralise la
            // contestation (art. 314 / 333 al. 1 Cciv).
            presomptionApplicable = true;
            messages.add("Enfant conçu dans les 180 premiers jours du "
                    + "mariage MAIS possession d'état conforme du mari "
                    + "documentée (art. 314 Cciv) : la présomption de "
                    + "paternité est rétablie / maintenue.");
        } else {
            // Cas nominal — présomption applicable.
            presomptionApplicable = true;
            messages.add("Enfant conçu ou né pendant le mariage (art. 312 "
                    + "Cciv) : la présomption de paternité du mari s'applique. "
                    + "Le mari est réputé être le père de l'enfant.");
        }

        // 3. Évaluation de la voie de désaveu (art. 316 al. 2 Cciv).
        String voieDesaveu;
        String delaiDesaveu;

        if (!presomptionApplicable && presomptionRenversee) {
            // Présomption écartée de plein droit — désaveu sans objet.
            voieDesaveu = "DESAVEU_SANS_OBJET";
            delaiDesaveu = "Désaveu sans objet — la présomption est écartée "
                    + "de plein droit (art. 313 al. 1 Cciv). Aucune action en "
                    + "désaveu nécessaire ; établir la paternité suppose une "
                    + "reconnaissance ou une recherche de paternité.";
        } else if (possessionEtat) {
            // Désaveu très difficile en présence de possession d'état conforme.
            voieDesaveu = "DESAVEU_DIFFICILE_POSSESSION_ETAT";
            delaiDesaveu = "Désaveu très difficile en présence d'une "
                    + "possession d'état conforme du mari (art. 333 al. 1 "
                    + "Cciv) : passé 5 ans de possession d'état, la "
                    + "contestation est irrecevable. Avant 5 ans, le délai "
                    + "art. 316 al. 2 Cciv (6 mois à compter de la naissance "
                    + "ou de la connaissance) reste applicable.";
            alertes.add("Possession d'état conforme du mari documentée "
                    + "(art. 333 al. 1 Cciv) — l'action en désaveu est très "
                    + "difficile, voire irrecevable au-delà de 5 ans de "
                    + "possession d'état. Justifier précisément les motifs "
                    + "et les pièces avant introduction de l'action.");
        } else if (desaveuEnvisage) {
            // Cas standard — évaluer le délai 6 mois.
            LocalDate pointDepart = req.dateConnaissanceNaissance() != null
                    ? req.dateConnaissanceNaissance() : dateNaissance;
            long moisEcoules = ChronoUnit.MONTHS.between(pointDepart, today);
            if (moisEcoules >= DELAI_DESAVEU_MOIS) {
                voieDesaveu = "DESAVEU_DELAI_FORCLOS";
                delaiDesaveu = "Délai de désaveu forclos (art. 316 al. 2 "
                        + "Cciv) : plus de 6 mois écoulés depuis la naissance "
                        + "ou la connaissance de la naissance (Cass. 1ère "
                        + "civ., 19/2/2014). Action en désaveu irrecevable.";
                alertes.add("Délai 6 mois (art. 316 al. 2 Cciv) DÉPASSÉ — "
                        + moisEcoules + " mois écoulés depuis le "
                        + (req.dateConnaissanceNaissance() != null
                                ? "point de départ documenté"
                                : "jour de la naissance")
                        + ". L'action en désaveu est forclose. Vérifier "
                        + "d'éventuels actes interruptifs ou un point de "
                        + "départ différent (connaissance tardive).");
            } else {
                voieDesaveu = "DESAVEU_RECEVABLE";
                long moisRestants = DELAI_DESAVEU_MOIS - moisEcoules;
                delaiDesaveu = "Action en désaveu recevable (art. 316 al. 2 "
                        + "Cciv) — délai 6 mois à compter de la naissance "
                        + "ou de la connaissance de la naissance (Cass. 1ère "
                        + "civ., 19/2/2014). Mois écoulés : " + moisEcoules
                        + " — il reste environ " + moisRestants + " mois "
                        + "pour agir.";
            }
        } else {
            voieDesaveu = "INDETERMINE";
            delaiDesaveu = "Désaveu non envisagé dans la requête — la "
                    + "présomption de paternité du mari s'applique sans "
                    + "contestation (art. 312 Cciv).";
        }

        // 4. Impact de la possession d'état conforme (art. 333 al. 1 Cciv).
        String possessionEtatImpact;
        if (possessionEtat) {
            possessionEtatImpact = "Possession d'état conforme du mari "
                    + "documentée (art. 333 al. 1 Cciv) : passé 5 ans, "
                    + "l'action en contestation devient irrecevable. La "
                    + "possession d'état conforme renforce la présomption de "
                    + "paternité et neutralise le désaveu — sauf preuve d'un "
                    + "vice manifeste (art. 314 Cciv pour le rétablissement).";
        } else {
            possessionEtatImpact = "Aucune possession d'état conforme du mari "
                    + "documentée — l'action en désaveu reste possible "
                    + "(art. 316 al. 2 Cciv) dans le délai de 6 mois. "
                    + "L'établissement éventuel d'une possession d'état "
                    + "conforme de 5 ans rendrait la contestation irrecevable.";
        }

        // 5. Vigilance complémentaire — accouchement posthume / dissolution
        //    récente : avertir l'utilisateur sur la délicatesse du calcul.
        if (dateDisso != null && joursDepuisDisso != null
                && joursDepuisDisso >= 270L && joursDepuisDisso <= 300L) {
            alertes.add("Naissance dans la fourchette critique 270-300 jours "
                    + "après dissolution (art. 313 al. 1 Cciv) — vérifier la "
                    + "date précise d'accouchement et la possibilité d'une "
                    + "conception avant dissolution. Tout calcul incorrect "
                    + "peut renverser le verdict présomption applicable / "
                    + "renversée.");
        }
        if (req.dateAccouchement() != null
                && !req.dateAccouchement().equals(dateNaissance)) {
            messages.add("Date d'accouchement distincte de la date de "
                    + "naissance déclarée — accouchement posthume ou "
                    + "déclaration différée à vérifier.");
        }

        return new PresomptionPaterniteResult(
                presomptionApplicable,
                presomptionRenversee,
                voieDesaveu,
                delaiDesaveu,
                possessionEtatImpact,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }
}

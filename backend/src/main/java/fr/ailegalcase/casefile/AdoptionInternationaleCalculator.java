package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-216-17 : calculateur statique pour l'adoption internationale en droit
 * français (art. 370-3 à 370-5 Cciv + Convention La Haye du 29/5/1993 +
 * Loi n°2001-111 du 6/2/2001 + Loi n°2022-219 du 21/2/2022).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>vérifie l'agrément obligatoire (condition sine qua non, loi 2001) ;</li>
 *   <li>évalue l'applicabilité de la Convention La Haye 1993 selon le pays
 *       d'origine ;</li>
 *   <li>oriente entre OAA agréé / voie autonome / organisme d'État ;</li>
 *   <li>alerte sur le risque kafala (Maroc / Algérie / Tunisie) — kafala
 *       irréductiblement inacceptable comme adoption en droit FR
 *       (art. 370-3 al. 3 Cciv, Cass. 1ère civ. 10/10/2006) ;</li>
 *   <li>signale la nécessité d'un exequatur TJ pour toute décision
 *       d'adoption rendue à l'étranger (art. 370-5 Cciv) ;</li>
 *   <li>estime les délais procéduraux typiques.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, l'adoption
 * internationale est régie par les art. 343 et suiv. CcB avec la même
 * Convention La Haye 1993 mais des conditions de fond distinctes (outil
 * F-FA-BE-ADOPTION-INTL futur, hors périmètre F-216).</p>
 */
public final class AdoptionInternationaleCalculator {

    static final String BASE_JURIDIQUE =
            "art. 370-3 à 370-5 Cciv + Convention La Haye 29/5/1993 + "
                    + "Loi n°2001-111 du 6/2/2001 (agrément) + "
                    + "Loi n°2022-219 du 21/2/2022 (réforme)";

    /**
     * Pays du Maghreb appliquant la kafala et n'ayant pas ratifié la
     * Convention La Haye 1993 pour l'adoption (la kafala ≠ adoption en
     * droit FR — Cass. 1ère civ., 10/10/2006).
     */
    static final Set<String> PAYS_KAFALA = Set.of("MAROC", "ALGERIE", "TUNISIE");

    /**
     * Liste indicative (non exhaustive) de quelques États signataires de la
     * Convention La Haye 1993 — utilisée si l'avocat n'a pas renseigné
     * {@code conventionLaHayeApplicable} explicitement. La Convention compte
     * plus de 100 États parties — cette liste sert uniquement de filet de
     * sécurité indicatif côté serveur.
     */
    static final Set<String> PAYS_CONVENTION_INDICATIFS = Set.of(
            "COLOMBIE", "VIETNAM", "BRESIL", "PHILIPPINES", "INDE", "CHINE",
            "MEXIQUE", "PEROU", "BULGARIE", "POLOGNE", "PORTUGAL", "ESPAGNE",
            "ITALIE", "HONGRIE", "RUSSIE_HORS_CONVENTION_SIGNATURE",
            "MADAGASCAR", "BURKINA_FASO", "BURUNDI", "CAP_VERT",
            "CONGO", "COSTA_RICA", "EQUATEUR", "ETHIOPIE_HORS_CONVENTION",
            "GUATEMALA", "HAITI", "KENYA", "MALI", "MOLDAVIE",
            "MONGOLIE", "NEPAL_HORS_CONVENTION", "PARAGUAY",
            "REPUBLIQUE_DOMINICAINE", "RWANDA", "SENEGAL", "THAILANDE",
            "TOGO", "TURQUIE", "UKRAINE", "VENEZUELA"
    );

    private AdoptionInternationaleCalculator() {}

    /**
     * Évalue les conditions de l'adoption internationale et émet le verdict
     * + les alertes correspondantes.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static AdoptionInternationaleResult compute(
            AdoptionInternationaleRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-ADOPTION-INTERNATIONALE applicable uniquement "
                            + "en France (art. 370-3 Cciv).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        String paysCode = normalizePays(req.paysOrigineEnfant());
        boolean agrement = Boolean.TRUE.equals(req.agrement2025());
        boolean adoptantMarie = Boolean.TRUE.equals(req.adoptantMarie());
        boolean exequaturRequis = Boolean.TRUE.equals(req.exequaturRequis());
        VoieProcedureAdoptionEnum voieDemandee = req.voieProcedure();
        FormeAdoptionEnum formeDemandee = req.formeAdoptionDemandee();

        // 1. Alerte kafala — pays Maghreb pratiquant la kafala
        boolean alerteKafala = paysCode != null && PAYS_KAFALA.contains(paysCode);
        if (alerteKafala) {
            alertes.add("Pays d'origine pratiquant la kafala (" + paysCode
                    + ") : la kafala n'est PAS une adoption en droit français "
                    + "(art. 370-3 al. 3 Cciv, Cass. 1ère civ., 10/10/2006). "
                    + "Aucune adoption (plénière ou simple) ne peut être prononcée "
                    + "tant que l'enfant n'a pas acquis la nationalité française "
                    + "ou que la loi personnelle de l'enfant n'autorise pas "
                    + "l'adoption. Réorienter le client vers une demande de "
                    + "tutelle ou d'accueil kafala.");
            return new AdoptionInternationaleResult(
                    false,
                    voieDemandee,
                    false,
                    true,
                    exequaturRequis,
                    "Délai non applicable — kafala incompatible.",
                    "KAFALA_INCOMPATIBLE",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // 2. Agrément — condition sine qua non (loi n°2001-111 du 6/2/2001)
        if (!agrement) {
            alertes.add("Agrément requis : aucun projet d'adoption "
                    + "internationale n'est recevable en France sans agrément "
                    + "préalable du Conseil départemental (loi n°2001-111 du "
                    + "6/2/2001, validité 5 ans renouvelable). Orienter le "
                    + "client vers le dépôt d'une demande d'agrément auprès "
                    + "de l'Aide Sociale à l'Enfance (ASE) du département de "
                    + "résidence avant toute démarche internationale.");
            return new AdoptionInternationaleResult(
                    false,
                    voieDemandee,
                    false,
                    false,
                    exequaturRequis,
                    "Délai estimé d'obtention de l'agrément : 9 à 12 mois.",
                    "AGREMENT_REQUIS",
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }
        messages.add("Agrément valide (loi n°2001-111 du 6/2/2001) — "
                + "vérifier la date d'expiration (durée 5 ans). À renouveler "
                + "avant péremption auprès du Conseil départemental.");

        // 3. Convention La Haye 1993 — applicabilité
        boolean conventionApplicable = resolveConventionApplicable(
                req.conventionLaHayeApplicable(), paysCode);
        if (conventionApplicable) {
            messages.add("Convention La Haye 29/5/1993 applicable au pays "
                    + "d'origine (" + paysCode + ") : procédure bilatérale "
                    + "via Autorité centrale (Mission de l'Adoption Internationale, "
                    + "MAI). Voie OAA agréé privilégiée — sécurité juridique "
                    + "renforcée (reconnaissance automatique de la décision "
                    + "étrangère, art. 23 Convention).");
        } else if (paysCode != null) {
            alertes.add("Pays d'origine (" + paysCode + ") non signataire ou "
                    + "statut Convention La Haye non confirmé : la voie autonome "
                    + "ou OAA agréé reste possible mais expose à un risque "
                    + "accru de refus de transcription en France (art. 370-3 "
                    + "al. 2 Cciv). Vérifier la liste officielle des États "
                    + "parties sur hcch.net et préparer un dossier de "
                    + "transcription renforcé (vérification équivalence "
                    + "filiation, consentements, autorités étrangères).");
        }

        // 4. Voie procédurale — verdict + recommandation
        VoieProcedureAdoptionEnum voieFinale = voieDemandee != null
                ? voieDemandee
                : (conventionApplicable
                        ? VoieProcedureAdoptionEnum.OAA_AGREE
                        : VoieProcedureAdoptionEnum.VOIE_AUTONOME);
        String verdictCode;
        switch (voieFinale) {
            case OAA_AGREE -> {
                verdictCode = "OAA_CONVENTION";
                messages.add("Voie OAA agréé : engagement avec un Organisme "
                        + "Autorisé pour l'Adoption habilité par le ministère "
                        + "de la Justice (liste publiée sur diplomatie.gouv.fr). "
                        + "Avantage : sécurisation procédurale et accompagnement "
                        + "post-adoption. Inconvénient : délais 2 à 5 ans, "
                        + "coût 8 000 à 20 000 €.");
            }
            case VOIE_AUTONOME -> {
                verdictCode = "VOIE_AUTONOME_RISQUE";
                alertes.add("Voie autonome (démarche individuelle) : possible "
                        + "uniquement dans certains États n'imposant pas le "
                        + "passage par un OAA. Risque renforcé de refus de "
                        + "transcription en France (art. 370-3 al. 2 Cciv) — "
                        + "informer le client par écrit des risques juridiques "
                        + "et de la durée probable (3 à 7 ans).");
            }
            case ORGANISME_ETAT -> {
                verdictCode = "OAA_CONVENTION";
                messages.add("Voie organisme d'État (AFA ou opérateur public "
                        + "résiduel) : ouverte pour certains pays partenaires. "
                        + "Sécurité juridique équivalente à l'OAA agréé.");
            }
            default -> verdictCode = "OK";
        }

        // 5. Exequatur — décision étrangère rendue (art. 370-5 Cciv)
        if (exequaturRequis) {
            messages.add("Exequatur requis (art. 370-5 Cciv) : la décision "
                    + "étrangère d'adoption doit être présentée au tribunal "
                    + "judiciaire (TJ) du lieu de résidence pour qu'une "
                    + "décision française de transcription soit prise. Délai "
                    + "moyen 6 à 12 mois.");
            verdictCode = "EXEQUATUR_REQUIS";
        }

        // 6. Forme demandée — alerte irréversibilité plénière
        if (formeDemandee == FormeAdoptionEnum.PLENIERE) {
            alertes.add("Forme PLÉNIÈRE demandée : rupture définitive de la "
                    + "filiation antérieure (art. 356 Cciv). Devoir de conseil "
                    + "renforcé — formaliser l'information du client par écrit. "
                    + "Vérifier que la loi personnelle de l'enfant autorise "
                    + "l'adoption plénière (art. 370-3 al. 2 Cciv).");
        } else if (formeDemandee == FormeAdoptionEnum.SIMPLE) {
            messages.add("Forme SIMPLE demandée (art. 360-362 Cciv) : "
                    + "cumulable avec la filiation d'origine — option à "
                    + "privilégier si la loi personnelle de l'enfant interdit "
                    + "l'adoption plénière (cas fréquent pour les pays de "
                    + "tradition coranique).");
        }

        // 7. Âge adoptant — rappel des règles d'âge (réforme 2022)
        Integer ageAdoptant = req.ageAdoptant();
        if (ageAdoptant != null && ageAdoptant < 26 && !adoptantMarie) {
            alertes.add("Âge adoptant (" + ageAdoptant + " ans) : la Loi "
                    + "n°2022-219 du 21/2/2022 a abaissé l'âge minimum à 26 ans "
                    + "(seul) ou 28 ans (couple). À vérifier au regard de la "
                    + "situation exacte.");
        }

        // 8. Différence d'âge minimale (art. 344, principe général : 15 ans)
        Integer ageAdopte = req.ageAdopte();
        if (ageAdoptant != null && ageAdopte != null
                && ageAdoptant > 0 && ageAdopte > 0) {
            int diff = ageAdoptant - ageAdopte;
            if (diff < 15) {
                alertes.add("Différence d'âge entre adoptant (" + ageAdoptant
                        + " ans) et adopté (" + ageAdopte + " ans) inférieure "
                        + "à 15 ans — dérogation possible par le tribunal en "
                        + "présence de justes motifs (art. 344 al. 2 Cciv), à "
                        + "motiver dans la requête.");
            }
        }

        // 9. Délai estimé global
        String delai = exequaturRequis
                ? "Exequatur : 6 à 12 mois (TJ). Pas de nouveau délai d'adoption."
                : (conventionApplicable
                        ? "Procédure La Haye via OAA : 2 à 5 ans."
                        : "Procédure hors convention : 3 à 7 ans (risque de refus accru).");

        // 10. Réforme 2022 — rappel utile
        messages.add("Réforme 2022 (Loi n°2022-219 du 21/2/2022) : extension "
                + "de l'adoption aux couples pacsés (4 ans de vie commune), "
                + "abaissement de l'âge minimum à 26 ans (seul) / 28 ans "
                + "(couple), suppression de la condition d'âge minimum de "
                + "l'adopté.");

        return new AdoptionInternationaleResult(
                true,
                voieFinale,
                conventionApplicable,
                false,
                exequaturRequis,
                delai,
                verdictCode,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }

    /**
     * Normalise un nom de pays libre en code MAJUSCULE_SANS_ESPACE_NI_ACCENT
     * pour permettre la comparaison avec les sets {@link #PAYS_KAFALA} et
     * {@link #PAYS_CONVENTION_INDICATIFS}. Conserve les underscores existants.
     */
    static String normalizePays(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        // Normalisation Unicode pour retirer les accents.
        String normalized = java.text.Normalizer
                .normalize(trimmed, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized
                .toUpperCase(Locale.FRENCH)
                .replace(' ', '_')
                .replace('-', '_');
    }

    /**
     * Détermine si la Convention La Haye 1993 s'applique au pays d'origine.
     * Si l'avocat a renseigné explicitement le drapeau, on l'utilise ;
     * sinon, on consulte la liste indicative serveur (filet de sécurité).
     */
    static boolean resolveConventionApplicable(Boolean explicitFlag, String paysCode) {
        if (explicitFlag != null) return explicitFlag;
        if (paysCode == null) return false;
        return PAYS_CONVENTION_INDICATIFS.contains(paysCode);
    }
}

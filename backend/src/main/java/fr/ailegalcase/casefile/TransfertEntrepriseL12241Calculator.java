package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-05 : moteur d'analyse de l'applicabilité de l'article L. 1224-1 CT
 * lors d'un transfert d'entreprise (FRANCE).
 *
 * <p>Vérifie les conditions cumulatives d'application :
 * <ul>
 *   <li><b>Entité économique autonome (EEA)</b> identifiée avant le transfert
 *       — Cass. soc. 18/07/2000 n°98-46.071 (ensemble organisé de personnes et
 *       d'éléments corporels et incorporels permettant l'exercice d'une
 *       activité économique poursuivant un objectif propre).</li>
 *   <li><b>Activité économique</b> préservée après le transfert (continuité
 *       d'exploitation).</li>
 * </ul>
 *
 * <p>Si les conditions sont réunies, l'art. L. 1224-1 CT impose le maintien
 * automatique <em>de plein droit</em> de tous les contrats de travail en
 * cours au moment du transfert, avec toutes leurs clauses, au repreneur.</p>
 *
 * <p>Détecte aussi les irrégularités :
 * <ul>
 *   <li>Licenciements pré-transfert pour permettre le rachat (frauduleux —
 *       jurisprudence constante de la chambre sociale).</li>
 *   <li>Défaut d'information-consultation du CSE (L. 1224-3 CT,
 *       L. 2323-1 s. CT).</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la notion d'EEA telle que développée
 * par la chambre sociale est française (la BE applique le maintien via la
 * CCT 32bis, régime distinct).</p>
 */
public final class TransfertEntrepriseL12241Calculator {

    /** Type de transfert envisagé / réalisé. */
    public enum TypeTransfert {
        CESSION,
        FUSION,
        APPORT_PARTIEL_ACTIF,
        EXTERNALISATION,
        REPRISE_ACTIVITE,
        AUTRE
    }

    /** Verdict d'applicabilité de L. 1224-1. */
    public enum AnalyseTransfert {
        L1224_APPLICABLE,
        L1224_INAPPLICABLE,
        L1224_INCERTAIN
    }

    /** Code structuré d'un point d'analyse détecté. */
    public enum CodePoint {
        DT72_EEA_IDENTIFIEE,
        DT72_ACTIVITE_PRESERVEE,
        DT72_LICENCIEMENTS_PRE_TRANSFERT,
        DT72_CONSULTATION_CSE
    }

    /** Point d'analyse détecté — exposé dans la réponse API. */
    public record PointAnalyseTransfert(
            CodePoint code,
            String libelle,
            String fondement,
            String conclusion
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseTransfert analyseL1224_1,
            int scoreApplicabilite,
            List<PointAnalyseTransfert> pointsAnalyse,
            boolean alerteLicenciementsFrauduleux,
            boolean alerteDefautConsultationCse,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private TransfertEntrepriseL12241Calculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null
     */
    public static Result compute(TransfertEntrepriseL12241Input input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.nbLicenciementsPreTransfert() < 0) {
            throw new IllegalArgumentException(
                    "Le nombre de licenciements pré-transfert ne peut pas être négatif");
        }

        List<PointAnalyseTransfert> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        boolean eea = Boolean.TRUE.equals(input.eeaIdentifieeAvantTransfert());
        boolean activite = Boolean.TRUE.equals(input.activiteEconomiquePreservee());
        boolean salariesTransferes = Boolean.TRUE.equals(input.salariesTransferes());
        boolean contratsModifies = Boolean.TRUE.equals(input.contratsModifiesParRepreneur());
        boolean licenciementsPre = Boolean.TRUE.equals(input.licenciementsPreTransfert())
                && input.nbLicenciementsPreTransfert() > 0;
        boolean cseConsulte = Boolean.TRUE.equals(input.informationConsultationCseRealisee());

        // ── 1. Entité économique autonome (EEA) ──────────────────────────────
        points.add(new PointAnalyseTransfert(
                CodePoint.DT72_EEA_IDENTIFIEE,
                eea ? "Entité économique autonome identifiée avant le transfert"
                    : "Entité économique autonome NON identifiée",
                "Cass. soc. 18/07/2000 n°98-46.071 — EEA : ensemble organisé de personnes et d'éléments",
                eea ? "Condition d'EEA réunie — la première condition d'application de L. 1224-1 est satisfaite."
                    : "Absence d'EEA — L. 1224-1 inapplicable (transfert simple d'actifs sans organisation autonome)."));
        bases.add("Cass. soc. 18/07/2000 n°98-46.071 — entité économique autonome");

        // ── 2. Activité économique préservée ──────────────────────────────────
        points.add(new PointAnalyseTransfert(
                CodePoint.DT72_ACTIVITE_PRESERVEE,
                activite ? "Activité économique préservée après le transfert"
                         : "Activité économique NON préservée",
                "L. 1224-1 CT — continuité de l'activité au sein de l'entité transférée",
                activite ? "L'activité économique est poursuivie — condition cumulative satisfaite."
                         : "L'activité n'est pas poursuivie — L. 1224-1 inapplicable."));
        bases.add("L. 1224-1 CT — maintien automatique des contrats de travail");

        // ── 3. Licenciements pré-transfert frauduleux ─────────────────────────
        if (licenciementsPre) {
            points.add(new PointAnalyseTransfert(
                    CodePoint.DT72_LICENCIEMENTS_PRE_TRANSFERT,
                    "Licenciements prononcés par le cédant avant le transfert (" +
                            input.nbLicenciementsPreTransfert() + " salariés)",
                    "Cass. soc. — licenciements pré-transfert pour permettre le rachat = fraude à L. 1224-1",
                    "ALERTE : des licenciements ont été prononcés par le cédant peu avant le transfert. " +
                            "Si l'objectif était de permettre le rachat (vider l'EEA de ses salariés), ces " +
                            "licenciements sont nuls et les contrats doivent être réintégrés chez le repreneur."));
            messages.add("ALERTE : " + input.nbLicenciementsPreTransfert() +
                    " licenciement(s) pré-transfert détecté(s). Vérifier l'intention frauduleuse (rachat allégé).");
            bases.add("Cass. soc. — fraude à L. 1224-1 par licenciements pré-cession");
        }

        // ── 4. Consultation CSE ───────────────────────────────────────────────
        if (!cseConsulte) {
            points.add(new PointAnalyseTransfert(
                    CodePoint.DT72_CONSULTATION_CSE,
                    "Défaut d'information-consultation du CSE",
                    "L. 1224-3 CT + L. 2323-1 s. CT — consultation obligatoire avant le transfert",
                    "ALERTE : le CSE n'a pas été consulté préalablement au transfert. " +
                            "Manquement à l'obligation d'information-consultation susceptible de fonder " +
                            "une action en référé du CSE et des sanctions pénales (délit d'entrave)."));
            messages.add("ALERTE : absence de consultation du CSE avant transfert (L. 1224-3 CT).");
            bases.add("L. 1224-3 CT — information-consultation du CSE");
        } else {
            points.add(new PointAnalyseTransfert(
                    CodePoint.DT72_CONSULTATION_CSE,
                    "Information-consultation du CSE réalisée",
                    "L. 1224-3 CT + L. 2323-1 s. CT",
                    "Le CSE a été consulté préalablement — obligation employeur satisfaite."));
        }

        // ── 5. Verdict applicabilité ──────────────────────────────────────────
        AnalyseTransfert verdict;
        int score;

        if (eea && activite) {
            verdict = AnalyseTransfert.L1224_APPLICABLE;
            score = 80;
            messages.add("L. 1224-1 CT applicable — maintien automatique de plein droit de tous les " +
                    "contrats de travail en cours au moment du transfert, avec toutes leurs clauses.");
            if (salariesTransferes) {
                score = Math.min(100, score + 10);
            } else {
                messages.add("Anomalie : malgré l'applicabilité de L. 1224-1, les salariés ne sont " +
                        "pas indiqués comme transférés. Vérifier la liste nominative des contrats repris.");
                score -= 15;
            }
            if (contratsModifies) {
                messages.add("ALERTE : modification de clauses contractuelles par le repreneur. " +
                        "Le repreneur ne peut pas modifier unilatéralement le contrat — toute " +
                        "modification suppose l'accord du salarié (Cass. soc.).");
                bases.add("Cass. soc. — interdiction de modifier unilatéralement les contrats transférés");
                score -= 10;
            }
        } else if (!eea) {
            verdict = AnalyseTransfert.L1224_INAPPLICABLE;
            score = 15;
            messages.add("L. 1224-1 CT inapplicable — absence d'entité économique autonome au sens " +
                    "de Cass. soc. 18/07/2000. Les contrats ne sont pas transférés de plein droit.");
        } else {
            // EEA identifiée mais activité non préservée → incertain
            verdict = AnalyseTransfert.L1224_INCERTAIN;
            score = 45;
            messages.add("Applicabilité INCERTAINE de L. 1224-1 — l'EEA est identifiée mais la " +
                    "continuité d'activité n'est pas démontrée. Vérifier les éléments factuels " +
                    "de poursuite d'exploitation.");
        }

        // Type de transfert — ajusté score (renforce / fragilise applicabilité)
        switch (input.typeTransfert()) {
            case CESSION, FUSION, APPORT_PARTIEL_ACTIF, REPRISE_ACTIVITE -> {
                // Cas classiques L. 1224-1 — pas d'ajustement
            }
            case EXTERNALISATION -> {
                messages.add("Externalisation : la qualification d'EEA suppose le transfert effectif " +
                        "de moyens (personnel, matériels, organisation). Une simple sous-traitance " +
                        "ne suffit pas (Cass. soc. — exigence de transfert organisé).");
                if (verdict == AnalyseTransfert.L1224_APPLICABLE) {
                    score = Math.max(0, score - 5);
                }
            }
            case AUTRE -> messages.add(
                    "Type de transfert atypique — qualification à analyser au cas par cas.");
        }

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                licenciementsPre,
                !cseConsulte,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}

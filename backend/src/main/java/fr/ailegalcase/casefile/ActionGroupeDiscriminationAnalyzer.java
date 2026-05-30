package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-09 : analyseur de la recevabilité d'une action de groupe en
 * discrimination au travail (art. L. 1134-7 à L. 1134-10 Code travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Apprécie trois conditions cumulatives de recevabilité :
 * <ul>
 *   <li><b>Qualité à agir</b> (art. L. 1134-7 CT) : syndicat représentatif ou
 *       association régulièrement déclarée depuis au moins 5 ans intervenant
 *       dans la lutte contre les discriminations ;</li>
 *   <li><b>Mise en demeure préalable + délai de carence de 6 mois</b>
 *       (art. L. 1134-9 CT) : la saisine n'est possible que 6 mois après que
 *       l'employeur a été mis en demeure de faire cesser le manquement ;</li>
 *   <li><b>Pluralité</b> : plusieurs personnes (≥ 2) placées dans une situation
 *       similaire de discrimination.</li>
 * </ul>
 *
 * <p>Invariant CLAUDE.md — un outil = une situation métier : cet outil traite la
 * recevabilité du <b>contentieux collectif</b> de l'action de groupe en
 * discrimination (L. 1134-7 et s. CT, loi J21 du 18/11/2016). Le contentieux
 * individuel de la discrimination (preuve, nullité, indemnisation d'un salarié)
 * et la phase ultérieure de réparation individuelle (L. 1134-10) sont des
 * situations distinctes.
 */
public final class ActionGroupeDiscriminationAnalyzer {

    /** Délai de carence après mise en demeure de l'employeur (art. L. 1134-9 CT). */
    public static final int DELAI_CARENCE_MOIS = 6;

    /** Seuil de pluralité : au moins 2 personnes en situation similaire. */
    public static final int SEUIL_PLURALITE = 2;

    private static final String BASE_JURIDIQUE =
            "L. 1134-7 Code travail (organisations habilitées : syndicats "
                    + "représentatifs, associations déclarées depuis 5 ans luttant contre "
                    + "les discriminations) ; L. 1134-8 CT (objet : cessation du "
                    + "manquement et reparation des préjudices) ; L. 1134-9 CT (mise en "
                    + "demeure préalable de l'employeur et délai de 6 mois avant saisine) ; "
                    + "L. 1134-10 CT (articulation avec la réparation individuelle) ; "
                    + "L. 1132-1 CT (critères de discrimination prohibés) ; "
                    + "loi J21 du 18/11/2016 (action de groupe en discrimination)";

    private ActionGroupeDiscriminationAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static ActionGroupeDiscriminationResult analyze(
            ActionGroupeDiscriminationTypeOrganisation typeOrganisation,
            ActionGroupeDiscriminationMotif motifDiscrimination,
            int nombrePersonnesConcernees,
            ActionGroupeDiscriminationObjet objetAction,
            LocalDate dateMiseEnDemeure) {
        return analyze(typeOrganisation, motifDiscrimination, nombrePersonnesConcernees,
                objetAction, dateMiseEnDemeure, LocalDate.now());
    }

    /**
     * Analyse la recevabilité de l'action de groupe.
     *
     * @param today date de référence injectée (testabilité du délai de carence).
     */
    public static ActionGroupeDiscriminationResult analyze(
            ActionGroupeDiscriminationTypeOrganisation typeOrganisation,
            ActionGroupeDiscriminationMotif motifDiscrimination,
            int nombrePersonnesConcernees,
            ActionGroupeDiscriminationObjet objetAction,
            LocalDate dateMiseEnDemeure,
            LocalDate today) {
        validate(typeOrganisation, motifDiscrimination, nombrePersonnesConcernees,
                dateMiseEnDemeure, today);

        ActionGroupeDiscriminationObjet objet =
                objetAction != null ? objetAction : ActionGroupeDiscriminationObjet.LES_DEUX;

        boolean qualiteAAgir =
                typeOrganisation == ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF
                        || typeOrganisation == ActionGroupeDiscriminationTypeOrganisation.ASSOCIATION_AGREEE_5ANS;

        boolean pluraliteEtablie = nombrePersonnesConcernees >= SEUIL_PLURALITE;

        LocalDate dateRecevabiliteSaisine =
                dateMiseEnDemeure != null ? dateMiseEnDemeure.plusMonths(DELAI_CARENCE_MOIS) : null;
        boolean delaiCarenceRespecte =
                dateRecevabiliteSaisine != null && !today.isBefore(dateRecevabiliteSaisine);

        ActionGroupeDiscriminationVerdict verdict = computeVerdict(
                qualiteAAgir, pluraliteEtablie, dateMiseEnDemeure, delaiCarenceRespecte);

        List<ActionGroupeDiscriminationChecklistItem> checklist = buildChecklist(
                qualiteAAgir, pluraliteEtablie, dateMiseEnDemeure, delaiCarenceRespecte);

        return new ActionGroupeDiscriminationResult(
                typeOrganisation,
                motifDiscrimination,
                nombrePersonnesConcernees,
                objet,
                dateMiseEnDemeure,
                qualiteAAgir,
                pluraliteEtablie,
                dateRecevabiliteSaisine,
                delaiCarenceRespecte,
                verdict,
                checklist,
                BASE_JURIDIQUE);
    }

    private static ActionGroupeDiscriminationVerdict computeVerdict(
            boolean qualiteAAgir,
            boolean pluraliteEtablie,
            LocalDate dateMiseEnDemeure,
            boolean delaiCarenceRespecte) {
        if (!qualiteAAgir) {
            return ActionGroupeDiscriminationVerdict.IRRECEVABLE_QUALITE;
        }
        if (dateMiseEnDemeure == null) {
            return ActionGroupeDiscriminationVerdict.INFO_MANQUANTE;
        }
        if (!delaiCarenceRespecte) {
            return ActionGroupeDiscriminationVerdict.PREMATURE;
        }
        if (!pluraliteEtablie) {
            // Qualité et carence OK mais pluralité non établie : l'action de groupe
            // suppose plusieurs situations similaires ; à défaut elle est prématurée
            // sur le plan de la condition de pluralité (saisine non encore caractérisée).
            return ActionGroupeDiscriminationVerdict.INFO_MANQUANTE;
        }
        return ActionGroupeDiscriminationVerdict.RECEVABLE;
    }

    private static List<ActionGroupeDiscriminationChecklistItem> buildChecklist(
            boolean qualiteAAgir,
            boolean pluraliteEtablie,
            LocalDate dateMiseEnDemeure,
            boolean delaiCarenceRespecte) {
        List<ActionGroupeDiscriminationChecklistItem> items = new ArrayList<>();

        items.add(new ActionGroupeDiscriminationChecklistItem(
                qualiteAAgir
                        ? "Qualité à agir établie : organisation habilitée à exercer l'action de groupe "
                                + "(syndicat représentatif ou association déclarée depuis ≥ 5 ans)"
                        : "Qualité à agir NON établie : seuls un syndicat représentatif ou une association "
                                + "régulièrement déclarée depuis au moins 5 ans luttant contre les "
                                + "discriminations peuvent exercer l'action de groupe",
                true,
                !qualiteAAgir,
                "L. 1134-7 Code travail"));

        boolean miseEnDemeureAbsente = dateMiseEnDemeure == null;
        items.add(new ActionGroupeDiscriminationChecklistItem(
                miseEnDemeureAbsente
                        ? "Adresser à l'employeur une mise en demeure écrite de faire cesser le manquement : "
                                + "préalable obligatoire à la saisine (aucune mise en demeure renseignée)"
                        : "Mise en demeure écrite de l'employeur de faire cesser le manquement adressée",
                true,
                miseEnDemeureAbsente,
                "L. 1134-9 Code travail"));

        items.add(new ActionGroupeDiscriminationChecklistItem(
                delaiCarenceRespecte
                        ? "Délai de carence de 6 mois après mise en demeure écoulé : la saisine est possible"
                        : "Respecter le délai de carence de 6 mois après mise en demeure avant toute saisine "
                                + "(saisine prématurée tant que ce délai n'est pas écoulé)",
                true,
                !delaiCarenceRespecte,
                "L. 1134-9 Code travail"));

        items.add(new ActionGroupeDiscriminationChecklistItem(
                pluraliteEtablie
                        ? "Pluralité établie : plusieurs personnes placées dans une situation similaire de "
                                + "discrimination"
                        : "Caractériser la pluralité : l'action de groupe suppose plusieurs candidats ou "
                                + "salariés placés dans une situation similaire",
                true,
                !pluraliteEtablie,
                "L. 1134-7 ; L. 1132-1 Code travail"));

        items.add(new ActionGroupeDiscriminationChecklistItem(
                "Saisir le tribunal judiciaire de l'action de groupe (action de groupe judiciaire) après "
                        + "expiration du délai de carence",
                true,
                false,
                "L. 1134-7 et s. Code travail"));

        items.add(new ActionGroupeDiscriminationChecklistItem(
                "Anticiper la phase de réparation individuelle ultérieure des préjudices subis par chaque "
                        + "personne concernée",
                false,
                false,
                "L. 1134-10 Code travail"));

        return items;
    }

    private static void validate(
            ActionGroupeDiscriminationTypeOrganisation typeOrganisation,
            ActionGroupeDiscriminationMotif motifDiscrimination,
            int nombrePersonnesConcernees,
            LocalDate dateMiseEnDemeure,
            LocalDate today) {
        if (typeOrganisation == null) {
            throw new IllegalArgumentException("typeOrganisation est requis");
        }
        if (motifDiscrimination == null) {
            throw new IllegalArgumentException("motifDiscrimination est requis");
        }
        if (nombrePersonnesConcernees < 1) {
            throw new IllegalArgumentException(
                    "nombrePersonnesConcernees doit être supérieur ou égal à 1");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateMiseEnDemeure != null && dateMiseEnDemeure.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateMiseEnDemeure ne peut pas être dans le futur");
        }
    }
}

package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-217-04 : arbre décisionnel de l'autorité parentale belge (CC art. 374-375).
 *
 * <p>L'autorité parentale est <b>conjointe par principe</b> dès lors que la
 * filiation est établie à l'égard des deux parents (CC art. 374 §1) — elle se
 * maintient après séparation, indépendamment du mode d'hébergement. L'autorité
 * <b>exclusive</b> ne peut être confiée à un parent que par décision du Tribunal
 * de la famille, dans l'intérêt de l'enfant, et suppose un motif grave
 * caractérisé (désintérêt durable, mise en danger, incapacité — CC art. 374 §1
 * al. 2 / art. 375).</p>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. L'autorité parentale française repose sur
 * des mécaniques distinctes (F-FA-19) — pas de réutilisation possible.</p>
 *
 * <p><b>Validation juridique requise</b> : les articles CC art. 374-375 reflètent
 * l'état du droit connu du modèle et doivent être relus par un avocat belge avant
 * mise en production. Le contenu juridique (articles, règles, libellés) est
 * centralisé ici (source unique de vérité).</p>
 */
public final class AutoriteParentaleBeCalculator {

    /** Verdict de qualification du régime d'autorité parentale. */
    public enum Verdict {
        AUTORITE_CONJOINTE,
        AUTORITE_EXCLUSIVE_FONDEE,
        AUTORITE_EXCLUSIVE_NON_FONDEE,
        QUALIFICATION_INCOMPLETE
    }

    /** Voie procédurale recommandée. */
    public enum VoieProcedurale {
        AUCUNE_AUTORITE_CONJOINTE_DROIT,
        ACCORD_HOMOLOGUE_TF,
        REQUETE_TRIBUNAL_FAMILLE,
        ETABLISSEMENT_FILIATION_PREALABLE
    }

    /** Mode d'hébergement principal de l'enfant. */
    public enum ModeHebergement {
        HEBERGEMENT_EGALITAIRE,
        HEBERGEMENT_PRINCIPAL_UN_PARENT,
        HEBERGEMENT_NON_FIXE
    }

    /** Code structuré d'un facteur retenu. */
    public enum FacteurCode {
        DESINTERET_DURABLE,
        MISE_EN_DANGER,
        INCAPACITE_PARENT,
        ACCORD_PARENTAL,
        DECISION_ANTERIEURE
    }

    /** Facteur retenu — structure exposée dans la réponse API. */
    public record Facteur(
            FacteurCode code,
            String libelle,
            String fondement,
            boolean favorableExclusive,
            String explication
    ) {}

    private AutoriteParentaleBeCalculator() {}

    /**
     * Applique l'arbre décisionnel de l'autorité parentale belge.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, voie procédurale, facteurs, bases)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static AutoriteParentaleBeResult compute(AutoriteParentaleBeInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Autorité parentale — outil disponible uniquement en BELGIQUE");
        }
        requireFlag(input.filiationEtablieDeuxParents(), "filiationEtablieDeuxParents");
        requireFlag(input.accordParentalExiste(), "accordParentalExiste");
        requireFlag(input.demandeAutoriteExclusive(), "demandeAutoriteExclusive");
        requireFlag(input.desinteretDurableParent(), "desinteretDurableParent");
        requireFlag(input.miseEnDangerEnfant(), "miseEnDangerEnfant");
        requireFlag(input.incapaciteParent(), "incapaciteParent");
        requireFlag(input.decisionJudiciaireAnterieure(), "decisionJudiciaireAnterieure");
        if (input.modeHebergementPrincipal() == null) {
            throw new IllegalArgumentException("modeHebergementPrincipal est requis");
        }
        validateCommentaire(input.commentaire());

        boolean filiation = Boolean.TRUE.equals(input.filiationEtablieDeuxParents());
        boolean demandeExclusive = Boolean.TRUE.equals(input.demandeAutoriteExclusive());
        boolean accord = Boolean.TRUE.equals(input.accordParentalExiste());
        boolean motifGrave = Boolean.TRUE.equals(input.desinteretDurableParent())
                || Boolean.TRUE.equals(input.miseEnDangerEnfant())
                || Boolean.TRUE.equals(input.incapaciteParent());

        List<Facteur> facteurs = detecterFacteurs(input);

        Verdict verdict;
        VoieProcedurale voie;
        if (!filiation) {
            verdict = Verdict.QUALIFICATION_INCOMPLETE;
            voie = VoieProcedurale.ETABLISSEMENT_FILIATION_PREALABLE;
        } else if (!demandeExclusive) {
            verdict = Verdict.AUTORITE_CONJOINTE;
            voie = accord ? VoieProcedurale.ACCORD_HOMOLOGUE_TF
                    : VoieProcedurale.AUCUNE_AUTORITE_CONJOINTE_DROIT;
        } else if (motifGrave) {
            verdict = Verdict.AUTORITE_EXCLUSIVE_FONDEE;
            voie = VoieProcedurale.REQUETE_TRIBUNAL_FAMILLE;
        } else {
            verdict = Verdict.AUTORITE_EXCLUSIVE_NON_FONDEE;
            voie = VoieProcedurale.AUCUNE_AUTORITE_CONJOINTE_DROIT;
        }

        List<String> bases = basesJuridiques(verdict);
        List<String> messages = construireMessages(input, verdict, voie);

        return new AutoriteParentaleBeResult(
                verdict, voie, List.copyOf(facteurs), bases, messages, countryNormalized);
    }

    private static void requireFlag(Boolean value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " est requis");
        }
    }

    private static void validateCommentaire(String commentaire) {
        if (commentaire != null && commentaire.length() > 1000) {
            throw new IllegalArgumentException("Le commentaire ne peut dépasser 1000 caractères");
        }
    }

    /** Détecte les facteurs retenus selon les critères cochés (ordre stable). */
    private static List<Facteur> detecterFacteurs(AutoriteParentaleBeInput in) {
        List<Facteur> facteurs = new ArrayList<>();

        if (Boolean.TRUE.equals(in.desinteretDurableParent())) {
            facteurs.add(new Facteur(
                    FacteurCode.DESINTERET_DURABLE,
                    "Désintérêt durable d'un parent invoqué",
                    "CC art. 374 §1 al. 2 (dérogation à l'exercice conjoint dans l'intérêt de l'enfant)",
                    true,
                    "Le désintérêt durable d'un parent pour l'enfant est un motif grave susceptible "
                            + "de fonder l'attribution de l'autorité parentale exclusive à l'autre parent."));
        }
        if (Boolean.TRUE.equals(in.miseEnDangerEnfant())) {
            facteurs.add(new Facteur(
                    FacteurCode.MISE_EN_DANGER,
                    "Mise en danger de l'enfant invoquée",
                    "CC art. 374 §1 al. 2 (dérogation à l'exercice conjoint dans l'intérêt de l'enfant)",
                    true,
                    "La mise en danger de l'enfant est un motif grave susceptible de fonder le retrait "
                            + "de l'exercice conjoint et l'attribution de l'autorité exclusive à l'autre parent."));
        }
        if (Boolean.TRUE.equals(in.incapaciteParent())) {
            facteurs.add(new Facteur(
                    FacteurCode.INCAPACITE_PARENT,
                    "Incapacité d'un parent à exercer l'autorité parentale",
                    "CC art. 374 §1 al. 2 et art. 375 (incapacité — dérogation, déchéance)",
                    true,
                    "L'incapacité durable d'un parent (état de santé, éloignement, situation personnelle) "
                            + "peut justifier l'exercice exclusif de l'autorité parentale par l'autre parent."));
        }
        if (Boolean.TRUE.equals(in.accordParentalExiste())) {
            facteurs.add(new Facteur(
                    FacteurCode.ACCORD_PARENTAL,
                    "Accord parental sur les modalités d'exercice",
                    "CC art. 374 §1 (accord des parents homologué par le Tribunal de la famille)",
                    false,
                    "Un accord entre les parents peut être homologué par le Tribunal de la famille : "
                            + "l'homologation lui confère force exécutoire et sécurise les modalités convenues."));
        }
        if (Boolean.TRUE.equals(in.decisionJudiciaireAnterieure())) {
            facteurs.add(new Facteur(
                    FacteurCode.DECISION_ANTERIEURE,
                    "Décision judiciaire antérieure sur l'autorité parentale",
                    "CC art. 387bis (révision de toute décision dans l'intérêt de l'enfant)",
                    false,
                    "Une décision antérieure peut être révisée par le Tribunal de la famille si un "
                            + "élément nouveau le justifie dans l'intérêt de l'enfant."));
        }
        return facteurs;
    }

    private static List<String> basesJuridiques(Verdict verdict) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("CC art. 374 §1 (autorité parentale exercée conjointement par les père et mère)");
        if (verdict == Verdict.AUTORITE_EXCLUSIVE_FONDEE
                || verdict == Verdict.AUTORITE_EXCLUSIVE_NON_FONDEE) {
            bases.add("CC art. 374 §1 al. 2 (le Tribunal de la famille peut confier l'exercice exclusif à un parent)");
            bases.add("CC art. 375 (déchéance de l'autorité parentale — cas extrêmes)");
        }
        if (verdict == Verdict.QUALIFICATION_INCOMPLETE) {
            bases.add("CC Livre 2 (établissement de la filiation — préalable à l'autorité parentale)");
        }
        return new ArrayList<>(bases);
    }

    private static List<String> construireMessages(AutoriteParentaleBeInput in,
                                                   Verdict verdict,
                                                   VoieProcedurale voie) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case AUTORITE_CONJOINTE -> {
                msgs.add("L'autorité parentale conjointe est le principe (CC art. 374 §1) : elle se "
                        + "maintient même après séparation, indépendamment du mode d'hébergement.");
                if (voie == VoieProcedurale.ACCORD_HOMOLOGUE_TF) {
                    msgs.add("Un accord parental existe : le faire homologuer par le Tribunal de la "
                            + "famille pour lui donner force exécutoire.");
                }
            }
            case AUTORITE_EXCLUSIVE_FONDEE -> msgs.add(
                    "Un motif grave est caractérisé : la demande d'autorité exclusive devant le "
                            + "Tribunal de la famille est plaidable — réunir les pièces établissant le motif.");
            case AUTORITE_EXCLUSIVE_NON_FONDEE -> msgs.add(
                    "Aucun motif grave n'est caractérisé : le Tribunal de la famille maintiendra "
                            + "l'autorité parentale conjointe (CC art. 374 §1 — principe légal).");
            case QUALIFICATION_INCOMPLETE -> msgs.add(
                    "La filiation n'est pas établie à l'égard des deux parents : l'autorité parentale "
                            + "partagée ne peut s'appliquer en l'état — établir la filiation au préalable.");
        }
        if (in.modeHebergementPrincipal() == ModeHebergement.HEBERGEMENT_EGALITAIRE
                && verdict == Verdict.AUTORITE_CONJOINTE) {
            msgs.add("Hébergement égalitaire envisagé : le mode d'hébergement est indépendant de "
                    + "l'exercice de l'autorité parentale (loi du 18/07/2006).");
        }
        return msgs;
    }
}

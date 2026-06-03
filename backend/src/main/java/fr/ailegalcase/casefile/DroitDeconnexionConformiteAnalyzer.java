package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-53 : analyseur de la <b>conformité à l'obligation relative au droit à la
 * déconnexion</b> (art. L.2242-17 7° CT, F-DT-83). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; aucun outil
 * existant ne couvre le droit à la déconnexion) :
 * <ul>
 *   <li><b>Applicabilité</b> — l'obligation de négocier le droit à la déconnexion
 *       dans le cadre de la négociation annuelle obligatoire (NAO) sur l'égalité
 *       professionnelle et la qualité de vie et des conditions de travail (QVCT)
 *       s'applique si {@code effectif >= 50} ET {@code delegueSyndicalPresent}.
 *       À défaut → {@code NON_REQUIS} (l'employeur reste libre d'adopter une
 *       charte).</li>
 *   <li><b>Checklist conformité</b> (si applicable) :
 *     <ul>
 *       <li>{@code OBLIGATION} — accord négocié ou charte employeur présent
 *           ({@code accordOuChartePresent}) ;</li>
 *       <li>{@code PROCEDURE} — plages / modalités de déconnexion définies
 *           ({@code plagesDeconnexionDefinies}) ;</li>
 *       <li>{@code PROCEDURE} — actions de formation / sensibilisation prévues
 *           ({@code actionsSensibilisation}) ;</li>
 *       <li>{@code PROCEDURE} — avis du CSE recueilli en cas de charte
 *           unilatérale ({@code avisCseRecueilliPourCharte}).</li>
 *     </ul>
 *   </li>
 *   <li><b>Verdict</b> — {@code CONFORME} si tous les items applicables sont
 *       remplis ; {@code NON_CONFORME} si au moins un item ne l'est pas ;
 *       {@code NON_REQUIS} si l'obligation n'est pas déclenchée.</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class DroitDeconnexionConformiteAnalyzer {

    /** Seuil d'effectif déclenchant l'obligation de négocier (art. L.2242-17 CT). */
    static final int SEUIL_OBLIGATION_NEGOCIER = 50;

    static final String BASE_JURIDIQUE =
            "art. L.2242-17 7° CT — la négociation annuelle sur l'égalité "
                    + "professionnelle entre les femmes et les hommes et la qualité "
                    + "de vie et des conditions de travail (QVCT) porte notamment sur "
                    + "les modalités du plein exercice par le salarié de son droit à "
                    + "la déconnexion et la mise en place de dispositifs de régulation "
                    + "de l'utilisation des outils numériques ; à défaut d'accord, "
                    + "l'employeur élabore une charte, après avis du CSE, définissant "
                    + "les modalités d'exercice du droit à la déconnexion et prévoyant "
                    + "des actions de formation et de sensibilisation à un usage "
                    + "raisonnable des outils numériques (à vérifier par avocat)";

    private DroitDeconnexionConformiteAnalyzer() {
    }

    /**
     * Analyse la conformité à l'obligation relative au droit à la déconnexion et
     * rend un verdict {@code CONFORME} / {@code NON_CONFORME} / {@code NON_REQUIS}.
     */
    public static DroitDeconnexionConformiteResult analyze(
            Integer effectif,
            Boolean delegueSyndicalPresent,
            Boolean accordOuChartePresent,
            Boolean plagesDeconnexionDefinies,
            Boolean actionsSensibilisation,
            Boolean avisCseRecueilliPourCharte) {

        validate(effectif, delegueSyndicalPresent, accordOuChartePresent,
                plagesDeconnexionDefinies, actionsSensibilisation,
                avisCseRecueilliPourCharte);

        int eff = effectif;
        boolean ds = delegueSyndicalPresent;
        boolean accordOuCharte = accordOuChartePresent;
        boolean plages = plagesDeconnexionDefinies;
        boolean sensibilisation = actionsSensibilisation;
        boolean avisCse = avisCseRecueilliPourCharte;

        boolean obligationDeNegocier = eff >= SEUIL_OBLIGATION_NEGOCIER && ds;

        List<DroitDeconnexionConformiteItem> checklist = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // ── Obligation non applicable : information ──────────────────────────
        if (!obligationDeNegocier) {
            checklist.add(new DroitDeconnexionConformiteItem(
                    "Obligation de négocier le droit à la déconnexion non applicable",
                    true,
                    "INFORMATION",
                    "L'obligation de négocier le droit à la déconnexion dans la "
                            + "négociation annuelle QVCT suppose un effectif d'au "
                            + "moins 50 salariés ET la présence d'au moins un délégué "
                            + "syndical (art. L.2242-1 et L.2242-17 7° CT). L'entreprise "
                            + "n'atteint pas ce seuil ou ne dispose pas de délégué "
                            + "syndical : l'employeur reste libre d'adopter une charte "
                            + "(à vérifier par avocat)."));
            notes.add("Obligation de négociation non déclenchée : entreprise de "
                    + "moins de 50 salariés ou absence de délégué syndical "
                    + "(art. L.2242-17 7° CT).");

            return new DroitDeconnexionConformiteResult(
                    eff, ds, accordOuCharte, plages, sensibilisation, avisCse,
                    false, List.copyOf(checklist), 0,
                    DroitDeconnexionConformiteStatut.NON_REQUIS,
                    List.copyOf(notes), BASE_JURIDIQUE);
        }

        // ── Checklist conformité (obligation applicable) ────────────────────
        int itemsManquants = 0;

        // Item 1 — accord ou charte (OBLIGATION)
        if (!accordOuCharte) {
            itemsManquants++;
        }
        checklist.add(new DroitDeconnexionConformiteItem(
                "Accord négocié ou charte employeur sur le droit à la déconnexion",
                accordOuCharte,
                "OBLIGATION",
                "Art. L.2242-17 7° CT : le droit à la déconnexion doit être négocié "
                        + "dans la NAO QVCT ; à défaut d'accord, l'employeur élabore "
                        + "une charte. " + (accordOuCharte
                        ? "Un accord ou une charte est en place."
                        : "Aucun accord ni charte détecté — obligation non remplie "
                                + "(à vérifier par avocat).")));

        // Item 2 — plages / modalités de déconnexion (PROCEDURE)
        if (!plages) {
            itemsManquants++;
        }
        checklist.add(new DroitDeconnexionConformiteItem(
                "Plages / modalités d'exercice du droit à la déconnexion définies",
                plages,
                "PROCEDURE",
                "L'accord ou la charte doit définir les modalités d'exercice du "
                        + "droit à la déconnexion (dispositifs de régulation de "
                        + "l'usage des outils numériques). " + (plages
                        ? "Des plages / modalités sont définies."
                        : "Aucune plage / modalité définie — point non satisfait "
                                + "(à vérifier par avocat).")));

        // Item 3 — actions de sensibilisation (PROCEDURE)
        if (!sensibilisation) {
            itemsManquants++;
        }
        checklist.add(new DroitDeconnexionConformiteItem(
                "Actions de formation et de sensibilisation à un usage raisonnable "
                        + "des outils numériques",
                sensibilisation,
                "PROCEDURE",
                "Art. L.2242-17 7° CT : la charte prévoit la mise en œuvre, à "
                        + "destination des salariés et de l'encadrement, d'actions de "
                        + "formation et de sensibilisation. " + (sensibilisation
                        ? "Des actions de sensibilisation sont prévues."
                        : "Aucune action de sensibilisation prévue — point non "
                                + "satisfait (à vérifier par avocat).")));

        // Item 4 — avis du CSE en cas de charte (PROCEDURE)
        if (!avisCse) {
            itemsManquants++;
        }
        checklist.add(new DroitDeconnexionConformiteItem(
                "Avis du CSE recueilli avant l'élaboration de la charte",
                avisCse,
                "PROCEDURE",
                "À défaut d'accord, la charte est élaborée après avis du CSE "
                        + "(art. L.2242-17 7° CT). En présence d'un accord négocié, "
                        + "cet avis n'est pas requis ; vérifier le mode d'adoption. "
                        + (avisCse
                        ? "L'avis du CSE a été recueilli."
                        : "Avis du CSE non recueilli — point non satisfait pour une "
                                + "charte unilatérale (à vérifier par avocat).")));

        DroitDeconnexionConformiteStatut statut = itemsManquants == 0
                ? DroitDeconnexionConformiteStatut.CONFORME
                : DroitDeconnexionConformiteStatut.NON_CONFORME;

        notes.add(statut == DroitDeconnexionConformiteStatut.CONFORME
                ? "Obligation relative au droit à la déconnexion satisfaite : tous "
                        + "les items de la checklist sont remplis (art. L.2242-17 7° "
                        + "CT)."
                : "Obligation relative au droit à la déconnexion NON satisfaite : "
                        + itemsManquants + " item(s) manquant(s) (art. L.2242-17 7° "
                        + "CT).");

        return new DroitDeconnexionConformiteResult(
                eff, ds, accordOuCharte, plages, sensibilisation, avisCse,
                true, List.copyOf(checklist), itemsManquants, statut,
                List.copyOf(notes), BASE_JURIDIQUE);
    }

    private static void validate(Integer effectif,
                                 Boolean delegueSyndicalPresent,
                                 Boolean accordOuChartePresent,
                                 Boolean plagesDeconnexionDefinies,
                                 Boolean actionsSensibilisation,
                                 Boolean avisCseRecueilliPourCharte) {
        if (effectif == null) {
            throw new IllegalArgumentException("effectif est requis");
        }
        if (effectif <= 0) {
            throw new IllegalArgumentException("effectif doit être strictement positif");
        }
        if (delegueSyndicalPresent == null) {
            throw new IllegalArgumentException("delegueSyndicalPresent est requis");
        }
        if (accordOuChartePresent == null) {
            throw new IllegalArgumentException("accordOuChartePresent est requis");
        }
        if (plagesDeconnexionDefinies == null) {
            throw new IllegalArgumentException("plagesDeconnexionDefinies est requis");
        }
        if (actionsSensibilisation == null) {
            throw new IllegalArgumentException("actionsSensibilisation est requis");
        }
        if (avisCseRecueilliPourCharte == null) {
            throw new IllegalArgumentException("avisCseRecueilliPourCharte est requis");
        }
    }
}

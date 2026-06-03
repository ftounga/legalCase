package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-43 : analyseur du <b>congé pour évènement familial</b> (art. L.3142-1 à
 * L.3142-5 CT, F-DT-76). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; distinct du
 * congé de paternité/maternité F-212 et du congé parental d'éducation F-DT-78) :
 * <ul>
 *   <li><b>Durée légale (L.3142-4)</b> — mariage/PACS du salarié : 4 jours ;
 *       naissance/adoption : 3 jours ; décès d'un enfant : 5 jours (porté à
 *       7 jours ouvrés dans les cas renforcés — durée majorée possible signalée) ;
 *       décès du conjoint/partenaire/concubin : 3 jours ; décès du père, de la
 *       mère, beau-père, belle-mère, frère, sœur : 3 jours ; annonce d'un
 *       handicap, d'un cancer ou d'une pathologie chronique chez un enfant :
 *       2 jours ; déménagement : 0 jour légal (renvoi CCN).</li>
 *   <li><b>Comparaison loi / CCN (L.3142-5)</b> — si {@code conventionPlusFavorable}
 *       ET {@code dureeConventionnelleJours != null} ET
 *       {@code dureeConventionnelleJours > dureeLegaleJours} →
 *       {@code dureeApplicableJours = dureeConventionnelleJours},
 *       {@code base = CONVENTIONNELLE} ; sinon {@code dureeApplicableJours =
 *       dureeLegaleJours}, {@code base = LEGALE}.</li>
 *   <li><b>Maintien salaire (L.3142-2 / L.3142-3)</b> — {@code maintienSalaire =
 *       true} : congé assimilé à du temps de travail effectif, pas de retenue de
 *       salaire ni de réduction des droits à congés payés.</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class CongesEvenementsFamiliauxAnalyzer {

    static final String BASE_JURIDIQUE =
            "art. L.3142-1 à L.3142-5 du Code du travail — congés pour évènements "
                    + "familiaux : durées légales minimales (art. L.3142-4 : "
                    + "mariage/PACS du salarié "
                    + "4 jours ; naissance/adoption 3 jours ; décès d'un enfant 5 jours, "
                    + "porté à 7 jours ouvrés dans les cas renforcés ; décès du conjoint, "
                    + "partenaire de PACS, concubin, père, mère, beau-père, belle-mère, "
                    + "frère, sœur 3 jours ; annonce d'un handicap, d'un cancer ou d'une "
                    + "pathologie chronique chez un enfant 2 jours) ; ces congés sont "
                    + "assimilés à du temps de travail effectif (maintien intégral du "
                    + "salaire, pas de réduction des droits à congés payés) ; une "
                    + "convention ou un accord collectif peut prévoir une durée plus "
                    + "favorable, qui l'emporte alors (à vérifier par avocat)";

    private CongesEvenementsFamiliauxAnalyzer() {
    }

    /**
     * Détermine la durée légale minimale (jours) pour un évènement familial donné.
     */
    static int dureeLegaleJours(CongesEvenementsFamiliauxTypeEvenement type) {
        return switch (type) {
            case MARIAGE_PACS -> 4;
            case NAISSANCE -> 3;
            case DECES_ENFANT -> 5;
            case DECES_CONJOINT_PARTENAIRE -> 3;
            case DECES_PERE_MERE -> 3;
            case ANNONCE_HANDICAP_ENFANT -> 2;
            case DEMENAGEMENT_NON_LEGAL -> 0;
        };
    }

    /**
     * Analyse la durée de congé applicable, la base de calcul retenue et le
     * maintien de salaire pour un évènement familial.
     */
    public static CongesEvenementsFamiliauxResult analyze(
            CongesEvenementsFamiliauxTypeEvenement typeEvenement,
            Boolean conventionPlusFavorable,
            Integer dureeConventionnelleJours) {

        validate(typeEvenement, conventionPlusFavorable, dureeConventionnelleJours);

        boolean conventionFavorable = conventionPlusFavorable;
        int dureeLegale = dureeLegaleJours(typeEvenement);

        List<String> notes = new ArrayList<>();

        // ── Durée légale selon l'évènement ──────────────────────────────────
        switch (typeEvenement) {
            case MARIAGE_PACS -> notes.add("Mariage ou PACS du salarié : congé légal de "
                    + "4 jours (art. L.3142-4 CT).");
            case NAISSANCE -> notes.add("Naissance ou arrivée d'un enfant adopté : congé "
                    + "légal de 3 jours (art. L.3142-4 CT).");
            case DECES_ENFANT -> notes.add("Décès d'un enfant : congé légal de 5 jours, "
                    + "porté à 7 jours ouvrés si l'enfant a moins de 25 ans, est à charge "
                    + "effective et permanente, ou si le salarié est lui-même parent d'un "
                    + "enfant de moins de 25 ans (art. L.3142-4 CT — durée majorée à "
                    + "vérifier par avocat).");
            case DECES_CONJOINT_PARTENAIRE -> notes.add("Décès du conjoint, du partenaire "
                    + "de PACS ou du concubin : congé légal de 3 jours (art. L.3142-4 CT).");
            case DECES_PERE_MERE -> notes.add("Décès du père, de la mère, du beau-père, de "
                    + "la belle-mère, d'un frère ou d'une sœur : congé légal de 3 jours "
                    + "(art. L.3142-4 CT).");
            case ANNONCE_HANDICAP_ENFANT -> notes.add("Annonce de la survenue d'un "
                    + "handicap, d'un cancer ou d'une pathologie chronique chez un "
                    + "enfant : congé légal de 2 jours (art. L.3142-4 CT).");
            case DEMENAGEMENT_NON_LEGAL -> notes.add("Le déménagement n'ouvre droit à "
                    + "aucun congé légal pour évènement familial : seule une éventuelle "
                    + "disposition conventionnelle (convention ou accord collectif) peut "
                    + "prévoir un tel congé (à vérifier par avocat).");
        }

        boolean dureeMajoreePossible = typeEvenement == CongesEvenementsFamiliauxTypeEvenement.DECES_ENFANT;

        // ── Comparaison loi / convention (L.3142-5) ─────────────────────────
        int dureeApplicable;
        CongesEvenementsFamiliauxBase base;
        if (conventionFavorable && dureeConventionnelleJours != null
                && dureeConventionnelleJours > dureeLegale) {
            dureeApplicable = dureeConventionnelleJours;
            base = CongesEvenementsFamiliauxBase.CONVENTIONNELLE;
            notes.add("La convention collective prévoit une durée plus favorable ("
                    + dureeConventionnelleJours + " jours) que la durée légale ("
                    + dureeLegale + " jours) : la durée conventionnelle est retenue "
                    + "(art. L.3142-5 CT — disposition la plus favorable au salarié).");
        } else {
            dureeApplicable = dureeLegale;
            base = CongesEvenementsFamiliauxBase.LEGALE;
            if (conventionFavorable && dureeConventionnelleJours != null) {
                notes.add("La durée conventionnelle annoncée (" + dureeConventionnelleJours
                        + " jours) n'est pas plus favorable que la durée légale ("
                        + dureeLegale + " jours) : la durée légale est retenue.");
            } else {
                notes.add("Aucune durée conventionnelle plus favorable retenue : la durée "
                        + "légale (" + dureeLegale + " jours) s'applique. Vérifier la "
                        + "convention collective applicable, qui peut prévoir une durée "
                        + "supérieure (à vérifier par avocat).");
            }
        }

        // ── Maintien du salaire (L.3142-2 / L.3142-3) ───────────────────────
        notes.add("Ce congé est assimilé à du temps de travail effectif : le salaire est "
                + "intégralement maintenu et les droits à congés payés ne sont pas réduits "
                + "(art. L.3142-2 et L.3142-3 CT).");

        return new CongesEvenementsFamiliauxResult(
                typeEvenement,
                conventionFavorable,
                dureeConventionnelleJours,
                dureeLegale,
                dureeApplicable,
                base,
                true,
                true,
                dureeMajoreePossible,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(CongesEvenementsFamiliauxTypeEvenement typeEvenement,
                                 Boolean conventionPlusFavorable,
                                 Integer dureeConventionnelleJours) {
        if (typeEvenement == null) {
            throw new IllegalArgumentException("typeEvenement est requis");
        }
        if (conventionPlusFavorable == null) {
            throw new IllegalArgumentException("conventionPlusFavorable est requis");
        }
        if (dureeConventionnelleJours != null && dureeConventionnelleJours <= 0) {
            throw new IllegalArgumentException(
                    "dureeConventionnelleJours doit être strictement positif si fourni");
        }
        if (conventionPlusFavorable && dureeConventionnelleJours == null) {
            throw new IllegalArgumentException(
                    "dureeConventionnelleJours est requis lorsque conventionPlusFavorable = true");
        }
    }
}

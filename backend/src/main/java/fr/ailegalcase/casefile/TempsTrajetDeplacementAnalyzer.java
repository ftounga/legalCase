package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-51 : analyseur de l'outil "Temps de trajet / déplacement professionnel"
 * (art. L.3121-4 CT ; CJUE 10/09/2015 C-266/14 « Tyco », F-DT-81). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ;
 * <b>DISTINCT</b> du remboursement de frais de déplacement et de l'astreinte) :
 * <ul>
 *   <li><b>ITINERANT_SANS_LIEU_FIXE</b> → qualification TEMPS_TRAVAIL : pour un
 *       salarié sans lieu de travail fixe, le déplacement domicile–premier/dernier
 *       client constitue du temps de travail effectif (CJUE C-266/14) ; déjà
 *       rémunéré comme tel → pas de contrepartie distincte due.</li>
 *   <li><b>DOMICILE_TRAVAIL_HABITUEL</b> / <b>DOMICILE_CLIENT_DEPASSEMENT</b> → le
 *       trajet n'est pas du temps de travail effectif (L.3121-4) ; s'il dépasse le
 *       temps normal de trajet, il ouvre droit à une contrepartie (repos /
 *       financière), sauf si une contrepartie est déjà prévue par accord.</li>
 * </ul>
 *
 * <p>depassementMinutes = max(0, quotidien − normal). Base juridique annotée
 * « à vérifier par avocat ».
 */
public final class TempsTrajetDeplacementAnalyzer {

    static final String BASE_JURIDIQUE =
            "art. L.3121-4 du Code du travail — le temps de déplacement professionnel "
                    + "pour se rendre sur le lieu d'exécution du contrat n'est pas un temps "
                    + "de travail effectif ; toutefois, s'il dépasse le temps normal de "
                    + "trajet entre le domicile et le lieu habituel de travail, il fait "
                    + "l'objet d'une contrepartie (repos ou financière). CJUE 10/09/2015, "
                    + "C-266/14 « Tyco » : pour les travailleurs sans lieu de travail fixe, "
                    + "le déplacement quotidien domicile–premier/dernier client constitue du "
                    + "temps de travail. Jurisprudence Cass. soc. — appréciation du "
                    + "dépassement du temps normal de trajet (à vérifier par avocat)";

    static final String NOTE_ITINERANT =
            "Salarié itinérant sans lieu de travail fixe : le déplacement domicile–"
                    + "premier/dernier client peut être qualifié de temps de travail effectif "
                    + "(CJUE C-266/14 ; Cass. soc.). Aucune contrepartie distincte due — le "
                    + "temps est déjà rémunéré comme temps de travail.";

    static final String NOTE_DEPASSEMENT_DUE =
            "Le temps de trajet dépasse le temps normal de trajet : une contrepartie "
                    + "(repos ou financière) est due pour la part excédentaire (art. L.3121-4 "
                    + "CT). Le montant relève de l'accord / de la décision unilatérale et "
                    + "n'est pas recalculé ici.";

    static final String NOTE_DEPASSEMENT_DEJA_PREVUE =
            "Le temps de trajet dépasse le temps normal de trajet, mais une contrepartie "
                    + "(repos / financière) est déjà prévue par accord ou usage : pas de "
                    + "contrepartie supplémentaire due au titre de l'art. L.3121-4 CT.";

    static final String NOTE_SANS_DEPASSEMENT =
            "Le temps de trajet n'excède pas le temps normal de trajet : le trajet n'est "
                    + "pas du temps de travail effectif et n'ouvre droit à aucune contrepartie "
                    + "(art. L.3121-4 CT).";

    private TempsTrajetDeplacementAnalyzer() {
    }

    /**
     * Qualifie le temps de trajet et détermine si une contrepartie est due.
     */
    public static TempsTrajetDeplacementResult analyze(
            TypeTrajet typeTrajet,
            Integer tempsTrajetQuotidienMinutes,
            Integer tempsTrajetNormalMinutes,
            Boolean contrepartiePrevueAccord) {

        validate(typeTrajet, tempsTrajetQuotidienMinutes, tempsTrajetNormalMinutes);

        int quotidien = tempsTrajetQuotidienMinutes;
        int normal = tempsTrajetNormalMinutes;
        boolean prevue = Boolean.TRUE.equals(contrepartiePrevueAccord);
        int depassement = Math.max(0, quotidien - normal);
        List<String> notes = new ArrayList<>();

        if (typeTrajet == TypeTrajet.ITINERANT_SANS_LIEU_FIXE) {
            notes.add(NOTE_ITINERANT);
            return new TempsTrajetDeplacementResult(
                    TempsTrajetQualification.TEMPS_TRAVAIL,
                    typeTrajet, quotidien, normal, prevue, false, depassement,
                    "salarié itinérant sans lieu de travail fixe — temps de travail effectif",
                    List.copyOf(notes), BASE_JURIDIQUE);
        }

        // DOMICILE_TRAVAIL_HABITUEL ou DOMICILE_CLIENT_DEPASSEMENT
        if (depassement <= 0) {
            notes.add(NOTE_SANS_DEPASSEMENT);
            return new TempsTrajetDeplacementResult(
                    TempsTrajetQualification.TRAJET_SANS_CONTREPARTIE,
                    typeTrajet, quotidien, normal, prevue, false, depassement,
                    "trajet " + quotidien + " min ≤ trajet normal " + normal
                            + " min — pas de dépassement",
                    List.copyOf(notes), BASE_JURIDIQUE);
        }

        // dépassement constaté
        boolean due = !prevue;
        notes.add(due ? NOTE_DEPASSEMENT_DUE : NOTE_DEPASSEMENT_DEJA_PREVUE);
        return new TempsTrajetDeplacementResult(
                TempsTrajetQualification.TRAJET_AVEC_CONTREPARTIE,
                typeTrajet, quotidien, normal, prevue, due, depassement,
                "trajet " + quotidien + " min > trajet normal " + normal
                        + " min — dépassement de " + depassement + " min",
                List.copyOf(notes), BASE_JURIDIQUE);
    }

    private static void validate(TypeTrajet typeTrajet,
                                 Integer tempsTrajetQuotidienMinutes,
                                 Integer tempsTrajetNormalMinutes) {
        if (typeTrajet == null) {
            throw new IllegalArgumentException("typeTrajet est requis");
        }
        if (tempsTrajetQuotidienMinutes == null) {
            throw new IllegalArgumentException("tempsTrajetQuotidienMinutes est requis");
        }
        if (tempsTrajetNormalMinutes == null) {
            throw new IllegalArgumentException("tempsTrajetNormalMinutes est requis");
        }
        if (tempsTrajetQuotidienMinutes < 0) {
            throw new IllegalArgumentException(
                    "tempsTrajetQuotidienMinutes doit être supérieur ou égal à 0");
        }
        if (tempsTrajetNormalMinutes < 0) {
            throw new IllegalArgumentException(
                    "tempsTrajetNormalMinutes doit être supérieur ou égal à 0");
        }
    }
}

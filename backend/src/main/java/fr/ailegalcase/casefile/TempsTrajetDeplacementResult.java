package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-51 : résultat interne business de l'outil "Temps de trajet /
 * déplacement professionnel" (art. L.3121-4 CT ; CJUE C-266/14, F-DT-81). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param qualification verdict de qualification du temps de trajet.
 * @param typeTrajet type de trajet saisi.
 * @param tempsTrajetQuotidienMinutes temps de trajet quotidien constaté (minutes).
 * @param tempsTrajetNormalMinutes temps de trajet normal de référence (minutes).
 * @param contrepartiePrevueAccord une contrepartie est déjà prévue par accord.
 * @param contrepartieDue une contrepartie (repos / financière) est due.
 * @param depassementMinutes part du trajet excédant le temps normal (minutes).
 * @param base description de la base d'analyse.
 * @param notes notes / points de vigilance.
 * @param baseJuridique fondements juridiques applicables.
 */
public record TempsTrajetDeplacementResult(
        TempsTrajetQualification qualification,
        TypeTrajet typeTrajet,
        int tempsTrajetQuotidienMinutes,
        int tempsTrajetNormalMinutes,
        boolean contrepartiePrevueAccord,
        boolean contrepartieDue,
        int depassementMinutes,
        String base,
        List<String> notes,
        String baseJuridique
) {}

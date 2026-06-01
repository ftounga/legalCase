package fr.ailegalcase.casefile;

/**
 * SF-218-35 : requête POST pour l'analyse de validité d'un règlement intérieur
 * (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise (requis, &gt; 0). Le règlement
 *        intérieur est obligatoire dès 50 salariés (art. L.1311-2).
 * @param reglementExiste true si un règlement intérieur existe (requis).
 * @param contenuHygieneSecurite true si le RI comporte les mesures d'hygiène et
 *        de sécurité (art. L.1321-1 1° ; requis).
 * @param contenuDiscipline true si le RI comporte les règles générales et
 *        permanentes relatives à la discipline, la nature et l'échelle des
 *        sanctions (art. L.1321-1 2° ; requis).
 * @param contenuDroitsDefense true si le RI comporte les dispositions relatives
 *        aux droits de la défense des salariés (art. L.1321-1 3° ; requis).
 * @param contenuHarcelementAgissements true si le RI rappelle les dispositions
 *        relatives au harcèlement moral, sexuel et aux agissements sexistes
 *        (art. L.1321-2 ; requis).
 * @param clauseAtteinteLibertesNonJustifiee true si le RI stipule une clause
 *        portant atteinte aux droits et libertés non justifiée par la nature de
 *        la tâche ni proportionnée (clause interdite, art. L.1321-3 ; défaut
 *        false).
 * @param clauseSanctionPecuniaire true si le RI stipule une sanction pécuniaire
 *        (interdite, art. L.1331-2 ; défaut false).
 * @param consultationCseRealisee true si le CSE a été consulté avant la mise en
 *        place du RI (art. L.1321-4 ; requis).
 * @param transmissionInspectionTravail true si le RI a été transmis à
 *        l'inspection du travail (art. L.1321-4 ; requis).
 * @param depotGreffeCph true si le RI a été déposé au greffe du conseil de
 *        prud'hommes (art. L.1321-4 ; requis).
 */
public record ReglementInterieurValiditeRequest(
        Integer effectif,
        Boolean reglementExiste,
        Boolean contenuHygieneSecurite,
        Boolean contenuDiscipline,
        Boolean contenuDroitsDefense,
        Boolean contenuHarcelementAgissements,
        Boolean clauseAtteinteLibertesNonJustifiee,
        Boolean clauseSanctionPecuniaire,
        Boolean consultationCseRealisee,
        Boolean transmissionInspectionTravail,
        Boolean depotGreffeCph
) {}

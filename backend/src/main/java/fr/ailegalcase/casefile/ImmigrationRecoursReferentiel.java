package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Référentiel statique des types de recours immigration France + Belgique.
 */
public final class ImmigrationRecoursReferentiel {

    private ImmigrationRecoursReferentiel() {}

    private static final List<RecoursType> ALL_TYPES = List.of(

            // ========== FRANCE ==========

            new RecoursType(
                    "RECOURS_GRACIEUX_PREFET",
                    "Recours gracieux auprès du préfet",
                    "FRANCE",
                    60,
                    "Préfecture du département de résidence",
                    List.of(
                            "Articles L. 211-1 et suivants du CESEDA",
                            "Article L. 432-1 du Code des relations entre le public et l'administration (CRPA)",
                            "Article R. 421-1 du Code de justice administrative"
                    ),
                    List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                    List.of(
                            "Copie de la décision de refus contestée",
                            "Copie du passeport en cours de validité",
                            "Copie du titre de séjour précédent (si applicable)",
                            "Justificatifs appuyant la demande (contrat de travail, attestation d'hébergement, etc.)",
                            "Tout document utile à l'appui du recours"
                    )
            ),
            new RecoursType(
                    "RECOURS_CONTENTIEUX_TA",
                    "Recours contentieux devant le tribunal administratif",
                    "FRANCE",
                    60,
                    "Tribunal administratif territorialement compétent",
                    List.of(
                            "Articles L. 211-1 et suivants du CESEDA",
                            "Article R. 421-1 du Code de justice administrative",
                            "Article R. 776-1 du Code de justice administrative (contentieux des étrangers)",
                            "Article L. 614-1 du CESEDA (OQTF)"
                    ),
                    List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                    List.of(
                            "Copie de la décision de refus contestée",
                            "Copie du passeport en cours de validité",
                            "Copie du titre de séjour précédent (si applicable)",
                            "Justificatif de domicile",
                            "Justificatifs appuyant les moyens de droit",
                            "Bordereau récapitulatif des pièces"
                    )
            ),
            new RecoursType(
                    "RECOURS_CNDA",
                    "Recours devant la Cour nationale du droit d'asile (CNDA)",
                    "FRANCE",
                    30,
                    "Cour nationale du droit d'asile — 35 rue Cuvier, 93558 Montreuil Cedex",
                    List.of(
                            "Articles L. 532-1 et suivants du CESEDA",
                            "Article L. 532-4 du CESEDA (délai de recours)",
                            "Convention de Genève du 28 juillet 1951 relative au statut des réfugiés"
                    ),
                    List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                    List.of(
                            "Copie de la décision de l'OFPRA contestée",
                            "Copie du récépissé de demande d'asile",
                            "Copie du passeport ou document de voyage (si disponible)",
                            "Récit de vie détaillé",
                            "Preuves documentaires à l'appui (articles de presse, rapports pays d'origine, certificats médicaux)",
                            "Bordereau récapitulatif des pièces"
                    )
            ),

            // ========== BELGIQUE ==========

            new RecoursType(
                    "RECOURS_CGRA",
                    "Recours devant le Commissariat général aux réfugiés et apatrides (CGRA)",
                    "BELGIQUE",
                    15,
                    "CGRA — Avenue de la Couronne 145A, 1050 Bruxelles",
                    List.of(
                            "Loi du 15 décembre 1980 sur l'accès au territoire, le séjour, l'établissement et l'éloignement des étrangers",
                            "Arrêté royal du 8 octobre 1981",
                            "Directive 2013/32/UE relative aux procédures d'asile"
                    ),
                    List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                    List.of(
                            "Copie de la décision de l'Office des étrangers contestée",
                            "Copie de l'attestation d'immatriculation (carte orange)",
                            "Copie du passeport ou document de voyage (si disponible)",
                            "Questionnaire CGRA complété",
                            "Preuves documentaires à l'appui",
                            "Inventaire des pièces"
                    )
            ),
            new RecoursType(
                    "RECOURS_CCE",
                    "Recours devant le Conseil du contentieux des étrangers (CCE)",
                    "BELGIQUE",
                    30,
                    "CCE — Rue Gaucheret 92-94, 1030 Bruxelles",
                    List.of(
                            "Loi du 15 décembre 1980, articles 39/1 et suivants",
                            "Arrêté royal du 8 octobre 1981",
                            "Loi du 15 décembre 1980, article 39/57-1 (procédure de plein contentieux)"
                    ),
                    List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                    List.of(
                            "Copie de la décision contestée (refus de séjour, OQT, interdiction d'entrée)",
                            "Copie du titre de séjour ou attestation d'immatriculation",
                            "Copie du passeport en cours de validité",
                            "Justificatifs appuyant les moyens de droit",
                            "Preuve de paiement du droit de rôle (186 €)",
                            "Inventaire des pièces"
                    )
            ),
            new RecoursType(
                    "RECOURS_CE_BELGIQUE",
                    "Recours devant le Conseil d'État (cassation administrative)",
                    "BELGIQUE",
                    30,
                    "Conseil d'État — Rue de la Science 33, 1040 Bruxelles",
                    List.of(
                            "Lois coordonnées du 12 janvier 1973 sur le Conseil d'État, article 14",
                            "Loi du 15 décembre 1980, article 39/67"
                    ),
                    List.of("EN_TETE", "OBJET", "VISA_TEXTES", "EXPOSE_FAITS", "MOYENS_DROIT", "CONCLUSIONS", "PIECES"),
                    List.of(
                            "Copie de l'arrêt du CCE contesté",
                            "Copie de la décision administrative initiale",
                            "Copie du passeport en cours de validité",
                            "Mémoire en cassation",
                            "Preuve de paiement du droit de rôle",
                            "Inventaire des pièces"
                    )
            )
    );

    public static List<RecoursType> getTypes(String country) {
        return ALL_TYPES.stream()
                .filter(t -> t.country().equals(country))
                .toList();
    }

    public static RecoursType getByCode(String code) {
        return ALL_TYPES.stream()
                .filter(t -> t.code().equals(code))
                .findFirst()
                .orElse(null);
    }

    public static List<RecoursType> getAll() {
        return ALL_TYPES;
    }

    public static boolean isTypeValid(String code) {
        return ALL_TYPES.stream().anyMatch(t -> t.code().equals(code));
    }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country) || "BELGIQUE".equals(country);
    }
}

package fr.ailegalcase.casefile;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * F-243 / SF-243-01 — Référentiel des stades procéduraux.
 *
 * <p>Nomenclature procédurale technique <strong>stable</strong> (juridictions / stades /
 * positions juridiques), encodée en constantes Java. Ce n'est <em>pas</em> un référentiel
 * métier {@code legal_referentials} : non paramétrable par workspace, non éditable par un
 * administrateur métier. D'où le nom {@code Catalog} et non {@code Referential}.
 *
 * <p>Le catalogue est indexé par couple {@code (domain, country)} — 6 combinaisons exhaustives :
 * {@code DROIT_DU_TRAVAIL|DROIT_IMMIGRATION|DROIT_FAMILLE} × {@code FRANCE|BELGIQUE}.
 *
 * <p>Hiérarchie : une {@link Stage} référence une {@link Jurisdiction} parente ;
 * une {@link Position} référence un ou plusieurs {@link Stage} parents.
 */
public final class ProcedureStageCatalog {

    private ProcedureStageCatalog() {
    }

    /** Domaine juridique du dossier. */
    public static final String DROIT_DU_TRAVAIL = "DROIT_DU_TRAVAIL";
    public static final String DROIT_IMMIGRATION = "DROIT_IMMIGRATION";
    public static final String DROIT_FAMILLE = "DROIT_FAMILLE";

    /** Pays du workspace. */
    public static final String FRANCE = "FRANCE";
    public static final String BELGIQUE = "BELGIQUE";

    private static final Set<String> DOMAINS = Set.of(DROIT_DU_TRAVAIL, DROIT_IMMIGRATION, DROIT_FAMILLE);
    private static final Set<String> COUNTRIES = Set.of(FRANCE, BELGIQUE);

    /** Une juridiction : code stable + libellé humain. */
    public record Jurisdiction(String code, String label) {
    }

    /** Un stade procédural : code, libellé, code de la juridiction parente. */
    public record Stage(String code, String label, String jurisdictionCode) {
    }

    /** Une position juridique : code, libellé, codes des stades pour lesquels elle est valide. */
    public record Position(String code, String label, List<String> stageCodes) {
    }

    /** Sous-référentiel applicable pour un couple (domaine, pays). */
    public record CatalogEntry(List<Jurisdiction> jurisdictions, List<Stage> stages, List<Position> positions) {
    }

    private static final Map<String, CatalogEntry> CATALOG = new LinkedHashMap<>();

    private static String key(String domain, String country) {
        return domain + "|" + country;
    }

    private static void register(String domain, String country, CatalogEntry entry) {
        CATALOG.put(key(domain, country), entry);
    }

    static {
        // ===================== DROIT_DU_TRAVAIL × FRANCE =====================
        register(DROIT_DU_TRAVAIL, FRANCE, new CatalogEntry(
                List.of(
                        new Jurisdiction("CPH", "Conseil de prud'hommes"),
                        new Jurisdiction("CA_SOC", "Cour d'appel — chambre sociale"),
                        new Jurisdiction("CASS_SOC", "Cour de cassation — chambre sociale")
                ),
                List.of(
                        new Stage("BCO", "Bureau de conciliation et d'orientation", "CPH"),
                        new Stage("FOND", "Bureau de jugement (fond)", "CPH"),
                        new Stage("REFERE", "Référé prud'homal", "CPH"),
                        new Stage("DEPARTAGE", "Audience de départage", "CPH"),
                        new Stage("APPEL", "Appel", "CA_SOC"),
                        new Stage("POURVOI", "Pourvoi en cassation", "CASS_SOC")
                ),
                List.of(
                        new Position("DEMANDEUR", "Demandeur (salarié)", List.of("BCO", "FOND", "REFERE", "DEPARTAGE")),
                        new Position("DEFENDEUR", "Défendeur (employeur)", List.of("BCO", "FOND", "REFERE", "DEPARTAGE")),
                        new Position("APPELANT", "Appelant", List.of("APPEL")),
                        new Position("INTIME", "Intimé", List.of("APPEL")),
                        new Position("DEMANDEUR_POURVOI", "Demandeur au pourvoi", List.of("POURVOI")),
                        new Position("DEFENDEUR_POURVOI", "Défendeur au pourvoi", List.of("POURVOI"))
                )
        ));

        // ===================== DROIT_DU_TRAVAIL × BELGIQUE =====================
        register(DROIT_DU_TRAVAIL, BELGIQUE, new CatalogEntry(
                List.of(
                        new Jurisdiction("TT", "Tribunal du travail"),
                        new Jurisdiction("CT", "Cour du travail"),
                        new Jurisdiction("CASS_BE", "Cour de cassation")
                ),
                List.of(
                        new Stage("FOND", "Fond", "TT"),
                        new Stage("REFERE", "Référé — président", "TT"),
                        new Stage("APPEL", "Appel", "CT"),
                        new Stage("POURVOI", "Pourvoi en cassation", "CASS_BE")
                ),
                List.of(
                        new Position("DEMANDEUR", "Demandeur", List.of("FOND", "REFERE")),
                        new Position("DEFENDEUR", "Défendeur", List.of("FOND", "REFERE")),
                        new Position("APPELANT", "Appelant", List.of("APPEL")),
                        new Position("INTIME", "Intimé", List.of("APPEL")),
                        new Position("DEMANDEUR_POURVOI", "Demandeur au pourvoi", List.of("POURVOI")),
                        new Position("DEFENDEUR_POURVOI", "Défendeur au pourvoi", List.of("POURVOI"))
                )
        ));

        // ===================== DROIT_IMMIGRATION × FRANCE =====================
        register(DROIT_IMMIGRATION, FRANCE, new CatalogEntry(
                List.of(
                        new Jurisdiction("TA", "Tribunal administratif"),
                        new Jurisdiction("CAA", "Cour administrative d'appel"),
                        new Jurisdiction("CE", "Conseil d'État"),
                        new Jurisdiction("CNDA", "Cour nationale du droit d'asile"),
                        new Jurisdiction("PREF", "Préfecture / OFII")
                ),
                List.of(
                        new Stage("RECOURS_OQTF", "Recours contre OQTF", "TA"),
                        new Stage("REFERE_LIBERTE", "Référé-liberté", "TA"),
                        new Stage("REFERE_SUSPENSION", "Référé-suspension", "TA"),
                        new Stage("RECOURS_TITRE", "Recours refus titre / regroupement", "TA"),
                        new Stage("APPEL", "Appel", "CAA"),
                        new Stage("CASSATION", "Cassation", "CE"),
                        new Stage("RECOURS_ASILE", "Recours asile", "CNDA"),
                        new Stage("DEMANDE_TITRE", "Demande d'admission au séjour — hors contentieux", "PREF")
                ),
                List.of(
                        new Position("REQUERANT", "Requérant",
                                List.of("RECOURS_OQTF", "REFERE_LIBERTE", "REFERE_SUSPENSION",
                                        "RECOURS_TITRE", "APPEL", "CASSATION", "RECOURS_ASILE")),
                        new Position("DEMANDEUR_TITRE", "Demandeur de titre", List.of("DEMANDE_TITRE"))
                )
        ));

        // ===================== DROIT_IMMIGRATION × BELGIQUE =====================
        register(DROIT_IMMIGRATION, BELGIQUE, new CatalogEntry(
                List.of(
                        new Jurisdiction("CCE", "Conseil du contentieux des étrangers"),
                        new Jurisdiction("CE_BE", "Conseil d'État"),
                        new Jurisdiction("OE", "Office des étrangers")
                ),
                List.of(
                        new Stage("RECOURS_PLEIN_CONTENTIEUX", "Recours en plein contentieux", "CCE"),
                        new Stage("REFERE_EXTREME_URGENCE", "Référé en extrême urgence", "CCE"),
                        new Stage("CASSATION", "Cassation administrative", "CE_BE"),
                        new Stage("DEMANDE_TITRE", "Demande de titre", "OE")
                ),
                List.of(
                        new Position("REQUERANT", "Requérant",
                                List.of("RECOURS_PLEIN_CONTENTIEUX", "REFERE_EXTREME_URGENCE", "CASSATION")),
                        new Position("DEMANDEUR_TITRE", "Demandeur de titre", List.of("DEMANDE_TITRE"))
                )
        ));

        // ===================== DROIT_FAMILLE × FRANCE =====================
        register(DROIT_FAMILLE, FRANCE, new CatalogEntry(
                List.of(
                        new Jurisdiction("JAF", "Juge aux affaires familiales — TJ"),
                        new Jurisdiction("CA_FAM", "Cour d'appel — chambre de la famille"),
                        new Jurisdiction("CASS_CIV1", "Cour de cassation — 1ʳᵉ chambre civile"),
                        new Jurisdiction("TJ", "Tribunal judiciaire")
                ),
                List.of(
                        new Stage("DIVORCE_FOND", "Divorce — fond", "JAF"),
                        new Stage("MESURES_PROVISOIRES", "Mesures provisoires", "JAF"),
                        new Stage("REFERE", "Référé", "JAF"),
                        new Stage("ORDONNANCE_PROTECTION", "Ordonnance de protection", "JAF"),
                        new Stage("APPEL", "Appel", "CA_FAM"),
                        new Stage("POURVOI", "Pourvoi en cassation", "CASS_CIV1"),
                        new Stage("FILIATION", "Filiation", "TJ"),
                        new Stage("SUCCESSION", "Succession", "TJ")
                ),
                List.of(
                        new Position("DEMANDEUR", "Demandeur",
                                List.of("DIVORCE_FOND", "MESURES_PROVISOIRES", "REFERE", "FILIATION", "SUCCESSION")),
                        new Position("DEFENDEUR", "Défendeur",
                                List.of("DIVORCE_FOND", "MESURES_PROVISOIRES", "REFERE", "FILIATION", "SUCCESSION")),
                        new Position("REQUERANT", "Requérant", List.of("ORDONNANCE_PROTECTION")),
                        new Position("APPELANT", "Appelant", List.of("APPEL")),
                        new Position("INTIME", "Intimé", List.of("APPEL")),
                        new Position("DEMANDEUR_POURVOI", "Demandeur au pourvoi", List.of("POURVOI")),
                        new Position("DEFENDEUR_POURVOI", "Défendeur au pourvoi", List.of("POURVOI"))
                )
        ));

        // ===================== DROIT_FAMILLE × BELGIQUE =====================
        register(DROIT_FAMILLE, BELGIQUE, new CatalogEntry(
                List.of(
                        new Jurisdiction("TF", "Tribunal de la famille"),
                        new Jurisdiction("CA_FAM_BE", "Cour d'appel — chambre de la famille"),
                        new Jurisdiction("CASS_BE", "Cour de cassation")
                ),
                List.of(
                        new Stage("FOND", "Fond", "TF"),
                        new Stage("REFERE", "Référé — mesures urgentes", "TF"),
                        new Stage("APPEL", "Appel", "CA_FAM_BE"),
                        new Stage("POURVOI", "Pourvoi en cassation", "CASS_BE")
                ),
                List.of(
                        new Position("DEMANDEUR", "Demandeur", List.of("FOND", "REFERE")),
                        new Position("DEFENDEUR", "Défendeur", List.of("FOND", "REFERE")),
                        new Position("APPELANT", "Appelant", List.of("APPEL")),
                        new Position("INTIME", "Intimé", List.of("APPEL")),
                        new Position("DEMANDEUR_POURVOI", "Demandeur au pourvoi", List.of("POURVOI")),
                        new Position("DEFENDEUR_POURVOI", "Défendeur au pourvoi", List.of("POURVOI"))
                )
        ));
    }

    /** @return {@code true} si {@code domain} fait partie des domaines reconnus. */
    public static boolean isKnownDomain(String domain) {
        return domain != null && DOMAINS.contains(domain);
    }

    /** @return {@code true} si {@code country} fait partie des pays reconnus. */
    public static boolean isKnownCountry(String country) {
        return country != null && COUNTRIES.contains(country);
    }

    /**
     * Retourne le sous-référentiel applicable pour un couple (domaine, pays).
     *
     * @throws IllegalArgumentException si le couple n'est pas reconnu.
     */
    public static CatalogEntry forDomainAndCountry(String domain, String country) {
        CatalogEntry entry = CATALOG.get(key(domain, country));
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Aucun référentiel de stade procédural pour domaine=" + domain + ", pays=" + country);
        }
        return entry;
    }

    private static Optional<Jurisdiction> findJurisdiction(CatalogEntry entry, String code) {
        return entry.jurisdictions().stream().filter(j -> j.code().equals(code)).findFirst();
    }

    private static Optional<Stage> findStage(CatalogEntry entry, String code) {
        return entry.stages().stream().filter(s -> s.code().equals(code)).findFirst();
    }

    private static Optional<Position> findPosition(CatalogEntry entry, String code) {
        return entry.positions().stream().filter(p -> p.code().equals(code)).findFirst();
    }

    /** @return le libellé de la juridiction, ou {@code null} si {@code code} est null ou inconnu. */
    public static String jurisdictionLabel(String domain, String country, String code) {
        if (code == null) return null;
        return findJurisdiction(forDomainAndCountry(domain, country), code)
                .map(Jurisdiction::label).orElse(null);
    }

    /** @return le libellé du stade, ou {@code null} si {@code code} est null ou inconnu. */
    public static String stageLabel(String domain, String country, String code) {
        if (code == null) return null;
        return findStage(forDomainAndCountry(domain, country), code)
                .map(Stage::label).orElse(null);
    }

    /** @return le libellé de la position, ou {@code null} si {@code code} est null ou inconnu. */
    public static String positionLabel(String domain, String country, String code) {
        if (code == null) return null;
        return findPosition(forDomainAndCountry(domain, country), code)
                .map(Position::label).orElse(null);
    }

    /**
     * Valide la cohérence sémantique d'une combinaison (juridiction, stade, position) pour un
     * couple (domaine, pays). Les 3 champs sont nullable. La validation suppose que la cascade
     * a déjà été appliquée (effacer un parent efface les enfants).
     *
     * @return un {@link Optional} contenant le message d'erreur si la combinaison est incohérente,
     *         {@link Optional#empty()} si elle est valide.
     */
    public static Optional<String> validate(String domain, String country,
                                            String jurisdiction, String stage, String position) {
        CatalogEntry entry = forDomainAndCountry(domain, country);

        Jurisdiction j = null;
        if (jurisdiction != null) {
            j = findJurisdiction(entry, jurisdiction).orElse(null);
            if (j == null) {
                return Optional.of("Juridiction inconnue pour ce domaine : " + jurisdiction);
            }
        }

        Stage s = null;
        if (stage != null) {
            if (jurisdiction == null) {
                return Optional.of("Un stade ne peut être renseigné sans juridiction.");
            }
            s = findStage(entry, stage).orElse(null);
            if (s == null) {
                return Optional.of("Stade inconnu pour ce domaine : " + stage);
            }
            if (!s.jurisdictionCode().equals(jurisdiction)) {
                return Optional.of("Le stade '" + stage + "' n'appartient pas à la juridiction '"
                        + jurisdiction + "'.");
            }
        }

        if (position != null) {
            if (stage == null) {
                return Optional.of("Une position ne peut être renseignée sans stade.");
            }
            Position p = findPosition(entry, position).orElse(null);
            if (p == null) {
                return Optional.of("Position inconnue pour ce domaine : " + position);
            }
            if (!p.stageCodes().contains(stage)) {
                return Optional.of("La position '" + position + "' n'est pas valide pour le stade '"
                        + stage + "'.");
            }
        }

        return Optional.empty();
    }

    /**
     * Vérifie l'intégrité interne du catalogue : chaque stade référence une juridiction existante,
     * chaque position référence des stades existants. Utilisé par les tests unitaires.
     *
     * @return la liste des incohérences détectées (vide si le catalogue est cohérent).
     */
    public static List<String> selfCheck() {
        List<String> errors = new java.util.ArrayList<>();
        for (Map.Entry<String, CatalogEntry> e : CATALOG.entrySet()) {
            String ctx = e.getKey();
            CatalogEntry entry = e.getValue();
            Set<String> jurisdictionCodes = new LinkedHashSet<>();
            for (Jurisdiction j : entry.jurisdictions()) {
                jurisdictionCodes.add(j.code());
            }
            Set<String> stageCodes = new LinkedHashSet<>();
            for (Stage s : entry.stages()) {
                stageCodes.add(s.code());
                if (!jurisdictionCodes.contains(s.jurisdictionCode())) {
                    errors.add(ctx + " : stade '" + s.code() + "' référence une juridiction inconnue '"
                            + s.jurisdictionCode() + "'");
                }
            }
            for (Position p : entry.positions()) {
                if (p.stageCodes().isEmpty()) {
                    errors.add(ctx + " : position '" + p.code() + "' ne référence aucun stade");
                }
                for (String sc : p.stageCodes()) {
                    if (!stageCodes.contains(sc)) {
                        errors.add(ctx + " : position '" + p.code() + "' référence un stade inconnu '"
                                + sc + "'");
                    }
                }
            }
            if (entry.jurisdictions().isEmpty() || entry.stages().isEmpty() || entry.positions().isEmpty()) {
                errors.add(ctx + " : sous-référentiel incomplet (juridictions/stades/positions vides)");
            }
        }
        return errors;
    }

    /** @return les clés {@code domain|country} de toutes les combinaisons encodées (6 attendues). */
    public static Set<String> allCombinationKeys() {
        return new LinkedHashSet<>(CATALOG.keySet());
    }
}

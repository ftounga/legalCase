package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Référentiel statique des modes de garde France + Belgique.
 */
public final class GardeModeReferentiel {

    private GardeModeReferentiel() {}

    private static final List<GardeMode> ALL = List.of(

            // ========== FRANCE ==========

            new GardeMode("ALTERNEE_FR", "Résidence alternée", "FRANCE",
                    "L'enfant réside alternativement chez chaque parent, en général une semaine sur deux.",
                    "ALTERNEE_1_SUR_2",
                    List.of("Semaine A : lundi → dimanche"),
                    List.of("Semaine B : lundi → dimanche"),
                    "Vacances scolaires partagées par moitié, alternance années paires/impaires pour Noël et été.",
                    "Article 373-2-9 du Code civil"),

            new GardeMode("DVH_CLASSIQUE_FR", "Droit de visite et d'hébergement classique", "FRANCE",
                    "L'enfant réside principalement chez un parent. L'autre a un DVH les 1er, 3e et 5e week-ends + mercredi.",
                    "DVH_CLASSIQUE",
                    List.of("Lundi → vendredi (hors mercredi si DVH mercredi)", "2e et 4e week-ends"),
                    List.of("1er, 3e et 5e week-ends (vendredi sortie école → dimanche 18h)", "Mercredi (sortie école → jeudi matin)"),
                    "Moitié des vacances scolaires en alternance.",
                    "Article 373-2-9 du Code civil ; usage des JAF"),

            new GardeMode("DVH_ELARGI_FR", "DVH élargi", "FRANCE",
                    "DVH étendu : week-ends élargis (vendredi → lundi matin) + mercredi + une partie des petites vacances.",
                    "DVH_ELARGI",
                    List.of("Mardi → vendredi matin", "2e et 4e week-ends"),
                    List.of("1er, 3e et 5e week-ends (vendredi sortie → lundi matin)", "Mercredi sortie → jeudi matin", "Première semaine des petites vacances"),
                    "Vacances partagées, avec priorité au parent non-gardien sur la première semaine des petites vacances.",
                    "Article 373-2-9 du Code civil"),

            // ========== BELGIQUE ==========

            new GardeMode("ALTERNEE_BE", "Hébergement égalitaire", "BELGIQUE",
                    "L'enfant réside alternativement chez chaque parent, une semaine sur deux. Présomption légale depuis 2006.",
                    "ALTERNEE_1_SUR_2",
                    List.of("Semaine A : lundi → dimanche"),
                    List.of("Semaine B : lundi → dimanche"),
                    "Vacances scolaires partagées par moitié, alternance années paires/impaires.",
                    "Article 374 §2 du Code civil belge (loi du 18 juillet 2006)"),

            new GardeMode("SECONDAIRE_BE", "Hébergement secondaire classique", "BELGIQUE",
                    "L'enfant réside principalement chez un parent. L'autre a un hébergement secondaire (1 WE sur 2 + mercredi).",
                    "DVH_CLASSIQUE",
                    List.of("Lundi → vendredi (hors mercredi)", "2e et 4e week-ends"),
                    List.of("1er et 3e week-ends (vendredi 18h → dimanche 18h)", "Mercredi après-midi"),
                    "Moitié des vacances scolaires en alternance.",
                    "Article 374 du Code civil belge"),

            new GardeMode("SECONDAIRE_ELARGI_BE", "Hébergement secondaire élargi", "BELGIQUE",
                    "Hébergement secondaire étendu : WE élargis + mercredi nuit + partie des vacances.",
                    "DVH_ELARGI",
                    List.of("Mardi → vendredi matin", "2e et 4e week-ends"),
                    List.of("1er et 3e week-ends (vendredi 18h → lundi matin)", "Mercredi sortie → jeudi matin", "Première semaine des congés scolaires"),
                    "Vacances partagées avec priorité au parent hébergeant secondaire sur la première semaine.",
                    "Article 374 du Code civil belge ; usage des tribunaux de la famille")
    );

    public static GardeMode getByCode(String code) {
        return ALL.stream().filter(g -> g.code().equals(code)).findFirst().orElse(null);
    }

    public static List<GardeMode> getByCountry(String country) {
        return ALL.stream().filter(g -> g.country().equals(country)).toList();
    }

    public static List<GardeMode> getAll() { return ALL; }

    public static boolean isCodeValid(String code) {
        return ALL.stream().anyMatch(g -> g.code().equals(code));
    }
}

package fr.ailegalcase.analysis;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-179 SF-179-01 — pré-filtrage par expressions régulières des références
 * jurisprudentielles citées dans le texte d'un document (FR + BE).
 *
 * <p>Ce pré-filtrage n'est qu'un <strong>indice</strong> : il repère les
 * formats standard les plus fréquents. Le texte est par ailleurs soumis à
 * Claude Sonnet dans le prompt de vérification, ce qui rattrape les références
 * noyées dans le texte que la regex rate (formats variables, abréviations
 * inhabituelles). Les regex seuls ratent les formats variables ; Claude seul
 * rate les références noyées dans 30 pages — d'où l'approche hybride.</p>
 *
 * <p>Couverture FR : Cour de cassation ({@code Cass. soc.}, {@code Cass. civ.},
 * {@code Cass. com.}…), Conseil d'État ({@code CE}), cours d'appel
 * ({@code CA <ville>}), Conseil constitutionnel ({@code Cons. const.}).
 * Couverture BE : tribunal du travail ({@code Trib. trav.}), Cour
 * constitutionnelle ({@code Cour const.} / {@code C. const.}), Cour de
 * cassation belge ({@code Cass.} avec n° de rôle), Conseil d'État belge.</p>
 */
public final class JurisprudenceReferenceExtractor {

    private JurisprudenceReferenceExtractor() {
    }

    /** Limite de candidats remontés par document (garde-fou anti-bruit). */
    static final int MAX_CANDIDATES_PER_DOCUMENT = 40;

    /**
     * Cour de cassation FR : "Cass. soc. 25 septembre 2013, n° 12-17.516",
     * "Cass. civ. 1re, 3 mars 2021, n°19-25.000", "Cassation sociale ...".
     */
    private static final Pattern CASS_FR = Pattern.compile(
            "Cass(?:ation)?\\.?\\s*(?:soc|civ|com|crim|mixte|ass\\.?\\s*pl[ée]n)\\.?[^\\n]{0,80}?"
                    + "n[°ºo]\\s*\\d{1,2}[-./]\\d{1,2}[-./.]\\d{2,4}",
            Pattern.CASE_INSENSITIVE);

    /**
     * Conseil d'État FR/BE : "CE 30 juin 2017, n° 398445", "Conseil d'État n°123456".
     */
    private static final Pattern CONSEIL_ETAT = Pattern.compile(
            "(?:CE|Conseil\\s+d['’]?[ÉE]tat)\\b[^\\n]{0,80}?n[°ºo]\\s*\\d{3,7}",
            Pattern.CASE_INSENSITIVE);

    /**
     * Cour d'appel FR : "CA Paris, 12 mars 2020, n° 18/12345",
     * "Cour d'appel de Lyon 5 mai 2019, RG 17/00001".
     */
    private static final Pattern COUR_APPEL = Pattern.compile(
            "(?:CA|Cour\\s+d['’]?appel)\\s+(?:de\\s+|d['’])?[A-ZÀ-Ý][\\wÀ-ÿ-]+"
                    + "[^\\n]{0,60}?(?:n[°ºo]|RG)\\s*\\d{1,2}[/.]\\d{3,6}",
            Pattern.CASE_INSENSITIVE);

    /**
     * Conseil constitutionnel FR : "Cons. const. n° 2013-672 DC",
     * "Conseil constitutionnel, décision n°2020-800 QPC".
     */
    private static final Pattern CONSEIL_CONSTIT_FR = Pattern.compile(
            "(?:Cons\\.?\\s*const\\.?|Conseil\\s+constitutionnel)[^\\n]{0,60}?"
                    + "n[°ºo]\\s*\\d{4}-\\d{1,4}\\s*(?:DC|QPC|L|FNR)?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Cour constitutionnelle belge : "Cour const. n° 45/2021",
     * "C. const., arrêt n°12/2019".
     */
    private static final Pattern COUR_CONST_BE = Pattern.compile(
            "(?:Cour\\s+const(?:itutionnelle)?\\.?|C\\.?\\s*const\\.?|"
                    + "Grondwettelijk\\s+Hof)[^\\n]{0,60}?n[°ºo]\\s*\\d{1,3}/\\d{4}",
            Pattern.CASE_INSENSITIVE);

    /**
     * Tribunal du travail belge : "Trib. trav. Bruxelles, 5 mai 2020",
     * "Tribunal du travail de Liège 12/03/2019".
     */
    private static final Pattern TRIB_TRAV_BE = Pattern.compile(
            "(?:Trib\\.?\\s*trav\\.?|Tribunal\\s+du\\s+travail)\\s+(?:de\\s+|d['’])?"
                    + "[A-ZÀ-Ý][\\wÀ-ÿ-]+[^\\n]{0,40}?"
                    + "\\d{1,2}[\\s/.-](?:\\d{1,2}|janvier|février|mars|avril|mai|juin|juillet|"
                    + "ao[uû]t|septembre|octobre|novembre|décembre)[\\s/.-]\\d{2,4}",
            Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> PATTERNS = List.of(
            CASS_FR, CONSEIL_ETAT, COUR_APPEL, CONSEIL_CONSTIT_FR, COUR_CONST_BE, TRIB_TRAV_BE);

    /**
     * Extrait les candidats de référence jurisprudentielle d'un texte de
     * document. Renvoie une liste dédupliquée (insensible à la casse et aux
     * espaces multiples), bornée par {@link #MAX_CANDIDATES_PER_DOCUMENT}.
     *
     * @param text texte brut du document (peut être {@code null})
     * @return liste de libellés candidats — vide si aucun ou si {@code text} null
     */
    public static List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // LinkedHashSet : dédup en conservant l'ordre d'apparition.
        Set<String> seen = new LinkedHashSet<>();
        Set<String> normalizedSeen = new LinkedHashSet<>();
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find() && seen.size() < MAX_CANDIDATES_PER_DOCUMENT) {
                String raw = matcher.group().replaceAll("\\s+", " ").trim();
                if (raw.isEmpty()) {
                    continue;
                }
                String norm = raw.toLowerCase(java.util.Locale.ROOT);
                if (normalizedSeen.add(norm)) {
                    seen.add(raw);
                }
            }
        }
        return List.copyOf(seen);
    }
}

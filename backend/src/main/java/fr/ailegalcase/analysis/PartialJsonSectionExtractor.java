package fr.ailegalcase.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F-185 SF-185-01 — extracteur incrémental de sections JSON top-level dans un
 * flux de tokens streamé par Anthropic.
 *
 * <p>Hypothèse : Sonnet produit du JSON conforme au schéma de
 * {@link CaseAnalysisService#SYSTEM_PROMPT_TEMPLATE}, soit un objet racine
 * {@code {"timeline": [...], "faits": [...], "points_juridiques": [...], ...}}.
 *
 * <p>Algorithme : machine à états qui lit caractère par caractère le buffer
 * accumulé, suit la profondeur des accolades / crochets, gère l'échappement et
 * les chaînes de caractères, et détecte qu'une section top-level est complète
 * quand sa valeur (array ou objet) revient à profondeur 0 (relative à la racine).
 *
 * <p>Usage : appeler {@link #append(String)} à chaque delta reçu. La méthode
 * retourne la liste des nouvelles sections complétées depuis le dernier appel
 * (vide si aucune nouvelle section close). Les sections sont retournées comme
 * {@code Map<String, String>} où la clé est le nom de la section et la valeur
 * est le JSON brut de la valeur (ex. {@code "[{...}, {...}]"} pour un array,
 * {@code "{...}"} pour un objet, {@code "42"} pour un scalaire).
 *
 * <p>Robustesse : si le JSON est malformé en cours de stream (ex. troncature,
 * caractère invalide), l'extracteur ne lèvera pas d'exception — il retournera
 * simplement une liste vide jusqu'à ce qu'il puisse à nouveau parser. Aucune
 * section n'est jamais retournée incomplète.
 *
 * <p>Cet extracteur n'est pas thread-safe : un seul producteur doit appeler
 * {@link #append(String)} séquentiellement par stream Anthropic. C'est le cas
 * naturel puisqu'un appel Anthropic = un thread de consommation RabbitMQ.
 */
public final class PartialJsonSectionExtractor {

    private final StringBuilder buffer = new StringBuilder();
    private int cursor = 0;
    private final Map<String, String> emittedSections = new LinkedHashMap<>();

    /**
     * Append des tokens streamés au buffer interne et retourne la liste des
     * sections nouvellement complétées (chaque entrée = nom + JSON brut de la
     * valeur). Liste vide si rien de nouveau.
     */
    public List<Map.Entry<String, String>> append(String chunk) {
        if (chunk == null || chunk.isEmpty()) return List.of();
        buffer.append(chunk);
        return scanNewSections();
    }

    /**
     * Snapshot des sections déjà émises (immuable, ordre d'arrivée préservé).
     * Utilisé pour persister l'état partiel courant en DB.
     */
    public Map<String, String> snapshot() {
        // LinkedHashMap pour préserver l'ordre d'insertion (l'ordre d'arrivée
        // dans le stream Sonnet) — Map.copyOf() ne garantit pas l'ordre.
        return Collections.unmodifiableMap(new LinkedHashMap<>(emittedSections));
    }

    private List<Map.Entry<String, String>> scanNewSections() {
        List<Map.Entry<String, String>> newlyClosed = new ArrayList<>();

        // Phase 1 : trouver le `{` de l'objet racine si pas encore fait.
        // SF-185-07 — Sonnet 4.6 enveloppe parfois sa sortie JSON dans un bloc
        // Markdown (```json\n{...}\n```). On scanne jusqu'au 1er `{` au lieu
        // d'exiger qu'il soit le 1er char non-whitespace, sinon l'extracteur
        // abandonne sur le 1er backtick et reste stuck pour toujours.
        if (cursor == 0) {
            int rootStart = buffer.indexOf("{");
            if (rootStart < 0) return newlyClosed; // pas encore de `{` dans le buffer, on attend
            cursor = rootStart + 1; // on est maintenant à l'intérieur du `{`
        }

        // Phase 2 : itérer sur les paires "key": value à la racine
        while (cursor < buffer.length()) {
            int keyStart = indexOfNonWhitespaceOrComma(buffer, cursor);
            if (keyStart < 0) return newlyClosed;
            char c = buffer.charAt(keyStart);
            if (c == '}') {
                // Fin de l'objet racine, plus de section à attendre
                cursor = keyStart + 1;
                return newlyClosed;
            }
            if (c != '"') {
                // Caractère inattendu (probablement on est en train de stream une chaîne incomplète)
                return newlyClosed;
            }
            // On est sur le `"` qui ouvre la clé
            int keyEnd = findStringEnd(buffer, keyStart);
            if (keyEnd < 0) return newlyClosed; // clé pas encore complète
            String sectionName = buffer.substring(keyStart + 1, keyEnd);

            int colonPos = indexOfNonWhitespace(buffer, keyEnd + 1);
            if (colonPos < 0 || buffer.charAt(colonPos) != ':') return newlyClosed;
            int valueStart = indexOfNonWhitespace(buffer, colonPos + 1);
            if (valueStart < 0) return newlyClosed;

            int valueEnd = findValueEnd(buffer, valueStart);
            if (valueEnd < 0) return newlyClosed; // valeur pas encore complète

            String valueJson = buffer.substring(valueStart, valueEnd + 1);
            // Avancer le curseur après la valeur (gère la virgule / accolade fermante au tour suivant)
            cursor = valueEnd + 1;

            if (!emittedSections.containsKey(sectionName)) {
                emittedSections.put(sectionName, valueJson);
                newlyClosed.add(Map.entry(sectionName, valueJson));
            }
        }

        return newlyClosed;
    }

    /**
     * Cherche la fin d'une chaîne JSON démarrant à {@code start} (qui doit pointer
     * sur le {@code "} ouvrant). Gère les échappements {@code \"}, {@code \\}, etc.
     * Retourne l'index du {@code "} fermant, ou -1 si la chaîne n'est pas terminée.
     */
    private static int findStringEnd(StringBuilder buf, int start) {
        int i = start + 1;
        while (i < buf.length()) {
            char c = buf.charAt(i);
            if (c == '\\') {
                i += 2; // skip the escaped char
                continue;
            }
            if (c == '"') return i;
            i++;
        }
        return -1;
    }

    /**
     * Cherche la fin d'une valeur JSON démarrant à {@code start} (qui doit pointer
     * sur le 1er caractère non-whitespace de la valeur : {@code {}, {@code [},
     * {@code "}, ou un scalaire). Suit la profondeur des structures imbriquées,
     * gère les chaînes (caractères spéciaux à ignorer).
     *
     * @return index du dernier caractère de la valeur (inclusif), ou -1 si la
     *         valeur n'est pas encore terminée dans le buffer.
     */
    private static int findValueEnd(StringBuilder buf, int start) {
        char first = buf.charAt(start);
        if (first == '"') {
            return findStringEnd(buf, start);
        }
        if (first == '{' || first == '[') {
            return findStructureEnd(buf, start);
        }
        // Scalaire : nombre, true, false, null. On scanne jusqu'à la prochaine
        // virgule, accolade fermante ou crochet fermant à la racine.
        int i = start;
        while (i < buf.length()) {
            char c = buf.charAt(i);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                if (i == start) return -1;
                return i - 1;
            }
            i++;
        }
        return -1; // pas encore de séparateur visible
    }

    /**
     * Cherche la fin d'une structure {@code {...}} ou {@code [...]} à profondeur
     * arbitraire, en gérant correctement les chaînes JSON internes (les accolades
     * dans les chaînes ne comptent pas).
     */
    private static int findStructureEnd(StringBuilder buf, int start) {
        char open = buf.charAt(start);
        char close = (open == '{') ? '}' : ']';
        int depth = 0;
        int i = start;
        while (i < buf.length()) {
            char c = buf.charAt(i);
            if (c == '"') {
                int strEnd = findStringEnd(buf, i);
                if (strEnd < 0) return -1;
                i = strEnd + 1;
                continue;
            }
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
            i++;
        }
        return -1;
    }

    private static int indexOfNonWhitespace(StringBuilder buf, int from) {
        for (int i = from; i < buf.length(); i++) {
            if (!Character.isWhitespace(buf.charAt(i))) return i;
        }
        return -1;
    }

    private static int indexOfNonWhitespaceOrComma(StringBuilder buf, int from) {
        for (int i = from; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (!Character.isWhitespace(c) && c != ',') return i;
        }
        return -1;
    }
}

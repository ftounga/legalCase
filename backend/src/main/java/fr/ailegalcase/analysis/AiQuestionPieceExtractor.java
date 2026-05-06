package fr.ailegalcase.analysis;

import java.util.List;
import java.util.regex.Pattern;

/**
 * F-196 SF-196-01 — Extracteur statique de libellé de pièce depuis le texte
 * d'une question complémentaire F-94 ({@code ai_questions.question_text}).
 *
 * <p>V1 : matching keyword/regex. ~10-15 patterns courants couvrant Travail
 * (FR + BE), Immigration (FR + BE), Famille (FR + BE). V2 si signal terrain :
 * fuzzy / sémantique. Hors scope explicite de la mini-spec V1.</p>
 *
 * <p>Convention : si un pattern matche, on retourne le libellé canonique
 * (forme normalisée mise en avant — première majuscule, reste minuscule,
 * sans accent perdu) à insérer tel quel dans
 * {@code analysis_result.pieces_manquantes}. Sinon {@code null} (fail-open
 * silencieux : la question reste {@code INFO_ONLY}).</p>
 *
 * <p>Pattern miroir {@link RisqueToolMatcher} (F-195 — keyword statique de la
 * même famille). Toutes les méthodes sont statiques et pures (aucun
 * side-effect, aucune dépendance Spring).</p>
 */
public final class AiQuestionPieceExtractor {

    private AiQuestionPieceExtractor() {}

    /** Mapping ordonné — premier pattern qui matche gagne. */
    private static final List<Mapping> MAPPINGS = List.of(
            // ── Droit du travail FR ────────────────────────────────────────
            new Mapping(compile("lettre.*licenc"), "Lettre de licenciement"),
            new Mapping(compile("contrat.*travail"), "Contrat de travail"),
            new Mapping(compile("fiches?.*paie|bulletins?.*salair|bulletins?.*paie"), "Fiches de paie"),
            new Mapping(compile("attestation.*p[oô]le.*emploi|attestation.*pole.*emploi"),
                    "Attestation Pôle emploi"),
            new Mapping(compile("solde.*tout.*compte"), "Solde de tout compte"),
            new Mapping(compile("certificat.*travail"), "Certificat de travail"),
            new Mapping(compile("convention.*collective"), "Convention collective"),
            new Mapping(compile("entretien.*pr[eé]alable|convocation.*entretien"),
                    "Convocation à l'entretien préalable"),
            new Mapping(compile("rupture.*conventionnelle|convention.*rupture"),
                    "Convention de rupture conventionnelle"),
            new Mapping(compile("homologation.*direccte|homologation.*dreets"),
                    "Homologation DREETS"),
            // ── Famille FR + BE ────────────────────────────────────────────
            new Mapping(compile("acte.*mariage"), "Acte de mariage"),
            new Mapping(compile("livret.*famille"), "Livret de famille"),
            new Mapping(compile("acte.*naissance"), "Acte de naissance"),
            new Mapping(compile("jugement.*divorce"), "Jugement de divorce"),
            new Mapping(compile("contrat.*mariage"), "Contrat de mariage"),
            // ── Immigration FR + BE ────────────────────────────────────────
            new Mapping(compile("titre.*s[eé]jour|carte.*s[eé]jour"), "Titre de séjour"),
            new Mapping(compile("r[eé]c[eé]piss[eé]"), "Récépissé de demande"),
            new Mapping(compile("visa\\b"), "Visa"),
            new Mapping(compile("passeport"), "Passeport"),
            new Mapping(compile("oqtf|obligation.*quitter"), "OQTF (Obligation de Quitter le Territoire Français)"),
            new Mapping(compile("annexe.*13"), "Annexe 13"),
            new Mapping(compile("attestation.*h[eé]bergement"), "Attestation d'hébergement")
    );

    /**
     * Tente d'extraire un libellé de pièce du texte d'une question. Renvoie
     * {@code null} si aucun pattern ne matche.
     *
     * <p>Le matching est case-insensitive ; on n'exige PAS la présence de
     * "avez-vous" pour rester souple (variantes "Disposez-vous de…",
     * "Possédez-vous…", "Pouvez-vous nous transmettre…", etc.).</p>
     */
    public static String extractPieceLibelle(String questionText) {
        if (questionText == null || questionText.isBlank()) return null;
        String normalized = questionText.toLowerCase();
        for (Mapping m : MAPPINGS) {
            if (m.pattern.matcher(normalized).find()) {
                return m.pieceLibelle;
            }
        }
        return null;
    }

    /**
     * Heuristique réponse → décision : {@code true} si la réponse exprime
     * "oui" (la pièce est en possession), {@code false} si "non", {@code null}
     * sinon. Trim + case-insensitive ; tolère "Oui", "OUI ", "oui je l'ai",
     * "non, perdue", etc.
     */
    public static Boolean parseYesNo(String answerText) {
        if (answerText == null) return null;
        String t = answerText.trim().toLowerCase();
        if (t.isEmpty()) return null;
        // matching mot — éviter de capturer "non" dans "nonobstant" (rare)
        if (t.startsWith("oui") || t.contains(" oui ") || t.equals("oui")
                || t.startsWith("yes") || t.equals("y")) {
            return Boolean.TRUE;
        }
        if (t.startsWith("non") || t.contains(" non ") || t.equals("non")
                || t.startsWith("no")  || t.equals("n")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Pattern compile(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private record Mapping(Pattern pattern, String pieceLibelle) {}
}

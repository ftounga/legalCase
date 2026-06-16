package fr.ailegalcase.casefile.overview;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * F-289 / SF-289-01 — Vue d'ensemble agrégée d'un dossier (poste de pilotage / journal).
 *
 * <p>DTO d'agrégation <b>en lecture seule</b>, assemblé en {@code fail-open} par source
 * (cf. {@link OverviewService}). Aucune table propre, aucun appel LLM. Le contrat de
 * champs JSON est <b>figé</b> : le frontend s'aligne dessus.</p>
 *
 * @param pilotage      bandeau d'état (phase / tour / couperet / santé)
 * @param attention     to-do priorisée, triée par urgence, <b>tronquée à 5</b>
 * @param attentionTotal total réel d'items d'attention (avant troncature)
 * @param fil           le fil chronologique, trié par date croissante
 */
public record OverviewResponse(
        Pilotage pilotage,
        List<AttentionItem> attention,
        int attentionTotal,
        List<TimelineEvent> fil
) {

    /**
     * Bandeau d'état du dossier.
     *
     * @param currentPhaseLabel  libellé de la phase courante (null si aucune)
     * @param awaitingParty      à qui le tour : {@code OURS} / {@code ADVERSE} (null si N/A)
     * @param nextDeadline       prochain couperet daté (null si aucun)
     * @param sante              recevabilité + prescription + risque avocat
     * @param analysisStale      vrai si des pièces sont en attente d'analyse
     * @param pendingPiecesCount nombre de pièces ajoutées depuis la dernière analyse
     * @param toolsCalculated    nombre d'outils décisionnels calculés (null si indisponible)
     * @param toolsVisible       nombre d'outils visibles (null en V1, non trivial à calculer)
     */
    public record Pilotage(
            String currentPhaseLabel,
            String awaitingParty,
            NextDeadline nextDeadline,
            Sante sante,
            boolean analysisStale,
            int pendingPiecesCount,
            Integer toolsCalculated,
            Integer toolsVisible
    ) {}

    /**
     * Prochain couperet daté.
     *
     * @param urgency OVERDUE | CRITICAL | SOON | UPCOMING
     */
    public record NextDeadline(
            String label,
            LocalDate dueDate,
            long daysUntil,
            String urgency
    ) {}

    /**
     * Santé du dossier (valeurs textuelles, jamais inventées).
     *
     * @param admissibility    recevabilité (nom de l'enum d'intake) ou null
     * @param prescriptionStatus statut prescription (nom de l'enum d'intake) ou null
     * @param riskLevelAvocat  niveau de risque avocat (FAIBLE|MOYEN|ELEVE) ou null
     */
    public record Sante(
            String admissibility,
            String prescriptionStatus,
            String riskLevelAvocat
    ) {}

    /**
     * Un item « ce qui requiert ton attention ».
     *
     * @param type    ECHEANCE | PIECE_MANQUANTE | QUESTION_IA | ANALYSE_OBSOLETE
     * @param urgency OVERDUE | CRITICAL | SOON | UPCOMING | INFO
     * @param questionId  SF-289-02 — id de la question IA à répondre <b>inline</b>
     *                    (non null uniquement pour un item {@code QUESTION_IA}
     *                    portant <b>une seule</b> question sans réponse ; null si
     *                    plusieurs questions sont agrégées → l'avocat est routé).
     * @param pieceLibelle SF-289-02 — libellé original exact de la pièce manquante
     *                    (clé d'upsert du PUT {@code /pieces-manquantes/status}),
     *                    non null pour un item {@code PIECE_MANQUANTE}.
     */
    public record AttentionItem(
            String type,
            String label,
            String urgency,
            Action action,
            UUID questionId,
            String pieceLibelle,
            // SF-289-07 — texte de la 1ʳᵉ question sans réponse (non null pour
            // QUESTION_IA), affiché dans le champ de réponse inline pour que
            // l'avocat sache à quoi il répond, même s'il en reste plusieurs.
            String questionText
    ) {}

    /**
     * Un événement du fil chronologique.
     *
     * @param voie    PROCEDURE | ECHANGE | PIECES | PRODUCTION | ECHEANCE
     * @param acteur  OURS | ADVERSE | SYSTEME | null
     * @param temps   PASSE | AUJOURDHUI | FUTUR
     */
    public record TimelineEvent(
            LocalDate date,
            String voie,
            String acteur,
            String titre,
            String detail,
            String temps,
            String urgency,
            List<Attachment> attachments,
            Action action
    ) {}

    /** Pièce attachée à un événement du fil (dépliable). */
    public record Attachment(
            UUID documentId,
            String filename
    ) {}

    /**
     * Action contextuelle portée par un item / événement.
     *
     * @param kind      NAVIGATE | GENERATE_REPLY | RELAUNCH_ANALYSIS | VALIDATE_DEADLINE
     *                  | OPEN_DOC | ANSWER_QUESTION | MARK_PIECE
     * @param targetTab dossier | analyse | decision | suivi (null si N/A)
     * @param anchor    ancre de scroll dans l'onglet cible (null si N/A)
     * @param route     route applicative (null si simple changement d'onglet)
     */
    public record Action(
            String kind,
            String targetTab,
            String anchor,
            String route
    ) {}
}

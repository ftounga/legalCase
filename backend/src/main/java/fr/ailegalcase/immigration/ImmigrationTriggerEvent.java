package fr.ailegalcase.immigration;

/**
 * F-150 SF-150-01 : événement factuel détecté dans le dossier qui ouvre un
 * nouveau droit de séjour.
 *
 * <p>Enrichi côté backend lors du parsing de la réponse IA à partir du code
 * d'événement via {@link ImmigrationTriggerEventReferential}. Exposé dans
 * {@code CaseAnalysisResponse.immigrationTriggerEvents} au frontend.
 *
 * @param eventCode            code stable (enum {@link ImmigrationTriggerEventCode})
 * @param eventLabel           libellé humain de l'événement
 * @param eventDate            date de l'événement (YYYY-MM-DD), peut être null
 * @param sourceDocument       nom du document support (peut être null)
 * @param justification        phrase courte de l'IA expliquant l'indice factuel
 * @param baseLegale           base légale CESEDA applicable
 * @param suggestedTitleCode   code titre de séjour cible (enum F-IM-05)
 * @param suggestedTitleLabel  libellé humain du titre cible
 */
public record ImmigrationTriggerEvent(
        String eventCode,
        String eventLabel,
        String eventDate,
        String sourceDocument,
        String justification,
        String baseLegale,
        String suggestedTitleCode,
        String suggestedTitleLabel
) {}

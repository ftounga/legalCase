package fr.ailegalcase.immigration;

import java.util.Map;
import java.util.Optional;

/**
 * F-150 SF-150-01 : référentiel statique des événements déclencheurs immigration FR.
 *
 * <p>Pour chaque code d'événement, associe :
 * <ul>
 *   <li>le libellé humain (français)</li>
 *   <li>la base légale CESEDA applicable</li>
 *   <li>le code de titre de séjour cible (enum F-IM-05)</li>
 *   <li>le libellé humain du titre cible</li>
 * </ul>
 *
 * <p>V1 : France uniquement. Voir mini-spec SF-150-01 "Hors scope".
 */
public final class ImmigrationTriggerEventReferential {

    public record EventDefinition(
            String eventLabel,
            String baseLegale,
            String suggestedTitleCode,
            String suggestedTitleLabel
    ) {}

    private static final Map<ImmigrationTriggerEventCode, EventDefinition> DEFINITIONS = Map.ofEntries(
            Map.entry(ImmigrationTriggerEventCode.MARIAGE_RESSORTISSANT_FR, new EventDefinition(
                    "Mariage célébré ou publié avec un ressortissant français",
                    "Art. L.423-1 CESEDA — conjoint de Français",
                    "CST_VPF",
                    "Carte de séjour temporaire « Vie privée et familiale »"
            )),
            Map.entry(ImmigrationTriggerEventCode.PACS_RESSORTISSANT_FR, new EventDefinition(
                    "PACS avec un ressortissant français (communauté de vie > 1 an)",
                    "Art. L.423-1 CESEDA — partenaire de Français",
                    "CST_VPF",
                    "Carte de séjour temporaire « Vie privée et familiale »"
            )),
            Map.entry(ImmigrationTriggerEventCode.NAISSANCE_ENFANT_FR, new EventDefinition(
                    "Naissance ou reconnaissance imminente d'un enfant français",
                    "Art. L.423-7 CESEDA — parent d'enfant français",
                    "CST_VPF",
                    "Carte de séjour temporaire « Vie privée et familiale — parent d'enfant français »"
            )),
            Map.entry(ImmigrationTriggerEventCode.DOCTORAT_OBTENU, new EventDefinition(
                    "Doctorat obtenu ou soutenance programmée en France",
                    "Art. L.421-14 CESEDA — passeport-talent chercheur",
                    "CARTE_PLURIANNUELLE",
                    "Carte pluriannuelle « Passeport talent — chercheur »"
            )),
            Map.entry(ImmigrationTriggerEventCode.CDI_OBTENU_SALARIE, new EventDefinition(
                    "CDI signé (même non commencé)",
                    "Art. L.421-1 CESEDA — salarié",
                    "CST_SALARIE",
                    "Carte de séjour temporaire « Salarié »"
            )),
            Map.entry(ImmigrationTriggerEventCode.ENTREE_LEGALE_10ANS, new EventDefinition(
                    "Présence régulière en France depuis 10 ans ou plus",
                    "Art. L.426-1 CESEDA — titulaire depuis 10 ans",
                    "CARTE_RESIDENT",
                    "Carte de résident (10 ans)"
            )),
            Map.entry(ImmigrationTriggerEventCode.VIOLENCES_CONJUGALES_CONSTATEES, new EventDefinition(
                    "Violences conjugales constatées (conjoint victime)",
                    "Art. L.423-3 CESEDA — protection du conjoint victime",
                    "CST_VPF",
                    "Carte de séjour temporaire « Vie privée et familiale — protection »"
            )),
            Map.entry(ImmigrationTriggerEventCode.DEMANDE_ASILE_ACCORDEE_OFPRA, new EventDefinition(
                    "Protection internationale accordée (OFPRA ou CNDA)",
                    "Art. L.424-1 CESEDA — réfugié statutaire",
                    "CST_VPF",
                    "Carte de séjour pluriannuelle « Bénéficiaire de la protection internationale »"
            )),
            Map.entry(ImmigrationTriggerEventCode.ENFANT_NE_FR_13ANS_PRESENCE, new EventDefinition(
                    "Enfant étranger né en France et y ayant résidé depuis l'âge de 13 ans",
                    "Art. L.423-23 CESEDA — étranger né en France",
                    "CARTE_RESIDENT",
                    "Carte de résident de plein droit"
            )),
            Map.entry(ImmigrationTriggerEventCode.REGROUPEMENT_FAMILIAL_AUTORISE, new EventDefinition(
                    "Autorisation de regroupement familial délivrée",
                    "Art. L.434-7 CESEDA — regroupement familial",
                    "CST_VPF",
                    "Carte de séjour temporaire « Vie privée et familiale — regroupement familial »"
            ))
    );

    /** Résout le code en définition enrichie, {@link Optional#empty()} si code inconnu. */
    public static Optional<EventDefinition> resolve(ImmigrationTriggerEventCode code) {
        return Optional.ofNullable(DEFINITIONS.get(code));
    }

    /**
     * Parse une chaîne en code si elle correspond à une valeur de l'enum,
     * sinon retourne {@link Optional#empty()}. Tolère null et casse mixte.
     */
    public static Optional<ImmigrationTriggerEventCode> parseCode(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(ImmigrationTriggerEventCode.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private ImmigrationTriggerEventReferential() {}
}

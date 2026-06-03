import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CongesEvenementsFamiliauxTypeEvenement } from '../../core/models/conges-evenements-familiaux.model';

/**
 * SF-218-44 — Helper partagé pour {@link CongesEvenementsFamiliauxSectionComponent}
 * (F-DT-76-conges-evenements-familiaux) — Travail FR mono-pays.
 *
 * Champ pré-fill (depuis {@link TravailExtractedData}, sous-record consolidé
 * `Sf218dDetail` sérialisé `@JsonUnwrapped` en clés SNAKE_CASE) :
 *  - typeEvenement : `aiData['type_evenement_familial']` (chaîne) — type
 *    d'évènement familial détecté par l'IA, normalisé vers l'enum produit
 *    (l'IA peut produire des libellés intermédiaires : MARIAGE, PACS, DECES_CONJOINT…).
 *
 * Champs NON pré-remplis :
 *  - conventionPlusFavorable : présence d'une CCN plus favorable — saisie avocat
 *    (donnée juridique non factualisable de façon fiable depuis les pièces).
 *  - dureeConventionnelleJours : durée conventionnelle — saisie avocat
 *    (corollaire de la CCN applicable, non factualisable).
 *  - `conge_evt_familial_detecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS un
 *    champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 1 champ pré-remplissable.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/**
 * Normalise un libellé d'évènement familial brut (IA) vers l'enum produit.
 * Retourne null si non reconnu (saisie avocat).
 */
function normalizeTypeEvenement(raw: unknown): CongesEvenementsFamiliauxTypeEvenement | null {
  if (typeof raw !== 'string') return null;
  const v = raw.trim().toUpperCase().replace(/[\s-]+/g, '_');
  if (v === '') return null;

  // Correspondances directes sur l'enum.
  const direct: ReadonlyArray<CongesEvenementsFamiliauxTypeEvenement> = [
    'MARIAGE_PACS',
    'NAISSANCE',
    'DECES_ENFANT',
    'DECES_CONJOINT_PARTENAIRE',
    'DECES_PERE_MERE',
    'ANNONCE_HANDICAP_ENFANT',
    'DEMENAGEMENT_NON_LEGAL',
  ];
  if (direct.includes(v as CongesEvenementsFamiliauxTypeEvenement)) {
    return v as CongesEvenementsFamiliauxTypeEvenement;
  }

  // Libellés intermédiaires fréquents produits par l'IA.
  if (v === 'MARIAGE' || v === 'PACS') return 'MARIAGE_PACS';
  if (v === 'ADOPTION' || v === 'NAISSANCE_ADOPTION') return 'NAISSANCE';
  if (
    v === 'DECES_CONJOINT' ||
    v === 'DECES_PARTENAIRE' ||
    v === 'DECES_CONCUBIN' ||
    v === 'DECES_PARTENAIRE_PACS'
  ) {
    return 'DECES_CONJOINT_PARTENAIRE';
  }
  if (
    v === 'DECES_PARENT' ||
    v === 'DECES_PERE' ||
    v === 'DECES_MERE' ||
    v === 'DECES_FRERE' ||
    v === 'DECES_SOEUR' ||
    v === 'DECES_BEAU_PERE' ||
    v === 'DECES_BELLE_MERE'
  ) {
    return 'DECES_PERE_MERE';
  }
  if (
    v === 'ANNONCE_HANDICAP' ||
    v === 'HANDICAP_ENFANT' ||
    v === 'ANNONCE_CANCER_ENFANT' ||
    v === 'PATHOLOGIE_CHRONIQUE_ENFANT'
  ) {
    return 'ANNONCE_HANDICAP_ENFANT';
  }
  if (v === 'DEMENAGEMENT') return 'DEMENAGEMENT_NON_LEGAL';
  return null;
}

export const CongesEvenementsFamiliauxPrefillRules = {

  computeTypeEvenement(input: PrefillCountInput): CongesEvenementsFamiliauxTypeEvenement | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData as { type_evenement_familial?: unknown } | null | undefined;
    if (!ai) return null;
    return normalizeTypeEvenement(ai.type_evenement_familial);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeTypeEvenement(input) !== null) n++;
    return n;
  },
} as const;

/**
 * SF-215-19 / SF-215-20 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Protection temporaire Ukraine (BE) »
 * (F-IM-34-protection-temporaire-ukraine-be).
 *
 * BELGIQUE uniquement — régime de protection temporaire institué par la décision
 * d'exécution (UE) 2022/382 du Conseil (activation directive 2001/55/CE) au
 * bénéfice des personnes déplacées d'Ukraine. L'outil vérifie l'éligibilité,
 * calcule la durée de protection restante, expose les droits (travail SANS
 * single permit, aides), le prochain renouvellement et le chemin procédural
 * (enregistrement Office des étrangers, attestation d'immatriculation, etc.).
 */

/**
 * Titre de séjour belge actuellement détenu par l'intéressé — whitelist stricte
 * (miroir backend) :
 *  - AUCUN                     : aucun titre encore délivré
 *  - ATTESTATION_IMMATRICULATION : attestation d'immatriculation (carte A « orange »)
 *  - TITRE_A                   : titre de séjour A (séjour limité)
 *  - TITRE_B                   : titre de séjour B
 *  - TITRE_AUTRE               : autre titre
 */
export type TitreSejourBE =
  | 'AUCUN'
  | 'ATTESTATION_IMMATRICULATION'
  | 'TITRE_A'
  | 'TITRE_B'
  | 'TITRE_AUTRE';

export interface ProtectionTemporaireUkraineBeRequest {
  /** Date d'arrivée sur le territoire (ISO yyyy-MM-dd). */
  dateArrivee: string;
  /** L'intéressé est-il de nationalité ukrainienne ? */
  nationaliteUkrainienne: boolean;
  /** Résidait-il en Ukraine avant le 24 février 2022 ? */
  residenceUkraineAvant24Fev2022: boolean;
  /** Apatride / ressortissant d'un pays tiers bénéficiant d'une protection en Ukraine ? */
  apatridesUkraine?: boolean | null;
  /** Membre de famille d'un bénéficiaire de la protection temporaire ? */
  membreFamilleProtege?: boolean | null;
  titreSejourBE: TitreSejourBE;
}

export interface ProtectionTemporaireUkraineBeResponse {
  caseFileId: string;
  dateArrivee: string;
  nationaliteUkrainienne: boolean;
  residenceUkraineAvant24Fev2022: boolean;
  apatridesUkraine?: boolean | null;
  membreFamilleProtege?: boolean | null;
  titreSejourBE: string;
  /** Éligibilité à la protection temporaire. */
  eligible: boolean;
  /** Durée de protection restante en JOURS calendaires (peut être 0). */
  dureeProtectionRestante: number;
  /** Droits au travail (texte) — mentionne l'absence de single permit requis. */
  droitsTravail: string;
  /** Droits aux aides (CPAS, allocations, scolarisation, soins...). */
  droitsAides: string[];
  /**
   * Prochain renouvellement : `true`/`false` (renouvellement requis prochainement)
   * OU un message explicatif. Le composant gère les deux formes.
   */
  prochainRenouvellement: boolean | string;
  /** Chemin procédural — étapes ordonnées (stepper / liste numérotée). */
  cheminProcedure: string[];
  baseJuridique?: string;
}

export interface TitreSejourBeOption {
  code: TitreSejourBE;
  label: string;
}

export const PROTECTION_TEMPORAIRE_UKRAINE_TITRES: ReadonlyArray<TitreSejourBeOption> = [
  { code: 'AUCUN', label: 'Aucun titre encore délivré' },
  { code: 'ATTESTATION_IMMATRICULATION', label: "Attestation d'immatriculation (carte A orange)" },
  { code: 'TITRE_A', label: 'Titre de séjour A (séjour limité)' },
  { code: 'TITRE_B', label: 'Titre de séjour B' },
  { code: 'TITRE_AUTRE', label: 'Autre titre' },
];

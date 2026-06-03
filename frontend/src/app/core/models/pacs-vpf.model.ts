/**
 * SF-220-04 : modèles miroirs du contrat API (backend SF-220-04) pour l'outil
 * décisionnel "VPF au titre d'un PACS L.423-23" (F-IM-50-pacs-vpf-fr). FR uniquement.
 *
 * Apprécie le PACS comme FAISCEAU d'indices de vie privée et familiale
 * (CESEDA L.423-23). Le PACS n'ouvre PAS de droit automatique au séjour
 * (distinct du conjoint marié F-IM-21) ; sa valeur probante dépend de
 * l'ancienneté (~1 an) et de l'intensité de la communauté de vie. Distinct de
 * F-IM-27 (VPF liens personnels L.423-23 générale) : angle propre au PACS.
 */

export type PartenaireStatut = 'FRANCAIS' | 'ETRANGER_REGULIER' | 'AUTRE';

export type IntensiteCommunauteVie = 'FORTE' | 'MOYENNE' | 'FAIBLE' | 'NON_ETABLIE';

export type EligibilitePacsVpf =
  | 'FAISCEAU_FAVORABLE'
  | 'FAISCEAU_INSUFFISANT'
  | 'A_CONSOLIDER'
  | 'NON_ELIGIBLE';

export interface PacsVpfRequest {
  pacsConclu: boolean;
  datePacs: string | null;
  partenaireStatut: PartenaireStatut;
  dureeVieCommuneMois: number | null;
  intensiteCommunauteVie: IntensiteCommunauteVie;
  autresLiensPrivesFamiliaux: boolean;
}

export interface PacsVpfResponse {
  caseFileId: string;
  pacsConclu: boolean;
  datePacs: string | null;
  partenaireStatut: PartenaireStatut;
  dureeVieCommuneMois: number | null;
  intensiteCommunauteVie: IntensiteCommunauteVie;
  autresLiensPrivesFamiliaux: boolean;
  country: string;
  eligibilite: EligibilitePacsVpf;
  elementsFavorables: string[];
  elementsManquants: string[];
  basesJuridiques: string[];
  messages: string[];
}

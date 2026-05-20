/**
 * SF-206-08 — Helper partagé pour l'outil "Résiliation judiciaire du contrat
 * de travail aux torts de l'employeur" (F-DT-40). Module pur — runtime
 * (`prefillFromAi()`) et static (`getPrefillCount()`) appellent les MÊMES
 * fonctions (contrat F-236 / F-237 / F-246).
 *
 * Le pipeline IA (SF-206-07 backend) extrait un sous-objet
 * `resiliation_judiciaire_detail` (12 champs) projeté à plat sur
 * `TravailExtractedData`. FRANCE uniquement (`workspaceCountry === 'FRANCE'` —
 * la résiliation judiciaire CPH avec effets licenciement sans cause à la
 * date du jugement est un mécanisme franco-français — Cass. soc. 16/03/1989 ;
 * Cass. soc. 20/01/1998 ; art. L.1411-1 CT ; art. 1224, 1227-1228 C. civ.).
 *
 *   defautPaiementSalaire                ← aiData.resiliationJudDefautPaiementSalaire
 *   montantImpayesEur                    ← aiData.resiliationJudMontantImpayes
 *   harcelement                          ← aiData.resiliationJudHarcelement
 *   manquementSecurite                   ← aiData.resiliationJudManquementSecurite
 *   modificationUnilateraleContrat       ← aiData.resiliationJudModificationContrat
 *   declassement                         ← aiData.resiliationJudDeclassement
 *   discrimination                       ← aiData.resiliationJudDiscrimination
 *   heuresSupNonPayees                   ← aiData.resiliationJudHeuresSupNonPayees
 *   nonRespectDureesRepos                ← aiData.resiliationJudNonRespectRepos
 *   manquementsPersistantsAuJourDemande  ← aiData.resiliationJudManquementsPersistants
 *   salarieToujoursEnPoste               ← aiData.resiliationJudSalarieEnPoste
 *   licenciementIntervenuEnCoursInstance ← aiData.resiliationJudLicenciementEnCours
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface ResiliationJudiciaireCphPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'resiliationJudDefautPaiementSalaire'
    | 'resiliationJudMontantImpayes'
    | 'resiliationJudHarcelement'
    | 'resiliationJudManquementSecurite'
    | 'resiliationJudModificationContrat'
    | 'resiliationJudDeclassement'
    | 'resiliationJudDiscrimination'
    | 'resiliationJudHeuresSupNonPayees'
    | 'resiliationJudNonRespectRepos'
    | 'resiliationJudManquementsPersistants'
    | 'resiliationJudSalarieEnPoste'
    | 'resiliationJudLicenciementEnCours'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: ResiliationJudiciaireCphPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function bool(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `defautPaiementSalaire`. */
export function computeDefautPaiement(input: ResiliationJudiciaireCphPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudDefautPaiementSalaire);
}

/** Décimal ≥ 0 — `montantImpayesEur`. */
export function computeMontantImpayes(input: ResiliationJudiciaireCphPrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.resiliationJudMontantImpayes;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return v;
}

/** Booléen direct — `harcelement`. */
export function computeHarcelement(input: ResiliationJudiciaireCphPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudHarcelement);
}

/** Booléen direct — `manquementSecurite`. */
export function computeManquementSecurite(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudManquementSecurite);
}

/** Booléen direct — `modificationUnilateraleContrat`. */
export function computeModificationContrat(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudModificationContrat);
}

/** Booléen direct — `declassement`. */
export function computeDeclassement(input: ResiliationJudiciaireCphPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudDeclassement);
}

/** Booléen direct — `discrimination`. */
export function computeDiscrimination(input: ResiliationJudiciaireCphPrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudDiscrimination);
}

/** Booléen direct — `heuresSupNonPayees`. */
export function computeHeuresSupNonPayees(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudHeuresSupNonPayees);
}

/** Booléen direct — `nonRespectDureesRepos`. */
export function computeNonRespectRepos(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudNonRespectRepos);
}

/** Booléen direct — `manquementsPersistantsAuJourDemande`. */
export function computeManquementsPersistants(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudManquementsPersistants);
}

/** Booléen direct — `salarieToujoursEnPoste`. */
export function computeSalarieEnPoste(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudSalarieEnPoste);
}

/** Booléen direct — `licenciementIntervenuEnCoursInstance`. */
export function computeLicenciementEnCours(
  input: ResiliationJudiciaireCphPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.resiliationJudLicenciementEnCours);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 12 champs IA — `manquementsCommentaire` reste à la saisie avocat.
 */
export function computePrefillCount(input: ResiliationJudiciaireCphPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeDefautPaiement(input) !== null) count++;
  if (computeMontantImpayes(input) !== null) count++;
  if (computeHarcelement(input) !== null) count++;
  if (computeManquementSecurite(input) !== null) count++;
  if (computeModificationContrat(input) !== null) count++;
  if (computeDeclassement(input) !== null) count++;
  if (computeDiscrimination(input) !== null) count++;
  if (computeHeuresSupNonPayees(input) !== null) count++;
  if (computeNonRespectRepos(input) !== null) count++;
  if (computeManquementsPersistants(input) !== null) count++;
  if (computeSalarieEnPoste(input) !== null) count++;
  if (computeLicenciementEnCours(input) !== null) count++;
  return count;
}

export const ResiliationJudiciaireCphSectionPrefillRules = {
  computeDefautPaiement,
  computeMontantImpayes,
  computeHarcelement,
  computeManquementSecurite,
  computeModificationContrat,
  computeDeclassement,
  computeDiscrimination,
  computeHeuresSupNonPayees,
  computeNonRespectRepos,
  computeManquementsPersistants,
  computeSalarieEnPoste,
  computeLicenciementEnCours,
  computePrefillCount,
};

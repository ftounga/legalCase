/**
 * SF-206-06 — Helper partagé pour l'outil "Prise d'acte de la rupture aux
 * torts de l'employeur" (F-DT-39). Module pur — runtime (`prefillFromAi()`)
 * et static (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236
 * / F-237 / F-246).
 *
 * Le pipeline IA (SF-206-05 backend) extrait un sous-objet `prise_acte_detail`
 * (11 champs) projeté à plat sur `TravailExtractedData`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — la prise d'acte CPH avec effets
 * licenciement / démission est un mécanisme franco-français, Cass. soc.
 * 25/06/2003 n°01-42.679 — aucun équivalent direct en droit belge).
 *
 *   defautPaiementSalaire             ← aiData.priseActeDefautPaiementSalaire
 *   montantImpayesEur                 ← aiData.priseActeMontantImpayes
 *   harcelement                       ← aiData.priseActeHarcelement
 *   manquementSecurite                ← aiData.priseActeManquementSecurite
 *   modificationUnilateraleContrat    ← aiData.priseActeModificationContrat
 *   declassement                      ← aiData.priseActeDeclassement
 *   discrimination                    ← aiData.priseActeDiscrimination
 *   heuresSupNonPayees                ← aiData.priseActeHeuresSupNonPayees
 *   nonRespectDureesRepos             ← aiData.priseActeNonRespectRepos
 *   griefsActuelsEtPersistants        ← aiData.priseActeGriefsPersistants
 *   griefRendImpossiblePoursuite      ← aiData.priseActeGriefImpossiblePoursuite
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface PriseActeRupturePrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'priseActeDefautPaiementSalaire'
    | 'priseActeMontantImpayes'
    | 'priseActeHarcelement'
    | 'priseActeManquementSecurite'
    | 'priseActeModificationContrat'
    | 'priseActeDeclassement'
    | 'priseActeDiscrimination'
    | 'priseActeHeuresSupNonPayees'
    | 'priseActeNonRespectRepos'
    | 'priseActeGriefsPersistants'
    | 'priseActeGriefImpossiblePoursuite'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: PriseActeRupturePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function bool(v: boolean | null | undefined): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `defautPaiementSalaire`. */
export function computeDefautPaiement(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeDefautPaiementSalaire);
}

/** Décimal ≥ 0 — `montantImpayesEur`. */
export function computeMontantImpayes(input: PriseActeRupturePrefillInput): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.priseActeMontantImpayes;
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return v;
}

/** Booléen direct — `harcelement`. */
export function computeHarcelement(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeHarcelement);
}

/** Booléen direct — `manquementSecurite`. */
export function computeManquementSecurite(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeManquementSecurite);
}

/** Booléen direct — `modificationUnilateraleContrat`. */
export function computeModificationContrat(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeModificationContrat);
}

/** Booléen direct — `declassement`. */
export function computeDeclassement(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeDeclassement);
}

/** Booléen direct — `discrimination`. */
export function computeDiscrimination(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeDiscrimination);
}

/** Booléen direct — `heuresSupNonPayees`. */
export function computeHeuresSupNonPayees(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeHeuresSupNonPayees);
}

/** Booléen direct — `nonRespectDureesRepos`. */
export function computeNonRespectRepos(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeNonRespectRepos);
}

/** Booléen direct — `griefsActuelsEtPersistants`. */
export function computeGriefsPersistants(input: PriseActeRupturePrefillInput): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeGriefsPersistants);
}

/** Booléen direct — `griefRendImpossiblePoursuite`. */
export function computeGriefImpossiblePoursuite(
  input: PriseActeRupturePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  return bool(input.aiData?.priseActeGriefImpossiblePoursuite);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 11 champs IA — `griefsCommentaire` reste à la saisie avocat.
 */
export function computePrefillCount(input: PriseActeRupturePrefillInput): number {
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
  if (computeGriefsPersistants(input) !== null) count++;
  if (computeGriefImpossiblePoursuite(input) !== null) count++;
  return count;
}

export const PriseActeRuptureSectionPrefillRules = {
  computeDefautPaiement,
  computeMontantImpayes,
  computeHarcelement,
  computeManquementSecurite,
  computeModificationContrat,
  computeDeclassement,
  computeDiscrimination,
  computeHeuresSupNonPayees,
  computeNonRespectRepos,
  computeGriefsPersistants,
  computeGriefImpossiblePoursuite,
  computePrefillCount,
};

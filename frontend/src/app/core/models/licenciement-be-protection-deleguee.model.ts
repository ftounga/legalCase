/**
 * SF-213-08b : modèle frontend de l'outil F-213 — Protection du délégué
 * syndical / candidat non élu BE contre le licenciement.
 *
 * <p>BE uniquement — <b>Loi du 19 mars 1991</b> (protection des
 * représentants des travailleurs au CE et CPPT) +
 * <b>CCT n° 5 du 24 mai 1971</b> (statut des délégations syndicales).
 * La France a un dispositif distinct (statut protégé délégué / CSE /
 * conseiller prud'homme — L. 2411-1 et s. C. trav.) — outil dédié hors
 * scope V1.</p>
 *
 * <p>Contrat figé dans SF-213-08 backend
 * ({@code LicenciementBeProtectionDelegueeResponse} +
 * {@code LicenciementBeProtectionDelegueeRequest}).</p>
 *
 * <p>Verdict binaire (2 états) :
 * <ul>
 *   <li>{@code LICENCIEMENT_INTERDIT_SANS_PROCEDURE} — la rupture
 *       intervient dans la fenêtre de protection : interdite sauf motif
 *       grave reconnu ou raisons d'ordre économique/technique reconnues
 *       par la juridiction. Indemnité forfaitaire = 2 ans (4 si
 *       circonstances aggravantes / récidive). Délai 30 jours pour
 *       demander la réintégration (Loi 19/03/1991 art. 14).</li>
 *   <li>{@code HORS_PERIODE_PROTECTION} — régime du licenciement de
 *       droit commun (préavis, indemnité compensatoire, motif grave).</li>
 * </ul></p>
 */

/**
 * Statut protégé du travailleur (4 cas — pilote la durée de la fenêtre).
 *
 * <ul>
 *   <li>{@code DELEGUE_SYNDICAL_ELU} — élu CE/CPPT — mandat 4 ans + 2 ans résiduels.</li>
 *   <li>{@code CANDIDAT_NON_ELU} — candidat aux élections sociales — 2 ans.</li>
 *   <li>{@code DELEGUE_CCT5} — délégué syndical CCT n° 5 — 2 ans.</li>
 *   <li>{@code CONSEILLER_CPPT} — membre CPPT élu — 4 ans cycle.</li>
 * </ul>
 */
export type LicenciementBeStatutProtege =
  | 'DELEGUE_SYNDICAL_ELU'
  | 'CANDIDAT_NON_ELU'
  | 'DELEGUE_CCT5'
  | 'CONSEILLER_CPPT';

/**
 * Verdict binaire de validité du licenciement vis-à-vis de la protection.
 */
export type LicenciementBeProtectionDelegueeVerdict =
  | 'LICENCIEMENT_INTERDIT_SANS_PROCEDURE'
  | 'HORS_PERIODE_PROTECTION';

/**
 * Body POST `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee`.
 *
 * <p>Champs obligatoires : {@code statutProtege}, {@code ancienneteAnnees},
 * {@code remunerationAnnuelleBrute}, {@code dateLicenciement},
 * {@code dateElectionOuMandat}. Les flags par défaut {@code false}.</p>
 *
 * <p>Cohérence Validator : {@code employeurRefuseReintegration=true}
 * implique {@code demandeReintegrationDansTrente=true} (sinon 400).</p>
 */
export interface LicenciementBeProtectionDelegueeRequest {
  /** Obligatoire. */
  statutProtege: LicenciementBeStatutProtege;
  /** Obligatoire — années pleines, ≥ 0. */
  ancienneteAnnees: number;
  /** Obligatoire — base du calcul de l'indemnité forfaitaire (€), > 0. */
  remunerationAnnuelleBrute: number;
  /** Obligatoire — ISO yyyy-MM-dd. */
  dateLicenciement: string;
  /** Obligatoire — ISO yyyy-MM-dd ; pivot fenêtre protection. */
  dateElectionOuMandat: string;
  /** Le délégué a-t-il demandé la réintégration dans les 30 jours ? */
  demandeReintegrationDansTrente: boolean;
  /** L'employeur a-t-il refusé ? (requis seulement si demande faite). */
  employeurRefuseReintegration: boolean;
  /** Circonstances aggravantes / récidive → barème 4 ans. */
  circonstancesAggravantes: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface LicenciementBeProtectionDelegueeResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;

  // -- Echo inputs --
  statutProtege: LicenciementBeStatutProtege;
  ancienneteAnnees: number;
  remunerationAnnuelleBrute: number;
  dateLicenciement: string;
  dateElectionOuMandat: string;
  demandeReintegrationDansTrente: boolean;
  employeurRefuseReintegration: boolean;
  circonstancesAggravantes: boolean;

  // -- Outputs métier --
  /** Verdict binaire — INTERDIT (rouge) ou HORS_PROTECTION (vert). */
  verdict: LicenciementBeProtectionDelegueeVerdict;
  /** Vrai si le licenciement tombe dans la fenêtre de protection. */
  licenciementDansProtection: boolean;
  /** Fin de la fenêtre de protection (ISO). */
  dateFinProtection: string | null;
  /** Indemnité forfaitaire (€) = remunerationAnnuelleBrute × anneesForfait. */
  indemniteForfaitaire: number | null;
  /** Nombre d'années forfait (2 par défaut, 4 si circonstances aggravantes). */
  anneesForfait: number | null;
  /** Date limite de la demande de réintégration (ISO) = dateLicenciement + 30j. */
  dateLimiteDemandeReintegration: string | null;
  /** Nombre de jours restants avant la dateLimiteDemandeReintegration (peut être négatif). */
  joursRestantsReintegration: number | null;
  /** Vrai si le délai 30 jours est dépassé. */
  delaiReintegrationDepasse: boolean;
  /** Base juridique humanisée (Loi 19/03/1991 + CCT n° 5). */
  baseJuridique: string;
  /** Avertissement éventuel (délai dépassé, employeur refuse réintégration…). */
  avertissement: string | null;
}

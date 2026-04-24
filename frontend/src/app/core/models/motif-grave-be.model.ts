/**
 * SF-DT-27-02 : types frontend pour l'outil décisionnel
 * "Motif grave BE — validité art. 35 Loi 03/07/1978 + indemnisation
 * préavis + fourchette CCT 109". Contrat calqué sur
 * `MotifGraveBeRequest` / `MotifGraveBeResponse` backend (SF-DT-27-01).
 *
 * BE uniquement. L'équivalent FR (faute grave disciplinaire) est couvert
 * par F-DT-08 (validité disciplinaire) et F-DT-01 (licenciement simple).
 */

export interface MotifGraveBeRequest {
  /** Date de connaissance du fait fautif (YYYY-MM-DD). */
  dateConnaissanceFait: string;
  /** Date de notification de la rupture (YYYY-MM-DD). */
  dateNotificationRupture: string;
  /** Date de notification des motifs par recommandé (YYYY-MM-DD). */
  dateNotificationMotifs: string;
  /** Ancienneté en années (entier ≥ 0). */
  anciennetteAnnees: number;
  /** Salaire mensuel brut de référence (> 0). */
  salaireMensuelReference: number;
}

export interface MotifGraveBeResponse {
  caseFileId: string;
  dateConnaissanceFait: string;
  dateNotificationRupture: string;
  dateNotificationMotifs: string;
  anciennetteAnnees: number;
  salaireMensuelReference: number;
  /** Nb de jours ouvrables BE entre connaissance et rupture. */
  delaiRuptureJoursOuvrables: number;
  /** Nb de jours ouvrables BE entre rupture et notification motifs. */
  delaiMotifsJoursOuvrables: number;
  /** True si les 2 délais ≤ 3 j ouvrables (motif grave procéduralement valable). */
  motifGraveProceduralementValide: boolean;
  /** Indemnité compensatoire préavis (loi 26/12/2013 approximée) si invalide. */
  indemnitePreavisSiInvalide: number;
  /** Borne basse fourchette CCT 109 (3 semaines) si invalide. */
  indemniteManifestementDeraisonnableMin: number;
  /** Borne haute fourchette CCT 109 (17 semaines) si invalide. */
  indemniteManifestementDeraisonnableMax: number;
  /** Formule en texte JetBrains Mono. */
  formule: string;
  /** Base juridique en texte JetBrains Mono italique. */
  baseJuridique: string;
  /** Messages explicatifs (rendus via LegalCitationsPipe). */
  messages: string[];
}

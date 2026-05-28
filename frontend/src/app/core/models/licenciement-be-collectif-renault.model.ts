/**
 * SF-219-07b : modèle frontend de l'outil F-219 — Licenciement collectif
 * BE selon la <b>Loi du 13/02/1998</b> (procédure dite « Renault ») +
 * <b>CCT n° 24</b> (information / consultation des travailleurs) +
 * <b>CCT n° 39</b> (mesures sociales d'accompagnement) +
 * <b>Directive 98/59/CE</b>.
 *
 * <p><b>BE uniquement</b> — le PSE français (Code du travail art.
 * L. 1233-21 et s.) est procéduralement très différent (DIRECCTE / DDETS,
 * validation/homologation, calendrier d'expertise CSE) et ne peut pas
 * être mappé à la procédure Renault.</p>
 *
 * <p>Contrat figé dans SF-219-07 backend
 * ({@code LicenciementBeCollectifRenaultRequest} +
 * {@code LicenciementBeCollectifRenaultResponse}).</p>
 */

/** Verdict 6 états de l'outil checklist procédurale licenciement collectif BE. */
export type LicenciementBeCollectifRenaultVerdict =
  | 'NON_APPLICABLE_SEUIL'
  | 'CONFORME'
  | 'NON_CONFORME_PHASE_INFORMATION_INCOMPLETE'
  | 'NON_CONFORME_CONSULTATION_INSUFFISANTE'
  | 'NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE'
  | 'NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE';

/**
 * Tranche de taille de l'entreprise — détermine le seuil de
 * licenciements déclenchant l'application de la Loi 13/02/1998.
 *
 * <ul>
 *   <li>{@code PETITE_MOINS_20} — effectif &lt; 20, procédure Renault
 *       non applicable (CCT n° 9 reste éventuellement applicable).</li>
 *   <li>{@code PME_20_99} — effectif 20-99, seuil = 10 licenciements / 60 j.</li>
 *   <li>{@code GRANDE_100_299} — effectif 100-299, seuil = 10 % de
 *       l'effectif / 60 j.</li>
 *   <li>{@code TRES_GRANDE_300_PLUS} — effectif ≥ 300, seuil = 30
 *       licenciements / 60 j.</li>
 * </ul>
 */
export type LicenciementBeCollectifRenaultTailleEntreprise =
  | 'PETITE_MOINS_20'
  | 'PME_20_99'
  | 'GRANDE_100_299'
  | 'TRES_GRANDE_300_PLUS';

/**
 * Phase courante de la procédure Loi Renault atteinte par l'employeur.
 *
 * <ul>
 *   <li>{@code AUCUNE_PROCEDURE_DEMARREE} — projet seulement.</li>
 *   <li>{@code INFORMATION} — phase 1 : information écrite préalable du
 *       CE / CPPT / DS (CCT n° 24 art. 6).</li>
 *   <li>{@code CONSULTATION} — phase 2 : consultation effective +
 *       réponses motivées employeur (CCT n° 24 art. 7 + CCT n° 39).</li>
 *   <li>{@code DECISION_ET_NOTIFICATION} — phase 3 : notification à
 *       l'autorité régionale (Forem / Actiris / VDAB) + délai d'attente
 *       30 jours.</li>
 * </ul>
 */
export type LicenciementBeCollectifRenaultPhase =
  | 'AUCUNE_PROCEDURE_DEMARREE'
  | 'INFORMATION'
  | 'CONSULTATION'
  | 'DECISION_ET_NOTIFICATION';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-collectif-renault}.
 *
 * <p>Champs obligatoires alignés sur le backend
 * ({@code @NotNull / @PositiveOrZero}). Les dates optionnelles
 * ({@code dateNotificationAutorite}, {@code datePremierPreavisNotifie})
 * peuvent rester {@code null} si pas applicables.</p>
 */
export interface LicenciementBeCollectifRenaultRequest {
  /** Date à laquelle l'employeur a annoncé son intention (point de départ fenêtre 60 j). */
  dateProjet: string;
  /** Tranche de taille de l'entreprise. */
  tailleEntreprise: LicenciementBeCollectifRenaultTailleEntreprise;
  /** Effectif moyen (4 trimestres civils précédents). */
  effectifMoyen: number;
  /** Nombre de licenciements projetés sur la fenêtre de 60 jours. */
  nombreLicenciementsEnvisages: number;
  /** Phase courante de la procédure. */
  phaseAtteinte: LicenciementBeCollectifRenaultPhase;
  /** Notification écrite préalable au CE/CPPT/DS effectuée. */
  informationEcriteCePrealable: boolean;
  /** Documents légaux (motifs, catégories, critères) communiqués. */
  documentsLegauxCommuniques: boolean;
  /** Réunion(s) consultative(s) effectivement tenue(s). */
  consultationEffectiveTenue: boolean;
  /** Réponses écrites motivées employeur aux questions/contre-propositions. */
  reponsesMotiveesEmployeur: boolean;
  /** Notification écrite à l'autorité régionale (Forem / Actiris / VDAB) effectuée. */
  notificationAutoriteRegionaleFaite: boolean;
  /** Date de notification à l'autorité (point de départ délai 30 j). */
  dateNotificationAutorite: string | null;
  /** Date du 1er préavis individuel notifié (si applicable). */
  datePremierPreavisNotifie: string | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs métier.
 */
export interface LicenciementBeCollectifRenaultResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateProjet: string;
  tailleEntreprise: LicenciementBeCollectifRenaultTailleEntreprise;
  effectifMoyen: number;
  nombreLicenciementsEnvisages: number;
  phaseAtteinte: LicenciementBeCollectifRenaultPhase;
  informationEcriteCePrealable: boolean;
  documentsLegauxCommuniques: boolean;
  consultationEffectiveTenue: boolean;
  reponsesMotiveesEmployeur: boolean;
  notificationAutoriteRegionaleFaite: boolean;
  dateNotificationAutorite: string | null;
  datePremierPreavisNotifie: string | null;

  // -- Outputs métier --
  /** Verdict 6 états (NON_APPLICABLE_SEUIL / CONFORME / 4× NON_CONFORME_*). */
  verdict: LicenciementBeCollectifRenaultVerdict;
  /** True ssi {@code verdict === 'CONFORME'} (compatibilité UI). */
  conforme: boolean;
  /** Seuil de licenciements légal applicable (selon taille). */
  seuilDeclenchementAtteint: boolean;
  /** Nombre minimal de licenciements / 60 j déclenchant Loi Renault. */
  seuilLegalApplicable: number;
  /** Date d'expiration du délai d'attente 30 jours (si notification faite). */
  dateFinDelaiAttente: string | null;
  /** True si aucun préavis n'a été notifié avant la fin du délai 30 j. */
  delaiAttenteRespecte: boolean;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict. */
  raison: string;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}

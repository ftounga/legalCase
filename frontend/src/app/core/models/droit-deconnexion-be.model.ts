/**
 * SF-219-19b : modèle frontend de l'outil F-219 — droit à la
 * déconnexion BE (Loi du 03/10/2022 « Deal pour l'emploi » M.B.
 * 10/11/2022, art. 16 + AR du 19/02/2023 + CCT n° 149).
 *
 * <p><b>BE uniquement</b> — le droit à la déconnexion français
 * (C. trav. art. L. 2242-17, 7° ; Loi n° 2016-1088 du 08/08/2016
 * dite « El Khomri ») constitue un régime distinct (obligation de
 * négociation annuelle dans les entreprises ≥ 50 salariés, et non
 * obligation de résultat pesant sur les entreprises ≥ 20
 * travailleurs). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-19 backend
 * ({@code DroitDeconnexionBeRequest} + {@code DroitDeconnexionBeResponse}).</p>
 */

/**
 * Statut de l'accord (CCT d'entreprise ou modification du règlement
 * de travail) qui formalise les modalités du droit à la déconnexion BE.
 *
 * <ul>
 *   <li>{@code CCT_ENTREPRISE_DEPOSEE} — CCT d'entreprise négociée
 *       et déposée au greffe du SPF Emploi (instrument privilégié).</li>
 *   <li>{@code REGLEMENT_TRAVAIL_MODIFIE} — modification du règlement
 *       de travail par procédure ordinaire (Loi 08/04/1965).</li>
 *   <li>{@code NEGOCIATION_EN_COURS} — concertation engagée mais
 *       aucun instrument formalisé.</li>
 *   <li>{@code AUCUNE_INITIATIVE} — manquement frontal à l'obligation
 *       légale.</li>
 *   <li>{@code INDETERMINE} — pas encore d'instruction (analyse cas
 *       par cas).</li>
 * </ul>
 */
export type DroitDeconnexionBeStatutAccord =
  | 'CCT_ENTREPRISE_DEPOSEE'
  | 'REGLEMENT_TRAVAIL_MODIFIE'
  | 'NEGOCIATION_EN_COURS'
  | 'AUCUNE_INITIATIVE'
  | 'INDETERMINE';

/**
 * Verdict hiérarchisé 6 états — ordre de priorité (court-circuits
 * successifs côté backend {@code DroitDeconnexionBeChecker}) :
 *
 * <ol>
 *   <li>{@code HORS_CHAMP_SEUIL_NON_ATTEINT} — employeur &lt; 20
 *       travailleurs : obligation déconnexion non applicable
 *       (art. 16 § 1).</li>
 *   <li>{@code MANQUEMENT_GRAVE_AUCUNE_INITIATIVE} — aucun accord
 *       ni amorce de concertation : manquement frontal exposant
 *       l'employeur aux sanctions du C. pén. social (art. 137 —
 *       sanction niveau 2) et à l'action en cessation.</li>
 *   <li>{@code NON_CONFORME_INSTRUMENT_MANQUANT} — concertation
 *       engagée mais aucun instrument formalisé : manquement à
 *       l'obligation de résultat (art. 16 § 1).</li>
 *   <li>{@code NON_CONFORME_CONTENU_INCOMPLET} — instrument
 *       formalisé mais thèmes obligatoires manquants
 *       (art. 16 § 2).</li>
 *   <li>{@code CONFORME_ACCORD_COMPLET} — CCT ou règlement de
 *       travail formalisé ET 3 thèmes art. 16 § 2 traités.</li>
 *   <li>{@code A_ANALYSER} — configuration ambigüe (statut
 *       indéterminé) → analyse cas par cas.</li>
 * </ol>
 */
export type DroitDeconnexionBeVerdict =
  | 'HORS_CHAMP_SEUIL_NON_ATTEINT'
  | 'CONFORME_ACCORD_COMPLET'
  | 'NON_CONFORME_CONTENU_INCOMPLET'
  | 'NON_CONFORME_INSTRUMENT_MANQUANT'
  | 'MANQUEMENT_GRAVE_AUCUNE_INITIATIVE'
  | 'A_ANALYSER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/droit-deconnexion-be}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}) sauf
 * {@code dateEntreeVigueurInstrument} qui n'est pertinente que si
 * un instrument est formalisé (CCT déposée ou règlement modifié).</p>
 */
export interface DroitDeconnexionBeRequest {
  /** Effectif total travailleurs (seuil art. 16 § 1 : 20). */
  effectifEntreprise: number;
  /** Statut de l'accord (aiguille le verdict). */
  statutAccord: DroitDeconnexionBeStatutAccord;
  /** Date dépôt CCT ou publication règlement modifié (≥ 01/04/2023). */
  dateEntreeVigueurInstrument: string | null;
  /** Modalités pratiques de déconnexion définies (art. 16 § 2 1°). */
  modalitesPratiquesDeconnexionDefinies: boolean;
  /** Sensibilisation / formation prévue (art. 16 § 2 2°). */
  sensibilisationFormationPrevue: boolean;
  /** Modalités d'organisation du travail définies (art. 16 § 2 3°). */
  modalitesOrganisationTravailDefinies: boolean;
  /** Consultation organe concertation CE / CPPT / DS effectuée. */
  consultationOrganeConcertationEffectuee: boolean;
  /** Manquement signalé au CBE ou SPF Emploi (contentieux en cours). */
  manquementSignaleCbeOuSpf: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + ventilation
 * des conformités + verdict + métadonnées légales.
 */
export interface DroitDeconnexionBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  effectifEntreprise: number;
  statutAccord: DroitDeconnexionBeStatutAccord;
  dateEntreeVigueurInstrument: string | null;
  modalitesPratiquesDeconnexionDefinies: boolean;
  sensibilisationFormationPrevue: boolean;
  modalitesOrganisationTravailDefinies: boolean;
  consultationOrganeConcertationEffectuee: boolean;
  manquementSignaleCbeOuSpf: boolean;

  // -- Verdict + ventilation --
  verdict: DroitDeconnexionBeVerdict;
  /** Seuil de 20 travailleurs atteint (art. 16 § 1). */
  seuilAtteint: boolean;
  /** Instrument formalisé (CCT déposée ou règlement modifié). */
  instrumentFormalise: boolean;
  /** Contenu complet (3 thèmes art. 16 § 2 traités). */
  contenuComplet: boolean;
  /** Modalités pratiques déconnexion respectées (art. 16 § 2 1°). */
  modalitesPratiquesRespectees: boolean;
  /** Sensibilisation / formation respectée (art. 16 § 2 2°). */
  sensibilisationRespectee: boolean;
  /** Modalités organisation travail respectées (art. 16 § 2 3°). */
  modalitesOrganisationRespectees: boolean;
  /** Consultation organe concertation respectée. */
  consultationRespectee: boolean;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement (jurisprudence post-2023 limitée, articulation CCT 149, etc.). */
  avertissement: string;
}

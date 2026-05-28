import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-18b — Helper partagé pour le pré-remplissage IA de
 * {@code Semaine4JoursBeSectionComponent} (F-219, BE uniquement —
 * Loi du 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022, art. 5
 * + art. 6 ; Loi 16/03/1971 art. 19 ; Loi 03/07/1978 art. 25 ; Loi
 * 08/04/1965 art. 12).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * antérieures (0 champ pré-rempli). Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code statutDemande} — qualification administrative de la
 *       demande (accordée / refusée / en attente / licenciement),
 *       déclaratif avocat, hors pipeline.</li>
 *   <li>{@code travailleurTempsPlein} — flag temps plein /
 *       temps partiel propre au régime semaine 4 jours
 *       (art. 5 § 1) ; le record Travail BE n'extrait pas ce
 *       statut horaire.</li>
 *   <li>{@code demandeEcriteTravailleur} / {@code dateDemande} —
 *       formalisme + date d'une demande spécifique (lettre
 *       recommandée), donnée RH employeur non extractible du
 *       dossier salarié principal.</li>
 *   <li>{@code dureeHebdomadaireHeures} — horaire collectif
 *       sectoriel CCT (38 à 40 h), donnée employeur hors
 *       pipeline.</li>
 *   <li>{@code journeeMaximaleHeures} / {@code cctAutorise10h} —
 *       paramètre de l'horaire compressé spécifique au régime,
 *       hors pipeline.</li>
 *   <li>{@code avenantEcritSigne} / {@code dureeAvenantMois} /
 *       {@code avenantRenouvele} — formalisation contractuelle
 *       (annexe au contrat), donnée employeur RH, hors pipeline.</li>
 *   <li>{@code reglementTravailModifie} — flag procédure simplifiée
 *       art. 6 Loi 03/10/2022, donnée employeur, hors pipeline.</li>
 *   <li>{@code refusMotiveParEcrit} — qualification juridique du
 *       refus (art. 5 § 5), déclaratif avocat.</li>
 *   <li>{@code dateLicenciement} — bien que {@code dateRuptureContrat}
 *       existe dans {@code TravailExtractedData}, le lien causal
 *       avec la demande semaine 4 jours (présomption de
 *       représailles art. 5 § 6) reste un déclaratif avocat — pas
 *       de pré-fill mécanique V1 pour éviter des faux positifs sur
 *       les licenciements antérieurs à la demande.</li>
 *   <li>{@code motifLicenciementObjectifEtabli} — qualification
 *       juridique (motif sans rapport avec la demande), déclaratif
 *       avocat exclusivement.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code semaine-4-jours-be-section-prefill-rules.spec.ts}.</p>
 */
export const Semaine4JoursBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier RH employeur (demande écrite du travailleur, accusé de
   * réception, avenant au contrat, règlement de travail modifié,
   * éventuelle lettre de licenciement).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

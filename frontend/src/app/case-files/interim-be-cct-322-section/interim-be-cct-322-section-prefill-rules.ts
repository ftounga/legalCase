import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-14b — Helper partagé pour le pré-remplissage IA de
 * {@code InterimBeCct322SectionComponent} (F-219, BE uniquement —
 * Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 + CCT n° 108 du
 * 16/07/2013 + AR 11/10/1976 + Loi-programme du 27/12/2012).
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
 *   <li>{@code dateDebutMission} — date de début de la 1re prestation
 *       intérimaire chez CET utilisateur (donnée mission ETI, hors
 *       trajectoire du salarié principal extraite par le pipeline).</li>
 *   <li>{@code motifMission} — motif limitativement énuméré (remplacement,
 *       surcroît, travail exceptionnel, insertion, artistique, flux).
 *       Donnée mission ETI / utilisateur non extractible.</li>
 *   <li>{@code remplacementGreveOuLockout} — flag art. 19 (interdiction
 *       absolue). Information opérationnelle déclarative non extractible.</li>
 *   <li>{@code dureeTotaleMissionJours} — durée cumulée des contrats
 *       successifs ETI. Donnée opérationnelle ETI non extractible.</li>
 *   <li>{@code dureeMaxLegaleJours} — plafond légal applicable au motif
 *       (paramétrable selon AR / CCT). Constante réglementaire — saisie
 *       par l'avocat selon le motif et la date d'occupation.</li>
 *   <li>{@code salaireHoraireIntimaireBrut} — taux horaire intérimaire.
 *       Donnée contrat ETI/intérimaire non rattachable au salaire mensuel
 *       du salarié principal extrait par le pipeline.</li>
 *   <li>{@code salaireHoraireReferenceBrut} — taux horaire d'un permanent
 *       de même qualification chez l'utilisateur (art. 10). Donnée
 *       comparative utilisateur non extractible.</li>
 *   <li>{@code contratEcritSigne} — formalisme contrat écrit ETI/intérimaire
 *       (art. 8 § 1). Document hors pipeline Travail BE standard.</li>
 *   <li>{@code dimonaDeclareeParEti} — déclaration Dimona ETI (DmfA ONSS).
 *       Donnée administrative employeur ETI non extractible.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code interim-be-cct-322-section-prefill-rules.spec.ts}.</p>
 */
export const InterimBeCct322SectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier mission ETI (contrat de travail intérimaire, attestation
   * Dimona ETI, contrat de mise à disposition utilisateur, paramètres
   * légaux applicables au motif).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

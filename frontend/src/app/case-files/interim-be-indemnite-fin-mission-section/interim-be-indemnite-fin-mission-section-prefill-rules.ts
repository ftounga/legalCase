import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-15b — Helper partagé pour le pré-remplissage IA de
 * {@code InterimBeIndemniteFinMissionSectionComponent} (F-219, BE
 * uniquement — Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 + CCT
 * n° 322bis du 16/12/2010 + AR du 30/03/1967 art. 19 + CCT
 * sectorielles + jurisprudence Cass. BE 04/02/1991 et 03/05/2010).
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
 *   <li>{@code dateDebutMission} / {@code dateFinPrevue} /
 *       {@code dateFinReelle} — ancrages temporels de la mission
 *       ETI / utilisateur, hors trajectoire du salarié principal
 *       extrait par le pipeline.</li>
 *   <li>{@code dureeReellePrestationJours} / {@code dureePrevueJours}
 *       — données opérationnelles ETI (calendrier mission) non
 *       extractibles du salarié principal.</li>
 *   <li>{@code salaireHoraireBrut} — taux horaire intérimaire contrat
 *       ETI, non rattachable au salaire mensuel du salarié principal.</li>
 *   <li>{@code heuresPrestees} / {@code heuresSupplementairesSemaine}
 *       / {@code heuresSupplementairesDimancheFerie} — relevés
 *       d'heures du contrat ETI, hors pipeline Travail BE standard.</li>
 *   <li>{@code peculeDejaVerseParFsi} — flag administratif Fonds
 *       Social Intérimaires (FSI), donnée employeur ETI / FSI non
 *       extractible.</li>
 *   <li>{@code ruptureAnticipeeParEtiSansMotifGrave} — qualification
 *       juridique de la rupture (art. 39 Loi 03/07/1978 par renvoi
 *       Loi 24/07/1987 art. 8) — déclaratif avocat, hors pipeline.</li>
 *   <li>{@code commissionParitaireUtilisateur} — code CP sectorielle
 *       de l'utilisateur (parité art. 10), donnée employeur
 *       utilisateur hors pipeline salarié principal.</li>
 *   <li>{@code ancienneteSectorielleJours} — ancienneté sectorielle
 *       cumulée chez le même utilisateur, donnée RH utilisateur
 *       hors pipeline.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code interim-be-indemnite-fin-mission-section-prefill-rules.spec.ts}.</p>
 */
export const InterimBeIndemniteFinMissionSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier mission (contrat de travail intérimaire, attestation
   * Dimona ETI, attestation FSI, relevés d'heures mission, CCT
   * sectorielle de l'utilisateur).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-05b — Helper partagé pour le pré-remplissage IA de
 * {@code OutplacementBeGeneral30semSectionComponent} (F-219, BE uniquement).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * antérieures (0 champ pré-rempli) — décision pilote pour les outils
 * BE-only V1 qui privilégie une saisie avocat assumée plutôt qu'une
 * dérivation IA incomplète. Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code licenciementEffectif} / {@code motifGrave} — qualification
 *       juridique fine de la rupture (le prompt Travail extrait
 *       {@code motifRupture} en texte libre, mais la transposition vers
 *       les booléens « licenciement effectif » et « motif grave au sens
 *       art. 35 Loi 03/07/1978 » nécessite la lecture par l'avocat de la
 *       lettre de rupture / C4 / éventuelle assignation).</li>
 *   <li>{@code semainesPreavisOuIndemnite} — durée du préavis notifié (ou
 *       indemnité de rupture équivalente). Le prompt actuel n'extrait pas
 *       directement ce nombre en semaines ; l'avocat le déduit du calcul
 *       Claeys (cf. {@code formule-claeys-be}) ou de la lettre de
 *       préavis.</li>
 *   <li>{@code dateFinContrat} — date à laquelle le contrat prend fin (fin
 *       de préavis presté ou date du congé pour indemnité). L'IA extrait
 *       {@code dateRuptureContrat} mais le sens diffère (rupture
 *       juridique ≠ fin du préavis presté). Saisie avocat évite tout
 *       décalage de 30 sem ± qui invaliderait la fenêtre 15 jours.</li>
 *   <li>{@code offreNotifiee} / {@code dateNotificationOffre} — éléments
 *       déclaratifs du dossier (offre employeur reçue par recommandé /
 *       courriel) absents du prompt IA actuel.</li>
 *   <li>{@code heuresProgramme} / {@code dureeProgrammeMois} — clauses
 *       contractuelles de l'offre d'outplacement (prestataire externe :
 *       SBS Skills, Hudson, LEE Hecht Harrison, Right Management…) — non
 *       extractibles depuis les pièces génériques.</li>
 *   <li>{@code offreEcrite} / {@code offreIndividuelle} /
 *       {@code contenuMinimumRespecte} — qualifications juridiques
 *       formelles relevant exclusivement de l'analyse avocat sur la
 *       lettre d'offre et l'annexe « programme ».</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code outplacement-be-general-30sem-section-prefill-rules.spec.ts}.</p>
 */
export const OutplacementBeGeneral30semSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (lettre de rupture, C4, lettre d'offre
   * outplacement, programme prestataire).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

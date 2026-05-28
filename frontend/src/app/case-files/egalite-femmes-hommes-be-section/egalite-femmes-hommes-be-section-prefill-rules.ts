import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-22b — Helper partagé pour le pré-remplissage IA de
 * {@code EgaliteFemmesHommesBeSectionComponent} (F-219, BE uniquement —
 * Loi du 22/04/2012 visant à lutter contre l'écart salarial entre
 * hommes et femmes + AR du 17/08/2013 + AR du 25/04/2014 + CCT n° 25
 * + C. pén. social art. 195/1).
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
 *   <li>{@code effectifEntreprise} — effectif moyen ETP au sens de la
 *       Loi 04/12/2007 art. 7 (équivalents temps plein, mois incomplets,
 *       intérimaires, sociétés liées) : donnée RH employeur non
 *       extractible du dossier salarié individuel.</li>
 *   <li>{@code statutRapport} — statut administratif du rapport biennal
 *       (déposé complet / déposé incomplet / en préparation / non déposé
 *       / indéterminé) : donnée procédurale interne à l'entreprise
 *       (dépôt CE, dépôt EBES BNB), hors pipeline IA.</li>
 *   <li>{@code dateDepotRapport} — date de dépôt formel au Conseil
 *       d'entreprise ou délégation syndicale, déclaratif employeur.</li>
 *   <li>Ventilations 1 à 5 ({@code ventilationNiveauFonctionFournie},
 *       {@code ventilationAncienneteFournie},
 *       {@code ventilationQualificationFournie},
 *       {@code ventilationRegimeTravailFournie},
 *       {@code ventilationComposantsRemunerationFournie}) — flags de
 *       contenu art. 4 AR 17/08/2013 portant sur la structure du rapport
 *       déposé. Non extractibles d'un dossier salarié individuel
 *       (nécessitent l'accès au rapport biennal lui-même).</li>
 *   <li>{@code ecartSalarialNonJustifieConstate} — résultat de
 *       l'analyse interne art. 5 Loi 22/04/2012, déclaratif employeur
 *       ou avocat post-instruction.</li>
 *   <li>{@code pourcentageEcartConstate} — magnitude indicative de
 *       l'écart, donnée chiffrée du rapport, hors pipeline.</li>
 *   <li>{@code planActionEtabli} — flag procédural sur l'existence
 *       d'un plan d'action concret avec objectifs chiffrés et
 *       calendrier (art. 5 Loi 22/04/2012 + CCT sectorielle ou 6 mois).</li>
 *   <li>{@code mediateurDesigne} — faculté de désignation d'un
 *       médiateur égalité H/F (art. 14 Loi 22/04/2012 + CCT n° 25),
 *       indicateur de bonne pratique non extractible.</li>
 *   <li>{@code plainteIefhOuInspectionEnCours} — indicateur d'un
 *       contentieux en cours (Institut pour l'égalité des femmes et
 *       des hommes / SPF Emploi inspection), donnée procédurale
 *       externe au dossier salarié principal.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code egalite-femmes-hommes-be-section-prefill-rules.spec.ts}.</p>
 */
export const EgaliteFemmesHommesBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * rapport biennal d'analyse égalité salariale H/F déposé au CE / DS
   * (effectif moyen ETP, statut du rapport, ventilations, plan
   * d'action, médiateur, plainte IEFH).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

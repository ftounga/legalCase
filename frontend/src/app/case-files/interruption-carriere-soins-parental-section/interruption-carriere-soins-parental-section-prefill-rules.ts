import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-32b — Helper partage pour le pre-remplissage IA de
 * {@code InterruptionCarriereSoinsParentalSectionComponent} (F-219,
 * BE uniquement — Loi du 22/01/1985 art. 99 a 107quater + AR du
 * 29/10/1997 + CCT n 64).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MEME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pre-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * anterieures (0 champ pre-rempli). Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code forme} — choix tactique avocat entre TEMPS_PLEIN 4
 *       mois, MI_TEMPS 8 mois, CINQUIEME_TEMPS 20 mois (AR 29/10/1997
 *       art. 3 modifie AR 12/12/2001). Decision strategique du
 *       travailleur en accord avec son projet familial, non
 *       deductible automatiquement du dossier salarie.</li>
 *   <li>{@code ancienneteMois} — anciennete chez le meme employeur en
 *       mois. Le pipeline IA Travail BE extrait souvent
 *       {@code dateEntree} ou {@code anneesCarriereSalarie}, mais la
 *       periode de reference du conge parental est specifique
 *       (15 mois precedant la demande, art. 5 AR 29/10/1997) et le
 *       calcul exige une date de demande non disponible V1. Saisie
 *       avocat requise.</li>
 *   <li>{@code ageEnfantMois} — age de l'enfant en mois a la date du
 *       debut de l'interruption (art. 4 AR 29/10/1997). Donnee
 *       relative a l'enfant (date de naissance enfant), non
 *       extractible mecaniquement du dossier salarie generique
 *       (pipeline IA centre sur le travailleur, pas sur sa cellule
 *       familiale).</li>
 *   <li>{@code enfantHandicap} — drapeau handicap &gt;= 66 pour cent
 *       qui porte le plafond a 21 ans (art. 4 al. 2 AR 29/10/1997).
 *       Donnee medico-administrative externe (attestation Vierge
 *       Noire / DG Personnes handicapees), non disponible dans le
 *       dossier salarie.</li>
 *   <li>{@code soldeRestantMoisEtp} — solde individuel restant du
 *       droit 4 mois ETP par enfant et par parent (art. 2 AR
 *       29/10/1997). Donnee gerée par ONEM (compteur individuel
 *       enfant / parent), non extractible du dossier salarie.</li>
 *   <li>{@code modeNotification} — choix tactique avocat entre lettre
 *       recommandee, remise en main propre ou autre (art. 6 AR
 *       29/10/1997). Decision strategique non deductible.</li>
 *   <li>{@code employeurAccepte} — accord employeur sur la forme
 *       demandee. Information procedurale externe (reponse employeur
 *       a la notification), non extractible.</li>
 *   <li>{@code employeurAdiffere} — drapeau differe motive employeur
 *       maximum 6 mois (art. 7 AR 29/10/1997). Information
 *       procedurale specifique (notification employeur), non
 *       extractible.</li>
 *   <li>{@code cumulAllocationOnemDemande} — choix tactique avocat de
 *       cumuler l'allocation forfaitaire ONEM (AR 12/08/1991 art. 4).
 *       Decision strategique non deductible.</li>
 *   <li>{@code dateDebutInterruption} — date prospective choisie par
 *       le travailleur. Non extractible du dossier salarie.</li>
 *   <li>{@code dateNotification} — date de notification effective a
 *       l'employeur. Information procedurale specifique, non
 *       extractible.</li>
 *   <li>{@code remunerationMensuelleBrute} — base de calcul de
 *       l'indemnite de protection 6 mois art. 101 Loi 22/01/1985.
 *       Bien que le dossier salarie expose
 *       {@code salaireBrutMensuel}, la base de protection conge
 *       parental est juridiquement encadree (12 derniers mois precedant
 *       la demande). Saisie avocat V1 pour eviter un calcul errone par
 *       assimilation mecanique.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symetrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parite F-236</b> : count = 0 quel que soit l'input —
 * teste par
 * {@code interruption-carriere-soins-parental-section-prefill-rules.spec.ts}.</p>
 */
export const InterruptionCarriereSoinsParentalSectionPrefillRules = {

  /**
   * Comptage parite F-236 — V1 : aucun champ pre-rempli implique 0.
   *
   * <p>Le param {@code input} est conserve pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilise en V1.
   * Sans pre-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier ONEM (formulaire C61 / C62, attestation employeur),
   * l'acte de naissance de l'enfant, l'eventuelle attestation
   * handicap (Vierge Noire / DG Personnes handicapees), la
   * notification employeur (recommandee ou main propre) et le
   * dossier de remuneration art. 101 Loi 22/01/1985.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

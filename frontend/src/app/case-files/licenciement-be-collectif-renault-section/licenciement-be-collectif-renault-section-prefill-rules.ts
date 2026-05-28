import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-07b — Helper partagé pour le pré-remplissage IA de
 * {@code LicenciementBeCollectifRenaultSectionComponent} (F-219, BE
 * uniquement — Loi du 13/02/1998 « loi Renault » + CCT n° 24 + CCT n° 39 +
 * Directive 98/59/CE).
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
 *   <li>{@code dateProjet} — date à laquelle l'employeur a annoncé son
 *       intention de licenciement collectif (point de départ fenêtre 60 j).
 *       Non extractible depuis le dossier salarié (information employeur
 *       interne) ; saisie avocat depuis le procès-verbal CE / communication
 *       interne.</li>
 *   <li>{@code tailleEntreprise} — tranche 4 cas (PME &lt; 20 / 20-99 /
 *       100-299 / ≥ 300) — qualification employeur fondée sur l'effectif
 *       moyen 4 trimestres, non extractible du dossier salarié individuel.</li>
 *   <li>{@code effectifMoyen} — donnée employeur (DmfA ONSS), non
 *       extractible du dossier salarié.</li>
 *   <li>{@code nombreLicenciementsEnvisages} — donnée projet employeur,
 *       confidentielle, saisie avocat sur la base du dossier procédural
 *       collectif.</li>
 *   <li>{@code phaseAtteinte} — qualification procédurale (4 cas) — relève
 *       du jugement avocat depuis le calendrier procédural employeur.</li>
 *   <li>{@code informationEcriteCePrealable} /
 *       {@code documentsLegauxCommuniques} /
 *       {@code consultationEffectiveTenue} /
 *       {@code reponsesMotiveesEmployeur} /
 *       {@code notificationAutoriteRegionaleFaite} — checklist procédurale
 *       booléenne fondée sur les actes de procédure (procès-verbaux CE,
 *       courriers employeur, accusé de réception Forem/Actiris/VDAB) —
 *       saisie avocat systématique.</li>
 *   <li>{@code dateNotificationAutorite} — date de la notification écrite
 *       à l'autorité régionale (Forem / Actiris / VDAB), document employeur
 *       non extractible du dossier salarié.</li>
 *   <li>{@code datePremierPreavisNotifie} — la pipeline IA Travail BE
 *       extrait certes {@code dateRuptureContrat}, mais cette date n'est
 *       pas équivalente à la date de notification du préavis collectif :
 *       la rupture peut intervenir mois plus tard. Saisie avocat depuis
 *       la lettre de préavis individuelle pour éviter tout faux positif
 *       de non-respect du délai 30 j.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code licenciement-be-collectif-renault-section-prefill-rules.spec.ts}.</p>
 */
export const LicenciementBeCollectifRenaultSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier procédural (procès-verbal CE, courriers
   * employeur, accusé de réception Forem/Actiris/VDAB, lettre de
   * préavis individuelle).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

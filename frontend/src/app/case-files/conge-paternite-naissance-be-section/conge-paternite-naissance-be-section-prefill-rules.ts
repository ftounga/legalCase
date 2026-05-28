import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-31b — Helper partage pour le pre-remplissage IA de
 * {@code CongePaterniteNaissanceBeSectionComponent} (F-219, BE
 * uniquement — Loi du 03/07/1978 art. 30 paragr. 2 + Loi du
 * 07/04/2023 + AR du 17/10/1994 + AR du 03/07/1996).
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
 *   <li>{@code statutTravailleur} — qualification SALARIE /
 *       INDEPENDANT / AGENT_PUBLIC_STATUTAIRE / AUTRE issue du contrat
 *       de travail et du cadre social — Loi 03/07/1978 art. 30
 *       paragr. 2 reserve aux salaries au contraire du regime INASTI
 *       AR 23/05/2019 (independants) et AR 19/11/1998 (statutaires).
 *       Non extractible mecaniquement du dossier salarie generique du
 *       pipeline Travail BE qui ne distingue pas explicitement les
 *       statuts.</li>
 *   <li>{@code lienFiliation} — qualification juridico-civile (pere
 *       biologique art. 315 C. civ., comaternite art. 325/2, co-parent
 *       art. 327/1, cohabitant legal Loi 07/04/2023). Donnee
 *       d'etat-civil externe au dossier salarie.</li>
 *   <li>{@code etapeProcedure} — etape declarative AVANT_NAISSANCE /
 *       NAISSANCE_SURVENUE / NOTIFICATION_EMPLOYEUR_FAITE /
 *       CONGE_EN_COURS_DE_PRISE / CONGE_PRIS_ENTIEREMENT /
 *       DELAI_QUATRE_MOIS_DEPASSE. Statut de la procedure RH non
 *       extractible.</li>
 *   <li>{@code filiationEtablie} — preuve juridique (acte de
 *       naissance, reconnaissance, comaternite, mariage / cohabitation
 *       legale art. 1475 et s. C. civ.). Donnee d'etat-civil externe.</li>
 *   <li>{@code contratTravailEnCours} — etat de la relation salariee
 *       a la date de la naissance. Donnee qui dependrait d'une
 *       comparaison entre dateRuptureContrat (eventuellement extrait
 *       par le pipeline) et dateNaissance (non extrait V1). Reste
 *       saisie avocat pour eviter une erreur silencieuse.</li>
 *   <li>{@code notificationEmployeurFaite} — formalite RH (AR
 *       17/10/1994) attestee par accuse de reception. Donnee
 *       procedurale externe au dossier salarie.</li>
 *   <li>{@code dateNaissance} — date de naissance de l'enfant (NB :
 *       distinct de la date de naissance du salarie qui pourrait etre
 *       extraite). Donnee familiale externe au dossier salarie.</li>
 *   <li>{@code dateNotificationEmployeur} — date d'envoi de l'avis
 *       AR 17/10/1994 a l'employeur. Donnee procedurale externe.</li>
 *   <li>{@code joursDejaPrisOuvrables} — nombre de jours deja
 *       consommes. Donnee de suivi RH non extractible.</li>
 *   <li>{@code dateFinPriseEffective} — date du dernier jour pris si
 *       conge termine. Donnee de suivi RH non extractible.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symetrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parite F-236</b> : count = 0 quel que soit l'input —
 * teste par {@code conge-paternite-naissance-be-section-prefill-rules.spec.ts}.</p>
 */
export const CongePaterniteNaissanceBeSectionPrefillRules = {

  /**
   * Comptage parite F-236 — V1 : aucun champ pre-rempli implique 0.
   *
   * <p>Le param {@code input} est conserve pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilise en V1.
   * Sans pre-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis
   * l'etat-civil de l'enfant, la decision RH d'octroi du conge et le
   * journal de prise effective des jours.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

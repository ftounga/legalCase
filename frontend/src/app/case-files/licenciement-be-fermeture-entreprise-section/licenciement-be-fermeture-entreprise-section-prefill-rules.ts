import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-06b — Helper partagé pour le pré-remplissage IA de
 * {@code LicenciementBeFermetureEntrepriseSectionComponent} (F-219, BE
 * uniquement — Loi 26/06/2002 + AR 23/03/2007 + CCT n° 9bis, Fonds de
 * Fermeture des Entreprises FFE).
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
 *   <li>{@code dateNaissance} — l'IA Travail BE extrait certes
 *       {@code dateNaissanceSalarie}, mais la transposition vers le calcul
 *       précis d'âge à la date de fermeture (pivot autour du seuil 45 ans
 *       du supplément) est sensible. Saisie avocat évite tout faux
 *       positif d'éligibilité au supplément.</li>
 *   <li>{@code dateDebutContrat} — non extrait par le prompt Travail BE
 *       actuel (seule la date de rupture est extraite). La date de début
 *       de contrat conditionne le seuil 1 an d'ancienneté FFE — saisie
 *       avocat systématique depuis le contrat de travail / attestation
 *       d'occupation.</li>
 *   <li>{@code dateFermeture} — date de cessation officielle / jugement
 *       de faillite, propre au dossier employeur (≠ date de rupture du
 *       contrat). Non extractible.</li>
 *   <li>{@code remunerationMensuelleBrute} — montant brut extrait par
 *       l'IA Travail BE mais base FFE pour reprise créances spécifique
 *       (plafonds légaux). Saisie avocat préférée V1 pour cohérence avec
 *       le reste des outils F-219.</li>
 *   <li>{@code typeFermeture} — qualification juridique 6 cas (cessation
 *       définitive / faillite / partielle / transfert / fusion /
 *       temporaire) — relève du jugement avocat sur la base des actes
 *       officiels (jugement d'ouverture, décision AG).</li>
 *   <li>{@code statutEmployeur} — qualification solvabilité 3 cas
 *       (solvable / insolvable avéré / faillite déclarée) — saisie
 *       avocat depuis les actes / déclaration ONEM-FFE.</li>
 *   <li>{@code effectifEtp} — donnée employeur (DmfA ONSS / déclaration
 *       sociale) non extractible du dossier salarié.</li>
 *   <li>{@code salairesImpayes} / {@code peculeVacancesImpaye} /
 *       {@code indemniteRuptureImpayee} — montants impayés à la date de
 *       fermeture, propres au dossier individuel — saisie avocat
 *       systématique depuis les bulletins de paie / attestation
 *       employeur.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code licenciement-be-fermeture-entreprise-section-prefill-rules.spec.ts}.</p>
 */
export const LicenciementBeFermetureEntrepriseSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (contrat de travail, jugement de faillite,
   * attestation employeur, déclaration ONEM-FFE).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

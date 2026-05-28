import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-29b — Helper partage pour le pre-remplissage IA de
 * {@code AtMpRenteCapitalBeSectionComponent} (F-219, BE uniquement —
 * Loi du 10/04/1971 art. 24 + Lois coordonnees 03/06/1970 art. 35 +
 * AR 21/12/1971 + AR 10/12/1987 + AR 24/02/2005).
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
 *   <li>{@code origine} — qualification juridico-medicale AT vs MP
 *       (art. 7-8 Loi 10/04/1971 vs Lois coordonnees 03/06/1970) que
 *       seul l'avocat tranche apres consultation du dossier medical et
 *       des rapports d'expertise. Non extractible mecaniquement du
 *       dossier salarie generique.</li>
 *   <li>{@code statutReconnaissance} — statut decisionnel Fedris /
 *       assureur-loi (RECONNU / CONTESTE / EN_COURS) issu de la
 *       decision administrative. Donnee externe au dossier salarie.</li>
 *   <li>{@code dateConsolidation} — date d'arret de l'incapacite
 *       temporaire et de fixation du taux IPP par l'expertise medicale
 *       Fedris ou assureur-loi (art. 24 paragr. 2 AR 03/07/1996).
 *       Donnee medicale externe.</li>
 *   <li>{@code tauxIpp} — taux d'incapacite permanente partielle
 *       arrete par expertise medicale specialiste. Non extractible
 *       d'un dossier salarie generique.</li>
 *   <li>{@code remunerationBaseAnnuelle} — remuneration de base
 *       annuelle art. 34 Loi 10/04/1971 plafonnee art. 39 (plafond
 *       ONSS — 49 853,86 EUR au 01/01/2024). Bien que le dossier
 *       salarie expose un salaire annuel brut, la remuneration de
 *       base AT/MP suit un cadre juridique specifique (12 mois
 *       precedents l'AT/MP, art. 34 paragr. 1, sans assimilation
 *       automatique au salaireBrutAnnuel pipeline IA). Saisie avocat
 *       requise V1 pour eviter une erreur silencieuse de plafonnement.</li>
 *   <li>{@code dateNaissance} — utilisee par le coefficient d'age du
 *       bareme AR 24/02/2005 (Table I-bis). Bien que le pipeline IA
 *       extrait souvent {@code dateNaissanceSalarie}, la donnee est
 *       sensible : le coefficient d'age affecte le capital de
 *       capitalisation et toute erreur impacte directement le
 *       montant. Saisie avocat V1, conforme au pattern uniforme F-219
 *       (0 prefill).</li>
 *   <li>{@code demandeConversionPartielle} — choix strategique de
 *       l'avocat (1/3 max de la rente en capital art. 45ter Loi
 *       10/04/1971 + AR 10/12/1987, possible apres 3 ans). Decision
 *       avocat, non extractible.</li>
 *   <li>{@code dateDemandeConversion} — date prospective de la demande
 *       de conversion choisie par l'avocat. Decision avocat.</li>
 *   <li>{@code ageConsolidationOverride} — mode test / scenario
 *       avance. Saisie ad-hoc avocat.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symetrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parite F-236</b> : count = 0 quel que soit l'input —
 * teste par {@code at-mp-rente-capital-be-section-prefill-rules.spec.ts}.</p>
 */
export const AtMpRenteCapitalBeSectionPrefillRules = {

  /**
   * Comptage parite F-236 — V1 : aucun champ pre-rempli implique 0.
   *
   * <p>Le param {@code input} est conserve pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilise en V1.
   * Sans pre-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis la
   * decision Fedris / assureur-loi, le rapport d'expertise medicale et
   * la fiche de remuneration de base art. 34 Loi 10/04/1971.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-21b — Helper partagé pour le pré-remplissage IA de
 * {@code EcoChequesChequesRepasBeSectionComponent} (F-219, BE
 * uniquement — CCT n°98 du CNT du 20/02/2009 pour les éco-chèques +
 * Loi du 25/04/2014 portant des dispositions diverses + AR du
 * 03/02/2010 modifiant l'art. 19bis de l'AR du 28/11/1969 portant
 * exécution de la loi du 27/06/1969 sur la sécurité sociale des
 * travailleurs).
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
 *   <li>{@code typeAvantage} — qualification administrative (éco-chèques
 *       vs chèques-repas vs indéterminé) propre au régime des avantages
 *       extra-légaux belges (CCT n°98 vs Loi 25/04/2014) ; le pipeline
 *       IA Travail BE n'extrait pas mécaniquement la nature précise de
 *       l'avantage litigieux.</li>
 *   <li>{@code montantAnnuelEur} — montant cumulé sur l'année civile,
 *       déclaratif RH / fiches de paie, non extractible du dossier
 *       salarié principal.</li>
 *   <li>{@code valeurFacialeUnitaireEur} — valeur faciale par chèque
 *       (max 8 EUR pour chèques-repas, art. 19bis §2 8° AR 28/11/1969),
 *       déclaratif fiches de paie / extrait CCT, hors pipeline.</li>
 *   <li>{@code contributionTravailleurUnitaireEur} — contribution
 *       travailleur (min 1,09 EUR pour chèques-repas, art. 19bis §2 7°
 *       AR 28/11/1969), déclaratif fiches de paie / convention.</li>
 *   <li>{@code joursEffectivementPrestes} — historique RH employeur
 *       (compteur jours prestés sur l'année), non extractible du dossier
 *       salarié.</li>
 *   <li>{@code cctSectorielleOuEntrepriseExiste} — flag juridique
 *       (existence d'une CCT sectorielle ou d'entreprise, art. 3
 *       CCT n°98 / art. 19bis §2 1° AR 28/11/1969), déclaratif avocat
 *       après vérification du règlement de travail / convention
 *       collective applicable.</li>
 *   <li>{@code conventionIndividuelleEcrite} — flag juridique
 *       (existence d'une convention individuelle écrite à défaut de
 *       CCT), déclaratif avocat après vérification du contrat de
 *       travail.</li>
 *   <li>{@code paiementElectronique} — flag opérationnel (paiement
 *       électronique obligatoire chèques-repas depuis 01/01/2016,
 *       Loi 25/04/2014 art. 51), déclaratif fiches de paie.</li>
 *   <li>{@code cumulFraisBouche} — flag interdiction de cumul avec
 *       indemnité forfaitaire frais de bouche (art. 19bis §2 4° al. 4
 *       AR 28/11/1969), déclaratif avocat.</li>
 *   <li>{@code substitutionRemuneration} — flag substitution à une
 *       rémunération préexistante (art. 4 CCT n°98 + jurisprudence
 *       Cass. 16/10/2017 S.16.0042.F), déclaratif avocat après analyse
 *       de l'historique salarial.</li>
 *   <li>{@code dateAttribution} — date d'attribution servant au contrôle
 *       de validité (24 mois éco-chèques art. 13 CCT n°98 ; 12 mois
 *       chèques-repas art. 19bis §2 5° AR 28/11/1969), déclaratif
 *       fiches de paie.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code eco-cheques-cheques-repas-be-section-prefill-rules.spec.ts}.</p>
 */
export const EcoChequesChequesRepasBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * fiches de paie / extrait CCT / convention individuelle écrite /
   * historique salarial.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

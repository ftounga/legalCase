import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-23b — Helper partagé pour le pré-remplissage IA de
 * {@code DiscriminationBeHandicapAmenagementSectionComponent} (F-219,
 * BE uniquement — Loi du 10/05/2007 tendant à lutter contre certaines
 * formes de discrimination + CCT n° 95 du 10/10/2008 du CNT — NAR +
 * Directive 2000/78/CE art. 5 + Convention ONU 13/12/2006 art. 5 et 27).
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
 *   <li>{@code statutHandicap} — statut de reconnaissance (AVIQ / VAPH /
 *       PHARE / DGPH / SEPP / mutuelle / factuel / contesté / indéterminé),
 *       donnée RH médicale / administrative non extractible du dossier
 *       salarié principal — déclarative employeur ou avocat.</li>
 *   <li>{@code dateDemandeAmenagement} — date de la demande écrite
 *       travailleur ou suggestion SEPP, donnée procédurale interne au
 *       processus aménagement raisonnable.</li>
 *   <li>{@code typeAmenagementDemande} — texte libre court décrivant
 *       l'aménagement (poste / horaires / télétravail / équipement /
 *       réaffectation / formation / autre), narratif déclaratif.</li>
 *   <li>{@code coutEstimeAmenagement} — coût en euros apprécié APRÈS
 *       subsides mobilisables, chiffre déclaratif employeur / devis.</li>
 *   <li>{@code subsidesDemandes} — flag procédural sur la mobilisation
 *       AVIQ / VDAB / PHARE / Actiris / Bruxelles Formation, déclaratif.</li>
 *   <li>{@code effectifEntreprise} — proportionnalité art. 14 al. 2
 *       Loi 10/05/2007, donnée RH non extractible du dossier individuel.</li>
 *   <li>{@code chiffreAffairesAnnuel} — élément de proportionnalité,
 *       donnée comptable externe au dossier salarié.</li>
 *   <li>{@code reponseEmployeur} — nature de la réponse (accordé /
 *       contre-proposition / refusé motivé / refusé non motivé / silence /
 *       en attente / indéterminé), déclarative employeur.</li>
 *   <li>{@code motivationDetailleeFournie} — flag procédural art. 28 § 3
 *       (présence d'une motivation écrite détaillée), non extractible.</li>
 *   <li>{@code avisSeppFavorable} — avis médecin du travail SEPP, donnée
 *       médicale déclarative (preuve majeure).</li>
 *   <li>{@code chargeDisproportionneeInvoquee} — moyen de défense
 *       employeur, déclaratif.</li>
 *   <li>{@code devisExterneFourni} — flag preuve documentaire,
 *       déclaratif.</li>
 *   <li>{@code mesuresAlternativesProposees} — flag procédural,
 *       déclaratif.</li>
 *   <li>{@code sanctionSubie} — qualification de la sanction post-demande
 *       (licenciement / régression / harcèlement / aucune), aiguille la
 *       présomption représailles art. 17, déclarative.</li>
 *   <li>{@code dateSanction} — chronologie post-demande, donnée
 *       procédurale interne.</li>
 *   <li>{@code salaireMensuelBrut} — assiette indemnité forfaitaire 6
 *       mois art. 28 § 2 1°, donnée RH déclarative employeur.</li>
 *   <li>{@code procedureUniaSaisie} — flag procédural (Centre
 *       interfédéral pour l'égalité des chances UNIA), déclaratif.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code discrimination-be-handicap-amenagement-section-prefill-rules.spec.ts}.</p>
 */
export const DiscriminationBeHandicapAmenagementSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier RH médical (statut handicap, demande écrite, avis SEPP,
   * réponse employeur, motivation, devis, alternatives, sanction).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-28b — Helper partage pour le pre-remplissage IA de
 * {@code MpFedrisReconnaissanceSectionComponent} (F-219, BE
 * uniquement — Lois coordonnees du 03/06/1970 + AR du 28/03/1969
 * liste fermee modifie 06/12/2018 + AR du 16/12/1985 systeme ouvert).
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
 *   <li>{@code typeMaladie} — qualification juridico-medicale (liste
 *       fermee AR 28/03/1969 vs systeme ouvert art. 30bis) que seul
 *       l'avocat tranche apres consultation de la liste, de la
 *       jurisprudence Cass. BE 28/05/2008 S.07.0033.F et apres analyse
 *       du rapport medical specialiste. Non extractible mecaniquement.</li>
 *   <li>{@code codeMaladieListe} — code AR 28/03/1969 (sections 1.x a
 *       6.x, ex. « 1.404.01 silicose », « 2.3.1 amiante mesotheliome »,
 *       « 5.01.01 affection lombaire chronique »), exige consultation de
 *       la nomenclature et appariement par un specialiste en sante
 *       au travail.</li>
 *   <li>{@code libelleMaladie} — nom medical de la pathologie issue
 *       du dossier medical victime / rapport specialiste / certificat
 *       Fedris. Champ medical, non extractible du dossier salarie
 *       generique du pipeline Travail BE.</li>
 *   <li>{@code dateConstatationMedicale} — date de premiere
 *       constatation medicale (rapport specialiste, certificat medical
 *       initial Fedris formulaire 501). Donnee medicale externe au
 *       dossier salarie.</li>
 *   <li>{@code dateConnaissanceProfessionnel} — date d'information de
 *       la victime sur le caractere professionnel (par un medecin du
 *       travail, par Fedris, par une expertise). Point de depart du
 *       delai de prescription triennal art. 31 lois coordonnees
 *       03/06/1970 — exige une instruction medicale et juridique
 *       specifique non couverte par l'extraction Travail BE V1.</li>
 *   <li>{@code dateDeclarationEnvisagee} — date prospective de
 *       declaration Fedris choisie par l'avocat selon la strategie
 *       (immediate art. 24 lois coordonnees pour interrompre la
 *       prescription, ou differee selon l'instruction du dossier
 *       medical). Decision avocat, non extractible.</li>
 *   <li>{@code exposition} — caracterisation in concreto de
 *       l'exposition professionnelle au risque correspondant a la
 *       section concernee de la liste fermee (CV professionnel, fiches
 *       de poste, mesures d'ambiance bruit / poussieres / vibrations,
 *       registres d'exposition amiante art. 23 AR 16/03/2006,
 *       attestations employeurs successifs). Appreciation avocat /
 *       medecin du travail INAMI.</li>
 *   <li>{@code causalite} — caracterisation du lien causal direct et
 *       determinant en systeme ouvert (Cass. BE 28/05/2008 S.07.0033.F
 *       — simple facteur favorisant insuffisant ; Cass. BE 04/02/2002
 *       S.01.0095.F — caractere direct et essentiel exige).
 *       Appreciation medicale specialiste contradictoire + analyse
 *       juridique avocat.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symetrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parite F-236</b> : count = 0 quel que soit l'input —
 * teste par {@code mp-fedris-reconnaissance-section-prefill-rules.spec.ts}.</p>
 */
export const MpFedrisReconnaissanceSectionPrefillRules = {

  /**
   * Comptage parite F-236 — V1 : aucun champ pre-rempli implique 0.
   *
   * <p>Le param {@code input} est conserve pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilise en V1.
   * Sans pre-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * rapport medical specialiste, le CV professionnel, le carnet
   * medical de surveillance sante art. 27 AR 28/05/2003, et la
   * nomenclature AR 28/03/1969.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

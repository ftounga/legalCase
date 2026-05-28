import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-19b — Helper partagé pour le pré-remplissage IA de
 * {@code DroitDeconnexionBeSectionComponent} (F-219, BE uniquement —
 * Loi du 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022, art. 16
 * + AR du 19/02/2023 + CCT n° 149).
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
 *   <li>{@code effectifEntreprise} — effectif total travailleurs
 *       (seuil 20 art. 16 § 1), donnée RH employeur non extraite
 *       du dossier salarié principal.</li>
 *   <li>{@code statutAccord} — qualification administrative du
 *       statut de l'accord (CCT déposée / règlement modifié /
 *       négociation / aucune initiative / indéterminé), déclaratif
 *       avocat sur la base d'un certificat de dépôt SPF Emploi ou
 *       d'un PV de CE/CPPT/DS, hors pipeline.</li>
 *   <li>{@code dateEntreeVigueurInstrument} — date de dépôt CCT ou
 *       de publication du règlement de travail modifié, donnée
 *       employeur non extraite.</li>
 *   <li>{@code modalitesPratiquesDeconnexionDefinies} /
 *       {@code sensibilisationFormationPrevue} /
 *       {@code modalitesOrganisationTravailDefinies} — flags de
 *       contenu de l'instrument (3 thèmes art. 16 § 2), déclaratifs
 *       avocat après lecture du texte de la CCT ou du règlement.</li>
 *   <li>{@code consultationOrganeConcertationEffectuee} — flag
 *       procédural (consultation CE / CPPT / DS), déclaratif avocat
 *       sur la base d'un PV.</li>
 *   <li>{@code manquementSignaleCbeOuSpf} — indicateur d'un
 *       contentieux en cours (action en cessation, contrôle SPF
 *       Emploi), déclaratif avocat.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code droit-deconnexion-be-section-prefill-rules.spec.ts}.</p>
 */
export const DroitDeconnexionBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier RH employeur (effectif, certificat de dépôt CCT au SPF
   * Emploi, texte du règlement de travail modifié, éventuel PV de
   * consultation organe concertation, éventuel courrier SPF).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

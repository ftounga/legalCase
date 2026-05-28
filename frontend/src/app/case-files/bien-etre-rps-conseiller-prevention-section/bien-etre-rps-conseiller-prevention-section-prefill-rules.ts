import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-30b — Helper partage pour le pre-remplissage IA de
 * {@code BienEtreRpsConseillerPreventionSectionComponent} (F-219, BE
 * uniquement — Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014).
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
 *   <li>{@code typeRisque} — qualification juridico-medicale du risque
 *       psychosocial (harcelement moral art. 32bis, harcelement sexuel
 *       art. 32bis al. 3, violence au travail, autres RPS stress /
 *       burn-out / conflits art. 32/1 § 2). Releve d'une appreciation
 *       avocat apres recueil des faits et du contexte. Non extractible
 *       mecaniquement du dossier salarie generique.</li>
 *   <li>{@code modeDemande} — choix tactique avocat : demande informelle
 *       (mediation interne confidentielle, art. 14 a 16), formelle
 *       individuelle (enquete ecrite avec rapport employeur, art. 16
 *       a 22) ou formelle collective (analyse organisationnelle, art.
 *       16 § 2). Decision strategique non deductible automatiquement.</li>
 *   <li>{@code etapeProcedure} — etat d'avancement de la procedure
 *       interne (intention / entretien prealable / demande informelle
 *       en cours / demande formelle deposee / avis rendu / mesures
 *       employeur). Releve du dossier procedural specifique CPAP, non
 *       extractible du dossier salarie generique du pipeline Travail BE.</li>
 *   <li>{@code conseillerIdentifie} — un CPAP interne ou externe a-t-il
 *       ete designe par l'employeur (art. 32sexies § 2). Information
 *       organisationnelle interne a l'entreprise (registre du personnel,
 *       reglement de travail, contrat SEPP), non extractible
 *       automatiquement.</li>
 *   <li>{@code entretienPrealableRealise} — formalite procedurale
 *       prealable obligatoire avant demande formelle (art. 16 § 1 AR
 *       10/04/2014, delai 10 jours). Documentee par compte-rendu
 *       entretien CPAP / mention dans le formulaire de demande, non
 *       extractible du dossier salarie.</li>
 *   <li>{@code demandeEcriteSignee} — formalisme de la demande formelle
 *       (art. 16 § 2 AR 10/04/2014). Information procedurale specifique
 *       releve du dossier CPAP.</li>
 *   <li>{@code accuseReceptionRecu} — formalite CPAP post-depot (art.
 *       16 § 4 AR 10/04/2014). Information procedurale specifique au
 *       dossier CPAP, non extractible.</li>
 *   <li>{@code notificationEmployeurFaite} — formalite CPAP post-
 *       acceptation (art. 17 § 1 AR 10/04/2014). Information
 *       procedurale specifique CPAP, non extractible.</li>
 *   <li>{@code dateDepotDemande} — date precise du depot de la demande
 *       d'intervention psychosociale (point de depart du calcul du
 *       delai 3 mois enquete art. 22 § 1 et du delai 2 mois mesures
 *       employeur art. 32). Information procedurale specifique CPAP,
 *       non extractible du dossier salarie generique.</li>
 *   <li>{@code dateAvisRendu} — date de transmission du rapport motive
 *       par le CPAP a l'employeur et au travailleur. Information
 *       procedurale specifique CPAP, non extractible.</li>
 *   <li>{@code joursDepuisDepot} — calcule en aval cote frontend a
 *       partir de {@code dateDepotDemande}, non pre-fillable.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symetrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parite F-236</b> : count = 0 quel que soit l'input —
 * teste par {@code bien-etre-rps-conseiller-prevention-section-prefill-rules.spec.ts}.</p>
 */
export const BienEtreRpsConseillerPreventionSectionPrefillRules = {

  /**
   * Comptage parite F-236 — V1 : aucun champ pre-rempli implique 0.
   *
   * <p>Le param {@code input} est conserve pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilise en V1.
   * Sans pre-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier procedural CPAP (compte-rendu entretien prealable,
   * formulaire de demande d'intervention psychosociale, accuse de
   * reception CPAP, notification CPAP a l'employeur, registre
   * d'analyses des risques psychosociaux art. 6 AR 10/04/2014).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;

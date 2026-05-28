/**
 * SF-219-23b : modèle frontend de l'outil F-219 — refus d'aménagements
 * raisonnables handicap BE (Loi du 10/05/2007 tendant à lutter contre
 * certaines formes de discrimination M.B. 30/05/2007 + CCT n° 95 du
 * 10/10/2008 du CNT — NAR + Directive 2000/78/CE art. 5 + Convention
 * ONU 13/12/2006 art. 5 et 27).
 *
 * <p><b>BE uniquement</b> — le régime français d'OETH (Code du travail
 * art. L. 5213-1 et s. + L. 5213-6 obligation d'aménagement raisonnable
 * + L. 1132-1 prohibition de la discrimination + art. R. 5213-32 et s.)
 * constitue un dispositif juridiquement distinct (taux d'emploi 6 %,
 * contribution AGEFIPH, indemnités spécifiques L. 1226-14 inaptitude
 * d'origine professionnelle). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-23 backend
 * ({@code DiscriminationBeHandicapAmenagementRequest} +
 * {@code DiscriminationBeHandicapAmenagementResponse}).</p>
 */

/**
 * Statut de reconnaissance du handicap — qualification préalable à
 * l'analyse de l'obligation d'aménagement (Loi 10/05/2007 art. 4 4°
 * + jurisprudence CJUE 11/04/2013 C-335/11 et C-337/11 HK Danmark
 * / Ring et Werge — notion fonctionnelle de handicap).
 */
export type DiscriminationBeHandicapAmenagementStatutHandicap =
  | 'RECONNU_OFFICIEL'
  | 'MEDECIN_TRAVAIL_AVIS'
  | 'INVALIDITE_MUTUELLE'
  | 'DURABLE_FACTUEL'
  | 'CONTESTE'
  | 'INDETERMINE';

/**
 * Nature de la réponse de l'employeur à la demande d'aménagement
 * raisonnable (art. 14 Loi 10/05/2007 + CCT n° 95).
 */
export type DiscriminationBeHandicapAmenagementReponseEmployeur =
  | 'ACCORDE'
  | 'CONTRE_PROPOSITION'
  | 'REFUSE_MOTIVE_DETAILLE'
  | 'REFUSE_NON_MOTIVE'
  | 'NON_REPONDU_RAISONNABLE'
  | 'EN_ATTENTE'
  | 'INDETERMINE';

/**
 * Sanction éventuellement subie post-demande — déclenche la
 * présomption représailles art. 17 Loi 10/05/2007 lorsqu'elle est
 * autre que {@code AUCUNE}.
 */
export type DiscriminationBeHandicapAmenagementSanctionSubie =
  | 'LICENCIEMENT'
  | 'REGRESSION_CONDITIONS_TRAVAIL'
  | 'HARCELEMENT_REPRESAILLE'
  | 'AUCUNE';

/**
 * Verdict hiérarchisé 7 états — ordre de priorité (court-circuits
 * backend {@code DiscriminationBeHandicapAmenagementChecker}) :
 *
 * <ol>
 *   <li>{@code A_ANALYSER} — configuration ambigüe / procédure en cours.</li>
 *   <li>{@code FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE} — statut
 *       handicap contestable (préalable à trancher).</li>
 *   <li>{@code REPRESAILLES_PRESUMEES_LICENCIEMENT} — sanction après
 *       demande, présomption art. 17.</li>
 *   <li>{@code DISCRIMINATION_PRESUMEE_NON_MOTIVATION} — refus sans
 *       motivation écrite détaillée.</li>
 *   <li>{@code DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE} — refus
 *       motivé mais charge disproportionnée non démontrée.</li>
 *   <li>{@code CONFORME_AMENAGEMENT_ACCORDE} — aménagement accordé /
 *       contre-proposition acceptable.</li>
 *   <li>{@code CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE} — refus
 *       fondé sur charge disproportionnée objectivement démontrée.</li>
 * </ol>
 */
export type DiscriminationBeHandicapAmenagementVerdict =
  | 'A_ANALYSER'
  | 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE'
  | 'REPRESAILLES_PRESUMEES_LICENCIEMENT'
  | 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION'
  | 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE'
  | 'CONFORME_AMENAGEMENT_ACCORDE'
  | 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/discrimination-be-handicap-amenagement}.
 *
 * <p>Tous les champs marqués {@code @NotNull} backend sont requis ;
 * les autres (dates / champs monétaires / texte libre) sont optionnels.</p>
 */
export interface DiscriminationBeHandicapAmenagementRequest {
  /** Statut handicap (Loi 10/05/2007 art. 4 4° + CJUE Ring/Werge). */
  statutHandicap: DiscriminationBeHandicapAmenagementStatutHandicap;
  /** Date demande aménagement (facultatif). */
  dateDemandeAmenagement: string | null;
  /** Type d'aménagement demandé (texte libre court, max 500 car., facultatif). */
  typeAmenagementDemande: string | null;
  /** Coût estimé aménagement en euros (apprécié APRÈS subsides — facultatif). */
  coutEstimeAmenagement: number | null;
  /** Subsides demandés (AVIQ / VDAB / PHARE / Actiris / Bruxelles Formation). */
  subsidesDemandes: boolean;
  /** Effectif entreprise — proportionnalité art. 14 al. 2 Loi 10/05/2007. */
  effectifEntreprise: number;
  /** Chiffre d'affaires annuel (facultatif, élément de proportionnalité). */
  chiffreAffairesAnnuel: number | null;
  /** Nature de la réponse employeur. */
  reponseEmployeur: DiscriminationBeHandicapAmenagementReponseEmployeur;
  /** Motivation détaillée fournie par l'employeur (art. 28 § 3). */
  motivationDetailleeFournie: boolean;
  /** Avis SEPP médecin du travail favorable à l'aménagement. */
  avisSeppFavorable: boolean;
  /** Charge disproportionnée invoquée par l'employeur. */
  chargeDisproportionneeInvoquee: boolean;
  /** Devis externe à l'appui (devis professionnel produit par l'employeur). */
  devisExterneFourni: boolean;
  /** Mesures alternatives raisonnables proposées par l'employeur. */
  mesuresAlternativesProposees: boolean;
  /** Sanction subie après demande — déclenche présomption représailles. */
  sanctionSubie: DiscriminationBeHandicapAmenagementSanctionSubie;
  /** Date sanction subie (facultatif — chronologie post-demande). */
  dateSanction: string | null;
  /** Salaire mensuel brut en euros (indemnité forfaitaire 6 mois indicative). */
  salaireMensuelBrut: number | null;
  /** Procédure UNIA saisie (Centre interfédéral pour l'égalité des chances). */
  procedureUniaSaisie: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs
 * calculés + verdict + métadonnées légales.
 */
export interface DiscriminationBeHandicapAmenagementResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  statutHandicap: DiscriminationBeHandicapAmenagementStatutHandicap;
  dateDemandeAmenagement: string | null;
  typeAmenagementDemande: string | null;
  coutEstimeAmenagement: number | null;
  subsidesDemandes: boolean;
  effectifEntreprise: number;
  chiffreAffairesAnnuel: number | null;
  reponseEmployeur: DiscriminationBeHandicapAmenagementReponseEmployeur;
  motivationDetailleeFournie: boolean;
  avisSeppFavorable: boolean;
  chargeDisproportionneeInvoquee: boolean;
  devisExterneFourni: boolean;
  mesuresAlternativesProposees: boolean;
  sanctionSubie: DiscriminationBeHandicapAmenagementSanctionSubie;
  dateSanction: string | null;
  salaireMensuelBrut: number | null;
  procedureUniaSaisie: boolean;

  // -- Outputs calculés --
  /** Verdict hiérarchisé 7 états. */
  verdict: DiscriminationBeHandicapAmenagementVerdict;
  /** Qualification handicap considérée comme acquise (reconnue / SEPP / mutuelle / factuel). */
  handicapQualifie: boolean;
  /** Demande d'aménagement formalisée (écrit travailleur ou suggestion SEPP). */
  demandeFormalisee: boolean;
  /** Refus caractérisé (REFUSE_MOTIVE_DETAILLE / REFUSE_NON_MOTIVE / NON_REPONDU_RAISONNABLE). */
  refusCaracterise: boolean;
  /** Charge disproportionnée objectivement démontrée par l'employeur. */
  chargeDisproportionneeDemontree: boolean;
  /** Représailles présumées (sanction post-demande, art. 17). */
  representaillesPresumees: boolean;
  /** Indemnité forfaitaire 6 mois indicative (art. 28 § 2 1°) en euros. */
  indemniteForfaitaire6Mois: number | null;

  // -- Verdict + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (SEPP, UNIA, subsides, articulation reclassement). */
  avertissement: string;
}

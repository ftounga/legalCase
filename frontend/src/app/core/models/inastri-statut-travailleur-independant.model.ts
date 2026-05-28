/**
 * SF-219-27b : modèle frontend de l'outil F-219 — INASTI statut
 * travailleur indépendant BE (Loi du 27/06/1969 + AR n° 38 du
 * 27/07/1967 + AR du 19/12/1967 + Loi-programme I du 27/12/2006
 * art. 328 à 333 « doctrine Bart Buysse » + art. 337/1 à 337/3
 * critères sectoriels).
 *
 * <p><b>BE uniquement</b> — le régime français de qualification
 * salarié / indépendant (Code du travail art. L. 8221-6 présomption
 * de non-salariat + URSSAF redressement art. R. 243-18 + Loi
 * Madelin + statut autoentrepreneur Loi 04/08/2008) constitue un
 * dispositif juridiquement distinct dans ses critères et sa
 * procédure (pas de présomption sectorielle 5 secteurs, pas de
 * doctrine Bart Buysse, pas de présomption art. 328 § 1 C. pén.
 * soc. mobilisable en l'absence de DPAE). Aucune transposition
 * mécanique. Le gate {@code workspaceCountry} est porté côté
 * composant ; le backend renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-27 backend
 * ({@code InastriStatutTravailleurIndependantRequest} +
 * {@code InastriStatutTravailleurIndependantResponse}).</p>
 */

/**
 * Volonté des parties — critère 1/4 art. 333 Loi-programme I
 * 27/12/2006 (indice non décisif, primauté de l'exécution réelle —
 * Cass. BE 23/12/2002 S.02.0021.F).
 *
 * <ul>
 *   <li>{@code INDEPENDANT_EXPLICITE} — convention écrite expressément
 *       indépendante (contrat d'entreprise / prestations / INASTI).</li>
 *   <li>{@code SALARIE_EXPLICITE} — convention écrite expressément
 *       salariée (contrat de travail Loi 03/07/1978).</li>
 *   <li>{@code IMPLICITE_OU_AMBIGUE} — pas de qualification écrite
 *       claire ; déduction comportements concordants.</li>
 * </ul>
 */
export type InastriStatutTravailleurIndependantVolonte =
  | 'INDEPENDANT_EXPLICITE'
  | 'SALARIE_EXPLICITE'
  | 'IMPLICITE_OU_AMBIGUE';

/**
 * Modalité d'évaluation des 3 critères de liberté (organisation du
 * temps de travail, organisation du travail, contrôle hiérarchique)
 * — art. 333 Loi-programme I 27/12/2006.
 *
 * <ul>
 *   <li>{@code TOTALE_INDEPENDANT} — liberté totale / absence de
 *       contrôle (indice indépendance).</li>
 *   <li>{@code PARTIELLE} — liberté partielle / contrôle ponctuel
 *       (indice neutre).</li>
 *   <li>{@code AUCUNE_SALARIE} — aucune liberté / contrôle régulier
 *       (indice salariat).</li>
 * </ul>
 */
export type InastriStatutTravailleurIndependantLiberte =
  | 'TOTALE_INDEPENDANT'
  | 'PARTIELLE'
  | 'AUCUNE_SALARIE';

/**
 * Secteur d'activité économique pour la présomption sectorielle
 * art. 337/1 à 337/3 (Loi-programme I 27/12/2006 modifiée Loi
 * 25/08/2012 ; extension agriculture / horticulture AR 29/10/2013 ;
 * Cour const. arrêt 167/2013 du 19/12/2013).
 */
export type InastriStatutTravailleurIndependantSecteur =
  | 'CONSTRUCTION'
  | 'TRANSPORT_DE_CHOSES'
  | 'GARDIENNAGE'
  | 'NETTOYAGE'
  | 'AGRICULTURE_HORTICULTURE'
  | 'AUTRE';

/**
 * Statut déclaré par la personne au moment de l'analyse.
 *
 * <ul>
 *   <li>{@code INDEPENDANT_INASTI} — déclaré indépendant, affilié
 *       caisse d'assurances sociales (AR n° 38 du 27/07/1967).</li>
 *   <li>{@code SALARIE_DIMONA} — déclaré salarié, DIMONA ONSS
 *       effectuée (Loi-programme 24/12/2002).</li>
 *   <li>{@code SANS_DECLARATION} — aucune affiliation déclarée
 *       (présomption simple de salariat art. 328 § 1 C. pén. soc.
 *       mobilisable en cas d'absence DIMONA).</li>
 * </ul>
 */
export type InastriStatutTravailleurIndependantStatutDeclare =
  | 'INDEPENDANT_INASTI'
  | 'SALARIE_DIMONA'
  | 'SANS_DECLARATION';

/**
 * Verdict de l'outil — 5 cas.
 *
 * <ul>
 *   <li>{@code INDEPENDANT_CONFIRME} — qualification d'indépendant
 *       confirmée (4 critères généraux compatibles autonomie).</li>
 *   <li>{@code SALARIE_REQUALIFIE} — relation à requalifier en
 *       salariat (majorité des critères pointe vers subordination).</li>
 *   <li>{@code FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE} —
 *       présomption sectorielle art. 337/2 § 1 déclenchée.</li>
 *   <li>{@code PRESUMPTION_GENERALE_SALARIAT} — présomption générale
 *       art. 328 § 1 C. pén. soc. mobilisable.</li>
 *   <li>{@code A_QUALIFIER} — analyse complémentaire avocat requise.</li>
 * </ul>
 */
export type InastriStatutTravailleurIndependantVerdict =
  | 'INDEPENDANT_CONFIRME'
  | 'SALARIE_REQUALIFIE'
  | 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE'
  | 'PRESUMPTION_GENERALE_SALARIAT'
  | 'A_QUALIFIER';

/**
 * Body POST {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/inastri-statut-travailleur-independant}.
 */
export interface InastriStatutTravailleurIndependantRequest {
  /** Critère 1/4 — volonté des parties (art. 333). */
  volonteParties: InastriStatutTravailleurIndependantVolonte;
  /** Critère 2/4 — liberté d'organisation du temps de travail. */
  liberteOrganisationTemps: InastriStatutTravailleurIndependantLiberte;
  /** Critère 3/4 — liberté d'organisation du travail. */
  liberteOrganisationTravail: InastriStatutTravailleurIndependantLiberte;
  /** Critère 4/4 — exercice d'un contrôle hiérarchique. */
  controleHierarchique: InastriStatutTravailleurIndependantLiberte;
  /** Secteur économique (présomption art. 337/2). */
  secteur: InastriStatutTravailleurIndependantSecteur;
  /** Nombre de critères sectoriels indiquant le salariat (0-9). */
  nbCriteresSectorielsSalarie: number;
  /** Statut déclaré par la personne. */
  statutDeclare: InastriStatutTravailleurIndependantStatutDeclare;
  /** DIMONA ONSS effectuée ? */
  dimonaPresente: boolean;
  /** Présence d'un autre client (indice d'autonomie économique). */
  autreClientPresent: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs
 * calculés (scores critères + flags présomptions) + verdict +
 * métadonnées légales.
 */
export interface InastriStatutTravailleurIndependantResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  volonteParties: InastriStatutTravailleurIndependantVolonte;
  liberteOrganisationTemps: InastriStatutTravailleurIndependantLiberte;
  liberteOrganisationTravail: InastriStatutTravailleurIndependantLiberte;
  controleHierarchique: InastriStatutTravailleurIndependantLiberte;
  secteur: InastriStatutTravailleurIndependantSecteur;
  nbCriteresSectorielsSalarie: number;
  statutDeclare: InastriStatutTravailleurIndependantStatutDeclare;
  dimonaPresente: boolean;
  autreClientPresent: boolean;

  // -- Outputs calculés --
  /** Verdict 5 états. */
  verdict: InastriStatutTravailleurIndependantVerdict;
  /** Score des 4 critères généraux orientant vers indépendant. */
  scoreCriteresGenerauxIndependant: number;
  /** Score des 4 critères généraux orientant vers salariat. */
  scoreCriteresGenerauxSalarie: number;
  /** Présomption sectorielle applicable (secteur visé) ? */
  presumptionSectorielleApplicable: boolean;
  /** Présomption sectorielle effectivement déclenchée ? */
  presumptionSectorielleDeclenchee: boolean;
  /** Présomption générale art. 328 § 1 mobilisable ? */
  presumptionGeneraleSalariatMobilisable: boolean;
  /** Requalification salariat recommandée ? */
  requalificationSalariatRecommandee: boolean;
  /** DIMONA cohérente avec statut déclaré ? */
  dimonaCoherenteAvecStatutDeclare: boolean;
  /** Mono-client = indice de salariat (Cass. BE 23/12/2002) ? */
  monoClientIndiceSalariat: boolean;

  // -- Verdict + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (primauté exécution, charge probatoire, articulation). */
  avertissement: string;
}

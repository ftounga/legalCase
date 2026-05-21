/**
 * F-217 SF-217-15 : modèles TypeScript de l'outil décisionnel
 * "Protection du majeur incapable (Belgique)" (`protection-majeur-be`).
 *
 * BELGIQUE uniquement. Contrat API importé de SF-217-14 (backend, endpoints
 * POST/GET figés — loi du 17/03/2013 / CC art. 490 nouveau / CJ art. 594-595).
 */

/** Verdict d'analyse de la situation de protection du majeur. */
export type ProtectionMajeurBeVerdict =
  | 'MANDAT_EXTRA_JUDICIAIRE_VALABLE'
  | 'MESURE_PROTECTION_RECOMMANDEE'
  | 'URGENCE_MESURE_PROVISOIRE'
  | 'QUALIFICATION_INCOMPLETE';

/** Nature de l'altération à l'origine de l'incapacité. */
export type NatureAlterationBe =
  | 'MEDICALE_DURABLE'
  | 'MEDICALE_TEMPORAIRE'
  | 'PRODIGALITE'
  | 'AUTRE';

/** Gravité de l'incapacité (CC art. 490 nouveau — à vérifier). */
export type GraviteIncapaciteBe =
  | 'INCAPACITE_GERER_BIENS'
  | 'INCAPACITE_GERER_PERSONNE'
  | 'LES_DEUX'
  | 'INCAPACITE_PARTIELLE';

/** Niveau d'urgence procédurale. */
export type NiveauUrgenceProtectionBe =
  | 'URGENCE_VITALE'
  | 'URGENCE_PATRIMONIALE'
  | 'AUCUNE_URGENCE';

/** Mode de saisine envisagé. */
export type ModeSaisineProtectionBe = 'REQUETE_JP' | 'INDETERMINE';

/** Mesure de protection recommandée. */
export type MesureProtectionBe =
  | 'MANDAT_EXTRA_JUDICIAIRE'
  | 'ADMINISTRATION_BIENS'
  | 'ADMINISTRATION_PERSONNE'
  | 'ADMINISTRATION_BIENS_ET_PERSONNE'
  | 'MESURE_PROVISOIRE_URGENCE'
  | 'AUCUNE_RECOMMANDATION';

/** Juridiction compétente pour la saisine. */
export type JuridictionProtectionBe = 'JUSTICE_PAIX' | 'AUTRE_JURIDICTION';

/** Code structuré d'un acte protégé. */
export type ActeProtegeBeCode =
  | 'DISPOSITION_BIENS_IMMOBILIERS'
  | 'DISPOSITION_BIENS_MOBILIERS_VALEURS'
  | 'OPERATION_BANCAIRE'
  | 'ACTES_PATRIMONIAUX_COURANTS'
  | 'ACTES_PERSONNELS_LIEU_VIE'
  | 'ACTES_PERSONNELS_SOINS'
  | 'MARIAGE_PACS_TESTAMENT'
  | 'ACTES_QUOTIDIENS';

/** Nécessité d'autorisation / autonomie sur un acte protégé. */
export type NecessiteActeBe =
  | 'AUTORISATION_JP_PREALABLE'
  | 'ADMINISTRATEUR_SEUL'
  | 'AUTONOMIE_PRESERVEE'
  | 'INTERDICTION_ABSOLUE';

/** Acte protégé exposé dans la réponse. */
export interface ActeProtegeBe {
  code: ActeProtegeBeCode;
  libelle: string;
  necessite: NecessiteActeBe;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/protection-majeur-be`.
 * `mandatExtraJudiciaireDateSignature` est requise si
 * `mandatExtraJudiciaireSigne === true`.
 */
export interface ProtectionMajeurBeRequest {
  natureAlteration: NatureAlterationBe;
  graviteIncapacite: GraviteIncapaciteBe;
  mandatExtraJudiciaireSigne: boolean;
  mandatExtraJudiciaireDateSignature: string | null;
  declarationAnticipeeExiste: boolean;
  environnementFamilialProtecteur: boolean;
  niveauUrgence: NiveauUrgenceProtectionBe;
  modeSaisineEnvisage: ModeSaisineProtectionBe;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot des inputs pour permettre la
 * ré-édition du formulaire après rechargement, **plus** les champs calculés
 * (verdict, mesure recommandée, juridiction, actes protégés, actions
 * concrètes, bases juridiques, messages).
 */
export interface ProtectionMajeurBeResponse extends ProtectionMajeurBeRequest {
  caseFileId: string;
  verdict: ProtectionMajeurBeVerdict;
  mesureRecommandee: MesureProtectionBe;
  juridictionCompetente: JuridictionProtectionBe | null;
  fondementProcedural: string;
  actesProteges: ActeProtegeBe[];
  actionsConcretes: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}

/**
 * F-217 SF-217-13 : modèles TypeScript de l'outil décisionnel
 * "Succession BE — acceptation / renonciation"
 * (`succession-be-acceptation-renonciation`).
 *
 * BELGIQUE uniquement. Contrat API importé de SF-217-12 (backend, endpoints
 * POST/GET figés — CC art. 774+ nouveau).
 */

/** Verdict d'analyse de l'option successorale. */
export type SuccessionBeAcceptationRenonciationVerdict =
  | 'OPTION_LIBRE_DELAI_OK'
  | 'OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE'
  | 'OPTION_RECOMMANDEE_RENONCIATION'
  | 'ACCEPTATION_TACITE_PROBABLE'
  | 'DELAI_CRITIQUE'
  | 'DELAI_DEPASSE'
  | 'QUALIFICATION_INCOMPLETE';

/** Qualité d'héritier appelé à la succession. */
export type QualiteHeritierBe =
  | 'CONJOINT_SURVIVANT'
  | 'COHABITANT_LEGAL_SURVIVANT'
  | 'ENFANT'
  | 'DESCENDANT_REPRESENTATION'
  | 'PARENT'
  | 'FRERE_SOEUR'
  | 'LEGATAIRE_UNIVERSEL'
  | 'LEGATAIRE_PARTICULIER';

/** État du patrimoine successoral apparent. */
export type EtatPatrimoineSuccessoralBe =
  | 'SOLVABLE'
  | 'DOUTEUX'
  | 'INSOLVABLE'
  | 'INCONNU';

/** Acte de l'héritier susceptible de valoir acceptation tacite. */
export type ActeHeritierBe =
  | 'AUCUN'
  | 'ENCAISSEMENT_CREANCE_DEFUNT'
  | 'PAIEMENT_DETTE_DEFUNT'
  | 'VENTE_BIEN_SUCCESSION'
  | 'PRISE_POSSESSION_BIENS'
  | 'ACTE_CONSERVATOIRE_NEUTRE'
  | 'DEMANDE_INVENTAIRE';

/** Volonté exprimée par le client. */
export type VolonteClientSuccessionBe =
  | 'ACCEPTER'
  | 'ACCEPTER_BENEFICE_INVENTAIRE'
  | 'RENONCER'
  | 'INDECIS';

/** Option recommandée par l'outil. */
export type OptionRecommandeeSuccessionBe =
  | 'ACCEPTER'
  | 'ACCEPTER_BENEFICE_INVENTAIRE'
  | 'RENONCER';

/** Statut du délai d'option à la date du calcul. */
export type DelaiStatutBe = 'OK' | 'CRITIQUE' | 'DEPASSE';

/** Code structuré d'un risque identifié. */
export type RisqueSuccessionBeCode =
  | 'ACTE_HERITIER_VALANT_ACCEPTATION_TACITE'
  | 'DELAI_TRES_COURT'
  | 'PATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE'
  | 'MISE_EN_DEMEURE_DELAI_REDUIT'
  | 'DEVOLUTION_FORCEE_RISQUE';

/** Sévérité d'un risque. */
export type SeveriteRisqueBe = 'LOW' | 'MEDIUM' | 'HIGH';

/** Risque identifié — exposé dans la réponse. */
export interface RisqueSuccessionBe {
  code: RisqueSuccessionBeCode;
  libelle: string;
  fondement: string;
  severite: SeveriteRisqueBe;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/succession-be-acceptation-renonciation`.
 * `dateDeces` au format ISO `YYYY-MM-DD`, non future.
 * `dateMiseEnDemeureCreancier` requise si `miseEnDemeureCreancier === true`.
 */
export interface SuccessionBeAcceptationRenonciationRequest {
  dateDeces: string;
  qualiteHeritier: QualiteHeritierBe;
  etatPatrimoineSuccessoral: EtatPatrimoineSuccessoralBe;
  actesAccomplis: ActeHeritierBe[];
  volonteClient: VolonteClientSuccessionBe;
  miseEnDemeureCreancier: boolean;
  dateMiseEnDemeureCreancier: string | null;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot des inputs (hérité de
 * `SuccessionBeAcceptationRenonciationRequest`) pour permettre la ré-édition
 * du formulaire après rechargement, **plus** les champs calculés (verdict,
 * option recommandée, délai, risques, actions concrètes, bases juridiques).
 */
export interface SuccessionBeAcceptationRenonciationResponse
  extends SuccessionBeAcceptationRenonciationRequest {
  caseFileId: string;
  verdict: SuccessionBeAcceptationRenonciationVerdict;
  optionRecommandee: OptionRecommandeeSuccessionBe;
  dateLimiteOption: string | null;
  joursRestants: number | null;
  delaiStatut: DelaiStatutBe;
  actionsConcretes: string[];
  risques: RisqueSuccessionBe[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}

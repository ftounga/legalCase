/**
 * SF-213-09b : modèle frontend de l'outil F-213 — Acte équipollent à
 * rupture BE (Loi du 03/07/1978 art. 20 + jurisprudence Cass. BE
 * 23/12/1957).
 *
 * <p>BE uniquement — la France a un dispositif distinct (prise d'acte
 * L. 1237-19 C. trav. FR + résiliation judiciaire) — outil non exposé
 * côté France.</p>
 *
 * <p>Contrat figé dans SF-213-09 backend
 * ({@code LicenciementBeActeEquipollentResponse} +
 * {@code LicenciementBeActeEquipollentRequest}).</p>
 *
 * <p>Verdict 4 états :
 * <ul>
 *   <li>{@code ACTE_EQUIPOLLENT_PROBABLE} (rouge) — modification
 *       substantielle d'un élément essentiel : rupture imputable à
 *       l'employeur ; ouvre droit à l'indemnité compensatoire de préavis
 *       (ICP).</li>
 *   <li>{@code PAS_ACTE_EQUIPOLLENT} (vert) — modification mineure relevant
 *       du Ius Variandi.</li>
 *   <li>{@code RISQUE_ACCEPTATION_TACITE} (rouge) — silence du salarié
 *       prolongé (> 30 j) sur une modification potentiellement
 *       substantielle : peut valoir acceptation tacite.</li>
 *   <li>{@code A_ANALYSER} (orange) — éléments insuffisants pour trancher
 *       (ampleur indéterminée ou caractère essentiel discutable).</li>
 * </ul></p>
 */

/**
 * Type de modification unilatérale (5 cas).
 *
 * <ul>
 *   <li>{@code SALAIRE} — taux, structure, primes contractuelles.</li>
 *   <li>{@code LIEU_TRAVAIL} — hors clause de mobilité contractuelle.</li>
 *   <li>{@code FONCTION} — responsabilités, qualification.</li>
 *   <li>{@code HORAIRE} — horaire de travail.</li>
 *   <li>{@code AUTRE} — autre élément du contrat.</li>
 * </ul>
 */
export type TypeModificationUnilaterale =
  | 'SALAIRE'
  | 'LIEU_TRAVAIL'
  | 'FONCTION'
  | 'HORAIRE'
  | 'AUTRE';

/**
 * Ampleur de la modification (pilote le verdict).
 *
 * <ul>
 *   <li>{@code MINEURE} — Ius Variandi → {@code PAS_ACTE_EQUIPOLLENT}.</li>
 *   <li>{@code SUBSTANTIELLE} — point de bascule vers acte équipollent
 *       (combiné à {@code elementEssentielDuContrat=true}).</li>
 *   <li>{@code INDETERMINEES} — analyse approfondie requise →
 *       {@code A_ANALYSER}.</li>
 * </ul>
 */
export type AmpleurModification =
  | 'MINEURE'
  | 'SUBSTANTIELLE'
  | 'INDETERMINEES';

/** Verdict 4 états — voir doc en-tête. */
export type LicenciementBeActeEquipollentVerdict =
  | 'ACTE_EQUIPOLLENT_PROBABLE'
  | 'PAS_ACTE_EQUIPOLLENT'
  | 'RISQUE_ACCEPTATION_TACITE'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent}.
 *
 * <p>Champs obligatoires : {@code typeModification},
 * {@code ampleurModification}, {@code dateModification}. Les flags par
 * défaut {@code false}. Les champs optionnels {@code remunerationHebdomadaireBrute}
 * et {@code dureePreavisCalculeeSemaines} servent au calcul ICP indicatif
 * (rendu seulement si verdict {@code ACTE_EQUIPOLLENT_PROBABLE}).</p>
 */
export interface LicenciementBeActeEquivalentRequest {
  /** Obligatoire — nature de l'élément modifié. */
  typeModification: TypeModificationUnilaterale;
  /** Obligatoire — pilote du verdict. */
  ampleurModification: AmpleurModification;
  /** Obligatoire — date de la modification (ISO yyyy-MM-dd) — ancre du délai d'acceptation tacite. */
  dateModification: string;
  /** Caractère essentiel du contrat (défaut false) — combiné à SUBSTANTIELLE → ACTE_EQUIPOLLENT_PROBABLE. */
  elementEssentielDuContrat: boolean;
  /** Le salarié a-t-il protesté formellement ? (défaut false). */
  salarieAProteste: boolean;
  /** Délai (en jours) depuis la modification — calculé serveur si null. */
  delaiDepuisModificationJours: number | null;
  /** Rémunération hebdomadaire brute (€) — base de l'ICP indicatif. */
  remunerationHebdomadaireBrute: number | null;
  /** Durée de préavis (semaines) calculée par ailleurs (SF-213-03 / SF-213-04). */
  dureePreavisCalculeeSemaines: number | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface LicenciementBeActeEquivalentResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  typeModification: TypeModificationUnilaterale;
  ampleurModification: AmpleurModification;
  elementEssentielDuContrat: boolean;
  dateModification: string;
  salarieAProteste: boolean;
  delaiDepuisModificationJours: number | null;
  remunerationHebdomadaireBrute: number | null;
  dureePreavisCalculeeSemaines: number | null;

  // -- Outputs métier --
  /** Verdict 4 états. */
  verdict: LicenciementBeActeEquipollentVerdict;
  /** Fondement juridique humanisé (loi + article + jurisprudence). */
  fondementJuridique: string;
  /** ICP indicatif (€) — calculée si verdict ACTE_EQUIPOLLENT_PROBABLE + données dispo. */
  icpIndicatif: number | null;
  /** True si le silence du salarié peut valoir acceptation tacite. */
  risqueAcceptationTacite: boolean;
  /** Délai de protestation recommandé (jours) — généralement 30. */
  delaiRecommandeProtestationJours: number;
  /** Base juridique courte (Loi 03/07/1978 art. 20). */
  baseJuridique: string;
  /** Avertissement éventuel (délai dépassé, données partielles…). */
  avertissement: string | null;
}

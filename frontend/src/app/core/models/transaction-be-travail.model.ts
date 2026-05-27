/**
 * SF-213-06b : modèle frontend de l'outil F-213 — Validité d'une transaction
 * de fin de contrat en droit belge.
 *
 * <p>Belgique uniquement — art. 2044 Code civil belge + Loi du 03/07/1978
 * art. 6 (dispositions impératives, renonciation à un droit d'ordre public
 * prohibée). La France a un outil distinct {@code F-DT-31-transaction}
 * (régime art. 2044 Cciv FR différent : moins exigeant sur la spécificité
 * des renonciations, pas de catalogue dispositions impératives CCT BE).</p>
 *
 * <p>Contrat figé dans SF-213-06 backend
 * ({@code TransactionBeTravailResponse} +
 * {@code TransactionBeTravailRequest}).</p>
 *
 * <p>Référence juridique :
 * <ul>
 *   <li>Art. 2044 Code civil belge — la transaction suppose des concessions
 *       réciproques. À défaut, il s'agit d'une quittance, pas d'une
 *       transaction (verdict {@code INVALIDE}, raison
 *       {@code ABSENCE_CONCESSIONS}).</li>
 *   <li>Loi du 03/07/1978 art. 6 — les renonciations à un droit d'ordre
 *       public (créances salariales futures, dispositions impératives
 *       CCT…) sont prohibées. Si détectée → verdict
 *       {@code INVALIDE_PARTIELLE} (clause partiellement nulle, le reste
 *       de la transaction subsiste).</li>
 *   <li>Mention de litige né ou à naître requise — sans cela la transaction
 *       ne relève pas de l'art. 2044 (verdict {@code A_COMPLETER}, raison
 *       {@code LITIGE_NON_VISE}).</li>
 *   <li>Ratio {@code montantTransactionBrut / indemniteLegaleEtimee × 100} :
 *       avertissement de lésion si {@code < 50 %} (la transaction reste
 *       valable mais signale un déséquilibre à expliciter à la cliente).</li>
 * </ul></p>
 *
 * <p><b>Correctif orthographique</b> : la mini-spec utilisait
 * {@code renonciationOrdrePunlicDetectee} (typo « Punlic »). La forme
 * correcte {@code renonciationOrdrePublicDetectee} est appliquée
 * partout (modèle frontend, backend, tests) — alignement strict avec
 * le contrat backend mergé SF-213-06.</p>
 */

/**
 * Verdict de l'analyseur (4 états — miroir enum backend
 * {@code TransactionBeTravailVerdict}).
 *
 * <ul>
 *   <li>{@code VALIDE} — concessions employeur décrites + mention de
 *       contestation + renonciations expresses listées + pas de
 *       renonciation à un droit d'ordre public (badge vert).</li>
 *   <li>{@code INVALIDE} — absence de concessions réciproques
 *       (art. 2044 Cciv : c'est une quittance, pas une transaction).
 *       Badge rouge.</li>
 *   <li>{@code INVALIDE_PARTIELLE} — renonciation à un droit d'ordre
 *       public détectée (Loi 03/07/1978 art. 6 : clause partiellement
 *       nulle, le reste subsiste). Badge ambre.</li>
 *   <li>{@code A_COMPLETER} — élément formel manquant (litige non visé
 *       ou checklist renonciations vide). Badge gris.</li>
 * </ul>
 */
export type TransactionBeTravailVerdict =
  | 'VALIDE'
  | 'INVALIDE'
  | 'INVALIDE_PARTIELLE'
  | 'A_COMPLETER';

/**
 * Raison précise associée au verdict (null si {@code VALIDE}).
 *
 * <ul>
 *   <li>{@code ABSENCE_CONCESSIONS} — l'employeur n'a rien concédé
 *       (verdict {@code INVALIDE} ; art. 2044 Cciv BE).</li>
 *   <li>{@code RENONCIATION_ORDRE_PUBLIC} — renonciation à un droit
 *       d'ordre public détectée (verdict {@code INVALIDE_PARTIELLE} ;
 *       Loi 03/07/1978 art. 6).</li>
 *   <li>{@code LITIGE_NON_VISE} — litige non visé ou liste des
 *       renonciations vide (verdict {@code A_COMPLETER}).</li>
 * </ul>
 */
export type TransactionBeTravailRaisonInvalidite =
  | 'ABSENCE_CONCESSIONS'
  | 'RENONCIATION_ORDRE_PUBLIC'
  | 'LITIGE_NON_VISE';

/**
 * Item d'une checklist de renonciations expresses (miroir record backend
 * {@code TransactionBeTravailChecklistItem}).
 *
 * <p>{@code valide=true} → renonciation conforme (droit acquis disponible).
 * {@code valide=false} → renonciation suspectée de porter sur un droit
 * d'ordre public. V1 : tous les items sont {@code valide=true} sauf si
 * {@code renonciationOrdrePublicDetectee=true} au niveau global (auquel
 * cas tous sont {@code false} — stratégie conservatrice).</p>
 */
export interface TransactionBeTravailChecklistItem {
  /** Libellé de la renonciation (ex. « Renonciation à l'indemnité de rupture »). */
  item: string;
  /** True si renonciation conforme ; false si suspectée d'ordre public. */
  valide: boolean;
}

/**
 * Body POST `/api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail`.
 *
 * <p>Seuls {@code montantTransactionBrut} et {@code renonciationsListees}
 * (peut être vide) sont <b>requis</b>. Les autres champs sont des flags
 * IA défautés par le backend (false pour booleans, null pour
 * {@code indemniteLegaleEtimee}).</p>
 */
export interface TransactionBeTravailRequest {
  /** Montant total brut proposé (€ > 0). */
  montantTransactionBrut: number;
  /**
   * Indemnité légale estimée (ICP + CCT 109…) en €.
   * Optionnel — si > 0, déclenche le calcul du ratio + avertissement
   * de lésion si ratio < 50 %.
   */
  indemniteLegaleEtimee?: number | null;
  /**
   * L'employeur concède-t-il quelque chose ? Défaut false côté backend.
   * Si false → verdict {@code INVALIDE} (raison ABSENCE_CONCESSIONS).
   */
  concessionsEmployeurDescrites: boolean;
  /** Liste (peut être vide) des renonciations expresses du salarié. */
  renonciationsListees: string[];
  /**
   * Renonciation à un droit d'ordre public détectée. Défaut false.
   * Si true → verdict {@code INVALIDE_PARTIELLE} (raison
   * RENONCIATION_ORDRE_PUBLIC).
   */
  renonciationOrdrePublicDetectee: boolean;
  /**
   * La transaction mentionne-t-elle un litige né ou à naître ?
   * Défaut false. Si false → verdict {@code A_COMPLETER} (raison
   * LITIGE_NON_VISE).
   */
  mentionContestation: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 *
 * <p>{@code ratioPourcentage} est {@code null} si
 * {@code indemniteLegaleEtimee} n'a pas été fourni / était nul.
 * {@code avertissement} est {@code null} sauf en cas de lésion détectée
 * (ratio &lt; 50 %).</p>
 */
export interface TransactionBeTravailResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;
  /** Echo montant brut proposé. */
  montantTransactionBrut: number;
  /** Echo indemnité légale estimée (peut être null). */
  indemniteLegaleEtimee: number | null;
  /** Echo flag concessions employeur. */
  concessionsEmployeurDescrites: boolean;
  /** Echo liste renonciations. */
  renonciationsListees: string[];
  /** Echo flag renonciation ordre public (orthographe corrigée). */
  renonciationOrdrePublicDetectee: boolean;
  /** Echo flag mention contestation. */
  mentionContestation: boolean;
  /** Verdict 4 états. */
  verdict: TransactionBeTravailVerdict;
  /** Raison du verdict — null si VALIDE. */
  raisonInvalidite: TransactionBeTravailRaisonInvalidite | null;
  /**
   * Ratio (montantTransactionBrut / indemniteLegaleEtimee) × 100.
   * Null si indemnité légale non fournie ou ≤ 0.
   */
  ratioPourcentage: number | null;
  /**
   * Avertissement éventuel (lésion ratio < 50 %). Null sinon.
   * Affiché en bannière ambre.
   */
  avertissement: string | null;
  /**
   * Checklist des renonciations expresses — 1 item par entrée de
   * {@code renonciationsListees}. Vide si la liste d'entrée était vide.
   */
  checklistRenonciations: TransactionBeTravailChecklistItem[];
  /** Base juridique humanisée (art. 2044 Cciv + Loi 03/07/1978 art. 6). */
  baseJuridique: string;
}

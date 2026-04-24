import {
  CoherenceAlert,
  CoherenceAlertSeverity,
  CoherenceAlertSource,
} from './coherence-alert.model';

/**
 * SF-155-05 : helper fluide pour construire une `CoherenceAlert<F>` en
 * consolidant plusieurs sources (F96 / QUESTION_IA / IA / PIECE_MANQUANTE).
 *
 * Design :
 * - Chaque composant conserve sa logique métier pour détecter une
 *   divergence (ex. "l'IA dit MOTIF=TRAVAIL, l'avocat dit ETUDES") —
 *   le builder ne décide **pas** qu'il y a divergence.
 * - Une fois une divergence détectée pour une source donnée, le composant
 *   appelle `addSource(source, { expectedDisplay, reason, pieceTexte? })`.
 *   - Le builder retient la **première** `expectedDisplay` (ordre d'insertion).
 *   - Les sources ultérieures avec la même `expectedDisplay` deviennent
 *     des `contributors` supplémentaires et leur `reason` est agrégée.
 *   - Les sources avec une `expectedDisplay` divergente sont **ignorées**
 *     (pattern canonique `buildMotifAlert` — la première source gagne).
 * - Si ≥ 2 sources ont contribué, `source` final = `MULTI` ; sinon égale
 *   à l'unique source.
 * - `.build()` retourne `null` si aucune source n'a contribué —
 *   le composant consommateur peut ainsi garder la même sémantique
 *   "return null" que la version manuelle.
 *
 * Pattern d'usage :
 *
 * ```ts
 * const alert = CoherenceAlertBuilder
 *   .forField<HLNAlertField>('SALAIRE')
 *   .withSeverity('WARNING')
 *   .addSource('IA', {
 *     expectedDisplay: `${aiVal.toLocaleString('fr-FR')} €`,
 *     reason: `Analyse du dossier : ${aiVal.toLocaleString('fr-FR')} €`,
 *   })
 *   .addSource('F96', {
 *     expectedDisplay: `${aiVal.toLocaleString('fr-FR')} €`,
 *     reason: `Checklist : salaire confirmé ${aiVal} €`,
 *   })
 *   .build();
 * ```
 *
 * @template F type enum des fields audités par le composant.
 */
export class CoherenceAlertBuilder<F extends string> {
  private readonly contributors: CoherenceAlertSource[] = [];
  private readonly reasons: string[] = [];
  private severity: CoherenceAlertSeverity = 'WARNING';
  private expected: string | null = null;
  private pieceTexte: string | null = null;

  private constructor(private readonly field: F) {}

  /**
   * Crée un builder pour un field donné. Appelle `.addSource()` N fois
   * puis `.build()` pour obtenir l'alerte (ou `null` si aucune divergence).
   */
  static forField<F extends string>(field: F): CoherenceAlertBuilder<F> {
    return new CoherenceAlertBuilder<F>(field);
  }

  /**
   * Override la sévérité (défaut `WARNING`). Typiquement `CRITICAL` pour
   * les cas urgents (48h OQTF, placement CRA détecté, transfert imminent).
   */
  withSeverity(severity: CoherenceAlertSeverity): this {
    this.severity = severity;
    return this;
  }

  /**
   * Ajoute un contributor à l'alerte.
   *
   * Règles :
   * - La première `expectedDisplay` non vide est retenue comme valeur
   *   canonique de l'alerte.
   * - Les sources suivantes sont ajoutées aux `contributors` **uniquement
   *   si** leur `expectedDisplay` matche la valeur retenue (consolidation).
   *   Les sources avec une `expectedDisplay` différente sont ignorées
   *   (alignement sur pattern `buildMotifAlert` canonique).
   * - Si `pieceTexte` est fourni par un contributor `PIECE_MANQUANTE`, il
   *   est retenu sur l'alerte finale.
   * - La source `MULTI` ne doit pas être passée ici — elle est dérivée
   *   automatiquement quand ≥ 2 contributors sont ajoutés.
   *
   * @param source source du contributor (toute sauf `MULTI`).
   * @param data `expectedDisplay` + `reason` + optionnel `pieceTexte`.
   * @returns this (chaînable).
   */
  addSource(
    source: Exclude<CoherenceAlertSource, 'MULTI'>,
    data: { expectedDisplay: string; reason: string; pieceTexte?: string | null }
  ): this {
    if (!data.expectedDisplay || !data.reason) return this;
    // Première source : fixe la valeur canonique.
    if (this.expected === null) {
      this.expected = data.expectedDisplay;
      this.contributors.push(source);
      this.reasons.push(data.reason);
      if (data.pieceTexte) this.pieceTexte = data.pieceTexte;
      return this;
    }
    // Sources suivantes : consolident uniquement si même expected.
    if (data.expectedDisplay !== this.expected) return this;
    this.contributors.push(source);
    this.reasons.push(data.reason);
    if (data.pieceTexte && !this.pieceTexte) this.pieceTexte = data.pieceTexte;
    return this;
  }

  /**
   * Raccourci pratique pour ajouter une pièce manquante F-145 qui ne fixe
   * pas l'`expectedDisplay` mais enrichit `pieceTexte` et contribue aux
   * `contributors` si l'alerte est déjà initiée par une autre source.
   *
   * Diffère de `addSource('PIECE_MANQUANTE', ...)` dans le sens où une
   * pièce manquante n'est jamais **seule** à fonder une alerte (il faut
   * une divergence IA/F96/QUESTION_IA d'abord) — si l'alerte n'est pas
   * encore initiée, cet appel est silencieusement ignoré.
   *
   * @param pieceTexte texte F-145 à attacher à l'alerte.
   * @param reason motif lisible (ajouté aux reasons agrégés).
   */
  addPieceManquante(pieceTexte: string, reason?: string): this {
    if (!pieceTexte) return this;
    if (this.expected === null) return this; // pas d'alerte à laquelle s'accrocher
    this.contributors.push('PIECE_MANQUANTE');
    this.reasons.push(reason ?? `Pièce manquante : ${pieceTexte}`);
    this.pieceTexte = pieceTexte;
    return this;
  }

  /**
   * Produit l'alerte finale, ou `null` si aucune source n'a contribué.
   *
   * - `source` = `MULTI` si ≥ 2 contributors, sinon le seul contributor.
   * - `reason` = contributors `reason` joints par ` ET `.
   * - `contributors` préserve l'ordre d'insertion.
   */
  build(): CoherenceAlert<F> | null {
    if (this.contributors.length === 0 || this.expected === null) return null;
    const source: CoherenceAlertSource =
      this.contributors.length > 1 ? 'MULTI' : this.contributors[0];
    return {
      field: this.field,
      source,
      contributors: [...this.contributors],
      severity: this.severity,
      expectedDisplay: this.expected,
      reason: this.reasons.join(' ET '),
      pieceTexte: this.pieceTexte,
    };
  }
}

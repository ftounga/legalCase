import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { DecisionToolSummary, MetierAlertLevel, formatSummary } from '../decision-tool-summary.model';

/**
 * F-192 SF-192-02 — Verdict du badge `🎯` affiché sur la card du panel
 * F-IA-04 quand au moins une piste 🟢 RETAINED de la synthèse est mappée
 * sur l'outil. Calculé via static `getRetainedPistesBadge` exposé par chaque
 * composant outil instrumenté.
 */
export type RetainedPistesBadgeKind = 'aligned' | 'divergent' | 'none';
export interface RetainedPistesBadgeInput {
  kind: RetainedPistesBadgeKind;
  count: number;
}

/**
 * F-193 SF-193-02 — Verdict du pill `🔍 Procédure (V/N/T)` affiché sur la
 * card du panel F-IA-04 quand au moins un check F-96 est aligné sur l'outil.
 * Calculé via le helper partagé `procedure-check-badge.helper.ts` exposé via
 * static `getProcedureChecksBadge` par chaque composant outil instrumenté.
 */
export type ProcedureChecksBadgeKind =
  | 'verified'
  | 'non_compliant'
  | 'to_verify'
  | 'mixed'
  | 'none';

export interface ProcedureChecksBadgeInput {
  kind: ProcedureChecksBadgeKind;
  counts: { verified: number; nonCompliant: number; toVerify: number };
}

/**
 * F-194 SF-194-02 — Verdict du pill `📎 Pièces (D/O/N)` affiché sur la card
 * du panel F-IA-04 quand au moins une pièce est mappée sur cet outil. Calculé
 * via le helper partagé `piece-manquante-badge.helper.ts` exposé via static
 * `getPiecesBadge` par chaque composant outil instrumenté.
 *
 * Couleurs (palette navy/or DESIGN_SYSTEM.md) :
 *   - missing        : navy clair (rappelle qu'il manque encore des pièces)
 *   - obtained       : or plein (toutes obtenues)
 *   - partial        : or contour (mix obtenues + non applicable)
 *   - not_applicable : gris (toutes écartées)
 */
export type PiecesBadgeKind =
  | 'missing'
  | 'partial'
  | 'obtained'
  | 'not_applicable'
  | 'none';

export interface PiecesBadgeInput {
  kind: PiecesBadgeKind;
  counts: { aDemander: number; obtenues: number; nonApplicable: number };
}

/**
 * F-177 SF-177-01 — Card réutilisable pour outil décisionnel.
 *
 * Rend un titre + verdict synthétique + jusqu'à 3 badges en coin (pré-fill IA, alerte F-IA-03,
 * alerte métier). Émet `open` au click (ou Enter/Space) sur la card non-disabled.
 *
 * Consommée par :
 * - SF-177-02 : panel F-IA-04 (remplace l'expand inline → ouvre un MatDialog)
 * - SF-177-09 : dashboard agrégé en haut de page dossier
 */
@Component({
  selector: 'app-decision-tool-card',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './decision-tool-card.component.html',
  styleUrls: ['./decision-tool-card.component.scss'],
})
export class DecisionToolCardComponent {
  @Input({ required: true }) toolId!: string;
  @Input({ required: true }) theme!: string;
  @Input({ required: true }) icon!: string;
  @Input({ required: true }) title!: string;

  @Input() summary: DecisionToolSummary | null = null;
  @Input() prefillCount: number | null = null;
  @Input() coherenceAlertCount: number | null = null;
  @Input() metierAlertLevel: MetierAlertLevel | null = null;
  /**
   * F-192 SF-192-02 — Affiche un pill or `🎯 Aligné/Divergence stratégie
   * retenue (N)` à côté du pill `auto_awesome` quand `kind ≠ 'none'`. `null`
   * (composant non instrumenté) → pill silencieusement masqué.
   */
  @Input() retainedPistesBadge: RetainedPistesBadgeInput | null = null;
  /**
   * F-193 SF-193-02 — Affiche un pill `🔍 Procédure (V/N/T)` à côté des
   * autres pills quand `kind ≠ 'none'`. Couleur selon kind :
   * verified=or, non_compliant=rouge subtil, to_verify=gris, mixed=or
   * (priorité visuelle : non_compliant l'emporte si présent dans counts).
   * `null` (composant non instrumenté) → pill silencieusement masqué.
   */
  @Input() proceduresChecksBadge: ProcedureChecksBadgeInput | null = null;
  /**
   * F-194 SF-194-02 — Affiche un pill `📎 Pièces (D/O/N)` (À demander /
   * Obtenues / Non applicable) à côté des autres pills quand `kind ≠ 'none'`.
   * Couleur selon kind : missing=navy clair (priorité visuelle — il manque
   * encore des pièces), obtained=or plein, partial=or contour, not_applicable
   * =gris. `null` (composant non instrumenté) → pill silencieusement masqué.
   */
  @Input() piecesBadge: PiecesBadgeInput | null = null;
  @Input() disabled = false;
  @Input() flashing = false;

  @Output() open = new EventEmitter<void>();

  protected get formattedSummary(): string | null {
    return formatSummary(this.summary);
  }

  protected get alertLevelClass(): string {
    if (!this.summary?.alertLevel) {
      return '';
    }
    return `card--${this.summary.alertLevel.toLowerCase()}`;
  }

  protected get showPrefillBadge(): boolean {
    return this.prefillCount !== null && this.prefillCount > 0;
  }

  protected get showCoherenceBadge(): boolean {
    return this.coherenceAlertCount !== null && this.coherenceAlertCount > 0;
  }

  protected get showMetierBadge(): boolean {
    return this.metierAlertLevel === 'WARNING' || this.metierAlertLevel === 'ALERT';
  }

  protected get prefillTooltip(): string {
    return `${this.prefillCount} champ(s) pré-rempli(s) par l'IA`;
  }

  protected get prefilledClass(): string {
    return this.showPrefillBadge ? 'tool-card--prefilled' : '';
  }

  protected get prefillAriaLabel(): string {
    const count = this.prefillCount ?? 0;
    return `Pré-rempli par l'IA, ${count} champ${count > 1 ? 's' : ''}`;
  }

  protected get coherenceTooltip(): string {
    return `${this.coherenceAlertCount} alerte(s) de cohérence IA`;
  }

  protected get showRetainedPistesBadge(): boolean {
    return (
      this.retainedPistesBadge !== null
      && this.retainedPistesBadge.kind !== 'none'
      && this.retainedPistesBadge.count > 0
    );
  }

  protected get retainedPistesBadgeLabel(): string {
    if (!this.retainedPistesBadge) return '';
    const n = this.retainedPistesBadge.count;
    return this.retainedPistesBadge.kind === 'aligned'
      ? `Aligné stratégie retenue (${n})`
      : `Divergence stratégie retenue (${n})`;
  }

  protected get retainedPistesBadgeTooltip(): string {
    if (!this.retainedPistesBadge) return '';
    return this.retainedPistesBadge.kind === 'aligned'
      ? `${this.retainedPistesBadge.count} piste(s) 🟢 retenue(s) alignée(s) avec cet outil`
      : `${this.retainedPistesBadge.count} piste(s) 🟢 retenue(s) divergente(s) du recommandé`;
  }

  protected get retainedPistesBadgeClass(): string {
    if (!this.retainedPistesBadge) return '';
    return this.retainedPistesBadge.kind === 'aligned'
      ? 'tool-card__badge--retained-aligned'
      : 'tool-card__badge--retained-divergent';
  }

  /* F-193 SF-193-02 — pill « 🔍 Procédure (V/N/T) ». */
  protected get showProceduresChecksBadge(): boolean {
    if (!this.proceduresChecksBadge) return false;
    if (this.proceduresChecksBadge.kind === 'none') return false;
    const c = this.proceduresChecksBadge.counts;
    return c.verified + c.nonCompliant + c.toVerify > 0;
  }

  /** Formate les compteurs en `V/N/T` (vérifié / non conforme / à vérifier). */
  protected get proceduresChecksBadgeLabel(): string {
    const c = this.proceduresChecksBadge?.counts;
    if (!c) return '';
    return `${c.verified}/${c.nonCompliant}/${c.toVerify}`;
  }

  protected get proceduresChecksBadgeAriaLabel(): string {
    const c = this.proceduresChecksBadge?.counts;
    if (!c) return '';
    return `Vérifications procédurales : ${c.verified} confirmée(s), ${c.nonCompliant} non conforme(s), ${c.toVerify} à vérifier`;
  }

  protected get proceduresChecksBadgeTooltip(): string {
    const c = this.proceduresChecksBadge?.counts;
    if (!c) return '';
    const parts: string[] = [];
    if (c.verified > 0) parts.push(`${c.verified} confirmée(s)`);
    if (c.nonCompliant > 0) parts.push(`${c.nonCompliant} non conforme(s)`);
    if (c.toVerify > 0) parts.push(`${c.toVerify} à vérifier`);
    return parts.length > 0 ? `Vérifications procédurales — ${parts.join(', ')}` : '';
  }

  /**
   * Couleur visuelle. La règle du DESIGN_SYSTEM.md veut le rouge réservé aux
   * alertes critiques : on l'utilise en pill subtil (fond chaud très clair
   * + texte rouge foncé) plutôt qu'en fond rouge plein. Mixed retombe sur
   * la priorité visuelle (non_compliant > to_verify > verified) pour
   * préserver l'attention de l'avocat.
   */
  protected get proceduresChecksBadgeClass(): string {
    if (!this.proceduresChecksBadge) return '';
    const c = this.proceduresChecksBadge.counts;
    if (c.nonCompliant > 0) return 'tool-card__badge--procedures-non-compliant';
    if (c.toVerify > 0) return 'tool-card__badge--procedures-to-verify';
    if (c.verified > 0) return 'tool-card__badge--procedures-verified';
    return '';
  }

  /* F-194 SF-194-02 — pill « 📎 Pièces (D/O/N) ». */
  protected get showPiecesBadge(): boolean {
    if (!this.piecesBadge) return false;
    if (this.piecesBadge.kind === 'none') return false;
    const c = this.piecesBadge.counts;
    return c.aDemander + c.obtenues + c.nonApplicable > 0;
  }

  /** Formate les compteurs en `D/O/N` (À demander / Obtenues / Non applicable). */
  protected get piecesBadgeLabel(): string {
    const c = this.piecesBadge?.counts;
    if (!c) return '';
    return `${c.aDemander}/${c.obtenues}/${c.nonApplicable}`;
  }

  protected get piecesBadgeAriaLabel(): string {
    const c = this.piecesBadge?.counts;
    if (!c) return '';
    return `Pièces : ${c.aDemander} à demander, ${c.obtenues} obtenue(s), ${c.nonApplicable} non applicable(s)`;
  }

  protected get piecesBadgeTooltip(): string {
    const c = this.piecesBadge?.counts;
    if (!c) return '';
    const parts: string[] = [];
    if (c.aDemander > 0) parts.push(`${c.aDemander} à demander`);
    if (c.obtenues > 0) parts.push(`${c.obtenues} obtenue(s)`);
    if (c.nonApplicable > 0) parts.push(`${c.nonApplicable} non applicable(s)`);
    return parts.length > 0 ? `Pièces — ${parts.join(', ')}` : '';
  }

  /**
   * Couleur visuelle. Palette navy/or DESIGN_SYSTEM.md :
   *   - missing        : navy clair (priorité — pièces encore à demander)
   *   - obtained       : or plein (toutes obtenues)
   *   - partial        : or contour (mix obtenues + non applicable)
   *   - not_applicable : gris (toutes écartées)
   */
  protected get piecesBadgeClass(): string {
    if (!this.piecesBadge) return '';
    const c = this.piecesBadge.counts;
    if (c.aDemander > 0) return 'tool-card__badge--pieces-missing';
    if (c.obtenues > 0 && c.nonApplicable > 0) return 'tool-card__badge--pieces-partial';
    if (c.obtenues > 0) return 'tool-card__badge--pieces-obtained';
    if (c.nonApplicable > 0) return 'tool-card__badge--pieces-not-applicable';
    return '';
  }

  protected onClick(): void {
    if (this.disabled) {
      return;
    }
    this.open.emit();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (this.disabled) {
      return;
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.open.emit();
    }
  }
}

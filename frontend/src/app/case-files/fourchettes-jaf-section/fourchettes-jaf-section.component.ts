import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CaseAnalysisResult, JurisprudenceRange } from '../../core/models/case-analysis.model';

interface JafEntry {
  category: string;
  range: JurisprudenceRange;
}

/**
 * SF-198-05 : wrapper F-IA-04 pour l'outil F-153-fourchettes-jaf.
 *
 * Présentationnel pur — agrège les fourchettes jurisprudentielles JAF (p25/p50/p75)
 * disséminées dans `synthesis.pensionAlimentaireEstimate.jurisprudenceRange` et
 * `synthesis.prestationCompensatoireEstimate.jurisprudenceRange` (livrées par
 * F-153 SF-153-01) sous une vue "comparateur".
 */
@Component({
  selector: 'app-fourchettes-jaf-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './fourchettes-jaf-section.component.html',
  styleUrl: './fourchettes-jaf-section.component.scss',
})
export class FourchettesJafSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'FOURCHETTES JURISPRUDENTIELLES JAF';
  static readonly TOOL_ICON = 'analytics';

  /**
   * F-237 SF-237-02 : wrapper d'affichage F-IA-04 exempté du helper PrefillRules.
   * L'étiquette `PREFILL_COUNT_ALWAYS_ZERO = true` indique au test d'intégrité
   * (`prefill-count-integrity.spec.ts`) qu'il vérifie uniquement
   * `getPrefillCount({}) === 0`. Composant présentationnel pur : agrège les
   * fourchettes jurisprudentielles de `synthesis`, pas de saisie ni pré-fill IA.
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(): number {
    return 0;
  }

  @Input() synthesis: CaseAnalysisResult | null = null;
  @Input() forceExpanded = false;

  collapsed = signal(false);

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  get entries(): JafEntry[] {
    const out: JafEntry[] = [];
    const pa = this.synthesis?.pensionAlimentaireEstimate?.jurisprudenceRange;
    if (pa) out.push({ category: 'Pension alimentaire', range: pa });
    const pc = this.synthesis?.prestationCompensatoireEstimate?.jurisprudenceRange;
    if (pc) out.push({ category: 'Prestation compensatoire', range: pc });
    return out;
  }

  get hasData(): boolean {
    return this.entries.length > 0;
  }

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}

import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CaseAnalysisResult, PensionAlimentaireEstimate } from '../../core/models/case-analysis.model';

/**
 * SF-198-02 : wrapper F-IA-04 pour l'outil F-FA-02-pension-alimentaire.
 *
 * Présentationnel pur — affiche la fourchette `synthesis.pensionAlimentaireEstimate`
 * pré-calculée par le backend (`PensionAlimentaireCalculator`) lors du pipeline IA.
 * Inclut la fourchette jurisprudentielle JAF p25/p50/p75 (F-153 SF-153-01).
 *
 * Reprend le bloc `mat-expansion-panel` historique de `synthesis.component.html`
 * lignes 554-598.
 */
@Component({
  selector: 'app-pension-alimentaire-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './pension-alimentaire-section.component.html',
  styleUrl: './pension-alimentaire-section.component.scss',
})
export class PensionAlimentaireSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'PENSION ALIMENTAIRE';
  static readonly TOOL_ICON = 'family_restroom';

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

  get estimate(): PensionAlimentaireEstimate | null {
    return this.synthesis?.pensionAlimentaireEstimate ?? null;
  }

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}

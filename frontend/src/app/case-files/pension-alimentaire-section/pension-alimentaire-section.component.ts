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
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

@Component({
  selector: 'app-pension-alimentaire-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule,
    ToolJurisprudenceCitationsComponent],
  templateUrl: './pension-alimentaire-section.component.html',
  styleUrl: './pension-alimentaire-section.component.scss',
})
export class PensionAlimentaireSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-02-pension-alimentaire';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PENSION ALIMENTAIRE';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * F-237 SF-237-02 : wrapper d'affichage F-IA-04 exempté du helper PrefillRules.
   * L'étiquette `PREFILL_COUNT_ALWAYS_ZERO = true` indique au test d'intégrité
   * (`prefill-count-integrity.spec.ts`) qu'il vérifie uniquement
   * `getPrefillCount({}) === 0`. Composant présentationnel pur : affiche la
   * fourchette `synthesis.pensionAlimentaireEstimate` pré-calculée backend
   * (pas de formulaire pré-fillable).
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(): number {
    return 0;
  }

  @Input() synthesis: CaseAnalysisResult | null = null;
  @Input() forceExpanded = false;
  // F-JU-03 SF-JU-03-99c — flag standalone (mode simulateur). False = mode dossier (défaut).
  @Input() standaloneMode = false;

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

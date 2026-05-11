import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CaseAnalysisResult, PrestationCompensatoireEstimate } from '../../core/models/case-analysis.model';

/**
 * SF-198-01 : wrapper F-IA-04 pour l'outil F-FA-01-prestation-compensatoire.
 *
 * Présentationnel pur — affiche la fourchette `synthesis.prestationCompensatoireEstimate`
 * pré-calculée par le backend (`PrestationCompensatoireCalculator`) lors du pipeline IA.
 * Pas d'API ni de saisie utilisateur — l'outil restitue ce que l'IA a déjà extrait.
 *
 * Pattern miroir de `RuptureAmiableInfoSectionComponent` (composant info simple)
 * et reprend exactement la mise en page du bloc `mat-expansion-panel` historique
 * dans `synthesis.component.html` lignes 600-644.
 */
@Component({
  selector: 'app-prestation-compensatoire-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './prestation-compensatoire-section.component.html',
  styleUrl: './prestation-compensatoire-section.component.scss',
})
export class PrestationCompensatoireSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PRESTATION COMPENSATOIRE';
  static readonly TOOL_ICON = 'balance';

  /**
   * F-237 SF-237-02 : wrapper d'affichage F-IA-04 exempté du helper PrefillRules.
   * L'étiquette `PREFILL_COUNT_ALWAYS_ZERO = true` indique au test d'intégrité
   * (`prefill-count-integrity.spec.ts`) qu'il vérifie uniquement
   * `getPrefillCount({}) === 0`. Composant lecture seule : affiche la fourchette
   * `synthesis.prestationCompensatoireEstimate` pré-calculée backend.
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

  get estimate(): PrestationCompensatoireEstimate | null {
    return this.synthesis?.prestationCompensatoireEstimate ?? null;
  }

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}

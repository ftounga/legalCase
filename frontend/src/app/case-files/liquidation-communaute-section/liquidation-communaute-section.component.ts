import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CaseAnalysisResult, LiquidationCommunaute, BienItem } from '../../core/models/case-analysis.model';

interface LiquidationSection {
  titre: string;
  items: BienItem[];
}

/**
 * SF-198-03 : wrapper F-IA-04 pour l'outil F-FA-04-liquidation-communaute.
 *
 * Présentationnel pur — affiche l'inventaire `synthesis.liquidationCommunaute`
 * extrait par le pipeline IA. Reprend le bloc `mat-expansion-panel` historique
 * de `synthesis.component.html` lignes 646-682.
 */
@Component({
  selector: 'app-liquidation-communaute-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './liquidation-communaute-section.component.html',
  styleUrl: './liquidation-communaute-section.component.scss',
})
export class LiquidationCommunauteSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'LIQUIDATION DE COMMUNAUTÉ';
  static readonly TOOL_ICON = 'account_balance';

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

  get liquidation(): LiquidationCommunaute | null {
    return this.synthesis?.liquidationCommunaute ?? null;
  }

  get sections(): LiquidationSection[] {
    const l = this.liquidation;
    if (!l) return [];
    return [
      { titre: 'Actif commun',          items: l.actifCommun ?? [] },
      { titre: 'Biens propres époux A', items: l.biensPropresEpouxA ?? [] },
      { titre: 'Biens propres époux B', items: l.biensPropresEpouxB ?? [] },
      { titre: 'Passif commun',         items: l.passifCommun ?? [] },
    ];
  }

  formatRegime(regime: string | null): string {
    const labels: Record<string, string> = {
      COMMUNAUTE_LEGALE:     'Communauté légale',
      SEPARATION_BIENS:      'Séparation de biens',
      PARTICIPATION_ACQUETS: 'Participation aux acquêts',
      COMMUNAUTE_UNIVERSELLE: 'Communauté universelle',
    };
    return regime ? (labels[regime] ?? regime) : 'Non détecté';
  }

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}

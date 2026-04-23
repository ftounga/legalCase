import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ImmigrationStrategyScenario, StrategyRiskLevel } from '../../core/models/case-analysis.model';

/**
 * F-151 SF-151-01 : panneau comparatif de scenarii stratégiques immigration.
 * Affiché dans la synthèse d'un dossier quand ≥ 2 options juridiques sont ouvertes.
 */
@Component({
  selector: 'app-immigration-strategy-comparator-section',
  standalone: true,
  imports: [MatIconModule, MatExpansionModule, MatTooltipModule],
  templateUrl: './immigration-strategy-comparator-section.component.html',
  styleUrl: './immigration-strategy-comparator-section.component.scss',
})
export class ImmigrationStrategyComparatorSectionComponent {
  @Input() scenarios: ImmigrationStrategyScenario[] = [];

  riskLabel(risk: StrategyRiskLevel | null): string {
    switch (risk) {
      case 'FAIBLE': return 'Risque faible';
      case 'MOYEN':  return 'Risque moyen';
      case 'ELEVE':  return 'Risque élevé';
      default:        return 'Risque non qualifié';
    }
  }

  riskClass(risk: StrategyRiskLevel | null): string {
    switch (risk) {
      case 'FAIBLE': return 'risk-badge risk-badge--faible';
      case 'MOYEN':  return 'risk-badge risk-badge--moyen';
      case 'ELEVE':  return 'risk-badge risk-badge--eleve';
      default:        return 'risk-badge risk-badge--unknown';
    }
  }
}

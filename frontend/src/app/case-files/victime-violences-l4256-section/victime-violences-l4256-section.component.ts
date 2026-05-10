import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * F-208 — Wrapper informationnel pour l'outil décisionnel "Titre de séjour victime de violences — L.425-6 CESEDA".
 *
 * Backend complet livré dans la PR #915 (calculator/analyzer + endpoint POST/GET +
 * table victime_violences_l4256_analyses + migration Liquibase). Ce wrapper
 * frontend rappelle le cadre juridique et l'existence de l'endpoint ; le composant
 * de saisie/visualisation complet (formulaire + verdict + pré-fill IA + F-IA-03)
 * sera livré dans une SF ultérieure de F-208.
 *
 * Pattern miroir de {@code RuptureAmiableInfoSectionComponent} (composant
 * informationnel sans HTTP).
 */
@Component({
  selector: 'app-victime-violences-l4256-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './victime-violences-l4256-section.component.html',
  styleUrl: './victime-violences-l4256-section.component.scss'
})
export class VictimeViolencesL4256SectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'VICTIME VIOLENCES L.425-6';
  static readonly TOOL_ICON = 'shield';

  /**
   * F-236 SF-236-02/05 : wrapper informationnel F-208 — aucun pré-fill IA tant
   * que le composant de saisie complet n'est pas livré. Toujours 0.
   */
  static getPrefillCount(): number {
    return 0;
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;

  collapsed = signal(false);

  readonly title = 'Titre de séjour victime de violences — L.425-6 CESEDA';
  readonly description = "Outil P1 — Délivrance de plein droit d'une carte de séjour temporaire VPF aux victimes de violences conjugales / familiales (art. L.425-6+ CESEDA). Analyzer scoring 3 verdicts disponible via POST /api/v1/case-files/{id}/victime-violences-l4256-analysis.";

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }
}

import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * F-208 — Wrapper informationnel pour l'outil décisionnel "Recours contre transfert Dublin — 7 jours suspensif".
 *
 * Backend complet livré dans la PR #915 (calculator/analyzer + endpoint POST/GET +
 * table dublin_recours_analyses + migration Liquibase). Ce wrapper
 * frontend rappelle le cadre juridique et l'existence de l'endpoint ; le composant
 * de saisie/visualisation complet (formulaire + verdict + pré-fill IA + F-IA-03)
 * sera livré dans une SF ultérieure de F-208.
 *
 * Pattern miroir de {@code RuptureAmiableInfoSectionComponent} (composant
 * informationnel sans HTTP).
 */
@Component({
  selector: 'app-dublin-recours-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './dublin-recours-section.component.html',
  styleUrl: './dublin-recours-section.component.scss'
})
export class DublinRecoursSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'DUBLIN RECOURS 7J';
  static readonly TOOL_ICON = 'flight_takeoff';

  /**
   * F-237 SF-237-02 : wrapper informationnel F-208 exempté du helper PrefillRules.
   * L'étiquette `PREFILL_COUNT_ALWAYS_ZERO = true` indique au test d'intégrité
   * (`prefill-count-integrity.spec.ts`) qu'il vérifie uniquement
   * `getPrefillCount({}) === 0`. Aucun pré-fill IA possible tant que le composant
   * de saisie complet n'est pas livré.
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(): number {
    return 0;
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;

  collapsed = signal(false);

  readonly title = 'Recours contre transfert Dublin — 7 jours suspensif';
  readonly description = "Outil P1 — Délai 7 j contre la décision de transfert vers l'État membre responsable (art. L.572 CESEDA + Règlement Dublin III 604/2013). Calculator backend disponible via POST /api/v1/case-files/{id}/dublin-recours-analysis.";

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

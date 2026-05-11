import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * F-208 — Wrapper informationnel pour l'outil décisionnel "Juge des Libertés et de la Détention — Rétention administrative".
 *
 * Backend complet livré dans la PR #915 (calculator/analyzer + endpoint POST/GET +
 * table jld_retention_analyses + migration Liquibase). Ce wrapper
 * frontend rappelle le cadre juridique et l'existence de l'endpoint ; le composant
 * de saisie/visualisation complet (formulaire + verdict + pré-fill IA + F-IA-03)
 * sera livré dans une SF ultérieure de F-208.
 *
 * Pattern miroir de {@code RuptureAmiableInfoSectionComponent} (composant
 * informationnel sans HTTP).
 */
@Component({
  selector: 'app-jld-retention-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './jld-retention-section.component.html',
  styleUrl: './jld-retention-section.component.scss'
})
export class JldRetentionSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'JLD RÉTENTION 24-48H';
  static readonly TOOL_ICON = 'gavel';

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

  readonly title = 'Juge des Libertés et de la Détention — Rétention administrative';
  readonly description = "Outil P1 — Délai de saisine 48 h en cas de placement en CRA. CESEDA L. 741+ et L. 742+. Calculator backend disponible via POST /api/v1/case-files/{id}/jld-retention-analysis.";

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

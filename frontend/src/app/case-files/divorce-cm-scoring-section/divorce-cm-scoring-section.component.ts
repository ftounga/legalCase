import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { DivorceConsentementScoringSectionComponent } from '../divorce-consentement-scoring-section/divorce-consentement-scoring-section.component';

/**
 * SF-198-04 : wrapper F-IA-04 pour l'outil F-152-divorce-consentement-scoring.
 *
 * Délègue le rendu au composant existant `DivorceConsentementScoringSectionComponent`
 * (livré par F-152, présentationnel pur, signatures `[detection]` + `[scoring]`).
 *
 * Ce wrapper est nécessaire parce que le composant child n'est pas auto-suffisant
 * pour le panel F-IA-04 (qui passe `synthesis` + `caseFileId`, pas detection/scoring
 * individuellement). La migration 191 avait DELETE `F-152-divorce-consentement-scoring`
 * pour cette raison ; cette SF restaure le seed avec un wrapper conforme.
 */
@Component({
  selector: 'app-divorce-cm-scoring-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, DivorceConsentementScoringSectionComponent],
  templateUrl: './divorce-cm-scoring-section.component.html',
  styleUrl: './divorce-cm-scoring-section.component.scss',
})
export class DivorceCmScoringSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'VALIDITÉ DIVORCE CONSENTEMENT MUTUEL';
  static readonly TOOL_ICON = 'verified';

  /**
   * F-237 SF-237-02 : wrapper d'affichage F-IA-04 exempté du helper PrefillRules.
   * L'étiquette `PREFILL_COUNT_ALWAYS_ZERO = true` indique au test d'intégrité
   * (`prefill-count-integrity.spec.ts`) qu'il vérifie uniquement
   * `getPrefillCount({}) === 0`. Le composant délègue le rendu au composant
   * `DivorceConsentementScoringSectionComponent` (présentationnel pur, pas de
   * pré-fill IA propre — le scoring vient de `synthesis`).
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

  get detection() {
    return this.synthesis?.divorceConsentementValidityDetection ?? null;
  }

  get scoring() {
    return this.synthesis?.divorceConsentementScoring ?? null;
  }

  get hasData(): boolean {
    return !!this.detection && !!this.scoring;
  }
}

import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { AnalysisJobType } from '../../core/services/analysis-sse.service';

const LABELS: Record<AnalysisJobType, string> = {
  CASE_ANALYSIS: 'Analyse du dossier en cours…',
  ENRICHED_ANALYSIS: 'Re-synthèse enrichie en cours…',
  DOCUMENT_ANALYSIS: 'Analyse des documents en cours…',
  // F-185 SF-185-02 : Q&A async — affiché si plusieurs jobs en parallèle.
  QUESTION_GENERATION: 'Génération des questions complémentaires…',
};

@Component({
  selector: 'app-decisional-tools-progress-banner',
  standalone: true,
  imports: [MatProgressBarModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (jobs().length > 0) {
      <div class="banner" role="status" aria-live="polite">
        <mat-icon class="banner__icon">auto_awesome</mat-icon>
        <span class="banner__label">{{ label() }}</span>
        <mat-progress-bar mode="indeterminate" class="banner__bar"></mat-progress-bar>
      </div>
    }
  `,
  styleUrl: './decisional-tools-progress-banner.component.scss',
})
export class DecisionalToolsProgressBannerComponent {
  private readonly jobsSignal = signal<AnalysisJobType[]>([]);

  @Input()
  set activeJobTypes(value: AnalysisJobType[]) {
    this.jobsSignal.set(value ?? []);
  }

  protected readonly jobs = this.jobsSignal.asReadonly();

  protected readonly label = computed<string>(() => {
    const list = this.jobs();
    if (list.length === 0) return '';
    if (list.length === 1) return LABELS[list[0]];
    return `Analyses en cours… (${list.length})`;
  });
}

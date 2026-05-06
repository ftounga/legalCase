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

const SECTION_AWARE_JOBS: ReadonlySet<AnalysisJobType> = new Set<AnalysisJobType>([
  'CASE_ANALYSIS',
  'ENRICHED_ANALYSIS',
]);

@Component({
  selector: 'app-decisional-tools-progress-banner',
  standalone: true,
  imports: [MatProgressBarModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (jobs().length > 0) {
      <div class="banner" role="status" aria-live="polite">
        <mat-icon class="banner__icon">auto_awesome</mat-icon>
        <div class="banner__text">
          <span class="banner__label">{{ label() }}</span>
          @if (sectionsLine(); as line) {
            <span class="banner__sections">{{ line }}</span>
          }
        </div>
        <mat-progress-bar mode="indeterminate" class="banner__bar"></mat-progress-bar>
      </div>
    }
  `,
  styleUrl: './decisional-tools-progress-banner.component.scss',
})
export class DecisionalToolsProgressBannerComponent {
  private readonly jobsSignal = signal<AnalysisJobType[]>([]);
  private readonly sectionsReceivedSignal = signal<number | null>(null);
  private readonly sectionsExpectedSignal = signal<number>(0);

  @Input()
  set activeJobTypes(value: AnalysisJobType[]) {
    this.jobsSignal.set(value ?? []);
  }

  /**
   * F-190 SF-190-03 — nombre de sections reçues via le streaming PARTIAL.
   * `null` masque la sous-ligne (pas de partial encore disponible).
   */
  @Input()
  set sectionsReceived(value: number | null) {
    this.sectionsReceivedSignal.set(value);
  }

  /**
   * F-190 SF-190-03 — total attendu (7 dans la version actuelle). 0 masque.
   */
  @Input()
  set sectionsExpected(value: number) {
    this.sectionsExpectedSignal.set(value ?? 0);
  }

  protected readonly jobs = this.jobsSignal.asReadonly();

  protected readonly label = computed<string>(() => {
    const list = this.jobs();
    if (list.length === 0) return '';
    if (list.length === 1) return LABELS[list[0]];
    return `Analyses en cours… (${list.length})`;
  });

  protected readonly sectionsLine = computed<string | null>(() => {
    const received = this.sectionsReceivedSignal();
    const expected = this.sectionsExpectedSignal();
    if (received === null || expected <= 0) return null;
    const list = this.jobs();
    const hasSectionAwareJob = list.some(job => SECTION_AWARE_JOBS.has(job));
    if (!hasSectionAwareJob) return null;
    return `${received}/${expected} sections reçues`;
  });
}

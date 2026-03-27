import { Component, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseFile } from '../../core/models/case-file.model';
import { AnalysisDiff, CaseAnalysisVersionSummary, SectionDiff, TimelineSectionDiff } from '../../core/models/case-analysis.model';

interface DiffSection {
  title: string;
  icon: string;
  data: SectionDiff | null;
  timeline?: TimelineSectionDiff;
}

@Component({
  selector: 'app-analysis-diff',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './analysis-diff.component.html',
  styleUrl: './analysis-diff.component.scss'
})
export class AnalysisDiffComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  versions = signal<CaseAnalysisVersionSummary[]>([]);
  diff = signal<AnalysisDiff | null>(null);
  loading = signal(true);
  diffLoading = signal(false);

  fromId = signal<string | null>(null);
  toId = signal<string | null>(null);

  readonly canCompute = computed(() => {
    const f = this.fromId();
    const t = this.toId();
    return !!f && !!t && f !== t;
  });

  readonly isOrderReversed = computed(() => {
    const f = this.fromId();
    const t = this.toId();
    if (!f || !t || f === t) return false;
    const fromV = this.versions().find(v => v.id === f);
    const toV   = this.versions().find(v => v.id === t);
    if (!fromV || !toV) return false;
    return fromV.version > toV.version;
  });

  readonly totalAdded = computed(() => {
    const d = this.diff();
    if (!d) return 0;
    return d.faits.added.length + d.pointsJuridiques.added.length +
           d.risques.added.length + d.questionsOuvertes.added.length +
           d.timeline.added.length;
  });

  readonly totalRemoved = computed(() => {
    const d = this.diff();
    if (!d) return 0;
    return d.faits.removed.length + d.pointsJuridiques.removed.length +
           d.risques.removed.length + d.questionsOuvertes.removed.length +
           d.timeline.removed.length;
  });

  readonly totalEnriched = computed(() => {
    const d = this.diff();
    if (!d) return 0;
    return d.faits.enriched.length + d.pointsJuridiques.enriched.length +
           d.risques.enriched.length + d.questionsOuvertes.enriched.length +
           d.timeline.enriched.length;
  });

  readonly sections = computed((): DiffSection[] => {
    const d = this.diff();
    if (!d) return [];
    return [
      { title: 'Faits', icon: 'gavel', data: d.faits },
      { title: 'Points juridiques', icon: 'balance', data: d.pointsJuridiques },
      { title: 'Risques', icon: 'warning_amber', data: d.risques },
      { title: 'Questions ouvertes', icon: 'help_outline', data: d.questionsOuvertes },
      { title: 'Chronologie', icon: 'timeline', data: null, timeline: d.timeline },
    ];
  });

  private caseFileId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private caseFileService: CaseFileService,
    private caseAnalysisService: CaseAnalysisService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.caseFileId = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(this.caseFileId).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/case-files']);
      }
    });
    this.caseAnalysisService.getVersions(this.caseFileId).subscribe({
      next: v => this.versions.set(v),
      error: () => {
        this.snackBar.open('Impossible de charger les versions', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  onVersionChange(): void {
    if (!this.canCompute()) return;
    this.loadDiff();
  }

  private loadDiff(): void {
    const fromId = this.fromId()!;
    const toId = this.toId()!;
    this.diffLoading.set(true);
    this.diff.set(null);
    this.caseAnalysisService.getDiff(this.caseFileId, fromId, toId).subscribe({
      next: d => {
        this.diff.set(d);
        this.diffLoading.set(false);
      },
      error: () => {
        this.diffLoading.set(false);
        this.snackBar.open('Erreur lors du calcul du diff', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  versionLabel(v: CaseAnalysisVersionSummary): string {
    const type = v.analysisType === 'ENRICHED' ? 'Enrichie' : 'Standard';
    const date = new Date(v.updatedAt).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
    return `v${v.version} — ${type} — ${date}`;
  }

  totalItems(section: DiffSection): number {
    if (section.timeline) {
      const t = section.timeline;
      return t.added.length + t.removed.length + t.unchanged.length + t.enriched.length;
    }
    const d = section.data!;
    return d.added.length + d.removed.length + d.unchanged.length + d.enriched.length;
  }

  sectionAddedCount(section: DiffSection): number {
    return section.timeline
      ? section.timeline.added.length
      : section.data!.added.length;
  }

  sectionRemovedCount(section: DiffSection): number {
    return section.timeline
      ? section.timeline.removed.length
      : section.data!.removed.length;
  }

  sectionEnrichedCount(section: DiffSection): number {
    return section.timeline
      ? section.timeline.enriched.length
      : section.data!.enriched.length;
  }

  goBack(): void {
    this.router.navigate(['/case-files', this.caseFileId]);
  }
}

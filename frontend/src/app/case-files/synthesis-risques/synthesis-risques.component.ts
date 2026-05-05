import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseAnalysisResult, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { SourceRefComponent } from '../../shared/source-ref/source-ref.component';

/**
 * F-162 SF-162-05 — page Risques dédiée. Cards stylées selon le `riskLevel`
 * global de l'analyse (le modèle actuel ne donne pas de gravité par item).
 */
@Component({
  selector: 'app-synthesis-risques',
  standalone: true,
  imports: [
    RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule,
    SourceRefComponent,
  ],
  templateUrl: './synthesis-risques.component.html',
  styleUrl: './synthesis-risques.component.scss',
})
export class SynthesisRisquesComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  loading = signal(true);

  private readonly sourceMap = computed(() => {
    const map = new Map<string, string>();
    const docs = this.synthesis()?.analysisDocuments;
    if (!docs) return map;
    for (const doc of docs) map.set(`Document ${doc.index}`, doc.name);
    return map;
  });

  constructor(
    private route: ActivatedRoute,
    private caseFileService: CaseFileService,
    private caseAnalysisService: CaseAnalysisService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    const versionParam = this.route.snapshot.queryParamMap.get('version');

    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loadAnalysis(id, versionParam ? Number(versionParam) : null);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadAnalysis(caseFileId: string, requestedVersion: number | null): void {
    this.caseAnalysisService.getVersions(caseFileId).subscribe({
      next: versions => {
        if (versions.length === 0) {
          this.loading.set(false);
          return;
        }
        const target = this.pickVersion(versions, requestedVersion);
        this.caseAnalysisService.getByVersion(caseFileId, target.version).subscribe({
          next: result => {
            this.synthesis.set(result);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      },
      error: () => this.loading.set(false),
    });
  }

  private pickVersion(
    versions: CaseAnalysisVersionSummary[],
    requested: number | null,
  ): CaseAnalysisVersionSummary {
    if (requested != null && !Number.isNaN(requested)) {
      const match = versions.find(v => v.version === requested);
      if (match) return match;
    }
    return versions[0];
  }

  /** F-162 SF-162-05 — classe CSS calquée sur le riskLevel global de l'analyse. */
  gravityClass(level: string | null | undefined): string {
    switch (level) {
      case 'FAIBLE': return 'risque-card--faible';
      case 'MOYEN':  return 'risque-card--moyen';
      case 'ELEVE':  return 'risque-card--eleve';
      default:       return '';
    }
  }

  riskLabel(level: string | null | undefined): string {
    const labels: Record<string, string> = { FAIBLE: 'Faible', MOYEN: 'Moyen', ELEVE: 'Élevé' };
    if (!level) return '';
    return labels[level] ?? level;
  }

  resolveSource(source: string | null): string | null {
    if (!source) return null;
    if (/^Document \d+$/i.test(source)) {
      return this.sourceMap().get(source) ?? source;
    }
    return source;
  }
}

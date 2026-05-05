import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseAnalysisResult, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';

/**
 * F-162 SF-162-02 — page Timeline dédiée. Vue horizontale (rail) des événements
 * chronologiques détectés par l'IA. Sert de patron canonique pour SF-162-03/04/05.
 */
@Component({
  selector: 'app-synthesis-timeline',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './synthesis-timeline.component.html',
  styleUrl: './synthesis-timeline.component.scss',
})
export class SynthesisTimelineComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  loading = signal(true);

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
}

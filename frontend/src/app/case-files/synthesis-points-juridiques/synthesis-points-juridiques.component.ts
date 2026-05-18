import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { JurisprudenceCitationService } from '../../core/services/jurisprudence-citation.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseAnalysisResult, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { JurisprudenceCitation } from '../../core/models/jurisprudence-citation.model';
import { SourceRefComponent } from '../../shared/source-ref/source-ref.component';
import { JurisprudenceDeeplinksComponent } from '../../shared/jurisprudence-deeplinks/jurisprudence-deeplinks.component';
import { JurisprudenceAppuiComponent } from '../jurisprudence-appui/jurisprudence-appui.component';

const PREVIEW_MAX_CHARS = 200;

/**
 * F-162 SF-162-04 — page Points juridiques dédiée. Cards expandables : preview
 * 200 chars par défaut, intégrale au clic. Patron `.detail-page` SF-162-02.
 */
@Component({
  selector: 'app-synthesis-points-juridiques',
  standalone: true,
  imports: [
    RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule,
    SourceRefComponent, JurisprudenceDeeplinksComponent, JurisprudenceAppuiComponent,
  ],
  templateUrl: './synthesis-points-juridiques.component.html',
  styleUrl: './synthesis-points-juridiques.component.scss',
})
export class SynthesisPointsJuridiquesComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  loading = signal(true);
  expandedIds = signal<Set<number>>(new Set());
  workspaceCountry = signal<'FR' | 'BE'>('FR');
  /** F-242 SF-242-02 — citations d'appui du dossier, réparties par index de point. */
  citationsByPoint = signal<Map<number, JurisprudenceCitation[]>>(new Map());

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
    private workspaceService: WorkspaceService,
    private jurisprudenceCitationService: JurisprudenceCitationService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    const versionParam = this.route.snapshot.queryParamMap.get('version');

    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => this.workspaceCountry.set(ws.country === 'BELGIQUE' ? 'BE' : 'FR'),
      error: () => this.workspaceCountry.set('FR'),
    });

    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loadAnalysis(id, versionParam ? Number(versionParam) : null);
        this.loadCitations(id);
      },
      error: () => this.loading.set(false),
    });
  }

  /**
   * F-242 SF-242-02 — charge les citations d'appui du dossier et les répartit
   * par `pointJuridiqueIndex`. Un échec laisse la map vide (zone repliée).
   */
  private loadCitations(caseFileId: string): void {
    this.jurisprudenceCitationService.list(caseFileId).subscribe({
      next: response => this.citationsByPoint.set(this.groupByPoint(response.citations)),
      error: () => this.citationsByPoint.set(new Map()),
    });
  }

  /** Regroupe les citations par index de point juridique. */
  private groupByPoint(
    citations: JurisprudenceCitation[],
  ): Map<number, JurisprudenceCitation[]> {
    const map = new Map<number, JurisprudenceCitation[]>();
    for (const citation of citations) {
      const list = map.get(citation.pointJuridiqueIndex) ?? [];
      list.push(citation);
      map.set(citation.pointJuridiqueIndex, list);
    }
    return map;
  }

  /** Citations d'appui rattachées au point juridique d'index donné. */
  citationsForPoint(index: number): JurisprudenceCitation[] {
    return this.citationsByPoint().get(index) ?? [];
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

  isLong(text: string): boolean {
    return (text?.length ?? 0) > PREVIEW_MAX_CHARS;
  }

  preview(text: string): string {
    if (!this.isLong(text)) return text;
    return text.slice(0, PREVIEW_MAX_CHARS).trimEnd() + '…';
  }

  isExpanded(index: number): boolean {
    return this.expandedIds().has(index);
  }

  toggle(index: number): void {
    this.expandedIds.update(set => {
      const next = new Set(set);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  }

  resolveSource(source: string | null): string | null {
    if (!source) return null;
    if (/^Document \d+$/i.test(source)) {
      return this.sourceMap().get(source) ?? source;
    }
    return source;
  }
}

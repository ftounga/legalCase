import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseFile } from '../../core/models/case-file.model';
import { AnalysisItem, CaseAnalysisResult, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { SourceRefComponent } from '../../shared/source-ref/source-ref.component';

interface FaitsGroup {
  theme: string;
  items: AnalysisItem[];
}

/**
 * F-162 SF-162-03 — page Faits dédiée. Vue groupée par thème (heuristique côté
 * client : préfixe avant ":" ou "Autres"). Réutilise le patron `.detail-page`
 * posé en SF-162-02.
 */
@Component({
  selector: 'app-synthesis-faits',
  standalone: true,
  imports: [
    RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule,
    SourceRefComponent,
  ],
  templateUrl: './synthesis-faits.component.html',
  styleUrl: './synthesis-faits.component.scss',
})
export class SynthesisFaitsComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  loading = signal(true);

  /** Map "Document N" → nom de fichier, alimentée à partir de `analysisDocuments`. */
  private readonly sourceMap = computed(() => {
    const map = new Map<string, string>();
    const docs = this.synthesis()?.analysisDocuments;
    if (!docs) return map;
    for (const doc of docs) map.set(`Document ${doc.index}`, doc.name);
    return map;
  });

  /** F-162 SF-162-03 — groupement client par thème détecté avant le ":". */
  readonly groupedFaits = computed<FaitsGroup[]>(() => {
    const faits = this.synthesis()?.faits ?? [];
    if (faits.length === 0) return [];
    const buckets = new Map<string, AnalysisItem[]>();
    for (const f of faits) {
      const theme = SynthesisFaitsComponent.detectTheme(f.texte);
      const list = buckets.get(theme);
      if (list) list.push(f);
      else buckets.set(theme, [f]);
    }
    return [...buckets.entries()].map(([theme, items]) => ({ theme, items }));
  });

  /**
   * Heuristique simple : si le texte commence par un mot capitalisé suivi
   * de `:` (ex. "Procédure : ...") on prend ce mot comme thème, sinon
   * "Autres". On accepte aussi un thème multi-mots avant le `:` (ex.
   * "Conditions de travail : ...").
   */
  static detectTheme(texte: string): string {
    const trimmed = texte.trim();
    const colon = trimmed.indexOf(':');
    if (colon < 1 || colon > 60) return 'Autres';
    const head = trimmed.slice(0, colon).trim();
    if (!head || !/^[A-ZÀ-Ý]/.test(head)) return 'Autres';
    return head;
  }

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

  resolveSource(source: string | null): string | null {
    if (!source) return null;
    if (/^Document \d+$/i.test(source)) {
      return this.sourceMap().get(source) ?? source;
    }
    return source;
  }
}

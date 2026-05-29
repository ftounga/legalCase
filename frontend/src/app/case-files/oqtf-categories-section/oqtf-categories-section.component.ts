import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { OqtfCategoriesService } from '../../core/services/oqtf-categories.service';
import {
  CategorieL611,
  OqtfCategoriesRequest,
  OqtfCategoriesResponse,
} from '../../core/models/oqtf-categories.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { OqtfCategoriesPrefillRules } from './oqtf-categories-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-10 — Outil décisionnel « OQTF catégories L. 611-1 » (F-IM-29).
 *
 * FR uniquement (CESEDA L. 611-1, 1° à 7°). Pour la catégorie choisie, affiche
 * les moyens de défense spécifiques, la base juridique, le délai de recours et
 * (CAT_7 = remise à un État membre / Dublin) un renvoi vers F-IM-22.
 * Pattern miroir : {@link RegroupementFamilialSectionComponent} (F-IM-26).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - pré-fill IA (dateNotificationOqtf, motifOqtf depuis motifOqtfCode)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-oqtf-categories-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    RouterModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './oqtf-categories-section.component.html',
  styleUrl: './oqtf-categories-section.component.scss',
})
export class OqtfCategoriesSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-29-oqtf-categories-l6111-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'OQTF CATÉGORIES L.611-1 (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return OqtfCategoriesPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfCategoriesResponse | null>(null);

  categorieL611 = signal<CategorieL611 | null>(null);
  dateNotificationOqtf = signal<string | null>(null);
  motifOqtf = signal<string | null>(null);

  provenanceDate = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);

  readonly categorieOptions: ReadonlyArray<{ code: CategorieL611; label: string }> = [
    { code: 'CAT_1', label: '1° — Refus de titre / d\'asile ou retrait de titre' },
    { code: 'CAT_2', label: '2° — Titre de séjour expiré ou non renouvelé' },
    { code: 'CAT_3', label: '3° — Entrée ou séjour irrégulier' },
    { code: 'CAT_4', label: '4° — Refus de délivrance d\'un premier titre' },
    { code: 'CAT_5', label: '5° — Comportement menaçant l\'ordre public' },
    { code: 'CAT_6', label: '6° — Activité professionnelle sans autorisation' },
    { code: 'CAT_7', label: '7° — Remise à un autre État membre (Dublin)' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: OqtfCategoriesService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isFrance() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    if (!this.categorieL611()) return false;
    const date = this.dateNotificationOqtf();
    if (!date || !OqtfCategoriesPrefillRules.ISO_DATE_RE.test(date)) return false;
    const motif = this.motifOqtf();
    if (motif !== null && motif.length > 300) return false;
    return true;
  }

  onCategorieChange(value: CategorieL611 | null): void {
    this.categorieL611.set(value);
  }

  onDateChange(value: string | null): void {
    this.dateNotificationOqtf.set(value);
    this.provenanceDate.set(null);
  }

  onMotifChange(value: string | null): void {
    this.motifOqtf.set(value);
    this.provenanceMotif.set(null);
  }

  isCat7(): boolean {
    return this.result()?.categorieL611 === 'CAT_7';
  }

  categorieLabel(code: CategorieL611 | null | undefined): string {
    const opt = this.categorieOptions.find((o) => o.code === code);
    return opt ? opt.label : '';
  }

  analyze(): void {
    if (!this.formValid()) return;
    const motif = this.motifOqtf();
    const request: OqtfCategoriesRequest = {
      categorieL611: this.categorieL611()!,
      dateNotificationOqtf: this.dateNotificationOqtf()!,
      motifOqtf: motif && motif.trim().length > 0 ? motif.trim() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse OQTF catégories enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.dashboardRefresh?.triggerRefresh();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
        this.cdr.markForCheck();
      },
    });
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const date = OqtfCategoriesPrefillRules.computeDateNotificationOqtf(input);
    if (date !== null && this.dateNotificationOqtf() === null) {
      this.dateNotificationOqtf.set(date);
      this.provenanceDate.set('IA');
    }

    const motif = OqtfCategoriesPrefillRules.computeMotifOqtf(input);
    if (motif !== null && (this.motifOqtf() === null || this.motifOqtf() === '')) {
      this.motifOqtf.set(motif);
      this.provenanceMotif.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.categorieL611.set(r.categorieL611 ?? null);
        this.dateNotificationOqtf.set(r.dateNotificationOqtf ?? null);
        this.motifOqtf.set(r.motifOqtf ?? null);
        this.provenanceDate.set(null);
        this.provenanceMotif.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 = pas encore d'analyse, on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}

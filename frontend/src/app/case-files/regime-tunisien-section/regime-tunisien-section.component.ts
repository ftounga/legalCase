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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RegimeTunisienService } from '../../core/services/regime-tunisien.service';
import {
  RegimeTunisienRequest,
  RegimeTunisienResponse,
  CategorieRegimeTunisien,
  RegimeTunisien,
} from '../../core/models/regime-tunisien.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RegimeTunisienPrefillRules } from './regime-tunisien-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-220-01 — Outil décisionnel « Régime franco-tunisien (accord 17/03/1988) » (F-IM-47).
 *
 * FR uniquement. Aiguille un ressortissant tunisien : droit commun CESEDA SAUF
 * particularités dérogatoires de l'accord 1988 (étudiant, commerçant, salarié).
 * Distinct de F-IM-17 (régime algérien fermé) — la Tunisie reste largement
 * renvoyée au CESEDA. Pattern miroir : {@link RegroupementFamilialSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or Immigration
 *  - pré-fill IA (catégorie, durée séjour, titre en cours)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)</p>
 */
@Component({
  selector: 'app-regime-tunisien-section',
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
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './regime-tunisien-section.component.html',
  styleUrl: './regime-tunisien-section.component.scss',
})
export class RegimeTunisienSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-47-regime-tunisien-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RÉGIME FRANCO-TUNISIEN — ACCORD 17/03/1988 (FR)';
  static readonly TOOL_ICON = 'public';

  static getPrefillCount(input: PrefillCountInput): number {
    return RegimeTunisienPrefillRules.computePrefillCount(input);
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
  result = signal<RegimeTunisienResponse | null>(null);

  categorie = signal<CategorieRegimeTunisien | null>(null);
  dureeSejourEnvisageeMois = signal<number | null>(null);
  titreEnCours = signal<boolean>(false);
  dejaResident = signal<boolean>(false);

  provenanceCategorie = signal<'IA' | null>(null);
  provenanceDuree = signal<'IA' | null>(null);
  provenanceTitre = signal<'IA' | null>(null);

  readonly categorieOptions: ReadonlyArray<{ code: CategorieRegimeTunisien; label: string }> = [
    { code: 'ETUDIANT', label: 'Étudiant' },
    { code: 'COMMERCANT', label: 'Commerçant' },
    { code: 'SALARIE', label: 'Salarié' },
    { code: 'FAMILIAL', label: 'Familial' },
    { code: 'AUTRE', label: 'Autre' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: RegimeTunisienService,
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
    if (!this.categorie()) return false;
    const duree = this.dureeSejourEnvisageeMois();
    if (duree !== null && duree < 0) return false;
    return true;
  }

  onCategorieChange(value: CategorieRegimeTunisien | null): void {
    this.categorie.set(value);
    this.provenanceCategorie.set(null);
  }

  onDureeChange(value: number | null): void {
    this.dureeSejourEnvisageeMois.set(value);
    this.provenanceDuree.set(null);
  }

  onTitreChange(value: boolean): void {
    this.titreEnCours.set(value);
    this.provenanceTitre.set(null);
  }

  onResidentChange(value: boolean): void {
    this.dejaResident.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RegimeTunisienRequest = {
      categorie: this.categorie()!,
      dureeSejourEnvisageeMois: this.dureeSejourEnvisageeMois(),
      titreEnCours: this.titreEnCours(),
      dejaResident: this.dejaResident(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse régime franco-tunisien enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(regime: RegimeTunisien | null | undefined): string {
    switch (regime) {
      case 'ACCORD_1988_DEROGATOIRE': return 'rt-banner rt-banner--info';
      case 'MIXTE':                   return 'rt-banner rt-banner--warning';
      case 'DROIT_COMMUN_CESEDA':     return 'rt-banner rt-banner--neutral';
      default:                        return 'rt-banner';
    }
  }

  bannerIcon(regime: RegimeTunisien | null | undefined): string {
    switch (regime) {
      case 'ACCORD_1988_DEROGATOIRE': return 'gavel';
      case 'MIXTE':                   return 'call_split';
      case 'DROIT_COMMUN_CESEDA':     return 'menu_book';
      default:                        return 'info_outline';
    }
  }

  regimeLabel(regime: RegimeTunisien | null | undefined): string {
    switch (regime) {
      case 'ACCORD_1988_DEROGATOIRE':
        return 'Particularité dérogatoire de l\'accord franco-tunisien de 1988';
      case 'MIXTE':
        return 'Régime mixte — particularité accord 1988 + droit commun CESEDA';
      case 'DROIT_COMMUN_CESEDA':
        return 'Droit commun CESEDA (l\'accord 1988 ne déroge pas)';
      default:
        return '';
    }
  }

  categorieLabel(code: CategorieRegimeTunisien | null | undefined): string {
    switch (code) {
      case 'ETUDIANT':   return 'Étudiant';
      case 'COMMERCANT': return 'Commerçant';
      case 'SALARIE':    return 'Salarié';
      case 'FAMILIAL':   return 'Familial';
      case 'AUTRE':      return 'Autre';
      default:           return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const cat = RegimeTunisienPrefillRules.computeCategorie(input);
    if (cat !== null && !this.categorie()) {
      this.categorie.set(cat);
      this.provenanceCategorie.set('IA');
    }

    const duree = RegimeTunisienPrefillRules.computeDureeSejourEnvisageeMois(input);
    if (duree !== null && this.dureeSejourEnvisageeMois() === null) {
      this.dureeSejourEnvisageeMois.set(duree);
      this.provenanceDuree.set('IA');
    }

    const titre = RegimeTunisienPrefillRules.computeTitreEnCours(input);
    if (titre !== null && !this.titreEnCours()) {
      this.titreEnCours.set(titre);
      this.provenanceTitre.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.categorie.set(r.categorie ?? null);
        this.dureeSejourEnvisageeMois.set(r.dureeSejourEnvisageeMois ?? null);
        this.titreEnCours.set(r.titreEnCours ?? false);
        this.dejaResident.set(r.dejaResident ?? false);
        this.provenanceCategorie.set(null);
        this.provenanceDuree.set(null);
        this.provenanceTitre.set(null);
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

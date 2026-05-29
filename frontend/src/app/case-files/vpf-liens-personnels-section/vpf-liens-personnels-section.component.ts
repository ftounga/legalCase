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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VpfLiensPersonnelsService } from '../../core/services/vpf-liens-personnels.service';
import {
  NiveauIntegration,
  VpfLiensPersonnelsRequest,
  VpfLiensPersonnelsResponse,
  VerdictVpfLiensPersonnels,
} from '../../core/models/vpf-liens-personnels.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VpfLiensPersonnelsPrefillRules } from './vpf-liens-personnels-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-06 — Outil décisionnel « Vie privée et familiale — liens personnels
 * L.423-23 CESEDA » (F-IM-27-vpf-liens-personnels-l42323-fr).
 *
 * FR uniquement (CESEDA L.423-23 — scoring d'éligibilité au titre VPF :
 * durée de résidence, attaches familiales, intégration). Pattern miroir :
 * {@link RegroupementFamilialSectionComponent} (F-IM-26).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert ELIGIBLE_PROBABLE, orange
 *    ELIGIBLE_SOUS_RESERVE / DOSSIER_A_CONSOLIDER, rouge NON_ELIGIBLE
 *  - scoring 0-100 en barre de progression (mat-progress-bar)
 *  - pré-fill IA (durée résidence, minorité à l'entrée, enfants, intégration)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-vpf-liens-personnels-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './vpf-liens-personnels-section.component.html',
  styleUrl: './vpf-liens-personnels-section.component.scss',
})
export class VpfLiensPersonnelsSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-27-vpf-liens-personnels-l42323-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VPF LIENS PERSONNELS L.423-23 (FR)';
  static readonly TOOL_ICON = 'diversity_3';

  static getPrefillCount(input: PrefillCountInput): number {
    return VpfLiensPersonnelsPrefillRules.computePrefillCount(input);
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
  result = signal<VpfLiensPersonnelsResponse | null>(null);

  dureeResidenceFranceMois = signal<number | null>(null);
  entreeEnFranceMineur = signal<boolean>(false);
  enfantsEnFrance = signal<boolean>(false);
  conjointEnFrance = signal<boolean>(false);
  parentsEnFrance = signal<boolean>(false);
  situationFamilialeAlEtranger = signal<string | null>(null);
  niveauIntegration = signal<NiveauIntegration | null>(null);
  ancienneConvictionPenale = signal<boolean>(false);

  provenanceDuree = signal<'IA' | null>(null);
  provenanceMineur = signal<'IA' | null>(null);
  provenanceEnfants = signal<'IA' | null>(null);
  provenanceNiveau = signal<'IA' | null>(null);

  readonly niveauOptions: ReadonlyArray<{ code: NiveauIntegration; label: string }> = [
    { code: 'FORT', label: 'Fort' },
    { code: 'MOYEN', label: 'Moyen' },
    { code: 'FAIBLE', label: 'Faible' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: VpfLiensPersonnelsService,
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
    const duree = this.dureeResidenceFranceMois();
    if (duree === null || duree < 0) return false;
    if (!this.niveauIntegration()) return false;
    return true;
  }

  onDureeChange(value: number | null): void {
    this.dureeResidenceFranceMois.set(value);
    this.provenanceDuree.set(null);
  }

  onMineurChange(value: boolean): void {
    this.entreeEnFranceMineur.set(value);
    this.provenanceMineur.set(null);
  }

  onEnfantsChange(value: boolean): void {
    this.enfantsEnFrance.set(value);
    this.provenanceEnfants.set(null);
  }

  onConjointChange(value: boolean): void {
    this.conjointEnFrance.set(value);
  }

  onParentsChange(value: boolean): void {
    this.parentsEnFrance.set(value);
  }

  onSituationChange(value: string | null): void {
    this.situationFamilialeAlEtranger.set(value);
  }

  onNiveauChange(value: NiveauIntegration | null): void {
    this.niveauIntegration.set(value);
    this.provenanceNiveau.set(null);
  }

  onConvictionChange(value: boolean): void {
    this.ancienneConvictionPenale.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const situation = this.situationFamilialeAlEtranger();
    const request: VpfLiensPersonnelsRequest = {
      dureeResidenceFranceMois: this.dureeResidenceFranceMois()!,
      entreeEnFranceMineur: this.entreeEnFranceMineur(),
      enfantsEnFrance: this.enfantsEnFrance(),
      conjointEnFrance: this.conjointEnFrance(),
      parentsEnFrance: this.parentsEnFrance(),
      situationFamilialeAlEtranger: situation && situation.trim().length > 0 ? situation.trim() : null,
      niveauIntegration: this.niveauIntegration()!,
      ancienneConvictionPenale: this.ancienneConvictionPenale(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse VPF liens personnels enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(verdict: VerdictVpfLiensPersonnels | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'vpf-banner vpf-banner--success';
      case 'ELIGIBLE_SOUS_RESERVE':
      case 'DOSSIER_A_CONSOLIDER':  return 'vpf-banner vpf-banner--warning';
      case 'NON_ELIGIBLE':          return 'vpf-banner vpf-banner--danger';
      default:                      return 'vpf-banner';
    }
  }

  bannerIcon(verdict: VerdictVpfLiensPersonnels | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'check_circle';
      case 'ELIGIBLE_SOUS_RESERVE':
      case 'DOSSIER_A_CONSOLIDER':  return 'warning';
      case 'NON_ELIGIBLE':          return 'error';
      default:                      return 'info_outline';
    }
  }

  verdictLabel(verdict: VerdictVpfLiensPersonnels | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'Éligibilité probable au titre VPF';
      case 'ELIGIBLE_SOUS_RESERVE': return 'Éligible sous réserve';
      case 'NON_ELIGIBLE':          return 'Non éligible en l\'état';
      case 'DOSSIER_A_CONSOLIDER':  return 'Dossier à consolider';
      default:                      return '';
    }
  }

  scoreBarColor(verdict: VerdictVpfLiensPersonnels | null | undefined): 'primary' | 'accent' | 'warn' {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'primary';
      case 'NON_ELIGIBLE':          return 'warn';
      default:                      return 'accent';
    }
  }

  niveauLabel(niveau: NiveauIntegration | null | undefined): string {
    switch (niveau) {
      case 'FORT':   return 'Fort';
      case 'MOYEN':  return 'Moyen';
      case 'FAIBLE': return 'Faible';
      default:       return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const duree = VpfLiensPersonnelsPrefillRules.computeDureeResidenceFranceMois(input);
    if (duree !== null && this.dureeResidenceFranceMois() === null) {
      this.dureeResidenceFranceMois.set(duree);
      this.provenanceDuree.set('IA');
    }

    const mineur = VpfLiensPersonnelsPrefillRules.computeEntreeEnFranceMineur(input);
    if (mineur === true && !this.entreeEnFranceMineur()) {
      this.entreeEnFranceMineur.set(true);
      this.provenanceMineur.set('IA');
    }

    const enfants = VpfLiensPersonnelsPrefillRules.computeEnfantsEnFrance(input);
    if (enfants === true && !this.enfantsEnFrance()) {
      this.enfantsEnFrance.set(true);
      this.provenanceEnfants.set('IA');
    }

    const niveau = VpfLiensPersonnelsPrefillRules.computeNiveauIntegration(input);
    if (niveau !== null && !this.niveauIntegration()) {
      this.niveauIntegration.set(niveau);
      this.provenanceNiveau.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dureeResidenceFranceMois.set(r.dureeResidenceFranceMois ?? null);
        this.entreeEnFranceMineur.set(r.entreeEnFranceMineur ?? false);
        this.enfantsEnFrance.set(r.enfantsEnFrance ?? false);
        this.conjointEnFrance.set(r.conjointEnFrance ?? false);
        this.parentsEnFrance.set(r.parentsEnFrance ?? false);
        this.situationFamilialeAlEtranger.set(r.situationFamilialeAlEtranger ?? null);
        this.niveauIntegration.set(r.niveauIntegration ?? null);
        this.ancienneConvictionPenale.set(r.ancienneConvictionPenale ?? false);
        this.provenanceDuree.set(null);
        this.provenanceMineur.set(null);
        this.provenanceEnfants.set(null);
        this.provenanceNiveau.set(null);
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

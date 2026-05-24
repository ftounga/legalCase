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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';

import { EtrangerMaladeService } from '../../core/services/etranger-malade.service';
import {
  AvisOFII,
  EtrangerMaladeRequest,
  EtrangerMaladeResponse,
  VerdictEtrangerMalade,
} from '../../core/models/etranger-malade.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { EtrangerMaladePrefillRules } from './etranger-malade-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-02 — Outil décisionnel « Étranger malade L. 425-9 CESEDA » (F-IM-25).
 *
 * FR uniquement (CESEDA L.425-9, R.425-9+, Ord. 2020-1733, Loi 7 mars 2016 OFII).
 * Pattern miroir : {@link JldRetentionSectionComponent} (F-IM-21).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert ELIGIBLE_PROBABLE, orange SOUS_RESERVE,
 *    rouge NON_ELIGIBLE, navy EN_ATTENTE_AVIS_OFII
 *  - pré-fill IA (pathologie, traitement, avis OFII, date avis)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-etranger-malade-section',
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
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './etranger-malade-section.component.html',
  styleUrl: './etranger-malade-section.component.scss',
})
export class EtrangerMaladeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-25-etranger-malade-l4259-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ÉTRANGER MALADE L.425-9 (FR)';
  static readonly TOOL_ICON = 'medical_services';

  static getPrefillCount(input: PrefillCountInput): number {
    return EtrangerMaladePrefillRules.computePrefillCount(input);
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
  result = signal<EtrangerMaladeResponse | null>(null);

  pathologiePrincipale = signal<string>('');
  paysOrigine = signal<string>('');
  traitementDisponiblePaysOrigine = signal<boolean>(false);
  avisOFIIRendu = signal<boolean>(false);
  avisOFII = signal<AvisOFII | null>(null);
  dateAvisOFII = signal<string | null>(null);

  provenancePathologie = signal<'IA' | null>(null);
  provenanceTraitement = signal<'IA' | null>(null);
  provenanceAvisOFII = signal<'IA' | null>(null);
  provenanceDateAvisOFII = signal<'IA' | null>(null);

  readonly avisOptions: ReadonlyArray<{ code: AvisOFII; label: string }> = [
    { code: 'FAVORABLE', label: 'Favorable' },
    { code: 'DEFAVORABLE', label: 'Défavorable' },
    { code: 'EN_ATTENTE', label: 'En attente' },
  ];
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: EtrangerMaladeService,
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
    if (!this.pathologiePrincipale().trim()) return false;
    if (this.pathologiePrincipale().length > 500) return false;
    if (!this.paysOrigine().trim()) return false;
    if (this.avisOFIIRendu()) {
      const a = this.avisOFII();
      if (!a) return false;
      if (a === 'DEFAVORABLE' && !this.dateAvisOFII()) return false;
      const d = this.dateAvisOFII();
      if (d && d > this.todayIso) return false;
    }
    return true;
  }

  onPathologieChange(value: string): void {
    this.pathologiePrincipale.set(value ?? '');
    this.provenancePathologie.set(null);
  }

  onPaysOrigineChange(value: string): void {
    this.paysOrigine.set(value ?? '');
  }

  onTraitementChange(value: boolean): void {
    this.traitementDisponiblePaysOrigine.set(value);
    this.provenanceTraitement.set(null);
  }

  onAvisOFIIRenduChange(value: boolean): void {
    this.avisOFIIRendu.set(value);
    if (!value) {
      this.avisOFII.set(null);
      this.dateAvisOFII.set(null);
      this.provenanceAvisOFII.set(null);
      this.provenanceDateAvisOFII.set(null);
    }
  }

  onAvisOFIIChange(value: AvisOFII | null): void {
    this.avisOFII.set(value);
    this.provenanceAvisOFII.set(null);
    // Si EN_ATTENTE / null, on ne demande plus la date
    if (value !== 'DEFAVORABLE' && value !== 'FAVORABLE') {
      // dateAvisOFII reste possible mais non requise
    }
  }

  onDateAvisOFIIChange(value: string | null): void {
    this.dateAvisOFII.set(value || null);
    this.provenanceDateAvisOFII.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: EtrangerMaladeRequest = {
      pathologiePrincipale: this.pathologiePrincipale().trim(),
      paysOrigine: this.paysOrigine().trim(),
      traitementDisponiblePaysOrigine: this.traitementDisponiblePaysOrigine(),
      avisOFIIRendu: this.avisOFIIRendu(),
      avisOFII: this.avisOFIIRendu() ? this.avisOFII() : null,
      dateAvisOFII: this.avisOFIIRendu() ? this.dateAvisOFII() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse étranger malade enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(verdict: VerdictEtrangerMalade | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'em-banner em-banner--success';
      case 'ELIGIBLE_SOUS_RESERVE': return 'em-banner em-banner--warning';
      case 'NON_ELIGIBLE':          return 'em-banner em-banner--danger';
      case 'EN_ATTENTE_AVIS_OFII':  return 'em-banner em-banner--info';
      default:                      return 'em-banner';
    }
  }

  bannerIcon(verdict: VerdictEtrangerMalade | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'check_circle';
      case 'ELIGIBLE_SOUS_RESERVE': return 'warning';
      case 'NON_ELIGIBLE':          return 'error';
      case 'EN_ATTENTE_AVIS_OFII':  return 'hourglass_top';
      default:                      return 'info_outline';
    }
  }

  verdictLabel(verdict: VerdictEtrangerMalade | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PROBABLE':     return 'Éligible — protection probable';
      case 'ELIGIBLE_SOUS_RESERVE': return 'Éligible sous réserve';
      case 'NON_ELIGIBLE':          return 'Non éligible';
      case 'EN_ATTENTE_AVIS_OFII':  return 'En attente de l\'avis OFII';
      default:                      return '';
    }
  }

  chipLabelCritere(code: string): string {
    switch (code) {
      case 'PATHOLOGIE_NON_RENSEIGNEE':       return 'Pathologie non renseignée';
      case 'TRAITEMENT_DISPONIBLE_PAYS_ORIGINE': return 'Traitement disponible dans le pays d\'origine';
      case 'AVIS_OFII_DEFAVORABLE':           return 'Avis OFII défavorable';
      default:                                return code;
    }
  }

  /**
   * Affiche la sous-section « délai recours TA » seulement si le verdict est
   * NON_ELIGIBLE et qu'un délai a été calculé (DEFAVORABLE + dateAvis).
   */
  showDelaiRecoursTA(r: EtrangerMaladeResponse | null): boolean {
    return !!(r && r.delaiRecoursTA);
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const pathologie = EtrangerMaladePrefillRules.computePathologiePrincipale(input);
    if (pathologie !== null && !this.pathologiePrincipale()) {
      this.pathologiePrincipale.set(pathologie);
      this.provenancePathologie.set('IA');
    }

    const traitement = EtrangerMaladePrefillRules.computeTraitementDisponiblePaysOrigine(input);
    if (traitement !== null && this.provenanceTraitement() === null) {
      this.traitementDisponiblePaysOrigine.set(traitement);
      this.provenanceTraitement.set('IA');
    }

    const avis = EtrangerMaladePrefillRules.computeAvisOFII(input);
    if (avis !== null && !this.avisOFII()) {
      this.avisOFII.set(avis);
      this.avisOFIIRendu.set(true);
      this.provenanceAvisOFII.set('IA');
    }

    const dateAvis = EtrangerMaladePrefillRules.computeDateAvisOFII(input);
    if (dateAvis !== null && !this.dateAvisOFII()) {
      this.dateAvisOFII.set(dateAvis);
      this.provenanceDateAvisOFII.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.pathologiePrincipale.set(r.pathologiePrincipale ?? '');
        this.paysOrigine.set(r.paysOrigine ?? '');
        this.traitementDisponiblePaysOrigine.set(!!r.traitementDisponiblePaysOrigine);
        if (r.avisOFII) {
          this.avisOFIIRendu.set(true);
          this.avisOFII.set(r.avisOFII);
        } else {
          this.avisOFIIRendu.set(false);
          this.avisOFII.set(null);
        }
        this.dateAvisOFII.set(r.dateAvisOFII);
        this.provenancePathologie.set(null);
        this.provenanceTraitement.set(null);
        this.provenanceAvisOFII.set(null);
        this.provenanceDateAvisOFII.set(null);
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

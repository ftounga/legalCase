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

import { RegroupementFamilialService } from '../../core/services/regroupement-familial.service';
import {
  RegroupementFamilialRequest,
  RegroupementFamilialResponse,
  TypeRegroupement,
  VerdictRegroupementFamilial,
} from '../../core/models/regroupement-familial.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RegroupementFamilialPrefillRules } from './regroupement-familial-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-04 — Outil décisionnel « Regroupement familial L.434-1+ CESEDA » (F-IM-26).
 *
 * FR uniquement (CESEDA L.434-1 à L.434-10, R.434-1+ — ressources SMIC + surface
 * habitable). Pattern miroir : {@link EtrangerMaladeSectionComponent} (F-IM-25).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert ELIGIBLE, orange SOUS_RESERVE,
 *    rouge NON_ELIGIBLE_*
 *  - pré-fill IA (durée séjour, ressources, type regroupement)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-regroupement-familial-section',
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
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './regroupement-familial-section.component.html',
  styleUrl: './regroupement-familial-section.component.scss',
})
export class RegroupementFamilialSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-26-regroupement-familial-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'REGROUPEMENT FAMILIAL L.434-1+ (FR)';
  static readonly TOOL_ICON = 'family_restroom';

  static getPrefillCount(input: PrefillCountInput): number {
    return RegroupementFamilialPrefillRules.computePrefillCount(input);
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
  result = signal<RegroupementFamilialResponse | null>(null);

  dureeSejourRegulierMois = signal<number | null>(null);
  ressourcesMensuellesNettes = signal<number | null>(null);
  tailleLogementM2 = signal<number | null>(null);
  nombrePersonnesFoyer = signal<number | null>(null);
  typeRegroupement = signal<TypeRegroupement | null>(null);
  membresFamilleARegrouper = signal<number | null>(null);

  provenanceDuree = signal<'IA' | null>(null);
  provenanceRessources = signal<'IA' | null>(null);
  provenanceType = signal<'IA' | null>(null);

  readonly typeOptions: ReadonlyArray<{ code: TypeRegroupement; label: string }> = [
    { code: 'CONJOINT', label: 'Conjoint' },
    { code: 'ENFANT_MINEUR', label: 'Enfant mineur' },
    { code: 'AUTRE', label: 'Autre' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: RegroupementFamilialService,
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
    const duree = this.dureeSejourRegulierMois();
    if (duree === null || duree < 0) return false;
    const ressources = this.ressourcesMensuellesNettes();
    if (ressources === null || ressources < 0) return false;
    const taille = this.tailleLogementM2();
    if (taille === null || taille <= 0) return false;
    const foyer = this.nombrePersonnesFoyer();
    if (foyer === null || foyer < 1) return false;
    if (!this.typeRegroupement()) return false;
    const membres = this.membresFamilleARegrouper();
    if (membres === null || membres < 1 || membres > 6) return false;
    return true;
  }

  onDureeChange(value: number | null): void {
    this.dureeSejourRegulierMois.set(value);
    this.provenanceDuree.set(null);
  }

  onRessourcesChange(value: number | null): void {
    this.ressourcesMensuellesNettes.set(value);
    this.provenanceRessources.set(null);
  }

  onTailleChange(value: number | null): void {
    this.tailleLogementM2.set(value);
  }

  onNombrePersonnesChange(value: number | null): void {
    this.nombrePersonnesFoyer.set(value);
  }

  onTypeChange(value: TypeRegroupement | null): void {
    this.typeRegroupement.set(value);
    this.provenanceType.set(null);
  }

  onMembresChange(value: number | null): void {
    this.membresFamilleARegrouper.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RegroupementFamilialRequest = {
      dureeSejourRegulierMois: this.dureeSejourRegulierMois()!,
      ressourcesMensuellesNettes: this.ressourcesMensuellesNettes()!,
      tailleLogementM2: this.tailleLogementM2()!,
      nombrePersonnesFoyer: this.nombrePersonnesFoyer()!,
      typeRegroupement: this.typeRegroupement()!,
      membresFamilleARegrouper: this.membresFamilleARegrouper()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse regroupement familial enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(verdict: VerdictRegroupementFamilial | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                  return 'rf-banner rf-banner--success';
      case 'ELIGIBLE_SOUS_RESERVE':     return 'rf-banner rf-banner--warning';
      case 'NON_ELIGIBLE_DELAI':
      case 'NON_ELIGIBLE_RESSOURCES':
      case 'NON_ELIGIBLE_LOGEMENT':     return 'rf-banner rf-banner--danger';
      default:                          return 'rf-banner';
    }
  }

  bannerIcon(verdict: VerdictRegroupementFamilial | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                  return 'check_circle';
      case 'ELIGIBLE_SOUS_RESERVE':     return 'warning';
      case 'NON_ELIGIBLE_DELAI':
      case 'NON_ELIGIBLE_RESSOURCES':
      case 'NON_ELIGIBLE_LOGEMENT':     return 'error';
      default:                          return 'info_outline';
    }
  }

  verdictLabel(verdict: VerdictRegroupementFamilial | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE':                  return 'Éligible au regroupement familial';
      case 'ELIGIBLE_SOUS_RESERVE':     return 'Éligible sous réserve';
      case 'NON_ELIGIBLE_DELAI':        return 'Non éligible — durée de séjour insuffisante';
      case 'NON_ELIGIBLE_RESSOURCES':   return 'Non éligible — ressources insuffisantes';
      case 'NON_ELIGIBLE_LOGEMENT':     return 'Non éligible — logement insuffisant';
      default:                          return '';
    }
  }

  chipLabelCritere(code: string): string {
    switch (code) {
      case 'DUREE_SEJOUR_INSUFFISANTE':  return 'Durée de séjour régulier insuffisante (18 mois)';
      case 'RESSOURCES_INSUFFISANTES':   return 'Ressources mensuelles insuffisantes';
      case 'LOGEMENT_INSUFFISANT':       return 'Surface du logement insuffisante';
      default:                           return code;
    }
  }

  typeLabel(type: TypeRegroupement | null | undefined): string {
    switch (type) {
      case 'CONJOINT':      return 'Conjoint';
      case 'ENFANT_MINEUR': return 'Enfant mineur';
      case 'AUTRE':         return 'Autre';
      default:              return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const duree = RegroupementFamilialPrefillRules.computeDureeSejourRegulierMois(input);
    if (duree !== null && this.dureeSejourRegulierMois() === null) {
      this.dureeSejourRegulierMois.set(duree);
      this.provenanceDuree.set('IA');
    }

    const ressources = RegroupementFamilialPrefillRules.computeRessourcesMensuellesNettes(input);
    if (ressources !== null && this.ressourcesMensuellesNettes() === null) {
      this.ressourcesMensuellesNettes.set(ressources);
      this.provenanceRessources.set('IA');
    }

    const type = RegroupementFamilialPrefillRules.computeTypeRegroupement(input);
    if (type !== null && !this.typeRegroupement()) {
      this.typeRegroupement.set(type);
      this.provenanceType.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dureeSejourRegulierMois.set(r.dureeSejourRegulierMois ?? null);
        this.ressourcesMensuellesNettes.set(r.ressourcesMensuellesNettes ?? null);
        this.tailleLogementM2.set(r.tailleLogementM2 ?? null);
        this.nombrePersonnesFoyer.set(r.nombrePersonnesFoyer ?? null);
        this.typeRegroupement.set(r.typeRegroupement ?? null);
        this.membresFamilleARegrouper.set(r.membresFamilleARegrouper ?? null);
        this.provenanceDuree.set(null);
        this.provenanceRessources.set(null);
        this.provenanceType.set(null);
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

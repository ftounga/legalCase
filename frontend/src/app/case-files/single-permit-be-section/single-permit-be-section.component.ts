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
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SinglePermitBeService } from '../../core/services/single-permit-be.service';
import {
  SinglePermitBeRequest,
  SinglePermitBeResponse,
  SinglePermitMotif,
  SinglePermitRegion,
  SinglePermitStatutRenouvellement,
  SinglePermitTypeActivite,
} from '../../core/models/single-permit-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { SinglePermitBePrefillRules } from './single-permit-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-215-02 — Outil décisionnel « Permis unique BE » (F-IM-25-single-permit-be).
 *
 * BELGIQUE uniquement — combine travail + séjour (single permit) sur la base de :
 *  - Loi 15/12/1980 art. 61/25-2 à 61/25-7
 *  - Accord de coopération du 02/02/2018 (compétences régionales)
 *  - Décret CRWAPE 16/05/2019 + AGW 02/05/2019 (Wallonie — FOREM)
 *  - Arrêté du Gouvernement flamand du 07/12/2018 (VDAB)
 *  - Ordonnance bruxelloise du 09/07/2015 (ACTIRIS)
 *
 * Pattern miroir : {@link EtrangerMaladeSectionComponent} (F-IM-25 FR) — même
 * famille F-IA-04 (standalone OnPush, palette navy/or, dashboardRefresh, signals
 * provenance IA, static getPrefillCount miroir du runtime prefillFromAi).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert DEPOSE_EN_TEMPS, orange DANS_DELAI,
 *    rouge URGENT, rouge sombre EXPIRE
 *  - pré-fill IA (5 champs : dates début/fin, région, type activité, motif)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-single-permit-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './single-permit-be-section.component.html',
  styleUrl: './single-permit-be-section.component.scss',
})
export class SinglePermitBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-25-single-permit-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PERMIS UNIQUE BE (travail+séjour)';
  static readonly TOOL_ICON = 'badge';

  static getPrefillCount(input: PrefillCountInput): number {
    return SinglePermitBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<SinglePermitBeResponse | null>(null);

  dateDebutPermit = signal<string | null>(null);
  dateFinPermit = signal<string | null>(null);
  regionInstruction = signal<SinglePermitRegion | null>(null);
  typeActivite = signal<SinglePermitTypeActivite | null>(null);
  motifDemande = signal<SinglePermitMotif | null>(null);

  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceDateFin = signal<'IA' | null>(null);
  provenanceRegion = signal<'IA' | null>(null);
  provenanceTypeActivite = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);

  readonly regionOptions: ReadonlyArray<{ code: SinglePermitRegion; label: string }> = [
    { code: 'WALLONIE', label: 'Wallonie (FOREM)' },
    { code: 'FLANDRE', label: 'Flandre (VDAB)' },
    { code: 'BRUXELLES', label: 'Bruxelles (ACTIRIS)' },
  ];

  readonly typeActiviteOptions: ReadonlyArray<{ code: SinglePermitTypeActivite; label: string }> = [
    { code: 'SALARIE', label: 'Travailleur salarié' },
    { code: 'STAGIAIRE', label: 'Stagiaire' },
    { code: 'DETACHE', label: 'Travailleur détaché' },
    { code: 'CHERCHEUR', label: 'Chercheur' },
    { code: 'ETUDIANT', label: 'Étudiant (job étudiant)' },
  ];

  readonly motifOptions: ReadonlyArray<{ code: SinglePermitMotif; label: string }> = [
    { code: 'NOUVEAU', label: 'Nouvelle demande' },
    { code: 'RENOUVELLEMENT', label: 'Renouvellement' },
  ];

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private readonly service: SinglePermitBeService,
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
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
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
    if (!this.dateDebutPermit()) return false;
    if (!this.dateFinPermit()) return false;
    if (this.dateDebutPermit()! > this.dateFinPermit()!) return false;
    if (!this.regionInstruction()) return false;
    if (!this.typeActivite()) return false;
    if (!this.motifDemande()) return false;
    return true;
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutPermit.set(value || null);
    this.provenanceDateDebut.set(null);
  }

  onDateFinChange(value: string | null): void {
    this.dateFinPermit.set(value || null);
    this.provenanceDateFin.set(null);
  }

  onRegionChange(value: SinglePermitRegion | null): void {
    this.regionInstruction.set(value);
    this.provenanceRegion.set(null);
  }

  onTypeActiviteChange(value: SinglePermitTypeActivite | null): void {
    this.typeActivite.set(value);
    this.provenanceTypeActivite.set(null);
  }

  onMotifChange(value: SinglePermitMotif | null): void {
    this.motifDemande.set(value);
    this.provenanceMotif.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: SinglePermitBeRequest = {
      dateDebutPermit: this.dateDebutPermit()!,
      dateFinPermit: this.dateFinPermit()!,
      regionInstruction: this.regionInstruction()!,
      typeActivite: this.typeActivite()!,
      motifDemande: this.motifDemande()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse permis unique BE enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(statut: SinglePermitStatutRenouvellement | null | undefined): string {
    switch (statut) {
      case 'DEPOSE_EN_TEMPS': return 'sp-banner sp-banner--success';
      case 'DANS_DELAI':      return 'sp-banner sp-banner--warning';
      case 'URGENT':          return 'sp-banner sp-banner--danger';
      case 'EXPIRE':          return 'sp-banner sp-banner--danger-strong';
      default:                return 'sp-banner';
    }
  }

  bannerIcon(statut: SinglePermitStatutRenouvellement | null | undefined): string {
    switch (statut) {
      case 'DEPOSE_EN_TEMPS': return 'check_circle';
      case 'DANS_DELAI':      return 'schedule';
      case 'URGENT':          return 'warning';
      case 'EXPIRE':          return 'block';
      default:                return 'info_outline';
    }
  }

  statutLabel(statut: SinglePermitStatutRenouvellement | null | undefined): string {
    switch (statut) {
      case 'DEPOSE_EN_TEMPS': return 'Dépôt en temps utile';
      case 'DANS_DELAI':      return 'Dans le délai légal';
      case 'URGENT':          return 'Renouvellement urgent';
      case 'EXPIRE':          return 'Permit expiré';
      default:                return '';
    }
  }

  regionOptionLabel(code: SinglePermitRegion | null | undefined): string {
    return this.regionOptions.find((o) => o.code === code)?.label ?? (code ?? '');
  }

  typeActiviteOptionLabel(code: SinglePermitTypeActivite | null | undefined): string {
    return this.typeActiviteOptions.find((o) => o.code === code)?.label ?? (code ?? '');
  }

  motifOptionLabel(code: SinglePermitMotif | null | undefined): string {
    return this.motifOptions.find((o) => o.code === code)?.label ?? (code ?? '');
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateDebut = SinglePermitBePrefillRules.computeDateDebutPermit(input);
    if (dateDebut !== null && !this.dateDebutPermit()) {
      this.dateDebutPermit.set(dateDebut);
      this.provenanceDateDebut.set('IA');
    }

    const dateFin = SinglePermitBePrefillRules.computeDateFinPermit(input);
    if (dateFin !== null && !this.dateFinPermit()) {
      this.dateFinPermit.set(dateFin);
      this.provenanceDateFin.set('IA');
    }

    const region = SinglePermitBePrefillRules.computeRegionInstruction(input);
    if (region !== null && !this.regionInstruction()) {
      this.regionInstruction.set(region);
      this.provenanceRegion.set('IA');
    }

    const type = SinglePermitBePrefillRules.computeTypeActivite(input);
    if (type !== null && !this.typeActivite()) {
      this.typeActivite.set(type);
      this.provenanceTypeActivite.set('IA');
    }

    const motif = SinglePermitBePrefillRules.computeMotifDemande(input);
    if (motif !== null && !this.motifDemande()) {
      this.motifDemande.set(motif);
      this.provenanceMotif.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutPermit.set(r.dateDebutPermit ?? null);
        this.dateFinPermit.set(r.dateFinPermit ?? null);
        this.regionInstruction.set((r.regionInstruction as SinglePermitRegion) ?? null);
        this.typeActivite.set((r.typeActivite as SinglePermitTypeActivite) ?? null);
        this.motifDemande.set((r.motifDemande as SinglePermitMotif) ?? null);
        this.provenanceDateDebut.set(null);
        this.provenanceDateFin.set(null);
        this.provenanceRegion.set(null);
        this.provenanceTypeActivite.set(null);
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

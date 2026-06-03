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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PacsVpfService } from '../../core/services/pacs-vpf.service';
import {
  PacsVpfRequest,
  PacsVpfResponse,
  EligibilitePacsVpf,
  PartenaireStatut,
  IntensiteCommunauteVie,
} from '../../core/models/pacs-vpf.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { PacsVpfPrefillRules } from './pacs-vpf-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-220-04 — Outil décisionnel « VPF au titre d'un PACS L.423-23 » (F-IM-50).
 *
 * FR uniquement. Apprécie le PACS comme FAISCEAU d'indices de vie privée et
 * familiale (CESEDA L.423-23). Le PACS n'ouvre PAS de droit automatique au
 * séjour (distinct du conjoint marié F-IM-21) ; sa valeur probante dépend de
 * l'ancienneté (~1 an) et de l'intensité de la communauté de vie. Distinct de
 * F-IM-27 (VPF liens personnels L.423-23 générale). Pattern miroir :
 * {@link VpfJeuneMajeurSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or Immigration
 *  - pré-fill IA (PACS conclu, date, durée vie commune, intensité)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)</p>
 */
@Component({
  selector: 'app-pacs-vpf-section',
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
  templateUrl: './pacs-vpf-section.component.html',
  styleUrl: './pacs-vpf-section.component.scss',
})
export class PacsVpfSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-50-pacs-vpf-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VPF AU TITRE D\'UN PACS L.423-23 (FR)';
  static readonly TOOL_ICON = 'favorite';

  static getPrefillCount(input: PrefillCountInput): number {
    return PacsVpfPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly partenaireStatuts: PartenaireStatut[] = ['FRANCAIS', 'ETRANGER_REGULIER', 'AUTRE'];
  readonly intensites: IntensiteCommunauteVie[] = ['FORTE', 'MOYENNE', 'FAIBLE', 'NON_ETABLIE'];

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<PacsVpfResponse | null>(null);

  pacsConclu = signal<boolean>(false);
  datePacs = signal<string | null>(null);
  partenaireStatut = signal<PartenaireStatut>('AUTRE');
  dureeVieCommuneMois = signal<number | null>(null);
  intensiteCommunauteVie = signal<IntensiteCommunauteVie>('NON_ETABLIE');
  autresLiensPrivesFamiliaux = signal<boolean>(false);

  provenancePacsConclu = signal<'IA' | null>(null);
  provenanceDatePacs = signal<'IA' | null>(null);
  provenanceDuree = signal<'IA' | null>(null);
  provenanceIntensite = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: PacsVpfService,
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
    // Le PACS est l'objet de l'outil — il faut au moins déclarer s'il est conclu.
    // pacsConclu est un booléen toujours défini ; on autorise l'analyse en toutes
    // circonstances (le verdict NON_ELIGIBLE couvre l'absence de PACS).
    return true;
  }

  onPacsConcluChange(value: boolean): void {
    this.pacsConclu.set(value);
    this.provenancePacsConclu.set(null);
  }

  onDatePacsChange(value: string | null): void {
    this.datePacs.set(value);
    this.provenanceDatePacs.set(null);
  }

  onDureeChange(value: number | null): void {
    this.dureeVieCommuneMois.set(value);
    this.provenanceDuree.set(null);
  }

  onIntensiteChange(value: IntensiteCommunauteVie): void {
    this.intensiteCommunauteVie.set(value);
    this.provenanceIntensite.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: PacsVpfRequest = {
      pacsConclu: this.pacsConclu(),
      datePacs: this.datePacs(),
      partenaireStatut: this.partenaireStatut(),
      dureeVieCommuneMois: this.dureeVieCommuneMois(),
      intensiteCommunauteVie: this.intensiteCommunauteVie(),
      autresLiensPrivesFamiliaux: this.autresLiensPrivesFamiliaux(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse VPF PACS enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(result: PacsVpfResponse | null | undefined): string {
    if (!result) return 'pvp-banner';
    switch (result.eligibilite) {
      case 'FAISCEAU_FAVORABLE':
        return 'pvp-banner pvp-banner--success';
      case 'FAISCEAU_INSUFFISANT':
      case 'A_CONSOLIDER':
        return 'pvp-banner pvp-banner--warning';
      case 'NON_ELIGIBLE':
      default:
        return 'pvp-banner pvp-banner--neutral';
    }
  }

  bannerIcon(result: PacsVpfResponse | null | undefined): string {
    if (!result) return 'info_outline';
    switch (result.eligibilite) {
      case 'FAISCEAU_FAVORABLE':
        return 'verified';
      case 'A_CONSOLIDER':
        return 'help_outline';
      case 'FAISCEAU_INSUFFISANT':
        return 'report_problem';
      case 'NON_ELIGIBLE':
      default:
        return 'cancel';
    }
  }

  eligibiliteLabel(e: EligibilitePacsVpf | null | undefined): string {
    switch (e) {
      case 'FAISCEAU_FAVORABLE':
        return 'Faisceau d\'indices favorable (L.423-23)';
      case 'FAISCEAU_INSUFFISANT':
        return 'Faisceau insuffisant (PACS récent ou peu intense)';
      case 'A_CONSOLIDER':
        return 'Faisceau à consolider (éléments à factualiser)';
      case 'NON_ELIGIBLE':
        return 'Non éligible par cette voie en l\'état';
      default:
        return '';
    }
  }

  intensiteLabel(i: IntensiteCommunauteVie): string {
    switch (i) {
      case 'FORTE':
        return 'Forte (stabilité établie)';
      case 'MOYENNE':
        return 'Moyenne';
      case 'FAIBLE':
        return 'Faible';
      case 'NON_ETABLIE':
      default:
        return 'Non établie';
    }
  }

  partenaireLabel(p: PartenaireStatut): string {
    switch (p) {
      case 'FRANCAIS':
        return 'Partenaire français';
      case 'ETRANGER_REGULIER':
        return 'Partenaire étranger en séjour régulier';
      case 'AUTRE':
      default:
        return 'Autre situation';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const pacsConclu = PacsVpfPrefillRules.computePacsConclu(input);
    if (pacsConclu !== null && !this.pacsConclu()) {
      this.pacsConclu.set(pacsConclu);
      this.provenancePacsConclu.set('IA');
    }

    const datePacs = PacsVpfPrefillRules.computeDatePacs(input);
    if (datePacs !== null && this.datePacs() === null) {
      this.datePacs.set(datePacs);
      this.provenanceDatePacs.set('IA');
    }

    const duree = PacsVpfPrefillRules.computeDureeVieCommuneMois(input);
    if (duree !== null && this.dureeVieCommuneMois() === null) {
      this.dureeVieCommuneMois.set(duree);
      this.provenanceDuree.set('IA');
    }

    const intensite = PacsVpfPrefillRules.computeIntensiteCommunauteVie(input);
    if (intensite !== null && this.intensiteCommunauteVie() === 'NON_ETABLIE') {
      this.intensiteCommunauteVie.set(intensite);
      this.provenanceIntensite.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.pacsConclu.set(r.pacsConclu ?? false);
        this.datePacs.set(r.datePacs ?? null);
        this.partenaireStatut.set(r.partenaireStatut ?? 'AUTRE');
        this.dureeVieCommuneMois.set(r.dureeVieCommuneMois ?? null);
        this.intensiteCommunauteVie.set(r.intensiteCommunauteVie ?? 'NON_ETABLIE');
        this.autresLiensPrivesFamiliaux.set(r.autresLiensPrivesFamiliaux ?? false);
        this.provenancePacsConclu.set(null);
        this.provenanceDatePacs.set(null);
        this.provenanceDuree.set(null);
        this.provenanceIntensite.set(null);
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

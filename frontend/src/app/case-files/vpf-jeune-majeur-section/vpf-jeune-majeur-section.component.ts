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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VpfJeuneMajeurService } from '../../core/services/vpf-jeune-majeur.service';
import {
  VpfJeuneMajeurRequest,
  VpfJeuneMajeurResponse,
  EligibiliteVpfJeuneMajeur,
} from '../../core/models/vpf-jeune-majeur.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VpfJeuneMajeurPrefillRules } from './vpf-jeune-majeur-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-220-03 — Outil décisionnel « VPF jeune majeur L.423-22 » (F-IM-49).
 *
 * FR uniquement. Évalue l'éligibilité d'un jeune majeur (16-21 ans, entré
 * mineur, scolarisé / pris en charge ASE) à la carte « vie privée et familiale »
 * de l'art. L.423-22 CESEDA (transition à la majorité / sortie ASE). Distinct de
 * F-IM-27 (VPF liens personnels L.423-23), F-IM-19 (mineurs) et F-IM-38
 * (évaluation de l'âge MNA). Pattern miroir : {@link RegimeMayotteSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or Immigration
 *  - pré-fill IA (âge, entrée mineur, prise en charge ASE, scolarisé)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)</p>
 */
@Component({
  selector: 'app-vpf-jeune-majeur-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './vpf-jeune-majeur-section.component.html',
  styleUrl: './vpf-jeune-majeur-section.component.scss',
})
export class VpfJeuneMajeurSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-49-vpf-jeune-majeur-l42322-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VPF JEUNE MAJEUR L.423-22 (FR)';
  static readonly TOOL_ICON = 'school';

  static getPrefillCount(input: PrefillCountInput): number {
    return VpfJeuneMajeurPrefillRules.computePrefillCount(input);
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
  result = signal<VpfJeuneMajeurResponse | null>(null);

  age = signal<number | null>(null);
  entreMineur = signal<boolean>(false);
  dateEntreeFrance = signal<string | null>(null);
  ageEntreeAse = signal<number | null>(null);
  priseEnChargeAse = signal<boolean>(false);
  dateDebutPriseEnCharge = signal<string | null>(null);
  ancienneteMoisPriseEnCharge = signal<number | null>(null);
  scolariseOuFormation = signal<boolean>(false);
  caractereReelEtSerieuxFormation = signal<boolean>(false);
  avisStructureFavorable = signal<boolean>(false);
  absenceLienFamillePays = signal<boolean>(false);

  provenanceAge = signal<'IA' | null>(null);
  provenanceEntreMineur = signal<'IA' | null>(null);
  provenancePriseEnCharge = signal<'IA' | null>(null);
  provenanceScolarise = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: VpfJeuneMajeurService,
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
    const a = this.age();
    return a !== null && a > 0 && a <= 30;
  }

  onAgeChange(value: number | null): void {
    this.age.set(value);
    this.provenanceAge.set(null);
  }

  onEntreMineurChange(value: boolean): void {
    this.entreMineur.set(value);
    this.provenanceEntreMineur.set(null);
  }

  onPriseEnChargeChange(value: boolean): void {
    this.priseEnChargeAse.set(value);
    this.provenancePriseEnCharge.set(null);
  }

  onScolariseChange(value: boolean): void {
    this.scolariseOuFormation.set(value);
    this.provenanceScolarise.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: VpfJeuneMajeurRequest = {
      age: this.age()!,
      entreMineur: this.entreMineur(),
      dateEntreeFrance: this.dateEntreeFrance(),
      ageEntreeAse: this.ageEntreeAse(),
      priseEnChargeAse: this.priseEnChargeAse(),
      dateDebutPriseEnCharge: this.dateDebutPriseEnCharge(),
      ancienneteMoisPriseEnCharge: this.ancienneteMoisPriseEnCharge(),
      scolariseOuFormation: this.scolariseOuFormation(),
      caractereReelEtSerieuxFormation: this.caractereReelEtSerieuxFormation(),
      avisStructureFavorable: this.avisStructureFavorable(),
      absenceLienFamillePays: this.absenceLienFamillePays(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse VPF jeune majeur enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(result: VpfJeuneMajeurResponse | null | undefined): string {
    if (!result) return 'vjm-banner';
    switch (result.eligibilite) {
      case 'ELIGIBLE_L42322':
        return 'vjm-banner vjm-banner--success';
      case 'ELIGIBLE_SOUS_RESERVE':
      case 'ORIENTER_AES':
        return 'vjm-banner vjm-banner--warning';
      case 'NON_ELIGIBLE':
      default:
        return 'vjm-banner vjm-banner--neutral';
    }
  }

  bannerIcon(result: VpfJeuneMajeurResponse | null | undefined): string {
    if (!result) return 'info_outline';
    switch (result.eligibilite) {
      case 'ELIGIBLE_L42322':
        return 'verified';
      case 'ELIGIBLE_SOUS_RESERVE':
        return 'help_outline';
      case 'ORIENTER_AES':
        return 'alt_route';
      case 'NON_ELIGIBLE':
      default:
        return 'cancel';
    }
  }

  eligibiliteLabel(e: EligibiliteVpfJeuneMajeur | null | undefined): string {
    switch (e) {
      case 'ELIGIBLE_L42322':
        return 'Éligible carte VPF L.423-22 (jeune majeur entré mineur)';
      case 'ELIGIBLE_SOUS_RESERVE':
        return 'Éligible sous réserve (un critère à confirmer)';
      case 'ORIENTER_AES':
        return 'Orienter vers l\'admission exceptionnelle (L.435-3)';
      case 'NON_ELIGIBLE':
        return 'Non éligible à la voie L.423-22 en l\'état';
      default:
        return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const age = VpfJeuneMajeurPrefillRules.computeAge(input);
    if (age !== null && this.age() === null) {
      this.age.set(age);
      this.provenanceAge.set('IA');
    }

    const entreMineur = VpfJeuneMajeurPrefillRules.computeEntreMineur(input);
    if (entreMineur !== null && !this.entreMineur()) {
      this.entreMineur.set(entreMineur);
      this.provenanceEntreMineur.set('IA');
    }

    const pec = VpfJeuneMajeurPrefillRules.computePriseEnChargeAse(input);
    if (pec !== null && !this.priseEnChargeAse()) {
      this.priseEnChargeAse.set(pec);
      this.provenancePriseEnCharge.set('IA');
    }

    const scol = VpfJeuneMajeurPrefillRules.computeScolariseOuFormation(input);
    if (scol !== null && !this.scolariseOuFormation()) {
      this.scolariseOuFormation.set(scol);
      this.provenanceScolarise.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.age.set(r.age ?? null);
        this.entreMineur.set(r.entreMineur ?? false);
        this.dateEntreeFrance.set(r.dateEntreeFrance ?? null);
        this.ageEntreeAse.set(r.ageEntreeAse ?? null);
        this.priseEnChargeAse.set(r.priseEnChargeAse ?? false);
        this.dateDebutPriseEnCharge.set(r.dateDebutPriseEnCharge ?? null);
        this.ancienneteMoisPriseEnCharge.set(r.ancienneteMoisPriseEnCharge ?? null);
        this.scolariseOuFormation.set(r.scolariseOuFormation ?? false);
        this.caractereReelEtSerieuxFormation.set(r.caractereReelEtSerieuxFormation ?? false);
        this.provenanceAge.set(null);
        this.provenanceEntreMineur.set(null);
        this.provenancePriseEnCharge.set(null);
        this.provenanceScolarise.set(null);
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

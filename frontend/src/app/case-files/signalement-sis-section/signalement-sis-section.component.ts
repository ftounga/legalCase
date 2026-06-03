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

import { SignalementSisService } from '../../core/services/signalement-sis.service';
import {
  SignalementSisRequest,
  SignalementSisResponse,
  ActionPossibleSis,
  EtatSignalant,
  MotifSignalement,
} from '../../core/models/signalement-sis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { SignalementSisPrefillRules } from './signalement-sis-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-220-06 — Outil décisionnel « contestation / radiation d'un signalement SIS
 * aux fins de non-admission » (F-IM-52, Règl. UE 2018/1860 / CESEDA L.312-3).
 *
 * FR uniquement. Identifie la voie de contestation / radiation d'un signalement
 * SIS selon l'État signalant (FRANCE → contestation autorité FR ;
 * AUTRE_ETAT_MEMBRE → droit d'accès / rectification de l'État signalant ; titre
 * valide + signalement étranger → consultation entre États). Distinct de
 * F-IM-20 (mesures d'éloignement : expulsion / IRTF / IAT). Pattern miroir :
 * {@link DecheanceNationaliteSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or Immigration
 *  - pré-fill IA (signalement connu, État signalant, motif, titre valide)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)</p>
 */
@Component({
  selector: 'app-signalement-sis-section',
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
  templateUrl: './signalement-sis-section.component.html',
  styleUrl: './signalement-sis-section.component.scss',
})
export class SignalementSisSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-52-signalement-sis-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'CONTESTATION SIGNALEMENT SIS (NON-ADMISSION) (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return SignalementSisPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly etatsSignalant: EtatSignalant[] = ['FRANCE', 'AUTRE_ETAT_MEMBRE', 'INCONNU'];
  readonly motifs: MotifSignalement[] = [
    'IRTF',
    'MESURE_ELOIGNEMENT_ETRANGERE',
    'MENACE_ORDRE_PUBLIC',
    'AUTRE',
  ];

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<SignalementSisResponse | null>(null);

  signalementConnu = signal<boolean | null>(null);
  etatSignalant = signal<EtatSignalant | null>(null);
  motifSignalement = signal<MotifSignalement | null>(null);
  titreSejourValide = signal<boolean | null>(null);
  dateSignalement = signal<string | null>(null);

  provenanceSignalementConnu = signal<'IA' | null>(null);
  provenanceEtatSignalant = signal<'IA' | null>(null);
  provenanceMotifSignalement = signal<'IA' | null>(null);
  provenanceTitreSejourValide = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: SignalementSisService,
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
    // L'État signalant est l'élément structurant : la voie de radiation en
    // dépend. L'analyse exige au moins un État signalant sélectionné.
    return this.etatSignalant() !== null;
  }

  onSignalementConnuChange(value: boolean | null): void {
    this.signalementConnu.set(value);
    this.provenanceSignalementConnu.set(null);
  }

  onEtatSignalantChange(value: EtatSignalant): void {
    this.etatSignalant.set(value);
    this.provenanceEtatSignalant.set(null);
  }

  onMotifSignalementChange(value: MotifSignalement): void {
    this.motifSignalement.set(value);
    this.provenanceMotifSignalement.set(null);
  }

  onTitreSejourValideChange(value: boolean | null): void {
    this.titreSejourValide.set(value);
    this.provenanceTitreSejourValide.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: SignalementSisRequest = {
      signalementConnu: this.signalementConnu(),
      etatSignalant: this.etatSignalant(),
      motifSignalement: this.motifSignalement(),
      titreSejourValide: this.titreSejourValide(),
      dateSignalement: this.dateSignalement(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse du signalement SIS enregistrée', 'OK', { duration: 2500 });
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

  bannerClass(result: SignalementSisResponse | null | undefined): string {
    if (!result) return 'sis-banner';
    switch (result.actionPossible) {
      case 'RADIATION_AUTORITE_FR':
        return 'sis-banner sis-banner--success';
      case 'CONSULTATION_ENTRE_ETATS':
        return 'sis-banner sis-banner--warning';
      case 'RADIATION_ETAT_SIGNALANT':
      case 'DROIT_ACCES_RECTIFICATION':
        return 'sis-banner sis-banner--info';
      case 'INDETERMINE':
      default:
        return 'sis-banner sis-banner--neutral';
    }
  }

  bannerIcon(result: SignalementSisResponse | null | undefined): string {
    if (!result) return 'info_outline';
    switch (result.actionPossible) {
      case 'RADIATION_AUTORITE_FR':
        return 'verified';
      case 'CONSULTATION_ENTRE_ETATS':
        return 'sync_alt';
      case 'RADIATION_ETAT_SIGNALANT':
        return 'public';
      case 'DROIT_ACCES_RECTIFICATION':
        return 'manage_search';
      case 'INDETERMINE':
      default:
        return 'help_outline';
    }
  }

  actionLabel(v: ActionPossibleSis | null | undefined): string {
    switch (v) {
      case 'RADIATION_AUTORITE_FR':
        return 'Radiation via contestation devant l\'autorité française';
      case 'RADIATION_ETAT_SIGNALANT':
        return 'Radiation relevant de l\'État signalant';
      case 'DROIT_ACCES_RECTIFICATION':
        return 'Droit d\'accès / rectification (État signalant à identifier)';
      case 'CONSULTATION_ENTRE_ETATS':
        return 'Consultation entre États (titre valide vs non-admission)';
      case 'INDETERMINE':
        return 'Situation indéterminée';
      default:
        return '';
    }
  }

  etatSignalantLabel(e: EtatSignalant | null | undefined): string {
    switch (e) {
      case 'FRANCE':
        return 'France';
      case 'AUTRE_ETAT_MEMBRE':
        return 'Autre État membre Schengen';
      case 'INCONNU':
        return 'État signalant inconnu';
      default:
        return '';
    }
  }

  motifLabel(m: MotifSignalement | null | undefined): string {
    switch (m) {
      case 'IRTF':
        return 'IRTF (interdiction de retour sur le territoire français)';
      case 'MESURE_ELOIGNEMENT_ETRANGERE':
        return 'Mesure d\'éloignement d\'un autre État membre';
      case 'MENACE_ORDRE_PUBLIC':
        return 'Menace pour l\'ordre public';
      case 'AUTRE':
        return 'Autre motif';
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

    const signalementConnu = SignalementSisPrefillRules.computeSignalementConnu(input);
    if (signalementConnu !== null && this.signalementConnu() === null) {
      this.signalementConnu.set(signalementConnu);
      this.provenanceSignalementConnu.set('IA');
    }

    const etatSignalant = SignalementSisPrefillRules.computeEtatSignalant(input);
    if (etatSignalant !== null && this.etatSignalant() === null) {
      this.etatSignalant.set(etatSignalant);
      this.provenanceEtatSignalant.set('IA');
    }

    const motifSignalement = SignalementSisPrefillRules.computeMotifSignalement(input);
    if (motifSignalement !== null && this.motifSignalement() === null) {
      this.motifSignalement.set(motifSignalement);
      this.provenanceMotifSignalement.set('IA');
    }

    const titreSejourValide = SignalementSisPrefillRules.computeTitreSejourValide(input);
    if (titreSejourValide !== null && this.titreSejourValide() === null) {
      this.titreSejourValide.set(titreSejourValide);
      this.provenanceTitreSejourValide.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.signalementConnu.set(r.signalementConnu ?? null);
        this.etatSignalant.set(r.etatSignalant ?? null);
        this.motifSignalement.set(r.motifSignalement ?? null);
        this.titreSejourValide.set(r.titreSejourValide ?? null);
        this.dateSignalement.set(r.dateSignalement ?? null);
        this.provenanceSignalementConnu.set(null);
        this.provenanceEtatSignalant.set(null);
        this.provenanceMotifSignalement.set(null);
        this.provenanceTitreSejourValide.set(null);
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

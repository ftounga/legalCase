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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { CarteResidentService } from '../../core/services/carte-resident.service';
import {
  CarteResidentNiveauIntegration,
  CarteResidentRequest,
  CarteResidentResponse,
  CarteResidentVerdict,
} from '../../core/models/carte-resident.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CarteResidentPrefillRules } from './carte-resident-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-24 — Outil décisionnel « Carte de résident — article L. 426-1 du
 * CESEDA » (F-IM-36).
 *
 * FR uniquement (régime distinct en BE). Évalue l'éligibilité à la carte de
 * résident de dix ans de l'article L. 426-1 du CESEDA (durée de séjour régulier,
 * intégration républicaine, ressources stables et suffisantes, absence de
 * condamnations pénales graves), affiche la checklist des critères non remplis
 * (chipsCriteresNonRemplis) et la liste des atouts du dossier (atouts). Pattern
 * miroir : {@link VictimeTraiteSectionComponent} (F-IM-35).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - gate FRANCE + bannière BE
 *  - pré-fill IA (dureeSejourRegulierAnnees depuis aesDureePresenceMois ÷ 12,
 *    ressourcesMensuellesNettes depuis carteResidentRessources)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-carte-resident-section',
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
    MatProgressSpinnerModule,
    RouterModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './carte-resident-section.component.html',
  styleUrl: './carte-resident-section.component.scss',
})
export class CarteResidentSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-36-carte-resident-l4261-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'CARTE DE RÉSIDENT L. 426-1 (FR)';
  static readonly TOOL_ICON = 'badge';

  static getPrefillCount(input: PrefillCountInput): number {
    return CarteResidentPrefillRules.computePrefillCount(input);
  }

  readonly niveauxIntegration: { value: CarteResidentNiveauIntegration; label: string }[] = [
    { value: 'FORT', label: 'Fort' },
    { value: 'MOYEN', label: 'Moyen' },
    { value: 'FAIBLE', label: 'Faible' },
  ];

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<CarteResidentResponse | null>(null);

  dureeSejourRegulierAnnees = signal<number | null>(null);
  typesTitresAnterieurs = signal<string | null>(null);
  niveauIntegration = signal<CarteResidentNiveauIntegration>('MOYEN');
  ressourcesMensuellesNettes = signal<number | null>(null);
  condamnationsPenalesGraves = signal(false);

  provenanceDuree = signal<'IA' | null>(null);
  provenanceRessources = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: CarteResidentService,
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
    const duree = this.dureeSejourRegulierAnnees();
    if (duree === null || duree < 0) return false;
    const ressources = this.ressourcesMensuellesNettes();
    if (ressources === null || ressources < 0) return false;
    return true;
  }

  onDureeChange(value: number | null): void {
    this.dureeSejourRegulierAnnees.set(value);
    this.provenanceDuree.set(null);
  }

  onRessourcesChange(value: number | null): void {
    this.ressourcesMensuellesNettes.set(value);
    this.provenanceRessources.set(null);
  }

  onNiveauChange(value: CarteResidentNiveauIntegration): void {
    this.niveauIntegration.set(value);
  }

  onCondamnationsChange(value: boolean): void {
    this.condamnationsPenalesGraves.set(value);
  }

  verdictLabel(verdict: CarteResidentVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'Éligible';
      case 'ELIGIBLE_SOUS_RESERVE': return 'Éligible sous réserve';
      case 'NON_ELIGIBLE_DELAI': return 'Non éligible — délai insuffisant';
      case 'NON_ELIGIBLE_INTEGRATION': return 'Non éligible — intégration insuffisante';
      case 'NON_ELIGIBLE_RESSOURCES': return 'Non éligible — ressources insuffisantes';
      case 'INADMISSIBLE': return 'Inadmissible';
      default: return '';
    }
  }

  verdictClass(verdict: CarteResidentVerdict | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'cr-chip--ok';
      case 'ELIGIBLE_SOUS_RESERVE': return 'cr-chip--warning';
      case 'NON_ELIGIBLE_DELAI':
      case 'NON_ELIGIBLE_INTEGRATION':
      case 'NON_ELIGIBLE_RESSOURCES': return 'cr-chip--ko';
      case 'INADMISSIBLE': return 'cr-chip--inadmissible';
      default: return 'cr-chip--neutral';
    }
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CarteResidentRequest = {
      dureeSejourRegulierAnnees: this.dureeSejourRegulierAnnees() as number,
      typesTitresAnterieurs: this.typesTitresAnterieurs() ?? null,
      niveauIntegration: this.niveauIntegration(),
      ressourcesMensuellesNettes: this.ressourcesMensuellesNettes() as number,
      condamnationsPenalesGraves: this.condamnationsPenalesGraves(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse carte de résident enregistrée', 'OK', { duration: 2500 });
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

    const duree = CarteResidentPrefillRules.computeDureeSejourRegulierAnnees(input);
    if (duree !== null && this.provenanceDuree() === null && this.dureeSejourRegulierAnnees() === null) {
      this.dureeSejourRegulierAnnees.set(duree);
      this.provenanceDuree.set('IA');
    }

    const ressources = CarteResidentPrefillRules.computeRessourcesMensuellesNettes(input);
    if (ressources !== null && this.provenanceRessources() === null && this.ressourcesMensuellesNettes() === null) {
      this.ressourcesMensuellesNettes.set(ressources);
      this.provenanceRessources.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
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

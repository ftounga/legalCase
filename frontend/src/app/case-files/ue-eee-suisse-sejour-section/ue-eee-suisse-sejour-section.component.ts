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
import { MatSnackBar } from '@angular/material/snack-bar';

import { UeEeeSuisseSejourService } from '../../core/services/ue-eee-suisse-sejour.service';
import {
  ActiviteProfessionnelle,
  TitreSejourUe,
  UeEeeSuisseSejourRequest,
  UeEeeSuisseSejourResponse,
} from '../../core/models/ue-eee-suisse-sejour.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { UeEeeSuisseSejourPrefillRules } from './ue-eee-suisse-sejour-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-40 — Outil décisionnel « Séjour UE/EEE/Suisse » (F-IM-44).
 *
 * FRANCE uniquement — droit au séjour des citoyens de l'Union européenne, de
 * l'EEE et de la Suisse (directive 2004/38/CE, art. L. 233-1 et s. du CESEDA).
 * Identifie automatiquement les citoyens UE (pré-fill `nationaliteUe`) et
 * détermine le droit au séjour automatique de 3 mois, le droit au séjour
 * permanent au-delà de 5 ans, le titre obtenu et la situation du membre de
 * famille non-UE le cas échéant.
 *
 * Analyseur de droits (pas de délai / pas de bridge échéance F-69) : indicateur
 * principal = `droitSejourPlus5Ans` (badge vert si acquis).
 * Pattern miroir : {@link ItfJudiciaireSectionComponent} (F-IM-43).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - pré-fill IA (nationalité + citoyenneté UE + durée de séjour)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-ue-eee-suisse-sejour-section',
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
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './ue-eee-suisse-sejour-section.component.html',
  styleUrl: './ue-eee-suisse-sejour-section.component.scss',
})
export class UeEeeSuisseSejourSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-44-ue-eee-suisse-sejour-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'SÉJOUR UE/EEE/SUISSE (FR)';
  static readonly TOOL_ICON = 'public';

  static getPrefillCount(input: PrefillCountInput): number {
    return UeEeeSuisseSejourPrefillRules.computePrefillCount(input);
  }

  readonly activiteOptions: { value: ActiviteProfessionnelle; label: string }[] = [
    { value: 'SALARIE', label: 'Salarié' },
    { value: 'INDEPENDANT', label: 'Indépendant' },
    { value: 'ETUDIANT', label: 'Étudiant' },
    { value: 'RETRAITE', label: 'Retraité' },
    { value: 'SANS_ACTIVITE_RESSOURCES_SUFFISANTES', label: 'Sans activité (ressources suffisantes)' },
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
  result = signal<UeEeeSuisseSejourResponse | null>(null);

  nationalite = signal<string | null>(null);
  estCitoyenUE = signal<boolean>(false);
  membreFamilleNonUE = signal<boolean>(false);
  dureeSejourMois = signal<number | null>(null);
  activiteProfessionnelle = signal<ActiviteProfessionnelle>('SALARIE');

  provenanceNationalite = signal<'IA' | null>(null);
  provenanceCitoyenUE = signal<'IA' | null>(null);
  provenanceDuree = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: UeEeeSuisseSejourService,
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
    const nat = this.nationalite();
    if (!nat || nat.trim().length === 0) return false;
    if (nat.length > 100) return false;
    const duree = this.dureeSejourMois();
    if (duree === null || duree < 0) return false;
    return true;
  }

  onNationaliteChange(value: string | null): void {
    this.nationalite.set(value || null);
    this.provenanceNationalite.set(null);
  }

  onEstCitoyenUEChange(value: boolean): void {
    this.estCitoyenUE.set(value);
    this.provenanceCitoyenUE.set(null);
  }

  onMembreFamilleNonUEChange(value: boolean): void {
    this.membreFamilleNonUE.set(value);
  }

  onDureeChange(value: number | null): void {
    this.dureeSejourMois.set(value ?? null);
    this.provenanceDuree.set(null);
  }

  onActiviteChange(value: ActiviteProfessionnelle): void {
    this.activiteProfessionnelle.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: UeEeeSuisseSejourRequest = {
      nationalite: this.nationalite()!.trim(),
      estCitoyenUE: this.estCitoyenUE(),
      membreFamilleNonUE: this.membreFamilleNonUE(),
      dureeSejourMois: this.dureeSejourMois()!,
      activiteProfessionnelle: this.activiteProfessionnelle(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse séjour UE/EEE/Suisse enregistrée', 'OK', { duration: 2500 });
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

  activiteLabel(value: ActiviteProfessionnelle | null | undefined): string {
    const found = this.activiteOptions.find((o) => o.value === value);
    return found ? found.label : '';
  }

  titreLabel(titre: TitreSejourUe | null | undefined): string {
    switch (titre) {
      case 'ATTESTATION_ENREGISTREMENT':       return "Attestation d'enregistrement";
      case 'CARTE_SEJOUR_MEMBRE_FAMILLE':      return 'Carte de séjour « membre de famille »';
      default:                                 return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const nat = UeEeeSuisseSejourPrefillRules.computeNationalite(input);
    if (nat !== null && this.nationalite() === null) {
      this.nationalite.set(nat);
      this.provenanceNationalite.set('IA');
    }

    const citoyen = UeEeeSuisseSejourPrefillRules.computeEstCitoyenUE(input);
    if (citoyen !== null && this.provenanceCitoyenUE() === null && !this.estCitoyenUE()) {
      this.estCitoyenUE.set(citoyen);
      this.provenanceCitoyenUE.set('IA');
    }

    const duree = UeEeeSuisseSejourPrefillRules.computeDureeSejourMois(input);
    if (duree !== null && this.dureeSejourMois() === null) {
      this.dureeSejourMois.set(duree);
      this.provenanceDuree.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.nationalite.set(r.nationalite ?? null);
        this.estCitoyenUE.set(r.estCitoyenUE ?? false);
        this.membreFamilleNonUE.set(r.membreFamilleNonUE ?? false);
        this.dureeSejourMois.set(r.dureeSejourMois ?? null);
        this.activiteProfessionnelle.set(r.activiteProfessionnelle ?? 'SALARIE');
        this.provenanceNationalite.set(null);
        this.provenanceCitoyenUE.set(null);
        this.provenanceDuree.set(null);
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

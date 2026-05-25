import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuditionMineurFrService } from '../../core/services/audition-mineur-fr.service';
import {
  AuditionMineurRequest,
  AuditionMineurResponse,
  CapaciteDiscernement,
  ProcedureAudition,
} from '../../core/models/audition-mineur-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AuditionMineurFrPrefillRules } from './audition-mineur-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-14 : composant Angular standalone pour l'outil décisionnel
 * "Audition du mineur par le JAF" (F-FA-AUDITION-MINEUR).
 *
 * FRANCE uniquement (art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC + CIDE
 * art. 12 + Cass. 1ère civ., 18/3/2015, n°14-11.392).
 *
 * Pattern de référence : `adoption-internationale-fr-section` (SF-216-18)
 * et `retrait-ap-fr-section` (SF-216-12).
 *
 * Pré-fill IA (2 champs F-246) :
 *  - ageEnfant ← `agesEnfantsDetectes[0]`
 *  - demandeFormalisee ← `demandeAuditionFormaliseeDetectee`
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-audition-mineur-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './audition-mineur-fr-section.component.html',
  styleUrl: './audition-mineur-fr-section.component.scss',
})
export class AuditionMineurFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-AUDITION-MINEUR';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'AUDITION DU MINEUR';
  static readonly TOOL_ICON = 'record_voice_over';

  /**
   * SF-216-14 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return AuditionMineurFrPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AuditionMineurResponse | null>(null);

  // --- Form fields (7 champs contrat backend SF-216-13) ---
  ageEnfant = signal<number | null>(null);
  capaciteDiscernement = signal<CapaciteDiscernement | null>(null);
  demandeFormalisee = signal<boolean>(false);
  demandeParEnfantLuiMeme = signal<boolean>(false);
  refusMotive = signal<boolean>(false);
  motivationRefus = signal<string | null>(null);
  procedureEnCours = signal<ProcedureAudition | null>(null);

  /** Options select capacité de discernement. */
  readonly capacites: ReadonlyArray<{ value: CapaciteDiscernement; label: string }> = [
    { value: 'CERTAINE', label: 'Certaine (élément concret documenté)' },
    { value: 'PROBABLE', label: 'Probable (âge / maturité)' },
    { value: 'DOUTEUSE', label: 'Douteuse (jeune âge ou doute)' },
    { value: 'INCONNUE', label: 'Inconnue (non instruite)' },
  ];

  /** Options select procédure. */
  readonly procedures: ReadonlyArray<{ value: ProcedureAudition; label: string }> = [
    { value: 'DIVORCE', label: 'Divorce / séparation de corps' },
    { value: 'AUTORITE_PARENTALE', label: 'Autorité parentale' },
    { value: 'GARDE', label: 'Garde / résidence' },
    { value: 'SUCCESSION', label: 'Succession' },
    { value: 'AUTRE', label: 'Autre procédure civile' },
  ];

  constructor(
    private service: AuditionMineurFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + âge enfant 0-17 + capacité de discernement renseignée.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const age = this.ageEnfant();
    if (age === null || !Number.isFinite(age) || age < 0 || age >= 18) return false;
    if (this.capaciteDiscernement() === null) return false;
    return true;
  }

  // --- Handlers ---

  onAgeChange(value: number | string | null): void {
    this.ageEnfant.set(this.parseNonNegativeInt(value));
  }

  onCapaciteChange(value: CapaciteDiscernement | null): void {
    this.capaciteDiscernement.set(value);
  }

  onDemandeFormaliseeChange(checked: boolean): void {
    this.demandeFormalisee.set(checked);
  }

  onDemandeParEnfantChange(checked: boolean): void {
    this.demandeParEnfantLuiMeme.set(checked);
  }

  onRefusMotiveChange(checked: boolean): void {
    this.refusMotive.set(checked);
    if (!checked) this.motivationRefus.set(null);
  }

  onMotivationRefusChange(value: string | null): void {
    this.motivationRefus.set(value && value.trim() !== '' ? value : null);
  }

  onProcedureChange(value: ProcedureAudition | null): void {
    this.procedureEnCours.set(value);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  /** Affiche une alerte UI préventive si l'enfant a moins de 5 ans. */
  isJeuneEnfant(): boolean {
    const age = this.ageEnfant();
    return age !== null && age < 5;
  }

  private applyPrefillFromAi(): void {
    const v = AuditionMineurFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.ageEnfant !== null && this.ageEnfant() === null) {
      this.ageEnfant.set(v.ageEnfant);
    }
    if (v.demandeFormalisee !== null) {
      this.demandeFormalisee.set(v.demandeFormalisee);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AuditionMineurRequest = {
      ageEnfant: this.ageEnfant(),
      capaciteDiscernement: this.capaciteDiscernement(),
      demandeFormalisee: this.demandeFormalisee(),
      demandeParEnfantLuiMeme: this.demandeParEnfantLuiMeme(),
      refusMotive: this.refusMotive(),
      motivationRefus: this.motivationRefus(),
      procedureEnCours: this.procedureEnCours(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Audition du mineur évaluée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: AuditionMineurResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  modaliteLabel(m: string): string {
    const labels: Record<string, string> = {
      SEUL: 'Audition par le juge seul',
      AVEC_AVOCAT: 'Avec avocat désigné pour l\'enfant',
      AVEC_TIERS: 'Avec un tiers de confiance (psy, assistant social)',
    };
    return labels[m] ?? m;
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.refusContestable) return 'verdict-warning';
    if (!r.conditionsRemplies && r.verdict !== 'AUDITION_DE_DROIT') return 'verdict-warning';
    return 'verdict-success';
  }
}

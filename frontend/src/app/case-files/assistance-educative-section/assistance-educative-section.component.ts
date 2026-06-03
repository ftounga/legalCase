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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AssistanceEducativeService } from '../../core/services/assistance-educative.service';
import {
  AssistanceEducativeRequest,
  AssistanceEducativeResponse,
  VerdictAssistanceEducative,
} from '../../core/models/assistance-educative.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AssistanceEducativePrefillRules } from './assistance-educative-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-222-04 : composant Angular standalone pour l'outil décisionnel "Assistance
 * éducative" — mineur en danger (F-FA-ASSISTANCE-EDUCATIVE).
 *
 * FRANCE uniquement (art. 375 et s. Cciv). Invariant « 1 situation = 1 outil » :
 * UN SEUL outil oriente vers les 4 issues d'UNE situation (mineur en danger) :
 * AED (administrative ASE), AEMO (judiciaire, milieu ouvert), OPP / placement
 * (judiciaire, retrait) ou pas de mesure. L'outil CONSEILLE l'avocat sur le
 * danger ; la mesure judiciaire est ordonnée par le juge des enfants.
 *
 * Pattern de référence : `habilitation-familiale-section` (SF-222-03).
 *
 * Formulaire (5 critères du contrat backend SF-222-04) :
 *  - dangerCaracterise (checkbox)
 *  - urgence (checkbox)
 *  - adhesionFamille (checkbox)
 *  - maintienMilieuFamilialPossible (checkbox)
 *  - mesureAmiableASEEnvisageable (checkbox)
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-assistance-educative-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './assistance-educative-section.component.html',
  styleUrl: './assistance-educative-section.component.scss',
})
export class AssistanceEducativeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-ASSISTANCE-EDUCATIVE';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ASSISTANCE EDUCATIVE';
  static readonly TOOL_ICON = 'child_care';

  /**
   * SF-222-04 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de critères pré-remplissables FR (max 5).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return AssistanceEducativePrefillRules.computePrefillCount({
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
  result = signal<AssistanceEducativeResponse | null>(null);

  // --- Form fields (5 critères du contrat backend SF-222-04) ---
  dangerCaracterise = signal<boolean>(false);
  urgence = signal<boolean>(false);
  adhesionFamille = signal<boolean>(false);
  maintienMilieuFamilialPossible = signal<boolean>(false);
  mesureAmiableASEEnvisageable = signal<boolean>(false);

  // Provenance IA par critère (badge dans l'UI tant que valeur non modifiée).
  provenanceDanger = signal<'IA' | null>(null);
  provenanceUrgence = signal<'IA' | null>(null);
  provenanceAdhesion = signal<'IA' | null>(null);
  provenanceMaintien = signal<'IA' | null>(null);
  provenanceMesureAmiable = signal<'IA' | null>(null);

  constructor(
    private service: AssistanceEducativeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA SF-222-04 — renseigne les 5 critères détectables depuis
   * `familleExtractedData`. No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = AssistanceEducativePrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const danger = rules.computeDanger(ruleInput);
    if (danger !== null) {
      this.dangerCaracterise.set(danger);
      this.provenanceDanger.set('IA');
    }
    const urgence = rules.computeUrgence(ruleInput);
    if (urgence !== null) {
      this.urgence.set(urgence);
      this.provenanceUrgence.set('IA');
    }
    const adhesion = rules.computeAdhesion(ruleInput);
    if (adhesion !== null) {
      this.adhesionFamille.set(adhesion);
      this.provenanceAdhesion.set('IA');
    }
    const maintien = rules.computeMaintien(ruleInput);
    if (maintien !== null) {
      this.maintienMilieuFamilialPossible.set(maintien);
      this.provenanceMaintien.set('IA');
    }
    const amiable = rules.computeMesureAmiable(ruleInput);
    if (amiable !== null) {
      this.mesureAmiableASEEnvisageable.set(amiable);
      this.provenanceMesureAmiable.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FR (les booléens sont toujours saisissables). */
  formValid(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onDangerChange(checked: boolean): void {
    this.dangerCaracterise.set(checked);
    this.provenanceDanger.set(null);
  }

  onUrgenceChange(checked: boolean): void {
    this.urgence.set(checked);
    this.provenanceUrgence.set(null);
  }

  onAdhesionChange(checked: boolean): void {
    this.adhesionFamille.set(checked);
    this.provenanceAdhesion.set(null);
  }

  onMaintienChange(checked: boolean): void {
    this.maintienMilieuFamilialPossible.set(checked);
    this.provenanceMaintien.set(null);
  }

  onMesureAmiableChange(checked: boolean): void {
    this.mesureAmiableASEEnvisageable.set(checked);
    this.provenanceMesureAmiable.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AssistanceEducativeRequest = {
      dangerCaracterise: this.dangerCaracterise(),
      urgence: this.urgence(),
      adhesionFamille: this.adhesionFamille(),
      maintienMilieuFamilialPossible: this.maintienMilieuFamilialPossible(),
      mesureAmiableASEEnvisageable: this.mesureAmiableASEEnvisageable(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Assistance éducative évaluée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
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
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: AssistanceEducativeResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Libellé humain pour le verdict. */
  verdictLabel(verdict: VerdictAssistanceEducative): string {
    const labels: Record<VerdictAssistanceEducative, string> = {
      AED: 'Aide éducative à domicile (AED — mesure administrative ASE)',
      AEMO: 'Action éducative en milieu ouvert (AEMO — mesure judiciaire)',
      OPP_PLACEMENT: 'Ordonnance de placement provisoire (OPP) / placement',
      PAS_DE_MESURE: 'Pas de mesure d\'assistance éducative',
    };
    return labels[verdict];
  }

  /** Classe CSS par verdict pour la pastille. */
  verdictClass(verdict: VerdictAssistanceEducative): string {
    const cls: Record<VerdictAssistanceEducative, string> = {
      AED: 'verdict-info',
      AEMO: 'verdict-warning',
      OPP_PLACEMENT: 'verdict-danger',
      PAS_DE_MESURE: 'verdict-success',
    };
    return cls[verdict];
  }
}

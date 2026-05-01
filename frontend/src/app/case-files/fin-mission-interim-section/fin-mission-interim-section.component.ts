import {
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FinMissionInterimService } from '../../core/services/fin-mission-interim.service';
import {
  MOTIF_EXCLUSION_OPTIONS,
  MotifExclusionInterim,
  FinMissionInterimRequest,
  FinMissionInterimResponse,
} from '../../core/models/fin-mission-interim.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-DT-18-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-18 (indemnité fin mission intérim).
 */
export type FMIAlertField = 'SALAIRE_MENSUEL';

export type FMIAlertSource = CoherenceAlertSource;
export type FMICoherenceAlert = CoherenceAlert<FMIAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on affiche une alerte de cohérence
 * entre le salaire mensuel extrait par l'IA et la saisie de l'avocat.
 * Aligné sur SF-DT-17-02 / template canonique
 * `harcelement-licenciement-nul-section` (10 %).
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-18-02 : outil décisionnel "Indemnité fin mission intérim" (F-DT-18).
 * France uniquement (pas d'équivalent légal forfaitaire en Belgique,
 * loi du 24/07/1987 sur le travail intérimaire). Consomme l'API SF-DT-18-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-18-fin-mission-interim`, règle ALWAYS_ON FR+travail).
 *
 * Pattern de référence STRICT : `indemnite-precarite-cdd-section` (F-DT-17,
 * jumeau direct, structure quasi-identique). Template canonique global :
 * `harcelement-licenciement-nul-section` (SF-155-04-A1).
 *
 * Pré-fill IA sur `salaireMensuelReference` (champ d'aide non envoyé au
 * backend) + alertes cohérence F-IA-03 via `CoherenceAlertBuilder`.
 */
@Component({
  selector: 'app-fin-mission-interim-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './fin-mission-interim-section.component.html',
  styleUrl: './fin-mission-interim-section.component.scss',
})
export class FinMissionInterimSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INDEMNITÉ FIN MISSION INTÉRIM';
  static readonly TOOL_ICON = 'work_history';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  readonly motifExclusionOptions = MOTIF_EXCLUSION_OPTIONS;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<FinMissionInterimResponse | null>(null);

  // Champs d'aide UX (non envoyés au backend, mais utilisés pour pré-fill IA
  // et auto-calcul de totalRemunerationsBrutesEur).
  salaireMensuelReference = signal<number | null>(null);

  // Champs envoyés au backend.
  totalRemunerationsBrutesEur = signal<number | null>(null);
  dureeMissionJours = signal<number | null>(null);
  motifExclusion = signal<MotifExclusionInterim | null>(null);
  dateFinMission = signal<string | null>(null);

  // true si totalRemunerationsBrutesEur a été auto-calculé et que l'avocat
  // n'a pas encore tapé manuellement dedans — permet de ne pas écraser un
  // edit manuel.
  private totalRemunerationsAutoCalc = signal(false);

  provenanceSalaire = signal<'IA' | null>(null);

  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  coherenceAlerts = computed<Partial<Record<FMIAlertField, FMICoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<FMIAlertField, FMICoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE_MENSUEL = salaireAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: FinMissionInterimService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'FRANCE') return;

    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      if (this.salaireMensuelReference() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelReference.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
        this.recomputeTotalRemunerations();
      }
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(field: FMIAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE_MENSUEL' ? 'salaire_brut_mensuel' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: FMICoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: FMICoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  private buildSalaireAlert(): FMICoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelReference();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<FMIAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const salairePiece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'FMI_SALAIRE']);
    if (salairePiece) builder.addPieceManquante(salairePiece);
    return builder.build();
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const total = this.totalRemunerationsBrutesEur();
    const duree = this.dureeMissionJours();
    const dateFin = this.dateFinMission();
    return total !== null && total > 0
        && duree !== null && duree > 0
        && !!dateFin;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
    this.recomputeTotalRemunerations();
  }

  onDureeChange(value: number | null): void {
    this.dureeMissionJours.set(value);
    this.recomputeTotalRemunerations();
  }

  onTotalRemunerationsChange(value: number | null): void {
    this.totalRemunerationsBrutesEur.set(value);
    // L'avocat a saisi manuellement → l'auto-calc n'écrasera plus.
    this.totalRemunerationsAutoCalc.set(false);
  }

  onMotifExclusionChange(value: MotifExclusionInterim | null): void {
    this.motifExclusion.set(value);
  }

  onDateFinMissionChange(value: string | null): void {
    this.dateFinMission.set(value && value.length > 0 ? value : null);
  }

  /**
   * Auto-calcule `totalRemunerationsBrutesEur` si `salaireMensuelReference`
   * et `dureeMissionJours` sont remplis ET que l'avocat n'a pas encore saisi
   * manuellement. Base 30 jours/mois (convention courante intérim).
   */
  private recomputeTotalRemunerations(): void {
    // Ne jamais écraser une saisie manuelle.
    if (!this.totalRemunerationsAutoCalc() && this.totalRemunerationsBrutesEur() !== null) return;
    const s = this.salaireMensuelReference();
    const d = this.dureeMissionJours();
    if (typeof s === 'number' && s > 0 && typeof d === 'number' && d > 0) {
      const total = Math.round(s * d / 30 * 100) / 100;
      this.totalRemunerationsBrutesEur.set(total);
      this.totalRemunerationsAutoCalc.set(true);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: FinMissionInterimRequest = {
      totalRemunerationsBrutesEur: this.totalRemunerationsBrutesEur()!,
      dureeMissionJours: this.dureeMissionJours()!,
      motifExclusion: this.motifExclusion(),
      dateFinMission: this.dateFinMission()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.totalRemunerationsBrutesEur.set(r.totalRemunerationsBrutesEur);
        this.dureeMissionJours.set(r.dureeMissionJours);
        this.motifExclusion.set(r.motifExclusion);
        this.dateFinMission.set(r.dateFinMission);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /**
   * Classe CSS de la bannière de résultat selon que l'exclusion est retenue
   * ou non. Pas de gradation `--danger-medium/-strong/-dark` réservée aux
   * urgences absolues 48h (DESIGN_SYSTEM.md / SF-155-07).
   */
  resultBannerClass(): string {
    const r = this.result();
    if (!r) return '';
    return r.exclusionRetenue ? 'fmi-amount-block fmi-amount-block--danger' : 'fmi-amount-block';
  }
}

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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TravailDissimuleService } from '../../core/services/travail-dissimule.service';
import { TravailDissimuleResponse } from '../../core/models/travail-dissimule.model';
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
 * SF-DT-21-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-21 (indemnité travail dissimulé). Un seul field pré-remplissable
 * aujourd'hui (SALAIRE) — la formule 6 × salaire ne dépend d'aucun autre
 * paramètre. On conserve l'enum pour préserver la forme canonique.
 */
export type TDAlertField = 'SALAIRE';

// Alias locaux — pattern SF-155-05 (interface générique partagée).
export type TDAlertSource = CoherenceAlertSource;
export type TDCoherenceAlert = CoherenceAlert<TDAlertField>;

/**
 * SF-DT-21-02 : seuil d'écart relatif au-delà duquel on affiche une alerte
 * de cohérence entre le salaire extrait par l'IA et la saisie de l'avocat.
 * Aligné avec les autres outils décisionnels (F-DT-11, F-DT-09).
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-21-02 : outil décisionnel dédié "Indemnité forfaitaire pour
 * travail dissimulé" (F-DT-21, art. L.8223-1). FR uniquement — pas
 * d'équivalent législatif en droit belge.
 *
 * Consomme l'API SF-DT-21-01. Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-DT-21-travail-dissimule', règle ALWAYS_ON
 * DROIT_DU_TRAVAIL/FRANCE migration 110).
 *
 * Template canonique : `harcelement-licenciement-nul-section` (F-DT-11).
 * Pattern IA : `immigration-title-decision-section` (F-IM-05).
 * Helper alertes : `CoherenceAlertBuilder` (SF-155-05).
 */
@Component({
  selector: 'app-travail-dissimule-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './travail-dissimule-section.component.html',
  styleUrl: './travail-dissimule-section.component.scss',
})
export class TravailDissimuleSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-DT-21-02 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<TravailDissimuleResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);

  // Provenance IA par champ. 'IA' quand pré-rempli par `prefillFromAi()` ;
  // remis à null dès que l'avocat modifie manuellement (onSalaireChange).
  provenanceSalaire = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // Gate pays : bannière info si BE (règle CLAUDE.md — pas de masquage silencieux).
  isBelgium = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Alertes de cohérence — gate : uniquement en mode formulaire
  // (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<TDAlertField, TDCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<TDAlertField, TDCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: TravailDissimuleService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // Push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // Gate pays — ne pas charger l'API si BE (bannière info suffira).
    if (this.isBelgium()) return;
    this.load();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-appliquer le pré-fill quand `aiData` change avant la première
    // résolution backend. Pas d'écrasement si l'avocat a saisi manuellement
    // (provenance = null) ou si un résultat persisté est déjà affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-remplit le form depuis `aiData`. N'écrase jamais une saisie avocat
   * existante (garde au call site).
   *
   * Note : la formule L.8223-1 = 6 × salaire est indépendante de la durée
   * de dissimulation — aucun pré-fill de durée n'est implémenté.
   * `salaireEstDeduit` est exposé comme hint info (pas un pré-fill).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      if (this.salaireMensuelReference() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelReference.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
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

  explanationFor(field: TDAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel' : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: TDCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: TDCoherenceAlert): string {
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

  /**
   * Divergence salaire — écart relatif > 10 % (SALAIRE_DIVERGENCE_RATIO)
   * entre valeur IA et saisie avocat. Enrichissement 4-sources SF-155-06
   * sur `PIECE_MANQUANTE` si fiche de paie absente.
   */
  private buildSalaireAlert(): TDCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelReference();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<TDAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const salairePiece = this.findPieceManquante(
      ['SALAIRE_BRUT_MENSUEL', 'TRAVAIL_DISSIMULE_SALAIRE']);
    if (salairePiece) builder.addPieceManquante(salairePiece);
    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une `PieceManquanteEntry` dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null`.
   */
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
    const s = this.salaireMensuelReference();
    return s !== null && s > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Handler change salaire — efface le badge IA dès que l'avocat modifie. */
  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
  }

  /**
   * true si l'IA a déduit le salaire brut d'un montant net (× 1,30 —
   * cf. SF-130-01). Note discrète sous le field pour contextualiser.
   */
  salaireEstDeduit(): boolean {
    return this.aiDataSignal()?.salaireEstDeduit === true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
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
        this.salaireMensuelReference.set(r.salaireMensuelReference);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // Fallback pré-fill IA uniquement ici (pas si GET 200).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}

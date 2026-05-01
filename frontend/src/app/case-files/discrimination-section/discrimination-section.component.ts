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
import { DiscriminationService } from '../../core/services/discrimination.service';
import {
  ContexteActe,
  CONTEXTES_ACTE,
  ContexteActeOption,
  DiscriminationResponse,
  MotifDiscrimination,
  MotifDiscriminationOption,
  MOTIFS_DISCRIMINATION_BE,
  MOTIFS_DISCRIMINATION_FR,
} from '../../core/models/discrimination.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-DT-12-02 : champs pré-remplissables depuis l'analyse IA. Palier 1 —
 * seul `SALAIRE` est audité ici (les codes `motifDiscrimination` et
 * `contexteActe` n'étant pas encore exposés par le prompt IA travail).
 */
export type DiscriminationAlertField = 'SALAIRE';

export type DiscriminationCoherenceAlert = CoherenceAlert<DiscriminationAlertField>;

/**
 * SF-DT-12-02 : seuil d'écart relatif au-delà duquel on affiche une alerte
 * de cohérence entre le salaire extrait par l'IA et la saisie de l'avocat.
 * Aligné avec le seuil utilisé par F-DT-09 comparateur indemnités et F-DT-11.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-12-02 : outil décisionnel dédié "Discrimination — dommages-intérêts"
 * (F-DT-12). FR + BE. Consomme l'API SF-DT-12-01.
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * 'F-DT-12-discrimination-dommages-interets').
 *
 * Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02) —
 * même famille "calcul montant FR+BE dual". Pattern IA (signals miroirs +
 * ngOnChanges + provenance par champ) : `immigration-title-decision-section`
 * (F-IM-05-04). Pré-fill IA palier 1 : salaire uniquement, motif et contexte
 * saisis par l'avocat (extension IA palier 2 = future SF).
 */
@Component({
  selector: 'app-discrimination-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './discrimination-section.component.html',
  styleUrl: './discrimination-section.component.scss',
})
export class DiscriminationSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DISCRIMINATION — DOMMAGES-INTÉRÊTS';
  static readonly TOOL_ICON = 'balance';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-DT-12-02 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // SF-DT-12-02 : snapshots signal des inputs IA pour que `computed` réagisse
  // aux changements post-mount (pattern canonique immigration-title-decision).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DiscriminationResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);
  motifDiscrimination = signal<MotifDiscrimination | null>(null);
  contexteActe = signal<ContexteActe | null>(null);

  // SF-DT-12-02 : provenance IA par champ. 'IA' quand le champ a été
  // pré-rempli par `prefillFromAi()` ; remis à null dès que l'avocat
  // modifie manuellement (pattern canonique F-DT-11).
  provenanceSalaire = signal<'IA' | null>(null);

  motifsDisponibles = computed<MotifDiscriminationOption[]>(() =>
    this.workspaceCountry === 'BELGIQUE' ? MOTIFS_DISCRIMINATION_BE : MOTIFS_DISCRIMINATION_FR
  );

  contextesDisponibles: ContexteActeOption[] = CONTEXTES_ACTE;

  // SF-DT-12-02 : alertes de cohérence calculées dynamiquement.
  // Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<DiscriminationAlertField, DiscriminationCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DiscriminationAlertField, DiscriminationCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: DiscriminationService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // SF-DT-12-02 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    // SF-DT-12-02 : ré-hydrater les signals à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-appliquer le pré-fill quand `aiData` change avant première résolution
    // backend. Ne PAS écraser si l'avocat a déjà saisi manuellement (provenance
    // devenue null) OU si un résultat persisté est présent (form masqué).
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-DT-12-02 : pré-remplit le salaire depuis `aiData.salaireBrutMensuel`.
   * N'écrase jamais une saisie avocat existante (la garde est dans le call site
   * et via provenanceSalaire).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      // Ne pré-remplit QUE si le champ est encore vide ou déjà marqué 'IA'.
      if (this.salaireMensuelReference() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelReference.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
      }
    }
    // Palier 2 future SF-DT-12-03 : mapping IA → motifDiscrimination /
    // contexteActe quand le prompt IA travail extraira ces codes.
  }

  alertTooltip(alert: DiscriminationCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DiscriminationCoherenceAlert): string {
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
   * SF-DT-12-02 : divergence salaire — écart relatif > 10 % entre valeur IA
   * et saisie avocat. Via CoherenceAlertBuilder partagé (SF-155-05).
   * Enrichissement PIECE_MANQUANTE (fiche de paie) si disponible (SF-155-06).
   */
  private buildSalaireAlert(): DiscriminationCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelReference();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;

    const builder = CoherenceAlertBuilder.forField<DiscriminationAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    // PIECE_MANQUANTE salaire (fiche de paie) → contributor additionnel.
    const salairePiece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'DISCRIMINATION_SALAIRE']);
    if (salairePiece) builder.addPieceManquante(salairePiece);
    return builder.build();
  }

  /**
   * SF-DT-12-02 : renvoie le `texte` d'une `PieceManquanteEntry` dont
   * `critereCode` (case-insensitive) appartient à la liste des codes
   * acceptés, ou `null` si aucune pièce ne matche. La première gagne.
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
    const m = this.motifDiscrimination();
    const c = this.contexteActe();
    return s !== null && s > 0 && m !== null && c !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * SF-DT-12-02 : handler change salaire — efface le badge IA dès que
   * l'avocat modifie manuellement (pattern canonique).
   */
  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
  }

  onMotifChange(value: MotifDiscrimination | null): void {
    this.motifDiscrimination.set(value);
  }

  onContexteChange(value: ContexteActe | null): void {
    this.contexteActe.set(value);
  }

  /**
   * SF-DT-12-02 : true si l'IA a déduit le salaire brut d'un montant net
   * (via × 1,30 — cf. SF-130-01). On affiche alors une note discrète sous
   * le champ pour contextualiser la valeur pré-remplie.
   */
  salaireEstDeduit(): boolean {
    return this.aiDataSignal()?.salaireEstDeduit === true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
      motifDiscrimination: this.motifDiscrimination()!,
      contexteActe: this.contexteActe()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Fourchette indicative calculée', 'OK', { duration: 2500 });
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
        this.motifDiscrimination.set(r.motifDiscrimination);
        this.contexteActe.set(r.contexteActe);
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

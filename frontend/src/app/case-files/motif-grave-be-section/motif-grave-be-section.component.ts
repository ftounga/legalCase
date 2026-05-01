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
import { MotifGraveBeService } from '../../core/services/motif-grave-be.service';
import {
  MotifGraveBeRequest,
  MotifGraveBeResponse,
} from '../../core/models/motif-grave-be.model';
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
 * SF-DT-27-02 : champs d'alerte F-IA-03 exposés par l'outil F-DT-27
 * (motif grave BE). Seulement les 2 fields alimentés par l'IA travail
 * (`dateLicenciement` → DATE_RUPTURE, `salaireBrutMensuel` → SALAIRE).
 * Les 3 autres champs (dateConnaissanceFait, dateNotificationMotifs,
 * anciennetteAnnees) ne sont pas extraits par le prompt IA travail
 * actuel — pas d'alerte possible à ce jour.
 */
export type MotifGraveBeAlertField = 'DATE_RUPTURE' | 'SALAIRE';

// SF-155-05 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type MotifGraveBeAlertSource = CoherenceAlertSource;
export type MotifGraveBeCoherenceAlert = CoherenceAlert<MotifGraveBeAlertField>;

/**
 * SF-155-04-A1 : seuil d'écart relatif au-delà duquel on affiche une alerte
 * de cohérence entre le salaire extrait par l'IA et la saisie de l'avocat.
 * Aligné avec le seuil utilisé par F-DT-09 / F-DT-11.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-27-02 : outil décisionnel dédié "Motif grave BE — validité
 * des 2 délais de 3 jours ouvrables de l'art. 35 Loi 03/07/1978 +
 * conséquence indemnitaire (préavis + CCT 109)". BE uniquement.
 *
 * L'équivalent FR (faute grave disciplinaire) est couvert par F-DT-08
 * et F-DT-01 — si `workspaceCountry !== 'BELGIQUE'`, affichage d'une
 * bannière info (pas de masquage silencieux). Consomme l'API
 * SF-DT-27-01 (PR #497). Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-DT-27-motif-grave-be', règle ALWAYS_ON BE).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Pattern IA : `immigration-title-decision-section` (F-IM-05).
 * - Pattern gate BE-only : `annexe13-be-section` (F-IM-08-06).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-motif-grave-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './motif-grave-be-section.component.html',
  styleUrl: './motif-grave-be-section.component.scss',
})
export class MotifGraveBeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'MOTIF GRAVE BE (ART. 35 LOI 03/07/1978)';
  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  // SF-155-04 : inputs IA (tous optionnels — null-safe partout).
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
  result = signal<MotifGraveBeResponse | null>(null);

  dateConnaissanceFait = signal<string | null>(null);
  dateNotificationRupture = signal<string | null>(null);
  dateNotificationMotifs = signal<string | null>(null);
  anciennetteAnnees = signal<number | null>(null);
  salaireMensuelReference = signal<number | null>(null);

  // Provenance IA par champ. 'IA' quand pré-rempli par prefillFromAi() ;
  // remis à null dès que l'avocat modifie manuellement (onXxxChange).
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<MotifGraveBeAlertField, MotifGraveBeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<MotifGraveBeAlertField, MotifGraveBeCoherenceAlert>> = {};
    const dateA = this.buildDateRuptureAlert();
    if (dateA) alerts.DATE_RUPTURE = dateA;
    const salaireA = this.buildSalaireAlert();
    if (salaireA) alerts.SALAIRE = salaireA;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // Motif grave = qualification juridique, pas urgence — jamais de CRITICAL ici.
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: MotifGraveBeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // Push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isBelgium()) {
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

    // Ré-applique le prefill si aiData change, seulement si l'on est encore en
    // mode formulaire ET qu'aucun résultat persisté n'a été chargé
    // (évite d'écraser la saisie avocat ou un résultat backend existant).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : map field → sourceKey (fallback générique motif-grave-be). */
  explanationFor(field: MotifGraveBeAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel' : 'MOTIF_GRAVE_BE_DATE_RUPTURE';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: MotifGraveBeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: MotifGraveBeCoherenceAlert): string {
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
   * Divergence salaire — écart relatif > 10 % entre IA et saisie avocat.
   * Construit via CoherenceAlertBuilder partagé (SF-155-05).
   */
  private buildSalaireAlert(): MotifGraveBeCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelReference();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<MotifGraveBeAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const salairePiece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'MOTIF_GRAVE_BE_SALAIRE']);
    if (salairePiece) builder.addPieceManquante(salairePiece);
    return builder.build();
  }

  /**
   * Divergence `dateLicenciement` IA vs `dateNotificationRupture` saisie.
   * Dans le contexte belge, la date du licenciement = date de notification
   * de la rupture (art. 35 al. 2). Toute différence est suspecte.
   */
  private buildDateRuptureAlert(): MotifGraveBeCoherenceAlert | null {
    const userDate = this.dateNotificationRupture();
    if (!userDate) return null;
    const builder = CoherenceAlertBuilder.forField<MotifGraveBeAlertField>('DATE_RUPTURE')
      .withSeverity('WARNING');
    const ai = this.aiDataSignal();
    const aiDate = typeof ai?.dateLicenciement === 'string' ? ai.dateLicenciement : null;
    if (aiDate && aiDate !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de licenciement ${aiDate}`,
      });
    }
    const piece = this.findPieceManquante(['MOTIF_GRAVE_BE_DATE_RUPTURE', 'DATE_LICENCIEMENT']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une PieceManquanteEntry dont `critereCode`
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
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const dConn = this.dateConnaissanceFait();
    const dRup = this.dateNotificationRupture();
    const dMot = this.dateNotificationMotifs();
    const anc = this.anciennetteAnnees();
    const sal = this.salaireMensuelReference();
    if (!dConn || !dRup || !dMot) return false;
    // Dates non futures
    if (dConn > this.todayIso) return false;
    if (dRup > this.todayIso) return false;
    if (dMot > this.todayIso) return false;
    // Ordre chronologique (rupture ≥ connaissance, motifs ≥ rupture)
    if (dRup < dConn) return false;
    if (dMot < dRup) return false;
    if (anc === null || anc === undefined || anc < 0 || !Number.isInteger(anc)) return false;
    if (sal === null || sal === undefined || sal <= 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onDateConnaissanceChange(value: string | null): void {
    this.dateConnaissanceFait.set(value || null);
  }

  onDateRuptureChange(value: string | null): void {
    this.dateNotificationRupture.set(value || null);
    this.provenanceDateRupture.set(null);
  }

  onDateMotifsChange(value: string | null): void {
    this.dateNotificationMotifs.set(value || null);
  }

  onAnciennetteChange(value: number | null | undefined): void {
    this.anciennetteAnnees.set(value === null || value === undefined ? null : value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
  }

  /**
   * SF-155-04 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - chaque champ indépendant (prefill partiel OK)
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. dateLicenciement → dateNotificationRupture.
    if (typeof ai.dateLicenciement === 'string' && ai.dateLicenciement.length > 0) {
      if (this.dateNotificationRupture() === null || this.provenanceDateRupture() === 'IA') {
        this.dateNotificationRupture.set(ai.dateLicenciement);
        this.provenanceDateRupture.set('IA');
      }
    }

    // 2. salaireBrutMensuel → salaireMensuelReference.
    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      if (this.salaireMensuelReference() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelReference.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
      }
    }
  }

  /** SF-155-04 : true si l'IA a déduit le salaire brut d'un montant net (×1,30). */
  salaireEstDeduit(): boolean {
    return this.aiDataSignal()?.salaireEstDeduit === true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: MotifGraveBeRequest = {
      dateConnaissanceFait: this.dateConnaissanceFait()!,
      dateNotificationRupture: this.dateNotificationRupture()!,
      dateNotificationMotifs: this.dateNotificationMotifs()!,
      anciennetteAnnees: this.anciennetteAnnees()!,
      salaireMensuelReference: this.salaireMensuelReference()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Motif grave BE analysé', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateConnaissanceFait.set(r.dateConnaissanceFait);
        this.dateNotificationRupture.set(r.dateNotificationRupture);
        this.dateNotificationMotifs.set(r.dateNotificationMotifs);
        this.anciennetteAnnees.set(r.anciennetteAnnees);
        this.salaireMensuelReference.set(r.salaireMensuelReference);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceDateRupture.set(null);
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

  /**
   * Classe CSS du bandeau de résultat. Navy si valide (qualification
   * juridique favorable à l'employeur procéduralement), or si invalide
   * (conséquence indemnitaire pour l'employeur). Pas de rouge : le
   * motif grave invalide n'est pas une urgence temporelle critique.
   */
  bannerClass(valide: boolean | undefined): string {
    if (valide === true) return 'motif-grave-banner motif-grave-banner--info';
    if (valide === false) return 'motif-grave-banner motif-grave-banner--warning';
    return 'motif-grave-banner';
  }

  bannerIcon(valide: boolean | undefined): string {
    if (valide === true) return 'check_circle';
    if (valide === false) return 'warning';
    return 'info_outline';
  }

  bannerTitle(valide: boolean | undefined): string {
    if (valide === true) return 'Motif grave procéduralement valable';
    if (valide === false) return 'Motif grave procéduralement invalide';
    return '';
  }
}

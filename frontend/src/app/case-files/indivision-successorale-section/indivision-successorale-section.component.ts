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
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { IndivisionSuccessoraleService } from '../../core/services/indivision-successorale.service';
import {
  DispositifRecommandeIndivision,
  DISPOSITIF_RECOMMANDE_INDIVISION_LABELS,
  IndivisionSuccessoraleRequest,
  IndivisionSuccessoraleResponse,
  TypeIndivisionSuccessorale,
  TYPE_INDIVISION_SUCCESSORALE_LABELS,
  VerdictGestionIndivision,
  VERDICT_GESTION_INDIVISION_LABELS,
} from '../../core/models/indivision-successorale.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-FA-24-12 : champs d'alerte F-IA-03 exposés par l'outil "Indivision
 * successorale".
 * - DATE_OUVERTURE_SUCCESSION : divergence sur la date d'ouverture.
 * - TYPE_INDIVISION : divergence sur le type d'indivision détecté.
 */
export type IndivisionSuccessoraleAlertField =
  | 'DATE_OUVERTURE_SUCCESSION'
  | 'TYPE_INDIVISION';

export type IndivisionSuccessoraleAlertSource = CoherenceAlertSource;
export type IndivisionSuccessoraleCoherenceAlert =
  CoherenceAlert<IndivisionSuccessoraleAlertField>;

/**
 * SF-FA-24-12 : outil décisionnel "Indivision successorale" (FR — art. 815 à
 * 832-2 + 1873-1 et s. + 815-1 et s. Cciv : 3 régimes + verdict gestion +
 * indemnité occupation art. 815-9 + frais gestion + dispositif partage).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC BE art. 577-2+
 * traité dans la feature jumelle backlog F-FA-24-BE-indivision).
 *
 * Consomme l'API SF-FA-24-11 (mergée PR #681). Affiché conditionnellement par
 * le panel F-IA-04 (tool_id 'F-FA-24-indivision-successorale' — migration
 * UUID `f1a04001-0000-0000-0000-ee0000000189`).
 *
 * Pattern de référence : `donation-section` (PR #678, F-FA-24 jumeau).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-indivision-successorale-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './indivision-successorale-section.component.html',
  styleUrl: './indivision-successorale-section.component.scss',
})
export class IndivisionSuccessoraleSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndivisionSuccessoraleResponse | null>(null);

  // Form fields.
  typeIndivision = signal<TypeIndivisionSuccessorale | null>(null);
  dateOuvertureSuccession = signal<string | null>(null);
  nbHeritiers = signal<number | null>(null);
  valeurPatrimoineIndivisEur = signal<number | null>(null);
  valeurBienOccupeEur = signal<number | null>(null);
  consentementsTous = signal<boolean | null>(null);
  occupationExclusive = signal<boolean | null>(null);
  actesAdministrationContestes = signal<boolean | null>(null);
  demandePartage = signal<boolean | null>(null);

  /** Provenance IA des champs pré-remplis. */
  provenanceTypeIndivision = signal<'IA' | null>(null);
  provenanceDateOuverture = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  coherenceAlerts = computed<
    Partial<Record<IndivisionSuccessoraleAlertField, IndivisionSuccessoraleCoherenceAlert>>
  >(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<
      Record<IndivisionSuccessoraleAlertField, IndivisionSuccessoraleCoherenceAlert>
    > = {};
    const date = this.buildDateOuvertureAlert();
    if (date) alerts.DATE_OUVERTURE_SUCCESSION = date;
    const type = this.buildTypeIndivisionAlert();
    if (type) alerts.TYPE_INDIVISION = type;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly typeOptions: ReadonlyArray<{ code: TypeIndivisionSuccessorale; label: string }> = [
    { code: 'INDIVISION_LEGALE', label: TYPE_INDIVISION_SUCCESSORALE_LABELS.INDIVISION_LEGALE },
    { code: 'INDIVISION_CONVENTIONNELLE',
      label: TYPE_INDIVISION_SUCCESSORALE_LABELS.INDIVISION_CONVENTIONNELLE },
    { code: 'MAINTIEN_FORCE', label: TYPE_INDIVISION_SUCCESSORALE_LABELS.MAINTIEN_FORCE },
  ];

  constructor(
    private service: IndivisionSuccessoraleService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isFrance()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) {
      this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    }

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: IndivisionSuccessoraleCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IndivisionSuccessoraleCoherenceAlert): string {
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

  /** Divergence sur la date d'ouverture de la succession. */
  private buildDateOuvertureAlert(): IndivisionSuccessoraleCoherenceAlert | null {
    const userVal = this.dateOuvertureSuccession();
    if (!userVal) return null;

    const builder = CoherenceAlertBuilder
      .forField<IndivisionSuccessoraleAlertField>('DATE_OUVERTURE_SUCCESSION');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'INDIVISION_DATE_OUVERTURE') continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : ${ev}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'INDIVISION_DATE_OUVERTURE') continue;
      const expected = (q.expectedValue ?? q.answerText ?? '').trim();
      if (!expected || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.dateOuvertureSuccessionDetectee`.
    const iaDate = this.aiDataSignal()?.dateOuvertureSuccessionDetectee?.trim();
    if (iaDate && iaDate !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : ${iaDate}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['ACTE_NOTORIETE', 'ACTE_DECES']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur le type d'indivision. */
  private buildTypeIndivisionAlert(): IndivisionSuccessoraleCoherenceAlert | null {
    const userVal = this.typeIndivision();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
      .forField<IndivisionSuccessoraleAlertField>('TYPE_INDIVISION');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'INDIVISION_TYPE') continue;
      const expected = this.parseTypeFromIa(chk.expectedValue);
      if (!expected || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: this.typeLabel(expected),
        reason: `Checklist procédurale : ${this.typeLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'INDIVISION_TYPE') continue;
      const expected = this.parseTypeFromIa(q.expectedValue ?? q.answerText);
      if (!expected || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.typeLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.typeIndivisionSuccessoraleDetecte`.
    const iaType = this.parseTypeFromIa(this.aiDataSignal()?.typeIndivisionSuccessoraleDetecte);
    if (iaType && iaType !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: this.typeLabel(iaType),
        reason: `Analyse du dossier : ${this.typeLabel(iaType)}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(
      ['INDIVISION', 'INDIVISION_CONVENTION', 'JUGEMENT_MAINTIEN']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Parse un type d'indivision depuis une valeur IA / F-96.
   * Accepte les valeurs canoniques ou des alias courts.
   */
  private parseTypeFromIa(value: string | null | undefined): TypeIndivisionSuccessorale | null {
    if (!value) return null;
    const v = value.toString().trim().toUpperCase();
    if (!v) return null;
    if (v === 'INDIVISION_LEGALE' || v === 'LEGALE' || v === 'LEGAL') return 'INDIVISION_LEGALE';
    if (v === 'INDIVISION_CONVENTIONNELLE' || v === 'CONVENTIONNELLE'
        || v === 'CONVENTION') return 'INDIVISION_CONVENTIONNELLE';
    if (v === 'MAINTIEN_FORCE' || v === 'MAINTIEN' || v === 'FORCE') return 'MAINTIEN_FORCE';
    return null;
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
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (this.typeIndivision() === null) return false;
    const date = this.dateOuvertureSuccession();
    if (!date) return false;
    // Date future = invalide.
    const now = new Date().toISOString().slice(0, 10);
    if (date > now) return false;
    const nb = this.nbHeritiers();
    if (nb === null || nb === undefined || nb < 2 || nb > 50) return false;
    const patr = this.valeurPatrimoineIndivisEur();
    if (patr === null || patr === undefined || patr < 0) return false;
    const occ = this.valeurBienOccupeEur();
    if (occ !== null && occ !== undefined) {
      if (occ < 0) return false;
      if (occ > patr) return false;
    }
    if (this.consentementsTous() === null) return false;
    if (this.occupationExclusive() === null) return false;
    if (this.actesAdministrationContestes() === null) return false;
    if (this.demandePartage() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent provenance IA au 1er changement manuel.

  onTypeIndivisionChange(value: TypeIndivisionSuccessorale | null): void {
    this.typeIndivision.set(value);
    this.provenanceTypeIndivision.set(null);
  }

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureSuccession.set(value || null);
    this.provenanceDateOuverture.set(null);
  }

  onNbHeritiersChange(value: number | null): void {
    this.nbHeritiers.set(value === null || value === undefined ? null : value);
  }

  onValeurPatrimoineChange(value: number | null): void {
    this.valeurPatrimoineIndivisEur.set(
      value === null || value === undefined ? null : value);
  }

  onValeurBienOccupeChange(value: number | null): void {
    this.valeurBienOccupeEur.set(
      value === null || value === undefined ? null : value);
  }

  onConsentementsTousChange(value: boolean | null): void {
    this.consentementsTous.set(value);
  }

  onOccupationExclusiveChange(value: boolean | null): void {
    this.occupationExclusive.set(value);
  }

  onActesAdministrationContestesChange(value: boolean | null): void {
    this.actesAdministrationContestes.set(value);
  }

  onDemandePartageChange(value: boolean | null): void {
    this.demandePartage.set(value);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles :
   * - silencieux si aiData absent
   * - ne pré-remplit que si le champ est encore vide ou marqué IA
   * - n'écrase jamais une saisie avocat (provenance !== 'IA')
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // typeIndivision
    const iaType = this.parseTypeFromIa(ai.typeIndivisionSuccessoraleDetecte);
    if (iaType) {
      if (this.typeIndivision() === null
          || this.provenanceTypeIndivision() === 'IA') {
        this.typeIndivision.set(iaType);
        this.provenanceTypeIndivision.set('IA');
      }
    }

    // dateOuvertureSuccession (réutilise pré-fill SF-FA-24-08)
    const iaDate = ai.dateOuvertureSuccessionDetectee;
    if (iaDate && typeof iaDate === 'string' && iaDate.trim()) {
      if (!this.dateOuvertureSuccession() || this.provenanceDateOuverture() === 'IA') {
        this.dateOuvertureSuccession.set(iaDate);
        this.provenanceDateOuverture.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: IndivisionSuccessoraleRequest = {
      typeIndivision: this.typeIndivision()!,
      dateOuvertureSuccession: this.dateOuvertureSuccession()!,
      nbHeritiers: this.nbHeritiers()!,
      valeurPatrimoineIndivisEur: this.valeurPatrimoineIndivisEur()!,
      valeurBienOccupeEur: this.valeurBienOccupeEur(),
      consentementsTous: this.consentementsTous()!,
      occupationExclusive: this.occupationExclusive()!,
      actesAdministrationContestes: this.actesAdministrationContestes()!,
      demandePartage: this.demandePartage()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indivision successorale analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceTypeIndivision.set(null);
        this.provenanceDateOuverture.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /** Libellé humain du type. */
  typeLabel(code: TypeIndivisionSuccessorale | null | undefined): string {
    if (!code) return '';
    return TYPE_INDIVISION_SUCCESSORALE_LABELS[code] ?? code;
  }

  /** Libellé humain du verdict. */
  verdictLabel(code: VerdictGestionIndivision | null | undefined): string {
    if (!code) return '';
    return VERDICT_GESTION_INDIVISION_LABELS[code] ?? code;
  }

  /** Libellé humain du dispositif recommandé. */
  dispositifLabel(code: string | null | undefined): string {
    if (!code) return '';
    const k = code as DispositifRecommandeIndivision;
    return DISPOSITIF_RECOMMANDE_INDIVISION_LABELS[k] ?? code;
  }

  /**
   * Classe CSS du bandeau verdict :
   * - `BLOCAGE` → critical (rouge), seul cas réservé à la palette rouge.
   * - `CONFLICTUELLE` → warn (or).
   * - `HARMONIEUSE` → info (navy).
   */
  verdictBannerClass(code: VerdictGestionIndivision | null | undefined): string {
    if (!code) return 'indivision-banner indivision-banner--info';
    if (code === 'BLOCAGE') return 'indivision-banner indivision-banner--critical';
    if (code === 'CONFLICTUELLE') return 'indivision-banner indivision-banner--warn';
    return 'indivision-banner indivision-banner--info';
  }

  /** Classe CSS du chip header. */
  verdictChipClass(code: VerdictGestionIndivision | null | undefined): string {
    if (!code) return 'indivision-chip indivision-chip--info';
    if (code === 'BLOCAGE') return 'indivision-chip indivision-chip--critical';
    if (code === 'CONFLICTUELLE') return 'indivision-chip indivision-chip--warn';
    return 'indivision-chip indivision-chip--info';
  }

  /** Icône du bandeau verdict. */
  verdictIcon(code: VerdictGestionIndivision | null | undefined): string {
    if (code === 'BLOCAGE') return 'gpp_bad';
    if (code === 'CONFLICTUELLE') return 'warning';
    return 'verified';
  }

  /** TrackBy messages. */
  trackMessage(_i: number, m: string): string {
    return m;
  }
}

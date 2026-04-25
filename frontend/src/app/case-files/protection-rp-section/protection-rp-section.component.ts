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
import { ProtectionRpService } from '../../core/services/protection-rp.service';
import {
  CRITERE_REGULARITE_LABELS,
  CritereRegularite,
  MOTIF_LICENCIEMENT_LABELS,
  MotifLicenciement,
  PROCEDURE_SUIVIE_LABELS,
  ProcedureSuivie,
  ProtectionRpRequest,
  ProtectionRpResponse,
  STATUT_PROTEGE_LABELS,
  StatutProtege,
  VerdictLegalite,
  mapMotifLicenciementFromIa,
} from '../../core/models/protection-rp.model';
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

/**
 * SF-DT-30-02 : champs d'alerte F-IA-03 exposés par l'outil "Protection RP".
 * - MOTIF_LICENCIEMENT : divergence si IA détecte un motif (ex. INAPTITUDE)
 *   et l'avocat saisit un motif différent (ex. FAUTE_GRAVE).
 */
export type ProtectionRpAlertField = 'MOTIF_LICENCIEMENT';

export type ProtectionRpAlertSource = CoherenceAlertSource;
export type ProtectionRpCoherenceAlert = CoherenceAlert<ProtectionRpAlertField>;

/**
 * SF-DT-30-02 : outil décisionnel "Protection des représentants du
 * personnel" (FR — art. L.2411-1 et s. + L.2422-1 + R.2422-1).
 * 4 critères : statut protégé, procédure d'autorisation IT, autorisation
 * obtenue, licenciement hors instruction en cours.
 *
 * FR uniquement (bannière info si dossier BE — pas masquage silencieux).
 *
 * Consomme l'API SF-DT-30-01 (mergée PR #631). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-DT-30-protection-rp' — migration 166).
 *
 * Patterns de référence :
 * - Pattern jumeau FR-only : `pse-section` (F-DT-14, PR #627).
 * - Pattern jumeau FR-only : `refere-prudhomal-section` (F-DT-34, PR #618).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-protection-rp-section',
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
  templateUrl: './protection-rp-section.component.html',
  styleUrl: './protection-rp-section.component.scss',
})
export class ProtectionRpSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
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
  result = signal<ProtectionRpResponse | null>(null);

  // Form fields
  statutProtege = signal<StatutProtege | null>(null);
  dateExpirationMandat = signal<string | null>(null);
  datePresumeeRupture = signal<string | null>(null);
  procedureSuivie = signal<ProcedureSuivie | null>(null);
  motifLicenciement = signal<MotifLicenciement | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);

  /** Provenance IA pour les champs pré-remplissables (uniquement motif pour V1). */
  provenanceMotifLicenciement = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly statutProtegeOptions = STATUT_PROTEGE_LABELS;
  readonly procedureSuivieOptions = PROCEDURE_SUIVIE_LABELS;
  readonly motifLicenciementOptions = MOTIF_LICENCIEMENT_LABELS;
  readonly critereLabels = CRITERE_REGULARITE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<ProtectionRpAlertField, ProtectionRpCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ProtectionRpAlertField, ProtectionRpCoherenceAlert>> = {};
    const a = this.buildMotifLicenciementAlert();
    if (a) alerts.MOTIF_LICENCIEMENT = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: ProtectionRpService,
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
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le prefill si aiData change post-mount, en mode formulaire
    // ET sans résultat persisté chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: ProtectionRpCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ProtectionRpCoherenceAlert): string {
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
   * Divergence motif de licenciement — IA détecte un motif (ex. INAPTITUDE)
   * et l'avocat saisit un motif différent. Multi-sources F96 / QUESTION_IA /
   * IA / PIECE_MANQUANTE.
   */
  private buildMotifLicenciementAlert(): ProtectionRpCoherenceAlert | null {
    const userMotif = this.motifLicenciement();
    if (!userMotif) return null;

    const builder = CoherenceAlertBuilder.forField<ProtectionRpAlertField>('MOTIF_LICENCIEMENT');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PROTECTION_RP_MOTIF') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = mapMotifLicenciementFromIa(ev);
      if (!expected || expected === userMotif) continue;
      builder.addSource('F96', {
        expectedDisplay: this.motifLabel(expected),
        reason: `Checklist procédurale : motif attendu ${this.motifLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PROTECTION_RP_MOTIF') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = mapMotifLicenciementFromIa(ev);
      if (!expected || expected === userMotif) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.motifLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.motifLicenciement` mappé.
    const iaMotifRaw = this.aiDataSignal()?.motifLicenciement;
    const iaMotif = mapMotifLicenciementFromIa(iaMotifRaw ?? null);
    if (iaMotif && iaMotif !== userMotif) {
      builder.addSource('IA', {
        expectedDisplay: this.motifLabel(iaMotif),
        reason: `Analyse du dossier : motif détecté "${this.motifLabel(iaMotif)}"`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['PROTECTION_RP_MOTIF', 'MOTIF_LICENCIEMENT']);
    if (piece) builder.addPieceManquante(piece);

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
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (!this.statutProtege()) return false;
    if (!this.dateExpirationMandat()) return false;
    if (!this.datePresumeeRupture()) return false;
    if (!this.procedureSuivie()) return false;
    if (!this.motifLicenciement()) return false;
    const sal = this.salaireMensuelBrutEur();
    if (sal !== null && sal !== undefined && sal < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onStatutProtegeChange(value: StatutProtege | null): void {
    this.statutProtege.set(value);
  }

  onDateExpirationMandatChange(value: string | null): void {
    this.dateExpirationMandat.set(value || null);
  }

  onDatePresumeeRuptureChange(value: string | null): void {
    this.datePresumeeRupture.set(value || null);
  }

  onProcedureSuivieChange(value: ProcedureSuivie | null): void {
    this.procedureSuivie.set(value);
  }

  onMotifLicenciementChange(value: MotifLicenciement | null): void {
    this.motifLicenciement.set(value);
    this.provenanceMotifLicenciement.set(null);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value === null || value === undefined ? null : value);
  }

  /**
   * SF-DT-30-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // motifLicenciement ← aiData.motifLicenciement mappé.
    const iaMotif = mapMotifLicenciementFromIa(ai.motifLicenciement ?? null);
    if (iaMotif) {
      if (this.motifLicenciement() === null
          || this.provenanceMotifLicenciement() === 'IA') {
        this.motifLicenciement.set(iaMotif);
        this.provenanceMotifLicenciement.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ProtectionRpRequest = {
      statutProtege: this.statutProtege()!,
      dateExpirationMandat: this.dateExpirationMandat()!,
      datePresumeeRupture: this.datePresumeeRupture()!,
      procedureSuivie: this.procedureSuivie()!,
      motifLicenciement: this.motifLicenciement()!,
    };
    const sal = this.salaireMensuelBrutEur();
    if (sal !== null && sal !== undefined) {
      request.salaireMensuelBrutEur = sal;
    }
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Protection RP analysée', 'OK', { duration: 2500 });
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
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceMotifLicenciement.set(null);
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
   * Classe CSS du bandeau verdict.
   * - VALIDE → navy/info.
   * - CONTESTABLE → or/warning.
   * - NUL → rouge/critical (palette urgence — vice de procédure absolu, L.2422-1).
   */
  bannerClass(verdict: VerdictLegalite | string | undefined | null): string {
    switch (verdict) {
      case 'VALIDE': return 'protection-rp-banner protection-rp-banner--info';
      case 'CONTESTABLE': return 'protection-rp-banner protection-rp-banner--warning';
      case 'NUL': return 'protection-rp-banner protection-rp-banner--critical';
      default: return 'protection-rp-banner protection-rp-banner--info';
    }
  }

  /** Libellé humain d'un critère de régularité. */
  critereLabel(code: CritereRegularite | string | null | undefined): string {
    if (!code) return '';
    const label = (this.critereLabels as Record<string, string>)[code as string];
    return label ?? code;
  }

  /** Libellé humain d'un motif. */
  motifLabel(code: MotifLicenciement): string {
    const opt = MOTIF_LICENCIEMENT_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}

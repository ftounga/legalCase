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
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RevisionsPostDivorceService } from '../../core/services/revisions-post-divorce.service';
import {
  ModeResidence,
  MODES_RESIDENCE_FR,
  RevisionsPostDivorceRequest,
  RevisionsPostDivorceResponse,
  TYPES_REVISION_FR,
  TypeRevisionPostDivorce,
  VerdictRevisionPossible,
  modeResidenceLabel,
  typeRevisionLabel,
  verdictRevisionLabel,
} from '../../core/models/revisions-post-divorce.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-FA-13-02 : champs audités par F-IA-03 pour cet outil.
 *
 * - `REVENUS_DEBITEUR_ACTUEL` : revenus actuels du débiteur (PENSION /
 *   PRESTATION_COMPENSATOIRE).
 * - `REVENUS_CREANCIER_ACTUEL` : revenus actuels du créancier (PENSION /
 *   PRESTATION_COMPENSATOIRE).
 * - `NB_ENFANTS` : nombre d'enfants à charge (RESIDENCE / DROIT_VISITE).
 */
export type RevisionsPostDivorceAlertField =
  | 'REVENUS_DEBITEUR_ACTUEL'
  | 'REVENUS_CREANCIER_ACTUEL'
  | 'NB_ENFANTS';

export type RevisionsPostDivorceCoherenceAlert =
  CoherenceAlert<RevisionsPostDivorceAlertField>;

/** Seuil divergence revenus (10 %) — aligné F-FA-08 / F-FA-09 / F-DT-09. */
const REVENUS_DIVERGENCE_RATIO = 0.10;

/** Longueur minimale du champ `changementCirconstance`. */
const MIN_CHANGEMENT_LEN = 20;

/**
 * SF-FA-13-02 : composant outil décisionnel "Révisions post-divorce" (FR).
 *
 * Pattern emprunté au canonique `harcelement-licenciement-nul-section`
 * (palette navy / or — pas de rouge dominant) + `divorce-faute-section`
 * (mat-select + scoring) + `divorce-alteration-section` (verdict probabilité).
 *
 * Outil single-country FR. La gate `workspaceCountry === 'BELGIQUE'` masque
 * le formulaire et redirige vers F-FA-21 (à venir).
 *
 * Articles juridiques :
 * - Pension alimentaire : art. 373-2-13 Cciv
 * - Prestation compensatoire : art. 276-3 Cciv
 * - Résidence / DVH : art. 373-2-9 Cciv
 * - Déménagement parent : art. 373-2 Cciv
 */
@Component({
  selector: 'app-revisions-post-divorce-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule, MatChipsModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './revisions-post-divorce-section.component.html',
  styleUrl: './revisions-post-divorce-section.component.scss',
})
export class RevisionsPostDivorceSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉVISIONS POST-DIVORCE (FR)';
  static readonly TOOL_ICON = 'history';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  /** SF-FA-13-02 : pré-fill IA Famille — facultatif, no-op si absent. */
  @Input() aiData?: Partial<FamilleExtractedData> | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signals
  private aiDataSignal = signal<Partial<FamilleExtractedData> | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RevisionsPostDivorceResponse | null>(null);

  // Form signals
  typeRevision = signal<TypeRevisionPostDivorce | null>(null);
  dateDecisionInitiale = signal<string | null>(null);
  changementCirconstance = signal<string>('');

  revenusInitialsDebiteurEur = signal<number | null>(null);
  revenusActuelsDebiteurEur = signal<number | null>(null);
  revenusInitialsCreancierEur = signal<number | null>(null);
  revenusActuelsCreancierEur = signal<number | null>(null);

  nbEnfantsACharge = signal<number | null>(null);
  ageEnfants = signal<number[]>([]);
  modeResidenceActuel = signal<ModeResidence | null>(null);
  modeResidenceDemande = signal<ModeResidence | null>(null);

  // Provenance IA
  provenanceRevenusActuelsDebiteur = signal<'IA' | null>(null);
  provenanceRevenusActuelsCreancier = signal<'IA' | null>(null);
  provenanceNbEnfants = signal<'IA' | null>(null);

  readonly typesOptions = TYPES_REVISION_FR;
  readonly modesResidenceOptions = MODES_RESIDENCE_FR;

  /** Display helpers. */
  typeRevisionLabel = typeRevisionLabel;
  modeResidenceLabel = modeResidenceLabel;
  verdictRevisionLabel = verdictRevisionLabel;

  // ---------------------------------------------------------------------------
  // Conditional visibility computeds
  // ---------------------------------------------------------------------------

  /** Vrai si le typeRevision sélectionné est financier (pension / prestation). */
  showRevenusFields = computed(() => {
    const t = this.typeRevision();
    return t === 'PENSION_ALIMENTAIRE' || t === 'PRESTATION_COMPENSATOIRE';
  });

  /** Vrai si le typeRevision sélectionné implique enfants / résidence. */
  showResidenceFields = computed(() => {
    const t = this.typeRevision();
    return t === 'RESIDENCE' || t === 'DROIT_VISITE';
  });

  /** Vrai si le typeRevision sélectionné est un déménagement parent. */
  showDemenagementHint = computed(() => this.typeRevision() === 'DEMENAGEMENT_PARENT');

  /**
   * SF-FA-13-02 : alertes cohérence (gate showForm). Seuls les fields visibles
   * pour le typeRevision sélectionné contribuent.
   */
  coherenceAlerts = computed<Partial<Record<RevisionsPostDivorceAlertField, RevisionsPostDivorceCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RevisionsPostDivorceAlertField, RevisionsPostDivorceCoherenceAlert>> = {};

    if (this.showRevenusFields()) {
      const ai = this.aiDataSignal() ?? {};
      const debAlert = this.buildRevenusAlert(
        'REVENUS_DEBITEUR_ACTUEL',
        this.revenusActuelsDebiteurEur(),
        ai.revenusAnnuelsEpoux1Eur,
        'FA13_REVENUS_DEBITEUR_ACTUEL',
      );
      if (debAlert) alerts.REVENUS_DEBITEUR_ACTUEL = debAlert;
      const creAlert = this.buildRevenusAlert(
        'REVENUS_CREANCIER_ACTUEL',
        this.revenusActuelsCreancierEur(),
        ai.revenusAnnuelsEpoux2Eur,
        'FA13_REVENUS_CREANCIER_ACTUEL',
      );
      if (creAlert) alerts.REVENUS_CREANCIER_ACTUEL = creAlert;
    }

    if (this.showResidenceFields()) {
      const nbAlert = this.buildNbEnfantsAlert();
      if (nbAlert) alerts.NB_ENFANTS = nbAlert;
    }

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: RevisionsPostDivorceService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.load();
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  isFranceGate(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  // ---------------------------------------------------------------------------
  // Form / handlers
  // ---------------------------------------------------------------------------

  formValid(): boolean {
    if (!this.isFranceGate()) return false;
    if (!this.typeRevision()) return false;
    if (!this.dateDecisionInitiale()) return false;
    if (this.changementCirconstance().trim().length < MIN_CHANGEMENT_LEN) return false;
    return true;
  }

  onTypeRevisionChange(value: TypeRevisionPostDivorce | null): void {
    this.typeRevision.set(value);
  }

  onDateDecisionChange(value: string | null): void {
    this.dateDecisionInitiale.set(value || null);
  }

  onChangementCirconstanceChange(value: string | null): void {
    this.changementCirconstance.set(value ?? '');
  }

  onRevenusInitialsDebiteurChange(value: number | null): void {
    this.revenusInitialsDebiteurEur.set(value);
  }

  onRevenusActuelsDebiteurChange(value: number | null): void {
    this.revenusActuelsDebiteurEur.set(value);
    this.provenanceRevenusActuelsDebiteur.set(null);
  }

  onRevenusInitialsCreancierChange(value: number | null): void {
    this.revenusInitialsCreancierEur.set(value);
  }

  onRevenusActuelsCreancierChange(value: number | null): void {
    this.revenusActuelsCreancierEur.set(value);
    this.provenanceRevenusActuelsCreancier.set(null);
  }

  onNbEnfantsChange(value: number | null): void {
    this.nbEnfantsACharge.set(value);
    this.provenanceNbEnfants.set(null);
  }

  onAgeEnfantsAdd(value: string | null): void {
    if (!value) return;
    const n = Number(value);
    if (!Number.isFinite(n) || n < 0 || n > 30) return;
    this.ageEnfants.update((arr) => [...arr, Math.round(n)]);
  }

  onAgeEnfantRemove(index: number): void {
    this.ageEnfants.update((arr) => arr.filter((_, i) => i !== index));
  }

  onModeResidenceActuelChange(value: ModeResidence | null): void {
    this.modeResidenceActuel.set(value);
  }

  onModeResidenceDemandeChange(value: ModeResidence | null): void {
    this.modeResidenceDemande.set(value);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  /**
   * SF-FA-13-02 : pré-remplit les revenus actuels et le nombre d'enfants depuis
   * `aiData`. Le mapping débiteur/créancier suit la convention :
   * - revenusAnnuelsEpoux1Eur → débiteur (par défaut)
   * - revenusAnnuelsEpoux2Eur → créancier
   *
   * L'avocat peut inverser manuellement si nécessaire ; le badge IA est alors
   * effacé via `onRevenusActuels*Change`.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    if (typeof ai.revenusAnnuelsEpoux1Eur === 'number'
        && ai.revenusAnnuelsEpoux1Eur > 0) {
      if (this.revenusActuelsDebiteurEur() === null
          || this.provenanceRevenusActuelsDebiteur() === 'IA') {
        this.revenusActuelsDebiteurEur.set(ai.revenusAnnuelsEpoux1Eur);
        this.provenanceRevenusActuelsDebiteur.set('IA');
      }
    }
    if (typeof ai.revenusAnnuelsEpoux2Eur === 'number'
        && ai.revenusAnnuelsEpoux2Eur > 0) {
      if (this.revenusActuelsCreancierEur() === null
          || this.provenanceRevenusActuelsCreancier() === 'IA') {
        this.revenusActuelsCreancierEur.set(ai.revenusAnnuelsEpoux2Eur);
        this.provenanceRevenusActuelsCreancier.set('IA');
      }
    }
    // Nombre d'enfants : peut venir de aiData via clé optionnelle.
    const nbEnfantsIa = (ai as { nbEnfantsACharge?: number | null }).nbEnfantsACharge;
    if (typeof nbEnfantsIa === 'number' && nbEnfantsIa >= 0) {
      if (this.nbEnfantsACharge() === null || this.provenanceNbEnfants() === 'IA') {
        this.nbEnfantsACharge.set(nbEnfantsIa);
        this.provenanceNbEnfants.set('IA');
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Coherence alerts builders
  // ---------------------------------------------------------------------------

  private buildRevenusAlert(
    field: 'REVENUS_DEBITEUR_ACTUEL' | 'REVENUS_CREANCIER_ACTUEL',
    userValue: number | null,
    iaValue: number | null | undefined,
    critereCode: string,
  ): RevisionsPostDivorceCoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    if (typeof userValue !== 'number' || userValue <= 0) return null;

    const builder = CoherenceAlertBuilder.forField<RevisionsPostDivorceAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode.toUpperCase()) continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null || ev <= 0) continue;
      const ratio = Math.abs(userValue - ev) / ev;
      if (ratio <= REVENUS_DIVERGENCE_RATIO) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formatEuros(ev),
        reason: `Checklist procédurale : revenus ${this.formatEuros(ev)}`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // 2. QUESTION_IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode.toUpperCase()) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = this.parseNumber(q.expectedValue);
      if (ev === null || ev <= 0) continue;
      const ratio = Math.abs(userValue - ev) / ev;
      if (ratio <= REVENUS_DIVERGENCE_RATIO) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formatEuros(ev),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA aiData
    if (typeof iaValue === 'number' && iaValue > 0) {
      const ratio = Math.abs(userValue - iaValue) / iaValue;
      if (ratio > REVENUS_DIVERGENCE_RATIO) {
        builder.addSource('IA', {
          expectedDisplay: this.formatEuros(iaValue),
          reason: `Analyse du dossier : revenus ${this.formatEuros(iaValue)}`,
        });
      }
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildNbEnfantsAlert(): RevisionsPostDivorceCoherenceAlert | null {
    if (!this.isFranceGate()) return null;
    const user = this.nbEnfantsACharge();
    if (user === null || user < 0) return null;

    const builder = CoherenceAlertBuilder.forField<RevisionsPostDivorceAlertField>('NB_ENFANTS');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA13_NB_ENFANTS') continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null || ev < 0) continue;
      if (ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: `${ev} enfant(s)`,
        reason: `Checklist procédurale : ${ev} enfant(s) à charge`
          + (chk.raison ? ' (' + chk.raison + ')' : ''),
      });
      break;
    }

    // 2. IA aiData (clé optionnelle).
    const iaNb = (this.aiDataSignal() as { nbEnfantsACharge?: number | null } | null | undefined)
      ?.nbEnfantsACharge;
    if (typeof iaNb === 'number' && iaNb >= 0 && iaNb !== user) {
      builder.addSource('IA', {
        expectedDisplay: `${iaNb} enfant(s)`,
        reason: `Analyse du dossier : ${iaNb} enfant(s) à charge`,
      });
    }

    // 3. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['FA13_NB_ENFANTS']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private parseNumber(raw: string | null | undefined): number | null {
    if (raw === null || raw === undefined || raw === '') return null;
    const n = Number(raw);
    return Number.isFinite(n) ? n : null;
  }

  private formatEuros(value: number): string {
    return `${Math.round(value).toLocaleString('fr-FR')} €`;
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

  alertTooltip(alert: RevisionsPostDivorceCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RevisionsPostDivorceCoherenceAlert): string {
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

  // ---------------------------------------------------------------------------
  // Verdict / display helpers
  // ---------------------------------------------------------------------------

  verdictClass(verdict: VerdictRevisionPossible): string {
    switch (verdict) {
      case 'ELEVEE': return 'verdict-banner verdict-banner--ok';
      case 'MOYENNE': return 'verdict-banner verdict-banner--warn';
      case 'FAIBLE': return 'verdict-banner verdict-banner--ko';
    }
  }

  /** Helper formatage signe écart %. */
  ecartFormatted(value: number | null): string {
    if (value === null || value === undefined) return '—';
    const sign = value > 0 ? '+' : '';
    return `${sign}${value.toFixed(1)} %`;
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: RevisionsPostDivorceRequest = {
      typeRevision: this.typeRevision()!,
      dateDecisionInitiale: this.dateDecisionInitiale()!,
      changementCirconstance: this.changementCirconstance().trim(),
      revenusInitialsDebiteurEur: this.revenusInitialsDebiteurEur(),
      revenusActuelsDebiteurEur: this.revenusActuelsDebiteurEur(),
      revenusInitialsCreancierEur: this.revenusInitialsCreancierEur(),
      revenusActuelsCreancierEur: this.revenusActuelsCreancierEur(),
      nbEnfantsACharge: this.nbEnfantsACharge(),
      ageEnfants: this.ageEnfants().length > 0 ? [...this.ageEnfants()] : null,
      modeResidenceActuel: this.modeResidenceActuel(),
      modeResidenceDemande: this.modeResidenceDemande(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResult(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse révision post-divorce calculée', 'OK',
          { duration: 2500 });
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
    if (!this.isFranceGate()) {
      // Pas d'appel backend en BE — l'outil n'est pas disponible.
      return;
    }
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResult(r);
        this.provenanceRevenusActuelsDebiteur.set(null);
        this.provenanceRevenusActuelsCreancier.set(null);
        this.provenanceNbEnfants.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si pas d'analyse persistée — on reste en form mode
        // et on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: RevisionsPostDivorceResponse): void {
    this.typeRevision.set(r.typeRevision);
    this.dateDecisionInitiale.set(r.dateDecisionInitiale);
    this.changementCirconstance.set(r.changementCirconstance);
    this.revenusInitialsDebiteurEur.set(r.revenusInitialsDebiteurEur);
    this.revenusActuelsDebiteurEur.set(r.revenusActuelsDebiteurEur);
    this.revenusInitialsCreancierEur.set(r.revenusInitialsCreancierEur);
    this.revenusActuelsCreancierEur.set(r.revenusActuelsCreancierEur);
    this.nbEnfantsACharge.set(r.nbEnfantsACharge);
    this.ageEnfants.set(r.ageEnfants ?? []);
    this.modeResidenceActuel.set(r.modeResidenceActuel);
    this.modeResidenceDemande.set(r.modeResidenceDemande);
  }
}

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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PartageSuccessoralService } from '../../core/services/partage-successoral.service';
import {
  ModePartage,
  MODE_PARTAGE_LABELS,
  PartageSuccessoralRequest,
  PartageSuccessoralResponse,
  VerdictRecevabilitePartage,
  VERDICT_RECEVABILITE_PARTAGE_LABELS,
} from '../../core/models/partage-successoral.model';
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
import { PartageSuccessoralPrefillRules } from './partage-successoral-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-FA-24-10 : champs d'alerte F-IA-03 exposés par l'outil "Partage
 * successoral".
 * - MODE_PARTAGE : divergence sur la modalité (amiable / judiciaire / partiel).
 * - CONSENTEMENTS : divergence sur le consentement de tous les héritiers.
 * - PRESENCE_IMMEUBLES : divergence sur la présence d'immeubles
 *   (impacte la procédure : notaire obligatoire si oui).
 */
export type PartageSuccessoralAlertField =
  | 'MODE_PARTAGE'
  | 'CONSENTEMENTS'
  | 'PRESENCE_IMMEUBLES';

export type PartageSuccessoralAlertSource = CoherenceAlertSource;
export type PartageSuccessoralCoherenceAlert =
  CoherenceAlert<PartageSuccessoralAlertField>;

/**
 * SF-FA-24-10 : outil décisionnel "Partage successoral" (FR — art.
 * 815-840 Cciv + 1364 CPC). 3 modalités : amiable, judiciaire, partiel.
 *
 * Consomme l'API SF-FA-24-09 (mergée PR #680). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id `F-FA-24-partage-successoral`).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CJ art. 1207
 * traité dans la feature jumelle backlog).
 *
 * Pattern de référence : `donation-section` (PR #678, F-FA-24 jumeau).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-partage-successoral-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './partage-successoral-section.component.html',
  styleUrl: './partage-successoral-section.component.scss',
})
export class PartageSuccessoralSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99d — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-24-partage-successoral';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PARTAGE SUCCESSORAL (FR)';
  static readonly TOOL_ICON = 'handshake';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return PartageSuccessoralPrefillRules.computePrefillCount(input);
  }

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

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PartageSuccessoralResponse | null>(null);

  // Form fields
  modePartageDemande = signal<ModePartage | null>(null);
  nombreCoheritiers = signal<number | null>(null);
  consentementsTous = signal<boolean | null>(null);
  presenceImmeubles = signal<boolean | null>(null);
  accordsValuation = signal<boolean | null>(null);
  desaccordPersistant = signal<boolean | null>(null);
  dateDeces = signal<string | null>(null);
  valeurMasseEur = signal<number | null>(null);

  /** Provenance IA des champs pré-remplis. */
  provenanceMode = signal<'IA' | null>(null);
  provenanceNombreCoheritiers = signal<'IA' | null>(null);
  provenanceDateDeces = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  coherenceAlerts = computed<Partial<Record<
    PartageSuccessoralAlertField,
    PartageSuccessoralCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<
      PartageSuccessoralAlertField,
      PartageSuccessoralCoherenceAlert>> = {};
    const mode = this.buildModeAlert();
    if (mode) alerts.MODE_PARTAGE = mode;
    const consent = this.buildConsentementsAlert();
    if (consent) alerts.CONSENTEMENTS = consent;
    const immeubles = this.buildPresenceImmeublesAlert();
    if (immeubles) alerts.PRESENCE_IMMEUBLES = immeubles;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly modeOptions: ReadonlyArray<{ code: ModePartage; label: string }> = [
    { code: 'PARTAGE_AMIABLE', label: MODE_PARTAGE_LABELS.PARTAGE_AMIABLE },
    { code: 'PARTAGE_JUDICIAIRE', label: MODE_PARTAGE_LABELS.PARTAGE_JUDICIAIRE },
    { code: 'PARTAGE_PARTIEL', label: MODE_PARTAGE_LABELS.PARTAGE_PARTIEL },
  ];

  constructor(
    private service: PartageSuccessoralService,
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
    if (this.isFrance()) {
      this.load();
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
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: PartageSuccessoralCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PartageSuccessoralCoherenceAlert): string {
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

  /** Divergence sur la modalité de partage. */
  private buildModeAlert(): PartageSuccessoralCoherenceAlert | null {
    const userVal = this.modePartageDemande();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
      .forField<PartageSuccessoralAlertField>('MODE_PARTAGE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PARTAGE_MODE') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseModeFromIa(ev);
      if (!expected || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: this.modeLabel(expected),
        reason: `Checklist procédurale : ${this.modeLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PARTAGE_MODE') continue;
      const answer = q.answerText?.trim();
      if (!answer) continue;
      const expected = this.parseModeFromIa(q.expectedValue ?? answer);
      if (!expected || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.modeLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.modePartageDemandeDetecte`.
    const iaMode = this.parseModeFromIa(
      this.aiDataSignal()?.modePartageDemandeDetecte ?? null);
    if (iaMode && iaMode !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: this.modeLabel(iaMode),
        reason: `Analyse du dossier : ${this.modeLabel(iaMode)}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['PARTAGE_MODE', 'ACTE_PARTAGE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur le consentement de tous les héritiers. */
  private buildConsentementsAlert(): PartageSuccessoralCoherenceAlert | null {
    const userVal = this.consentementsTous();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
      .forField<PartageSuccessoralAlertField>('CONSENTEMENTS');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PARTAGE_CONSENTEMENTS') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Tous consentent' : 'Consentements partiels',
        reason: `Checklist procédurale : ${expected ? 'tous les héritiers consentent' : 'consentements partiels'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PARTAGE_CONSENTEMENTS') continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Tous consentent' : 'Consentements partiels',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['PARTAGE_CONSENTEMENTS']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Divergence sur la présence d'immeubles. */
  private buildPresenceImmeublesAlert(): PartageSuccessoralCoherenceAlert | null {
    const userVal = this.presenceImmeubles();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
      .forField<PartageSuccessoralAlertField>('PRESENCE_IMMEUBLES');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PARTAGE_PRESENCE_IMMEUBLES') continue;
      const expected = this.parseBoolFromIa(chk.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Immeubles présents' : 'Aucun immeuble',
        reason: `Checklist procédurale : ${expected ? 'immeubles dans la masse (notaire obligatoire)' : 'aucun immeuble'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PARTAGE_PRESENCE_IMMEUBLES') continue;
      const expected = this.parseBoolFromIa(q.expectedValue);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Immeubles présents' : 'Aucun immeuble',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — pas de champ dédié sur FamilleExtractedData pour
    // `presenceImmeubles` (vue plus structurelle qui sera ajoutée en
    // SF future si besoin). Pas de source IA ici.

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['PARTAGE_PRESENCE_IMMEUBLES', 'TITRE_PROPRIETE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
    return null;
  }

  /**
   * Parse le mode depuis une valeur IA / F-96. Accepte les valeurs
   * canoniques (`PARTAGE_AMIABLE` etc.) ou alias courts (`AMIABLE`,
   * `JUDICIAIRE`, `PARTIEL`).
   */
  private parseModeFromIa(value: string | null | undefined): ModePartage | null {
    if (!value) return null;
    const v = value.toString().trim().toUpperCase();
    if (!v) return null;
    if (v === 'PARTAGE_AMIABLE' || v === 'AMIABLE') return 'PARTAGE_AMIABLE';
    if (v === 'PARTAGE_JUDICIAIRE' || v === 'JUDICIAIRE') return 'PARTAGE_JUDICIAIRE';
    if (v === 'PARTAGE_PARTIEL' || v === 'PARTIEL') return 'PARTAGE_PARTIEL';
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
    if (this.modePartageDemande() === null) return false;
    const n = this.nombreCoheritiers();
    if (n === null || n === undefined || n < 2 || n > 50) return false;
    if (this.consentementsTous() === null) return false;
    if (this.presenceImmeubles() === null) return false;
    if (this.accordsValuation() === null) return false;
    if (this.desaccordPersistant() === null) return false;
    if (!this.dateDeces()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent provenance IA au 1er changement manuel.

  onModePartageChange(value: ModePartage | null): void {
    this.modePartageDemande.set(value);
    this.provenanceMode.set(null);
  }

  onNombreCoheritiersChange(value: number | null): void {
    this.nombreCoheritiers.set(value === null || value === undefined ? null : value);
    this.provenanceNombreCoheritiers.set(null);
  }

  onConsentementsTousChange(value: boolean | null): void {
    this.consentementsTous.set(value);
  }

  onPresenceImmeublesChange(value: boolean | null): void {
    this.presenceImmeubles.set(value);
  }

  onAccordsValuationChange(value: boolean | null): void {
    this.accordsValuation.set(value);
  }

  onDesaccordPersistantChange(value: boolean | null): void {
    this.desaccordPersistant.set(value);
  }

  onDateDecesChange(value: string | null): void {
    this.dateDeces.set(value || null);
    this.provenanceDateDeces.set(null);
  }

  onValeurMasseEurChange(value: number | null): void {
    this.valeurMasseEur.set(value === null || value === undefined ? null : value);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles :
   * - silencieux si aiData absent
   * - ne pré-remplit que si le champ est encore vide ou marqué IA
   * - n'écrase jamais une saisie avocat (provenance !== 'IA')
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };
    const m = PartageSuccessoralPrefillRules.computeModePartage(h);
    if (m !== null && (this.modePartageDemande() === null || this.provenanceMode() === 'IA')) {
      this.modePartageDemande.set(m);
      this.provenanceMode.set('IA');
    }
    const nc = PartageSuccessoralPrefillRules.computeNombreCoheritiers(h);
    if (nc !== null && (this.nombreCoheritiers() === null || this.provenanceNombreCoheritiers() === 'IA')) {
      this.nombreCoheritiers.set(nc);
      this.provenanceNombreCoheritiers.set('IA');
    }
    const dd = PartageSuccessoralPrefillRules.computeDateDeces(h);
    if (dd !== null && (!this.dateDeces() || this.provenanceDateDeces() === 'IA')) {
      this.dateDeces.set(dd);
      this.provenanceDateDeces.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PartageSuccessoralRequest = {
      modePartageDemande: this.modePartageDemande()!,
      nombreCoheritiers: this.nombreCoheritiers()!,
      consentementsTous: this.consentementsTous()!,
      presenceImmeubles: this.presenceImmeubles()!,
      accordsValuation: this.accordsValuation()!,
      desaccordPersistant: this.desaccordPersistant()!,
      dateDeces: this.dateDeces()!,
      valeurMasseEur: this.valeurMasseEur(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Partage successoral analysé', 'OK', { duration: 2500 });
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
        this.provenanceMode.set(null);
        this.provenanceNombreCoheritiers.set(null);
        this.provenanceDateDeces.set(null);
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

  /** Libellé humain du mode. */
  modeLabel(code: ModePartage | null | undefined): string {
    if (!code) return '';
    return MODE_PARTAGE_LABELS[code] ?? code;
  }

  /** Libellé humain du verdict. */
  verdictLabel(code: VerdictRecevabilitePartage | null | undefined): string {
    if (!code) return '';
    return VERDICT_RECEVABILITE_PARTAGE_LABELS[code] ?? code;
  }

  /**
   * Classe CSS du bandeau verdict (palette canonique) :
   * - `FAIBLE` → critical (rouge, alerte critique).
   * - `MOYENNE` → warn (or).
   * - `ELEVEE` → info (navy).
   */
  verdictBannerClass(code: VerdictRecevabilitePartage | null | undefined): string {
    if (!code) return 'partage-banner partage-banner--info';
    if (code === 'FAIBLE') return 'partage-banner partage-banner--critical';
    if (code === 'MOYENNE') return 'partage-banner partage-banner--warn';
    return 'partage-banner partage-banner--info';
  }

  /** Classe CSS du chip header. */
  verdictChipClass(code: VerdictRecevabilitePartage | null | undefined): string {
    if (!code) return 'partage-chip partage-chip--info';
    if (code === 'FAIBLE') return 'partage-chip partage-chip--critical';
    if (code === 'MOYENNE') return 'partage-chip partage-chip--warn';
    return 'partage-chip partage-chip--info';
  }

  /** Icône du bandeau verdict. */
  verdictIcon(code: VerdictRecevabilitePartage | null | undefined): string {
    if (code === 'FAIBLE') return 'gpp_bad';
    if (code === 'MOYENNE') return 'warning';
    return 'verified';
  }
}

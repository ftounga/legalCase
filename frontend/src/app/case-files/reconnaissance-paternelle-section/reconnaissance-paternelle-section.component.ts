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
import { ReconnaissancePaternelleService } from '../../core/services/reconnaissance-paternelle.service';
import {
  ReconnaissancePaternelleRequest,
  ReconnaissancePaternelleResponse,
  SOUS_TYPE_RECONNAISSANCE_LABELS,
  SousTypeReconnaissance,
  VerdictRecevabiliteReconnaissance,
} from '../../core/models/reconnaissance-paternelle.model';
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
import { ReconnaissancePaternellePrefillRules } from './reconnaissance-paternelle-section-prefill-rules';

/**
 * SF-FA-18-02 : champs d'alerte F-IA-03 exposés par l'outil
 * "Reconnaissance paternelle" (art. 316 Cciv). Multi-sources :
 * - F-96 (procedureCheck.expectedValue)
 * - QUESTION_IA (aiQuestion.expectedValue + answerText "oui")
 * - IA (aiData.*Detected)
 * - PIECE_MANQUANTE (contributor enrichissant)
 *
 * 4 critères de validité observables dans la décision.
 */
export type ReconnaissancePaternelleAlertField =
  | 'CONSENTEMENT_LIBRE'
  | 'PATERNITE_VRAISEMBLABLE'
  | 'ENFANT_NON_RECONNU'
  | 'PROCEDURE_RESPECTEE';

export type ReconnaissancePaternelleAlertSource = CoherenceAlertSource;
export type ReconnaissancePaternelleCoherenceAlert =
  CoherenceAlert<ReconnaissancePaternelleAlertField>;

/**
 * SF-FA-18-02 : outil décisionnel "Reconnaissance paternelle"
 * (FR — art. 316 + 332-335 + 372 Cciv).
 *
 * 3 sous-types (prénatale, post-natale acte de naissance, post-natale ultérieure)
 * × 4 critères booléens + procuration éventuelle. Verdict de recevabilité
 * scoring niveau 5.
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC art. 327 et s.
 * au backlog jumeau).
 *
 * Consomme l'API SF-FA-18-01 (mergée PR #652). Affiché conditionnellement par
 * le panel F-IA-04 (tool_id 'F-FA-18-reconnaissance-paternelle' — migration 178).
 *
 * Pattern de référence : `partage-judiciaire-section` (SF-FA-17-02 — PR #638).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-reconnaissance-paternelle-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './reconnaissance-paternelle-section.component.html',
  styleUrl: './reconnaissance-paternelle-section.component.scss',
})
export class ReconnaissancePaternelleSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RECONNAISSANCE PATERNELLE (FR)';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ReconnaissancePaternellePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
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
  result = signal<ReconnaissancePaternelleResponse | null>(null);

  // Form fields
  sousType = signal<SousTypeReconnaissance | null>(null);
  /** ISO YYYY-MM-DD via input type="date". */
  dateNaissanceEnfant = signal<string | null>(null);
  dateReconnaissance = signal<string | null>(null);
  consentementLibreDuPere = signal<boolean | null>(null);
  paterniteVraisemblable = signal<boolean | null>(null);
  enfantNonReconnuParAutrePere = signal<boolean | null>(null);
  procedureRespectee = signal<boolean | null>(null);
  presenceParProcuration = signal<boolean>(false);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceConsentementLibre = signal<'IA' | null>(null);
  provenancePaterniteVraisemblable = signal<'IA' | null>(null);
  provenanceEnfantNonReconnu = signal<'IA' | null>(null);
  provenanceProcedureRespectee = signal<'IA' | null>(null);
  provenanceDateNaissance = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly sousTypeOptions = SOUS_TYPE_RECONNAISSANCE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Sous-type prénatale → pas de date naissance requise. */
  isPrenatal = computed<boolean>(() => this.sousType() === 'RECONNAISSANCE_PRENATALE');

  /**
   * Alertes de cohérence F-IA-03 par field. Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<ReconnaissancePaternelleAlertField,
      ReconnaissancePaternelleCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<ReconnaissancePaternelleAlertField,
        ReconnaissancePaternelleCoherenceAlert>> = {};
    const cl = this.buildConsentementLibreAlert();
    if (cl) alerts.CONSENTEMENT_LIBRE = cl;
    const pv = this.buildPaterniteVraisemblableAlert();
    if (pv) alerts.PATERNITE_VRAISEMBLABLE = pv;
    const en = this.buildEnfantNonReconnuAlert();
    if (en) alerts.ENFANT_NON_RECONNU = en;
    const pr = this.buildProcedureRespecteeAlert();
    if (pr) alerts.PROCEDURE_RESPECTEE = pr;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: ReconnaissancePaternelleService,
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

    // Ré-applique le prefill si aiData change post-mount, en mode formulaire
    // ET sans résultat persisté chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: ReconnaissancePaternelleCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ReconnaissancePaternelleCoherenceAlert): string {
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
   * Helper interne factorisé pour construire une alerte multi-sources sur un
   * champ booléen (CONSENTEMENT_LIBRE / PATERNITE_VRAISEMBLABLE / etc.).
   *
   * @param field code interne (clé de l'enum field)
   * @param userVal valeur saisie par l'avocat (null = pas de saisie → pas d'alerte)
   * @param critereCode code utilisé côté F-96 / QUESTION_IA pour pointer le critère
   * @param iaVal valeur détectée par l'IA (aiData.*Detected)
   * @param trueLabel libellé humain quand attendu = true (ex: "Consentement libre")
   * @param falseLabel libellé humain quand attendu = false (ex: "Vice de consentement")
   * @param fieldLabel libellé du critère pour les phrases reason F96/IA
   */
  private buildBooleanAlert(
    field: ReconnaissancePaternelleAlertField,
    userVal: boolean | null,
    critereCode: string,
    iaVal: boolean | null | undefined,
    trueLabel: string,
    falseLabel: string,
    fieldLabel: string,
  ): ReconnaissancePaternelleCoherenceAlert | null {
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder
        .forField<ReconnaissancePaternelleAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? trueLabel : falseLabel,
        reason: `Checklist procédurale : ${fieldLabel} ${expected ? 'vérifié' : 'non vérifié'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? trueLabel : falseLabel,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.*Detected`.
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? trueLabel : falseLabel,
        reason: `Analyse du dossier : ${fieldLabel} ${iaVal ? 'vérifié' : 'non vérifié'}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildConsentementLibreAlert(): ReconnaissancePaternelleCoherenceAlert | null {
    return this.buildBooleanAlert(
      'CONSENTEMENT_LIBRE',
      this.consentementLibreDuPere(),
      'RECONNAISSANCE_PATERNELLE_CONSENTEMENT',
      this.aiDataSignal()?.consentementLibreDuPereDetected,
      'Consentement libre',
      'Vice de consentement',
      'consentement libre du père',
    );
  }

  private buildPaterniteVraisemblableAlert(): ReconnaissancePaternelleCoherenceAlert | null {
    return this.buildBooleanAlert(
      'PATERNITE_VRAISEMBLABLE',
      this.paterniteVraisemblable(),
      'RECONNAISSANCE_PATERNELLE_VRAISEMBLABLE',
      this.aiDataSignal()?.paterniteVraisemblableDetected,
      'Paternité vraisemblable',
      'Paternité non vraisemblable',
      'paternité vraisemblable',
    );
  }

  private buildEnfantNonReconnuAlert(): ReconnaissancePaternelleCoherenceAlert | null {
    return this.buildBooleanAlert(
      'ENFANT_NON_RECONNU',
      this.enfantNonReconnuParAutrePere(),
      'RECONNAISSANCE_PATERNELLE_ENFANT_NON_RECONNU',
      this.aiDataSignal()?.enfantNonReconnuParAutrePereDetected,
      'Enfant non reconnu par tiers',
      'Enfant déjà reconnu',
      'enfant non reconnu par un autre père',
    );
  }

  private buildProcedureRespecteeAlert(): ReconnaissancePaternelleCoherenceAlert | null {
    return this.buildBooleanAlert(
      'PROCEDURE_RESPECTEE',
      this.procedureRespectee(),
      'RECONNAISSANCE_PATERNELLE_PROCEDURE',
      this.aiDataSignal()?.procedureRespecteeReconnaissanceDetected,
      'Procédure respectée',
      'Procédure non respectée',
      'procédure respectée',
    );
  }

  /**
   * Mappe une chaîne IA / F96 brute vers booléen. Tolère "oui", "true",
   * "vrai", "1", etc. Retourne null si non reconnu.
   */
  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
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
    if (!this.sousType()) return false;
    if (this.consentementLibreDuPere() === null) return false;
    if (this.paterniteVraisemblable() === null) return false;
    if (this.enfantNonReconnuParAutrePere() === null) return false;
    if (this.procedureRespectee() === null) return false;
    // Date naissance requise pour POST_NATALE_*, optionnelle pour PRENATALE.
    if (!this.isPrenatal() && !this.dateNaissanceEnfant()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onSousTypeChange(value: SousTypeReconnaissance | null): void {
    this.sousType.set(value);
  }

  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceEnfant.set(value || null);
    this.provenanceDateNaissance.set(null);
  }

  onDateReconnaissanceChange(value: string | null): void {
    this.dateReconnaissance.set(value || null);
  }

  onConsentementLibreChange(value: boolean | null): void {
    this.consentementLibreDuPere.set(value);
    this.provenanceConsentementLibre.set(null);
  }

  onPaterniteVraisemblableChange(value: boolean | null): void {
    this.paterniteVraisemblable.set(value);
    this.provenancePaterniteVraisemblable.set(null);
  }

  onEnfantNonReconnuChange(value: boolean | null): void {
    this.enfantNonReconnuParAutrePere.set(value);
    this.provenanceEnfantNonReconnu.set(null);
  }

  onProcedureRespecteeChange(value: boolean | null): void {
    this.procedureRespectee.set(value);
    this.provenanceProcedureRespectee.set(null);
  }

  onPresenceProcurationChange(value: boolean): void {
    this.presenceParProcuration.set(value);
  }

  /**
   * SF-FA-18-02 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide / `null` (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const helperInput = { aiData: this.aiDataSignal() };
    const cl = ReconnaissancePaternellePrefillRules.computeConsentementLibre(helperInput);
    if (cl !== null
        && (this.consentementLibreDuPere() === null || this.provenanceConsentementLibre() === 'IA')) {
      this.consentementLibreDuPere.set(cl);
      this.provenanceConsentementLibre.set('IA');
    }
    const pv = ReconnaissancePaternellePrefillRules.computePaterniteVraisemblable(helperInput);
    if (pv !== null
        && (this.paterniteVraisemblable() === null || this.provenancePaterniteVraisemblable() === 'IA')) {
      this.paterniteVraisemblable.set(pv);
      this.provenancePaterniteVraisemblable.set('IA');
    }
    const en = ReconnaissancePaternellePrefillRules.computeEnfantNonReconnu(helperInput);
    if (en !== null
        && (this.enfantNonReconnuParAutrePere() === null || this.provenanceEnfantNonReconnu() === 'IA')) {
      this.enfantNonReconnuParAutrePere.set(en);
      this.provenanceEnfantNonReconnu.set('IA');
    }
    const pr = ReconnaissancePaternellePrefillRules.computeProcedureRespectee(helperInput);
    if (pr !== null
        && (this.procedureRespectee() === null || this.provenanceProcedureRespectee() === 'IA')) {
      this.procedureRespectee.set(pr);
      this.provenanceProcedureRespectee.set('IA');
    }
    const dn = ReconnaissancePaternellePrefillRules.computeDateNaissance(helperInput);
    if (dn !== null
        && (this.dateNaissanceEnfant() === null || this.provenanceDateNaissance() === 'IA')) {
      this.dateNaissanceEnfant.set(dn);
      this.provenanceDateNaissance.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ReconnaissancePaternelleRequest = {
      sousType: this.sousType()!,
      dateNaissanceEnfant: this.dateNaissanceEnfant() || null,
      dateReconnaissance: this.dateReconnaissance() || null,
      consentementLibreDuPere: this.consentementLibreDuPere()!,
      paterniteVraisemblable: this.paterniteVraisemblable()!,
      enfantNonReconnuParAutrePere: this.enfantNonReconnuParAutrePere()!,
      procedureRespectee: this.procedureRespectee()!,
      presenceParProcuration: this.presenceParProcuration(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Reconnaissance paternelle analysée', 'OK', { duration: 2500 });
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
        this.provenanceConsentementLibre.set(null);
        this.provenancePaterniteVraisemblable.set(null);
        this.provenanceEnfantNonReconnu.set(null);
        this.provenanceProcedureRespectee.set(null);
        this.provenanceDateNaissance.set(null);
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
   * - ELEVEE → navy/info (recevabilité forte).
   * - MOYENNE → or/warning (procédure partiellement contestable).
   * - FAIBLE → rouge/critical (vice de consentement, autre père reconnu, etc.).
   */
  bannerClass(verdict: VerdictRecevabiliteReconnaissance | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'rec-pat-banner rec-pat-banner--info';
      case 'MOYENNE': return 'rec-pat-banner rec-pat-banner--warning';
      case 'FAIBLE': return 'rec-pat-banner rec-pat-banner--critical';
      default: return 'rec-pat-banner rec-pat-banner--info';
    }
  }

  /** Libellé humain d'un sous-type. */
  sousTypeLabel(code: SousTypeReconnaissance | null | undefined): string {
    if (!code) return '';
    const opt = SOUS_TYPE_RECONNAISSANCE_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}

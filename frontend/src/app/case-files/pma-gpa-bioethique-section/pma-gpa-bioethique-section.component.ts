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
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PmaGpaBioethiqueService } from '../../core/services/pma-gpa-bioethique.service';
import {
  DispositifBioethique,
  DISPOSITIF_BIOETHIQUE_LABELS,
  PAYS_GPA_OPTIONS,
  PmaGpaBioethiqueRequest,
  PmaGpaBioethiqueResponse,
  VerdictRecevabiliteBioethique,
} from '../../core/models/pma-gpa-bioethique.model';
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
import { PmaGpaBioethiquePrefillRules } from './pma-gpa-bioethique-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-FA-27-02 : champs d'alerte F-IA-03 exposés par l'outil
 * "PMA / GPA / bioéthique".
 *
 * - DISPOSITIF : divergence entre le dispositif sélectionné et celui
 *   détecté par l'IA (`aiData.dispositifBioethiqueDetecte`).
 * - DATE_DON : alerte si l'IA dispose d'une date de don (placeholder —
 *   `FamilleExtractedData` ne le contient pas encore, builder prêt à
 *   recevoir l'extension).
 */
export type PmaGpaAlertField = 'DISPOSITIF' | 'DATE_DON';

export type PmaGpaAlertSource = CoherenceAlertSource;
export type PmaGpaCoherenceAlert = CoherenceAlert<PmaGpaAlertField>;

/**
 * SF-FA-27-02 : outil décisionnel "PMA / GPA / bioéthique" (FR — loi
 * bioéthique 2/8/2021).
 *
 * 3 dispositifs mutuellement exclusifs (art. 342-9 Cciv reconnaissance
 * anticipée PMA / Cass. 18/12/2022 transcription GPA / art. 16-8-1 Cciv
 * accès aux origines).
 *
 * FR uniquement (bannière info si dossier BE — loi 6/7/2007 PMA BE,
 * traitée dans une feature jumelle au backlog).
 *
 * Consomme l'API SF-FA-27-01 (mergée PR #656). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id `'F-FA-27-pma-gpa'`, visibility rule UUID
 * `f1a04001-0000-0000-0000-ee0000000180`, priority 89, ALWAYS_ON FRANCE
 * DROIT_FAMILLE — migration 180).
 *
 * Pattern de référence : `communaute-universelle-section` (PR #654, SF-FA-16-02).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-pma-gpa-bioethique-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './pma-gpa-bioethique-section.component.html',
  styleUrl: './pma-gpa-bioethique-section.component.scss',
})
export class PmaGpaBioethiqueSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e v4 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-27-pma-gpa';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PMA / GPA / BIOÉTHIQUE (FR)';
  static readonly TOOL_ICON = 'child_friendly';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return PmaGpaBioethiquePrefillRules.computePrefillCount(input);
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
  result = signal<PmaGpaBioethiqueResponse | null>(null);

  // Form fields — communs
  dispositif = signal<DispositifBioethique | null>('PMA_RECONNAISSANCE_ANTICIPEE');

  // PMA
  consentementsConjointsNotaire = signal<boolean | null>(null);
  dateReconnaissanceAnterieurePMA = signal<string | null>(null);
  datePMA = signal<string | null>(null);
  conditionsAccesPMA = signal<boolean | null>(null);

  // GPA
  paysGPACode = signal<string | null>(null);
  parentBiologiqueAvere = signal<boolean | null>(null);
  decisionEtrangereProduite = signal<boolean | null>(null);
  adoptionDemande = signal<boolean | null>(null);

  // DON gamètes
  dateDon = signal<string | null>(null);
  demandeAcces = signal<boolean | null>(null);
  ageDemandeur = signal<number | null>(null);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceDispositif = signal<'IA' | null>(null);

  readonly dispositifOptions = DISPOSITIF_BIOETHIQUE_LABELS;
  readonly paysOptions = PAYS_GPA_OPTIONS;

  /** Date d'entrée en vigueur de l'accès aux origines (art. 16-8-1 Cciv). */
  readonly DATE_ACCES_ORIGINES_VIGUEUR = '2022-09-01';

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  isPma = computed<boolean>(() => this.dispositif() === 'PMA_RECONNAISSANCE_ANTICIPEE');
  isGpa = computed<boolean>(() => this.dispositif() === 'GPA_TRANSCRIPTION_ETAT_CIVIL');
  isDon = computed<boolean>(() => this.dispositif() === 'DON_GAMETES_ACCES_ORIGINES');

  /**
   * Date de don postérieure à l'entrée en vigueur (1/9/2022) ?
   * Utilisé pour bandeau d'aide en mode DON.
   */
  isDonPosterieurReforme = computed<boolean>(() => {
    const d = this.dateDon();
    if (!d) return true;
    return d >= this.DATE_ACCES_ORIGINES_VIGUEUR;
  });

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<PmaGpaAlertField, PmaGpaCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PmaGpaAlertField, PmaGpaCoherenceAlert>> = {};
    const d = this.buildDispositifAlert();
    if (d) alerts.DISPOSITIF = d;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: PmaGpaBioethiqueService,
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

  alertTooltip(alert: PmaGpaCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PmaGpaCoherenceAlert): string {
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
   * Divergence du dispositif sélectionné vs IA / F-96 / Question IA.
   * Multi-sources priorisées F96 > QUESTION_IA > IA > PIECE_MANQUANTE.
   */
  private buildDispositifAlert(): PmaGpaCoherenceAlert | null {
    const userVal = this.dispositif();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<PmaGpaAlertField>('DISPOSITIF');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PMA_GPA_DISPOSITIF') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseDispositifFromIa(ev);
      if (!expected || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: this.dispositifLabel(expected),
        reason: `Checklist procédurale : dispositif ${this.dispositifLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PMA_GPA_DISPOSITIF') continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = this.parseDispositifFromIa(ev);
      if (!expected || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.dispositifLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText ?? ''}"`,
      });
      break;
    }

    // 3. IA — `aiData.dispositifBioethiqueDetecte`.
    const iaDispRaw = this.aiDataSignal()?.dispositifBioethiqueDetecte;
    const iaDisp = this.parseDispositifFromIa(iaDispRaw);
    if (iaDisp && iaDisp !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: this.dispositifLabel(iaDisp),
        reason: `Analyse du dossier : dispositif ${this.dispositifLabel(iaDisp)}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['PMA_GPA_DISPOSITIF', 'PMA_GPA_BIOETHIQUE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private parseDispositifFromIa(value: string | null | undefined): DispositifBioethique | null {
    if (!value) return null;
    const v = value.toString().trim().toUpperCase();
    if (v === 'PMA_RECONNAISSANCE_ANTICIPEE'
      || v === 'GPA_TRANSCRIPTION_ETAT_CIVIL'
      || v === 'DON_GAMETES_ACCES_ORIGINES') {
      return v as DispositifBioethique;
    }
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
    if (!this.dispositif()) return false;

    if (this.isPma()) {
      if (this.consentementsConjointsNotaire() === null) return false;
      if (this.conditionsAccesPMA() === null) return false;
    } else if (this.isGpa()) {
      if (this.paysGPACode() === null) return false;
      if (this.parentBiologiqueAvere() === null) return false;
      if (this.decisionEtrangereProduite() === null) return false;
      if (this.adoptionDemande() === null) return false;
    } else if (this.isDon()) {
      if (this.demandeAcces() === null) return false;
      const a = this.ageDemandeur();
      if (a === null || a === undefined || a < 0) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Reset des champs propres aux dispositifs non actifs pour éviter les
   * submits ambigus.
   */
  onDispositifChange(value: DispositifBioethique | null): void {
    this.dispositif.set(value);
    this.provenanceDispositif.set(null);

    if (value !== 'PMA_RECONNAISSANCE_ANTICIPEE') {
      this.consentementsConjointsNotaire.set(null);
      this.dateReconnaissanceAnterieurePMA.set(null);
      this.datePMA.set(null);
      this.conditionsAccesPMA.set(null);
    }
    if (value !== 'GPA_TRANSCRIPTION_ETAT_CIVIL') {
      this.paysGPACode.set(null);
      this.parentBiologiqueAvere.set(null);
      this.decisionEtrangereProduite.set(null);
      this.adoptionDemande.set(null);
    }
    if (value !== 'DON_GAMETES_ACCES_ORIGINES') {
      this.dateDon.set(null);
      this.demandeAcces.set(null);
      this.ageDemandeur.set(null);
    }
  }

  // Handlers onChange (PMA)
  onConsentementsNotaireChange(value: boolean | null): void {
    this.consentementsConjointsNotaire.set(value);
  }
  onDateReconnaissanceChange(value: string | null): void {
    this.dateReconnaissanceAnterieurePMA.set(value || null);
  }
  onDatePMAChange(value: string | null): void {
    this.datePMA.set(value || null);
  }
  onConditionsAccesPMAChange(value: boolean | null): void {
    this.conditionsAccesPMA.set(value);
  }

  // Handlers onChange (GPA)
  onPaysGPAChange(value: string | null): void {
    this.paysGPACode.set(value);
  }
  onParentBiologiqueChange(value: boolean | null): void {
    this.parentBiologiqueAvere.set(value);
  }
  onDecisionEtrangereChange(value: boolean | null): void {
    this.decisionEtrangereProduite.set(value);
  }
  onAdoptionDemandeChange(value: boolean | null): void {
    this.adoptionDemande.set(value);
  }

  // Handlers onChange (DON)
  onDateDonChange(value: string | null): void {
    this.dateDon.set(value || null);
  }
  onDemandeAccesChange(value: boolean | null): void {
    this.demandeAcces.set(value);
  }
  onAgeDemandeurChange(value: number | null): void {
    this.ageDemandeur.set(value === null || value === undefined ? null : value);
  }

  /**
   * SF-FA-27-02 : pré-fill depuis `aiData.dispositifBioethiqueDetecte`.
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le dispositif est encore au défaut PMA et
   *   que la provenance n'est pas une saisie avocat
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé pour la détection.
    const iaDisp = PmaGpaBioethiquePrefillRules.computeDispositif({ aiData: this.aiDataSignal() });
    if (iaDisp) {
      // On considère "champ vide" si encore sur la valeur initiale par défaut
      // ET pas de provenance IA déjà posée → ré-écrasable.
      const isDefault = this.provenanceDispositif() !== 'IA'
        && this.dispositif() === 'PMA_RECONNAISSANCE_ANTICIPEE';
      if (isDefault || this.provenanceDispositif() === 'IA') {
        this.onDispositifChange(iaDisp);
        this.provenanceDispositif.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const d = this.dispositif()!;
    const isPma = d === 'PMA_RECONNAISSANCE_ANTICIPEE';
    const isGpa = d === 'GPA_TRANSCRIPTION_ETAT_CIVIL';
    const isDon = d === 'DON_GAMETES_ACCES_ORIGINES';

    const paysCode = this.paysGPACode();
    const paysOption = paysCode
      ? this.paysOptions.find((p) => p.code === paysCode) ?? null
      : null;
    const paysLegal = paysOption ? paysOption.legal : null;

    const request: PmaGpaBioethiqueRequest = {
      dispositif: d,
      // PMA
      consentementsConjointsNotaire: isPma ? this.consentementsConjointsNotaire() : null,
      dateReconnaissanceAnterieurePMA: isPma ? this.dateReconnaissanceAnterieurePMA() : null,
      datePMA: isPma ? this.datePMA() : null,
      conditionsAccesPMA: isPma ? this.conditionsAccesPMA() : null,
      // GPA
      paysGPALegal: isGpa ? paysLegal : null,
      parentBiologiqueAvere: isGpa ? this.parentBiologiqueAvere() : null,
      decisionEtrangereProduite: isGpa ? this.decisionEtrangereProduite() : null,
      adoptionDemande: isGpa ? this.adoptionDemande() : null,
      // DON
      dateDon: isDon ? this.dateDon() : null,
      demandeAcces: isDon ? this.demandeAcces() : null,
      ageDemandeur: isDon ? this.ageDemandeur() : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('PMA / GPA / bioéthique analysée', 'OK', { duration: 2500 });
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
        this.provenanceDispositif.set(null);
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

  /**
   * Classe CSS du bandeau verdict.
   * - ELEVEE : info (navy).
   * - MOYENNE : warning (or).
   * - FAIBLE : critical (rouge — recevabilité quasi-nulle).
   */
  bannerClass(verdict: VerdictRecevabiliteBioethique | string | undefined | null): string {
    if (verdict === 'FAIBLE') return 'pma-gpa-banner pma-gpa-banner--critical';
    if (verdict === 'MOYENNE') return 'pma-gpa-banner pma-gpa-banner--warning';
    return 'pma-gpa-banner pma-gpa-banner--info';
  }

  /** Libellé humain d'un dispositif. */
  dispositifLabel(code: DispositifBioethique | null | undefined): string {
    if (!code) return '';
    const opt = DISPOSITIF_BIOETHIQUE_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }

  /** Libellé humain d'un pays GPA. */
  paysLabel(code: string | null | undefined): string {
    if (!code) return '';
    const opt = PAYS_GPA_OPTIONS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}

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
import { PossessionEtatService } from '../../core/services/possession-etat.service';
import {
  DISPOSITIF_APPLICABLE_LABELS,
  DispositifApplicable,
  PossessionEtatRequest,
  PossessionEtatResponse,
  VerdictRecevabilitePossessionEtat,
} from '../../core/models/possession-etat.model';
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
 * SF-FA-18-08 : champs d'alerte F-IA-03 exposés par l'outil
 * "Possession d'état" (art. 311-1 + 311-2 + 317 Cciv).
 */
export type PossessionEtatAlertField =
  | 'TRACTATUS'
  | 'FAMA'
  | 'NOMEN'
  | 'CONTINUE'
  | 'PAISIBLE'
  | 'NON_EQUIVOQUE';

export type PossessionEtatAlertSource = CoherenceAlertSource;
export type PossessionEtatCoherenceAlert =
  CoherenceAlert<PossessionEtatAlertField>;

/**
 * SF-FA-18-08 : outil décisionnel "Possession d'état"
 * (FR — art. 311-1 + 311-2 + 317 Cciv).
 *
 * Mode de preuve de la filiation par les faits : faisceau d'indices
 * (tractatus + fama + nomen) soumis aux conditions cardinales (art. 311-2 :
 * continue, paisible, non équivoque). Selon les critères + la durée, oriente
 * vers le constat par notaire (acte de notoriété art. 317) ou la preuve
 * judiciaire (art. 311-1 + 311-2).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC art. 331-1
 * au backlog jumeau).
 *
 * Consomme l'API SF-FA-18-07 (mergée PR #670). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-18-possession-etat' — migration 185).
 *
 * Pattern de référence : `recherche-paternite-section` (PR #669 —
 * chantier F-FA-18 jumeau).
 */
@Component({
  selector: 'app-possession-etat-section',
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
  templateUrl: './possession-etat-section.component.html',
  styleUrl: './possession-etat-section.component.scss',
})
export class PossessionEtatSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'POSSESSION D\'ÉTAT (FR)';
  static readonly TOOL_ICON = 'family_restroom';

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
  result = signal<PossessionEtatResponse | null>(null);

  // Form fields
  /** ISO YYYY-MM-DD via input type="date". */
  dateDebutPossession = signal<string | null>(null);
  /** ISO YYYY-MM-DD via input type="date". */
  dateFinPossession = signal<string | null>(null);
  tractatus = signal<boolean>(false);
  fama = signal<boolean>(false);
  nomen = signal<boolean>(false);
  continueCondition = signal<boolean>(false);
  paisible = signal<boolean>(false);
  nonEquivoque = signal<boolean>(false);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceTractatus = signal<'IA' | null>(null);
  provenanceFama = signal<'IA' | null>(null);
  provenanceNomen = signal<'IA' | null>(null);
  provenanceContinue = signal<'IA' | null>(null);
  provenancePaisible = signal<'IA' | null>(null);
  provenanceNonEquivoque = signal<'IA' | null>(null);

  /** Listes pour mat-radio dispositif (preview / résultat). */
  readonly dispositifOptions = DISPOSITIF_APPLICABLE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Durée calculée live côté UI à partir des dates (cohérent avec le calcul
   * backend `ChronoUnit.MONTHS / 12`). Renvoie `null` si dates invalides.
   */
  dureePossessionAnnees = computed<number | null>(() => {
    const debut = this.dateDebutPossession();
    const fin = this.dateFinPossession();
    if (!debut || !fin) return null;
    const dDebut = new Date(debut);
    const dFin = new Date(fin);
    if (isNaN(dDebut.getTime()) || isNaN(dFin.getTime())) return null;
    if (dFin < dDebut) return null;
    const months = (dFin.getFullYear() - dDebut.getFullYear()) * 12
      + (dFin.getMonth() - dDebut.getMonth())
      + (dFin.getDate() >= dDebut.getDate() ? 0 : -1);
    return Math.floor(months / 12);
  });

  /**
   * Alertes de cohérence F-IA-03 par field. Gate : uniquement en mode
   * formulaire. Source IA principale : `possessionEtatConforme5AnsDetected`
   * (faisceau "conforme" → tous les critères cardinaux + tractatus/fama).
   */
  coherenceAlerts = computed<Partial<Record<PossessionEtatAlertField,
      PossessionEtatCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PossessionEtatAlertField,
        PossessionEtatCoherenceAlert>> = {};
    const tract = this.buildBooleanAlert(
      'TRACTATUS', this.tractatus(),
      'POSSESSION_ETAT_TRACTATUS',
      this.iaConformeBoolean(),
      'Tractatus établi',
      'Tractatus non établi',
      'tractatus',
    );
    if (tract) alerts.TRACTATUS = tract;
    const fama = this.buildBooleanAlert(
      'FAMA', this.fama(),
      'POSSESSION_ETAT_FAMA',
      this.iaConformeBoolean(),
      'Fama établie',
      'Fama non établie',
      'fama',
    );
    if (fama) alerts.FAMA = fama;
    const cont = this.buildBooleanAlert(
      'CONTINUE', this.continueCondition(),
      'POSSESSION_ETAT_CONTINUE',
      this.iaConformeBoolean(),
      'Possession continue',
      'Possession non continue',
      'condition continue',
    );
    if (cont) alerts.CONTINUE = cont;
    const pais = this.buildBooleanAlert(
      'PAISIBLE', this.paisible(),
      'POSSESSION_ETAT_PAISIBLE',
      this.iaConformeBoolean(),
      'Possession paisible',
      'Possession non paisible',
      'condition paisible',
    );
    if (pais) alerts.PAISIBLE = pais;
    const nonEq = this.buildBooleanAlert(
      'NON_EQUIVOQUE', this.nonEquivoque(),
      'POSSESSION_ETAT_NON_EQUIVOQUE',
      this.iaConformeBoolean(),
      'Possession non équivoque',
      'Possession équivoque',
      'condition non équivoque',
    );
    if (nonEq) alerts.NON_EQUIVOQUE = nonEq;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  /**
   * Chip alerte délai de contestation selon dispositif :
   * - rouge / critical si dispositif AUCUN.
   * - or / warning si dispositif PREUVE_JUSTICE (10 ans cessation).
   * - null sinon (CONSTAT_NOTAIRE — délai 5 ans depuis acte = stable).
   */
  delaiContestationAlert = computed<'critical' | 'warning' | null>(() => {
    const r = this.result();
    if (!r) return null;
    if (r.dispositifApplicable === 'AUCUN') return 'critical';
    if (r.dispositifApplicable === 'PREUVE_JUSTICE') return 'warning';
    return null;
  });

  constructor(
    private service: PossessionEtatService,
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

  /**
   * Renvoie la valeur IA "faisceau conforme" (bool) si présente dans aiData
   * — utilisée uniformément pour les 5 critères cardinaux (tractatus, fama,
   * continue, paisible, nonEquivoque). Le pipeline IA actuel ne distingue
   * pas les booléens granulaires : le fallback gracieux est une détection
   * agrégée du faisceau cardinal "conforme 5 ans".
   */
  private iaConformeBoolean(): boolean | null | undefined {
    return this.aiDataSignal()?.possessionEtatConforme5AnsDetected;
  }

  alertTooltip(alert: PossessionEtatCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PossessionEtatCoherenceAlert): string {
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
   * Helper interne factorisé pour construire une alerte multi-sources sur
   * un champ booléen. Pattern emprunté à `recherche-paternite-section`.
   */
  private buildBooleanAlert(
    field: PossessionEtatAlertField,
    userVal: boolean,
    critereCode: string,
    iaVal: boolean | null | undefined,
    trueLabel: string,
    falseLabel: string,
    fieldLabel: string,
  ): PossessionEtatCoherenceAlert | null {
    const builder = CoherenceAlertBuilder
        .forField<PossessionEtatAlertField>(field);

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

    // 3. IA — `aiData.possessionEtatConforme5AnsDetected` (fallback agrégé).
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? trueLabel : falseLabel,
        reason: `Analyse du dossier : faisceau cardinal "conforme 5 ans" ${iaVal ? 'détecté' : 'non détecté'}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([critereCode]);
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
    const debut = this.dateDebutPossession();
    const fin = this.dateFinPossession();
    if (!debut || !fin) return false;
    const dDebut = new Date(debut);
    const dFin = new Date(fin);
    if (isNaN(dDebut.getTime()) || isNaN(dFin.getTime())) return false;
    if (dFin < dDebut) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onDateDebutChange(value: string | null): void {
    this.dateDebutPossession.set(value || null);
  }

  onDateFinChange(value: string | null): void {
    this.dateFinPossession.set(value || null);
  }

  onTractatusChange(value: boolean): void {
    this.tractatus.set(value);
    this.provenanceTractatus.set(null);
  }

  onFamaChange(value: boolean): void {
    this.fama.set(value);
    this.provenanceFama.set(null);
  }

  onNomenChange(value: boolean): void {
    this.nomen.set(value);
    this.provenanceNomen.set(null);
  }

  onContinueChange(value: boolean): void {
    this.continueCondition.set(value);
    this.provenanceContinue.set(null);
  }

  onPaisibleChange(value: boolean): void {
    this.paisible.set(value);
    this.provenancePaisible.set(null);
  }

  onNonEquivoqueChange(value: boolean): void {
    this.nonEquivoque.set(value);
    this.provenanceNonEquivoque.set(null);
  }

  /**
   * SF-FA-18-08 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Source IA : `possessionEtatConforme5AnsDetected` (fallback agrégé —
   * pré-coche tractatus + fama + 3 conditions cardinales en faisceau).
   *
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide / `false` (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    const conforme = ai.possessionEtatConforme5AnsDetected;
    if (conforme !== true) return;

    // Pré-coche tractatus + fama + 3 conditions cardinales (faisceau).
    if (!this.tractatus() || this.provenanceTractatus() === 'IA') {
      this.tractatus.set(true);
      this.provenanceTractatus.set('IA');
    }
    if (!this.fama() || this.provenanceFama() === 'IA') {
      this.fama.set(true);
      this.provenanceFama.set('IA');
    }
    if (!this.continueCondition() || this.provenanceContinue() === 'IA') {
      this.continueCondition.set(true);
      this.provenanceContinue.set('IA');
    }
    if (!this.paisible() || this.provenancePaisible() === 'IA') {
      this.paisible.set(true);
      this.provenancePaisible.set('IA');
    }
    if (!this.nonEquivoque() || this.provenanceNonEquivoque() === 'IA') {
      this.nonEquivoque.set(true);
      this.provenanceNonEquivoque.set('IA');
    }
    // Nomen non pré-rempli — facultatif depuis 2005, l'avocat le coche
    // explicitement s'il est porté.
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PossessionEtatRequest = {
      dateDebutPossession: this.dateDebutPossession()!,
      dateFinPossession: this.dateFinPossession()!,
      tractatus: this.tractatus(),
      fama: this.fama(),
      nomen: this.nomen(),
      continueCondition: this.continueCondition(),
      paisible: this.paisible(),
      nonEquivoque: this.nonEquivoque(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Possession d\'état analysée', 'OK', { duration: 2500 });
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
        this.provenanceTractatus.set(null);
        this.provenanceFama.set(null);
        this.provenanceNomen.set(null);
        this.provenanceContinue.set(null);
        this.provenancePaisible.set(null);
        this.provenanceNonEquivoque.set(null);
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
   * - MOYENNE → or/warning (faisceau d'indices partiel).
   * - FAIBLE → rouge/critical (possession non caractérisée).
   */
  bannerClass(verdict: VerdictRecevabilitePossessionEtat | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'pos-etat-banner pos-etat-banner--info';
      case 'MOYENNE': return 'pos-etat-banner pos-etat-banner--warning';
      case 'FAIBLE': return 'pos-etat-banner pos-etat-banner--critical';
      default: return 'pos-etat-banner pos-etat-banner--info';
    }
  }

  /** Libellé humain d'un dispositif applicable. */
  dispositifLabel(code: DispositifApplicable | null | undefined): string {
    if (!code) return '';
    const opt = DISPOSITIF_APPLICABLE_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }

  dispositifSub(code: DispositifApplicable | null | undefined): string {
    if (!code) return '';
    const opt = DISPOSITIF_APPLICABLE_LABELS.find((o) => o.code === code);
    return opt?.sub ?? '';
  }
}

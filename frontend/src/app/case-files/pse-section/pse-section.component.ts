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
import { PseService } from '../../core/services/pse.service';
import {
  AVIS_CSE_LABELS,
  AvisCse,
  CONTENU_MESURE_LABELS,
  ContenuMesure,
  CRITERE_VALIDITE_LABELS,
  CritereValidite,
  MODE_ADOPTION_LABELS,
  ModeAdoption,
  PseRequest,
  PseResponse,
  STATUT_DREETS_LABELS,
  StatutDreets,
  VerdictValidite,
} from '../../core/models/pse.model';
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
import {
  PseSectionPrefillRules,
  computeDateProjet as computeDateProjetRule,
} from './pse-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-DT-14-02 : champs d'alerte F-IA-03 exposés par l'outil PSE (F-DT-14).
 * - DATE_PROJET : divergence si IA a `dateLicenciement` et l'avocat saisit
 *   une date éloignée de plus de 30 jours.
 */
export type PseAlertField = 'DATE_PROJET';

export type PseAlertSource = CoherenceAlertSource;
export type PseCoherenceAlert = CoherenceAlert<PseAlertField>;

/** Seuil d'écart absolu (jours) pour la date de projet avant alerte F-IA-03. */
const DATE_PROJET_DIVERGENCE_JOURS = 30;

/** Seuils L.1233-24-1 (déclenchement PSE). */
const SEUIL_LICENCIEMENTS_PSE = 10;
const SEUIL_ENTREPRISE_PSE = 50;
const PERIODE_REFERENCE_JOURS = 30;

/**
 * SF-DT-14-02 : outil décisionnel "PSE — critères de validité" (FR).
 * 4 axes : déclenchement L.1233-24-1, consultation CSE L.1233-30, statut
 * DREETS L.1233-57-2, contenu minimal L.1233-61. Délai de contestation
 * 60 jours (L.1235-7-1).
 *
 * FR uniquement (bannière info si dossier BE — pas masquage silencieux).
 *
 * Consomme l'API SF-DT-14-01 (mergée PR #623). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-DT-14-pse-validite' — migration 164).
 *
 * Patterns de référence :
 * - Pattern jumeau BE-only : `credit-temps-be-section` (F-DT-29 PR #624).
 * - Pattern jumeau FR-only : `refere-prudhomal-section` (F-DT-34).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-pse-section',
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
  templateUrl: './pse-section.component.html',
  styleUrl: './pse-section.component.scss',
})
export class PseSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PSE — CRITÈRES DE VALIDITÉ (FR)';
  static readonly TOOL_ICON = 'groups';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return PseSectionPrefillRules.computePrefillCount({ aiData: input.aiData });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
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
  result = signal<PseResponse | null>(null);

  // Form fields — section Déclenchement
  tailleEntrepriseSalaries = signal<number | null>(null);
  nombreLicenciementsEnvisages = signal<number | null>(null);
  periodeJours = signal<number | null>(PERIODE_REFERENCE_JOURS);
  dateProjet = signal<string | null>(null);

  // Form fields — section Mode + DREETS
  modeAdoption = signal<ModeAdoption | null>(null);
  dreetsStatut = signal<StatutDreets | null>(null);
  dateNotificationDreets = signal<string | null>(null);

  // Form fields — section CSE
  csaeConsulteAvis = signal<AvisCse | null>(null);

  // Form fields — section Contenu
  contenuMesures = signal<ContenuMesure[]>([]);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceDateProjet = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Listes pour mat-radio / mat-checkbox. */
  readonly modeAdoptionOptions = MODE_ADOPTION_LABELS;
  readonly avisCseOptions = AVIS_CSE_LABELS;
  readonly statutDreetsOptions = STATUT_DREETS_LABELS;
  readonly contenuMesureOptions = CONTENU_MESURE_LABELS;
  readonly critereLabels = CRITERE_VALIDITE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Indication live "PSE requis" calculée depuis le formulaire — preview UX
   * pour aider l'avocat à vérifier le déclenchement L.1233-24-1 avant le
   * POST. Le verdict définitif vient du backend.
   */
  pseRequisPreview = computed<boolean>(() => {
    const taille = this.tailleEntrepriseSalaries();
    const nb = this.nombreLicenciementsEnvisages();
    const periode = this.periodeJours();
    if (taille === null || nb === null || periode === null) return false;
    return nb >= SEUIL_LICENCIEMENTS_PSE
      && periode <= PERIODE_REFERENCE_JOURS
      && taille >= SEUIL_ENTREPRISE_PSE;
  });

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<PseAlertField, PseCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PseAlertField, PseCoherenceAlert>> = {};
    const a = this.buildDateProjetAlert();
    if (a) alerts.DATE_PROJET = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: PseService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
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

    // Ré-applique le prefill si aiData change, en mode formulaire ET sans
    // résultat persisté chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
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

  /** SF-IA-03-15c : map field → sourceKey. */
  explanationFor(field: PseAlertField): SourceExplanation[] {
    const key = field === 'DATE_PROJET' ? 'PSE_DATE_PROJET' : field;
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: PseCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PseCoherenceAlert): string {
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
   * Calcule l'écart en jours entre deux dates ISO (YYYY-MM-DD). Retourne
   * null si l'une des deux est invalide.
   */
  private diffJours(a: string, b: string): number | null {
    const da = new Date(a);
    const db = new Date(b);
    if (isNaN(da.getTime()) || isNaN(db.getTime())) return null;
    const ms = Math.abs(da.getTime() - db.getTime());
    return Math.round(ms / (1000 * 60 * 60 * 24));
  }

  /**
   * Divergence date de projet — écart absolu > 30 jours entre IA
   * (`dateLicenciement`) et saisie avocat. Multi-sources F96 / QUESTION_IA /
   * IA / PIECE_MANQUANTE.
   */
  private buildDateProjetAlert(): PseCoherenceAlert | null {
    const userDate = this.dateProjet();
    if (!userDate) return null;

    const builder = CoherenceAlertBuilder.forField<PseAlertField>('DATE_PROJET');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PSE_DATE_PROJET') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const diff = this.diffJours(userDate, ev);
      if (diff === null || diff <= DATE_PROJET_DIVERGENCE_JOURS) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date de projet attendue ${ev}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PSE_DATE_PROJET') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const diff = this.diffJours(userDate, ev);
      if (diff === null || diff <= DATE_PROJET_DIVERGENCE_JOURS) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.dateLicenciement`.
    const iaDate = this.aiDataSignal()?.dateLicenciement;
    if (iaDate) {
      const diff = this.diffJours(userDate, iaDate);
      if (diff !== null && diff > DATE_PROJET_DIVERGENCE_JOURS) {
        builder.addSource('IA', {
          expectedDisplay: iaDate,
          reason: `Analyse du dossier : date de licenciement ${iaDate}`,
        });
      }
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['PSE_DATE_PROJET', 'DATE_PROJET']);
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
    const taille = this.tailleEntrepriseSalaries();
    if (taille === null || taille === undefined || taille < 0) return false;
    const nb = this.nombreLicenciementsEnvisages();
    if (nb === null || nb === undefined || nb < 0) return false;
    const periode = this.periodeJours();
    if (periode === null || periode === undefined || periode < 0) return false;
    if (!this.modeAdoption()) return false;
    if (!this.csaeConsulteAvis()) return false;
    if (!this.dreetsStatut()) return false;
    if (!this.dateProjet()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onTailleEntrepriseChange(value: number | null): void {
    this.tailleEntrepriseSalaries.set(value === null || value === undefined ? null : value);
  }

  onNombreLicenciementsChange(value: number | null): void {
    this.nombreLicenciementsEnvisages.set(value === null || value === undefined ? null : value);
  }

  onPeriodeJoursChange(value: number | null): void {
    this.periodeJours.set(value === null || value === undefined ? null : value);
  }

  onDateProjetChange(value: string | null): void {
    this.dateProjet.set(value || null);
    this.provenanceDateProjet.set(null);
  }

  onModeAdoptionChange(value: ModeAdoption | null): void {
    this.modeAdoption.set(value);
  }

  onDreetsStatutChange(value: StatutDreets | null): void {
    this.dreetsStatut.set(value);
  }

  onDateNotificationDreetsChange(value: string | null): void {
    this.dateNotificationDreets.set(value || null);
  }

  onCsaeAvisChange(value: AvisCse | null): void {
    this.csaeConsulteAvis.set(value);
  }

  isMesureChecked(code: ContenuMesure): boolean {
    return this.contenuMesures().includes(code);
  }

  onMesureToggle(code: ContenuMesure, checked: boolean): void {
    const current = this.contenuMesures();
    if (checked && !current.includes(code)) {
      this.contenuMesures.set([...current, code]);
    } else if (!checked && current.includes(code)) {
      this.contenuMesures.set(current.filter((c) => c !== code));
    }
  }

  /**
   * SF-DT-14-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : valeur calculée par le helper partagé (parité static).
    const iaDate = computeDateProjetRule({ aiData: ai });
    if (iaDate) {
      if (this.dateProjet() === null
          || this.provenanceDateProjet() === 'IA') {
        this.dateProjet.set(iaDate);
        this.provenanceDateProjet.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PseRequest = {
      tailleEntrepriseSalaries: this.tailleEntrepriseSalaries()!,
      nombreLicenciementsEnvisages: this.nombreLicenciementsEnvisages()!,
      periodeJours: this.periodeJours()!,
      modeAdoption: this.modeAdoption()!,
      csaeConsulteAvis: this.csaeConsulteAvis()!,
      dreetsStatut: this.dreetsStatut()!,
      contenuMesures: [...this.contenuMesures()],
      dateProjet: this.dateProjet()!,
    };
    if (this.dateNotificationDreets()) {
      request.dateNotificationDreets = this.dateNotificationDreets()!;
    }
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('PSE analysé', 'OK', { duration: 2500 });
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
        // Hydrate l'aperçu (champs texte) avec les valeurs persistées —
        // l'avocat peut cliquer "Modifier" pour ré-éditer.
        this.tailleEntrepriseSalaries.set(null);
        this.nombreLicenciementsEnvisages.set(null);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceDateProjet.set(null);
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
   * - NUL → rouge/critical (palette urgence — bloquant procédural).
   */
  bannerClass(verdict: VerdictValidite | string | undefined | null): string {
    switch (verdict) {
      case 'VALIDE': return 'pse-banner pse-banner--info';
      case 'CONTESTABLE': return 'pse-banner pse-banner--warning';
      case 'NUL': return 'pse-banner pse-banner--critical';
      default: return 'pse-banner pse-banner--info';
    }
  }

  /** Libellé humain d'un critère de validité. */
  critereLabel(code: CritereValidite | string | null | undefined): string {
    if (!code) return '';
    const label = (this.critereLabels as Record<string, string>)[code as string];
    return label ?? code;
  }
}

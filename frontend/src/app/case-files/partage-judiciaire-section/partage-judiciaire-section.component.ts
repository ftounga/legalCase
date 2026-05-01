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
import { PartageJudiciaireService } from '../../core/services/partage-judiciaire.service';
import {
  PartageJudiciaireRequest,
  PartageJudiciaireResponse,
  TYPE_BIEN_INDIVISION_LABELS,
  TypeBienIndivision,
  VerdictRecevabilite,
} from '../../core/models/partage-judiciaire.model';
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
 * SF-FA-17-02 : champs d'alerte F-IA-03 exposés par l'outil "Partage judiciaire".
 * - PV_DIFFICULTES : divergence PV (art. 1366 CPC) — IA dit oui, avocat dit non
 *   (le préalable obligatoire est noté absent côté avocat alors que l'IA détecte
 *   sa présence dans le dossier).
 * - TENTATIVE_AMIABLE : divergence sur la tentative amiable épuisée.
 */
export type PartageJudiciaireAlertField = 'PV_DIFFICULTES' | 'TENTATIVE_AMIABLE';

export type PartageJudiciaireAlertSource = CoherenceAlertSource;
export type PartageJudiciaireCoherenceAlert =
  CoherenceAlert<PartageJudiciaireAlertField>;

/**
 * SF-FA-17-02 : outil décisionnel "Partage judiciaire" (FR — art. 840 et s.
 * Code civil + 1364+ et 1366 Code de procédure civile).
 *
 * 5 critères : PV difficultés établi, tentative amiable épuisée, type bien,
 * nombre co-indivisaires ≥ 2, désaccord motivé.
 *
 * FR uniquement (bannière info si dossier BE — équivalent CJ art. 1207+ au
 * backlog jumeau).
 *
 * Consomme l'API SF-FA-17-01 (mergée PR #636). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-17-partage-judiciaire' — migration 169).
 *
 * Pattern de référence : `protection-rp-section` (PR #634).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-partage-judiciaire-section',
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
  templateUrl: './partage-judiciaire-section.component.html',
  styleUrl: './partage-judiciaire-section.component.scss',
})
export class PartageJudiciaireSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PARTAGE JUDICIAIRE (FR)';
  static readonly TOOL_ICON = 'balance';

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
  result = signal<PartageJudiciaireResponse | null>(null);

  // Form fields
  pvDifficultesEtabli = signal<boolean | null>(null);
  tentativeAmiableEpuiseuee = signal<boolean | null>(null);
  typeBienIndivision = signal<TypeBienIndivision | null>(null);
  nombreCoindivisaires = signal<number | null>(null);
  desaccordMotive = signal<boolean | null>(null);
  valeurEstimeeBiensEur = signal<number | null>(null);

  /** Provenance IA pour les champs pré-remplissables. */
  provenancePvDifficultes = signal<'IA' | null>(null);
  provenanceTentativeAmiable = signal<'IA' | null>(null);
  provenanceNombreCoindivisaires = signal<'IA' | null>(null);
  provenanceValeurBiens = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly typeBienOptions = TYPE_BIEN_INDIVISION_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<PartageJudiciaireAlertField, PartageJudiciaireCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PartageJudiciaireAlertField, PartageJudiciaireCoherenceAlert>> = {};
    const pv = this.buildPvDifficultesAlert();
    if (pv) alerts.PV_DIFFICULTES = pv;
    const ta = this.buildTentativeAmiableAlert();
    if (ta) alerts.TENTATIVE_AMIABLE = ta;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: PartageJudiciaireService,
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

  alertTooltip(alert: PartageJudiciaireCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PartageJudiciaireCoherenceAlert): string {
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
   * Divergence PV de difficultés (art. 1366 CPC). Multi-sources F96 / QUESTION_IA
   * / IA / PIECE_MANQUANTE. L'alerte est levée quand l'avocat saisit `false`
   * alors qu'au moins une source IA atteste l'inverse.
   */
  private buildPvDifficultesAlert(): PartageJudiciaireCoherenceAlert | null {
    const userVal = this.pvDifficultesEtabli();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<PartageJudiciaireAlertField>('PV_DIFFICULTES');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PARTAGE_JUDICIAIRE_PV') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'PV établi' : 'PV non établi',
        reason: `Checklist procédurale : PV de difficultés ${expected ? 'établi' : 'non établi'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PARTAGE_JUDICIAIRE_PV') continue;
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
        expectedDisplay: expected ? 'PV établi' : 'PV non établi',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.pvDifficultesEtablisDetected`.
    const iaPv = this.aiDataSignal()?.pvDifficultesEtablisDetected;
    if (iaPv !== null && iaPv !== undefined && iaPv !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaPv ? 'PV établi' : 'PV non établi',
        reason: `Analyse du dossier : PV de difficultés ${iaPv ? 'établi' : 'non établi'}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['PARTAGE_JUDICIAIRE_PV', 'PV_DIFFICULTES']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Divergence Tentative amiable épuisée. Multi-sources F96 / QUESTION_IA / IA.
   */
  private buildTentativeAmiableAlert(): PartageJudiciaireCoherenceAlert | null {
    const userVal = this.tentativeAmiableEpuiseuee();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<PartageJudiciaireAlertField>('TENTATIVE_AMIABLE');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Tentative amiable épuisée' : 'Tentative non épuisée',
        reason: `Checklist procédurale : tentative amiable ${expected ? 'épuisée' : 'non épuisée'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE') continue;
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
        expectedDisplay: expected ? 'Tentative amiable épuisée' : 'Tentative non épuisée',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.tentativeAmiableEpuiseueeDetected`.
    const iaTa = this.aiDataSignal()?.tentativeAmiableEpuiseueeDetected;
    if (iaTa !== null && iaTa !== undefined && iaTa !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaTa ? 'Tentative amiable épuisée' : 'Tentative non épuisée',
        reason: `Analyse du dossier : tentative amiable ${iaTa ? 'épuisée' : 'non épuisée'}`,
      });
    }

    // 4. PIECE_MANQUANTE.
    const piece = this.findPieceManquante([
      'PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE',
      'TENTATIVE_AMIABLE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Mappe une chaîne IA / F96 brute vers booléen. Tolère "oui", "true",
   * "vrai", "1", "OUI", etc. Retourne null si non reconnu.
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
    if (this.pvDifficultesEtabli() === null) return false;
    if (this.tentativeAmiableEpuiseuee() === null) return false;
    if (this.desaccordMotive() === null) return false;
    if (!this.typeBienIndivision()) return false;
    const nb = this.nombreCoindivisaires();
    if (nb === null || nb === undefined || nb < 2) return false;
    const valeur = this.valeurEstimeeBiensEur();
    if (valeur === null || valeur === undefined || valeur < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onPvDifficultesChange(value: boolean | null): void {
    this.pvDifficultesEtabli.set(value);
    this.provenancePvDifficultes.set(null);
  }

  onTentativeAmiableChange(value: boolean | null): void {
    this.tentativeAmiableEpuiseuee.set(value);
    this.provenanceTentativeAmiable.set(null);
  }

  onTypeBienChange(value: TypeBienIndivision | null): void {
    this.typeBienIndivision.set(value);
  }

  onNombreCoindivisairesChange(value: number | null): void {
    this.nombreCoindivisaires.set(value === null || value === undefined ? null : value);
    this.provenanceNombreCoindivisaires.set(null);
  }

  onDesaccordMotiveChange(value: boolean | null): void {
    this.desaccordMotive.set(value);
  }

  onValeurBiensChange(value: number | null): void {
    this.valeurEstimeeBiensEur.set(value === null || value === undefined ? null : value);
    this.provenanceValeurBiens.set(null);
  }

  /**
   * SF-FA-17-02 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide / `null` (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // pvDifficultesEtabli ← aiData.pvDifficultesEtablisDetected.
    const iaPv = ai.pvDifficultesEtablisDetected;
    if (iaPv !== null && iaPv !== undefined) {
      if (this.pvDifficultesEtabli() === null
          || this.provenancePvDifficultes() === 'IA') {
        this.pvDifficultesEtabli.set(iaPv);
        this.provenancePvDifficultes.set('IA');
      }
    }

    // tentativeAmiableEpuiseuee ← aiData.tentativeAmiableEpuiseueeDetected.
    const iaTa = ai.tentativeAmiableEpuiseueeDetected;
    if (iaTa !== null && iaTa !== undefined) {
      if (this.tentativeAmiableEpuiseuee() === null
          || this.provenanceTentativeAmiable() === 'IA') {
        this.tentativeAmiableEpuiseuee.set(iaTa);
        this.provenanceTentativeAmiable.set('IA');
      }
    }

    // nombreCoindivisaires ← aiData.nombreCoindivisairesDetecte.
    const iaNb = ai.nombreCoindivisairesDetecte;
    if (iaNb !== null && iaNb !== undefined && iaNb >= 2) {
      if (this.nombreCoindivisaires() === null
          || this.provenanceNombreCoindivisaires() === 'IA') {
        this.nombreCoindivisaires.set(iaNb);
        this.provenanceNombreCoindivisaires.set('IA');
      }
    }

    // valeurEstimeeBiensEur ← aiData.valeurBiensIndivisionEur.
    const iaValeur = ai.valeurBiensIndivisionEur;
    if (iaValeur !== null && iaValeur !== undefined && iaValeur >= 0) {
      if (this.valeurEstimeeBiensEur() === null
          || this.provenanceValeurBiens() === 'IA') {
        this.valeurEstimeeBiensEur.set(iaValeur);
        this.provenanceValeurBiens.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PartageJudiciaireRequest = {
      pvDifficultesEtabli: this.pvDifficultesEtabli()!,
      tentativeAmiableEpuiseuee: this.tentativeAmiableEpuiseuee()!,
      typeBienIndivision: this.typeBienIndivision()!,
      nombreCoindivisaires: this.nombreCoindivisaires()!,
      desaccordMotive: this.desaccordMotive()!,
      valeurEstimeeBiensEur: this.valeurEstimeeBiensEur()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Partage judiciaire analysé', 'OK', { duration: 2500 });
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
        this.provenancePvDifficultes.set(null);
        this.provenanceTentativeAmiable.set(null);
        this.provenanceNombreCoindivisaires.set(null);
        this.provenanceValeurBiens.set(null);
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
   * - MOYENNE → or/warning (procédure plus longue, licitation potentielle).
   * - FAIBLE → rouge/critical (préalable non rempli, refus probable).
   */
  bannerClass(verdict: VerdictRecevabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'partage-jud-banner partage-jud-banner--info';
      case 'MOYENNE': return 'partage-jud-banner partage-jud-banner--warning';
      case 'FAIBLE': return 'partage-jud-banner partage-jud-banner--critical';
      default: return 'partage-jud-banner partage-jud-banner--info';
    }
  }

  /** Libellé humain d'un type de bien. */
  typeBienLabel(code: TypeBienIndivision | null | undefined): string {
    if (!code) return '';
    const opt = TYPE_BIEN_INDIVISION_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}

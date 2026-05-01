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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdoptionService } from '../../core/services/adoption.service';
import {
  AdoptionRequest,
  AdoptionResponse,
  FORME_ADOPTION_LABELS,
  FormeAdoption,
  VerdictRecevabiliteAdoption,
} from '../../core/models/adoption.model';
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
 * SF-FA-18-10 : champs d'alerte F-IA-03 exposés par l'outil "Adoption"
 * (art. 343-370-2 Cciv).
 */
export type AdoptionAlertField =
  | 'FORME_ADOPTION'
  | 'PUPILLE_ETAT'
  | 'ADOPTANT_MARIE'
  | 'AGE_ADOPTANT'
  | 'AGE_ADOPTE';

export type AdoptionAlertSource = CoherenceAlertSource;
export type AdoptionCoherenceAlert = CoherenceAlert<AdoptionAlertField>;

/**
 * SF-FA-18-10 : outil décisionnel "Adoption"
 * (FR — art. 343-370-2 Cciv).
 *
 * Évalue la recevabilité d'une adoption (plénière ou simple) au regard des
 * conditions cardinales du Code civil français. Peut basculer
 * plénière → simple si conditions plénière non remplies mais simple OK.
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC belge art. 343
 * et s. au backlog jumeau).
 *
 * Consomme l'API SF-FA-18-09 (mergée PR #677). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-18-adoption' — migration 187).
 *
 * Pattern de référence : `possession-etat-section` (PR #676 — chantier
 * F-FA-18 jumeau).
 */
@Component({
  selector: 'app-adoption-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './adoption-section.component.html',
  styleUrl: './adoption-section.component.scss',
})
export class AdoptionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ADOPTION (FR)';
  static readonly TOOL_ICON = 'family_restroom';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AdoptionResponse | null>(null);

  // Form fields
  formeAdoption = signal<'PLENIERE' | 'SIMPLE'>('PLENIERE');
  ageAdoptant = signal<number | null>(null);
  ageAdopte = signal<number | null>(null);
  consentementParents = signal<boolean>(false);
  consentementAdopte = signal<boolean>(false);
  consentementConjointAdoptant = signal<boolean>(false);
  enquetes = signal<boolean>(false);
  placement6mois = signal<boolean>(false);
  pupilleEtat = signal<boolean>(false);
  adoptantMarie = signal<boolean>(false);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceFormeAdoption = signal<'IA' | null>(null);
  provenancePupilleEtat = signal<'IA' | null>(null);
  provenanceAdoptantMarie = signal<'IA' | null>(null);
  provenanceAgeAdoptant = signal<'IA' | null>(null);
  provenanceAgeAdopte = signal<'IA' | null>(null);

  /** Listes pour radio. */
  readonly formeOptions = FORME_ADOPTION_LABELS.filter(
    (o) => o.code !== 'AUCUNE');

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Différence d'âge calculée live (cohérent avec le calcul backend). */
  differenceAgeAns = computed<number | null>(() => {
    const ad = this.ageAdoptant();
    const en = this.ageAdopte();
    if (ad === null || en === null) return null;
    return ad - en;
  });

  /**
   * Alertes de cohérence F-IA-03 par field. Gate : uniquement en mode
   * formulaire.
   */
  coherenceAlerts = computed<Partial<Record<AdoptionAlertField,
      AdoptionCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AdoptionAlertField,
        AdoptionCoherenceAlert>> = {};

    const ai = this.aiDataSignal();

    // FORME_ADOPTION (string PLENIERE/SIMPLE)
    const forme = this.buildEnumAlert(
      'FORME_ADOPTION',
      this.formeAdoption(),
      'ADOPTION_FORME',
      ai?.formeAdoptionDemandeeDetected ?? null,
      'forme d\'adoption',
    );
    if (forme) alerts.FORME_ADOPTION = forme;

    // PUPILLE_ETAT (boolean)
    const pup = this.buildBooleanAlert(
      'PUPILLE_ETAT', this.pupilleEtat(),
      'ADOPTION_PUPILLE_ETAT',
      ai?.pupilleEtatDetected ?? null,
      'Adopté pupille de l\'État',
      'Adopté non pupille',
      'pupille de l\'État',
    );
    if (pup) alerts.PUPILLE_ETAT = pup;

    // ADOPTANT_MARIE (boolean)
    const marie = this.buildBooleanAlert(
      'ADOPTANT_MARIE', this.adoptantMarie(),
      'ADOPTION_ADOPTANT_MARIE',
      ai?.adoptantMarieDetected ?? null,
      'Adoptant marié',
      'Adoptant célibataire',
      'situation matrimoniale adoptant',
    );
    if (marie) alerts.ADOPTANT_MARIE = marie;

    // AGE_ADOPTANT (number)
    const ageAd = this.buildNumberAlert(
      'AGE_ADOPTANT', this.ageAdoptant(),
      'ADOPTION_AGE_ADOPTANT',
      ai?.ageAdoptantDetecte ?? null,
      'Âge adoptant',
      'âge adoptant',
    );
    if (ageAd) alerts.AGE_ADOPTANT = ageAd;

    // AGE_ADOPTE (number)
    const ageEn = this.buildNumberAlert(
      'AGE_ADOPTE', this.ageAdopte(),
      'ADOPTION_AGE_ADOPTE',
      ai?.ageAdopteDetecte ?? null,
      'Âge adopté',
      'âge adopté',
    );
    if (ageEn) alerts.AGE_ADOPTE = ageEn;

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  /**
   * Chip alerte selon forme recommandée :
   * - rouge / critical si forme AUCUNE.
   * - or / warning si bascule plénière → simple détectée.
   * - null sinon.
   */
  formeRecommandeeAlert = computed<'critical' | 'warning' | null>(() => {
    const r = this.result();
    if (!r) return null;
    if (r.formeRecommandee === 'AUCUNE') return 'critical';
    if (r.formeAdoption !== r.formeRecommandee) return 'warning';
    return null;
  });

  constructor(
    private service: AdoptionService,
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

  alertTooltip(alert: AdoptionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AdoptionCoherenceAlert): string {
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

  /** Helper : alerte sur un champ booléen. */
  private buildBooleanAlert(
    field: AdoptionAlertField,
    userVal: boolean,
    critereCode: string,
    iaVal: boolean | null | undefined,
    trueLabel: string,
    falseLabel: string,
    fieldLabel: string,
  ): AdoptionCoherenceAlert | null {
    const builder = CoherenceAlertBuilder
        .forField<AdoptionAlertField>(field);

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

    // 3. IA — booléen direct.
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? trueLabel : falseLabel,
        reason: `Analyse du dossier : ${fieldLabel} ${iaVal ? 'détecté' : 'non détecté'}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Helper : alerte sur un champ enum string. */
  private buildEnumAlert(
    field: AdoptionAlertField,
    userVal: string,
    critereCode: string,
    iaVal: string | null | undefined,
    fieldLabel: string,
  ): AdoptionCoherenceAlert | null {
    const builder = CoherenceAlertBuilder
        .forField<AdoptionAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = chk.expectedValue?.toString().trim().toUpperCase();
      if (!ev || ev === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : ${fieldLabel} attendu = ${ev}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = q.expectedValue?.toString().trim().toUpperCase();
      if (!ev || ev === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — string enum direct.
    if (iaVal && typeof iaVal === 'string') {
      const norm = iaVal.trim().toUpperCase();
      if (norm && norm !== userVal && (norm === 'PLENIERE' || norm === 'SIMPLE')) {
        builder.addSource('IA', {
          expectedDisplay: norm,
          reason: `Analyse du dossier : ${fieldLabel} détecté = ${norm}`,
        });
      }
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Helper : alerte sur un champ numérique (âge). */
  private buildNumberAlert(
    field: AdoptionAlertField,
    userVal: number | null,
    critereCode: string,
    iaVal: number | null | undefined,
    label: string,
    fieldLabel: string,
  ): AdoptionCoherenceAlert | null {
    if (userVal === null) return null;
    const builder = CoherenceAlertBuilder
        .forField<AdoptionAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const num = Number(ev);
      if (Number.isNaN(num) || num === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: `${label} ${num}`,
        reason: `Checklist procédurale : ${fieldLabel} attendu = ${num}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. IA — number direct.
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: `${label} ${iaVal}`,
        reason: `Analyse du dossier : ${fieldLabel} détecté = ${iaVal}`,
      });
    }

    // 3. PIECE_MANQUANTE
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
    if (this.ageAdoptant() === null || this.ageAdopte() === null) return false;
    if ((this.ageAdoptant() ?? -1) < 0) return false;
    if ((this.ageAdopte() ?? -1) < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onFormeAdoptionChange(value: 'PLENIERE' | 'SIMPLE'): void {
    this.formeAdoption.set(value);
    this.provenanceFormeAdoption.set(null);
  }

  onAgeAdoptantChange(value: number | null): void {
    this.ageAdoptant.set(value === null || Number.isNaN(value) ? null : value);
    this.provenanceAgeAdoptant.set(null);
  }

  onAgeAdopteChange(value: number | null): void {
    this.ageAdopte.set(value === null || Number.isNaN(value) ? null : value);
    this.provenanceAgeAdopte.set(null);
  }

  onConsentementParentsChange(value: boolean): void {
    this.consentementParents.set(value);
  }

  onConsentementAdopteChange(value: boolean): void {
    this.consentementAdopte.set(value);
  }

  onConsentementConjointChange(value: boolean): void {
    this.consentementConjointAdoptant.set(value);
  }

  onEnquetesChange(value: boolean): void {
    this.enquetes.set(value);
  }

  onPlacement6moisChange(value: boolean): void {
    this.placement6mois.set(value);
  }

  onPupilleEtatChange(value: boolean): void {
    this.pupilleEtat.set(value);
    this.provenancePupilleEtat.set(null);
  }

  onAdoptantMarieChange(value: boolean): void {
    this.adoptantMarie.set(value);
    this.provenanceAdoptantMarie.set(null);
  }

  /**
   * SF-FA-18-10 : pré-fill depuis `aiData` (FamilleExtractedData).
   *
   * Sources IA :
   *  - `formeAdoptionDemandeeDetected` (PLENIERE/SIMPLE) → forme
   *  - `pupilleEtatDetected` (boolean) → pupilleEtat
   *  - `adoptantMarieDetected` (boolean) → adoptantMarie
   *  - `ageAdoptantDetecte` (number) → ageAdoptant
   *  - `ageAdopteDetecte` (number) → ageAdopte
   *
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore par défaut (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // formeAdoption
    const forme = ai.formeAdoptionDemandeeDetected;
    if ((forme === 'PLENIERE' || forme === 'SIMPLE')
        && (this.formeAdoption() === 'PLENIERE'
            || this.provenanceFormeAdoption() === 'IA')) {
      // Par défaut formeAdoption = PLENIERE — on respecte uniquement si l'IA
      // détecte 'SIMPLE' OU si la provenance était déjà IA.
      if (forme !== this.formeAdoption() || this.provenanceFormeAdoption() === 'IA') {
        this.formeAdoption.set(forme);
        this.provenanceFormeAdoption.set('IA');
      }
    }

    // pupilleEtat
    if (ai.pupilleEtatDetected === true
        && (!this.pupilleEtat() || this.provenancePupilleEtat() === 'IA')) {
      this.pupilleEtat.set(true);
      this.provenancePupilleEtat.set('IA');
    }

    // adoptantMarie
    if (ai.adoptantMarieDetected === true
        && (!this.adoptantMarie() || this.provenanceAdoptantMarie() === 'IA')) {
      this.adoptantMarie.set(true);
      this.provenanceAdoptantMarie.set('IA');
    }

    // ageAdoptant
    if (typeof ai.ageAdoptantDetecte === 'number' && ai.ageAdoptantDetecte >= 0
        && (this.ageAdoptant() === null
            || this.provenanceAgeAdoptant() === 'IA')) {
      this.ageAdoptant.set(ai.ageAdoptantDetecte);
      this.provenanceAgeAdoptant.set('IA');
    }

    // ageAdopte
    if (typeof ai.ageAdopteDetecte === 'number' && ai.ageAdopteDetecte >= 0
        && (this.ageAdopte() === null
            || this.provenanceAgeAdopte() === 'IA')) {
      this.ageAdopte.set(ai.ageAdopteDetecte);
      this.provenanceAgeAdopte.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AdoptionRequest = {
      formeAdoption: this.formeAdoption(),
      ageAdoptant: this.ageAdoptant()!,
      ageAdopte: this.ageAdopte()!,
      consentementParents: this.consentementParents(),
      consentementAdopte: this.consentementAdopte(),
      consentementConjointAdoptant: this.consentementConjointAdoptant(),
      enquetes: this.enquetes(),
      placement6mois: this.placement6mois(),
      pupilleEtat: this.pupilleEtat(),
      adoptantMarie: this.adoptantMarie(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Adoption analysée', 'OK', { duration: 2500 });
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
        this.provenanceFormeAdoption.set(null);
        this.provenancePupilleEtat.set(null);
        this.provenanceAdoptantMarie.set(null);
        this.provenanceAgeAdoptant.set(null);
        this.provenanceAgeAdopte.set(null);
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
   * - MOYENNE → or/warning (critères secondaires manquants).
   * - FAIBLE → rouge/critical (critère cardinal manquant).
   */
  bannerClass(verdict: VerdictRecevabiliteAdoption | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'adoption-banner adoption-banner--info';
      case 'MOYENNE': return 'adoption-banner adoption-banner--warning';
      case 'FAIBLE': return 'adoption-banner adoption-banner--critical';
      default: return 'adoption-banner adoption-banner--info';
    }
  }

  /** Libellé humain d'une forme. */
  formeLabel(code: FormeAdoption | null | undefined): string {
    if (!code) return '';
    const opt = FORME_ADOPTION_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }

  formeSub(code: FormeAdoption | null | undefined): string {
    if (!code) return '';
    const opt = FORME_ADOPTION_LABELS.find((o) => o.code === code);
    return opt?.sub ?? '';
  }
}

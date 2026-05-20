import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
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
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AbandonPostePresomptionDemissionService } from '../../core/services/abandon-poste-presomption-demission.service';
import {
  AbandonPosteModeNotification,
  AbandonPosteMotifAbsence,
  AbandonPostePresomptionDemissionRequest,
  AbandonPostePresomptionDemissionResponse,
  AbandonPosteVerdict,
} from '../../core/models/abandon-poste-presomption-demission.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { AbandonPostePresomptionDemissionSectionPrefillRules } from './abandon-poste-presomption-demission-section-prefill-rules';

/**
 * SF-206-02 : champs F-IA-03 audités par l'outil F-DT-42. Quatre champs
 * croisables avec la checklist procédurale F-96 / les questions IA / les
 * pièces manquantes.
 */
export type APDAlertField =
  | 'DATE_MISE_EN_DEMEURE'
  | 'DELAI_ACCORDE'
  | 'MENTIONS_MED'
  | 'MOTIF_LEGITIME';

export type APDCoherenceAlert = CoherenceAlert<APDAlertField>;

/** Codes `critereCode` (F-96 / questions IA) reconnus par field. */
const F96_CODE_DATE_MED = 'DT42_DATE_MISE_EN_DEMEURE';
const F96_CODE_DELAI = 'DT42_DELAI_ACCORDE';
const F96_CODE_MENTIONS_MED = 'DT42_MENTIONS_MED';
const F96_CODE_MOTIF_LEGITIME = 'DT42_MOTIF_LEGITIME';

/**
 * SF-206-02 : composant Angular standalone pour l'outil décisionnel
 * "Abandon de poste / présomption de démission" (F-DT-42). FRANCE uniquement
 * (mécanisme franco-français — loi 21/12/2022, L.1237-1-1 CT).
 *
 * Pattern de référence : `procedure-nullite-licenciement-section` (F-DT-36) —
 * formulaire de saisie, POST de calcul, verdict 3 niveaux, F-IA-03, refresh
 * dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict : CONTESTATION_SOLIDE (navy vert — la présomption tombe) /
 * CONTESTATION_INCERTAINE (or) / CONTESTATION_DIFFICILE (rouge — la présomption
 * opère). Rouge réservé à CONTESTATION_DIFFICILE (signal d'alerte pour l'avocat
 * du salarié).
 *
 * Échéance inter-onglets (SF-206-00b) : la date d'expiration du délai est
 * affichée avec un renvoi explicite vers l'onglet Suivi.
 */
@Component({
  selector: 'app-abandon-poste-presomption-demission-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './abandon-poste-presomption-demission-section.component.html',
  styleUrl: './abandon-poste-presomption-demission-section.component.scss',
})
export class AbandonPostePresomptionDemissionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ABANDON DE POSTE / PRÉSOMPTION DE DÉMISSION';
  static readonly TOOL_ICON = 'no_accounts';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return AbandonPostePresomptionDemissionSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Inputs IA / contexte (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /** Mode simulateur autonome (hors dossier) — coupe F-IA-03 + refresh. */
  @Input() standaloneMode = false;

  // Snapshots signal des inputs IA pour réactivité des `computed`.
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AbandonPostePresomptionDemissionResponse | null>(null);

  // --- Form fields ---
  dateMiseEnDemeurePresentee = signal<string | null>(null);
  modeNotificationMiseEnDemeure = signal<AbandonPosteModeNotification>('LRAR');
  delaiAccordeJours = signal<number>(15);
  miseEnDemeureMentionneDelai = signal<boolean>(true);
  miseEnDemeureMentionneConsequences = signal<boolean>(true);
  motifAbsenceInvoque = signal<AbandonPosteMotifAbsence>('AUCUN');
  motifAbsenceCommentaire = signal<string | null>(null);
  repriseOuJustificationDansDelai = signal<boolean>(false);
  dateRepriseOuJustification = signal<string | null>(null);

  /**
   * SF-206-02 : provenance IA par champ pré-rempli. `'IA'` tant que la valeur
   * provient de l'analyse ; remis à `null` dès qu'un handler `onXxxChange()`
   * détecte une modification manuelle (le badge `auto_awesome` disparaît).
   */
  provenanceDateMiseEnDemeure = signal<'IA' | null>(null);
  provenanceModeNotification = signal<'IA' | null>(null);
  provenanceDelaiAccordeJours = signal<'IA' | null>(null);
  provenanceMotifAbsence = signal<'IA' | null>(null);
  provenanceDateReprise = signal<'IA' | null>(null);
  provenanceMedMentionneDelai = signal<'IA' | null>(null);
  provenanceMedMentionneConsequences = signal<'IA' | null>(null);
  provenanceRepriseDansDelai = signal<'IA' | null>(null);

  /**
   * Coherence alerts F-IA-03 — gate `showForm()` strict (alertes masquées
   * après calcul rendu). Aucune source IA en standalone.
   */
  coherenceAlerts = computed<Partial<Record<APDAlertField, APDCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<APDAlertField, APDCoherenceAlert>> = {};
    const dateAlert = this.buildDateMedAlert();
    if (dateAlert) alerts.DATE_MISE_EN_DEMEURE = dateAlert;
    const delaiAlert = this.buildDelaiAccordeAlert();
    if (delaiAlert) alerts.DELAI_ACCORDE = delaiAlert;
    const mentionsAlert = this.buildMentionsMedAlert();
    if (mentionsAlert) alerts.MENTIONS_MED = mentionsAlert;
    const motifAlert = this.buildMotifLegitimeAlert();
    if (motifAlert) alerts.MOTIF_LEGITIME = motifAlert;
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.values(this.coherenceAlerts()).length }));

  /** Options du <mat-select> mode de notification. */
  readonly modeNotificationOptions: { value: AbandonPosteModeNotification; label: string }[] = [
    { value: 'LRAR', label: 'Lettre recommandée AR (LRAR)' },
    { value: 'REMISE_MAIN_PROPRE', label: 'Remise en main propre contre décharge' },
    { value: 'AUTRE', label: 'Autre (irrégulier)' },
  ];

  /** Options du <mat-select> motif d'absence invoqué. */
  readonly motifAbsenceOptions: { value: AbandonPosteMotifAbsence; label: string }[] = [
    { value: 'AUCUN', label: 'Aucun motif invoqué' },
    { value: 'MEDICAL', label: 'Motif médical (arrêt de travail, hospitalisation)' },
    { value: 'DROIT_RETRAIT', label: 'Droit de retrait (L.4131-1)' },
    { value: 'DROIT_GREVE', label: 'Droit de grève' },
    { value: 'MODIFICATION_CONTRAT_REFUSEE', label: 'Refus d\'une modification du contrat' },
    { value: 'DEFAUT_PAIEMENT_SALAIRE', label: 'Défaut de paiement du salaire' },
    { value: 'AUTRE', label: 'Autre motif légitime' },
  ];

  constructor(
    private service: AbandonPostePresomptionDemissionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA (SF-206-02) — renseigne les 8 champs depuis le sous-objet
   * `abandon_poste_detail` (projeté à plat dans `travailExtractedData`).
   *
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. Délègue au helper partagé. No-op
   * gracieux si `aiData` absent ou dossier BE. N'écrase pas une saisie
   * avocat (provenance `null` AVEC valeur modifiée = modification manuelle).
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = AbandonPostePresomptionDemissionSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // --- Date de la MED ---
    const dateMed = rules.computeDateMiseEnDemeure(ruleInput);
    if (dateMed
        && (this.dateMiseEnDemeurePresentee() === null || this.provenanceDateMiseEnDemeure() === 'IA')) {
      this.dateMiseEnDemeurePresentee.set(dateMed);
      this.provenanceDateMiseEnDemeure.set('IA');
    }

    // --- Mode de notification ---
    const mode = rules.computeModeNotification(ruleInput);
    if (mode !== null) {
      this.modeNotificationMiseEnDemeure.set(mode);
      this.provenanceModeNotification.set('IA');
    }

    // --- Délai accordé ---
    const delai = rules.computeDelaiAccordeJours(ruleInput);
    if (delai !== null) {
      this.delaiAccordeJours.set(delai);
      this.provenanceDelaiAccordeJours.set('IA');
    }

    // --- Mentions MED (booléens factuels) ---
    const medDelai = rules.computeMedMentionneDelai(ruleInput);
    if (medDelai !== null) {
      this.miseEnDemeureMentionneDelai.set(medDelai);
      this.provenanceMedMentionneDelai.set('IA');
    }
    const medCons = rules.computeMedMentionneConsequences(ruleInput);
    if (medCons !== null) {
      this.miseEnDemeureMentionneConsequences.set(medCons);
      this.provenanceMedMentionneConsequences.set('IA');
    }

    // --- Motif d'absence ---
    const motif = rules.computeMotifAbsence(ruleInput);
    if (motif !== null) {
      this.motifAbsenceInvoque.set(motif);
      this.provenanceMotifAbsence.set('IA');
    }

    // --- Reprise dans le délai (booléen + date associée) ---
    const reprise = rules.computeRepriseDansDelai(ruleInput);
    if (reprise !== null) {
      this.repriseOuJustificationDansDelai.set(reprise);
      this.provenanceRepriseDansDelai.set('IA');
    }
    const dateReprise = rules.computeDateReprise(ruleInput);
    if (dateReprise
        && (this.dateRepriseOuJustification() === null || this.provenanceDateReprise() === 'IA')) {
      this.dateRepriseOuJustification.set(dateReprise);
      this.provenanceDateReprise.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + délai ≥ 0. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const d = this.delaiAccordeJours();
    return typeof d === 'number' && d >= 0;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onDateMedChange(value: string | null): void {
    this.dateMiseEnDemeurePresentee.set(value || null);
    this.provenanceDateMiseEnDemeure.set(null);
  }

  onModeNotificationChange(value: AbandonPosteModeNotification): void {
    this.modeNotificationMiseEnDemeure.set(value);
    this.provenanceModeNotification.set(null);
  }

  onDelaiAccordeChange(value: number | string | null): void {
    const num = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
    if (!Number.isFinite(num) || num < 0) {
      this.delaiAccordeJours.set(0);
    } else {
      this.delaiAccordeJours.set(Math.trunc(num));
    }
    this.provenanceDelaiAccordeJours.set(null);
  }

  onMedMentionneDelaiChange(value: boolean): void {
    this.miseEnDemeureMentionneDelai.set(value);
    this.provenanceMedMentionneDelai.set(null);
  }

  onMedMentionneConsequencesChange(value: boolean): void {
    this.miseEnDemeureMentionneConsequences.set(value);
    this.provenanceMedMentionneConsequences.set(null);
  }

  onMotifAbsenceChange(value: AbandonPosteMotifAbsence): void {
    this.motifAbsenceInvoque.set(value);
    this.provenanceMotifAbsence.set(null);
    if (value === 'AUCUN') {
      this.motifAbsenceCommentaire.set(null);
    }
  }

  onRepriseDansDelaiChange(value: boolean): void {
    this.repriseOuJustificationDansDelai.set(value);
    this.provenanceRepriseDansDelai.set(null);
    if (!value) {
      this.dateRepriseOuJustification.set(null);
      this.provenanceDateReprise.set(null);
    }
  }

  onDateRepriseChange(value: string | null): void {
    this.dateRepriseOuJustification.set(value || null);
    this.provenanceDateReprise.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AbandonPostePresomptionDemissionRequest = {
      dateMiseEnDemeurePresentee: this.dateMiseEnDemeurePresentee() || null,
      modeNotificationMiseEnDemeure: this.modeNotificationMiseEnDemeure(),
      delaiAccordeJours: this.delaiAccordeJours(),
      miseEnDemeureMentionneDelai: this.miseEnDemeureMentionneDelai(),
      miseEnDemeureMentionneConsequences: this.miseEnDemeureMentionneConsequences(),
      motifAbsenceInvoque: this.motifAbsenceInvoque(),
      motifAbsenceCommentaire: this.motifAbsenceCommentaire()?.trim() || null,
      repriseOuJustificationDansDelai: this.repriseOuJustificationDansDelai(),
      dateRepriseOuJustification: this.repriseOuJustificationDansDelai()
        ? (this.dateRepriseOuJustification() || null)
        : null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la contestation calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        // OnPush + subscribe : forcer la détection de changement.
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: AbandonPostePresomptionDemissionResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs par
   * défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: AbandonPostePresomptionDemissionResponse): void {
    this.dateMiseEnDemeurePresentee.set(r.dateMiseEnDemeurePresentee);
    this.modeNotificationMiseEnDemeure.set(r.modeNotificationMiseEnDemeure);
    this.delaiAccordeJours.set(r.delaiAccordeJours);
    this.miseEnDemeureMentionneDelai.set(!!r.miseEnDemeureMentionneDelai);
    this.miseEnDemeureMentionneConsequences.set(!!r.miseEnDemeureMentionneConsequences);
    this.motifAbsenceInvoque.set(r.motifAbsenceInvoque);
    this.motifAbsenceCommentaire.set(r.motifAbsenceCommentaire);
    this.repriseOuJustificationDansDelai.set(!!r.repriseOuJustificationDansDelai);
    this.dateRepriseOuJustification.set(r.dateRepriseOuJustification);
    // SF-206-02 : valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceDateMiseEnDemeure.set(null);
    this.provenanceModeNotification.set(null);
    this.provenanceDelaiAccordeJours.set(null);
    this.provenanceMotifAbsence.set(null);
    this.provenanceDateReprise.set(null);
    this.provenanceMedMentionneDelai.set(null);
    this.provenanceMedMentionneConsequences.set(null);
    this.provenanceRepriseDansDelai.set(null);
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field croisable)
  // ---------------------------------------------------------------------------

  /** Date de présentation MED — divergence stricte F-96 / question IA. */
  private buildDateMedAlert(): APDCoherenceAlert | null {
    const user = this.dateMiseEnDemeurePresentee();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<APDAlertField>('DATE_MISE_EN_DEMEURE');
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DATE_MED) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : mise en demeure attendue le ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DATE_MED) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT42_DATE_MISE_EN_DEMEURE', 'MISE_EN_DEMEURE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Délai accordé — alerte IA si délai saisi < 15 jours (D.1237-2-1) + check
   * F-96 / question IA si divergence numérique.
   */
  private buildDelaiAccordeAlert(): APDCoherenceAlert | null {
    const user = this.delaiAccordeJours();
    const builder = CoherenceAlertBuilder.forField<APDAlertField>('DELAI_ACCORDE');
    if (typeof user === 'number' && user >= 0 && user < 15) {
      builder.addSource('IA', {
        expectedDisplay: 'Délai < 15 jours',
        reason: `Le délai saisi (${user} j) est inférieur au minimum légal de 15 jours calendaires fixé par D.1237-2-1 — irrégularité de l'employeur, à exploiter dans la contestation.`,
      });
    }
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_DELAI) continue;
      const ev = chk.expectedValue?.trim();
      if (!ev || ev === String(user)) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : délai attendu ${ev} j${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_DELAI) continue;
      const ev = q.expectedValue?.trim();
      if (!ev || ev === String(user)) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    return builder.build();
  }

  /** Mentions de la MED — divergence F-96 / question IA sur l'un des 2 booléens. */
  private buildMentionsMedAlert(): APDCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<APDAlertField>('MENTIONS_MED');
    const userBoth = this.miseEnDemeureMentionneDelai() && this.miseEnDemeureMentionneConsequences();
    const userDisplay = userBoth ? 'Mentions complètes' : 'Mentions incomplètes';
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_MENTIONS_MED) continue;
      const expected = this.parseBoolean(chk.expectedValue);
      if (expected === null || expected === userBoth) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Mentions complètes' : 'Mentions incomplètes',
        reason: `Checklist procédurale : MED ${expected ? 'complète' : 'incomplète'}${chk.raison ? ' (' + chk.raison + ')' : ''} — saisie : ${userDisplay.toLowerCase()}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_MENTIONS_MED) continue;
      const expected = this.parseBoolean(q.expectedValue);
      if (expected === null || expected === userBoth) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Mentions complètes' : 'Mentions incomplètes',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT42_MENTIONS_MED', 'MISE_EN_DEMEURE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Motif légitime invoqué — divergence F-96 / question IA. */
  private buildMotifLegitimeAlert(): APDCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<APDAlertField>('MOTIF_LEGITIME');
    const userHasMotif = this.motifAbsenceInvoque() !== 'AUCUN';
    const userDisplay = userHasMotif ? 'Motif légitime invoqué' : 'Aucun motif légitime';
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== F96_CODE_MOTIF_LEGITIME) continue;
      const expected = this.parseBoolean(chk.expectedValue);
      if (expected === null || expected === userHasMotif) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Motif légitime' : 'Aucun motif',
        reason: `Checklist procédurale : motif légitime ${expected ? 'attesté' : 'absent'}${chk.raison ? ' (' + chk.raison + ')' : ''} — saisie : ${userDisplay.toLowerCase()}`,
      });
      break;
    }
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== F96_CODE_MOTIF_LEGITIME) continue;
      const expected = this.parseBoolean(q.expectedValue);
      if (expected === null || expected === userHasMotif) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Motif légitime' : 'Aucun motif',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
    const piece = this.findPieceManquante(['DT42_MOTIF_LEGITIME', 'ARRET_DE_TRAVAIL', 'JUSTIFICATIF_ABSENCE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /** Parse une `expectedValue` textuelle en booléen, ou null si non mappable. */
  private parseBoolean(raw: string | null | undefined): boolean | null {
    if (!raw) return null;
    const v = raw.trim().toLowerCase();
    if (v === 'true' || v === 'oui' || v === '1') return true;
    if (v === 'false' || v === 'non' || v === '0') return false;
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

  alertTooltip(alert: APDCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: APDCoherenceAlert): string {
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
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. Du point de vue de l'avocat du salarié :
   * une contestation solide est une opportunité (navy/vert) ; difficile est
   * un signal d'alerte (rouge — la présomption opère).
   */
  verdictBannerClass(verdict: AbandonPosteVerdict): string {
    switch (verdict) {
      case 'CONTESTATION_SOLIDE':
        return 'apd-verdict-banner apd-verdict-banner--available';
      case 'CONTESTATION_INCERTAINE':
        return 'apd-verdict-banner apd-verdict-banner--medium';
      case 'CONTESTATION_DIFFICILE':
        return 'apd-verdict-banner apd-verdict-banner--danger';
    }
  }

  verdictBannerLabel(verdict: AbandonPosteVerdict): string {
    switch (verdict) {
      case 'CONTESTATION_SOLIDE': return 'Contestation solide';
      case 'CONTESTATION_INCERTAINE': return 'Contestation incertaine';
      case 'CONTESTATION_DIFFICILE': return 'Contestation difficile';
    }
  }

  verdictBannerIcon(verdict: AbandonPosteVerdict): string {
    switch (verdict) {
      case 'CONTESTATION_SOLIDE': return 'check_circle';
      case 'CONTESTATION_INCERTAINE': return 'warning';
      case 'CONTESTATION_DIFFICILE': return 'error';
    }
  }
}

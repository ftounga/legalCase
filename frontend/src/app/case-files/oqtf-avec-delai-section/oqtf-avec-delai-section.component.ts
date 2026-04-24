import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OqtfAvecDelaiService } from '../../core/services/oqtf-avec-delai.service';
import {
  MOTIFS_OQTF,
  MotifOqtf,
  MotifOqtfOption,
  OqtfAvecDelaiResponse,
  StatutDelai,
} from '../../core/models/oqtf-avec-delai.model';
import { ImmigrationExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSeverity } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { DecisionalHeaderFlagComponent } from '../decisional-tools-panel/decisional-header-flag/decisional-header-flag.component';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/** Enum valeurs valides pour pré-fill / alertes motif. */
const MOTIFS_OQTF_SET: ReadonlySet<MotifOqtf> = new Set<MotifOqtf>([
  'REFUS_TITRE', 'EXPIRATION_TITRE', 'SEJOUR_IRREGULIER', 'RETRAIT_TITRE', 'AUTRE',
]);

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type OqtfAlertField = 'DATE_NOTIFICATION' | 'MOTIF_OQTF' | 'RECOURS_FORME';

// SF-155-05 : types factorisés — rétro-compat via aliases.
export type OqtfAlertSeverity = CoherenceAlertSeverity;
export type OqtfCoherenceAlert = CoherenceAlert<OqtfAlertField>;

/**
 * SF-IM-08-02 : outil décisionnel dédié "OQTF avec délai de départ
 * volontaire — France" (F-IM-08). FR uniquement — OQT belge (annexe 13)
 * traitée dans SF-IM-08-05 / SF-IM-08-06 (procédure juridiquement
 * distincte, invariant "un outil = une situation métier"). Consomme
 * l'API SF-IM-08-01. Affiché conditionnellement par le panel F-IA-04
 * (tool_id 'F-IM-08-oqtf-avec-delai-fr').
 *
 * SF-155-04-B1 (2026-04-24) : pré-fill IA depuis `ImmigrationExtractedData`
 * FR + validation F-IA-03 (3 alertes de cohérence dont 1 critique pour
 * détecter un recours déjà formé oublié par l'avocat).
 */
@Component({
  selector: 'app-oqtf-avec-delai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    // SF-155-07 (DIV-6) : flag header partagé (cohérent avec oqtf-sans-delai + annexe13-be).
    DecisionalHeaderFlagComponent,
  ],
  templateUrl: './oqtf-avec-delai-section.component.html',
  styleUrl: './oqtf-avec-delai-section.component.scss',
})
export class OqtfAvecDelaiSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04-B1 : sources IA pour pré-fill + alertes de cohérence.
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // SF-155-06 : signals miroirs des inputs IA pour que les `computed`
  // (coherenceAlerts) réagissent aux changements post-mount.
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfAvecDelaiResponse | null>(null);

  dateNotificationOqtf = signal<string | null>(null);
  motifOqtf = signal<MotifOqtf | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  // SF-155-04-B1 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceMotifOqtf = signal<'IA' | null>(null);
  provenanceRecoursForme = signal<'IA' | null>(null);

  readonly motifs: MotifOqtfOption[] = MOTIFS_OQTF;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  // SF-155-04-B1 : alertes de cohérence F-IA-03 sur 3 champs.
  coherenceAlerts = computed<Partial<Record<OqtfAlertField, OqtfCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<OqtfAlertField, OqtfCoherenceAlert>> = {};
    const a1 = this.buildDateNotificationAlert();
    if (a1) alerts.DATE_NOTIFICATION = a1;
    const a2 = this.buildMotifOqtfAlert();
    if (a2) alerts.MOTIF_OQTF = a2;
    const a3 = this.buildRecoursFormeAlert();
    if (a3) alerts.RECOURS_FORME = a3;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  // SF-155-07 (DIV-7) : map {sourceKey → explanations} pour popovers SF-IA-03-15c.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private service: OqtfAvecDelaiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
    // SF-155-07 (DIV-7) : injection `@Optional()` — fail-open strict.
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // SF-155-06 : hydrate les signals miroirs avant évaluations computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isFrance()) {
      this.load();
      // SF-155-07 (DIV-7) : pré-charge les explications F-IA-03-15a.
      this.loadSourceExplanations();
    }
  }

  /**
   * SF-155-07 (DIV-7) : charge les explications de sources via F-IA-03-15a.
   * Fail-open strict : erreur ou absence du service = map vide.
   */
  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-155-07 (DIV-7) : mapping field → sourceKey. Convention `IM08_<FIELD>`.
   */
  explanationFor(field: OqtfAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_NOTIFICATION': return 'IM08_DATE_NOTIFICATION';
        case 'MOTIF_OQTF': return 'IM08_MOTIF_OQTF';
        case 'RECOURS_FORME': return 'IM08_RECOURS_FORME';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    // SF-155-06 : ré-hydrate les signals miroirs à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // Re-prefill si aiData arrive après le mount et qu'aucune analyse persistée
    // n'est présente ni en cours de saisie avancée. On évite d'écraser :
    // - l'analyse déjà chargée (result !== null),
    // - une valeur déjà saisie par l'avocat (on ne ré-applique que si le champ
    //   est encore vide — c'est-à-dire que le formulaire n'a pas été touché).
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateNotificationOqtf();
    const m = this.motifOqtf();
    if (!d || !m) return false;
    if (d > this.todayIso) return false; // pas dans le futur
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr) return false;
      if (dr < d) return false; // recours >= notification
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** SF-155-04-B1 : handlers de modification manuelle — effacement du badge IA. */
  onDateNotificationChange(value: string | null): void {
    this.dateNotificationOqtf.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onMotifOqtfChange(value: MotifOqtf | null): void {
    this.motifOqtf.set(value);
    this.provenanceMotifOqtf.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    this.provenanceRecoursForme.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateNotificationOqtf: this.dateNotificationOqtf()!,
      motifOqtf: this.motifOqtf()!,
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? { dateRecours: this.dateRecours()! }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('OQTF analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  /**
   * Classe CSS du bandeau de résultat selon le statut calculé.
   * Rouge réservé au seul statut EXPIRE (alerte critique —
   * DESIGN_SYSTEM.md autorise la couleur rouge pour les statuts
   * d'urgence uniquement).
   */
  bannerClass(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':   return 'oqtf-banner oqtf-banner--info';
      case 'URGENT':       return 'oqtf-banner oqtf-banner--warning';
      case 'EXPIRE':       return 'oqtf-banner oqtf-banner--danger';
      case 'RECOURS_FORME':return 'oqtf-banner oqtf-banner--success';
      default:             return 'oqtf-banner';
    }
  }

  bannerIcon(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'URGENT':        return 'Délai urgent';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  /** Affiche le compteur jours restants si pertinent (hors EXPIRE/RECOURS_FORME). */
  showJoursRestants(r: OqtfAvecDelaiResponse | null): boolean {
    if (!r) return false;
    return r.statutDelaiRecours === 'DISPONIBLE' || r.statutDelaiRecours === 'URGENT';
  }

  /**
   * SF-155-04-B1 : libellé du badge d'alerte côté template (tooltip fallback).
   */
  alertBadgeLabel(alert: OqtfCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque d\'irrecevabilité'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /**
   * SF-155-04-B1 : pré-remplissage des 3 champs clés depuis l'analyse IA.
   * Pas d'écrasement des valeurs déjà saisies.
   */
  private prefillFromAi(): void {
    const ai = this.aiData;
    if (!ai) return;

    // 1. Date de notification OQTF — format YYYY-MM-DD strict, non futur.
    const date = ai.dateNotificationOqtf;
    if (typeof date === 'string' && ISO_DATE_RE.test(date) && date <= this.todayIso) {
      if (!this.dateNotificationOqtf()) {
        this.dateNotificationOqtf.set(date);
        this.provenanceDateNotification.set('IA');
      }
    }

    // 2. Motif OQTF — seulement si valeur dans l'enum front (garde-fou).
    const code = ai.motifOqtfCode;
    if (code && MOTIFS_OQTF_SET.has(code as MotifOqtf)) {
      if (!this.motifOqtf()) {
        this.motifOqtf.set(code as MotifOqtf);
        this.provenanceMotifOqtf.set('IA');
      }
    }

    // 3. Recours formé — dérivé de DetectedAnswer {OUI/NON/INCONNU}.
    //    INCONNU = pas de signal, on reste à la valeur par défaut (false).
    const detected = ai.recoursFormeDetected;
    if (detected && (detected.reponse === 'OUI' || detected.reponse === 'NON')) {
      // On ne pré-fill que si l'avocat n'a pas déjà manipulé le toggle
      // (signal à 'false' défaut → inchangé si détecté NON, mais badge sans
      // effet visible ; pour OUI on change la valeur).
      if (this.provenanceRecoursForme() === null && !this.recoursForme()) {
        this.recoursForme.set(detected.reponse === 'OUI');
        this.provenanceRecoursForme.set('IA');
      } else if (this.provenanceRecoursForme() === null && this.recoursForme() && detected.reponse === 'OUI') {
        // Déjà true par défaut impossible ici (défaut false) — no-op défensif
        this.provenanceRecoursForme.set('IA');
      }
    }
  }

  private buildDateNotificationAlert(): OqtfCoherenceAlert | null {
    const user = this.dateNotificationOqtf();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<OqtfAlertField>('DATE_NOTIFICATION').withSeverity('WARNING');

    // IA : divergence date.
    const ai = this.aiDataSignal()?.dateNotificationOqtf;
    if (typeof ai === 'string' && ISO_DATE_RE.test(ai) && ai !== user) {
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `L'analyse a détecté une date de notification différente : ${ai}`,
      });
    }

    // SF-155-06 : pièce manquante (Annexe OQTF elle-même / notification).
    const piece = this.findPieceManquante(['IM08_DATE_NOTIFICATION', 'IM08_OQTF_NOTIFICATION']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildMotifOqtfAlert(): OqtfCoherenceAlert | null {
    const user = this.motifOqtf();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<OqtfAlertField>('MOTIF_OQTF').withSeverity('WARNING');

    // SF-155-06 : F96 — checklist procédurale `IM08_MOTIF_OQTF` NON_COMPLIANT.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM08_MOTIF_OQTF') continue;
      const ev = chk.expectedValue?.toUpperCase() as MotifOqtf | undefined;
      if (!ev || !MOTIFS_OQTF_SET.has(ev)) continue;
      if (ev === user) continue;
      const label = MOTIFS_OQTF.find(m => m.code === ev)?.label ?? ev;
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : motif attendu ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // IA : motifOqtfCode.
    const aiCode = this.aiDataSignal()?.motifOqtfCode;
    if (aiCode && MOTIFS_OQTF_SET.has(aiCode as MotifOqtf) && aiCode !== user) {
      const label = MOTIFS_OQTF.find(m => m.code === aiCode)?.label ?? aiCode;
      builder.addSource('IA', {
        expectedDisplay: label,
        reason: `L'analyse a détecté un motif différent : ${label}`,
      });
    }

    // SF-155-06 : PIECE_MANQUANTE sur le motif.
    const piece = this.findPieceManquante(['IM08_MOTIF_OQTF']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte critique : l'IA a détecté un recours déjà formé mais l'avocat a
   * laissé le toggle à "non formé" — risque majeur d'oubli (irrecevabilité
   * potentielle ou action en doublon).
   * SF-155-06 : enrichi avec QUESTION_IA (réponse "oui" à une question "Un
   * recours a-t-il déjà été formé ?") et PIECE_MANQUANTE (requête en annulation).
   */
  private buildRecoursFormeAlert(): OqtfCoherenceAlert | null {
    if (this.recoursForme()) return null; // avocat en phase avec la détection
    const builder = CoherenceAlertBuilder.forField<OqtfAlertField>('RECOURS_FORME').withSeverity('CRITICAL');

    // SF-155-06 : QUESTION_IA — réponse "oui" sur IM08_RECOURS_FORME.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM08_RECOURS_FORME') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: 'Recours déjà formé détecté',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // IA : recoursFormeDetected === 'OUI'.
    const detected = this.aiDataSignal()?.recoursFormeDetected;
    if (detected && detected.reponse === 'OUI') {
      builder.addSource('IA', {
        expectedDisplay: 'Recours déjà formé détecté',
        reason: `L'analyse a détecté qu'un recours a déjà été formé${detected.justification ? ' — ' + detected.justification : ''}. Vérifiez avant d'introduire une nouvelle action (risque d'irrecevabilité).`,
      });
    }

    // SF-155-06 : PIECE_MANQUANTE (requête en annulation, accusé de réception).
    const piece = this.findPieceManquante(['IM08_RECOURS_FORME']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-06 : recherche une `PieceManquanteEntry` dont `critereCode`
   * appartient (case-insensitive) à la liste acceptée. Retourne le 1er `texte`
   * trouvé, ou `null`.
   */
  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationOqtf.set(r.dateNotificationOqtf);
        this.motifOqtf.set(r.motifOqtf);
        this.recoursForme.set(r.recoursForme);
        this.dateRecours.set(r.dateRecours);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire et on
        // tente le pré-fill depuis l'IA (si aiData disponible au mount).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}

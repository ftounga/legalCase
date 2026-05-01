import { Component, Input, OnChanges, OnInit, Optional, SimpleChanges, signal, computed } from '@angular/core';
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
import { Annexe13BeService } from '../../core/services/annexe13-be.service';
import {
  MOTIFS_OQT,
  MotifOqt,
  MotifOqtOption,
  TYPES_RECOURS,
  TypeRecours,
  TypeRecoursOption,
  Annexe13BeResponse,
  StatutRecoursAnnul,
} from '../../core/models/annexe13-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DecisionalHeaderFlagComponent } from '../decisional-tools-panel/decisional-header-flag/decisional-header-flag.component';
import { ImmigrationExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/** SF-155-04-C : whitelist stricte alignée sur Annexe13BeCalculator.MOTIFS_VALIDES (4 codes). */
const MOTIFS_OQT_WHITELIST = new Set<MotifOqt>(
  MOTIFS_OQT.map(m => m.code),
);

/** SF-155-04-C : champs éligibles à la validation F-IA-03. */
export type IM08AnnexeBeAlertField =
  | 'TRANSFERT_IMMINENT'
  | 'DATE_NOTIFICATION'
  | 'DELAI_DEPART'
  | 'MOTIF_OQT';

// SF-155-05 : alias rétro-compat — `CoherenceAlert<F>` partagée.
// Le champ `blocker: boolean` legacy est dérivé de `severity === 'CRITICAL'`.
export type IM08AnnexeBeAlertSource = CoherenceAlertSource;
export type IM08AnnexeBeCoherenceAlert = CoherenceAlert<IM08AnnexeBeAlertField>;

/**
 * SF-IM-08-06 : outil décisionnel dédié "Annexe 13 — OQT belge"
 * (F-IM-08). BE uniquement — l'OQTF française est traitée dans
 * SF-IM-08-01/02 (avec délai) et SF-IM-08-03/04 (sans délai)
 * (procédures juridiquement distinctes, invariant
 * "un outil = une situation métier"). Consomme l'API SF-IM-08-05.
 * Affiché conditionnellement par le panel F-IA-04
 * (tool_id 'F-IM-08-annexe13-be').
 *
 * SF-155-04-C : pré-fill IA + validation F-IA-03 (pattern canonique
 * `immigration-title-decision-section`).
 */
@Component({
  selector: 'app-annexe13-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    DecisionalHeaderFlagComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './annexe13-be-section.component.html',
  styleUrl: './annexe13-be-section.component.scss',
})
export class Annexe13BeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ANNEXE 13 — OQT BELGE (BE)';
  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  /** SF-155-04-C : 4 champs IA BE pour pré-fill (ImmigrationExtractedData côté BE). */
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Annexe13BeResponse | null>(null);

  dateNotificationAnnexe13 = signal<string | null>(null);
  delaiDepartImposeJours = signal<number>(30);
  motifOqt = signal<MotifOqt | null>(null);
  transfertImminent = signal<boolean>(false);
  recoursForme = signal<boolean>(false);
  typeRecours = signal<TypeRecours | null>(null);
  dateRecours = signal<string | null>(null);

  // SF-155-04-C : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacée dès que l'avocat modifie manuellement le champ (onXxxChange).
  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceDelaiDepart = signal<'IA' | null>(null);
  provenanceMotifOqt = signal<'IA' | null>(null);
  provenanceTransfertImminent = signal<'IA' | null>(null);

  // SF-155-04-C : signals miroir des inputs (cohérent avec pattern canonique).
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // SF-IA-03-15c — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly motifs: MotifOqtOption[] = MOTIFS_OQT;
  readonly typesRecours: TypeRecoursOption[] = TYPES_RECOURS;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * SF-155-04-C : alertes de cohérence F-IA-03 exposées par field.
   * Retourne `{}` quand le formulaire n'est pas visible (pattern canonique).
   * SF-155-06 : délègue la construction de chaque alerte à un `buildXxx()`
   * dédié exploitant les 4 sources (F96 / QUESTION_IA / IA / PIECE_MANQUANTE)
   * via le helper `CoherenceAlertBuilder` partagé.
   */
  coherenceAlerts = computed<Partial<Record<IM08AnnexeBeAlertField, IM08AnnexeBeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM08AnnexeBeAlertField, IM08AnnexeBeCoherenceAlert>> = {};

    const transfert = this.buildTransfertAlert();
    if (transfert) alerts.TRANSFERT_IMMINENT = transfert;
    const dateA = this.buildDateNotificationAlert();
    if (dateA) alerts.DATE_NOTIFICATION = dateA;
    const delai = this.buildDelaiDepartAlert();
    if (delai) alerts.DELAI_DEPART = delai;
    const motif = this.buildMotifOqtAlert();
    if (motif) alerts.MOTIF_OQT = motif;

    return alerts;
  });

  // SF-155-06 : 4 builders extraits — F96 / QUESTION_IA / IA / PIECE_MANQUANTE.

  private buildTransfertAlert(): IM08AnnexeBeCoherenceAlert | null {
    if (this.transfertImminent() !== false) return null;
    const builder = CoherenceAlertBuilder.forField<IM08AnnexeBeAlertField>('TRANSFERT_IMMINENT')
      .withSeverity('CRITICAL');
    const ai = this.aiDataSignal();
    if (ai?.transfertImminentDetected === true) {
      builder.addSource('IA', {
        expectedDisplay: 'Transfert imminent détecté',
        reason: 'L\'IA a détecté un risque de transfert imminent (centre fermé, escorte, vol programmé) — à vérifier',
      });
    }
    const piece = this.findPieceManquante(['IM08_TRANSFERT', 'IM08_TRANSFERT_IMMINENT']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildDateNotificationAlert(): IM08AnnexeBeCoherenceAlert | null {
    const userDate = this.dateNotificationAnnexe13();
    if (!userDate) return null;
    const builder = CoherenceAlertBuilder.forField<IM08AnnexeBeAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');
    const ai = this.aiDataSignal();
    const aiDate = typeof ai?.dateNotificationAnnexe13 === 'string' ? ai.dateNotificationAnnexe13 : null;
    if (aiDate && aiDate !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de notification ${aiDate}`,
      });
    }
    const piece = this.findPieceManquante(['IM08_DATE_NOTIFICATION']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildDelaiDepartAlert(): IM08AnnexeBeCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<IM08AnnexeBeAlertField>('DELAI_DEPART')
      .withSeverity('WARNING');
    const ai = this.aiDataSignal();
    const aiDelai = ai?.delaiDepartImposeJours;
    if (typeof aiDelai === 'number' && Number.isInteger(aiDelai) && aiDelai >= 0
        && aiDelai !== this.delaiDepartImposeJours()) {
      builder.addSource('IA', {
        expectedDisplay: `${aiDelai} jour(s)`,
        reason: `Analyse du dossier : délai de départ imposé = ${aiDelai} jour(s)`,
      });
    }
    const piece = this.findPieceManquante(['IM08_DELAI_DEPART']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  private buildMotifOqtAlert(): IM08AnnexeBeCoherenceAlert | null {
    const userMotif = this.motifOqt();
    if (!userMotif) return null;
    const builder = CoherenceAlertBuilder.forField<IM08AnnexeBeAlertField>('MOTIF_OQT')
      .withSeverity('WARNING');

    // SF-155-06 : F96 — critereCode `IM08_MOTIF_OQT_BE` NON_COMPLIANT.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM08_MOTIF_OQT_BE'
          && chk.critereCode?.toUpperCase() !== 'IM08_MOTIF_OQT') continue;
      const ev = chk.expectedValue?.toUpperCase() as MotifOqt | undefined;
      if (!ev || !MOTIFS_OQT_WHITELIST.has(ev)) continue;
      if (ev === userMotif) continue;
      const label = MOTIFS_OQT.find(m => m.code === ev)?.label ?? ev;
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : motif attendu ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // IA : motifOqtCodeBe.
    const ai = this.aiDataSignal();
    const aiMotif = ai?.motifOqtCodeBe as MotifOqt | null | undefined;
    if (aiMotif && MOTIFS_OQT_WHITELIST.has(aiMotif) && aiMotif !== userMotif) {
      const label = MOTIFS_OQT.find(m => m.code === aiMotif)?.label ?? aiMotif;
      builder.addSource('IA', {
        expectedDisplay: label,
        reason: `Analyse du dossier : motif ${label}`,
      });
    }

    const piece = this.findPieceManquante(['IM08_MOTIF_OQT_BE', 'IM08_MOTIF_OQT']);
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

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // SF-155-05 : blockers dérivés de severity === 'CRITICAL' (ancien champ `blocker: boolean`).
    const blockers = values.filter(a => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: Annexe13BeService,
    private snackBar: MatSnackBar,
    private sourceExplanationService: SourceExplanationService,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isBelgium()) {
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
    // SF-155-04-C : ré-applique le prefill si aiData change, seulement si l'on est
    // encore en mode formulaire ET qu'aucun résultat persisté n'a été chargé
    // (évite d'écraser la saisie avocat ou un résultat backend existant).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : map field → sourceKey (fallback générique annexe13). */
  explanationFor(field: IM08AnnexeBeAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'TRANSFERT_IMMINENT': return 'IM08_TRANSFERT';
        case 'DATE_NOTIFICATION': return 'IM08_DATE_NOTIFICATION';
        case 'DELAI_DEPART': return 'IM08_DELAI_DEPART';
        case 'MOTIF_OQT': return 'IM08_MOTIF_OQT';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: IM08AnnexeBeCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: IM08AnnexeBeCoherenceAlert): string {
    // SF-155-05 : `blocker` legacy → `severity === 'CRITICAL'`.
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateNotificationAnnexe13();
    const m = this.motifOqt();
    const delai = this.delaiDepartImposeJours();
    if (!d || !m) return false;
    if (d > this.todayIso) return false; // pas dans le futur
    if (delai === null || delai === undefined) return false;
    if (delai < 0 || delai > 30) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      const tr = this.typeRecours();
      if (!dr || !tr) return false;
      if (dr < d) return false; // recours >= notification
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateNotificationAnnexe13: this.dateNotificationAnnexe13()!,
      delaiDepartImposeJours: this.delaiDepartImposeJours(),
      motifOqt: this.motifOqt()!,
      transfertImminent: this.transfertImminent(),
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? {
            dateRecours: this.dateRecours()!,
            typeRecours: this.typeRecours()!,
          }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Annexe 13 analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  // SF-155-04-C : handlers onChange — effacent la provenance IA au 1er changement manuel.
  onDateNotificationChange(value: string | null): void {
    this.dateNotificationAnnexe13.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onDelaiDepartChange(value: number | null | undefined): void {
    this.delaiDepartImposeJours.set(value ?? 30);
    this.provenanceDelaiDepart.set(null);
  }

  onMotifOqtChange(value: MotifOqt | null): void {
    this.motifOqt.set(value);
    this.provenanceMotifOqt.set(null);
  }

  onTransfertImminentChange(value: boolean): void {
    this.transfertImminent.set(value);
    this.provenanceTransfertImminent.set(null);
  }

  /**
   * SF-155-04-C : pré-fill IA depuis aiData BE (ImmigrationExtractedData).
   *
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - chaque champ est indépendant (prefill partiel OK)
   * - motif hors-whitelist → skip
   * - délai non entier / négatif → skip (garde défaut 30)
   * - transfert non boolean → skip
   * - date : passthrough sans parsing (format déjà normalisé backend)
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    if (typeof ai.dateNotificationAnnexe13 === 'string' && ai.dateNotificationAnnexe13.length > 0) {
      this.dateNotificationAnnexe13.set(ai.dateNotificationAnnexe13);
      this.provenanceDateNotification.set('IA');
    }

    if (typeof ai.delaiDepartImposeJours === 'number'
        && Number.isInteger(ai.delaiDepartImposeJours)
        && ai.delaiDepartImposeJours >= 0) {
      this.delaiDepartImposeJours.set(ai.delaiDepartImposeJours);
      this.provenanceDelaiDepart.set('IA');
    }

    const motif = ai.motifOqtCodeBe as MotifOqt | null | undefined;
    if (motif && typeof motif === 'string' && MOTIFS_OQT_WHITELIST.has(motif)) {
      this.motifOqt.set(motif);
      this.provenanceMotifOqt.set('IA');
    }

    if (typeof ai.transfertImminentDetected === 'boolean') {
      this.transfertImminent.set(ai.transfertImminentDetected);
      this.provenanceTransfertImminent.set('IA');
    }
  }

  /**
   * Classe CSS du bandeau de résultat selon le statut calculé.
   * Rouge réservé au seul statut EXPIRE (alerte critique —
   * DESIGN_SYSTEM.md autorise la couleur rouge pour les statuts
   * d'urgence uniquement).
   */
  bannerClass(statut: StatutRecoursAnnul | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':   return 'annexe13-banner annexe13-banner--info';
      case 'URGENT':       return 'annexe13-banner annexe13-banner--warning';
      case 'EXPIRE':       return 'annexe13-banner annexe13-banner--danger';
      case 'RECOURS_FORME':return 'annexe13-banner annexe13-banner--success';
      default:             return 'annexe13-banner';
    }
  }

  bannerIcon(statut: StatutRecoursAnnul | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutRecoursAnnul | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'URGENT':        return 'Délai urgent';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  typeRecoursLabel(code: TypeRecours | null | undefined): string {
    switch (code) {
      case 'ANNULATION_30J':       return 'Annulation 30 jours';
      case 'EXTREME_URGENCE_5JO':  return 'Extrême urgence 5 jours ouvrables';
      default:                     return '';
    }
  }

  /** Affiche le compteur jours restants si pertinent (hors EXPIRE/RECOURS_FORME). */
  showJoursRestants(r: Annexe13BeResponse | null): boolean {
    if (!r) return false;
    return r.statutRecoursAnnulation === 'DISPONIBLE' || r.statutRecoursAnnulation === 'URGENT';
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationAnnexe13.set(r.dateNotificationAnnexe13);
        this.delaiDepartImposeJours.set(r.delaiDepartImposeJours);
        this.motifOqt.set(r.motifOqt);
        this.transfertImminent.set(r.transfertImminent);
        this.recoursForme.set(r.recoursForme);
        this.typeRecours.set(r.typeRecours);
        this.dateRecours.set(r.dateRecours);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire
        // et on applique le pré-fill IA si aiData disponible (SF-155-04-C).
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }
}

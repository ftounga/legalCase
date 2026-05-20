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
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
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
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { C4OnemChecklistService } from '../../core/services/c4-onem-checklist.service';
import {
  C4_ONEM_MENTION_LABELS,
  C4OnemChecklistRequest,
  C4OnemChecklistResponse,
  C4OnemMention,
  C4OnemVerdict,
} from '../../core/models/c4-onem-checklist.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { C4OnemChecklistPrefillRules } from './c4-onem-checklist-section-prefill-rules';

/**
 * SF-207-04 (frontend) — Champs d'alerte de cohérence F-IA-03 exposés par
 * l'outil F-207 C4 ONEM checklist conformité.
 */
export type C4OnemAlertField =
  | 'DATE_SORTIE_SERVICE'
  | 'FAUTE_GRAVE_MENTIONNEE';

export type C4OnemAlertSource = CoherenceAlertSource;
export type C4OnemCoherenceAlert = CoherenceAlert<C4OnemAlertField>;

/** Seuil en jours absolu au-delà duquel on alerte sur une divergence de date. */
const DATE_DIVERGENCE_DAYS = 15;

/**
 * SF-207-04 — Outil décisionnel « C4 ONEM — checklist conformité » (F-207).
 * BELGIQUE uniquement (AR 25/11/1991 portant réglementation du chômage,
 * art. 92 mentions obligatoires + art. 144 exclusion faute grave 4-52 sem.).
 *
 * Consomme l'API SF-207-02-backend (PR #1123) — endpoint
 * `/api/v1/case-files/{caseFileId}/decision-tools/c4-onem-checklist`.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-207-c4-onem-checklist`, règle ALWAYS_ON BE + travail seedée par la
 * migration livrée avec cette SF).
 *
 * Pattern canonique : `prescription-be-litige-travail-section` (F-IA-04
 * signal-based, gate strict BE, pré-fill IA + F-IA-03, refresh dashboard).
 */
@Component({
  selector: 'app-c4-onem-checklist-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatCheckboxModule, MatSlideToggleModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './c4-onem-checklist-section.component.html',
  styleUrl: './c4-onem-checklist-section.component.scss',
})
export class C4OnemChecklistSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'C4 ONEM — CHECKLIST CONFORMITÉ (BE)';
  static readonly TOOL_ICON = 'fact_check';

  /** F-177 SF-177-12 — delegate au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return C4OnemChecklistPrefillRules.computePrefillCount(input);
  }

  readonly mentionLabels = C4_ONEM_MENTION_LABELS;

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR (le C4 est un document belge). */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<C4OnemChecklistResponse | null>(null);

  // -- Form fields (signals) --
  raisonSocialeEmployeur = signal<string | null>(null);
  numeroBce = signal<string | null>(null);
  nomSalarie = signal<string | null>(null);
  numeroNationalRegistre = signal<string | null>(null);
  dateEntreeService = signal<string | null>(null);
  dateSortieService = signal<string | null>(null);
  categorieOnem = signal<string | null>(null);
  motifExplicite = signal<string | null>(null);
  fauteGraveMentionnee = signal<boolean>(false);
  preavisPresteJours = signal<number | null>(null);
  dernierSalaireMensuelBrut = signal<number | null>(null);

  // -- Provenance IA par champ pré-rempli --
  provenanceRaisonSociale = signal<'IA' | 'IA_DERIVE' | null>(null);
  provenanceNumeroBce = signal<'IA' | 'IA_DERIVE' | null>(null);
  provenanceNomSalarie = signal<'IA' | null>(null);
  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDateSortie = signal<'IA' | 'IA_DERIVE' | null>(null);
  provenanceCategorieOnem = signal<'IA' | null>(null);
  provenanceMotifExplicite = signal<'IA' | 'IA_DERIVE' | null>(null);
  provenanceFauteGrave = signal<'IA' | null>(null);
  provenancePreavis = signal<'IA' | null>(null);
  provenanceDernierSalaire = signal<'IA' | 'IA_DERIVE' | null>(null);

  // SF-IA-03-15 : map {sourceKey → explanations} (popover enrichi).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03. Gate : uniquement en mode formulaire.
   * Fields audités :
   *  - `DATE_SORTIE_SERVICE` : écart absolu ≥ 15 jours entre la date de
   *    rupture IA et la saisie avocat.
   *  - `FAUTE_GRAVE_MENTIONNEE` : l'IA détecte « faute grave » dans le motif
   *    mais l'avocat décoche le toggle (l'avocat conteste la mention C4).
   */
  coherenceAlerts = computed<Partial<Record<C4OnemAlertField, C4OnemCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<C4OnemAlertField, C4OnemCoherenceAlert>> = {};
    const dateAlert = this.buildDateSortieAlert();
    if (dateAlert) alerts.DATE_SORTIE_SERVICE = dateAlert;
    const fauteAlert = this.buildFauteGraveAlert();
    if (fauteAlert) alerts.FAUTE_GRAVE_MENTIONNEE = fauteAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: C4OnemChecklistService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);

    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-207-04 — Pré-remplit le form depuis `aiData`. Délègue au helper pur
   * partagé pour garantir la parité avec le static `getPrefillCount`.
   *
   * Stratégie : on ne pré-remplit que les champs encore vides ou déjà marqués
   * `IA*` (un champ saisi manuellement n'est jamais écrasé).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. raisonSocialeEmployeur — provenance IA si direct, IA_DERIVE si fallback.
    const rsValueDirect = (typeof ai.raisonSocialeEmployeur === 'string' && ai.raisonSocialeEmployeur.trim().length > 0);
    const rsValue = C4OnemChecklistPrefillRules.computeRaisonSocialeEmployeur(ruleInput);
    if (rsValue !== null && this.canPrefill(this.raisonSocialeEmployeur, this.provenanceRaisonSociale)) {
      this.raisonSocialeEmployeur.set(rsValue);
      this.provenanceRaisonSociale.set(rsValueDirect ? 'IA' : 'IA_DERIVE');
    }

    // 2. numeroBce — provenance IA si direct, IA_DERIVE si fallback bceEmployeur.
    const bceDirect = typeof ai.numeroBce === 'string' && ai.numeroBce.replace(/\D/g, '').length === 10;
    const bceValue = C4OnemChecklistPrefillRules.computeNumeroBce(ruleInput);
    if (bceValue !== null && this.canPrefill(this.numeroBce, this.provenanceNumeroBce)) {
      this.numeroBce.set(bceValue);
      this.provenanceNumeroBce.set(bceDirect ? 'IA' : 'IA_DERIVE');
    }

    // 3. nomSalarie
    const nomValue = C4OnemChecklistPrefillRules.computeNomSalarie(ruleInput);
    if (nomValue !== null && this.canPrefill(this.nomSalarie, this.provenanceNomSalarie)) {
      this.nomSalarie.set(nomValue);
      this.provenanceNomSalarie.set('IA');
    }

    // 4. dateEntreeService
    const dateEntreeValue = C4OnemChecklistPrefillRules.computeDateEntreeService(ruleInput);
    if (dateEntreeValue !== null && this.canPrefill(this.dateEntreeService, this.provenanceDateEntree)) {
      this.dateEntreeService.set(dateEntreeValue);
      this.provenanceDateEntree.set('IA');
    }

    // 5. dateSortieService — IA si dateRuptureContrat, IA_DERIVE si fallback dateLicenciement.
    const dateRuptureDirect = typeof ai.dateRuptureContrat === 'string'
      && /^\d{4}-\d{2}-\d{2}$/.test(ai.dateRuptureContrat);
    const dateSortieValue = C4OnemChecklistPrefillRules.computeDateSortieService(ruleInput);
    if (dateSortieValue !== null && this.canPrefill(this.dateSortieService, this.provenanceDateSortie)) {
      this.dateSortieService.set(dateSortieValue);
      this.provenanceDateSortie.set(dateRuptureDirect ? 'IA' : 'IA_DERIVE');
    }

    // 6. categorieOnem
    const catValue = C4OnemChecklistPrefillRules.computeCategorieOnem(ruleInput);
    if (catValue !== null && this.canPrefill(this.categorieOnem, this.provenanceCategorieOnem)) {
      this.categorieOnem.set(catValue);
      this.provenanceCategorieOnem.set('IA');
    }

    // 7. motifExplicite — IA si direct, IA_DERIVE si fallback motifLicenciement/motifRupture.
    const motifDirect = typeof ai.motifExplicite === 'string'
      && ai.motifExplicite.trim().length >= 5;
    const motifValue = C4OnemChecklistPrefillRules.computeMotifExplicite(ruleInput);
    if (motifValue !== null && this.canPrefill(this.motifExplicite, this.provenanceMotifExplicite)) {
      this.motifExplicite.set(motifValue);
      this.provenanceMotifExplicite.set(motifDirect ? 'IA' : 'IA_DERIVE');
    }

    // 8. fauteGraveMentionnee — pré-coche le toggle si signal IA détecté.
    const fauteValue = C4OnemChecklistPrefillRules.computeFauteGraveMentionnee(ruleInput);
    if (fauteValue !== null && this.provenanceFauteGrave() === null) {
      this.fauteGraveMentionnee.set(fauteValue);
      this.provenanceFauteGrave.set('IA');
    }

    // 9. preavisPresteJours
    const preavisValue = C4OnemChecklistPrefillRules.computePreavisPresteJours(ruleInput);
    if (preavisValue !== null && this.canPrefill(this.preavisPresteJours, this.provenancePreavis)) {
      this.preavisPresteJours.set(preavisValue);
      this.provenancePreavis.set('IA');
    }

    // 10. dernierSalaireMensuelBrut — IA si direct, IA_DERIVE si fallback salaireBrutMensuel.
    const salaireDirect = typeof ai.dernierSalaireMensuelBrut === 'number'
      && ai.dernierSalaireMensuelBrut > 0;
    const salaireValue = C4OnemChecklistPrefillRules.computeDernierSalaireMensuelBrut(ruleInput);
    if (salaireValue !== null && this.canPrefill(this.dernierSalaireMensuelBrut, this.provenanceDernierSalaire)) {
      this.dernierSalaireMensuelBrut.set(salaireValue);
      this.provenanceDernierSalaire.set(salaireDirect ? 'IA' : 'IA_DERIVE');
    }
  }

  /**
   * Helper : un pré-fill IA n'écrase pas un champ saisi manuellement
   * (provenance `null` + valeur non null = saisie manuelle).
   */
  private canPrefill<T>(field: { (): T | null }, provenance: { (): unknown }): boolean {
    return field() === null || provenance() !== null;
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** Mapping field → sourceKey pour le popover enrichi. */
  explanationFor(field: C4OnemAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_SORTIE_SERVICE': return 'date_rupture_contrat';
        case 'FAUTE_GRAVE_MENTIONNEE': return 'motif_rupture';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: C4OnemCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: C4OnemCoherenceAlert): string {
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

  /** Divergence dateSortieService — écart absolu ≥ 15 jours entre IA et saisie. */
  private buildDateSortieAlert(): C4OnemCoherenceAlert | null {
    const aiDate = this.aiDataSignal()?.dateRuptureContrat;
    const userDate = this.dateSortieService();
    if (!aiDate || !userDate) return null;
    const diff = dateDaysDiff(aiDate, userDate);
    if (diff === null || diff < DATE_DIVERGENCE_DAYS) return null;
    return CoherenceAlertBuilder.forField<C4OnemAlertField>('DATE_SORTIE_SERVICE')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de rupture du contrat ${aiDate}`,
      })
      .build();
  }

  /**
   * Divergence faute grave — l'IA détecte le motif faute grave mais l'avocat
   * décoche le toggle (cas où l'avocat conteste la qualification du C4).
   */
  private buildFauteGraveAlert(): C4OnemCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const iaDetected = C4OnemChecklistPrefillRules.computeFauteGraveMentionnee(
      { aiData: ai },
    );
    if (iaDetected !== true) return null;
    if (this.fauteGraveMentionnee()) return null;
    const motif = nonEmpty(ai.motifExplicite) ?? nonEmpty(ai.motifRupture)
      ?? nonEmpty(ai.motifLicenciement) ?? 'motif faute grave';
    return CoherenceAlertBuilder.forField<C4OnemAlertField>('FAUTE_GRAVE_MENTIONNEE')
      .addSource('IA', {
        expectedDisplay: 'Faute grave détectée',
        reason: `Analyse du dossier : motif "${motif}" → mention faute grave probable sur le C4`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  /**
   * Le form est valide quand les 4 champs strictement obligatoires côté
   * contrat sont remplis : `nomSalarie`, `dateEntreeService`,
   * `dateSortieService`, `fauteGraveMentionnee` (toujours présent — boolean).
   * Les autres champs peuvent être absents — leur absence est traitée par le
   * calculateur comme une mention manquante (verdict NON_CONFORME).
   */
  formValid(): boolean {
    const nom = this.nomSalarie();
    const entree = this.dateEntreeService();
    const sortie = this.dateSortieService();
    if (!nom || nom.trim().length === 0) return false;
    if (!entree || !sortie) return false;
    // Vérification simple : sortie ≥ entrée (le backend re-valide en 400 sinon).
    return Date.parse(sortie) >= Date.parse(entree);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers (effacent la provenance IA) --
  onRaisonSocialeChange(value: string | null): void {
    this.raisonSocialeEmployeur.set(value || null);
    this.provenanceRaisonSociale.set(null);
  }
  onNumeroBceChange(value: string | null): void {
    this.numeroBce.set(value || null);
    this.provenanceNumeroBce.set(null);
  }
  onNomSalarieChange(value: string | null): void {
    this.nomSalarie.set(value || null);
    this.provenanceNomSalarie.set(null);
  }
  onNumeroNationalRegistreChange(value: string | null): void {
    this.numeroNationalRegistre.set(value || null);
  }
  onDateEntreeChange(value: string | null): void {
    this.dateEntreeService.set(value);
    this.provenanceDateEntree.set(null);
  }
  onDateSortieChange(value: string | null): void {
    this.dateSortieService.set(value);
    this.provenanceDateSortie.set(null);
  }
  onCategorieOnemChange(value: string | null): void {
    this.categorieOnem.set(value || null);
    this.provenanceCategorieOnem.set(null);
  }
  onMotifExpliciteChange(value: string | null): void {
    this.motifExplicite.set(value || null);
    this.provenanceMotifExplicite.set(null);
  }
  onFauteGraveChange(value: boolean): void {
    this.fauteGraveMentionnee.set(value);
    this.provenanceFauteGrave.set(null);
  }
  onPreavisChange(value: number | null): void {
    this.preavisPresteJours.set(value);
    this.provenancePreavis.set(null);
  }
  onDernierSalaireChange(value: number | null): void {
    this.dernierSalaireMensuelBrut.set(value);
    this.provenanceDernierSalaire.set(null);
  }

  /** Classe CSS du verdict (vert / ambre / rouge). */
  verdictClass(verdict: C4OnemVerdict): string {
    switch (verdict) {
      case 'CONFORME': return 'verdict-ok';
      case 'NON_CONFORME': return 'verdict-warn';
      case 'RISQUE_EXCLUSION_FAUTE_GRAVE': return 'verdict-critical';
    }
  }

  verdictIcon(verdict: C4OnemVerdict): string {
    switch (verdict) {
      case 'CONFORME': return 'check_circle';
      case 'NON_CONFORME': return 'warning';
      case 'RISQUE_EXCLUSION_FAUTE_GRAVE': return 'error';
    }
  }

  verdictLabel(verdict: C4OnemVerdict): string {
    switch (verdict) {
      case 'CONFORME': return 'C4 conforme';
      case 'NON_CONFORME': return 'C4 à rectifier';
      case 'RISQUE_EXCLUSION_FAUTE_GRAVE': return 'Risque d\'exclusion ONEM';
    }
  }

  /** Libellé FR d'une mention manquante. */
  mentionLabel(m: C4OnemMention): string {
    return this.mentionLabels[m] ?? m;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: C4OnemChecklistRequest = {
      raisonSocialeEmployeur: this.raisonSocialeEmployeur(),
      numeroBce: this.numeroBce(),
      nomSalarie: this.nomSalarie()!,
      numeroNationalRegistre: this.numeroNationalRegistre(),
      dateEntreeService: this.dateEntreeService()!,
      dateSortieService: this.dateSortieService()!,
      categorieOnem: this.categorieOnem(),
      motifExplicite: this.motifExplicite(),
      fauteGraveMentionnee: this.fauteGraveMentionnee(),
      preavisPresteJours: this.preavisPresteJours(),
      dernierSalaireMensuelBrut: this.dernierSalaireMensuelBrut(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.raisonSocialeEmployeur.set(r.raisonSocialeEmployeur);
        this.numeroBce.set(r.numeroBce);
        this.nomSalarie.set(r.nomSalarie);
        this.numeroNationalRegistre.set(r.numeroNationalRegistre);
        this.dateEntreeService.set(r.dateEntreeService);
        this.dateSortieService.set(r.dateSortieService);
        this.categorieOnem.set(r.categorieOnem);
        this.motifExplicite.set(r.motifExplicite);
        this.fauteGraveMentionnee.set(r.fauteGraveMentionnee);
        this.preavisPresteJours.set(r.preavisPresteJours);
        this.dernierSalaireMensuelBrut.set(r.dernierSalaireMensuelBrut);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceRaisonSociale.set(null);
        this.provenanceNumeroBce.set(null);
        this.provenanceNomSalarie.set(null);
        this.provenanceDateEntree.set(null);
        this.provenanceDateSortie.set(null);
        this.provenanceCategorieOnem.set(null);
        this.provenanceMotifExplicite.set(null);
        this.provenanceFauteGrave.set(null);
        this.provenancePreavis.set(null);
        this.provenanceDernierSalaire.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}

/** Différence absolue en jours entre 2 dates ISO. Null si parsing échoue. */
function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}

function nonEmpty(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

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
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
  C4OnemChecklistRequest,
  C4OnemChecklistResponse,
  C4OnemMention,
  C4OnemVerdict,
  humanizeMention,
} from '../../core/models/c4-onem-checklist.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { C4OnemChecklistPrefillRules } from './c4-onem-checklist-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-207-02b — Champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-207 (checklist C4 ONEM Travail BE). Limité aux 3 champs « critiques »
 * où une divergence avocat/IA porte un risque procédural (date d'entrée,
 * date de sortie, qualification de la faute grave).
 */
export type C4OnemAlertField = 'DATE_ENTREE' | 'DATE_SORTIE' | 'FAUTE_GRAVE';

export type C4OnemAlertSource = CoherenceAlertSource;
export type C4OnemCoherenceAlert = CoherenceAlert<C4OnemAlertField>;

/**
 * Seuil d'écart en jours absolu au-delà duquel on affiche une alerte de
 * cohérence entre la date IA et la saisie avocat (aligné canonique
 * F-DT-25 / F-207 SF-207-01b).
 */
const DATE_DIVERGENCE_DAYS = 15;

/**
 * SF-207-02b : outil décisionnel "Checklist C4 ONEM" (F-207). BE uniquement
 * (AR du 25 novembre 1991 art. 92 / art. 144 + Loi 03/07/1978). Consomme
 * l'API SF-207-02 (PR #1123).
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `c4-onem-checklist`, règle ALWAYS_ON BE + travail).
 *
 * Pattern canonique : `prescription-be-litige-travail-section` (SF-207-01b)
 * dupliqué et étendu aux 10 champs + verdict tri-état + lettre rectificative.
 */
@Component({
  selector: 'app-c4-onem-checklist-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './c4-onem-checklist-section.component.html',
  styleUrl: './c4-onem-checklist-section.component.scss',
})
export class C4OnemChecklistSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'c4-onem-checklist';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CHECKLIST C4 ONEM (BE)';
  static readonly TOOL_ICON = 'fact_check';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return C4OnemChecklistPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — l'outil n'a pas d'équivalent FR. */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Exposé au template pour humaniser les mentions.
  readonly humanizeMention = humanizeMention;

  private cdr = inject(ChangeDetectorRef);

  // Snapshots signal des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  copying = signal(false);
  showForm = signal(true);
  result = signal<C4OnemChecklistResponse | null>(null);

  // Champs du form (signals — édités via handlers).
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

  // Provenance IA par champ pré-rempli.
  provenanceRaisonSociale = signal<'IA' | null>(null);
  provenanceNumeroBce = signal<'IA' | null>(null);
  provenanceNomSalarie = signal<'IA' | null>(null);
  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDateSortie = signal<'IA' | null>(null);
  provenanceCategorieOnem = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);
  provenanceFauteGrave = signal<'IA' | null>(null);
  provenancePreavis = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  // SF-IA-03-15 : map {sourceKey → explanations} (popover enrichi).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Gate strict workspace BE (l'outil n'existe pas côté FR). */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence calculées dynamiquement (3 fields).
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<C4OnemAlertField, C4OnemCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<C4OnemAlertField, C4OnemCoherenceAlert>> = {};
    const dEntree = this.buildDateAlert('DATE_ENTREE');
    if (dEntree) alerts.DATE_ENTREE = dEntree;
    const dSortie = this.buildDateAlert('DATE_SORTIE');
    if (dSortie) alerts.DATE_SORTIE = dSortie;
    const fGrave = this.buildFauteGraveAlert();
    if (fGrave) alerts.FAUTE_GRAVE = fGrave;
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

    // Ré-applique le pré-fill si aiData change avant première résolution.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-207-02b — Pré-remplit le form depuis `aiData`. Délègue intégralement
   * au helper pur partagé pour garantir la parité runtime / static
   * `getPrefillCount` (divergence impossible par construction).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. raisonSocialeEmployeur
    const rs = C4OnemChecklistPrefillRules.computeRaisonSociale(ruleInput);
    if (rs !== null && (this.raisonSocialeEmployeur() === null || this.provenanceRaisonSociale() === 'IA')) {
      this.raisonSocialeEmployeur.set(rs);
      this.provenanceRaisonSociale.set('IA');
    }

    // 2. numeroBce
    const bce = C4OnemChecklistPrefillRules.computeNumeroBce(ruleInput);
    if (bce !== null && (this.numeroBce() === null || this.provenanceNumeroBce() === 'IA')) {
      this.numeroBce.set(bce);
      this.provenanceNumeroBce.set('IA');
    }

    // 3. nomSalarie
    const nom = C4OnemChecklistPrefillRules.computeNomSalarie(ruleInput);
    if (nom !== null && (this.nomSalarie() === null || this.provenanceNomSalarie() === 'IA')) {
      this.nomSalarie.set(nom);
      this.provenanceNomSalarie.set('IA');
    }

    // 4. dateEntreeService
    const dEnt = C4OnemChecklistPrefillRules.computeDateEntreeService(ruleInput);
    if (dEnt !== null && (this.dateEntreeService() === null || this.provenanceDateEntree() === 'IA')) {
      this.dateEntreeService.set(dEnt);
      this.provenanceDateEntree.set('IA');
    }

    // 5. dateSortieService
    const dSor = C4OnemChecklistPrefillRules.computeDateSortieService(ruleInput);
    if (dSor !== null && (this.dateSortieService() === null || this.provenanceDateSortie() === 'IA')) {
      this.dateSortieService.set(dSor);
      this.provenanceDateSortie.set('IA');
    }

    // 6. categorieOnem
    const cat = C4OnemChecklistPrefillRules.computeCategorieOnem(ruleInput);
    if (cat !== null && (this.categorieOnem() === null || this.provenanceCategorieOnem() === 'IA')) {
      this.categorieOnem.set(cat);
      this.provenanceCategorieOnem.set('IA');
    }

    // 7. motifExplicite
    const mot = C4OnemChecklistPrefillRules.computeMotifExplicite(ruleInput);
    if (mot !== null && (this.motifExplicite() === null || this.provenanceMotif() === 'IA')) {
      this.motifExplicite.set(mot);
      this.provenanceMotif.set('IA');
    }

    // 8. fauteGraveMentionnee (true only)
    const fg = C4OnemChecklistPrefillRules.computeFauteGraveMentionnee(ruleInput);
    if (fg === true && (this.provenanceFauteGrave() === null || this.provenanceFauteGrave() === 'IA')) {
      this.fauteGraveMentionnee.set(true);
      this.provenanceFauteGrave.set('IA');
    }

    // 9. preavisPresteJours
    const prv = C4OnemChecklistPrefillRules.computePreavisPresteJours(ruleInput);
    if (prv !== null && (this.preavisPresteJours() === null || this.provenancePreavis() === 'IA')) {
      this.preavisPresteJours.set(prv);
      this.provenancePreavis.set('IA');
    }

    // 10. dernierSalaireMensuelBrut
    const sal = C4OnemChecklistPrefillRules.computeDernierSalaire(ruleInput);
    if (sal !== null && (this.dernierSalaireMensuelBrut() === null || this.provenanceSalaire() === 'IA')) {
      this.dernierSalaireMensuelBrut.set(sal);
      this.provenanceSalaire.set('IA');
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => { this.sourceExplanations.set(map); this.cdr.markForCheck(); },
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15 : mapping field → sourceKey pour le popover enrichi. */
  explanationFor(field: C4OnemAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_ENTREE': return 'date_entree';
        case 'DATE_SORTIE': return 'date_rupture_contrat';
        case 'FAUTE_GRAVE': return 'motif_rupture';
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

  /**
   * Divergence date (entrée / sortie) — écart absolu ≥ 15 jours entre IA
   * et saisie. Source IA :
   *   - DATE_ENTREE  ← `aiData.dateEntree`
   *   - DATE_SORTIE  ← `aiData.dateRuptureContrat`
   */
  private buildDateAlert(field: 'DATE_ENTREE' | 'DATE_SORTIE'): C4OnemCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const aiDate = field === 'DATE_ENTREE' ? ai.dateEntree : ai.dateRuptureContrat;
    const userDate = field === 'DATE_ENTREE' ? this.dateEntreeService() : this.dateSortieService();
    if (!aiDate || !userDate) return null;
    const diff = dateDaysDiff(aiDate, userDate);
    if (diff === null || diff < DATE_DIVERGENCE_DAYS) return null;
    const label = field === 'DATE_ENTREE' ? 'date d\'entrée en service' : 'date de rupture du contrat';
    return CoherenceAlertBuilder.forField<C4OnemAlertField>(field)
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : ${label} ${aiDate}`,
      })
      .build();
  }

  /**
   * Divergence faute grave — l'IA détecte « faute grave » dans `motifRupture`
   * mais l'avocat a décoché la case (ou vice-versa).
   */
  private buildFauteGraveAlert(): C4OnemCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    const iaFG = C4OnemChecklistPrefillRules.computeFauteGraveMentionnee({ aiData: ai });
    if (iaFG !== true) return null;
    const userFG = this.fauteGraveMentionnee();
    if (userFG === true) return null;
    return CoherenceAlertBuilder.forField<C4OnemAlertField>('FAUTE_GRAVE')
      .addSource('IA', {
        expectedDisplay: 'oui (faute grave détectée)',
        reason: `Analyse du dossier : motif "${ai.motifRupture}" contient « faute grave »`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const nom = (this.nomSalarie() ?? '').trim();
    const de = this.dateEntreeService();
    const ds = this.dateSortieService();
    if (!nom || !de || !ds) return false;
    // Cohérence min : sortie ≥ entrée (le calculateur le re-vérifiera).
    return ds >= de;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onRaisonSocialeChange(value: string | null): void {
    this.raisonSocialeEmployeur.set(value);
    this.provenanceRaisonSociale.set(null);
  }
  onNumeroBceChange(value: string | null): void {
    this.numeroBce.set(value);
    this.provenanceNumeroBce.set(null);
  }
  onNomSalarieChange(value: string | null): void {
    this.nomSalarie.set(value);
    this.provenanceNomSalarie.set(null);
  }
  onNumeroNationalRegistreChange(value: string | null): void {
    this.numeroNationalRegistre.set(value);
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
    this.categorieOnem.set(value);
    this.provenanceCategorieOnem.set(null);
  }
  onMotifExpliciteChange(value: string | null): void {
    this.motifExplicite.set(value);
    this.provenanceMotif.set(null);
  }
  onFauteGraveChange(value: boolean): void {
    this.fauteGraveMentionnee.set(value);
    this.provenanceFauteGrave.set(null);
  }
  onPreavisChange(value: number | null): void {
    this.preavisPresteJours.set(value);
    this.provenancePreavis.set(null);
  }
  onSalaireChange(value: number | null): void {
    this.dernierSalaireMensuelBrut.set(value);
    this.provenanceSalaire.set(null);
  }

  /** Classe CSS du verdict (verte / ambre / rouge). */
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
      case 'NON_CONFORME': return 'C4 non conforme';
      case 'RISQUE_EXCLUSION_FAUTE_GRAVE': return 'Risque exclusion ONEM (faute grave)';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: C4OnemChecklistRequest = {
      raisonSocialeEmployeur: this.raisonSocialeEmployeur() || null,
      numeroBce: this.numeroBce() || null,
      nomSalarie: (this.nomSalarie() ?? '').trim(),
      numeroNationalRegistre: this.numeroNationalRegistre() || null,
      dateEntreeService: this.dateEntreeService()!,
      dateSortieService: this.dateSortieService()!,
      categorieOnem: this.categorieOnem() || null,
      motifExplicite: this.motifExplicite() || null,
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
        // Refresh dashboard décisionnel (pattern SF-IA-02-03).
        this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
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
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Liste humanisée des mentions manquantes (FR) — alimente la liste à puces
   * du verdict NON_CONFORME.
   */
  mentionsManquantesHumanisees(): { code: C4OnemMention; label: string }[] {
    const r = this.result();
    if (!r) return [];
    return r.mentionsManquantes.map(m => ({ code: m, label: humanizeMention(m) }));
  }

  /** Copie la lettre rectificative dans le presse-papier (Clipboard API). */
  copyLettre(): void {
    const lettre = this.result()?.lettreRectificativeProposee;
    if (!lettre || this.copying()) return;
    this.copying.set(true);
    navigator.clipboard.writeText(lettre).then(
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Lettre rectificative copiée dans le presse-papier.',
          'Fermer',
          { duration: 3000, panelClass: 'snack-success' },
        );
        this.cdr.markForCheck();
      },
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Impossible de copier la lettre dans le presse-papier.',
          'Fermer',
          { duration: 4000, panelClass: 'snack-error' },
        );
        this.cdr.markForCheck();
      },
    );
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
        this.provenanceMotif.set(null);
        this.provenanceFauteGrave.set(null);
        this.provenancePreavis.set(null);
        this.provenanceSalaire.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}

/**
 * Utilitaire : différence absolue en jours entre 2 dates ISO. Renvoie null
 * si l'une des dates n'est pas parsable.
 */
function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}

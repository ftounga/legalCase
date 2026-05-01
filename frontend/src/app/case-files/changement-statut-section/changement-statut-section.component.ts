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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangementStatutService } from '../../core/services/changement-statut.service';
import {
  ChangementStatutRequest,
  ChangementStatutResponse,
  TITRE_SEJOUR_LABELS,
  TitreSejourCode,
  VerdictTransition,
  mapTitreSejourFromIa,
  titreLabel,
} from '../../core/models/changement-statut.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-IM-11-02 : champs F-IA-03 audités par l'outil "Changement de statut".
 * - TITRE_ACTUEL : divergence si IA détecte un titre actuel et l'avocat
 *   saisit une valeur différente (ex. IA = ETUDIANT, avocat = VISITEUR).
 */
export type ChangementStatutAlertField = 'TITRE_ACTUEL';

export type ChangementStatutAlertSource = CoherenceAlertSource;
export type ChangementStatutCoherenceAlert = CoherenceAlert<ChangementStatutAlertField>;

/**
 * SF-IM-11-02 : section frontend dédiée Changement de statut CESEDA (FR).
 * Régime art. L.421+ et R.5221 CESEDA — voie de transition entre titres.
 *
 * Single-country FR — Belgique : bannière info ("régime CESEDA propre à
 * la France") + form masqué (pas d'appel HTTP). L'équivalent BE relève
 * de la procédure 9bis OE (feature jumelle au backlog F-IM-11-BE).
 *
 * Pattern de référence : `protection-rp-section` (SF-DT-30-02 PR #634
 * — multi-fieldsets + bandeau verdict 3 niveaux + builder F-IA-03).
 * Gate FR avec bannière info importé de `aes-metiers-tension-section`
 * (SF-IM-09-05).
 */
@Component({
  selector: 'app-changement-statut-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSlideToggleModule,
    MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './changement-statut-section.component.html',
  styleUrl: './changement-statut-section.component.scss',
})
export class ChangementStatutSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CHANGEMENT DE STATUT (FR)';
  static readonly TOOL_ICON = 'swap_horiz';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Sources IA pour pré-fill + alertes de cohérence F-IA-03.
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ChangementStatutResponse | null>(null);

  // Form fields (signal-based).
  titreActuel = signal<TitreSejourCode | null>(null);
  titreEnvisage = signal<TitreSejourCode | null>(null);
  dureeRestanteSurTitreActuelMois = signal<number | null>(null);
  documentJustificatifFourni = signal<boolean>(false);
  remunerationContratEur = signal<number | null>(null);
  casierJudiciaireVierge = signal<boolean>(true);

  /** Provenance IA — badge "Pré-rempli depuis l'analyse" effaçable. */
  provenanceTitreActuel = signal<'IA' | null>(null);
  /** SF-IM-11-03 : provenance IA pour la durée restante calculée depuis dateExpirationTitre. */
  provenanceDureeRestante = signal<'IA' | null>(null);

  /** SF-IM-11-03 : flag de log "warn date malformée" — émis une seule fois par instance. */
  private dateExpirationParseWarned = false;

  /** Listes pour mat-radio. */
  readonly titreOptions = TITRE_SEJOUR_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Le champ rémunération est conditionnellement visible (titreEnvisage = SALARIE). */
  remunerationVisible = computed<boolean>(() => this.titreEnvisage() === 'SALARIE');

  /** Alertes de cohérence F-IA-03 par field — gate uniquement en mode formulaire. */
  coherenceAlerts = computed<Partial<Record<ChangementStatutAlertField, ChangementStatutCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<ChangementStatutAlertField, ChangementStatutCoherenceAlert>> = {};
    const a = this.buildTitreActuelAlert();
    if (a) alerts.TITRE_ACTUEL = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: ChangementStatutService,
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

    // Re-prefill quand aiData arrive après le mount, sans écraser les valeurs avocat.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (!this.titreActuel()) return false;
    if (!this.titreEnvisage()) return false;
    const duree = this.dureeRestanteSurTitreActuelMois();
    if (duree === null || duree === undefined || duree < 0) return false;
    // Rémunération obligatoire si titre envisagé = SALARIE.
    if (this.remunerationVisible()) {
      const remu = this.remunerationContratEur();
      if (remu === null || remu === undefined || remu < 0) return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers de modification — effacent la provenance IA.
  onTitreActuelChange(value: TitreSejourCode | null): void {
    this.titreActuel.set(value);
    this.provenanceTitreActuel.set(null);
  }

  onTitreEnvisageChange(value: TitreSejourCode | null): void {
    this.titreEnvisage.set(value);
    if (value !== 'SALARIE') {
      // Champ masqué — vide la rémunération pour ne pas envoyer un résidu.
      this.remunerationContratEur.set(null);
    }
  }

  onDureeRestanteChange(value: number | null): void {
    this.dureeRestanteSurTitreActuelMois.set(value === null || value === undefined ? null : value);
    // SF-IM-11-03 : toute saisie manuelle efface la provenance IA (badge masqué).
    this.provenanceDureeRestante.set(null);
  }

  onDocumentJustificatifChange(value: boolean): void {
    this.documentJustificatifFourni.set(value);
  }

  onRemunerationChange(value: number | null): void {
    this.remunerationContratEur.set(value === null || value === undefined ? null : value);
  }

  onCasierJudiciaireChange(value: boolean): void {
    this.casierJudiciaireVierge.set(value);
  }

  /**
   * Bandeau verdict :
   * - ELEVEE → navy/info, transition admise + tous critères réunis.
   * - MOYENNE → or/warning, transition possible mais conditions limites.
   * - FAIBLE → rouge alerte, critère bloquant (durée < 2 mois, casier non
   *   vierge, justificatif absent pour transitions exigeantes).
   */
  bannerClass(verdict: VerdictTransition | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'cs-banner cs-banner--info';
      case 'MOYENNE': return 'cs-banner cs-banner--warning';
      case 'FAIBLE': return 'cs-banner cs-banner--critical';
      default: return 'cs-banner cs-banner--info';
    }
  }

  bannerIcon(verdict: VerdictTransition | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'verified';
      case 'MOYENNE': return 'warning';
      case 'FAIBLE': return 'gpp_bad';
      default: return 'info_outline';
    }
  }

  bannerLabel(verdict: VerdictTransition | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'Transition viable';
      case 'MOYENNE': return 'Transition possible — conditions limites';
      case 'FAIBLE': return 'Transition compromise';
      default: return '';
    }
  }

  /** Libellé humain depuis un code titre (export model). */
  titreLabel(code: string | null | undefined): string {
    return titreLabel(code as TitreSejourCode | null);
  }

  /**
   * SF-IM-11-02 : pré-fill depuis `aiData` (ImmigrationExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // titreActuel ← aiData.typeTitreSejourCode (priorité) sinon typeTitreSejour heuristique.
    const iaTitreFromCode = mapTitreSejourFromIa(ai.typeTitreSejourCode ?? null);
    const iaTitreFromText = iaTitreFromCode ?? mapTitreSejourFromIa(ai.typeTitreSejour ?? null);
    if (iaTitreFromText) {
      if (this.titreActuel() === null
          || this.provenanceTitreActuel() === 'IA') {
        this.titreActuel.set(iaTitreFromText);
        this.provenanceTitreActuel.set('IA');
      }
    }

    // SF-IM-11-03 : durée restante ← floor((dateExpirationTitre - aujourd'hui) / 30.44 jours).
    // Plancher à 0 si la date est passée. Pas d'écrasement si l'avocat a saisi manuellement.
    const dateStr = ai.dateExpirationTitre;
    if (dateStr && typeof dateStr === 'string' && dateStr.trim() !== '') {
      const expiration = new Date(dateStr);
      if (Number.isNaN(expiration.getTime())) {
        if (!this.dateExpirationParseWarned) {
          // eslint-disable-next-line no-console
          console.warn(
            `[changement-statut-section] dateExpirationTitre malformée, ignorée : "${dateStr}"`,
          );
          this.dateExpirationParseWarned = true;
        }
      } else {
        const today = new Date();
        const diffMs = expiration.getTime() - today.getTime();
        const moisCalcules = Math.max(
          0,
          Math.floor(diffMs / (1000 * 60 * 60 * 24 * 30.44)),
        );
        if (this.dureeRestanteSurTitreActuelMois() === null
            || this.provenanceDureeRestante() === 'IA') {
          this.dureeRestanteSurTitreActuelMois.set(moisCalcules);
          this.provenanceDureeRestante.set('IA');
        }
      }
    }
  }

  /**
   * Divergence titre actuel — IA détecte un titre (typeTitreSejourCode ou
   * typeTitreSejour) et l'avocat saisit un titre différent.
   * Multi-sources F96 / QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildTitreActuelAlert(): ChangementStatutCoherenceAlert | null {
    const userTitre = this.titreActuel();
    if (!userTitre) return null;

    const builder = CoherenceAlertBuilder.forField<ChangementStatutAlertField>('TITRE_ACTUEL');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM11_TITRE_ACTUEL') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = mapTitreSejourFromIa(ev);
      if (!expected || expected === userTitre) continue;
      builder.addSource('F96', {
        expectedDisplay: this.titreLabel(expected),
        reason: `Checklist procédurale : titre attendu ${this.titreLabel(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM11_TITRE_ACTUEL') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = mapTitreSejourFromIa(ev);
      if (!expected || expected === userTitre) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.titreLabel(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.typeTitreSejourCode` (priorité) ou `typeTitreSejour`.
    const ai = this.aiDataSignal();
    const iaCode = ai?.typeTitreSejourCode ?? null;
    const iaText = ai?.typeTitreSejour ?? null;
    const iaTitre = mapTitreSejourFromIa(iaCode) ?? mapTitreSejourFromIa(iaText);
    if (iaTitre && iaTitre !== userTitre) {
      builder.addSource('IA', {
        expectedDisplay: this.titreLabel(iaTitre),
        reason: `Analyse du dossier : titre détecté "${this.titreLabel(iaTitre)}"`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['IM11_TITRE_ACTUEL', 'TITRE_SEJOUR_ACTUEL']);
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

  alertTooltip(alert: ChangementStatutCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: ChangementStatutCoherenceAlert): string {
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
   * Classe CSS d'un risque selon sa nature. Heuristique :
   * - "bloquant", "irrecevable", "rejet", "ordre public" → rouge (critical).
   * - autre → or (warning).
   */
  riskChipClass(risk: string): string {
    if (!risk) return 'cs-chip-risk cs-chip-risk--warning';
    const r = risk.toLowerCase();
    if (r.includes('bloquant') || r.includes('irrecevable')
        || r.includes('rejet') || r.includes('ordre public')
        || r.includes('refus') || r.includes('casier')) {
      return 'cs-chip-risk cs-chip-risk--critical';
    }
    return 'cs-chip-risk cs-chip-risk--warning';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ChangementStatutRequest = {
      titreActuel: this.titreActuel()!,
      titreEnvisage: this.titreEnvisage()!,
      dureeRestanteSurTitreActuelMois: this.dureeRestanteSurTitreActuelMois()!,
      documentJustificatifFourni: this.documentJustificatifFourni(),
      casierJudiciaireVierge: this.casierJudiciaireVierge(),
    };
    if (this.remunerationVisible()) {
      const remu = this.remunerationContratEur();
      if (remu !== null && remu !== undefined) {
        request.remunerationContratEur = remu;
      }
    }
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Changement de statut analysé', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        // Hydrate le form avec les valeurs persistées (même si on est en mode résultat).
        const titre = mapTitreSejourFromIa(r.titreActuel);
        if (titre) this.titreActuel.set(titre);
        const env = mapTitreSejourFromIa(r.titreEnvisage);
        if (env) this.titreEnvisage.set(env);
        this.dureeRestanteSurTitreActuelMois.set(r.dureeRestanteMois);
        this.documentJustificatifFourni.set(r.documentJustificatifFourni);
        this.remunerationContratEur.set(r.remunerationContratEur);
        this.casierJudiciaireVierge.set(r.casierJudiciaireVierge);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceTitreActuel.set(null);
        this.provenanceDureeRestante.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si jamais calculé — fallback pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}

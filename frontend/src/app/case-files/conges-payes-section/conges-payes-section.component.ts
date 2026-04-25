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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CongesPayesIndemniteService } from '../../core/services/conges-payes-indemnite.service';
import {
  CongesPayesIndemniteRequest,
  CongesPayesIndemniteResponse,
  MethodeCpForcee,
} from '../../core/models/conges-payes-indemnite.model';
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
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-DT-26-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-26 (indemnité compensatrice de congés payés). Seulement les 2 fields
 * alimentés par l'IA travail (`salaireBrutMensuel` → SALAIRE_MENSUEL,
 * `dateLicenciement` → DATE_RUPTURE). Les autres champs (totalRemuneration,
 * joursAcquis, joursPris) ne sont pas extraits par le prompt IA travail
 * actuel — pas d'alerte possible à ce jour.
 */
export type CpAlertField = 'SALAIRE_MENSUEL' | 'SALAIRE_DEDUIT' | 'DATE_RUPTURE';

// SF-155-05 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type CpAlertSource = CoherenceAlertSource;
export type CpCoherenceAlert = CoherenceAlert<CpAlertField>;

/**
 * Seuil d'écart relatif au-delà duquel on affiche une alerte de cohérence
 * entre le salaire mensuel extrait par l'IA et la saisie de l'avocat.
 * Aligné avec le seuil utilisé par F-DT-09, F-DT-11, F-DT-17.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;

/**
 * SF-DT-26-02 : outil décisionnel "Indemnité compensatrice de congés payés"
 * (F-DT-26). France uniquement (le pécule de vacances belge ONSS suit un
 * régime distinct traité dans une feature séparée). Consomme l'API
 * SF-DT-26-01.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-DT-26-conges-payes-indemnite`, règle ALWAYS_ON FR + travail).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Pattern structure form + résultat : `indemnite-precarite-cdd-section`
 *   (SF-DT-17-02) — calque structure form + radio méthode + total amount.
 * - Pattern IA : `immigration-title-decision-section` (F-IM-05) +
 *   `motif-grave-be-section` (SF-DT-27-02).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 *
 * Spécificité : **comparateur 2 méthodes** (méthode du dixième L.3141-24 vs
 * méthode du maintien L.3141-22) affiché côte-à-côte avec badge
 * "Le plus favorable" sur la méthode retenue.
 */
@Component({
  selector: 'app-conges-payes-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatRadioModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './conges-payes-section.component.html',
  styleUrl: './conges-payes-section.component.scss',
})
export class CongesPayesSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04 : inputs IA (tous optionnels — null-safe partout).
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
  result = signal<CongesPayesIndemniteResponse | null>(null);

  // Champs envoyés au backend.
  totalRemunerationPeriodeEur = signal<number | null>(null);
  joursAcquisAnnee = signal<number | null>(null);
  joursPris = signal<number | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  dateRupture = signal<string | null>(null);
  methodeForcee = signal<MethodeCpForcee | null>(null);

  // Provenance IA par champ. 'IA' quand pré-rempli par prefillFromAi() ;
  // remis à null dès que l'avocat modifie manuellement (onXxxChange).
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<CpAlertField, CpCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<CpAlertField, CpCoherenceAlert>> = {};
    const salaireA = this.buildSalaireAlert();
    if (salaireA) alerts.SALAIRE_MENSUEL = salaireA;
    const salaireDeduitNote = this.buildSalaireDeduitNote();
    if (salaireDeduitNote) alerts.SALAIRE_DEDUIT = salaireDeduitNote;
    const dateA = this.buildDateRuptureAlert();
    if (dateA) alerts.DATE_RUPTURE = dateA;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // Indemnité de CP = calcul indemnitaire, pas urgence — jamais de CRITICAL.
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: CongesPayesIndemniteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // Push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le prefill si aiData change, seulement si l'on est encore en
    // mode formulaire ET qu'aucun résultat persisté n'a été chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.workspaceCountry === 'FRANCE' && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-DT-26-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - chaque champ indépendant (prefill partiel OK)
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'FRANCE') return;

    // 1. salaireBrutMensuel → salaireMensuelBrutEur.
    if (typeof ai.salaireBrutMensuel === 'number' && ai.salaireBrutMensuel > 0) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(ai.salaireBrutMensuel);
        this.provenanceSalaire.set('IA');
      }
    }

    // 2. dateLicenciement → dateRupture.
    if (typeof ai.dateLicenciement === 'string' && ai.dateLicenciement.length > 0) {
      if (this.dateRupture() === null || this.provenanceDateRupture() === 'IA') {
        this.dateRupture.set(ai.dateLicenciement);
        this.provenanceDateRupture.set('IA');
      }
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
  explanationFor(field: CpAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE_MENSUEL' ? 'salaire_brut_mensuel'
              : field === 'SALAIRE_DEDUIT' ? 'salaire_brut_mensuel'
              : field === 'DATE_RUPTURE'    ? 'date_licenciement'
              : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: CpCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: CpCoherenceAlert): string {
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
   * Divergence salaire — écart relatif > 10 % entre IA et saisie avocat.
   * Construit via CoherenceAlertBuilder partagé (SF-155-05).
   */
  private buildSalaireAlert(): CpCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;
    const builder = CoherenceAlertBuilder.forField<CpAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
      });
    const salairePiece = this.findPieceManquante(['SALAIRE_BRUT_MENSUEL', 'CP_SALAIRE']);
    if (salairePiece) builder.addPieceManquante(salairePiece);
    return builder.build();
  }

  /**
   * Note non-bloquante (severity INFO) signalant que le salaire brut a été
   * déduit d'un net par l'IA (×1,30) — l'avocat peut vouloir saisir une
   * valeur plus précise si une fiche de paie est disponible. Pattern aligné
   * avec `heures-sup-section` (SALAIRE_DEDUIT).
   */
  private buildSalaireDeduitNote(): CpCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai || ai.salaireEstDeduit !== true) return null;
    const aiSalaire = ai.salaireBrutMensuel;
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    return CoherenceAlertBuilder.forField<CpAlertField>('SALAIRE_DEDUIT')
      .withSeverity('INFO')
      .addSource('IA', {
        expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} € (déduit ×1,30)`,
        reason: `Analyse du dossier : salaire brut déduit d'un net via × 1,30 — fiche de paie recommandée pour précision (méthode du maintien sensible au brut exact).`,
      })
      .build();
  }

  /**
   * Divergence `dateLicenciement` IA vs `dateRupture` saisie.
   * Toute différence est suspecte (rupture du contrat = 1 seule date).
   */
  private buildDateRuptureAlert(): CpCoherenceAlert | null {
    const userDate = this.dateRupture();
    if (!userDate) return null;
    const ai = this.aiDataSignal();
    const aiDate = typeof ai?.dateLicenciement === 'string' ? ai.dateLicenciement : null;
    if (!aiDate || aiDate === userDate) return null;
    const builder = CoherenceAlertBuilder.forField<CpAlertField>('DATE_RUPTURE')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de licenciement ${aiDate}`,
      });
    const piece = this.findPieceManquante(['DATE_LICENCIEMENT', 'CP_DATE_RUPTURE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une PieceManquanteEntry dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null`.
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

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const total = this.totalRemunerationPeriodeEur();
    const acquis = this.joursAcquisAnnee();
    const pris = this.joursPris();
    const salaire = this.salaireMensuelBrutEur();
    const date = this.dateRupture();
    if (total === null || total <= 0) return false;
    if (acquis === null || acquis < 0) return false;
    if (pris === null || pris < 0) return false;
    // UI-side gate : joursPris ne peut pas excéder joursAcquisAnnee.
    if (pris > acquis) return false;
    if (salaire === null || salaire <= 0) return false;
    if (!date) return false;
    if (date > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onTotalRemunerationChange(value: number | null): void {
    this.totalRemunerationPeriodeEur.set(value);
  }

  onJoursAcquisChange(value: number | null): void {
    this.joursAcquisAnnee.set(value);
  }

  onJoursPrisChange(value: number | null): void {
    this.joursPris.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value || null);
    this.provenanceDateRupture.set(null);
  }

  onMethodeForceeChange(value: MethodeCpForcee | null): void {
    this.methodeForcee.set(value);
  }

  /** Helper template : libellé de la méthode (lisible). */
  methodeLabel(m: MethodeCpForcee): string {
    return m === 'DIX_POURCENT' ? 'Méthode du dixième' : 'Méthode du maintien';
  }

  /** Helper template : code court (badge). */
  methodeCode(m: MethodeCpForcee): string {
    return m === 'DIX_POURCENT' ? '10 %' : 'Maintien';
  }

  /**
   * Référence juridique de chaque méthode (pour la carte du comparateur).
   */
  methodeArticle(m: MethodeCpForcee): string {
    return m === 'DIX_POURCENT' ? 'Art. L.3141-24 Code du travail'
                                : 'Art. L.3141-22 Code du travail';
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: CongesPayesIndemniteRequest = {
      totalRemunerationPeriodeEur: this.totalRemunerationPeriodeEur()!,
      joursAcquisAnnee: this.joursAcquisAnnee()!,
      joursPris: this.joursPris()!,
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      dateRupture: this.dateRupture()!,
      methodeForcee: this.methodeForcee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité de congés payés calculée', 'OK', { duration: 2500 });
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
        this.totalRemunerationPeriodeEur.set(r.totalRemunerationPeriodeEur);
        this.joursAcquisAnnee.set(r.joursAcquisAnnee);
        this.joursPris.set(r.joursPris);
        this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
        this.dateRupture.set(r.dateRupture);
        this.methodeForcee.set(r.methodeForcee);
        // Valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceSalaire.set(null);
        this.provenanceDateRupture.set(null);
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

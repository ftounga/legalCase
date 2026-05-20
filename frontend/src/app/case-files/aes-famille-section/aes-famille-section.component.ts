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
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AesFamilleService } from '../../core/services/aes-famille.service';
import {
  AesFamilleRequest,
  AesFamilleResponse,
} from '../../core/models/aes-famille.model';
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
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AesFamillePrefillRules } from './aes-famille-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-IM-09-06 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-IM-09 AES voie familiale (L.435-1).
 */
export type AesFamilleAlertField = 'DATE_ENTREE_FRANCE' | 'DUREE_PRESENCE';

export type AesFamilleAlertSource = CoherenceAlertSource;
export type AesFamilleCoherenceAlert = CoherenceAlert<AesFamilleAlertField>;

/** Seuil divergence durée de présence (mois). Au-delà → alerte cohérence. */
const DUREE_PRESENCE_DIVERGENCE_MOIS = 6;

/**
 * SF-IM-09-06 : outil décisionnel AES voie familiale (L.435-1 CESEDA +
 * circulaire Valls 28/11/2012). Single-country FRANCE — bannière info pour BE.
 *
 * Pattern canonique : `harcelement-licenciement-nul-section` (F-DT-11-02) +
 * pré-fill IA / cohérence F-IA-03 (`immigration-title-decision-section`,
 * F-IM-05-04).
 */
@Component({
  selector: 'app-aes-famille-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatChipsModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './aes-famille-section.component.html',
  styleUrl: './aes-famille-section.component.scss',
})
export class AesFamilleSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AES — VOIE FAMILIALE (L.435-1)';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return AesFamillePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-09-aes-famille/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AesFamilleResponse | null>(null);

  // Form signals
  dateEntreeFrance = signal<string | null>(null);
  dureePresenceMois = signal<number | null>(null);
  conjointFrancaisOuRegulier = signal(false);
  enfantsScolarisesFrance = signal<number>(0);
  dureeScolaritePlusAncienEnfantAnnees = signal<number>(0);
  preuvesInsertion = signal(false);
  menaceOrdrePublic = signal(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA (badges) — SF-246-18 : 4 champs pré-remplissables.
  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDureePresence = signal<'IA' | null>(null);
  provenanceDureeScolarite = signal<'IA' | null>(null);
  provenanceDateDepotDemande = signal<'IA' | null>(null);

  // Source explanations (F-IA-03-15c)
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // Alertes de cohérence (F-IA-03)
  coherenceAlerts = computed<Partial<Record<AesFamilleAlertField, AesFamilleCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AesFamilleAlertField, AesFamilleCoherenceAlert>> = {};
    const dateAlert = this.buildDateEntreeAlert();
    if (dateAlert) alerts.DATE_ENTREE_FRANCE = dateAlert;
    const dureeAlert = this.buildDureePresenceAlert();
    if (dureeAlert) alerts.DUREE_PRESENCE = dureeAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: AesFamilleService,
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
    if (this.workspaceCountry === 'FRANCE') {
      if (this.standaloneMode) {

        // F-163 SF-163-02d : pas de dossier à interroger en standalone.

        this.collapsed.set(false);

        this.loading.set(false);

        this.showForm.set(true);

        return;

      }
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'FRANCE') {
      this.prefillFromAi();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.applyResultToForm(r);
        this.provenanceDateEntree.set(null);
        this.provenanceDureePresence.set(null);
        this.provenanceDureeScolarite.set(null);
        this.provenanceDateDepotDemande.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * Pré-remplit le form depuis aiData. Champs ImmigrationExtractedData typés
   * SF-246-18 : 4 champs pré-remplis depuis ImmigrationExtractedData typé.
   * No-op si aiData null ou champs manquants.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'FRANCE') return;
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };
    const date = AesFamillePrefillRules.computeDateEntreeFrance(input);
    if (date !== null
        && (this.dateEntreeFrance() === null || this.provenanceDateEntree() === 'IA')) {
      this.dateEntreeFrance.set(date);
      this.provenanceDateEntree.set('IA');

      if (this.dureePresenceMois() === null || this.provenanceDureePresence() === 'IA') {
        const months = AesFamillePrefillRules.computeDureePresenceMois(input);
        if (months !== null) {
          this.dureePresenceMois.set(months);
          this.provenanceDureePresence.set('IA');
        }
      }
    }
    // SF-246-18 : durée scolarité enfant le plus ancien.
    const dureeScol = AesFamillePrefillRules.computeDureeScolaritePlusAncienEnfant(input);
    if (dureeScol !== null
        && (this.dureeScolaritePlusAncienEnfantAnnees() === 0 || this.provenanceDureeScolarite() === 'IA')) {
      this.dureeScolaritePlusAncienEnfantAnnees.set(dureeScol);
      this.provenanceDureeScolarite.set('IA');
    }
    // SF-246-18 : date de dépôt de la demande.
    const depot = AesFamillePrefillRules.computeDateDepotDemande(input);
    if (depot !== null && !this.dateDepotDemande()) {
      this.dateDepotDemande.set(depot);
      this.provenanceDateDepotDemande.set('IA');
    }
  }

  private computeMonthsSince(isoDate: string): number | null {
    const d = new Date(isoDate);
    if (isNaN(d.getTime())) return null;
    const now = new Date();
    const months = (now.getFullYear() - d.getFullYear()) * 12
                 + (now.getMonth() - d.getMonth());
    return Math.max(0, months);
  }

  /**
   * Mapping field → sourceKey pour le popover F-IA-03-15c.
   */
  explanationFor(field: AesFamilleAlertField): SourceExplanation[] {
    const key = field === 'DATE_ENTREE_FRANCE' ? 'date_entree_france' : 'duree_presence_mois';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: AesFamilleCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AesFamilleCoherenceAlert): string {
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

  private buildDateEntreeAlert(): AesFamilleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    // SF-246-18 : champ typé aesDateEntreeFrance.
    const aiDate = ai.aesDateEntreeFrance;
    if (typeof aiDate !== 'string' || aiDate.length < 10) return null;
    const user = this.dateEntreeFrance();
    if (!user) return null;
    const aiPart = aiDate.substring(0, 10);
    if (aiPart === user) return null;
    return CoherenceAlertBuilder.forField<AesFamilleAlertField>('DATE_ENTREE_FRANCE')
      .addSource('IA', {
        expectedDisplay: aiPart,
        reason: `Analyse du dossier : date d'entrée en France ${aiPart}`,
      })
      .build();
  }

  private buildDureePresenceAlert(): AesFamilleCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (!ai) return null;
    // SF-246-18 : champ typé aesDateEntreeFrance.
    const aiDate = ai.aesDateEntreeFrance;
    if (typeof aiDate !== 'string' || aiDate.length < 10) return null;
    const user = this.dureePresenceMois();
    if (typeof user !== 'number' || user < 0) return null;
    const aiMonths = this.computeMonthsSince(aiDate.substring(0, 10));
    if (aiMonths === null) return null;
    if (Math.abs(aiMonths - user) <= DUREE_PRESENCE_DIVERGENCE_MOIS) return null;
    return CoherenceAlertBuilder.forField<AesFamilleAlertField>('DUREE_PRESENCE')
      .addSource('IA', {
        expectedDisplay: `${aiMonths} mois`,
        reason: `Analyse du dossier : présence ~${aiMonths} mois (date d'entrée ${aiDate.substring(0, 10)})`,
      })
      .build();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const date = this.dateEntreeFrance();
    const mois = this.dureePresenceMois();
    return !!date && typeof mois === 'number' && mois >= 0
      && this.enfantsScolarisesFrance() >= 0
      && this.dureeScolaritePlusAncienEnfantAnnees() >= 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onDateEntreeChange(value: string | null): void {
    this.dateEntreeFrance.set(value);
    this.provenanceDateEntree.set(null);
  }

  onDureePresenceChange(value: number | null): void {
    this.dureePresenceMois.set(value);
    this.provenanceDureePresence.set(null);
  }

  onEnfantsScolarisesChange(value: number | null): void {
    this.enfantsScolarisesFrance.set(value ?? 0);
  }

  onDureeScolariteChange(value: number | null): void {
    this.dureeScolaritePlusAncienEnfantAnnees.set(value ?? 0);
    this.provenanceDureeScolarite.set(null);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotDemande.set(value && value.length >= 10 ? value : null);
    this.provenanceDateDepotDemande.set(null);
  }

  /**
   * Verdict bannière classe CSS pour palette navy/or/danger classique
   * (pas de rouge dominant — pas d'urgence < 72h).
   */
  verdictBannerClass(): string {
    const v = this.result()?.verdictProbabiliteAcceptation;
    if (v === 'ELEVEE') return 'aes-verdict-banner aes-verdict-banner--success';
    if (v === 'MOYENNE') return 'aes-verdict-banner aes-verdict-banner--info';
    return 'aes-verdict-banner aes-verdict-banner--danger';
  }

  verdictIcon(): string {
    const v = this.result()?.verdictProbabiliteAcceptation;
    if (v === 'ELEVEE') return 'check_circle';
    if (v === 'MOYENNE') return 'info_outline';
    return 'error';
  }

  verdictLabel(): string {
    const v = this.result()?.verdictProbabiliteAcceptation;
    if (v === 'ELEVEE') return 'Probabilité élevée';
    if (v === 'MOYENNE') return 'Probabilité moyenne';
    return 'Probabilité faible';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'FRANCE') return;
    const request: AesFamilleRequest = {
      dateEntreeFrance: this.dateEntreeFrance()!,
      dureePresenceMois: this.dureePresenceMois()!,
      conjointFrancaisOuRegulier: this.conjointFrancaisOuRegulier(),
      enfantsScolarisesFrance: this.enfantsScolarisesFrance(),
      dureeScolaritePlusAncienEnfantAnnees: this.dureeScolaritePlusAncienEnfantAnnees(),
      preuvesInsertion: this.preuvesInsertion(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      dateDepotDemande: this.dateDepotDemande(),
    };
    this.calculating.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse AES famille calculée', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private applyResultToForm(r: AesFamilleResponse): void {
    this.dateEntreeFrance.set(r.dateEntreeFrance);
    this.dureePresenceMois.set(r.dureePresenceMois);
    this.conjointFrancaisOuRegulier.set(r.conjointFrancaisOuRegulier);
    this.enfantsScolarisesFrance.set(r.enfantsScolarisesFrance);
    this.dureeScolaritePlusAncienEnfantAnnees.set(r.dureeScolaritePlusAncienEnfantAnnees);
    this.preuvesInsertion.set(r.preuvesInsertion);
    this.menaceOrdrePublic.set(r.menaceOrdrePublic);
    this.dateDepotDemande.set(r.dateDepotDemande);
  }
}

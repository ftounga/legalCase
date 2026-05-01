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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TravailProcedureService } from '../../core/services/travail-procedure.service';
import {
  TravailProcedureCode,
  TravailProcedureJalon,
  TravailProcedureOption,
  TRAVAIL_PROCEDURE_CODES,
  TRAVAIL_PROCEDURE_OPTIONS_BE,
  TRAVAIL_PROCEDURE_OPTIONS_FR,
} from '../../core/models/travail-procedure.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/** SF-136-02 : champs d'alerte de cohérence F-IA-03. */
export type TravailProcedureAlertField = 'TYPE_PROCEDURE';
export type TravailProcedureCoherenceAlert = CoherenceAlert<TravailProcedureAlertField>;

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-136-02 : outil décisionnel "Calendrier procédural travail"
 * (F-136). Couvre FR (prud'hommes / appel CA sociale / cassation sociale)
 * et BE (tribunal du travail / cour du travail / cassation BE). Consomme
 * l'endpoint `GET /api/v1/case-files/{caseFileId}/travail-procedure-jalons`
 * (SF-136-02 backend) qui lit `LegalReferentialService.getTravailProcedureJalons`
 * seedé par la SF-136-01.
 *
 * <p>Comportement clé :
 * <ul>
 *   <li>Gate `workspaceCountry` : la liste des codes affichés correspond au
 *       pays du workspace (FR ou BE). Pas de masquage silencieux pour
 *       l'autre pays — bannière info qui explique la restriction.</li>
 *   <li>Pré-fill IA : si {@code aiData.procedureTravailDetectee} est défini
 *       et valide, sélectionne le code automatiquement et affiche le badge
 *       "Pré-rempli depuis l'analyse" (`auto_awesome`). Le champ n'existe
 *       pas encore dans `TravailExtractedData` — pré-fill = no-op gracieux,
 *       branchera proprement dès que le pipeline IA SF future le détectera.</li>
 *   <li>Pas de persistance — chaque clic refait le calcul backend (stateless).</li>
 *   <li>Convention F-155 SF-155-07 (DIV-9) : datepicker `<input type="date">`
 *       natif, pas MatDatepicker.</li>
 * </ul>
 *
 * <p>Palette navy + or (DESIGN_SYSTEM) — pas de rouge dominant. Police
 * JetBrains Mono pour `articleRef`.
 */
@Component({
  selector: 'app-travail-procedure-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule, MatChipsModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './travail-procedure-section.component.html',
  styleUrl: './travail-procedure-section.component.scss',
})
export class TravailProcedureSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CALENDRIER PROCÉDURAL — DROIT DU TRAVAIL';
  static readonly TOOL_ICON = 'event';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;

  // Signals miroirs des inputs IA pour les `computed`.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  // Signal miroir du workspace country pour réactivité des computed.
  private workspaceCountrySignal = signal<'FRANCE' | 'BELGIQUE'>('FRANCE');

  collapsed = signal(true);
  loading = signal(false);
  jalons = signal<TravailProcedureJalon[]>([]);
  /** True dès qu'un appel backend a abouti (succès ou liste vide). */
  hasResult = signal<boolean>(false);

  typeProcedure = signal<TravailProcedureCode | null>(null);
  dateDeclencheur = signal<string | null>(null);

  /** SF-136-02 : provenance IA pour le badge `auto_awesome`. */
  provenanceTypeProcedure = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountrySignal() === 'FRANCE');

  /** Options visibles selon le pays du workspace. */
  options = computed<ReadonlyArray<TravailProcedureOption>>(() =>
    this.isFrance() ? TRAVAIL_PROCEDURE_OPTIONS_FR : TRAVAIL_PROCEDURE_OPTIONS_BE
  );

  /**
   * Bannière info displayed when the workspace country has no incoming
   * mismatch — but always visible to surface the gate visually so the
   * lawyer understands why only 3 codes are shown.
   */
  countryBannerLabel = computed<string>(() =>
    this.isFrance()
      ? "Cet outil présente uniquement les procédures de droit du travail françaises (prud'hommes, appel social, cassation sociale)."
      : "Cet outil présente uniquement les procédures de droit du travail belges (tribunal du travail, cour du travail, cassation)."
  );

  /** Alertes de cohérence — popover sur le select. */
  coherenceAlerts = computed<Partial<Record<TravailProcedureAlertField, TravailProcedureCoherenceAlert>>>(() => {
    const alerts: Partial<Record<TravailProcedureAlertField, TravailProcedureCoherenceAlert>> = {};
    const a = this.buildTypeProcedureAlert();
    if (a) alerts.TYPE_PROCEDURE = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: TravailProcedureService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    this.workspaceCountrySignal.set(this.workspaceCountry);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    // Pré-fill IA — no-op gracieux si le champ n'existe pas dans aiData.
    this.prefillFromAi();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['workspaceCountry']) {
      this.workspaceCountrySignal.set(this.workspaceCountry);
    }
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      // Re-tente le pré-fill si aiData arrive après le mount sans saisie
      // utilisateur antérieure.
      if (!this.typeProcedure()) {
        this.prefillFromAi();
      }
    }
    if (changes['procedureChecks']) {
      this.procedureChecksSignal.set(this.procedureChecks ?? []);
    }
    if (changes['aiQuestions']) {
      this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const t = this.typeProcedure();
    if (!t) return false;
    const d = this.dateDeclencheur();
    if (d && !ISO_DATE_RE.test(d)) return false;
    return true;
  }

  /** Modification manuelle du type — efface la provenance IA. */
  onTypeProcedureChange(value: TravailProcedureCode | null): void {
    this.typeProcedure.set(value);
    this.provenanceTypeProcedure.set(null);
    // On vide les jalons précédents pour éviter de mélanger avec un autre type.
    this.jalons.set([]);
    this.hasResult.set(false);
  }

  onDateDeclencheurChange(value: string | null): void {
    this.dateDeclencheur.set(value || null);
  }

  /**
   * Lance le calcul du calendrier — appelle le backend et met à jour la
   * liste de jalons. Affiche une snackbar en cas d'erreur HTTP.
   */
  calculate(): void {
    if (!this.formValid()) return;
    this.loading.set(true);
    this.service.getJalons(this.caseFileId, this.typeProcedure()!, this.dateDeclencheur())
      .subscribe({
        next: (list) => {
          this.jalons.set(list);
          this.hasResult.set(true);
          this.loading.set(false);
          this.dashboardRefresh?.triggerRefresh();
        },
        error: (err) => {
          this.loading.set(false);
          this.hasResult.set(false);
          const msg = err?.error?.message || err?.error || 'Erreur lors de la récupération du calendrier';
          this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        },
      });
  }

  /** Libellé d'un jalon offset (ex. "+45 jours"). */
  offsetLabel(j: TravailProcedureJalon): string {
    return `+${j.offsetDays} ${j.offsetDays > 1 ? 'jours' : 'jour'}`;
  }

  /** Tooltip du badge d'alerte (template). */
  alertBadgeLabel(alert: TravailProcedureCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque procédural'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /**
   * SF-136-02 : pré-remplissage depuis l'analyse IA. Le champ
   * `procedureTravailDetectee` (et `dateDeclencheurProcedure`) n'existent
   * pas encore dans `TravailExtractedData` — l'accès via cast est
   * volontaire. No-op gracieux si non présent.
   */
  private prefillFromAi(): void {
    const ai = this.aiData as (TravailExtractedData & {
      procedureTravailDetectee?: string | null;
      dateDeclencheurProcedure?: string | null;
    }) | null | undefined;
    if (!ai) return;

    const detected = ai.procedureTravailDetectee;
    if (typeof detected === 'string'
        && TRAVAIL_PROCEDURE_CODES.has(detected as TravailProcedureCode)) {
      // Gate pays : on n'applique que si le code est cohérent avec le workspace.
      const isFrCode = detected.endsWith('_FR');
      if ((this.isFrance() && isFrCode) || (!this.isFrance() && !isFrCode)) {
        if (!this.typeProcedure()) {
          this.typeProcedure.set(detected as TravailProcedureCode);
          this.provenanceTypeProcedure.set('IA');
        }
      }
    }

    const date = ai.dateDeclencheurProcedure;
    if (typeof date === 'string' && ISO_DATE_RE.test(date)) {
      if (!this.dateDeclencheur()) {
        this.dateDeclencheur.set(date);
      }
    }
  }

  private buildTypeProcedureAlert(): TravailProcedureCoherenceAlert | null {
    const user = this.typeProcedure();
    if (!user) return null;
    const builder = CoherenceAlertBuilder
      .forField<TravailProcedureAlertField>('TYPE_PROCEDURE')
      .withSeverity('WARNING');

    // 1. ProcedureCheck NON_COMPLIANT sur 'TRAVAIL_PROCEDURE_TYPE'.
    let added = false;
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'TRAVAIL_PROCEDURE_TYPE') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !TRAVAIL_PROCEDURE_CODES.has(ev as TravailProcedureCode)) continue;
      if (ev === user) continue;
      const label = this.findOptionLabel(ev as TravailProcedureCode) ?? ev;
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : type attendu ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      added = true;
      break;
    }

    // 2. Question IA — réponse non-vide divergente sur 'TRAVAIL_PROCEDURE_TYPE'.
    if (!added) {
      for (const q of this.aiQuestionsSignal()) {
        if (q.critereCode?.toUpperCase() !== 'TRAVAIL_PROCEDURE_TYPE') continue;
        const answer = q.answerText?.trim().toUpperCase();
        if (!answer) continue;
        if (!TRAVAIL_PROCEDURE_CODES.has(answer as TravailProcedureCode)) continue;
        if (answer === user) continue;
        const label = this.findOptionLabel(answer as TravailProcedureCode) ?? answer;
        builder.addSource('QUESTION_IA', {
          expectedDisplay: label,
          reason: `Question complémentaire : "${q.questionText}" → ${label}`,
        });
        added = true;
        break;
      }
    }

    return added ? builder.build() : null;
  }

  private findOptionLabel(code: TravailProcedureCode): string | null {
    const all = [...TRAVAIL_PROCEDURE_OPTIONS_FR, ...TRAVAIL_PROCEDURE_OPTIONS_BE];
    return all.find(o => o.code === code)?.label ?? null;
  }
}

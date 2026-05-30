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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AppelCphService } from '../../core/services/appel-cph.service';
import {
  AppelCphRequest,
  AppelCphResponse,
  ModeNotification,
  PartieAppelante,
  RepresentationConstituee,
  VerdictAppelCph,
} from '../../core/models/appel-cph.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AppelCphPrefillRules } from './appel-cph-section-prefill-rules';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/** Field unique audité par la validation F-IA-03 de cet outil. */
export type AppelCphAlertField = 'DATE_NOTIFICATION';

export type AppelCphCoherenceAlert = CoherenceAlert<AppelCphAlertField>;

/**
 * SF-218-02 — Outil décisionnel « Appel CPH devant la Cour d'appel »
 * (F-DT-86-appel-cph-cour-appel).
 *
 * FRANCE uniquement — appel d'un jugement du conseil de prud'hommes devant la
 * chambre sociale de la cour d'appel. Calculateur du délai d'appel d'1 mois
 * (art. 538 CPC ; R. 1461-1 et s. du Code du travail) + verdict de recevabilité
 * (DELAI_OUVERT / DELAI_URGENT / DELAI_EXPIRE / VOIE_FERMEE) + checklist des
 * formalités d'appel social. Si le jugement a été rendu en premier et dernier
 * ressort, la voie de l'appel est fermée (VOIE_FERMEE) et seul le pourvoi en
 * cassation (F-DT-87) est ouvert.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or (rouge réservé DELAI_EXPIRE)
 *  - pré-fill IA (dateNotificationJugement depuis TravailExtractedData) + badge
 *    `auto_awesome` + handler `onDateNotificationChange()`
 *  - validation F-IA-03 via `CoherenceAlertBuilder` partagé + popover trigger
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-appel-cph-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './appel-cph-section.component.html',
  styleUrl: './appel-cph-section.component.scss',
})
export class AppelCphSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-86-appel-cph-cour-appel';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = "APPEL CPH — COUR D'APPEL (FR)";
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return AppelCphPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<AppelCphResponse | null>(null);

  dateNotificationJugement = signal<string | null>(null);
  partieAppelante = signal<PartieAppelante>('SALARIE');
  modeNotification = signal<ModeNotification>('SIGNIFICATION');
  representationConstituee = signal<RepresentationConstituee>('AVOCAT');
  jugementEnDernierRessort = signal<boolean>(false);

  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  provenanceDateNotification = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Validation F-IA-03 — alerte si la date de notification saisie diverge
   * d'au moins 15 jours de celle détectée par l'IA (ou confirmée par une
   * checklist procédurale F-96). Pattern canonique `anciennete-section`.
   */
  coherenceAlerts = computed<Partial<Record<AppelCphAlertField, AppelCphCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const ai = this.aiDataSignal();
    const saisie = this.dateNotificationJugement();
    if (!saisie) return {};
    const alerts: Partial<Record<AppelCphAlertField, AppelCphCoherenceAlert>> = {};

    const builder = CoherenceAlertBuilder.forField<AppelCphAlertField>('DATE_NOTIFICATION');
    let hasSource = false;

    // Source IA : date de notification du jugement extraite par le pipeline.
    if (ai?.dateNotificationJugement) {
      const diff = dateDaysDiff(ai.dateNotificationJugement, saisie);
      if (diff !== null && diff >= 15) {
        builder.addSource('IA', {
          expectedDisplay: ai.dateNotificationJugement,
          reason: `Analyse du dossier : date de notification ${ai.dateNotificationJugement}`,
        });
        hasSource = true;
      }
    }

    // Source F-96 : une vérification procédurale confirme une date de
    // notification divergente (croisement checklist F-96).
    const f96 = this.f96NotificationDate();
    if (f96) {
      const diff = dateDaysDiff(f96, saisie);
      if (diff !== null && diff >= 15) {
        builder.addSource('F96', {
          expectedDisplay: f96,
          reason: `Checklist procédurale : notification ${f96}`,
        });
        hasSource = true;
      }
    }

    if (hasSource) {
      const a = builder.build();
      if (a) alerts.DATE_NOTIFICATION = a;
    }
    return alerts;
  });

  alertsSummary = computed(() => ({ total: Object.keys(this.coherenceAlerts()).length }));

  /** Reason fallback affichée dans le popover si aucune explication enrichie. */
  reasonFor(field: AppelCphAlertField): string {
    const alert = this.coherenceAlerts()[field];
    if (!alert) return '';
    return `La date de notification attendue est ${alert.expectedDisplay}.`;
  }

  constructor(
    private readonly service: AppelCphService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isFrance() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      if (this.isFrance() && this.showForm() && !this.result()) {
        this.prefillFromAi();
      }
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    return !!this.dateNotificationJugement();
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationJugement.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onPartieAppelanteChange(value: PartieAppelante): void {
    this.partieAppelante.set(value);
  }

  onModeNotificationChange(value: ModeNotification): void {
    this.modeNotification.set(value);
  }

  onRepresentationChange(value: RepresentationConstituee): void {
    this.representationConstituee.set(value);
  }

  onJugementEnDernierRessortChange(value: boolean): void {
    this.jugementEnDernierRessort.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AppelCphRequest = {
      dateNotificationJugement: this.dateNotificationJugement()!,
      partieAppelante: this.partieAppelante(),
      modeNotification: this.modeNotification(),
      representationConstituee: this.representationConstituee(),
      jugementEnDernierRessort: this.jugementEnDernierRessort(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse appel CPH enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.dashboardRefresh?.triggerRefresh();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
        this.cdr.markForCheck();
      },
    });
  }

  verdictLabel(v: VerdictAppelCph | null | undefined): string {
    switch (v) {
      case 'DELAI_OUVERT': return 'Délai ouvert';
      case 'DELAI_URGENT': return 'Délai urgent';
      case 'DELAI_EXPIRE': return 'Délai expiré';
      case 'VOIE_FERMEE':  return 'Voie fermée';
      default:             return '';
    }
  }

  verdictClass(v: VerdictAppelCph | null | undefined): string {
    switch (v) {
      case 'DELAI_OUVERT': return 'acc-banner acc-banner--success';
      case 'DELAI_URGENT': return 'acc-banner acc-banner--warning';
      case 'DELAI_EXPIRE': return 'acc-banner acc-banner--danger';
      case 'VOIE_FERMEE':  return 'acc-banner acc-banner--navy';
      default:             return 'acc-banner';
    }
  }

  verdictIcon(v: VerdictAppelCph | null | undefined): string {
    switch (v) {
      case 'DELAI_OUVERT': return 'check_circle';
      case 'DELAI_URGENT': return 'hourglass_top';
      case 'DELAI_EXPIRE': return 'error';
      case 'VOIE_FERMEE':  return 'block';
      default:             return 'info_outline';
    }
  }

  /** Item bloquant mis en avant si aucune représentation n'est constituée. */
  itemBloquant(item: { obligatoire: boolean }): boolean {
    return item.obligatoire && this.result()?.representationConstituee === 'AUCUNE';
  }

  private f96NotificationDate(): string | null {
    const checks = this.procedureChecks;
    if (!checks || checks.length === 0) return null;
    const ISO = /\b(\d{4}-\d{2}-\d{2})\b/;
    for (const c of checks) {
      const haystack = `${c.description ?? ''} ${c.raison ?? ''} ${c.expectedValue ?? ''}`;
      const lower = haystack.toLowerCase();
      if (lower.includes('notification') && (lower.includes('jugement') || lower.includes('prud'))) {
        // Priorité à expectedValue (valeur canonique F-96), sinon scan global.
        if (c.expectedValue) {
          const me = ISO.exec(c.expectedValue);
          if (me) return me[1];
        }
        const m = ISO.exec(haystack);
        if (m) return m[1];
      }
    }
    return null;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };
    const date = AppelCphPrefillRules.computeDateNotificationJugement(input);
    if (date !== null && this.dateNotificationJugement() === null) {
      this.dateNotificationJugement.set(date);
      this.provenanceDateNotification.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationJugement.set(r.dateNotificationJugement ?? null);
        this.partieAppelante.set(r.partieAppelante);
        this.modeNotification.set(r.modeNotification);
        this.representationConstituee.set(r.representationConstituee);
        this.jugementEnDernierRessort.set(r.jugementEnDernierRessort);
        this.provenanceDateNotification.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 = pas encore d'analyse, on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}

function dateDaysDiff(a: string, b: string): number | null {
  const ta = Date.parse(a);
  const tb = Date.parse(b);
  if (Number.isNaN(ta) || Number.isNaN(tb)) return null;
  return Math.abs(ta - tb) / 86400000;
}

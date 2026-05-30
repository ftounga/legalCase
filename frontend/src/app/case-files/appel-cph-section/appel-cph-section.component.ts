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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AppelCphService } from '../../core/services/appel-cph.service';
import {
  AppelCphModeNotification,
  AppelCphPartieAppelante,
  AppelCphRepresentation,
  AppelCphRequest,
  AppelCphResponse,
  AppelCphVerdict,
} from '../../core/models/appel-cph.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AppelCphPrefillRules } from './appel-cph-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-02 — Outil décisionnel « Appel CPH cour d'appel » (F-DT-86).
 *
 * FRANCE uniquement — appel d'un jugement du Conseil de prud'hommes (CPH)
 * devant la chambre sociale de la Cour d'appel (art. 538 CPC ; R. 1461-1 CPC).
 * Le délai d'appel est d'un mois à compter de la notification du jugement.
 * Calculateur de délai (verdict 4 états : DELAI_OUVERT vert / DELAI_URGENT
 * orange / DELAI_EXPIRE rouge / VOIE_FERMEE gris) + checklist des formalités
 * de l'appel social + renvoi vers le pourvoi en cassation (F-DT-87) lorsque la
 * voie d'appel est fermée (jugement en dernier ressort, R. 1462-1 CPC).
 * Pattern miroir : {@link AppelCaaCassationSectionComponent} (F-IM-41).
 *
 * <p>Complète le backend SF-218-01 (PR #1508, déjà mergé sur master). C'est
 * l'entrée TOOL_REGISTRY de ce composant qui répare `DecisionToolVisibilityIntegrityIT`
 * (le seed `decision_tool_visibility_rules` référençait un tool_id sans pendant
 * frontend → master rouge).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert DELAI_OUVERT, orange DELAI_URGENT,
 *    rouge DELAI_EXPIRE, gris VOIE_FERMEE
 *  - pré-fill IA (date de notification du jugement CPH) depuis TravailExtractedData
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si verdict DELAI_OUVERT ou DELAI_URGENT, une échéance
 * `dateLimiteAppel` est créée via {@link CaseDeadlineService} avec le label
 * « Appel CPH cour d'appel » (best-effort, n'interrompt pas le flux).
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
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './appel-cph-section.component.html',
  styleUrl: './appel-cph-section.component.scss',
})
export class AppelCphSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-86-appel-cph-cour-appel';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = "APPEL CPH COUR D'APPEL (FR)";
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return AppelCphPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<AppelCphResponse | null>(null);
  deadlineCreated = signal(false);

  dateNotificationJugement = signal<string | null>(null);
  partieAppelante = signal<AppelCphPartieAppelante>('SALARIE');
  modeNotification = signal<AppelCphModeNotification>('SIGNIFICATION');
  representationConstituee = signal<AppelCphRepresentation>('AVOCAT');
  jugementEnDernierRessort = signal<boolean>(false);

  provenanceDateNotification = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: AppelCphService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
    @Optional() private readonly deadlineService?: CaseDeadlineService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
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
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    if (!this.dateNotificationJugement()) return false;
    return true;
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationJugement.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onPartieAppelanteChange(value: AppelCphPartieAppelante): void {
    this.partieAppelante.set(value);
  }

  onModeNotificationChange(value: AppelCphModeNotification): void {
    this.modeNotification.set(value);
  }

  onRepresentationChange(value: AppelCphRepresentation): void {
    this.representationConstituee.set(value);
  }

  onDernierRessortChange(value: boolean): void {
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
        this.snackBar.open('Analyse appel CPH cour d\'appel enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.maybeCreateDeadline(r);
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

  /**
   * Bridge échéance F-69 — si le verdict laisse le délai d'appel ouvert
   * (DELAI_OUVERT ou DELAI_URGENT), on alimente `app-case-deadlines-section`
   * avec l'échéance `dateLimiteAppel`. Best-effort : un échec n'interrompt pas
   * le flux (le dashboard refresh affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: AppelCphResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.verdict !== 'DELAI_OUVERT' && r.verdict !== 'DELAI_URGENT') return;
    if (!r.dateLimiteAppel) return;
    this.deadlineService.create(this.caseFileId, 'Appel CPH cour d\'appel', r.dateLimiteAppel).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  verdictLabel(verdict: AppelCphVerdict | null | undefined): string {
    switch (verdict) {
      case 'DELAI_OUVERT': return "Délai d'appel ouvert";
      case 'DELAI_URGENT': return "Délai d'appel urgent";
      case 'DELAI_EXPIRE': return "Délai d'appel expiré";
      case 'VOIE_FERMEE':  return "Voie d'appel fermée";
      default:             return '';
    }
  }

  partieAppelanteLabel(partie: AppelCphPartieAppelante | null | undefined): string {
    switch (partie) {
      case 'SALARIE':  return 'Salarié';
      case 'EMPLOYEUR': return 'Employeur';
      default:          return '';
    }
  }

  modeNotificationLabel(mode: AppelCphModeNotification | null | undefined): string {
    switch (mode) {
      case 'SIGNIFICATION': return 'Signification (commissaire de justice)';
      case 'LRAR':          return 'Notification LRAR (greffe)';
      default:              return '';
    }
  }

  representationLabel(rep: AppelCphRepresentation | null | undefined): string {
    switch (rep) {
      case 'AVOCAT':            return 'Avocat';
      case 'DEFENSEUR_SYNDICAL': return 'Défenseur syndical';
      case 'AUCUNE':            return 'Aucune représentation constituée';
      default:                  return '';
    }
  }

  bannerClass(verdict: AppelCphVerdict | null | undefined): string {
    switch (verdict) {
      case 'DELAI_OUVERT': return 'acph-banner acph-banner--success';
      case 'DELAI_URGENT': return 'acph-banner acph-banner--warning';
      case 'DELAI_EXPIRE': return 'acph-banner acph-banner--danger';
      case 'VOIE_FERMEE':  return 'acph-banner acph-banner--neutral';
      default:             return 'acph-banner';
    }
  }

  bannerIcon(verdict: AppelCphVerdict | null | undefined): string {
    switch (verdict) {
      case 'DELAI_OUVERT': return 'check_circle';
      case 'DELAI_URGENT': return 'warning';
      case 'DELAI_EXPIRE': return 'error';
      case 'VOIE_FERMEE':  return 'block';
      default:             return 'info_outline';
    }
  }

  /** Le compteur de jours n'a de sens que tant que la voie d'appel est ouverte. */
  showJoursRestants(verdict: AppelCphVerdict | null | undefined): boolean {
    return verdict === 'DELAI_OUVERT' || verdict === 'DELAI_URGENT' || verdict === 'DELAI_EXPIRE';
  }

  /** Lien croisé pourvoi F-DT-87 : voie fermée OU renvoi explicite renvoyé par le backend. */
  showPourvoiLink(r: AppelCphResponse | null | undefined): boolean {
    if (!r) return false;
    return r.verdict === 'VOIE_FERMEE' || !!r.renvoiPourvoiCassation || r.jugementEnDernierRessort;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNotif = AppelCphPrefillRules.computeDateNotificationJugement(input);
    if (dateNotif !== null && this.dateNotificationJugement() === null) {
      this.dateNotificationJugement.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationJugement.set(r.dateNotificationJugement ?? null);
        this.partieAppelante.set(r.partieAppelante ?? 'SALARIE');
        this.modeNotification.set(r.modeNotification ?? 'SIGNIFICATION');
        this.representationConstituee.set(r.representationConstituee ?? 'AVOCAT');
        this.jugementEnDernierRessort.set(r.jugementEnDernierRessort ?? false);
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

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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Annexe13quinquiesBeService } from '../../core/services/annexe13quinquies-be.service';
import {
  ANNEXE13QUINQUIES_MOTIFS,
  Annexe13quinquiesBeRequest,
  Annexe13quinquiesBeResponse,
  Annexe13quinquiesStatutRecours,
  MotifInterdictionEntree,
  MotifInterdictionEntreeOption,
} from '../../core/models/annexe13quinquies-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { Annexe13quinquiesBePrefillRules } from './annexe13quinquies-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Alerte de cohérence F-IA-03 — VOIE (a) : badge inline computed, AUCUNE
 * directive `appCoherencePopover` ni binding `[coherenceAlert]`. L'alerte est un
 * simple objet {label, tooltip} affiché en badge statique sous le champ divergent.
 */
export interface IM33_InlineCoherenceAlert {
  label: string;
  tooltip: string;
}

/**
 * SF-215-18 — Outil décisionnel « Annexe 13quinquies OQT + interdiction d'entrée (BE) »
 * (F-IM-33-annexe13quinquies-ie-be).
 *
 * BELGIQUE uniquement — l'annexe 13quinquies est un ordre de quitter le territoire
 * (OQT) ASSORTI d'une interdiction d'entrée (IE) sur le territoire Schengen
 * (Loi du 15/12/1980, art. 74/11). Durée IE : 3 / 5 / 8 ans selon le motif.
 * Visibilité CONTEXTUAL — flag `interdiction_entree_be_detectee`.
 *
 * Pattern miroir : {@link CceAnnulationBeSectionComponent}
 * (F-IM-31-cce-annulation-30j-be, SF-215-14).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush ; badge durée IE coloré (3 ans orange / 5 ans rouge /
 *    8 ans rouge sombre) ; dates en JetBrains Mono ; statutRecours badge couleur
 *    (DISPONIBLE vert / URGENT orange / EXPIRE rouge / FORME bleu)
 *  - pré-fill IA RÉEL 2 champs (dateNotificationAnnexe, motifInterdictionEntree) ;
 *    precedentSejour + recoursForme + dateRecours aspirationnels → non comptés
 *  - VOIE (a) F-IA-03 : alerte = badge inline computed (PAS de directive popover)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 *  - lien croisé vers F-IM-31 (recours CCE annulation) si statutRecours = URGENT
 */
@Component({
  selector: 'app-annexe13quinquies-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './annexe13quinquies-be-section.component.html',
  styleUrl: './annexe13quinquies-be-section.component.scss',
})
export class Annexe13quinquiesBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-33-annexe13quinquies-ie-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ANNEXE 13QUINQUIES OQT + INTERDICTION ENTRÉE (BE)';
  static readonly TOOL_ICON = 'block';

  static getPrefillCount(input: PrefillCountInput): number {
    return Annexe13quinquiesBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Annexe13quinquiesBeResponse | null>(null);

  dateNotificationAnnexe = signal<string | null>(null);
  motifInterdictionEntree = signal<MotifInterdictionEntree | null>(null);
  precedentSejour = signal<boolean>(false);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);

  readonly motifs: ReadonlyArray<MotifInterdictionEntreeOption> =
    ANNEXE13QUINQUIES_MOTIFS;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * VOIE (a) F-IA-03 — alerte inline sur la date de notification uniquement
   * (le motif est un enum whitelisté strictement pré-rempli). Renvoie un objet
   * {label, tooltip} ou null. AUCUNE directive popover.
   */
  dateNotificationAlert = computed<IM33_InlineCoherenceAlert | null>(() => {
    if (this.standaloneMode) return null;
    if (!this.showForm()) return null;
    const userDate = this.dateNotificationAnnexe();
    if (!userDate) return null;
    const aiDate = this.aiData?.interdictionEntreeDateNotification;
    if (typeof aiDate !== 'string' || !ISO_DATE_RE.test(aiDate)) return null;
    if (aiDate === userDate) return null;
    const aiFr = this.formatDateFr(aiDate);
    return {
      label: `Incohérence détectée (${aiFr})`,
      tooltip: `Analyse du dossier : notification de l'annexe 13quinquies le ${aiFr}`,
    };
  });

  constructor(
    private readonly service: Annexe13quinquiesBeService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
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
    const dateNotif = this.dateNotificationAnnexe();
    if (!dateNotif || !ISO_DATE_RE.test(dateNotif)) return false;
    if (!this.motifInterdictionEntree()) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr || !ISO_DATE_RE.test(dr)) return false;
    }
    return true;
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationAnnexe.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onMotifChange(value: MotifInterdictionEntree | null): void {
    this.motifInterdictionEntree.set(value);
    this.provenanceMotif.set(null);
  }

  onPrecedentSejourChange(value: boolean): void {
    this.precedentSejour.set(value);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    if (!value) {
      this.dateRecours.set(null);
    }
  }

  onDateRecoursChange(value: string | null): void {
    this.dateRecours.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: Annexe13quinquiesBeRequest = {
      dateNotificationAnnexe: this.dateNotificationAnnexe()!,
      motifInterdictionEntree: this.motifInterdictionEntree()!,
      precedentSejour: this.precedentSejour(),
      recoursForme: this.recoursForme(),
      dateRecours: this.recoursForme() ? this.dateRecours() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse annexe 13quinquies enregistrée', 'OK', { duration: 2500 });
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

  // ---- Badge durée IE : 3 ans orange / 5 ans rouge / 8 ans rouge sombre ----
  dureeBadgeClass(duree: number | null | undefined): string {
    switch (duree) {
      case 3:  return 'duree-badge duree-badge--3ans';
      case 5:  return 'duree-badge duree-badge--5ans';
      case 8:  return 'duree-badge duree-badge--8ans';
      default: return 'duree-badge';
    }
  }

  dureeLabel(duree: number | null | undefined): string {
    if (duree === null || duree === undefined) return '—';
    return `${duree} an${duree > 1 ? 's' : ''}`;
  }

  // ---- statutRecours badge couleur (4 états) ----
  statutChipClass(statut: Annexe13quinquiesStatutRecours | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE': return 'a13q-chip a13q-chip--success';
      case 'URGENT':     return 'a13q-chip a13q-chip--warning';
      case 'EXPIRE':     return 'a13q-chip a13q-chip--danger';
      case 'FORME':      return 'a13q-chip a13q-chip--info';
      default:           return 'a13q-chip';
    }
  }

  statutBannerClass(statut: Annexe13quinquiesStatutRecours | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE': return 'a13q-banner a13q-banner--success';
      case 'URGENT':     return 'a13q-banner a13q-banner--warning';
      case 'EXPIRE':     return 'a13q-banner a13q-banner--danger';
      case 'FORME':      return 'a13q-banner a13q-banner--info';
      default:           return 'a13q-banner';
    }
  }

  statutIcon(statut: Annexe13quinquiesStatutRecours | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE': return 'event_available';
      case 'URGENT':     return 'schedule';
      case 'EXPIRE':     return 'event_busy';
      case 'FORME':      return 'task_alt';
      default:           return 'info_outline';
    }
  }

  statutLabel(statut: Annexe13quinquiesStatutRecours | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE': return 'Recours disponible';
      case 'URGENT':     return 'Recours urgent';
      case 'EXPIRE':     return 'Délai de recours expiré';
      case 'FORME':      return 'Recours déjà formé';
      default:           return '';
    }
  }

  /** Lien croisé vers F-IM-31 (recours CCE annulation 30j) affiché si URGENT. */
  showCrossLinkCceAnnulation(statut: Annexe13quinquiesStatutRecours | null | undefined): boolean {
    return statut === 'URGENT';
  }

  motifLabel(code: MotifInterdictionEntree | string | null | undefined): string {
    if (!code) return '';
    return this.motifs.find((m) => m.code === code)?.label ?? String(code);
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd (typographie JetBrains Mono côté template). */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  /** Classe CSS des jours restants — rouge si négatif (délai dépassé). */
  joursRestantsClass(jours: number | null | undefined): string {
    if (jours === null || jours === undefined) return 'a13q-jours';
    return jours < 0 ? 'a13q-jours a13q-jours--negative' : 'a13q-jours';
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNotif = Annexe13quinquiesBePrefillRules.computeDateNotificationAnnexe(input);
    if (dateNotif !== null && !this.dateNotificationAnnexe()) {
      this.dateNotificationAnnexe.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }

    const motif = Annexe13quinquiesBePrefillRules.computeMotifInterdictionEntree(input);
    if (motif !== null && !this.motifInterdictionEntree()) {
      this.motifInterdictionEntree.set(motif);
      this.provenanceMotif.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationAnnexe.set(r.dateNotificationAnnexe ?? null);
        this.motifInterdictionEntree.set((r.motifInterdictionEntree as MotifInterdictionEntree) ?? null);
        this.precedentSejour.set(r.precedentSejour ?? false);
        this.recoursForme.set(r.recoursForme ?? false);
        this.dateRecours.set(r.dateRecours ?? null);
        this.provenanceDateNotification.set(null);
        this.provenanceMotif.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}

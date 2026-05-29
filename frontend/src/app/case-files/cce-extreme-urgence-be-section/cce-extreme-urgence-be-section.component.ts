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

import { CceExtremeUrgenceBeService } from '../../core/services/cce-extreme-urgence-be.service';
import {
  CCE_EXTREME_URGENCE_DELAI_JOURS_OUVRABLES,
  CCE_EXTREME_URGENCE_TYPES_ACTE,
  CceExtremeUrgenceBeRequest,
  CceExtremeUrgenceBeResponse,
  CceExtremeUrgenceStatut,
  CceExtremeUrgenceTypeActe,
  CceExtremeUrgenceTypeActeOption,
} from '../../core/models/cce-extreme-urgence-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CceExtremeUrgenceBePrefillRules } from './cce-extreme-urgence-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Alerte de cohérence F-IA-03 — modèle LOCAL au composant (voie a anti-régression :
 * PAS de dépendance à la directive `appCoherencePopover` ni au binding
 * `[coherenceAlert]` — cf. hotfixes #1385/#1386). Rendu via un simple badge inline.
 */
export interface IM32_CceCoherenceAlert {
  expectedDisplay: string;
  reason: string;
}

/**
 * SF-215-16 — Outil décisionnel « Recours CCE extrême urgence 5j (BE) »
 * (F-IM-32-cce-extreme-urgence-5j-be).
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (juridiction administrative
 * belge en droit des étrangers) — AUCUN lien avec le crédit / la banque.
 *
 * BELGIQUE uniquement — calculateur du délai du recours en EXTRÊME URGENCE devant
 * le CCE : 5 jours OUVRABLES à compter de la notification de l'acte exécutoire
 * (Loi 15/12/1980 art. 39/82). Visibilité CONTEXTUAL — flag
 * `recours_cce_extreme_urgence`. Cas d'URGENCE ABSOLUE — affichage proéminent.
 *
 * Pattern miroir : {@link CceAnnulationBeSectionComponent}
 * (F-IM-31-cce-annulation-30j-be, SF-215-14, vague 7).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert DISPONIBLE, ROUGE PROÉMINENT CRITIQUE/EXPIRE
 *    (exception justifiée au rouge réservé : urgence absolue), bleu RECOURS_FORME
 *  - pré-fill IA RÉEL 2 champs (dateActeExecutoire, typeActe) ;
 *    recoursForme + dateRecours aspirationnels → ne comptent jamais
 *  - `joursOuvrablesRestants` affiché en GROS, JetBrains Mono, « jours ouvrables BE »
 *  - `dateLimiteRecours` + `audienceEstimee` en JetBrains Mono ; audience en info-box
 *  - bandeau rouge proéminent avec `actionImmediate` si CRITIQUE/EXPIRE
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 *  - VOIE (a) : aucune directive appCoherencePopover ni binding [coherenceAlert]
 *    (anti-régression hotfixes #1385/#1386) — alerte F-IA-03 = badge inline simple
 */
@Component({
  selector: 'app-cce-extreme-urgence-be-section',
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
  templateUrl: './cce-extreme-urgence-be-section.component.html',
  styleUrl: './cce-extreme-urgence-be-section.component.scss',
})
export class CceExtremeUrgenceBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-32-cce-extreme-urgence-5j-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RECOURS CCE EXTRÊME URGENCE 5J (BE)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return CceExtremeUrgenceBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly delaiJoursOuvrables = CCE_EXTREME_URGENCE_DELAI_JOURS_OUVRABLES;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<CceExtremeUrgenceBeResponse | null>(null);

  dateActeExecutoire = signal<string | null>(null);
  typeActe = signal<CceExtremeUrgenceTypeActe | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  provenanceDateActe = signal<'IA' | null>(null);
  provenanceTypeActe = signal<'IA' | null>(null);

  readonly typesActe: ReadonlyArray<CceExtremeUrgenceTypeActeOption> =
    CCE_EXTREME_URGENCE_TYPES_ACTE;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alerte de cohérence F-IA-03 — uniquement sur la date de l'acte exécutoire
   * (le type d'acte est un enum whitelisté strictement pré-rempli).
   * Renvoie `null` si pas de divergence — aucune dépendance à la directive popover.
   */
  dateActeAlert = computed<IM32_CceCoherenceAlert | null>(() => {
    if (this.standaloneMode) return null;
    if (!this.showForm()) return null;
    const userDate = this.dateActeExecutoire();
    if (!userDate) return null;
    const aiDate = this.aiData?.recoursExtremeUrgenceDateActe;
    if (typeof aiDate !== 'string' || !ISO_DATE_RE.test(aiDate) || aiDate === userDate) {
      return null;
    }
    return {
      expectedDisplay: this.formatDateFr(aiDate),
      reason: `Analyse du dossier : acte exécutoire daté du ${this.formatDateFr(aiDate)}`,
    };
  });

  constructor(
    private readonly service: CceExtremeUrgenceBeService,
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
    const dateActe = this.dateActeExecutoire();
    if (!dateActe || !ISO_DATE_RE.test(dateActe)) return false;
    if (!this.typeActe()) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr || !ISO_DATE_RE.test(dr)) return false;
    }
    return true;
  }

  onDateActeChange(value: string | null): void {
    this.dateActeExecutoire.set(value || null);
    this.provenanceDateActe.set(null);
  }

  onTypeActeChange(value: CceExtremeUrgenceTypeActe | null): void {
    this.typeActe.set(value);
    this.provenanceTypeActe.set(null);
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
    const request: CceExtremeUrgenceBeRequest = {
      dateActeExecutoire: this.dateActeExecutoire()!,
      typeActe: this.typeActe()!,
      recoursForme: this.recoursForme(),
      dateRecours: this.recoursForme() ? this.dateRecours() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse recours CCE extrême urgence enregistrée', 'OK', { duration: 2500 });
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

  /** True si le statut justifie le BANDEAU ROUGE PROÉMINENT (urgence absolue). */
  isUrgenceAbsolue(statut: CceExtremeUrgenceStatut | null | undefined): boolean {
    return statut === 'CRITIQUE' || statut === 'EXPIRE';
  }

  statutBannerClass(statut: CceExtremeUrgenceStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'cce-banner cce-banner--success';
      case 'CRITIQUE':      return 'cce-banner cce-banner--critical';
      case 'EXPIRE':        return 'cce-banner cce-banner--critical';
      case 'RECOURS_FORME': return 'cce-banner cce-banner--info';
      default:              return 'cce-banner';
    }
  }

  statutChipClass(statut: CceExtremeUrgenceStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'cce-chip cce-chip--success';
      case 'CRITIQUE':      return 'cce-chip cce-chip--critical';
      case 'EXPIRE':        return 'cce-chip cce-chip--critical';
      case 'RECOURS_FORME': return 'cce-chip cce-chip--info';
      default:              return 'cce-chip';
    }
  }

  statutIcon(statut: CceExtremeUrgenceStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'event_available';
      case 'CRITIQUE':      return 'priority_high';
      case 'EXPIRE':        return 'event_busy';
      case 'RECOURS_FORME': return 'task_alt';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: CceExtremeUrgenceStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'CRITIQUE':      return 'URGENCE ABSOLUE';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours déjà formé';
      default:              return '';
    }
  }

  typeActeLabel(code: CceExtremeUrgenceTypeActe | string | null | undefined): string {
    if (!code) return '';
    return this.typesActe.find((t) => t.code === code)?.label ?? String(code);
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd (JetBrains Mono côté template). */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  /** Classe CSS des jours ouvrables restants — rouge si ≤ 0 (délai dépassé/épuisé). */
  joursRestantsClass(jours: number | null | undefined): string {
    if (jours === null || jours === undefined) return 'cce-jours';
    return jours <= 0 ? 'cce-jours cce-jours--negative' : 'cce-jours';
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateActe = CceExtremeUrgenceBePrefillRules.computeDateActeExecutoire(input);
    if (dateActe !== null && !this.dateActeExecutoire()) {
      this.dateActeExecutoire.set(dateActe);
      this.provenanceDateActe.set('IA');
    }

    const type = CceExtremeUrgenceBePrefillRules.computeTypeActe(input);
    if (type !== null && !this.typeActe()) {
      this.typeActe.set(type);
      this.provenanceTypeActe.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateActeExecutoire.set(r.dateActeExecutoire ?? null);
        this.typeActe.set((r.typeActe as CceExtremeUrgenceTypeActe) ?? null);
        this.recoursForme.set(r.recoursForme ?? false);
        this.dateRecours.set(r.dateRecours ?? null);
        this.provenanceDateActe.set(null);
        this.provenanceTypeActe.set(null);
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

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

import { PourvoiCassationSocService } from '../../core/services/pourvoi-cassation-soc.service';
import {
  CasOuverturePourvoi,
  ForceProbatoireCas,
  PourvoiCassationSocRequest,
  PourvoiCassationSocResponse,
  PourvoiCassationSocVerdict,
  PourvoiVerdictDelai,
  RisqueNonAdmission,
} from '../../core/models/pourvoi-cassation-soc.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { PourvoiCassationSocPrefillRules } from './pourvoi-cassation-soc-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-06 — Outil décisionnel « Pourvoi en cassation chambre sociale »
 * (F-DT-87-pourvoi-cassation-soc).
 *
 * FRANCE uniquement — pourvoi en cassation d'un arrêt de la chambre sociale de
 * la Cour d'appel devant la Cour de cassation (art. 901 et s. CPC ; art. 604
 * CPC — cas d'ouverture ; art. 612 CPC — délai de 2 mois ; art. 973 CPC —
 * représentation par avocat aux Conseils obligatoire ; art. 1014 CPC —
 * procédure de non-admission / filtre NPC).
 *
 * Le composant : multi-select des 7 cas d'ouverture (avec force probatoire),
 * calcul du délai de 2 mois (date limite + jours restants + verdict délai),
 * scoring du risque de non-admission (filtre NPC) et verdict global
 * d'opportunité (4 états).
 * Pattern miroir : {@link AppelCphSectionComponent} (F-DT-86, SF-218-02).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert POURVOI_RECOMMANDE,
 *    orange POURVOI_RISQUE, navy POURVOI_DECONSEILLE, rouge DELAI_EXPIRE
 *  - badge risqueNonAdmission (ELEVE rouge / MODERE or / FAIBLE vert)
 *  - pré-fill IA (date de notification de l'arrêt CA) depuis TravailExtractedData
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Complète le backend SF-218-05. Le pourvoi en cassation requiert la
 * représentation obligatoire par un avocat aux Conseils (art. 973 CPC) :
 * un item bloquant le signale quand cette représentation n'est pas constituée.
 */
@Component({
  selector: 'app-pourvoi-cassation-soc-section',
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
  templateUrl: './pourvoi-cassation-soc-section.component.html',
  styleUrl: './pourvoi-cassation-soc-section.component.scss',
})
export class PourvoiCassationSocSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-87-pourvoi-cassation-soc';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'POURVOI CASSATION SOCIALE (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return PourvoiCassationSocPrefillRules.computePrefillCount(input);
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
  result = signal<PourvoiCassationSocResponse | null>(null);

  dateNotificationArret = signal<string | null>(null);
  casOuverture = signal<CasOuverturePourvoi[]>([]);
  representationAvocatCassation = signal<boolean>(false);
  moyenSerieuxIdentifie = signal<boolean>(false);

  provenanceDateNotificationArret = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Les 7 cas d'ouverture proposés dans le multi-select. */
  readonly casOuvertureOptions: { value: CasOuverturePourvoi; label: string }[] = [
    { value: 'VIOLATION_LOI', label: 'Violation de la loi' },
    { value: 'DEFAUT_BASE_LEGALE', label: 'Défaut de base légale' },
    { value: 'DENATURATION', label: 'Dénaturation' },
    { value: 'MANQUE_DE_BASE', label: 'Manque de base légale' },
    { value: 'CONTRADICTION_MOTIFS', label: 'Contradiction de motifs' },
    { value: 'VICE_FORME', label: 'Vice de forme' },
    { value: 'PERTE_FONDEMENT_JURIDIQUE', label: 'Perte de fondement juridique' },
  ];

  constructor(
    private readonly service: PourvoiCassationSocService,
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

  /** Form valide : date de notification présente + au moins un cas d'ouverture. */
  formValid(): boolean {
    if (!this.dateNotificationArret()) return false;
    if (this.casOuverture().length === 0) return false;
    return true;
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationArret.set(value || null);
    this.provenanceDateNotificationArret.set(null);
  }

  onCasOuvertureChange(value: CasOuverturePourvoi[]): void {
    this.casOuverture.set(value ?? []);
  }

  onRepresentationChange(value: boolean): void {
    this.representationAvocatCassation.set(value);
  }

  onMoyenSerieuxChange(value: boolean): void {
    this.moyenSerieuxIdentifie.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: PourvoiCassationSocRequest = {
      dateNotificationArret: this.dateNotificationArret()!,
      casOuverture: this.casOuverture(),
      representationAvocatCassation: this.representationAvocatCassation(),
      moyenSerieuxIdentifie: this.moyenSerieuxIdentifie(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse pourvoi cassation sociale enregistrée', 'OK', { duration: 2500 });
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

  // --- labels / helpers -----------------------------------------------------

  verdictLabel(verdict: PourvoiCassationSocVerdict | null | undefined): string {
    switch (verdict) {
      case 'POURVOI_RECOMMANDE':  return 'Pourvoi recommandé';
      case 'POURVOI_RISQUE':      return 'Pourvoi à risque';
      case 'POURVOI_DECONSEILLE': return 'Pourvoi déconseillé';
      case 'DELAI_EXPIRE':        return 'Délai de pourvoi expiré';
      default:                    return '';
    }
  }

  verdictDelaiLabel(verdict: PourvoiVerdictDelai | null | undefined): string {
    switch (verdict) {
      case 'DELAI_OUVERT': return 'Délai de pourvoi ouvert';
      case 'DELAI_URGENT': return 'Délai de pourvoi urgent';
      case 'DELAI_EXPIRE': return 'Délai de pourvoi expiré';
      default:             return '';
    }
  }

  risqueLabel(risque: RisqueNonAdmission | null | undefined): string {
    switch (risque) {
      case 'ELEVE':  return 'Risque de non-admission élevé';
      case 'MODERE': return 'Risque de non-admission modéré';
      case 'FAIBLE': return 'Risque de non-admission faible';
      default:       return '';
    }
  }

  forceProbatoireLabel(force: ForceProbatoireCas | null | undefined): string {
    switch (force) {
      case 'FORTE':   return 'Force forte';
      case 'MOYENNE': return 'Force moyenne';
      case 'FAIBLE':  return 'Force faible';
      default:        return '';
    }
  }

  /** Classe du chip verdict global. */
  verdictChipClass(verdict: PourvoiCassationSocVerdict | null | undefined): string {
    switch (verdict) {
      case 'POURVOI_RECOMMANDE':  return 'pcs-chip pcs-chip--success';
      case 'POURVOI_RISQUE':      return 'pcs-chip pcs-chip--warning';
      case 'POURVOI_DECONSEILLE': return 'pcs-chip pcs-chip--navy';
      case 'DELAI_EXPIRE':        return 'pcs-chip pcs-chip--danger';
      default:                    return 'pcs-chip';
    }
  }

  bannerClass(verdict: PourvoiCassationSocVerdict | null | undefined): string {
    switch (verdict) {
      case 'POURVOI_RECOMMANDE':  return 'pcs-banner pcs-banner--success';
      case 'POURVOI_RISQUE':      return 'pcs-banner pcs-banner--warning';
      case 'POURVOI_DECONSEILLE': return 'pcs-banner pcs-banner--navy';
      case 'DELAI_EXPIRE':        return 'pcs-banner pcs-banner--danger';
      default:                    return 'pcs-banner';
    }
  }

  bannerIcon(verdict: PourvoiCassationSocVerdict | null | undefined): string {
    switch (verdict) {
      case 'POURVOI_RECOMMANDE':  return 'check_circle';
      case 'POURVOI_RISQUE':      return 'warning';
      case 'POURVOI_DECONSEILLE': return 'thumb_down';
      case 'DELAI_EXPIRE':        return 'error';
      default:                    return 'info_outline';
    }
  }

  /** Classe du badge risque de non-admission (filtre NPC). */
  risqueBadgeClass(risque: RisqueNonAdmission | null | undefined): string {
    switch (risque) {
      case 'ELEVE':  return 'pcs-risque pcs-risque--danger';
      case 'MODERE': return 'pcs-risque pcs-risque--warning';
      case 'FAIBLE': return 'pcs-risque pcs-risque--success';
      default:       return 'pcs-risque';
    }
  }

  /** Classe colorant la force probatoire d'un cas d'ouverture. */
  forceProbatoireClass(force: ForceProbatoireCas | null | undefined): string {
    switch (force) {
      case 'FORTE':   return 'pcs-force pcs-force--strong';
      case 'MOYENNE': return 'pcs-force pcs-force--medium';
      case 'FAIBLE':  return 'pcs-force pcs-force--weak';
      default:        return 'pcs-force';
    }
  }

  /** Le compteur de jours n'a de sens que tant que le délai n'est pas expiré (affiché aussi expiré, en négatif). */
  showJoursRestants(verdict: PourvoiCassationSocVerdict | null | undefined): boolean {
    return verdict !== null && verdict !== undefined;
  }

  casOuvertureLabel(cas: CasOuverturePourvoi | null | undefined): string {
    const opt = this.casOuvertureOptions.find((o) => o.value === cas);
    return opt ? opt.label : (cas ?? '');
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNotif = PourvoiCassationSocPrefillRules.computeDateNotificationArret(input);
    if (dateNotif !== null && this.dateNotificationArret() === null) {
      this.dateNotificationArret.set(dateNotif);
      this.provenanceDateNotificationArret.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationArret.set(r.dateNotificationArret ?? null);
        this.casOuverture.set(r.casOuverture ?? []);
        this.representationAvocatCassation.set(r.representationAvocatCassation ?? false);
        this.moyenSerieuxIdentifie.set(r.moyenSerieuxIdentifie ?? false);
        this.provenanceDateNotificationArret.set(null);
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

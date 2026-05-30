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

import { IntermittentSpectacleAreService } from '../../core/services/intermittent-spectacle-are.service';
import {
  AnnexeIntermittent,
  IntermittentSpectacleAreRequest,
  IntermittentSpectacleAreResponse,
  OuvertureDroits,
  StatutOuverture,
} from '../../core/models/intermittent-spectacle-are.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { IntermittentSpectacleArePrefillRules } from './intermittent-spectacle-are-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-18 — Outil décisionnel « Intermittent du spectacle : ouverture des
 * droits ARE (annexes 8/10) » (F-DT-106-intermittent-spectacle-are).
 *
 * FRANCE uniquement — applique le régime des intermittents du spectacle du
 * règlement d'assurance chômage Unedic (annexe 8 techniciens / annexe 10
 * artistes) : condition d'affiliation de 507 heures sur les 12 mois précédant
 * la fin du contrat, conversion des cachets artistiques (1 cachet = 12 h),
 * écrêtage des heures de formation assimilables → verdict d'ouverture des
 * droits, déficit / excédent d'heures et date indicative de réexamen annuel.
 *
 * Statut (3 états) :
 *  - DROITS_OUVERTS (vert) : seuil 507 h atteint.
 *  - DROITS_NON_OUVERTS (rouge) : seuil non atteint.
 *  - A_VERIFIER (or) : total dans la marge de ± 10 h → vérification France Travail.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé NON_OUVERTS / heures
 *    manquantes ; vert OUVERTS / heures excédentaires ; `<input type="date">` ;
 *    JetBrains Mono heures / baseJuridique ; bannière gate FR ; MatSnackBar ;
 *    pas de refresh dashboard requis (calcul sans action validée) — refresh
 *    déclenché par cohérence avec le pattern frère.
 *  - pré-fill IA (dateFinContrat, annexe) depuis TravailExtractedData + badge
 *    `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (date de fin de contrat future ; cachets saisis
 *    hors annexe 10 ; total proche du seuil 507 h).
 */
@Component({
  selector: 'app-intermittent-spectacle-are-section',
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
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './intermittent-spectacle-are-section.component.html',
  styleUrl: './intermittent-spectacle-are-section.component.scss',
})
export class IntermittentSpectacleAreSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-106-intermittent-spectacle-are';
  protected readonly brancheActiveForJurisprudence = 'default';

  /** Seuil d'affiliation Unedic (507 h sur 12 mois) — à actualiser à chaque convention. */
  protected readonly SEUIL_HEURES = 507;

  static readonly TOOL_LABEL = 'INTERMITTENT SPECTACLE ARE (FR)';
  static readonly TOOL_ICON = 'theater_comedy';

  static getPrefillCount(input: PrefillCountInput): number {
    return IntermittentSpectacleArePrefillRules.computePrefillCount(input);
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
  result = signal<IntermittentSpectacleAreResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  annexe = signal<AnnexeIntermittent>('ANNEXE_8_TECHNICIENS');
  dateFinContrat = signal<string | null>(null);
  heuresTravaillees12Mois = signal<number | null>(null);
  nombreCachets = signal<number | null>(null);
  heuresFormationDispensees = signal<number | null>(null);

  provenanceAnnexe = signal<'IA' | null>(null);
  provenanceDateFinContrat = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** true si annexe 10 (artistes) → champ nombre de cachets visible. */
  isAnnexe10 = computed<boolean>(() => this.annexe() === 'ANNEXE_10_ARTISTES');

  /** Total d'heures estimé localement (travaillées + cachets × 12) pour la barre. */
  totalHeuresEstime = computed<number>(() => {
    const travaillees = this.heuresTravaillees12Mois() ?? 0;
    const cachets = this.isAnnexe10() ? (this.nombreCachets() ?? 0) * 12 : 0;
    const formation = this.heuresFormationDispensees() ?? 0;
    return Math.max(0, travaillees + cachets + formation);
  });

  /** Pourcentage de progression vers le seuil 507 h (borné à 100 %). */
  progressionPct = computed<number>(() => {
    const total = this.totalHeuresEstime();
    return Math.min(100, Math.round((total / this.SEUIL_HEURES) * 100));
  });

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : date de fin future,
   * cachets saisis hors annexe 10, total proche du seuil.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const fin = this.dateFinContrat();
    if (fin) {
      const today = new Date().toISOString().slice(0, 10);
      if (fin > today) {
        alerts.push(
          'La date de fin de contrat est dans le futur : la recherche de droits porte sur les 12 mois précédant une fin de contrat passée.',
        );
      }
    }
    if (!this.isAnnexe10() && (this.nombreCachets() ?? 0) > 0) {
      alerts.push(
        'Des cachets sont saisis alors que l\'annexe 8 (techniciens) est sélectionnée : la conversion des cachets ne s\'applique qu\'à l\'annexe 10 (artistes).',
      );
    }
    const ecart = Math.abs(this.totalHeuresEstime() - this.SEUIL_HEURES);
    if (this.totalHeuresEstime() > 0 && ecart <= 10) {
      alerts.push(
        'Le total est dans la marge de ± 10 h du seuil de 507 h : une vérification finale auprès de France Travail est recommandée.',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: IntermittentSpectacleAreService,
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

  /** Form valide : date de fin présente + heures travaillées >= 0. */
  formValid(): boolean {
    if (!this.dateFinContrat()) return false;
    const h = this.heuresTravaillees12Mois();
    if (h === null || h < 0) return false;
    if ((this.nombreCachets() ?? 0) < 0) return false;
    if ((this.heuresFormationDispensees() ?? 0) < 0) return false;
    return true;
  }

  onAnnexeChange(value: AnnexeIntermittent): void {
    this.annexe.set(value);
    this.provenanceAnnexe.set(null);
  }

  onDateFinContratChange(value: string | null): void {
    this.dateFinContrat.set(value || null);
    this.provenanceDateFinContrat.set(null);
  }

  onHeuresTravailleesChange(value: number | null): void {
    this.heuresTravaillees12Mois.set(value === null || value === undefined ? null : Number(value));
  }

  onNombreCachetsChange(value: number | null): void {
    this.nombreCachets.set(value === null || value === undefined ? null : Number(value));
  }

  onHeuresFormationChange(value: number | null): void {
    this.heuresFormationDispensees.set(value === null || value === undefined ? null : Number(value));
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: IntermittentSpectacleAreRequest = {
      annexe: this.annexe(),
      dateFinContrat: this.dateFinContrat()!,
      heuresTravaillees12Mois: this.heuresTravaillees12Mois()!,
      nombreCachets: this.nombreCachets() ?? 0,
      heuresFormationDispensees: this.heuresFormationDispensees() ?? 0,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse ouverture droits ARE enregistrée', 'OK', { duration: 2500 });
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

  annexeLabel(a: AnnexeIntermittent | null | undefined): string {
    switch (a) {
      case 'ANNEXE_8_TECHNICIENS': return 'Annexe 8 — techniciens';
      case 'ANNEXE_10_ARTISTES':   return 'Annexe 10 — artistes';
      default:                     return '';
    }
  }

  ouvertureLabel(o: OuvertureDroits | null | undefined): string {
    switch (o) {
      case 'OUVERTS':     return 'Droits ouverts';
      case 'NON_OUVERTS': return 'Droits non ouverts';
      default:            return '';
    }
  }

  ouvertureChipClass(o: OuvertureDroits | null | undefined): string {
    switch (o) {
      case 'OUVERTS':     return 'is-chip is-chip--success';
      case 'NON_OUVERTS': return 'is-chip is-chip--danger';
      default:            return 'is-chip';
    }
  }

  statutLabel(s: StatutOuverture | null | undefined): string {
    switch (s) {
      case 'DROITS_OUVERTS':     return 'Droits ouverts';
      case 'DROITS_NON_OUVERTS': return 'Droits non ouverts';
      case 'A_VERIFIER':         return 'À vérifier (France Travail)';
      default:                   return '';
    }
  }

  statutChipClass(s: StatutOuverture | null | undefined): string {
    switch (s) {
      case 'DROITS_OUVERTS':     return 'is-chip is-chip--success';
      case 'DROITS_NON_OUVERTS': return 'is-chip is-chip--danger';
      case 'A_VERIFIER':         return 'is-chip is-chip--warning';
      default:                   return 'is-chip';
    }
  }

  bannerClass(s: StatutOuverture | null | undefined): string {
    switch (s) {
      case 'DROITS_OUVERTS':     return 'is-banner is-banner--success';
      case 'DROITS_NON_OUVERTS': return 'is-banner is-banner--danger';
      case 'A_VERIFIER':         return 'is-banner is-banner--warning';
      default:                   return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: StatutOuverture | null | undefined): string {
    switch (s) {
      case 'DROITS_OUVERTS':     return 'verified';
      case 'DROITS_NON_OUVERTS': return 'block';
      case 'A_VERIFIER':         return 'help_outline';
      default:                   return 'info_outline';
    }
  }

  /** Pourcentage de progression du résultat persisté vers le seuil 507 h. */
  resultProgressionPct(r: IntermittentSpectacleAreResponse): number {
    const seuil = r.seuilHeures || this.SEUIL_HEURES;
    return Math.min(100, Math.round((r.heuresTotalesRetenues / seuil) * 100));
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const annexe = IntermittentSpectacleArePrefillRules.computeAnnexe(input);
    if (annexe !== null && this.provenanceAnnexe() === null) {
      this.annexe.set(annexe);
      this.provenanceAnnexe.set('IA');
    }

    const fin = IntermittentSpectacleArePrefillRules.computeDateFinContrat(input);
    if (fin !== null && this.dateFinContrat() === null) {
      this.dateFinContrat.set(fin);
      this.provenanceDateFinContrat.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.annexe.set(r.annexe ?? 'ANNEXE_8_TECHNICIENS');
        this.dateFinContrat.set(r.dateFinContrat ?? null);
        this.heuresTravaillees12Mois.set(r.heuresTravaillees12Mois ?? null);
        this.nombreCachets.set(r.nombreCachets ?? null);
        this.heuresFormationDispensees.set(r.heuresFormationDispensees ?? null);
        this.provenanceAnnexe.set(null);
        this.provenanceDateFinContrat.set(null);
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

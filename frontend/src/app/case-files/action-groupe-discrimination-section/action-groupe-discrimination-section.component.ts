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

import { ActionGroupeDiscriminationService } from '../../core/services/action-groupe-discrimination.service';
import {
  ActionGroupeDiscriminationRequest,
  ActionGroupeDiscriminationResponse,
  ActionGroupeDiscriminationVerdict,
  MotifDiscrimination,
  ObjetActionGroupe,
  TypeOrganisationActionGroupe,
} from '../../core/models/action-groupe-discrimination.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ActionGroupeDiscriminationPrefillRules } from './action-groupe-discrimination-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-10 — Outil décisionnel « Action de groupe en discrimination »
 * (F-DT-90-action-groupe-discrimination).
 *
 * FRANCE uniquement — analyse de la recevabilité d'une action de groupe en
 * discrimination au travail (loi J21 du 18/11/2016, L. 1134-7 à L. 1134-10 Code
 * travail) : qualité de l'organisation habilitée (syndicat représentatif ou
 * association régulièrement déclarée depuis ≥ 5 ans), mise en demeure préalable
 * de l'employeur et délai de carence de 6 mois avant saisine, pluralité de
 * personnes placées dans une situation similaire. Produit un verdict de
 * recevabilité, la date à partir de laquelle la saisine est recevable et une
 * checklist procédurale.
 *
 * Verdict de recevabilité (4 états) :
 *  - RECEVABLE (vert) : qualité à agir + pluralité + carence respectée.
 *  - PREMATURE (or) : mise en demeure faite mais délai de 6 mois non écoulé.
 *  - IRRECEVABLE_QUALITE (rouge) : organisation non habilitée.
 *  - INFO_MANQUANTE (gris) : mise en demeure absente — recevabilité non vérifiable.
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush ; vert RECEVABLE, or PREMATURE, rouge réservé
 *    IRRECEVABLE_QUALITE, gris INFO_MANQUANTE ; JetBrains Mono dates / baseJuridique.
 *  - pré-fill IA (motifDiscrimination, dateMiseEnDemeure) depuis TravailExtractedData
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *  - F-IA-03 : coherenceAlerts croise motifDiscrimination / dateMiseEnDemeure et aiData.
 */
@Component({
  selector: 'app-action-groupe-discrimination-section',
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
  templateUrl: './action-groupe-discrimination-section.component.html',
  styleUrl: './action-groupe-discrimination-section.component.scss',
})
export class ActionGroupeDiscriminationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-90-action-groupe-discrimination';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ACTION DE GROUPE DISCRIMINATION (FR)';
  static readonly TOOL_ICON = 'groups';

  static getPrefillCount(input: PrefillCountInput): number {
    return ActionGroupeDiscriminationPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly typeOrganisationOptions: { value: TypeOrganisationActionGroupe; label: string }[] = [
    { value: 'SYNDICAT_REPRESENTATIF', label: 'Syndicat représentatif' },
    { value: 'ASSOCIATION_AGREEE_5ANS', label: 'Association déclarée depuis ≥ 5 ans' },
    { value: 'AUTRE', label: 'Autre (non habilitée)' },
  ];

  readonly motifOptions: { value: MotifDiscrimination; label: string }[] = [
    { value: 'ORIGINE', label: 'Origine' },
    { value: 'SEXE', label: 'Sexe' },
    { value: 'AGE', label: 'Âge' },
    { value: 'HANDICAP', label: 'Handicap' },
    { value: 'ETAT_SANTE', label: 'État de santé' },
    { value: 'GROSSESSE', label: 'Grossesse' },
    { value: 'ACTIVITE_SYNDICALE', label: 'Activité syndicale' },
    { value: 'RELIGION', label: 'Religion / convictions' },
    { value: 'ORIENTATION_SEXUELLE', label: 'Orientation sexuelle' },
    { value: 'AUTRE', label: 'Autre critère (L. 1132-1)' },
  ];

  readonly objetOptions: { value: ObjetActionGroupe; label: string }[] = [
    { value: 'CESSATION_MANQUEMENT', label: 'Cessation du manquement' },
    { value: 'REPARATION_PREJUDICES', label: 'Réparation des préjudices' },
    { value: 'LES_DEUX', label: 'Cessation et réparation' },
  ];

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<ActionGroupeDiscriminationResponse | null>(null);

  typeOrganisation = signal<TypeOrganisationActionGroupe | null>(null);
  motifDiscrimination = signal<MotifDiscrimination | null>(null);
  dateMiseEnDemeure = signal<string | null>(null);
  nombrePersonnesConcernees = signal<number | null>(null);
  objetAction = signal<ObjetActionGroupe>('LES_DEUX');

  provenanceMotifDiscrimination = signal<'IA' | null>(null);
  provenanceDateMiseEnDemeure = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Date du jour (YYYY-MM-DD) — borne max du picker mise en demeure (pas de date future). */
  readonly todayIso = new Date().toISOString().slice(0, 10);

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes). Signale l'absence de mise en
   * demeure (condition bloquante L. 1134-9) et une date future incohérente.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    if (this.dateMiseEnDemeure() === null) {
      alerts.push(
        "Aucune mise en demeure renseignée : la saisine n'est recevable qu'après mise en demeure de l'employeur et un délai de 6 mois (L. 1134-9) — la recevabilité ne pourra pas être confirmée.",
      );
    } else if ((this.dateMiseEnDemeure() ?? '') > this.todayIso) {
      alerts.push(
        'La date de mise en demeure est postérieure à aujourd\'hui : vérifiez la date saisie.',
      );
    }
    if (
      this.nombrePersonnesConcernees() !== null &&
      (this.nombrePersonnesConcernees() ?? 0) < 2
    ) {
      alerts.push(
        "L'action de groupe suppose une pluralité de personnes placées dans une situation similaire (au moins 2).",
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: ActionGroupeDiscriminationService,
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

  /** Form valide : type d'organisation + motif + nombre de personnes ≥ 1 renseignés. */
  formValid(): boolean {
    if (this.typeOrganisation() === null) return false;
    if (this.motifDiscrimination() === null) return false;
    if (this.nombrePersonnesConcernees() === null || (this.nombrePersonnesConcernees() ?? 0) < 1) return false;
    return true;
  }

  onTypeOrganisationChange(value: TypeOrganisationActionGroupe): void {
    this.typeOrganisation.set(value);
  }

  onMotifChange(value: MotifDiscrimination): void {
    this.motifDiscrimination.set(value);
    this.provenanceMotifDiscrimination.set(null);
  }

  onDateMiseEnDemeureChange(value: string | null): void {
    this.dateMiseEnDemeure.set(value && value.trim() !== '' ? value : null);
    this.provenanceDateMiseEnDemeure.set(null);
  }

  onNombrePersonnesChange(value: number | null): void {
    this.nombrePersonnesConcernees.set(value === null || value === undefined ? null : Math.max(0, Math.trunc(Number(value))));
  }

  onObjetChange(value: ObjetActionGroupe): void {
    this.objetAction.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ActionGroupeDiscriminationRequest = {
      typeOrganisation: this.typeOrganisation()!,
      dateMiseEnDemeure: this.dateMiseEnDemeure(),
      motifDiscrimination: this.motifDiscrimination()!,
      nombrePersonnesConcernees: this.nombrePersonnesConcernees()!,
      objetAction: this.objetAction(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse action de groupe discrimination enregistrée', 'OK', { duration: 2500 });
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

  verdictLabel(verdict: ActionGroupeDiscriminationVerdict | null | undefined): string {
    switch (verdict) {
      case 'RECEVABLE':            return 'Action recevable';
      case 'PREMATURE':            return 'Action prématurée (carence en cours)';
      case 'IRRECEVABLE_QUALITE':  return 'Irrecevable — organisation non habilitée';
      case 'INFO_MANQUANTE':       return 'Information manquante (mise en demeure)';
      default:                     return '';
    }
  }

  /** Classe du chip verdict (header). */
  chipClass(verdict: ActionGroupeDiscriminationVerdict | null | undefined): string {
    switch (verdict) {
      case 'RECEVABLE':            return 'agd-chip agd-chip--success';
      case 'PREMATURE':            return 'agd-chip agd-chip--warning';
      case 'IRRECEVABLE_QUALITE':  return 'agd-chip agd-chip--danger';
      case 'INFO_MANQUANTE':       return 'agd-chip agd-chip--neutral';
      default:                     return 'agd-chip';
    }
  }

  bannerClass(verdict: ActionGroupeDiscriminationVerdict | null | undefined): string {
    switch (verdict) {
      case 'RECEVABLE':            return 'agd-banner agd-banner--success';
      case 'PREMATURE':            return 'agd-banner agd-banner--warning';
      case 'IRRECEVABLE_QUALITE':  return 'agd-banner agd-banner--danger';
      case 'INFO_MANQUANTE':       return 'agd-banner agd-banner--neutral';
      default:                     return 'agd-banner';
    }
  }

  bannerIcon(verdict: ActionGroupeDiscriminationVerdict | null | undefined): string {
    switch (verdict) {
      case 'RECEVABLE':            return 'verified';
      case 'PREMATURE':            return 'hourglass_top';
      case 'IRRECEVABLE_QUALITE':  return 'block';
      case 'INFO_MANQUANTE':       return 'help_outline';
      default:                     return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const motif = ActionGroupeDiscriminationPrefillRules.computeMotifDiscrimination(input);
    if (motif !== null && this.motifDiscrimination() === null) {
      this.motifDiscrimination.set(motif);
      this.provenanceMotifDiscrimination.set('IA');
    }

    const dateMed = ActionGroupeDiscriminationPrefillRules.computeDateMiseEnDemeure(input);
    if (dateMed !== null && this.dateMiseEnDemeure() === null) {
      this.dateMiseEnDemeure.set(dateMed);
      this.provenanceDateMiseEnDemeure.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeOrganisation.set(r.typeOrganisation ?? null);
        this.motifDiscrimination.set(r.motifDiscrimination ?? null);
        this.dateMiseEnDemeure.set(r.dateMiseEnDemeure ?? null);
        this.nombrePersonnesConcernees.set(r.nombrePersonnesConcernees ?? null);
        this.objetAction.set(r.objetAction ?? 'LES_DEUX');
        this.provenanceMotifDiscrimination.set(null);
        this.provenanceDateMiseEnDemeure.set(null);
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

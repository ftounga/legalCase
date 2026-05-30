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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApprentissageRuptureService } from '../../core/services/apprentissage-rupture.service';
import {
  AuteurRupture,
  ApprentissageRuptureRequest,
  ApprentissageRuptureResponse,
  MotifRupture,
  PeriodeRupture,
  ValiditeRupture,
  VerdictGlobal,
} from '../../core/models/apprentissage-rupture.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ApprentissageRupturePrefillRules } from './apprentissage-rupture-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-24 — Outil décisionnel « Apprentissage : validité de la rupture du
 * contrat » (F-DT-110-apprentissage-rupture).
 *
 * FRANCE uniquement — applique le régime hybride du contrat d'apprentissage
 * (art. L.6222-18 et s. CT) : distingue la rupture pendant les 45 premiers jours
 * (libre, sans motif) de la rupture après ce délai (limitée à : accord écrit des
 * parties, faute grave, force majeure, inaptitude médicale, exclusion définitive
 * du CFA), qualifie le motif invoqué et signale les conséquences (saisine du
 * conseil de prud'hommes, procédure de licenciement, indemnités).
 *
 * Verdict global (3 états) :
 *  - RUPTURE_REGULIERE (vert) : rupture valide.
 *  - RUPTURE_A_SECURISER (orange) : motif recevable mais formalité manquante.
 *  - RUPTURE_IRREGULIERE (rouge) : rupture irrégulière.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé NON_VALIDE /
 *    RUPTURE_IRREGULIERE ; orange A_SECURISER / RUPTURE_A_SECURISER ; vert
 *    VALIDE / RUPTURE_REGULIERE ; `<input type="date">` ; JetBrains Mono
 *    baseJuridique ; bannière gate FR ; MatSnackBar ; pas de refresh dashboard
 *    requis (calcul de qualification sans action validée).
 *  - pré-fill IA (dateDebutContrat, dateRupture, motifRupture) depuis
 *    TravailExtractedData + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (dates croisées ; SANS_MOTIF combiné à un auteur
 *    EMPLOYEUR après ouverture de motifs).
 */
@Component({
  selector: 'app-apprentissage-rupture-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './apprentissage-rupture-section.component.html',
  styleUrl: './apprentissage-rupture-section.component.scss',
})
export class ApprentissageRuptureSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-110-apprentissage-rupture';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RUPTURE APPRENTISSAGE (FR)';
  static readonly TOOL_ICON = 'school';

  static getPrefillCount(input: PrefillCountInput): number {
    return ApprentissageRupturePrefillRules.computePrefillCount(input);
  }

  /** Options du select auteur de la rupture. */
  readonly auteurOptions: ReadonlyArray<{ value: AuteurRupture; label: string }> = [
    { value: 'EMPLOYEUR', label: 'Employeur' },
    { value: 'APPRENTI', label: 'Apprenti' },
  ];

  /** Options du select motif de rupture. */
  readonly motifOptions: ReadonlyArray<{ value: MotifRupture; label: string }> = [
    { value: 'SANS_MOTIF', label: 'Sans motif (rupture libre)' },
    { value: 'ACCORD_PARTIES', label: 'Accord écrit des parties' },
    { value: 'FAUTE_GRAVE', label: 'Faute grave' },
    { value: 'FORCE_MAJEURE', label: 'Force majeure' },
    { value: 'INAPTITUDE', label: 'Inaptitude médicale' },
    { value: 'EXCLUSION_DEFINITIVE_CFA', label: 'Exclusion définitive du CFA' },
  ];

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<ApprentissageRuptureResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  dateDebutContrat = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  auteurRupture = signal<AuteurRupture>('EMPLOYEUR');
  motifRupture = signal<MotifRupture | null>(null);
  apprentiMajeur = signal<boolean>(true);

  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceMotif = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : dates croisées ;
   * SANS_MOTIF sélectionné alors que les dates suggèrent une rupture au-delà du
   * délai des 45 jours (rupture libre alors irrégulière) ; faute grave saisie
   * (rappel procédure de licenciement à sécuriser).
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const debut = this.dateDebutContrat();
    const rupture = this.dateRupture();
    if (debut && rupture && rupture < debut) {
      alerts.push(
        'La date de rupture est antérieure à la date de début du contrat : vérifiez les dates du contrat d\'apprentissage.',
      );
    }
    if (debut && rupture && rupture >= debut && this.motifRupture() === 'SANS_MOTIF') {
      const jours = this.joursDepuisDebutLocal();
      if (jours !== null && jours > 45) {
        alerts.push(
          'Une rupture « sans motif » est sélectionnée alors que plus de 45 jours se sont écoulés depuis le début du contrat : la rupture libre n\'est plus possible (art. L.6222-18), un motif valable est requis.',
        );
      }
    }
    if (this.motifRupture() === 'FAUTE_GRAVE') {
      alerts.push(
        'La rupture pour faute grave passe désormais par une procédure de licenciement (réforme 2018) : vérifiez le respect des formalités pour sécuriser la rupture.',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: ApprentissageRuptureService,
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

  /** Nombre de jours écoulés (estimation locale, basée sur les dates saisies). */
  joursDepuisDebutLocal(): number | null {
    const debut = this.dateDebutContrat();
    const rupture = this.dateRupture();
    if (!debut || !rupture) return null;
    const d1 = new Date(debut + 'T00:00:00Z').getTime();
    const d2 = new Date(rupture + 'T00:00:00Z').getTime();
    if (isNaN(d1) || isNaN(d2)) return null;
    return Math.round((d2 - d1) / (1000 * 60 * 60 * 24));
  }

  /** Dates requises + rupture ≥ début + motif sélectionné. */
  formValid(): boolean {
    if (!this.dateDebutContrat() || !this.dateRupture()) return false;
    if (!this.motifRupture()) return false;
    const jours = this.joursDepuisDebutLocal();
    if (jours !== null && jours < 0) return false;
    return true;
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutContrat.set(value || null);
    this.provenanceDateDebut.set(null);
  }

  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value || null);
    this.provenanceDateRupture.set(null);
  }

  onAuteurChange(value: AuteurRupture): void {
    this.auteurRupture.set(value);
  }

  onMotifChange(value: MotifRupture): void {
    this.motifRupture.set(value);
    this.provenanceMotif.set(null);
  }

  onApprentiMajeurChange(value: boolean): void {
    this.apprentiMajeur.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ApprentissageRuptureRequest = {
      dateDebutContrat: this.dateDebutContrat()!,
      dateRupture: this.dateRupture()!,
      auteurRupture: this.auteurRupture(),
      motifRupture: this.motifRupture()!,
      apprentiMajeur: this.apprentiMajeur(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse rupture apprentissage enregistrée', 'OK', { duration: 2500 });
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

  verdictLabel(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'Rupture régulière';
      case 'RUPTURE_A_SECURISER': return 'Rupture à sécuriser';
      case 'RUPTURE_IRREGULIERE': return 'Rupture irrégulière';
      default:                    return '';
    }
  }

  verdictChipClass(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'is-chip is-chip--success';
      case 'RUPTURE_A_SECURISER': return 'is-chip is-chip--warning';
      case 'RUPTURE_IRREGULIERE': return 'is-chip is-chip--danger';
      default:                    return 'is-chip';
    }
  }

  bannerClass(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'is-banner is-banner--success';
      case 'RUPTURE_A_SECURISER': return 'is-banner is-banner--warning';
      case 'RUPTURE_IRREGULIERE': return 'is-banner is-banner--danger';
      default:                    return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'verified';
      case 'RUPTURE_A_SECURISER': return 'shield';
      case 'RUPTURE_IRREGULIERE': return 'gavel';
      default:                    return 'info_outline';
    }
  }

  validiteLabel(v: ValiditeRupture | null | undefined): string {
    switch (v) {
      case 'VALIDE':       return 'Motif valide';
      case 'A_SECURISER':  return 'Motif à sécuriser';
      case 'NON_VALIDE':   return 'Motif non valide';
      default:             return '';
    }
  }

  validiteChipClass(v: ValiditeRupture | null | undefined): string {
    switch (v) {
      case 'VALIDE':      return 'is-chip is-chip--success';
      case 'A_SECURISER': return 'is-chip is-chip--warning';
      case 'NON_VALIDE':  return 'is-chip is-chip--danger';
      default:            return 'is-chip';
    }
  }

  periodeLabel(p: PeriodeRupture | null | undefined): string {
    switch (p) {
      case 'DANS_45_PREMIERS_JOURS': return 'Dans les 45 premiers jours';
      case 'APRES_45_JOURS':         return 'Après les 45 jours';
      default:                       return '';
    }
  }

  periodeChipClass(p: PeriodeRupture | null | undefined): string {
    switch (p) {
      case 'DANS_45_PREMIERS_JOURS': return 'is-chip is-chip--neutral';
      case 'APRES_45_JOURS':         return 'is-chip is-chip--navy';
      default:                       return 'is-chip';
    }
  }

  /** La rupture est-elle irrégulière (CPH) → encadré saisine. */
  ruptureIrreguliere(): boolean {
    return this.result()?.verdictGlobal === 'RUPTURE_IRREGULIERE';
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const debut = ApprentissageRupturePrefillRules.computeDateDebutContrat(input);
    if (debut !== null && this.dateDebutContrat() === null) {
      this.dateDebutContrat.set(debut);
      this.provenanceDateDebut.set('IA');
    }

    const rupture = ApprentissageRupturePrefillRules.computeDateRupture(input);
    if (rupture !== null && this.dateRupture() === null) {
      this.dateRupture.set(rupture);
      this.provenanceDateRupture.set('IA');
    }

    const motif = ApprentissageRupturePrefillRules.computeMotifRupture(input);
    if (motif !== null && this.motifRupture() === null) {
      this.motifRupture.set(motif);
      this.provenanceMotif.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutContrat.set(r.dateDebutContrat ?? null);
        this.dateRupture.set(r.dateRupture ?? null);
        this.auteurRupture.set(r.auteurRupture ?? 'EMPLOYEUR');
        this.motifRupture.set(r.motifRupture ?? null);
        this.apprentiMajeur.set(r.apprentiMajeur ?? true);
        this.provenanceDateDebut.set(null);
        this.provenanceDateRupture.set(null);
        this.provenanceMotif.set(null);
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

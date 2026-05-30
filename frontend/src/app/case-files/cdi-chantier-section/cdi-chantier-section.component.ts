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

import { CdiChantierService } from '../../core/services/cdi-chantier.service';
import {
  CdiChantierRequest,
  CdiChantierResponse,
  CdiChantierVerdictGlobal,
  FondementRecours,
  MotifLicenciement,
  SecteurChantier,
} from '../../core/models/cdi-chantier.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CdiChantierPrefillRules } from './cdi-chantier-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-26 — Outil décisionnel « Licenciement CDI de chantier / d'opération »
 * (F-DT-37-licenciement-cdi-chantier).
 *
 * FRANCE uniquement — applique le régime du contrat de chantier ou d'opération
 * (art. L.1223-8 et L.1223-9 CT) : vérifie la validité du recours (accord de
 * branche étendu OU usage constant — BTP / ingénierie), qualifie le motif de fin
 * de chantier (cause réelle et sérieuse, motif spécifique L.1236-8 CT) et calcule
 * l'indemnité de licenciement (art. R.1234-2 CT, sauf CCN plus favorable).
 *
 * Verdict global (3 états) :
 *  - LICENCIEMENT_FONDE (vert) : recours valide + chantier achevé.
 *  - LICENCIEMENT_A_SECURISER (orange) : motif fragile / reclassement non tracé.
 *  - RECOURS_INVALIDE (rouge) : recours invalide → requalification probable.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé RECOURS_INVALIDE /
 *    MOTIF_NON_FONDE ; orange A_SECURISER ; vert FONDE / FIN_CHANTIER_CRS ;
 *    `<input type="date">` ; JetBrains Mono indemnité + baseJuridique ; bannière
 *    gate FR ; MatSnackBar ; refresh dashboard dans next: (analyse enregistrée).
 *  - pré-fill IA (dateEntree, dateRupture, secteur) depuis TravailExtractedData +
 *    badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (dates croisées ; fondementRecours=AUCUN ;
 *    chantier non achevé).
 */
@Component({
  selector: 'app-cdi-chantier-section',
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
  templateUrl: './cdi-chantier-section.component.html',
  styleUrl: './cdi-chantier-section.component.scss',
})
export class CdiChantierSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-37-licenciement-cdi-chantier';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'LICENCIEMENT CDI DE CHANTIER (FR)';
  static readonly TOOL_ICON = 'engineering';

  static getPrefillCount(input: PrefillCountInput): number {
    return CdiChantierPrefillRules.computePrefillCount(input);
  }

  /** Options du select fondement du recours. */
  readonly fondementOptions: ReadonlyArray<{ value: FondementRecours; label: string }> = [
    { value: 'ACCORD_BRANCHE_ETENDU', label: 'Accord de branche étendu' },
    { value: 'USAGE_CONSTANT_SECTEUR', label: 'Usage constant du secteur' },
    { value: 'AUCUN', label: 'Aucun (ni accord ni usage)' },
  ];

  /** Options du select secteur. */
  readonly secteurOptions: ReadonlyArray<{ value: SecteurChantier; label: string }> = [
    { value: 'BTP', label: 'BTP' },
    { value: 'INGENIERIE', label: 'Ingénierie' },
    { value: 'AUTRE', label: 'Autre' },
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
  result = signal<CdiChantierResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  dateEntree = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  fondementRecours = signal<FondementRecours | null>(null);
  secteur = signal<SecteurChantier | null>(null);
  chantierAcheve = signal<boolean>(true);
  salaireMensuelMoyen = signal<number | null>(null);
  reclassementAutreChantierPropose = signal<boolean>(false);

  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceSecteur = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : dates croisées ;
   * fondementRecours=AUCUN (requalification probable) ; chantier non achevé
   * (motif non caractérisé).
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const entree = this.dateEntree();
    const rupture = this.dateRupture();
    if (entree && rupture && rupture < entree) {
      alerts.push(
        "La date de rupture est antérieure à la date d'entrée : vérifiez les dates du contrat.",
      );
    }
    if (this.fondementRecours() === 'AUCUN') {
      alerts.push(
        "Aucun accord de branche étendu ni usage constant n'est invoqué : le recours au CDI de chantier risque une requalification en CDI de droit commun (art. L.1223-8 CT).",
      );
    }
    if (this.fondementRecours() !== null && this.chantierAcheve() === false) {
      alerts.push(
        "Le chantier / l'opération n'est pas achevé : le motif de fin de chantier n'est pas caractérisé (art. L.1236-8 CT).",
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: CdiChantierService,
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

  /** Dates requises + rupture ≥ entrée + fondement + secteur + salaire > 0. */
  formValid(): boolean {
    if (!this.dateEntree() || !this.dateRupture()) return false;
    if (!this.fondementRecours() || !this.secteur()) return false;
    const salaire = this.salaireMensuelMoyen();
    if (salaire === null || salaire <= 0) return false;
    if (this.dateRupture()! < this.dateEntree()!) return false;
    return true;
  }

  onDateEntreeChange(value: string | null): void {
    this.dateEntree.set(value || null);
    this.provenanceDateEntree.set(null);
  }

  onDateRuptureChange(value: string | null): void {
    this.dateRupture.set(value || null);
    this.provenanceDateRupture.set(null);
  }

  onFondementChange(value: FondementRecours): void {
    this.fondementRecours.set(value);
  }

  onSecteurChange(value: SecteurChantier): void {
    this.secteur.set(value);
    this.provenanceSecteur.set(null);
  }

  onChantierAcheveChange(value: boolean): void {
    this.chantierAcheve.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelMoyen.set(value === null || isNaN(value as number) ? null : value);
  }

  onReclassementChange(value: boolean): void {
    this.reclassementAutreChantierPropose.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CdiChantierRequest = {
      dateEntree: this.dateEntree()!,
      dateRupture: this.dateRupture()!,
      fondementRecours: this.fondementRecours()!,
      secteur: this.secteur()!,
      chantierAcheve: this.chantierAcheve(),
      salaireMensuelMoyen: this.salaireMensuelMoyen()!,
      reclassementAutreChantierPropose: this.reclassementAutreChantierPropose(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse CDI de chantier enregistrée', 'OK', { duration: 2500 });
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

  verdictLabel(v: CdiChantierVerdictGlobal | null | undefined): string {
    switch (v) {
      case 'LICENCIEMENT_FONDE':       return 'Licenciement fondé';
      case 'LICENCIEMENT_A_SECURISER': return 'Licenciement à sécuriser';
      case 'RECOURS_INVALIDE':         return 'Recours invalide';
      default:                         return '';
    }
  }

  verdictChipClass(v: CdiChantierVerdictGlobal | null | undefined): string {
    switch (v) {
      case 'LICENCIEMENT_FONDE':       return 'is-chip is-chip--success';
      case 'LICENCIEMENT_A_SECURISER': return 'is-chip is-chip--warning';
      case 'RECOURS_INVALIDE':         return 'is-chip is-chip--danger';
      default:                         return 'is-chip';
    }
  }

  bannerClass(v: CdiChantierVerdictGlobal | null | undefined): string {
    switch (v) {
      case 'LICENCIEMENT_FONDE':       return 'is-banner is-banner--success';
      case 'LICENCIEMENT_A_SECURISER': return 'is-banner is-banner--warning';
      case 'RECOURS_INVALIDE':         return 'is-banner is-banner--danger';
      default:                         return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(v: CdiChantierVerdictGlobal | null | undefined): string {
    switch (v) {
      case 'LICENCIEMENT_FONDE':       return 'verified';
      case 'LICENCIEMENT_A_SECURISER': return 'shield';
      case 'RECOURS_INVALIDE':         return 'gavel';
      default:                         return 'info_outline';
    }
  }

  /** Badge validité du recours (oui vert / non rouge). */
  recoursChipClass(valide: boolean | null | undefined): string {
    return valide ? 'is-chip is-chip--success' : 'is-chip is-chip--danger';
  }

  recoursLabel(valide: boolean | null | undefined): string {
    return valide ? 'Recours valide' : 'Recours invalide';
  }

  motifLabel(m: MotifLicenciement | null | undefined): string {
    switch (m) {
      case 'FIN_CHANTIER_CRS': return 'Fin de chantier — cause réelle et sérieuse';
      case 'MOTIF_NON_FONDE':  return 'Motif non fondé';
      default:                 return '';
    }
  }

  motifChipClass(m: MotifLicenciement | null | undefined): string {
    switch (m) {
      case 'FIN_CHANTIER_CRS': return 'is-chip is-chip--success';
      case 'MOTIF_NON_FONDE':  return 'is-chip is-chip--danger';
      default:                 return 'is-chip';
    }
  }

  /** Le recours est-il invalide → encadré requalification. */
  recoursInvalide(): boolean {
    return this.result()?.recoursValide === false;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const entree = CdiChantierPrefillRules.computeDateEntree(input);
    if (entree !== null && this.dateEntree() === null) {
      this.dateEntree.set(entree);
      this.provenanceDateEntree.set('IA');
    }

    const rupture = CdiChantierPrefillRules.computeDateRupture(input);
    if (rupture !== null && this.dateRupture() === null) {
      this.dateRupture.set(rupture);
      this.provenanceDateRupture.set('IA');
    }

    const sect = CdiChantierPrefillRules.computeSecteur(input);
    if (sect !== null && this.secteur() === null) {
      this.secteur.set(sect);
      this.provenanceSecteur.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntree.set(r.dateEntree ?? null);
        this.dateRupture.set(r.dateRupture ?? null);
        this.fondementRecours.set(r.fondementRecours ?? null);
        this.secteur.set(r.secteur ?? null);
        this.chantierAcheve.set(r.chantierAcheve ?? true);
        this.salaireMensuelMoyen.set(r.salaireMensuelMoyen ?? null);
        this.reclassementAutreChantierPropose.set(r.reclassementAutreChantierPropose ?? false);
        this.provenanceDateEntree.set(null);
        this.provenanceDateRupture.set(null);
        this.provenanceSecteur.set(null);
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

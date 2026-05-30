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

import { ParticulierEmployeurCesuService } from '../../core/services/particulier-employeur-cesu.service';
import {
  CategorieEmploye,
  CauseRupture,
  EligibiliteIndemnite,
  ParticulierEmployeurCesuRequest,
  ParticulierEmployeurCesuResponse,
  ParticulierEmployeurCesuVerdict,
} from '../../core/models/particulier-employeur-cesu.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ParticulierEmployeurCesuPrefillRules } from './particulier-employeur-cesu-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-14 — Outil décisionnel « Particulier employeur (CESU) : préavis et
 * indemnité de licenciement » (F-DT-108-particulier-employeur-cesu).
 *
 * FRANCE uniquement — applique le régime conventionnel propre du salarié du
 * particulier employeur (CESU : garde d'enfants, employé de maison, assistant
 * maternel), distinct du droit commun : préavis conventionnel selon l'ancienneté
 * et indemnité de licenciement / rupture selon la CCN des salariés du particulier
 * employeur (IDCC 3239, 2021) ou la CCN des assistants maternels (indemnité de
 * rupture, formule 1/80).
 *
 * Verdict global (3 états) :
 *  - RUPTURE_REGULIERE (navy) : préavis et indemnité formalisés.
 *  - RUPTURE_A_SECURISER (or) : préavis ou indemnité à formaliser.
 *  - INDEMNITE_NON_DUE (rouge) : indemnité non due (faute grave / ancienneté < 8 mois).
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé verdict INDEMNITE_NON_DUE
 *    et badge éligibilité NON_DUE ; `<input type="date">` ; JetBrains Mono
 *    montants/baseJuridique ; bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (dateEntree, dateRupture, categorieEmploye) depuis
 *    TravailExtractedData + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (dates croisées dateEntree / dateRupture).
 */
@Component({
  selector: 'app-particulier-employeur-cesu-section',
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
  templateUrl: './particulier-employeur-cesu-section.component.html',
  styleUrl: './particulier-employeur-cesu-section.component.scss',
})
export class ParticulierEmployeurCesuSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-108-particulier-employeur-cesu';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PARTICULIER EMPLOYEUR / CESU (FR)';
  static readonly TOOL_ICON = 'home';

  static getPrefillCount(input: PrefillCountInput): number {
    return ParticulierEmployeurCesuPrefillRules.computePrefillCount(input);
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
  result = signal<ParticulierEmployeurCesuResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  dateEntree = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  categorieEmploye = signal<CategorieEmploye>('SALARIE_PARTICULIER_EMPLOYEUR');
  causeRupture = signal<CauseRupture>('LICENCIEMENT_MOTIF_PERSONNEL');
  salaireMensuelMoyen = signal<number | null>(null);

  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceCategorie = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** RETRAIT_ENFANT réservé à l'assistant maternel. */
  retraitEnfantDisabled = computed<boolean>(
    () => this.categorieEmploye() !== 'ASSISTANT_MATERNEL',
  );

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : croise les dates saisies
   * (rupture antérieure à l'entrée) et signale une incohérence avec les pièces.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const entree = this.dateEntree();
    const rupture = this.dateRupture();
    if (entree && rupture && rupture < entree) {
      alerts.push(
        "La date de rupture est antérieure à la date d'entrée : vérifiez les dates avant le calcul.",
      );
    }
    if (
      this.categorieEmploye() === 'SALARIE_PARTICULIER_EMPLOYEUR' &&
      this.causeRupture() === 'RETRAIT_ENFANT'
    ) {
      alerts.push(
        'Le retrait de l\'enfant ne concerne que l\'assistant maternel : sélectionnez une autre cause de rupture.',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: ParticulierEmployeurCesuService,
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

  /** Form valide : dates présentes et cohérentes + salaire strictement positif. */
  formValid(): boolean {
    if (!this.dateEntree() || !this.dateRupture()) return false;
    if ((this.dateRupture() as string) < (this.dateEntree() as string)) return false;
    if (this.salaireMensuelMoyen() === null || (this.salaireMensuelMoyen() ?? 0) <= 0) return false;
    // RETRAIT_ENFANT incohérent avec SALARIE_PARTICULIER_EMPLOYEUR (bloqué côté API).
    if (
      this.categorieEmploye() === 'SALARIE_PARTICULIER_EMPLOYEUR' &&
      this.causeRupture() === 'RETRAIT_ENFANT'
    ) {
      return false;
    }
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

  onCategorieChange(value: CategorieEmploye): void {
    this.categorieEmploye.set(value);
    this.provenanceCategorie.set(null);
    // Si on quitte ASSISTANT_MATERNEL alors que RETRAIT_ENFANT est sélectionné,
    // réinitialiser la cause vers une valeur cohérente.
    if (value !== 'ASSISTANT_MATERNEL' && this.causeRupture() === 'RETRAIT_ENFANT') {
      this.causeRupture.set('LICENCIEMENT_MOTIF_PERSONNEL');
    }
  }

  onCauseChange(value: CauseRupture): void {
    this.causeRupture.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelMoyen.set(value === null || value === undefined ? null : Number(value));
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ParticulierEmployeurCesuRequest = {
      dateEntree: this.dateEntree()!,
      dateRupture: this.dateRupture()!,
      categorieEmploye: this.categorieEmploye(),
      causeRupture: this.causeRupture(),
      salaireMensuelMoyen: this.salaireMensuelMoyen()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse particulier employeur / CESU enregistrée', 'OK', { duration: 2500 });
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

  categorieLabel(c: CategorieEmploye | null | undefined): string {
    switch (c) {
      case 'SALARIE_PARTICULIER_EMPLOYEUR': return 'Salarié du particulier employeur';
      case 'ASSISTANT_MATERNEL':            return 'Assistant maternel';
      default:                              return '';
    }
  }

  causeLabel(c: CauseRupture | null | undefined): string {
    switch (c) {
      case 'LICENCIEMENT_MOTIF_PERSONNEL': return 'Licenciement (motif personnel)';
      case 'RETRAIT_ENFANT':               return 'Retrait de l\'enfant';
      case 'FAUTE_GRAVE':                  return 'Faute grave';
      case 'FORCE_MAJEURE':                return 'Force majeure';
      case 'DEPART_RETRAITE':              return 'Départ à la retraite';
      default:                             return '';
    }
  }

  methodeLabel(m: ParticulierEmployeurCesuResponse['methodeCalcul'] | null | undefined): string {
    switch (m) {
      case 'CONVENTIONNEL_PE':         return 'CCN particulier employeur (1/4 mois/an, 1/3 au-delà de 10 ans)';
      case 'INDEMNITE_RUPTURE_ASSMAT': return 'Indemnité de rupture assistant maternel (1/80 des salaires nets)';
      default:                         return '';
    }
  }

  eligibiliteLabel(e: EligibiliteIndemnite | null | undefined): string {
    switch (e) {
      case 'DUE':     return 'Indemnité due';
      case 'NON_DUE': return 'Indemnité non due';
      default:        return '';
    }
  }

  eligibiliteChipClass(e: EligibiliteIndemnite | null | undefined): string {
    switch (e) {
      case 'DUE':     return 'pec-chip pec-chip--success';
      case 'NON_DUE': return 'pec-chip pec-chip--danger';
      default:        return 'pec-chip';
    }
  }

  verdictLabel(v: ParticulierEmployeurCesuVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'Rupture régulière';
      case 'RUPTURE_A_SECURISER': return 'Rupture à sécuriser';
      case 'INDEMNITE_NON_DUE':   return 'Indemnité non due';
      default:                    return '';
    }
  }

  chipClass(v: ParticulierEmployeurCesuVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'pec-chip pec-chip--navy';
      case 'RUPTURE_A_SECURISER': return 'pec-chip pec-chip--warning';
      case 'INDEMNITE_NON_DUE':   return 'pec-chip pec-chip--danger';
      default:                    return 'pec-chip';
    }
  }

  bannerClass(v: ParticulierEmployeurCesuVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'pec-banner pec-banner--navy';
      case 'RUPTURE_A_SECURISER': return 'pec-banner pec-banner--warning';
      case 'INDEMNITE_NON_DUE':   return 'pec-banner pec-banner--danger';
      default:                    return 'pec-banner';
    }
  }

  bannerIcon(v: ParticulierEmployeurCesuVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_REGULIERE':   return 'verified';
      case 'RUPTURE_A_SECURISER': return 'gpp_maybe';
      case 'INDEMNITE_NON_DUE':   return 'block';
      default:                    return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const entree = ParticulierEmployeurCesuPrefillRules.computeDateEntree(input);
    if (entree !== null && this.dateEntree() === null) {
      this.dateEntree.set(entree);
      this.provenanceDateEntree.set('IA');
    }

    const rupture = ParticulierEmployeurCesuPrefillRules.computeDateRupture(input);
    if (rupture !== null && this.dateRupture() === null) {
      this.dateRupture.set(rupture);
      this.provenanceDateRupture.set('IA');
    }

    const categorie = ParticulierEmployeurCesuPrefillRules.computeCategorieEmploye(input);
    if (categorie !== null && this.provenanceCategorie() === null) {
      this.categorieEmploye.set(categorie);
      this.provenanceCategorie.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntree.set(r.dateEntree ?? null);
        this.dateRupture.set(r.dateRupture ?? null);
        this.categorieEmploye.set(r.categorieEmploye ?? 'SALARIE_PARTICULIER_EMPLOYEUR');
        this.causeRupture.set(r.causeRupture ?? 'LICENCIEMENT_MOTIF_PERSONNEL');
        this.salaireMensuelMoyen.set(r.salaireMensuelMoyen ?? null);
        this.provenanceDateEntree.set(null);
        this.provenanceDateRupture.set(null);
        this.provenanceCategorie.set(null);
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

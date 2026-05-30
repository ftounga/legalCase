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

import { JournalisteStatutService } from '../../core/services/journaliste-statut.service';
import {
  ClauseValide,
  JournalisteStatutRequest,
  JournalisteStatutResponse,
  JournalisteStatutVerdict,
  StatutJournaliste,
  TypeRuptureJournaliste,
} from '../../core/models/journaliste-statut.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { JournalisteStatutPrefillRules } from './journaliste-statut-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-16 — Outil décisionnel « Statut du journaliste professionnel : clauses
 * spécifiques et indemnité de congédiement » (F-DT-105-journaliste-statut).
 *
 * FRANCE uniquement — applique le régime du journaliste professionnel
 * (art. L.7111-1 et s. CT) lors d'une rupture : qualification du statut
 * (présomption via carte de presse CCIJP), validité de la clause de cession
 * (cession du titre, L.7112-5 1°) ou de conscience (changement notable
 * d'orientation, L.7112-5 2°/3°), indemnité de congédiement spécifique
 * (1 mois/année, L.7112-3) et renvoi à la commission arbitrale paritaire
 * (L.7112-4) lorsque l'ancienneté dépasse 15 ans.
 *
 * Verdict global (4 états) :
 *  - RUPTURE_ASSIMILEE_LICENCIEMENT (navy) : clause valide, rupture assimilée.
 *  - INDEMNITE_DUE (navy) : indemnité de congédiement due.
 *  - COMMISSION_ARBITRALE (or) : montant fixé souverainement par la commission.
 *  - INDEMNITE_NON_DUE (rouge) : indemnité non due de droit (faute grave / démission).
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé verdict INDEMNITE_NON_DUE
 *    et badge clauseValide NON_VALIDE ; `<input type="date">` ; JetBrains Mono
 *    montant indemnité / baseJuridique ; bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (dateEntree, dateRupture, carteIdentiteProfessionnelle) depuis
 *    TravailExtractedData + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (dates croisées dateEntree / dateRupture ;
 *    clause invoquée sans fait générateur constaté).
 */
@Component({
  selector: 'app-journaliste-statut-section',
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
  templateUrl: './journaliste-statut-section.component.html',
  styleUrl: './journaliste-statut-section.component.scss',
})
export class JournalisteStatutSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-105-journaliste-statut';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'STATUT JOURNALISTE (FR)';
  static readonly TOOL_ICON = 'newspaper';

  static getPrefillCount(input: PrefillCountInput): number {
    return JournalisteStatutPrefillRules.computePrefillCount(input);
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
  result = signal<JournalisteStatutResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  dateEntree = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  typeRupture = signal<TypeRuptureJournaliste>('LICENCIEMENT');
  salaireMensuelMoyen = signal<number | null>(null);
  carteIdentiteProfessionnelle = signal<boolean>(true);
  cessionTitreConstatee = signal<boolean>(false);
  changementOrientationConstate = signal<boolean>(false);

  provenanceDateEntree = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceCarte = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : dates croisées et clause
   * invoquée sans fait générateur constaté.
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
    if (this.typeRupture() === 'CLAUSE_CESSION' && !this.cessionTitreConstatee()) {
      alerts.push(
        'Clause de cession invoquée sans cession du titre constatée : la clause sera jugée non valide (art. L.7112-5 1°).',
      );
    }
    if (this.typeRupture() === 'CLAUSE_CONSCIENCE' && !this.changementOrientationConstate()) {
      alerts.push(
        "Clause de conscience invoquée sans changement notable d'orientation constaté : la clause sera jugée non valide (art. L.7112-5 2°/3°).",
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: JournalisteStatutService,
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

  onTypeRuptureChange(value: TypeRuptureJournaliste): void {
    this.typeRupture.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelMoyen.set(value === null || value === undefined ? null : Number(value));
  }

  onCarteChange(value: boolean): void {
    this.carteIdentiteProfessionnelle.set(value);
    this.provenanceCarte.set(null);
  }

  onCessionChange(value: boolean): void {
    this.cessionTitreConstatee.set(value);
  }

  onChangementOrientationChange(value: boolean): void {
    this.changementOrientationConstate.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: JournalisteStatutRequest = {
      dateEntree: this.dateEntree()!,
      dateRupture: this.dateRupture()!,
      typeRupture: this.typeRupture(),
      salaireMensuelMoyen: this.salaireMensuelMoyen()!,
      carteIdentiteProfessionnelle: this.carteIdentiteProfessionnelle(),
      cessionTitreConstatee: this.cessionTitreConstatee(),
      changementOrientationConstate: this.changementOrientationConstate(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse statut journaliste enregistrée', 'OK', { duration: 2500 });
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

  typeRuptureLabel(t: TypeRuptureJournaliste | null | undefined): string {
    switch (t) {
      case 'LICENCIEMENT':      return 'Licenciement';
      case 'CLAUSE_CESSION':    return 'Clause de cession (cession du titre)';
      case 'CLAUSE_CONSCIENCE': return "Clause de conscience (changement d'orientation)";
      case 'DEMISSION':         return 'Démission';
      case 'FAUTE_GRAVE':       return 'Faute grave';
      default:                  return '';
    }
  }

  statutLabel(s: StatutJournaliste | null | undefined): string {
    switch (s) {
      case 'CONFIRME':     return 'Journaliste professionnel confirmé';
      case 'A_QUALIFIER':  return 'Statut à qualifier';
      default:             return '';
    }
  }

  statutChipClass(s: StatutJournaliste | null | undefined): string {
    switch (s) {
      case 'CONFIRME':    return 'js-chip js-chip--success';
      case 'A_QUALIFIER': return 'js-chip js-chip--warning';
      default:            return 'js-chip';
    }
  }

  clauseLabel(c: ClauseValide | null | undefined): string {
    switch (c) {
      case 'VALIDE':     return 'Clause valide';
      case 'NON_VALIDE': return 'Clause non valide';
      case 'SANS_OBJET': return 'Sans objet';
      default:           return '';
    }
  }

  clauseChipClass(c: ClauseValide | null | undefined): string {
    switch (c) {
      case 'VALIDE':     return 'js-chip js-chip--success';
      case 'NON_VALIDE': return 'js-chip js-chip--danger';
      case 'SANS_OBJET': return 'js-chip js-chip--neutral';
      default:           return 'js-chip';
    }
  }

  verdictLabel(v: JournalisteStatutVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_ASSIMILEE_LICENCIEMENT': return 'Rupture assimilée à un licenciement';
      case 'INDEMNITE_DUE':                  return 'Indemnité de congédiement due';
      case 'COMMISSION_ARBITRALE':           return 'Commission arbitrale';
      case 'INDEMNITE_NON_DUE':              return 'Indemnité non due';
      default:                               return '';
    }
  }

  chipClass(v: JournalisteStatutVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_ASSIMILEE_LICENCIEMENT': return 'js-chip js-chip--navy';
      case 'INDEMNITE_DUE':                  return 'js-chip js-chip--navy';
      case 'COMMISSION_ARBITRALE':           return 'js-chip js-chip--warning';
      case 'INDEMNITE_NON_DUE':              return 'js-chip js-chip--danger';
      default:                               return 'js-chip';
    }
  }

  bannerClass(v: JournalisteStatutVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_ASSIMILEE_LICENCIEMENT': return 'js-banner js-banner--navy';
      case 'INDEMNITE_DUE':                  return 'js-banner js-banner--navy';
      case 'COMMISSION_ARBITRALE':           return 'js-banner js-banner--warning';
      case 'INDEMNITE_NON_DUE':              return 'js-banner js-banner--danger';
      default:                               return 'js-banner';
    }
  }

  bannerIcon(v: JournalisteStatutVerdict | null | undefined): string {
    switch (v) {
      case 'RUPTURE_ASSIMILEE_LICENCIEMENT': return 'verified';
      case 'INDEMNITE_DUE':                  return 'verified';
      case 'COMMISSION_ARBITRALE':           return 'gavel';
      case 'INDEMNITE_NON_DUE':              return 'block';
      default:                               return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const entree = JournalisteStatutPrefillRules.computeDateEntree(input);
    if (entree !== null && this.dateEntree() === null) {
      this.dateEntree.set(entree);
      this.provenanceDateEntree.set('IA');
    }

    const rupture = JournalisteStatutPrefillRules.computeDateRupture(input);
    if (rupture !== null && this.dateRupture() === null) {
      this.dateRupture.set(rupture);
      this.provenanceDateRupture.set('IA');
    }

    const carte = JournalisteStatutPrefillRules.computeCarteIdentiteProfessionnelle(input);
    if (carte !== null && this.provenanceCarte() === null) {
      this.carteIdentiteProfessionnelle.set(carte);
      this.provenanceCarte.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntree.set(r.dateEntree ?? null);
        this.dateRupture.set(r.dateRupture ?? null);
        this.typeRupture.set(r.typeRupture ?? 'LICENCIEMENT');
        this.salaireMensuelMoyen.set(r.salaireMensuelMoyen ?? null);
        this.carteIdentiteProfessionnelle.set(r.carteIdentiteProfessionnelle ?? true);
        this.cessionTitreConstatee.set(r.cessionTitreConstatee ?? false);
        this.changementOrientationConstate.set(r.changementOrientationConstate ?? false);
        this.provenanceDateEntree.set(null);
        this.provenanceDateRupture.set(null);
        this.provenanceCarte.set(null);
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

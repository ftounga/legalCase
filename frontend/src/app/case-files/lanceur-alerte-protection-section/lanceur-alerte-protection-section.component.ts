import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LanceurAlerteProtectionService } from '../../core/services/lanceur-alerte-protection.service';
import {
  LanceurAlerteAnalyse,
  LanceurAlerteNatureMesure,
  LanceurAlerteNatureSignalement,
  LanceurAlertePointAnalyse,
  LanceurAlerteProcedure,
  LanceurAlerteProtectionRequest,
  LanceurAlerteProtectionResponse,
} from '../../core/models/lanceur-alerte-protection.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { LanceurAlerteProtectionSectionPrefillRules } from './lanceur-alerte-protection-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-212-26 : composant Angular standalone pour l'outil décisionnel
 * "Protection du lanceur d'alerte" — tool_id canonique
 * `F-DT-61-lanceur-alerte-protection`. FRANCE uniquement (loi Waserman
 * n° 2022-401 du 21/03/2022 — la BE transpose la même directive UE
 * 2019/1937 par sa loi du 28/11/2022 avec un régime distinct).
 *
 * Pattern de référence : `faute-inexcusable-fr-section` (F-DT-91,
 * SF-212-10) — formulaire 6 champs, POST de calcul, verdict 3 niveaux,
 * refresh dashboard, OnPush + markForCheck dans les subscribe.
 *
 * Verdict 3 niveaux :
 *  - PROTECTION_FORTE (vert — qualité de lanceur d'alerte caractérisée + procédure conforme)
 *  - PROTECTION_PARTIELLE (or — qualité reconnue mais procédure fragile, risque de requalification)
 *  - HORS_CHAMP (rouge — contrepartie financière ou nature non protégée)
 *
 * Bannière FR-only : si le workspace n'est pas en FRANCE, l'écran
 * affiche uniquement un avertissement pédagogique (régime belge distinct).
 */
@Component({
  selector: 'app-lanceur-alerte-protection-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './lanceur-alerte-protection-section.component.html',
  styleUrl: './lanceur-alerte-protection-section.component.scss',
})
export class LanceurAlerteProtectionSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-61-lanceur-alerte-protection';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PROTECTION DU LANCEUR D’ALERTE';
  static readonly TOOL_ICON = 'campaign';

  /**
   * SF-244 — délégué au helper partagé (parité stricte avec `prefillFromAi()`).
   * Retourne le nombre exact de champs pré-remplissables FR pour le badge tab.
   * Couvre les 4 champs `lanceurAlerte*`.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return LanceurAlerteProtectionSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LanceurAlerteProtectionResponse | null>(null);

  // --- Form fields (6 champs du contrat backend) ---
  natureSignalement = signal<LanceurAlerteNatureSignalement>('CRIME_DELIT');
  contrepartieFinanciere = signal<boolean>(false);
  procedureSignalement = signal<LanceurAlerteProcedure>('INTERNE');
  referentInterneSaisi = signal<boolean>(false);
  mesureRepresailleDetectee = signal<boolean>(false);
  natureMesureRepresaille = signal<LanceurAlerteNatureMesure>('AUCUNE');

  // Provenance IA par champ pré-rempli — badge `auto_awesome` s'efface au
  // premier `onXxxChange()` manuel sur ce champ.
  provenanceNatureSignalement = signal<'IA' | null>(null);
  provenanceProcedure = signal<'IA' | null>(null);
  provenanceMesureRepresaille = signal<'IA' | null>(null);
  provenanceNatureMesure = signal<'IA' | null>(null);

  constructor(
    private service: LanceurAlerteProtectionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA (SF-212-26) — renseigne les 4 champs depuis le sous-objet
   * `lanceur_alerte_detail` (projeté à plat dans `travailExtractedData`).
   * Parité stricte avec `getPrefillCount()` : mêmes mappings, même gate
   * `workspaceCountry === 'FRANCE'`. No-op gracieux si `aiData` absent ou
   * dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;
    const rules = LanceurAlerteProtectionSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const nature = rules.computeNatureSignalement(ruleInput);
    if (nature !== null) {
      this.natureSignalement.set(nature);
      this.provenanceNatureSignalement.set('IA');
    }
    const procedure = rules.computeProcedure(ruleInput);
    if (procedure !== null) {
      this.procedureSignalement.set(procedure);
      this.provenanceProcedure.set('IA');
    }
    const mesure = rules.computeMesureRepresaille(ruleInput);
    if (mesure !== null) {
      this.mesureRepresailleDetectee.set(mesure);
      this.provenanceMesureRepresaille.set('IA');
    }
    const natureMesure = rules.computeNatureMesure(ruleInput);
    if (natureMesure !== null) {
      this.natureMesureRepresaille.set(natureMesure);
      this.provenanceNatureMesure.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FRANCE + cohérence mesure × nature. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    // Si mesure de représailles signalée, nature doit être différente d'AUCUNE.
    if (this.mesureRepresailleDetectee() && this.natureMesureRepresaille() === 'AUCUNE') {
      return false;
    }
    // Si pas de mesure, la nature doit valoir AUCUNE.
    if (!this.mesureRepresailleDetectee() && this.natureMesureRepresaille() !== 'AUCUNE') {
      return false;
    }
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA du champ ---

  onNatureSignalementChange(value: LanceurAlerteNatureSignalement): void {
    this.natureSignalement.set(value);
    this.provenanceNatureSignalement.set(null);
  }

  onContrepartieChange(value: boolean): void {
    this.contrepartieFinanciere.set(value);
  }

  onProcedureChange(value: LanceurAlerteProcedure): void {
    this.procedureSignalement.set(value);
    this.provenanceProcedure.set(null);
    // Si procédure n'est plus INTERNE, ne plus exiger referentInterneSaisi.
    if (value !== 'INTERNE') {
      this.referentInterneSaisi.set(false);
    }
  }

  onReferentSaisiChange(value: boolean): void {
    this.referentInterneSaisi.set(value);
  }

  onMesureRepresailleChange(value: boolean): void {
    this.mesureRepresailleDetectee.set(value);
    this.provenanceMesureRepresaille.set(null);
    // Synchroniser natureMesureRepresaille.
    if (!value) {
      this.natureMesureRepresaille.set('AUCUNE');
      this.provenanceNatureMesure.set(null);
    } else if (this.natureMesureRepresaille() === 'AUCUNE') {
      this.natureMesureRepresaille.set('LICENCIEMENT');
    }
  }

  onNatureMesureChange(value: LanceurAlerteNatureMesure): void {
    this.natureMesureRepresaille.set(value);
    this.provenanceNatureMesure.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: LanceurAlerteProtectionRequest = {
      natureSignalement: this.natureSignalement(),
      contrepartieFinanciere: this.contrepartieFinanciere(),
      procedureSignalement: this.procedureSignalement(),
      referentInterneSaisi: this.procedureSignalement() === 'INTERNE'
          ? this.referentInterneSaisi()
          : null,
      mesureRepresailleDetectee: this.mesureRepresailleDetectee(),
      natureMesureRepresaille: this.natureMesureRepresaille(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Évaluation de la protection lanceur d\'alerte calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        if (r) {
          this.applyResult(r);
        }
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: LanceurAlerteProtectionResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /**
   * Ré-injecte le snapshot d'inputs de la réponse dans les champs du
   * formulaire — sans quoi un clic « Modifier » repartirait des valeurs
   * par défaut au lieu des dernières valeurs saisies.
   */
  private hydrateForm(r: LanceurAlerteProtectionResponse): void {
    this.natureSignalement.set(r.natureSignalement);
    this.contrepartieFinanciere.set(r.contrepartieFinanciere ?? false);
    this.procedureSignalement.set(r.procedureSignalement);
    this.referentInterneSaisi.set(r.referentInterneSaisi ?? false);
    this.mesureRepresailleDetectee.set(r.mesureRepresailleDetectee ?? false);
    this.natureMesureRepresaille.set(r.natureMesureRepresaille);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceNatureSignalement.set(null);
    this.provenanceProcedure.set(null);
    this.provenanceMesureRepresaille.set(null);
    this.provenanceNatureMesure.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Verdict → bannière de couleur. 3 niveaux :
   *  - PROTECTION_FORTE → vert
   *  - PROTECTION_PARTIELLE → or
   *  - HORS_CHAMP → rouge
   */
  verdictBannerClass(v: LanceurAlerteAnalyse): string {
    switch (v) {
      case 'PROTECTION_FORTE':
        return 'lap-verdict-banner lap-verdict-banner--forte';
      case 'PROTECTION_PARTIELLE':
        return 'lap-verdict-banner lap-verdict-banner--partielle';
      case 'HORS_CHAMP':
        return 'lap-verdict-banner lap-verdict-banner--hors-champ';
    }
  }

  verdictBannerLabel(v: LanceurAlerteAnalyse): string {
    switch (v) {
      case 'PROTECTION_FORTE': return 'Protection forte';
      case 'PROTECTION_PARTIELLE': return 'Protection partielle';
      case 'HORS_CHAMP': return 'Hors champ';
    }
  }

  verdictBannerIcon(v: LanceurAlerteAnalyse): string {
    switch (v) {
      case 'PROTECTION_FORTE': return 'verified_user';
      case 'PROTECTION_PARTIELLE': return 'help_outline';
      case 'HORS_CHAMP': return 'cancel';
    }
  }

  /** Libellé court d'un point d'analyse (utilisé pour le tracking dans la liste). */
  pointTrackKey(_index: number, p: LanceurAlertePointAnalyse): string {
    return `${p.code}:${p.libelle}`;
  }

  /** Format euros locale FR. */
  formatEuros(v: number | null | undefined): string {
    if (v === null || v === undefined || !Number.isFinite(v)) return '—';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(v);
  }
}

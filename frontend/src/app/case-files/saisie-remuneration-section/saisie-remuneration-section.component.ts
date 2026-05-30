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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SaisieRemunerationService } from '../../core/services/saisie-remuneration.service';
import {
  SaisieRemunerationRequest,
  SaisieRemunerationResponse,
  SaisieRemunerationVerdict,
} from '../../core/models/saisie-remuneration.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { SaisieRemunerationPrefillRules } from './saisie-remuneration-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-08 — Outil décisionnel « Saisie sur rémunération (quotité saisissable) »
 * (F-DT-89-saisie-arret-remuneration).
 *
 * FRANCE uniquement — calcul de la quotité mensuelle saisissable d'une
 * rémunération selon le barème annuel par tranches (R. 3252-2 Code travail),
 * avec correction pour personnes à charge (R. 3252-3) et fraction absolument
 * insaisissable (montant forfaitaire RSA, L. 3252-3). Accompagne l'avocat côté
 * créancier (recouvrer une créance sur le salaire d'un débiteur) ou côté salarié
 * saisi. Outil frère post-jugement de l'exécution CPH (F-DT-88, SF-218-04).
 *
 * Verdict d'orientation (3 états) :
 *  - SAISISSABLE (navy) : quotité saisissable > 0 — saisie possible.
 *  - INSAISISSABLE (or) : rémunération ≤ fraction insaisissable RSA — rien à saisir.
 *  - ALIMENTAIRE_PAIEMENT_DIRECT (navy) : créance alimentaire → paiement direct
 *    (loi 1973) hors barème classique.
 *
 * Le résultat affiche la quotité saisissable mensuelle, le montant laissé au
 * salarié et le nombre de mois de recouvrement (JetBrains Mono), le détail par
 * tranche (tableau), la fraction insaisissable et la mention « barème R. 3252-2
 * à actualiser annuellement ».
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; navy SAISISSABLE et
 *    ALIMENTAIRE_PAIEMENT_DIRECT, or INSAISISSABLE ; rouge non utilisé (info).
 *  - pré-fill IA (remunerationNetteMensuelle depuis salaireBrutMensuel — proxy
 *    brut→net signalé par note de provenance ; nombrePersonnesACharge) depuis
 *    TravailExtractedData
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *  - F-IA-03 : coherenceAlerts croise remunerationNetteMensuelle / salaireBrutMensuel.
 */
@Component({
  selector: 'app-saisie-remuneration-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './saisie-remuneration-section.component.html',
  styleUrl: './saisie-remuneration-section.component.scss',
})
export class SaisieRemunerationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-89-saisie-arret-remuneration';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'SAISIE SUR RÉMUNÉRATION (FR)';
  static readonly TOOL_ICON = 'savings';

  static getPrefillCount(input: PrefillCountInput): number {
    return SaisieRemunerationPrefillRules.computePrefillCount(input);
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
  result = signal<SaisieRemunerationResponse | null>(null);

  remunerationNetteMensuelle = signal<number | null>(null);
  nombrePersonnesACharge = signal<number>(0);
  creanceTotale = signal<number | null>(null);
  creanceAlimentaire = signal<boolean>(false);

  provenanceRemuneration = signal<'IA' | null>(null);
  provenanceNbPersonnesACharge = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes). Signale que la rémunération
   * nette saisie provient du salaire BRUT détecté par l'IA (la conversion
   * brut → net n'est pas garantie : l'assiette de la quotité doit être nette).
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const brut = this.aiData?.salaireBrutMensuel;
    if (
      this.provenanceRemuneration() === 'IA' &&
      typeof brut === 'number' &&
      brut > 0 &&
      this.remunerationNetteMensuelle() === brut
    ) {
      alerts.push(
        "La rémunération a été pré-remplie depuis le salaire BRUT détecté : l'assiette de la quotité saisissable doit être la rémunération NETTE — ajustez si nécessaire.",
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: SaisieRemunerationService,
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

  /** Form valide : rémunération nette + créance totale strictement positives. */
  formValid(): boolean {
    if (this.remunerationNetteMensuelle() === null || (this.remunerationNetteMensuelle() ?? 0) <= 0) return false;
    if (this.creanceTotale() === null || (this.creanceTotale() ?? 0) <= 0) return false;
    if ((this.nombrePersonnesACharge() ?? 0) < 0) return false;
    return true;
  }

  onRemunerationChange(value: number | null): void {
    this.remunerationNetteMensuelle.set(value === null || value === undefined ? null : Number(value));
    this.provenanceRemuneration.set(null);
  }

  onNombrePersonnesAChargeChange(value: number | null): void {
    const n = value === null || value === undefined ? 0 : Math.max(0, Math.trunc(Number(value)));
    this.nombrePersonnesACharge.set(n);
    this.provenanceNbPersonnesACharge.set(null);
  }

  onCreanceTotaleChange(value: number | null): void {
    this.creanceTotale.set(value === null || value === undefined ? null : Number(value));
  }

  onCreanceAlimentaireChange(value: boolean): void {
    this.creanceAlimentaire.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: SaisieRemunerationRequest = {
      remunerationNetteMensuelle: this.remunerationNetteMensuelle()!,
      nombrePersonnesACharge: this.nombrePersonnesACharge() ?? 0,
      creanceTotale: this.creanceTotale()!,
      creanceAlimentaire: this.creanceAlimentaire(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse saisie sur rémunération enregistrée', 'OK', { duration: 2500 });
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

  verdictLabel(verdict: SaisieRemunerationVerdict | null | undefined): string {
    switch (verdict) {
      case 'SAISISSABLE':                  return 'Rémunération saisissable';
      case 'INSAISISSABLE':                return 'Rémunération insaisissable';
      case 'ALIMENTAIRE_PAIEMENT_DIRECT':  return 'Créance alimentaire — paiement direct';
      default:                             return '';
    }
  }

  /** Classe du chip verdict (header). */
  chipClass(verdict: SaisieRemunerationVerdict | null | undefined): string {
    switch (verdict) {
      case 'SAISISSABLE':                  return 'srm-chip srm-chip--navy';
      case 'INSAISISSABLE':                return 'srm-chip srm-chip--warning';
      case 'ALIMENTAIRE_PAIEMENT_DIRECT':  return 'srm-chip srm-chip--navy';
      default:                             return 'srm-chip';
    }
  }

  bannerClass(verdict: SaisieRemunerationVerdict | null | undefined): string {
    switch (verdict) {
      case 'SAISISSABLE':                  return 'srm-banner srm-banner--navy';
      case 'INSAISISSABLE':                return 'srm-banner srm-banner--warning';
      case 'ALIMENTAIRE_PAIEMENT_DIRECT':  return 'srm-banner srm-banner--navy';
      default:                             return 'srm-banner';
    }
  }

  bannerIcon(verdict: SaisieRemunerationVerdict | null | undefined): string {
    switch (verdict) {
      case 'SAISISSABLE':                  return 'savings';
      case 'INSAISISSABLE':                return 'shield';
      case 'ALIMENTAIRE_PAIEMENT_DIRECT':  return 'family_restroom';
      default:                             return 'info_outline';
    }
  }

  /** Le détail par tranche n'a de sens que pour une saisie classique avec tranches. */
  showTranches(r: SaisieRemunerationResponse | null | undefined): boolean {
    return !!r && Array.isArray(r.tranches) && r.tranches.length > 0;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const remuneration = SaisieRemunerationPrefillRules.computeRemunerationNetteMensuelle(input);
    if (remuneration !== null && this.remunerationNetteMensuelle() === null) {
      this.remunerationNetteMensuelle.set(remuneration);
      this.provenanceRemuneration.set('IA');
    }

    const nbPersonnes = SaisieRemunerationPrefillRules.computeNombrePersonnesACharge(input);
    if (nbPersonnes !== null && this.nombrePersonnesACharge() === 0 && this.provenanceNbPersonnesACharge() === null) {
      this.nombrePersonnesACharge.set(nbPersonnes);
      this.provenanceNbPersonnesACharge.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.remunerationNetteMensuelle.set(r.remunerationNetteMensuelle ?? null);
        this.nombrePersonnesACharge.set(r.nombrePersonnesACharge ?? 0);
        this.creanceTotale.set(r.creanceTotale ?? null);
        this.creanceAlimentaire.set(r.creanceAlimentaire ?? false);
        this.provenanceRemuneration.set(null);
        this.provenanceNbPersonnesACharge.set(null);
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

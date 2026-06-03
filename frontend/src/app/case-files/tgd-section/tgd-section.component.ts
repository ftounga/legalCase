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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TgdService } from '../../core/services/tgd.service';
import {
  TgdRequest,
  TgdResponse,
  VerdictTgd,
} from '../../core/models/tgd.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TgdPrefillRules } from './tgd-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-222-02 : composant Angular standalone pour l'outil décisionnel "Téléphone
 * Grave Danger — éligibilité" (F-FA-TGD).
 *
 * FRANCE uniquement (art. 41-3-1 CPP — dispositif de téléprotection des victimes
 * de violences). L'outil CONSEILLE l'avocat sur l'éligibilité ; l'attribution
 * relève du procureur de la République.
 *
 * Pattern de référence : `asf-caf-section` (SF-222-01).
 *
 * Anti-doublon F-FA-14 (ordonnance de protection) : analyzer d'éligibilité au
 * dispositif TGD, distinct de la MESURE TGD recommandée par l'ordonnance de
 * protection. En cas d'interdiction de contact manquante, le résultat renvoie
 * vers F-FA-14.
 *
 * Formulaire (5 critères booléens du contrat backend SF-222-02) :
 *  - dangerGrave (checkbox)
 *  - violencesAvereesOuVraisemblables (checkbox)
 *  - interdictionContactProcedure (checkbox)
 *  - nonCohabitation (checkbox)
 *  - consentementVictime (checkbox)
 *
 * Verdict affiché : verdict (3 niveaux) + liste des critères manquants + bases
 * juridiques + messages (dont la mention « décision = procureur »).
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-tgd-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './tgd-section.component.html',
  styleUrl: './tgd-section.component.scss',
})
export class TgdSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-TGD';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'TGD TELEPHONE GRAVE DANGER';
  static readonly TOOL_ICON = 'phone_in_talk';

  /**
   * SF-222-02 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de critères pré-remplissables FR (max 5).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return TgdPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<TgdResponse | null>(null);

  // --- Form fields (5 critères du contrat backend SF-222-02) ---
  dangerGrave = signal<boolean>(false);
  violencesAvereesOuVraisemblables = signal<boolean>(false);
  interdictionContactProcedure = signal<boolean>(false);
  nonCohabitation = signal<boolean>(false);
  consentementVictime = signal<boolean>(false);

  // Provenance IA par critère (badge dans l'UI tant que valeur non modifiée).
  provenanceDangerGrave = signal<'IA' | null>(null);
  provenanceViolences = signal<'IA' | null>(null);
  provenanceInterdiction = signal<'IA' | null>(null);
  provenanceNonCohabitation = signal<'IA' | null>(null);
  provenanceConsentement = signal<'IA' | null>(null);

  constructor(
    private service: TgdService,
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
   * Pré-fill IA SF-222-02 — renseigne les 5 critères détectables depuis
   * `familleExtractedData`. No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = TgdPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const danger = rules.computeDangerGrave(ruleInput);
    if (danger !== null) {
      this.dangerGrave.set(danger);
      this.provenanceDangerGrave.set('IA');
    }
    const violences = rules.computeViolences(ruleInput);
    if (violences !== null) {
      this.violencesAvereesOuVraisemblables.set(violences);
      this.provenanceViolences.set('IA');
    }
    const interdiction = rules.computeInterdictionContact(ruleInput);
    if (interdiction !== null) {
      this.interdictionContactProcedure.set(interdiction);
      this.provenanceInterdiction.set('IA');
    }
    const nonCohab = rules.computeNonCohabitation(ruleInput);
    if (nonCohab !== null) {
      this.nonCohabitation.set(nonCohab);
      this.provenanceNonCohabitation.set('IA');
    }
    const consentement = rules.computeConsentement(ruleInput);
    if (consentement !== null) {
      this.consentementVictime.set(consentement);
      this.provenanceConsentement.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Form valide : FR (les 5 critères sont des booléens, toujours saisissables). */
  formValid(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onDangerGraveChange(checked: boolean): void {
    this.dangerGrave.set(checked);
    this.provenanceDangerGrave.set(null);
  }

  onViolencesChange(checked: boolean): void {
    this.violencesAvereesOuVraisemblables.set(checked);
    this.provenanceViolences.set(null);
  }

  onInterdictionChange(checked: boolean): void {
    this.interdictionContactProcedure.set(checked);
    this.provenanceInterdiction.set(null);
  }

  onNonCohabitationChange(checked: boolean): void {
    this.nonCohabitation.set(checked);
    this.provenanceNonCohabitation.set(null);
  }

  onConsentementChange(checked: boolean): void {
    this.consentementVictime.set(checked);
    this.provenanceConsentement.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: TgdRequest = {
      dangerGrave: this.dangerGrave(),
      violencesAvereesOuVraisemblables: this.violencesAvereesOuVraisemblables(),
      interdictionContactProcedure: this.interdictionContactProcedure(),
      nonCohabitation: this.nonCohabitation(),
      consentementVictime: this.consentementVictime(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Éligibilité TGD évaluée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: TgdResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Libellé humain pour le verdict. */
  verdictLabel(verdict: VerdictTgd): string {
    const labels: Record<VerdictTgd, string> = {
      ELIGIBLE_TGD: 'Éligible au TGD',
      ELIGIBLE_SOUS_RESERVE: 'Éligible sous réserve',
      NON_ELIGIBLE: 'Non éligible en l\'état',
    };
    return labels[verdict];
  }

  /** Classe CSS par verdict pour la pastille. */
  verdictClass(verdict: VerdictTgd): string {
    const cls: Record<VerdictTgd, string> = {
      ELIGIBLE_TGD: 'verdict-success',
      ELIGIBLE_SOUS_RESERVE: 'verdict-info',
      NON_ELIGIBLE: 'verdict-warning',
    };
    return cls[verdict];
  }
}

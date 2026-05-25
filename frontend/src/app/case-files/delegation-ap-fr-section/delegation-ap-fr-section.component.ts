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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DelegationApFrService } from '../../core/services/delegation-ap-fr.service';
import {
  DelegationApFrRequest,
  DelegationApFrResponse,
  TiersLienFamilial,
  TypeDelegationAp,
  VerdictRecevabiliteDelegationAp,
} from '../../core/models/delegation-ap-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DelegationApFrPrefillRules } from './delegation-ap-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-10 : composant Angular standalone pour l'outil décisionnel
 * "Délégation d'autorité parentale" (F-FA-XX-delegation-ap).
 *
 * FRANCE uniquement (art. 376-1 Cciv + art. 371-1 Cciv + art. L. 228-1 CASF
 * + Cass. 1ère civ., 22/11/2005).
 *
 * Pattern de référence : `aripa-recouvrement-fr-section` (SF-216-08) et
 * `liquidation-communaute-fr-section` (SF-216-06).
 *
 * Formulaire (8 champs) :
 *  - typeDelegation (3 voies : VOLONTAIRE_CONJOINTE | JUDICIAIRE_DESINTERET | JUDICIAIRE_TIERS)
 *  - tiersLienFamilial (5 valeurs)
 *  - accordParents (checkbox, pré-coché si détecté)
 *  - motivationDelegation (textarea libre)
 *  - ageEnfant (0-18 ans, pré-rempli depuis filiation_detection_v2)
 *  - dangerCaracterise (checkbox)
 *  - decisionsJudiciairesPrecedentes (checkbox)
 *  - desinteretParentPlusUnAn (checkbox)
 *
 * Verdict affiché : verdictRecevabilite, voieProcedurale, etapes (stepper),
 * dureeEstimeeJours, baseLegale, messages, alertes.
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-delegation-ap-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule, MatTooltipModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './delegation-ap-fr-section.component.html',
  styleUrl: './delegation-ap-fr-section.component.scss',
})
export class DelegationApFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-XX-delegation-ap';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'DELEGATION AUTORITE PARENTALE';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-216-10 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (max 3 : âge enfant,
   * lien tiers, accord parents).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return DelegationApFrPrefillRules.computePrefillCount({
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
  result = signal<DelegationApFrResponse | null>(null);

  // --- Form fields (8 champs du contrat backend SF-216-09) ---
  typeDelegation = signal<TypeDelegationAp | null>(null);
  tiersLienFamilial = signal<TiersLienFamilial | null>(null);
  accordParents = signal<boolean>(false);
  motivationDelegation = signal<string>('');
  ageEnfant = signal<number | null>(null);
  dangerCaracterise = signal<boolean>(false);
  decisionsJudiciairesPrecedentes = signal<boolean>(false);
  desinteretParentPlusUnAn = signal<boolean>(false);

  // Provenance IA par champ (badge dans l'UI tant que valeur non modifiée).
  provenanceAgeEnfant = signal<'IA' | null>(null);
  provenanceTiersLien = signal<'IA' | null>(null);
  provenanceAccordParents = signal<'IA' | null>(null);

  /** Options select type de délégation. */
  readonly typesDelegation: ReadonlyArray<{ value: TypeDelegationAp; label: string }> = [
    { value: 'VOLONTAIRE_CONJOINTE', label: 'Volontaire conjointe (accord des parents)' },
    { value: 'JUDICIAIRE_DESINTERET', label: 'Judiciaire — désintérêt parental (> 1 an)' },
    { value: 'JUDICIAIRE_TIERS', label: 'Judiciaire — requête du tiers' },
  ];

  /** Options select lien familial tiers. */
  readonly tiersLiens: ReadonlyArray<{ value: TiersLienFamilial; label: string }> = [
    { value: 'GRANDS_PARENTS', label: 'Grand(s)-parent(s)' },
    { value: 'ONCLE_TANTE', label: 'Oncle / tante' },
    { value: 'FAMILLE_ELARGIE', label: 'Famille élargie' },
    { value: 'ASSOCIATION_HABILITEE', label: 'Association habilitée' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  constructor(
    private service: DelegationApFrService,
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
   * Pré-fill IA SF-216-10 — renseigne les 3 champs détectables depuis
   * `familleExtractedData` (âge enfant, lien tiers, accord parents).
   * No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = DelegationApFrPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const age = rules.computeAgeEnfant(ruleInput);
    if (age !== null) {
      this.ageEnfant.set(age);
      this.provenanceAgeEnfant.set('IA');
    }
    const tiers = rules.computeTiersLienFamilial(ruleInput);
    if (tiers !== null) {
      this.tiersLienFamilial.set(tiers);
      this.provenanceTiersLien.set('IA');
    }
    const accord = rules.computeAccordParents(ruleInput);
    if (accord !== null) {
      this.accordParents.set(accord);
      this.provenanceAccordParents.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + type de délégation + tiers + âge enfant renseignés.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.typeDelegation() === null) return false;
    if (this.tiersLienFamilial() === null) return false;
    const age = this.ageEnfant();
    if (age === null || !Number.isFinite(age) || age < 0 || age > 18) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onTypeDelegationChange(value: TypeDelegationAp | null): void {
    this.typeDelegation.set(value);
  }

  onTiersLienChange(value: TiersLienFamilial | null): void {
    this.tiersLienFamilial.set(value);
    this.provenanceTiersLien.set(null);
  }

  onAccordParentsChange(checked: boolean): void {
    this.accordParents.set(checked);
    this.provenanceAccordParents.set(null);
  }

  onMotivationChange(value: string): void {
    this.motivationDelegation.set(value ?? '');
  }

  onAgeEnfantChange(value: number | string | null): void {
    this.ageEnfant.set(this.parseAge(value));
    this.provenanceAgeEnfant.set(null);
  }

  onDangerChange(checked: boolean): void {
    this.dangerCaracterise.set(checked);
  }

  onDecisionsPrecChange(checked: boolean): void {
    this.decisionsJudiciairesPrecedentes.set(checked);
  }

  onDesinteretChange(checked: boolean): void {
    this.desinteretParentPlusUnAn.set(checked);
  }

  private parseAge(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0 || num > 18) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const motivation = this.motivationDelegation().trim();
    const request: DelegationApFrRequest = {
      typeDelegation: this.typeDelegation(),
      tiersLienFamilial: this.tiersLienFamilial(),
      accordParents: this.accordParents(),
      motivationDelegation: motivation.length > 0 ? motivation : null,
      ageEnfant: this.ageEnfant(),
      dangerCaracterise: this.dangerCaracterise(),
      decisionsJudiciairesPrecedentes: this.decisionsJudiciairesPrecedentes(),
      desinteretParentPlusUnAn: this.desinteretParentPlusUnAn(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Recevabilité calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: DelegationApFrResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /** Libellé humain pour le verdict de recevabilité. */
  verdictLabel(verdict: VerdictRecevabiliteDelegationAp): string {
    const labels: Record<VerdictRecevabiliteDelegationAp, string> = {
      RECEVABLE_VOLONTAIRE: 'Recevable — voie volontaire conjointe',
      RECEVABLE_JUDICIAIRE_DESINTERET: 'Recevable — voie judiciaire (désintérêt)',
      RECEVABLE_JUDICIAIRE_TIERS: 'Recevable — voie judiciaire (requête tiers)',
      IRRECEVABLE_ENFANT_MAJEUR: 'Irrecevable — enfant majeur',
      IRRECEVABLE_VOIE_INADEQUATE: 'Irrecevable — voie inadéquate',
    };
    return labels[verdict];
  }

  /** Couleur Material par verdict pour la pastille. */
  verdictClass(verdict: VerdictRecevabiliteDelegationAp): string {
    const cls: Record<VerdictRecevabiliteDelegationAp, string> = {
      RECEVABLE_VOLONTAIRE: 'verdict-success',
      RECEVABLE_JUDICIAIRE_DESINTERET: 'verdict-primary',
      RECEVABLE_JUDICIAIRE_TIERS: 'verdict-primary',
      IRRECEVABLE_ENFANT_MAJEUR: 'verdict-warning',
      IRRECEVABLE_VOIE_INADEQUATE: 'verdict-warning',
    };
    return cls[verdict];
  }

  /** Libellé humain pour la voie procédurale. */
  voieLabel(voie: TypeDelegationAp): string {
    const labels: Record<TypeDelegationAp, string> = {
      VOLONTAIRE_CONJOINTE: 'Voie volontaire conjointe (acte notarié / JAF)',
      JUDICIAIRE_DESINTERET: 'Voie judiciaire — désintérêt',
      JUDICIAIRE_TIERS: 'Voie judiciaire — requête tiers',
    };
    return labels[voie];
  }
}

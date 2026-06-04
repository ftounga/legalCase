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
import { MatSnackBar } from '@angular/material/snack-bar';
import { EtatCivilBeModificationService } from '../../core/services/etat-civil-be-modification.service';
import {
  EtatCivilBeModificationRequest,
  EtatCivilBeModificationResponse,
  EtatCivilBeModificationVerdict,
  TypeModificationEtatCivil,
} from '../../core/models/etat-civil-be-modification.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { EtatCivilBeModificationSectionPrefillRules } from './etat-civil-be-modification-section-prefill-rules';

interface TypeOption { value: TypeModificationEtatCivil; label: string; }

/**
 * SF-223-09 : composant Angular standalone pour l'outil décisionnel
 * "Modification de l'état civil (Belgique)" (`etat-civil-be-modification`).
 * BELGIQUE uniquement (changement de nom / prénom — loi 18/06/2018 ; changement
 * de sexe — loi 25/06/2017, auto-déclaration administrative — à vérifier par
 * avocat belge).
 *
 * 1 outil = 1 situation « modification de l'état civil » (nom / prénom / sexe).
 * DISTINCT de la rectification d'état civil (erreur matérielle d'acte — P4
 * différé F-224) et du changement d'état civil FR.
 *
 * Pré-fill IA F-246 : type + majorité + nationalité/résidence pré-remplis si
 * factualisables (sous-objet `etat_civil_modification_be_detection`). Aucune
 * citation jurisprudentielle BE (F-JU-04 parké — silence > erreur). OnPush +
 * ChangeDetectorRef.markForCheck() dans next/error des subscribe (mémoire
 * `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-etat-civil-be-modification-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './etat-civil-be-modification-section.component.html',
  styleUrl: './etat-civil-be-modification-section.component.scss',
})
export class EtatCivilBeModificationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE parké F-JU-04 : aucune en V1).
  protected readonly toolIdForJurisprudence = 'etat-civil-be-modification';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'MODIFICATION DE L\'ÉTAT CIVIL (BELGIQUE)';
  static readonly TOOL_ICON = 'badge';

  /**
   * SF-223-09 — pré-fill F-246 : type + majorité + nationalité/résidence si
   * factualisables. Délègue au helper partagé (parité runtime/static — garde-fou
   * `prefill-count-integrity`).
   */
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return EtatCivilBeModificationSectionPrefillRules.computePrefillCount(input);
  }

  static readonly TYPE_OPTIONS: readonly TypeOption[] = [
    { value: 'CHANGEMENT_PRENOM', label: 'Changement de prénom (officier de l\'état civil — loi 18/06/2018)' },
    { value: 'CHANGEMENT_NOM', label: 'Changement de nom (SPF Justice)' },
    { value: 'CHANGEMENT_SEXE', label: 'Changement de sexe (auto-déclaration — loi 25/06/2017)' },
  ];
  readonly typeOptions = EtatCivilBeModificationSectionComponent.TYPE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<EtatCivilBeModificationResponse | null>(null);

  // --- Form fields ---
  typeModification = signal<TypeModificationEtatCivil | null>(null);
  personneMajeure = signal<boolean>(true);
  nationaliteBelgeOuResident = signal<boolean>(true);
  motifLegitime = signal<boolean>(false);
  secondeDemandePrenom = signal<boolean>(false);
  declarationSexeReiteree = signal<boolean>(false);
  consentementRepresentantsSiMineur = signal<boolean>(false);

  constructor(
    private service: EtatCivilBeModificationService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
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
   * F-246 — pré-remplit type + majorité + nationalité/résidence depuis le
   * sous-objet IA `etat_civil_modification_be_detection` (clés camelCase plates,
   * @JsonUnwrapped). Les booleans de fond restent à apprécier par l'avocat.
   */
  private prefillFromAi(): void {
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.cdr.markForCheck();
      return;
    }
    const ai = this.aiData as (FamilleExtractedData & {
      etatCivilModificationTypeDetecte?: string | null;
      etatCivilModificationMajeurDetecte?: boolean | null;
      etatCivilModificationNationaliteResidentDetectee?: boolean | null;
    }) | null | undefined;
    if (!ai) {
      this.cdr.markForCheck();
      return;
    }
    const type = ai.etatCivilModificationTypeDetecte;
    if (type === 'CHANGEMENT_NOM' || type === 'CHANGEMENT_PRENOM' || type === 'CHANGEMENT_SEXE') {
      this.typeModification.set(type);
    }
    if (typeof ai.etatCivilModificationMajeurDetecte === 'boolean') {
      this.personneMajeure.set(ai.etatCivilModificationMajeurDetecte);
    }
    if (typeof ai.etatCivilModificationNationaliteResidentDetectee === 'boolean') {
      this.nationaliteBelgeOuResident.set(ai.etatCivilModificationNationaliteResidentDetectee);
    }
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** True si la branche changement de NOM est sélectionnée (motif visible). */
  isNom(): boolean {
    return this.typeModification() === 'CHANGEMENT_NOM';
  }

  /** True si la branche changement de PRÉNOM est sélectionnée. */
  isPrenom(): boolean {
    return this.typeModification() === 'CHANGEMENT_PRENOM';
  }

  /** True si la branche changement de SEXE est sélectionnée. */
  isSexe(): boolean {
    return this.typeModification() === 'CHANGEMENT_SEXE';
  }

  /** Form valide : workspace BE + type renseigné. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.typeModification()) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: EtatCivilBeModificationRequest = {
      typeModification: this.typeModification()!,
      personneMajeure: this.personneMajeure(),
      nationaliteBelgeOuResident: this.nationaliteBelgeOuResident(),
      motifLegitime: this.isNom() ? this.motifLegitime() : null,
      secondeDemandePrenom: this.isPrenom() ? this.secondeDemandePrenom() : null,
      declarationSexeReiteree: this.isSexe() ? this.declarationSexeReiteree() : null,
      consentementRepresentantsSiMineur: this.personneMajeure() ? null : this.consentementRepresentantsSiMineur(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Modification d\'état civil qualifiée', 'OK', { duration: 2500 });
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

  private applyResult(r: EtatCivilBeModificationResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: EtatCivilBeModificationResponse): void {
    this.typeModification.set(r.typeModification);
    this.personneMajeure.set(r.personneMajeure);
    this.nationaliteBelgeOuResident.set(r.nationaliteBelgeOuResident);
    this.motifLegitime.set(r.motifLegitime ?? false);
    this.secondeDemandePrenom.set(r.secondeDemandePrenom ?? false);
    this.declarationSexeReiteree.set(r.declarationSexeReiteree ?? false);
    this.consentementRepresentantsSiMineur.set(r.consentementRepresentantsSiMineur ?? false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Vert pour MODIFICATION_RECEVABLE. Or pour MODIFICATION_RECEVABLE_SOUS_CONDITIONS.
   * Rouge pour MODIFICATION_IRRECEVABLE. Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: EtatCivilBeModificationVerdict): string {
    switch (verdict) {
      case 'MODIFICATION_RECEVABLE':
        return 'eciv-verdict-banner eciv-verdict-banner--ok';
      case 'MODIFICATION_RECEVABLE_SOUS_CONDITIONS':
        return 'eciv-verdict-banner eciv-verdict-banner--warn';
      case 'MODIFICATION_IRRECEVABLE':
        return 'eciv-verdict-banner eciv-verdict-banner--ko';
      case 'QUALIFICATION_INCOMPLETE':
        return 'eciv-verdict-banner eciv-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: EtatCivilBeModificationVerdict): string {
    switch (verdict) {
      case 'MODIFICATION_RECEVABLE': return 'Modification recevable';
      case 'MODIFICATION_RECEVABLE_SOUS_CONDITIONS': return 'Recevable sous conditions';
      case 'MODIFICATION_IRRECEVABLE': return 'Modification irrecevable';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: EtatCivilBeModificationVerdict): string {
    switch (verdict) {
      case 'MODIFICATION_RECEVABLE': return 'check_circle';
      case 'MODIFICATION_RECEVABLE_SOUS_CONDITIONS': return 'rule';
      case 'MODIFICATION_IRRECEVABLE': return 'block';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  typeLabel(t: TypeModificationEtatCivil): string {
    return this.typeOptions.find(opt => opt.value === t)?.label ?? t;
  }
}

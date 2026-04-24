import {
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9terService } from '../../core/services/belgian-9ter.service';
import {
  Belgian9terResponse,
  Verdict9ter,
} from '../../core/models/belgian-9ter.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

/**
 * SF-IM-14-06 : outil décisionnel "Régularisation 9ter médical" (BE).
 * Consomme l'API `POST + GET /api/v1/case-files/{id}/belgian-9ter`
 * livrée par SF-IM-14-02 (PR #509). BELGIQUE uniquement — l'équivalent
 * français (étranger malade L.425-9 CESEDA) est juridiquement
 * distinct (invariant "1 outil = 1 situation").
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-IM-14-9ter-medical-be`, ALWAYS_ON BE, priority 65). Le wiring
 * effectif TOOL_REGISTRY est livré dans une SF d'intégration
 * dédiée (cf. mini-spec).
 *
 * Pré-fill IA : `aiData?: Partial<ImmigrationExtractedData>`. Les
 * champs médicaux ne sont pas (encore) exposés par
 * `ImmigrationExtractedData` — handler gracieux qui ignore les champs
 * absents (no-op si `aiData` ne fournit rien d'exploitable).
 */
@Component({
  selector: 'app-belgian-9ter-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './belgian-9ter-section.component.html',
  styleUrl: './belgian-9ter-section.component.scss',
})
export class Belgian9terSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  /**
   * Pré-fill IA. À ce stade, `ImmigrationExtractedData` n'expose pas
   * de champs médicaux ; le composant accepte un objet partiel et
   * ignore gracieusement les champs absents (no-op).
   */
  @Input() aiData?: Partial<ImmigrationExtractedData> | null;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Belgian9terResponse | null>(null);

  // Form signals.
  dateDebutSymptomes = signal<string | null>(null);
  maladieGraveCertifiee = signal<boolean>(false);
  soinsNecessairesDisponiblesBe = signal<boolean>(false);
  soinsInaccessiblesPaysOrigine = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA par champ — effacée au 1er onChange manuel.
  // Reste null tant que `aiData` ne fournit pas de champ médical
  // exploitable (ImmigrationExtractedData ne les expose pas encore).
  provenanceDateDebutSymptomes = signal<'IA' | null>(null);
  provenanceMaladieGrave = signal<'IA' | null>(null);
  provenanceSoinsBe = signal<'IA' | null>(null);
  provenanceSoinsInaccessibles = signal<'IA' | null>(null);
  provenanceMenace = signal<'IA' | null>(null);
  provenanceDateDepotDemande = signal<'IA' | null>(null);

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: Belgian9terService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.isBelgium()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Re-prefill si aiData change après le ngOnInit, en mode formulaire,
    // sans résultat persisté (évite d'écraser saisie avocat ou résultat backend).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /**
   * Toutes les conditions d'entrée sont des booléens — le formulaire est
   * valide même si tous les toggles sont à false (le backend acceptera
   * un score 0 et renverra un verdict FAIBLE).
   * Seules les contraintes temporelles bloquent la soumission :
   * - dateDebutSymptomes ≤ today,
   * - dateDepotDemande ≤ today,
   * - dateDepotDemande ≥ dateDebutSymptomes (si les deux fournies).
   */
  formValid(): boolean {
    const symp = this.dateDebutSymptomes();
    const depot = this.dateDepotDemande();
    if (symp && symp > this.todayIso) return false;
    if (depot && depot > this.todayIso) return false;
    if (symp && depot && depot < symp) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateDebutSymptomes: this.dateDebutSymptomes(),
      maladieGraveCertifiee: this.maladieGraveCertifiee(),
      soinsNecessairesDisponiblesBe: this.soinsNecessairesDisponiblesBe(),
      soinsInaccessiblesPaysOrigine: this.soinsInaccessiblesPaysOrigine(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      dateDepotDemande: this.dateDepotDemande(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Régularisation 9ter analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
      },
    });
  }

  // Handlers onChange — effacent la provenance IA au 1er changement.
  onDateDebutSymptomesChange(value: string | null): void {
    this.dateDebutSymptomes.set(value || null);
    this.provenanceDateDebutSymptomes.set(null);
  }

  onMaladieGraveChange(value: boolean): void {
    this.maladieGraveCertifiee.set(value);
    this.provenanceMaladieGrave.set(null);
  }

  onSoinsBeChange(value: boolean): void {
    this.soinsNecessairesDisponiblesBe.set(value);
    this.provenanceSoinsBe.set(null);
  }

  onSoinsInaccessiblesChange(value: boolean): void {
    this.soinsInaccessiblesPaysOrigine.set(value);
    this.provenanceSoinsInaccessibles.set(null);
  }

  onMenaceChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
    this.provenanceMenace.set(null);
  }

  onDateDepotDemandeChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepotDemande.set(null);
  }

  /**
   * Pré-fill IA. À ce stade, `ImmigrationExtractedData` n'expose pas
   * de champs médicaux. Les champs ci-dessous sont lus de manière
   * défensive — s'ils existent un jour dans le DTO, le composant les
   * récupèrera automatiquement.
   *
   * Règles (fail-open) :
   * - Passe silencieusement si `aiData` absent.
   * - Chaque champ est indépendant (prefill partiel OK).
   * - Validation de type : string non vide pour les dates, boolean
   *   strict pour les toggles.
   */
  private prefillFromAi(): void {
    const ai: any = this.aiData;
    if (!ai) return;

    if (typeof ai.dateDebutSymptomes === 'string' && ai.dateDebutSymptomes.length > 0) {
      this.dateDebutSymptomes.set(ai.dateDebutSymptomes);
      this.provenanceDateDebutSymptomes.set('IA');
    }
    if (typeof ai.maladieGraveCertifiee === 'boolean') {
      this.maladieGraveCertifiee.set(ai.maladieGraveCertifiee);
      this.provenanceMaladieGrave.set('IA');
    } else if (typeof ai.maladieGrave === 'boolean') {
      // Tolérance : nom alternatif possible côté extraction IA.
      this.maladieGraveCertifiee.set(ai.maladieGrave);
      this.provenanceMaladieGrave.set('IA');
    }
    if (typeof ai.soinsNecessairesDisponiblesBe === 'boolean') {
      this.soinsNecessairesDisponiblesBe.set(ai.soinsNecessairesDisponiblesBe);
      this.provenanceSoinsBe.set('IA');
    }
    if (typeof ai.soinsInaccessiblesPaysOrigine === 'boolean') {
      this.soinsInaccessiblesPaysOrigine.set(ai.soinsInaccessiblesPaysOrigine);
      this.provenanceSoinsInaccessibles.set('IA');
    }
    if (typeof ai.menaceOrdrePublic === 'boolean') {
      this.menaceOrdrePublic.set(ai.menaceOrdrePublic);
      this.provenanceMenace.set('IA');
    }
    if (typeof ai.dateDepotDemande === 'string' && ai.dateDepotDemande.length > 0) {
      this.dateDepotDemande.set(ai.dateDepotDemande);
      this.provenanceDateDepotDemande.set('IA');
    }
  }

  /**
   * Bannière du résultat selon le verdict :
   * - ELEVEE → succès (vert).
   * - MOYENNE → info (navy).
   * - FAIBLE → warning (or).
   * Rouge réservé à la `criteresNonRemplis` "menace ordre public".
   */
  bannerClass(verdict: Verdict9ter | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'belgian-9ter-banner belgian-9ter-banner--success';
      case 'MOYENNE': return 'belgian-9ter-banner belgian-9ter-banner--info';
      case 'FAIBLE':  return 'belgian-9ter-banner belgian-9ter-banner--warning';
      default:        return 'belgian-9ter-banner';
    }
  }

  bannerIcon(verdict: Verdict9ter | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'check_circle';
      case 'MOYENNE': return 'info_outline';
      case 'FAIBLE':  return 'warning';
      default:        return 'info_outline';
    }
  }

  verdictLabel(verdict: Verdict9ter | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE':  return 'Probabilité faible';
      default:        return '';
    }
  }

  /** True quand le verdict est FAIBLE et que la menace ordre public est présente. */
  hasCriticalMenace(r: Belgian9terResponse | null): boolean {
    if (!r) return false;
    return !r.pasMenace;
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutSymptomes.set(r.dateDebutSymptomes);
        this.maladieGraveCertifiee.set(r.maladieGraveCertifiee);
        this.soinsNecessairesDisponiblesBe.set(r.soinsNecessairesDisponiblesBe);
        this.soinsInaccessiblesPaysOrigine.set(r.soinsInaccessiblesPaysOrigine);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.dateDepotDemande.set(r.dateDepotDemande);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire et
        // on applique le pré-fill IA si aiData disponible.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }
}

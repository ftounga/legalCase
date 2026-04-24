import {
  Component, Input, OnChanges, OnInit, Optional, SimpleChanges,
  signal, computed,
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
import { Belgian9bisService } from '../../core/services/belgian-9bis.service';
import {
  Belgian9bisResponse,
  Verdict9bis,
} from '../../core/models/belgian-9bis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

/**
 * SF-IM-14-05 : outil décisionnel "9bis humanitaire BE"
 * (Loi 15/12/1980 art. 9bis + AR 17/05/2007). BE uniquement.
 *
 * Consomme l'API SF-IM-14-01 (Belgian9bisController). Affiché conditionnellement
 * par le panel F-IA-04 via tool_id `F-IM-14-9bis-humanitaire-be`.
 *
 * Pré-fill IA partiel depuis ImmigrationExtractedData :
 * - dateDepotProcedure → dateDepotDemande
 * (autres champs non couverts aujourd'hui — no-op gracieux).
 */
@Component({
  selector: 'app-belgian-9bis-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './belgian-9bis-section.component.html',
  styleUrl: './belgian-9bis-section.component.scss',
})
export class Belgian9bisSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  /** SF-IM-14-05 : pré-fill IA depuis ImmigrationExtractedData (BE). */
  @Input() aiData?: Partial<ImmigrationExtractedData> | null;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Belgian9bisResponse | null>(null);

  // Form state
  dateEntreeBelgique = signal<string | null>(null);
  dureePresenceMois = signal<number>(0);
  circonstancesExceptionnelles = signal<boolean>(false);
  liensFamiliauxBe = signal<boolean>(false);
  liensProfessionnels = signal<boolean>(false);
  scolariteEnfantsBe = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA par champ.
  provenanceDateDepot = signal<'IA' | null>(null);

  private aiDataSignal = signal<Partial<ImmigrationExtractedData> | null | undefined>(undefined);

  /** Aujourd'hui en YYYY-MM-DD pour [max] des champs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: Belgian9bisService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    if (this.isBelgium()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      // Ré-applique le prefill si aiData change, seulement si l'on est encore
      // en mode formulaire ET qu'aucun résultat persisté n'a été chargé.
      if (!changes['aiData'].firstChange
          && this.isBelgium() && this.showForm() && !this.result()) {
        this.prefillFromAi();
      }
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateEntreeBelgique();
    const duree = this.dureePresenceMois();
    if (!d) return false;
    if (d > this.todayIso) return false;
    if (duree === null || duree === undefined) return false;
    if (!Number.isFinite(duree) || duree < 0) return false;
    const dDepot = this.dateDepotDemande();
    if (dDepot && dDepot < d) return false; // dépôt >= entrée
    if (dDepot && dDepot > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const dDepot = this.dateDepotDemande();
    const request = {
      dateEntreeBelgique: this.dateEntreeBelgique()!,
      dureePresenceMois: this.dureePresenceMois(),
      circonstancesExceptionnelles: this.circonstancesExceptionnelles(),
      liensFamiliauxBe: this.liensFamiliauxBe(),
      liensProfessionnels: this.liensProfessionnels(),
      scolariteEnfantsBe: this.scolariteEnfantsBe(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      ...(dDepot ? { dateDepotDemande: dDepot } : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse 9bis humanitaire enregistrée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  // Handlers — effacent la provenance IA au 1er changement manuel.
  onDateEntreeChange(value: string | null): void {
    this.dateEntreeBelgique.set(value || null);
  }

  onDureePresenceChange(value: number | null | undefined): void {
    this.dureePresenceMois.set(value ?? 0);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepot.set(null);
  }

  /**
   * Pré-fill IA depuis aiData. Règles fail-open :
   * - aiData absent → no-op
   * - dateDepotProcedure (string) → dateDepotDemande + provenance IA
   * - autres champs non mappés actuellement (no-op gracieux)
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    const dDepot = ai.dateDepotProcedure;
    if (typeof dDepot === 'string' && dDepot.length > 0 && !this.dateDepotDemande()) {
      this.dateDepotDemande.set(dDepot);
      this.provenanceDateDepot.set('IA');
    }
  }

  /**
   * Classe CSS du bandeau verdict.
   * Palette navy/or — pas de rouge dominant (pas d'urgence 48h ici).
   */
  bannerClass(verdict: Verdict9bis | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'b9bis-banner b9bis-banner--success';
      case 'MOYENNE': return 'b9bis-banner b9bis-banner--info';
      case 'FAIBLE':  return 'b9bis-banner b9bis-banner--warning';
      default:        return 'b9bis-banner';
    }
  }

  bannerIcon(verdict: Verdict9bis | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'check_circle';
      case 'MOYENNE': return 'info_outline';
      case 'FAIBLE':  return 'warning';
      default:        return 'info_outline';
    }
  }

  verdictLabel(verdict: Verdict9bis | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE':  return 'Probabilité faible';
      default:        return '';
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntreeBelgique.set(r.dateEntreeBelgique);
        this.dureePresenceMois.set(r.dureePresenceMois);
        this.circonstancesExceptionnelles.set(r.circonstancesExceptionnelles);
        this.liensFamiliauxBe.set(r.liensFamiliauxBe);
        this.liensProfessionnels.set(r.liensProfessionnels);
        this.scolariteEnfantsBe.set(r.scolariteEnfantsBe);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.dateDepotDemande.set(r.dateDepotDemande);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en formulaire et on
        // applique le pré-fill IA si aiData disponible.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }
}

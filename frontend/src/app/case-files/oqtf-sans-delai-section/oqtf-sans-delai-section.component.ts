import { Component, Input, OnInit, Optional, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OqtfSansDelaiService } from '../../core/services/oqtf-sans-delai.service';
import {
  MOTIFS_SANS_DELAI,
  MotifSansDelai,
  MotifSansDelaiOption,
  OqtfSansDelaiResponse,
  StatutDelaiSd,
} from '../../core/models/oqtf-sans-delai.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-IM-08-04 : outil décisionnel dédié "OQTF SANS délai de départ
 * volontaire — France" (F-IM-08). FR uniquement. Procédure d'urgence
 * absolue : 48h pour former un recours (contre 30 jours pour l'OQTF
 * avec délai). Invariant "un outil = une situation métier" : séparé
 * de SF-IM-08-02 (OQTF avec délai).
 * Consomme l'API SF-IM-08-03. Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-IM-08-oqtf-sans-delai-fr').
 */
@Component({
  selector: 'app-oqtf-sans-delai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
  ],
  templateUrl: './oqtf-sans-delai-section.component.html',
  styleUrl: './oqtf-sans-delai-section.component.scss',
})
export class OqtfSansDelaiSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfSansDelaiResponse | null>(null);

  dateHeureNotificationOqtf = signal<string | null>(null);
  motifSansDelai = signal<MotifSansDelai | null>(null);
  placementCra = signal<boolean>(false);
  recoursForme = signal<boolean>(false);
  dateHeureRecours = signal<string | null>(null);

  readonly motifs: MotifSansDelaiOption[] = MOTIFS_SANS_DELAI;

  /** Maintenant au format ISO local (YYYY-MM-DDTHH:mm) pour `max` des inputs datetime-local. */
  readonly nowLocalIso: string = this.buildNowLocalIso();

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: OqtfSansDelaiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.isFrance()) {
      this.load();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateHeureNotificationOqtf();
    const m = this.motifSansDelai();
    if (!d || !m) return false;
    if (d > this.nowLocalIso) return false; // pas dans le futur
    if (this.recoursForme()) {
      const dr = this.dateHeureRecours();
      if (!dr) return false;
      if (dr < d) return false; // recours >= notification
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateHeureNotificationOqtf: this.dateHeureNotificationOqtf()!,
      motifSansDelai: this.motifSansDelai()!,
      placementCra: this.placementCra(),
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? { dateHeureRecours: this.dateHeureRecours()! }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('OQTF sans délai analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  /**
   * Classe CSS du bandeau de résultat selon le statut calculé.
   * Urgence absolue 48h : palette rouge dominante — DISPONIBLE et
   * URGENT sont déjà en rouge (contrairement à SF-IM-08-02 où DISPONIBLE
   * est bleu / URGENT est or). Seul RECOURS_FORME bascule en vert.
   * Autorisé par DESIGN_SYSTEM.md pour les situations d'urgence critique.
   */
  bannerClass(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'oqtf-sd-banner oqtf-sd-banner--danger-medium';
      case 'URGENT':        return 'oqtf-sd-banner oqtf-sd-banner--danger-strong';
      case 'EXPIRE':        return 'oqtf-sd-banner oqtf-sd-banner--danger-dark';
      case 'RECOURS_FORME': return 'oqtf-sd-banner oqtf-sd-banner--success';
      default:              return 'oqtf-sd-banner';
    }
  }

  bannerIcon(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'warning';
      case 'URGENT':        return 'error';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'warning';
    }
  }

  statutLabel(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai 48h disponible';
      case 'URGENT':        return 'Délai 48h urgent';
      case 'EXPIRE':        return 'Délai 48h expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  /** Affiche le compteur d'heures restantes si pertinent (hors EXPIRE/RECOURS_FORME). */
  showHeuresRestantes(r: OqtfSansDelaiResponse | null): boolean {
    if (!r) return false;
    return r.statutDelaiRecours === 'DISPONIBLE' || r.statutDelaiRecours === 'URGENT';
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateHeureNotificationOqtf.set(this.toLocalInputValue(r.dateHeureNotificationOqtf));
        this.motifSansDelai.set(r.motifSansDelai);
        this.placementCra.set(r.placementCra);
        this.recoursForme.set(r.recoursForme);
        this.dateHeureRecours.set(r.dateHeureRecours ? this.toLocalInputValue(r.dateHeureRecours) : null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
      },
    });
  }

  /**
   * Convertit une ISO string backend (avec ou sans offset) en chaîne
   * compatible avec `<input type="datetime-local">` (YYYY-MM-DDTHH:mm).
   * Tolérant aux valeurs déjà au bon format.
   */
  private toLocalInputValue(iso: string): string {
    if (!iso) return '';
    // Déjà au format datetime-local (pas de Z / pas d'offset)
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(iso)) return iso;
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private buildNowLocalIso(): string {
    const d = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}

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
import { OqtfAvecDelaiService } from '../../core/services/oqtf-avec-delai.service';
import {
  MOTIFS_OQTF,
  MotifOqtf,
  MotifOqtfOption,
  OqtfAvecDelaiResponse,
  StatutDelai,
} from '../../core/models/oqtf-avec-delai.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-IM-08-02 : outil décisionnel dédié "OQTF avec délai de départ
 * volontaire — France" (F-IM-08). FR uniquement — OQT belge (annexe 13)
 * traitée dans SF-IM-08-05 / SF-IM-08-06 (procédure juridiquement
 * distincte, invariant "un outil = une situation métier"). Consomme
 * l'API SF-IM-08-01. Affiché conditionnellement par le panel F-IA-04
 * (tool_id 'F-IM-08-oqtf-avec-delai-fr').
 */
@Component({
  selector: 'app-oqtf-avec-delai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
  ],
  templateUrl: './oqtf-avec-delai-section.component.html',
  styleUrl: './oqtf-avec-delai-section.component.scss',
})
export class OqtfAvecDelaiSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfAvecDelaiResponse | null>(null);

  dateNotificationOqtf = signal<string | null>(null);
  motifOqtf = signal<MotifOqtf | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  readonly motifs: MotifOqtfOption[] = MOTIFS_OQTF;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: OqtfAvecDelaiService,
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
    const d = this.dateNotificationOqtf();
    const m = this.motifOqtf();
    if (!d || !m) return false;
    if (d > this.todayIso) return false; // pas dans le futur
    if (this.recoursForme()) {
      const dr = this.dateRecours();
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
      dateNotificationOqtf: this.dateNotificationOqtf()!,
      motifOqtf: this.motifOqtf()!,
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? { dateRecours: this.dateRecours()! }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('OQTF analysée', 'OK', { duration: 2500 });
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
   * Rouge réservé au seul statut EXPIRE (alerte critique —
   * DESIGN_SYSTEM.md autorise la couleur rouge pour les statuts
   * d'urgence uniquement).
   */
  bannerClass(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':   return 'oqtf-banner oqtf-banner--info';
      case 'URGENT':       return 'oqtf-banner oqtf-banner--warning';
      case 'EXPIRE':       return 'oqtf-banner oqtf-banner--danger';
      case 'RECOURS_FORME':return 'oqtf-banner oqtf-banner--success';
      default:             return 'oqtf-banner';
    }
  }

  bannerIcon(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'URGENT':        return 'Délai urgent';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  /** Affiche le compteur jours restants si pertinent (hors EXPIRE/RECOURS_FORME). */
  showJoursRestants(r: OqtfAvecDelaiResponse | null): boolean {
    if (!r) return false;
    return r.statutDelaiRecours === 'DISPONIBLE' || r.statutDelaiRecours === 'URGENT';
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationOqtf.set(r.dateNotificationOqtf);
        this.motifOqtf.set(r.motifOqtf);
        this.recoursForme.set(r.recoursForme);
        this.dateRecours.set(r.dateRecours);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
      },
    });
  }
}

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
import { Annexe13BeService } from '../../core/services/annexe13-be.service';
import {
  MOTIFS_OQT,
  MotifOqt,
  MotifOqtOption,
  TYPES_RECOURS,
  TypeRecours,
  TypeRecoursOption,
  Annexe13BeResponse,
  StatutRecoursAnnul,
} from '../../core/models/annexe13-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-IM-08-06 : outil décisionnel dédié "Annexe 13 — OQT belge"
 * (F-IM-08). BE uniquement — l'OQTF française est traitée dans
 * SF-IM-08-01/02 (avec délai) et SF-IM-08-03/04 (sans délai)
 * (procédures juridiquement distinctes, invariant
 * "un outil = une situation métier"). Consomme l'API SF-IM-08-05.
 * Affiché conditionnellement par le panel F-IA-04
 * (tool_id 'F-IM-08-annexe13-be').
 */
@Component({
  selector: 'app-annexe13-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
  ],
  templateUrl: './annexe13-be-section.component.html',
  styleUrl: './annexe13-be-section.component.scss',
})
export class Annexe13BeSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Annexe13BeResponse | null>(null);

  dateNotificationAnnexe13 = signal<string | null>(null);
  delaiDepartImposeJours = signal<number>(30);
  motifOqt = signal<MotifOqt | null>(null);
  transfertImminent = signal<boolean>(false);
  recoursForme = signal<boolean>(false);
  typeRecours = signal<TypeRecours | null>(null);
  dateRecours = signal<string | null>(null);

  readonly motifs: MotifOqtOption[] = MOTIFS_OQT;
  readonly typesRecours: TypeRecoursOption[] = TYPES_RECOURS;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: Annexe13BeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.isBelgium()) {
      this.load();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateNotificationAnnexe13();
    const m = this.motifOqt();
    const delai = this.delaiDepartImposeJours();
    if (!d || !m) return false;
    if (d > this.todayIso) return false; // pas dans le futur
    if (delai === null || delai === undefined) return false;
    if (delai < 0 || delai > 30) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      const tr = this.typeRecours();
      if (!dr || !tr) return false;
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
      dateNotificationAnnexe13: this.dateNotificationAnnexe13()!,
      delaiDepartImposeJours: this.delaiDepartImposeJours(),
      motifOqt: this.motifOqt()!,
      transfertImminent: this.transfertImminent(),
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? {
            dateRecours: this.dateRecours()!,
            typeRecours: this.typeRecours()!,
          }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Annexe 13 analysée', 'OK', { duration: 2500 });
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
  bannerClass(statut: StatutRecoursAnnul | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':   return 'annexe13-banner annexe13-banner--info';
      case 'URGENT':       return 'annexe13-banner annexe13-banner--warning';
      case 'EXPIRE':       return 'annexe13-banner annexe13-banner--danger';
      case 'RECOURS_FORME':return 'annexe13-banner annexe13-banner--success';
      default:             return 'annexe13-banner';
    }
  }

  bannerIcon(statut: StatutRecoursAnnul | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutRecoursAnnul | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'URGENT':        return 'Délai urgent';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  typeRecoursLabel(code: TypeRecours | null | undefined): string {
    switch (code) {
      case 'ANNULATION_30J':       return 'Annulation 30 jours';
      case 'EXTREME_URGENCE_5JO':  return 'Extrême urgence 5 jours ouvrables';
      default:                     return '';
    }
  }

  /** Affiche le compteur jours restants si pertinent (hors EXPIRE/RECOURS_FORME). */
  showJoursRestants(r: Annexe13BeResponse | null): boolean {
    if (!r) return false;
    return r.statutRecoursAnnulation === 'DISPONIBLE' || r.statutRecoursAnnulation === 'URGENT';
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationAnnexe13.set(r.dateNotificationAnnexe13);
        this.delaiDepartImposeJours.set(r.delaiDepartImposeJours);
        this.motifOqt.set(r.motifOqt);
        this.transfertImminent.set(r.transfertImminent);
        this.recoursForme.set(r.recoursForme);
        this.typeRecours.set(r.typeRecours);
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

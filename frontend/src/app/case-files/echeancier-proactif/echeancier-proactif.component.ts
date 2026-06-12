import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EcheancierService } from '../../core/services/echeancier.service';
import {
  EcheancierItem,
  EcheancierResponse,
  EcheancierUrgency,
} from '../../core/models/echeancier.model';

/**
 * F-284 / SF-284-02 — échéancier procédural proactif (onglet Suivi, en tête).
 *
 * Met en avant le prochain couperet du dossier (hero compte à rebours) + les 3
 * échéances suivantes, priorisés par urgence. Vue de lecture sur l'agrégat backend
 * (délais F-69 confirmés + échéances de réponse contradictoires F-282). L'édition
 * fine reste dans `case-deadlines-section` (lien « Voir tous les délais »), la réponse
 * aux rounds dans `contradictoire-timeline`. Pas de saisie ici (cf. SF-284-00b — INV
 * anti-surcharge : hero + 3 + lien, pas de recopie intégrale de la liste).
 */
@Component({
  selector: 'app-echeancier-proactif',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './echeancier-proactif.component.html',
  styleUrl: './echeancier-proactif.component.scss',
})
export class EcheancierProactifComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;

  /** Demande de voir le détail complet des délais (le parent scrolle vers #section-deadlines). */
  @Output() viewAllRequested = new EventEmitter<void>();

  private readonly service = inject(EcheancierService);
  private readonly cdr = inject(ChangeDetectorRef);

  /** Nombre d'items secondaires affichés sous le hero (UX-3 : hero + 3). */
  static readonly SECONDARY_LIMIT = 3;

  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly data = signal<EcheancierResponse | null>(null);

  readonly hero = computed<EcheancierItem | null>(() => this.data()?.nextItem ?? null);

  /** Items après le hero, limités à 3 (le reste renvoie vers la liste détaillée F-69). */
  readonly secondary = computed<EcheancierItem[]>(() => {
    const items = this.data()?.items ?? [];
    return items.slice(1, 1 + EcheancierProactifComponent.SECONDARY_LIMIT);
  });

  /** Nombre de délais non montrés (hero + secondaires) → bascule l'affichage du lien. */
  readonly hiddenCount = computed<number>(() => {
    const total = this.data()?.counts.total ?? 0;
    const shown = (this.hero() ? 1 : 0) + this.secondary().length;
    return Math.max(0, total - shown);
  });

  readonly isEmpty = computed<boolean>(() => !this.loading() && (this.data()?.counts.total ?? 0) === 0);

  /** True si rien d'urgent (aucun OVERDUE ni CRITICAL) → ton apaisé, pas d'alarme. */
  readonly underControl = computed<boolean>(() => {
    const c = this.data()?.counts;
    return !!c && c.overdue === 0 && c.critical === 0;
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (resp) => {
        this.data.set(resp);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // Lecture seule : on masque la carte plutôt que de polluer l'onglet d'un snackbar.
        this.failed.set(true);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  onViewAll(): void {
    this.viewAllRequested.emit();
  }

  urgencyClass(urgency: EcheancierUrgency): string {
    return `eche--${urgency.toLowerCase()}`;
  }

  /** Libellé du compte à rebours : « Aujourd'hui » / « J-3 » / « J+2 (dépassé) ». */
  daysLabel(daysUntil: number): string {
    if (daysUntil === 0) return "Aujourd'hui";
    if (daysUntil < 0) return `J+${Math.abs(daysUntil)} (dépassé)`;
    return `J-${daysUntil}`;
  }

  urgencyIcon(urgency: EcheancierUrgency): string {
    switch (urgency) {
      case 'OVERDUE':
        return 'error';
      case 'CRITICAL':
        return 'priority_high';
      case 'SOON':
        return 'schedule';
      default:
        return 'event_available';
    }
  }

  kindIcon(kind: EcheancierItem['kind']): string {
    return kind === 'CONTRADICTOIRE' ? 'forum' : 'gavel';
  }

  kindLabel(kind: EcheancierItem['kind']): string {
    return kind === 'CONTRADICTOIRE' ? 'Réponse' : 'Délai';
  }
}

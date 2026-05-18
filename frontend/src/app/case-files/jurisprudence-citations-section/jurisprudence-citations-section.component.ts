import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  JurisprudenceCheck,
  JurisprudenceCheckStatut,
} from '../../core/models/jurisprudence-check.model';

/** Métadonnées d'affichage d'un statut de vérification. */
interface StatutMeta {
  label: string;
  icon: string;
  /** Classe CSS du badge (couleur DESIGN_SYSTEM). */
  cssClass: string;
}

const STATUT_META: Record<JurisprudenceCheckStatut, StatutMeta> = {
  VERIFIED: { label: 'Vérifiée', icon: 'check_circle', cssClass: 'jc-badge--verified' },
  SUSPECT: { label: 'Suspecte', icon: 'warning', cssClass: 'jc-badge--suspect' },
  NOT_FOUND: { label: 'Non trouvée', icon: 'cancel', cssClass: 'jc-badge--not-found' },
  UNCERTAIN: { label: 'Incertaine', icon: 'help_outline', cssClass: 'jc-badge--uncertain' },
};

/** Un groupe de références rattachées à un même document. */
interface DocumentGroup {
  documentName: string;
  checks: JurisprudenceCheck[];
}

/**
 * F-179 SF-179-03 — section « Jurisprudences citées » de la page de synthèse.
 *
 * <p>Affiche, groupées par document, les références jurisprudentielles
 * détectées dans les documents uploadés, avec un badge de statut
 * (Vérifiée / Suspecte / Non trouvée / Incertaine), une explication courte
 * et un lien source quand disponible. Composant en lecture seule.</p>
 *
 * <p>SF-179-04 ajoute la mise en évidence des arrêts {@code SUSPECT} (alerte
 * de cohérence + popover).</p>
 */
@Component({
  selector: 'app-jurisprudence-citations-section',
  standalone: true,
  imports: [MatIconModule, MatExpansionModule, MatTooltipModule],
  templateUrl: './jurisprudence-citations-section.component.html',
  styleUrl: './jurisprudence-citations-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JurisprudenceCitationsSectionComponent {
  readonly checksSignal = signal<JurisprudenceCheck[]>([]);

  @Input() set checks(value: JurisprudenceCheck[] | null | undefined) {
    this.checksSignal.set(value ?? []);
  }

  /** Références groupées par document, ordre stable. */
  readonly groups = computed<DocumentGroup[]>(() => {
    const byDoc = new Map<string, JurisprudenceCheck[]>();
    for (const check of this.checksSignal()) {
      const key = check.documentName ?? 'Document';
      const list = byDoc.get(key) ?? [];
      list.push(check);
      byDoc.set(key, list);
    }
    return [...byDoc.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([documentName, checks]) => ({ documentName, checks }));
  });

  /** Nombre total de références. */
  readonly totalCount = computed(() => this.checksSignal().length);

  /** Nombre de références suspectes (cas le plus précieux). */
  readonly suspectCount = computed(
    () => this.checksSignal().filter((c) => c.statut === 'SUSPECT').length,
  );

  /**
   * true si toutes les références sont UNCERTAIN — l'UI affiche alors un
   * message invitant à la vérification manuelle (cf. cadrage écran).
   */
  readonly allUncertain = computed(() => {
    const checks = this.checksSignal();
    return checks.length > 0 && checks.every((c) => c.statut === 'UNCERTAIN');
  });

  /** Texte récapitulatif pour la description du panneau. */
  readonly summary = computed(() => {
    const total = this.totalCount();
    const suspect = this.suspectCount();
    const refLabel = `${total} référence${total > 1 ? 's' : ''}`;
    if (suspect > 0) {
      return `${refLabel} — ⚠️ ${suspect} suspecte${suspect > 1 ? 's' : ''}`;
    }
    return refLabel;
  });

  statutLabel(statut: JurisprudenceCheckStatut): string {
    return (STATUT_META[statut] ?? STATUT_META.UNCERTAIN).label;
  }

  statutIcon(statut: JurisprudenceCheckStatut): string {
    return (STATUT_META[statut] ?? STATUT_META.UNCERTAIN).icon;
  }

  statutClass(statut: JurisprudenceCheckStatut): string {
    return (STATUT_META[statut] ?? STATUT_META.UNCERTAIN).cssClass;
  }

  /** SF-179-04 — un check SUSPECT déclenche une alerte de cohérence. */
  isAlert(check: JurisprudenceCheck): boolean {
    return check.statut === 'SUSPECT';
  }
}

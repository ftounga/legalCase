import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  JurisprudenceCheck,
  JurisprudenceCheckStatut,
} from '../../core/models/jurisprudence-check.model';
import { JurisprudenceCheckService } from '../../core/services/jurisprudence-check.service';

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
  imports: [
    MatIconModule,
    MatExpansionModule,
    MatTooltipModule,
    MatButtonModule,
  ],
  templateUrl: './jurisprudence-citations-section.component.html',
  styleUrl: './jurisprudence-citations-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JurisprudenceCitationsSectionComponent {
  readonly checksSignal = signal<JurisprudenceCheck[]>([]);

  @Input() set checks(value: JurisprudenceCheck[] | null | undefined) {
    this.checksSignal.set(value ?? []);
  }

  /**
   * F-98 SF-98-56 — dossier auquel appartiennent les citations. Requis pour
   * persister le marquage « adverse à réfuter ». En son absence, l'action de
   * marquage n'est pas proposée (dégradation propre).
   */
  @Input() caseFileId: string | null = null;

  /** Id du check dont le marquage est en cours de persistance (PATCH). */
  readonly markingId = signal<string | null>(null);

  private readonly jurisprudenceCheckService = inject(JurisprudenceCheckService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

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

  /**
   * F-98 SF-98-56 — true si la citation peut être marquée « adverse à réfuter ».
   * Seuls les statuts SUSPECT / NOT_FOUND sont réfutables (VERIFIED = arrêt
   * valable, UNCERTAIN = silence > erreur).
   */
  canMarkAdverse(check: JurisprudenceCheck): boolean {
    return check.statut === 'SUSPECT' || check.statut === 'NOT_FOUND';
  }

  /**
   * F-98 SF-98-56 — true s'il existe au moins une citation marquable
   * (SUSPECT / NOT_FOUND) dans le panneau. Pilote la mention de continuité.
   */
  readonly hasMarkableCitations = computed(() =>
    this.checksSignal().some((c) => this.canMarkAdverse(c)),
  );

  /**
   * F-98 SF-98-56 — bascule le marquage « adverse à réfuter » d'une citation.
   *
   * <p>Persiste via le service (PATCH), met à jour l'état local du check dans
   * le `next` puis `markForCheck()` (OnPush). En cas d'échec, l'état local
   * n'est pas modifié et une snackbar d'erreur est affichée.</p>
   */
  toggleAdverse(check: JurisprudenceCheck): void {
    const caseFileId = this.caseFileId;
    if (!caseFileId || !this.canMarkAdverse(check) || this.markingId() !== null) {
      return;
    }
    const target = !check.markedAdverse;
    this.markingId.set(check.id);
    this.jurisprudenceCheckService
      .markAdverse(caseFileId, check.id, target)
      .subscribe({
        next: (updated) => {
          this.markingId.set(null);
          this.checksSignal.update((list) =>
            list.map((c) =>
              c.id === check.id
                ? { ...c, markedAdverse: updated.markedAdverse }
                : c,
            ),
          );
          this.cdr.markForCheck();
        },
        error: () => {
          this.markingId.set(null);
          this.snackBar.open(
            'Impossible de modifier le marquage de cette citation.',
            'Fermer',
            { duration: 4000, panelClass: ['snack-error'] },
          );
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * SF-179-04 — message du popover d'alerte de cohérence sur un arrêt SUSPECT :
   * confronte la position alléguée par le document à la réalité de l'arrêt
   * (explication backend). Esprit F-IA-03 : signaler une incohérence entre une
   * source citée et son contenu réel.
   */
  alertTooltip(check: JurisprudenceCheck): string {
    const parts: string[] = [
      'Position alléguée incohérente avec le contenu réel de l\'arrêt.',
    ];
    if (check.positionAlleguee) {
      parts.push(`Alléguée : ${check.positionAlleguee}`);
    }
    if (check.explication) {
      parts.push(`Réalité : ${check.explication}`);
    } else {
      parts.push('Arrêt réel mais position alléguée à vérifier.');
    }
    return parts.join('\n');
  }
}

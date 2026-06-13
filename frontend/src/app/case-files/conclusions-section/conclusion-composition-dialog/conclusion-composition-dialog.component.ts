import { ChangeDetectionStrategy, Component, Inject, computed, signal } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import {
  Composition,
  CompositionExclusion,
} from '../../../core/services/conclusion-composition.service';

/** Donnée d'entrée du modal : la composition lue via `GET …/composition`. */
export interface ConclusionCompositionDialogData {
  composition: Composition;
}

/**
 * Résultat de fermeture « Confirmer & générer » : l'ensemble des exclusions
 * (cases décochées). Une fermeture par « Annuler » renvoie `undefined`.
 */
export type ConclusionCompositionDialogResult = {
  exclusions: CompositionExclusion[];
};

/** État local d'un item dans le modal (case cochée = intégré). */
interface DialogItem {
  dimensionKey: string;
  key: string;
  label: string;
  checked: boolean;
}

/** Une section repliée par dimension. */
interface DialogDimension {
  key: string;
  label: string;
  items: DialogItem[];
}

/**
 * F-288 / SF-288-01 — Modal de **composition** des conclusions.
 *
 * Déclenché au clic « Générer » / « Régénérer » : l'avocat décoche les
 * éléments curables (vague 1 : outils décisionnels calculés) qu'il ne veut pas
 * verser dans l'acte, puis « Confirmer & générer ». Le modal est pré-coché
 * selon l'ensemble d'exclusions persisté (`included = false` ⇒ décoché). S'il
 * décoche tout, un avertissement non bloquant apparaît mais la génération reste
 * permise.
 *
 * Ce n'est PAS un outil décisionnel (pas de `TOOL_REGISTRY`, pas de calcul) :
 * c'est un sélecteur de composition qui retourne l'ensemble des `itemKey`
 * exclus. Standalone, OnPush + signals (état local muté par les cases).
 */
@Component({
  selector: 'app-conclusion-composition-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  templateUrl: './conclusion-composition-dialog.component.html',
  styleUrl: './conclusion-composition-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConclusionCompositionDialogComponent {
  /** Dimensions + items, état coché local (signal pour réactivité OnPush). */
  readonly dimensions = signal<DialogDimension[]>([]);

  /**
   * Vrai si AUCUN item n'est coché sur l'ensemble des dimensions : l'acte sera
   * fondé sur les seuls faits/pièces → avertissement non bloquant.
   */
  readonly allUnchecked = computed<boolean>(() =>
    this.dimensions().every((d) => d.items.every((i) => !i.checked)),
  );

  constructor(
    private readonly dialogRef: MatDialogRef<
      ConclusionCompositionDialogComponent,
      ConclusionCompositionDialogResult | undefined
    >,
    @Inject(MAT_DIALOG_DATA) data: ConclusionCompositionDialogData,
  ) {
    this.dimensions.set(
      (data.composition?.dimensions ?? []).map((dim) => ({
        key: dim.key,
        label: dim.label,
        items: (dim.items ?? []).map((item) => ({
          dimensionKey: dim.key,
          key: item.key,
          label: item.label,
          checked: item.included,
        })),
      })),
    );
  }

  /** Bascule la case d'un item (clic checkbox). */
  toggle(dimensionKey: string, itemKey: string, checked: boolean): void {
    this.dimensions.update((dims) =>
      dims.map((d) =>
        d.key !== dimensionKey
          ? d
          : {
              ...d,
              items: d.items.map((i) =>
                i.key === itemKey ? { ...i, checked } : i,
              ),
            },
      ),
    );
  }

  /** « Tout cocher » pour une dimension. */
  checkAll(dimensionKey: string): void {
    this.setAll(dimensionKey, true);
  }

  /** « Tout décocher » pour une dimension. */
  uncheckAll(dimensionKey: string): void {
    this.setAll(dimensionKey, false);
  }

  private setAll(dimensionKey: string, checked: boolean): void {
    this.dimensions.update((dims) =>
      dims.map((d) =>
        d.key !== dimensionKey
          ? d
          : { ...d, items: d.items.map((i) => ({ ...i, checked })) },
      ),
    );
  }

  /** « Annuler » : ferme sans générer (résultat `undefined`). */
  cancel(): void {
    this.dialogRef.close(undefined);
  }

  /**
   * « Confirmer & générer » : ferme en renvoyant l'ensemble des exclusions
   * (toutes les cases **décochées**, toutes dimensions confondues).
   */
  confirm(): void {
    const exclusions: CompositionExclusion[] = [];
    for (const dim of this.dimensions()) {
      for (const item of dim.items) {
        if (!item.checked) {
          exclusions.push({ dimension: dim.key, itemKey: item.key });
        }
      }
    }
    this.dialogRef.close({ exclusions });
  }
}

import { Injectable, Type, inject } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { DecisionToolModalComponent } from './decision-tool-modal.component';

export interface DecisionToolModalArgs {
  toolId: string;
  title: string;
  icon: string;
  component: Type<unknown>;
  inputs: Record<string, unknown>;
  /** Callback invoqué quand l'utilisateur clique "Enregistrer".
   *  Retourne `true` si l'outil a accepté (ferme le dialog), `false` sinon (laisse ouvert).
   *  Si non défini, le bouton "Enregistrer" n'est pas affiché (cas outil display-only). */
  onSave?: () => boolean | Promise<boolean>;
}

export type DecisionToolModalCloseValue = 'saved' | undefined;

/**
 * F-177 SF-177-02 — Service d'ouverture d'un MatDialog hébergeant un composant outil décisionnel.
 *
 * Centralise la config (width 90vw, maxWidth 1200px, height 90vh, maxHeight 900px) et
 * permet aux consommateurs (panel F-IA-04, dashboard agrégé) de ne fournir que le composant
 * outil + ses inputs + un callback de save.
 *
 * Consommé par :
 * - SF-177-09 : dashboard agrégé en haut de page dossier
 * - SF-177-11 : intégration globale panel F-IA-04 après instrumentation des outils
 */
@Injectable({ providedIn: 'root' })
export class DecisionToolModalService {
  private readonly dialog = inject(MatDialog);

  open(args: DecisionToolModalArgs): MatDialogRef<DecisionToolModalComponent, DecisionToolModalCloseValue> {
    return this.dialog.open<DecisionToolModalComponent, DecisionToolModalArgs, DecisionToolModalCloseValue>(
      DecisionToolModalComponent,
      {
        data: args,
        width: '90vw',
        maxWidth: '1200px',
        height: '90vh',
        maxHeight: '900px',
        panelClass: 'decision-tool-modal-panel',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
      },
    );
  }
}

import {
  ChangeDetectorRef,
  DestroyRef,
  Directive,
  ElementRef,
  Renderer2,
  effect,
  inject,
} from '@angular/core';
import { WorkspaceStateService } from '../../core/services/workspace-state.service';

/**
 * F-156 SF-156-02 — directive partagée qui désactive visuellement l'élément hôte
 * quand le workspace courant est en `PENDING_PAYMENT` (CA9) et ajoute un tooltip
 * via `title=` expliquant pourquoi.
 *
 * <p>Usage :
 * <pre>
 *   &lt;button mat-flat-button appDisabledIfPendingPayment (click)="..."&gt;...&lt;/button&gt;
 * </pre>
 *
 * <p>Comportement :
 * <ul>
 *   <li>`disabled = true` sur l'élément hôte (compatible boutons Material — Angular Material
 *       répercute l'attribut sur le bouton natif).</li>
 *   <li>Ajout d'une classe CSS `is-disabled-pending-payment` pour styling éventuel.</li>
 *   <li>`title` natif = « Workspace en attente d'activation Stripe » — permet un tooltip
 *       sans dépendance Material additionnelle.</li>
 * </ul>
 */
@Directive({
  selector: '[appDisabledIfPendingPayment]',
  standalone: true,
})
export class DisabledIfPendingPaymentDirective {
  private readonly state = inject(WorkspaceStateService);
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const disabled = this.state.isPendingPayment();
      const host = this.el.nativeElement;
      if (!host) return;
      if (disabled) {
        this.renderer.setAttribute(host, 'disabled', 'true');
        this.renderer.setAttribute(
          host,
          'title',
          'Workspace en attente d\'activation Stripe — disponible dès paiement confirmé.'
        );
        this.renderer.addClass(host, 'is-disabled-pending-payment');
        this.renderer.setAttribute(host, 'aria-disabled', 'true');
      } else {
        this.renderer.removeAttribute(host, 'disabled');
        this.renderer.removeAttribute(host, 'title');
        this.renderer.removeClass(host, 'is-disabled-pending-payment');
        this.renderer.removeAttribute(host, 'aria-disabled');
      }
      this.cdr.markForCheck();
    });
  }
}

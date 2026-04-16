import {
  ComponentRef,
  Directive,
  ElementRef,
  HostListener,
  Input,
  OnDestroy,
  ViewContainerRef,
} from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherenceSourceNavigator } from '../../core/services/coherence-source-navigator.service';
import { CoherencePopoverComponent } from './coherence-popover.component';

/**
 * Directive à attacher à un badge d'incohérence (SF-IA-03-15b).
 * Ouvre un popover enrichi au hover (CDK Overlay + CoherencePopoverComponent)
 * et dispatche la navigation vers la source au clic sur le lien.
 *
 * Usage :
 *   <span [appCoherencePopover]="explanationFor('CONVENTION')"
 *         [appCoherencePopoverReason]="reasonFor('CONVENTION')"
 *         [appCoherencePopoverCaseFileId]="caseFileId"
 *         [appCoherencePopoverBlocker]="false">...</span>
 */
@Directive({
  selector: '[appCoherencePopover]',
  standalone: true,
})
export class CoherencePopoverTriggerDirective implements OnDestroy {
  @Input('appCoherencePopover') explanation: SourceExplanation | null = null;
  @Input('appCoherencePopoverReason') reason = '';
  @Input('appCoherencePopoverCaseFileId') caseFileId = '';
  @Input('appCoherencePopoverBlocker') blocker = false;

  private overlayRef: OverlayRef | null = null;
  private componentRef: ComponentRef<CoherencePopoverComponent> | null = null;
  private closeTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private overlay: Overlay,
    private elementRef: ElementRef<HTMLElement>,
    private viewContainer: ViewContainerRef,
    private navigator: CoherenceSourceNavigator,
  ) {}

  @HostListener('mouseenter')
  onMouseEnter(): void {
    this.cancelScheduledClose();
    if (this.overlayRef) return;
    this.openPopover();
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.scheduleClose();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closePopover();
  }

  private openPopover(): void {
    const positionStrategy = this.overlay
      .position()
      .flexibleConnectedTo(this.elementRef.nativeElement)
      .withPositions([
        { originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom', offsetY: -8 },
        { originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetY: 8 },
      ])
      .withFlexibleDimensions(false)
      .withPush(true);

    this.overlayRef = this.overlay.create({
      positionStrategy,
      scrollStrategy: this.overlay.scrollStrategies.reposition(),
      hasBackdrop: false,
    });

    const portal = new ComponentPortal(CoherencePopoverComponent, this.viewContainer);
    this.componentRef = this.overlayRef.attach(portal);
    this.componentRef.instance.explanation = this.explanation;
    this.componentRef.instance.reason = this.reason;
    this.componentRef.instance.blocker = this.blocker;
    this.componentRef.instance.onAction = () => this.onSourceClicked();

    const overlayEl = this.overlayRef.overlayElement;
    overlayEl.addEventListener('mouseenter', this.cancelScheduledClose);
    overlayEl.addEventListener('mouseleave', this.scheduleClose);
  }

  private onSourceClicked(): void {
    if (!this.explanation || !this.caseFileId) return;
    this.navigator.navigate(this.caseFileId, this.explanation.actionType, this.explanation.actionTarget);
    this.closePopover();
  }

  private scheduleClose = (): void => {
    this.cancelScheduledClose();
    this.closeTimer = setTimeout(() => this.closePopover(), 150);
  };

  private cancelScheduledClose = (): void => {
    if (this.closeTimer) {
      clearTimeout(this.closeTimer);
      this.closeTimer = null;
    }
  };

  private closePopover(): void {
    this.cancelScheduledClose();
    if (this.overlayRef) {
      this.overlayRef.dispose();
      this.overlayRef = null;
      this.componentRef = null;
    }
  }

  ngOnDestroy(): void {
    this.closePopover();
  }
}

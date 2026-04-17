import { Component } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { CoherencePopoverTriggerDirective } from './coherence-popover-trigger.directive';
import { CoherenceSourceNavigator } from '../../core/services/coherence-source-navigator.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';

@Component({
  standalone: true,
  imports: [CoherencePopoverTriggerDirective],
  template: `
    <span id="badge"
          [appCoherencePopover]="explanation"
          [appCoherencePopoverReason]="reason"
          [appCoherencePopoverCaseFileId]="caseFileId"
          [appCoherencePopoverBlocker]="blocker">
      Badge
    </span>
  `,
})
class HostComponent {
  explanation: SourceExplanation[] | null = null;
  reason = 'Raison test';
  caseFileId = 'case-1';
  blocker = false;
}

describe('CoherencePopoverTriggerDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let component: HostComponent;
  let navigator: jest.Mocked<CoherenceSourceNavigator>;

  beforeEach(async () => {
    navigator = { navigate: jest.fn() } as any;
    await TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [
        provideAnimationsAsync(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CoherenceSourceNavigator, useValue: navigator },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function triggerMouse(event: 'mouseenter' | 'mouseleave'): void {
    const badge = fixture.nativeElement.querySelector('#badge') as HTMLElement;
    badge.dispatchEvent(new MouseEvent(event));
    fixture.detectChanges();
  }

  it('ouvre le popover au mouseenter', () => {
    triggerMouse('mouseenter');
    const popover = document.querySelector('app-coherence-popover');
    expect(popover).not.toBeNull();
  });

  it('ferme le popover après mouseleave + délai', async () => {
    triggerMouse('mouseenter');
    triggerMouse('mouseleave');
    await new Promise(r => setTimeout(r, 250));
    fixture.detectChanges();
    const popover = document.querySelector('app-coherence-popover');
    expect(popover).toBeNull();
  });

  it('ferme le popover à Escape', () => {
    triggerMouse('mouseenter');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    const popover = document.querySelector('app-coherence-popover');
    expect(popover).toBeNull();
  });

  it('clique sur le lien → navigator appelé', () => {
    component.explanation = [{
      sourceKey: 'FR_CONVOCATION',
      sourceType: 'DOCUMENT',
      label: 'contrat.pdf',
      sentence: 'Test',
      secondaryText: 'Clause 6.2',
      actionType: 'OPEN_DOCUMENT',
      actionTarget: 'doc-1',
    }];
    fixture.detectChanges();
    triggerMouse('mouseenter');
    const link = document.querySelector('.source-link') as HTMLElement;
    expect(link).not.toBeNull();
    link.click();
    expect(navigator.navigate).toHaveBeenCalledWith('case-1', 'OPEN_DOCUMENT', 'doc-1');
  });

  it('pas de lien si actionType NONE', () => {
    component.explanation = [{
      sourceKey: 'k', sourceType: 'ANALYSIS_DETECTION', label: 'l', sentence: 's',
      secondaryText: null, actionType: 'NONE', actionTarget: null,
    }];
    fixture.detectChanges();
    triggerMouse('mouseenter');
    const link = document.querySelector('.source-link');
    expect(link).toBeNull();
  });
});

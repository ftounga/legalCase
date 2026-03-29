import { TestBed, ComponentFixture } from '@angular/core/testing';
import { signal } from '@angular/core';
import { TourOverlayComponent } from './tour-overlay.component';
import { TourService } from '../core/services/tour.service';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

function makeTourServiceStub(step: number) {
  return {
    currentStep: signal(step),
    isActive: signal(true),
    next: jasmine.createSpy('next'),
    skip: jasmine.createSpy('skip')
  };
}

describe('TourOverlayComponent', () => {
  let fixture: ComponentFixture<TourOverlayComponent>;
  let stub: ReturnType<typeof makeTourServiceStub>;

  function setup(step: number): void {
    stub = makeTourServiceStub(step);
    TestBed.configureTestingModule({
      imports: [TourOverlayComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        { provide: TourService, useValue: stub }
      ]
    });
    fixture = TestBed.createComponent(TourOverlayComponent);
    fixture.detectChanges();
  }

  // U-08 : étape 0 — titre "Bienvenue" affiché
  it('U-08: step 0 shows welcome title', () => {
    setup(0);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Bienvenue');
  });

  // U-09 : étape 4 — bouton "Commencer" affiché
  it('U-09: last step shows "Commencer" button', () => {
    setup(4);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Commencer');
  });

  // U-10 : clic "Passer" → tourService.skip() appelé
  it('U-10: clicking skip calls tourService.skip()', () => {
    setup(0);
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.tour-card__skip');
    btn.click();
    expect(stub.skip).toHaveBeenCalled();
  });

  // U-11 : clic "Suivant" → tourService.next() appelé
  it('U-11: clicking next calls tourService.next()', () => {
    setup(0);
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    const nextBtn = Array.from(buttons).find(b => b.textContent?.includes('Suivant'));
    nextBtn?.click();
    expect(stub.next).toHaveBeenCalled();
  });
});

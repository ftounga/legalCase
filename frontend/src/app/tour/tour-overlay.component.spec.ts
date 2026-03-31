import { TestBed, ComponentFixture } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { TourOverlayComponent } from './tour-overlay.component';
import { TourService } from '../core/services/tour.service';
import { CaseFileService } from '../core/services/case-file.service';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

function makeTourServiceStub(step: number) {
  return {
    currentStep: signal(step),
    isActive: signal(true),
    next: jasmine.createSpy('next'),
    skip: jasmine.createSpy('skip'),
    advanceToStep2: jasmine.createSpy('advanceToStep2')
  };
}

describe('TourOverlayComponent', () => {
  let fixture: ComponentFixture<TourOverlayComponent>;
  let stub: ReturnType<typeof makeTourServiceStub>;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;

  function setup(step: number): void {
    stub = makeTourServiceStub(step);
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['list', 'create', 'update', 'exportZip']);
    caseFileServiceSpy.list.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 1 }));

    TestBed.configureTestingModule({
      imports: [TourOverlayComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        { provide: TourService, useValue: stub },
        { provide: CaseFileService, useValue: caseFileServiceSpy }
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

  // U-11 : clic "Suivant" à step 0 → tourService.next() appelé
  it('U-11: clicking next at step 0 calls tourService.next()', () => {
    setup(0);
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    const nextBtn = Array.from(buttons).find(b => b.textContent?.includes('Suivant'));
    nextBtn?.click();
    expect(stub.next).toHaveBeenCalled();
    expect(stub.advanceToStep2).not.toHaveBeenCalled();
  });

  // U-12 : clic "Suivant" à step 1 → caseFileService.list() puis tourService.advanceToStep2() appelés
  it('U-12: clicking next at step 1 calls advanceToStep2() with correct flag', async () => {
    setup(1);
    caseFileServiceSpy.list.and.returnValue(of({ content: [], totalElements: 3, totalPages: 1, number: 0, size: 1 }));

    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button');
    const nextBtn = Array.from(buttons).find(b => b.textContent?.includes('Suivant'));
    nextBtn?.click();

    await fixture.whenStable();
    expect(caseFileServiceSpy.list).toHaveBeenCalledWith(0, 1);
    expect(stub.advanceToStep2).toHaveBeenCalledWith(true);
    expect(stub.next).not.toHaveBeenCalled();
  });
});

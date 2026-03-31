import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TourService } from './tour.service';
import { CaseFileService } from './case-file.service';
import { CaseFileStatusService } from './case-file-status.service';

describe('TourService', () => {
  let service: TourService;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let caseFileStatusServiceSpy: jasmine.SpyObj<CaseFileStatusService>;
  let router: Router;

  const WS_ID = 'ws-test-1';
  const STORAGE_KEY = 'onboarding_tour_done_' + WS_ID;

  beforeEach(() => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['create']);
    caseFileStatusServiceSpy = jasmine.createSpyObj('CaseFileStatusService', ['delete']);
    caseFileStatusServiceSpy.delete.and.returnValue(of(undefined));

    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: CaseFileStatusService, useValue: caseFileStatusServiceSpy }
      ]
    });
    service = TestBed.inject(TourService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
  });

  afterEach(() => localStorage.clear());

  // U-01 : shouldShow — clé absente → true
  it('U-01: shouldShow returns true when key absent', () => {
    expect(service.shouldShow(WS_ID)).toBeTrue();
  });

  // U-02 : shouldShow — clé présente → false
  it('U-02: shouldShow returns false when key present', () => {
    localStorage.setItem(STORAGE_KEY, '1');
    expect(service.shouldShow(WS_ID)).toBeFalse();
  });

  // U-03 : shouldShow — workspaceId null → false
  it('U-03: shouldShow returns false for null workspaceId', () => {
    expect(service.shouldShow(null)).toBeFalse();
  });

  // U-04 : start() → isActive = true, currentStep = 0
  it('U-04: start() activates tour at step 0', () => {
    service.start(WS_ID);
    expect(service.isActive()).toBeTrue();
    expect(service.currentStep()).toBe(0);
  });

  // U-05 : next() → currentStep = 1
  it('U-05: next() advances to step 1', () => {
    service.start(WS_ID);
    service.next();
    expect(service.currentStep()).toBe(1);
  });

  // U-06 : next() sur étape 4 → isActive = false, localStorage posé
  it('U-06: next() on last step stops tour and sets localStorage', () => {
    service.start(WS_ID);
    service.next(); // → 1
    service.next(); // → 2
    service.next(); // → 3
    service.next(); // → 4
    service.next(); // → stop (5 >= TOTAL_STEPS)
    expect(service.isActive()).toBeFalse();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('1');
  });

  // U-07 : skip() → isActive = false, localStorage posé
  it('U-07: skip() stops tour and sets localStorage', () => {
    service.start(WS_ID);
    service.skip();
    expect(service.isActive()).toBeFalse();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('1');
  });

  // U-08 : advanceToStep2 — 0 dossiers → CaseFileService.create() appelé avec le bon titre
  it('U-08: advanceToStep2 with no files calls create() with demo title', fakeAsync(() => {
    caseFileServiceSpy.create.and.returnValue(of({ id: 'cf-demo-1', title: 'Dossier de démonstration' } as any));
    service.start(WS_ID);

    service.advanceToStep2(false);
    tick();

    expect(caseFileServiceSpy.create).toHaveBeenCalledOnceWith({ title: 'Dossier de démonstration' });
  }));

  // U-09 : advanceToStep2 — 0 dossiers → navigation vers /case-files/{id}
  it('U-09: advanceToStep2 with no files navigates to demo case file', fakeAsync(() => {
    caseFileServiceSpy.create.and.returnValue(of({ id: 'cf-demo-1', title: 'Dossier de démonstration' } as any));
    service.start(WS_ID);

    service.advanceToStep2(false);
    tick();

    expect(router.navigate).toHaveBeenCalledWith(['/case-files', 'cf-demo-1']);
    expect(service.currentStep()).toBe(2);
    expect(service.demoCaseFileId()).toBe('cf-demo-1');
  }));

  // U-10 : stop() après création demo → CaseFileStatusService.delete() appelé
  it('U-10: stop() after demo creation calls delete() with demo id', fakeAsync(() => {
    caseFileServiceSpy.create.and.returnValue(of({ id: 'cf-demo-1', title: 'Dossier de démonstration' } as any));
    service.start(WS_ID);
    service.advanceToStep2(false);
    tick();

    service.skip(); // calls stop() → cleanup()

    expect(caseFileStatusServiceSpy.delete).toHaveBeenCalledWith('cf-demo-1');
    expect(service.demoCaseFileId()).toBeNull();
  }));

  // U-11 : skip() sans dossier demo → delete() NON appelé
  it('U-11: skip() without demo case file does not call delete()', () => {
    service.start(WS_ID);
    service.skip();
    expect(caseFileStatusServiceSpy.delete).not.toHaveBeenCalled();
  });

  // U-12 : advanceToStep2 — ≥1 dossiers → create() NON appelé
  it('U-12: advanceToStep2 with existing files does not call create()', () => {
    service.start(WS_ID);
    service.advanceToStep2(true);
    expect(caseFileServiceSpy.create).not.toHaveBeenCalled();
    expect(service.currentStep()).toBe(2);
  });

  // U-13 : advanceToStep2 — échec create → step 2 sans bloquer, demoCaseFileId null
  it('U-13: advanceToStep2 create failure advances to step 2, demoCaseFileId stays null', fakeAsync(() => {
    caseFileServiceSpy.create.and.returnValue(throwError(() => new Error('Network error')));
    service.start(WS_ID);

    service.advanceToStep2(false);
    tick();

    expect(service.currentStep()).toBe(2);
    expect(service.demoCaseFileId()).toBeNull();
  }));
});

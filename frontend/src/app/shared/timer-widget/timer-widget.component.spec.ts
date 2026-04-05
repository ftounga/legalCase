import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TimerWidgetComponent } from './timer-widget.component';
import { TimeService } from '../../core/services/time.service';
import { AuthService } from '../../core/services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { BillingRateResponse, TimeEntryResponse } from '../../core/models/time-tracking.models';
import { signal } from '@angular/core';

const CURRENT_USER_ID = 'user-1';

const mockEntry = (overrides: Partial<TimeEntryResponse> = {}): TimeEntryResponse => ({
  id: 'entry-1',
  caseFileId: 'case-1',
  userId: CURRENT_USER_ID,
  startedAt: new Date().toISOString(),
  ...overrides
});

const mockRate: BillingRateResponse = { ratePerHour: 200, effectiveFrom: '2026-01-01' };

describe('TimerWidgetComponent', () => {
  let component: TimerWidgetComponent;
  let fixture: ComponentFixture<TimerWidgetComponent>;
  let timeServiceSpy: any;
  let snackBarSpy: jest.Mocked<MatSnackBar>;
  let authServiceSpy: any;

  beforeEach(async () => {
    const activeEntry = signal<TimeEntryResponse | null>(null);
    const entries = signal<TimeEntryResponse[]>([]);
    const currentUser = signal<{ id: string } | null>({ id: CURRENT_USER_ID });

    timeServiceSpy = {
      activeEntry,
      entries,
      loadEntries: jest.fn().mockReturnValue(of(void 0)),
      getBillingRate: jest.fn().mockReturnValue(of(mockRate)),
      startTimer: jest.fn(),
      stopTimer: jest.fn(),
      formatDuration: jest.fn((s: number) => `${s}s`)
    };

    snackBarSpy = { open: jest.fn() } as any;
    authServiceSpy = { currentUser };

    await TestBed.configureTestingModule({
      imports: [TimerWidgetComponent, NoopAnimationsModule],
      providers: [
        { provide: TimeService, useValue: timeServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TimerWidgetComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    fixture.detectChanges();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('affiche le bouton "Démarrer le chrono" quand taux configuré et pas de timer actif', () => {
    timeServiceSpy.activeEntry.set(null);
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('.timer-btn--start');
    expect(btn).toBeTruthy();
    expect(btn.textContent).toContain('Démarrer le chrono');
  });

  it('affiche le bouton "Arrêter" quand un timer est actif', () => {
    timeServiceSpy.activeEntry.set(mockEntry());
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('.timer-btn--stop');
    expect(btn).toBeTruthy();
    expect(btn.textContent).toContain('Arrêter');
  });

  it('affiche le chrono HH:MM:SS quand un timer est actif', () => {
    timeServiceSpy.activeEntry.set(mockEntry());
    component.elapsedSeconds.set(90);
    fixture.detectChanges();
    const display = fixture.nativeElement.querySelector('.timer-display');
    expect(display).toBeTruthy();
    expect(display.textContent).toBe('00:01:30');
  });

  it('affiche le warning et masque le chrono si aucun taux configuré', () => {
    timeServiceSpy.getBillingRate.mockReturnValue(of(null));
    component.noRateConfigured.set(true);
    fixture.detectChanges();
    const warning = fixture.nativeElement.querySelector('.no-rate-warning');
    const btn = fixture.nativeElement.querySelector('.timer-btn--start');
    expect(warning).toBeTruthy();
    expect(btn).toBeNull();
  });

  it('totalSeconds() inclut les entrées terminées et l\'elapsed actif', () => {
    const done = mockEntry({ durationSeconds: 3600, stoppedAt: new Date().toISOString() });
    timeServiceSpy.entries.set([done]);
    timeServiceSpy.activeEntry.set(mockEntry());
    component.elapsedSeconds.set(600);
    fixture.detectChanges();
    expect(component.totalSeconds()).toBe(4200);
  });

  it('affiche le total enregistré quand totalSeconds > 0', () => {
    const done = mockEntry({ durationSeconds: 3600, stoppedAt: new Date().toISOString() });
    timeServiceSpy.entries.set([done]);
    fixture.detectChanges();
    const total = fixture.nativeElement.querySelector('.timer-total');
    expect(total).toBeTruthy();
  });

  it('n\'affiche pas le total si totalSeconds === 0', () => {
    timeServiceSpy.entries.set([]);
    fixture.detectChanges();
    const total = fixture.nativeElement.querySelector('.timer-total');
    expect(total).toBeNull();
  });

  describe('onStart()', () => {
    it('appelle startTimer et met à jour l\'état', () => {
      const entry = mockEntry();
      timeServiceSpy.startTimer.mockReturnValue(of(entry));
      component.onStart();
      expect(timeServiceSpy.startTimer).toHaveBeenCalledWith('case-1');
    });

    it('gère l\'erreur 409 — bloque le bouton et affiche un message', () => {
      timeServiceSpy.startTimer.mockReturnValue(throwError(() => ({ status: 409 })));
      component.onStart();
      expect(component.blockedByOtherTimer()).toBe(true);
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        expect.stringContaining('Un timer est déjà actif'),
        'Fermer',
        expect.any(Object)
      );
    });
  });

  describe('onStop()', () => {
    it('appelle stopTimer avec l\'id de l\'entrée active', () => {
      const entry = mockEntry();
      const stopped = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 60 });
      timeServiceSpy.activeEntry.set(entry);
      timeServiceSpy.stopTimer.mockReturnValue(of(stopped));
      component.onStop();
      expect(timeServiceSpy.stopTimer).toHaveBeenCalledWith('entry-1');
    });

    it('réinitialise elapsedSeconds à 0 après stop', () => {
      const entry = mockEntry();
      const stopped = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 60 });
      timeServiceSpy.activeEntry.set(entry);
      timeServiceSpy.stopTimer.mockReturnValue(of(stopped));
      component.elapsedSeconds.set(120);
      component.onStop();
      expect(component.elapsedSeconds()).toBe(0);
    });
  });

  describe('formattedElapsed', () => {
    it('formate correctement 3661 secondes en HH:MM:SS', () => {
      component.elapsedSeconds.set(3661);
      expect(component.formattedElapsed).toBe('01:01:01');
    });

    it('formate correctement 0 secondes', () => {
      component.elapsedSeconds.set(0);
      expect(component.formattedElapsed).toBe('00:00:00');
    });
  });
});

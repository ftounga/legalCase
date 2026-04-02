import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { TimerWidgetComponent } from './timer-widget.component';
import { TimeService } from '../../core/services/time.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TimeEntryResponse } from '../../core/models/time-tracking.models';
import { signal } from '@angular/core';

const mockEntry = (overrides: Partial<TimeEntryResponse> = {}): TimeEntryResponse => ({
  id: 'entry-1',
  caseFileId: 'case-1',
  startedAt: new Date().toISOString(),
  ...overrides
});

describe('TimerWidgetComponent', () => {
  let component: TimerWidgetComponent;
  let fixture: ComponentFixture<TimerWidgetComponent>;
  let timeServiceSpy: jest.Mocked<Partial<TimeService>> & { activeEntry: ReturnType<typeof signal<TimeEntryResponse | null>>; entries: ReturnType<typeof signal<TimeEntryResponse[]>> };
  let snackBarSpy: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    const activeEntry = signal<TimeEntryResponse | null>(null);
    const entries = signal<TimeEntryResponse[]>([]);

    timeServiceSpy = {
      activeEntry,
      entries,
      loadEntries: jest.fn().mockReturnValue(of(void 0)),
      startTimer: jest.fn(),
      stopTimer: jest.fn(),
      formatDuration: jest.fn((s: number) => `${s}s`)
    } as any;

    snackBarSpy = { open: jest.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [TimerWidgetComponent, NoopAnimationsModule],
      providers: [
        { provide: TimeService, useValue: timeServiceSpy },
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

  it('affiche le bouton "Démarrer le chrono" quand pas de timer actif', () => {
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
    component.elapsedSeconds.set(90); // 00:01:30
    fixture.detectChanges();
    const display = fixture.nativeElement.querySelector('.timer-display');
    expect(display).toBeTruthy();
    expect(display.textContent).toBe('00:01:30');
  });

  describe('onStart()', () => {
    it('appelle startTimer et met à jour l\'état', () => {
      const entry = mockEntry();
      timeServiceSpy.startTimer!.mockReturnValue(of(entry));

      component.onStart();

      expect(timeServiceSpy.startTimer).toHaveBeenCalledWith('case-1');
    });

    it('gère l\'erreur 409 — bloque le bouton et affiche un message', () => {
      timeServiceSpy.startTimer!.mockReturnValue(throwError(() => ({ status: 409 })));

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
      timeServiceSpy.stopTimer!.mockReturnValue(of(stopped));

      component.onStop();

      expect(timeServiceSpy.stopTimer).toHaveBeenCalledWith('entry-1');
    });

    it('réinitialise elapsedSeconds à 0 après stop', () => {
      const entry = mockEntry();
      const stopped = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 60 });
      timeServiceSpy.activeEntry.set(entry);
      timeServiceSpy.stopTimer!.mockReturnValue(of(stopped));
      component.elapsedSeconds.set(120);

      component.onStop();

      expect(component.elapsedSeconds()).toBe(0);
    });
  });

  describe('liste des sessions', () => {
    it('affiche la liste des entrées triées par date décroissante', () => {
      const older = mockEntry({ id: 'e1', startedAt: '2026-04-01T08:00:00Z', stoppedAt: '2026-04-01T09:00:00Z', durationSeconds: 3600 });
      const newer = mockEntry({ id: 'e2', startedAt: '2026-04-02T08:00:00Z', stoppedAt: '2026-04-02T09:00:00Z', durationSeconds: 3600 });
      timeServiceSpy.entries.set([older, newer]);
      fixture.detectChanges();

      const items = fixture.nativeElement.querySelectorAll('.session-item');
      expect(items.length).toBe(2);
      // La plus récente (e2) doit être première
      expect(items[0].textContent).toContain('02/04/2026');
    });

    it('affiche le badge "En cours" pour les entrées sans stoppedAt', () => {
      timeServiceSpy.entries.set([mockEntry()]);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.session-badge--active');
      expect(badge).toBeTruthy();
      expect(badge.textContent).toContain('En cours');
    });

    it('affiche le badge "Terminé" pour les entrées avec stoppedAt', () => {
      const done = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 120 });
      timeServiceSpy.entries.set([done]);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.session-badge--done');
      expect(badge).toBeTruthy();
      expect(badge.textContent).toContain('Terminé');
    });

    it('n\'affiche pas plus de 10 entrées', () => {
      const manyEntries = Array.from({ length: 15 }, (_, i) =>
        mockEntry({
          id: `e${i}`,
          startedAt: new Date(Date.now() - i * 3600000).toISOString(),
          stoppedAt: new Date(Date.now() - i * 3600000 + 1800000).toISOString(),
          durationSeconds: 1800
        })
      );
      timeServiceSpy.entries.set(manyEntries);
      fixture.detectChanges();

      const items = fixture.nativeElement.querySelectorAll('.session-item');
      expect(items.length).toBe(10);
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

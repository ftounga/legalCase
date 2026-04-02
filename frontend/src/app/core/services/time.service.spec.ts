import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TimeService } from './time.service';
import { TimeEntryResponse } from '../models/time-tracking.models';

const mockEntry = (overrides: Partial<TimeEntryResponse> = {}): TimeEntryResponse => ({
  id: 'entry-1',
  caseFileId: 'case-1',
  startedAt: new Date().toISOString(),
  ...overrides
});

describe('TimeService', () => {
  let service: TimeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TimeService]
    });
    service = TestBed.inject(TimeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('startTimer()', () => {
    it('met à jour le signal activeEntry après succès', () => {
      const entry = mockEntry();
      let result: TimeEntryResponse | undefined;

      service.startTimer('case-1').subscribe(e => (result = e));

      const req = http.expectOne('/api/v1/case-files/case-1/time-entries/start');
      expect(req.request.method).toBe('POST');
      req.flush(entry);

      expect(result).toEqual(entry);
      expect(service.activeEntry()).toEqual(entry);
    });
  });

  describe('stopTimer()', () => {
    it('efface le signal activeEntry après succès', () => {
      const active = mockEntry();
      const stopped = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 120 });

      service.activeEntry.set(active);
      service.entries.set([active]);

      service.stopTimer('entry-1').subscribe();

      const req = http.expectOne('/api/v1/time-entries/entry-1/stop');
      expect(req.request.method).toBe('POST');
      req.flush(stopped);

      expect(service.activeEntry()).toBeNull();
    });

    it('met à jour l\'entrée dans la liste des entries', () => {
      const active = mockEntry();
      const stopped = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 90 });

      service.entries.set([active]);

      service.stopTimer('entry-1').subscribe();
      http.expectOne('/api/v1/time-entries/entry-1/stop').flush(stopped);

      expect(service.entries()[0].stoppedAt).toBeDefined();
      expect(service.entries()[0].durationSeconds).toBe(90);
    });
  });

  describe('loadEntries()', () => {
    it('initialise entries et détecte un timer actif (sans stoppedAt)', () => {
      const active = mockEntry();
      const done = mockEntry({ id: 'entry-2', stoppedAt: new Date().toISOString(), durationSeconds: 300 });

      service.loadEntries('case-1').subscribe();
      http.expectOne('/api/v1/case-files/case-1/time-entries').flush([active, done]);

      expect(service.entries().length).toBe(2);
      expect(service.activeEntry()).toEqual(active);
    });

    it('activeEntry est null si toutes les entrées sont terminées', () => {
      const done = mockEntry({ stoppedAt: new Date().toISOString(), durationSeconds: 120 });

      service.loadEntries('case-1').subscribe();
      http.expectOne('/api/v1/case-files/case-1/time-entries').flush([done]);

      expect(service.activeEntry()).toBeNull();
    });
  });

  describe('formatDuration()', () => {
    it('3661 secondes → "1h 01min 01s"', () => {
      expect(service.formatDuration(3661)).toBe('1h 01min 01s');
    });

    it('90 secondes → "1min 30s"', () => {
      expect(service.formatDuration(90)).toBe('1min 30s');
    });

    it('45 secondes → "45s"', () => {
      expect(service.formatDuration(45)).toBe('45s');
    });

    it('3600 secondes → "1h 00min 00s"', () => {
      expect(service.formatDuration(3600)).toBe('1h 00min 00s');
    });

    it('0 secondes → "0s"', () => {
      expect(service.formatDuration(0)).toBe('0s');
    });

    it('5400 secondes (1h30) → "1h 30min 00s"', () => {
      expect(service.formatDuration(5400)).toBe('1h 30min 00s');
    });
  });

  describe('getBillingRate()', () => {
    it('appelle GET /api/v1/workspace/billing-rate', () => {
      service.getBillingRate().subscribe();
      const req = http.expectOne('/api/v1/workspace/billing-rate');
      expect(req.request.method).toBe('GET');
      req.flush({ ratePerHour: 250, effectiveFrom: '2026-04-01T00:00:00Z' });
    });
  });

  describe('saveBillingRate()', () => {
    it('appelle PUT /api/v1/workspace/billing-rate avec le bon body', () => {
      service.saveBillingRate(300).subscribe();
      const req = http.expectOne('/api/v1/workspace/billing-rate');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ ratePerHour: 300 });
      req.flush({ ratePerHour: 300, effectiveFrom: '2026-04-02T00:00:00Z' });
    });
  });
});

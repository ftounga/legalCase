import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UsageEventService } from './usage-event.service';
import { UsageEvent } from '../models/usage-event.model';

describe('UsageEventService', () => {
  let service: UsageEventService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(UsageEventService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getUsageEvents — GET /api/v1/case-files/{id}/usage', () => {
    const mockEvents: UsageEvent[] = [
      { id: 'u1', eventType: 'CHUNK_ANALYSIS', tokensInput: 100, tokensOutput: 50, estimatedCost: 0.000123, createdAt: '2026-03-18T10:00:00Z' }
    ];

    service.getUsageEvents('cf1').subscribe(events => {
      expect(events.length).toBe(1);
      expect(events[0].eventType).toBe('CHUNK_ANALYSIS');
    });

    const req = http.expectOne('/api/v1/case-files/cf1/usage');
    expect(req.request.method).toBe('GET');
    req.flush(mockEvents);
  });
});

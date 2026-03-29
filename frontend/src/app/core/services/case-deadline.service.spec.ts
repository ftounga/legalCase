import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { CaseDeadlineService } from './case-deadline.service';

describe('CaseDeadlineService', () => {
  let service: CaseDeadlineService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CaseDeadlineService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CaseDeadlineService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should GET deadlines for a case file', () => {
    service.list('cf-1').subscribe();
    httpMock.expectOne('/api/v1/case-files/cf-1/deadlines').flush([]);
  });

  it('should POST to create a deadline', () => {
    service.create('cf-1', 'Prescription', '2026-06-01').subscribe();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/deadlines');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ label: 'Prescription', dueDate: '2026-06-01' });
    req.flush({});
  });

  it('should PUT to update a deadline', () => {
    service.update('cf-1', 'd-1', 'Modifié', '2026-07-01').subscribe();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/deadlines/d-1');
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should DELETE a deadline', () => {
    service.delete('cf-1', 'd-1').subscribe();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/deadlines/d-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});

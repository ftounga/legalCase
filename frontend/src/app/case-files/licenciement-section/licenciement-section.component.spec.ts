import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { LicenciementSectionComponent } from './licenciement-section.component';

describe('LicenciementSectionComponent', () => {
  let component: LicenciementSectionComponent;
  let fixture: ComponentFixture<LicenciementSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '55555555-5555-5555-5555-555555555555';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/licenciement`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID, country: 'FRANCE', scoreRisque: 35, verdict: 'RISQUE_MODERE',
    criteres: [
      { code: 'FR_CONVOCATION', label: 'Convocation', reponse: 'OUI', pointsRisque: 0, bloquant: true, commentaire: 'Conforme' },
      { code: 'FR_MOTIVATION', label: 'Motivation', reponse: 'NON', pointsRisque: 20, bloquant: true, commentaire: 'NON CONFORME' },
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LicenciementSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LicenciementSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => { httpMock.verify(); });

  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  it('should create', () => { initNoExisting(); expect(component).toBeTruthy(); });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });

  it('should show form when no existing', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
  });

  it('should call POST when analyze()', () => {
    initNoExisting();
    component.analyze();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    req.flush(MOCK_RESPONSE);
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should display existing from GET', () => {
    initWithExisting();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });
});

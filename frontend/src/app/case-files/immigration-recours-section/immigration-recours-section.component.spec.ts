import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ImmigrationRecoursSectionComponent } from './immigration-recours-section.component';

describe('ImmigrationRecoursSectionComponent', () => {
  let component: ImmigrationRecoursSectionComponent;
  let fixture: ComponentFixture<ImmigrationRecoursSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '22222222-2222-2222-2222-222222222222';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/recours`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    recoursType: 'RECOURS_GRACIEUX_PREFET',
    recoursLabel: 'Recours gracieux auprès du préfet',
    dateNotification: '2026-03-01',
    dateLimite: '2026-04-30',
    dateLimiteDepassee: false,
    avertissement: null,
    requerant: { nom: 'Dupont', prenom: 'Jean', nationalite: 'Marocaine', adresse: '12 rue Test' },
    decisionContestee: { autorite: 'Préfet', date: '2026-02-15', reference: 'REF-001' },
    exposeFaits: 'Faits.',
    document: {
      enTete: 'En-tête test',
      objetDemande: 'Objet test',
      visaTextes: 'Visa test',
      exposeFaits: 'Faits test',
      moyensDroit: 'Moyens test',
      conclusions: 'Conclusions test',
      piecesJointes: ['Pièce 1', 'Pièce 2']
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationRecoursSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationRecoursSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  it('should create', () => {
    initNoExisting();
    expect(component).toBeTruthy();
  });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });

  it('should show form when no existing recours', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
    expect(component.recours()).toBeNull();
  });

  it('should call POST when generate() is called', () => {
    initNoExisting();

    component.recoursType.set('RECOURS_GRACIEUX_PREFET');
    component.dateNotification.set('2026-03-01');
    component.nom.set('Dupont');
    component.prenom.set('Jean');
    component.nationalite.set('Marocaine');
    component.adresse.set('12 rue Test');
    component.autorite.set('Préfet');
    component.dateDecision.set('2026-02-15');
    component.generate();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.recoursType).toBe('RECOURS_GRACIEUX_PREFET');
    req.flush(MOCK_RESPONSE);

    expect(component.recours()).toBeTruthy();
    expect(component.showForm()).toBe(false);
  });

  it('should display existing recours from GET', () => {
    initWithExisting();
    expect(component.recours()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.nom()).toBe('Dupont');
  });
});

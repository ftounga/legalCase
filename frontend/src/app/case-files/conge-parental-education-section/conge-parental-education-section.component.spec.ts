import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CongeParentalEducationSectionComponent } from './conge-parental-education-section.component';
import { CongeParentalEducationResponse } from '../../core/models/conge-parental-education.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('CongeParentalEducationSectionComponent', () => {
  let component: CongeParentalEducationSectionComponent;
  let fixture: ComponentFixture<CongeParentalEducationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/conge-parental-education-analysis';

  function eligibleResponse(overrides: Partial<CongeParentalEducationResponse> = {}): CongeParentalEducationResponse {
    return {
      caseFileId: 'case-1',
      statut: 'ELIGIBLE',
      ancienneteMois: 18,
      modaliteRetenue: 'TEMPS_PLEIN',
      nombreEnfants: 1,
      dateNaissanceOuAdoption: '2025-03-01',
      dateFinMax: '2028-03-01',
      dureeMaxMois: 36,
      protectionReintegration: true,
      mentionPreparE: true,
      notes: ['Condition d\'ancienneté remplie : 18 mois.'],
      country: 'FRANCE',
      baseJuridique: 'art. L.1225-47 à L.1225-60 du Code du travail (à vérifier par avocat)',
      ...overrides,
    };
  }

  function nonEligibleResponse(): CongeParentalEducationResponse {
    return eligibleResponse({
      statut: 'NON_ELIGIBLE',
      ancienneteMois: 6,
      dateFinMax: null,
      dureeMaxMois: 0,
      notes: ['Ancienneté insuffisante (art. L.1225-47 CT).'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [CongeParentalEducationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CongeParentalEducationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('se crée', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('charge l\'analyse existante au ngOnInit (GET 200) et masque le formulaire', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(eligibleResponse());
    expect(component.result()?.statut).toBe('ELIGIBLE');
    expect(component.result()?.dateFinMax).toBe('2028-03-01');
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige ancienneté ≥ 0, date et nombreEnfants ≥ 1', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onAncienneteChange(18);
    expect(component.formValid()).toBe(false);
    component.onDateChange('2025-03-01');
    expect(component.formValid()).toBe(true);
    component.onNombreEnfantsChange(0);
    expect(component.formValid()).toBe(false);
  });

  it('POST éligible → ELIGIBLE + dateFinMax affichée', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onAncienneteChange(18);
    component.onDateChange('2025-03-01');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.ancienneteMois).toBe(18);
    expect(req.request.body.dateNaissanceOuAdoption).toBe('2025-03-01');
    req.flush(eligibleResponse());
    expect(component.result()?.statut).toBe('ELIGIBLE');
    expect(component.result()?.dateFinMax).toBe('2028-03-01');
  });

  it('POST ancienneté insuffisante → NON_ELIGIBLE + pas de dateFinMax', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onAncienneteChange(6);
    component.onDateChange('2025-03-01');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush(nonEligibleResponse());
    expect(component.result()?.statut).toBe('NON_ELIGIBLE');
    expect(component.result()?.dateFinMax).toBeNull();
  });

  it('POST temps partiel → modalité transmise', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onAncienneteChange(18);
    component.onDateChange('2025-03-01');
    component.onModaliteChange('TEMPS_PARTIEL');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.modalite).toBe('TEMPS_PARTIEL');
    req.flush(eligibleResponse({ modaliteRetenue: 'TEMPS_PARTIEL' }));
    expect(component.result()?.modaliteRetenue).toBe('TEMPS_PARTIEL');
  });

  it('statutChipClass distingue ELIGIBLE (success) et NON_ELIGIBLE (danger)', () => {
    expect(component.statutChipClass('ELIGIBLE')).toContain('is-chip--success');
    expect(component.statutChipClass('NON_ELIGIBLE')).toContain('is-chip--danger');
  });

  it('pré-remplit la date de naissance depuis Sf218dDetail (snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = { date_naissance_ou_adoption: '2025-03-01' } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.dateNaissanceOuAdoption()).toBe('2025-03-01');
    expect(component.provenanceDate()).toBe('IA');
  });

  it('getPrefillCount = 1 (nominal), 0 (vide), 0 (BE)', () => {
    expect(CongeParentalEducationSectionComponent.getPrefillCount({
      aiData: { date_naissance_ou_adoption: '2025-03-01' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(CongeParentalEducationSectionComponent.getPrefillCount({})).toBe(0);
    expect(CongeParentalEducationSectionComponent.getPrefillCount({
      aiData: { date_naissance_ou_adoption: '2025-03-01' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('coherenceAlerts signale une divergence date IA ↔ saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { date_naissance_ou_adoption: '2025-03-01' } as unknown as TravailExtractedData;
    component.onDateChange('2024-01-15'); // IA dit 2025-03-01, saisie dit 2024-01-15
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_NAISSANCE).toBeTruthy();
    expect(alerts.DATE_NAISSANCE!.field).toBe('DATE_NAISSANCE');
  });

  it('gate FR : isFrance() false en BELGIQUE et pas d\'appel GET', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    expect(component.isFrance()).toBe(false);
    httpMock.expectNone(BASE_URL);
  });

  it('error POST → snackBar erreur sans casser le composant', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onAncienneteChange(18);
    component.onDateChange('2025-03-01');
    component.analyze();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });
});

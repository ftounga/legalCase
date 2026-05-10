import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MediationFamilialeSectionComponent } from './mediation-familiale-section.component';
import { MediationFamilialePreSaisineResponse } from '../../core/models/mediation-familiale.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('MediationFamilialeSectionComponent', () => {
  let component: MediationFamilialeSectionComponent;
  let fixture: ComponentFixture<MediationFamilialeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/mediation-familiale-pre-saisine';

  function recevableResponse(): MediationFamilialePreSaisineResponse {
    return {
      caseFileId: 'case-1',
      motifSaisine: 'AUTORITE_PARENTALE',
      mediationTentee: true,
      dateMediation: '2026-04-10',
      exceptionApplicable: 'AUCUNE',
      exceptionDetail: null,
      verdict: 'RECEVABLE',
      motifInScope: true,
      dispenseApplicable: false,
      piecesAJoindre: ['Attestation de tentative de médiation familiale (médiateur agréé)'],
      baseJuridique: 'Cciv art. 373-2-10 al. 3 + Loi 18/11/2016 + CPC art. 1108',
      formule: 'Saisine recevable (médiation tentée le 2026-04-10)',
      messages: ['Saisine du JAF recevable'],
      country: 'FRANCE',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        MediationFamilialeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MediationFamilialeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('mount FRANCE → GET initial 404 → showForm reste true', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('GET 200 → restore les champs form + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(recevableResponse());

    expect(component.result()!.verdict).toBe('RECEVABLE');
    expect(component.showForm()).toBe(false);
    expect(component.motifSaisine()).toBe('AUTORITE_PARENTALE');
    expect(component.mediationTentee()).toBe(true);
  });

  it('POST nominal → result + showForm=false + dashboardRefresh + snackbar succès', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onMotifChange('AUTORITE_PARENTALE');
    component.onMediationTenteeChange(true);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.motifSaisine).toBe('AUTORITE_PARENTALE');
    req.flush(recevableResponse());

    expect(component.result()!.verdict).toBe('RECEVABLE');
    expect(component.showForm()).toBe(false);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('prefill IA — motifSaisineMediationDetecte est appliqué après 404', () => {
    component.aiData = {
      motifSaisineMediationDetecte: 'CONTRIBUTION_ENTRETIEN',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.motifSaisine()).toBe('CONTRIBUTION_ENTRETIEN');
    expect(component.provenanceMotif()).toBe('IA');
  });

  it('formValid retourne false sans motif', () => {
    expect(component.formValid()).toBe(false);
    component.onMotifChange('AUTORITE_PARENTALE');
    expect(component.formValid()).toBe(true);
  });

  it('isFrance() basée sur workspaceCountry', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.workspaceCountry = 'FRANCE';
    expect(component.isFrance()).toBe(true);
  });

  it('getPrefillCount — 0 si aiData absent', () => {
    expect(MediationFamilialeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount — 1 si motifSaisineMediationDetecte présent', () => {
    expect(MediationFamilialeSectionComponent.getPrefillCount({
      aiData: { motifSaisineMediationDetecte: 'AUTORITE_PARENTALE' } as FamilleExtractedData,
    })).toBe(1);
  });
});

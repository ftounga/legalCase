import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { TempsTrajetDeplacementSectionComponent } from './temps-trajet-deplacement-section.component';
import { TempsTrajetDeplacementResponse } from '../../core/models/temps-trajet-deplacement.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('TempsTrajetDeplacementSectionComponent', () => {
  let component: TempsTrajetDeplacementSectionComponent;
  let fixture: ComponentFixture<TempsTrajetDeplacementSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/temps-trajet-deplacement-analysis';

  function depassementResponse(overrides: Partial<TempsTrajetDeplacementResponse> = {}): TempsTrajetDeplacementResponse {
    return {
      caseFileId: 'case-1',
      qualification: 'TRAJET_AVEC_CONTREPARTIE',
      typeTrajet: 'DOMICILE_TRAVAIL_HABITUEL',
      tempsTrajetQuotidienMinutes: 90,
      tempsTrajetNormalMinutes: 30,
      contrepartiePrevueAccord: false,
      contrepartieDue: true,
      depassementMinutes: 60,
      base: 'trajet 90 min > trajet normal 30 min — dépassement de 60 min',
      notes: ['Le temps de trajet dépasse le temps normal de trajet : une contrepartie est due.'],
      country: 'FRANCE',
      baseJuridique: 'art. L.3121-4 du Code du travail ; CJUE C-266/14 (à vérifier par avocat)',
      ...overrides,
    };
  }

  function itinerantResponse(): TempsTrajetDeplacementResponse {
    return depassementResponse({
      qualification: 'TEMPS_TRAVAIL',
      typeTrajet: 'ITINERANT_SANS_LIEU_FIXE',
      contrepartieDue: false,
      base: 'salarié itinérant sans lieu de travail fixe — temps de travail effectif',
      notes: ['Salarié itinérant sans lieu de travail fixe : temps de travail effectif (CJUE C-266/14).'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [TempsTrajetDeplacementSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TempsTrajetDeplacementSectionComponent);
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
    req.flush(depassementResponse());
    expect(component.result()?.qualification).toBe('TRAJET_AVEC_CONTREPARTIE');
    expect(component.result()?.contrepartieDue).toBe(true);
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige type + quotidien (+ normal si non itinérant)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onTypeTrajetChange('DOMICILE_TRAVAIL_HABITUEL');
    component.onQuotidienChange(90);
    expect(component.formValid()).toBe(false); // normal manquant
    component.onNormalChange(30);
    expect(component.formValid()).toBe(true);
  });

  it('formValid() n\'exige pas le temps normal pour un salarié itinérant', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    component.onTypeTrajetChange('ITINERANT_SANS_LIEU_FIXE');
    component.onQuotidienChange(50);
    expect(component.isItinerant()).toBe(true);
    expect(component.formValid()).toBe(true);
  });

  it('POST dépassement → TRAJET_AVEC_CONTREPARTIE, contrepartie DUE', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeTrajetChange('DOMICILE_TRAVAIL_HABITUEL');
    component.onQuotidienChange(90);
    component.onNormalChange(30);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeTrajet).toBe('DOMICILE_TRAVAIL_HABITUEL');
    expect(req.request.body.tempsTrajetQuotidienMinutes).toBe(90);
    expect(req.request.body.tempsTrajetNormalMinutes).toBe(30);
    req.flush(depassementResponse());
    expect(component.result()?.qualification).toBe('TRAJET_AVEC_CONTREPARTIE');
    expect(component.result()?.contrepartieDue).toBe(true);
    expect(component.result()?.depassementMinutes).toBe(60);
  });

  it('POST itinérant → TEMPS_TRAVAIL, normal défaut 0 transmis', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTypeTrajetChange('ITINERANT_SANS_LIEU_FIXE');
    component.onQuotidienChange(50);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.typeTrajet).toBe('ITINERANT_SANS_LIEU_FIXE');
    expect(req.request.body.tempsTrajetNormalMinutes).toBe(0);
    req.flush(itinerantResponse());
    expect(component.result()?.qualification).toBe('TEMPS_TRAVAIL');
    expect(component.result()?.contrepartieDue).toBe(false);
  });

  it('qualificationChipClass : TEMPS_TRAVAIL (success), AVEC (info), SANS (neutral)', () => {
    expect(component.qualificationChipClass('TEMPS_TRAVAIL')).toContain('is-chip--success');
    expect(component.qualificationChipClass('TRAJET_AVEC_CONTREPARTIE')).toContain('is-chip--info');
    expect(component.qualificationChipClass('TRAJET_SANS_CONTREPARTIE')).toContain('is-chip--neutral');
  });

  it('contrepartieChipClass : DUE (success vert), NON_DUE (danger rouge)', () => {
    expect(component.contrepartieChipClass(true)).toContain('is-chip--success');
    expect(component.contrepartieChipClass(false)).toContain('is-chip--danger');
  });

  it('pré-remplit type + quotidien depuis Sf218dDetail (snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = {
      type_trajet: 'ITINERANT_SANS_LIEU_FIXE',
      temps_trajet_quotidien_minutes: 75,
    } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.typeTrajet()).toBe('ITINERANT_SANS_LIEU_FIXE');
    expect(component.provenanceType()).toBe('IA');
    expect(component.tempsTrajetQuotidienMinutes()).toBe(75);
    expect(component.provenanceQuotidien()).toBe('IA');
  });

  it('getPrefillCount = 2 (nominal), 0 (vide), 0 (BE)', () => {
    expect(TempsTrajetDeplacementSectionComponent.getPrefillCount({
      aiData: { type_trajet: 'DOMICILE_TRAVAIL_HABITUEL', temps_trajet_quotidien_minutes: 90 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(2);
    expect(TempsTrajetDeplacementSectionComponent.getPrefillCount({})).toBe(0);
    expect(TempsTrajetDeplacementSectionComponent.getPrefillCount({
      aiData: { type_trajet: 'DOMICILE_TRAVAIL_HABITUEL', temps_trajet_quotidien_minutes: 90 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('coherenceAlerts signale une divergence type IA ↔ saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { type_trajet: 'ITINERANT_SANS_LIEU_FIXE' } as unknown as TravailExtractedData;
    component.onTypeTrajetChange('DOMICILE_TRAVAIL_HABITUEL'); // IA dit itinérant, saisie dit habituel
    const alerts = component.coherenceAlerts();
    expect(alerts.TYPE_TRAJET).toBeTruthy();
    expect(alerts.TYPE_TRAJET!.field).toBe('TYPE_TRAJET');
  });

  it('coherenceAlerts signale une divergence quotidien IA ↔ saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { temps_trajet_quotidien_minutes: 90 } as unknown as TravailExtractedData;
    component.onQuotidienChange(45);
    const alerts = component.coherenceAlerts();
    expect(alerts.QUOTIDIEN).toBeTruthy();
    expect(alerts.QUOTIDIEN!.field).toBe('QUOTIDIEN');
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
    component.onTypeTrajetChange('DOMICILE_TRAVAIL_HABITUEL');
    component.onQuotidienChange(90);
    component.onNormalChange(30);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });
});

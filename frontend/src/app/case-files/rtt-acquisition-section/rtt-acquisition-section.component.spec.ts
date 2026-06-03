import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RttAcquisitionSectionComponent } from './rtt-acquisition-section.component';
import { RttAcquisitionResponse } from '../../core/models/rtt-acquisition.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('RttAcquisitionSectionComponent', () => {
  let component: RttAcquisitionSectionComponent;
  let fixture: ComponentFixture<RttAcquisitionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/rtt-acquisition-analysis';

  function calculeResponse(overrides: Partial<RttAcquisitionResponse> = {}): RttAcquisitionResponse {
    return {
      caseFileId: 'case-1',
      statut: 'CALCULE',
      horaireHebdomadaireCollectif: 37,
      accordCollectifPresent: true,
      semainesTravailleesAn: 47,
      nombreJrttTheorique: 12.7,
      base: 'horaire collectif 37 h, 47 semaines travaillées, JRTT sans majoration',
      notes: ['Les JRTT compensent les heures effectuées entre 35 h et l\'horaire collectif et ne donnent lieu à aucune majoration.'],
      country: 'FRANCE',
      baseJuridique: 'art. L.3121-41 à L.3121-44 du Code du travail (à vérifier par avocat)',
      ...overrides,
    };
  }

  function renvoiResponse(): RttAcquisitionResponse {
    return calculeResponse({
      statut: 'RENVOI_HEURES_SUP',
      accordCollectifPresent: false,
      nombreJrttTheorique: null,
      base: 'absence d\'accord d\'aménagement du temps de travail sur l\'année',
      notes: ['À défaut d\'accord d\'aménagement, les heures effectuées au-delà de 35 h relèvent du régime des heures supplémentaires (voir l\'outil dédié — F-DT-19).'],
    });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [RttAcquisitionSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RttAcquisitionSectionComponent);
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
    req.flush(calculeResponse());
    expect(component.result()?.statut).toBe('CALCULE');
    expect(component.result()?.nombreJrttTheorique).toBe(12.7);
    expect(component.showForm()).toBe(false);
  });

  it('affiche le formulaire si aucune analyse (GET 404)', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid() exige un horaire collectif cohérent (> 35 et ≤ 48)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.onHoraireChange(35);
    expect(component.formValid()).toBe(false);
    component.onHoraireChange(50);
    expect(component.formValid()).toBe(false);
    component.onHoraireChange(37);
    expect(component.formValid()).toBe(true);
  });

  it('POST accord présent → CALCULE + nombreJrttTheorique affiché', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onHoraireChange(39);
    component.onAccordChange(true);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.horaireHebdomadaireCollectif).toBe(39);
    expect(req.request.body.accordCollectifPresent).toBe(true);
    req.flush(calculeResponse({ horaireHebdomadaireCollectif: 39, nombreJrttTheorique: 24.1 }));
    expect(component.result()?.statut).toBe('CALCULE');
    expect(component.result()?.nombreJrttTheorique).toBe(24.1);
  });

  it('POST sans accord → RENVOI_HEURES_SUP, pas de JRTT', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onHoraireChange(37);
    component.onAccordChange(false);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.accordCollectifPresent).toBe(false);
    req.flush(renvoiResponse());
    expect(component.result()?.statut).toBe('RENVOI_HEURES_SUP');
    expect(component.result()?.nombreJrttTheorique).toBeNull();
  });

  it('POST transmet semainesTravailleesAn quand saisi', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onHoraireChange(37);
    component.onSemainesChange(40);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.semainesTravailleesAn).toBe(40);
    req.flush(calculeResponse({ semainesTravailleesAn: 40, nombreJrttTheorique: 10.8 }));
    expect(component.result()?.semainesTravailleesAn).toBe(40);
  });

  it('statutChipClass : CALCULE (success), RENVOI_HEURES_SUP (warning orange)', () => {
    expect(component.statutChipClass('CALCULE')).toContain('is-chip--success');
    expect(component.statutChipClass('RENVOI_HEURES_SUP')).toContain('is-chip--warning');
  });

  it('pré-remplit l\'horaire depuis Sf218dDetail (snake_case) avec badge IA', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const aiData = { horaire_hebdomadaire_collectif: 39 } as unknown as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.horaireHebdomadaireCollectif()).toBe(39);
    expect(component.provenanceHoraire()).toBe('IA');
  });

  it('getPrefillCount = 1 (nominal), 0 (vide), 0 (BE)', () => {
    expect(RttAcquisitionSectionComponent.getPrefillCount({
      aiData: { horaire_hebdomadaire_collectif: 37 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(RttAcquisitionSectionComponent.getPrefillCount({})).toBe(0);
    expect(RttAcquisitionSectionComponent.getPrefillCount({
      aiData: { horaire_hebdomadaire_collectif: 37 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('coherenceAlerts signale une divergence horaire IA ↔ saisie', () => {
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.aiData = { horaire_hebdomadaire_collectif: 37 } as unknown as TravailExtractedData;
    component.onHoraireChange(39); // IA dit 37, saisie dit 39
    const alerts = component.coherenceAlerts();
    expect(alerts.HORAIRE).toBeTruthy();
    expect(alerts.HORAIRE!.field).toBe('HORAIRE');
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
    component.onHoraireChange(37);
    component.analyze();
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });
});

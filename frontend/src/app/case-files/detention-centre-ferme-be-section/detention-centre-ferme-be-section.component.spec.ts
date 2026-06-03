import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DetentionCentreFermeBeSectionComponent } from './detention-centre-ferme-be-section.component';
import { DetentionCentreFermeBeResponse } from '../../core/models/detention-centre-ferme-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('DetentionCentreFermeBeSectionComponent', () => {
  let component: DetentionCentreFermeBeSectionComponent;
  let fixture: ComponentFixture<DetentionCentreFermeBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/detention-centre-ferme-be-analysis';

  function beResponse(
    overrides: Partial<DetentionCentreFermeBeResponse> = {},
  ): DetentionCentreFermeBeResponse {
    return {
      caseFileId: 'case-1',
      dateDebutDetention: '2026-05-30',
      baseLegaleDetention: 'ART_7',
      prolongationNotifiee: false,
      dateProlongation: null,
      requeteMiseEnLiberteDeposee: false,
      dateNotificationDecisionDetention: '2026-06-01',
      verdict: 'REQUETE_OUVERTE',
      dureeDetentionJours: 4,
      dateLimiteRequete: '2026-06-06',
      joursRestantsRequete: 3,
      basesJuridiques: ['Loi du 15/12/1980 art. 71 et s. (à vérifier par avocat)'],
      messages: ['Fenêtre ouverte.'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [DetentionCentreFermeBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DetentionCentreFermeBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(DetentionCentreFermeBeSectionComponent.TOOL_LABEL).toContain('DÉTENTION CENTRE FERMÉ');
    expect(DetentionCentreFermeBeSectionComponent.TOOL_ICON).toBe('lock');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=3 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(DetentionCentreFermeBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(DetentionCentreFermeBeSectionComponent.getPrefillCount({
      aiData: { detentionDateDebut: '2026-05-30' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 3 (nominal) when all real signals present (BELGIQUE)', () => {
    expect(DetentionCentreFermeBeSectionComponent.getPrefillCount({
      aiData: {
        detentionDateDebut: '2026-05-30',
        detentionBaseLegale: 'ART_74_5',
        detentionDateNotification: '2026-06-01',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount ignores invalid base legale value', () => {
    expect(DetentionCentreFermeBeSectionComponent.getPrefillCount({
      aiData: { detentionBaseLegale: 'ART_999' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(DetentionCentreFermeBeSectionComponent.getPrefillCount({
      aiData: { detentionDateDebut: '2026-05-30' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---- HTTP lifecycle ----
  it('BELGIQUE -> GET called on ngOnInit', () => {
    expect(component.isBelgique()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('FRANCE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    expect(component.result()!.verdict).toBe('REQUETE_OUVERTE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutDetention()).toBe('2026-05-30');
    expect(component.baseLegaleDetention()).toBe('ART_7');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation (bouton désactivé si vide / conditions) ----
  it('formValid false until date present; true once date set', () => {
    expect(component.formValid()).toBe(false);
    component.dateDebutDetention.set('2026-05-30');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false when prolongation notifiée but no date', () => {
    component.dateDebutDetention.set('2026-05-30');
    component.onProlongationNotifieeChange(true);
    expect(component.formValid()).toBe(false);
    component.onDateProlongationChange('2026-06-01');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false when requête déposée but no notification date', () => {
    component.dateDebutDetention.set('2026-05-30');
    component.onRequeteDeposeeChange(true);
    expect(component.formValid()).toBe(false);
    component.onDateNotificationChange('2026-06-01');
    expect(component.formValid()).toBe(true);
  });

  // ---- pré-fill IA (3 champs) ----
  it('prefills the 3 real fields from aiData on ngOnChanges (BELGIQUE)', () => {
    const aiData: ImmigrationExtractedData = {
      detentionDateDebut: '2026-05-15',
      detentionBaseLegale: 'ART_74_5',
      detentionDateNotification: '2026-05-20',
    } as ImmigrationExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateDebutDetention()).toBe('2026-05-15');
    expect(component.baseLegaleDetention()).toBe('ART_74_5');
    expect(component.dateNotificationDecisionDetention()).toBe('2026-05-20');
    expect(component.provenanceDateDebut()).toBe('IA');
  });

  // ---- POST nominal ----
  it('POST on analyze() and switches to result mode', () => {
    component.dateDebutDetention.set('2026-05-30');
    component.baseLegaleDetention.set('ART_7');
    component.dateNotificationDecisionDetention.set('2026-06-01');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateDebutDetention).toBe('2026-05-30');
    expect(req.request.body.baseLegaleDetention).toBe('ART_7');
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('REQUETE_OUVERTE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('does not POST when form invalid', () => {
    component.analyze();
    httpMock.expectNone(BASE_URL);
  });

  // ---- prolongation verdict ----
  it('exposes prolongation verdict on PROLONGATION_A_CONTESTER', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({
      verdict: 'PROLONGATION_A_CONTESTER',
      prolongationNotifiee: true,
      dateProlongation: '2026-06-01',
    }));
    expect(component.result()!.verdict).toBe('PROLONGATION_A_CONTESTER');
    expect(component.result()!.prolongationNotifiee).toBe(true);
  });

  // ---- verdict mapping ----
  it('maps verdict to banner/chip/icon/label classes', () => {
    expect(component.verdictChipClass('REQUETE_OUVERTE')).toContain('success');
    expect(component.verdictChipClass('PROLONGATION_A_CONTESTER')).toContain('success');
    expect(component.verdictChipClass('REQUETE_TARDIVE')).toContain('warning');
    expect(component.verdictChipClass('DETENTION_EN_COURS')).toContain('info');
    expect(component.verdictChipClass('REQUETE_DEPOSEE')).toContain('info');
    expect(component.verdictLabel('REQUETE_TARDIVE')).toBe('Requête tardive');
    expect(component.verdictLabel('PROLONGATION_A_CONTESTER')).toBe('Prolongation à contester');
    expect(component.verdictIcon('REQUETE_OUVERTE')).toBe('gavel');
  });

  it('baseLegaleLabel resolves whitelist labels', () => {
    expect(component.baseLegaleLabel('ART_74_5')).toContain('74/5');
    expect(component.baseLegaleLabel(null)).toBe('—');
  });

  it('formatDateFr converts ISO to dd/mm/yyyy', () => {
    expect(component.formatDateFr('2026-05-30')).toBe('30/05/2026');
    expect(component.formatDateFr(null)).toBe('—');
  });
});

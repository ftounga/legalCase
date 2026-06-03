import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { SignalementSisSectionComponent } from './signalement-sis-section.component';
import { SignalementSisResponse } from '../../core/models/signalement-sis.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('SignalementSisSectionComponent', () => {
  let component: SignalementSisSectionComponent;
  let fixture: ComponentFixture<SignalementSisSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/signalement-sis-analysis';

  function frResponse(overrides: Partial<SignalementSisResponse> = {}): SignalementSisResponse {
    return {
      caseFileId: 'case-1',
      signalementConnu: true,
      etatSignalant: 'FRANCE',
      motifSignalement: 'IRTF',
      titreSejourValide: false,
      dateSignalement: null,
      country: 'FRANCE',
      actionPossible: 'RADIATION_AUTORITE_FR',
      demarches: ['Contester la mesure sous-jacente'],
      autoriteCompetente: 'Autorité française',
      basesJuridiques: ['Règlement (UE) 2018/1860'],
      messages: [],
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
      imports: [SignalementSisSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(SignalementSisSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(SignalementSisSectionComponent.TOOL_LABEL).toContain('SIS');
    expect(SignalementSisSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(SignalementSisSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present', () => {
    expect(SignalementSisSectionComponent.getPrefillCount({
      aiData: {
        signalementSisDetecte: true,
        signalementSisEtatSignalant: 'AUTRE_ETAT_MEMBRE',
        signalementSisMotifSignalement: 'MESURE_ELOIGNEMENT_ETRANGERE',
        signalementSisTitreSejourValide: true,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(SignalementSisSectionComponent.getPrefillCount({
      aiData: { signalementSisEtatSignalant: 'FRANCE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.result()!.actionPossible).toBe('RADIATION_AUTORITE_FR');
    expect(component.showForm()).toBe(false);
    expect(component.etatSignalant()).toBe('FRANCE');
    expect(component.motifSignalement()).toBe('IRTF');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires an etatSignalant', () => {
    expect(component.formValid()).toBe(false);
    component.etatSignalant.set('FRANCE');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.etatSignalant.set('FRANCE');
    component.motifSignalement.set('IRTF');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.etatSignalant).toBe('FRANCE');
    expect(req.request.body.motifSignalement).toBe('IRTF');
    req.flush(frResponse());
    expect(component.result()!.actionPossible).toBe('RADIATION_AUTORITE_FR');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when etatSignalant missing (form invalid)', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.etatSignalant.set('FRANCE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 4 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      signalementSisDetecte: true,
      signalementSisEtatSignalant: 'AUTRE_ETAT_MEMBRE',
      signalementSisMotifSignalement: 'MENACE_ORDRE_PUBLIC',
      signalementSisTitreSejourValide: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.signalementConnu()).toBe(true);
    expect(component.provenanceSignalementConnu()).toBe('IA');
    expect(component.etatSignalant()).toBe('AUTRE_ETAT_MEMBRE');
    expect(component.provenanceEtatSignalant()).toBe('IA');
    expect(component.motifSignalement()).toBe('MENACE_ORDRE_PUBLIC');
    expect(component.provenanceMotifSignalement()).toBe('IA');
    expect(component.titreSejourValide()).toBe(true);
    expect(component.provenanceTitreSejourValide()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      signalementSisEtatSignalant: 'INCONNU',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ etatSignalant: 'FRANCE' }));
    expect(component.etatSignalant()).toBe('FRANCE');
    expect(component.provenanceEtatSignalant()).toBeNull();
  });

  it('onEtatSignalantChange / onMotifSignalementChange / onTitreSejourValideChange clear provenance', () => {
    component.aiData = {
      signalementSisDetecte: true,
      signalementSisEtatSignalant: 'AUTRE_ETAT_MEMBRE',
      signalementSisMotifSignalement: 'IRTF',
      signalementSisTitreSejourValide: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceEtatSignalant()).toBe('IA');
    component.onEtatSignalantChange('FRANCE');
    expect(component.provenanceEtatSignalant()).toBeNull();
    component.onMotifSignalementChange('AUTRE');
    expect(component.provenanceMotifSignalement()).toBeNull();
    component.onTitreSejourValideChange(false);
    expect(component.provenanceTitreSejourValide()).toBeNull();
    component.onSignalementConnuChange(false);
    expect(component.provenanceSignalementConnu()).toBeNull();
  });

  it('bannerClass / bannerIcon cover the action states', () => {
    expect(component.bannerClass(frResponse({ actionPossible: 'RADIATION_AUTORITE_FR' }))).toContain('sis-banner--success');
    expect(component.bannerClass(frResponse({ actionPossible: 'CONSULTATION_ENTRE_ETATS' }))).toContain('sis-banner--warning');
    expect(component.bannerClass(frResponse({ actionPossible: 'RADIATION_ETAT_SIGNALANT' }))).toContain('sis-banner--info');
    expect(component.bannerClass(frResponse({ actionPossible: 'INDETERMINE' }))).toContain('sis-banner--neutral');
    expect(component.bannerIcon(frResponse({ actionPossible: 'RADIATION_AUTORITE_FR' }))).toBe('verified');
    expect(component.bannerIcon(frResponse({ actionPossible: 'CONSULTATION_ENTRE_ETATS' }))).toBe('sync_alt');
    expect(component.bannerIcon(frResponse({ actionPossible: 'RADIATION_ETAT_SIGNALANT' }))).toBe('public');
  });

  it('actionLabel maps codes to FR labels', () => {
    expect(component.actionLabel('RADIATION_AUTORITE_FR')).toContain('française');
    expect(component.actionLabel('RADIATION_ETAT_SIGNALANT')).toContain('État signalant');
    expect(component.actionLabel('DROIT_ACCES_RECTIFICATION')).toContain('accès');
    expect(component.actionLabel('CONSULTATION_ENTRE_ETATS')).toContain('Consultation');
    expect(component.actionLabel('INDETERMINE')).toContain('indéterminée');
    expect(component.actionLabel(null)).toBe('');
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.etatSignalant()).toBeNull();
    component.aiData = {
      signalementSisEtatSignalant: 'FRANCE',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.etatSignalant()).toBe('FRANCE');
    expect(component.provenanceEtatSignalant()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ etatSignalant: 'FRANCE' }));
    component.aiData = {
      signalementSisEtatSignalant: 'INCONNU',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.etatSignalant()).toBe('FRANCE');
    expect(component.provenanceEtatSignalant()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.sis-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});

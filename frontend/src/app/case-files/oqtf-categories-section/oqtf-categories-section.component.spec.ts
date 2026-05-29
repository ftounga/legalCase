import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { OqtfCategoriesSectionComponent } from './oqtf-categories-section.component';
import { OqtfCategoriesResponse } from '../../core/models/oqtf-categories.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('OqtfCategoriesSectionComponent', () => {
  let component: OqtfCategoriesSectionComponent;
  let fixture: ComponentFixture<OqtfCategoriesSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/oqtf-categories-analysis';

  function frResponse(overrides: Partial<OqtfCategoriesResponse> = {}): OqtfCategoriesResponse {
    return {
      caseFileId: 'case-1',
      categorieL611: 'CAT_3',
      dateNotificationOqtf: '2025-01-15',
      motifOqtf: null,
      country: 'FRANCE',
      categorieLibelle: '3° — Entrée ou séjour irrégulier',
      moyensDefense: ['Vie privée et familiale (art. 8 CEDH)', 'Erreur de droit'],
      baseJuridique: 'CESEDA L.611-1, 3°',
      delaiRecours: '30 jours',
      procedureParallele: null,
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.categorieL611.set('CAT_3');
    component.dateNotificationOqtf.set('2025-01-15');
    component.motifOqtf.set('Séjour irrégulier');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OqtfCategoriesSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OqtfCategoriesSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('creates', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('FRANCE : charge l\'analyse existante au ngOnInit', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()?.categorieL611).toBe('CAT_3');
    expect(component.showForm()).toBe(false);
  });

  it('404 : bascule sur le formulaire et tente le pré-fill', () => {
    fixture.detectChanges();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('gate FRANCE : ne déclenche aucun appel HTTP en Belgique', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('bannière BE affichée hors France', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="be-banner"]');
    expect(banner).toBeTruthy();
  });

  it('formValid : faux sans catégorie ou sans date valide', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.categorieL611.set('CAT_1');
    expect(component.formValid()).toBe(false);
    component.dateNotificationOqtf.set('2025-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : rejette un motif > 300 caractères', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    component.categorieL611.set('CAT_1');
    component.dateNotificationOqtf.set('2025-01-15');
    component.motifOqtf.set('x'.repeat(301));
    expect(component.formValid()).toBe(false);
  });

  it('analyze : POST + bascule sur le résultat + snackbar', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.categorieL611).toBe('CAT_3');
    expect(req.request.body.motifOqtf).toBe('Séjour irrégulier');
    req.flush(frResponse());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.categorieLibelle).toContain('3°');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze : erreur HTTP → snackbar erreur, reste sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('CAT_7 : isCat7 vrai + procedureParallele DUBLIN + lien F-IM-22 rendu', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({
      categorieL611: 'CAT_7',
      categorieLibelle: '7° — Remise à un autre État membre (Dublin)',
      procedureParallele: 'Recours Dublin (F-IM-22) — règlement (UE) 604/2013',
    }));
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(component.isCat7()).toBe(true);
    const parallele = fixture.nativeElement.querySelector('[data-testid="procedure-parallele"]');
    expect(parallele).toBeTruthy();
    const lien = fixture.nativeElement.querySelector('[data-testid="lien-fim22"]');
    expect(lien).toBeTruthy();
    expect(lien.textContent).toContain('F-IM-22');
  });

  it('CAT_3 (non Dublin) : aucun lien F-IM-22 affiché', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse());
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(component.isCat7()).toBe(false);
    const lien = fixture.nativeElement.querySelector('[data-testid="lien-fim22"]');
    expect(lien).toBeNull();
  });

  it('prefillFromAi : pré-remplit date + motif depuis ImmigrationExtractedData', () => {
    const aiData = {
      dateNotificationOqtf: '2025-02-01',
      motifOqtfCode: 'REFUS_TITRE',
    } as ImmigrationExtractedData;
    fixture.detectChanges();
    flush404();
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.dateNotificationOqtf()).toBe('2025-02-01');
    expect(component.provenanceDate()).toBe('IA');
    expect(component.motifOqtf()).toBe('Refus de titre de séjour');
    expect(component.provenanceMotif()).toBe('IA');
  });

  it('getPrefillCount static reflète le runtime (2 champs)', () => {
    const count = OqtfCategoriesSectionComponent.getPrefillCount({
      aiData: { dateNotificationOqtf: '2025-01-15', motifOqtfCode: 'AUTRE' },
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(2);
  });

  it('toggleCollapse inverse l\'état replié', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    const before = component.collapsed();
    component.toggleCollapse();
    expect(component.collapsed()).toBe(!before);
  });

  it('standaloneMode : pas d\'appel HTTP, formulaire visible', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});

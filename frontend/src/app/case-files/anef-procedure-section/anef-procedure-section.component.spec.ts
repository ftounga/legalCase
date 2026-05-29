import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { AnefProcedureSectionComponent } from './anef-procedure-section.component';
import { AnefProcedureResponse } from '../../core/models/anef-procedure.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AnefProcedureSectionComponent', () => {
  let component: AnefProcedureSectionComponent;
  let fixture: ComponentFixture<AnefProcedureSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/anef-procedure-analysis';

  function frResponse(overrides: Partial<AnefProcedureResponse> = {}): AnefProcedureResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      typeTitreConcerne: 'Carte de séjour pluriannuelle',
      dateExpirationTitre: '2026-03-10',
      panneeANEFSignalee: false,
      dateTentativeDepot: null,
      demandeAdresseePrefecture: false,
      statut: 'NORMAL',
      etapesAlternatives: [
        'Conserver les captures d\'écran de la panne',
        'Adresser un courrier RAR à la préfecture',
        'Saisir le tribunal administratif en référé',
      ],
      etapesStandard: [
        'Créer le compte ANEF',
        'Compléter la demande en ligne',
        'Joindre les pièces justificatives',
        'Suivre l\'instruction sur le portail',
      ],
      delaiRecoursForFaute: '2 mois à compter de la décision implicite de rejet',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.typeTitreConcerne.set('Carte de séjour pluriannuelle');
    component.dateExpirationTitre.set('2026-03-10');
    component.panneeANEFSignalee.set(false);
    component.demandeAdresseePrefecture.set(false);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AnefProcedureSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AnefProcedureSectionComponent);
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
    expect(component.result()?.statut).toBe('NORMAL');
    expect(component.showForm()).toBe(false);
  });

  it('404 : bascule sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('gate FRANCE : aucun appel HTTP en Belgique + bannière BE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
    const banner = fixture.nativeElement.querySelector('[data-testid="be-banner"]');
    expect(banner).toBeTruthy();
  });

  it('formValid : exige type + date expiration ISO + tentative ISO si renseignée', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.typeTitreConcerne.set('VPF');
    expect(component.formValid()).toBe(false);
    component.dateExpirationTitre.set('2026-03-10');
    expect(component.formValid()).toBe(true);
    component.dateTentativeDepot.set('not-a-date');
    expect(component.formValid()).toBe(false);
    component.dateTentativeDepot.set('2026-02-01');
    expect(component.formValid()).toBe(true);
  });

  it('analyze : POST + bascule sur le résultat + snackbar', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeTitreConcerne).toBe('Carte de séjour pluriannuelle');
    expect(req.request.body.dateExpirationTitre).toBe('2026-03-10');
    expect(req.request.body.panneeANEFSignalee).toBe(false);
    expect(req.request.body.demandeAdresseePrefecture).toBe(false);
    req.flush(frResponse());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.statut).toBe('NORMAL');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze : panne non signalée → dateTentativeDepot null dans le payload', () => {
    fixture.detectChanges();
    flush404();
    component.typeTitreConcerne.set('VPF');
    component.dateExpirationTitre.set('2026-03-10');
    component.panneeANEFSignalee.set(false);
    component.dateTentativeDepot.set('2026-02-01'); // ignorée car pas de panne
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.dateTentativeDepot).toBeNull();
    req.flush(frResponse());
    expect(component.result()).toBeTruthy();
  });

  it('analyze : panne signalée → dateTentativeDepot transmise', () => {
    fixture.detectChanges();
    flush404();
    component.typeTitreConcerne.set('VPF');
    component.dateExpirationTitre.set('2026-03-10');
    component.panneeANEFSignalee.set(true);
    component.dateTentativeDepot.set('2026-02-01');
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.panneeANEFSignalee).toBe(true);
    expect(req.request.body.dateTentativeDepot).toBe('2026-02-01');
    req.flush(frResponse({ statut: 'PANNE_EN_COURS' }));
    expect(component.result()?.statut).toBe('PANNE_EN_COURS');
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

  it('PANNE_EN_COURS : stepper alternatif rendu + bannière panne', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ statut: 'PANNE_EN_COURS' }));
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(component.isPanne()).toBe(true);
    const altStepper = fixture.nativeElement.querySelector('[data-testid="stepper-alt"]');
    expect(altStepper).toBeTruthy();
    const steps = fixture.nativeElement.querySelectorAll('[data-testid="stepper-step"]');
    expect(steps.length).toBe(3);
    expect(steps[1].textContent).toContain('préfecture');
    const banner = fixture.nativeElement.querySelector('[data-testid="panne-banner"]');
    expect(banner).toBeTruthy();
    // pas le stepper standard
    expect(fixture.nativeElement.querySelector('[data-testid="stepper-standard"]')).toBeNull();
  });

  it('NORMAL : stepper standard rendu, pas de bannière panne', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ statut: 'NORMAL' }));
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(component.isPanne()).toBe(false);
    const stdStepper = fixture.nativeElement.querySelector('[data-testid="stepper-standard"]');
    expect(stdStepper).toBeTruthy();
    const steps = fixture.nativeElement.querySelectorAll('[data-testid="stepper-step"]');
    expect(steps.length).toBe(4);
    expect(fixture.nativeElement.querySelector('[data-testid="panne-banner"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="stepper-alt"]')).toBeNull();
  });

  it('RECOURS_POSSIBLE : affiche les étapes alternatives + délai recours', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ statut: 'RECOURS_POSSIBLE' }));
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(component.isPanne()).toBe(true);
    const delai = fixture.nativeElement.querySelector('[data-testid="delai-recours"]');
    expect(delai.textContent).toContain('2 mois');
  });

  it('statut URGENT : chip rendu avec le bon libellé', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ statut: 'URGENT' }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('[data-testid="statut-chip"]');
    expect(chip.textContent).toContain('Urgent');
  });

  it('prefillFromAi : pré-remplit type + date expiration depuis l\'analyse', () => {
    const aiData = {
      dateExpirationTitre: '2026-03-10',
      typeTitreSejour: 'Carte de séjour pluriannuelle',
    } as ImmigrationExtractedData;
    fixture.detectChanges();
    flush404();
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.dateExpirationTitre()).toBe('2026-03-10');
    expect(component.typeTitreConcerne()).toBe('Carte de séjour pluriannuelle');
    expect(component.provenanceExpiration()).toBe('IA');
    expect(component.provenanceTypeTitre()).toBe('IA');
  });

  it('getPrefillCount static reflète le runtime (2 champs)', () => {
    const count = AnefProcedureSectionComponent.getPrefillCount({
      aiData: { dateExpirationTitre: '2026-03-10', typeTitreSejour: 'VPF' },
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

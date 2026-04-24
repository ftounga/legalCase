import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HeuresSupSectionComponent } from './heures-sup-section.component';
import { HeuresSupResponse } from '../../core/models/heures-sup.model';

describe('HeuresSupSectionComponent', () => {
  let component: HeuresSupSectionComponent;
  let fixture: ComponentFixture<HeuresSupSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/heures-sup';

  function frResponse(): HeuresSupResponse {
    return {
      caseFileId: 'case-1',
      tauxHoraireBrut: 15,
      heuresSupDeclarees25pct: 40,
      heuresSupDeclarees50pct: 10,
      heuresHorsContingent: 0,
      tauxMajoration25: 25,
      tauxMajoration50: 50,
      heuresSupSemaine: null,
      heuresDimancheJoursFeries: null,
      country: 'FRANCE',
      rappelMajoration25pct: 150,
      rappelMajoration50pct: 75,
      rappelMajoration100pct: 0,
      rappelTotal: 225,
      reposCompensateurHeuresDues: 0,
      formule: '40 × 15 × 25% + 10 × 15 × 50% = 225,00 €',
      baseJuridique: 'Art. L.3121-28 Code du travail',
      messages: ['Prescription 3 ans (L.3245-1)'],
    };
  }

  function beResponse(): HeuresSupResponse {
    return {
      caseFileId: 'case-1',
      tauxHoraireBrut: 15,
      heuresSupDeclarees25pct: null,
      heuresSupDeclarees50pct: null,
      heuresHorsContingent: null,
      tauxMajoration25: null,
      tauxMajoration50: null,
      heuresSupSemaine: 30,
      heuresDimancheJoursFeries: 5,
      country: 'BELGIQUE',
      rappelMajoration25pct: 0,
      rappelMajoration50pct: 225,
      rappelMajoration100pct: 75,
      rappelTotal: 300,
      reposCompensateurHeuresDues: 35,
      formule: '30 × 15 × 50% + 5 × 15 × 100% = 300,00 €',
      baseJuridique: 'Art. 29 Loi 16/03/1971',
      messages: ['Repos compensatoire obligatoire'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        HeuresSupSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(HeuresSupSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('charge l\'analyse existante FR si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()!.rappelTotal).toBe(225);
    expect(component.showForm()).toBe(false);
    expect(component.tauxHoraireBrut()).toBe(15);
    expect(component.heuresSupDeclarees25pct()).toBe(40);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('FR : formValid false si taux ≤ 0 ou aucune heure saisie', () => {
    component.workspaceCountry = 'FRANCE';
    component.tauxHoraireBrut.set(0);
    component.heuresSupDeclarees25pct.set(40);
    expect(component.formValid()).toBe(false);

    component.tauxHoraireBrut.set(15);
    component.heuresSupDeclarees25pct.set(0);
    component.heuresSupDeclarees50pct.set(0);
    component.heuresHorsContingent.set(0);
    expect(component.formValid()).toBe(false);

    component.heuresSupDeclarees25pct.set(40);
    expect(component.formValid()).toBe(true);
  });

  it('FR : formValid false si tauxMajoration hors [10, 50]', () => {
    component.workspaceCountry = 'FRANCE';
    component.tauxHoraireBrut.set(15);
    component.heuresSupDeclarees25pct.set(40);
    component.tauxMajoration25.set(5);
    expect(component.formValid()).toBe(false);

    component.tauxMajoration25.set(25);
    component.tauxMajoration50.set(60);
    expect(component.formValid()).toBe(false);

    component.tauxMajoration50.set(50);
    expect(component.formValid()).toBe(true);
  });

  it('BE : formValid true si taux > 0 et au moins une heure > 0', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.tauxHoraireBrut.set(15);
    component.heuresSupSemaine.set(0);
    component.heuresDimancheJoursFeries.set(0);
    expect(component.formValid()).toBe(false);

    component.heuresSupSemaine.set(30);
    expect(component.formValid()).toBe(true);
  });

  it('FR calculate() POST + affiche résultat + snackbar succès', () => {
    component.workspaceCountry = 'FRANCE';
    component.tauxHoraireBrut.set(15);
    component.heuresSupDeclarees25pct.set(40);
    component.heuresSupDeclarees50pct.set(10);
    component.heuresHorsContingent.set(0);
    component.tauxMajoration25.set(25);
    component.tauxMajoration50.set(50);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      tauxHoraireBrut: 15,
      heuresSupDeclarees25pct: 40,
      heuresSupDeclarees50pct: 10,
      heuresHorsContingent: 0,
      tauxMajoration25: 25,
      tauxMajoration50: 50,
    });
    req.flush(frResponse());

    expect(component.result()!.rappelTotal).toBe(225);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Rappel heures sup calculé', 'OK', jasmine.any(Object));
  });

  it('BE calculate() POST envoie uniquement les champs BE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.tauxHoraireBrut.set(15);
    component.heuresSupSemaine.set(30);
    component.heuresDimancheJoursFeries.set(5);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      tauxHoraireBrut: 15,
      heuresSupSemaine: 30,
      heuresDimancheJoursFeries: 5,
    });
    req.flush(beResponse());

    expect(component.result()!.rappelTotal).toBe(300);
    expect(component.result()!.reposCompensateurHeuresDues).toBe(35);
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.tauxHoraireBrut.set(15);
    component.heuresSupDeclarees25pct.set(40);
    component.calculate();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.tauxHoraireBrut.set(null);
    component.calculate();
    httpMock.expectNone(r => r.method === 'POST');
  });
});

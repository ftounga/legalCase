import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RegimeCommunauteLegaleBeSectionComponent } from './regime-communaute-legale-be-section.component';
import { RegimeCommunauteLegaleBeResponse } from '../../core/models/regime-communaute-legale-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('RegimeCommunauteLegaleBeSectionComponent', () => {
  let component: RegimeCommunauteLegaleBeSectionComponent;
  let fixture: ComponentFixture<RegimeCommunauteLegaleBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/regime-mat-be-communaute-legale';

  function response(verdict: RegimeCommunauteLegaleBeResponse['verdict'])
      : RegimeCommunauteLegaleBeResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse (ré-édition du formulaire).
      dateMariage: '2015-06-20',
      contratMariageSigne: verdict === 'REGIME_CONVENTIONNEL_DETECTE',
      biens: [{
        libelle: 'Appartement', categorie: 'IMMOBILIER',
        acquisAvantMariage: false, acquisParDonationOuSuccession: false,
        financeParFondsPropres: false, affectationProfessionnelle: false,
        outilDeTravailPersonnel: false, commentaire: null,
      }],
      dettes: [],
      // Champs calculés.
      verdict,
      biensQualifies: [{
        libelle: 'Appartement', qualification: 'COMMUN',
        modeGestion: 'GESTION_CONCURRENTE',
        fondement: 'CC art. 3.39', explication: 'Bien acquis pendant le mariage.',
      }],
      dettesQualifiees: [],
      syntheseComposition: {
        nbBiensCommuns: 1, nbBiensPropres: 0, nbBiensMixtesAvecRecompense: 0,
        nbDettesCommunes: 0, nbDettesPropres: 0,
      },
      basesJuridiques: ['CC Livre 3', 'Loi du 22/07/2018'],
      messages: ['Vérifier la disponibilité de l\'acte de mariage.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-17T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        RegimeCommunauteLegaleBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegimeCommunauteLegaleBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Chargement (GET)
  // ---------------------------------------------------------------------------

  it('mount BELGIQUE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount FRANCE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('COMMUNAUTE_LEGALE_APPLICABLE'));
    expect(component.result()!.verdict).toBe('COMMUNAUTE_LEGALE_APPLICABLE');
    expect(component.showForm()).toBe(false);
  });

  it('GET 200 → formulaire ré-hydraté depuis le snapshot d\'inputs', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('COMMUNAUTE_LEGALE_APPLICABLE'));
    expect(component.dateMariage()).toBe('2015-06-20');
    expect(component.biens().length).toBe(1);
    expect(component.biens()[0].libelle).toBe('Appartement');

    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.dateMariage()).toBe('2015-06-20');
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Listes dynamiques de biens / dettes
  // ---------------------------------------------------------------------------

  it('addBien / removeBien gèrent la liste dynamique', () => {
    expect(component.biens().length).toBe(1);
    component.addBien();
    expect(component.biens().length).toBe(2);
    component.removeBien(0);
    expect(component.biens().length).toBe(1);
  });

  it('addDette / removeDette gèrent la liste dynamique', () => {
    expect(component.dettes().length).toBe(0);
    component.addDette();
    expect(component.dettes().length).toBe(1);
    component.removeDette(0);
    expect(component.dettes().length).toBe(0);
  });

  it('updateBien applique un patch partiel', () => {
    component.updateBien(0, { libelle: 'Maison', categorie: 'IMMOBILIER' });
    expect(component.biens()[0].libelle).toBe('Maison');
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux sans date de mariage', () => {
    component.updateBien(0, { libelle: 'Appartement' });
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux sans bien à libellé non vide', () => {
    component.dateMariage.set('2015-06-20');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.dateMariage.set('2015-06-20');
    component.updateBien(0, { libelle: 'Appartement' });
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai avec date ISO + bien à libellé', () => {
    component.dateMariage.set('2015-06-20');
    component.updateBien(0, { libelle: 'Appartement' });
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateMariage.set('2015-06-20');
    component.contratMariageSigne.set(false);
    component.updateBien(0, { libelle: 'Appartement ', categorie: 'IMMOBILIER' });
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateMariage).toBe('2015-06-20');
    expect(req.request.body.biens.length).toBe(1);
    // Le libellé est trimmé avant envoi.
    expect(req.request.body.biens[0].libelle).toBe('Appartement');
    req.flush(response('COMMUNAUTE_LEGALE_APPLICABLE'));

    expect(component.result()!.verdict).toBe('COMMUNAUTE_LEGALE_APPLICABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() filtre les biens à libellé vide', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateMariage.set('2015-06-20');
    component.updateBien(0, { libelle: 'Appartement' });
    component.addBien(); // bien #2 sans libellé
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.biens.length).toBe(1);
    req.flush(response('COMMUNAUTE_LEGALE_APPLICABLE'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateMariage.set('2015-06-20');
    component.updateBien(0, { libelle: 'Appartement' });
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  it('calculate() no-op si formulaire invalide (pas d\'appel POST)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Verdicts — rouge réservé à QUALIFICATION_INCOMPLETE
  // ---------------------------------------------------------------------------

  it('verdict QUALIFICATION_INCOMPLETE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--danger');
    expect(component.verdictBannerIcon('QUALIFICATION_INCOMPLETE')).toBe('error');
  });

  it('verdict REGIME_CONVENTIONNEL_DETECTE → banner or --medium (pas rouge)', () => {
    expect(component.verdictBannerClass('REGIME_CONVENTIONNEL_DETECTE')).toContain('--medium');
    expect(component.verdictBannerClass('REGIME_CONVENTIONNEL_DETECTE')).not.toContain('--danger');
  });

  it('verdict COMMUNAUTE_LEGALE_APPLICABLE → banner navy --available (pas rouge)', () => {
    expect(component.verdictBannerClass('COMMUNAUTE_LEGALE_APPLICABLE')).toContain('--available');
    expect(component.verdictBannerClass('COMMUNAUTE_LEGALE_APPLICABLE')).not.toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rcl-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
  });

  it('pas de bannière FR si workspace BELGIQUE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="rcl-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // getPrefillCount = 0 (PREFILL_COUNT_ALWAYS_ZERO)
  // ---------------------------------------------------------------------------

  it('getPrefillCount() retourne 0 (V1 — aucun flag patrimonial extrait)', () => {
    expect(RegimeCommunauteLegaleBeSectionComponent.getPrefillCount({})).toBe(0);
    expect(RegimeCommunauteLegaleBeSectionComponent.getPrefillCount({
      aiData: { dateMariage: '2015-06-20' },
    })).toBe(0);
    expect(RegimeCommunauteLegaleBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_MARIAGE présent si F-96 diverge de la saisie', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date de mariage',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_MARIAGE', expectedValue: '2016-09-01',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateMariage.set('2015-06-20');
    const alert = component.coherenceAlerts().DATE_MARIAGE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date de mariage',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_MARIAGE', expectedValue: '2016-09-01',
    }];
    component.standaloneMode = true;
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateMariage.set('2015-06-20');
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date de mariage',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_MARIAGE', expectedValue: '2016-09-01',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateMariage.set('2015-06-20');
    expect(component.coherenceAlerts().DATE_MARIAGE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_MARIAGE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});

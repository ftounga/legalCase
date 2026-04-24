import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HeuresSupSectionComponent } from './heures-sup-section.component';
import { HeuresSupResponse } from '../../core/models/heures-sup.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

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

  /** Fixture IA FR complète : salaire 3 034 € → taux 20.00 €/h dérivé. */
  function aiDataFrComplete(): TravailExtractedData {
    return {
      salaireBrutMensuel: 3034,
      heuresSupMentionneesDansDossier: {
        totalDeclarees25pct: 10,
        totalDeclarees50pct: 5,
        horsContingent: 2,
      },
      salaireEstDeduit: false,
    };
  }

  /** Simule un flush 404 sur GET initial (fallback mode formulaire + prefill IA). */
  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
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

  // ========================================================================
  // Tests existants SF-DT-19-02 — doivent rester verts
  // ========================================================================

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
    flush404();
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

  // ========================================================================
  // SF-155-04-A3 — pré-fill IA + validation F-IA-03
  // ========================================================================

  it('FR prefillFromAi complet — 4 champs remplis + 4 signals provenance IA', () => {
    component.aiData = aiDataFrComplete();
    component.ngOnInit();
    flush404();

    // 3034 / 151.67 = 20.004... arrondi à 20.00
    expect(component.tauxHoraireBrut()).toBe(20);
    expect(component.heuresSupDeclarees25pct()).toBe(10);
    expect(component.heuresSupDeclarees50pct()).toBe(5);
    expect(component.heuresHorsContingent()).toBe(2);
    expect(component.provenanceTauxHoraire()).toBe('IA');
    expect(component.provenanceHeures25()).toBe('IA');
    expect(component.provenanceHeures50()).toBe('IA');
    expect(component.provenanceHorsContingent()).toBe('IA');
  });

  it('FR prefillFromAi partiel — seul 25pct renseigné', () => {
    component.aiData = {
      heuresSupMentionneesDansDossier: {
        totalDeclarees25pct: 8,
        totalDeclarees50pct: null,
        horsContingent: null,
      },
    };
    component.ngOnInit();
    flush404();

    expect(component.heuresSupDeclarees25pct()).toBe(8);
    expect(component.provenanceHeures25()).toBe('IA');
    expect(component.heuresSupDeclarees50pct()).toBeNull();
    expect(component.provenanceHeures50()).toBeNull();
    expect(component.heuresHorsContingent()).toBeNull();
    expect(component.provenanceHorsContingent()).toBeNull();
    expect(component.tauxHoraireBrut()).toBeNull();
    expect(component.provenanceTauxHoraire()).toBeNull();
  });

  it('FR calcul tauxHoraireBrut arrondi 2 décimales', () => {
    // 1821.04 / 151.67 = 12.0066... → 12.01
    component.aiData = { salaireBrutMensuel: 1821.04 };
    component.ngOnInit();
    flush404();
    expect(component.tauxHoraireBrut()).toBe(12.01);
    expect(component.provenanceTauxHoraire()).toBe('IA');
  });

  it('FR prefillFromAi — salaireBrutMensuel absent → pas de taux horaire', () => {
    component.aiData = {
      heuresSupMentionneesDansDossier: { totalDeclarees25pct: 5 },
    };
    component.ngOnInit();
    flush404();
    expect(component.tauxHoraireBrut()).toBeNull();
    expect(component.provenanceTauxHoraire()).toBeNull();
    expect(component.heuresSupDeclarees25pct()).toBe(5);
  });

  it('FR prefillFromAi — salaireBrutMensuel ≤ 0 → garde-fou division', () => {
    component.aiData = { salaireBrutMensuel: 0 };
    component.ngOnInit();
    flush404();
    expect(component.tauxHoraireBrut()).toBeNull();
    expect(component.provenanceTauxHoraire()).toBeNull();
  });

  it('BE prefillFromAi null — aucun pré-fill IA même avec aiData complet', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = aiDataFrComplete();
    component.ngOnInit();
    flush404();

    expect(component.tauxHoraireBrut()).toBeNull();
    expect(component.heuresSupDeclarees25pct()).toBeNull();
    expect(component.provenanceTauxHoraire()).toBeNull();
    expect(component.provenanceHeures25()).toBeNull();
    expect(component.provenanceHeures50()).toBeNull();
    expect(component.provenanceHorsContingent()).toBeNull();
  });

  it('modification manuelle — badge provenance disparaît au onXxxChange', () => {
    component.aiData = aiDataFrComplete();
    component.ngOnInit();
    flush404();

    expect(component.provenanceTauxHoraire()).toBe('IA');
    component.onTauxHoraireChange();
    expect(component.provenanceTauxHoraire()).toBeNull();

    expect(component.provenanceHeures25()).toBe('IA');
    component.onHeures25Change();
    expect(component.provenanceHeures25()).toBeNull();

    expect(component.provenanceHeures50()).toBe('IA');
    component.onHeures50Change();
    expect(component.provenanceHeures50()).toBeNull();

    expect(component.provenanceHorsContingent()).toBe('IA');
    component.onHorsContingentChange();
    expect(component.provenanceHorsContingent()).toBeNull();
  });

  it('coherenceAlerts — divergence tauxHoraire > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3034 }; // dérive 20 €/h
    component.ngOnInit();
    flush404();

    // 30 € saisi contre 20 dérivé → 50 % d'écart, > seuil 10 %.
    component.onTauxHoraireChange();
    component.tauxHoraireBrut.set(30);
    expect(component.coherenceAlerts().TAUX_HORAIRE).toBeDefined();
    // SF-155-05 : `level` legacy → `severity` partagé.
    expect(component.coherenceAlerts().TAUX_HORAIRE!.severity).toBe('WARNING');
  });

  it('coherenceAlerts — pas d\'alerte taux horaire si écart < 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3034 }; // dérive 20
    component.ngOnInit();
    flush404();

    component.onTauxHoraireChange();
    component.tauxHoraireBrut.set(21); // 5 % d'écart
    expect(component.coherenceAlerts().TAUX_HORAIRE).toBeUndefined();
  });

  it('coherenceAlerts — divergence heures sup saisies > IA (abs ≥ 5 ET rel ≥ 50 %)', () => {
    component.aiData = {
      heuresSupMentionneesDansDossier: {
        totalDeclarees25pct: 5,
        totalDeclarees50pct: 3,
        horsContingent: 2,
      }, // total IA = 10 h
    };
    component.ngOnInit();
    flush404();

    // Avocat saisit 40 + 10 + 0 = 50 h → écart 40 h / 400 %
    component.onHeures25Change();
    component.onHeures50Change();
    component.onHorsContingentChange();
    component.heuresSupDeclarees25pct.set(40);
    component.heuresSupDeclarees50pct.set(10);
    component.heuresHorsContingent.set(0);
    expect(component.coherenceAlerts().HEURES_SUP).toBeDefined();
    expect(component.coherenceAlerts().HEURES_SUP!.severity).toBe('WARNING');
  });

  it('coherenceAlerts — pas d\'alerte heures sup si écart < 5 h', () => {
    component.aiData = {
      heuresSupMentionneesDansDossier: {
        totalDeclarees25pct: 10,
        totalDeclarees50pct: 5,
        horsContingent: 0,
      }, // total IA = 15
    };
    component.ngOnInit();
    flush404();

    // Avocat saisit 17 (écart 2 h, < seuil 5) — pas d'alerte
    component.onHeures25Change();
    component.onHeures50Change();
    component.onHorsContingentChange();
    component.heuresSupDeclarees25pct.set(12);
    component.heuresSupDeclarees50pct.set(5);
    component.heuresHorsContingent.set(0);
    expect(component.coherenceAlerts().HEURES_SUP).toBeUndefined();
  });

  it('coherenceAlerts — note info SALAIRE_DEDUIT si flag true', () => {
    component.aiData = {
      salaireBrutMensuel: 3034,
      salaireEstDeduit: true,
    };
    component.ngOnInit();
    flush404();

    const alert = component.coherenceAlerts().SALAIRE_DEDUIT;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('INFO');
  });

  it('coherenceAlerts vide en mode BE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = aiDataFrComplete();
    component.ngOnInit();
    flush404();
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });

  it('coherenceAlerts vide si showForm() === false (après calcul)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse()); // GET 200 → showForm false
    // Injecter aiData divergent post-load
    component.aiData = { salaireBrutMensuel: 999999 };
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });
    expect(component.showForm()).toBe(false);
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });

  it('ngOnChanges re-prefill quand aiData arrive après ngOnInit (form vierge)', () => {
    component.ngOnInit();
    flush404();
    // Pas d'aiData → form vierge
    expect(component.tauxHoraireBrut()).toBeNull();
    expect(component.provenanceTauxHoraire()).toBeNull();

    // aiData arrive ensuite
    component.aiData = { salaireBrutMensuel: 3034 };
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });
    expect(component.tauxHoraireBrut()).toBe(20);
    expect(component.provenanceTauxHoraire()).toBe('IA');
  });

  it('ngOnChanges — pas d\'écrasement manuel : taux saisi puis aiData arrive', () => {
    component.ngOnInit();
    flush404();
    // L'avocat saisit 18 manuellement
    component.onTauxHoraireChange();
    component.tauxHoraireBrut.set(18);
    expect(component.provenanceTauxHoraire()).toBeNull();
    // aiData arrive avec une autre valeur
    component.aiData = { salaireBrutMensuel: 3034 };
    component.ngOnChanges({
      aiData: new SimpleChange(null, component.aiData, false),
    });
    // La valeur saisie doit être préservée (canPrefill renvoie false).
    expect(component.tauxHoraireBrut()).toBe(18);
    expect(component.provenanceTauxHoraire()).toBeNull();
  });

  it('loadExisting priorise persistance (GET 200) — pas de prefill IA', () => {
    component.aiData = aiDataFrComplete();
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse()); // GET 200 → showForm false, values loaded from persisted
    expect(component.showForm()).toBe(false);
    expect(component.tauxHoraireBrut()).toBe(15); // valeur persistée, pas 20 (IA)
    expect(component.provenanceTauxHoraire()).toBeNull();
  });

  it('fixture malformée — heuresSupMentionneesDansDossier non-objet → graceful', () => {
    // @ts-expect-error simulate malformed payload
    component.aiData = {
      salaireBrutMensuel: 3034,
      heuresSupMentionneesDansDossier: 42,
    };
    component.ngOnInit();
    flush404();
    // Taux horaire OK depuis salaireBrutMensuel
    expect(component.tauxHoraireBrut()).toBe(20);
    // Mais pas d'exception ni pré-fill heures sup
    expect(component.heuresSupDeclarees25pct()).toBeNull();
    expect(component.heuresSupDeclarees50pct()).toBeNull();
    expect(component.heuresHorsContingent()).toBeNull();
  });

  it('alertsSummary reflète le nombre d\'alertes actives', () => {
    component.aiData = {
      salaireBrutMensuel: 3034,
      salaireEstDeduit: true,
      heuresSupMentionneesDansDossier: {
        totalDeclarees25pct: 5,
        totalDeclarees50pct: 0,
        horsContingent: 0,
      },
    };
    component.ngOnInit();
    flush404();

    // Pré-fill IA → taux=20, h25=5. Pas de divergence.
    // Mais salaireEstDeduit=true → SALAIRE_DEDUIT note.
    expect(component.alertsSummary().total).toBe(1);

    // L'avocat force taux 30 → ajoute TAUX_HORAIRE + SALAIRE_DEDUIT = 2 alertes
    component.onTauxHoraireChange();
    component.tauxHoraireBrut.set(30);
    expect(component.alertsSummary().total).toBe(2);
    expect(component.alertsSummary().blockers).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // SF-155-05 — interface `CoherenceAlert<HsAlertField>` partagée
  // ---------------------------------------------------------------------------

  it('SF-155-05 : alerte TAUX_HORAIRE expose contract CoherenceAlert — contributors=[IA], severity=WARNING', () => {
    component.aiData = { salaireBrutMensuel: 3034 }; // dérive 20 €/h
    component.ngOnInit();
    flush404();
    component.onTauxHoraireChange();
    component.tauxHoraireBrut.set(30); // 50 % d'écart
    const alert = component.coherenceAlerts().TAUX_HORAIRE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('TAUX_HORAIRE');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toContain('€/h');
  });

  it('SF-155-05 : alerte SALAIRE_DEDUIT expose severity=INFO (pas WARNING)', () => {
    component.aiData = { salaireBrutMensuel: 3034, salaireEstDeduit: true };
    component.ngOnInit();
    flush404();
    const alert = component.coherenceAlerts().SALAIRE_DEDUIT;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('INFO');
    expect(alert!.contributors).toEqual(['IA']);
  });
});

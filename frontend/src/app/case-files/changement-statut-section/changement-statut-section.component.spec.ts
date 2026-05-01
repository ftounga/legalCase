import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ChangementStatutSectionComponent } from './changement-statut-section.component';
import { ChangementStatutResponse } from '../../core/models/changement-statut.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('ChangementStatutSectionComponent (SF-IM-11-02)', () => {
  let component: ChangementStatutSectionComponent;
  let fixture: ComponentFixture<ChangementStatutSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/changement-statut-analysis';

  function response(overrides: Partial<ChangementStatutResponse> = {}): ChangementStatutResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      titreActuel: 'ETUDIANT',
      titreEnvisage: 'SALARIE',
      nouveauTitreEnvisage: 'SALARIE',
      dureeRestanteMois: 6,
      documentJustificatifFourni: true,
      remunerationContratEur: 2800,
      casierJudiciaireVierge: true,
      verdictTransition: 'ELEVEE',
      documentsRequis: ['Contrat de travail', 'Diplôme', 'Justificatif domicile'],
      risqueRefus: [],
      delaiInstructionMois: 3,
      baseJuridique: 'CESEDA L.421-1 + R.5221-3',
      formule: 'ETUDIANT (6 mois restants) + 2 800 € ≥ 1,5 SMIC + justificatif → ELEVEE',
      messages: ['Transition admise. Déposer la demande à la préfecture.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        ChangementStatutSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ChangementStatutSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // 1. Mount + lifecycle
  // ---------------------------------------------------------------------------

  it('FRANCE → isFrance() true, GET émis au ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('GET 200 → résultat hydraté + champs persistés + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.verdictTransition).toBe('ELEVEE');
    expect(component.titreActuel()).toBe('ETUDIANT');
    expect(component.titreEnvisage()).toBe('SALARIE');
    expect(component.dureeRestanteSurTitreActuelMois()).toBe(6);
    expect(component.documentJustificatifFourni()).toBe(true);
    expect(component.remunerationContratEur()).toBe(2800);
    expect(component.casierJudiciaireVierge()).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceTitreActuel()).toBeNull();
  });

  it('GET 404 → mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2. Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA : titreActuel ← typeTitreSejourCode (cas exact ETUDIANT via VLS_TS_ETUDIANT)', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.titreActuel()).toBe('ETUDIANT');
    expect(component.provenanceTitreActuel()).toBe('IA');
  });

  it('pré-fill IA : normalisation insensible casse (etudiant → ETUDIANT via heuristique texte)', () => {
    component.aiData = { typeTitreSejour: 'etudiant' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.titreActuel()).toBe('ETUDIANT');
    expect(component.provenanceTitreActuel()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.titreActuel()).toBeNull();
    expect(component.provenanceTitreActuel()).toBeNull();
  });

  it('pré-fill avec valeur IA non reconnue → no-op gracieux', () => {
    component.aiData = { typeTitreSejourCode: 'FOO_BAR' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.titreActuel()).toBeNull();
    expect(component.provenanceTitreActuel()).toBeNull();
  });

  it('onTitreActuelChange efface le badge IA', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceTitreActuel()).toBe('IA');
    component.onTitreActuelChange('VISITEUR');
    expect(component.titreActuel()).toBe('VISITEUR');
    expect(component.provenanceTitreActuel()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2bis. SF-IM-11-03 — Pré-fill durée restante depuis dateExpirationTitre
  // ---------------------------------------------------------------------------

  describe('SF-IM-11-03 — pré-fill durée restante', () => {
    beforeEach(() => {
      jest.useFakeTimers();
      // 2026-05-01 00:00 UTC : aligné avec l'interprétation civile de la date
      // "today = 2026-05-01" → delta vers '2026-08-31' = 122 jours → 4 mois.
      jest.setSystemTime(new Date('2026-05-01T00:00:00Z'));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('calcule 4 mois quand dateExpirationTitre = 2026-08-31 et today = 2026-05-01', () => {
      component.aiData = {
        dateExpirationTitre: '2026-08-31',
      } as ImmigrationExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(component.dureeRestanteSurTitreActuelMois()).toBe(4);
      expect(component.provenanceDureeRestante()).toBe('IA');
    });

    it('ne touche pas le champ quand aiData = null', () => {
      component.aiData = null;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(component.dureeRestanteSurTitreActuelMois()).toBeNull();
      expect(component.provenanceDureeRestante()).toBeNull();
    });

    it('ne touche pas le champ quand dateExpirationTitre est null ou chaîne vide', () => {
      component.aiData = { dateExpirationTitre: null } as ImmigrationExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(component.dureeRestanteSurTitreActuelMois()).toBeNull();
      expect(component.provenanceDureeRestante()).toBeNull();

      // Reset pour second cas (chaîne vide).
      const fixture2 = TestBed.createComponent(ChangementStatutSectionComponent);
      const c2 = fixture2.componentInstance;
      c2.caseFileId = 'case-1';
      c2.workspaceCountry = 'FRANCE';
      c2.aiData = { dateExpirationTitre: '' } as ImmigrationExtractedData;
      c2.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(c2.dureeRestanteSurTitreActuelMois()).toBeNull();
      expect(c2.provenanceDureeRestante()).toBeNull();
    });

    it('pose 0 quand dateExpirationTitre est dans le passé', () => {
      component.aiData = {
        dateExpirationTitre: '2024-01-01',
      } as ImmigrationExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(component.dureeRestanteSurTitreActuelMois()).toBe(0);
      expect(component.provenanceDureeRestante()).toBe('IA');
    });

    it('n\'écrase pas une valeur saisie manuellement (provenance null)', () => {
      // Saisie manuelle préalable.
      component.dureeRestanteSurTitreActuelMois.set(7);
      component.provenanceDureeRestante.set(null);

      component.aiData = {
        dateExpirationTitre: '2026-08-31',
      } as ImmigrationExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      // La saisie avocat (7) doit être préservée.
      expect(component.dureeRestanteSurTitreActuelMois()).toBe(7);
      expect(component.provenanceDureeRestante()).toBeNull();
    });

    it('onDureeRestanteChange remet provenanceDureeRestante à null', () => {
      component.aiData = {
        dateExpirationTitre: '2026-08-31',
      } as ImmigrationExtractedData;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(component.provenanceDureeRestante()).toBe('IA');

      component.onDureeRestanteChange(5);
      expect(component.dureeRestanteSurTitreActuelMois()).toBe(5);
      expect(component.provenanceDureeRestante()).toBeNull();
    });

    it('aiData arrivant après le mount via ngOnChanges déclenche le pré-fill', () => {
      component.aiData = null;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

      expect(component.dureeRestanteSurTitreActuelMois()).toBeNull();

      const newAi = {
        dateExpirationTitre: '2026-08-31',
      } as ImmigrationExtractedData;
      component.aiData = newAi;
      component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

      expect(component.dureeRestanteSurTitreActuelMois()).toBe(4);
      expect(component.provenanceDureeRestante()).toBe('IA');
    });
  });

  // ---------------------------------------------------------------------------
  // 3. Form validation
  // ---------------------------------------------------------------------------

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand tous les champs requis présents (transition non-SALARIE)', () => {
    component.titreActuel.set('ETUDIANT');
    component.titreEnvisage.set('APS');
    component.dureeRestanteSurTitreActuelMois.set(3);
    expect(component.formValid()).toBe(true);

    component.dureeRestanteSurTitreActuelMois.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si remuneration manquante quand titreEnvisage = SALARIE', () => {
    component.titreActuel.set('ETUDIANT');
    component.titreEnvisage.set('SALARIE');
    component.dureeRestanteSurTitreActuelMois.set(6);
    component.remunerationContratEur.set(null);
    expect(component.formValid()).toBe(false);

    component.remunerationContratEur.set(2800);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dureeRestante négatif ou rémunération négative', () => {
    component.titreActuel.set('ETUDIANT');
    component.titreEnvisage.set('SALARIE');
    component.dureeRestanteSurTitreActuelMois.set(-1);
    component.remunerationContratEur.set(2800);
    expect(component.formValid()).toBe(false);

    component.dureeRestanteSurTitreActuelMois.set(6);
    component.remunerationContratEur.set(-100);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // 4. Champ rémunération conditionnel
  // ---------------------------------------------------------------------------

  it('remunerationVisible() false si titreEnvisage != SALARIE', () => {
    component.titreEnvisage.set('APS');
    expect(component.remunerationVisible()).toBe(false);

    component.titreEnvisage.set('VPF');
    expect(component.remunerationVisible()).toBe(false);

    component.titreEnvisage.set('SALARIE');
    expect(component.remunerationVisible()).toBe(true);
  });

  it('onTitreEnvisageChange(non-SALARIE) efface remunerationContratEur', () => {
    component.remunerationContratEur.set(2800);
    component.onTitreEnvisageChange('APS');
    expect(component.remunerationContratEur()).toBeNull();
  });

  it('calculate() omet remunerationContratEur si titreEnvisage != SALARIE (même si signal a une valeur)', () => {
    component.titreActuel.set('ETUDIANT');
    component.titreEnvisage.set('APS');
    component.dureeRestanteSurTitreActuelMois.set(6);
    component.remunerationContratEur.set(2800); // résidu — doit être ignoré
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.remunerationContratEur).toBeUndefined();
    req.flush(response({ titreEnvisage: 'APS' }));
  });

  // ---------------------------------------------------------------------------
  // 5. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() POST + résultat + snackbar succès + triggerRefresh', () => {
    component.titreActuel.set('ETUDIANT');
    component.titreEnvisage.set('SALARIE');
    component.dureeRestanteSurTitreActuelMois.set(6);
    component.documentJustificatifFourni.set(true);
    component.remunerationContratEur.set(2800);
    component.casierJudiciaireVierge.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      titreActuel: 'ETUDIANT',
      titreEnvisage: 'SALARIE',
      dureeRestanteSurTitreActuelMois: 6,
      documentJustificatifFourni: true,
      casierJudiciaireVierge: true,
      remunerationContratEur: 2800,
    });
    req.flush(response());

    expect(component.result()!.verdictTransition).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Changement de statut analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.titreActuel.set('ETUDIANT');
    component.titreEnvisage.set('SALARIE');
    component.dureeRestanteSurTitreActuelMois.set(6);
    component.remunerationContratEur.set(2800);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // 6. F-IA-03 alertes de cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.TITRE_ACTUEL présent si IA diverge de saisie', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit ETUDIANT. Avocat saisit VISITEUR → divergence.
    component.onTitreActuelChange('VISITEUR');

    const alert = component.coherenceAlerts().TITRE_ACTUEL;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('TITRE_ACTUEL');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toContain('Étudiant');
  });

  it('coherenceAlerts.TITRE_ACTUEL absent si IA convergent', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.titreActuel()).toBe('ETUDIANT');
    expect(component.coherenceAlerts().TITRE_ACTUEL).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onTitreActuelChange('VISITEUR');
    expect(component.coherenceAlerts().TITRE_ACTUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().TITRE_ACTUEL).toBeUndefined();
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { typeTitreSejourCode: 'VLS_TS_ETUDIANT' } as ImmigrationExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Titre actuel',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM11_TITRE_ACTUEL',
        expectedValue: 'ETUDIANT',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Avocat saisit VISITEUR → divergence IA + F96 (même valeur ETUDIANT).
    component.onTitreActuelChange('VISITEUR');

    const alert = component.coherenceAlerts().TITRE_ACTUEL;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  // ---------------------------------------------------------------------------
  // 7. ngOnChanges + non-régression
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { typeTitreSejourCode: 'CST_VPF' } as ImmigrationExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.titreActuel()).toBe('VPF');
    expect(component.provenanceTitreActuel()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onTitreActuelChange('VISITEUR');
    expect(component.provenanceTitreActuel()).toBeNull();

    const newAi = { typeTitreSejourCode: 'CST_VPF' } as ImmigrationExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.titreActuel()).toBe('VISITEUR');
    expect(component.provenanceTitreActuel()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 8. Misc
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('ELEVEE')).toContain('cs-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('cs-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('cs-banner--critical');
    expect(component.bannerClass(null)).toContain('cs-banner--info');
  });

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel('ELEVEE')).toBe('Transition viable');
    expect(component.bannerLabel('MOYENNE')).toContain('conditions limites');
    expect(component.bannerLabel('FAIBLE')).toContain('compromise');
    expect(component.bannerLabel(null)).toBe('');
  });

  it('riskChipClass : "casier non vierge" → critical, "rémunération limite" → warning', () => {
    expect(component.riskChipClass('Casier non vierge — risque ordre public')).toContain('critical');
    expect(component.riskChipClass('Rejet probable')).toContain('critical');
    expect(component.riskChipClass('Rémunération limite (1,2 SMIC)')).toContain('warning');
  });

  it('titreLabel retourne le libellé humain ou le code en fallback', () => {
    expect(component.titreLabel('ETUDIANT')).toContain('Étudiant');
    expect(component.titreLabel('SALARIE')).toContain('Salarié');
    expect(component.titreLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.titreLabel(null)).toBe('');
  });
});

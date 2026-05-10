import { TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ReferentialEditDialogComponent, ReferentialEditDialogData } from './referential-edit-dialog.component';

const mockDialogRef = { close: jasmine.createSpy('close') };

function createComponent(sectionType: string, valueJson: string) {
  const data: ReferentialEditDialogData = {
    entry: { key: 'KEY', label: 'Test', valueJson, isSystem: true },
    sectionType,
  };
  TestBed.configureTestingModule({
    imports: [ReferentialEditDialogComponent],
    providers: [
      provideAnimationsAsync(),
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });
  const fixture = TestBed.createComponent(ReferentialEditDialogComponent);
  fixture.detectChanges();
  return fixture.componentInstance;
}

describe('ReferentialEditDialogComponent — formulaires typés', () => {
  beforeEach(() => mockDialogRef.close.calls.reset());

  // EDT-01 : LITIGATION_TYPE sérialise correctement
  it('EDT-01 : LITIGATION_TYPE → sérialise {years, article}', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    expect(c.form.get('litigYears')?.value).toBe(3);
    expect(c.form.get('litigArticle')?.value).toBe('Art. L3245-1');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ years: 3, article: 'Art. L3245-1' });
  });

  // EDT-02 : LITIGATION_TYPE invalide si années hors borne
  it('EDT-02 : LITIGATION_TYPE → invalide si années = 0', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(0);
    expect(c.form.get('litigYears')?.hasError('min')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  it('EDT-02b : LITIGATION_TYPE → invalide si années = 31', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(31);
    expect(c.form.get('litigYears')?.hasError('max')).toBe(true);
  });

  // EDT-03 : BAREME_MACRON toggle true → {"supported":true}
  it('EDT-03 : BAREME_MACRON → toggle true sérialise {"supported":true}', () => {
    const c = createComponent('BAREME_MACRON', '{"supported":true}');
    expect(c.form.get('baremeSupported')?.value).toBe(true);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ supported: true });
  });

  it('EDT-03b : BAREME_MACRON → toggle false sérialise {"supported":false}', () => {
    const c = createComponent('BAREME_MACRON', '{"supported":false}');
    c.form.get('baremeSupported')?.setValue(false);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ supported: false });
  });

  // EDT-04 : PENSION_TAUX grille 5×2 → matrice correcte
  it('EDT-04 : PENSION_TAUX → valeurs pré-remplies en %, sérialise en /100', () => {
    const c = createComponent('PENSION_TAUX', '[[0.18,0.11],[0.26,0.16],[0.30,0.19],[0.33,0.21],[0.35,0.22]]');
    expect(c.form.get('pension_0_0')?.value).toBeCloseTo(18, 1);
    expect(c.form.get('pension_0_1')?.value).toBeCloseTo(11, 1);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const matrix = JSON.parse(result.valueJson);
    expect(matrix.length).toBe(5);
    expect(matrix[0][0]).toBeCloseTo(0.18, 3);
    expect(matrix[0][1]).toBeCloseTo(0.11, 3);
  });

  // EDT-05 : PENSION_TAUX invalide si cellule > 100
  it('EDT-05 : PENSION_TAUX → invalide si cellule > 100', () => {
    const c = createComponent('PENSION_TAUX', '[[0.18,0.11],[0.26,0.16],[0.30,0.19],[0.33,0.21],[0.35,0.22]]');
    c.form.get('pension_2_0')?.setValue(101);
    expect(c.form.get('pension_2_0')?.hasError('max')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  // EDT-06 : PRESTATION_COEFF sérialise correctement
  it('EDT-06 : PRESTATION_COEFF → sérialise {coeff, dureeReferenceAns}', () => {
    const c = createComponent('PRESTATION_COEFF', '{"coeff":0.30,"dureeReferenceAns":8}');
    expect(c.form.get('prestCoeff')?.value).toBeCloseTo(30, 1);
    expect(c.form.get('prestDuree')?.value).toBe(8);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.coeff).toBeCloseTo(0.30, 3);
    expect(parsed.dureeReferenceAns).toBe(8);
  });

  // EDT-07 : IMMIGRATION_PIECES newlines → string[]
  it('EDT-07 : IMMIGRATION_PIECES → newlines sérialisés en string[], lignes vides filtrées', () => {
    const c = createComponent('IMMIGRATION_PIECES', '["Passeport","Titre de séjour"]');
    expect(c.form.get('piecesText')?.value).toContain('Passeport');
    c.form.get('piecesText')?.setValue('Passeport\n\nTitre de séjour\n');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual(['Passeport', 'Titre de séjour']);
  });

  // EDT-08 : dialog s'ouvre avec valeurs pré-remplies
  it('EDT-08 : dialog pré-remplit label depuis entry.label', () => {
    const c = createComponent('IMMIGRATION_JALONS', '[{"label":"test","offsetDays":30}]');
    expect(c.form.get('label')?.value).toBe('Test');
  });

  // ---- SF-110-12 : formulaires typés pour les 6 types restants -----------

  // EDT-09 : INDEMNITE_BAREMES — forme MACRON
  it('EDT-09 : INDEMNITE_BAREMES MACRON → tableau entries, tri par an, sérialise correctement', () => {
    const macron = '{"entries":[{"an":0,"min":0,"max":1},{"an":2,"min":3,"max":3.5},{"an":1,"min":1,"max":2}]}';
    const c = createComponent('INDEMNITE_BAREMES', macron);
    expect(c.form.get('baremeShape')?.value).toBe('MACRON');
    expect(c.macronEntries.length).toBe(3);
    expect(c.macronEntries.at(0).get('an')?.value).toBe(0);
    c.macronEntries.at(0).get('max')?.setValue(2); // modif
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.entries.length).toBe(3);
    expect(parsed.entries[0]).toEqual({ an: 0, min: 0, max: 2 }); // trié par an
    expect(parsed.entries[1].an).toBe(1);
    expect(parsed.entries[2].an).toBe(2);
  });

  it('EDT-09 bis : INDEMNITE_BAREMES MACRON → addMacronEntry et removeMacronEntry', () => {
    const c = createComponent('INDEMNITE_BAREMES', '{"entries":[{"an":0,"min":0,"max":1}]}');
    c.addMacronEntry();
    expect(c.macronEntries.length).toBe(2);
    c.removeMacronEntry(0);
    expect(c.macronEntries.length).toBe(1);
  });

  // EDT-09 ter : INDEMNITE_BAREMES — forme CCT109
  it('EDT-09 ter : INDEMNITE_BAREMES CCT109 → 2 champs, sérialise minSemaines/maxSemaines', () => {
    const c = createComponent('INDEMNITE_BAREMES', '{"minSemaines":3,"maxSemaines":17}');
    expect(c.form.get('baremeShape')?.value).toBe('CCT109');
    expect(c.form.get('cct109Min')?.value).toBe(3);
    expect(c.form.get('cct109Max')?.value).toBe(17);
    c.form.get('cct109Max')?.setValue(20);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ minSemaines: 3, maxSemaines: 20 });
  });

  it('EDT-09 quater : INDEMNITE_BAREMES CCT109 → erreur si min > max', () => {
    const c = createComponent('INDEMNITE_BAREMES', '{"minSemaines":3,"maxSemaines":17}');
    c.form.get('cct109Min')?.setValue(20);
    expect(c.form.hasError('minMaxInverted')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  it('EDT-09 quinquies : INDEMNITE_BAREMES schéma inconnu → fallback JSON', () => {
    const c = createComponent('INDEMNITE_BAREMES', '{"something":"else"}');
    expect(c.form.get('valueJson')).not.toBeNull();
    expect(c.form.get('baremeShape')).toBeNull();
  });

  // EDT-10 : GARDE_MODES
  it('EDT-10 : GARDE_MODES → 6 champs, split/join sur périodes, sérialise correctement', () => {
    const g = '{"repartitionType":"ALTERNEE_1_SUR_2","periodesA":["Semaine A"],"periodesB":["Semaine B"],"vacances":"Moitié","joursA":182,"joursB":183}';
    const c = createComponent('GARDE_MODES', g);
    expect(c.form.get('gardeRepartition')?.value).toBe('ALTERNEE_1_SUR_2');
    expect(c.form.get('gardePeriodesA')?.value).toBe('Semaine A');
    expect(c.form.get('gardeJoursA')?.value).toBe(182);
    c.form.get('gardeJoursA')?.setValue(180);
    c.form.get('gardePeriodesA')?.setValue('Semaine A\n\nSemaine A bis\n');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.repartitionType).toBe('ALTERNEE_1_SUR_2');
    expect(parsed.periodesA).toEqual(['Semaine A', 'Semaine A bis']); // lignes vides filtrées
    expect(parsed.periodesB).toEqual(['Semaine B']);
    expect(parsed.joursA).toBe(180);
    expect(parsed.joursB).toBe(183);
  });

  // EDT-11 : DIVORCE_ETAPES
  it('EDT-11 : DIVORCE_ETAPES → 4 champs typés, sérialise correctement', () => {
    const e = '{"ordre":1,"description":"Dépôt requête","delai":"J+0","obligatoire":true}';
    const c = createComponent('DIVORCE_ETAPES', e);
    expect(c.form.get('etapeOrdre')?.value).toBe(1);
    expect(c.form.get('etapeDelai')?.value).toBe('J+0');
    c.form.get('etapeOrdre')?.setValue(3);
    c.form.get('etapeObligatoire')?.setValue(false);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({
      ordre: 3,
      description: 'Dépôt requête',
      delai: 'J+0',
      obligatoire: false,
    });
  });

  it('EDT-11 bis : DIVORCE_ETAPES → invalide si ordre < 1', () => {
    const c = createComponent('DIVORCE_ETAPES', '{"ordre":1,"description":"X","delai":"—","obligatoire":true}');
    c.form.get('etapeOrdre')?.setValue(0);
    expect(c.form.invalid).toBe(true);
  });

  // EDT-12 : DIVORCE_PIECES
  it('EDT-12 : DIVORCE_PIECES → 2 champs, sérialise correctement', () => {
    const p = '{"description":"Acte mariage","obligatoire":true}';
    const c = createComponent('DIVORCE_PIECES', p);
    expect(c.form.get('pieceDescription')?.value).toBe('Acte mariage');
    expect(c.form.get('pieceObligatoire')?.value).toBe(true);
    c.form.get('pieceObligatoire')?.setValue(false);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({
      description: 'Acte mariage',
      obligatoire: false,
    });
  });

  // EDT-13 : CONVENTION_BAREMES complet (plus de convJson)
  it('EDT-13 : CONVENTION_BAREMES → congés + 2 tableaux dynamiques, sérialise trié par min', () => {
    const btp = '{"congesLegauxJours":25,"congesSupp":[{"min":10,"jours":4},{"min":5,"jours":2}],"primes":[{"min":5,"pct":4},{"min":3,"pct":2}]}';
    const c = createComponent('CONVENTION_BAREMES', btp);
    expect(c.form.get('convConges')?.value).toBe(25);
    expect(c.convCongesSupp.length).toBe(2);
    expect(c.convPrimes.length).toBe(2);
    c.addPrime();
    c.convPrimes.at(2).get('min')?.setValue(15);
    c.convPrimes.at(2).get('pct')?.setValue(12);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.congesLegauxJours).toBe(25);
    // tri par min
    expect(parsed.congesSupp[0].min).toBe(5);
    expect(parsed.congesSupp[1].min).toBe(10);
    expect(parsed.primes[0].min).toBe(3);
    expect(parsed.primes[1].min).toBe(5);
    expect(parsed.primes[2].min).toBe(15); // ajouté
    expect(parsed.primes[2].pct).toBe(12);
  });

  it('EDT-13 bis : CONVENTION_BAREMES → removeCongesSupp retire une ligne', () => {
    const c = createComponent('CONVENTION_BAREMES', '{"congesLegauxJours":25,"congesSupp":[{"min":10,"jours":4},{"min":5,"jours":2}],"primes":[]}');
    expect(c.convCongesSupp.length).toBe(2);
    c.removeCongesSupp(0);
    expect(c.convCongesSupp.length).toBe(1);
  });

  // EDT-14 : IMMIGRATION_JALONS
  it('EDT-14 : IMMIGRATION_JALONS → tableau racine, addJalon/removeJalon', () => {
    const j = '[{"label":"Instruction préfecture","offsetDays":120},{"label":"Silence vaut rejet","offsetDays":60}]';
    const c = createComponent('IMMIGRATION_JALONS', j);
    expect(c.jalons.length).toBe(2);
    expect(c.jalons.at(0).get('label')?.value).toBe('Instruction préfecture');
    c.addJalon();
    c.jalons.at(2).get('label')?.setValue('Nouveau jalon');
    c.jalons.at(2).get('offsetDays')?.setValue(30);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(Array.isArray(parsed)).toBe(true);
    expect(parsed.length).toBe(3);
    expect(parsed[2]).toEqual({ label: 'Nouveau jalon', offsetDays: 30 });
  });

  it('EDT-14 bis : IMMIGRATION_JALONS → schéma non-tableau → fallback JSON', () => {
    const c = createComponent('IMMIGRATION_JALONS', '{"not":"an array"}');
    expect(c.form.get('valueJson')).not.toBeNull();
    expect(c.form.get('jalons')).toBeNull();
  });

  it('EDT-14 ter : IMMIGRATION_JALONS → offsetDays invalide si > 1825', () => {
    const c = createComponent('IMMIGRATION_JALONS', '[{"label":"Test","offsetDays":120}]');
    c.jalons.at(0).get('offsetDays')?.setValue(2000);
    expect(c.jalons.at(0).get('offsetDays')?.hasError('max')).toBe(true);
  });

  // ----- SF-225-01 : 5 types orphelins -----

  // EDT-15 : CONVENTION_PREAVIS
  it('EDT-15 : CONVENTION_PREAVIS → champs article + matrice JSON, sérialise correctement', () => {
    const initial = '{"fonctions":{"OUVRIER":[{"min":0,"max":6,"mois":1}]},"article":"CCN Banque art. 27"}';
    const c = createComponent('CONVENTION_PREAVIS', initial);
    expect(c.form.get('preavisArticle')?.value).toBe('CCN Banque art. 27');
    const fonctionsRaw = c.form.get('preavisFonctions')?.value as string;
    expect(fonctionsRaw).toContain('OUVRIER');
    c.form.get('preavisArticle')?.setValue('CCN Banque art. 28');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.article).toBe('CCN Banque art. 28');
    expect(parsed.fonctions.OUVRIER).toBeDefined();
    expect(parsed.fonctions.OUVRIER[0].mois).toBe(1);
  });

  it('EDT-15 bis : CONVENTION_PREAVIS → matrice JSON invalide → form invalide', () => {
    const c = createComponent('CONVENTION_PREAVIS', '{"fonctions":{},"article":"X"}');
    c.form.get('preavisFonctions')?.setValue('not-json{');
    expect(c.form.get('preavisFonctions')?.hasError('invalidJson')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  // EDT-16 : TRAVAIL_PROCEDURE_JALONS
  it('EDT-16 : TRAVAIL_PROCEDURE_JALONS → FormArray procedureJalons (label, offsetDays, articleRef)', () => {
    const j = '[{"label":"BCO","offsetDays":45,"articleRef":"R.1452-3"},{"label":"Jugement","offsetDays":270,"articleRef":"R.1454-25"}]';
    const c = createComponent('TRAVAIL_PROCEDURE_JALONS', j);
    expect(c.procedureJalons.length).toBe(2);
    expect(c.procedureJalons.at(0).get('label')?.value).toBe('BCO');
    expect(c.procedureJalons.at(0).get('articleRef')?.value).toBe('R.1452-3');
    c.addProcedureJalon();
    c.procedureJalons.at(2).get('label')?.setValue('Notification');
    c.procedureJalons.at(2).get('offsetDays')?.setValue(330);
    c.procedureJalons.at(2).get('articleRef')?.setValue('R.1454-26');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(Array.isArray(parsed)).toBe(true);
    expect(parsed.length).toBe(3);
    expect(parsed[2]).toEqual({ label: 'Notification', offsetDays: 330, articleRef: 'R.1454-26' });
  });

  it('EDT-16 bis : TRAVAIL_PROCEDURE_JALONS → articleRef vide est omis du payload', () => {
    const c = createComponent('TRAVAIL_PROCEDURE_JALONS', '[{"label":"X","offsetDays":1,"articleRef":""}]');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed[0]).toEqual({ label: 'X', offsetDays: 1 });
    expect('articleRef' in parsed[0]).toBe(false);
  });

  it('EDT-16 ter : TRAVAIL_PROCEDURE_JALONS → schéma non-tableau → fallback JSON', () => {
    const c = createComponent('TRAVAIL_PROCEDURE_JALONS', '{"not":"array"}');
    expect(c.form.get('valueJson')).not.toBeNull();
    expect(c.form.get('procedureJalons')).toBeNull();
  });

  // EDT-17 : FAMILLE_PROCEDURE_JALONS (même builder)
  it('EDT-17 : FAMILLE_PROCEDURE_JALONS → FormArray procedureJalons fonctionne', () => {
    const j = '[{"label":"Dépôt","offsetDays":0,"articleRef":"CJ Art. 572bis"}]';
    const c = createComponent('FAMILLE_PROCEDURE_JALONS', j);
    expect(c.procedureJalons.length).toBe(1);
    expect(c.procedureJalons.at(0).get('label')?.value).toBe('Dépôt');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed[0].label).toBe('Dépôt');
    expect(parsed[0].articleRef).toBe('CJ Art. 572bis');
  });

  // EDT-18 : MAJEURS_PROTEGES_REGIMES
  it('EDT-18 : MAJEURS_PROTEGES_REGIMES → 5 champs typés, sérialise correctement', () => {
    const m = '{"articles":["Cciv 425","Cciv 440"],"delaiProcedureMois":8,"delaiInitialAnsMax":5,"renouvelable":true,"criteresEligibilite":["A","B"]}';
    const c = createComponent('MAJEURS_PROTEGES_REGIMES', m);
    expect(c.form.get('mpDelaiProcedure')?.value).toBe(8);
    expect(c.form.get('mpDureeInitiale')?.value).toBe(5);
    expect(c.form.get('mpRenouvelable')?.value).toBe(true);
    expect(c.form.get('mpArticles')?.value).toContain('Cciv 425');
    expect(c.form.get('mpCriteres')?.value).toContain('A');
    c.form.get('mpDelaiProcedure')?.setValue(10);
    c.form.get('mpRenouvelable')?.setValue(false);
    c.form.get('mpCriteres')?.setValue('A\nB\nC\n');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.delaiProcedureMois).toBe(10);
    expect(parsed.delaiInitialAnsMax).toBe(5);
    expect(parsed.renouvelable).toBe(false);
    expect(parsed.articles).toEqual(['Cciv 425', 'Cciv 440']);
    expect(parsed.criteresEligibilite).toEqual(['A', 'B', 'C']);
  });

  it('EDT-18 bis : MAJEURS_PROTEGES_REGIMES → délai procédure invalide si > 60', () => {
    const c = createComponent('MAJEURS_PROTEGES_REGIMES', '{"articles":["X"],"delaiProcedureMois":8,"delaiInitialAnsMax":5,"renouvelable":true,"criteresEligibilite":["X"]}');
    c.form.get('mpDelaiProcedure')?.setValue(120);
    expect(c.form.get('mpDelaiProcedure')?.hasError('max')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  // EDT-19 : IM21_VALIDITY_CRITERES
  it('EDT-19 : IM21_VALIDITY_CRITERES → toggle binaire + description, sérialise correctement', () => {
    const i = '{"binaire":true,"description":"Le demandeur est-il en situation régulière ?"}';
    const c = createComponent('IM21_VALIDITY_CRITERES', i);
    expect(c.form.get('im21Binaire')?.value).toBe(true);
    expect(c.form.get('im21Description')?.value).toBe('Le demandeur est-il en situation régulière ?');
    c.form.get('im21Binaire')?.setValue(false);
    c.form.get('im21Description')?.setValue('Critère reformulé');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({
      binaire: false,
      description: 'Critère reformulé',
    });
  });

  it('EDT-19 bis : IM21_VALIDITY_CRITERES → description vide → form invalide', () => {
    const c = createComponent('IM21_VALIDITY_CRITERES', '{"binaire":true,"description":"X"}');
    c.form.get('im21Description')?.setValue('');
    expect(c.form.get('im21Description')?.hasError('required')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  // ---- SF-225-02 : validation IA live (pattern F-IA-03) ------------------

  it('EDT-FIA03-01 : LITIGATION_TYPE → years=15 déclenche alerte LITIG_YEARS (>10 inhabituel)', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(15);
    const alert = c.coherenceAlerts().LITIG_YEARS;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toBe('≤ 10 ans');
    expect(alert!.reason).toContain('15 ans');
  });

  it('EDT-FIA03-01 bis : LITIGATION_TYPE → years=5 ne déclenche aucune alerte', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(5);
    expect(c.coherenceAlerts().LITIG_YEARS).toBeUndefined();
  });

  it('EDT-FIA03-02 : IMMIGRATION_TITLES → titleDelai=1200 déclenche alerte TITLE_DELAI', () => {
    const c = createComponent('IMMIGRATION_TITLES',
      '{"motif":"X","conditions":"Y","pieces":["P"],"delaiMoyenJours":120}');
    c.form.get('titleDelai')?.setValue(1200);
    const alert = c.coherenceAlerts().TITLE_DELAI;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('≤ 999 jours');
    expect(alert!.reason).toContain('1200');
  });

  it('EDT-FIA03-03 : IMMIGRATION_RECOURS → recoursDelai=180 déclenche alerte RECOURS_DELAI', () => {
    const c = createComponent('IMMIGRATION_RECOURS',
      '{"delaiJours":30,"juridiction":"TA","textesApplicables":[],"piecesStandard":[]}');
    c.form.get('recoursDelai')?.setValue(180);
    const alert = c.coherenceAlerts().RECOURS_DELAI;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('2–90 jours');
  });

  it('EDT-FIA03-03 bis : IMMIGRATION_RECOURS → recoursDelai=30 (nominal) → pas d\'alerte', () => {
    const c = createComponent('IMMIGRATION_RECOURS',
      '{"delaiJours":30,"juridiction":"TA","textesApplicables":[],"piecesStandard":[]}');
    expect(c.coherenceAlerts().RECOURS_DELAI).toBeUndefined();
  });

  it('EDT-FIA03-04 : MAJEURS_PROTEGES_REGIMES → mpDelaiProcedure=24 déclenche alerte MP_DELAI_PROCEDURE', () => {
    const c = createComponent('MAJEURS_PROTEGES_REGIMES',
      '{"articles":["X"],"delaiProcedureMois":8,"delaiInitialAnsMax":5,"renouvelable":true,"criteresEligibilite":["X"]}');
    c.form.get('mpDelaiProcedure')?.setValue(24);
    const alert = c.coherenceAlerts().MP_DELAI_PROCEDURE;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('≤ 12 mois');
    expect(alert!.reason).toContain('24 mois');
  });

  it('EDT-FIA03-05 : IM21_VALIDITY_CRITERES → description > 800 char déclenche alerte INFO', () => {
    const c = createComponent('IM21_VALIDITY_CRITERES', '{"binaire":true,"description":"court"}');
    const longDesc = 'A'.repeat(900);
    c.form.get('im21Description')?.setValue(longDesc);
    const alert = c.coherenceAlerts().IM21_DESCRIPTION;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('INFO');
    expect(alert!.expectedDisplay).toBe('≤ 800 char');
  });

  it('EDT-FIA03-06 : CONVENTION_BAREMES → convConges=15 déclenche alerte CONV_CONGES', () => {
    const c = createComponent('CONVENTION_BAREMES',
      '{"congesLegauxJours":25,"congesSupp":[],"primes":[]}');
    c.form.get('convConges')?.setValue(15);
    const alert = c.coherenceAlerts().CONV_CONGES;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('≥ 20 jours');
    expect(alert!.reason).toContain('15 jours');
  });

  it('EDT-FIA03-07 : LICENCIEMENT_CRITERES → critPoids=35 déclenche alerte CRIT_POIDS', () => {
    const c = createComponent('LICENCIEMENT_CRITERES',
      '{"poids":10,"bloquant":false,"description":"D"}');
    c.form.get('critPoids')?.setValue(35);
    const alert = c.coherenceAlerts().CRIT_POIDS;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('< 30');
    expect(alert!.reason).toContain('35');
  });

  it('EDT-FIA03-08 : DIVORCE_ETAPES → etapeOrdre=15 déclenche alerte ETAPE_ORDRE (INFO)', () => {
    const c = createComponent('DIVORCE_ETAPES',
      '{"ordre":1,"description":"X","delai":"—","obligatoire":true}');
    c.form.get('etapeOrdre')?.setValue(15);
    const alert = c.coherenceAlerts().ETAPE_ORDRE;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('INFO');
    expect(alert!.expectedDisplay).toBe('≤ 10');
  });

  it('EDT-FIA03-NONE : valeurs nominales → coherenceAlerts() retourne {}', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    expect(Object.keys(c.coherenceAlerts())).toEqual([]);
  });

  it('EDT-FIA03-BUILDER : alert utilise CoherenceAlertBuilder (source IA, contributors=[\'IA\'])', () => {
    // Vérifie que les alertes utilisent bien le helper partagé (source/contributors normalisés).
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(20);
    const alert = c.coherenceAlerts().LITIG_YEARS;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.field).toBe('LITIG_YEARS');
  });

  it('EDT-FIA03-TOOLTIP : alertTooltip / alertBadgeLabel produisent un texte lisible', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(20);
    const alert = c.coherenceAlerts().LITIG_YEARS!;
    expect(c.alertTooltip(alert)).toContain('20 ans');
    expect(c.alertBadgeLabel(alert)).toContain('Valeur inhabituelle');
  });
});

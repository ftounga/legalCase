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
});

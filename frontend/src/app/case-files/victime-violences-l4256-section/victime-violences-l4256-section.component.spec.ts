import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { VictimeViolencesL4256SectionComponent } from './victime-violences-l4256-section.component';

describe('VictimeViolencesL4256SectionComponent', () => {
  let component: VictimeViolencesL4256SectionComponent;
  let fixture: ComponentFixture<VictimeViolencesL4256SectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VictimeViolencesL4256SectionComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(VictimeViolencesL4256SectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'test-case-id';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle collapsed state', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('should expand when forceExpanded is true', () => {
    component.collapsed.set(true);
    component.forceExpanded = true;
    component.ngOnInit();
    expect(component.collapsed()).toBe(false);
  });
});

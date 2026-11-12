import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, afterEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { AdminSessionService } from './core/auth/admin-session.service';
import { AuthApiService } from './core/api/auth-api.service';

describe('Accès visibles aux espaces administrateur', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([]), {provide:AuthApiService,useValue:{}}] });
  });
  afterEach(() => localStorage.clear());

  it('propose un accès administrateur dans la navigation publique', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('header a[href="/admin/login"]');
    expect(link?.textContent).toContain('Accès administrateur');
  });

  for (const [role, label] of [['GLOBAL', 'Administration globale'], ['AGENCE', 'Administration de mon agence']] as const) {
    it(`identifie clairement le périmètre ${role}`, () => {
      const session = TestBed.inject(AdminSessionService);
      session.setSession({ token: 'test-only', email:'test@example.test', nom:'Test', prenom:'UI', role, agenceId: role === 'AGENCE' ? 1 : null });
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('header a[href="/admin"]')?.textContent).toContain(label);
      expect(fixture.nativeElement.querySelector('header a[href="/admin/login"]')).toBeNull();
    });
  }
});

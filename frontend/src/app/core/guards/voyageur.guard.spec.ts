import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthApiService } from '../api/auth-api.service';
import { VOYAGEUR_SESSION_KEY } from '../auth/voyageur-session.service';
import { voyageurGuard } from './voyageur.guard';

describe('voyageurGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthApiService, useValue: { loginVoyageur: vi.fn() } }, // ← juste ça
      ],
    });
  });

  it('devrait rediriger vers /voyageur quand aucune session voyageur n\'existe', () => {
    const router = TestBed.inject(Router);

    const result = TestBed.runInInjectionContext(() => voyageurGuard({} as never, {} as never));

    expect(result).toEqual(router.parseUrl('/voyageur'));
  });

  it('devrait autoriser l\'accès quand la session voyageur existe', () => {
    localStorage.setItem(
      VOYAGEUR_SESSION_KEY,
      JSON.stringify({
        voyageur: {
          id: 3,
          matricule: 'G1234',
          nom: 'Doe',
          prenom: 'Jane',
          email: 'jane@example.com',
          typeVoyageur: 'GLOBAL',
          agenceId: null,
          agenceNom: null,
          solde: 0,
          token: null,
        },
        token: null,
      }),
    );

    const result = TestBed.runInInjectionContext(() => voyageurGuard({} as never, {} as never));

    expect(result).toBe(true);
  });
});

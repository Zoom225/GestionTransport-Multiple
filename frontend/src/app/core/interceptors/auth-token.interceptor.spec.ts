import { HttpRequest, HttpResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { firstValueFrom, of } from 'rxjs';
import { AdminSessionService } from '../auth/admin-session.service';
import { VoyageurSessionService } from '../auth/voyageur-session.service';
import { authTokenInterceptor } from './auth-token.interceptor';
import { AuthApiService } from '../api/auth-api.service';

describe('authTokenInterceptor', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthApiService, useValue: { loginVoyageur: vi.fn() } }, // ← fix
      ],
    });
  });

  it('devrait attacher le token bearer sur les appels API protégés', async () => {
    const session = TestBed.inject(AdminSessionService);
    session.setSession({
      token: 'jwt-token',
      email: 'admin@example.com',
      nom: 'Admin',
      prenom: 'Root',
      role: 'GLOBAL',
      agenceId: null
    });

    let receivedRequest!: HttpRequest<unknown>;

    await firstValueFrom(
      TestBed.runInInjectionContext(() =>
        authTokenInterceptor(new HttpRequest('GET', '/api/agences'), (req) => {
          receivedRequest = req;
          return of(new HttpResponse({ status: 200 }));
        })
      )
    );

    expect(receivedRequest?.headers.get('Authorization')).toBe('Bearer jwt-token');
  });

  it('ne devrait pas attacher le token sur l\'appel de connexion', async () => {
    const session = TestBed.inject(AdminSessionService);
    session.setSession({
      token: 'jwt-token',
      email: 'admin@example.com',
      nom: 'Admin',
      prenom: 'Root',
      role: 'GLOBAL',
      agenceId: null
    });

    let receivedRequest!: HttpRequest<unknown>;

    await firstValueFrom(
      TestBed.runInInjectionContext(() =>
        authTokenInterceptor(new HttpRequest('POST', '/api/auth/login', null), (req) => {
          receivedRequest = req;
          return of(new HttpResponse({ status: 200 }));
        })
      )
    );

    expect(receivedRequest?.headers.has('Authorization')).toBe(false);
  });

  it('devrait utiliser le token voyageur quand seule la session voyageur existe', async () => {
    const session = TestBed.inject(VoyageurSessionService);
    session.setVoyageur({
      id: 1,
      matricule: 'G1001',
      nom: 'Doe',
      prenom: 'John',
      email: 'john@example.com',
      typeVoyageur: 'VIP',
      agenceId: null,
      agenceNom: null,
      solde: 0,
      token: 'voyageur-token'
    });

    let receivedRequest!: HttpRequest<unknown>;

    await firstValueFrom(
      TestBed.runInInjectionContext(() =>
        authTokenInterceptor(new HttpRequest('GET', '/api/reservations/voyageur/1'), (req) => {
          receivedRequest = req;
          return of(new HttpResponse({ status: 200 }));
        })
      )
    );

    expect(receivedRequest?.headers.get('Authorization')).toBe('Bearer voyageur-token');
  });

  it('limite le jeton à l’API configurée sur un autre hôte', async () => {
    const session = TestBed.inject(AdminSessionService);
    session.setSession({
      token: 'jwt-token',
      email: 'admin@example.test',
      nom: 'Admin',
      prenom: 'Validation',
      role: 'GLOBAL',
      agenceId: null,
    });

    const previous = environment.apiBaseUrl;
    environment.apiBaseUrl = 'https://api.example.test/api';
    try {
      for (const [url, expected] of [
        ['https://api.example.test/api/agences', 'Bearer jwt-token'],
        ['https://api.example.test/api/auth/login', null],
        ['https://api.example.test/api-autre', null],
        ['https://autre.example.test/api/agences', null],
      ] as const) {
        await firstValueFrom(TestBed.runInInjectionContext(() =>
          authTokenInterceptor(new HttpRequest('GET', url), req => {
            expect(req.headers.get('Authorization')).toBe(expected);
            return of(new HttpResponse({ status: 200 }));
          }),
        ));
      }
    } finally {
      environment.apiBaseUrl = previous;
    }
  });
});

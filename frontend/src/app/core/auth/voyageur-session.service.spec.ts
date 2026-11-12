import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthApiService } from '../api/auth-api.service';
import { VOYAGEUR_SESSION_KEY, VoyageurSessionService } from './voyageur-session.service';

describe('VoyageurSessionService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthApiService,
          useValue: { loginVoyageur: vi.fn() }, // ← mock AuthApiService
        },
      ],
    });
  });

  it('devrait persister et restaurer une session voyageur', () => {
    TestBed.configureTestingModule({});
    const service = TestBed.inject(VoyageurSessionService);

    service.setVoyageur({
      id: 1,
      matricule: 'G1234',
      nom: 'Doe',
      prenom: 'John',
      email: 'john@example.com',
      typeVoyageur: 'GLOBAL',
      agenceId: null,
      agenceNom: null,
      solde: 0
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.voyageurId()).toBe(1);
    expect(service.matricule()).toBe('G1234');
  });

  it('devrait restaurer une session voyageur depuis localStorage', () => {
    localStorage.setItem(
      VOYAGEUR_SESSION_KEY,
      JSON.stringify({
        voyageur: {
          id: 9,
          matricule: 'S12345',
          nom: 'Smith',
          prenom: 'Anna',
          email: 'anna@example.com',
          typeVoyageur: 'AGENCE',
          agenceId: 5,
          agenceNom: 'Transport Bruxelles',
          solde: 12,
        },
        token: null,
      }),
    );

    TestBed.configureTestingModule({});
    const service = TestBed.inject(VoyageurSessionService);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.voyageurId()).toBe(9);
    expect(service.matricule()).toBe('S12345');
  });

  it('devrait nettoyer un contenu localStorage voyageur invalide', () => {
    localStorage.setItem(VOYAGEUR_SESSION_KEY, '{broken');

    TestBed.configureTestingModule({});
    const service = TestBed.inject(VoyageurSessionService);

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(VOYAGEUR_SESSION_KEY)).toBeNull();
  });

  it('devrait effacer la session voyageur', () => {
    TestBed.configureTestingModule({});
    const service = TestBed.inject(VoyageurSessionService);

    service.setVoyageur({
      id: 2,
      matricule: 'S12345',
      nom: 'Smith',
      prenom: 'Anna',
      email: 'anna@example.com',
      typeVoyageur: 'AGENCE',
      agenceId: 1,
      agenceNom: 'Agence A',
      solde: 10
    });

    service.clearVoyageur();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.voyageur()).toBeNull();
  });
});

import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { VoyageurResponse } from '../../../shared/models/voyageur.model';
import { VoyageurHomePage } from './voyageur-home-page.component';

describe('VoyageurHomePage', () => {
  const voyageur: VoyageurResponse = {
    id: 1,
    matricule: 'S10001',
    nom: 'Bernard',
    prenom: 'Tom',
    email: 'tom.bernard@email.com',
    typeVoyageur: 'AGENCE',
    agenceId: 1,
    agenceNom: 'Agence Lyon',
    solde: 0
  };

  let voyageursApiMock: { getByMatricule: ReturnType<typeof vi.fn> };
  let voyageurSessionMock = {
    isAuthenticated: vi.fn().mockReturnValue(false),
    setVoyageur: vi.fn(),
    login: vi.fn().mockReturnValue(of(voyageur)),
  };

  beforeEach(async () => {
    vi.useFakeTimers();

    voyageursApiMock = {
      getByMatricule: vi.fn().mockReturnValue(of(voyageur))
    };

    voyageurSessionMock = {
      isAuthenticated: vi.fn().mockReturnValue(false),
      setVoyageur: vi.fn(),
      login: vi.fn().mockReturnValue(of(voyageur))
    };

    await TestBed.configureTestingModule({
      imports: [VoyageurHomePage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: VoyageursApiService, useValue: voyageursApiMock },
        { provide: VoyageurSessionService, useValue: voyageurSessionMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('redirige vers le profil si un voyageur est déjà connecté', () => {
    voyageurSessionMock.isAuthenticated.mockReturnValue(true);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    TestBed.createComponent(VoyageurHomePage);

    expect(navigateSpy).toHaveBeenCalledWith('/voyageur/profile');
  });

  it('normalise le matricule, sauvegarde la session et redirige après succès', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(VoyageurHomePage);
    const component = fixture.componentInstance;

    component.form.controls.matricule.setValue('s10001');
    component.form.controls.password.setValue('Voyageur1234!'); // Added
    component.submit();

    expect(voyageurSessionMock.login).toHaveBeenCalledWith('S10001', 'Voyageur1234!'); // Modified
    expect(component.foundVoyageur()).toEqual(voyageur);

    vi.advanceTimersByTime(600);

    expect(navigateSpy).toHaveBeenCalledWith('/voyageur/profile');
  });

  it('affiche un message utile quand les identifiants sont invalides', () => { // Modified description
    voyageurSessionMock.login.mockReturnValue(
      throwError(() => ({ error: { message: 'Identifiants invalides' } })),
    );
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(VoyageurHomePage);
    const component = fixture.componentInstance;

    component.form.controls.matricule.setValue('L99999');
    component.form.controls.password.setValue('wrong-password'); // Added
    component.submit();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toContain('Identifiants invalides'); // Modified
    expect(voyageurSessionMock.setVoyageur).not.toHaveBeenCalled();
    expect(navigateSpy).not.toHaveBeenCalledWith('/voyageur/profile');
  });
});

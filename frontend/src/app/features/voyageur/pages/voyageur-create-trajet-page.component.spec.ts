import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { VehiculesApiService } from '../../../core/api/vehicules-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { VoyageurResponse } from '../../../shared/models/voyageur.model';
import { AgenceResponse, VehiculeResponse } from '../../../shared/models/agence-vehicule.model';
import { VoyageurCreateTrajetPage } from './voyageur-create-trajet-page.component';

const formatDate = (date: Date): string => {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

describe('VoyageurCreateTrajetPage', () => {
  const voyageur: VoyageurResponse = {
    id: 1,
    matricule: 'S10001',
    nom: 'Bernard',
    prenom: 'Tom',
    email: 'tom.bernard@email.com',
    typeVoyageur: 'LOCAL',
    agenceId: 1,
    agenceNom: 'Agence Lyon',
    solde: 0
  };

  const agences: AgenceResponse[] = [
    {
      id: 1,
      nom: 'Agence Lyon',
      adresse: 'Lyon',
      heureOuverture: '08:00',
      heureFermeture: '22:00',
      dureeTrajetMinutes: 90,
      dureeEntreTrajetMinutes: 15,
      anneeCivile: 2026
    }
  ];

  const vehicules: VehiculeResponse[] = [
    { id: 10, nom: 'Vehicule A', agenceId: 1, agenceNom: 'Agence Lyon' }
  ];

  const createdTrajet: TrajetResponse = {
    id: 99,
    vehiculeId: 10,
    vehiculeNom: 'Vehicule A',
    agenceNom: 'Agence Lyon',
    organisateurId: 1,
    organisateurNom: 'Tom Bernard',
    date: '2026-05-10',
    heureDebut: '09:00',
    heureFin: '10:30',
    typeTrajet: 'PRIVE',
    statut: 'PLANIFIE',
    nbPassagersActuels: 1,
    prixParPassager: 15,
    dateConversionPublic: null
  };

  let agencesApiMock: { getAll: ReturnType<typeof vi.fn> };
  let vehiculesApiMock: { getByAgence: ReturnType<typeof vi.fn> };
  let trajetsApiMock: { create: ReturnType<typeof vi.fn> };
  let reservationsApiMock: { createPrivateInvites: ReturnType<typeof vi.fn> };
  let voyageurSessionMock: {
    isAuthenticated: ReturnType<typeof vi.fn>;
    voyageur: ReturnType<typeof vi.fn>;
  };
  let activatedRouteMock: { snapshot: { queryParamMap: { get: ReturnType<typeof vi.fn> } } };

  beforeEach(async () => {
    agencesApiMock = {
      getAll: vi.fn().mockReturnValue(of(agences))
    };
    vehiculesApiMock = {
      getByAgence: vi.fn().mockReturnValue(of(vehicules))
    };
    trajetsApiMock = {
      create: vi.fn().mockReturnValue(of(createdTrajet))
    };
    reservationsApiMock = {
      createPrivateInvites: vi.fn().mockReturnValue(of([]))
    };
    voyageurSessionMock = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      voyageur: vi.fn().mockReturnValue(voyageur)
    };
    activatedRouteMock = {
      snapshot: {
        queryParamMap: {
          get: vi.fn().mockReturnValue(null)
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [VoyageurCreateTrajetPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AgencesApiService, useValue: agencesApiMock },
        { provide: VehiculesApiService, useValue: vehiculesApiMock },
        { provide: TrajetsApiService, useValue: trajetsApiMock },
        { provide: ReservationsApiService, useValue: reservationsApiMock },
        { provide: VoyageurSessionService, useValue: voyageurSessionMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('initialise cette agence, le vehicule et la date minimale pour un voyageur LOCAL', () => {
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;

    expect(component.isAgenceVoyageur()).toBe(true);
    expect(component.form.controls.agenceId.value).toBe(1);
    expect(component.form.controls.vehiculeId.value).toBe(10);
    expect(component.form.controls.date.value).toBe(component.todayDate());
    expect(vehiculesApiMock.getByAgence).toHaveBeenCalledWith(1);
  });

  it('préselectionne le type PRIVE si le query param type=PRIVE est fourni', () => {
    activatedRouteMock.snapshot.queryParamMap.get.mockReturnValue('PRIVE');
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;

    expect(component.form.controls.typeTrajet.value).toBe('PRIVE');
  });

  it('bloque la soumission si la date est au delà de la dernière date autorisée', () => {
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;
    const tooLateDate = new Date(component.maxBookingDate());
    tooLateDate.setDate(tooLateDate.getDate() + 1);

    component.form.patchValue({
      date: formatDate(tooLateDate),
      heureDebut: '09:00',
      typeTrajet: 'PUBLIC'
    });

    component.submit();

    expect(component.form.controls.date.hasError('maxBookingDate')).toBe(true);
    expect(trajetsApiMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('Dernière date autorisée');
    expect(component.errorMessage()).toContain(component.maxBookingDate());
  });

  it('crée un trajet privé et ajoute les passagers invités', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;

    component.form.patchValue({
      date: component.todayDate(),
      heureDebut: '09:00',
      typeTrajet: 'PRIVE',
      passager1: 'g1002',
      passager2: 'l10001'
    });

    component.submit();

    expect(trajetsApiMock.create).toHaveBeenCalledWith({
      vehiculeId: 10,
      trajetDate: `${component.todayDate()}T09:00:00`,
      trajetType: 'PRIVE',
    });
    expect(reservationsApiMock.createPrivateInvites).toHaveBeenCalledWith({
      trajetId: 99,
      inviteeMatricules: ['G1002', 'L10001']
    });
    expect(component.message()).toContain('Les invitations sont en attente de paiement');
    vi.runAllTimers();
    expect(navigateSpy).toHaveBeenCalledWith('/voyageur/reservations');
    vi.useRealTimers();
  });

  it('traduit le message backend de délai en message frontend compréhensible', () => {
    trajetsApiMock.create.mockReturnValue(
      throwError(() => ({ error: { message: 'Le voyageur LOCAL ne peut pas réserver plus de 14 jours à l\'avance.' } }))
    );
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;

    component.form.patchValue({
      date: component.todayDate(),
      heureDebut: '09:00',
      typeTrajet: 'PUBLIC'
    });

    component.submit();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toContain('Le profil LOCAL ne peut pas réserver plus de 14 jours');
    expect(component.errorMessage()).toContain(component.maxBookingDate());
  });
  it('affiche une erreur métier sans code HTTP technique', () => {
    trajetsApiMock.create.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: {
          status: 400,
          message: 'Vous avez une restriction active et ne pouvez pas créer de trajet.'
        }
      }))
    );
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;

    component.form.patchValue({
      date: component.todayDate(),
      heureDebut: '09:00',
      typeTrajet: 'PUBLIC'
    });

    component.submit();

    expect(component.errorMessage()).toContain('Vous avez une restriction active');
    expect(component.errorMessage()).not.toContain('code 400');
    expect(component.errorMessage()).not.toContain('Erreur :');
  });
});

import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
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
    peutProposerTrajet: true,
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
    { id: 10, nom: 'Vehicule A', agenceId: 1, agenceNom: 'Agence Lyon', disponible: true }
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
  let trajetsApiMock: { getVilles: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn> };
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
      getVilles: vi.fn().mockReturnValue(of([{code:'MONS',libelle:'Mons'},{code:'BRUXELLES',libelle:'Bruxelles'}])),
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
        { provide: VoyageursApiService, useValue: {getAll: vi.fn().mockReturnValue(of([]))} },
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
    component.form.patchValue({villeDepart:'MONS',villeArrivee:'BRUXELLES'});

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
    component.form.patchValue({villeDepart:'MONS',villeArrivee:'BRUXELLES'});

    expect(component.form.controls.typeTrajet.value).toBe('PRIVE');
  });

  it('demande un choix explicite parmi plusieurs véhicules disponibles de cette agence', () => {
    vehiculesApiMock.getByAgence.mockReturnValue(of([
      ...vehicules, {...vehicules[0], id:11}, {...vehicules[0], id:12, disponible:false},
      {...vehicules[0], id:13, agenceId:2}
    ]));
    const component = TestBed.createComponent(VoyageurCreateTrajetPage).componentInstance;
    expect(component.vehicules().map(v=>v.id)).toEqual([10,11]);
    expect(component.form.controls.vehiculeId.enabled).toBe(true);
    expect(component.form.controls.vehiculeId.value).toBeNull();
  });

  it('garde le choix désactivé lorsqu’aucun véhicule n’est disponible', () => {
    vehiculesApiMock.getByAgence.mockReturnValue(of([{...vehicules[0], disponible:false}]));
    const component = TestBed.createComponent(VoyageurCreateTrajetPage).componentInstance;
    expect(component.vehicules()).toEqual([]);
    expect(component.form.controls.vehiculeId.disabled).toBe(true);
    expect(component.loadingVehicules()).toBe(false);
  });

  it('annule le chargement précédent quand cette agence change', () => {
    const first = new Subject<VehiculeResponse[]>();
    const second = new Subject<VehiculeResponse[]>();
    vehiculesApiMock.getByAgence.mockReturnValueOnce(first).mockReturnValueOnce(second);
    const component = TestBed.createComponent(VoyageurCreateTrajetPage).componentInstance;
    expect(component.loadingVehicules()).toBe(true);
    component.form.controls.agenceId.setValue(2);
    component.onAgenceChange(2);
    first.next(vehicules);
    expect(component.vehicules()).toEqual([]);
    expect(component.form.controls.vehiculeId.disabled).toBe(true);
    second.next([{...vehicules[0], id:20, agenceId:2}]);
    expect(component.form.controls.vehiculeId.value).toBe(20);
    expect(component.loadingVehicules()).toBe(false);
  });

  it('permet de recharger les véhicules après une erreur réseau', () => {
    vehiculesApiMock.getByAgence.mockReturnValueOnce(throwError(()=>({status:0}))).mockReturnValueOnce(of(vehicules));
    const component = TestBed.createComponent(VoyageurCreateTrajetPage).componentInstance;
    expect(component.vehicleError()).not.toBe('');
    expect(component.form.controls.vehiculeId.disabled).toBe(true);
    component.onAgenceChange(1);
    expect(component.vehicleError()).toBe('');
    expect(component.form.controls.vehiculeId.value).toBe(10);
  });

  it('refuse un départ et une destination identiques', () => {
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;
    component.form.patchValue({villeDepart:'MONS',villeArrivee:'MONS',date:component.todayDate(),heureDebut:'10:00'});
    component.submit();
    expect(trajetsApiMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('deux villes différentes');
  });

  it('crée un trajet privé et ajoute les passagers invités', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;
    component.form.patchValue({villeDepart:'MONS',villeArrivee:'BRUXELLES'});

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
      villeDepart:'MONS', villeArrivee:'BRUXELLES', publicationAutomatiqueAutorisee:false,
    });
    expect(reservationsApiMock.createPrivateInvites).toHaveBeenCalledWith({
      trajetId: 99,
      inviteeMatricules: ['G1002', 'L10001']
    });
    expect(component.message()).toContain('Les invitations sont en attente de paiement');
    vi.runAllTimers();
    expect(navigateSpy).toHaveBeenCalledWith('/voyageur/profile');
    vi.useRealTimers();
  });

  it('refuse une création si le compte ne peut pas proposer de trajet', () => {
    voyageurSessionMock.voyageur.mockReturnValue({...voyageur,peutProposerTrajet:false});
    const fixture = TestBed.createComponent(VoyageurCreateTrajetPage);
    const component = fixture.componentInstance;
    component.form.patchValue({villeDepart:'MONS',villeArrivee:'BRUXELLES',date:component.todayDate(),heureDebut:'10:00'});
    component.submit();
    expect(trajetsApiMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain("n'est pas autorisé");
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
    component.form.patchValue({villeDepart:'MONS',villeArrivee:'BRUXELLES'});

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

import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { VoyageurResponse } from '../../../shared/models/voyageur.model';
import { AgenceResponse } from '../../../shared/models/agence-vehicule.model';
import { AdminVoyageursPage } from './admin-voyageurs-page.component';

describe('AdminVoyageursPage', () => {
  const voyageurs: VoyageurResponse[] = [
	{ id: 1, matricule: 'G1001', nom: 'Martin', prenom: 'Lucas', email: 'lucas@email.com', typeVoyageur: 'VIP', agenceId: null, agenceNom: null, solde: 0 },
	{ id: 2, matricule: 'S10001', nom: 'Bernard', prenom: 'Tom', email: 'tom@email.com', typeVoyageur: 'LOCAL', agenceId: 1, agenceNom: 'Agence Lyon', solde: 5 },
	{ id: 3, matricule: 'S10002', nom: 'Leroy', prenom: 'Sarah', email: 'sarah@email.com', typeVoyageur: 'LOCAL', agenceId: 2, agenceNom: 'Agence Paris', solde: 0 }
  ];

  const agences: AgenceResponse[] = [
	{ id: 1, nom: 'Agence Lyon', adresse: 'Lyon', heureOuverture: '08:00', heureFermeture: '22:00', dureeTrajetMinutes: 90, dureeEntreTrajetMinutes: 15, anneeCivile: 2026 },
	{ id: 2, nom: 'Agence Paris', adresse: 'Paris', heureOuverture: '07:00', heureFermeture: '23:00', dureeTrajetMinutes: 90, dureeEntreTrajetMinutes: 15, anneeCivile: 2026 }
  ];

  let voyageursApiMock: {
	getAll: ReturnType<typeof vi.fn>;
	create: ReturnType<typeof vi.fn>;
	update: ReturnType<typeof vi.fn>;
	delete: ReturnType<typeof vi.fn>;
	hasRestriction: ReturnType<typeof vi.fn>;
	getRestrictions: ReturnType<typeof vi.fn>;
  };
  let agencesApiMock: { getAll: ReturnType<typeof vi.fn> };
  let adminSessionMock: {
	isGlobalAdmin: ReturnType<typeof vi.fn>;
	isAgenceAdmin: ReturnType<typeof vi.fn>;
	agenceId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
	voyageursApiMock = {
	  getAll: vi.fn().mockReturnValue(of(voyageurs)),
	  create: vi.fn().mockReturnValue(of(voyageurs[0])),
	  update: vi.fn().mockReturnValue(of(voyageurs[1])),
	  delete: vi.fn().mockReturnValue(of(void 0)),
	  hasRestriction: vi.fn((id: number) => of(id === 2)),
	  getRestrictions: vi.fn().mockReturnValue(of([]))
	};
	agencesApiMock = {
	  getAll: vi.fn().mockReturnValue(of(agences))
	};
	adminSessionMock = {
	  isGlobalAdmin: vi.fn().mockReturnValue(false),
	  isAgenceAdmin: vi.fn().mockReturnValue(true),
	  agenceId: vi.fn().mockReturnValue(1)
	};

	await TestBed.configureTestingModule({
	  imports: [AdminVoyageursPage],
	  providers: [
		provideRouter([]),
		provideNoopAnimations(),
		{ provide: VoyageursApiService, useValue: voyageursApiMock },
		{ provide: AgencesApiService, useValue: agencesApiMock },
		{ provide: AdminSessionService, useValue: adminSessionMock }
	  ]
	}).compileComponents();
  });

  afterEach(() => {
	vi.restoreAllMocks();
	TestBed.resetTestingModule();
  });

  it('charge uniquement les voyageurs et agences autorises pour un admin de agence', () => {
	const fixture = TestBed.createComponent(AdminVoyageursPage);
	const component = fixture.componentInstance;

	expect(component.filteredVoyageurs()).toHaveLength(2);
	expect(component.filteredVoyageurs().map((m) => m.id)).toEqual([1, 2]);
	expect(component.agences()).toHaveLength(1);
	expect(component.agences()[0].id).toBe(1);
  });

  it('applique la recherche et les filtres par type', () => {
	adminSessionMock.isGlobalAdmin.mockReturnValue(true);
	adminSessionMock.isAgenceAdmin.mockReturnValue(false);
	adminSessionMock.agenceId.mockReturnValue(null);
	const fixture = TestBed.createComponent(AdminVoyageursPage);
	const component = fixture.componentInstance;

	component.searchQuery.set('sarah');
	expect(component.displayedVoyageurs()).toHaveLength(1);
	expect(component.displayedVoyageurs()[0].matricule).toBe('S10002');

	component.searchQuery.set('');
	component.typeFilter.set('LOCAL');
	expect(component.displayedVoyageurs()).toHaveLength(2);
	expect(component.countByType('LOCAL')).toBe(2);
  });

  it('affiche le statut de restriction active d un voyageur', () => {
	const fixture = TestBed.createComponent(AdminVoyageursPage);
	const component = fixture.componentInstance;
	fixture.detectChanges();

	expect(voyageursApiMock.hasRestriction).toHaveBeenCalledWith(2);
	expect(component.hasActiveRestriction(2)).toBe(true);
	expect(fixture.nativeElement.textContent).toContain('Pénalité active');
  });

  it('passe en mode édition puis sauvegarde la mise à jour du voyageur', () => {
	const scrollSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);
	const fixture = TestBed.createComponent(AdminVoyageursPage);
	const component = fixture.componentInstance;

	component.edit(voyageurs[1]);

	expect(component.editingId()).toBe(2);
	expect(component.form.controls.matricule.value).toBe('S10001');
	expect(scrollSpy).toHaveBeenCalled();

	component.form.controls.nom.setValue('Bernard-Modifie');
	component.save();

	expect(voyageursApiMock.update).toHaveBeenCalledWith(
	  2,
	  expect.objectContaining({ matricule: 'S10001', nom: 'Bernard-Modifie', typeVoyageur: 'LOCAL', agenceId: 1 })
	);
	expect(component.message()).toContain('Voyageur mis à jour');
	expect(component.editingId()).toBeNull();
  });

  it('supprime un voyageur après confirmation', () => {
	vi.spyOn(window, 'confirm').mockReturnValue(true);
	const fixture = TestBed.createComponent(AdminVoyageursPage);
	const component = fixture.componentInstance;

	component.remove(2);

	expect(voyageursApiMock.delete).toHaveBeenCalledWith(2);
	expect(component.message()).toContain('Voyageur supprimé');
  });

  it('affiche les restrictions actives et historiques sans les supprimer', () => {
    const historique = [{ id: 9, motif: 'Absence vérifiée', sourceEvenement: 'absence:1', dateDebut: '2026-11-01', dateFin: '2026-11-08', active: false }];
    voyageursApiMock.getRestrictions = vi.fn().mockReturnValue(of(historique));
    const fixture = TestBed.createComponent(AdminVoyageursPage);
    fixture.componentInstance.voirRestrictions(1);
    expect(voyageursApiMock.getRestrictions).toHaveBeenCalledWith(1);
    expect(fixture.componentInstance.historiqueRestrictions()).toEqual(historique);
    expect(fixture.componentInstance.historiqueVoyageurId()).toBe(1);
  });
});

import { StatutTrajet, TypeTrajet } from './enums.model';

export interface TrajetRequest {
  vehiculeId: number;
  organisateurId: number;
  date: string;
  heureDebut: string;
  typeTrajet: TypeTrajet;
}

export interface TrajetResponse {
  villeDepart?: string | null;
  villeArrivee?: string | null;
  chauffeurId?: number;
  chauffeurNom?: string;
  placesRestantes?: number;
  placesMaximum?: number;
  agenceId?: number;
  id: number;
  vehiculeId: number;
  vehiculeNom: string;
  agenceNom: string;
  organisateurId: number;
  organisateurNom: string;
  date: string;
  heureDebut: string;
  heureFin: string;
  typeTrajet: TypeTrajet;
  statut: StatutTrajet;
  nbPassagersActuels: number;
  prixParPassager: number;
  dateConversionPublic: string | null;
}

export interface CreateTrajetRequest {
  villeDepart: string;
  villeArrivee: string;
  publicationAutomatiqueAutorisee: boolean;
  chauffeurId?: number;
  vehiculeId: number;
  trajetDate: string;
  trajetType: TypeTrajet;
}

export interface VilleOption { code: string; libelle: string; }
export interface RechercheTrajet { depart?: string; arrivee?: string; date?: string; }

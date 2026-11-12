import { TypeVoyageur } from './enums.model';

export interface LoginRequest {
  matricule: string;
  password: string; // Added password field
}

export interface VoyageurRequest {
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeVoyageur: TypeVoyageur;
  agenceId?: number;
  password?: string;
}

export interface VoyageurResponse {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeVoyageur: TypeVoyageur;
  agenceId: number | null;
  agenceNom: string | null;
  solde: number;
  token?: string;
}

export interface LoginResponse extends VoyageurResponse {
  token: string;
}

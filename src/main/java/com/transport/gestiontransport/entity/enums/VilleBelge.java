package com.transport.gestiontransport.entity.enums;

import java.util.Arrays;
import java.util.List;

public enum VilleBelge {
    BRUXELLES("Bruxelles"), MONS("Mons"), CHARLEROI("Charleroi"), NAMUR("Namur"),
    LIEGE("Liège"), ANVERS("Anvers"), GAND("Gand"), LOUVAIN("Louvain"),
    NIVELLES("Nivelles"), WAVRE("Wavre");

    private final String libelle;

    VilleBelge(String libelle) { this.libelle = libelle; }
    public String getLibelle() { return libelle; }
    public record Option(String code, String libelle) {}
    public static List<Option> options() {
        return Arrays.stream(values()).map(ville -> new Option(ville.name(), ville.libelle)).toList();
    }
}

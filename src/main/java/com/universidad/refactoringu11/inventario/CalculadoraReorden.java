package com.universidad.refactoringu11.inventario;

/**
 * Colaborador especializado en la política de cálculo de reorden de inventario (SRP).
 */
public class CalculadoraReorden {

    public int calcularCantidadReorden(int nuevoStock, int stockMinimo) {
        if (nuevoStock < stockMinimo) {
            return (stockMinimo * 3) - nuevoStock;
        }
        return 0;
    }
}

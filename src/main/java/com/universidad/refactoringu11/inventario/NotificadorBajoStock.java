package com.universidad.refactoringu11.inventario;

/**
 * Puerto de notificación de bajo stock (Principio de Inversión de Dependencias).
 * Pertenece al paquete de mayor nivel de abstracción (inventario / dominio).
 */
public interface NotificadorBajoStock {
    void notificar(Producto producto, int cantidadReorden);
}

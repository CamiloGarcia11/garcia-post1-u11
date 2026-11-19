package com.universidad.refactoringu11.inventario;

public class ClasificadorProducto {

    /**
     * Calcula el recargo de envío delegando polimórficamente en TipoProducto.
     * Se elimina por completo el switch statement duplicado.
     */
    public double calcularRecargoEnvio(Producto producto) {
        if (producto == null || producto.getTipo() == null) {
            throw new IllegalArgumentException("El producto y su tipo no pueden ser nulos");
        }
        return producto.getTipo().calcularRecargoEnvio(producto.getPrecio());
    }
}

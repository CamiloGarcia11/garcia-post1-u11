package com.universidad.refactoringu11.inventario;

public class EtiquetadoProducto {

    /**
     * Genera la etiqueta del producto delegando polimórficamente en TipoProducto.
     * Se elimina por completo el switch statement duplicado.
     */
    public String generarEtiqueta(Producto producto) {
        if (producto == null || producto.getTipo() == null) {
            throw new IllegalArgumentException("El producto y su tipo no pueden ser nulos");
        }
        return producto.getTipo().generarEtiqueta(producto.getNombre());
    }
}

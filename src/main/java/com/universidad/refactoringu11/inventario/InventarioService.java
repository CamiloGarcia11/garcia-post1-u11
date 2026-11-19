package com.universidad.refactoringu11.inventario;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InventarioService {

    private final List<Producto> productos = new ArrayList<>();
    private final List<String> movimientos = new ArrayList<>();
    private final CalculadoraReorden calculadoraReorden = new CalculadoraReorden();

    public void agregarProducto(Producto producto) {
        productos.add(producto);
    }

    /**
     * Actualiza el inventario aplicando métodos de composición (Extract Method / Extract Class).
     * Orquestación limpia de menos de 10 líneas con responsabilidades delegadas.
     */
    public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
        if (esEntradaInvalida(productoId, cantidad, tipoMovimiento)) return "ERROR_DATOS_INVALIDOS";

        Producto producto = buscarProductoPorId(productoId);
        if (producto == null) return "ERROR_PRODUCTO_NO_EXISTE";

        int nuevoStock = calcularNuevoStock(producto, cantidad, tipoMovimiento);
        if (nuevoStock < 0) return "ERROR_STOCK_INSUFICIENTE";

        producto.setStock(nuevoStock);
        registrarMovimiento(tipoMovimiento, cantidad, producto, nuevoStock);

        int cantidadReorden = calculadoraReorden.calcularCantidadReorden(nuevoStock, producto.getStockMinimo());
        return resolverResultadoMovimiento(producto, nuevoStock, cantidadReorden);
    }

    private boolean esEntradaInvalida(Long productoId, int cantidad, String tipoMovimiento) {
        return productoId == null || cantidad <= 0 ||
                (!tipoMovimiento.equals("ENTRADA") && !tipoMovimiento.equals("SALIDA"));
    }

    private Producto buscarProductoPorId(Long productoId) {
        return productos.stream()
                .filter(p -> p.getId().equals(productoId))
                .findFirst()
                .orElse(null);
    }

    private int calcularNuevoStock(Producto producto, int cantidad, String tipoMovimiento) {
        return tipoMovimiento.equals("ENTRADA")
                ? producto.getStock() + cantidad
                : producto.getStock() - cantidad;
    }

    private void registrarMovimiento(String tipoMovimiento, int cantidad, Producto producto, int nuevoStock) {
        String registro = tipoMovimiento + " de " + cantidad + " unidades de " + producto.getNombre() + " -> stock final: " + nuevoStock;
        movimientos.add(registro);
        System.out.println(registro);
    }

    private String resolverResultadoMovimiento(Producto producto, int nuevoStock, int cantidadReorden) {
        if (cantidadReorden > 0) {
            System.out.println("ALERTA: " + producto.getNombre() + " bajo stock minimo. Reordenar " + cantidadReorden + " unidades.");
            return "OK_BAJO_STOCK_REORDEN_" + cantidadReorden;
        }
        return "OK_" + nuevoStock;
    }
}

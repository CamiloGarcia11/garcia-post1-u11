package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.NotificadorBajoStock;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteBajoStockService implements NotificadorBajoStock {

    private final InventarioService inventarioService;
    private final List<String> alertasRecibidas = new ArrayList<>();

    public ReporteBajoStockService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @Override
    public void notificar(Producto producto, int cantidadReorden) {
        String alerta = "ALERTA REPORTE: " + producto.getNombre() + " requiere reorden de " + cantidadReorden + " unidades.";
        alertasRecibidas.add(alerta);
        System.out.println(alerta);
    }

    public String generarReporte() {
        return "Reporte de bajo stock: " + alertasRecibidas.size() + " alertas registradas.";
    }

    public List<String> getAlertasRecibidas() {
        return alertasRecibidas;
    }
}

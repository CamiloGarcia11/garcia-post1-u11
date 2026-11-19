# Refactorización Guiada por Pruebas: Code Smells, Paquetes y Acoplamiento (`refactoring-u11`)

**Estudiante:** Edwin Camilo García  
**Materia:** Patrones de Diseño de Software  
**Unidad 11:** Refactorización Guiada por Pruebas: Code Smells, Paquetes y Acoplamiento en un Proyecto Java Real  
**Repositorio GitHub:** [https://github.com/CamiloGarcia11/garcia-post1-u11](https://github.com/CamiloGarcia11/garcia-post1-u11)  

---

## Parte 1: Red de Seguridad y Refactorización Guiada por Evidencia

### 1.1 Diagnóstico de Code Smells Iniciales

1. **Long Method (Bloater) en `InventarioService.actualizarInventario()`:**
   - **Antes:** Un único método de más de 38 líneas acumulaba 5 responsabilidades divergentes: validación de parámetros de entrada, búsqueda y filtrado de producto en colección, cálculo y mutación de stock, cálculo de cantidad de reorden ante bajo stock, y auditoría en consola/lista de movimientos con toma de decisiones de notificación.
   - **Riesgo:** Alta complejidad ciclomática, violación del Principio de Responsabilidad Única (SRP), y alta fragilidad frente a cambios en políticas de abastecimiento.
2. **Switch Statements Duplicados (Object-Orientation Abusers) en `ClasificadorProducto` y `EtiquetadoProducto`:**
   - **Antes:** Ambas clases contenían una estructura condicional idéntica `switch (producto.getTipo())` con cuatro ramas (`LIBRO`, `ELECTRONICO`, `ROPA`, `ALIMENTO`).
   - **Riesgo:** Violación del Principio Abierto/Cerrado (OCP) y del principio DRY. La adición de un nuevo tipo de producto (e.g., `FARMACIA`) exigía localizar y modificar simultáneamente ambos archivos en el código fuente.

### 1.2 Red de Seguridad (Pruebas de Caracterización)
Antes de modificar cualquier línea de lógica, se construyó una suite de pruebas de caracterización sin levantar el contexto de Spring (instanciación directa rápida POJO):
- `InventarioServiceTest`: Casos de entrada de stock, salida normal, salida con stock insuficiente, salida que cruza el umbral de stock mínimo activando reorden con cálculo exacto, movimiento no soportado y producto inexistente.
- `ClasificadorProductoTest`: Verificación de cálculo de recargos para cada tipo de producto.
- `EtiquetadoProductoTest`: Verificación de generación de etiquetas especializadas por tipo.

Todas las pruebas se ejecutaron en verde previo a las intervenciones, permaneciendo intactas durante todo el proceso.

---

### 1.3 Decisiones de Diseño (Parte 1)

#### Decisión de diseño 1: Descomposición de `actualizarInventario()` y extracción de reorden
- **Elección:** Se extrajo la lógica de cálculo de reorden en una clase colaboradora independiente: [`CalculadoraReorden.java`](src/main/java/com/universidad/refactoringu11/inventario/CalculadoraReorden.java).
- **Justificación:**  
  La fórmula de reorden (`(stockMinimo * 3) - nuevoStock`) no es un mero detalle de paso aritmético, sino una **política de aprovisionamiento de inventario** sujeta a cambios por requerimientos de la cadena de suministro (stocks de seguridad dinámicos, estacionalidad o factores de tiempo de entrega). Extraerla como colaborador independiente separa el ciclo de vida de la regla logística de la orquestación transaccional del servicio (`InventarioService`), permitiendo probar y evolucionar la política sin modificar la lógica de persistencia y movimientos.
- **Resultado en `actualizarInventario()`:** Reducción de 38 líneas a **9 líneas de orquestación clara**, delegando en métodos privados autoexplicativos (`esEntradaInvalida`, `buscarProductoPorId`, `calcularNuevoStock`, `registrarMovimiento`, `resolverResultadoMovimiento`).

#### Decisión de diseño 2: Mecanismo de polimorfismo para eliminar Switch Statements duplicados
- **Elección:** Se aplicó **Enum Polimórfico** (*Replace Conditional with Polymorphism*) sobre [`TipoProducto.java`](src/main/java/com/universidad/refactoringu11/inventario/TipoProducto.java) mediante métodos abstractos `calcularRecargoEnvio(double precio)` y `generarEtiqueta(String nombre)`.
- **Justificación:**  
  En el dominio modelado, el conjunto de tipos de producto es cerrado y cohesivo, y el cálculo de recargos o etiquetas no depende de servicios o repositorios inyectados por Spring. Crear una jerarquía externa de clases (`LibroStrategy`, `RopaStrategy`, etc.) habría introducido una sobrecarga innecesaria de clases y complejidad accidental (*Class Explosion*). El enum polimórfico mantiene la encapsulación en el propio tipo, elimina al 100% las estructuras switch en `ClasificadorProducto` y `EtiquetadoProducto`, y respeta el Principio Abierto/Cerrado.

---

### 1.4 Evidencia de Refactorización y Pruebas (Parte 1)

| Métrica / Elemento | Antes del Refactor | Después del Refactor |
|---|---|---|
| **Líneas de `actualizarInventario()`** | 38 líneas | **9 líneas** (-76%) |
| **Responsabilidades en `InventarioService`** | 5 mezcladas | 1 orquestadora delegada |
| **Switch Statements sobre `TipoProducto`** | 2 duplicados (8 ramas) | **0** (100% polimórfico) |
| **Pruebas de caracterización** | 12 pruebas ejecutadas | **12 pruebas PASSED** (0 fallos) |

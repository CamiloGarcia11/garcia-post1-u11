# Refactorización Guiada por Pruebas: Code Smells, Paquetes y Acoplamiento (`refactoring-u11`)

[![CI](https://github.com/CamiloGarcia11/garcia-post1-u11/actions/workflows/ci.yml/badge.svg)](https://github.com/CamiloGarcia11/garcia-post1-u11/actions/workflows/ci.yml)

**Estudiante:** Edwin Camilo García  
**Materia:** Patrones de Diseño de Software  
**Unidad 11:** Refactorización Guiada por Pruebas: Code Smells, Paquetes y Acoplamiento en un Proyecto Java Real  
**Repositorio GitHub:** [https://github.com/CamiloGarcia11/garcia-post1-u11](https://github.com/CamiloGarcia11/garcia-post1-u11)  

---

## 1. Parte 1 — Red de Seguridad y Refactorización Guiada por Evidencia

### 1.1 Diagnóstico de Code Smells Iniciales

1. **Long Method (Bloater) en `InventarioService.actualizarInventario()`:**
   - **Estado Original:** Un método de más de 38 líneas acumulaba cinco responsabilidades distintas: validación de parámetros de entrada, búsqueda secuencial de productos, cálculo y mutación de stock, cálculo de cantidad de reorden ante bajo stock, y auditoría en consola/lista de movimientos con toma de decisiones de notificación.
   - **Impacto:** Alta complejidad ciclomática, violación del Principio de Responsabilidad Única (SRP), y fragilidad ante cambios.
2. **Switch Statements Duplicados (Object-Orientation Abusers) en `ClasificadorProducto` y `EtiquetadoProducto`:**
   - **Estado Original:** Ambas clases contenían una estructura condicional idéntica `switch (producto.getTipo())` con cuatro ramas (`LIBRO`, `ELECTRONICO`, `ROPA`, `ALIMENTO`), variando únicamente el valor devuelto.
   - **Impacto:** Violación del Principio Abierto/Cerrado (OCP) y del principio DRY. Añadir un nuevo tipo de producto requería modificar ambas clases en paralelo.

### 1.2 Red de Seguridad (Pruebas de Caracterización)
Antes de modificar una sola línea de código, se escribió una red de seguridad completa con JUnit 5 (instanciación directa sin arranque de contexto Spring para máxima velocidad):
- **`InventarioServiceTest`:** Valida entrada de stock, salida normal, salida con stock insuficiente, salida que cruza el umbral de stock mínimo activando reorden con cálculo exacto, movimiento no soportado y producto inexistente.
- **`ClasificadorProductoTest`:** Valida cálculo de recargo para cada tipo de producto.
- **`EtiquetadoProductoTest`:** Valida generación de etiquetas especializadas por tipo.

Todas las pruebas se ejecutaron en verde sobre el código original y permanecieron invariantes durante y después de la refactorización.

---

### 1.3 Decisiones de Diseño (Parte 1)

#### Decisión de diseño 1: Descomposición de `actualizarInventario()` y extracción de reorden
- **Elección:** Se extrajo la lógica de cálculo de reorden en una clase colaboradora independiente: [`CalculadoraReorden.java`](src/main/java/com/universidad/refactoringu11/inventario/CalculadoraReorden.java).
- **Justificación:**  
  La fórmula de reorden (`(stockMinimo * 3) - nuevoStock`) representa una **política de aprovisionamiento de inventario** (cobertura triple de buffer de seguridad) sujeta a evolución por requerimientos de la cadena de suministro (estacionalidad, lead-time de proveedores o criticidad del SKU). Aislarla en `CalculadoraReorden` cumple con el Principio de Responsabilidad Única (SRP), permitiendo probarla y modificarla de forma aislada sin comprometer la orquestación transaccional de movimientos en `InventarioService`.
- **Resultado en `actualizarInventario()`:** El método quedó reducido a **9 líneas de orquestación limpia** delegando en métodos privados autoexplicativos (`esEntradaInvalida`, `buscarProductoPorId`, `calcularNuevoStock`, `registrarMovimiento`, `resolverResultadoMovimiento`).

#### Decisión de diseño 2: Mecanismo de polimorfismo para eliminar Switch Statements duplicados
- **Elección:** Se aplicó **Enum Polimórfico** (*Replace Conditional with Polymorphism*) sobre [`TipoProducto.java`](src/main/java/com/universidad/refactoringu11/inventario/TipoProducto.java) mediante métodos abstractos `calcularRecargoEnvio(double precio)` y `generarEtiqueta(String nombre)`.
- **Justificación:**  
  El conjunto de tipos de producto es cerrado y cohesivo en este dominio, y sus cálculos no requieren colaboradores inyectados por Spring. Crear una jerarquía de clases externa (`LibroStrategy`, `RopaStrategy`, etc.) habría provocado una explosión innecesaria de clases (*Class Explosion*). El enum polimórfico elimina al 100% las estructuras switch en `ClasificadorProducto` y `EtiquetadoProducto`, manteniendo sus firmas públicas originales y garantizando el Principio Abierto/Cerrado.

---

### 1.4 Evidencia de Refactorización y Pruebas (Parte 1)

| Métrica / Elemento | Antes del Refactor | Después del Refactor |
|---|---|---|
| **Líneas de `actualizarInventario()`** | 38 líneas | **9 líneas** (-76% de reducción) |
| **Responsabilidades en `InventarioService`** | 5 mezcladas | 1 orquestadora delegada |
| **Switch Statements sobre `TipoProducto`** | 2 duplicados (8 ramas) | **0** (100% polimórfico) |
| **Pruebas de caracterización ejecutadas** | 12 pruebas | **12 pruebas PASSED** (0 fallos) |

---

## 2. Parte 2 — Diagnóstico y Resolución de Acoplamiento entre Paquetes

### 2.1 Diagnóstico de Dependencia Cíclica (ArchUnit en Rojo)
Al agregar el módulo de reportes (`ReporteBajoStockService`) para generar alertas de bajo stock, se introdujo una **dependencia cíclica bidireccional entre paquetes**:

#### Diagrama de Paquetes ANTES de la Refactorización (Ciclo Presente):
```text
┌──────────────────────────────────────────────┐
│        paquete: inventario (Dominio)         │
│  - InventarioService                         │
└──────────────────────┬───────────────────────┘
                       │  ▲
   importa             │  │  importa en constructor
   ReporteBajoStock    │  │  InventarioService
                       ▼  │
┌─────────────────────────┴────────────────────┐
│         paquete: reportes (Soporte)          │
│  - ReporteBajoStockService                   │
└──────────────────────────────────────────────┘
```

#### Salida de ArchUnit (`ArquitecturaPaquetesTest`) en Rojo:
```text
[ERROR] Failures: 
[ERROR]   ArquitecturaPaquetesTest.losPaquetesNoDebenTenerDependenciasCiclicas:18 
Architecture Violation [Priority: MEDIUM] - Rule 'slices matching 'com.universidad.refactoringu11.(*)..' should be free of cycles' was violated (1 times):
Cycle detected: Slice inventario -> Slice reportes -> Slice inventario
  Slice inventario depends on Slice reportes:
    Field <com.universidad.refactoringu11.inventario.InventarioService.reporteBajoStockService> has type <com.universidad.refactoringu11.reportes.ReporteBajoStockService>
  Slice reportes depends on Slice inventario:
    Constructor <com.universidad.refactoringu11.reportes.ReporteBajoStockService.<init>(com.universidad.refactoringu11.inventario.InventarioService)> has parameter of type <com.universidad.refactoringu11.inventario.InventarioService>
```

---

### 2.2 Resolución Mediante Inversión de Dependencias (DIP)

Se desacoplaron los paquetes invirtiendo la dirección de la dependencia: el dominio no debe conocer a los módulos de infraestructura o reporte. Se introdujo el puerto/interfaz [`NotificadorBajoStock.java`](src/main/java/com/universidad/refactoringu11/inventario/NotificadorBajoStock.java) en el paquete `inventario`:

#### Diagrama de Paquetes DESPUÉS de la Refactorización (Ciclo Resuelto):
```text
┌──────────────────────────────────────────────┐
│        paquete: inventario (Dominio)         │
│  - InventarioService                         │
│  - <<interface>> NotificadorBajoStock        │
└──────────────────────▲───────────────────────┘
                       │
                       │  implementa NotificadorBajoStock
                       │  e inyecta InventarioService
                       │  (Flujo unidireccional: reportes -> inventario)
                       │
┌──────────────────────┴───────────────────────┐
│         paquete: reportes (Soporte)          │
│  - ReporteBajoStockService                   │
└──────────────────────────────────────────────┘
```

#### Salida de ArchUnit (`ArquitecturaPaquetesTest`) en Verde:
```text
[INFO] Running com.universidad.refactoringu11.ArquitecturaPaquetesTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.382 s -- in com.universidad.refactoringu11.ArquitecturaPaquetesTest
[INFO] BUILD SUCCESS
```

---

### 2.3 Decisiones de Diseño (Parte 2)

#### Decisión de diseño 3: Ubicación de la interfaz `NotificadorBajoStock`
- **Elección:** La interfaz `NotificadorBajoStock` se ubicó en el paquete **`inventario`** (dominio) y no en `reportes`.
- **Justificación:**  
  Según el Principio de Inversión de Dependencias (DIP) y la regla de arquitectura de capas ("la infraestructura depende del dominio; el dominio nunca depende de la infraestructura"), `inventario` es el paquete de mayor nivel de abstracción del sistema. El dominio define el contrato (puerto de salida) que requiere para funcionar sin acoplarse a detalles de entrega. Si `NotificadorBajoStock` se ubicara en `reportes`, `inventario` continuaría dependiendo de dicho paquete para importar la interfaz, perpetuando el ciclo o invirtiendo incorrectamente la jerarquía arquitectónica.

#### Decisión de diseño 4: Granularidad del método de notificación (`notificar` por evento vs lote)
- **Elección:** Se definió una granularidad **por evento individual:** `notificar(Producto producto, int cantidadReorden)`.
- **Justificación:**  
  La notificación por evento individual se acopla de manera natural al flujo transaccional en tiempo real de `actualizarInventario()`, permitiendo reaccionar de forma inmediata ante eventos críticos de desabastecimiento (e.g. disparar una alerta instantánea o webhook sin incurrir en latencia).  
  *¿En qué escenario se preferiría una notificación por lote (`notificar(List<Producto>)`)?*  
  Una alternativa por lote sería superior en procesos de sincronización nocturna (batch processing), generación de órdenes consolidadas de compra a proveedores periódicos, o para optimizar I/O reduciendo el tráfico de red cuando se actualizan masivamente miles de SKUs mediante cargas de archivo.

---

### 2.4 Verificación de Regresión: Pruebas de la Parte 1 Intactas
Tras resolver el ciclo, se ejecutó la suite completa de pruebas:
- `InventarioServiceTest` (6 pruebas) -> **PASSED**
- `ClasificadorProductoTest` (4 pruebas) -> **PASSED**
- `EtiquetadoProductoTest` (4 pruebas) -> **PASSED**
- `ArquitecturaPaquetesTest` (1 prueba) -> **PASSED**

**Total:** 15 pruebas ejecutadas, **0 fallos, 0 errores**. Se comprobó formalmente que la refactorización arquitectónica no alteró en ningún aspecto el comportamiento observable de la aplicación.

---

## 3. Instrucciones de Ejecución

```bash
# Compilar el proyecto y ejecutar la suite completa de pruebas
mvn test

# Ejecutar exclusivamente la prueba de arquitectura de paquetes
mvn test -Dtest=ArquitecturaPaquetesTest

# Ejecutar las pruebas de caracterización de inventario
mvn test -Dtest=InventarioServiceTest,ClasificadorProductoTest,EtiquetadoProductoTest
```

---

## 4. Conclusiones

1. **La disciplina de las pruebas de caracterización:** Blindar el comportamiento observable antes de tocar una sola línea de lógica es la única garantía que permite acometer refactorizaciones agresivas (como reducir un método de 38 a 9 líneas) sin introducir regresiones silenciosas.
2. **Reemplazo metódico de code smells:** La eliminación de Long Method mediante composición y de Switch Statements duplicados mediante enum polimórfico convirtió un diseño monolítico y repetitivo en un sistema modular, legible y extensible que cumple los principios SOLID (SRP y OCP).
3. **El rol insustituible de las pruebas de arquitectura:** Las pruebas unitarias convencionales no tienen visibilidad sobre el acoplamiento estructural ni detectan dependencias cíclicas que degradan la arquitectura en silencio. ArchUnit demostró ser una herramienta esencial para validar que los límites entre paquetes respeten rigurosamente el principio de Inversión de Dependencias.

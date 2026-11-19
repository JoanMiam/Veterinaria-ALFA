package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.Producto;
import prueba.BdPrueba;

/**
 * PRU-002-02 — Pruebas Unitarias para InventarioController.buscarProducto (MR-002).
 *
 * <p>Verifica que el controlador delegue al DAO y propague el resultado sin alterarlo.
 */
@DisplayName("PRU-002-02 | InventarioController — buscarProducto (MR-002)")
class InventarioControllerBusquedaTest {

    private BdPrueba bd;
    private InventarioController controller;

    @BeforeEach
    void setUp() throws SQLException {
        bd = BdPrueba.nueva();
        bd.insertarProducto("Amoxicilina", 50, "L001", BdPrueba.vigente()); // id 1
        bd.insertarProducto("Ibuprofeno", 20, "L003", BdPrueba.mes(9));     // id 2
        controller = new InventarioController(bd.dao);
    }

    @AfterEach
    void tearDown() throws SQLException {
        bd.close();
    }

    @Test
    @DisplayName("TC-01: Búsqueda por ID numérico → el controller retorna el producto del DAO")
    void tc01_busquedaPorId_propagaResultadoDelDao() {
        Producto p = controller.buscarProducto("1").orElseThrow();

        assertEquals(1, p.id());
        assertEquals("Amoxicilina", p.nombre());
    }

    @Test
    @DisplayName("TC-02: Búsqueda por nombre exacto → el controller retorna el producto del DAO")
    void tc02_busquedaPorNombre_propagaResultadoDelDao() {
        assertEquals("Ibuprofeno", controller.buscarProducto("Ibuprofeno").orElseThrow().nombre());
    }

    @Test
    @DisplayName("TC-03: Sin coincidencia → el controller retorna vacío")
    void tc03_sinCoincidencia_vacio() {
        assertTrue(controller.buscarProducto("MedicamentoNoExiste").isEmpty());
    }

    @Test
    @DisplayName("TC-04: El controller no transforma el resultado: es el mismo producto que da el DAO")
    void tc04_resultadoNoEsTransformado() {
        assertEquals(bd.dao.buscarProductoPorIdONombre("Amoxicilina"), controller.buscarProducto("Amoxicilina"));
    }

    @Test
    @DisplayName("TC-05: Nombre parcial → vacío (el controller no añade coincidencia parcial)")
    void tc05_nombreParcial_vacio() {
        assertTrue(controller.buscarProducto("Amox").isEmpty());
    }
}

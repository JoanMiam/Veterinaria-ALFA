package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import prueba.BdPrueba;

/**
 * PRU-002-01 — Pruebas Unitarias para InventarioDAO.buscarProductoPorIdONombre (MR-002).
 *
 * <p>[MR-010] El método devuelve {@code Optional<Producto>} en lugar de {@code Object[]}/null.
 */
@DisplayName("PRU-002-01 | InventarioDAO — buscarProductoPorIdONombre (MR-002)")
class InventarioDAOBusquedaTest {

    private BdPrueba bd;
    private InventarioDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        bd = BdPrueba.nueva();
        dao = bd.dao;
        bd.insertarProducto("Amoxicilina", 50, "L001", BdPrueba.vigente()); // id 1
        bd.insertarProducto("Paracetamol", 30, "L002", BdPrueba.mes(12));   // id 2
    }

    @AfterEach
    void tearDown() throws SQLException {
        bd.close();
    }

    @Test
    @DisplayName("TC-01: ID numérico válido → retorna el producto correcto")
    void tc01_idNumericoValido_retornaProducto() {
        Producto p = dao.buscarProductoPorIdONombre("1").orElseThrow();

        assertEquals(1, p.id());
        assertEquals("Amoxicilina", p.nombre());
    }

    @Test
    @DisplayName("TC-02: Nombre exacto válido → retorna el producto correcto")
    void tc02_nombreExactoValido_retornaProducto() {
        Producto p = dao.buscarProductoPorIdONombre("Paracetamol").orElseThrow();

        assertEquals("Paracetamol", p.nombre());
    }

    @Test
    @DisplayName("TC-03: ID numérico inexistente → vacío")
    void tc03_idNumericoInexistente_vacio() {
        assertTrue(dao.buscarProductoPorIdONombre("9999").isEmpty());
    }

    @Test
    @DisplayName("TC-04: Nombre inexistente → vacío")
    void tc04_nombreInexistente_vacio() {
        assertTrue(dao.buscarProductoPorIdONombre("MedicamentoInexistente").isEmpty());
    }

    @Test
    @DisplayName("TC-05: Nombre parcial ('Amox') → vacío (nombre completo, sin LIKE)")
    void tc05_nombreParcial_vacio() {
        assertTrue(dao.buscarProductoPorIdONombre("Amox").isEmpty(),
                "'Amox' no debe coincidir con 'Amoxicilina'");
    }

    @Test
    @DisplayName("TC-06: Cadena en blanco o nula → vacío")
    void tc06_cadenaEnBlanco_vacio() {
        assertTrue(dao.buscarProductoPorIdONombre("   ").isEmpty());
        assertTrue(dao.buscarProductoPorIdONombre(null).isEmpty());
    }

    @Test
    @DisplayName("TC-07: El resultado trae el producto completo (lote, existencias, caducidad)")
    void tc07_resultado_productoCompleto() {
        Optional<Producto> r = dao.buscarProductoPorIdONombre("Amoxicilina");

        assertTrue(r.isPresent());
        assertEquals("L001", r.get().lote());
        assertEquals(50, r.get().existencias());
        assertEquals(BdPrueba.vigente(), r.get().caducidad());
    }

    @Test
    @DisplayName("TC-08: Entrada '2' (ID de Paracetamol) resuelve por la ruta de ID")
    void tc08_entradaNumerica_usaRutaId() {
        Producto p = dao.buscarProductoPorIdONombre("2").orElseThrow();

        assertEquals(2, p.id());
        assertEquals("Paracetamol", p.nombre());
    }
}

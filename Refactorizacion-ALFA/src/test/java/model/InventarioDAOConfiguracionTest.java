package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controller.InventarioController;
import prueba.BdPrueba;

/**
 * PRU-005-01 — guardarConfiguracion, obtenerConfiguracion y exportación HTML
 * (inventario, ventas, apartados) (MR-005).
 *
 * <p>[MR-010] La configuración viaja como {@link ConfiguracionVeterinaria} y la
 * exportación la hace el controlador con {@link ReportesHtml}.
 */
@DisplayName("PRU-005-01 | InventarioDAO — configuracion y exportación HTML (MR-005)")
class InventarioDAOConfiguracionTest {

    private BdPrueba bd;
    private InventarioDAO dao;
    private InventarioController controller;

    @BeforeEach
    void setUp() throws SQLException {
        bd = BdPrueba.nueva();
        dao = bd.dao;
        controller = new InventarioController(dao);
    }

    @AfterEach
    void tearDown() throws SQLException {
        bd.close();
    }

    private static ConfiguracionVeterinaria config(String nombre, String direccion, String tel, String rfc, String hor) {
        return new ConfiguracionVeterinaria(null, nombre, direccion, tel, rfc, hor);
    }

    private static String leer(File f) throws IOException {
        return Files.readString(f.toPath(), StandardCharsets.UTF_8);
    }

    private static File temporal(String prefijo) throws IOException {
        File tmp = File.createTempFile(prefijo, ".html");
        tmp.deleteOnExit();
        return tmp;
    }

    @Test
    @DisplayName("TC-01: guardarConfiguracion primera inserción crea exactamente un registro con id=1")
    void tc01_guardarConfiguracion_primeraInsercion() throws Exception {
        dao.guardarConfiguracion(config("Vet ALFA", "Calle 1", "9991234567", "ALFA010101AAA", "L-V 9-18"));

        assertEquals(1, bd.contar("configuracion_veterinaria"));
        assertEquals(1, bd.entero("SELECT id FROM configuracion_veterinaria"));
    }

    @Test
    @DisplayName("TC-02: guardarConfiguracion segunda llamada actualiza sin duplicar registros")
    void tc02_guardarConfiguracion_actualizaSinDuplicar() throws Exception {
        dao.guardarConfiguracion(config("Vet ALFA", "Calle 1", "999", "RFC1", "L-V"));
        dao.guardarConfiguracion(config("Vet BETA", "Calle 2", "888", "RFC2", "L-S"));

        assertEquals(1, bd.contar("configuracion_veterinaria"));
        assertEquals("Vet BETA", dao.obtenerConfiguracion().orElseThrow().nombre());
    }

    @Test
    @DisplayName("TC-03: obtenerConfiguracion retorna los valores guardados")
    void tc03_obtenerConfiguracion_conDatos() throws Exception {
        dao.guardarConfiguracion(config("Vet ALFA", "Av. Principal", "9998887766", "ALFA010101BBB", "L-D 8-20"));

        ConfiguracionVeterinaria c = dao.obtenerConfiguracion().orElseThrow();

        assertNull(c.logoBase64(), "logo debe ser null si no se guardó");
        assertFalse(c.tieneLogo());
        assertEquals("Vet ALFA", c.nombre());
        assertEquals("Av. Principal", c.direccion());
        assertEquals("9998887766", c.telefono());
        assertEquals("ALFA010101BBB", c.rfc());
        assertEquals("L-D 8-20", c.horarios());
    }

    @Test
    @DisplayName("TC-04: obtenerConfiguracion retorna vacío cuando no hay configuración; el nombre es obligatorio")
    void tc04_obtenerConfiguracion_sinDatos() {
        assertTrue(dao.obtenerConfiguracion().isEmpty());
        assertThrows(OperacionRechazadaException.class, () -> dao.guardarConfiguracion(config("  ", null, null, null, null)));
        assertTrue(dao.obtenerConfiguracion().isEmpty(), "Un rechazo no debe guardar nada");
    }

    @Test
    @DisplayName("TC-05: exportarInventario genera archivo HTML con datos de productos")
    void tc05_exportarInventario_conProductos() throws Exception {
        bd.insertarProducto("Amoxicilina", 10, BdPrueba.vigente());
        bd.insertarProducto("Paracetamol", 5, BdPrueba.vigente());
        dao.guardarConfiguracion(config("Vet ALFA", null, null, null, null));
        File tmp = temporal("inventario_test_");

        controller.exportarInventario(tmp);

        String html = leer(tmp);
        assertTrue(html.contains("<table"));
        assertTrue(html.contains("Amoxicilina"));
        assertTrue(html.contains("Paracetamol"));
        assertTrue(html.contains("Vet ALFA"), "Debe incluir nombre de la clínica en encabezado");
    }

    @Test
    @DisplayName("TC-06: exportarVentas genera archivo HTML con historial de ventas")
    void tc06_exportarVentas_conVentas() throws Exception {
        int id = bd.insertarProducto("Ibuprofeno", 20, BdPrueba.vigente());
        bd.insertarVenta(id, "Ibuprofeno", 3, "2026-05-22");
        File tmp = temporal("ventas_test_");

        controller.exportarVentas(tmp);

        String html = leer(tmp);
        assertTrue(html.contains("<table"));
        assertTrue(html.contains("Ibuprofeno"));
    }

    @Test
    @DisplayName("TC-07: exportarVentas sin configuración genera encabezado genérico")
    void tc07_exportarVentas_sinConfiguracion() throws Exception {
        int id = bd.insertarProducto("Vitamina C", 15, BdPrueba.vigente());
        bd.insertarVenta(id, "Vitamina C", 1, "2026-05-22");
        File tmp = temporal("ventas_sin_config_");

        controller.exportarVentas(tmp);

        String html = leer(tmp);
        assertTrue(html.contains("<table"));
        assertFalse(html.contains("data:image/png;base64,"), "No debe incluir logo sin configuración");
    }

    @Test
    @DisplayName("TC-08: exportarApartados genera archivo HTML con productos apartados")
    void tc08_exportarApartados_conApartados() throws Exception {
        int id = bd.insertarProducto("Ketamina", 8, BdPrueba.vigente());
        bd.marcarApartado(id, "2026-05-22");
        File tmp = temporal("apartados_test_");

        controller.exportarApartados(tmp);

        String html = leer(tmp);
        assertTrue(html.contains("<table"));
        assertTrue(html.contains("Ketamina"));
        assertTrue(html.contains("2026-05-22"));
    }
}

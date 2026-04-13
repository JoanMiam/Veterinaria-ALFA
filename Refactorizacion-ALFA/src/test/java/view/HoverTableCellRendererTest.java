package view;

import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-001-01 — Pruebas Unitarias para HoverTableCellRenderer (MR-001)
 *
 * Verifica que el toggle de colores de caducidad funciona correctamente
 * a nivel de componente, sin depender de la vista completa.
 */
@DisplayName("PRU-001-01 | HoverTableCellRenderer — Toggle colores caducidad (MR-001)")
class HoverTableCellRendererTest {

    @BeforeAll
    static void configurarModoHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private InventarioView.HoverTableCellRenderer crearRenderer() {
        return new InventarioView.HoverTableCellRenderer();
    }

    private JTable crearTabla(String fechaCaducidad) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Existencias", "Lote", "Caducidad", "Fecha Entrada"}, 0
        );
        model.addRow(new Object[]{1, "Medicamento Test", "10", "L001", fechaCaducidad, "2025-01-01"});
        return new JTable(model);
    }

    private boolean leerCampoMostrarColores(InventarioView.HoverTableCellRenderer renderer)
            throws Exception {
        Field field = InventarioView.HoverTableCellRenderer.class.getDeclaredField("mostrarColores");
        field.setAccessible(true);
        return (boolean) field.get(renderer);
    }

    /** Devuelve una fecha "yyyy-MM" cuyo primer día ya pasó (producto caducado). */
    private String fechaCaducada() {
        return YearMonth.now().minusMonths(2).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /**
     * Devuelve una fecha "yyyy-MM" cuyo primer día está entre 1 y 30 días
     * en el futuro (producto próximo a caducar).
     * Busca en los próximos 3 meses para cubrir bordes de mes.
     */
    private String fechaProximaCaducidad() {
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i <= 3; i++) {
            YearMonth ym = YearMonth.now().plusMonths(i);
            long dias = ChronoUnit.DAYS.between(hoy, ym.atDay(1));
            if (dias >= 1 && dias <= 30) {
                return ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            }
        }
        // Fallback: si no encuentra ninguno, usa el mes siguiente de todos modos
        return YearMonth.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    // -------------------------------------------------------------------------
    // TC-01: setMostrarColores(false) actualiza el campo interno
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-01: setMostrarColores(false) → campo mostrarColores vale false")
    void tc01_setMostrarColoresFalse_campoEsFalse() throws Exception {
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();

        renderer.setMostrarColores(false);

        assertFalse(leerCampoMostrarColores(renderer),
                "Después de setMostrarColores(false), el campo debe ser false");
    }

    // -------------------------------------------------------------------------
    // TC-02: setMostrarColores(true) actualiza el campo interno
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-02: setMostrarColores(true) → campo mostrarColores vale true")
    void tc02_setMostrarColoresTrue_campoEsTrue() throws Exception {
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();
        renderer.setMostrarColores(false); // estado previo: desactivado

        renderer.setMostrarColores(true);

        assertTrue(leerCampoMostrarColores(renderer),
                "Después de setMostrarColores(true), el campo debe ser true");
    }

    // -------------------------------------------------------------------------
    // TC-03: Producto caducado + colores ON → fondo rojo fuerte (255, 0, 0)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-03: Producto caducado + colores ON → fondo (255, 0, 0)")
    void tc03_productoCaducado_coloresActivos_fondoRojoFuerte() {
        String fecha = fechaCaducada();
        JTable tabla = crearTabla(fecha);
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();

        renderer.setMostrarColores(true);
        Component celda = renderer.getTableCellRendererComponent(tabla, fecha, false, false, 0, 4);

        assertEquals(new Color(255, 0, 0), celda.getBackground(),
                "Producto caducado con colores ON debe mostrar rojo fuerte");
    }

    // -------------------------------------------------------------------------
    // TC-04: Producto caducado + colores OFF → sin rojo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-04: Producto caducado + colores OFF → fondo normal (sin rojo)")
    void tc04_productoCaducado_coloresInactivos_sinRojo() {
        String fecha = fechaCaducada();
        JTable tabla = crearTabla(fecha);
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();

        renderer.setMostrarColores(false);
        Component celda = renderer.getTableCellRendererComponent(tabla, fecha, false, false, 0, 4);

        assertNotEquals(new Color(255, 0, 0), celda.getBackground(),
                "Producto caducado con colores OFF no debe mostrar rojo fuerte");
        assertNotEquals(new Color(255, 153, 153), celda.getBackground(),
                "Producto caducado con colores OFF no debe mostrar rojo claro");
    }

    // -------------------------------------------------------------------------
    // TC-05: Producto próximo a caducar + colores ON → fondo rojo claro (255, 153, 153)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-05: Producto próximo a caducar + colores ON → fondo (255, 153, 153)")
    void tc05_productoProximo_coloresActivos_fondoRojoClaro() {
        String fecha = fechaProximaCaducidad();
        JTable tabla = crearTabla(fecha);
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();

        renderer.setMostrarColores(true);
        Component celda = renderer.getTableCellRendererComponent(tabla, fecha, false, false, 0, 4);

        assertEquals(new Color(255, 153, 153), celda.getBackground(),
                "Producto próximo a caducar con colores ON debe mostrar rojo claro");
    }

    // -------------------------------------------------------------------------
    // TC-06: Producto próximo a caducar + colores OFF → sin rojo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-06: Producto próximo a caducar + colores OFF → fondo normal (sin rojo)")
    void tc06_productoProximo_coloresInactivos_sinRojo() {
        String fecha = fechaProximaCaducidad();
        JTable tabla = crearTabla(fecha);
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();

        renderer.setMostrarColores(false);
        Component celda = renderer.getTableCellRendererComponent(tabla, fecha, false, false, 0, 4);

        assertNotEquals(new Color(255, 0, 0), celda.getBackground(),
                "Producto próximo a caducar con colores OFF no debe mostrar rojo fuerte");
        assertNotEquals(new Color(255, 153, 153), celda.getBackground(),
                "Producto próximo a caducar con colores OFF no debe mostrar rojo claro");
    }

    // -------------------------------------------------------------------------
    // TC-07: Estado inicial por defecto → mostrarColores es true
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-07: Estado inicial → mostrarColores es true por defecto")
    void tc07_estadoInicial_mostrarColoresEsTrue() throws Exception {
        InventarioView.HoverTableCellRenderer renderer = crearRenderer();

        assertTrue(leerCampoMostrarColores(renderer),
                "Al crear el renderer, mostrarColores debe ser true (comportamiento original)");
    }
}

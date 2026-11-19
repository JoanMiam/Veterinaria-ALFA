package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import prueba.BdPrueba;

/**
 * PRU-010-01 — Migraciones de esquema con {@code PRAGMA user_version} (MR-010).
 */
@DisplayName("PRU-010-01 | EsquemaBD — migraciones versionadas (MR-010)")
class EsquemaBDTest {

    @Test
    @DisplayName("TC-01: una BD nueva queda en la versión actual con todas las tablas y la columna activo")
    void tc01_bdNueva_versionActual() throws SQLException {
        try (BdPrueba bd = BdPrueba.nueva()) {
            assertEquals(EsquemaBD.VERSION_ACTUAL, EsquemaBD.leerVersion(bd.conexion));
            assertEquals(0, bd.contar("productos"));
            assertEquals(0, bd.contar("historial_ventas"));
            assertEquals(0, bd.contar("configuracion_veterinaria"));
            assertTrue(EsquemaBD.columnaExiste(bd.conexion, "productos", "activo"));
        }
    }

    @Test
    @DisplayName("TC-02: una BD de versiones anteriores (v0) se migra sin perder datos")
    void tc02_bdAnterior_seMigraSinPerderDatos() throws SQLException {
        try (BdPrueba bd = BdPrueba.conEsquemaAnterior("Meloxicam", 12, BdPrueba.vigente())) {
            assertEquals(EsquemaBD.VERSION_ACTUAL, EsquemaBD.leerVersion(bd.conexion));
            assertEquals(1, bd.contar("productos"));
            assertEquals(12, bd.entero("SELECT existencias FROM productos"));
            assertEquals(0, bd.contar("configuracion_veterinaria"), "Se crea la tabla que faltaba");
        }
    }

    @Test
    @DisplayName("TC-03: aplicar las migraciones dos veces no cambia nada (idempotente)")
    void tc03_migracionIdempotente() throws SQLException {
        try (BdPrueba bd = BdPrueba.nueva()) {
            bd.insertarProducto("Med", 3, BdPrueba.vigente());

            EsquemaBD.actualizar(bd.conexion);
            new InventarioDAO(bd.conexion);

            assertEquals(EsquemaBD.VERSION_ACTUAL, EsquemaBD.leerVersion(bd.conexion));
            assertEquals(1, bd.contar("productos"));
        }
    }

    @Test
    @DisplayName("TC-04: una BD de una versión más nueva de la app se rechaza con mensaje claro")
    void tc04_bdMasNueva_seRechaza() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement st = c.createStatement()) {
            st.execute("PRAGMA user_version = " + (EsquemaBD.VERSION_ACTUAL + 1));

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> EsquemaBD.actualizar(c));

            assertTrue(e.getMessage().contains("versión más nueva"), e.getMessage());
        }
    }

    @Test
    @DisplayName("TC-05: si una migración falla, la BD queda como estaba (v0)")
    void tc05_migracionFallida_seDeshace() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement st = c.createStatement()) {
            // Una vista llamada "productos" hace fallar la migración v2 (ALTER TABLE sobre una vista).
            st.execute("CREATE VIEW productos AS SELECT 1 AS id");

            assertThrows(SQLException.class, () -> EsquemaBD.actualizar(c));

            assertEquals(0, EsquemaBD.leerVersion(c));
            try (var rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name = 'historial_ventas'")) {
                rs.next();
                assertEquals(0, rs.getInt(1), "Las tablas creadas por la migración fallida se deshacen");
            }
        }
    }
}

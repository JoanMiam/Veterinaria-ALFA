package view;

import java.awt.GraphicsEnvironment;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-007-01 — Pruebas Unitarias: FormularioConfirmableDialog (MR-007).
 *
 * <p>Verifica que {@link FormularioConfirmableDialog} establece correctamente
 * {@code DO_NOTHING_ON_CLOSE}, registra un {@link java.awt.event.WindowListener},
 * y que {@link FormularioConfirmableDialog#confirmarCierre()} cierra el diálogo
 * directamente cuando {@link FormularioConfirmableDialog#tieneDatosIngresados()}
 * retorna {@code false}. Las pruebas TC-01 a TC-04 requieren entorno gráfico y
 * se omiten automáticamente en modo headless; TC-05 a TC-11 verifican la jerarquía
 * de clases y son compatibles con headless.
 */
@DisplayName("PRU-007-01 | FormularioConfirmableDialog — Cierre seguro (MR-007)")
class FormularioConfirmableDialogTest {

    private JFrame frameParent;

    @BeforeEach
    void setUp() {
        if (!GraphicsEnvironment.isHeadless()) {
            frameParent = new JFrame();
        }
    }

    @AfterEach
    void tearDown() {
        if (frameParent != null) {
            frameParent.dispose();
            frameParent = null;
        }
    }

    // =========================================================================
    // TC-01: Constructor(Frame) establece DO_NOTHING_ON_CLOSE
    // =========================================================================

    @Test
    @DisplayName("TC-01: Constructor(Frame) establece DO_NOTHING_ON_CLOSE")
    void tc01_constructorFrame_estableceDoNothingOnClose() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Requiere entorno gráfico — omitida en modo headless");

        FormularioConfirmableDialog dialog = new FormularioConfirmableDialog(frameParent, "Test", false) {
            @Override
            protected boolean tieneDatosIngresados() { return false; }
        };

        assertEquals(JDialog.DO_NOTHING_ON_CLOSE, dialog.getDefaultCloseOperation(),
                "El constructor(Frame) debe establecer DO_NOTHING_ON_CLOSE");
        dialog.dispose();
    }

    // =========================================================================
    // TC-02: Constructor(Dialog) establece DO_NOTHING_ON_CLOSE
    // =========================================================================

    @Test
    @DisplayName("TC-02: Constructor(Dialog) establece DO_NOTHING_ON_CLOSE")
    void tc02_constructorDialog_estableceDoNothingOnClose() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Requiere entorno gráfico — omitida en modo headless");

        JDialog dialogParent = new JDialog(frameParent);
        FormularioConfirmableDialog dialog = new FormularioConfirmableDialog(dialogParent, "Test", false) {
            @Override
            protected boolean tieneDatosIngresados() { return false; }
        };

        assertEquals(JDialog.DO_NOTHING_ON_CLOSE, dialog.getDefaultCloseOperation(),
                "El constructor(Dialog) debe establecer DO_NOTHING_ON_CLOSE");
        dialog.dispose();
        dialogParent.dispose();
    }

    // =========================================================================
    // TC-03: Constructor registra WindowListener
    // =========================================================================

    @Test
    @DisplayName("TC-03: Constructor registra al menos un WindowListener")
    void tc03_constructor_registraWindowListener() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Requiere entorno gráfico — omitida en modo headless");

        FormularioConfirmableDialog dialog = new FormularioConfirmableDialog(frameParent, "Test", false) {
            @Override
            protected boolean tieneDatosIngresados() { return false; }
        };

        assertTrue(dialog.getWindowListeners().length > 0,
                "El diálogo debe tener al menos un WindowListener registrado tras la construcción");
        dialog.dispose();
    }

    // =========================================================================
    // TC-04: confirmarCierre() sin datos → dispose() directo, sin JOptionPane
    // =========================================================================

    @Test
    @DisplayName("TC-04: confirmarCierre() con tieneDatosIngresados()==false cierra directamente")
    void tc04_confirmarCierre_sinDatos_disposeDirecto() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Requiere entorno gráfico — omitida en modo headless");

        FormularioConfirmableDialog dialog = new FormularioConfirmableDialog(frameParent, "Test", false) {
            @Override
            protected boolean tieneDatosIngresados() { return false; }
        };

        dialog.pack();
        assertTrue(dialog.isDisplayable(),
                "El diálogo debe ser displayable antes de llamar confirmarCierre()");

        dialog.confirmarCierre();

        assertFalse(dialog.isDisplayable(),
                "Con tieneDatosIngresados()==false, confirmarCierre() debe cerrar directamente");
    }

    // =========================================================================
    // TC-05 a TC-11: Verificación de jerarquía de clases (compatibles headless)
    // =========================================================================

    @Test
    @DisplayName("TC-05: AgregarMedicamentoView extiende FormularioConfirmableDialog")
    void tc05_agregarMedicamentoView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(AgregarMedicamentoView.class),
                "AgregarMedicamentoView debe extender FormularioConfirmableDialog");
    }

    @Test
    @DisplayName("TC-06: EditarMedicamentoView extiende FormularioConfirmableDialog")
    void tc06_editarMedicamentoView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(EditarMedicamentoView.class),
                "EditarMedicamentoView debe extender FormularioConfirmableDialog");
    }

    @Test
    @DisplayName("TC-07: PersonalizacionView extiende FormularioConfirmableDialog")
    void tc07_personalizacionView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(PersonalizacionView.class),
                "PersonalizacionView debe extender FormularioConfirmableDialog");
    }

    @Test
    @DisplayName("TC-08: RegistrarVentaView extiende FormularioConfirmableDialog")
    void tc08_registrarVentaView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(RegistrarVentaView.class),
                "RegistrarVentaView debe extender FormularioConfirmableDialog");
    }

    @Test
    @DisplayName("TC-09: EditarVentaView extiende FormularioConfirmableDialog")
    void tc09_editarVentaView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(EditarVentaView.class),
                "EditarVentaView debe extender FormularioConfirmableDialog");
    }

    @Test
    @DisplayName("TC-10: RegistrarApartadoView extiende FormularioConfirmableDialog")
    void tc10_registrarApartadoView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(RegistrarApartadoView.class),
                "RegistrarApartadoView debe extender FormularioConfirmableDialog");
    }

    @Test
    @DisplayName("TC-11: EditarApartadoView extiende FormularioConfirmableDialog")
    void tc11_editarApartadoView_extiendeBase() {
        assertTrue(FormularioConfirmableDialog.class.isAssignableFrom(EditarApartadoView.class),
                "EditarApartadoView debe extender FormularioConfirmableDialog");
    }
}

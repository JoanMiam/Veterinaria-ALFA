package model;

/**
 * [MR-010] Registro del historial de ventas.
 *
 * @param id         identificador de la venta.
 * @param idProducto producto (lote) vendido.
 * @param nombre     nombre del medicamento al momento de la venta.
 * @param cantidad   unidades vendidas.
 * @param fecha      fecha de venta {@code yyyy-MM-dd} (puede ser {@code null} en registros antiguos).
 */
public record Venta(int id, int idProducto, String nombre, int cantidad, String fecha) {
}

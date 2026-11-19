package model;

/**
 * [MR-010] Medicamento del inventario (un lote). Sustituye los {@code Object[]} que
 * viajaban entre capas.
 *
 * <p>Las fechas se conservan como texto tal como están en la BD, porque registros
 * antiguos pueden traer valores inválidos; para interpretarlas usar {@link ReglasCaducidad}.
 *
 * @param id            identificador del producto/lote.
 * @param nombre        nombre del medicamento.
 * @param existencias   unidades disponibles.
 * @param lote          número de lote.
 * @param caducidad     mes de caducidad {@code yyyy-MM}.
 * @param fechaEntrada  fecha de entrada {@code yyyy-MM-dd}.
 * @param fechaApartado fecha de apartado {@code yyyy-MM-dd}, o {@code null} si no está apartado.
 */
public record Producto(int id,
                       String nombre,
                       int existencias,
                       String lote,
                       String caducidad,
                       String fechaEntrada,
                       String fechaApartado) {

    /** @return {@code true} si el producto tiene un apartado registrado. */
    public boolean estaApartado() {
        return fechaApartado != null && !fechaApartado.isBlank();
    }
}

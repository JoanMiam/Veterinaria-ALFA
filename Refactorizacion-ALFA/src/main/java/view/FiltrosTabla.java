package view;

import java.util.regex.Pattern;

import javax.swing.RowFilter;

/**
 * [FIX-009] Filtros de texto seguros para las tablas.
 *
 * <p>Antes el texto del buscador se usaba como expresión regular sin escapar:
 * escribir {@code (}, {@code [} o {@code *} lanzaba {@code PatternSyntaxException}
 * en el hilo de la interfaz. Ahora el texto se trata siempre como literal.
 */
final class FiltrosTabla {

    private FiltrosTabla() { }

    /**
     * Crea un filtro que muestra las filas cuyo texto contiene {@code texto},
     * sin distinguir mayúsculas ni minúsculas (incluye acentos).
     *
     * @param texto    texto escrito por el usuario.
     * @param columnas columnas donde buscar; vacío = todas.
     * @return el filtro, o {@code null} (sin filtro) si el texto está vacío.
     */
    static RowFilter<Object, Object> contieneTexto(String texto, int... columnas) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return RowFilter.regexFilter("(?iu)" + Pattern.quote(texto.trim()), columnas);
    }
}

package model;

import java.util.List;

/**
 * [MR-005 / MR-010] Genera el HTML de los reportes (inventario, ventas, apartados) a
 * partir de los datos ya consultados. Antes vivía dentro del DAO.
 *
 * <p>Todo el texto proveniente de la BD se escapa (FIX-009).
 */
public final class ReportesHtml {

    private ReportesHtml() { }

    /** Columnas: ID, Nombre, Existencias, Lote, Caducidad, Fecha Entrada. */
    public static String inventario(List<Producto> productos, ConfiguracionVeterinaria config) {
        StringBuilder html = encabezado("Reporte de Inventario", config);
        html.append("<table><thead><tr>")
            .append("<th>ID</th><th>Nombre</th><th>Existencias</th><th>Lote</th><th>Caducidad</th><th>Fecha Entrada</th>")
            .append("</tr></thead><tbody>\n");
        for (Producto p : productos) {
            html.append("<tr>")
                .append(celda(p.id()))
                .append(celda(p.nombre()))
                .append(celda(p.existencias()))
                .append(celda(p.lote()))
                .append(celda(p.caducidad()))
                .append(celda(p.fechaEntrada()))
                .append("</tr>\n");
        }
        return cerrar(html);
    }

    /** Columnas: Nombre del Medicamento, Cantidad Vendida, Fecha de Venta. */
    public static String ventas(List<Venta> ventas, ConfiguracionVeterinaria config) {
        StringBuilder html = encabezado("Reporte de Ventas", config);
        html.append("<table><thead><tr>")
            .append("<th>Nombre del Medicamento</th><th>Cantidad Vendida</th><th>Fecha de Venta</th>")
            .append("</tr></thead><tbody>\n");
        for (Venta v : ventas) {
            html.append("<tr>")
                .append(celda(v.nombre()))
                .append(celda(v.cantidad()))
                .append(celda(v.fecha() != null ? v.fecha() : "N/A"))
                .append("</tr>\n");
        }
        return cerrar(html);
    }

    /** Columnas: ID, Nombre, Existencias, Lote, Caducidad, Fecha Entrada, Fecha Apartado. */
    public static String apartados(List<Producto> apartados, ConfiguracionVeterinaria config) {
        StringBuilder html = encabezado("Reporte de Apartados", config);
        html.append("<table><thead><tr>")
            .append("<th>ID</th><th>Nombre</th><th>Existencias</th><th>Lote</th><th>Caducidad</th>")
            .append("<th>Fecha Entrada</th><th>Fecha Apartado</th>")
            .append("</tr></thead><tbody>\n");
        for (Producto p : apartados) {
            html.append("<tr>")
                .append(celda(p.id()))
                .append(celda(p.nombre()))
                .append(celda(p.existencias()))
                .append(celda(p.lote()))
                .append(celda(p.caducidad()))
                .append(celda(p.fechaEntrada()))
                .append(celda(p.fechaApartado()))
                .append("</tr>\n");
        }
        return cerrar(html);
    }

    /** Escapa texto para insertarlo en HTML ({@code null} → cadena vacía). */
    public static String escapar(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Documento HTML hasta el título del reporte. Si hay configuración de la clínica,
     * incluye logotipo, nombre y datos de contacto.
     */
    private static StringBuilder encabezado(String titulo, ConfiguracionVeterinaria config) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
            .append("<title>").append(escapar(titulo)).append("</title>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;font-size:13px;margin:40px 60px;color:#000;}")
            .append("h1{text-align:center;font-size:18px;font-weight:bold;margin-bottom:6px;}")
            .append(".vet-info{text-align:center;font-size:12px;margin-bottom:20px;}")
            .append("table{width:100%;border-collapse:collapse;}")
            .append("th,td{border:1px solid #000;padding:6px 10px;}")
            .append("th{background-color:#BDD7EE;font-weight:bold;text-align:center;}")
            .append("</style></head><body>\n");
        if (config != null && config.nombre() != null) {
            if (config.tieneLogo()) {
                // JPG en Base64 empieza con /9j/; PNG con iVBOR
                String mime = config.logoBase64().startsWith("/9j/") ? "image/jpeg" : "image/png";
                html.append("<div style=\"text-align:center;margin-bottom:10px;\">")
                    .append("<img src=\"data:").append(mime).append(";base64,").append(escapar(config.logoBase64()))
                    .append("\" style=\"max-height:80px;\"/></div>\n");
            }
            html.append("<h1>").append(escapar(config.nombre())).append("</h1>\n");
            html.append("<div class=\"vet-info\">");
            if (config.direccion() != null) html.append(escapar(config.direccion())).append(" &nbsp;|&nbsp; ");
            if (config.telefono() != null) html.append("Tel: ").append(escapar(config.telefono())).append(" &nbsp;|&nbsp; ");
            if (config.rfc() != null) html.append("RFC: ").append(escapar(config.rfc()));
            if (config.horarios() != null) html.append("<br/>Horarios: ").append(escapar(config.horarios()));
            html.append("</div>\n");
        }
        html.append("<h1>").append(escapar(titulo)).append("</h1>\n");
        return html;
    }

    private static String celda(Object valor) {
        return "<td>" + escapar(valor) + "</td>";
    }

    private static String cerrar(StringBuilder html) {
        return html.append("</tbody></table></body></html>").toString();
    }
}

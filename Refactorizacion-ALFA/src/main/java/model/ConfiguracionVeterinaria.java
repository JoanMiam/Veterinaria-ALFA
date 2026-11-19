package model;

/**
 * [MR-010] Datos de la clínica que aparecen en el encabezado de los reportes.
 *
 * @param logoBase64 logotipo PNG/JPG codificado en Base64, o {@code null}.
 * @param nombre     nombre de la veterinaria (obligatorio).
 * @param direccion  dirección.
 * @param telefono   teléfono de contacto.
 * @param rfc        RFC.
 * @param horarios   horarios de atención.
 */
public record ConfiguracionVeterinaria(String logoBase64,
                                       String nombre,
                                       String direccion,
                                       String telefono,
                                       String rfc,
                                       String horarios) {

    /** @return {@code true} si hay un logotipo cargado. */
    public boolean tieneLogo() {
        return logoBase64 != null && !logoBase64.isEmpty();
    }
}

package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DReporte;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU8: Reportes y Estadisticas (Texto + CSV).
 */
public class NReporte {
    private final DReporte dReporte = new DReporte();

    public ResultadoReporte generarReporte(List<String> params, Path directorioBase) throws SQLException, IOException {
        NUsuario.validarCantidad(params, 2, "GENREP[\"tipo_reporte\",\"id_proceso\"]");
        String tipo = NUsuario.exigirTexto(params.get(0), "tipo_reporte").toUpperCase();
        String idProcesoStr = params.get(1).trim();

        List<String[]> tabla = switch (tipo) {
            case "EMBUDO" -> dReporte.reporteEmbudo(NUsuario.parsearLong(idProcesoStr, "id_proceso"));
            case "TAREAS", "PENDIENTES" -> dReporte.reporteBandejaTareas(idProcesoStr);
            case "PUNTAJES", "NOTAS" -> dReporte.reportePuntajes(NUsuario.parsearLong(idProcesoStr, "id_proceso"));
            case "GENERAL", "ESTADISTICAS", "*" -> dReporte.reporteEstadisticasGenerales();
            default -> throw new IllegalArgumentException(
                    "Tipo de reporte desconocido: " + tipo + ". Use EMBUDO, TAREAS, PUNTAJES o GENERAL.");
        };

        Files.createDirectories(directorioBase);
        String nombreCsv = "reporte_" + tipo.toLowerCase() + ".csv";
        Path archivoCsv = directorioBase.resolve(nombreCsv);
        Files.writeString(archivoCsv, construirCsv(tabla), StandardCharsets.UTF_8);

        return new ResultadoReporte("REPORTE OPERATIVO: " + tipo, tabla, archivoCsv);
    }

    private String construirCsv(List<String[]> filas) {
        StringBuilder sb = new StringBuilder();
        for (String[] fila : filas) {
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) sb.append(';');
                String val = fila[i] == null ? "" : fila[i].replace("\"", "\"\"");
                sb.append('"').append(val).append('"');
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public record ResultadoReporte(String titulo, List<String[]> tabla, Path archivoCsv) {}
}

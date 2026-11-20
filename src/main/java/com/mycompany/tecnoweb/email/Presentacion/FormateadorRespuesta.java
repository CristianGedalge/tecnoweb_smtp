package com.mycompany.tecnoweb.email.Presentacion;

import java.util.List;

/**
 * Capa de Presentacion: Formatea las tablas de consulta, mensajes de Exito/Error
 * y el manual de ayuda para enviarlos por correo.
 */
public final class FormateadorRespuesta {

    private FormateadorRespuesta() {
    }

    public static String formatearTabla(String titulo, List<String[]> filas) {
        if (filas == null || filas.isEmpty()) {
            return titulo + "\n(Sin registros)";
        }

        String[] cabecera = filas.get(0);
        int cols = cabecera.length;
        int[] anchos = new int[cols];
        for (String[] fila : filas) {
            for (int c = 0; c < Math.min(cols, fila.length); c++) {
                String celda = fila[c] == null ? "" : fila[c];
                anchos[c] = Math.max(anchos[c], celda.length());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(titulo).append(" ===\n\n");
        String lineaSeparadora = construirSeparador(anchos);

        sb.append(lineaSeparadora).append("\n");
        sb.append(construirFila(cabecera, anchos)).append("\n");
        sb.append(lineaSeparadora).append("\n");

        for (int i = 1; i < filas.size(); i++) {
            sb.append(construirFila(filas.get(i), anchos)).append("\n");
        }
        sb.append(lineaSeparadora).append("\n");
        sb.append("Total de registros obtenidos: ").append(Math.max(0, filas.size() - 1)).append("\n");
        return sb.toString();
    }

    private static String construirSeparador(int[] anchos) {
        StringBuilder sb = new StringBuilder("+");
        for (int ancho : anchos) {
            sb.append("-".repeat(ancho + 2)).append("+");
        }
        return sb.toString();
    }

    private static String construirFila(String[] fila, int[] anchos) {
        StringBuilder sb = new StringBuilder("|");
        for (int c = 0; c < anchos.length; c++) {
            String val = (c < fila.length && fila[c] != null) ? fila[c] : "";
            sb.append(" ").append( String.format("%-" + anchos[c] + "s", val) ).append(" |");
        }
        return sb.toString();
    }

    public static String generarAyudaCompleta() {
        return """
                ======================================================================
                  SISTEMA DE SELECCION DE PERSONAL POR CORREO - TECNOWEB (UAGRM)
                ======================================================================
                Envie un correo colocando el comando en el ASUNTO con el formato:
                  COMANDO["param1","param2",...]

                --- CU1. GESTION DE USUARIOS, PERSONAS Y ROLES ---
                  LISROL["*"]
                  LISPER["*"]
                  INSPER["ci","complemento","departamento","nombres","apellidos","fecha_nac","celular"]
                  MODPER["id_persona","ci","nombres","apellidos","celular"]
                  LISUSU["*"]
                  INSUSU["id_persona","codigo_rol","correo","password"]
                  Roles: ADMIN, AYUDANTE, CANDIDATO, EMPRESA.
                  RECLUTADO es quien aprobo un proceso: consultar con LISREC.
                  MODUSU["id_usuario","codigo_rol","correo"]
                  ELIUSU["id_usuario"]

                --- CU2. CANDIDATOS, PRIVACIDAD Y CONSENTIMIENTO ---
                  DATACA["id_persona","carrera","registro","anio","ppa"]
                  LISCON["id_persona"]
                  INSCON["id_persona","tipo_consentimiento","aceptado"]
                  ELICON["id_consentimiento"]
                  LISDOC["id_postulacion"]
                  INSDOC["id_postulacion","codigo_tipo_doc"]   (adjuntar archivo al correo)
                  VALDOC["id_documento","estado","id_evaluador","observacion"]

                --- CU3. EMPRESAS, PERFILES DE CARGO E INSTRUMENTOS ---
                  LISEMP["*"]
                  INSEMP["nit","razon_social","rubro","nombre_contacto","cargo_contacto","correo_contacto","telefono"]
                  MODEMP["id_empresa","razon_social","rubro","correo_contacto","telefono"]
                  LISCAR["*"]
                  INSCAR["id_empresa","codigo_proceso","cargo","funciones","vacantes","top_n"]
                  INSREQ["id_proceso","codigo_dimension","nivel_esperado"]
                  LISPRU["id_proceso"]
                  ASIPRU["id_proceso","codigo_prueba","peso","puntaje_minimo","obligatoria"]
                  INSPRE["codigo_prueba","codigo_dimension","enunciado","opciones","opcion_correcta","puntaje"]
                  INSPRU["codigo","nombre","tipo","descripcion"]
                  INSCRI["codigo_prueba","nombre","maximo","orden"]
                  INSDIM["codigo","nombre","tipo"]

                --- CU4. CLASIFICAR CANDIDATOS, PLANES Y TRAZABILIDAD ---
                  LISPLA["id_proceso"]
                  INSCOV["codigo","nombre","carrera","id_admin"]
                  LISPOS["id_proceso"]
                  INSPOS["id_proceso","id_persona"]
                  ASIETA["id_postulacion","codigo_etapa","estado_etapa","id_evaluador","observacion"]
                  LISHIS["id_postulacion"]
                  FINPOS["id_postulacion","id_evaluador"]
                  LISREC["id_proceso_o_asterisco"]

                --- CU5. EJECUTAR PRUEBAS, CONSOLIDAR Y CALIFICAR ---
                  INIPRU["id_postulacion","codigo_prueba"]
                  RESPRU["id_intento","respuestas"]
                  CALRUB["id_postulacion","codigo_prueba","nombre_criterio","puntaje","observacion"]
                  LISRES["id_postulacion"]
                  CALPPA["id_postulacion"]

                --- CU6. BUSQUEDA ARITMETICA, RANKING Y MATRIZ ---
                  BUSCAN["id_proceso","puntaje_min","afinidad_min"]
                  GENRAN["id_proceso"]
                  GENMAT["id_proceso"]

                --- CU7. INFORMES INDIVIDUALES, EJECUTIVOS Y DESPACHO ---
                  INSINF["id_postulacion","recomendacion","fortalezas","debilidades","conclusion"]
                  GENIND["id_postulacion"]
                  GENINF["id_proceso","resumen_ejecutivo"]
                  ENVINF["id_proceso"]

                --- CU8. REPORTES Y ESTADISTICAS ---
                  GENREP["tipo_reporte","id_proceso"]   (EMBUDO | TAREAS | PUNTAJES | GENERAL)
                  AYUDA["*"]
                ======================================================================
                """;
    }
}

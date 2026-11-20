package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela intento_prueba, respuesta_intento, calificacion_criterio,
 * resultado_prueba y resultado_dimension (CU5).
 */
public class DEvaluacion {
    private long id;
    private long postulacionId;
    private long procesoPruebaId;
    private long intentoId;
    private String codigoPrueba;
    private String respuestasPipe;
    private String nombreCriterio;
    private BigDecimal puntajeBruto;
    private BigDecimal puntajeMaximo;
    private BigDecimal puntajeNormalizado;
    private boolean aprobada;
    private String observaciones;

    public DEvaluacion() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPostulacionId() { return postulacionId; }
    public void setPostulacionId(long postulacionId) { this.postulacionId = postulacionId; }

    public long getProcesoPruebaId() { return procesoPruebaId; }
    public void setProcesoPruebaId(long procesoPruebaId) { this.procesoPruebaId = procesoPruebaId; }

    public long getIntentoId() { return intentoId; }
    public void setIntentoId(long intentoId) { this.intentoId = intentoId; }

    public String getCodigoPrueba() { return codigoPrueba; }
    public void setCodigoPrueba(String codigoPrueba) { this.codigoPrueba = codigoPrueba; }

    public String getRespuestasPipe() { return respuestasPipe; }
    public void setRespuestasPipe(String respuestasPipe) { this.respuestasPipe = respuestasPipe; }

    public String getNombreCriterio() { return nombreCriterio; }
    public void setNombreCriterio(String nombreCriterio) { this.nombreCriterio = nombreCriterio; }

    public BigDecimal getPuntajeBruto() { return puntajeBruto; }
    public void setPuntajeBruto(BigDecimal puntajeBruto) { this.puntajeBruto = puntajeBruto; }

    public BigDecimal getPuntajeMaximo() { return puntajeMaximo; }
    public void setPuntajeMaximo(BigDecimal puntajeMaximo) { this.puntajeMaximo = puntajeMaximo; }

    public BigDecimal getPuntajeNormalizado() { return puntajeNormalizado; }
    public void setPuntajeNormalizado(BigDecimal puntajeNormalizado) { this.puntajeNormalizado = puntajeNormalizado; }

    public boolean isAprobada() { return aprobada; }
    public void setAprobada(boolean aprobada) { this.aprobada = aprobada; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String iniciar() throws SQLException {
        return iniciarIntentoPrueba(this.postulacionId, this.codigoPrueba);
    }

    public String responder() throws SQLException {
        return registrarRespuestasYCalificar(this.intentoId, this.respuestasPipe);
    }

    public String calificarRubrica() throws SQLException {
        return calificarRubricaManual(this.postulacionId, this.codigoPrueba,
                this.nombreCriterio, this.puntajeBruto, this.observaciones);
    }

    public String iniciarIntentoPrueba(long idPostulacion, String codigoPrueba) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long procPruebaId;
                int pruebaId;
                String nombrePrueba;
                String sqlPP = "SELECT pp.id, p.id, p.nombre "
                        + "FROM postulacion po "
                        + "JOIN proceso_etapa pe ON pe.proceso_id = po.proceso_id "
                        + "JOIN proceso_prueba pp ON pp.proceso_etapa_id = pe.id "
                        + "JOIN prueba p ON p.id = pp.prueba_id "
                        + "WHERE po.id = ? AND UPPER(p.codigo) = UPPER(?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlPP)) {
                    ps.setLong(1, idPostulacion);
                    ps.setString(2, codigoPrueba.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("La prueba " + codigoPrueba + " no esta asignada al proceso de la postulacion " + idPostulacion);
                        }
                        procPruebaId = rs.getLong(1);
                        pruebaId = rs.getInt(2);
                        nombrePrueba = rs.getString(3);
                    }
                }

                int nroIntento = 1;
                try (PreparedStatement psNro = conn.prepareStatement(
                        "SELECT COALESCE(MAX(nro_intento), 0) + 1 FROM intento_prueba WHERE postulacion_id = ? AND proceso_prueba_id = ?")) {
                    psNro.setLong(1, idPostulacion);
                    psNro.setLong(2, procPruebaId);
                    try (ResultSet rs = psNro.executeQuery()) {
                        if (rs.next()) nroIntento = rs.getInt(1);
                    }
                }

                long intId;
                try (PreparedStatement psIns = conn.prepareStatement(
                        "INSERT INTO intento_prueba (postulacion_id, proceso_prueba_id, nro_intento, estado, iniciado_en) "
                                + "VALUES (?, ?, ?, 'EN_CURSO', now()) RETURNING id")) {
                    psIns.setLong(1, idPostulacion);
                    psIns.setLong(2, procPruebaId);
                    psIns.setInt(3, nroIntento);
                    try (ResultSet rs = psIns.executeQuery()) {
                        rs.next();
                        intId = rs.getLong(1);
                    }
                }

                StringBuilder cuestionario = new StringBuilder();
                cuestionario.append("Éxito: Intento de prueba iniciado correctamente (ID_INTENTO: ").append(intId).append(")\n");
                cuestionario.append("Prueba: ").append(codigoPrueba.toUpperCase()).append(" - ").append(nombrePrueba).append("\n");
                cuestionario.append("Para responder envie: RESPRU[\"").append(intId).append("\",\"1:A|2:B|...\"]\n\n");
                cuestionario.append("--- PREGUNTAS ---\n");

                try (PreparedStatement psPreg = conn.prepareStatement(
                        "SELECT id, orden, COALESCE(enunciado, '') FROM pregunta WHERE prueba_id = ? ORDER BY orden ASC")) {
                    psPreg.setInt(1, pruebaId);
                    try (ResultSet rsP = psPreg.executeQuery()) {
                        while (rsP.next()) {
                            long pregId = rsP.getLong(1);
                            int orden = rsP.getInt(2);
                            cuestionario.append(orden).append(") [ID:").append(pregId).append("] ").append(rsP.getString(3)).append("\n");
                            try (PreparedStatement psOp = conn.prepareStatement(
                                    "SELECT etiqueta, COALESCE(texto, '') FROM opcion_pregunta WHERE pregunta_id = ? ORDER BY etiqueta ASC")) {
                                psOp.setLong(1, pregId);
                                try (ResultSet rsO = psOp.executeQuery()) {
                                    while (rsO.next()) {
                                        cuestionario.append("   ").append(rsO.getString(1)).append(": ").append(rsO.getString(2)).append("\n");
                                    }
                                }
                            }
                        }
                    }
                }

                conn.commit();
                this.intentoId = intId;
                return cuestionario.toString();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public String registrarRespuestasYCalificar(long idIntento, String respuestasPipe) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long postId;
                long procPruebaId;
                int pruebaId;
                BigDecimal umbralMinimo;

                String sqlInt = "SELECT i.postulacion_id, i.proceso_prueba_id, pp.prueba_id, COALESCE(pp.puntaje_minimo, 51.00) "
                        + "FROM intento_prueba i "
                        + "JOIN proceso_prueba pp ON pp.id = i.proceso_prueba_id "
                        + "WHERE i.id = ? AND i.estado = 'EN_CURSO' FOR UPDATE OF i";
                try (PreparedStatement ps = conn.prepareStatement(sqlInt)) {
                    ps.setLong(1, idIntento);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No existe el intento de prueba ID " + idIntento);
                        postId = rs.getLong(1);
                        procPruebaId = rs.getLong(2);
                        pruebaId = rs.getInt(3);
                        umbralMinimo = rs.getBigDecimal(4);
                    }
                }

                String[] pares = respuestasPipe.split("\\|");
                for (String par : pares) {
                    if (par.isBlank()) continue;
                    String[] kv = par.trim().split(":", 2);
                    if (kv.length < 2) continue;
                    int numPregunta = Integer.parseInt(kv[0].trim());
                    String etiqueta = kv[1].trim().toUpperCase().substring(0, 1);

                    Long preguntaId = null;
                    Long opcionId = null;
                    try (PreparedStatement psBus = conn.prepareStatement(
                            "SELECT p.id, op.id FROM pregunta p "
                                    + "JOIN opcion_pregunta op ON op.pregunta_id = p.id "
                                    + "WHERE p.prueba_id = ? AND (p.orden = ? OR p.id = ?) AND UPPER(op.etiqueta) = ? LIMIT 1")) {
                        psBus.setInt(1, pruebaId);
                        psBus.setInt(2, numPregunta);
                        psBus.setLong(3, numPregunta);
                        psBus.setString(4, etiqueta);
                        try (ResultSet rs = psBus.executeQuery()) {
                            if (rs.next()) {
                                preguntaId = rs.getLong(1);
                                opcionId = rs.getLong(2);
                            }
                        }
                    }

                    if (preguntaId != null && opcionId != null) {
                        try (PreparedStatement psResp = conn.prepareStatement(
                                "INSERT INTO respuesta_intento (intento_id, pregunta_id, opcion_id, respondida_en) "
                                        + "VALUES (?, ?, ?, now()) "
                                        + "ON CONFLICT (intento_id, pregunta_id) DO UPDATE SET opcion_id = EXCLUDED.opcion_id")) {
                            psResp.setLong(1, idIntento);
                            psResp.setLong(2, preguntaId);
                            psResp.setLong(3, opcionId);
                            psResp.executeUpdate();
                        }
                    }
                }

                try (PreparedStatement psFin = conn.prepareStatement(
                        "UPDATE intento_prueba SET estado = 'FINALIZADO', finalizado_en = now() WHERE id = ?")) {
                    psFin.setLong(1, idIntento);
                    psFin.executeUpdate();
                }

                BigDecimal bruto = BigDecimal.ZERO;
                try (PreparedStatement psBruto = conn.prepareStatement(
                        "SELECT COALESCE(SUM(op.puntaje), 0) FROM respuesta_intento ri "
                                + "JOIN opcion_pregunta op ON op.id = ri.opcion_id WHERE ri.intento_id = ?")) {
                    psBruto.setLong(1, idIntento);
                    try (ResultSet rs = psBruto.executeQuery()) {
                        if (rs.next()) bruto = rs.getBigDecimal(1);
                    }
                }

                BigDecimal maximo = BigDecimal.ZERO;
                try (PreparedStatement psMax = conn.prepareStatement(
                        "SELECT COALESCE(SUM(max_pts), 100) FROM ("
                                + "SELECT MAX(op.puntaje) AS max_pts FROM pregunta p "
                                + "JOIN opcion_pregunta op ON op.pregunta_id = p.id "
                                + "WHERE p.prueba_id = ? GROUP BY p.id) t")) {
                    psMax.setInt(1, pruebaId);
                    try (ResultSet rs = psMax.executeQuery()) {
                        if (rs.next()) maximo = rs.getBigDecimal(1);
                    }
                }
                if (maximo.compareTo(BigDecimal.ZERO) <= 0) {
                    maximo = new BigDecimal("100.00");
                }

                BigDecimal normalizado = bruto.multiply(new BigDecimal("100"))
                        .divide(maximo, 2, RoundingMode.HALF_UP);
                boolean okAprobada = normalizado.compareTo(umbralMinimo) >= 0;

                long resultadoPruebaId;
                String sqlRes = "INSERT INTO resultado_prueba (postulacion_id, proceso_prueba_id, intento_id, puntaje_bruto, puntaje_maximo, umbral_aplicado, aprobada, motivo_no_aprobacion, calificado_en) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, now()) "
                        + "ON CONFLICT (postulacion_id, proceso_prueba_id) DO UPDATE "
                        + "SET intento_id = EXCLUDED.intento_id, puntaje_bruto = EXCLUDED.puntaje_bruto, "
                        + "puntaje_maximo = EXCLUDED.puntaje_maximo, aprobada = EXCLUDED.aprobada, "
                        + "motivo_no_aprobacion = EXCLUDED.motivo_no_aprobacion, calificado_en = now() "
                        + "RETURNING id";
                try (PreparedStatement psRes = conn.prepareStatement(sqlRes)) {
                    psRes.setLong(1, postId);
                    psRes.setLong(2, procPruebaId);
                    psRes.setLong(3, idIntento);
                    psRes.setBigDecimal(4, bruto);
                    psRes.setBigDecimal(5, maximo);
                    psRes.setBigDecimal(6, umbralMinimo);
                    psRes.setBoolean(7, okAprobada);
                    if (okAprobada) psRes.setNull(8, java.sql.Types.VARCHAR);
                    else psRes.setString(8, "PUNTAJE_BAJO");
                    try (ResultSet rs = psRes.executeQuery()) {
                        rs.next();
                        resultadoPruebaId = rs.getLong(1);
                    }
                }

                String sqlDim = "INSERT INTO resultado_dimension (resultado_prueba_id, dimension_id, valor) "
                        + "SELECT ?, p.dimension_id, "
                        + "LEAST(10.00, ROUND(COALESCE(SUM(op.puntaje), 0) / GREATEST(1.0, SUM(op_max.max_p)) * 10, 2)) "
                        + "FROM pregunta p "
                        + "JOIN (SELECT pregunta_id, MAX(puntaje) AS max_p FROM opcion_pregunta GROUP BY pregunta_id) op_max ON op_max.pregunta_id = p.id "
                        + "LEFT JOIN respuesta_intento ri ON ri.pregunta_id = p.id AND ri.intento_id = ? "
                        + "LEFT JOIN opcion_pregunta op ON op.id = ri.opcion_id "
                        + "WHERE p.prueba_id = ? AND p.dimension_id IS NOT NULL "
                        + "GROUP BY p.dimension_id "
                        + "ON CONFLICT (resultado_prueba_id, dimension_id) DO UPDATE SET valor = EXCLUDED.valor";
                try (PreparedStatement psDim = conn.prepareStatement(sqlDim)) {
                    psDim.setLong(1, resultadoPruebaId);
                    psDim.setLong(2, idIntento);
                    psDim.setInt(3, pruebaId);
                    psDim.executeUpdate();
                }

                conn.commit();
                this.id = resultadoPruebaId;
                this.puntajeNormalizado = normalizado;
                this.aprobada = okAprobada;
                return "Éxito: Prueba finalizada y calificada. Puntaje normalizado: " + normalizado
                        + "/100 (" + (okAprobada ? "APROBADA" : "NO APROBADA") + ").";
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public String calificarRubricaManual(long idPostulacion, String codigoPrueba,
                                         String nombreCriterio, BigDecimal puntaje, String observacion) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long procPruebaId;
                int pruebaId;
                BigDecimal umbralMinimo;
                String sqlPP = "SELECT pp.id, p.id, COALESCE(pp.puntaje_minimo, 51.00) "
                        + "FROM postulacion po "
                        + "JOIN proceso_etapa pe ON pe.proceso_id = po.proceso_id "
                        + "JOIN proceso_prueba pp ON pp.proceso_etapa_id = pe.id "
                        + "JOIN prueba p ON p.id = pp.prueba_id "
                        + "WHERE po.id = ? AND UPPER(p.codigo) = UPPER(?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlPP)) {
                    ps.setLong(1, idPostulacion);
                    ps.setString(2, codigoPrueba.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("La prueba " + codigoPrueba + " no esta asignada al proceso de la postulacion " + idPostulacion);
                        }
                        procPruebaId = rs.getLong(1);
                        pruebaId = rs.getInt(2);
                        umbralMinimo = rs.getBigDecimal(3);
                    }
                }

                Integer criterioId = null;
                BigDecimal maxCriterio = new BigDecimal("10.00");
                try (PreparedStatement psCrit = conn.prepareStatement(
                        "SELECT id, puntaje_maximo FROM rubrica_criterio WHERE prueba_id = ? AND nombre ILIKE ? LIMIT 1")) {
                    psCrit.setInt(1, pruebaId);
                    psCrit.setString(2, "%" + nombreCriterio.trim() + "%");
                    try (ResultSet rs = psCrit.executeQuery()) {
                        if (rs.next()) {
                            criterioId = rs.getInt(1);
                            maxCriterio = rs.getBigDecimal(2);
                        }
                    }
                }

                if (criterioId == null) {
                    try (PreparedStatement psInsCrit = conn.prepareStatement(
                            "INSERT INTO rubrica_criterio (prueba_id, tipo_prueba, nombre, puntaje_maximo, orden) "
                                    + "VALUES (?, 'RUBRICA', ?, 100.00, 1) RETURNING id, puntaje_maximo")) {
                        psInsCrit.setInt(1, pruebaId);
                        psInsCrit.setString(2, nombreCriterio.trim());
                        try (ResultSet rs = psInsCrit.executeQuery()) {
                            rs.next();
                            criterioId = rs.getInt(1);
                            maxCriterio = rs.getBigDecimal(2);
                        }
                    }
                }

                if (puntaje.signum() < 0 || puntaje.compareTo(maxCriterio) > 0) {
                    throw new SQLException("El puntaje debe estar entre 0 y " + maxCriterio);
                }
                BigDecimal brutoInicial = puntaje;
                boolean aprobadaInicial = brutoInicial.multiply(new BigDecimal("100"))
                        .divide(maxCriterio, 2, RoundingMode.HALF_UP).compareTo(umbralMinimo) >= 0;

                long resultadoPruebaId;
                try (PreparedStatement psRes = conn.prepareStatement(
                        "INSERT INTO resultado_prueba (postulacion_id, proceso_prueba_id, puntaje_bruto, puntaje_maximo, umbral_aplicado, aprobada, motivo_no_aprobacion, observaciones) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                                + "ON CONFLICT (postulacion_id, proceso_prueba_id) DO UPDATE "
                                + "SET observaciones = EXCLUDED.observaciones RETURNING id")) {
                    psRes.setLong(1, idPostulacion);
                    psRes.setLong(2, procPruebaId);
                    psRes.setBigDecimal(3, brutoInicial);
                    psRes.setBigDecimal(4, maxCriterio);
                    psRes.setBigDecimal(5, umbralMinimo);
                    psRes.setBoolean(6, aprobadaInicial);
                    if (aprobadaInicial) psRes.setNull(7, java.sql.Types.VARCHAR);
                    else psRes.setString(7, "PUNTAJE_BAJO");
                    psRes.setString(8, observacion);
                    try (ResultSet rs = psRes.executeQuery()) {
                        rs.next();
                        resultadoPruebaId = rs.getLong(1);
                    }
                }

                try (PreparedStatement psCal = conn.prepareStatement(
                        "INSERT INTO calificacion_criterio (resultado_prueba_id, criterio_id, puntaje, comentario) "
                                + "VALUES (?, ?, ?, ?) "
                                + "ON CONFLICT (resultado_prueba_id, criterio_id) DO UPDATE "
                                + "SET puntaje = EXCLUDED.puntaje, comentario = EXCLUDED.comentario")) {
                    psCal.setLong(1, resultadoPruebaId);
                    psCal.setInt(2, criterioId);
                    psCal.setBigDecimal(3, brutoInicial);
                    psCal.setString(4, observacion);
                    psCal.executeUpdate();
                }

                BigDecimal sumaBruto = BigDecimal.ZERO;
                BigDecimal sumaMax = BigDecimal.ZERO;
                try (PreparedStatement psSum = conn.prepareStatement(
                        "SELECT COALESCE(SUM(cc.puntaje), 0), SUM(rc.puntaje_maximo) "
                                + "FROM rubrica_criterio rc LEFT JOIN calificacion_criterio cc "
                                + "ON cc.criterio_id = rc.id AND cc.resultado_prueba_id = ? "
                                + "WHERE rc.prueba_id = ?")) {
                    psSum.setLong(1, resultadoPruebaId);
                    psSum.setInt(2, pruebaId);
                    try (ResultSet rs = psSum.executeQuery()) {
                        if (rs.next()) {
                            sumaBruto = rs.getBigDecimal(1);
                            sumaMax = rs.getBigDecimal(2);
                        }
                    }
                }

                BigDecimal normFinal = sumaBruto.multiply(new BigDecimal("100"))
                        .divide(sumaMax, 2, RoundingMode.HALF_UP);
                boolean aprobadaFinal = normFinal.compareTo(umbralMinimo) >= 0;

                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE resultado_prueba SET puntaje_bruto = ?, puntaje_maximo = ?, aprobada = ?, "
                                + "motivo_no_aprobacion = ?, observaciones = ? WHERE id = ?")) {
                    psUpd.setBigDecimal(1, sumaBruto);
                    psUpd.setBigDecimal(2, sumaMax);
                    psUpd.setBoolean(3, aprobadaFinal);
                    if (aprobadaFinal) psUpd.setNull(4, java.sql.Types.VARCHAR);
                    else psUpd.setString(4, "PUNTAJE_BAJO");
                    psUpd.setString(5, observacion);
                    psUpd.setLong(6, resultadoPruebaId);
                    psUpd.executeUpdate();
                }

                conn.commit();
                this.id = resultadoPruebaId;
                this.puntajeNormalizado = normFinal;
                this.aprobada = aprobadaFinal;
                return "Éxito: Rubrica calificada en " + codigoPrueba.toUpperCase()
                        + ". Nota normalizada: " + normFinal + "/100.";
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<String[]> listarResultadosPostulacion(long idPostulacion) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"PRUEBA", "BRUTO", "MAXIMO", "NORMALIZADO_100", "PESO_%", "APROBADA", "OBSERVACIONES"});

        String sql = "SELECT p.codigo, r.puntaje_bruto, r.puntaje_maximo, r.puntaje_normalizado, "
                + "pp.peso, CASE WHEN r.aprobada THEN 'SI' ELSE 'NO' END, COALESCE(r.observaciones, '') "
                + "FROM resultado_prueba r "
                + "JOIN proceso_prueba pp ON pp.id = r.proceso_prueba_id "
                + "JOIN prueba p ON p.id = pp.prueba_id "
                + "WHERE r.postulacion_id = ? "
                + "ORDER BY pp.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getBigDecimal(2)),
                        String.valueOf(rs.getBigDecimal(3)),
                        String.valueOf(rs.getBigDecimal(4)),
                        String.valueOf(rs.getBigDecimal(5)),
                        rs.getString(6),
                        rs.getString(7)
                    });
                }
            }

            try (PreparedStatement psTot = conn.prepareStatement(
                    "SELECT puntaje_total, pruebas_calificadas, pruebas_configuradas "
                            + "FROM v_puntaje_postulacion WHERE postulacion_id = ?")) {
                psTot.setLong(1, idPostulacion);
                try (ResultSet rsT = psTot.executeQuery()) {
                    if (rsT.next()) {
                        filas.add(new String[]{
                            "TOTAL_PONDERADO",
                            "-",
                            "100.00",
                            String.valueOf(rsT.getBigDecimal(1)),
                            "100.00",
                            "CALIFICADAS: " + rsT.getInt(2) + "/" + rsT.getInt(3),
                            "Consolidado v_puntaje_postulacion"
                        });
                    }
                }
            }
        }
        return filas;
    }
}

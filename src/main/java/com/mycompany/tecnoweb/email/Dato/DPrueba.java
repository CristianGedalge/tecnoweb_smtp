package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela prueba, proceso_prueba, rubrica_criterio, pregunta y opcion_pregunta (CU3).
 */
public class DPrueba {
    private long id;
    private long procesoId;
    private String codigoPrueba;
    private String codigoDimension;
    private BigDecimal peso;
    private BigDecimal puntajeMinimo;
    private boolean obligatoria;
    private String enunciado;
    private String opcionesPipe;
    private String etiquetaCorrecta;
    private BigDecimal puntajePregunta;

    public DPrueba() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProcesoId() { return procesoId; }
    public void setProcesoId(long procesoId) { this.procesoId = procesoId; }

    public String getCodigoPrueba() { return codigoPrueba; }
    public void setCodigoPrueba(String codigoPrueba) { this.codigoPrueba = codigoPrueba; }

    public String getCodigoDimension() { return codigoDimension; }
    public void setCodigoDimension(String codigoDimension) { this.codigoDimension = codigoDimension; }

    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }

    public BigDecimal getPuntajeMinimo() { return puntajeMinimo; }
    public void setPuntajeMinimo(BigDecimal puntajeMinimo) { this.puntajeMinimo = puntajeMinimo; }

    public boolean isObligatoria() { return obligatoria; }
    public void setObligatoria(boolean obligatoria) { this.obligatoria = obligatoria; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public String getOpcionesPipe() { return opcionesPipe; }
    public void setOpcionesPipe(String opcionesPipe) { this.opcionesPipe = opcionesPipe; }

    public String getEtiquetaCorrecta() { return etiquetaCorrecta; }
    public void setEtiquetaCorrecta(String etiquetaCorrecta) { this.etiquetaCorrecta = etiquetaCorrecta; }

    public BigDecimal getPuntajePregunta() { return puntajePregunta; }
    public void setPuntajePregunta(BigDecimal puntajePregunta) { this.puntajePregunta = puntajePregunta; }

    public long asignarAProceso() throws SQLException {
        return asignarPruebaAProceso(this.procesoId, this.codigoPrueba, this.peso, this.puntajeMinimo, this.obligatoria);
    }

    public long insertarPregunta() throws SQLException {
        return insertarPreguntaConOpciones(this.codigoPrueba, this.codigoDimension,
                this.enunciado, this.opcionesPipe, this.etiquetaCorrecta, this.puntajePregunta);
    }

    public List<String[]> listarPruebasProceso(String filtroProcesoId) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean catalogoGeneral = filtroProcesoId == null || filtroProcesoId.isBlank() || filtroProcesoId.equals("*");

        if (catalogoGeneral) {
            filas.add(new String[]{"ID_PRUEBA", "CODIGO", "NOMBRE", "TIPO", "DESCRIPCION"});
            String sql = "SELECT id, codigo, nombre, tipo, COALESCE(descripcion, '') FROM prueba ORDER BY id ASC";
            try (Connection conn = ConexionDB.conectar();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                    });
                }
            }
            return filas;
        }

        filas.add(new String[]{"ID_PROC_PRUEBA", "CODIGO_PRUEBA", "NOMBRE", "TIPO", "PESO_%", "MINIMO", "OBLIGATORIA"});
        String sql = "SELECT pp.id, p.codigo, p.nombre, p.tipo, pp.peso, COALESCE(pp.puntaje_minimo, 0), "
                + "CASE WHEN pp.obligatoria THEN 'SI' ELSE 'NO' END "
                + "FROM proceso_prueba pp "
                + "JOIN proceso_etapa pe ON pe.id = pp.proceso_etapa_id "
                + "JOIN prueba p ON p.id = pp.prueba_id "
                + "WHERE pe.proceso_id = ? "
                + "ORDER BY pe.orden ASC, pp.orden ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(filtroProcesoId.trim()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        String.valueOf(rs.getBigDecimal(5)),
                        String.valueOf(rs.getBigDecimal(6)),
                        rs.getString(7)
                    });
                }
            }
        }
        return filas;
    }

    public long asignarPruebaAProceso(long idProceso, String codigoPrueba, BigDecimal peso,
                                      BigDecimal puntajeMinimo, boolean obligatoria) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            int pruebaId;
            String tipoPrueba;
            try (PreparedStatement psP = conn.prepareStatement(
                    "SELECT id, tipo FROM prueba WHERE UPPER(codigo) = UPPER(?)")) {
                psP.setString(1, codigoPrueba.trim());
                try (ResultSet rs = psP.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Prueba no encontrada en catalogo: " + codigoPrueba);
                    }
                    pruebaId = rs.getInt(1);
                    tipoPrueba = rs.getString(2);
                }
            }

            String codigoEtapaPreferida = switch (codigoPrueba.trim().toUpperCase()) {
                case "CV_ANALISIS" -> "ANALISIS_CV";
                case "ENTREVISTA" -> "ENTREVISTA";
                case "TEST_HABILIDADES_BLANDAS" -> "HABILIDADES_BLANDAS";
                case "PPA" -> "EVALUACION_TECNICA";
                default -> "TT".equals(tipoPrueba) ? "EVALUACION_TECNICA" : "EVALUACION_PSICOLOGICA";
            };

            long procesoEtapaId = obtenerProcesoEtapaId(conn, idProceso, codigoEtapaPreferida);

            String sql = "INSERT INTO proceso_prueba (proceso_etapa_id, prueba_id, orden, peso, obligatoria, puntaje_minimo, umbral_origen, max_intentos) "
                    + "VALUES (?, ?, 1, ?, ?, ?, 'FIJO', 3) "
                    + "ON CONFLICT (proceso_etapa_id, prueba_id) DO UPDATE "
                    + "SET peso = EXCLUDED.peso, obligatoria = EXCLUDED.obligatoria, puntaje_minimo = EXCLUDED.puntaje_minimo "
                    + "RETURNING id";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, procesoEtapaId);
                ps.setInt(2, pruebaId);
                ps.setBigDecimal(3, peso);
                ps.setBoolean(4, obligatoria);
                ps.setBigDecimal(5, puntajeMinimo);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    this.id = rs.getLong(1);
                    return this.id;
                }
            }
        }
    }

    public long insertarPreguntaConOpciones(String codigoPrueba, String codigoDimension,
                                            String enunciado, String opcionesPipe,
                                            String etiquetaCorrecta, BigDecimal puntajeCorrecta) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                int pruebaId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM prueba WHERE UPPER(codigo) = UPPER(?) AND tipo = 'TEST'")) {
                    ps.setString(1, codigoPrueba.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("La prueba debe existir y ser de tipo TEST: " + codigoPrueba);
                        }
                        pruebaId = rs.getInt(1);
                    }
                }

                Integer dimensionId = null;
                if (codigoDimension != null && !codigoDimension.isBlank() && !codigoDimension.equals("*")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM dimension_evaluada WHERE UPPER(codigo) = UPPER(?)")) {
                        ps.setString(1, codigoDimension.trim());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) dimensionId = rs.getInt(1);
                        }
                    }
                }

                int siguienteOrden = 1;
                try (PreparedStatement psOrd = conn.prepareStatement(
                        "SELECT COALESCE(MAX(orden), 0) + 1 FROM pregunta WHERE prueba_id = ?")) {
                    psOrd.setInt(1, pruebaId);
                    try (ResultSet rs = psOrd.executeQuery()) {
                        if (rs.next()) siguienteOrden = rs.getInt(1);
                    }
                }

                long preguntaId;
                try (PreparedStatement psPreg = conn.prepareStatement(
                        "INSERT INTO pregunta (prueba_id, tipo_prueba, dimension_id, orden, enunciado) "
                                + "VALUES (?, 'TEST', ?, ?, ?) RETURNING id")) {
                    psPreg.setInt(1, pruebaId);
                    if (dimensionId != null) psPreg.setInt(2, dimensionId);
                    else psPreg.setNull(2, java.sql.Types.INTEGER);
                    psPreg.setInt(3, siguienteOrden);
                    psPreg.setString(4, enunciado.trim());
                    try (ResultSet rs = psPreg.executeQuery()) {
                        rs.next();
                        preguntaId = rs.getLong(1);
                    }
                }

                String[] partesOpciones = opcionesPipe.split("\\|");
                for (String op : partesOpciones) {
                    String limpia = op.trim();
                    if (limpia.isEmpty()) continue;
                    String[] kv = limpia.split(":", 2);
                    String etiqueta = kv[0].trim().toUpperCase().substring(0, 1);
                    String texto = kv.length > 1 ? kv[1].trim() : limpia;
                    BigDecimal pts = etiqueta.equalsIgnoreCase(etiquetaCorrecta.trim()) ? puntajeCorrecta : BigDecimal.ZERO;

                    try (PreparedStatement psOp = conn.prepareStatement(
                            "INSERT INTO opcion_pregunta (pregunta_id, etiqueta, texto, puntaje) VALUES (?, ?, ?, ?)")) {
                        psOp.setLong(1, preguntaId);
                        psOp.setString(2, etiqueta);
                        psOp.setString(3, texto);
                        psOp.setBigDecimal(4, pts);
                        psOp.executeUpdate();
                    }
                }

                conn.commit();
                this.id = preguntaId;
                return preguntaId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private long obtenerProcesoEtapaId(Connection conn, long idProceso, String codigoEtapa) throws SQLException {
        String sql = "SELECT pe.id FROM proceso_etapa pe "
                + "WHERE pe.proceso_id = ? AND pe.codigo = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            ps.setString(2, codigoEtapa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM proceso_etapa WHERE proceso_id = ? ORDER BY orden ASC LIMIT 1")) {
            ps.setLong(1, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("El proceso ID " + idProceso + " no tiene etapas configuradas.");
            }
        }
    }
}

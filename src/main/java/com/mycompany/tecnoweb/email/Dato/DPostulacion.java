package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela proceso_etapa, postulacion, postulacion_etapa
 * e historial_postulacion_etapa (CU4).
 */
public class DPostulacion {
    private long id;
    private String codigo;
    private long procesoId;
    private long personaId;
    private Long registradaPor;
    private String estado;
    private String motivoCierre;
    private String codigoEtapa;
    private String estadoEtapa;
    private long evaluadorId;
    private String observacion;

    public DPostulacion() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public long getProcesoId() { return procesoId; }
    public void setProcesoId(long procesoId) { this.procesoId = procesoId; }

    public long getPersonaId() { return personaId; }
    public void setPersonaId(long personaId) { this.personaId = personaId; }

    public Long getRegistradaPor() { return registradaPor; }
    public void setRegistradaPor(Long registradaPor) { this.registradaPor = registradaPor; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoCierre() { return motivoCierre; }
    public void setMotivoCierre(String motivoCierre) { this.motivoCierre = motivoCierre; }

    public String getCodigoEtapa() { return codigoEtapa; }
    public void setCodigoEtapa(String codigoEtapa) { this.codigoEtapa = codigoEtapa; }

    public String getEstadoEtapa() { return estadoEtapa; }
    public void setEstadoEtapa(String estadoEtapa) { this.estadoEtapa = estadoEtapa; }

    public long getEvaluadorId() { return evaluadorId; }
    public void setEvaluadorId(long evaluadorId) { this.evaluadorId = evaluadorId; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public long insertar() throws SQLException {
        return insertarPostulacion(this.procesoId, this.personaId);
    }

    public boolean modificarEtapa() throws SQLException {
        return actualizarEtapaPostulacion(this.id, this.codigoEtapa, this.estadoEtapa, this.evaluadorId, this.observacion);
    }

    public List<String[]> listarPlanProceso(long idProceso) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_PROC_ETAPA", "ORDEN", "CODIGO_ETAPA", "NOMBRE", "EJECUCION", "ELIMINATORIA", "ACTIVA"});

        String sql = "SELECT pe.id, pe.orden, pe.codigo, pe.nombre, pe.ejecucion, "
                + "CASE WHEN pe.es_eliminatoria THEN 'SI' ELSE 'NO' END, "
                + "CASE WHEN pe.activa THEN 'SI' ELSE 'NO' END "
                + "FROM proceso_etapa pe "
                + "WHERE pe.proceso_id = ? "
                + "ORDER BY pe.orden ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        String.valueOf(rs.getInt(2)),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)
                    });
                }
            }
        }
        return filas;
    }

    public List<String[]> listarPostulaciones(String filtroProcesoId) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_POST", "CODIGO", "PROCESO_ID", "PERSONA_ID", "CANDIDATO", "CI", "ESTADO"});

        boolean todos = filtroProcesoId == null || filtroProcesoId.isBlank() || filtroProcesoId.equals("*");
        String sql = "SELECT po.id, po.codigo, po.proceso_id, po.persona_id, "
                + "TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno, '') || ' ' || COALESCE(pe.apellido_materno, '')), "
                + "pe.ci_numero, po.estado "
                + "FROM postulacion po "
                + "JOIN persona pe ON pe.id = po.persona_id "
                + (todos ? "" : "WHERE po.proceso_id = ? ")
                + "ORDER BY po.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setLong(1, Long.parseLong(filtroProcesoId.trim()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        String.valueOf(rs.getLong(3)),
                        String.valueOf(rs.getLong(4)),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)
                    });
                }
            }
        }
        return filas;
    }

    public long insertarPostulacion(long idProceso, long idPersona) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                String tipoProceso;
                try (PreparedStatement psTipo = conn.prepareStatement("SELECT tipo FROM proceso WHERE id = ?")) {
                    psTipo.setLong(1, idProceso);
                    try (ResultSet rs = psTipo.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No existe el proceso con ID " + idProceso);
                        tipoProceso = rs.getString(1);
                    }
                }

                Long regPor = null;
                if ("EMPRESA".equalsIgnoreCase(tipoProceso)) {
                    try (PreparedStatement psCon = conn.prepareStatement(
                            "SELECT contacto_id FROM proceso WHERE id = ?")) {
                        psCon.setLong(1, idProceso);
                        try (ResultSet rs = psCon.executeQuery()) {
                            if (rs.next()) regPor = rs.getLong(1);
                        }
                    }
                }

                String codigoPost = String.format("POS-%d-%04d", idProceso, idPersona);
                long postulacionId;
                try (PreparedStatement psIns = conn.prepareStatement(
                        "INSERT INTO postulacion (codigo, proceso_id, persona_id, registrada_por, estado) "
                                + "VALUES (?, ?, ?, ?, 'EN_PROCESO') RETURNING id")) {
                    psIns.setString(1, codigoPost);
                    psIns.setLong(2, idProceso);
                    psIns.setLong(3, idPersona);
                    if (regPor != null) psIns.setLong(4, regPor);
                    else psIns.setNull(4, java.sql.Types.BIGINT);
                    try (ResultSet rs = psIns.executeQuery()) {
                        rs.next();
                        postulacionId = rs.getLong(1);
                    }
                }

                try (PreparedStatement psEta = conn.prepareStatement(
                        "INSERT INTO postulacion_etapa (postulacion_id, proceso_etapa_id, estado, iniciada_en, motivo) "
                                + "SELECT ?, id, CASE WHEN orden = 1 THEN 'APROBADA' WHEN orden = 2 THEN 'EN_CURSO' ELSE 'PENDIENTE' END, "
                                + "now(), 'Inicio de postulacion por correo' "
                                + "FROM proceso_etapa WHERE proceso_id = ? ORDER BY orden ASC")) {
                    psEta.setLong(1, postulacionId);
                    psEta.setLong(2, idProceso);
                    psEta.executeUpdate();
                }

                conn.commit();
                this.id = postulacionId;
                this.codigo = codigoPost;
                return postulacionId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean actualizarEtapaPostulacion(long idPostulacion, String codigoEtapa,
                                              String nuevoEstado, long idEvaluador, String observacion) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                if (idEvaluador > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT 1 FROM usuario u JOIN rol r ON r.id=u.rol_id WHERE u.id=? "
                            + "AND u.estado='ACTIVO' AND r.codigo IN ('ADMIN','AYUDANTE')")) {
                        ps.setLong(1,idEvaluador);
                        try (ResultSet rs=ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("El evaluador debe ser administrador o ayudante activo.");
                        }
                    }
                }
                if (idEvaluador > 0) {
                    try (Statement st = conn.createStatement()) {
                        st.execute("SET LOCAL app.usuario_id = '" + idEvaluador + "'");
                    }
                }

                String sql = "UPDATE postulacion_etapa pe "
                        + "SET estado = UPPER(?), asignado_a = ?, asignado_en = COALESCE(asignado_en, now()), "
                        + "iniciada_en = COALESCE(iniciada_en, now()), "
                        + "finalizada_en = CASE WHEN UPPER(?) IN ('APROBADA','REPROBADA','OMITIDA') THEN now() ELSE finalizada_en END, "
                        + "motivo = ? "
                        + "FROM proceso_etapa pre "
                        + "WHERE pe.proceso_etapa_id = pre.id "
                        + "AND pe.postulacion_id = ? AND UPPER(pre.codigo) = UPPER(?)";

                int filasAfectadas;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nuevoEstado.trim());
                    if (idEvaluador > 0) ps.setLong(2, idEvaluador);
                    else ps.setNull(2, java.sql.Types.BIGINT);
                    ps.setString(3, nuevoEstado.trim());
                    ps.setString(4, observacion);
                    ps.setLong(5, idPostulacion);
                    ps.setString(6, codigoEtapa.trim());
                    filasAfectadas = ps.executeUpdate();
                }

                if (filasAfectadas > 0 && "REPROBADA".equalsIgnoreCase(nuevoEstado.trim())) {
                    try (PreparedStatement psPost = conn.prepareStatement(
                            "UPDATE postulacion SET estado = 'NO_APROBADA', motivo_cierre = ? WHERE id = ?")) {
                        psPost.setString(1, "Reprobado en etapa " + codigoEtapa + ": " + observacion);
                        psPost.setLong(2, idPostulacion);
                        psPost.executeUpdate();
                    }
                }

                conn.commit();
                return filasAfectadas > 0;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<String[]> listarHistorial(long idPostulacion) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_HIST", "ETAPA", "ESTADO_ANT", "ESTADO_NUEVO", "USUARIO", "MOTIVO"});

        String sql = "SELECT h.id, pre.codigo, COALESCE(h.estado_anterior, '-'), h.estado_nuevo, "
                + "COALESCE(u.correo, 'sistema'), COALESCE(h.motivo, '') "
                + "FROM historial_postulacion_etapa h "
                + "JOIN postulacion_etapa pe ON pe.id = h.postulacion_etapa_id "
                + "JOIN proceso_etapa pre ON pre.id = pe.proceso_etapa_id "
                + "LEFT JOIN usuario u ON u.id = h.usuario_id "
                + "WHERE pe.postulacion_id = ? "
                + "ORDER BY h.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6)
                    });
                }
            }
        }
        return filas;
    }
}

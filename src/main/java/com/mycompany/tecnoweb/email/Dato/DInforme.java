package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Informes ejecutivos y recomendaciones en postulacion (CU7).
 */
public class DInforme {
    private long id;
    private long procesoId;
    private long postulacionId;
    private String estado;
    private String resumen;
    private String recomendacionGeneral;
    private String etiqueta;
    private String fortalezas;
    private String debilidades;
    private String conclusion;

    public DInforme() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProcesoId() { return procesoId; }
    public void setProcesoId(long procesoId) { this.procesoId = procesoId; }

    public long getPostulacionId() { return postulacionId; }
    public void setPostulacionId(long postulacionId) { this.postulacionId = postulacionId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public String getRecomendacionGeneral() { return recomendacionGeneral; }
    public void setRecomendacionGeneral(String recomendacionGeneral) { this.recomendacionGeneral = recomendacionGeneral; }


    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getFortalezas() { return fortalezas; }
    public void setFortalezas(String fortalezas) { this.fortalezas = fortalezas; }

    public String getDebilidades() { return debilidades; }
    public void setDebilidades(String debilidades) { this.debilidades = debilidades; }

    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }

    public void guardarInformeIndividual() throws SQLException {
        registrarInformeIndividual(this.postulacionId, this.etiqueta,
                this.fortalezas, this.debilidades, this.conclusion);
    }

    public void registrarInformeIndividual(long idPostulacion, String etiqueta,
                                           String fortalezas, String debilidades, String conclusion) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long procId;
                try (PreparedStatement ps = conn.prepareStatement("SELECT proceso_id FROM postulacion WHERE id = ?")) {
                    ps.setLong(1, idPostulacion);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No existe la postulacion con ID " + idPostulacion);
                        procId = rs.getLong(1);
                    }
                }

                long elaboradoPor = obtenerUsuarioElaborador(conn);
                long informeId;
                String sqlInf = "INSERT INTO informe_final (proceso_id, estado, resumen, elaborado_por) "
                        + "VALUES (?, 'BORRADOR', 'Informe en elaboracion', ?) "
                        + "ON CONFLICT (proceso_id) DO UPDATE SET updated_at = now() RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sqlInf)) {
                    ps.setLong(1, procId);
                    ps.setLong(2, elaboradoPor);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        informeId = rs.getLong(1);
                    }
                }

                String etiquetaValida = normalizarEtiqueta(etiqueta);
                String textoRecomendacion = "Fortalezas: " + fortalezas
                        + " | Debilidades: " + debilidades
                        + " | Conclusion: " + conclusion;

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE postulacion SET etiqueta = ?, recomendacion = ? WHERE id = ?")) {
                    ps.setString(1,etiquetaValida);
                    ps.setString(2,textoRecomendacion);
                    ps.setLong(3,idPostulacion);
                    ps.executeUpdate();
                }

                conn.commit();
                this.id = informeId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public long emitirInformeEjecutivo(long idProceso, String resumenEjecutivo,
                                       String nombrePdf, String rutaPdf, long tamanoPdf) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long elaboradoPor = obtenerUsuarioElaborador(conn);
                long informeId;
                String sqlInf = "INSERT INTO informe_final (proceso_id, estado, resumen, recomendacion_general, pdf_ruta, elaborado_por, pdf_nombre, pdf_tamano, emitido_en) "
                        + "VALUES (?, 'EMITIDO', ?, 'Informe ejecutivo emitido por correo', ?, ?, ?, ?, now()) "
                        + "ON CONFLICT (proceso_id) DO UPDATE "
                        + "SET estado = 'EMITIDO', resumen = EXCLUDED.resumen, pdf_ruta = EXCLUDED.pdf_ruta, "
                        + "pdf_nombre = EXCLUDED.pdf_nombre, pdf_tamano = EXCLUDED.pdf_tamano, emitido_en = now() "
                        + "RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sqlInf)) {
                    ps.setLong(1, idProceso);
                    ps.setString(2, resumenEjecutivo);
                    ps.setString(3, rutaPdf);
                    ps.setLong(4, elaboradoPor);
                    ps.setString(5, nombrePdf);
                    ps.setLong(6, tamanoPdf);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        informeId = rs.getLong(1);
                    }
                }

                try (PreparedStatement psProc = conn.prepareStatement(
                        "UPDATE proceso SET estado = 'INFORME_EMITIDO' WHERE id = ?")) {
                    psProc.setLong(1, idProceso);
                    psProc.executeUpdate();
                }

                conn.commit();
                this.id = informeId;
                return informeId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<String[]> listarCandidatosDeInforme(long idProceso) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"POSTULACION_ID", "CANDIDATO", "PUNTAJE", "DICTAMEN", "RECOMENDACION"});

        String sql = "SELECT po.id, "
                + "TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno, '') || ' ' || COALESCE(pe.apellido_materno, '')), "
                + "COALESCE(vp.puntaje_total, 0.00), "
                + "COALESCE(po.etiqueta, 'PENDIENTE'), "
                + "COALESCE(po.recomendacion, '-') "
                + "FROM postulacion po "
                + "JOIN persona pe ON pe.id = po.persona_id "
                + "LEFT JOIN v_puntaje_postulacion vp ON vp.postulacion_id = po.id "
                + "LEFT JOIN informe_final inf ON inf.proceso_id = po.proceso_id "
                + "WHERE po.proceso_id = ? "
                + "ORDER BY COALESCE(vp.puntaje_total, 0.00) DESC, po.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        String.valueOf(rs.getBigDecimal(3)),
                        rs.getString(4),
                        rs.getString(5)
                    });
                }
            }
        }
        return filas;
    }

    public String[] obtenerDatosDespacho(long idProceso) throws SQLException {
        String sql = "SELECT pr.codigo, pr.nombre, COALESCE(e.correo, pe.correo_contacto, ''), "
                + "COALESCE(inf.resumen, 'Sin resumen'), COALESCE(inf.pdf_ruta, '') "
                + "FROM proceso pr "
                + "LEFT JOIN contacto_empresa ce ON ce.id = pr.contacto_id "
                + "LEFT JOIN empresa e ON e.id = ce.empresa_id "
                + "LEFT JOIN persona pe ON pe.id = ce.persona_id "
                + "LEFT JOIN informe_final inf ON inf.proceso_id = pr.id "
                + "WHERE pr.id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("No existe el proceso con ID " + idProceso);
                return new String[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5)
                };
            }
        }
    }

    private String normalizarEtiqueta(String raw) {
        if (raw == null) return "RECOMENDADO";
        String u = raw.trim().toUpperCase().replace(" ", "_");
        return switch (u) {
            case "ALTAMENTE_RECOMENDADO", "RECOMENDADO", "RECOMENDADO_CON_RESERVAS", "NO_RECOMENDADO" -> u;
            default -> "RECOMENDADO";
        };
    }

    private long obtenerUsuarioElaborador(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM usuario ORDER BY id ASC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            throw new SQLException("No hay usuarios registrados para firmar el informe.");
        }
    }
}

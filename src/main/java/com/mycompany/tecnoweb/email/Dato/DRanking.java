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
 * Capa de Datos: Modela ranking_resultado, busqueda aritmetica, calculo de afinidad
 * y matriz comparativa (CU6).
 */
public class DRanking {
    private long id;
    private long procesoId;
    private long postulacionId;
    private int posicion;
    private BigDecimal puntajeTotal;
    private BigDecimal puntajeMinimoFiltro;
    private BigDecimal afinidadMinimaFiltro;
    private boolean esTop;
    private boolean visible;

    public DRanking() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProcesoId() { return procesoId; }
    public void setProcesoId(long procesoId) { this.procesoId = procesoId; }

    public long getPostulacionId() { return postulacionId; }
    public void setPostulacionId(long postulacionId) { this.postulacionId = postulacionId; }

    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }

    public BigDecimal getPuntajeTotal() { return puntajeTotal; }
    public void setPuntajeTotal(BigDecimal puntajeTotal) { this.puntajeTotal = puntajeTotal; }

    public BigDecimal getPuntajeMinimoFiltro() { return puntajeMinimoFiltro; }
    public void setPuntajeMinimoFiltro(BigDecimal puntajeMinimoFiltro) { this.puntajeMinimoFiltro = puntajeMinimoFiltro; }

    public BigDecimal getAfinidadMinimaFiltro() { return afinidadMinimaFiltro; }
    public void setAfinidadMinimaFiltro(BigDecimal afinidadMinimaFiltro) { this.afinidadMinimaFiltro = afinidadMinimaFiltro; }

    public boolean isEsTop() { return esTop; }
    public void setEsTop(boolean esTop) { this.esTop = esTop; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public List<String[]> buscar() throws SQLException {
        return buscarCandidatosAritmetica(this.procesoId, this.puntajeMinimoFiltro, this.afinidadMinimaFiltro);
    }

    public List<String[]> generarRanking() throws SQLException {
        return generarYConsultarRanking(this.procesoId);
    }

    public List<String[]> generarMatriz() throws SQLException {
        return generarMatrizComparativa(this.procesoId);
    }

    private static final String CTE_AFINIDAD = """
            WITH afinidad_cte AS (
                SELECT po.id AS postulacion_id,
                       COALESCE(
                           ROUND(100.0 * (1.0 - SUM(ABS(COALESCE(rd.valor, 0) - pr.valor_ideal)) / NULLIF(10.0 * COUNT(pr.dimension_id), 0)), 2),
                           100.00
                       ) AS afinidad_pct
                  FROM postulacion po
                  LEFT JOIN perfil_requerido pr ON pr.proceso_id = po.proceso_id
                  LEFT JOIN (
                      SELECT r.postulacion_id, rdim.dimension_id, AVG(rdim.valor) AS valor
                        FROM resultado_dimension rdim
                        JOIN resultado_prueba r ON r.id = rdim.resultado_prueba_id
                       GROUP BY r.postulacion_id, rdim.dimension_id
                  ) rd ON rd.postulacion_id = po.id AND rd.dimension_id = pr.dimension_id
                 WHERE po.proceso_id = ?
                 GROUP BY po.id
            )
            """;

    public List<String[]> buscarCandidatosAritmetica(long idProceso, BigDecimal puntajeMin, BigDecimal afinidadMin) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"POSTULACION_ID", "CODIGO", "CANDIDATO", "PUNTAJE_TOTAL", "AFINIDAD_%", "ESTADO"});

        String sql = CTE_AFINIDAD
                + "SELECT po.id, po.codigo, "
                + "TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno, '') || ' ' || COALESCE(pe.apellido_materno, '')), "
                + "COALESCE(vp.puntaje_total, 0.00) AS puntaje, "
                + "COALESCE(af.afinidad_pct, 100.00) AS afinidad, "
                + "po.estado "
                + "FROM postulacion po "
                + "JOIN persona pe ON pe.id = po.persona_id "
                + "LEFT JOIN v_puntaje_postulacion vp ON vp.postulacion_id = po.id "
                + "LEFT JOIN afinidad_cte af ON af.postulacion_id = po.id "
                + "WHERE po.proceso_id = ? "
                + "AND COALESCE(vp.puntaje_total, 0.00) >= ? "
                + "AND COALESCE(af.afinidad_pct, 100.00) >= ? "
                + "ORDER BY puntaje DESC, afinidad DESC, po.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            ps.setLong(2, idProceso);
            ps.setBigDecimal(3, puntajeMin);
            ps.setBigDecimal(4, afinidadMin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        rs.getString(3),
                        String.valueOf(rs.getBigDecimal(4)),
                        String.valueOf(rs.getBigDecimal(5)),
                        rs.getString(6)
                    });
                }
            }
        }
        return filas;
    }

    public List<String[]> generarYConsultarRanking(long idProceso) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"POSICION", "POSTULACION_ID", "CODIGO", "CANDIDATO", "PUNTAJE_TOTAL", "AFINIDAD_%", "ES_TOP"});

        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psDel = conn.prepareStatement(
                        "DELETE FROM ranking_resultado WHERE proceso_id = ?")) {
                    psDel.setLong(1, idProceso);
                    psDel.executeUpdate();
                }

                String sqlIns = "INSERT INTO ranking_resultado (proceso_id, postulacion_id, posicion, puntaje_total, es_top, visible, publicado_en) "
                        + "SELECT po.proceso_id, po.id, "
                        + "ROW_NUMBER() OVER (ORDER BY COALESCE(vp.puntaje_total, 0) DESC, po.id ASC) AS pos, "
                        + "LEAST(100.00, GREATEST(0.00, COALESCE(vp.puntaje_total, 0.00))), "
                        + "(ROW_NUMBER() OVER (ORDER BY COALESCE(vp.puntaje_total, 0) DESC, po.id ASC) <= pr.top_n), "
                        + "FALSE, NULL "
                        + "FROM postulacion po "
                        + "JOIN proceso pr ON pr.id = po.proceso_id "
                        + "LEFT JOIN v_puntaje_postulacion vp ON vp.postulacion_id = po.id "
                        + "WHERE po.proceso_id = ? AND po.estado NOT IN ('NO_APROBADA', 'RETIRADA')";
                try (PreparedStatement psIns = conn.prepareStatement(sqlIns)) {
                    psIns.setLong(1, idProceso);
                    psIns.executeUpdate();
                }

                String sqlSel = CTE_AFINIDAD
                        + "SELECT rr.posicion, rr.postulacion_id, po.codigo, "
                        + "TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno, '') || ' ' || COALESCE(pe.apellido_materno, '')), "
                        + "rr.puntaje_total, COALESCE(af.afinidad_pct, 100.00), "
                        + "CASE WHEN rr.es_top THEN 'SI' ELSE 'NO' END "
                        + "FROM ranking_resultado rr "
                        + "JOIN postulacion po ON po.id = rr.postulacion_id "
                        + "JOIN persona pe ON pe.id = po.persona_id "
                        + "LEFT JOIN afinidad_cte af ON af.postulacion_id = po.id "
                        + "WHERE rr.proceso_id = ? "
                        + "ORDER BY rr.posicion ASC";
                try (PreparedStatement psSel = conn.prepareStatement(sqlSel)) {
                    psSel.setLong(1, idProceso);
                    psSel.setLong(2, idProceso);
                    try (ResultSet rs = psSel.executeQuery()) {
                        while (rs.next()) {
                            filas.add(new String[]{
                                String.valueOf(rs.getInt(1)),
                                String.valueOf(rs.getLong(2)),
                                rs.getString(3),
                                rs.getString(4),
                                String.valueOf(rs.getBigDecimal(5)),
                                String.valueOf(rs.getBigDecimal(6)),
                                rs.getString(7)
                            });
                        }
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return filas;
    }

    public List<String[]> generarMatrizComparativa(long idProceso) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{
            "POSTULACION_ID", "CANDIDATO", "DETALLE_PRUEBAS", "PERFIL_DIMENSIONES_OBT_VS_IDEAL", "PUNTAJE_TOTAL", "AFINIDAD_%"
        });

        String sql = CTE_AFINIDAD
                + "SELECT po.id, "
                + "TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno, '')), "
                + "COALESCE((SELECT STRING_AGG(p.codigo || ':' || r.puntaje_normalizado, ' | ' ORDER BY p.codigo) "
                + "          FROM resultado_prueba r JOIN proceso_prueba pp ON pp.id = r.proceso_prueba_id "
                + "          JOIN prueba p ON p.id = pp.prueba_id WHERE r.postulacion_id = po.id), 'SIN_PRUEBAS'), "
                + "COALESCE((SELECT STRING_AGG(d.codigo || ':' || COALESCE(ROUND(rd.val, 1)::text, 'PEND') || '/' || prq.valor_ideal, ' | ' ORDER BY d.orden) "
                + "          FROM perfil_requerido prq JOIN dimension_evaluada d ON d.id = prq.dimension_id "
                + "          LEFT JOIN (SELECT r2.postulacion_id, rdim2.dimension_id, AVG(rdim2.valor) AS val "
                + "                     FROM resultado_dimension rdim2 JOIN resultado_prueba r2 ON r2.id = rdim2.resultado_prueba_id "
                + "                     GROUP BY r2.postulacion_id, rdim2.dimension_id) rd "
                + "            ON rd.postulacion_id = po.id AND rd.dimension_id = prq.dimension_id "
                + "          WHERE prq.proceso_id = po.proceso_id), 'SIN_PERFIL_REQ'), "
                + "COALESCE(vp.puntaje_total, 0.00), "
                + "COALESCE(af.afinidad_pct, 100.00) "
                + "FROM postulacion po "
                + "JOIN persona pe ON pe.id = po.persona_id "
                + "LEFT JOIN v_puntaje_postulacion vp ON vp.postulacion_id = po.id "
                + "LEFT JOIN afinidad_cte af ON af.postulacion_id = po.id "
                + "WHERE po.proceso_id = ? "
                + "ORDER BY COALESCE(vp.puntaje_total, 0.00) DESC, po.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            ps.setLong(2, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        String.valueOf(rs.getBigDecimal(5)),
                        String.valueOf(rs.getBigDecimal(6))
                    });
                }
            }
        }
        return filas;
    }
}

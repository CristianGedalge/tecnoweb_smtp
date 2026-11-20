package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela los reportes y estadisticas sobre las vistas
 * v_embudo_proceso, v_bandeja_tareas, v_puntaje_postulacion y tablas base (CU8).
 */
public class DReporte {
    private String tipoReporte;
    private long procesoId;
    private String filtroProceso;

    public DReporte() {
    }

    public DReporte(String tipoReporte, String filtroProceso) {
        this.tipoReporte = tipoReporte;
        this.filtroProceso = filtroProceso;
    }

    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }

    public long getProcesoId() { return procesoId; }
    public void setProcesoId(long procesoId) { this.procesoId = procesoId; }

    public String getFiltroProceso() { return filtroProceso; }
    public void setFiltroProceso(String filtroProceso) { this.filtroProceso = filtroProceso; }

    public List<String[]> reporteEmbudo(long idProceso) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ORDEN", "CODIGO_ETAPA", "NOMBRE_ETAPA", "APROBADAS", "EN_CURSO", "REPROBADAS"});

        String sql = "SELECT orden, etapa, etapa_nombre, aprobadas, en_curso, reprobadas "
                + "FROM v_embudo_proceso WHERE proceso_id = ? ORDER BY orden ASC";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        rs.getString(2),
                        rs.getString(3),
                        String.valueOf(rs.getLong(4)),
                        String.valueOf(rs.getLong(5)),
                        String.valueOf(rs.getLong(6))
                    });
                }
            }
        }
        return filas;
    }

    public List<String[]> reporteBandejaTareas(String filtroProcesoId) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"TAREA_ID", "PROCESO", "CANDIDATO", "ETAPA", "ESTADO", "DIAS_PENDIENTE"});

        boolean todos = filtroProcesoId == null || filtroProcesoId.isBlank() || filtroProcesoId.equals("*");
        String sql = "SELECT bt.tarea_id, bt.codigo_proceso, bt.codigo_candidato, bt.etapa, bt.estado, bt.dias_pendiente "
                + "FROM v_bandeja_tareas bt "
                + (todos ? "" : "JOIN proceso pr ON pr.codigo = bt.codigo_proceso WHERE pr.id = ? ")
                + "ORDER BY bt.dias_pendiente DESC, bt.tarea_id ASC";

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
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        String.valueOf(rs.getInt(6))
                    });
                }
            }
        }
        return filas;
    }

    public List<String[]> reportePuntajes(long idProceso) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"POSTULACION_ID", "CODIGO", "CANDIDATO", "PUNTAJE_TOTAL", "PRUEBAS_CALIFICADAS", "PRUEBAS_CONFIGURADAS"});

        String sql = "SELECT vp.postulacion_id, po.codigo, "
                + "TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno, '')), "
                + "vp.puntaje_total, vp.pruebas_calificadas, vp.pruebas_configuradas "
                + "FROM v_puntaje_postulacion vp "
                + "JOIN postulacion po ON po.id = vp.postulacion_id "
                + "JOIN persona pe ON pe.id = po.persona_id "
                + "WHERE vp.proceso_id = ? "
                + "ORDER BY vp.puntaje_total DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProceso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        rs.getString(3),
                        String.valueOf(rs.getBigDecimal(4)),
                        String.valueOf(rs.getInt(5)),
                        String.valueOf(rs.getInt(6))
                    });
                }
            }
        }
        return filas;
    }

    public List<String[]> reporteEstadisticasGenerales() throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"INDICADOR", "VALOR"});

        String sql = """
                SELECT 'Total Personas Registradas', COUNT(*)::text FROM persona
                UNION ALL
                SELECT 'Total Usuarios Activos', COUNT(*)::text FROM usuario WHERE estado = 'ACTIVO'
                UNION ALL
                SELECT 'Total Empresas Clientes', COUNT(*)::text FROM empresa
                UNION ALL
                SELECT 'Total Procesos / Cargos', COUNT(*)::text FROM proceso
                UNION ALL
                SELECT 'Total Postulaciones', COUNT(*)::text FROM postulacion
                UNION ALL
                SELECT 'Postulaciones en Proceso', COUNT(*)::text FROM postulacion WHERE estado = 'EN_PROCESO'
                UNION ALL
                SELECT 'Postulaciones Aprobadas', COUNT(*)::text FROM postulacion WHERE estado = 'APROBADA'
                UNION ALL
                SELECT 'Postulaciones No Aprobadas', COUNT(*)::text FROM postulacion WHERE estado = 'NO_APROBADA'
                UNION ALL
                SELECT 'Promedio General de Puntajes', COALESCE(ROUND(AVG(puntaje_total), 2)::text, '0.00') FROM v_puntaje_postulacion
                UNION ALL
                SELECT 'Informes Finales Emitidos', COUNT(*)::text FROM informe_final WHERE estado = 'EMITIDO'
                """;
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new String[]{rs.getString(1), rs.getString(2)});
            }
        }
        return filas;
    }
}

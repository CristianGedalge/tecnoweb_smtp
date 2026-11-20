package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela proceso, dimension_evaluada y perfil_requerido (CU3).
 */
public class DCargo {
    private long procesoId;
    private long empresaId;
    private long contactoId;
    private String codigoProceso;
    private String tipoProceso = "EMPRESA";
    private String cargo;
    private String area = "General";
    private String nivelJerarquico = "TECNICO";
    private int vacantes;
    private int topN;
    private String descripcionFunciones;
    private String estado = "ABIERTO";
    private String codigoDimension;
    private int valorIdeal;

    public DCargo() {
    }

    public DCargo(long empresaId, String codigoProceso, String cargo, String descripcionFunciones, int vacantes, int topN) {
        this.empresaId = empresaId;
        this.codigoProceso = codigoProceso;
        this.cargo = cargo;
        this.descripcionFunciones = descripcionFunciones;
        this.vacantes = vacantes;
        this.topN = topN;
    }

    public long getProcesoId() { return procesoId; }
    public void setProcesoId(long procesoId) { this.procesoId = procesoId; }

    public long getEmpresaId() { return empresaId; }
    public void setEmpresaId(long empresaId) { this.empresaId = empresaId; }

    public long getContactoId() { return contactoId; }
    public void setContactoId(long contactoId) { this.contactoId = contactoId; }

    public String getCodigoProceso() { return codigoProceso; }
    public void setCodigoProceso(String codigoProceso) { this.codigoProceso = codigoProceso; }

    public String getTipoProceso() { return tipoProceso; }
    public void setTipoProceso(String tipoProceso) { this.tipoProceso = tipoProceso; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getNivelJerarquico() { return nivelJerarquico; }
    public void setNivelJerarquico(String nivelJerarquico) { this.nivelJerarquico = nivelJerarquico; }

    public int getVacantes() { return vacantes; }
    public void setVacantes(int vacantes) { this.vacantes = vacantes; }

    public int getTopN() { return topN; }
    public void setTopN(int topN) { this.topN = topN; }

    public String getDescripcionFunciones() { return descripcionFunciones; }
    public void setDescripcionFunciones(String descripcionFunciones) { this.descripcionFunciones = descripcionFunciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCodigoDimension() { return codigoDimension; }
    public void setCodigoDimension(String codigoDimension) { this.codigoDimension = codigoDimension; }

    public int getValorIdeal() { return valorIdeal; }
    public void setValorIdeal(int valorIdeal) { this.valorIdeal = valorIdeal; }

    public long insertar() throws SQLException {
        return insertarCargo(this.empresaId, this.codigoProceso, this.cargo,
                this.descripcionFunciones, this.vacantes, this.topN);
    }

    public void guardarPerfilRequerido() throws SQLException {
        definirPerfilRequerido(this.procesoId, this.codigoDimension, this.valorIdeal);
    }

    public List<String[]> listarCargos(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_PROCESO", "CODIGO", "EMPRESA", "CARGO", "NIVEL", "VACANTES", "TOP_N", "ESTADO"});

        boolean todos = filtro == null || filtro.isBlank() || filtro.equals("*");
        String sql = "SELECT p.id, p.codigo, COALESCE(e.razon_social, 'UAGRM'), "
                + "COALESCE(p.cargo, p.nombre), COALESCE(p.nivel_jerarquico, 'TECNICO'), "
                + "COALESCE(p.vacantes, 1), p.top_n, p.estado "
                + "FROM proceso p "
                + "LEFT JOIN contacto_empresa ce ON ce.id = p.contacto_id "
                + "LEFT JOIN empresa e ON e.id = ce.empresa_id "
                + (todos ? "" : "WHERE p.codigo ILIKE ? OR p.nombre ILIKE ? ")
                + "ORDER BY p.id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                String patron = "%" + filtro.trim() + "%";
                ps.setString(1, patron);
                ps.setString(2, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        String.valueOf(rs.getInt(6)),
                        String.valueOf(rs.getInt(7)),
                        rs.getString(8)
                    });
                }
            }
        }
        return filas;
    }

    public long insertarCargo(long idEmpresa, String codigoProceso, String cargo,
                              String funciones, int vacantes, int topN) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long idContacto = obtenerContactoEmpresa(conn, idEmpresa);
                long creadoPor = obtenerUsuarioCreador(conn);

                long idProc;
                try (PreparedStatement psProc = conn.prepareStatement(
                        "INSERT INTO proceso (codigo, tipo, nombre, estado, top_n, creado_por) "
                                + "VALUES (UPPER(?), 'EMPRESA', ?, 'ABIERTO', ?, ?) RETURNING id")) {
                    psProc.setString(1, codigoProceso.trim());
                    psProc.setString(2, cargo.trim());
                    psProc.setInt(3, Math.max(1, Math.min(50, topN)));
                    psProc.setLong(4, creadoPor);
                    try (ResultSet rs = psProc.executeQuery()) {
                        rs.next();
                        idProc = rs.getLong(1);
                    }
                }

                try (PreparedStatement psSol = conn.prepareStatement(
                        "UPDATE proceso SET contacto_id = ?, cargo = ?, area = 'General', "
                        + "vacantes = ?, descripcion_funciones = ? WHERE id = ?")) {
                    psSol.setLong(1, idContacto);
                    psSol.setString(2, cargo.trim());
                    psSol.setInt(3, vacantes);
                    psSol.setString(4, funciones.trim());
                    psSol.setLong(5, idProc);
                    psSol.executeUpdate();
                }
                crearEtapas(conn, idProc);

                conn.commit();
                this.procesoId = idProc;
                this.contactoId = idContacto;
                return idProc;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void crearEtapas(Connection conn, long idProceso) throws SQLException {
        String[][] etapas = {
            {"INSCRIPCION", "Inscripcion", "AUTOMATICA"},
            {"REQUISITOS", "Revision de documentos", "MANUAL"},
            {"ANALISIS_CV", "Analisis de CV", "MANUAL"},
            {"EVALUACION_TECNICA", "Evaluacion tecnica", "AUTOMATICA"},
            {"EVALUACION_PSICOLOGICA", "Evaluacion psicologica", "AUTOMATICA"},
            {"HABILIDADES_BLANDAS", "Habilidades blandas", "AUTOMATICA"},
            {"ENTREVISTA", "Entrevista", "MANUAL"},
            {"INVESTIGACION_ANTECEDENTES", "Antecedentes", "MANUAL"},
            {"SELECCION", "Seleccion", "MANUAL"},
            {"INFORME", "Informe", "MANUAL"}
        };
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO proceso_etapa (proceso_id,codigo,nombre,ejecucion,orden,es_eliminatoria) VALUES (?,?,?,?,?,?)")) {
            for (int i = 0; i < etapas.length; i++) {
                ps.setLong(1,idProceso);
                ps.setString(2,etapas[i][0]);
                ps.setString(3,etapas[i][1]);
                ps.setString(4,etapas[i][2]);
                ps.setInt(5,i+1);
                ps.setBoolean(6,i < 8);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void definirPerfilRequerido(long idProceso, String codigoDimension, int valorIdeal) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            int dimensionId;
            try (PreparedStatement psDim = conn.prepareStatement(
                    "SELECT id FROM dimension_evaluada WHERE UPPER(codigo) = UPPER(?)")) {
                psDim.setString(1, codigoDimension.trim());
                try (ResultSet rs = psDim.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Dimension no encontrada: " + codigoDimension);
                    }
                    dimensionId = rs.getInt(1);
                }
            }

            String sql = "INSERT INTO perfil_requerido (proceso_id, dimension_id, valor_ideal) "
                    + "VALUES (?, ?, ?) "
                    + "ON CONFLICT (proceso_id, dimension_id) DO UPDATE SET valor_ideal = EXCLUDED.valor_ideal";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, idProceso);
                ps.setInt(2, dimensionId);
                ps.setInt(3, valorIdeal);
                ps.executeUpdate();
            }
        }
    }

    private long obtenerContactoEmpresa(Connection conn, long idEmpresa) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM contacto_empresa WHERE empresa_id = ? ORDER BY es_principal DESC, id ASC LIMIT 1")) {
            ps.setLong(1, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("La empresa ID " + idEmpresa + " no existe o no tiene contacto registrado.");
            }
        }
    }

    private long obtenerUsuarioCreador(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT u.id FROM usuario u JOIN rol r ON r.id=u.rol_id WHERE r.codigo='ADMIN' AND u.estado='ACTIVO' ORDER BY u.id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        throw new SQLException("Registre primero un usuario ADMIN activo con INSUSU.");
    }
}

package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Consentimientos; version de politica configurada en properties (CU2).
 */
public class DConsentimiento {
    private long id;
    private long personaId;
    private String tipo;
    private String versionPolitica;
    private boolean otorgado;
    private String ipOrigen;

    public DConsentimiento() {
    }

    public DConsentimiento(long id, long personaId, String tipo, String versionPolitica, boolean otorgado) {
        this.id = id;
        this.personaId = personaId;
        this.tipo = tipo;
        this.versionPolitica = versionPolitica;
        this.otorgado = otorgado;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPersonaId() { return personaId; }
    public void setPersonaId(long personaId) { this.personaId = personaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getVersionPolitica() { return versionPolitica; }
    public void setVersionPolitica(String versionPolitica) { this.versionPolitica = versionPolitica; }

    public boolean isOtorgado() { return otorgado; }
    public void setOtorgado(boolean otorgado) { this.otorgado = otorgado; }

    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }

    public long insertar() throws SQLException {
        return insertar(this.personaId, this.tipo, this.otorgado);
    }

    public long revocar() throws SQLException {
        return revocar(this.id);
    }

    public List<String[]> listar(String filtroPersonaId) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID", "PERSONA_ID", "NOMBRE_PERSONA", "TIPO", "VERSION", "OTORGADO", "REGISTRADO_EN"});

        boolean todos = filtroPersonaId == null || filtroPersonaId.isBlank() || filtroPersonaId.equals("*");
        String sql = "SELECT c.id, c.persona_id, "
                + "TRIM(p.nombres || ' ' || COALESCE(p.apellido_paterno, '')), "
                + "c.tipo, c.version_politica, CASE WHEN c.otorgado THEN 'SI' ELSE 'NO' END, "
                + "TO_CHAR(c.registrado_en, 'YYYY-MM-DD HH24:MI') "
                + "FROM consentimiento c "
                + "JOIN persona p ON p.id = c.persona_id "
                + (todos ? "" : "WHERE c.persona_id = ? ")
                + "ORDER BY c.id DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setLong(1, Long.parseLong(filtroPersonaId.trim()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        String.valueOf(rs.getLong(2)),
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

    public long insertar(long idPersona, String tipoConsentimiento, boolean otorgado) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            String version = obtenerVersionPolitica(conn);
            String sql = "INSERT INTO consentimiento (persona_id, tipo, version_politica, otorgado) "
                    + "VALUES (?, UPPER(?), ?, ?) RETURNING id";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, idPersona);
                ps.setString(2, tipoConsentimiento.trim());
                ps.setString(3, version);
                ps.setBoolean(4, otorgado);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getLong(1);
                        return this.id;
                    }
                    throw new SQLException("No se pudo registrar el consentimiento.");
                }
            }
        }
    }

    public long revocar(long idConsentimiento) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            long pId;
            String t;
            String ver;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT persona_id, tipo, version_politica FROM consentimiento WHERE id = ?")) {
                ps.setLong(1, idConsentimiento);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No existe el consentimiento con ID " + idConsentimiento);
                    }
                    pId = rs.getLong(1);
                    t = rs.getString(2);
                    ver = rs.getString(3);
                }
            }
            try (PreparedStatement psIns = conn.prepareStatement(
                    "INSERT INTO consentimiento (persona_id, tipo, version_politica, otorgado) VALUES (?, ?, ?, FALSE) RETURNING id")) {
                psIns.setLong(1, pId);
                psIns.setString(2, t);
                psIns.setString(3, ver);
                try (ResultSet rs = psIns.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getLong(1);
                        return this.id;
                    }
                    throw new SQLException("No se pudo registrar la revocacion del consentimiento.");
                }
            }
        }
    }

    private String obtenerVersionPolitica(Connection conn) throws SQLException {
        return ConexionDB.versionPolitica();
    }
}

package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Capa de Datos: Modela las tablas usuario y rol (CU1).
 */
public class DUsuario {
    private long id;
    private long personaId;
    private int rolId;
    private String codigoRol;
    private String correo;
    private String password;
    private String estado;

    public DUsuario() {
    }

    public DUsuario(long id, long personaId, String codigoRol, String correo, String password, String estado) {
        this.id = id;
        this.personaId = personaId;
        this.codigoRol = codigoRol;
        this.correo = correo;
        this.password = password;
        this.estado = estado;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPersonaId() { return personaId; }
    public void setPersonaId(long personaId) { this.personaId = personaId; }

    public int getRolId() { return rolId; }
    public void setRolId(int rolId) { this.rolId = rolId; }

    public String getCodigoRol() { return codigoRol; }
    public void setCodigoRol(String codigoRol) { this.codigoRol = codigoRol; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public long insertar() throws SQLException {
        return insertarUsuario(this.personaId, this.codigoRol, this.correo, this.password);
    }

    public boolean modificar() throws SQLException {
        return modificarUsuario(this.id, this.codigoRol, this.correo);
    }

    public boolean eliminar() throws SQLException {
        return desactivarUsuario(this.id);
    }

    public List<String[]> listarRoles() throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID", "CODIGO", "NOMBRE", "DESCRIPCION"});
        String sql = "SELECT id, codigo, nombre, COALESCE(descripcion, '') FROM rol ORDER BY id ASC";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new String[]{
                    String.valueOf(rs.getInt(1)),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4)
                });
            }
        }
        return filas;
    }

    public List<String[]> listarUsuarios(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_USUARIO", "CORREO", "ROL", "ESTADO", "PERSONA_ID", "NOMBRE_COMPLETO", "CONDICION"});

        boolean todos = filtro == null || filtro.isBlank() || filtro.equals("*");
        String sql = "SELECT u.id, u.correo, r.codigo, u.estado, COALESCE(p.id::text, '-'), "
                + "TRIM(COALESCE(p.nombres, '') || ' ' || COALESCE(p.apellido_paterno, '') || ' ' || COALESCE(p.apellido_materno, '')), "
                + "CASE WHEN EXISTS (SELECT 1 FROM v_reclutados vr WHERE vr.persona_id=p.id) THEN 'RECLUTADO' ELSE 'SIN_PROCESO_APROBADO' END "
                + "FROM usuario u "
                + "JOIN rol r ON r.id = u.rol_id "
                + "LEFT JOIN persona p ON p.usuario_id = u.id "
                + (todos ? "" : "WHERE u.correo ILIKE ? OR r.codigo ILIKE ? ")
                + "ORDER BY u.id ASC";

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
                        rs.getString(6),
                        rs.getString(7)
                    });
                }
            }
        }
        return filas;
    }

    public long insertarUsuario(long idPersona, String codigoRol, String correo, String password) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                int rolIdObtenido = obtenerRolId(conn, codigoRol);
                String hash = hashPassword(password);
                long idUsuario;

                String sqlUsu = "INSERT INTO usuario (rol_id, correo, password_hash, estado) "
                        + "VALUES (?, LOWER(?), ?, 'ACTIVO') RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sqlUsu)) {
                    ps.setInt(1, rolIdObtenido);
                    ps.setString(2, correo.trim());
                    ps.setString(3, hash);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("No se pudo crear el usuario.");
                        idUsuario = rs.getLong(1);
                    }
                }

                if (idPersona > 0) {
                    try (PreparedStatement psPer = conn.prepareStatement(
                            "UPDATE persona SET usuario_id = ?, correo_contacto = COALESCE(correo_contacto, LOWER(?)) WHERE id = ?")) {
                        psPer.setLong(1, idUsuario);
                        psPer.setString(2, correo.trim());
                        psPer.setLong(3, idPersona);
                        if (psPer.executeUpdate() == 0) {
                            throw new SQLException("No existe la persona con ID " + idPersona);
                        }
                    }
                }

                conn.commit();
                this.id = idUsuario;
                return idUsuario;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean modificarUsuario(long idUsuario, String codigoRol, String correo) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            int rolIdObtenido = obtenerRolId(conn, codigoRol);
            String sql = "UPDATE usuario SET rol_id = ?, correo = LOWER(?) WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, rolIdObtenido);
                ps.setString(2, correo.trim());
                ps.setLong(3, idUsuario);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean desactivarUsuario(long idUsuario) throws SQLException {
        String sql = "UPDATE usuario SET estado = 'INACTIVO' WHERE id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    private int obtenerRolId(Connection conn, String codigoRol) throws SQLException {
        if ("RECLUTADO".equalsIgnoreCase(codigoRol.trim())) {
            throw new SQLException("Reclutado es una condicion obtenida al aprobar el proceso. Use CANDIDATO y FINPOS.");
        }
        if (java.util.Set.of("EGRESADO", "CANDIDATO_EXTERNO").contains(codigoRol.trim().toUpperCase())) codigoRol = "CANDIDATO";
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM rol WHERE UPPER(codigo) = UPPER(?)")) {
            ps.setString(1, codigoRol.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Rol no valido: " + codigoRol + ". Use LISROL[\"*\"] para ver roles.");
            }
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return password;
        }
    }
}

package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela la tabla persona (CU1).
 */
public class DPersona {
    private long id;
    private Long usuarioId;
    private String ciNumero;
    private String ciComplemento;
    private String ciExpedido;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Date fechaNacimiento;
    private String telefono;
    private String correoContacto;
    private String ocupacion;
    private String telefonoFijo;

    public void setOcupacion(String valor) { ocupacion=valor; }
    public void setTelefonoFijo(String valor) { telefonoFijo=valor; }

    public DPersona() {
    }

    public DPersona(long id, String ciNumero, String ciComplemento, String ciExpedido,
                    String nombres, String apellidoPaterno, String apellidoMaterno,
                    Date fechaNacimiento, String telefono, String correoContacto) {
        this.id = id;
        this.ciNumero = ciNumero;
        this.ciComplemento = ciComplemento;
        this.ciExpedido = ciExpedido;
        this.nombres = nombres;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.correoContacto = correoContacto;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getCiNumero() { return ciNumero; }
    public void setCiNumero(String ciNumero) { this.ciNumero = ciNumero; }

    public String getCiComplemento() { return ciComplemento; }
    public void setCiComplemento(String ciComplemento) { this.ciComplemento = ciComplemento; }

    public String getCiExpedido() { return ciExpedido; }
    public void setCiExpedido(String ciExpedido) { this.ciExpedido = ciExpedido; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }

    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }

    public Date getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(Date fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreoContacto() { return correoContacto; }
    public void setCorreoContacto(String correoContacto) { this.correoContacto = correoContacto; }

    public long insertar() throws SQLException {
        return insertar(this.ciNumero, this.ciComplemento, this.ciExpedido,
                this.nombres, this.apellidoPaterno, this.apellidoMaterno,
                this.fechaNacimiento, this.telefono, this.correoContacto);
    }

    public boolean modificar() throws SQLException {
        return modificar(this.id, this.ciNumero, this.nombres,
                this.apellidoPaterno, this.apellidoMaterno, this.telefono);
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID", "CI", "EXP", "NOMBRES", "APELLIDOS", "FECHA_NAC", "CELULAR", "CORREO", "OCUPACION", "TELEFONO_FIJO"});

        boolean todos = filtro == null || filtro.isBlank() || filtro.equals("*");
        String sql = "SELECT id, ci_numero, ci_complemento, COALESCE(ci_expedido, ''), nombres, "
                + "COALESCE(apellido_paterno, ''), COALESCE(apellido_materno, ''), "
                + "fecha_nacimiento, COALESCE(telefono, ''), COALESCE(correo_contacto, ''), COALESCE(ocupacion,''), COALESCE(telefono_fijo,'') "
                + "FROM persona "
                + (todos ? "" : "WHERE ci_numero ILIKE ? OR nombres ILIKE ? OR apellido_paterno ILIKE ? ")
                + "ORDER BY id ASC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                String patron = "%" + filtro.trim() + "%";
                ps.setString(1, patron);
                ps.setString(2, patron);
                ps.setString(3, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ci = rs.getString(2) + (rs.getString(3).isBlank() ? "" : "-" + rs.getString(3));
                    String apellidos = (rs.getString(6) + " " + rs.getString(7)).trim();
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        ci,
                        rs.getString(4),
                        rs.getString(5),
                        apellidos,
                        rs.getDate(8)==null ? "" : rs.getDate(8).toString(),
                        rs.getString(9),
                        rs.getString(10),
                        rs.getString(11),
                        rs.getString(12)
                    });
                }
            }
        }
        return filas;
    }

    public long insertar(String ciNumero, String ciComplemento, String ciExpedido,
                         String nombres, String apellidoPaterno, String apellidoMaterno,
                         Date fechaNacimiento, String telefono, String correoContacto) throws SQLException {
        String sql = "INSERT INTO persona (ci_numero, ci_complemento, ci_expedido, nombres, "
                + "apellido_paterno, apellido_materno, fecha_nacimiento, telefono, correo_contacto, ocupacion, telefono_fijo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ciNumero);
            ps.setString(2, ciComplemento == null ? "" : ciComplemento);
            ps.setString(3, ciExpedido);
            ps.setString(4, nombres);
            ps.setString(5, apellidoPaterno);
            ps.setString(6, apellidoMaterno);
            ps.setDate(7, fechaNacimiento);
            ps.setString(8, telefono);
            ps.setString(9, correoContacto);
            ps.setString(10, ocupacion);
            ps.setString(11, telefonoFijo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.id = rs.getLong(1);
                    return this.id;
                }
                throw new SQLException("No se pudo obtener el ID de la persona insertada.");
            }
        }
    }

    public boolean modificar(long idPersona, String ciNumero, String nombres,
                             String apellidoPaterno, String apellidoMaterno, String telefono) throws SQLException {
        String sql = "UPDATE persona SET ci_numero = ?, nombres = ?, apellido_paterno = ?, "
                + "apellido_materno = ?, telefono = ? WHERE id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ciNumero);
            ps.setString(2, nombres);
            ps.setString(3, apellidoPaterno);
            ps.setString(4, apellidoMaterno);
            ps.setString(5, telefono);
            ps.setLong(6, idPersona);
            return ps.executeUpdate() > 0;
        }
    }
}

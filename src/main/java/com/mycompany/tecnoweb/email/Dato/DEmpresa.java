package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de Datos: Modela las tablas empresa y contacto_empresa (CU3).
 */
public class DEmpresa {
    private long id;
    private String nit;
    private String razonSocial;
    private String direccion;
    private String correo;
    private String telefono;
    private String estado;
    private String nombreContacto;
    private String cargoContacto;

    public DEmpresa() {
    }

    public DEmpresa(long id, String nit, String razonSocial, String direccion,
                    String nombreContacto, String cargoContacto, String correo, String telefono) {
        this.id = id;
        this.nit = nit;
        this.razonSocial = razonSocial;
        this.direccion = direccion;
        this.nombreContacto = nombreContacto;
        this.cargoContacto = cargoContacto;
        this.correo = correo;
        this.telefono = telefono;
        this.estado = "ACTIVA";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getCargoContacto() { return cargoContacto; }
    public void setCargoContacto(String cargoContacto) { this.cargoContacto = cargoContacto; }

    public long insertar() throws SQLException {
        return insertar(this.nit, this.razonSocial, this.direccion,
                this.nombreContacto, this.cargoContacto, this.correo, this.telefono);
    }

    public boolean modificar() throws SQLException {
        return modificar(this.id, this.razonSocial, this.direccion, this.correo, this.telefono);
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID", "NIT", "RAZON_SOCIAL", "DIRECCION", "CORREO", "TELEFONO", "CONTACTO", "ESTADO"});

        boolean todos = filtro == null || filtro.isBlank() || filtro.equals("*");
        String sql = "SELECT e.id, e.nit, e.razon_social, COALESCE(e.direccion, ''), e.correo, "
                + "COALESCE(e.telefono, ''), "
                + "COALESCE(TRIM(p.nombres || ' ' || COALESCE(p.apellido_paterno, '')) || ' (' || ce.cargo || ')', '-'), "
                + "e.estado "
                + "FROM empresa e "
                + "LEFT JOIN contacto_empresa ce ON ce.empresa_id = e.id AND ce.es_principal = TRUE "
                + "LEFT JOIN persona p ON p.id = ce.persona_id "
                + (todos ? "" : "WHERE e.nit ILIKE ? OR e.razon_social ILIKE ? ")
                + "ORDER BY e.id ASC";

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
                        rs.getString(7),
                        rs.getString(8)
                    });
                }
            }
        }
        return filas;
    }

    public long insertar(String nit, String razonSocial, String direccionRubro,
                         String nombreContacto, String cargoContacto, String correoContacto, String telefono) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                long idEmpresa;
                String sqlEmp = "INSERT INTO empresa (nit, razon_social, direccion, correo, telefono, estado) "
                        + "VALUES (?, ?, ?, LOWER(?), ?, 'ACTIVA') RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sqlEmp)) {
                    ps.setString(1, nit.trim());
                    ps.setString(2, razonSocial.trim());
                    ps.setString(3, direccionRubro.trim());
                    ps.setString(4, correoContacto.trim());
                    ps.setString(5, telefono.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        idEmpresa = rs.getLong(1);
                    }
                }

                String[] partes = nombreContacto.trim().split("\\s+", 2);
                String nombres = partes[0];
                String apellido = partes.length > 1 ? partes[1] : "Contacto";
                String ciContacto = "E" + nit.trim();
                if (ciContacto.length() > 15) ciContacto = ciContacto.substring(0, 15);

                long personaId;
                try (PreparedStatement psBus = conn.prepareStatement(
                        "SELECT id FROM persona WHERE ci_numero = ?")) {
                    psBus.setString(1, ciContacto);
                    try (ResultSet rs = psBus.executeQuery()) {
                        if (rs.next()) {
                            personaId = rs.getLong(1);
                        } else {
                            try (PreparedStatement psPer = conn.prepareStatement(
                                    "INSERT INTO persona (ci_numero, ci_complemento, ci_expedido, nombres, apellido_paterno, fecha_nacimiento, telefono, correo_contacto) "
                                            + "VALUES (?, '', 'SC', ?, ?, ?, ?, LOWER(?)) RETURNING id")) {
                                psPer.setString(1, ciContacto);
                                psPer.setString(2, nombres);
                                psPer.setString(3, apellido);
                                psPer.setDate(4, Date.valueOf(LocalDate.of(1990, 1, 1)));
                                psPer.setString(5, telefono.trim());
                                psPer.setString(6, correoContacto.trim());
                                try (ResultSet rsPer = psPer.executeQuery()) {
                                    rsPer.next();
                                    personaId = rsPer.getLong(1);
                                }
                            }
                        }
                    }
                }

                try (PreparedStatement psCon = conn.prepareStatement(
                        "INSERT INTO contacto_empresa (empresa_id, persona_id, cargo, es_principal, activo) "
                                + "VALUES (?, ?, ?, TRUE, TRUE)")) {
                    psCon.setLong(1, idEmpresa);
                    psCon.setLong(2, personaId);
                    psCon.setString(3, cargoContacto.trim());
                    psCon.executeUpdate();
                }

                conn.commit();
                this.id = idEmpresa;
                return idEmpresa;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean modificar(long idEmpresa, String razonSocial, String direccionRubro,
                             String correoContacto, String telefono) throws SQLException {
        String sql = "UPDATE empresa SET razon_social = ?, direccion = ?, correo = LOWER(?), telefono = ? WHERE id = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, razonSocial.trim());
            ps.setString(2, direccionRubro.trim());
            ps.setString(3, correoContacto.trim());
            ps.setString(4, telefono.trim());
            ps.setLong(5, idEmpresa);
            return ps.executeUpdate() > 0;
        }
    }
}

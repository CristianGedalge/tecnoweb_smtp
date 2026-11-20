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
 * Capa de Datos: Documento y metadatos del adjunto en una sola tabla (CU2).
 */
public class DDocumento {
    private long id;
    private long postulacionId;
    private String codigoTipoDoc;
    private String nombreOriginal;
    private String claveAlmacenamiento;
    private String mimeType;
    private long tamanoBytes;
    private String estado;
    private boolean esVigente;
    private String observacion;

    public DDocumento() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPostulacionId() { return postulacionId; }
    public void setPostulacionId(long postulacionId) { this.postulacionId = postulacionId; }


    public String getCodigoTipoDoc() { return codigoTipoDoc; }
    public void setCodigoTipoDoc(String codigoTipoDoc) { this.codigoTipoDoc = codigoTipoDoc; }


    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getClaveAlmacenamiento() { return claveAlmacenamiento; }
    public void setClaveAlmacenamiento(String claveAlmacenamiento) { this.claveAlmacenamiento = claveAlmacenamiento; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean isEsVigente() { return esVigente; }
    public void setEsVigente(boolean esVigente) { this.esVigente = esVigente; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public long insertar() throws SQLException {
        return registrarDocumento(this.postulacionId, this.codigoTipoDoc,
                this.nombreOriginal, this.claveAlmacenamiento, this.mimeType, this.tamanoBytes);
    }

    public List<String[]> listarDocumentosPostulacion(String filtroPostulacionId) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"ID_DOC", "POSTULACION_ID", "TIPO_DOCUMENTO", "ARCHIVO", "ESTADO", "VIGENTE"});

        boolean todos = filtroPostulacionId == null || filtroPostulacionId.isBlank() || filtroPostulacionId.equals("*");
        String sql = "SELECT dp.id, dp.postulacion_id, dp.tipo_documento, dp.nombre_original, dp.estado, "
                + "CASE WHEN dp.es_vigente THEN 'SI' ELSE 'NO' END "
                + "FROM documento_postulacion dp "
                + (todos ? "" : "WHERE dp.postulacion_id = ? ")
                + "ORDER BY dp.id DESC";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setLong(1, Long.parseLong(filtroPostulacionId.trim()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        String.valueOf(rs.getLong(1)),
                        String.valueOf(rs.getLong(2)),
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

    public long registrarDocumento(long postulacionId, String codigoTipoDoc, String nombreArchivo,
                                   String rutaAlmacenamiento, String mimeType, long tamanoBytes) throws SQLException {
        try (Connection conn = ConexionDB.conectar()) {
            conn.setAutoCommit(false);
            try {
                String tipoDoc = codigoTipoDoc.trim().toUpperCase(java.util.Locale.ROOT);
                if (!java.util.Set.of("HISTORICO_ACADEMICO", "CEDULA_IDENTIDAD", "FOTOGRAFIA", "CURRICULUM_VITAE").contains(tipoDoc)) {
                    throw new SQLException("Tipo de documento no valido: " + tipoDoc);
                }
                try (PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE documento_postulacion SET es_vigente = FALSE WHERE postulacion_id = ? AND tipo_documento = ? AND es_vigente = TRUE")) {
                    psUpd.setLong(1, postulacionId);
                    psUpd.setString(2, tipoDoc);
                    psUpd.executeUpdate();
                }

                long docPostId;
                try (PreparedStatement psDoc = conn.prepareStatement(
                        "INSERT INTO documento_postulacion (postulacion_id,tipo_documento,nombre_original,clave_almacenamiento,mime_type,tamano_bytes) "
                        + "VALUES (?,?,?,?,?,?) RETURNING id")) {
                    psDoc.setLong(1,postulacionId);
                    psDoc.setString(2,tipoDoc);
                    psDoc.setString(3,nombreArchivo);
                    psDoc.setString(4,rutaAlmacenamiento);
                    psDoc.setString(5,mimeType);
                    psDoc.setLong(6,tamanoBytes);
                    try (ResultSet rs = psDoc.executeQuery()) { rs.next(); docPostId = rs.getLong(1); }
                }
                conn.commit();
                this.id = docPostId;
                return docPostId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "0".repeat(64);
        }
    }
}

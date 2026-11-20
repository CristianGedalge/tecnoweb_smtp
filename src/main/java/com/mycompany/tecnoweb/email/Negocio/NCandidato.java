package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DConsentimiento;
import com.mycompany.tecnoweb.email.Dato.DDocumento;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU2: Gestion de Candidatos, Privacidad y Consentimiento.
 */
public class NCandidato {
    private final DConsentimiento dConsentimiento = new DConsentimiento();
    private final DDocumento dDocumento = new DDocumento();

    public List<String[]> listarConsentimientos(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISCON[\"id_persona\"]");
        return dConsentimiento.listar(params.get(0));
    }

    public String registrarConsentimiento(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 3, "INSCON[\"id_persona\",\"tipo_consentimiento\",\"aceptado\"]");
        long idPersona = NUsuario.parsearLong(params.get(0), "id_persona");
        String tipo = NUsuario.exigirTexto(params.get(1), "tipo_consentimiento");
        String aceptadoStr = NUsuario.exigirTexto(params.get(2), "aceptado").toUpperCase();
        boolean otorgado = aceptadoStr.equals("SI") || aceptadoStr.equals("TRUE") || aceptadoStr.equals("1") || aceptadoStr.equals("ACEPTADO");

        dConsentimiento.setPersonaId(idPersona);
        dConsentimiento.setTipo(tipo);
        dConsentimiento.setOtorgado(otorgado);

        long id = dConsentimiento.insertar();
        return "Éxito: Consentimiento registrado correctamente (ID: " + id + ").";
    }

    public String revocarConsentimiento(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "ELICON[\"id_consentimiento\"]");
        long idConsentimiento = NUsuario.parsearLong(params.get(0), "id_consentimiento");
        dConsentimiento.setId(idConsentimiento);
        long idNuevo = dConsentimiento.revocar();
        return "Éxito: Consentimiento revocado correctamente (Evento ID: " + idNuevo + ").";
    }

    public List<String[]> listarDocumentos(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISDOC[\"id_postulacion\"]");
        return dDocumento.listarDocumentosPostulacion(params.get(0));
    }

    public String registrarDocumento(List<String> params, String nombreAdjunto,
                                     String rutaAdjunto, String mimeAdjunto, long tamanoBytes) throws SQLException {
        if (params == null || (params.size() != 2 && params.size() != 3)) {
            throw new IllegalArgumentException("Formato esperado: INSDOC[\"id_postulacion\",\"codigo_tipo_doc\"]");
        }
        long idPostulacion = NUsuario.parsearLong(params.get(0), "id_postulacion");
        String codigoTipo = NUsuario.exigirTexto(params.get(1), "codigo_tipo_doc");

        if (rutaAdjunto == null || rutaAdjunto.isBlank() || tamanoBytes <= 0) {
            throw new IllegalArgumentException("INSDOC requiere un archivo adjunto real.");
        }
        String nombreFinal = nombreAdjunto;
        if ((nombreFinal == null || nombreFinal.isBlank()) && params.size() == 3) {
            nombreFinal = params.get(2).trim();
        }
        if (nombreFinal == null || nombreFinal.isBlank()) {
            nombreFinal = codigoTipo.toLowerCase() + "_postulacion_" + idPostulacion + ".pdf";
        }

        dDocumento.setPostulacionId(idPostulacion);
        dDocumento.setCodigoTipoDoc(codigoTipo);
        dDocumento.setNombreOriginal(nombreFinal);
        dDocumento.setClaveAlmacenamiento(rutaAdjunto);
        dDocumento.setMimeType(mimeAdjunto);
        dDocumento.setTamanoBytes(tamanoBytes);

        long idDoc = dDocumento.insertar();
        return "Éxito: Documento " + codigoTipo.toUpperCase() + " (" + nombreFinal + ") registrado correctamente (ID: " + idDoc + ").";
    }

}

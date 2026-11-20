package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DPostulacion;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU4: Clasificar Candidatos, Asignar Planes y Trazabilidad.
 */
public class NPostulacion {
    private final DPostulacion dPostulacion = new DPostulacion();

    public List<String[]> listarPlan(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISPLA[\"id_proceso\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        return dPostulacion.listarPlanProceso(idProceso);
    }

    public List<String[]> listarPostulaciones(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISPOS[\"id_proceso\"]");
        return dPostulacion.listarPostulaciones(params.get(0));
    }

    public String insertarPostulacion(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 2, "INSPOS[\"id_proceso\",\"id_persona\"]");
        dPostulacion.setProcesoId(NUsuario.parsearLong(params.get(0), "id_proceso"));
        dPostulacion.setPersonaId(NUsuario.parsearLong(params.get(1), "id_persona"));

        long idPostulacion = dPostulacion.insertar();
        return "Éxito: Postulacion registrada y plan de etapas inicializado (ID_POSTULACION: " + idPostulacion + ").";
    }

    public String asignarActualizarEtapa(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 5,
                "ASIETA[\"id_postulacion\",\"codigo_etapa\",\"estado_etapa\",\"id_evaluador\",\"observacion\"]");
        long idPostulacion = NUsuario.parsearLong(params.get(0), "id_postulacion");
        String codigoEtapa = NUsuario.exigirTexto(params.get(1), "codigo_etapa");
        String estadoEtapa = NUsuario.exigirTexto(params.get(2), "estado_etapa");
        long idEvaluador = NUsuario.parsearLong(params.get(3), "id_evaluador");
        String observacion = NUsuario.exigirTexto(params.get(4), "observacion");

        dPostulacion.setId(idPostulacion);
        dPostulacion.setCodigoEtapa(codigoEtapa);
        dPostulacion.setEstadoEtapa(estadoEtapa);
        dPostulacion.setEvaluadorId(idEvaluador);
        dPostulacion.setObservacion(observacion);

        boolean ok = dPostulacion.modificarEtapa();
        if (!ok) {
            return "Error: No se encontro la etapa " + codigoEtapa + " para la postulacion ID " + idPostulacion + ".";
        }
        return "Éxito: Etapa " + codigoEtapa.toUpperCase() + " actualizada a " + estadoEtapa.toUpperCase()
                + " y registrada en historial de trazabilidad.";
    }

    public List<String[]> listarHistorial(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISHIS[\"id_postulacion\"]");
        long idPostulacion = NUsuario.parsearLong(params.get(0), "id_postulacion");
        return dPostulacion.listarHistorial(idPostulacion);
    }
}

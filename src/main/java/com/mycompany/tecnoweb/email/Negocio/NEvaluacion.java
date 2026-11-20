package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DEvaluacion;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU5: Ejecutar Pruebas, Consolidar Resultados y Calificar.
 */
public class NEvaluacion {
    private final DEvaluacion dEvaluacion = new DEvaluacion();

    public String iniciarPrueba(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 2, "INIPRU[\"id_postulacion\",\"codigo_prueba\"]");
        dEvaluacion.setPostulacionId(NUsuario.parsearLong(params.get(0), "id_postulacion"));
        dEvaluacion.setCodigoPrueba(NUsuario.exigirTexto(params.get(1), "codigo_prueba"));
        return dEvaluacion.iniciar();
    }

    public String responderPrueba(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 2, "RESPRU[\"id_intento\",\"respuestas\"]");
        dEvaluacion.setIntentoId(NUsuario.parsearLong(params.get(0), "id_intento"));
        dEvaluacion.setRespuestasPipe(NUsuario.exigirTexto(params.get(1), "respuestas"));
        return dEvaluacion.responder();
    }

    public String calificarRubrica(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 5,
                "CALRUB[\"id_postulacion\",\"codigo_prueba\",\"nombre_criterio\",\"puntaje\",\"observacion\"]");
        dEvaluacion.setPostulacionId(NUsuario.parsearLong(params.get(0), "id_postulacion"));
        dEvaluacion.setCodigoPrueba(NUsuario.exigirTexto(params.get(1), "codigo_prueba"));
        dEvaluacion.setNombreCriterio(NUsuario.exigirTexto(params.get(2), "nombre_criterio"));
        dEvaluacion.setPuntajeBruto(new BigDecimal(NUsuario.exigirTexto(params.get(3), "puntaje")));
        dEvaluacion.setObservaciones(NUsuario.exigirTexto(params.get(4), "observacion"));

        return dEvaluacion.calificarRubrica();
    }

    public List<String[]> listarResultados(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISRES[\"id_postulacion\"]");
        long idPostulacion = NUsuario.parsearLong(params.get(0), "id_postulacion");
        return dEvaluacion.listarResultadosPostulacion(idPostulacion);
    }
}

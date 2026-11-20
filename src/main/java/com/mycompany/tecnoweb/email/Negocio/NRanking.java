package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DRanking;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU6: Busqueda Aritmetica, Ranking y Matriz Comparativa.
 */
public class NRanking {
    private final DRanking dRanking = new DRanking();

    public List<String[]> buscarCandidatos(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 3, "BUSCAN[\"id_proceso\",\"puntaje_min\",\"afinidad_min\"]");
        dRanking.setProcesoId(NUsuario.parsearLong(params.get(0), "id_proceso"));
        dRanking.setPuntajeMinimoFiltro(new BigDecimal(NUsuario.exigirTexto(params.get(1), "puntaje_min")));
        dRanking.setAfinidadMinimaFiltro(new BigDecimal(NUsuario.exigirTexto(params.get(2), "afinidad_min")));
        return dRanking.buscar();
    }

    public List<String[]> generarRanking(List<String> params) throws SQLException {
        if (params == null || (params.size() != 1 && params.size() != 2)) {
            throw new IllegalArgumentException("Formato esperado: GENRAN[\"id_proceso\"]");
        }
        dRanking.setProcesoId(NUsuario.parsearLong(params.get(0), "id_proceso"));
        return dRanking.generarRanking();
    }

    public List<String[]> generarMatriz(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "GENMAT[\"id_proceso\"]");
        dRanking.setProcesoId(NUsuario.parsearLong(params.get(0), "id_proceso"));
        return dRanking.generarMatriz();
    }
}

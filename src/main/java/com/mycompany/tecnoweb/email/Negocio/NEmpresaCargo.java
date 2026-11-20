package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DCargo;
import com.mycompany.tecnoweb.email.Dato.DEmpresa;
import com.mycompany.tecnoweb.email.Dato.DPrueba;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de Negocio para CU3: Empresas Solicitantes, Perfiles de Cargo e Instrumentos.
 */
public class NEmpresaCargo {
    private final DEmpresa dEmpresa = new DEmpresa();
    private final DCargo dCargo = new DCargo();
    private final DPrueba dPrueba = new DPrueba();

    public List<String[]> listarEmpresas(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISEMP[\"*\"]");
        return dEmpresa.listar(params.get(0));
    }

    public String insertarEmpresa(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 7,
                "INSEMP[\"nit\",\"razon_social\",\"rubro\",\"nombre_contacto\",\"cargo_contacto\",\"correo_contacto\",\"telefono\"]");
        dEmpresa.setNit(NUsuario.exigirTexto(params.get(0), "nit"));
        dEmpresa.setRazonSocial(NUsuario.exigirTexto(params.get(1), "razon_social"));
        dEmpresa.setDireccion(NUsuario.exigirTexto(params.get(2), "rubro"));
        dEmpresa.setNombreContacto(NUsuario.exigirTexto(params.get(3), "nombre_contacto"));
        dEmpresa.setCargoContacto(NUsuario.exigirTexto(params.get(4), "cargo_contacto"));
        dEmpresa.setCorreo(NUsuario.exigirTexto(params.get(5), "correo_contacto"));
        dEmpresa.setTelefono(NUsuario.exigirTexto(params.get(6), "telefono"));

        long id = dEmpresa.insertar();
        return "Éxito: Empresa y contacto principal registrados correctamente (ID_EMPRESA: " + id + ").";
    }

    public String modificarEmpresa(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 5,
                "MODEMP[\"id_empresa\",\"razon_social\",\"rubro\",\"correo_contacto\",\"telefono\"]");
        long idEmpresa = NUsuario.parsearLong(params.get(0), "id_empresa");
        dEmpresa.setId(idEmpresa);
        dEmpresa.setRazonSocial(NUsuario.exigirTexto(params.get(1), "razon_social"));
        dEmpresa.setDireccion(NUsuario.exigirTexto(params.get(2), "rubro"));
        dEmpresa.setCorreo(NUsuario.exigirTexto(params.get(3), "correo_contacto"));
        dEmpresa.setTelefono(NUsuario.exigirTexto(params.get(4), "telefono"));

        boolean ok = dEmpresa.modificar();
        if (!ok) return "Error: No existe la empresa con ID " + idEmpresa + ".";
        return "Éxito: Empresa modificada correctamente (ID_EMPRESA: " + idEmpresa + ").";
    }

    public List<String[]> listarCargos(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISCAR[\"*\"]");
        return dCargo.listarCargos(params.get(0));
    }

    public String insertarCargo(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 6,
                "INSCAR[\"id_empresa\",\"codigo_proceso\",\"cargo\",\"funciones\",\"vacantes\",\"top_n\"]");
        dCargo.setEmpresaId(NUsuario.parsearLong(params.get(0), "id_empresa"));
        dCargo.setCodigoProceso(NUsuario.exigirTexto(params.get(1), "codigo_proceso"));
        dCargo.setCargo(NUsuario.exigirTexto(params.get(2), "cargo"));
        dCargo.setDescripcionFunciones(NUsuario.exigirTexto(params.get(3), "funciones"));
        dCargo.setVacantes((int) NUsuario.parsearLong(params.get(4), "vacantes"));
        dCargo.setTopN((int) NUsuario.parsearLong(params.get(5), "top_n"));

        long idProceso = dCargo.insertar();
        return "Éxito: Cargo y proceso de seleccion creados correctamente (ID_PROCESO: " + idProceso + ").";
    }

    public String definirRequisitoPerfil(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 3, "INSREQ[\"id_proceso\",\"codigo_dimension\",\"nivel_esperado\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        String dimension = NUsuario.exigirTexto(params.get(1), "codigo_dimension");
        int nivel = (int) NUsuario.parsearLong(params.get(2), "nivel_esperado");
        if (nivel < 1 || nivel > 10) {
            throw new IllegalArgumentException("El nivel esperado de la dimension debe estar entre 1 y 10.");
        }

        dCargo.setProcesoId(idProceso);
        dCargo.setCodigoDimension(dimension);
        dCargo.setValorIdeal(nivel);
        dCargo.guardarPerfilRequerido();
        return "Éxito: Perfil requerido actualizado para el proceso " + idProceso + " (" + dimension.toUpperCase() + " = " + nivel + ").";
    }

    public List<String[]> listarPruebas(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 1, "LISPRU[\"id_proceso\"]");
        return dPrueba.listarPruebasProceso(params.get(0));
    }

    public String asignarPruebaAProceso(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 5,
                "ASIPRU[\"id_proceso\",\"codigo_prueba\",\"peso\",\"puntaje_minimo\",\"obligatoria\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        String codigoPrueba = NUsuario.exigirTexto(params.get(1), "codigo_prueba");
        BigDecimal peso = new BigDecimal(NUsuario.exigirTexto(params.get(2), "peso"));
        BigDecimal minimo = new BigDecimal(NUsuario.exigirTexto(params.get(3), "puntaje_minimo"));
        String obligStr = NUsuario.exigirTexto(params.get(4), "obligatoria").toUpperCase();
        boolean obligatoria = obligStr.equals("SI") || obligStr.equals("TRUE") || obligStr.equals("1");

        dPrueba.setProcesoId(idProceso);
        dPrueba.setCodigoPrueba(codigoPrueba);
        dPrueba.setPeso(peso);
        dPrueba.setPuntajeMinimo(minimo);
        dPrueba.setObligatoria(obligatoria);

        long id = dPrueba.asignarAProceso();
        return "Éxito: Prueba " + codigoPrueba.toUpperCase() + " asignada al proceso " + idProceso + " (ID_PROCESO_PRUEBA: " + id + ").";
    }

    public String insertarPregunta(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 6,
                "INSPRE[\"codigo_prueba\",\"codigo_dimension\",\"enunciado\",\"opciones\",\"opcion_correcta\",\"puntaje\"]");
        dPrueba.setCodigoPrueba(NUsuario.exigirTexto(params.get(0), "codigo_prueba"));
        dPrueba.setCodigoDimension(params.get(1).trim());
        dPrueba.setEnunciado(NUsuario.exigirTexto(params.get(2), "enunciado"));
        dPrueba.setOpcionesPipe(NUsuario.exigirTexto(params.get(3), "opciones"));
        dPrueba.setEtiquetaCorrecta(NUsuario.exigirTexto(params.get(4), "opcion_correcta"));
        dPrueba.setPuntajePregunta(new BigDecimal(NUsuario.exigirTexto(params.get(5), "puntaje")));

        long idPregunta = dPrueba.insertarPregunta();
        return "Éxito: Pregunta y opciones insertadas correctamente (ID_PREGUNTA: " + idPregunta + ").";
    }
}

package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DPersona;
import com.mycompany.tecnoweb.email.Dato.DUsuario;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Capa de Negocio para CU1: Gestion de Usuarios, Personas y Roles.
 */
public class NUsuario {
    private static final Set<String> DEPARTAMENTOS = Set.of("LP", "CB", "SC", "OR", "PT", "CH", "TJ", "BE", "PD");
    private final DPersona dPersona = new DPersona();
    private final DUsuario dUsuario = new DUsuario();

    public List<String[]> listarRoles(List<String> params) throws SQLException {
        validarCantidad(params, 1, "LISROL[\"*\"]");
        return dUsuario.listarRoles();
    }

    public List<String[]> listarPersonas(List<String> params) throws SQLException {
        validarCantidad(params, 1, "LISPER[\"*\"]");
        return dPersona.listar(params.get(0));
    }

    public String insertarPersona(List<String> params) throws SQLException {
        validarCantidad(params, 7,
                "INSPER[\"ci\",\"complemento\",\"departamento\",\"nombres\",\"apellidos\",\"fecha_nac\",\"celular\"]");

        String ci = ValidacionPersona.ci(params.get(0));
        String p2 = params.get(2).trim().toUpperCase();

        String complemento;
        String expedido;
        String nombres;
        String apellidos;
        Date fechaNac;
        String celular;
        String correo = null;
        String ocupacion = null;
        String fijo = null;

        if (DEPARTAMENTOS.contains(p2) || params.get(5).trim().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                || (params.get(1).trim().matches("[A-Za-z0-9]{0,3}") && !params.get(6).contains("@"))) {
            if (!DEPARTAMENTOS.contains(p2)) throw new IllegalArgumentException("Departamento de expedicion no valido.");
            complemento = params.get(1).trim();
            if (!complemento.matches("[A-Za-z0-9]{0,3}")) throw new IllegalArgumentException("El complemento admite hasta 3 letras o digitos.");
            expedido = p2;
            nombres = exigirTexto(params.get(3), "nombres");
            apellidos = exigirTexto(params.get(4), "apellidos");
            fechaNac = parsearFecha(params.get(5));
            celular = exigirTexto(params.get(6), "celular");
        } else {
            complemento = "";
            expedido = null;
            nombres = exigirTexto(params.get(1), "nombres");
            apellidos = exigirTexto(params.get(2), "apellidos");
            fechaNac = null;
            ocupacion = ValidacionPersona.ocupacion(params.get(3));
            fijo = ValidacionPersona.telefono(params.get(4), "telefono_fijo");
            celular = exigirTexto(params.get(5), "celular");
            correo = ValidacionPersona.correo(params.get(6));
        }

        nombres = ValidacionPersona.nombre(nombres,"nombres",100);
        apellidos = ValidacionPersona.nombre(apellidos,"apellidos",121);
        celular = ValidacionPersona.telefono(celular,"celular");

        String[] partesApellidos = dividirApellidos(apellidos);
        if (partesApellidos[0].length()>60 || (partesApellidos[1]!=null && partesApellidos[1].length()>60))
            throw new IllegalArgumentException("Cada apellido admite hasta 60 caracteres.");
        dPersona.setCiNumero(ci);
        dPersona.setCiComplemento(complemento);
        dPersona.setCiExpedido(expedido);
        dPersona.setNombres(nombres);
        dPersona.setApellidoPaterno(partesApellidos[0]);
        dPersona.setApellidoMaterno(partesApellidos[1]);
        dPersona.setFechaNacimiento(fechaNac);
        dPersona.setTelefono(celular);
        dPersona.setCorreoContacto(correo);
        dPersona.setOcupacion(ocupacion);
        dPersona.setTelefonoFijo(fijo);

        long id = dPersona.insertar();
        return "Éxito: Registro insertado correctamente en persona (ID: " + id + ").";
    }

    public String modificarPersona(List<String> params) throws SQLException {
        validarCantidad(params, 5, "MODPER[\"id_persona\",\"ci\",\"nombres\",\"apellidos\",\"celular\"]");
        long idPersona = parsearLong(params.get(0), "id_persona");
        if (idPersona<=0) throw new IllegalArgumentException("El id_persona debe ser mayor que cero.");
        String ci = ValidacionPersona.ci(params.get(1));
        String nombres = ValidacionPersona.nombre(params.get(2),"nombres",100);
        String[] apellidos = dividirApellidos(ValidacionPersona.nombre(params.get(3),"apellidos",121));
        if (apellidos[0].length()>60 || (apellidos[1]!=null && apellidos[1].length()>60))
            throw new IllegalArgumentException("Cada apellido admite hasta 60 caracteres.");
        String celular = ValidacionPersona.telefono(params.get(4),"celular");

        dPersona.setId(idPersona);
        dPersona.setCiNumero(ci);
        dPersona.setNombres(nombres);
        dPersona.setApellidoPaterno(apellidos[0]);
        dPersona.setApellidoMaterno(apellidos[1]);
        dPersona.setTelefono(celular);

        boolean ok = dPersona.modificar();
        if (!ok) {
            return "Error: No existe una persona con ID " + idPersona + ".";
        }
        return "Éxito: Persona modificada correctamente (ID: " + idPersona + ").";
    }

    public List<String[]> listarUsuarios(List<String> params) throws SQLException {
        validarCantidad(params, 1, "LISUSU[\"*\"]");
        return dUsuario.listarUsuarios(params.get(0));
    }

    public String insertarUsuario(List<String> params) throws SQLException {
        validarCantidad(params, 4, "INSUSU[\"id_persona\",\"codigo_rol\",\"correo\",\"password\"]");
        dUsuario.setPersonaId(parsearLong(params.get(0), "id_persona"));
        dUsuario.setCodigoRol(exigirTexto(params.get(1), "codigo_rol"));
        dUsuario.setCorreo(exigirTexto(params.get(2), "correo"));
        dUsuario.setPassword(exigirTexto(params.get(3), "password"));

        long idUsuario = dUsuario.insertar();
        return "Éxito: Usuario registrado correctamente (ID: " + idUsuario + ").";
    }

    public String modificarUsuario(List<String> params) throws SQLException {
        validarCantidad(params, 3, "MODUSU[\"id_usuario\",\"codigo_rol\",\"correo\"]");
        long idUsuario = parsearLong(params.get(0), "id_usuario");
        dUsuario.setId(idUsuario);
        dUsuario.setCodigoRol(exigirTexto(params.get(1), "codigo_rol"));
        dUsuario.setCorreo(exigirTexto(params.get(2), "correo"));

        boolean ok = dUsuario.modificar();
        if (!ok) {
            return "Error: No existe el usuario con ID " + idUsuario + ".";
        }
        return "Éxito: Usuario modificado correctamente (ID: " + idUsuario + ").";
    }

    public String desactivarUsuario(List<String> params) throws SQLException {
        validarCantidad(params, 1, "ELIUSU[\"id_usuario\"]");
        long idUsuario = parsearLong(params.get(0), "id_usuario");
        dUsuario.setId(idUsuario);
        boolean ok = dUsuario.eliminar();
        if (!ok) {
            return "Error: No existe el usuario con ID " + idUsuario + ".";
        }
        return "Éxito: Usuario desactivado correctamente (ID: " + idUsuario + ").";
    }

    private static String[] dividirApellidos(String apellidos) {
        String limpio = apellidos.trim();
        int idx = limpio.indexOf(' ');
        if (idx < 0) return new String[]{limpio, null};
        return new String[]{limpio.substring(0, idx).trim(), limpio.substring(idx + 1).trim()};
    }

    private static Date parsearFecha(String texto) {
        try {
            return Date.valueOf(LocalDate.parse(texto.trim()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de fecha invalido ('" + texto + "'). Use YYYY-MM-DD.");
        }
    }

    public static void validarCantidad(List<String> params, int esperados, String ejemploUso) {
        if (params == null || params.size() != esperados) {
            int recibidos = params == null ? 0 : params.size();
            throw new IllegalArgumentException("Se esperaban " + esperados + " parametros y se recibieron "
                    + recibidos + ". Formato: " + ejemploUso);
        }
    }

    public static String exigirTexto(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El parametro '" + nombreCampo + "' no puede estar vacio.");
        }
        return valor.trim();
    }

    public static long parsearLong(String valor, String nombreCampo) {
        try {
            return Long.parseLong(valor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("El parametro '" + nombreCampo + "' debe ser un numero entero valido.");
        }
    }
}

package com.mycompany.tecnoweb.email.Presentacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capa de Presentacion: Analiza y valida la sintaxis COMANDO["p1","p2",...]
 * enviada en el Asunto o Cuerpo del correo.
 */
public final class AnalizadorComandos {
    private static final Pattern PATRON_GENERAL = Pattern.compile(
            "^\\s*([A-Za-z_]+)\\s*\\[(.*)\\]\\s*$", Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PREFIJO_RESPUESTA = Pattern.compile(
            "^(?:RE|FW|FWD|RV)\\s*:\\s*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    public record ComandoParseado(String nombre, List<String> parametros) {}

    public ComandoParseado analizar(String asunto, String cuerpo) {
        String origen = seleccionarEntrada(asunto, cuerpo);
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("El mensaje no contiene ningun comando en el Asunto ni en el Cuerpo.");
        }

        Matcher matcher = PATRON_GENERAL.matcher(limpiarBordes(origen));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Sintaxis invalida. Use el formato COMANDO[\"param1\",\"param2\"] en el Asunto. Envie AYUDA[\"*\"] para ver los comandos.");
        }

        String comando = matcher.group(1).toUpperCase(Locale.ROOT);
        String interior = limpiarBordes(matcher.group(2));
        List<String> parametros = parsearParametros(comando, interior);
        return new ComandoParseado(comando, List.copyOf(parametros));
    }

    private String seleccionarEntrada(String asunto, String cuerpo) {
        // Si el asunto intenta contener un comando, sus errores no deben provocar
        // que se ejecute silenciosamente otro comando del cuerpo.
        if (asunto != null && (asunto.contains("[") || asunto.contains("]"))) {
            return limpiarPrefijosAsunto(asunto);
        }
        if (cuerpo != null && !cuerpo.isBlank()) {
            for (String linea : cuerpo.split("\\R")) {
                String l = limpiarBordes(linea);
                if (!l.isEmpty() && l.contains("[") && l.contains("]")) {
                    return l;
                }
            }
        }
        return asunto != null && !asunto.isBlank() ? limpiarPrefijosAsunto(asunto.trim()) : cuerpo;
    }

    private String limpiarPrefijosAsunto(String asunto) {
        String res = limpiarBordes(asunto);
        Matcher prefijo = PREFIJO_RESPUESTA.matcher(res);
        while (prefijo.find()) {
            res = limpiarBordes(res.substring(prefijo.end()));
            prefijo = PREFIJO_RESPUESTA.matcher(res);
        }
        return res;
    }

    private List<String> parsearParametros(String comando, String interiorRaw) {
        List<String> resultado = new ArrayList<>();
        String interior = limpiarBordes(interiorRaw);

        if (interior.isEmpty() || ((comando.equals("AYUDA") || comando.equals("HELP") || comando.equals("PING")) && interior.equals("*"))) {
            if (comando.equals("AYUDA") || comando.equals("HELP") || comando.equals("PING")) {
                resultado.add("*");
                return resultado;
            }
            return resultado;
        }

        // Caso especial ECO[texto sin comillas]
        if (comando.equals("ECO") && !esComillaApertura(interior.charAt(0))) {
            resultado.add(interior);
            return resultado;
        }

        int i = 0;
        int n = interior.length();
        while (i < n) {
            while (i < n && esEspacio(interior.charAt(i))) i++;
            if (i >= n) break;

            if (!esComillaApertura(interior.charAt(i))) {
                throw new IllegalArgumentException(
                        "Faltan comillas dobles en los parametros de " + comando + ". Formato: " + comando + "[\"param1\",\"param2\"]");
            }
            char apertura = interior.charAt(i++);
            char cierre = apertura == '"' ? '"' : '\u201D';
            StringBuilder valor = new StringBuilder();
            boolean cerrado = false;
            while (i < n) {
                char c = interior.charAt(i);
                if (c == '\\' && i + 1 < n
                        && (interior.charAt(i+1) == '"' || interior.charAt(i+1) == '\\'
                            || interior.charAt(i+1) == cierre)) {
                    valor.append(interior.charAt(i+1));
                    i += 2;
                } else if (c == cierre) {
                    cerrado = true;
                    i++; // saltar comilla de cierre
                    break;
                } else {
                    valor.append(c);
                    i++;
                }
            }
            if (!cerrado) {
                throw new IllegalArgumentException(
                        "Comillas sin cerrar en los parametros del comando " + comando + ".");
            }
            resultado.add(valor.toString());

            while (i < n && esEspacio(interior.charAt(i))) i++;
            if (i < n) {
                if (interior.charAt(i) != ',') {
                    throw new IllegalArgumentException(
                            "Los parametros deben separarse con coma (,) en el comando " + comando + ".");
                }
                i++; // saltar coma
                while (i < n && esEspacio(interior.charAt(i))) i++;
                if (i == n) {
                    throw new IllegalArgumentException("Coma final sin parametro adicional en " + comando + ".");
                }
            }
        }
        return resultado;
    }

    private static boolean esComillaApertura(char c) {
        return c == '"' || c == '\u201C' || c == '\u201E';
    }

    private static boolean esEspacio(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\uFEFF';
    }

    private static String limpiarBordes(String texto) {
        int desde=0, hasta=texto.length();
        while (desde<hasta && esEspacio(texto.charAt(desde))) desde++;
        while (hasta>desde && esEspacio(texto.charAt(hasta-1))) hasta--;
        return texto.substring(desde,hasta);
    }
}

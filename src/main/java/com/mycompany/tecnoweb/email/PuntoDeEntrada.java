package com.mycompany.tecnoweb.email;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import com.mycompany.tecnoweb.email.Presentacion.ClientePOP3;
import com.mycompany.tecnoweb.email.Presentacion.ClientePOP3.AdjuntoExtraido;
import com.mycompany.tecnoweb.email.Presentacion.ClienteSMTP;
import com.mycompany.tecnoweb.email.Presentacion.ProcesadorComandos;
import com.mycompany.tecnoweb.email.Presentacion.RegistroEventos;
import com.mycompany.tecnoweb.email.Presentacion.ProcesadorComandos.RespuestaComando;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.mail.Message;

/**
 * Punto de entrada principal del sistema de correo en 3 capas
 * (Presentacion -> Negocio -> Dato -> PostgreSQL).
 */
public final class PuntoDeEntrada {

    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        Path archivoConfig = Path.of("correo.properties");
        if (Files.isRegularFile(archivoConfig)) {
            try (Reader lector = Files.newBufferedReader(archivoConfig, StandardCharsets.UTF_8)) {
                config.load(lector);
            }
        }
        ConexionDB.configurar(config);

        Path dirAdjuntos = Path.of(config.getProperty("adjuntos.directorio", "storage_correo"));
        ProcesadorComandos procesador = new ProcesadorComandos(dirAdjuntos);

        // Modo de prueba directa por terminal: --probar COMANDO["p1","p2"]
        if (args.length >= 2 && args[0].equals("--probar")) {
            String cmd = args[1];
            RespuestaComando resp = procesador.procesarMensaje(cmd, "", null);
            System.out.println(resp.cuerpoTexto());
            if (resp.archivoAdjunto() != null) {
                System.out.println("Archivo generado: " + resp.archivoAdjunto().toAbsolutePath());
            }
            return;
        }

        if (!Files.isRegularFile(archivoConfig)) {
            throw new IllegalArgumentException(
                    "Falta correo.properties. Copia correo.properties.example y completa tu cuenta.");
        }
        for (String clave : new String[]{"pop3.host", "pop3.usuario", "pop3.password", "smtp.host", "smtp.emisor"}) {
            exigir(config, clave);
        }
        if (Boolean.parseBoolean(config.getProperty("smtp.auth", "false"))) {
            exigir(config, "smtp.usuario");
            exigir(config, "smtp.password");
        }

        int intervalo = Integer.parseInt(config.getProperty("intervalo.segundos", "10"));
        if (intervalo <= 0) throw new IllegalArgumentException("intervalo.segundos debe ser positivo.");
        boolean unaVez = args.length == 1 && args[0].equals("--una-vez");
        if (args.length > 0 && !unaVez) {
            throw new IllegalArgumentException("Uso: sin argumentos, --una-vez, o --probar COMANDO[\"...\"]");
        }

        Path historial = Path.of(config.getProperty("procesados.archivo", ".correos-procesados"));
        Set<String> procesados = Files.exists(historial)
                ? new HashSet<>(Files.readAllLines(historial, StandardCharsets.UTF_8))
                : new HashSet<>();
        String cuenta = config.getProperty("pop3.host") + ":" + config.getProperty("pop3.port", "110")
                + ":" + config.getProperty("pop3.usuario") + ":";

        ClienteSMTP smtp = new ClienteSMTP(config);
        System.out.println("Sistema TecnoWeb Email iniciado (3 Capas + PostgreSQL). Ctrl+C para detener.");

        do {
            try (ClientePOP3 pop3 = new ClientePOP3(config)) {
                Message[] mensajes = pop3.leerMensajes();
                System.out.println("Mensajes en el buzon: " + mensajes.length);
                for (Message mensaje : mensajes) {
                    try {
                        String id = Base64.getEncoder().encodeToString(
                                (cuenta + pop3.obtenerId(mensaje)).getBytes(StandardCharsets.UTF_8));
                        if (procesados.contains(id)) continue;
                        RegistroEventos.acceso("CORREO_RECIBIDO id=" + id);

                        if (!procesador.esAutomatico(mensaje)) {
                            String emisor = procesador.obtenerCorreoEmisor(mensaje);
                            if (!emisor.equalsIgnoreCase(config.getProperty("smtp.emisor"))) {
                                String asunto = mensaje.getSubject() != null ? mensaje.getSubject() : "";
                                String cuerpo = procesador.getCuerpo(mensaje);
                                AdjuntoExtraido adjunto = pop3.extraerPrimerAdjunto(mensaje, dirAdjuntos);

                                RespuestaComando respuesta = procesador.procesarMensaje(asunto, cuerpo, adjunto);
                                smtp.enviarCorreo(emisor, respuesta.asuntoRespuesta(),
                                        respuesta.cuerpoTexto(), respuesta.archivoAdjunto());
                                System.out.println("Respuesta enviada a " + emisor + " (" + respuesta.asuntoRespuesta() + ")");

                                // Si el comando fue ENVINF y hay un correo de contacto empresarial distinto, despacharlo tambien
                                if (respuesta.destinatarioAdicional() != null
                                        && !respuesta.destinatarioAdicional().isBlank()
                                        && !respuesta.destinatarioAdicional().equalsIgnoreCase(emisor)) {
                                    smtp.enviarCorreo(
                                            respuesta.destinatarioAdicional(),
                                            "Informe Ejecutivo de Seleccion - TecnoWeb",
                                            respuesta.cuerpoAdicional(),
                                            respuesta.archivoAdjunto());
                                    System.out.println("Informe despachado a empresa: " + respuesta.destinatarioAdicional());
                                }
                            }
                        }

                        Files.writeString(historial, id + System.lineSeparator(), StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        procesados.add(id);
                        RegistroEventos.acceso("CORREO_COMPLETADO id=" + id);
                    } catch (Exception error) {
                        RegistroEventos.error("CORREO_NO_COMPLETADO",error);
                        System.err.println("No se completo un mensaje: " + error.getMessage());
                        if (unaVez) throw error;
                    }
                }
            } catch (Exception error) {
                RegistroEventos.error("CONEXION_CORREO",error);
                System.err.println("Error de correo: " + error.getMessage());
                if (unaVez) throw error;
            }
            if (!unaVez) Thread.sleep(intervalo * 1000L);
        } while (!unaVez);
    }

    private static void exigir(Properties config, String clave) {
        String valor = config.getProperty(clave);
        if (valor == null || valor.isBlank() || valor.contains("TU_USUARIO") || valor.contains("TU_CONTRASENA")) {
            throw new IllegalArgumentException("Completa " + clave + " en correo.properties.");
        }
    }
}

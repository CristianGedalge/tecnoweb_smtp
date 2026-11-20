package com.mycompany.tecnoweb.email.Presentacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Capa de Presentacion: Envia respuestas por SMTP (texto plano y archivos adjuntos PDF/CSV).
 */
public final class ClienteSMTP {
    private final Properties config;
    private final Session sesion;

    public ClienteSMTP(Properties config) {
        this.config = config;
        Properties opciones = ClientePOP3.opcionesConexion(config, "smtp");
        opciones.setProperty("mail.smtp.auth", config.getProperty("smtp.auth", "false"));
        sesion = Session.getInstance(opciones);
    }

    public void enviarCorreo(String destinatario, String asunto, String cuerpo) throws MessagingException {
        enviarCorreo(destinatario, asunto, cuerpo, null);
    }

    public void enviarCorreo(String destinatario, String asunto, String cuerpo, Path adjunto) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(config.getProperty("smtp.emisor"), true));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario, true));
        mensaje.setSubject(asunto, StandardCharsets.UTF_8.name());
        mensaje.setSentDate(new Date());
        mensaje.setHeader("Auto-Submitted", "auto-replied");

        if (adjunto != null && Files.isRegularFile(adjunto)) {
            try {
                MimeBodyPart parteTexto = new MimeBodyPart();
                parteTexto.setText(cuerpo, StandardCharsets.UTF_8.name());

                MimeBodyPart parteArchivo = new MimeBodyPart();
                parteArchivo.attachFile(adjunto.toFile());

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(parteTexto);
                multipart.addBodyPart(parteArchivo);
                mensaje.setContent(multipart);
            } catch (IOException ex) {
                throw new MessagingException("No se pudo adjuntar el archivo " + adjunto + ": " + ex.getMessage(), ex);
            }
        } else {
            mensaje.setText(cuerpo, StandardCharsets.UTF_8.name());
        }

        mensaje.saveChanges();
        boolean autenticar = Boolean.parseBoolean(config.getProperty("smtp.auth", "false"));
        try (Transport transporte = sesion.getTransport("smtp")) {
            transporte.connect(config.getProperty("smtp.host"),
                    Integer.parseInt(config.getProperty("smtp.port", "25")),
                    autenticar ? config.getProperty("smtp.usuario") : null,
                    autenticar ? config.getProperty("smtp.password") : null);
            transporte.sendMessage(mensaje, mensaje.getAllRecipients());
        }
    }
}

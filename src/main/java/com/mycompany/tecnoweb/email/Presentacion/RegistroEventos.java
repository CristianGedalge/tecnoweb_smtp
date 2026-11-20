package com.mycompany.tecnoweb.email.Presentacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Auditoria local UTF-8. No registra cuerpos, parametros ni contrasenas. */
public final class RegistroEventos {
    private RegistroEventos() {}

    public static void acceso(String evento) { escribir("access.log",evento); }

    public static void error(String contexto, Throwable causa) {
        StringWriter traza=new StringWriter();
        causa.printStackTrace(new PrintWriter(traza));
        escribir("error.log",contexto+System.lineSeparator()+traza);
    }

    private static synchronized void escribir(String nombre, String mensaje) {
        try {
            Path carpeta=Path.of("logs");
            Files.createDirectories(carpeta);
            Files.writeString(carpeta.resolve(nombre),Instant.now()+" "+mensaje+System.lineSeparator(),
                    StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch (IOException e) {
            // No se interrumpe ni se repite un comando ya ejecutado si falla el disco.
            System.err.println("No se pudo escribir "+nombre+": "+e.getClass().getSimpleName());
        }
    }
}

package com.mycompany.tecnoweb.email;

import com.mycompany.tecnoweb.email.Presentacion.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.MimeMessage;

/** Pruebas locales de parsing y MIME: no usan PostgreSQL, POP3 ni SMTP. */
public class AnalizadorComandosTest {
    private static final AnalizadorComandos parser=new AnalizadorComandos();
    private static int casos;
    private static void comprobar(boolean ok,String detalle) {
        if (!ok) throw new AssertionError(detalle);
        casos++;
    }
    private static void valido(String entrada,String nombre,String... valores) {
        var resultado=parser.analizar(entrada,"");
        comprobar(resultado.nombre().equals(nombre),"Nombre: "+entrada);
        comprobar(resultado.parametros().equals(List.of(valores)),"Parametros: "+entrada);
    }
    private static void invalido(String entrada) {
        try { parser.analizar(entrada,""); }
        catch (IllegalArgumentException esperado) { casos++; return; }
        throw new AssertionError("Se acepto sintaxis invalida: "+entrada);
    }
    public static void main(String[] args) throws Exception {
        valido("LISPER[\"*\"]","LISPER","*");
        valido(" \t lisper \t[ \"*\" \t] \r\n","LISPER","*");
        valido("\uFEFF\u00A0lisper\u202F[\u00A0\"*\"\u202F]\u00A0","LISPER","*");
        valido("INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\",\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]",
                "INSPER","4715292","Juan Carlos","Perez Seras","Estudiante","33554433","71055123","juanperez@uagrm.edu.bo");
        valido("ECO[\"María, Muñoz — selección 🎓\"]","ECO","María, Muñoz — selección 🎓");
        valido("ECO[\"Dijo \\\"Hola\\\" al candidato\"]","ECO","Dijo \"Hola\" al candidato");
        valido("ECO[\"  texto con espacios  \"]","ECO","  texto con espacios  ");
        valido("ECO[\"C:\\nuevo\\archivo\"]","ECO","C:\\nuevo\\archivo");
        valido("ECO[\"C:\\\\datos\\\\cv.pdf\"]","ECO","C:\\datos\\cv.pdf");
        valido("ECO[\"texto [con corchetes], y comas\"]","ECO","texto [con corchetes], y comas");
        valido("LISPER[“*”]","LISPER","*");
        valido("ECO[\"El área “Sistemas”\"]","ECO","El área “Sistemas”");
        valido(" Re: Fwd: RV: LISPER[\"*\"] ","LISPER","*");
        valido("AYUDA[*]","AYUDA","*");
        valido("ECO[\"\"]","ECO","");
        var cuerpo=parser.analizar("Solicitud de consulta","Buenos días\n LISPER[\"Muñoz\"]\nGracias");
        comprobar(cuerpo.parametros().equals(List.of("Muñoz")),"Comando desde el cuerpo");
        Locale anterior=Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR")); valido("lisper[\"*\"]","LISPER","*"); }
        finally { Locale.setDefault(anterior); }
        for (String entrada:new String[]{"", "LISPER", "LISPER[*]", "LISPER[\"sin cierre]", "LISPER[\"*\",]",
                "INSPER[\"1\" \"Ana\"]", "LISPER[\"*\"] basura", "LISPER[\"a\",,\"b\"]", "LISPER['*']",
                "LISPER[“*\"]", "LISPER[\"*\"] INSPER[\"otro\"]"}) invalido(entrada);
        try { parser.analizar("LISPER[sin comillas]","ECO[\"no ejecutar\"]"); throw new AssertionError("No debe ocultar un asunto invalido"); }
        catch (IllegalArgumentException esperado) { casos++; }

        // JavaMail debe decodificar tanto encabezados MIME como texto UTF-8.
        Session sesion=Session.getInstance(new Properties());
        MimeMessage enviado=new MimeMessage(sesion);
        enviado.setSubject("ECO[\"María dijo \\\"sí\\\" 🎓\"]","UTF-8");
        enviado.setText("ECO[\"Muñoz, selección\"]","UTF-8");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); enviado.writeTo(bytes);
        MimeMessage recibido=new MimeMessage(sesion,new ByteArrayInputStream(bytes.toByteArray()));
        comprobar(parser.analizar(recibido.getSubject(),"").parametros().equals(List.of("María dijo \"sí\" 🎓")),"Asunto MIME UTF-8");
        ProcesadorComandos procesador=new ProcesadorComandos();
        comprobar(procesador.getCuerpo(recibido).contains("Muñoz, selección"),"Cuerpo MIME UTF-8");
        comprobar(procesador.procesarComando("INSPER[\"1\",\"Ana\"]").startsWith("Error:"),"Validar cantidad antes de SQL");
        comprobar(procesador.procesarComando("INSPER[sin comillas]").startsWith("Error:"),"Responder error de sintaxis");
        comprobar(procesador.procesarComando("DESCONOCIDO[\"*\"]").startsWith("Error:"),"Rechazar comando desconocido");
        comprobar(procesador.procesarComando("ECO[\"Éxito, UTF-8\"]").equals("Éxito, UTF-8"),"Respuesta UTF-8");
        comprobar(Files.isRegularFile(Path.of("logs/access.log")),"access.log creado");
        comprobar(Files.readString(Path.of("logs/error.log")).contains("PARSING_INVALIDO"),"Error con traza registrado");
        System.out.println("OK: "+casos+" comprobaciones de parser, MIME y logging; sin conexiones externas.");
    }
}

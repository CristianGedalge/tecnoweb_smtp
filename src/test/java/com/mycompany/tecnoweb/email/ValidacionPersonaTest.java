package com.mycompany.tecnoweb.email;

import com.mycompany.tecnoweb.email.Negocio.ValidacionPersona;
import com.mycompany.tecnoweb.email.Presentacion.ProcesadorComandos;

/** Verifica reglas personales y respuestas de error sin abrir conexiones SQL. */
public class ValidacionPersonaTest {
    private static int casos;
    private static final ProcesadorComandos app=new ProcesadorComandos();
    private static void igual(String esperado,String real) {
        if (!esperado.equals(real)) throw new AssertionError("Esperado: "+esperado+"; recibido: "+real);
        casos++;
    }
    private static void error(String comando,String detalle) {
        String respuesta=app.procesarComando(comando);
        if (!respuesta.startsWith("Error: ") || !respuesta.contains(detalle)) throw new AssertionError(respuesta);
        casos++;
    }
    private static String ins(String nombre,String apellido,String ocupacion,String fijo,String celular,String correo) {
        return "INSPER[\"4715292\",\""+nombre+"\",\""+apellido+"\",\""+ocupacion+"\",\""+fijo+"\",\""+celular+"\",\""+correo+"\"]";
    }
    public static void main(String[] args) {
        for (String nombre:new String[]{"María José","Muñoz","Jean-Luc","O'Connor","D’Ávila","李明"})
            igual(nombre,ValidacionPersona.nombre(nombre,"nombres",100));
        igual("José",ValidacionPersona.nombre("Jose\u0301","nombres",100));
        igual("Juan Carlos",ValidacionPersona.nombre("Juan  Carlos","nombres",100));
        igual("+591 (3) 334-5566",ValidacionPersona.telefono("+591 (3) 334-5566","telefono_fijo"));
        igual("4715292",ValidacionPersona.ci("4715292"));
        igual("ana+cv@example.com",ValidacionPersona.correo("ana+cv@example.com"));
        igual("Ing. de Sistemas (TI)",ValidacionPersona.ocupacion("Ing. de Sistemas (TI)"));
        for (String nombre:new String[]{"ej#r$5G","Juan123","<script>","---","😊"})
            error(ins(nombre,"Perez","Estudiante","33554433","71055123","ana@example.com"),"'nombres' solo admite");
        error(ins("Juan","Perez5","Estudiante","33554433","71055123","ana@example.com"),"'apellidos' solo admite");
        error(ins("Juan","Perez","ej#r$5G","33554433","71055123","ana@example.com"),"La ocupacion");
        error(ins("Juan","Perez","Estudiante","---","71055123","ana@example.com"),"'telefono_fijo'");
        error(ins("Juan","Perez","Estudiante","33554433","ej#r$5G","ana@example.com"),"'celular'");
        error(ins("Juan","Perez","Estudiante","33554433","71055123","sin-correo"),"Correo de contacto no valido");
        error(ins("Juan","Perez","Estudiante","33554433","71055123","ana@@example.com"),"Correo de contacto no valido");
        error(ins("Juan","Perez","Estudiante","33554433","71055123","ana..perez@example.com"),"Correo de contacto no valido");
        error(ins("A".repeat(101),"Perez","Estudiante","33554433","71055123","ana@example.com"),"hasta 100 caracteres");
        error(ins("Juan","P".repeat(61),"Estudiante","33554433","71055123","ana@example.com"),"Cada apellido");
        error("INSPER[\"ej#r$5G\",\"Juan\",\"Perez\",\"Estudiante\",\"33554433\",\"71055123\",\"ana@example.com\"]","El CI");
        error("INSPER[\"4715292\",\"#\",\"SC\",\"Juan\",\"Perez\",\"2000-01-01\",\"71055123\"]","El complemento");
        error("INSPER[\"4715292\",\"\",\"XX\",\"Juan\",\"Perez\",\"2000-01-01\",\"71055123\"]","Departamento");
        error("INSPER[\"4715292\",\"\",\"SC\",\"Juan\",\"Perez\",\"2000-02-31\",\"71055123\"]","Formato de fecha invalido");
        error("MODPER[\"1\",\"4715292\",\"ej#r$5G\",\"Perez\",\"71055123\"]","'nombres' solo admite");
        error("MODPER[\"1\",\"4715292\",\"Juan\",\"Perez5\",\"71055123\"]","'apellidos' solo admite");
        error("MODPER[\"1\",\"4715292\",\"Juan\",\"Perez\",\"---\"]","'celular'");
        error("MODPER[\"-1\",\"4715292\",\"Juan\",\"Perez\",\"71055123\"]","mayor que cero");
        igual("ej#r$5G",app.procesarComando("ECO[\"ej#r$5G\"]"));
        System.out.println("OK: "+casos+" comprobaciones de validacion personal; sin base de datos.");
    }
}

package com.mycompany.tecnoweb.email;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import com.mycompany.tecnoweb.email.Presentacion.ProcesadorComandos;
import com.mycompany.tecnoweb.email.Presentacion.ClientePOP3.AdjuntoExtraido;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Ejecutar contra una base NUEVA con talento_schema.sql; no envia correos. */
public class FlujoReducidoIT {
    private static final ProcesadorComandos app=new ProcesadorComandos(Path.of("target/it-informes"));
    private static int comprobaciones;
    private static String comando(String texto) {
        String respuesta=app.procesarComando(texto);
        if (respuesta.contains("Error") || respuesta.contains("error")) throw new AssertionError(texto+"\n"+respuesta);
        comprobaciones++;
        return respuesta;
    }
    private static void falla(String texto) {
        String respuesta=app.procesarComando(texto);
        if (!respuesta.contains("Error")) throw new AssertionError("Debio rechazar: "+texto+"\n"+respuesta);
        comprobaciones++;
    }
    private static long numero(String sql) throws Exception {
        try (Connection c=ConexionDB.conectar(); Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) {
            r.next(); return r.getLong(1);
        }
    }
    private static void verificar(boolean condicion,String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
    public static void main(String[] args) throws Exception {
        if (args.length!=2) throw new IllegalArgumentException("Uso: FlujoReducidoIT jdbc_url usuario (base nueva de prueba)");
        Properties p=new Properties(); p.setProperty("db.url",args[0]); p.setProperty("db.usuario",args[1]); p.setProperty("db.password","");
        ConexionDB.configurar(p);
        verificar(numero("SELECT count(*) FROM information_schema.tables WHERE table_schema='talento' AND table_type='BASE TABLE'")==26,"Deben existir 26 tablas");
        comando("INSPER[\"100\",\"\",\"SC\",\"Admin\",\"Prueba\",\"1990-01-01\",\"70000000\"]");
        comando("INSUSU[\"1\",\"ADMIN\",\"admin@example.test\",\"clave-prueba\"]");
        comando("INSPER[\"200\",\"\",\"SC\",\"Ana\",\"Prueba\",\"2000-01-01\",\"70000001\"]");
        comando("INSUSU[\"2\",\"CANDIDATO\",\"ana@example.test\",\"clave-prueba\"]");
        falla("MODUSU[\"2\",\"RECLUTADO\",\"ana@example.test\"]");
        comando("INSEMP[\"999\",\"Empresa Prueba\",\"Tecnologia\",\"Contacto Prueba\",\"RRHH\",\"empresa@example.test\",\"70000002\"]");
        comando("INSCAR[\"1\",\"EMP-TEST\",\"Analista\",\"Desarrollar\",\"1\",\"3\"]");
        comando("INSPRU[\"TEST_LOCAL\",\"Prueba local\",\"TEST\",\"Instrumento de prueba\"]");
        comando("INSDIM[\"TRABAJO_EQUIPO\",\"Trabajo en equipo\",\"HABILIDAD_BLANDA\"]");
        comando("INSPRE[\"TEST_LOCAL\",\"SOCIABILIDAD\",\"Pregunta\",\"A:Correcta|B:Incorrecta\",\"A\",\"10\"]");
        comando("ASIPRU[\"1\",\"TEST_LOCAL\",\"50\",\"51\",\"SI\"]");
        comando("ASIPRU[\"1\",\"ENTREVISTA\",\"50\",\"51\",\"SI\"]");
        comando("INSCRI[\"ENTREVISTA\",\"Comunicacion\",\"10\",\"1\"]");
        comando("INSCRI[\"ENTREVISTA\",\"Experiencia\",\"10\",\"2\"]");
        comando("INSREQ[\"1\",\"SOCIABILIDAD\",\"8\"]");
        falla("INSPOS[\"1\",\"2\"]");
        comando("INSCON[\"2\",\"TRATAMIENTO_DATOS\",\"SI\"]");
        comando("INSPOS[\"1\",\"2\"]");
        long post=numero("SELECT id FROM postulacion WHERE proceso_id=1 AND persona_id=2");
        verificar(numero("SELECT count(*) FROM historial_postulacion_etapa")==10,"El trigger debe crear el historial inicial");
        falla("FINPOS[\""+post+"\",\"1\"]");
        falla("INSDOC[\""+post+"\",\"CURRICULUM_VITAE\"]");
        Path cv=Path.of("target/it-cv.txt"); Files.writeString(cv,"Curriculum de prueba");
        var respuesta=app.procesarMensaje("INSDOC[\""+post+"\",\"CURRICULUM_VITAE\"]","",new AdjuntoExtraido("cv.txt",cv,"text/plain",Files.size(cv)));
        verificar(!respuesta.cuerpoTexto().contains("Error"),respuesta.cuerpoTexto());
        long doc=numero("SELECT id FROM documento_postulacion WHERE postulacion_id="+post);
        comando("VALDOC[\""+doc+"\",\"VALIDADO\",\"1\",\"CV revisado\"]");
        comando("INIPRU[\""+post+"\",\"TEST_LOCAL\"]");
        long intento=numero("SELECT id FROM intento_prueba WHERE postulacion_id="+post);
        comando("RESPRU[\""+intento+"\",\"1:A\"]");
        falla("RESPRU[\""+intento+"\",\"1:B\"]");
        comando("CALRUB[\""+post+"\",\"ENTREVISTA\",\"Comunicacion\",\"10\",\"Bien\"]");
        verificar(numero("SELECT puntaje_normalizado FROM resultado_prueba WHERE intento_id IS NULL")==50,"No ignorar criterios pendientes");
        comando("CALRUB[\""+post+"\",\"ENTREVISTA\",\"Experiencia\",\"10\",\"Bien\"]");
        for (String etapa:new String[]{"REQUISITOS","ANALISIS_CV","EVALUACION_TECNICA","EVALUACION_PSICOLOGICA","HABILIDADES_BLANDAS","ENTREVISTA","INVESTIGACION_ANTECEDENTES"})
            comando("ASIETA[\""+post+"\",\""+etapa+"\",\"APROBADA\",\"1\",\"Revisada\"]");
        falla("FINPOS[\""+post+"\",\"2\"]");
        comando("FINPOS[\""+post+"\",\"1\"]");
        verificar(numero("SELECT count(*) FROM v_reclutados")==1,"Debe aparecer como reclutado");
        verificar(comando("LISREC[\"1\"]").contains("Ana"),"Debe listar a Ana");
        verificar(comando("LISUSU[\"*\"]").contains("RECLUTADO"),"Debe mostrar condicion de reclutado");
        for (String cmd:new String[]{"LISROL[\"*\"]","LISPER[\"*\"]","LISCON[\"2\"]","LISDOC[\""+post+"\"]","LISEMP[\"*\"]","LISCAR[\"*\"]","LISPRU[\"1\"]","LISPLA[\"1\"]","LISPOS[\"1\"]","LISHIS[\""+post+"\"]","LISRES[\""+post+"\"]","BUSCAN[\"1\",\"0\",\"0\"]","GENRAN[\"1\"]","GENMAT[\"1\"]"}) comando(cmd);
        comando("INSINF[\""+post+"\",\"RECOMENDADO\",\"Tecnica\",\"Ninguna\",\"Apto\"]");
        var informe=app.procesarMensaje("GENINF[\"1\",\"Candidato recomendado\"]","",null);
        verificar(informe.archivoAdjunto()!=null && Files.size(informe.archivoAdjunto())>100,"Debe generar PDF");
        String pdf=Files.readString(informe.archivoAdjunto(),java.nio.charset.StandardCharsets.ISO_8859_1);
        int start=pdf.lastIndexOf("startxref\n")+10;
        int offset=Integer.parseInt(pdf.substring(start,pdf.indexOf('\n',start)));
        verificar(pdf.startsWith("xref\n",offset),"La tabla xref debe apuntar a su posicion real");
        var individual=app.procesarMensaje("GENIND[\""+post+"\"]","",null);
        verificar(individual.archivoAdjunto()!=null && Files.exists(individual.archivoAdjunto()),"Debe emitir informe individual");
        var despacho=app.procesarMensaje("ENVINF[\"1\"]","",null);
        verificar("empresa@example.test".equals(despacho.destinatarioAdicional()),"Debe resolver destinatario del despacho");
        for (String tipo:new String[]{"EMBUDO","TAREAS","PUNTAJES","GENERAL"}) comando("GENREP[\""+tipo+"\",\"1\"]");
        comando("DATACA[\"2\",\"Informatica\",\"REG-200\",\"2026\",\"80\"]");
        comando("INSCOV[\"TAL-TEST\",\"Convocatoria\",\"Informatica\",\"1\"]");
        comando("ASIPRU[\"2\",\"PPA\",\"100\",\"51\",\"SI\"]");
        comando("INSPOS[\"2\",\"2\"]");
        long postTalento=numero("SELECT id FROM postulacion WHERE proceso_id=2");
        comando("CALPPA[\""+postTalento+"\"]");
        verificar(numero("SELECT puntaje_bruto FROM resultado_prueba WHERE postulacion_id="+postTalento)==80,"Debe calcular PPA");
        try (Connection c=ConexionDB.conectar(); Statement st=c.createStatement()) {
            boolean rechazado=false;
            try { st.executeUpdate("UPDATE resultado_prueba SET postulacion_id="+postTalento+" WHERE intento_id="+intento); }
            catch (SQLException esperado) { rechazado=true; }
            verificar(rechazado,"El trigger debe rechazar UPDATE con prueba de otro proceso");
        }
        comando("ASIETA[\""+postTalento+"\",\"REQUISITOS\",\"REPROBADA\",\"1\",\"Falta documento\"]");
        verificar(numero("SELECT count(*) FROM v_reclutados WHERE persona_id=2")==1,"Reprobar otro proceso no borra el aprobado");
        comando("ELICON[\"1\"]");
        falla("INSPOS[\"2\",\"2\"]");
        comando("INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\",\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]");
        verificar(numero("SELECT count(*) FROM persona WHERE ci_numero='4715292' AND ocupacion='Estudiante' AND telefono_fijo='33554433' AND telefono='71055123' AND correo_contacto='juanperez@uagrm.edu.bo' AND fecha_nacimiento IS NULL")==1,"Guardar los siete campos del ejemplo sin inventar fecha de nacimiento");
        falla("INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\",\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]");
        falla("INSPER[\"99999\",\"\",\"XX\",\"Ana\",\"Prueba\",\"2000-01-01\",\"70000001\"]");
        falla("INSPER[\"99999\",\"\",\"SC\",\"Ana\",\"Prueba\",\"2000-02-31\",\"70000001\"]");
        System.out.println("OK: "+comprobaciones+" comprobaciones CU1-CU8; SMTP no enviado.");
    }
}

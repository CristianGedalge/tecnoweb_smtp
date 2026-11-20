package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DSeleccion;
import com.mycompany.tecnoweb.email.Presentacion.FormateadorRespuesta;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** Comandos del esquema de 26 tablas. Reclutado significa aprobado. */
public class NSeleccion {
    private final DSeleccion datos = new DSeleccion();

    public String ejecutar(String comando, List<String> p) throws SQLException {
        return switch (comando) {
            case "DATACA" -> {
                cantidad(p,5,"DATACA[id_persona,carrera,registro,anio,ppa]");
                BigDecimal ppa=new BigDecimal(p.get(4));
                int anio=Integer.parseInt(p.get(3));
                if (ppa.signum()<0 || ppa.compareTo(new BigDecimal("100"))>0 || anio<1950 || anio>2100)
                    throw new IllegalArgumentException("PPA entre 0 y 100; anio entre 1950 y 2100.");
                yield cambiado(datos.ejecutar("UPDATE persona SET carrera=?,registro_universitario=?,anio_egreso=?,ppa=? WHERE id=?",
                        texto(p,1),texto(p,2),anio,ppa,id(p,0)));
            }
            case "INSCOV" -> {
                cantidad(p,4,"INSCOV[codigo,nombre,carrera,id_admin]");
                yield "Convocatoria creada. ID_PROCESO: "+datos.crearConvocatoria(texto(p,0),texto(p,1),texto(p,2),id(p,3));
            }
            case "VALDOC" -> {
                cantidad(p,4,"VALDOC[id_documento,estado,id_evaluador,observacion]");
                String estado=texto(p,1).toUpperCase();
                if (!Set.of("VALIDADO","OBSERVADO","RECHAZADO").contains(estado))
                    throw new IllegalArgumentException("Estado: VALIDADO, OBSERVADO o RECHAZADO.");
                yield cambiado(datos.ejecutar("UPDATE documento_postulacion SET estado=?,validado_por=?,validado_en=now(),observacion=? "
                        + "WHERE id=? AND EXISTS (SELECT 1 FROM usuario u JOIN rol r ON r.id=u.rol_id "
                        + "WHERE u.id=? AND u.estado='ACTIVO' AND r.codigo IN ('ADMIN','AYUDANTE'))",
                        estado,id(p,2),p.get(3),id(p,0),id(p,2)));
            }
            case "FINPOS" -> {
                cantidad(p,2,"FINPOS[id_postulacion,id_evaluador]");
                datos.finalizar(id(p,0),id(p,1));
                yield "Postulacion APROBADA. El candidato es ahora reclutado en este proceso.";
            }
            case "LISREC" -> {
                cantidad(p,1,"LISREC[id_proceso o *]");
                boolean todos=p.get(0).equals("*");
                yield FormateadorRespuesta.formatearTabla("RECLUTADOS (procesos aprobados)",todos
                        ? datos.consultar("SELECT * FROM v_reclutados ORDER BY postulacion_id")
                        : datos.consultar("SELECT * FROM v_reclutados WHERE proceso_id=? ORDER BY postulacion_id",id(p,0)));
            }
            case "INSPRU" -> {
                cantidad(p,4,"INSPRU[codigo,nombre,tipo,descripcion]");
                String tipo=texto(p,2).toUpperCase();
                if (!Set.of("TEST","RUBRICA","CALCULADA").contains(tipo)) throw new IllegalArgumentException("Tipo de prueba invalido.");
                yield cambiado(datos.ejecutar("INSERT INTO prueba(codigo,nombre,tipo,descripcion) VALUES (UPPER(?),?,?,?)",
                        texto(p,0),texto(p,1),tipo,p.get(3)));
            }
            case "INSCRI" -> {
                cantidad(p,4,"INSCRI[codigo_prueba,nombre,maximo,orden]");
                yield cambiado(datos.ejecutar("INSERT INTO rubrica_criterio(prueba_id,nombre,puntaje_maximo,orden) "
                        + "SELECT id,?,?,? FROM prueba WHERE codigo=UPPER(?) AND tipo='RUBRICA'",
                        texto(p,1),new BigDecimal(p.get(2)),Integer.parseInt(p.get(3)),texto(p,0)));
            }
            case "INSDIM" -> {
                cantidad(p,3,"INSDIM[codigo,nombre,tipo]");
                String tipo=texto(p,2).toUpperCase();
                if (!Set.of("PERSONALIDAD","HABILIDAD_BLANDA","HABILIDAD_DURA").contains(tipo))
                    throw new IllegalArgumentException("Tipo de dimension invalido.");
                yield cambiado(datos.ejecutar("INSERT INTO dimension_evaluada(codigo,nombre,tipo) VALUES (UPPER(?),?,?)",
                        texto(p,0),texto(p,1),tipo));
            }
            case "CALPPA" -> {
                cantidad(p,1,"CALPPA[id_postulacion]");
                yield cambiado(datos.ejecutar("INSERT INTO resultado_prueba(postulacion_id,proceso_prueba_id,puntaje_bruto,puntaje_maximo,umbral_aplicado,aprobada,motivo_no_aprobacion) "
                        + "SELECT po.id,pp.id,per.ppa,100,COALESCE(pp.puntaje_minimo,51),per.ppa>=COALESCE(pp.puntaje_minimo,51), "
                        + "CASE WHEN per.ppa>=COALESCE(pp.puntaje_minimo,51) THEN NULL ELSE 'PUNTAJE_BAJO' END "
                        + "FROM postulacion po JOIN persona per ON per.id=po.persona_id JOIN proceso_etapa pe ON pe.proceso_id=po.proceso_id "
                        + "JOIN proceso_prueba pp ON pp.proceso_etapa_id=pe.id JOIN prueba pr ON pr.id=pp.prueba_id "
                        + "WHERE po.id=? AND po.estado='EN_PROCESO' AND pe.activa AND pr.codigo='PPA' AND pr.tipo='CALCULADA' AND per.ppa IS NOT NULL "
                        + "ON CONFLICT(postulacion_id,proceso_prueba_id) DO UPDATE SET puntaje_bruto=EXCLUDED.puntaje_bruto, "
                        + "aprobada=EXCLUDED.aprobada,umbral_aplicado=EXCLUDED.umbral_aplicado,motivo_no_aprobacion=EXCLUDED.motivo_no_aprobacion",
                        id(p,0)));
            }
            default -> throw new IllegalArgumentException("Comando complementario desconocido.");
        };
    }
    private static void cantidad(List<String> p,int n,String uso) { NUsuario.validarCantidad(p,n,uso); }
    private static long id(List<String> p,int n) { return NUsuario.parsearLong(p.get(n),"id"); }
    private static String texto(List<String> p,int n) { return NUsuario.exigirTexto(p.get(n),"parametro "+(n+1)); }
    private static String cambiado(int n) {
        if (n==0) throw new IllegalArgumentException("No existe el registro o no cumple las condiciones de la operacion.");
        return "Operacion registrada correctamente.";
    }
}

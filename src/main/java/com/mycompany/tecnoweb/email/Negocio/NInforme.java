package com.mycompany.tecnoweb.email.Negocio;

import com.mycompany.tecnoweb.email.Dato.DInforme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;

/**
 * Capa de Negocio para CU7: Informes Individuales, Recomendaciones Ejecutivas y Despacho.
 */
public class NInforme {
    private final DInforme dInforme = new DInforme();

    public ResultadoInforme emitirInformeIndividual(List<String> params, Path directorioBase) throws SQLException, IOException {
        NUsuario.validarCantidad(params,1,"GENIND[\"id_postulacion\"]");
        long id=NUsuario.parsearLong(params.get(0),"id_postulacion");
        var datos=new com.mycompany.tecnoweb.email.Dato.DSeleccion();
        List<String[]> filas=datos.consultar("SELECT po.id,TRIM(pe.nombres || ' ' || COALESCE(pe.apellido_paterno,'')) AS candidato, "
                + "COALESCE(vp.puntaje_total,0) AS puntaje,COALESCE(po.etiqueta,'PENDIENTE') AS dictamen, "
                + "COALESCE(po.recomendacion,'Sin recomendacion') AS recomendacion FROM postulacion po "
                + "JOIN persona pe ON pe.id=po.persona_id LEFT JOIN v_puntaje_postulacion vp ON vp.postulacion_id=po.id WHERE po.id=?",id);
        if (filas.size()<2) throw new IllegalArgumentException("La postulacion no existe.");
        Files.createDirectories(directorioBase);
        Path pdf=directorioBase.resolve("informe_individual_"+id+".pdf");
        Files.write(pdf,construirPdfBasico(id,"Informe individual de postulacion",filas));
        return new ResultadoInforme("Informe individual generado para la postulacion "+id,filas,pdf,null,null);
    }

    public String registrarInformeIndividual(List<String> params) throws SQLException {
        NUsuario.validarCantidad(params, 5,
                "INSINF[\"id_postulacion\",\"recomendacion\",\"fortalezas\",\"debilidades\",\"conclusion\"]");
        long idPostulacion = NUsuario.parsearLong(params.get(0), "id_postulacion");
        dInforme.setPostulacionId(idPostulacion);
        dInforme.setEtiqueta(NUsuario.exigirTexto(params.get(1), "recomendacion"));
        dInforme.setFortalezas(NUsuario.exigirTexto(params.get(2), "fortalezas"));
        dInforme.setDebilidades(NUsuario.exigirTexto(params.get(3), "debilidades"));
        dInforme.setConclusion(NUsuario.exigirTexto(params.get(4), "conclusion"));

        dInforme.guardarInformeIndividual();
        return "Éxito: Informe individual y dictamen registrados para la postulacion ID " + idPostulacion + ".";
    }

    public ResultadoInforme emitirInformeFinal(List<String> params, Path directorioBase) throws SQLException, IOException {
        NUsuario.validarCantidad(params, 2, "GENINF[\"id_proceso\",\"resumen_ejecutivo\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        String resumen = NUsuario.exigirTexto(params.get(1), "resumen_ejecutivo");

        dInforme.setProcesoId(idProceso);
        dInforme.setResumen(resumen);

        List<String[]> candidatos = dInforme.listarCandidatosDeInforme(idProceso);
        Files.createDirectories(directorioBase);
        String nombrePdf = "informe_ejecutivo_proceso_" + idProceso + ".pdf";
        Path archivoPdf = directorioBase.resolve(nombrePdf);

        byte[] bytesPdf = construirPdfBasico(idProceso, resumen, candidatos);
        Files.write(archivoPdf, bytesPdf);

        long idInforme = dInforme.emitirInformeEjecutivo(
                idProceso, resumen, nombrePdf, archivoPdf.toAbsolutePath().toString(), bytesPdf.length);

        String mensaje = "Éxito: Informe ejecutivo final emitido (ID_INFORME: " + idInforme + ").\n"
                + "Resumen Ejecutivo: " + resumen + "\n"
                + "Se adjunta el documento PDF generado (" + nombrePdf + ").";
        return new ResultadoInforme(mensaje, candidatos, archivoPdf, null, null);
    }

    public ResultadoInforme prepararDespachoEmpresa(List<String> params, Path directorioBase) throws SQLException, IOException {
        NUsuario.validarCantidad(params, 1, "ENVINF[\"id_proceso\"]");
        long idProceso = NUsuario.parsearLong(params.get(0), "id_proceso");
        dInforme.setProcesoId(idProceso);

        String[] datos = dInforme.obtenerDatosDespacho(idProceso);
        String codigoProc = datos[0];
        String nombreProc = datos[1];
        String correoEmpresa = datos[2];
        String resumen = datos[3];
        String rutaExistente = datos[4];

        List<String[]> candidatos = dInforme.listarCandidatosDeInforme(idProceso);
        Path archivoPdf = null;
        if (rutaExistente != null && !rutaExistente.isBlank() && Files.isRegularFile(Path.of(rutaExistente))) {
            archivoPdf = Path.of(rutaExistente);
        } else {
            Files.createDirectories(directorioBase);
            archivoPdf = directorioBase.resolve("informe_ejecutivo_proceso_" + idProceso + ".pdf");
            Files.write(archivoPdf, construirPdfBasico(idProceso, resumen, candidatos));
        }

        String mensajeRemitente = "Éxito: Informe del proceso " + codigoProc + " (" + nombreProc + ") "
                + (correoEmpresa.isBlank() ? "listo para despacho." : "despachado al contacto empresarial: " + correoEmpresa);
        String cuerpoEmpresa = "Estimado cliente,\n\nSe remite el Informe Ejecutivo Final del proceso "
                + codigoProc + " - " + nombreProc + ".\n\nResumen: " + resumen + "\n";

        return new ResultadoInforme(mensajeRemitente, candidatos, archivoPdf,
                correoEmpresa.isBlank() ? null : correoEmpresa, cuerpoEmpresa);
    }

    private byte[] construirPdfBasico(long idProceso, String resumen, List<String[]> candidatos) {
        List<String> lineas=new ArrayList<>();
        lineas.add("INFORME DE SELECCION - REFERENCIA "+idProceso);
        agregarLineas(lineas,"Resumen: "+resumen);
        for (int i = 1; i < candidatos.size(); i++) {
            String[] c = candidatos.get(i);
            agregarLineas(lineas,c[0]+" - "+c[1]+" | Nota: "+c[2]+" | "+c[3]);
            agregarLineas(lineas,c[4]);
        }
        int paginas=(lineas.size()+44)/45;
        List<String> objetos=new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder hijos=new StringBuilder();
        for (int i=0;i<paginas;i++) hijos.append(4+i*2).append(" 0 R ");
        objetos.add("<< /Type /Pages /Kids ["+hijos+"] /Count "+paginas+" >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        for (int i=0;i<paginas;i++) {
            int contenido=5+i*2;
            objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents "+contenido
                    +" 0 R /Resources << /Font << /F1 3 0 R >> >> >>");
            StringBuilder stream=new StringBuilder("BT /F1 11 Tf 45 760 Td 15 TL\n");
            for (int j=i*45;j<Math.min(lineas.size(),(i+1)*45);j++)
                stream.append('(').append(escaparPdf(lineas.get(j))).append(") Tj T*\n");
            stream.append("ET\n");
            objetos.add("<< /Length "+stream.toString().getBytes(StandardCharsets.ISO_8859_1).length+" >>\nstream\n"+stream+"endstream");
        }
        ByteArrayOutputStream salida=new ByteArrayOutputStream();
        salida.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets=new ArrayList<>();
        for (int i=0;i<objetos.size();i++) {
            offsets.add(salida.size());
            salida.writeBytes(((i+1)+" 0 obj\n"+objetos.get(i)+"\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref=salida.size();
        StringBuilder tabla=new StringBuilder("xref\n0 "+(objetos.size()+1)+"\n0000000000 65535 f \n");
        for (int offset:offsets) tabla.append(String.format(java.util.Locale.ROOT,"%010d 00000 n \n",offset));
        tabla.append("trailer\n<< /Size ").append(objetos.size()+1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        salida.writeBytes(tabla.toString().getBytes(StandardCharsets.ISO_8859_1));
        return salida.toByteArray();
    }

    private static void agregarLineas(List<String> lineas,String texto) {
        String limpio=texto==null?"":texto.replaceAll("\\s+"," ");
        for (int i=0;i<limpio.length();i+=85) lineas.add(limpio.substring(i,Math.min(i+85,limpio.length())));
    }

    private static String escaparPdf(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", " ");
    }

    public record ResultadoInforme(
            String mensajeCabecera,
            List<String[]> tablaCandidatos,
            Path archivoAdjunto,
            String correoEmpresaAdicional,
            String cuerpoEmpresaAdicional
    ) {}
}

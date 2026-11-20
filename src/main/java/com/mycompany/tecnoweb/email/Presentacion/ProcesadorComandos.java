package com.mycompany.tecnoweb.email.Presentacion;

import com.mycompany.tecnoweb.email.Negocio.NCandidato;
import com.mycompany.tecnoweb.email.Negocio.NEmpresaCargo;
import com.mycompany.tecnoweb.email.Negocio.NEvaluacion;
import com.mycompany.tecnoweb.email.Negocio.NInforme;
import com.mycompany.tecnoweb.email.Negocio.NInforme.ResultadoInforme;
import com.mycompany.tecnoweb.email.Negocio.NPostulacion;
import com.mycompany.tecnoweb.email.Negocio.NRanking;
import com.mycompany.tecnoweb.email.Negocio.NReporte;
import com.mycompany.tecnoweb.email.Negocio.NReporte.ResultadoReporte;
import com.mycompany.tecnoweb.email.Negocio.NUsuario;
import com.mycompany.tecnoweb.email.Negocio.NSeleccion;
import com.mycompany.tecnoweb.email.Presentacion.AnalizadorComandos.ComandoParseado;
import com.mycompany.tecnoweb.email.Presentacion.ClientePOP3.AdjuntoExtraido;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

/**
 * Capa de Presentacion: Enrutador que conecta los comandos por correo
 * con la Capa de Negocio (CU1 a CU8).
 */
public final class ProcesadorComandos {
    private final AnalizadorComandos analizador = new AnalizadorComandos();
    private final NUsuario nUsuario = new NUsuario();
    private final NCandidato nCandidato = new NCandidato();
    private final NEmpresaCargo nEmpresaCargo = new NEmpresaCargo();
    private final NPostulacion nPostulacion = new NPostulacion();
    private final NEvaluacion nEvaluacion = new NEvaluacion();
    private final NRanking nRanking = new NRanking();
    private final NInforme nInforme = new NInforme();
    private final NReporte nReporte = new NReporte();
    private final NSeleccion nSeleccion = new NSeleccion();
    private final Path directorioAdjuntos;

    public record RespuestaComando(
            String asuntoRespuesta,
            String cuerpoTexto,
            Path archivoAdjunto,
            String destinatarioAdicional,
            String cuerpoAdicional
    ) {
        public static RespuestaComando simple(String comando, String cuerpo) {
            return new RespuestaComando("Respuesta [" + comando + "] - TecnoWeb", cuerpo, null, null, null);
        }
    }

    public ProcesadorComandos() {
        this(Path.of("storage_correo"));
    }

    public ProcesadorComandos(Path directorioAdjuntos) {
        this.directorioAdjuntos = directorioAdjuntos;
    }

    public String procesarComando(String entradaTexto) {
        return procesarMensaje(entradaTexto, "", null).cuerpoTexto();
    }

    public RespuestaComando procesarMensaje(String asunto, String cuerpo, AdjuntoExtraido adjunto) {
        RespuestaComando respuesta = ejecutarMensaje(asunto,cuerpo,adjunto);
        RegistroEventos.acceso("RESPUESTA " + respuesta.asuntoRespuesta() + " resultado="
                + (respuesta.cuerpoTexto().startsWith("Error") ? "ERROR" : "OK"));
        return respuesta;
    }

    private RespuestaComando ejecutarMensaje(String asunto, String cuerpo, AdjuntoExtraido adjunto) {
        ComandoParseado cmd;
        try {
            cmd = analizador.analizar(asunto, cuerpo);
        } catch (IllegalArgumentException ex) {
            RegistroEventos.error("PARSING_INVALIDO",ex);
            return RespuestaComando.simple("ERROR", "Error: " + ex.getMessage());
        }

        String nombre = cmd.nombre();
        List<String> p = cmd.parametros();
        RegistroEventos.acceso("COMANDO " + nombre + " parametros=" + p.size());

        try {
            return switch (nombre) {
                case "DATACA", "INSCOV", "VALDOC", "FINPOS", "LISREC", "INSPRU", "INSCRI", "INSDIM", "CALPPA" ->
                    RespuestaComando.simple(nombre, nSeleccion.ejecutar(nombre, p));
                // Utilitarios y Ayuda
                case "AYUDA", "HELP" -> RespuestaComando.simple(nombre, FormateadorRespuesta.generarAyudaCompleta());
                case "PING" -> RespuestaComando.simple(nombre, "PONG: El sistema de correo y base de datos esta activo.");
                case "ECO" -> RespuestaComando.simple(nombre, p.isEmpty() ? "" : p.get(0));

                // CU1: Gestion de Usuarios, Personas y Roles
                case "LISROL" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("CATALOGO DE ROLES (rol)", nUsuario.listarRoles(p)));
                case "LISPER" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("LISTADO DE PERSONAS (persona)", nUsuario.listarPersonas(p)));
                case "INSPER" -> RespuestaComando.simple(nombre, nUsuario.insertarPersona(p));
                case "MODPER" -> RespuestaComando.simple(nombre, nUsuario.modificarPersona(p));
                case "LISUSU" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("LISTADO DE USUARIOS (usuario)", nUsuario.listarUsuarios(p)));
                case "INSUSU" -> RespuestaComando.simple(nombre, nUsuario.insertarUsuario(p));
                case "MODUSU" -> RespuestaComando.simple(nombre, nUsuario.modificarUsuario(p));
                case "ELIUSU" -> RespuestaComando.simple(nombre, nUsuario.desactivarUsuario(p));

                // CU2: Candidatos, Privacidad y Consentimiento
                case "LISCON" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("CONSENTIMIENTOS DE PRIVACIDAD (consentimiento)", nCandidato.listarConsentimientos(p)));
                case "INSCON" -> RespuestaComando.simple(nombre, nCandidato.registrarConsentimiento(p));
                case "ELICON" -> RespuestaComando.simple(nombre, nCandidato.revocarConsentimiento(p));
                case "LISDOC" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("DOCUMENTOS DE POSTULACION (documento_postulacion)", nCandidato.listarDocumentos(p)));
                case "INSDOC" -> RespuestaComando.simple(nombre, nCandidato.registrarDocumento(
                        p,
                        adjunto != null ? adjunto.nombreOriginal() : null,
                        adjunto != null ? adjunto.rutaLocal().toAbsolutePath().toString() : null,
                        adjunto != null ? adjunto.mimeType() : "application/pdf",
                        adjunto != null ? adjunto.tamanoBytes() : 1024L));

                // CU3: Empresas Solicitantes, Perfiles de Cargo e Instrumentos
                case "LISEMP" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("EMPRESAS REGISTRADAS (empresa)", nEmpresaCargo.listarEmpresas(p)));
                case "INSEMP" -> RespuestaComando.simple(nombre, nEmpresaCargo.insertarEmpresa(p));
                case "MODEMP" -> RespuestaComando.simple(nombre, nEmpresaCargo.modificarEmpresa(p));
                case "LISCAR" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("CARGOS Y PROCESOS (proceso)", nEmpresaCargo.listarCargos(p)));
                case "INSCAR" -> RespuestaComando.simple(nombre, nEmpresaCargo.insertarCargo(p));
                case "INSREQ" -> RespuestaComando.simple(nombre, nEmpresaCargo.definirRequisitoPerfil(p));
                case "LISPRU" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("PRUEBAS DE EVALUACION (prueba / proceso_prueba)", nEmpresaCargo.listarPruebas(p)));
                case "ASIPRU" -> RespuestaComando.simple(nombre, nEmpresaCargo.asignarPruebaAProceso(p));
                case "INSPRE" -> RespuestaComando.simple(nombre, nEmpresaCargo.insertarPregunta(p));

                // CU4: Clasificar Candidatos, Asignar Planes y Trazabilidad
                case "LISPLA" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("PLAN DE ETAPAS DEL PROCESO (proceso_etapa)", nPostulacion.listarPlan(p)));
                case "LISPOS" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("POSTULACIONES DEL PROCESO (postulacion)", nPostulacion.listarPostulaciones(p)));
                case "INSPOS" -> RespuestaComando.simple(nombre, nPostulacion.insertarPostulacion(p));
                case "ASIETA" -> RespuestaComando.simple(nombre, nPostulacion.asignarActualizarEtapa(p));
                case "LISHIS" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("HISTORIAL DE TRAZABILIDAD (historial_postulacion_etapa)", nPostulacion.listarHistorial(p)));

                // CU5: Ejecutar Pruebas, Consolidar Resultados y Calificar
                case "INIPRU" -> RespuestaComando.simple(nombre, nEvaluacion.iniciarPrueba(p));
                case "RESPRU" -> RespuestaComando.simple(nombre, nEvaluacion.responderPrueba(p));
                case "CALRUB" -> RespuestaComando.simple(nombre, nEvaluacion.calificarRubrica(p));
                case "LISRES" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("RESULTADOS DE EVALUACION (resultado_prueba)", nEvaluacion.listarResultados(p)));

                // CU6: Busqueda Aritmetica, Ranking y Matriz Comparativa
                case "BUSCAN" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("BUSQUEDA ARITMETICA Y AFINIDAD", nRanking.buscarCandidatos(p)));
                case "GENRAN" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("RANKING DEL PROCESO (ranking_resultado)", nRanking.generarRanking(p)));
                case "GENMAT" -> RespuestaComando.simple(nombre,
                        FormateadorRespuesta.formatearTabla("MATRIZ COMPARATIVA DE CANDIDATOS VS PERFIL", nRanking.generarMatriz(p)));

                // CU7: Informes Individuales, Recomendaciones Ejecutivas y Despacho
                case "INSINF" -> RespuestaComando.simple(nombre, nInforme.registrarInformeIndividual(p));
                case "GENIND" -> {
                    ResultadoInforme res=nInforme.emitirInformeIndividual(p,directorioAdjuntos);
                    yield new RespuestaComando("Respuesta [GENIND] - Informe individual",res.mensajeCabecera(),res.archivoAdjunto(),null,null);
                }
                case "GENINF" -> {
                    ResultadoInforme res = nInforme.emitirInformeFinal(p, directorioAdjuntos);
                    String texto = res.mensajeCabecera() + "\n\n"
                            + FormateadorRespuesta.formatearTabla("RESUMEN DE CANDIDATOS EN INFORME", res.tablaCandidatos());
                    yield new RespuestaComando("Respuesta [GENINF] - Informe Ejecutivo", texto, res.archivoAdjunto(), null, null);
                }
                case "ENVINF" -> {
                    ResultadoInforme res = nInforme.prepararDespachoEmpresa(p, directorioAdjuntos);
                    String texto = res.mensajeCabecera() + "\n\n"
                            + FormateadorRespuesta.formatearTabla("CANDIDATOS INCLUIDOS EN EL DESPACHO", res.tablaCandidatos());
                    yield new RespuestaComando("Respuesta [ENVINF] - Despacho de Informe",
                            texto, res.archivoAdjunto(), res.correoEmpresaAdicional(),
                            res.cuerpoEmpresaAdicional() + "\n" + texto);
                }

                // CU8: Reportes y Estadisticas
                case "GENREP", "LISEST" -> {
                    List<String> argsRep = nombre.equals("LISEST") ? List.of("GENERAL", "*") : p;
                    ResultadoReporte rep = nReporte.generarReporte(argsRep, directorioAdjuntos);
                    String texto = FormateadorRespuesta.formatearTabla(rep.titulo(), rep.tabla());
                    yield new RespuestaComando("Respuesta [" + nombre + "] - Reporte", texto, rep.archivoCsv(), null, null);
                }

                default -> RespuestaComando.simple(nombre,
                        "Error: Comando desconocido '" + nombre + "'. Envie AYUDA[\"*\"] en el Asunto para ver la lista de comandos.");
            };
        } catch (IllegalArgumentException ex) {
            RegistroEventos.error("VALIDACION " + nombre,ex);
            return RespuestaComando.simple(nombre, "Error: " + ex.getMessage());
        } catch (SQLException ex) {
            RegistroEventos.error("BASE_DATOS " + nombre,ex);
            return RespuestaComando.simple(nombre, "Error de Base de Datos: " + ex.getMessage());
        } catch (Exception ex) {
            RegistroEventos.error("EJECUCION " + nombre,ex);
            return RespuestaComando.simple(nombre, "Error inesperado: " + ex.getMessage());
        }
    }

    public String getCuerpo(Part parte) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())) return "";
        if (parte.isMimeType("text/plain")) return (String) parte.getContent();
        if (parte.isMimeType("multipart/*")) {
            Multipart contenido = (Multipart) parte.getContent();
            for (int i = 0; i < contenido.getCount(); i++) {
                String texto = getCuerpo(contenido.getBodyPart(i));
                if (!texto.isBlank()) return texto;
            }
        }
        return "";
    }

    public String obtenerCorreoEmisor(Message mensaje) throws MessagingException {
        if (mensaje.getFrom() == null || mensaje.getFrom().length == 0
                || !(mensaje.getFrom()[0] instanceof InternetAddress emisor)) {
            throw new MessagingException("El mensaje no tiene un remitente valido.");
        }
        emisor.validate();
        return emisor.getAddress();
    }

    public boolean esAutomatico(Message mensaje) throws MessagingException {
        String[] valores = mensaje.getHeader("Auto-Submitted");
        if (valores != null) {
            for (String valor : valores) {
                if (!valor.trim().equalsIgnoreCase("no")) return true;
            }
        }
        return mensaje.isMimeType("multipart/report");
    }
}

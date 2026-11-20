# Manual de Comandos por Caso de Uso — Tecnoweb Email

Guía rápida de todos los comandos soportados por el sistema de selección de personal vía correo electrónico.

> **Regla general de envío:**
> - Coloca el comando en el **Asunto (`Subject`)** del correo (también se reconoce si va en el cuerpo).
> - Todos los parámetros deben ir entre **comillas dobles (`"..."`)** y separados por **coma (`,`)**.
> - Formato: `COMANDO["param1","param2",...]`

---

## Utilitarios del Sistema

| Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|
| **`AYUDA`** | Envía la lista completa de comandos disponibles. | `AYUDA["*"]` | `AYUDA["*"]` |
| **`PING`** | Verifica que el sistema de correo y servidor respondan. | `PING["*"]` | `PING["*"]` |
| **`ECO`** | Devuelve el texto enviado como eco. | `ECO["texto"]` | `ECO["Hola TecnoWeb"]` |

---

## CU1 — Gestión de Usuarios, Personas y Roles (`NUsuario`)
*Tablas: `rol`, `persona`, `usuario`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 1 | **`LISROL`** | Lista los roles del catálogo. | `LISROL["*"]` | `LISROL["*"]` |
| 2 | **`LISPER`** | Lista todas las personas o filtra por CI/nombre. | `LISPER["*"]` o `LISPER["filtro"]` | `LISPER["*"]` |
| 3 | **`INSPER`** | Registra una persona nueva. | `INSPER["ci","complemento","departamento","nombres","apellidos","fecha_nac","celular"]` | `INSPER["4715292","","SC","Juan Carlos","Perez Seras","2000-05-12","71055123"]` |
| 4 | **`MODPER`** | Modifica los datos de una persona. | `MODPER["id_persona","ci","nombres","apellidos","celular"]` | `MODPER["1","4715292","Juan Carlos","Perez Seras","71099999"]` |
| 5 | **`LISUSU`** | Lista usuarios con su rol, correo y estado. | `LISUSU["*"]` o `LISUSU["filtro"]` | `LISUSU["*"]` |
| 6 | **`INSUSU`** | Crea cuenta de usuario vinculada a una persona y rol. | `INSUSU["id_persona","codigo_rol","correo","password"]` | `INSUSU["1","CANDIDATO","juanperez@uagrm.edu.bo","123456"]` |
| 7 | **`MODUSU`** | Actualiza el rol o correo de un usuario. | `MODUSU["id_usuario","codigo_rol","correo"]` | `MODUSU["1","AYUDANTE","juanperez@uagrm.edu.bo"]` |
| 8 | **`ELIUSU`** | Desactiva lógicamente una cuenta (`INACTIVO`). | `ELIUSU["id_usuario"]` | `ELIUSU["1"]` |

*(Nota: `INSPER` también acepta el formato directo `INSPER["4715292","Juan Carlos","Perez Seras","Estudiante","33554433","71055123","juanperez@uagrm.edu.bo"]`).*

En ese formato se guardan ocupación y teléfono fijo, además del celular y correo.
La fecha de nacimiento y el departamento permanecen vacíos si no se proporcionan.
INSPER y MODPER validan nombres y apellidos: letras Unicode (incluyendo tildes
y ñ), espacios, apóstrofes y guiones. Rechazan dígitos y símbolos como `#`, `$`
o emojis en esos campos. Por ejemplo, un nombre `ej#r$5G` responde:
`Error: El campo 'nombres' solo admite letras, espacios, apostrofes y guiones.`
El CI admite de 1 a 15 dígitos; los teléfonos requieren al menos un dígito y
admiten `+`, espacios, paréntesis y guiones hasta 20 caracteres. También se
validan correo, ocupación, complemento, departamento y fecha en INSPER.
Estas reglas no restringen las contraseñas ni el texto libre de ECO.
Los siete parámetros son obligatorios; las comillas dentro de un valor se escriben
como `\"`. Ejemplo: `ECO["El candidato dijo \"sí\""]`.

---

## CU2 — Gestión de Candidatos, Privacidad y Consentimiento (`NCandidato`)
*Tablas: `consentimiento`, `documento_postulacion`, `persona`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 9 | **`LISCON`** | Lista los consentimientos de una persona (o `"*"`). | `LISCON["id_persona"]` | `LISCON["1"]` |
| 10 | **`INSCON`** | Registra aceptación o rechazo de consentimiento (`TRATAMIENTO_DATOS`, `PUBLICACION_PERFIL`, `INVESTIGACION_ANTECEDENTES`). | `INSCON["id_persona","tipo_consentimiento","aceptado"]` | `INSCON["1","TRATAMIENTO_DATOS","SI"]` |
| 11 | **`ELICON`** | Revoca un consentimiento vigente (inserta evento `FALSE`). | `ELICON["id_consentimiento"]` | `ELICON["1"]` |
| 12 | **`LISDOC`** | Lista los documentos subidos para una postulación. | `LISDOC["id_postulacion"]` | `LISDOC["1"]` |
| 13 | **`INSDOC`** | Registra el archivo adjunto del correo (`CURRICULUM_VITAE`, `CEDULA_IDENTIDAD`, `HISTORICO_ACADEMICO`, `FOTOGRAFIA`). | `INSDOC["id_postulacion","codigo_tipo_doc"]` | `INSDOC["1","CURRICULUM_VITAE"]` |

---

## CU3 — Empresas Solicitantes, Perfiles de Cargo e Instrumentos (`NEmpresaCargo`)
*Tablas: `empresa`, `contacto_empresa`, `proceso`, `dimension_evaluada`, `perfil_requerido`, `prueba`, `proceso_prueba`, `rubrica_criterio`, `pregunta`, `opcion_pregunta`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 15 | **`LISEMP`** | Lista las empresas registradas y su contacto principal. | `LISEMP["*"]` o `LISEMP["filtro"]` | `LISEMP["*"]` |
| 16 | **`INSEMP`** | Registra una empresa y su contacto principal. | `INSEMP["nit","razon_social","rubro","nombre_contacto","cargo_contacto","correo_contacto","telefono"]` | `INSEMP["1020304055","Banco Ganadero","Banca","Maria Lopez","Jefe RRHH","rrhh@bg.com.bo","33445566"]` |
| 17 | **`MODEMP`** | Modifica datos de la empresa y correo de contacto. | `MODEMP["id_empresa","razon_social","rubro","correo_contacto","telefono"]` | `MODEMP["1","Banco Ganadero SA","Finanzas","talento@bg.com.bo","33445577"]` |
| 18 | **`LISCAR`** | Lista los cargos y procesos de selección. | `LISCAR["*"]` o `LISCAR["filtro"]` | `LISCAR["*"]` |
| 19 | **`INSCAR`** | Crea el proceso, la solicitud de cargo y sus etapas iniciales. | `INSCAR["id_empresa","codigo_proceso","cargo","funciones","vacantes","top_n"]` | `INSCAR["1","EMP-2026-001","Analista de Sistemas","Desarrollo Java y PostgreSQL","2","5"]` |
| 20 | **`INSREQ`** | Define el nivel ideal (1–10) de una dimensión en el perfil del cargo. | `INSREQ["id_proceso","codigo_dimension","nivel_esperado"]` | `INSREQ["1","CAPACIDAD_ESCOLAR","8"]` |
| 21 | **`LISPRU`** | Lista catálogo de pruebas (`"*"`) o las asignadas a un proceso. | `LISPRU["*"]` o `LISPRU["id_proceso"]` | `LISPRU["1"]` |
| 22 | **`ASIPRU`** | Asigna una prueba del catálogo al proceso con peso y nota mínima. | `ASIPRU["id_proceso","codigo_prueba","peso","puntaje_minimo","obligatoria"]` | `ASIPRU["1","TEST_INTELIGENCIA","60","51","SI"]` |
| 23 | **`INSPRE`** | Inserta una pregunta con sus opciones (`A:...|B:...`) en un TEST. | `INSPRE["codigo_prueba","codigo_dimension","enunciado","opciones","opcion_correcta","puntaje"]` | `INSPRE["TEST_INTELIGENCIA","CAPACIDAD_ESCOLAR","Serie 2,4,8,?","A:10|B:12|C:16","C","20"]` |

---

## CU4 — Clasificar Candidatos, Asignar Planes y Trazabilidad (`NPostulacion`)
*Tablas: `proceso_etapa`, `postulacion`, `postulacion_etapa`, `historial_postulacion_etapa`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 24 | **`LISPLA`** | Muestra el plan de etapas configurado para un proceso. | `LISPLA["id_proceso"]` | `LISPLA["1"]` |
| 25 | **`LISPOS`** | Lista los candidatos postulados a un proceso (o `"*"`). | `LISPOS["id_proceso"]` | `LISPOS["1"]` |
| 26 | **`INSPOS`** | Registra la postulación de una persona a un proceso e inicializa etapas. | `INSPOS["id_proceso","id_persona"]` | `INSPOS["1","1"]` |
| 27 | **`ASIETA`** | Cambia el estado de una etapa (`APROBADA`, `REPROBADA`, `EN_REVISION`, etc.), asigna evaluador y registra trazabilidad. | `ASIETA["id_postulacion","codigo_etapa","estado_etapa","id_evaluador","observacion"]` | `ASIETA["1","REQUISITOS","APROBADA","1","Documentos completos"]` |
| 28 | **`LISHIS`** | Consulta la bitácora completa de cambios de etapa de una postulación. | `LISHIS["id_postulacion"]` | `LISHIS["1"]` |

---

## CU5 — Ejecutar Pruebas, Consolidar Resultados y Calificar (`NEvaluacion`)
*Tablas: `intento_prueba`, `respuesta_intento`, `calificacion_criterio`, `resultado_prueba`, `resultado_dimension`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 29 | **`INIPRU`** | Inicia un intento de prueba TEST y devuelve las preguntas al correo. | `INIPRU["id_postulacion","codigo_prueba"]` | `INIPRU["1","TEST_INTELIGENCIA"]` |
| 30 | **`RESPRU`** | Envía respuestas (`"1:C|2:A"`), cierra el intento y calcula notas. | `RESPRU["id_intento","respuestas"]` | `RESPRU["1","1:C"]` |
| 31 | **`CALRUB`** | Califica un criterio de prueba manual (`CV_ANALISIS`, `ENTREVISTA`). | `CALRUB["id_postulacion","codigo_prueba","nombre_criterio","puntaje","observacion"]` | `CALRUB["1","ENTREVISTA","Dominio tecnico","85","Excelente entrevista"]` |
| 32 | **`LISRES`** | Lista calificaciones por prueba y el total ponderado de la postulación. | `LISRES["id_postulacion"]` | `LISRES["1"]` |

---

## CU6 — Búsqueda Aritmética, Ranking y Matriz Comparativa (`NRanking`)
*Tablas: `ranking_resultado`, `perfil_requerido`, `resultado_dimension`, `resultado_prueba`, `postulacion`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 33 | **`BUSCAN`** | Filtra candidatos que superen puntaje mínimo y % de afinidad. | `BUSCAN["id_proceso","puntaje_min","afinidad_min"]` | `BUSCAN["1","50","70"]` |
| 34 | **`GENRAN`** | Calcula posiciones, guarda en `ranking_resultado` y muestra Top N. | `GENRAN["id_proceso"]` | `GENRAN["1"]` |
| 35 | **`GENMAT`** | Genera matriz comparativa (pruebas + dimensiones obtenidas vs. ideal). | `GENMAT["id_proceso"]` | `GENMAT["1"]` |

---

## CU7 — Informes Individuales, Recomendaciones Ejecutivas y Despacho (`NInforme`)
*Tablas: `informe_final`, `postulacion`, `contacto_empresa`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 36 | **`INSINF`** | Registra dictamen individual (`ALTAMENTE_RECOMENDADO`, `RECOMENDADO`, `RECOMENDADO_CON_RESERVAS`, `NO_RECOMENDADO`). | `INSINF["id_postulacion","recomendacion","fortalezas","debilidades","conclusion"]` | `INSINF["1","RECOMENDADO","Solido en Java y SQL","Poca experiencia liderando","Apto para el cargo"]` |
| 37 | **`GENINF`** | Emite el informe ejecutivo final del proceso y adjunta el PDF generado. | `GENINF["id_proceso","resumen_ejecutivo"]` | `GENINF["1","Se presenta terna final evaluada"]` |
| 38 | **`ENVINF`** | Envía por SMTP el informe final en PDF al correo del contacto de la empresa. | `ENVINF["id_proceso"]` | `ENVINF["1"]` |

---

## CU8 — Reportes y Estadísticas (`NReporte`)
*Vistas: `v_embudo_proceso`, `v_bandeja_tareas`, `v_puntaje_postulacion`*

| # | Comando | Descripción | Sintaxis | Ejemplo listo para enviar |
|---|---|---|---|---|
| 39 | **`GENREP`** | Genera reporte en texto y adjunta `.csv`. Tipos: `EMBUDO`, `TAREAS`, `PUNTAJES`, `GENERAL`. | `GENREP["tipo_reporte","id_proceso"]` | `GENREP["EMBUDO","1"]` |
| 40 | **`LISEST`** | Devuelve las estadísticas generales de todo el sistema y adjunta `.csv`. | `LISEST["*"]` | `LISEST["*"]` |


## Comandos del modelo reducido

Todos los parámetros se envían entre comillas dobles. Los identificadores de
estos ejemplos deben reemplazarse por los devueltos por el sistema.

| CU | Comando y ejemplo | Función |
|---|---|---|
| CU2 | `DATACA["2","Informatica","REG-200","2026","80"]` | Guarda los datos académicos opcionales en persona. |
| CU2 | `VALDOC["1","VALIDADO","1","CV revisado"]` | Revisa un documento: VALIDADO, OBSERVADO o RECHAZADO; evaluador ADMIN o AYUDANTE activo. |
| CU3 | `INSPRU["TECNICA_SQL","Prueba SQL","TEST","Conocimientos SQL"]` | Crea instrumento TEST, RUBRICA o CALCULADA. CALPPA implementa la prueba calculada PPA. |
| CU3 | `INSCRI["ENTREVISTA","Comunicacion","10","1"]` | Define un criterio de rúbrica antes de calificar. |
| CU3 | `INSDIM["EQUIPO","Trabajo en equipo","HABILIDAD_BLANDA"]` | Crea una dimensión PERSONALIDAD, HABILIDAD_BLANDA o HABILIDAD_DURA. |
| CU4 | `INSCOV["TAL-2026","Seleccion de egresados","Informatica","1"]` | Crea convocatoria universitaria y plan inicial; último parámetro: administrador activo. |
| CU4 | `FINPOS["1","1"]` | Aprueba la postulación y registra la selección; segundo parámetro: evaluador. |
| CU1/CU4 | `LISREC["1"]` o `LISREC["*"]` | Lista reclutados por proceso o todos. |
| CU5 | `CALPPA["1"]` | Calcula el resultado PPA usando el dato académico y el mínimo del plan. |
| CU7 | `GENIND["1"]` | Adjunta PDF individual de la postulación, con recomendación guardada por INSINF. |

### Secuencia mínima

1. INSPER e INSUSU: crear administrador, ayudantes y candidatos (rol CANDIDATO).
2. INSEMP e INSCAR, o INSCOV para talento universitario.
3. Preparar instrumentos con INSPRU, INSPRE, INSCRI e INSREQ.
4. ASIPRU: asignar pruebas y pesos que sumen 100.
5. INSCON de TRATAMIENTO_DATOS antes de INSPOS.
6. INSDOC con CV adjunto real. El documento queda PENDIENTE hasta VALDOC.
7. INIPRU y RESPRU para tests; CALRUB para cada criterio; CALPPA si está asignado.
8. ASIETA para revisar y aprobar las etapas eliminatorias; usar el motivo para la conclusión de antecedentes.
9. FINPOS convierte al candidato en reclutado en ese proceso. Verificar con LISREC.
10. BUSCAN, GENRAN y GENMAT para búsqueda, ranking interno y comparación.
11. INSINF y GENIND para recomendación y PDF individual; GENINF y ENVINF para informe ejecutivo y despacho.
12. GENREP o LISEST para reportes.

FINPOS rechaza planes sin pesos válidos, pruebas obligatorias sin aprobar,
rúbricas incompletas, etapas eliminatorias pendientes, CV sin validar o
consentimiento revocado. INSUSU y MODUSU no permiten otorgar RECLUTADO
manualmente. El candidato conserva su rol de acceso y puede postular a otros
procesos. GENRAN no publica sus datos ni lo aprueba automáticamente.

INSINV fue retirado. Los catálogos académicos, las invitaciones por token y
las solicitudes públicas de información no forman parte de esta versión.

-- INSTALACION: ejecutar este esquema de 26 tablas en una base vacia.
BEGIN;
-- =====================================================================
--  TALENTO UAGRM — Version reducida: 26 tablas, CU1 a CU8 por correo
--  Motor: PostgreSQL 14+ (probado en 16/17)
--  Esquema: talento
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS talento;
SET search_path = talento, public;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END $$;

-- =====================================================================
-- 1. SEGURIDAD, PERSONAS Y TRAZABILIDAD
-- =====================================================================

CREATE TABLE rol (
  id INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR(30) NOT NULL,
  nombre VARCHAR(60) NOT NULL,
  descripcion VARCHAR(255) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_rol_codigo UNIQUE (codigo)
);

CREATE TABLE usuario (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  rol_id INTEGER NOT NULL,
  correo VARCHAR(190) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','ACTIVO','BLOQUEADO','INACTIVO')),
  intentos_fallidos SMALLINT NOT NULL DEFAULT 0,
  ultimo_acceso TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_usuario_correo UNIQUE (correo),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
);
CREATE INDEX ix_usuario_rol ON usuario (rol_id);
CREATE TRIGGER trg_usuario_updated BEFORE UPDATE ON usuario FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE persona (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  usuario_id BIGINT NULL,
  nombres VARCHAR(100) NOT NULL,
  apellido_paterno VARCHAR(60) NULL,
  apellido_materno VARCHAR(60) NULL,
  ci_numero VARCHAR(15) NOT NULL,
  ci_complemento VARCHAR(3) NOT NULL DEFAULT '',
  ci_expedido VARCHAR(10) NULL CHECK (ci_expedido IN ('LP','CB','SC','OR','PT','CH','TJ','BE','PD')),
  fecha_nacimiento DATE NULL,
  ocupacion VARCHAR(100) NULL,
  telefono_fijo VARCHAR(20) NULL,
  carrera VARCHAR(150) NULL,
  registro_universitario VARCHAR(20) UNIQUE,
  anio_egreso SMALLINT CHECK (anio_egreso BETWEEN 1950 AND 2100),
  ppa DECIMAL(5,2) CHECK (ppa BETWEEN 0 AND 100),
  telefono VARCHAR(20) NULL,
  correo_contacto VARCHAR(190) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_persona_usuario UNIQUE (usuario_id),
  CONSTRAINT uq_persona_ci UNIQUE (ci_numero, ci_complemento),
  CONSTRAINT fk_persona_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT ck_persona_apellido CHECK (apellido_paterno IS NOT NULL OR apellido_materno IS NOT NULL)
);
CREATE INDEX ix_persona_apellidos ON persona (apellido_paterno, apellido_materno, nombres);
CREATE TRIGGER trg_persona_updated BEFORE UPDATE ON persona FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE consentimiento (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  persona_id BIGINT NOT NULL,
  tipo VARCHAR(26) NOT NULL CHECK (tipo IN ('TRATAMIENTO_DATOS','PUBLICACION_PERFIL','INVESTIGACION_ANTECEDENTES')),
  version_politica VARCHAR(20) NOT NULL,
  otorgado BOOLEAN NOT NULL,
  registrado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  ip_origen INET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT fk_consentimiento_persona FOREIGN KEY (persona_id) REFERENCES persona (id)
);
CREATE INDEX ix_consentimiento_persona_tipo ON consentimiento (persona_id, tipo, registrado_en);


-- =====================================================================
-- 3. EMPRESAS CLIENTE (FLUJO B)
-- =====================================================================

CREATE TABLE empresa (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  razon_social VARCHAR(200) NOT NULL,
  nit VARCHAR(20) NOT NULL,
  correo VARCHAR(190) NOT NULL,
  telefono VARCHAR(30) NULL,
  direccion VARCHAR(255) NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','INACTIVA')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_empresa_nit UNIQUE (nit)
);
CREATE TRIGGER trg_empresa_updated BEFORE UPDATE ON empresa FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE contacto_empresa (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  empresa_id BIGINT NOT NULL,
  persona_id BIGINT NOT NULL,
  cargo VARCHAR(100) NOT NULL,
  es_principal BOOLEAN NOT NULL DEFAULT FALSE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_contacto_persona UNIQUE (persona_id),
  CONSTRAINT fk_contacto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
  CONSTRAINT fk_contacto_persona FOREIGN KEY (persona_id) REFERENCES persona (id)
);
CREATE INDEX ix_contacto_empresa ON contacto_empresa (empresa_id);

-- =====================================================================
-- 4. CATALOGOS DE EVALUACION Y BANCO DE PREGUNTAS
-- =====================================================================


CREATE TABLE dimension_evaluada (
  id INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('PERSONALIDAD','HABILIDAD_BLANDA','HABILIDAD_DURA')),
  orden SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_dimension_codigo UNIQUE (codigo)
);

CREATE TABLE prueba (
  id INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR(50) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('TEST','RUBRICA','CALCULADA')),
  descripcion TEXT NULL,
  es_plantilla BOOLEAN NOT NULL DEFAULT TRUE,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_prueba_codigo UNIQUE (codigo),
  CONSTRAINT uq_prueba_id_tipo UNIQUE (id, tipo)
);

CREATE TABLE rubrica_criterio (
  id INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL,
  prueba_id INTEGER NOT NULL,
  tipo_prueba VARCHAR(10) NOT NULL DEFAULT 'RUBRICA' CHECK (tipo_prueba IN ('TEST','RUBRICA','CALCULADA')),
  nombre VARCHAR(100) NOT NULL,
  puntaje_maximo DECIMAL(5,2) NOT NULL,
  orden SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_criterio_prueba_nombre UNIQUE (prueba_id, nombre),
  CONSTRAINT fk_criterio_prueba FOREIGN KEY (prueba_id, tipo_prueba) REFERENCES prueba (id, tipo),
  CONSTRAINT ck_criterio_tipo   CHECK (tipo_prueba = 'RUBRICA'),
  CONSTRAINT ck_criterio_maximo CHECK (puntaje_maximo > 0)
);

CREATE TABLE pregunta (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  prueba_id INTEGER NOT NULL,
  tipo_prueba VARCHAR(10) NOT NULL DEFAULT 'TEST' CHECK (tipo_prueba IN ('TEST','RUBRICA','CALCULADA')),
  dimension_id INTEGER NULL,
  orden SMALLINT NOT NULL,
  enunciado TEXT NULL,
  imagen_ruta VARCHAR(400) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_pregunta_prueba_orden UNIQUE (prueba_id, orden),
  CONSTRAINT fk_pregunta_prueba    FOREIGN KEY (prueba_id, tipo_prueba) REFERENCES prueba (id, tipo),
  CONSTRAINT ck_pregunta_tipo      CHECK (tipo_prueba = 'TEST'),
  CONSTRAINT fk_pregunta_dimension FOREIGN KEY (dimension_id)      REFERENCES dimension_evaluada (id),
  CONSTRAINT ck_pregunta_contenido CHECK (enunciado IS NOT NULL OR imagen_ruta IS NOT NULL)
);
CREATE INDEX ix_pregunta_dimension ON pregunta (dimension_id);

CREATE TABLE opcion_pregunta (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  pregunta_id BIGINT NOT NULL,
  etiqueta CHAR(1) NOT NULL,
  texto VARCHAR(500) NULL,
  imagen_ruta VARCHAR(400) NULL,
  puntaje DECIMAL(5,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_opcion_pregunta_etiqueta UNIQUE (pregunta_id, etiqueta),
  CONSTRAINT uq_opcion_id_pregunta UNIQUE (id, pregunta_id),
  CONSTRAINT fk_opcion_pregunta FOREIGN KEY (pregunta_id)       REFERENCES pregunta (id) ON DELETE CASCADE,
  CONSTRAINT ck_opcion_puntaje  CHECK (puntaje >= 0),
  CONSTRAINT ck_opcion_contenido CHECK (texto IS NOT NULL OR imagen_ruta IS NOT NULL)
);

-- =====================================================================
-- 5. PROCESOS, ETAPAS Y CONFIGURACION
-- =====================================================================

CREATE TABLE proceso (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR(30) NOT NULL,
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('TALENTO','EMPRESA')),
  nombre VARCHAR(200) NOT NULL,
  estado VARCHAR(15) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','CONFIGURACION','ABIERTO','EN_EVALUACION','CERRADO','INFORME_EMITIDO','CANCELADO')),
  contacto_id BIGINT REFERENCES contacto_empresa(id),
  cargo VARCHAR(150),
  area VARCHAR(100),
  nivel_jerarquico VARCHAR(10) NOT NULL DEFAULT 'TECNICO'
    CHECK (nivel_jerarquico IN ('OPERATIVO','TECNICO','JEFATURA','GERENCIA','DIRECCION')),
  vacantes SMALLINT NOT NULL DEFAULT 1 CHECK (vacantes >= 1),
  descripcion_funciones TEXT,
  carrera VARCHAR(150),
  fecha_apertura DATE NOT NULL DEFAULT CURRENT_DATE,
  fecha_cierre DATE CHECK (fecha_cierre >= fecha_apertura),
  top_n SMALLINT NOT NULL DEFAULT 5,
  creado_por BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_proceso_codigo UNIQUE (codigo),
  CONSTRAINT uq_proceso_id_tipo UNIQUE (id, tipo),
  CONSTRAINT fk_proceso_creado_por FOREIGN KEY (creado_por) REFERENCES usuario (id),
  CONSTRAINT ck_proceso_top CHECK (top_n BETWEEN 1 AND 50)
);
CREATE INDEX ix_proceso_tipo_estado ON proceso (tipo, estado);
CREATE TRIGGER trg_proceso_updated BEFORE UPDATE ON proceso FOR EACH ROW EXECUTE FUNCTION set_updated_at();



CREATE TABLE proceso_etapa (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  proceso_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  ejecucion VARCHAR(10) NOT NULL CHECK (ejecucion IN ('AUTOMATICA','MANUAL')),
  orden SMALLINT NOT NULL,
  es_eliminatoria BOOLEAN NOT NULL DEFAULT TRUE,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_proceso_etapa_etapa UNIQUE (proceso_id, codigo),
  CONSTRAINT uq_proceso_etapa_orden UNIQUE (proceso_id, orden),
  CONSTRAINT fk_proceso_etapa_proceso FOREIGN KEY (proceso_id) REFERENCES proceso (id)
);

CREATE TABLE proceso_prueba (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  proceso_etapa_id BIGINT NOT NULL,
  prueba_id INTEGER NOT NULL,
  orden SMALLINT NOT NULL DEFAULT 1,
  peso DECIMAL(5,2) NOT NULL DEFAULT 0,
  obligatoria BOOLEAN NOT NULL DEFAULT TRUE,
  puntaje_minimo DECIMAL(5,2) NULL,
  umbral_origen VARCHAR(10) NOT NULL DEFAULT 'FIJO' CHECK (umbral_origen = 'FIJO'),
  tiempo_minimo_seg INTEGER NULL,
  tiempo_maximo_seg INTEGER NULL,
  max_intentos SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_proceso_prueba UNIQUE (proceso_etapa_id, prueba_id),
  CONSTRAINT fk_proceso_prueba_etapa  FOREIGN KEY (proceso_etapa_id) REFERENCES proceso_etapa (id),
  CONSTRAINT fk_proceso_prueba_prueba FOREIGN KEY (prueba_id)        REFERENCES prueba (id),
  CONSTRAINT ck_proceso_prueba_peso     CHECK (peso BETWEEN 0 AND 100),
  CONSTRAINT ck_proceso_prueba_minimo   CHECK (puntaje_minimo IS NULL OR puntaje_minimo BETWEEN 0 AND 100),
  CONSTRAINT ck_proceso_prueba_intentos CHECK (max_intentos >= 1),
  CONSTRAINT ck_proceso_prueba_tiempos  CHECK (tiempo_maximo_seg IS NULL OR tiempo_minimo_seg IS NULL OR tiempo_maximo_seg >= tiempo_minimo_seg)
);
CREATE INDEX ix_proceso_prueba_prueba ON proceso_prueba (prueba_id);

CREATE TABLE perfil_requerido (
  proceso_id BIGINT NOT NULL,
  dimension_id INTEGER NOT NULL,
  valor_ideal SMALLINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (proceso_id, dimension_id),
  CONSTRAINT fk_perfil_proceso   FOREIGN KEY (proceso_id)   REFERENCES proceso (id) ON DELETE CASCADE,
  CONSTRAINT fk_perfil_dimension FOREIGN KEY (dimension_id) REFERENCES dimension_evaluada (id),
  CONSTRAINT ck_perfil_valor CHECK (valor_ideal BETWEEN 1 AND 10)
);
CREATE INDEX ix_perfil_dimension ON perfil_requerido (dimension_id);



-- =====================================================================
-- 6. POSTULACIONES Y EJECUCION DEL PROCESO
-- =====================================================================

CREATE TABLE postulacion (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  codigo VARCHAR(20) NOT NULL,
  proceso_id BIGINT NOT NULL,
  persona_id BIGINT NOT NULL,
  registrada_por BIGINT NULL,
  estado VARCHAR(11) NOT NULL DEFAULT 'INSCRITA' CHECK (estado IN ('INSCRITA','EN_PROCESO','APROBADA','NO_APROBADA','RETIRADA')),
  motivo_cierre VARCHAR(500) NULL,
  etiqueta VARCHAR(24) CHECK (etiqueta IN ('ALTAMENTE_RECOMENDADO','RECOMENDADO','RECOMENDADO_CON_RESERVAS','NO_RECOMENDADO')),
  recomendacion TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_postulacion_codigo UNIQUE (codigo),
  CONSTRAINT uq_postulacion_proceso_persona UNIQUE (proceso_id, persona_id),
  CONSTRAINT fk_postulacion_proceso        FOREIGN KEY (proceso_id)     REFERENCES proceso (id),
  CONSTRAINT fk_postulacion_persona        FOREIGN KEY (persona_id)     REFERENCES persona (id),
  CONSTRAINT fk_postulacion_registrada_por FOREIGN KEY (registrada_por) REFERENCES contacto_empresa (id)
);
CREATE INDEX ix_postulacion_persona ON postulacion (persona_id);
CREATE INDEX ix_postulacion_proceso_estado ON postulacion (proceso_id, estado);
CREATE INDEX ix_postulacion_registrada_por ON postulacion (registrada_por);
CREATE TRIGGER trg_postulacion_updated BEFORE UPDATE ON postulacion FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE documento_postulacion (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  postulacion_id BIGINT NOT NULL,
  tipo_documento VARCHAR(40) NOT NULL CHECK (tipo_documento IN
    ('HISTORICO_ACADEMICO','CEDULA_IDENTIDAD','FOTOGRAFIA','CURRICULUM_VITAE')),
  nombre_original VARCHAR(255) NOT NULL,
  clave_almacenamiento VARCHAR(400) NOT NULL UNIQUE,
  mime_type VARCHAR(100) NOT NULL,
  tamano_bytes BIGINT NOT NULL CHECK (tamano_bytes > 0),
  estado VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','VALIDADO','OBSERVADO','RECHAZADO')),
  es_vigente BOOLEAN NOT NULL DEFAULT TRUE,
  validado_por BIGINT NULL,
  validado_en TIMESTAMPTZ NULL,
  observacion VARCHAR(500) NULL,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT fk_documento_postulacion FOREIGN KEY (postulacion_id)    REFERENCES postulacion (id),
  CONSTRAINT fk_documento_validado    FOREIGN KEY (validado_por)      REFERENCES usuario (id)
);
CREATE INDEX ix_documento_postulacion ON documento_postulacion (postulacion_id);
CREATE INDEX ix_documento_tipo ON documento_postulacion (tipo_documento);

CREATE TABLE postulacion_etapa (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  postulacion_id BIGINT NOT NULL,
  proceso_etapa_id BIGINT NOT NULL,
  estado VARCHAR(11) NOT NULL DEFAULT 'BLOQUEADA' CHECK (estado IN ('BLOQUEADA','PENDIENTE','EN_CURSO','EN_REVISION','APROBADA','REPROBADA','OMITIDA')),
  asignado_a BIGINT NULL,
  asignado_en TIMESTAMPTZ NULL,
  iniciada_en TIMESTAMPTZ NULL,
  finalizada_en TIMESTAMPTZ NULL,
  motivo VARCHAR(500) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_postulacion_etapa UNIQUE (postulacion_id, proceso_etapa_id),
  CONSTRAINT fk_postulacion_etapa_postulacion FOREIGN KEY (postulacion_id)   REFERENCES postulacion (id),
  CONSTRAINT fk_postulacion_etapa_proceso     FOREIGN KEY (proceso_etapa_id) REFERENCES proceso_etapa (id),
  CONSTRAINT fk_postulacion_etapa_asignado    FOREIGN KEY (asignado_a)       REFERENCES usuario (id),
  CONSTRAINT ck_postulacion_etapa_fechas CHECK (finalizada_en IS NULL OR iniciada_en IS NULL OR finalizada_en >= iniciada_en)
);
CREATE INDEX ix_postulacion_etapa_bandeja ON postulacion_etapa (asignado_a, estado);
CREATE INDEX ix_postulacion_etapa_embudo ON postulacion_etapa (proceso_etapa_id, estado);
CREATE TRIGGER trg_postulacion_etapa_updated BEFORE UPDATE ON postulacion_etapa FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE historial_postulacion_etapa (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  postulacion_etapa_id BIGINT NOT NULL,
  estado_anterior VARCHAR(11) NULL CHECK (estado_anterior IN ('BLOQUEADA','PENDIENTE','EN_CURSO','EN_REVISION','APROBADA','REPROBADA','OMITIDA')),
  estado_nuevo VARCHAR(11) NOT NULL CHECK (estado_nuevo IN ('BLOQUEADA','PENDIENTE','EN_CURSO','EN_REVISION','APROBADA','REPROBADA','OMITIDA')),
  usuario_id BIGINT NULL,
  motivo VARCHAR(500) NULL,
  creado_en TIMESTAMPTZ(3) NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ(3) NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT fk_historial_postulacion_etapa FOREIGN KEY (postulacion_etapa_id) REFERENCES postulacion_etapa (id),
  CONSTRAINT fk_historial_usuario           FOREIGN KEY (usuario_id)           REFERENCES usuario (id) ON DELETE SET NULL
);
CREATE INDEX ix_historial_etapa ON historial_postulacion_etapa (postulacion_etapa_id, creado_en);


CREATE TABLE intento_prueba (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  postulacion_id BIGINT NOT NULL,
  proceso_prueba_id BIGINT NOT NULL,
  nro_intento SMALLINT NOT NULL DEFAULT 1,
  estado VARCHAR(10) NOT NULL DEFAULT 'EN_CURSO' CHECK (estado IN ('EN_CURSO','FINALIZADO','EXPIRADO','ANULADO')),
  iniciado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  finalizado_en TIMESTAMPTZ NULL,
  duracion_seg INTEGER GENERATED ALWAYS AS (EXTRACT(EPOCH FROM (finalizado_en - iniciado_en))::integer) STORED,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_intento UNIQUE (postulacion_id, proceso_prueba_id, nro_intento),
  CONSTRAINT uq_intento_id_post_prueba UNIQUE (id, postulacion_id, proceso_prueba_id),
  CONSTRAINT fk_intento_postulacion    FOREIGN KEY (postulacion_id)     REFERENCES postulacion (id),
  CONSTRAINT fk_intento_proceso_prueba FOREIGN KEY (proceso_prueba_id)  REFERENCES proceso_prueba (id),
  CONSTRAINT ck_intento_fechas CHECK (finalizado_en IS NULL OR finalizado_en >= iniciado_en)
);
CREATE INDEX ix_intento_proceso_prueba ON intento_prueba (proceso_prueba_id);

CREATE TABLE respuesta_intento (
  intento_id BIGINT NOT NULL,
  pregunta_id BIGINT NOT NULL,
  opcion_id BIGINT NULL,
  respondida_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (intento_id, pregunta_id),
  CONSTRAINT fk_respuesta_intento  FOREIGN KEY (intento_id)  REFERENCES intento_prueba (id) ON DELETE CASCADE,
  CONSTRAINT fk_respuesta_pregunta FOREIGN KEY (pregunta_id) REFERENCES pregunta (id),
  CONSTRAINT fk_respuesta_opcion   FOREIGN KEY (opcion_id, pregunta_id) REFERENCES opcion_pregunta (id, pregunta_id)
);
CREATE INDEX ix_respuesta_pregunta ON respuesta_intento (pregunta_id);
CREATE INDEX ix_respuesta_opcion ON respuesta_intento (opcion_id, pregunta_id);

CREATE TABLE resultado_prueba (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  postulacion_id BIGINT NOT NULL,
  proceso_prueba_id BIGINT NOT NULL,
  intento_id BIGINT NULL,
  puntaje_bruto DECIMAL(7,2) NOT NULL,
  puntaje_maximo DECIMAL(7,2) NOT NULL,
  puntaje_normalizado NUMERIC(5,2) GENERATED ALWAYS AS (ROUND(puntaje_bruto / puntaje_maximo * 100, 2)) STORED,
  umbral_aplicado DECIMAL(5,2) NULL,
  aprobada BOOLEAN NOT NULL,
  motivo_no_aprobacion VARCHAR(14) NULL CHECK (motivo_no_aprobacion IN ('PUNTAJE_BAJO','TIEMPO_MINIMO','TIEMPO_AGOTADO','OTRO')),
  evaluador_id BIGINT NULL,
  observaciones TEXT NULL,
  calificado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_resultado_postulacion_prueba UNIQUE (postulacion_id, proceso_prueba_id),
  CONSTRAINT uq_resultado_id_post_prueba UNIQUE (id, postulacion_id, proceso_prueba_id),
  CONSTRAINT fk_resultado_postulacion    FOREIGN KEY (postulacion_id)    REFERENCES postulacion (id),
  CONSTRAINT fk_resultado_proceso_prueba FOREIGN KEY (proceso_prueba_id) REFERENCES proceso_prueba (id),
  CONSTRAINT fk_resultado_evaluador      FOREIGN KEY (evaluador_id)      REFERENCES usuario (id),
  CONSTRAINT fk_resultado_intento FOREIGN KEY (intento_id, postulacion_id, proceso_prueba_id) REFERENCES intento_prueba (id, postulacion_id, proceso_prueba_id),
  CONSTRAINT ck_resultado_maximo CHECK (puntaje_maximo > 0),
  CONSTRAINT ck_resultado_bruto  CHECK (puntaje_bruto >= 0 AND puntaje_bruto <= puntaje_maximo),
  CONSTRAINT ck_resultado_motivo CHECK (NOT aprobada OR motivo_no_aprobacion IS NULL)
);
CREATE INDEX ix_resultado_proceso_prueba ON resultado_prueba (proceso_prueba_id);
CREATE INDEX ix_resultado_evaluador ON resultado_prueba (evaluador_id);

CREATE TABLE calificacion_criterio (
  resultado_prueba_id BIGINT NOT NULL,
  criterio_id INTEGER NOT NULL,
  puntaje DECIMAL(5,2) NOT NULL,
  comentario VARCHAR(500) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (resultado_prueba_id, criterio_id),
  CONSTRAINT fk_calificacion_resultado FOREIGN KEY (resultado_prueba_id) REFERENCES resultado_prueba (id) ON DELETE CASCADE,
  CONSTRAINT fk_calificacion_criterio  FOREIGN KEY (criterio_id)         REFERENCES rubrica_criterio (id),
  CONSTRAINT ck_calificacion_puntaje CHECK (puntaje >= 0)
);
CREATE INDEX ix_calificacion_criterio ON calificacion_criterio (criterio_id);

CREATE TABLE resultado_dimension (
  resultado_prueba_id BIGINT NOT NULL,
  dimension_id INTEGER NOT NULL,
  valor DECIMAL(4,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (resultado_prueba_id, dimension_id),
  CONSTRAINT fk_resultado_dimension_resultado FOREIGN KEY (resultado_prueba_id) REFERENCES resultado_prueba (id) ON DELETE CASCADE,
  CONSTRAINT fk_resultado_dimension_dimension FOREIGN KEY (dimension_id)        REFERENCES dimension_evaluada (id),
  CONSTRAINT ck_resultado_dimension_valor CHECK (valor BETWEEN 0 AND 10)
);
CREATE INDEX ix_resultado_dimension ON resultado_dimension (dimension_id);

-- =====================================================================
-- 8. SALIDAS: RANKING, INFORMES Y SOLICITUDES DE INFORMACION
-- =====================================================================

CREATE TABLE ranking_resultado (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  proceso_id BIGINT NOT NULL,
  postulacion_id BIGINT NOT NULL,
  posicion INTEGER NOT NULL,
  puntaje_total DECIMAL(5,2) NOT NULL,
  es_top BOOLEAN NOT NULL DEFAULT FALSE,
  visible BOOLEAN NOT NULL DEFAULT FALSE,
  publicado_en TIMESTAMPTZ NULL,
  publicado_por BIGINT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_ranking_postulacion UNIQUE (proceso_id, postulacion_id),
  CONSTRAINT uq_ranking_posicion UNIQUE (proceso_id, posicion),
  CONSTRAINT fk_ranking_proceso      FOREIGN KEY (proceso_id)     REFERENCES proceso (id),
  CONSTRAINT fk_ranking_postulacion  FOREIGN KEY (postulacion_id) REFERENCES postulacion (id),
  CONSTRAINT fk_ranking_publicado_por FOREIGN KEY (publicado_por) REFERENCES usuario (id),
  CONSTRAINT ck_ranking_puntaje CHECK (puntaje_total BETWEEN 0 AND 100)
);
CREATE INDEX ix_ranking_postulacion ON ranking_resultado (postulacion_id);
CREATE INDEX ix_ranking_publico ON ranking_resultado (visible, publicado_en);

CREATE TABLE informe_final (
  id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
  proceso_id BIGINT NOT NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','EMITIDO')),
  resumen TEXT NULL,
  recomendacion_general TEXT NULL,
  pdf_nombre VARCHAR(255),
  pdf_ruta VARCHAR(400),
  pdf_tamano BIGINT CHECK (pdf_tamano > 0),
  elaborado_por BIGINT NOT NULL,
  emitido_en TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (id),
  CONSTRAINT uq_informe_proceso UNIQUE (proceso_id),
  CONSTRAINT fk_informe_proceso   FOREIGN KEY (proceso_id)     REFERENCES proceso (id),
  CONSTRAINT fk_informe_elaborado FOREIGN KEY (elaborado_por)  REFERENCES usuario (id),
  CONSTRAINT ck_informe_emision CHECK (estado = 'BORRADOR' OR emitido_en IS NOT NULL)
);
CREATE INDEX ix_informe_elaborado_por ON informe_final (elaborado_por);




-- =====================================================================
-- 8b. INDICES UNICOS PARCIALES
-- =====================================================================

CREATE UNIQUE INDEX uq_contacto_principal ON contacto_empresa (empresa_id) WHERE es_principal;
CREATE UNIQUE INDEX uq_documento_vigente ON documento_postulacion (postulacion_id, tipo_documento) WHERE es_vigente;

-- =====================================================================
-- 9. TRIGGERS
-- =====================================================================

-- Comprueba la inscripcion sin depender de catalogos academicos separados.
CREATE OR REPLACE FUNCTION fn_postulacion_bi() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_proceso proceso%ROWTYPE;
BEGIN
  SELECT * INTO STRICT v_proceso FROM proceso WHERE id = NEW.proceso_id;
  IF v_proceso.estado <> 'ABIERTO' THEN
    RAISE EXCEPTION 'El proceso no esta abierto para inscripciones';
  END IF;
  IF NOT COALESCE((SELECT otorgado FROM consentimiento
       WHERE persona_id = NEW.persona_id AND tipo = 'TRATAMIENTO_DATOS'
       ORDER BY registrado_en DESC, id DESC LIMIT 1), FALSE) THEN
    RAISE EXCEPTION 'El candidato debe otorgar consentimiento de tratamiento de datos';
  END IF;
  IF v_proceso.tipo = 'TALENTO' AND v_proceso.carrera IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM persona WHERE id = NEW.persona_id AND carrera = v_proceso.carrera
      AND registro_universitario IS NOT NULL AND ppa IS NOT NULL) THEN
    RAISE EXCEPTION 'Los datos academicos no corresponden a la carrera del proceso';
  END IF;
  IF v_proceso.tipo = 'EMPRESA' AND NOT EXISTS (
    SELECT 1 FROM contacto_empresa origen JOIN contacto_empresa registrador
      ON origen.empresa_id = registrador.empresa_id
    WHERE origen.id = v_proceso.contacto_id AND registrador.id = NEW.registrada_por) THEN
    RAISE EXCEPTION 'El contacto no pertenece a la empresa solicitante';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_postulacion_bi BEFORE INSERT ON postulacion
  FOR EACH ROW EXECUTE FUNCTION fn_postulacion_bi();

CREATE OR REPLACE FUNCTION fn_postulacion_etapa_bi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (SELECT proceso_id FROM postulacion WHERE id = NEW.postulacion_id)
     IS DISTINCT FROM (SELECT proceso_id FROM proceso_etapa WHERE id = NEW.proceso_etapa_id) THEN
    RAISE EXCEPTION 'La etapa no pertenece al proceso de la postulacion';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_postulacion_etapa_bi BEFORE INSERT OR UPDATE ON postulacion_etapa
  FOR EACH ROW EXECUTE FUNCTION fn_postulacion_etapa_bi();

CREATE OR REPLACE FUNCTION fn_postulacion_etapa_hist() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  v_usuario BIGINT := NULLIF(current_setting('app.usuario_id', true), '')::bigint;
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO historial_postulacion_etapa (postulacion_etapa_id, estado_anterior, estado_nuevo, usuario_id, motivo)
    VALUES (NEW.id, NULL, NEW.estado, v_usuario, NEW.motivo);
  ELSIF OLD.estado IS DISTINCT FROM NEW.estado THEN
    INSERT INTO historial_postulacion_etapa (postulacion_etapa_id, estado_anterior, estado_nuevo, usuario_id, motivo)
    VALUES (NEW.id, OLD.estado, NEW.estado, v_usuario, NEW.motivo);
  END IF;
  RETURN NULL;
END $$;
CREATE TRIGGER trg_postulacion_etapa_ai AFTER INSERT ON postulacion_etapa
  FOR EACH ROW EXECUTE FUNCTION fn_postulacion_etapa_hist();
CREATE TRIGGER trg_postulacion_etapa_au AFTER UPDATE ON postulacion_etapa
  FOR EACH ROW EXECUTE FUNCTION fn_postulacion_etapa_hist();

CREATE OR REPLACE FUNCTION fn_intento_prueba_bi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (SELECT p.tipo FROM proceso_prueba pp JOIN prueba p ON p.id = pp.prueba_id
       WHERE pp.id = NEW.proceso_prueba_id) IS DISTINCT FROM 'TEST' THEN
    RAISE EXCEPTION 'Solo las pruebas de tipo TEST se rinden en intentos';
  END IF;
  IF (SELECT proceso_id FROM postulacion WHERE id = NEW.postulacion_id)
     IS DISTINCT FROM (SELECT pe.proceso_id FROM proceso_prueba pp JOIN proceso_etapa pe ON pe.id = pp.proceso_etapa_id
                        WHERE pp.id = NEW.proceso_prueba_id) THEN
    RAISE EXCEPTION 'La prueba no pertenece al proceso de la postulacion';
  END IF;
  IF NEW.nro_intento > (SELECT max_intentos FROM proceso_prueba WHERE id = NEW.proceso_prueba_id) THEN
    RAISE EXCEPTION 'Se supero el numero maximo de intentos de la prueba';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_intento_prueba_bi BEFORE INSERT OR UPDATE ON intento_prueba
  FOR EACH ROW EXECUTE FUNCTION fn_intento_prueba_bi();

CREATE OR REPLACE FUNCTION fn_respuesta_intento_bi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (SELECT prueba_id FROM pregunta WHERE id = NEW.pregunta_id)
     IS DISTINCT FROM (SELECT pp.prueba_id FROM intento_prueba i JOIN proceso_prueba pp ON pp.id = i.proceso_prueba_id
                        WHERE i.id = NEW.intento_id) THEN
    RAISE EXCEPTION 'La pregunta no pertenece a la prueba del intento';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_respuesta_intento_bi BEFORE INSERT OR UPDATE ON respuesta_intento
  FOR EACH ROW EXECUTE FUNCTION fn_respuesta_intento_bi();

CREATE OR REPLACE FUNCTION fn_resultado_prueba_bi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (SELECT proceso_id FROM postulacion WHERE id = NEW.postulacion_id)
     IS DISTINCT FROM (SELECT pe.proceso_id FROM proceso_prueba pp JOIN proceso_etapa pe ON pe.id = pp.proceso_etapa_id
                        WHERE pp.id = NEW.proceso_prueba_id) THEN
    RAISE EXCEPTION 'La prueba no pertenece al proceso de la postulacion';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_resultado_prueba_bi BEFORE INSERT OR UPDATE ON resultado_prueba
  FOR EACH ROW EXECUTE FUNCTION fn_resultado_prueba_bi();

CREATE OR REPLACE FUNCTION fn_calificacion_criterio_bi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (SELECT prueba_id FROM rubrica_criterio WHERE id = NEW.criterio_id)
     IS DISTINCT FROM (SELECT pp.prueba_id FROM resultado_prueba r JOIN proceso_prueba pp ON pp.id = r.proceso_prueba_id
                        WHERE r.id = NEW.resultado_prueba_id) THEN
    RAISE EXCEPTION 'El criterio no pertenece a la rubrica de la prueba';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_calificacion_criterio_bi BEFORE INSERT OR UPDATE ON calificacion_criterio
  FOR EACH ROW EXECUTE FUNCTION fn_calificacion_criterio_bi();

CREATE OR REPLACE FUNCTION fn_ranking_resultado_bi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF (SELECT proceso_id FROM postulacion WHERE id = NEW.postulacion_id) IS DISTINCT FROM NEW.proceso_id THEN
    RAISE EXCEPTION 'La postulacion no pertenece al proceso del ranking';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_ranking_resultado_bi BEFORE INSERT OR UPDATE ON ranking_resultado
  FOR EACH ROW EXECUTE FUNCTION fn_ranking_resultado_bi();

-- =====================================================================
-- 10. VISTAS
-- =====================================================================

CREATE OR REPLACE VIEW v_proceso_pesos AS
SELECT pe.proceso_id,
       ROUND(COALESCE(SUM(pp.peso), 0), 2)            AS suma_pesos,
       (ROUND(COALESCE(SUM(pp.peso), 0), 2) = 100.00) AS pesos_validos
  FROM proceso_etapa pe
  LEFT JOIN proceso_prueba pp ON pp.proceso_etapa_id = pe.id
 WHERE pe.activa
 GROUP BY pe.proceso_id;

CREATE OR REPLACE VIEW v_puntaje_postulacion AS
SELECT po.id AS postulacion_id,
       po.proceso_id,
       ROUND(SUM(r.puntaje_normalizado * pp.peso) / 100, 2) AS puntaje_total,
       COUNT(r.id)                                          AS pruebas_calificadas,
       (SELECT COUNT(*)
          FROM proceso_prueba pp2
          JOIN proceso_etapa pe2 ON pe2.id = pp2.proceso_etapa_id
         WHERE pe2.proceso_id = po.proceso_id AND pe2.activa) AS pruebas_configuradas
  FROM postulacion po
  JOIN resultado_prueba r ON r.postulacion_id = po.id
  JOIN proceso_prueba pp  ON pp.id = r.proceso_prueba_id
  JOIN proceso_etapa pe ON pe.id=pp.proceso_etapa_id AND pe.activa
 GROUP BY po.id, po.proceso_id;

CREATE OR REPLACE VIEW v_ranking_provisional AS
SELECT vp.proceso_id,
       vp.postulacion_id,
       vp.puntaje_total,
       RANK() OVER (PARTITION BY vp.proceso_id ORDER BY vp.puntaje_total DESC) AS posicion_calculada
  FROM v_puntaje_postulacion vp;

CREATE OR REPLACE VIEW v_bandeja_tareas AS
SELECT pt.id           AS tarea_id,
       pt.asignado_a,
       pt.estado,
       po.codigo       AS codigo_candidato,
       pr.codigo       AS codigo_proceso,
       pr.tipo         AS tipo_proceso,
       pe.codigo        AS etapa,
       pt.asignado_en,
       FLOOR(EXTRACT(EPOCH FROM (now() - COALESCE(pt.asignado_en, pt.iniciada_en, pt.updated_at))) / 86400)::integer AS dias_pendiente
  FROM postulacion_etapa pt
  JOIN postulacion  po ON po.id = pt.postulacion_id
  JOIN proceso      pr ON pr.id = po.proceso_id
  JOIN proceso_etapa pe ON pe.id = pt.proceso_etapa_id
 WHERE pe.ejecucion = 'MANUAL'
   AND pt.estado IN ('PENDIENTE','EN_REVISION');

CREATE OR REPLACE VIEW v_embudo_proceso AS
SELECT pe.proceso_id,
       pe.orden,
       pe.codigo AS etapa,
       pe.nombre AS etapa_nombre,
       COUNT(*) FILTER (WHERE pt.estado = 'APROBADA')                                AS aprobadas,
       COUNT(*) FILTER (WHERE pt.estado IN ('PENDIENTE','EN_CURSO','EN_REVISION'))   AS en_curso,
       COUNT(*) FILTER (WHERE pt.estado = 'REPROBADA')                               AS reprobadas
  FROM proceso_etapa pe
  LEFT JOIN postulacion_etapa pt ON pt.proceso_etapa_id = pe.id
 GROUP BY pe.id, pe.proceso_id, pe.orden, pe.codigo, pe.nombre;

-- Reclutado = persona con al menos una postulacion aprobada; no es evaluador.
CREATE OR REPLACE VIEW v_reclutados AS
SELECT po.id AS postulacion_id, po.proceso_id, po.persona_id, po.codigo,
       p.nombres, p.apellido_paterno, p.correo_contacto
FROM postulacion po JOIN persona p ON p.id = po.persona_id
WHERE po.estado = 'APROBADA';

-- La condicion de reclutado pertenece al proceso, no concede permisos de evaluador.
CREATE OR REPLACE FUNCTION fn_aprobar_postulacion() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE v_evaluador BIGINT := NULLIF(current_setting('app.usuario_id', true), '')::bigint;
BEGIN
  IF NEW.estado = 'APROBADA' AND OLD.estado IS DISTINCT FROM NEW.estado THEN
    IF OLD.estado <> 'EN_PROCESO' OR NOT EXISTS (
      SELECT 1 FROM usuario u JOIN rol r ON r.id=u.rol_id WHERE u.id=v_evaluador
        AND u.estado='ACTIVO' AND r.codigo IN ('ADMIN','AYUDANTE')) THEN
      RAISE EXCEPTION 'Se requiere una postulacion en proceso y un evaluador activo';
    END IF;
    IF NOT COALESCE((SELECT pesos_validos FROM v_proceso_pesos WHERE proceso_id=NEW.proceso_id),FALSE) THEN
      RAISE EXCEPTION 'Los pesos del plan activo deben sumar 100';
    END IF;
    IF EXISTS (
      SELECT 1 FROM proceso_prueba pp JOIN proceso_etapa pe ON pe.id=pp.proceso_etapa_id
      LEFT JOIN resultado_prueba r ON r.proceso_prueba_id=pp.id AND r.postulacion_id=NEW.id
      WHERE pe.proceso_id=NEW.proceso_id AND pe.activa AND pp.obligatoria
        AND (r.id IS NULL OR NOT r.aprobada)) THEN
      RAISE EXCEPTION 'Faltan pruebas obligatorias aprobadas';
    END IF;
    IF EXISTS (
      SELECT 1 FROM proceso_etapa pe LEFT JOIN postulacion_etapa pt
        ON pt.proceso_etapa_id=pe.id AND pt.postulacion_id=NEW.id
      WHERE pe.proceso_id=NEW.proceso_id AND pe.activa AND pe.es_eliminatoria
        AND (pt.id IS NULL OR pt.estado <> 'APROBADA')) THEN
      RAISE EXCEPTION 'Faltan etapas eliminatorias aprobadas';
    END IF;
    IF EXISTS (
      SELECT 1 FROM proceso_prueba pp JOIN proceso_etapa pe ON pe.id=pp.proceso_etapa_id
      JOIN rubrica_criterio rc ON rc.prueba_id=pp.prueba_id
      LEFT JOIN resultado_prueba r ON r.proceso_prueba_id=pp.id AND r.postulacion_id=NEW.id
      LEFT JOIN calificacion_criterio cc ON cc.resultado_prueba_id=r.id AND cc.criterio_id=rc.id
      WHERE pe.proceso_id=NEW.proceso_id AND pe.activa AND pp.obligatoria AND cc.criterio_id IS NULL) THEN
      RAISE EXCEPTION 'Faltan criterios de rubrica por calificar';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM documento_postulacion WHERE postulacion_id=NEW.id
      AND tipo_documento='CURRICULUM_VITAE' AND es_vigente AND estado='VALIDADO') THEN
      RAISE EXCEPTION 'Se requiere un curriculum vigente y validado';
    END IF;
    IF NOT COALESCE((SELECT otorgado FROM consentimiento WHERE persona_id=NEW.persona_id
        AND tipo='TRATAMIENTO_DATOS' ORDER BY registrado_en DESC,id DESC LIMIT 1),FALSE) THEN
      RAISE EXCEPTION 'El consentimiento de tratamiento de datos no esta vigente';
    END IF;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_aprobar_postulacion BEFORE UPDATE ON postulacion
  FOR EACH ROW EXECUTE FUNCTION fn_aprobar_postulacion();

-- =====================================================================
-- 11. DATOS INICIALES (catalogos fijos del sistema)
-- =====================================================================

INSERT INTO rol (codigo, nombre, descripcion) VALUES
  ('ADMIN',             'Administrador de la consultora', 'Configura procesos, pruebas, ranking y solicitudes'),
  ('AYUDANTE',          'Ayudante / junior de carrera',   'Revisa tareas manuales: CV, entrevista y antecedentes'),
  ('CANDIDATO',         'Candidato',                      'Participa en procesos de seleccion'),
  ('RECLUTADO',         'Reclutado',                      'Candidato con al menos una postulacion aprobada'),
  ('EMPRESA',           'Empresa cliente',                'Solicita evaluaciones y recibe informes')
ON CONFLICT (codigo) DO NOTHING;


INSERT INTO dimension_evaluada (codigo, nombre, tipo, orden) VALUES
  ('SOCIABILIDAD',          'Sociabilidad',          'PERSONALIDAD',  1),
  ('CAPACIDAD_ESCOLAR',     'Capacidad escolar',     'PERSONALIDAD',  2),
  ('ESTABILIDAD_EMOCIONAL', 'Estabilidad emocional', 'PERSONALIDAD',  3),
  ('SUMISION',              'Sumisión',              'PERSONALIDAD',  4),
  ('RETRAIMIENTO',          'Retraimiento',          'PERSONALIDAD',  5),
  ('SUPEREGO',              'Superego',              'PERSONALIDAD',  6),
  ('TIMIDEZ',               'Timidez',               'PERSONALIDAD',  7),
  ('SEVERIDAD',             'Severidad',             'PERSONALIDAD',  8),
  ('CONFIANZA',             'Confianza',             'PERSONALIDAD',  9),
  ('OBJETIVIDAD',           'Objetividad',           'PERSONALIDAD', 10),
  ('INGENUIDAD',            'Ingenuidad',            'PERSONALIDAD', 11),
  ('SEGURIDAD_DE_SI_MISMO', 'Seguridad de sí mismo', 'PERSONALIDAD', 12),
  ('CONSERVADURISMO',       'Conservadurismo',       'PERSONALIDAD', 13),
  ('DEPENDENCIA_GRUPAL',    'Dependencia grupal',    'PERSONALIDAD', 14),
  ('INDIFERENCIA',          'Indiferencia',          'PERSONALIDAD', 15),
  ('TRANQUILIDAD',          'Tranquilidad',          'PERSONALIDAD', 16)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO prueba (codigo, nombre, tipo, descripcion) VALUES
  ('PPA',                        'Promedio ponderado histórico',   'CALCULADA', 'Compara el PPA de la persona con el minimo configurado en el plan'),
  ('CV_ANALISIS',                'Análisis de currículum',         'RUBRICA',   'Revisión manual con rúbrica'),
  ('TEST_PERSONALIDAD',          'Test de personalidad',           'TEST',      'Obligatorio; genera el gráfico de perfil'),
  ('TEST_INTELIGENCIA',          'Test de inteligencia',           'TEST',      'Obligatorio; matrices de figuras'),
  ('TEST_INTELIGENCIA_EMOCIONAL','Test de inteligencia emocional', 'TEST',      'Obligatorio'),
  ('TEST_LECTURA_COMPRENSIVA',   'Test de lectura comprensiva',    'TEST',      NULL),
  ('TEST_APTITUD_NUMERICA',      'Test de aptitud numérica',       'TEST',      NULL),
  ('TEST_HABILIDADES_BLANDAS',   'Test de habilidades blandas',    'TEST',      'Genera el gráfico de habilidades'),
  ('ENTREVISTA',                 'Entrevista',                     'RUBRICA',   'Formulario completado por el junior de la carrera')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rubrica_criterio (prueba_id, nombre, puntaje_maximo, orden)
SELECT p.id, c.nombre, 10.00, c.orden
  FROM prueba p
  CROSS JOIN (SELECT 'Formación' AS nombre, 1 AS orden UNION ALL SELECT 'Experiencia', 2
        UNION ALL SELECT 'Habilidades', 3 UNION ALL SELECT 'Presentación', 4) c
 WHERE p.codigo = 'CV_ANALISIS'
ON CONFLICT (prueba_id, nombre) DO NOTHING;



COMMIT;

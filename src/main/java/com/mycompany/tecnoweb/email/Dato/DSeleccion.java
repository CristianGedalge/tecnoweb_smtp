package com.mycompany.tecnoweb.email.Dato;

import com.mycompany.tecnoweb.email.Conexion.ConexionDB;
import java.sql.*;
import java.util.*;

/** Operaciones complementarias del modelo reducido, sin tablas auxiliares. */
public class DSeleccion {
    public int ejecutar(String sql, Object... valores) throws SQLException {
        try (Connection c = ConexionDB.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) p.setObject(i+1, valores[i]);
            return p.executeUpdate();
        }
    }

    public List<String[]> consultar(String sql, Object... valores) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        try (Connection c = ConexionDB.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) p.setObject(i+1, valores[i]);
            try (ResultSet r = p.executeQuery()) {
                int n = r.getMetaData().getColumnCount();
                String[] titulos = new String[n];
                for (int i=0; i<n; i++) titulos[i] = r.getMetaData().getColumnLabel(i+1);
                filas.add(titulos);
                while (r.next()) {
                    String[] fila = new String[n];
                    for (int i=0; i<n; i++) fila[i] = r.getString(i+1);
                    filas.add(fila);
                }
            }
        }
        return filas;
    }

    public long crearConvocatoria(String codigo, String nombre, String carrera, long admin) throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO proceso(codigo,tipo,nombre,carrera,creado_por,estado) "
                        + "SELECT UPPER(?),'TALENTO',?,?,u.id,'ABIERTO' FROM usuario u JOIN rol r ON r.id=u.rol_id "
                        + "WHERE u.id=? AND r.codigo='ADMIN' AND u.estado='ACTIVO' RETURNING id")) {
                    p.setString(1,codigo); p.setString(2,nombre); p.setString(3,carrera); p.setLong(4,admin);
                    try (ResultSet r=p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Se requiere un administrador activo.");
                        id=r.getLong(1);
                    }
                }
                DCargo.crearEtapas(c,id);
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void finalizar(long id, long evaluador) throws SQLException {
        try (Connection c=ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p=c.prepareStatement("SELECT set_config('app.usuario_id',?,true)")) {
                    p.setString(1,Long.toString(evaluador)); p.execute();
                }
                // El trigger valida plan, resultados, documentos y etapas antes de aprobar.
                try (PreparedStatement p=c.prepareStatement(
                        "UPDATE postulacion SET estado='APROBADA',motivo_cierre='Proceso aprobado' WHERE id=? AND estado='EN_PROCESO'")) {
                    p.setLong(1,id);
                    if (p.executeUpdate()!=1) throw new SQLException("La postulacion no existe o no esta en proceso.");
                }
                try (PreparedStatement p=c.prepareStatement(
                        "UPDATE postulacion_etapa pt SET estado='APROBADA',finalizada_en=now(),motivo='Seleccion confirmada' "
                        + "FROM proceso_etapa pe WHERE pe.id=pt.proceso_etapa_id AND pe.codigo='SELECCION' AND pt.postulacion_id=?")) {
                    p.setLong(1,id); p.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }
}

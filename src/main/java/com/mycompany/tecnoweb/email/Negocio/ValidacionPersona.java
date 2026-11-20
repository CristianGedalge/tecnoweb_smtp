package com.mycompany.tecnoweb.email.Negocio;

import java.text.Normalizer;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

/** Reglas de los campos personales; no se aplican a contrasenas ni texto libre. */
public final class ValidacionPersona {
    private ValidacionPersona() {}

    public static String nombre(String valor, String campo, int maximo) {
        String texto=Normalizer.normalize(NUsuario.exigirTexto(valor,campo),Normalizer.Form.NFC);
        texto=texto.replaceAll("[\\p{Zs}\\t]+"," ");
        if (!texto.matches("\\p{L}[\\p{L}\\p{M}]*(?:[ '\u2019-]\\p{L}[\\p{L}\\p{M}]*)*"))
            throw new IllegalArgumentException("El campo '"+campo+"' solo admite letras, espacios, apostrofes y guiones.");
        if (texto.length()>maximo)
            throw new IllegalArgumentException("El campo '"+campo+"' admite hasta "+maximo+" caracteres.");
        return texto;
    }

    public static String ci(String valor) {
        String texto=NUsuario.exigirTexto(valor,"ci");
        if (!texto.matches("[0-9]{1,15}")) throw new IllegalArgumentException("El CI debe contener de 1 a 15 digitos.");
        return texto;
    }

    public static String telefono(String valor,String campo) {
        String texto=NUsuario.exigirTexto(valor,campo);
        if (!texto.matches("[+0-9 ()-]{1,20}") || !texto.matches(".*[0-9].*"))
            throw new IllegalArgumentException("El campo '"+campo+"' debe contener digitos y solo admite +, espacios, parentesis y guiones (maximo 20 caracteres).");
        return texto;
    }

    public static String correo(String valor) {
        String texto=NUsuario.exigirTexto(valor,"correo");
        try {
            if (texto.length()>190 || !texto.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+")) throw new AddressException();
            InternetAddress direccion=new InternetAddress(texto,true);
            direccion.validate();
            if (!texto.equals(direccion.getAddress())) throw new AddressException();
        } catch (AddressException ex) { throw new IllegalArgumentException("Correo de contacto no valido."); }
        return texto;
    }

    public static String ocupacion(String valor) {
        String texto=Normalizer.normalize(NUsuario.exigirTexto(valor,"ocupacion"),Normalizer.Form.NFC);
        if (texto.length()>100 || !texto.matches("[\\p{L}\\p{M}0-9 .,'\u2019()/&-]+") || !texto.matches(".*\\p{L}.*"))
            throw new IllegalArgumentException("La ocupacion debe incluir letras y solo admite letras, numeros, espacios y . , ' / ( ) & - (maximo 100 caracteres).");
        return texto;
    }
}

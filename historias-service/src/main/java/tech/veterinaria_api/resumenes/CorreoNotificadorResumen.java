package tech.veterinaria_api.resumenes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import tech.veterinaria_api.common.ServicioRemotoException;

@Component
public class CorreoNotificadorResumen implements NotificadorResumen {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public CorreoNotificadorResumen(ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.correo.remitente:no-responder@veterinaria.local}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String emailDestino, String nombreDestinatario, String nombreMascota,
            ContenidoResumen contenido) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new ServicioRemotoException("El envío de correo no está configurado (spring.mail.host)");
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(emailDestino);
        mensaje.setSubject("Resumen de la consulta de " + nombreMascota);
        mensaje.setText("""
                Hola, %s:

                Este es el resumen de la consulta de %s, revisado por tu veterinario.

                Qué encontramos
                %s

                Tratamiento
                %s

                Cuidados en casa
                %s

                Cuándo volver
                %s

                Si tienes dudas, comunícate con la clínica.
                """.formatted(nombreDestinatario, nombreMascota, contenido.hallazgos(), contenido.tratamiento(),
                contenido.cuidadosEnCasa(), contenido.proximaVisita()));
        try {
            sender.send(mensaje);
        } catch (MailException e) {
            // Sin el destinatario en el mensaje (dato personal).
            throw new ServicioRemotoException("No se pudo enviar el correo del resumen", e);
        }
    }
}

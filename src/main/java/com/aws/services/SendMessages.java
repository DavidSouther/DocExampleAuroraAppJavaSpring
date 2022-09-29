package com.aws.services;

import com.aws.rest.App;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

@Component
public class SendMessages {
    private static String sender = "dpsouth+aws-rest@amazon.com";
    private static String subject = "Weekly AWS Status Report";
    private static String bodyText = "Hello,\r\n\r\nPlease see the attached file for a weekly update.";
    private static String bodyHTML = "<!DOCTYPE html><html lang=\"en-US\"><body><h1>Hello!</h1><p>Please see the attached file for a weekly update.</p></body></html>";
    private static String attachmentName = "WorkReport.xls";

    public void sendReport(InputStream is, String emailAddress) throws IOException {
        byte[] fileContent = IOUtils.toByteArray(is);

        try {
            send(makeEmail(fileContent, emailAddress));
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void send(MimeMessage message) throws MessagingException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        ByteBuffer buf = ByteBuffer.wrap(outputStream.toByteArray());
        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        SdkBytes data = SdkBytes.fromByteArray(arr);
        RawMessage rawMessage = RawMessage.builder().data(data).build();
        SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build();

        try {
            System.out.println("Attempting to send an email through Amazon SES...");
            SesClient client = SesClient.builder().region(App.region).build();
            client.sendRawEmail(rawEmailRequest);
        } catch (SesException e) {
            e.printStackTrace();
        }
    }

    private MimeMessage makeEmail(byte[] attachment, String emailAddress) throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);

        message.setSubject(subject, "UTF-8");
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(bodyText, "text/plain; charset=UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(bodyHTML, "text/html; charset=UTF-8");

        MimeMultipart msgBody = new MimeMultipart("alternative");
        msgBody.addBodyPart(textPart);
        msgBody.addBodyPart(htmlPart);

        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(msgBody);

        MimeMultipart msg = new MimeMultipart("mixed");
        msg.addBodyPart(wrap);

        MimeBodyPart att = new MimeBodyPart();
        DataSource fds = new ByteArrayDataSource(attachment, "application/vnc.openxmlformats-officedocument.spreadsheetml.sheet");
        att.setDataHandler(new DataHandler(fds));
        att.setFileName(attachmentName);

        msg.addBodyPart(att);

        message.setContent(msg);

        return message;
    }
}

package fr.progica.dto;

import lombok.Data;

@Data
public class MailDto {
    private String to;
    private String subject;
    private String text;

    public MailDto(String to, String subject, String text) {
        this.to = to;
        this.subject = subject;
        this.text = text;
    }
}
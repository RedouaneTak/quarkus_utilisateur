package fr.progica.dto;

import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
public class UtilisateurDto {

    @Schema(required = true, example ="MyLogin")
    private String login;
    @Schema(required = true, example ="redouane.takdjerad@gmail.com")
    private String email;
    @Schema(required = true, minLength =8, maxLength =20, example ="My%3Password")
    private String password;

    @Override
    public String toString() {
        return
                login + email + password;
    }
}

package fr.progica.endpoints;




import fr.progica.Validator;
import fr.progica.dto.MailDto;
import fr.progica.dto.UtilisateurDto;
import fr.progica.entities.UtilisateurEntity;
import fr.progica.restclient.MailClient;
import fr.progica.repositories.UserRepository;
import fr.progica.security.SecurityTools;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;


import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Path("/users")
@Tag(name="Users")
@Authenticated
public class UserService {

    @Inject
    JsonWebToken jsonWebToken;

    @Inject
    @Claim(standard = Claims.groups)
    String role;

    @Inject
    UserRepository userRepository;
    @Context
    UriInfo uriInfo;
    @Inject
    @RestClient
    MailClient mailClient;


    @POST
    @PermitAll
    @Path("/authentification")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getToken(@HeaderParam("login") String login, @HeaderParam("password") String password){
        UtilisateurEntity utilisateur=userRepository.findById(login);
        if (utilisateur == null){
            return Response.ok("login inconnu !").status(404).build();
        }
        if(BcryptUtil.matches(password, utilisateur.getPassword())){

            String token = SecurityTools.getToken(utilisateur);

            return Response.ok().header("Authorization",token).build();

        }
        return Response.ok().status(Response.Status.FORBIDDEN).build();
    }
    @PermitAll
    @Transactional
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response insert(UtilisateurDto utilisateur) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path("users").path("confirm");
        if(userRepository.findById(utilisateur.getLogin()) != null)
            return Response.status(417, "Ce login existe deja, veuillez choisir un autre!").build();
        if(!Validator.isValidMail(utilisateur.getEmail()))
            return Response.status( 400,"Le mail  n'est pas au bon format!").build();
        if(!Validator.isValidPassword(utilisateur.getPassword()))
            return Response.status(400, "Le password n'est pas correct !").build();


        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);
        Date expiration = calendar.getTime();
        String url = String.format("%s|%s|%s|%s|%s",
                utilisateur.getLogin(),
                utilisateur.getEmail(),
                BcryptUtil.bcryptHash(utilisateur.getPassword(), 31, "MyPersonalSalt01".getBytes()),
                "user",
                new SimpleDateFormat("dd-MM-yy-HH:mm:ss").format(expiration));
        String urlEncode = SecurityTools.encrypt(url);
        uriBuilder.queryParam("code",urlEncode.toString());
        //uriBuilder.path(URI.create(urlEncode).toString());

        StringBuilder body = new StringBuilder("Veuiller cliquer sur le lien suivant pour confirmer la création de votre compte.");
        body.append(uriBuilder.build());
        MailDto mailDto = new MailDto(utilisateur.getEmail(), "Confirmation de compte", body.toString());

        if (utilisateur == null)
            return Response.status(Response.Status.BAD_REQUEST).build();


        mailClient.sendMail(mailDto);
        return Response.ok().status(200).build();
    }
    @PermitAll
    @Transactional
    @GET
    @Path("/confirm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmMail(@QueryParam("code") String code) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ParseException {

        String []urldecode = SecurityTools.decrypt(code).split("\\|");
        UtilisateurEntity utilisateur = new UtilisateurEntity();
        utilisateur.setLogin(urldecode[0]);
        utilisateur.setMail(urldecode[1]);
        utilisateur.setPassword(urldecode[2]);
        utilisateur.setRole(urldecode[3]);
        try{
            Date expireAt=  new SimpleDateFormat("dd-MM-yy-HH:mm:ss").parse(urldecode[4]);
            if(expireAt.before(Calendar.getInstance().getTime())){

                Response.ok("Le lien n'est plus valide !!!!!!").status(400,"Le lien n'est plus valide !!!!!!");
            }
        }
        catch(ParseException e){
            Response.ok("Le lien n'est plus valide !!!!!!").status(400,"Le lien n'est plus valide !!!!!!");
        }
        userRepository.persist(utilisateur);
        return  Response.ok("Compte activé").build();


    }
}

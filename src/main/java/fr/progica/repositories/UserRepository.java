package fr.progica.repositories;

import fr.progica.entities.UtilisateurEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class UserRepository implements PanacheRepositoryBase <UtilisateurEntity, String> {

}

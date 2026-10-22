package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class Author {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String observations = "";

	@Column(nullable = false)
	private String login;

	/**
	 * Password hash, see {@link utils.web.PasswordHasher}
	 */
	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String mail;

	/** Registration number (RM). Nullable so the authors saved before it existed can still be loaded. */
	private String registration;

	/** Returns the registration number, or an empty text when it was never informed. */
	public String getRegistration() {
		return registration == null ? "" : registration;
	}

	/** Sets the registration number. */
	public void setRegistration(String registration) {
		this.registration = registration;
	}

	/** Returns the mail. */
	public String getMail() {
		return mail;
	}

	/** Sets the mail. */
	public void setMail(String mail) {
		this.mail = mail;
	}

	/** Returns the login. */
	public String getLogin() {
		return login;
	}

	/** Sets the login. */
	public void setLogin(String login) {
		this.login = login;
	}

	/** Returns the password. */
	public String getPassword() {
		return password;
	}

	/** Sets the password. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** Returns the id. */
	public long getId() {
		return id;
	}

	/** Sets the id. */
	public void setId(long identifier) {
		this.id = identifier;
	}

	/** Returns the name. */
	public String getName() {
		return name;
	}

	/** Sets the name. */
	public void setName(String name) {
		this.name = name;
	}

	/** Returns the observations. */
	public String getObservations() {
		return observations;
	}

	/** Sets the observations. */
	public void setObservations(String observations) {
		this.observations = observations;
	}

}

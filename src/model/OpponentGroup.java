/**
 * 
 */
package model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class OpponentGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String name;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "groupId")
	private Opponent[] opponents;

	public OpponentGroup(int amountOfOpponents, String name) {
		this.opponents = new Opponent[amountOfOpponents];
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Opponent[] getOpponents() {
		return opponents;
	}

	public void setOpponents(Opponent[] opponents) {
		this.opponents = opponents;
	}
}

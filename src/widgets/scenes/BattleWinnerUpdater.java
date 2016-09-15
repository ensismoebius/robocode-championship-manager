package widgets.scenes;

import javax.persistence.EntityManager;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import model.Opponent;
import utils.database.Database;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleWinnerUpdater extends Scene {

	private Opponent opponent1;
	private Opponent opponent2;

	private HBox hbox;
	private ToggleGroup radioGroup;

	private RadioButton rbOpponent1;
	private RadioButton rbOpponent2;

	public BattleWinnerUpdater() {
		super(new HBox());

		this.hbox = new HBox(10);

		this.radioGroup = new ToggleGroup();

		this.rbOpponent1 = new RadioButton();
		this.rbOpponent1.setToggleGroup(radioGroup);

		this.rbOpponent2 = new RadioButton();
		this.rbOpponent2.setToggleGroup(radioGroup);

		Button updateButton = new Button("Atualizar ganhador!");

		updateButton.setOnAction(event -> {
			EntityManager em = Database.getEntityManager();

			em.getTransaction().begin();

			if (this.rbOpponent1.isSelected()) {
				this.opponent1.setLevel(this.opponent1.getLevel() + 1);

				try {
					em.persist((Opponent) this.opponent1.clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}

			if (this.rbOpponent2.isSelected()) {
				this.opponent2.setLevel(this.opponent2.getLevel() + 1);

				try {
					em.persist((Opponent) this.opponent2.clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}

			em.getTransaction().commit();
			em.close();

		});

		this.hbox.getChildren().addAll(this.rbOpponent1, this.rbOpponent2, updateButton);
		this.setRoot(this.hbox);
	}

	public void setOpponents(Opponent opponent1, Opponent opponent2) {
		this.opponent1 = opponent1;
		this.opponent2 = opponent2;

		this.rbOpponent1.setText(opponent1.getName());
		this.rbOpponent2.setText(opponent2.getName());
	}
}

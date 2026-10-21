package support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Finds the fields of a dialog and changes them, the way the operator does. The
 * calls must run on the JavaFX thread, inside {@link FxSupport#onNextDialog}.
 */
public final class DialogFields {

	private DialogFields() {
	}

	/** Every node of the dialog, including the content of the tabs, which is not in the scene graph until it is selected. */
	public static List<Node> nodes(Node root) {
		List<Node> all = new ArrayList<Node>();
		collect(root, all);
		return all;
	}

	private static void collect(Node node, List<Node> all) {
		all.add(node);
		if (node instanceof TabPane tabs) {
			for (Tab tab : tabs.getTabs()) {
				collect(tab.getContent(), all);
			}
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				collect(child, all);
			}
		}
	}

	/** The first node of the dialog that matches, or null. */
	public static Node find(Node root, Predicate<Node> match) {
		return nodes(root).stream().filter(match).findFirst().orElse(null);
	}

	/** The input under the caption of a field of the form: the caption is the first label of a box with two children. */
	public static Node input(Node panel, String caption) {
		for (Node node : nodes(panel)) {
			if (node instanceof VBox box && box.getChildren().size() == 2 && box.getChildren().get(0) instanceof Label label && caption.equals(label.getText())) {
				return box.getChildren().get(1);
			}
		}
		throw new IllegalStateException("no field named " + caption);
	}

	/** Types the text in the field under the caption, the first text field of a row when the input is a row. */
	public static void text(Node panel, String caption, String value) {
		Node input = input(panel, caption);
		((TextField) (input instanceof HBox row ? row.getChildren().get(0) : input)).setText(value);
	}

	/** Selects the option of the choice under the caption. */
	public static void choose(Node panel, String caption, int index) {
		((ChoiceBox<?>) input(panel, caption)).getSelectionModel().select(index);
	}

	/** Ticks or unticks the box under the caption. */
	public static void check(Node panel, String caption, boolean selected) {
		((CheckBox) input(panel, caption)).setSelected(selected);
	}

	/** The button of the dialog with the text. */
	public static Button button(Node panel, String text) {
		return nodes(panel).stream().filter(node -> node instanceof Button button && text.equals(button.getText())).map(Button.class::cast).findFirst().orElseThrow();
	}
}

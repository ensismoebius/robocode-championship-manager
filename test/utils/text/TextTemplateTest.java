package utils.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

class TextTemplateTest {

	private static TextTemplate template(String text) throws IOException {
		return new TextTemplate(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void replacesSeveralPlaceholdersPerLine() throws IOException {
		TextTemplate t = template("<!-- begin:main -->\n<p>{a} and {b}</p>\n<!-- end:main -->\n");
		t.assign("a", "one");
		t.assign("b", "$2 \\ two");
		t.parse("main");

		assertTrue(t.out("main").contains("<p>one and $2 \\ two</p>"), t.out("main"));
	}

	@Test
	void repeatsBlocks() throws IOException {
		TextTemplate t = template("<!-- begin:main -->\n<ul>\n<!-- begin:item -->\n<li>{name}</li>\n<!-- end:item -->\n</ul>\n<!-- end:main -->\n");
		for (String name : new String[] { "x", "y" }) {
			t.assign("name", name);
			t.parse("item");
		}
		t.parse("main");

		String out = t.out("main");
		assertTrue(out.contains("<li>x</li>") && out.contains("<li>y</li>"), out);
	}

	@Test
	void leavesJavascriptBracesAlone() throws IOException {
		TextTemplate t = template("<!-- begin:main -->\nfunction f() {\nreturn {};\n}\n<!-- end:main -->\n");
		t.parse("main");

		String out = t.out("main");
		assertTrue(out.contains("function f() {") && out.contains("return {};"), out);
		assertFalse(out.contains("null"));
	}

	@Test
	void theAssignmentsCanBeReadAndReplaced() throws IOException {
		TextTemplate template = new TextTemplate(new ByteArrayInputStream("<!-- begin:bloco --> oi <!-- end:bloco -->".getBytes(StandardCharsets.UTF_8)));
		template.assign("nome", "Ana");
		assertEquals("Ana", template.getAllAssignments().get("nome"));

		HashMap<String, Object> replaced = new HashMap<String, Object>();
		replaced.put("nome", "Bia");
		template.setAllAssignments(replaced);
		assertEquals("Bia", template.getAllAssignments().get("nome"));
		assertEquals(1, template.getAllAssignments().size());
	}

	@Test
	void aNestedBlockNotParsedYetLeavesNoText() throws IOException {
		TextTemplate t = template("<!-- begin:main -->\n<ul>\n<!-- begin:item -->\n<li>{name}</li>\n<!-- end:item -->\n</ul>\n<!-- end:main -->\n");
		t.assign("name", "x");

		// The item is parsed by its own name, before it is shown: until then it has no text
		t.parse("main");

		assertFalse(t.out("main").contains("<li>"), t.out("main"));
	}

	@Test
	void textAfterTheBlockOpenerIsTheFirstTextOfTheBlock() throws IOException {
		TextTemplate t = template("<!-- begin:main -->primeiro\n<p>segundo</p>\n<!-- end:main -->\n");

		t.parse("main");

		String out = t.out("main");
		assertTrue(out.contains("primeiro"), out);
		assertTrue(out.indexOf("primeiro") < out.indexOf("segundo"), out);
	}

	@Test
	void aBlockThatIsNotInTheTemplateCanNotBeParsed() throws IOException {
		TextTemplate template = new TextTemplate(new ByteArrayInputStream("<!-- begin:bloco --> oi <!-- end:bloco -->".getBytes(StandardCharsets.UTF_8)));

		assertThrows(IllegalArgumentException.class, () -> template.parse("ausente"));
	}
}

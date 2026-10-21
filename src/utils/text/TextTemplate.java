package utils.text;

/**
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This program is licensed under GPLv3 see the README file for more information
 * JSP is a better tecnology than this,but looks like jetty do not give support
 * for this so i have to use this workaround
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class TextTemplate {

	private static final String BEGIN_PARSED = "begin:PARSED";
	private static final String END_PARSED = "end:PARSED";

	private ArrayList<HashMap<String, Object>> template;
	private HashMap<String, Object> assignments;

	/** Loads a template from the stream, with no values assigned yet. */
	public TextTemplate(InputStream inputStream) throws IOException {
		this.assignments = new HashMap<String, Object>();
		this.template = new TextTemplatePreprocessor().loadTemplate(inputStream);
	}

	/** Returns the all assignments. */
	public HashMap<String, Object> getAllAssignments() {
		return assignments;
	}

	/** Replaces every value assigned to the template with the given ones. */
	public void setAllAssignments(HashMap<String, Object> keyValues) {
		this.assignments = keyValues;
	}

	/** Assigns a value to a place holder of the template. */
	public void assign(String key, Object value) {
		this.assignments.put(key, value);
	}

	/** The text of a block of the template, with the values assigned to it. */
	public String out(String blockName) {
		return this.out(this.template, blockName);
	}

	/** Parses a block of the template, keeping its place holders for the text of the block. */
	public void parse(String blockName) {
		List<Object> indexes = this.getBlockIndexes(this.template, blockName, null);

		List<Object> block = accessBlock(indexes, this.template);
		if (block == null) {
			throw new IllegalArgumentException("Block not found: " + blockName);
		}
		this.parseBlockElements(block);
	}

	/** The text of a block of a parsed template, with the place holders replaced by their values. */
	@SuppressWarnings("unchecked")
	private String out(ArrayList<HashMap<String, Object>> template, String blockName) {
		// the final result
		StringBuilder finalText = new StringBuilder();

		// How many blocks to ignore (non parsed template blocks)
		int ignore = 0;

		for (HashMap<String, Object> hashMap : template) {

			String itemName = (String) hashMap.keySet().toArray()[0];
			List<Object> list = (List<Object>) hashMap.get(itemName);

			for (Object object : list) {
				if (object.equals(TextTemplate.BEGIN_PARSED)) {
					ignore++;
					continue;

				}

				if (object.equals(TextTemplate.END_PARSED)) {
					ignore--;
					continue;
				}

				if (ignore > 0)
					continue;

				if (object instanceof String) {
					finalText.append(object).append(System.lineSeparator());
				} else {

					ArrayList<HashMap<String, Object>> mapList = new ArrayList<HashMap<String, Object>>();
					HashMap<String, Object> map = new HashMap<String, Object>();
					map.put("1", object);

					mapList.add(map);

					finalText.append(this.out(mapList, blockName));
				}
			}

		}

		return finalText.toString();
	}

	/** Splits the items of a block into text and place holders. */
	@SuppressWarnings("unchecked")
	private List<Object> parseBlockElements(List<Object> itens) {

		Pattern placeHoldersPattern = Pattern.compile("\\{(\\w+)\\}");

		List<Object> tempItens = new ArrayList<Object>();

		for (int index = 0; index < itens.size(); index++) {

			if (itens.get(index) instanceof String) {

				String line = (String) itens.get(index);

				if (line.equals(TextTemplate.BEGIN_PARSED)) {
					continue;
				}

				if (line.equals(TextTemplate.END_PARSED)) {
					break;
				}

				// Replaces every placeholder of the line by its assigned value
				Matcher matcher2 = placeHoldersPattern.matcher(line);
				StringBuilder replaced = new StringBuilder();
				while (matcher2.find()) {
					Object value = this.assignments.get(matcher2.group(1));
					matcher2.appendReplacement(replaced, Matcher.quoteReplacement(value == null ? "" : value.toString()));
				}
				matcher2.appendTail(replaced);
				tempItens.add(replaced.toString());
			} else {

				HashMap<String, Object> hashMap = (HashMap<String, Object>) itens.get(index);

				String itemName = (String) hashMap.keySet().toArray()[0];

				List<Object> list = ((List<Object>) hashMap.get(itemName));

				boolean parsed = list.get(0).equals(TextTemplate.BEGIN_PARSED);
				if (parsed) {

					// Removes the PARSED flags from parsed blocks
					while (!list.remove(0).equals(TextTemplate.END_PARSED))
						;

					tempItens.add(list);
				} else {
					tempItens.add("");
				}

			}
		}
		itens.add(0, TextTemplate.BEGIN_PARSED);
		itens.add(TextTemplate.END_PARSED);
		itens.addAll(tempItens);
		return itens;
	}

	/**
	 * Return the reference to an block of the template so we can modify whatever
	 * block we want
	 * 
	 * @param indexes
	 * @param template
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Object> accessBlock(List<Object> indexes, Object template) {

		// If theres not indexes, there is no point
		if (indexes.size() < 1) {
			return null;
		}

		// When the indexes size reach 1 means we found our element
		if (indexes.size() == 1) {
			return (ArrayList<Object>) ((HashMap<?, ?>) template).get(indexes.get(0));
		}

		if (template instanceof List) {
			// Access an element of the template and removes an indexer, and then go to next
			// indexer
			List<?> list = (List<?>) template;
			Object item = list.get((int) indexes.remove(0));
			return accessBlock(indexes, item);
		} else {
			// Access an element of the template and removes an indexer, and then go to next
			// indexer
			HashMap<?, ?> map = (HashMap<?, ?>) template;
			Object item = map.get(indexes.remove(0));
			return accessBlock(indexes, item);
		}
	}

	/**
	 * Retrives the indexers list of an block ( if any ) given your name
	 * 
	 * @param template
	 * @param blockNameToFind
	 * @param currentBlock
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Object> getBlockIndexes(ArrayList<?> template, String blockNameToFind, String currentBlock) {

		// Current indexers, the itens will be removed while method recurses
		List<Object> indexes = new ArrayList<Object>();

		// Checks each element of the template
		for (int index = 0; index < template.size(); index++) {

			// We are looking for blocks, so we must check only if element is a hashmap
			if (template.get(index) instanceof HashMap) {

				HashMap<String, Object> map = (HashMap<String, Object>) template.get(index);

				// Checks the block name matches
				if (map.get(blockNameToFind) != null) {
					if (currentBlock != null) {
						indexes.add(currentBlock);
					}
					indexes.add(index);
					indexes.add(blockNameToFind);
					return indexes;
				}

				// Acces the next level of template
				String blockName = (String) map.keySet().toArray()[0];
				List<Object> res = this.getBlockIndexes((ArrayList<?>) map.get(blockName), blockNameToFind, blockName);

				// If something has been found stores it!
				if (res.size() > 0) {
					if (currentBlock != null) {
						indexes.add(currentBlock);
					}
					indexes.add(index);
					indexes.addAll(res);
				}
			} else {
				// Non hashmap elements are ignored
				continue;
			}
		}
		return indexes;
	}
}

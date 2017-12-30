package utils.text;

/**
 * 
 */

import java.io.IOException;
import java.net.URI;
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

	public TextTemplate(URI templatePath) throws IOException {
		this.assignments = new HashMap<String, Object>();
		this.template = new TextTemplatePreprocessor().loadTemplate(templatePath);
	}

	public HashMap<String, Object> getAllAssignments() {
		return assignments;
	}

	public void setAllAssignments(HashMap<String, Object> keyValues) {
		this.assignments = keyValues;
	}

	public void assign(String key, Object value) {
		this.assignments.put(key, value);
	}

	public String out(String blockName) {
		return this.out(this.template, blockName);
	}

	public void parse(String blockName) {
		List<Object> indexes = this.getBlockIndexes(this.template, blockName, null);
		
		//TODO BUG here!!! 
		List<Object> block = (List<Object>) accessBlock(indexes, this.template);
		this.parseBlockElements(block);
	}

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
					finalText.append(object);
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

	@SuppressWarnings("unchecked")
	private List<Object> parseBlockElements(List<Object> itens) {

		Pattern placeHoldersPattern = Pattern.compile("\\{(.*)\\}", Pattern.CASE_INSENSITIVE);

		List<Object> tempItens = new ArrayList<Object>();

		for (int i = 0; i < itens.size(); i++) {

			if (itens.get(i) instanceof String) {

				String line = (String) itens.get(i);

				if (line.equals(TextTemplate.BEGIN_PARSED)) {
					continue;
				}

				if (line.equals(TextTemplate.END_PARSED)) {
					break;
				}

				Matcher m = placeHoldersPattern.matcher(line);

				if (m.find()) {

					String placeholder = m.group(1);
					String value = this.assignments.get(placeholder) == null ? ""
							: this.assignments.get(placeholder).toString();

					tempItens.add(m.replaceAll((String) value).trim());
				} else {
					tempItens.add(line);
				}
			} else {

				HashMap<String, Object> hashMap = (HashMap<String, Object>) itens.get(i);

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
		for (int i = 0; i < template.size(); i++) {

			// We are looking for blocks, so we must check only if element is a hashmap
			if (template.get(i) instanceof HashMap) {

				HashMap<String, Object> map = (HashMap<String, Object>) template.get(i);

				// Checks the block name matches
				if (map.get(blockNameToFind) != null) {
					if (currentBlock != null) {
						indexes.add(currentBlock);
					}
					indexes.add(i);
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
					indexes.add(i);
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

package utils.text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Preprocessor of text templates
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class TextTemplatePreprocessor {

	/**
	 * Used to parse the template to a data structure
	 */
	private int lineIndex;

	/**
	 * Loads the template and return a Hashmap ArrayList representation of it
	 * 
	 * @param inputStream
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<HashMap<String, Object>> loadTemplate(InputStream inputStream) {
		List<String> template = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.toList());

		HashMap<String, Object> parsed = this.process(template, "root", 0);
		return (ArrayList<HashMap<String, Object>>) parsed.get("root");
	}

	/**
	 * Parse the template to a data structure (easier to process)
	 * 
	 * @param lines
	 * @param blockName
	 * @param initialLineIndex
	 * @return
	 */
	private HashMap<String, Object> process(List<String> lines, String blockName, int initialLineIndex) {

		// The list of object at the template
		HashMap<String, Object> contents = new HashMap<String, Object>();

		// Pattern to find the blocks
		String pattern = "(<!--\\s?(begin|end):(.*)\\s?-->)(.*)";
		Pattern blockPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

		for (this.lineIndex = initialLineIndex; this.lineIndex < lines.size(); this.lineIndex++) {

			// Read line by line
			String line = lines.get(lineIndex);

			// Checks if the line has a block opener or ender
			Matcher matcher2 = blockPattern.matcher(line);
			if (matcher2.find()) {

				// Its is a block opener or ender?
				String startOrEnd = matcher2.group(2).trim();

				// The of the new find blovk
				String newBlockName = matcher2.group(3).trim();

				// The contents in the sameline of the block opener if any
				String lineContent = startOrEnd.equals("begin") ? matcher2.group(4) : "";

				// Adds the contents of the sameline of the block opener if any
				if (!lineContent.trim().equals("")) {
					this.putItem(contents, newBlockName, lineContent);
				}

				// When block ends return its contents
				if (startOrEnd.equals("end"))
					return contents;

				// Whem a new block has found parse this block
				HashMap<String, Object> subContents = this.process(lines, newBlockName, this.lineIndex + 1);

				this.putItem(contents, blockName, subContents);

			} else {

				// Stores the line at the current block
				if (blockName == null || line.equals(""))
					continue;

				this.putItem(contents, blockName, line);
			}
		}

		// Return all the sweet things (yumi...)
		return contents;
	}

	@SuppressWarnings("unchecked")
	private void putItem(HashMap<String, Object> contents, String blockName, Object item) {
		if (contents.get(blockName) == null) {
			contents.put(blockName, new ArrayList<Object>());
		}

		((List<Object>) contents.get(blockName)).add(item);
	}

}

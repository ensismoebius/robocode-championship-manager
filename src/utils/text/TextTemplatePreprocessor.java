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
	/** Finds the block openers and enders: <!-- begin:name --> and <!-- end:name -->. */
	private static final Pattern BLOCK_PATTERN = Pattern.compile("(<!--\\s?(begin|end):(.*)\\s?-->)(.*)", Pattern.CASE_INSENSITIVE);


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

		for (this.lineIndex = initialLineIndex; this.lineIndex < lines.size(); this.lineIndex++) {

			// Read line by line
			String line = lines.get(lineIndex);

			// Checks if the line has a block opener or ender
			Matcher matcher2 = BLOCK_PATTERN.matcher(line);
			if (matcher2.find()) {

				// Its is a block opener or ender?
				String startOrEnd = matcher2.group(2).trim();

				// The of the new find blovk
				String newBlockName = matcher2.group(3).trim();

				// Adds the contents of the sameline of the block opener if any
				this.putSameLine(contents, newBlockName, startOrEnd.equals("begin") ? matcher2.group(4) : "");

				// When block ends return its contents
				if (startOrEnd.equals("end"))
					return contents;

				// Whem a new block has found parse this block
				HashMap<String, Object> subContents = this.process(lines, newBlockName, this.lineIndex + 1);

				this.putItem(contents, blockName, subContents);

			} else {
				this.storeLine(contents, blockName, line);
			}
		}

		// Return all the sweet things (yumi...)
		return contents;
	}

	/** Adds the text written after a block opener, when there is some. */
	private void putSameLine(HashMap<String, Object> contents, String blockName, String lineContent) {
		if (!lineContent.trim().equals("")) {
			this.putItem(contents, blockName, lineContent);
		}
	}

	/** Stores a plain line at the current block: empty lines are skipped, and lines outside any block are ignored. */
	private void storeLine(HashMap<String, Object> contents, String blockName, String line) {
		if (blockName == null || line.equals("")) {
			return;
		}
		this.putItem(contents, blockName, line);
	}

	/** Adds an item to a block of the template, creating the block the first time. */
	@SuppressWarnings("unchecked")
	private void putItem(HashMap<String, Object> contents, String blockName, Object item) {
		if (contents.get(blockName) == null) {
			contents.put(blockName, new ArrayList<Object>());
		}

		((List<Object>) contents.get(blockName)).add(item);
	}

}

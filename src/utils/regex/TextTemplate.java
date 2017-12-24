/**
 * 
 */
package utils.regex;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
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

	//TODO Implement an data estructure to parse the template
	private String text;

	private HashMap<String, Object> assignments;

	private HashMap<String, List<String>> processedBlocks;

	private String parsingBlock;

	public TextTemplate(URI templatePath) throws IOException {
		this.assignments = new HashMap<String, Object>();
		this.processedBlocks = new HashMap<String, List<String>>();
		this.text = String.join("", Files.readAllLines(Paths.get(templatePath)));
	}

	public String getText() {
		return text;
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
		// Retrieve the block
		String block = this.getBlockContents(blockName);

		String nextSubBlockName = this.getNextSubBlockName(block);

		this.text = this.replaceBlock(this.text, blockName,
				String.join("", this.processedBlocks.get(new String(blockName.toCharArray()))));

		if (nextSubBlockName != "") {
			this.out(nextSubBlockName);
		}

		return this.text;
	}

	public boolean parse(String blockName) {

		// Retrieve the block
		this.parsingBlock = this.getBlockContents(blockName);

		// This the final result
		StringBuilder resultsStringBuilder = new StringBuilder();

		// Have to made this workaround otherwise sub block may be misparsed
		// Replaces subBlock to avoid placeholders misreplacements
		String nextSubBlockName = this.getNextSubBlockName(parsingBlock);
		String tempNextSubBlockName = String.valueOf(Math.random() * 10);
		String tempSubBlockValue = "<!-- begin:" + tempNextSubBlockName + " --><!-- end:" + tempNextSubBlockName
				+ " -->";
		String previousSubBlockValue = "";
		if (nextSubBlockName != "") {
			previousSubBlockValue = this.getBlockContents(nextSubBlockName);
			parsingBlock = this.replaceBlock(parsingBlock, nextSubBlockName, tempSubBlockValue);
		}

		// Iterates over key/values pairs in order to replace the placeholders
		this.assignments.forEach((key, value) -> {

			// If the value its a list reapeat the block until list ends
			if (value instanceof List<?>) {

				List<String> values = (List<String>) value;

				values.forEach(item -> {
					resultsStringBuilder.append(this.replacePlaceholders(parsingBlock, key, item));
				});
			} else {
				// If it is not a list just replace the placeholder
				resultsStringBuilder.append(this.replacePlaceholders(parsingBlock, key, (String) value));
			}
		});

		// No assignments just show the template!
		if (this.assignments.size() == 0) {
			resultsStringBuilder.append(parsingBlock);
		}

		String finalResult = resultsStringBuilder.toString();

		// errerererererererer
		if (nextSubBlockName != "") {

			previousSubBlockValue = "<!-- begin:" + nextSubBlockName + " -->" + previousSubBlockValue + "<!-- end:"
					+ nextSubBlockName + " -->";
			finalResult = this.replaceBlock(finalResult, tempNextSubBlockName, previousSubBlockValue);
		}

		// Creates an entry in processed blocks, if none
		if (this.processedBlocks.get(blockName) == null) {
			this.processedBlocks.put(blockName, new ArrayList<String>());
		}

		// Stores the processed block
		this.processedBlocks.get(blockName).add(finalResult);

		return true;
	}

	private String replaceBlock(String block, String blockName, String value) {
		String pattern = "(<!--\\s?begin:" + blockName + "\\s?-->)((.|\\n)*)(<!--\\s?end:" + blockName + "\\s?-->)";
		Pattern blockPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher m = blockPattern.matcher(block);

		if (m.find()) {
			return m.replaceAll(value).trim();
		}
		return block;
	}

	private String replacePlaceholders(String block, String placehoder, String value) {
		Pattern placeHoldersPattern = Pattern.compile("\\{" + placehoder + "\\}",
				Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher m = placeHoldersPattern.matcher(block);

		return m.find() ? m.replaceAll(value).trim() : "";

	}

	private String getNextSubBlockName(String blockName) {
		String pattern = "(<!--\\s?begin:(.*)\\s?-->)((.|\\n)*)(<!--\\s?end:.*\\s?-->)";
		Pattern blockPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher m = blockPattern.matcher(blockName);

		return m.find() ? m.group(2).trim() : "";
	}

	private String getBlockContents(String blockName) {
		String pattern = "(<!--\\s?begin:" + blockName + "\\s?-->)((.|\\n)*)(<!--\\s?end:" + blockName + "\\s?-->)";
		Pattern blockPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher m = blockPattern.matcher(this.text);

		return m.find() ? m.group(2).trim() : "";
	}
}

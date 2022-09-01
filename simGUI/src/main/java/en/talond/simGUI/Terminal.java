package en.talond.simGUI;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import en.talond.simGUI.io.Storage;



/**
 * The Terminal simultaneously prints and records each message that is passed to it.
 * Its purpose revolves entirely around debugging.
 * @author talon
 *
 */
public class Terminal {

	
	
	
	
	//message log
	private static final List<String> LOG = new LinkedList<>();
	
	
	
	
	
	/**
	 * Writes messages to the console, then logs them.
	 * @param msg to say
	 */
	public static final void say(String msg) {
		LOG.add(msg);
		System.out.println(msg);
	}
	
	
	
	
	
	/**
	 * Says a highly-indented message, so it is less noticeable in the log
	 * @param message to whisper
	 */
	public static final void mention(String minorMsg) {
		say("\t\t\t\t\t"+minorMsg);
	}
	
	
	
	
	
	/**
	 * Prints the full stack trace and logs the exception message
	 * @param exception to log
	 */
	public static final void specifyFullError(Exception e) {
		LOG.add(":: ERROR :: "+e.getLocalizedMessage());
		System.out.println();
		e.printStackTrace();
		System.out.println();
	}
	
	
	
	
	
	/**
	 * Writes the logged messages to a file
	 * @param filepath
	 * @throws IOException
	 */
	public static final void writeLog(String filepath) throws IOException {
		Storage.writeLines(LOG.toArray(new String[] {}), filepath);
	}
	
}
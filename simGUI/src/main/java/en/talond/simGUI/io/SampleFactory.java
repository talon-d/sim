package en.talond.simGUI.io;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.data.Batch;
import en.talond.simGUI.data.Request;






/**
 * This class allows a user to create, manage, and view sample request data.
 * @author talon
 *
 */
public class SampleFactory {

	
	
	
	
	

	/**
	 * formats the internal rich date object to a simple string
	 * @param date
	 * @return date as string
	 */
	public static final String assembleDate(final LocalDate date) {
		final String year = Integer.toString(date.getYear());
		String month = Integer.toString(date.getMonthValue());
		if(month.length() == 1)
			month = "0"+month;
		String day = Integer.toString(date.getDayOfMonth());
		if(day.length() == 1)
			day = "0"+day;
		return year+month+day;
	}
	
	
	
	
	
	
    /**
     * Factory method for converting deprecated Java Dates
     * to the LocalDate object used by all sim objects.
     * The UiBooster library uses the deprecated Date object for its
     * express date picker window. Rather than modifying the window,
     * this factory method is much quicker.
     * @param raw date from gui
     * @return local date
     */
    public static final LocalDate convertUiDate(Date fromUi) {
    	return fromUi.toInstant()
  		      .atZone(ZoneId.systemDefault())
  		      .toLocalDate();
    }
	
	
	
    
    
	

	/**
	 * Parses the 8-character data format used in sample names and filenames
	 * Date Format: yyyyMMdd
	 * @param date string
	 * @return local date representation
	 */
	public static final LocalDate parseDate(String date) {
		try {
			final int
			year = Integer.parseInt(date.substring(0,4)),
			month = Integer.parseInt(date.substring(4,6)),
			day = Integer.parseInt(date.substring(6,8));
			return LocalDate.of(year, month, day);
		} catch (Exception e) {
			Terminal.say("Couldn't parse string date: "+date);
			return null;
		}
	}
	
	
	
	/**
	 * Parses a batch string into a fully featured Batch instance.
	 * Batch name must not have # marker.
	 * @param batch string
	 * @return batch instance
	 */
	public static final Batch parseBatch(String batch) {
		Terminal.say("\t\t\tAttempting to parse batch: "+batch);
		Batch id; try {
			id = new Batch (Double.parseDouble(batch));
		} catch (NumberFormatException e) {
			String[] ids = batch.split("(:\\()|-");
			double num = Double.parseDouble(ids[0]);
			int start = Integer.parseInt(ids[1]);
			int end = Integer.parseInt(ids[2].replaceFirst("\\)", ""));
			id = new Batch(num,start,end);
		} catch (Exception e) {
			id = null;
		}
		return id;
	}
	
	
	
	/**
	 * Infers a basic canna gc request from a given name
	 * @param name
	 * @return basic request
	 */
	public static final Request parseBasicRequest(String name) {
		name = name.toUpperCase();
		Terminal.say("Attempting to autoparse request: "+name);
		try {
			String[] toExtractBatch = name.split("#");
			String main = toExtractBatch[0];
			String batch = toExtractBatch[1];
			String[] tokens = main.split("_");
			String prefix = tokens[0];
			LocalDate d = parseDate( tokens[1] );
			String suffixes = tokens[2];
			String[] targets = new String[] {};
			boolean[] flags = new boolean[] {true,false,true};
			Batch id = parseBatch(batch);
			Request r = new Request(prefix,d,suffixes,id,flags[0],flags[1],flags[2],targets);
			Terminal.say("Successfully autoparsed request: "+name+" \tinto: "+r.getName());
			return r;
		} catch (Exception e) {
			Terminal.say("Autoparse operation unsuccessful!\t"+e.getMessage());
			return null;
		}
	}
	
	
	
}

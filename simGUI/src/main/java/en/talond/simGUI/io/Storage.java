package en.talond.simGUI.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import en.talond.simGUI.Terminal;
import en.talond.simGUI.WorkingSet;




/**
 * This class provide IO functionality for the local filesystem
 * @author talon
 *
 */
public class Storage {

	
	
    //filesystem location of the working set
    public static final String 
    WORKING_SET_NAME = "sim-set.dat",
    LOG_NAME = "console-log_"+LocalDate.now().toString()+".txt",
    ROOT = new File("").getAbsolutePath();
	
	
	
	/**
	 * Loads program data from the filesystem
	 */
	public static final void init() {
		Terminal.say("Loading stored info...");
		try {
			WorkingSet.open(ROOT+"/"+WORKING_SET_NAME);
			Terminal.say("Successfully loaded data.");
		} catch (Exception e) {
			Terminal.say("Couldn't load data.");
			Terminal.specifyFullError(e);
		}
	}

	
	
	/**
	 * Stores program data in the filesystem
	 */
	public static final void close() {
		Terminal.say("Storing info...");
		try {
			WorkingSet.close();
			Terminal.writeLog(ROOT+"/"+LOG_NAME);
			Terminal.say("Successfully stored data.");
		} catch (Exception e) {
			Terminal.say("Couldn't store data.");
			Terminal.specifyFullError(e);
		}
	}

	
	
	
	
	
	
	
	
	
	
	/**
	 * Loads an excel file from storage
	 * @param file to load
	 * @return loaded excel file
	 * @throws IOException
	 * @throws EncryptedDocumentException
	 */
	public static final Workbook loadExcelFile(final File toLoad) throws EncryptedDocumentException, IOException {
		return WorkbookFactory.create(toLoad);
	}

	
	
	/**
	 * Loads the lines of a file encoded with UTF-16 strings
	 * @param file to load
	 * @return array of lines
	 * @throws IOException
	 * @see CalTable
	 */
	public static final String[] loadUTF16File(final File toLoad) throws IOException {
		return Files.readLines(toLoad, Charset.forName("UTF-16")).toArray(new String[0]);
	}
			
	
	
	/**
	 * Loads the lines of a plain text file (UTF8)
	 * @param pathToFile
	 * @return
	 * @throws FileNotFoundException
	 */
	public static final String[] readLines(final String pathToFile) throws FileNotFoundException {
		final Scanner reader = new Scanner(new File(pathToFile));
		final List<String> lines = new LinkedList<>();
		while(reader.hasNextLine())
			lines.add(reader.nextLine());
		return lines.toArray(new String[0]);
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Overwrites the lines of a file
	 * @param linesToWrite
	 * @param toWriteTo
	 * @throws IOException
	 */
	public static final void writeLines(final String[] linesToWrite, final String writePath) throws IOException
	{
		final FileWriter fw = new FileWriter(new File(writePath));
		for(final String line : linesToWrite)
			fw.append(line+"\n");
		fw.close();
	}
	
	
	
	/**
	 * Writes the assembled excel file
	 * @throws IOException
	 */
	public static final void writeReport(Workbook toWrite, File location) throws IOException {
		final FileOutputStream fos = new FileOutputStream(location);
		toWrite.write(fos);
		fos.close();
	}
	
	
	
	
	
	
	
	
	/* Local Gson implementation, with a LocalDate type adapter */
	private static final Gson SERIALIZER = new GsonBuilder().registerTypeAdapter(LocalDate.class, new DateAdapter()).create();
	
	
	
	/**
	 * Deserializes a JSON string into its appropriate object
	 * @param <T> of any type, though some object request custom type adapters
	 * @param object to deserialize
	 * @param example of the class to instantiate
	 * @return instance of <T> from JSON string
	 * @throws Exception with many failure cases
	 */	@SuppressWarnings("unchecked")	// can cause an exception if an invalid example parameter is given
	public static final <T> T deserialize(String toDeserialize, T example) throws Exception {
		return (T) SERIALIZER.fromJson(toDeserialize, example.getClass());
	}
	
	 
	 
	/**
	 * Serializer generic objects into a simple JSON string.
	 * Implementation of the google Gson library
	 * @param <T> of any type, though it may need custom deserialization
	 * @param genertic object to serialize
	 * @return serialized object JSON string
	 */
	public static final <T> String serialize(T toSerialize) {
		return SERIALIZER.toJson(toSerialize);
	}
	


	/**
	 * Allows the gson object to read/write the local date object properly
	 * Used only for simple LocalDates (year, month, day)
	 * @author talon
	 *
	 */
	private static final class DateAdapter extends TypeAdapter<LocalDate> {
		/**
		 * serializes local dates
		 */
		@Override public void write(JsonWriter out, LocalDate value) throws IOException {
			out.beginObject();
			out.name("year");
			out.value(value.getYear());
			out.name("month");
			out.value(value.getMonthValue());
			out.name("day");
			out.value(value.getDayOfMonth());
			out.endObject();
		}
		/**
		 * deserializes local dates
		 */
		@Override public LocalDate read(JsonReader in) throws IOException {
			in.beginObject();
			in.nextName();
			int year = in.nextInt();
			in.nextName();
			int month = in.nextInt();
			in.nextName();
			int day = in.nextInt();
			in.endObject();
			return LocalDate.of(year, month, day);
		}
	}

}

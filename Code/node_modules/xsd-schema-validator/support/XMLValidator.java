package support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class XMLValidator implements ErrorHandler {

  static final String END = "---end---";

  private boolean withErrors = false;

  public XMLValidator() {

  }

  private static void println(PrintStream out, String msg) {
    out.println(msg);
    out.flush();
  }

  private static void println(String msg) {
    println(System.out, msg);
  }

  @Override
  public void warning(SAXParseException exception) throws SAXException {
    println("[warning] " + exception.getMessage() + ". Line (" + exception.getLineNumber() + "), Column (" + exception.getColumnNumber() + ").");

    withErrors = true;
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    println("[error] " + exception.getMessage() + ". Line (" + exception.getLineNumber() + "), Column (" + exception.getColumnNumber() + ").");

    withErrors = true;
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    println("[fatal] " + exception.getMessage() + ". Line (" + exception.getLineNumber() + "), Column (" + exception.getColumnNumber() + ").");

    withErrors = true;
  }

  private static boolean isInputEnd(String str) {
    return str.replaceAll("[\\n\\r]+", "").equals(END);
  }

  private static Schema loadSchema(String fileName) throws Exception {
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = sf.newSchema(new File(fileName));

    return schema;
  }

  private static InputStream readFromSysIn() throws IOException {

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    
    StringWriter writer = new StringWriter();
    
    while (true) {
      String line = reader.readLine();

      if (isInputEnd(line)) {
        break;
      }

      writer.append(line + System.getProperty("line.separator"));
//      println("line: " + line);
      //writer.append(System.getProperty("\n"));
    }
    
//    println("Printing writer: " + writer.toString());
    reader.close();
    
    byte[] buffer = writer.toString().getBytes();
    writer.close();

    return new ByteArrayInputStream(buffer);
  }

  public static void main(String[] args) throws Exception {

    boolean readStdin = false;
    String fileName = null;
    String schemaFile = null;

    for (String str: args) {
      if ("-stdin".equals(str)) {
        readStdin = true;
        System.out.println("WAS at stdin");
      } else
      if (str.startsWith("-file=")) {
        fileName = str.replaceFirst("-file=", "");
        
        System.out.println("WAS at file");
      } else
      if (str.startsWith("-schema=")) {
        schemaFile = str.replaceFirst("-schema=", "");
        System.out.println("WAS at schema");
      }
    }

    if (schemaFile == null) {
      println(System.err, "[error] specify schema via -schema=[SCHEMA]");
      System.exit(1);
    }

    InputStream inputStream;

    if (readStdin) {
      inputStream = readFromSysIn();
    } else {
      inputStream = new FileInputStream(fileName);
    }

    XMLValidator handler = new XMLValidator();

    try {
    	//Y
      Schema schema = loadSchema(schemaFile);
      Validator validator = schema.newValidator();
      
      validator.setErrorHandler(handler);
      validator.validate(new StreamSource(inputStream));
      
      println("WAS at schema " + schemaFile);
      
      println("result=" + (handler.withErrors ? "WITH_ERRORS" : "OK"));

    } catch (Exception e) {
      println("[fatal] " + e.getMessage());
      println("result=FATAL_ERROR");

      handler.withErrors = true;
    }
    
    inputStream.close();

    System.exit(handler.withErrors ? 1 : 0);
  }
}
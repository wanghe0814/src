import java.io.*;
import java_cup.runtime.*;

public class P4 {
    public static void main(String[] args)
        throws IOException  
    {
        
        if (args.length != 2) {
            System.err.println("please supply name of file to be parsed " +
			                   "and name of file for unparsed version.");
            System.exit(-1);
        }
  
        FileReader inFile = null;
        try {
            inFile = new FileReader(args[0]);
        } catch (FileNotFoundException ex) {
            System.err.println("File " + args[0] + " not found.");
            System.exit(-1);
        }
 
        PrintWriter outFile = null;
        try {
            outFile = new PrintWriter(args[1]);
        } catch (FileNotFoundException ex) {
            System.err.println("File " + args[1] +
                               " could not be opened for writing.");
            System.exit(-1);
        }

        parser P = new parser(new Yylex(inFile));

        Symbol root = null;  

        try {
            root = P.parse();  
            System.out.println ("program parsed correctly.");
        } catch (Exception ex){
            System.err.println("Exception occured during parse: " + ex);
            System.exit(-1);
        }

		 
		SymTable symT = new SymTable();
      	((ASTnode)root.value).nameAnalyze(symT);
		if(ErrMsg.isError == true){
		System.err.println("Errors occured during name analyze" );
			 
		} else {
			System.out.println("name analyze succeeded");
		}
		
        ((ASTnode)root.value).unparse(outFile, 0);
        outFile.close();


        return;
    }
}
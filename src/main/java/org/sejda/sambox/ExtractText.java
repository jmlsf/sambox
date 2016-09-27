package org.sejda.sambox;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.io.SeekableSources;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.text.PDFTextStripper;

public class ExtractText {
    public static void main(String[] args) {
        try { 
            if (args.length != 1) {
                System.out.println("ERROR: must provide exactly one filename, but instead provided " + args.length);
                System.exit(1);
            }
            PDDocument pddocument = PDFParser.parse(SeekableSources.seekableSourceFrom(new File(args[0])));
            File outputFile = new File(args[0] + ".json");

            PDFTextStripper textStripper = new PDFTextStripper();
            
            BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
            textStripper.writeText(pddocument, outputWriter);

            outputWriter.close();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }       
    }
}

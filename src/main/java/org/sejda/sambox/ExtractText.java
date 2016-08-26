package org.sejda.sambox;

// import static org.sejda.core.support.io.IOUtils.createTemporaryBuffer;
// import static org.sejda.core.support.io.model.FileOutput.file;
// import static org.sejda.core.support.prefix.NameGenerator.nameGenerator;
// import static org.sejda.core.support.prefix.model.NameGenerationRequest.nameRequest;

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

// import org.sejda.core.support.io.MultipleOutputWriter;
// import org.sejda.core.support.io.OutputWriters;
// import org.sejda.impl.sambox.component.DefaultPdfSourceOpener;
// import org.sejda.impl.sambox.component.PDDocumentHandler;
// import org.sejda.impl.sambox.component.PdfTextExtractor;



public class ExtractText {
    public static void main(String[] args) {
        try { 
            //InputStream inputStream = new FileInputStream("test.pdf");

            PDDocument pddocument = PDFParser.parse(SeekableSources.seekableSourceFrom(new File("test.pdf")));
            File outputFile = new File("test.txt");

            PDFTextStripper textStripper = new PDFTextStripper();
            // textStripper.setStartPage(1);
            // textStripper.setEndPage(endPageIncluding);
            
            BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
            textStripper.writeText(pddocument, outputWriter);

//            pddocument.close();
            outputWriter.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        
    }
}

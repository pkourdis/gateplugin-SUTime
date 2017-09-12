package gate.creole;

import gate.*;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.util.CoreMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;
import java.net.URISyntaxException;
import java.text.NumberFormat;

/**
 * A processing resource for GATE Developer implementing the SUTime library for temporal tagging.
 *
 * @author Panagiotis Kourdis <kourdis@gmail.com>
 */
@CreoleResource(
        name = "SUTime Stanford Temporal Tagger",
        icon = "SUTime.png",
        comment = "Annotate documents with TIMEX3 tags using the SUTime library."
)
public class SUTime extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

    // name of the annotation set to be used as input
    private String inputASName;

    @RunTime
    @Optional
    @CreoleParameter(comment = "The annotation set to be used for input.", defaultValue = "")
    public void setInputASName(String name) {
        inputASName = name;
    }

    public String getInputASName() {
        return inputASName;
    }

    // name of the annotation set to be used for output (i.e. write the TIME3X tags)
    private String outputASName;

    @RunTime
    @Optional
    @CreoleParameter(comment = "The annotation set to be used for output.", defaultValue = "")
    public void setOutputASName(String name) {
        outputASName = name;
    }

    public String getOutputASName() {
        return outputASName;
    }

    // reference date to be used for normalization
    private String referenceDate;

    @RunTime
    @Optional
    @CreoleParameter(comment = "Reference date of the document. Permissible date values are 'YYYY-MM-DD', 'today' (today's date), 'fileCreationDate' (date file was created), " +
            "'fileLastAccessDate' (date file was last accessed) and 'fileLastModifiedDate' (date file was last modified).", defaultValue = "today")
    public void setReferenceDate(String date) {
        referenceDate = date;
    }

    public String getReferenceDate() {
        return referenceDate;
    }

    private static final ZoneId defaultZoneId = ZoneId.systemDefault();
    private String fileDate = null;

    @Override
    public void execute() throws ExecutionException {

        long execStartTime = System.currentTimeMillis(); // start time
        fireStatusChanged("Performing temporal tagging annotations with SUTime in " + document.getName());
        fireProgressChanged(0);

        if (document == null) throw new ExecutionException("No document to process!");
        String docContent = document.getContent().toString(); // document's content
        int docContentLength = docContent.length(); // length of document's content

        Properties props = new Properties();
        AnnotationPipeline pipeline = new AnnotationPipeline();
        pipeline.addAnnotator(new TokenizerAnnotator(false));
        pipeline.addAnnotator(new TimeAnnotator("sutime", props));

        String refDate = referenceDate;
        switch (refDate) {
            case "": // no reference date provided by the user
                throw new ExecutionException("No reference date provided. Please give a valid option.");
            case "today": // user asked for today's date as reference date
                refDate = LocalDate.now().toString();
                break;
            case "fileCreationDate": // user asked for file's creation date as reference date
            case "fileLastAccessDate": // user asked for file's last access date as reference date
            case "fileLastModifiedDate": // user asked for file's lat modification date as reference date
                setFileDate(refDate);
                if (fileDate != null) { refDate = fileDate; }
                else { // user asked for file's date as reference date but it is null
                    throw new ExecutionException(refDate + " is null for " + document.getName() +
                            ". Skipping temporal tagging for this document.");
                }
                break;
            default:
                // TODO check if it is a valid "YYYY-MM-DD'" date
                break;
        }

        Annotation annotation = new Annotation(docContent);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, refDate);
        pipeline.annotate(annotation);
        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);

        // no temporal expressions detected by SUTime
        if (timexAnnsAll.isEmpty()) {
            fireProcessFinished();
            fireStatusChanged("No temporal expressions detected in " + document.getName() + " in "
                    + NumberFormat.getInstance().format((double)(System.currentTimeMillis() - execStartTime) / 1000)
                    + " seconds!");
            return;
        }

        AnnotationSet outputAS = document.getAnnotations(outputASName);
        for (CoreMap cm : timexAnnsAll) {
            List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
            long startOffset = tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            long endOffset =  tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
            fireProgressChanged((int) ((float) endOffset / docContentLength * 100));
            FeatureMap timex3Features = Factory.newFeatureMap();
            timex3Features.put("Type", cm.get(TimeExpression.Annotation.class).getTemporal().getTimexType().name());
            timex3Features.put("Value", cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue());
            try {
                outputAS.add(startOffset, endOffset, "TIMEX3", timex3Features);
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        }

        fireProcessFinished();
        fireStatusChanged("Temporal expressions detected and normalized in " + document.getName() + " in "
                + NumberFormat.getInstance().format((double)(System.currentTimeMillis() - execStartTime) / 1000)
                + " seconds!");
    }

    /**
     * Sets as file date its creation, last access or last modification date.
     *
     * @param fDate A string with value "fileCreationDate", or "fileLastAccessDate" or "fileLastModifiedDate".
     */
    private void setFileDate(String fDate) {

        try {
            Path file = Paths.get(document.getSourceUrl().toURI());
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            if (attr != null && fDate.equals("fileCreationDate")) {
                LocalDateTime localDateTime = LocalDateTime.ofInstant(attr.creationTime().toInstant(), defaultZoneId);
                fileDate = localDateTime.toLocalDate().toString();
            } else if (attr != null && fDate.equals("fileLastAccessDate")) {
                LocalDateTime localDateTime = LocalDateTime.ofInstant(attr.lastAccessTime().toInstant(), defaultZoneId);
                fileDate = localDateTime.toLocalDate().toString();
            }
            else if (attr != null && fDate.equals("fileLastModifiedDate")) {
                LocalDateTime localDateTime = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), defaultZoneId);
                fileDate = localDateTime.toLocalDate().toString();
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}
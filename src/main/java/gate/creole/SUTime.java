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

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
 * A GATE processing resource for temporal tagging using the SUTIme library part of the Stanford CoreNLP suite.
 *
 * @author Panagiotis Kourdis <kourdis@gmail.com>
 */
@CreoleResource(
        name = "SUTime Stanford Temporal Tagger",
        icon = "SUTime.png",
        comment = "Annotate documents with TIMEX3 tags using the SUTime library."
)
public class SUTime extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

    private static final ZoneId defaultZoneId = ZoneId.systemDefault(); //system's time zone
    private static final String dateFormat = "yyyy-MM-dd"; //date format required by SUTime
    private String inputASName; //name of the annotation set to be used as input
    private String outputASName; //name of the annotation set to be used for output (i.e. write the TIME3X tags)
    private String referenceDate; //reference date to be used for normalization
    private String fileDate = null; //file date to be retrieved from the operating system
    private AnnotationPipeline annotationPipeline; //Stanford CoreNLP annotation pipeline

    @RunTime
    @Optional
    @CreoleParameter(comment = "The annotation set to be used for input.", defaultValue = "SUTime")
    public void setInputASName(String name) {
        inputASName = name;
    }

    /**
     * Provides the name of the input annotation set.
     *
     * @return The name of the annotation set used for input.
     */
    public String getInputASName() {
        return inputASName;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "The annotation set to be used for output.", defaultValue = "")
    public void setOutputASName(String name) {
        outputASName = name;
    }

    /**
     * Provides the name of the output annotation set.
     *
     * @return The name of the annotation set used for output.
     */
    public String getOutputASName() {
        return outputASName;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Reference date of the document. Permissible date values are 'yyyy-MM-dd', " +
            "'today' (today's date), 'creationDate' (date file was created), " +
            "'lastAccessDate' (date file was last accessed) and 'lastModifiedDate' (date file was last modified).",
            defaultValue = "today")
    public void setReferenceDate(String date) {
        referenceDate = date;
    }

    /**
     * Provides the reference date of the current document.
     *
     * @return The reference date used normalizing temporal expressions by SUTime.
     */
    public String getReferenceDate() {
        return referenceDate;
    }

    @Override
    public Resource init() throws ResourceInstantiationException {

        Properties props = new Properties();
        annotationPipeline = new AnnotationPipeline();
        annotationPipeline.addAnnotator(new TokenizerAnnotator(false));
        annotationPipeline.addAnnotator(new TimeAnnotator("sutime", props));
        return this;
    }

    @Override
    public void reInit() throws ResourceInstantiationException {

        annotationPipeline = null;
        init();
    }

    @Override
    public void execute() throws ExecutionException {

        long execStartTime = System.currentTimeMillis(); //execution start time

        //update GATE
        fireStatusChanged("Performing temporal tagging annotations with SUTime in " + document.getName());
        fireProgressChanged(0);

        //document information
        if (document == null) throw new ExecutionException("No document to process!");
        String docContent = document.getContent().toString(); //document's content
        int docContentLength = docContent.length(); //length of document's content

        //set reference date
        String refDate = referenceDate;
        switch (refDate) {
            case "": //no reference date provided by the user
                throw new ExecutionException("No reference date provided. Please give a valid option.");
            case "today": //user asked for today's date as reference date
                refDate = LocalDate.now().toString();
                break;
            case "creationDate": //user asked for file's creation date as reference date
            case "lastAccessDate": //user asked for file's last access date as reference date
            case "lastModifiedDate": //user asked for file's lat modification date as reference date
                setFileDate(refDate);
                if (fileDate != null) { refDate = fileDate; }
                else { //user asked for file's date as reference date but it is null
                    throw new ExecutionException(refDate + " is null for " + document.getName());
                }
                break;
            default:
                if (!isDateValid(refDate)) //check if it is a valid date and in the right format
                    throw new ExecutionException(refDate + " is not a valid date or formatted as 'yyyy-MM-dd'.");
                break;
        }

        //Stanford CoreNLP - SUTime
        Annotation annotation = new Annotation(docContent);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, refDate);
        annotationPipeline.annotate(annotation);
        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);

        //no temporal expressions detected
        if (timexAnnsAll.isEmpty()) {
            fireProcessFinished();
            fireStatusChanged("No temporal expressions detected for " + document.getName() + " in "
                    + NumberFormat.getInstance().format((double)(System.currentTimeMillis() - execStartTime) / 1000)
                    + " seconds!");
            return;
        }

        //write TIMEX3 annotations
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

        //update GATE
        fireProcessFinished();
        fireStatusChanged("Temporal expressions detected and normalized for " + document.getName() + " in "
                + NumberFormat.getInstance().format((double)(System.currentTimeMillis() - execStartTime) / 1000)
                + " seconds!");
    }

    /**
     * Sets as file date its creation, or last access or last modification date.
     *
     * @param fDate A string with value "creationDate", or "lastAccessDate" or "lastModifiedDate".
     */
    private void setFileDate(String fDate) {

        try {
            Path file = Paths.get(document.getSourceUrl().toURI());
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            if (attr != null && fDate.equals("creationDate")) {
                LocalDateTime creationTime = LocalDateTime.ofInstant(attr.creationTime().toInstant(), defaultZoneId);
                fileDate = creationTime.toLocalDate().toString();
            } else if (attr != null && fDate.equals("lastAccessDate")) {
                LocalDateTime lastAccessTime = LocalDateTime.ofInstant(attr.lastAccessTime().toInstant(), defaultZoneId);
                fileDate = lastAccessTime.toLocalDate().toString();
            }
            else if (attr != null && fDate.equals("lastModifiedDate")) {
                LocalDateTime lastModifiedTime = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), defaultZoneId);
                fileDate = lastModifiedTime.toLocalDate().toString();
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if date is a valid with the right format.
     *
     * @param dateToValidate The date as string to be validated.
     * @return True if date is valid and in right format else false.
     */
    private boolean isDateValid(String dateToValidate){

        if (dateToValidate == null) return false;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setLenient(false); //makes the parse method below throw exception

        try { //if it is not a valid date with the right format it will throw a ParseException
            simpleDateFormat.parse(dateToValidate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
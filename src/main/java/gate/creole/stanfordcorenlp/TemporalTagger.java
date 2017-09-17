package gate.creole.stanfordcorenlp;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
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
import java.time.format.DateTimeFormatter;
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
        name = "Stanford Temporal Tagger SUTime",
        icon = "SUTime.png",
        comment = "Annotate documents with TIMEX3 tags using the SUTime library developed by the Stanford CoreNLP group.",
        helpURL = "https://pkourdis.github.io/gateplugin-TemporalTagger/"
)
public class TemporalTagger extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

    private static final ZoneId defaultZoneId = ZoneId.systemDefault(); //system's time zone
    private static final String dateFormat = "yyyy-MM-dd"; //date format required by SUTime
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
    private String inputAnnotationSetName; //name of the annotation set to be used for input
    private String inputAnnotationName; //name of the annotation to be used for input
    private String inputFeatureName; //name of the feature to be used for input
    private String outputAnnotationSetName; //name of the annotation set to write the results
    private String outputAnnotationName; //name of the annotation set to write the results
    private String referenceDate; //reference date to be used for normalization
    private String fileDate; //file date to be retrieved from the operating system
    private AnnotationPipeline annotationPipeline; //Stanford CoreNLP annotation pipeline

    @RunTime
    @Optional
    @CreoleParameter(comment = "Name of the annotation set that has the annotation with the date to be used as reference (if `referenceDate`=annotation)", defaultValue = "")
    public void setInputAnnotationSetName(String name) {
        inputAnnotationSetName = name;
    }

    /**
     * Provides the name of the input annotation set to look for the reference date.
     *
     * @return The name of the annotation set used for input.
     */
    public String getInputAnnotationSetName() {
        return inputAnnotationSetName;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Name of the annotation that has the date to be used as reference (if `referenceDate`=annotation).", defaultValue = "")
    public void setInputAnnotationName(String name) { inputAnnotationName = name;}

    /**
     * Provides the name of the input annotation to look for the reference dats.
     *
     * @return The name of the annotation used for input.
     */
    public String getInputAnnotationName() {return inputAnnotationName; }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Name of the feature with value the date to be used as reference (if `referenceDate`=annotation). Date should be in \"yyy-MM-dd\" format.", defaultValue = "")
    public void setInputFeatureName(String name) { inputFeatureName = name;}

    /**
     * Provides the name of the feature holding the date to be used as reference date. Date should be in "yyy-MM-dd" format.
     *
     * @return The name of the feature with the document date.
     */
    public String getInputFeatureName() {return inputFeatureName; }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Name of the annotation set to write the results.", defaultValue = "SUTime")
    public void setOutputAnnotationSetName(String name) {
        outputAnnotationSetName = name;
    }

    /**
     * Provides the name of the output annotation set.
     *
     * @return The name of the annotation set used for output.
     */
    public String getOutputAnnotationSetName() {
        return outputAnnotationSetName;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Name of the annotation to write the results.", defaultValue = "TIMEX3")
    public void setOutputAnnotationName(String name) { outputAnnotationName = name;}

    /**
     * Provides the name of the output annotation to write the results.
     *
     * @return The name of the annotation used for output.
     */
    public String getOutputAnnotationName() {return outputAnnotationName; }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Reference date of the document. Permissible date values are 'yyyy-MM-dd', " +
            "annotation (document annotation holding the date)" +
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
        String refDate = referenceDate; //set reference date (might change later if found in annotation)

        //update GATE
        fireStatusChanged("Performing temporal expressions tagging with SUTime in " + document.getName());
        fireProgressChanged(0);

        //document information
        if (document == null) throw new ExecutionException("No document to process!");
        String docContent = document.getContent().toString(); //document's content
        int docContentLength = docContent.length(); //length of document's content

        // reference date
        switch (refDate) {
            case "": //no explicit reference date provided by the user
                throw new ExecutionException("No reference date provided. Please give a valid option.");
            case "today": //user asked for today's date as reference date
                refDate = LocalDate.now().format(dateTimeFormatter);
                break;
            case "creationDate": //user asked for file's creation date as reference date
            case "lastAccessDate": //user asked for file's last access date as reference date
            case "lastModifiedDate": //user asked for file's lat modification date as reference date
                setFileDate(refDate);
                if (fileDate != null) {
                    refDate = fileDate;
                } else { //user asked for file's date as reference date but it is null
                    throw new ExecutionException(refDate + " is null for " + document.getName());
                }
                break;
            case "annotation":
                boolean foundValidDocumentDate = false;
                AnnotationSet inputAS = document.getAnnotations(inputAnnotationSetName);
                AnnotationSet inputAN = inputAS.get(inputAnnotationName);
                for (gate.Annotation annotation : inputAN) {
                    FeatureMap features = annotation.getFeatures();
                    refDate = (String) features.get(inputFeatureName);
                    if (isDateValid(refDate)) { //valid date found in the specified annotation
                        foundValidDocumentDate = true;
                        break;
                    }
                }
                if (!foundValidDocumentDate) //if it is not found throw execution exception
                    throw new ExecutionException("No valid date found in the specified annotation for ." + document.getName());
            default:
                if (!isDateValid(refDate)) //check if it is not a valid date and in the right format
                    throw new ExecutionException(refDate + " is not a valid date and/or formatted as 'yyyy-MM-dd'.");
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
        AnnotationSet outputAS = document.getAnnotations(outputAnnotationSetName);
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
                fileDate = creationTime.toLocalDate().format(dateTimeFormatter);
            } else if (attr != null && fDate.equals("lastAccessDate")) {
                LocalDateTime lastAccessTime = LocalDateTime.ofInstant(attr.lastAccessTime().toInstant(), defaultZoneId);
                fileDate = lastAccessTime.toLocalDate().format(dateTimeFormatter);
            }
            else if (attr != null && fDate.equals("lastModifiedDate")) {
                LocalDateTime lastModifiedTime = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), defaultZoneId);
                fileDate = lastModifiedTime.toLocalDate().format(dateTimeFormatter);
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if date is a valid with the right format.
     *
     * @param dateToValidate The date as string to be validated.
     * @return True if date is not valid and in right format else false.
     */
    private boolean isDateValid(String dateToValidate){

        if (dateToValidate == null) return false;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setLenient(false); //makes the parse method below throw exception

        try { //if it is not a valid date with the right format it will throw a ParseException
            simpleDateFormat.parse(dateToValidate);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
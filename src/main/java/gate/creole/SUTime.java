package gate.creole;

import edu.stanford.nlp.pipeline.Annotation;
import gate.*;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.util.CoreMap;
import gate.util.InvalidOffsetException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.List;
import java.util.Properties;

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
    @CreoleParameter(comment = "Reference date of the document. Permissible date values are 'YYYY-MM-DD', 'today' (today's date), 'creationDate' (date file was created), " +
            "'lastAccessDate' (date file was last accessed) and 'lastModifiedDate' (date file was last modified).", defaultValue = "today")
    public void setReferenceDate(String date) {
        referenceDate = date;
    }

    public String getReferenceDate() {
        return referenceDate;
    }

    private static final ZoneId defaultZoneId = ZoneId.systemDefault();
    private LocalDate creationDate = null;
    private LocalDate lastAccessDate = null;
    private LocalDate lastModifiedDate = null;

    @Override
    public void execute() throws ExecutionException {

        long execStartTime = System.currentTimeMillis(); // start time
        fireStatusChanged("Performing temporal tagging annotations with SUTime in " + document.getName());
        fireProgressChanged(0);

        if (document == null) throw new ExecutionException("No document to process!");
        String docContent = document.getContent().toString();
        int docContentLength = docContent.length();

        Properties props = new Properties();
        AnnotationPipeline pipeline = new AnnotationPipeline();
        pipeline.addAnnotator(new TokenizerAnnotator(false));
        pipeline.addAnnotator(new TimeAnnotator("sutime", props));

        setDocumentFileDates(); // set dates for the document based on the file
        String refDate = referenceDate;
        if (refDate.equals("")) {
        throw new ExecutionException("Empty reference date. Please provide a valid option.");
        } else if (refDate.equals("today")) {
            refDate = LocalDate.now().toString();
        } else if (refDate.equals("creationDate") && creationDate != null ) {
            refDate = creationDate.toString();
        } else if (refDate.equals("creationDate") && creationDate == null ) {
            throw new ExecutionException("Creation time cannot be determined for " + document.getName() + ". Skipping temporal tagging for this document.");
        } else if (refDate.equals("lastAccessDate") && lastAccessDate != null ) {
            refDate = lastAccessDate.toString();
        } else if (refDate.equals("lastAccessDate") && lastAccessDate == null ) {
            throw new ExecutionException("Last access time cannot be determined for " + document.getName() + ". Skipping temporal tagging for this document.");
        } else if (refDate.equals("lastModifiedDate") && lastModifiedDate != null ) {
            refDate = lastModifiedDate.toString();
        } else if (refDate.equals("lastModifiedDate") && lastModifiedDate == null ) {
            throw new ExecutionException("Last modified time cannot be determined for " + document.getName() + ". Skipping temporal tagging for this document.");
        }

        Annotation annotation = new Annotation(docContent);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, refDate);
        pipeline.annotate(annotation);
        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);

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
     * Sets the creation, last access and last modified dates of the file.
     *
     */
    private void setDocumentFileDates() {

        try {
            LocalDateTime localDateTime;
            Path file = Paths.get(document.getSourceUrl().toURI());
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            if (attr != null) {
                localDateTime = LocalDateTime.ofInstant(attr.creationTime().toInstant(), defaultZoneId);
                creationDate = localDateTime.toLocalDate();
                localDateTime = LocalDateTime.ofInstant(attr.lastAccessTime().toInstant(), defaultZoneId);
                lastAccessDate = localDateTime.toLocalDate();
                localDateTime = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), defaultZoneId);
                lastModifiedDate = localDateTime.toLocalDate();
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}
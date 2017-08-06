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
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;
import java.net.URL;
import java.io.File;

@CreoleResource(
        name = "SUTime: Stanford Temporal Tagger",
        icon = "SUTime.png",
        comment = "Annotate documents with TIMEX3 tags using the SUTime library."
)

public class SUTime extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

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

    private String referenceDate = null;

    @RunTime
    @Optional
    @CreoleParameter(comment = "Reference date of the document.", defaultValue = "")
    public void setReferenceDate(String date) {
        referenceDate = date;
    }

    public String getReferenceDate() {
        return referenceDate;
    }

    @Override
    public void execute() throws ExecutionException {

        String docContent;
        long execStartTime;

        execStartTime = System.currentTimeMillis();
        fireStatusChanged("Performing temporal tagging annotations with SUTime in" + document.getName());
        fireProgressChanged(0);

        if(document == null) throw new ExecutionException("No document to process!");
        docContent = document.getContent().toString();

        Properties props = new Properties();
        AnnotationPipeline pipeline = new AnnotationPipeline();
        pipeline.addAnnotator(new TokenizerAnnotator(false));
        pipeline.addAnnotator(new TimeAnnotator("sutime", props));

        LocalDate todaysDate;
        if (referenceDate.equals("")) {
            todaysDate = new LocalDate();
            referenceDate = todaysDate.toString();
        }

        Annotation annotation = new Annotation(docContent);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, referenceDate);
        pipeline.annotate(annotation);

        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
        if (timexAnnsAll.isEmpty()) return;
        AnnotationSet outputAS = document.getAnnotations(outputASName);
        for (CoreMap cm : timexAnnsAll) {
            List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
            long startOffset = tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            long endOffset =  tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
            FeatureMap timex3Features = Factory.newFeatureMap();
            timex3Features.put("Type", cm.get(TimeExpression.Annotation.class).getTemporal().getTimexType().name());
            timex3Features.put("Value", cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue().toString());
            try {
                outputAS.add(startOffset, endOffset, "TIMEX3", timex3Features);
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        }
    }
}

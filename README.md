# gateplugin-SUTime

A [GATE](https://gate.ac.uk) processing resource plugin to annotate documents with [TIMEML's TIMEX3](http://www.timeml.org/tempeval2/tempeval2-trial/guidelines/timex3guidelines-072009.pdf) tags using the Stanford Temporal Tagger [SUTime](https://nlp.stanford.edu/software/sutime.shtml) Java library. SUTime is a deterministic rule-based temporal tagger for recognizing and normalizing temporal expressions developed by the [Stanford NLP Group](https://nlp.stanford.edu). It is described in detail in the paper: 

Angel X. Chang and Christopher D. Manning. 2012. [SUTIME: A Library for Recognizing and Normalizing Time Expressions](https://nlp.stanford.edu/pubs/lrec2012-sutime.pdf). *8th International Conference on Language Resources and Evaluation (LREC 2012).*

## Getting started

Following the instructions given below will get you a working version of the plugin on your local machine.

### Prerequisites

1. [Download](https://gate.ac.uk/download/) and install the GATE software. $GATE_HOME refers to the GATE root directory.

2. [Download](http://central.maven.org/maven2/de/jollyday/jollyday/0.4.9/jollyday-0.4.9.jar) the Jollyday library version 0.4.9 and place in it $GATE_HOME/lib.

3. [Download](http://central.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0.jar) the Stanford CoreNLP library and place it in $GATE_HOME/lib.

4. [Download](http://central.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0-models.jar) the Stanford CoreNLP models library and place it in $GATEH_HOME/lib.

### Installing


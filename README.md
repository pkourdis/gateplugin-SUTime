# gateplugin-SUTime

A [GATE](https://gate.ac.uk) processing resource plugin to annotate documents with [TIMEML's TIMEX3](http://www.timeml.org/tempeval2/tempeval2-trial/guidelines/timex3guidelines-072009.pdf) tags using the Stanford Temporal Tagger [SUTime](https://nlp.stanford.edu/software/sutime.shtml) Java library. SUTime is a deterministic rule-based temporal tagger for recognizing and normalizing temporal expressions developed by the [Stanford NLP Group](https://nlp.stanford.edu). It is described in detail in the paper: 

Angel X. Chang and Christopher D. Manning. 2012. [SUTIME: A Library for Recognizing and Normalizing Time Expressions](https://nlp.stanford.edu/pubs/lrec2012-sutime.pdf). *8th International Conference on Language Resources and Evaluation (LREC 2012).*

## Getting Up and Running

Following the instructions given below will get you a working version of the plugin on your local machine. It is assumed that GATE software is installed ([download here](https://gate.ac.uk/download/)) and $GATE_HOME refers to the GATE root directory.

### Prerequisites

The folowwing libraries should be placed inside the $GATE_HOME/lib directory: 
- Jollyday library (version 0.4.9) ([download here](http://central.maven.org/maven2/de/jollyday/jollyday/0.4.9/jollyday-0.4.9.jar)).

- Stanford CoreNLP library ([download here](http://central.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0.jar)).

- Stanford CoreNLP models library ([download here](http://central.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0-models.jar)).

### Installation

Download the latest version [from here](https://github.com/pkourdis/gateplugin-SUTime/releases), unzip the file and place the folder inside the $GATE_HOME/plugins directory.

## Usage

The following runtime parameters are available:

`inputASName`: No usage (future releases).

`outputASName`: Name of the annotation set to write the TIMEX3 annotations.
 
`referenceDate`: A date set by the user as reference for normalizing temporal expressions. Permissible values are:

| Value  | Description |
| -----  | ----------- |
|  2017-08-25  | Date provided in "YYYY-MM-DD" format. |
| today  | Today's date (default value). |
| creationDate | Date file was created as recorded by the operating system. |
| lastAccessDate | Date file was last accessed as recorded by the operating system. |
| lastModifiedDate |Date file was last modified as recorded by the operating system. |


User should verify that file dates are supported by the operating system.

## Release History

* **Version 0.2 - September 10, 2017**: Minor corrections to initial version.
* **Version 0.1 - September 10, 2017**: Initial working version.
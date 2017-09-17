# gateplugin-SUTime

## 1. About

A [GATE](https://gate.ac.uk) processing resource plugin to annotate documents with [TIMEML's TIMEX3](http://www.timeml.org/tempeval2/tempeval2-trial/guidelines/timex3guidelines-072009.pdf) tags using the Stanford Temporal Tagger [SUTime](https://nlp.stanford.edu/software/sutime.shtml) Java library. SUTime is a deterministic rule-based temporal tagger for recognizing and normalizing temporal expressions developed by the [Stanford NLP Group](https://nlp.stanford.edu). It is described in detail in the paper: 

Angel X. Chang and Christopher D. Manning. 2012. [SUTIME: A Library for Recognizing and Normalizing Time Expressions](https://nlp.stanford.edu/pubs/lrec2012-sutime.pdf). *8th International Conference on Language Resources and Evaluation (LREC 2012).*

## 2. Getting Up and Running

The instructions given below will get you a working version of the plugin on your local machine. It is assumed that GATE software is installed ([download here](https://gate.ac.uk/download/)) and $GATE_HOME refers to the GATE root directory.

### 2.1 Prerequisites

The following libraries should be placed inside the $GATE_HOME/lib directory: 
- Jollyday library (version 0.4.9) ([download here](http://central.maven.org/maven2/de/jollyday/jollyday/0.4.9/jollyday-0.4.9.jar)).

- Stanford CoreNLP library ([download here](http://central.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0.jar)).

- Stanford CoreNLP models library ([download here](http://central.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0-models.jar)).

### 2.2 Installation

[Download here](https://github.com/pkourdis/gateplugin-SUTime/releases) the latest version, unzip the file and place the folder inside the $GATE_HOME/plugins directory.

## 3. Usage

The following runtime parameters are available:

`inputAnnotationSetName`: Name of the annotation set that has the annotation with the date to be used as reference (if `referenceDate`=annotation).

`inputAnnotationName`: Name of the annotation that has the date to be used as reference (if `referenceDate`=annotation).

`inputFeatureName`: Name of the feature with value the date to be used as reference (if `referenceDate`=annotation). Date should be in "yyy-MM-dd" format.

`outputASName`: Name of the annotation set to write the results. Default value is SUTime.

`outputAnnotationName`: Name of the annotation to write the results. Default value is TIMEX3.

`referenceDate`: Date set by the user as reference for normalizing temporal expressions. Permissible values are:

| Value  | Description |
| -----  | ----------- |
|  2017-08-25  | Date provided in "yyy-MM-dd" format. |
| annotation | The user has to provide the `inputAnnotationSetName`, `inputAnnotationName` and `inputFeatureName`. Date stored in `inputFeatureName` should be in "yyy-MM-dd" format.
| today  | Today's date (default value). |
| creationDate | Date file was created as recorded by the operating system. |
| lastAccessDate | Date file was last accessed as recorded by the operating system. |
| lastModifiedDate |Date file was last modified as recorded by the operating system. |


User should verify that file dates are supported by the operating system.

## 4. Example

Sample screenshots from GATE Developer:

##### Runtime parameters window

![Screenshot](https://user-images.githubusercontent.com/11142121/30518264-99484798-9b2d-11e7-8371-9ccde708a932.png)

##### Document annotated with TIME3X tags

![Screenshot](https://user-images.githubusercontent.com/11142121/30256611-ef21e2e0-9660-11e7-9d02-8de678894b1a.png)

## 5. Release History

* **Version 0.4 - September 16, 2017**: Reference date can also be retrieved from a document annotation. 
* **Version 0.3 - September 12, 2017**: Improve reference date handling. 
* **Version 0.2 - September 10, 2017**: Minor corrections to initial version.
* **Version 0.1 - September 10, 2017**: Initial working version.
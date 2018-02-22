package main.core;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;

/*
    Demo:
    https://github.com/stanfordnlp/CoreNLP/blob/master/src/edu/stanford/nlp/parser/nndep/demo/DependencyParserDemo.java
 */
public class NLParser {
    private MaxentTagger tagger;
    private DependencyParser parser;

    public NLParser() {
        String modelPath = DependencyParser.DEFAULT_MODEL;
        String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";

        tagger = new MaxentTagger(taggerPath);
        parser = DependencyParser.loadFromModelFile(modelPath);
    }

    List<TaggedWord> tag(String sentence) {
        List<HasWord> words = tokenize(sentence);
        return tag(words);
    }

    List<TaggedWord> tag(List<HasWord> words) {
        return tagger.tagSentence(words);
    }

    Collection<TypedDependency> genDependencies(List<TaggedWord> taggedWords) {
        GrammaticalStructure gs = parser.predict(taggedWords);
        return gs.allTypedDependencies();
    }

    private List<HasWord> tokenize(String sentence) {
        DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(sentence));
        return tokenizer.iterator().next();
    }

    // Test
    public static void main(String[] args) {
        NLParser nlParser = new NLParser();
        String text = "return the authors, where the papers of the author in VLDB after 2000 is more than Bob”";

        List<HasWord> words = nlParser.tokenize(text);
        List<TaggedWord> taggedWords = nlParser.tag(words);
        Collection<TypedDependency> dependencies = nlParser.genDependencies(taggedWords);

        taggedWords.stream().forEachOrdered(taggedWord ->
                System.out.println(String.format("%s: %s", taggedWord.word(), taggedWord.tag()))
        );

        for(TypedDependency dep: dependencies) {
            System.out.println(String.format("%d: %s -> %d: %s", dep.gov().index(), dep.gov().word(), dep.dep().index(), dep.dep().word()));
        }
    }
}


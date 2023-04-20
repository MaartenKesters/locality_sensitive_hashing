/**
 * Copyright (c) DTAI - KU Leuven – All rights reserved. Proprietary, do not
 * copy or distribute without permission. Written by Pieter Robberechts, 2023
 */
import java.util.HashSet;
import java.util.Set;
import java.util.Map;


/**
 * Searching similar objects. Objects should be represented as a mapping from
 * an object identifier to a set containing the associated values.
 * 
 */
public abstract class SimilaritySearcher {

    Reader reader;

    public SimilaritySearcher(Reader reader) {
        this.reader = reader;
    }

    /**
     * Returns the pairs of the objectMapping that have a similarity coefficient exceeding threshold
     * @param threshold the similarity threshold
     * @return the pairs with similarity above the threshold
     */
    abstract public Set<SimilarPair> getSimilarPairsAboveThreshold(double threshold);

    /**
     * Jaccard similarity between two sets.
     * @param set1
     * @param set2
     * @return the similarity
     */
    public <T> double jaccardSimilarity(Set<T> set1, Set<T> set2) {
        // Calculate the size of the union of the two sets
        Set<T> union = new HashSet<>(set1);
        union.addAll(set2);
        int unionSize = union.size();

        // Calculate the size of the intersection of the two sets
        Set<T> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        int intersectionSize = intersection.size();

        // Calculate and return the Jaccard similarity
        return (double) intersectionSize / unionSize;
    }
}

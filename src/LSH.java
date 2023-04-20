/**
 * Copyright (c) DTAI - KU Leuven – All rights reserved. Proprietary, do not
 * copy or distribute without permission. Written by Pieter Robberechts, 2023
 */

import java.nio.ByteBuffer;
import java.util.*;
import java.io.*;

/**
 * Implementation of minhash and locality sensitive hashing (LSH) to find
 * similar objects.
 *
 * The LSH should first construct a signature matrix. Based on this, LSH is
 * performed resulting in a mapping of band ids to hash tables (stored in
 * bandToBuckets). From this bandsToBuckets mapping, the most similar items
 * should then be retrieved.
 *
 */
public class LSH extends SimilaritySearcher {

    int numHashes;
    int numBands;
    int numBuckets;
    int seed;

    /**
     * Construct an LSH similarity searcher.
     *
     * @param reader the document reader
     * @param numHashes number of hashes to use to construct the signature matrix
     * @param numBands number of bands to use during locality sensitive hashing
     * @param numBuckets number of buckets to use during locality sensitive hashing
     * @param seed should be used to generate any random numbers needed
     */
    public LSH(Reader reader, int numHashes, int numBands, int numBuckets, int seed){
        super(reader);

        this.numHashes = numHashes;
        this.numBands = numBands;
        this.numBuckets = numBuckets;
        this.seed = seed;
    }


    /**
     * Returns the pairs with similarity above threshold (approximate).
     */
    @Override
    public Set<SimilarPair> getSimilarPairsAboveThreshold(double threshold) {
        // TODO THIS METHOD IS REQUIRED

        Set<SimilarPair> similarPairsAboveThreshold = new HashSet<SimilarPair>();

        // Construct the signature matrix
        int[][] hashTable = Minhash.constructHashTable(numHashes, reader.getNumShingles(), seed);
        int[][] signatureMatrix = Minhash.constructSignatureMatrix(reader, hashTable);

        // Compute the number of rows per band
        int rowsPerBand = signatureMatrix.length / numBands;

        // Allocate reusable byte buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(rowsPerBand * 4);

        // For each band, generate a hash table mapping the vectors of r integers (portion of the minhash signatures in that band) to buckets
        for (int bandId = 0; bandId < numBands; bandId++) {
            List<List<Integer>> bandHashTable = new ArrayList<>(numBuckets);
            for (int bucketId = 0; bucketId < numBuckets; bucketId++) {
                bandHashTable.add(new ArrayList<>());
            }

//            int[][] bandHashTable = new int[numBuckets][];
//            for (int i = 0; i < numBuckets; i++) {
//                bandHashTable[i] = new int[0];
//            }

            // For each column in the band, hash the corresponding vector of r integers to a bucket
            for (int col = 0; col < signatureMatrix[0].length; col++) {
                // Create a vector that needs to be hashed from the portion of the minhash signature of the current column
                int[] vector = new int[rowsPerBand];
                for (int row = 0; row < rowsPerBand; row++) {
                    vector[row] = signatureMatrix[bandId * rowsPerBand + row][col];
                }

                // Convert the int array to a byte array, so we can use the MurmurHash function
                byteBuffer.clear();
                for (int i = 0; i < rowsPerBand; i++) {
                    byteBuffer.putInt(vector[i]);
                }
                byte[] bytes = byteBuffer.array();

                // Hash the portion of the minhash signature of the current column and band and add the column id to the corresponding bucket in the bandHashTable
                int bucketId = Math.abs(MurmurHash.hash32(bytes, bytes.length, this.seed)) % numBuckets;
                List<Integer> bucket = bandHashTable.get(bucketId);
                bucket.add(col);

//                int bucketId = Math.abs(MurmurHash.hash32(bytes, bytes.length, this.seed)) % numBuckets;
//                int[] bucket = bandHashTable[bucketId];
//                int[] newBucket = new int[bucket.length + 1];
//                System.arraycopy(bucket, 0, newBucket, 0, bucket.length);
//                newBucket[bucket.length] = col;
//                bandHashTable[bucketId] = newBucket;
            }

            // Find the candidate pairs for the current band and compute the similarity
            for (int bucketId = 0; bucketId < numBuckets; bucketId++) {
                List<Integer> bucket = bandHashTable.get(bucketId);
                if (bucket.size() > 1) {
                    // Create all pairs of the columns in the bucket
                    for (int i = 0; i < bucket.size(); i++) {
                        for (int j = i + 1; j < bucket.size(); j++) {
                            int col1 = bucket.get(i);
                            int col2 = bucket.get(j);

                            // Create sets from the signature matrix of the columns of the current pair and compute the approximate Jaccard similarity
                            Set<Integer> sign_set1 = getSignatureColumn(signatureMatrix, col1);
                            Set<Integer> sign_set2 = getSignatureColumn(signatureMatrix, col2);
                            double approx_similarity = jaccardSimilarity(sign_set1, sign_set2);

                            // If the approximate similarity is above the threshold, calculate the exact similarity
                            if (approx_similarity >= threshold) {
                                similarPairsAboveThreshold.add(new SimilarPair(reader.getExternalId(col1), reader.getExternalId(col2), approx_similarity));
                            }
                        }
                    }
                }
            }

            // Find the candidate pairs for the current band and compute the similarity
//            bandHashTable.parallelStream().filter(bucket -> bucket != null && bucket.size() > 1).forEach(bucket -> {
//                // Create all pairs of the columns in the bucket
//                for (int i = 0; i < bucket.size(); i++) {
//                    for (int j = i + 1; j < bucket.size(); j++) {
//                        int col1 = bucket.get(i);
//                        int col2 = bucket.get(j);
//
//                        // Create sets from the signature matrix of the columns of the current pair and compute the approximate Jaccard similarity
//                        Set<Integer> sign_set1 = getSignatureColumn(signatureMatrix, col1);
//                        Set<Integer> sign_set2 = getSignatureColumn(signatureMatrix, col2);
//                        double approx_similarity = jaccardSimilarity(sign_set1, sign_set2);
//
//                        // If the approximate similarity is above the threshold, calculate the exact similarity
//                        if (approx_similarity >= threshold) {
//                            similarPairsAboveThreshold.add(new SimilarPair(reader.getExternalId(col1), reader.getExternalId(col2), approx_similarity));
//                        }
//                    }
//                }
//            });
        }

        return similarPairsAboveThreshold;
    }

    

    /**
     * Returns a set of a column (document) in the signature matrix
     */
    public Set<Integer> getSignatureColumn(int[][] signatureMatrix, int col) {
        Set<Integer> column = new HashSet<>();

        for (int[] matrix : signatureMatrix) {
            column.add(matrix[col]);
        }

        return column;
    }

}

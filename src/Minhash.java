/**
 * Copyright (c) DTAI - KU Leuven – All rights reserved. Proprietary, do not
 * copy or distribute without permission. Written by Pieter Robberechts, 2023
 */

import java.util.Random;
import java.util.Set;

/**
 * Class for computing MinHash signatures.
 */
public final class Minhash {

    private Minhash(){
    }

    /**
     * Construct the table of hash values needed to construct the signature matrix.
     * Position (i,j) contains the result of applying function j to row number i.
     * @param numHashes number of hashes that will be used in the signature matrix
     * @param numValues number of unique values that occur in the object set representations (i.e. number of rows of the characteristic matrix)
     * @param seed should be used to generate any random numbers needed
     * @return the (numValues x numHashes) matrix of hash values
     */
    public static int[][] constructHashTable(int numHashes, int numValues, int seed) {
        // TODO THIS METHOD IS REQUIRED

        int[][] hashes = new int[numValues][numHashes];
        Random rand = new Random(seed);
        int prime = Primes.findLeastPrimeNumber(numValues);

        // For each hash function of the form '((ax + b) mod prime) mod numValues' where prime >= numValues, randomly generate integers a and b modulo prime with a != 0
        int[] a = new int[numHashes];
        int[] b = new int[numHashes];
        for (int i = 0; i < numHashes; i++) {
            a[i] = rand.nextInt(prime - 1) + 1;
            b[i] = rand.nextInt(prime);
        }

        // For each row, compute the hash value for each hash function
        for (int i = 0; i < numValues; i++) {
            for (int j = 0; j < numHashes; j++) {
                hashes[i][j] = ((a[j] * i + b[j]) % prime) % numValues;
            }
        }

        return hashes;
    }

    /**
     * Construct the signature matrix.
     *
     * @param reader iterator returning the set representation of objects for which the signature matrix should be constructed
     * @param hashValues (numValues x numHashes) matrix of hash values
     * @return the (numHashes x numObjects) signature matrix
     */
    public static int[][] constructSignatureMatrix(Reader reader, int[][] hashValues) {
        // TODO THIS METHOD IS REQUIRED

        int numDocs = reader.getMaxDocs();
        int numHashes = hashValues[0].length;
        int[][] signatureMatrix = new int[numHashes][numDocs];

        reader.reset();

        // For each document, compute the minimum hash value for each hash function and add it to the signature matrix
        for (int i = 0; i < numDocs; i++) {
            Set<Integer> shingles = reader.next();
            for (int j = 0; j < numHashes; j++) {
                int minHashValue = Integer.MAX_VALUE;
                for (int shingle : shingles) {
                    int hashValue = hashValues[shingle][j];
                    if (hashValue < minHashValue) {
                        minHashValue = hashValue;
                    }
                }
                signatureMatrix[j][i] = minHashValue;
            }
        }

        return signatureMatrix;
    }

}

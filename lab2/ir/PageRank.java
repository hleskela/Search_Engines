/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  

import java.util.*;
import java.io.*;

public class PageRank{

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;
    private int NUMBER_OF_DOCS = 0; //TODO remove
    private double Jc = 0; //TODO remove this too
    final static int NUMBER_OF_WALKS = 25;

    /**
     * Mapping from document integer name in linkDavis.txt to actual document name (- the .f)
     **/
    HashMap<String,String> actualDocNames = new HashMap<String,String>();

    /**
     * A list of scores for the final sorting and printout
     **/
    LinkedList<Score> scores = new LinkedList<Score>();

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON  = 0.00001; //original was 0.0001, converged after 7.  0.000001 converged after 34

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 19;//1000;

    
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	readDocNames();
	computePagerank( noOfDocs );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previously unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new Hashtable<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	    // Compute the number of sinks.
	    for ( int i=0; i<fileIndex; i++ ) {
		if ( out[i] == 0 )
		    numberOfSinks++;
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */

    private void readDocNames() {
	String filename = "articleTitles.txt";
	int fileIndex = 0;
	try {
	    System.err.println( "Reading file articleTitles.txt... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ){
		int index = line.indexOf( ";" );
		String docID = line.substring( 0, index );
		String docName = line.substring(index+1, line.length()).concat(".f"); //TODO is this slow?
		actualDocNames.put(docID,docName);
	    }
	} catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	} catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Done reading the articleTitles.txt file!" );
    }
    
    


    /*
     *   Computes the pagerank of each document.
     */
    void computePagerank( int numberOfDocs ) {
	this.NUMBER_OF_DOCS = numberOfDocs;
	double jumpProbability = 1.0/(double) numberOfDocs;
	double[] x = new double[numberOfDocs];
	double[] xPrime = new double[numberOfDocs];

	double[] G = new double[numberOfDocs];
	G = calculateG(G);

	xPrime[0] = 1; //Initial starting point
	x[0] = 0;
	for(int j = 1 ; j < numberOfDocs; j++){
	    xPrime[j] = 0;
	    x[j] = 0;
	}
	
	double[] deltaX = calculateDifferenceVector(x, xPrime);
	double length = calculateLength(deltaX);
	int i= 0;

	while(Math.abs(length) > EPSILON && MAX_NUMBER_OF_ITERATIONS > i){
	    i++;
	    printXPrimeDebug(xPrime, i, length);
	    x = xPrime;
	    xPrime = calculateNewXPrime(xPrime, G);
	    deltaX = calculateDifferenceVector(x, xPrime);
	    length = calculateLength(deltaX);
	}
	createPrintableInfo(xPrime);
	//printXPrimeDebug(xPrime, i, length);
	
    }

    /**
     *
     **/
    private void createPrintableInfo(double[] xPrime){
	Score s;
	for(int h = 0; h<NUMBER_OF_DOCS; h++){
	    String docname = docName[h];
	    s = new Score(xPrime[h], docname, actualDocNames.get(docname));
	    scores.add(s);
	}
	Collections.sort(scores);
	for(int i=0; i<NUMBER_OF_DOCS;i++){
	    Score tmp = scores.get(i);
	    //System.err.println(tmp.docID + " : " + tmp.docName + " : " + tmp.score);
	    System.err.println(tmp.docName + ";" + tmp.score);
	}
    }

    /**
     * Calculates G = cP + (1-c)J, where P is the transition matrix,
     * J is the Jump matrix, and c =0.85 (BORED = 0.15)
     * If a document doesn't have any outlinks, then it can jump to 
     * any other document, meaning P is 1/(NUMBER_OF_DOCS-1)

     *
     **/
    private double[] calculateG(double[]G){
	System.err.println("Calculating G");
	this.Jc = (BORED/(double) NUMBER_OF_DOCS);
	for(int i = 0; i < this.NUMBER_OF_DOCS; i++){
	    double cP;
	    if( 0 == out[i]){
		cP = (1.0-BORED)/(double) (NUMBER_OF_DOCS-1);
	    } else{
		cP = (1.0-BORED)/(double) out[i];
	    }
	    G[i] = cP+this.Jc;
	}
	System.err.println("Done calculating G");
	return G;
    }

    private void printXPrime(double[] xPrime){
	for(int h = 0; h<NUMBER_OF_DOCS; h++){
	    String docname = docName[h];
	    System.err.println(xPrime[h]+ " "+ docname +" has docName "+ actualDocNames.get(docname));
	}
    }

    private void printXPrimeDebug(double[] xPrime, int i, double length){	
	double finalSum = 0.0;
	for(int h = 0; h<NUMBER_OF_DOCS; h++){
	    finalSum+=xPrime[h];
	}
	System.err.println("Number of iterations: "+i);
	System.err.println("Epsilon vs diff: Math.abs(length) "+Math.abs(length) +"Epsilon " + EPSILON);
	System.err.println("FinalSum: "+finalSum);
    }

    private double[] calculateNewXPrime(double [] xPrime, double[] G){
	double[] newXPrime = new double[NUMBER_OF_DOCS];
	Hashtable<Integer,Boolean> rowInG = new Hashtable<Integer,Boolean>();
	for(int i = 0; i < NUMBER_OF_DOCS ; i++){
	    rowInG = link.get(i);
	    if(rowInG == null){
		for(int j = 0; j < NUMBER_OF_DOCS; j++){
		    newXPrime[j] += xPrime[i] * G[i];
		}
		newXPrime[i] -= (1.0-BORED)/(NUMBER_OF_DOCS-1)*xPrime[i];
	    } else{
		for(int j=0; j< NUMBER_OF_DOCS; j++){
		    if(rowInG.keySet().contains(j)){
			newXPrime[j] += xPrime[i]*G[i];
		    } else {
			newXPrime[j] += xPrime[i]*Jc;
		    }
		}
	    }
	}
	return newXPrime;
    }


    private double[] calculateDifferenceVector(double[] x, double[] xPrime ){
	int numberOfDocs = x.length;
	double[] deltaX = new double[numberOfDocs];
	for(int k = 0; k < numberOfDocs; k++){
	    deltaX[k] = x[k]-xPrime[k]; // Calculated according to lecture slides
	}

	return deltaX;
    }

    private double calculateLength(double[] deltaX){
	double length = 0.0;
	int numberOfDocs = deltaX.length;
	for(int k = 0; k < numberOfDocs; k++){
	    double d = deltaX[k];
	    length += d*d;
	}
	length = Math.sqrt(length);
	return length;
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }


    /**
     * A class to keep track of the three parameters of a document.
     * What a mess, huh!?!
     **/
    private class Score implements Comparable<Score>, Serializable{
	public double score;
	public String docID;
	public String docName;

	public Score(double score, String docID, String docName){
	    this.score = score;
	    this.docID = docID;
	    this.docName = docName;
	}

	public int compareTo( Score other ) {
	    return Double.compare( other.score, score );
	}

    }
}
 

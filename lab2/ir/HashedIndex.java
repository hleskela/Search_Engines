/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) {
	/*	if(token.equals("zombie")){
	    System.out.println("Zombie warning when building index, offset: " + offset);
	    }*/

	if(index.containsKey(token) == false){	    
	    PostingsList pl = new PostingsList();
	    PostingsEntry pe =  new PostingsEntry();
	    pe.docID = docID;
	    pe.offset.add(offset);
	    pl.add(pe);
	    index.put(token, pl);
	} else {	    
	    // if the dictionary has the token, add the occurence in postingslist
	    PostingsList pl = getPostings(token);

	    // check if the token was added previously with this docID
	    if(pl.getLast().docID != docID){
		PostingsEntry pe = new PostingsEntry();
		pe.docID = docID;
		pe.offset.add(offset);
		pl.add(pe);
		index.put(token, pl);
	    } else{
		pl.getLast().offset.add(offset);
	    }
	}
    }


    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
	if(index.isEmpty() == false){
	    Iterator<String> it = index.keySet().iterator();
	    return it;
	}
	return null;
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
	// this line is the only thing needed in method, returns null on fail
	return index.get(token);
    }


    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
	LinkedList<String> terms = query.terms;
	LinkedList<PostingsList> pList = new LinkedList();
	PostingsList answer = new PostingsList();
	if(query.terms.size() > 0){
	    for(String s : terms){
		System.out.println("Search terms is " + s);
		pList.add(getPostings(s));
	    }
	    

	    if(queryType == Index.RANKED_QUERY){
		if(rankingType == Index.TF_IDF){
		    answer = rankedSearch(terms);
		} else if(rankingType == Index.COMBINATION){
		    answer = rankedSearch(terms);
		    answer = tfPagerankCombined(answer,1,10000);
		} else if(rankingType == Index.PAGERANK){
		    answer = rankedSearch(terms);
		    answer = tfPagerankCombined(answer,0,1);
		}
		return answer; //TODO implement stuff
	    }

	    // This should be the same for both intersection query and phrase, i.e. if only one word
	    // but not for ranked retrieval
	    if(pList.size() == 1){
		return pList.get(0);
	    }

	    
	    int i = 2;
	    int limit = pList.size(); // number of search terms
	    
	    answer = intersect(pList.get(0), pList.get(1), queryType);
	    while(i < limit){
		answer = intersect(answer, pList.get(i), queryType);
		i++;
	    }	
	    
	    return answer;
	    
	}
	return null;
    }

    public PostingsList tfPagerankCombined(PostingsList answer, double x, double y){
	for(PostingsEntry pe : (LinkedList<PostingsEntry>)answer.getList()){
	    String docName = docIDs.get(""+pe.docID);
	    docName = docName.substring(10,docName.length()); //TODO gets rid of davisWiki/
	    double pagerank = docPageRanks.get(docName);
	    pe.score = pe.score*x + pagerank*y;
	}
	answer.sort();
	return answer;
    }

    /**
     *
     **/
    public PostingsList getPageRankPostingsList(){
	PostingsList answer = new PostingsList();
	for(Map.Entry<String,String> entry : docIDs.entrySet()){
            int docID = Integer.parseInt(entry.getKey());
            String docName = entry.getValue();
	    docName = docName.substring(10,docName.length());//10 removes the davisWiki/
	    System.err.println(docName);
	    double rank = docPageRanks.get(docName);
            PostingsEntry pe = new PostingsEntry(docID, rank);
            answer.add(pe);
        }
	answer.sort();
	return answer;
    }

    /**
     * This is an ugly copy-paste of the intersect method. TODO change this
     **/
    public PostingsEntry positionalIntersect(LinkedList<Integer> offset1, LinkedList<Integer> offset2, int docID){	
	ListIterator<Integer> it1 = offset1.listIterator(0);
 	ListIterator<Integer> it2 = offset2.listIterator(0);
	int limitpl1 = offset1.size();
	int limitpl2 = offset2.size();
	int counter1 = 0;
	int counter2 = 0;
	boolean isFirstTime = true;
	int position1 = 0;
	int position2 = 0;
	PostingsEntry answer = new PostingsEntry();
	answer.docID = docID;
	System.err.println("IN POSITIONAL INTERSECT");
	while(counter1 < limitpl1 && counter2 < limitpl2){
	    // Ugly, ask for help
	    if(isFirstTime){
		position1 = it1.next();
		position2 = it2.next();
	    	isFirstTime = false;
	    }

	    if(position2-position1 == 1){
		System.err.println("docID "+ docID);
		System.err.println("Position1 "+ position1);
		System.err.println("Position2 "+ position2);
		answer.offset.add(position2);
		counter1++;
		counter2++;
		if(it1.hasNext())
		    position1 = it1.next();
		if(it2.hasNext())
		    position2 = it2.next();
	    } else if( position1 < position2){
		if(it1.hasNext())
		    position1 = it1.next();
		counter1++;
	    } else{
		if(it2.hasNext())
		    position2 = it2.next();
		counter2++;
	    }
	}
	if(answer.offset.size()>0){
	    return answer;
	}
	return null;
    }

    /**
     * The intersect algorithm presented in the book, page 11 (48 in pdf)
     * TODO: sorted intersect?
     **/
    public PostingsList intersect(PostingsList pl1, PostingsList pl2, int queryType){

	if(pl1 == null || pl2 == null){
	    return null;
	}
	
	int limitpl1 = pl1.size();
	int limitpl2 = pl2.size();
	// is this even needed?
	if(limitpl1 == 0 || limitpl2 == 0){
	    return null;
	}
	
	ListIterator<PostingsEntry> it1 = pl1.listIterator(0);
	ListIterator<PostingsEntry> it2 = pl2.listIterator(0);

	PostingsList answer = new PostingsList();
	PostingsEntry pe1 = new PostingsEntry();
	PostingsEntry pe2 = new PostingsEntry();
	
	boolean isFirstTime = true;
	int counter1 = 0;
	int counter2 = 0;
	while(counter1 < limitpl1 && counter2 < limitpl2){
	    // Ugly, ask for help
	    if(isFirstTime){
		pe1 = it1.next();
		pe2 = it2.next();
	    	isFirstTime = false;
	    }

	    if(pe1.docID == pe2.docID){

		if(Index.PHRASE_QUERY == queryType){
		    PostingsEntry phrasePostingsEntry = positionalIntersect(pe1.offset, pe2.offset, pe2.docID);
		    if(null != phrasePostingsEntry){
			answer.add(phrasePostingsEntry);
		    }
		    if(it1.hasNext())
			pe1 = it1.next();
		    if(it2.hasNext())
			pe2 = it2.next();
		    counter1++;
		    counter2++;
		    continue;
		}

		answer.add(pe1);
		if(it1.hasNext())
		    pe1 = it1.next();
		if(it2.hasNext())
		    pe2 = it2.next();
		counter1++;
		counter2++;
	    } else if( pe1.docID < pe2.docID){
		if(it1.hasNext())
		    pe1 = it1.next();
		counter1++;
	    } else{
		if(it2.hasNext())
		    pe2 = it2.next();
		counter2++;
	    }
	}
	
	ListIterator<PostingsEntry> li = answer.listIterator(0);
	PostingsEntry pe = new PostingsEntry();
	while(li.hasNext()){
	    pe = li.next();
	}
	
	return answer;
    }
    

    /**
     * Calculates the scores for the different PostingsEntries
     */ 
    public void calculateScores(){
	double n = docIDs.size();
	for(PostingsList pl : index.values()){
	    double df = pl.size();
	    double idf = Math.log(n/df); //TODO using natural log, should it be log10?
	    for(PostingsEntry pe : (LinkedList<PostingsEntry>) pl.getList()){
		//System.err.println("docLengths.get"+pe.docID+" : "+docLengths.get(""+pe.docID));
		pe.score = pe.offset.size()*idf; //docLengths.get(""+pe.docID); We normalize with magnitude, thats why we don't lenght normalize
	    }
	}
    }    


    /**
     * The search using fastCosineScore, see link for details
     * http://www.ics.uci.edu/~djp3/classes/2008_09_26_CS221/Lectures/Lecture26.pdf
     **/
    public PostingsList rankedSearch(LinkedList<String> terms){
	double N = docIDs.size();
	PostingsList answer = new PostingsList();
	double queryTf = 1/terms.size(); //TODO only if the words are unique
	LinkedList<PostingsList> postLists = new LinkedList<PostingsList>();
	
	clearHashMaps();
	
	for(String s : terms){
	    PostingsList queryPostingsList = index.get(s); //TODO returns one list, no for loop needed down below
	    if(queryPostingsList == null){
		continue;
		//return answer; //TODO fix this so that it works for some words that don't exist, but some w
	    }
	    double df = queryPostingsList.size();
	    double queryScore = Math.log(N/(df+1)); //TODO why +1? and should it be 1+ log?
		for(PostingsEntry pe : (LinkedList<PostingsEntry>) queryPostingsList.getList()){
		    if(docScores.get(""+pe.docID) == null){
			docScores.put(""+pe.docID, queryScore*pe.score);
			docMagnitude.put(""+pe.docID, pe.score*pe.score); //TODO don't need to if this, since mag is only updated with score
		    } else{
			docScores.put(""+pe.docID, docScores.get(""+pe.docID) + (queryScore*pe.score));
			docMagnitude.put(""+pe.docID, docMagnitude.get(""+pe.docID)+(pe.score*pe.score));
		    }
		}
	}
	
	// Final step, calculates the rank with scores and magnitude
	for(Map.Entry<String,Double> entry : docScores.entrySet()){
	    int docID = Integer.parseInt(entry.getKey());
	    double mag = docMagnitude.get(entry.getKey());
	    double rank = entry.getValue()/Math.sqrt(mag);

	    //Debug info
	    /*System.err.println(docIDs.get(""+docID));
	    System.err.println("The querys score, round 2: " + entry.getValue());
	    System.err.println("The querys mag, round 2: " + mag);
	    System.err.println("Actual rank that is set: "+rank);*/
	    

	    PostingsEntry pe = new PostingsEntry(docID, rank);	    
	    answer.add(pe);
	} 
	
	answer.sort();
	return answer;
    }

    private void clearHashMaps(){
	docScores.clear();
	docMagnitude.clear();
    }

    /**
     * This function is used to get the rank from the text file of docNames and rank.
     * We store this in a HashMap to combine it later with the tf-idf score.
     **/
    public void readRankScore() {
	System.err.println("Start reading the pagerank19.out....");
        String filename = "pagerank19.out";
        try {
            BufferedReader in = new BufferedReader( new FileReader( filename ));
            String line;
            while ((line = in.readLine()) != null){
                int index = line.indexOf( ";" );
                String docName = line.substring( 0, index );
                double docRank = Double.parseDouble(line.substring(index+1, line.length())); //TODO is this slow?
                docPageRanks.put(docName, docRank);
            }
        } catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	} catch ( IOException e ) {
            System.err.println( "Error reading file " + filename );
        }
        System.err.println( "Done reading the pagerank19.out file!" );
	System.err.println( "Size of docPageRanks is: " + docPageRanks.size() );
    }


    /**
     *  No need for cleanup in a HashedIndex.
     * Then why did you put it here?!?!? / Hannes
     */
    public void cleanup() {
    }
}

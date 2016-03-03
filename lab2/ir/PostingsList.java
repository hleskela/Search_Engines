/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.Serializable;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {
    
    /** The postings list as a linked list. */
    private LinkedList<PostingsEntry> list = new LinkedList<PostingsEntry>();


    /**  Number of postings in this list  */
    public int size() {
	return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
	return list.get( i );
    }

    /**  Returns the ith posting */
    public LinkedList getList() {
	return list;
    }


    public boolean contains(PostingsEntry pe){
	return list.contains(pe);
    }
    //
    //  YOUR CODE HERE
    //

    public PostingsEntry getLast(){
	return list.getLast();
    }

    public PostingsEntry pollFirst(){
	return list.pollFirst();
    }

    public ListIterator<PostingsEntry> listIterator(int index){
	return list.listIterator(index);
    }

    /**
     *
     **/
    public void add(PostingsEntry e){
	list.add(e); //TODO is anything else needed?
    }

    public void sort(){
	Collections.sort(list);
    }
}
	

			   

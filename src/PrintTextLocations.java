/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * This is an example on how to get some x/y coordinates of text.
 *
 * @author Ben Litchfield
 */
public class PrintTextLocations extends PDFTextStripper
{
	
	protected float contentYScale;
	protected HashMap<Float, Integer> mapOfYScales;
	protected final String openSup = "<sup>";
	protected final String closedSup = "</sup>";
	protected final String openTitle = "<title>";
	protected final String closedTitle = "</title>";
	
	protected enum TagType {
		OPEN_SUP,
		CLOSED_SUP,
		OPEN_TITLE,
		CLOSED_TITLE
	}
	
	protected class TagPosition
	{
		protected TagType tagType;
		protected int index;
		
		public TagPosition(TagType tagType, int index)
		{
			this.tagType = tagType;
			this.index = index;
		}
		
		public TagType getTagType()
		{
			return this.tagType;
		}
		
		public int getIndex()
		{
			return this.index;
		}
	}
    /**
     * Instantiate a new PDFTextStripper object.
     *
     * @throws IOException If there is an error loading the properties.
     */
    public PrintTextLocations() throws IOException
    {
    }

    /**
     * This will print the documents data.
     *
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main( String[] args ) throws IOException
    {
        /*if( args.length != 1 )
        {
            usage();
        }
        else*/
        {
            try (PDDocument document = PDDocument.load(new File("C:\\Code\\java_workspace\\bio.pdf")))
            {
                PDFTextStripper stripper = new PrintTextLocations();
                //stripper.setSortByPosition( true );
                //stripper.setStartPage( 0 );
                //stripper.setEndPage( document.getNumberOfPages() );

                File f = new File("test.txt");
                Writer out = new OutputStreamWriter(new FileOutputStream(f));
                stripper.writeText(document, out);
                out.close();
                
            }
        }
    }
    
    @Override
    protected void writePage() throws IOException
    {
    	mapOfYScales = new HashMap<Float, Integer>();
    	System.out.println(charactersByArticle);
    	for(List<TextPosition> textList : charactersByArticle)
    	{
    		for(TextPosition text : textList) 
    		{
    			Float yScale = text.getYScale();
    			if(mapOfYScales.containsKey(yScale))
    			{
    				mapOfYScales.put(yScale, mapOfYScales.get(yScale) + 1);
    			}
    			else
    			{
    				mapOfYScales.put(yScale, 1);
    			}
    		}
    	}
    	System.out.println(mapOfYScales);
    	contentYScale = 0;
    	int tempMaxCount = 0;
    	for(Map.Entry<Float, Integer> entry : mapOfYScales.entrySet())
    	{
    		if(tempMaxCount < entry.getValue())
    		{
    			tempMaxCount = entry.getValue();
    			contentYScale = entry.getKey();
    		}
    	}
    	System.out.println(" yscale = " + contentYScale + " count = " + tempMaxCount);
    	super.writePage();
    }

    /**
     * Override the default functionality of PDFTextStripper.
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException
    {
    	//System.out.println(textPositions);
    	int superStartIndex = -1, superEndIndex = -1;
    	List<TagPosition> tagPositionList = new ArrayList<TagPosition>();
    	Stack<TagPosition> stack = new Stack<TagPosition>();
    	for(int i = 0;i < textPositions.size();i++)
    	{
    		float yScale = textPositions.get(i).getYScale();
    		if(yScale < contentYScale && 
    			(stack.isEmpty() || stack.peek().getTagType() != TagType.OPEN_SUP))
    		{
    			if(!stack.isEmpty() && stack.peek().getTagType() == TagType.OPEN_TITLE)
    			{
    				stack.push(new TagPosition(TagType.CLOSED_TITLE, i - 1));
    			}
    			stack.push(new TagPosition(TagType.OPEN_SUP, i));
    		}
    		else if(yScale >= contentYScale && 
    			(!stack.isEmpty() && stack.peek().getTagType() == TagType.OPEN_SUP))
    		{
    			stack.push(new TagPosition(TagType.CLOSED_SUP, i - 1));
    		}
    		
    		if(yScale > contentYScale && 
    			(stack.isEmpty() || stack.peek().getTagType() != TagType.OPEN_TITLE))
    		{
    			if(!stack.isEmpty() && stack.peek().getTagType() == TagType.OPEN_SUP)
    			{
    				stack.push(new TagPosition(TagType.CLOSED_SUP, i - 1));
    			}
    			stack.push(new TagPosition(TagType.OPEN_TITLE, i));
    		}
    		else if(yScale <= contentYScale && 
    			(!stack.isEmpty() && stack.peek().getTagType() == TagType.OPEN_TITLE))
    		{
    			stack.push(new TagPosition(TagType.CLOSED_TITLE, i - 1));
    		}
    		
    		if(i == textPositions.size() - 1 && 
    			!stack.isEmpty() && 
    			(stack.peek().getTagType() == TagType.OPEN_SUP || stack.peek().getTagType() == TagType.OPEN_TITLE))
    		{
    			switch(stack.peek().getTagType())
    			{
    				case OPEN_SUP:
    					stack.push(new TagPosition(TagType.CLOSED_SUP, i));
    					break;
    				case OPEN_TITLE:
    					stack.push(new TagPosition(TagType.CLOSED_TITLE, i));
    					break;
    				default:
    					break;
    			}
    		}
    		/*
    		if(yScale < contentYScale && superStartIndex == -1)
    		{
    			superStartIndex = i;
    		}
    		else if(superStartIndex != -1 && yScale >= contentYScale)
    		{
    			superEndIndex = i - 1;
    		}
    		
    		if(superStartIndex != -1 && superEndIndex != -1)
    		{
    			tagPositionList.add(new TagPosition(TagType.OPEN_SUP, superStartIndex));
    			tagPositionList.add(new TagPosition(TagType.CLOSED_SUP, superEndIndex));
    			superStartIndex = -1;
    			superEndIndex = -1;
    		}
    		*/
    		//System.out.println("[" + i + "] yscale = " + textPositions.get(i).getYScale() + " char = " + textPositions.get(i).getUnicode());
    	}
    	/*
    	if(superStartIndex != -1) {
    		tagPositionList.add(new TagPosition(TagType.OPEN_SUP, superStartIndex));
    		tagPositionList.add(new TagPosition(TagType.CLOSED_SUP, textPositions.size() - 1));
    	}
    	*/
    	/*
    	if(tagPositionList.size() != 0)
    	{
    		StringBuilder sb = new StringBuilder(string);
    		for(int i = tagPositionList.size() - 1;i >= 0;i--)
    		{
    			TagPosition tagPosition = tagPositionList.get(i);
    			switch(tagPosition.getTagType())
    			{
    				case OPEN_SUP:
    					sb.insert(tagPosition.getIndex(), openSup);
    					break;
    				case CLOSED_SUP:
    					sb.insert(tagPosition.getIndex() + 1, closedSup);
    					break;
    				case OPEN_TITLE:
    					sb.insert(tagPosition.getIndex(), openTitle);
    					break;
    				case CLOSED_TITLE:
    					sb.insert(tagPosition.getIndex() + 1, closedTitle);
    					break;
    				default:
    					break;
    			}
    		}
    		string = sb.toString();
    	}
    	*/
    	if(!stack.isEmpty())
    	{
    		StringBuilder sb = new StringBuilder(string);
    		while(!stack.isEmpty())
    		{
    			TagPosition tagPosition = stack.pop();
    			switch(tagPosition.getTagType())
    			{
    				case OPEN_SUP:
    					sb.insert(tagPosition.getIndex(), openSup);
    					break;
    				case CLOSED_SUP:
    					sb.insert(tagPosition.getIndex() + 1, closedSup);
    					break;
    				case OPEN_TITLE:
    					sb.insert(tagPosition.getIndex(), openTitle);
    					break;
    				case CLOSED_TITLE:
    					sb.insert(tagPosition.getIndex() + 1, closedTitle);
    					break;
    				default:
    					break;
    			}
    		}
    		string = sb.toString();
    	}
    	
    	System.out.println(string);

    	writeString(string);
    }
    
    protected void writeString(String text) throws IOException
    {
    	System.out.println("XXX write to output: " + text);
        output.write(text);
    }
    
    /**
     * This will print the usage for this document.
     */
    private static void usage()
    {
        System.err.println( "Usage: java " + PrintTextLocations.class.getName() + " <input-pdf>" );
    }
}
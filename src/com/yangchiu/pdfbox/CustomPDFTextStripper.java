package com.yangchiu.pdfbox;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Tag words smaller than average size as <sup></sup>
 * and
 * Tag words larger then average size as <title></title>
 */
public class CustomPDFTextStripper extends PDFTextStripper
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
	
	public CustomPDFTextStripper() throws IOException {
		super();
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
        if( args.length != 1 )
        {
            usage();
        }
        else
        {
            try (PDDocument document = PDDocument.load(new File(args[0])))
            {
                PDFTextStripper stripper = new CustomPDFTextStripper();

                Writer out = new OutputStreamWriter(System.out);
                stripper.writeText(document, out);
                out.close();
            }
        }
    }
    
    @Override
    protected void writePage() throws IOException
    {
    	mapOfYScales = new HashMap<Float, Integer>();
    	//System.out.println(charactersByArticle);
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
    	//System.out.println(mapOfYScales);
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

    	super.writePage();
    }

    /**
     * Override the default functionality of PDFTextStripper.
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException
    {
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
    	}

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
    	
    	//System.out.println(string);

    	writeString(string);
    }

    /**
     * This will print the usage for this document.
     */
    private static void usage()
    {
        System.err.println( "Usage: java " + CustomPDFTextStripper.class.getName() + " <input-pdf>" );
    }
}
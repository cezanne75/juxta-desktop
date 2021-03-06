/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Educational Community License (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.opensource.org/licenses/ecl1.txt">
 * http://www.opensource.org/licenses/ecl1.txt.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002-2006 by 
 * The Rector and Visitors of the University of Virginia. 
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package edu.virginia.speclab.legacy.juxta.author.model.manifest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.virginia.speclab.exceptions.ReportedException;
import edu.virginia.speclab.legacy.diff.document.DocumentWriter;
import edu.virginia.speclab.legacy.diff.token.TokenizerSettings;
import edu.virginia.speclab.legacy.juxta.author.model.JuxtaSession_1_3_1;
import edu.virginia.speclab.util.SimpleLogger;

public class DocumentManifestXMLFile
{
    private ComparisonSetNode comparisonSetNode;
	private AnnotationSetNode annotationSetNode;
    private File basePath, cacheDirectory;
	private File manifestFile;
    
    public DocumentManifestXMLFile( File file, File cacheDirectory ) throws ReportedException  
    {
		this.manifestFile = file;
		this.cacheDirectory = cacheDirectory;  
	    loadDocument(file);
    }
    
    public DocumentManifestXMLFile( JuxtaSession_1_3_1 session )
    {
        createFile(session);
    }
    
    private void createFile(JuxtaSession_1_3_1 session)
    {		
        basePath = session.getBasePath();
		manifestFile = new File(basePath.getPath() + "/manifest.xml");
        comparisonSetNode = new ComparisonSetNode(session);
		annotationSetNode = new AnnotationSetNode(session);
    }
    
    public TokenizerSettings getTokenizerSettings()
    {
        if( comparisonSetNode != null )
        {
            return comparisonSetNode.getSettings();
        }
        
        return null;
    }

    /**
     * Load the Juxta document from a <code>File</close> object.
     * @param juxtaFile The Juxta data file.
     * @throws JuxtaFileParsingException
     * @throws JuxtaFileParsingException If the file is not well formed XML or the document
     * itself is not valid.
     * @throws JuxtaSystemRequirementsException If the JAXP XML processor packages are not installed.
     */
    private void loadDocument(File juxtaFile) throws ReportedException
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Node document = parser.parse(juxtaFile);
            basePath = new File(juxtaFile.getAbsoluteFile().getParent());
            loadDocument(document);
        }
        catch (SAXException e)
        {
            throw new ReportedException(e, juxtaFile.getName()
                    + " is not well formed.");
        }
        catch (IOException e)
        {
            throw new ReportedException( e,
                    "IO Exception parsing document: " + juxtaFile.getName());
        } 
        catch (FactoryConfigurationError e)
        {
            throw new ReportedException( "Could not locate factory class.","Factory Configuration Error" );
        } 
        catch (ParserConfigurationException e)
        {
            throw new ReportedException( e,
                    "Could not locate a JAXP Parser.");
        }
    }

    /**
     * Load the Juxta document from a &ltjuxta&gt element.
     * 
     * @param document
     *            The DOM Node of the juxta element.
     * @throws JuxtaFileParsingException
     *             If the element is not valid.
     */
    private void loadDocument(Node document)
            throws ReportedException
    {
        boolean juxtaNodeFound = false;

        NodeList documentChildren = document.getChildNodes();
        if (documentChildren != null)
        {
            for (int i = 0; i < documentChildren.getLength(); i++)
            {
                Node currentNode = documentChildren.item(i);

                if (currentNode.getNodeType() == Node.ELEMENT_NODE
                     && currentNode.getNodeName().equals("juxta"))
                {
                    juxtaNodeFound = true;
                    if( loadComparisonSet(currentNode) )
                    {
                        if( !loadAnnotationSet(currentNode) )
                        {
                            SimpleLogger.logError("<annotation-set> not found.");
                        }
                    }
                    else
                    {
                        throw new ReportedException("<comparison-set> not found.","<comparison-set> not found.");
                    }                    
                }
            }
        }

        if (juxtaNodeFound == false)
            throw new ReportedException("Juxta element node not found.","Juxta element node not found.");
    }

    private boolean loadComparisonSet(Node juxtaNode) throws ReportedException
    {
        NodeList juxtaChildren = juxtaNode.getChildNodes();

        if (juxtaChildren != null)
        {
            for (int i = 0; i < juxtaChildren.getLength(); i++)
            {
                Node currentNode = juxtaChildren.item(i);

                if (currentNode.getNodeType() == Node.ELEMENT_NODE
                        && currentNode.getNodeName().equals("comparison-set"))
                {                    
                    comparisonSetNode = new ComparisonSetNode(currentNode);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean loadAnnotationSet(Node juxtaNode) throws ReportedException
    {
        NodeList juxtaChildren = juxtaNode.getChildNodes();

        if (juxtaChildren != null)
        {
            for (int i = 0; i < juxtaChildren.getLength(); i++)
            {
                Node currentNode = juxtaChildren.item(i);

                if (currentNode.getNodeType() == Node.ELEMENT_NODE
                        && currentNode.getNodeName().equals("annotation-set"))
                {                    
                    annotationSetNode = new AnnotationSetNode(currentNode);
                    return true;
                }
            }
        }

        return false;
    }
    
    public LinkedList createDocumentEntrySet() throws ReportedException
    {
        return comparisonSetNode.createDocumentEntryList(basePath,cacheDirectory);
    }

    public void save() throws IOException
    {
        SimpleLogger.logInfo("Saving manifest file.");
        
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");        
        buffer.append("<juxta>\n");        
        buffer.append("\t<comparison-set name=\""+DocumentWriter.escapeText( comparisonSetNode.getName() )+"\" ");
        
        TokenizerSettings settings = comparisonSetNode.getSettings();
        
        // add tokenizer setting attributes to comparison set element
        buffer.append("filter-whitespace=\""+settings.filterWhitespace()+"\" ");
        buffer.append("filter-punctuation=\""+settings.filterPunctuation()+"\" ");
        buffer.append("filter-case=\""+settings.filterCase()+"\">\n");
        
        for( Iterator i = comparisonSetNode.getComparandList().iterator(); i.hasNext(); )
        {
            ComparandNode node = (ComparandNode) i.next();
            
            buffer.append("\t\t<comparand " +
                           "docid=\""+Integer.toString(node.getDocumentID())+"\" " +
                           "fragstart=\""+Integer.toString(node.getFragmentStart())+"\" " +
                           "fragend=\""+Integer.toString(node.getFragmentEnd())+"\" " +
                           ">\n");
            buffer.append("\t\t\t<file>"+DocumentWriter.escapeText( "docs/"+node.getFileName() )+"</file>\n");                       
            buffer.append("\t\t</comparand>\n");
        }
        
        buffer.append("\t</comparison-set>\n");

		buffer.append("\t<annotation-set>\n");
		
        for( Iterator i = annotationSetNode.getAnnotationNodeList().iterator(); i.hasNext(); )
        {
            AnnotationNode node = (AnnotationNode) i.next();
            
            buffer.append("\t\t<annotation>\n");
            buffer.append("\t\t\t<difference type=\""+node.getDifferenceTypeString()+"\">\n");
			
			buffer.append("\t\t\t\t<base docid=\""+node.getBaseDocumentID()+"\" ");
					buffer.append("offset=\""+node.getBaseOffset()+"\" ");
					buffer.append("length=\""+node.getBaseLength()+"\" ");
			buffer.append(">");
			if( node.getBaseQuote() != null ) buffer.append(node.getBaseQuote());
			buffer.append("</base>\n");
			
			buffer.append("\t\t\t\t<witness docid=\""+node.getWitnessDocumentID()+"\" ");
				buffer.append("offset=\""+node.getWitnessOffset()+"\" ");
				buffer.append("length=\""+node.getWitnessLength()+"\" ");
			buffer.append(">");
			if( node.getWitnessQuote() != null ) buffer.append(DocumentWriter.escapeText( node.getWitnessQuote() )); 
			buffer.append("</witness>\n");
			buffer.append("\t\t\t</difference>\n");
            buffer.append("\t\t\t<notes>"+DocumentWriter.escapeText( node.getNotes() )+"</notes>\n");
            buffer.append("\t\t\t<image>"+node.includeImage()+"</image>\n");
            buffer.append("\t\t</annotation>\n");
        }

		buffer.append("\t</annotation-set>\n");		
        buffer.append("</juxta>\n");
		
		Charset utf8 = Charset.forName("UTF-8");
		OutputStreamWriter outStream = new OutputStreamWriter( new FileOutputStream(manifestFile), utf8 );
		BufferedWriter writer = new BufferedWriter(outStream);
        writer.write(buffer.toString());
        writer.close();
    }

    public LinkedList createAnnotationList()
    {
        if( annotationSetNode != null )
            return annotationSetNode.createAnnotationList();
        else
            return null;   
    }

}

/*
 * Copyright 2012 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.xml.sax.SAXException;

import com.marklogic.client.Format;
import com.marklogic.client.QueryOptionsManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.QueryOptionsHandle;
import com.marklogic.client.io.StringHandle;

public class QueryOptionsManagerTest {
	
	private static final org.slf4j.Logger logger = LoggerFactory
			.getLogger(QueryOptionsManagerTest.class);;
	
	
	@BeforeClass
	public static void beforeClass() {
		Common.connectAdmin();
	}
	@AfterClass
	public static void afterClass() {
		Common.release();
	}
	

	
	@Test
	public void testQueryOptionsManager() throws JAXBException {
		QueryOptionsManager mgr = Common.client.newQueryOptionsManager();
		assertNotNull("Client could not create query options manager", mgr);

        QueryOptionsHandle options = new QueryOptionsHandle();
		mgr.writeOptions("testempty", new QueryOptionsHandle());
        
        String optionsResult = mgr.readOptions("testempty", new StringHandle()).get();
        logger.debug("Empty options from server {}", optionsResult);
        assertTrue("Empty options result not empty",optionsResult.contains("options"));
        assertTrue("Empty options result not empty",optionsResult.contains("\"http://marklogic.com/appservices/search\"/>"));
		
		mgr.deleteOptions("testempty");
		
		
	};
	

	@Test
	public void testXMLDocsAsSearchOptions() throws ParserConfigurationException, SAXException, IOException {
		String optionsName = "invalid";
		
		Document domDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = domDocument.createElementNS("http://marklogic.com/appservices/search","options");
		Element rf = domDocument.createElementNS("http://marklogic.com/appservices/search","return-facets");
		rf.setTextContent("true");
		root.appendChild(rf);
		root.setAttributeNS("http://www.w3.org/XML/1998/namespace", "lang", "en");  // MarkLogic adds this if I don't
		domDocument.appendChild(root);

		QueryOptionsManager queryOptionsMgr = Common.client.newQueryOptionsManager();
		
		queryOptionsMgr.writeOptions(optionsName, new DOMHandle(domDocument));

		String domString = ((DOMImplementationLS) DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.getDOMImplementation()).createLSSerializer().writeToString(domDocument);
		
		String optionsString = queryOptionsMgr.readOptions(optionsName, new StringHandle()).get();
		assertNotNull("Read null string for XML content",optionsString);
		logger.debug("Two XML Strings {} and {}", domString, optionsString);
		//TODO  xml assertions too stringent.
		// assertXMLEqual("Failed to read XML document as String", domString, optionsString);

		Document readDoc = queryOptionsMgr.readOptions(optionsName, new DOMHandle()).get();
		assertNotNull("Read null document for XML content",readDoc);
		//TODO xml assertions too stringent
		// assertXMLEqual("Failed to read XML document as DOM",domDocument,readDoc);

	}
	


	@Test
	public void testJSONOptions() throws JAXBException {
		QueryOptionsManager mgr = Common.client.newQueryOptionsManager();
		assertNotNull("Client could not create query options manager", mgr);

		FileHandle jsonHandle = new FileHandle(new File("src/test/resources/json-config.json"));
		jsonHandle.setFormat(Format.JSON);
		mgr.writeOptions("jsonoptions", jsonHandle);
		QueryOptionsHandle options = mgr.readOptions("jsonoptions", new QueryOptionsHandle());
		
		assertEquals("JSON options came back incorrectly", options.getConstraints().get(0).getName(), "decade");
		
		
		StringHandle jsonStringHandle = new StringHandle();
		jsonStringHandle.setFormat(Format.JSON);
		mgr.readOptions("jsonoptions", jsonStringHandle);
		assertTrue("JSON String from QueryManager must start with json options", jsonStringHandle.get().startsWith("{\"options\":"));
		
		mgr.deleteOptions("jsonoptions");
		
		
	};
	
}

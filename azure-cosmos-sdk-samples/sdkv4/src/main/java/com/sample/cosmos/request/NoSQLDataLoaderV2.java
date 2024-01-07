package com.sample.cosmos.request;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;

public class NoSQLDataLoaderV2 {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        CosmosClient cosmosClient = CosmosClientUtil.getClient();
        CosmosContainer cosmosContainer = CosmosClientUtil.getCollection(cosmosClient);
        Document document = readXMLDocumentFromFile("C:\\Work\\Sources\\azure-cosmos-java-gremlin\\src\\main\\resources\\air-routes.graphml");
        Element root = document.getDocumentElement();
        System.out.println(root.getNodeName());
        NodeList nList = document.getElementsByTagName("node");
        Map<String,String> mapsNodeType = new HashMap<>();
        Map<String,String> mapsNodeNames = new HashMap<>();
        Map<String,String> docs = new HashMap<>();
        ArrayList<String> fdocs = new ArrayList<>();

        //StringBuilder nodes = new StringBuilder();
        //
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node node = nList.item(temp);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                //Print each employee's detail
                Element eElement = (Element) node;
                if(eElement.getAttribute("id").equals("0"))
                    continue;;
                /*if(eElement.getAttribute("id").equals("10"))
                    break;;*/
                JSONObject json = new JSONObject();
                json.put("id", eElement.getAttribute("id"));
                json.put("doc_type", "node");

                System.out.println("\n id : " + eElement.getAttribute("id"));
                NodeList nList2 = eElement.getElementsByTagName("data");
                //System.out.println("\n length : " +nList2.getLength());

                String type = "";
                String city = "";
                String desc = "";
                for(int x=0,size= nList2.getLength(); x<size; x++) {
                    Node cNode = nList2.item(x);
                    if(cNode.getAttributes().getNamedItem("key").getNodeValue().equals("type")) {
                        type = cNode.getTextContent();
                    }
                    if(cNode.getAttributes().getNamedItem("key").getNodeValue().equals("city")) {
                        city = cNode.getTextContent();
                    }
                    if(cNode.getAttributes().getNamedItem("key").getNodeValue().equals("desc")) {
                        desc = cNode.getTextContent();
                    }
                    json.put(cNode.getAttributes().getNamedItem("key").getNodeValue(), cNode.getTextContent());
                }
                json.put("nodeType", type);
                mapsNodeType.put(eElement.getAttribute("id"), type);
                if(type.equals("airport")) {
                    json.put("id", type+"_"+city);
                    json.put("partitionKey", type+"_"+city);
                    mapsNodeNames.put(eElement.getAttribute("id"), city.replace(" ",""));
                } else {
                    json.put("id", type+"_"+desc);
                    json.put("partitionKey", type+"_"+desc);
                    mapsNodeNames.put(eElement.getAttribute("id"), desc.replace(" ",""));
                }
                docs.put(eElement.getAttribute("id"), json.toString());
                fdocs.add(json.toString());
                /*if(eElement.getAttribute("id").equals("3473")) {
                    System.out.println(json.toString());
                    break;
                }*/
               //ObjectNode json1 = OBJECT_MAPPER.readValue(json.toString(), ObjectNode.class);
               //cosmosContainer.upsertItem(json1);
            }
        }

        System.out.println("mapsNodeNames size "+mapsNodeNames.size());
        System.out.println("mapsNodeType size "+mapsNodeType.size());

        nList = document.getElementsByTagName("edge");
        System.out.println("nList "+nList.getLength());

        /*ArrayList<String> routes = new ArrayList<>();

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node node = nList.item(temp);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                //Print each employee's detail
                Element eElement = (Element) node;
                String source = eElement.getAttribute("source");
                String target = eElement.getAttribute("target");
                if(source.equals("69")) {
                    routes.add(target);
                }
            }
        }
        routes.add("69");
        routes.add("3473");*/
        //for (int temp = 43000; temp < 43500; temp++) {
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node node = nList.item(temp);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                //Print each employee's detail
                Element eElement = (Element) node;
                /*if(eElement.getAttribute("id").equals("0"))
                    continue;;*/

                /*if(Integer.parseInt(eElement.getAttribute("id"))<18613)
                    continue;;*/

                /*if(eElement.getAttribute("id").equals("47078"))
                    break;;*/
                //
                String source = eElement.getAttribute("source");
                String target = eElement.getAttribute("target");
                //
                String pKeyFwd = "";
                JSONObject jsonFwd = new JSONObject();
                //jsonFwd.put("id", eElement.getAttribute("id"));
                jsonFwd.put("id", UUID.randomUUID().toString());
                jsonFwd.put("doc_type", "edge");
                //
                JSONObject jsonBkwd = new JSONObject();
                //jsonBkwd.put("id", eElement.getAttribute("id"));
                jsonBkwd.put("doc_type", "edge");
                jsonBkwd.put("id", UUID.randomUUID().toString());

                System.out.println("\n id : " + eElement.getAttribute("id"));
                NodeList nList2 = eElement.getElementsByTagName("data");
                //System.out.println("\n length : " +nList2.getLength());

                String type = "";
                for(int x=0,size= nList2.getLength(); x<size; x++) {
                    Node cNode = nList2.item(x);
                    if(cNode.getAttributes().getNamedItem("key").getNodeValue().equals("labelE")) {
                        type = cNode.getTextContent();
                    }
                    //jsonFwd.put("id", mapsNodeType.get(source)+"_"+mapsNodeNames.get(source)+"_"+type);
                    jsonFwd.put("partitionKey", mapsNodeType.get(source)+"_"+mapsNodeNames.get(source)+"_"+type);
                    jsonFwd.put("source", mapsNodeNames.get(source));
                    jsonFwd.put("sourceType", mapsNodeType.get(source));
                    jsonFwd.put("destination", mapsNodeNames.get(target));
                    jsonFwd.put("destinationType", mapsNodeType.get(target));
                    jsonFwd.put("edgeType", type);
                    jsonFwd.put(cNode.getAttributes().getNamedItem("key").getNodeValue(), cNode.getTextContent());
                    //
                    //jsonBkwd.put("id", mapsNodeType.get(target)+"_"+mapsNodeNames.get(target)+"_"+type);
                    jsonBkwd.put("partitionKey", mapsNodeType.get(target)+"_"+mapsNodeNames.get(target)+"_"+type);
                    jsonBkwd.put("source", mapsNodeNames.get(target));
                    jsonBkwd.put("sourceType", mapsNodeType.get(target));
                    jsonBkwd.put("destination", mapsNodeNames.get(source));
                    jsonBkwd.put("destinationType", mapsNodeType.get(source));
                    jsonBkwd.put("edgeType", type);
                    jsonBkwd.put(cNode.getAttributes().getNamedItem("key").getNodeValue(), cNode.getTextContent());
                }
                System.out.println(jsonFwd.toString());
                System.out.println(jsonBkwd.toString());
                ObjectNode json1 = OBJECT_MAPPER.readValue(jsonFwd.toString(), ObjectNode.class);
                ObjectNode json2 = OBJECT_MAPPER.readValue(jsonBkwd.toString(), ObjectNode.class);
                //if(routes.contains(source)) {
                    System.out.println("Source "+source + " Target "+target + " id "+eElement.getAttribute("id"));
                    //docs.put(eElement.getAttribute("id"), jsonFwd.toString());
                    //docs.put(eElement.getAttribute("id"), jsonBkwd.toString());
                    fdocs.add(jsonFwd.toString());
                    fdocs.add(jsonBkwd.toString());
                    //fdocs.add(docs.get(source));
                    //fdocs.add(docs.get(target));
                //}
                //cosmosContainer.upsertItem(json1);
                //cosmosContainer.upsertItem(json2);
                //break;
            }
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter("C:/tmp/airroutes-all.json", true));) {
            for(String doc: fdocs)
                writer.append(doc).append("\n");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("C:/tmp/airroutes-all.json"));) {
            String line = bufferedReader.readLine();

            while (line != null) {
                //System.out.println(line);
                cosmosContainer.upsertItem(OBJECT_MAPPER.readValue(line, ObjectNode.class));
                line = bufferedReader.readLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    public static Document readXMLDocumentFromFile(String fileNameWithPath) throws Exception {

        //Get Document Builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        //Build Document
        Document document = builder.parse(new File(fileNameWithPath));

        //Normalize the XML Structure; It's just too important !!
        document.getDocumentElement().normalize();

        return document;
    }
}

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: 4/13/12
 * Time: 1:19 AM
 * To change this template use File | Settings | File Templates.
 */
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

public class TouchDevelop {
    public static TreeMap<String,Set<String>> work_table = new TreeMap<String, Set<String>>();
    public static HashSet<String> scriptPairs = new HashSet<String>();
    public static HashSet<String> nodePairs = new HashSet<String>();

    // hashsets to keep track of nodes that are part of some pattern, so that they are not
    // considered for future patterns(which would be sub-patterns in already found patterns)
    public static HashSet<Integer> index1 = new HashSet<Integer>();
    public static HashSet<Integer> index2 = new HashSet<Integer>();

    public static TreeMap<String,JsonNode> patterns = new TreeMap<String,JsonNode>();
    public static ArrayList<String> thingRefs = new ArrayList<String>();
    public static ArrayList<String> operators = new ArrayList<String>();
    public  static String serializedPattern = "serializedPattern";
    public  static String serializedWorkTable = "serializedWorkTable";
    public  static String parentFolderName = "/home/balaji/ast";

    public static int allowedMismatches = 2;    // max no of #s allowed in a pattern
    public static int minimumPatternSize = 3;   // minimum no of nodes in a pattern
    public static int minimumDifference = 2;    // minimum difference b/w no of nodes in a pattern and no of #s
    public static int minimumClusterSize = 3;   // minimum no of occurences of a pattern to be considered as a cluster


    static {
        String[] a ={"math","senses","radio","media","time","wall","web","bazaar","phone","languages","social","maps",
                "locations","tags","colors","player","code"};
        thingRefs.addAll(Arrays.asList(a));
        String[] b = {"+","-","*","/",">","<","=","and","or","not","||",":\u003D",":=","\u2260","\u2264","\u2265","≤","≥","≠","→"};
        operators.addAll(Arrays.asList(b));
    }


    public static void processActionSubtrees(String script1, String script2, JsonNode action1, JsonNode action2,ObjectMapper objectMapper){
        StringBuffer nodePair;
        nodePairs.clear();
        JsonNode node1 = action1.get("body");
        JsonNode node2 = action2.get("body");

        int action1Size = node1.size();
        int action2Size = node2.size();
        int i = 0;
        int j;
        while(i < action1Size){
            j = 0;
            JsonNode child1 = (JsonNode) node1.get(i);
            if(isComment(child1)){
                i++;
                continue;
            }
            while(j < action2Size){
                nodePair = null;
                nodePair = new StringBuffer();
                JsonNode child2 = (JsonNode) node2.get(j);
                if(isComment(child2) || index2.contains(j)) {
                    j++;
                    continue;
                }
                // check if the nodes are already compared or not. if yes, skip
                nodePair = nodePair.append(script1).append(child1.hashCode()).append(i).append(script2).append(child2.hashCode()).append(j);
                if(nodePairs.contains(nodePair.toString())){
                    j++;
                    continue;
                }
                else {
                    nodePairs.add(nodePair.toString());
                }
                // if node types are same
                if( getJsonNodeTypeField(child1) != null && getJsonNodeTypeField(child1).equalsIgnoreCase(getJsonNodeTypeField(child2))){
                    // if the elements are equal, pass the node indexes to compareNodes
                    if(child1.equals(child2)){
                        compareNodes(objectMapper,script1,script2,action1,action2,i,j);
                    }
                }
                j++;
            }
            i++;
        }
    }



    public static void compareNodes(ObjectMapper objectMapper, String script1, String script2, JsonNode action1, JsonNode action2, int i, int j){
        JsonNode node1 = action1.get("body");
        JsonNode node2 = action2.get("body");

        int action1Size = node1.size();
        int action2Size = node2.size();
        
        int original_i=i;
        int original_j=j;

        ArrayNode pattern = objectMapper.createArrayNode();
        pattern.add(node1.get(i));

        int mismatches = 0;
        int h = 0;
        int diff = 0;
        i++;
        j++;
        while(i < action1Size){
            JsonNode child1 = (JsonNode) node1.get(i);
            if(isComment(child1)){
                i++;
                continue;
            }
            while(i < action1Size && j < action2Size){
                child1 = (JsonNode) node1.get(i);
                if(isComment(child1)){
                    i++;
                    continue;
                }
                JsonNode child2 = (JsonNode) node2.get(j);
                if(isComment(child2)) {
                    j++;
                    continue;
                }
                if(mismatches > 3){
                    break;
                }

                if( getJsonNodeTypeField(child1) != null && getJsonNodeTypeField(child1).equalsIgnoreCase(getJsonNodeTypeField(child2))){
                    if(child1.equals(child2)){
                        pattern.add(child1);
                        index1.add(i);
                        index2.add(j);
                        i++;
                        j++;
                    }
                    // same type but not equal. so add a hash
                    else{
                        ObjectNode hashNode = objectMapper.createObjectNode();
                        hashNode.put(TDStrings.type,"#");
                        pattern.add(hashNode);
                        mismatches++;
                        i++;
                        j++;

                    }
                }
                else{
                    ObjectNode hashNode = objectMapper.createObjectNode();
                    hashNode.put(TDStrings.type,"#");
                    pattern.add(hashNode);
                    mismatches++;
                    i++;
                    j++;
                }
            }
            h = pattern.size();
            if(h > 0){
                pattern = trimPattern(pattern);
                diff = h - pattern.size();
                mismatches = mismatches - diff;
                if(mismatches <= allowedMismatches && pattern.size() - mismatches >= minimumDifference){
                    if(pattern.size() >= minimumPatternSize){
                        addPatternToWorkTable(script1, script2, action1.get(TDStrings.actionName).asText(),
                                action2.get(TDStrings.actionName).asText(), pattern, original_i, original_j,i, j);
                    }
                    return;
                }
                else {
                    index1.clear();
                    index2.clear();
                }
            }
            index1.clear();
            index2.clear();
            return;
        }

    }


    public static void main(String[] args){
        if(patterns == null)
            patterns = new TreeMap<String,JsonNode>();

        if(work_table == null)
            work_table = new TreeMap<String, Set<String>>();

        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.INDENT_OUTPUT,true);
        File parentFolder = new File(parentFolderName);
        parentFolder.mkdir();

        File[] files = parentFolder.listFiles();
        int i = 0;
        for (File file1: files){
            for (File file2 : files){
                String f1 = file1.getName();
                String f2 = file2.getName();
                //check if the files are alredy compared or not
                if(f1.equals(f2) || scriptPairs.contains(f1+f2) || scriptPairs.contains(f2+f1)){
                    continue;
                }
                else {
                    try{
                        System.out.println(i++);
                        JsonNode rootNode1 = m.readTree(file1);
                        JsonNode rootNode2 = m.readTree(file2);
                        JsonNode script1Actions;
                        JsonNode script2Actions;
                        //getActions returns just the action nodes with unwanted informationn stripped out
                        script1Actions = getActions(f1,rootNode1,m);
                        script2Actions = getActions(f2,rootNode2,m);

                        // if aNode is null, then there are no action elements
                        if(script1Actions.get("aNode") == null || script2Actions.get("aNode") == null){
                            return;
                        }

                        // compare every action of f1 with every action of f2

                        Iterator it1 = script1Actions.get("aNode").getElements();

                        while(it1.hasNext()){
                            JsonNode action1 = (JsonNode)it1.next();
                            Iterator it2 = script2Actions.get("aNode").getElements();
                            while(it2.hasNext()){
                                JsonNode action2 = (JsonNode)it2.next();
                                index1.clear();
                                index2.clear();
                                // comaprision
                                processActionSubtrees(f1, f2, action1, action2,m);
                            }
                        }
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        }

        Iterator it1 = work_table.entrySet().iterator();

        while (it1.hasNext()){
            Map.Entry pairs = (Map.Entry)it1.next();
            String k = pairs.getKey().toString();
            Set<String> s = (Set<String>)pairs.getValue();
            // cluster size has to be atleast 3. i.e at least 3 scripts with this pattern
            if(s.size() <= minimumClusterSize){
                patterns.remove(k);
                it1.remove();
            }
        }

        Iterator it = patterns.entrySet().iterator();
        it1 = work_table.entrySet().iterator();
        FileWriter fstream;
        BufferedWriter out;
        ObjectWriter writer = m.writerWithDefaultPrettyPrinter();
        while (it.hasNext()) {
            try{
                Map.Entry pairs = (Map.Entry)it.next();
                new File("patterns").mkdir();
                new File("patterns/"+pairs.getKey().toString()).createNewFile();
                File f =  new File("patterns/"+pairs.getKey().toString());
                f.createNewFile();
                fstream = new FileWriter(f);
                out = new BufferedWriter(fstream);
                JsonNode n = (JsonNode)pairs.getValue();
                out.write(writer.writeValueAsString(n));
                out.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }


        Date date = new Date();
        long da = date.getTime();
        try{
            new File("work_table"+da).createNewFile();
            File f =  new File("work_table"+da);
            fstream = new FileWriter(f);
            out = new BufferedWriter(fstream);
            while (it1.hasNext()) {
                Map.Entry pairs = (Map.Entry)it1.next();
                Set<String> s = (Set<String>)pairs.getValue();
                Iterator it2 = s.iterator();
                out.write(pairs.getKey().toString()+":::\n\t");
                while (it2.hasNext()){
                    out.write(it2.next().toString()+"\n\t");
                }
                out.write("\n");
            }
            out.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("Done!\n");
        System.out.println("total patterns : "+work_table.size()+"\n");

    }

    public static void addPatternToWorkTable(String script1, String script2, String action1, String action2, ArrayNode pattern, int start1, int start2, int end1, int end2){
        try{
            --end1;
            --end2;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(pattern.toString().getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            String hashtext = bigInt.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while(hashtext.length() < 32 ){
                hashtext = "0"+hashtext;
            }


            Set<String> loc = work_table.get(hashtext);
            if(loc == null)
                loc = new TreeSet<String>();
            loc.add(script1+"_"+action1+"_"+start1+"_"+end1);
            loc.add(script2+"_"+action2+"_"+start2+"_"+end2);
            work_table.put(hashtext,loc);
            patterns.put(hashtext,pattern);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

    }

    public static ArrayNode trimPattern(ArrayNode pattern){
        int i = pattern.size() - 1;
        JsonNode node = pattern.get(i);
        while (node.get(TDStrings.type) != null && node.get(TDStrings.type).asText().equals("#")){
            pattern.remove(i--);
            node = pattern.get(i);
        }
        return pattern;

    }

    public static JsonNode getActions(String fileName, JsonNode rootNode,ObjectMapper objectMapper){
        // get things node. It contains all actions
        JsonNode things = rootNode.get(TDStrings.things);

        //create a new JsonNode object
        ObjectNode oNode = objectMapper.createObjectNode();
        // node contain all actions
        ArrayNode aNode = objectMapper.createArrayNode();
        oNode.put("scriptName",fileName);
        oNode.put("aNode",aNode);

        Iterator it1 = things.getElements();
        while (it1.hasNext()){
            JsonNode node1 = (JsonNode)it1.next();
            ObjectNode actionNode = objectMapper.createObjectNode();

            if(node1.isContainerNode() && node1.get("type") != null &&
                    node1.get("type").asText().equalsIgnoreCase("action")){
                actionNode.put("actionName",node1.get(TDStrings.name).asText().replaceAll("\"", ""));
                JsonNode actionBody = node1.get("body");
                actionNode.put("body",cleanActionBody(actionBody,objectMapper));
                aNode.add(actionNode);
            }
        }
        FileWriter fstream;
        BufferedWriter out;
        //pretty print the action to a file
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        new File("actions").mkdir();
        try{
            new File("actions/"+fileName).createNewFile();
            File f =  new File("actions/"+fileName);
            f.createNewFile();
            fstream = new FileWriter(f);
            out = new BufferedWriter(fstream);
            out.write(writer.writeValueAsString(oNode));
            out.close();
        }

        catch (Exception ex){
            ex.printStackTrace();
        }

        return oNode;
    }

    public static JsonNode cleanActionBody(JsonNode actionBody,ObjectMapper objectMapper){
        ObjectNode oNode = objectMapper.createObjectNode();
        ArrayNode aNode = objectMapper.createArrayNode();
        oNode.put("aNode",aNode);
        Iterator it = actionBody.getElements();

        while (it.hasNext()){
            ObjectNode statement = (ObjectNode)it.next();
            String type = statement.get(TDStrings.type).asText();
            // if it is a comment, don't process it.
            if(isComment(statement))
                continue;
            else if(TDStrings.whileRef.equalsIgnoreCase(type) || TDStrings.ifRef.equalsIgnoreCase(type)
                    || TDStrings.forEachRef.equalsIgnoreCase(type) || TDStrings.forRef.equalsIgnoreCase(type)){
                cleanTokens(statement, TDStrings.condition);
                cleanTokens(statement, TDStrings.boundLocal);
                cleanTokens(statement, TDStrings.collection);
                cleanTokens(statement, TDStrings.where);
                ArrayNode whileBody = (ArrayNode)statement.get(TDStrings.body);
                ArrayNode thenBody = (ArrayNode)statement.get(TDStrings.thenBody);
                ArrayNode elseBody = (ArrayNode)statement.get(TDStrings.elseBody);
                if(whileBody != null && whileBody.size() > 0){
                    statement.remove(TDStrings.body);
                    aNode.add(statement);
                    Iterator whileBodyIterator = whileBody.getElements();
                    while (whileBodyIterator.hasNext()){
                        ObjectNode temp = (ObjectNode)whileBodyIterator.next();
                        cleanTokens(temp,"");
                        aNode.add(temp);
                    }
                }
                if(thenBody != null && thenBody.size() > 0){
                    statement.remove(TDStrings.thenBody);
                    aNode.add(statement);
                    Iterator whileBodyIterator = thenBody.getElements();
                    while (whileBodyIterator.hasNext()){
                        ObjectNode temp = (ObjectNode)whileBodyIterator.next();
                        cleanTokens(temp,"");
                        aNode.add(temp);
                    }
                }
                if(elseBody != null && elseBody.size() > 0){
                    statement.remove(TDStrings.elseBody);
                    aNode.add(statement);
                    Iterator whileBodyIterator = elseBody.getElements();
                    while (whileBodyIterator.hasNext()){
                        ObjectNode temp = (ObjectNode)whileBodyIterator.next();
                        cleanTokens(temp,"");
                        aNode.add(temp);
                    }
                }
                //cleanTokens(statement, TDStrings.body);
            }   // if it is tokens element, then we have some clean up to do
            else if(statement.get(TDStrings.tokens) != null){
                cleanTokens(statement,"");
                aNode.add(statement);
            }

        }

        return aNode;
    }

    public static void cleanTokens(ObjectNode statement,String fieldName) {
        ArrayNode tokens = null;
        if(fieldName.isEmpty()){
            tokens = (ArrayNode)statement.get(TDStrings.tokens);
        }
        else if(fieldName.equalsIgnoreCase(TDStrings.collection) || fieldName.equalsIgnoreCase(TDStrings.boundLocal)){
            statement.remove(fieldName);
        }
        else if(fieldName.equalsIgnoreCase(TDStrings.where)){
            JsonNode on =  (JsonNode)statement.get(fieldName);
            if(on != null){
                JsonNode fs = on.get(0);
                if(fs != null)
                    cleanTokens((ObjectNode)fs,TDStrings.condition);
            }

        }
        else{
            JsonNode on =  (JsonNode)statement.get(fieldName);
            if(on != null)
                tokens = (ArrayNode)on.get(TDStrings.tokens);
        }
        processTokens(tokens,statement,fieldName);

    }

    public static void processTokens(ArrayNode tokens,ObjectNode statement,String fieldName){
        if(tokens != null){
            for(int var = 0; var < tokens.size(); var++){
                ObjectNode token = (ObjectNode) tokens.get(var);
                String type = token.get(TDStrings.type).asText();

                if(TDStrings.operator.equalsIgnoreCase(type)){
                    String data = token.get(TDStrings.data).asText();
                    if(operators.contains(data)){
                        //token.put(TDStrings.data,"op");
                    }
                    else{

                        tokens.remove(var);
                        var--;

                        //token.put(TDStrings.data,"c");
                    }
                }

                // if the node is of type literal, remove the data element and assign the kind(String, number)
                // to the type element
                else if(TDStrings.literal.equalsIgnoreCase(type)){
                    token.put(TDStrings.type,TDStrings.thingRef);
                    token.remove(TDStrings.kind);
                    token.remove(TDStrings.data);
                }
                /*
               If the element is a thingRef, we dont' bother about its data. So remove it. However, this is not
               true for propertyRef is important
                */
                else if(TDStrings.thingRef.equalsIgnoreCase(type)){
                    String data = token.get(TDStrings.data).asText();
                    if(thingRefs.contains(data.toLowerCase())){
                        if(data.equalsIgnoreCase("code")){
                            token.put(TDStrings.type,"code");
                        }
                        else{
                            token.put(TDStrings.type,data);
                        }
                    }
                    else if("...".equalsIgnoreCase(data)){
                        tokens.remove(var);
                        var--;
                    }
                    token.remove(TDStrings.data);
                }
                else if(TDStrings.propertyRef.equalsIgnoreCase(type)){
                    token.put(TDStrings.type,token.get(TDStrings.data).asText());
                    token.remove(TDStrings.data);
                }
            }
            if(fieldName.isEmpty()){
                statement.put(TDStrings.tokens,tokens);
            }
            else{
                ObjectNode n = (ObjectNode) statement.get(fieldName);
                n.put(TDStrings.tokens,tokens);
            }
        }

    }
    public static String getJsonNodeTypeField(JsonNode node){
        return node.get(TDStrings.type).asText();
    }

    public static boolean  isComment(JsonNode node){
        return getJsonNodeTypeField(node).equalsIgnoreCase(TDStrings.comment);
    }

    // unused functions

    public static boolean  isCompoundNode(JsonNode node){
        return getJsonNodeTypeField(node).equalsIgnoreCase(TDStrings.whileRef);
    }
    public static boolean  isExprStatement(JsonNode node){
        return getJsonNodeTypeField(node).equalsIgnoreCase(TDStrings.exprStmt);
    }

    public static void readWorkTableFromFile(){
        try{
            System.out.println("Reading serializedWorkTable file");
            File serializedFile = new File(serializedWorkTable);
            if(!serializedFile.exists()){
                return;
            }
            FileInputStream fileIn = new FileInputStream(serializedFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            System.out.println("Loading work_table Object...");
            work_table = (TreeMap<String,Set<String>>)in.readObject();
            System.out.println("Object work_table loaded successfully");
            in.close();
            fileIn.close();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void readPatternsFromFile(){
        try{
            System.out.println("Reading serializedPattern file");
            File serializedFile = new File(serializedPattern);
            if(!serializedFile.exists()){
                return;
            }
            FileInputStream fileIn = new FileInputStream(serializedFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            System.out.println("Loading patterns Object...");
            patterns = (TreeMap<String,JsonNode>)in.readObject();
            System.out.println("Object patterns loaded successfully");
            in.close();
            fileIn.close();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void serializePattern(){
        try{
            FileOutputStream fos = null;
            ObjectOutputStream out = null;
            fos = new FileOutputStream(serializedPattern);
            out = new ObjectOutputStream(fos);
            out.writeObject(patterns);
            out.close();

            System.out.println("Serialization of pattern complete");
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }


    public static void serializeWorkTable(){
        try{
            FileOutputStream fos = null;
            ObjectOutputStream out = null;
            fos = new FileOutputStream(serializedWorkTable);
            out = new ObjectOutputStream(fos);
            out.writeObject(work_table);
            out.close();
            System.out.println("Serialization of work table complete");
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }



}
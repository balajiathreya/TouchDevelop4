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
    public static TreeMap<String,JsonNode> scripts = new TreeMap<String, JsonNode>();
    public static TreeMap<String,Set<String>> work_table = new TreeMap<String, Set<String>>();
    public static HashSet<String> scriptPairs = new HashSet<String>();
    public static HashSet<String> nodePairs = new HashSet<String>();
    public static HashSet<Integer> index1 = new HashSet<Integer>();
    public static HashSet<Integer> index2 = new HashSet<Integer>();
    public static TreeMap<String,JsonNode> patterns = new TreeMap<String,JsonNode>();
    public static ArrayList<String> thingRefs = new ArrayList<String>();
    public static ArrayList<String> operators = new ArrayList<String>();
    public  static String serializedPattern = "serializedPattern";
    public  static String serializedWorkTable = "serializedWorkTable";
    public  static String parentFolderName = "/home/balaji/ast";
    //public  static String parentFolderName = "/home/balaji/foreach";

    public static int allowedMismatches = 2;
    public static int minimumPatternSize = 3;
    public static int minimumDifference = 2;
    public static int minimumClusterSize = 3;


    static {
        String[] a ={"math","senses","radio","media","time","wall","web","bazaar","phone","languages","social","maps",
                "locations","tags","colors","player","code"};
        thingRefs.addAll(Arrays.asList(a));
        String[] b = {"+","-","*","/",">","<","=","and","or","not","||",":\u003D",":=","\u2260","\u2264","\u2265","≤","≥","≠","→"};
        operators.addAll(Arrays.asList(b));
    }


    public static void processActionSubtrees(String script1, String script2, JsonNode action1, JsonNode action2,ObjectMapper objectMapper){
        //ObjectNode pattern = objectMapper.createObjectNode();
        //pattern.put("aNode",aNode);
        StringBuffer nodePair = new StringBuffer();
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
            if(isComment(child1) || index1.contains(i)){
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
                nodePair = nodePair.append(script1).append(child1.hashCode()).append(i).append(script2).append(child2.hashCode()).append(j);
                if(nodePairs.contains(nodePair.toString())){
                    j++;
                    continue;
                }
                else {
                    nodePairs.add(nodePair.toString());
                }
                /*
                try{
                    MessageDigest m = MessageDigest.getInstance("MD5");
                    m.reset();
                    m.update(nodePair.toString().getBytes());
                    byte[] digest = m.digest();
                    BigInteger bigInt = new BigInteger(1,digest);
                    String hashtext = bigInt.toString(16);
                    // Now we need to zero pad it if you actually want the full 32 chars.
                    while(hashtext.length() < 32 ){
                        hashtext = "0"+hashtext;
                    }
                    if(nodePairs.contains(hashtext)){
                        j++;
                        continue;
                    }
                    else {
                        nodePairs.add(hashtext);
                    }
                }
                catch (Exception ex){
                    ex.printStackTrace;
                }

*/
                if( getJsonNodeTypeField(child1) != null && getJsonNodeTypeField(child1).equalsIgnoreCase(getJsonNodeTypeField(child2))){
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

        //ObjectNode pattern = objectMapper.createObjectNode();
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
                //System.out.println("types: " + getJsonNodeTypeField(child1) + " " + getJsonNodeTypeField(child2));
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
                        //System.out.println("double of "+child1.toString());
                        i++;
                        j++;
                    }
                    // same type but not equal
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
                // no of statements >= 2
                // no of #s should be less than or equal to 2
                //  no of nodes in pattern - no of # >= 2.
                if(mismatches <= allowedMismatches && pattern.size() - mismatches >= minimumDifference){
                    if(pattern.size() >= minimumPatternSize){
                        addPatternToWorkTable(script1, script2, action1.get(TDStrings.actionName).asText(),
                                action2.get(TDStrings.actionName).asText(), pattern, i, j);
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
        //load stuff from serialized file
        //readPatternsFromFile();
        //readWorkTableFromFile();
        if(patterns == null)
            patterns = new TreeMap<String,JsonNode>();

        if(work_table == null)
            work_table = new TreeMap<String, Set<String>>();

        if(patterns.size() != 0 && work_table.size() != 0){
            System.out.println("adaaaaaaaaaaaaaaa");
        }
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.INDENT_OUTPUT,true);
        File parentFolder = new File(parentFolderName);
        //File parentFolder = new File("/home/balaji/foreach");
        File[] files = parentFolder.listFiles();
        int i = 0;
        for (File file1: files){
            for (File file2 : files){
                String f1 = file1.getName();
                String f2 = file2.getName();
                if(f1.equals(f2) || scriptPairs.contains(f1+f2) || scriptPairs.contains(f2+f1)){
                    continue;
                }
                else {
                    try{
                        /*
                        //String file1 = "/home/balaji/vlhj";
                        //String file1 = "/home/balaji/vpzd";
                        String file1 = "/home/balaji/wbxsa";
                        //String file2 = "/home/balaji/tabh";
                        //String file2 = "/home/balaji/ggae";
                        String file2 = "/home/balaji/eeco";
                          */
                        System.out.println(i++);
                        JsonNode rootNode1 = m.readTree(file1);
                        //JsonNode rootNode2 = m.readTree(new File("/home/balaji/tabh"));
                        JsonNode rootNode2 = m.readTree(file2);
                        JsonNode script1Actions;
                        JsonNode script2Actions;

                        if(scripts.containsKey(f1)){
                            script1Actions = scripts.get(f1);
                        }
                        else {
                            script1Actions = getActions(f1,rootNode1,m);
                            //scripts.put(f1,script1Actions);
                        }

                        if(scripts.containsKey(f2)){
                            script2Actions = scripts.get(f2);
                        }
                        else {
                            script2Actions = getActions(f2,rootNode2,m);
                            //scripts.put(f2,script2Actions);
                        }

                        if(script1Actions.get("aNode") == null || script2Actions.get("aNode") == null){
                            return;
                        }

                        Iterator it1 = script1Actions.get("aNode").getElements();

                        while(it1.hasNext()){
                            JsonNode action1 = (JsonNode)it1.next();
                            Iterator it2 = script2Actions.get("aNode").getElements();
                            while(it2.hasNext()){
                                JsonNode action2 = (JsonNode)it2.next();
                                /*
                                System.out.println("Comparing actions: " + f1 + ":" +
                                        action1.get(TDStrings.actionName) + " and " + f2 + ":" +
                                        action2.get(TDStrings.actionName));
                                        */
                                index1.clear();
                                index2.clear();

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
                //new File("/home/balaji/1-patterns/"+pairs.getKey().toString()).createNewFile();
                new File("patterns/"+pairs.getKey().toString()).createNewFile();
                File f =  new File("patterns/"+pairs.getKey().toString());
                f.createNewFile();
                fstream = new FileWriter(f);
                out = new BufferedWriter(fstream);
                JsonNode n = (JsonNode)pairs.getValue();
                out.write(writer.writeValueAsString(n));
                out.close();
                //m.writeValue(f, pairs.getValue());
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
                //f.createNewFile();
            }
            out.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("Done!\n");
        System.out.println("total patterns : "+work_table.size()+"\n");
        //serializePattern();
        //serializeWorkTable();

    }

    public static void addPatternToWorkTable(String script1, String script2, String action1, String action2, ArrayNode pattern, int i, int j){
        try{
            int start1,start2, end1, end2;
            end1 = i - 1;
            end2 = j - 1;
            start1 = i - pattern.size();
            start2 = j - pattern.size();
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
        JsonNode things = rootNode.get(TDStrings.things);
        ObjectNode oNode = objectMapper.createObjectNode();
        // node contain all actions
        ArrayNode aNode = objectMapper.createArrayNode();
        oNode.put("scriptName",fileName);
        oNode.put("aNode",aNode);
        // add the name of the script
        if("hvch".equalsIgnoreCase(fileName) || "iqri".equalsIgnoreCase(fileName)){
            System.out.print(fileName);
        }

        Iterator it1 = things.getElements();
        while (it1.hasNext()){
            JsonNode node1 = (JsonNode)it1.next();
            ObjectNode actionNode = objectMapper.createObjectNode();

            actionNode.put("actionName",node1.get(TDStrings.name).asText().replaceAll("\"", ""));
            if(node1.isContainerNode() && node1.get("type") != null &&
                    node1.get("type").asText().equalsIgnoreCase("action")){
                JsonNode actionBody = node1.get("body");
                actionNode.put("body",cleanActionBody(actionBody,objectMapper));
                aNode.add(actionNode);
            }
        }
        FileWriter fstream;
        BufferedWriter out;
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        try{
            new File("actions/"+fileName).createNewFile();
            File f =  new File("actions/"+fileName);
            f.createNewFile();
            fstream = new FileWriter(f);
            out = new BufferedWriter(fstream);
            out.write(writer.writeValueAsString(oNode));
            out.close();
            //f.createNewFile();
            //objectMapper.writeValue(f, oNode);
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


    public static String getJsonNodeTypeField(JsonNode node){
        return node.get(TDStrings.type).asText();
    }

    public static boolean  isComment(JsonNode node){
        return getJsonNodeTypeField(node).equalsIgnoreCase(TDStrings.comment);
    }

    public static boolean  isCompoundNode(JsonNode node){
        return getJsonNodeTypeField(node).equalsIgnoreCase(TDStrings.whileRef);
    }
    public static boolean  isExprStatement(JsonNode node){
        return getJsonNodeTypeField(node).equalsIgnoreCase(TDStrings.exprStmt);
    }


}
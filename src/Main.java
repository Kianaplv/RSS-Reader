import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class Main {
    final static int MAX_ITEMS = 5;

    private static String toString(InputStream inputStream) throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream , "UTF-8"));
        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null)
            stringBuilder.append(inputLine);
        return stringBuilder.toString();
    }
    // To get html source from url
    public static String fetchPageSource(String urlString) throws Exception
    {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML , like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return toString(urlConnection.getInputStream());
    }
    // To get rss url from url
    public static String extractRssUrl(String url) throws IOException
    {
        Document doc = Jsoup.connect(url).get();
        return doc.select("[type='application/rss+xml']").attr("abs:href");
    }

    // To finding a webpage title from html
    public static String extractPageTitle(String html)
    {
        try
        {
            Document doc = Jsoup.parse(html);
            return doc.select("title").first().text();
        }
        catch (Exception e)
        {
            return "Error: no title tag found in page source!";
        }
    }

    // To get rss content from RSS url
    public static void retrieveRssContent(String rssUrl)
    {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(rssXml);
            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < MAX_ITEMS; ++i) {
                Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) itemNode;
                    System.out.println("Title: " + element.getElementsByTagName("title").item(0).getTextContent());
                    System.out.println("Link: " + element.getElementsByTagName("link").item(0).getTextContent());
                    System.out.println("Description: " + element.getElementsByTagName("description").item(0).getTextContent());
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner sc=new Scanner(System.in);
        int userInput=0;
        System.out.println("Welcome to RSS Reader!");
        while(userInput!=4) {
            System.out.println("Type a valid number for your desired action:");
            System.out.println("[1] Show updates");
            System.out.println("[2] Add URL");
            System.out.println("[3] Remove URL");
            System.out.println("[4] Exit");
            userInput = sc.nextInt();
            if (isInternetConnected()) {
                if (userInput > 4 || userInput < 1) {
                    System.out.println("please enter a number between 1 and 4");
                } else {
                    File URLFile = new File("data.txt");
                    switch (userInput) {
                        case 1:
                            System.out.println("Show updates for:");
                            ArrayList<String> listOfUrl = titlePage(URLFile);
                            int index = sc.nextInt();
                            int length = listOfUrl.size();
                            if (index == 0) {
                                for (int j = 0; j < length; j++) {
                                    String Url = listOfUrl.get(j);
                                    System.out.println(extractPageTitle(fetchPageSource(Url)));
                                    retrieveRssContent(extractRssUrl(Url));
                                }
                            } else if (index < -1 || index > length) {
                                System.out.println("You should enter a number between -1 and " + length);
                                break;
                            } else if (index == -1) {
                                break;
                            } else if (1 <= index && index <= length) {
                                String Url = listOfUrl.get(index - 1);
                                System.out.println(extractPageTitle(fetchPageSource(Url)));
                                retrieveRssContent(extractRssUrl(Url));
                            }
                            break;
                        case 2:
                            System.out.println("Please enter website URL to add:");
                            FileWriter fileWriter = new FileWriter(URLFile, true);
                            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                            String userInputURL = sc.next();
                            if (!checkURLExist(URLFile, userInputURL)) {
                                String htmlSource = fetchPageSource(userInputURL);
                                String title = extractPageTitle(htmlSource);
                                bufferedWriter.write(title);
                                bufferedWriter.write(";");
                                bufferedWriter.write(userInputURL);
                                bufferedWriter.write(";");
                                String rssURL = extractRssUrl(userInputURL);
                                bufferedWriter.write(rssURL);
                                bufferedWriter.newLine();
                                bufferedWriter.close();
                                System.out.println("Added " + userInputURL + " successfully");
                            } else
                                System.out.println("URL has been added");
                            break;
                        case 3:
                            System.out.println("Please enter website URL to remove:");
                            String lineToRemove = sc.next();
                            int i = 0;
                            if (checkURLExist(URLFile, lineToRemove)) {
                                String[] s = new String[1];
                                FileReader fReader = new FileReader(URLFile);
                                BufferedReader bReader = new BufferedReader(fReader);
                                String currentLine;
                                while ((currentLine = bReader.readLine()) != null) {
                                    String currentURL = getUrl(currentLine);
                                    if (currentURL.equals(lineToRemove))
                                        continue;
                                    s = Arrays.copyOf(s, i + 1);
                                    s[i] = currentLine;
                                    i++;
                                }
                                bReader.close();
                                FileWriter newFileWriter = new FileWriter(URLFile);
                                BufferedWriter newBuffered = new BufferedWriter(newFileWriter);
                                for (int j = 0; j < i; j++) {
                                    newBuffered.write(s[j]);
                                    newBuffered.newLine();
                                }
                                newBuffered.close();
                                System.out.println("removed " + lineToRemove + " successfully.");
                            } else
                                System.out.println("Couldn't find " + lineToRemove);
                            break;
                    }
                }
            }
        }
    }
    //for checking if the Url exist in the file
    //if it exists return true
    public static boolean checkURLExist(File dataFile, String URL) throws IOException {
        FileReader fileReader= new FileReader(dataFile);
        BufferedReader bufferedReader=new BufferedReader(fileReader);
        String readFile;
        while((readFile = bufferedReader.readLine()) != null){
            if(getUrl(readFile).equals(URL))
                return true;
        }
        return false;
    }
    //to find url in file's lines
    public static String getUrl(String line){
        String currentURL = null;
        String[] separateLineInfo= line.split(";");
        currentURL= separateLineInfo[1];
        return currentURL;
    }
    public static ArrayList<String>  titlePage(File dataFile) throws IOException {
        ArrayList<String> listOfUrl = new ArrayList<>();
        FileReader fileReader= new FileReader(dataFile);
        BufferedReader bufferedReader=new BufferedReader(fileReader);
        String currentLine;
        String currentTitle;
        String[] s= new String[3];
        System.out.println("[0] All website");
        while((currentLine = bufferedReader.readLine()) != null){
            s=currentLine.split(";");
            currentTitle=s[0];
            listOfUrl.add(s[1]);
            System.out.println("["+(listOfUrl.indexOf(s[1])+1) + "]" +currentTitle);
        }
        System.out.println("Enter -1 to return");
        return listOfUrl;
    }
    //check the internet
    public static Boolean isInternetConnected(){
        try {
            URL url = new URL("http://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (MalformedURLException e) {
            System.out.println("Internet is not connected");
            return false;
        } catch (IOException e) {
            System.out.println("Internet is not connected");
            return false;
        }
    }
}

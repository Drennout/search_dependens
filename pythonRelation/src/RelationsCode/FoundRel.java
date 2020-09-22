package RelationsCode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

public class FoundRel {
    public static void downloadFile(String url_file, String file) throws IOException{ // Метод скачивания
        URL url = new URL(url_file); // указываем url страницы
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    public static void graph(String parent, String heir){ // Метод вывода графа
        System.out.println("\"" + parent + "\"" + " -> " + "\"" + heir + "\"");
    }

    public static void requarOutput(File lib_dir, String name_lib) // Метод поиска и вывода зависимостей
            throws ParserConfigurationException, SAXException, IOException{
        SAXParserFactory factory = SAXParserFactory.newInstance(); //создание парсера
        SAXParser parser = factory.newSAXParser();
        String url_lib = "https://pypi.org/simple/" + name_lib + "/"; //путь до библиотеки на pypi.org

        downloadFile(url_lib, lib_dir.getPath() + "/" + name_lib + ".html"); //загрузка html файла

        XMLHandler handler = new XMLHandler();
        parser.parse(new File(lib_dir.getPath() + "/" + name_lib + ".html"),handler);

        //Получили ссылку и теперб можем скчать сам архив
        downloadFile(handler.url_package, lib_dir.getPath() + "/" + name_lib + ".whl");


        //разархивация
        String metadata_path = null; // хранит путь до мета данных
        File whl_dir = new File(lib_dir.getPath() + "/" + name_lib + ".whl");
        File rar_dir = new File(lib_dir.getPath() + "/" + name_lib);
        rar_dir.mkdir();
        ZipUtils.extract(whl_dir, rar_dir);

        //отлавливаем мета
        for (String path : rar_dir.list()){
            if (path.contains("dist-info")) // если название папки имеет дист инфо, то там есть мета данные
                metadata_path = lib_dir.getPath() + "/" + name_lib + "/" + path + "/METADATA";
        }
        File metadata = new File(metadata_path);
        Scanner scan = new Scanner(metadata);
        String line;

        while(scan.hasNextLine()){ // Вытаскиваем зависимости и выводим их в виде кода graphgz
            line = scan.nextLine();
            if (line.contains("Requires-Dist:") && !line.contains("extra")){
                line = line.substring(line.indexOf(' ') + 1, line.length());
                if(line.indexOf(' ') != -1)
                    line = line.substring(0, line.indexOf(' '));
                graph(name_lib, line);
                requarOutput(lib_dir, line);// вызываем рекурсию по поиску зависимостей зависимости
            }
        }
    }

    public static void deleteDir(String path){ // удаление папки библиотеки
        File fileToDel = new File(path);
        if (fileToDel.exists()) {
            if (fileToDel.isDirectory()) {
                if (fileToDel.list().length > 0) {
                    for (String s : fileToDel.list()) {
                        deleteDir(path + "/" + s);
                    }
                }
                fileToDel.delete();
            }

            if (fileToDel.delete()) {
                //System.out.println("File " + fileToDel + " deleted");
            }
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        Scanner in = new Scanner(System.in);
        String name_lib = in.next(), download_in = "/home/serzhei/Загрузки/"; // Вводим название библиотеки и путь для скачивания
        File lib_dir = new File(download_in + name_lib); // создание новой папки
        lib_dir.mkdir();
        System.out.println("digraph G {"); // выводим граф
        requarOutput(lib_dir, name_lib);
        System.out.println("}");
        deleteDir(lib_dir.getPath()); // удаление
    }

    private static class XMLHandler extends DefaultHandler {
        String url_package;
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("a"))
                if (attributes.getValue("href").contains("whl#"))
                    url_package = attributes.getValue("href");
        }
    }
}




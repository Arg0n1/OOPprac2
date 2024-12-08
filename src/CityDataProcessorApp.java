import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.*;
import java.util.stream.StreamSupport;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class CityDataProcessorApp {
    public static void main(String[] args) {
        new UserInterface(new FileProcessor()).run();
    }
}

class UserInterface {
    private final FileProcessor fileProcessor;

    public UserInterface(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Введите путь до файла-справочника или 'exit' для завершения работы:");
                String input = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("Завершение работы.");
                    break;
                }

                Path filePath = Path.of(input);
                if (!Files.exists(filePath)) {
                    System.out.println("Файл не найден. Попробуйте снова.");
                    continue;
                }

                try {
                    Instant start = Instant.now();
                    fileProcessor.processFile(filePath);
                    Instant end = Instant.now();
                    System.out.printf("Время обработки файла: %d мс%n%n", Duration.between(start, end).toMillis());
                } catch (UnsupportedFileFormatException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    System.out.println("Произошла ошибка при обработке файла: " + e.getMessage());
                }
            }
        }
    }
}

class FileProcessor {
    private final XmlSaxParser xmlParser = new XmlSaxParser();

    public void processFile(Path filePath) throws Exception {
        String fileType = getFileExtension(filePath);

        Stream<Record> records = switch (fileType.toLowerCase()) {
            case "xml" -> xmlParser.parse(filePath);
            case "csv" -> CsvParser.parse(filePath);
            default -> throw new UnsupportedFileFormatException("Неподдерживаемый формат файла. Поддерживаются только XML и CSV.");
        };

        FileStatistics stats = new FileStatistics();
        records.parallel().forEach(stats::addRecord);

        stats.printStatistics();
    }

    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex != -1) ? fileName.substring(lastDotIndex + 1) : "";
    }
}

class CsvParser {
    public static Stream<Record> parse(Path filePath) throws Exception {
        return Files.lines(filePath)
                .skip(1)
                .parallel()
                .map(line -> line.split(";"))
                .filter(parts -> parts.length >= 4)
                .map(parts -> new Record(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()));
    }
}

class XmlSaxParser {
    public Stream<Record> parse(Path filePath) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        XmlHandler handler = new XmlHandler();

        saxParser.parse(filePath.toFile(), handler);

        return StreamSupport.stream(handler.getRecords().spliterator(), false);
    }
}

class XmlHandler extends DefaultHandler {
    private final List<Record> records = new ArrayList<>();
    private String city, street, house, floor;

    public List<Record> getRecords() {
        return records;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("item".equals(qName)) {
            city = attributes.getValue("city");
            street = attributes.getValue("street");
            house = attributes.getValue("house");
            floor = attributes.getValue("floor");

            if (city != null && street != null && house != null && floor != null) {
                records.add(new Record(city, street, house, floor));
            }
        }
    }
}

class Record {
    private final String city;
    private final String street;
    private final String house;
    private final String floor;

    public Record(String city, String street, String house, String floor) {
        this.city = city;
        this.street = street;
        this.house = house;
        this.floor = floor;
    }

    public String getCity() {
        return city;
    }

    public String getCompositeKey() {
        return String.join(",", city, street, house, floor);
    }

    public int getFloorNumber() {
        return Integer.parseInt(floor);
    }
}

class FileStatistics {
    private final Map<String, Integer> duplicateRecords = new ConcurrentHashMap<>();
    private final Map<String, int[]> cityStats = new ConcurrentHashMap<>();

    public void addRecord(Record record) {
        String key = record.getCompositeKey();
        duplicateRecords.merge(key, 1, Integer::sum);

        cityStats.computeIfAbsent(record.getCity(), k -> new int[5])[record.getFloorNumber() - 1]++;
    }

    public void printStatistics() {
        System.out.println("Дублирующиеся записи:");
        duplicateRecords.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.printf("%s - %d раз(а)%n", entry.getKey(), entry.getValue()));

        System.out.println("\nСтатистика по городам:");
        cityStats.forEach((city, floors) -> System.out.printf(
                "%s: 1-этажных: %d, 2-этажных: %d, 3-этажных: %d, 4-этажных: %d, 5-этажных: %d%n",
                city, floors[0], floors[1], floors[2], floors[3], floors[4]));
    }
}

class UnsupportedFileFormatException extends Exception {
    public UnsupportedFileFormatException(String message) {
        super(message);
    }
}
